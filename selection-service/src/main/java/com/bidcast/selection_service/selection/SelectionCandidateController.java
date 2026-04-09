package com.bidcast.selection_service.selection;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/selection")
@RequiredArgsConstructor
@Tag(name = "Selection", description = "Endpoints usados por el device player para pedir reproducciones pagas ya confirmadas")
public class SelectionCandidateController {

    private final SelectionCandidateService selectionCandidateService;

    @PostMapping("/candidates")
    @Operation(
            summary = "Selecciona las proximas reproducciones para un device",
            description = "Devuelve hasta N reproducciones pagas ya reservadas para un device dentro de una session. "
                    + "Si una seleccion se devuelve, su budget ya fue consumido del hot state."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Lista de reproducciones seleccionadas",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = SelectedCandidate.class)))
    )
    @ApiResponse(responseCode = "409", description = "La session ya esta siendo procesada por otro worker")
    @ApiResponse(responseCode = "503", description = "Redis o Redisson no esta disponible en el hot path")
    public ResponseEntity<List<SelectedCandidate>> selectCandidates(
            @RequestBody @Valid CandidateSelectionRequest request) {
        return ResponseEntity.ok(selectionCandidateService.selectCandidates(request));
    }
}
