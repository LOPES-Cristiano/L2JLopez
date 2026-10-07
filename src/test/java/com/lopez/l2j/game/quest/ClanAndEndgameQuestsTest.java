package com.lopez.l2j.game.quest;

import static org.assertj.core.api.Assertions.assertThat;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.quest.impl.Quest350EnhanceYourWeapon;
import com.lopez.l2j.game.quest.impl.Quest501ProofOfClanAlliance;
import com.lopez.l2j.game.quest.impl.Quest605AllianceWithKetraOrcs;
import com.lopez.l2j.game.quest.impl.Quest617GatherTheFlames;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClanAndEndgameQuestsTest {

	private Quest501ProofOfClanAlliance quest501;
	private Quest605AllianceWithKetraOrcs quest605;
	private Quest350EnhanceYourWeapon quest350;
	private Quest617GatherTheFlames quest617;

	private GameSession session;
	private PlayerCharacter playerChar;

	@BeforeEach
	void setUp() {
		quest501 = new Quest501ProofOfClanAlliance();
		quest605 = new Quest605AllianceWithKetraOrcs();
		quest350 = new Quest350EnhanceYourWeapon();
		quest617 = new Quest617GatherTheFlames();

		playerChar = new PlayerCharacter(1001, "acc", "ClanHero", 75, 0, 0, 0, 0, 0, false, 0, 0, 0,
				3000, 1500, 1500, 0, 0, 0, 0, "", 0, 0, 0, 100, 100, 0, 0, 3000.0, 1500.0, 1500.0);
		playerChar.inventory(new Inventory(playerChar.objectId()));
		playerChar.clanId(50);
		playerChar.clanLeader(true);

		var mockCtx = org.mockito.Mockito.mock(com.lopez.l2j.network.game.GameSession.Context.class);
		var mockInvSvc = org.mockito.Mockito.mock(InventoryService.class);
		org.mockito.Mockito.when(mockCtx.inventories()).thenReturn(mockInvSvc);

		session = new GameSession(mockCtx, new byte[16], "127.0.0.1", p -> {});
		setField(session, "state", GameClientPacket.State.IN_GAME);
		setField(session, "active", playerChar);
		setField(session, "inWorld", true);
	}

	private static void setField(Object obj, String fieldName, Object val) {
		try {
			var f = obj.getClass().getDeclaredField(fieldName);
			f.setAccessible(true);
			f.set(obj, val);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	@DisplayName("CP 7.1: Integridade do Catálogo de Quests de Clã (501 e 503)")
	void testClanQuestCatalogIntegrity() {
		var entries = ClanQuestCatalog.allEntries();
		assertThat(entries).hasSize(2);

		var q501 = ClanQuestCatalog.findById(501);
		assertThat(q501).isPresent();
		assertThat(q501.get().targetClanLevel()).isEqualTo(4);
		assertThat(q501.get().rewardItemId()).isEqualTo(3874); // Alliance Manifesto
		assertThat(q501.get().rewardSp()).isEqualTo(120_000);

		var q503 = ClanQuestCatalog.findById(503);
		assertThat(q503).isPresent();
		assertThat(q503.get().targetClanLevel()).isEqualTo(5);
		assertThat(q503.get().rewardItemId()).isEqualTo(3870); // Seal of Aspiration
		assertThat(q503.get().rewardSp()).isEqualTo(250_000);
	}

	@Test
	@DisplayName("CP 7.2: Integridade do Catálogo de Alianças Épicas Ketra e Varka (605 e 611)")
	void testFactionAllianceCatalogIntegrity() {
		var ketra = FactionAllianceCatalog.findByQuestId(605);
		assertThat(ketra).isPresent();
		assertThat(ketra.get().factionName()).isEqualTo("Ketra Orcs");
		assertThat(ketra.get().hierarchNpcId()).isEqualTo(31371);
		assertThat(ketra.get().markOfAllianceItemIds()).containsExactly(7211, 7212, 7213, 7214, 7215);

		var varka = FactionAllianceCatalog.findByQuestId(611);
		assertThat(varka).isPresent();
		assertThat(varka.get().factionName()).isEqualTo("Varka Silenos");
		assertThat(varka.get().hierarchNpcId()).isEqualTo(31378);
		assertThat(varka.get().markOfAllianceItemIds()).containsExactly(7221, 7222, 7223, 7224, 7225);
	}

	@Test
	@DisplayName("CP 7.3: Integridade do Catálogo Endgame de Farm e Soul Crystals (350, 373, 617, 619)")
	void testEndgameFarmCatalogIntegrity() {
		var q350 = EndgameFarmCatalog.findById(350);
		assertThat(q350).isPresent();
		assertThat(q350.get().questName()).isEqualTo("Enhance Your Weapon");

		var q373 = EndgameFarmCatalog.findById(373);
		assertThat(q373).isPresent();
		assertThat(q373.get().questName()).isEqualTo("Supplier of Reagents");

		var q617 = EndgameFarmCatalog.findById(617);
		assertThat(q617).isPresent();
		assertThat(q617.get().questName()).isEqualTo("Gather the Flames");

		var q619 = EndgameFarmCatalog.findById(619);
		assertThat(q619).isPresent();
		assertThat(q619.get().questName()).isEqualTo("Relics of the Old Empire");
	}

	@Test
	@DisplayName("CP 7.1: Fluxo da Quest 501 (Proof of Clan Alliance) — Concessão de Alliance Manifesto e SP")
	void testQuest501ProofOfClanAllianceFlow() {
		NpcTemplate rodemaiTpl = new NpcTemplate(30756, 30756, "Sir Kristof Rodemai", false, "", false,
				8.0, 16.0, 70, "male", "L2Npc", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, "", 0);
		NpcInstance rodemai = new NpcInstance(9001, rodemaiTpl, 0, 0, 0, 0);

		NpcTemplate kalisTpl = new NpcTemplate(30759, 30759, "Witch Kalis", false, "", false,
				8.0, 16.0, 70, "female", "L2Npc", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, "", 0);
		NpcInstance kalis = new NpcInstance(9002, kalisTpl, 0, 0, 0, 0);

		// 1. Líder fala com Rodemai e aceita a missão
		String talk1 = quest501.onTalk(rodemai, session);
		assertThat(talk1).isEqualTo("30756-04.htm");

		quest501.onAdvEvent("30756-07.htm", rodemai, session);
		QuestState st = session.getQuestState(Quest501ProofOfClanAlliance.QUEST_NAME);
		assertThat(st).isNotNull();
		assertThat(st.isStarted()).isTrue();
		assertThat(st.getInt("cond")).isEqualTo(1);

		// 2. Líder fala com Bruxa Kalis
		quest501.onAdvEvent("30759-03.htm", kalis, session);
		assertThat(st.getInt("part")).isEqualTo(2);

		// Simula entrega de 3 Symbols of Loyalty
		st.giveItems(Quest501ProofOfClanAlliance.SYMBOL_OF_LOYALTY, 3);
		quest501.onAdvEvent("30759-07.htm", kalis, session);
		assertThat(st.hasQuestItems(Quest501ProofOfClanAlliance.ANTIDOTE_RECIPE)).isTrue();
		assertThat(st.count(Quest501ProofOfClanAlliance.SYMBOL_OF_LOYALTY)).isEqualTo(0);

		// 3. Coleta dos ingredientes do antídoto
		st.giveItems(Quest501ProofOfClanAlliance.HERB_OF_HARIT, 1);
		st.giveItems(Quest501ProofOfClanAlliance.HERB_OF_VANOR, 1);
		st.giveItems(Quest501ProofOfClanAlliance.HERB_OF_OEL_MAHUM, 1);
		st.giveItems(Quest501ProofOfClanAlliance.BLOOD_OF_EVA, 1);

		// Entrega ingredientes para Kalis
		st.set("part", 5);
		String kalisReward = quest501.onTalk(kalis, session);
		assertThat(kalisReward).isEqualTo("30759-08.htm");
		assertThat(st.hasQuestItems(Quest501ProofOfClanAlliance.VOUCHER_OF_FAITH)).isTrue();

		// 4. Retorna para Rodemai para concluir a quest
		int spBefore = playerChar.sp();
		String rodemaiFinish = quest501.onTalk(rodemai, session);
		assertThat(rodemaiFinish).isEqualTo("30756-09.htm");
		assertThat(st.isCompleted()).isTrue();
		assertThat(st.hasQuestItems(Quest501ProofOfClanAlliance.ALLIANCE_MANIFESTO)).isTrue();
		assertThat(playerChar.sp()).isEqualTo(spBefore + 120_000);
	}

	@Test
	@DisplayName("CP 7.2: Fluxo da Quest 605 (Alliance with Ketra Orcs) — Coleta e Promoção para Nível 1")
	void testQuest605AllianceWithKetraOrcsFlow() {
		NpcTemplate wahkanTpl = new NpcTemplate(31371, 31371, "Hierarch Wahkan", false, "", false,
				8.0, 16.0, 80, "male", "L2Npc", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, "", 0);
		NpcInstance wahkan = new NpcInstance(9003, wahkanTpl, 0, 0, 0, 0);

		// Inicia aliança
		quest605.onAdvEvent("31371-04.htm", wahkan, session);
		QuestState st = session.getQuestState(Quest605AllianceWithKetraOrcs.QUEST_NAME);
		assertThat(st).isNotNull();
		assertThat(st.isStarted()).isTrue();
		assertThat(st.getInt("cond")).isEqualTo(1);

		// Simula abate de 100 soldados Varka
		st.giveItems(Quest605AllianceWithKetraOrcs.VARKA_BADGE_SOLDIER, 100);

		// Fala com Wahkan para receber a Mark of Ketra's Alliance Stage 1
		String promote = quest605.onTalk(wahkan, session);
		assertThat(promote).isEqualTo("31371-05.htm");
		assertThat(st.hasQuestItems(Quest605AllianceWithKetraOrcs.MARK_KETRA_1)).isTrue();
		assertThat(st.count(Quest605AllianceWithKetraOrcs.VARKA_BADGE_SOLDIER)).isEqualTo(0);
		assertThat(st.getInt("cond")).isEqualTo(2);
	}

	@Test
	@DisplayName("CP 7.3: Quest 350 (Enhance Your Weapon) — Escolha do Soul Crystal")
	void testQuest350EnhanceYourWeaponSelection() {
		NpcTemplate jurekTpl = new NpcTemplate(30115, 30115, "Magister Jurek", false, "", false,
				8.0, 16.0, 70, "male", "L2Npc", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, "", 0);
		NpcInstance jurek = new NpcInstance(9004, jurekTpl, 0, 0, 0, 0);

		quest350.onAdvEvent("30115-04.htm", jurek, session);
		QuestState st = session.getQuestState(Quest350EnhanceYourWeapon.QUEST_NAME);
		assertThat(st).isNotNull();
		assertThat(st.isStarted()).isTrue();

		// Escolhe Red Soul Crystal
		quest350.onAdvEvent("30115-09.htm", jurek, session);
		assertThat(st.hasQuestItems(Quest350EnhanceYourWeapon.RED_SOUL_CRYSTAL_0)).isTrue();
	}

	@Test
	@DisplayName("CP 7.3: Quest 617 (Gather the Flames) — Troca de 1000 Torches por Receita S-Grade")
	void testQuest617GatherTheFlamesTorchExchange() {
		NpcTemplate vulcanTpl = new NpcTemplate(31539, 31539, "Blacksmith Vulcan", false, "", false,
				8.0, 16.0, 80, "male", "L2Npc", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, "", 0);
		NpcInstance vulcan = new NpcInstance(9005, vulcanTpl, 0, 0, 0, 0);

		quest617.onAdvEvent("31539-03.htm", vulcan, session);
		QuestState st = session.getQuestState(Quest617GatherTheFlames.QUEST_NAME);
		assertThat(st).isNotNull();
		assertThat(st.isStarted()).isTrue();

		// Simula coleta de 1.000 Torches
		st.giveItems(Quest617GatherTheFlames.TORCH, 1000);
		assertThat(st.count(Quest617GatherTheFlames.TORCH)).isEqualTo(1000);

		// Troca com Vulcan por receita S-Grade
		String exchangeResult = quest617.onAdvEvent("31539-05.htm", vulcan, session);
		assertThat(exchangeResult).isEqualTo("31539-07.htm");
		assertThat(st.count(Quest617GatherTheFlames.TORCH)).isEqualTo(0);

		// Verifica que alguma das 10 receitas S-Grade oficiais foi entregue
		boolean hasSGradeRecipe = Quest617GatherTheFlames.S_GRADE_WEAPON_RECIPES.stream()
				.anyMatch(st::hasQuestItems);
		assertThat(hasSGradeRecipe).isTrue();
	}

	@Test
	@DisplayName("Fluxo da Quest 503 (Pursuit of Clan Ambition) para obtencao do Scepter of Judgement")
	void testQuest503PursuitOfClanAmbitionFlow() {
		var quest503 = new com.lopez.l2j.game.quest.impl.Quest503PursuitOfClanAmbition(null);

		playerChar.clanLeader(true);
		playerChar.level(60);

		NpcTemplate gustafTpl = new NpcTemplate(30758, 30758, "Sir Gustaf Athebalt", false, "", false,
				8.0, 16.0, 75, "male", "L2Npc", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, "", 0);
		NpcInstance gustaf = new NpcInstance(9006, gustafTpl, 0, 0, 0, 0);

		// Inicio da quest
		String intro = quest503.onTalk(gustaf, session);
		assertThat(intro).isEqualTo("gustaf_intro.htm");

		quest503.onAdvEvent("quest_accept", gustaf, session);
		QuestState st = session.getQuestState(com.lopez.l2j.game.quest.impl.Quest503PursuitOfClanAmbition.QUEST_NAME);
		assertThat(st).isNotNull();
		assertThat(st.isStarted()).isTrue();
		assertThat(st.getCond()).isEqualTo(1);
		assertThat(st.getQuestItemsCount(com.lopez.l2j.game.quest.impl.Quest503PursuitOfClanAmbition.GUSTAF_INSTRUCTION)).isEqualTo(1);

		// Dialogo com Martien
		NpcTemplate martienTpl = new NpcTemplate(30645, 30645, "Martien", false, "", false,
				8.0, 16.0, 75, "male", "L2Npc", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, "", 0);
		NpcInstance martien = new NpcInstance(9007, martienTpl, 0, 0, 0, 0);

		quest503.onTalk(martien, session);
		assertThat(st.getCond()).isEqualTo(2);

		// Finalizacao com Gustaf
		quest503.onAdvEvent("gustaf_finish", gustaf, session);
		assertThat(st.isCompleted()).isTrue();
		assertThat(st.getQuestItemsCount(com.lopez.l2j.game.quest.impl.Quest503PursuitOfClanAmbition.SCEPTER_OF_JUDGEMENT)).isEqualTo(1);
	}
}
