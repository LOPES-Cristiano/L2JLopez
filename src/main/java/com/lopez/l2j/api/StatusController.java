package com.lopez.l2j.api;

import com.lopez.l2j.config.ServerProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class StatusController {

	private final ServerProperties props;
	private final Instant startedAt = Instant.now();

	public StatusController(ServerProperties props) {
		this.props = props;
	}

	@GetMapping("/status")
	public Map<String, Object> status() {
		return Map.of(
				"server", props.serverName(),
				"status", "UP",
				"uptimeSeconds", Duration.between(startedAt, Instant.now()).toSeconds(),
				"rates", props.rates(),
				"network", props.network(),
				"features", props.features());
	}
}
