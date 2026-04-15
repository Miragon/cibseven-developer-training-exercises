package io.miragon.training.adapter.inbound.cibseven

import io.miragon.training.application.port.inbound.ClaimMembershipUseCase
import io.miragon.training.domain.MembershipId
import org.cibseven.bpm.engine.delegate.DelegateExecution
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ClaimMembershipDelegate(
    private val useCase: ClaimMembershipUseCase,
) : BaseDelegate() {

    override fun executeTask(execution: DelegateExecution) {
        val membershipId = execution.getVariable("membershipId") as String
        log.debug { "Received task to claim membership: $membershipId" }
        val hasEmptySpots = useCase.claimMembership(MembershipId(UUID.fromString(membershipId)))
        execution.setVariable("hasEmptySpots", hasEmptySpots)
    }
}
