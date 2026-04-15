package io.miragon.training.application.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.miragon.training.application.port.inbound.RevokeClaimUseCase
import io.miragon.training.application.port.outbound.MembershipRepository
import io.miragon.training.domain.MembershipId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RevokeClaimService(
    private val repository: MembershipRepository,
) : RevokeClaimUseCase {

    private val log = KotlinLogging.logger {}

    override fun revokeClaim(membershipId: MembershipId) {
        val membership = repository.find(membershipId)
        log.info { "Revoking membership claim for ${membership.email.value} (membershipId=${membershipId.value})" }
    }
}
