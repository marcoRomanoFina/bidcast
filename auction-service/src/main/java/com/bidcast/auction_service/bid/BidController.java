package com.bidcast.auction_service.bid;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


// controller para por el momento solamente registrar un Bid
@RestController
@RequestMapping("/api/v1/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidRegistrationService bidRegistrationService;

    @PostMapping
    public ResponseEntity<SessionBid> registerBid(@RequestBody @Valid BidRegistrationRequest request) {
        SessionBid bid = bidRegistrationService.registerBid(request);
        return ResponseEntity.ok(bid);
    }
}
