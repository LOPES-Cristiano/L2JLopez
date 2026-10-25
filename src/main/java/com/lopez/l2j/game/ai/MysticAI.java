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
	public static final int SAFE_DISTANCE = 100;
	private long lastRetreatTime = 0;

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
		var skillList = npcSkillTable.getSkills(npc.npcId(), npc.idTemplate());
		if (skillList == null || skillList.isEmpty()) {
			return null;
		}

		// 1. Se estiver ferido (HP < 50%), prioriza habilidades de cura
		if (npc.currentHp() < (npc.maxHp() * 0.5)) {
			for (var entry : skillList) {
				var opt = skillTable.get(entry.skillId(), entry.level());
				if (opt.isPresent()) {
					SkillTemplate sk = opt.get();
					if (!sk.isPassive() && !sk.isToggle() && sk.isHeal()
							&& !npc.isSkillDisabled(sk.id())
							&& npc.currentMp() >= sk.mpConsume()) {
						return sk;
					}
				}
			}
		}

		// 2. Mystic prioriza conjurar skills magicas ofensivas / debuffs sempre que tiver MP e fora de cooldown
		java.util.List<SkillTemplate> candidates = new java.util.ArrayList<>();
		for (var entry : skillList) {
			var opt = skillTable.get(entry.skillId(), entry.level());
			if (opt.isPresent()) {
				SkillTemplate sk = opt.get();
				if (sk.isHeal() || sk.isPassive() || sk.isToggle()) {
					continue;
				}
				if (npc.isSkillDisabled(sk.id()) || npc.currentMp() < sk.mpConsume()) {
					continue;
				}
				if (sk.isMagic() && npc.isMuted()) {
					continue;
				}
				if (!sk.isMagic() && npc.isPhysicalMuted()) {
					continue;
				}
				boolean isSelfBuff = sk.isBuff() || "TARGET_SELF".equalsIgnoreCase(sk.target());
				if (isSelfBuff && npc.hasActiveBuff(sk.id())) {
					continue;
				}
				candidates.add(sk);
			}
		}
		if (!candidates.isEmpty()) {
			return candidates.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(candidates.size()));
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

		SkillTemplate chosenSkill = chooseMonsterSkill();
		if (chosenSkill != null) {
			long now = System.currentTimeMillis();
			if (dist < SAFE_DISTANCE && !npc.isRooted() && !chosenSkill.isHeal() && (now - lastRetreatTime >= 3500L)) {
				lastRetreatTime = now;
				retreatFromTarget(player.x(), player.y(), dist);
			}
			int castRange = chosenSkill.castRange() > 0 ? chosenSkill.castRange() : CAST_RANGE;
			if (dist > castRange && !npc.isRooted()) {
				moveTowards(player.x(), player.y(), player.z(), player.objectId(), Math.max(40, castRange - 50), dist);
			} else {
				executeMagicAttack(player, character, chosenSkill);
			}
		} else {
			// Quando as skills estiverem em cooldown ou MP acabar, avanca ate getEffectiveAttackRange() e ataca fisicamente
			int effectiveRange = getEffectiveAttackRange();
			int reach = effectiveRange + 30;
			if (dist <= reach) {
				executeAttack(player, character);
			} else if (!npc.isRooted()) {
				moveTowards(player.x(), player.y(), player.z(), player.objectId(), effectiveRange, dist);
				double newDist = Math.hypot(player.x() - npc.x(), player.y() - npc.y());
				if (newDist <= reach) {
					executeAttack(player, character);
				}
			}
		}
	}

	private void executeMagicAttack(com.lopez.l2j.game.world.GameWorld.OnlinePlayer player,
			com.lopez.l2j.game.model.PlayerCharacter character, SkillTemplate chosenSkill) {
		if (character.isDead() || npc.isCasting() || player.isTeleporting()) {
			return;
		}
		if (isPeaceBlocked(player.x(), player.y(), player.z(), character)) {
			return;
		}
		int mAtkSpd = Math.max(100, npc.mAtkSpd());
		long cooldownMs = Math.max(400L, 333_000L / mAtkSpd);
		long now = System.currentTimeMillis();

		if (now - npc.lastAttackTime() >= cooldownMs) {
			var template = charTemplates != null ? charTemplates.get(character.classId()).orElse(null) : null;
			if (chosenSkill != null) {
				npc.lastAttackTime(now);
				castMonsterSkill(chosenSkill, player, character, template);
			}
		}
	}

	private void retreatFromTarget(int targetX, int targetY, double dist) {
		double dx = npc.x() - targetX;
		double dy = npc.y() - targetY;
		if (dist <= 0.001) {
			dx = 1.0;
			dy = 0.0;
			dist = 1.0;
		}
		double retreatStep = 110.0;
		int fromX = npc.x();
		int fromY = npc.y();
		int fromZ = npc.z();
		int newX = (int) Math.round(fromX + (dx / dist) * retreatStep);
		int newY = (int) Math.round(fromY + (dy / dist) * retreatStep);
		int newZ = fromZ;

		com.lopez.l2j.game.geodata.GeoEngine geo = (combatService != null) ? combatService.geoEngine() : null;
		if (geo != null && geo.isEnabled()) {
			com.lopez.l2j.game.geodata.GeoEngine.Location checked = geo.moveCheck(fromX, fromY, fromZ, newX, newY, fromZ);
			newX = checked.x();
			newY = checked.y();
			newZ = checked.z();
		}

		int heading = (int) Math.round(Math.atan2(dy, dx) * 10430.378);
		npc.moveTo(newX, newY, newZ, heading);
		world.updateNpcPosition(npc, fromX, fromY);

		var moveLoc = new com.lopez.l2j.network.game.packet.GameServerPacket.MoveToLocation(
				npc.objectId(), newX, newY, newZ, fromX, fromY, fromZ);
		world.broadcastAround(fromX, fromY, GameWorld.VISIBILITY_RADIUS, moveLoc);
	}
}
