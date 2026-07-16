package io.miragon.training.adapter.outbound.teams;

import io.miragon.training.application.port.outbound.EmployeeNotifier;
import io.miragon.training.domain.NewMember;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Alternative sink: posts an Adaptive Card to a Microsoft Teams channel via a Power Automate
 * "Workflows" webhook (template "When a Teams webhook request is received"). Enabled with
 * {@code notification.sink=teams}. No token needed in the worker — the webhook URL is the secret.
 */
@Component
@ConditionalOnProperty(name = "notification.sink", havingValue = "teams")
public class TeamsEmployeeNotifier implements EmployeeNotifier {

    private static final Logger log = LoggerFactory.getLogger(TeamsEmployeeNotifier.class);

    private final RestClient restClient;
    private final String webhookUrl;

    public TeamsEmployeeNotifier(RestClient restClient,
                                 @Value("${notification.teams.webhook-url}") String webhookUrl) {
        this.restClient = restClient;
        this.webhookUrl = webhookUrl;
    }

    @Override
    public void publish(NewMember member) {
        restClient.post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(adaptiveCardMessage(member))
                .retrieve()
                .toBodilessEntity();

        log.info("Posted new member {} to Teams", member.name());
    }

    private Map<String, Object> adaptiveCardMessage(NewMember member) {
        Map<String, Object> textBlock = Map.of(
                "type", "TextBlock",
                "text", "🎉 New Inner Circle member: " + member.name() + "!",
                "weight", "Bolder",
                "size", "Medium",
                "wrap", true
        );
        Map<String, Object> card = Map.of(
                "$schema", "http://adaptivecards.io/schemas/adaptive-card.json",
                "type", "AdaptiveCard",
                "version", "1.4",
                "body", List.of(textBlock)
        );
        Map<String, Object> attachment = Map.of(
                "contentType", "application/vnd.microsoft.card.adaptive",
                "content", card
        );
        return Map.of(
                "type", "message",
                "attachments", List.of(attachment)
        );
    }
}
