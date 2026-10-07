package com.lopez.l2j.game.quest;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Catálogo Canônico das Quests de Acesso aos Grand Bosses e Instâncias Épicas de Lineage II Interlude.
 */
public final class GrandBossAccessCatalog {

	public record EpicBossAccessInfo(
			String bossName,
			int bossNpcId,
			int questId,
			String questName,
			int minLevel,
			int starterNpcId,
			int accessItemId,
			String accessItemName,
			int entranceNpcId
	) {}

	private static final Map<String, EpicBossAccessInfo> BOSSES = new LinkedHashMap<>();

	static {
		BOSSES.put("ANTHARAS", new EpicBossAccessInfo(
				"Antharas", 29019, 337, "337_AudienceWithTheLandDragon", 50, 30753, 3865, "Portal Stone", 30755
		));
		BOSSES.put("BAIUM", new EpicBossAccessInfo(
				"Baium", 29020, 348, "348_ArrogantSearch", 60, 30864, 4295, "Blooded Fabric", 30952
		));
		BOSSES.put("VALAKAS", new EpicBossAccessInfo(
				"Valakas", 29028, 618, "618_IntoTheFlame", 70, 31540, 7265, "Floating Stone", 31385
		));
		BOSSES.put("FRINTEZZA", new EpicBossAccessInfo(
				"Frintezza", 29045, 119, "119_LastImperialPrince", 74, 31453, 8073, "Frintezza's Magic Force Field Removal Scroll", 32011
		));
		BOSSES.put("SAILREN", new EpicBossAccessInfo(
				"Sailren", 29065, 641, "641_AttackSailren", 75, 32109, 8782, "Gazkh Fragment", 32110
		));
	}

	public static Map<String, EpicBossAccessInfo> getAllBosses() {
		return Collections.unmodifiableMap(BOSSES);
	}

	public static Optional<EpicBossAccessInfo> getByBossName(String bossName) {
		if (bossName == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(BOSSES.get(bossName.toUpperCase()));
	}

	public static boolean isBossAccessItem(int itemId) {
		return BOSSES.values().stream().anyMatch(b -> b.accessItemId() == itemId);
	}
}
