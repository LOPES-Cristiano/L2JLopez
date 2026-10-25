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

	public static final int SKILL_SHADOW_SENSE = 294;

	public interface DayNightListener {
		void onDayNightChange(boolean isNight);
	}

	private final GameWorld world;
	private final List<DayNightListener> listeners = new CopyOnWriteArrayList<>();
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
			Thread.ofVirtual().name("game-time-clock-", 0).factory());

	@Autowired(required = false)
	@org.springframework.context.annotation.Lazy
	private com.lopez.l2j.game.boss.epic.ZakenService zakenService;

	private volatile boolean currentNight;
	private volatile int lastGameHour = -1;

	@Autowired
	public GameTimeController(GameWorld world) {
		this.world = world;
		this.currentNight = isNightTime(GameTime.now());
		this.lastGameHour = getGameHour();
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

	public void setZakenService(com.lopez.l2j.game.boss.epic.ZakenService zakenService) {
		this.zakenService = zakenService;
	}

	private void tick() {
		try {
			int now = GameTime.now();
			int curHour = (now / 60) % 24;
			if (curHour != lastGameHour) {
				lastGameHour = curHour;
				if (zakenService != null) {
					try {
						zakenService.onGameHourChanged(curHour);
					} catch (Exception e) {
						log.warn("Erro ao notificar ZakenService na mudanca de hora", e);
					}
				}
			}

			boolean night = isNightTime(now);
			if (night != currentNight) {
				currentNight = night;
				log.info("Transicao de ciclo de tempo: {}", currentNight ? "Anoiteceu (00:00)" : "Amanheceu (06:00)");
				if (world != null) {
					var skyPacket = currentNight
							? new com.lopez.l2j.network.game.packet.GameServerPacket.SunSet()
							: new com.lopez.l2j.network.game.packet.GameServerPacket.SunRise();
					world.broadcast(skyPacket, p -> true);

					String msg = currentNight
							? "The sun sets, night has fallen upon the kingdom."
							: "The sun rises, a new day begins.";
					world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "World", msg), p -> true);

					// Notifica jogadores Dark Elf com Shadow Sense (Skill 294)
					int sysMsgId = currentNight ? 1291 : 1292; // It is now midnight and the effect of Shadow Sense can be felt / It is now daytime...
					for (var op : world.players()) {
						if (op != null && op.character() != null && op.character().race() == 2) {
							op.send(com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage.of(
									sysMsgId,
									new com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage.SkillName(SKILL_SHADOW_SENSE, 1)));
						}
					}
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
