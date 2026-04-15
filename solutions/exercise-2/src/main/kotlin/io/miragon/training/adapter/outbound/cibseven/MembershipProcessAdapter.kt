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
        runtimeService.startProcessInstanceByKey(
            MembershipProcessApi.PROCESS_KEY,
            mapOf(
                MembershipProcessApi.Variables.MEMBERSHIP_ID to membership.id.value.toString(),
                MembershipProcessApi.Variables.EMAIL to membership.email.value,
                MembershipProcessApi.Variables.NAME to membership.name.value,
                MembershipProcessApi.Variables.AGE to membership.age.value,
            )
        )
    }
}
