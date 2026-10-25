package com.lopez.l2j.game.ai;

import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.geodata.GeoEngine;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcSkillTable;
import com.lopez.l2j.game.skill.SkillTable;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket.MoveToLocation;

/**
 * Arquetipo RangerAI: Monstros arqueiros e de ataque à distância.
 * Mantem distancia de seguranca (kiting) recuando se o jogador se aproximar a menos de 100 unidades,
 * disparando flechas e habilidades de longo alcance.
 */
public class RangerAI extends FighterAI {

	public static final int SAFE_DISTANCE = 100;
	public static final int DESIRED_RANGE = 500;
	private long lastRetreatTime = 0;

	public RangerAI(NpcInstance npc, GameWorld world, CombatService combatService,
			CharTemplateTable charTemplates, NpcSkillTable npcSkillTable, SkillTable skillTable) {
		super(npc, world, combatService, charTemplates, npcSkillTable, skillTable);
	}

	public RangerAI(NpcInstance npc, GameWorld world, CombatService combatService,
			CharTemplateTable charTemplates, NpcSkillTable npcSkillTable, SkillTable skillTable,
			java.util.concurrent.ScheduledExecutorService scheduler, com.lopez.l2j.game.zone.ZoneTable zones) {
		super(npc, world, combatService, charTemplates, npcSkillTable, skillTable, scheduler, zones);
	}

	@Override
	public AiArchetype getArchetype() {
		return AiArchetype.RANGER;
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

		int attackRange = Math.max(500, npc.template().attackRange());

		if (npc.isDisabled()) return;

		long now = System.currentTimeMillis();
		// Kiting: se o alvo colar (< 100u) e nao estiver enraizado (rooted), da um passo de recuo
		if (dist < SAFE_DISTANCE && !npc.isRooted() && (now - lastRetreatTime >= 3000L)) {
			lastRetreatTime = now;
			retreatFromTarget(player.x(), player.y(), dist);
			return;
		}

		// Se estiver fora do alcance de tiro, aproxima-se ate DESIRED_RANGE
		if (dist > attackRange && !npc.isRooted()) {
			moveTowards(player.x(), player.y(), player.z(), player.objectId(), DESIRED_RANGE, dist);
		} else {
			// No alcance seguro: dispara habilidades ofensivas de alcance ou ataques com arco
			com.lopez.l2j.game.skill.SkillTemplate rangedSkill = chooseRangedMonsterSkill(dist);
			if (rangedSkill != null && java.util.concurrent.ThreadLocalRandom.current().nextInt(100) < 25) {
				int pAtkSpd = Math.max(100, npc.pAtkSpd());
				long cooldownMs = Math.max(400L, 333_000L / pAtkSpd);
				if (now - npc.lastAttackTime() >= cooldownMs) {
					npc.lastAttackTime(now);
					var template = charTemplates != null ? charTemplates.get(character.classId()).orElse(null) : null;
					castMonsterSkill(rangedSkill, player, character, template);
					return;
				}
			}
			executeAttack(player, character);
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
		double retreatStep = 120.0;
		int fromX = npc.x();
		int fromY = npc.y();
		int fromZ = npc.z();
		int newX = (int) Math.round(fromX + (dx / dist) * retreatStep);
		int newY = (int) Math.round(fromY + (dy / dist) * retreatStep);
		int newZ = fromZ;

		GeoEngine geo = (combatService != null) ? combatService.geoEngine() : null;
		if (geo != null && geo.isEnabled()) {
			GeoEngine.Location checked = geo.moveCheck(fromX, fromY, fromZ, newX, newY, fromZ);
			newX = checked.x();
			newY = checked.y();
			newZ = checked.z();
		}

		int heading = (int) Math.round(Math.atan2(dy, dx) * 10430.378);

		npc.moveTo(newX, newY, newZ, heading);
		world.updateNpcPosition(npc, fromX, fromY);

		var moveLoc = new MoveToLocation(npc.objectId(), newX, newY, newZ, fromX, fromY, fromZ);
		world.broadcastAround(fromX, fromY, GameWorld.VISIBILITY_RADIUS, moveLoc);
	}
}
