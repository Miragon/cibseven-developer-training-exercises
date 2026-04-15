package io.miragon.training.application.port.inbound

import io.miragon.training.domain.MembershipId

interface ClaimMembershipUseCase {

    fun claimMembership(membershipId: MembershipId): Boolean
}
