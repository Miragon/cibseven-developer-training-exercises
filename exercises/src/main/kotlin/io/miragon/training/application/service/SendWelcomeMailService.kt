package io.miragon.training.application.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.miragon.training.application.port.inbound.SendWelcomeMailUseCase
import io.miragon.training.application.port.outbound.MembershipRepository
import io.miragon.training.domain.MembershipId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SendWelcomeMailService(
    private val repository: MembershipRepository,
) : SendWelcomeMailUseCase {

    private val log = KotlinLogging.logger {}

    override fun sendWelcomeMail(membershipId: MembershipId) {
        val membership = repository.find(membershipId)

        // TODO Aufgabe 1:
        //  Logge eine Nachricht im Format "Sending welcome mail to [email]"
        //  In späteren Aufgaben wird hier eine echte Mail gesendet.
        TODO("Aufgabe 1: Logge 'Sending welcome mail to ${membership.email.value}'")
    }
}
