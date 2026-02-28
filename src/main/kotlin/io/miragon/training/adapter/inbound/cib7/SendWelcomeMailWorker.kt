package io.miragon.training.adapter.inbound.cib7

import io.miragon.training.adapter.process.NewsletterSubscriptionProcessApi.TaskTypes
import io.miragon.training.application.port.inbound.SendWelcomeMailUseCase
import io.miragon.training.domain.SubscriptionId
import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import dev.bpmcrafters.processengine.worker.Variable
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.util.*

@Component
class SendWelcomeMailWorker(
    private val useCase: SendWelcomeMailUseCase
) {
    private val log = KotlinLogging.logger {}

    @ProcessEngineWorker(topic = TaskTypes.SEND_WELCOME_MAIL)
    fun sendWelcomeMail(@Variable subscriptionId: String): Map<String, Any> {
        log.debug { "Received task to send welcome mail for subscription: $subscriptionId" }

        //TODO: Link Worker to Business Logic
    }
}
