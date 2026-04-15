package io.miragon.training.application.port.inbound

import io.miragon.training.domain.MembershipId

interface SendWelcomeMailUseCase {

    fun sendWelcomeMail(membershipId: MembershipId)
}
