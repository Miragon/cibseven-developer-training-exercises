# Aufgabe 7 – Kompensation (SAGA-Muster)

## Ziel-Modell

![BPMN Modell der Aufgabe](assets/exercise-07.svg)

Referenz-Modell: `../models/exercise-07/newsletter.bpmn`.

## Lernziele

- Abgeschlossene Aktionen bei Abbruch automatisch rückgängig machen (BPMN-Kompensation)
- Compensation Boundary Event + Compensating End Event einsetzen
- SAGA-Muster in Prozessmodellen anwenden

## Hintergrund

### Kompensation

Erinnerst du dich an `revokeClaim`? In Aufgabe 6 haben wir den Service Task eingeführt, um den Membership-Platz bei Ablehnung oder Timeout wieder freizugeben – als expliziter Knoten direkt im Sequenzfluss. Damals: pragmatisch. Heute: nicht mehr zeitgemäß.

Statt den `revokeClaim` weiterhin als expliziten Service Task an jeden Decline-Pfad zu hängen, nutzen wir **BPMN-Kompensation**. Der Prozess deklariert einmal, *welche Aktion* (`revokeClaim`) *welche andere Aktion* (`claimMembership`) rückgängig macht. Sobald ein Compensating End Event erreicht wird, kümmert sich die Engine um den Rest.

**Warum ist das besser?** Bei mehreren abzusichernden Aktionen (z.B. claimMembership + sendConfirmationMail + Drittdienste) wächst der manuelle Kompensierungspfad schnell und wird schwer wartbar. Mit BPMN-Kompensation deklariert man die Zuordnung einmal – und die Engine übernimmt die Ausführung automatisch.

> **Kompensation ≠ Transaktions-Rollback.** Das technische Rollback aus dem Trainingskapitel *Async & Transaction Boundaries* (Topic 4) macht eine *einzelne, noch nicht committete* Engine-Transaktion rückgängig – automatisch, unsichtbar. Kompensation ist das fachliche Gegenstück: Sie macht *bereits committete* Arbeit über **neue** Transaktionen (den `revokeClaim`-Aufruf) wieder rückgängig, nachdem der Wait State längst passiert ist. Kurz: Rollback greift *vor* dem Commit, Kompensation *danach*.

```
serviceTask_claimMembership ──── [Kompensations-Boundary] ──── serviceTask_revokeClaim
                                                                (isForCompensation=true)
endEvent_membershipDeclined  →  [Compensating End Event]  →  Engine ruft revokeClaim auf
```

## Aufgaben

### 1. BPMN anpassen – Kompensation

Ändere `newsletter.bpmn` im Miragon BPMN Modeler:

- [ ] Compensation Boundary Event an `serviceTask_claimMembership` anhängen
- [ ] `serviceTask_revokeClaim` mit `isForCompensation=true` markieren und per Association mit dem Boundary verknüpfen
- [ ] Decline-Pfade (Timer-Abbruch **und** Message-Ablehnung) direkt mit `endEvent_membershipDeclined` verbinden (kein `revokeClaim` im Pfad)
- [ ] `endEvent_membershipDeclined` in Compensating End Event umwandeln

Referenz-Modell: `../models/exercise-07/newsletter.bpmn`

**Hinweis:** Der `RevokeClaimDelegate` bleibt unverändert – er wird jetzt nur anders aufgerufen (durch die BPMN-Engine statt via Sequenzfluss). Es muss kein Java-Code geändert werden.

**Kontrollfrage:** Warum funktioniert `RevokeClaimDelegate` ohne Änderungen weiter, obwohl er nicht mehr im Sequenzfluss liegt?

## Testen

**Kompensation prüfen – Timer-Ablauf:**
1. `POST /api/memberships` → Prozess startet, Claim wird gesetzt
2. Warte bis Timer-Boundary ausgelöst wird (z.B. Timer-Konfiguration auf 30s für den Test setzen)
3. Log zeigt `"Revoking membership claim"` – obwohl kein expliziter Service Task im Pfad
4. Cockpit: Prozessinstanz endet mit „Membership declined"

**Kompensation prüfen – Manuelle Ablehnung:**
1. `POST /api/memberships` → warte auf UserTask `confirmMembership`
2. Trigger Confirmation-Rejected-Message → `event_confirmationRejected` Boundary löst aus
3. Pfad geht direkt zu `endEvent_membershipDeclined` → Compensation feuert → `revokeClaim` automatisch ausgeführt

## Kontrolle

- [ ] Log zeigt `"Revoking membership claim"` beim Timer-Ablauf (ohne expliziten Task im Pfad)
- [ ] Log zeigt `"Revoking membership claim"` nach Ablehnung via Message
- [ ] Cockpit: Kompensations-Handler wird in der Prozesshistorie sichtbar
- [ ] `revokeClaim` ist **nirgendwo** mehr im Sequenzfluss – nur noch als Compensation Handler

## Prozess-Test erweitern

Fachlich ändert sich am Ergebnis der Decline-Pfade nichts – `serviceTask_revokeClaim`
läuft weiterhin, nur jetzt als Kompensations-Handler. Deine bestehenden Assertions
`hasPassed(Elements.SERVICE_TASK_REVOKE_CLAIM.getValue(), Elements.END_EVENT_MEMBERSHIP_DECLINED.getValue())`
und `verify(revokeClaimUseCase).revokeClaim(id)` gelten unverändert.

> **Weiterführendes:** BPMN-Kompensation eignet sich besonders für **SAGA-Muster** in Microservices: Jeder Schritt hat einen zugehörigen Kompensationsschritt. Bei Fehlern kompensiert die Engine alle bisher erfolgreichen Schritte in umgekehrter Reihenfolge. In CIB Seven kann Kompensation auch über Subprocess-Grenzen hinweg ausgelöst werden.

## Referenzlösung

`../solutions/exercise-07/`

---

➡️ [Weiter zu Aufgabe 8](exercise-08.md)
