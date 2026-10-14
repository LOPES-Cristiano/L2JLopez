package com.lopez.l2j.game.ai;

import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcSkillTable;
import com.lopez.l2j.game.skill.SkillTable;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.world.GameWorld;

/**
 * Arquetipo MysticAI: Monstros magicos (xamans, feiticeiros, espiritos).
 * Prioriza conjuracao de magias de dano (nukes) e controle (sleep/root/stun) a distancia (600u).
 */
public class MysticAI extends FighterAI {

	public static final int CAST_RANGE = 600;

	public MysticAI(NpcInstance npc, GameWorld world, CombatService combatService,
			CharTemplateTable charTemplates, NpcSkillTable npcSkillTable, SkillTable skillTable) {
		super(npc, world, combatService, charTemplates, npcSkillTable, skillTable);
	}

	public MysticAI(NpcInstance npc, GameWorld world, CombatService combatService,
			CharTemplateTable charTemplates, NpcSkillTable npcSkillTable, SkillTable skillTable,
			java.util.concurrent.ScheduledExecutorService scheduler, com.lopez.l2j.game.zone.ZoneTable zones) {
		super(npc, world, combatService, charTemplates, npcSkillTable, skillTable, scheduler, zones);
	}

	@Override
	public AiArchetype getArchetype() {
		return AiArchetype.MYSTIC;
	}

	@Override
	protected SkillTemplate chooseMonsterSkill() {
		if (npcSkillTable == null || skillTable == null) {
			return null;
		}
		var skillList = npcSkillTable.getSkills(npc.npcId());
		if (skillList == null || skillList.isEmpty()) {
			return null;
		}

		// 1. Se estiver ferido (HP < 50%), prioriza habilidades de cura
		if (npc.currentHp() < (npc.maxHp() * 0.5)) {
			for (var entry : skillList) {
				var opt = skillTable.get(entry.skillId(), entry.level());
				if (opt.isPresent() && opt.get().isHeal() && npc.currentMp() >= opt.get().mpConsume()) {
					return opt.get();
				}
			}
		}

		// 2. Mystic prioriza conjurar skills magicas ofensivas / debuffs sempre que tiver MP
		for (var entry : skillList) {
			var opt = skillTable.get(entry.skillId(), entry.level());
			if (opt.isPresent()) {
				SkillTemplate sk = opt.get();
				if (!sk.isHeal() && !sk.isPassive() && !sk.isToggle() && npc.currentMp() >= sk.mpConsume()) {
					return sk;
				}
			}
		}

		return null;
	}

	@Override
	public void processCombat() {
		if (npc.isDead() || npc.isCasting() || npc.isAttacking() || npc.targetPlayerId() == 0) return;
		var playerOpt = world.player(npc.targetPlayerId());
		if (playerOpt.isEmpty()) return;

		var player = playerOpt.get();
		var character = player.character();
		if (character == null || character.isDead() || player.isTeleporting()) return;
		if (isPeaceBlocked(player.x(), player.y(), player.z(), character)) {
			return;
		}

		double dx = player.x() - npc.x();
		double dy = player.y() - npc.y();
		double dz = player.z() - npc.z();
		double dist = Math.hypot(dx, dy);
		double dist3d = Math.sqrt(dx * dx + dy * dy + dz * dz);

		if (dist3d > 1500.0) return;
		if (Math.abs(dz) > 400.0 || !hasLineOfSight(character)) return;
		if (npc.isDisabled()) return;

		// Conjuradores tentam manter alcance de 600u
		if (dist > CAST_RANGE && !npc.isRooted()) {
			moveTowards(player.x(), player.y(), player.z(), player.objectId(), CAST_RANGE - 50, dist);
		} else {
			executeMagicAttack(player, character);
		}
	}

	private void executeMagicAttack(com.lopez.l2j.game.world.GameWorld.OnlinePlayer player, com.lopez.l2j.game.model.PlayerCharacter character) {
		if (character.isDead() || npc.isCasting() || player.isTeleporting()) {
			return;
		}
		if (isPeaceBlocked(player.x(), player.y(), player.z(), character)) {
			return;
		}
		int mAtkSpd = Math.max(100, npc.template().mAtkSpd());
		long cooldownMs = 500_000L / mAtkSpd;
		long now = System.currentTimeMillis();

		if (now - npc.lastAttackTime() >= cooldownMs) {
			SkillTemplate chosenSkill = chooseMonsterSkill();
			var template = charTemplates != null ? charTemplates.get(character.classId()).orElse(null) : null;
			if (chosenSkill != null) {
				npc.lastAttackTime(now);
				castMonsterSkill(chosenSkill, player, character, template);
			} else {
				// Fallback para ataque fisico apenas se estiver no alcance de melee
				int attackRange = Math.max(40, npc.template().attackRange());
				int reach = attackRange + 30;
				double dist = Math.hypot(player.x() - npc.x(), player.y() - npc.y());
				if (dist <= reach) {
					executeAttack(player, character);
				} else if (!npc.isRooted()) {
					moveTowards(player.x(), player.y(), player.z(), player.objectId(), attackRange, dist);
				}
			}
		}
	}
}
