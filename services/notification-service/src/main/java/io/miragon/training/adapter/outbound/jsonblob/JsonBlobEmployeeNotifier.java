package io.miragon.training.adapter.outbound.jsonblob;

import io.miragon.training.application.port.outbound.EmployeeNotifier;
import io.miragon.training.domain.NewMember;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * DEFAULT sink: appends the new member to a shared, public jsonblob.com list (the "Inner Circle
 * Members Wall"). GET the current array → append → PUT it back. Last-write-wins, which is fine at
 * training pace. The blob is viewed live via {@code wall.html}.
 */
@Component
@ConditionalOnProperty(name = "notification.sink", havingValue = "jsonblob", matchIfMissing = true)
public class JsonBlobEmployeeNotifier implements EmployeeNotifier {

    private static final Logger log = LoggerFactory.getLogger(JsonBlobEmployeeNotifier.class);

    private final RestClient restClient;
    private final String blobUrl;

    public JsonBlobEmployeeNotifier(RestClient restClient,
                                    @Value("${notification.jsonblob.blob-url}") String blobUrl) {
        this.restClient = restClient;
        this.blobUrl = blobUrl;
    }

    @Override
    public void publish(NewMember member) {
        // TODO Aufgabe 6: publish the member to the shared members wall (jsonblob.com)
        //  1. GET the blob at blobUrl -> a List<Map<String,Object>> (use ParameterizedTypeReference)
        //  2. Append a map {name, email, joinedAt} for the new member
        //  3. PUT the updated list back to blobUrl (contentType application/json)
        //  Use the injected `restClient`.
        throw new UnsupportedOperationException("TODO Aufgabe 6: implement the members-wall REST calls");
    }
}
