# Aufgabe 5 – Signal Events

## Lernziele

- Signal End Events und Signal Start Events verstehen
- Signale innerhalb eines Prozessmodells verwenden
- Outbound Port für externe Kommunikation einführen

## Hintergrund

Wenn jemand seine Membership erfolgreich aktiviert, dann ist das ein echtes Highlight! 🎉
Wir haben jemanden auf seiner Reise durch die Quarterlife Crisis für Miravelo gewonnen – das verdient Aufmerksamkeit!

Das Team will diese Erfolgsmomente feiern: Eine Nachricht im Community-Forum posten, eine Benachrichtigung an Slack senden, vielleicht sogar einen Webhook triggern. Denn jede aktivierte Membership ist ein Beweis, dass unser Konzept funktioniert.

Technisch lösen wir das mit einem **Signal Event**: Sobald die Membership aktiviert wird, feuert ein Signal End Event. Dieses Signal wird im selben Prozessmodell durch einen separaten Prozess mit Signal Start Event aufgefangen – und dort starten wir die Benachrichtigungen.

### Erweiterter Prozessablauf

```
...
[Send Welcome Mail]
        ↓
[Membership activated]     ← Signal End Event (wirft: Signal_membershipActivated)

                ↓ (wird empfangen durch)

[Membership activated]     ← Signal Start Event (empfängt: Signal_membershipActivated)
        ↓
[Publish message in forum] ← Service Task
        ↓
[Done]
```

## Aufgaben

### 1. BPMN erweitern

Erweitere den Prozess nach `../models/task-5-signal.bpmn`.

**Änderungen:**

1. End Event `endEvent_membershipActivated` → **Signal End Event**
   - Signal Name: `Signal_membershipActivated`

2. Neuer **Top-Level Signal Start Event** (außerhalb des Hauptflusses, aber im selben Pool):
   - ID: `startEvent_membershipActivated`
   - Name: `Membership activated`
   - Signal: `Signal_membershipActivated`

3. Neuer Service Task nach dem Signal Start Event:
   - ID: `serviceTask_publishSignal`
   - Name: `Publish message in forum`
   - Delegate Expression: `#{notifyAboutSignedMembershipDelegate}`

### 2. Outbound Port erstellen

**Neue Datei:** `application/port/outbound/MembershipEventPublisher.java`

Erstelle ein Interface mit einer Methode `publishMembershipActivated(MembershipId membershipId)`.

### 3. Use Case und Service

**`NotifyAboutSignedMembershipUseCase`** / **`NotifyAboutSignedMembershipService`**:

Lade die Membership aus dem Repository und publiziere das Event über den `MembershipEventPublisher`.

### 4. Publisher-Adapter implementieren

**Neue Datei:** `adapter/outbound/MembershipEventPublisherAdapter.java`

Implementiere das `MembershipEventPublisher`-Interface. Für den Moment reicht ein einfaches Logging – z.B. `"EVENT: MembershipActivated(id=...)"`. Später könnte hier ein HTTP-Webhook oder Kafka-Event angebunden werden.

### 5. `NotifyAboutSignedMembershipDelegate` erstellen

Analog zu bisherigen Delegates, ruft `NotifyAboutSignedMembershipUseCase` auf.

## Testen

```bash
# Membership komplett durchführen (starten + UserTask abschließen)
MEMBERSHIP_ID=$(curl -s -X POST http://localhost:8080/api/memberships \
  -d '{"email": "frank@miravelo.com", "name": "Frank", "age": 29}')
```

Im Cockpit:
1. UserTask `Confirm membership` erscheint
2. Task abschließen
3. Im Log erscheint: `EVENT: MembershipActivated(id=...)`

## Referenzlösung

`../solutions/exercise-5/`

---

➡️ [Weiter zu Aufgabe 6](exercise-6.md)
