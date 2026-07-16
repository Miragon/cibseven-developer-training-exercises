package io.miragon.training.adapter.outbound.teams;

import io.miragon.training.application.port.outbound.NotificationPublisherOutPort;
import io.miragon.training.domain.Notification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Publishes a {@link Notification} as an Adaptive Card to a Microsoft Teams channel via a Power
 * Automate "Workflows" webhook. No token needed — the webhook URL is the secret (supply it via
 * {@code notification.teams.webhook-url}).
 */
@Component
public class MicrosoftTeamsMessagePublisher implements NotificationPublisherOutPort {

    private final RestClient restClient;
    private final String webhookUrl;

    public MicrosoftTeamsMessagePublisher(RestClient restClient,
                                          @Value("${notification.teams.webhook-url}") String webhookUrl) {
        this.restClient = restClient;
        this.webhookUrl = webhookUrl;
    }

    @Override
    public void publish(Notification notification) {
        // TODO Aufgabe 6: POST an Adaptive Card to the Teams webhook (this.webhookUrl) via restClient.
        //   Body: { "type": "message", "attachments": [ {
        //     "contentType": "application/vnd.microsoft.card.adaptive",
        //     "content": { "$schema": "http://adaptivecards.io/schemas/adaptive-card.json",
        //                  "type": "AdaptiveCard", "version": "1.4",
        //                  "body": [ { "type": "TextBlock", "text": notification.title(), "weight": "Bolder" },
        //                            { "type": "TextBlock", "text": notification.text(), "wrap": true } ] } } ] }
        throw new UnsupportedOperationException("TODO Aufgabe 6: implement the Teams notification POST");
    }
}
