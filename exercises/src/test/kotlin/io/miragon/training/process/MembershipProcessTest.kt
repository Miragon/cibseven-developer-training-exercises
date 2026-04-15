package io.miragon.training.process

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

/**
 * Prozesstest für den Newsletter-Subscription-Prozess.
 *
 * Aufgabe 1: Implementiere den Happy-Path-Test.
 */
@SpringBootTest
class MembershipProcessTest {

    // TODO Aufgabe 1:
    //  Implementiere einen Prozesstest für den Happy Path:
    //  1. Starte den Prozess über den REST-Endpoint POST /api/memberships
    //  2. Prüfe, dass eine UserTask "Fill out form" existiert
    //  3. Schließe die UserTask ab (via CIB7 TaskService oder REST API)
    //  4. Prüfe, dass der Delegate aufgerufen wurde (via Mock oder Log-Assertion)
    //  5. Prüfe, dass der Prozess erfolgreich beendet wurde
    //
    //  Hinweis: Für Tests kannst du folgende CIB7-Services injecten:
    //    @Autowired lateinit var taskService: TaskService
    //    @Autowired lateinit var historyService: HistoryService
    @Test
    fun `happy path - newsletter process completes successfully`() {
        TODO("Aufgabe 1: Implementiere diesen Test")
    }
}
