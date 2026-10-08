package com.lopez.l2j.game.service;

import static org.junit.jupiter.api.Assertions.*;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VipAndVoteConfigurationTest {

	private VipService vipService;
	private VoteRewardService voteRewardService;

	private PlayerCharacter createPlayer(int objId, String name) {
		return new PlayerCharacter(objId, "acc", name, 80, 0L, 0, 0, 0, 0, false, 0, 0, 0,
				1000, 1000, 1000, 0, 0, 0, 0, "Title", 0, 0L, 0L, 0, 0, 0, 0, 1000.0, 1000.0, 1000.0);
	}

	@BeforeEach
	void setUp() {
		Config.load();
		vipService = new VipService();
		voteRewardService = new VoteRewardService();
	}

	@Test
	void testVipConfigLoading() {
		assertTrue(Config.ALLOW_VIP_NAME_COLOR);
		assertEquals("0088FF", Config.VIP_NAME_COLOR);
		assertTrue(Config.ALLOW_VIP_TITLE_COLOR);
		assertEquals("0088FF", Config.VIP_TITLE_COLOR);
		assertEquals(30, Config.VIP_DIAS);
		assertEquals(60, Config.VIP_DIAS_2);
		assertEquals(90, Config.VIP_DIAS_3);
	}

	@Test
	void testVipStatusAndRates() {
		PlayerCharacter player = createPlayer(2001, "VipChar");

		assertFalse(vipService.isVip(player));
		assertEquals(1.0f, vipService.getXpMultiplier(player));
		assertEquals(1.0f, vipService.getDropMultiplier(player));

		vipService.setVip(player, 30);
		assertTrue(vipService.isVip(player));
		assertEquals(Integer.decode("0x0088FF"), player.nameColor());
		assertEquals(Integer.decode("0x0088FF"), player.titleColor());

		// Rates com ALLOW_VIP_XPSP
		Config.ALLOW_VIP_XPSP = true;
		Config.VIP_XP = 2.0f;
		Config.VIP_SP = 2.0f;
		Config.VIP_DROP_RATE = 1.5f;
		Config.VIP_SPOIL_RATE = 1.5f;

		assertEquals(2.0f, vipService.getXpMultiplier(player));
		assertEquals(2.0f, vipService.getSpMultiplier(player));
		assertEquals(1.5f, vipService.getDropMultiplier(player));
		assertEquals(1.5f, vipService.getSpoilMultiplier(player));

		// Remove VIP
		vipService.removeVip(player);
		assertFalse(vipService.isVip(player));
		assertEquals(1.0f, vipService.getXpMultiplier(player));
	}

	@Test
	void testVipExpiration() {
		PlayerCharacter player = createPlayer(2002, "ExpiringVip");
		vipService.setVip(player, 1);
		assertTrue(vipService.isVip(player));

		// Força timestamp no passado
		player.vipExpiration(System.currentTimeMillis() - 5000L);
		assertFalse(vipService.isVip(player));
	}

	@Test
	void testVoteSystemConfigAndCooldown() {
		assertEquals(3470, Config.VOTE_SYSTEM_REWARD_ID);
		assertEquals(5, Config.VOTE_SYSTEM_REWARD_COUNT);
		assertEquals("e2ec0d41791613092ac03b6243ec6b87", Config.API_KEY_TOPZONE);
		assertEquals("14093", Config.SERVER_ID_KEY_TOPZONE);
		assertEquals("0vocH6Te6bpQ89H8", Config.API_KEY_HOPZONE);
		assertEquals("l2nightmare", Config.SERVER_ID_NETWORK);

		int charId = 3001;
		assertTrue(voteRewardService.canVote(charId));
		assertEquals(0L, voteRewardService.getCooldownRemainingMillis(charId));

		assertTrue(voteRewardService.recordVote(charId));
		assertFalse(voteRewardService.canVote(charId));
		assertTrue(voteRewardService.getCooldownRemainingMillis(charId) > 0);

		// Rejeita voto duplicado durante cooldown
		assertFalse(voteRewardService.recordVote(charId));

		// Limpa cooldown
		voteRewardService.clearCooldown(charId);
		assertTrue(voteRewardService.canVote(charId));
	}

	@Test
	void testAddOnConfigProperties() {
		assertEquals(12, Config.RAID_BOSS_INFO_PAGE_LIMIT);
		assertEquals(12, Config.RAID_BOSS_DROP_PAGE_LIMIT);
		assertEquals("(MMM dd, HH:mm)", Config.RAID_BOSS_DATE_FORMAT);
		assertEquals(0, Config.CLAN_SKILL_BY_ITEM);
		assertFalse(Config.CUSTOM_STARTER_ITEMS_ENABLED);
		assertFalse(Config.ANNOUNCE_HERO_LOGIN);
		assertFalse(Config.ANNOUNCE_VIP_LOGIN);
	}
}
