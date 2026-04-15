package io.miragon.training.application.port.inbound

import io.miragon.training.domain.MembershipId

interface ReSendConfirmationMailUseCase {

    fun reSendConfirmationMail(membershipId: MembershipId)
}
