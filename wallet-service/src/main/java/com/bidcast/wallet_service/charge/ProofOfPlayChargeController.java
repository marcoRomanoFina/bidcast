package com.bidcast.wallet_service.charge;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/proof-of-play-charges")
@RequiredArgsConstructor
@Slf4j
public class ProofOfPlayChargeController {

    private final ProofOfPlaySettlementService settlementService;

    @PostMapping
    public ResponseEntity<Void> chargeProofOfPlay(@Valid @RequestBody ProofOfPlayChargeCommand command) {
        log.info("Recibido ProofOfPlayCharge: proofOfPlayId={}", command.proofOfPlayId());

        settlementService.processProofOfPlayCharge(command);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}

