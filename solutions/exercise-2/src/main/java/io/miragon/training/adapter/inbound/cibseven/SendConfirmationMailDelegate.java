package io.miragon.training.adapter.inbound.cibseven;

import io.miragon.training.application.port.inbound.SendConfirmationMailUseCase;
import io.miragon.training.domain.SubscriptionId;
import org.cibseven.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SendConfirmationMailDelegate extends BaseDelegate {

    private final SendConfirmationMailUseCase useCase;

    public SendConfirmationMailDelegate(SendConfirmationMailUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    protected void executeTask(DelegateExecution execution) {
        var subscriptionId = (String) execution.getVariable("subscriptionId");
        log.debug("Received task to send confirmation mail for subscription: {}", subscriptionId);
        useCase.sendConfirmationMail(new SubscriptionId(UUID.fromString(subscriptionId)));
    }
}
