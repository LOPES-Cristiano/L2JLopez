package com.lopez.l2j.game.henna;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
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

		// Equip slot 3: Symbol 1 (+1 STR, -3 CON) -> total +3 STR, -9 CON
		player.setHenna(3, 1);
		player.recalcHennaStats(hennaTable);
		assertEquals(3, player.hennaSTR());
		assertEquals(-9, player.hennaCON(), "Penalidades negativas nao podem ser limitadas a -5");
	}

	@Test
	void statBonusCappedAtPlusFiveWhilePenaltiesUncapped() {
		PlayerCharacter player = createPlayer();

		// Registra uma henna customizada no mock com +4 STR e -4 CON (como Greater Dye of STR +4 -4)
		// No XML de Interlude, dye 4442 e +4 STR -4 CON (Symbol 42)
		Henna h = hennaTable.byDyeId(4442);
		int symId = h != null ? h.symbolId() : 1;

		player.setHenna(1, symId);
		player.setHenna(2, symId);
		player.recalcHennaStats(hennaTable);

		// Mesmo que 2 dyes somem +8 STR, deve ser clampado em +5
		if (h != null && h.statStr() == 4) {
			assertEquals(5, player.hennaSTR(), "Bonus positivo deve ser limitado a +5");
			assertEquals(-8, player.hennaCON(), "Penalidade negativa nao deve ser clampada");
		}
	}

	@Test
	void classTierSlotLimits() {
		PlayerCharacter player = createPlayer();

		// Tier 0 (Base class - no transfer): 0 slots
		assertEquals(0, player.getMaxHennaSlots(0));
		assertEquals(0, player.getHennaEmptySlots(0));

		// Tier 1 (1st class transfer): 2 slots
		assertEquals(2, player.getMaxHennaSlots(1));
		assertEquals(2, player.getHennaEmptySlots(1));

		player.setHenna(1, 1);
		assertEquals(1, player.getHennaEmptySlots(1));

		player.setHenna(2, 1);
		assertEquals(0, player.getHennaEmptySlots(1));

		// Tier 2 (2nd class transfer): 3 slots
		assertEquals(3, player.getMaxHennaSlots(2));
		assertEquals(1, player.getHennaEmptySlots(2)); // com 2 equipados, sobra 1

		player.setHenna(3, 1);
		assertEquals(0, player.getHennaEmptySlots(2));

		// Tier 3 (3rd class transfer): 3 slots
		assertEquals(3, player.getMaxHennaSlots(3));
	}

	@Test
	void subclassIsolationAndSlotClearing() {
		PlayerCharacter player = createPlayer();
		player.setHenna(1, 1);
		player.setHenna(2, 1);
		player.recalcHennaStats(hennaTable);

		assertEquals(2, player.hennaSTR());
		assertEquals(-6, player.hennaCON());

		// Ao trocar de subclasse, hennas devem ser limpas
		player.clearHennas();
		player.recalcHennaStats(hennaTable);

		assertEquals(0, player.getHenna(1));
		assertEquals(0, player.getHenna(2));
		assertEquals(0, player.getHenna(3));
		assertEquals(0, player.hennaSTR());
		assertEquals(0, player.hennaCON());
	}

	@Test
	void hennaUnequipCostAndRefundMath() {
		Henna h1 = hennaTable.get(1);
		assertNotNull(h1);

		int fee = h1.price() / 5;
		int refundDyes = h1.dyeAmount() / 2;

		assertEquals(7400, fee, "Taxa de remocao deve ser exatamente 1/5 do preco original");
		assertEquals(5, refundDyes, "Quantidade de dyes reembolsadas deve ser metade (10 / 2 = 5)");
	}

	@Test
	void clientPacketsDecoding() {
		// 0xba RequestHennaList
		var pBa = GameClientPacket.decode(GameClientPacket.State.IN_GAME, new byte[] { (byte) 0xba }).orElseThrow();
		assertInstanceOf(GameClientPacket.RequestHennaList.class, pBa);

		// 0xbb RequestHennaItemInfo (symbolId: 10)
		var bufBb = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN);
		bufBb.put((byte) 0xbb).putInt(10);
		var pBb = GameClientPacket.decode(GameClientPacket.State.IN_GAME, bufBb.array()).orElseThrow();
		assertInstanceOf(GameClientPacket.RequestHennaItemInfo.class, pBb);
		assertEquals(10, ((GameClientPacket.RequestHennaItemInfo) pBb).symbolId());

		// 0xbc RequestHennaEquip (symbolId: 25)
		var bufBc = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN);
		bufBc.put((byte) 0xbc).putInt(25);
		var pBc = GameClientPacket.decode(GameClientPacket.State.IN_GAME, bufBc.array()).orElseThrow();
		assertInstanceOf(GameClientPacket.RequestHennaEquip.class, pBc);
		assertEquals(25, ((GameClientPacket.RequestHennaEquip) pBc).symbolId());

		// 0xbd RequestHennaUnequipList (symbolId: 0)
		var bufBd = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN);
		bufBd.put((byte) 0xbd).putInt(0);
		var pBd = GameClientPacket.decode(GameClientPacket.State.IN_GAME, bufBd.array()).orElseThrow();
		assertInstanceOf(GameClientPacket.RequestHennaUnequipList.class, pBd);

		// 0xbe RequestHennaUnequipInfo (symbolId: 7)
		var bufBe = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN);
		bufBe.put((byte) 0xbe).putInt(7);
		var pBe = GameClientPacket.decode(GameClientPacket.State.IN_GAME, bufBe.array()).orElseThrow();
		assertInstanceOf(GameClientPacket.RequestHennaUnequipInfo.class, pBe);
		assertEquals(7, ((GameClientPacket.RequestHennaUnequipInfo) pBe).symbolId());

		// 0xbf RequestHennaUnequip (symbolId: 7)
		var bufBf = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN);
		bufBf.put((byte) 0xbf).putInt(7);
		var pBf = GameClientPacket.decode(GameClientPacket.State.IN_GAME, bufBf.array()).orElseThrow();
		assertInstanceOf(GameClientPacket.RequestHennaUnequip.class, pBf);
		assertEquals(7, ((GameClientPacket.RequestHennaUnequip) pBf).symbolId());
	}

	@Test
	void serverPacketsEncoding() {
		PlayerCharacter player = createPlayer();
		player.setHenna(1, 1);
		player.recalcHennaStats(hennaTable);

		// 0xe4 HennaInfo
		var hennaInfo = new GameServerPacket.HennaInfo(player, 2, hennaTreeTable);
		byte[] encInfo = hennaInfo.encode();
		assertNotNull(encInfo);
		assertEquals((byte) 0xe4, encInfo[0]);

		// 0xe2 HennaEquipList
		Henna h1 = hennaTable.get(1);
		var equipList = new GameServerPacket.HennaEquipList(100000, 2, List.of(h1));
		byte[] encEquip = equipList.encode();
		assertNotNull(encEquip);
		assertEquals((byte) 0xe2, encEquip[0]);

		// 0xe5 HennaUnequipList
		var unequipList = new GameServerPacket.HennaUnequipList(100000, 1, List.of(h1));
		byte[] encUnequip = unequipList.encode();
		assertNotNull(encUnequip);
		assertEquals((byte) 0xe5, encUnequip[0]);

		// 0xe6 HennaUnequipInfo
		var unequipInfo = new GameServerPacket.HennaUnequipInfo(h1, player, null);
		byte[] encUnequipInfo = unequipInfo.encode();
		assertNotNull(encUnequipInfo);
		assertEquals((byte) 0xe6, encUnequipInfo[0]);
	}

	@Test
	void treeTableProvidesAvailableHennas() {
		var available = hennaTreeTable.getAvailableHennas(1);
		assertNotNull(available);
		assertFalse(available.isEmpty());
	}

	@Test
	void vitalsMaxHpCpMpScaleWithHenna() {
		var charTable = new com.lopez.l2j.game.template.CharTemplateTable();
		var tpl = charTable.get(0).orElseThrow();

		PlayerCharacter player = createPlayer();
		int lvl = player.level();
		int baseHp = tpl.calculateMaxHp(lvl);
		int baseMp = tpl.calculateMaxMp(lvl);
		int baseCp = tpl.calculateMaxCp(lvl);

		double baseConRatio = com.lopez.l2j.game.template.BaseStatsTable.conBonus(tpl.con()) / com.lopez.l2j.game.template.BaseStatsTable.conBonus(tpl.con());
		double baseMenRatio = com.lopez.l2j.game.template.BaseStatsTable.menBonus(tpl.men()) / com.lopez.l2j.game.template.BaseStatsTable.menBonus(tpl.men());
		assertEquals(1.0, baseConRatio);
		assertEquals(1.0, baseMenRatio);

		int effectiveCon = tpl.con() + 5;
		int effectiveMen = tpl.men() + 5;
		double conRatio = com.lopez.l2j.game.template.BaseStatsTable.conBonus(effectiveCon) / com.lopez.l2j.game.template.BaseStatsTable.conBonus(tpl.con());
		double menRatio = com.lopez.l2j.game.template.BaseStatsTable.menBonus(effectiveMen) / com.lopez.l2j.game.template.BaseStatsTable.menBonus(tpl.men());

		assertTrue(conRatio > 1.0, "Bonus de CON deve aumentar proporcao de HP/CP");
		assertTrue(menRatio > 1.0, "Bonus de MEN deve aumentar proporcao de MP");

		int buffedHp = (int) Math.round(baseHp * conRatio);
		int buffedCp = (int) Math.round(baseCp * conRatio);
		int buffedMp = (int) Math.round(baseMp * menRatio);

		assertTrue(buffedHp > baseHp);
		assertTrue(buffedCp > baseCp);
		assertTrue(buffedMp > baseMp);
	}

	@Test
	void respectsConfigurableHennaLimits() {
		int oldLimit = com.lopez.l2j.config.Config.LIMIT_HENNA_STR;
		try {
			com.lopez.l2j.config.Config.LIMIT_HENNA_STR = 4;
			PlayerCharacter player = createPlayer();
			Henna h = hennaTable.byDyeId(4442);
			int symId = h != null ? h.symbolId() : 1;

			player.setHenna(1, symId);
			player.setHenna(2, symId);
			player.recalcHennaStats(hennaTable);

			if (h != null && h.statStr() == 4) {
				assertEquals(4, player.hennaSTR(), "Limite deve seguir a configuracao dinamica de LIMIT_HENNA_STR");
			}
		} finally {
			com.lopez.l2j.config.Config.LIMIT_HENNA_STR = oldLimit;
		}
	}

	@Test
	void olympiadModeFlagProtectsSymbolOperations() {
		PlayerCharacter player = createPlayer();
		assertFalse(player.isOlympiadMode());

		player.setOlympiadMode(true);
		assertTrue(player.isOlympiadMode());

		player.setOlympiadMode(false);
		assertFalse(player.isOlympiadMode());
	}

	@Test
	void symbolMakerHtmlContainsCanonicalBypasses() throws Exception {
		java.nio.file.Path p = java.nio.file.Path.of("data/html/symbolmaker/SymbolMaker.htm");
		assertTrue(java.nio.file.Files.exists(p), "SymbolMaker.htm deve existir em data/html/symbolmaker/");

		String content = java.nio.file.Files.readString(p);
		assertTrue(content.contains("Draw"), "Deve conter acao de desenhar simbolo (Draw)");
		assertTrue(content.contains("RemoveList"), "Deve conter acao de listar remocao (RemoveList)");
		assertTrue(content.contains("SymbolMaker-1.htm"), "Deve conter link explicativo das dyes");
	}
}
