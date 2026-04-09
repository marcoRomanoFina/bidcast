package com.bidcast.selection_service.offer;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/session-offers")
@RequiredArgsConstructor
@Tag(name = "Session Offers", description = "Alta de offers economicas que participan dentro de una session")
public class SessionOfferController {

    private final SessionOfferService sessionOfferService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Crea una offer dentro de una session",
            description = "Registra una offer economica por campaign dentro de una session, toma snapshots de creatives desde advertisement-service "
                    + "y la deja lista para entrar al hot path."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Offer creada correctamente",
            content = @Content(schema = @Schema(implementation = SessionOffer.class))
    )
    @ApiResponse(responseCode = "400", description = "Request invalido")
    public ResponseEntity<SessionOffer> create(@RequestBody @Valid CreateSessionOfferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionOfferService.create(request));
    }
}
