package io.miragon.training;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "io.miragon.training", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    @ArchTest
    static final ArchRule hexagonal_architecture = layeredArchitecture()
            .consideringAllDependencies()
            .layer("Domain").definedBy("..domain..")
            .layer("InPorts").definedBy("..application.port.inbound..")
            .layer("OutPorts").definedBy("..application.port.outbound..")
            .layer("Application").definedBy("..application.service..")
            .layer("InAdapters").definedBy("..adapter.inbound..")
            .layer("OutAdapters").definedBy("..adapter.outbound..")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("InPorts", "OutPorts", "Application", "InAdapters", "OutAdapters")
            .whereLayer("InPorts").mayOnlyBeAccessedByLayers("Application", "InAdapters")
            .whereLayer("OutPorts").mayOnlyBeAccessedByLayers("Application", "OutAdapters")
            .whereLayer("Application").mayOnlyBeAccessedByLayers("InAdapters");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_cibseven = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("org.cibseven.bpm..");

    @ArchTest
    static final ArchRule application_should_not_depend_on_cibseven = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("org.cibseven.bpm..");
}
