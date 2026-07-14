package io.miragon.training.adapter.outbound.db;

import io.miragon.training.domain.Age;
import io.miragon.training.domain.Email;
import io.miragon.training.domain.Name;
import io.miragon.training.domain.Subscription;
import io.miragon.training.domain.SubscriptionId;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionEntityMapper {

    public SubscriptionEntity toEntity(Subscription subscription) {
        return new SubscriptionEntity(
                subscription.id().value(),
                subscription.email().value(),
                subscription.name().value(),
                subscription.age().value()
        );
    }

    public Subscription toDomain(SubscriptionEntity entity) {
        return new Subscription(
                new SubscriptionId(entity.getId()),
                new Email(entity.getEmail()),
                new Name(entity.getName()),
                new Age(entity.getAge())
        );
    }
}
