# CIB Seven Developer Training Exercises

Practical exercises for the CIB Seven developer training. The project implements a newsletter subscription / membership workflow using CIB Seven as the process engine and a hexagonal architecture to keep business logic decoupled from infrastructure.

---

## Repository Structure

```
cibseven-developer-training-exercises/
├── exercises/                        # Starter template with TODOs
│   ├── docs/                         # Exercise descriptions (exercise-0.md … exercise-7.md)
│   └── src/main/java/io/miragon/training/
│       ├── adapter/
│       │   ├── inbound/
│       │   │   ├── cibseven/         # JavaDelegate implementations
│       │   │   └── rest/             # REST controllers
│       │   └── outbound/
│       │       ├── cibseven/         # Process engine adapter (start/correlate)
│       │       └── db/               # JPA persistence adapter
│       ├── application/
│       │   ├── port/
│       │   │   ├── inbound/          # Use case interfaces
│       │   │   └── outbound/         # Repository and process port interfaces
│       │   └── service/              # Use case implementations
│       └── domain/                   # Domain model (pure Java, no framework deps)
├── solutions/                        # Cumulative solutions per exercise
│   └── exercise-{0-7}/
├── models/                           # Reference BPMN/DMN models
├── stack/
│   ├── docker-compose.yml            # PostgreSQL + MailHog
│   └── init-schemas.sql
└── pom.xml
```

---

## Technical Stack

| Component | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Process Engine | CIB Seven 2.1 |
| Database | PostgreSQL (JPA / Hibernate) |
| Build | Maven |
| Architecture tests | ArchUnit |

---

## CIB Seven

[CIB Seven](https://cibseven.org) is a community-maintained distribution of Camunda Platform 7. It provides full compatibility with the Camunda 7 API while being independently maintained and open-source.

In this project CIB Seven runs embedded inside Spring Boot, exposes the Camunda web application at `http://localhost:8080/camunda`, and handles BPMN process execution.

Service tasks are implemented using the `JavaDelegate` pattern via `DelegateExpression`:

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

---

## Architecture

The project follows a **hexagonal architecture** (ports & adapters):

```
REST / JavaDelegates           Application              CIB7 / Database
  (inbound adapters)   →   ports + services   →     (outbound adapters)
                               ↑
                            Domain
                        (engine-neutral)
```

Architecture rules are enforced at build time via [ArchUnit](https://www.archunit.org/) tests.

---

## Exercises

### Background: Miravelo

**Miravelo** ist ein Lifestyle-Online-Shop für Menschen in der Quarterlife-Crisis — Gravel Bikes, Rennräder und alles, was dazugehört. Das Unternehmen wächst und setzt zunehmend auf automatisierte Prozesse.

Die Übungen finden im Kontext des **Newsletter-Anmeldeprozesses** statt. Ab Aufgabe 3 wird daraus der exklusive **Miravelo Inner Circle** mit limitierter Membership.

Detaillierte Aufgabenbeschreibungen befinden sich in `exercises/docs/`.

| Aufgabe | Thema | Beschreibung |
|---|---|---|
| 0 | BPMN Modellierung | Camunda Modeler kennenlernen, einfachen Prozess modellieren |
| 1 | Prozess-Automatisierung | JavaDelegate, RuntimeService, REST-Endpoint |
| 2 | Bestätigungs-Mail | Double-Opt-In, weitere Service Tasks |
| 3 | Membership & Gateway | Exclusive Gateway, Kapazitätsprüfung, Domain-Refactoring |
| 4 | Boundary Events & Subprozesse | Timer, Message Events, Subprocess |
| 5 | Signal Events | Signal End/Start Events, Event-Publishing |
| 6 | Call Activity & DMN | Call Activity, DMN-Entscheidungstabelle, Business Rule Task |
| 7 | Kompensation (SAGA) | Compensation Boundary Events, automatisches Rollback |

---

## Quick Start

```bash
# Start PostgreSQL
cd stack && docker-compose up -d

# Build everything
./mvnw clean install

# Run exercises starter
cd exercises && ../mvnw spring-boot:run

# CIB Seven Cockpit
open http://localhost:8080/camunda    # admin / admin
```
