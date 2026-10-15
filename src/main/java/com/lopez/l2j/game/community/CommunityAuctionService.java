package com.lopez.l2j.game.community;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemIconTable;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.item.ItemTemplateTable;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.world.GameWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Casa de Leiloes e Mercado entre jogadores BBS (_bbsauction) - Onda F7.
 * Liquidacao estrita exclusiva em Adena (57) e Gold Bar (3470) - ZERO DOAÇÃO!
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
			long createTime
	) {}

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

	public AuctionListing listItem(PlayerCharacter seller, int itemObjectId, int count, long price, int currencyItemId) {
		if (seller == null || seller.inventory() == null || price <= 0 || count <= 0) {
			return null;
		}

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
		if (item.count() < count) {
			return null;
		}

		int enchant = item.enchant();
		int itemId = item.itemId();

		// Remove o item colocado a venda do inventario do vendedor
		inv.remove(item, count);

		int auctionId = auctionIdGen.incrementAndGet();
		AuctionListing listing = new AuctionListing(
				auctionId, seller.objectId(), seller.name(), itemId, count, enchant, price, currencyItemId, System.currentTimeMillis()
		);
		activeListings.put(auctionId, listing);
		seller.sendMessage("Seu item foi anunciado no Leilao com sucesso!");
		return listing;
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

		// Entrega o item ao comprador mantendo encantamento
		int nextId = (objectIdFactory != null) ? objectIdFactory.nextId() : objIdGen.incrementAndGet();
		ItemTemplate tmpl = resolveTemplate(listing.itemId(), "AuctionItem " + listing.itemId());
		ItemInstance boughtItem = new ItemInstance(nextId, tmpl, buyer.objectId(), listing.count());
		boughtItem.enchant(listing.enchant());
		buyerInv.addItem(boughtItem);

		// Credita pagamento ao vendedor
		if (gameWorld != null) {
			var onlineSeller = gameWorld.findPlayer(listing.sellerObjectId());
			if (onlineSeller != null && onlineSeller.character() != null) {
				int coinObjId = (objectIdFactory != null) ? objectIdFactory.nextId() : objIdGen.incrementAndGet();
				ItemTemplate coinTmpl = resolveTemplate(listing.currencyItemId(), "Currency");
				ItemInstance coinItem = new ItemInstance(coinObjId, coinTmpl, listing.sellerObjectId(), (int) listing.price());
				onlineSeller.character().inventory().addItem(coinItem);
				onlineSeller.character().sendMessage("Seu item no Leilao foi vendido! Voce recebeu " + listing.price() + " moedas.");
			}
		}

		activeListings.remove(auctionId);
		buyer.sendMessage("Item arrematado com sucesso!");
		return true;
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
			sb.append("<td>Item ").append(listing.itemId()).append("</td>");
			sb.append("<td>").append(listing.count()).append("</td>");
			sb.append("<td>+").append(listing.enchant()).append("</td>");
			sb.append("<td>").append(listing.price()).append(" ").append(curName).append("</td>");
			sb.append("<td>").append(listing.sellerName()).append("</td>");
			sb.append("<td><button value=\"Comprar\" action=\"bypass -h _bbsauction;buy;").append(listing.auctionId()).append("\" width=60 height=20 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
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
