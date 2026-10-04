package com.lopez.l2j.features.achievements;

import java.util.Optional;

/**
 * Fabrica das condicoes a partir dos atributos do XML. Substitui as ~30 classes
 * Condition do L2JDream (uma por atributo) por lambdas, mantendo os mesmos nomes de atributo
 * para que o achievements.xml antigo continue valido.
 *
 * <p>Diferenca intencional: atributos booleanos com valor "false" nao criam condicao
 * (antes {@code mustBeHero="false"} ainda exigia ser heroi).
 */
final class Conditions {

	private Conditions() {
	}

	/** @return a condicao, ou vazio se o atributo nao e uma condicao (id, name, reward...). */
	static Optional<AchievementCondition> fromAttribute(String attribute, String value) {
		return switch (attribute) {
			case "minLevel" -> min(value, (p, v) -> p.level() >= v);
			case "minPvPCount" -> min(value, (p, v) -> p.pvpKills() >= v);
			case "minPkCount" -> min(value, (p, v) -> p.pkKills() >= v);
			case "minKarmaCount" -> min(value, (p, v) -> p.karma() >= v);
			case "minAdenaCount" -> min(value, (p, v) -> p.adena() >= v);
			case "minClanLevel" -> min(value, (p, v) -> p.inClan() && p.clanLevel() >= v);
			case "minClanMembersCount" -> min(value, (p, v) -> p.inClan() && p.clanMembers() >= v);
			case "crpAmmount" -> min(value, (p, v) -> p.inClan() && p.clanReputation() >= v);
			case "maxHP" -> min(value, (p, v) -> p.maxHp() >= v);
			case "maxMP" -> min(value, (p, v) -> p.maxMp() >= v);
			case "maxCP" -> min(value, (p, v) -> p.maxCp() >= v);
			case "minWeaponEnchant" -> min(value, (p, v) -> p.weaponEnchant() >= v);
			case "minHeadEnchant" -> min(value, (p, v) -> p.headEnchant() >= v);
			case "minChestEnchant" -> min(value, (p, v) -> p.chestEnchant() >= v);
			case "minFeetEnchant" -> min(value, (p, v) -> p.feetEnchant() >= v);
			case "minLegsEnchant" -> min(value, (p, v) -> p.legsEnchant() >= v);
			// grafia original com erro de digitacao ("Glovest") preservada por compatibilidade
			case "minGlovestEnchant" -> min(value, (p, v) -> p.glovesEnchant() >= v);
			case "minSkillEnchant" -> min(value, (p, v) -> p.maxSkillEnchant() >= v);
			case "minSubclassCount" -> min(value, (p, v) -> p.subclassCount() >= v);
			case "minOnlineTime" -> min(value, (p, v) -> p.onlineDays() >= v);
			case "raidToKill" -> min(value, (p, v) -> p.raidsKilled().contains((int) v));
			case "mustBeHero" -> flag(value, (p, c) -> p.hero());
			case "mustBeNoble" -> flag(value, (p, c) -> p.noble());
			case "mustBeClanLeader" -> flag(value, (p, c) -> p.clanLeader() && p.inClan());
			case "mustBeMarried" -> flag(value, (p, c) -> p.married());
			case "mustBeVip" -> flag(value, (p, c) -> p.vip());
			case "mustBeMageClass" -> flag(value, (p, c) -> p.mageClass());
			case "lordOfCastle" -> flag(value, (p, c) -> p.inClan() && p.castleLord());
			case "Cursedweapon" -> flag(value, (p, c) -> p.cursedWeaponEquipped());
			case "CompleteAchievements" -> min(value, (p, c, v) -> c.completedCount() >= v);
			case "itemAmmount" -> items(value);
			default -> Optional.empty();
		};
	}

	private interface NumericRule {
		boolean test(PlayerSnapshot p, long threshold);
	}

	private interface ContextRule {
		boolean test(PlayerSnapshot p, AchievementCondition.Context c, long threshold);
	}

	private static Optional<AchievementCondition> min(String value, NumericRule rule) {
		long threshold = parseLong(value);
		return Optional.of((p, c) -> rule.test(p, threshold));
	}

	private static Optional<AchievementCondition> min(String value, ContextRule rule) {
		long threshold = parseLong(value);
		return Optional.of((p, c) -> rule.test(p, c, threshold));
	}

	private static Optional<AchievementCondition> flag(String value, AchievementCondition rule) {
		return Boolean.parseBoolean(value.trim()) ? Optional.of(rule) : Optional.empty();
	}

	/** Formato "itemId,quantidade", como no XML original. */
	private static Optional<AchievementCondition> items(String value) {
		String[] parts = value.split(",");
		if (parts.length != 2) {
			throw new IllegalArgumentException("itemAmmount invalido (esperado 'id,qtd'): " + value);
		}
		int itemId = (int) parseLong(parts[0]);
		long amount = parseLong(parts[1]);
		return Optional.of((p, c) -> p.itemCount(itemId) >= amount);
	}

	private static long parseLong(String raw) {
		try {
			return Long.parseLong(raw.trim());
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Valor numerico invalido: '" + raw + "'", e);
		}
	}
}
