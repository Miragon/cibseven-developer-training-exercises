package io.miragon.training.application.port.outbound;

public interface EmployeeNotificationProcess {

    void start(String memberName, String memberEmail);
}
