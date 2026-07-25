# Aufgabe 7 – Boundary Events & Subprozesse

## Ziel-Modell

![BPMN Modell der Aufgabe](assets/exercise-07.svg)

## Lernziele

- Subprozesse (Subprocesses) modellieren
- Non-interrupting Timer Boundary Events (täglich wiederholen)
- Interrupting Timer Boundary Events (Timeout → Abbruch)
- Message Boundary Events (nutzerinitiierter Abbruch)

## Hintergrund

Miravelo stellt fest: Viele Bewerber bestätigen ihre Membership nie.
Das kostet wertvolle Plätze! Neue Anforderungen:

1. **Täglich** eine Erinnerungsmail senden (non-interrupting Timer)
2. Nach **3,5 Tagen** ohne Bestätigung → Membership automatisch abbrechen (interrupting Timer)
3. Nutzer kann Bewerbung selbst **ablehnen** (Message Boundary)

### Neuer Prozessablauf

```
[Claim membership] → [Has empty spots?]
                            ↓ Yes
              ┌─────────────────────────────┐
              │  Confirm Membership         │
              │  [Send confirmation mail]   │
              │  [Confirm membership]       │ ←── Timer (täglich): Erinnerungsmail
              └─────────────────────────────┘
                      ↑ Timer (3.5 Tage): Abbruch
                      ↑ Message: Ablehnung durch Nutzer
                            ↓ Confirmed
              ⬦ ─╱ [Send Welcome Mail] ╲─ ⬦ → [Membership activated]
              Fork ╲ [Notify community] ╱ Join   (Parallel Gateway aus Aufgabe 6)
```

## Aufgaben

### 1. BPMN erweitern

Erstelle den Prozess nach `../models/exercise-07/newsletter.bpmn`.

**Neuer Subprozess** `subProcess_confirmMembership`:
- Enthält: `serviceTask_sendConfirmationMail` + `userTask_confirmMembership`

**Boundary Events am Subprozess:**

| Element | Typ | ID | Name | Konfiguration |
|---|---|---|---|---|
| Täglich | Non-Interrupting Timer | `timer_resendEveryDay` | Every day | Duration: `PT1M` (1 Minute, für Tests) |
| Timeout | Interrupting Timer | `timer_abortAfter3HalfDays` | After 3½ days | Duration: `PT3M` (3 Minuten, für Tests) |
| Ablehnung | Interrupting Message | `event_confirmationRejected` | Confirmation rejected | Message: `Message_ConfirmationRejected` |

**Neue Service Tasks:**

| Element | ID | Name | Delegate |
|---|---|---|---|
| Erinnerungsmail | `serviceTask_reSendConfirmationMail` | Re-Send confirmation mail | `#{reSendConfirmationMailDelegate}` |
| Claim freigeben | `serviceTask_revokeClaim` | Revoke claim | `#{revokeClaimDelegate}` |

**Neue End Events:**

| ID | Name |
|---|---|
| `endEvent_membershipDeclined` | Membership declined |
| `endEvent_membershipActivated` | Membership activated |

### 2. Neue Use Cases & Delegates implementieren

**`ReSendConfirmationMailUseCase`** / **`ReSendConfirmationMailService`**:
- Loggt "Re-sending confirmation mail to [email]"

**`RevokeClaimUseCase`** / **`RevokeClaimService`**:
- Loggt "Revoking claim for [membershipId]" 
- Gibt den Kapazitäts-Slot wieder frei (Counter dekrementieren)

**`RevokeClaimDelegate`** / **`ReSendConfirmationMailDelegate`**: analog zu bisherigen Delegates

### 3. Message-Boundary korrelieren

Der Message Boundary `Message_ConfirmationRejected` wird von außen ausgelöst.
Füge einen REST-Endpoint hinzu:

```
POST /api/memberships/{membershipId}/reject
```

Implementiere die Korrelation in `MembershipProcessAdapter`: Verwende `runtimeService.createMessageCorrelation(...)` mit dem Message-Namen aus dem BPMN-Modell und filtere auf die Prozessvariable `membershipId`.

> Async-Continuations (siehe Aufgabe 3): Setze `asyncAfter` zusätzlich an allen Boundary Events (`timer_resendEveryDay`, `timer_abortAfter3HalfDays`, `event_confirmationRejected`).

## Testen

```bash
# Membership starten
MEMBERSHIP_ID=$(curl -s -X POST http://localhost:8080/api/memberships \
  -d '{"email": "eve@miravelo.com", "name": "Eve", "age": 26}')

# Nach ~1 Minute: Erinnerungsmail im Log

# Ablehnung senden
curl -X POST http://localhost:8080/api/memberships/$MEMBERSHIP_ID/reject

# Nach ~3 Minuten ohne Bestätigung: Timeout-Abbruch
```

## Prozess-Test erweitern

Dein Prozess-Test aus [Aufgabe 5](exercise-05.md) deckt bisher nur Happy Path und Ablehnung ab.
Jetzt sind drei neue Pfade dazugekommen – ergänze für jeden einen Test:

> Seit Aufgabe 5 referenzierst du Element-IDs über die generierte `SubscribeNewsletterProcessApi` statt
> über Strings. Die neuen Elemente (Timer, Compensation) tauchen nach dem nächsten `generate-sources`
> automatisch als `Elements.*`-Konstanten auf.

- **Abbruch-Timer (interrupting):** Warte am User Task, feuere den Timer mit dem neuen Helfer
  `fireTimer(processEngine, Elements.TIMER_ABORT_AFTER_3_HALF_DAYS.getValue())`, treibe weiter und prüfe
  `hasPassed(Elements.SERVICE_TASK_REVOKE_CLAIM.getValue(), Elements.END_EVENT_MEMBERSHIP_DECLINED.getValue())`.
  Mocke dafür `RevokeClaimUseCase`.
- **Reject-Message:** Statt des Timers `membershipProcess.rejectMembership(id)` aufrufen – gleicher
  Ausgang (`revokeClaim` → declined).
- **Resend-Timer (non-interrupting):** `fireTimer(..., Elements.TIMER_RESEND_EVERY_DAY.getValue())`, dann prüfen, dass
  `reSendConfirmationMailUseCase.reSendConfirmationMail(id)` ein **zweites** Mal lief und der Prozess
  weiter am User Task wartet. Mocke `ReSendConfirmationMailUseCase`.

Den `fireTimer`-Helfer (Timer-Job direkt ausführen, unabhängig vom Fälligkeitsdatum) findest du in
`ProcessEngineTestUtils`.

## Referenzlösung

`../solutions/exercise-07/`

---

➡️ [Weiter zu Aufgabe 8](exercise-08.md)
