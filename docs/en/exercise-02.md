# Exercise 2 – Automate the process technically

> **Prerequisite:** Exercise 1 is complete – the engine starts and deploys `newsletter.bpmn`.
> **Working directory:** `services/process-application`
> **New in this exercise:** technical modeling, JavaDelegate, hexagonal architecture, starting a process via `RuntimeService`.

## What this is about

The newsletter is live. Since the launch of the new gravel bike, the sign-ups keep rolling in –
and someone is now clicking through every single one in the Cockpit.

That's not a solution. We're developers, we automate things, even if it's just a
newsletter for cycling enthusiasts.

> *"I'm not clicking through this 500 times by hand."*
> — The entire team, during gravel-bike season

From now on the process starts via a REST endpoint, and the Service Task
`Send Welcome Mail` runs real Java code.

## Learning goals

After this exercise you can

- complete a business-level BPMN technically (element IDs, process key, form fields,
  `isExecutable`, `historyTimeToLive`),
- bind a Service Task to a Spring bean via a **Delegate Expression**,
- name the layers of the hexagonal architecture along the path of a request,
- start a process instance from Java via the `RuntimeService`,
- implement a REST endpoint that kicks off the process.

## Target model

![BPMN model for the exercise](../assets/exercise-02.svg)

Reference model: `../../models/exercise-02/newsletter.bpmn`

The flow stays the same as in Exercise 1. What changes is the **binding**: the
Service Task no longer calls an inline expression, but your Java code.

Here's how a request travels through the architecture:

```
POST /api/subscriptions
       ↓
SubscriptionController              (adapter/inbound/rest)
       ↓
RegisterSubscriptionUseCase         (application/port/inbound)
       ↓
RegisterSubscriptionService         (application/service)          ← TODO
       ↓
SubscriptionProcess.startProcess()  (application/port/outbound)
       ↓
SubscriptionProcessAdapter          (adapter/outbound/cibseven)    ← TODO
       ↓
RuntimeService.startProcessInstanceByKey(...)
```

And here's how the engine calls back into your code:

```
[BPMN: serviceTask_sendWelcomeMail]
       ↓
SendWelcomeMailDelegate             (adapter/inbound/cibseven)     ← TODO
       ↓
SendWelcomeMailUseCase              (application/port/inbound)
       ↓
SendWelcomeMailService              (application/service)          ← TODO
```

## The task

### 1. Uncomment the business layer

The classes for this exercise are commented out with `TODO Aufgabe 2` – they depended on the
engine that was only activated in Exercise 1. In each of these files, remove the lines with
`/*` and `*/`:

- `application/service/RegisterSubscriptionService.java`
- `application/service/SendWelcomeMailService.java`
- `adapter/inbound/rest/SubscriptionController.java`
- `adapter/inbound/cibseven/BaseDelegate.java`
- `adapter/inbound/cibseven/SendWelcomeMailDelegate.java`
- `adapter/outbound/cibseven/SubscriptionProcessAdapter.java`

### 2. Complete the model technically

The file `src/main/resources/bpmn/newsletter.bpmn` in the module is the consultant's version
from Exercise 1 – already technically complete. You have two options:

- **Model it yourself (recommended):** Take your business-level model from Exercise 0, add the
  technical attributes from the tables below in the Miragon BPMN Modeler, and use it to replace
  the file in the module. That way you practice technical modeling on your own model.
- **Follow along:** Open the existing file and check it against the tables. What the
  consultant set, you'll then see attribute by attribute.

**Element IDs and names:**

| Element | Type | ID | Name |
|---|---|---|---|
| Start | None Start Event | `startEvent_newsletterWanted` | Newsletter wanted |
| Form | User Task | `userTask_fillOutForm` | Fill out form |
| Welcome mail | Service Task | `serviceTask_sendWelcomeMail` | Send Welcome Mail |
| End | None End Event | `endEvent_userSubscribed` | User subscribed |

**Process properties:** process key `subscribeNewsletter` · `Executable` enabled ·
`History Time To Live` = `180`

**Form fields** (on the User Task `userTask_fillOutForm`):

| Field ID | Label | Type |
|---|---|---|
| `email` | E-Mail | string |
| `name` | Name | string |
| `age` | Age | long |

### 3. Bind the Service Task to the delegate

This is the substantive change compared to Exercise 1 – even if you keep using the existing
file:

| Element | Before (Exercise 1) | Now |
|---|---|---|
| `serviceTask_sendWelcomeMail` | Implementation: *Expression*, `${execution.setVariable('welcomeMailSentTo', email)}` | Implementation: **Delegate Expression**, `#{sendWelcomeMailDelegate}` |

### 4. Implement `RegisterSubscriptionService`

**File:** `application/service/RegisterSubscriptionService.java`

Replace the `TODO` with this logic:

1. Create a `Subscription` object from the command's email, name, and age.
2. Save it via the repository.
3. Start the process via the process port.
4. Return `subscription.id()`.

### 5. Implement `SendWelcomeMailService`

**File:** `application/service/SendWelcomeMailService.java`

Load the subscription via the repository and log the email address the welcome mail
goes to.

### 6. Implement `SendWelcomeMailDelegate`

**File:** `adapter/inbound/cibseven/SendWelcomeMailDelegate.java`

Replace the `TODO` in `executeTask(execution)`:

- Read the process variable `subscriptionId` from the `DelegateExecution`.
- Use it to call `useCase.sendWelcomeMail(...)`.

### 7. Implement `SubscriptionProcessAdapter`

**File:** `adapter/outbound/cibseven/SubscriptionProcessAdapter.java`

Replace the `TODO` in `startProcess(subscription)`:

- Start the instance with `runtimeService.startProcessInstanceByKey(...)` and the process key
  `subscribeNewsletter`.
- Pass the process variables `subscriptionId`, `email`, `name`, and `age` as a map. The
  keys must match the variable names in the model exactly.

## Constraints

- **Element ID convention** – mandatory in every exercise from now on:

  | Prefix | For |
  |---|---|
  | `startEvent_` | Start Events |
  | `endEvent_` | End Events |
  | `userTask_` | User Tasks |
  | `serviceTask_` | Service Tasks |
  | `gateway_` | Gateways |
  | `subProcess_` | Subprocesses |
  | `boundaryEvent_` | Boundary Events |

- The delegate is an **adapter**: it reads process variables and calls a use case.
  Business logic belongs in the service, not in the delegate.
- The domain classes (`domain/`) stay free of framework imports. The ArchUnit test
  `ArchitectureTest` verifies this.

## Expected result

Restart the application so the changed model gets deployed:

```bash
cd services/process-application && ../../mvnw spring-boot:run
```

Then trigger the process via the REST interface – from now on no one starts it by hand
in the Cockpit anymore:

```bash
curl -X POST http://localhost:8080/api/subscriptions \
  -H "Content-Type: application/json" \
  -d '{"email": "alice@miravelo.com", "name": "Alice", "age": 28}'
```

The call returns the ID of the subscription. In the Cockpit
(`http://localhost:8080/webapp/#/seven/auth/start`, admin/admin) an instance of
`Subscribe Newsletter` then runs, with `Fill out form` in the **Tasklist**. After completing the
task, the Service Task runs through and the log shows
`Sending welcome mail to alice@miravelo.com`.

## Self-check

- [ ] All six classes are uncommented and compile
- [ ] The Service Task uses `#{sendWelcomeMailDelegate}` instead of the inline expression
- [ ] A `POST /api/subscriptions` creates a process instance with the four process variables
- [ ] After completing the User Task, the log line with the email address appears in the log
- [ ] The instance ends at `endEvent_userSubscribed`
- [ ] `./mvnw -pl services/process-application test -Dtest=ArchitectureTest` is green

## Hints

Process tests get their own spot in [Exercise 5](exercise-05.md) – there you write
a full-fledged test against the then-finished process. The placeholder already lives
under `src/test/java/io/miragon/training/process/MembershipProcessTest.java`.

## Reference solution

`../../solutions/exercise-02/` – or load it directly:

```bash
./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=02
```

## Next step

In Exercise 3 the confirmation step is added – and the process is no longer started directly,
but via a message.

➡️ [Next: Exercise 3](exercise-03.md)
