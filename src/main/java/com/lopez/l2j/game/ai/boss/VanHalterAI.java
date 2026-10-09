package com.lopez.l2j.game.ai.boss;

import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.npc.NpcSkillTable;
import com.lopez.l2j.game.skill.SkillTable;
import com.lopez.l2j.game.world.GameWorld;
import java.util.concurrent.ThreadLocalRandom;

/**
 * IA da High Priestess van Halter (Pagan Temple Boss ID 29062).
 * Especializada em drenagem sombria, sangramento e estupefacao.
 */
public class VanHalterAI extends RaidBossAI {

	public static final int SKILL_VANHALTER_STRIKE = 4734;
	public static final int SKILL_VANHALTER_BLEED = 4735;
	public static final int SKILL_DARK_VORTEX = 4736;

	public VanHalterAI(
			NpcInstance npc,
			GameWorld world,
			CombatService combatService,
			CharTemplateTable charTemplates,
			NpcSkillTable npcSkillTable,
			SkillTable skillTable) {
		super(npc, world, combatService, charTemplates, npcSkillTable, skillTable);
	}

	public VanHalterAI(
			NpcInstance npc,
			GameWorld world,
			CombatService combatService,
			CharTemplateTable charTemplates,
			NpcSkillTable npcSkillTable,
			SkillTable skillTable,
			java.util.concurrent.ScheduledExecutorService scheduler,
			com.lopez.l2j.game.zone.ZoneTable zones) {
		super(npc, world, combatService, charTemplates, npcSkillTable, skillTable, scheduler, zones);
	}

	@Override
	protected SkillTemplate chooseMonsterSkill() {
		if (skillTable == null) return null;

		int roll = ThreadLocalRandom.current().nextInt(100);
		int skillId;
		if (roll < 35) {
			skillId = SKILL_VANHALTER_STRIKE;
		} else if (roll < 70) {
			skillId = SKILL_VANHALTER_BLEED;
		} else {
			skillId = SKILL_DARK_VORTEX;
		}

		return skillTable.get(skillId, 1).orElseGet(super::chooseMonsterSkill);
	}
}
