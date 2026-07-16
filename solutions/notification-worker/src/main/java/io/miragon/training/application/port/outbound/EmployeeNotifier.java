package io.miragon.training.application.port.outbound;

import io.miragon.training.domain.NewMember;

/**
 * Outbound port: publishes a "new member joined" event to an external channel (e.g. a Microsoft
 * Teams channel) so the whole team is notified.
 */
public interface EmployeeNotifier {

    void publish(NewMember member);
}
