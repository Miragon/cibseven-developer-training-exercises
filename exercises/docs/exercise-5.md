# Aufgabe 5 – Signal Events

## Lernziele

- Signal End Events und Signal Start Events verstehen
- Signale innerhalb eines Prozessmodells verwenden
- Outbound Port für externe Kommunikation einführen

## Hintergrund

Wenn eine Membership erfolgreich bestätigt wird, soll ein **Signal** geworfen werden.
Dieses Signal wird im gleichen Prozessmodell durch ein Signal Start Event
(in einem separaten Event-Subprozess) empfangen und verarbeitet.

Ziel: Externe Systeme über die erfolgreiche Membership benachrichtigen
(z.B. Forum-Post, Slack-Nachricht, Webhook).

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

**Neue Datei:** `application/port/outbound/MembershipEventPublisher.kt`

```kotlin
interface MembershipEventPublisher {
    fun publishMembershipActivated(membershipId: MembershipId)
}
```

### 3. Use Case und Service

**`NotifyAboutSignedMembershipUseCase`** / **`NotifyAboutSignedMembershipService`**:
```kotlin
override fun notify(membershipId: MembershipId) {
    val membership = repository.find(membershipId)
    log.info { "Publishing membership activation for ${membership.email.value}" }
    publisher.publishMembershipActivated(membershipId)
}
```

### 4. Publisher-Adapter implementieren

**Neue Datei:** `adapter/outbound/MembershipEventPublisherAdapter.kt`

Einfache Implementierung (Logging reicht für jetzt):
```kotlin
@Component
class MembershipEventPublisherAdapter : MembershipEventPublisher {
    private val log = KotlinLogging.logger {}
    override fun publishMembershipActivated(membershipId: MembershipId) {
        log.info { "EVENT: MembershipActivated(id=${membershipId.value})" }
        // TODO Aufgabe 5 Bonus: HTTP-Webhook oder Kafka-Event senden
    }
}
```

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
