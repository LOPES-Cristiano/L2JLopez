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
		long start = System.nanoTime();
		int std = loadSpawns("spawnlist");
		int custom = loadSpawns("custom_spawnlist");
		log.info("SpawnService: {} spawns padrao, {} customizados carregados no mundo (total npcs: {}) em {} ms",
				std, custom, world.totalNpcs(), (System.nanoTime() - start) / 1_000_000);
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
				var opt = templates.get(rec.npcTemplateId());
				if (opt.isPresent()) {
					NpcTemplate template = opt.get();
					int count = Math.max(1, rec.count());
					for (int c = 0; c < count; c++) {
						int objectId = objectIds.nextId();
						NpcInstance npc = new NpcInstance(objectId, template, rec.x(), rec.y(), rec.z(), rec.heading());
						world.addNpc(npc);
						spawned++;
					}
				}
			}
			return spawned;
		} catch (RuntimeException e) {
			log.debug("Tabela {} nao carregada: {}", tableName, e.getMessage());
			return 0;
		}
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
