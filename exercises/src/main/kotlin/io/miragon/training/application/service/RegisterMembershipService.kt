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

        // TODO Aufgabe 1:
        //  1. Erstelle ein Membership-Objekt aus dem Command (Email, Name, Age)
        //  2. Speichere es mit repository.save(...)
        //  3. Starte den Prozess mit process.startProcess(...)
        //  4. Gib die MembershipId zurück
        TODO("Aufgabe 1: Implementiere die Membership-Registrierung")
    }
}
