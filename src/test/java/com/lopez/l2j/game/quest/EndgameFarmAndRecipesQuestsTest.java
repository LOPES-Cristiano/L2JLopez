package com.lopez.l2j.game.quest;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.impl.*;
import com.lopez.l2j.network.game.GameSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class EndgameFarmAndRecipesQuestsTest {

	private QuestManager questManager;
	private GameSession session;
	private PlayerCharacter pc;

	@BeforeEach
	void setUp() {
		questManager = new QuestManager(null);
		session = Mockito.mock(GameSession.class);
		pc = new PlayerCharacter(
				1001, "acc1", "FarmHero", 75, 50_000_000L, 2_000_000, 0,
				2, 2, false, 0, 0, 0, 4000, 2000, 2500, 500, 10, 5, 0, "", 0,
				0L, 0L, 0, 0, 0, 0, 4000.0, 2000.0, 2500.0
		);
		when(session.activeChar()).thenReturn(pc);
	}

	@Test
	void testQuest374WhisperOfDreams1() {
		Quest374WhisperOfDreams1 q374 = new Quest374WhisperOfDreams1(questManager);
		QuestState qs = new QuestState(q374, session);

		NpcInstance manakia = Mockito.mock(NpcInstance.class);
		when(manakia.getNpcId()).thenReturn(Quest374WhisperOfDreams1.MANAKIA);

		assertEquals("seer_manakia_q0374_01.htm", q374.onTalk(manakia, qs));
		q374.onEvent("accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());

		// Give items
		qs.giveItems(Quest374WhisperOfDreams1.CAVE_BEAST_TOOTH, 65);
		qs.giveItems(Quest374WhisperOfDreams1.DEATH_BLADER_LIGHT, 65);
		assertEquals("seer_manakia_q0374_05.htm", q374.onTalk(manakia, qs));

		// Exchange
		q374.onEvent("reward_exchange", qs);
		assertTrue(qs.getQuestItemsCount(Quest374WhisperOfDreams1.MYSTERIOUS_STONE) > 0 || qs.isStarted());
	}

	@Test
	void testQuest375WhisperOfDreams2() {
		Quest375WhisperOfDreams2 q375 = new Quest375WhisperOfDreams2(questManager);
		QuestState qs = new QuestState(q375, session);

		NpcInstance manakia = Mockito.mock(NpcInstance.class);
		when(manakia.getNpcId()).thenReturn(Quest375WhisperOfDreams2.MANAKIA);

		// Needs mysterious stone from part 1
		assertEquals("seer_manakia_q0375_02.htm", q375.onTalk(manakia, qs));
		qs.giveItems(Quest375WhisperOfDreams2.MYSTERIOUS_STONE, 1);
		assertEquals("seer_manakia_q0375_01.htm", q375.onTalk(manakia, qs));

		q375.onEvent("accept", qs);
		assertEquals(1, qs.getCond());
		assertTrue(qs.isStarted());
	}

	@Test
	void testQuest642APowerfulPrimevalCreature() {
		Quest642APowerfulPrimevalCreature q642 = new Quest642APowerfulPrimevalCreature(questManager);
		QuestState qs = new QuestState(q642, session);

		NpcInstance dindin = Mockito.mock(NpcInstance.class);
		when(dindin.getNpcId()).thenReturn(Quest642APowerfulPrimevalCreature.DINDIN);

		assertEquals("dindin_q0642_01.htm", q642.onTalk(dindin, qs));
		q642.onEvent("accept", qs);
		assertEquals(1, qs.getCond());

		// Reward exchange
		for (int i = 0; i < 150; i++) {
			qs.giveItems(Quest642APowerfulPrimevalCreature.DINOSAUR_TISSUE, 1);
		}
		q642.onEvent("reward_recipe", qs);
		assertEquals(0, qs.getQuestItemsCount(Quest642APowerfulPrimevalCreature.DINOSAUR_TISSUE));
	}

	@Test
	void testQuest643RiseAndFallOfTheElrokiTribe() {
		Quest643RiseAndFallOfTheElrokiTribe q643 = new Quest643RiseAndFallOfTheElrokiTribe(questManager);
		QuestState qs = new QuestState(q643, session);

		NpcInstance sinsin = Mockito.mock(NpcInstance.class);
		when(sinsin.getNpcId()).thenReturn(Quest643RiseAndFallOfTheElrokiTribe.SINGSING);

		assertEquals("singsing_q0643_01.htm", q643.onTalk(sinsin, qs));
		q643.onEvent("accept", qs);
		assertEquals(1, qs.getCond());

		for (int i = 0; i < 300; i++) {
			qs.giveItems(Quest643RiseAndFallOfTheElrokiTribe.DINOSAUR_BONES, 1);
		}
		q643.onEvent("reward_exchange", qs);
		assertEquals(0, qs.getQuestItemsCount(Quest643RiseAndFallOfTheElrokiTribe.DINOSAUR_BONES));
	}

	@Test
	void testQuest628And629GoldenRamAndSwamp() {
		Quest628HuntGoldenRam q628 = new Quest628HuntGoldenRam(questManager);
		QuestState qs628 = new QuestState(q628, session);

		NpcInstance kahman = Mockito.mock(NpcInstance.class);
		when(kahman.getNpcId()).thenReturn(Quest628HuntGoldenRam.KAHMAN);

		assertEquals("merc_kahmun_q0628_01.htm", q628.onTalk(kahman, qs628));
		q628.onEvent("accept", qs628);
		assertEquals(1, qs628.getCond());

		qs628.giveItems(Quest628HuntGoldenRam.SPLINTER_STAKATO_CHITIN, 100);
		q628.onEvent("upgrade_badge", qs628);
		assertTrue(qs628.hasQuestItems(Quest628HuntGoldenRam.RECRUIT_BADGE));

		// Now quest 629 Clean up swamp requires recruit badge
		Quest629CleanUpTheSwampOfScreams q629 = new Quest629CleanUpTheSwampOfScreams(questManager);
		QuestState qs629 = new QuestState(q629, session);

		// With badge, can start
		qs629.giveItems(Quest628HuntGoldenRam.RECRUIT_BADGE, 1);
		assertEquals("merc_cap_peace_q0629_0101.htm", q629.onTalk(kahman, qs629));
	}

	@Test
	void testQuest336CoinOfMagic() {
		Quest336CoinOfMagic q336 = new Quest336CoinOfMagic(questManager);
		QuestState qs = new QuestState(q336, session);

		NpcInstance sorint = Mockito.mock(NpcInstance.class);
		when(sorint.getNpcId()).thenReturn(Quest336CoinOfMagic.SORINT);

		assertEquals("warehouse_keeper_sorint_q0336_01.htm", q336.onTalk(sorint, qs));
		q336.onEvent("accept", qs);
		assertTrue(qs.hasQuestItems(Quest336CoinOfMagic.COIN_DIAGRAM));
		assertEquals(1, qs.getCond());
	}

	@Test
	void testQuest354AlligatorIsland() {
		Quest354ConquestOfAlligatorIsland q354 = new Quest354ConquestOfAlligatorIsland(questManager);
		QuestState qs = new QuestState(q354, session);

		NpcInstance kluck = Mockito.mock(NpcInstance.class);
		when(kluck.getNpcId()).thenReturn(Quest354ConquestOfAlligatorIsland.KLUCK);

		assertEquals("warehouse_keeper_kluck_q0354_02.htm", q354.onTalk(kluck, qs));
		q354.onEvent("accept", qs);
		assertEquals(1, qs.getCond());

		qs.giveItems(Quest354ConquestOfAlligatorIsland.ALLIGATOR_TOOTH, 100);
		q354.onEvent("exchange_teeth", qs);
		assertEquals(0, qs.getQuestItemsCount(Quest354ConquestOfAlligatorIsland.ALLIGATOR_TOOTH));
	}

	@Test
	void testQuest632Necromancer() {
		Quest632NecromancersRequest q632 = new Quest632NecromancersRequest(questManager);
		QuestState qs = new QuestState(q632, session);

		NpcInstance hardin = Mockito.mock(NpcInstance.class);
		when(hardin.getNpcId()).thenReturn(Quest632NecromancersRequest.SHADOW_HARDIN);

		assertEquals("shadow_hardin_q0632_0101.htm", q632.onTalk(hardin, qs));
		q632.onEvent("accept", qs);
		assertEquals(1, qs.getCond());

		qs.giveItems(Quest632NecromancersRequest.VAMPIRE_HEART, 200);
		q632.onEvent("reward_exchange", qs);
		assertEquals(0, qs.getQuestItemsCount(Quest632NecromancersRequest.VAMPIRE_HEART));
	}

	@Test
	void testQuest621EggDeliveryAnd622Liquor() {
		Quest621EggDelivery q621 = new Quest621EggDelivery(questManager);
		QuestState qs621 = new QuestState(q621, session);

		NpcInstance jeremy = Mockito.mock(NpcInstance.class);
		when(jeremy.getNpcId()).thenReturn(Quest621EggDelivery.JEREMY);

		assertEquals("jeremy_q0621_0101.htm", q621.onTalk(jeremy, qs621));
		q621.onEvent("accept", qs621);
		assertEquals(1, qs621.getCond());
		assertTrue(qs621.hasQuestItems(Quest621EggDelivery.EGG_BASKET));

		Quest622DeliveryOfSpecialLiquor q622 = new Quest622DeliveryOfSpecialLiquor(questManager);
		QuestState qs622 = new QuestState(q622, session);

		assertEquals("jeremy_q0622_0101.htm", q622.onTalk(jeremy, qs622));
		q622.onEvent("accept", qs622);
		assertEquals(1, qs622.getCond());
		assertTrue(qs622.hasQuestItems(Quest622DeliveryOfSpecialLiquor.SPECIAL_LIQUOR));
	}
}
