package com.lopez.l2j.game.ai;

import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcSkillTable;
import com.lopez.l2j.game.skill.SkillTable;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket.MoveToLocation;

/**
 * Arquetipo RangerAI: Monstros arqueiros e de ataque à distância.
 * Mantem distancia de seguranca (kiting) recuando se o jogador se aproximar a menos de 150 unidades,
 * disparando flechas e habilidades de longo alcance.
 */
public class RangerAI extends FighterAI {

	public static final int SAFE_DISTANCE = 80;
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

		int attackRange = Math.max(500, npc.template().attackRange());

		if (npc.isDisabled()) return;

		long now = System.currentTimeMillis();
		// Kiting moderado: Apenas se o jogador estiver muito colado (< 80u), nao estiver enraizado (rooted),
		// com cooldown de 8 segundos e 35% de chance (evita teleportes continuos de arqueiros)
		if (dist < SAFE_DISTANCE && !npc.isRooted() && (now - lastRetreatTime >= 8000L)
				&& java.util.concurrent.ThreadLocalRandom.current().nextInt(100) < 35) {
			lastRetreatTime = now;
			retreatFromTarget(player.x(), player.y(), dist);
			return;
		}

		// Se estiver fora do alcance de tiro, aproxima-se ate DESIRED_RANGE
		if (dist > attackRange && !npc.isRooted()) {
			moveTowards(player.x(), player.y(), player.z(), player.objectId(), DESIRED_RANGE, dist);
		} else {
			// No alcance seguro: dispara ataques
			executeAttack(player, character);
		}
	}

	private void retreatFromTarget(int targetX, int targetY, double dist) {
		if (dist <= 0) return;
		double dx = npc.x() - targetX;
		double dy = npc.y() - targetY;
		double retreatStep = 120.0;
		int fromX = npc.x();
		int fromY = npc.y();
		int fromZ = npc.z();
		int newX = (int) Math.round(fromX + (dx / dist) * retreatStep);
		int newY = (int) Math.round(fromY + (dy / dist) * retreatStep);
		int heading = (int) Math.round(Math.atan2(dy, dx) * 10430.378);

		npc.moveTo(newX, newY, fromZ, heading);
		world.updateNpcPosition(npc, fromX, fromY);

		var moveLoc = new MoveToLocation(npc.objectId(), newX, newY, fromZ, fromX, fromY, fromZ);
		world.broadcastAround(fromX, fromY, GameWorld.VISIBILITY_RADIUS, moveLoc);
	}
}
