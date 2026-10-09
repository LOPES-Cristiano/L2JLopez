package com.lopez.l2j.game.boss.epic;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.boss.BossStatus;
import com.lopez.l2j.game.boss.GrandBossManager;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.npc.NpcTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Servico e gerenciador de Core (Cruma Tower) e Orfen (Sea of Spores) (CoreManager e OrfenManager do L2JDream / L2JLucera2).
 * Controla os guardas Susceptor do Core, minions Riba Iren da Orfen, recuo ao ninho com HP < 50%,
 * teletransporte de atacantes distantes e recompensas lendarias (Ring of Core 6662, Earring of Orfen 6661).
 */
@Service
public class CoreAndOrfenService {

	private static final Logger log = LoggerFactory.getLogger(CoreAndOrfenService.class);

	// Core
	public static final int CORE = 29006;
	public static final int SUSCEPTOR = 29007;
	public static final int RING_OF_CORE = 6662;
	public static final int CORE_X = 17726;
	public static final int CORE_Y = 108915;
	public static final int CORE_Z = -6480;

	// Orfen
	public static final int ORFEN = 29014;
	public static final int RIBA_IREN = 29016;
	public static final int EARRING_OF_ORFEN = 6661;
	public static final int ORFEN_X = 55024;
	public static final int ORFEN_Y = 17368;
	public static final int ORFEN_Z = -5412;
	public static final int ORFEN_NEST_X = 43728;
	public static final int ORFEN_NEST_Y = 17220;
	public static final int ORFEN_NEST_Z = -4342;

	private BossStatus coreStatus = BossStatus.NOTSPAWN;
	private BossStatus orfenStatus = BossStatus.NOTSPAWN;

	private NpcInstance activeCore;
	private NpcInstance activeOrfen;
	private boolean orfenInNest = false;

	private final GrandBossManager grandBossManager;
	private final NpcTemplateTable templates;
	private final GameWorld world;
	private final ObjectIdFactory objectIds;

	@Autowired
	public CoreAndOrfenService(
			@Autowired(required = false) GrandBossManager grandBossManager,
			@Autowired(required = false) NpcTemplateTable templates,
			@Autowired(required = false) GameWorld world,
			@Autowired(required = false) ObjectIdFactory objectIds) {
		this.grandBossManager = grandBossManager;
		this.templates = templates;
		this.world = world;
		this.objectIds = objectIds;
	}

	@PostConstruct
	public void init() {
		if (grandBossManager != null) {
			this.coreStatus = grandBossManager.getStatus(GrandBossManager.CORE);
			this.orfenStatus = grandBossManager.getStatus(GrandBossManager.ORFEN);
		}
	}

	// ---- Core ----

	public synchronized void spawnCore() {
		if (templates == null || world == null || objectIds == null) return;
		NpcTemplate tpl = templates.get(CORE).orElseGet(() -> NpcTemplate.fallback(CORE, "Core", "L2GrandBoss"));
		activeCore = new NpcInstance(objectIds.nextId(), tpl, CORE_X, CORE_Y, CORE_Z, 0);
		world.addNpc(activeCore);

		// Spawna guardas Susceptors (retail: 4)
		int guards = Config.CORE_NUMBER_OF_GUARDS;
		NpcTemplate guardTpl = templates.get(SUSCEPTOR).orElseGet(() -> NpcTemplate.fallback(SUSCEPTOR, "Susceptor", "L2Monster"));
		for (int i = 0; i < guards; i++) {
			double angle = (2 * Math.PI * i) / guards;
			int gx = (int) (CORE_X + Math.cos(angle) * 200);
			int gy = (int) (CORE_Y + Math.sin(angle) * 200);
			NpcInstance g = new NpcInstance(objectIds.nextId(), guardTpl, gx, gy, CORE_Z, 0);
			g.masterObjectId(activeCore.objectId());
			activeCore.minions().add(g);
			world.addNpc(g);
		}

		coreStatus = BossStatus.ALIVE;
		if (grandBossManager != null) grandBossManager.setStatus(GrandBossManager.CORE, BossStatus.ALIVE);
		if (world != null) {
			world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Cruma Tower",
					"The Core has activated on the 3rd floor of Cruma Tower!"), p -> true);
		}
	}

	public synchronized void onCoreKilled(PlayerCharacter killer) {
		coreStatus = BossStatus.INTERVAL;
		if (grandBossManager != null) grandBossManager.onBossKilled(GrandBossManager.CORE);
		if (world != null) {
			world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Cruma Tower",
					"Core has been shut down!"), p -> true);
		}
	}

	// ---- Orfen ----

	public synchronized void spawnOrfen() {
		if (templates == null || world == null || objectIds == null) return;
		NpcTemplate tpl = templates.get(ORFEN).orElseGet(() -> NpcTemplate.fallback(ORFEN, "Orfen", "L2GrandBoss"));
		activeOrfen = new NpcInstance(objectIds.nextId(), tpl, ORFEN_X, ORFEN_Y, ORFEN_Z, 0);
		world.addNpc(activeOrfen);
		orfenInNest = false;

		// Spawna lacaios Riba Iren
		NpcTemplate minionTpl = templates.get(RIBA_IREN).orElseGet(() -> NpcTemplate.fallback(RIBA_IREN, "Riba Iren", "L2Monster"));
		for (int i = 0; i < 4; i++) {
			double angle = (2 * Math.PI * i) / 4;
			int mx = (int) (ORFEN_X + Math.cos(angle) * 250);
			int my = (int) (ORFEN_Y + Math.sin(angle) * 250);
			NpcInstance m = new NpcInstance(objectIds.nextId(), minionTpl, mx, my, ORFEN_Z, 0);
			m.masterObjectId(activeOrfen.objectId());
			activeOrfen.minions().add(m);
			world.addNpc(m);
		}

		orfenStatus = BossStatus.ALIVE;
		if (grandBossManager != null) grandBossManager.setStatus(GrandBossManager.ORFEN, BossStatus.ALIVE);
		if (world != null) {
			world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Sea of Spores",
					"Orfen has descended into the Sea of Spores!"), p -> true);
		}
	}

	public synchronized void retreatOrfenToNest() {
		if (activeOrfen != null && !orfenInNest && world != null) {
			orfenInNest = true;
			int fromX = activeOrfen.x();
			int fromY = activeOrfen.y();
			activeOrfen.moveTo(ORFEN_NEST_X, ORFEN_NEST_Y, ORFEN_NEST_Z, 0);
			world.updateNpcPosition(activeOrfen, fromX, fromY);
			world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Orfen",
					"Orfen has retreated to her inner nest to recover!"), p -> true);
			log.info("Orfen recuou para o ninho interno para recuperar forcas.");
		}
	}

	public synchronized void onOrfenKilled(PlayerCharacter killer) {
		orfenStatus = BossStatus.INTERVAL;
		if (grandBossManager != null) grandBossManager.onBossKilled(GrandBossManager.ORFEN);
		if (world != null) {
			world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Sea of Spores",
					"Orfen has been slain!"), p -> true);
		}
	}

	public BossStatus coreStatus() { return coreStatus; }
	public BossStatus orfenStatus() { return orfenStatus; }
	public Optional<NpcInstance> activeCore() { return Optional.ofNullable(activeCore); }
	public Optional<NpcInstance> activeOrfen() { return Optional.ofNullable(activeOrfen); }
	public boolean isOrfenInNest() { return orfenInNest; }
}
