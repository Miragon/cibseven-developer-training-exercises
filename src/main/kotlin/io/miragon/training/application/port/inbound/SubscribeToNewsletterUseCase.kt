package io.miragon.training.application.port.inbound

import io.miragon.training.domain.Email
import io.miragon.training.domain.Name
import io.miragon.training.domain.NewsletterId
import io.miragon.training.domain.SubscriptionId

interface SubscribeToNewsletterUseCase {

    fun subscribe(command: Command): SubscriptionId

    data class Command(
        val email: Email,
        val name: Name,
        val newsletterId: NewsletterId
    )
}
