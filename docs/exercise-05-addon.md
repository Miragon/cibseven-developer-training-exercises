# Aufgabe 5 · Add-on – Von Strings zu typsicheren Konstanten (bpmn-to-code)

> Add-on zu [Aufgabe 5](exercise-05.md). Es kommt **kein neues Modell** dazu – du härtest den
> Prozess-Test, den du gerade geschrieben hast.

## Hintergrund

Dein Test ist grün – und trotzdem tickt in ihm eine kleine Zeitbombe. Zähl mal die String-Literale:
`"userTask_confirmMembership"`, `"serviceTask_sendWelcomeMail"`, `"endEvent_membershipConfirmed"` … jede
dieser IDs ist eine **handgetippte Kopie** einer ID aus dem BPMN. Benennt nächste Woche jemand
`userTask_confirmMembership` im Modeler um, merkt das **niemand**: Der Compiler ist zufrieden, der Test
prüft klaglos gegen eine ID, die es gar nicht mehr gibt – und wird grün oder rot aus dem falschen Grund.
Genau der Silent-Failure, den wir mit dem Test eigentlich *ausrotten* wollten.

Die Lösung ist [**bpmn-to-code**](https://github.com/Miragon/bpmn-to-code): ein Maven-Plugin, das aus
deinen BPMN-Dateien beim Build eine **typsichere Process-API** generiert – eine Java-Klasse pro Prozess,
in der jede Element-ID, jeder Message-Name und der Process-Key als Konstante steht
(`Elements.USER_TASK_CONFIRM_MEMBERSHIP`, `Messages.MESSAGE_SUBSCRIPTION_REQUESTED`, `PROCESS_ID`).
Umbenennen im Modeler → nächster Build regeneriert → aus dem stillen Laufzeit-Fehler wird ein
**Compilerfehler**, den du sofort siehst.

> Ehrlich gesagt hätte sich das schon vorher gelohnt – der Outbound-Adapter korreliert seine Nachricht
> per `"Message_SubscriptionRequested"`, auch so ein handgetippter String. Aber beim **Testen** zahlt es
> sich am meisten aus: Kein anderer Code referenziert so viele Element-IDs auf einmal wie ein Prozess-Test.

## Aufgaben

### a) Plugin + Runtime in die `pom.xml`

Version zentral aus der Root-`pom.xml`, im Modul also ohne `<version>`:

```xml
<!-- dependencies -->
<dependency>
    <groupId>io.miragon</groupId>
    <artifactId>bpmn-to-code-runtime</artifactId>
</dependency>
```

```xml
<!-- build > plugins -->
<plugin>
    <groupId>io.miragon</groupId>
    <artifactId>bpmn-to-code-maven</artifactId>
    <executions>
        <execution>
            <id>generate-process-api</id>
            <phase>generate-sources</phase>
            <goals><goal>generate-bpmn-api</goal></goals>
        </execution>
    </executions>
    <configuration>
        <baseDir>${project.basedir}</baseDir>
        <filePattern>src/main/resources/bpmn/*.bpmn</filePattern>
        <outputFolderPath>${project.basedir}/src/main/java</outputFolderPath>
        <packagePath>io.miragon.training.adapter.process</packagePath>
        <outputLanguage>JAVA</outputLanguage>
        <processEngine>CAMUNDA_7</processEngine>
    </configuration>
</plugin>
```

### b) Generieren

Das Plugin läuft in der Phase `generate-sources` – ein Build genügt:

```bash
../../mvnw -pl services/process-application generate-sources
```

Danach liegt `io.miragon.training.adapter.process.SubscribeNewsletterProcessApi` in `src/main/java`.

### c) Strings ersetzen

Die Wrapper-Typen (`ElementId`, `MessageName`, `ProcessId`) sind keine Strings –
in String-Kontexten rufst du `.getValue()` auf:

```java
import io.miragon.training.adapter.process.SubscribeNewsletterProcessApi.Elements;

assertThat(instance)
        .isEnded()
        .hasPassedInOrder(
                Elements.START_EVENT_SUBMIT_REGISTRATION.getValue(),
                Elements.SERVICE_TASK_CLAIM_MEMBERSHIP.getValue(),
                Elements.GATEWAY_HAS_EMPTY_SPOTS.getValue(),
                Elements.SERVICE_TASK_SEND_CONFIRMATION_MAIL.getValue(),
                Elements.USER_TASK_CONFIRM_MEMBERSHIP.getValue(),
                Elements.SERVICE_TASK_SEND_WELCOME_MAIL.getValue(),
                Elements.END_EVENT_MEMBERSHIP_CONFIRMED.getValue())
        .hasNotPassed(
                Elements.SERVICE_TASK_SEND_REJECTION_MAIL.getValue(),
                Elements.END_EVENT_MEMBERSHIP_REJECTED.getValue());
```

Ersetze genauso den Process-Key im `ProcessEngineTestUtils` und den Message-Namen im Outbound-Adapter:

```java
// ProcessEngineTestUtils: statt "subscribeNewsletter"
private static final String PROCESS_DEFINITION_KEY = SubscribeNewsletterProcessApi.PROCESS_ID.getValue();

// MembershipProcessAdapter: statt "Message_SubscriptionRequested"
runtimeService.createMessageCorrelation(Messages.MESSAGE_SUBSCRIPTION_REQUESTED.getValue()) ...
```

Ab hier gilt: **Ab dieser Aufgabe nutzen alle Lösungen die generierte Process-API** – jede weitere Stufe
(Boundary, Signal, Compensation, Call Activity) referenziert ihre neuen Elemente über Konstanten statt
über Strings.

> Variablen-Namen wie `"membershipId"` oder `"hasEmptySpots"` lassen wir hier bewusst als Strings stehen –
> die Process-API kann sie zwar auch typisieren, der Fokus dieses Add-ons sind aber die Element-IDs.

## Testen

```bash
../../mvnw -pl services/process-application test -Dtest=MembershipProcessTest
```

> **Gegenprobe:** Benenne testweise ein Element im `newsletter.bpmn` um, führe `generate-sources` aus –
> die entsprechende Konstante verschwindet und dein Test **kompiliert nicht mehr**. Genau das wollten wir.

> **Ausblick:** In der **Extra-Aufgabe** geht die Process-API noch einen Schritt weiter: Dort ersetzen
> engine-neutrale Worker die JavaDelegates und binden sich über `ServiceTasks`-Konstanten aus genau
> dieser API an die Service Tasks – der native Engine-Import verschwindet komplett.

## Referenzlösung

`../solutions/exercise-05/`

---

➡️ [Weiter zu Aufgabe 6](exercise-06.md)
