package com.lopez.l2j.game.event.official;

import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemRepository;
import com.lopez.l2j.game.item.ItemTemplateTable;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Servico dos Eventos Oficiais Retail do Lineage II (Interlude).
 * Suporta ativacao/desativacao dinamica, tabela de drops de eventos (Big Squash, Christmas,
 * Event Medals, L2Day) e troca de itens por recompensas nos NPCs Event Managers (Roy the Cat, Winnie, etc).
 */
public class OfficialEventService {

	private static final Logger log = LoggerFactory.getLogger(OfficialEventService.class);

	public record RolledDrop(int itemId, int count) {}

	private final Map<OfficialEventType, OfficialEventDef> events = new ConcurrentHashMap<>();
	private final ItemRepository itemRepository;
	private final ItemTemplateTable itemTable;
	private final ObjectIdFactory idFactory;

	public OfficialEventService(ItemRepository itemRepository, ItemTemplateTable itemTable, ObjectIdFactory idFactory) {
		this.itemRepository = itemRepository;
		this.itemTable = itemTable;
		this.idFactory = idFactory != null ? idFactory : ObjectIdFactory.sequential(0x55000000);
		initializeDefaultEvents();
	}

	public OfficialEventService() {
		this(null, null, null);
	}

	private void initializeDefaultEvents() {
		// 1. Big Squash Event
		List<EventDropEntry> squashDrops = List.of(
				new EventDropEntry(6391, 1, 3, 15.0, 20), // Nectar
				new EventDropEntry(6389, 1, 1, 5.0, 20)   // Squash Seed
		);
		List<EventRewardExchange> squashExchanges = List.of(
				new EventRewardExchange(101, "Large Squash Seed", Map.of(6391, 10), Map.of(6390, 1)),
				new EventRewardExchange(102, "Chrono Cithara Musical Weapon", Map.of(6391, 50), Map.of(4202, 1))
		);
		events.put(OfficialEventType.BIG_SQUASH, new OfficialEventDef(
				OfficialEventType.BIG_SQUASH, "Big Squash", "Smash watermelons and gourds for treasures",
				OfficialEventState.ACTIVE, squashDrops, squashExchanges));

		// 2. Christmas Event
		List<EventDropEntry> christmasDrops = List.of(
				new EventDropEntry(5556, 1, 1, 10.0, 20), // Star Ornament
				new EventDropEntry(5557, 1, 1, 10.0, 20), // Bead Ornament
				new EventDropEntry(5558, 1, 1, 10.0, 20), // Fir Tree
				new EventDropEntry(5559, 1, 1, 10.0, 20)  // Flower Pot
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
				OfficialEventState.INACTIVE, christmasDrops, christmasExchanges));

		// 3. Event Medals
		List<EventDropEntry> medalDrops = List.of(
				new EventDropEntry(6392, 1, 2, 20.0, 20), // Medal
				new EventDropEntry(6393, 1, 1, 5.0, 30)   // Glittering Medal
		);
		List<EventRewardExchange> medalExchanges = List.of(
				new EventRewardExchange(301, "10x Greater Haste Potions", Map.of(6392, 50), Map.of(1062, 10)),
				new EventRewardExchange(302, "5x Blessed Scroll of Escape", Map.of(6393, 20), Map.of(1538, 5)),
				new EventRewardExchange(303, "5x Coin of Luck", Map.of(6392, 500, 6393, 50), Map.of(4037, 5))
		);
		events.put(OfficialEventType.EVENT_MEDALS, new OfficialEventDef(
				OfficialEventType.EVENT_MEDALS, "Event Medals", "Collect collector and glittering medals for Roy the Cat",
				OfficialEventState.ACTIVE, medalDrops, medalExchanges));
	}

	public void setEventState(OfficialEventType type, OfficialEventState state) {
		OfficialEventDef def = events.get(type);
		if (def != null) {
			def.setState(state);
			log.info("Official event {} state changed to {}", def.getName(), state);
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

	public synchronized boolean exchangeReward(PlayerCharacter player, int exchangeId, Map<Integer, Integer> playerInventoryCounts) {
		Optional<EventRewardExchange> opt = findExchange(exchangeId);
		if (opt.isEmpty()) {
			return false;
		}

		EventRewardExchange exchange = opt.get();
		// Validate player has required items
		for (Map.Entry<Integer, Integer> req : exchange.requiredItems().entrySet()) {
			int owned = playerInventoryCounts.getOrDefault(req.getKey(), 0);
			if (owned < req.getValue()) {
				return false; // Not enough items
			}
		}

		// Deduct required items from map
		for (Map.Entry<Integer, Integer> req : exchange.requiredItems().entrySet()) {
			playerInventoryCounts.put(req.getKey(), playerInventoryCounts.get(req.getKey()) - req.getValue());
		}

		// Award reward items
		if (itemRepository != null && itemTable != null) {
			for (Map.Entry<Integer, Integer> reward : exchange.rewardItems().entrySet()) {
				itemTable.get(reward.getKey()).ifPresent(tmpl -> {
					ItemInstance item = new ItemInstance(idFactory.nextId(), tmpl, player.objectId(), reward.getValue());
					itemRepository.insert(item, "EventExchangeReward");
				});
			}
		}

		log.info("Player {} (ID: {}) exchanged event reward #{} ({})",
				player.name(), player.objectId(), exchangeId, exchange.name());
		return true;
	}

	public String generateHtml(PlayerCharacter player) {
		StringBuilder sb = new StringBuilder();
		sb.append("<html><title>Official Events Manager</title><body><center>");
		sb.append("<font color=\"LEVEL\">=== Lineage II Official Events ===</font><br><br>");

		for (OfficialEventDef def : events.values()) {
			boolean active = def.getState() == OfficialEventState.ACTIVE;
			sb.append("<table width=280 bgcolor=000000><tr>");
			sb.append("<td><font color=\"").append(active ? "00FF00" : "FF0000").append("\">")
					.append(def.getName()).append(" [").append(def.getState()).append("]</font><br1>");
			sb.append("<font color=\"B09878\">").append(def.getDescription()).append("</font></td>");
			sb.append("</tr></table><br1>");

			if (active && !def.getExchanges().isEmpty()) {
				sb.append("<table width=280 border=0>");
				for (EventRewardExchange ex : def.getExchanges()) {
					sb.append("<tr>");
					sb.append("<td><font color=\"FFFFFF\">").append(ex.name()).append("</font></td>");
					sb.append("<td align=right><button value=\"Exchange\" action=\"bypass -h event_exchange ")
							.append(ex.exchangeId()).append("\" width=65 height=20 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
					sb.append("</tr>");
				}
				sb.append("</table><br>");
			}
		}
		sb.append("</center></body></html>");
		return sb.toString();
	}
}
