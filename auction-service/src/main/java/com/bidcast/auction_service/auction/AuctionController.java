package com.bidcast.auction_service.auction;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auction")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionEngine auctionEngine;

    @GetMapping("/next")
    public ResponseEntity<AuctionResult> getNextAd(@RequestParam String sessionId) {
        AuctionResult result = auctionEngine.evaluateNext(sessionId);

        return switch (result) {
            case WinningAd winningAd -> ResponseEntity.ok(winningAd);
            case NoAdFound noAdFound -> ResponseEntity.noContent().build();
        };
    }
}
