package com.lopez.l2j.game.npc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Carrega a tabela {@code npc} (e {@code custom_npc} se disponivel) sob demanda.
 */
@Repository
class JdbcNpcTemplateTable implements NpcTemplateTable {

	private static final Logger log = LoggerFactory.getLogger(JdbcNpcTemplateTable.class);

	private final JdbcClient jdbc;
	private volatile Map<Integer, NpcTemplate> cache;

	JdbcNpcTemplateTable(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public Optional<NpcTemplate> get(int npcId) {
		return Optional.ofNullable(cache().get(npcId));
	}

	@Override
	public int size() {
		return cache().size();
	}

	@Override
	public java.util.Collection<NpcTemplate> all() {
		return cache().values();
	}

	private Map<Integer, NpcTemplate> cache() {
		Map<Integer, NpcTemplate> c = cache;
		if (c == null) {
			synchronized (this) {
				c = cache;
				if (c == null) {
					cache = c = load();
				}
			}
		}
		return c;
	}

	private Map<Integer, NpcTemplate> load() {
		long start = System.nanoTime();
		Map<Integer, NpcTemplate> map = new HashMap<>();
		int std = loadTable(map, "npc");
		int custom = loadTable(map, "custom_npc");
		log.info("NpcTable: {} NPCs padrão, {} custom carregados em {} ms", std, custom,
				(System.nanoTime() - start) / 1_000_000);
		return Map.copyOf(map);
	}

	private int loadTable(Map<Integer, NpcTemplate> map, String table) {
		try {
			List<NpcTemplate> list = jdbc.sql("SELECT * FROM " + table)
					.query((rs, i) -> mapRow(rs))
					.list();
			list.forEach(t -> map.put(t.id(), t));
			return list.size();
		} catch (RuntimeException e) {
			log.warn("Tabela {} indisponível: {}", table, e.getMessage());
			return 0;
		}
	}

	private static NpcTemplate mapRow(ResultSet rs) throws SQLException {
		return new NpcTemplate(
				rs.getInt("id"),
				rs.getInt("idTemplate"),
				rs.getString("name"),
				rs.getInt("serverSideName") == 1,
				rs.getString("title"),
				rs.getInt("serverSideTitle") == 1,
				rs.getDouble("collision_radius"),
				rs.getDouble("collision_height"),
				rs.getInt("level"),
				rs.getString("sex"),
				rs.getString("type"),
				rs.getInt("attackrange"),
				rs.getInt("hp"),
				rs.getInt("mp"),
				rs.getInt("patk"),
				rs.getInt("pdef"),
				rs.getInt("matk"),
				rs.getInt("mdef"),
				rs.getInt("atkspd"),
				rs.getInt("matkspd"),
				rs.getInt("rhand"),
				rs.getInt("lhand"),
				rs.getInt("armor"),
				rs.getInt("walkspd"),
				rs.getInt("runspd"),
				rs.getInt("aggro"),
				rs.getInt("isUndead") == 1,
				rs.getLong("exp"),
				rs.getInt("sp"),
				getStringSafe(rs, "faction_id"),
				getIntSafe(rs, "faction_range", 0));
	}

	private static String getStringSafe(ResultSet rs, String column) {
		try {
			String val = rs.getString(column);
			if (val == null || val.isBlank() || "null".equalsIgnoreCase(val.trim()) || "none".equalsIgnoreCase(val.trim())) {
				return null;
			}
			return val.trim();
		} catch (SQLException e) {
			return null;
		}
	}

	private static int getIntSafe(ResultSet rs, String column, int def) {
		try {
			return rs.getInt(column);
		} catch (SQLException e) {
			return def;
		}
	}
}
