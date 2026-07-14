package io.miragon.training.application.service;

import io.miragon.training.application.port.inbound.NotifyAboutSignedMembershipUseCase;
import io.miragon.training.application.port.outbound.MembershipRepository;
import io.miragon.training.domain.MembershipId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NotifyAboutSignedMembershipService implements NotifyAboutSignedMembershipUseCase {

    private static final Logger log = LoggerFactory.getLogger(NotifyAboutSignedMembershipService.class);

    private final MembershipRepository repository;

    public NotifyAboutSignedMembershipService(MembershipRepository repository) {
        this.repository = repository;
    }

    @Override
    public void notifyAboutSignedMembership(MembershipId membershipId) {
        var membership = repository.find(membershipId);
        log.info("Publishing signed membership event for {} (membershipId={})", membership.email().value(), membershipId.value());
    }
}
