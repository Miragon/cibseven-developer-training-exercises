# Aufgabe 1 – Automatisierung des Prozesses

## Lernziele

- Hexagonale Architektur (Ports & Adapters) verstehen
- BPMN Service Task mit Java-Code verbinden (JavaDelegate-Pattern)
- Prozess über RuntimeService starten
- REST-Endpoint zum Starten des Prozesses implementieren

## Hintergrund

In Aufgabe 0 hast du den Prozess modelliert. Jetzt wird er technisch automatisiert:
Der Service Task `Send Welcome Mail` soll echten Java-Code ausführen.

Das Projekt folgt der hexagonalen Architektur:

```
POST /api/memberships
       ↓
MembershipController          (adapter/inbound/rest)
       ↓
RegisterMembershipUseCase     (application/port/inbound)
       ↓
RegisterMembershipService     (application/service)        ← TODO
       ↓
MembershipProcess.startProcess()  (application/port/outbound)
       ↓
MembershipProcessAdapter          (adapter/outbound/cibseven) ← TODO
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

## Aufgaben

### 1. `RegisterMembershipService` implementieren

**Datei:** `application/service/RegisterMembershipService.kt`

Ersetze das `TODO` mit folgender Logik:
1. Erstelle ein `Membership`-Objekt: `Membership(email = Email(command.email), name = Name(command.name), age = Age(command.age))`
2. Speichere es: `repository.save(membership)`
3. Starte den Prozess: `process.startProcess(membership)`
4. Gib `membership.id` zurück

### 2. `SendWelcomeMailService` implementieren

**Datei:** `application/service/SendWelcomeMailService.kt`

Ersetze das `TODO` mit einem Log-Statement:
```kotlin
log.info { "Sending welcome mail to ${membership.email.value}" }
```

### 3. `SendWelcomeMailDelegate` implementieren

**Datei:** `adapter/inbound/cibseven/SendWelcomeMailDelegate.kt`

Ersetze das `TODO` in `executeTask(execution)`:
```kotlin
val membershipId = execution.getVariable("membershipId") as String
log.debug { "Received task to send welcome mail for membership: $membershipId" }
useCase.sendWelcomeMail(MembershipId(UUID.fromString(membershipId)))
```

### 4. `MembershipProcessAdapter` implementieren

**Datei:** `adapter/outbound/cibseven/MembershipProcessAdapter.kt`

Ersetze das `TODO` in `startProcess(membership)`:
```kotlin
runtimeService.startProcessInstanceByKey(
    MembershipProcessApi.PROCESS_KEY,
    mapOf(
        MembershipProcessApi.Variables.MEMBERSHIP_ID to membership.id.value.toString(),
        MembershipProcessApi.Variables.EMAIL to membership.email.value,
        MembershipProcessApi.Variables.NAME to membership.name.value,
        MembershipProcessApi.Variables.AGE to membership.age.value,
    )
)
```

## Testen

```bash
# Anwendung starten
../mvnw spring-boot:run

# Membership registrieren
curl -X POST http://localhost:8080/api/memberships \
  -H "Content-Type: application/json" \
  -d '{"email": "alice@miravelo.com", "name": "Alice", "age": 28}'
```

Danach im **Cockpit** unter http://localhost:8080/camunda:
- Unter **Processes** → eine Instanz von `Subscribe Newsletter` vorhanden
- UserTask `Fill out form` erscheint in **Task List**
- Nach Abschluss der UserTask → Service Task läuft durch → Log: "Sending welcome mail to alice@miravelo.com"

## Bonus: Prozesstest

Implementiere den Test in `src/test/kotlin/io/miragon/training/process/MembershipProcessTest.kt`.

## Referenzlösung

`../solutions/exercise-1/`
