package com.lopez.l2j.network.game.handler.packet;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.buffshop.BuffShopService.BuffShopItem;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.offlinetrade.OfflineShopItem;
import com.lopez.l2j.game.offlinetrade.OfflineTradeService;
import com.lopez.l2j.game.offlinetrade.OfflineTradeService.BuyStoreList;
import com.lopez.l2j.game.offlinetrade.OfflineTradeService.SellStoreList;
import com.lopez.l2j.game.recipe.RecipeEntry;
import com.lopez.l2j.game.recipe.RecipeService;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.GameEngineContext;
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
import com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.MagicSkillUse;
import com.lopez.l2j.network.game.packet.GameServerPacket.MultiSellList;
import com.lopez.l2j.network.game.packet.GameServerPacket.PrivateStoreItem;
import com.lopez.l2j.network.game.packet.GameServerPacket.PrivateStoreListBuy;
import com.lopez.l2j.network.game.packet.GameServerPacket.PrivateStoreListSell;
import com.lopez.l2j.network.game.packet.GameServerPacket.PrivateStoreManageListBuy;
import com.lopez.l2j.network.game.packet.GameServerPacket.PrivateStoreManageListSell;
import com.lopez.l2j.network.game.packet.GameServerPacket.PrivateStoreMsgBuy;
import com.lopez.l2j.network.game.packet.GameServerPacket.PrivateStoreMsgSell;
import com.lopez.l2j.network.game.packet.GameServerPacket.RecipeItemMakeInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.SellList;
import com.lopez.l2j.network.game.packet.GameServerPacket.StatusUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handler modular para operacoes comerciais, lojas privadas (BuffShop/Personal Store),
 * Warehouse (deposito/retirada), Crafting/Recipes e MultiSell.
 */
public class TradeStorePacketHandler {

	private static final Logger log = LoggerFactory.getLogger(TradeStorePacketHandler.class);

	private final GameSession session;

	public TradeStorePacketHandler(GameSession session) {
		this.session = session;
	}

	private OfflineTradeService resolveOfflineTrade(GameSession.Context ctx) {
		if (ctx != null && ctx.offlineTrade() != null) {
			return ctx.offlineTrade();
		}
		if (ctx != null) {
			var bean = ctx.getBean(OfflineTradeService.class);
			if (bean != null) {
				return bean;
			}
		}
		var engineBean = GameEngineContext.getBean(OfflineTradeService.class);
		return engineBean != null ? engineBean : OfflineTradeService.getInstance();
	}

	private RecipeService resolveRecipeService(GameSession.Context ctx) {
		if (ctx != null) {
			var bean = ctx.getBean(RecipeService.class);
			if (bean != null) {
				return bean;
			}
		}
		var engineBean = GameEngineContext.getBean(RecipeService.class);
		return engineBean != null ? engineBean : RecipeService.getInstance();
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
			if (!it.isEquipped() && it.template() != null && it.template().type2() != ItemTemplate.TYPE2_QUEST
					&& it.itemId() != ItemTemplate.ADENA_ID && (it.template().isTradeable() || it.template().price() > 0)) {
				availableItems.add(new PrivateStoreItem(it.objectId(), it.itemId(), it.count(), it.template().price(), it.template().bodyPart(), it.enchant()));
			}
		}
		List<PrivateStoreItem> currentItems = new ArrayList<>();
		SellStoreList existing = resolveOfflineTrade(ctx).getSellStoreList(active.objectId());
		if (existing != null && existing.items() != null) {
			for (OfflineShopItem si : existing.items()) {
				currentItems.add(new PrivateStoreItem(si.objectId(), si.itemId(), si.count(), si.price(), si.bodyPart(), si.enchant()));
			}
		}
		int currencyId = Config.SELL_BY_ITEM ? Config.SELL_ITEM : ItemTemplate.ADENA_ID;
		int coinCount = active.inventory().byItemId(currencyId).map(ItemInstance::count).orElse(0);
		session.send(new PrivateStoreManageListSell(active.objectId(), existing != null && existing.packageSale(), coinCount, availableItems, currentItems));
	}

	public void handleSetPrivateStoreListSell(SetPrivateStoreListSell p) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (!session.inWorld() || active == null || ctx == null) {
			session.send(new ActionFailed());
			return;
		}
		if (p.items() == null || p.items().isEmpty()) {
			handlePrivateStoreQuitSell();
			return;
		}
		if (active.isBuffShop()) {
			if (ctx.buffShop() == null) {
				session.send(new ActionFailed());
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
			return;
		}

		if (active.isDead() || active.isOlympiadMode() || active.isInCombat()) {
			session.send(new ActionFailed());
			return;
		}

		List<OfflineShopItem> sellItems = new ArrayList<>();
		for (var req : p.items()) {
			if (req.count() <= 0 || req.price() < 0) {
				session.send(new ActionFailed());
				return;
			}
			var itemOpt = active.inventory().byObjectId(req.objectId())
					.or(() -> active.inventory().byItemId(req.objectId()));
			if (itemOpt.isEmpty()) {
				session.send(new ActionFailed());
				return;
			}
			ItemInstance invItem = itemOpt.get();
			if (invItem.isEquipped() || invItem.count() < req.count() || invItem.itemId() == ItemTemplate.ADENA_ID) {
				session.send(new ActionFailed());
				return;
			}
			if (invItem.template() != null && (invItem.template().isQuestItem() || !invItem.template().isTradeable())) {
				session.send(new ActionFailed());
				return;
			}
			int bodyPart = invItem.template() != null ? invItem.template().bodyPart() : 0;
			sellItems.add(new OfflineShopItem(invItem.objectId(), invItem.itemId(), req.count(), req.price(), invItem.enchant(), bodyPart));
		}

		if (sellItems.isEmpty()) {
			handlePrivateStoreQuitSell();
			return;
		}

		String title = active.storeTitle() != null ? active.storeTitle() : "Sell Store";
		resolveOfflineTrade(ctx).setSellStoreList(active.objectId(), p.packageSale(), title, sellItems);
		active.sitting(true);
		active.privateStoreType(PlayerCharacter.STORE_TYPE_SELL);
		session.sendUserInfoAndBroadcastCharInfo();
		if (ctx.world() != null) {
			ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS,
					new PrivateStoreMsgSell(active.objectId(), title), false);
		}
	}

	public void handlePrivateStoreQuitSell() {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (!session.inWorld() || active == null) {
			return;
		}
		if (active.isBuffShop()) {
			if (ctx != null && ctx.buffShop() != null) {
				ctx.buffShop().stopShop(active);
			}
			session.sendUserInfoAndBroadcastCharInfo();
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Buff shop encerrada."));
			return;
		}
		resolveOfflineTrade(ctx).removeSellStoreList(active.objectId());
		active.privateStoreType(PlayerCharacter.STORE_TYPE_NONE);
		active.sitting(false);
		session.sendUserInfoAndBroadcastCharInfo();
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

	public void showPrivateStoreSellWindow(PlayerCharacter seller) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (!session.inWorld() || active == null || seller == null) {
			session.send(new ActionFailed());
			return;
		}
		SellStoreList store = resolveOfflineTrade(ctx).getSellStoreList(seller.objectId());
		List<PrivateStoreItem> items = new ArrayList<>();
		if (store != null && store.items() != null) {
			for (OfflineShopItem si : store.items()) {
				var invItemOpt = seller.inventory().byObjectId(si.objectId())
						.or(() -> seller.inventory().byItemId(si.itemId()));
				if (invItemOpt.isPresent()) {
					ItemInstance invItem = invItemOpt.get();
					int availableCount = Math.min(si.count(), invItem.count());
					if (availableCount > 0) {
						items.add(new PrivateStoreItem(invItem.objectId(), invItem.itemId(), availableCount, si.price(), si.bodyPart(), invItem.enchant()));
					}
				}
			}
		}
		session.send(new PrivateStoreListSell(seller.objectId(), store != null && store.packageSale(), (int) active.inventory().adena(), items));
	}

	public void showPrivateStoreBuyWindow(PlayerCharacter buyer) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (!session.inWorld() || active == null || buyer == null) {
			session.send(new ActionFailed());
			return;
		}
		BuyStoreList store = resolveOfflineTrade(ctx).getBuyStoreList(buyer.objectId());
		List<PrivateStoreItem> items = new ArrayList<>();
		if (store != null && store.items() != null) {
			for (OfflineShopItem bi : store.items()) {
				var sellerItemOpt = active.inventory().items().stream()
						.filter(i -> !i.isEquipped() && i.itemId() == bi.itemId() && i.enchant() == bi.enchant())
						.findFirst();
				int objId = sellerItemOpt.map(ItemInstance::objectId).orElse(bi.objectId());
				items.add(new PrivateStoreItem(objId, bi.itemId(), bi.count(), bi.price(), bi.bodyPart(), bi.enchant()));
			}
		}
		session.send(new PrivateStoreListBuy(buyer.objectId(), (int) active.inventory().adena(), items));
	}

	public void handlePrivateStoreBuy(RequestPrivateStoreBuy p) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (!session.inWorld() || active == null || ctx == null || ctx.world() == null) {
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
		var sellerOnline = sellerOpt.get();
		var sellerChar = sellerOnline.character();
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
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce esta muito longe da loja."));
			session.send(new ActionFailed());
			return;
		}

		if (sellerChar.isBuffShop()) {
			if (ctx.buffShop() == null) {
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
						var anim = new MagicSkillUse(sellerChar.objectId(), active.objectId(), sk.id(), sk.level(), 500, 0,
								sellerChar.x(), sellerChar.y(), sellerChar.z(), active.x(), active.y(), active.z());
						session.send(anim);
						sellerOnline.send(anim);
					}
				}
				int currencyId = Config.SELL_BY_ITEM ? Config.SELL_ITEM : ItemTemplate.ADENA_ID;
				var buyerCur = active.inventory().byItemId(currencyId).orElse(null);
				if (buyerCur != null) {
					session.send(new InventoryUpdate(List.of(ItemInfo.of(buyerCur, buyerCur.count() == 0 ? ItemInfo.REMOVED : ItemInfo.MODIFIED))));
				}
				session.refreshWeightAndPenalties();

				var sellerCur = sellerChar.inventory().byItemId(currencyId).orElse(null);
				if (sellerCur != null) {
					sellerOnline.send(new InventoryUpdate(List.of(ItemInfo.of(sellerCur, ItemInfo.MODIFIED))));
					if (sellerOnline instanceof GameSession sellerSession) {
						sellerSession.refreshWeightAndPenalties();
					}
				}

				String coinName = Config.SELL_BY_ITEM ? Config.COIN_TEXT : "Adena";
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce comprou buffs por " + result.totalCost() + " " + coinName + "."));
				sellerOnline.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						active.name() + " comprou buffs na sua loja por " + result.totalCost() + " " + coinName + "."));
			} else {
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", result.message()));
			}
			return;
		}

		// Fluxo de Loja de Venda de Itens Reais (STORE_TYPE_SELL)
		if (sellerChar.privateStoreType() != PlayerCharacter.STORE_TYPE_SELL
				&& sellerChar.privateStoreType() != PlayerCharacter.STORE_PACKAGE_SELL) {
			session.send(new ActionFailed());
			return;
		}
		if (p.items() == null || p.items().isEmpty()) {
			session.send(new ActionFailed());
			return;
		}

		OfflineTradeService storeService = resolveOfflineTrade(ctx);
		SellStoreList sellStore = storeService.getSellStoreList(sellerChar.objectId());
		if (sellStore == null || sellStore.items().isEmpty()) {
			session.send(new ActionFailed());
			return;
		}

		PlayerCharacter firstLock = active.objectId() < sellerChar.objectId() ? active : sellerChar;
		PlayerCharacter secondLock = active.objectId() < sellerChar.objectId() ? sellerChar : active;

		synchronized (firstLock) {
			synchronized (secondLock) {
				long totalCost = 0L;
				record ValidatedBuyItem(OfflineShopItem storeEntry, ItemInstance sellerItem, int count, int unitPrice) {}
				List<ValidatedBuyItem> validated = new ArrayList<>();

				for (var req : p.items()) {
					if (req.count() <= 0) {
						session.send(new ActionFailed());
						return;
					}
					OfflineShopItem matchedStore = null;
					for (OfflineShopItem si : sellStore.items()) {
						if (si.objectId() == req.objectId() || si.itemId() == req.objectId()) {
							matchedStore = si;
							break;
						}
					}
					if (matchedStore == null || matchedStore.count() < req.count()) {
						session.send(new ActionFailed());
						return;
					}
					final int matchedItemId = matchedStore.itemId();
					var sellerItemOpt = sellerChar.inventory().byObjectId(matchedStore.objectId())
							.or(() -> sellerChar.inventory().byItemId(matchedItemId));
					if (sellerItemOpt.isEmpty()) {
						session.send(new ActionFailed());
						return;
					}
					ItemInstance sellerItem = sellerItemOpt.get();
					if (sellerItem.isEquipped() || sellerItem.count() < req.count()) {
						session.send(new ActionFailed());
						return;
					}
					int unitPrice = matchedStore.price();
					long lineCost = (long) unitPrice * req.count();
					if (lineCost < 0 || totalCost + lineCost > Integer.MAX_VALUE) {
						session.send(new ActionFailed());
						return;
					}
					totalCost += lineCost;
					validated.add(new ValidatedBuyItem(matchedStore, sellerItem, req.count(), unitPrice));
				}

				if (active.inventory().adena() < totalCost) {
					session.send(SystemMessage.id(SystemMessage.YOU_NOT_ENOUGH_ADENA));
					session.send(new ActionFailed());
					return;
				}

				List<ItemInfo> buyerUpdates = new ArrayList<>();
				List<ItemInfo> sellerUpdates = new ArrayList<>();

				if (totalCost > 0) {
					if (ctx.inventories() != null) {
						var consumedAdena = ctx.inventories().consumeItem(active.inventory(), ItemTemplate.ADENA_ID, (int) totalCost, "PrivateStoreBuy");
						if (consumedAdena != null) {
							buyerUpdates.add(ItemInfo.of(consumedAdena.item(), consumedAdena.removed() ? ItemInfo.REMOVED : ItemInfo.MODIFIED));
						}
						var addedAdena = ctx.inventories().addItem(sellerChar.inventory(), ItemTemplate.ADENA_ID, (int) totalCost, "PrivateStoreSell");
						if (addedAdena != null && addedAdena.item() != null) {
							sellerUpdates.add(ItemInfo.of(addedAdena.item(), addedAdena.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED));
						}
					} else {
						active.inventory().destroyItemByItemId(ItemTemplate.ADENA_ID, (int) totalCost);
						sellerChar.inventory().addAdena((int) totalCost);
					}
				}

				for (ValidatedBuyItem vb : validated) {
					int itemId = vb.sellerItem().itemId();
					int enchant = vb.sellerItem().enchant();
					if (ctx.inventories() != null) {
						var destroyed = ctx.inventories().destroyItem(sellerChar.inventory(), vb.sellerItem().objectId(), vb.count(), "PrivateStoreSell");
						if (destroyed != null) {
							sellerUpdates.add(ItemInfo.of(destroyed.item(), destroyed.removed() ? ItemInfo.REMOVED : ItemInfo.MODIFIED));
						}
						var added = ctx.inventories().addItem(active.inventory(), itemId, vb.count(), "PrivateStoreBuy");
						if (added != null) {
							List<ItemInstance> createdList = (added.allItems() != null && !added.allItems().isEmpty())
									? added.allItems()
									: (added.item() != null ? List.of(added.item()) : List.of());
							for (ItemInstance createdItem : createdList) {
								if (enchant > 0 && !createdItem.template().stackable()) {
									createdItem.enchant(enchant);
									ctx.inventories().saveItem(createdItem);
								}
								buyerUpdates.add(ItemInfo.of(createdItem, added.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED));
							}
						}
					} else {
						sellerChar.inventory().remove(vb.sellerItem(), vb.count());
						active.inventory().addItem(itemId, vb.count());
					}

					// Atualiza quantidade restante na lista da loja
					OfflineShopItem oldEntry = vb.storeEntry();
					int remaining = oldEntry.count() - vb.count();
					sellStore.items().remove(oldEntry);
					if (remaining > 0) {
						sellStore.items().add(new OfflineShopItem(oldEntry.objectId(), oldEntry.itemId(), remaining, oldEntry.price(), oldEntry.enchant(), oldEntry.bodyPart()));
					}
				}

				if (!buyerUpdates.isEmpty()) {
					session.send(new InventoryUpdate(buyerUpdates));
				}
				session.refreshWeightAndPenalties();

				if (!sellerUpdates.isEmpty()) {
					sellerOnline.send(new InventoryUpdate(sellerUpdates));
				}
				if (sellerOnline instanceof GameSession sellerSession) {
					sellerSession.refreshWeightAndPenalties();
				}

				if (sellStore.items().isEmpty()) {
					storeService.removeSellStoreList(sellerChar.objectId());
					sellerChar.privateStoreType(PlayerCharacter.STORE_TYPE_NONE);
					sellerChar.sitting(false);
					if (sellerOnline instanceof GameSession sellerSession) {
						sellerSession.sendUserInfoAndBroadcastCharInfo();
					}
				}
			}
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
		var entryOpt = containerOpt.get().entries().stream().filter(e -> e.entryId() == p.entryId()).findFirst();
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

		List<ItemInfo> updates = new ArrayList<>();
		// 3. Consome os ingredientes agregados
		for (var ingEntry : aggregatedIngredients.entrySet()) {
			var consumed = ctx.inventories().consumeItem(active.inventory(), ingEntry.getKey(), ingEntry.getValue().intValue(), "MultiSell");
			if (consumed != null) {
				updates.add(consumed.removed()
						? ItemInfo.of(consumed.item(), ItemInfo.REMOVED)
						: ItemInfo.of(consumed.item(), ItemInfo.MODIFIED));
			}
		}

		// 4. Adiciona os produtos
		for (var prod : entry.products()) {
			long totalAdd = (long) prod.count() * amount;
			var added = ctx.inventories().addItem(active.inventory(), prod.itemId(), (int) totalAdd, "MultiSell");
			if (added != null) {
				if (added.allItems() != null && !added.allItems().isEmpty()) {
					for (var item : added.allItems()) {
						updates.add(ItemInfo.of(item, added.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED));
					}
				} else {
					updates.add(ItemInfo.of(added.item(), added.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED));
				}
			}
			session.send(SystemMessage.of(SystemMessage.YOU_PICKED_UP_S1_S2,
					new SystemMessage.ItemName(prod.itemId()),
					new SystemMessage.Number((int) totalAdd)));
		}

		// 5. Atualiza o inventario do jogador via InventoryUpdate (obrigatorio para multisell sync)
		if (!updates.isEmpty()) {
			session.send(new InventoryUpdate(updates));
		}
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

	private com.lopez.l2j.game.service.WarehousePasskeyService resolveWarehousePasskeyService(GameSession.Context ctx) {
		if (ctx != null) {
			var bean = ctx.getBean(com.lopez.l2j.game.service.WarehousePasskeyService.class);
			if (bean != null) {
				return bean;
			}
		}
		var engineBean = GameEngineContext.getBean(com.lopez.l2j.game.service.WarehousePasskeyService.class);
		return engineBean != null ? engineBean : com.lopez.l2j.game.service.WarehousePasskeyService.getInstance();
	}

	public void handleSendWarehouseWithdrawList(SendWareHouseWithDrawList p) {
		handleWareHouseWithdraw(p);
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
		var passkeyService = resolveWarehousePasskeyService(ctx);
		if (passkeyService != null && passkeyService.hasPasskey(active.objectId()) && !passkeyService.isUnlocked(active.objectId())) {
			session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay(
					0, 2, "Warehouse", "Seu armazem esta protegido por Passkey. Desbloqueie antes de retirar itens."));
			session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage(
					0, passkeyService.buildPasskeyHtml(active.objectId(), "unlock", "", "Desbloqueie o armazem para retirar itens.")));
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

	// ---- Trade (Troca Direta) ----

	private com.lopez.l2j.game.trade.TradeService resolveTradeService(GameSession.Context ctx) {
		if (ctx == null) return null;
		var bean = ctx.getBean(com.lopez.l2j.game.trade.TradeService.class);
		return bean != null ? bean : com.lopez.l2j.network.game.GameEngineContext.getBean(com.lopez.l2j.game.trade.TradeService.class);
	}

	public void handleTradeRequest(GameClientPacket.TradeRequest p) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		var tradeService = resolveTradeService(ctx);
		if (!session.inWorld() || active == null || ctx == null || ctx.world() == null || tradeService == null) {
			session.send(new ActionFailed());
			return;
		}
		var targetOpt = ctx.world().player(p.objectId());
		if (targetOpt.isEmpty() || targetOpt.get().character() == null || targetOpt.get().character().instanceId() != active.instanceId()) {
			session.send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
			session.send(new ActionFailed());
			return;
		}
		PlayerCharacter partner = targetOpt.get().character();
		if (tradeService.requestTrade(active, partner, session::send)) {
			targetOpt.get().send(new com.lopez.l2j.network.game.packet.GameServerPacket.SendTradeRequest(active.objectId()));
		} else {
			session.send(new ActionFailed());
		}
	}

	public void handleAnswerTradeRequest(GameClientPacket.AnswerTradeRequest p) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		var tradeService = resolveTradeService(ctx);
		if (!session.inWorld() || active == null || ctx == null || ctx.world() == null || tradeService == null) {
			session.send(new ActionFailed());
			return;
		}
		var reqIdOpt = tradeService.getPendingRequester(active.objectId());
		if (reqIdOpt.isEmpty()) {
			session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.SendTradeDone(0));
			return;
		}
		var reqOnlineOpt = ctx.world().player(reqIdOpt.get());
		if (reqOnlineOpt.isEmpty() || reqOnlineOpt.get().character() == null) {
			session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.SendTradeDone(0));
			return;
		}
		var reqOnline = reqOnlineOpt.get();
		PlayerCharacter requester = reqOnline.character();
		var tradeOpt = tradeService.answerTrade(active, requester, p.response() == 1, session::send, reqOnline::send);
		if (tradeOpt.isPresent()) {
			var myItems = active.inventory().items().stream()
					.filter(i -> !i.isEquipped() && i.template() != null && i.template().isTradeable())
					.toList();
			var reqItems = requester.inventory().items().stream()
					.filter(i -> !i.isEquipped() && i.template() != null && i.template().isTradeable())
					.toList();
			session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.TradeStart(requester.objectId(), myItems));
			reqOnline.send(new com.lopez.l2j.network.game.packet.GameServerPacket.TradeStart(active.objectId(), reqItems));
		}
	}

	public void handleAddTradeItem(GameClientPacket.AddTradeItem p) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		var tradeService = resolveTradeService(ctx);
		if (!session.inWorld() || active == null || ctx == null || ctx.world() == null || tradeService == null) {
			session.send(new ActionFailed());
			return;
		}
		var tradeOpt = tradeService.getTrade(active.objectId());
		if (tradeOpt.isEmpty()) {
			session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.SendTradeDone(0));
			return;
		}
		var itemOpt = active.inventory().byObjectId(p.objectId());
		if (itemOpt.isEmpty()) {
			return;
		}
		if (tradeService.addItem(active, p.objectId(), p.count(), session::send)) {
			ItemInstance item = itemOpt.get();
			session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.TradeOwnAdd(item, p.count()));
			PlayerCharacter partner = tradeOpt.get().getPartner(active);
			ctx.world().player(partner.objectId()).ifPresent(po ->
					po.send(new com.lopez.l2j.network.game.packet.GameServerPacket.TradeOtherAdd(item, p.count())));
		}
	}

	public void handleTradeDone(GameClientPacket.TradeDone p) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		var tradeService = resolveTradeService(ctx);
		if (!session.inWorld() || active == null || ctx == null || ctx.world() == null || tradeService == null) {
			return;
		}
		var tradeOpt = tradeService.getTrade(active.objectId());
		if (tradeOpt.isEmpty()) {
			session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.SendTradeDone(0));
			return;
		}
		var trade = tradeOpt.get();
		PlayerCharacter partner = trade.getPartner(active);
		var partnerOnline = ctx.world().player(partner.objectId()).orElse(null);
		java.util.function.Consumer<com.lopez.l2j.network.game.packet.GameServerPacket> p1Sender =
				trade.player1().objectId() == active.objectId() ? session::send : (partnerOnline != null ? partnerOnline::send : null);
		java.util.function.Consumer<com.lopez.l2j.network.game.packet.GameServerPacket> p2Sender =
				trade.player2().objectId() == active.objectId() ? session::send : (partnerOnline != null ? partnerOnline::send : null);

		if (p.response() != 1) {
			tradeService.cancelTrade(active, p1Sender, p2Sender);
			session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.SendTradeDone(0));
			if (partnerOnline != null) {
				partnerOnline.send(new com.lopez.l2j.network.game.packet.GameServerPacket.SendTradeDone(0));
			}
			return;
		}

		tradeService.lockTrade(active);
		session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.TradePressOwnOk());
		if (partnerOnline != null) {
			partnerOnline.send(new com.lopez.l2j.network.game.packet.GameServerPacket.TradePressOtherOk());
		}
		if (trade.locked1() && trade.locked2()) {
			tradeService.confirmTrade(trade.player1(), p1Sender, p2Sender);
			boolean ok = tradeService.confirmTrade(trade.player2(), p1Sender, p2Sender);
			int res = ok ? 1 : 0;
			session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.SendTradeDone(res));
			session.refreshWeightAndPenalties();
			if (partnerOnline != null) {
				partnerOnline.send(new com.lopez.l2j.network.game.packet.GameServerPacket.SendTradeDone(res));
				if (partnerOnline instanceof GameSession ps) {
					ps.refreshWeightAndPenalties();
				}
			}
		}
	}

	// ---- Private Store Buy & Recipe Craft ----

	public void handlePrivateStoreManageBuy() {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (!session.inWorld() || active == null) {
			session.send(new ActionFailed());
			return;
		}
		List<PrivateStoreItem> available = new ArrayList<>();
		for (var it : active.inventory().items()) {
			if (!it.isEquipped() && it.template() != null && it.template().isTradeable() && it.itemId() != ItemTemplate.ADENA_ID) {
				available.add(new PrivateStoreItem(it.objectId(), it.itemId(), it.count(), it.template().price(), it.template().bodyPart(), it.enchant()));
			}
		}
		List<PrivateStoreItem> current = new ArrayList<>();
		BuyStoreList existing = resolveOfflineTrade(ctx).getBuyStoreList(active.objectId());
		if (existing != null && existing.items() != null) {
			for (OfflineShopItem bi : existing.items()) {
				current.add(new PrivateStoreItem(bi.objectId(), bi.itemId(), bi.count(), bi.price(), bi.bodyPart(), bi.enchant()));
			}
		}
		session.send(new PrivateStoreManageListBuy(
				active.objectId(), (int) active.inventory().adena(), current, available));
	}

	public void handleSetPrivateStoreListBuy(GameClientPacket.SetPrivateStoreListBuy p) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (!session.inWorld() || active == null) {
			session.send(new ActionFailed());
			return;
		}
		if (p.items() == null || p.items().isEmpty()) {
			handlePrivateStoreQuitBuy();
			return;
		}
		if (active.isDead() || active.isOlympiadMode() || active.isInCombat()) {
			session.send(new ActionFailed());
			return;
		}

		long totalCost = 0L;
		List<OfflineShopItem> buyItems = new ArrayList<>();
		for (var req : p.items()) {
			if (req.count() <= 0 || req.price() < 0) {
				session.send(new ActionFailed());
				return;
			}
			long lineCost = (long) req.price() * req.count();
			if (lineCost < 0 || totalCost + lineCost > Integer.MAX_VALUE) {
				session.send(new ActionFailed());
				return;
			}
			totalCost += lineCost;
			int bodyPart = 0;
			if (ctx != null && ctx.inventories() != null && ctx.inventories().templates() != null) {
				bodyPart = ctx.inventories().templates().get(req.itemId()).map(ItemTemplate::bodyPart).orElse(0);
			}
			buyItems.add(new OfflineShopItem(0, req.itemId(), req.count(), req.price(), req.enchant(), bodyPart));
		}

		if (totalCost > active.inventory().adena()) {
			session.send(SystemMessage.id(SystemMessage.YOU_NOT_ENOUGH_ADENA));
			session.send(new ActionFailed());
			handlePrivateStoreQuitBuy();
			return;
		}

		String title = active.storeTitle() != null ? active.storeTitle() : "Buy Store";
		resolveOfflineTrade(ctx).setBuyStoreList(active.objectId(), title, buyItems);
		active.sitting(true);
		active.privateStoreType(PlayerCharacter.STORE_TYPE_BUY);
		session.sendUserInfoAndBroadcastCharInfo();
		if (ctx != null && ctx.world() != null) {
			ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS,
					new PrivateStoreMsgBuy(active.objectId(), title), false);
		}
	}

	public void handlePrivateStoreQuitBuy() {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (!session.inWorld() || active == null) {
			return;
		}
		resolveOfflineTrade(ctx).removeBuyStoreList(active.objectId());
		active.privateStoreType(PlayerCharacter.STORE_TYPE_NONE);
		active.sitting(false);
		session.sendUserInfoAndBroadcastCharInfo();
	}

	public void handleSetPrivateStoreMsgBuy(GameClientPacket.SetPrivateStoreMsgBuy p) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (!session.inWorld() || active == null) {
			return;
		}
		active.storeTitle(p.storeMsg());
		if (ctx != null && ctx.world() != null) {
			ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS,
					new PrivateStoreMsgBuy(active.objectId(), p.storeMsg()), false);
		}
	}

	public void handlePrivateStoreSell(GameClientPacket.RequestPrivateStoreSell p) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (!session.inWorld() || active == null || ctx == null || ctx.world() == null) {
			session.send(new ActionFailed());
			return;
		}
		if (p.items() == null || p.items().isEmpty() || active.isDead() || active.isOlympiadMode()) {
			session.send(new ActionFailed());
			return;
		}
		var buyerOpt = ctx.world().player(p.buyerId());
		if (buyerOpt.isEmpty() || buyerOpt.get().character() == null) {
			session.send(new ActionFailed());
			return;
		}
		var buyerOnline = buyerOpt.get();
		PlayerCharacter buyerChar = buyerOnline.character();
		if (buyerChar == active || buyerChar.isDead() || buyerChar.privateStoreType() != PlayerCharacter.STORE_TYPE_BUY) {
			session.send(new ActionFailed());
			return;
		}
		double dist = Math.hypot(active.x() - buyerChar.x(), active.y() - buyerChar.y());
		if (dist > 250.0) {
			session.send(new ActionFailed());
			return;
		}

		OfflineTradeService storeService = resolveOfflineTrade(ctx);
		BuyStoreList buyStore = storeService.getBuyStoreList(buyerChar.objectId());
		if (buyStore == null || buyStore.items().isEmpty()) {
			session.send(new ActionFailed());
			return;
		}

		PlayerCharacter firstLock = active.objectId() < buyerChar.objectId() ? active : buyerChar;
		PlayerCharacter secondLock = active.objectId() < buyerChar.objectId() ? buyerChar : active;

		synchronized (firstLock) {
			synchronized (secondLock) {
				long totalCost = 0L;
				record ValidatedSellItem(OfflineShopItem storeOrder, ItemInstance sellerItem, int count, int unitPrice) {}
				List<ValidatedSellItem> validated = new ArrayList<>();

				for (var req : p.items()) {
					if (req.count() <= 0) {
						session.send(new ActionFailed());
						return;
					}
					OfflineShopItem matchedOrder = null;
					for (OfflineShopItem bi : buyStore.items()) {
						if (bi.itemId() == req.itemId() && bi.enchant() == req.enchant()) {
							matchedOrder = bi;
							break;
						}
					}
					if (matchedOrder == null || matchedOrder.count() < req.count()) {
						session.send(new ActionFailed());
						return;
					}
					var sellerItemOpt = active.inventory().byObjectId(req.objectId())
							.or(() -> active.inventory().byItemId(req.itemId()));
					if (sellerItemOpt.isEmpty()) {
						session.send(new ActionFailed());
						return;
					}
					ItemInstance sellerItem = sellerItemOpt.get();
					if (sellerItem.isEquipped() || sellerItem.itemId() != req.itemId() || sellerItem.count() < req.count()) {
						session.send(new ActionFailed());
						return;
					}
					int unitPrice = matchedOrder.price();
					long lineCost = (long) unitPrice * req.count();
					if (lineCost < 0 || totalCost + lineCost > Integer.MAX_VALUE) {
						session.send(new ActionFailed());
						return;
					}
					totalCost += lineCost;
					validated.add(new ValidatedSellItem(matchedOrder, sellerItem, req.count(), unitPrice));
				}

				if (buyerChar.inventory().adena() < totalCost) {
					session.send(new ActionFailed());
					return;
				}

				List<ItemInfo> sellerUpdates = new ArrayList<>();
				List<ItemInfo> buyerUpdates = new ArrayList<>();

				if (totalCost > 0) {
					if (ctx.inventories() != null) {
						var consumedAdena = ctx.inventories().consumeItem(buyerChar.inventory(), ItemTemplate.ADENA_ID, (int) totalCost, "PrivateStoreBuy");
						if (consumedAdena != null) {
							buyerUpdates.add(ItemInfo.of(consumedAdena.item(), consumedAdena.removed() ? ItemInfo.REMOVED : ItemInfo.MODIFIED));
						}
						var addedAdena = ctx.inventories().addItem(active.inventory(), ItemTemplate.ADENA_ID, (int) totalCost, "PrivateStoreSell");
						if (addedAdena != null && addedAdena.item() != null) {
							sellerUpdates.add(ItemInfo.of(addedAdena.item(), addedAdena.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED));
						}
					} else {
						buyerChar.inventory().destroyItemByItemId(ItemTemplate.ADENA_ID, (int) totalCost);
						active.inventory().addAdena((int) totalCost);
					}
				}

				for (ValidatedSellItem vs : validated) {
					int itemId = vs.sellerItem().itemId();
					int enchant = vs.sellerItem().enchant();
					if (ctx.inventories() != null) {
						var destroyed = ctx.inventories().destroyItem(active.inventory(), vs.sellerItem().objectId(), vs.count(), "PrivateStoreSell");
						if (destroyed != null) {
							sellerUpdates.add(ItemInfo.of(destroyed.item(), destroyed.removed() ? ItemInfo.REMOVED : ItemInfo.MODIFIED));
						}
						var added = ctx.inventories().addItem(buyerChar.inventory(), itemId, vs.count(), "PrivateStoreBuy");
						if (added != null) {
							List<ItemInstance> createdList = (added.allItems() != null && !added.allItems().isEmpty())
									? added.allItems()
									: (added.item() != null ? List.of(added.item()) : List.of());
							for (ItemInstance createdItem : createdList) {
								if (enchant > 0 && !createdItem.template().stackable()) {
									createdItem.enchant(enchant);
									ctx.inventories().saveItem(createdItem);
								}
								buyerUpdates.add(ItemInfo.of(createdItem, added.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED));
							}
						}
					} else {
						active.inventory().remove(vs.sellerItem(), vs.count());
						buyerChar.inventory().addItem(itemId, vs.count());
					}

					OfflineShopItem oldOrder = vs.storeOrder();
					int remaining = oldOrder.count() - vs.count();
					buyStore.items().remove(oldOrder);
					if (remaining > 0) {
						buyStore.items().add(new OfflineShopItem(oldOrder.objectId(), oldOrder.itemId(), remaining, oldOrder.price(), oldOrder.enchant(), oldOrder.bodyPart()));
					}
				}

				if (!sellerUpdates.isEmpty()) {
					session.send(new InventoryUpdate(sellerUpdates));
				}
				session.refreshWeightAndPenalties();

				if (!buyerUpdates.isEmpty()) {
					buyerOnline.send(new InventoryUpdate(buyerUpdates));
				}
				if (buyerOnline instanceof GameSession buyerSession) {
					buyerSession.refreshWeightAndPenalties();
				}

				if (buyStore.items().isEmpty()) {
					storeService.removeBuyStoreList(buyerChar.objectId());
					buyerChar.privateStoreType(PlayerCharacter.STORE_TYPE_NONE);
					buyerChar.sitting(false);
					if (buyerOnline instanceof GameSession buyerSession) {
						buyerSession.sendUserInfoAndBroadcastCharInfo();
					}
				}
			}
		}
	}

	public void handleRecipeItemMakeInfo(GameClientPacket.RequestRecipeItemMakeInfo p) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (!session.inWorld() || active == null) {
			return;
		}
		RecipeService recipeService = resolveRecipeService(ctx);
		var recipeOpt = recipeService.getRecipe(p.id());
		if (recipeOpt.isEmpty()) {
			session.send(new ActionFailed());
			return;
		}
		RecipeEntry recipe = recipeOpt.get();
		session.send(new RecipeItemMakeInfo(
				p.id(), recipe.isDwarven(), (int) active.currentMp(), active.maxMp(), -1));
	}

	public void handleRecipeItemMakeSelf(GameClientPacket.RequestRecipeItemMakeSelf p) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (!session.inWorld() || active == null || active.isDead() || active.isOlympiadMode()) {
			session.send(new ActionFailed());
			return;
		}
		RecipeService recipeService = resolveRecipeService(ctx);
		var recipeOpt = recipeService.getRecipe(p.id());
		if (recipeOpt.isEmpty()) {
			session.send(new ActionFailed());
			return;
		}
		RecipeEntry recipe = recipeOpt.get();

		if (active.currentMp() < recipe.mpCost()) {
			session.send(SystemMessage.id(SystemMessage.NOT_ENOUGH_MP));
			session.send(new RecipeItemMakeInfo(
					p.id(), recipe.isDwarven(), (int) active.currentMp(), active.maxMp(), 0));
			session.send(new ActionFailed());
			return;
		}

		for (RecipeEntry.Ingredient ing : recipe.ingredients()) {
			int have = active.inventory().byItemId(ing.itemId()).map(ItemInstance::count).orElse(0);
			if (have < ing.count()) {
				session.send(new RecipeItemMakeInfo(
						p.id(), recipe.isDwarven(), (int) active.currentMp(), active.maxMp(), 0));
				session.send(new ActionFailed());
				return;
			}
		}

		active.currentMp(active.currentMp() - recipe.mpCost());

		List<ItemInfo> updates = new ArrayList<>();
		for (RecipeEntry.Ingredient ing : recipe.ingredients()) {
			if (ctx != null && ctx.inventories() != null) {
				var consumed = ctx.inventories().consumeItem(active.inventory(), ing.itemId(), ing.count(), "Craft");
				if (consumed != null) {
					updates.add(ItemInfo.of(consumed.item(), consumed.removed() ? ItemInfo.REMOVED : ItemInfo.MODIFIED));
				}
			} else {
				active.inventory().destroyItemByItemId(ing.itemId(), ing.count());
			}
		}

		boolean success = ThreadLocalRandom.current().nextInt(100) < recipe.successRate();
		if (success) {
			if (ctx != null && ctx.inventories() != null) {
				var added = ctx.inventories().addItem(active.inventory(), recipe.productId(), recipe.productCount(), "Craft");
				if (added != null) {
					if (added.allItems() != null && !added.allItems().isEmpty()) {
						for (var it : added.allItems()) {
							updates.add(ItemInfo.of(it, added.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED));
						}
					} else if (added.item() != null) {
						updates.add(ItemInfo.of(added.item(), added.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED));
					}
				}
			} else {
				active.inventory().addItem(recipe.productId(), recipe.productCount());
			}
		}

		if (!updates.isEmpty()) {
			session.send(new InventoryUpdate(updates));
		}
		session.send(StatusUpdate.mp(active.objectId(), (int) active.currentMp(), active.maxMp()));
		session.send(new RecipeItemMakeInfo(
				p.id(), recipe.isDwarven(), (int) active.currentMp(), active.maxMp(), success ? 1 : 0));
		session.refreshWeightAndPenalties();
	}
}
