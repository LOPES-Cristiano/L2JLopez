package com.lopez.l2j.game.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.html.HtmCache;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.model.ExperienceTable;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.skill.SkillService;
import com.lopez.l2j.game.skill.SkillTreeTable;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClassMasterPopupTest {

	private boolean originalClassMaster;
	private boolean originalAltClassMaster;
	private boolean originalPopupWindow;

	private List<GameServerPacket> sent;
	private GameSession session;
	private PlayerCharacter player;

	@BeforeEach
	void setUp() {
		originalClassMaster = Config.CLASS_MASTER;
		originalAltClassMaster = Config.ALT_CLASS_MASTER;
		originalPopupWindow = Config.CLASS_MASTER_POPUP_WINDOW;

		Config.CLASS_MASTER = true;
		Config.ALT_CLASS_MASTER = true;
		Config.CLASS_MASTER_POPUP_WINDOW = true;

		sent = new ArrayList<>();
		player = new PlayerCharacter(0x10000001, "TestFighter", "Hero", 0, 0, 0, 0, 0, 0, false, 0, 0, 0,
				100, 50, 50, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100.0, 50.0, 50.0);
		player.inventory(new Inventory(player.objectId()));
		player.level(19);
		player.exp(ExperienceTable.expForLevel(19));

		var charTemplates = new CharTemplateTable();
		var repo = new com.lopez.l2j.game.item.InMemoryItemRepository();
		var invService = new InventoryService(com.lopez.l2j.game.item.TestItems.table(), repo,
				com.lopez.l2j.game.model.ObjectIdFactory.sequential(0x20000000), 10_000_000);
		var charRepo = new com.lopez.l2j.game.model.InMemoryCharacterRepository();
		var charService = new CharacterService(charRepo, charTemplates, invService);
		var world = new GameWorld();
		var htmls = new HtmCache("data/html");
		var trees = new SkillTreeTable(java.nio.file.Path.of("data/xml/player/skilltree"));
		var skillService = new SkillService(null, trees, null, false, 0, true);

		var ctx = new GameSession.Context(746, 746, null, charService, invService, world, htmls,
				null, null, null, null, null, null, null, skillService, null, null, null, null,
				List.of(), null, null, null, null, null, null, null, null, null, null, null, null,
				null, "TestServer");

		session = new GameSession(ctx, new byte[8], "127.0.0.1", sent::add);
		setField(session, "state", GameClientPacket.State.IN_GAME);
		setField(session, "active", player);
		setField(session, "inWorld", true);
		world.add(session);
	}

	@AfterEach
	void tearDown() {
		Config.CLASS_MASTER = originalClassMaster;
		Config.ALT_CLASS_MASTER = originalAltClassMaster;
		Config.CLASS_MASTER_POPUP_WINDOW = originalPopupWindow;
	}

	@Test
	@DisplayName("Ao atingir level 20, Class Master popup deve ser enviado automaticamente")
	void popupSentOnLevel20Reached() {
		long expForLv20 = ExperienceTable.expForLevel(20);
		long needed = expForLv20 - player.exp();

		session.applyExpAndSp(needed, 1000);

		boolean popupFound = sent.stream().anyMatch(p -> p instanceof NpcHtmlMessage html && (
				html.html().contains("change_class") || html.html().contains("Class Master")
		));

		assertTrue(popupFound, "Popup do Class Master deve ser enviado ao subir para level 20");
	}

	@Test
	@DisplayName("Se ClassMasterPopupWindow=False, popup nao deve ser enviado")
	void popupNotSentWhenDisabled() {
		Config.CLASS_MASTER_POPUP_WINDOW = false;

		long expForLv20 = ExperienceTable.expForLevel(20);
		long needed = expForLv20 - player.exp();

		session.applyExpAndSp(needed, 1000);

		boolean popupFound = sent.stream().anyMatch(p -> p instanceof NpcHtmlMessage html && (
				html.html().contains("change_class") || html.html().contains("Class Master")
		));

		assertFalse(popupFound, "Popup nao deve ser enviado se a configuracao estiver desativada");
	}

	@Test
	@DisplayName("Player no nivel 20 ao checar popup de troca de classe pendente deve receber o HTML")
	void popupSentOnCheckIfEligible() {
		player.level(20);
		player.exp(ExperienceTable.expForLevel(20));

		session.checkClassMasterPopup();

		boolean popupFound = sent.stream().anyMatch(p -> p instanceof NpcHtmlMessage html && (
				html.html().contains("change_class") || html.html().contains("Class Master")
		));

		assertTrue(popupFound, "Player elegivel para 1st class deve receber o popup");
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
