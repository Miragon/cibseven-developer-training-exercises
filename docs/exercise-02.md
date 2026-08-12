# Aufgabe 2 – Automatisierung des Prozesses

## Ziel-Modell

In Aufgabe 0 hast du den Prozess **fachlich** modelliert, in Aufgabe 1 die technisch
fertige Version des Consultants gesehen. Jetzt machst du die **technische Modellierung
selbst** – und verbindest sie mit Java-Code:

![BPMN Modell der Aufgabe](assets/exercise-02.svg)

Referenz-Modell: `../models/exercise-02/newsletter.bpmn`

## Lernziele

- Ein fachliches BPMN **technisch** vervollständigen (IDs, Prozess-Key, Formularfelder,
  Delegate Expression, `historyTimeToLive`)
- Hexagonale Architektur (Ports & Adapters) verstehen
- BPMN Service Task mit Java-Code verbinden (JavaDelegate-Pattern)
- Prozess über RuntimeService starten
- REST-Endpoint zum Starten des Prozesses implementieren

## Hintergrund

Der Newsletter ist live. Seit dem Launch des neuen Gravel Bikes kommen die Sign-ups rein –
und irgendwer muss jetzt jede Anmeldung manuell im Cockpit durchklicken.

Das ist natürlich **keine** Lösung. Wir sind Entwickler. Wir automatisieren Dinge, selbst
wenn es nur ein Newsletter für Fahrrad-Enthusiasten ist.

> *„Ich klick das doch nicht 500 Mal von Hand durch."*
> — Das gesamte Team, zur Gravel-Bike-Saison

Jetzt wird der Prozess technisch automatisiert: Der Service Task `Send Welcome Mail` soll
echten Code ausführen.

Das Projekt folgt der hexagonalen Architektur:

```
POST /api/subscriptions
       ↓
SubscriptionController          (adapter/inbound/rest)
       ↓
RegisterSubscriptionUseCase     (application/port/inbound)
       ↓
RegisterSubscriptionService     (application/service)        ← TODO
       ↓
SubscriptionProcess.startProcess()  (application/port/outbound)
       ↓
SubscriptionProcessAdapter          (adapter/outbound/cibseven) ← TODO
       ↓
RuntimeService.startProcessInstanceByKey(...)
```

```
[BPMN: serviceTask_sendWelcomeMail]
       ↓
SendWelcomeMailDelegate           (adapter/inbound/cibseven) ← TODO
       ↓
SendWelcomeMailUseCase            (application/port/inbound)
       ↓
SendWelcomeMailService            (application/service)       ← TODO
```

## Technische Modellierung

Das mitgelieferte `src/main/resources/bpmn/newsletter.bpmn` ist bislang nur **fachlich**
modelliert. Damit die Engine es ausführen und der Java-Code andocken kann, vervollständige
es im **Miragon BPMN Modeler** technisch:

### Element-IDs & Namen

| Element | Typ | ID | Name |
|---|---|---|---|
| Start-Event | None Start Event | `startEvent_newsletterWanted` | Newsletter wanted |
| Formular | User Task | `userTask_fillOutForm` | Fill out form |
| Welcome Mail | Service Task | `serviceTask_sendWelcomeMail` | Send Welcome Mail |
| End-Event | None End Event | `endEvent_userSubscribed` | User subscribed |

**Prozess-ID:** `subscribeNewsletter` · **`Executable`** aktivieren · **`History Time To Live`** = `180`

**Formular-Felder** (am User Task konfigurieren):
- `email` (String) – E-Mail-Adresse
- `name` (String) – Vollständiger Name
- `age` (Long) – Alter in Jahren

**Service Task Konfiguration:**
- Implementation: `Delegate Expression`
- Delegate Expression: `#{sendWelcomeMailDelegate}`

### Element-ID-Konventionen

| Präfix | Für |
|---|---|
| `startEvent_` | Start-Events |
| `endEvent_` | End-Events |
| `userTask_` | User Tasks |
| `serviceTask_` | Service Tasks |
| `gateway_` | Gateways |
| `subProcess_` | Subprozesse |
| `boundaryEvent_` | Boundary Events |

> Genau diese technischen Attribute hatte in Aufgabe 1 der Consultant gesetzt. Ab jetzt
> machst du sie selbst – in den nächsten Aufgaben gehört das zum Handwerk.

## Aufgaben

### 0. Business-Schicht einkommentieren

Die für diese Aufgabe benötigten Klassen sind mit `TODO Aufgabe 2` auskommentiert
(sie hingen bisher an der erst in Aufgabe 1 aktivierten Engine bzw. an noch nicht
implementierter Logik). **Kommentiere sie jetzt ein** – entferne dazu jeweils die Zeilen
mit `/*` und `*/`:

- `application/service/RegisterSubscriptionService.java`
- `application/service/SendWelcomeMailService.java`
- `adapter/inbound/rest/SubscriptionController.java`
- `adapter/inbound/cibseven/BaseDelegate.java`
- `adapter/inbound/cibseven/SendWelcomeMailDelegate.java`
- `adapter/outbound/cibseven/SubscriptionProcessAdapter.java`

Anschließend füllst du die `TODO`s in den folgenden Schritten.

### 1. `RegisterSubscriptionService` implementieren

**Datei:** `application/service/RegisterSubscriptionService.java`

Ersetze das `TODO` mit folgender Logik:
1. Erstelle ein `Subscription`-Objekt mit E-Mail, Name und Alter aus dem Command
2. Speichere es über das Repository
3. Starte den Prozess über den Process-Port
4. Gib die `subscription.id` zurück

### 2. `SendWelcomeMailService` implementieren

**Datei:** `application/service/SendWelcomeMailService.java`

Ersetze das `TODO` mit einem Log-Statement, das die E-Mail-Adresse der Subscription ausgibt.

### 3. `SendWelcomeMailDelegate` implementieren

**Datei:** `adapter/inbound/cibseven/SendWelcomeMailDelegate.java`

Ersetze das `TODO` in `executeTask(execution)`:
- Lies die Prozessvariable `subscriptionId` aus der `DelegateExecution`
- Rufe den UseCase `sendWelcomeMail(...)` mit der gelesenen ID auf

### 4. `SubscriptionProcessAdapter` implementieren

**Datei:** `adapter/outbound/cibseven/SubscriptionProcessAdapter.java`

Ersetze das `TODO` in `startProcess(subscription)`:
- Verwende `runtimeService.startProcessInstanceByKey(...)` mit dem Prozess-Key `subscribeNewsletter`
- Übergib die Prozessvariablen (`subscriptionId`, `email`, `name`, `age`) als Map – die Schlüssel entsprechen den Variablennamen im BPMN-Modell

## Testen

```bash
# Anwendung starten
../../mvnw spring-boot:run

# Subscription registrieren
curl -X POST http://localhost:8080/api/subscriptions \
  -H "Content-Type: application/json" \
  -d '{"email": "alice@miravelo.com", "name": "Alice", "age": 28}'
```

Danach im **Cockpit** unter http://localhost:8080/camunda:
- Unter **Processes** → eine Instanz von `Subscribe Newsletter` vorhanden
- UserTask `Fill out form` erscheint in **Task List**
- Nach Abschluss der UserTask → Service Task läuft durch → Log: "Sending welcome mail to alice@miravelo.com"

## Bonus: Prozesstest

Prozess-Tests bekommen in **[Aufgabe 5](exercise-05.md)** ihren eigenen, ausführlichen Platz –
dort schreibst du einen vollwertigen Prozess-Test gegen den Membership-Prozess (In-Memory-Engine,
gemockte Use Cases, ohne PostgreSQL). Der Platzhalter dafür liegt schon bereit unter
`src/test/java/io/miragon/training/process/MembershipProcessTest.java`.

## Referenzlösung

`../solutions/exercise-02/`

---

➡️ [Weiter zu Aufgabe 3](exercise-03.md)
