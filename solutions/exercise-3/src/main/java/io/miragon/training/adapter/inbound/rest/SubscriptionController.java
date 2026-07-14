package io.miragon.training.adapter.inbound.rest;

import io.miragon.training.application.port.inbound.RegisterSubscriptionUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final RegisterSubscriptionUseCase registerSubscription;

    public SubscriptionController(RegisterSubscriptionUseCase registerSubscription) {
        this.registerSubscription = registerSubscription;
    }

    @PostMapping
    public ResponseEntity<String> register(@RequestBody SubscriptionForm form) {
        var subscriptionId = registerSubscription.register(
                new RegisterSubscriptionUseCase.Command(form.email(), form.name(), form.age())
        );
        return ResponseEntity.ok(subscriptionId.value().toString());
    }

    public record SubscriptionForm(String email, String name, int age) {}
}
