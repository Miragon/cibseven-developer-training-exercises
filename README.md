# CIB Seven Developer Training Exercises

Practical exercises for the CIB Seven developer training. The project implements a newsletter subscription workflow using CIB Seven as the process engine, the Process Engine API as an engine-neutral abstraction layer, and a hexagonal architecture to keep business logic decoupled from infrastructure.

---

## Repository Structure

```
cibseven-developer-training-exercises/
├── src/
│   ├── main/
│   │   ├── kotlin/io/miragon/training/
│   │   │   ├── adapter/
│   │   │   │   ├── inbound/
│   │   │   │   │   ├── cib7/       # Service task workers (Process Engine Worker)
│   │   │   │   │   └── rest/       # REST controllers
│   │   │   │   ├── outbound/
│   │   │   │   │   ├── cib7/       # Process engine adapter (start/correlate)
│   │   │   │   │   └── db/         # JPA persistence adapter
│   │   │   │   └── process/        # Generated BPMN process API constants
│   │   │   ├── application/
│   │   │   │   ├── port/
│   │   │   │   │   ├── inbound/    # Use case interfaces
│   │   │   │   │   └── outbound/   # Repository and process port interfaces
│   │   │   │   └── service/        # Use case implementations
│   │   │   └── domain/             # Domain model (pure Kotlin, no framework deps)
│   │   └── resources/
│   │       ├── application.yaml
│   │       └── bpmn/
│   │           └── newsletter.bpmn
│   └── test/
│       └── kotlin/io/miragon/training/
│           ├── KonsistArchitectureTest.kt
│           └── konsist/            # Architecture rule helpers (Konsist)
├── stack/
│   ├── docker-compose.yml          # PostgreSQL for local development
│   └── init-schemas.sql
├── build.gradle.kts
├── gradle/libs.versions.toml
└── settings.gradle.kts
```

---

## Technical Stack

| Component           | Technology                                  |
|---------------------|---------------------------------------------|
| Language            | Kotlin 2.3                                  |
| Framework           | Spring Boot 3.5                             |
| Process Engine      | CIB Seven 2.1                               |
| Process Engine API  | bpm-crafters process-engine-api 1.4         |
| Database            | PostgreSQL (JPA / Hibernate)                |
| Build               | Gradle 9.2 with version catalog             |
| Architecture tests  | Konsist                                     |

---

## CIB Seven

[CIB Seven](https://cibseven.org) is a community-maintained distribution of Camunda Platform 7. It provides full compatibility with the Camunda 7 API while being independently maintained and open-source.

In this project CIB Seven runs embedded inside Spring Boot, exposes the Camunda web application at `/camunda`, and handles BPMN process execution for the newsletter subscription workflow.

**Local access**
- Application: `http://localhost:8080`
- Cockpit / Tasklist: `http://localhost:8080/camunda`
- Credentials: `admin` / `admin`

---

## Process Engine API

[Process Engine API](https://github.com/bpm-crafters/process-engine-api) is an engine-neutral abstraction layer for BPMN process engines — similar in spirit to how JPA abstracts databases or Spring Cloud Stream abstracts messaging systems.

Key benefits:
- **Engine neutrality** — write your integration code once; swap between CIB Seven, Camunda 7, Camunda 8, or Operaton by changing configuration and dependencies, not business logic
- **Adapter pattern** — each supported engine has its own adapter; this project uses the CIB Seven embedded adapter
- **No engine lock-in** — the domain and application layers have zero knowledge of which engine is running underneath

### Process Engine Worker

Service tasks are handled using the [Process Engine Worker](https://github.com/bpm-crafters/process-engine-worker) library. It provides a `@ProcessEngineWorker` annotation that registers a method as a task handler for a given topic — no `JavaDelegate` coupling, no engine-specific interfaces in your business code.

```kotlin
@ProcessEngineWorker(topic = TaskTypes.SEND_WELCOME_MAIL)
fun sendWelcomeMail(@Variable subscriptionId: String): Map<String, Any> {
    useCase.sendWelcomeMail(SubscriptionId(UUID.fromString(subscriptionId)))
    return emptyMap()
}
```

Workers are delivered via embedded scheduled polling (configurable interval, default 5 seconds).

---

## Architecture

The project follows a **hexagonal architecture** (ports & adapters):

```
REST / CIB7 Workers          Application              CIB7 / Database
  (inbound adapters)   →   ports + services   →     (outbound adapters)
                               ↑
                            Domain
                        (engine-neutral)
```

Architecture rules are enforced at build time via [Konsist](https://github.com/LemonAppDev/konsist) tests (see `KonsistArchitectureTest`).

---

## Getting Started

**1. Start the database**
```bash
cd stack
docker-compose up -d
```

**2. Run the application**
```bash
./gradlew bootRun
```

**3. Trigger the newsletter subscription flow**

Subscribe:
```bash
curl -X POST http://localhost:8080/api/subscriptions/subscribe \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","name":"Jane Doe","newsletterId":"00000000-0000-0000-0000-000000000001"}'
```

Confirm (replace `<subscriptionId>` with the returned ID):
```bash
curl -X POST http://localhost:8080/api/subscriptions/confirm/<subscriptionId>
```

---

## Exercises

### Background: Miravelo

**Miravelo** is a company that sells bikes — gravel bikes, road bikes, and more. As the business grows, Miravelo increasingly relies on automated processes to handle their operations efficiently.

Their process landscape currently covers various areas such as:
- Order Fulfillment 
- Customer Onboarding 
- Customer Support 
- Newsletter Registration

This training takes place in the context of the **Newsletter Registration** process. Miravelo wants to automate how customers sign up for their newsletter — including sending a confirmation email, waiting for the customer to confirm, and eventually welcoming them or cleaning up if they never respond.

### Exercise Tasks

> Detailed step-by-step instructions will be added here.

---

## Configuration Reference

| Property | Value |
|---|---|
| Server port | `8080` |
| Database URL | `jdbc:postgresql://localhost:5432/cibseven-training` |
| Database credentials | `admin` / `admin` |
| Cockpit path | `/camunda` |
