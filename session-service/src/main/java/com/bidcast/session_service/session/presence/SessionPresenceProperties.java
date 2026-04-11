package com.bidcast.session_service.session.presence;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "adcast.session.presence")
@Validated
@Getter
@Setter
public class SessionPresenceProperties {

    @NotNull
    private Duration heartbeatInterval = Duration.ofSeconds(15);

    @NotNull
    private Duration staleAfter = Duration.ofSeconds(60);

    @NotNull
    private Duration closeEmptyAfter = Duration.ofMinutes(5);

    @NotNull
    private Duration cleanupInterval = Duration.ofSeconds(30);
}
