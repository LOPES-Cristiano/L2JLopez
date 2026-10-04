package com.lopez.l2j.network.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.item.ItemTemplateTable;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.ActionFailed;
import com.lopez.l2j.network.game.packet.GameServerPacket.Attack;
import com.lopez.l2j.network.game.packet.GameServerPacket.ChangeWaitType;
import com.lopez.l2j.network.game.packet.GameServerPacket.MyTargetSelected;
import com.lopez.l2j.network.game.packet.GameServerPacket.SetupGauge;
import com.lopez.l2j.network.game.packet.GameServerPacket.ShowMiniMap;
import com.lopez.l2j.network.game.packet.GameServerPacket.SocialAction;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameSessionFeaturesTest {

	private List<GameServerPacket> sent;
	private GameSession session;
	private PlayerCharacter player;
	private GameWorld world;
	private ItemTemplateTable itemTemplates;
	private InventoryService inventoryService;
	private NpcInstance monster;

	@BeforeEach
	void setUp() {
		sent = new ArrayList<>();
		world = new GameWorld();
		var repo = new com.lopez.l2j.game.item.InMemoryItemRepository();
		inventoryService = new InventoryService(com.lopez.l2j.game.item.TestItems.table(), repo,
				com.lopez.l2j.game.model.ObjectIdFactory.sequential(0x20000000), 10_000_000);
		var charTemplates = new CharTemplateTable();
		var charService = new com.lopez.l2j.game.service.CharacterService(null, charTemplates, inventoryService);
		var combat = new CombatService();

		var ctx = new GameSession.Context(746, 746, null, charService, inventoryService, world, null,
				null, null, combat, null, null, null, null, null, null, "TestServer");

		player = new PlayerCharacter(1001, "Archer", "Hero", 1, 0, 0, 0, 0, 0, false, 0, 0, 0,
				200, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 200.0, 100.0, 100.0);
		player.inventory(new Inventory(player.objectId()));
		player.moveTo(0, 0, 0);

		session = new GameSession(ctx, new byte[8], "127.0.0.1", sent::add);
		setField(session, "state", GameClientPacket.State.IN_GAME);
		setField(session, "active", player);
		setField(session, "inWorld", true);
		world.add(session);

		var monsterTemplate = new NpcTemplate(20001, 20001, "Gremlin", false, "", false, 10.0, 15.0, 1, "male",
				"L2Monster", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		monster = new NpcInstance(30001, monsterTemplate, 30, 0, 0, 0);
		world.addNpc(monster);
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
	void bowRequiresArrowsAndFailsWhenMissing() {
		// Equip Bow (Bow: id 14, subType bow, crystalType none)
		var bowInst = inventoryService.addItem(player.inventory(), 14, 1, "Test").item();
		inventoryService.toggleEquip(player.inventory(), bowInst.objectId());

		// Tenta atacar monstro sem flechas
		invokeMethod(session, "onAttackNpc", new Class<?>[] { NpcInstance.class }, monster);

		// Deve conter mensagem SystemMessage 726 (NOT_ENOUGH_ARROWS) e ActionFailed
		boolean hasNotEnoughArrows = sent.stream().anyMatch(p -> p instanceof SystemMessage sm && sm.id() == SystemMessage.NOT_ENOUGH_ARROWS);
		assertTrue(hasNotEnoughArrows, "Deve recusar o ataque por falta de flechas");
	}

	@Test
	void bowConsumesArrowWhenAttacking() {
		// Equip Bow (Bow id 14)
		var bowInst = inventoryService.addItem(player.inventory(), 14, 1, "Test").item();
		inventoryService.toggleEquip(player.inventory(), bowInst.objectId());

		// Adiciona 10 Wooden Arrows (id 17)
		inventoryService.addItem(player.inventory(), 17, 10, "Test");
		assertEquals(10, player.inventory().byItemId(17).orElseThrow().count());

		// Ataca o monstro
		invokeMethod(session, "onAttackNpc", new Class<?>[] { NpcInstance.class }, monster);

		// Deve ter consumido 1 flecha
		assertEquals(9, player.inventory().byItemId(17).orElseThrow().count());

		// E deve ter enviado o pacote Attack (animacao)
		assertTrue(sent.stream().anyMatch(p -> p instanceof Attack), "Deve enviar o pacote Attack");
	}

	@Test
	void attackCooldownRejectsSpamClicks() {
		// Adiciona adaga (Dagger id 10)
		var dagger = inventoryService.addItem(player.inventory(), 10, 1, "Test").item();
		inventoryService.toggleEquip(player.inventory(), dagger.objectId());

		// 1º ataque: aceito e dispara Attack
		invokeMethod(session, "onAttackNpc", new Class<?>[] { NpcInstance.class }, monster);
		int attacksCount1 = (int) sent.stream().filter(p -> p instanceof Attack).count();
		assertEquals(1, attacksCount1);

		// 2º ataque imediato: dentro do cooldown do swing, deve falhar (ActionFailed) e NAO gerar novo Attack
		invokeMethod(session, "onAttackNpc", new Class<?>[] { NpcInstance.class }, monster);
		int attacksCount2 = (int) sent.stream().filter(p -> p instanceof Attack).count();
		assertEquals(1, attacksCount2, "Spam clicks nao devem disparar novo Attack dentro do cooldown");
		assertTrue(sent.stream().anyMatch(p -> p instanceof ActionFailed), "Deve responder ActionFailed no spam click");
	}

	@Test
	void actionTwoExecutesAttack() {
		// Seleciona monstro como alvo
		setField(session, "targetObjectId", monster.objectId());

		// Dispara Action 2 (Attack)
		session.handle(new byte[] { 0x45, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 }); // RequestActionUse(2, false, false)

		// Deve ter iniciado o ataque
		assertTrue(sent.stream().anyMatch(p -> p instanceof Attack), "Acao 2 deve iniciar o ataque ao alvo selecionado");
	}

	@Test
	void scrollOfEscapeShowsCastGaugeAndConsumesScroll() {
		// Adiciona Scroll of Escape (id 736)
		var soe = inventoryService.addItem(player.inventory(), 736, 1, "Test").item();

		// Usa o item
		session.handle(new byte[] { 0x14, (byte) soe.objectId(), (byte) (soe.objectId() >> 8), (byte) (soe.objectId() >> 16), (byte) (soe.objectId() >> 24) });

		// Deve ter consumido o scroll
		assertTrue(player.inventory().byItemId(736).isEmpty(), "Scroll deve ter sido consumido");

		// Deve ter enviado SetupGauge da barra de conjuracao
		assertTrue(sent.stream().anyMatch(p -> p instanceof SetupGauge), "Deve exibir a barra de conjuracao");
	}

	@Test
	void socialActionSendsSocialPacket() {
		// Envia opcode 0x1b com actionId 8 (Danca)
		session.handle(new byte[] { 0x1b, 0x08, 0x00, 0x00, 0x00 });

		assertTrue(sent.stream().anyMatch(p -> p instanceof SocialAction sa && sa.actionId() == 8),
				"Deve emitir pacote SocialAction correspondente a acao social clicada");
	}

	@Test
	void sitStandTogglesSittingAndSendsChangeWaitType() {
		assertFalse(player.sitting());

		// Envia Action 0 (Sit)
		session.handle(new byte[] { 0x45, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 });
		assertTrue(player.sitting(), "Personagem deve estar sentado");
		assertTrue(sent.stream().anyMatch(p -> p instanceof ChangeWaitType cwt && cwt.type() == 0),
				"Deve enviar ChangeWaitType tipo 0 (sentado)");

		// Envia Action 0 (Stand)
		session.handle(new byte[] { 0x45, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 });
		assertFalse(player.sitting(), "Personagem deve estar em pe");
		assertTrue(sent.stream().anyMatch(p -> p instanceof ChangeWaitType cwt && cwt.type() == 1),
				"Deve enviar ChangeWaitType tipo 1 (em pe)");
	}

	@Test
	void nextTargetSelectsNearestAttackableMob() {
		// Dispara Action 4 (Target Next)
		session.handle(new byte[] { 0x45, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 });

		assertTrue(sent.stream().anyMatch(p -> p instanceof MyTargetSelected mts && mts.objectId() == monster.objectId()),
				"Target Next deve selecionar o monstro mais proximo");
	}

	@Test
	void altMRequestShowMiniMapSendsShowMiniMap() {
		// Envia opcode 0xcd (RequestShowMiniMap - disparado ao apertar Alt+M ou clicar no icone de mapa)
		session.handle(new byte[] { (byte) 0xcd });

		assertTrue(sent.stream().anyMatch(p -> p instanceof ShowMiniMap),
				"Pressionar Alt+M ou clicar no mapa deve enviar pacote ShowMiniMap (0x9d) para abrir a janela de mapa mundi");
	}

	private static void invokeMethod(Object target, String name, Class<?>[] paramTypes, Object... args) {
		try {
			var m = target.getClass().getDeclaredMethod(name, paramTypes);
			m.setAccessible(true);
			m.invoke(target, args);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
