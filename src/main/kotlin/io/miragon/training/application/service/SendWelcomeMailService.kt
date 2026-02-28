package io.miragon.training.application.service

import io.miragon.training.application.port.inbound.SendWelcomeMailUseCase
import io.miragon.training.application.port.outbound.NewsletterSubscriptionRepository
import io.miragon.training.domain.SubscriptionId
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SendWelcomeMailService(
    private val repository: NewsletterSubscriptionRepository,
) : SendWelcomeMailUseCase {

    private val log = KotlinLogging.logger {}

    override fun sendWelcomeMail(subscriptionId: SubscriptionId) {
        val subscription = repository.find(subscriptionId)
        log.info { "Sending welcome mail to ${subscription.email}" }
    }
}
