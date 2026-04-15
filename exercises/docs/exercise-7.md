# Aufgabe 7 – DMN-Kategorisierung & User Task für VIP-Kunden

## Lernziele

- DMN-Entscheidungstabellen modellieren und einbinden
- Business Rule Tasks in BPMN verwenden
- User Tasks für manuelle Eingriffe in komplexen Szenarien nutzen
- Kombiniertes Szenario: DMN-Ergebnis bestimmt Prozessfluss

## Hintergrund

Miravelo hat festgestellt: Einige abgebrochene Bewerber sind besonders wertvoll
(z.B. Influencer zwischen 21 und 29 Jahren – die "Quarter-Life-Crisis"-Zielgruppe 🎯).

Bei einem Abbruch soll das System automatisch prüfen, ob der Bewerber "high value" ist.
Wenn ja, soll ein Mitarbeiter manuell eingreifen und den Bewerber persönlich kontaktieren.

### Erweiterter Rejection-Subprozess

```
[handleRejection Subprozess]
        ↓
[Categorize applicant]     ← Business Rule Task (DMN)
        ↓
[Is high value?]           ← Exclusive Gateway
   ↓ Yes              ↓ No
[Contact personally]  [Revoke claim]
 (User Task)               ↓
        ↓             [End]
[Revoke claim]
        ↓
[End]
```

## Aufgaben

### 1. DMN-Entscheidungstabelle einbinden

Kopiere die Referenz-DMN in das Projekt:

```bash
cp ../models/categorize-applicant.dmn src/main/resources/bpmn/categorize-applicant.dmn
```

Inhalt der DMN-Tabelle:
- **Decision ID:** `categorizeApplicant`
- **Input:** `age` (Integer)
- **Output:** `isHighValue` (Boolean)
- **Regel:** Alter zwischen 21 und 29 → `true` (Quarter-Life-Crisis!), sonst `false`

### 2. Business Rule Task im Subprozess hinzufügen

Erweitere `membership-rejection.bpmn`:

| Element | Typ | ID | Name | Konfiguration |
|---|---|---|---|---|
| Kategorisierung | Business Rule Task | `businessRuleTask_categorizeApplicant` | Categorize applicant | Decision Ref: `categorizeApplicant`, Result Variable: `isHighValue` |
| VIP-Check | Exclusive Gateway | `gateway_isHighValue` | Is high value? | – |
| Persönlicher Kontakt | User Task | `userTask_contactPersonally` | Contact applicant personally | – |

**Formular-Feld am User Task:**
- `contactNote` (String) – Notiz für die persönliche Kontaktaufnahme

**Gateway-Bedingungen:**
- Ja-Pfad: `${isHighValue == true}`
- Nein-Pfad (Default): direkt zu `Revoke claim`

### 3. Variablen-Übergabe für DMN

Der Business Rule Task braucht die Variable `age` als Input.
Stelle sicher, dass `age` in den Subprozess übergeben wird (In-Mapping in Call Activity).

### 4. `MembershipProcessApi` erweitern

```kotlin
object RejectionSubProcess {
    // ... bestehende Konstanten ...
    const val BUSINESS_RULE_TASK_CATEGORIZE: String = "businessRuleTask_categorizeApplicant"
    const val GATEWAY_IS_HIGH_VALUE: String = "gateway_isHighValue"
    const val USER_TASK_CONTACT_PERSONALLY: String = "userTask_contactPersonally"
}

object Variables {
    // ... bestehende Variablen ...
    const val IS_HIGH_VALUE: String = "isHighValue"
}
```

## Testen

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

**Normaler Bewerber (Alter außerhalb 21-29):**
```bash
MEMBERSHIP_ID=$(curl -s -X POST http://localhost:8080/api/memberships \
  -d '{"email": "ivan@miravelo.com", "name": "Ivan", "age": 35}')

curl -X POST http://localhost:8080/api/memberships/$MEMBERSHIP_ID/reject
# Direkt zu Revoke claim – kein User Task
```

## Referenzlösung

`../solutions/exercise-7/`
