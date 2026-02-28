package io.miragon.training.adapter.outbound.db

import io.miragon.training.application.port.outbound.NewsletterSubscriptionRepository
import io.miragon.training.domain.NewsletterSubscription
import io.miragon.training.domain.SubscriptionId
import org.springframework.stereotype.Component

@Component
class NewsletterSubscriptionPersistenceAdapter(
    private val repository: NewsletterSubscriptionJpaRepository
) : NewsletterSubscriptionRepository {

    override fun find(subscriptionId: SubscriptionId): NewsletterSubscription {
        val entity = repository.findBySubscriptionId(subscriptionId.value) ?: throw NoSuchElementException()
        return NewsletterSubscriptionEntityMapper.toDomain(entity)
    }

    override fun search(subscriptionId: SubscriptionId): NewsletterSubscription? {
        val entity = repository.findBySubscriptionId(subscriptionId.value) ?: return null
        return NewsletterSubscriptionEntityMapper.toDomain(entity)
    }

    override fun save(subscription: NewsletterSubscription) {
        val entity = NewsletterSubscriptionEntityMapper.toEntity(subscription)
        repository.save(entity)
    }

    override fun delete(subscriptionId: SubscriptionId) {
        repository.deleteById(subscriptionId.value)
    }
}
