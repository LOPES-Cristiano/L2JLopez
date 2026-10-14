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
	private final com.lopez.l2j.game.npc.NpcTemplateTable npcTemplates;
	private final com.lopez.l2j.game.service.InventoryService inventoryService;
	private final com.lopez.l2j.game.model.ObjectIdFactory idFactory;

	public SoloFarmService() {
		this(null, null, null);
	}

	@org.springframework.beans.factory.annotation.Autowired
	public SoloFarmService(@org.springframework.beans.factory.annotation.Autowired(required = false) com.lopez.l2j.game.npc.NpcTemplateTable npcTemplates,
						   @org.springframework.beans.factory.annotation.Autowired(required = false) com.lopez.l2j.game.service.InventoryService inventoryService,
						   @org.springframework.beans.factory.annotation.Autowired(required = false) com.lopez.l2j.game.model.ObjectIdFactory idFactory) {
		this.npcTemplates = npcTemplates;
		this.inventoryService = inventoryService;
		this.idFactory = idFactory != null ? idFactory : com.lopez.l2j.game.model.ObjectIdFactory.sequential(0x56000000);
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
			if (inventoryService != null) {
				var consumed = inventoryService.consumeItem(player.inventory(), 57, (int) totalCostAdena, "SoloFarmEntry");
				if (consumed != null) {
					com.lopez.l2j.game.world.GameWorld.getInstance().sendPacket(player.objectId(),
							new com.lopez.l2j.network.game.packet.GameServerPacket.InventoryUpdate(java.util.List.of(
									com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo.of(consumed.item(),
											consumed.removed() ? com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo.REMOVED : com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo.MODIFIED))));
				}
			} else {
				adenaOpt.get().count((int) (adenaOpt.get().count() - totalCostAdena));
			}
		}

		int instanceId = instanceCounter.incrementAndGet();
		com.lopez.l2j.game.world.GameWorld.getInstance().teleportPlayer(player, ENTRY_X, ENTRY_Y, ENTRY_Z, instanceId);

		SoloFarmSession session = new SoloFarmSession(instanceId, player, mobCount);
		activeSessions.put(instanceId, session);

		int initialWave = Math.min(mobCount, 12);
		for (int i = 0; i < initialWave; i++) {
			spawnSoloFarmMob(instanceId, i);
		}

		log.info("SoloFarm iniciado: Jogador {} contratou {} monstros na instancia {}.",
				player.name(), mobCount, instanceId);

		return SoloFarmResult.ok("Arena SoloFarm iniciada com sucesso! " + mobCount + " monstros liberados.", session);
	}

	private void spawnSoloFarmMob(int instanceId, int index) {
		var world = com.lopez.l2j.game.world.GameWorld.getInstance();
		if (world == null) return;
		int npcId = 21314; // Ketra / Solo Farm Monster
		var tmpl = npcTemplates != null ? npcTemplates.get(npcId).orElse(null) : null;
		if (tmpl == null) {
			tmpl = com.lopez.l2j.game.npc.NpcTemplate.fallback(npcId, "SoloFarm Guardian", "L2Monster");
		}
		double angle = (index % 12) * (Math.PI * 2.0 / 12.0);
		int mx = ENTRY_X + (int) (Math.cos(angle) * 160);
		int my = ENTRY_Y + (int) (Math.sin(angle) * 160);
		var npc = new com.lopez.l2j.game.npc.NpcInstance(idFactory.nextId(), tmpl, mx, my, ENTRY_Z, 0);
		npc.instanceId(instanceId);
		world.addNpc(npc);
		var info = new com.lopez.l2j.network.game.packet.GameServerPacket.NpcInfo(npc);
		for (var near : world.findPlayersAround(mx, my, com.lopez.l2j.game.world.GameWorld.VISIBILITY_RADIUS, instanceId)) {
			near.send(info);
		}
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
			if (inventoryService != null) {
				var added = inventoryService.addItem(session.player().inventory(), 57, 1500, "SoloFarmKill");
				if (added != null && added.item() != null) {
					com.lopez.l2j.game.world.GameWorld.getInstance().sendPacket(session.player().objectId(),
							new com.lopez.l2j.network.game.packet.GameServerPacket.InventoryUpdate(java.util.List.of(
									com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo.of(added.item(),
											added.created() ? com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo.ADDED : com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo.MODIFIED))));
				}
			} else {
				var adenaOpt = session.player().inventory().byItemId(57);
				adenaOpt.ifPresent(adena -> adena.count(adena.count() + 1500));
			}
		}

		var world = com.lopez.l2j.game.world.GameWorld.getInstance();
		if (session.mobsKilled >= session.totalMobsBought) {
			session.completed = true;
			if (world != null) {
				world.removeNpcsByInstance(instanceId);
				world.teleportPlayer(session.player(), EXIT_X, EXIT_Y, EXIT_Z, 0);
				world.sendMessage(session.player().objectId(), "SoloFarm concluido! Voce abateu todos os " + session.mobsKilled + " monstros.");
			} else {
				session.player().instanceId(0);
				session.player().x(EXIT_X);
				session.player().y(EXIT_Y);
				session.player().z(EXIT_Z);
			}
			log.info("SoloFarm concluido para jogador {}! Total mortos: {}",
					session.player().name(), session.mobsKilled);
			return true;
		} else if (session.mobsKilled + 12 <= session.totalMobsBought) {
			spawnSoloFarmMob(instanceId, session.mobsKilled);
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
