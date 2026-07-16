package io.miragon.training.adapter.inbound.cibseven;

import io.miragon.training.application.port.inbound.StartEmployeeNotificationUseCase;
import io.miragon.training.application.port.inbound.StartEmployeeNotificationUseCase.Command;
import org.cibseven.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

/**
 * Backs the message throw event {@code throw_notifyNewMember}. When a membership is confirmed it
 * fires the "new member joined" message by starting the separate {@code employeeNotification}
 * process, whose external task is picked up by the remote {@code notification-worker}.
 */
@Component
public class NotifyNewMemberDelegate extends BaseDelegate {

    private final StartEmployeeNotificationUseCase useCase;

    public NotifyNewMemberDelegate(StartEmployeeNotificationUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    protected void executeTask(DelegateExecution execution) {
        var name = (String) execution.getVariable("name");
        var email = (String) execution.getVariable("email");
        log.debug("New member joined: {} — starting employee notification process", name);
        useCase.startEmployeeNotification(new Command(name, email));
    }
}
