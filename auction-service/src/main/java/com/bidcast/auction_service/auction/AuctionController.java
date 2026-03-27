package com.bidcast.auction_service.auction;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// controlador para calcular el proximo ad ganador a mostrar para una session

@RestController
@RequestMapping("/api/v1/auction")
@RequiredArgsConstructor
@Validated
@Tag(name = "Auction Engine", description = "Real-time bidding engine (RTB)")
public class AuctionController {

    private final AuctionEngine auctionEngine;

    @Operation(summary = "Get the next winning ad", description = "Evaluates active bids in Redis and returns the ad with the best price and available budget.")
    @GetMapping("/next")
    public ResponseEntity<WinningAd> getNextAd(
            @RequestParam @NotBlank(message = "sessionId is required") String sessionId) {

        WinningAd result = auctionEngine.evaluateNext(sessionId);
        return ResponseEntity.ok((WinningAd) result);
    }
}
