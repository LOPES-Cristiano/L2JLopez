package com.lopez.l2j.network.game.handler.packet;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.effect.PlayerEffects.ActiveBuff;
import com.lopez.l2j.game.effect.ConsumableTable;
import com.lopez.l2j.game.effect.ConsumableTable.Consumable;
import com.lopez.l2j.game.item.EnchantScrollTable;
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
import com.lopez.l2j.network.game.packet.GameServerPacket.StatusUpdate;
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
 * Handler modular para itens, equipamentos, consumiveis, soulshots e encantamento.
 */
public class ItemPacketHandler {

	private static final Logger log = LoggerFactory.getLogger(ItemPacketHandler.class);
	private static final int MALE_MAX_HAIR_STYLE = 4;

	private final GameSession session;

	private volatile boolean soulshotCharged;
	private volatile int chargedGrade = -1;
	private volatile boolean spiritshotCharged;
	private volatile boolean blessedSpiritshot;
	private volatile int chargedSpSGrade = -1;
	private final Set<Integer> autoSoulShots = ConcurrentHashMap.newKeySet();
	private volatile int activeEnchantScrollObjectId;
	private final Map<Integer, Long> consumableReuse = new ConcurrentHashMap<>();
	private final Map<String, ScheduledFuture<?>> hotTasks = new ConcurrentHashMap<>();

	public ItemPacketHandler(GameSession session) {
		this.session = session;
	}

	public boolean isSoulshotCharged() {
		return soulshotCharged;
	}

	public void setSoulshotCharged(boolean charged) {
		this.soulshotCharged = charged;
	}

	public int chargedGrade() {
		return chargedGrade;
	}

	public void setChargedGrade(int grade) {
		this.chargedGrade = grade;
	}

	public boolean isSpiritshotCharged() {
		return spiritshotCharged;
	}

	public void setSpiritshotCharged(boolean charged) {
		this.spiritshotCharged = charged;
	}

	public boolean isBlessedSpiritshot() {
		return blessedSpiritshot;
	}

	public void setBlessedSpiritshot(boolean blessed) {
		this.blessedSpiritshot = blessed;
	}

	public int chargedSpSGrade() {
		return chargedSpSGrade;
	}

	public void setChargedSpSGrade(int grade) {
		this.chargedSpSGrade = grade;
	}

	public Set<Integer> autoSoulShots() {
		return autoSoulShots;
	}

	public int activeEnchantScrollObjectId() {
		return activeEnchantScrollObjectId;
	}

	public void activeEnchantScrollObjectId(int id) {
		this.activeEnchantScrollObjectId = id;
	}

	public Map<Integer, Long> consumableReuse() {
		return consumableReuse;
	}

	public Map<String, ScheduledFuture<?>> hotTasks() {
		return hotTasks;
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
				session.useScrollOfEscape(item);
				return;
			}
			if (item.itemId() == 1665 || item.itemId() == 1863) {
				session.onShowMiniMap();
				return;
			}
			if (EnchantScrollTable.isEnchantScroll(item.itemId())) {
				activeEnchantScrollObjectId = item.objectId();
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
				if (ctx.extractableItems().extract(active, item, session::send)) {
					session.send(ItemList.of(active.inventory().items(), false));
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
			var consumable = ConsumableTable.get(item.itemId());
			if (consumable.isPresent()) {
				useConsumable(consumable.get());
			} else {
				session.send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED, new SystemMessage.ItemName(item.itemId())));
			}
			session.send(new ActionFailed());
			return;
		}
		if (active.sitting()) {
			session.send(new ActionFailed());
			return;
		}
		if (ctx != null && ctx.inventories() != null) {
			afterEquipChange(ctx.inventories().toggleEquip(active.inventory(), item.objectId()));
		}
	}

	public void handleUnEquip(RequestUnEquipItem p) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (!session.inWorld() || active == null || ctx == null || ctx.inventories() == null) {
			return;
		}
		afterEquipChange(ctx.inventories().unequipBodyPart(active.inventory(), p.bodyPart()));
	}

	public void handleDestroyItem(RequestDestroyItem p) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (!session.inWorld() || active == null || active.isDead() || ctx == null || ctx.inventories() == null) {
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

	public void handleEnchantItem(RequestEnchantItem p) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (!session.inWorld() || active == null || active.isDead() || ctx == null || ctx.inventories() == null) {
			session.send(new ActionFailed());
			return;
		}
		int scrollObjectId = activeEnchantScrollObjectId;
		activeEnchantScrollObjectId = 0;
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
		if (!target.template().isEquipable()) {
			session.send(SystemMessage.id(SystemMessage.INAPPROPRIATE_ENCHANT_CONDITION));
			session.send(EnchantResult.CANCEL);
			return;
		}

		String targetGrade = target.template().crystalType();
		if (targetGrade == null || !targetGrade.equalsIgnoreCase(scrollInfo.grade())) {
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

		int safeLimit = (target.template().bodyPart() == ItemSlots.SLOT_FULL_ARMOR) ? 4 : 3;
		boolean success;
		if (target.enchant() < safeLimit) {
			success = true;
		} else {
			success = java.util.concurrent.ThreadLocalRandom.current().nextInt(100) < 66;
		}

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
			session.send(new InventoryUpdate(List.of(ItemInfo.of(target, ItemInfo.MODIFIED))));
			if (target.isEquipped()) {
				updateArmorSetBonus();
				updateEquippedItemSkills();
				session.sendUserInfoAndBroadcastCharInfo();
			}
			session.broadcastAppearance();
		} else {
			if (scrollInfo.isBlessed()) {
				target.enchant(0);
				ctx.inventories().saveItem(target);
				session.send(SystemMessage.id(SystemMessage.BLESSED_ENCHANT_FAILED));
				session.send(EnchantResult.BLESSED_FAIL);
				session.send(new InventoryUpdate(List.of(ItemInfo.of(target, ItemInfo.MODIFIED))));
				if (target.isEquipped()) {
					updateArmorSetBonus();
					updateEquippedItemSkills();
					session.sendUserInfoAndBroadcastCharInfo();
				}
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
				session.broadcastAppearance();
			}
		}
	}

	public void handleAutoSoulShot(RequestAutoSoulShot p) {
		PlayerCharacter active = session.activeChar();
		if (!session.inWorld() || active == null || active.isDead()) {
			return;
		}
		if (!ConsumableTable.isShot(p.itemId())) {
			session.send(new ExAutoSoulShot(p.itemId(), 0));
			return;
		}
		if (active.inventory().byItemId(p.itemId()).isEmpty()) {
			return;
		}
		var name = new SystemMessage.ItemName(p.itemId());
		if (p.type() == 1) {
			autoSoulShots.add(p.itemId());
			session.send(new ExAutoSoulShot(p.itemId(), 1));
			session.send(SystemMessage.of(SystemMessage.USE_OF_S1_WILL_BE_AUTO, name));
			if (ConsumableTable.isSoulshot(p.itemId())) {
				chargeSoulShot(p.itemId(), false);
			} else {
				chargeSpiritShot(p.itemId(), false, ConsumableTable.isBlessedSpiritshot(p.itemId()));
			}
		} else {
			autoSoulShots.remove(p.itemId());
			session.send(new ExAutoSoulShot(p.itemId(), 0));
			session.send(SystemMessage.of(SystemMessage.AUTO_USE_OF_S1_CANCELLED, name));
		}
	}

	public void handleBuyItem(RequestBuyItem p) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (!session.inWorld() || p.items().isEmpty() || active == null || ctx == null || ctx.buylists() == null) {
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

		if (active.inventory().size() + slots > 80) {
			session.send(SystemMessage.id(SystemMessage.SLOTS_FULL));
			session.send(new ActionFailed());
			return;
		}

		List<ItemInfo> updates = new ArrayList<>();
		if (totalCost > 0) {
			var consumed = ctx.inventories().consumeItem(active.inventory(), ItemTemplate.ADENA_ID, (int) totalCost, "Buy");
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
		if (!session.inWorld() || p.items().isEmpty() || active == null || ctx == null || ctx.inventories() == null) {
			session.send(new ActionFailed());
			return;
		}
		long totalEarned = 0;
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
			int pricePerItem = Math.max(1, item.template().price() / 2);
			totalEarned += (long) pricePerItem * count;
			var upd = ctx.inventories().destroyItem(active.inventory(), item.objectId(), count, "Sell");
			if (upd != null) {
				updates.add(upd.removed()
						? ItemInfo.of(upd.item(), ItemInfo.REMOVED)
						: ItemInfo.of(upd.item(), ItemInfo.MODIFIED));
			}
		}
		if (totalEarned > 0) {
			var adenaUpd = ctx.inventories().addItem(active.inventory(), ItemTemplate.ADENA_ID, (int) totalEarned, "SellReward");
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
					? SystemMessage.of(SystemMessage.EQUIPMENT_S1_S2_REMOVED, new SystemMessage.Number(item.enchant()), name)
					: SystemMessage.of(SystemMessage.S1_DISARMED, name));
		}
		updateArmorSetBonus();
		updateEquippedItemSkills();
		updateAugmentationBonus();
		session.send(InventoryUpdate.modified(r.changed()));
		session.sendUserInfoAndBroadcastCharInfo();
		var t = ctx != null && ctx.characters() != null ? ctx.characters().template(active) : null;
		if (t != null) {
			var stats = PlayerStats.calculate(active, t);
			if (stats.gradePenalty() > 0) {
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "The equipment's grade is too high. A penalty is applied."));
			}
		}
		if (r.changed().stream().anyMatch(i -> i.template().kind() == Kind.WEAPON)) {
			soulshotCharged = false;
			rechargeAutoSoulShots();
		}
	}

	public void updateArmorSetBonus() {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (ctx == null || ctx.armorSets() == null || active == null) {
			return;
		}
		var set = ctx.armorSets().findMatchingSet(active.inventory());
		Set<Integer> newSetSkills = new java.util.HashSet<>();
		if (set != null) {
			if (set.skillId() > 0) newSetSkills.add(set.skillId());
			if (set.shieldSkillId() > 0 && set.hasShield(active.inventory())) newSetSkills.add(set.shieldSkillId());
			if (set.enchant6Skill() > 0 && set.isEnchanted6(active.inventory())) newSetSkills.add(set.enchant6Skill());
		}

		Set<Integer> currentSetSkills = active.armorSetSkillIds();
		boolean changed = false;

		for (int skId : currentSetSkills) {
			if (!newSetSkills.contains(skId)) {
				active.skills().remove(skId);
				changed = true;
			}
		}

		for (int skId : newSetSkills) {
			if (!active.skills().containsKey(skId)) {
				active.skills().put(skId, 1);
				changed = true;
			}
		}

		currentSetSkills.clear();
		currentSetSkills.addAll(newSetSkills);

		if (set == null) {
			active.clearArmorSetBonus();
		} else {
			active.setArmorSetBonus(set.chest(), List.of());
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
		boolean changed = false;

		for (var entry : currentItemSkills.entrySet()) {
			int skillId = entry.getKey();
			if (!newItemSkills.containsKey(skillId)) {
				active.skills().remove(skillId);
				changed = true;
			}
		}

		for (var entry : newItemSkills.entrySet()) {
			int skillId = entry.getKey();
			int lvl = entry.getValue();
			Integer curLvl = active.skills().get(skillId);
			if (curLvl == null || curLvl != lvl) {
				active.skills().put(skillId, lvl);
				changed = true;
			}
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
				case "CON" -> active.augCON(active.augCON() + (int) s.value());
				case "INT" -> active.augINT(active.augINT() + (int) s.value());
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
		Long readyAt = consumableReuse.get(c.skillId());
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
			if (active.charges() >= 2) {
				session.send(SystemMessage.id(SystemMessage.FORCE_MAXLEVEL_REACHED));
				return;
			}
		}
		if (!consumeItem(c.itemId(), 1)) {
			return;
		}
		if (c.reuseMs() > 0) {
			consumableReuse.put(c.skillId(), now + c.reuseMs());
		}
		session.send(SystemMessage.of(SystemMessage.USE_S1, new SystemMessage.ItemName(c.itemId())));
		session.broadcastSelfSkill(c.skillId(), c.level());

		switch (c.type()) {
			case HEAL_HP -> {
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
			case HEAL_CP -> {
				double before = active.currentCp();
				active.currentCp(before + c.amount());
				session.send(SystemMessage.of(SystemMessage.S1_CP_WILL_BE_RESTORED,
						new SystemMessage.Number((int) (active.currentCp() - before))));
				session.sendVitals();
			}
			case HOT_HP, HOT_MP -> startHealOverTime(c);
			case BUFF -> applyBuff(c);
			case FACE, HAIR_COLOR, HAIR_STYLE -> changeAppearance(c);
			case MYSTERY -> startBigHead(c);
			case ENERGY_STONE -> session.increaseCharges(1, 2);
			case REMEDY -> {
			}
			default -> {
			}
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
		var previous = hotTasks.remove("BigHead");
		if (previous != null) {
			previous.cancel(false);
		}
		owner.startAbnormalEffect(ConsumableTable.ABNORMAL_BIG_HEAD);
		session.send(SystemMessage.of(SystemMessage.YOU_FEEL_S1_EFFECT, new SystemMessage.SkillName(c.skillId(), c.level())));
		session.broadcastAppearance();
		AtomicReference<ScheduledFuture<?>> self = new AtomicReference<>();
		ScheduledFuture<?> task = session.autoAttackScheduler().schedule(() -> {
			if (!hotTasks.remove("BigHead", self.get())) {
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
		hotTasks.put("BigHead", task);
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
		session.send(new InventoryUpdate(List.of(ItemInfo.of(r.item(), r.removed() ? ItemInfo.REMOVED : ItemInfo.MODIFIED))));
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
		var previous = hotTasks.remove(stack);
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
		hotTasks.put(stack, task);
	}

	private void stopHot(String stack, ScheduledFuture<?> task) {
		if (task != null) {
			task.cancel(false);
			hotTasks.remove(stack, task);
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
		int maxBuffs = Config.MAX_BUFFS_AMOUNT > 0 ? Config.MAX_BUFFS_AMOUNT : 20;
		var currentActive = owner.effects().active();
		if (currentActive.size() >= maxBuffs && !owner.effects().hasSkill(c.skillId())) {
			owner.effects().remove(currentActive.get(0));
		}
		owner.effects().put(buff);
		session.send(SystemMessage.of(SystemMessage.YOU_FEEL_S1_EFFECT, new SystemMessage.SkillName(c.skillId(), c.level())));
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
		if (soulshotCharged) {
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
		if (!consumeItem(itemId, count)) {
			if (autoSoulShots.remove(itemId)) {
				session.send(new ExAutoSoulShot(itemId, 0));
				session.send(SystemMessage.of(SystemMessage.AUTO_USE_OF_S1_CANCELLED, new SystemMessage.ItemName(itemId)));
			} else {
				session.send(SystemMessage.id(SystemMessage.NOT_ENOUGH_SOULSHOTS));
			}
			return false;
		}
		chargedGrade = weaponGrade;
		soulshotCharged = true;
		session.send(SystemMessage.id(SystemMessage.ENABLED_SOULSHOT));
		session.broadcastSelfSkill(c.skillId(), 1);
		return true;
	}

	public boolean chargeSpiritShot(int itemId, boolean quiet, boolean blessed) {
		if (spiritshotCharged) {
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
			if (autoSoulShots.remove(itemId)) {
				session.send(new ExAutoSoulShot(itemId, 0));
				session.send(SystemMessage.of(SystemMessage.AUTO_USE_OF_S1_CANCELLED, new SystemMessage.ItemName(itemId)));
			} else {
				session.send(SystemMessage.id(SystemMessage.NOT_ENOUGH_SOULSHOTS));
			}
			return false;
		}
		chargedSpSGrade = weaponGrade;
		blessedSpiritshot = blessed;
		spiritshotCharged = true;
		session.send(SystemMessage.id(SystemMessage.ENABLED_SOULSHOT));
		session.broadcastSelfSkill(c.skillId(), 1);
		return true;
	}

	public void rechargeAutoSoulShots() {
		for (int itemId : autoSoulShots) {
			if (ConsumableTable.isSoulshot(itemId)) {
				if (!soulshotCharged && chargeSoulShot(itemId, true)) {
					// soulshot carregado
				}
			} else if (ConsumableTable.isSpiritshot(itemId)) {
				if (!spiritshotCharged && chargeSpiritShot(itemId, true, ConsumableTable.isBlessedSpiritshot(itemId))) {
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
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Thief Key e consumida pela habilidade Unlock (ladinos)."));
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

		var npcTarget = ctx.world().npc(session.targetObjectId()).filter(n -> !n.isDead() && isChestNpc(n)).orElse(null);
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
}
