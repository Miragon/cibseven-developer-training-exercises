package io.miragon.training.adapter.outbound.jsonblob;

import io.miragon.training.application.port.outbound.EmployeeNotifier;
import io.miragon.training.domain.NewMember;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        List<Map<String, Object>> members = restClient.get()
                .uri(blobUrl)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        List<Map<String, Object>> updated = members == null ? new ArrayList<>() : new ArrayList<>(members);
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("name", member.name());
        entry.put("email", member.email());
        entry.put("joinedAt", member.joinedAt());
        updated.add(entry);

        restClient.put()
                .uri(blobUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(updated)
                .retrieve()
                .toBodilessEntity();

        log.info("Added {} to the members wall ({} members now)", member.name(), updated.size());
    }
}
