package io.miragon.training.process;

import io.miragon.training.application.port.inbound.ClaimMembershipUseCase;
import io.miragon.training.application.port.inbound.NotifyAboutSignedMembershipUseCase;
import io.miragon.training.application.port.inbound.ReSendConfirmationMailUseCase;
import io.miragon.training.application.port.inbound.RevokeClaimUseCase;
import io.miragon.training.application.port.inbound.SendConfirmationMailUseCase;
import io.miragon.training.application.port.inbound.SendRejectionMailUseCase;
import io.miragon.training.application.port.inbound.SendWelcomeMailUseCase;
import io.miragon.training.application.port.outbound.MembershipProcess;
import io.miragon.training.domain.Age;
import io.miragon.training.domain.Email;
import io.miragon.training.domain.Membership;
import io.miragon.training.domain.MembershipId;
import io.miragon.training.domain.Name;
import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.RuntimeService;
import org.cibseven.bpm.engine.TaskService;
import org.cibseven.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static io.miragon.training.process.util.ProcessEngineTestUtils.continueToNextWaitState;
import static io.miragon.training.process.util.ProcessEngineTestUtils.findProcessInstance;
import static io.miragon.training.process.util.ProcessEngineTestUtils.fireTimer;
import static org.cibseven.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.cibseven.bpm.engine.test.assertions.bpmn.BpmnAwareTests.init;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Process test for the membership process at the "compensation" stage (exercise 8) — revokeClaim now runs as a compensation handler.
 *
 * <p>Reaching {@code endEvent_membershipActivated} now throws a signal that starts a second,
 * independent instance ({@code startEvent_membershipActivated} → {@code serviceTask_publishSignal})
 * for the forum notification. The tests drive only the originating instance (via the
 * instance-scoped {@code continueToNextWaitState}) and prove the broadcast by asserting that the
 * signal started exactly one additional instance.
 */
@SpringBootTest
@ActiveProfiles("test")
class MembershipProcessTest {

    @Autowired
    private MembershipProcess membershipProcess;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private ProcessEngine processEngine;

    @MockitoBean
    private ClaimMembershipUseCase claimMembershipUseCase;

    @MockitoBean
    private SendConfirmationMailUseCase sendConfirmationMailUseCase;

    @MockitoBean
    private ReSendConfirmationMailUseCase reSendConfirmationMailUseCase;

    @MockitoBean
    private SendRejectionMailUseCase sendRejectionMailUseCase;

    @MockitoBean
    private SendWelcomeMailUseCase sendWelcomeMailUseCase;

    @MockitoBean
    private RevokeClaimUseCase revokeClaimUseCase;

    @MockitoBean
    private NotifyAboutSignedMembershipUseCase notifyAboutSignedMembershipUseCase;

    @BeforeEach
    void setUp() {
        init(processEngine);
    }

    /**
     * The signal broadcast starts a second instance that we deliberately do not drive to completion;
     * clean up all running instances so counts stay isolated between test methods.
     */
    @AfterEach
    void tearDown() {
        runtimeService.createProcessInstanceQuery().list()
                .forEach(pi -> runtimeService.deleteProcessInstance(pi.getId(), "test cleanup"));
    }

    private ProcessInstance startWaitingAtConfirmation(MembershipId id, String email, String name) {
        when(claimMembershipUseCase.claimMembership(any())).thenReturn(true);
        membershipProcess.startProcess(new Membership(id, new Email(email), new Name(name), new Age(30)));
        ProcessInstance instance = findProcessInstance(runtimeService, id.value().toString());
        continueToNextWaitState(processEngine, instance.getProcessInstanceId());
        assertThat(instance).isWaitingAt("userTask_confirmMembership");
        return instance;
    }

    private void completeConfirmationTask(ProcessInstance instance) {
        String taskId = taskService.createTaskQuery()
                .processInstanceId(instance.getProcessInstanceId())
                .singleResult()
                .getId();
        taskService.complete(taskId);
        continueToNextWaitState(processEngine, instance.getProcessInstanceId());
    }

    private long runningInstanceCount() {
        return runtimeService.createProcessInstanceQuery()
                .processDefinitionKey("subscribeNewsletter")
                .count();
    }

    @Test
    void happyPath_membershipActivatedAndSignalBroadcast() {
        MembershipId id = new MembershipId();
        ProcessInstance instance = startWaitingAtConfirmation(id, "jane@example.com", "Jane");

        completeConfirmationTask(instance);

        assertThat(instance)
                .isEnded()
                .hasPassedInOrder(
                        "serviceTask_sendConfirmationMail",
                        "userTask_confirmMembership",
                        "serviceTask_sendWelcomeMail",
                        "endEvent_membershipActivated")
                .hasNotPassed("serviceTask_revokeClaim", "endEvent_membershipDeclined");

        verify(sendWelcomeMailUseCase, times(1)).sendWelcomeMail(id);
        // the signal thrown at activation started exactly one forum-notification instance
        org.assertj.core.api.Assertions.assertThat(runningInstanceCount()).isEqualTo(1L);
    }

    @Test
    void noCapacity_membershipIsRejectedWithoutSignal() {
        when(claimMembershipUseCase.claimMembership(any())).thenReturn(false);
        MembershipId id = new MembershipId();
        membershipProcess.startProcess(new Membership(id, new Email("jack@example.com"), new Name("Jack"), new Age(40)));

        ProcessInstance instance = findProcessInstance(runtimeService, id.value().toString());
        continueToNextWaitState(processEngine, instance.getProcessInstanceId());

        assertThat(instance)
                .isEnded()
                .hasPassedInOrder("serviceTask_sendRejectionMail", "endEvent_membershipRejected")
                .hasNotPassed("serviceTask_sendWelcomeMail", "endEvent_membershipActivated");

        verify(sendRejectionMailUseCase).sendRejectionMail(id);
        org.assertj.core.api.Assertions.assertThat(runningInstanceCount()).isEqualTo(0L);
    }

    @Test
    void abortTimer_interruptsSubprocessAndRevokesClaim() {
        MembershipId id = new MembershipId();
        ProcessInstance instance = startWaitingAtConfirmation(id, "amy@example.com", "Amy");

        fireTimer(processEngine, "timer_abortAfter3HalfDays");
        continueToNextWaitState(processEngine, instance.getProcessInstanceId());

        assertThat(instance)
                .isEnded()
                .hasPassed("serviceTask_revokeClaim", "endEvent_membershipDeclined")
                .hasNotPassed("serviceTask_sendWelcomeMail", "endEvent_membershipActivated");

        verify(revokeClaimUseCase, times(1)).revokeClaim(id);
        org.assertj.core.api.Assertions.assertThat(runningInstanceCount()).isEqualTo(0L);
    }

    @Test
    void rejectMessage_interruptsSubprocessAndRevokesClaim() {
        MembershipId id = new MembershipId();
        ProcessInstance instance = startWaitingAtConfirmation(id, "ben@example.com", "Ben");

        membershipProcess.rejectMembership(id);
        continueToNextWaitState(processEngine, instance.getProcessInstanceId());

        assertThat(instance)
                .isEnded()
                .hasPassed("serviceTask_revokeClaim", "endEvent_membershipDeclined")
                .hasNotPassed("serviceTask_sendWelcomeMail", "endEvent_membershipActivated");

        verify(revokeClaimUseCase, times(1)).revokeClaim(id);
    }

    @Test
    void resendTimer_nonInterrupting_resendsConfirmationMailAndKeepsWaiting() {
        MembershipId id = new MembershipId();
        ProcessInstance instance = startWaitingAtConfirmation(id, "cara@example.com", "Cara");
        verify(sendConfirmationMailUseCase, times(1)).sendConfirmationMail(id);

        fireTimer(processEngine, "timer_resendEveryDay");
        continueToNextWaitState(processEngine, instance.getProcessInstanceId());

        assertThat(instance).isWaitingAt("userTask_confirmMembership");
        verify(reSendConfirmationMailUseCase, times(1)).reSendConfirmationMail(id);

        completeConfirmationTask(instance);
        assertThat(instance).isEnded().hasPassed("endEvent_membershipActivated");
    }
}
