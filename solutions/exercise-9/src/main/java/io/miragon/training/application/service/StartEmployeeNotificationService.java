package io.miragon.training.application.service;

import io.miragon.training.application.port.inbound.StartEmployeeNotificationUseCase;
import io.miragon.training.application.port.outbound.EmployeeNotificationProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class StartEmployeeNotificationService implements StartEmployeeNotificationUseCase {

    private static final Logger log = LoggerFactory.getLogger(StartEmployeeNotificationService.class);

    private final EmployeeNotificationProcess process;

    public StartEmployeeNotificationService(EmployeeNotificationProcess process) {
        this.process = process;
    }

    @Override
    public void startEmployeeNotification(Command command) {
        log.info("Starting employee notification process for new member {}", command.memberName());
        process.start(command.memberName(), command.memberEmail());
    }
}
