package com.lopez.l2j.game.ai.boss;

import com.lopez.l2j.game.ai.PriestAI;
import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcSkillTable;
import com.lopez.l2j.game.skill.SkillTable;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.world.GameWorld;

/**
 * Inteligencia Artificial especifica das Formigas Enfermeiras da Queen Ant (ai.QueenAntNurse do Lucera2).
 * Prioriza curar a Larva (29004) e a Queen Ant (29001) com magia de cura constante.
 */
public class QueenAntNurseAI extends PriestAI {

	public static final int QUEEN_ANT = 29001;
	public static final int QUEEN_ANT_LARVA = 29004;
	public static final int SKILL_HEAL_1 = 4020;
	public static final int SKILL_HEAL_2 = 4024;

	public QueenAntNurseAI(NpcInstance npc, GameWorld world, CombatService combatService,
			CharTemplateTable charTemplates, NpcSkillTable npcSkillTable, SkillTable skillTable) {
		super(npc, world, combatService, charTemplates, npcSkillTable, skillTable);
	}

	@Override
	public void processCombat() {
		if (npc.isDead() || world == null) return;

		// Busca a Rainha ou a Larva nas proximidades para curar
		NpcInstance healTarget = null;
		for (NpcInstance nearby : world.npcs()) {
			if (nearby == null || nearby.isDead()) continue;
			if (nearby.npcId() == QUEEN_ANT_LARVA && nearby.currentHp() < nearby.maxHp()) {
				healTarget = nearby;
				break;
			}
			if (nearby.npcId() == QUEEN_ANT && nearby.currentHp() < nearby.maxHp()) {
				healTarget = nearby;
			}
		}

		if (healTarget != null) {
			double dist = Math.hypot(healTarget.x() - npc.x(), healTarget.y() - npc.y());
			if (dist > 600) {
				moveTowards(healTarget.x(), healTarget.y(), healTarget.z(), healTarget.objectId(), 400, dist);
			} else {
				// Cura o alvo
				double healAmount = healTarget.maxHp() * 0.05;
				healTarget.currentHp(Math.min(healTarget.maxHp(), healTarget.currentHp() + healAmount));
			}
		}

		super.processCombat();
	}
}
