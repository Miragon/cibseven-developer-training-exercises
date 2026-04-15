package io.miragon.training.adapter.inbound.rest

import io.miragon.training.application.port.inbound.RegisterMembershipUseCase
import io.miragon.training.domain.MembershipId
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/memberships")
class MembershipController(
    private val registerMembership: RegisterMembershipUseCase,
) {

    @PostMapping
    fun register(@RequestBody form: MembershipForm): ResponseEntity<String> {
        val membershipId = registerMembership.register(
            RegisterMembershipUseCase.Command(
                email = form.email,
                name = form.name,
                age = form.age,
            )
        )
        return ResponseEntity.ok(membershipId.value.toString())
    }

    data class MembershipForm(
        val email: String,
        val name: String,
        val age: Int,
    )
}
