package io.miragon.training.application.service;

// TODO Exercise 2: Uncomment this class (remove the /* and */ lines)
//  and then implement the TODO in the method body.
/*
import io.miragon.training.application.port.inbound.SendWelcomeMailUseCase;
import io.miragon.training.application.port.outbound.MembershipRepository;
import io.miragon.training.domain.MembershipId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SendWelcomeMailService implements SendWelcomeMailUseCase {

    private static final Logger log = LoggerFactory.getLogger(SendWelcomeMailService.class);

    private final MembershipRepository repository;

    public SendWelcomeMailService(MembershipRepository repository) {
        this.repository = repository;
    }

    @Override
    public void sendWelcomeMail(MembershipId membershipId) {
        var membership = repository.find(membershipId);

        // TODO Exercise 2:
        //  Log a message in the format "Sending welcome mail to [email]"
        //  In later exercises a real mail will be sent here.
        throw new UnsupportedOperationException("Exercise 2: Log 'Sending welcome mail to " + membership.email().value() + "'");
    }
}
*/
