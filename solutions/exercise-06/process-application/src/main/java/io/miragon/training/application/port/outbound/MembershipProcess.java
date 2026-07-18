package io.miragon.training.application.port.outbound;

import io.miragon.training.domain.Membership;

public interface MembershipProcess {

    void startProcess(Membership membership);
}
