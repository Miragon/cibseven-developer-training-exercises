package io.miragon.training.adapter.outbound.cibseven;

import io.miragon.training.application.port.outbound.MembershipProcess;
import io.miragon.training.domain.Membership;
import org.cibseven.bpm.engine.RuntimeService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MembershipProcessAdapter implements MembershipProcess {

    private final RuntimeService runtimeService;

    public MembershipProcessAdapter(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Override
    public void startProcess(Membership membership) {
        runtimeService.startProcessInstanceByKey("subscribeNewsletter", Map.of(
                "membershipId", membership.id().value().toString(),
                "email", membership.email().value(),
                "name", membership.name().value(),
                "age", membership.age().value()
        ));
    }
}
