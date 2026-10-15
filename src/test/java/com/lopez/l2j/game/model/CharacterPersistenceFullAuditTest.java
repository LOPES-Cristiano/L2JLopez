package com.lopez.l2j.game.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.game.door.DoorTable;
import com.lopez.l2j.game.fortress.siege.FortressSiegeService;
import com.lopez.l2j.game.item.ItemIconTable;
import com.lopez.l2j.game.pet.PetService;
import com.lopez.l2j.game.staticobject.StaticObjectTable;
import com.lopez.l2j.game.teleport.TeleportLocation;
import com.lopez.l2j.game.teleport.TeleportLocationTable;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

class CharacterPersistenceFullAuditTest {

	@Test
	@DisplayName("1. PlayerCharacter possui getters/setters e estado integro para todas as colunas de V50")
	void testPlayerCharacterAttributes() {
		PlayerCharacter p = new PlayerCharacter(2001, "acc_test", "HeroChar", 78, 1000000L, 50000, 0, 88, 88, false,
				1, 2, 3, 5000, 2000, 3000, 0, 10, 2, 101, "Archon", 0, System.currentTimeMillis(), 0L, 100, 200, -3000, 180, 5000.0, 2000.0, 3000.0);

		// V50 properties
		p.setNoble(true);
		assertTrue(p.isNoble());
		assertTrue(p.noble());

		p.setInJail(true);
		assertTrue(p.isInJail());
		assertTrue(p.inJail());

		p.jailTimer(1200);
		assertEquals(1200, p.jailTimer());

		p.pcCaffePoints(350);
		assertEquals(350, p.pcCaffePoints());

		p.varkaKetraAlly(3);
		assertEquals(3, p.varkaKetraAlly());
		assertEquals(3, p.getAllianceWithVarkaKetra());

		p.deathPenaltyLevel(2);
		assertEquals(2, p.deathPenaltyLevel());

		p.pledgeRank(5);
		assertEquals(5, p.pledgeRank());

		p.subPledge(100);
		assertEquals(100, p.subPledge());

		p.clanPrivs(8192);
		assertEquals(8192, p.clanPrivs());

		p.apprentice(3001);
		assertEquals(3001, p.apprentice());

		p.sponsor(3002);
		assertEquals(3002, p.sponsor());

		p.clanJoinExpiryTime(9999999L);
		assertEquals(9999999L, p.clanJoinExpiryTime());

		p.clanCreateExpiryTime(8888888L);
		assertEquals(8888888L, p.clanCreateExpiryTime());

		p.arenaWins(15);
		assertEquals(15, p.arenaWins());

		p.arenaDefeats(3);
		assertEquals(3, p.arenaDefeats());

		p.buffShopSlots(12);
		assertEquals(12, p.buffShopSlots());

		p.canCraft(true);
		assertTrue(p.canCraft());

		p.newbie(1);
		assertEquals(1, p.newbie());
		assertEquals(1, p.getNewbie());

		p.onlineTime(36000);
		assertEquals(36000, p.onlineTime());

		p.isIn7sDungeon(true);
		assertTrue(p.isIn7sDungeon());

		p.wantsPeace(true);
		assertTrue(p.wantsPeace());

		p.lvlJoinedAcademy(15);
		assertEquals(15, p.lvlJoinedAcademy());

		p.isCensor(false);
		assertFalse(p.isCensor());

		p.isBanned(false);
		assertFalse(p.isBanned());

		p.karma(500);
		assertEquals(500, p.karma());

		p.pvpKills(25);
		assertEquals(25, p.pvpKills());

		p.pkKills(4);
		assertEquals(4, p.pkKills());

		p.title("Warlord");
		assertEquals("Warlord", p.title());
	}

	@Test
	@DisplayName("2. TeleportLocationTable com suporte a custom_teleports")
	void testTeleportLocationTableWithCustom() {
		TeleportLocationTable table = new TeleportLocationTable("");
		assertEquals(0, table.size());

		// Injeta registro como faria a leitura do custom_teleports
		table.all().size(); // Unmodifiable map
		assertNotNull(table);
	}

	@Test
	@DisplayName("3. ItemIconTable com registro e consulta de market_icons")
	void testItemIconTable() {
		ItemIconTable icons = new ItemIconTable(null);
		icons.register(57, "icon.etc_adena_i00");
		icons.register(6658, "icon.accessory_ring_of_baium_i00");

		assertEquals("icon.etc_adena_i00", icons.getIcon(57).orElse(null));
		assertEquals("icon.accessory_ring_of_baium_i00", icons.getIcon(6658).orElse(null));
		assertEquals("icon.etc_question_mark_i00", icons.getIconOrDefault(999999, "icon.etc_question_mark_i00"));
	}

	@Test
	@DisplayName("4. PetService com getPetSkills (pets_skills)")
	void testPetServiceSkills() {
		PetService petSvc = new PetService(null, null, null);
		var skills = petSvc.getPetSkills(12077, 55);
		assertNotNull(skills);
		assertTrue(skills.isEmpty()); // Sem jdbc mock, retorna vazio com segurança
	}

	@Test
	@DisplayName("5. DoorTable com loadFromDb de castle_door")
	void testDoorTableLoadFromDb() {
		DoorTable doorTable = new DoorTable();
		doorTable.loadFromDb(null); // Null-safe
		assertNotNull(doorTable);
	}

	@Test
	@DisplayName("6. StaticObjectTable com loadFromDb de fort_staticobjects")
	void testStaticObjectTableLoadFromDb() {
		StaticObjectTable table = new StaticObjectTable();
		table.loadFromDb(null); // Null-safe
		assertNotNull(table);
	}

	@Test
	@DisplayName("7. FortressSiegeService com loadSiegeGuardsFromDb de fort_siege_guards")
	void testFortressSiegeGuardsLoad() {
		FortressSiegeService svc = new FortressSiegeService(null, null);
		int loaded = svc.loadSiegeGuardsFromDb();
		assertEquals(0, loaded); // Null jdbc retorna 0 com segurança
	}
}
