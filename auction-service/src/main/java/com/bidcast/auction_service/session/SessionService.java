package com.bidcast.auction_service.session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final DeviceSessionRepository sessionRepository;

    @Transactional
    public void startSession(String sessionId, String deviceId, String publisherId) {
        log.info("Iniciando rastreador de sesión local: {} para el publisher {}", sessionId, publisherId);
        
        DeviceSession deviceSession = DeviceSession.builder()
                .sessionId(sessionId)
                .deviceId(deviceId)
                .publisherId(publisherId)
                .status(SessionStatus.ACTIVE)
                .build();
        
        sessionRepository.save(deviceSession);
    }

    @Transactional
    public void closeSession(String sessionId) {
        log.info("Cerrando rastreador de sesión local: {}", sessionId);
        
        sessionRepository.findById(sessionId).ifPresent(foundSession -> {
            foundSession.setStatus(SessionStatus.CLOSED);
            foundSession.setClosedAt(Instant.now());
            sessionRepository.save(foundSession);
        });
    }

    @Transactional(readOnly = true)
    public boolean isSessionActive(String sessionId) {
        return sessionRepository.findById(sessionId)
                .map(s -> s.getStatus() == SessionStatus.ACTIVE)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<DeviceSession> findStaleActiveSessions(Instant olderThan) {
        return sessionRepository.findAll().stream()
                .filter(s -> s.getStatus() == SessionStatus.ACTIVE && s.getStartedAt().isBefore(olderThan))
                .collect(Collectors.toList());
    }
}
