package com.lopez.l2j.network.game.handler.packet;

import com.lopez.l2j.config.Config;
import java.util.HashMap;
import java.util.Map;
import com.lopez.l2j.game.buffshop.BuffShopService.BuffShopItem;
import com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameClientPacket.MultiSellChoose;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestPrivateStoreBuy;
import com.lopez.l2j.network.game.packet.GameClientPacket.SendWareHouseDepositList;
import com.lopez.l2j.network.game.packet.GameClientPacket.SendWareHouseWithDrawList;
import com.lopez.l2j.network.game.packet.GameClientPacket.SetPrivateStoreListSell;
import com.lopez.l2j.network.game.packet.GameClientPacket.SetPrivateStoreMsgSell;
import com.lopez.l2j.network.game.packet.GameServerPacket.ActionFailed;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.InventoryUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.ItemList;
import com.lopez.l2j.network.game.packet.GameServerPacket.MagicSkillUse;
import com.lopez.l2j.network.game.packet.GameServerPacket.MultiSellList;
import com.lopez.l2j.network.game.packet.GameServerPacket.PrivateStoreItem;
import com.lopez.l2j.network.game.packet.GameServerPacket.PrivateStoreManageListSell;
import com.lopez.l2j.network.game.packet.GameServerPacket.PrivateStoreMsgSell;
import com.lopez.l2j.network.game.packet.GameServerPacket.SellList;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler modular para operacoes comerciais, lojas privadas (BuffShop/Personal Store),
 * Warehouse (deposito/retirada) e MultiSell.
 */
public class TradeStorePacketHandler {

	private static final Logger log = LoggerFactory.getLogger(TradeStorePacketHandler.class);

	private final GameSession session;

	public TradeStorePacketHandler(GameSession session) {
		this.session = session;
	}

	public void handlePrivateStoreManageSell() {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (!session.inWorld() || active == null || ctx == null) {
			session.send(new ActionFailed());
			return;
		}
		if (active.isBuffShop() && ctx.buffShop() != null) {
			var skillTable = ctx.skillService() != null ? ctx.skillService().table() : null;
			session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage(0, ctx.buffShop().renderSellerManageHtml(active, skillTable, 1)));
			return;
		}
		List<PrivateStoreItem> availableItems = new ArrayList<>();
		for (var it : active.inventory().items()) {
			if (!it.isEquipped() && it.template().type2() != ItemTemplate.TYPE2_QUEST && it.template().price() > 0) {
				availableItems.add(new PrivateStoreItem(it.objectId(), it.itemId(), it.count(), it.template().price(), 0, it.enchant()));
			}
		}
		List<PrivateStoreItem> currentItems = new ArrayList<>();
		int currencyId = Config.SELL_BY_ITEM ? Config.SELL_ITEM : 57;
		int coinCount = active.inventory().byItemId(currencyId).map(ItemInstance::count).orElse(0);
		session.send(new PrivateStoreManageListSell(active.objectId(), false, coinCount, availableItems, currentItems));
	}

	public void handleSetPrivateStoreListSell(SetPrivateStoreListSell p) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (!session.inWorld() || active == null || ctx == null || ctx.buffShop() == null) {
			session.send(new ActionFailed());
			return;
		}
		if (p.items().isEmpty()) {
			handlePrivateStoreQuitSell();
			return;
		}
		List<BuffShopItem> buffItems = new ArrayList<>();
		for (var item : p.items()) {
			int skillId = item.objectId();
			int price = item.price();
			var skOpt = ctx.skillService() != null ? ctx.skillService().known(active, skillId)
					: java.util.Optional.<com.lopez.l2j.game.skill.SkillTemplate>empty();
			int level = skOpt.map(com.lopez.l2j.game.skill.SkillTemplate::level).orElse(1);
			String name = skOpt.map(com.lopez.l2j.game.skill.SkillTemplate::name).orElse("Skill #" + skillId);
			buffItems.add(new BuffShopItem(skillId, level, price, name));
		}
		ctx.buffShop().startShop(active, active.storeTitle() != null ? active.storeTitle() : "Buff Store", buffItems);
		session.sendUserInfoAndBroadcastCharInfo();
		if (ctx.world() != null) {
			ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS,
					new PrivateStoreMsgSell(active.objectId(), active.storeTitle()), false);
		}
		session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Buff shop iniciada com " + buffItems.size() + " buffs a venda!"));
	}

	public void handlePrivateStoreQuitSell() {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (!session.inWorld() || active == null || ctx == null || ctx.buffShop() == null) {
			return;
		}
		ctx.buffShop().stopShop(active);
		session.sendUserInfoAndBroadcastCharInfo();
		session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Buff shop encerrada."));
	}

	public void handleSetPrivateStoreMsgSell(SetPrivateStoreMsgSell p) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (!session.inWorld() || active == null) {
			return;
		}
		active.storeTitle(p.storeMsg());
		if (ctx != null && ctx.world() != null) {
			ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS,
					new PrivateStoreMsgSell(active.objectId(), p.storeMsg()), false);
		}
	}

	public void handlePrivateStoreBuy(RequestPrivateStoreBuy p) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (!session.inWorld() || active == null || ctx == null || ctx.buffShop() == null) {
			session.send(new ActionFailed());
			return;
		}
		var sellerOpt = ctx.world().player(p.sellerId());
		if (sellerOpt.isEmpty() || sellerOpt.get().character() == null) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Vendedor nao encontrado."));
			session.send(new ActionFailed());
			return;
		}
		if (active.isDead() || active.isOlympiadMode()) {
			session.send(new ActionFailed());
			return;
		}
		var sellerChar = sellerOpt.get().character();
		if (sellerChar == active) {
			session.send(new ActionFailed());
			return;
		}
		if (sellerChar.isDead()) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "O vendedor esta morto."));
			session.send(new ActionFailed());
			return;
		}
		double dist = Math.hypot(active.x() - sellerChar.x(), active.y() - sellerChar.y());
		if (dist > 250.0) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce esta muito longe da loja para comprar buffs."));
			session.send(new ActionFailed());
			return;
		}
		List<Integer> skillIds = p.items().stream().map(GameClientPacket.StoreItemRequest::objectId).toList();
		var skillTable = ctx.skillService() != null ? ctx.skillService().table() : null;
		var result = ctx.buffShop().purchaseBuffs(active, p.sellerId(), skillIds, sellerChar, ctx.inventories(),
				skillTable, ctx.skillService());
		if (result.success()) {
			for (int skillId : result.appliedSkills()) {
				int lvl = sellerChar.skillLevel(skillId);
				if (lvl <= 0) lvl = 1;
				var skOpt = skillTable != null ? skillTable.get(skillId, lvl)
						: java.util.Optional.<com.lopez.l2j.game.skill.SkillTemplate>empty();
				if (skOpt.isPresent()) {
					var sk = skOpt.get();
					session.applySkillEffects(sk, false);
					var anim = new MagicSkillUse(sellerChar.objectId(), active.objectId(), sk.id(), sk.level(), 500, 0);
					session.send(anim);
					sellerOpt.get().send(anim);
				}
			}
			String coinName = Config.SELL_BY_ITEM ? Config.COIN_TEXT : "Adena";
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce comprou buffs por " + result.totalCost() + " " + coinName + "."));
			sellerOpt.get().send(new CreatureSay(0, CreatureSay.ALL, "SYS",
					active.name() + " comprou buffs na sua loja por " + result.totalCost() + " " + coinName + "."));
		} else {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", result.message()));
		}
	}

	public void handleMultiSellChoose(MultiSellChoose p) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (!session.inWorld() || active == null || active.isDead() || active.isStoreOpen() || active.isBuffShop()
				|| active.isOlympiadMode() || p.amount() <= 0 || ctx == null || ctx.multisell() == null) {
			session.send(new ActionFailed());
			return;
		}
		var containerOpt = ctx.multisell().get(p.listId());
		if (containerOpt.isEmpty()) {
			session.send(new ActionFailed());
			return;
		}
		var container = containerOpt.get();
		var entryOpt = container.entries().stream().filter(e -> e.entryId() == p.entryId()).findFirst();
		if (entryOpt.isEmpty()) {
			session.send(new ActionFailed());
			return;
		}
		var entry = entryOpt.get();
		int amount = Math.min(5000, p.amount());

		// 1. Agrega ingredientes por itemId e valida quantidades totais necessarias
		Map<Integer, Long> aggregatedIngredients = new HashMap<>();
		for (var ing : entry.ingredients()) {
			long needed = (long) ing.count() * amount;
			if (needed <= 0 || needed > Integer.MAX_VALUE) {
				session.send(new ActionFailed());
				return;
			}
			aggregatedIngredients.merge(ing.itemId(), needed, Long::sum);
		}

		for (var ingEntry : aggregatedIngredients.entrySet()) {
			long needed = ingEntry.getValue();
			long count = active.inventory().byItemId(ingEntry.getKey()).map(i -> (long) i.count()).orElse(0L);
			if (count < needed || needed > Integer.MAX_VALUE) {
				session.send(SystemMessage.id(SystemMessage.YOU_NOT_ENOUGH_ADENA));
				session.send(new ActionFailed());
				return;
			}
		}

		// 2. Valida capacidade de slots do inventario para os produtos
		int slotsNeeded = 0;
		for (var prod : entry.products()) {
			long totalAdd = (long) prod.count() * amount;
			if (totalAdd <= 0 || totalAdd > Integer.MAX_VALUE) {
				session.send(new ActionFailed());
				return;
			}
			var tmpl = ctx.inventories().templates().get(prod.itemId()).orElse(null);
			if (tmpl == null) {
				session.send(new ActionFailed());
				return;
			}
			if (!tmpl.stackable()) {
				slotsNeeded += (int) totalAdd;
			} else if (active.inventory().byItemId(prod.itemId()).isEmpty()) {
				slotsNeeded++;
			}
		}
		if (active.inventory().size() + slotsNeeded > active.maxInventorySlots()) {
			session.send(SystemMessage.id(SystemMessage.SLOTS_FULL));
			session.send(new ActionFailed());
			return;
		}

		// 3. Consome os ingredientes agregados
		for (var ingEntry : aggregatedIngredients.entrySet()) {
			ctx.inventories().consumeItem(active.inventory(), ingEntry.getKey(), ingEntry.getValue().intValue(), "MultiSell");
		}

		// 4. Adiciona os produtos
		for (var prod : entry.products()) {
			long totalAdd = (long) prod.count() * amount;
			ctx.inventories().addItem(active.inventory(), prod.itemId(), (int) totalAdd, "MultiSell");
			session.send(SystemMessage.of(SystemMessage.YOU_PICKED_UP_S1_S2,
					new SystemMessage.ItemName(prod.itemId()),
					new SystemMessage.Number((int) totalAdd)));
		}

		// 4. Atualiza o inventario do jogador
		session.send(ItemList.of(active.inventory().items(), false));
		session.refreshWeightAndPenalties();
	}

	public void handleWareHouseDeposit(SendWareHouseDepositList p) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (!session.inWorld() || active == null || ctx == null || ctx.warehouse() == null || p.items().isEmpty()) {
			session.send(new ActionFailed());
			return;
		}
		if (active.isDead() || active.isOlympiadMode() || active.isStoreOpen() || active.isBuffShop() || active.isInCombat() || active.autoAttacking()) {
			session.send(new ActionFailed());
			return;
		}
		if (!com.lopez.l2j.config.Config.ALT_KARMA_PLAYER_CAN_USE_WAREHOUSE && active.karma() > 0) {
			session.send(new ActionFailed());
			return;
		}
		List<ItemInfo> updates = new ArrayList<>();
		var adenaBefore = active.inventory().byItemId(ItemTemplate.ADENA_ID).map(ItemInstance::count).orElse(0);

		for (var req : p.items()) {
			var opt = active.inventory().byObjectId(req.objectId());
			if (opt.isEmpty()) {
				continue;
			}
			var item = opt.get();
			int count = Math.min(req.count(), item.count());
			if (count <= 0 || item.isEquipped()) {
				continue;
			}
			int prevCount = item.count();
			boolean ok = ctx.warehouse().depositItem(active.inventory(), item.objectId(), count);
			if (ok) {
				if (prevCount == count) {
					updates.add(ItemInfo.of(item, ItemInfo.REMOVED));
				} else {
					updates.add(ItemInfo.of(item, ItemInfo.MODIFIED));
				}
			}
		}

		var adenaAfter = active.inventory().byItemId(ItemTemplate.ADENA_ID).orElse(null);
		if (adenaAfter != null && adenaAfter.count() != adenaBefore) {
			updates.add(ItemInfo.of(adenaAfter, adenaAfter.count() == 0 ? ItemInfo.REMOVED : ItemInfo.MODIFIED));
		}

		if (!updates.isEmpty()) {
			session.send(new InventoryUpdate(updates));
			session.refreshWeightAndPenalties();
		}
		session.send(new ActionFailed());
	}

	public void handleWareHouseWithdraw(SendWareHouseWithDrawList p) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (!session.inWorld() || active == null || ctx == null || ctx.warehouse() == null || p.items().isEmpty()) {
			session.send(new ActionFailed());
			return;
		}
		if (active.isDead() || active.isOlympiadMode() || active.isStoreOpen() || active.isBuffShop() || active.isInCombat() || active.autoAttacking()) {
			session.send(new ActionFailed());
			return;
		}
		if (!com.lopez.l2j.config.Config.ALT_KARMA_PLAYER_CAN_USE_WAREHOUSE && active.karma() > 0) {
			session.send(new ActionFailed());
			return;
		}
		List<ItemInfo> updates = new ArrayList<>();
		for (var req : p.items()) {
			int beforeCount = active.inventory().byObjectId(req.objectId()).map(ItemInstance::count).orElse(0);
			boolean ok = ctx.warehouse().withdrawItem(active.inventory(), active.objectId(), req.objectId(),
					req.count());
			if (ok) {
				var item = active.inventory().byObjectId(req.objectId()).orElse(null);
				if (item != null) {
					updates.add(ItemInfo.of(item, beforeCount == 0 ? ItemInfo.ADDED : ItemInfo.MODIFIED));
				}
			}
		}
		if (!updates.isEmpty()) {
			session.send(new InventoryUpdate(updates));
			session.refreshWeightAndPenalties();
		}
		session.send(new ActionFailed());
	}

	public void showMultiSell(NpcInstance npc, int listId) {
		var ctx = session.context();
		if (ctx == null || ctx.multisell() == null) {
			session.send(new ActionFailed());
			return;
		}
		var containerOpt = ctx.multisell().get(listId);
		if (containerOpt.isEmpty()) {
			log.warn("MultiSell id {} nao encontrada", listId);
			session.send(new ActionFailed());
			return;
		}
		var container = containerOpt.get();
		List<MultiSellList.MultiSellEntryView> views = new ArrayList<>();
		for (var entry : container.entries()) {
			List<MultiSellList.ItemView> ingredients = new ArrayList<>();
			for (var ing : entry.ingredients()) {
				var t = ctx.inventories().templates().get(ing.itemId()).orElse(null);
				int type2 = t != null ? t.type2() : 0;
				ingredients.add(new MultiSellList.ItemView(ing.itemId(), 0, type2, ing.count(), ing.enchantLevel()));
			}
			List<MultiSellList.ItemView> products = new ArrayList<>();
			for (var prod : entry.products()) {
				var t = ctx.inventories().templates().get(prod.itemId()).orElse(null);
				int bodyPart = t != null ? t.bodyPart() : 0;
				int type2 = t != null ? t.type2() : 0;
				products.add(
						new MultiSellList.ItemView(prod.itemId(), bodyPart, type2, prod.count(), prod.enchantLevel()));
			}
			views.add(new MultiSellList.MultiSellEntryView(entry.entryId(), ingredients, products));
		}
		session.send(new MultiSellList(listId, 1, 1, 40, views));
	}

	public void showSellList(NpcInstance npc) {
		PlayerCharacter active = session.activeChar();
		if (active == null) {
			return;
		}
		var sellable = active.inventory().items().stream()
				.filter(it -> !it.isEquipped() && it.template().type2() != ItemTemplate.TYPE2_QUEST
						&& it.template().price() > 0)
				.map(it -> new SellList.SellItemView(
						it.objectId(),
						it.itemId(),
						(int) it.count(),
						it.template().type1(),
						it.template().type2(),
						it.template().bodyPart(),
						it.enchant(),
						Math.max(1, it.template().price() / 2)))
				.toList();
		session.send(new SellList((int) active.inventory().adena(), 0, sellable));
	}
}
