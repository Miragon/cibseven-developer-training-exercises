package io.miragon.training.application.port.outbound

import io.miragon.training.domain.Membership
import io.miragon.training.domain.MembershipId

interface MembershipRepository {

    fun find(id: MembershipId): Membership

    fun save(membership: Membership): Membership
}
