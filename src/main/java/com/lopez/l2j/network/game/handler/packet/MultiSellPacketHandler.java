package com.lopez.l2j.network.game.handler.packet;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.augmentation.Augmentation;
import com.lopez.l2j.game.augmentation.AugmentationService;
import com.lopez.l2j.game.castle.Castle;
import com.lopez.l2j.game.castle.CastleManager;
import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.multisell.MultiSellContainer;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket.MultiSellChoose;
import com.lopez.l2j.network.game.packet.GameServerPacket.ActionFailed;
import com.lopez.l2j.network.game.packet.GameServerPacket.InventoryUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.MultiSellList;
import com.lopez.l2j.network.game.packet.GameServerPacket.PledgeShowInfoUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handler modular para operacoes de MultiSell, Blacksmith of Mammon, Wear (Try-On),
 * Pet Tickets, Clan Hall Auction Currency e GoldBar Exchange (custom.properties / options.properties).
 * Implementa #268 (MaintainEnchantment + Augmentation transfer), #269 (Castle Tax calculation e credito no tesouro)
 * e #270 (NPC Proximity Validation <= 250 unidades).
 */
@Component
public class MultiSellPacketHandler {

	private static final Logger log = LoggerFactory.getLogger(MultiSellPacketHandler.class);
	private static final int ADENA_ITEM_ID = 57;
	public static final double NPC_INTERACTION_DISTANCE = 250.0;

	public record MaintainedEquipmentState(
			int enchantLevel,
			Augmentation augmentation,
			int sourceObjectId) {
	}

	public boolean doesBlacksmithRequireRecipes() {
		return Config.ALT_BLACKSMITH_USE_RECIPES;
	}

	public int getWearDelaySeconds() {
		return Math.max(1, Config.WEAR_DELAY);
	}

	public long getWearDelayMillis() {
		return getWearDelaySeconds() * 1000L;
	}

	public int getWearPriceAdena() {
		return Math.max(0, Config.WEAR_PRICE);
	}

	public boolean processWearPreview(PlayerCharacter player, int itemId) {
		if (player == null || player.isDead() || itemId <= 0) {
			return false;
		}
		int price = getWearPriceAdena();
		if (price > 0 && player.inventory() != null && !player.inventory().items().isEmpty()) {
			if (player.inventory().adena() < price) {
				return false;
			}
			if (!player.inventory().destroyItemByItemId(ADENA_ITEM_ID, price)) {
				return false;
			}
		}
		if (Config.LOG_ITEMS && !CharacterPacketHandler.getIgnoredLogItemIds().contains(itemId)) {
			log.info("[WearPreview] player={} itemId={} durationSec={} price={}",
					player.name(), itemId, getWearDelaySeconds(), price);
		}
		return true;
	}

	public boolean isValidPetTicket(int itemId, boolean specialPet) {
		if (specialPet) {
			return itemId > 0 && itemId == Config.SPECIAL_PET_TICKET_ID;
		}
		return itemId > 0 && (itemId == Config.PET_TICKET_ID || itemId == Config.SPECIAL_PET_TICKET_ID);
	}

	public int getAuctionBidCurrencyItemId() {
		return Config.AUCTION_BID_ITEM_ID > 0 ? Config.AUCTION_BID_ITEM_ID : ADENA_ITEM_ID;
	}

	public boolean isGoldBarExchangeEnabled() {
		return Config.GOLDBAR_ENABLED && Config.GOLD_BAR_ID > 0 && Config.GOLD_BAR_PRICE > 0;
	}

	public boolean areCustomTablesEnabled() {
		return Config.LOAD_CUSTOM_TELEPORTS
				|| Config.LOAD_CUSTOM_ITEM_TABLES
				|| Config.LOAD_CUSTOM_NPC_TABLE
				|| Config.LOAD_CUSTOM_DROPLIST_TABLE
				|| Config.LOAD_CUSTOM_ARMOR_SET_TABLE
				|| Config.LOAD_CUSTOM_SPAWNLIST_TABLE;
	}

	/**
	 * #270 - Valida se o jogador esta dentro da distancia maxima de interacao (<= 250 unidades)
	 * e na mesma instancia do NPC associado ao MultiSell.
	 */
	public boolean validateNpcProximity(PlayerCharacter player, NpcInstance npc, GameSession.Context ctx) {
		if (player == null) {
			return false;
		}
		NpcInstance targetNpc = npc;
		if (targetNpc == null && player.targetObjectId() > 0 && ctx != null && ctx.world() != null) {
			targetNpc = ctx.world().npc(player.targetObjectId()).orElse(null);
		}
		if (targetNpc == null) {
			return true;
		}
		if (targetNpc.instanceId() != player.instanceId()) {
			return false;
		}
		double dist = Math.hypot(player.x() - targetNpc.x(), player.y() - targetNpc.y());
		return dist <= NPC_INTERACTION_DISTANCE;
	}

	/**
	 * #269 - Resolve o castelo associado a posicao atual do NPC ou do jogador para calculo de imposto.
	 */
	public Optional<Castle> resolveNearestCastle(PlayerCharacter player, NpcInstance npc, GameSession.Context ctx) {
		if (ctx == null || ctx.castles() == null) {
			return Optional.empty();
		}
		int refX = npc != null ? npc.x() : (player != null ? player.x() : 0);
		int refY = npc != null ? npc.y() : (player != null ? player.y() : 0);
		int refZ = npc != null ? npc.z() : (player != null ? player.z() : 0);
		Optional<Castle> byZone = ctx.castles().getCastleAt(refX, refY, refZ, ctx.zones());
		if (byZone.isPresent()) {
			return byZone;
		}
		if (ctx.mapRegions() != null) {
			int townId = ctx.mapRegions().getClosestTownId(refX, refY);
			if (townId > 0) {
				return ctx.castles().getCastleByTownId(townId);
			}
		}
		return Optional.empty();
	}

	/**
	 * #269 - Calcula o imposto do castelo sobre o custo base em Adena de uma entrada MultiSell
	 * quando applyTaxes=true e maintainEnchantment=false.
	 */
	public long calculateCastleTax(MultiSellContainer container, long baseAdenaCost, Castle castle) {
		if (container == null || !container.applyTaxes() || container.maintainEnchantment()) {
			return 0L;
		}
		if (baseAdenaCost <= 0L || castle == null || castle.taxPercent() <= 0) {
			return 0L;
		}
		return Math.max(0L, Math.round(baseAdenaCost * (castle.taxPercent() / 100.0)));
	}

	/**
	 * #269 - Credita o imposto recolhido no tesouro do castelo correspondente.
	 */
	public void creditCastleTax(MultiSellContainer container, long taxAmount, Castle castle, CastleManager castleManager) {
		if (container == null || !container.applyTaxes() || taxAmount <= 0L || castle == null) {
			return;
		}
		if (castleManager != null) {
			castleManager.addToTreasury(castle.id(), taxAmount);
		} else {
			castle.addToTreasury(taxAmount);
		}
	}

	/**
	 * #268 - Extrai o nivel de encantamento e a Augmentation do equipamento ingrediente no inventario do jogador
	 * quando maintainEnchantment=true.
	 */
	public MaintainedEquipmentState extractMaintainedEquipmentState(
			PlayerCharacter player,
			MultiSellContainer container,
			int itemId,
			int requestedEnchant,
			int requestedAugmentationId) {
		if (player == null || player.inventory() == null || container == null || !container.maintainEnchantment()) {
			return new MaintainedEquipmentState(0, null, 0);
		}
		ItemInstance bestMatch = null;
		for (var invItem : player.inventory().items()) {
			if (invItem.isEquipped() || invItem.itemId() != itemId) {
				continue;
			}
			if (invItem.template() == null || invItem.template().stackable()) {
				continue;
			}
			if (requestedEnchant > 0 && invItem.enchant() == requestedEnchant) {
				if (requestedAugmentationId <= 0
						|| (invItem.augmentation() != null && invItem.augmentation().augmentationId() == requestedAugmentationId)) {
					bestMatch = invItem;
					break;
				}
			}
			if (bestMatch == null || invItem.enchant() > bestMatch.enchant()) {
				bestMatch = invItem;
			}
		}
		if (bestMatch == null) {
			return new MaintainedEquipmentState(0, null, 0);
		}
		return new MaintainedEquipmentState(bestMatch.enchant(), bestMatch.augmentation(), bestMatch.objectId());
	}

	/**
	 * #268 - Aplica o encantamento e a Augmentation preservados ao novo equipamento produzido no MultiSell.
	 */
	public void applyMaintainedEquipmentState(
			ItemInstance createdItem,
			MultiSellContainer container,
			int targetEnchant,
			Augmentation preservedAugmentation,
			AugmentationService augmentationService,
			InventoryService inventoryService) {
		if (createdItem == null || createdItem.template() == null || createdItem.template().stackable()) {
			return;
		}
		boolean changed = false;
		if (targetEnchant > 0 && createdItem.enchant() != targetEnchant) {
			createdItem.enchant(targetEnchant);
			changed = true;
		}
		if (container != null && container.maintainEnchantment() && preservedAugmentation != null && createdItem.augmentation() == null) {
			Augmentation transferred = new Augmentation(
					createdItem.objectId(),
					preservedAugmentation.augmentationId(),
					preservedAugmentation.skillId(),
					preservedAugmentation.skillLevel());
			createdItem.augmentation(transferred);
			changed = true;
		}
		if (changed && inventoryService != null) {
			inventoryService.saveItem(createdItem);
		}
	}

	/**
	 * Exibe a janela de MultiSell com calculo de taxa de castelo (#269) e preservacao de enchant (#268).
	 */
	public void showMultiSell(GameSession session, NpcInstance npc, int listId) {
		if (session == null) {
			return;
		}
		var ctx = session.context();
		if (ctx == null || ctx.multisell() == null) {
			session.send(new ActionFailed());
			return;
		}
		PlayerCharacter active = session.activeChar();
		if (active != null && npc != null && !validateNpcProximity(active, npc, ctx)) {
			session.send(SystemMessage.id(SystemMessage.TOO_FAR_FROM_NPC));
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
		Castle nearestCastle = container.applyTaxes() ? resolveNearestCastle(active, npc, ctx).orElse(null) : null;

		List<MultiSellList.MultiSellEntryView> views = new ArrayList<>();
		for (var entry : container.entries()) {
			List<MultiSellList.ItemView> ingredients = new ArrayList<>();
			int preservedEnchant = 0;
			long mergedAdena = 0L;
			int adenaViewIndex = -1;

			for (var ing : entry.ingredients()) {
				if (ing.isTaxIngredient()) {
					continue;
				}
				var t = (ctx.inventories() != null && ctx.inventories().templates() != null && ing.itemId() > 0)
						? ctx.inventories().templates().get(ing.itemId()).orElse(null)
						: null;
				if (ing.maintainIngredient() && !Config.ALT_BLACKSMITH_USE_RECIPES) {
					if (t == null || t.type2() == ItemTemplate.TYPE2_OTHER) {
						continue;
					}
				}
				int type2;
				if (ing.itemId() < 0 || ing.itemId() == 65336 || ing.itemId() == 65436) {
					type2 = 65535;
				} else {
					type2 = t != null ? t.type2() : 0;
				}
				int ingEnchant = ing.enchantLevel();
				if (container.maintainEnchantment() && active != null && active.inventory() != null
						&& t != null && !t.stackable()) {
					for (var invItem : active.inventory().items()) {
						if (!invItem.isEquipped() && invItem.itemId() == ing.itemId() && invItem.enchant() > ingEnchant) {
							ingEnchant = invItem.enchant();
							break;
						}
					}
					if (ingEnchant > preservedEnchant) {
						preservedEnchant = ingEnchant;
					}
				}
				if (ing.itemId() == ADENA_ITEM_ID && !ing.maintainIngredient()) {
					if (adenaViewIndex < 0) {
						adenaViewIndex = ingredients.size();
						ingredients.add(new MultiSellList.ItemView(ADENA_ITEM_ID, 0, type2, ing.count(), ingEnchant));
					}
					mergedAdena += ing.count();
				} else {
					ingredients.add(new MultiSellList.ItemView(ing.itemId(), 0, type2, ing.count(), ingEnchant));
				}
			}
			if (adenaViewIndex >= 0) {
				long taxAdena = calculateCastleTax(container, mergedAdena, nearestCastle);
				var prevAdena = ingredients.get(adenaViewIndex);
				ingredients.set(adenaViewIndex, new MultiSellList.ItemView(
						ADENA_ITEM_ID, 0, prevAdena.type2(), mergedAdena + taxAdena, prevAdena.enchant()));
			}

			List<MultiSellList.ItemView> products = new ArrayList<>();
			boolean stackable = true;
			for (var prod : entry.products()) {
				var t = (ctx.inventories() != null && ctx.inventories().templates() != null && prod.itemId() > 0)
						? ctx.inventories().templates().get(prod.itemId()).orElse(null)
						: null;
				int bodyPart = t != null ? t.bodyPart() : 0;
				int type2 = t != null ? t.type2() : 0;
				if (t != null && !t.stackable()) {
					stackable = false;
				}
				int prodEnchant = prod.enchantLevel() > 0 ? prod.enchantLevel()
						: (container.maintainEnchantment() && t != null && !t.stackable() ? preservedEnchant : 0);
				products.add(
						new MultiSellList.ItemView(prod.itemId(), bodyPart, type2, prod.count(), prodEnchant));
			}
			views.add(new MultiSellList.MultiSellEntryView(entry.entryId(), stackable, ingredients, products));
		}

		final int pageSize = 40;
		if (views.isEmpty()) {
			session.send(new MultiSellList(listId, 1, 1, pageSize, List.of()));
			return;
		}
		int total = views.size();
		int page = 1;
		for (int i = 0; i < total; i += pageSize) {
			int end = Math.min(i + pageSize, total);
			int finished = (end >= total) ? 1 : 0;
			session.send(new MultiSellList(listId, page++, finished, pageSize, views.subList(i, end)));
		}
	}

	/**
	 * Processa a escolha de MultiSell aplicando #268 (MaintainEnchantment + Augmentation),
	 * #269 (Castle Tax calculation e credito no tesouro) e #270 (NPC Proximity check).
	 */
	public void handleMultiSellChoose(GameSession session, MultiSellChoose p, NpcInstance lastInteractedNpc) {
		if (session == null) {
			return;
		}
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (!session.inWorld() || active == null || active.isDead() || active.isStoreOpen() || active.isBuffShop()
				|| active.isOlympiadMode() || p.amount() <= 0 || ctx == null || ctx.multisell() == null) {
			session.send(new ActionFailed());
			return;
		}
		if (!validateNpcProximity(active, lastInteractedNpc, ctx)) {
			session.send(SystemMessage.id(SystemMessage.TOO_FAR_FROM_NPC));
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
		Castle nearestCastle = container.applyTaxes() ? resolveNearestCastle(active, lastInteractedNpc, ctx).orElse(null) : null;

		// 1. Agrega ingredientes por itemId e valida quantidades totais necessarias
		Map<Integer, Long> aggregatedIngredients = new HashMap<>();
		Map<Integer, Long> maintainedIngredients = new HashMap<>();
		long clanRepNeeded = 0L;
		long baseAdenaPerUnit = 0L;
		for (var ing : entry.ingredients()) {
			if (ing.isTaxIngredient()) {
				continue;
			}
			if (ing.maintainIngredient()) {
				if (!Config.ALT_BLACKSMITH_USE_RECIPES) {
					var tmpl = ctx.inventories() != null && ctx.inventories().templates() != null
							? ctx.inventories().templates().get(ing.itemId()).orElse(null)
							: null;
					if (tmpl == null || tmpl.type2() == ItemTemplate.TYPE2_OTHER) {
						continue;
					}
				}
				maintainedIngredients.merge(ing.itemId(), Math.max(1L, ing.count()), Long::max);
				continue;
			}
			long needed = (long) ing.count() * amount;
			if (needed <= 0 || needed > Integer.MAX_VALUE) {
				session.send(new ActionFailed());
				return;
			}
			if (ing.itemId() == -200 || ing.itemId() == 65336) {
				clanRepNeeded += needed;
			} else if (ing.itemId() == -100 || ing.itemId() == 65436 || ing.itemId() == -300) {
				continue;
			} else {
				if (ing.itemId() == ADENA_ITEM_ID) {
					baseAdenaPerUnit += ing.count();
				}
				aggregatedIngredients.merge(ing.itemId(), needed, Long::sum);
			}
		}

		// #269 - Adiciona o imposto do castelo sobre o custo em Adena quando applyTaxes=true
		long totalCastleTax = 0L;
		if (baseAdenaPerUnit > 0L && container.applyTaxes() && !container.maintainEnchantment()) {
			long taxPerUnit = calculateCastleTax(container, baseAdenaPerUnit, nearestCastle);
			if (taxPerUnit > 0L) {
				totalCastleTax = taxPerUnit * amount;
				long updatedAdena = aggregatedIngredients.getOrDefault(ADENA_ITEM_ID, 0L) + totalCastleTax;
				if (updatedAdena <= 0L || updatedAdena > Integer.MAX_VALUE) {
					session.send(new ActionFailed());
					return;
				}
				aggregatedIngredients.put(ADENA_ITEM_ID, updatedAdena);
			}
		}

		// Valida Clan Reputation (-200) se exigido
		Clan playerClan = null;
		if (clanRepNeeded > 0) {
			if (active.clanId() <= 0 || ctx.clans() == null) {
				session.send(SystemMessage.id(SystemMessage.YOU_NOT_ENOUGH_ADENA));
				session.send(new ActionFailed());
				return;
			}
			playerClan = ctx.clans().byClanId(active.clanId()).orElse(null);
			if (playerClan == null || playerClan.reputationScore() < clanRepNeeded) {
				session.send(SystemMessage.id(SystemMessage.YOU_NOT_ENOUGH_ADENA));
				session.send(new ActionFailed());
				return;
			}
		}

		// Valida ingredientes mantidos (ex: recipes em maintainIngredient)
		for (var maintEntry : maintainedIngredients.entrySet()) {
			long needed = maintEntry.getValue();
			long count = active.inventory().items().stream()
					.filter(i -> !i.isEquipped() && i.itemId() == maintEntry.getKey())
					.mapToLong(ItemInstance::count)
					.sum();
			if (count < needed) {
				session.send(SystemMessage.id(SystemMessage.YOU_NOT_ENOUGH_ADENA));
				session.send(new ActionFailed());
				return;
			}
		}

		// #268 - Valida ingredientes consumiveis e rastreia enchantLevel/augmentation se maintainEnchantment=true
		int preservedEnchant = 0;
		Augmentation preservedAugmentation = null;
		int maintainedSourceObjectId = 0;
		for (var ingEntry : aggregatedIngredients.entrySet()) {
			int itemId = ingEntry.getKey();
			long needed = ingEntry.getValue();
			long count = active.inventory().items().stream()
					.filter(i -> !i.isEquipped() && i.itemId() == itemId)
					.mapToLong(ItemInstance::count)
					.sum();
			if (count < needed || needed > Integer.MAX_VALUE) {
				session.send(SystemMessage.id(SystemMessage.YOU_NOT_ENOUGH_ADENA));
				session.send(new ActionFailed());
				return;
			}
			if (container.maintainEnchantment()) {
				MaintainedEquipmentState state = extractMaintainedEquipmentState(
						active, container, itemId, p.enchant(), p.augmentationId());
				if (state.sourceObjectId() > 0) {
					if (state.enchantLevel() > preservedEnchant) {
						preservedEnchant = state.enchantLevel();
					}
					if (state.augmentation() != null && preservedAugmentation == null) {
						preservedAugmentation = state.augmentation();
					}
					if (maintainedSourceObjectId == 0) {
						maintainedSourceObjectId = state.sourceObjectId();
					}
				}
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

		if (clanRepNeeded > 0 && playerClan != null) {
			playerClan.reputationScore(playerClan.reputationScore() - (int) clanRepNeeded);
			session.send(new PledgeShowInfoUpdate(playerClan));
		}

		List<ItemInfo> updates = new ArrayList<>();
		// 3. Consome os ingredientes agregados
		for (var ingEntry : aggregatedIngredients.entrySet()) {
			int itemId = ingEntry.getKey();
			int toConsume = ingEntry.getValue().intValue();
			if (container.maintainEnchantment() && maintainedSourceObjectId > 0) {
				var specificItem = active.inventory().byObjectId(maintainedSourceObjectId).orElse(null);
				if (specificItem != null && specificItem.itemId() == itemId && !specificItem.template().stackable()) {
					if (ctx.augmentation() != null && specificItem.isAugmented()) {
						ctx.augmentation().removeAugmentation(specificItem);
					}
					var destroyed = ctx.inventories().destroyItem(active.inventory(), maintainedSourceObjectId, 1, "MultiSell");
					if (destroyed != null) {
						updates.add(destroyed.removed()
								? ItemInfo.of(destroyed.item(), ItemInfo.REMOVED)
								: ItemInfo.of(destroyed.item(), ItemInfo.MODIFIED));
					}
					toConsume -= 1;
				}
			}
			if (toConsume > 0) {
				var consumed = ctx.inventories().consumeItem(active.inventory(), itemId, toConsume, "MultiSell");
				if (consumed != null) {
					updates.add(consumed.removed()
							? ItemInfo.of(consumed.item(), ItemInfo.REMOVED)
							: ItemInfo.of(consumed.item(), ItemInfo.MODIFIED));
				}
			}
		}

		// #269 - Credita o imposto recolhido no tesouro do castelo
		if (totalCastleTax > 0L && nearestCastle != null) {
			creditCastleTax(container, totalCastleTax, nearestCastle, ctx.castles());
		}

		// 4. Adiciona os produtos (preservando encantamento/augment quando aplicavel)
		for (var prod : entry.products()) {
			long totalAdd = (long) prod.count() * amount;
			int targetEnchant = prod.enchantLevel() > 0 ? prod.enchantLevel()
					: (container.maintainEnchantment() ? preservedEnchant : 0);
			var added = ctx.inventories().addItem(active.inventory(), prod.itemId(), (int) totalAdd, "MultiSell");
			if (added != null) {
				List<ItemInstance> createdItems = (added.allItems() != null && !added.allItems().isEmpty())
						? added.allItems()
						: (added.item() != null ? List.of(added.item()) : List.of());
				for (var item : createdItems) {
					applyMaintainedEquipmentState(
							item, container, targetEnchant, preservedAugmentation, ctx.augmentation(), ctx.inventories());
					updates.add(ItemInfo.of(item, added.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED));
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
}
