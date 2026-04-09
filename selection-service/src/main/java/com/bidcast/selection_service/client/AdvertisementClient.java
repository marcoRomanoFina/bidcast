package com.bidcast.selection_service.client;

import java.util.UUID;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import lombok.extern.slf4j.Slf4j;

// client para hablar con el advertisement
@Service
@Slf4j
public class AdvertisementClient {

    private final RestClient restClient;
    private final String advertisementServiceUrl;
    private final Validator validator;

    public AdvertisementClient(
            RestClient restClient,
            Validator validator,
            @Value("${bidcast.advertisement-service.url:http://advertisement-service:8082}") String advertisementServiceUrl) {
        this.restClient = restClient;
        this.validator = validator;
        this.advertisementServiceUrl = advertisementServiceUrl;
    }

    public AdvertisementCampaignResponse getCampaign(UUID campaignId) {
        log.debug("Fetching campaign snapshot {}", campaignId);
        AdvertisementCampaignResponse response = restClient.get()
                .uri(advertisementServiceUrl + "/api/campaigns/" + campaignId)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (req, res) -> {
                    throw new RuntimeException("Advertisement service error: " + res.getStatusCode());
                })
                .body(AdvertisementCampaignResponse.class);

        if (response == null) {
            throw new IllegalStateException("Advertisement service returned an empty body for campaign " + campaignId);
        }

        var violations = validator.validate(response);
        if (!violations.isEmpty()) {
            String details = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .sorted()
                    .findFirst()
                    .orElse("Invalid advertisement campaign response");
            throw new IllegalStateException("Advertisement service returned invalid campaign data: " + details);
        }

        return response;
    }
}
