package io.miragon.training;

import org.springframework.boot.SpringApplication;
// TODO Aufgabe 1: Imports einkommentieren
// import org.springframework.boot.autoconfigure.SpringBootApplication;
// import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Einstiegspunkt der Anwendung.
 *
 * <p>TODO Aufgabe 1: Aktiviere die Annotationen {@code @SpringBootApplication} und
 * {@code @EnableJpaRepositories}. Erst dadurch greifen Spring-Boot-Auto-Configuration und
 * das automatische BPMN-Deployment der CIB-Seven-Engine (alle {@code *.bpmn} unter
 * {@code src/main/resources} werden beim Start deployt). Ohne die Annotationen startet nur
 * ein leerer Kontext – keine Engine, kein Cockpit.
 */
// TODO Aufgabe 1: Annotationen einkommentieren
// @SpringBootApplication
// @EnableJpaRepositories
public class TrainingApplication {
    public static void main(String[] args) {
        SpringApplication.run(TrainingApplication.class, args);
    }
}
