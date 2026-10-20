package com.lopez.l2j.game.roulette;

import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemRepository;
import com.lopez.l2j.game.item.ItemTemplateTable;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Servico moderno de Roleta da Sorte e Minigames do Lineage II.
 * Oferece apostas com Adena ou Coin of Luck, sorteio ponderado com chances proporcionais,
 * categorias de raridade (Common, Rare, Epic, Jackpot), protecao contra flood
 * e interfaces visuais HTML.
 */
@Service
public class RouletteService {

	private static final Logger log = LoggerFactory.getLogger(RouletteService.class);

	private final List<RouletteItem> prizePool = new ArrayList<>();
	private final Map<Integer, Long> cooldowns = new ConcurrentHashMap<>();
	private static final long COOLDOWN_MS = 3000L;

	private int defaultCostItemId = 57; // Adena
	private long defaultCostCount = 5_000_000L; // 5M Adena

	private final InventoryService inventoryService;
	private final ItemRepository itemRepository;
	private final ItemTemplateTable itemTable;
	private final ObjectIdFactory idFactory;

	public RouletteService(ItemRepository itemRepository, ItemTemplateTable itemTable, ObjectIdFactory idFactory) {
		this(null, itemRepository, itemTable, idFactory);
	}

	@Autowired
	public RouletteService(
			@Autowired(required = false) InventoryService inventoryService,
			@Autowired(required = false) ItemRepository itemRepository,
			@Autowired(required = false) ItemTemplateTable itemTable,
			@Autowired(required = false) ObjectIdFactory idFactory) {
		this.inventoryService = inventoryService;
		this.itemRepository = itemRepository;
		this.itemTable = itemTable != null ? itemTable : (inventoryService != null ? inventoryService.templates() : null);
		this.idFactory = idFactory != null ? idFactory : ObjectIdFactory.sequential(0x60000000);
		initializeDefaultPrizes();
	}

	public RouletteService() {
		this(null, null, null, null);
	}

	private void initializeDefaultPrizes() {
		// Common (Total weight ~ 60)
		prizePool.add(new RouletteItem(1061, "Greater Healing Potion", 50, 0, 25.0, RouletteRarity.COMMON, "icon.etc_potion_gold_i00"));
		prizePool.add(new RouletteItem(1467, "Soulshot: S Grade", 2000, 0, 20.0, RouletteRarity.COMMON, "icon.etc_spirit_bullet_red_i00"));
		prizePool.add(new RouletteItem(1538, "Blessed Scroll of Escape", 5, 0, 15.0, RouletteRarity.COMMON, "icon.etc_scroll_of_escape_i00"));

		// Rare (Total weight ~ 25)
		prizePool.add(new RouletteItem(6622, "Giant's Codex", 2, 0, 12.0, RouletteRarity.RARE, "icon.etc_giant_book_i00"));
		prizePool.add(new RouletteItem(8752, "High-grade Life Stone: Level 76", 3, 0, 13.0, RouletteRarity.RARE, "icon.etc_life_stone_76_i01"));

		// Epic (Total weight ~ 12)
		prizePool.add(new RouletteItem(8762, "Top-grade Life Stone: Level 76", 2, 0, 7.0, RouletteRarity.EPIC, "icon.etc_life_stone_76_i03"));
		prizePool.add(new RouletteItem(6577, "Blessed Scroll: Enchant Weapon (S)", 1, 0, 5.0, RouletteRarity.EPIC, "icon.etc_blessed_scrl_of_ench_wp_s_i00"));

		// Jackpot (Total weight ~ 3)
		prizePool.add(new RouletteItem(4037, "Coin of Luck", 50, 0, 2.0, RouletteRarity.JACKPOT, "icon.etc_coins_gold_i00"));
		prizePool.add(new RouletteItem(7575, "Draconic Bow +10", 1, 10, 1.0, RouletteRarity.JACKPOT, "icon.weapon_draconic_bow_i00"));
	}

	public List<RouletteItem> getPrizePool() {
		return Collections.unmodifiableList(prizePool);
	}

	public void addPrize(RouletteItem item) {
		prizePool.add(item);
	}

	public int getDefaultCostItemId() {
		return defaultCostItemId;
	}

	public void setDefaultCost(int itemId, long count) {
		this.defaultCostItemId = itemId;
		this.defaultCostCount = count;
	}

	public long getDefaultCostCount() {
		return defaultCostCount;
	}

	public synchronized RouletteSpinResult spin(PlayerCharacter player) {
		return spin(player, defaultCostItemId, defaultCostCount);
	}

	public synchronized RouletteSpinResult spin(PlayerCharacter player, int costItemId, long costCount) {
		Map<Integer, Long> counts = new HashMap<>();
		if (player != null && player.inventory() != null) {
			for (ItemInstance item : player.inventory().items()) {
				counts.merge(item.itemId(), (long) item.count(), Long::sum);
			}
		}
		return spin(player, costItemId, costCount, counts);
	}

	public synchronized RouletteSpinResult spin(PlayerCharacter player, int costItemId, long costCount, Map<Integer, Long> playerInventoryCounts) {
		if (player == null || player.isDead()) {
			return RouletteSpinResult.failure("You cannot spin while dead.");
		}

		long now = System.currentTimeMillis();
		Long lastSpin = cooldowns.get(player.objectId());
		if (lastSpin != null && (now - lastSpin) < COOLDOWN_MS) {
			long waitSec = (COOLDOWN_MS - (now - lastSpin) + 999) / 1000;
			return RouletteSpinResult.failure("Please wait " + waitSec + " seconds before spinning again.");
		}

		long ownedInMap = playerInventoryCounts != null ? playerInventoryCounts.getOrDefault(costItemId, 0L) : 0L;
		long ownedInInv = (player.inventory() != null)
				? player.inventory().byItemId(costItemId).map(i -> (long) i.count()).orElse(0L)
				: 0L;
		long ownedCurrency = Math.max(ownedInMap, ownedInInv);
		if (ownedCurrency < costCount) {
			return RouletteSpinResult.failure("You do not have enough required currency for the roulette.");
		}

		// Deduct currency in map and real inventory/DB
		if (playerInventoryCounts != null) {
			playerInventoryCounts.put(costItemId, Math.max(0L, ownedCurrency - costCount));
		}
		consumeCostFromInventory(player, costItemId, (int) Math.min(Integer.MAX_VALUE, costCount));
		cooldowns.put(player.objectId(), now);

		// Roll prize
		RouletteItem wonItem = selectRandomPrize();
		if (wonItem == null) {
			return RouletteSpinResult.failure("The roulette wheel stopped on nothing.");
		}

		// Award item to both player.inventory() and DB
		awardPrizeToInventoryAndDb(player, wonItem);

		log.info("Player {} (ID: {}) spun roulette and won {} x{} (Rarity: {})",
				player.name(), player.objectId(), wonItem.name(), wonItem.count(), wonItem.rarity());

		return RouletteSpinResult.success(wonItem);
	}

	private void consumeCostFromInventory(PlayerCharacter player, int costItemId, int costCount) {
		if (costCount <= 0 || player == null || player.inventory() == null) {
			return;
		}
		ItemInstance existing = player.inventory().byItemId(costItemId).orElse(null);
		if (existing == null) {
			return;
		}
		if (inventoryService != null && existing.count() >= costCount) {
			inventoryService.consumeItem(player.inventory(), costItemId, costCount, "RouletteBet");
			return;
		}
		if (existing.count() > costCount) {
			existing.count(existing.count() - costCount);
			if (itemRepository != null) {
				itemRepository.update(existing);
			}
		} else {
			player.inventory().remove(existing);
			existing.count(0);
			if (itemRepository != null) {
				itemRepository.delete(existing.objectId());
			}
		}
	}

	private void awardPrizeToInventoryAndDb(PlayerCharacter player, RouletteItem wonItem) {
		int count = (int) Math.min(Integer.MAX_VALUE, wonItem.count());
		if (count <= 0) {
			return;
		}
		if (inventoryService != null && player.inventory() != null && wonItem.enchantLevel() <= 0) {
			try {
				var addResult = inventoryService.addItem(player.inventory(), wonItem.itemId(), count, "RouletteReward");
				if (addResult != null && addResult.item() != null) {
					return;
				}
			} catch (Exception e) {
				log.debug("InventoryService.addItem fallback for roulette prize {}: {}", wonItem.itemId(), e.getMessage());
			}
		}
		ItemTemplateTable templates = this.itemTable != null ? this.itemTable
				: (inventoryService != null ? inventoryService.templates() : null);
		if (templates != null) {
			var tmplOpt = templates.get(wonItem.itemId());
			if (tmplOpt.isPresent()) {
				var tmpl = tmplOpt.get();
				if (player.inventory() != null && tmpl.isStackable() && wonItem.enchantLevel() <= 0) {
					ItemInstance existing = player.inventory().byItemId(wonItem.itemId()).orElse(null);
					if (existing != null) {
						existing.count((int) Math.min(Integer.MAX_VALUE, (long) existing.count() + count));
						if (itemRepository != null) {
							itemRepository.update(existing);
						}
						return;
					}
				}
				ItemInstance item = new ItemInstance(idFactory.nextId(), tmpl, player.objectId(), count);
				if (wonItem.enchantLevel() > 0) {
					item.enchant(wonItem.enchantLevel());
				}
				if (player.inventory() != null) {
					player.inventory().add(item);
				}
				if (itemRepository != null) {
					itemRepository.insert(item, "RouletteReward");
				}
				return;
			}
		}
		if (player.inventory() != null) {
			player.inventory().addItem(wonItem.itemId(), count);
			if (wonItem.enchantLevel() > 0) {
				player.inventory().byItemId(wonItem.itemId()).ifPresent(it -> it.enchant(wonItem.enchantLevel()));
			}
		}
	}

	public RouletteItem selectRandomPrize() {
		if (prizePool.isEmpty()) {
			return null;
		}

		double totalWeight = prizePool.stream().mapToDouble(RouletteItem::weight).sum();
		double roll = ThreadLocalRandom.current().nextDouble() * totalWeight;

		double cumulative = 0.0;
		for (RouletteItem item : prizePool) {
			cumulative += item.weight();
			if (roll <= cumulative) {
				return item;
			}
		}
		return prizePool.get(prizePool.size() - 1);
	}

	public String generateMainHtml(PlayerCharacter player) {
		StringBuilder sb = new StringBuilder();
		sb.append("<html><title>Lucky Roulette</title><body><center>");
		sb.append("<font color=\"LEVEL\">=== Lucky Wheel of Fortune ===</font><br>");
		sb.append("Spin cost: <font color=\"8FA888\">").append(String.format("%,d", defaultCostCount)).append(" Adena</font><br><br>");

		sb.append("<button value=\"SPIN WHEEL!\" action=\"bypass -h voiced_roulette spin\" width=114 height=30 back=\"L2UI_CH3.bigbutton2_down\" fore=\"L2UI_CH3.bigbutton2\"><br><br>");

		sb.append("<table width=280 bgcolor=000000>");
		sb.append("<tr><td colspan=2 align=center><font color=\"LEVEL\">Possible Rewards</font></td></tr>");
		for (RouletteItem item : prizePool) {
			String color = switch (item.rarity()) {
				case JACKPOT -> "C8B588";
				case EPIC -> "B0B5BC";
				case RARE -> "8FA8BE";
				case COMMON -> "FFFFFF";
			};
			sb.append("<tr>");
			sb.append("<td><font color=\"").append(color).append("\">").append(item.name()).append(" x").append(item.count());
			if (item.enchantLevel() > 0) sb.append(" (+").append(item.enchantLevel()).append(")");
			sb.append("</font></td>");
			sb.append("<td align=right><font color=\"808080\">[").append(item.rarity()).append("]</font></td>");
			sb.append("</tr>");
		}
		sb.append("</table>");
		sb.append("</center></body></html>");
		return sb.toString();
	}

	public String generateResultHtml(RouletteSpinResult result) {
		StringBuilder sb = new StringBuilder();
		sb.append("<html><title>Roulette Result</title><body><center>");
		if (result.success()) {
			sb.append("<font color=\"LEVEL\">*** YOU WON! ***</font><br><br>");
			sb.append("<font color=\"8FA888\">").append(result.item().name()).append("</font><br1>");
			sb.append("Amount: <font color=\"FFFFFF\">").append(result.item().count()).append("</font><br>");
			if (result.item().enchantLevel() > 0) {
				sb.append("Enchant: <font color=\"LEVEL\">+").append(result.item().enchantLevel()).append("</font><br>");
			}
			sb.append("<br><button value=\"Spin Again\" action=\"bypass -h voiced_roulette spin\" width=95 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\">");
		} else {
			sb.append("<font color=\"B86B6B\">*** SPIN FAILED ***</font><br><br>");
			sb.append("<font color=\"FFFFFF\">").append(result.message()).append("</font><br><br>");
			sb.append("<button value=\"Back\" action=\"bypass -h voiced_roulette\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">");
		}
		sb.append("</center></body></html>");
		return sb.toString();
	}
}
