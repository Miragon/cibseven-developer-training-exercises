package io.miragon.training.adapter.outbound.cibseven;

import io.miragon.training.adapter.process.EmployeeNotificationProcessApi;
import io.miragon.training.application.port.outbound.EmployeeNotificationProcess;
import org.cibseven.bpm.engine.RuntimeService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EmployeeNotificationProcessAdapter implements EmployeeNotificationProcess {

    private final RuntimeService runtimeService;

    public EmployeeNotificationProcessAdapter(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Override
    public void start(String memberName, String memberEmail) {
        runtimeService.startProcessInstanceByKey(
                EmployeeNotificationProcessApi.PROCESS_ID.getValue(),
                Map.of(
                        "name", memberName,
                        "email", memberEmail
                ));
    }
}
