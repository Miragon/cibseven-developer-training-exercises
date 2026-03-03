package io.miragon.training.adapter.outbound.cib7

import io.miragon.training.adapter.process.NewsletterSubscriptionProcessApi.Messages
import io.miragon.training.adapter.process.NewsletterSubscriptionProcessApi.Variables
import io.miragon.training.application.port.outbound.NewsletterSubscriptionProcess
import io.miragon.training.domain.SubscriptionId
import dev.bpmcrafters.processengineapi.CommonRestrictions
import dev.bpmcrafters.processengineapi.correlation.CorrelateMessageCmd
import dev.bpmcrafters.processengineapi.correlation.Correlation
import dev.bpmcrafters.processengineapi.correlation.CorrelationApi
import dev.bpmcrafters.processengineapi.process.StartProcessApi
import dev.bpmcrafters.processengineapi.process.StartProcessByMessageCmd
import org.springframework.stereotype.Component

@Component
class NewsletterSubscriptionProcessAdapter(
    private val startProcessApi: StartProcessApi,
    private val correlationApi: CorrelationApi,
) : NewsletterSubscriptionProcess {

    override fun submitForm(id: SubscriptionId) {
        startProcessApi.startProcess(
            cmd = StartProcessByMessageCmd(
                messageName = Messages.MESSAGE_FORM_SUBMITTED,
                payload = mapOf(
                    Variables.SUBSCRIPTION_ID to id.value.toString(),
                    CommonRestrictions.CORRELATION_KEY to id.value.toString()
                )
            )
        ).join()
    }

    override fun confirmSubscription(id: SubscriptionId) {
        //TODO: Implement message correlation
    }

    private fun messageEventRestrictions() = CommonRestrictions.builder()
        .withRestriction("useGlobalCorrelationKey", "true")
        .build()
}
