package com.lopez.l2j.features;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.events.PlayerKilledEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
			case 3 -> Config.PVP_COLOR_SYSTEM && Config.TITLE_FOR_AMMOUNT_1 != null ? Config.TITLE_FOR_AMMOUNT_1 : "Killing Spree";
			case 5 -> Config.PVP_COLOR_SYSTEM && Config.TITLE_FOR_AMMOUNT_2 != null ? Config.TITLE_FOR_AMMOUNT_2 : "Rampage";
			case 7 -> Config.PVP_COLOR_SYSTEM && Config.TITLE_FOR_AMMOUNT_3 != null ? Config.TITLE_FOR_AMMOUNT_3 : "Godlike";
			default -> null;
		};
		if (title != null) {
			String modeTag = Config.ANNOUNCE_PK_PVP_NORMAL_MESSAGE ? "NORMAL_MSG" : "ANNOUNCEMENT";
			log.info("[{}] Player {} esta em {} ({} kills seguidas)", modeTag, event.killerId(), title, streak);
		}
	}
}

