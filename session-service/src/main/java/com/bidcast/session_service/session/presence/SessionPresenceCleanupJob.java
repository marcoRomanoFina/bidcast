package com.bidcast.session_service.session.presence;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "adcast.session.presence.cleanup",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class SessionPresenceCleanupJob {

    private final SessionPresenceCleanupService sessionPresenceCleanupService;

    @Scheduled(fixedDelayString = "${adcast.session.presence.cleanup-interval:30s}")
    @SchedulerLock(name = "sessionPresenceCleanup", lockAtMostFor = "PT30S", lockAtLeastFor = "PT5S")
    public void cleanup() {
        sessionPresenceCleanupService.cleanup();
    }
}
