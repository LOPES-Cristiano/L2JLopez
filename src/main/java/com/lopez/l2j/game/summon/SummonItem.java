package com.lopez.l2j.game.summon;

/**
 * Definicao de item invocador (summon_items.xml).
 *
 * @param itemId id do item consumido/utilizado (ex: 2375 Wolf Collar)
 * @param npcId id do template do NPC invocado (ex: 12077 Wolf)
 * @param summonType 0 = static (objeto fixo), 1 = pet (mascote companheiro), 2 = mount/wyvern (montaria)
 */
public record SummonItem(int itemId, int npcId, int summonType) {

	public boolean isStatic() {
		return summonType == 0;
	}

	public boolean isPet() {
		return summonType == 1;
	}

	public boolean isMount() {
		return summonType == 2;
	}
}
