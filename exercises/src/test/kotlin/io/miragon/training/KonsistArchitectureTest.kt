package io.miragon.training

import io.miragon.training.konsist.BasicCodingGuidelinesTest
import io.miragon.training.konsist.DomainAndApplicationEngineNeutralTest
import io.miragon.training.konsist.HexagonalArchitectureTest
import org.junit.jupiter.api.Nested

class KonsistArchitectureTest {

    private val rootPackage = "io.miragon.training"

    @Nested
    inner class Architecture : HexagonalArchitectureTest(rootPackage)

    @Nested
    inner class CodingGuidelines : BasicCodingGuidelinesTest(rootPackage)

    @Nested
    inner class EngineNeutral : DomainAndApplicationEngineNeutralTest(rootPackage)
}
