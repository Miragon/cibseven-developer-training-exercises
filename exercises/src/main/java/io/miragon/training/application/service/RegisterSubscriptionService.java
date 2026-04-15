package io.miragon.training.application.service;

import io.miragon.training.application.port.inbound.RegisterSubscriptionUseCase;
import io.miragon.training.application.port.outbound.SubscriptionProcess;
import io.miragon.training.application.port.outbound.SubscriptionRepository;
import io.miragon.training.domain.Age;
import io.miragon.training.domain.Email;
import io.miragon.training.domain.Name;
import io.miragon.training.domain.Subscription;
import io.miragon.training.domain.SubscriptionId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RegisterSubscriptionService implements RegisterSubscriptionUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegisterSubscriptionService.class);

    private final SubscriptionRepository repository;
    private final SubscriptionProcess process;

    public RegisterSubscriptionService(SubscriptionRepository repository, SubscriptionProcess process) {
        this.repository = repository;
        this.process = process;
    }

    @Override
    public SubscriptionId register(Command command) {
        log.info("Registering subscription for {}", command.email());

        // TODO Aufgabe 1:
        //  1. Erstelle ein Subscription-Objekt aus dem Command (Email, Name, Age)
        //  2. Speichere es mit repository.save(...)
        //  3. Starte den Prozess mit process.startProcess(...)
        //  4. Gib die SubscriptionId zurück
        throw new UnsupportedOperationException("Aufgabe 1: Implementiere die Subscription-Registrierung");
    }
}
