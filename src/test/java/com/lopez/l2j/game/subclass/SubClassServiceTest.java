package com.lopez.l2j.game.subclass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.game.model.PlayerCharacter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SubClassServiceTest {

	private SubClassService subClassService;

	@BeforeEach
	void setUp() {
		com.lopez.l2j.config.Config.ALT_SUBCLASS_WITHOUT_QUESTS = true;
		com.lopez.l2j.config.Config.MAX_SUBCLASSES = 3;
		subClassService = new SubClassService(null);
	}

	@Test
	@DisplayName("getSecondClassId deve mapear 1st, 2nd e 3rd classes para a respectiva 2nd class base")
	void testGetSecondClassIdMapping() {
		// Human Fighter -> Gladiator (2), Warlord (3), Paladin (5), Dark Avenger (6)
		assertEquals(2, subClassService.getSecondClassId(1)); // Warrior -> Gladiator
		assertEquals(2, subClassService.getSecondClassId(2)); // Gladiator
		assertEquals(2, subClassService.getSecondClassId(88)); // Duelist -> Gladiator

		assertEquals(3, subClassService.getSecondClassId(3)); // Warlord
		assertEquals(3, subClassService.getSecondClassId(89)); // Dreadnought -> Warlord

		assertEquals(5, subClassService.getSecondClassId(4)); // Human Knight -> Paladin
		assertEquals(5, subClassService.getSecondClassId(5)); // Paladin
		assertEquals(5, subClassService.getSecondClassId(90)); // Phoenix Knight -> Paladin

		assertEquals(6, subClassService.getSecondClassId(6)); // Dark Avenger
		assertEquals(6, subClassService.getSecondClassId(91)); // Hell Knight -> Dark Avenger

		// Human Mystic -> Sorcerer (12), Necromancer (13), Warlock (14), Bishop (16), Prophet (17)
		assertEquals(12, subClassService.getSecondClassId(10)); // Human Mystic -> Sorcerer
		assertEquals(12, subClassService.getSecondClassId(11)); // Human Wizard -> Sorcerer
		assertEquals(12, subClassService.getSecondClassId(12)); // Sorcerer
		assertEquals(12, subClassService.getSecondClassId(94)); // Archmage -> Sorcerer

		assertEquals(13, subClassService.getSecondClassId(13)); // Necromancer
		assertEquals(13, subClassService.getSecondClassId(95)); // Soultaker -> Necromancer

		// Elven Fighter -> Temple Knight (20), Swordsinger (21), Plains Walker (23), Silver Ranger (24)
		assertEquals(20, subClassService.getSecondClassId(19)); // Elven Knight -> Temple Knight
		assertEquals(20, subClassService.getSecondClassId(20)); // Temple Knight
		assertEquals(20, subClassService.getSecondClassId(99)); // Eva's Templar -> Temple Knight

		// Dark Elf Fighter -> Shillien Knight (33), Bladedancer (34), Abyss Walker (36), Phantom Ranger (37)
		assertEquals(33, subClassService.getSecondClassId(32)); // Palus Knight -> Shillien Knight
		assertEquals(33, subClassService.getSecondClassId(33)); // Shillien Knight
		assertEquals(33, subClassService.getSecondClassId(106)); // Shillien Templar -> Shillien Knight
	}

	@Test
	@DisplayName("canAddSubClass deve exigir nivel 75 para players comuns e permitir bypass para GM")
	void testCanAddSubClassLevelRequirements() {
		// Player Lv 74
		PlayerCharacter playerLv74 = new PlayerCharacter(1001, "Player74", "Hero", 74, 0, 0, 0, 0, 0, false, 0, 0, 0,
				100, 50, 50, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100.0, 50.0, 50.0);
		assertFalse(subClassService.canAddSubClass(playerLv74));

		// Player Lv 75
		PlayerCharacter playerLv75 = new PlayerCharacter(1002, "Player75", "Hero", 75, 0, 0, 0, 0, 0, false, 0, 0, 0,
				100, 50, 50, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100.0, 50.0, 50.0);
		assertTrue(subClassService.canAddSubClass(playerLv75));

		// GM Lv 1
		PlayerCharacter gmLv1 = new PlayerCharacter(1003, "AdminGM", "Admin", 1, 0, 0, 0, 0, 0, false, 0, 0, 0,
				100, 50, 50, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100.0, 50.0, 50.0);
		gmLv1.accessLevel(100);
		assertTrue(subClassService.canAddSubClass(gmLv1));
	}

	@Test
	@DisplayName("canAddSubClass nao deve permitir mais de 3 subclasses")
	void testMaxSubclassesLimit() {
		PlayerCharacter player = new PlayerCharacter(1004, "MaxSub", "Hero", 75, 0, 0, 0, 0, 0, false, 0, 0, 0,
				100, 50, 50, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100.0, 50.0, 50.0);

		player.subClasses().put(1, new SubClass(2, 0, 0, 75, 1));
		player.subClasses().put(2, new SubClass(8, 0, 0, 75, 2));
		assertTrue(subClassService.canAddSubClass(player));

		player.subClasses().put(3, new SubClass(12, 0, 0, 75, 3));
		assertFalse(subClassService.canAddSubClass(player));
	}

	@Test
	@DisplayName("Elfo da Luz nao pode escolher classes de Elfo Negro e vice-versa")
	void testRaceRestrictionsLightAndDarkElf() {
		// Light Elf (race 1, base class 21 = Swordsinger)
		PlayerCharacter lightElf = new PlayerCharacter(1005, "LightElf", "Hero", 75, 0, 0,
				SubClassService.RACE_LIGHT_ELF, 21, 21, false, 0, 0, 0, 100, 50, 50, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100.0, 50.0, 50.0);

		List<Integer> lightElfSubs = subClassService.getAvailableSubClasses(lightElf);
		// Dark Elf classes (33, 34, 36, 37, 40, 41, 43) não podem estar na lista
		assertFalse(lightElfSubs.contains(33)); // Shillien Knight
		assertFalse(lightElfSubs.contains(34)); // Bladedancer
		assertFalse(lightElfSubs.contains(36)); // Abyss Walker
		assertFalse(lightElfSubs.contains(40)); // Spellhowler

		// Dark Elf (race 2, base class 34 = Bladedancer)
		PlayerCharacter darkElf = new PlayerCharacter(1006, "DarkElf", "Hero", 75, 0, 0,
				SubClassService.RACE_DARK_ELF, 34, 34, false, 0, 0, 0, 100, 50, 50, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100.0, 50.0, 50.0);

		List<Integer> darkElfSubs = subClassService.getAvailableSubClasses(darkElf);
		// Light Elf classes (20, 21, 23, 24, 27, 28, 30) não podem estar na lista
		assertFalse(darkElfSubs.contains(20)); // Temple Knight
		assertFalse(darkElfSubs.contains(21)); // Swordsinger
		assertFalse(darkElfSubs.contains(23)); // Plains Walker
		assertFalse(darkElfSubs.contains(27)); // Spellsinger
	}

	@Test
	@DisplayName("Grupos de exclusao mutua de classes devem ser respeitados")
	void testExclusionGroups() {
		// Paladin (5) é Tank -> Grupo: Paladin (5), Dark Avenger (6), Temple Knight (20), Shillien Knight (33)
		PlayerCharacter paladin = new PlayerCharacter(1007, "SirPaladin", "Hero", 75, 0, 0,
				SubClassService.RACE_HUMAN, 5, 5, false, 0, 0, 0, 100, 50, 50, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100.0, 50.0, 50.0);

		List<Integer> paladinSubs = subClassService.getAvailableSubClasses(paladin);
		assertFalse(paladinSubs.contains(5)); // Própria classe
		assertFalse(paladinSubs.contains(6)); // Dark Avenger (mesmo grupo tank)
		assertFalse(paladinSubs.contains(20)); // Temple Knight (mesmo grupo tank)
		assertFalse(paladinSubs.contains(33)); // Shillien Knight (mesmo grupo tank)
		assertTrue(paladinSubs.contains(2)); // Gladiator permitido
		assertTrue(paladinSubs.contains(8)); // Treasure Hunter permitido
		assertTrue(paladinSubs.contains(12)); // Sorcerer permitido

		// Gladiator (2) e Warlord (3) são mutuamente exclusivos
		PlayerCharacter gladiator = new PlayerCharacter(1008, "DualMaster", "Hero", 75, 0, 0,
				SubClassService.RACE_HUMAN, 2, 2, false, 0, 0, 0, 100, 50, 50, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100.0, 50.0, 50.0);

		List<Integer> gladSubs = subClassService.getAvailableSubClasses(gladiator);
		assertFalse(gladSubs.contains(2)); // Própria classe
		assertFalse(gladSubs.contains(3)); // Warlord (mesmo grupo)
	}

	@Test
	@DisplayName("Validação de requisitos configuráveis de Subclasse (sem quest, com itens ou com quest)")
	void testSubclassQuestAndItemConfigurations() {
		PlayerCharacter player = new PlayerCharacter(1009, "QuestTester", "Hero", 75, 0, 0,
				SubClassService.RACE_HUMAN, 2, 2, false, 0, 0, 0, 100, 50, 50, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100.0, 50.0, 50.0);
		player.inventory(new com.lopez.l2j.game.item.Inventory(player.objectId()));

		// 1. AltSubClassWithoutQuests = true -> liberado direto
		com.lopez.l2j.config.Config.ALT_SUBCLASS_WITHOUT_QUESTS = true;
		assertTrue(subClassService.canAddSubClass(player));

		// 2. AltSubClassWithoutQuests = false e sem itens/quest -> bloqueado
		com.lopez.l2j.config.Config.ALT_SUBCLASS_WITHOUT_QUESTS = false;
		com.lopez.l2j.config.Config.SUBCLASS_WITH_ITEM_AND_NO_QUEST = false;
		com.lopez.l2j.config.Config.SUBCLASS_WITH_CUSTOM_ITEM = false;
		assertFalse(subClassService.canAddSubClass(player));

		// 3. SubclassWithItemAndNoQuest = true -> precisa de 5011 (Destiny) e 5904 (Mimir)
		com.lopez.l2j.config.Config.SUBCLASS_WITH_ITEM_AND_NO_QUEST = true;
		assertFalse(subClassService.canAddSubClass(player)); // Sem os itens ainda

		var destinyTpl = com.lopez.l2j.game.item.ItemTemplate.etc(5011, 5011, "Star of Destiny", "quest", "normal", 0, "none", 0, java.util.List.of(), true, true, true, true);
		var elixirTpl = com.lopez.l2j.game.item.ItemTemplate.etc(5904, 5904, "Mimir's Elixir", "quest", "normal", 0, "none", 0, java.util.List.of(), true, true, true, true);
		player.inventory().add(new com.lopez.l2j.game.item.ItemInstance(9001, destinyTpl, player.objectId(), 1));
		assertFalse(subClassService.canAddSubClass(player)); // Só tem Destiny

		player.inventory().add(new com.lopez.l2j.game.item.ItemInstance(9002, elixirTpl, player.objectId(), 1));
		assertTrue(subClassService.canAddSubClass(player)); // Agora possui ambos

		// 4. SubclassWithCustomItem
		com.lopez.l2j.config.Config.SUBCLASS_WITH_ITEM_AND_NO_QUEST = false;
		com.lopez.l2j.config.Config.SUBCLASS_WITH_CUSTOM_ITEM = true;
		com.lopez.l2j.config.Config.SUBCLASS_WITH_CUSTOM_ITEM_ID = 57;
		com.lopez.l2j.config.Config.SUBCLASS_WITH_CUSTOM_ITEM_COUNT = 1000000;

		assertFalse(subClassService.canAddSubClass(player)); // Não tem 1M de adena

		var adenaTpl = com.lopez.l2j.game.item.ItemTemplate.etc(57, 57, "Adena", "none", "asset", 0, "none", 1, java.util.List.of(), true, true, true, true);
		player.inventory().add(new com.lopez.l2j.game.item.ItemInstance(9003, adenaTpl, player.objectId(), 1000000));
		assertTrue(subClassService.canAddSubClass(player));
	}

	@Test
	@DisplayName("Configuração dinâmica de MaxSubClasses e níveis de Subclasse/Player")
	void testDynamicMaxSubclassesAndLevelLimits() {
		com.lopez.l2j.config.Config.ALT_SUBCLASS_WITHOUT_QUESTS = true;
		com.lopez.l2j.config.Config.MAX_SUBCLASSES = 4;
		com.lopez.l2j.config.Config.SUBCLASS_MAX_LEVEL = 80;
		com.lopez.l2j.config.Config.PLAYER_MAX_LEVEL = 81;
		com.lopez.l2j.config.Config.SUBCLASS_INIT_LEVEL = 40;

		PlayerCharacter player = new PlayerCharacter(1010, "FourSubs", "Hero", 75, 0, 0,
				SubClassService.RACE_HUMAN, 2, 2, false, 0, 0, 0, 100, 50, 50, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100.0, 50.0, 50.0);

		player.subClasses().put(1, new SubClass(5, 0, 0, 75, 1));
		player.subClasses().put(2, new SubClass(8, 0, 0, 75, 2));
		player.subClasses().put(3, new SubClass(12, 0, 0, 75, 3));

		// Com MaxSubClasses = 4, tendo 3 ele ainda pode adicionar a 4ª
		assertTrue(subClassService.canAddSubClass(player));

		player.subClasses().put(4, new SubClass(14, 0, 0, 75, 4));
		assertFalse(subClassService.canAddSubClass(player));

		assertEquals(80, SubClassService.getMaxLevel(true));
		assertEquals(81, SubClassService.getMaxLevel(false));
		assertEquals(40, SubClassService.getInitialLevel());
	}
}
