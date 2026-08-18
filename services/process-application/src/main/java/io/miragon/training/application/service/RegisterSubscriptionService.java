package io.miragon.training.application.service;

// TODO Exercise 2: Uncomment this class (remove the /* and */ lines)
//  and then implement the TODOs in the method body.
/*
import io.miragon.training.application.port.inbound.RegisterSubscriptionUseCase;
import io.miragon.training.application.port.outbound.SubscriptionProcess;
import io.miragon.training.application.port.outbound.SubscriptionRepository;
import io.miragon.training.domain.Age;
import io.miragon.training.domain.Email;
import io.miragon.training.domain.Name;
import io.miragon.training.domain.Subscription;
import io.miragon.training.domain.SubscriptionId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RegisterSubscriptionService implements RegisterSubscriptionUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegisterSubscriptionService.class);

    private final SubscriptionRepository repository;
    private final SubscriptionProcess process;

    public RegisterSubscriptionService(SubscriptionRepository repository, SubscriptionProcess process) {
        this.repository = repository;
        this.process = process;
    }

    @Override
    public SubscriptionId register(Command command) {
        log.info("Registering subscription for {}", command.email());

        // TODO Exercise 2:
        //  1. Create a Subscription object from the Command (Email, Name, Age)
        //  2. Save it with repository.save(...)
        //  3. Start the process with process.startProcess(...)
        //  4. Return the SubscriptionId
        throw new UnsupportedOperationException("Exercise 2: Implement the subscription registration");
    }
}
*/
