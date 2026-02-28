package io.miragon.training.application.port.inbound

import io.miragon.training.domain.SubscriptionId

interface ConfirmSubscriptionUseCase {
    fun confirm(subscriptionId: SubscriptionId)
}
