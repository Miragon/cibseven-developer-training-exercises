# Aufgabe 1 – Engine & Tooling: das Modul zum Laufen bringen

## Ziel-Modell

![BPMN Modell der Aufgabe](assets/exercise-1.svg)

## Lernziele

- Ein CIB-Seven-Spring-Boot-Modul **lauffähig machen**: Dependencies, Annotation, Konfiguration
- Verstehen, was die Engine zum Starten braucht (DB, Auto-Configuration, Auto-Deployment)
- Das **Cockpit** kennenlernen: Processes, Tasklist, History
- Die von der Engine **automatisch angelegten Datenbank-Tabellen** (`act_*`) ansehen
- Eine Prozessinstanz starten und einen User Task bearbeiten

## Hintergrund

In Aufgabe 0 hast du den Newsletter **fachlich** modelliert. Ein externer Consultant,
mit dem Miravelo zusammenarbeitet, hat das Modell **technisch fertig modelliert** –
inklusive einer kleinen Logik am Service Task „Send Welcome Mail": Er setzt per
Inline-**Expression** einfach eine Prozessvariable `welcomeMailSentTo` (kein Java nötig,
den echten Versand bauen wir später).

Was jetzt noch fehlt, ist die **Laufzeitumgebung**: Ein Spring-Boot-Modul, in dem die
CIB-Seven-Engine läuft. Genau das richtest du in dieser Aufgabe ein – und lernst dabei
Engine, Cockpit und Datenbank kennen.

### Ausgangslage

Du arbeitest ab jetzt im Modul **`exercise/`** – das ist das einzige Modul, in dem du
alle Aufgaben bearbeitest. Es enthält bereits das komplette Skelett der hexagonalen
Architektur (für die späteren Aufgaben), im Auslieferungszustand ist die CIB-Seven-Engine
aber **noch nicht scharf geschaltet**: Dependencies, Konfiguration und die
`@SpringBootApplication`-Annotation sind auskommentiert. Deine Aufgabe: alles einschalten.
Die Versionen der Dependencies kommen zentral aus der Root-`pom.xml`, du fügst sie also
**ohne** Versionsnummer hinzu.

> Die eigentliche Business-Logik (Services, REST-Controller, Delegate, Prozess-Adapter) ist
> mit `TODO Aufgabe 2` auskommentiert – die brauchst du in dieser Aufgabe noch nicht.

## Aufgabe

> Es geht ums **Einrichten & Kennenlernen** – kein Business-Code.

### 1. Datenbank & Stack starten

```bash
cd stack && docker-compose up -d
```

Startet PostgreSQL (und MailHog). Die Engine legt sich beim ersten Start ihr eigenes
Schema an.

### 2. Dependencies hinzufügen

Öffne `exercise/pom.xml` und **kommentiere den `TODO Aufgabe 1`-Block ein**: die
CIB-Seven-Starter (`webapp-4` + `rest-4`). Erst damit sind Engine, Cockpit-Webapp und
REST-API überhaupt vorhanden. (`spring-boot-starter-data-jpa` und der `postgresql`-Treiber
sind bereits aktiv.)

### 3. Konfiguration setzen

Kommentiere in `services/process-application/src/main/resources/application.yaml` die Konfiguration ein:
Datenbank-Anbindung (PostgreSQL aus dem Stack), Cockpit-Admin-User und Webclient.
Ohne DB-Verbindung startet die Engine nicht.

### 4. Deployment-Annotation setzen

In `services/process-application/src/main/java/io/miragon/training/TrainingApplication.java`:
aktiviere die Imports und Annotationen **`@SpringBootApplication`** und
**`@EnableJpaRepositories`**. Erst dadurch greifen Auto-Configuration und das automatische
BPMN-Deployment – alle `*.bpmn` unter `src/main/resources` werden beim Start deployt.

### 5. Los geht's – starten

```bash
cd services/process-application && ../../mvnw spring-boot:run
```

Im Log sollte `Auto-Deploying resources: [... newsletter.bpmn]` und
`Started TrainingApplication` erscheinen.

### 6. Die Datenbank-Tabellen ansehen

Die Engine hat beim Start automatisch ihr Schema angelegt – Dutzende `act_*`-Tabellen:

```bash
docker exec -it postgres psql -U admin -d cibseven-training
```

| Tabelle | Präfix | Inhalt |
|---|---|---|
| `act_re_procdef` | `re` = Repository | Deployte **Prozessdefinitionen** (`subscribeNewsletter`) |
| `act_ru_execution` | `ru` = Runtime | **Laufende** Prozessinstanzen |
| `act_ru_task` | `ru` | Offene **User Tasks** |
| `act_ru_variable` | `ru` | **Prozessvariablen** laufender Instanzen |
| `act_hi_procinst` | `hi` = History | **Abgeschlossene** Prozessinstanzen |

```sql
SELECT key_, name_, version_ FROM exercise.act_re_procdef;
```

### 7. Cockpit öffnen

[http://localhost:8080/camunda](http://localhost:8080/camunda) (admin / admin).
Unter **Processes** erscheint `Subscribe Newsletter`. Klick dich durch **Cockpit**,
**Tasklist** und **Admin**.

### 8. Prozess starten & durchspielen

1. **Tasklist** → **Start process** → `Subscribe Newsletter`
2. User Task **„Fill out form"** öffnen, `email` / `name` / `age` ausfüllen, abschließen
3. Der Service Task setzt per Expression die Variable `welcomeMailSentTo` – sichtbar in
   der History und in der DB:
   ```sql
   SELECT name_, text_ FROM exercise.act_hi_varinst WHERE name_ = 'welcomeMailSentTo';
   ```
4. Die Instanz ist am End-Event „User subscribed" angekommen (History → COMPLETED)

## Kontrolle

- [ ] Postgres läuft (`docker-compose up -d`)
- [ ] Dependencies, Konfiguration und `@SpringBootApplication` sind gesetzt
- [ ] Die Anwendung startet, `newsletter.bpmn` wird deployt, das Cockpit ist erreichbar
- [ ] `Subscribe Newsletter` erscheint unter **Processes**
- [ ] Eine Instanz wurde gestartet, der User Task bearbeitet, der Prozess sauber beendet
- [ ] Du hast `act_re_procdef`, `act_ru_*` und `act_hi_*` in der DB gesehen

## Referenzlösung

- Fertiges, lauffähiges Modul: `../solutions/exercise-1/`
- Modell: `../models/task-1-basic-newsletter.bpmn`
- Du kannst die Lösung auch direkt in dein Modul laden:
  `./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=1`
  (ersetzt `src/main` inkl. `application.yaml`; nur die `pom.xml` – und damit die in Schritt 2
  aktivierten CIB-Deps – bleibt bestehen)

---

➡️ [Weiter zu Aufgabe 2](exercise-2.md)
