package com.lopez.l2j.network.game.handler.packet;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.effect.PlayerEffects;
import com.lopez.l2j.game.effect.PlayerEffects.ActiveBuff;
import com.lopez.l2j.game.effect.ConsumableTable;
import com.lopez.l2j.game.effect.ConsumableTable.Consumable;
import com.lopez.l2j.game.item.EnchantScrollTable;
import com.lopez.l2j.game.item.EnchantTableService;
import com.lopez.l2j.game.item.EquipmentRestrictionService;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemSkillHolder;
import com.lopez.l2j.game.item.ItemSlots;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.item.ItemTemplate.Kind;
import com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.PlayerStats;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.service.InventoryService.EquipResult;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.game.skill.StatFunc;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestAutoSoulShot;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestBuyItem;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestDestroyItem;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestDropItem;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestEnchantItem;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestSellItem;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestUnEquipItem;
import com.lopez.l2j.network.game.packet.GameClientPacket.UseItem;
import com.lopez.l2j.network.game.packet.GameServerPacket.ActionFailed;
import com.lopez.l2j.network.game.packet.GameServerPacket.ChooseInventoryItem;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.EnchantResult;
import com.lopez.l2j.network.game.packet.GameServerPacket.ExAutoSoulShot;
import com.lopez.l2j.network.game.packet.GameServerPacket.InventoryUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.ItemList;
import com.lopez.l2j.network.game.packet.GameServerPacket.MagicEffectIcons;
import com.lopez.l2j.network.game.packet.GameServerPacket.MagicSkillUse;
import com.lopez.l2j.network.game.packet.GameServerPacket.SellList;
import com.lopez.l2j.network.game.packet.GameServerPacket.SocialAction;
import com.lopez.l2j.game.augmentation.AugmentationService;
import com.lopez.l2j.network.game.packet.GameServerPacket.ExShowVariationMakeWindow;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Handler modular para itens, equipamentos, consumiveis, soulshots e
 * encantamento.
 */
public class ItemPacketHandler {

	private static final Logger log = LoggerFactory.getLogger(ItemPacketHandler.class);
	private static final int MALE_MAX_HAIR_STYLE = 4;

	public record SeededMonsterState(int seedItemId, int cropItemId, int seederPlayerId, boolean harvested) {
	}

	private static final Map<Integer, SeededMonsterState> SEEDED_MONSTERS = new ConcurrentHashMap<>();
	private static final Map<Integer, Map<Integer, Integer>> SOUL_CRYSTAL_ABSORBERS = new ConcurrentHashMap<>();

	public static SeededMonsterState getSeededMonsterState(int npcObjectId) {
		return SEEDED_MONSTERS.get(npcObjectId);
	}

	public static Map<Integer, Integer> getSoulCrystalAbsorbers(int npcObjectId) {
		return SOUL_CRYSTAL_ABSORBERS.getOrDefault(npcObjectId, Map.of());
	}

	public static void clearSoulCrystalAbsorbers(int npcObjectId) {
		SOUL_CRYSTAL_ABSORBERS.remove(npcObjectId);
	}

	private final GameSession session;

	public ItemPacketHandler(GameSession session) {
		this.session = session;
	}

	public boolean isSoulshotCharged() {
		return session.soulshotCharged();
	}

	public void setSoulshotCharged(boolean charged) {
		session.soulshotCharged(charged);
	}

	public int chargedGrade() {
		return session.chargedGrade();
	}

	public void setChargedGrade(int grade) {
		session.chargedGrade(grade);
	}

	public boolean isSpiritshotCharged() {
		return session.spiritshotCharged();
	}

	public void setSpiritshotCharged(boolean charged) {
		session.spiritshotCharged(charged);
	}

	public boolean isBlessedSpiritshot() {
		return session.blessedSpiritshot();
	}

	public void setBlessedSpiritshot(boolean blessed) {
		session.blessedSpiritshot(blessed);
	}

	public int chargedSpSGrade() {
		return session.chargedSpSGrade();
	}

	public void setChargedSpSGrade(int grade) {
		session.chargedSpSGrade(grade);
	}

	public Set<Integer> autoSoulShots() {
		return session.autoSoulShots();
	}

	public int activeEnchantScrollObjectId() {
		return session.activeEnchantScrollObjectId();
	}

	public void activeEnchantScrollObjectId(int id) {
		session.activeEnchantScrollObjectId(id);
	}

	public Map<Integer, Long> consumableReuse() {
		return session.consumableReuse();
	}

	public Map<String, ScheduledFuture<?>> hotTasks() {
		return session.hotTasks();
	}

	public void handleUseItem(UseItem p) {
		PlayerCharacter active = session.activeChar();
		if (!session.inWorld() || active == null) {
			return;
		}
		var item = active.inventory().byObjectId(p.objectId()).orElse(null);
		if (item == null) {
			session.send(new ActionFailed());
			return;
		}
		var ctx = session.context();
		if (!item.template().isEquipable()) {
			if (session.isScrollOfEscape(item.itemId())) {
				if (active.isOlympiadMode() || active.isInJail() || active.team() > 0
						|| active.inventory().isEquipped(6718)
						|| (ctx != null && ((ctx.tvt() != null && ctx.tvt().isPlayerInEvent(active.objectId()))
								|| (ctx.ctf() != null && ctx.ctf().isPlayerInEvent(active.objectId()))
								|| (ctx.dm() != null && ctx.dm().isPlayerInEvent(active.objectId()))))) {
					session.send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED,
							new SystemMessage.ItemName(item.itemId())));
					session.send(new ActionFailed());
					return;
				}
				session.useScrollOfEscape(item);
				return;
			}
			if (item.itemId() == 1665 || item.itemId() == 1863) {
				session.onShowMiniMap();
				return;
			}
			if (item.itemId() == 5707) {
				session.onSSQStatus(1);
				session.send(new ActionFailed());
				return;
			}
			if (EnchantScrollTable.isEnchantScroll(item.itemId())) {
				session.activeEnchantScrollObjectId(item.objectId());
				session.send(new ChooseInventoryItem(item.itemId()));
				session.send(new ActionFailed());
				return;
			}
			if (ctx != null && ctx.summonItems() != null && ctx.summonItems().isSummonItem(item.itemId())) {
				if (ctx.summonItems().useSummonItem(active, item, session::send)) {
					session.sendUserInfoAndBroadcastCharInfo();
				}
				session.send(new ActionFailed());
				return;
			}
			if (ctx != null && ctx.extractableItems() != null && ctx.extractableItems().isExtractable(item.itemId())) {
				int maxSlots = active.maxInventorySlots() > 0 ? active.maxInventorySlots() : 80;
				if (active.inventory().size() >= maxSlots) {
					session.send(SystemMessage.id(SystemMessage.SLOTS_FULL));
					session.send(new ActionFailed());
					return;
				}
				if (ctx.extractableItems().extract(active, item, session::send)) {
					session.send(ItemList.of(active.inventory().items(), false));
					session.refreshWeightAndPenalties();
				}
				session.send(new ActionFailed());
				return;
			}
			if (isChestKey(item.itemId())) {
				useChestKey(item);
				return;
			}
			if (item.itemId() == 1661) {
				useThiefKey(item);
				return;
			}
			if (handleSpecialMiscellaneousItem(active, item)) {
				session.send(new ActionFailed());
				return;
			}
			if (Config.NOBLESSE_ITEM_ID > 0 && item.itemId() == Config.NOBLESSE_ITEM_ID) {
				useNoblesseItem(item);
				return;
			}
			if (AugmentationService.isLifeStone(item.itemId())) {
				if (active.isDead() || active.sitting()) {
					session.send(new ActionFailed());
					return;
				}
				session.send(SystemMessage.id(SystemMessage.SELECT_THE_ITEM_TO_BE_AUGMENTED));
				session.send(ExShowVariationMakeWindow.STATIC_PACKET);
				session.send(new ActionFailed());
				return;
			}
			var recipeService = (ctx != null && ctx.recipes() != null)
					? ctx.recipes()
					: com.lopez.l2j.game.recipe.RecipeService.getInstance();
			var recipeOpt = recipeService.getRecipeByItemId(item.itemId());
			if (recipeOpt.isPresent()) {
				var recipe = recipeOpt.get();
				if (!recipeService.isCraftingEnabled()) {
					session.send(SystemMessage.id(SystemMessage.CANT_CRAFT_DURING_COMBAT));
					session.send(new ActionFailed());
					return;
				}
				if (active.privateStoreType() == PlayerCharacter.STORE_MANUFACTURE) {
					session.send(SystemMessage.id(SystemMessage.CANT_ALTER_RECIPEBOOK_WHILE_CRAFTING));
					session.send(new ActionFailed());
					return;
				}
				if (recipeService.hasRecipe(active.objectId(), recipe.id())) {
					session.send(SystemMessage.id(SystemMessage.RECIPE_ALREADY_REGISTERED));
					session.send(new ActionFailed());
					return;
				}
				if (!active.isGm()) {
					if (recipe.isDwarven()) {
						int dwarfCraftLvl = active.skillLevel(172);
						if (dwarfCraftLvl <= 0
								&& (active.canCraft() || com.lopez.l2j.game.craft.DwarfCraftBonus.isDwarf(active))
								&& active.skills().isEmpty()) {
							dwarfCraftLvl = getFallbackDwarfCraftLevel(active.level());
						}
						if (dwarfCraftLvl <= 0) {
							session.send(SystemMessage.id(SystemMessage.CANT_REGISTER_NO_ABILITY_TO_CRAFT));
							session.send(new ActionFailed());
							return;
						}
						if (recipe.level() > dwarfCraftLvl) {
							session.send(SystemMessage.id(SystemMessage.CREATE_LVL_TOO_LOW_TO_REGISTER));
							session.send(new ActionFailed());
							return;
						}
						int maxDwarfRecipes = (int) Math.round(com.lopez.l2j.game.model.PlayerStats.applyStat(
								active, "DwarfRecipeLimit", Config.DWARF_RECIPE_LIMIT));
						var currentBook = recipeService.getRecipeBook(active.objectId(), true);
						if (currentBook.size() >= maxDwarfRecipes) {
							session.send(SystemMessage.of(SystemMessage.UP_TO_S1_RECIPES_CAN_REGISTER,
									new SystemMessage.Number(maxDwarfRecipes)));
							session.send(new ActionFailed());
							return;
						}
					} else {
						int commonCraftLvl = active.skillLevel(1320);
						if (commonCraftLvl <= 0 && active.skills().isEmpty()) {
							commonCraftLvl = getFallbackCommonCraftLevel(active.level());
						}
						if (commonCraftLvl <= 0) {
							session.send(SystemMessage.id(SystemMessage.CANT_REGISTER_NO_ABILITY_TO_CRAFT));
							session.send(new ActionFailed());
							return;
						}
						if (recipe.level() > commonCraftLvl) {
							session.send(SystemMessage.id(SystemMessage.CREATE_LVL_TOO_LOW_TO_REGISTER));
							session.send(new ActionFailed());
							return;
						}
						int maxCommonRecipes = (int) Math.round(com.lopez.l2j.game.model.PlayerStats.applyStat(
								active, "CommonRecipeLimit", Config.COMMON_RECIPE_LIMIT));
						var currentBook = recipeService.getRecipeBook(active.objectId(), false);
						if (currentBook.size() >= maxCommonRecipes) {
							session.send(SystemMessage.of(SystemMessage.UP_TO_S1_RECIPES_CAN_REGISTER,
									new SystemMessage.Number(maxCommonRecipes)));
							session.send(new ActionFailed());
							return;
						}
					}
				}
				if (ctx != null && ctx.inventories() != null) {
					var consumed = ctx.inventories().consumeItem(active.inventory(), item.itemId(), 1,
							"RegisterRecipe");
					if (consumed != null) {
						session.send(new InventoryUpdate(List.of(ItemInfo.of(consumed.item(),
								consumed.removed() ? ItemInfo.REMOVED : ItemInfo.MODIFIED))));
						session.refreshWeightAndPenalties();
					}
				} else {
					active.inventory().destroyItemByItemId(item.itemId(), 1);
				}
				recipeService.registerRecipe(active.objectId(), recipe.id());
				var book = recipeService.getRecipeBook(active.objectId(), recipe.isDwarven());
				session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.RecipeBookItemList(
						recipe.isDwarven(), active.maxMp(), book));
				session.send(SystemMessage.of(SystemMessage.S1_ADDED, new SystemMessage.ItemName(item.itemId())));
				session.send(new ActionFailed());
				return;
			}
			if ("recipe".equalsIgnoreCase(item.template().subType())
					|| (item.template().name() != null && item.template().name().startsWith("Recipe:"))) {
				session.send(SystemMessage.id(SystemMessage.RECIPE_INCORRECT));
				session.send(new ActionFailed());
				return;
			}
			var consumable = ConsumableTable.get(item.itemId());
			if (consumable.isPresent()) {
				useConsumable(consumable.get());
			} else {
				session.send(
						SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED, new SystemMessage.ItemName(item.itemId())));
			}
			session.send(new ActionFailed());
			return;
		}
		if (active.sitting()) {
			session.send(new ActionFailed());
			return;
		}
		if (!item.isEquipped()
				&& (!EquipmentRestrictionService.canEquip(active, item) || !canEquipSpecialItem(active, item, ctx))) {
			session.send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED, new SystemMessage.ItemName(item.itemId())));
			session.send(new ActionFailed());
			return;
		}
		if (Config.BLOCK_CHANGE_WEAPON_WHILE_ATTACKING && item.template().type2() == ItemTemplate.TYPE2_WEAPON
				&& session.isAutoAttacking()) {
			session.send(new ActionFailed());
			return;
		}
		if (ctx != null && ctx.inventories() != null) {
			afterEquipChange(ctx.inventories().toggleEquip(active.inventory(), item.objectId()));
		}
	}

	private static int getFallbackDwarfCraftLevel(int playerLevel) {
		if (playerLevel >= 70)
			return 9;
		if (playerLevel >= 62)
			return 8;
		if (playerLevel >= 55)
			return 7;
		if (playerLevel >= 49)
			return 6;
		if (playerLevel >= 43)
			return 5;
		if (playerLevel >= 36)
			return 4;
		if (playerLevel >= 28)
			return 3;
		if (playerLevel >= 20)
			return 2;
		if (playerLevel >= 5)
			return 1;
		return 0;
	}

	private static int getFallbackCommonCraftLevel(int playerLevel) {
		if (playerLevel >= 70)
			return 9;
		if (playerLevel >= 62)
			return 8;
		if (playerLevel >= 55)
			return 7;
		if (playerLevel >= 49)
			return 6;
		if (playerLevel >= 43)
			return 5;
		if (playerLevel >= 36)
			return 4;
		if (playerLevel >= 28)
			return 3;
		if (playerLevel >= 20)
			return 2;
		return playerLevel >= 1 ? 1 : 0;
	}

	private boolean canEquipSpecialItem(PlayerCharacter active, ItemInstance item, GameSession.Context ctx) {
		int itemId = item.itemId();
		// #227 - Hero Weapons (6611-6621) & Hero Circlet (6842)
		if ((itemId >= 6611 && itemId <= 6621) || itemId == 6842) {
			if (!active.isHero() && !active.isGm()) {
				return false;
			}
		}
		// #227 - Castle Lord's Crown (6841) & Castle Circlets (6834-6840, 8182-8183)
		if (com.lopez.l2j.game.castle.crown.CrownTable.isCrownOrCirclet(itemId)) {
			if (active.clanId() <= 0 || ctx == null || ctx.clans() == null) {
				return false;
			}
			var clan = ctx.clans().byClanId(active.clanId()).orElse(null);
			if (clan == null || clan.castleId() <= 0) {
				return false;
			}
			if (itemId == com.lopez.l2j.game.castle.crown.CrownTable.LORDS_CROWN && !active.isClanLeader()) {
				return false;
			}
			if (itemId != com.lopez.l2j.game.castle.crown.CrownTable.LORDS_CROWN
					&& com.lopez.l2j.game.castle.crown.CrownTable.getCircletId(clan.castleId()) != itemId) {
				return false;
			}
		}
		// #227 - Apella Armor Sets (7860-7879) require Clan membership
		if (itemId >= 7860 && itemId <= 7879) {
			if (active.clanId() <= 0) {
				return false;
			}
		}
		return true;
	}

	public static boolean isMercenaryTicket(int itemId) {
		return (itemId >= 3960 && itemId <= 4037)
				|| (itemId >= 5205 && itemId <= 5217)
				|| (itemId >= 6019 && itemId <= 6040)
				|| (itemId >= 6301 && itemId <= 6312)
				|| (itemId >= 6779 && itemId <= 6811)
				|| (itemId >= 7998 && itemId <= 8029);
	}

	public void handleUnEquip(RequestUnEquipItem p) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (!session.inWorld() || active == null || ctx == null || ctx.inventories() == null) {
			return;
		}
		if (Config.BLOCK_CHANGE_WEAPON_WHILE_ATTACKING
				&& (p.bodyPart() == ItemSlots.SLOT_R_HAND || p.bodyPart() == ItemSlots.SLOT_LR_HAND)
				&& session.isAutoAttacking()) {
			session.send(new ActionFailed());
			return;
		}
		afterEquipChange(ctx.inventories().unequipBodyPart(active.inventory(), p.bodyPart()));
	}

	public void handleDestroyItem(RequestDestroyItem p) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (!session.inWorld() || active == null || active.isDead() || active.isStoreOpen() || active.isBuffShop()
				|| ctx == null || ctx.inventories() == null) {
			session.send(new ActionFailed());
			return;
		}
		var opt = active.inventory().byObjectId(p.objectId());
		if (opt.isEmpty()) {
			session.send(new ActionFailed());
			return;
		}
		var item = opt.get();
		if (item.isEquipped() || !item.template().destroyable() || p.count() <= 0 || item.count() < p.count()) {
			session.send(new ActionFailed());
			return;
		}
		var result = ctx.inventories().destroyItem(active.inventory(), p.objectId(), p.count(), "UserDestroy");
		if (result == null) {
			session.send(new ActionFailed());
			return;
		}
		session.send(new InventoryUpdate(
				List.of(ItemInfo.of(result.item(), result.removed() ? ItemInfo.REMOVED : ItemInfo.MODIFIED))));
		session.refreshWeightAndPenalties();
		session.send(new ActionFailed());
	}

	public void handleDropItem(RequestDropItem p) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (!session.inWorld() || active == null || active.isDead() || active.isOlympiadMode()
				|| active.isStoreOpen() || active.isBuffShop() || ctx == null || ctx.inventories() == null) {
			session.send(new ActionFailed());
			return;
		}
		if (p.count() <= 0) {
			session.send(new ActionFailed());
			return;
		}
		var itemOpt = active.inventory().byObjectId(p.objectId());
		if (itemOpt.isEmpty()) {
			session.send(new ActionFailed());
			return;
		}
		var item = itemOpt.get();
		if (item.isEquipped() || !item.template().dropable() || item.count() < p.count()) {
			session.send(SystemMessage.id(SystemMessage.CANNOT_DISCARD_THIS_ITEM));
			session.send(new ActionFailed());
			return;
		}
		if (ctx.mercenaries() != null && active.clanId() > 0 && isMercenaryTicket(item.itemId())) {
			var clan = ctx.clans() != null ? ctx.clans().byClanId(active.clanId()).orElse(null) : null;
			boolean hasMercPriv = active.isClanLeader()
					|| (active.clanPrivileges() & com.lopez.l2j.game.clan.Clan.CP_CS_MERCENARIES) != 0;
			if (clan != null && clan.castleId() > 0 && hasMercPriv) {
				if (ctx.sevenSigns() != null) {
					ctx.mercenaries().setSevenSignsManager(ctx.sevenSigns());
				}
				int prevCount = item.count();
				if (ctx.mercenaries().dropMercenaryTicket(active, clan, item.itemId(), p.x(), p.y(), p.z(),
						active.heading())) {
					var remaining = active.inventory().byObjectId(item.objectId()).orElse(null);
					session.send(new InventoryUpdate(List.of(
							ItemInfo.of(remaining != null ? remaining : item,
									(remaining == null || prevCount <= 1) ? ItemInfo.REMOVED : ItemInfo.MODIFIED))));
					session.refreshWeightAndPenalties();
					session.send(new ActionFailed());
					return;
				}
			}
			session.send(SystemMessage.id(SystemMessage.CANNOT_DISCARD_THIS_ITEM));
			session.send(new ActionFailed());
			return;
		}
		var result = ctx.inventories().destroyItem(active.inventory(), p.objectId(), p.count(), "Drop");
		if (result == null) {
			session.send(new ActionFailed());
			return;
		}
		session.send(new InventoryUpdate(List.of(
				ItemInfo.of(result.item(), result.removed() ? ItemInfo.REMOVED : ItemInfo.MODIFIED))));
		session.refreshWeightAndPenalties();

		if (ctx.groundItems() != null) {
			ctx.groundItems().dropItem(active.objectId(), item.itemId(), p.count(), active.x(), active.y(), active.z());
		}
		session.send(new ActionFailed());
	}

	public void handleEnchantItem(RequestEnchantItem p) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (!session.inWorld() || active == null || active.isDead() || active.isOlympiadMode()
				|| active.isStoreOpen() || active.isBuffShop() || ctx == null || ctx.inventories() == null) {
			session.send(EnchantResult.CANCEL);
			session.send(new ActionFailed());
			return;
		}
		int scrollObjectId = session.activeEnchantScrollObjectId();
		session.activeEnchantScrollObjectId(0);
		if (scrollObjectId == 0) {
			session.send(EnchantResult.CANCEL);
			session.send(new ActionFailed());
			return;
		}
		var scrollOpt = active.inventory().byObjectId(scrollObjectId);
		if (scrollOpt.isEmpty()) {
			session.send(EnchantResult.CANCEL);
			session.send(new ActionFailed());
			return;
		}
		var scroll = scrollOpt.get();
		var scrollInfoOpt = EnchantScrollTable.get(scroll.itemId());
		if (scrollInfoOpt.isEmpty()) {
			session.send(EnchantResult.CANCEL);
			session.send(new ActionFailed());
			return;
		}
		var scrollInfo = scrollInfoOpt.get();
		var targetOpt = active.inventory().byObjectId(p.objectId());
		if (targetOpt.isEmpty()) {
			session.send(EnchantResult.CANCEL);
			session.send(new ActionFailed());
			return;
		}
		var target = targetOpt.get();
		int targetItemId = target.itemId();
		if (!target.template().isEquipable()
				|| targetItemId == 8190 || targetItemId == 8689
				|| target.mana() > 0
				|| "rod".equalsIgnoreCase(target.template().weaponType())
				|| com.lopez.l2j.game.pet.PetDataTable.isPetWeapon(targetItemId)
				|| com.lopez.l2j.game.pet.PetDataTable.isPetArmor(targetItemId)) {
			session.send(SystemMessage.id(SystemMessage.INAPPROPRIATE_ENCHANT_CONDITION));
			session.send(EnchantResult.CANCEL);
			return;
		}

		if (!EnchantTableService.isScrollAllowed(scrollInfo)) {
			session.send(SystemMessage.id(SystemMessage.INAPPROPRIATE_ENCHANT_CONDITION));
			session.send(EnchantResult.CANCEL);
			return;
		}

		if (EnchantTableService.isOverEnchant(target, scrollInfo)) {
			session.send(SystemMessage.id(SystemMessage.INAPPROPRIATE_ENCHANT_CONDITION));
			session.send(EnchantResult.CANCEL);
			return;
		}

		String targetGrade = target.template().crystalType();
		boolean gradeMatch = "all".equalsIgnoreCase(scrollInfo.grade())
				|| (targetGrade != null && targetGrade.equalsIgnoreCase(scrollInfo.grade()));
		if (!gradeMatch) {
			session.send(SystemMessage.id(SystemMessage.INAPPROPRIATE_ENCHANT_CONDITION));
			session.send(EnchantResult.CANCEL);
			return;
		}

		if (!Config.HERO_WEAPONS_CAN_BE_ENCHANTED
				&& ((targetItemId >= 6611 && targetItemId <= 6621) || targetItemId == 6842)) {
			session.send(SystemMessage.id(SystemMessage.INAPPROPRIATE_ENCHANT_CONDITION));
			session.send(EnchantResult.CANCEL);
			return;
		}

		boolean isWeapon = target.template().type2() == ItemTemplate.TYPE2_WEAPON;
		if (scrollInfo.isWeapon() != isWeapon) {
			session.send(SystemMessage.id(SystemMessage.INAPPROPRIATE_ENCHANT_CONDITION));
			session.send(EnchantResult.CANCEL);
			return;
		}

		var consumedScroll = ctx.inventories().destroyItem(active.inventory(), scroll.objectId(), 1, "Enchant");
		if (consumedScroll == null) {
			session.send(EnchantResult.CANCEL);
			return;
		}
		session.send(new InventoryUpdate(List.of(
				ItemInfo.of(consumedScroll.item(), consumedScroll.removed() ? ItemInfo.REMOVED : ItemInfo.MODIFIED))));

		int chance = EnchantTableService.getEnchantChance(scrollInfo, target, active.race());
		boolean success = java.util.concurrent.ThreadLocalRandom.current().nextInt(100) < chance;

		if (success) {
			target.enchant(target.enchant() + 1);
			ctx.inventories().saveItem(target);
			session.send(EnchantResult.SUCCESS);
			if (target.enchant() == 1) {
				session.send(SystemMessage.of(SystemMessage.S1_SUCCESSFULLY_ENCHANTED,
						new SystemMessage.ItemName(target.itemId())));
			} else {
				session.send(SystemMessage.of(SystemMessage.S1_S2_SUCCESSFULLY_ENCHANTED,
						new SystemMessage.Number(target.enchant()), new SystemMessage.ItemName(target.itemId())));
			}
			if (target.isEquipped()) {
				updateArmorSetBonus();
				updateEquippedItemSkills();
				session.sendUserInfoAndBroadcastCharInfo();
			}
			session.send(new InventoryUpdate(List.of(ItemInfo.of(target, ItemInfo.MODIFIED))));
			session.broadcastAppearance();
		} else {
			int failEnchant = EnchantTableService.calculateFailureEnchant(scrollInfo, target);
			if (failEnchant >= 0) {
				target.enchant(failEnchant);
				ctx.inventories().saveItem(target);
				session.send(SystemMessage.id(SystemMessage.BLESSED_ENCHANT_FAILED));
				session.send(EnchantResult.BLESSED_FAIL);
				if (target.isEquipped()) {
					updateArmorSetBonus();
					updateEquippedItemSkills();
					session.sendUserInfoAndBroadcastCharInfo();
				}
				session.send(new InventoryUpdate(List.of(ItemInfo.of(target, ItemInfo.MODIFIED))));
				session.broadcastAppearance();
			} else {
				int oldEnchant = target.enchant();
				int itemId = target.itemId();
				if (target.isEquipped()) {
					afterEquipChange(ctx.inventories().toggleEquip(active.inventory(), target.objectId()));
				}
				ctx.inventories().destroyItem(active.inventory(), target.objectId(), 1, "EnchantBreak");
				if (oldEnchant > 0) {
					session.send(SystemMessage.of(SystemMessage.ENCHANTMENT_FAILED_S1_S2_EVAPORATED,
							new SystemMessage.Number(oldEnchant), new SystemMessage.ItemName(itemId)));
				} else {
					session.send(SystemMessage.of(SystemMessage.ENCHANTMENT_FAILED_S1_EVAPORATED,
							new SystemMessage.ItemName(itemId)));
				}
				session.send(EnchantResult.FAIL);
				session.send(new InventoryUpdate(List.of(ItemInfo.of(target, ItemInfo.REMOVED))));

				// Cristais gerados pela quebra do item
				int crystalId = EnchantTableService.getCrystalId(target.template().crystalType());
				int crystalCount = EnchantTableService.calculateCrystalsOnBreak(target);
				if (crystalId > 0 && crystalCount > 0) {
					var addedCrystals = ctx.inventories().addItem(active.inventory(), crystalId, crystalCount,
							"EnchantBreak");
					if (addedCrystals != null) {
						session.send(new InventoryUpdate(List.of(
								ItemInfo.of(addedCrystals.item(),
										addedCrystals.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED))));
					}
					session.send(SystemMessage.of(SystemMessage.EARNED_S2_S1_S,
							new SystemMessage.ItemName(crystalId), new SystemMessage.ItemNumber(crystalCount)));
				}
				session.refreshWeightAndPenalties();
				session.broadcastAppearance();
			}
		}
	}

	public void handleAutoSoulShot(RequestAutoSoulShot p) {
		PlayerCharacter active = session.activeChar();
		if (!session.inWorld() || active == null || active.isDead()) {
			return;
		}
		if (!ConsumableTable.isShot(p.itemId()) && !isBeastShot(p.itemId())) {
			session.send(new ExAutoSoulShot(p.itemId(), 0));
			return;
		}
		if (active.inventory().byItemId(p.itemId()).isEmpty()) {
			return;
		}
		var name = new SystemMessage.ItemName(p.itemId());
		if (p.type() == 1) {
			session.autoSoulShots().add(p.itemId());
			session.send(new ExAutoSoulShot(p.itemId(), 1));
			session.send(SystemMessage.of(SystemMessage.USE_OF_S1_WILL_BE_AUTO, name));
			if (isBeastShot(p.itemId())) {
				chargeBeastShot(p.itemId(), false);
			} else if (ConsumableTable.isSoulshot(p.itemId())) {
				chargeSoulShot(p.itemId(), false);
			} else {
				chargeSpiritShot(p.itemId(), false, ConsumableTable.isBlessedSpiritshot(p.itemId()));
			}
		} else {
			session.autoSoulShots().remove(p.itemId());
			session.send(new ExAutoSoulShot(p.itemId(), 0));
			session.send(SystemMessage.of(SystemMessage.AUTO_USE_OF_S1_CANCELLED, name));
		}
	}

	public void handleBuyItem(RequestBuyItem p) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (!session.inWorld() || p.items().isEmpty() || active == null || active.isDead() || active.isOlympiadMode()
				|| active.isStoreOpen() || active.isBuffShop() || ctx == null || ctx.buylists() == null) {
			session.send(new ActionFailed());
			return;
		}
		if (!Config.ALT_KARMA_PLAYER_CAN_SHOP && active.karma() > 0) {
			session.send(new ActionFailed());
			return;
		}
		var blOpt = ctx.buylists().get(p.listId());
		if (blOpt.isEmpty()) {
			session.send(new ActionFailed());
			return;
		}
		var bl = blOpt.get();
		long totalCost = 0;
		int slots = 0;

		for (var itemReq : p.items()) {
			if (itemReq.count() <= 0) {
				session.send(new ActionFailed());
				return;
			}
			var prodOpt = bl.getProduct(itemReq.itemId());
			if (prodOpt.isEmpty()) {
				session.send(new ActionFailed());
				return;
			}
			var prod = prodOpt.get();
			var template = ctx.inventories().templates().get(prod.itemId()).orElse(null);
			if (template == null) {
				session.send(new ActionFailed());
				return;
			}
			totalCost += (long) prod.price() * itemReq.count();
			if (!template.stackable()) {
				slots += itemReq.count();
			} else if (active.inventory().byItemId(prod.itemId()).isEmpty()) {
				slots++;
			}
		}

		if (totalCost > Integer.MAX_VALUE || totalCost < 0) {
			session.send(new ActionFailed());
			return;
		}

		if (active.inventory().adena() < totalCost) {
			session.send(SystemMessage.id(SystemMessage.YOU_NOT_ENOUGH_ADENA));
			session.send(new ActionFailed());
			return;
		}

		if (active.inventory().size() + slots > active.maxInventorySlots()) {
			session.send(SystemMessage.id(SystemMessage.SLOTS_FULL));
			session.send(new ActionFailed());
			return;
		}

		List<ItemInfo> updates = new ArrayList<>();
		if (totalCost > 0) {
			var consumed = ctx.inventories().consumeItem(active.inventory(), ItemTemplate.ADENA_ID, (int) totalCost,
					"Buy");
			if (consumed != null) {
				updates.add(consumed.removed()
						? ItemInfo.of(consumed.item(), ItemInfo.REMOVED)
						: ItemInfo.of(consumed.item(), ItemInfo.MODIFIED));
			}
		}

		for (var itemReq : p.items()) {
			var added = ctx.inventories().addItem(active.inventory(), itemReq.itemId(), itemReq.count(), "Buy");
			if (added != null) {
				updates.add(ItemInfo.of(added.item(), added.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED));
			}
		}

		session.send(new InventoryUpdate(updates));
		session.refreshWeightAndPenalties();
	}

	public void handleSellItem(RequestSellItem p) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (!session.inWorld() || p.items().isEmpty() || active == null || active.isDead() || active.isOlympiadMode()
				|| active.isStoreOpen() || active.isBuffShop() || ctx == null || ctx.inventories() == null) {
			session.send(new ActionFailed());
			return;
		}
		java.util.Set<Integer> processedObjects = new java.util.HashSet<>();
		long totalEarned = 0;
		for (var req : p.items()) {
			if (req.count() <= 0 || !processedObjects.add(req.objectId())) {
				session.send(new ActionFailed());
				return;
			}
			var itOpt = active.inventory().byObjectId(req.objectId());
			if (itOpt.isEmpty()) {
				continue;
			}
			var item = itOpt.get();
			if (item.isEquipped() || item.template().type2() == ItemTemplate.TYPE2_QUEST) {
				continue;
			}
			int count = Math.min((int) item.count(), Math.max(1, req.count()));
			if (Config.SET_MAX_ETC_ITEM_SELL && item.template().type2() == ItemTemplate.TYPE2_OTHER) {
				count = Math.min(count, Config.SET_MAX_ETC_ITEM_SELL_QNT);
			}
			int pricePerItem = Math.max(1, item.template().price() / 2);
			totalEarned += (long) pricePerItem * count;
		}

		if (Config.L2OFF_ADENA_PROTECTION && ((long) active.inventory().adena() + totalEarned > Integer.MAX_VALUE)) {
			session.send(SystemMessage.id(SystemMessage.SLOTS_FULL));
			session.send(new ActionFailed());
			return;
		}

		List<ItemInfo> updates = new ArrayList<>();
		for (var req : p.items()) {
			var itOpt = active.inventory().byObjectId(req.objectId());
			if (itOpt.isEmpty()) {
				continue;
			}
			var item = itOpt.get();
			if (item.isEquipped() || item.template().type2() == ItemTemplate.TYPE2_QUEST) {
				continue;
			}
			int count = Math.min((int) item.count(), Math.max(1, req.count()));
			if (Config.SET_MAX_ETC_ITEM_SELL && item.template().type2() == ItemTemplate.TYPE2_OTHER) {
				count = Math.min(count, Config.SET_MAX_ETC_ITEM_SELL_QNT);
			}
			var upd = ctx.inventories().destroyItem(active.inventory(), item.objectId(), count, "Sell");
			if (upd != null) {
				updates.add(upd.removed()
						? ItemInfo.of(upd.item(), ItemInfo.REMOVED)
						: ItemInfo.of(upd.item(), ItemInfo.MODIFIED));
			}
		}
		if (totalEarned > 0) {
			var adenaUpd = ctx.inventories().addItem(active.inventory(), ItemTemplate.ADENA_ID, (int) totalEarned,
					"SellReward");
			if (adenaUpd != null) {
				updates.add(ItemInfo.of(adenaUpd.item(), adenaUpd.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED));
			}
		}
		if (!updates.isEmpty()) {
			session.send(new InventoryUpdate(updates));
			session.send(ItemList.of(active.inventory().items(), false));
			session.refreshWeightAndPenalties();
		}
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

	public void afterEquipChange(EquipResult r) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (!r.ok() || active == null) {
			session.send(new ActionFailed());
			return;
		}
		var item = r.item();
		var name = new SystemMessage.ItemName(item.itemId());
		if (r.equipped()) {
			session.send(item.enchant() > 0
					? SystemMessage.of(SystemMessage.S1_S2_EQUIPPED, new SystemMessage.Number(item.enchant()), name)
					: SystemMessage.of(SystemMessage.S1_EQUIPPED, name));
		} else {
			session.send(item.enchant() > 0
					? SystemMessage.of(SystemMessage.EQUIPMENT_S1_S2_REMOVED, new SystemMessage.Number(item.enchant()),
							name)
					: SystemMessage.of(SystemMessage.S1_DISARMED, name));
		}
		// #225 - Ao equipar um BOW, localiza e equipa automaticamente flechas da mesma
		// grade na mao esquerda
		List<ItemInstance> modifiedItems = new ArrayList<>(r.changed());
		if (r.equipped() && item.template() != null && "bow".equalsIgnoreCase(item.template().weaponType())
				&& ctx != null && ctx.inventories() != null) {
			int arrowId = resolveArrowItemIdForGrade(item.template().crystalType());
			var currentLHand = active.inventory().paperdoll(ItemSlots.LHAND);
			if (arrowId > 0 && (currentLHand == null || currentLHand.itemId() != arrowId)) {
				var arrowItem = active.inventory().byItemId(arrowId).orElse(null);
				if (arrowItem != null && !arrowItem.isEquipped()) {
					var arrowEquip = ctx.inventories().toggleEquip(active.inventory(), arrowItem.objectId());
					if (arrowEquip.ok()) {
						modifiedItems.addAll(arrowEquip.changed());
					}
				}
			}
		}
		updateArmorSetBonus();
		updateEquippedItemSkills();
		updateAugmentationBonus();
		var t = ctx != null && ctx.characters() != null ? ctx.characters().template(active) : null;
		if (t != null) {
			session.recalcMaxVitals(t);
		}
		session.sendUserInfoAndBroadcastCharInfo();
		session.send(InventoryUpdate.modified(modifiedItems));
		if (t != null) {
			var stats = PlayerStats.calculate(active, t);
			session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.EtcStatusUpdate(active, stats));
		}
		if (r.changed().stream().anyMatch(i -> i.template().kind() == Kind.WEAPON)) {
			session.soulshotCharged(false);
			session.spiritshotCharged(false);
			rechargeAutoSoulShots();
		}
	}

	private static int resolveArrowItemIdForGrade(String crystalType) {
		if (crystalType == null) {
			return 17;
		}
		return switch (crystalType.toLowerCase(java.util.Locale.ROOT)) {
			case "d" -> 1341;
			case "c" -> 1342;
			case "b" -> 1343;
			case "a" -> 1344;
			case "s", "s80", "s84" -> 1345;
			default -> 17;
		};
	}

	public void updateArmorSetBonus() {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (ctx == null || ctx.armorSets() == null || active == null) {
			return;
		}
		var set = ctx.armorSets().findMatchingSet(active.inventory());
		Map<Integer, Integer> newSetSkillLevels = new HashMap<>();
		if (set != null) {
			if (set.skillId() > 0) {
				newSetSkillLevels.put(set.skillId(), Math.max(1, set.skillLvl()));
			}
			if (set.skillSetId() > 0) {
				newSetSkillLevels.putIfAbsent(set.skillSetId(), 1);
			}
			if (set.shieldSkillId() > 0 && set.hasShield(active.inventory())) {
				newSetSkillLevels.put(set.shieldSkillId(), 1);
			}
			if (set.enchant6Skill() > 0 && set.isEnchanted6(active.inventory())) {
				newSetSkillLevels.put(set.enchant6Skill(), 1);
			}
		}

		Set<Integer> newSetSkills = newSetSkillLevels.keySet();
		Set<Integer> currentSetSkills = active.armorSetSkillIds();
		boolean changed = !currentSetSkills.equals(newSetSkills);

		for (int skId : currentSetSkills) {
			if (!newSetSkills.contains(skId)) {
				active.skills().remove(skId);
			}
		}

		for (var entry : newSetSkillLevels.entrySet()) {
			int prevLvl = active.skills().getOrDefault(entry.getKey(), 0);
			if (prevLvl != entry.getValue()) {
				changed = true;
			}
			active.skills().put(entry.getKey(), entry.getValue());
		}

		currentSetSkills.clear();
		currentSetSkills.addAll(newSetSkills);

		if (set == null) {
			active.clearArmorSetBonus();
		} else {
			active.setArmorSetBonus(set.chest(), set.statFuncs());
		}

		if (changed) {
			if (ctx.skillService() != null) {
				ctx.skillService().refreshPassives(active);
			}
			var t = ctx.characters() != null ? ctx.characters().template(active) : null;
			if (t != null) {
				session.recalcMaxVitals(t);
			}
			session.sendSkillList();
		}
	}

	public void updateEquippedItemSkills() {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (active == null || active.inventory() == null) {
			return;
		}
		Map<Integer, Integer> newItemSkills = new HashMap<>();
		for (ItemInstance item : active.inventory().equipped()) {
			var tpl = item.template();
			if (tpl == null) {
				continue;
			}
			for (ItemSkillHolder h : tpl.itemSkills()) {
				newItemSkills.merge(h.skillId(), h.level(), Math::max);
			}
			if (tpl.kind() == Kind.WEAPON && item.enchant() >= 4 && tpl.enchant4Skill() != null) {
				var e4 = tpl.enchant4Skill();
				newItemSkills.merge(e4.skillId(), e4.level(), Math::max);
			}
		}

		Map<Integer, Integer> currentItemSkills = active.equippedItemSkills();
		boolean changed = !currentItemSkills.equals(newItemSkills);

		for (var entry : currentItemSkills.entrySet()) {
			int skillId = entry.getKey();
			if (!newItemSkills.containsKey(skillId)) {
				active.skills().remove(skillId);
			}
		}

		for (var entry : newItemSkills.entrySet()) {
			active.skills().put(entry.getKey(), entry.getValue());
		}

		currentItemSkills.clear();
		currentItemSkills.putAll(newItemSkills);

		if (changed) {
			if (ctx != null && ctx.skillService() != null) {
				ctx.skillService().refreshPassives(active);
			}
			var t = ctx != null && ctx.characters() != null ? ctx.characters().template(active) : null;
			if (t != null) {
				session.recalcMaxVitals(t);
			}
			session.sendSkillList();
		}
	}

	public void updateAugmentationBonus() {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (active == null) {
			return;
		}
		var weapon = activeWeapon();
		int oldSkillId = active.activeAugmentationSkillId();
		if (weapon == null || !weapon.isAugmented() || ctx == null || ctx.augmentation() == null) {
			if (oldSkillId > 0) {
				active.skills().remove(oldSkillId);
				if (AugmentationService.shouldCancelAugmentationEffectOnUnequip() && active.effects() != null) {
					active.effects().removeSkill(oldSkillId);
					session.refreshBuffs();
				}
				if (ctx != null && ctx.skillService() != null) {
					ctx.skillService().refreshPassives(active);
				}
				session.sendSkillList();
			}
			active.clearAugmentationBonus();
			return;
		}
		active.clearAugmentationBonus();
		var aug = weapon.augmentation();
		var stats = ctx.augmentation().getAugStatsById(aug.attributes());
		List<StatFunc> funcs = new ArrayList<>();
		for (var s : stats) {
			switch (s.stat()) {
				case "pAtk" -> funcs.add(new StatFunc("pAtk", StatFunc.Op.ADD, 0x40, s.value()));
				case "mAtk" -> funcs.add(new StatFunc("mAtk", StatFunc.Op.ADD, 0x40, s.value()));
				case "pDef" -> funcs.add(new StatFunc("pDef", StatFunc.Op.ADD, 0x40, s.value()));
				case "mDef" -> funcs.add(new StatFunc("mDef", StatFunc.Op.ADD, 0x40, s.value()));
				case "rCrit" -> funcs.add(new StatFunc("rCrit", StatFunc.Op.ADD, 0x40, s.value()));
				case "accCombat" -> funcs.add(new StatFunc("accCombat", StatFunc.Op.ADD, 0x40, s.value()));
				case "rEvas" -> funcs.add(new StatFunc("rEvas", StatFunc.Op.ADD, 0x40, s.value()));
				case "maxHp" -> funcs.add(new StatFunc("maxHp", StatFunc.Op.ADD, 0x40, s.value()));
				case "maxMp" -> funcs.add(new StatFunc("maxMp", StatFunc.Op.ADD, 0x40, s.value()));
				case "maxCp" -> funcs.add(new StatFunc("maxCp", StatFunc.Op.ADD, 0x40, s.value()));
				case "regHp" -> funcs.add(new StatFunc("regHp", StatFunc.Op.ADD, 0x40, s.value()));
				case "regMp" -> funcs.add(new StatFunc("regMp", StatFunc.Op.ADD, 0x40, s.value()));
				case "regCp" -> funcs.add(new StatFunc("regCp", StatFunc.Op.ADD, 0x40, s.value()));
				case "STR" -> active.augSTR(active.augSTR() + (int) s.value());
				case "DEX" -> active.augDEX(active.augDEX() + (int) s.value());
				case "CON" -> active.augCON(active.augCON() + (int) s.value());
				case "INT" -> active.augINT(active.augINT() + (int) s.value());
				case "WIT" -> active.augWIT(active.augWIT() + (int) s.value());
				case "MEN" -> active.augMEN(active.augMEN() + (int) s.value());
			}
		}
		active.augmentationFuncs(funcs);

		if (aug.hasSkill()) {
			active.setAugmentationSkill(aug.skillId(), aug.skillLevel());
			active.skills().put(aug.skillId(), aug.skillLevel());
			if (ctx.skillService() != null) {
				ctx.skillService().refreshPassives(active);
			}
			session.sendSkillList();
		} else if (oldSkillId > 0) {
			active.skills().remove(oldSkillId);
			if (ctx.skillService() != null) {
				ctx.skillService().refreshPassives(active);
			}
			session.sendSkillList();
		}
	}

	public void useConsumable(Consumable c) {
		PlayerCharacter active = session.activeChar();
		if (active == null || active.isDead()) {
			return;
		}
		if (c.type() == ConsumableTable.Type.SOULSHOT) {
			chargeSoulShot(c.itemId(), false);
			return;
		}
		if (c.type() == ConsumableTable.Type.SPIRITSHOT || c.type() == ConsumableTable.Type.BLESSED_SPIRITSHOT) {
			chargeSpiritShot(c.itemId(), false, c.type() == ConsumableTable.Type.BLESSED_SPIRITSHOT);
			return;
		}
		long now = System.currentTimeMillis();
		Long readyAt = session.consumableReuse().get(c.skillId());
		if (readyAt != null && readyAt > now) {
			session.send(SystemMessage.of(SystemMessage.S1_PREPARED_FOR_REUSE, new SystemMessage.ItemName(c.itemId())));
			return;
		}
		if (c.type() == ConsumableTable.Type.HAIR_STYLE && !active.female() && c.amount() > MALE_MAX_HAIR_STYLE) {
			session.send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED, new SystemMessage.ItemName(c.itemId())));
			return;
		}
		if ((c.type() == ConsumableTable.Type.HEAL_MP || c.type() == ConsumableTable.Type.HOT_MP)
				&& !Config.ALLOW_MANA_POTIONS) {
			session.send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED, new SystemMessage.ItemName(c.itemId())));
			return;
		}
		if (c.type() == ConsumableTable.Type.ENERGY_STONE) {
			int cls = active.classId();
			boolean canUseEnergyStone = active.isGm()
					|| cls == 1 || cls == 2 || cls == 3 || cls == 45 || cls == 47 || cls == 48
					|| cls == 88 || cls == 89 || cls == 114
					|| active.skillLevel(50) > 0 || active.skillLevel(8) > 0;
			if (!canUseEnergyStone) {
				session.send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED, new SystemMessage.ItemName(c.itemId())));
				return;
			}
			if (active.charges() >= 2) {
				session.send(SystemMessage.id(SystemMessage.FORCE_MAXLEVEL_REACHED));
				return;
			}
		}
		if (c.type() == ConsumableTable.Type.ELIXIR_HP
				|| c.type() == ConsumableTable.Type.ELIXIR_MP
				|| c.type() == ConsumableTable.Type.ELIXIR_CP) {
			if (active.isOlympiadMode() || active.level() < elixirMinLevel(c.grade())) {
				session.send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED, new SystemMessage.ItemName(c.itemId())));
				return;
			}
		}
		GameSession resTarget = null;
		com.lopez.l2j.game.summon.SummonInstance resPetTarget = null;
		if (c.type() == ConsumableTable.Type.RESURRECT_SCROLL) {
			var ctx = session.context();
			if (session.targetObjectId() == 0 || session.targetObjectId() == active.objectId()
					|| ctx == null || ctx.world() == null) {
				session.send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
				return;
			}
			var mySum = active.activeSummon();
			if (mySum != null && mySum.objectId() == session.targetObjectId() && mySum.isDead()) {
				resPetTarget = mySum;
			} else {
				var other = ctx.world().player(session.targetObjectId()).orElse(null);
				if (!(other instanceof GameSession gs) || gs.active() == null || !gs.active().isDead()
						|| active.isOlympiadMode() || gs.active().isOlympiadMode()) {
					session.send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
					return;
				}
				double dist = Math.hypot(active.x() - gs.active().x(), active.y() - gs.active().y());
				if (dist > 400.0) {
					session.send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
					return;
				}
				resTarget = gs;
			}
		}
		if (!consumeItem(c.itemId(), 1)) {
			return;
		}
		if (c.reuseMs() > 0) {
			session.consumableReuse().put(c.skillId(), now + c.reuseMs());
		}
		session.send(SystemMessage.of(SystemMessage.USE_S1, new SystemMessage.ItemName(c.itemId())));
		if (c.type() == ConsumableTable.Type.RESURRECT_SCROLL && resPetTarget != null) {
			session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.SetupGauge(
					com.lopez.l2j.network.game.packet.GameServerPacket.SetupGauge.BLUE, 2000));
			var msu = new MagicSkillUse(active.objectId(), resPetTarget.objectId(), c.skillId(), c.level(), 2000,
					c.reuseMs(),
					active.x(), active.y(), active.z(), resPetTarget.x(), resPetTarget.y(), resPetTarget.z());
			session.send(msu);
			if (session.context() != null && session.context().world() != null) {
				session.context().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, msu, false);
			}
			double expRestore = c.itemId() == 3936 || c.itemId() == 6387 ? 100.0 : 0.0;
			com.lopez.l2j.game.pet.PetService.getInstance().resurrectPet(active, resPetTarget, expRestore,
					session::send);
			return;
		} else if (c.type() == ConsumableTable.Type.RESURRECT_SCROLL && resTarget != null
				&& resTarget.active() != null) {
			var tgt = resTarget.active();
			session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.SetupGauge(
					com.lopez.l2j.network.game.packet.GameServerPacket.SetupGauge.BLUE, 2000));
			var msu = new MagicSkillUse(active.objectId(), tgt.objectId(), c.skillId(), c.level(), 2000, c.reuseMs(),
					active.x(), active.y(), active.z(), tgt.x(), tgt.y(), tgt.z());
			session.send(msu);
			if (session.context() != null && session.context().world() != null) {
				session.context().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, msu, false);
			}
		} else {
			session.broadcastSelfSkill(c.skillId(), c.level());
		}

		switch (c.type()) {
			case HEAL_HP, ELIXIR_HP -> {
				double before = active.currentHp();
				active.currentHp(before + c.amount());
				session.send(SystemMessage.of(SystemMessage.S1_HP_RESTORED,
						new SystemMessage.Number((int) (active.currentHp() - before))));
				session.sendVitals();
			}
			case HEAL_MP -> {
				double power = Config.MANA_POTION_POWER > 0 ? Config.MANA_POTION_POWER : c.amount();
				double before = active.currentMp();
				active.currentMp(before + power);
				session.send(SystemMessage.of(SystemMessage.S1_MP_RESTORED,
						new SystemMessage.Number((int) (active.currentMp() - before))));
				session.sendVitals();
			}
			case ELIXIR_MP -> {
				double before = active.currentMp();
				active.currentMp(before + c.amount());
				session.send(SystemMessage.of(SystemMessage.S1_MP_RESTORED,
						new SystemMessage.Number((int) (active.currentMp() - before))));
				session.sendVitals();
			}
			case HEAL_CP, ELIXIR_CP -> {
				double before = active.currentCp();
				active.currentCp(before + c.amount());
				session.send(SystemMessage.of(SystemMessage.S1_CP_WILL_BE_RESTORED,
						new SystemMessage.Number((int) (active.currentCp() - before))));
				session.sendVitals();
			}
			case HOT_HP, HOT_MP -> startHealOverTime(c);
			case BUFF -> applyBuff(c);
			case SKILL_SCROLL -> applySkillScroll(c);
			case FACE, HAIR_COLOR, HAIR_STYLE -> changeAppearance(c);
			case MYSTERY -> startBigHead(c);
			case ENERGY_STONE -> session.increaseCharges(1, 2);
			case REMEDY -> applyRemedy(c);
			case RESURRECT_SCROLL -> applyResurrectScroll(c, resTarget);
			default -> {
			}
		}
	}

	private static int elixirMinLevel(String grade) {
		if (grade == null) {
			return 1;
		}
		return switch (grade.toLowerCase(java.util.Locale.ROOT)) {
			case "d" -> 20;
			case "c" -> 40;
			case "b" -> 52;
			case "a" -> 61;
			case "s", "s80", "s84" -> 76;
			default -> 1;
		};
	}

	private void applyRemedy(Consumable c) {
		PlayerCharacter active = session.activeChar();
		if (active == null) {
			return;
		}
		int itemId = c.itemId();
		// 1831/1832 (Antidote / Advanced Antidote) -> Poison
		// 1833/1834 (Bandage / Emergency Dressing) -> Bleed
		// 6037 (Scroll of Waking) -> Sleep
		// 3889 (Potion of Recovery) -> Poison, Bleed & Hold/Root
		boolean curePoison = itemId == 1831 || itemId == 1832 || itemId == 3889;
		boolean cureBleed = itemId == 1833 || itemId == 1834 || itemId == 3889;
		boolean cureSleep = itemId == 6037;
		boolean cureHold = itemId == 3889;

		if (curePoison) {
			active.effects().removeDebuffsByStackOrName("poison");
			active.stopAbnormalEffect(0x0002);
			cancelDotTasks("poison");
		}
		if (cureBleed) {
			active.effects().removeDebuffsByStackOrName("bleed");
			active.stopAbnormalEffect(0x0001);
			cancelDotTasks("bleed");
		}
		if (cureSleep) {
			active.effects().removeDebuffsByStackOrName("sleep");
			active.stopAbnormalEffect(0x0080);
			active.onDamaged(); // acorda caso esteja em estado sleeping
		}
		if (cureHold) {
			active.effects().removeDebuffsByStackOrName("root");
			active.effects().removeDebuffsByStackOrName("hold");
			active.stopAbnormalEffect(0x0200);
			active.root(0L);
		}
		session.refreshBuffs();
		session.sendMagicEffectIcons();
		session.broadcastAppearance();
	}

	private void cancelDotTasks(String keyword) {
		var exact = session.hotTasks().remove("skilldot_" + keyword);
		if (exact != null) {
			exact.cancel(false);
		}
		String kw = keyword.toLowerCase(java.util.Locale.ROOT);
		var it = session.hotTasks().entrySet().iterator();
		while (it.hasNext()) {
			var entry = it.next();
			String key = entry.getKey().toLowerCase(java.util.Locale.ROOT);
			if (key.startsWith("skilldot_") && key.contains(kw)) {
				if (entry.getValue() != null) {
					entry.getValue().cancel(false);
				}
				it.remove();
			}
		}
	}

	private void applySkillScroll(Consumable c) {
		PlayerCharacter owner = session.activeChar();
		if (owner == null) {
			return;
		}
		var ctx = session.context();
		SkillTemplate sk = (ctx != null && ctx.skillService() != null && ctx.skillService().table() != null)
				? ctx.skillService().table().get(c.skillId(), c.level()).orElse(null)
				: null;
		boolean hasEffectFuncs = sk != null && sk.effects() != null
				&& sk.effects().stream().anyMatch(e -> e.funcs() != null && !e.funcs().isEmpty());
		if (hasEffectFuncs) {
			session.applySkillEffects(sk, false);
			session.sendVitals();
			return;
		}

		long durationMs = 3_600_000L;
		String stackType;
		StatFunc func;
		switch (c.itemId()) {
			case 3926 -> { // Scroll of Guidance (Skill 2050)
				stackType = "hit_up";
				func = new StatFunc("accCombat", StatFunc.Op.ADD, 0x40, 4.0);
			}
			case 3927 -> { // Scroll of Death Whisper (Skill 2051)
				stackType = "critical_dmg_up";
				func = new StatFunc("cAtk", StatFunc.Op.MUL, 0x30, 1.35);
			}
			case 3928 -> { // Scroll of Focus (Skill 2052)
				stackType = "critical_prob_up";
				func = new StatFunc("rCrit", StatFunc.Op.MUL, 0x30, 1.30);
			}
			case 3929 -> { // Scroll of Greater Acumen (Skill 2053)
				stackType = "casting_time_down";
				func = new StatFunc("mAtkSpd", StatFunc.Op.MUL, 0x30, 1.30);
			}
			case 3930 -> { // Scroll of Haste (Skill 2054)
				stackType = "attack_time_down";
				func = new StatFunc("pAtkSpd", StatFunc.Op.MUL, 0x30, 1.33);
			}
			case 3931 -> { // Scroll of Agility (Skill 2055)
				stackType = "rEvas";
				func = new StatFunc("rEvas", StatFunc.Op.ADD, 0x40, 4.0);
			}
			case 3932 -> { // Scroll of Mystic Empower (Skill 2056)
				stackType = "ma_up";
				func = new StatFunc("mAtk", StatFunc.Op.MUL, 0x30, 1.75);
			}
			case 3933 -> { // Scroll of Might (Skill 2057)
				stackType = "pa_up";
				func = new StatFunc("pAtk", StatFunc.Op.MUL, 0x30, 1.15);
			}
			case 3934 -> { // Scroll of Wind Walk (Skill 2058)
				stackType = "speed_up";
				func = new StatFunc("runSpd", StatFunc.Op.ADD, 0x40, 33.0);
			}
			case 3935 -> { // Scroll of Shield (Skill 2059)
				stackType = "pd_up";
				func = new StatFunc("pDef", StatFunc.Op.MUL, 0x30, 1.15);
			}
			default -> {
				return;
			}
		}
		var buff = ActiveBuff.ofSkill(c.skillId(), c.level(), stackType, System.currentTimeMillis() + durationMs,
				List.of(func));
		owner.effects().put(buff, PlayerEffects.getExtraBuffSlots(owner));
		session.send(SystemMessage.of(SystemMessage.YOU_FEEL_S1_EFFECT,
				new SystemMessage.SkillName(c.skillId(), c.level())));
		session.refreshBuffs();
		session.sendVitals();
		session.saveBuffs();
		session.autoAttackScheduler().schedule(() -> {
			if (owner.effects().remove(buff) && session.activeChar() == owner && session.inWorld()) {
				session.send(SystemMessage.of(SystemMessage.EFFECT_S1_DISAPPEARED,
						new SystemMessage.SkillName(c.skillId(), c.level())));
				session.refreshBuffs();
				session.saveBuffs();
			}
		}, durationMs, TimeUnit.MILLISECONDS);
	}

	private void applyResurrectScroll(Consumable c, GameSession target) {
		PlayerCharacter active = session.activeChar();
		if (active == null || target == null || target.active() == null || !target.active().isDead()) {
			return;
		}
		var resSkill = new SkillTemplate(c.skillId(), c.level(),
				c.itemId() == 3936 ? "Blessed Scroll of Resurrection" : "Scroll of Resurrection",
				SkillTemplate.OperateType.ACTIVE, "RESURRECT", "TARGET_CORPSE_PLAYER",
				false, 0, 0, 0, c.amount(), 400, 0, 2000, 0, c.reuseMs(), 0,
				0.0, false, List.of(), List.of(), null, null);
		if (target.context() != null && target.context().characters() != null && target.context().world() != null) {
			target.magicSkillHandler().handleResurrect(resSkill, active.name());
		}
		if (target.active().isDead()) {
			PlayerCharacter tgt = target.active();
			tgt.sitting(false);
			double hpPercent = c.amount() > 0 ? Math.min(100.0, c.amount()) : 20.0;
			int revivedHp = Math.max(1, (int) Math.round(tgt.maxHp() * (hpPercent / 100.0)));
			tgt.currentHp(revivedHp);
			tgt.currentCp(0.0);
			var revive = new com.lopez.l2j.network.game.packet.GameServerPacket.Revive(tgt.objectId());
			target.send(revive);
			if (target.context() != null && target.context().world() != null) {
				target.context().world().broadcastAround(target, GameWorld.VISIBILITY_RADIUS, revive, false);
			}
			target.sendVitals();
			target.send(SystemMessage.of(SystemMessage.S1_HP_RESTORED, new SystemMessage.Number(revivedHp)));
		}
	}

	private void changeAppearance(Consumable c) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (active == null) {
			return;
		}
		int value = (int) c.amount();
		switch (c.type()) {
			case FACE -> active.face(value);
			case HAIR_COLOR -> active.hairColor(value);
			case HAIR_STYLE -> active.hairStyle(value);
			default -> {
				return;
			}
		}
		if (ctx != null && ctx.characters() != null) {
			ctx.characters().save(active, true);
		}
		session.broadcastAppearance();
	}

	private void startBigHead(Consumable c) {
		PlayerCharacter owner = session.activeChar();
		if (owner == null) {
			return;
		}
		var previous = session.hotTasks().remove("BigHead");
		if (previous != null) {
			previous.cancel(false);
		}
		owner.startAbnormalEffect(ConsumableTable.ABNORMAL_BIG_HEAD);
		session.send(SystemMessage.of(SystemMessage.YOU_FEEL_S1_EFFECT,
				new SystemMessage.SkillName(c.skillId(), c.level())));
		session.broadcastAppearance();
		AtomicReference<ScheduledFuture<?>> self = new AtomicReference<>();
		ScheduledFuture<?> task = session.autoAttackScheduler().schedule(() -> {
			if (!session.hotTasks().remove("BigHead", self.get())) {
				return;
			}
			owner.stopAbnormalEffect(ConsumableTable.ABNORMAL_BIG_HEAD);
			if (session.activeChar() == owner && session.inWorld()) {
				session.send(SystemMessage.of(SystemMessage.S1_HAS_WORN_OFF,
						new SystemMessage.SkillName(c.skillId(), c.level())));
				session.broadcastAppearance();
			}
		}, (long) c.ticks() * c.intervalMs(), TimeUnit.MILLISECONDS);
		self.set(task);
		session.hotTasks().put("BigHead", task);
	}

	public boolean consumeItem(int itemId, int count) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (active == null || ctx == null || ctx.inventories() == null) {
			return false;
		}
		var r = ctx.inventories().consumeItem(active.inventory(), itemId, count, "Consume");
		if (r == null) {
			return false;
		}
		session.send(new InventoryUpdate(
				List.of(ItemInfo.of(r.item(), r.removed() ? ItemInfo.REMOVED : ItemInfo.MODIFIED))));
		session.refreshWeightAndPenalties();
		return true;
	}

	private void startHealOverTime(Consumable c) {
		PlayerCharacter owner = session.activeChar();
		if (owner == null) {
			return;
		}
		boolean hp = c.type() == ConsumableTable.Type.HOT_HP;
		String stack = hp ? "HpRecover" : "MpRecover";
		var previous = session.hotTasks().remove(stack);
		if (previous != null) {
			previous.cancel(false);
		}
		int[] remaining = { c.ticks() };
		AtomicReference<ScheduledFuture<?>> self = new AtomicReference<>();
		ScheduledFuture<?> task = session.autoAttackScheduler().scheduleAtFixedRate(() -> {
			if (session.activeChar() != owner || !session.inWorld() || owner.isDead() || remaining[0] <= 0) {
				stopHot(stack, self.get());
				return;
			}
			remaining[0]--;
			if (hp) {
				owner.currentHp(owner.currentHp() + c.amount());
			} else {
				owner.currentMp(owner.currentMp() + c.amount());
			}
			session.sendVitals();
			if (remaining[0] <= 0) {
				stopHot(stack, self.get());
			}
		}, c.intervalMs(), c.intervalMs(), TimeUnit.MILLISECONDS);
		self.set(task);
		session.hotTasks().put(stack, task);
	}

	private void stopHot(String stack, ScheduledFuture<?> task) {
		if (task != null) {
			task.cancel(false);
			session.hotTasks().remove(stack, task);
		}
	}

	public void applyBuff(Consumable c) {
		applyBuff(c, 0);
	}

	public void applyBuff(Consumable c, long remainingMs) {
		PlayerCharacter owner = session.activeChar();
		if (owner == null) {
			return;
		}
		var d = c.buff();
		long durationMs = remainingMs > 0 ? remainingMs : d.durationMs();
		var buff = new ActiveBuff(c.skillId(), c.level(), d.stackType(), System.currentTimeMillis() + durationMs,
				d.runSpdAdd(), d.pAtkSpdMul(), d.mAtkSpdMul(), d.accuracyAdd());
		owner.effects().put(buff, PlayerEffects.getExtraBuffSlots(owner));
		session.send(SystemMessage.of(SystemMessage.YOU_FEEL_S1_EFFECT,
				new SystemMessage.SkillName(c.skillId(), c.level())));
		session.refreshBuffs();
		session.saveBuffs();
		session.autoAttackScheduler().schedule(() -> {
			if (owner.effects().remove(buff) && session.activeChar() == owner && session.inWorld()) {
				session.send(SystemMessage.of(SystemMessage.S1_HAS_WORN_OFF,
						new SystemMessage.SkillName(c.skillId(), c.level())));
				session.refreshBuffs();
				session.saveBuffs();
			}
		}, durationMs, TimeUnit.MILLISECONDS);
	}

	public boolean chargeSoulShot(int itemId, boolean quiet) {
		if (session.soulshotCharged()) {
			return true;
		}
		var c = ConsumableTable.get(itemId).orElse(null);
		if (c == null || c.type() != ConsumableTable.Type.SOULSHOT) {
			return false;
		}
		var weapon = activeWeapon();
		if (weapon == null) {
			if (!quiet) {
				session.send(SystemMessage.id(SystemMessage.CANNOT_USE_SOULSHOTS));
			}
			return false;
		}
		int weaponGrade = ConsumableTable.gradeIndex(weapon.template().crystalType());
		if (weaponGrade != ConsumableTable.gradeIndex(c.grade())) {
			if (!quiet) {
				session.send(SystemMessage.id(SystemMessage.SOULSHOTS_GRADE_MISMATCH));
			}
			return false;
		}
		int count = weapon.template().soulshots() > 0 ? weapon.template().soulshots() : 1;
		if (Config.CONSUME_SOUL_SHOT) {
			if (!consumeItem(itemId, count)) {
				if (session.autoSoulShots().remove(itemId)) {
					session.send(new ExAutoSoulShot(itemId, 0));
					session.send(SystemMessage.of(SystemMessage.AUTO_USE_OF_S1_CANCELLED,
							new SystemMessage.ItemName(itemId)));
				} else {
					session.send(SystemMessage.id(SystemMessage.NOT_ENOUGH_SOULSHOTS));
				}
				return false;
			}
		}
		session.chargedGrade(weaponGrade);
		session.soulshotCharged(true);
		if (!quiet) {
			session.send(SystemMessage.id(SystemMessage.ENABLED_SOULSHOT));
		}
		session.broadcastSelfSkill(c.skillId(), 1);
		return true;
	}

	public boolean chargeSpiritShot(int itemId, boolean quiet, boolean blessed) {
		if (session.spiritshotCharged()) {
			return true;
		}
		var c = ConsumableTable.get(itemId).orElse(null);
		if (c == null) {
			return false;
		}
		var weapon = activeWeapon();
		if (weapon == null) {
			if (!quiet) {
				session.send(SystemMessage.id(SystemMessage.CANNOT_USE_SOULSHOTS));
			}
			return false;
		}
		int weaponGrade = ConsumableTable.gradeIndex(weapon.template().crystalType());
		if (weaponGrade != ConsumableTable.gradeIndex(c.grade())) {
			if (!quiet) {
				session.send(SystemMessage.id(SystemMessage.SOULSHOTS_GRADE_MISMATCH));
			}
			return false;
		}
		int count = weapon.template().spiritshots() > 0 ? weapon.template().spiritshots() : 1;
		if (!consumeItem(itemId, count)) {
			if (session.autoSoulShots().remove(itemId)) {
				session.send(new ExAutoSoulShot(itemId, 0));
				session.send(
						SystemMessage.of(SystemMessage.AUTO_USE_OF_S1_CANCELLED, new SystemMessage.ItemName(itemId)));
			} else {
				session.send(SystemMessage.id(SystemMessage.NOT_ENOUGH_SOULSHOTS));
			}
			return false;
		}
		session.chargedSpSGrade(weaponGrade);
		session.blessedSpiritshot(blessed);
		session.spiritshotCharged(true);
		if (!quiet) {
			session.send(SystemMessage.id(SystemMessage.ENABLED_SOULSHOT));
		}
		session.broadcastSelfSkill(c.skillId(), 1);
		return true;
	}

	public void rechargeAutoSoulShots() {
		for (int itemId : session.autoSoulShots()) {
			if (isBeastShot(itemId)) {
				chargeBeastShot(itemId, true);
			} else if (ConsumableTable.isSoulshot(itemId)) {
				if (!session.soulshotCharged() && chargeSoulShot(itemId, true)) {
					// soulshot carregado
				}
			} else if (ConsumableTable.isSpiritshot(itemId)) {
				if (!session.spiritshotCharged()
						&& chargeSpiritShot(itemId, true, ConsumableTable.isBlessedSpiritshot(itemId))) {
					// spiritshot carregado
				}
			}
		}
	}

	public ItemInstance activeWeapon() {
		PlayerCharacter active = session.activeChar();
		if (active == null || active.inventory() == null) {
			return null;
		}
		var inv = active.inventory();
		var w = inv.paperdoll(ItemSlots.RHAND);
		if (w == null) {
			w = inv.paperdoll(ItemSlots.LRHAND);
		}
		return w != null && w.template().kind() == Kind.WEAPON ? w : null;
	}

	private static boolean isDeluxeChestKey(int itemId) {
		return itemId >= 6665 && itemId <= 6672;
	}

	private static boolean isNormalChestKey(int itemId) {
		return itemId >= 5197 && itemId <= 5204;
	}

	private static boolean isChestKey(int itemId) {
		return isDeluxeChestKey(itemId) || isNormalChestKey(itemId) || itemId == 9205;
	}

	private static boolean isChestNpc(NpcInstance npc) {
		if (npc == null || npc.template() == null) {
			return false;
		}
		int id = npc.npcId();
		if (id >= 18257 && id <= 18298) {
			return true;
		}
		if (id >= 21801 && id <= 21824) {
			return true;
		}
		String type = npc.template().type();
		if (type != null && type.equalsIgnoreCase("L2Chest")) {
			return true;
		}
		String name = npc.name();
		if (name != null) {
			String lower = name.toLowerCase(java.util.Locale.ROOT);
			if (lower.contains("chest") || lower.contains("box")) {
				return true;
			}
		}
		return false;
	}

	private void useThiefKey(ItemInstance keyItem) {
		PlayerCharacter active = session.activeChar();
		if (!session.inWorld() || active == null || active.isDead() || active.sitting() || active.isDisabled()) {
			session.send(new ActionFailed());
			return;
		}
		var ctx = session.context();
		var svc = ctx != null ? ctx.skillService() : null;
		var sk = svc == null ? null : svc.known(active, 27).orElse(null);
		if (sk != null) {
			session.castSkill(sk, true);
		} else {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
					"Thief Key e consumida pela habilidade Unlock (ladinos)."));
			session.send(new ActionFailed());
		}
	}

	private void useChestKey(ItemInstance keyItem) {
		PlayerCharacter active = session.activeChar();
		if (!session.inWorld() || active == null || active.isDead() || active.sitting() || active.isDisabled()) {
			session.send(new ActionFailed());
			return;
		}
		var ctx = session.context();
		int itemId = keyItem.itemId();
		SkillTemplate sk = null;
		var skTable = ctx != null && ctx.skillService() != null ? ctx.skillService().table() : null;
		if (skTable != null) {
			if (isDeluxeChestKey(itemId)) {
				int level = itemId - 6665 + 1;
				sk = skTable.get(2229, level).orElse(null);
			} else if (isNormalChestKey(itemId)) {
				int level = 5204 - itemId + 1;
				sk = skTable.get(2065, level).orElse(null);
			} else if (itemId == 9205) {
				sk = skTable.get(2229, 8).orElse(null);
			}
		}

		if (sk == null) {
			session.send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED, new SystemMessage.ItemName(itemId)));
			session.send(new ActionFailed());
			return;
		}

		if (session.targetObjectId() == 0 || ctx == null || ctx.world() == null) {
			session.send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
			session.send(new ActionFailed());
			return;
		}

		var npcTarget = ctx.world().npc(session.targetObjectId()).filter(n -> !n.isDead() && isChestNpc(n))
				.orElse(null);
		var doorTarget = (npcTarget == null && ctx.doors() != null) ? ctx.doors().door(session.targetObjectId()) : null;
		if (npcTarget == null && doorTarget == null) {
			session.send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
			session.send(new ActionFailed());
			return;
		}
		if (doorTarget != null && !doorTarget.unlockable()) {
			session.send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
			session.send(new ActionFailed());
			return;
		}

		session.castSkill(sk, true);
	}

	public static boolean isBeastShot(int itemId) {
		return itemId == 6645 || itemId == 6646 || itemId == 6647;
	}

	public boolean chargeBeastShot(int itemId, boolean quiet) {
		PlayerCharacter active = session.activeChar();
		if (active == null || active.activeSummon() == null || active.activeSummon().isDead()) {
			if (!quiet) {
				session.send(SystemMessage.id(SystemMessage.CANNOT_USE_SOULSHOTS));
			}
			return false;
		}
		var sum = active.activeSummon();
		if (itemId == 6645) {
			if (sum.soulshotCharged())
				return true;
			if (!consumeItem(itemId, 1)) {
				if (session.autoSoulShots().remove(itemId)) {
					session.send(new ExAutoSoulShot(itemId, 0));
					session.send(SystemMessage.of(SystemMessage.AUTO_USE_OF_S1_CANCELLED,
							new SystemMessage.ItemName(itemId)));
				}
				return false;
			}
			sum.soulshotCharged(true);
			session.broadcastSelfSkill(2033, 1);
			return true;
		}
		if (itemId == 6646 || itemId == 6647) {
			if (sum.spiritshotCharged() > 0)
				return true;
			if (!consumeItem(itemId, 1)) {
				if (session.autoSoulShots().remove(itemId)) {
					session.send(new ExAutoSoulShot(itemId, 0));
					session.send(SystemMessage.of(SystemMessage.AUTO_USE_OF_S1_CANCELLED,
							new SystemMessage.ItemName(itemId)));
				}
				return false;
			}
			sum.spiritshotCharged(itemId == 6647 ? 2 : 1);
			session.broadcastSelfSkill(itemId == 6647 ? 2009 : 2008, 1);
			return true;
		}
		return false;
	}

	public static boolean isSoulCrystal(int itemId) {
		return (itemId >= 4629 && itemId <= 4664)
				|| (itemId >= 5577 && itemId <= 5582)
				|| itemId == 5908 || itemId == 5911 || itemId == 5914
				|| (itemId >= 9570 && itemId <= 9572);
	}

	public static boolean isManorSeed(int itemId) {
		if ((itemId >= 5016 && itemId <= 5065)
				|| (itemId >= 5800 && itemId <= 5859)
				|| (itemId >= 6360 && itemId <= 6388)) {
			return true;
		}
		var manor = com.lopez.l2j.game.manor.CastleManorManager.getInstance();
		return manor != null && manor.getSeed(itemId).isPresent();
	}

	public static boolean isHelpBook(int itemId) {
		return itemId == 5588
				|| (itemId >= 7063 && itemId <= 7066)
				|| (itemId >= 7082 && itemId <= 7112);
	}

	public static boolean isCrystalCarolOrFirework(int itemId) {
		return itemId == 6403 || itemId == 6406 || itemId == 6407
				|| (itemId >= 4410 && itemId <= 4417)
				|| itemId == 4420 || itemId == 4421 || itemId == 5062
				|| (itemId >= 5562 && itemId <= 5566)
				|| (itemId >= 5583 && itemId <= 5587)
				|| itemId == 6903 || itemId == 7061 || itemId == 7062 || itemId == 8555;
	}

	private boolean handleSpecialMiscellaneousItem(PlayerCharacter active, ItemInstance item) {
		int itemId = item.itemId();
		var ctx = session.context();
		// Beast SoulShot / SpiritShot (6645-6647)
		if (isBeastShot(itemId)) {
			chargeBeastShot(itemId, false);
			return true;
		}
		// Lottery Ticket (4442) - exibe numeros apostados e rodada
		if (itemId == com.lopez.l2j.game.service.LotteryService.LOTTERY_TICKET_ITEM_ID) {
			int round = item.enchant() > 0 ? item.enchant()
					: (ctx != null && ctx.lottery() != null ? ctx.lottery().getCurrentLotteryId() : 1);
			Set<Integer> numbers = com.lopez.l2j.game.service.LotteryService.decodeNumbersBitmask(item.customType1(),
					item.customType2());
			String numStr = numbers.isEmpty() ? "N/A" : numbers.toString();
			session.send(new CreatureSay(0, CreatureSay.ALL, "Lottery",
					"Bilhete da Loteria de Aden #" + round + " | Numeros escolhidos: " + numStr));
			String html = "<html><body><title>Lottery Ticket #" + round + "</title><br>"
					+ "<b>Rodada da Loteria:</b> #" + round + "<br>"
					+ "<b>Numeros Apostados:</b> " + numStr + "<br></body></html>";
			session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage(0, html));
			return true;
		}
		// Monster Race Ticket (4443) - exibe raia, corrida e valor apostado
		if (itemId == com.lopez.l2j.game.service.MonsterRaceService.RACE_TICKET_ITEM_ID) {
			int enc = item.enchant();
			int laneFirst = enc >= 10 ? (enc / 10) : Math.max(1, enc);
			int laneSecond = enc >= 10 ? (enc % 10) : 0;
			int raceId = item.customType1() > 0 ? item.customType1()
					: (ctx != null && ctx.monsterRace() != null ? ctx.monsterRace().getCurrentRaceId() : 1);
			int betAmount = Math.max(100, item.customType2() * 100);
			String laneDesc = laneSecond > 0 ? ("1o Raia " + laneFirst + " / 2o Raia " + laneSecond)
					: ("Raia " + laneFirst);
			session.send(new CreatureSay(0, CreatureSay.ALL, "MonsterTrack",
					"Bilhete Monster Derby #" + raceId + " | " + laneDesc + " | Aposta: " + betAmount + " Adena"));
			String html = "<html><body><title>Monster Race Ticket #" + raceId + "</title><br>"
					+ "<b>Corrida:</b> #" + raceId + "<br>"
					+ "<b>Aposta:</b> " + laneDesc + "<br>"
					+ "<b>Valor:</b> " + betAmount + " Adena<br></body></html>";
			session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage(0, html));
			return true;
		}
		// Beast Spice: Golden Spice (6643) & Crystal Spice (6644) para Beast Farm (NPCs
		// 21451..21507)
		if (itemId == 6643 || itemId == 6644) {
			if (ctx == null || ctx.world() == null || session.targetObjectId() == 0) {
				session.send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
				return true;
			}
			var npcTarget = ctx.world().npc(session.targetObjectId()).filter(n -> !n.isDead()).orElse(null);
			if (npcTarget == null || npcTarget.npcId() < 21451 || npcTarget.npcId() > 21507) {
				session.send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
				return true;
			}
			if (!consumeItem(itemId, 1)) {
				return true;
			}
			int skillId = (itemId == 6643) ? 2188 : 2189;
			var msu = new MagicSkillUse(active.objectId(), npcTarget.objectId(), skillId, 1, 500, 1000,
					active.x(), active.y(), active.z(), npcTarget.x(), npcTarget.y(), npcTarget.z());
			session.send(msu);
			ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, msu, false);
			handleBeastSpiceFeed(active, npcTarget, itemId == 6644, ctx);
			return true;
		}
		// #211, #213 - FishShots (6535-6540) - valida vara de pescar equipada
		if (itemId >= 6535 && itemId <= 6540) {
			var weapon = activeWeapon();
			if (weapon == null || (!"rod".equalsIgnoreCase(weapon.template().weaponType())
					&& !(weapon.itemId() >= 6529 && weapon.itemId() <= 6534))) {
				session.send(SystemMessage.id(SystemMessage.CANNOT_USE_SOULSHOTS));
				return true;
			}
			int rodGrade = ConsumableTable.gradeIndex(weapon.template().crystalType());
			int shotGrade = itemId - 6535;
			if (rodGrade != shotGrade) {
				session.send(SystemMessage.id(SystemMessage.SOULSHOTS_GRADE_MISMATCH));
				return true;
			}
			if (consumeItem(itemId, 1)) {
				int skillId = 2181 + shotGrade;
				session.broadcastSelfSkill(skillId, 1);
				session.send(SystemMessage.id(SystemMessage.ENABLED_SOULSHOT));
			}
			return true;
		}
		// #212, #235 - PetFood (2515 Wolf, 4038 Hatchling, 5168/5169 Strider, 6316
		// Wyvern, 7582 Baby, 9668 Great Wolf, 10425 Improved Baby)
		if (itemId == 2515 || itemId == 4038 || itemId == 5168 || itemId == 5169 || itemId == 6316 || itemId == 7582
				|| itemId == 9668 || itemId == 10425) {
			var sum = active.activeSummon();
			if (sum != null && !sum.isDead()) {
				if (sum.isPet() && sum.itemObjectId() > 0) {
					if (com.lopez.l2j.game.pet.PetService.getInstance().feed(
							active, sum.itemObjectId(), sum.npcId(), itemId, session::send)) {
						var fedItem = active.inventory().byItemId(itemId).orElse(null);
						if (fedItem != null) {
							session.send(new InventoryUpdate(List.of(ItemInfo.of(fedItem, ItemInfo.MODIFIED))));
						} else {
							session.send(new InventoryUpdate(List.of(ItemInfo.of(item, ItemInfo.REMOVED))));
						}
						session.refreshWeightAndPenalties();
						return true;
					}
				}
				if (consumeItem(itemId, 1)) {
					sum.currentFed(Math.min(sum.maxFed(), sum.currentFed() + 250));
					session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.PetStatusUpdate(sum));
				}
			} else if (active.isMounted()) {
				consumeItem(itemId, 1);
			}
			return true;
		}
		// #202-#203 - SoulCrystals (4629-4664, 5577-5582, 5908, 5911, 5914, 9570-9572)
		if (isSoulCrystal(itemId)) {
			long now = System.currentTimeMillis();
			Long readyAt = session.consumableReuse().get(2096);
			if (readyAt != null && readyAt > now) {
				return true;
			}
			if (ctx == null || ctx.world() == null || session.targetObjectId() == 0) {
				session.send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
				return true;
			}
			var npcTarget = ctx.world().npc(session.targetObjectId()).filter(n -> !n.isDead() && n.isMonster())
					.orElse(null);
			if (npcTarget == null) {
				session.send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
				return true;
			}
			session.consumableReuse().put(2096, now + 2000L);
			SOUL_CRYSTAL_ABSORBERS.computeIfAbsent(npcTarget.objectId(), k -> new ConcurrentHashMap<>())
					.put(active.objectId(), itemId);
			var msu = new MagicSkillUse(active.objectId(), npcTarget.objectId(), 2096, 1, 500, 2000,
					active.x(), active.y(), active.z(), npcTarget.x(), npcTarget.y(), npcTarget.z());
			session.send(msu);
			ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, msu, false);
			return true;
		}
		// #204 - Manor Seeds (5016-5065, 5800-5859, 6360-6388)
		if (isManorSeed(itemId)) {
			if (ctx == null || ctx.world() == null || session.targetObjectId() == 0) {
				session.send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
				return true;
			}
			var npcTarget = ctx.world().npc(session.targetObjectId()).filter(n -> !n.isDead() && n.isMonster())
					.orElse(null);
			if (npcTarget == null) {
				session.send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
				return true;
			}
			if (SEEDED_MONSTERS.containsKey(npcTarget.objectId())) {
				session.send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED, new SystemMessage.ItemName(itemId)));
				return true;
			}
			if (!consumeItem(itemId, 1)) {
				return true;
			}
			var manor = ctx.manor() != null ? ctx.manor() : com.lopez.l2j.game.manor.CastleManorManager.getInstance();
			int cropId = (manor != null
					? manor.getSeed(itemId).map(com.lopez.l2j.game.manor.SeedTemplate::cropId).orElse(0)
					: 0);
			if (cropId <= 0) {
				cropId = itemId + 52;
			}
			SEEDED_MONSTERS.put(npcTarget.objectId(), new SeededMonsterState(itemId, cropId, active.objectId(), false));
			var msu = new MagicSkillUse(active.objectId(), npcTarget.objectId(), 2097, 1, 500, 0,
					active.x(), active.y(), active.z(), npcTarget.x(), npcTarget.y(), npcTarget.z());
			session.send(msu);
			ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, msu, false);
			session.send(SystemMessage.id(889)); // THE_SEED_WAS_SUCCESSFULLY_SOWN
			return true;
		}
		// #205 - Harvester (5125)
		if (itemId == 5125) {
			if (ctx == null || ctx.world() == null || session.targetObjectId() == 0) {
				session.send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
				return true;
			}
			var npcTarget = ctx.world().npc(session.targetObjectId()).filter(NpcInstance::isDead).orElse(null);
			if (npcTarget == null) {
				session.send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
				return true;
			}
			SeededMonsterState seeded = SEEDED_MONSTERS.get(npcTarget.objectId());
			if (seeded == null || seeded.harvested() || seeded.seederPlayerId() != active.objectId()) {
				session.send(SystemMessage.id(894)); // THE_HARVEST_FAILED_BECAUSE_THE_SEED_WAS_NOT_SOWN
				return true;
			}
			SEEDED_MONSTERS.put(npcTarget.objectId(),
					new SeededMonsterState(seeded.seedItemId(), seeded.cropItemId(), seeded.seederPlayerId(), true));
			var msu = new MagicSkillUse(active.objectId(), npcTarget.objectId(), 2098, 1, 500, 0,
					active.x(), active.y(), active.z(), npcTarget.x(), npcTarget.y(), npcTarget.z());
			session.send(msu);
			ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, msu, false);
			int cropCount = com.lopez.l2j.game.manor.CastleManorManager.calculateManorCropDropCount(1);
			if (ctx.inventories() != null && seeded.cropItemId() > 0 && cropCount > 0) {
				var added = ctx.inventories().addItem(active.inventory(), seeded.cropItemId(), cropCount, "Harvest");
				if (added != null && added.item() != null) {
					session.send(new InventoryUpdate(List.of(
							ItemInfo.of(added.item(), added.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED))));
					session.refreshWeightAndPenalties();
				}
				session.send(SystemMessage.of(SystemMessage.EARNED_S2_S1_S,
						new SystemMessage.ItemName(seeded.cropItemId()), new SystemMessage.ItemNumber(cropCount)));
			}
			return true;
		}
		// #206 - Mercenary Ticket via UseItem (posiciona na coordenada atual do
		// jogador)
		if (isMercenaryTicket(itemId) && ctx != null && ctx.mercenaries() != null && active.clanId() > 0) {
			var clan = ctx.clans() != null ? ctx.clans().byClanId(active.clanId()).orElse(null) : null;
			boolean hasMercPriv = active.isClanLeader()
					|| (active.clanPrivileges() & com.lopez.l2j.game.clan.Clan.CP_CS_MERCENARIES) != 0;
			if (clan != null && clan.castleId() > 0 && hasMercPriv) {
				if (ctx.sevenSigns() != null) {
					ctx.mercenaries().setSevenSignsManager(ctx.sevenSigns());
				}
				int prevCount = item.count();
				if (ctx.mercenaries().hireMercenary(active, clan, itemId, active.x(), active.y(), active.z(),
						active.heading())) {
					var remaining = active.inventory().byObjectId(item.objectId()).orElse(null);
					session.send(new InventoryUpdate(List.of(
							ItemInfo.of(remaining != null ? remaining : item,
									(remaining == null || prevCount <= 1) ? ItemInfo.REMOVED : ItemInfo.MODIFIED))));
					session.refreshWeightAndPenalties();
					return true;
				}
			}
		}
		// #207-#208 - HelpBook / Tutorial & Quest Books (5588, 7063-7066, 7082-7112)
		if (isHelpBook(itemId)) {
			String html = (ctx != null && ctx.htmls() != null) ? ctx.htmls().get("help/" + itemId + ".htm") : null;
			if (html == null || html.isBlank()) {
				html = "<html><body><title>Book #" + itemId + "</title><br>Reading " + item.template().name()
						+ ".</body></html>";
			}
			session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage(0, html));
			return true;
		}
		// #208-#209, #212 - SpecialXMas (5555 XMas Seal, 5560/5561 Christmas Tree)
		if (itemId == 5555) {
			session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.ShowXMasSeal(5555));
			return true;
		}
		if (itemId == 5560 || itemId == 5561) {
			if (consumeItem(itemId, 1)) {
				session.broadcastSelfSkill(itemId == 5560 ? 2137 : 2138, 1);
			}
			return true;
		}
		// #209-#210 - SquashSeed (6389, 6390) & Nectar (6391)
		if (itemId == 6389 || itemId == 6390) {
			if (consumeItem(itemId, 1)) {
				session.broadcastSelfSkill(itemId == 6389 ? 2003 : 2004, 1);
			}
			return true;
		}
		if (itemId == 6391) {
			if (consumeItem(itemId, 1)) {
				session.broadcastSelfSkill(2005, 1);
			}
			return true;
		}
		// #210-#211 - PaganMark (8064 Visitor's Mark -> 8065 Faded Visitor's Mark)
		if (itemId == 8064) {
			if (consumeItem(8064, 1) && ctx != null && ctx.inventories() != null) {
				var added = ctx.inventories().addItem(active.inventory(), 8065, 1, "PaganMark");
				if (added != null && added.item() != null) {
					session.send(new InventoryUpdate(List.of(ItemInfo.of(added.item(), ItemInfo.ADDED))));
				}
				if (ctx.doors() != null) {
					ctx.doors().openDoor(19160001);
				}
			}
			return true;
		}
		// #216 - SP Scrolls (5593 +500 SP, 5594 +5000 SP, 5595 +100000 SP)
		if (itemId == 5593 || itemId == 5594 || itemId == 5595) {
			int spGain = switch (itemId) {
				case 5593 -> 500;
				case 5594 -> 5000;
				default -> 100000;
			};
			if (consumeItem(itemId, 1)) {
				active.sp(active.sp() + spGain);
				session.broadcastSelfSkill(2167, 1);
				session.sendUserInfoAndBroadcastCharInfo();
			}
			return true;
		}
		// #217 - Elemental Potions (9997-10002) & Fisherman's Potions (8193-8202)
		if ((itemId >= 9997 && itemId <= 10002) || (itemId >= 8193 && itemId <= 8202)) {
			if (consumeItem(itemId, 1)) {
				int skillId = (itemId >= 9997) ? (2335 + (itemId - 9997)) : (2274);
				session.broadcastSelfSkill(skillId, 1);
			}
			return true;
		}
		// Calculator (4393)
		if (itemId == 4393) {
			session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.ShowCalc(4393));
			return true;
		}
		// #224 - Rolling Dice (4625-4628) com cooldown de 4000ms
		if (itemId >= 4625 && itemId <= 4628) {
			long now = System.currentTimeMillis();
			Long readyAt = session.consumableReuse().get(itemId);
			if (readyAt != null && readyAt > now) {
				session.send(SystemMessage.of(SystemMessage.S1_PREPARED_FOR_REUSE, new SystemMessage.ItemName(itemId)));
				return true;
			}
			session.consumableReuse().put(itemId, now + 4000L);
			int number = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 7);
			var dice = new com.lopez.l2j.network.game.packet.GameServerPacket.Dice(
					active.objectId(), itemId, number, active.x() - 30, active.y() - 30, active.z());
			session.send(dice);
			if (ctx != null && ctx.world() != null) {
				ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, dice, false);
			}
			session.send(
					SystemMessage.of(834, new SystemMessage.Text(active.name()), new SystemMessage.Number(number)));
			return true;
		}
		// #221, #223 - Pagan / Chapel / Splendor / Enigma Keys (8056, 8060, 8273, 8274,
		// 8275, 7260)
		if (itemId == 8056 || itemId == 8060 || itemId == 8273 || itemId == 8274 || itemId == 8275 || itemId == 7260) {
			if (ctx != null && ctx.doors() != null && session.targetObjectId() != 0) {
				var door = ctx.doors().door(session.targetObjectId());
				if (door != null && consumeItem(itemId, 1)) {
					ctx.doors().openDoor(door.doorId());
					int skillId = switch (itemId) {
						case 8273 -> 2241;
						case 8274 -> 2242;
						case 8275 -> 2243;
						default -> 2240;
					};
					session.broadcastSelfSkill(skillId, 1);
					return true;
				}
			}
			session.send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
			return true;
		}
		// #213-#214, #225 - Fireworks & Crystal Carols (6403, 6406, 6407, 4410-4417,
		// 4420-4421, 5062, 5562-5566, 5583-5587, 6903, 7061, 7062, 8555)
		if (isCrystalCarolOrFirework(itemId)) {
			long now = System.currentTimeMillis();
			Long readyAt = session.consumableReuse().get(itemId);
			if (readyAt != null && readyAt > now) {
				return true;
			}
			if (consumeItem(itemId, 1)) {
				session.consumableReuse().put(itemId, now + 3000L);
				int skillId = switch (itemId) {
					case 6403 -> 2023;
					case 6406 -> 2024;
					case 6407 -> 2025;
					case 4411 -> 2069;
					case 4412 -> 2068;
					case 4413 -> 2070;
					case 4414 -> 2072;
					case 4415 -> 2071;
					case 4416 -> 2073;
					case 4417 -> 2067;
					case 5562 -> 2140;
					case 5563 -> 2141;
					case 5564 -> 2142;
					case 5565 -> 2143;
					case 5566 -> 2144;
					default -> 2073;
				};
				session.broadcastSelfSkill(skillId, 1);
			}
			return true;
		}
		// #215 - Herbs (8600-8614: HP, MP, Power, Magic, Atk.Spd, Casting Spd,
		// Critical, Speed, Superior)
		if (itemId >= 8600 && itemId <= 8614) {
			if (consumeItem(itemId, 1)) {
				int skillId = 2278;
				switch (itemId) {
					case 8600 -> active.currentHp(active.currentHp() + active.maxHp() * 0.15);
					case 8601 -> active.currentHp(active.currentHp() + active.maxHp() * 0.30);
					case 8602 -> active.currentHp(active.maxHp());
					case 8603 -> {
						active.currentMp(active.currentMp() + active.maxMp() * 0.12);
						skillId = 2279;
					}
					case 8604 -> {
						active.currentMp(active.currentMp() + active.maxMp() * 0.25);
						skillId = 2279;
					}
					case 8605 -> {
						active.currentMp(active.maxMp());
						skillId = 2279;
					}
					case 8606 -> {
						applyHerbStatBuff(active, 2280, "pa_up", new StatFunc("pAtk", StatFunc.Op.MUL, 0x30, 1.15));
						skillId = 2280;
					}
					case 8607 -> {
						applyHerbStatBuff(active, 2281, "ma_up", new StatFunc("mAtk", StatFunc.Op.MUL, 0x30, 1.75));
						skillId = 2281;
					}
					case 8608 -> {
						applyHerbStatBuff(active, 2282, "attack_time_down",
								new StatFunc("pAtkSpd", StatFunc.Op.MUL, 0x30, 1.33));
						skillId = 2282;
					}
					case 8609 -> {
						applyHerbStatBuff(active, 2283, "casting_time_down",
								new StatFunc("mAtkSpd", StatFunc.Op.MUL, 0x30, 1.30));
						skillId = 2283;
					}
					case 8610 -> {
						applyHerbStatBuff(active, 2284, "critical_prob_up",
								new StatFunc("rCrit", StatFunc.Op.MUL, 0x30, 1.30));
						skillId = 2284;
					}
					case 8611 -> {
						applyHerbStatBuff(active, 2285, "speed_up",
								new StatFunc("runSpd", StatFunc.Op.ADD, 0x40, 33.0));
						skillId = 2285;
					}
					default -> {
						active.currentHp(active.currentHp() + active.maxHp() * 0.10);
						active.currentMp(active.currentMp() + active.maxMp() * 0.10);
					}
				}
				session.broadcastSelfSkill(skillId, 1);
				session.sendVitals();
			}
			return true;
		}
		// Hot Springs Remedies (8030-8033)
		if (itemId >= 8030 && itemId <= 8033) {
			if (consumeItem(itemId, 1)) {
				int debuffSkillId = switch (itemId) {
					case 8030 -> 4551; // Rheumatism
					case 8031 -> 4552; // Cholera
					case 8032 -> 4553; // Flu
					default -> 4554; // Malaria
				};
				active.effects().removeSkill(debuffSkillId);
				session.refreshBuffs();
				session.sendMagicEffectIcons();
			}
			return true;
		}
		return false;
	}

	private void handleBeastSpiceFeed(PlayerCharacter active, NpcInstance npc, boolean crystalSpice,
			GameSession.Context ctx) {
		String curTitle = npc.championTitle() != null ? npc.championTitle() : "";
		int nextStage;
		if (curTitle.contains("x2") || curTitle.contains("Stage 3")) {
			nextStage = 4;
		} else if (curTitle.contains("x1") || curTitle.contains("Stage 2")) {
			nextStage = 3;
		} else if (curTitle.contains("x1/2") || curTitle.contains("Stage 1")) {
			nextStage = 2;
		} else {
			nextStage = 1;
		}

		// No ultimo estagio (ou com 25% de chance no Stage 3), pode ser domesticado
		// como Tamed Beast
		boolean tamed = nextStage >= 4 && java.util.concurrent.ThreadLocalRandom.current().nextInt(100) < 35;
		if (tamed) {
			npc.champion(true);
			npc.championTitle("Tamed Beast (" + active.name() + ")");
			npc.currentHp(npc.maxHp());
			active.currentHp(Math.min(active.maxHp(), active.currentHp() + active.maxHp() * 0.25));
			active.currentMp(Math.min(active.maxMp(), active.currentMp() + active.maxMp() * 0.20));
			if (crystalSpice) {
				applyHerbStatBuff(active, 2283, "casting_time_down",
						new StatFunc("mAtkSpd", StatFunc.Op.MUL, 0x30, 1.30));
			} else {
				applyHerbStatBuff(active, 2280, "pa_up", new StatFunc("pAtk", StatFunc.Op.MUL, 0x30, 1.15));
			}
			session.send(new CreatureSay(npc.objectId(), CreatureSay.ALL, npc.name(),
					"Mestre " + active.name() + ", estou ao seu dispor!"));
		} else {
			String stageLabel = switch (nextStage) {
				case 1 -> "Feed x1";
				case 2 -> "Feed x2";
				default -> "Feed x4";
			};
			double statMult = switch (nextStage) {
				case 1 -> 1.25;
				case 2 -> 1.60;
				default -> 2.20;
			};
			npc.champion(true);
			npc.championTitle(stageLabel + (crystalSpice ? " (Crystal)" : " (Golden)"));
			npc.maxHpMul(statMult);
			npc.pAtkMul(statMult);
			npc.mAtkMul(statMult);
			npc.currentHp(npc.maxHp());
			session.send(new CreatureSay(0, CreatureSay.ALL, "BeastFarm",
					npc.name() + " se alimentou de " + (crystalSpice ? "Crystal Spice" : "Golden Spice")
							+ " e evoluiu para " + stageLabel + "!"));
		}

		var info = new com.lopez.l2j.network.game.packet.GameServerPacket.NpcInfo(npc);
		session.send(info);
		if (ctx != null && ctx.world() != null) {
			ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, info, false);
		}
	}

	private void applyHerbStatBuff(PlayerCharacter owner, int skillId, String stackType, StatFunc func) {
		long durationMs = 120_000L; // 2 minutos para Herbs of Power/Magic/Speed/Critical
		var buff = ActiveBuff.ofSkill(skillId, 1, stackType, System.currentTimeMillis() + durationMs, List.of(func));
		owner.effects().put(buff, PlayerEffects.getExtraBuffSlots(owner));
		session.refreshBuffs();
		session.sendVitals();
	}

	// ---- Pet Inventory & Pet Rename Delegators (#071, #236-#238, #241) ----

	public void handleGiveItemToPet(com.lopez.l2j.network.game.packet.GameClientPacket.RequestGiveItemToPet p) {
		PlayerCharacter active = session.activeChar();
		if (!session.inWorld() || active == null || active.isDead() || active.activeSummon() == null || p == null) {
			session.send(new ActionFailed());
			return;
		}
		boolean ok = com.lopez.l2j.game.pet.PetService.getInstance().transferItemToPet(
				active, active.activeSummon(), p.objectId(), p.amount(), session::send);
		if (!ok) {
			session.send(new ActionFailed());
		} else {
			session.refreshWeightAndPenalties();
		}
	}

	public void handleGetItemFromPet(com.lopez.l2j.network.game.packet.GameClientPacket.RequestGetItemFromPet p) {
		PlayerCharacter active = session.activeChar();
		if (!session.inWorld() || active == null || active.isDead() || active.activeSummon() == null || p == null) {
			session.send(new ActionFailed());
			return;
		}
		boolean ok = com.lopez.l2j.game.pet.PetService.getInstance().transferItemFromPet(
				active, active.activeSummon(), p.objectId(), p.amount(), session::send);
		if (!ok) {
			session.send(new ActionFailed());
		} else {
			session.refreshWeightAndPenalties();
		}
	}

	public void handlePetUseItem(com.lopez.l2j.network.game.packet.GameClientPacket.RequestPetUseItem p) {
		PlayerCharacter active = session.activeChar();
		if (!session.inWorld() || active == null || active.isDead() || active.activeSummon() == null || p == null) {
			session.send(new ActionFailed());
			return;
		}
		boolean ok = com.lopez.l2j.game.pet.PetService.getInstance().usePetItem(
				active, active.activeSummon(), p.objectId(), session::send);
		if (!ok) {
			session.send(new ActionFailed());
		}
	}

	public void handlePetGetItem(com.lopez.l2j.network.game.packet.GameClientPacket.RequestPetGetItem p) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (!session.inWorld() || active == null || active.isDead() || active.activeSummon() == null
				|| ctx == null || ctx.groundItems() == null || p == null) {
			session.send(new ActionFailed());
			return;
		}
		var groundOpt = ctx.groundItems().byObjectId(p.objectId());
		if (groundOpt.isEmpty()) {
			session.send(new ActionFailed());
			return;
		}
		com.lopez.l2j.game.pet.PetService.getInstance().petPickUpItem(
				active, active.activeSummon(), groundOpt.get(), ctx.groundItems(), session::send);
	}

	public void handleChangePetName(com.lopez.l2j.network.game.packet.GameClientPacket.RequestChangePetName p) {
		PlayerCharacter active = session.activeChar();
		if (!session.inWorld() || active == null || active.activeSummon() == null || p == null) {
			session.send(new ActionFailed());
			return;
		}
		com.lopez.l2j.game.pet.PetService.getInstance().changePetName(
				active, active.activeSummon(), p.name(), session::send);
	}

	private void useNoblesseItem(ItemInstance item) {
		PlayerCharacter active = session.activeChar();
		if (!session.inWorld() || active == null || active.isDead()) {
			session.send(new ActionFailed());
			return;
		}
		if (active.isNoble()) {
			session.send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED, new SystemMessage.ItemName(item.itemId())));
			session.send(new ActionFailed());
			return;
		}
		if (!consumeItem(item.itemId(), 1)) {
			session.send(new ActionFailed());
			return;
		}
		active.setNoble(true);
		session.send(SystemMessage.id(SystemMessage.YOU_HAVE_BECOME_A_NOBLESSE));
		session.send(new SocialAction(active.objectId(), 16));
		session.send(new ActionFailed());
		session.sendUserInfoAndBroadcastCharInfo();
		var ctx = session.context();
		if (ctx != null && ctx.characters() != null) {
			ctx.characters().save(active, true);
		}
	}
}
