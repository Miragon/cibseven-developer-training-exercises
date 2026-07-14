package io.miragon.training.process;

import io.miragon.training.application.port.inbound.ClaimMembershipUseCase;
import io.miragon.training.application.port.inbound.ReSendConfirmationMailUseCase;
import io.miragon.training.application.port.inbound.RevokeClaimUseCase;
import io.miragon.training.application.port.inbound.SendConfirmationMailUseCase;
import io.miragon.training.application.port.inbound.SendRejectionMailUseCase;
import io.miragon.training.application.port.inbound.SendWelcomeMailUseCase;
import io.miragon.training.adapter.process.SubscribeNewsletterProcessApi.Elements;
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
 * Process test for the membership process at the "boundary events" stage (exercise 6).
 *
 * <p>On top of the gateway process (exercise 5) this adds a confirmation subprocess with three
 * boundary events: a non-interrupting daily resend timer, an interrupting abort timer, and an
 * interrupting reject message. Each of those paths gets its own test.
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

    private ProcessInstance startWaitingAtConfirmation(MembershipId id, String email, String name) {
        when(claimMembershipUseCase.claimMembership(any())).thenReturn(true);
        membershipProcess.startProcess(new Membership(id, new Email(email), new Name(name), new Age(30)));
        ProcessInstance instance = findProcessInstance(runtimeService, id.value().toString());
        continueToNextWaitState(processEngine);
        assertThat(instance).isWaitingAt(Elements.USER_TASK_CONFIRM_MEMBERSHIP.getValue());
        return instance;
    }

    private void completeConfirmationTask(ProcessInstance instance) {
        String taskId = taskService.createTaskQuery()
                .processInstanceId(instance.getProcessInstanceId())
                .singleResult()
                .getId();
        taskService.complete(taskId);
        continueToNextWaitState(processEngine);
    }

    @Test
    void happyPath_membershipIsActivated() {
        MembershipId id = new MembershipId();
        ProcessInstance instance = startWaitingAtConfirmation(id, "jane@example.com", "Jane");

        completeConfirmationTask(instance);

        assertThat(instance)
                .isEnded()
                .hasPassedInOrder(
                        Elements.SERVICE_TASK_CLAIM_MEMBERSHIP.getValue(),
                        Elements.SERVICE_TASK_SEND_CONFIRMATION_MAIL.getValue(),
                        Elements.USER_TASK_CONFIRM_MEMBERSHIP.getValue(),
                        Elements.SERVICE_TASK_SEND_WELCOME_MAIL.getValue(),
                        Elements.END_EVENT_MEMBERSHIP_ACTIVATED.getValue())
                .hasNotPassed(
                        Elements.SERVICE_TASK_REVOKE_CLAIM.getValue(),
                        Elements.END_EVENT_MEMBERSHIP_DECLINED.getValue(),
                        Elements.SERVICE_TASK_SEND_REJECTION_MAIL.getValue(),
                        Elements.END_EVENT_MEMBERSHIP_REJECTED.getValue());

        verify(sendConfirmationMailUseCase, times(1)).sendConfirmationMail(id);
        verify(sendWelcomeMailUseCase, times(1)).sendWelcomeMail(id);
        verify(revokeClaimUseCase, never()).revokeClaim(any());
    }

    @Test
    void noCapacity_membershipIsRejected() {
        when(claimMembershipUseCase.claimMembership(any())).thenReturn(false);
        MembershipId id = new MembershipId();
        membershipProcess.startProcess(new Membership(id, new Email("jack@example.com"), new Name("Jack"), new Age(40)));

        ProcessInstance instance = findProcessInstance(runtimeService, id.value().toString());
        continueToNextWaitState(processEngine);

        assertThat(instance)
                .isEnded()
                .hasPassedInOrder(Elements.SERVICE_TASK_SEND_REJECTION_MAIL.getValue(), Elements.END_EVENT_MEMBERSHIP_REJECTED.getValue())
                .hasNotPassed(Elements.SUB_PROCESS_CONFIRM_MEMBERSHIP.getValue(), Elements.SERVICE_TASK_SEND_WELCOME_MAIL.getValue());

        verify(sendRejectionMailUseCase).sendRejectionMail(id);
        verify(sendWelcomeMailUseCase, never()).sendWelcomeMail(any());
    }

    @Test
    void abortTimer_interruptsSubprocessAndRevokesClaim() {
        MembershipId id = new MembershipId();
        ProcessInstance instance = startWaitingAtConfirmation(id, "amy@example.com", "Amy");

        fireTimer(processEngine, Elements.TIMER_ABORT_AFTER_3_HALF_DAYS.getValue());
        continueToNextWaitState(processEngine);

        assertThat(instance)
                .isEnded()
                .hasPassedInOrder(
                        Elements.USER_TASK_CONFIRM_MEMBERSHIP.getValue(),
                        Elements.SERVICE_TASK_REVOKE_CLAIM.getValue(),
                        Elements.END_EVENT_MEMBERSHIP_DECLINED.getValue())
                .hasNotPassed(Elements.SERVICE_TASK_SEND_WELCOME_MAIL.getValue(), Elements.END_EVENT_MEMBERSHIP_ACTIVATED.getValue());

        verify(revokeClaimUseCase, times(1)).revokeClaim(id);
        verify(sendWelcomeMailUseCase, never()).sendWelcomeMail(any());
    }

    @Test
    void rejectMessage_interruptsSubprocessAndRevokesClaim() {
        MembershipId id = new MembershipId();
        ProcessInstance instance = startWaitingAtConfirmation(id, "ben@example.com", "Ben");

        membershipProcess.rejectMembership(id);
        continueToNextWaitState(processEngine);

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
        continueToNextWaitState(processEngine);

        assertThat(instance).isWaitingAt(Elements.USER_TASK_CONFIRM_MEMBERSHIP.getValue());
        verify(reSendConfirmationMailUseCase, times(1)).reSendConfirmationMail(id);

        completeConfirmationTask(instance);

        assertThat(instance)
                .isEnded()
                .hasPassed(Elements.SERVICE_TASK_SEND_WELCOME_MAIL.getValue(), Elements.END_EVENT_MEMBERSHIP_ACTIVATED.getValue());
    }
}
