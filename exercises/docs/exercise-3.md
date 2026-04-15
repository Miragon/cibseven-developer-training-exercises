# Aufgabe 3 – Membership & Kapazitätsprüfung

## Lernziele

- Domain-Konzepte umbenennen (Refactoring)
- Exclusive Gateway modellieren und implementieren
- Neuen Service Task (Kapazitätsprüfung) hinzufügen
- Alternativen Prozessausgang implementieren

## Hintergrund

**Strategie-Meeting. Freitagnachmittag. Jemand hat Red Bull mitgebracht.**

Miravelo startet den **Miravelo Inner Circle** – eine limitierte, exklusive Membership
für echte Fans der Marke. Gravel Bike im Keller, Siebträger auf der Arbeitsplatte,
Halbmarathon im Kalender – du weißt, wen wir meinen.

Zehn Plätze. Zählt bis zehn. Das ist die Kapazität.

Warum zehn? Weil Knappheit Wert erzeugt. Weil FOMO ein Business-Modell ist. Weil irgendjemand
ein Buch über Luxusmarken gelesen hat und jetzt „Premium Positioning" in jeden Satz einbaut.

> *„Wir sind nicht exklusiv weil wir gut sind. Wir sind exklusiv weil wir nur zehn Plätze
> haben und der Counter in der Datenbank auf 10 steht."*
> — Ehrlichster Kommentar im Sprint Planning

Das Gute daran: Aus Prozesssicht brauchen wir ein **Gateway**. Der gnadenlose Türsteher im
Prozessfluss. Hat die Person einen Platz bekommen? Herzlichen Glückwunsch, weiter. Kein Platz?
Ablehnungsmail. Kein Einspruch. Das Gateway entscheidet.

Mit 27 eine Absage vom Fahrradladen des Vertrauens zu bekommen trifft anders. Aber das ist
jetzt das Problem der Bewerber, nicht deins.

### Neuer Prozessablauf

```
[Submit registration form]
         ↓
[Claim membership]         ← NEU (Service Task)
         ↓
[Has empty spots?]         ← NEU (Exclusive Gateway)
   ↓ Yes              ↓ No
[Send confirmation]   [Send rejection mail]  ← NEU
         ↓                    ↓
[Confirm membership]  [Membership rejected]  ← NEU End Event
         ↓
[Send Welcome Mail]
         ↓
[Membership confirmed]
```

## Aufgaben

### 1. BPMN komplett neu modellieren

Erstelle den Prozess nach dem Referenz-Modell `../models/task-3-gateway.bpmn`.

Neue Elemente:

| Element | Typ | ID | Name | Konfiguration |
|---|---|---|---|---|
| Claim | Service Task | `serviceTask_claimMembership` | Claim membership | `#{claimMembershipDelegate}` |
| Gateway | Exclusive Gateway | `gateway_hasEmptySpots` | Has empty spots? | Default-Flow: `Yes`-Pfad |
| Rejection Mail | Service Task | `serviceTask_sendRejectionMail` | Send rejection mail | `#{sendRejectionMailDelegate}` |
| Abgelehnt | End Event | `endEvent_membershipRejected` | Membership rejected | – |

**Gateway-Bedingung (Nein-Pfad):** `${!hasEmptySpots}`

### 2. Domain erweitern: `MembershipCapacity`

**Neue Datei:** `domain/MembershipCapacity.kt`

```kotlin
data class MembershipCapacity(
    val maxSpots: Int = 100,
    val claimedSpots: Int = 0,
) {
    val hasEmptySpots: Boolean get() = claimedSpots < maxSpots
    fun claim() = copy(claimedSpots = claimedSpots + 1)
}
```

### 3. Use Cases und Services erstellen

Erstelle nach dem bewährten Muster (analog zu Aufgabe 2):

- `ClaimMembershipUseCase` / `ClaimMembershipService`
  - Prüft Kapazität (einfacher Counter in Memory reicht)
  - Setzt Prozessvariable `hasEmptySpots` (via `DelegateExecution.setVariable(...)`)
- `SendRejectionMailUseCase` / `SendRejectionMailService`
  - Loggt "Sending rejection mail to [email]"

### 4. Delegates erstellen

- `ClaimMembershipDelegate`: Prüft Kapazität, setzt Variable `hasEmptySpots`
- `SendRejectionMailDelegate`: Liest `membershipId`, ruft Use Case auf

**Variable in Delegate setzen:**
```kotlin
val hasEmptySpots = membershipCapacityService.hasEmptySpots()
execution.setVariable("hasEmptySpots", hasEmptySpots)
```

### 5. `MembershipProcessApi` aktualisieren

Neue Konstanten für die neuen Elemente und die neue Variable `hasEmptySpots`.

## Testen

**Happy Path (Kapazität vorhanden):**
```bash
curl -X POST http://localhost:8080/api/memberships \
  -d '{"email": "carol@miravelo.com", "name": "Carol", "age": 27}'
```

**Rejection Path (Kapazität auf 0 setzen → Anwendungs-Config anpassen):**
```bash
# Setze maxSpots = 0 in der Konfiguration
curl -X POST http://localhost:8080/api/memberships \
  -d '{"email": "dave@miravelo.com", "name": "Dave", "age": 30}'
# Erwartetes Log: "Sending rejection mail to dave@miravelo.com"
```

## Referenzlösung

`../solutions/exercise-3/`
