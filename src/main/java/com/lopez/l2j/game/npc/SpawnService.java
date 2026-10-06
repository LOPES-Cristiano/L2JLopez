package com.lopez.l2j.game.npc;

import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.world.GameWorld;
import jakarta.annotation.PostConstruct;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Carrega a lista de spawns do mundo (tabelas {@code spawnlist} e {@code custom_spawnlist}) e gerencia
 * instâncias de NPCs ativos no {@link GameWorld}.
 */
@Service
public class SpawnService {

	private static final Logger log = LoggerFactory.getLogger(SpawnService.class);

	private final JdbcClient jdbc;
	private final NpcTemplateTable templates;
	private final GameWorld world;
	private final ObjectIdFactory objectIds;
	private final com.lopez.l2j.game.npc.minion.MinionTable minionTable;

	public SpawnService(JdbcClient jdbc, NpcTemplateTable templates, GameWorld world, ObjectIdFactory objectIds) {
		this(jdbc, templates, world, objectIds, null);
	}

	@org.springframework.beans.factory.annotation.Autowired
	public SpawnService(JdbcClient jdbc, NpcTemplateTable templates, GameWorld world, ObjectIdFactory objectIds,
			@org.springframework.beans.factory.annotation.Autowired(required = false) com.lopez.l2j.game.npc.minion.MinionTable minionTable) {
		this.jdbc = jdbc;
		this.templates = templates;
		this.world = world;
		this.objectIds = objectIds;
		this.minionTable = minionTable;
	}

	@PostConstruct
	public void init() {
		if (jdbc == null) {
			return;
		}
		long start = System.nanoTime();
		int std = loadSpawns("spawnlist");
		int custom = loadSpawns("custom_spawnlist");
		int raid = loadRaidBossSpawns();
		int vanHalter = loadSpawns("vanhalter_spawnlist");
		int tomb = loadSpawns("lastimperialtomb_spawnlist");
		int fort = loadFortSpawns();
		int random = loadRandomSpawns();
		int minions = spawnMinionsForWorldMasters();
		log.info("SpawnService: {} padrao, {} custom, {} raid bosses, {} vanhalter, {} tomb, {} fort, {} random, {} minions carregados (total npcs ativos no mundo: {}) em {} ms",
				std, custom, raid, vanHalter, tomb, fort, random, minions, world.totalNpcs(), (System.nanoTime() - start) / 1_000_000);
	}

	public Optional<NpcInstance> spawn(int npcId, int x, int y, int z, int heading) {
		return spawn(npcId, x, y, z, heading, false);
	}

	public Optional<NpcInstance> spawn(int npcId, int x, int y, int z, int heading, boolean storeInDb) {
		return templates.get(npcId).map(template -> {
			int objectId = objectIds.nextId();
			NpcInstance npc = new NpcInstance(objectId, template, x, y, z, heading);
			world.addNpc(npc);
			if (minionTable != null && minionTable.hasMinions(npcId)) {
				spawnMinionsForMaster(npc);
			}
			if (storeInDb && jdbc != null) {
				try {
					jdbc.sql("INSERT INTO custom_spawnlist (location, count, npc_templateid, locx, locy, locz, heading, respawn_delay) VALUES ('GM_Spawn', 1, ?, ?, ?, ?, ?, 60)")
							.params(npcId, x, y, z, heading)
							.update();
				} catch (Exception e) {
					log.warn("Nao foi possivel persistir spawn de NPC {} no banco: {}", npcId, e.getMessage());
				}
			}
			return npc;
		});
	}

	public boolean deleteSpawn(NpcInstance npc, boolean deleteFromDb) {
		if (npc == null) {
			return false;
		}
		if (npc.hasMinions()) {
			for (NpcInstance minion : npc.minions()) {
				world.removeNpc(minion);
			}
			npc.minions().clear();
		}
		world.removeNpc(npc);
		if (deleteFromDb && jdbc != null) {
			try {
				jdbc.sql("DELETE FROM custom_spawnlist WHERE npc_templateid = ? AND locx = ? AND locy = ?")
						.params(npc.npcId(), npc.x(), npc.y())
						.update();
			} catch (Exception e) {
				log.warn("Nao foi possivel remover spawn de NPC {} do banco: {}", npc.npcId(), e.getMessage());
			}
		}
		return true;
	}

	private int loadSpawns(String tableName) {
		try {
			String sql = "SELECT npc_templateid, locx, locy, locz, heading, count FROM " + tableName;
			if ("spawnlist".equals(tableName) || "custom_spawnlist".equals(tableName) || "vanhalter_spawnlist".equals(tableName)) {
				sql += " WHERE periodOfDay = 0 OR periodOfDay IS NULL";
			}
			List<SpawnRecord> spawns = jdbc.sql(sql)
					.query((rs, i) -> mapSpawn(rs))
					.list();

			int spawned = 0;
			for (SpawnRecord rec : spawns) {
				NpcTemplate template = templates.get(rec.npcTemplateId())
						.orElseGet(() -> fallbackTemplate(rec.npcTemplateId(), "NPC " + rec.npcTemplateId(), "L2Npc"));
				int count = Math.max(1, rec.count());
				for (int c = 0; c < count; c++) {
					int objectId = objectIds.nextId();
					int sx = rec.x();
					int sy = rec.y();
					if (c > 0) {
						double angle = (2 * Math.PI * c) / count;
						int dist = 40 + (c * 25);
						sx += (int) (Math.cos(angle) * dist);
						sy += (int) (Math.sin(angle) * dist);
					}
					NpcInstance npc = new NpcInstance(objectId, template, sx, sy, rec.z(), rec.heading());
					world.addNpc(npc);
					spawned++;
				}
			}
			return spawned;
		} catch (Exception e) {
			log.warn("Tabela {} nao carregada: {}", tableName, e.getMessage());
			return 0;
		}
	}

	private int loadRaidBossSpawns() {
		try {
			List<SpawnRecord> spawns = jdbc.sql("SELECT boss_id, loc_x, loc_y, loc_z, heading, amount FROM raidboss_spawnlist")
					.query((rs, i) -> new SpawnRecord(
							rs.getInt("boss_id"),
							rs.getInt("loc_x"),
							rs.getInt("loc_y"),
							rs.getInt("loc_z"),
							rs.getInt("heading"),
							rs.getInt("amount")))
					.list();

			int spawned = 0;
			for (SpawnRecord rec : spawns) {
				NpcTemplate template = templates.get(rec.npcTemplateId())
						.orElseGet(() -> fallbackTemplate(rec.npcTemplateId(), "Raid Boss " + rec.npcTemplateId(), "L2RaidBoss"));
				int count = Math.max(1, rec.count());
				for (int c = 0; c < count; c++) {
					int objectId = objectIds.nextId();
					int sx = rec.x();
					int sy = rec.y();
					if (c > 0) {
						double angle = (2 * Math.PI * c) / count;
						int dist = 50 + (c * 30);
						sx += (int) (Math.cos(angle) * dist);
						sy += (int) (Math.sin(angle) * dist);
					}
					NpcInstance npc = new NpcInstance(objectId, template, sx, sy, rec.z(), rec.heading());
					world.addNpc(npc);
					spawned++;
				}
			}
			return spawned;
		} catch (Exception e) {
			log.warn("Tabela raidboss_spawnlist nao carregada: {}", e.getMessage());
			return 0;
		}
	}

	private int loadFortSpawns() {
		try {
			List<SpawnRecord> spawns = jdbc.sql("SELECT npcId, x, y, z, heading FROM fort_spawnlist")
					.query((rs, i) -> new SpawnRecord(
							rs.getInt("npcId"),
							rs.getInt("x"),
							rs.getInt("y"),
							rs.getInt("z"),
							rs.getInt("heading"),
							1))
					.list();

			int spawned = 0;
			for (SpawnRecord rec : spawns) {
				NpcTemplate template = templates.get(rec.npcTemplateId())
						.orElseGet(() -> fallbackTemplate(rec.npcTemplateId(), "Fort NPC " + rec.npcTemplateId(), "L2FortSiegeGuard"));
				int objectId = objectIds.nextId();
				NpcInstance npc = new NpcInstance(objectId, template, rec.x(), rec.y(), rec.z(), rec.heading());
				world.addNpc(npc);
				spawned++;
			}
			return spawned;
		} catch (Exception e) {
			log.warn("Tabela fort_spawnlist nao carregada: {}", e.getMessage());
			return 0;
		}
	}

	private int loadRandomSpawns() {
		try {
			record RandomLoc(int groupId, int x, int y, int z, int heading) {}
			List<RandomLoc> locs = jdbc.sql("SELECT groupId, x, y, z, heading FROM random_spawn_loc")
					.query((rs, i) -> new RandomLoc(
							rs.getInt("groupId"),
							rs.getInt("x"),
							rs.getInt("y"),
							rs.getInt("z"),
							rs.getInt("heading")))
					.list();
			if (locs.isEmpty()) {
				return 0;
			}
			java.util.Map<Integer, RandomLoc> locByGroup = new java.util.HashMap<>();
			for (RandomLoc l : locs) {
				locByGroup.putIfAbsent(l.groupId(), l);
			}

			record RandomGroup(int groupId, int npcId, int count) {}
			List<RandomGroup> groups = jdbc.sql("SELECT groupId, npcId, count FROM random_spawn")
					.query((rs, i) -> new RandomGroup(
							rs.getInt("groupId"),
							rs.getInt("npcId"),
							rs.getInt("count")))
					.list();

			int spawned = 0;
			for (RandomGroup g : groups) {
				RandomLoc loc = locByGroup.get(g.groupId());
				if (loc == null) {
					continue;
				}
				NpcTemplate template = templates.get(g.npcId())
						.orElseGet(() -> fallbackTemplate(g.npcId(), "Random Spawn " + g.npcId(), "L2Monster"));
				int count = Math.max(1, g.count());
				for (int c = 0; c < count; c++) {
					int objectId = objectIds.nextId();
					int sx = loc.x();
					int sy = loc.y();
					if (c > 0) {
						double angle = (2 * Math.PI * c) / count;
						int dist = 30 + (c * 20);
						sx += (int) (Math.cos(angle) * dist);
						sy += (int) (Math.sin(angle) * dist);
					}
					NpcInstance npc = new NpcInstance(objectId, template, sx, sy, loc.z(), loc.heading());
					world.addNpc(npc);
					spawned++;
				}
			}
			return spawned;
		} catch (Exception e) {
			log.warn("Tabelas random_spawn/random_spawn_loc nao carregadas: {}", e.getMessage());
			return 0;
		}
	}

	public int spawnMinionsForMaster(NpcInstance master) {
		if (minionTable == null || master == null || !minionTable.hasMinions(master.npcId())) {
			return 0;
		}
		int spawned = 0;
		List<com.lopez.l2j.game.npc.minion.MinionEntry> entries = minionTable.getMinionsForBoss(master.npcId());
		for (var entry : entries) {
			int count = entry.amountMin() == entry.amountMax() ? entry.amountMin() :
					java.util.concurrent.ThreadLocalRandom.current().nextInt(entry.amountMin(), entry.amountMax() + 1);
			for (int i = 0; i < count; i++) {
				NpcTemplate minionTemplate = templates.get(entry.minionId())
						.orElseGet(() -> fallbackTemplate(entry.minionId(), "Minion " + entry.minionId(), "L2Minion"));
				double angle = java.util.concurrent.ThreadLocalRandom.current().nextDouble(0, 2 * Math.PI);
				int dist = java.util.concurrent.ThreadLocalRandom.current().nextInt(40, 100);
				int mx = master.x() + (int) (Math.cos(angle) * dist);
				int my = master.y() + (int) (Math.sin(angle) * dist);
				int mz = master.z();
				int mHeading = master.heading();

				int minionObjId = objectIds.nextId();
				NpcInstance minion = new NpcInstance(minionObjId, minionTemplate, mx, my, mz, mHeading);
				minion.masterObjectId(master.objectId());
				master.minions().add(minion);
				world.addNpc(minion);
				spawned++;
			}
		}
		return spawned;
	}

	public int spawnMinionsForWorldMasters() {
		if (minionTable == null) {
			return 0;
		}
		int count = 0;
		for (NpcInstance npc : new java.util.ArrayList<>(world.npcs())) {
			if (!npc.isMinion() && minionTable.hasMinions(npc.npcId()) && !npc.hasMinions()) {
				count += spawnMinionsForMaster(npc);
			}
		}
		return count;
	}

	private static NpcTemplate fallbackTemplate(int id, String name, String type) {
		return new NpcTemplate(
				id, id, name, true, "", false,
				9.0, 24.0, 70, "male", type, 40,
				2000, 1000, 150, 150, 150, 150, 250, 333,
				0, 0, 0, 50, 120, 0, false, 0, 0);
	}

	private static SpawnRecord mapSpawn(ResultSet rs) throws SQLException {
		return new SpawnRecord(
				rs.getInt("npc_templateid"),
				rs.getInt("locx"),
				rs.getInt("locy"),
				rs.getInt("locz"),
				rs.getInt("heading"),
				rs.getInt("count"));
	}

	private record SpawnRecord(int npcTemplateId, int x, int y, int z, int heading, int count) {
	}
}
