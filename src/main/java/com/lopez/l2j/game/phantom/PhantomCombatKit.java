package com.lopez.l2j.game.phantom;

import java.util.List;

/**
 * Define os 7 Kits Oficiais de Combate para Phantoms (Fake Players) - Onda C8:
 * FIGHTER_BURST, FIGHTER_DPS, ARCHER_KITE, MAGE_NUKE, DAGGER, MAGE_DOT_CC, HEALER_SUPPORT.
 */
public enum PhantomCombatKit {

	FIGHTER_BURST(
			"Fighter Burst",
			List.of(261, 6, 176, 420, 287), // Triple Sonic Buster, Sonic Blaster, Frenzy, Zealot, Lionheart
			40,
			150,
			false,
			false
	),
	FIGHTER_DPS(
			"Fighter DPS",
			List.of(284, 281, 35, 275), // Hurricane Assault, Fist Fury, Burning Fist, Soul Breaker
			40,
			150,
			false,
			false
	),
	ARCHER_KITE(
			"Archer Kite",
			List.of(56, 101, 343, 313), // Double Shot, Stun Shot, Hamstring Shot, Snipe
			250, // Min safe distance
			800, // Max firing distance
			true,
			false
	),
	MAGE_NUKE(
			"Mage Nuke",
			List.of(1230, 1177, 1239, 1232), // Prominence, Hydro Blast, Hurricane, Aura Flare
			100,
			750,
			false,
			false
	),
	DAGGER(
			"Dagger",
			List.of(16, 30, 321, 344), // Deadly Blow, Backstab, Shadow Step, Blinding Blow
			40,
			100,
			false,
			false
	),
	MAGE_DOT_CC(
			"Mage DoT/CC",
			List.of(1263, 1164, 1064, 1092, 1170), // Curse Gloom, Vampiric Claw, Silence, Fear, Anchor
			150,
			700,
			false,
			false
	),
	HEALER_SUPPORT(
			"Healer Support",
			List.of(1218, 1335, 1401, 1258, 1040), // Greater Battle Heal, Balance Life, Major Heal, Mass Resurrection, Shield
			150,
			800,
			false,
			true
	);

	private final String displayName;
	private final List<Integer> skills;
	private final int minDistance;
	private final int maxDistance;
	private final boolean kiting;
	private final boolean healer;

	PhantomCombatKit(String displayName, List<Integer> skills, int minDistance, int maxDistance, boolean kiting, boolean healer) {
		this.displayName = displayName;
		this.skills = skills;
		this.minDistance = minDistance;
		this.maxDistance = maxDistance;
		this.kiting = kiting;
		this.healer = healer;
	}

	public String displayName() {
		return displayName;
	}

	public List<Integer> skills() {
		return skills;
	}

	public int minDistance() {
		return minDistance;
	}

	public int maxDistance() {
		return maxDistance;
	}

	public boolean isKiting() {
		return kiting;
	}

	public boolean isHealer() {
		return healer;
	}
}
