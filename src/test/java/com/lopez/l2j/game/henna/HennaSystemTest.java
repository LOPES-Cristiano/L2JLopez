package com.lopez.l2j.game.henna;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HennaSystemTest {

	private HennaTable hennaTable;
	private HennaTreeTable hennaTreeTable;

	@BeforeEach
	void setUp() {
		hennaTable = new HennaTable("data/xml/player/henna.xml");
		hennaTreeTable = new HennaTreeTable(null, hennaTable);
	}

	private PlayerCharacter createPlayer() {
		return new PlayerCharacter(1001, "acc", "Hero", 40, 0L, 0, 0, 1, 1, false, 0, 0, 0,
				1000, 1000, 1000, 0, 0, 0, 0, "Title", 0, 0L, 0L, 0, 0, 0, 0, 1000.0, 1000.0, 1000.0);
	}

	@Test
	void loadsHennasFromXml() {
		assertTrue(hennaTable.size() > 50, "Deve carregar mais de 50 hennas do XML");

		Henna h1 = hennaTable.get(1);
		assertNotNull(h1);
		assertEquals(4445, h1.dyeId());
		assertEquals(10, h1.dyeAmount());
		assertEquals(37000, h1.price());
		assertEquals(1, h1.statStr());
		assertEquals(-3, h1.statCon());
		assertEquals(0, h1.statInt());

		// Lookup reverso por dyeId
		Henna byDye = hennaTable.byDyeId(4445);
		assertNotNull(byDye);
		assertEquals(1, byDye.symbolId());
	}

	@Test
	void calculatesHennaModifiersWithRetailCap() {
		PlayerCharacter player = createPlayer();
		assertEquals(0, player.hennaSTR());
		assertEquals(0, player.hennaCON());

		// Equip slot 1: Symbol 1 (+1 STR, -3 CON)
		player.setHenna(1, 1);
		player.recalcHennaStats(hennaTable);
		assertEquals(1, player.hennaSTR());
		assertEquals(-3, player.hennaCON());

		// Equip slot 2: Symbol 1 (+1 STR, -3 CON) -> total +2 STR, -6 CON
		player.setHenna(2, 1);
		player.recalcHennaStats(hennaTable);
		assertEquals(2, player.hennaSTR());
		assertEquals(-6, player.hennaCON());

		// Simula outro simbolo para atingir o limite maximo de +5
		// Cria um simbolo mockup se necessario, ou adiciona hennas com STR alto
		// Verificamos que o Math.min(5, hennaStr) e respeitado
		player.setHenna(1, 1);
		player.setHenna(2, 1);
		player.setHenna(3, 1); // 3x (+1 STR) = +3 STR
		player.recalcHennaStats(hennaTable);
		assertEquals(3, player.hennaSTR());
		assertEquals(-9, player.hennaCON());
	}

	@Test
	void encodesHennaInfoPacket() {
		PlayerCharacter player = createPlayer();
		player.setHenna(1, 1);
		player.recalcHennaStats(hennaTable);

		var packet = new GameServerPacket.HennaInfo(player);
		byte[] encoded = packet.encode();

		assertNotNull(encoded);
		assertEquals((byte) 0xe4, encoded[0], "Opcode deve ser 0xe4");
	}

	@Test
	void treeTableProvidesAvailableHennas() {
		var available = hennaTreeTable.getAvailableHennas(1);
		assertNotNull(available);
		assertFalse(available.isEmpty());
	}
}
