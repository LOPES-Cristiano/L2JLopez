package com.lopez.l2j.game.quest;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Catálogo Canônico das Quests de Farm Endgame, Reagentes e Soul Crystals do Lineage II Interlude.
 *
 * Mapeia as principais jornadas econômicas de obtenção de Special Abilities (SA), receitas e partes S-Grade.
 */
public final class EndgameFarmCatalog {

	public record EndgameQuestEntry(
			int questId,
			String questName,
			int minLevel,
			int startNpcId,
			String startNpcName,
			String zoneName,
			String primaryRewardDesc) {
	}

	private static final Map<Integer, EndgameQuestEntry> BY_ID = new LinkedHashMap<>();

	static {
		// Quest 350: Enhance Your Weapon (Soul Crystals para SA)
		BY_ID.put(350, new EndgameQuestEntry(
				350,
				"Enhance Your Weapon",
				40,
				30115,
				"Magister Jurek",
				"Giran / Aden Magic Guild",
				"Red, Green and Blue Soul Crystals (Stages 0 to 13) for weapon Special Abilities"
		));

		// Quest 373: Supplier of Reagents (Alquimia no Ivory Tower)
		BY_ID.put(373, new EndgameQuestEntry(
				373,
				"Supplier of Reagents",
				57,
				30166,
				"Trader Wesley",
				"Ivory Tower Basement & Blazing Swamp",
				"Pure Silver, True Gold, Moonstone Shards and Reagents for Subclass & Nobless"
		));

		// Quest 617: Gather the Flames (Receitas S-Grade em FotG)
		BY_ID.put(617, new EndgameQuestEntry(
				617,
				"Gather the Flames",
				74,
				31539,
				"Blacksmith Vulcan",
				"Forge of the Gods",
				"1000 Torches exchangeable for S-Grade Weapon Recipes with Warsmith Rooney"
		));

		// Quest 619: Relics of the Old Empire (Receitas e partes S-Grade em IT)
		BY_ID.put(619, new EndgameQuestEntry(
				619,
				"Relics of the Old Empire",
				74,
				31538,
				"Ghost of Adventurer",
				"Imperial Tomb",
				"Broken Relics exchangeable for S-Grade Armor Key Materials and Recipes"
		));

		// Quest 605: Alliance with Ketra Orcs
		BY_ID.put(605, new EndgameQuestEntry(
				605,
				"Alliance with Ketra Orcs",
				74,
				31371,
				"Hierarch Wahkan",
				"Ketra Orc Outpost",
				"Ketra Alliance Badges, Buffalo Horns and access to Ketra shops/Raid Bosses"
		));

		// Quest 611: Alliance with Varka Silenos
		BY_ID.put(611, new EndgameQuestEntry(
				611,
				"Alliance with Varka Silenos",
				74,
				31378,
				"Naran Ashanuk",
				"Varka Silenos Stronghold",
				"Varka Alliance Badges, Mane of Ketra Orcs and access to Varka shops/Raid Bosses"
		));
	}

	private EndgameFarmCatalog() {
	}

	public static Map<Integer, EndgameQuestEntry> allEntries() {
		return Collections.unmodifiableMap(BY_ID);
	}

	public static Map<Integer, EndgameQuestEntry> getAllEntries() {
		return allEntries();
	}

	public static Optional<EndgameQuestEntry> findById(int questId) {
		return Optional.ofNullable(BY_ID.get(questId));
	}
}
