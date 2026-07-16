package io.miragon.training.application.port.outbound;

import io.miragon.training.domain.NewMember;

/**
 * Outbound port: publishes a "new member joined" event to some collaborative surface everyone can
 * see. Implementations are selected at runtime via the {@code notification.sink} property.
 */
public interface EmployeeNotifier {

    void publish(NewMember member);
}
