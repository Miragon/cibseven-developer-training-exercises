package io.miragon.training.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class EngineNeutralArchitectureTest(
    val rootPackage: String,
    val engine: Engine = Engine.CAMUNDA_7
) {

    @Test
    fun `does not have engine-specific dependencies`() {
        Konsist.scopeFromProduction(rootPackage).imports.assertFalse {
            it.name.startsWith(engine.packagePath)
        }
    }

    enum class Engine(val packagePath: String) {
        CAMUNDA_7("org.camunda.bpm"),
        OPERATON("org.operaton.bpm"),
        CIB_SEVEN("org.cibseven.bpm")
    }
}
