package com.lopez.l2j.features.achievements;

import java.util.Map;
import java.util.Set;
import lombok.Builder;

/**
 * Fotografia imutavel dos fatos do jogador que as conquistas precisam avaliar.
 * Substitui o acoplamento das antigas Conditions com L2PcInstance: quem monta o
 * snapshot (o mundo do jogo) conhece o Player, as conquistas nao.
 */
@Builder
public record PlayerSnapshot(
		int ownerId,
		int level,
		int pvpKills,
		int pkKills,
		int karma,
		long adena,
		boolean hero,
		boolean noble,
		boolean married,
		boolean vip,
		boolean mageClass,
		boolean cursedWeaponEquipped,
		boolean clanLeader,
		boolean castleLord,
		int clanId,
		int clanLevel,
		int clanMembers,
		int clanReputation,
		int maxHp,
		int maxMp,
		int maxCp,
		int weaponEnchant,
		int headEnchant,
		int chestEnchant,
		int feetEnchant,
		int legsEnchant,
		int glovesEnchant,
		int maxSkillEnchant,
		int subclassCount,
		long onlineDays,
		Map<Integer, Long> itemCounts,
		Set<Integer> raidsKilled) {

	public PlayerSnapshot {
		itemCounts = itemCounts == null ? Map.of() : Map.copyOf(itemCounts);
		raidsKilled = raidsKilled == null ? Set.of() : Set.copyOf(raidsKilled);
	}

	public long itemCount(int itemId) {
		return itemCounts.getOrDefault(itemId, 0L);
	}

	public boolean inClan() {
		return clanId > 0;
	}
}
