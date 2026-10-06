package com.lopez.l2j.game.castle.crown;

import com.lopez.l2j.game.castle.CastleManager;
import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.clan.ClanMember;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CrownServiceTest {

	private CastleManager castleManager;
	private CrownService crownService;

	private PlayerCharacter dummyPlayer(int id, String name) {
		PlayerCharacter player = new PlayerCharacter(id, "acc", name, 75, 0L, 0, 0, 0, 0, false,
				0, 0, 0, 100, 100, 100, 0, 0, 0, 0, "", 0, 0L, 0L, 0, 0, 0, 0, 0.0, 0.0, 0.0);
		player.inventory(new Inventory(id));
		return player;
	}

	private ItemTemplate dummyTemplate(int itemId, String name) {
		return ItemTemplate.armor(itemId, itemId, name, "hair", "none", 10, "none", 0, 0, 0, false, false, false, false);
	}

	@BeforeEach
	void setUp() {
		castleManager = new CastleManager(null);
		crownService = new CrownService(castleManager, null, ObjectIdFactory.sequential(1000));
	}

	@Test
	void testRewardLordCrownAndCirclet() {
		PlayerCharacter leader = dummyPlayer(100, "CastleLord");
		crownService.rewardLordCrown(leader, CastleManager.ADEN);

		// Deve conter Lord's Crown (6841) e Aden Circlet (6840)
		assertTrue(leader.inventory().byItemId(CrownTable.LORDS_CROWN).isPresent());
		assertTrue(leader.inventory().byItemId(CrownTable.ADEN_CIRCLET).isPresent());
	}

	@Test
	void testCheckCrownsRemovesInvalidCrownFromNonLeader() {
		PlayerCharacter nonLeader = dummyPlayer(101, "Member");
		Clan clan = new Clan(1, "ClanOwner", 999, "RealLeader", 5);
		clan.castleId(CastleManager.GIRAN);
		clan.addMember(new ClanMember(101, "Member", 70, 10, "", true, 0));

		// Jogador possui Lord's Crown indevidamente
		nonLeader.inventory().add(new ItemInstance(1, dummyTemplate(CrownTable.LORDS_CROWN, "Lord's Crown"), nonLeader.objectId(), 1));
		// E possui o diadema correto de Giran
		nonLeader.inventory().add(new ItemInstance(2, dummyTemplate(CrownTable.GIRAN_CIRCLET, "Giran Circlet"), nonLeader.objectId(), 1));

		crownService.checkCrowns(nonLeader, clan);

		// Lord's Crown deve ter sido removido (nao e lider)
		assertTrue(nonLeader.inventory().byItemId(CrownTable.LORDS_CROWN).isEmpty());
		// Diadema do castelo pertencente ao cla deve ser mantido
		assertTrue(nonLeader.inventory().byItemId(CrownTable.GIRAN_CIRCLET).isPresent());
	}

	@Test
	void testRemoveLordCrown() {
		PlayerCharacter leader = dummyPlayer(100, "FormerLord");
		leader.inventory().add(new ItemInstance(1, dummyTemplate(CrownTable.LORDS_CROWN, "Lord's Crown"), leader.objectId(), 1));

		crownService.removeLordCrown(leader);
		assertTrue(leader.inventory().byItemId(CrownTable.LORDS_CROWN).isEmpty());
	}
}
