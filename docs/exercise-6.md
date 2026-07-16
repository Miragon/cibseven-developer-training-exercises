# Aufgabe 6 – Remote Engine & External Task

## Ziel-Modell

Hauptprozess mit dem neuen Message Throw Event `throw_notifyNewMember`:

![BPMN Hauptprozess](assets/exercise-6.svg)

Der zweite Prozess `employeeNotification` mit dem External Service Task:

![BPMN Benachrichtigungs-Prozess](assets/exercise-6-notification.svg)

## Lernziele

- Message **Throw Event** (per Delegate) einsetzen, um einen **zweiten Prozess** zu starten
- **External Service Task** (`camunda:type="external"` + Topic) modellieren
- Einen **External Task Worker** in einem **eigenen Service** (eigene JVM) bauen, der sich
  über die **REST-API** mit der Engine verbindet (Remote Engine)
- Aus dem Worker einen echten **Web-Service** aufrufen (kollaborative „Members Wall")

## Hintergrund

Miravelo wächst – und jedes Mal, wenn jemand dem **Inner Circle** beitritt, sollen es *alle*
mitbekommen. Bisher endet der Prozess still mit „Membership confirmed". Jetzt soll er, sobald ein
Mitglied bestätigt ist, eine Nachricht **werfen**, die einen zweiten, eigenständigen
**Benachrichtigungs-Prozess** startet. Dessen einzige Aufgabe (ein External Service Task) wird
nicht mehr von einem Delegate in der Engine erledigt, sondern von einem **separaten Worker-Service**,
der die Engine nur über deren REST-API kennt – das klassische **External-Task-Pattern**.

Der Worker trägt jeden neuen Member in eine **gemeinsame, öffentliche „Inner Circle Members Wall"**
ein, die der ganze Schulungsraum live im Browser mitliest. Jeder sieht seinen eigenen Eintrag –
und den der anderen – auftauchen. Das ist unser öffentliches Erfolgserlebnis. 🎉

### Neuer Prozessablauf

```
Hauptprozess (subscribeNewsletter):
  ... → [Send Welcome Mail] → (⨯ Throw: Notify new member) → [Membership confirmed]
                                        │  #{notifyNewMemberDelegate}
                                        ▼  startet
Zweiter Prozess (employeeNotification):
  (Start) → [Notify employees]   → (End)
             external, topic="notifyEmployees"
                     ▲
                     │ fetch & lock, complete
        Worker-Service (eigene JVM):  NotifyEmployeesHandler → RestClient → Members Wall
```

## Architektur

Es kommen **zwei Dinge** dazu:

1. **Hauptservice** (das `exercise`-Modul): ein Message-Throw-Event + ein zweiter Prozess
   (`employeeNotification`) mit einem External Service Task.
2. **Worker-Service** (neues Modul `services/notification-service`): ein eigenständiger Spring-Boot-Prozess
   **ohne eigene Engine**, der sich per External Task Client remote an `http://localhost:8080/engine-rest`
   hängt und den Topic `notifyEmployees` abarbeitet. Er hat einen Out-Port `EmployeeNotifier` mit
   **zwei Implementierungen**, umschaltbar per `notification.sink`:
   - `JsonBlobEmployeeNotifier` (Default) → Members Wall auf jsonblob.com
   - `TeamsEmployeeNotifier` → Microsoft-Teams-Kanal (Alternative)

## Aufgaben

### Teil A – Hauptservice

#### 1. Zweiten Prozess `employee-notification.bpmn` anlegen

Ein einfacher One-Tasker unter `src/main/resources/bpmn/employee-notification.bpmn`:

- Prozess-ID `employeeNotification`, `isExecutable="true"`, `camunda:historyTimeToLive="180"`
- `Start` → **Service Task** `serviceTask_notifyEmployees` → `End`
- Der Service Task ist ein **External Task**:
  ```xml
  <bpmn:serviceTask id="serviceTask_notifyEmployees" name="Notify employees"
                    camunda:type="external" camunda:topic="notifyEmployees" />
  ```

#### 2. Message Throw Event im Hauptprozess ergänzen

Im `newsletter.bpmn` auf dem Confirmed-Pfad – nach `serviceTask_sendWelcomeMail`, vor dem
Confirmed-End-Event – ein **Message Intermediate Throw Event** einfügen, das per Delegate den
zweiten Prozess startet:

```xml
<bpmn:intermediateThrowEvent id="throw_notifyNewMember" name="Notify new member">
  <bpmn:messageEventDefinition messageRef="Message_NewMemberJoined"
      camunda:delegateExpression="#{notifyNewMemberDelegate}" />
</bpmn:intermediateThrowEvent>
```

(plus die `bpmn:message`-Deklaration `Message_NewMemberJoined`).

#### 3. Delegate, Use Case, Service & Outbound-Adapter

Nach dem bekannten Muster (Delegate → Use Case → Service → Outbound-Port → Adapter):

- `adapter/inbound/cibseven/NotifyNewMemberDelegate` (extends `BaseDelegate`) – liest `name` und
  `email` aus der Execution, ruft `StartEmployeeNotificationUseCase`.
- `application/port/inbound/StartEmployeeNotificationUseCase` (+ `Command`-Record).
- `application/service/StartEmployeeNotificationService`.
- `application/port/outbound/EmployeeNotificationProcess`.
- `adapter/outbound/cibseven/EmployeeNotificationProcessAdapter` – startet den zweiten Prozess:
  ```java
  runtimeService.startProcessInstanceByKey(
      EmployeeNotificationProcessApi.PROCESS_ID.getValue(),
      Map.of("name", memberName, "email", memberEmail));
  ```

> Die typsichere `EmployeeNotificationProcessApi` wird beim Build aus der neuen BPMN generiert
> (bpmn-to-code, siehe [Aufgabe 5 · Add-on](exercise-5-addon.md)).

### Teil B – Worker-Service (`services/notification-service`)

Der Worker liegt im Modul `services/notification-service/` und wird von dir gefüllt (`TODO Aufgabe 6`).

#### 4. External Task Handler abonnieren

`adapter/inbound/cibseven/NotifyEmployeesHandler` implementiert `ExternalTaskHandler` und
abonniert den Topic:

```java
@Component
@ExternalTaskSubscription(topicName = "notifyEmployees")
public class NotifyEmployeesHandler implements ExternalTaskHandler {
    public void execute(ExternalTask task, ExternalTaskService service) {
        String name = task.getVariable("name");
        String email = task.getVariable("email");
        useCase.notify(new NewMember(name, email, LocalDateTime.now().toString()));
        service.complete(task);   // Task abschließen!
    }
}
```

#### 5. Members Wall aufrufen

`adapter/outbound/jsonblob/JsonBlobEmployeeNotifier` (Default-Sink): den geteilten Blob **GET**en,
den neuen Member anhängen, die aktualisierte Liste **PUT**en (per `RestClient`).

Die Verbindung zur Remote Engine steht in `application.yaml`:

```yaml
camunda:
  bpm:
    client:
      base-url: http://localhost:8080/engine-rest   # Remote Engine REST-API
notification:
  sink: jsonblob                                     # jsonblob (default) | teams
  jsonblob:
    blob-url: https://jsonblob.com/api/jsonBlob/CHANGE-ME
```

## Members Wall einrichten (jsonblob.com – kein Account nötig)

1. [jsonblob.com](https://jsonblob.com) öffnen, den Editor-Inhalt durch `[]` ersetzen, **Save**.
2. Die URL wird zu `https://jsonblob.com/<uuid>`; die **API-URL** ist
   `https://jsonblob.com/api/jsonBlob/<uuid>` – diese in `notification.jsonblob.blob-url` eintragen.
3. `wall.html` (im Worker-Modul) im Browser öffnen, dieselbe Blob-URL eintragen → die Wand füllt
   sich live.

> **Alternative – Microsoft Teams:** `notification.sink=teams` setzen und in
> `notification.teams.webhook-url` eine **Power-Automate-„Workflows"-Webhook-URL** eintragen
> (Vorlage „When a Teams webhook request is received"). Nicht den alten Incoming-Webhook-Connector
> verwenden (wird 2026 abgeschaltet). Nur für Team-Mitglieder sichtbar.
>
> **Alternative – webhook.site:** eine geteilte URL, roher Live-Feed, komplett ohne Account.

## Testen

```bash
# 1. Stack + Hauptservice (Port 8080) starten
cd stack && docker-compose up -d
cd ../solutions/exercise-6 && ../../mvnw spring-boot:run

# 2. Worker mit deiner Blob-URL starten (eigenes Terminal)
cd solutions/notification-worker
../../mvnw spring-boot:run -Dspring-boot.run.arguments=--notification.jsonblob.blob-url=https://jsonblob.com/api/jsonBlob/<uuid>

# 3. Membership anlegen und im Cockpit (http://localhost:8080/camunda, admin/admin)
#    die Confirm-Aufgabe abschließen → der External Task wird vom Worker abgeholt →
#    ein neuer Member erscheint auf wall.html.
curl -X POST http://localhost:8080/api/memberships \
  -H "Content-Type: application/json" \
  -d '{"email":"jane@example.com","name":"Jane","age":30}'
```

**Beweis, dass der Worker wirklich remote & separat ist:** Worker **stoppen**, eine Membership
durchführen → der `employeeNotification`-Prozess wartet im Cockpit am External Task. Worker
**starten** → er holt den Task ab und postet. 🎉

Automatisiert: `../../mvnw -pl solutions/exercise-6 test -Dtest=MembershipProcessTest`.

## Referenzlösung

- Hauptservice: `../solutions/exercise-6/`
- Worker-Service: `../solutions/notification-worker/`

---

➡️ [Weiter zu Aufgabe 7](exercise-7.md)
