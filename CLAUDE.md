# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Start PostgreSQL (required before running the app)
cd stack && docker-compose up -d

# Build
./mvnw clean install

# Run exercises starter (http://localhost:8080)
cd exercises && ../mvnw spring-boot:run

# Run a specific solution
cd solutions/exercise-1 && ../../mvnw spring-boot:run

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
- `exercises/` — Starter template with TODO placeholders for participants
- `solutions/exercise-{0-7}/` — Cumulative solutions, each building on the previous
- `models/` — Reference BPMN/DMN models

## Domain

Exercises 0-2: Newsletter subscription (Subscription naming).
Exercises 3-7: Miravelo Inner Circle membership (Membership naming).

Workflow: subscribe → send confirmation mail → wait for confirmation (with timer retry/abort) → send welcome mail.

## Architecture Rules (ArchUnit)

Architecture constraints are verified at test time. Domain classes must have zero framework imports; adapters must only depend on application ports. Run `./mvnw test -Dtest=ArchitectureTest` to verify.
