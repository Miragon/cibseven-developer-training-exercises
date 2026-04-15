package io.miragon.training.application.port.outbound

import io.miragon.training.domain.Membership

interface MembershipProcess {

    fun startProcess(membership: Membership)
}
