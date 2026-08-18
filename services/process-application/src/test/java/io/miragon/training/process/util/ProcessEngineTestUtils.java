package io.miragon.training.process.util;

import org.cibseven.bpm.engine.ManagementService;
import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.RuntimeService;
import org.cibseven.bpm.engine.runtime.Job;
import org.cibseven.bpm.engine.runtime.ProcessInstance;

/**
 * Kleine Helfer, um eine Prozessinstanz im Unit-Test deterministisch zu treiben.
 *
 * <p>Der Job Executor ist im {@code test}-Profil aus (siehe {@code application-test.yaml}).
 * Dadurch wartet der Prozess an jeder {@code camunda:asyncBefore/After}: kein Hintergrund-Thread
 * holt den Job ab. Diese Helfer schieben den Prozess stattdessen aus dem Testthread vorwärts –
 * das Timing bleibt vollständig unter Kontrolle und der Test schnell und reproduzierbar.
 *
 * <p>Diese Klasse ist in Aufgabe 5 <b>vorgegeben</b> – du schreibst damit deine Tests, musst sie
 * aber nicht selbst bauen. Siehe {@code docs/de/exercise-05.md}.
 */
public final class ProcessEngineTestUtils {

    private static final String PROCESS_DEFINITION_KEY = "subscribeNewsletter";

    private ProcessEngineTestUtils() {
    }

    /**
     * Führt die offenen Async-Continuation-("message")-Jobs nacheinander aus, bis die Instanz
     * ihren nächsten Wait State (User/Receive Task, Timer oder Ende) erreicht.
     */
    public static void continueToNextWaitState(ProcessEngine processEngine) {
        continueToNextWaitState(processEngine, null);
    }

    /**
     * Wie {@link #continueToNextWaitState(ProcessEngine)}, treibt aber nur die Jobs einer einzelnen
     * Instanz. Nützlich, sobald eine Aktivität ein Signal wirft, das eine zweite, unabhängige
     * Instanz startet, die von diesem Aufruf <em>nicht</em> mitgetrieben werden soll (ab Aufgabe 7).
     */
    public static void continueToNextWaitState(ProcessEngine processEngine, String processInstanceId) {
        ManagementService managementService = processEngine.getManagementService();
        for (int i = 0; i < 50; i++) {
            var query = managementService.createJobQuery().active().messages();
            if (processInstanceId != null) {
                query = query.processInstanceId(processInstanceId);
            }
            Job job = query.listPage(0, 1).stream().findFirst().orElse(null);
            if (job == null) {
                return;
            }
            managementService.executeJob(job.getId());
        }
    }

    /**
     * Feuert den Timer-Job des angegebenen Boundary-/Catch-Events direkt, unabhängig vom
     * Fälligkeitsdatum. Prüft die Timer-Verdrahtung, ohne echte Zeit abzuwarten (ab Aufgabe 6).
     */
    public static void fireTimer(ProcessEngine processEngine, String timerActivityId) {
        Job timer = processEngine.getManagementService().createJobQuery()
                .timers()
                .activityId(timerActivityId)
                .singleResult();
        if (timer == null) {
            throw new IllegalStateException("No timer job found for activity '" + timerActivityId + "'");
        }
        processEngine.getManagementService().executeJob(timer.getId());
    }

    /**
     * Findet die laufende Membership-Prozessinstanz mit der gegebenen membershipId.
     * Lässt den Test scheitern, wenn keine solche Instanz existiert.
     */
    public static ProcessInstance findProcessInstance(RuntimeService runtimeService, String membershipId) {
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(PROCESS_DEFINITION_KEY)
                .variableValueEquals("membershipId", membershipId)
                .singleResult();
        if (instance == null) {
            throw new AssertionError("No process instance found for membershipId " + membershipId);
        }
        return instance;
    }
}
