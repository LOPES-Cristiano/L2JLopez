package com.lopez.l2j.network.game.handler.bypass;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.buffshop.BuffShopService;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.ChangeWaitType;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.MagicSkillUse;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage;
import com.lopez.l2j.network.game.packet.GameServerPacket.PlaySound;
import com.lopez.l2j.network.game.packet.GameServerPacket.PrivateStoreMsgSell;
import com.lopez.l2j.network.game.packet.GameServerPacket.ServerClose;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler modular para bypasses interativos da Buff Shop (HTML).
 * Processa acoes de configuracao do vendedor (selecao de buffs, precos, inicio, parada, modo offline)
 * e acoes de compra do comprador (compra individual, comprar todos).
 */
@Component
public class BypassBuffShopHandler implements IBypassHandler {

	private final BuffShopService buffShopService;

	@Autowired
	public BypassBuffShopHandler(BuffShopService buffShopService) {
		this.buffShopService = buffShopService;
	}

	@Override
	public boolean canHandle(String command) {
		if (command == null) return false;
		return command.startsWith("voiced_buffshop") || command.startsWith("buffshop_");
	}

	@Override
	public boolean handleBypass(String command, GameSession session) {
		if (session == null || session.activeCharacter() == null || buffShopService == null) {
			return false;
		}
		var active = session.activeCharacter();
		var ctx = session.context();
		var skillTable = ctx != null && ctx.skillService() != null ? ctx.skillService().table() : null;

		String argsStr = command.startsWith("voiced_buffshop")
				? (command.length() > 15 ? command.substring(15).trim() : "")
				: (command.length() > 9 ? command.substring(9).trim() : "");

		if (argsStr.isEmpty()) {
			session.send(new NpcHtmlMessage(0, buffShopService.renderSellerManageHtml(active, skillTable, 1)));
			return true;
		}

		String[] parts = argsStr.split("\\s+");
		String action = parts[0].toLowerCase();

		switch (action) {
			case "page" -> {
				int page = parts.length > 1 ? parseInt(parts[1], 1) : 1;
				session.send(new NpcHtmlMessage(0, buffShopService.renderSellerManageHtml(active, skillTable, page)));
				return true;
			}
			case "toggle" -> {
				int skillId = parts.length > 1 ? parseInt(parts[1], 0) : 0;
				int page = parts.length > 2 ? parseInt(parts[2], 1) : 1;
				if (skillId > 0) {
					buffShopService.toggleDraftBuff(active, skillId, skillTable);
				}
				session.send(new NpcHtmlMessage(0, buffShopService.renderSellerManageHtml(active, skillTable, page)));
				return true;
			}
			case "price_menu" -> {
				int skillId = parts.length > 1 ? parseInt(parts[1], 0) : 0;
				int page = parts.length > 2 ? parseInt(parts[2], 1) : 1;
				if (skillId > 0) {
					session.send(new NpcHtmlMessage(0, buffShopService.renderSkillPriceEditHtml(active, skillId, page, skillTable)));
				}
				return true;
			}
			case "price" -> {
				int skillId = parts.length > 1 ? parseInt(parts[1], 0) : 0;
				int price = parts.length > 2 ? parseInt(parts[2], -1) : -1;
				int page = parts.length > 3 ? parseInt(parts[3], 1) : 1;
				if (skillId > 0 && price >= 0) {
					buffShopService.setDraftPrice(active, skillId, price);
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Preco atualizado para " + BuffShopService.formatAdena(price) + " Adena."));
				}
				session.send(new NpcHtmlMessage(0, buffShopService.renderSellerManageHtml(active, skillTable, page)));
				return true;
			}
			case "setall" -> {
				int price = parts.length > 1 ? parseInt(parts[1], -1) : -1;
				int page = parts.length > 2 ? parseInt(parts[2], 1) : 1;
				if (price >= 0) {
					buffShopService.setAllDraftPrices(active, price, skillTable);
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Preco de todos os buffs alterado para " + BuffShopService.formatAdena(price) + " Adena."));
				}
				session.send(new NpcHtmlMessage(0, buffShopService.renderSellerManageHtml(active, skillTable, page)));
				return true;
			}
			case "selectall" -> {
				int page = parts.length > 1 ? parseInt(parts[1], 1) : 1;
				buffShopService.selectAllDraftBuffs(active, skillTable);
				session.send(new NpcHtmlMessage(0, buffShopService.renderSellerManageHtml(active, skillTable, page)));
				return true;
			}
			case "clearall" -> {
				int page = parts.length > 1 ? parseInt(parts[1], 1) : 1;
				buffShopService.clearDraftBuffs(active);
				session.send(new NpcHtmlMessage(0, buffShopService.renderSellerManageHtml(active, skillTable, page)));
				return true;
			}
			case "title_menu" -> {
				session.send(new NpcHtmlMessage(0, buffShopService.renderTitleEditHtml(active)));
				return true;
			}
			case "title" -> {
				if (parts.length > 1) {
					String newTitle = argsStr.substring(parts[0].length()).trim();
					if (!newTitle.isBlank()) {
						buffShopService.setDraftTitle(active, newTitle);
						if (buffShopService.isBuffShop(active.objectId())) {
							active.storeTitle(newTitle);
							session.sendUserInfoAndBroadcastCharInfo();
							if (ctx != null && ctx.world() != null) {
								ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS,
										new PrivateStoreMsgSell(active.objectId(), newTitle), false);
							}
						}
						session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Titulo da loja salvo: " + newTitle));
					}
				}
				session.send(new NpcHtmlMessage(0, buffShopService.renderSellerManageHtml(active, skillTable, 1)));
				return true;
			}
			case "start" -> {
				var draft = buffShopService.getDraftOrActiveItems(active, skillTable);
				if (draft.isEmpty()) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce deve selecionar ao menos uma habilidade para vender!"));
					session.send(new NpcHtmlMessage(0, buffShopService.renderSellerManageHtml(active, skillTable, 1)));
					return true;
				}
				String title = buffShopService.getDraftTitle(active);
				boolean ok = buffShopService.startShop(active, title, List.copyOf(draft.values()));
				if (ok) {
					var wait = new ChangeWaitType(active.objectId(), 0, session.x(), session.y(), session.z());
					session.send(wait);
					if (ctx != null && ctx.world() != null) {
						ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, wait, false);
					}
					var storeMsg = new PrivateStoreMsgSell(active.objectId(), title);
					session.send(storeMsg);
					if (ctx != null && ctx.world() != null) {
						ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, storeMsg, false);
					}
					session.sendUserInfoAndBroadcastCharInfo();
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Buff Shop iniciada com sucesso! (" + draft.size() + " buffs a venda)"));
				} else {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nao foi possivel iniciar a Buff Shop."));
				}
				session.send(new NpcHtmlMessage(0, buffShopService.renderSellerManageHtml(active, skillTable, 1)));
				return true;
			}
			case "stop" -> {
				buffShopService.stopShop(active);
				var wait = new ChangeWaitType(active.objectId(), 1, session.x(), session.y(), session.z());
				session.send(wait);
				if (ctx != null && ctx.world() != null) {
					ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, wait, false);
				}
				var storeMsg = new PrivateStoreMsgSell(active.objectId(), "");
				session.send(storeMsg);
				if (ctx != null && ctx.world() != null) {
					ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, storeMsg, false);
				}
				session.sendUserInfoAndBroadcastCharInfo();
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Buff Shop encerrada com sucesso."));
				session.send(new NpcHtmlMessage(0, buffShopService.renderSellerManageHtml(active, skillTable, 1)));
				return true;
			}
			case "offline" -> {
				if (!buffShopService.isBuffShop(active.objectId())) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce precisa estar com a Buff Shop aberta para ativar o modo offline."));
					session.send(new NpcHtmlMessage(0, buffShopService.renderSellerManageHtml(active, skillTable, 1)));
					return true;
				}
				var shopOpt = buffShopService.getShop(active.objectId());
				if (shopOpt.isPresent()) {
					buffShopService.saveOfflineShop(shopOpt.get());
					if (ctx != null && ctx.offlineTrade() != null) {
						ctx.offlineTrade().startOfflineTrade(active, 1, false, active.storeTitle(),
								List.of(new com.lopez.l2j.game.offlinetrade.OfflineShopItem(57, 1, 1)),
								com.lopez.l2j.game.offlinetrade.OfflineTradeService.DEFAULT_OFFLINE_DURATION);
					}
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Modo offline ativado com sucesso! Sua loja continuara online. Desconectando..."));
					session.close(new ServerClose());
				}
				return true;
			}
			case "buyer_page" -> {
				int sellerId = parts.length > 1 ? parseInt(parts[1], 0) : 0;
				int page = parts.length > 2 ? parseInt(parts[2], 1) : 1;
				if (sellerId > 0) {
					session.send(new NpcHtmlMessage(0, buffShopService.renderBuyerShopHtml(active, sellerId, skillTable, page)));
				}
				return true;
			}
			case "buy" -> {
				int sellerId = parts.length > 1 ? parseInt(parts[1], 0) : 0;
				int skillId = parts.length > 2 ? parseInt(parts[2], 0) : 0;
				int page = parts.length > 3 ? parseInt(parts[3], 1) : 1;
				handleBuy(session, active, sellerId, List.of(skillId), page);
				return true;
			}
			case "buyall" -> {
				int sellerId = parts.length > 1 ? parseInt(parts[1], 0) : 0;
				var shopOpt = buffShopService.getShop(sellerId);
				if (shopOpt.isEmpty()) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Esta loja de buffs nao esta mais ativa."));
					return true;
				}
				List<Integer> allSkills = new ArrayList<>(shopOpt.get().items().keySet());
				handleBuy(session, active, sellerId, allSkills, 1);
				return true;
			}
			case "close" -> {
				return true;
			}
			default -> {
				session.send(new NpcHtmlMessage(0, buffShopService.renderSellerManageHtml(active, skillTable, 1)));
				return true;
			}
		}
	}

	private void handleBuy(GameSession session, PlayerCharacter active, int sellerId, List<Integer> skillIds, int page) {
		var ctx = session.context();
		if (ctx == null) return;
		var skillTable = ctx.skillService() != null ? ctx.skillService().table() : null;

		var sellerOpt = ctx.world() != null ? ctx.world().player(sellerId) : java.util.Optional.<GameWorld.OnlinePlayer>empty();
		PlayerCharacter sellerChar = sellerOpt.map(GameWorld.OnlinePlayer::character).orElse(null);

		// Distancia maxima de compra: 250 unidades
		if (sellerOpt.isPresent()) {
			var seller = sellerOpt.get();
			double dx = active.x() - seller.x();
			double dy = active.y() - seller.y();
			if (dx * dx + dy * dy > 250.0 * 250.0) {
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce esta muito longe da loja de buffs (max 250 de distancia)."));
				session.send(new NpcHtmlMessage(0, buffShopService.renderBuyerShopHtml(active, sellerId, skillTable, page)));
				return;
			}
		}

		var result = buffShopService.purchaseBuffs(active, sellerId, skillIds, sellerChar, ctx.inventories(), skillTable, ctx.skillService());
		if (result.success()) {
			String coinName = Config.SELL_BY_ITEM ? Config.COIN_TEXT : "Adena";
			for (int skId : result.appliedSkills()) {
				int lvl = sellerChar != null ? sellerChar.skillLevel(skId) : 1;
				if (lvl <= 0) lvl = 1;
				var skOpt = skillTable != null ? skillTable.get(skId, lvl) : java.util.Optional.<com.lopez.l2j.game.skill.SkillTemplate>empty();
				if (skOpt.isPresent()) {
					var sk = skOpt.get();
					session.applySkillEffects(sk, false);
					if (sellerChar != null) {
						var anim = new MagicSkillUse(sellerChar.objectId(), active.objectId(), sk.id(), sk.level(), 500, 0);
						session.send(anim);
						if (ctx.world() != null) {
							ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, anim, false);
						}
					}
				}
			}
			session.send(new PlaySound("ItemSound.quest_finish"));
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce comprou " + result.appliedSkills().size() + " buff(s) por " + BuffShopService.formatAdena(result.totalCost()) + " " + coinName + "!"));
			if (sellerOpt.isPresent()) {
				sellerOpt.get().send(new CreatureSay(0, CreatureSay.ALL, "SYS", active.name() + " comprou " + result.appliedSkills().size() + " buff(s) na sua loja por " + BuffShopService.formatAdena(result.totalCost()) + " " + coinName + "!"));
			}
		} else {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", result.message()));
		}
		session.send(new NpcHtmlMessage(0, buffShopService.renderBuyerShopHtml(active, sellerId, skillTable, page)));
	}

	private int parseInt(String str, int def) {
		try {
			return Integer.parseInt(str.trim());
		} catch (Exception e) {
			return def;
		}
	}
}
