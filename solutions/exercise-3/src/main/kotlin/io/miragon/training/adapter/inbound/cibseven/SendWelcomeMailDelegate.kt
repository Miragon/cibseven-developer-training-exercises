package io.miragon.training.adapter.inbound.cibseven

import io.miragon.training.application.port.inbound.SendWelcomeMailUseCase
import io.miragon.training.domain.MembershipId
import org.cibseven.bpm.engine.delegate.DelegateExecution
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class SendWelcomeMailDelegate(
    private val useCase: SendWelcomeMailUseCase,
) : BaseDelegate() {

    override fun executeTask(execution: DelegateExecution) {
        val membershipId = execution.getVariable("membershipId") as String
        log.debug { "Received task to send welcome mail for membership: $membershipId" }
        useCase.sendWelcomeMail(MembershipId(UUID.fromString(membershipId)))
    }
}
