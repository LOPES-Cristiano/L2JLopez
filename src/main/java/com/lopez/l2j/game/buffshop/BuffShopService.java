package com.lopez.l2j.game.buffshop;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.game.skill.SkillService;
import com.lopez.l2j.game.skill.SkillTable;
import com.lopez.l2j.game.skill.SkillTemplate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Servico de Buff Shop e Lojas Offline de Buffs (Item 39 do Roteiro Mestre).
 * Permite que jogadores sentem em modo loja vendendo seus buffs por Adena para outros jogadores,
 * com suporte completo a persistencia offline no banco de dados.
 */
@Service
public class BuffShopService {

	private static final Logger log = LoggerFactory.getLogger(BuffShopService.class);

	public static final int ADENA_ID = 57;
	public static final int MAX_BUFFS_PER_SHOP = 30;

	public record BuffShopItem(int skillId, int level, int price, String name) {}

	public record BuffShop(int sellerId, String sellerName, String title, Map<Integer, BuffShopItem> items,
			long startTime, boolean offline) {
		public BuffShop(int sellerId, String sellerName, String title, Map<Integer, BuffShopItem> items) {
			this(sellerId, sellerName, title, items, System.currentTimeMillis(), false);
		}
	}

	public record PurchaseResult(boolean success, String message, int totalCost, List<Integer> appliedSkills) {}

	private final JdbcClient jdbc;
	private final Map<Integer, BuffShop> activeShops = new ConcurrentHashMap<>();

	@Autowired
	public BuffShopService(JdbcClient jdbc) {
		this.jdbc = jdbc;
		loadOfflineShops();
	}

	public Optional<BuffShop> getShop(int sellerId) {
		return Optional.ofNullable(activeShops.get(sellerId));
	}

	public boolean isBuffShop(int sellerId) {
		return activeShops.containsKey(sellerId);
	}

	public Collection<BuffShop> getAllShops() {
		return Collections.unmodifiableCollection(activeShops.values());
	}

	/**
	 * Identifica quais skills ativas o personagem possui que podem ser disponibilizadas para venda como buff.
	 */
	public List<BuffShopItem> getAvailableBuffSkills(PlayerCharacter player, SkillTable skillTable) {
		List<BuffShopItem> list = new ArrayList<>();
		if (player == null || skillTable == null) {
			return list;
		}

		for (var entry : player.skills().entrySet()) {
			int skillId = entry.getKey();
			int level = entry.getValue();
			var tmplOpt = skillTable.get(skillId, level);
			if (tmplOpt.isEmpty()) {
				continue;
			}
			SkillTemplate t = tmplOpt.get();
			// Apenas habilidades ativas com efeitos benéficos (buffs, songs, dances, chants)
			if (!t.isPassive() && t.target() != null && (t.target().equalsIgnoreCase("TARGET_ONE")
					|| t.target().equalsIgnoreCase("TARGET_PARTY")
					|| t.target().equalsIgnoreCase("TARGET_CLAN")
					|| t.target().equalsIgnoreCase("TARGET_SELF")
					|| t.target().equalsIgnoreCase("TARGET_CORPSE_ALLY"))) {
				if (!t.effects().isEmpty() || t.skillType().equalsIgnoreCase("BUFF")
						|| t.skillType().equalsIgnoreCase("HEAL")
						|| t.skillType().equalsIgnoreCase("COMBATPOINTHEAL")
						|| t.skillType().equalsIgnoreCase("MANAHEAL")) {
					list.add(new BuffShopItem(skillId, level, 10_000, t.name()));
				}
			}
		}
		return list;
	}

	/**
	 * Inicia a loja de buffs para o jogador especificado.
	 */
	public synchronized boolean startShop(PlayerCharacter seller, String title, List<BuffShopItem> items) {
		if (seller == null || items == null || items.isEmpty()) {
			return false;
		}

		String cleanTitle = title != null && !title.isBlank() ? title.trim() : "Buff Store";
		if (cleanTitle.length() > 50) {
			cleanTitle = cleanTitle.substring(0, 50);
		}

		Map<Integer, BuffShopItem> itemMap = new ConcurrentHashMap<>();
		for (BuffShopItem item : items) {
			if (item.price() >= 0 && itemMap.size() < MAX_BUFFS_PER_SHOP) {
				itemMap.put(item.skillId(), item);
			}
		}

		if (itemMap.isEmpty()) {
			return false;
		}

		seller.sitting(true);
		seller.privateStoreType(1); // STORE_PRIVATE_SELL
		seller.storeTitle(cleanTitle);
		seller.setBuffShop(true);

		BuffShop shop = new BuffShop(seller.objectId(), seller.name(), cleanTitle, itemMap);
		activeShops.put(seller.objectId(), shop);
		log.info("BuffShop iniciada por {} com {} buffs: '{}'", seller.name(), itemMap.size(), cleanTitle);
		return true;
	}

	/**
	 * Encerra a loja de buffs.
	 */
	public synchronized void stopShop(PlayerCharacter seller) {
		if (seller == null) {
			return;
		}
		seller.privateStoreType(0);
		seller.storeTitle(null);
		seller.setBuffShop(false);
		seller.sitting(false);

		activeShops.remove(seller.objectId());
		removeOfflineShop(seller.objectId());
		log.info("BuffShop encerrada para {}", seller.name());
	}

	/**
	 * Processa a compra de buffs de uma loja ativa.
	 */
	public PurchaseResult purchaseBuffs(PlayerCharacter buyer, int sellerId, List<Integer> skillIds,
			PlayerCharacter seller, InventoryService inventories, SkillTable skillTable, SkillService skillService) {
		if (buyer == null) {
			return new PurchaseResult(false, "Comprador invalido.", 0, List.of());
		}

		BuffShop shop = activeShops.get(sellerId);
		if (shop == null) {
			return new PurchaseResult(false, "Esta loja de buffs nao esta mais ativa.", 0, List.of());
		}

		if (buyer.objectId() == sellerId) {
			return new PurchaseResult(false, "Voce nao pode comprar buffs da sua propria loja.", 0, List.of());
		}

		if (skillIds == null || skillIds.isEmpty()) {
			return new PurchaseResult(false, "Nenhum buff selecionado.", 0, List.of());
		}

		long totalCost = 0;
		List<BuffShopItem> toApply = new ArrayList<>();
		for (int skillId : skillIds) {
			BuffShopItem item = shop.items().get(skillId);
			if (item != null) {
				totalCost += item.price();
				toApply.add(item);
			}
		}

		if (toApply.isEmpty()) {
			return new PurchaseResult(false, "Nenhum dos buffs selecionados esta disponivel.", 0, List.of());
		}

		if (totalCost > Integer.MAX_VALUE) {
			return new PurchaseResult(false, "Preco total excede o limite de adena.", 0, List.of());
		}

		var adenaItem = buyer.inventory().byItemId(ADENA_ID).orElse(null);
		if (adenaItem == null || adenaItem.count() < totalCost) {
			return new PurchaseResult(false, "Voce nao possui Adena suficiente. Custo: " + totalCost, 0, List.of());
		}

		if (inventories != null && totalCost > 0) {
			inventories.consumeItem(buyer.inventory(), ADENA_ID, (int) totalCost, "BuffShopPurchase");
			if (seller != null) {
				inventories.addItem(seller.inventory(), ADENA_ID, (int) totalCost, "BuffShopRevenue");
			}
		}

		List<Integer> applied = new ArrayList<>();
		for (BuffShopItem item : toApply) {
			applied.add(item.skillId());
		}

		return new PurchaseResult(true, "Buffs adquiridos com sucesso!", (int) totalCost, applied);
	}

	/**
	 * Persiste uma loja no modo offline na base de dados.
	 */
	public synchronized void saveOfflineShop(BuffShop shop) {
		if (shop == null || jdbc == null) {
			return;
		}

		try {
			jdbc.sql("DELETE FROM character_offline_buffshop WHERE charId = :charId")
					.param("charId", shop.sellerId())
					.update();
			jdbc.sql("DELETE FROM character_offline_buffshop_skills WHERE charId = :charId")
					.param("charId", shop.sellerId())
					.update();

			jdbc.sql("INSERT INTO character_offline_buffshop (charId, time, title) VALUES (:charId, :time, :title)")
					.param("charId", shop.sellerId())
					.param("time", shop.startTime())
					.param("title", shop.title())
					.update();

			for (BuffShopItem item : shop.items().values()) {
				jdbc.sql("INSERT INTO character_offline_buffshop_skills (charId, item, price) VALUES (:charId, :item, :price)")
						.param("charId", shop.sellerId())
						.param("item", item.skillId())
						.param("price", item.price())
						.update();
			}
			log.info("BuffShop offline salva para charId {}", shop.sellerId());
		} catch (Exception e) {
			log.warn("Erro ao salvar BuffShop offline para charId {}: {}", shop.sellerId(), e.getMessage());
		}
	}

	public synchronized void removeOfflineShop(int charId) {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("DELETE FROM character_offline_buffshop WHERE charId = :charId")
					.param("charId", charId)
					.update();
			jdbc.sql("DELETE FROM character_offline_buffshop_skills WHERE charId = :charId")
					.param("charId", charId)
					.update();
		} catch (Exception e) {
			log.warn("Erro ao deletar BuffShop offline para charId {}: {}", charId, e.getMessage());
		}
	}

	public synchronized void loadOfflineShops() {
		if (jdbc == null) {
			return;
		}
		try {
			var rows = jdbc.sql("SELECT charId, time, title FROM character_offline_buffshop").query().listOfRows();
			for (var r : rows) {
				int charId = ((Number) r.get("charId")).intValue();
				long time = ((Number) r.get("time")).longValue();
				String title = (String) r.get("title");

				var skillRows = jdbc.sql("SELECT item, price FROM character_offline_buffshop_skills WHERE charId = :charId")
						.param("charId", charId)
						.query()
						.listOfRows();

				Map<Integer, BuffShopItem> itemMap = new ConcurrentHashMap<>();
				for (var sr : skillRows) {
					int skillId = ((Number) sr.get("item")).intValue();
					int price = ((Number) sr.get("price")).intValue();
					itemMap.put(skillId, new BuffShopItem(skillId, 1, price, "Skill #" + skillId));
				}

				BuffShop shop = new BuffShop(charId, "OfflineTrader", title != null ? title : "Buff Shop", itemMap, time, true);
				activeShops.put(charId, shop);
			}
			log.info("BuffShopService: {} lojas offline carregadas do banco de dados.", rows.size());
		} catch (Exception e) {
			log.warn("Aviso ao carregar lojas de buff offline: {}", e.getMessage());
		}
	}
}
