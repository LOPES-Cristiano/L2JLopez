package com.lopez.l2j.game.olympiad;

import jakarta.annotation.PostConstruct;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Gerenciador das Grandes Olimpiadas de Lineage II Interlude.
 * Controla os Nobres registrados, pontuacao, historico de lutas,
 * determinacao de Herois e sincronizacao com olympiad_nobles.
 */
@Service
public class OlympiadManager {

	private static final Logger log = LoggerFactory.getLogger(OlympiadManager.class);

	public static final int PERIOD_COMPETITION = 0;
	public static final int PERIOD_VALIDATION = 1;
	public static final int MIN_MATCHES_FOR_HERO = 5;

	private final Map<Integer, OlympiadNoble> nobles = new ConcurrentHashMap<>();
	private final JdbcClient jdbc;

	private int currentCycle = 1;
	private int period = PERIOD_COMPETITION;

	@Autowired
	public OlympiadManager(@Autowired(required = false) JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@PostConstruct
	public void init() {
		loadFromDb();
	}

	public int currentCycle() {
		return currentCycle;
	}

	public void currentCycle(int currentCycle) {
		this.currentCycle = currentCycle;
	}

	public int period() {
		return period;
	}

	public void period(int period) {
		this.period = period;
	}

	public boolean isNoble(int charId) {
		return nobles.containsKey(charId);
	}

	public Optional<OlympiadNoble> getNoble(int charId) {
		return Optional.ofNullable(nobles.get(charId));
	}

	public Collection<OlympiadNoble> allNobles() {
		return Collections.unmodifiableCollection(nobles.values());
	}

	public synchronized OlympiadNoble registerNoble(int charId, String charName, int classId) {
		OlympiadNoble noble = nobles.computeIfAbsent(charId, id -> new OlympiadNoble(id, charName, classId));
		if (charName != null && !charName.isBlank()) {
			noble.charName(charName);
		}
		persistNoble(noble);
		return noble;
	}

	public synchronized void recordMatch(int winnerId, int loserId, boolean draw) {
		OlympiadNoble winner = nobles.get(winnerId);
		OlympiadNoble loser = nobles.get(loserId);
		if (winner == null || loser == null) {
			return;
		}

		if (draw) {
			winner.recordDraw();
			loser.recordDraw();
		} else {
			int pointTransfer = Math.max(1, Math.min(10, Math.min(winner.points(), loser.points()) / 5));
			winner.recordWin(pointTransfer);
			loser.recordLoss(pointTransfer);
		}

		persistNoble(winner);
		persistNoble(loser);

		log.info("Olympiad Match gravada: Winner={}, Loser={}, Draw={}",
				winner.charName(), loser.charName(), draw);
	}

	/**
	 * Computa os novos Herois da temporada por classId.
	 * Retorna mapa de classId -> Nobre com maior pontuacao (e minimo de partidas).
	 */
	public Map<Integer, OlympiadNoble> computeHeroes() {
		Map<Integer, OlympiadNoble> heroes = new HashMap<>();

		for (OlympiadNoble noble : nobles.values()) {
			if (noble.competitionsDone() < MIN_MATCHES_FOR_HERO) {
				continue;
			}
			OlympiadNoble existing = heroes.get(noble.classId());
			if (existing == null || noble.points() > existing.points()) {
				heroes.put(noble.classId(), noble);
			}
		}

		return Collections.unmodifiableMap(heroes);
	}

	public void resetWeeklyPoints() {
		for (OlympiadNoble noble : nobles.values()) {
			noble.points(noble.points() + OlympiadNoble.DEFAULT_POINTS);
			persistNoble(noble);
		}
		log.info("Olympiad: +{} pontos semanais concedidos aos nobress", OlympiadNoble.DEFAULT_POINTS);
	}

	private void loadFromDb() {
		if (jdbc == null) {
			return;
		}
		try {
			var rows = jdbc.sql("""
					SELECT charId, class_id, olympiad_points, competitions_done,
					       competitions_won, competitions_lost, competitions_drawn
					FROM olympiad_nobles
					""").query().listOfRows();

			for (var row : rows) {
				int charId = ((Number) row.get("charId")).intValue();
				int classId = ((Number) row.get("class_id")).intValue();
				int points = ((Number) row.get("olympiad_points")).intValue();
				int done = ((Number) row.get("competitions_done")).intValue();
				int won = ((Number) row.get("competitions_won")).intValue();
				int lost = ((Number) row.get("competitions_lost")).intValue();
				int drawn = ((Number) row.get("competitions_drawn")).intValue();

				nobles.put(charId, new OlympiadNoble(charId, "", classId, points, done, won, lost, drawn));
			}
			log.info("OlympiadManager carregou {} nobres do banco", rows.size());
		} catch (Exception ex) {
			log.warn("Nao foi possivel carregar olympiad_nobles do banco: {}", ex.getMessage());
		}
	}

	private void persistNoble(OlympiadNoble noble) {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("""
					INSERT INTO olympiad_nobles (charId, class_id, olympiad_points, competitions_done,
					                            competitions_won, competitions_lost, competitions_drawn)
					VALUES (:charId, :class_id, :olympiad_points, :competitions_done,
					        :competitions_won, :competitions_lost, :competitions_drawn)
					ON DUPLICATE KEY UPDATE
						class_id = :class_id,
						olympiad_points = :olympiad_points,
						competitions_done = :competitions_done,
						competitions_won = :competitions_won,
						competitions_lost = :competitions_lost,
						competitions_drawn = :competitions_drawn
					""")
					.param("charId", noble.charId())
					.param("class_id", noble.classId())
					.param("olympiad_points", noble.points())
					.param("competitions_done", noble.competitionsDone())
					.param("competitions_won", noble.competitionsWon())
					.param("competitions_lost", noble.competitionsLost())
					.param("competitions_drawn", noble.competitionsDrawn())
					.update();
		} catch (Exception ex) {
			log.error("Erro ao persistir nobre olympiad {}", noble.charId(), ex);
		}
	}
}
