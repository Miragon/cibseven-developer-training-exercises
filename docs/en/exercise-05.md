# Exercise 5 – Securing the process with tests

> **Prerequisite:** Exercise 4 is complete – the gateway, the capacity check, and both process outcomes are working.
> **Working directory:** `services/process-application`
> **New in this exercise:** in-memory engine with h2, the job executor turned off, `@MockitoBean`, assertions with `BpmnAwareTests`.

## What this is about

**Monday morning. The process is running. Supposedly.**

You've built a solid process: gateway, confirmation mail, rejection. In the
Cockpit demo everything worked – once. But how do you know it will **still** work
next week, when someone attaches a boundary event, reroutes a sequence flow, or
flips a condition?

Are you going to click through the Cockpit every single time? Start PostgreSQL, fire off curl calls,
read logs? Nobody does that reliably. That's exactly where processes die quietly: a sequence flow
points into the void after a refactoring, the gateway takes the wrong path – and nobody notices
until someone gets a rejection despite a free spot.

> *"Works on my machine" is not a test strategy. It's an excuse with better PR.*

A **process test** starts the real process in an in-memory engine, lets the real
delegates run, and only replaces the business logic behind them with mocks. Which path the
process instance has to take is then written down as an **assertion** in the test – verifiable on every
build instead of just once in a demo.

## Learning goals

After this exercise you can

- secure a process as a unit test, without PostgreSQL and without any running infrastructure,
- run the engine in the test on h2 and with the job executor turned off,
- mock the use cases behind the delegates in a targeted way using `@MockitoBean`,
- execute the async continuations yourself in the test and thereby drive the instance in a controlled way up to
  the next wait state,
- check process paths with `BpmnAwareTests` (`isWaitingAt`, `hasPassedInOrder`, `isEnded`).

## Target model

![BPMN model of the exercise](../assets/exercise-05.svg)

There is **no new model**. You test the process from Exercise 4: Message Start →
Claim → Gateway → confirmation → welcome mail, respectively rejection.

Reference model (unchanged from Exercise 4): `../../models/exercise-05/newsletter.bpmn`

## The task

### 1. Add the test dependency

The assertions come from the CIB Seven port of `camunda-bpm-assert`. The version is
managed centrally in the root `pom.xml`:

```xml
<dependency>
    <groupId>org.cibseven.bpm</groupId>
    <artifactId>cibseven-bpm-assert</artifactId>
    <scope>test</scope>
</dependency>
```

`spring-boot-starter-test` (JUnit 5, Mockito, AssertJ) and `h2` are already present.

### 2. Create the test profile

**New file:** `src/test/resources/application-test.yaml`

```yaml
spring:
  main:
    allow-bean-definition-overriding: true
  datasource:
    url: jdbc:h2:mem:cibseven-test;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS exercise
    username: sa
    password:
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
        default_schema: exercise

camunda:
  bpm:
    admin-user:
      id: admin
      password: admin
    database:
      type: h2
      schema-update: true
    job-execution:
      enabled: false   # <-- the key point: we run the async continuations ourselves
    webapp:
      enabled: false

# The webapp bean validates this secret at start-up, even when the webapp is off:
cibseven:
  webclient:
    authentication:
      jwtSecret: M9nU3ORo3s+gK23D9mO5I2h+EIqnosCFDCJi+2bKoulKqZkeQT8pGYg5RhuORlf/fWhLu5meC/SPZCv9NNuj6SK/vE5Sid04UQGrnyh04EpBdiAosAO91xezjgmbSeALUtneibseGpS0tNE4RvLIl+gXiAKqNXyO
```

> **Term: job executor.** The engine's background thread. It picks up the jobs that
> arise from an asynchronous continuation (`asyncBefore` / `asyncAfter` from
> [Exercise 4](exercise-04.md)) and works through them – exactly right in production,
> but a source of randomness in a test: the test never knows how far the instance currently is.
> That's why we turn it off and run the jobs ourselves.

### 3. Create the test helper

**New file:** `src/test/java/io/miragon/training/process/util/ProcessEngineTestUtils.java`

Two helpers are enough for this exercise:

- `continueToNextWaitState(processEngine)` – runs the open async jobs one after another,
  until the instance reaches its next wait state (user task or end).
- `findProcessInstance(runtimeService, membershipId)` – finds the running instance via the
  process key `subscribeNewsletter` and the variable `membershipId`.

```java
public static void continueToNextWaitState(ProcessEngine processEngine) {
    ManagementService ms = processEngine.getManagementService();
    for (int i = 0; i < 50; i++) {
        Job job = ms.createJobQuery().active().messages().listPage(0, 1)
                .stream().findFirst().orElse(null);
        if (job == null) return;
        ms.executeJob(job.getId());
    }
}
```

You'll only need the `fireTimer` helper with the boundary events in Exercise 6. The
complete file is in the reference solution.

### 4. Write the happy-path test

**File:** `src/test/java/io/miragon/training/process/MembershipProcessTest.java`

Scaffold:

```java
@SpringBootTest
@ActiveProfiles("test")
class MembershipProcessTest {

    @Autowired private MembershipProcess membershipProcess;
    @Autowired private RuntimeService runtimeService;
    @Autowired private TaskService taskService;
    @Autowired private ProcessEngine processEngine;

    @MockitoBean private ClaimMembershipUseCase claimMembershipUseCase;
    @MockitoBean private SendConfirmationMailUseCase sendConfirmationMailUseCase;
    @MockitoBean private SendRejectionMailUseCase sendRejectionMailUseCase;
    @MockitoBean private SendWelcomeMailUseCase sendWelcomeMailUseCase;

    @BeforeEach
    void setUp() {
        init(processEngine); // BpmnAwareTests.init(...)
    }
}
```

The happy path: `claimMembership` returns `true`, the process instance waits at the user task, you
complete it, the welcome mail goes out, and the end is `endEvent_membershipConfirmed`.

```java
@Test
void happyPath_membershipIsConfirmedAndWelcomeMailIsSent() {
    when(claimMembershipUseCase.claimMembership(any())).thenReturn(true);

    Membership membership = new Membership(new Email("jane@example.com"), new Name("Jane"), new Age(30));
    membershipProcess.startProcess(membership);

    ProcessInstance instance = findProcessInstance(runtimeService, membership.id().value().toString());
    continueToNextWaitState(processEngine);

    assertThat(instance).isWaitingAt("userTask_confirmMembership");

    String taskId = taskService.createTaskQuery()
            .processInstanceId(instance.getProcessInstanceId()).singleResult().getId();
    taskService.complete(taskId);
    continueToNextWaitState(processEngine);

    assertThat(instance)
            .isEnded()
            .hasPassedInOrder(
                    "startEvent_submitRegistration",
                    "serviceTask_claimMembership",
                    "gateway_hasEmptySpots",
                    "serviceTask_sendConfirmationMail",
                    "userTask_confirmMembership",
                    "serviceTask_sendWelcomeMail",
                    "endEvent_membershipConfirmed")
            .hasNotPassed("serviceTask_sendRejectionMail", "endEvent_membershipRejected");

    verify(sendWelcomeMailUseCase).sendWelcomeMail(membership.id());
}
```

### 5. Test the rejection path yourself

Write a second test `noCapacity_membershipIsRejected`:

- `claimMembership` returns `false`.
- The process instance runs without a wait state straight through to `endEvent_membershipRejected`.
- What's checked: `serviceTask_sendRejectionMail` was passed, confirmation and
  welcome mail were **not**, and `sendWelcomeMailUseCase` was never called
  (`verify(..., never())`).

## Constraints

- The test runs **without** PostgreSQL and without a running stack. Two knobs make
  it fast and reproducible:
  1. **h2 instead of PostgreSQL** – an in-memory database that is freshly created
     and discarded for each test run (`ddl-auto: create-drop`).
  2. **Job executor off** – you run the continuations yourself from the test thread. This way
     you determine how far the instance is when you write your assertion.
- Only **the use cases** get mocked. Delegates, model, and engine run for real – otherwise
  you're testing your mocks instead of your process.
- In this exercise the element IDs are still string literals in the test. Note how many there
  are – the [add-on](exercise-05-addon.md) clears them away next.

## Expected result

Run just this one test class – from the repository root directory:

```bash
./mvnw -pl services/process-application test -Dtest=MembershipProcessTest
```

Both tests pass in a few seconds, without PostgreSQL running. If one
fails, the assertion shows you at which activity the instance actually stood.

## Self-check

- [ ] `application-test.yaml` exists, the job executor is turned off in the test profile
- [ ] `ProcessEngineTestUtils` brings the instance up to the next wait state
- [ ] The happy-path test checks the order **and** the paths not taken
- [ ] The rejection test checks that the welcome mail was never called
- [ ] Both tests pass green, without the Docker stack running

## Reference solution

`../../solutions/exercise-05/`

## Next step

The element IDs are still hand-typed strings in the test – fragile the moment someone renames
in the modeler. The add-on turns them into verified constants.

➡️ [Next — Add-on: bpmn-to-code](exercise-05-addon.md)
