package io.miragon.training.application.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.miragon.training.application.port.inbound.ReSendConfirmationMailUseCase
import io.miragon.training.application.port.outbound.MembershipRepository
import io.miragon.training.domain.MembershipId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ReSendConfirmationMailService(
    private val repository: MembershipRepository,
) : ReSendConfirmationMailUseCase {

    private val log = KotlinLogging.logger {}

    override fun reSendConfirmationMail(membershipId: MembershipId) {
        val membership = repository.find(membershipId)
        log.info { "Re-sending confirmation mail to ${membership.email.value} (membershipId=${membershipId.value})" }
    }
}
