package io.miragon.training.application.port.inbound

import io.miragon.training.domain.SubscriptionId

interface SendWelcomeMailUseCase {
    fun sendWelcomeMail(subscriptionId: SubscriptionId)
}
