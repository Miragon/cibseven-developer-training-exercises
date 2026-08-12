# Aufgabe 9 – Die Engine als geteilte Infrastruktur (Remote Engine Pattern)

> **Voraussetzung: Aufgabe 8** ist abgeschlossen – der vollständige Membership-Prozess (Subprozess,
> Boundary Events, Kompensation, Call Activity & DMN) läuft, inklusive des `notifyCommunity`-Zweigs am
> Parallel-Gateway, den du in Aufgabe 6 gebaut hast.

## Story

Die Engine hat sich im Unternehmen etabliert. Immer mehr **Abteilungen** wollen sie nutzen – aber niemand
will eine **eigene** Engine betreiben. Um zu zeigen, dass die Engine eine **wiederverwendbare
Infrastruktur-Komponente** sein kann, lagern wir das Benachrichtigen beim Anlegen eines neuen Members als
**Signal** aus.

Der Membership-Prozess **broadcastet** beim Aktivieren ein Signal `Signal_MemberActivated`. Die
**Logistik-Abteilung** besitzt einen **eigenen, remote implementierten Prozess** in ihrem eigenen Service:
`sendWelcomeKit`. Dieser Service **besitzt das Modell**, **deployt es selbst** in die geteilte Engine, hört
per **Signal-Start-Event** auf das Broadcast und verschickt ein Welcome-Kit. Eine Engine, viele Abteilungen –
ohne dass jede eine eigene braucht.

## Worum es wirklich geht: Wem gehört der Prozess?

**External Task** ist nur der **Mechanismus** – die Engine legt einen Task ab, ein Worker holt ihn per REST
ab und meldet zurück. Das sagt aber nichts darüber, **wem der Prozess gehört**.

Genau das ist hier der Punkt: Der **Logistik-Service besitzt seinen Prozess vollständig** – **Modell,
Worker, Deployment und Tests** liegen bei ihm. Die geteilte Engine **führt den Prozess nur aus**; sie kennt
weder seine fachliche Logik noch sein Modell – das hat der Logistik-Service selbst in die Engine deployt. So
kann eine Abteilung ihren Prozess betreiben, **ohne eine eigene Engine zu brauchen**.

## Lernziele

- Den **Mechanismus** (External Task) von der Frage **„wem gehört der Prozess?"** trennen.
- Erleben, wie ein **eigener Service einen Prozess komplett besitzt** – Modell, Worker, Deployment und
  Tests –, während die geteilte Engine ihn nur ausführt.
- Einen **typisierten Engine-Client** aus der offiziellen OpenAPI-Spec **generieren** und damit die Engine
  über `/engine-rest` treiben.
- Jede Remote-Naht dort testen, wo sie lebt (Worker-Unit-Test, Prozesstest auf In-Memory-Engine,
  Adapter-Test gegen einen HTTP-Stub).

## Zielarchitektur

```
process-application  (GENERISCHER ENGINE-HOST — embedded Engine + /engine-rest + Cockpit, :8080)
  • besitzt den Membership-Prozess; behält den in-engine "Notify community"-Zweig (Teams)
  • NEU (additiv): das terminale End-Event "Membership activated" wirft als Signal-End-Event  Signal_MemberActivated {name}
  • kennt die Logistik nicht und trägt kein send-welcome-kit.bpmn

logistics-service  (REMOTE-OWNER — eigene JVM, :8090)
  • generierter, typisierter Client  <- openapi-generator aus cibseven-engine-rest-openapi
  • deployt  send-welcome-kit.bpmn  beim Start per REST in die Engine (idempotent)
  • erfüllt den Service-Task  shipWelcomeKit  als External Task (Richtung 1: Engine -> Worker)
  • treibt die Engine über den generierten Client (Richtung 2: Worker -> Engine)
  • besitzt seine eigenen Tests (In-Memory-Engine nur im Test-Scope)
```

Den typisierten Client **generiert der Logistik-Service selbst** aus der offiziellen OpenAPI-Spec – es gibt
kein separates Client-Modul. Der Aufbau orientiert sich am Referenz-Blueprint
[`miragon-blueprints/cibseven-remote-example`](https://github.com/miragon-blueprints/cibseven-remote-example)
(dort in Kotlin/Gradle; hier Java/Maven).

## Neuer Prozessablauf

```
Membership (subscribeNewsletter, Engine-Host):
  ... (Confirmed) → ⬦ Parallel-Fork ┬─ [Send Welcome Mail] ┬ ⬦ Parallel-Join → (◉ "Membership activated" = Signal-End-Event)
                                    └─ [Notify community]  ┘                              │  wirft Signal_MemberActivated {name}
                                       (#{notifyCommunityDelegate}, Teams)                ▼
Logistik (sendWelcomeKit, Remote-Service besitzt & deployt das Modell):
  (Signal-Start: Signal_MemberActivated) ┐
                                         ⬦ → [Ship welcome kit] external, topic="shipWelcomeKit" → (End)
  (Start: manuell / Test) ───────────────┘             ▲  fetch & lock, complete
                        logistics-service:  ShipWelcomeKitWorker → WelcomeKitShipmentOutPort
```

Der `sendWelcomeKit`-Prozess hat **zwei Start-Events**: das **Signal-Start-Event** ist der Produktions-Trigger
(reagiert auf das Broadcast), das leere **manuelle Start-Event** erlaubt einen Start per
`startProcessInstanceByKey` über die REST-API – z. B. um ein Kit **erneut** zu verschicken oder zum Testen,
falls das Signal mal nicht durchkommt. Genau dafür treibt der Service die Engine über den generierten Client.

Das Parallel-Gateway aus Aufgabe 6 bleibt erhalten – wir machen nur das **terminale End-Event**
„Membership activated" zu einem **Signal-End-Event** (es beendet den Prozess **und** wirft das Signal, kein
neuer Zweig). Da ein Broadcast „fire-and-forget" ist, blockiert die
Membership-Aktivierung nicht auf der Logistik: Signal = **1:N-Broadcast**, mehrere Abteilungen könnten auf
dasselbe Ereignis hören.

## Aufgaben

### Vorbereitung

**Baseline: Membership-Prozess aus Aufgabe 8.** Aufgabe 9 **baut auf dem fertigen Membership-Prozess auf** –
Teil A ändert dessen terminales End-Event „Membership activated", und ohne den kompletten Vorprozess (Claim,
Gateway, Confirm-Subprozess, Boundaries, Kompensation, Call Activity, Notify) hängt das Signal an nichts.
Hast du die Aufgaben 1–8 durchgearbeitet, ist `services/process-application` bereits im richtigen Zustand.
**Steigst du direkt hier ein, hol dir zuerst die Baseline** (voller Prozess **+ Code** aus Aufgabe 8):

```bash
./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=08
```

> Nur das **fertige Ergebnis** laufen lassen, ohne Teil A selbst zu bauen? `-Dsolution=09` lädt den kompletten
> Host inkl. Signal-End-Event in `services/process-application`. Das End-to-End-Setup unten läuft ohnehin
> direkt gegen die fertigen `solutions/exercise-09/…`-Module – da ist alles schon integriert.

**Neue Abteilung anlegen.** Bis hierher gab es in `services/` nur die `process-application`. Jetzt kommt eine
**neue Abteilung** dazu – also legen wir ihren Service an. Die Vorlage liegt unter
`templates/exercise-09/logistics-service`:

1. Vorlage nach `services/` kopieren:

   ```bash
   cp -R templates/exercise-09/logistics-service services/logistics-service
   ```

2. Das Modul in der root `pom.xml` unter `<modules>` eintragen:

   ```xml
   <module>services/logistics-service</module>
   ```

Ab jetzt baut `./mvnw` den neuen Service mit. (Er kompiliert schon im Ausgangszustand – der Client-Teil ist
noch auskommentiert.)

### Teil A – Engine-Host (`services/process-application`)

Hier ist fast nichts zu tun – beim Host wird nur das End-Event zum Signal-Werfer:

1. Das bestehende terminale End-Event **„Membership activated"** (nach dem Parallel-Join) zu einem
   **Signal-End-Event** machen – es beendet den Prozess **und** wirft das Signal. `Send Welcome Mail` und
   `Notify community` bleiben unverändert. **Kein neuer Zweig, kein neues Element.**
2. Das End-Event wirft `Signal_MemberActivated` – **ohne** Java-Delegate, die Engine wirft das Signal nativ.
   Die Payload (`name`) gibt das End-Event über `camunda:in` mit:

   ```xml
   <bpmn:endEvent id="endEvent_membershipActivated" name="Membership activated" camunda:asyncBefore="true">
     <bpmn:signalEventDefinition signalRef="Signal_MemberActivated">
       <bpmn:extensionElements>
         <camunda:in source="name" target="name" />
       </bpmn:extensionElements>
     </bpmn:signalEventDefinition>
   </bpmn:endEvent>
   ```

   Dazu einmalig das Signal auf Definitions-Ebene deklarieren:
   `<bpmn:signal id="Signal_MemberActivated" name="Signal_MemberActivated" />`.

Das war's. Der Host ruft nur „neues Mitglied aktiviert" in den Raum – wer darauf reagiert, ist ihm egal.
Kein `RuntimeService`, kein Delegate – reines BPMN.

### Teil B – Logistik-Service (`services/logistics-service`)

Hier steckt die eigentliche Arbeit – das ist die **Capstone-Aufgabe**: Der Ablauf ist bewusst **erst den
Prozess selbst modellieren, dann die APIs generieren, dann den Worker schreiben.** Fülle die
**`TODO Aufgabe 9`**-Stellen der Reihe nach:

1. **Prozess von Grund auf modellieren** (`send-welcome-kit.bpmn`) – die Datei enthält bewusst nur ein
   **leeres Modell mit einem Start-Event**. Modelliere den kompletten `sendWelcomeKit`-Prozess selbst –
   das ist dein Abschlusstest, ob das Gelernte sitzt. Ziel (siehe [Neuer Prozessablauf](#neuer-prozessablauf)):
   - Prozess-**ID** `sendWelcomeKit`, `isExecutable=true`, `historyTimeToLive` gesetzt.
   - Zwei Start-Events: ein **Signal-Start-Event** auf `Signal_MemberActivated` (Produktion) und ein leeres
     **Start-Event** (manueller Start / Test), zu einem Gateway zusammengeführt.
   - Ein **External Service Task** „Ship welcome kit" (Implementation = *External*, Topic `shipWelcomeKit`).
   - Ein **End-Event**, saubere Sequenzflüsse und sprechende Element-IDs.
2. **APIs generieren** (`pom.xml`) – zwei auskommentierte Generator-Blöcke aktivieren:
   - **Process-API** (`bpmn-to-code`): erzeugt aus deinem (jetzt externen) Task die Konstante
     `SendWelcomeKitProcessApi.ServiceTasks.SHIP_WELCOME_KIT`.
   - **Engine-Client** (`openapi-generator`): erzeugt aus CIB sevens offizieller OpenAPI-Spec einen
     **typisierten `/engine-rest`-Client** (statt fehleranfälliger REST-Calls von Hand). Setze die beiden
     `TODO`-Werte (`generatorName` = `java`, `library` = `restclient`).

   Beide Blöcke einkommentieren, dann generieren:

   ```bash
   ./mvnw -pl services/logistics-service generate-sources
   ```

   (`org.cibseven.rest.client.api/.model` + `SendWelcomeKitProcessApi` erscheinen unter `target/…` bzw. `src`.)
3. **Modell deployen** (`EngineDeploymentAdapter`) – beim Start das eigene `send-welcome-kit.bpmn` in die
   Engine schicken. **Idempotent**: ein Neustart erzeugt kein zweites Deployment.
4. **Worker schreiben** (`ShipWelcomeKitWorker`) – die Klasse ist absichtlich leer. Mach sie zur Bean
   (`@Component`), abonniere den Topic
   (`@ExternalTaskSubscription(topicName = SendWelcomeKitProcessApi.ServiceTasks.SHIP_WELCOME_KIT)`), lass sie
   von `BaseExternalTaskWorker` erben, lies den `name`, verschicke das Kit über den Use Case und schließe den
   Task ab.
5. **Client verdrahten & Engine treiben** (`EngineClientConfig` + `RemoteWelcomeKitProcessAdapter`) – die
   `ProcessDefinitionApi`-Bean bereitstellen und den Prozess per `startProcessInstanceByKey` starten. Das
   nutzt das **manuelle Start-Event** und steckt hinter der „Kit erneut senden"-Aktion `POST /api/welcome-kits`.

## Testen

### Automatisiert (ohne laufende Engine)

Der Logistik-Service testet **jede Naht selbst** – Worker-Unit-Test, Prozesstest (In-Memory-Engine,
Signal-Start → External Task als Wait-State → complete), Deployment-Adapter- und Remote-Adapter-Test
(HTTP-Stub via `MockRestServiceServer`):

```bash
./mvnw -pl solutions/exercise-09/logistics-service test
./mvnw -pl solutions/exercise-09/process-application test -Dtest=MembershipProcessTest
```

### End-to-End (beide Services)

```bash
# 1. Stack + Engine-Host (:8080) starten
cd stack && docker-compose up -d
cd ../solutions/exercise-09/process-application && ../../../mvnw spring-boot:run

# 2. Logistik-Service (:8090) in einem eigenen Terminal starten – er deployt sein Modell beim Start
cd solutions/exercise-09/logistics-service && ../../../mvnw spring-boot:run

# 3. Beweis, dass der Remote-Service das Modell deployt hat:
curl http://localhost:8080/engine-rest/deployment    # listet nun das logistics-service-Deployment
#   → Neustart des Logistik-Services erzeugt KEIN zweites Deployment (idempotent).

# 4. Member anlegen und im Cockpit (http://localhost:8080/camunda, admin/admin) die Confirm-Aufgabe
#    abschließen → das Signal feuert → eine sendWelcomeKit-Instanz läuft → der Worker verschickt das Kit.
curl -X POST http://localhost:8080/api/memberships \
  -H "Content-Type: application/json" \
  -d '{"email":"jane@example.com","name":"Jane","age":30}'

# 5. Welcome-Kit erneut senden (treibt die Engine über den generierten Client):
curl -X POST http://localhost:8090/api/welcome-kits -H "Content-Type: application/json" -d '{"name":"Jane"}'
```

**Beweis, dass der Remote-Service wirklich Owner ist:** Logistik-Service **stoppen**, einen Member aktivieren
→ im Cockpit wartet eine `sendWelcomeKit`-Instanz am External Task `Ship welcome kit`. Logistik-Service
**starten** → er holt den Task ab und verschickt das Kit. 🎉

## Akzeptanzkriterien

- `send-welcome-kit.bpmn` liegt **nur** im Logistik-Service; es erscheint in den Deployments der Engine
  **erst nachdem** der Logistik-Service gestartet ist (`GET /engine-rest/deployment` / Cockpit).
- Ein Neustart des Logistik-Services erzeugt **kein** doppeltes Deployment (idempotent).
- Member aktivieren ⇒ `Signal_MemberActivated` wird broadcastet ⇒ eine `sendWelcomeKit`-Instanz läuft ⇒ der
  Worker verschickt das Kit; die Membership-Aktivierung wartet nicht auf die Logistik.
- Die Tests des Logistik-Services sind **grün, ohne** dass der Engine-Host läuft.

## Hinweise für Trainer / Ausblick

- Kernkontrast: *External Task ist nur der Mechanismus – die eigentliche Frage ist, wem der Prozess gehört
  (wer sein Modell besitzt und deployt)*. (Im Training: die Ownership-Folie aus Kapitel 7.1.)
- Signal = **1:N-Broadcast**: Als Erweiterung kann eine **zweite** Abteilung (z. B. Analytics mit
  `recordSignup`) auf **dasselbe** Signal hören – ein Prozess, den ein weiterer Remote-Service besitzt und
  deployt. Genau das beweist „eine Engine, viele Abteilungen" live.
- CIB Seven läuft hier weiterhin embedded im Host – „remote" ist die Sicht des **Clients**; eine echte
  Standalone-Engine (`cibseven/cibseven:run`) ist dasselbe Bild mit ausgetauschtem Host.
- **Transaktionsgrenze (Anknüpfung an Aufgabe 4):** Der External Task ist die Commit-Grenze zwischen Engine
  und Worker. Die Engine committet, sobald sie den Task anlegt, und wartet als Wait State – der Worker holt
  ihn per `fetchAndLock`, arbeitet in **seiner eigenen** Transaktion und meldet erst `complete` (oder
  `handleFailure`) zurück. Ein fehlgeschlagener `shipWelcomeKit` rollt deshalb **nichts** in der Engine
  zurück; ob und wie oft neu versucht wird, ist eine **Worker-Entscheidung** (Retries/Backoff), kein
  Engine-Rollback. Das ist der bewusste Gegenpol zum `asyncBefore`-Muster: dort setzt *das Modell* die
  Grenze, hier bringt der Mechanismus sie mit und die Fehlerbehandlung wandert zum Owner.
- **Signal-Broadcast ist synchron im Werfer.** Ein Signal-Wurf liefert in CIB seven/Camunda 7 **in der
  Transaktion des Werfers** aus: Ohne Marker würde das Signal-End-Event `endEvent_membershipActivated` die
  `sendWelcomeKit`-Instanz anlegen und synchron bis zum External Task treiben – alles in der Membership-Aktivierungs-Transaktion.
  Ein Fehler dort (Prozess noch nicht deployt, Race beim Start) würde die Aktivierung mit zurückrollen.
  Deshalb sind **zwei** Grenzen gesetzt: `asyncBefore` auf dem Signal-End-Event `endEvent_membershipActivated`
  (der Signal-Wurf committet in eigener Transaktion, nach dem Join) **und** `asyncBefore` auf dem
  **Signal-Start** `startEvent_memberActivated` im Logistik-Prozess (die neue Instanz committet sofort und
  läuft im eigenen Job – die beiden Prozesse sind an der Signalnaht sauber entkoppelt). Erst damit gilt
  „die Membership-Aktivierung wartet nicht auf die Logistik" auch **vor** dem External Task.

## Referenzlösung

- Engine-Host: `../solutions/exercise-09/process-application/`
- Logistik-Service: `../solutions/exercise-09/logistics-service/`
- Generierter Client: `../services/cibseven-engine-client/`
- Vorlage/Blueprint: [`miragon-blueprints/cibseven-remote-example`](https://github.com/miragon-blueprints/cibseven-remote-example)

---

🎉 **Geschafft!** Du hast einen Prozess gebaut, den ein **eigener Remote-Service besitzt und deployt** –
und dabei die Engine als wiederverwendbare Infrastruktur erlebt. Lust auf mehr? Die
[Extra-Aufgabe](extra-task-1.md) baut den Prozess engine-neutral um.
