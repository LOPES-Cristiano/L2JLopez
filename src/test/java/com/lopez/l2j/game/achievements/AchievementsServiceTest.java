package com.lopez.l2j.game.achievements;

import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AchievementsServiceTest {

	private AchievementsService achievementsService;

	private PlayerCharacter createPlayer(int id, String name, int level, int pvp, int pk) {
		return new PlayerCharacter(id, "acc", name, level, 0L, 0, 0, 0, 0, false,
				0, 0, 0, 1000, 500, 300, 0, pvp, pk, 0, "", 0, 0L, 0L, 0, 0, 0, 0, 1000.0, 500.0, 300.0);
	}

	@BeforeEach
	void setUp() {
		achievementsService = new AchievementsService();
	}

	@Test
	void testDefaultAchievementsRegistered() {
		assertFalse(achievementsService.getAchievements().isEmpty());
		assertNotNull(achievementsService.getAchievement(1)); // First Steps
		assertNotNull(achievementsService.getAchievement(5)); // First Blood
	}

	@Test
	void testCheckAvailableAchievements() {
		PlayerCharacter player = createPlayer(1, "Hero", 25, 2, 0);

		List<AchievementDef> ready = achievementsService.checkAvailableAchievements(player, 100_000L, 0, 0, 5L);
		// Level 20 achievement (id 1) and First Blood (id 5) should be ready
		assertTrue(ready.stream().anyMatch(a -> a.id() == 1));
		assertTrue(ready.stream().anyMatch(a -> a.id() == 5));
		// Level 40 achievement (id 2) should NOT be ready
		assertFalse(ready.stream().anyMatch(a -> a.id() == 2));
	}

	@Test
	void testClaimAchievement() {
		PlayerCharacter player = createPlayer(10, "Warrior", 42, 1, 0);

		assertFalse(achievementsService.hasCompleted(10, 1));

		// Claim level 20 achievement
		boolean claimed = achievementsService.claimAchievement(player, 1, 0L, 0, 0, 0L);
		assertTrue(claimed);
		assertTrue(achievementsService.hasCompleted(10, 1));

		// Non-repeatable achievement cannot be claimed twice
		boolean claimedAgain = achievementsService.claimAchievement(player, 1, 0L, 0, 0, 0L);
		assertFalse(claimedAgain);

		// Claiming unmet achievement fails
		boolean claimedLv80 = achievementsService.claimAchievement(player, 4, 0L, 0, 0, 0L);
		assertFalse(claimedLv80);
	}

	@Test
	void testGenerateHtml() {
		PlayerCharacter player = createPlayer(10, "Warrior", 42, 1, 0);
		String html = achievementsService.generateHtml(player, 50_000L, 0, 0, 1L);
		assertNotNull(html);
		assertTrue(html.contains("Achievements System"));
		assertTrue(html.contains("First Steps"));
	}
}
