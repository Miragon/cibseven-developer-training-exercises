package io.miragon.training.application.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.miragon.training.application.port.inbound.ClaimMembershipUseCase
import io.miragon.training.application.port.outbound.MembershipRepository
import io.miragon.training.domain.MembershipId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.atomic.AtomicInteger

@Service
@Transactional
class ClaimMembershipService(
    private val repository: MembershipRepository,
) : ClaimMembershipUseCase {

    private val log = KotlinLogging.logger {}

    private val claimedSpots = AtomicInteger(0)
    private val maxSpots = 10

    override fun claimMembership(membershipId: MembershipId): Boolean {
        val membership = repository.find(membershipId)
        val claimed = claimedSpots.incrementAndGet()
        val hasSpot = claimed <= maxSpots
        log.info { "Claiming membership for ${membership.email.value} – spot $claimed/$maxSpots, hasSpot=$hasSpot" }
        return hasSpot
    }
}
