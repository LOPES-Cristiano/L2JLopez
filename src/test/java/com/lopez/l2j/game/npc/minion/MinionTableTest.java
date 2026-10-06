package com.lopez.l2j.game.npc.minion;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.junit.jupiter.api.Test;

class MinionTableTest {

	@Test
	void shouldLoadMinionsFromXmlIfPresent() {
		File file = new File("data/xml/world/minion.xml");
		if (!file.exists()) {
			return;
		}

		MinionTable table = new MinionTable("data/xml/world/minion.xml");
		table.load();

		assertThat(table.totalMasters()).isGreaterThan(0);
		assertThat(table.totalMinionRules()).isGreaterThan(0);

		// Exemplo: Queen Ant (29001) tem Royal Guards e Nurse Ants
		if (table.hasMinions(29001)) {
			var minions = table.getMinionsForBoss(29001);
			assertThat(minions).isNotEmpty();
			for (var m : minions) {
				assertThat(m.bossId()).isEqualTo(29001);
				assertThat(m.minionId()).isGreaterThan(0);
				assertThat(m.amountMax()).isGreaterThanOrEqualTo(m.amountMin());
			}
		}
	}
}
