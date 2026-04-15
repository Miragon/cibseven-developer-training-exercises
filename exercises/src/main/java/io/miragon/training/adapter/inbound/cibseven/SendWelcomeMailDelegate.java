package io.miragon.training.adapter.inbound.cibseven;

import io.miragon.training.application.port.inbound.SendWelcomeMailUseCase;
import io.miragon.training.domain.SubscriptionId;
import org.cibseven.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SendWelcomeMailDelegate extends BaseDelegate {

    private final SendWelcomeMailUseCase useCase;

    public SendWelcomeMailDelegate(SendWelcomeMailUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    protected void executeTask(DelegateExecution execution) {
        // TODO Aufgabe 1:
        //  1. Lese die Variable "subscriptionId" aus der DelegateExecution
        //     Hinweis: (String) execution.getVariable("subscriptionId")
        //  2. Erstelle eine SubscriptionId: new SubscriptionId(UUID.fromString(...))
        //  3. Rufe useCase.sendWelcomeMail(subscriptionId) auf
        throw new UnsupportedOperationException("Aufgabe 1: Verbinde den Delegate mit dem Use Case");
    }
}
