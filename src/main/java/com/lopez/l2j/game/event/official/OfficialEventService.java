package com.lopez.l2j.game.event.official;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemRepository;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.item.ItemTemplateTable;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Servico dos Eventos Oficiais Retail do Lineage II (Interlude).
 * Suporta ativacao/desativacao dinamica, tabela de drops de eventos (Big Squash, Christmas,
 * Event Medals, L2Day, Starlight Festival, L2DropDay) e troca de itens por recompensas nos NPCs Event Managers.
 */
@Service
public class OfficialEventService {

	private static final Logger log = LoggerFactory.getLogger(OfficialEventService.class);

	public record RolledDrop(int itemId, int count) {}

	public record ExchangeResult(
			boolean success,
			EventRewardExchange exchange,
			Map<Integer, Integer> rewardedItems,
			String message
	) {
		public boolean ok() {
			return success;
		}

		public static ExchangeResult success(EventRewardExchange exchange) {
			return new ExchangeResult(true, exchange,
					exchange != null ? exchange.rewardItems() : Map.of(),
					"Exchanged reward: " + (exchange != null ? exchange.name() : ""));
		}

		public static ExchangeResult failure(String message) {
			return new ExchangeResult(false, null, Map.of(), message);
		}
	}

	private final Map<OfficialEventType, OfficialEventDef> events = new ConcurrentHashMap<>();
	private final InventoryService inventoryService;
	private final ItemRepository itemRepository;
	private final ItemTemplateTable itemTable;
	private final ObjectIdFactory idFactory;

	@Autowired
	public OfficialEventService(
			@Autowired(required = false) InventoryService inventoryService,
			@Autowired(required = false) ItemRepository itemRepository,
			@Autowired(required = false) ObjectIdFactory idFactory,
			@Autowired(required = false) ItemTemplateTable itemTable) {
		this.inventoryService = inventoryService;
		this.itemRepository = itemRepository;
		this.itemTable = itemTable != null ? itemTable : (inventoryService != null ? inventoryService.templates() : null);
		this.idFactory = idFactory != null ? idFactory : ObjectIdFactory.sequential(0x55000000);
		initializeDefaultEvents();
	}

	public OfficialEventService(ItemRepository itemRepository, ItemTemplateTable itemTable, ObjectIdFactory idFactory) {
		this(null, itemRepository, idFactory, itemTable);
	}

	public OfficialEventService() {
		this(null, null, null, null);
	}

	private void initializeDefaultEvents() {
		// 1. Big Squash Event
		double squashSeedChance = Config.BIG_SQUASH_DROP_CHANCE > 0 ? Config.BIG_SQUASH_DROP_CHANCE : 5.0;
		double nectarChance = Math.max(15.0, squashSeedChance * 3.0);
		List<EventDropEntry> squashDrops = new ArrayList<>();
		squashDrops.add(new EventDropEntry(6391, 1, 3, nectarChance, 20)); // Nectar
		if (Config.BIG_SQUASH_USE_SEEDS) {
			squashDrops.add(new EventDropEntry(6389, 1, 1, squashSeedChance, 20)); // Squash Seed
		}
		List<EventRewardExchange> squashExchanges = List.of(
				new EventRewardExchange(101, "Large Squash Seed", Map.of(6391, 10), Map.of(6390, 1)),
				new EventRewardExchange(102, "Chrono Cithara Musical Weapon", Map.of(6391, 50), Map.of(4202, 1))
		);
		events.put(OfficialEventType.BIG_SQUASH, new OfficialEventDef(
				OfficialEventType.BIG_SQUASH, "Big Squash", "Smash watermelons and gourds for treasures",
				OfficialEventState.ACTIVE, squashDrops, squashExchanges));

		// 2. Christmas Event
		double xmasChance = Config.CRISTMAS_DROP_CHANCE > 0 ? Config.CRISTMAS_DROP_CHANCE : 10.0;
		List<EventDropEntry> christmasDrops = List.of(
				new EventDropEntry(5556, 1, 1, xmasChance, 20), // Star Ornament
				new EventDropEntry(5557, 1, 1, xmasChance, 20), // Bead Ornament
				new EventDropEntry(5558, 1, 1, xmasChance, 20), // Fir Tree
				new EventDropEntry(5559, 1, 1, xmasChance, 20)  // Flower Pot
		);
		List<EventRewardExchange> christmasExchanges = List.of(
				new EventRewardExchange(201, "Christmas Tree",
						Map.of(5556, 1, 5557, 1, 5558, 1, 5559, 1),
						Map.of(5560, 1)),
				new EventRewardExchange(202, "Special Christmas Tree",
						Map.of(5560, 10),
						Map.of(5561, 1))
		);
		events.put(OfficialEventType.CHRISTMAS, new OfficialEventDef(
				OfficialEventType.CHRISTMAS, "Christmas Festival", "Gather holiday ornaments and grow festive trees",
				Config.CRISTMAS_ADD_DROP ? OfficialEventState.ACTIVE : OfficialEventState.INACTIVE,
				christmasDrops, christmasExchanges));

		// 3. Event Medals
		double medalChance1 = Config.MEDAL_1_DROP_CHANCE > 0 ? Math.max(20.0, Config.MEDAL_1_DROP_CHANCE) : 20.0;
		double medalChance2 = Config.MEDAL_2_DROP_CHANCE > 0 ? Config.MEDAL_2_DROP_CHANCE : 5.0;
		List<EventDropEntry> medalDrops = List.of(
				new EventDropEntry(6392, 1, 2, medalChance1, 20), // Medal
				new EventDropEntry(6393, 1, 1, medalChance2, 30)  // Glittering Medal
		);
		List<EventRewardExchange> medalExchanges = List.of(
				new EventRewardExchange(301, "10x Greater Haste Potions", Map.of(6392, 50), Map.of(1062, 10)),
				new EventRewardExchange(302, "5x Blessed Scroll of Escape", Map.of(6393, 20), Map.of(1538, 5)),
				new EventRewardExchange(303, "5x Coin of Luck", Map.of(6392, 500, 6393, 50), Map.of(4037, 5))
		);
		events.put(OfficialEventType.EVENT_MEDALS, new OfficialEventDef(
				OfficialEventType.EVENT_MEDALS, "Event Medals", "Collect collector and glittering medals for Roy the Cat",
				OfficialEventState.ACTIVE, medalDrops, medalExchanges));

		// 4. Starlight Festival
		double starChance1 = Config.STAR_1_DROP_CHANCE > 0 ? Config.STAR_1_DROP_CHANCE : 10.0;
		double starChance2 = Config.STAR_2_DROP_CHANCE > 0 ? Config.STAR_2_DROP_CHANCE : 5.0;
		double starChance3 = Config.STAR_3_DROP_CHANCE > 0 ? Config.STAR_3_DROP_CHANCE : 2.0;
		List<EventDropEntry> starDrops = List.of(
				new EventDropEntry(6403, 1, 1, starChance1, 20), // Star Shard
				new EventDropEntry(6404, 1, 1, starChance2, 20), // Bead Shard
				new EventDropEntry(6405, 1, 1, starChance3, 20)  // Firework Part
		);
		List<EventRewardExchange> starExchanges = List.of(
				new EventRewardExchange(401, "Elven Firecracker", Map.of(6403, 2, 6404, 2), Map.of(6406, 1)),
				new EventRewardExchange(402, "Large Firecracker", Map.of(6403, 4, 6404, 4, 6405, 1), Map.of(6407, 1))
		);
		events.put(OfficialEventType.STARLIGHT_FESTIVAL, new OfficialEventDef(
				OfficialEventType.STARLIGHT_FESTIVAL, "Starlight Festival", "Collect star shards and craft fireworks",
				Config.STAR_ADD_DROP ? OfficialEventState.ACTIVE : OfficialEventState.INACTIVE,
				starDrops, starExchanges));

		// 5. L2 Day Event
		double l2dayChance = Config.L2_DAY_DROP_CHANCE > 0 ? Config.L2_DAY_DROP_CHANCE : 5.0;
		List<EventDropEntry> l2dayDrops = List.of(
				new EventDropEntry(3875, 1, 1, l2dayChance, 20), // A
				new EventDropEntry(3877, 1, 1, l2dayChance, 20), // E
				new EventDropEntry(3879, 1, 1, l2dayChance, 20), // G
				new EventDropEntry(3881, 1, 1, l2dayChance, 20), // I
				new EventDropEntry(3882, 1, 1, l2dayChance, 20), // L
				new EventDropEntry(3883, 1, 1, l2dayChance, 20), // N
				new EventDropEntry(3888, 1, 1, l2dayChance, 20)  // II
		);
		events.put(OfficialEventType.L2_DAY, new OfficialEventDef(
				OfficialEventType.L2_DAY, "L2 Day", "Collect letters from monsters to form LINEAGEII and NCSOFT",
				Config.L2_DAY_ADD_DROP ? OfficialEventState.ACTIVE : OfficialEventState.INACTIVE,
				l2dayDrops, List.of()));
	}

	public boolean isEventMessagesEnabled() {
		return Config.ENABLE_EVENT_MESSAGE;
	}

	public boolean shouldSpawnEventManagers(OfficialEventType type) {
		return switch (type) {
			case BIG_SQUASH -> Config.BIG_SQUASH_SPAWN_MANAGER;
			case CHRISTMAS -> Config.CRISTMAS_SPAWN_SANTA;
			case EVENT_MEDALS -> Config.MEDAL_SPAWN_MENEGER;
			case STARLIGHT_FESTIVAL -> Config.STAR_SPAWN_MANAGER;
			case L2_DAY -> Config.L2_DAY_SPAWN_MANAGER;
		};
	}

	public boolean isEventDropConfigEnabled(OfficialEventType type) {
		return switch (type) {
			case BIG_SQUASH -> Config.BIG_SQUASH_ADD_DROP;
			case CHRISTMAS -> Config.CRISTMAS_ADD_DROP;
			case EVENT_MEDALS -> Config.MEDAL_ADD_DROP;
			case STARLIGHT_FESTIVAL -> Config.STAR_ADD_DROP;
			case L2_DAY -> Config.L2_DAY_ADD_DROP;
		};
	}

	public boolean isL2DropDayEnabled() {
		return Config.L2_DROP_DAY_ADD_DROP;
	}

	public String getL2DayConfiguredRewardsSummary() {
		return Config.L2_DAY_REWARDS + "|" + Config.L2_DAY_REWARDS_ACCESSORIE + "|" + Config.L2_DAY_REWARDS_SCROLL
				+ "|" + Config.L2_DAY_SCROLL_CHS + "|" + Config.L2_DAY_ENCH_SCROLL_CHS + "|" + Config.L2_DAY_ACC_SCROLL_CHS;
	}

	public int getChristmasTreeLifetimeMinutes() {
		return Math.max(1, Config.CRISTMAS_TREE_LIFE_TIME);
	}

	public List<EventDropEntry> getConfiguredL2DropDayEntries() {
		int[] items = {
				Config.L2_DROP_DAY_ITEM_1, Config.L2_DROP_DAY_ITEM_2, Config.L2_DROP_DAY_ITEM_3,
				Config.L2_DROP_DAY_ITEM_4, Config.L2_DROP_DAY_ITEM_5, Config.L2_DROP_DAY_ITEM_6,
				Config.L2_DROP_DAY_ITEM_7, Config.L2_DROP_DAY_ITEM_8, Config.L2_DROP_DAY_ITEM_9,
				Config.L2_DROP_DAY_ITEM_10, Config.L2_DROP_DAY_ITEM_11, Config.L2_DROP_DAY_ITEM_12,
				Config.L2_DROP_DAY_ITEM_13, Config.L2_DROP_DAY_ITEM_14, Config.L2_DROP_DAY_ITEM_15,
				Config.L2_DROP_DAY_ITEM_16, Config.L2_DROP_DAY_ITEM_17, Config.L2_DROP_DAY_ITEM_18,
				Config.L2_DROP_DAY_ITEM_19, Config.L2_DROP_DAY_ITEM_20, Config.L2_DROP_DAY_ITEM_21,
				Config.L2_DROP_DAY_ITEM_22
		};
		int[] chances = {
				Config.L2_DROP_DAY_CHANCE_1, Config.L2_DROP_DAY_CHANCE_2, Config.L2_DROP_DAY_CHANCE_3,
				Config.L2_DROP_DAY_CHANCE_4, Config.L2_DROP_DAY_CHANCE_5, Config.L2_DROP_DAY_CHANCE_6,
				Config.L2_DROP_DAY_CHANCE_7, Config.L2_DROP_DAY_CHANCE_8, Config.L2_DROP_DAY_CHANCE_9,
				Config.L2_DROP_DAY_CHANCE_10, Config.L2_DROP_DAY_CHANCE_11, Config.L2_DROP_DAY_CHANCE_12,
				Config.L2_DROP_DAY_CHANCE_13, Config.L2_DROP_DAY_CHANCE_14, Config.L2_DROP_DAY_CHANCE_15,
				Config.L2_DROP_DAY_CHANCE_16, Config.L2_DROP_DAY_CHANCE_17, Config.L2_DROP_DAY_CHANCE_18,
				Config.L2_DROP_DAY_CHANCE_19, Config.L2_DROP_DAY_CHANCE_20, Config.L2_DROP_DAY_CHANCE_21,
				Config.L2_DROP_DAY_CHANCE_22
		};
		List<EventDropEntry> entries = new ArrayList<>();
		for (int i = 0; i < items.length; i++) {
			if (items[i] > 0 && chances[i] > 0) {
				entries.add(new EventDropEntry(items[i], 1, 1, chances[i], 20));
			}
		}
		return entries;
	}

	public void setEventState(OfficialEventType type, OfficialEventState state) {
		OfficialEventDef def = events.get(type);
		if (def != null) {
			def.setState(state);
			log.info("Official event {} state changed to {}", def.getName(), state);
			if (isEventMessagesEnabled()) {
				GameWorld world = GameWorld.getInstance();
				if (world != null) {
					CreatureSay msg = new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Event",
							"Official Event '" + def.getName() + "' is now " + state + "!");
					for (var online : world.onlinePlayers()) {
						online.send(msg);
					}
				}
			}
		}
	}

	public boolean isEventActive(OfficialEventType type) {
		OfficialEventDef def = events.get(type);
		return def != null && def.getState() == OfficialEventState.ACTIVE;
	}

	public OfficialEventDef getEvent(OfficialEventType type) {
		return events.get(type);
	}

	public Map<OfficialEventType, OfficialEventDef> getEvents() {
		return Collections.unmodifiableMap(events);
	}

	public List<RolledDrop> calculateMonsterDrops(int monsterLevel) {
		List<RolledDrop> drops = new ArrayList<>();
		for (OfficialEventDef def : events.values()) {
			if (def.getState() != OfficialEventState.ACTIVE) {
				continue;
			}
			for (EventDropEntry entry : def.getDrops()) {
				if (monsterLevel < entry.minMonsterLevel()) {
					continue;
				}
				double roll = ThreadLocalRandom.current().nextDouble() * 100.0;
				if (roll < entry.chancePercent()) {
					int count = entry.minCount();
					if (entry.maxCount() > entry.minCount()) {
						count += ThreadLocalRandom.current().nextInt(entry.maxCount() - entry.minCount() + 1);
					}
					drops.add(new RolledDrop(entry.itemId(), count));
				}
			}
		}
		if (Config.L2_DROP_DAY_ADD_DROP) {
			for (EventDropEntry entry : getConfiguredL2DropDayEntries()) {
				if (monsterLevel < entry.minMonsterLevel()) {
					continue;
				}
				double roll = ThreadLocalRandom.current().nextDouble() * 100.0;
				if (roll < entry.chancePercent()) {
					drops.add(new RolledDrop(entry.itemId(), 1));
				}
			}
		}
		return drops;
	}

	public Optional<EventRewardExchange> findExchange(int exchangeId) {
		for (OfficialEventDef def : events.values()) {
			for (EventRewardExchange exchange : def.getExchanges()) {
				if (exchange.exchangeId() == exchangeId) {
					return Optional.of(exchange);
				}
			}
		}
		return Optional.empty();
	}

	public synchronized ExchangeResult exchangeReward(PlayerCharacter player, int exchangeId) {
		return exchangeReward(player, null, String.valueOf(exchangeId));
	}

	public synchronized ExchangeResult exchangeReward(PlayerCharacter player, String eventId, String rewardId) {
		if (player == null || player.inventory() == null) {
			return ExchangeResult.failure("Player inventory not available.");
		}
		String lookupToken = (rewardId != null && !rewardId.isBlank()) ? rewardId.trim()
				: (eventId != null ? eventId.trim() : "");
		if (lookupToken.isEmpty()) {
			return ExchangeResult.failure("Invalid exchange reward identifier.");
		}

		OfficialEventDef matchedEvent = null;
		EventRewardExchange matchedExchange = null;

		if (eventId != null && !eventId.isBlank() && rewardId != null && !rewardId.isBlank()) {
			for (OfficialEventDef def : events.values()) {
				if (def.getType().name().equalsIgnoreCase(eventId.trim())
						|| def.getName().equalsIgnoreCase(eventId.trim())) {
					matchedEvent = def;
					break;
				}
			}
		}

		if (matchedEvent != null) {
			if (matchedEvent.getState() != OfficialEventState.ACTIVE) {
				return ExchangeResult.failure("Event " + matchedEvent.getName() + " is not currently active.");
			}
			for (EventRewardExchange ex : matchedEvent.getExchanges()) {
				if (String.valueOf(ex.exchangeId()).equalsIgnoreCase(lookupToken)
						|| ex.name().equalsIgnoreCase(lookupToken)) {
					matchedExchange = ex;
					break;
				}
			}
		} else {
			for (OfficialEventDef def : events.values()) {
				for (EventRewardExchange ex : def.getExchanges()) {
					if (String.valueOf(ex.exchangeId()).equalsIgnoreCase(lookupToken)
							|| ex.name().equalsIgnoreCase(lookupToken)) {
						matchedEvent = def;
						matchedExchange = ex;
						break;
					}
				}
				if (matchedExchange != null) {
					break;
				}
			}
		}

		if (matchedExchange == null) {
			return ExchangeResult.failure("Exchange reward not found.");
		}
		if (matchedEvent != null && matchedEvent.getState() != OfficialEventState.ACTIVE) {
			return ExchangeResult.failure("Event " + matchedEvent.getName() + " is not active.");
		}

		for (Map.Entry<Integer, Integer> req : matchedExchange.requiredItems().entrySet()) {
			int owned = player.inventory().getItemCount(req.getKey());
			if (owned < req.getValue()) {
				return ExchangeResult.failure("Insufficient required items for exchange.");
			}
		}

		for (Map.Entry<Integer, Integer> req : matchedExchange.requiredItems().entrySet()) {
			consumeItemReal(player, req.getKey(), req.getValue());
		}

		for (Map.Entry<Integer, Integer> reward : matchedExchange.rewardItems().entrySet()) {
			awardItemReal(player, reward.getKey(), reward.getValue());
		}

		log.info("Player {} (ID: {}) exchanged event reward #{} ({})",
				player.name(), player.objectId(), matchedExchange.exchangeId(), matchedExchange.name());
		return ExchangeResult.success(matchedExchange);
	}

	public synchronized boolean exchangeReward(PlayerCharacter player, int exchangeId, Map<Integer, Integer> playerInventoryCounts) {
		Optional<EventRewardExchange> opt = findExchange(exchangeId);
		if (opt.isEmpty()) {
			return false;
		}

		EventRewardExchange exchange = opt.get();
		// Validate player has required items
		for (Map.Entry<Integer, Integer> req : exchange.requiredItems().entrySet()) {
			int owned = playerInventoryCounts != null
					? playerInventoryCounts.getOrDefault(req.getKey(), 0)
					: (player != null && player.inventory() != null ? player.inventory().getItemCount(req.getKey()) : 0);
			if (owned < req.getValue()) {
				return false; // Not enough items
			}
		}

		// Deduct required items from map and from real inventory if present
		for (Map.Entry<Integer, Integer> req : exchange.requiredItems().entrySet()) {
			if (playerInventoryCounts != null && playerInventoryCounts.containsKey(req.getKey())) {
				playerInventoryCounts.put(req.getKey(), playerInventoryCounts.get(req.getKey()) - req.getValue());
			}
			if (player != null && player.inventory() != null
					&& player.inventory().getItemCount(req.getKey()) >= req.getValue()) {
				consumeItemReal(player, req.getKey(), req.getValue());
			}
		}

		// Award reward items
		if (player != null) {
			for (Map.Entry<Integer, Integer> reward : exchange.rewardItems().entrySet()) {
				awardItemReal(player, reward.getKey(), reward.getValue());
			}
		}

		if (player != null) {
			log.info("Player {} (ID: {}) exchanged event reward #{} ({})",
					player.name(), player.objectId(), exchangeId, exchange.name());
		}
		return true;
	}

	private void consumeItemReal(PlayerCharacter player, int itemId, int count) {
		if (player == null || player.inventory() == null || count <= 0) {
			return;
		}
		if (inventoryService != null) {
			inventoryService.consumeItem(player.inventory(), itemId, count, "EventExchange");
			return;
		}
		var opt = player.inventory().byItemId(itemId);
		if (opt.isPresent()) {
			ItemInstance item = opt.get();
			if (item.count() <= count) {
				player.inventory().remove(item);
				item.count(0);
				if (itemRepository != null) {
					itemRepository.delete(item.objectId());
				}
			} else {
				item.count(item.count() - count);
				if (itemRepository != null) {
					itemRepository.update(item);
				}
			}
		}
	}

	private void awardItemReal(PlayerCharacter player, int itemId, int count) {
		if (player == null || count <= 0) {
			return;
		}
		if (inventoryService != null && player.inventory() != null) {
			inventoryService.addItem(player.inventory(), itemId, count, "EventExchangeReward");
			return;
		}
		if (itemTable != null) {
			var tmplOpt = itemTable.get(itemId);
			if (tmplOpt.isPresent()) {
				ItemTemplate tmpl = tmplOpt.get();
				if (tmpl.stackable() && player.inventory() != null) {
					var existing = player.inventory().byItemId(itemId);
					if (existing.isPresent()) {
						ItemInstance item = existing.get();
						item.count((int) Math.min(Integer.MAX_VALUE, (long) item.count() + count));
						if (itemRepository != null) {
							itemRepository.update(item);
						}
						return;
					}
				}
				ItemInstance item = new ItemInstance(idFactory.nextId(), tmpl, player.objectId(), count);
				if (player.inventory() != null) {
					player.inventory().add(item);
				}
				if (itemRepository != null) {
					itemRepository.insert(item, "EventExchangeReward");
				}
				return;
			}
		}
		if (player.inventory() != null) {
			player.inventory().addItem(itemId, count);
		}
	}

	public String generateHtml(PlayerCharacter player) {
		StringBuilder sb = new StringBuilder();
		sb.append("<html><title>Official Events Manager</title><body><center>");
		sb.append("<font color=\"LEVEL\">=== Lineage II Official Events ===</font><br><br>");

		for (OfficialEventDef def : events.values()) {
			boolean active = def.getState() == OfficialEventState.ACTIVE;
			sb.append("<table width=280 bgcolor=000000><tr>");
			sb.append("<td><font color=\"").append(active ? "8FA888" : "B86B6B").append("\">")
					.append(def.getName()).append(" [").append(def.getState()).append("]</font><br1>");
			sb.append("<font color=\"B09878\">").append(def.getDescription()).append("</font></td>");
			sb.append("</tr></table><br1>");

			if (active && !def.getExchanges().isEmpty()) {
				sb.append("<table width=280 border=0>");
				for (EventRewardExchange ex : def.getExchanges()) {
					sb.append("<tr>");
					sb.append("<td><font color=\"FFFFFF\">").append(ex.name()).append("</font></td>");
					sb.append("<td align=right><button value=\"Exchange\" action=\"bypass -h event_exchange ")
							.append(ex.exchangeId()).append("\" width=65 height=19 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>");
					sb.append("</tr>");
				}
				sb.append("</table><br>");
			}
		}
		sb.append("</center></body></html>");
		return sb.toString();
	}
}
