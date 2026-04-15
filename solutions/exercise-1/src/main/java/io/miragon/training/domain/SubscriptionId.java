package io.miragon.training.domain;

import java.util.UUID;

public record SubscriptionId(UUID value) {
    public SubscriptionId() {
        this(UUID.randomUUID());
    }
}
