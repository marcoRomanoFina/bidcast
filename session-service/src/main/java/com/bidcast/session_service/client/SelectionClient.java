package com.bidcast.session_service.client;

import com.bidcast.session_service.core.exception.SessionNotificationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class SelectionClient {

    private final RestClient restClient;
    private final String selectionServiceUrl;

    public SelectionClient(
            RestClient restClient,
            @Value("${adcast.selection-service.url:http://selection-service:8084}") String selectionServiceUrl) {
        this.restClient = restClient;
        this.selectionServiceUrl = selectionServiceUrl;
    }

    public void notifySessionCreated(SelectionSessionCreatedRequest request) {
        log.debug("Notifying selection-service about session {}", request.sessionId());
        try {
            restClient.post()
                    .uri(selectionServiceUrl + "/api/v1/internal/sessions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (req, res) -> {
                        throw new SessionNotificationException("Selection service error: " + res.getStatusCode());
                    })
                    .toBodilessEntity();
        } catch (SessionNotificationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SessionNotificationException("Could not notify selection-service", ex);
        }
    }
}
