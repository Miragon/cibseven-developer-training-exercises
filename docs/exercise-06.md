# Aufgabe 6 – Parallel Gateway, Remote Engine & External Task

## Ziel-Modell

Hauptprozess mit einem **Parallel Gateway**: nach der Bestätigung laufen *Send Welcome Mail* und die
neue *Notify community*-Aufgabe **gleichzeitig**; ein zweites Parallel Gateway führt beide wieder zusammen.

![BPMN Hauptprozess](assets/exercise-06.svg)

Referenz-Modell: `../models/exercise-06/newsletter.bpmn`.

## Lernziele

- Ein **Parallel Gateway** (Fork/Join) einsetzen, um zwei Zweige **echt parallel** auszuführen
- **External Service Task** (`camunda:type="external"` + Topic) modellieren
- Einen **External Task Worker** in einem **eigenen Service** (eigene JVM) bauen, der sich
  über die **REST-API** mit der Engine verbindet (Remote Engine)
- Aus dem Worker einen echten **Web-Service** aufrufen (Nachricht in einen Microsoft-Teams-Kanal posten)

## Hintergrund

Miravelo wächst – und jedes Mal, wenn jemand dem **Inner Circle** beitritt, sollen es *alle*
mitbekommen. Bisher endet der Prozess still mit „Send Welcome Mail". Jetzt soll parallel zur
Willkommens-Mail eine **Community-Benachrichtigung** rausgehen. Die beiden Dinge hängen nicht
voneinander ab – also modellieren wir sie **parallel**: ein **Parallel Gateway** teilt den Fluss in
zwei Zweige auf, ein zweites führt sie wieder zusammen, bevor die Membership aktiviert ist.

Die Community-Benachrichtigung (`serviceTask_notifyCommunity`) erledigt **kein Delegate in der
Engine**, sondern ein **separater Worker-Service**, der die Engine nur über deren REST-API kennt –
das klassische **External-Task-Pattern**. Der Worker postet jeden neuen Member als **Karte in einen
gemeinsamen Microsoft-Teams-Kanal**, den das ganze Team mitliest – unser öffentliches
Erfolgserlebnis. 🎉

### Warum eine Remote Engine – und keine embedded?

Bisher lief die Engine **embedded** – mitten in der Prozess-Anwendung. Hier machen wir es bewusst
anders: Der Notification-Worker **betreibt keine eigene Engine**, er **nutzt** nur die fremde – über
REST. Genau das meint **„Remote Engine"**: die Engine läuft woanders, unser Service klopft nur an.

Warum der Aufwand? Weil so ein Schnitt eine **Architektur-Entscheidung nach den Eigenschaften des
Service** ist, kein Selbstzweck. Spielen wir es an einem – zugegeben etwas überspitzten – Szenario durch:

- **Skalierung:** Die Prozess-Anwendung stemmt alle Anmeldungen und darf ordentlich hochskalieren.
  Der Worker tuckert dagegen gemütlich vor sich hin – ein neues Mitglied alle paar Minuten. Den
  wollen wir **deutlich schwächer skalieren** (eine Instanz reicht), statt in jeder Kopie eine ganze
  Engine mitzuschleppen.
- **Security:** In diesem Service steckt eine **Webhook-URL mit Secret** (die Signatur ist quasi ein
  Passwort). Solche Geheimnisse **isoliert** man lieber in einem kleinen, streng abgesicherten
  Service – nicht in der breit ausgerollten Prozess-Anwendung, wo die Angriffsfläche riesig ist.

**Unterschiedliche Charakteristiken → unterschiedliche Services.** Die Engine bleibt in der
Prozess-Anwendung; unser Worker redet nur höflich per REST mit ihr.

### Neuer Prozessablauf

```
Hauptprozess (subscribeNewsletter):

  ... → (Confirm) →╱[Send Welcome Mail]──────────────╲→ [Membership confirmed]
                 ⬦                                    ⬦
   Parallel-Fork  ╲[Notify community]────────────────╱  Parallel-Join
                    external, topic="notifyCommunity"
                              ▲
                              │ fetch & lock, complete
        Worker-Service (eigene JVM):  NotifyCommunityHandler → RestClient → Teams-Kanal
```

Weil der Join **beide** Zweige abwartet, ist die Membership erst „confirmed", wenn sowohl die Mail
raus ist **als auch** der Worker die Community-Benachrichtigung abgeschlossen hat.

## Architektur

Es kommen **zwei Dinge** dazu:

1. **Hauptservice** (das `services/process-application`-Modul): ein Parallel Gateway, das den Fluss
   in `serviceTask_sendWelcomeMail` (bekannter Delegate) und den neuen **External Service Task**
   `serviceTask_notifyCommunity` (Topic `notifyCommunity`) aufteilt, plus ein Join-Gateway.
2. **Worker-Service** (Modul `services/notification-service`): ein eigenständiger Spring-Boot-Prozess
   **ohne eigene Engine**, der sich per External Task Client remote an `http://localhost:8080/engine-rest`
   hängt und den Topic `notifyCommunity` abarbeitet. Über den Out-Port `NotificationPublisherOutPort`
   postet der `MicrosoftTeamsMessagePublisher` eine **Adaptive Card** in einen Microsoft-Teams-Kanal.

## Aufgaben

### Teil A – Hauptservice

#### 1. Parallel Gateway + `serviceTask_notifyCommunity` ergänzen

Im `newsletter.bpmn` auf dem Confirmed-Pfad – zwischen dem Ende des Confirm-Schritts und dem
Aktivierungs-End-Event – ein **Parallel Gateway (Fork)** einfügen, das zwei Zweige öffnet:

- Zweig A: das bestehende `serviceTask_sendWelcomeMail`
- Zweig B: einen neuen **External Service Task**:
  ```xml
  <bpmn:serviceTask id="serviceTask_notifyCommunity" name="Notify community"
                    camunda:type="external" camunda:topic="notifyCommunity" />
  ```

Ein zweites **Parallel Gateway (Join)** führt beide Zweige wieder zusammen, bevor das End-Event
erreicht wird.

> Für `serviceTask_notifyCommunity` brauchst du **keinen** Delegate, keinen Use Case und keinen
> Outbound-Adapter im Hauptservice – die Arbeit erledigt der externe Worker (Teil B). Der
> Task-Type-Konstante `ServiceTasks.NOTIFY_COMMUNITY` wird beim Build aus der BPMN generiert
> (bpmn-to-code, siehe [Aufgabe 5 · Add-on](exercise-05-addon.md)).

### Teil B – Worker-Service (`services/notification-service`)

Im Worker-Modul `services/notification-service/` implementierst du nur den **Handler** (den „Delegate"
des Workers) und den **Service**. Der Teams-Adapter `MicrosoftTeamsMessagePublisher` ist bereits
**vorgegeben** – der Adaptive-Card-Aufbau und der REST-Call sind Infrastruktur, kein Lernziel.

#### 2. External Task Handler abonnieren (der „Delegate")

`adapter/inbound/cibseven/NotifyCommunityHandler` implementiert `ExternalTaskHandler` und
abonniert den Topic:

```java
@Component
@ExternalTaskSubscription(topicName = "notifyCommunity")
public class NotifyCommunityHandler implements ExternalTaskHandler {
    public void execute(ExternalTask task, ExternalTaskService taskService) {
        String name = task.getVariable("name");
        publishNotification.publish(new Notification(
                "Miravelo Inner Circle", "🎉 New Inner Circle member: " + name + "!"));
        taskService.complete(task);   // Task abschließen!
    }
}
```

Der Task läuft jetzt **direkt in der Haupt-Prozessinstanz** – die Variable `name` (bei Prozessstart
gesetzt) ist also in Reichweite. Der Handler übersetzt das External-Task-Event in ein Domain-Objekt
**`Notification`** (Titel + Text) und gibt es an den Use Case `PublishNotificationUseCase` weiter –
**nicht** den Member selbst.

#### 3. Service implementieren

`application/service/PublishNotificationService` (implementiert `PublishNotificationUseCase`) reicht
die `Notification` an den Out-Port weiter:

```java
public void publish(Notification notification) {
    notificationPublisher.publish(notification);   // NotificationPublisherOutPort
}
```

> Der Out-Port `NotificationPublisherOutPort` und seine Implementierung
> `MicrosoftTeamsMessagePublisher` (der Teams-Adapter) sind schon da – du rufst sie nur auf.

Die Verbindung zur Remote Engine und die Ziel-URL stehen in `application.yaml`:

```yaml
camunda:
  bpm:
    client:
      base-url: http://localhost:8080/engine-rest   # Remote Engine REST-API
notification:
  teams:
    # Echte URL per Umgebungsvariable TEAMS_WEBHOOK_URL – kein Secret ins Repo committen.
    webhook-url: ${TEAMS_WEBHOOK_URL:https://CHANGE-ME}
```

## Teams-Webhook einrichten (Power Automate „Workflows")

1. In Teams beim Ziel-**Kanal** auf **••• → Workflows** (oder die **Workflows**-App → **Neuer Flow**).
2. Vorlage **„Post to a channel when a webhook request is received"** wählen (Trigger
   „When a Teams webhook request is received"), Teams-Verbindung bestätigen.
3. **Team** + **Kanal** auswählen → **Erstellen**. Es entsteht eine **HTTP-POST-URL** (`https://…`).
4. Diese URL beim Start des Workers als Umgebungsvariable übergeben:
   `TEAMS_WEBHOOK_URL='<deine-URL>'` (oder Argument `--notification.teams.webhook-url=<URL>`).

> ⚠️ Die Signatur (`sig=…`) in der URL ist ein **Secret** – nicht ins Repo committen. Den alten
> „Incoming Webhook"-Connector **nicht** verwenden (2026 abgeschaltet). Der Kanal ist nur für
> Team-Mitglieder sichtbar. Da die Ausgabe hinter dem Out-Port `NotificationPublisherOutPort` steckt, lässt
> sich statt Teams leicht ein anderer Web-Service anbinden.

## Testen

```bash
# 1. Stack + Hauptservice (Port 8080) starten
cd stack && docker-compose up -d
cd ../solutions/exercise-06/process-application && ../../../mvnw spring-boot:run

# 2. Worker mit deiner Teams-Webhook-URL starten (eigenes Terminal)
cd solutions/exercise-06/notification-service
TEAMS_WEBHOOK_URL='<deine-Teams-Webhook-URL>' ../../../mvnw spring-boot:run

# 3. Membership anlegen und im Cockpit (http://localhost:8080/camunda, admin/admin)
#    die Confirm-Aufgabe abschließen → der External Task wird vom Worker abgeholt →
#    eine Karte erscheint im Teams-Kanal.
curl -X POST http://localhost:8080/api/memberships \
  -H "Content-Type: application/json" \
  -d '{"email":"jane@example.com","name":"Jane","age":30}'
```

**Beweis, dass der Worker wirklich remote & separat ist:** Worker **stoppen**, eine Membership
durchführen → der Prozess wartet im Cockpit am External Task `Notify community` (der Parallel-Join
kann noch nicht feuern). Worker **starten** → er holt den Task ab und postet, der Join feuert, die
Membership ist aktiviert. 🎉

Automatisiert: `./mvnw -pl solutions/exercise-06/process-application test -Dtest=MembershipProcessTest`.

## Referenzlösung

- Hauptservice: `../solutions/exercise-06/process-application/`
- Worker-Service: `../solutions/exercise-06/notification-service/`

---

➡️ [Weiter zu Aufgabe 7](exercise-07.md)
