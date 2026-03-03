package io.miragon.training.application.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.miragon.training.application.port.inbound.SendConfirmationMailUseCase
import io.miragon.training.application.port.outbound.NewsletterSubscriptionRepository
import io.miragon.training.domain.SubscriptionId
import org.springframework.stereotype.Service

@Service
class SendConfirmationMailService(
    private val repository: NewsletterSubscriptionRepository,
) : SendConfirmationMailUseCase {

    private val log = KotlinLogging.logger {}

    override fun sendConfirmationMail(subscriptionId: SubscriptionId) {
        val subscription = repository.find(subscriptionId)
        log.info { "Sending confirmation mail to ${subscription.email}" }
    }
}
