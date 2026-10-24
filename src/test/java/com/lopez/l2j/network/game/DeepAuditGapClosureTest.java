package com.lopez.l2j.network.game;

import static org.junit.jupiter.api.Assertions.*;

import com.lopez.l2j.game.clan.ClanTable;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemSlots;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.PlayerStats;
import com.lopez.l2j.game.skill.StatFunc;
import com.lopez.l2j.game.template.CharTemplate;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameClientPacket.State;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.login.packet.PacketWriter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DeepAuditGapClosureTest {

	@Test
	void testClientPacketOpcodesAndNewPacketsDecoding() {
		// 0x3c RequestPledgeMemberList
		byte[] p3c = new PacketWriter().writeC(0x3c).toByteArray();
		assertInstanceOf(GameClientPacket.RequestPledgeMemberList.class, GameClientPacket.decode(State.IN_GAME, p3c).orElseThrow());

		// 0x52 RequestReplySurrenderPledgeWar
		byte[] p52 = new PacketWriter().writeC(0x52).writeS("EnemyClan").writeD(1).toByteArray();
		var surrender = assertInstanceOf(GameClientPacket.RequestReplySurrenderPledgeWar.class, GameClientPacket.decode(State.IN_GAME, p52).orElseThrow());
		assertEquals("EnemyClan", surrender.reqName());
		assertEquals(1, surrender.answer());

		// 0x53 RequestSetPledgeCrest
		byte[] crestBytes = new byte[] { 1, 2, 3, 4 };
		byte[] p53 = new PacketWriter().writeC(0x53).writeD(crestBytes.length).writeB(crestBytes).toByteArray();
		var setCrest = assertInstanceOf(GameClientPacket.RequestSetPledgeCrest.class, GameClientPacket.decode(State.IN_GAME, p53).orElseThrow());
		assertArrayEquals(crestBytes, setCrest.data());

		// 0x24 RequestJoinPledge
		byte[] p24 = new PacketWriter().writeC(0x24).writeD(100200).writeD(0).toByteArray();
		var joinPledge = assertInstanceOf(GameClientPacket.RequestJoinPledge.class, GameClientPacket.decode(State.IN_GAME, p24).orElseThrow());
		assertEquals(100200, joinPledge.targetId());

		// 0x64 RequestQuestAbort
		byte[] p64 = new PacketWriter().writeC(0x64).writeD(255).toByteArray();
		var abort = assertInstanceOf(GameClientPacket.RequestQuestAbort.class, GameClientPacket.decode(State.IN_GAME, p64).orElseThrow());
		assertEquals(255, abort.questId());

		// 0x72 RequestCrystallizeItem
		byte[] p72 = new PacketWriter().writeC(0x72).writeD(555).writeD(1).toByteArray();
		var cryst = assertInstanceOf(GameClientPacket.RequestCrystallizeItem.class, GameClientPacket.decode(State.IN_GAME, p72).orElseThrow());
		assertEquals(555, cryst.objectId());
		assertEquals(1, cryst.count());

		// 0xc5 DlgAnswer
		byte[] pc5 = new PacketWriter().writeC(0xc5).writeD(1510).writeD(1).writeD(999).toByteArray();
		var dlg = assertInstanceOf(GameClientPacket.DlgAnswer.class, GameClientPacket.decode(State.IN_GAME, pc5).orElseThrow());
		assertEquals(1510, dlg.messageId());
		assertEquals(1, dlg.answer());

		// 0xd0:0x22 RequestCursedWeaponList & 0xd0:0x23 RequestCursedWeaponLocation
		byte[] cwList = new PacketWriter().writeC(0xd0).writeH(0x22).toByteArray();
		assertInstanceOf(GameClientPacket.RequestCursedWeaponList.class, GameClientPacket.decode(State.IN_GAME, cwList).orElseThrow());
		byte[] cwLoc = new PacketWriter().writeC(0xd0).writeH(0x23).toByteArray();
		assertInstanceOf(GameClientPacket.RequestCursedWeaponLocation.class, GameClientPacket.decode(State.IN_GAME, cwLoc).orElseThrow());

		// 0xd0:0x27 RequestDuelStart
		byte[] duelStart = new PacketWriter().writeC(0xd0).writeH(0x27).writeS("Rival").writeD(0).toByteArray();
		var ds = assertInstanceOf(GameClientPacket.RequestDuelStart.class, GameClientPacket.decode(State.IN_GAME, duelStart).orElseThrow());
		assertEquals("Rival", ds.player());
		assertEquals(0, ds.partyDuel());
	}

	@Test
	void testXmlItemStatFuncsAppliedInPlayerStats() {
		PlayerCharacter pc = new PlayerCharacter(1, "Tester", 76, 83400, 147900, -3400);
		CharTemplate tpl = new CharTemplate(
				88, "Duelist", 0,
				40, 43, 30, 21, 20, 25,
				4, 80, 6, 41, 300, 333,
				33, 44, 33, 115, 69000,
				83400, 147900, -3400, false,
				9.0, 23.0, 8.0, 23.5,
				2000.0, 1000.0, 1500.0, 4
		);

		PlayerStats baseStats = PlayerStats.calculate(pc, tpl);
		double baseMaxMp = PlayerStats.applyStat(pc, "maxMp", 1000.0);

		ItemTemplate ringWithXmlFuncs = new ItemTemplate(
				6660, 6660, "Ring of Queen Ant", ItemTemplate.Kind.ARMOR, "ring",
				0, 2, ItemSlots.SLOT_R_FINGER, 150,
				false, "s", 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, true, true, true, true
		).withStatFuncs(List.of(
				new StatFunc("mDef", StatFunc.Op.ADD, 0x10, 48.0),
				new StatFunc("maxMp", StatFunc.Op.ADD, 0x40, 250.0),
				new StatFunc("pAtkSpd", StatFunc.Op.MUL, 0x30, 1.15)
		));
		ItemInstance item = new ItemInstance(101, ringWithXmlFuncs, pc.objectId(), 1);
		pc.inventory().add(item);
		pc.inventory().equip(item);

		PlayerStats boostedStats = PlayerStats.calculate(pc, tpl);
		double boostedMaxMp = PlayerStats.applyStat(pc, "maxMp", 1000.0);
		assertTrue(boostedStats.mDef() > baseStats.mDef(), "XML StatFunc mDef deve aumentar a M.Def do jogador");
		assertTrue(boostedStats.pAtkSpd() > baseStats.pAtkSpd(), "XML StatFunc pAtkSpd deve aumentar a Atk.Spd do jogador");
		assertEquals(baseMaxMp + 250.0, boostedMaxMp, 0.01, "XML StatFunc maxMp deve somar +250 MP ao jogador");
	}

	@Test
	void testServerPacketsEncoding() {
		PlayerCharacter pc = new PlayerCharacter(77, "NobleHero", 80, 83400, 147900, -3400);
		pc.setNoble(true);
		pc.setHero(true);
		pc.clanId(500);
		pc.clanPrivileges(ClanTable.getInstance() != null ? 0xFF : 0x7F);
		pc.pledgeRank(6);

		var die = GameServerPacket.Die.forPlayer(pc);
		assertNotNull(die.encode());

		var ride = new GameServerPacket.Ride(pc.objectId(), 1, 2, 12621);
		assertTrue(ride.encode().length > 0);

		var calc = new GameServerPacket.ShowCalc(4393);
		assertEquals(0xdc, calc.encode()[0] & 0xFF);

		var dice = new GameServerPacket.Dice(pc.objectId(), 4625, 6, pc.x(), pc.y(), pc.z());
		assertEquals(0xd4, dice.encode()[0] & 0xFF);

		var grades = new GameServerPacket.PledgePowerGradeList(Map.of(1, 3, 2, 5));
		assertEquals(0xfe, grades.encode()[0] & 0xFF);
	}
}
