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
@Tag(name = "Session Offers", description = "Endpoints to register priced offers that participate inside a session")
// Controller fino: expone el alta de offers, mientras la lógica real vive en SessionOfferService.
public class SessionOfferController {

    private final SessionOfferService sessionOfferService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create an offer inside a session",
            description = "Registers a priced offer for a campaign inside a session, takes creative snapshots from advertisement-service, "
                    + "and makes it ready for the hot path."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Offer created successfully",
            content = @Content(schema = @Schema(implementation = SessionOffer.class))
    )
    @ApiResponse(responseCode = "400", description = "Invalid request")
    public ResponseEntity<SessionOffer> create(@RequestBody @Valid CreateSessionOfferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionOfferService.create(request));
    }
}
