package com.lopez.l2j.network.game.handler.packet;

import com.lopez.l2j.game.henna.Henna;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.ActionFailed;
import com.lopez.l2j.network.game.packet.GameServerPacket.HennaEquipList;
import com.lopez.l2j.network.game.packet.GameServerPacket.HennaInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.HennaItemInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.HennaUnequipInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.HennaUnequipList;
import com.lopez.l2j.network.game.packet.GameServerPacket.InventoryUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.ItemList;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import com.lopez.l2j.network.game.packet.GameServerPacket.UserInfo;

import java.util.ArrayList;
import java.util.List;

public class HennaPacketHandler {

	private final GameSession session;

	public HennaPacketHandler(GameSession session) {
		this.session = session;
	}

	private GameSession.Context ctx() {
		return session.context();
	}

	private PlayerCharacter active() {
		return session.activeChar();
	}

	private void send(GameServerPacket p) {
		session.send(p);
	}

	public int getClassLevel(PlayerCharacter player) {
		if (player == null || ctx() == null || ctx().characters() == null) {
			return 0;
		}
		var tpl = ctx().characters().template(player);
		return tpl != null ? tpl.classTier() : 0;
	}

	public void loadHennasForCurrentSubclass() {
		PlayerCharacter active = active();
		if (active == null || ctx() == null) {
			return;
		}
		active.clearHennas();
		if (ctx().characterHennas() != null) {
			var hennas = ctx().characterHennas().restoreHennas(active.objectId(), active.classIndex());
			for (var entry : hennas.entrySet()) {
				int slot = entry.getKey();
				int symbolId = entry.getValue();
				if (slot >= 1 && slot <= 3) {
					active.setHenna(slot, symbolId);
				}
			}
		}
		active.recalcHennaStats(ctx().hennas(), ctx().hennaTrees());
	}

	public void handleHennaList() {
		PlayerCharacter active = active();
		if (active == null || ctx() == null || ctx().hennaTrees() == null || ctx().hennas() == null) {
			send(new ActionFailed());
			return;
		}
		int classLevel = getClassLevel(active);
		int emptySlots = active.getHennaEmptySlots(classLevel);
		var inv = active.inventory();
		long adena = inv != null ? inv.adena() : 0;
		var availableTree = ctx().hennaTrees().getAvailableHennas(active.classId());
		var list = new ArrayList<Henna>();
		if (inv != null) {
			for (var h : availableTree) {
				if (inv.getItemCount(h.dyeId()) > 0) {
					list.add(h);
				}
			}
		}
		send(new HennaEquipList((int) adena, emptySlots, list));
	}

	public void handleHennaItemInfo(int symbolId) {
		PlayerCharacter active = active();
		if (active == null || ctx() == null || ctx().hennas() == null) {
			send(new ActionFailed());
			return;
		}
		var h = ctx().hennas().get(symbolId);
		if (h == null) {
			send(new ActionFailed());
			return;
		}
		var tpl = ctx().characters() != null ? ctx().characters().template(active) : null;
		send(new HennaItemInfo(h, active, tpl));
	}

	public void handleHennaEquip(int symbolId) {
		PlayerCharacter active = active();
		if (active == null || ctx() == null || ctx().hennas() == null) {
			send(new ActionFailed());
			return;
		}
		if (active.isOlympiadMode()) {
			send(SystemMessage.id(SystemMessage.THE_SYMBOL_CANNOT_BE_DRAWN));
			return;
		}
		var h = ctx().hennas().get(symbolId);
		if (h == null) {
			send(SystemMessage.id(SystemMessage.THE_SYMBOL_CANNOT_BE_DRAWN));
			return;
		}
		if (ctx().hennaTrees() != null && !ctx().hennaTrees().isAllowed(active.classId(), symbolId)) {
			send(SystemMessage.id(SystemMessage.THE_SYMBOL_CANNOT_BE_DRAWN));
			return;
		}
		int classLevel = getClassLevel(active);
		if (active.getHennaEmptySlots(classLevel) == 0) {
			send(SystemMessage.id(SystemMessage.NO_SLOT_EXISTS_TO_DRAW_THE_SYMBOL));
			return;
		}
		var inv = active.inventory();
		if (inv == null || inv.adena() < h.price() || inv.getItemCount(h.dyeId()) < h.dyeAmount()) {
			send(SystemMessage.id(SystemMessage.THE_SYMBOL_CANNOT_BE_DRAWN));
			return;
		}

		int maxSlots = active.getMaxHennaSlots(classLevel);
		int targetSlot = -1;
		for (int i = 1; i <= maxSlots; i++) {
			if (active.getHenna(i) == 0) {
				targetSlot = i;
				break;
			}
		}
		if (targetSlot == -1) {
			send(SystemMessage.id(SystemMessage.NO_SLOT_EXISTS_TO_DRAW_THE_SYMBOL));
			return;
		}

		List<ItemInfo> updates = new ArrayList<>();
		if (ctx().inventories() != null) {
			var consumedDyes = ctx().inventories().consumeItem(inv, h.dyeId(), h.dyeAmount(), "HennaDraw");
			if (consumedDyes != null) {
				updates.add(ItemInfo.of(consumedDyes.item(), consumedDyes.removed() ? ItemInfo.REMOVED : ItemInfo.MODIFIED));
			}
			var consumedAdena = ctx().inventories().consumeItem(inv, ItemTemplate.ADENA_ID, h.price(), "HennaDraw");
			if (consumedAdena != null) {
				updates.add(ItemInfo.of(consumedAdena.item(), consumedAdena.removed() ? ItemInfo.REMOVED : ItemInfo.MODIFIED));
			}
		} else {
			inv.destroyItemByItemId(h.dyeId(), h.dyeAmount());
			inv.destroyItemByItemId(ItemTemplate.ADENA_ID, h.price());
		}

		active.setHenna(targetSlot, symbolId);
		if (ctx().characterHennas() != null) {
			ctx().characterHennas().saveHenna(active.objectId(), active.classIndex(), targetSlot, symbolId);
		}

		active.recalcHennaStats(ctx().hennas(), ctx().hennaTrees());
		var tpl = ctx().characters() != null ? ctx().characters().template(active) : null;
		if (tpl != null) {
			session.recalcMaxVitals(tpl);
		}

		send(SystemMessage.id(SystemMessage.THE_SYMBOL_HAS_BEEN_ADDED));
		send(new HennaInfo(active, classLevel, ctx().hennaTrees()));
		if (tpl != null) {
			send(new UserInfo(active, tpl));
		}
		if (!updates.isEmpty()) {
			send(new InventoryUpdate(updates));
		}
		session.refreshWeightAndPenalties();
	}

	public void handleHennaUnequipList() {
		PlayerCharacter active = active();
		if (active == null || ctx() == null || ctx().hennas() == null) {
			send(new ActionFailed());
			return;
		}
		int classLevel = getClassLevel(active);
		int emptySlots = active.getHennaEmptySlots(classLevel);
		var inv = active.inventory();
		long adena = inv != null ? inv.adena() : 0;

		var list = new ArrayList<Henna>();
		int maxSlots = active.getMaxHennaSlots(classLevel);
		for (int i = 1; i <= maxSlots; i++) {
			int symbolId = active.getHenna(i);
			if (symbolId > 0) {
				var h = ctx().hennas().get(symbolId);
				if (h != null) {
					list.add(h);
				}
			}
		}
		send(new HennaUnequipList((int) adena, emptySlots, list));
	}

	public void handleHennaUnequipInfo(int symbolId) {
		PlayerCharacter active = active();
		if (active == null || ctx() == null || ctx().hennas() == null) {
			send(new ActionFailed());
			return;
		}
		var h = ctx().hennas().get(symbolId);
		if (h == null) {
			send(new ActionFailed());
			return;
		}
		var tpl = ctx().characters() != null ? ctx().characters().template(active) : null;
		send(new HennaUnequipInfo(h, active, tpl));
	}

	public void handleHennaUnequip(int symbolId) {
		PlayerCharacter active = active();
		if (active == null || ctx() == null || ctx().hennas() == null) {
			send(new ActionFailed());
			return;
		}
		if (active.isOlympiadMode()) {
			return;
		}
		int classLevel = getClassLevel(active);
		int maxSlots = active.getMaxHennaSlots(classLevel);
		int targetSlot = -1;
		for (int i = 1; i <= maxSlots; i++) {
			if (active.getHenna(i) == symbolId) {
				targetSlot = i;
				break;
			}
		}
		if (targetSlot == -1) {
			send(new ActionFailed());
			return;
		}
		handleHennaRemoveBySlot(targetSlot);
	}

	public void handleHennaRemoveBySlot(int slot) {
		PlayerCharacter active = active();
		if (active == null || ctx() == null || ctx().hennas() == null) {
			send(new ActionFailed());
			return;
		}
		if (active.isOlympiadMode()) {
			return;
		}
		if (slot < 1 || slot > 3) {
			send(new ActionFailed());
			return;
		}
		int symbolId = active.getHenna(slot);
		if (symbolId <= 0) {
			send(new ActionFailed());
			return;
		}
		var h = ctx().hennas().get(symbolId);
		if (h == null) {
			send(new ActionFailed());
			return;
		}

		var inv = active.inventory();
		int fee = h.price() / 5;
		if (inv == null || inv.adena() < fee) {
			send(SystemMessage.id(SystemMessage.YOU_NOT_ENOUGH_ADENA));
			return;
		}
		List<ItemInfo> updates = new ArrayList<>();
		if (ctx().inventories() != null) {
			var consumedAdena = ctx().inventories().consumeItem(inv, ItemTemplate.ADENA_ID, fee, "HennaDelete");
			if (consumedAdena != null) {
				updates.add(ItemInfo.of(consumedAdena.item(), consumedAdena.removed() ? ItemInfo.REMOVED : ItemInfo.MODIFIED));
			}
			int refundDyes = h.dyeAmount() / 2;
			if (refundDyes > 0) {
				var addedDyes = ctx().inventories().addItem(inv, h.dyeId(), refundDyes, "HennaRefund");
				if (addedDyes != null) {
					updates.add(ItemInfo.of(addedDyes.item(), addedDyes.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED));
				}
			}
		} else {
			inv.destroyItemByItemId(ItemTemplate.ADENA_ID, fee);
			int refundDyes = h.dyeAmount() / 2;
			if (refundDyes > 0) {
				inv.addAdena(refundDyes);
			}
		}

		active.setHenna(slot, 0);

		if (ctx().characterHennas() != null) {
			ctx().characterHennas().deleteHenna(active.objectId(), active.classIndex(), slot);
		}

		active.recalcHennaStats(ctx().hennas(), ctx().hennaTrees());
		var tpl = ctx().characters() != null ? ctx().characters().template(active) : null;
		if (tpl != null) {
			session.recalcMaxVitals(tpl);
		}

		int classLevel = getClassLevel(active);
		send(SystemMessage.id(SystemMessage.THE_SYMBOL_HAS_BEEN_DELETED));
		send(new HennaInfo(active, classLevel, ctx().hennaTrees()));
		if (tpl != null) {
			send(new UserInfo(active, tpl));
		}
		if (!updates.isEmpty()) {
			send(new InventoryUpdate(updates));
		}
		session.refreshWeightAndPenalties();
	}

	public void handleCursedWeaponList() {
		PlayerCharacter active = active();
		if (active == null) {
			send(new ActionFailed());
			return;
		}
		if (ctx() == null || ctx().cursedWeapons() == null) {
			send(new GameServerPacket.ExCursedWeaponList(List.of()));
			return;
		}
		var ids = ctx().cursedWeapons().allWeapons().stream()
				.map(com.lopez.l2j.game.cursed.CursedWeapon::itemId)
				.toList();
		send(new GameServerPacket.ExCursedWeaponList(ids));
	}

	public void handleCursedWeaponLocation() {
		PlayerCharacter active = active();
		if (active == null) {
			send(new ActionFailed());
			return;
		}
		if (ctx() == null || ctx().cursedWeapons() == null) {
			send(new GameServerPacket.ExCursedWeaponLocation(List.of()));
			return;
		}
		var activeOrDropped = ctx().cursedWeapons().activeOrDroppedWeapons();
		var list = new ArrayList<GameServerPacket.CursedWeaponLocationInfo>();
		for (var cw : activeOrDropped) {
			int status = cw.isActive() ? 1 : 0;
			int x = cw.x();
			int y = cw.y();
			int z = cw.z();
			if (cw.isActive() && ctx().world() != null) {
				var carrier = ctx().world().player(cw.playerId()).orElse(null);
				if (carrier != null) {
					x = carrier.x();
					y = carrier.y();
					z = carrier.z();
				}
			}
			list.add(new GameServerPacket.CursedWeaponLocationInfo(cw.itemId(), status, x, y, z));
		}
		send(new GameServerPacket.ExCursedWeaponLocation(list));
	}
}
