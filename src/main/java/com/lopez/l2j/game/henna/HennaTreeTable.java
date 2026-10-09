package com.lopez.l2j.game.henna;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Mapeamento das hennas / dyes compativeis por classe de personagem (henna_trees).
 */
@Component
public class HennaTreeTable {

	private static final Logger log = LoggerFactory.getLogger(HennaTreeTable.class);

	private final Map<Integer, List<Henna>> byClassId = new ConcurrentHashMap<>();
	private final HennaTable hennaTable;

	@Autowired
	public HennaTreeTable(@Autowired(required = false) JdbcClient jdbc, HennaTable hennaTable) {
		this.hennaTable = hennaTable != null ? hennaTable : new HennaTable();
		load(jdbc);
	}

	public List<Henna> getAvailableHennas(int classId) {
		List<Henna> list = byClassId.get(classId);
		if (list != null && !list.isEmpty()) {
			return list;
		}
		// Fallback se nao houver mapeamento no banco (ex.: ambiente de teste)
		return new ArrayList<>(hennaTable.all());
	}

	public boolean isAllowed(int classId, int symbolId) {
		List<Henna> list = byClassId.get(classId);
		if (list == null || list.isEmpty()) {
			return hennaTable.get(symbolId) != null;
		}
		return list.stream().anyMatch(h -> h.symbolId() == symbolId);
	}

	public boolean isHennaAllowed(int classId, int symbolId) {
		return isAllowed(classId, symbolId);
	}

	private void load(JdbcClient jdbc) {
		if (jdbc == null) {
			log.info("JdbcClient nao fornecido para HennaTreeTable (modo standalone/teste)");
			return;
		}

		try {
			List<Map<String, Object>> rows = jdbc.sql("SELECT class_id, symbol_id FROM henna_trees ORDER BY class_id, symbol_id")
					.query()
					.listOfRows();

			for (var row : rows) {
				int classId = ((Number) row.get("class_id")).intValue();
				int symbolId = ((Number) row.get("symbol_id")).intValue();
				Henna template = hennaTable.get(symbolId);
				if (template != null) {
					byClassId.computeIfAbsent(classId, k -> new ArrayList<>()).add(template);
				}
			}

			log.info("HennaTreeTable carregada: {} associacoes de classe->henna", rows.size());
		} catch (Exception ex) {
			log.warn("Nao foi possivel carregar henna_trees do banco: {}. Usando fallback aberto.", ex.getMessage());
		}
	}
}
