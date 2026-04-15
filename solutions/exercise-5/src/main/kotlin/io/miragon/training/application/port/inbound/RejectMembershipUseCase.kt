package io.miragon.training.application.port.inbound

import io.miragon.training.domain.MembershipId

interface RejectMembershipUseCase {

    fun rejectMembership(membershipId: MembershipId)
}
