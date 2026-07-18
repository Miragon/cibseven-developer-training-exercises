package io.miragon.training.application.port.outbound;

import io.miragon.training.domain.Subscription;

public interface SubscriptionProcess {

    void startProcess(Subscription subscription);
}
