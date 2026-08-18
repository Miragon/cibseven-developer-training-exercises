# Aufgabe 3 – Double-Opt-In per Bestätigungs-Mail

> **Voraussetzung:** Aufgabe 2 ist abgeschlossen – der Prozess startet über `POST /api/memberships` und verschickt die Willkommens-Mail über einen Delegate.
> **Arbeitsverzeichnis:** `services/process-application`
> **Neu in dieser Aufgabe:** Message Start Event, Nachrichten-Korrelation, ein zweiter Service Task, ein Bestätigungs-User-Task.

## Darum geht es

Rose hat das neue **Backroad AL** auf den Markt gebracht, Miravelo launcht es exklusiv im
Store. Social Media dreht durch, über Nacht kommen 500 Anmeldungen rein.

Das Team starrt auf die Datenbank und stellt Fragen:

- Sind das echte E-Mail-Adressen?
- Wer ist diese `noreply@throwaway.xyz`?
- Irgendwer hat `admin@miravelo.com` eingetragen. Als Witz. Wahrscheinlich.

> *„500 Sign-ups. Das ist entweder viral oder ein Bot-Angriff."*
> — CTO, beim zweiten Kaffee

Die Antwort ist ein **Double-Opt-In**: erst Mail bestätigen, dann Willkommens-Mail. Und
weil die Anmeldedaten inzwischen ohnehin per REST hereinkommen, wandert das Ausfüllen des
Formulars aus dem Prozess heraus – der Prozess startet ab jetzt mit einer **Nachricht**.

## Lernziele

Nach dieser Aufgabe kannst du

- ein None Start Event durch ein **Message Start Event** ersetzen und begründen, warum,
- eine Prozessinstanz über `createMessageCorrelation(...).correlateStartMessage()` starten,
- mehrere Service Tasks in einem Prozess betreiben,
- einen User Task als **Wait State** einsetzen (Begriff aus [Aufgabe 1](exercise-01.md)),
  an dem die Prozessinstanz auf die Bestätigung wartet,
- einen weiteren Use Case samt Service und Delegate nach bewährtem Muster ergänzen.

## Ziel-Modell

![BPMN-Modell der Aufgabe](../assets/exercise-03.svg)

Referenzmodell: `../../models/exercise-03/membership.bpmn`

**Achte auf drei Änderungen gegenüber Aufgabe 2**, nicht nur auf die beiden neuen Elemente:

1. Das Start Event heißt jetzt `startEvent_submitRegistration` („Submit registration form")
   und ist ein **Message Start Event**.
2. Der User Task `userTask_fillOutForm` **entfällt** samt seiner Formularfelder. Die
   Anmeldedaten kommen über den REST-Aufruf herein und werden beim Start als
   Prozessvariablen gesetzt – ein Formular in der Tasklist braucht es dafür nicht mehr.
3. An seine Stelle tritt weiter hinten der neue User Task `userTask_confirmMembership`.

## Aufgabe

### 1. Start Event auf eine Nachricht umstellen

Öffne `src/main/resources/bpmn/membership.bpmn` im Miragon BPMN Modeler und ersetze das
None Start Event durch ein Message Start Event:

| Eigenschaft | Wert |
|---|---|
| ID | `startEvent_submitRegistration` |
| Name | Submit registration form |
| Typ | Message Start Event |
| Message Name | `Message_SubscriptionRequested` |

Lösche anschließend den User Task `userTask_fillOutForm` inklusive seiner Formularfelder und
verbinde das Start Event mit dem neuen Bestätigungs-Service-Task.

### 2. Prozess um Bestätigungsschritt erweitern

Zwischen Message Start Event und Willkommens-Mail kommen zwei Elemente dazu: ein **Service
Task**, der die Bestätigungs-Mail verschickt, und ein **User Task** als **Wait State** – dort
bleibt die Prozessinstanz stehen, bis die Bestätigung als abgeschlossener Task
zurückgemeldet wird. Das ist genau der Zustand, den du in Aufgabe 1 in `act_ru_task`
gesehen hast.

| Element | Typ | ID | Name | Konfiguration |
|---|---|---|---|---|
| Bestätigungs-Mail | Service Task | `serviceTask_sendConfirmationMail` | Send confirmation mail | Delegate Expression: `#{sendConfirmationMailDelegate}` |
| Bestätigung | User Task | `userTask_confirmMembership` | Confirm membership | – |

Der Service Task steht **vor** dem User Task: erst die Mail verschicken, dann auf die
Bestätigung warten.

### 3. `SendConfirmationMailUseCase` anlegen

**Neue Datei:** `application/port/inbound/SendConfirmationMailUseCase.java`

Ein Interface mit der Methode `sendConfirmationMail(MembershipId)`.

### 4. `SendConfirmationMailService` implementieren

**Neue Datei:** `application/service/SendConfirmationMailService.java`

Lade die Membership über das Repository und logge die E-Mail-Adresse, an die die
Bestätigungs-Mail geht.

### 5. `SendConfirmationMailDelegate` anlegen

**Neue Datei:** `adapter/inbound/cibseven/SendConfirmationMailDelegate.java`

Orientiere dich an `SendWelcomeMailDelegate`. Der Delegate liest `membershipId` aus der
`DelegateExecution` und ruft `useCase.sendConfirmationMail(...)` auf.

### 6. Prozessstart auf Korrelation umstellen

**Datei:** `adapter/outbound/cibseven/MembershipProcessAdapter.java`

Ein Message Start Event lässt sich nicht mehr über `startProcessInstanceByKey` auslösen.
Stelle `startProcess(...)` auf die Korrelation der Nachricht `Message_SubscriptionRequested`
um. Der `RuntimeService` liefert dir über `createMessageCorrelation(...)` einen Correlation
Builder; die vier Prozessvariablen (`membershipId`, `email`, `name`, `age`) bleiben dieselben
wie in Aufgabe 2. Die konkreten Argumente füllst du selbst:

```java
runtimeService.createMessageCorrelation(/* Message-Name */)
        .setVariables(/* membershipId, email, name, age */)
        .correlateStartMessage();
```

## Randbedingungen

- Der Prozess-Key bleibt `subscribeNewsletter`, der Message-Name ist exakt
  `Message_SubscriptionRequested` – Tippfehler führen zur Laufzeit zu
  `MismatchingMessageCorrelationException`.
- Die Prozessvariablen (`membershipId`, `email`, `name`, `age`) bleiben unverändert; sie
  werden jetzt beim Korrelieren gesetzt statt beim Starten.
- Der neue Use Case folgt demselben Schnitt wie die bestehenden: Port im `application/port/inbound`,
  Implementierung im `application/service`, Engine-Anbindung im `adapter/inbound/cibseven`.

## Erwartetes Ergebnis

Starte die Anwendung neu und melde eine Person an:

```bash
curl -X POST http://localhost:8080/api/memberships \
  -H "Content-Type: application/json" \
  -d '{"email": "bob@miravelo.com", "name": "Bob", "age": 25}'
```

1. Der Service Task `Send confirmation mail` läuft sofort durch – im Log erscheint
   `Sending confirmation mail to bob@miravelo.com`.
2. Der User Task `Confirm membership` erscheint in der Tasklist und die Prozessinstanz wartet.
3. Nach dem Abschließen läuft `Send Welcome Mail` durch und die Instanz endet.

## Selbstcheck

- [ ] Das Start Event ist ein Message Start Event mit dem Namen `Message_SubscriptionRequested`
- [ ] `userTask_fillOutForm` ist aus dem Modell verschwunden
- [ ] Der Prozess wird über `correlateStartMessage()` gestartet und der REST-Aufruf
      liefert weiterhin eine ID zurück
- [ ] Beide Log-Zeilen (Bestätigung, Willkommen) erscheinen in der richtigen Reihenfolge
- [ ] Zwischen den beiden Mails wartet der Prozess am User Task

## Hinweise

**Warum ein Message Start Event?** Ein None Start Event sagt „irgendwer startet hier
irgendwie". Ein Message Start Event benennt den fachlichen Auslöser – *eine Registrierung
ist eingegangen* – und macht ihn im Modell sichtbar. Technisch bekommst du damit dieselbe
Korrelations-API, die du ab Aufgabe 6 auch für Nachrichten **an laufende Instanzen**
brauchst (Ablehnung per Message Boundary Event).

## Referenzlösung

`../../solutions/exercise-03/` – oder direkt laden:

```bash
./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=03
```

## Nächster Schritt

In Aufgabe 4 bekommt der Inner Circle seine Exklusivität – mit Kapazitätsprüfung,
Gateway und Transaktionsgrenzen.

➡️ [Weiter zu Aufgabe 4](exercise-04.md)
