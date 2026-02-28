package io.miragon.training.application.port.outbound

import io.miragon.training.domain.SubscriptionId

interface NewsletterSubscriptionProcess {
    fun submitForm(id: SubscriptionId)
    fun confirmSubscription(id: SubscriptionId)
}
