package com.bidcast.auction_service.pop;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


// controlador para registrar un PoP mandado por el device player
@RestController
@RequestMapping("/api/v1/auction")
@RequiredArgsConstructor
public class ProofOfPlayController {

    private final ProofOfPlayService proofOfPlayService;

    @PostMapping("/pop")
    public ResponseEntity<Void> recordPlay(@RequestBody @Valid PopRequest request) {
        proofOfPlayService.recordPlay(request);
        return ResponseEntity.ok().build();
    }
}
