package com.lopez.l2j.network.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.drop.DropData;
import com.lopez.l2j.game.drop.DropService;
import com.lopez.l2j.game.drop.DropTable;
import com.lopez.l2j.game.html.HtmCache;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.npc.NpcTemplateTable;
import com.lopez.l2j.game.npc.SpawnService;
import com.lopez.l2j.game.service.CharacterService;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameClientPacket.Action;
import com.lopez.l2j.network.game.packet.GameClientPacket.MoveBackwardToLocation;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestBypassToServer;
import com.lopez.l2j.network.game.packet.GameClientPacket.Say2;
import com.lopez.l2j.network.game.packet.GameClientPacket.ValidatePosition;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.DeleteObject;
import com.lopez.l2j.network.game.packet.GameServerPacket.Die;
import com.lopez.l2j.network.game.packet.GameServerPacket.MyTargetSelected;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameSessionAdminAndNpcTest {

	private List<GameServerPacket> sent;
	private GameSession session;
	private GameSession.Context ctx;
	private PlayerCharacter player;
	private GameWorld world;
	private InventoryService inventoryService;
	private SpawnService spawnService;
	private DropService dropService;
	private NpcInstance npc;
	private NpcTemplate npcTemplate;

	@BeforeEach
	void setUp() {
		sent = new ArrayList<>();
		world = new GameWorld();
		var itemRepo = new com.lopez.l2j.game.item.InMemoryItemRepository();
		inventoryService = new InventoryService(com.lopez.l2j.game.item.TestItems.table(), itemRepo,
				ObjectIdFactory.sequential(0x20000000), 10_000_000);
		var charTemplates = new CharTemplateTable();
		var charRepo = new com.lopez.l2j.game.model.InMemoryCharacterRepository();
		var charService = new CharacterService(charRepo, charTemplates, inventoryService);
		var combat = new CombatService();
		var htmls = new HtmCache("data/html");

		npcTemplate = new NpcTemplate(30008, 30008, "Roien", false, "Grand Master", false, 10.0, 15.0, 70, "male",
				"L2VillageMaster", 100, 100, 20, 10, 30, 5, 15, 2000, 1000, 0, 0, 0, 50, 100, 0, false);
		var templates = NpcTemplateTable.of(List.of(npcTemplate));
		spawnService = new SpawnService(null, templates, world, ObjectIdFactory.sequential(0x50000000));
		DropTable dropTable = new DropTable() {
			@Override
			public List<DropData> getDrops(int mobId) {
				return List.of(new DropData(mobId, 57, 100, 500, 0, 700000));
			}

			@Override
			public int size() {
				return 1;
			}
		};
		dropService = new DropService(dropTable, 1.0, 1.0, 1.0, true);

		ctx = new GameSession.Context(746, 746, null, charService, inventoryService, world, htmls,
				null, null, combat, dropService, null, null, null, null, null, null, null,
				spawnService, List.of("admin", "Admin"), null, "TestServer");

		player = new PlayerCharacter(1001, "AdminHero", "Lopez", 1, 0, 0, 0, 0, 0, false, 0, 0, 0,
				500, 300, 200, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 500.0, 300.0, 200.0);
		player.inventory(new Inventory(player.objectId()));
		player.moveTo(0, 0, 0);

		session = new GameSession(ctx, new byte[8], "127.0.0.1", sent::add);
		setField(session, "state", GameClientPacket.State.IN_GAME);
		setField(session, "active", player);
		setField(session, "inWorld", true);
		setField(session, "account", "admin");
		world.add(session);

		npc = new NpcInstance(50001, npcTemplate, 100, 0, 0, 0);
		world.addNpc(npc);
	}

	@Test
	void testNpcTargetingAndDialogInteraction() {
		// 1st click: selects target
		invoke(session, "onAction", new Action(npc.objectId(), 0, 0, 0, 0));
		assertTrue(sent.stream().anyMatch(p -> p instanceof MyTargetSelected mts && mts.objectId() == npc.objectId()));

		// 2nd click within interact range: shows NPC HTML and rotates player towards NPC
		sent.clear();
		invoke(session, "onAction", new Action(npc.objectId(), 0, 0, 0, 0));
		var htmlPacket = sent.stream().filter(p -> p instanceof NpcHtmlMessage).findFirst();
		assertTrue(htmlPacket.isPresent(), "Dialog HTML should open on 2nd click within range");
	}

	@Test
	void testNpcPendingInteractWhileApproaching() {
		// Player is far away (800 units)
		player.moveTo(800, 0, 0);

		// 1st click: selects target
		invoke(session, "onAction", new Action(npc.objectId(), 800, 0, 0, 0));
		// 2nd click: attempts to talk to NPC from afar, triggers pending interact & approach
		invoke(session, "onAction", new Action(npc.objectId(), 800, 0, 0, 0));
		int pending = (int) getField(session, "pendingNpcInteractObjectId");
		assertEquals(npc.objectId(), pending, "Pending NPC interact should be set when far away");

		// Moving towards NPC should NOT clear pending interact
		invoke(session, "onMove", new MoveBackwardToLocation(100, 0, 0, 800, 0, 0, 1));
		int pendingAfterMove = (int) getField(session, "pendingNpcInteractObjectId");
		assertEquals(npc.objectId(), pendingAfterMove, "Pending NPC interact must be preserved during approach move");

		// Player arrives within range
		sent.clear();
		invoke(session, "onValidatePosition", new ValidatePosition(150, 0, 0, 0));
		assertTrue(sent.stream().anyMatch(p -> p instanceof NpcHtmlMessage), "NPC dialog should trigger when player arrives");
	}

	@Test
	void testNpcQuestBypassFallbackForMaster() {
		// Clicking "Quest" bypass on Grand Master Roien
		invoke(session, "onBypass", new RequestBypassToServer("npc_" + npc.objectId() + "_Quest"));
		var htmlOpt = sent.stream()
				.filter(p -> p instanceof NpcHtmlMessage)
				.map(p -> (NpcHtmlMessage) p)
				.findFirst();
		assertTrue(htmlOpt.isPresent());
		String html = htmlOpt.get().html();
		assertTrue(html.contains("Roien") && html.contains("SkillList"));
	}

	@Test
	void testGmShiftClickInspection() {
		player.accessLevel(100);

		// Shift-Click (actionId == 1) on NPC
		invoke(session, "onAction", new Action(npc.objectId(), 0, 0, 0, 1));
		var htmlOpt = sent.stream()
				.filter(p -> p instanceof NpcHtmlMessage)
				.map(p -> (NpcHtmlMessage) p)
				.findFirst();
		assertTrue(htmlOpt.isPresent(), "Shift-Click as GM should open NPC Admin Inspection Window");
		String html = htmlOpt.get().html();
		assertTrue(html.contains("Roien"));
		assertTrue(html.contains("admin_kill"));
		assertTrue(html.contains("admin_delete"));
		assertTrue(html.contains("admin_heal"));
		assertTrue(html.contains("admin_show_droplist"));
	}

	@Test
	void testAdminDropListCommand() {
		player.accessLevel(100);
		invoke(session, "onSay", new Say2("//droplist 30008", 0, null));
		var htmlOpt = sent.stream()
				.filter(p -> p instanceof NpcHtmlMessage)
				.map(p -> (NpcHtmlMessage) p)
				.findFirst();
		assertTrue(htmlOpt.isPresent(), "//droplist should display drops");
		String html = htmlOpt.get().html();
		assertTrue(html.contains("DropList:") && html.contains("30008"));
	}

	@Test
	void testAdminSpawnAndDeleteCommands() {
		player.accessLevel(100);

		// //spawn 30008
		int initialNpcs = world.npcs().size();
		invoke(session, "onSay", new Say2("//spawn 30008", 0, null));
		assertEquals(initialNpcs + 1, world.npcs().size(), "NPC should be spawned in world");

		var spawnedNpc = world.npcs().stream().filter(n -> n.objectId() != npc.objectId()).findFirst().orElseThrow();
		setField(session, "targetObjectId", spawnedNpc.objectId());

		// //delete
		invoke(session, "onSay", new Say2("//delete", 0, null));
		assertFalse(world.npc(spawnedNpc.objectId()).isPresent(), "NPC should be removed from world");
		assertTrue(sent.stream().anyMatch(p -> p instanceof DeleteObject del && del.objectId() == spawnedNpc.objectId()));
	}

	@Test
	void testAdminKillAndHealCommands() {
		player.accessLevel(100);
		setField(session, "targetObjectId", npc.objectId());

		// //kill
		invoke(session, "onSay", new Say2("//kill", 0, null));
		assertEquals(0.0, npc.currentHp(), 0.01, "NPC HP should be 0 after //kill");
		assertTrue(sent.stream().anyMatch(p -> p instanceof Die die && die.charObjId() == npc.objectId()));

		// //heal
		invoke(session, "onSay", new Say2("//heal", 0, null));
		assertEquals(npc.template().maxHp(), npc.currentHp(), 0.01, "NPC HP should be fully restored after //heal");
	}

	@Test
	void testAdminItemAndLevelCommands() {
		player.accessLevel(100);

		// //item 57 1000000
		invoke(session, "onSay", new Say2("//item 57 1000000", 0, null));
		assertEquals(1000000, player.inventory().adena(), "Adena should be added to inventory");

		// //level 75
		invoke(session, "onSay", new Say2("//level 75", 0, null));
		assertEquals(75, player.level(), "Player level should be set to 75");
	}

	@Test
	void testAdminInvulCommand() {
		player.accessLevel(100);
		assertFalse(player.invul());

		// //invul
		invoke(session, "onSay", new Say2("//invul", 0, null));
		assertTrue(player.invul(), "Invul should be activated");

		// Test damage immunity
		double hpBefore = player.currentHp();
		ctx.combat().applyDamagePlayer(player, 1000);
		assertEquals(hpBefore, player.currentHp(), 0.01, "Player should take 0 damage when invulnerable");

		// //invul again to toggle off
		invoke(session, "onSay", new Say2("//invul", 0, null));
		assertFalse(player.invul(), "Invul should be deactivated");
	}

	@Test
	void testAdminSetParamCommand() {
		player.accessLevel(100);

		// //setparam hp 9999
		invoke(session, "onSay", new Say2("//setparam hp 9999", 0, null));
		assertEquals(9999, player.maxHp());
		assertEquals(9999.0, player.currentHp(), 0.01);
	}

	@Test
	void testAdminEnchantCommand() {
		player.accessLevel(100);

		var weaponTpl = ctx.inventories().templates().get(1).orElse(null); // Short Sword
		if (weaponTpl != null) {
			var added = ctx.inventories().addItem(player.inventory(), 1, 1, "test");
			player.inventory().equip(added.item());
			assertEquals(0, added.item().enchant());

			// //setew 16
			invoke(session, "onSay", new Say2("//setew 16", 0, null));
			assertEquals(16, added.item().enchant(), "Weapon should be enchanted to +16");
		}
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

	private static void invoke(Object target, String name, Object arg) {
		try {
			Method found = null;
			for (var m : target.getClass().getDeclaredMethods()) {
				if (m.getName().equals(name) && m.getParameterCount() == 1
						&& m.getParameterTypes()[0].isAssignableFrom(arg.getClass())) {
					found = m;
					break;
				}
			}
			if (found == null) {
				throw new NoSuchMethodException(name + "(" + arg.getClass().getSimpleName() + ")");
			}
			found.setAccessible(true);
			found.invoke(target, arg);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
