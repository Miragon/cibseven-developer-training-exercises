package io.miragon.training.adapter.inbound.rest

import io.miragon.training.application.port.inbound.RegisterMembershipUseCase
import io.miragon.training.application.port.inbound.RejectMembershipUseCase
import io.miragon.training.domain.MembershipId
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/memberships")
class MembershipController(
    private val registerMembership: RegisterMembershipUseCase,
    private val rejectMembership: RejectMembershipUseCase,
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

    @PostMapping("/{membershipId}/reject")
    fun reject(@PathVariable membershipId: UUID): ResponseEntity<Void> {
        rejectMembership.rejectMembership(MembershipId(membershipId))
        return ResponseEntity.ok().build()
    }

    data class MembershipForm(
        val email: String,
        val name: String,
        val age: Int,
    )
}
