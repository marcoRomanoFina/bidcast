package com.bidcast.selection_service.pop;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/selection")
@RequiredArgsConstructor
@Tag(name = "Proof Of Play", description = "Confirmation of plays that were actually shown by the device player")
// Controller de confirmación de reproducción.
// Mantiene el endpoint chico y delega validación/reglas reales al service.
public class ProofOfPlayController {

    private final ProofOfPlayService proofOfPlayService;

    @PostMapping("/pop")
    @Operation(
            summary = "Record a Proof of Play",
            description = "Validates the signed receipt, persists the real playback, and updates idempotency and global campaign recency."
    )
    @ApiResponse(responseCode = "200", description = "Proof of Play recorded successfully")
    @ApiResponse(responseCode = "400", description = "The receipt is invalid or does not match the reported playback")
    @ApiResponse(responseCode = "503", description = "Redis is unavailable for idempotency or global recency tracking")
    public ResponseEntity<Void> recordPlay(@RequestBody @Valid PopRequest request) {
        proofOfPlayService.recordPlay(request);
        return ResponseEntity.ok().build();
    }
}
