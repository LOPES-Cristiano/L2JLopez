package com.lopez.l2j.game.boss;

import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Gerenciador de pontos de Raid Boss por jogador (tabela character_raid_points)
 * e calculo de ranking para a janela de Raids do mapa mundi (Alt+M / RequestGetBossRecord).
 */
@Service
public class RaidPointsService {

	private static final Logger log = LoggerFactory.getLogger(RaidPointsService.class);

	private final JdbcClient jdbc;
	// charId -> (bossId -> points)
	private final Map<Integer, Map<Integer, Integer>> raidPoints = new ConcurrentHashMap<>();

	@Autowired
	public RaidPointsService(@Autowired(required = false) JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@PostConstruct
	public void load() {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("SELECT charId, boss_id, points FROM character_raid_points")
					.query((rs, i) -> {
						int charId = rs.getInt("charId");
						int bossId = rs.getInt("boss_id");
						int points = rs.getInt("points");
						raidPoints.computeIfAbsent(charId, k -> new ConcurrentHashMap<>()).put(bossId, points);
						return null;
					}).list();
			log.info("RaidPointsService: carregados pontos de raid para {} personagens", raidPoints.size());
		} catch (Exception e) {
			log.warn("Nao foi possivel carregar character_raid_points: {}", e.getMessage());
		}
	}

	public int getPointsByOwnerId(int charId) {
		Map<Integer, Integer> map = raidPoints.get(charId);
		if (map == null || map.isEmpty()) {
			return 0;
		}
		return map.values().stream().mapToInt(Integer::intValue).sum();
	}

	public int calculateRanking(int charId) {
		int myPoints = getPointsByOwnerId(charId);
		if (myPoints <= 0) {
			return 0;
		}
		int rank = 1;
		for (var entry : raidPoints.entrySet()) {
			if (entry.getKey() != charId) {
				int total = entry.getValue().values().stream().mapToInt(Integer::intValue).sum();
				if (total > myPoints) {
					rank++;
				}
			}
		}
		return rank;
	}

	public Map<Integer, Integer> getList(int charId) {
		Map<Integer, Integer> map = raidPoints.get(charId);
		return map != null ? Collections.unmodifiableMap(map) : Collections.emptyMap();
	}

	public void addPoints(int charId, int bossId, int points) {
		if (charId <= 0 || bossId <= 0 || points <= 0) {
			return;
		}
		var charMap = raidPoints.computeIfAbsent(charId, k -> new ConcurrentHashMap<>());
		int newTotal = charMap.merge(bossId, points, Integer::sum);
		if (jdbc != null) {
			try {
				jdbc.sql("REPLACE INTO character_raid_points (charId, boss_id, points) VALUES (?, ?, ?)")
						.param(charId)
						.param(bossId)
						.param(newTotal)
						.update();
			} catch (Exception e) {
				log.warn("Erro ao salvar pontos de raid para charId {} bossId {}: {}", charId, bossId, e.getMessage());
			}
		}
	}
}
