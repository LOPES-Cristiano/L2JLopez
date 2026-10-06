package com.lopez.l2j.game.npc.daynight;

import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.npc.NpcTemplateTable;
import com.lopez.l2j.game.time.GameTimeController;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Servico de gerenciamento de spawns dinamicos de Dia e Noite.
 * Porta de DayNightSpawnManager do legado L2JDream.
 * Gerencia criaturas que surgem exclusivamente durante o dia (periodOfDay=1)
 * ou noite (periodOfDay=2), alem de chefes noturnos como Raid Boss Hellmann (ID 25328)
 * e o efeito passivo Shadow Sense (Skill 294) para Dark Elves.
 */
@Service
public class DayNightSpawnService implements GameTimeController.DayNightListener {

	private static final Logger log = LoggerFactory.getLogger(DayNightSpawnService.class);

	public static final int RAID_HELLMANN_ID = 25328;
	public static final int SHADOW_SENSE_SKILL_ID = 294;

	private final JdbcClient jdbc;
	private final NpcTemplateTable templates;
	private final GameWorld world;
	private final ObjectIdFactory objectIds;
	private final GameTimeController timeController;

	private final List<DayNightSpawnData> daySpawns = new CopyOnWriteArrayList<>();
	private final List<DayNightSpawnData> nightSpawns = new CopyOnWriteArrayList<>();

	private final List<NpcInstance> activeDayInstances = new CopyOnWriteArrayList<>();
	private final List<NpcInstance> activeNightInstances = new CopyOnWriteArrayList<>();
	private volatile NpcInstance activeNightBoss = null;

	private volatile boolean currentNight = false;

	@Autowired
	public DayNightSpawnService(@Autowired(required = false) JdbcClient jdbc,
								NpcTemplateTable templates,
								GameWorld world,
								ObjectIdFactory objectIds,
								@Autowired(required = false) GameTimeController timeController) {
		this.jdbc = jdbc;
		this.templates = templates;
		this.world = world;
		this.objectIds = objectIds;
		this.timeController = timeController;
	}

	@PostConstruct
	public void init() {
		loadFromDatabase();

		if (timeController != null) {
			timeController.addListener(this);
			currentNight = timeController.isNight();
		}

		notifyChangeMode(currentNight);
		log.info("DayNightSpawnService inicializado: {} spawns diurnos, {} spawns noturnos (estado atual: {})",
				daySpawns.size(), nightSpawns.size(), currentNight ? "Noite" : "Dia");
	}

	public void addDayCreature(DayNightSpawnData spawn) {
		if (spawn != null) {
			daySpawns.add(spawn);
			if (!currentNight) {
				spawnSingle(spawn, activeDayInstances);
			}
		}
	}

	public void addNightCreature(DayNightSpawnData spawn) {
		if (spawn != null) {
			nightSpawns.add(spawn);
			if (currentNight) {
				spawnSingle(spawn, activeNightInstances);
			}
		}
	}

	public void cleanUp() {
		despawnDayCreatures();
		despawnNightCreatures();
		despawnNightBoss();
		daySpawns.clear();
		nightSpawns.clear();
	}

	@Override
	public void onDayNightChange(boolean isNight) {
		notifyChangeMode(isNight);
	}

	public synchronized void notifyChangeMode(boolean isNight) {
		this.currentNight = isNight;
		if (isNight) {
			despawnDayCreatures();
			spawnNightCreatures();
			spawnNightBoss();
			notifyShadowSense(true);
		} else {
			despawnNightCreatures();
			despawnNightBoss();
			spawnDayCreatures();
			notifyShadowSense(false);
		}
	}

	public synchronized void spawnDayCreatures() {
		if (!activeDayInstances.isEmpty()) {
			return;
		}
		for (DayNightSpawnData spawn : daySpawns) {
			spawnSingle(spawn, activeDayInstances);
		}
	}

	public synchronized void despawnDayCreatures() {
		for (NpcInstance npc : activeDayInstances) {
			if (world != null) {
				world.removeNpc(npc);
			}
		}
		activeDayInstances.clear();
	}

	public synchronized void spawnNightCreatures() {
		if (!activeNightInstances.isEmpty()) {
			return;
		}
		for (DayNightSpawnData spawn : nightSpawns) {
			spawnSingle(spawn, activeNightInstances);
		}
	}

	public synchronized void despawnNightCreatures() {
		for (NpcInstance npc : activeNightInstances) {
			if (world != null) {
				world.removeNpc(npc);
			}
		}
		activeNightInstances.clear();
	}

	public synchronized void spawnNightBoss() {
		if (activeNightBoss != null) {
			return;
		}
		// Se existir um template para Hellmann ou spawn configurado
		Optional<NpcTemplate> tplOpt = templates.get(RAID_HELLMANN_ID);
		if (tplOpt.isPresent() && world != null && objectIds != null) {
			int objectId = objectIds.nextId();
			// Coordenadas padrao de Hellmann (Cursed Village / Forest of the Dead)
			NpcInstance boss = new NpcInstance(objectId, tplOpt.get(), 58941, -50030, -3230, 0);
			world.addNpc(boss);
			activeNightBoss = boss;
			log.info("DayNightSpawnService: Raid Boss Hellmann (ID {}) surgiu nas sombras da noite.", RAID_HELLMANN_ID);
		}
	}

	public synchronized void despawnNightBoss() {
		if (activeNightBoss != null) {
			if (world != null) {
				world.removeNpc(activeNightBoss);
			}
			log.info("DayNightSpawnService: Raid Boss Hellmann (ID {}) desapareceu ao raiar do sol.", RAID_HELLMANN_ID);
			activeNightBoss = null;
		}
	}

	private void spawnSingle(DayNightSpawnData data, List<NpcInstance> instanceList) {
		if (world == null || objectIds == null) {
			return;
		}
		NpcTemplate template = templates.get(data.npcTemplateId())
				.orElseGet(() -> NpcTemplate.fallback(data.npcTemplateId(), "NPC " + data.npcTemplateId(), "L2Monster"));

		int count = Math.max(1, data.count());
		for (int i = 0; i < count; i++) {
			int objectId = objectIds.nextId();
			NpcInstance npc = new NpcInstance(objectId, template, data.x(), data.y(), data.z(), data.heading());
			world.addNpc(npc);
			instanceList.add(npc);
		}
	}

	private void notifyShadowSense(boolean isNight) {
		if (world == null) {
			return;
		}
		int msgId = isNight ? SystemMessage.S1_NIGHT_EFFECT_APPLIES : SystemMessage.S1_NIGHT_EFFECT_DISAPPEARS;
		var packet = SystemMessage.of(msgId, new SystemMessage.SkillName(SHADOW_SENSE_SKILL_ID, 1));
		for (GameWorld.OnlinePlayer player : world.players()) {
			var pc = player.character();
			// Dark Elf (race ordinal 2)
			if (pc != null && pc.race() == 2) {
				player.send(packet);
			}
		}
	}

	private void loadFromDatabase() {
		if (jdbc == null) {
			return;
		}
		loadTable("spawnlist");
		loadTable("custom_spawnlist");
		loadTable("vanhalter_spawnlist");
	}

	private void loadTable(String tableName) {
		try {
			List<DayNightRow> rows = jdbc.sql("SELECT npc_templateid, locx, locy, locz, heading, count, periodOfDay FROM "
							+ tableName + " WHERE periodOfDay IN (1, 2)")
					.query((rs, i) -> new DayNightRow(
							rs.getInt("npc_templateid"),
							rs.getInt("locx"),
							rs.getInt("locy"),
							rs.getInt("locz"),
							rs.getInt("heading"),
							rs.getInt("count"),
							rs.getInt("periodOfDay")))
					.list();

			for (DayNightRow r : rows) {
				DayNightSpawnData data = new DayNightSpawnData(r.npcId(), r.x(), r.y(), r.z(), r.heading(), r.count());
				if (r.periodOfDay() == 1) {
					daySpawns.add(data);
				} else if (r.periodOfDay() == 2) {
					nightSpawns.add(data);
				}
			}
		} catch (Exception e) {
			log.debug("Tabela {} sem suporte a periodOfDay ou vazia: {}", tableName, e.getMessage());
		}
	}

	public boolean isNight() {
		return currentNight;
	}

	public List<DayNightSpawnData> daySpawns() {
		return Collections.unmodifiableList(daySpawns);
	}

	public List<DayNightSpawnData> nightSpawns() {
		return Collections.unmodifiableList(nightSpawns);
	}

	public List<NpcInstance> activeDayInstances() {
		return Collections.unmodifiableList(activeDayInstances);
	}

	public List<NpcInstance> activeNightInstances() {
		return Collections.unmodifiableList(activeNightInstances);
	}

	public NpcInstance activeNightBoss() {
		return activeNightBoss;
	}

	private record DayNightRow(int npcId, int x, int y, int z, int heading, int count, int periodOfDay) {
	}
}
