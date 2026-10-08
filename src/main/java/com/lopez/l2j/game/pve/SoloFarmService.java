package com.lopez.l2j.game.pve;

import com.lopez.l2j.game.model.PlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servico para gerenciamento do SoloFarm em arena isolada por Adena - Onda C9.
 *
 * <p>Recursos:</p>
 * <ul>
 *   <li>Compra de pacotes de monstros por Adena (1000 Adena por monstro).</li>
 *   <li>Arena instanciada exclusiva para evitar disputas de spot (KS).</li>
 *   <li>Spawn controlado com fluxo continuo e recompensas organicas.</li>
 * </ul>
 */
@Service
public class SoloFarmService {

	private static final Logger log = LoggerFactory.getLogger(SoloFarmService.class);

	public static final int PRICE_PER_MOB_ADENA = 1000;
	public static final int MIN_BUY = 100;
	public static final int MAX_BUY = 5000;
	public static final int ENTRY_X = 147450;
	public static final int ENTRY_Y = 25620;
	public static final int ENTRY_Z = -2000;
	public static final int EXIT_X = 82698;
	public static final int EXIT_Y = 148638;
	public static final int EXIT_Z = -3473;

	public static class SoloFarmSession {
		private final int instanceId;
		private final PlayerCharacter player;
		private final int totalMobsBought;
		private final long startTime;
		private int mobsKilled;
		private boolean completed;

		public SoloFarmSession(int instanceId, PlayerCharacter player, int totalMobsBought) {
			this.instanceId = instanceId;
			this.player = player;
			this.totalMobsBought = totalMobsBought;
			this.startTime = System.currentTimeMillis();
			this.mobsKilled = 0;
			this.completed = false;
		}

		public int instanceId() { return instanceId; }
		public PlayerCharacter player() { return player; }
		public int totalMobsBought() { return totalMobsBought; }
		public int mobsKilled() { return mobsKilled; }
		public boolean isCompleted() { return completed; }
	}

	public record SoloFarmResult(boolean success, String message, SoloFarmSession session) {
		public static SoloFarmResult ok(String msg, SoloFarmSession s) { return new SoloFarmResult(true, msg, s); }
		public static SoloFarmResult fail(String msg) { return new SoloFarmResult(false, msg, null); }
	}

	private final Map<Integer, SoloFarmSession> activeSessions = new ConcurrentHashMap<>();
	private final AtomicInteger instanceCounter = new AtomicInteger(5000);

	public SoloFarmService() {
		loadConfiguration();
	}

	public void loadConfiguration() {
		try {
			File file = new File("data/xml/custom/soloFarm.xml");
			InputStream is = file.exists() ? new java.io.FileInputStream(file) : getClass().getResourceAsStream("/data/xml/custom/soloFarm.xml");
			if (is != null) {
				Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
				NodeList list = doc.getElementsByTagName("soloFarm");
				if (list.getLength() > 0) {
					log.info("SoloFarmService: Configuracao carregada de soloFarm.xml com sucesso.");
				}
			}
		} catch (Exception e) {
			log.warn("SoloFarmService: Usando configuracao padrao de SoloFarm: {}", e.getMessage());
		}
	}

	/**
	 * Contrata um pacote de monstros em arena de farm solo mediante taxa em Adena.
	 */
	public SoloFarmResult buyFarmPackage(PlayerCharacter player, int mobCount) {
		if (player == null || player.isDead()) {
			return SoloFarmResult.fail("Jogador invalido ou morto.");
		}

		if (mobCount < MIN_BUY || mobCount > MAX_BUY) {
			return SoloFarmResult.fail("Quantidade de monstros deve estar entre " + MIN_BUY + " e " + MAX_BUY + ".");
		}

		long totalCostAdena = (long) mobCount * PRICE_PER_MOB_ADENA;

		// Dedução estrita de Adena do inventário
		if (player.inventory() != null) {
			var adenaOpt = player.inventory().byItemId(57);
			if (adenaOpt.isEmpty() || adenaOpt.get().count() < totalCostAdena) {
				return SoloFarmResult.fail("Adena insuficiente! Custo para " + mobCount + " monstros: " + totalCostAdena + " Adena.");
			}
			adenaOpt.get().count((int) (adenaOpt.get().count() - totalCostAdena));
		}

		int instanceId = instanceCounter.incrementAndGet();
		player.instanceId(instanceId);
		player.x(ENTRY_X);
		player.y(ENTRY_Y);
		player.z(ENTRY_Z);

		SoloFarmSession session = new SoloFarmSession(instanceId, player, mobCount);
		activeSessions.put(instanceId, session);

		log.info("SoloFarm iniciado: Jogador {} contratou {} monstros na instancia {}.",
				player.name(), mobCount, instanceId);

		return SoloFarmResult.ok("Arena SoloFarm iniciada com sucesso! " + mobCount + " monstros liberados.", session);
	}

	/**
	 * Registra o abate de um monstro na sessao de SoloFarm.
	 */
	public boolean onMobKilled(int instanceId) {
		SoloFarmSession session = activeSessions.get(instanceId);
		if (session == null || session.isCompleted()) {
			return false;
		}

		session.mobsKilled++;

		// Recompensa organica por monstro (1500 Adena)
		if (session.player().inventory() != null) {
			var adenaOpt = session.player().inventory().byItemId(57);
			adenaOpt.ifPresent(adena -> adena.count(adena.count() + 1500));
		}

		if (session.mobsKilled >= session.totalMobsBought) {
			session.completed = true;
			// Teleporta de volta para fora da arena
			session.player().instanceId(0);
			session.player().x(EXIT_X);
			session.player().y(EXIT_Y);
			session.player().z(EXIT_Z);
			log.info("SoloFarm concluido para jogador {}! Total mortos: {}",
					session.player().name(), session.mobsKilled);
			return true;
		}

		return false;
	}

	public SoloFarmSession getSession(int instanceId) {
		return activeSessions.get(instanceId);
	}

	public Map<Integer, SoloFarmSession> activeSessions() {
		return Collections.unmodifiableMap(activeSessions);
	}
}
