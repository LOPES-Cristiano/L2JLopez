package com.lopez.l2j.game.dressme;

/**
 * Representa uma skin visual de armadura, capa ou arma do sistema DressMe (Item 30).
 */
public record DressMeEntry(
		int skillId,
		String name,
		String type,
		boolean vip,
		VisualArmor armor,
		VisualWeapon weapon,
		VisualEffect effect
) {
	public record VisualArmor(int chest, int legs, int gloves, int feet, int helmet) {}
	public record VisualWeapon(String type, int rhand, int lhand, int lrhand) {}
	public record VisualEffect(int skillId, int level, boolean recurring, int interval) {}

	public boolean isArmor() {
		return "ARMOR".equalsIgnoreCase(type) || "CLOAK".equalsIgnoreCase(type);
	}

	public boolean isWeapon() {
		return "WEAPON".equalsIgnoreCase(type);
	}
}
