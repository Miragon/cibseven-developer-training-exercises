package io.miragon.training.adapter.inbound.cibseven;

import io.miragon.training.application.port.inbound.NotifyAboutSignedMembershipUseCase;
import io.miragon.training.domain.MembershipId;
import org.cibseven.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class NotifyAboutSignedMembershipDelegate extends BaseDelegate {

    private final NotifyAboutSignedMembershipUseCase useCase;

    public NotifyAboutSignedMembershipDelegate(NotifyAboutSignedMembershipUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    protected void executeTask(DelegateExecution execution) {
        var membershipId = (String) execution.getVariable("membershipId");
        log.debug("Received task to notify about signed membership: {}", membershipId);
        useCase.notifyAboutSignedMembership(new MembershipId(UUID.fromString(membershipId)));
    }
}
