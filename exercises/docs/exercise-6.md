# Aufgabe 6 – Call Activity

## Lernziele

- Call Activities modellieren und einsetzen
- Subprozesse in eigenständige Prozesse auslagern
- Datenaustausch zwischen Haupt- und Subprozess

## Hintergrund

Der Rejection-Pfad (Message Boundary → Ablehnung) soll in einen eigenen,
wiederverwendbaren Prozess ausgelagert werden – `membership-rejection.bpmn`.

### Neue Prozessstruktur

```
Hauptprozess (newsletter.bpmn):
  ...
  [event_confirmationRejected] → [CallActivity: handleRejection] → [Membership declined]

Subprozess (membership-rejection.bpmn):
  [Start] → [Revoke claim] → [End]
```

## Aufgaben

### 1. Subprozess `membership-rejection.bpmn` erstellen

Neue Datei: `src/main/resources/bpmn/membership-rejection.bpmn`

Referenz: `../models/task-6-call-activity-sub.bpmn`

Inhalt:
- Process ID: `handleRejection`
- Start Event → Service Task `Revoke claim` (`#{revokeClaimDelegate}`) → End Event

**Wichtig:** Prozessvariablen müssen zwischen Haupt- und Subprozess übergeben werden.
In der Call Activity: `In/Out Mappings` für `membershipId` konfigurieren.

### 2. Hauptprozess anpassen

Ersetze den direkten Pfad vom Message Boundary zur `serviceTask_revokeClaim`
durch eine **Call Activity**:

| Element | Typ | ID | Name | Konfiguration |
|---|---|---|---|---|
| Rejection-Handler | Call Activity | `callActivity_handleRejection` | Handle rejection | Called Element: `handleRejection` |

Referenz: `../models/task-6-call-activity-main.bpmn`

### 3. Variablen-Übergabe

In der Call Activity müssen Variablen übergeben werden:

**In-Mapping (Hauptprozess → Subprozess):**
- `membershipId` → `membershipId`

**Out-Mapping (Subprozess → Hauptprozess):** (optional, falls Ergebnis zurückgegeben werden soll)

### 4. `MembershipProcessApi` erweitern

Neues Subprozess-Modul hinzufügen:
```kotlin
object RejectionSubProcess {
    const val PROCESS_KEY: String = "handleRejection"
    const val SERVICE_TASK_REVOKE_CLAIM: String = "serviceTask_revokeClaim"
}
```

## Testen

```bash
MEMBERSHIP_ID=$(curl -s -X POST http://localhost:8080/api/memberships \
  -d '{"email": "grace@miravelo.com", "name": "Grace", "age": 24}')

# Ablehnung auslösen
curl -X POST http://localhost:8080/api/memberships/$MEMBERSHIP_ID/reject
```

Im Cockpit:
1. In **Process Instances**: Hauptprozess hat eine Call Activity
2. Eine separate Instanz von `handleRejection` erscheint kurz im Cockpit
3. Log: "Revoking claim for [membershipId]"

## Referenzlösung

`../solutions/exercise-6/`
