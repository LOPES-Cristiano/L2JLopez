package com.lopez.l2j.game.drop;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.InventoryUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.StatusUpdate;
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

	public List<DropData> getDrops(int mobId) {
		return dropTable.getDrops(mobId);
	}

	/**
	 * Sorteia os drops de um monstro baseado na tabela de droplist e nas taxas configuradas.
	 */
	public List<DropReward> rollDrops(int mobId) {
		return rollDrops(mobId, 0, 0);
	}

	public List<DropReward> rollDrops(int mobId, int playerLevel, int mobLevel) {
		List<DropData> rules = dropTable.getDrops(mobId);
		if (rules.isEmpty()) {
			return List.of();
		}

		double levelPenalty = 1.0;
		if (Config.getBoolean("UseDeepBlueDropRules", true) && playerLevel > 0 && mobLevel > 0) {
			int diff = playerLevel - mobLevel;
			if (diff >= 9) {
				levelPenalty = Math.max(0.0, 1.0 - ((diff - 8) * 0.2));
			}
		}
		if (levelPenalty <= 0.0) {
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
				double rate = (rateAdena > 0 ? rateAdena : Config.RATE_DROP_ADENA) * levelPenalty;
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
				double rate = (rateDrop > 0 ? rateDrop : Config.RATE_DROP_ITEMS) * levelPenalty;
				long effectiveChance = Math.round(rule.chance() * rate);
				if (effectiveChance >= DropData.MAX_CHANCE || rng.nextInt(DropData.MAX_CHANCE) < effectiveChance) {
					int count = randomCount(rng, rule.min(), rule.max());
					rewards.add(new DropReward(rule.itemId(), count, false));
				}
			}
		}

		return rewards;
	}

	public List<DropReward> rewardMonsterDeath(
			PlayerCharacter player,
			int mobId,
			InventoryService inventoryService,
			Consumer<GameServerPacket> packetSender) {
		return rewardMonsterDeath(player, mobId, 0, inventoryService, packetSender);
	}

	/**
	 * Processa a entrega das recompensas de drop ao jogador que abateu o monstro.
	 */
	public List<DropReward> rewardMonsterDeath(
			PlayerCharacter player,
			int mobId,
			int mobLevel,
			InventoryService inventoryService,
			Consumer<GameServerPacket> packetSender) {

		List<DropReward> rewards = rollDrops(mobId, player != null ? player.level() : 0, mobLevel);
		if (rewards.isEmpty()) {
			return List.of();
		}

		List<ItemInfo> itemUpdates = new ArrayList<>();

		for (DropReward reward : rewards) {
			boolean shouldLoot = reward.isAdena() ? Config.AUTO_LOOT_ADENA : (autoLoot && Config.AUTO_LOOT);
			if (shouldLoot && inventoryService != null) {
				try {
					var addResult = inventoryService.addItem(player.inventory(), reward.itemId(), reward.count(), "Drop");
					if (addResult != null) {
						int change = addResult.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED;
						itemUpdates.add(ItemInfo.of(addResult.item(), change));
					}
					if (packetSender != null) {
						if (reward.isAdena()) {
							packetSender.accept(SystemMessage.of(
									SystemMessage.YOU_PICKED_UP_S1_ADENA,
									new SystemMessage.Number(reward.count())));
						} else if (reward.count() > 1) {
							packetSender.accept(SystemMessage.of(
									SystemMessage.YOU_PICKED_UP_S1_S2,
									new SystemMessage.ItemName(reward.itemId()),
									new SystemMessage.Number(reward.count())));
						} else {
							packetSender.accept(SystemMessage.of(
									SystemMessage.YOU_PICKED_UP_S1,
									new SystemMessage.ItemName(reward.itemId())));
						}
					}
				} catch (Exception e) {
					log.warn("Falha ao entregar drop {} x{} para {}: {}",
							reward.itemId(), reward.count(), player.name(), e.getMessage());
				}
			}
		}

		if (packetSender != null) {
			if (!itemUpdates.isEmpty()) {
				packetSender.accept(new InventoryUpdate(itemUpdates));
			}
			if (player.inventory() != null) {
				packetSender.accept(new StatusUpdate(player.objectId(),
						List.of(new StatusUpdate.Attribute(StatusUpdate.CUR_LOAD, player.inventory().currentLoad()))));
			}
		}

		return rewards;
	}

	/**
	 * Sorteia os drops de Spoil de um monstro (categoria < 0).
	 */
	public List<DropReward> rollSpoil(int mobId, int playerLevel, int mobLevel) {
		List<DropData> rules = dropTable.getDrops(mobId);
		if (rules.isEmpty()) {
			return List.of();
		}

		double levelPenalty = 1.0;
		if (Config.getBoolean("UseDeepBlueDropRules", true) && playerLevel > 0 && mobLevel > 0) {
			int diff = playerLevel - mobLevel;
			if (diff >= 9) {
				levelPenalty = Math.max(0.0, 1.0 - ((diff - 8) * 0.2));
			}
		}
		if (levelPenalty <= 0.0) {
			return List.of();
		}

		List<DropReward> rewards = new ArrayList<>();
		ThreadLocalRandom rng = ThreadLocalRandom.current();

		for (DropData rule : rules) {
			if (!rule.isSpoil()) {
				continue;
			}

			double rate = (rateSpoil > 0 ? rateSpoil : Config.RATE_DROP_SPOIL) * levelPenalty;
			long effectiveChance = Math.round(rule.chance() * rate);
			if (effectiveChance >= DropData.MAX_CHANCE) {
				double extraMultiplier = (double) effectiveChance / DropData.MAX_CHANCE;
				int baseCount = randomCount(rng, rule.min(), rule.max());
				int finalCount = (int) Math.max(1, Math.round(baseCount * extraMultiplier));
				rewards.add(new DropReward(rule.itemId(), finalCount, false));
			} else if (rng.nextInt(DropData.MAX_CHANCE) < effectiveChance) {
				int baseCount = randomCount(rng, rule.min(), rule.max());
				int finalCount = (int) Math.max(1, Math.round(baseCount * rate));
				rewards.add(new DropReward(rule.itemId(), finalCount, false));
			}
		}

		return rewards;
	}

	/**
	 * Colhe os itens de spoil de um monstro abatido (habilidade Sweeper dos Anoes).
	 */
	public boolean sweep(
			PlayerCharacter player,
			NpcInstance npc,
			InventoryService inventoryService,
			Consumer<GameServerPacket> packetSender) {

		if (player == null || npc == null || !npc.isDead() || !npc.isSpoiled()) {
			return false;
		}

		List<DropReward> rewards = npc.spoilRewards();
		if (rewards == null) {
			rewards = rollSpoil(npc.npcId(), player.level(), npc.template() != null ? npc.template().level() : 0);
		}

		// Consome o spoil do monstro
		npc.spoiled(false);
		npc.spoilRewards(null);

		if (rewards == null || rewards.isEmpty()) {
			return true;
		}

		List<ItemInfo> itemUpdates = new ArrayList<>();
		for (DropReward reward : rewards) {
			if (inventoryService != null) {
				try {
					var addResult = inventoryService.addItem(player.inventory(), reward.itemId(), reward.count(), "Sweep");
					if (addResult != null) {
						int change = addResult.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED;
						itemUpdates.add(ItemInfo.of(addResult.item(), change));
					}
					if (packetSender != null) {
						if (reward.isAdena()) {
							packetSender.accept(SystemMessage.of(
									SystemMessage.YOU_PICKED_UP_S1_ADENA,
									new SystemMessage.Number(reward.count())));
						} else if (reward.count() > 1) {
							packetSender.accept(SystemMessage.of(
									SystemMessage.YOU_PICKED_UP_S1_S2,
									new SystemMessage.ItemName(reward.itemId()),
									new SystemMessage.Number(reward.count())));
						} else {
							packetSender.accept(SystemMessage.of(
									SystemMessage.YOU_PICKED_UP_S1,
									new SystemMessage.ItemName(reward.itemId())));
						}
					}
				} catch (Exception e) {
					log.warn("Falha ao entregar sweep {} x{} para {}: {}",
							reward.itemId(), reward.count(), player.name(), e.getMessage());
				}
			}
		}

		if (packetSender != null) {
			if (!itemUpdates.isEmpty()) {
				packetSender.accept(new InventoryUpdate(itemUpdates));
			}
			if (player.inventory() != null) {
				packetSender.accept(new StatusUpdate(player.objectId(),
						List.of(new StatusUpdate.Attribute(StatusUpdate.CUR_LOAD, player.inventory().currentLoad()))));
			}
		}

		return true;
	}

	private static int randomCount(ThreadLocalRandom rng, int min, int max) {
		if (min >= max) {
			return min;
		}
		return rng.nextInt(min, max + 1);
	}
}
