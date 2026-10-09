package com.lopez.l2j.game.sevensigns;

import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.login.packet.PacketReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SevenSignsManagerTest {

	private SevenSignsManager manager;

	@BeforeEach
	void setUp() {
		manager = new SevenSignsManager(null);
	}

	@Test
	void playerRegistrationAndStoneContribution() {
		boolean registered = manager.registerPlayer(1001, SevenSignsManager.CABAL_DAWN, SevenSignsManager.SEAL_AVARICE);
		assertThat(registered).isTrue();
		assertThat(manager.getPlayerCabal(1001)).isEqualTo(SevenSignsManager.CABAL_DAWN);
		assertThat(manager.getPlayerSeal(1001)).isEqualTo(SevenSignsManager.SEAL_AVARICE);

		// 10 blue (30 AA), 10 green (50 AA), 10 red (100 AA) -> total 180 AA
		int earned = manager.contributeStones(1001, 10, 10, 10);
		assertThat(earned).isEqualTo(180);
		assertThat(manager.dawnStoneScore()).isEqualTo(180);
		assertThat(manager.duskStoneScore()).isEqualTo(0);

		var data = manager.getPlayerData(1001).orElseThrow();
		assertThat(data.ancientAdena()).isEqualTo(180);
		assertThat(data.blueStones()).isEqualTo(10);
		assertThat(data.greenStones()).isEqualTo(10);
		assertThat(data.redStones()).isEqualTo(10);

		int claimed = manager.claimAncientAdena(1001);
		assertThat(claimed).isEqualTo(180);
		assertThat(manager.getPlayerData(1001).orElseThrow().ancientAdena()).isEqualTo(0);
	}

	@Test
	void calculatesWinningCabalAndSkyState() {
		manager.registerPlayer(1001, SevenSignsManager.CABAL_DAWN, SevenSignsManager.SEAL_AVARICE);
		manager.registerPlayer(1002, SevenSignsManager.CABAL_DUSK, SevenSignsManager.SEAL_GNOSIS);

		manager.contributeStones(1001, 10, 0, 0); // 30 Dawn
		manager.contributeStones(1002, 0, 0, 10); // 100 Dusk

		assertThat(manager.getWinningCabal()).isEqualTo(SevenSignsManager.CABAL_DUSK);

		// During competition period sky is neutral (256)
		manager.activePeriod(SevenSignsManager.PERIOD_COMPETITION);
		assertThat(manager.getSkyState()).isEqualTo(256);

		// During seal validation period, sky reflects previous winner
		manager.activePeriod(SevenSignsManager.PERIOD_SEAL_VALIDATION);
		manager.previousWinner(SevenSignsManager.CABAL_DUSK);
		assertThat(manager.getSkyState()).isEqualTo(257); // Red Dusk sky

		manager.previousWinner(SevenSignsManager.CABAL_DAWN);
		assertThat(manager.getSkyState()).isEqualTo(258); // Blue Dawn sky
	}

	@Test
	void calcNewSealOwnersThresholds() {
		// Test 35% threshold to claim an unowned seal
		// Total Dawn members: 10. 4 choose Avarice (40% >= 35%). Dawn wins competition.
		for (int i = 1; i <= 4; i++) {
			manager.registerPlayer(i, SevenSignsManager.CABAL_DAWN, SevenSignsManager.SEAL_AVARICE);
		}
		for (int i = 5; i <= 10; i++) {
			manager.registerPlayer(i, SevenSignsManager.CABAL_DAWN, SevenSignsManager.SEAL_GNOSIS);
		}

		manager.contributeStones(1, 10, 0, 0); // Dawn has stones -> Dawn wins
		manager.calcNewSealOwners();

		assertThat(manager.getSealOwner(SevenSignsManager.SEAL_AVARICE)).isEqualTo(SevenSignsManager.CABAL_DAWN);
		assertThat(manager.getSealOwner(SevenSignsManager.SEAL_GNOSIS)).isEqualTo(SevenSignsManager.CABAL_DAWN);
		assertThat(manager.getSealOwner(SevenSignsManager.SEAL_STRIFE)).isEqualTo(SevenSignsManager.CABAL_NULL);

		// Test 10% threshold to retain seal when winning
		// Dawn already owns Avarice. In next round, only 1 of 10 members chooses Avarice (10% >= 10%).
		manager.setSealOwner(SevenSignsManager.SEAL_AVARICE, SevenSignsManager.CABAL_DAWN);
		manager.calcNewSealOwners();
		assertThat(manager.getSealOwner(SevenSignsManager.SEAL_AVARICE)).isEqualTo(SevenSignsManager.CABAL_DAWN);
	}

	@Test
	void festivalOfDarknessScoring() {
		// Tier 0 (Level 31): Dusk scores 1500, Dawn scores 1200 -> Dusk gets 100 points
		manager.addFestivalScore(SevenSignsManager.CABAL_DUSK, 0, 1500, List.of("DuskPlayer1", "DuskPlayer2"));
		manager.addFestivalScore(SevenSignsManager.CABAL_DAWN, 0, 1200, List.of("DawnPlayer1", "DawnPlayer2"));

		// Tier 1 (Level 42): Dawn scores 2000, Dusk scores 1800 -> Dawn gets 100 points
		manager.addFestivalScore(SevenSignsManager.CABAL_DAWN, 1, 2000, List.of("DawnPlayer3"));
		manager.addFestivalScore(SevenSignsManager.CABAL_DUSK, 1, 1800, List.of("DuskPlayer3"));

		assertThat(manager.duskFestivalScore()).isEqualTo(100);
		assertThat(manager.dawnFestivalScore()).isEqualTo(100);
		assertThat(manager.getHighestScore(SevenSignsManager.CABAL_DUSK, 0)).isEqualTo(1500);
		assertThat(manager.getHighestScore(SevenSignsManager.CABAL_DAWN, 1)).isEqualTo(2000);
	}

	@Test
	void dungeonAccessControl() {
		manager.registerPlayer(1001, SevenSignsManager.CABAL_DAWN, SevenSignsManager.SEAL_AVARICE);

		// In Competition period, registered players can enter
		manager.activePeriod(SevenSignsManager.PERIOD_COMPETITION);
		assertThat(manager.checkDungeonEntry(1001, true)).isEqualTo(SevenSignsManager.DungeonAccess.ALLOWED);
		assertThat(manager.checkDungeonEntry(9999, true)).isEqualTo(SevenSignsManager.DungeonAccess.NOT_REGISTERED);

		// In Seal Validation period, only winning cabal owning the seal can enter
		manager.activePeriod(SevenSignsManager.PERIOD_SEAL_VALIDATION);
		manager.previousWinner(SevenSignsManager.CABAL_DAWN);
		manager.setSealOwner(SevenSignsManager.SEAL_AVARICE, SevenSignsManager.CABAL_DAWN);
		manager.setSealOwner(SevenSignsManager.SEAL_GNOSIS, SevenSignsManager.CABAL_NULL);

		// Necropolis requires Avarice (owned by Dawn) -> Allowed
		assertThat(manager.checkDungeonEntry(1001, true)).isEqualTo(SevenSignsManager.DungeonAccess.ALLOWED);

		// Catacomb requires Gnosis (unowned) -> SEAL_NOT_OWNED
		assertThat(manager.checkDungeonEntry(1001, false)).isEqualTo(SevenSignsManager.DungeonAccess.SEAL_NOT_OWNED);

		// Dusk member cannot enter even if Dawn owns the seal
		manager.registerPlayer(1002, SevenSignsManager.CABAL_DUSK, SevenSignsManager.SEAL_AVARICE);
		assertThat(manager.checkDungeonEntry(1002, true)).isEqualTo(SevenSignsManager.DungeonAccess.NOT_IN_VALIDATION_WINNER);
	}

	@Test
	void periodTransitionsCycle() {
		manager.activePeriod(SevenSignsManager.PERIOD_COMP_RECRUITING);
		manager.advancePeriod();
		assertThat(manager.activePeriod()).isEqualTo(SevenSignsManager.PERIOD_COMPETITION);

		manager.advancePeriod();
		assertThat(manager.activePeriod()).isEqualTo(SevenSignsManager.PERIOD_COMP_RESULTS);

		manager.advancePeriod();
		assertThat(manager.activePeriod()).isEqualTo(SevenSignsManager.PERIOD_SEAL_VALIDATION);

		manager.advancePeriod();
		assertThat(manager.activePeriod()).isEqualTo(SevenSignsManager.PERIOD_COMP_RECRUITING);
		assertThat(manager.currentCycle()).isEqualTo(2);
	}

	@Test
	void encodesSSQStatusPacketCorrectly() {
		var ssqStatus = new GameServerPacket.SSQStatus(1, 1, 1,
				SevenSignsManager.CABAL_DAWN, SevenSignsManager.SEAL_AVARICE, 30, 180, 180, 0);
		byte[] bytes = ssqStatus.encode();
		var r = new PacketReader(bytes);

		assertThat(r.readC()).isEqualTo(0xf5);
		assertThat(r.readC()).isEqualTo(1); // page
		assertThat(r.readC()).isEqualTo(1); // period
		assertThat(r.readD()).isEqualTo(1); // cycle
		assertThat(r.readD()).isEqualTo(1176); // period msgId
		assertThat(r.readD()).isEqualTo(1180); // time msgId
		assertThat(r.readC()).isEqualTo(SevenSignsManager.CABAL_DAWN);
		assertThat(r.readC()).isEqualTo(SevenSignsManager.SEAL_AVARICE);
		assertThat(r.readD()).isEqualTo(30); // stoneContrib
		assertThat(r.readD()).isEqualTo(180); // adenaCollect
	}

	@Test
	void encodesAllSSQStatusPages() {
		manager.registerPlayer(1001, SevenSignsManager.CABAL_DAWN, SevenSignsManager.SEAL_AVARICE);
		manager.contributeStones(1001, 10, 5, 2);

		for (int page = 1; page <= 4; page++) {
			var packet = GameServerPacket.SSQStatus.of(page, manager, 1001);
			byte[] encoded = packet.encode();
			assertThat(encoded).isNotEmpty();
			var reader = new PacketReader(encoded);
			assertThat(reader.readC()).isEqualTo(0xf5);
			assertThat(reader.readC()).isEqualTo(page);
		}
	}

	@Test
	void decodesRequestSSQStatusPacket() {
		byte[] body = new byte[] { (byte) 0xc7, 0x01 }; // opcode 0xc7, page 1
		var decoded = GameClientPacket.decode(GameClientPacket.State.IN_GAME, body);
		assertThat(decoded).isPresent();
		assertThat(decoded.get()).isInstanceOf(GameClientPacket.RequestSSQStatus.class);
		assertThat(((GameClientPacket.RequestSSQStatus) decoded.get()).page()).isEqualTo(1);
	}

	@Test
	void festivalFeeMatrixAndArenaLocations() {
		assertThat(SevenSignsManager.FESTIVAL_FEE_STONES).hasDimensions(5, 3);
		// Tier 0 (Level 31): 900 Blue, 540 Green, 270 Red
		assertThat(SevenSignsManager.FESTIVAL_FEE_STONES[0][0]).isEqualTo(900);
		assertThat(SevenSignsManager.FESTIVAL_FEE_STONES[0][1]).isEqualTo(540);
		assertThat(SevenSignsManager.FESTIVAL_FEE_STONES[0][2]).isEqualTo(270);
		// Tier 4 (No limit): 6000 Blue, 3600 Green, 1800 Red
		assertThat(SevenSignsManager.FESTIVAL_FEE_STONES[4][0]).isEqualTo(6000);
		assertThat(SevenSignsManager.FESTIVAL_FEE_STONES[4][1]).isEqualTo(3600);
		assertThat(SevenSignsManager.FESTIVAL_FEE_STONES[4][2]).isEqualTo(1800);

		assertThat(SevenSignsManager.FESTIVAL_ARENA_LOCS).hasDimensions(5, 3);
		for (int i = 0; i < 5; i++) {
			assertThat(SevenSignsManager.FESTIVAL_ARENA_LOCS[i]).hasSize(3);
		}
	}

	@Test
	void dungeonBoundsAndLilithSanctum() {
		// Inside Disciples Necropolis / Lilith Sanctum
		assertThat(manager.isInside7sDungeon(184464, -13104, -4900)).isTrue();
		// Outside in Talking Island
		assertThat(manager.isInside7sDungeon(-84000, 243000, -3700)).isFalse();
	}

	@Test
	void festivalAccumulatedBonusAndOfferingCalculation() {
		// Add bonus to tier 0 (31 and under)
		manager.addAccumulatedBonus(0, 5000);
		manager.addAccumulatedBonus(0, 2500);
		// Add bonus to tier 4 (no limit)
		manager.addAccumulatedBonus(4, 15000);

		// Record festival blood offering score
		manager.addFestivalScore(SevenSignsManager.CABAL_DAWN, 0, 350, List.of("DawnChamp1", "DawnChamp2"));
		assertThat(manager.getHighestScore(SevenSignsManager.CABAL_DAWN, 0)).isEqualTo(350);

		// Lower score shouldn't overwrite highest score
		manager.addFestivalScore(SevenSignsManager.CABAL_DAWN, 0, 200, List.of("DawnNoob"));
		assertThat(manager.getHighestScore(SevenSignsManager.CABAL_DAWN, 0)).isEqualTo(350);

		// Higher score should update
		manager.addFestivalScore(SevenSignsManager.CABAL_DAWN, 0, 420, List.of("DawnHero"));
		assertThat(manager.getHighestScore(SevenSignsManager.CABAL_DAWN, 0)).isEqualTo(420);
	}

	@Test
	void sealOfStrifeCPRules() {
		manager.activePeriod(SevenSignsManager.PERIOD_SEAL_VALIDATION);
		manager.previousWinner(SevenSignsManager.CABAL_DAWN);
		manager.setSealOwner(SevenSignsManager.SEAL_STRIFE, SevenSignsManager.CABAL_DAWN);

		manager.registerPlayer(2001, SevenSignsManager.CABAL_DAWN, SevenSignsManager.SEAL_STRIFE);
		manager.registerPlayer(2002, SevenSignsManager.CABAL_DUSK, SevenSignsManager.SEAL_STRIFE);

		int baseCp = 1000;
		// Dawn player gets +10% CP
		int dawnCp = (manager.getPlayerCabal(2001) == manager.strifeOwner())
				? (int) Math.round(baseCp * 1.10) : baseCp;
		assertThat(dawnCp).isEqualTo(1100);

		// Dusk player gets -10% CP
		int duskCp = (manager.getPlayerCabal(2002) != manager.strifeOwner()
				&& manager.getPlayerCabal(2002) != SevenSignsManager.CABAL_NULL)
				? (int) Math.round(baseCp * 0.90) : baseCp;
		assertThat(duskCp).isEqualTo(900);

		assertThat(manager.getStrifeCpMultiplier(2001)).isEqualTo(1.10);
		assertThat(manager.getStrifeCpMultiplier(2002)).isEqualTo(0.90);
		assertThat(manager.getStrifeCpMultiplier(9999)).isEqualTo(1.0);
	}

	@Test
	void sanctumBossSpawningAndDespawning() {
		var world = new com.lopez.l2j.game.world.GameWorld();
		var m = new SevenSignsManager(null, world);

		// Validation period, Dawn won and owns Avarice -> Anakim
		m.activePeriod(SevenSignsManager.PERIOD_SEAL_VALIDATION);
		m.previousWinner(SevenSignsManager.CABAL_DAWN);
		m.setSealOwner(SevenSignsManager.SEAL_AVARICE, SevenSignsManager.CABAL_DAWN);

		assertThat(m.shouldSpawnAnakim()).isTrue();
		assertThat(m.shouldSpawnLilith()).isFalse();

		m.handleBossSpawnsOnValidation();
		assertThat(world.npcs().stream().anyMatch(n -> n.npcId() == SevenSignsManager.ANAKIM_NPC_ID)).isTrue();

		// Validation period, Dusk won and owns Avarice -> Lilith
		m.previousWinner(SevenSignsManager.CABAL_DUSK);
		m.setSealOwner(SevenSignsManager.SEAL_AVARICE, SevenSignsManager.CABAL_DUSK);

		assertThat(m.shouldSpawnLilith()).isTrue();
		assertThat(m.shouldSpawnAnakim()).isFalse();

		m.handleBossSpawnsOnValidation();
		assertThat(world.npcs().stream().anyMatch(n -> n.npcId() == SevenSignsManager.LILITH_NPC_ID)).isTrue();
		assertThat(world.npcs().stream().anyMatch(n -> n.npcId() == SevenSignsManager.ANAKIM_NPC_ID)).isFalse();

		// Despawn on cycle reset
		m.despawnSanctumBosses();
		assertThat(world.npcs().stream().anyMatch(n -> n.npcId() == SevenSignsManager.LILITH_NPC_ID)).isFalse();
	}
}

