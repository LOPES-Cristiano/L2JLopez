package com.lopez.l2j.game.sevensigns;

import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.login.packet.PacketReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
	void encodesSSQStatusPacketCorrectly() {
		var ssqStatus = new GameServerPacket.SSQStatus(1, 1, 1,
				SevenSignsManager.CABAL_DAWN, SevenSignsManager.SEAL_AVARICE, 30, 180, 180, 0);
		byte[] bytes = ssqStatus.encode();
		var r = new PacketReader(bytes);

		assertThat(r.readC()).isEqualTo(0xf5);
		assertThat(r.readC()).isEqualTo(1); // page
		assertThat(r.readC()).isEqualTo(1); // period
		assertThat(r.readD()).isEqualTo(1); // cycle
		assertThat(r.readD()).isEqualTo(257); // period msgId
		assertThat(r.readD()).isEqualTo(258); // time msgId
		assertThat(r.readC()).isEqualTo(SevenSignsManager.CABAL_DAWN);
		assertThat(r.readC()).isEqualTo(SevenSignsManager.SEAL_AVARICE);
		assertThat(r.readD()).isEqualTo(30); // stoneContrib
		assertThat(r.readD()).isEqualTo(180); // adenaCollect
	}

	@Test
	void decodesRequestSSQStatusPacket() {
		byte[] body = new byte[] { (byte) 0xc7, 0x01 }; // opcode 0xc7, page 1
		var decoded = GameClientPacket.decode(GameClientPacket.State.IN_GAME, body);
		assertThat(decoded).isPresent();
		assertThat(decoded.get()).isInstanceOf(GameClientPacket.RequestSSQStatus.class);
		assertThat(((GameClientPacket.RequestSSQStatus) decoded.get()).page()).isEqualTo(1);
	}
}
