package io.miragon.training.adapter.inbound.cib7

import io.miragon.training.adapter.process.NewsletterSubscriptionProcessApi.TaskTypes
import io.miragon.training.application.port.inbound.SendConfirmationMailUseCase
import io.miragon.training.domain.SubscriptionId
import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import dev.bpmcrafters.processengine.worker.Variable
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.util.*

@Component
class SendConfirmationMailWorker(
    private val useCase: SendConfirmationMailUseCase
) {
    private val log = KotlinLogging.logger {}

    @ProcessEngineWorker(topic = TaskTypes.SEND_CONFIRMATION_MAIL)
    fun sendConfirmationMail(@Variable subscriptionId: String) {
        log.debug { "Received task to send confirmation mail for subscription: $subscriptionId" }

        //TODO: Link Worker to Business Logic
    }
}
