# Aufgabe 6 – Boundary Events & Subprozesse

## Ziel-Modell

![BPMN Modell der Aufgabe](assets/exercise-06.svg)

## Lernziele

- Subprozesse (Subprocesses) modellieren
- Non-interrupting Timer Boundary Events (täglich wiederholen)
- Interrupting Timer Boundary Events (Timeout → Abbruch)
- Message Boundary Events (nutzerinitiierter Abbruch)
- Ein **Parallel Gateway** (Fork/Join) einsetzen und parallel zur Willkommens-Mail eine
  **Community-Benachrichtigung** verschicken (In-Engine-Delegate, der in einen Microsoft-Teams-Kanal postet)

## Hintergrund

Miravelo stellt fest: Viele Bewerber bestätigen ihre Membership nie.
Das kostet wertvolle Plätze! Neue Anforderungen:

1. **Täglich** eine Erinnerungsmail senden (non-interrupting Timer)
2. Nach **3,5 Tagen** ohne Bestätigung → Membership automatisch abbrechen (interrupting Timer)
3. Nutzer kann Bewerbung selbst **ablehnen** (Message Boundary)

Und wenn jemand es doch bis zur Aktivierung schafft, soll es die **ganze Community** mitbekommen:
parallel zur Willkommens-Mail geht eine Benachrichtigung in einen gemeinsamen **Microsoft-Teams-Kanal**.
Die beiden Dinge hängen nicht voneinander ab – deshalb modellieren wir sie mit einem **Parallel Gateway**.

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
              Fork ╲ [Notify community] ╱ Join
```

## Aufgaben

### 1. BPMN erweitern

Erstelle den Prozess nach `../models/exercise-06/newsletter.bpmn`.

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

> **Async-Continuations (siehe Aufgabe 4):** Setze `asyncAfter` zusätzlich an allen Boundary Events. Jedes Event bekommt so eine saubere TX-Grenze für das, was danach kommt:
> - `timer_resendEveryDay` (non-interrupting) → der Resend läuft in einer eigenen Transaktion und wiederholt sich unabhängig, ohne den wartenden Subprozess zu berühren.
> - `timer_abortAfter3HalfDays` und `event_confirmationRejected` (interrupting) → saubere Grenze **vor** dem Abbruch/der Kompensation (Aufgabe 7/8).
>
> Der neue Resend-Task `serviceTask_reSendConfirmationMail` ist ein externer Effekt und bekommt – wie in Aufgabe 4 – `asyncBefore`.

### 4. Parallel Gateway + Notify community

Auf dem Confirmed-Pfad – zwischen dem Ende des Subprozesses und dem Aktivierungs-End-Event – kommt
ein **Parallel Gateway (Fork)** hinzu, das zwei Zweige öffnet:

- Zweig A: das bestehende `serviceTask_sendWelcomeMail`
- Zweig B: einen neuen Service Task `serviceTask_notifyCommunity` (Name *Notify community*),
  gebunden an den Delegate `#{notifyCommunityDelegate}`

Ein zweites **Parallel Gateway (Join)** führt beide Zweige wieder zusammen, bevor das
`endEvent_membershipActivated` erreicht wird. Weil der Join **beide** Zweige abwartet, ist die
Membership erst „activated", wenn Mail **und** Benachrichtigung durch sind.

> **Transaktionsgrenze pro Zweig (Pflicht, siehe Aufgabe 4):** Ohne Marker liegen `sendWelcomeMail` und
> `notifyCommunity` in **einer** Transaktion. Scheitert der Teams-Webhook, rollt die Engine den Job
> zurück und wiederholt ihn – die Welcome-Mail wäre dann schon raus und würde **erneut** verschickt.
> Setze deshalb `asyncBefore` an **beide** Zweige (`serviceTask_sendWelcomeMail`,
> `serviceTask_notifyCommunity`), damit jeder externe Effekt einzeln committet und unabhängig retryt.

Die Benachrichtigung läuft komplett **in der Engine** (ein ganz normaler Delegate). Implementiere:

- `adapter/inbound/cibseven/NotifyCommunityDelegate` (analog zu `SendWelcomeMailDelegate`): liest
  `membershipId` und ruft den Use Case
- `application/port/inbound/NotifyCommunityUseCase` + `application/service/NotifyCommunityService`:
  lädt die Membership, baut eine `Notification` (Titel + Text) und reicht sie an den Out-Port weiter
- `application/port/outbound/NotificationPublisherOutPort` + `adapter/outbound/teams/MicrosoftTeamsMessagePublisher`:
  postet die Benachrichtigung als **Adaptive Card** in einen Teams-Kanal (Power-Automate-„Workflows"-Webhook).
  Der Adaptive-Card-Aufbau und der REST-Call sind Infrastruktur – schau dir die Referenzlösung an und
  übernimm sie; ein `adapter/config/RestClientConfig` stellt den `RestClient` bereit
- `domain/Notification` (record `title`, `text`)

Die Ziel-URL steht in `application.yaml`:

```yaml
notification:
  teams:
    # Echte URL per Umgebungsvariable TEAMS_WEBHOOK_URL – kein Secret ins Repo committen.
    webhook-url: ${TEAMS_WEBHOOK_URL:https://CHANGE-ME}
```

> 💡 Dass diese Teams-Anbindung mitten in der Prozess-Anwendung sitzt, ist bewusst noch nicht ideal –
> in **Aufgabe 9** lagern wir sie in einen eigenen Remote-Worker aus (Wiederverwendung + Isolation des
> Webhook-Secrets). Für jetzt reicht der In-Engine-Delegate.

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
> über Strings. Die neuen Elemente (Timer, Parallel Gateway) tauchen nach dem nächsten `generate-sources`
> automatisch als `Elements.*`-Konstanten auf. Der `notifyCommunity`-Zweig ist ein In-Engine-Delegate –
> mocke im Test `NotifyCommunityUseCase` (wie `SendWelcomeMailUseCase`), damit kein echter Teams-Call fliegt.

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

`../solutions/exercise-06/`

---

➡️ [Weiter zu Aufgabe 7](exercise-07.md)
