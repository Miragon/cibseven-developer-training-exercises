import io.github.emaarco.bpmn.adapter.GenerateBpmnModelsTask
import io.github.emaarco.bpmn.domain.shared.OutputLanguage
import io.github.emaarco.bpmn.domain.shared.ProcessEngine
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.springframework)
    alias(libs.plugins.spring.dependency)
    alias(libs.plugins.bpmnToCode)
}

group = "io.miragon"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
    gradlePluginPortal()
}

dependencies {
    implementation(platform(libs.process.engine.adapter.cib7.bom))
    implementation(libs.bundles.defaultService)
    implementation(libs.bundles.database)
    implementation(libs.cib7.webapp)
    implementation(libs.bundles.processEngineApiCib7)
    testImplementation(libs.bundles.test)
    testImplementation(libs.bundles.konsist)
}

tasks.register<GenerateBpmnModelsTask>("generateBpmnModels") {
    baseDir = projectDir.toString()
    filePattern = "src/main/resources/bpmn/*.bpmn"
    outputFolderPath = "$projectDir/src/main/kotlin"
    packagePath = "io.miragon.training.adapter.process"
    outputLanguage = OutputLanguage.KOTLIN
    processEngine = ProcessEngine.CAMUNDA_7
    useVersioning = false
}

tasks.named("classes") {
    dependsOn("generateBpmnModels")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<BootJar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

java.sourceCompatibility = JavaVersion.VERSION_21
