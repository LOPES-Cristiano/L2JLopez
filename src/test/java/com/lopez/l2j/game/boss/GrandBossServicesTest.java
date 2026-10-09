package com.lopez.l2j.game.boss;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.boss.BossStatus;
import com.lopez.l2j.game.boss.epic.AntharasService;
import com.lopez.l2j.game.boss.epic.BaiumService;
import com.lopez.l2j.game.boss.epic.SailrenService;
import com.lopez.l2j.game.boss.epic.ValakasService;
import com.lopez.l2j.game.boss.epic.ZakenService;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GrandBossServicesTest {

	@BeforeEach
	public void setup() {
		Config.QUEST_REQUIRED_FOR_BOSS = true;
		Config.VALAKAS_LAIR_CAPACITY = 500;
		Config.BAIUM_CHECK_QUEST_FOR_AWAKE = true;
		Config.QUEEN_ANT_MAX_SAFE_LEVEL = 48;
		Config.ZAKEN_MAX_LEVEL_IN_ZONE = 80;
		Config.ZAKEN_DOOR_CLOSED_DEFAULT = true;
		Config.ZAKEN_DOOR_OPEN_HOUR = "0";
		Config.ZAKEN_DOOR_OPEN_TIME = 5;
	}

	private PlayerCharacter createPlayer(int id, String name, int level) {
		return new PlayerCharacter(id, "acc", name, level, 0, 0, 0, 0, 0, false, 0, 0, 0,
				100, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100, 100, 100);
	}

	@Test
	public void testAntharasEntryRequirements() {
		AntharasService antharas = new AntharasService(null, null, null, null, null);
		PlayerCharacter player = createPlayer(1, "DragonSlayer", 78);

		// Without Portal Stone (3865) -> cannot enter
		assertFalse(antharas.canEnter(player), "Player without Portal Stone cannot enter Antharas Lair");

		// Add Portal Stone to inventory
		player.inventory().addItem(AntharasService.PORTAL_STONE, 1);

		assertTrue(antharas.canEnter(player), "Player with Portal Stone must be allowed to enter Antharas Lair");

		// Enter lair consumes 1 Portal Stone and teleports player
		boolean entered = antharas.enterLair(player);
		assertTrue(entered, "Enter lair should succeed");
		assertEquals(0, player.inventory().getItemCount(AntharasService.PORTAL_STONE), "Portal Stone must be consumed");
		assertTrue(antharas.getPlayersInside().contains(player.objectId()), "Player must be tracked inside lair");
	}

	@Test
	public void testValakasEntryCapacityAndRequirement() {
		ValakasService valakas = new ValakasService(null, null, null, null);
		PlayerCharacter player = createPlayer(2, "FireMage", 80);

		// Without Floating Stone (7267) -> cannot enter
		assertFalse(valakas.canEnter(player), "Player without Floating Stone cannot enter Valakas Lair");

		player.inventory().addItem(ValakasService.FLOATING_STONE, 1);
		assertTrue(valakas.canEnter(player), "Player with Floating Stone can enter Valakas Lair");

		// Test capacity limit
		Config.VALAKAS_LAIR_CAPACITY = 1;
		valakas.enterLair(player);

		PlayerCharacter player2 = createPlayer(3, "SecondPlayer", 80);
		player2.inventory().addItem(ValakasService.FLOATING_STONE, 1);
		assertFalse(valakas.canEnter(player2), "Second player cannot enter when capacity limit is reached");
	}

	@Test
	public void testBaiumCombatLock() {
		BaiumService baium = new BaiumService(null, null, null, null);
		PlayerCharacter player = createPlayer(4, "Paladin", 75);
		player.inventory().addItem(BaiumService.BLOODED_FABRIC, 1);

		assertTrue(baium.canEnter(player), "Player with Blooded Fabric can enter before combat starts");
		baium.enterFloor(player);

		// Once Baium awakens and fighting starts, room locks
		baium.setStatus(BossStatus.FIGHTING);
		assertEquals(BossStatus.FIGHTING, baium.getStatus());

		PlayerCharacter latePlayer = createPlayer(5, "LatePlayer", 75);
		latePlayer.inventory().addItem(BaiumService.BLOODED_FABRIC, 1);
		assertFalse(baium.canEnter(latePlayer), "Late players must be locked out while Baium is in FIGHTING state");
	}

	@Test
	public void testSailrenEntryWithGazkh() {
		SailrenService sailren = new SailrenService(null, null, null, null);
		PlayerCharacter player = createPlayer(6, "DinoHunter", 76);

		// Without Gazkh (8784)
		assertFalse(sailren.canEnter(player), "Cannot enter Sailren nest without Gazkh item");

		player.inventory().addItem(SailrenService.GAZKH, 1);
		assertTrue(sailren.canEnter(player), "Can enter Sailren nest with Gazkh");

		sailren.enterNest(player);
		assertEquals(0, player.inventory().getItemCount(SailrenService.GAZKH), "Gazkh item must be consumed");
		assertTrue(sailren.getPlayersInside().contains(player.objectId()));
	}

	@Test
	public void testZakenMaxLevelConfig() {
		assertEquals(80, Config.ZAKEN_MAX_LEVEL_IN_ZONE);
		assertTrue(Config.ZAKEN_DOOR_CLOSED_DEFAULT);
		assertEquals("0", Config.ZAKEN_DOOR_OPEN_HOUR);
		assertEquals(5, Config.ZAKEN_DOOR_OPEN_TIME);
	}
}
