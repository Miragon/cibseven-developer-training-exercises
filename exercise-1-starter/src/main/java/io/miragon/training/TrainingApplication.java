package io.miragon.training;

import org.springframework.boot.SpringApplication;
// TODO Aufgabe 1: Import einkommentieren
// import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Einstiegspunkt der Anwendung.
 *
 * <p>TODO Aufgabe 1: Aktiviere die Annotation {@code @SpringBootApplication}. Erst dadurch
 * greifen Spring-Boot-Auto-Configuration und das automatische BPMN-Deployment der
 * CIB-Seven-Engine (alle {@code *.bpmn} unter {@code src/main/resources} werden beim Start
 * deployt). Ohne die Annotation startet nur ein leerer Kontext – keine Engine, kein Cockpit.
 */
// TODO Aufgabe 1: Annotation einkommentieren
// @SpringBootApplication
public class TrainingApplication {
    public static void main(String[] args) {
        SpringApplication.run(TrainingApplication.class, args);
    }
}
