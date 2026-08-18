# CIB Seven Developer Training Exercises

Praxisübungen für das CIB Seven Developer Training. Das Projekt implementiert einen Newsletter-Anmeldeprozess mit CIB Seven als Process Engine und einer hexagonalen Architektur, die Business-Logik von Infrastruktur entkoppelt.

## Übungen

### Hintergrund: Miravelo

**Miravelo** ist ein Online-Shop für hochwertige Fahrräder — Gravel Bikes für lange
Wochenendtouren, Rennräder für alle, die es auf dem Asphalt schnell mögen. Die Kundschaft
ist jung, markenbewusst und ziemlich leidenschaftlich.

Der Shop wächst, neue Produkte kommen laufend dazu. Das Team beschließt:
Wir bauen einen **Newsletter**, damit Kundinnen und Kunden über Produkt-Launches und
exklusive Angebote informiert bleiben. Jemand trägt sich ein, bekommt eine
Willkommens-Mail – fertig.

> *„Das ist doch in einer Stunde gebaut."*
> — Jeder Entwickler, der einen Newsletter unterschätzt hat.

Das Training findet im Kontext des **Newsletter-Anmeldeprozesses** statt.
Ab Aufgabe 4 wird aus dem einfachen Newsletter der exklusive **Miravelo Inner Circle** —
eine auf tausend Plätze limitierte Membership für die treuesten Kundinnen und Kunden.

Was folgt, ist eine Reise durch immer komplexere BPMN-Muster: Gateways, Boundary Events,
Subprozesse, Parallel Gateways, Call Activities, DMN-Entscheidungstabellen und Kompensation —
jede Aufgabe baut auf der vorherigen auf.

![Prozessmodell](docs/newsletter-subscription.png)

### Aufgabenübersicht

Detaillierte Aufgabenbeschreibungen befinden sich in [`docs/`](docs/).

| Aufgabe | Thema | Beschreibung |
|---|---|---|
| [0](docs/exercise-00.md) | Fachliche BPMN-Modellierung | Miragon BPMN Modeler kennenlernen, Prozess rein fachlich modellieren |
| [1](docs/exercise-01.md) | Engine zum Laufen bringen | Modul scharf schalten, Cockpit und `act_*`-Tabellen der Engine kennenlernen |
| [2](docs/exercise-02.md) | Technische Modellierung & Automatisierung | Technisch modellieren, JavaDelegate, RuntimeService, REST-Endpoint |
| [3](docs/exercise-03.md) | Double-Opt-In | Message Start Event, Nachrichten-Korrelation, Bestätigungsschritt |
| [4](docs/exercise-04.md) | Kapazitätsprüfung mit Gateway | Exclusive Gateway, Domain-Refactoring, Transaktionsgrenzen, Business Key, Task-Formular |
| [5](docs/exercise-05.md) | Prozess-Tests | Prozess-Unit-Test mit In-Memory-Engine, gemockten Use Cases, ohne PostgreSQL |
| [5 · Add-on](docs/exercise-05-addon.md) | bpmn-to-code | Element-IDs als generierte Konstanten statt handgetippter Strings |
| [6](docs/exercise-06.md) | Subprozess, Boundary Events & Parallelität | Subprozess, Timer- und Message-Boundary-Events, Parallel Gateway, Teams-Anbindung |
| [7](docs/exercise-07.md) | Kompensation (SAGA) | Compensation Boundary Event, Compensating End Event, Kompensations-Handler |
| [8](docs/exercise-08.md) | Call Activity & DMN | Call Activity, DMN-Entscheidungstabelle, Business Rule Task |
| [9](docs/exercise-09.md) | Remote Engine als geteilte Infrastruktur | Eine Abteilung besitzt einen **eigenen** kleinen Prozess (`sendWelcomeKit`) in ihrem Remote-Service: Modell, Worker, Deployment und Tests liegen dort; getriggert per Signal-Broadcast; die Engine über einen generierten OpenAPI-Client getrieben |
| [Extra 1](docs/extra-task-1.md) | Process-Engine-API | Aufgabe 9 engine-neutral umbauen: Worker statt JavaDelegate, Adapter-Tausch statt Engine-Lock-in |

> Aufbau, Sprache und Qualitätskriterien der Aufgaben sind in
> [`docs/aufgaben-template.md`](docs/aufgaben-template.md) festgehalten. Neue oder geänderte
> Aufgaben werden mit dem Skill `aufgaben-review` gegengeprüft
> (`.claude/skills/aufgaben-review/SKILL.md`).

## Quick Start

```bash
# PostgreSQL starten
cd stack && docker-compose up -d

# Alles bauen
./mvnw clean install

# process-application-Modul starten (das eine Modul, in dem du alle Aufgaben bearbeitest)
cd services/process-application && ../../mvnw spring-boot:run

# CIB Seven Cockpit
open http://localhost:8080/webapp/#/seven/auth/start    # admin / admin
```

### Lösung einer Aufgabe laden

Das `process-application`-Modul startet im Zustand von **Aufgabe 1** (Engine noch auskommentiert). Wenn du
eine Aufgabe nicht ganz fertig bekommst, kannst du die Referenzlösung in dein `process-application`-Modul
kopieren und mit ihr weiterarbeiten:

```bash
# solutions/exercise-02 in das process-application-Modul kopieren (gültige Werte: 01–09, zweistellig)
./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=02
```

Der Task ersetzt `src/main` komplett (Java, `application.yaml`, BPMN/DMN); `src/test` bleibt
unberührt. Alle Module – das `process-application`-Modul **und** alle Solutions – laufen auf demselben Port
(`8080`) und demselben DB-Schema (`exercise`); es läuft also immer nur **ein** Modul zur Zeit.
Die in **Aufgabe 1** aktivierten CIB-Seven-Abhängigkeiten (`pom.xml`) bleiben bestehen – lade
eine Lösung ab `exercise-2` daher erst, nachdem Aufgabe 1 abgeschlossen ist.

## Repository-Struktur

```
cibseven-developer-training-exercises/
├── docs/                             # Aufgabenbeschreibungen (exercise-00.md … exercise-09.md) + Assets
├── services/                         # Die Services, an denen du arbeitest
│   ├── process-application/          # Prozess-Anwendung (startet im Zustand von Aufgabe 1)
│   │   └── src/main/java/io/miragon/training/
│   │       ├── adapter/
│   │       │   ├── inbound/
│   │       │   │   ├── cibseven/     # JavaDelegate-Implementierungen
│   │       │   │   └── rest/         # REST-Controller
│   │       │   └── outbound/
│   │       │       ├── cibseven/     # Process-Engine-Adapter (Start/Korrelation)
│   │       │       └── db/           # JPA-Persistence-Adapter
│   │       ├── application/
│   │       │   ├── port/
│   │       │   │   ├── inbound/      # Use-Case-Interfaces
│   │       │   │   └── outbound/     # Repository- und Prozess-Port-Interfaces
│   │       │   └── service/          # Use-Case-Implementierungen
│   │       └── domain/               # Domain-Modell (reines Java, keine Framework-Abhängigkeiten)
│   └── (logistics-service/)          # erst in Aufgabe 9 aus templates/ angelegt (Remote-Owner)
├── templates/
│   └── exercise-09/logistics-service/ # Vorlage für den Aufgabe-9-Worker (in services/ kopieren)
├── solutions/                        # Kumulative Lösungen pro Aufgabe (exercise-01 … exercise-09, extra-task-1)
│   ├── exercise-{01-09}/             # exercise-09/ ist verschachtelt: process-application/ + logistics-service/
│   └── extra-task-1/
├── models/                           # Referenz-BPMN-/DMN-Modelle
├── stack/
│   ├── docker-compose.yml            # PostgreSQL + MailHog
│   └── init-schemas.sql
└── pom.xml
```

## Technologie-Stack

| Komponente | Technologie |
|---|---|
| Sprache | Java 21 |
| Framework | Spring Boot 4 |
| Process Engine | CIB Seven 2.2.0 |
| Datenbank | PostgreSQL (JPA / Hibernate) |
| Build | Maven |
| Architektur-Tests | ArchUnit |

## CIB Seven

[CIB Seven](https://cibseven.org) ist eine community-gepflegte Distribution von Camunda Platform 7. Sie bietet volle Kompatibilität mit der Camunda-7-API und wird unabhängig als Open Source weiterentwickelt.

In diesem Projekt läuft CIB Seven eingebettet in Spring Boot, stellt die Camunda-Webanwendung unter `http://localhost:8080/webapp/#/seven/auth/start` bereit und übernimmt die BPMN-Prozessausführung für den Newsletter-Anmeldeprozess.

Service Tasks werden über das `JavaDelegate`-Pattern mit `DelegateExpression` angebunden:

```java
@Component
public class SendWelcomeMailDelegate extends BaseDelegate {

    private final SendWelcomeMailUseCase useCase;

    public SendWelcomeMailDelegate(SendWelcomeMailUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    protected void executeTask(DelegateExecution execution) {
        var subscriptionId = (String) execution.getVariable("subscriptionId");
        useCase.sendWelcomeMail(new SubscriptionId(UUID.fromString(subscriptionId)));
    }
}
```

## Architektur

Das Projekt folgt einer **hexagonalen Architektur** (Ports & Adapters):

```
REST / JavaDelegates           Application              CIB7 / Database
  (Inbound-Adapter)    →   Ports + Services   →     (Outbound-Adapter)
                               ↑
                            Domain
                        (engine-neutral)
```

Architekturregeln werden zur Build-Zeit über [ArchUnit](https://www.archunit.org/)-Tests sichergestellt.
