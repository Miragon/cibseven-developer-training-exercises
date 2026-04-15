# Aufgabe 2 – Bestätigungs-Mail

## Lernziele

- Einen bestehenden Prozess in Camunda Modeler erweitern
- Mehrere Service Tasks implementieren
- Sequenzielle Flows mit User Tasks kombinieren

## Hintergrund

Über Nacht haben sich 500 Leute angemeldet. Fünfhundert. Für einen Newsletter über einen
Berliner Lifestyle-Meetup.

Das Team starrt auf die Datenbank und beginnt, Fragen zu stellen:

- Sind das echte E-Mail-Adressen?
- Wer ist überhaupt diese `noreply@throwaway.xyz`?
- Warten wir kurz – *sind wir noch ein Newsletter?*

Zweites Problem, größeres Problem: Irgendwer hat `admin@miravelo.com` eingetragen. Als Witz.
Wahrscheinlich.

Das Team beschließt: Wir bauen einen **Bestätigungsschritt**. Erst Mail bestätigen,
dann Welcome Mail. Klassisches Double-Opt-In. Und während wir dabei sind – vielleicht
sind wir nicht mehr nur ein Newsletter. Vielleicht sind wir etwas... Exklusiveres.

> *„500 Sign-ups. Das ist entweder viral oder ein Bot-Angriff."*
> — CTO, beim zweiten Kaffee

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
