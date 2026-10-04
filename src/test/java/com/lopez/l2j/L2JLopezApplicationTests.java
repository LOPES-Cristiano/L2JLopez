package com.lopez.l2j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.lopez.l2j.config.ServerProperties;
import com.lopez.l2j.events.PlayerKilledEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class L2JLopezApplicationTests {

	@Autowired
	ServerProperties props;

	@Autowired
	ApplicationEventPublisher publisher;

	@Test
	void contextLoadsWithTypedConfig() {
		assertNotNull(props);
		assertEquals("L2JLopez", props.serverName());
		assertEquals(746, props.network().protocolMax());
	}

	@Test
	void eventsAreDelivered() {
		for (int i = 0; i < 5; i++) {
			publisher.publishEvent(new PlayerKilledEvent(1, 2, true));
		}
	}
}
