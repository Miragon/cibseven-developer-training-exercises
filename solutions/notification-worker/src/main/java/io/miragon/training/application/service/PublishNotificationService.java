package io.miragon.training.application.service;

import io.miragon.training.application.port.inbound.PublishNotificationUseCase;
import io.miragon.training.application.port.outbound.NotificationPublisherOutPort;
import io.miragon.training.domain.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PublishNotificationService implements PublishNotificationUseCase {

    private static final Logger log = LoggerFactory.getLogger(PublishNotificationService.class);

    private final NotificationPublisherOutPort notificationPublisher;

    public PublishNotificationService(NotificationPublisherOutPort notificationPublisher) {
        this.notificationPublisher = notificationPublisher;
    }

    @Override
    public void publish(Notification notification) {
        log.info("Publishing notification: {}", notification.title());
        notificationPublisher.publish(notification);
    }
}
