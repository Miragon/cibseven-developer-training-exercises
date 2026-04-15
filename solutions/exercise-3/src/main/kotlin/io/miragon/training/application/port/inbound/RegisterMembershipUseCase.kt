package io.miragon.training.application.port.inbound

import io.miragon.training.domain.MembershipId

interface RegisterMembershipUseCase {

    fun register(command: Command): MembershipId

    data class Command(
        val email: String,
        val name: String,
        val age: Int,
    )
}
