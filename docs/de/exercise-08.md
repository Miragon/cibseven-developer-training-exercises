# Aufgabe 8 – Call Activity und DMN-Entscheidung

> **Voraussetzung:** Aufgabe 7 ist abgeschlossen – der Hauptprozess kennt die Compensation-Boundary an `serviceTask_claimMembership`.
> **Arbeitsverzeichnis:** `services/process-application`
> **Neu in dieser Aufgabe:** eigenständiger Prozess, Call Activity mit Variablen-Mapping, DMN-Entscheidungstabelle, Business Rule Task.

## Darum geht es

Die Kompensation läuft sauber: Wird eine Membership abgelehnt, gibt die Engine den Platz von
selbst wieder frei. Aber Miravelo hat eine Erkenntnis gewonnen.

Einige dieser Crisis-Aspiranten zwischen 21 und 29 sind viel zu wertvoll, um sie einfach
ziehen zu lassen. Sie verdienen gut, stecken mitten in ihrer Quarterlife-Crisis und suchen
genau das, was Miravelo bietet. Die muss jemand persönlich zurückholen.

Damit der Hauptprozess davon nicht aufgebläht wird, wandert die gesamte Ablehnungsbehandlung
in einen **eigenen Prozess**, den eine **Call Activity** aufruft. Wer die Zielgruppe ist,
entscheidet keine `if`-Kaskade im Code, sondern eine **DMN-Entscheidungstabelle** – die kann
der Fachbereich später selbst anfassen.

> Man könnte das auch mit einem eingebetteten Subprozess lösen. Wir nehmen die Call
> Activity, weil wir ihre Eigenheiten kennenlernen wollen: eigene Prozessdefinition, eigene
> Instanz, explizites Variablen-Mapping.

## Lernziele

Nach dieser Aufgabe kannst du

- einen Prozessteil in eine eigene Prozessdefinition auslagern,
- ihn über eine Call Activity aufrufen und Variablen per In-Mapping übergeben,
- eine DMN-Entscheidungstabelle modellieren, deployen und über einen Business Rule Task
  auswerten,
- das Ergebnis einer Entscheidung an einem Exclusive Gateway verzweigen,
- den Unterschied zwischen Haupt- und aufgerufener Prozessinstanz im Test berücksichtigen.

## Ziel-Modell

Hauptprozess:

![BPMN-Hauptprozess](../assets/exercise-08-main.svg)

Aufgerufener Prozess `handleRejection`:

![BPMN-Subprozess](../assets/exercise-08-sub.svg)

```
Hauptprozess (newsletter.bpmn):
  [timer_abortAfter3HalfDays | event_confirmationRejected]
                    ↓
     [Call Activity: handleRejection]
                    ↓
     (◉ Membership declined – Compensating End Event)
                    ↓
        Engine kompensiert → [Revoke claim]

Aufgerufener Prozess (membership-rejection.bpmn):
  (Start) → [Categorize applicant (DMN)] → ◇ [High value?]
                                            ↓ ja                ↓ nein (Default)
                                  [Write regret mail]     (◉ Accept rejection)
                                     (User Task)
                                            ↓
                                  (◉ Tried to reacquire applicant)
```

Referenzmodelle: `../../models/exercise-08/newsletter.bpmn`,
`../../models/exercise-08/membership-rejection.bpmn`,
`../../models/exercise-08/categorize-applicant.dmn`

## Aufgabe

### 1. DMN-Entscheidungstabelle modellieren

Der neue Ablehnungsprozess trifft eine fachliche Entscheidung: Wer von den abgelehnten
Aspiranten ist High Value und damit einen persönlichen Rückholversuch wert? Diese Entscheidung
modellierst du als DMN-Entscheidungstabelle – deine erste. So lernst du den DMN-Editor, die
Hit Policy und die FEEL-Range-Schreibweise kennen. Du hast zwei Wege:

- **Selbst modellieren (empfohlen):** Lege in einem DMN-Modeler die neue Datei
  `src/main/resources/dmn/categorize-applicant.dmn` an und baue die Tabelle nach der
  Spezifikation unten.
- **Fallback – fertiges Modell kopieren:** Wer den DMN-Editor überspringen will, kopiert das
  Referenzmodell ins Modul:

  ```bash
  cp models/exercise-08/categorize-applicant.dmn \
     services/process-application/src/main/resources/dmn/categorize-applicant.dmn
  ```

Die Spezifikation für den Selbst-modellieren-Weg – IDs und Typen müssen exakt stimmen, damit
der Business Rule Task in Schritt 2 die Entscheidung findet:

| Eigenschaft | Wert |
|---|---|
| Decision ID | `categorizeApplicant` |
| Hit Policy | `FIRST` |
| Input | `age` (integer) |
| Output | `isHighValue` (boolean) |
| Regel | Alter im Bereich `[21..29]` → `true`; Default `-` → `false` |

Die FEEL-Range `[21..29]` schließt beide Grenzen ein (21 und 29 gehören dazu). Alle `*.dmn`
unter `src/main/resources` werden wie die BPMN-Dateien beim Start automatisch deployt.

### 2. Prozess `membership-rejection.bpmn` modellieren

**Neue Datei:** `src/main/resources/bpmn/membership-rejection.bpmn`, Prozess-Key
`handleRejection`.

| Element | Typ | ID | Name | Konfiguration |
|---|---|---|---|---|
| Start | None Start Event | `startEvent_confirmationRejected` | Confirmation rejected | – |
| Kategorisierung | Business Rule Task | `serviceTask_categorizeApplicant` | Categorize applicant | Decision Ref `categorizeApplicant`, Result Variable `isHighValue`, Map Decision Result `singleEntry` |
| Verzweigung | Exclusive Gateway | `gateway_highValue` | High value? | Default-Flow: Nein-Pfad |
| Persönlicher Kontakt | User Task | `userTask_writeRegretMail` | Write an email expressing regret | `asyncAfter="true"` |
| Ende ja-Pfad | End Event | `endEvent_triedToReacquire` | Tried to reaquire applicant | – |
| Ende nein-Pfad | End Event | `endEvent_acceptRejection` | Accept rejection | – |

Bedingung am Ja-Pfad: `${isHighValue}`. Der Nein-Pfad ist der Default-Flow.

> Der Business Rule Task trägt im Referenzmodell das Präfix `serviceTask_` statt
> `businessRuleTask_`. Das ist gewachsen – übernimm die ID aus dem Referenzmodell, damit
> Doku, Modell und generierte Konstanten zusammenpassen.

### 3. Call Activity im Hauptprozess einsetzen

Im Hauptprozess ersetzt ein einziges Element alle bisherigen Abbruchschritte. Die Call
Activity verweist über ihr Attribut *Called Element* auf den Prozess-Key des aufgerufenen
Prozesses:

| Element | Typ | ID | Name | Konfiguration |
|---|---|---|---|---|
| Ablehnungsbehandlung | Call Activity | `callActivity_handleRejection` | Handle rejection | Called Element: `handleRejection` |

- Eingehende Flows: von `timer_abortAfter3HalfDays` und von `event_confirmationRejected`
- Ausgehender Flow: auf `endEvent_membershipDeclined` (das Compensating End Event aus
  Aufgabe 7)

Die Kompensation bleibt unangetastet: Nach der Rückkehr aus der Call Activity feuert das
Compensating End Event, und die Engine ruft `serviceTask_revokeClaim` auf.

### 4. Variablen übergeben

In-Mapping an der Call Activity (Hauptprozess → aufgerufener Prozess):

```xml
<bpmn:extensionElements>
  <camunda:in source="membershipId" target="membershipId" />
  <camunda:in source="age" target="age" />
</bpmn:extensionElements>
```

`age` ist die Eingabe der DMN-Entscheidung – ohne das Mapping läuft die Tabelle ins Leere.
Ein Out-Mapping brauchst du hier nicht: Der Hauptprozess verarbeitet kein Ergebnis.

### 5. Prozess-Test erweitern

Die Ablehnungsbehandlung liegt jetzt in der Call Activity. Ergänze beide DMN-Zweige:

- **Alter außerhalb 21–29** (zum Beispiel `40`): Nach Timeout oder Rückzug läuft die Call
  Activity ohne Wait State durch, danach greift die Kompensation. Prüfe
  `hasPassed(Elements.CALL_ACTIVITY_HANDLE_REJECTION.getValue(), Elements.SERVICE_TASK_REVOKE_CLAIM.getValue(), Elements.END_EVENT_MEMBERSHIP_DECLINED.getValue())`.
- **Alter zwischen 21 und 29:** Der aufgerufene Prozess wartet an `userTask_writeRegretMail`.
  Weil das Element im **aufgerufenen** Prozess liegt, kommt seine Konstante aus der zweiten
  generierten API: Hole die Aufgabe über
  `taskDefinitionKey(HandleRejectionProcessApi.Elements.USER_TASK_WRITE_REGRET_MAIL.getValue())`,
  schließe sie ab, führe die offenen Jobs aus und prüfe denselben Abschluss.

## Randbedingungen

- Der aufgerufene Prozess läuft als **eigene Prozessinstanz**. Assertions auf der
  Hauptinstanz sehen nur deren Aktivitäten – darunter die Call Activity selbst, nicht deren
  Innenleben.
- `mapDecisionResult=singleEntry` funktioniert nur, solange die Tabelle genau eine Zeile
  mit genau einer Ausgabespalte trifft. Bei mehreren Treffern brauchst du eine andere
  Mapping-Strategie.
- Die Compensation-Logik aus Aufgabe 7 bleibt im Hauptprozess; die Call Activity steht
  **zwischen** den Abbruch-Boundary-Events und dem Compensating End Event.
- `POST /api/memberships` liefert die Membership-ID als **reinen Text** zurück, nicht als
  JSON – deshalb unten kein `jq`.

## Erwartetes Ergebnis

Prüfe beide Zweige der Entscheidungstabelle, indem du zwei Anmeldungen mit
unterschiedlichem Alter ablehnen lässt.

**Ablehnung außerhalb der Zielgruppe:**

```bash
MEMBERSHIP_ID=$(curl -s -X POST http://localhost:8080/api/memberships \
  -H "Content-Type: application/json" \
  -d '{"email": "grace@miravelo.com", "name": "Grace", "age": 35}')

curl -X POST http://localhost:8080/api/memberships/$MEMBERSHIP_ID/reject
```

Im Cockpit: Die Hauptinstanz steht an der Call Activity `callActivity_handleRejection`,
während eine **eigene Prozessinstanz** von `handleRejection` durchläuft. Der Business Rule
Task wertet die DMN aus, `isHighValue` ist `false`, das Exclusive Gateway nimmt den
Default-Flow und die aufgerufene Instanz endet an *Accept rejection*. Zurück in der
Hauptinstanz feuert das Compensating End Event, im Log erscheint die Freigabe des Platzes.

**Ablehnung innerhalb der Zielgruppe:**

```bash
MEMBERSHIP_ID=$(curl -s -X POST http://localhost:8080/api/memberships \
  -H "Content-Type: application/json" \
  -d '{"email": "hanna@miravelo.com", "name": "Hanna", "age": 25}')

curl -X POST http://localhost:8080/api/memberships/$MEMBERSHIP_ID/reject
```

Diesmal liefert die DMN `isHighValue = true`, der User Task *Write an email expressing
regret* erscheint in der Tasklist. Nach dem Abschließen endet der aufgerufene Prozess an
*Tried to reaquire applicant*, der Hauptprozess kompensiert wie gehabt.

## Selbstcheck

- [ ] `handleRejection` ist eine eigene Datei und erscheint im Cockpit als eigene
      Prozessdefinition
- [ ] Die DMN liegt unter `src/main/resources/dmn/` und wird beim Start deployt
- [ ] Die Call Activity übergibt `membershipId` **und** `age` per In-Mapping
- [ ] Alter 21–29 führt zum User Task, jedes andere Alter direkt zum End Event
- [ ] Nach Rückkehr aus der Call Activity löst das Compensating End Event `revokeClaim` aus
- [ ] Beide neuen Testfälle sind grün

## Referenzlösung

`../../solutions/exercise-08/` – oder direkt laden:

```bash
./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=08
```

## Nächster Schritt

In Aufgabe 9 verlässt du das eine Modul: Eine andere Abteilung bekommt ihren eigenen Service –
und ihren eigenen Prozess auf derselben Engine.

➡️ [Weiter zu Aufgabe 9](exercise-09.md)
