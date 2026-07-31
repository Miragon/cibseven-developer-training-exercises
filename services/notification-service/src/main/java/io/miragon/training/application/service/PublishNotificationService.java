package io.miragon.training.application.service;

import io.miragon.training.application.port.inbound.PublishNotificationUseCase;
import io.miragon.training.application.port.outbound.NotificationPublisherOutPort;
import io.miragon.training.domain.Notification;
import org.springframework.stereotype.Service;

@Service
public class PublishNotificationService implements PublishNotificationUseCase {

    private final NotificationPublisherOutPort notificationPublisher;

    public PublishNotificationService(NotificationPublisherOutPort notificationPublisher) {
        this.notificationPublisher = notificationPublisher;
    }

    @Override
    public void publish(Notification notification) {
        // TODO Aufgabe 9: die Notification über den (bereits vorgegebenen) Out-Port veröffentlichen
        //   -> notificationPublisher.publish(notification);
        throw new UnsupportedOperationException("TODO Aufgabe 9: implement the service");
    }
}
