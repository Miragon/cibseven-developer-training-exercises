package io.miragon.training.adapter.outbound.teams;

import io.miragon.training.application.port.outbound.EmployeeNotifier;
import io.miragon.training.domain.NewMember;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Posts an Adaptive Card to a Microsoft Teams channel via a Power Automate "Workflows" webhook
 * (template "When a Teams webhook request is received"). No token needed in the worker — the
 * webhook URL is the secret (supply it via {@code notification.teams.webhook-url}).
 */
@Component
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
        // TODO Aufgabe 6: post an Adaptive Card to the Teams webhook (this.webhookUrl).
        //  Build the request body:
        //    { "type": "message",
        //      "attachments": [ {
        //        "contentType": "application/vnd.microsoft.card.adaptive",
        //        "content": { "$schema": "http://adaptivecards.io/schemas/adaptive-card.json",
        //                     "type": "AdaptiveCard", "version": "1.4",
        //                     "body": [ { "type": "TextBlock",
        //                                 "text": "🎉 New Inner Circle member: " + member.name() + "!" } ] } } ] }
        //  Then POST it with the injected `restClient` (contentType application/json).
        throw new UnsupportedOperationException("TODO Aufgabe 6: implement the Teams notification POST");
    }
}
