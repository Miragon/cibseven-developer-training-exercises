package io.miragon.training.adapter.outbound.cibseven

import io.miragon.training.adapter.process.MembershipProcessApi
import io.miragon.training.application.port.outbound.MembershipProcess
import io.miragon.training.domain.Membership
import org.cibseven.bpm.engine.RuntimeService
import org.springframework.stereotype.Component

@Component
class MembershipProcessAdapter(
    private val runtimeService: RuntimeService,
) : MembershipProcess {

    override fun startProcess(membership: Membership) {
        // TODO Aufgabe 1:
        //  Starte eine neue Prozessinstanz mit dem Key aus MembershipProcessApi.PROCESS_KEY.
        //  Übergib folgende Prozess-Variablen als Map:
        //    - "membershipId" -> membership.id.value.toString()
        //    - "email"        -> membership.email.value
        //    - "name"         -> membership.name.value
        //    - "age"          -> membership.age.value
        //  Hinweis: runtimeService.startProcessInstanceByKey(key, variables)
        TODO("Aufgabe 1: Starte den Prozess via RuntimeService")
    }
}
