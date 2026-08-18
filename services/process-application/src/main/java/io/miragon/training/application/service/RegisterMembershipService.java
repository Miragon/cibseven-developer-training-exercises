package io.miragon.training.application.service;

// TODO Exercise 2: Uncomment this class (remove the /* and */ lines)
//  and then implement the TODOs in the method body.
/*
import io.miragon.training.application.port.inbound.RegisterMembershipUseCase;
import io.miragon.training.application.port.outbound.MembershipProcess;
import io.miragon.training.application.port.outbound.MembershipRepository;
import io.miragon.training.domain.Age;
import io.miragon.training.domain.Email;
import io.miragon.training.domain.Name;
import io.miragon.training.domain.Membership;
import io.miragon.training.domain.MembershipId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RegisterMembershipService implements RegisterMembershipUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegisterMembershipService.class);

    private final MembershipRepository repository;
    private final MembershipProcess process;

    public RegisterMembershipService(MembershipRepository repository, MembershipProcess process) {
        this.repository = repository;
        this.process = process;
    }

    @Override
    public MembershipId register(Command command) {
        log.info("Registering membership for {}", command.email());

        // TODO Exercise 2:
        //  1. Create a Membership object from the Command (Email, Name, Age)
        //  2. Save it with repository.save(...)
        //  3. Start the process with process.startProcess(...)
        //  4. Return the MembershipId
        throw new UnsupportedOperationException("Exercise 2: Implement the membership registration");
    }
}
*/
