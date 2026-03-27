package com.bidcast.auction_service.settlement;

import com.bidcast.auction_service.session.DeviceSession;
import com.bidcast.auction_service.session.SessionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionReconciliationJobTest {

    @Mock private SessionService sessionService;
    @Mock private SettlementOrchestrator settlementOrchestrator;

    @InjectMocks
    private SessionReconciliationJob job;

    @Test
    @DisplayName("JOB: Procesa múltiples páginas y avanza correctamente")
    void shouldProcessMultiplePages() {
        DeviceSession s1 = DeviceSession.builder().sessionId("s1").publisherId("p1").build();
        DeviceSession s2 = DeviceSession.builder().sessionId("s2").publisherId("p2").build();
        
        // Página 0 tiene s1 y dice que hay más
        Slice<DeviceSession> page0 = new SliceImpl<>(List.of(s1), PageRequest.of(0, 1), true);
        // Página 1 tiene s2 y dice que no hay más
        Slice<DeviceSession> page1 = new SliceImpl<>(List.of(s2), PageRequest.of(1, 1), false);

        when(sessionService.findStaleActiveSessions(any(Instant.class), eq(PageRequest.of(0, 100))))
                .thenReturn(page0);
        when(sessionService.findStaleActiveSessions(any(Instant.class), eq(PageRequest.of(1, 100))))
                .thenReturn(page1);

        job.reapGhostSessions();

        verify(sessionService).closeSession("s1");
        verify(settlementOrchestrator).orchestrateSettlement("s1", "p1");
        verify(sessionService).closeSession("s2");
        verify(settlementOrchestrator).orchestrateSettlement("s2", "p2");
        
        // Verificamos que se llamaron a las dos páginas
        verify(sessionService, times(2)).findStaleActiveSessions(any(Instant.class), any());
    }

    @Test
    @DisplayName("JOB: No buclea infinito si una sesión falla (Avanza página)")
    void shouldNotLoopInfinitelyOnFailure() {
        DeviceSession s1 = DeviceSession.builder().sessionId("s1").publisherId("p1").build();
        
        // Simulamos que s1 siempre viene en la página 0 porque falla al cerrarse
        Slice<DeviceSession> page0 = new SliceImpl<>(List.of(s1), PageRequest.of(0, 1), true);
        // Página 1 viene vacía
        Slice<DeviceSession> page1 = new SliceImpl<>(Collections.emptyList(), PageRequest.of(1, 1), false);

        when(sessionService.findStaleActiveSessions(any(Instant.class), eq(PageRequest.of(0, 100))))
                .thenReturn(page0);
        when(sessionService.findStaleActiveSessions(any(Instant.class), eq(PageRequest.of(1, 100))))
                .thenReturn(page1);

        // Simulamos fallo en s1
        doThrow(new RuntimeException("Fallo épico")).when(sessionService).closeSession("s1");

        job.reapGhostSessions();

        // Se debió intentar cerrar s1 una vez en la página 0
        verify(sessionService, times(1)).closeSession("s1");
        
        // Crucial: Se debió llamar a la página 1 después del fallo en la página 0
        verify(sessionService).findStaleActiveSessions(any(Instant.class), eq(PageRequest.of(1, 100)));
    }

    @Test
    @DisplayName("JOB: Se detiene si no hay sesiones")
    void shouldStopIfNoSessionsFound() {
        when(sessionService.findStaleActiveSessions(any(Instant.class), any()))
                .thenReturn(new SliceImpl<>(Collections.emptyList()));

        job.reapGhostSessions();

        verify(sessionService, times(1)).findStaleActiveSessions(any(Instant.class), any());
        verify(sessionService, never()).closeSession(any());
    }
}
