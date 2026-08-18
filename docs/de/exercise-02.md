# Aufgabe 2 – Den Prozess technisch automatisieren

> **Voraussetzung:** Aufgabe 1 ist abgeschlossen – die Engine startet und deployt `newsletter.bpmn`.
> **Arbeitsverzeichnis:** `services/process-application`
> **Neu in dieser Aufgabe:** technische Modellierung, JavaDelegate, hexagonale Architektur, Prozessstart über `RuntimeService`.

## Darum geht es

Der Newsletter ist live. Seit dem Launch des neuen Gravel Bikes kommen die Anmeldungen rein –
und irgendwer klickt jetzt jede einzelne im Cockpit durch.

Das ist keine Lösung. Wir sind Entwickler, wir automatisieren Dinge, selbst wenn es nur ein
Newsletter für Fahrrad-Enthusiasten ist.

> *„Ich klick das doch nicht 500 Mal von Hand durch."*
> — Das gesamte Team, zur Gravel-Bike-Saison

Ab jetzt startet der Prozess über einen REST-Endpunkt, und der Service Task
`Send Welcome Mail` führt echten Java-Code aus.

## Lernziele

Nach dieser Aufgabe kannst du

- ein fachliches BPMN technisch vervollständigen (Element-IDs, Prozess-Key, Formularfelder,
  `isExecutable`, `historyTimeToLive`),
- einen Service Task über eine **Delegate Expression** an eine Spring-Bean binden,
- die Schichten der hexagonalen Architektur einer Anfrage entlang benennen,
- eine Prozessinstanz aus Java über den `RuntimeService` starten,
- einen REST-Endpunkt implementieren, der den Prozess anstößt.

## Ziel-Modell

![BPMN-Modell der Aufgabe](../assets/exercise-02.svg)

Referenzmodell: `../../models/exercise-02/newsletter.bpmn`

Der Ablauf bleibt derselbe wie in Aufgabe 1. Was sich ändert, ist die **Anbindung**: Der
Service Task ruft nicht mehr eine Inline-Expression auf, sondern deinen Java-Code.

So wandert eine Anfrage durch die Architektur:

```
POST /api/subscriptions
       ↓
SubscriptionController              (adapter/inbound/rest)
       ↓
RegisterSubscriptionUseCase         (application/port/inbound)
       ↓
RegisterSubscriptionService         (application/service)          ← TODO
       ↓
SubscriptionProcess.startProcess()  (application/port/outbound)
       ↓
SubscriptionProcessAdapter          (adapter/outbound/cibseven)    ← TODO
       ↓
RuntimeService.startProcessInstanceByKey(...)
```

Und so ruft die Engine zurück in deinen Code:

```
[BPMN: serviceTask_sendWelcomeMail]
       ↓
SendWelcomeMailDelegate             (adapter/inbound/cibseven)     ← TODO
       ↓
SendWelcomeMailUseCase              (application/port/inbound)
       ↓
SendWelcomeMailService              (application/service)          ← TODO
```

## Aufgabe

### 1. Business-Schicht einkommentieren

Die Klassen für diese Aufgabe sind mit `TODO Aufgabe 2` auskommentiert – sie hingen an der
erst in Aufgabe 1 aktivierten Engine. Entferne in diesen Dateien jeweils die Zeilen mit
`/*` und `*/`:

- `application/service/RegisterSubscriptionService.java`
- `application/service/SendWelcomeMailService.java`
- `adapter/inbound/rest/SubscriptionController.java`
- `adapter/inbound/cibseven/BaseDelegate.java`
- `adapter/inbound/cibseven/SendWelcomeMailDelegate.java`
- `adapter/outbound/cibseven/SubscriptionProcessAdapter.java`

### 2. Modell technisch vervollständigen

Die Datei `src/main/resources/bpmn/newsletter.bpmn` im Modul ist die Fassung des Consultants
aus Aufgabe 1 – technisch bereits vollständig. Du hast zwei Wege:

- **Selbst modellieren (empfohlen):** Nimm dein fachliches Modell aus Aufgabe 0, ergänze im
  Miragon BPMN Modeler die technischen Attribute aus den Tabellen unten und ersetze damit
  die Datei im Modul. So übst du die technische Modellierung an deinem eigenen Modell.
- **Nachvollziehen:** Öffne die vorhandene Datei und prüfe sie gegen die Tabellen. Was der
  Consultant gesetzt hat, siehst du dann Attribut für Attribut.

**Element-IDs und Namen:**

| Element | Typ | ID | Name |
|---|---|---|---|
| Start | None Start Event | `startEvent_newsletterWanted` | Newsletter wanted |
| Formular | User Task | `userTask_fillOutForm` | Fill out form |
| Willkommens-Mail | Service Task | `serviceTask_sendWelcomeMail` | Send Welcome Mail |
| Ende | None End Event | `endEvent_userSubscribed` | User subscribed |

**Prozess-Eigenschaften:** Prozess-Key `subscribeNewsletter` · `Executable` aktiviert ·
`History Time To Live` = `180`

**Formularfelder** (am User Task `userTask_fillOutForm`):

| Feld-ID | Label | Typ |
|---|---|---|
| `email` | E-Mail | string |
| `name` | Name | string |
| `age` | Age | long |

### 3. Service Task an den Delegate binden

Das ist die inhaltliche Änderung gegenüber Aufgabe 1 – auch dann, wenn du die vorhandene
Datei weiterverwendest:

| Element | Vorher (Aufgabe 1) | Jetzt |
|---|---|---|
| `serviceTask_sendWelcomeMail` | Implementation: *Expression*, `${execution.setVariable('welcomeMailSentTo', email)}` | Implementation: **Delegate Expression**, `#{sendWelcomeMailDelegate}` |

### 4. `RegisterSubscriptionService` implementieren

**Datei:** `application/service/RegisterSubscriptionService.java`

Ersetze das `TODO` durch diese Logik:

1. Erstelle ein `Subscription`-Objekt aus E-Mail, Name und Alter des Commands.
2. Speichere es über das Repository.
3. Starte den Prozess über den Process-Port.
4. Gib `subscription.id()` zurück.

### 5. `SendWelcomeMailService` implementieren

**Datei:** `application/service/SendWelcomeMailService.java`

Lade die Subscription über das Repository und logge die E-Mail-Adresse, an die die
Willkommens-Mail geht.

### 6. `SendWelcomeMailDelegate` implementieren

**Datei:** `adapter/inbound/cibseven/SendWelcomeMailDelegate.java`

Ersetze das `TODO` in `executeTask(execution)`:

- Lies die Prozessvariable `subscriptionId` aus der `DelegateExecution`.
- Rufe damit `useCase.sendWelcomeMail(...)` auf.

### 7. `SubscriptionProcessAdapter` implementieren

**Datei:** `adapter/outbound/cibseven/SubscriptionProcessAdapter.java`

Ersetze das `TODO` in `startProcess(subscription)`:

- Starte die Instanz mit `runtimeService.startProcessInstanceByKey(...)` und dem Prozess-Key
  `subscribeNewsletter`.
- Übergib die Prozessvariablen `subscriptionId`, `email`, `name` und `age` als Map. Die
  Schlüssel müssen exakt den Variablennamen im Modell entsprechen.

## Randbedingungen

- **Element-ID-Konvention** – ab jetzt in jeder Aufgabe verbindlich:

  | Präfix | Für |
  |---|---|
  | `startEvent_` | Start Events |
  | `endEvent_` | End Events |
  | `userTask_` | User Tasks |
  | `serviceTask_` | Service Tasks |
  | `gateway_` | Gateways |
  | `subProcess_` | Subprozesse |
  | `boundaryEvent_` | Boundary Events |

- Der Delegate ist ein **Adapter**: Er liest Prozessvariablen und ruft einen Use Case auf.
  Fachlogik gehört in den Service, nicht in den Delegate.
- Die Domain-Klassen (`domain/`) bleiben frei von Framework-Importen. Der ArchUnit-Test
  `ArchitectureTest` prüft das.

## Erwartetes Ergebnis

Starte die Anwendung neu, damit das geänderte Modell deployt wird:

```bash
cd services/process-application && ../../mvnw spring-boot:run
```

Löse den Prozess anschließend über die REST-Schnittstelle aus – ab jetzt startet ihn keine
Person mehr von Hand im Cockpit:

```bash
curl -X POST http://localhost:8080/api/subscriptions \
  -H "Content-Type: application/json" \
  -d '{"email": "alice@miravelo.com", "name": "Alice", "age": 28}'
```

Der Aufruf liefert die ID der Subscription. Im Cockpit
(`http://localhost:8080/webapp/#/seven/auth/start`, admin/admin) läuft daraufhin eine Instanz von
`Subscribe Newsletter`, in der **Tasklist** steht `Fill out form`. Nach dem Abschließen des
Tasks läuft der Service Task durch und im Log erscheint
`Sending welcome mail to alice@miravelo.com`.

## Selbstcheck

- [ ] Alle sechs Klassen sind einkommentiert und kompilieren
- [ ] Der Service Task nutzt `#{sendWelcomeMailDelegate}` statt der Inline-Expression
- [ ] Ein `POST /api/subscriptions` erzeugt eine Prozessinstanz mit den vier Prozessvariablen
- [ ] Nach Abschluss des User Tasks steht die Log-Zeile mit der E-Mail-Adresse im Log
- [ ] Die Instanz endet an `endEvent_userSubscribed`
- [ ] `./mvnw -pl services/process-application test -Dtest=ArchitectureTest` ist grün

## Hinweise

Prozess-Tests bekommen in [Aufgabe 5](exercise-05.md) ihren eigenen Platz – dort schreibst
du einen vollwertigen Test gegen den dann fertigen Prozess. Der Platzhalter liegt bereits
unter `src/test/java/io/miragon/training/process/MembershipProcessTest.java`.

## Referenzlösung

`../../solutions/exercise-02/` – oder direkt laden:

```bash
./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=02
```

## Nächster Schritt

In Aufgabe 3 kommt der Bestätigungsschritt dazu – und der Prozess wird nicht mehr direkt,
sondern über eine Nachricht gestartet.

➡️ [Weiter zu Aufgabe 3](exercise-03.md)
