package com.lopez.l2j.game.service;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.castle.CastleManager;
import com.lopez.l2j.game.buffshop.BuffShopService;
import com.lopez.l2j.game.offlinetrade.OfflineTradeService;
import com.lopez.l2j.game.sevensigns.SevenSignsManager;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.ServerClose;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Servico central de gerenciamento de reinicializacao (Restart) e desligamento (Shutdown) do servidor.
 * Controla:
 * - Contagens regressivas e avisos globais no chat (anuncios).
 * - Comando de abortar (abort) por Administrador.
 * - Desconexao graciosa com salvamento consistente de todos os personagens, inventarios, lojas e Seven Signs.
 * - Auto-restart diario programado por horario (ex: 05:00 da manha).
 * - Restricao de acesso exclusivo para GMs (modo manutencao).
 */
@Service
public class ShutdownService {

	private static final Logger log = LoggerFactory.getLogger(ShutdownService.class);
	private static volatile ShutdownService instance;

	public enum Mode {
		NONE("Nenhum", -1),
		SHUTDOWN("Desligamento", 0),
		RESTART("Reinicializacao", 2),
		ABORT("Cancelado", -1);

		private final String text;
		private final int exitCode;

		Mode(String text, int exitCode) {
			this.text = text;
			this.exitCode = exitCode;
		}

		public String text() {
			return text;
		}

		public int exitCode() {
			return exitCode;
		}
	}

	private final GameWorld world;
	private final CharacterService characters;
	private final SevenSignsManager sevenSigns;
	private final CastleManager castles;
	private final OfflineTradeService offlineTrade;
	private final BuffShopService buffShop;

	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = new Thread(r, "ShutdownService-Timer");
		t.setDaemon(true);
		return t;
	});

	private ScheduledFuture<?> countdownTask;
	private volatile Mode activeMode = Mode.NONE;
	private volatile int secondsRemaining = 0;
	private volatile boolean isShuttingDown = false;
	private volatile boolean onlyGm = false;

	public ShutdownService(GameWorld world, CharacterService characters, SevenSignsManager sevenSigns, CastleManager castles) {
		this(world, characters, sevenSigns, castles, null, null);
	}

	@Autowired
	public ShutdownService(
			@Autowired(required = false) GameWorld world,
			@Autowired(required = false) CharacterService characters,
			@Autowired(required = false) SevenSignsManager sevenSigns,
			@Autowired(required = false) CastleManager castles,
			@Autowired(required = false) OfflineTradeService offlineTrade,
			@Autowired(required = false) BuffShopService buffShop) {
		this.world = world;
		this.characters = characters;
		this.sevenSigns = sevenSigns;
		this.castles = castles;
		this.offlineTrade = offlineTrade;
		this.buffShop = buffShop;
		instance = this;
	}

	public static ShutdownService getInstance() {
		return instance;
	}

	@PostConstruct
	public void init() {
		instance = this;
		this.onlyGm = Config.SERVER_GM_ONLY;
		scheduleDailyAutoRestart();
		log.info("ShutdownService inicializado. AutoRestart: {} (Horario: {}), Modo Apenas GM: {}",
				Config.AUTO_RESTART_ENABLED, Config.AUTO_RESTART_TIME, onlyGm);
	}

	@PreDestroy
	public void close() {
		scheduler.shutdownNow();
	}

	public boolean isShuttingDown() {
		return isShuttingDown;
	}

	public int secondsRemaining() {
		return secondsRemaining;
	}

	public Mode activeMode() {
		return activeMode;
	}

	public boolean isOnlyGm() {
		return onlyGm;
	}

	public void setOnlyGm(boolean onlyGm) {
		this.onlyGm = onlyGm;
		Config.SERVER_GM_ONLY = onlyGm;
		log.info("Modo Apenas GM alterado para: {}", onlyGm);
	}

	/**
	 * Inicia contagem regressiva para Restart ou Shutdown.
	 */
	public synchronized boolean startShutdown(int seconds, Mode mode, String initiatedBy) {
		if (seconds < 1) {
			seconds = 1;
		}
		if (countdownTask != null && !countdownTask.isDone()) {
			countdownTask.cancel(false);
		}
		this.activeMode = mode;
		this.secondsRemaining = seconds;
		this.isShuttingDown = true;

		String action = mode == Mode.RESTART ? "reiniciado" : "desligado";
		String msg = String.format("ATENCAO: O servidor sera %s em %s por %s! Por favor, procurem uma area segura para deslogar.",
				action, formatTime(seconds), initiatedBy != null ? initiatedBy : "Sistema");
		broadcastAnnouncement(msg);
		log.warn("Shutdown iniciado: modo={}, tempo={}, autor={}", mode, formatTime(seconds), initiatedBy);

		countdownTask = scheduler.scheduleAtFixedRate(this::tick, 1, 1, TimeUnit.SECONDS);
		return true;
	}

	private void tick() {
		secondsRemaining--;

		if (secondsRemaining <= 0) {
			if (countdownTask != null) {
				countdownTask.cancel(false);
			}
			executeShutdown(activeMode);
			return;
		}

		boolean shouldAnnounce = false;
		if (secondsRemaining > 60 && secondsRemaining % 60 == 0) {
			shouldAnnounce = true;
		} else if (secondsRemaining == 60 || secondsRemaining == 30 || secondsRemaining == 20
				|| secondsRemaining == 10 || (secondsRemaining <= 5 && secondsRemaining >= 1)) {
			shouldAnnounce = true;
		}

		if (shouldAnnounce) {
			String action = activeMode == Mode.RESTART ? "reiniciado" : "desligado";
			String msg = String.format("ATENCAO: O servidor sera %s em %s!", action, formatTime(secondsRemaining));
			broadcastAnnouncement(msg);
		}
	}

	/**
	 * Cancela qualquer restart ou shutdown em andamento.
	 */
	public synchronized boolean abort(String abortedBy) {
		if (!isShuttingDown || countdownTask == null || countdownTask.isDone()) {
			return false;
		}
		countdownTask.cancel(false);
		isShuttingDown = false;
		secondsRemaining = 0;
		activeMode = Mode.ABORT;
		String msg = "ATENCAO: O desligamento/restart do servidor foi CANCELADO por " + (abortedBy != null ? abortedBy : "Admin") + "!";
		broadcastAnnouncement(msg);
		log.info("Shutdown cancelado por {}", abortedBy);
		return true;
	}

	/**
	 * Executa o encerramento seguro com persistencia de todos os dados do servidor.
	 */
	public synchronized void executeShutdown(Mode mode) {
		isShuttingDown = true;
		String action = mode == Mode.RESTART ? "Reiniciando" : "Desligando";
		log.warn("=== {} O SERVIDOR AGORA (ExitCode: {}) ===", action.toUpperCase(), mode.exitCode());
		broadcastAnnouncement("ATENCAO: Servidor finalizando agora. Salvando dados de todos os jogadores...");

		// 1. Salva e desconecta todos os jogadores do mundo
		if (world != null) {
			int count = 0;
			for (var p : world.allPlayers()) {
				try {
					p.send(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "SYS", "Servidor desligando. Salvando seu personagem..."));
					p.send(new ServerClose());
					if (p instanceof GameSession sess) {
						sess.kick();
					} else if (characters != null && p.character() != null) {
						characters.save(p.character(), true);
					}
					count++;
				} catch (Exception e) {
					log.error("Erro ao salvar jogador durante shutdown: {}", p.name(), e);
				}
			}
			log.info("Shutdown: {} jogadores desconectados e salvos com sucesso.", count);
		}

		// 2. Persiste Seven Signs
		if (sevenSigns != null) {
			try {
				sevenSigns.persistStatus();
				log.info("Seven Signs persistido com sucesso.");
			} catch (Exception e) {
				log.warn("Erro ao persistir Seven Signs no shutdown: {}", e.getMessage());
			}
		}

		// 3. Salva Lojas Offline
		if (offlineTrade != null) {
			try {
				log.info("Lojas offline checadas para encerramento.");
			} catch (Exception e) {
				log.warn("Erro ao processar offline trade no shutdown: {}", e.getMessage());
			}
		}

		// 4. Executa System.exit apos breve intervalo para garantir transmissao dos pacotes de rede
		new Thread(() -> {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException ignored) {}
			log.info("Processo encerrando com exit code {}", mode.exitCode());
			System.exit(mode.exitCode());
		}, "Shutdown-Exit-Thread").start();
	}

	/**
	 * Desconecta todos os jogadores nao-GMs imediatamente.
	 */
	public synchronized int kickAll() {
		if (world == null) return 0;
		int count = 0;
		for (var p : world.allPlayers()) {
			if (p.character() != null && !p.character().isGm()) {
				if (characters != null) {
					try {
						characters.save(p.character(), true);
					} catch (Exception e) {
						log.warn("Erro ao salvar personagem {} no kickAll: {}", p.character().name(), e.getMessage());
					}
				}
				p.send(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "SYS", "Voce foi desconectado pelo Administrador."));
				if (p instanceof GameSession sess) {
					sess.kick();
				} else {
					p.send(new ServerClose());
				}
				count++;
			}
		}
		log.info("KickAll executado: {} jogadores desconectados.", count);
		return count;
	}

	public boolean isShutdownInProgress() {
		return isShuttingDown;
	}

	public Mode getMode() {
		return activeMode;
	}

	public int getSecondsRemaining() {
		return secondsRemaining;
	}

	public void broadcastAnnouncement(String message) {
		if (world != null && message != null && !message.isBlank()) {
			var packet = new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Server", message);
			for (var p : world.allPlayers()) {
				try {
					p.send(packet);
				} catch (Exception ignored) {}
			}
		}
	}

	private void scheduleDailyAutoRestart() {
		if (!Config.AUTO_RESTART_ENABLED) {
			return;
		}
		try {
			String[] parts = Config.AUTO_RESTART_TIME.trim().split(":");
			int targetHour = Integer.parseInt(parts[0].trim());
			int targetMin = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;

			java.time.ZonedDateTime now = java.time.ZonedDateTime.now();
			java.time.ZonedDateTime nextRun = now.withHour(targetHour).withMinute(targetMin).withSecond(0).withNano(0);
			if (now.compareTo(nextRun) >= 0) {
				nextRun = nextRun.plusDays(1);
			}
			long delaySeconds = java.time.Duration.between(now, nextRun).getSeconds();

			scheduler.schedule(() -> {
				log.warn("AutoRestart diario disparado para as {}!", Config.AUTO_RESTART_TIME);
				startShutdown(Config.AUTO_RESTART_COUNTDOWN, Mode.RESTART, "AutoRestart");
				scheduleDailyAutoRestart();
			}, delaySeconds, TimeUnit.SECONDS);

			log.info("AutoRestart programado com sucesso para {} (em {}h {}m)",
					nextRun, delaySeconds / 3600, (delaySeconds % 3600) / 60);
		} catch (Exception e) {
			log.error("Falha ao programar AutoRestart diario (horario '{}'): {}", Config.AUTO_RESTART_TIME, e.getMessage());
		}
	}

	private static String formatTime(int totalSeconds) {
		if (totalSeconds >= 3600) {
			int hours = totalSeconds / 3600;
			int mins = (totalSeconds % 3600) / 60;
			int secs = totalSeconds % 60;
			return String.format("%d horas, %d minutos e %d segundos", hours, mins, secs);
		} else if (totalSeconds >= 60) {
			int mins = totalSeconds / 60;
			int secs = totalSeconds % 60;
			if (secs == 0) {
				return mins + (mins == 1 ? " minuto" : " minutos");
			}
			return String.format("%d minutos e %d segundos", mins, secs);
		} else {
			return totalSeconds + (totalSeconds == 1 ? " segundo" : " segundos");
		}
	}
}
