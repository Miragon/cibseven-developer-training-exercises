package io.miragon.training.adapter.inbound.cibseven

import io.miragon.training.application.port.inbound.RevokeClaimUseCase
import io.miragon.training.domain.MembershipId
import org.cibseven.bpm.engine.delegate.DelegateExecution
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class RevokeClaimDelegate(
    private val useCase: RevokeClaimUseCase,
) : BaseDelegate() {

    override fun executeTask(execution: DelegateExecution) {
        val membershipId = execution.getVariable("membershipId") as String
        log.debug { "Received task to revoke membership claim: $membershipId" }
        useCase.revokeClaim(MembershipId(UUID.fromString(membershipId)))
    }
}
