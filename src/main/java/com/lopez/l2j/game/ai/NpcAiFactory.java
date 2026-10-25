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
		return createAI(npc, world, combatService, charTemplates, npcSkillTable, skillTable, null, null);
	}

	public static AbstractNpcAI createAI(NpcInstance npc, GameWorld world, CombatService combatService,
			CharTemplateTable charTemplates, NpcSkillTable npcSkillTable, SkillTable skillTable,
			java.util.concurrent.ScheduledExecutorService scheduler, com.lopez.l2j.game.zone.ZoneTable zones) {
		if (npc == null || npc.template() == null) {
			return new FighterAI(npc, world, combatService, charTemplates, npcSkillTable, skillTable, scheduler, zones);
		}

		int npcId = npc.npcId();
		int idTemplate = npc.idTemplate();
		String type = npc.template().type();
		String name = npc.template().name().toLowerCase();
		int attackRange = npc.template().attackRange();

		// 0. Chefes Epicos e Raid Bosses Especializados (Lucera2 / L2JDream)
		if (npcId == 29019 || npcId == 29066 || npcId == 29067 || npcId == 29068) {
			return new com.lopez.l2j.game.ai.boss.AntharasAI(npc, world, combatService, charTemplates, npcSkillTable, skillTable, scheduler, zones);
		}
		if (npcId == 29028) {
			return new com.lopez.l2j.game.ai.boss.ValakasAI(npc, world, combatService, charTemplates, npcSkillTable, skillTable, scheduler, zones);
		}
		if (npcId == 29020) {
			return new com.lopez.l2j.game.ai.boss.BaiumAI(npc, world, combatService, charTemplates, npcSkillTable, skillTable, scheduler, zones);
		}
		if (npcId == 29002) {
			return new com.lopez.l2j.game.ai.boss.QueenAntNurseAI(npc, world, combatService, charTemplates, npcSkillTable, skillTable, scheduler, zones);
		}
		if (npcId == 29014) {
			return new com.lopez.l2j.game.ai.boss.OrfenAI(npc, world, combatService, charTemplates, npcSkillTable, skillTable, scheduler, zones);
		}
		if (npcId == 29022) {
			return new com.lopez.l2j.game.ai.boss.ZakenAI(npc, world, combatService, charTemplates, npcSkillTable, skillTable, scheduler, zones);
		}
		if (npcId == 29062) {
			return new com.lopez.l2j.game.ai.boss.VanHalterAI(npc, world, combatService, charTemplates, npcSkillTable, skillTable, scheduler, zones);
		}
		if (npc.template().isRaidBoss() || npc.template().isGrandBoss()) {
			return new com.lopez.l2j.game.ai.boss.RaidBossAI(npc, world, combatService, charTemplates, npcSkillTable, skillTable, scheduler, zones);
		}

		// 1. Guardas de Cerco (Castelos e Fortalezas) e Guardas de Vilas
		if ("L2SiegeGuard".equalsIgnoreCase(type) || "L2FortSiegeGuard".equalsIgnoreCase(type) || "L2Defender".equalsIgnoreCase(type)) {
			return new SiegeGuardAI(npc, world, combatService, charTemplates, npcSkillTable, skillTable, scheduler, zones);
		}
		if ("L2Guard".equalsIgnoreCase(type) || name.contains("guard") || name.contains("sentry")) {
			return new GuardAI(npc, world, combatService, charTemplates, npcSkillTable, skillTable, scheduler, zones);
		}

		// 2. Curandeiros e Suportes (Priest)
		if (hasHealSkill(npcId, idTemplate, npcSkillTable, skillTable) || name.contains("priest") || name.contains("healer")) {
			return new PriestAI(npc, world, combatService, charTemplates, npcSkillTable, skillTable, scheduler, zones);
		}

		// 3. Arqueiros e Ataque à Distancia (Ranger)
		if (attackRange >= 400 || name.contains("archer") || name.contains("sniper") || name.contains("bowman")) {
			return new RangerAI(npc, world, combatService, charTemplates, npcSkillTable, skillTable, scheduler, zones);
		}

		// 4. Conjuradores Mágicos (Mystic)
		boolean isNamedMystic = name.contains("mage") || name.contains("wizard") || name.contains("witch")
				|| name.contains("shaman") || name.contains("oracle") || name.contains("seer")
				|| name.contains("necromancer") || name.contains("sorcerer") || name.contains("invoker")
				|| name.contains("howler") || name.contains("elder");
		if (isNamedMystic || hasMagicDamageSkill(npcId, idTemplate, npcSkillTable, skillTable)) {
			return new MysticAI(npc, world, combatService, charTemplates, npcSkillTable, skillTable, scheduler, zones);
		}

		// 5. Padrao: Guerreiro Melee (Fighter)
		return new FighterAI(npc, world, combatService, charTemplates, npcSkillTable, skillTable, scheduler, zones);
	}

	private static boolean hasHealSkill(int npcId, int idTemplate, NpcSkillTable npcSkillTable, SkillTable skillTable) {
		if (npcSkillTable == null || skillTable == null) return false;
		var skills = npcSkillTable.getSkills(npcId, idTemplate);
		if (skills == null) return false;
		for (var entry : skills) {
			var opt = skillTable.get(entry.skillId(), entry.level());
			if (opt.isPresent() && !opt.get().isPassive() && !opt.get().isToggle() && opt.get().isHeal()) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasMagicDamageSkill(int npcId, int idTemplate, NpcSkillTable npcSkillTable, SkillTable skillTable) {
		if (npcSkillTable == null || skillTable == null) return false;
		var skills = npcSkillTable.getSkills(npcId, idTemplate);
		if (skills == null) return false;
		boolean hasLongRangeMagicDamage = false;
		boolean hasPhysicalSkill = false;
		for (var entry : skills) {
			var opt = skillTable.get(entry.skillId(), entry.level());
			if (opt.isEmpty()) {
				continue;
			}
			var sk = opt.get();
			if (sk.isPassive() || sk.isToggle() || sk.isBuff() || sk.isHeal()) {
				continue;
			}
			if (sk.isPhysicalDamage() || "STUN".equalsIgnoreCase(sk.skillType())) {
				hasPhysicalSkill = true;
			}
			if (sk.isMagicDamage() && sk.castRange() >= 400) {
				hasLongRangeMagicDamage = true;
			}
		}
		return hasLongRangeMagicDamage && !hasPhysicalSkill;
	}
}
