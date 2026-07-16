package io.miragon.training.adapter.outbound.cibseven;

// TODO Aufgabe 2: Diese Klasse einkommentieren (die Zeilen mit /* und */ entfernen)
//  und anschließend das TODO in startProcess(...) implementieren.
//  Sie benötigt die CIB-Seven-Engine, die du in Aufgabe 1 scharf schaltest.
/*
import io.miragon.training.application.port.outbound.SubscriptionProcess;
import io.miragon.training.domain.Subscription;
import org.cibseven.bpm.engine.RuntimeService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SubscriptionProcessAdapter implements SubscriptionProcess {

    private final RuntimeService runtimeService;

    public SubscriptionProcessAdapter(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Override
    public void startProcess(Subscription subscription) {
        // TODO Aufgabe 2:
        //  Starte eine neue Prozessinstanz mit dem Key "subscribeNewsletter".
        //  Übergib folgende Prozess-Variablen als Map:
        //    - "subscriptionId" -> subscription.id().value().toString()
        //    - "email"          -> subscription.email().value()
        //    - "name"           -> subscription.name().value()
        //    - "age"            -> subscription.age().value()
        //  Hinweis: runtimeService.startProcessInstanceByKey(key, variables)
        throw new UnsupportedOperationException("Aufgabe 2: Starte den Prozess via RuntimeService");
    }
}
*/
