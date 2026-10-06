package com.lopez.l2j.game.castle.crown;

import java.util.Map;
import java.util.Set;

/**
 * Mapeamento e catalogo oficial de Coroas e Diademas de Castelo de Lineage II Interlude.
 * Portado de CrownTable.java do L2JDream.
 */
public final class CrownTable {

	public static final int LORDS_CROWN = 6841;

	// Castle Id -> Circlet Item Id
	public static final int GLUDIO_CIRCLET = 6838;
	public static final int DION_CIRCLET = 6835;
	public static final int GIRAN_CIRCLET = 6839;
	public static final int OREN_CIRCLET = 6837;
	public static final int ADEN_CIRCLET = 6840;
	public static final int INNADRIL_CIRCLET = 6834;
	public static final int GODDARD_CIRCLET = 6836;
	public static final int RUNE_CIRCLET = 8182;
	public static final int SCHUTTGART_CIRCLET = 8183;

	private static final Map<Integer, Integer> CASTLE_TO_CIRCLET = Map.of(
			1, GLUDIO_CIRCLET,
			2, DION_CIRCLET,
			3, GIRAN_CIRCLET,
			4, OREN_CIRCLET,
			5, ADEN_CIRCLET,
			6, INNADRIL_CIRCLET,
			7, GODDARD_CIRCLET,
			8, RUNE_CIRCLET,
			9, SCHUTTGART_CIRCLET
	);

	private static final Set<Integer> ALL_CROWNS = Set.of(
			LORDS_CROWN,
			GLUDIO_CIRCLET,
			DION_CIRCLET,
			GIRAN_CIRCLET,
			OREN_CIRCLET,
			ADEN_CIRCLET,
			INNADRIL_CIRCLET,
			GODDARD_CIRCLET,
			RUNE_CIRCLET,
			SCHUTTGART_CIRCLET
	);

	private CrownTable() {}

	public static int getCircletId(int castleId) {
		return CASTLE_TO_CIRCLET.getOrDefault(castleId, 0);
	}

	public static boolean isCrownOrCirclet(int itemId) {
		return ALL_CROWNS.contains(itemId);
	}

	public static Set<Integer> getAllCrownIds() {
		return ALL_CROWNS;
	}
}
