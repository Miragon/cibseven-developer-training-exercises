package io.miragon.training.application.service;

import io.miragon.training.application.port.inbound.NotifyAboutSignedMembershipUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotifyAboutSignedMembershipService implements NotifyAboutSignedMembershipUseCase {

    private static final Logger log = LoggerFactory.getLogger(NotifyAboutSignedMembershipService.class);

    @Override
    public void notifyAboutSignedMembership() {
        log.info("Publishing in the forum: a new member just joined the Miravelo Inner Circle! 🎉");
    }
}
