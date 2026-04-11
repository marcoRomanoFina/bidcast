package com.bidcast.session_service.integration;

import com.bidcast.session_service.client.SelectionClient;
import com.bidcast.session_service.core.outbox.OutboxEvent;
import com.bidcast.session_service.core.outbox.OutboxRepository;
import com.bidcast.session_service.session.Session;
import com.bidcast.session_service.session.SessionRepository;
import com.bidcast.session_service.session.SessionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.task.scheduling.enabled=false",
        "adcast.session.presence.cleanup.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@SuppressWarnings({"resource"})
class SessionFlowIntegrationTest {

    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

    static GenericContainer<?> rabbitmq = new GenericContainer<>(DockerImageName.parse("rabbitmq:3.13-management-alpine"))
            .withExposedPorts(5672, 15672);

    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379)
            .withCommand("redis-server", "--requirepass", "password");

    static {
        postgres.start();
        rabbitmq.start();
        redis.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", () -> rabbitmq.getMappedPort(5672));
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("spring.data.redis.password", () -> "password");
        registry.add("spring.redis.password", () -> "password");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @MockitoBean
    private SelectionClient selectionClient;

    @Test
    void sessionLifecycle_worksAgainstRealInfrastructure() throws Exception {
        UUID venueId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();

        doNothing().when(selectionClient).notifySessionCreated(any());

        String createResponse = mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "venueId": "%s",
                                  "name": "Dinner service",
                                  "ownerId": "%s",
                                  "basePricePerSlot": 3.25
                                }
                                """.formatted(venueId, ownerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("WAITING_DEVICE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String sessionId = createResponse.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/devices/ready", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceId": "%s"
                                }
                                """.formatted(deviceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/devices/{deviceId}/heartbeat", sessionId, deviceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/devices/{deviceId}/leave", sessionId, deviceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING_DEVICE"));

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/close", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.closedReason").value("MANUAL"));

        Session persisted = sessionRepository.findById(UUID.fromString(sessionId)).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(SessionStatus.CLOSED);
        assertThat(persisted.getClosedReason().name()).isEqualTo("MANUAL");

        assertThat(redisConnectionFactory.getConnection().ping()).isNotBlank();

        verify(selectionClient, times(1)).notifySessionCreated(any());
        assertThat(outboxRepository.findAll())
                .singleElement()
                .extracting(OutboxEvent::getAggregateId)
                .isEqualTo(sessionId);
    }
}
