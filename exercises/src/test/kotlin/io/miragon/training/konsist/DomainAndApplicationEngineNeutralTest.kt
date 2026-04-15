package io.miragon.training.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Checks that domain and application layers are engine-neutral.
 * Adapter layers are allowed to use engine-specific code (that's their purpose).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class DomainAndApplicationEngineNeutralTest(val rootPackage: String) {

    @Test
    fun `domain layer has no CIB Seven imports`() {
        Konsist.scopeFromProduction("$rootPackage.domain").imports.assertFalse {
            it.name.startsWith("org.cibseven.bpm")
        }
    }

    @Test
    fun `application layer has no CIB Seven imports`() {
        Konsist.scopeFromProduction("$rootPackage.application").imports.assertFalse {
            it.name.startsWith("org.cibseven.bpm")
        }
    }
}
