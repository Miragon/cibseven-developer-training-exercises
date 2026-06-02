package io.miragon.training.adapter.inbound.cibseven;

import io.miragon.training.application.port.inbound.NotifyAboutSignedMembershipUseCase;
import org.cibseven.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

@Component
public class NotifyAboutSignedMembershipDelegate extends BaseDelegate {

    private final NotifyAboutSignedMembershipUseCase useCase;

    public NotifyAboutSignedMembershipDelegate(NotifyAboutSignedMembershipUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    protected void executeTask(DelegateExecution execution) {
        // Runs in the signal-triggered instance (empty scope) – no membershipId to read.
        log.debug("Received task to notify about signed membership");
        useCase.notifyAboutSignedMembership();
    }
}
