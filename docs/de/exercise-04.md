# Aufgabe 4 – Kapazitätsprüfung mit Gateway

> **Voraussetzung:** Aufgabe 3 ist abgeschlossen – Double-Opt-In läuft, der Prozess startet per Nachricht.
> **Arbeitsverzeichnis:** `services/process-application`
> **Neu in dieser Aufgabe:** Domain-Refactoring zu *Membership*, Exclusive Gateway, alternativer Prozessausgang, Transaktionsgrenzen, Business Key, generiertes Task-Formular.

## Darum geht es

**Strategie-Meeting, Freitagnachmittag. Jemand hat exklusiven Matcha Latte mitgebracht.**

Miravelo startet den **Miravelo Inner Circle** – eine limitierte Membership für echte Fans
der Marke. Tausend Plätze. Mehr nicht.

Warum tausend? Weil Knappheit Wert erzeugt. Weil FOMO ein Geschäftsmodell ist. Weil
irgendjemand ein Buch über Luxusmarken gelesen hat.

> *„Wir sind nicht exklusiv, weil wir gut sind. Wir sind exklusiv, weil der Counter in der
> Datenbank auf 1000 steht."*
> — Ehrlichster Kommentar im Sprint Planning

Aus Prozesssicht ist das ein **Gateway**: Platz bekommen? Weiter im Text. Kein Platz?
Ablehnungsmail. Und weil jede Anmeldung ab jetzt ein fachliches Objekt mit eigener ID ist,
bekommt jede Prozessinstanz einen **Business Key** – Schluss mit „welche der 40 laufenden
Instanzen war noch mal Carol?".

## Lernziele

Nach dieser Aufgabe kannst du

- ein Exclusive Gateway modellieren, seine Bedingungen setzen und einen Default-Flow wählen,
- einen alternativen Prozessausgang (Ablehnung) umsetzen,
- eine Entscheidung aus Java-Code als Prozessvariable an das Gateway übergeben,
- **Transaktionsgrenzen** bewusst setzen und begründen, warum ein nicht wiederholbarer
  Schritt vor einem externen Effekt committen muss,
- einer Prozessinstanz einen Business Key zuordnen,
- einem User Task ein generiertes Task-Formular für einen Freigabeschritt geben.

## Ziel-Modell

![BPMN-Modell der Aufgabe](../assets/exercise-04.svg)

Referenzmodell: `../../models/exercise-04/newsletter.bpmn`

## Aufgabe

### 1. Domain umbenennen

Aus der Newsletter-Subscription wird die Membership im Inner Circle. Benenne die
bestehenden Klassen konsequent um – `Subscription` → `Membership`, `SubscriptionId` →
`MembershipId`, `RegisterSubscriptionUseCase` → `RegisterMembershipUseCase` und so weiter.
Der REST-Pfad wird zu `/api/memberships`, die Prozessvariable `subscriptionId` zu
`membershipId`.

> Der Prozess-Key bleibt `subscribeNewsletter` und die Datei weiterhin `newsletter.bpmn` –
> so, wie es in echten Projekten auch bleibt, wenn ein Prozess fachlich weiterwächst. Wir
> erwähnen das hier einmal und danach nicht mehr.

### 2. Modell erweitern

Vor dem Versand der Bestätigungs-Mail kommen ein **Service Task** für die Reservierung und
ein **Exclusive Gateway** dazu, das den Sequenzfluss in zwei Pfade teilt. Insgesamt sind es
vier neue Elemente – und zwei bestehende bekommen neue Element-IDs und Namen, weil aus der
Subscription eine Membership geworden ist.

**Neue Elemente:**

| Element | Typ | ID | Name | Konfiguration |
|---|---|---|---|---|
| Platz reservieren | Service Task | `serviceTask_claimMembership` | Claim membership | Delegate Expression: `#{claimMembershipDelegate}` |
| Kapazitätsentscheidung | Exclusive Gateway | `gateway_hasEmptySpots` | Has empty spots | Default-Flow: Ja-Pfad |
| Ablehnungs-Mail | Service Task | `serviceTask_sendRejectionMail` | Send rejection mail | Delegate Expression: `#{sendRejectionMailDelegate}` |
| Ablehnung | End Event | `endEvent_membershipRejected` | Membership rejected | – |

**Umbenannte Elemente:**

| Alt (Aufgabe 3) | Neu (Aufgabe 4) |
|---|---|
| `userTask_confirmSubscription` – Confirm subscription | `userTask_confirmMembership` – Confirm membership |
| `endEvent_userSubscribed` – User subscribed | `endEvent_membershipConfirmed` – Membership confirmed |

**Bedingung am Nein-Pfad:** `${!hasEmptySpots}`. Der Ja-Pfad ist der Default-Flow und
braucht keine Bedingung.

### 3. Use Cases und Services ergänzen

Nach dem Muster aus Aufgabe 3:

- **`ClaimMembershipUseCase` / `ClaimMembershipService`** – prüft die Kapazität und gibt
  `true` zurück, wenn noch ein Platz frei war. Ein einfacher Zähler im Speicher genügt
  (maximal 1000 Plätze); eine Datenbank brauchst du dafür nicht.
- **`SendRejectionMailUseCase` / `SendRejectionMailService`** – lädt die Membership und
  logget die Ablehnung mit der E-Mail-Adresse.

> Die Kapazität ist bewusst schlicht gehalten. Die Referenzlösung nutzt einen
> `AtomicInteger` samt Konstante `MAX_SPOTS` direkt im `ClaimMembershipService`. Wenn du
> es sauberer magst, modelliere stattdessen ein Domain-Objekt `MembershipCapacity` mit
> `maxSpots`, `claimedSpots`, `hasEmptySpots()` und `claim()` – fachlich ist beides gleichwertig.

### 4. Delegates ergänzen

- **`ClaimMembershipDelegate`** – liest `membershipId`, ruft den Use Case auf und schreibt
  dessen Ergebnis als Prozessvariable `hasEmptySpots` auf die `DelegateExecution`.
- **`SendRejectionMailDelegate`** – liest `membershipId` und ruft den Use Case auf.

> Das Setzen der Prozessvariable gehört in den **Delegate**, nicht in den Service: Der
> Service kennt die Engine nicht und gibt nur ein `boolean` zurück. Genau diese Trennung
> prüft der `ArchitectureTest`.

### 5. Transaktionsgrenzen setzen

> Theorie dazu: Trainingskapitel **„Async & Transaction Boundaries"** (Topic 4, *Execution
> Resilience*) – Save Points, Default- und manuelle Grenzen, Rollback in Aktion. Hier ist
> die erste Stelle, an der du es anwendest.

Bis hierher lief der Prozess komplett **synchron**. Ab diesem Modell setzt du
Transaktionsgrenzen – in zwei Stufen.

**a) Grenzen an den Wait States.** Die Engine committet automatisch an jedem Wait State –
an einem User Task muss sie den Zustand ohnehin speichern. Überall sonst setzt du die Grenze
selbst, mit einer **asynchronen Continuation**: Die Marker `asyncBefore` und `asyncAfter`
sagen der Engine, dass sie an dieser Stelle committen, einen Job anlegen und die Arbeit
danach in einer **neuen** Transaktion fortsetzen soll.

Ergänze die beiden Continuations, die hier fehlen:

- `asyncBefore` am Message Start Event `startEvent_submitRegistration` – saubere Grenze
  nach der Korrelation; der `correlateMessage`-Aufruf legt nur die Instanz an und kehrt zurück.
- `asyncAfter` am User Task `userTask_confirmMembership` – die Completion committet sofort.
  Sonst laufen Completion **und** der nachgelagerte Service Task in **einer** Transaktion:
  Wirft er, rollt die Completion mit zurück und der Task erscheint wieder in der Tasklist.

**b) Grenzen an den Service Tasks.** Mit `claimMembership` steht erstmals ein **nicht
wiederholbarer** Schritt – die Platzreservierung – direkt vor einem Mailversand. Zwischen
Message Start und User Task liegt **kein** Wait State; ohne weitere Marker laufen
`claimMembership` und `sendConfirmationMail` deshalb in **einer** Engine-Transaktion.

Wirft der Mailversand eine Exception, rollt die Engine die *gesamte* Transaktion zurück und
führt den Job erneut aus. Ergebnis: `claimMembership` läuft ein zweites Mal – ein doppelt
reservierter Platz, obwohl nur der Mailversand fehlgeschlagen ist.

**Regel:** Trenne die *nicht wiederholbare* Arbeit vom *externen, nicht zurückrollbaren*
Effekt durch eine eigene Transaktionsgrenze. Setze `asyncBefore` an jeden Service Task mit
externem Effekt:

| Marker | Element | Warum |
|---|---|---|
| `asyncBefore` | `serviceTask_sendConfirmationMail` | committet die Reservierung zuerst; ein Mail-Fehler wiederholt nur den Versand |
| `asyncBefore` | `serviceTask_sendRejectionMail` | liegt sonst in derselben Transaktion wie `claimMembership` |
| `asyncBefore` | `serviceTask_sendWelcomeMail` | Konsistenz; ab Aufgabe 6 zusätzlich auf einem Parallelzweig relevant |

`claimMembership` bekommt bewusst **keinen** Marker – es soll früh committen, gemeinsam mit
dem Token, das im Modell weiterrückt (das *Token* ist die gedachte Spielfigur, die den
aktuellen Stand einer Instanz im Prozessmodell markiert). Der Marker gehört auf den *nachgelagerten* Aufruf, der die
Reservierung sonst mit zurückrollt. Im Modeler: Element auswählen → Properties Panel →
*Asynchronous Before*.

### 6. Business Key setzen

Setze beim Start des Prozesses die `membershipId` als Business Key. Der Correlation Builder
im `MembershipProcessAdapter` bietet dafür `processInstanceBusinessKey(...)`:

```java
runtimeService.createMessageCorrelation("Message_SubscriptionRequested")
        .processInstanceBusinessKey(membership.id().value().toString())
        .setVariables(...)
        .correlateStartMessage();
```

Der Business Key verknüpft die Prozessinstanz mit dem fachlichen Objekt: Im Cockpit lässt
sich jede Instanz eindeutig einer Anmeldung zuordnen und gezielt suchen.

### 7. Task-Formular für die Freigabe

Der User Task `userTask_confirmMembership` hat bisher kein Formular – wer ihn in der Tasklist
öffnet, sieht keine einzige Prozessvariable und kann ihn nur blind abschließen. Gib ihm ein
**generiertes Task-Formular** (*Generated Task Form*, ein Bordmittel von Camunda 7 – keine
zusätzliche Datei, kein HTML), damit die freigebende Person die Anmeldedaten sieht:

| Feld-ID | Label | Typ | Zweck |
|---|---|---|---|
| `name` | Name | string | Kontext, wird aus der Prozessvariable vorbefüllt |
| `email` | E-Mail | string | Kontext, vorbefüllt |
| `age` | Age | long | Kontext, vorbefüllt |
| `confirmed` | Confirm membership | boolean | die eigentliche Freigabe (Checkbox) |

`name`, `email` und `age` tragen dieselben IDs wie die Prozessvariablen und werden dadurch
automatisch vorbefüllt. `confirmed` ist neu und wird beim Abschließen als boolesche
Prozessvariable gespeichert.

Im Modeler: User Task auswählen → Properties Panel → Abschnitt **Forms** → Formularfelder
anlegen. Im XML entsteht dabei ein `extensionElements`-Block mit `camunda:formData` direkt
im User Task:

```xml
<bpmn:userTask id="userTask_confirmMembership" name="Confirm membership" camunda:asyncAfter="true">
  <bpmn:extensionElements>
    <camunda:formData>
      <camunda:formField id="name" label="Name" type="string" />
      <camunda:formField id="email" label="E-Mail" type="string" />
      <camunda:formField id="age" label="Age" type="long" />
      <camunda:formField id="confirmed" label="Confirm membership" type="boolean" />
    </camunda:formData>
  </bpmn:extensionElements>
</bpmn:userTask>
```

## Randbedingungen

- Der Feldtyp muss zum Typ der Prozessvariable passen, sonst greift die Vorbefüllung nicht
  (`age` ist `long`, nicht `string`).
- `confirmed` steuert in dieser Aufgabe noch keinen Prozessfluss – es wird nur erfasst.
- Die Kapazität lebt im Arbeitsspeicher und ist nach einem Neustart wieder bei null. Das
  ist für das Training gewollt.
- Element-IDs und Variablennamen kannst du jederzeit aus dem Referenzmodell übernehmen.

## Erwartetes Ergebnis

Der Prozess hat jetzt zwei Ausgänge – prüfe beide. Zuerst den Weg, den fast alle nehmen:

**Freier Platz vorhanden:**

```bash
curl -X POST http://localhost:8080/api/memberships \
  -H "Content-Type: application/json" \
  -d '{"email": "carol@miravelo.com", "name": "Carol", "age": 27}'
```

Der Prozess reserviert einen Platz, nimmt den Ja-Pfad und wartet am User Task
`Confirm membership`. Öffne ihn in der Tasklist (`http://localhost:8080/webapp/#/seven/auth/start`,
admin/admin): Name, E-Mail und Alter sind vorbefüllt. Setze den Haken bei *Confirm
membership* und schließe den Task ab – die Instanz läuft über `Send Welcome Mail` bis
`Membership confirmed`, und `confirmed` steht in der History auf `true`.

**Kein Platz mehr frei:** Setze die maximale Platzzahl vorübergehend auf `0` (in der
Referenzlösung die Konstante `MAX_SPOTS` in `ClaimMembershipService`), starte die Anwendung
neu und schicke:

```bash
curl -X POST http://localhost:8080/api/memberships \
  -H "Content-Type: application/json" \
  -d '{"email": "dave@miravelo.com", "name": "Dave", "age": 30}'
```

Erwartetes Log: `Sending rejection mail to dave@miravelo.com`. Die Instanz endet an
`Membership rejected`, ohne je an einem User Task zu warten.

## Selbstcheck

- [ ] Alle Klassen sind auf *Membership* umbenannt, der REST-Pfad lautet `/api/memberships`
- [ ] Das Gateway hat einen Default-Flow und genau eine Bedingung (`${!hasEmptySpots}`)
- [ ] Der Ja-Pfad endet an `Membership confirmed`, der Nein-Pfad an `Membership rejected`
- [ ] `asyncBefore` steht am Message Start Event und an den drei Mail-Tasks,
      `asyncAfter` am User Task, `claimMembership` hat **keinen** Marker
- [ ] Im Cockpit trägt die Instanz die `membershipId` als Business Key
- [ ] Das Task-Formular zeigt die vorbefüllten Felder plus die Checkbox `confirmed`

## Hinweise

**Idempotenz-Merksatz:** Ein Retry darf einen Service Task erneut ausführen. Sobald eine
Aktion nur *einmal* passieren darf (Reservierung, Zahlung), muss sie entweder vor der Grenze
committen oder idempotent sein. Bei externen Schnittstellen begegnet dir dasselbe Muster in
[Aufgabe 9](exercise-09.md) und in [Extra-Aufgabe 1](extra-task-1.md) wieder.

## Referenzlösung

`../../solutions/exercise-04/` – oder direkt laden:

```bash
./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=04
```

## Nächster Schritt

Der Prozess hat jetzt zwei Ausgänge – und niemand prüft automatisch, ob er den richtigen
nimmt. In Aufgabe 5 sicherst du ihn mit einem Prozess-Test ab.

➡️ [Weiter zu Aufgabe 5](exercise-05.md)
