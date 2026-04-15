package io.miragon.training.adapter.inbound.cibseven

import io.github.oshai.kotlinlogging.KotlinLogging
import org.cibseven.bpm.engine.delegate.DelegateExecution
import org.cibseven.bpm.engine.delegate.JavaDelegate

abstract class BaseDelegate : JavaDelegate {

    protected val log = KotlinLogging.logger {}

    override fun execute(execution: DelegateExecution) {
        try {
            executeTask(execution)
        } catch (e: Exception) {
            log.error(e) { "Delegate execution failed for process instance ${execution.processInstanceId}" }
            throw e
        }
    }

    abstract fun executeTask(execution: DelegateExecution)
}
