package io.miragon.training.adapter.inbound.cibseven;

import io.miragon.training.application.port.inbound.PublishNotificationUseCase;
import org.cibseven.bpm.client.task.ExternalTask;
import org.cibseven.bpm.client.task.ExternalTaskHandler;
import org.cibseven.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;

/**
 * Remote external-task worker: subscribes to the {@code notifyCommunity} topic, turns the locked
 * task into a {@link io.miragon.training.domain.Notification}, publishes it, and completes the task.
 */
@Component
// TODO Aufgabe 9: subscribe to the topic — @ExternalTaskSubscription(topicName = "notifyCommunity")
//                 (org.cibseven.bpm.client.spring.annotation.ExternalTaskSubscription)
public class NotifyCommunityHandler implements ExternalTaskHandler {

    private final PublishNotificationUseCase publishNotification;

    public NotifyCommunityHandler(PublishNotificationUseCase publishNotification) {
        this.publishNotification = publishNotification;
    }

    @Override
    public void execute(ExternalTask task, ExternalTaskService taskService) {
        // TODO Aufgabe 9:
        //   1. read the "name" task variable       -> task.getVariable("name")
        //   2. build a Notification                -> new Notification("Miravelo Inner Circle", "🎉 New Inner Circle member: " + name + "!")
        //   3. publish it                          -> publishNotification.publish(notification)
        //   4. complete the task                   -> taskService.complete(task)
        throw new UnsupportedOperationException("TODO Aufgabe 9: implement the external task handler");
    }
}
