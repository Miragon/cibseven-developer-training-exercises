package io.miragon.training.application.port.outbound

import io.miragon.training.domain.Membership
import io.miragon.training.domain.MembershipId

interface MembershipProcess {

    fun startProcess(membership: Membership)

    fun rejectMembership(membershipId: MembershipId)
}
