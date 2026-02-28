package io.miragon.training.application.port.inbound

import io.miragon.training.domain.SubscriptionId

interface SendConfirmationMailUseCase {
    fun sendConfirmationMail(subscriptionId: SubscriptionId)
}
