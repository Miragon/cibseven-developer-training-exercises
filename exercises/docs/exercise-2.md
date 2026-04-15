# Aufgabe 2 – Bestätigungs-Mail

## Lernziele

- Einen bestehenden Prozess in Camunda Modeler erweitern
- Mehrere Service Tasks implementieren
- Sequenzielle Flows mit User Tasks kombinieren

## Hintergrund

Miravelo möchte sicherstellen, dass neue Mitglieder ihre E-Mail-Adresse bestätigen,
bevor sie die Welcome Mail erhalten.

### Neuer Prozessablauf

```
[Newsletter wanted]
        ↓
[Send confirmation mail]   ← NEU (Service Task)
        ↓
[Confirm subscription]     ← NEU (User Task)
        ↓
[Send Welcome Mail]
        ↓
[User subscribed]
```

## Aufgaben

### 1. BPMN erweitern

Öffne `src/main/resources/bpmn/newsletter.bpmn` im Camunda Modeler und erweitere den Prozess:

| Element | Typ | ID | Name | Konfiguration |
|---|---|---|---|---|
| Bestätigungs-Mail | Service Task | `serviceTask_sendConfirmationMail` | Send confirmation mail | Delegate Expression: `#{sendConfirmationMailDelegate}` |
| Bestätigung | User Task | `userTask_confirmSubscription` | Confirm subscription | – |

**Achtung:** Der Service Task `sendConfirmationMail` muss **vor** dem User Task stehen.

Referenz-Modell: `../models/task-2-with-confirmation.bpmn`

### 2. `MembershipProcessApi` aktualisieren

**Datei:** `adapter/process/MembershipProcessApi.kt`

Neue Elemente hinzufügen:
```kotlin
object Elements {
    // ... bestehende Konstanten ...
    const val SERVICE_TASK_SEND_CONFIRMATION_MAIL: String = "serviceTask_sendConfirmationMail"
    const val USER_TASK_CONFIRM_SUBSCRIPTION: String = "userTask_confirmSubscription"
}
```

### 3. `SendConfirmationMailUseCase` erstellen

**Neue Datei:** `application/port/inbound/SendConfirmationMailUseCase.kt`

```kotlin
interface SendConfirmationMailUseCase {
    fun sendConfirmationMail(membershipId: MembershipId)
}
```

### 4. `SendConfirmationMailService` implementieren

**Neue Datei:** `application/service/SendConfirmationMailService.kt`

```kotlin
@Service
@Transactional(readOnly = true)
class SendConfirmationMailService(
    private val repository: MembershipRepository,
) : SendConfirmationMailUseCase {
    override fun sendConfirmationMail(membershipId: MembershipId) {
        val membership = repository.find(membershipId)
        log.info { "Sending confirmation mail to ${membership.email.value}" }
    }
}
```

### 5. `SendConfirmationMailDelegate` erstellen

**Neue Datei:** `adapter/inbound/cibseven/SendConfirmationMailDelegate.kt`

Orientiere dich an `SendWelcomeMailDelegate`. Der Delegate soll:
- `membershipId` aus der `DelegateExecution` lesen
- `useCase.sendConfirmationMail(...)` aufrufen

## Testen

```bash
curl -X POST http://localhost:8080/api/memberships \
  -H "Content-Type: application/json" \
  -d '{"email": "bob@miravelo.com", "name": "Bob", "age": 25}'
```

Im Cockpit:
1. Service Task `Send confirmation mail` läuft durch → Log: "Sending confirmation mail to bob@miravelo.com"
2. UserTask `Confirm subscription` erscheint in der Task List
3. Nach Abschluss → Service Task `Send Welcome Mail` läuft durch

## Referenzlösung

`../solutions/exercise-2/`
