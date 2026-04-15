package io.miragon.training.adapter.inbound.cibseven

import io.miragon.training.application.port.inbound.ReSendConfirmationMailUseCase
import io.miragon.training.domain.MembershipId
import org.cibseven.bpm.engine.delegate.DelegateExecution
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ReSendConfirmationMailDelegate(
    private val useCase: ReSendConfirmationMailUseCase,
) : BaseDelegate() {

    override fun executeTask(execution: DelegateExecution) {
        val membershipId = execution.getVariable("membershipId") as String
        log.debug { "Received task to re-send confirmation mail for membership: $membershipId" }
        useCase.reSendConfirmationMail(MembershipId(UUID.fromString(membershipId)))
    }
}
