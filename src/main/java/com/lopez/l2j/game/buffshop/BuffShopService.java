package com.lopez.l2j.game.buffshop;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.game.skill.SkillService;
import com.lopez.l2j.game.skill.SkillTable;
import com.lopez.l2j.game.skill.SkillTemplate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
	public static final int MAX_BUFFS_PER_SHOP = 200;

	public enum BuffCategory {
		ALL("all", "Todos"),
		BUFFS("buffs", "Buffs"),
		DANCES("dances", "Dancas"),
		SONGS("songs", "Cancoes"),
		CHANTS("chants", "Chants"),
		SPECIAL("special", "Especiais");

		private final String code;
		private final String label;

		BuffCategory(String code, String label) {
			this.code = code;
			this.label = label;
		}

		public String getCode() {
			return code;
		}

		public String getLabel() {
			return label;
		}

		public static BuffCategory fromCode(String code) {
			if (code == null) return ALL;
			for (var c : values()) {
				if (c.code.equalsIgnoreCase(code)) return c;
			}
			return ALL;
		}

		public static BuffCategory getCategory(int skillId, String name) {
			if (isHotSpringsBuff(skillId) || skillId == 1355 || skillId == 1356 || skillId == 1357) {
				return SPECIAL;
			}
			if (name == null) return BUFFS;
			String lower = name.toLowerCase(Locale.ROOT);
			if (lower.startsWith("dance of") || lower.contains("dance")) {
				return DANCES;
			}
			if (lower.startsWith("song of") || lower.contains("song")) {
				return SONGS;
			}
			if (lower.startsWith("chant of") || lower.startsWith("war chant") || lower.startsWith("earth chant")
					|| lower.contains("pa'agri") || lower.contains("paagri")) {
				return CHANTS;
			}
			if (lower.startsWith("prophecy of") || lower.contains("hot springs")) {
				return SPECIAL;
			}
			return BUFFS;
		}

		public boolean matches(BuffShopItem item) {
			if (this == ALL) return true;
			return getCategory(item.skillId(), item.name()) == this;
		}
	}

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
	private final Map<Integer, Map<Integer, BuffShopItem>> draftShops = new ConcurrentHashMap<>();
	private final Map<Integer, String> draftTitles = new ConcurrentHashMap<>();

	@Autowired
	public BuffShopService(JdbcClient jdbc) {
		this.jdbc = jdbc;
		loadOfflineShops();
	}

	public static boolean isHotSpringsBuff(int skillId) {
		return skillId >= 4551 && skillId <= 4554;
	}

	public static String getSkillIcon(int skillId) {
		if (skillId < 1000) {
			return String.format(Locale.ROOT, "icon.skill%04d", skillId);
		}
		return "icon.skill" + skillId;
	}

	public static String formatAdena(long amount) {
		return String.format(Locale.ROOT, "%,d", amount).replace(',', '.');
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

	public Map<Integer, BuffShopItem> getDraftOrActiveItems(PlayerCharacter player, SkillTable skillTable) {
		if (player == null) {
			return Collections.emptyMap();
		}
		var active = activeShops.get(player.objectId());
		if (active != null) {
			return active.items();
		}
		return draftShops.computeIfAbsent(player.objectId(), k -> {
			Map<Integer, BuffShopItem> draft = new ConcurrentHashMap<>();
			var available = getAvailableBuffSkills(player, skillTable);
			for (var b : available) {
				draft.put(b.skillId(), b);
			}
			return draft;
		});
	}

	public String getDraftTitle(PlayerCharacter player) {
		if (player == null) return "Buff Store";
		if (draftTitles.containsKey(player.objectId())) {
			return draftTitles.get(player.objectId());
		}
		return player.storeTitle() != null && !player.storeTitle().isBlank() ? player.storeTitle() : "Buff Store";
	}

	public void setDraftTitle(PlayerCharacter player, String title) {
		if (player != null && title != null) {
			String clean = title.trim();
			if (clean.length() > 50) clean = clean.substring(0, 50);
			draftTitles.put(player.objectId(), clean);
		}
	}

	public void toggleDraftBuff(PlayerCharacter player, int skillId, SkillTable skillTable) {
		if (player == null) return;
		var draft = getDraftOrActiveItems(player, skillTable);
		if (draft.containsKey(skillId)) {
			draft.remove(skillId);
		} else {
			var available = getAvailableBuffSkills(player, skillTable);
			for (var b : available) {
				if (b.skillId() == skillId) {
					draft.put(skillId, b);
					break;
				}
			}
		}
	}

	public void setDraftPrice(PlayerCharacter player, int skillId, int price) {
		if (player == null || price < 0) return;
		var draft = draftShops.get(player.objectId());
		if (draft != null && draft.containsKey(skillId)) {
			var old = draft.get(skillId);
			draft.put(skillId, new BuffShopItem(old.skillId(), old.level(), price, old.name()));
		}
		var active = activeShops.get(player.objectId());
		if (active != null && active.items().containsKey(skillId)) {
			var old = active.items().get(skillId);
			active.items().put(skillId, new BuffShopItem(old.skillId(), old.level(), price, old.name()));
		}
	}

	public void setAllDraftPrices(PlayerCharacter player, int price, SkillTable skillTable) {
		if (player == null || price < 0) return;
		var draft = getDraftOrActiveItems(player, skillTable);
		for (var entry : draft.entrySet()) {
			var old = entry.getValue();
			draft.put(entry.getKey(), new BuffShopItem(old.skillId(), old.level(), price, old.name()));
		}
	}

	public void selectAllDraftBuffs(PlayerCharacter player, SkillTable skillTable) {
		if (player == null) return;
		var available = getAvailableBuffSkills(player, skillTable);
		var draft = draftShops.computeIfAbsent(player.objectId(), k -> new ConcurrentHashMap<>());
		for (var b : available) {
			if (!draft.containsKey(b.skillId())) {
				draft.put(b.skillId(), b);
			}
		}
	}

	public void clearDraftBuffs(PlayerCharacter player) {
		if (player == null) return;
		var draft = draftShops.computeIfAbsent(player.objectId(), k -> new ConcurrentHashMap<>());
		draft.clear();
	}

	public void selectDraftCategory(PlayerCharacter player, BuffCategory category, SkillTable skillTable) {
		if (player == null || category == null) return;
		var available = getAvailableBuffSkills(player, skillTable);
		var draft = draftShops.computeIfAbsent(player.objectId(), k -> new ConcurrentHashMap<>());
		for (var b : available) {
			if (category.matches(b)) {
				draft.put(b.skillId(), b);
			}
		}
	}

	public void clearDraftCategory(PlayerCharacter player, BuffCategory category, SkillTable skillTable) {
		if (player == null || category == null) return;
		var available = getAvailableBuffSkills(player, skillTable);
		var draft = draftShops.get(player.objectId());
		if (draft != null) {
			for (var b : available) {
				if (category.matches(b)) {
					draft.remove(b.skillId());
				}
			}
		}
	}

	private static final java.util.Set<Integer> EXCLUDED_BUFF_SHOP_SKILLS = java.util.Set.of(
			// Combat fighter self/target skills that should never be in a buff store
			4, // Dash
			78, // War Cry
			99, // Rapid Shot
			110, // Ultimate Defense
			111, // Ultimate Evasion
			121, // Battle Roar
			139, // Guts
			176, // Frenzy
			282, // Bear Spirit Totem
			287, // Lionheart
			288, // Guard Stance
			291, // Wolf Spirit Totem
			292, // Bison Spirit Totem
			297, // Fake Death
			298, // Rabbit Spirit Totem
			345, // Duelist Spirit
			353, // Ogre Spirit Totem
			354, // Puma Spirit Totem
			355, // Hawk Spirit Totem
			406, // Angelic Icon
			413, // Rapid Fire
			414, // Dead Eye
			420, // Zealot
			423, // Dark Form
			424, // Over the Body
			425, // Detect Weakness
			// Cures / Resurrect / Cleanses
			1016, 1018, 1020, 1031, 1340
	);

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
			if (EXCLUDED_BUFF_SHOP_SKILLS.contains(skillId)) {
				continue;
			}
			int level = entry.getValue();
			var tmplOpt = skillTable.get(skillId, level);
			if (tmplOpt.isEmpty()) {
				int maxLvl = skillTable.maxLevel(skillId);
				if (maxLvl > 0) {
					tmplOpt = skillTable.get(skillId, maxLvl);
					level = maxLvl;
				}
			}
			if (tmplOpt.isEmpty()) {
				continue;
			}
			SkillTemplate t = tmplOpt.get();
			boolean isHarmfulDebuff = t.isDebuff() && !isHotSpringsBuff(skillId);
			boolean isOffensive = t.isOffensive() && !isHotSpringsBuff(skillId);
			if (!t.isPassive() && !isOffensive && !isHarmfulDebuff && t.target() != null) {
				String tgt = t.target().toUpperCase(Locale.ROOT);
				boolean validTarget = tgt.equals("TARGET_ONE")
						|| tgt.equals("TARGET_PARTY")
						|| tgt.equals("TARGET_PARTY_MEMBER")
						|| tgt.equals("TARGET_CLAN")
						|| tgt.equals("TARGET_ALLY")
						|| tgt.equals("TARGET_AURA")
						|| tgt.equals("TARGET_CORPSE_ALLY")
						|| tgt.equals("TARGET_PET")
						|| tgt.equals("TARGET_SELF");
				if (validTarget) {
					boolean isBuff = !t.effects().isEmpty() || t.skillType().equalsIgnoreCase("BUFF") || t.isBuff() || isHotSpringsBuff(skillId);
					if (isBuff) {
						list.add(new BuffShopItem(skillId, level, 10_000, t.name()));
					}
				}
			}
		}
		list.sort(java.util.Comparator.comparing(BuffShopItem::name, String.CASE_INSENSITIVE_ORDER));
		return list;
	}

	/**
	 * Inicia a loja de buffs para o jogador especificado.
	 */
	public synchronized boolean startShop(PlayerCharacter seller, String title, List<BuffShopItem> items) {
		if (seller == null || items == null || items.isEmpty()) {
			return false;
		}
		if (seller.isDead() || seller.isOlympiadMode() || seller.isInCombat() || seller.autoAttacking()) {
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

		if (Config.BUFF_SHOP_TITLE_COLOR != null && !Config.BUFF_SHOP_TITLE_COLOR.isBlank()) {
			try {
				seller.titleColor(Integer.decode("0x" + Config.BUFF_SHOP_TITLE_COLOR.trim()));
			} catch (Exception ignored) {}
		}
		if (Config.BUFF_SHOP_NAME_COLOR != null && !Config.BUFF_SHOP_NAME_COLOR.isBlank()) {
			try {
				seller.nameColor(Integer.decode("0x" + Config.BUFF_SHOP_NAME_COLOR.trim()));
			} catch (Exception ignored) {}
		}

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
		seller.titleColor(0xFFFF77);
		seller.nameColor(0xFFFFFF);

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
		if (buyer.isDead() || buyer.isOlympiadMode()) {
			return new PurchaseResult(false, "Voce nao pode comprar buffs neste estado.", 0, List.of());
		}
		if (seller != null && seller.isDead()) {
			return new PurchaseResult(false, "O vendedor esta morto.", 0, List.of());
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

		Set<Integer> uniqueSkillIds = new LinkedHashSet<>(skillIds);
		long totalCost = 0;
		List<BuffShopItem> toApply = new ArrayList<>();
		for (int skillId : uniqueSkillIds) {
			BuffShopItem item = shop.items().get(skillId);
			if (item != null) {
				if (item.price() < 0) {
					continue;
				}
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

		int currencyId = Config.SELL_BY_ITEM ? Config.SELL_ITEM : ADENA_ID;
		String currencyName = Config.SELL_BY_ITEM ? Config.COIN_TEXT : "Adena";

		var currencyItem = buyer.inventory().byItemId(currencyId).orElse(null);
		if (currencyItem == null || currencyItem.count() < totalCost) {
			return new PurchaseResult(false, "Voce nao possui " + currencyName + " suficiente. Custo: " + formatAdena(totalCost), 0, List.of());
		}

		if (inventories != null && totalCost > 0) {
			inventories.consumeItem(buyer.inventory(), currencyId, (int) totalCost, "BuffShopPurchase");
			if (seller != null) {
				inventories.addItem(seller.inventory(), currencyId, (int) totalCost, "BuffShopRevenue");
			} else if (jdbc != null) {
				try {
					jdbc.sql("UPDATE items SET count = count + :totalCost WHERE owner_id = :ownerId AND item_id = :currencyId AND loc = 'INVENTORY'")
							.param("totalCost", totalCost)
							.param("ownerId", sellerId)
							.param("currencyId", currencyId)
							.update();
				} catch (Exception e) {
					log.warn("Erro ao atualizar adena offline para sellerId {}: {}", sellerId, e.getMessage());
				}
			}
		}

		List<Integer> applied = new ArrayList<>();
		for (BuffShopItem item : toApply) {
			applied.add(item.skillId());
		}

		return new PurchaseResult(true, "Buffs adquiridos com sucesso!", (int) totalCost, applied);
	}

	public String renderSellerManageHtml(PlayerCharacter seller, SkillTable skillTable, int page) {
		return renderSellerManageHtml(seller, skillTable, BuffCategory.ALL, page);
	}

	/**
	 * Renderiza o HTML do Gerenciador de Buff Shop para o vendedor com categorias e controles completos.
	 */
	public String renderSellerManageHtml(PlayerCharacter seller, SkillTable skillTable, BuffCategory category, int page) {
		if (seller == null) return "<html><body>Vendedor invalido.</body></html>";
		if (category == null) category = BuffCategory.ALL;

		var allAvailable = getAvailableBuffSkills(seller, skillTable);
		if (allAvailable.isEmpty() && !isBuffShop(seller.objectId())) {
			return "<html><body><center>"
					+ "<table width=290><tr><td align=center><font color=\"LEVEL\">=== Buff Store Manager ===</font></td></tr></table>"
					+ "<br><br><font color=\"FF5555\">Sua classe atual nao possui buffs para venda!</font><br><br>"
					+ "<font color=\"B09B79\">Classes compativeis: Prophet, Bishop, Elven Elder, Shillien Elder, Warcryer, Overlord, Bladedancer, Swordsinger e AIOx.</font>"
					+ "<br><br><button value=\"Fechar\" action=\"bypass -h voiced_buffshop close\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">"
					+ "</center></body></html>";
		}

		final BuffCategory curCat = category;
		var filtered = allAvailable.stream().filter(curCat::matches).toList();

		boolean isRunning = isBuffShop(seller.objectId());
		var draft = getDraftOrActiveItems(seller, skillTable);
		String title = getDraftTitle(seller);

		int catDraftCount = (int) filtered.stream().filter(b -> draft.containsKey(b.skillId())).count();
		int totalDraftCount = draft.size();

		int pageSize = 8;
		int totalPages = Math.max(1, (int) Math.ceil((double) filtered.size() / pageSize));
		int curPage = Math.max(1, Math.min(page, totalPages));
		int fromIdx = (curPage - 1) * pageSize;
		int toIdx = Math.min(fromIdx + pageSize, filtered.size());
		var pageItems = filtered.subList(fromIdx, toIdx);

		StringBuilder sb = new StringBuilder();
		sb.append("<html><body><center>");
		sb.append("<table width=285><tr><td align=center><font color=\"LEVEL\">=== Buff Store Manager ===</font></td></tr></table>");
		sb.append("<table width=285 bgcolor=\"000000\" cellpadding=2>");
		sb.append("<tr>");
		sb.append("<td width=140><font color=\"AAAAAA\">Status:</font> ").append(isRunning ? "<font color=\"00FF00\">ATIVA</font>" : "<font color=\"FF9900\">EDITANDO</font>").append("</td>");
		sb.append("<td width=145 align=right><font color=\"AAAAAA\">Total:</font> <font color=\"00FF00\">").append(totalDraftCount).append("</font>/").append(allAvailable.size()).append("</td>");
		sb.append("</tr>");
		sb.append("<tr>");
		sb.append("<td colspan=2><font color=\"AAAAAA\">Titulo:</font> <font color=\"LEVEL\">").append(title).append("</font> <a action=\"bypass -h voiced_buffshop title_menu\">[Alterar]</a></td>");
		sb.append("</tr>");
		sb.append("</table>");
		sb.append("<br1>");

		// Abas de categorias
		sb.append("<table width=285 cellpadding=0 cellspacing=1><tr>");
		for (BuffCategory c : BuffCategory.values()) {
			boolean activeTab = (c == curCat);
			String label = (activeTab ? "[" + c.getLabel() + "]" : c.getLabel());
			int w = switch (c) {
				case ALL -> 44;
				case BUFFS -> 44;
				case DANCES -> 47;
				case SONGS -> 47;
				case CHANTS -> 45;
				case SPECIAL -> 53;
			};
			sb.append("<td align=center><button value=\"").append(label)
					.append("\" action=\"bypass -h voiced_buffshop page ").append(c.getCode()).append(" 1\" width=").append(w)
					.append(" height=20 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
		}
		sb.append("</tr></table>");
		sb.append("<br1>");

		// Precos rapidos
		sb.append("<table width=285 cellpadding=0 cellspacing=1><tr>");
		sb.append("<td align=center><button value=\"10k\" action=\"bypass -h voiced_buffshop setall 10000 ").append(curCat.getCode()).append(" ").append(curPage).append("\" width=45 height=18 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
		sb.append("<td align=center><button value=\"50k\" action=\"bypass -h voiced_buffshop setall 50000 ").append(curCat.getCode()).append(" ").append(curPage).append("\" width=45 height=18 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
		sb.append("<td align=center><button value=\"100k\" action=\"bypass -h voiced_buffshop setall 100000 ").append(curCat.getCode()).append(" ").append(curPage).append("\" width=50 height=18 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
		sb.append("<td align=center><button value=\"500k\" action=\"bypass -h voiced_buffshop setall 500000 ").append(curCat.getCode()).append(" ").append(curPage).append("\" width=50 height=18 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
		sb.append("<td align=center><button value=\"1kk\" action=\"bypass -h voiced_buffshop setall 1000000 ").append(curCat.getCode()).append(" ").append(curPage).append("\" width=45 height=18 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
		sb.append("<td align=center><button value=\"2kk\" action=\"bypass -h voiced_buffshop setall 2000000 ").append(curCat.getCode()).append(" ").append(curPage).append("\" width=45 height=18 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
		sb.append("</tr></table>");

		// Botoes de selecao em lote
		sb.append("<table width=285 cellpadding=1 cellspacing=0><tr>");
		if (curCat != BuffCategory.ALL) {
			sb.append("<td align=center><button value=\"Marcar Aba\" action=\"bypass -h voiced_buffshop selectcat ").append(curCat.getCode()).append(" ").append(curPage).append("\" width=68 height=19 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
			sb.append("<td align=center><button value=\"Desmarcar Aba\" action=\"bypass -h voiced_buffshop clearcat ").append(curCat.getCode()).append(" ").append(curPage).append("\" width=75 height=19 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
		}
		sb.append("<td align=center><button value=\"Marcar Todos\" action=\"bypass -h voiced_buffshop selectall ").append(curCat.getCode()).append(" ").append(curPage).append("\" width=70 height=19 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
		sb.append("<td align=center><button value=\"Desmarcar Todos\" action=\"bypass -h voiced_buffshop clearall ").append(curCat.getCode()).append(" ").append(curPage).append("\" width=70 height=19 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
		sb.append("</tr></table>");
		sb.append("<img src=\"L2UI.SquareGray\" width=285 height=1><br1>");

		if (pageItems.isEmpty()) {
			sb.append("<br><font color=\"AAAAAA\">Nenhum buff nesta categoria.</font><br><br>");
		} else {
			for (var buff : pageItems) {
				boolean selected = draft.containsKey(buff.skillId());
				int currentPrice = selected ? draft.get(buff.skillId()).price() : buff.price();
				String icon = getSkillIcon(buff.skillId());

				sb.append("<table width=285 bgcolor=\"").append(selected ? "1a221a" : "111111").append("\" cellpadding=1 cellspacing=0><tr>");
				sb.append("<td width=34 valign=top><img src=\"").append(icon).append("\" width=32 height=32></td>");
				sb.append("<td width=185 valign=center>");
				sb.append("<font color=\"").append(selected ? "FFFFFF" : "777777").append("\">").append(buff.name()).append("</font><br1>");
				sb.append("<font color=\"888888\">Nv.").append(buff.level()).append("</font> - <font color=\"LEVEL\">").append(formatAdena(currentPrice)).append("a</font> ");
				sb.append("<a action=\"bypass -h voiced_buffshop price_menu ").append(buff.skillId()).append(" ").append(curCat.getCode()).append(" ").append(curPage).append("\">[Preco]</a>");
				sb.append("</td>");
				sb.append("<td width=66 align=right valign=center>");
				if (selected) {
					sb.append("<button value=\"Remover\" action=\"bypass -h voiced_buffshop toggle ").append(buff.skillId()).append(" ").append(curCat.getCode()).append(" ").append(curPage).append("\" width=62 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">");
				} else {
					sb.append("<button value=\"Adicionar\" action=\"bypass -h voiced_buffshop toggle ").append(buff.skillId()).append(" ").append(curCat.getCode()).append(" ").append(curPage).append("\" width=62 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">");
				}
				sb.append("</td></tr></table>");
			}
		}

		sb.append("<table width=285 cellpadding=0 cellspacing=0><tr>");
		sb.append("<td width=35 align=left>");
		if (curPage > 1) {
			sb.append("<button value=\"<<\" action=\"bypass -h voiced_buffshop page ").append(curCat.getCode()).append(" 1\" width=30 height=18 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">");
		}
		sb.append("</td>");
		sb.append("<td width=50 align=left>");
		if (curPage > 1) {
			sb.append("<button value=\"< Ant\" action=\"bypass -h voiced_buffshop page ").append(curCat.getCode()).append(" ").append(curPage - 1).append("\" width=45 height=18 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">");
		}
		sb.append("</td>");
		sb.append("<td width=115 align=center><font color=\"AAAAAA\">Pagina ").append(curPage).append(" / ").append(totalPages).append(" (").append(filtered.size()).append(" buffs)</font></td>");
		sb.append("<td width=50 align=right>");
		if (curPage < totalPages) {
			sb.append("<button value=\"Prox >\" action=\"bypass -h voiced_buffshop page ").append(curCat.getCode()).append(" ").append(curPage + 1).append("\" width=45 height=18 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">");
		}
		sb.append("</td>");
		sb.append("<td width=35 align=right>");
		if (curPage < totalPages) {
			sb.append("<button value=\">>\" action=\"bypass -h voiced_buffshop page ").append(curCat.getCode()).append(" ").append(totalPages).append("\" width=30 height=18 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">");
		}
		sb.append("</td></tr></table>");
		sb.append("<br1>");

		if (isRunning) {
			sb.append("<table width=280><tr>");
			sb.append("<td align=center><button value=\"Encerrar Loja\" action=\"bypass -h voiced_buffshop stop\" width=130 height=24 back=\"L2UI_CH3.bigbutton2_over\" fore=\"L2UI_CH3.bigbutton2\"></td>");
			sb.append("<td align=center><button value=\"Modo Offline\" action=\"bypass -h voiced_buffshop offline\" width=130 height=24 back=\"L2UI_CH3.bigbutton2_over\" fore=\"L2UI_CH3.bigbutton2\"></td>");
			sb.append("</tr></table>");
		} else {
			sb.append("<button value=\"INICIAR BUFF STORE (").append(totalDraftCount).append(" BUFFS)\" action=\"bypass -h voiced_buffshop start\" width=230 height=26 back=\"L2UI_CH3.bigbutton2_over\" fore=\"L2UI_CH3.bigbutton2\">");
		}
		sb.append("</center></body></html>");
		return sb.toString();
	}

	/**
	 * Renderiza a tela de alteracao do titulo da loja.
	 */
	public String renderTitleEditHtml(PlayerCharacter seller) {
		String cur = getDraftTitle(seller);
		return "<html><body><center>"
				+ "<table width=290><tr><td align=center><font color=\"LEVEL\">=== Titulo da Buff Store ===</font></td></tr></table>"
				+ "<br><br>Titulo atual: <font color=\"00FF00\">" + cur + "</font><br><br>"
				+ "Digite o novo titulo abaixo:<br>"
				+ "<edit var=\"t\" width=180 height=15><br><br>"
				+ "<button value=\"Salvar Titulo\" action=\"bypass -h voiced_buffshop title $t\" width=110 height=22 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"><br><br>"
				+ "<button value=\"Voltar\" action=\"bypass -h voiced_buffshop page 1\" width=80 height=20 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">"
				+ "</center></body></html>";
	}

	public String renderSkillPriceEditHtml(PlayerCharacter seller, int skillId, int page, SkillTable skillTable) {
		return renderSkillPriceEditHtml(seller, skillId, BuffCategory.ALL, page, skillTable);
	}

	/**
	 * Renderiza a tela de definicao individual de preco para uma skill especifica.
	 */
	public String renderSkillPriceEditHtml(PlayerCharacter seller, int skillId, BuffCategory category, int page, SkillTable skillTable) {
		if (category == null) category = BuffCategory.ALL;
		var draft = getDraftOrActiveItems(seller, skillTable);
		var item = draft.get(skillId);
		String name = item != null ? item.name() : ("Skill #" + skillId);
		int curPrice = item != null ? item.price() : 10000;
		String icon = getSkillIcon(skillId);

		return "<html><body><center>"
				+ "<table width=290><tr><td align=center><font color=\"LEVEL\">=== Preco do Buff ===</font></td></tr></table>"
				+ "<br><img src=\"" + icon + "\" width=32 height=32><br>"
				+ "<font color=\"FFFFFF\">" + name + "</font><br>"
				+ "Preco atual: <font color=\"00FF00\">" + formatAdena(curPrice) + " Adena</font><br><br>"
				+ "Precos rapidos:<br>"
				+ "<button value=\"10.000 Adena\" action=\"bypass -h voiced_buffshop price " + skillId + " 10000 " + category.getCode() + " " + page + "\" width=130 height=20 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"><br1>"
				+ "<button value=\"50.000 Adena\" action=\"bypass -h voiced_buffshop price " + skillId + " 50000 " + category.getCode() + " " + page + "\" width=130 height=20 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"><br1>"
				+ "<button value=\"100.000 Adena\" action=\"bypass -h voiced_buffshop price " + skillId + " 100000 " + category.getCode() + " " + page + "\" width=130 height=20 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"><br1>"
				+ "<button value=\"500.000 Adena\" action=\"bypass -h voiced_buffshop price " + skillId + " 500000 " + category.getCode() + " " + page + "\" width=130 height=20 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"><br1>"
				+ "<button value=\"1.000.000 Adena\" action=\"bypass -h voiced_buffshop price " + skillId + " 1000000 " + category.getCode() + " " + page + "\" width=130 height=20 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"><br><br>"
				+ "Ou digite o valor desejado:<br>"
				+ "<edit var=\"val\" width=110 height=15><br>"
				+ "<button value=\"Confirmar Preco\" action=\"bypass -h voiced_buffshop price " + skillId + " $val " + category.getCode() + " " + page + "\" width=110 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"><br><br>"
				+ "<button value=\"Voltar\" action=\"bypass -h voiced_buffshop page " + category.getCode() + " " + page + "\" width=75 height=20 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">"
				+ "</center></body></html>";
	}

	public String renderBuyerShopHtml(PlayerCharacter buyer, int sellerId, SkillTable skillTable, int page) {
		return renderBuyerShopHtml(buyer, sellerId, skillTable, BuffCategory.ALL, page);
	}

	/**
	 * Renderiza a loja de buffs para o comprador com suporte a abas por categoria, compra em lote e paginacao.
	 */
	public String renderBuyerShopHtml(PlayerCharacter buyer, int sellerId, SkillTable skillTable, BuffCategory category, int page) {
		if (buyer == null) return "<html><body>Comprador invalido.</body></html>";
		if (category == null) category = BuffCategory.ALL;

		BuffShop shop = activeShops.get(sellerId);
		if (shop == null) {
			return "<html><body><center><br><br><font color=\"FF5555\">Esta loja de buffs nao esta mais ativa.</font></center></body></html>";
		}

		List<BuffShopItem> allItems = new ArrayList<>(shop.items().values());
		allItems.sort(java.util.Comparator.comparing(BuffShopItem::name, String.CASE_INSENSITIVE_ORDER));

		final BuffCategory curCat = category;
		List<BuffShopItem> filtered = allItems.stream().filter(curCat::matches).toList();

		int currencyId = Config.SELL_BY_ITEM ? Config.SELL_ITEM : ADENA_ID;
		String coinName = Config.SELL_BY_ITEM ? Config.COIN_TEXT : "Adena";
		int buyerAdena = buyer.inventory().byItemId(currencyId).map(com.lopez.l2j.game.item.ItemInstance::count).orElse(0);

		long totalPriceAll = allItems.stream().mapToLong(BuffShopItem::price).sum();
		long totalPriceCat = filtered.stream().mapToLong(BuffShopItem::price).sum();

		int pageSize = 8;
		int totalPages = Math.max(1, (int) Math.ceil((double) filtered.size() / pageSize));
		int curPage = Math.max(1, Math.min(page, totalPages));
		int fromIdx = (curPage - 1) * pageSize;
		int toIdx = Math.min(fromIdx + pageSize, filtered.size());
		var pageItems = filtered.subList(fromIdx, toIdx);

		StringBuilder sb = new StringBuilder();
		sb.append("<html><body><center>");
		sb.append("<table width=285><tr><td align=center><font color=\"LEVEL\">=== ").append(shop.title()).append(" ===</font></td></tr></table>");
		sb.append("<table width=285 bgcolor=\"000000\" cellpadding=2>");
		sb.append("<tr>");
		sb.append("<td width=140><font color=\"AAAAAA\">Vendedor:</font> <font color=\"FFFFFF\">").append(shop.sellerName()).append("</font></td>");
		sb.append("<td width=145 align=right><font color=\"AAAAAA\">Seu Saldo:</font> <font color=\"00FF00\">").append(formatAdena(buyerAdena)).append("</font></td>");
		sb.append("</tr>");
		sb.append("<tr>");
		sb.append("<td colspan=2><font color=\"AAAAAA\">Categoria:</font> <font color=\"LEVEL\">").append(curCat.getLabel()).append("</font> <font color=\"888888\">(").append(filtered.size()).append(" buffs disponiveis)</font></td>");
		sb.append("</tr>");
		sb.append("</table>");
		sb.append("<br1>");

		// Abas de categorias
		sb.append("<table width=285 cellpadding=0 cellspacing=1><tr>");
		for (BuffCategory c : BuffCategory.values()) {
			boolean activeTab = (c == curCat);
			String label = (activeTab ? "[" + c.getLabel() + "]" : c.getLabel());
			int w = switch (c) {
				case ALL -> 44;
				case BUFFS -> 44;
				case DANCES -> 47;
				case SONGS -> 47;
				case CHANTS -> 45;
				case SPECIAL -> 53;
			};
			sb.append("<td align=center><button value=\"").append(label)
					.append("\" action=\"bypass -h voiced_buffshop buyer_page ").append(sellerId).append(" ").append(c.getCode()).append(" 1\" width=").append(w)
					.append(" height=20 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
		}
		sb.append("</tr></table>");
		sb.append("<br1>");

		// Botoes de compra em lote
		sb.append("<table width=285 cellpadding=1 cellspacing=0><tr>");
		if (curCat != BuffCategory.ALL && !filtered.isEmpty()) {
			sb.append("<td align=center><button value=\"Comprar ").append(curCat.getLabel()).append(" (").append(formatAdena(totalPriceCat)).append("a)\" action=\"bypass -h voiced_buffshop buycat ").append(sellerId).append(" ").append(curCat.getCode()).append(" ").append(curPage).append("\" width=140 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
		}
		sb.append("<td align=center><button value=\"Comprar Todos (").append(formatAdena(totalPriceAll)).append(" ").append(coinName).append(")\" action=\"bypass -h voiced_buffshop buyall ").append(sellerId).append("\" width=").append(curCat != BuffCategory.ALL && !filtered.isEmpty() ? "140" : "260").append(" height=21 back=\"L2UI_CH3.bigbutton2_over\" fore=\"L2UI_CH3.bigbutton2\"></td>");
		sb.append("</tr></table>");
		sb.append("<img src=\"L2UI.SquareGray\" width=285 height=1><br1>");

		if (pageItems.isEmpty()) {
			sb.append("<br><font color=\"AAAAAA\">Nenhum buff nesta categoria a venda.</font><br><br>");
		} else {
			for (var buff : pageItems) {
				String icon = getSkillIcon(buff.skillId());
				sb.append("<table width=285 bgcolor=\"111111\" cellpadding=1 cellspacing=0><tr>");
				sb.append("<td width=34 valign=top><img src=\"").append(icon).append("\" width=32 height=32></td>");
				sb.append("<td width=185 valign=center>");
				sb.append("<font color=\"FFFFFF\">").append(buff.name()).append("</font><br1>");
				sb.append("<font color=\"888888\">Nv.").append(buff.level()).append("</font> - <font color=\"LEVEL\">").append(formatAdena(buff.price())).append(" ").append(coinName).append("</font>");
				sb.append("</td>");
				sb.append("<td width=66 align=right valign=center>");
				sb.append("<button value=\"Comprar\" action=\"bypass -h voiced_buffshop buy ").append(sellerId).append(" ").append(buff.skillId()).append(" ").append(curCat.getCode()).append(" ").append(curPage).append("\" width=62 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">");
				sb.append("</td></tr></table>");
			}
		}

		sb.append("<table width=285 cellpadding=0 cellspacing=0><tr>");
		sb.append("<td width=35 align=left>");
		if (curPage > 1) {
			sb.append("<button value=\"<<\" action=\"bypass -h voiced_buffshop buyer_page ").append(sellerId).append(" ").append(curCat.getCode()).append(" 1\" width=30 height=18 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">");
		}
		sb.append("</td>");
		sb.append("<td width=50 align=left>");
		if (curPage > 1) {
			sb.append("<button value=\"< Ant\" action=\"bypass -h voiced_buffshop buyer_page ").append(sellerId).append(" ").append(curCat.getCode()).append(" ").append(curPage - 1).append("\" width=45 height=18 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">");
		}
		sb.append("</td>");
		sb.append("<td width=115 align=center><font color=\"AAAAAA\">Pagina ").append(curPage).append(" / ").append(totalPages).append(" (").append(filtered.size()).append(" buffs)</font></td>");
		sb.append("<td width=50 align=right>");
		if (curPage < totalPages) {
			sb.append("<button value=\"Prox >\" action=\"bypass -h voiced_buffshop buyer_page ").append(sellerId).append(" ").append(curCat.getCode()).append(" ").append(curPage + 1).append("\" width=45 height=18 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">");
		}
		sb.append("</td>");
		sb.append("<td width=35 align=right>");
		if (curPage < totalPages) {
			sb.append("<button value=\">>\" action=\"bypass -h voiced_buffshop buyer_page ").append(sellerId).append(" ").append(curCat.getCode()).append(" ").append(totalPages).append("\" width=30 height=18 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">");
		}
		sb.append("</td></tr></table>");

		sb.append("</center></body></html>");
		return sb.toString();
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
