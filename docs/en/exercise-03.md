# Exercise 3 – Automating a step

> **Prerequisite:** Exercise 2 is complete – "Confirm membership" is a User Task with a form.
> **Working directory:** `services/process-application`
> **New in this exercise:** Manual Task → Service Task, JavaDelegate, Delegate Expression, hexagonal architecture (Delegate → Use Case → Service).

## What this is about

One placeholder is still left: "Send welcome mail" so far runs through as a Manual Task. Now it
should actually do something. For that it becomes a **Service Task** – the element for work that
a **system** does.

So that the engine knows *which* code to run, you bind the Service Task via a **Delegate
Expression** to a Spring bean: the **JavaDelegate**. The delegate is the point where the engine
calls back into your code.

> *"We're developers. A task nobody executes is a to-do note, not a process."*

## Learning goals

After this exercise you can

- convert a Manual Task into a **Service Task**,
- bind a Service Task to a Spring bean via a **Delegate Expression**,
- implement a **JavaDelegate** that reads a process variable and calls a use case,
- name the layers of the hexagonal architecture along one execution (Delegate → Use Case → Service).

## Target model

![BPMN model of the exercise](../assets/exercise-03.svg)

Reference model: `../../models/exercise-03/membership.bpmn`

Compared to Exercise 2 exactly one element changes: the Manual Task "Send welcome mail" becomes
the Service Task `serviceTask_sendWelcomeMail`, bound to `#{sendWelcomeMailDelegate}`. The process
still starts via the start form in the Cockpit.

## The task

### 1. Enable the delegate layer

The classes for this exercise are commented out with `TODO Exercise 3`. Uncomment them:

- `application/port/inbound/SendWelcomeMailUseCase.java`
- `application/service/SendWelcomeMailService.java`
- `adapter/inbound/cibseven/BaseDelegate.java`
- `adapter/inbound/cibseven/SendWelcomeMailDelegate.java`

`SendWelcomeMailService` and `BaseDelegate` are complete afterwards; the delegate still carries a
`TODO` – you write the engine binding yourself.

### 2. Convert "Send welcome mail" into a Service Task

In the modeler, change the element's type from **Manual Task** to **Service Task** and bind it to
the delegate:

| Element | Type | ID | Name | Configuration |
|---|---|---|---|---|
| Welcome mail | Service Task | `serviceTask_sendWelcomeMail` | Send Welcome Mail | Delegate Expression: `#{sendWelcomeMailDelegate}` |

### 3. Implement `SendWelcomeMailService`

**File:** `application/service/SendWelcomeMailService.java`

The service logs the email address the welcome mail goes to. Business logic belongs here, not in
the delegate.

### 4. Implement `SendWelcomeMailDelegate`

**File:** `adapter/inbound/cibseven/SendWelcomeMailDelegate.java` – **write it yourself.**

Replace the `TODO` in `executeTask(execution)`:

- Read the process variable `email` via the `DelegateExecution` (it comes from the start form).
- Use it to call `useCase.sendWelcomeMail(...)`.

Which method of the `DelegateExecution` gives you the variable, you figure out yourself – the
task text names the API, not the finished line.

## Constraints

- The **delegate** is an **adapter**: it reads process variables and calls a use case. Business
  logic belongs in the service, not in the delegate.
- In this exercise the delegate reads the raw `email` directly from the process variable. A
  business Membership with its own ID and persistence comes only in Exercise 4.
- Start and confirmation still run through the Cockpit – no REST yet.

## Expected result

Restart the application and start an instance via the Tasklist (fill in the start form). Complete
the User Task `Confirm membership`. Then the Service Task runs through, and the log shows:

```
Sending welcome mail to alice@miravelo.com
```

The instance ends at `Member joined`.

## Self-check

- [ ] `serviceTask_sendWelcomeMail` is a Service Task and uses `#{sendWelcomeMailDelegate}`
- [ ] `SendWelcomeMailDelegate` is implemented yourself (no `UnsupportedOperationException` stub anymore)
- [ ] The delegate reads `email` from the `DelegateExecution` and calls the use case
- [ ] After completing the User Task, the log line with the email address appears in the log
- [ ] The instance ends at `Member joined`

## Hints

Why the separation delegate / service? The **delegate** knows the engine (`DelegateExecution`,
process variables); the **service** knows only the business logic. That keeps the business logic
testable without starting an engine – you'll make use of that in Exercise 6 in the process test.

## Reference solution

`../../solutions/exercise-03/` – or load it directly:

```bash
./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=03
```

## Next step

In Exercise 4 the **application** takes over the process: registration via REST, start via a
message, confirmation via a REST endpoint.

➡️ [Next: Exercise 4](exercise-04.md)
