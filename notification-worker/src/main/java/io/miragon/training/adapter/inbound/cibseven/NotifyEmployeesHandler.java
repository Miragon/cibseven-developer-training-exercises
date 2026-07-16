package io.miragon.training.adapter.inbound.cibseven;

import io.miragon.training.application.port.inbound.NotifyEmployeesUseCase;
import org.cibseven.bpm.client.task.ExternalTask;
import org.cibseven.bpm.client.task.ExternalTaskHandler;
import org.cibseven.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Remote external-task worker. Connects to the engine REST API (see {@code camunda.bpm.client}
 * config), fetches and locks tasks of the {@code notifyEmployees} topic, announces the new member,
 * and completes the task.
 */
@Component
// TODO Aufgabe 6: subscribe this handler to the external-task topic "notifyEmployees"
//  Hint: annotate the class with @ExternalTaskSubscription(topicName = "notifyEmployees")
//        (org.cibseven.bpm.client.spring.annotation.ExternalTaskSubscription)
public class NotifyEmployeesHandler implements ExternalTaskHandler {

    private static final Logger log = LoggerFactory.getLogger(NotifyEmployeesHandler.class);

    private final NotifyEmployeesUseCase useCase;

    public NotifyEmployeesHandler(NotifyEmployeesUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        // TODO Aufgabe 6:
        //  1. Read the "name" and "email" process variables (externalTask.getVariable(...))
        //  2. Announce the member: useCase.notify(new NewMember(name, email, LocalDateTime.now().toString()))
        //  3. Complete the external task: externalTaskService.complete(externalTask)
        throw new UnsupportedOperationException("TODO Aufgabe 6: implement the external task handler");
    }
}
