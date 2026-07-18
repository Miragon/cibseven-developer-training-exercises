package io.miragon.training.adapter.inbound.cibseven;

import io.miragon.training.application.port.inbound.PublishNotificationUseCase;
import io.miragon.training.domain.Notification;
import org.cibseven.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.cibseven.bpm.client.task.ExternalTask;
import org.cibseven.bpm.client.task.ExternalTaskHandler;
import org.cibseven.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Remote external-task worker: subscribes to the {@code notifyEmployees} topic, turns the locked
 * task into a {@link Notification}, publishes it, and completes the task.
 */
@Component
@ExternalTaskSubscription(topicName = "notifyEmployees")
public class NotifyEmployeesHandler implements ExternalTaskHandler {

    private static final Logger log = LoggerFactory.getLogger(NotifyEmployeesHandler.class);

    private final PublishNotificationUseCase publishNotification;

    public NotifyEmployeesHandler(PublishNotificationUseCase publishNotification) {
        this.publishNotification = publishNotification;
    }

    @Override
    public void execute(ExternalTask task, ExternalTaskService taskService) {
        String name = task.getVariable("name");
        log.info("Locked external task {} for new member {}", task.getId(), name);

        publishNotification.publish(new Notification(
                "Miravelo Inner Circle",
                "🎉 New Inner Circle member: " + name + "!"));

        taskService.complete(task);
    }
}
