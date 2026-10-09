package com.lopez.l2j.game.ai;

import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcSkillTable;
import com.lopez.l2j.game.skill.SkillTable;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.world.GameWorld;

/**
 * Arquetipo GuardAI: Guardas de vilas e castelos (L2Guard).
 * Possui velocidade acelerada (+50% runSpeed) e prioridade absoluta de aggro
 * contra personagens com karma positivo (PKs e criminais).
 */
public class GuardAI extends FighterAI {

	public GuardAI(NpcInstance npc, GameWorld world, CombatService combatService,
			CharTemplateTable charTemplates, NpcSkillTable npcSkillTable, SkillTable skillTable) {
		super(npc, world, combatService, charTemplates, npcSkillTable, skillTable);
	}

	public GuardAI(NpcInstance npc, GameWorld world, CombatService combatService,
			CharTemplateTable charTemplates, NpcSkillTable npcSkillTable, SkillTable skillTable,
			java.util.concurrent.ScheduledExecutorService scheduler, com.lopez.l2j.game.zone.ZoneTable zones) {
		super(npc, world, combatService, charTemplates, npcSkillTable, skillTable, scheduler, zones);
	}

	@Override
	public AiArchetype getArchetype() {
		return AiArchetype.GUARD;
	}

	@Override
	protected void moveTowards(int targetX, int targetY, int targetZ, int targetObjId, int attackRange, double dist) {
		npc.running(true);
		// Guardas correm 50% mais rapido para interceptar criminosos
		double runSpeed = Math.max(90, npc.template().runSpd() * 1.5);
		double step = Math.min(dist - attackRange, runSpeed * 0.5);
		double dx = targetX - npc.x();
		double dy = targetY - npc.y();
		int newX = (int) Math.round(npc.x() + (dx / dist) * step);
		int newY = (int) Math.round(npc.y() + (dy / dist) * step);
		int heading = (int) Math.round(Math.atan2(dy, dx) * 10430.378);
		int fromX = npc.x();
		int fromY = npc.y();
		npc.moveTo(newX, newY, targetZ, heading);
		world.updateNpcPosition(npc, fromX, fromY);

		var movePawn = new com.lopez.l2j.network.game.packet.GameServerPacket.MoveToPawn(
				npc.objectId(), targetObjId, attackRange, npc.x(), npc.y(), npc.z());
		world.player(targetObjId).ifPresent(p -> p.send(movePawn));
		world.broadcastAround(npc.x(), npc.y(), GameWorld.VISIBILITY_RADIUS, movePawn);
	}

	/**
	 * Varre arredores buscando criminais (PK/karma > 0) em raio de 1000 unidades.
	 */
	public Integer scanForCriminals() {
		var players = world.findPlayersAround(npc.x(), npc.y(), 1000);
		for (var session : players) {
			var character = session.character();
			if (character != null && !character.isDead() && !character.isGm() && character.karma() > 0) {
				return character.objectId();
			}
		}
		return null;
	}
}
