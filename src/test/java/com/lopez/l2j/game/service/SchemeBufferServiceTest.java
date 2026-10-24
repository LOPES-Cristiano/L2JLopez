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

	@Test
	@DisplayName("Lucera Buffer: aplica conjunto de buffs por menuId (ask=201) e grava/aplica perfil (BuffProf)")
	void testLuceraBufferTemplateAndProfileRecording() {
		when(inventory.destroyItemByItemId(eq(SchemeBufferService.ADENA_ID), anyInt())).thenReturn(true);

		// Aplicar template 201 (Fighter)
		bufferService.handleLuceraBufferAct(player, session, null, "ask=201&reply=1", false);
		assertFalse(player.effects().activeBuffs().isEmpty(), "Template 201 deve aplicar buffs ao jogador");

		// Limpar buffs via ask=-2
		bufferService.handleLuceraBufferAct(player, session, null, "ask=-2&reply=1", false);
		assertTrue(player.effects().activeBuffs().isEmpty(), "ask=-2 deve cancelar todos os buffs");

		// Iniciar gravacao de perfil (ask=-100)
		bufferService.handleLuceraBufferAct(player, session, null, "ask=-100&reply=999", false);
		assertTrue(bufferService.isRecordingProfile(player.objectId()), "Jogador deve estar gravando perfil");

		// Selecionar Wind Walk (menuId=1 -> skill 1204 lv 2) e Shield (menuId=3 -> skill 1040 lv 3)
		bufferService.handleLuceraBufferAct(player, session, null, "ask=1&reply=999", false);
		bufferService.handleLuceraBufferAct(player, session, null, "ask=3&reply=999", false);

		// Salvar no perfil One (ask=-2000 -> BuffProf-0)
		bufferService.handleLuceraBufferAct(player, session, null, "ask=-2000&reply=1", false);
		assertFalse(bufferService.isRecordingProfile(player.objectId()), "Gravacao deve encerrar ao salvar");
		verify(variablesService).setVariable(eq(player.objectId()), eq("BuffProf-0"), eq("1;3;"));
		verify(variablesService).setVariable(eq(player.objectId()), eq("buff_scheme_1"), eq("1204,1040"));

		// Simular perfil salvo e aplicar via ask=-1000
		player.effects().clear();
		when(variablesService.getVariable(eq(player.objectId()), eq("BuffProf-0"), any())).thenReturn("1;3;");
		bufferService.handleLuceraBufferAct(player, session, null, "ask=-1000&reply=1", false);
		assertEquals(2, player.effects().activeBuffs().size(), "Perfil gravado deve reaplicar os 2 buffs salvos");
	}

	@Test
	@DisplayName("Edicao de esquemas (adicionar, remover, limpar e renderizar HTML)")
	void testSchemeEditing() {
		when(variablesService.getVariable(eq(player.objectId()), eq("buff_scheme_3"), any())).thenReturn("1068,1040");

		assertTrue(bufferService.addSkillToScheme(player.objectId(), 3, 1204));
		verify(variablesService).setVariable(eq(player.objectId()), eq("buff_scheme_3"), eq("1068,1040,1204"));

		when(variablesService.getVariable(eq(player.objectId()), eq("buff_scheme_3"), any())).thenReturn("1068,1040,1204");
		assertTrue(bufferService.removeSkillFromScheme(player.objectId(), 3, 1040));
		verify(variablesService).setVariable(eq(player.objectId()), eq("buff_scheme_3"), eq("1068,1204"));

		String html = bufferService.renderSchemeEditHtml(player, 3);
		assertNotNull(html);
		assertTrue(html.contains("Editor de Esquema"), "HTML deve conter o titulo do Editor de Esquema");
	}

	@Test
	@DisplayName("Buff bloqueado por stackOrder superior nao cria entrada fantasma em normalBuffs")
	void testBlockedBuffDoesNotCreatePhantomFallbackEntry() {
		when(inventory.destroyItemByItemId(eq(SchemeBufferService.ADENA_ID), anyInt())).thenReturn(true);

		var statFunc = new com.lopez.l2j.game.skill.StatFunc(
				"pAtk", com.lopez.l2j.game.skill.StatFunc.Op.MUL, 0x30, 1.15);
		var greaterEffect = new com.lopez.l2j.game.skill.SkillTemplate.EffectTemplate(
				"Buff", 1, 1200, 0, "pAtk", 3.0, List.of(statFunc));
		var lesserEffect = new com.lopez.l2j.game.skill.SkillTemplate.EffectTemplate(
				"Buff", 1, 1200, 0, "pAtk", 1.15, List.of(statFunc));
		var greaterMight = new com.lopez.l2j.game.skill.SkillTemplate(
				1388, 3, "Greater Might", com.lopez.l2j.game.skill.SkillTemplate.OperateType.ACTIVE,
				"BUFF", "TARGET_ONE", true, 0, 0, 0, 0.0, 0, 0, 0, 0, 0, 0, 0.0, false, 0, 0,
				List.of(statFunc), List.of(greaterEffect), null, null);
		var might = new com.lopez.l2j.game.skill.SkillTemplate(
				1068, 3, "Might", com.lopez.l2j.game.skill.SkillTemplate.OperateType.ACTIVE,
				"BUFF", "TARGET_ONE", true, 0, 0, 0, 0.0, 0, 0, 0, 0, 0, 0, 0.0, false, 0, 0,
				List.of(statFunc), List.of(lesserEffect), null, null);

		com.lopez.l2j.game.skill.SkillTable table = mock(com.lopez.l2j.game.skill.SkillTable.class);
		when(table.maxLevel(1388)).thenReturn(3);
		when(table.maxLevel(1068)).thenReturn(3);
		when(skillService.table()).thenReturn(table);
		when(skillService.skill(1388, 3)).thenReturn(Optional.of(greaterMight));
		when(skillService.skill(1068, 3)).thenReturn(Optional.of(might));

		bufferService.saveScheme(player.objectId(), 1, List.of(1388, 1068));
		when(variablesService.getVariable(eq(player.objectId()), eq("buff_scheme_1"), any())).thenReturn("1388,1068");

		assertTrue(bufferService.applyScheme(player, session, 1, false));
		assertEquals(1, player.effects().activeBuffs().size(), "Nao deve criar buff fantasma skill_1068 quando bloqueado por Greater Might");
		assertTrue(player.effects().hasSkill(1388));
		assertFalse(player.effects().hasSkill(1068));
	}
}
