package com.lopez.l2j.game.ai.boss;

import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcSkillTable;
import com.lopez.l2j.game.skill.SkillTable;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Inteligencia Artificial especifica de Baium (ai.Baium do Lucera2 / L2JDream).
 * Executa golpes pesados de punho (4127), trovão em area (4128), terremoto da torre (4129)
 * e ataques devastadores de martelo de energia (4130, 4131).
 */
public class BaiumAI extends RaidBossAI {

	public static final int SKILL_FIST = 4127;
	public static final int SKILL_THUNDER = 4128;
	public static final int SKILL_EARTHQUAKE = 4129;
	public static final int SKILL_STRIKE_1 = 4130;
	public static final int SKILL_STRIKE_2 = 4131;

	public BaiumAI(NpcInstance npc, GameWorld world, CombatService combatService,
			CharTemplateTable charTemplates, NpcSkillTable npcSkillTable, SkillTable skillTable) {
		super(npc, world, combatService, charTemplates, npcSkillTable, skillTable);
	}

	@Override
	protected SkillTemplate chooseMonsterSkill() {
		if (skillTable == null) return null;

		double hpPercent = (npc.currentHp() / npc.maxHp()) * 100.0;
		int roll = ThreadLocalRandom.current().nextInt(100);

		int skillId = SKILL_FIST;
		if (roll < 30) {
			skillId = SKILL_THUNDER;
		} else if (roll < 60) {
			skillId = SKILL_EARTHQUAKE;
		} else if (roll < 80 && hpPercent <= 50.0) {
			skillId = SKILL_STRIKE_2;
		} else if (hpPercent <= 25.0) {
			skillId = SKILL_STRIKE_1;
		}

		return skillTable.get(skillId, 1).orElseGet(super::chooseMonsterSkill);
	}
}
