package com.lopez.l2j.game.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.game.effect.PlayerEffects.ActiveBuff;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.PlayerStats;
import com.lopez.l2j.game.template.CharTemplateTable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Le o datapack real (data/xml) e confere arvore, passivas, buffs e auto-learn. */
class SkillSystemTest {

	private static SkillTable table;
	private static SkillTreeTable trees;

	@BeforeAll
	static void load() {
		table = new SkillTable(Path.of("data", "xml", "stats", "skills"));
		trees = new SkillTreeTable(Path.of("data", "xml", "player", "skills"));
	}

	private static PlayerCharacter player(int level, int classId) {
		return new PlayerCharacter(0x10000001, "tester", "Hero", level, 0, 0, 0, classId, classId, false, 0, 0, 0,
				100, 50, 50, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100.0, 50.0, 50.0);
	}

	/** Repositorio em memoria. */
	private static final class MemRepo implements SkillRepository {
		final List<Skill> saved = new ArrayList<>();

		@Override
		public List<Skill> findByCharId(int charId, int classIndex) {
			return saved;
		}

		@Override
		public void save(int charId, int classIndex, Skill skill) {
			saved.removeIf(s -> s.id() == skill.id());
			saved.add(skill);
		}

		@Override
		public void delete(int charId, int classIndex, int skillId) {
			saved.removeIf(s -> s.id() == skillId);
		}

		@Override
		public void deleteAll(int charId, int classIndex) {
			saved.clear();
		}
	}

	@Test
	void parsesActivePassiveAndBuffSkills() {
		var strike = table.get(1177, 1).orElseThrow(); // Wind Strike
		assertEquals("MDAM", strike.skillType());
		assertTrue(strike.magic());
		assertEquals(12, strike.power(), 0.001);
		assertEquals(600, strike.castRange());

		var mastery = table.get(141, 3).orElseThrow(); // Weapon Mastery
		assertTrue(mastery.isPassive());
		assertEquals(2, mastery.funcs().size());

		var might = table.get(1068, 3).orElseThrow();
		assertEquals("BUFF", might.skillType());
		assertEquals(1, might.effects().size());
		var e = might.effects().get(0);
		assertEquals("pa_up", e.stackType());
		assertEquals(1_200_000L, e.durationMs());
		assertEquals(1.15, e.funcs().get(0).value(), 0.0001);
	}

	@Test
	void treeInheritsParentClassAndOffersNextLevels() {
		// Warrior (1) herda Human Fighter (0): Power Strike (3) vem do pai
		var all = trees.allFor(1);
		assertTrue(all.stream().anyMatch(s -> s.id() == 3));
		assertTrue(trees.canTeach(30010, 0));

		var avail = trees.available(0, 5, Map.of(3, 1));
		assertTrue(avail.stream().anyMatch(s -> s.id() == 3 && s.level() == 2));
		assertFalse(avail.stream().anyMatch(s -> s.id() == 3 && s.level() == 3));
	}

	@Test
	void autoLearnGivesSkillsAndPassivesChangeStats() {
		var repo = new MemRepo();
		var svc = new SkillService(table, trees, repo, true, 0, true);
		var p = player(20, 0);
		var template = new CharTemplateTable().get(0).orElseThrow();
		var before = PlayerStats.calculate(p, template);

		assertTrue(svc.rewardSkills(p));
		assertEquals(1, p.skillLevel(239)); // Expertise D no 20
		assertEquals(0, p.skillLevel(SkillService.SKILL_LUCKY)); // Lucky some no 10
		assertTrue(p.skillLevel(141) > 0); // Weapon Mastery
		assertFalse(repo.saved.isEmpty());

		var after = PlayerStats.calculate(p, template);
		assertTrue(after.pAtk() > before.pAtk(), "Weapon Mastery deve aumentar o P.Atk");
	}

	@Test
	void withoutAutoLearnOnlyFreeSkillsAreGiven() {
		var svc = new SkillService(table, trees, new MemRepo(), false, 0, true);
		var p = player(20, 0);
		svc.rewardSkills(p);
		assertEquals(1, p.skillLevel(239)); // Expertise e gratis
		assertTrue(p.skillLevel(3) <= 1); // Power Strike so o nivel inicial
		assertFalse(svc.available(p).isEmpty()); // resto fica para o treinador
	}

	@Test
	void skillBuffFuncsApplyToStats() {
		var p = player(20, 0);
		var template = new CharTemplateTable().get(0).orElseThrow();
		var base = PlayerStats.calculate(p, template);
		var might = table.get(1068, 3).orElseThrow().effects().get(0);
		p.effects().put(ActiveBuff.ofSkill(1068, 3, might.stackType(), System.currentTimeMillis() + 60_000,
				might.funcs()));
		assertEquals(Math.round(base.pAtk() * 1.15), PlayerStats.calculate(p, template).pAtk());
	}

	@Test
	void activeSkillWeaponRestrictionValidatesEquippedWeapon() {
		var strike = table.get(3, 1).orElseThrow(); // Power Strike
		org.junit.jupiter.api.Assertions.assertNotNull(strike.castCondition());

		var p = player(20, 0);
		assertFalse(strike.castCondition().test(p), "Desarmado nao deve poder usar Power Strike");

		var bowTemplate = com.lopez.l2j.game.item.TestItems.templates().stream()
				.filter(t -> t.id() == com.lopez.l2j.game.item.TestItems.BOW).findFirst().orElseThrow();
		var bowInstance = new com.lopez.l2j.game.item.ItemInstance(101, bowTemplate, 1, 1);
		p.inventory().add(bowInstance);
		p.inventory().equip(bowInstance);
		assertFalse(strike.castCondition().test(p), "Com arco nao deve poder usar Power Strike");

		var swordTemplate = com.lopez.l2j.game.item.TestItems.templates().stream()
				.filter(t -> t.id() == com.lopez.l2j.game.item.TestItems.SHORT_SWORD).findFirst().orElseThrow();
		var swordInstance = new com.lopez.l2j.game.item.ItemInstance(102, swordTemplate, 1, 1);
		p.inventory().add(swordInstance);
		p.inventory().equip(swordInstance);
		assertTrue(strike.castCondition().test(p), "Com espada deve poder usar Power Strike");
	}

	@Test
	void chargesParsedAndValidated() {
		var sonicFocus1 = table.get(8, 1).orElseThrow();
		assertEquals(1, sonicFocus1.giveCharges());
		assertEquals(1, sonicFocus1.maxCharges());

		var sonicFocus7 = table.get(8, 7).orElseThrow();
		assertEquals(1, sonicFocus7.giveCharges());
		assertFalse(sonicFocus1.continueAfterMax());
		assertFalse(sonicFocus7.continueAfterMax());

		var sonicRage = table.get(345, 1).orElseThrow();
		assertEquals(1, sonicRage.giveCharges());
		assertEquals(7, sonicRage.maxCharges());
		assertTrue(sonicRage.continueAfterMax());

		var ragingForce = table.get(346, 1).orElseThrow();
		assertEquals(1, ragingForce.giveCharges());
		assertEquals(7, ragingForce.maxCharges());
		assertTrue(ragingForce.continueAfterMax());

		var tss = table.get(261, 1).orElseThrow(); // Triple Sonic Slash
		assertEquals(4, tss.needCharges());
		assertTrue(tss.consumeCharges());
		assertTrue(tss.isChargedDam());

		var sonicBarrier = table.get(442, 1).orElseThrow(); // Sonic Barrier
		assertEquals(5, sonicBarrier.needCharges());
		assertTrue(sonicBarrier.consumeCharges());
		assertFalse(sonicBarrier.effects().isEmpty());
		assertEquals("Invincible", sonicBarrier.effects().get(0).name());

		var forceBarrier = table.get(443, 1).orElseThrow(); // Force Barrier
		assertEquals(4, forceBarrier.needCharges());
		assertTrue(forceBarrier.consumeCharges());
		assertFalse(forceBarrier.effects().isEmpty());
		assertEquals("Invincible", forceBarrier.effects().get(0).name());
	}

	@Test
	void battleRoarEffectIncreasesMaxHp() {
		var battleRoar = table.get(121, 1).orElseThrow();
		assertEquals("HEAL_PERCENT", battleRoar.skillType());
		assertFalse(battleRoar.effects().isEmpty());
		var effect = battleRoar.effects().get(0);
		assertEquals("max_hp_up", effect.stackType());
		assertFalse(effect.funcs().isEmpty());

		var p = player(40, 0);
		var template = new CharTemplateTable().get(0).orElseThrow();
		var baseMaxHp = template.calculateMaxHp(p.level());

		p.effects().put(ActiveBuff.ofSkill(121, 1, effect.stackType(), System.currentTimeMillis() + 60_000,
				effect.funcs()));
		var buffedMaxHp = PlayerStats.applyStat(p, "maxHp", baseMaxHp);
		assertEquals(Math.round(baseMaxHp * 1.10), Math.round(buffedMaxHp));
	}

	@Test
	void chargeDamageScalingInCombat() {
		var combat = new com.lopez.l2j.game.combat.CombatService();
		var p = player(76, 88); // Duelist
		var template = new CharTemplateTable().get(0).orElseThrow();

		var npcTemplate = new com.lopez.l2j.game.npc.NpcTemplate(20001, 20001, "Test Monster", false, "", false,
				9.0, 24.0, 70, "male", "Monster", 40, 10000000, 1000, 500, 200, 300, 200, 253, 333,
				0, 0, 0, 50, 120, 0, false, 5000L, 500);
		var npc = new com.lopez.l2j.game.npc.NpcInstance(0x20000001, npcTemplate, 0, 0, 0, 0);

		p.charges(0);
		double sum0 = 0;
		for (int i = 0; i < 20; i++) {
			npc.dead(false);
			npc.currentHp(10000000);
			sum0 += combat.skillPhysicalNpc(p, template, npc, 2000.0, false, false, true).damage();
		}
		double avg0 = sum0 / 20.0;

		p.charges(7);
		double sum7 = 0;
		for (int i = 0; i < 20; i++) {
			npc.dead(false);
			npc.currentHp(10000000);
			sum7 += combat.skillPhysicalNpc(p, template, npc, 2000.0, false, false, true).damage();
		}
		double avg7 = sum7 / 20.0;

		assertTrue(avg7 > avg0 * 2.0, "7 charges deve aumentar expressivamente o dano medio (esperado ~2.75x)");
	}

	@Test
	void cleanInvalidSkillsRemovesForeignClassSkills() {
		var repo = new MemRepo();
		var svc = new SkillService(table, trees, repo, false, 0, true);
		var p = player(76, 88); // Duelist

		// Adiciona skill legitimo de Duelist e um skill de Bishop (1217 - Greater Heal)
		p.skills().put(261, 1); // Triple Sonic Slash (valido)
		p.skills().put(1217, 1); // Greater Heal (invalido para Duelist)
		p.skills().put(239, 1); // Expertise (valido/comum)

		svc.cleanInvalidSkills(p);

		assertTrue(p.skills().containsKey(261), "Skill legitimo da classe deve ser mantido");
		assertTrue(p.skills().containsKey(239), "Skill comum (Expertise) deve ser mantido");
		assertFalse(p.skills().containsKey(1217), "Skill de outra classe (Bishop) deve ser removido do Duelist");
	}
}
