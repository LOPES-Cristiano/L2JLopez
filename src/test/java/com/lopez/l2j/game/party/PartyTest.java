package com.lopez.l2j.game.party;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PartyTest {

	private List<GameServerPacket> packets1;
	private List<GameServerPacket> packets2;
	private List<GameServerPacket> packets3;

	private GameSession session1;
	private GameSession session2;
	private GameSession session3;

	private PlayerCharacter char1;
	private PlayerCharacter char2;
	private PlayerCharacter char3;

	@BeforeEach
	void setUp() {
		packets1 = new ArrayList<>();
		packets2 = new ArrayList<>();
		packets3 = new ArrayList<>();

		char1 = createPlayer(101, "Leader", 10);
		char2 = createPlayer(102, "Member1", 10);
		char3 = createPlayer(103, "Member2", 20);

		char1.moveTo(0, 0, 0);
		char2.moveTo(100, 100, 0);
		char3.moveTo(3000, 3000, 0); // Longe (> 1500 de distancia)

		var ctx = new GameSession.Context(746, 746, null, null, null, null, "TestServer");
		byte[] key = new byte[8];

		session1 = new GameSession(ctx, key, "127.0.0.1", packets1::add);
		session2 = new GameSession(ctx, key, "127.0.0.1", packets2::add);
		session3 = new GameSession(ctx, key, "127.0.0.1", packets3::add);

		// Vincula personagens via reflection ou estado interno basico
		setField(session1, "active", char1);
		setField(session1, "inWorld", true);

		setField(session2, "active", char2);
		setField(session2, "inWorld", true);

		setField(session3, "active", char3);
		setField(session3, "inWorld", true);
	}

	private static void setField(Object target, String name, Object val) {
		try {
			var f = target.getClass().getDeclaredField(name);
			f.setAccessible(true);
			f.set(target, val);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void createPartyAndAddMember() {
		Party party = new Party(session1, session2, Party.ITEM_LOOTER);
		session1.party(party);
		session2.party(party);

		assertEquals(2, party.size());
		assertTrue(party.isLeader(char1.objectId()));
		assertFalse(party.isLeader(char2.objectId()));
		assertTrue(party.contains(char1.objectId()));
		assertTrue(party.contains(char2.objectId()));

		boolean added = party.addMember(session3);
		assertTrue(added);
		assertEquals(3, party.size());
		assertTrue(party.contains(char3.objectId()));
	}

	private static PlayerCharacter createPlayer(int objId, String name, int level) {
		var p = new PlayerCharacter(objId, name, "Title", 1, 0, 0, 0, 0, 0, false, 0, 0, 0,
				100, 50, 50, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100.0, 50.0, 50.0);
		p.level(level);
		return p;
	}

	@Test
	void partySizeLimit() {
		Party party = new Party(session1, session2, Party.ITEM_LOOTER);
		for (int i = 3; i <= 9; i++) {
			var charN = createPlayer(200 + i, "Player" + i, 10);
			var ctx = new GameSession.Context(746, 746, null, null, null, null, "TestServer");
			var sessN = new GameSession(ctx, new byte[8], "127.0.0.1", p -> {});
			setField(sessN, "active", charN);
			setField(sessN, "inWorld", true);
			assertTrue(party.addMember(sessN));
		}
		assertEquals(9, party.size());
		assertTrue(party.isFull());

		// 10º membro deve ser recusado
		var extraChar = createPlayer(210, "Extra", 10);
		var extraSess = new GameSession(new GameSession.Context(746, 746, null, null, null, null, "TestServer"), new byte[8], "127.0.0.1", p -> {});
		setField(extraSess, "active", extraChar);
		setField(extraSess, "inWorld", true);
		assertFalse(party.addMember(extraSess));
	}

	@Test
	void removeMemberDisbandsWhenLessThanTwo() {
		Party party = new Party(session1, session2, Party.ITEM_LOOTER);
		session1.party(party);
		session2.party(party);

		party.removeMember(session2);

		assertNull(session1.party());
		assertEquals(0, party.size());
	}

	@Test
	void distributeExpAndSpWithPartyBonus() {
		Party party = new Party(session1, session2, Party.ITEM_LOOTER);
		session1.party(party);
		session2.party(party);

		long initialExp1 = char1.exp();
		long initialExp2 = char2.exp();

		// Total XP: 1000, Total SP: 100. Com 2 membros: bonus 1.30 -> totalExp = 1300.
		// Niveis iguais (10 e 10): 50% para cada -> 650 XP e 65 SP cada.
		party.distributeExpAndSp(1000, 100, char1, 1.0, 1.0);

		assertEquals(initialExp1 + 650, char1.exp());
		assertEquals(initialExp2 + 650, char2.exp());
		assertEquals(65, char1.sp());
		assertEquals(65, char2.sp());
	}

	@Test
	void outOfRangeMemberDoesNotReceiveExp() {
		Party party = new Party(session1, session2, Party.ITEM_LOOTER);
		session1.party(party);
		session2.party(party);
		party.addMember(session3); // session3 esta em (3000, 3000), longe do killer char1 (0, 0)
		session3.party(party);

		long initialExp3 = char3.exp();
		party.distributeExpAndSp(1000, 100, char1, 1.0, 1.0);

		// session3 nao deve ter recebido EXP porque esta fora do raio de 1500
		assertEquals(initialExp3, char3.exp());
	}
}
