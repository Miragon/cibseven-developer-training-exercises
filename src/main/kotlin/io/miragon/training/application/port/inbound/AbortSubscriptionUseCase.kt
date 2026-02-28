package io.miragon.training.application.port.inbound

import io.miragon.training.domain.SubscriptionId

interface AbortSubscriptionUseCase {
    fun abort(subscriptionId: SubscriptionId)
}
