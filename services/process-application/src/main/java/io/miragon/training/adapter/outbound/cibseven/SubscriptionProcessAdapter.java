package io.miragon.training.adapter.outbound.cibseven;

// TODO Exercise 2: Uncomment this class (remove the /* and */ lines)
//  and then implement the TODO in startProcess(...).
//  It needs the CIB Seven engine that you switch on in Exercise 1.
/*
import io.miragon.training.application.port.outbound.SubscriptionProcess;
import io.miragon.training.domain.Subscription;
import org.cibseven.bpm.engine.RuntimeService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SubscriptionProcessAdapter implements SubscriptionProcess {

    private final RuntimeService runtimeService;

    public SubscriptionProcessAdapter(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Override
    public void startProcess(Subscription subscription) {
        // TODO Exercise 2: Start the process instance yourself via the RuntimeService.
        //  - Process key: "subscribeNewsletter".
        //  - Pass these four process variables as a Map (the keys must be named exactly like this):
        //      "subscriptionId" <- subscription.id().value().toString()
        //      "email"          <- subscription.email().value()
        //      "name"           <- subscription.name().value()
        //      "age"            <- subscription.age().value()
        //  Which RuntimeService method starts an instance by key (startProcessInstanceByKey)
        //  and how you assemble the key and variables Map is something you build yourself – no ready-made call line.
        throw new UnsupportedOperationException("Exercise 2: Start the process via the RuntimeService");
    }
}
*/
