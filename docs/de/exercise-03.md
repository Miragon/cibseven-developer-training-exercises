# Aufgabe 3 – Einen Schritt automatisieren

> **Voraussetzung:** Aufgabe 2 ist abgeschlossen – „Confirm membership" ist ein User Task mit Formular.
> **Arbeitsverzeichnis:** `services/process-application`
> **Neu in dieser Aufgabe:** Manual Task → Service Task, JavaDelegate, Delegate Expression, hexagonale Architektur (Delegate → Use Case → Service).

## Darum geht es

Ein Platzhalter ist noch übrig: „Send welcome mail" läuft bisher als Manual Task einfach durch.
Jetzt soll er wirklich etwas tun. Dafür wird er zum **Service Task** – dem Element für Arbeit, die
ein **System** erledigt.

Damit die Engine weiß, *welchen* Code sie ausführen soll, bindest du den Service Task über eine
**Delegate Expression** an eine Spring-Bean: den **JavaDelegate**. Der Delegate ist der Punkt, an
dem die Engine in deinen Code zurückruft.

> *„Wir sind Entwickler. Ein Task, den niemand ausführt, ist eine To-do-Notiz, kein Prozess."*

## Lernziele

Nach dieser Aufgabe kannst du

- einen Manual Task in einen **Service Task** umwandeln,
- einen Service Task über eine **Delegate Expression** an eine Spring-Bean binden,
- einen **JavaDelegate** implementieren, der eine Prozessvariable liest und einen Use Case aufruft,
- die Schichten der hexagonalen Architektur einer Ausführung entlang benennen (Delegate → Use Case → Service).

## Ziel-Modell

![BPMN-Modell der Aufgabe](../assets/exercise-03.svg)

Referenzmodell: `../../models/exercise-03/membership.bpmn`

Gegenüber Aufgabe 2 ändert sich genau ein Element: aus dem Manual Task „Send welcome mail" wird der
Service Task `serviceTask_sendWelcomeMail`, gebunden an `#{sendWelcomeMailDelegate}`. Der Prozess
startet weiterhin über das Start-Formular im Cockpit.

## Aufgabe

### 1. Delegate-Schicht aktivieren

Die Klassen für diese Aufgabe sind mit `TODO Exercise 3` auskommentiert. Kommentiere sie ein:

- `application/port/inbound/SendWelcomeMailUseCase.java`
- `application/service/SendWelcomeMailService.java`
- `adapter/inbound/cibseven/BaseDelegate.java`
- `adapter/inbound/cibseven/SendWelcomeMailDelegate.java`

`SendWelcomeMailService` und `BaseDelegate` sind danach fertig; der Delegate trägt weiterhin ein
`TODO` – die Engine-Anbindung schreibst du selbst.

### 2. „Send welcome mail" in einen Service Task umwandeln

Ändere im Modeler den Typ des Elements von **Manual Task** auf **Service Task** und binde ihn an
den Delegate:

| Element | Typ | ID | Name | Konfiguration |
|---|---|---|---|---|
| Willkommens-Mail | Service Task | `serviceTask_sendWelcomeMail` | Send Welcome Mail | Delegate Expression: `#{sendWelcomeMailDelegate}` |

### 3. `SendWelcomeMailService` implementieren

**Datei:** `application/service/SendWelcomeMailService.java`

Der Service logt die E-Mail-Adresse, an die die Willkommens-Mail geht. Fachlogik gehört hierhin,
nicht in den Delegate.

### 4. `SendWelcomeMailDelegate` implementieren

**Datei:** `adapter/inbound/cibseven/SendWelcomeMailDelegate.java` – **selbst schreiben.**

Ersetze das `TODO` in `executeTask(execution)`:

- Lies die Prozessvariable `email` über die `DelegateExecution` aus (sie stammt aus dem
  Start-Formular).
- Rufe damit `useCase.sendWelcomeMail(...)` auf.

Welche Methode der `DelegateExecution` die Variable liefert, findest du selbst heraus – der
Aufgabentext nennt dir die API, nicht die fertige Zeile.

## Randbedingungen

- Der **Delegate** ist ein **Adapter**: Er liest Prozessvariablen und ruft einen Use Case auf.
  Fachlogik gehört in den Service, nicht in den Delegate.
- In dieser Aufgabe liest der Delegate die rohe `email` direkt aus der Prozessvariable. Eine
  fachliche Membership mit eigener ID und Persistenz kommt erst in Aufgabe 4 dazu.
- Gestartet und bestätigt wird weiterhin über das Cockpit – noch kein REST.

## Erwartetes Ergebnis

Starte die Anwendung neu und starte über die Tasklist eine Instanz (Start-Formular ausfüllen).
Schließe den User Task `Confirm membership` ab. Danach läuft der Service Task durch, und im Log
erscheint:

```
Sending welcome mail to alice@miravelo.com
```

Die Instanz endet an `Member joined`.

## Selbstcheck

- [ ] `serviceTask_sendWelcomeMail` ist ein Service Task und nutzt `#{sendWelcomeMailDelegate}`
- [ ] `SendWelcomeMailDelegate` ist selbst implementiert (kein `UnsupportedOperationException`-Stub mehr)
- [ ] Der Delegate liest `email` aus der `DelegateExecution` und ruft den Use Case auf
- [ ] Nach dem Abschließen des User Tasks steht die Log-Zeile mit der E-Mail-Adresse im Log
- [ ] Die Instanz endet an `Member joined`

## Hinweise

Warum die Trennung Delegate / Service? Der **Delegate** kennt die Engine (`DelegateExecution`,
Prozessvariablen); der **Service** kennt nur die Fachlichkeit. So bleibt die Fachlogik testbar,
ohne eine Engine zu starten – das nutzt du in Aufgabe 6 beim Prozess-Test aus.

## Referenzlösung

`../../solutions/exercise-03/` – oder direkt laden:

```bash
./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=03
```

## Nächster Schritt

In Aufgabe 4 übernimmt die **Anwendung** den Prozess: Registrierung per REST, Start über eine
Nachricht, Bestätigung über einen REST-Endpunkt.

➡️ [Weiter zu Aufgabe 4](exercise-04.md)
