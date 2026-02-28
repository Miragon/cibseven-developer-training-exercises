package io.miragon.training.application.port.outbound

import io.miragon.training.domain.NewsletterSubscription
import io.miragon.training.domain.SubscriptionId

interface NewsletterSubscriptionRepository {
    fun find(subscriptionId: SubscriptionId): NewsletterSubscription
    fun search(subscriptionId: SubscriptionId): NewsletterSubscription?
    fun save(subscription: NewsletterSubscription)
    fun delete(subscriptionId: SubscriptionId)
}
