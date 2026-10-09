package com.lopez.l2j.game.service;

import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.item.ItemTemplateTable;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.npc.NpcTemplateTable;
import com.lopez.l2j.game.skill.SkillTable;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.game.world.GameWorld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminSearchServiceTest {

	private NpcTemplateTable npcTemplates;
	private ItemTemplateTable itemTemplates;
	private SkillTable skillTable;
	private GameWorld world;
	private AdminSearchService service;

	@BeforeEach
	void setUp() {
		NpcTemplate tpl1 = NpcTemplate.fallback(29019, "Antharas", "L2RaidBoss");
		NpcTemplate tpl2 = NpcTemplate.fallback(29028, "Valakas", "L2RaidBoss");
		NpcTemplate tpl3 = NpcTemplate.fallback(20001, "Gremlin Guard", "L2Monster");
		NpcTemplate tpl4 = NpcTemplate.fallback(20002, "Elven Scout Guard", "L2Monster");
		npcTemplates = NpcTemplateTable.of(List.of(tpl1, tpl2, tpl3, tpl4));

		ItemTemplate item1 = ItemTemplate.weapon(7575, 7575, "Draconic Bow", "lrhand", "bow", 1600, "s",
				581, 132, 293, 120, 0, 0, 0, 1, 1, 5, 48800000, List.of(), null, null, null,
				true, true, true, true);
		ItemTemplate item2 = ItemTemplate.armor(6379, 6379, "Draconic Leather Armor", "chest", "light", 4800, "s",
				215, 0, 0, 0, 31200000, List.of(), true, true, true, true);
		ItemTemplate item3 = ItemTemplate.etc(57, 57, "Adena", "money", "asset", 0, "none", 1,
				List.of(), true, true, true, true);
		itemTemplates = ItemTemplateTable.of(List.of(item1, item2, item3), List.of());

		skillTable = new SkillTable();
		var statFunc = new com.lopez.l2j.game.skill.StatFunc("pAtk", com.lopez.l2j.game.skill.StatFunc.Op.MUL, 0x30, 1.15);
		var effect = new com.lopez.l2j.game.skill.SkillTemplate.EffectTemplate("Buff", 1, 1200, 0, "pAtk", 1.0, List.of(statFunc));
		SkillTemplate skMight = new SkillTemplate(
				1068, 3, "Might", SkillTemplate.OperateType.ACTIVE, "BUFF",
				"TARGET_ONE", false, 10, 0, 0, 0.0,
				600, 0, 1500, 500, 0, 1,
				0.0, false, 0, 0,
				List.of(statFunc), List.of(effect), null, null
		);
		SkillTemplate skHaste = new SkillTemplate(
				1086, 2, "Haste", SkillTemplate.OperateType.ACTIVE, "BUFF",
				"TARGET_ONE", false, 15, 0, 0, 0.0,
				600, 0, 1500, 500, 0, 1,
				0.0, false, 0, 0,
				List.of(statFunc), List.of(effect), null, null
		);
		skillTable.register(skMight);
		skillTable.register(skHaste);

		world = mock(GameWorld.class);
		service = new AdminSearchService(npcTemplates, itemTemplates, skillTable, world);
	}

	@Test
	@DisplayName("Busca de NPC por ID numerico exato")
	void testSearchNpcById() {
		var results = service.searchNpcs("29019");
		assertEquals(1, results.size());
		assertEquals("Antharas", results.get(0).name());
		assertEquals(29019, results.get(0).id());
	}

	@Test
	@DisplayName("Busca de NPC case-insensitive por substring")
	void testSearchNpcByNameCaseInsensitive() {
		var results = service.searchNpcs("anth");
		assertEquals(1, results.size());
		assertEquals("Antharas", results.get(0).name());

		var resultsUpper = service.searchNpcs("VALAKAS");
		assertEquals(1, resultsUpper.size());
		assertEquals("Valakas", resultsUpper.get(0).name());
	}

	@Test
	@DisplayName("Busca de NPC com multiplos termos de busca")
	void testSearchNpcMultipleTokens() {
		var results = service.searchNpcs("guard elven");
		assertEquals(1, results.size());
		assertEquals("Elven Scout Guard", results.get(0).name());

		var guards = service.searchNpcs("guard");
		assertEquals(2, guards.size());
	}

	@Test
	@DisplayName("Busca de Item por ID e por substring case-insensitive")
	void testSearchItem() {
		var byId = service.searchItems("57");
		assertEquals(1, byId.size());
		assertEquals("Adena", byId.get(0).name());

		var draconic = service.searchItems("draconic");
		assertEquals(2, draconic.size());
		assertTrue(draconic.stream().anyMatch(i -> i.name().equals("Draconic Bow")));
		assertTrue(draconic.stream().anyMatch(i -> i.name().equals("Draconic Leather Armor")));

		var bow = service.searchItems("bow draconic");
		assertEquals(1, bow.size());
		assertEquals("Draconic Bow", bow.get(0).name());
	}

	@Test
	@DisplayName("Busca de Skill por ID e nome")
	void testSearchSkill() {
		var byId = service.searchSkills("1068");
		assertEquals(1, byId.size());
		assertEquals("Might", byId.get(0).name());
		assertEquals(3, byId.get(0).maxLevel());

		var haste = service.searchSkills("haste");
		assertEquals(1, haste.size());
		assertEquals("Haste", haste.get(0).name());
		assertEquals(2, haste.get(0).maxLevel());
	}

	@Test
	@DisplayName("Renderizacao de telas HTML com paginacao e feedback amigavel")
	void testRenderHtml() {
		String npcHtm = service.renderNpcSearchHtml("guard", 1);
		assertNotNull(npcHtm);
		assertTrue(npcHtm.contains("Gremlin Guard"));
		assertTrue(npcHtm.contains("Elven Scout Guard"));
		assertTrue(npcHtm.contains("Spawn"));

		String emptyNpc = service.renderNpcSearchHtml("inexistente_xyz_123", 1);
		assertTrue(emptyNpc.contains("Nenhum NPC encontrado"));

		String itemHtm = service.renderItemSearchHtml("draconic", 1);
		assertNotNull(itemHtm);
		assertTrue(itemHtm.contains("Draconic Bow"));
		assertTrue(itemHtm.contains("Give 1"));

		String skillHtm = service.renderSkillSearchHtml("might", 1);
		assertNotNull(skillHtm);
		assertTrue(skillHtm.contains("Might"));
		assertTrue(skillHtm.contains("Add Max"));

		String globalHtm = service.renderGlobalSearchHtml("a");
		assertNotNull(globalHtm);
		assertTrue(globalHtm.contains("Global Search Hub"));
	}
}
