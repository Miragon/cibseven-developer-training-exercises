package io.miragon.training.process;

import io.miragon.training.adapter.process.SubscribeNewsletterProcessApi.Elements;
import io.miragon.training.adapter.process.SubscribeNewsletterProcessApi.ServiceTasks;
import io.miragon.training.application.port.inbound.ClaimMembershipUseCase;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static io.miragon.training.process.util.ProcessEngineTestUtils.completeExternalTask;
import static io.miragon.training.process.util.ProcessEngineTestUtils.continueToNextWaitState;
import static io.miragon.training.process.util.ProcessEngineTestUtils.findProcessInstance;
import static org.cibseven.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.cibseven.bpm.engine.test.assertions.bpmn.BpmnAwareTests.init;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Process test for the membership process at the "gateway" stage (exercise 4).
 *
 * <p>The real BPMN and the real JavaDelegates run inside an in-memory engine; only the inbound
 * use cases (the business logic behind each delegate) are mocked, so the test verifies the process
 * <em>wiring</em> — sequence flows, the capacity gateway, and the wait state at the user task.
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
    private SendRejectionMailUseCase sendRejectionMailUseCase;

    @MockitoBean
    private SendWelcomeMailUseCase sendWelcomeMailUseCase;

    @BeforeEach
    void setUp() {
        init(processEngine);
    }

    @Test
    void happyPath_membershipIsConfirmedAndWelcomeMailIsSent() {
        when(claimMembershipUseCase.claimMembership(any())).thenReturn(true);

        Membership membership = new Membership(new Email("jane@example.com"), new Name("Jane"), new Age(30));
        MembershipId id = membership.id();
        membershipProcess.startProcess(membership);

        ProcessInstance instance = findProcessInstance(runtimeService, id.value().toString());
        continueToNextWaitState(processEngine);

        assertThat(instance).isWaitingAt(Elements.USER_TASK_CONFIRM_MEMBERSHIP.getValue());

        String taskId = taskService.createTaskQuery()
                .processInstanceId(instance.getProcessInstanceId())
                .singleResult()
                .getId();
        taskService.complete(taskId);
        continueToNextWaitState(processEngine);

        // The "Notify community" branch is an external task (topic notifyCommunity). The parallel
        // join only fires once a worker completes it, so we stand in for the remote worker here.
        completeExternalTask(processEngine, ServiceTasks.NOTIFY_COMMUNITY);
        continueToNextWaitState(processEngine);

        assertThat(instance)
                .isEnded()
                // Deterministic backbone up to the fork and after the join. The two branch tasks run
                // in parallel, so their relative order is not asserted here (see hasPassed below).
                .hasPassedInOrder(
                        Elements.START_EVENT_SUBMIT_REGISTRATION.getValue(),
                        Elements.SERVICE_TASK_CLAIM_MEMBERSHIP.getValue(),
                        Elements.GATEWAY_HAS_EMPTY_SPOTS.getValue(),
                        Elements.SERVICE_TASK_SEND_CONFIRMATION_MAIL.getValue(),
                        Elements.USER_TASK_CONFIRM_MEMBERSHIP.getValue(),
                        Elements.GATEWAY_NOTIFY_FORK.getValue(),
                        Elements.GATEWAY_NOTIFY_JOIN.getValue(),
                        Elements.END_EVENT_MEMBERSHIP_CONFIRMED.getValue())
                .hasPassed(
                        Elements.SERVICE_TASK_SEND_WELCOME_MAIL.getValue(),
                        Elements.SERVICE_TASK_NOTIFY_COMMUNITY.getValue())
                .hasNotPassed(
                        Elements.SERVICE_TASK_SEND_REJECTION_MAIL.getValue(),
                        Elements.END_EVENT_MEMBERSHIP_REJECTED.getValue());

        verify(claimMembershipUseCase).claimMembership(id);
        verify(sendConfirmationMailUseCase).sendConfirmationMail(id);
        verify(sendWelcomeMailUseCase).sendWelcomeMail(id);
        verify(sendRejectionMailUseCase, never()).sendRejectionMail(any());
    }

    @Test
    void noCapacity_membershipIsRejected() {
        when(claimMembershipUseCase.claimMembership(any())).thenReturn(false);

        Membership membership = new Membership(new Email("jack@example.com"), new Name("Jack"), new Age(40));
        MembershipId id = membership.id();
        membershipProcess.startProcess(membership);

        ProcessInstance instance = findProcessInstance(runtimeService, id.value().toString());
        continueToNextWaitState(processEngine);

        assertThat(instance)
                .isEnded()
                .hasPassedInOrder(
                        Elements.SERVICE_TASK_CLAIM_MEMBERSHIP.getValue(),
                        Elements.GATEWAY_HAS_EMPTY_SPOTS.getValue(),
                        Elements.SERVICE_TASK_SEND_REJECTION_MAIL.getValue(),
                        Elements.END_EVENT_MEMBERSHIP_REJECTED.getValue())
                .hasNotPassed(
                        Elements.SERVICE_TASK_SEND_CONFIRMATION_MAIL.getValue(),
                        Elements.USER_TASK_CONFIRM_MEMBERSHIP.getValue(),
                        Elements.SERVICE_TASK_SEND_WELCOME_MAIL.getValue(),
                        Elements.END_EVENT_MEMBERSHIP_CONFIRMED.getValue());

        verify(sendRejectionMailUseCase).sendRejectionMail(id);
        verify(sendWelcomeMailUseCase, never()).sendWelcomeMail(any());
    }
}
