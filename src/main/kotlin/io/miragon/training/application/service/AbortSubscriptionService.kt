package io.miragon.training.application.service

import io.miragon.training.application.port.inbound.AbortSubscriptionUseCase
import io.miragon.training.application.port.outbound.NewsletterSubscriptionRepository
import io.miragon.training.domain.SubscriptionId
import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.springframework.stereotype.Service

@Service
@Transactional
class AbortSubscriptionService(
    private val repository: NewsletterSubscriptionRepository,
) : AbortSubscriptionUseCase {

    private val log = KotlinLogging.logger {}

    override fun abort(subscriptionId: SubscriptionId) {
        val subscription = repository.find(subscriptionId)
        subscription.abortRegistration()
        repository.save(subscription)
        log.info { "Aborted subscription-registration ${subscription.id}" }
    }
}
