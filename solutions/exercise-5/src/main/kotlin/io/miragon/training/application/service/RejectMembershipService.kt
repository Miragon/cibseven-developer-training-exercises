package io.miragon.training.application.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.miragon.training.application.port.inbound.RejectMembershipUseCase
import io.miragon.training.application.port.outbound.MembershipProcess
import io.miragon.training.domain.MembershipId
import org.springframework.stereotype.Service

@Service
class RejectMembershipService(
    private val process: MembershipProcess,
) : RejectMembershipUseCase {

    private val log = KotlinLogging.logger {}

    override fun rejectMembership(membershipId: MembershipId) {
        log.info { "Rejecting membership confirmation for membershipId=${membershipId.value}" }
        process.rejectMembership(membershipId)
    }
}
