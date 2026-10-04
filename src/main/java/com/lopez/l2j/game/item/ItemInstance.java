package com.lopez.l2j.game.item;

/**
 * Instancia de item (linha da tabela {@code items}). Mutavel: quantidade, encantamento e localizacao mudam com o
 * jogo. Acesso pela thread da conexao do dono.
 */
public final class ItemInstance {

	/** Valores gravados na coluna {@code loc} (ItemLocation do legado). */
	public enum Location {
		VOID, INVENTORY, PAPERDOLL, WAREHOUSE, CLANWH, PET, PET_EQUIP, LEASE, FREIGHT
	}

	private final int objectId;
	private final ItemTemplate template;
	private int ownerId;
	private int count;
	private int enchant;
	private Location location = Location.INVENTORY;
	private int locationData;
	private int customType1;
	private int customType2;
	private int mana = -1;

	public ItemInstance(int objectId, ItemTemplate template, int ownerId, int count) {
		this.objectId = objectId;
		this.template = template;
		this.ownerId = ownerId;
		this.count = Math.max(1, count);
	}

	public int objectId() { return objectId; }
	public ItemTemplate template() { return template; }
	public int itemId() { return template.id(); }
	public int ownerId() { return ownerId; }
	public void ownerId(int value) { this.ownerId = value; }
	public int count() { return count; }
	public void count(int value) { this.count = value; }
	public int enchant() { return enchant; }
	public void enchant(int value) { this.enchant = value; }
	public Location location() { return location; }
	public int locationData() { return locationData; }
	public int customType1() { return customType1; }
	public void customType1(int value) { this.customType1 = value; }
	public int customType2() { return customType2; }
	public void customType2(int value) { this.customType2 = value; }
	public int mana() { return mana; }
	public void mana(int value) { this.mana = value; }

	public void location(Location location, int data) {
		this.location = location;
		this.locationData = data;
	}

	public boolean isEquipped() {
		return location == Location.PAPERDOLL || location == Location.PET_EQUIP;
	}

	@Override
	public String toString() {
		return template.name() + "(" + template.id() + ") x" + count + " #" + objectId;
	}
}
