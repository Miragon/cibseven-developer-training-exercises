package io.miragon.training.application.port.inbound;

import io.miragon.training.domain.NewMember;

public interface NotifyEmployeesUseCase {

    void notify(NewMember member);
}
