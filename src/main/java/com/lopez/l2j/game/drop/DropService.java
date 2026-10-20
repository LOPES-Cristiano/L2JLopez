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
	private final com.lopez.l2j.game.champion.ChampionService championService;
	private final com.lopez.l2j.game.item.GroundItemService groundItemService;
	private com.lopez.l2j.game.event.official.OfficialEventService officialEventService;
	private com.lopez.l2j.game.event.official.L2DayEventService l2DayEventService;

	public DropService(
			DropTable dropTable,
			@Value("${l2.rates.adena:1.0}") double rateAdena,
			@Value("${l2.rates.drop:1.0}") double rateDrop,
			@Value("${l2.rates.spoil:1.0}") double rateSpoil,
			@Value("${l2.game.autoloot:true}") boolean autoLoot) {
		this(dropTable, rateAdena, rateDrop, rateSpoil, autoLoot, null, null, null, null);
	}

	public DropService(
			DropTable dropTable,
			double rateAdena,
			double rateDrop,
			double rateSpoil,
			boolean autoLoot,
			com.lopez.l2j.game.champion.ChampionService championService) {
		this(dropTable, rateAdena, rateDrop, rateSpoil, autoLoot, championService, null, null, null);
	}

	public DropService(
			DropTable dropTable,
			double rateAdena,
			double rateDrop,
			double rateSpoil,
			boolean autoLoot,
			com.lopez.l2j.game.champion.ChampionService championService,
			com.lopez.l2j.game.item.GroundItemService groundItemService) {
		this(dropTable, rateAdena, rateDrop, rateSpoil, autoLoot, championService, groundItemService, null, null);
	}

	@org.springframework.beans.factory.annotation.Autowired
	public DropService(
			DropTable dropTable,
			@Value("${l2.rates.adena:1.0}") double rateAdena,
			@Value("${l2.rates.drop:1.0}") double rateDrop,
			@Value("${l2.rates.spoil:1.0}") double rateSpoil,
			@Value("${l2.game.autoloot:true}") boolean autoLoot,
			@org.springframework.beans.factory.annotation.Autowired(required = false) com.lopez.l2j.game.champion.ChampionService championService,
			@org.springframework.beans.factory.annotation.Autowired(required = false) com.lopez.l2j.game.item.GroundItemService groundItemService,
			@org.springframework.beans.factory.annotation.Autowired(required = false) com.lopez.l2j.game.event.official.OfficialEventService officialEventService,
			@org.springframework.beans.factory.annotation.Autowired(required = false) com.lopez.l2j.game.event.official.L2DayEventService l2DayEventService) {
		this.dropTable = dropTable;
		this.rateAdena = Math.max(0.1, rateAdena);
		this.rateDrop = Math.max(0.1, rateDrop);
		this.rateSpoil = Math.max(0.1, rateSpoil);
		this.autoLoot = autoLoot;
		this.championService = championService;
		this.groundItemService = groundItemService;
		this.officialEventService = officialEventService;
		this.l2DayEventService = l2DayEventService;
	}

	public void setOfficialEventService(com.lopez.l2j.game.event.official.OfficialEventService officialEventService) {
		this.officialEventService = officialEventService;
	}

	public void setL2DayEventService(com.lopez.l2j.game.event.official.L2DayEventService l2DayEventService) {
		this.l2DayEventService = l2DayEventService;
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
		return rollDrops(mobId, playerLevel, mobLevel, false);
	}

	public List<DropReward> rollDrops(int mobId, int playerLevel, int mobLevel, boolean isRaid) {
		List<DropData> rules = dropTable.getDrops(mobId);
		boolean hasEventServices = officialEventService != null || l2DayEventService != null;
		if (rules.isEmpty() && !hasEventServices) {
			return List.of();
		}

		double levelPenalty = 1.0;
		if (playerLevel > 0 && mobLevel > 0) {
			int diff = playerLevel - mobLevel;
			if (isRaid) {
				if (diff > Config.RAID_MAX_LEVEL_DIFF) {
					return List.of();
				}
				if (diff > Config.DEEP_BLUE_DROP_RAID_MAX_DIFF) {
					levelPenalty = Math.max(0.1, 1.0 - ((diff - Config.DEEP_BLUE_DROP_RAID_MAX_DIFF) * 0.15));
				}
			} else if (Config.USE_DEEP_BLUE_DROP_RULES && diff >= 9) {
				levelPenalty = Math.max(0.0, 1.0 - ((diff - 8) * 0.2));
			}
		}
		if (levelPenalty <= 0.0) {
			return List.of();
		}

		double raidRateMult = isRaid ? Math.max(0.1, Config.RATE_RAID_DROP_ITEMS * Math.max(0.1, Config.ITEMS_RAID)) : 1.0;
		List<DropReward> rewards = new ArrayList<>();
		ThreadLocalRandom rng = ThreadLocalRandom.current();
		boolean precise = Config.PRECISE_DROP_CALCULATION;

		for (DropData rule : rules) {
			if (rule.isSpoil()) {
				// Spoil e tratado separadamente via habilidade Sweeper/Spoil
				continue;
			}

			if (rule.isAdena()) {
				// Calculo de Adena
				double adenaRaidMult = isRaid ? Math.max(0.1, Config.ADENA_RAID) : 1.0;
				double rate = (rateAdena > 0 ? rateAdena : Config.RATE_DROP_ADENA) * adenaRaidMult * levelPenalty;
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
			} else if (isSealStone(rule.itemId())) {
				// Calculo de Seal Stones (Catacomb/Necropolis: 6360 Blue, 6361 Green, 6362 Red)
				double rate = Math.max(0.1, Config.RATE_DROP_SEAL_STONES) * levelPenalty;
				long effectiveChance = Math.round(rule.chance() * rate);
				if (effectiveChance >= DropData.MAX_CHANCE || rng.nextInt(DropData.MAX_CHANCE) < effectiveChance) {
					int baseCount = randomCount(rng, rule.min(), rule.max());
					int finalCount = (int) Math.max(1, Math.round(baseCount * rate));
					rewards.add(new DropReward(rule.itemId(), finalCount, false));
				}
			} else if (isHerbItem(rule.itemId())) {
				// Calculo de Herbs de acordo com a categoria
				double rate = resolveHerbRate(rule.itemId()) * levelPenalty;
				if (rate > 0) {
					long effectiveChance = Math.round(rule.chance() * rate);
					if (effectiveChance >= DropData.MAX_CHANCE || rng.nextInt(DropData.MAX_CHANCE) < effectiveChance) {
						int count = randomCount(rng, rule.min(), rule.max());
						rewards.add(new DropReward(rule.itemId(), count, false));
					}
				}
			} else {
				// Calculo de Itens / Materiais / Equipamentos
				double specificMult = isBossJewel(rule.itemId()) ? Math.max(0.1, Config.JEWEL_BOSS) : raidRateMult;
				double rate = (rateDrop > 0 ? rateDrop : Config.RATE_DROP_ITEMS) * specificMult * levelPenalty;
				if (precise) {
					double prob = ((double) rule.chance() * rate) / DropData.MAX_CHANCE;
					int guaranteed = (int) prob;
					double remainder = prob - guaranteed;
					int totalMultiplier = guaranteed + (rng.nextDouble() < remainder ? 1 : 0);
					if (totalMultiplier > 0) {
						int baseCount = randomCount(rng, rule.min(), rule.max());
						rewards.add(new DropReward(rule.itemId(), baseCount * totalMultiplier, false));
					}
				} else {
					long effectiveChance = Math.round(rule.chance() * rate);
					if (effectiveChance >= DropData.MAX_CHANCE || rng.nextInt(DropData.MAX_CHANCE) < effectiveChance) {
						int count = randomCount(rng, rule.min(), rule.max());
						rewards.add(new DropReward(rule.itemId(), count, false));
					}
				}
			}
		}

		// Drops de Eventos Oficiais e L2Day ativos
		int effectiveMobLevel = mobLevel > 0 ? mobLevel : (playerLevel > 0 ? playerLevel : 20);
		if (officialEventService != null) {
			List<com.lopez.l2j.game.event.official.OfficialEventService.RolledDrop> eventDrops =
					officialEventService.calculateMonsterDrops(effectiveMobLevel);
			for (var drop : eventDrops) {
				if (drop != null && drop.count() > 0) {
					rewards.add(new DropReward(drop.itemId(), drop.count(), drop.itemId() == 57));
				}
			}
		}
		if (l2DayEventService != null && l2DayEventService.isEventActive()) {
			int letterId = l2DayEventService.rollLetterDrop(effectiveMobLevel);
			if (letterId > 0) {
				rewards.add(new DropReward(letterId, 1, false));
			}
		}

		return rewards;
	}

	public static boolean isSealStone(int itemId) {
		return itemId >= 6360 && itemId <= 6362;
	}

	public static boolean isBossJewel(int itemId) {
		return itemId >= 6656 && itemId <= 6662;
	}

	public static boolean isHerbItem(int itemId) {
		return itemId >= 8600 && itemId <= 8614;
	}

	public static double resolveHerbRate(int itemId) {
		if (itemId == 8600 || itemId == 8603) {
			return Config.RATE_HP_MP_HERBS;
		}
		if (itemId == 8601 || itemId == 8604) {
			return Config.RATE_GREATER_HERBS;
		}
		if (itemId == 8602 || itemId == 8605) {
			return Config.RATE_SUPERIOR_HERBS;
		}
		if (itemId >= 8612 && itemId <= 8614) {
			return Config.RATE_SPECIAL_HERBS;
		}
		return Config.RATE_COMMON_HERBS;
	}

	/**
	 * Retorna o multiplicador especifico de drop/adena/spoil para GrandBoss, RaidBoss ou Minion.
	 */
	public double resolveNpcTypeDropMultiplier(NpcInstance npc, boolean isAdena, boolean isSpoil, int itemId) {
		if (npc == null || npc.template() == null) {
			return 1.0;
		}
		if (isBossJewel(itemId)) {
			return Math.max(0.1, Config.JEWEL_BOSS);
		}
		if (npc.template().isGrandBoss()) {
			if (isAdena) return Math.max(0.1, Config.ADENA_BOSS);
			if (isSpoil) return Math.max(0.1, Config.SPOIL_BOSS);
			return Math.max(0.1, Config.ITEMS_BOSS);
		}
		if (npc.template().isRaidBoss()) {
			if (isAdena) return Math.max(0.1, Config.ADENA_RAID);
			if (isSpoil) return Math.max(0.1, Config.SPOIL_RAID);
			return Math.max(0.1, Config.ITEMS_RAID);
		}
		if ("L2Minion".equalsIgnoreCase(npc.template().type()) || "L2RaidMinion".equalsIgnoreCase(npc.template().type())) {
			if (isAdena) return Math.max(0.1, Config.ADENA_MINON);
			if (isSpoil) return Math.max(0.1, Config.SPOIL_MINON);
			return Math.max(0.1, Config.ITEMS_MINON);
		}
		return 1.0;
	}

	/**
	 * Verifica se um jogador sem privilegio de GM pode inspecionar status/drops de NPC (Shift+Click).
	 */
	public boolean canViewNpcStats(PlayerCharacter player) {
		if (player != null && player.accessLevel() > 0) {
			return true;
		}
		return Config.ALT_GAME_VIEW_NPC;
	}

	public boolean canViewNpcDrops(PlayerCharacter player) {
		if (player != null && player.accessLevel() > 0) {
			return true;
		}
		return Config.ALT_GAME_VIEW_NPC && Config.ALT_GAME_VIEW_NPC_DROP;
	}

	/**
	 * Calcula a perda de itens do inventario de um jogador PK (com Karma > 0) ao morrer.
	 * Respeita CAN_GM_DROP_EQUIPMENT, MINIMUM_PK_REQUIRED_TO_DROP, LIST_OF_PET_ITEMS,
	 * LIST_OF_NON_DROPPABLE_ITEMS, KARMA_DROP_LIMIT, KARMA_RATE_DROP, KARMA_RATE_DROP_ITEM,
	 * KARMA_RATE_DROP_EQUIP e KARMA_RATE_DROP_EQUIP_WEAPON.
	 */
	public List<com.lopez.l2j.game.item.ItemInstance> calculatePkKarmaDrops(
			PlayerCharacter pkPlayer,
			boolean killedByPlayable,
			InventoryService inventoryService) {
		if (pkPlayer == null || pkPlayer.karma() <= 0 || pkPlayer.inventory() == null) {
			return List.of();
		}
		if (pkPlayer.accessLevel() > 0 && !Config.CAN_GM_DROP_EQUIPMENT) {
			return List.of();
		}
		if (pkPlayer.pkKills() < Config.MINIMUM_PK_REQUIRED_TO_DROP) {
			return List.of();
		}
		int dropLimit = Math.max(0, Config.KARMA_DROP_LIMIT);
		if (dropLimit == 0) {
			return List.of();
		}
		ThreadLocalRandom rng = ThreadLocalRandom.current();
		if (rng.nextInt(100) >= Config.KARMA_RATE_DROP) {
			return List.of();
		}

		List<com.lopez.l2j.game.item.ItemInstance> dropped = new ArrayList<>();
		for (var item : pkPlayer.inventory().items()) {
			if (dropped.size() >= dropLimit) {
				break;
			}
			int itemId = item.itemId();
			if (itemId == 57) {
				continue; // Adena nunca cai no PK drop
			}
			if (Config.LIST_OF_PET_ITEMS != null && Config.LIST_OF_PET_ITEMS.contains(itemId)) {
				continue;
			}
			if (Config.LIST_OF_NON_DROPPABLE_ITEMS != null && Config.LIST_OF_NON_DROPPABLE_ITEMS.contains(itemId)) {
				continue;
			}
			var tpl = inventoryService != null ? inventoryService.templates().get(itemId).orElse(null) : null;
			if (tpl != null && (!tpl.dropable() || tpl.isQuest())) {
				continue;
			}

			int chancePercent;
			if (item.isEquipped()) {
				boolean isWeapon = tpl != null && tpl.isWeapon();
				chancePercent = isWeapon ? Config.KARMA_RATE_DROP_EQUIP_WEAPON : Config.KARMA_RATE_DROP_EQUIP;
			} else {
				chancePercent = Config.KARMA_RATE_DROP_ITEM;
			}

			if (chancePercent > 0 && rng.nextInt(100) < chancePercent) {
				dropped.add(item);
			}
		}
		return dropped;
	}

	public List<DropReward> rewardMonsterDeath(
			PlayerCharacter player,
			int mobId,
			InventoryService inventoryService,
			Consumer<GameServerPacket> packetSender) {
		return rewardMonsterDeath(player, mobId, 0, inventoryService, packetSender);
	}

	public List<DropReward> rewardMonsterDeath(
			PlayerCharacter player,
			NpcInstance npc,
			InventoryService inventoryService,
			Consumer<GameServerPacket> packetSender) {
		if (npc == null) {
			return List.of();
		}
		int mobId = npc.npcId();
		int mobLevel = npc.template() != null ? npc.template().level() : 0;
		boolean isRaid = npc.template() != null && (npc.template().isRaidBoss() || npc.template().isGrandBoss());
		List<DropReward> rewards = rollDrops(mobId, player != null ? player.level() : 0, mobLevel, isRaid);
		double npcMult = resolveNpcTypeDropMultiplier(npc, false, false, 0);
		if (npcMult != 1.0 && !isRaid) {
			List<DropReward> scaled = new ArrayList<>(rewards.size());
			for (DropReward r : rewards) {
				double m = resolveNpcTypeDropMultiplier(npc, r.isAdena(), false, r.itemId());
				scaled.add(new DropReward(r.itemId(), Math.max(1, (int) Math.round(r.count() * m)), r.isAdena()));
			}
			rewards = scaled;
		}
		if (npc.isChampion() && championService != null) {
			rewards = championService.applyDropMultipliers(npc, rewards, player != null ? player.level() : 0);
		}
		return deliverRewards(player, rewards, inventoryService, packetSender, npc.x(), npc.y(), npc.z(), isRaid);
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

		List<DropReward> rewards = rollDrops(mobId, player != null ? player.level() : 0, mobLevel, false);
		int px = player != null ? player.x() : 0;
		int py = player != null ? player.y() : 0;
		int pz = player != null ? player.z() : 0;
		return deliverRewards(player, rewards, inventoryService, packetSender, px, py, pz, false);
	}

	private List<DropReward> deliverRewards(
			PlayerCharacter player,
			List<DropReward> rewards,
			InventoryService inventoryService,
			Consumer<GameServerPacket> packetSender,
			int dropX,
			int dropY,
			int dropZ,
			boolean isRaid) {

		if (rewards.isEmpty()) {
			return List.of();
		}

		List<ItemInfo> itemUpdates = new ArrayList<>();

		for (DropReward reward : rewards) {
			boolean isHerb = isHerbItem(reward.itemId());
			boolean shouldLoot;
			if (isHerb) {
				shouldLoot = Config.AUTO_LOOT_HERBS;
			} else if (isRaid) {
				shouldLoot = Config.AUTO_LOOT_RAID;
			} else if (reward.isAdena()) {
				shouldLoot = Config.AUTO_LOOT_ADENA;
			} else {
				shouldLoot = autoLoot && (Config.AUTO_LOOT || Config.AUTO_LOOT_DEFAULT);
			}
			boolean hasSpace = true;
			if (player != null && player.inventory() != null) {
				boolean alreadyOwned = player.inventory().byItemId(reward.itemId()).isPresent();
				var tpl = inventoryService != null ? inventoryService.templates().get(reward.itemId()).orElse(null) : null;
				boolean stackable = tpl != null && tpl.isStackable();
				if ((!stackable || !alreadyOwned) && player.inventory().size() >= player.maxInventorySlots()) {
					hasSpace = false;
				}
			}

			if (shouldLoot && hasSpace && inventoryService != null) {
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
			} else {
				String behavior = Config.PICKUP_FULL_INVENTORY;
				if (!hasSpace && "destroy".equalsIgnoreCase(behavior)) {
					continue;
				}
				if (groundItemService != null) {
					groundItemService.dropItem(0, reward.itemId(), reward.count(), dropX, dropY, dropZ);
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
		return rollSpoil(mobId, playerLevel, mobLevel, 1.0);
	}

	public List<DropReward> rollSpoil(int mobId, int playerLevel, int mobLevel, double spoilTypeMult) {
		List<DropData> rules = dropTable.getDrops(mobId);
		if (rules.isEmpty()) {
			return List.of();
		}

		double levelPenalty = 1.0;
		if (Config.USE_DEEP_BLUE_DROP_RULES && playerLevel > 0 && mobLevel > 0) {
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

			double effectiveRate = Config.RATE_DROP_SPOIL > 1.0f ? Config.RATE_DROP_SPOIL : (rateSpoil > 0 ? rateSpoil : Config.RATE_DROP_SPOIL);
			double rate = effectiveRate * Math.max(0.1, spoilTypeMult) * levelPenalty;
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

		if (player == null || npc == null || !npc.isDead() || !npc.isSweepable()) {
			return false;
		}

		List<DropReward> rewards = npc.spoilRewards();
		if (rewards == null || rewards.isEmpty()) {
			npc.spoiled(false);
			npc.spoilRewards(null);
			return false;
		}

		// Consome o spoil do monstro
		npc.spoiled(false);
		npc.spoilRewards(null);

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
						if (reward.count() > 1) {
							packetSender.accept(SystemMessage.of(
									SystemMessage.S1_SWEEPED_UP_S3_S2,
									new SystemMessage.Text(player.name()),
									new SystemMessage.Number(reward.count()),
									new SystemMessage.ItemName(reward.itemId())));
						} else {
							packetSender.accept(SystemMessage.of(
									SystemMessage.S1_SWEEPED_UP_S2,
									new SystemMessage.Text(player.name()),
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
