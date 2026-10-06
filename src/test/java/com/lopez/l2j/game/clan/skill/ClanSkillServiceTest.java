package com.lopez.l2j.game.clan.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.clan.ClanMember;
import com.lopez.l2j.game.clan.ClanTable;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClanSkillServiceTest {

	private ClanTable clanTable;
	private ClanSkillTreeTable skillTreeTable;
	private ClanSkillService skillService;

	private PlayerCharacter leader;
	private Clan clan;

	private PlayerCharacter dummyPlayer(int id, String name, int level) {
		return new PlayerCharacter(id, "acc", name, level, 0L, 0, 0, 0, 0, false,
				0, 0, 0, 100, 100, 100, 0, 0, 0, 0, "", 0, 0L, 0L, 0, 0, 0, 0, 0.0, 0.0, 0.0);
	}

	@BeforeEach
	void setUp() {
		clanTable = new ClanTable(null, ObjectIdFactory.sequential(1000));
		skillTreeTable = new ClanSkillTreeTable();
		skillService = new ClanSkillService(clanTable, skillTreeTable, null);

		leader = dummyPlayer(101, "LeaderOne", 75);
		leader.clanId(1);
		clan = new Clan(1, "ClanAlpha", 101, "LeaderOne", 5);
		clan.reputationScore(5000);
		clan.addMember(new ClanMember(101, "LeaderOne", 75, 88, "", true, 0));
		clanTable.registerClan(clan);
	}

	@Test
	void testSkillTreeLoadedProperly() {
		assertFalse(skillTreeTable.allSkills().isEmpty());
		var vitOpt = skillTreeTable.getSkill(370, 1);
		assertTrue(vitOpt.isPresent());
		assertEquals("Clan Vitality", vitOpt.get().name());
		assertEquals(5, vitOpt.get().minClanLevel());
		assertEquals(500, vitOpt.get().repCost());
		assertEquals(8166, vitOpt.get().itemId());
	}

	@Test
	void testGetAvailableSkills() {
		var available = skillService.getAvailableSkills(clan);
		assertFalse(available.isEmpty());
		// Clan Vitality level 1 should be available
		assertTrue(available.stream().anyMatch(s -> s.skillId() == 370 && s.level() == 1));
	}

	private com.lopez.l2j.game.item.ItemTemplate createItemTemplate(int id, String name) {
		return new com.lopez.l2j.game.item.ItemTemplate(
				id, id, name, com.lopez.l2j.game.item.ItemTemplate.Kind.ETC, "none",
				com.lopez.l2j.game.item.ItemTemplate.TYPE1_ITEM_QUESTITEM_ADENA, com.lopez.l2j.game.item.ItemTemplate.TYPE2_OTHER,
				0, 10, true, "none", 100,
				0, 0, 0, 0, 0, 0, 0, true, true, true, true
		);
	}

	@Test
	void testLearnSkillValidationAndSuccess() {
		int skillId = 370; // Clan Vitality

		// Missing required item 8166
		assertEquals(ClanSkillService.LearnResult.MISSING_REQUIRED_ITEM,
				skillService.learnSkill(leader, skillId));

		// Give item 8166
		var itemTpl = createItemTemplate(8166, "Destruction Tombstone");
		leader.inventory().add(new ItemInstance(1001, itemTpl, leader.objectId(), 1));

		// Not enough reputation
		clan.reputationScore(100);
		assertEquals(ClanSkillService.LearnResult.NOT_ENOUGH_REPUTATION,
				skillService.learnSkill(leader, skillId));

		// Restore reputation & succeed
		clan.reputationScore(1000);
		assertEquals(ClanSkillService.LearnResult.SUCCESS,
				skillService.learnSkill(leader, skillId));

		assertEquals(1, clan.getSkillLevel(skillId));
		assertEquals(500, clan.reputationScore()); // 1000 - 500 = 500
		assertEquals(0, leader.inventory().getItemCount(8166)); // 1 consumed
	}

	@Test
	void testMemberEligibility() {
		ClanMember mainMember = new ClanMember(201, "MainKnight", 70, 1, "", true, 0);
		ClanMember academyMember = new ClanMember(202, "AcademyStudent", 25, 1, "", true, Clan.SUBUNIT_ACADEMY);

		assertTrue(skillService.isMemberEligibleForClanSkills(mainMember));
		assertFalse(skillService.isMemberEligibleForClanSkills(academyMember));
	}
}
