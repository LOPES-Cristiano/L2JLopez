package com.lopez.l2j.game.clan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClanTableTest {

	private ClanTable clanTable;

	@BeforeEach
	void setUp() {
		clanTable = new ClanTable(null, ObjectIdFactory.sequential(1000));
	}

	private PlayerCharacter createPlayer(int objId, String name, int level, int clanId) {
		return new PlayerCharacter(objId, "acc", name, level, 0L, 0, 0, 0, 0, false, 0, 0, 0,
				1000, 1000, 1000, 0, 0, 0, clanId, "Title", 0, 0L, 0L, 0, 0, 0, 0, 1000.0, 1000.0, 1000.0);
	}

	@Test
	void rejectsClanCreationWhenLevelTooLow() {
		PlayerCharacter newbie = createPlayer(1, "Newbie", 9, 0);
		Clan clan = clanTable.createClan(newbie, "Dragons");
		assertNull(clan, "Nao deve permitir criar cla abaixo do nivel 10");
	}

	@Test
	void rejectsClanCreationWhenAlreadyInClan() {
		PlayerCharacter member = createPlayer(2, "Veteran", 40, 999);
		Clan clan = clanTable.createClan(member, "Warriors");
		assertNull(clan, "Nao deve permitir criar cla se ja pertencer a outro");
	}

	@Test
	void rejectsInvalidClanName() {
		PlayerCharacter leader = createPlayer(3, "Leader", 40, 0);
		assertNull(clanTable.createClan(leader, "AB"), "Nome muito curto (<3)");
		assertNull(clanTable.createClan(leader, "ThisNameIsWayTooLongForAClan"), "Nome muito longo (>16)");
		assertNull(clanTable.createClan(leader, "Clan!@#$"), "Caracteres especiais invalidos");
	}

	@Test
	void createsClanAndEnrollsLeader() {
		PlayerCharacter leader = createPlayer(10, "LeaderOne", 25, 0);
		Clan clan = clanTable.createClan(leader, "Valhalla");

		assertNotNull(clan);
		assertEquals("Valhalla", clan.name());
		assertEquals(10, clan.leaderId());
		assertEquals("LeaderOne", clan.leaderName());
		assertEquals(0, clan.level());
		assertEquals(1, clan.membersCount());
		assertEquals(clan.clanId(), leader.clanId());

		// Verifica busca por id e por nome
		assertTrue(clanTable.byClanId(clan.clanId()).isPresent());
		assertTrue(clanTable.byName("valhalla").isPresent());
		assertTrue(clanTable.byName("VALHALLA").isPresent());
	}

	@Test
	void rejectsDuplicateClanName() {
		PlayerCharacter leader1 = createPlayer(10, "LeaderOne", 25, 0);
		PlayerCharacter leader2 = createPlayer(20, "LeaderTwo", 30, 0);

		assertNotNull(clanTable.createClan(leader1, "Spartans"));
		assertNull(clanTable.createClan(leader2, "Spartans"), "Nao deve permitir nomes duplicados");
		assertNull(clanTable.createClan(leader2, "spartans"), "Case-insensitive duplicado");
	}

	@Test
	void dissolvesClan() {
		PlayerCharacter leader = createPlayer(10, "LeaderOne", 25, 0);
		Clan clan = clanTable.createClan(leader, "TempClan");
		assertNotNull(clan);

		assertTrue(clanTable.dissolveClan(clan.clanId()));
		assertFalse(clanTable.byClanId(clan.clanId()).isPresent());
		assertFalse(clanTable.byName("TempClan").isPresent());
	}

	@Test
	void levelsUpClanWhenRequirementsMet() {
		PlayerCharacter leader = createPlayer(10, "LeaderOne", 40, 0);
		Clan clan = clanTable.createClan(leader, "LeveledClan");
		assertNotNull(clan);
		assertEquals(0, clan.level());

		var inv = new com.lopez.l2j.game.item.Inventory(leader.objectId());
		leader.inventory(inv);

		// Sem SP nem Adena deve falhar
		assertFalse(clanTable.levelUpClan(leader));
		assertEquals(0, clan.level());

		// Adiciona SP insuficiente
		leader.sp(10000);
		assertFalse(clanTable.levelUpClan(leader));

		// Adiciona SP suficiente (20000) mas sem Adena
		leader.sp(30000);
		assertFalse(clanTable.levelUpClan(leader));

		// Adiciona Adena (item 57, 650000 count)
		var adenaTpl = new com.lopez.l2j.game.item.ItemTemplate(57, 57, "Adena", com.lopez.l2j.game.item.ItemTemplate.Kind.ETC,
				"none", com.lopez.l2j.game.item.ItemTemplate.TYPE1_ITEM_QUESTITEM_ADENA,
				com.lopez.l2j.game.item.ItemTemplate.TYPE2_MONEY, 0, 0, true, "none", 1, 0, 0, 0, 0, 0, 0, 0, true, true, true, true);
		inv.add(new com.lopez.l2j.game.item.ItemInstance(1001, adenaTpl, leader.objectId(), 700000));

		// Agora deve subir para o nivel 1
		assertTrue(clanTable.levelUpClan(leader));
		assertEquals(1, clan.level());
		assertEquals(10000, leader.sp()); // 30000 - 20000
		assertEquals(50000, inv.getItemCount(57)); // 700000 - 650000
	}
}
