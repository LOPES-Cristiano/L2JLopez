package com.lopez.l2j.network.game.handler.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AdminPanelHtmlAndCommandTest {

	@Test
	@DisplayName("AdminGeneralHandler must register kill_menu and handle it")
	void testAdminGeneralHandlerCommands() {
		AdminGeneralHandler handler = new AdminGeneralHandler();
		assertTrue(handler.getAdminCommandList().contains("kill_menu"));
		assertTrue(handler.getAdminCommandList().contains("kill"));
		assertTrue(handler.getAdminCommandList().contains("heal"));
		assertTrue(handler.getAdminCommandList().contains("delete"));
	}

	@Test
	@DisplayName("Admin HTML panels exist, are width 260 and have proper L2jFrozen navigation bypasses")
	void testAdminHtmlFiles() throws IOException {
		Path adminDir = Path.of("data", "html", "admin");
		Path menusDir = adminDir.resolve("menus");

		String[] menuFiles = {"main.htm", "game.htm", "effects.htm", "server.htm", "mod.htm"};
		for (String menu : menuFiles) {
			Path p = menusDir.resolve(menu);
			assertTrue(Files.exists(p), "Menu file " + menu + " must exist");
			String content = Files.readString(p);
			assertTrue(content.contains("width=260"), menu + " must use width 260 table layout");
			assertTrue(content.contains("admin_admin"), menu + " must have Main bypass");
			assertTrue(content.contains("admin_admin2"), menu + " must have Game bypass");
			assertTrue(content.contains("admin_admin3"), menu + " must have Effects bypass");
			assertTrue(content.contains("admin_admin4"), menu + " must have Server bypass");
			assertTrue(content.contains("admin_admin5"), menu + " must have Mods bypass");
			assertTrue(content.contains("menu_command"), menu + " must have QuickBox input menu_command");
		}

		Path mainHtm = menusDir.resolve("main.htm");
		String mainContent = Files.readString(mainHtm);
		assertTrue(mainContent.contains("admin_search $menu_command"), "main.htm must link to admin_search");
		assertTrue(mainContent.contains("admin_find_npc $menu_command"), "main.htm must link to admin_find_npc");
		assertTrue(mainContent.contains("admin_find_item $menu_command"), "main.htm must link to admin_find_item");
		assertTrue(mainContent.contains("admin_find_skill $menu_command"), "main.htm must link to admin_find_skill");
		assertTrue(mainContent.contains("admin_find_character $menu_command"), "main.htm must link to admin_find_character");

		Path rootAdmin = adminDir.resolve("admin.htm");
		assertTrue(Files.exists(rootAdmin), "data/html/admin/admin.htm must exist");
		String adminContent = Files.readString(rootAdmin);
		assertTrue(adminContent.contains("width=260"));
		assertTrue(adminContent.contains("admin_admin"));
	}

	@Test
	@DisplayName("Standard L2jFrozen submenus and directories exist and have proper links")
	void testSubmenusAndDirectories() throws IOException {
		Path adminDir = Path.of("data", "html", "admin");

		String[] keySubmenus = {
				"charclasses.htm", "charedit.htm", "charinfo.htm", "charlist.htm",
				"charmanage.htm", "charskills.htm", "itemcreation.htm", "enchant.htm",
				"gmshops.htm", "skills.htm", "spawn.htm", "spawns.htm", "teleports.htm",
				"announce.htm", "social.htm", "abnormal.htm", "shutdown.htm", "move.htm",
				"accountinfo.htm", "ipfind.htm", "cwinfo.htm"
		};

		for (String sub : keySubmenus) {
			Path p = adminDir.resolve(sub);
			assertTrue(Files.exists(p), "Standard submenu " + sub + " must exist in data/html/admin/");
			String content = Files.readString(p);
			assertFalse(content.isBlank(), sub + " content must not be blank");
		}

		// Check directory structure
		assertTrue(Files.isDirectory(adminDir.resolve("tele")), "admin/tele directory must exist");
		assertTrue(Files.isDirectory(adminDir.resolve("tele/others")), "admin/tele/others directory must exist");
		assertTrue(Files.isDirectory(adminDir.resolve("tele/locations")), "admin/tele/locations directory must exist");
		assertTrue(Files.isDirectory(adminDir.resolve("gmshop")), "admin/gmshop directory must exist");
		assertTrue(Files.isDirectory(adminDir.resolve("skills")), "admin/skills directory must exist");
		assertTrue(Files.isDirectory(adminDir.resolve("help")), "admin/help directory must exist");

		// Check specific functional elements
		String charclasses = Files.readString(adminDir.resolve("charclasses.htm"));
		assertTrue(charclasses.contains("admin_setclass 88"), "Duelist class must exist in charclasses");
		assertTrue(charclasses.contains("admin_current_player"), "Back button must link to admin_current_player");

		String enchant = Files.readString(adminDir.resolve("enchant.htm"));
		assertTrue(enchant.contains("admin_setew"), "Enchant must have weapon button");
		assertTrue(enchant.contains("admin_setec"), "Enchant must have chest button");

		String itemcreate = Files.readString(adminDir.resolve("itemcreation.htm"));
		assertTrue(itemcreate.contains("admin_create_item"), "Itemcreation must have create button");
		assertTrue(itemcreate.contains("admin_clear_inventory"), "Itemcreation must have clear inventory button");

		String spawn = Files.readString(adminDir.resolve("spawn.htm"));
		assertTrue(spawn.contains("admin_show_npcs"), "Spawn must link to show_npcs");
		assertTrue(spawn.contains("admin_spawn_reload"), "Spawn must have reload button");
	}
}
