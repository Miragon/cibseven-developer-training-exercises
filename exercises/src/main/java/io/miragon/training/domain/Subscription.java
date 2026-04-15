package io.miragon.training.domain;

public record Subscription(SubscriptionId id, Email email, Name name, Age age) {
    public Subscription(Email email, Name name, Age age) {
        this(new SubscriptionId(), email, name, age);
    }
}
