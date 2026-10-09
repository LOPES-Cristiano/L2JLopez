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
	private final Map<Integer, Map<Integer, BuffShopItem>> draftShops = new ConcurrentHashMap<>();
	private final Map<Integer, String> draftTitles = new ConcurrentHashMap<>();

	@Autowired
	public BuffShopService(JdbcClient jdbc) {
		this.jdbc = jdbc;
		loadOfflineShops();
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
		var draft = draftShops.get(player.objectId());
		if (draft != null) {
			draft.clear();
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
				continue;
			}
			SkillTemplate t = tmplOpt.get();
			// Apenas habilidades ativas beneficas aplicaveis a terceiros/grupo (sem dano, debuffs ou self combat)
			if (!t.isPassive() && !t.isOffensive() && !t.isDebuff() && t.target() != null && (t.target().equalsIgnoreCase("TARGET_ONE")
					|| t.target().equalsIgnoreCase("TARGET_PARTY")
					|| t.target().equalsIgnoreCase("TARGET_CLAN")
					|| t.target().equalsIgnoreCase("TARGET_AURA")
					|| t.target().equalsIgnoreCase("TARGET_CORPSE_ALLY"))) {
				if (!t.effects().isEmpty() || t.skillType().equalsIgnoreCase("BUFF") || t.isBuff()) {
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

	/**
	 * Renderiza o HTML do Gerenciador de Buff Shop para o vendedor.
	 */
	public String renderSellerManageHtml(PlayerCharacter seller, SkillTable skillTable, int page) {
		if (seller == null) return "<html><body>Vendedor invalido.</body></html>";

		var available = getAvailableBuffSkills(seller, skillTable);
		if (available.isEmpty() && !isBuffShop(seller.objectId())) {
			return "<html><body><center>"
					+ "<table width=290><tr><td align=center><font color=\"LEVEL\">=== Buff Store Manager ===</font></td></tr></table>"
					+ "<br><br><font color=\"FF5555\">Sua classe atual nao possui buffs para venda!</font><br><br>"
					+ "<font color=\"B09B79\">Classes compativeis: Prophet, Bishop, Elven Elder, Shillien Elder, Warcryer, Overlord, Bladedancer, Swordsinger e AIOx.</font>"
					+ "<br><br><button value=\"Fechar\" action=\"bypass -h voiced_buffshop close\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">"
					+ "</center></body></html>";
		}

		boolean isRunning = isBuffShop(seller.objectId());
		var draft = getDraftOrActiveItems(seller, skillTable);
		String title = getDraftTitle(seller);

		int pageSize = 6;
		int totalPages = Math.max(1, (int) Math.ceil((double) available.size() / pageSize));
		int curPage = Math.max(1, Math.min(page, totalPages));
		int fromIdx = (curPage - 1) * pageSize;
		int toIdx = Math.min(fromIdx + pageSize, available.size());
		var pageItems = available.subList(fromIdx, toIdx);

		StringBuilder sb = new StringBuilder();
		sb.append("<html><body><center>");
		sb.append("<table width=290><tr><td align=center><font color=\"LEVEL\">=== Buff Store Manager ===</font></td></tr></table>");
		sb.append("<table width=290 bgcolor=\"000000\">");
		sb.append("<tr>");
		sb.append("<td width=140><font color=\"AAAAAA\">Status:</font> ").append(isRunning ? "<font color=\"00FF00\">ATIVA</font>" : "<font color=\"FF9900\">EDITANDO</font>").append("</td>");
		sb.append("<td width=150 align=right><font color=\"AAAAAA\">Buffs:</font> <font color=\"00FF00\">").append(draft.size()).append("</font>/").append(available.size()).append("</td>");
		sb.append("</tr>");
		sb.append("<tr>");
		sb.append("<td colspan=2><font color=\"AAAAAA\">Titulo:</font> <font color=\"LEVEL\">").append(title).append("</font> <a action=\"bypass -h voiced_buffshop title_menu\">[Alterar]</a></td>");
		sb.append("</tr>");
		sb.append("</table>");
		sb.append("<br1>");

		sb.append("<table width=290><tr>");
		sb.append("<td align=center><button value=\"10k\" action=\"bypass -h voiced_buffshop setall 10000 ").append(curPage).append("\" width=50 height=18 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
		sb.append("<td align=center><button value=\"50k\" action=\"bypass -h voiced_buffshop setall 50000 ").append(curPage).append("\" width=50 height=18 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
		sb.append("<td align=center><button value=\"100k\" action=\"bypass -h voiced_buffshop setall 100000 ").append(curPage).append("\" width=55 height=18 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
		sb.append("<td align=center><button value=\"500k\" action=\"bypass -h voiced_buffshop setall 500000 ").append(curPage).append("\" width=55 height=18 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
		sb.append("<td align=center><button value=\"1kk\" action=\"bypass -h voiced_buffshop setall 1000000 ").append(curPage).append("\" width=50 height=18 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
		sb.append("</tr></table>");

		sb.append("<table width=290><tr>");
		sb.append("<td align=center><button value=\"Marcar Todos\" action=\"bypass -h voiced_buffshop selectall ").append(curPage).append("\" width=95 height=19 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
		sb.append("<td align=center><button value=\"Desmarcar Todos\" action=\"bypass -h voiced_buffshop clearall ").append(curPage).append("\" width=95 height=19 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
		sb.append("</tr></table>");
		sb.append("<img src=\"L2UI.SquareGray\" width=285 height=1><br>");

		for (var buff : pageItems) {
			boolean selected = draft.containsKey(buff.skillId());
			int currentPrice = selected ? draft.get(buff.skillId()).price() : buff.price();
			String icon = getSkillIcon(buff.skillId());

			sb.append("<table width=285 bgcolor=\"111111\"><tr>");
			sb.append("<td width=36 valign=top><img src=\"").append(icon).append("\" width=32 height=32></td>");
			sb.append("<td width=145 valign=top>");
			sb.append("<font color=\"").append(selected ? "FFFFFF" : "777777").append("\">").append(buff.name()).append("</font><br1>");
			sb.append("<font color=\"888888\">Nv.").append(buff.level()).append("</font> - <font color=\"LEVEL\">").append(formatAdena(currentPrice)).append("a</font> ");
			sb.append("<a action=\"bypass -h voiced_buffshop price_menu ").append(buff.skillId()).append(" ").append(curPage).append("\">[Preco]</a>");
			sb.append("</td>");
			sb.append("<td width=104 align=right valign=center>");
			if (selected) {
				sb.append("<button value=\"Remover\" action=\"bypass -h voiced_buffshop toggle ").append(buff.skillId()).append(" ").append(curPage).append("\" width=62 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">");
			} else {
				sb.append("<button value=\"Adicionar\" action=\"bypass -h voiced_buffshop toggle ").append(buff.skillId()).append(" ").append(curPage).append("\" width=62 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">");
			}
			sb.append("</td></tr></table>");
		}

		sb.append("<br>");
		sb.append("<table width=280><tr>");
		sb.append("<td width=80 align=left>");
		if (curPage > 1) {
			sb.append("<button value=\"< Anterior\" action=\"bypass -h voiced_buffshop page ").append(curPage - 1).append("\" width=75 height=20 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">");
		}
		sb.append("</td>");
		sb.append("<td width=120 align=center><font color=\"AAAAAA\">Pagina ").append(curPage).append(" de ").append(totalPages).append("</font></td>");
		sb.append("<td width=80 align=right>");
		if (curPage < totalPages) {
			sb.append("<button value=\"Proxima >\" action=\"bypass -h voiced_buffshop page ").append(curPage + 1).append("\" width=75 height=20 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">");
		}
		sb.append("</td></tr></table>");
		sb.append("<br>");

		if (isRunning) {
			sb.append("<table width=280><tr>");
			sb.append("<td align=center><button value=\"Encerrar Loja\" action=\"bypass -h voiced_buffshop stop\" width=120 height=24 back=\"L2UI_CH3.bigbutton2_over\" fore=\"L2UI_CH3.bigbutton2\"></td>");
			sb.append("<td align=center><button value=\"Modo Offline\" action=\"bypass -h voiced_buffshop offline\" width=120 height=24 back=\"L2UI_CH3.bigbutton2_over\" fore=\"L2UI_CH3.bigbutton2\"></td>");
			sb.append("</tr></table>");
		} else {
			sb.append("<button value=\"INICIAR BUFF STORE\" action=\"bypass -h voiced_buffshop start\" width=190 height=26 back=\"L2UI_CH3.bigbutton2_over\" fore=\"L2UI_CH3.bigbutton2\">");
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

	/**
	 * Renderiza a tela de definicao individual de preco para uma skill especifica.
	 */
	public String renderSkillPriceEditHtml(PlayerCharacter seller, int skillId, int page, SkillTable skillTable) {
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
				+ "<button value=\"10.000 Adena\" action=\"bypass -h voiced_buffshop price " + skillId + " 10000 " + page + "\" width=130 height=20 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"><br1>"
				+ "<button value=\"50.000 Adena\" action=\"bypass -h voiced_buffshop price " + skillId + " 50000 " + page + "\" width=130 height=20 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"><br1>"
				+ "<button value=\"100.000 Adena\" action=\"bypass -h voiced_buffshop price " + skillId + " 100000 " + page + "\" width=130 height=20 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"><br1>"
				+ "<button value=\"500.000 Adena\" action=\"bypass -h voiced_buffshop price " + skillId + " 500000 " + page + "\" width=130 height=20 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"><br1>"
				+ "<button value=\"1.000.000 Adena\" action=\"bypass -h voiced_buffshop price " + skillId + " 1000000 " + page + "\" width=130 height=20 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"><br><br>"
				+ "Ou digite o valor desejado:<br>"
				+ "<edit var=\"val\" width=110 height=15><br>"
				+ "<button value=\"Confirmar Preco\" action=\"bypass -h voiced_buffshop price " + skillId + " $val " + page + "\" width=110 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"><br><br>"
				+ "<button value=\"Voltar\" action=\"bypass -h voiced_buffshop page " + page + "\" width=75 height=20 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">"
				+ "</center></body></html>";
	}

	/**
	 * Renderiza a loja de buffs para o comprador com suporte nativo a icones, nomes, precos e botoes de compra.
	 */
	public String renderBuyerShopHtml(PlayerCharacter buyer, int sellerId, SkillTable skillTable, int page) {
		if (buyer == null) return "<html><body>Comprador invalido.</body></html>";

		BuffShop shop = activeShops.get(sellerId);
		if (shop == null) {
			return "<html><body><center><br><br><font color=\"FF5555\">Esta loja de buffs nao esta mais ativa.</font></center></body></html>";
		}

		List<BuffShopItem> items = new ArrayList<>(shop.items().values());
		int currencyId = Config.SELL_BY_ITEM ? Config.SELL_ITEM : ADENA_ID;
		String coinName = Config.SELL_BY_ITEM ? Config.COIN_TEXT : "Adena";
		int buyerAdena = buyer.inventory().byItemId(currencyId).map(com.lopez.l2j.game.item.ItemInstance::count).orElse(0);

		long totalPriceAll = items.stream().mapToLong(BuffShopItem::price).sum();

		int pageSize = 6;
		int totalPages = Math.max(1, (int) Math.ceil((double) items.size() / pageSize));
		int curPage = Math.max(1, Math.min(page, totalPages));
		int fromIdx = (curPage - 1) * pageSize;
		int toIdx = Math.min(fromIdx + pageSize, items.size());
		var pageItems = items.subList(fromIdx, toIdx);

		StringBuilder sb = new StringBuilder();
		sb.append("<html><body><center>");
		sb.append("<table width=290><tr><td align=center><font color=\"LEVEL\">=== ").append(shop.title()).append(" ===</font></td></tr></table>");
		sb.append("<table width=290 bgcolor=\"000000\">");
		sb.append("<tr>");
		sb.append("<td width=140><font color=\"AAAAAA\">Vendedor:</font> <font color=\"FFFFFF\">").append(shop.sellerName()).append("</font></td>");
		sb.append("<td width=150 align=right><font color=\"AAAAAA\">Seu Saldo:</font> <font color=\"00FF00\">").append(formatAdena(buyerAdena)).append("</font></td>");
		sb.append("</tr>");
		sb.append("</table>");
		sb.append("<img src=\"L2UI.SquareGray\" width=285 height=1><br>");

		for (var buff : pageItems) {
			String icon = getSkillIcon(buff.skillId());
			sb.append("<table width=285 bgcolor=\"111111\"><tr>");
			sb.append("<td width=36 valign=top><img src=\"").append(icon).append("\" width=32 height=32></td>");
			sb.append("<td width=155 valign=top>");
			sb.append("<font color=\"FFFFFF\">").append(buff.name()).append("</font><br1>");
			sb.append("<font color=\"888888\">Nv.").append(buff.level()).append("</font> - <font color=\"LEVEL\">").append(formatAdena(buff.price())).append(" ").append(coinName).append("</font>");
			sb.append("</td>");
			sb.append("<td width=94 align=right valign=center>");
			sb.append("<button value=\"Comprar\" action=\"bypass -h voiced_buffshop buy ").append(sellerId).append(" ").append(buff.skillId()).append(" ").append(curPage).append("\" width=62 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">");
			sb.append("</td></tr></table>");
		}

		sb.append("<br>");
		sb.append("<button value=\"Comprar Todos (").append(formatAdena(totalPriceAll)).append(" ").append(coinName).append(")\" action=\"bypass -h voiced_buffshop buyall ").append(sellerId).append("\" width=230 height=25 back=\"L2UI_CH3.bigbutton2_over\" fore=\"L2UI_CH3.bigbutton2\"><br>");

		sb.append("<table width=280><tr>");
		sb.append("<td width=80 align=left>");
		if (curPage > 1) {
			sb.append("<button value=\"< Anterior\" action=\"bypass -h voiced_buffshop buyer_page ").append(sellerId).append(" ").append(curPage - 1).append("\" width=75 height=20 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">");
		}
		sb.append("</td>");
		sb.append("<td width=120 align=center><font color=\"AAAAAA\">Pagina ").append(curPage).append(" de ").append(totalPages).append("</font></td>");
		sb.append("<td width=80 align=right>");
		if (curPage < totalPages) {
			sb.append("<button value=\"Proxima >\" action=\"bypass -h voiced_buffshop buyer_page ").append(sellerId).append(" ").append(curPage + 1).append("\" width=75 height=20 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">");
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
