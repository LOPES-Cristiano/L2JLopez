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
 * Servico e gerenciador do Ninho da Rainha Formiga (QueenAntManager do L2JDream / L2JLucera2).
 * Controla limite de nivel maximo seguro (QueenAntMaxSafeLevel = 48), invocacao da Larva (29004),
 * Formigas Enfermeiras (29002) que curam a Rainha e a Larva, e Guardas Reais (29003).
 */
@Service
public class QueenAntService {

	private static final Logger log = LoggerFactory.getLogger(QueenAntService.class);

	public static final int QUEEN_ANT = 29001;
	public static final int QUEEN_ANT_LARVA = 29004;
	public static final int QUEEN_ANT_NURSE = 29002;
	public static final int QUEEN_ANT_GUARD = 29003;
	public static final int RING_OF_QUEEN_ANT = 6660;

	public static final int NEST_X = -21610;
	public static final int NEST_Y = 181594;
	public static final int NEST_Z = -5734;

	private BossStatus status = BossStatus.NOTSPAWN;
	private NpcInstance activeQueenAnt;
	private NpcInstance activeLarva;
	private final List<NpcInstance> nurses = new CopyOnWriteArrayList<>();
	private final List<NpcInstance> guards = new CopyOnWriteArrayList<>();

	private final GrandBossManager grandBossManager;
	private final NpcTemplateTable templates;
	private final GameWorld world;
	private final ObjectIdFactory objectIds;

	@Autowired
	public QueenAntService(
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
			this.status = grandBossManager.getStatus(GrandBossManager.QUEEN_ANT);
		}
	}

	public BossStatus getStatus() {
		return status;
	}

	public boolean isLevelExceeded(PlayerCharacter player) {
		if (player == null) return false;
		return player.level() > Config.QUEEN_ANT_MAX_SAFE_LEVEL;
	}

	public synchronized void spawnQueenAnt() {
		if (templates == null || world == null || objectIds == null) return;

		NpcTemplate qaTpl = templates.get(QUEEN_ANT)
				.orElseGet(() -> NpcTemplate.fallback(QUEEN_ANT, "Queen Ant", "L2GrandBoss"));
		activeQueenAnt = new NpcInstance(objectIds.nextId(), qaTpl, NEST_X, NEST_Y, NEST_Z, 0);
		world.addNpc(activeQueenAnt);

		// Spawna Larva
		NpcTemplate larvaTpl = templates.get(QUEEN_ANT_LARVA)
				.orElseGet(() -> NpcTemplate.fallback(QUEEN_ANT_LARVA, "Larva", "L2Monster"));
		activeLarva = new NpcInstance(objectIds.nextId(), larvaTpl, NEST_X - 100, NEST_Y + 50, NEST_Z, 0);
		world.addNpc(activeLarva);

		// Spawna Enfermeiras (retail: 6)
		nurses.clear();
		int numNurses = Config.QUEEN_ANT_NUMBER_OF_NURSES;
		NpcTemplate nurseTpl = templates.get(QUEEN_ANT_NURSE)
				.orElseGet(() -> NpcTemplate.fallback(QUEEN_ANT_NURSE, "Nurse Ant", "L2Monster"));
		for (int i = 0; i < numNurses; i++) {
			double angle = (2 * Math.PI * i) / numNurses;
			int nx = (int) (NEST_X + Math.cos(angle) * 200);
			int ny = (int) (NEST_Y + Math.sin(angle) * 200);
			NpcInstance nurse = new NpcInstance(objectIds.nextId(), nurseTpl, nx, ny, NEST_Z, 0);
			nurse.masterObjectId(activeQueenAnt.objectId());
			activeQueenAnt.minions().add(nurse);
			nurses.add(nurse);
			world.addNpc(nurse);
		}

		// Spawna Guardas Reais (retail: 8)
		guards.clear();
		int numGuards = Config.QUEEN_ANT_NUMBER_OF_GUARDS;
		NpcTemplate guardTpl = templates.get(QUEEN_ANT_GUARD)
				.orElseGet(() -> NpcTemplate.fallback(QUEEN_ANT_GUARD, "Royal Guard Ant", "L2Monster"));
		for (int i = 0; i < numGuards; i++) {
			double angle = (2 * Math.PI * i) / numGuards;
			int gx = (int) (NEST_X + Math.cos(angle) * 350);
			int gy = (int) (NEST_Y + Math.sin(angle) * 350);
			NpcInstance guard = new NpcInstance(objectIds.nextId(), guardTpl, gx, gy, NEST_Z, 0);
			guard.masterObjectId(activeQueenAnt.objectId());
			activeQueenAnt.minions().add(guard);
			guards.add(guard);
			world.addNpc(guard);
		}

		status = BossStatus.ALIVE;
		if (grandBossManager != null) {
			grandBossManager.setStatus(GrandBossManager.QUEEN_ANT, BossStatus.ALIVE);
		}

		if (world != null) {
			world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Wasteland",
					"Queen Ant has emerged in the Wasteland nest!"), p -> true);
		}
		log.info("Queen Ant, Larva, {} Enfermeiras e {} Guardas Reais gerados no ninho!",
				numNurses, numGuards);
	}

	public synchronized void onQueenAntKilled(PlayerCharacter killer) {
		status = BossStatus.INTERVAL;

		// Despawna minions
		for (NpcInstance n : nurses) {
			if (world != null) world.removeNpc(n);
		}
		nurses.clear();
		for (NpcInstance g : guards) {
			if (world != null) world.removeNpc(g);
		}
		guards.clear();
		if (activeLarva != null && world != null) {
			world.removeNpc(activeLarva);
			activeLarva = null;
		}

		if (grandBossManager != null) {
			grandBossManager.onBossKilled(GrandBossManager.QUEEN_ANT);
		}

		if (world != null) {
			world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Wasteland",
					"Queen Ant has been slain!"), p -> true);
		}
	}

	public Optional<NpcInstance> activeQueenAnt() {
		return Optional.ofNullable(activeQueenAnt);
	}

	public Optional<NpcInstance> activeLarva() {
		return Optional.ofNullable(activeLarva);
	}

	public List<NpcInstance> nurses() {
		return nurses;
	}
}
