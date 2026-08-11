package io.miragon.training.adapter.inbound.cibseven;

import org.cibseven.bpm.engine.RuntimeService;
import org.cibseven.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Broadcasts the {@code Signal_MemberActivated} signal when a new member is activated — the engine host's
 * only job in the notification story. It says nothing about <em>who</em> reacts: any department can
 * subscribe its own process to this signal (1:N broadcast). The Logistics department's {@code sendWelcomeKit}
 * process (owned and deployed by its own remote service) catches it via a signal start event.
 *
 * <p>The signal name is the contract between the engine host and any listening department — the same way
 * an external-task topic is a string contract. The {@code name} of the new member travels as the signal
 * payload so listeners need not call back into the engine to learn it.
 */
@Component
public class BroadcastMemberActivatedDelegate extends BaseDelegate {

    /** Contract with any department process that subscribes via a signal start event. */
    public static final String SIGNAL_MEMBER_ACTIVATED = "Signal_MemberActivated";

    private final RuntimeService runtimeService;

    public BroadcastMemberActivatedDelegate(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Override
    protected void executeTask(DelegateExecution execution) {
        var name = (String) execution.getVariable("name");
        log.debug("Broadcasting {} for new member {}", SIGNAL_MEMBER_ACTIVATED, name);
        runtimeService.createSignalEvent(SIGNAL_MEMBER_ACTIVATED)
                .setVariables(Map.of("name", name))
                .send();
    }
}
