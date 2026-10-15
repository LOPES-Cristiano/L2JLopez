package com.lopez.l2j;

import com.lopez.l2j.game.model.ClassId;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.service.CustomServiceNpcHandler;
import com.lopez.l2j.game.service.VillageMasterOccupationService;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VillageMasterAndCustomNpcsTest {

	private QuestManager questManager;
	private VillageMasterOccupationService villageMasterService;
	private CustomServiceNpcHandler customServiceHandler;
	private List<GameServerPacket> sentPackets;
	private GameSession session;
	private PlayerCharacter player;

	@BeforeEach
	void setUp() {
		sentPackets = new ArrayList<>();
		questManager = new QuestManager(null);

		villageMasterService = new VillageMasterOccupationService(questManager);
		customServiceHandler = new CustomServiceNpcHandler(questManager);

		player = new PlayerCharacter(2001, "TestHeroVillage");
		player.level(20);
		player.classId(0); // Human Fighter
		player.inventory().addAdena(10_000_000);
		player.inventory().addItem(CustomServiceNpcHandler.GOLD_BAR, 100);

		var mockCtx = org.mockito.Mockito.mock(com.lopez.l2j.network.game.GameSession.Context.class);
		org.mockito.Mockito.when(mockCtx.questManager()).thenReturn(questManager);

		session = new GameSession(mockCtx, new byte[16], "127.0.0.1", sentPackets::add);
		setField(session, "state", GameClientPacket.State.IN_GAME);
		setField(session, "active", player);
		setField(session, "inWorld", true);
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
	@DisplayName("1. VillageMaster: listagem de 1ª ocupação para Human Fighter (Lv 20+)")
	void testVillageMaster1stClassListing() {
		String html = villageMasterService.listEligibleNextClasses(player);
		assertThat(html).containsIgnoringCase("warrior").containsIgnoringCase("knight").containsIgnoringCase("rogue");
	}

	@Test
	@DisplayName("2. VillageMaster: avanço de classe efetivo")
	void testVillageMasterClassChange() {
		String res = villageMasterService.changePlayerClass(player, ClassId.warrior.getId());
		assertThat(res).contains("Parabéns");
		assertThat(player.classId()).isEqualTo(ClassId.warrior.getId());
	}

	@Test
	@DisplayName("3. ClassMaster: avanço de 1ª classe via Adena")
	void testClassMaster1stClassAdvance() {
		NpcTemplate tpl = NpcTemplate.fallback(50032, "Class Master", "Service");
		NpcInstance npc = new NpcInstance(1001, tpl, 0, 0, 0, 0);

		long adenaBefore = player.inventory().adena();
		String reply = customServiceHandler.onAdvEvent("class_advance_" + ClassId.knight.getId(), npc, session);
		assertThat(reply).contains("sucesso");
		assertThat(player.classId()).isEqualTo(ClassId.knight.getId());
		assertThat(player.inventory().adena()).isEqualTo(adenaBefore - 100_000L);
	}

	@Test
	@DisplayName("4. NoblesSeller: aquisição de Noblesse com Gold Bars (Zero Doações)")
	void testNoblesSellerWithGoldBar() {
		NpcTemplate tpl = NpcTemplate.fallback(50034, "Noblesse Manager", "Service");
		NpcInstance npc = new NpcInstance(1002, tpl, 0, 0, 0, 0);

		assertThat(player.isNoble()).isFalse();
		int gbBefore = player.inventory().getItemCount(CustomServiceNpcHandler.GOLD_BAR);

		String reply = customServiceHandler.onAdvEvent("buy_nobles", npc, session);
		assertThat(reply).contains("Noblesse foi ativado");
		assertThat(player.isNoble()).isTrue();
		assertThat(player.inventory().getItemCount(CustomServiceNpcHandler.GOLD_BAR)).isEqualTo(gbBefore - 50);
	}

	@Test
	@DisplayName("5. Limpador PK / Karma: limpeza por Adena e Gold Bar")
	void testPkKarmaClean() {
		NpcTemplate tpl = NpcTemplate.fallback(50038, "Cleaner", "Service");
		NpcInstance npc = new NpcInstance(1003, tpl, 0, 0, 0, 0);

		player.karma(1000);
		player.pkKills(5);

		customServiceHandler.onAdvEvent("clean_karma", npc, session);
		assertThat(player.karma()).isEqualTo(0);

		customServiceHandler.onAdvEvent("clean_pk", npc, session);
		assertThat(player.pkKills()).isEqualTo(4);
	}

	@Test
	@DisplayName("6. LevelManager: subir e descer nível com Adena")
	void testLevelManager() {
		NpcTemplate tpl = NpcTemplate.fallback(50039, "Level Manager", "Service");
		NpcInstance npc = new NpcInstance(1004, tpl, 0, 0, 0, 0);

		int lvlBefore = player.level();
		customServiceHandler.onAdvEvent("level_up", npc, session);
		assertThat(player.level()).isEqualTo(lvlBefore + 1);

		customServiceHandler.onAdvEvent("level_down", npc, session);
		assertThat(player.level()).isEqualTo(lvlBefore);
	}

	@Test
	@DisplayName("7. RaidbossInfo: ativação do Radar no cliente para Grand Boss")
	void testRaidbossRadarControl() {
		NpcTemplate tpl = NpcTemplate.fallback(31729, "Raid Info", "Information");
		NpcInstance npc = new NpcInstance(1005, tpl, 0, 0, 0, 0);

		String reply = customServiceHandler.onAdvEvent("radar_boss_29020", npc, session); // Baium
		assertThat(reply).contains("radar foi apontado");
		assertThat(sentPackets).isNotEmpty();
	}
}
