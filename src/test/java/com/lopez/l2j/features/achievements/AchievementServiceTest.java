package com.lopez.l2j.features.achievements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.events.AchievementCompletedEvent;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AchievementServiceTest {

	static class MemoryStore implements AchievementProgressStore {
		final Map<Integer, Map<Integer, Integer>> data = new HashMap<>();

		@Override
		public Set<Integer> completedIds(int ownerId) {
			return new HashSet<>(data.getOrDefault(ownerId, Map.of()).keySet());
		}

		@Override
		public void recordCompletion(int ownerId, int achievementId) {
			data.computeIfAbsent(ownerId, k -> new HashMap<>()).merge(achievementId, 1, Integer::sum);
		}
	}

	MemoryStore store;
	List<Object> events;
	AchievementService service;

	@BeforeEach
	void setUp() throws Exception {
		store = new MemoryStore();
		events = new ArrayList<>();
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(AchievementService.CATALOG_RESOURCE)) {
			service = new AchievementService(AchievementXmlParser.parse(in), store, events::add);
		}
	}

	@Test
	void levelEightyUnlocksChampionAndPublishesEventOnce() {
		PlayerSnapshot p = PlayerSnapshot.builder().ownerId(7).level(80).build();

		List<Achievement> first = service.evaluate(p);
		assertEquals(List.of("Champion"), first.stream().map(Achievement::name).toList());
		assertEquals(1, events.size());
		AchievementCompletedEvent ev = (AchievementCompletedEvent) events.get(0);
		assertEquals(7, ev.ownerId());
		assertEquals(Map.of(9142, 5L), ev.rewards());

		// segunda avaliacao nao repete (nao repetivel)
		assertTrue(service.evaluate(p).isEmpty());
		assertEquals(1, events.size());
		assertEquals(1, store.data.get(7).get(1));
	}

	@Test
	void plainLowLevelPlayerUnlocksNothing() {
		assertTrue(service.evaluate(PlayerSnapshot.builder().ownerId(1).level(10).build()).isEmpty());
		assertTrue(events.isEmpty());
	}

	@Test
	void progressIsPerPlayer() {
		service.evaluate(PlayerSnapshot.builder().ownerId(1).level(80).build());
		assertEquals(1, service.evaluate(PlayerSnapshot.builder().ownerId(2).level(80).build()).size());
	}

	@Test
	void multiConditionAchievementNeedsAllConditions() {
		// Slayer(18): raid 77771 + nivel 80
		PlayerSnapshot noRaid = PlayerSnapshot.builder().ownerId(1).level(80).build();
		assertFalse(service.evaluate(noRaid).stream().anyMatch(a -> a.id() == 18));
		PlayerSnapshot withRaid = PlayerSnapshot.builder().ownerId(1).level(80).raidsKilled(Set.of(77771)).build();
		assertTrue(service.evaluate(withRaid).stream().anyMatch(a -> a.id() == 18));
	}

	@Test
	void itemRuleChecksAmount() {
		PlayerSnapshot few = PlayerSnapshot.builder().ownerId(1).itemCounts(Map.of(6392, 199L)).build();
		assertFalse(service.evaluate(few).stream().anyMatch(a -> a.id() == 8));
		PlayerSnapshot enough = PlayerSnapshot.builder().ownerId(1).itemCounts(Map.of(6392, 200L)).build();
		assertTrue(service.evaluate(enough).stream().anyMatch(a -> a.id() == 8));
	}

	@Test
	void clanRulesRequireBeingInAClan() {
		PlayerSnapshot loner = PlayerSnapshot.builder().ownerId(1).clanLeader(true).build();
		assertFalse(service.evaluate(loner).stream().anyMatch(a -> a.id() == 6));
		PlayerSnapshot leader = PlayerSnapshot.builder().ownerId(2).clanId(5).clanLeader(true).clanLevel(1).clanMembers(1).build();
		assertTrue(service.evaluate(leader).stream().anyMatch(a -> a.id() == 6));
	}

	@Test
	void completeAchievementsCountsPreviousOnesAndClosesOnNextEvaluation() {
		// Jogador "perfeito": completa varias conquistas de uma vez; #14 (CompleteAchievements=18) exige 18 previas.
		PlayerSnapshot all = PlayerSnapshot.builder().ownerId(1).level(80).hero(true).noble(true)
				.weaponEnchant(20).pvpKills(1000).pkKills(1000).clanId(1).clanLeader(true).clanLevel(8).clanMembers(5)
				.clanReputation(100).maxHp(1000).maxMp(1000).maxCp(1000).karma(100).adena(100)
				.castleLord(true).mageClass(true).married(true).vip(true).subclassCount(1).onlineDays(1)
				.maxSkillEnchant(10).cursedWeaponEquipped(true).headEnchant(16).chestEnchant(16)
				.feetEnchant(16).legsEnchant(16).glovesEnchant(16)
				.itemCounts(Map.of(6392, 200L)).raidsKilled(Set.of(77771)).build();
		List<Achievement> done = service.evaluate(all);
		// #14 (CompleteAchievements=18) vem antes de 18 estarem prontas na ordem de id, entao sai na proxima avaliacao
		assertEquals(24, done.size());
		assertFalse(done.stream().anyMatch(a -> a.id() == 14));
		List<Achievement> second = service.evaluate(all);
		assertEquals(List.of(14), second.stream().map(Achievement::id).toList());
	}
}
