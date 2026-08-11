# Aufgabe 9 – Starter: `logistics-service`

Dies ist die **Vorlage** für den Logistik-Service aus Aufgabe 9 (Remote-Owner des `sendWelcomeKit`-Prozesses).
Sie liegt bewusst hier unter `templates/` und **nicht** in `services/` – so bleibt `services/` für die
Aufgaben 0–8 aufgeräumt.

## So legst du den Service in Aufgabe 9 an

1. Diesen Ordner nach `services/logistics-service` kopieren:

   ```bash
   cp -R templates/exercise-09/logistics-service services/logistics-service
   ```

2. Das Modul in der root `pom.xml` unter `<modules>` eintragen:

   ```xml
   <module>services/logistics-service</module>
   ```

3. Die `TODO Aufgabe 9`-Stellen ausfüllen (Details in [`docs/exercise-09.md`](../../../docs/exercise-09.md)):
   - **`pom.xml`** – den auskommentierten OpenAPI-Generator-Block einkommentieren und die beiden
     `TODO`-Werte setzen → erzeugt den typisierten `/engine-rest`-Client.
   - **`EngineClientConfig`** – die `ProcessDefinitionApi`-Bean bereitstellen.
   - **`EngineDeploymentAdapter`** – das eigene Modell beim Start deployen.
   - **`ShipWelcomeKitWorker`** – den External-Task erfüllen.
   - **`RemoteWelcomeKitProcessAdapter`** – den Prozess über den Client starten.

Referenzlösung: [`solutions/exercise-09/logistics-service`](../../../solutions/exercise-09/logistics-service).

> Tipp: Die Vorlage kompiliert schon im Ausgangszustand (der Client-Teil ist auskommentiert). Zum Prüfen
> der Vorlage ohne Kopieren: `./mvnw -Pexercise-9 -pl templates/exercise-09/logistics-service compile`.
