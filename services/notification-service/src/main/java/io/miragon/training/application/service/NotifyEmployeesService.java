package io.miragon.training.application.service;

import io.miragon.training.application.port.inbound.NotifyEmployeesUseCase;
import io.miragon.training.application.port.outbound.EmployeeNotifier;
import io.miragon.training.domain.NewMember;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotifyEmployeesService implements NotifyEmployeesUseCase {

    private static final Logger log = LoggerFactory.getLogger(NotifyEmployeesService.class);

    private final EmployeeNotifier notifier;

    public NotifyEmployeesService(EmployeeNotifier notifier) {
        this.notifier = notifier;
    }

    @Override
    public void notify(NewMember member) {
        log.info("Announcing new Inner Circle member {}", member.name());
        notifier.publish(member);
    }
}
