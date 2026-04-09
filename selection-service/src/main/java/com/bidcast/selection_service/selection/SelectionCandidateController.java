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
@Tag(name = "Selection", description = "Endpoints used by the device player to request already confirmed paid plays")
// Controller del hot path de selección.
// Su trabajo es exponer el endpoint; la lógica real vive abajo en services especializados.
public class SelectionCandidateController {

    private final SelectionCandidateService selectionCandidateService;

    @PostMapping("/candidates")
    @Operation(
            summary = "Select the next plays for a device",
            description = "Returns up to N already reserved paid plays for a device inside a session. "
                    + "If a selection is returned, its budget has already been consumed from hot state."
    )
    @ApiResponse(
            responseCode = "200",
            description = "List of selected plays",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = SelectedCandidate.class)))
    )
    @ApiResponse(responseCode = "409", description = "The session is already being processed by another worker")
    @ApiResponse(responseCode = "503", description = "Redis or Redisson is unavailable in the hot path")
    public ResponseEntity<List<SelectedCandidate>> selectCandidates(
            @RequestBody @Valid CandidateSelectionRequest request) {
        return ResponseEntity.ok(selectionCandidateService.selectCandidates(request));
    }
}
