package io.miragon.training.application.port.outbound;

import io.miragon.training.domain.Subscription;
import io.miragon.training.domain.SubscriptionId;

public interface SubscriptionRepository {

    Subscription find(SubscriptionId id);

    Subscription save(Subscription subscription);
}
