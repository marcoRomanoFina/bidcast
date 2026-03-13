package com.bidcast.auction_service.settlement;

import com.bidcast.auction_service.core.exception.SessionReconciliationException;
import com.bidcast.auction_service.session.DeviceSession;
import com.bidcast.auction_service.session.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Job programado para detectar y limpiar "Sesiones Fantasma".
 * Asegura que ninguna sesión quede activa indefinidamente si el hardware no reportó su cierre.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SessionReconciliationJob {

    private final SessionService sessionService;
    private final SettlementOrchestrator settlementOrchestrator;

    /**
     * Se ejecuta cada hora buscando sesiones que lleven abiertas más de 24 horas.
     */
    @Scheduled(fixedRate = 3600000)
    public void reapGhostSessions() {
        log.info("Iniciando tarea de limpieza de sesiones fantasma...");
        
        Instant threshold = Instant.now().minus(24, ChronoUnit.HOURS);
        List<DeviceSession> staleSessions = sessionService.findStaleActiveSessions(threshold);
        
        if (staleSessions.isEmpty()) {
            log.info("No se encontraron sesiones fantasma.");
            return;
        }
        
        log.info("Se encontraron {} sesiones caducas. Forzando liquidación...", staleSessions.size());
        
        for (DeviceSession session : staleSessions) {
            try {
                log.warn("Limpiando sesión {} (Activa por más de 24h)", session.getSessionId());
                
                // Forzamos el cierre y la liquidación
                sessionService.closeSession(session.getSessionId());
                settlementOrchestrator.orchestrateSettlement(session.getSessionId(), session.getPublisherId());
                
            } catch (Exception e) {
                // Envolvemos el error en nuestra excepción de dominio para trazabilidad
                log.error(new SessionReconciliationException(session.getSessionId(), e.getMessage()).getMessage());
            }
        }
        
        log.info("Tarea de limpieza finalizada.");
    }
}
