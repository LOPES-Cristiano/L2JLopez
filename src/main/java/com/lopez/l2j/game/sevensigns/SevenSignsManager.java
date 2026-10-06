package com.lopez.l2j.game.sevensigns;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Gerenciador dos Sete Selos (Seven Signs: Cabals of Dawn and Dusk,
 * Selos da Avareza, Gnose e Luta, pedras de selo e Ancient Adena).
 */
@Service
public class SevenSignsManager {

	private static final Logger log = LoggerFactory.getLogger(SevenSignsManager.class);

	// Cabais
	public static final int CABAL_NULL = 0;
	public static final int CABAL_DUSK = 1;
	public static final int CABAL_DAWN = 2;

	// Selos
	public static final int SEAL_NULL = 0;
	public static final int SEAL_AVARICE = 1;
	public static final int SEAL_GNOSIS = 2;
	public static final int SEAL_STRIFE = 3;

	// Periodos do Ciclo
	public static final int PERIOD_COMP_RECRUITING = 0;
	public static final int PERIOD_COMPETITION = 1;
	public static final int PERIOD_COMP_RESULTS = 2;
	public static final int PERIOD_SEAL_VALIDATION = 3;

	// Itens de Pedras de Selo
	public static final int SEAL_STONE_BLUE_ID = 6360;
	public static final int SEAL_STONE_GREEN_ID = 6361;
	public static final int SEAL_STONE_RED_ID = 6362;
	public static final int ANCIENT_ADENA_ID = 5575;

	// Valores de conversao em Ancient Adena
	public static final int SEAL_STONE_BLUE_VALUE = 3;
	public static final int SEAL_STONE_GREEN_VALUE = 5;
	public static final int SEAL_STONE_RED_VALUE = 10;

	public record PlayerData(int charId, int cabal, int seal, int redStones, int greenStones, int blueStones, int ancientAdena) {
	}

	private final Map<Integer, PlayerData> players = new ConcurrentHashMap<>();
	private final JdbcClient jdbc;

	private int currentCycle = 1;
	private int activePeriod = PERIOD_COMPETITION;
	private int previousWinner = CABAL_NULL;

	private long dawnStoneScore = 0;
	private long duskStoneScore = 0;
	private int dawnFestivalScore = 0;
	private int duskFestivalScore = 0;

	private int avariceOwner = CABAL_NULL;
	private int gnosisOwner = CABAL_NULL;
	private int strifeOwner = CABAL_NULL;

	@Autowired
	public SevenSignsManager(@Autowired(required = false) JdbcClient jdbc) {
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

	public int activePeriod() {
		return activePeriod;
	}

	public void activePeriod(int activePeriod) {
		this.activePeriod = activePeriod;
	}

	public int previousWinner() {
		return previousWinner;
	}

	public void previousWinner(int previousWinner) {
		this.previousWinner = previousWinner;
	}

	public long dawnStoneScore() {
		return dawnStoneScore;
	}

	public long duskStoneScore() {
		return duskStoneScore;
	}

	public int dawnFestivalScore() {
		return dawnFestivalScore;
	}

	public int duskFestivalScore() {
		return duskFestivalScore;
	}

	public int avariceOwner() {
		return avariceOwner;
	}

	public int gnosisOwner() {
		return gnosisOwner;
	}

	public int strifeOwner() {
		return strifeOwner;
	}

	public int getSealOwner(int seal) {
		return switch (seal) {
			case SEAL_AVARICE -> avariceOwner;
			case SEAL_GNOSIS -> gnosisOwner;
			case SEAL_STRIFE -> strifeOwner;
			default -> CABAL_NULL;
		};
	}

	public void setSealOwner(int seal, int cabal) {
		switch (seal) {
			case SEAL_AVARICE -> avariceOwner = cabal;
			case SEAL_GNOSIS -> gnosisOwner = cabal;
			case SEAL_STRIFE -> strifeOwner = cabal;
		}
	}

	public Optional<PlayerData> getPlayerData(int charId) {
		return Optional.ofNullable(players.get(charId));
	}

	public int getPlayerCabal(int charId) {
		PlayerData data = players.get(charId);
		return data != null ? data.cabal() : CABAL_NULL;
	}

	public int getPlayerSeal(int charId) {
		PlayerData data = players.get(charId);
		return data != null ? data.seal() : SEAL_NULL;
	}

	public synchronized boolean registerPlayer(int charId, int cabal, int seal) {
		if (cabal != CABAL_DAWN && cabal != CABAL_DUSK) {
			return false;
		}
		PlayerData existing = players.get(charId);
		PlayerData updated = new PlayerData(charId, cabal, seal,
				existing != null ? existing.redStones() : 0,
				existing != null ? existing.greenStones() : 0,
				existing != null ? existing.blueStones() : 0,
				existing != null ? existing.ancientAdena() : 0);
		players.put(charId, updated);
		persistPlayerData(updated);
		return true;
	}

	public synchronized int contributeStones(int charId, int blue, int green, int red) {
		PlayerData existing = players.get(charId);
		if (existing == null || existing.cabal() == CABAL_NULL) {
			return 0;
		}

		int earnedAA = (blue * SEAL_STONE_BLUE_VALUE) + (green * SEAL_STONE_GREEN_VALUE) + (red * SEAL_STONE_RED_VALUE);
		long scoreGain = earnedAA;

		if (existing.cabal() == CABAL_DAWN) {
			dawnStoneScore += scoreGain;
		} else if (existing.cabal() == CABAL_DUSK) {
			duskStoneScore += scoreGain;
		}

		PlayerData updated = new PlayerData(charId, existing.cabal(), existing.seal(),
				existing.redStones() + red,
				existing.greenStones() + green,
				existing.blueStones() + blue,
				existing.ancientAdena() + earnedAA);
		players.put(charId, updated);

		persistPlayerData(updated);
		persistStatus();
		return earnedAA;
	}

	public synchronized int claimAncientAdena(int charId) {
		PlayerData existing = players.get(charId);
		if (existing == null || existing.ancientAdena() <= 0) {
			return 0;
		}
		int reward = existing.ancientAdena();
		PlayerData updated = new PlayerData(charId, existing.cabal(), existing.seal(),
				existing.redStones(), existing.greenStones(), existing.blueStones(), 0);
		players.put(charId, updated);
		persistPlayerData(updated);
		return reward;
	}

	public int getWinningCabal() {
		long totalDawn = dawnStoneScore + ((long) dawnFestivalScore * 500L);
		long totalDusk = duskStoneScore + ((long) duskFestivalScore * 500L);
		if (totalDawn > totalDusk) {
			return CABAL_DAWN;
		} else if (totalDusk > totalDawn) {
			return CABAL_DUSK;
		}
		return CABAL_NULL;
	}

	public int getSkyState() {
		if (activePeriod != PERIOD_SEAL_VALIDATION) {
			return 256; // Normal
		}
		return switch (previousWinner) {
			case CABAL_DUSK -> 257; // Blood Red sky
			case CABAL_DAWN -> 258; // Blue sky
			default -> 256;
		};
	}

	private void loadFromDb() {
		if (jdbc == null) {
			return;
		}
		try {
			var statusRows = jdbc.sql("""
					SELECT current_cycle, active_period, previous_winner,
					       dawn_stone_score, dusk_stone_score,
					       dawn_festival_score, dusk_festival_score,
					       avarice_owner, gnosis_owner, strife_owner
					FROM seven_signs_status WHERE id = 0
					""").query().listOfRows();

			if (!statusRows.isEmpty()) {
				var row = statusRows.getFirst();
				currentCycle = ((Number) row.get("current_cycle")).intValue();
				activePeriod = ((Number) row.get("active_period")).intValue();
				previousWinner = ((Number) row.get("previous_winner")).intValue();
				dawnStoneScore = ((Number) row.get("dawn_stone_score")).longValue();
				duskStoneScore = ((Number) row.get("dusk_stone_score")).longValue();
				dawnFestivalScore = ((Number) row.get("dawn_festival_score")).intValue();
				duskFestivalScore = ((Number) row.get("dusk_festival_score")).intValue();
				avariceOwner = ((Number) row.get("avarice_owner")).intValue();
				gnosisOwner = ((Number) row.get("gnosis_owner")).intValue();
				strifeOwner = ((Number) row.get("strife_owner")).intValue();
				log.info("SevenSignsManager: Status carregado do banco. Ciclo={}, Periodo={}, Vencedor={}",
						currentCycle, activePeriod, previousWinner);
			}

			var playerRows = jdbc.sql("""
					SELECT charId, cabal, seal, red_stones, green_stones, blue_stones, ancient_adena_amount
					FROM seven_signs
					""").query().listOfRows();

			for (var r : playerRows) {
				int cid = ((Number) r.get("charId")).intValue();
				int cabal = ((Number) r.get("cabal")).intValue();
				int seal = ((Number) r.get("seal")).intValue();
				int red = ((Number) r.get("red_stones")).intValue();
				int green = ((Number) r.get("green_stones")).intValue();
				int blue = ((Number) r.get("blue_stones")).intValue();
				int aa = ((Number) r.get("ancient_adena_amount")).intValue();
				players.put(cid, new PlayerData(cid, cabal, seal, red, green, blue, aa));
			}
			log.info("SevenSignsManager: {} registros de jogadores carregados", playerRows.size());
		} catch (Exception ex) {
			log.warn("Nao foi possivel carregar dados de seven_signs do banco: {}", ex.getMessage());
		}
	}

	private void persistStatus() {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("""
					INSERT INTO seven_signs_status (id, current_cycle, active_period, previous_winner,
					                                dawn_stone_score, dusk_stone_score,
					                                dawn_festival_score, dusk_festival_score,
					                                avarice_owner, gnosis_owner, strife_owner)
					VALUES (0, :current_cycle, :active_period, :previous_winner,
					        :dawn_stone_score, :dusk_stone_score,
					        :dawn_festival_score, :dusk_festival_score,
					        :avarice_owner, :gnosis_owner, :strife_owner)
					ON DUPLICATE KEY UPDATE
						current_cycle = :current_cycle,
						active_period = :active_period,
						previous_winner = :previous_winner,
						dawn_stone_score = :dawn_stone_score,
						dusk_stone_score = :dusk_stone_score,
						dawn_festival_score = :dawn_festival_score,
						dusk_festival_score = :dusk_festival_score,
						avarice_owner = :avarice_owner,
						gnosis_owner = :gnosis_owner,
						strife_owner = :strife_owner
					""")
					.param("current_cycle", currentCycle)
					.param("active_period", activePeriod)
					.param("previous_winner", previousWinner)
					.param("dawn_stone_score", dawnStoneScore)
					.param("dusk_stone_score", duskStoneScore)
					.param("dawn_festival_score", dawnFestivalScore)
					.param("dusk_festival_score", duskFestivalScore)
					.param("avarice_owner", avariceOwner)
					.param("gnosis_owner", gnosisOwner)
					.param("strife_owner", strifeOwner)
					.update();
		} catch (Exception ex) {
			log.error("Erro ao persistir seven_signs_status", ex);
		}
	}

	private void persistPlayerData(PlayerData p) {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("""
					INSERT INTO seven_signs (charId, cabal, seal, red_stones, green_stones, blue_stones, ancient_adena_amount)
					VALUES (:charId, :cabal, :seal, :red_stones, :green_stones, :blue_stones, :ancient_adena_amount)
					ON DUPLICATE KEY UPDATE
						cabal = :cabal,
						seal = :seal,
						red_stones = :red_stones,
						green_stones = :green_stones,
						blue_stones = :blue_stones,
						ancient_adena_amount = :ancient_adena_amount
					""")
					.param("charId", p.charId())
					.param("cabal", p.cabal())
					.param("seal", p.seal())
					.param("red_stones", p.redStones())
					.param("green_stones", p.greenStones())
					.param("blue_stones", p.blueStones())
					.param("ancient_adena_amount", p.ancientAdena())
					.update();
		} catch (Exception ex) {
			log.error("Erro ao persistir seven_signs player {}", p.charId(), ex);
		}
	}
}
