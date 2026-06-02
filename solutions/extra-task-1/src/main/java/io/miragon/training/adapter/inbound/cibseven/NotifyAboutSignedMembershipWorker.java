package io.miragon.training.adapter.inbound.cibseven;

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker;
import io.miragon.training.adapter.process.SubscribeNewsletterProcessApi.ServiceTasks;
import io.miragon.training.application.port.inbound.NotifyAboutSignedMembershipUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Runs in the signal-triggered process instance, which starts with an empty
 * variable scope – hence no {@code membershipId} is read here.
 */
@Component
public class NotifyAboutSignedMembershipWorker {

    private static final Logger log = LoggerFactory.getLogger(NotifyAboutSignedMembershipWorker.class);

    private final NotifyAboutSignedMembershipUseCase useCase;

    public NotifyAboutSignedMembershipWorker(NotifyAboutSignedMembershipUseCase useCase) {
        this.useCase = useCase;
    }

    @ProcessEngineWorker(topic = ServiceTasks.NOTIFY_ABOUT_SIGNED_MEMBERSHIP)
    public void notifyAboutSignedMembership() {
        log.debug("Received task to notify about signed membership");
        useCase.notifyAboutSignedMembership();
    }
}
