package com.lopez.l2j.game.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.game.item.InMemoryItemRepository;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.TestItems;
import com.lopez.l2j.game.model.InMemoryCharacterRepository;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.service.CharacterService;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.game.zone.ZoneTable;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.MoveToPawn;
import com.lopez.l2j.network.game.packet.GameServerPacket.StatusUpdate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CombatAttackFollowAndTimingTest {

	private PlayerCharacter player1;
	private PlayerCharacter player2;
	private GameSession session1;
	private GameSession session2;
	private List<GameServerPacket> sentPackets1;
	private List<GameServerPacket> sentPackets2;
	private GameWorld world;
	private ZoneTable zoneTable;

	@BeforeEach
	void setUp() {
		world = new GameWorld();
		sentPackets1 = new ArrayList<>();
		sentPackets2 = new ArrayList<>();

		player1 = new PlayerCharacter(0x10000001, "acc1", "HeroAttacker", 20, 0, 0, 0, 0, 0, false, 0, 0, 0,
				1000, 500, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 1000.0, 500.0, 100.0);
		player2 = new PlayerCharacter(0x10000002, "acc2", "TargetPlayer", 20, 0, 0, 0, 0, 0, false, 0, 0, 0,
				1000, 500, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 1000.0, 500.0, 100.0);

		player1.inventory(new Inventory(player1.objectId()));
		player2.inventory(new Inventory(player2.objectId()));

		player1.moveTo(0, 0, 0);
		player2.moveTo(200, 0, 0);

		var repo = new InMemoryItemRepository();
		var inventoryService = new InventoryService(TestItems.table(), repo, ObjectIdFactory.sequential(0x20000000), 10_000_000);
		var charTemplates = new CharTemplateTable();
		var charRepo = new InMemoryCharacterRepository();
		var charService = new CharacterService(charRepo, charTemplates, inventoryService);
		var combat = new CombatService();

		zoneTable = new ZoneTable("data/xml/zone");

		GameSession.Context ctx = new GameSession.Context(746, 746, null, charService, inventoryService, world, null,
				null, null, combat, zoneTable, "TestServer");

		session1 = new GameSession(ctx, new byte[8], "127.0.0.1", sentPackets1::add);
		setField(session1, "state", GameClientPacket.State.IN_GAME);
		setField(session1, "active", player1);
		setField(session1, "inWorld", true);
		world.add(session1);

		session2 = new GameSession(ctx, new byte[8], "127.0.0.1", sentPackets2::add);
		setField(session2, "state", GameClientPacket.State.IN_GAME);
		setField(session2, "active", player2);
		setField(session2, "inWorld", true);
		world.add(session2);
	}

	@Test
	@DisplayName("Ataque em player neutro sem CTRL deve apenas segui-lo (follow) sem entrar em auto-ataque")
	void testAttackPlayerWithoutCtrlFollows() {
		// Player 1 seleciona Player 2
		session1.onAction(new GameClientPacket.Action(player2.objectId(), 0, 0, 0, 0));
		assertEquals(player2.objectId(), session1.targetObjectId());

		// 2º clique de ação de ataque sem ctrl (Action ou AttackRequest com attackId=0)
		session1.onAction(new GameClientPacket.Action(player2.objectId(), 0, 0, 0, 0));

		// Nao deve ter entrado em auto-ataque
		assertFalse((boolean) getField(session1, "autoAttacking"), "Player sem ctrl nao deve auto-atacar alvo pacifico");
		assertEquals(0, player1.pvpFlag(), "Player que segue nao deve receber flag pvp");

		// Deve ter entrado em modo follow e enviado MoveToPawn
		assertTrue(session1.isFollowing(), "Player deve estar seguindo o alvo");
		assertEquals(session2, session1.followingTarget());

		boolean sentMoveToPawn = sentPackets1.stream().anyMatch(p -> p instanceof MoveToPawn);
		assertTrue(sentMoveToPawn, "Deve enviar MoveToPawn ao seguir player");
	}

	@Test
	@DisplayName("Ataque em player neutro com CTRL inicia ataque forçado (PvP flag)")
	void testAttackPlayerWithCtrlAttacks() {
		// Player 1 ataca forçado (attackId = 1 / ctrl = true)
		session1.onAttackRequest(new GameClientPacket.AttackRequest(player2.objectId(), 0, 0, 0, 1));

		assertTrue((boolean) getField(session1, "autoAttacking"), "Player com ctrl deve iniciar auto-ataque");
		assertTrue(player1.pvpFlag() > 0, "Atacante forcado deve receber pvp flag (nome rosa)");
		assertFalse(session1.isFollowing(), "Ataque cancela o modo follow");
	}

	@Test
	@DisplayName("Ataque sem CTRL em player flag (nome rosa) ataca normalmente sem precisar de CTRL")
	void testAttackFlaggedPlayerWithoutCtrlAttacks() {
		// Player 2 esta flag (nome rosa / pvpFlag > 0)
		player2.pvpFlag(1);

		session1.onAttackRequest(new GameClientPacket.AttackRequest(player2.objectId(), 0, 0, 0, 0));

		assertTrue((boolean) getField(session1, "autoAttacking"), "Player com flag ativo pode ser atacado sem ctrl");
	}

	@Test
	@DisplayName("Ataque sem CTRL em player PK (karma > 0) ataca normalmente sem precisar de CTRL")
	void testAttackKarmaPlayerWithoutCtrlAttacks() {
		// Player 2 esta PK (karma > 0)
		player2.karma(500);

		session1.onAttackRequest(new GameClientPacket.AttackRequest(player2.objectId(), 0, 0, 0, 0));

		assertTrue((boolean) getField(session1, "autoAttacking"), "Player com karma pode ser atacado sem ctrl");
	}

	@Test
	@DisplayName("Ataque sem CTRL em Zona de Arena/Combate ataca normalmente sem precisar de CTRL")
	void testAttackInArenaWithoutCtrlAttacks() {
		// Move ambos para dentro da arena (Gludin Arena: x=-88000, y=142000, z=-3500)
		player1.moveTo(-88000, 142000, -3500);
		player2.moveTo(-87980, 142000, -3500);

		session1.onAttackRequest(new GameClientPacket.AttackRequest(player2.objectId(), 0, 0, 0, 0));

		assertTrue((boolean) getField(session1, "autoAttacking"), "Em zona de combate PvP livre, pode atacar sem ctrl");
	}

	@Test
	@DisplayName("Ataque em monstro ataca diretamente sem precisar de CTRL")
	void testAttackMonsterWithoutCtrlAttacksDirectly() {
		NpcTemplate tpl = new NpcTemplate(20001, 20001, "Gremlin", false, "", false, 10.0, 15.0, 1, "male",
				"L2Monster", 40, 100, 50, 10, 10, 10, 10, 253, 333, 0, 0, 0, 50, 100, 0, false);
		NpcInstance mob = new NpcInstance(9999, tpl, 50, 0, 0, 0);
		world.addNpc(mob);

		session1.onAttackRequest(new GameClientPacket.AttackRequest(mob.objectId(), 0, 0, 0, 0));

		assertTrue((boolean) getField(session1, "autoAttacking"), "Monstro deve ser atacado sem necessidade de ctrl");
	}

	@Test
	@DisplayName("Movimentacao cancela o modo follow")
	void testMoveCancelsFollow() {
		session1.startFollow(session2);
		assertTrue(session1.isFollowing());

		// Envia clique no chão para andar
		session1.onMove(new GameClientPacket.MoveBackwardToLocation(50, 50, 0, 0, 0, 0, 1));

		assertFalse(session1.isFollowing(), "Clicar para andar deve parar de seguir o alvo");
	}

	@Test
	@DisplayName("Monstro morto garante HP 0 em todas as consultas e impede receber vida positiva")
	void testNpcInstanceDeathGuaranteesZeroHp() {
		NpcTemplate tpl = new NpcTemplate(20001, 20001, "Gremlin", false, "", false, 10.0, 15.0, 1, "male",
				"L2Monster", 40, 100, 50, 10, 10, 10, 10, 253, 333, 0, 0, 0, 50, 100, 0, false);
		NpcInstance mob = new NpcInstance(8888, tpl, 50, 0, 0, 0);

		assertEquals(100.0, mob.currentHp());
		assertFalse(mob.isDead());

		// Morte do monstro
		mob.dead(true);
		assertTrue(mob.isDead());
		assertEquals(0.0, mob.currentHp(), "Monstro morto deve ter HP estritamente 0");

		// Tentativa de curar ou dar HP para mob morto deve ser ignorada
		mob.currentHp(50.0);
		assertEquals(0.0, mob.currentHp(), "Monstro morto nao pode ter HP positivo");
		assertTrue(mob.isDead());
	}

	@Test
	@DisplayName("Clicar em monstro morto sempre envia StatusUpdate com HP 0 e pacote Die")
	void testDeadNpcTargetAlwaysSendsHpZeroAndDie() {
		NpcTemplate tpl = new NpcTemplate(20001, 20001, "Gremlin", false, "", false, 10.0, 15.0, 1, "male",
				"L2Monster", 40, 100, 50, 10, 10, 10, 10, 253, 333, 0, 0, 0, 50, 100, 0, false);
		NpcInstance mob = new NpcInstance(8889, tpl, 50, 0, 0, 0);
		mob.dead(true);
		world.addNpc(mob);

		sentPackets1.clear();

		// Primeiro clique para selecionar o monstro morto
		session1.onAction(new GameClientPacket.Action(mob.objectId(), 0, 0, 0, 0));

		// Deve conter StatusUpdate com HP 0 e Die
		boolean hasStatusHpZero = sentPackets1.stream()
				.filter(p -> p instanceof StatusUpdate)
				.map(p -> (StatusUpdate) p)
				.anyMatch(su -> su.objectId() == mob.objectId() && su.attributes().stream()
						.anyMatch(a -> a.id() == StatusUpdate.CUR_HP && a.value() == 0));

		boolean hasDiePacket = sentPackets1.stream()
				.anyMatch(p -> p instanceof GameServerPacket.Die die && die.charObjId() == mob.objectId());

		assertTrue(hasStatusHpZero, "Deve enviar StatusUpdate com HP=0 ao selecionar monstro morto");
		assertTrue(hasDiePacket, "Deve enviar Die ao selecionar monstro morto");

		// Segundo clique para tentar atacar monstro morto
		sentPackets1.clear();
		session1.onAction(new GameClientPacket.Action(mob.objectId(), 0, 0, 0, 0));

		hasStatusHpZero = sentPackets1.stream()
				.filter(p -> p instanceof StatusUpdate)
				.map(p -> (StatusUpdate) p)
				.anyMatch(su -> su.objectId() == mob.objectId() && su.attributes().stream()
						.anyMatch(a -> a.id() == StatusUpdate.CUR_HP && a.value() == 0));

		assertTrue(hasStatusHpZero, "Segundo clique em monstro morto tambem deve reforcar HP=0");
	}

	@Test
	@DisplayName("Spam de ataque (F1 / ActionUse / AttackRequest) durante cooldown de ataque rejeita com ActionFailed")
	void testSpamAttackDuringCooldownRejectsWithActionFailed() {
		NpcTemplate tpl = new NpcTemplate(20001, 20001, "Gremlin", false, "", false, 10.0, 15.0, 1, "male",
				"L2Monster", 40, 100, 50, 10, 10, 10, 10, 253, 333, 0, 0, 0, 50, 100, 0, false);
		NpcInstance mob = new NpcInstance(8890, tpl, 50, 0, 0, 0);
		world.addNpc(mob);

		session1.targetObjectId(mob.objectId());

		// Simula ataque em andamento com cooldown ativo por 1.5s
		setField(session1, "attackEndTime", System.currentTimeMillis() + 1500L);
		assertTrue(session1.isAttackingNow(), "Deve indicar que esta ativamente atacando");

		sentPackets1.clear();

		// Spam 1: Pressiona F1 (RequestActionUse com actionId = 2)
		session1.onActionUse(new GameClientPacket.RequestActionUse(2, false, false));
		boolean hasActionFailed1 = sentPackets1.stream().anyMatch(p -> p instanceof GameServerPacket.ActionFailed);
		assertTrue(hasActionFailed1, "Spam de F1 durante ataque deve receber ActionFailed");

		sentPackets1.clear();

		// Spam 2: Envia AttackRequest
		session1.onAttackRequest(new GameClientPacket.AttackRequest(mob.objectId(), 0, 0, 0, 0));
		boolean hasActionFailed2 = sentPackets1.stream().anyMatch(p -> p instanceof GameServerPacket.ActionFailed);
		assertTrue(hasActionFailed2, "Spam de AttackRequest durante ataque deve receber ActionFailed");

		sentPackets1.clear();

		// Spam 3: Clica no mob que ja esta selecionado (Action packet)
		session1.onAction(new GameClientPacket.Action(mob.objectId(), 0, 0, 0, 0));
		boolean hasActionFailed3 = sentPackets1.stream().anyMatch(p -> p instanceof GameServerPacket.ActionFailed);
		assertTrue(hasActionFailed3, "Spam de clique de ataque durante animacao deve receber ActionFailed");
	}

	@Test
	@DisplayName("stopAutoAttack cancela qualquer playerAutoAttackTask agendada")
	void testStopAutoAttackCancelsPendingTask() {
		NpcTemplate tpl = new NpcTemplate(20001, 20001, "Gremlin", false, "", false, 10.0, 15.0, 1, "male",
				"L2Monster", 40, 100, 50, 10, 10, 10, 10, 253, 333, 0, 0, 0, 50, 100, 0, false);
		NpcInstance mob = new NpcInstance(8891, tpl, 50, 0, 0, 0);
		world.addNpc(mob);

		setField(session1, "autoAttacking", true);
		session1.targetObjectId(mob.objectId());

		// Inicia agendamento via reflexão ou metodo
		try {
			var m = GameSession.class.getDeclaredMethod("schedulePlayerAutoAttack", NpcInstance.class, long.class);
			m.setAccessible(true);
			m.invoke(session1, mob, 10_000L);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		Object task = getField(session1, "playerAutoAttackTask");
		org.junit.jupiter.api.Assertions.assertNotNull(task, "Task deve estar agendada");

		// Cancela auto ataque
		session1.stopAutoAttack();

		Object cancelledTask = getField(session1, "playerAutoAttackTask");
		org.junit.jupiter.api.Assertions.assertNull(cancelledTask, "playerAutoAttackTask deve ser anulada ao parar auto-ataque");
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
}
