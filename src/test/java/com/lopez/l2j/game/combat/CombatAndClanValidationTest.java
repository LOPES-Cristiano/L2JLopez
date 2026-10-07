package com.lopez.l2j.game.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.clan.ClanTable;
import com.lopez.l2j.game.item.InMemoryItemRepository;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.TestItems;
import com.lopez.l2j.game.model.InMemoryCharacterRepository;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.party.Party;
import com.lopez.l2j.game.service.CharacterService;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.ActionFailed;
import com.lopez.l2j.network.game.packet.GameServerPacket.MagicSkillCanceld;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CombatAndClanValidationTest {

	private ClanTable clanTable;
	private PlayerCharacter leader;
	private PlayerCharacter member1;
	private PlayerCharacter member2;

	private GameWorld world;
	private GameSession session1;
	private GameSession session2;
	private List<GameServerPacket> sentPackets1;
	private List<GameServerPacket> sentPackets2;

	@BeforeEach
	void setUp() {
		clanTable = new ClanTable((org.springframework.jdbc.core.simple.JdbcClient) null,
				ObjectIdFactory.sequential(0x40000000), null);

		leader = new PlayerCharacter(0x10000010, "LeaderChar", "Leader", 25, 0, 0, 0, 0, 0, false, 0, 0, 0,
				1000, 500, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 1000.0, 500.0, 100.0);
		member1 = new PlayerCharacter(0x10000011, "MemberOne", "Member", 20, 0, 0, 0, 0, 0, false, 0, 0, 0,
				1000, 500, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 1000.0, 500.0, 100.0);
		member2 = new PlayerCharacter(0x10000012, "MemberTwo", "Member", 20, 0, 0, 0, 0, 0, false, 0, 0, 0,
				1000, 500, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 1000.0, 500.0, 100.0);

		world = new GameWorld();
		sentPackets1 = new ArrayList<>();
		sentPackets2 = new ArrayList<>();

		var repo = new InMemoryItemRepository();
		var inventoryService = new InventoryService(TestItems.table(), repo, ObjectIdFactory.sequential(0x20000000), 10_000_000);
		var charTemplates = new CharTemplateTable();
		var charRepo = new InMemoryCharacterRepository();
		var charService = new CharacterService(charRepo, charTemplates, inventoryService);
		var combat = new CombatService();

		GameSession.Context ctx = new GameSession.Context(746, 746, null, charService, inventoryService, world, null,
				null, null, combat, null, null, null, null, null, null, null, null, null, "TestServer");

		leader.inventory(new Inventory(leader.objectId()));
		member1.inventory(new Inventory(member1.objectId()));

		session1 = new GameSession(ctx, new byte[8], "127.0.0.1", sentPackets1::add);
		setField(session1, "state", GameClientPacket.State.IN_GAME);
		setField(session1, "active", leader);
		setField(session1, "inWorld", true);
		world.add(session1);

		session2 = new GameSession(ctx, new byte[8], "127.0.0.1", sentPackets2::add);
		setField(session2, "state", GameClientPacket.State.IN_GAME);
		setField(session2, "active", member1);
		setField(session2, "inWorld", true);
		world.add(session2);
	}

	@Test
	@DisplayName("V.34: Penalidade de 24h para saida voluntaria e expulsao de cla")
	void testClanPenalties() {
		// Criacao do cla
		Clan clan = clanTable.createClan(leader, "TestClan");
		assertNotNull(clan);
		assertEquals(clan.clanId(), leader.clanId());

		// Adiciona member1
		assertTrue(clanTable.addClanMember(clan, member1));
		assertEquals(clan.clanId(), member1.clanId());

		// Member1 sai voluntariamente
		assertTrue(clanTable.removeClanMember(clan, member1, false));
		assertEquals(0, member1.clanId());
		assertTrue(member1.hasClanJoinPenalty(), "Membro que saiu voluntariamente deve receber penalidade de 24h");
		assertFalse(clan.hasCharPenalty(), "Cla nao recebe penalidade quando membro sai por conta propria");

		// Tentativa de readicionar member1 enquanto sob penalidade deve falhar
		assertFalse(clanTable.addClanMember(clan, member1), "Membro com penalidade ativa nao pode ingressar em cla");

		// Tentativa de criar cla com jogador sob penalidade deve falhar
		assertNull(clanTable.createClan(member1, "RogueClan"), "Jogador com penalidade nao pode criar cla");

		// Adiciona member2
		assertTrue(clanTable.addClanMember(clan, member2));

		// Lider expulsa member2
		assertTrue(clanTable.removeClanMember(clan, member2, true));
		assertEquals(0, member2.clanId());
		assertTrue(member2.hasClanJoinPenalty(), "Membro expulso deve receber penalidade de 24h");
		assertTrue(clan.hasCharPenalty(), "Cla deve receber penalidade de 24h ao expulsar um membro");

		// Criar member3 limpo (sem penalidade)
		PlayerCharacter member3 = new PlayerCharacter(0x10000013, "MemberThree", "Member", 20, 0, 0, 0, 0, 0, false, 0, 0, 0,
				1000, 500, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 1000.0, 500.0, 100.0);
		assertFalse(member3.hasClanJoinPenalty());

		// Cla nao pode recrutar novos membros enquanto sob penalidade de expulsao
		assertFalse(clanTable.addClanMember(clan, member3), "Cla com charPenalty ativa nao pode recrutar membros");

		// Apos expirar o tempo, penalidade cessa
		clan.charPenaltyExpiryTime(System.currentTimeMillis() - 1000L);
		assertFalse(clan.hasCharPenalty());
		assertTrue(clanTable.addClanMember(clan, member3), "Cla deve poder recrutar normalmente apos o fim da penalidade");
	}

	@Test
	@DisplayName("V.08: Cancelamento de conjuracao (Cast Break) por dano massivo")
	void testCastBreakOnMassiveDamage() {
		// Coloca session1 em estado de conjuracao (casting = true com castTask ativo)
		setField(session1, "casting", true);
		var scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
		var futureTask = scheduler.schedule(() -> {}, 1, java.util.concurrent.TimeUnit.HOURS);
		setField(session1, "castTask", futureTask);
		leader.currentHp(1000.0);
		leader.maxHp(1000);

		// Dano insignificante (1 de HP em 1000) nao deve interromper cast
		session1.onAttacked(member1.objectId(), 1);
		boolean isCasting = (boolean) getField(session1, "casting");
		assertTrue(isCasting, "Dano pequeno de 1 nao deve interromper o cast");

		// Dano massivo (800 de dano em 1000 HP maximo = 80% do HP)
		session1.onAttacked(member1.objectId(), 800);
		isCasting = (boolean) getField(session1, "casting");
		assertFalse(isCasting, "Dano massivo deve quebrar a conjuracao");

		// Verifica se pacotes MagicSkillCanceld e CASTING_INTERRUPTED foram enviados
		boolean hasCancelPacket = sentPackets1.stream().anyMatch(p -> p instanceof MagicSkillCanceld);
		boolean hasInterruptedMsg = sentPackets1.stream().anyMatch(p -> p instanceof SystemMessage sm
				&& sm.id() == SystemMessage.CASTING_INTERRUPTED);

		assertTrue(hasCancelPacket, "Deve enviar pacote MagicSkillCanceld ao cancelar conjuracao");
		assertTrue(hasInterruptedMsg, "Deve enviar mensagem de sistema CASTING_INTERRUPTED");
	}

	@Test
	@DisplayName("V.10: Protecao contra friendly fire (auto-ataque em aliado de party ou cla sem ctrl)")
	void testFriendlyFireProtection() {
		// 1. Mesmo clã
		Clan clan = clanTable.createClan(leader, "AllyClan");
		clan.addMember(new com.lopez.l2j.game.clan.ClanMember(member1.objectId(), member1.name(), member1.level(), member1.classId(), "", false, 0));
		leader.clanId(clan.clanId());
		member1.clanId(clan.clanId());

		leader.moveTo(0, 0, 0);
		member1.moveTo(30, 0, 0);
		setField(session1, "targetObjectId", member1.objectId());

		// Invoca startAutoAttack(session2) [sem force attack]
		invokeMethod(session1, "startAutoAttack", new Class<?>[]{GameSession.class}, new Object[]{session2});

		// Nao deve ter entrado em auto-ataque
		boolean attacking = (boolean) getField(session1, "autoAttacking");
		assertFalse(attacking, "Auto-ataque em aliado do mesmo cla sem force attack deve ser bloqueado");

		// 2. Mesma Party
		leader.clanId(0);
		member1.clanId(0);
		Party party = new Party(session1, session2, Party.ITEM_LOOTER);
		setField(session1, "party", party);
		setField(session2, "party", party);

		invokeMethod(session1, "startAutoAttack", new Class<?>[]{GameSession.class}, new Object[]{session2});
		attacking = (boolean) getField(session1, "autoAttacking");
		assertFalse(attacking, "Auto-ataque em aliado da mesma party sem force attack deve ser bloqueado");
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

	private static Object getField(Object target, String name) {
		try {
			var f = target.getClass().getDeclaredField(name);
			f.setAccessible(true);
			return f.get(target);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static Object invokeMethod(Object target, String name, Class<?>[] paramTypes, Object[] args) {
		try {
			var m = target.getClass().getDeclaredMethod(name, paramTypes);
			m.setAccessible(true);
			return m.invoke(target, args);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
