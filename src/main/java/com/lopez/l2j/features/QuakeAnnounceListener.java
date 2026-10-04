package com.lopez.l2j.features;

import com.lopez.l2j.events.PlayerKilledEvent;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Piloto do padrao de eventos: feature desligavel por configuracao. */
@Component
@ConditionalOnProperty(prefix = "l2.features.quake", name = "enabled", havingValue = "true")
public class QuakeAnnounceListener {

	private static final Logger log = LoggerFactory.getLogger(QuakeAnnounceListener.class);
	private final Map<Integer, Integer> streaks = new ConcurrentHashMap<>();

	@EventListener
	public void onKill(PlayerKilledEvent event) {
		if (!event.pvp()) {
			return;
		}
		streaks.remove(event.victimId());
		int streak = streaks.merge(event.killerId(), 1, Integer::sum);
		String title = switch (streak) {
			case 3 -> "Killing Spree";
			case 5 -> "Rampage";
			case 7 -> "Godlike";
			default -> null;
		};
		if (title != null) {
			log.info("Player {} esta em {} ({} kills seguidas)", event.killerId(), title, streak);
		}
	}
}
