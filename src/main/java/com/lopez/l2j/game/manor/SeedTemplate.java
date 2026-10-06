package com.lopez.l2j.game.manor;

/**
 * Modelo de uma semente do sistema de Manor (data/xml/world/seeds.xml).
 */
public record SeedTemplate(
		int seedId,
		int castleId,
		int level,
		int cropId,
		int matureId,
		int reward1,
		int reward2,
		boolean isAlt,
		int limitSeeds,
		int limitCrops) {
}
