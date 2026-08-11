package io.miragon.training.adapter.inbound.cibseven;

import org.cibseven.bpm.client.task.ExternalTask;
import org.cibseven.bpm.client.task.ExternalTaskService;

/**
 * TODO Aufgabe 9: Implementiere den External-Task-Worker für den "Ship welcome kit"-Task.
 *
 * <p>Hier steht bewusst nur das Gerüst – der Worker ist deine Aufgabe (nachdem du den Prozess angepasst
 * und die Process-API generiert hast):
 * <ol>
 *   <li>Mach die Klasse zu einer Spring-Bean ({@code @Component}) und abonniere den Topic mit
 *       {@code @ExternalTaskSubscription(topicName = SendWelcomeKitProcessApi.ServiceTasks.SHIP_WELCOME_KIT)}.
 *       Die Konstante entsteht erst, wenn du den Service-Task im Modell als External Task mit Topic
 *       markiert und die Process-API neu generiert hast.</li>
 *   <li>Lies die Variable {@code name}, verschicke das Kit über {@code ShipWelcomeKitUseCase} (per
 *       Konstruktor injizieren) und schließe den Task ab:
 *       <pre>
 *   String name = task.getVariable("name");
 *   shipWelcomeKit.shipWelcomeKit(new Member(name));
 *   taskService.complete(task);
 *       </pre></li>
 * </ol>
 * Die gemeinsame Fehlerbehandlung steckt schon in {@link BaseExternalTaskWorker} – lass deinen Worker davon erben.
 */
public class ShipWelcomeKitWorker extends BaseExternalTaskWorker {

    @Override
    protected void executeTask(ExternalTask task, ExternalTaskService taskService) {
        throw new UnsupportedOperationException("TODO Aufgabe 9: implement the shipWelcomeKit worker");
    }
}
