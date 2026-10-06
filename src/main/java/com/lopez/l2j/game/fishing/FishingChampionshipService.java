package com.lopez.l2j.game.fishing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

/**
 * Servico do Campeonato de Pesca (fishingChampionship do legado).
 * Registra o comprimento dos maiores peixes pescados e premia os 5 melhores pescadores.
 */
@Service
public class FishingChampionshipService {

	public record FisherScore(String playerName, float fishLength, int rewarded) {
	}

	private final List<FisherScore> rankings = new CopyOnWriteArrayList<>();

	/**
	 * Registra um novo peixe pescado para o ranking do campeonato.
	 */
	public float registerCatch(String playerName, int fishLevel) {
		float length = 40.0f + (fishLevel * 2.5f) + ThreadLocalRandom.current().nextFloat() * 20.0f;
		// Arredonda para 2 casas decimais
		length = Math.round(length * 100.0f) / 100.0f;

		rankings.add(new FisherScore(playerName, length, 0));
		sortRankings();
		return length;
	}

	public List<FisherScore> getTopFishers(int limit) {
		sortRankings();
		List<FisherScore> top = new ArrayList<>();
		for (int i = 0; i < Math.min(limit, rankings.size()); i++) {
			top.add(rankings.get(i));
		}
		return Collections.unmodifiableList(top);
	}

	public record FisherReward(int rank, String playerName, float length, int adenaReward) {}

	private static final int[] REWARDS = { 800000, 500000, 300000, 200000, 100000 };
	private int currentCycle = 1;

	public int getCurrentCycle() {
		return currentCycle;
	}

	public List<FisherReward> finishCycle() {
		sortRankings();
		List<FisherReward> payouts = new ArrayList<>();
		int limit = Math.min(5, rankings.size());
		for (int i = 0; i < limit; i++) {
			FisherScore fs = rankings.get(i);
			payouts.add(new FisherReward(i + 1, fs.playerName(), fs.fishLength(), REWARDS[i]));
		}
		currentCycle++;
		rankings.clear();
		return payouts;
	}

	public void clear() {
		rankings.clear();
	}

	private void sortRankings() {
		rankings.sort(Comparator.comparingDouble(FisherScore::fishLength).reversed());
	}
}
