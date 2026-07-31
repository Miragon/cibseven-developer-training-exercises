# Aufgabe 8 – Call Activity & DMN

> **Voraussetzung:** Aufgabe 7 (Kompensation) ist abgeschlossen. Der Hauptprozess kennt bereits die Compensation-Boundary auf `serviceTask_claimMembership`.

## Ziel-Modell

Hauptprozess:

![BPMN Hauptprozess](assets/exercise-08-main.svg)

Sub-Prozess `handleRejection`:

![BPMN Sub-Prozess](assets/exercise-08-sub.svg)

## Lernziele

- Call Activities modellieren und einsetzen
- Subprozesse in eigenständige Prozesse auslagern
- Datenaustausch zwischen Haupt- und Subprozess (Variable Mappings)
- DMN-Entscheidungstabellen modellieren und einbinden
- Business Rule Tasks in BPMN verwenden
- User Tasks für manuelle Eingriffe basierend auf DMN-Ergebnissen

## Hintergrund

Nach Aufgabe 7 läuft die Kompensation sauber: Wird ein Membership abgelehnt, kümmert sich die Engine via `serviceTask_revokeClaim`. Aber das ist erst der Anfang.

Miravelo hat eine wichtige Erkenntnis gewonnen: Einige dieser „Crisis-Aspiranten" im Alter von 21–30 sind viel zu wertvoll, um sie einfach ziehen zu lassen. Die verdienen gut, sind mitten in ihrer Quarterlife Crisis und suchen genau das, was Miravelo bietet. Die müssen wir nochmal kontaktieren!

Um den Hauptprozess nicht aufzublähen, lagern wir die gesamte Rejection-Behandlung in einen eigenen Prozess aus und rufen ihn über eine **Call Activity** auf. Die Compensation-Logik aus Aufgabe 7 bleibt im Hauptprozess – die Call Activity steht zwischen den Decline-Boundary-Events und dem Compensating End Event.

> In diesem Fall könnte man das auch in einem Embedded Subprocess lösen – aber wir wollen verschiedene BPMN-Elemente kennenlernen ;)

Nachdem die Call Activity steht, kommt der nächste Schritt: Wir wollen automatisch erkennen, welche abgelehnten Bewerber besonders wertvoll sind. Die „Quarterlife-Crisis"-Zielgruppe (21–29 Jahre) soll per **DMN-Entscheidungstabelle** identifiziert werden. Wenn jemand als „high value" eingestuft wird, soll ein Mitarbeiter persönlich Kontakt aufnehmen – per **User Task**.

### Prozessstruktur

```
Hauptprozess (newsletter.bpmn):
  ...
  [boundary_timer | event_confirmationRejected]
        ↓
  [CallActivity: handleRejection]
        ↓
  [Compensating End Event: Membership declined]
        ↓ (Engine löst Compensation aus)
  [serviceTask_revokeClaim]

Subprozess (membership-rejection.bpmn):
  [Start] → [Categorize applicant (DMN)] → [Is high value?]
                                                ↓ Yes              ↓ No
                                          [Contact personally]  [End: accepted]
                                           (User Task)
                                                ↓
                                          [End: tried to reaquire]
```

## Aufgaben

### 1. Subprozess `membership-rejection.bpmn` erstellen

Neue Datei: `src/main/resources/bpmn/membership-rejection.bpmn`

Referenz: `../models/exercise-08/membership-rejection.bpmn`

Struktur:
- Process ID: `handleRejection`
- Start Event → Business Rule Task `Categorize applicant` → Exclusive Gateway → User Task `Contact personally` (Yes-Pfad) → End Event "Tried to reacquire"
- Default-Pfad (No): direkt → End Event "Accept rejection"

### 2. Hauptprozess anpassen

Ersetze die direkten Decline-Pfade aus Aufgabe 7 durch eine **Call Activity**:

| Element | Typ | ID | Name | Konfiguration |
|---|---|---|---|---|
| Rejection-Handler | Call Activity | `callActivity_handleRejection` | Handle rejection | Called Element: `handleRejection` |

- Eingehende Flows: `timer_abortAfter3HalfDays`, `event_confirmationRejected` Boundary
- Ausgehender Flow: → `endEvent_membershipDeclined` (Compensating End Event aus Aufgabe 7)

Die Compensation aus Aufgabe 7 bleibt unangetastet. Nach Rückkehr aus der Call Activity feuert das Compensating End Event und die Engine ruft `serviceTask_revokeClaim` auf.

Referenz: `../models/exercise-08/newsletter.bpmn`

### 3. Variablen-Übergabe konfigurieren

In der Call Activity müssen Variablen übergeben werden:

**In-Mapping (Hauptprozess → Subprozess):**
- `membershipId` → `membershipId`
- `age` → `age` (wird für die DMN-Entscheidung benötigt)

**Out-Mapping (Subprozess → Hauptprozess):** (optional, falls Ergebnis zurückgegeben werden soll)

### 4. DMN-Entscheidungstabelle einbinden

Kopiere die Referenz-DMN ins Projekt:

```bash
cp ../models/exercise-08/categorize-applicant.dmn src/main/resources/dmn/categorize-applicant.dmn
```

Inhalt der DMN-Tabelle:
- **Decision ID:** `categorizeApplicant`
- **Input:** `age` (Integer)
- **Output:** `isHighValue` (Boolean)
- **Regel:** Alter zwischen 21 und 29 → `true` (Quarter-Life-Crisis!), sonst `false`

### 5. Business Rule Task im Subprozess konfigurieren

| Element | Typ | ID | Name | Konfiguration |
|---|---|---|---|---|
| Kategorisierung | Business Rule Task | `businessRuleTask_categorizeApplicant` | Categorize applicant | Decision Ref: `categorizeApplicant`, Result Variable: `isHighValue`, Map Decision Result: `singleEntry` |
| VIP-Check | Exclusive Gateway | `gateway_highValue` | High value? | Default-Flow: No-Pfad |
| Persönlicher Kontakt | User Task | `userTask_writeRegretMail` | Write an email expressing regret | `asyncAfter=true` |

Gateway-Bedingungen:
- Yes-Pfad: `${isHighValue}`
- No-Pfad: Default

## Testen

**Call Activity prüfen (einfache Ablehnung, Alter außerhalb 21–29):**
```bash
MEMBERSHIP_ID=$(curl -s -X POST http://localhost:8080/api/memberships \
  -d '{"email": "grace@miravelo.com", "name": "Grace", "age": 35}' | jq -r .id)

# Confirmation-Rejected-Message triggern
curl -X POST http://localhost:8080/api/memberships/$MEMBERSHIP_ID/reject
```

Im Cockpit:
1. Hauptprozess hat eine Call Activity-Aufrufstelle
2. Separate Instanz von `handleRejection` läuft kurz durch
3. DMN evaluiert → `isHighValue = false` → direkt zu „Accept rejection" End Event
4. Rückkehr in Hauptprozess → Compensating End Event → Log: „Revoking claim for [membershipId]"

**VIP-Bewerber (Alter 21–29):**
```bash
MEMBERSHIP_ID=$(curl -s -X POST http://localhost:8080/api/memberships \
  -d '{"email": "hanna@miravelo.com", "name": "Hanna", "age": 25}' | jq -r .id)

curl -X POST http://localhost:8080/api/memberships/$MEMBERSHIP_ID/reject
```

Im Cockpit:
1. Im `handleRejection`-Subprozess → DMN → `isHighValue = true`
2. **User Task „Write email expressing regret"** erscheint in der Task List
3. Mitarbeiter füllt Notiz aus und schließt Task ab
4. Sub-Prozess endet → Hauptprozess → Compensation triggert `revokeClaim`

## Kontrolle

- [ ] Sub-Prozess `handleRejection` ist als eigene Datei modelliert und im Cockpit als separate Process Definition deployed
- [ ] Call Activity übergibt `membershipId` und `age` per In-Mapping
- [ ] DMN-Tabelle ist in `src/main/resources/dmn/` und wird beim Deployment registriert
- [ ] Bei Alter 21–29: User Task erscheint; bei anderem Alter: direkt zum End Event
- [ ] Nach Sub-Prozess-Rückkehr: Compensation aus Aufgabe 7 löst `revokeClaim` aus

## Prozess-Test erweitern

Die Decline-Behandlung liegt jetzt in der Call Activity `callActivity_handleRejection`, die per DM
`categorizeApplicant` (nach `age`) entscheidet. Erweitere den Test um beide DMN-Zweige:

- **Low-Value-Bewerber** (z. B. `age = 40`): Nach Timer-/Message-Abbruch läuft die Call Activity
  synchron durch (Accept Rejection), danach greift die Kompensation. Prüfe
  `hasPassed(SubscribeNewsletterProcessApi.Elements.CALL_ACTIVITY_HANDLE_REJECTION.getValue(), SubscribeNewsletterProcessApi.Elements.SERVICE_TASK_REVOKE_CLAIM.getValue(), SubscribeNewsletterProcessApi.Elements.END_EVENT_MEMBERSHIP_DECLINED.getValue())`.
- **High-Value-Bewerber** (`age` zwischen 21 und 29): Der aufgerufene Prozess wartet an
  `userTask_writeRegretMail`. Weil das Element im **aufgerufenen** Prozess liegt, kommt seine Konstante
  aus der zweiten generierten API: hole die Aufgabe über
  `taskDefinitionKey(HandleRejectionProcessApi.Elements.USER_TASK_WRITE_REGRET_MAIL.getValue())`,
  schließe sie ab, treibe weiter und prüfe denselben Abschluss.

> Assertions wie `hasPassed(...)` auf der Hauptinstanz sehen nur deren Aktivitäten (u. a. die Call
> Activity selbst) – die internen Schritte des aufgerufenen Prozesses laufen in einer eigenen Instanz.

## Referenzlösung

`../solutions/exercise-08/`

---

➡️ [Weiter zu Aufgabe 9](exercise-09.md)
