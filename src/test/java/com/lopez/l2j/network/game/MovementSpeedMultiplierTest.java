package com.lopez.l2j.network.game;

import static org.assertj.core.api.Assertions.assertThat;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.PlayerStats;
import com.lopez.l2j.game.template.CharTemplate;
import com.lopez.l2j.network.game.packet.GameServerPacket.CharInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.UserInfo;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

class MovementSpeedMultiplierTest {

	@Test
	void shouldCalculateCorrectMovementAndAttackMultipliers() {
		CharTemplate tpl = new CharTemplate(
				0, "Human Fighter", 0,
				40, 43, 30, 21, 11, 25,
				4, 80, 6, 41, 300, 333,
				33, 44, 33, 120, 81900,
				0, 0, 0, false,
				9.0, 23.0, 8.0, 23.5,
				80, 30, 32, 1);

		PlayerCharacter player = new PlayerCharacter(
				1001, "acc", "Runner", 1, 0, 0, 0,
				0, 0, false, 0, 0, 0,
				100, 100, 100, 0, 0, 0, 0,
				"", 0, 0, 0, 0, 0, 0, 0,
				100.0, 100.0, 100.0);

		PlayerStats statsBase = PlayerStats.calculate(player, tpl);
		assertThat(statsBase.movementSpeedMultiplier(tpl)).isEqualTo(1.0);
		assertThat(statsBase.walkSpeed(tpl)).isEqualTo(80);

		// Aumenta a velocidade do personagem (simulando Wind Walk ou GM Speed)
		player.gmSpeed(2); // +100 run speed
		PlayerStats statsBuffed = PlayerStats.calculate(player, tpl);
		assertThat(statsBuffed.runSpeed()).isEqualTo(220); // 120 + 100

		double expectedMultiplier = 220.0 / 120.0;
		assertThat(statsBuffed.movementSpeedMultiplier(tpl)).isEqualTo(expectedMultiplier);
		assertThat(statsBuffed.walkSpeed(tpl)).isEqualTo((int) Math.round(80.0 * expectedMultiplier));

		// Valida encode do UserInfo
		UserInfo userInfo = new UserInfo(player, tpl, player.inventory().paperdollView(), 0, statsBuffed);
		byte[] userBytes = userInfo.encode();
		assertThat(userBytes).isNotEmpty();

		// Valida encode do CharInfo
		CharInfo charInfo = new CharInfo(player, tpl, player.inventory().paperdollView(), 0, 0, 0);
		byte[] charBytes = charInfo.encode();
		assertThat(charBytes).isNotEmpty();
	}
}
