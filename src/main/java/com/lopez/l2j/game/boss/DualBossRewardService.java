package com.lopez.l2j.game.boss;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servico de Premiacao Dupla em Bosses (Dual Boss Reward) - Onda F6 / Source L2JRadical.
 * Premia tanto o Top DPS (maior causador de dano acumulado) quanto o Last Attacker (golpe final).
 * Economia Estrita: ZERO DOACOES. Premios em Adena 57 e Medalhas de Evento 6392 / 6393.
 */
@Service
public class DualBossRewardService {

	private static final Logger log = LoggerFactory.getLogger(DualBossRewardService.class);

	public static final int REWARD_ADENA_ID = 57;
	public static final long REWARD_ADENA_TOP_DPS = 5_000_000L;
	public static final long REWARD_ADENA_LAST_HIT = 2_500_000L;

	public static final int REWARD_MEDAL_ID = 6392; // Event Medal
	public static final int REWARD_GLITTERING_MEDAL_ID = 6393; // Glittering Medal

	// bossObjectId -> (attackerObjectId -> accumulatedDamage)
	private final Map<Integer, Map<Integer, Long>> damageByBoss = new ConcurrentHashMap<>();
	private final Map<Integer, PlayerCharacter> playerCache = new ConcurrentHashMap<>();

	public void recordDamage(int bossObjectId, PlayerCharacter attacker, int damage) {
		if (bossObjectId <= 0 || attacker == null || damage <= 0) return;
		playerCache.put(attacker.objectId(), attacker);
		damageByBoss.computeIfAbsent(bossObjectId, k -> new ConcurrentHashMap<>())
				.merge(attacker.objectId(), (long) damage, Long::sum);
	}

	public PlayerCharacter getTopDps(int bossObjectId) {
		Map<Integer, Long> map = damageByBoss.get(bossObjectId);
		if (map == null || map.isEmpty()) return null;
		int topPlayerId = 0;
		long maxDamage = -1;
		for (var entry : map.entrySet()) {
			if (entry.getValue() > maxDamage) {
				maxDamage = entry.getValue();
				topPlayerId = entry.getKey();
			}
		}
		return playerCache.get(topPlayerId);
	}

	public void rewardOnBossKill(NpcInstance boss, PlayerCharacter lastAttacker) {
		if (boss == null) return;
		int bossObjId = boss.objectId();
		PlayerCharacter topDps = getTopDps(bossObjId);

		// Entregar ao Top DPS
		if (topDps != null && topDps.inventory() != null) {
			topDps.inventory().addItem(REWARD_ADENA_ID, (int) Math.min(Integer.MAX_VALUE, REWARD_ADENA_TOP_DPS));
			topDps.inventory().addItem(REWARD_GLITTERING_MEDAL_ID, 5);
			topDps.sendMessage("[Dual Boss Reward] Voce foi o TOP DPS no Boss " + boss.name() + "! Recompensa entregue.");
			log.info("DualBossReward: TOP DPS {} recebeu recompensa de boss {}", topDps.name(), boss.name());
		}

		// Entregar ao Last Attacker
		if (lastAttacker != null && lastAttacker.inventory() != null) {
			lastAttacker.inventory().addItem(REWARD_ADENA_ID, (int) Math.min(Integer.MAX_VALUE, REWARD_ADENA_LAST_HIT));
			lastAttacker.inventory().addItem(REWARD_MEDAL_ID, 10);
			lastAttacker.sendMessage("[Dual Boss Reward] Voce deu o GOLPE FINAL no Boss " + boss.name() + "! Recompensa entregue.");
			log.info("DualBossReward: LAST HIT {} recebeu recompensa de boss {}", lastAttacker.name(), boss.name());
		}

		// Limpa tabela de dano do boss abatido
		damageByBoss.remove(bossObjId);
	}
}
