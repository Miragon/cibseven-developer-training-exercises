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
        // TODO Aufgabe 1:
        //  1. Lese die Variable "membershipId" aus der DelegateExecution
        //     Hinweis: execution.getVariable("membershipId") as String
        //  2. Erstelle eine MembershipId: MembershipId(UUID.fromString(...))
        //  3. Rufe useCase.sendWelcomeMail(membershipId) auf
        TODO("Aufgabe 1: Verbinde den Delegate mit dem Use Case")
    }
}
