package com.lopez.l2j.game.quest;

import static org.junit.jupiter.api.Assertions.*;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.quest.impl.*;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class SecondClassCompleteQuestsTest {

	private QuestManager questManager;
	private List<GameServerPacket> sentPackets;
	private GameSession session;
	private PlayerCharacter playerChar;

	@BeforeEach
	void setUp() {
		sentPackets = new ArrayList<>();
		questManager = new QuestManager(null);

		playerChar = new PlayerCharacter(2001, "SecondClassHero", "Title", 0, 0, 0, 0, 0, 0, false, 0, 0, 0,
				100, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100.0, 100.0, 100.0);
		playerChar.inventory(new Inventory(playerChar.objectId()));
		playerChar.level(40);
		playerChar.classId(1); // Warrior / Fighter

		var mockCtx = org.mockito.Mockito.mock(com.lopez.l2j.network.game.GameSession.Context.class);
		var mockInvSvc = org.mockito.Mockito.mock(InventoryService.class);

		org.mockito.Mockito.when(mockCtx.inventories()).thenReturn(mockInvSvc);
		org.mockito.Mockito.when(mockCtx.questManager()).thenReturn(questManager);

		session = new GameSession(mockCtx, new byte[16], "127.0.0.1", sentPackets::add);
		setField(session, "state", com.lopez.l2j.network.game.packet.GameClientPacket.State.IN_GAME);
		setField(session, "active", playerChar);
		setField(session, "inWorld", true);
	}

	@Test
	@DisplayName("Validação dos Trials: 213, 214, 215, 216")
	void testTrials() {
		// Quest 213 Seeker
		playerChar.classId(7); // Rogue
		Quest213TrialOfSeeker q213 = new Quest213TrialOfSeeker(questManager);
		QuestState qs213 = q213.newQuestState(session);
		q213.onAdvEvent("30106-05.htm", null, session);
		assertTrue(qs213.isStarted());
		assertTrue(qs213.hasQuestItems(Quest213TrialOfSeeker.DUFNERS_LETTER));

		// Quest 214 Scholar
		playerChar.classId(11); // Wizard
		Quest214TrialOfScholar q214 = new Quest214TrialOfScholar(questManager);
		QuestState qs214 = q214.newQuestState(session);
		q214.onAdvEvent("1", null, session);
		assertTrue(qs214.isStarted());
		assertTrue(qs214.hasQuestItems(Quest214TrialOfScholar.MIRIENS_SIGIL1));

		// Quest 215 Pilgrim
		playerChar.classId(15); // Cleric
		Quest215TrialOfPilgrim q215 = new Quest215TrialOfPilgrim(questManager);
		QuestState qs215 = q215.newQuestState(session);
		q215.onAdvEvent("1", null, session);
		assertTrue(qs215.isStarted());
		assertTrue(qs215.hasQuestItems(Quest215TrialOfPilgrim.VOUCHER_OF_TRIAL));

		// Quest 216 Guildsman
		playerChar.classId(54); // Scavenger
		Quest216TrialOfGuildsman q216 = new Quest216TrialOfGuildsman(questManager);
		QuestState qs216 = q216.newQuestState(session);
		q216.onAdvEvent("1", null, session);
		assertTrue(qs216.isStarted());
		assertTrue(qs216.hasQuestItems(Quest216TrialOfGuildsman.VALKONS_RECOMMEND));
	}

	@Test
	@DisplayName("Validação dos Testimonies: 218, 219, 220, 221")
	void testTestimonies() {
		// 218 Life (Elf)
		playerChar.classId(19);
		Quest218TestimonyOfLife q218 = new Quest218TestimonyOfLife(questManager);
		QuestState qs218 = q218.newQuestState(session);
		q218.onAdvEvent("1", null, session);
		assertTrue(qs218.isStarted());
		assertTrue(qs218.hasQuestItems(Quest218TestimonyOfLife.CARDIENS_LETTER));

		// 219 Fate (Dark Elf)
		playerChar.classId(32);
		Quest219TestimonyOfFate q219 = new Quest219TestimonyOfFate(questManager);
		QuestState qs219 = q219.newQuestState(session);
		q219.onAdvEvent("1", null, session);
		assertTrue(qs219.isStarted());
		assertTrue(qs219.hasQuestItems(Quest219TestimonyOfFate.KAIRAS_LETTER1));

		// 220 Glory (Orc)
		playerChar.classId(45);
		Quest220TestimonyOfGlory q220 = new Quest220TestimonyOfGlory(questManager);
		QuestState qs220 = q220.newQuestState(session);
		q220.onAdvEvent("30514-05.htm", null, session);
		assertTrue(qs220.isStarted());
		assertTrue(qs220.hasQuestItems(Quest220TestimonyOfGlory.VOKIYANS_ORDER1));

		// 221 Prosperity (Dwarf)
		playerChar.classId(54);
		Quest221TestimonyOfProsperity q221 = new Quest221TestimonyOfProsperity(questManager);
		QuestState qs221 = q221.newQuestState(session);
		q221.onAdvEvent("1", null, session);
		assertTrue(qs221.isStarted());
		assertTrue(qs221.hasQuestItems(Quest221TestimonyOfProsperity.RING_OF_TESTIMONY1));
	}

	@Test
	@DisplayName("Validação dos Tests: 222 Duelist, 223 Champion, 224 Sagittarius, 225 Searcher, 228 Magus, 230 Summoner")
	void testTests() {
		// 222 Duelist
		playerChar.classId(1);
		Quest222TestOfDuelist q222 = new Quest222TestOfDuelist(questManager);
		QuestState qs222 = q222.newQuestState(session);
		q222.onAdvEvent("30623-07.htm", null, session);
		assertTrue(qs222.isStarted());

		// 223 Champion
		Quest223TestOfChampion q223 = new Quest223TestOfChampion(questManager);
		QuestState qs223 = q223.newQuestState(session);
		q223.onAdvEvent("1", null, session);
		assertTrue(qs223.isStarted());

		// 224 Sagittarius
		playerChar.classId(7);
		Quest224TestOfSagittarius q224 = new Quest224TestOfSagittarius(questManager);
		QuestState qs224 = q224.newQuestState(session);
		q224.onAdvEvent("1", null, session);
		assertTrue(qs224.isStarted());

		// 225 Searcher
		Quest225TestOfSearcher q225 = new Quest225TestOfSearcher(questManager);
		QuestState qs225 = q225.newQuestState(session);
		q225.onAdvEvent("30690-05.htm", null, session);
		assertTrue(qs225.isStarted());

		// 228 Magus
		playerChar.classId(11);
		Quest228TestOfMagus q228 = new Quest228TestOfMagus(questManager);
		QuestState qs228 = q228.newQuestState(session);
		q228.onAdvEvent("1", null, session);
		assertTrue(qs228.isStarted());

		// 230 Summoner
		Quest230TestOfSummoner q230 = new Quest230TestOfSummoner(questManager);
		QuestState qs230 = q230.newQuestState(session);
		q230.onAdvEvent("1", null, session);
		assertTrue(qs230.isStarted());
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
}
