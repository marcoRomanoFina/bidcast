package com.bidcast.selection_service.session;

import com.bidcast.selection_service.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionClosedEventListener {

    private final SelectionSessionService selectionSessionService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_SELECTION_SESSION_CLOSED)
    public void onSessionClosed(SessionClosedEvent event) {
        log.info("Received session closed event for session {}", event.sessionId());
        selectionSessionService.close(event.sessionId());
    }
}
