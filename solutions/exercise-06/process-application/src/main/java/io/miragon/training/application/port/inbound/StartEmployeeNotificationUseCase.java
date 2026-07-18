package io.miragon.training.application.port.inbound;

public interface StartEmployeeNotificationUseCase {

    void startEmployeeNotification(Command command);

    record Command(String memberName, String memberEmail) {
    }
}
