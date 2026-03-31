package com.bidcast.auction_service.session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

// service con metodos para crear/close sessions y preguntas sobre ellas

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final DeviceSessionRepository sessionRepository;

    @Transactional
    public void startSession(String sessionId, String deviceId, String publisherId) {
        log.info("Starting local session tracker {} for publisher {}", sessionId, publisherId);
        
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
        log.info("Closing local session tracker {}", sessionId);
        
        sessionRepository.findById(sessionId).ifPresent(foundSession -> {
            foundSession.close();
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
    public Slice<DeviceSession> findStaleActiveSessions(Instant olderThan, Pageable pageable) {
        return sessionRepository.findStaleActiveSessions(olderThan, pageable);
    }
}
