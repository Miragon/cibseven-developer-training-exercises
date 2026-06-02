package io.miragon.training.application.port.inbound;

public interface NotifyAboutSignedMembershipUseCase {

    /**
     * Published from a signal-triggered flow that runs in its own, fresh process
     * instance – so it deliberately carries no membership-specific data.
     */
    void notifyAboutSignedMembership();
}
