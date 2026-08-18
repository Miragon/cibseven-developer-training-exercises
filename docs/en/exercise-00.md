# Exercise 0 – Model the process at the business level

> **Prerequisite:** none – this is where you start.
> **Working directory:** any folder you like (no code yet, no module yet).
> **New in this exercise:** BPMN modeler, Start Event, User Task, Service Task, End Event.

## What this is about

**Miravelo** is an online shop for premium bikes – gravel bikes for long weekend
tours, road bikes for everyone who likes to move fast on the asphalt. The customers
are young, brand-conscious and pretty passionate.

The shop is growing, new products keep arriving, and the team decides: let's build a
**newsletter**. Someone signs up, gets a welcome mail – done.

> *"That's built in an hour, surely."*
> — Every developer who has ever underestimated a newsletter.

Before anything gets automated, you capture the flow at the **business level**: what
happens, and in what order? This is the language in which the business side and
development reach agreement – without a single line of technology.

## Learning goals

After this exercise you can

- install a BPMN modeler and create a model in it,
- tell apart the four basic elements Start Event, User Task, Service Task and End Event,
- model a business process as an end-to-end flow,
- name elements so that the business side understands them without follow-up questions.

## Target model

![BPMN model for the exercise](../assets/exercise-00.svg)

## The task

### 1. Install the modeler

We work with the **[Miragon BPMN Modeler](https://miragon.github.io/bpmn-modeler/)**.
It comes as a VS Code extension, as an IntelliJ plugin and as a standalone desktop app –
pick the variant that fits your environment.

### 2. Model the process

Create a new BPMN diagram and model the sign-up process with exactly these four
elements:

| Element | Type | Name |
|---|---|---|
| Start | None Start Event | Newsletter wanted |
| Form | User Task | Fill out form |
| Welcome mail | Service Task | Send Welcome Mail |
| End | None End Event | User subscribed |

### 3. Connect the flow

Connect the elements with sequence flows into an end-to-end path from the Start Event
to the End Event.

### 4. Save the model

Save the file as `newsletter.bpmn` in a folder of your choice. You'll need it again in
**Exercise 2**.

## Constraints

- **Business level only.** This is purely about flow and naming. Process and element
  IDs by convention, form fields, wiring the Service Task to Java code as well as
  `isExecutable` and `historyTimeToLive` are deliberately left out here.
- **Don't copy it into the module yet.** Under `services/process-application/src/main/resources/bpmn/`
  there is already a `newsletter.bpmn` – that is the technically finished version you'll
  need in Exercise 1. Don't overwrite it just yet.
- Model names are English throughout; the task descriptions are available in German and
  English.

## Expected result

Your model shows four elements in a row, connected by three sequence flows. The modeler
reports no errors, and someone from the business side could read the flow out loud
without asking what a single element means.

## Self-check

- [ ] The model contains exactly one Start Event and one End Event
- [ ] User Task and Service Task sit between them in the right order
- [ ] All four elements are connected via sequence flows – no dangling element
- [ ] The names match the table above
- [ ] The file is saved as `newsletter.bpmn`

## Hints

Why a User Task and a Service Task? The **User Task** waits for a human – someone fills
out the form. The **Service Task** is handled by a system – here the mail dispatch. This
distinction is the most important one in business-level modeling: it defines where the
process waits and where it continues on its own.

## Reference solution

`../../models/exercise-00/newsletter.bpmn` – open the model in the modeler and compare it
with yours.

## Next step

In Exercise 1 you get the engine running that executes exactly these kinds of models.

➡️ [Next: Exercise 1](exercise-01.md)
