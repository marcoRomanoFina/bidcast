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
@Tag(name = "Proof Of Play", description = "Confirmacion de reproducciones efectivamente mostradas por el device player")
public class ProofOfPlayController {

    private final ProofOfPlayService proofOfPlayService;

    @PostMapping("/pop")
    @Operation(
            summary = "Registra un Proof of Play",
            description = "Valida el receipt firmado, persiste la reproduccion real y actualiza idempotencia y recencia global por campaign."
    )
    @ApiResponse(responseCode = "200", description = "PoP registrado correctamente")
    @ApiResponse(responseCode = "400", description = "El receipt no es valido o no coincide con la reproduccion informada")
    @ApiResponse(responseCode = "503", description = "Redis no esta disponible para idempotencia o recencia global")
    public ResponseEntity<Void> recordPlay(@RequestBody @Valid PopRequest request) {
        proofOfPlayService.recordPlay(request);
        return ResponseEntity.ok().build();
    }
}
