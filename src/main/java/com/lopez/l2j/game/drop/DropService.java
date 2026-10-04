package com.lopez.l2j.game.drop;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Servico de processamento de drops e recompensas de monstros derrotados.
 */
@Service
public class DropService {

	private static final Logger log = LoggerFactory.getLogger(DropService.class);

	private final DropTable dropTable;
	private final double rateAdena;
	private final double rateDrop;
	private final double rateSpoil;
	private final boolean autoLoot;

	public DropService(
			DropTable dropTable,
			@Value("${l2.rates.adena:1.0}") double rateAdena,
			@Value("${l2.rates.drop:1.0}") double rateDrop,
			@Value("${l2.rates.spoil:1.0}") double rateSpoil,
			@Value("${l2.game.autoloot:true}") boolean autoLoot) {
		this.dropTable = dropTable;
		this.rateAdena = Math.max(0.1, rateAdena);
		this.rateDrop = Math.max(0.1, rateDrop);
		this.rateSpoil = Math.max(0.1, rateSpoil);
		this.autoLoot = autoLoot;
	}

	/**
	 * Sorteia os drops de um monstro baseado na tabela de droplist e nas taxas configuradas.
	 */
	public List<DropReward> rollDrops(int mobId) {
		List<DropData> rules = dropTable.getDrops(mobId);
		if (rules.isEmpty()) {
			return List.of();
		}

		List<DropReward> rewards = new ArrayList<>();
		ThreadLocalRandom rng = ThreadLocalRandom.current();

		for (DropData rule : rules) {
			if (rule.isSpoil()) {
				// Spoil e tratado separadamente via habilidade Sweeper/Spoil
				continue;
			}

			if (rule.isAdena()) {
				// Calculo de Adena
				double rate = rateAdena;
				long effectiveChance = Math.round(rule.chance() * rate);
				if (effectiveChance > DropData.MAX_CHANCE) {
					// Quando o rate ultrapassa 100%, garante drop e multiplica a quantidade
					double extraMultiplier = (double) effectiveChance / DropData.MAX_CHANCE;
					int baseCount = randomCount(rng, rule.min(), rule.max());
					int finalCount = (int) Math.max(1, Math.round(baseCount * extraMultiplier));
					rewards.add(new DropReward(rule.itemId(), finalCount, true));
				} else if (rng.nextInt(DropData.MAX_CHANCE) < effectiveChance) {
					int baseCount = randomCount(rng, rule.min(), rule.max());
					int finalCount = (int) Math.max(1, Math.round(baseCount * rate));
					rewards.add(new DropReward(rule.itemId(), finalCount, true));
				}
			} else {
				// Calculo de Itens / Materiais / Equipamentos
				double rate = rateDrop;
				long effectiveChance = Math.round(rule.chance() * rate);
				if (effectiveChance >= DropData.MAX_CHANCE || rng.nextInt(DropData.MAX_CHANCE) < effectiveChance) {
					int count = randomCount(rng, rule.min(), rule.max());
					rewards.add(new DropReward(rule.itemId(), count, false));
				}
			}
		}

		return rewards;
	}

	/**
	 * Processa a entrega das recompensas de drop ao jogador que abateu o monstro.
	 */
	public List<DropReward> rewardMonsterDeath(
			PlayerCharacter player,
			int mobId,
			InventoryService inventoryService,
			Consumer<GameServerPacket> packetSender) {

		List<DropReward> rewards = rollDrops(mobId);
		if (rewards.isEmpty()) {
			return List.of();
		}

		for (DropReward reward : rewards) {
			if (autoLoot && inventoryService != null) {
				try {
					inventoryService.addItem(player.inventory(), reward.itemId(), reward.count(), "Drop");
					if (packetSender != null) {
						if (reward.isAdena()) {
							packetSender.accept(SystemMessage.of(
									SystemMessage.EARNED_S2_S1_S,
									new SystemMessage.Number(reward.count())));
						} else {
							packetSender.accept(SystemMessage.of(
									SystemMessage.YOU_PICKED_UP_S1_S2,
									new SystemMessage.Number(reward.count()),
									new SystemMessage.ItemName(reward.itemId())));
						}
					}
				} catch (Exception e) {
					log.warn("Falha ao entregar drop {} x{} para {}: {}",
							reward.itemId(), reward.count(), player.name(), e.getMessage());
				}
			}
		}

		return rewards;
	}

	private static int randomCount(ThreadLocalRandom rng, int min, int max) {
		if (min >= max) {
			return min;
		}
		return rng.nextInt(min, max + 1);
	}
}
