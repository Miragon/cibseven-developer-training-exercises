package io.miragon.training.adapter.inbound.cibseven;

import io.miragon.training.application.port.inbound.NotifyEmployeesUseCase;
import io.miragon.training.domain.NewMember;
import org.cibseven.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.cibseven.bpm.client.task.ExternalTask;
import org.cibseven.bpm.client.task.ExternalTaskHandler;
import org.cibseven.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Remote external-task worker. Connects to the engine REST API (see {@code camunda.bpm.client}
 * config), fetches and locks tasks of the {@code notifyEmployees} topic, announces the new member,
 * and completes the task.
 */
@Component
@ExternalTaskSubscription(topicName = "notifyEmployees")
public class NotifyEmployeesHandler implements ExternalTaskHandler {

    private static final Logger log = LoggerFactory.getLogger(NotifyEmployeesHandler.class);

    private final NotifyEmployeesUseCase useCase;

    public NotifyEmployeesHandler(NotifyEmployeesUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        String name = externalTask.getVariable("name");
        String email = externalTask.getVariable("email");
        log.info("Locked external task {} for new member {}", externalTask.getId(), name);

        useCase.notify(new NewMember(name, email, LocalDateTime.now().toString()));

        externalTaskService.complete(externalTask);
    }
}
