package com.lopez.l2j.game.service;

import static org.junit.jupiter.api.Assertions.*;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.item.ItemTemplateTable;
import com.lopez.l2j.game.item.TestItems;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.party.Party;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PvpRewardAndNoblesseTest {

	private PvpRewardService pvpRewardService;
	private InventoryService inventoryService;
	private GameWorld world;
	private ObjectIdFactory idFactory;
	private List<GameServerPacket> killerPackets;
	private List<GameServerPacket> victimPackets;
	private GameSession killerSession;
	private GameSession victimSession;
	private PlayerCharacter killer;
	private PlayerCharacter victim;

	@BeforeEach
	void setUp() throws Exception {
		Config.reload();
		Config.ALLOW_PVP_REWARD_SYSTEM = true;
		Config.PVP_REWARD_ITEM = "57,100;6392,10;";
		Config.ALLOW_PK_REWARD_SYSTEM = true;
		Config.PK_REWARD_ITEM = "57,100;6393,5;";
		Config.PVP_CONGRATULATIONS_MSG = true;
		Config.ANNOUNCE_PK_PVP = false;
		Config.KILL_BARAKIEL_SET_NOBLESS = true;
		Config.NOBLESSE_ITEM_ID = 9229;
		Config.LEAVE_BUFFS_ON_DIE = true;

		pvpRewardService = new PvpRewardService(null, null);
		pvpRewardService.setCooldownMinutes(5);

		idFactory = ObjectIdFactory.sequential(10000);
		List<ItemTemplate> templates = new ArrayList<>(TestItems.templates());
		templates.add(ItemTemplate.etc(6392, 6392, "Event Medal", "none", "stackable", 0, "none", 0, true, true, true, true));
		templates.add(ItemTemplate.etc(6393, 6393, "Glittering Medal", "none", "stackable", 0, "none", 0, true, true, true, true));
		templates.add(ItemTemplate.etc(9229, 9229, "Noblesse Tiara", "none", "normal", 0, "none", 0, true, true, true, true));
		ItemTemplateTable itemTemplates = ItemTemplateTable.of(templates, List.of());
		var itemRepo = new com.lopez.l2j.game.item.InMemoryItemRepository();
		inventoryService = new InventoryService(itemTemplates, itemRepo, idFactory, 10_000_000);
		world = new GameWorld();

		killerPackets = new ArrayList<>();
		victimPackets = new ArrayList<>();

		killer = new PlayerCharacter(101, "Killer", "Title", 0, 0, 0, 0, 0, 0, false, 0, 0, 0,
				1000, 1000, 1000, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 1000.0, 1000.0, 1000.0);
		killer.inventory(new Inventory(killer.objectId()));
		killer.level(75);

		victim = new PlayerCharacter(102, "Victim", "Title", 0, 0, 0, 0, 0, 0, false, 0, 0, 0,
				500, 500, 500, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 500.0, 500.0, 500.0);
		victim.inventory(new Inventory(victim.objectId()));
		victim.level(75);

		var mockCtx = org.mockito.Mockito.mock(com.lopez.l2j.network.game.GameSession.Context.class);
		var mockCharSvc = org.mockito.Mockito.mock(CharacterService.class);
		var charTemplates = new CharTemplateTable();
		var sampleTemplate = charTemplates.get(0).orElse(null);
		org.mockito.Mockito.when(mockCharSvc.template(org.mockito.Mockito.any())).thenReturn(sampleTemplate);
		org.mockito.Mockito.when(mockCtx.inventories()).thenReturn(inventoryService);
		org.mockito.Mockito.when(mockCtx.characters()).thenReturn(mockCharSvc);
		org.mockito.Mockito.when(mockCtx.world()).thenReturn(world);
		org.mockito.Mockito.when(mockCtx.pvpRewards()).thenReturn(pvpRewardService);

		killerSession = new GameSession(mockCtx, new byte[16], "192.168.1.10", killerPackets::add);
		setField(killerSession, "state", GameClientPacket.State.IN_GAME);
		setField(killerSession, "active", killer);
		setField(killerSession, "inWorld", true);

		victimSession = new GameSession(mockCtx, new byte[16], "192.168.1.20", victimPackets::add);
		setField(victimSession, "state", GameClientPacket.State.IN_GAME);
		setField(victimSession, "active", victim);
		setField(victimSession, "inWorld", true);

		world.add(killerSession);
		world.add(victimSession);
	}

	@Test
	@DisplayName("PvpRewardService: parseRewardItems faz parsing correto de itens e quantidades")
	void testParseRewardItems() {
		var list = pvpRewardService.parseRewardItems("57,100;6392,10;");
		assertEquals(2, list.size());
		assertEquals(57, list.get(0).itemId());
		assertEquals(100, list.get(0).count());
		assertEquals(6392, list.get(1).itemId());
		assertEquals(10, list.get(1).count());

		assertTrue(pvpRewardService.parseRewardItems("").isEmpty());
		assertTrue(pvpRewardService.parseRewardItems(null).isEmpty());
	}

	@Test
	@DisplayName("PvpRewardService: Anti-farm bloqueia mesmo IP e mesmo clã")
	void testAntiFarmProtection() throws Exception {
		// Mesma sessão / suicídio
		assertFalse(pvpRewardService.checkAntiFarm(killerSession, killerSession));

		// Mesmo IP
		var sameIpSession = new GameSession(killerSession.context(), new byte[16], "192.168.1.10", p -> {});
		setField(sameIpSession, "active", victim);
		assertFalse(pvpRewardService.checkAntiFarm(killerSession, sameIpSession), "Mesmo IP externo deve ser bloqueado");

		// Mesmo clã
		killer.clanId(5);
		victim.clanId(5);
		assertFalse(pvpRewardService.checkAntiFarm(killerSession, victimSession), "Membros do mesmo clã devem ser bloqueados");
		killer.clanId(0);
		victim.clanId(0);

		// Primeiro abate válido
		assertTrue(pvpRewardService.checkAntiFarm(killerSession, victimSession));

		// Segundo abate imediato deve ser bloqueado por cooldown
		assertFalse(pvpRewardService.checkAntiFarm(killerSession, victimSession), "Segundo abate em menos de 5 min deve ser bloqueado");
	}

	@Test
	@DisplayName("PvpRewardService: Abate PvP válido entrega itens configurados e incrementa pvpKills")
	void testPvPKillReward() {
		victim.pvpFlag(1); // Flag de PvP ativo
		assertEquals(0, killer.pvpKills());

		pvpRewardService.handleKill(killerSession, victimSession);

		assertEquals(1, killer.pvpKills(), "pvpKills deve ser incrementado");
		assertEquals(0, killer.pkKills());

		// Verifica itens recebidos no inventário
		var adena = killer.inventory().byItemId(57);
		assertTrue(adena.isPresent(), "Deve receber Adena (ID 57)");
		assertEquals(100, adena.get().count());

		var medal = killer.inventory().byItemId(6392);
		assertTrue(medal.isPresent(), "Deve receber Medalha de Evento (ID 6392)");
		assertEquals(10, medal.get().count());
	}

	@Test
	@DisplayName("PvpRewardService: Abate PK válido entrega itens PK, incrementa pkKills e aumenta Karma")
	void testPkKillReward() {
		victim.pvpFlag(0); // Vítima inocente
		victim.karma(0);
		assertEquals(0, killer.pkKills());
		int initialKarma = killer.karma();

		pvpRewardService.handleKill(killerSession, victimSession);

		assertEquals(1, killer.pkKills(), "pkKills deve ser incrementado");
		assertEquals(0, killer.pvpKills());
		assertTrue(killer.karma() > initialKarma, "Karma deve aumentar após PK");

		// Verifica itens de PK recebidos
		var adena = killer.inventory().byItemId(57);
		assertTrue(adena.isPresent());
		assertEquals(100, adena.get().count());

		var pkItem = killer.inventory().byItemId(6393);
		assertTrue(pkItem.isPresent());
		assertEquals(5, pkItem.get().count());
	}

	@Test
	@DisplayName("Barakiel: Derrota de Barakiel (ID 25325) concede Noblesse para jogadores nível >= 75")
	void testBarakielNoblesseReward() throws Exception {
		killer.level(75);
		assertFalse(killer.isNoble());

		PlayerCharacter partyMember = new PlayerCharacter(103, "Member", "Title", 0, 0, 0, 0, 0, 0, false, 0, 0, 0,
				500, 500, 500, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 500.0, 500.0, 500.0);
		partyMember.level(76);
		assertFalse(partyMember.isNoble());

		PlayerCharacter lowLevelMember = new PlayerCharacter(104, "LowLvl", "Title", 0, 0, 0, 0, 0, 0, false, 0, 0, 0,
				500, 500, 500, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 500.0, 500.0, 500.0);
		lowLevelMember.level(60);
		assertFalse(lowLevelMember.isNoble());

		GameSession memberSession = new GameSession(killerSession.context(), new byte[16], "1.1.1.2", p -> {});
		setField(memberSession, "active", partyMember);
		GameSession lowLevelSession = new GameSession(killerSession.context(), new byte[16], "1.1.1.3", p -> {});
		setField(lowLevelSession, "active", lowLevelMember);

		Party party = new Party(killerSession, memberSession, 0);
		party.addMember(lowLevelSession);
		setField(killerSession, "party", party);

		var barakielTpl = new com.lopez.l2j.game.npc.NpcTemplate(25325, 25325, "Flame of Splendor Barakiel",
				false, "RaidBoss", false, 40.0, 80.0, 80, "male", "L2RaidBoss", 100, 1000, 1000, 1000, 1000,
				100, 100, 1000, 1000, 100, 100, 100, 100, 100, 100, true);
		var barakiel = new com.lopez.l2j.game.npc.NpcInstance(20001, barakielTpl, 0, 0, 0, 0);

		Config.KILL_BARAKIEL_SET_NOBLESS = true;

		// Dispara a concessão via método privado em GameSession
		try {
			var method = GameSession.class.getDeclaredMethod("rewardBarakielNoblesse", Party.class, PlayerCharacter.class);
			method.setAccessible(true);
			method.invoke(killerSession, party, killer);
		} catch (Exception e) {
			fail("Erro ao invocar rewardBarakielNoblesse: " + e.getMessage());
		}

		assertTrue(killer.isNoble(), "Killer lvl 75 deve se tornar Nobre");
		assertTrue(partyMember.isNoble(), "Party member lvl 76 deve se tornar Nobre");
		assertFalse(lowLevelMember.isNoble(), "Party member lvl 60 NÃO deve se tornar Nobre");
	}

	@AfterEach
	void tearDown() {
		Config.reload();
	}

	@Test
	@DisplayName("LeaveBuffsOnDie = False: Mantém buffs ao morrer")
	void testLeaveBuffsOnDieConfig() {
		try {
			Config.LEAVE_BUFFS_ON_DIE = false;
			killer.effects().addBuff(1086, 1, 1200); // Haste
			assertEquals(1, killer.effects().active().size());

			killerSession.handlePlayerDeath(null);

			assertEquals(1, killer.effects().active().size(), "Com LeaveBuffsOnDie = False, buffs devem ser preservados na morte");
		} finally {
			Config.LEAVE_BUFFS_ON_DIE = true;
		}
	}

	private static void setField(Object target, String name, Object val) {
		try {
			Field f = target.getClass().getDeclaredField(name);
			f.setAccessible(true);
			f.set(target, val);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
