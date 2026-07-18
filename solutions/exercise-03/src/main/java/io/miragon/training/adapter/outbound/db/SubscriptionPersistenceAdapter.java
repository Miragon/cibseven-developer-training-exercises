package io.miragon.training.adapter.outbound.db;

import io.miragon.training.application.port.outbound.SubscriptionRepository;
import io.miragon.training.domain.Subscription;
import io.miragon.training.domain.SubscriptionId;
import org.springframework.stereotype.Component;

import java.util.NoSuchElementException;

@Component
public class SubscriptionPersistenceAdapter implements SubscriptionRepository {

    private final SubscriptionJpaRepository jpaRepository;
    private final SubscriptionEntityMapper mapper;

    public SubscriptionPersistenceAdapter(SubscriptionJpaRepository jpaRepository, SubscriptionEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Subscription find(SubscriptionId id) {
        var entity = jpaRepository.findById(id.value())
                .orElseThrow(() -> new NoSuchElementException("Subscription not found: " + id.value()));
        return mapper.toDomain(entity);
    }

    @Override
    public Subscription save(Subscription subscription) {
        var entity = mapper.toEntity(subscription);
        jpaRepository.save(entity);
        return subscription;
    }
}
