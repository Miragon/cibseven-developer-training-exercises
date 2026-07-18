package io.miragon.training.application.port.inbound;

import io.miragon.training.domain.SubscriptionId;

public interface RegisterSubscriptionUseCase {

    SubscriptionId register(Command command);

    record Command(String email, String name, int age) {}
}
