package io.miragon.training.application.port.inbound

import io.miragon.training.domain.MembershipId

interface RevokeClaimUseCase {

    fun revokeClaim(membershipId: MembershipId)
}
