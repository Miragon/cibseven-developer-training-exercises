package io.miragon.training.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BasicCodingGuidelinesTest(
    val pathFromRoot: String,
) {

    @Test
    fun `each class has package declaration`() {
        Konsist
            .scopeFromProduction(pathFromRoot)
            .classesAndInterfacesAndObjects()
            .assertTrue { it.resideInPackage("..") }
    }

    @Test
    fun `no wildcard imports`() {
        // Allow common framework wildcards: java.util, jakarta.persistence
        // Allow domain wildcard imports within the same module (mappers often need all domain classes)
        Konsist.scopeFromProduction().imports.assertFalse {
            it.isWildcard
                && !it.name.startsWith("java.util")
                && !it.name.startsWith("jakarta.persistence")
                && !it.name.contains(".domain")
        }
    }
}
