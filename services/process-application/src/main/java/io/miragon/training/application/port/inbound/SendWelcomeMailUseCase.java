package io.miragon.training.application.port.inbound;

import io.miragon.training.domain.SubscriptionId;

public interface SendWelcomeMailUseCase {

    void sendWelcomeMail(SubscriptionId subscriptionId);
}
