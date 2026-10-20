package com.lopez.l2j.game.time;

import com.lopez.l2j.network.game.GameTime;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Controlador de tempo de jogo e ciclo dia/noite (GameTimeController do L2JDream).
 * 1 dia de jogo dura 4 horas reais (1 hora de jogo = 10 minutos reais; 1 minuto de jogo = 10 segundos reais).
 * A noite ocorre entre 00:00 e 06:00 (minutos 0 a 359).
 */
@Component
public class GameTimeController {

	private static final Logger log = LoggerFactory.getLogger(GameTimeController.class);

	public interface DayNightListener {
		void onDayNightChange(boolean isNight);
	}

	private final GameWorld world;
	private final List<DayNightListener> listeners = new CopyOnWriteArrayList<>();
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
			Thread.ofVirtual().name("game-time-clock-", 0).factory());

	private volatile boolean currentNight;

	@Autowired
	public GameTimeController(GameWorld world) {
		this.world = world;
		this.currentNight = isNightTime(GameTime.now());
	}

	@PostConstruct
	public void start() {
		scheduler.scheduleAtFixedRate(this::tick, 10, 10, TimeUnit.SECONDS);
		log.info("GameTimeController iniciado: hora atual de jogo {:02d}:{:02d} ({})",
				getGameHour(), getGameMinute(), currentNight ? "Noite" : "Dia");
	}

	@PreDestroy
	public void stop() {
		scheduler.shutdown();
	}

	public void addListener(DayNightListener listener) {
		if (listener != null) {
			listeners.add(listener);
		}
	}

	public boolean isNight() {
		return currentNight;
	}

	public int getGameHour() {
		return (GameTime.now() / 60) % 24;
	}

	public int getGameMinute() {
		return GameTime.now() % 60;
	}

	public static boolean isNightTime(int gameMinutes) {
		// 00:00 a 06:00 (0 a 359 minutos) e noite
		return gameMinutes < 360;
	}

	private void tick() {
		try {
			int now = GameTime.now();
			boolean night = isNightTime(now);
			if (night != currentNight) {
				currentNight = night;
				log.info("Transicao de ciclo de tempo: {}", currentNight ? "Anoiteceu (00:00)" : "Amanheceu (06:00)");
				if (world != null) {
					String msg = currentNight
							? "The sun sets, night has fallen upon the kingdom."
							: "The sun rises, a new day begins.";
					world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "World", msg), p -> true);
				}
				for (DayNightListener l : listeners) {
					try {
						l.onDayNightChange(currentNight);
					} catch (Exception e) {
						log.warn("Erro ao notificar listener de dia/noite", e);
					}
				}
			}
		} catch (Exception ex) {
			log.error("Erro no ciclo de GameTimeController", ex);
		}
	}
}
