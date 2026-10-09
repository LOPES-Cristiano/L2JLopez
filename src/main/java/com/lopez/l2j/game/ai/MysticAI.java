package com.lopez.l2j.game.ai;

import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcSkillTable;
import com.lopez.l2j.game.skill.SkillTable;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.world.GameWorld;

import java.util.concurrent.ThreadLocalRandom;

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
	public void processCombat() {
		if (npc.isDead() || npc.isCasting() || npc.targetPlayerId() == 0) return;
		var playerOpt = world.player(npc.targetPlayerId());
		if (playerOpt.isEmpty()) return;

		var player = playerOpt.get();
		var character = player.character();
		if (character == null || character.isDead() || player.isTeleporting()) return;
		if (zones != null && (zones.isInsidePeace(player.x(), player.y(), player.z())
				|| zones.isInsidePeace(npc.x(), npc.y(), npc.z()))) {
			return;
		}

		double dx = player.x() - npc.x();
		double dy = player.y() - npc.y();
		double dist = Math.hypot(dx, dy);

		if (dist > 1500.0) return;
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
		if (zones != null && (zones.isInsidePeace(player.x(), player.y(), player.z())
				|| zones.isInsidePeace(npc.x(), npc.y(), npc.z()))) {
			return;
		}
		int mAtkSpd = Math.max(100, npc.template().mAtkSpd());
		long cooldownMs = 500_000L / mAtkSpd;
		long now = System.currentTimeMillis();

		if (now - npc.lastAttackTime() >= cooldownMs) {
			npc.lastAttackTime(now);
			var template = charTemplates.get(character.classId()).orElse(null);
			if (template != null) {
				SkillTemplate chosenSkill = chooseMonsterSkill();
				if (chosenSkill != null) {
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
}
