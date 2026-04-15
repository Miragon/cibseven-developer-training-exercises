package io.miragon.training.application.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.miragon.training.application.port.inbound.SendConfirmationMailUseCase
import io.miragon.training.application.port.outbound.MembershipRepository
import io.miragon.training.domain.MembershipId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SendConfirmationMailService(
    private val repository: MembershipRepository,
) : SendConfirmationMailUseCase {

    private val log = KotlinLogging.logger {}

    override fun sendConfirmationMail(membershipId: MembershipId) {
        val membership = repository.find(membershipId)
        log.info { "Sending confirmation mail to ${membership.email.value} (membershipId=${membershipId.value})" }
    }
}
