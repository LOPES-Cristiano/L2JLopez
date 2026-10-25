package com.lopez.l2j.game.fishing;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.CharacterVariablesService;
import com.lopez.l2j.game.service.InventoryService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Servico do Campeonato de Pesca (fishingChampionship do legado).
 * Registra o comprimento dos maiores peixes pescados e premia os 5 melhores pescadores.
 */
@Service
public class FishingChampionshipService {

	private static final Logger log = LoggerFactory.getLogger(FishingChampionshipService.class);

	public static final int ADENA_ITEM_ID = 57;
	public static final String CLAIM_VAR_PREFIX = "FISHING_CHAMP_CLAIMED_";

	public record FisherScore(int playerId, String playerName, float fishLength, int rewarded) {
		public FisherScore(String playerName, float fishLength, int rewarded) {
			this(0, playerName, fishLength, rewarded);
		}
	}

	public record FisherReward(int rank, String playerName, float length, int adenaReward) {}

	private static final int[] REWARDS = { 800000, 500000, 300000, 200000, 100000 };

	private InventoryService inventoryService;
	private CharacterVariablesService characterVariablesService;
	private ScheduledExecutorService scheduler;
	private org.springframework.jdbc.core.simple.JdbcClient jdbc;

	private final List<FisherScore> rankings = new CopyOnWriteArrayList<>();
	private final List<FisherScore> previousWinners = new CopyOnWriteArrayList<>();
	private final Set<String> claimedPeriodRewards = ConcurrentHashMap.newKeySet();
	private int currentCycle = 1;

	public FishingChampionshipService() {
		this(null, null, null);
	}

	@Autowired
	public FishingChampionshipService(
			@Autowired(required = false) InventoryService inventoryService,
			@Autowired(required = false) CharacterVariablesService characterVariablesService,
			@Autowired(required = false) org.springframework.jdbc.core.simple.JdbcClient jdbc) {
		this.inventoryService = inventoryService;
		this.characterVariablesService = characterVariablesService;
		this.jdbc = jdbc;
		initSchema();
		loadFromDb();
		if (Config.FISHERMAN_ENABLED) {
			startChampionshipScheduler();
		}
	}

	public void initSchema() {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("""
					CREATE TABLE IF NOT EXISTS fishing_championship (
					    PlayerName VARCHAR(35) NOT NULL,
					    fishLength FLOAT(10,2) NOT NULL,
					    rewarded INT(1) NOT NULL
					)
					""").update();
			jdbc.sql("""
					CREATE TABLE IF NOT EXISTS fishing_championship_date (
					    finish_date BIGINT(20) UNSIGNED NOT NULL
					)
					""").update();
		} catch (Exception e) {
			log.debug("FishingChampionshipService initSchema: {}", e.getMessage());
		}
	}

	public synchronized void startChampionshipScheduler() {
		if (scheduler != null && !scheduler.isShutdown()) {
			return;
		}
		int intervalMinutes = getChampionshipIntervalMinutes();
		scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "FishingChampionship-Scheduler");
			t.setDaemon(true);
			return t;
		});
		scheduler.scheduleAtFixedRate(this::finishCycle, intervalMinutes, intervalMinutes, TimeUnit.MINUTES);
		log.info("FishingChampionshipService: agendador iniciado (interval={} min, rewardId={}, rewardCount={})",
				intervalMinutes, getRewardItemId(), Config.FISHERMAN_REWARD_COUNT);
	}

	public synchronized void stopChampionshipScheduler() {
		if (scheduler != null) {
			scheduler.shutdownNow();
			scheduler = null;
		}
	}

	public boolean isFishermanEventEnabled() {
		return Config.FISHERMAN_ENABLED;
	}

	public int getChampionshipIntervalMinutes() {
		return Math.max(1, Config.FISHERMAN_INTERVAL);
	}

	public int getRewardItemId() {
		return Config.FISHERMAN_REWARD_ID > 0 ? Config.FISHERMAN_REWARD_ID : ADENA_ITEM_ID;
	}

	public void setInventoryService(InventoryService inventoryService) {
		this.inventoryService = inventoryService;
	}

	public void setCharacterVariablesService(CharacterVariablesService characterVariablesService) {
		this.characterVariablesService = characterVariablesService;
	}

	/**
	 * Registra um novo peixe pescado para o ranking do campeonato (compatibilidade com nível do peixe).
	 */
	public synchronized float registerCatch(String playerName, int fishLevel) {
		float length = 40.0f + (fishLevel * 2.5f) + ThreadLocalRandom.current().nextFloat() * 20.0f;
		length = Math.round(length * 100.0f) / 100.0f;

		rankings.add(new FisherScore(0, playerName, length, 0));
		sortRankings();
		saveToDb();
		return length;
	}

	/**
	 * Submete um peixe capturado com comprimento específico para o campeonato de pesca.
	 * Se o jogador já estiver no ranking com um peixe menor, atualiza para o maior comprimento.
	 */
	public synchronized void submitFish(int playerId, String playerName, double fishLength) {
		if (fishLength <= 0) {
			return;
		}
		float roundedLength = Math.round((float) fishLength * 100.0f) / 100.0f;

		if (playerId > 0) {
			for (int i = 0; i < rankings.size(); i++) {
				FisherScore existing = rankings.get(i);
				if (existing.playerId() == playerId) {
					if (roundedLength > existing.fishLength()) {
						rankings.set(i, new FisherScore(playerId, playerName, roundedLength, existing.rewarded()));
						sortRankings();
						saveToDb();
					}
					return;
				}
			}
		}

		rankings.add(new FisherScore(playerId, playerName, roundedLength, 0));
		sortRankings();
		saveToDb();
	}

	/**
	 * Retorna a posição atual (1-based) do jogador no ranking do campeonato, ou 0 se não ranqueado.
	 */
	public synchronized int getRank(int playerId) {
		if (playerId <= 0) {
			return 0;
		}
		sortRankings();
		for (int i = 0; i < rankings.size(); i++) {
			if (rankings.get(i).playerId() == playerId) {
				return i + 1;
			}
		}
		return 0;
	}

	/**
	 * Retorna a posição atual (1-based) pelo nome do jogador, ou 0 se não ranqueado.
	 */
	public synchronized int getRankByName(String playerName) {
		if (playerName == null || playerName.isBlank()) {
			return 0;
		}
		sortRankings();
		for (int i = 0; i < rankings.size(); i++) {
			if (playerName.equalsIgnoreCase(rankings.get(i).playerName())) {
				return i + 1;
			}
		}
		return 0;
	}

	/**
	 * Retorna a recompensa em Adena correspondente à colocação (1º ao 5º lugar).
	 */
	public int getRewardForRank(int rank) {
		if (rank < 1 || rank > REWARDS.length) {
			return 0;
		}
		if (Config.FISHERMAN_REWARD_COUNT > 0) {
			return Math.max(1, Config.FISHERMAN_REWARD_COUNT / rank);
		}
		return REWARDS[rank - 1];
	}

	/**
	 * Resgata o prêmio do campeonato de pesca para o jogador conforme sua posição no ranking,
	 * garantindo que não resgate mais de uma vez no ciclo atual e entregando Adena via InventoryService.
	 */
	public synchronized int claimReward(PlayerCharacter player) {
		if (player == null) {
			return 0;
		}
		int rank = getRank(player.objectId());
		if (rank <= 0 && player.name() != null) {
			rank = getRankByName(player.name());
		}
		boolean fromPreviousCycle = false;
		if (rank <= 0) {
			for (int i = 0; i < previousWinners.size(); i++) {
				FisherScore prev = previousWinners.get(i);
				if ((prev.playerId() > 0 && prev.playerId() == player.objectId())
						|| (player.name() != null && player.name().equalsIgnoreCase(prev.playerName()))) {
					if (prev.rewarded() == 0) {
						rank = i + 1;
						fromPreviousCycle = true;
					}
					break;
				}
			}
		}

		int reward = getRewardForRank(rank);
		if (reward <= 0) {
			return 0;
		}

		int targetCycle = fromPreviousCycle ? Math.max(1, currentCycle - 1) : currentCycle;
		String claimKey = targetCycle + ":" + player.objectId();
		if (claimedPeriodRewards.contains(claimKey)) {
			return 0;
		}

		String varName = CLAIM_VAR_PREFIX + targetCycle;
		if (characterVariablesService != null && characterVariablesService.getBoolean(player.objectId(), varName, false)) {
			return 0;
		}

		int index = rank - 1;
		if (fromPreviousCycle) {
			if (index >= 0 && index < previousWinners.size()) {
				FisherScore current = previousWinners.get(index);
				if (current.rewarded() == 1) {
					return 0;
				}
				previousWinners.set(index, new FisherScore(player.objectId(), current.playerName(), current.fishLength(), 1));
			}
		} else if (index >= 0 && index < rankings.size()) {
			FisherScore current = rankings.get(index);
			if (current.rewarded() == 1) {
				return 0;
			}
			rankings.set(index, new FisherScore(player.objectId(), current.playerName(), current.fishLength(), 1));
			saveToDb();
		}

		claimedPeriodRewards.add(claimKey);
		if (characterVariablesService != null) {
			characterVariablesService.set(player.objectId(), varName, true);
		}

		int rewardItemId = getRewardItemId();
		var inv = player.inventory();
		if (inv != null) {
			if (inventoryService != null) {
				var added = inventoryService.addItem(inv, rewardItemId, reward, "FishingChampionship");
				if (added == null && rewardItemId == ADENA_ITEM_ID) {
					inv.addAdena(reward);
				}
			} else if (rewardItemId == ADENA_ITEM_ID) {
				inv.addAdena(reward);
			}
		}

		log.info("FishingChampionship: Jogador {} (Rank #{}) resgatou {} (item {}) no ciclo #{}",
				player.name(), rank, reward, rewardItemId, targetCycle);
		return reward;
	}

	public List<FisherScore> getTopFishers(int limit) {
		sortRankings();
		List<FisherScore> top = new ArrayList<>();
		for (int i = 0; i < Math.min(limit, rankings.size()); i++) {
			top.add(rankings.get(i));
		}
		return Collections.unmodifiableList(top);
	}

	public List<FisherScore> getPreviousWinners() {
		return Collections.unmodifiableList(previousWinners);
	}

	public int getCurrentCycle() {
		return currentCycle;
	}

	public synchronized List<FisherReward> finishCycle() {
		sortRankings();
		previousWinners.clear();
		List<FisherReward> payouts = new ArrayList<>();
		int limit = Math.min(5, rankings.size());
		for (int i = 0; i < limit; i++) {
			FisherScore fs = rankings.get(i);
			previousWinners.add(fs);
			payouts.add(new FisherReward(i + 1, fs.playerName(), fs.fishLength(), getRewardForRank(i + 1)));
		}
		currentCycle++;
		rankings.clear();
		claimedPeriodRewards.clear();
		saveToDb();
		return payouts;
	}

	public synchronized void clear() {
		rankings.clear();
		previousWinners.clear();
		claimedPeriodRewards.clear();
		saveToDb();
	}

	public synchronized void loadFromDb() {
		if (jdbc == null) {
			return;
		}
		try {
			var rows = jdbc.sql("SELECT PlayerName, fishLength, rewarded FROM fishing_championship").query().listOfRows();
			for (var r : rows) {
				String name = (String) r.get("PlayerName");
				float len = ((Number) r.get("fishLength")).floatValue();
				int rewarded = ((Number) r.get("rewarded")).intValue();
				rankings.add(new FisherScore(0, name, len, rewarded));
			}
			sortRankings();
			log.info("FishingChampionshipService: {} registros carregados de fishing_championship", rankings.size());
		} catch (Exception ex) {
			log.warn("Nao foi possivel carregar fishing_championship do banco: {}", ex.getMessage());
		}
	}

	public synchronized void saveToDb() {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("DELETE FROM fishing_championship").update();
			for (FisherScore fs : rankings) {
				jdbc.sql("INSERT INTO fishing_championship (PlayerName, fishLength, rewarded) VALUES (:name, :len, :rew)")
						.param("name", fs.playerName())
						.param("len", fs.fishLength())
						.param("rew", fs.rewarded())
						.update();
			}
			jdbc.sql("DELETE FROM fishing_championship_date").update();
			jdbc.sql("INSERT INTO fishing_championship_date (finish_date) VALUES (:d)")
					.param("d", System.currentTimeMillis())
					.update();
		} catch (Exception ex) {
			log.warn("Erro ao salvar fishing_championship no banco: {}", ex.getMessage());
		}
	}

	private void sortRankings() {
		rankings.sort(Comparator.comparingDouble(FisherScore::fishLength).reversed());
	}
}
