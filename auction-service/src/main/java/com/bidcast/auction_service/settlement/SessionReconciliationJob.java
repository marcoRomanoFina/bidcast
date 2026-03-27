package com.bidcast.auction_service.settlement;

import com.bidcast.auction_service.session.DeviceSession;
import com.bidcast.auction_service.session.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Job programado para detectar y limpiar "Sesiones Fantasma".
 * Asegura que ninguna sesión quede activa indefinidamente si el hardware no
 * reportó su cierre.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SessionReconciliationJob {

    private final SessionService sessionService;
    private final SettlementOrchestrator settlementOrchestrator;
    private static final int BATCH_SIZE = 100;

    /**
     * Se ejecuta cada hora buscando sesiones que lleven abiertas más de 24 horas.
     * SchedulerLock: Garantiza exclusión mutua distribuida vía ShedLock.
     */
    @Scheduled(fixedRate = 3600000)
    @SchedulerLock(name = "SessionReconciliationJob_reapGhostSessions", lockAtMostFor = "10m", lockAtLeastFor = "1m")
    public void reapGhostSessions() {
        log.info("Starting ghost session cleanup job...");

        Instant threshold = Instant.now().minus(24, ChronoUnit.HOURS);
        int pageNumber = 0;
        long totalProcessed = 0;

        Slice<DeviceSession> slice;
        do {
            log.info("Searching expired sessions - page {}", pageNumber);
            slice = sessionService.findStaleActiveSessions(threshold, PageRequest.of(pageNumber, BATCH_SIZE));

            if (slice.isEmpty()) {
                break;
            }

            log.info("Processing batch of {} sessions (page {})...", slice.getNumberOfElements(), pageNumber);

            for (DeviceSession session : slice.getContent()) {
                try {
                    log.warn("Cleaning session {} (active for more than 24h)", session.getSessionId());

                    // Forzamos el cierre y la liquidación
                    sessionService.closeSession(session.getSessionId());
                    settlementOrchestrator.orchestrateSettlement(session.getSessionId(), session.getPublisherId());
                    totalProcessed++;

                } catch (Exception e) {
                    // Envolvemos el error en nuestra excepción de dominio para trazabilidad
                    log.error("Error processing session {}: {}", session.getSessionId(), e.getMessage());
                }
            }

            // Avanzamos de página siempre. "Salto Aceptable":
            // Si algunas fallaron y siguen ACTIVE, saltamos a la siguiente página para no
            // buclear.
            // Si todas se cerraron, la siguiente página traerá un nuevo subconjunto (o
            // estará vacía).
            // Las que se saltaron por error serán capturadas en la próxima ejecución del
            // Cron (cada 1h).
            pageNumber++;

        } while (slice.hasNext());

        if (totalProcessed == 0) {
            log.info("No ghost sessions were found.");
        } else {
            log.info("Cleanup job finished. Total processed: {}", totalProcessed);
        }
    }
}
