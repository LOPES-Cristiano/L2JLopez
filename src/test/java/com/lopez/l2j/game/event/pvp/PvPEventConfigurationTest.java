package com.lopez.l2j.game.event.pvp;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PvPEventConfigurationTest {

	private boolean origTvtEnabled;
	private boolean origCtfEnabled;
	private boolean origDmEnabled;
	private int origTvtMinLvl;
	private int origTvtMaxLvl;

	@BeforeEach
	void setUp() {
		origTvtEnabled = Config.TVT_ENABLED;
		origCtfEnabled = Config.CTF_ENABLED;
		origDmEnabled = Config.DM_ENABLED;
		origTvtMinLvl = Config.TVT_MIN_LEVEL;
		origTvtMaxLvl = Config.TVT_MAX_LEVEL;
	}

	@AfterEach
	void tearDown() {
		Config.TVT_ENABLED = origTvtEnabled;
		Config.CTF_ENABLED = origCtfEnabled;
		Config.DM_ENABLED = origDmEnabled;
		Config.TVT_MIN_LEVEL = origTvtMinLvl;
		Config.TVT_MAX_LEVEL = origTvtMaxLvl;
	}

	@Test
	void testTvtConfigurationAndRegistration() {
		TvtEventService service = new TvtEventService();

		assertEquals(Config.TVT_MIN_LEVEL, service.getMinLevel());
		assertEquals(Config.TVT_MAX_LEVEL, service.getMaxLevel());
		assertEquals(Config.TVT_REWARD_ID, service.getRewardItemId());
		assertEquals(Config.TVT_REWARD_AMOUNT, service.getRewardItemCount());

		PlayerCharacter player = createPlayer(101, "TvtFighter", 75);

		// 1. Quando TVT_ENABLED = false, registro eh recusado
		Config.TVT_ENABLED = false;
		service.openRegistration();
		var resDisabled = service.register(player);
		assertEquals(TvtEventService.RegisterResult.NOT_IN_REGISTRATION, resDisabled);

		// 2. Quando TVT_ENABLED = true, registro funciona para niveis validos
		Config.TVT_ENABLED = true;
		service.openRegistration();
		var resSuccess = service.register(player);
		assertEquals(TvtEventService.RegisterResult.SUCCESS, resSuccess);
		assertTrue(service.isParticipant(101));
	}

	@Test
	void testCtfConfigurationAndRegistration() {
		CtfEventService service = new CtfEventService();

		assertEquals(Config.CTF_MIN_LEVEL, service.getMinLevel());
		assertEquals(Config.CTF_MAX_LEVEL, service.getMaxLevel());
		assertEquals(Config.CTF_REWARD_ID, service.getRewardItemId());
		assertEquals(Config.CTF_REWARD_AMOUNT, service.getRewardItemCount());

		PlayerCharacter player = createPlayer(201, "CtfRunner", 78);

		Config.CTF_ENABLED = false;
		service.openRegistration();
		var resDisabled = service.register(player);
		assertEquals(CtfEventService.RegisterResult.NOT_IN_REGISTRATION, resDisabled);

		Config.CTF_ENABLED = true;
		service.openRegistration();
		var resSuccess = service.register(player);
		assertEquals(CtfEventService.RegisterResult.SUCCESS, resSuccess);
		assertTrue(service.isParticipant(201));
	}

	@Test
	void testDmConfigurationAndRegistration() {
		DmEventService service = new DmEventService();

		assertEquals(Config.DM_MIN_LEVEL, service.getMinLevel());
		assertEquals(Config.DM_MAX_LEVEL, service.getMaxLevel());
		assertEquals(Config.DM_REWARD_ID, service.getReward1ItemId());
		assertEquals(Config.DM_REWARD_AMOUNT, service.getReward1Count());

		PlayerCharacter player = createPlayer(301, "DmGladiator", 80);

		Config.DM_ENABLED = false;
		service.openRegistration();
		var resDisabled = service.register(player);
		assertEquals(DmEventService.RegisterResult.NOT_IN_REGISTRATION, resDisabled);

		Config.DM_ENABLED = true;
		service.openRegistration();
		var resSuccess = service.register(player);
		assertEquals(DmEventService.RegisterResult.SUCCESS, resSuccess);
		assertTrue(service.isParticipant(301));
	}

	private static PlayerCharacter createPlayer(int charId, String name, int level) {
		PlayerCharacter player = new PlayerCharacter(charId, "acc", name, level, 0, 0, 0, 0, 0, false, 0, 0, 0,
				1000, 500, 500, 0, 0, 0, 0, "", 0, 0, 0, 100, 100, 0, 0, 1000.0, 500.0, 500.0);
		player.level(level);
		return player;
	}
}
