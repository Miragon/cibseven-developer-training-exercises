package io.miragon.training.application.service;

import io.miragon.training.application.port.inbound.SendWelcomeMailUseCase;
import io.miragon.training.application.port.outbound.SubscriptionRepository;
import io.miragon.training.domain.SubscriptionId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SendWelcomeMailService implements SendWelcomeMailUseCase {

    private static final Logger log = LoggerFactory.getLogger(SendWelcomeMailService.class);

    private final SubscriptionRepository repository;

    public SendWelcomeMailService(SubscriptionRepository repository) {
        this.repository = repository;
    }

    @Override
    public void sendWelcomeMail(SubscriptionId subscriptionId) {
        var subscription = repository.find(subscriptionId);

        // TODO Aufgabe 1:
        //  Logge eine Nachricht im Format "Sending welcome mail to [email]"
        //  In späteren Aufgaben wird hier eine echte Mail gesendet.
        throw new UnsupportedOperationException("Aufgabe 1: Logge 'Sending welcome mail to " + subscription.email().value() + "'");
    }
}
