# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Start PostgreSQL (required before running the app)
cd stack && docker-compose up -d

# Build
./mvnw clean install

# Run the process-application module (http://localhost:8080) — the main module participants work in
cd services/process-application && ../../mvnw spring-boot:run

# Run a specific solution
cd solutions/exercise-01 && ../../mvnw spring-boot:run

# Load a reference solution into the process-application module (catch-up; valid: 01-09, two-digit)
./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=02

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=<TestClassName>
```

CIB Seven Cockpit: `http://localhost:8080/camunda` (admin/admin)

## Architecture

Hexagonal architecture (ports & adapters) enforced at build time via ArchUnit tests:

```
REST / JavaDelegates           Application              CIB7 / Database
  (inbound adapters)   →   ports + services   →     (outbound adapters)
                               ↑
                            Domain
                        (engine-neutral)
```

**Package layout** under `src/main/java/io/miragon/training/`:

- `adapter/inbound/rest/` — Spring MVC REST controllers
- `adapter/inbound/cibseven/` — JavaDelegate implementations (`DelegateExpression`)
- `adapter/outbound/cibseven/` — Process engine adapter (start process instances, correlate messages)
- `adapter/outbound/db/` — JPA persistence adapter
- `application/port/inbound/` — Use case interfaces
- `application/port/outbound/` — Repository and process port interfaces
- `application/service/` — Use case implementations
- `domain/` — Pure Java domain model (records), no framework dependencies

## Key Technologies

- **CIB Seven** — Community distribution of Camunda Platform 7, runs embedded in Spring Boot
- **JavaDelegate** — Service tasks use `DelegateExpression` (e.g. `#{sendWelcomeMailDelegate}`) to bind to Spring beans
- **ArchUnit** — Architecture tests in `ArchitectureTest.java`

## Project Structure

Multi-module Maven project:
- `services/process-application/` — The main module participants work in. Ships in the Aufgabe-1 (Hybrid) state:
  full hexagonal skeleton present, but CIB deps/config/`@SpringBootApplication` (`TODO Aufgabe 1`)
  and the business-layer beans (`TODO Aufgabe 2`) are commented out. Exercise 1 = switch the
  engine on; Exercise 2 = uncomment the business layer + fill the TODOs.
- `services/notification-service/` — External-task worker service (Aufgabe 9); connects remotely to the
  engine REST API and processes the `notifyCommunity` topic. Ships with `TODO Aufgabe 9`.
- `docs/` — Per-exercise instructions (`exercise-00.md … exercise-09.md`) + assets.
- `solutions/exercise-{01-09}/` + `solutions/extra-task-1/` — Cumulative solutions, each building on the previous.
  Exercise 9 is nested into two sub-services: `solutions/exercise-09/process-application/` (main) +
  `solutions/exercise-09/notification-service/` (the external-task worker)
- `models/` — Reference BPMN/DMN models
- All modules (process-application + every solution) run on the same port (`8080`) and DB schema (`exercise`) —
  one module at a time. `stack/init-schemas.sql` creates just that one schema.
- The `load-solution` antrun task replaces `services/process-application/src/main` wholesale (Java, `application.yaml`,
  BPMN/DMN) from a solution; `src/test` and `pom.xml` are left untouched.

## Domain

Exercises 0-3: Newsletter subscription (Subscription naming).
Exercises 4-10: Miravelo Inner Circle membership (Membership naming).

Workflow: subscribe → send confirmation mail → wait for confirmation (with timer retry/abort) → send welcome mail.

## Architecture Rules (ArchUnit)

Architecture constraints are verified at test time. Domain classes must have zero framework imports; adapters must only depend on application ports. Run `./mvnw test -Dtest=ArchitectureTest` to verify.
