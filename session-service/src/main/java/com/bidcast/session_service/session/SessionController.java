package com.bidcast.session_service.session;

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
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse create(@RequestBody @Valid CreateSessionRequest request) {
        return SessionResponse.from(sessionService.create(request));
    }

    @PostMapping("/{sessionId}/devices/ready")
    public SessionResponse markDeviceReady(@PathVariable UUID sessionId, @RequestBody @Valid DeviceReadyRequest request) {
        return SessionResponse.from(sessionService.markDeviceReady(sessionId, request.deviceId()));
    }

    @PostMapping("/{sessionId}/devices/{deviceId}/heartbeat")
    public SessionResponse heartbeat(@PathVariable UUID sessionId, @PathVariable UUID deviceId) {
        return SessionResponse.from(sessionService.heartbeat(sessionId, deviceId));
    }

    @PostMapping("/{sessionId}/devices/{deviceId}/leave")
    public SessionResponse leave(@PathVariable UUID sessionId, @PathVariable UUID deviceId) {
        return SessionResponse.from(sessionService.leave(sessionId, deviceId));
    }

    @PostMapping("/{sessionId}/close")
    public SessionResponse close(@PathVariable UUID sessionId) {
        return SessionResponse.from(sessionService.close(sessionId));
    }
}
