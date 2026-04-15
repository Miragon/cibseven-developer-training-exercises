package io.miragon.training.application.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.miragon.training.application.port.inbound.NotifyAboutSignedMembershipUseCase
import io.miragon.training.application.port.outbound.MembershipRepository
import io.miragon.training.domain.MembershipId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class NotifyAboutSignedMembershipService(
    private val repository: MembershipRepository,
) : NotifyAboutSignedMembershipUseCase {

    private val log = KotlinLogging.logger {}

    override fun notifyAboutSignedMembership(membershipId: MembershipId) {
        val membership = repository.find(membershipId)
        log.info { "Publishing signed membership event for ${membership.email.value} (membershipId=${membershipId.value})" }
    }
}
