package com.lopez.l2j.game.clan.clanhall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.clan.ClanMember;
import com.lopez.l2j.game.clan.ClanTable;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClanHallServiceTest {

	private ClanTable clanTable;
	private ClanHallService clanHallService;
	private ClanHallFunctionService functionService;

	private PlayerCharacter leader;
	private Clan clan;

	private PlayerCharacter dummyPlayer(int id, String name, int level) {
		return new PlayerCharacter(id, "acc", name, level, 0L, 0, 0, 0, 0, false,
				0, 0, 0, 100, 100, 100, 0, 0, 0, 0, "", 0, 0L, 0L, 0, 0, 0, 0, 0.0, 0.0, 0.0);
	}

	private ItemTemplate createItemTemplate(int id, String name) {
		return new ItemTemplate(
				id, id, name, ItemTemplate.Kind.ETC, "none",
				ItemTemplate.TYPE1_ITEM_QUESTITEM_ADENA, ItemTemplate.TYPE2_OTHER,
				0, 10, true, "none", 100,
				0, 0, 0, 0, 0, 0, 0, true, true, true, true
		);
	}

	@BeforeEach
	void setUp() {
		clanTable = new ClanTable(null, ObjectIdFactory.sequential(1000));
		clanHallService = new ClanHallService(clanTable, null);
		functionService = new ClanHallFunctionService(clanHallService, null);

		leader = dummyPlayer(101, "LeaderOne", 75);
		leader.clanId(1);
		clan = new Clan(1, "ClanAlpha", 101, "LeaderOne", 5);
		clan.addMember(new ClanMember(101, "LeaderOne", 75, 88, "", true, 0));
		clanTable.registerClan(clan);
	}

	@Test
	void testClanHallLists() {
		assertFalse(clanHallService.getAllClanHalls().isEmpty());
		assertFalse(clanHallService.getAuctionableClanHalls().isEmpty());
		assertFalse(clanHallService.getSiegeClanHalls().isEmpty());

		var bandit = clanHallService.getClanHall(35); // Bandit Stronghold
		assertTrue(bandit.isPresent());
		assertTrue(bandit.get().isSiegeType());

		var moonstone = clanHallService.getClanHall(22); // Moonstone Hall
		assertTrue(moonstone.isPresent());
		assertFalse(moonstone.get().isSiegeType());
	}

	@Test
	void testAuctionBiddingAndOwnership() {
		int hallId = 22;

		// Not enough adena
		assertEquals(ClanHallService.BidResult.NOT_ENOUGH_ADENA,
				clanHallService.bidOnAuction(leader, hallId, 10_000_000L));

		// Give adena (57)
		leader.inventory().add(new ItemInstance(1002, createItemTemplate(57, "Adena"), leader.objectId(), 15_000_000));

		// Success bid
		assertEquals(ClanHallService.BidResult.SUCCESS,
				clanHallService.bidOnAuction(leader, hallId, 10_000_000L));

		var hall = clanHallService.getClanHall(hallId).orElseThrow();
		assertEquals(1, hall.ownerId());
		assertEquals(hallId, clan.clanHallId());

		// Cannot bid on another hall while owning one
		assertEquals(ClanHallService.BidResult.ALREADY_HAS_CLAN_HALL,
				clanHallService.bidOnAuction(leader, 23, 10_000_000L));

		// Eviction / removeOwner
		assertTrue(clanHallService.removeOwner(hallId));
		assertEquals(0, hall.ownerId());
		assertEquals(0, clan.clanHallId());
	}

	@Test
	void testClanHallFunctionsAndBonuses() {
		int hallId = 22;
		clanHallService.setOwner(hallId, clan.clanId());

		// Install HP regen (+100%) and MP regen (+40%)
		long oneWeek = 7L * 86_400_000L;
		assertTrue(functionService.setFunction(hallId, ClanHallFunctionRecord.FUNC_RESTORE_HP, 100, 100_000, oneWeek));
		assertTrue(functionService.setFunction(hallId, ClanHallFunctionRecord.FUNC_RESTORE_MP, 40, 50_000, oneWeek));
		assertTrue(functionService.setFunction(hallId, ClanHallFunctionRecord.FUNC_RESTORE_EXP, 50, 70_000, oneWeek));

		// Owner player gets bonuses
		assertEquals(2.0, functionService.calculateHpRegenBonus(leader, hallId)); // 1.0 + 100/100 = 2.0x
		assertEquals(1.4, functionService.calculateMpRegenBonus(leader, hallId), 0.001); // 1.0 + 40/100 = 1.4x
		assertEquals(50, functionService.getExpRestorePercent(leader, hallId));

		// Player from another clan gets standard 1.0x
		PlayerCharacter otherPlayer = dummyPlayer(102, "Other", 70);
		otherPlayer.clanId(2);
		assertEquals(1.0, functionService.calculateHpRegenBonus(otherPlayer, hallId));
		assertEquals(1.0, functionService.calculateMpRegenBonus(otherPlayer, hallId));
		assertEquals(0, functionService.getExpRestorePercent(otherPlayer, hallId));

		// Support buff skills
		var buffs = functionService.getAvailableSupportBuffSkills(3);
		assertTrue(buffs.contains(4342)); // Wind walk

		// Remove function
		assertTrue(functionService.removeFunction(hallId, ClanHallFunctionRecord.FUNC_RESTORE_HP));
		assertEquals(1.0, functionService.calculateHpRegenBonus(leader, hallId));
	}
}
