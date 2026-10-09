package com.lopez.l2j.game.service;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.skill.SkillService;
import com.lopez.l2j.network.game.GameSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SchemeBufferServiceTest {

	private SchemeBufferService bufferService;
	private CharacterVariablesService variablesService;
	private SkillService skillService;
	private PlayerCharacter player;
	private GameSession session;
	private Inventory inventory;

	@BeforeEach
	void setUp() {
		variablesService = mock(CharacterVariablesService.class);
		skillService = mock(SkillService.class);
		bufferService = new SchemeBufferService(variablesService, skillService);

		player = new PlayerCharacter(
				2001, "acc", "BufferHero", 75, 0, 0, 0,
				0, 0, false, 0, 0, 0,
				2000, 1500, 1000, 0, 0, 0, 0,
				"", 0, 0, 0, 0, 0, 0, 0,
				500.0, 300.0, 200.0);
		inventory = mock(Inventory.class);
		player.inventory(inventory);

		session = mock(GameSession.class);
		when(session.character()).thenReturn(player);
		when(session.activeCharacter()).thenReturn(player);
	}

	@Test
	@DisplayName("Esquema padrao deve conter lista de buffs de guerreiro e mago")
	void testDefaultSchemes() {
		List<Integer> fighter = bufferService.getScheme(player.objectId(), 1);
		assertNotNull(fighter);
		assertFalse(fighter.isEmpty());
		assertTrue(fighter.contains(1068), "Fighter scheme deve conter Might (1068)");
		assertTrue(fighter.contains(1086), "Fighter scheme deve conter Haste (1086)");

		List<Integer> mage = bufferService.getScheme(player.objectId(), 2);
		assertNotNull(mage);
		assertFalse(mage.isEmpty());
		assertTrue(mage.contains(1085), "Mage scheme deve conter Acumen (1085)");
		assertTrue(mage.contains(1059), "Mage scheme deve conter Empower (1059)");
	}

	@Test
	@DisplayName("Salvar e carregar esquema customizado")
	void testSaveAndLoadScheme() {
		List<Integer> customBuffs = List.of(1068, 1040, 1204);
		bufferService.saveScheme(player.objectId(), 3, customBuffs);
		verify(variablesService).setVariable(eq(2001), eq("buff_scheme_3"), eq("1068,1040,1204"));

		when(variablesService.getVariable(eq(2001), eq("buff_scheme_3"), any())).thenReturn("1068,1040,1204");
		List<Integer> loaded = bufferService.getScheme(player.objectId(), 3);
		assertEquals(customBuffs, loaded);
	}

	@Test
	@DisplayName("Cura completa restaura HP, MP e CP ao maximo")
	void testHeal() {
		when(inventory.destroyItemByItemId(eq(SchemeBufferService.ADENA_ID), anyInt())).thenReturn(true);

		boolean ok = bufferService.heal(player, session);
		assertTrue(ok);
		assertEquals(player.maxHp(), player.currentHp());
		assertEquals(player.maxMp(), player.currentMp());
		assertEquals(player.maxCp(), player.currentCp());
	}

	@Test
	@DisplayName("Nao permite cura se o jogador estiver em combate")
	void testNoHealInCombat() {
		player.enterCombat();
		boolean ok = bufferService.heal(player, session);
		assertFalse(ok);
		assertNotEquals(player.maxHp(), player.currentHp());
	}

	@Test
	@DisplayName("Cancelar buffs limpa todos os efeitos ativos do jogador")
	void testCancelBuffs() {
		player.effects().addBuff(1068, 1, 60000);
		player.effects().addBuff(1040, 1, 60000);
		assertEquals(2, player.effects().activeBuffs().size());

		bufferService.cancelBuffs(player, session);
		assertEquals(0, player.effects().activeBuffs().size());
	}

	@Test
	@DisplayName("Aplicar esquema adiciona buffs aos efeitos do jogador")
	void testApplyScheme() {
		when(inventory.destroyItemByItemId(eq(SchemeBufferService.ADENA_ID), anyInt())).thenReturn(true);

		boolean ok = bufferService.applyScheme(player, session, 1, false);
		assertTrue(ok);
		assertTrue(player.effects().activeBuffs().size() > 0, "Deve conter buffs aplicados");
	}

	@Test
	@DisplayName("Aplicar esquema com skillService carrega skill e aplica funcs de status")
	void testApplySchemeWithSkillTemplate() {
		when(inventory.destroyItemByItemId(eq(SchemeBufferService.ADENA_ID), anyInt())).thenReturn(true);

		var statFunc = new com.lopez.l2j.game.skill.StatFunc(
				"pAtk",
				com.lopez.l2j.game.skill.StatFunc.Op.MUL,
				0x30,
				1.15
		);
		var effect = new com.lopez.l2j.game.skill.SkillTemplate.EffectTemplate(
				"Buff", 1, 1200, 0, "pAtk", 1.0, List.of(statFunc)
		);
		var sk = new com.lopez.l2j.game.skill.SkillTemplate(
				1068, 3, "Might", com.lopez.l2j.game.skill.SkillTemplate.OperateType.ACTIVE,
				"BUFF", "TARGET_ONE", false, 0, 0, 0, 0.0, 0, 0, 0, 0, 0, 0, 0.0, false, 0, 0,
				List.of(statFunc), List.of(effect), null, null
		);

		com.lopez.l2j.game.skill.SkillTable table = mock(com.lopez.l2j.game.skill.SkillTable.class);
		when(table.maxLevel(1068)).thenReturn(3);
		when(skillService.table()).thenReturn(table);
		when(skillService.skill(1068, 3)).thenReturn(Optional.of(sk));

		bufferService.saveScheme(player.objectId(), 1, List.of(1068));
		when(variablesService.getVariable(eq(player.objectId()), eq("buff_scheme_1"), any())).thenReturn("1068");

		boolean ok = bufferService.applyScheme(player, session, 1, false);
		assertTrue(ok);
		assertEquals(1, player.effects().activeBuffs().size());
		assertFalse(player.effects().funcs().isEmpty(), "Buff deve conter as funcoes de status (funcs)");
	}
}
