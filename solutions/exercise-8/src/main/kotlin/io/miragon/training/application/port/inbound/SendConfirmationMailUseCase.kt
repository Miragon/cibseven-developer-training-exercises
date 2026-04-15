package io.miragon.training.application.port.inbound

import io.miragon.training.domain.MembershipId

interface SendConfirmationMailUseCase {

    fun sendConfirmationMail(membershipId: MembershipId)
}
