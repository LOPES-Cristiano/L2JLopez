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
}
