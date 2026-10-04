package com.lopez.l2j.game.drop;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Carrega a tabela {@code droplist} (e {@code custom_droplist} se disponivel) agrupada por mobId.
 */
@Repository
public class JdbcDropTable implements DropTable {

	private static final Logger log = LoggerFactory.getLogger(JdbcDropTable.class);

	private final JdbcClient jdbc;
	private volatile Map<Integer, List<DropData>> cache;

	public JdbcDropTable(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<DropData> getDrops(int mobId) {
		return cache().getOrDefault(mobId, Collections.emptyList());
	}

	@Override
	public int size() {
		return cache().size();
	}

	private Map<Integer, List<DropData>> cache() {
		Map<Integer, List<DropData>> c = cache;
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

	private Map<Integer, List<DropData>> load() {
		long start = System.nanoTime();
		Map<Integer, List<DropData>> map = new HashMap<>();
		int std = loadTable(map, "droplist");
		int custom = loadTable(map, "custom_droplist");
		log.info("DropTable: {} drops padrao, {} custom carregados para {} mobs em {} ms",
				std, custom, map.size(), (System.nanoTime() - start) / 1_000_000);

		// Torna as listas imutaveis
		Map<Integer, List<DropData>> immutable = new HashMap<>(map.size());
		map.forEach((k, v) -> immutable.put(k, List.copyOf(v)));
		return Map.copyOf(immutable);
	}

	private int loadTable(Map<Integer, List<DropData>> map, String table) {
		try {
			List<DropData> list = jdbc.sql("SELECT mobId, itemId, min, max, category, chance FROM " + table)
					.query((rs, i) -> mapRow(rs))
					.list();
			for (DropData data : list) {
				map.computeIfAbsent(data.mobId(), k -> new ArrayList<>()).add(data);
			}
			return list.size();
		} catch (RuntimeException e) {
			log.warn("Tabela {} indisponivel: {}", table, e.getMessage());
			return 0;
		}
	}

	private static DropData mapRow(ResultSet rs) throws SQLException {
		return new DropData(
				rs.getInt("mobId"),
				rs.getInt("itemId"),
				rs.getInt("min"),
				rs.getInt("max"),
				rs.getInt("category"),
				rs.getInt("chance"));
	}
}
