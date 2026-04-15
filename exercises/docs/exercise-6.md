# Aufgabe 6 – Call Activity & DMN

## Lernziele

- Call Activities modellieren und einsetzen
- Subprozesse in eigenständige Prozesse auslagern
- Datenaustausch zwischen Haupt- und Subprozess (Variable Mappings)
- DMN-Entscheidungstabellen modellieren und einbinden
- Business Rule Tasks in BPMN verwenden
- User Tasks für manuelle Eingriffe basierend auf DMN-Ergebnissen

## Hintergrund

Wenn ein Nutzer seine Membership ablehnt, haben wir bisher einfach den Platz wieder freigegeben – `revokeClaim` und fertig. Aber halt! Nicht so schnell.

Miravelo hat eine wichtige Erkenntnis gewonnen: Einige dieser "Crisis-Aspiranten" im Alter von 21–30 sind viel zu wertvoll, um sie einfach ziehen zu lassen. Die verdienen gut, sind mitten in ihrer Quarterlife Crisis und suchen genau das, was Miravelo bietet. Die müssen wir nochmal kontaktieren!

Um den Hauptprozess nicht aufzublähen, lagern wir die gesamte Rejection-Behandlung in einen eigenen Prozess aus und rufen ihn über eine **Call Activity** auf.

> In diesem Fall könnte man das auch in einem Embedded Subprocess lösen – aber wir wollen verschiedene BPMN-Elemente kennenlernen ;)

Nachdem die Call Activity steht, kommt der nächste Schritt: Wir wollen automatisch erkennen, welche abgelehnten Bewerber besonders wertvoll sind. Die "Quarterlife-Crisis"-Zielgruppe (21–29 Jahre) soll per **DMN-Entscheidungstabelle** identifiziert werden. Wenn jemand als "high value" eingestuft wird, soll ein Mitarbeiter persönlich Kontakt aufnehmen – per **User Task**.

### Prozessstruktur

```
Hauptprozess (newsletter.bpmn):
  ...
  [event_confirmationRejected] → [CallActivity: handleRejection] → [Membership declined]

Subprozess (membership-rejection.bpmn):
  [Start] → [Categorize applicant] → [Is high value?]
                                          ↓ Yes              ↓ No
                                    [Contact personally]  [Revoke claim]
                                     (User Task)               ↓
                                          ↓             [End]
                                    [Revoke claim]
                                          ↓
                                    [End]
```

## Aufgaben

### 1. Subprozess `membership-rejection.bpmn` erstellen

Neue Datei: `src/main/resources/bpmn/membership-rejection.bpmn`

Referenz: `../models/task-6-call-activity-sub.bpmn`

Erstelle zunächst einen einfachen Subprozess:
- Process ID: `handleRejection`
- Start Event → Service Task `Revoke claim` (`#{revokeClaimDelegate}`) → End Event

### 2. Hauptprozess anpassen

Ersetze den direkten Pfad vom Message Boundary zur `serviceTask_revokeClaim`
durch eine **Call Activity**:

| Element | Typ | ID | Name | Konfiguration |
|---|---|---|---|---|
| Rejection-Handler | Call Activity | `callActivity_handleRejection` | Handle rejection | Called Element: `handleRejection` |

Referenz: `../models/task-6-call-activity-main.bpmn`

### 3. Variablen-Übergabe konfigurieren

In der Call Activity müssen Variablen übergeben werden:

**In-Mapping (Hauptprozess → Subprozess):**
- `membershipId` → `membershipId`
- `age` → `age` (wird später für die DMN-Entscheidung benötigt)

**Out-Mapping (Subprozess → Hauptprozess):** (optional, falls Ergebnis zurückgegeben werden soll)

### 4. DMN-Entscheidungstabelle einbinden

Kopiere die Referenz-DMN in das Projekt:

```bash
cp ../models/categorize-applicant.dmn src/main/resources/bpmn/categorize-applicant.dmn
```

Inhalt der DMN-Tabelle:
- **Decision ID:** `categorizeApplicant`
- **Input:** `age` (Integer)
- **Output:** `isHighValue` (Boolean)
- **Regel:** Alter zwischen 21 und 29 → `true` (Quarter-Life-Crisis!), sonst `false`

### 5. Subprozess um DMN-Kategorisierung erweitern

Erweitere `membership-rejection.bpmn` um einen Business Rule Task und ein Gateway:

| Element | Typ | ID | Name | Konfiguration |
|---|---|---|---|---|
| Kategorisierung | Business Rule Task | `businessRuleTask_categorizeApplicant` | Categorize applicant | Decision Ref: `categorizeApplicant`, Result Variable: `isHighValue` |
| VIP-Check | Exclusive Gateway | `gateway_isHighValue` | Is high value? | – |
| Persönlicher Kontakt | User Task | `userTask_contactPersonally` | Contact applicant personally | – |

Der Subprozess sieht nun so aus:
- Start → `Categorize applicant` (Business Rule Task) → `Is high value?` (Gateway)
  - Ja-Pfad (`${isHighValue == true}`): → `Contact applicant personally` (User Task) → `Revoke claim` → End
  - Nein-Pfad (Default): → `Revoke claim` → End

**Formular-Feld am User Task:**
- `contactNote` (String) – Notiz für die persönliche Kontaktaufnahme

## Testen

**Call Activity prüfen (einfache Ablehnung):**
```bash
MEMBERSHIP_ID=$(curl -s -X POST http://localhost:8080/api/memberships \
  -d '{"email": "grace@miravelo.com", "name": "Grace", "age": 35}')

# Ablehnung auslösen
curl -X POST http://localhost:8080/api/memberships/$MEMBERSHIP_ID/reject
```

Im Cockpit:
1. In **Process Instances**: Hauptprozess hat eine Call Activity
2. Eine separate Instanz von `handleRejection` erscheint kurz im Cockpit
3. Log: "Revoking claim for [membershipId]"
4. Alter außerhalb 21–29 → direkt zu Revoke claim, kein User Task

**VIP-Bewerber (Alter 21-29):**
```bash
MEMBERSHIP_ID=$(curl -s -X POST http://localhost:8080/api/memberships \
  -d '{"email": "hanna@miravelo.com", "name": "Hanna", "age": 25}')

# Ablehnung auslösen
curl -X POST http://localhost:8080/api/memberships/$MEMBERSHIP_ID/reject
```

Im Cockpit:
1. Im `handleRejection`-Subprozess → DMN evaluiert → `isHighValue = true`
2. **User Task "Contact applicant personally"** erscheint in der Task List
3. Mitarbeiter füllt Kontakt-Notiz aus und schließt Task ab
4. Dann läuft `Revoke claim` durch

## Referenzlösung

`../solutions/exercise-6/`
