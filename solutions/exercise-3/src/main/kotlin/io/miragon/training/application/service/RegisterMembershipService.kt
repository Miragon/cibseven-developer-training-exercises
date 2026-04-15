package io.miragon.training.application.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.miragon.training.application.port.inbound.RegisterMembershipUseCase
import io.miragon.training.application.port.outbound.MembershipProcess
import io.miragon.training.application.port.outbound.MembershipRepository
import io.miragon.training.domain.Age
import io.miragon.training.domain.Email
import io.miragon.training.domain.Membership
import io.miragon.training.domain.MembershipId
import io.miragon.training.domain.Name
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RegisterMembershipService(
    private val repository: MembershipRepository,
    private val process: MembershipProcess,
) : RegisterMembershipUseCase {

    private val log = KotlinLogging.logger {}

    override fun register(command: RegisterMembershipUseCase.Command): MembershipId {
        log.info { "Registering membership for ${command.email}" }
        val membership = Membership(
            email = Email(command.email),
            name = Name(command.name),
            age = Age(command.age),
        )
        repository.save(membership)
        process.startProcess(membership)
        return membership.id
    }
}
