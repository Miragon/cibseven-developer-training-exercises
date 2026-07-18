package io.miragon.training.application.port.inbound;

import io.miragon.training.domain.Notification;

public interface PublishNotificationUseCase {

    void publish(Notification notification);
}
