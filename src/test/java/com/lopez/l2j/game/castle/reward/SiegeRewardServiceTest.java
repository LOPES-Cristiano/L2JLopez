package com.lopez.l2j.game.castle.reward;

import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.clan.ClanMember;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SiegeRewardServiceTest {

	private SiegeRewardService siegeRewardService;

	private PlayerCharacter dummyPlayer(int id, String name) {
		PlayerCharacter player = new PlayerCharacter(id, "acc", name, 75, 0L, 0, 0, 0, 0, false,
				0, 0, 0, 100, 100, 100, 0, 0, 0, 0, "", 0, 0L, 0L, 0, 0, 0, 0, 0.0, 0.0, 0.0);
		player.inventory(new Inventory(id));
		return player;
	}

	@BeforeEach
	void setUp() {
		siegeRewardService = new SiegeRewardService(null, ObjectIdFactory.sequential(1000), null);
	}

	@Test
	void testRewardOnlineLeaderAndMember() {
		PlayerCharacter leader = dummyPlayer(100, "WinnerLeader");
		PlayerCharacter member = dummyPlayer(101, "WinnerMember");

		Clan winnerClan = new Clan(1, "WinnerClan", 100, "WinnerLeader", 5);
		winnerClan.addMember(new ClanMember(100, "WinnerLeader", 75, 10, "", true, 0));
		winnerClan.addMember(new ClanMember(101, "WinnerMember", 70, 10, "", true, 0));

		Map<Integer, PlayerCharacter> online = Map.of(100, leader, 101, member);
		siegeRewardService.rewardWinningClan(winnerClan, "Aden", online);

		// Lorde online deve ter Blood Alliance (9911)
		assertTrue(leader.inventory().byItemId(SiegeRewardService.BLOOD_ALLIANCE).isPresent());
		// Membro comum online nao deve ter Blood Alliance (regra leaderOnly)
		assertTrue(member.inventory().byItemId(SiegeRewardService.BLOOD_ALLIANCE).isEmpty());
		// Ambos devem ter Knight's Epaulettes (9912) e Adena (57)
		assertTrue(leader.inventory().byItemId(SiegeRewardService.KNIGHT_EPAULETTE).isPresent());
		assertTrue(member.inventory().byItemId(SiegeRewardService.KNIGHT_EPAULETTE).isPresent());
	}

	@Test
	void testRewardOfflineMemberAndClaimOnLogin() {
		Clan winnerClan = new Clan(1, "WinnerClan", 100, "WinnerLeader", 5);
		winnerClan.addMember(new ClanMember(100, "WinnerLeader", 75, 10, "", true, 0));
		winnerClan.addMember(new ClanMember(200, "OfflineGuy", 65, 10, "", false, 0));

		// Apenas lider esta online
		PlayerCharacter leader = dummyPlayer(100, "WinnerLeader");
		Map<Integer, PlayerCharacter> online = Map.of(100, leader);

		siegeRewardService.rewardWinningClan(winnerClan, "Giran", online);

		// Jogador offline 200 possui recompensas pendentes
		var pending = siegeRewardService.getPendingRewards(200);
		assertFalse(pending.isEmpty());

		// Quando jogador 200 conecta no mundo
		PlayerCharacter returningPlayer = dummyPlayer(200, "OfflineGuy");
		int claimed = siegeRewardService.claimRewards(returningPlayer);
		assertTrue(claimed > 0);
		assertTrue(returningPlayer.inventory().byItemId(SiegeRewardService.KNIGHT_EPAULETTE).isPresent());
		assertTrue(siegeRewardService.getPendingRewards(200).isEmpty());
	}
}
