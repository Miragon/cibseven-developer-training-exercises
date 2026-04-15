package io.miragon.training.application.port.inbound

import io.miragon.training.domain.MembershipId

interface SendRejectionMailUseCase {

    fun sendRejectionMail(membershipId: MembershipId)
}
