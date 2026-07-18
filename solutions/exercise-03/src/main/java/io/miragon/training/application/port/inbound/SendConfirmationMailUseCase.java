package io.miragon.training.application.port.inbound;

import io.miragon.training.domain.SubscriptionId;

public interface SendConfirmationMailUseCase {

    void sendConfirmationMail(SubscriptionId subscriptionId);
}
