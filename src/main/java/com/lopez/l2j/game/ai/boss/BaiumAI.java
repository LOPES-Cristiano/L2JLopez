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

	public BaiumAI(NpcInstance npc, GameWorld world, CombatService combatService,
			CharTemplateTable charTemplates, NpcSkillTable npcSkillTable, SkillTable skillTable,
			java.util.concurrent.ScheduledExecutorService scheduler, com.lopez.l2j.game.zone.ZoneTable zones) {
		super(npc, world, combatService, charTemplates, npcSkillTable, skillTable, scheduler, zones);
	}

	@Override
	protected SkillTemplate chooseMonsterSkill() {
		if (skillTable == null) return null;

		// 1. So tenta usar skill especial em 35% das acoes (nos outros 65%, retorna null para dar o ataque fisico pesado do Boss!)
		if (ThreadLocalRandom.current().nextInt(100) >= 35) {
			return null;
		}

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

		// 2. Verifica !npc.isSkillDisabled(skillId) antes de retornar a skill; se estiver em cooldown, retorna null para atacar fisicamente!
		if (npc.isSkillDisabled(skillId)) {
			return null;
		}

		return skillTable.get(skillId, 1).orElse(null);
	}
}
