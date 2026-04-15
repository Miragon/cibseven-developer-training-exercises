package io.miragon.training.adapter.inbound.rest;

import io.miragon.training.application.port.inbound.RegisterMembershipUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/memberships")
public class MembershipController {

    private final RegisterMembershipUseCase registerMembership;

    public MembershipController(RegisterMembershipUseCase registerMembership) {
        this.registerMembership = registerMembership;
    }

    @PostMapping
    public ResponseEntity<String> register(@RequestBody MembershipForm form) {
        var membershipId = registerMembership.register(
                new RegisterMembershipUseCase.Command(form.email(), form.name(), form.age()));
        return ResponseEntity.ok(membershipId.value().toString());
    }

    public record MembershipForm(String email, String name, int age) {}
}
