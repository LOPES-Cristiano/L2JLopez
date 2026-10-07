package com.lopez.l2j.game.ai;

import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcSkillTable;
import com.lopez.l2j.game.skill.SkillTable;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.world.GameWorld;

/**
 * Fabrica de criacao e resolucao de arquetipos de IA de NPCs.
 */
public class NpcAiFactory {

	public static AbstractNpcAI createAI(NpcInstance npc, GameWorld world, CombatService combatService,
			CharTemplateTable charTemplates, NpcSkillTable npcSkillTable, SkillTable skillTable) {
		if (npc == null || npc.template() == null) {
			return new FighterAI(npc, world, combatService, charTemplates, npcSkillTable, skillTable);
		}

		String type = npc.template().type();
		String name = npc.template().name().toLowerCase();
		int attackRange = npc.template().attackRange();

		// 1. Guardas de Vilas e Castelos
		if ("L2Guard".equalsIgnoreCase(type) || name.contains("guard") || name.contains("sentry")) {
			return new GuardAI(npc, world, combatService, charTemplates, npcSkillTable, skillTable);
		}

		// 2. Curandeiros e Suportes (Priest)
		if (hasHealSkill(npc.npcId(), npcSkillTable, skillTable) || name.contains("priest") || name.contains("healer") || name.contains("shaman")) {
			return new PriestAI(npc, world, combatService, charTemplates, npcSkillTable, skillTable);
		}

		// 3. Arqueiros e Ataque à Distancia (Ranger)
		if (attackRange >= 400 || name.contains("archer") || name.contains("sniper") || name.contains("bowman")) {
			return new RangerAI(npc, world, combatService, charTemplates, npcSkillTable, skillTable);
		}

		// 4. Conjuradores Mágicos (Mystic)
		if (hasMagicDamageSkill(npc.npcId(), npcSkillTable, skillTable) || name.contains("mage") || name.contains("wizard") || name.contains("witch")) {
			return new MysticAI(npc, world, combatService, charTemplates, npcSkillTable, skillTable);
		}

		// 5. Padrao: Guerreiro Melee (Fighter)
		return new FighterAI(npc, world, combatService, charTemplates, npcSkillTable, skillTable);
	}

	private static boolean hasHealSkill(int npcId, NpcSkillTable npcSkillTable, SkillTable skillTable) {
		if (npcSkillTable == null || skillTable == null) return false;
		var skills = npcSkillTable.getSkills(npcId);
		if (skills == null) return false;
		for (var entry : skills) {
			var opt = skillTable.get(entry.skillId(), entry.level());
			if (opt.isPresent() && opt.get().isHeal()) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasMagicDamageSkill(int npcId, NpcSkillTable npcSkillTable, SkillTable skillTable) {
		if (npcSkillTable == null || skillTable == null) return false;
		var skills = npcSkillTable.getSkills(npcId);
		if (skills == null) return false;
		for (var entry : skills) {
			var opt = skillTable.get(entry.skillId(), entry.level());
			if (opt.isPresent() && (opt.get().isMagicDamage() || opt.get().magic())) {
				return true;
			}
		}
		return false;
	}
}
