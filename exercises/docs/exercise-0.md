# Aufgabe 0 – BPMN Modellierung

## Lernziele

- Camunda Modeler installieren und kennenlernen
- Einen einfachen BPMN-Prozess modellieren
- Prozess-IDs und Element-IDs nach CIB7-Konvention vergeben
- Den Prozess im CIB Seven Cockpit deployen und testen

## Hintergrund

**Miravelo** ist ein aufstrebendes Startup, das eine exklusive Membership für Gleichgesinnte aufbaut.
Zur Einführung möchte das Team zunächst einen einfachen Newsletter-Anmeldeprozess modellieren,
bevor er in Aufgabe 1 automatisiert wird.

## Aufgabe

Modelliere mit dem **Camunda Modeler** einen einfachen Newsletter-Anmeldeprozess und speichere ihn
als `src/main/resources/bpmn/newsletter.bpmn`.

### Prozess-Schritte

```
[Newsletter wanted]  →  [Fill out form]  →  [Send Welcome Mail]  →  [User subscribed]
   (Start Event)          (User Task)         (Service Task)           (End Event)
```

### Anforderungen

| Element | Typ | ID | Name |
|---|---|---|---|
| Start-Event | None Start Event | `startEvent_newsletterWanted` | Newsletter wanted |
| Formular | User Task | `userTask_fillOutForm` | Fill out form |
| Welcome Mail | Service Task | `serviceTask_sendWelcomeMail` | Send Welcome Mail |
| End-Event | None End Event | `endEvent_userSubscribed` | User subscribed |

**Prozess-ID:** `subscribeNewsletter`

**Formular-Felder** (am User Task konfigurieren):
- `email` (String) – E-Mail-Adresse
- `name` (String) – Vollständiger Name
- `age` (Long) – Alter in Jahren

**Service Task Konfiguration:**
- Implementation: `Delegate Expression`
- Delegate Expression: `#{sendWelcomeMailDelegate}`

### Element-ID-Konventionen

| Präfix | Für |
|---|---|
| `startEvent_` | Start-Events |
| `endEvent_` | End-Events |
| `userTask_` | User Tasks |
| `serviceTask_` | Service Tasks |
| `gateway_` | Gateways |
| `subProcess_` | Subprozesse |
| `boundaryEvent_` | Boundary Events |

## Kontrolle

1. Anwendung starten: `../mvnw spring-boot:run`
2. Cockpit öffnen: [http://localhost:8080/camunda](http://localhost:8080/camunda) (admin / admin)
3. Unter **Processes** sollte `Subscribe Newsletter` erscheinen
4. Eine Prozessinstanz manuell starten und die UserTask ausfüllen

## Referenzlösung

`../models/task-1-basic-newsletter.bpmn`
