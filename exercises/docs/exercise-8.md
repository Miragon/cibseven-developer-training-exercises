# Aufgabe 8 – Kompensation (SAGA-Muster)

## Lernziel

Du lernst das BPMN-Kompensationsmuster kennen: Wie man bereits abgeschlossene Aktionen bei einem Fehler oder Abbruch automatisch rückgängig macht, ohne explizite Rollback-Logik in den Sequenzfluss einzubauen.

## Hintergrund: Was ist BPMN-Kompensation?

Stell dir vor, eine Membership wurde erfolgreich reserviert (`claimMembership`). Danach läuft der Timer ab oder der Benutzer lehnt die Bestätigung ab. Das Membership-Slot muss wieder freigegeben werden (`revokeClaim`).

**Bisherige Lösung (Aufgabe 4–7):** `revokeClaim` war ein expliziter Service Task im Sequenzfluss nach dem Timer.

**Problem:** Bei mehreren abzusichernden Aktionen (z. B. claimMembership + sendConfirmationMail + Drittdienste) wächst der Kompensierungspfad schnell und ist schwer wartbar.

**BPMN-Kompensation:** Der Prozess deklariert *welche Aktion* (`revokeClaim`) *welche andere Aktion* (`claimMembership`) rückgängig macht. Die BPMN-Engine übernimmt die Ausführung automatisch, sobald ein Compensating End Event erreicht wird.

```
serviceTask_claimMembership ──── [Kompensations-Boundary] ──── serviceTask_revokeClaim
                                                                (isForCompensation=true)
endEvent_membershipDeclined  →  [Compensating End Event]  →  Engine ruft revokeClaim auf
```

## BPMN-Änderungen

### Hauptprozess (`newsletter.bpmn`)

1. **Kompensations-Boundary Event** an `serviceTask_claimMembership` anhängen
   - Typ: Compensation Boundary Event
   - Mit Association verbinden zu: `serviceTask_revokeClaim`

2. **`serviceTask_revokeClaim` als Compensation Handler markieren**
   - `isForCompensation="true"` setzen
   - Task liegt **nicht** auf einem Sequenzfluss (kein Incoming/Outgoing)

3. **Timer-Ablaufpfad vereinfachen**
   - Vorher: `boundary_timerExpiry` → `serviceTask_revokeClaim` → `endEvent_membershipDeclined`
   - **Nachher:** `boundary_timerExpiry` → `endEvent_membershipDeclined` (direkt)
   - `revokeClaim` entfernen aus dem Timer-Pfad (die Kompensation übernimmt es)

4. **`endEvent_membershipDeclined` in Compensating End Event umwandeln**
   - Typ: Compensating End Event (der „Ring mit Pfeil"-Marker)
   - Beide Pfade (Timer und Call Activity) münden in dieses Event

### Sub-Prozess `handleRejection` (`membership-rejection.bpmn`)

- **`serviceTask_revokeClaim` entfernen** – der Hauptprozess kompensiert via Boundary Event
- Prozessfluss: Start → `businessRuleTask_categorizeApplicant` → Gateway → UserTask / `sendRejectionMail` → End

## Aufgaben

### 1. BPMN modellieren

Ändere `newsletter.bpmn` im Camunda Modeler:

- [ ] Compensation Boundary Event an `serviceTask_claimMembership` anhängen
- [ ] `serviceTask_revokeClaim` mit `isForCompensation=true` markieren und per Association verknüpfen
- [ ] Timer-Ablaufpfad: `boundary_timerExpiry` direkt mit `endEvent_membershipDeclined` verbinden
- [ ] `endEvent_membershipDeclined` in Compensating End Event umwandeln
- [ ] `membership-rejection.bpmn`: `serviceTask_revokeClaim` entfernen

### 2. Code anpassen

Der `RevokeClaimDelegate` bleibt unverändert – er wird jetzt nur anders aufgerufen (durch die BPMN-Engine statt via Sequenzfluss). Kein Kotlin-Code muss geändert werden.

**Kontrollfrage:** Warum funktioniert `RevokeClaimDelegate` ohne Änderungen weiter, obwohl er nicht mehr im Sequenzfluss liegt?

### 3. Verhalten testen

**Szenario A – Timer-Ablauf:**
1. `POST /api/memberships` → Prozess startet, Claim wird gesetzt
2. Warte bis Timer-Boundary ausgelöst wird (z. B. Timer-Konfiguration auf 30s für den Test setzen)
3. Überprüfe im Log: `revokeClaim` wurde aufgerufen (obwohl kein expliziter Service Task im Pfad)
4. Cockpit: Prozessinstanz endet mit „Membership declined"

**Szenario B – Manuelle Ablehnung:**
1. `POST /api/memberships` → warte auf UserTask `confirmSubscription`
2. `POST /api/memberships/{id}/reject` → Message Boundary löst aus
3. `handleRejection`-Prozess startet (Call Activity)
4. Nach Abschluss: Compensating End Event feuert → `revokeClaim` automatisch ausgeführt

## Kontrolle

- [ ] Log zeigt `"Revoking membership claim"` beim Timer-Ablauf (ohne expliziten Task im Pfad)
- [ ] Log zeigt `"Revoking membership claim"` nach Ablehnung via REST-Endpoint
- [ ] Cockpit: Kompensations-Handler wird in der Prozesshistorie sichtbar
- [ ] `handleRejection`-Sub-Prozess enthält **keinen** `revokeClaim`-Task mehr

## Referenzlösung

`solutions/exercise-8/`

## Weiterführendes

- BPMN-Kompensation eignet sich besonders für **SAGA-Muster** in Microservices: Jeder Schritt hat einen zugehörigen Kompensationsschritt. Bei Fehlern kompensiert die Engine alle bisher erfolgreichen Schritte in umgekehrter Reihenfolge.
- In CIB Seven kann Kompensation auch über Subprocess-Grenzen hinweg ausgelöst werden.
