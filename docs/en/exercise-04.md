# Exercise 4 – Capacity Check with a Gateway

> **Prerequisite:** Exercise 3 is complete – double opt-in is working, and the process starts via a message.
> **Working directory:** `services/process-application`
> **New in this exercise:** Exclusive Gateway, alternative process outcome, transaction boundaries, business key, generated Task form.

## What this is about

**Strategy meeting, Friday afternoon. Someone brought exclusive matcha lattes.**

The **Miravelo Inner Circle** gets its hard limit: a thousand spots. No more.

Why a thousand? Because scarcity creates value. Because FOMO is a business model. Because
someone read a book about luxury brands.

> *"We're not exclusive because we're good. We're exclusive because the counter in the
> database is set to 1000."*
> — Most honest comment in sprint planning

From a process perspective, this is a **gateway**: got a spot? Carry on. No spot?
Rejection mail. And because every registration is a business object with its own ID,
each process instance gets a **business key** – no more "which of the 40 running
instances was Carol again?".

## Learning goals

After this exercise you can

- model an Exclusive Gateway, set its conditions, and choose a default flow,
- implement an alternative process outcome (rejection),
- pass a decision from Java code to the gateway as a process variable,
- deliberately set **transaction boundaries** and explain why a non-repeatable
  step must commit before an external effect,
- assign a business key to a process instance,
- give a User Task a generated Task form for an approval step.

## Target model

![BPMN model of the exercise](../assets/exercise-04.svg)

Reference model: `../../models/exercise-04/membership.bpmn`

## The task

### 1. Extend the model

Before the confirmation mail is sent, a **Service Task** for the reservation and an
**Exclusive Gateway** are added, splitting the sequence flow into two paths. That's
four new elements in total.

You create the elements, delegate expressions and the gateway condition in the **Miragon BPMN
Modeler** (select the element → Properties Panel), not in the XML.

**New elements:**

| Element | Type | ID | Name | Configuration |
|---|---|---|---|---|
| Reserve spot | Service Task | `serviceTask_claimMembership` | Claim membership | Delegate Expression: `#{claimMembershipDelegate}` |
| Capacity decision | Exclusive Gateway | `gateway_hasEmptySpots` | Has empty spots | Default flow: yes path |
| Rejection mail | Service Task | `serviceTask_sendRejectionMail` | Send rejection mail | Delegate Expression: `#{sendRejectionMailDelegate}` |
| Rejection | End Event | `endEvent_membershipRejected` | Membership rejected | – |

**Condition on the no path:** `${!hasEmptySpots}`. The yes path is the default flow and
needs no condition.

### 2. Add use cases and services

Following the pattern from Exercise 3:

- **`ClaimMembershipUseCase` / `ClaimMembershipService`** – checks capacity and returns
  `true` if a spot was still free. A simple in-memory counter is enough (maximum 1000
  spots); you don't need a database for this.
- **`SendRejectionMailUseCase` / `SendRejectionMailService`** – loads the membership and
  logs the rejection with the email address.

> The capacity is deliberately kept simple. The reference solution uses an
> `AtomicInteger` together with a constant `MAX_SPOTS` right inside `ClaimMembershipService`.
> If you prefer it cleaner, model a domain object `MembershipCapacity` instead, with
> `maxSpots`, `claimedSpots`, `hasEmptySpots()` and `claim()` – functionally the two are equivalent.

### 3. Add delegates

- **`ClaimMembershipDelegate`** – reads `membershipId`, calls the use case, and writes
  its result as the process variable `hasEmptySpots` onto the `DelegateExecution`.
- **`SendRejectionMailDelegate`** – reads `membershipId` and calls the use case.

> Setting the process variable belongs in the **delegate**, not in the service: the
> service doesn't know the engine and only returns a `boolean`. This exact separation is
> checked by the `ArchitectureTest`.

### 4. Set transaction boundaries

> Theory for this: training chapter **"Async & Transaction Boundaries"** (Topic 4, *Execution
> Resilience*) – save points, default and manual boundaries, rollback in action. This is
> the first place where you apply it.

Until now the process ran completely **synchronously**. Starting with this model you set
transaction boundaries – in two steps.

**a) Boundaries at the wait states.** The engine commits automatically at every wait state –
at a User Task it has to persist the state anyway. Everywhere else you set the boundary
yourself, with an **asynchronous continuation**: the markers `asyncBefore` and `asyncAfter`
tell the engine to commit at this point, create a job, and continue the work afterwards in a
**new** transaction.

Add the two continuations that are missing here:

- `asyncBefore` on the Message Start Event `startEvent_submitRegistration` – a clean boundary
  after correlation; the `correlateMessage` call only creates the instance and returns.
- `asyncAfter` on the User Task `userTask_confirmMembership` – the completion commits immediately.
  Otherwise the completion **and** the downstream Service Task run in **one** transaction:
  if it throws, the completion rolls back with it and the task reappears in the tasklist.

**b) Boundaries at the Service Tasks.** With `claimMembership` there is, for the first time, a
**non-repeatable** step – the spot reservation – directly before a mail send. Between the
Message Start and the User Task there is **no** wait state; without further markers,
`claimMembership` and `sendConfirmationMail` therefore run in **one** engine transaction.

If the mail send throws an exception, the engine rolls back the *entire* transaction and
re-executes the job. Result: `claimMembership` runs a second time – a double-reserved spot,
even though only the mail send failed.

**Rule:** Separate the *non-repeatable* work from the *external, non-rollbackable*
effect with its own transaction boundary. Set `asyncBefore` on every Service Task with an
external effect:

| Marker | Element | Why |
|---|---|---|
| `asyncBefore` | `serviceTask_sendConfirmationMail` | commits the reservation first; a mail failure only retries the send |
| `asyncBefore` | `serviceTask_sendRejectionMail` | otherwise sits in the same transaction as `claimMembership` |
| `asyncBefore` | `serviceTask_sendWelcomeMail` | consistency; from Exercise 6 on it also matters on a parallel branch |

`claimMembership` deliberately gets **no** marker – it should commit early, together with
the token that advances in the model (the *token* is the imagined game piece that marks the
current state of an instance in the process model). The marker belongs on the *downstream*
call, which would otherwise roll back the reservation with it. In the modeler: select the
element → Properties Panel → *Asynchronous Before*.

### 5. Set the business key

When the process starts, set the `membershipId` as the business key. The correlation builder
in `MembershipProcessAdapter` (which you switched to `createMessageCorrelation(...)` in
Exercise 3) offers `processInstanceBusinessKey(...)` for this. Hook the call with the
`membershipId` into the existing chain – you fill in the concrete arguments yourself:

```java
runtimeService.createMessageCorrelation(/* message name */)
        .processInstanceBusinessKey(/* membershipId */)
        .setVariables(/* ... */)
        .correlateStartMessage();
```

The business key links the process instance to the business object: in the Cockpit, each
instance can be uniquely mapped to a registration and searched for specifically.

### 6. Task form for the approval

The User Task `userTask_confirmMembership` has no form yet – whoever opens it in the tasklist
sees not a single process variable and can only complete it blindly. Give it a
**generated Task form** (*Generated Task Form*, a built-in feature of Camunda 7 – no
extra file, no HTML), so that the approving person can see the registration data:

| Field ID | Label | Type | Purpose |
|---|---|---|---|
| `name` | Name | string | context, pre-filled from the process variable |
| `email` | E-Mail | string | context, pre-filled |
| `age` | Age | long | context, pre-filled |
| `confirmed` | Confirm membership | boolean | the actual approval (checkbox) |

`name`, `email` and `age` carry the same IDs as the process variables and are therefore
pre-filled automatically. `confirmed` is new and is stored as a boolean process variable on
completion.

> **Note: Why still a Generated Form here?** You know Generated Forms from Exercises 1 and 2 –
> as a simple way in. For an internal approval step like this one they are perfectly enough and
> therefore stay here. The actual process has long been driven by business REST endpoints (start
> in Exercise 2, rejection in Exercise 6); a production-like approval UI would be a frontend of
> its own – in the training we deliberately stick with the Generated Form.

In the modeler: select the User Task → Properties Panel → **Forms** section → add form
fields. In the XML this produces an `extensionElements` block with `camunda:formData` right
inside the User Task:

```xml
<bpmn:userTask id="userTask_confirmMembership" name="Confirm membership" camunda:asyncAfter="true">
  <bpmn:extensionElements>
    <camunda:formData>
      <camunda:formField id="name" label="Name" type="string" />
      <camunda:formField id="email" label="E-Mail" type="string" />
      <camunda:formField id="age" label="Age" type="long" />
      <camunda:formField id="confirmed" label="Confirm membership" type="boolean" />
    </camunda:formData>
  </bpmn:extensionElements>
</bpmn:userTask>
```

## Constraints

- The process key stays `subscribeNewsletter` and the message name `Message_SubscriptionRequested`
  – historical names that stay stable even as the process keeps growing at the business level
  (mentioned once in [Exercise 2](exercise-02.md)).
- The field type must match the type of the process variable, otherwise pre-filling won't
  take effect (`age` is `long`, not `string`).
- `confirmed` does not control any process flow in this exercise yet – it is only captured.
- The capacity lives in memory and is back at zero after a restart. That's intentional for
  the training.
- You can take element IDs and variable names from the reference model at any time.

## Expected result

The process now has two outcomes – check both. First the path that almost everyone takes:

**A free spot is available:**

```bash
curl -X POST http://localhost:8080/api/memberships \
  -H "Content-Type: application/json" \
  -d '{"email": "carol@miravelo.com", "name": "Carol", "age": 27}'
```

The process reserves a spot, takes the yes path, and waits at the User Task
`Confirm membership`. Open it in the tasklist (`http://localhost:8080/webapp/#/seven/auth/start`,
admin/admin): name, email, and age are pre-filled. Tick the box for *Confirm
membership* and complete the task – the instance runs through `Send Welcome Mail` to
`Membership confirmed`, and `confirmed` is `true` in the history.

**No spot left free:** Temporarily set the maximum number of spots to `0` (in the
reference solution the constant `MAX_SPOTS` in `ClaimMembershipService`), restart the
application, and send:

```bash
curl -X POST http://localhost:8080/api/memberships \
  -H "Content-Type: application/json" \
  -d '{"email": "dave@miravelo.com", "name": "Dave", "age": 30}'
```

Expected log: `Sending rejection mail to dave@miravelo.com`. The instance ends at
`Membership rejected`, without ever waiting at a User Task.

## Self-check

- [ ] The gateway has a default flow and exactly one condition (`${!hasEmptySpots}`)
- [ ] The yes path ends at `Membership confirmed`, the no path at `Membership rejected`
- [ ] `asyncBefore` is on the Message Start Event and on the three mail tasks,
      `asyncAfter` on the User Task, `claimMembership` has **no** marker
- [ ] In the Cockpit the instance carries the `membershipId` as its business key
- [ ] The Task form shows the pre-filled fields plus the `confirmed` checkbox

## Hints

**Idempotency rule of thumb:** A retry may re-execute a Service Task. As soon as an action
may happen only *once* (reservation, payment), it must either commit before the boundary or
be idempotent. With external interfaces you'll meet the same pattern again in
[Exercise 9](exercise-09.md) and in [Extra Exercise 1](extra-task-1.md).

## Reference solution

`../../solutions/exercise-04/` – or load it directly:

```bash
./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=04
```

## Next step

The process now has two outcomes – and nobody automatically checks that it takes the right
one. In Exercise 5 you secure it with a process test.

➡️ [Next: Exercise 5](exercise-05.md)
