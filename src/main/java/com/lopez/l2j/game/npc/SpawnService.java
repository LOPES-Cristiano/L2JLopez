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

	public SpawnService(JdbcClient jdbc, NpcTemplateTable templates, GameWorld world, ObjectIdFactory objectIds) {
		this.jdbc = jdbc;
		this.templates = templates;
		this.world = world;
		this.objectIds = objectIds;
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
		log.info("SpawnService: {} padrao, {} custom, {} raid bosses, {} vanhalter, {} tomb carregados (total npcs ativos no mundo: {}) em {} ms",
				std, custom, raid, vanHalter, tomb, world.totalNpcs(), (System.nanoTime() - start) / 1_000_000);
	}

	public Optional<NpcInstance> spawn(int npcId, int x, int y, int z, int heading) {
		return templates.get(npcId).map(template -> {
			int objectId = objectIds.nextId();
			NpcInstance npc = new NpcInstance(objectId, template, x, y, z, heading);
			world.addNpc(npc);
			return npc;
		});
	}

	private int loadSpawns(String tableName) {
		try {
			List<SpawnRecord> spawns = jdbc.sql("SELECT npc_templateid, locx, locy, locz, heading, count FROM " + tableName)
					.query((rs, i) -> mapSpawn(rs))
					.list();

			int spawned = 0;
			for (SpawnRecord rec : spawns) {
				NpcTemplate template = templates.get(rec.npcTemplateId())
						.orElseGet(() -> fallbackTemplate(rec.npcTemplateId(), "NPC " + rec.npcTemplateId(), "L2Npc"));
				int count = Math.max(1, rec.count());
				for (int c = 0; c < count; c++) {
					int objectId = objectIds.nextId();
					NpcInstance npc = new NpcInstance(objectId, template, rec.x(), rec.y(), rec.z(), rec.heading());
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
					NpcInstance npc = new NpcInstance(objectId, template, rec.x(), rec.y(), rec.z(), rec.heading());
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
