package com.bidcast.session_service;

import com.bidcast.session_service.client.SelectionClient;
import com.bidcast.session_service.core.event.EventPublisher;
import com.bidcast.session_service.core.outbox.OutboxRepository;
import com.bidcast.session_service.core.outbox.OutboxWorker;
import com.bidcast.session_service.session.presence.SessionPresenceCleanupService;
import com.bidcast.session_service.session.SessionDeviceRepository;
import com.bidcast.session_service.session.SessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(properties = {
		"spring.task.scheduling.enabled=false",
		"adcast.session.presence.cleanup.enabled=false",
		"spring.autoconfigure.exclude="
				+ "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
				+ "org.springframework.boot.jpa.autoconfigure.HibernateJpaAutoConfiguration,"
				+ "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration,"
				+ "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration,"
				+ "org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration,"
				+ "org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration"
})
class SessionServiceApplicationTests {

	@MockitoBean
	private SessionRepository sessionRepository;

	@MockitoBean
	private SessionDeviceRepository sessionDeviceRepository;

	@MockitoBean
	private SelectionClient selectionClient;

	@MockitoBean
	private EventPublisher eventPublisher;

	@MockitoBean
	private OutboxRepository outboxRepository;

	@MockitoBean
	private OutboxWorker outboxWorker;

	@MockitoBean
	private SessionPresenceCleanupService sessionPresenceCleanupService;

	@MockitoBean
	private RedisConnectionFactory redisConnectionFactory;

	@Test
	void contextLoads() {
	}

}
