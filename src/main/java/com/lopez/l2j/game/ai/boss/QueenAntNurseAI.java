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

	public QueenAntNurseAI(NpcInstance npc, GameWorld world, CombatService combatService,
			CharTemplateTable charTemplates, NpcSkillTable npcSkillTable, SkillTable skillTable,
			java.util.concurrent.ScheduledExecutorService scheduler, com.lopez.l2j.game.zone.ZoneTable zones) {
		super(npc, world, combatService, charTemplates, npcSkillTable, skillTable, scheduler, zones);
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
				// Cura o alvo e envia MagicSkillUse (4020 / 4024) e StatusUpdate
				int skillId = healTarget.npcId() == QUEEN_ANT_LARVA ? SKILL_HEAL_2 : SKILL_HEAL_1;
				int hitTime = 1500;
				int reuseDelay = 3000;
				if (skillTable != null) {
					var skOpt = skillTable.get(skillId, 1);
					if (skOpt.isPresent()) {
						hitTime = skOpt.get().hitTime() > 0 ? skOpt.get().hitTime() : 1500;
						reuseDelay = skOpt.get().reuseDelay() > 0 ? skOpt.get().reuseDelay() : 3000;
					}
				}
				double healAmount = healTarget.maxHp() * 0.05;
				healTarget.currentHp(Math.min(healTarget.maxHp(), healTarget.currentHp() + healAmount));
				int targetMaxHp = healTarget.maxHp() > 0 ? (int) healTarget.maxHp()
						: (healTarget.template() != null ? healTarget.template().maxHp() : 1000);

				var msu = new com.lopez.l2j.network.game.packet.GameServerPacket.MagicSkillUse(
						npc.objectId(), healTarget.objectId(), skillId, 1,
						hitTime, reuseDelay, npc.x(), npc.y(), npc.z(),
						healTarget.x(), healTarget.y(), healTarget.z());
				var su = com.lopez.l2j.network.game.packet.GameServerPacket.StatusUpdate.hp(
						healTarget.objectId(), (int) healTarget.currentHp(), targetMaxHp);
				world.broadcastAround(npc.x(), npc.y(), GameWorld.VISIBILITY_RADIUS, msu);
				world.broadcastAround(healTarget.x(), healTarget.y(), GameWorld.VISIBILITY_RADIUS, su);
			}
		}

		super.processCombat();
	}
}
