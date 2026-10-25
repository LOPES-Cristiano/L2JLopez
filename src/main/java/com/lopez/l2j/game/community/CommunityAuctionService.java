package com.lopez.l2j.game.community;

import com.lopez.l2j.game.cursed.CursedWeaponsManager;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemIconTable;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.item.ItemTemplateTable;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.world.GameWorld;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Casa de Leiloes e Mercado entre jogadores BBS (_bbsauction) - (#477).
 * Liquidacao estrita exclusiva em Adena (57) e Gold Bar (3470) - ZERO DOAÇÃO!
 * Persiste anuncios e pagamentos offline no banco de dados e preserva Augmentation, Mana e CustomTypes.
 */
@Service
public class CommunityAuctionService {

	private static final Logger log = LoggerFactory.getLogger(CommunityAuctionService.class);

	public record AuctionListing(
			int auctionId,
			int sellerObjectId,
			String sellerName,
			int itemId,
			int count,
			int enchant,
			long price,
			int currencyItemId,
			long createTime,
			int augmentationId,
			int mana,
			int customType1,
			int customType2
	) {
		public AuctionListing(
				int auctionId,
				int sellerObjectId,
				String sellerName,
				int itemId,
				int count,
				int enchant,
				long price,
				int currencyItemId,
				long createTime) {
			this(auctionId, sellerObjectId, sellerName, itemId, count, enchant, price, currencyItemId, createTime, 0, -1, 0, 0);
		}
	}

	private final Map<Integer, AuctionListing> activeListings = new ConcurrentHashMap<>();
	private final AtomicInteger auctionIdGen = new AtomicInteger(100);
	private final AtomicInteger objIdGen = new AtomicInteger(930000);

	@Autowired(required = false)
	private ItemIconTable itemIconTable;

	@Autowired(required = false)
	private ItemTemplateTable itemTemplateTable;

	@Autowired(required = false)
	private ObjectIdFactory objectIdFactory;

	@Autowired(required = false)
	private GameWorld gameWorld;

	@Autowired(required = false)
	private JdbcClient jdbc;

	@PostConstruct
	public void init() {
		initSchema();
		loadFromDb();
	}

	public void initSchema() {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("""
					CREATE TABLE IF NOT EXISTS bbs_auction (
						auction_id INT NOT NULL PRIMARY KEY,
						seller_id INT NOT NULL,
						seller_name VARCHAR(35) NOT NULL,
						item_id INT NOT NULL,
						count INT NOT NULL,
						enchant INT NOT NULL DEFAULT 0,
						price BIGINT NOT NULL,
						currency_id INT NOT NULL DEFAULT 57,
						create_time BIGINT NOT NULL,
						augmentation_id INT NOT NULL DEFAULT 0,
						mana INT NOT NULL DEFAULT -1,
						custom_type1 INT NOT NULL DEFAULT 0,
						custom_type2 INT NOT NULL DEFAULT 0
					)
					""").update();
		} catch (Exception ex) {
			log.debug("CommunityAuctionService initSchema ignorado: {}", ex.getMessage());
		}
	}

	public void loadFromDb() {
		if (jdbc == null) {
			return;
		}
		try {
			var rows = jdbc.sql("""
					SELECT auction_id, seller_id, seller_name, item_id, count, enchant, price, currency_id,
					       create_time, augmentation_id, mana, custom_type1, custom_type2
					FROM bbs_auction
					""").query().listOfRows();
			int maxId = 100;
			for (var r : rows) {
				int id = ((Number) r.get("auction_id")).intValue();
				maxId = Math.max(maxId, id);
				AuctionListing listing = new AuctionListing(
						id,
						((Number) r.get("seller_id")).intValue(),
						String.valueOf(r.get("seller_name")),
						((Number) r.get("item_id")).intValue(),
						((Number) r.get("count")).intValue(),
						((Number) r.get("enchant")).intValue(),
						((Number) r.get("price")).longValue(),
						((Number) r.get("currency_id")).intValue(),
						((Number) r.get("create_time")).longValue(),
						r.get("augmentation_id") != null ? ((Number) r.get("augmentation_id")).intValue() : 0,
						r.get("mana") != null ? ((Number) r.get("mana")).intValue() : -1,
						r.get("custom_type1") != null ? ((Number) r.get("custom_type1")).intValue() : 0,
						r.get("custom_type2") != null ? ((Number) r.get("custom_type2")).intValue() : 0
				);
				activeListings.put(id, listing);
			}
			auctionIdGen.set(maxId);
		} catch (Exception ex) {
			log.debug("CommunityAuctionService loadFromDb ignorado: {}", ex.getMessage());
		}
	}

	public synchronized AuctionListing listItem(PlayerCharacter seller, int itemObjectId, int count, long price, int currencyItemId) {
		if (seller == null || seller.inventory() == null || price <= 0 || count <= 0) {
			return null;
		}

		// Politica ZERO DOACOES: apenas Adena (57) e Gold Bar (3470)
		if (currencyItemId != 57 && currencyItemId != 3470) {
			seller.sendMessage("Apenas Adena (57) ou Gold Bar (3470) sao aceitos como moeda.");
			return null;
		}

		Inventory inv = seller.inventory();
		var itemOpt = inv.byObjectId(itemObjectId);
		if (itemOpt.isEmpty()) {
			return null;
		}

		ItemInstance item = itemOpt.get();
		if (item.count() < count || item.isEquipped()) {
			return null;
		}
		if (item.itemId() == CursedWeaponsManager.ZARICHE_ID || item.itemId() == CursedWeaponsManager.AKAMANAH_ID) {
			return null;
		}
		if (item.template() != null && (!item.template().tradeable() || "quest".equalsIgnoreCase(item.template().itemType()))) {
			return null;
		}

		int enchant = item.enchant();
		int itemId = item.itemId();
		int augId = item.augmentationId();
		int mana = item.mana();
		int ct1 = item.customType1();
		int ct2 = item.customType2();

		// Remove o item colocado a venda do inventario do vendedor
		inv.remove(item, count);

		int auctionId = auctionIdGen.incrementAndGet();
		AuctionListing listing = new AuctionListing(
				auctionId, seller.objectId(), seller.name(), itemId, count, enchant, price, currencyItemId,
				System.currentTimeMillis(), augId, mana, ct1, ct2
		);
		activeListings.put(auctionId, listing);
		persistListingToDb(listing);
		seller.sendMessage("Seu item foi anunciado no Leilao com sucesso!");
		return listing;
	}

	public synchronized boolean cancelListing(PlayerCharacter seller, int auctionId) {
		if (seller == null || seller.inventory() == null) {
			return false;
		}
		AuctionListing listing = activeListings.get(auctionId);
		if (listing == null || listing.sellerObjectId() != seller.objectId()) {
			return false;
		}
		activeListings.remove(auctionId);
		deleteListingFromDb(auctionId);

		int nextId = (objectIdFactory != null) ? objectIdFactory.nextId() : objIdGen.incrementAndGet();
		ItemTemplate tmpl = resolveTemplate(listing.itemId(), "AuctionItem " + listing.itemId());
		ItemInstance returned = new ItemInstance(
				nextId, tmpl, seller.objectId(), listing.count(), listing.enchant(),
				ItemInstance.LOC_INVENTORY, 0, listing.augmentationId(), listing.mana(),
				listing.customType1(), listing.customType2());
		seller.inventory().addItem(returned);
		seller.sendMessage("Anuncio cancelado e item devolvido ao seu inventario.");
		return true;
	}

	public synchronized boolean buyItem(PlayerCharacter buyer, int auctionId) {
		if (buyer == null || buyer.inventory() == null) return false;
		AuctionListing listing = activeListings.get(auctionId);
		if (listing == null) {
			buyer.sendMessage("Este anuncio nao esta mais disponivel.");
			return false;
		}

		if (listing.sellerObjectId() == buyer.objectId()) {
			buyer.sendMessage("Voce nao pode comprar seu proprio item anunciado.");
			return false;
		}

		Inventory buyerInv = buyer.inventory();
		if (buyerInv.count(listing.currencyItemId()) < listing.price()) {
			buyer.sendMessage("Saldo insuficiente da moeda do anuncio.");
			return false;
		}

		// Consome moeda do comprador
		buyerInv.destroyItemByItemId(listing.currencyItemId(), (int) listing.price());

		// Entrega o item ao comprador preservando encantamento, augmentation, mana e customTypes (#477)
		int nextId = (objectIdFactory != null) ? objectIdFactory.nextId() : objIdGen.incrementAndGet();
		ItemTemplate tmpl = resolveTemplate(listing.itemId(), "AuctionItem " + listing.itemId());
		ItemInstance boughtItem = new ItemInstance(
				nextId, tmpl, buyer.objectId(), listing.count(), listing.enchant(),
				ItemInstance.LOC_INVENTORY, 0, listing.augmentationId(), listing.mana(),
				listing.customType1(), listing.customType2());
		buyerInv.addItem(boughtItem);

		// Credita pagamento ao vendedor (online ou offline via SQL)
		boolean deliveredOnline = false;
		if (gameWorld != null) {
			var onlineSeller = gameWorld.findPlayer(listing.sellerObjectId());
			if (onlineSeller != null && onlineSeller.character() != null) {
				int coinObjId = (objectIdFactory != null) ? objectIdFactory.nextId() : objIdGen.incrementAndGet();
				ItemTemplate coinTmpl = resolveTemplate(listing.currencyItemId(), "Currency");
				ItemInstance coinItem = new ItemInstance(coinObjId, coinTmpl, listing.sellerObjectId(), (int) listing.price());
				onlineSeller.character().inventory().addItem(coinItem);
				onlineSeller.character().sendMessage("Seu item no Leilao foi vendido! Voce recebeu " + listing.price() + " moedas.");
				deliveredOnline = true;
			}
		}
		if (!deliveredOnline) {
			creditOfflineSellerInDb(listing.sellerObjectId(), listing.currencyItemId(), (int) listing.price());
		}

		activeListings.remove(auctionId);
		deleteListingFromDb(auctionId);
		buyer.sendMessage("Item arrematado com sucesso!");
		return true;
	}

	private void persistListingToDb(AuctionListing l) {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("""
					INSERT INTO bbs_auction (
						auction_id, seller_id, seller_name, item_id, count, enchant, price, currency_id,
						create_time, augmentation_id, mana, custom_type1, custom_type2
					) VALUES (
						:id, :sid, :sname, :iid, :cnt, :enc, :pr, :cid, :ct, :aug, :mana, :ct1, :ct2
					)
					ON DUPLICATE KEY UPDATE count = VALUES(count), price = VALUES(price)
					""")
					.param("id", l.auctionId())
					.param("sid", l.sellerObjectId())
					.param("sname", l.sellerName())
					.param("iid", l.itemId())
					.param("cnt", l.count())
					.param("enc", l.enchant())
					.param("pr", l.price())
					.param("cid", l.currencyItemId())
					.param("ct", l.createTime())
					.param("aug", l.augmentationId())
					.param("mana", l.mana())
					.param("ct1", l.customType1())
					.param("ct2", l.customType2())
					.update();
		} catch (Exception ex) {
			log.debug("Erro ao persistir bbs_auction: {}", ex.getMessage());
		}
	}

	private void deleteListingFromDb(int auctionId) {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("DELETE FROM bbs_auction WHERE auction_id = :id")
					.param("id", auctionId)
					.update();
		} catch (Exception ex) {
			log.debug("Erro ao remover bbs_auction: {}", ex.getMessage());
		}
	}

	private void creditOfflineSellerInDb(int sellerId, int currencyItemId, int amount) {
		if (jdbc == null || amount <= 0) {
			return;
		}
		try {
			int updated = jdbc.sql("""
					UPDATE items SET count = count + :amt
					WHERE owner_id = :owner AND item_id = :item AND loc = 'INVENTORY'
					""")
					.param("amt", amount)
					.param("owner", sellerId)
					.param("item", currencyItemId)
					.update();
			if (updated == 0) {
				int nextObjId = (objectIdFactory != null) ? objectIdFactory.nextId() : objIdGen.incrementAndGet();
				jdbc.sql("""
						INSERT INTO items (owner_id, object_id, item_id, count, enchant_level, loc, loc_data)
						VALUES (:owner, :obj, :item, :amt, 0, 'INVENTORY', 0)
						""")
						.param("owner", sellerId)
						.param("obj", nextObjId)
						.param("item", currencyItemId)
						.param("amt", amount)
						.update();
			}
		} catch (Exception ex) {
			log.debug("Erro ao creditar vendedor offline no bbs_auction: {}", ex.getMessage());
		}
	}

	public Collection<AuctionListing> getActiveListings() {
		return Collections.unmodifiableCollection(activeListings.values());
	}

	public String handleAuctionCommand(PlayerCharacter player, String command) {
		if (command.startsWith("_bbsauction;buy;")) {
			try {
				int auctionId = Integer.parseInt(command.substring(16).trim());
				buyItem(player, auctionId);
			} catch (Exception ignored) {}
		} else if (command.startsWith("_bbsauction;cancel;")) {
			try {
				int auctionId = Integer.parseInt(command.substring(19).trim());
				cancelListing(player, auctionId);
			} catch (Exception ignored) {}
		}
		return getAuctionHtml(player);
	}

	public String getAuctionHtml(PlayerCharacter player) {
		StringBuilder sb = new StringBuilder();
		sb.append("<html><body><center>");
		sb.append("<font color=\"LEVEL\" size=4><b>=== CASA DE LEILOES BBS (_bbsauction) ===</b></font><br>");
		sb.append("<font color=\"AAAAAA\">Negociacao Oficial: Apenas Adena (57) e Gold Bar (3470)</font><br><br>");

		sb.append("<table width=700 border=0 cellpadding=2 cellspacing=2 bgcolor=333333>");
		sb.append("<tr><td><b>Icone</b></td><td><b>Item</b></td><td><b>Qtd</b></td><td><b>Encantamento</b></td><td><b>Preco</b></td><td><b>Vendedor</b></td><td><b>Acao</b></td></tr>");

		for (AuctionListing listing : activeListings.values()) {
			String icon = itemIconTable != null ? itemIconTable.getIcon(listing.itemId()).orElse("icon.etc_gold_bar_i00") : "icon.etc_gold_bar_i00";
			String curName = listing.currencyItemId() == 57 ? "Adena" : "Gold Bar";

			sb.append("<tr>");
			sb.append("<td><img src=\"").append(icon).append("\" width=32 height=32></td>");
			sb.append("<td>Item ").append(listing.itemId()).append(listing.augmentationId() > 0 ? " [Aug]" : "").append("</td>");
			sb.append("<td>").append(listing.count()).append("</td>");
			sb.append("<td>+").append(listing.enchant()).append("</td>");
			sb.append("<td>").append(listing.price()).append(" ").append(curName).append("</td>");
			sb.append("<td>").append(listing.sellerName()).append("</td>");
			if (player != null && listing.sellerObjectId() == player.objectId()) {
				sb.append("<td><button value=\"Cancelar\" action=\"bypass -h _bbsauction;cancel;").append(listing.auctionId()).append("\" width=65 height=19 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>");
			} else {
				sb.append("<td><button value=\"Comprar\" action=\"bypass -h _bbsauction;buy;").append(listing.auctionId()).append("\" width=65 height=19 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>");
			}
			sb.append("</tr>");
		}
		sb.append("</table>");
		sb.append("<br><font color=\"777777\" size=1>Mercado 100% In-Game — Sem Doacoes!</font>");
		sb.append("</center></body></html>");
		return sb.toString();
	}

	private ItemTemplate resolveTemplate(int itemId, String fallbackName) {
		if (itemTemplateTable != null) {
			var opt = itemTemplateTable.get(itemId);
			if (opt.isPresent()) return opt.get();
		}
		return ItemTemplate.etc(itemId, itemId, fallbackName, "other", "normal", 100, "none", 0, true, true, true, true);
	}
}
