# Aufgabe 9 – Remote Engine & External Task

> **Voraussetzung: Aufgabe 8** ist abgeschlossen – der vollständige Membership-Prozess (Subprozess,
> Boundary Events, Kompensation, Call Activity & DMN) läuft, inklusive des `notifyCommunity`-Zweigs,
> den du in Aufgabe 6 als **In-Engine-Delegate** gebaut hast.

## Ziel-Modell

![BPMN Hauptprozess](assets/exercise-09-main.svg)

Referenz-Modell: `../models/exercise-09/newsletter.bpmn`. Das Prozess-Modell bleibt fachlich identisch –
nur der Task `serviceTask_notifyCommunity` wechselt vom Delegate zum **External Task**.

## Lernziele

- Einen bestehenden **In-Engine-Delegate** zu einem **External Service Task**
  (`camunda:type="external"` + Topic) umbauen
- Einen **External Task Worker** in einem **eigenen Service** (eigene JVM) bauen, der sich über die
  **REST-API** mit der Engine verbindet (Remote Engine)
- Verstehen, **warum** man einen Task auslagert (Wiederverwendung, Isolation von Secrets, Skalierung)

## Hintergrund

In Aufgabe 6 hast du die Community-Benachrichtigung als **Delegate** direkt in die Prozess-Anwendung
gebaut: Der `NotifyCommunityDelegate` ruft einen Use Case, der über den `MicrosoftTeamsMessagePublisher`
eine Karte in einen Teams-Kanal postet. Das funktioniert – aber es sitzt am falschen Ort.

**Zwei Gründe, es auszulagern:**

- **Wiederverwendung:** Nicht nur neue Members sollen gemeldet werden. Demnächst soll z. B. auch **jedes
  Leasing** eine Nachricht in denselben Kanal schicken. Die Benachrichtigung ist eine **eigenständige
  Fähigkeit**, die mehrere Prozesse nutzen – kein Detail des Membership-Prozesses.
- **Secret-Management:** In der Webhook-URL steckt eine **Signatur** – im Grunde ein Passwort für den
  Teams-Kanal. Und Miravelo findet: So ein Geheimnis hat in der großen, ständig umgebauten
  Prozess-Anwendung eigentlich nichts verloren. Also packen wir es dorthin, wo es hingehört – in einen
  kleinen, ruhigen Worker, der genau **eine** Sache tut und das Secret schön für sich behält. 🤫

Deshalb schneiden wir die Benachrichtigung heraus: `serviceTask_notifyCommunity` wird ein **External
Service Task**, und ein **separater Worker-Service** erledigt die Arbeit. Er kennt die Engine nur über
deren REST-API – das klassische **External-Task-Pattern**.

### Warum eine Remote Engine – und keine embedded?

Bisher lief die Engine **embedded** – mitten in der Prozess-Anwendung. Der Notification-Worker **betreibt
keine eigene Engine**, er **nutzt** nur die fremde – über REST. Genau das meint **„Remote Engine"**: die
Engine läuft woanders, unser Service klopft nur an. Der Worker darf so unabhängig deployt, schwächer
skaliert und strenger abgesichert werden als die Prozess-Anwendung.

### Neuer Prozessablauf

```
Hauptprozess (subscribeNewsletter):

  ... → (Confirmed) →╱[Send Welcome Mail]──────────────╲→ [Membership activated]
                   ⬦                                    ⬦
     Parallel-Fork  ╲[Notify community]────────────────╱  Parallel-Join
                      external, topic="notifyCommunity"
                                ▲
                                │ fetch & lock, complete
          Worker-Service (eigene JVM):  NotifyCommunityHandler → RestClient → Teams-Kanal
```

Weil der Join **beide** Zweige abwartet, ist die Membership erst „activated", wenn sowohl die Mail raus
ist **als auch** der Worker die Community-Benachrichtigung abgeschlossen hat.

## Aufgaben

### Teil A – Hauptservice (`services/process-application`)

#### 1. `serviceTask_notifyCommunity` auf External Task umstellen

Im `newsletter.bpmn` den Task von Delegate auf External Task umstellen:

```xml
<bpmn:serviceTask id="serviceTask_notifyCommunity" name="Notify community"
                  camunda:type="external" camunda:topic="notifyCommunity" />
```

#### 2. Den In-Engine-Notifier aus dem Hauptservice entfernen

Die Benachrichtigung wandert komplett in den Worker – im `process-application` fliegen daher raus:
`NotifyCommunityDelegate`, `NotifyCommunityUseCase`, `NotifyCommunityService`,
`NotificationPublisherOutPort`, `MicrosoftTeamsMessagePublisher`, `RestClientConfig`, das
`domain/Notification`-Record und der `notification.teams`-Block aus der `application.yaml`.

> Für `serviceTask_notifyCommunity` brauchst du jetzt **keinen** Delegate, keinen Use Case und keinen
> Outbound-Adapter mehr im Hauptservice – die Arbeit erledigt der externe Worker (Teil B). Die
> Task-Type-Konstante `ServiceTasks.NOTIFY_COMMUNITY` wird beim Build aus der BPMN generiert.

### Teil B – Worker-Service (`services/notification-service`)

Im Worker-Modul `services/notification-service/` implementierst du den **Handler** (den „Delegate"
des Workers) und den **Service**. Der Teams-Adapter `MicrosoftTeamsMessagePublisher` ist bereits
**vorgegeben** – der Adaptive-Card-Aufbau und der REST-Call sind Infrastruktur, kein Lernziel. (Es ist
exakt der Publisher, den du in Aufgabe 6 im Hauptservice gebaut hast – jetzt lebt er hier.)

#### 3. External Task Handler abonnieren (der „Delegate")

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

Der Handler übersetzt das External-Task-Event in ein Domain-Objekt **`Notification`** (Titel + Text)
und gibt es an den Use Case `PublishNotificationUseCase` weiter – **nicht** den Member selbst.

#### 4. Service implementieren

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
> „Incoming Webhook"-Connector **nicht** verwenden (2026 abgeschaltet). Da die Ausgabe hinter dem
> Out-Port `NotificationPublisherOutPort` steckt, lässt sich statt Teams leicht ein anderer Web-Service
> anbinden.

## Testen

```bash
# 1. Stack + Hauptservice (Port 8080) starten
cd stack && docker-compose up -d
cd ../solutions/exercise-09/process-application && ../../../mvnw spring-boot:run

# 2. Worker mit deiner Teams-Webhook-URL starten (eigenes Terminal)
cd solutions/exercise-09/notification-service
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

Automatisiert: `./mvnw -pl solutions/exercise-09/process-application test -Dtest=MembershipProcessTest`.
Im Prozess-Test steht `completeExternalTask(...)` stellvertretend für den Remote-Worker.

## Referenzlösung

- Hauptservice: `../solutions/exercise-09/process-application/`
- Worker-Service: `../solutions/exercise-09/notification-service/`

---

🎉 **Geschafft!** Du hast den vollständigen Membership-Prozess gebaut – von der reinen Modellierung bis
zum ausgelagerten Remote-Worker. Lust auf mehr? Die [Extra-Aufgabe](extra-task-1.md) baut den Prozess
engine-neutral um.
