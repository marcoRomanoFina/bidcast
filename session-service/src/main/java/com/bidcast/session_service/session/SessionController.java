package com.bidcast.session_service.session;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.bidcast.session_service.session.dto.CreateSessionRequest;
import com.bidcast.session_service.session.dto.DeviceReadyRequest;
import com.bidcast.session_service.session.dto.SessionResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@Tag(name = "Sessions", description = "Endpoints to create, activate, keep alive, and close venue sessions")
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a new venue session",
            description = "Creates a session in WAITING_DEVICE state. The session is activated only when the first device becomes ready."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Session created successfully",
            content = @Content(schema = @Schema(implementation = SessionResponse.class))
    )
    @ApiResponse(responseCode = "409", description = "Another open session already exists for the venue")
    public SessionResponse create(@RequestBody @Valid CreateSessionRequest request) {
        return SessionResponse.from(sessionService.create(request));
    }

    @PostMapping("/{sessionId}/devices/ready")
    @Operation(
            summary = "Mark a device as ready",
            description = "Registers a device inside the session and activates the session when the first ready device arrives."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Device registered successfully",
            content = @Content(schema = @Schema(implementation = SessionResponse.class))
    )
    @ApiResponse(responseCode = "404", description = "Session not found")
    public SessionResponse markDeviceReady(@PathVariable UUID sessionId, @RequestBody @Valid DeviceReadyRequest request) {
        return SessionResponse.from(sessionService.markDeviceReady(sessionId, request.deviceId()));
    }

    @PostMapping("/{sessionId}/devices/{deviceId}/heartbeat")
    @Operation(
            summary = "Refresh device presence",
            description = "Updates the last heartbeat timestamp of a device already participating in the session."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Heartbeat accepted",
            content = @Content(schema = @Schema(implementation = SessionResponse.class))
    )
    @ApiResponse(responseCode = "404", description = "Session or device not found")
    public SessionResponse heartbeat(@PathVariable UUID sessionId, @PathVariable UUID deviceId) {
        return SessionResponse.from(sessionService.heartbeat(sessionId, deviceId));
    }

    @PostMapping("/{sessionId}/devices/{deviceId}/leave")
    @Operation(
            summary = "Mark a device as leaving the session",
            description = "Marks the device as LEFT and may return the session to WAITING_DEVICE when no ready devices remain."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Device removed from the active session",
            content = @Content(schema = @Schema(implementation = SessionResponse.class))
    )
    @ApiResponse(responseCode = "404", description = "Session or device not found")
    public SessionResponse leave(@PathVariable UUID sessionId, @PathVariable UUID deviceId) {
        return SessionResponse.from(sessionService.leave(sessionId, deviceId));
    }

    @PostMapping("/{sessionId}/close")
    @Operation(
            summary = "Close a session manually",
            description = "Closes the session and stores a SessionClosedEvent in the outbox for asynchronous publication."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Session closed successfully",
            content = @Content(schema = @Schema(implementation = SessionResponse.class))
    )
    @ApiResponse(responseCode = "404", description = "Session not found")
    public SessionResponse close(@PathVariable UUID sessionId) {
        return SessionResponse.from(sessionService.close(sessionId));
    }
}
