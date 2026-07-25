package io.miragon.training.process;

import io.miragon.training.adapter.process.SubscribeNewsletterProcessApi;
import io.miragon.training.adapter.process.SubscribeNewsletterProcessApi.Elements;
import io.miragon.training.adapter.process.SubscribeNewsletterProcessApi.ServiceTasks;
import io.miragon.training.application.port.inbound.ClaimMembershipUseCase;
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

import static io.miragon.training.process.util.ProcessEngineTestUtils.completeExternalTask;
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
 * Process test for the membership process at the "compensation" stage (exercise 8) — revokeClaim now
 * runs as a compensation handler (SAGA-style rollback) instead of an explicit service task.
 *
 * <p>On the happy path the flow forks after confirmation: the welcome mail (a delegate) runs in
 * parallel with the "Notify community" external task; the parallel join only fires once a worker
 * completes the external task, which the tests do via {@code completeExternalTask}.
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

    @BeforeEach
    void setUp() {
        init(processEngine);
    }

    /**
     * Clean up any instances a test left running (e.g. one still parked at an external task), so the
     * running-instance counts stay isolated between test methods.
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
        assertThat(instance).isWaitingAt(Elements.USER_TASK_CONFIRM_MEMBERSHIP.getValue());
        return instance;
    }

    private void completeConfirmationTask(ProcessInstance instance) {
        String taskId = taskService.createTaskQuery()
                .processInstanceId(instance.getProcessInstanceId())
                .singleResult()
                .getId();
        taskService.complete(taskId);
        continueToNextWaitState(processEngine, instance.getProcessInstanceId());

        // After confirmation the flow forks: Send Welcome Mail (delegate) runs in parallel with the
        // "Notify community" external task. Stand in for the remote worker so the parallel join fires.
        completeExternalTask(processEngine, ServiceTasks.NOTIFY_COMMUNITY);
        continueToNextWaitState(processEngine, instance.getProcessInstanceId());
    }

    private long runningInstanceCount() {
        return runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(SubscribeNewsletterProcessApi.PROCESS_ID.getValue())
                .count();
    }

    @Test
    void happyPath_membershipActivated() {
        MembershipId id = new MembershipId();
        ProcessInstance instance = startWaitingAtConfirmation(id, "jane@example.com", "Jane");

        completeConfirmationTask(instance);

        assertThat(instance)
                .isEnded()
                // Deterministic backbone up to the fork and after the join. The two branch tasks run
                // in parallel, so their relative order is not asserted here (see hasPassed below).
                .hasPassedInOrder(
                        Elements.SERVICE_TASK_SEND_CONFIRMATION_MAIL.getValue(),
                        Elements.USER_TASK_CONFIRM_MEMBERSHIP.getValue(),
                        Elements.GATEWAY_NOTIFY_FORK.getValue(),
                        Elements.GATEWAY_NOTIFY_JOIN.getValue(),
                        Elements.END_EVENT_MEMBERSHIP_ACTIVATED.getValue())
                .hasPassed(
                        Elements.SERVICE_TASK_SEND_WELCOME_MAIL.getValue(),
                        Elements.SERVICE_TASK_NOTIFY_COMMUNITY.getValue())
                .hasNotPassed(Elements.SERVICE_TASK_REVOKE_CLAIM.getValue(), Elements.END_EVENT_MEMBERSHIP_DECLINED.getValue());

        verify(sendWelcomeMailUseCase, times(1)).sendWelcomeMail(id);
        // the instance fully completed — nothing left running
        org.assertj.core.api.Assertions.assertThat(runningInstanceCount()).isEqualTo(0L);
    }

    @Test
    void noCapacity_membershipIsRejected() {
        when(claimMembershipUseCase.claimMembership(any())).thenReturn(false);
        MembershipId id = new MembershipId();
        membershipProcess.startProcess(new Membership(id, new Email("jack@example.com"), new Name("Jack"), new Age(40)));

        ProcessInstance instance = findProcessInstance(runtimeService, id.value().toString());
        continueToNextWaitState(processEngine, instance.getProcessInstanceId());

        assertThat(instance)
                .isEnded()
                .hasPassedInOrder(Elements.SERVICE_TASK_SEND_REJECTION_MAIL.getValue(), Elements.END_EVENT_MEMBERSHIP_REJECTED.getValue())
                .hasNotPassed(Elements.SERVICE_TASK_SEND_WELCOME_MAIL.getValue(), Elements.END_EVENT_MEMBERSHIP_ACTIVATED.getValue());

        verify(sendRejectionMailUseCase).sendRejectionMail(id);
        org.assertj.core.api.Assertions.assertThat(runningInstanceCount()).isEqualTo(0L);
    }

    @Test
    void abortTimer_interruptsSubprocessAndRevokesClaim() {
        MembershipId id = new MembershipId();
        ProcessInstance instance = startWaitingAtConfirmation(id, "amy@example.com", "Amy");

        fireTimer(processEngine, Elements.TIMER_ABORT_AFTER_3_HALF_DAYS.getValue());
        continueToNextWaitState(processEngine, instance.getProcessInstanceId());

        assertThat(instance)
                .isEnded()
                .hasPassed(Elements.SERVICE_TASK_REVOKE_CLAIM.getValue(), Elements.END_EVENT_MEMBERSHIP_DECLINED.getValue())
                .hasNotPassed(Elements.SERVICE_TASK_SEND_WELCOME_MAIL.getValue(), Elements.END_EVENT_MEMBERSHIP_ACTIVATED.getValue());

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
                .hasPassed(Elements.SERVICE_TASK_REVOKE_CLAIM.getValue(), Elements.END_EVENT_MEMBERSHIP_DECLINED.getValue())
                .hasNotPassed(Elements.SERVICE_TASK_SEND_WELCOME_MAIL.getValue(), Elements.END_EVENT_MEMBERSHIP_ACTIVATED.getValue());

        verify(revokeClaimUseCase, times(1)).revokeClaim(id);
    }

    @Test
    void resendTimer_nonInterrupting_resendsConfirmationMailAndKeepsWaiting() {
        MembershipId id = new MembershipId();
        ProcessInstance instance = startWaitingAtConfirmation(id, "cara@example.com", "Cara");
        verify(sendConfirmationMailUseCase, times(1)).sendConfirmationMail(id);

        fireTimer(processEngine, Elements.TIMER_RESEND_EVERY_DAY.getValue());
        continueToNextWaitState(processEngine, instance.getProcessInstanceId());

        assertThat(instance).isWaitingAt(Elements.USER_TASK_CONFIRM_MEMBERSHIP.getValue());
        verify(reSendConfirmationMailUseCase, times(1)).reSendConfirmationMail(id);

        completeConfirmationTask(instance);
        assertThat(instance).isEnded().hasPassed(Elements.END_EVENT_MEMBERSHIP_ACTIVATED.getValue());
    }
}
