# Exercise 1 – Getting the engine running

> **Prerequisite:** Exercise 0 is complete (the target process exists at the business level).
> **Working directory:** `services/process-application`
> **New in this exercise:** CIB Seven starter, engine configuration, auto-deployment, Cockpit, `act_*` tables, wait state, Generated Form.

## What this is about

In Exercise 0 you modeled the **complete target process** of the Inner Circle. Before anyone
automates the whole flow, an external consultant that Miravelo works with roughed out a
**deliberately tiny first excerpt** at the technical level: registration via a form, then a
welcome mail – nothing more. The Service Task "Send Welcome Mail" only carries a small inline
**expression** that sets the process variable `welcomeMailSentTo`. No Java, no architecture, no
double opt-in.

This mini-version is **not** the target process and **not** the target model you build on – it
exists for one purpose only: to get the engine running at all and to get a feel for deployment,
execution, and task completion. From Exercise 2 on you rebuild the process cleanly.

What's still missing is the **runtime environment**: a Spring Boot module in which the CIB Seven
engine runs. That's exactly what you'll set up now – and along the way you'll get to know the
engine, the Cockpit, and the database.

## Learning goals

After this exercise you can

- get a Spring Boot module with an embedded CIB Seven engine up and running,
- name what the engine needs in order to start (database, auto-configuration, auto-deployment),
- find your way around the Cockpit between Processes, Tasklist, and Admin,
- connect to the engine's database and map the `act_*` tables to the categories Repository,
  Runtime, and History,
- tell from the data what distinguishes a running instance from a finished one,
- explain what a **wait state** is and why a waiting instance survives a restart,
- start a process instance and work through a User Task.

## Target model

![BPMN model of the exercise](../assets/exercise-01.svg)

This is the deliberately reduced excerpt from the consultant – registration via a form, then the
welcome mail. It sits ready at
`services/process-application/src/main/resources/bpmn/membership.bpmn`. You won't model anything
in this exercise – you'll bring it to life.

## The task

> This is about **setting up and getting familiar** – no business code.

### 1. Start the database

The engine stores its entire state in a relational database – without it, it won't start.
So bring up the Docker stack first; it comes with PostgreSQL and MailHog (a local mail server
for later):

```bash
cd stack && docker-compose up -d
```

### 2. Create the database schema

All modules in this training share the schema `exercise`. Create it once – the engine does
create its own tables, but the schema has to exist already:

```bash
docker exec -i postgres psql -U admin -d cibseven-training < stack/init-schemas.sql
```

### 3. Enable the dependencies

Open `services/process-application/pom.xml` and uncomment the `TODO Exercise 1` block:
the two CIB Seven starters (`webapp-4` and `rest-4`). Only with these are the engine,
the Cockpit webapp, and the REST API even present in the module. The versions come centrally
from the root `pom.xml` – so add them **without** a `<version>`.

`spring-boot-starter-data-jpa` and the PostgreSQL driver are already active.

### 4. Enable the configuration

In `services/process-application/src/main/resources/application.yaml`, uncomment the
`TODO Exercise 1` block: database connection, Cockpit admin user, and webclient. Without a
database connection the engine won't start.

### 5. Arm the application

In `services/process-application/src/main/java/io/miragon/training/TrainingApplication.java`,
enable the commented-out imports and the annotations **`@SpringBootApplication`** and
**`@EnableJpaRepositories`**. Only then do auto-configuration and the automatic BPMN deployment
kick in: all `*.bpmn` files under `src/main/resources` are deployed into the engine at startup.

### 6. Start the application

Now everything comes together: Spring Boot starts, auto-configuration brings up the engine,
and the engine deploys every BPMN file it finds. Watch the log while it happens – it shows you
exactly this sequence.

```bash
cd services/process-application && ../../mvnw spring-boot:run
```

### 7. Connect to the database and look at the engine's tables

On the first start the engine created its complete data model itself: several dozen tables with
the prefix `act_`. It's worth a look, because afterwards you'll understand what the engine even
stores – and where to look later when there's a problem.

Connect to the database with a tool of your choice. Two ways:

- **In the IDE** (recommended, because you'll need it throughout the whole training): in
  IntelliJ via *Database* → *+* → *Data Source* → *PostgreSQL* with host `localhost`,
  port `5432`, database `cibseven-training`, user `admin`, password `admin`. In VS Code the
  *SQLTools* extension does the same.
- **On the command line:**

  ```bash
  docker exec -it postgres psql -U admin -d cibseven-training
  ```

Look at the schema `exercise` to see which tables were created. These five are the most
important ones for the training:

| Table | Prefix | Content |
|---|---|---|
| `act_re_procdef` | `re` = Repository | deployed **process definitions** (`subscribeNewsletter`) |
| `act_ru_execution` | `ru` = Runtime | **running** process instances |
| `act_ru_task` | `ru` | open **User Tasks** |
| `act_ru_variable` | `ru` | **process variables** of running instances |
| `act_hi_procinst` | `hi` = History | **completed** process instances |

As a start, check whether your model was really deployed:

```sql
SELECT key_, name_, version_ FROM exercise.act_re_procdef;
```

The `act_ru_*` tables are still empty for now – nothing is running yet. Keep that in mind, you'll
take another look shortly.

### 8. Explore the Cockpit

The same data is also available with a UI: the Cockpit is the engine's web interface and your
most important tool for the coming exercises.

Open [http://localhost:8080/webapp/#/seven/auth/start](http://localhost:8080/webapp/#/seven/auth/start) (admin / admin). Under
**Processes** you'll see `Join Inner Circle` – the display name of the model. The technical
process key behind it is `subscribeNewsletter` (you'll see it again in `act_re_procdef` shortly).
Click your way through **Cockpit** (running instances), **Tasklist** (open User Tasks), and
**Admin** (user management).

### 9. Play through the process

The process is deployed, but has never run. Start an instance from the Tasklist and drive it
by hand all the way to the end – that way you'll see where the instance waits and where it
continues on its own:

1. **Tasklist** → **Start process** → `Join Inner Circle`
2. **Create a filter (only the first time):** the CIB Seven Tasklist only shows open tasks
   through a filter – as long as none exists, the list stays empty even though your instance
   is already waiting at the User Task. Click the **+** at the top left next to **Filters**
   ("Create a filter"), give it a name (e.g. `All tasks`) and save. From now on the open tasks
   appear.
3. Open the User Task **"Fill out form"**, fill in `email`, `name`, and `age`, and complete it
4. The Service Task then sets the variable `welcomeMailSentTo` via an expression
5. The instance has reached the End Event "Member joined" – in the History it shows as
   `COMPLETED`

The form you just filled out didn't come from nowhere:

> **Term: Generated Form.** A built-in feature of CIB Seven / Camunda 7: the form fields
> (`email`, `name`, `age`) sit directly in the BPMN model on the User Task. The Tasklist renders
> a form from them automatically – no extra file, no HTML, no frontend. It's the simplest way in
> for a User Task; in Exercise 2 you create such a form yourself. For more production-like
> applications it is later replaced by dedicated interfaces.

### 10. Look at the database once more

Now it gets interesting to see what the run changed. Open the same tables as in step 7 again and
compare:

```sql
-- Running instances and open tasks: empty again, since the instance has finished
SELECT id_, proc_def_id_ FROM exercise.act_ru_execution;
SELECT id_, name_ FROM exercise.act_ru_task;

-- The History, on the other hand, recorded everything
SELECT proc_def_key_, start_time_, end_time_, state_ FROM exercise.act_hi_procinst;
SELECT act_name_, act_type_ FROM exercise.act_hi_actinst ORDER BY start_time_;
SELECT name_, text_ FROM exercise.act_hi_varinst WHERE name_ = 'welcomeMailSentTo';
```

**Notice:** while the instance was waiting at the User Task, there were rows in `act_ru_execution`
and `act_ru_task`. After completion the runtime tables are empty, and everything that happened is
in the `act_hi_*` tables. That's exactly the difference between `ru` and `hi`.

> **Term: wait state.** A point at which the process instance **stops and waits for an event
> from outside** – here the User Task, which waits for its own completion. As it does, the engine
> writes the state into the `act_ru_*` tables and releases the thread. That's why a waiting instance
> survives a restart of the application: it doesn't live in memory, but in the database.
>
> A Service Task is **not** a wait state – it runs straight through without waiting for anything.
> Besides the User Task, wait states also include Timers, Message Events, and (from Exercise 9 on)
> External Tasks. The concept accompanies you throughout the whole training: it decides where a
> process can be interrupted and – from [Exercise 4](exercise-04.md) on – where the engine
> commits automatically.

Feel free to try it out: start a second instance and leave it **standing** at the User Task. Now
the runtime tables are populated. Restart the application – the instance is still waiting at the
same spot afterwards.

## Constraints

- You work exclusively in the module `services/process-application`. It's the only module you'll
  work in across all exercises.
- The module already contains the complete skeleton of the hexagonal architecture. The business
  layer (services, REST controllers, delegates, process adapter) is commented out with
  `TODO Exercise 2` and isn't needed here yet.
- All modules run on port `8080` and in the schema `exercise`. Always start only one at a time.

## Expected result

The log shows `Auto-Deploying resources: [... membership.bpmn]` and
`Started TrainingApplication`. The Cockpit is reachable, `Join Inner Circle` appears under
**Processes**, and an instance you started runs all the way to the End Event.

## Self-check

- [ ] PostgreSQL is running, the schema `exercise` exists
- [ ] Dependencies, configuration, and `@SpringBootApplication` are enabled
- [ ] The application starts and deploys `membership.bpmn`
- [ ] `Join Inner Circle` appears in the Cockpit under **Processes**
- [ ] An instance was started, the User Task worked through via the Generated Form, the process
      finished cleanly
- [ ] The database is connected in your tool, `act_re_procdef` contains `subscribeNewsletter`
- [ ] You can explain why the `act_ru_*` tables are empty after the run and what is in the
      `act_hi_*` tables instead
- [ ] You can say in your own words what a wait state is and which element in the model is one

## Hints

- If the application starts with a database error, check step 2 first: without the schema
  `exercise`, neither Hibernate nor the engine can find their tables.
- The prefixes of the engine tables are a handy mnemonic: `re` is fixed, `ru` is moving, `hi` is
  the past.

## Reference solution

- Finished module: `../../solutions/exercise-01/`
- Model: `../../models/exercise-01/membership.bpmn`
- Load it directly into the working module (replaces `src/main` including `application.yaml`; the
  `pom.xml` and thus your dependencies from step 3 stay intact):

  ```bash
  ./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=01
  ```

## Next step

In Exercise 2 you throw away the rudimentary version and rebuild the first excerpt of the target
process cleanly – including a Generated Form that you create yourself this time, and a Service
Task that runs real Java code.

➡️ [Next: Exercise 2](exercise-02.md)
