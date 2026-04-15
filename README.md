# CIB Seven Developer Training Exercises

Praxisübungen für das CIB Seven Developer Training. Das Projekt implementiert einen Newsletter-Anmeldeprozess mit CIB Seven als Process Engine und einer hexagonalen Architektur, die Business-Logik von Infrastruktur entkoppelt.

---

## Repository-Struktur

```
cibseven-developer-training-exercises/
├── exercises/                        # Starter-Template mit TODOs
│   ├── docs/                         # Aufgabenbeschreibungen (exercise-0.md … exercise-7.md)
│   └── src/main/java/io/miragon/training/
│       ├── adapter/
│       │   ├── inbound/
│       │   │   ├── cibseven/         # JavaDelegate-Implementierungen
│       │   │   └── rest/             # REST-Controller
│       │   └── outbound/
│       │       ├── cibseven/         # Process-Engine-Adapter (Start/Korrelation)
│       │       └── db/               # JPA-Persistence-Adapter
│       ├── application/
│       │   ├── port/
│       │   │   ├── inbound/          # Use-Case-Interfaces
│       │   │   └── outbound/         # Repository- und Prozess-Port-Interfaces
│       │   └── service/              # Use-Case-Implementierungen
│       └── domain/                   # Domain-Modell (reines Java, keine Framework-Abhängigkeiten)
├── solutions/                        # Kumulative Lösungen pro Aufgabe
│   └── exercise-{0-7}/
├── models/                           # Referenz-BPMN-/DMN-Modelle
├── stack/
│   ├── docker-compose.yml            # PostgreSQL + MailHog
│   └── init-schemas.sql
└── pom.xml
```

---

## Technologie-Stack

| Komponente | Technologie |
|---|---|
| Sprache | Java 21 |
| Framework | Spring Boot 3.5 |
| Process Engine | CIB Seven 2.1 |
| Datenbank | PostgreSQL (JPA / Hibernate) |
| Build | Maven |
| Architektur-Tests | ArchUnit |

---

## CIB Seven

[CIB Seven](https://cibseven.org) ist eine community-gepflegte Distribution von Camunda Platform 7. Sie bietet volle Kompatibilität mit der Camunda-7-API und wird unabhängig als Open Source weiterentwickelt.

In diesem Projekt läuft CIB Seven eingebettet in Spring Boot, stellt die Camunda-Webanwendung unter `http://localhost:8080/camunda` bereit und übernimmt die BPMN-Prozessausführung für den Newsletter-Anmeldeprozess.

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

---

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

---

## Übungen

### Hintergrund: Miravelo

**Miravelo** ist ein Lifestyle-Online-Shop für Menschen in der Quarterlife-Crisis —
Gravel Bikes für die Wochenenden, die zählen, und Rennräder für alle, die den
Asphalt unter den Reifen spüren wollen.

Die Kundenbasis wächst. Neue Produkte kommen raus. Das Team beschließt:
Wir bauen einen **Newsletter**. Damit Kunden über neue Drops, Produkt-Launches
und exklusive Angebote informiert bleiben.
Klassisch. Bodenständig. Kein Schnickschnack.
Jemand trägt sich ein, kriegt eine Welcome Mail – fertig.

> *„Das ist doch in einer Stunde gebaut."*
> — Jeder Entwickler, der einen Newsletter unterschätzt hat.

Das Training findet im Kontext des **Newsletter-Anmeldeprozesses** statt.
Ab Aufgabe 3 wird aus dem einfachen Newsletter der exklusive **Miravelo Inner Circle** —
eine limitierte Membership für echte Fans der Marke. Gravel Bike in der Garage,
Halbmarathon im Kalender – du weißt, wen wir meinen.

Was folgt, ist eine Reise durch immer komplexere BPMN-Muster: Gateways, Boundary Events,
Subprozesse, Signals, Call Activities, DMN-Entscheidungstabellen und Kompensation —
jede Aufgabe baut auf der vorherigen auf.

![Prozessmodell](docs/newsletter-subscription.png)

### Aufgabenübersicht

Detaillierte Aufgabenbeschreibungen befinden sich in [`exercises/docs/`](exercises/docs/).

| Aufgabe | Thema | Beschreibung |
|---|---|---|
| 0 | BPMN-Modellierung | Camunda Modeler kennenlernen, einfachen Prozess modellieren |
| 1 | Prozess-Automatisierung | JavaDelegate, RuntimeService, REST-Endpoint |
| 2 | Bestätigungs-Mail | Double-Opt-In-Pattern, weitere Service Tasks |
| 3 | Membership & Gateway | Exclusive Gateway, Kapazitätsprüfung, Domain-Refactoring |
| 4 | Boundary Events & Subprozesse | Timer, Message Boundary Events, Subprocess |
| 5 | Signal Events | Signal End/Start Events, Event-Publishing |
| 6 | Call Activity & DMN | Call Activity, DMN-Entscheidungstabelle, Business Rule Task |
| 7 | Kompensation (SAGA) | Compensation Boundary Events, automatisches Rollback |

---

## Quick Start

```bash
# PostgreSQL starten
cd stack && docker-compose up -d

# Alles bauen
./mvnw clean install

# Exercises-Starter starten
cd exercises && ../mvnw spring-boot:run

# CIB Seven Cockpit
open http://localhost:8080/camunda    # admin / admin
```
