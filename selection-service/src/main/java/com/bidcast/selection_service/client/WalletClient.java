package com.bidcast.selection_service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

// client para hablar con el wallet-service

@Service
@Slf4j
public class WalletClient {

    private final RestClient restClient;
    private final String walletServiceUrl;

    public WalletClient(
            RestClient restClient,
            @Value("${bidcast.wallet-service.url:http://wallet-service:8083}") String walletServiceUrl) {
        this.restClient = restClient;
        this.walletServiceUrl = walletServiceUrl;
    }

    public void freeze(WalletFreezeRequest request) {
        log.debug("Calling wallet freeze for advertiser {}", request.advertiserId());
        restClient.post()
                .uri(walletServiceUrl + "/api/v1/wallet/freeze")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (req, res) -> {
                    throw new RuntimeException("Wallet service error: " + res.getStatusCode());
                })
                .toBodilessEntity();
    }

    public void unfreeze(WalletFreezeRequest request) {
        log.debug("Calling wallet unfreeze for advertiser {}", request.advertiserId());
        restClient.post()
                .uri(walletServiceUrl + "/api/v1/wallet/unfreeze")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (req, res) -> {
                    throw new RuntimeException("Wallet service error: " + res.getStatusCode());
                })
                .toBodilessEntity();
    }
}
