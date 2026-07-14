# Extra-Aufgabe 1 – Raus aus dem Engine-Lock-in: die Process-Engine-API

> **Voraussetzung:** Aufgabe 8 ist abgeschlossen. Der Prozess läuft vollständig – mit Service Tasks, User Tasks, Gateways, Boundary Events, Subprozess, Signal, Call Activity, DMN und Kompensation.

## Ziel-Modell

Das Prozessmodell ändert sich **fachlich nicht**. Es ist exakt der Prozess aus Aufgabe 8 – nur die technische Anbindung der Service Tasks wechselt.

Hauptprozess:

![BPMN Hauptprozess](assets/exercise-8-main.svg)

Sub-Prozess `handleRejection`:

![BPMN Sub-Prozess](assets/exercise-8-sub.svg)

## Lernziele

- Verstehen, wo native Engine-Kopplung im Code entsteht (`JavaDelegate`, `DelegateExecution`, `RuntimeService`)
- Die **Process-Engine-API** als engine-neutralen Abstraktions-Layer einsetzen
- Service Tasks über das **Worker-Pattern** (`@ProcessEngineWorker`) statt `DelegateExpression` anbinden
- Prozesse engine-neutral starten und Nachrichten korrelieren (`StartProcessApi`, `CorrelationApi`)
- Den Engine-Wechsel auf einen **Adapter-Tausch** reduzieren
- Per Architektur-Test garantieren, dass kein `org.cibseven.bpm`-Import mehr in den Code leakt

## Hintergrund

**Strategie-Meeting. Der Prozess läuft. Jemand stellt die unbequeme Frage.**

> *„Schön, dass alles läuft. Aber wir haben gerade unsere komplette Fachlogik an eine Engine getackert. Was passiert, wenn wir in zwei Jahren wechseln müssen?"*
> — Die Person, die schon mal eine Migration mitgemacht hat.

Camunda 7 ist End-of-Life. Wir sind auf **CIB Seven** umgestiegen – einen gepflegten Fork, gute Wahl. Aber ehrlich: Unser Code weiß das ein bisschen *zu* genau. Jeder Service Task hängt an einem `JavaDelegate` mit `org.cibseven.bpm.engine.delegate.DelegateExecution`. Der Prozess-Adapter ruft `RuntimeService` direkt auf. Würden wir morgen auf Camunda 8 oder Operaton wechseln wollen, müssten wir **jeden Delegate und jeden Engine-Call anfassen**.

Genau dafür gibt es die [**Process-Engine-API**](https://github.com/bpm-crafters/process-engine-api) von bpm-crafters: ein engine-neutraler Layer – so wie JPA Datenbanken abstrahiert oder Spring Cloud Stream Messaging-Systeme. Sie bringt Adapter für verschiedene BPMN-Engines mit (CIB Seven, Camunda 7, Camunda 8, Operaton …). Ein Engine-Wechsel wird – stark vereinfacht – zum **Tausch eines Adapters**. In der Realität nie ganz so einfach, aber deutlich einfacher als sonst. Und: Wir bauen weniger Kopplung auf, machen weniger falsch und sammeln weniger technische Schulden.

> Als Vorlage für diese Aufgabe dient das Repo [**engine-safari**](https://github.com/emaarco/engine-safari), Modul `cib-seven-with-process-engine-api`.

Das Beste daran: **Domain, Application-Services und Ports bleiben unangetastet.** Sie waren schon immer engine-neutral – das war der ganze Sinn der hexagonalen Architektur. Wir fassen nur den Adapter-Layer an.

## Was sich ändert (und was nicht)

| Schicht | Aufgabe 8 (nativ CIB7) | Extra-Aufgabe 1 (Process-Engine-API) |
|---|---|---|
| `domain/`, `application/` | unverändert | **unverändert** |
| Inbound Service Tasks | `JavaDelegate` + `DelegateExecution` | `@ProcessEngineWorker`-Worker |
| Outbound Prozess-Adapter | `RuntimeService.createMessageCorrelation(...)` | `StartProcessApi` / `CorrelationApi` |
| BPMN Service Tasks | `camunda:delegateExpression="#{xDelegate}"` | `camunda:type="external"` + `camunda:topic` |
| Bootstrap | `@EnableProcessApplication` | entfällt (Adapter übernimmt Deployment & Worker) |

Alles andere im BPMN – Message-Start, Boundary-Events, Subprozess, Signal, Call Activity, DMN, Kompensation – bleibt **strukturell gleich**. DMN und User Tasks laufen weiterhin in der Engine; dafür brauchen wir keine Worker.

## Aufgaben

> Tipp: Am einfachsten kopierst du deine Aufgabe-7-Lösung und baust sie Schritt für Schritt um.

### 1. Dependencies & Adapter einbinden

In die `pom.xml` des Moduls:

- `dev.bpm-crafters.process-engine-api:process-engine-api`
- `dev.bpm-crafters.process-engine-worker:process-engine-worker-spring-boot-starter`
- `dev.bpm-crafters.process-engine-adapters:process-engine-adapter-cib-seven-embedded-spring-boot-starter`

Der CIB-Seven-Embedded-Adapter ist der Baustein, den du beim Engine-Wechsel später austauschen würdest – mehr nicht.

### 2. Service Tasks im BPMN auf External Tasks umstellen

Aus jedem

```xml
<bpmn:serviceTask id="serviceTask_sendConfirmationMail" name="Send confirmation mail"
                  camunda:delegateExpression="#{sendConfirmationMailDelegate}">
```

wird ein External Task mit Topic – plus ein Input-Mapping, damit der Worker die `membershipId` bekommt:

```xml
<bpmn:serviceTask id="serviceTask_sendConfirmationMail" name="Send confirmation mail"
                  camunda:type="external" camunda:topic="sendConfirmationMail">
  <bpmn:extensionElements>
    <camunda:inputOutput>
      <camunda:inputParameter name="membershipId">${membershipId}</camunda:inputParameter>
    </camunda:inputOutput>
  </bpmn:extensionElements>
```

Das machst du für alle sieben Service Tasks (`claimMembership`, `sendConfirmationMail`, `sendWelcomeMail`, `sendRejectionMail`, `reSendConfirmationMail`, `revokeClaim`, `notifyAboutSignedMembership`) – auch für den Kompensations-Task `revokeClaim`.

### 3. Delegates durch Worker ersetzen

Aus dem `JavaDelegate`

```java
@Component
public class SendConfirmationMailDelegate extends BaseDelegate {
    @Override
    protected void executeTask(DelegateExecution execution) {
        var membershipId = (String) execution.getVariable("membershipId");
        useCase.sendConfirmationMail(new MembershipId(UUID.fromString(membershipId)));
    }
}
```

wird ein engine-neutraler Worker – **kein** `org.cibseven.bpm`-Import mehr:

```java
@Component
public class SendConfirmationMailWorker {

    private final SendConfirmationMailUseCase useCase;

    public SendConfirmationMailWorker(SendConfirmationMailUseCase useCase) {
        this.useCase = useCase;
    }

    @ProcessEngineWorker(topic = ServiceTasks.SEND_CONFIRMATION_MAIL)
    public void sendConfirmationMail(@Variable(name = "membershipId") String membershipId) {
        useCase.sendConfirmationMail(new MembershipId(UUID.fromString(membershipId)));
    }
}
```

> Die Topic-Konstanten (`ServiceTasks.SEND_CONFIRMATION_MAIL`) stammen aus einer aus dem BPMN **generierten** Process-API – dazu in einer späteren Aufgabe mehr. Du kannst die Topics vorerst auch als schlichte Strings schreiben.

Der `claimMembership`-Worker gibt – anders als die übrigen – ein Ergebnis zurück, das das Gateway auswertet:

```java
@ProcessEngineWorker(topic = ServiceTasks.CLAIM_MEMBERSHIP)
public Map<String, Object> claimMembership(@Variable(name = "membershipId") String membershipId) {
    var hasEmptySpots = useCase.claimMembership(new MembershipId(UUID.fromString(membershipId)));
    return Map.of("hasEmptySpots", hasEmptySpots);
}
```

### 4. Outbound-Adapter auf die Engine-API umstellen

Statt `RuntimeService` injizierst du `StartProcessApi` und `CorrelationApi`:

```java
@Override
public void startProcess(Membership membership) {
    var membershipId = membership.id().value().toString();
    startProcessApi.startProcess(new StartProcessByMessageCmd(
            Messages.MESSAGE_SUBSCRIPTION_REQUESTED.getValue(),
            Map.of(
                    "membershipId", membershipId,
                    "email", membership.email().value(),
                    "name", membership.name().value(),
                    "age", membership.age().value(),
                    CommonRestrictions.CORRELATION_KEY, membershipId
            )
    )).join();
}

@Override
public void rejectMembership(MembershipId membershipId) {
    var id = membershipId.value().toString();
    correlationApi.correlateMessage(new CorrelateMessageCmd(
            Messages.MESSAGE_CONFIRMATION_REJECTED.getValue(),
            Map.of("membershipId", id),
            Correlation.withKey(id),
            CommonRestrictions.builder().withRestriction("useGlobalCorrelationKey", "true").build()
    )).join();
}
```

### 5. Bootstrap & Konfiguration

- `@EnableProcessApplication` von der `TrainingApplication` entfernen – der Adapter übernimmt Deployment und Worker-Registrierung.
- Einen `EngineCommandExecutor` als Bean bereitstellen, damit Engine- und Business-Daten in **einer** Transaktion committen:

  ```java
  @Bean
  public EngineCommandExecutor engineCommandExecutor() {
      return new EngineCommandExecutor(Runnable::run);
  }
  ```

- In der `application.yaml` den Worker-/Adapter-Block ergänzen (Polling-Strategie `embedded_scheduled`):

  ```yaml
  dev:
    bpm-crafters:
      process-api:
        worker:
          deployment:
            enabled: true
            bpmnResourcePattern: "classpath*:/**/*.bpmn"
            dmnResourcePattern: "classpath*:/**/*.dmn"
        adapter:
          cib-seven-embedded:
            enabled: true
            service-tasks:
              delivery-strategy: embedded_scheduled
              worker-id: extra-task-1-worker
              schedule-delivery-fixed-rate-in-seconds: 5
  ```

### 6. Guardrail setzen

Ein ArchUnit-Test macht die Kernaussage prüfbar: **Nirgends** im Java-Code darf noch ein `org.cibseven.bpm`-Import stehen.

```java
@ArchTest
static final ArchRule no_class_should_depend_on_the_native_engine = noClasses()
        .should().dependOnClassesThat().resideInAPackage("org.cibseven.bpm..");
```

Wenn dieser Test grün ist, lebt die Engine nur noch in `pom.xml` und `application.yaml` – genau das, was beim Wechsel angefasst werden müsste.

## Testen

Die REST-Schnittstelle und das Verhalten sind identisch zu Aufgabe 8 – nur der Port ist ein anderer (`8090`). Service Tasks werden jetzt per Polling (~5 s) abgearbeitet, es kann also einen Moment dauern.

**Einfache Ablehnung (Alter außerhalb 21–29):**
```bash
MEMBERSHIP_ID=$(curl -s -X POST http://localhost:8090/api/memberships \
  -d '{"email": "grace@miravelo.com", "name": "Grace", "age": 35}' | tr -d '"')

curl -X POST http://localhost:8090/api/memberships/$MEMBERSHIP_ID/reject
```

Im Cockpit (`http://localhost:8090/camunda`, admin/admin):
1. Worker `sendConfirmationMail` feuert, User Task „Confirm membership" erscheint
2. Nach dem Reject läuft die Call Activity `handleRejection`, danach feuert über die Kompensation der Worker `revokeClaim` (Log: „Revoking claim …")

**VIP-Bewerber (Alter 21–29):**
```bash
MEMBERSHIP_ID=$(curl -s -X POST http://localhost:8090/api/memberships \
  -d '{"email": "hanna@miravelo.com", "name": "Hanna", "age": 25}' | tr -d '"')

curl -X POST http://localhost:8090/api/memberships/$MEMBERSHIP_ID/reject
```

## Kontrolle

- [ ] Alle sieben Service Tasks sind im BPMN `camunda:type="external"` mit Topic
- [ ] Es gibt keine `JavaDelegate`-Klassen mehr – nur noch `@ProcessEngineWorker`-Worker
- [ ] Der Outbound-Adapter nutzt `StartProcessApi` / `CorrelationApi` statt `RuntimeService`
- [ ] `@EnableProcessApplication` ist entfernt, der `EngineCommandExecutor`-Bean ist gesetzt
- [ ] Der ArchUnit-Test bestätigt: **0** `org.cibseven.bpm`-Abhängigkeiten im Code
- [ ] Fachliches Verhalten ist identisch zu Aufgabe 8

## Referenzlösung

`../solutions/extra-task-1/`

---

🦁 **Geschafft!** Dein Prozess ist jetzt engine-neutral. CIB Seven läuft weiter unter der Haube – aber dein Code weiß davon nichts mehr. Ein Engine-Wechsel ist damit kein Code-Umbau mehr, sondern ein Adapter-Tausch.
