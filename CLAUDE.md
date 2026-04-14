# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Start PostgreSQL (required before running the app)
cd stack && docker-compose up -d

# Build
./mvnw clean install

# Run application (http://localhost:8080)
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=<TestClassName>
```

CIB Seven Cockpit: `http://localhost:8080/camunda` (admin/admin)

## Architecture

Hexagonal architecture (ports & adapters) enforced at build time via Konsist tests:

```
REST / CIB7 Workers          Application              CIB7 / Database
  (inbound adapters)   →   ports + services   →     (outbound adapters)
                               ↑
                            Domain
                        (engine-neutral)
```

**Package layout** under `src/main/kotlin/io/miragon/training/`:

- `adapter/inbound/rest/` — Spring MVC REST controllers
- `adapter/inbound/cib7/` — BPMN service task workers (`@ProcessEngineWorker`)
- `adapter/outbound/cib7/` — Process engine adapter (start process instances, correlate messages)
- `adapter/outbound/db/` — JPA persistence adapter
- `adapter/process/` — Generated BPMN process API constants (message names, variable names, topic names)
- `application/port/inbound/` — Use case interfaces
- `application/port/outbound/` — Repository and process port interfaces
- `application/service/` — Use case implementations
- `domain/` — Pure Kotlin domain model, no framework dependencies

## Key Technologies

- **CIB Seven** — Community distribution of Camunda Platform 7, runs embedded in Spring Boot
- **Process Engine API** (`bpm-crafters/process-engine-api`) — Engine-neutral abstraction layer; allows swapping process engines (CIB Seven, Camunda 8, Operaton) without changing business code
- **Process Engine Worker** (`bpm-crafters/process-engine-worker`) — `@ProcessEngineWorker(topic = "...")` annotation registers a method as a BPMN service task handler; uses scheduled polling (5s interval)
- **Konsist** — Architecture tests in `src/test/kotlin/io/miragon/training/konsist/` enforced by `KonsistArchitectureTest`

## Domain

Newsletter subscription workflow: subscribe → send confirmation mail → wait for confirmation (with timer retry/abort) → send welcome mail. Domain aggregate is `NewsletterSubscription` with status `PENDING → CONFIRMED | ABORTED`.

## Architecture Rules (Konsist)

Architecture constraints are verified at test time. Domain classes must have zero framework imports; adapters must only depend on application ports (not directly on other adapters or domain internals beyond what ports expose). Run `./mvnw test -Dtest=KonsistArchitectureTest` to verify.
