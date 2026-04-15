package io.miragon.training.application.service;

import io.miragon.training.application.port.inbound.SendConfirmationMailUseCase;
import io.miragon.training.application.port.outbound.SubscriptionRepository;
import io.miragon.training.domain.SubscriptionId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SendConfirmationMailService implements SendConfirmationMailUseCase {

    private static final Logger log = LoggerFactory.getLogger(SendConfirmationMailService.class);

    private final SubscriptionRepository repository;

    public SendConfirmationMailService(SubscriptionRepository repository) {
        this.repository = repository;
    }

    @Override
    public void sendConfirmationMail(SubscriptionId subscriptionId) {
        var subscription = repository.find(subscriptionId);
        log.info("Sending confirmation mail to {} (subscriptionId={})", subscription.email().value(), subscriptionId.value());
    }
}
