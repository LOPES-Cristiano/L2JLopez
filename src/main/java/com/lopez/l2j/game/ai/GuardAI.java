package com.lopez.l2j.game.ai;

import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.geodata.GeoEngine;
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
	public void processCombat() {
		if (npc.isDead() || npc.isCasting() || npc.isAttacking()) return;
		if (npc.targetPlayerId() == 0) {
			Integer pkId = scanForCriminals();
			if (pkId != null) {
				npc.targetPlayerId(pkId);
				npc.inCombat(true);
			} else {
				NpcInstance aggroMob = scanForAggressiveMonsters();
				if (aggroMob != null) {
					attackAggressiveMonster(aggroMob);
				}
				return;
			}
		}
		super.processCombat();
	}

	private void attackAggressiveMonster(NpcInstance mob) {
		if (mob == null || mob.isDead()) {
			return;
		}
		double dist = Math.hypot(mob.x() - npc.x(), mob.y() - npc.y());
		int effectiveRange = getEffectiveAttackRange();
		if (dist > effectiveRange + 30) {
			moveTowards(mob.x(), mob.y(), mob.z(), mob.objectId(), effectiveRange, dist);
			return;
		}
		int pAtkSpd = Math.max(100, npc.pAtkSpd());
		long cooldownMs = Math.max(400L, 333_000L / pAtkSpd);
		long now = System.currentTimeMillis();
		if (now - npc.lastAttackTime() >= cooldownMs) {
			npc.lastAttackTime(now);
			npc.attackEndTime(now + cooldownMs);
			int dmg = (int) Math.max(50, npc.pAtk() * 2);
			var atk = new com.lopez.l2j.network.game.packet.GameServerPacket.Attack(
					npc.objectId(), mob.objectId(), dmg, 0, npc.x(), npc.y(), npc.z());
			world.broadcastAround(npc.x(), npc.y(), GameWorld.VISIBILITY_RADIUS, atk);
			if (combatService != null) {
				combatService.applyDamage(mob, dmg, 0);
			} else {
				mob.currentHp(Math.max(0, mob.currentHp() - dmg));
				if (mob.currentHp() <= 0) {
					mob.dead(true);
				}
			}
		}
	}

	@Override
	protected void moveTowards(int targetX, int targetY, int targetZ, int targetObjId, int attackRange, double dist) {
		if (dist <= 0.001) return;
		npc.running(true);
		int stopDistance = Math.max(20, attackRange);
		// Guardas correm 50% mais rapido para interceptar criminosos
		double runSpeed = Math.max(90, (npc.template() != null ? npc.template().runSpd() : npc.runSpd()) * 1.5);
		double step = Math.max(16.0, Math.min(Math.max(0.0, dist - stopDistance), runSpeed * 0.5));

		int goalX = targetX;
		int goalY = targetY;
		int goalZ = targetZ;

		GeoEngine geo = (combatService != null) ? combatService.geoEngine() : null;
		if (geo != null && geo.isEnabled() && com.lopez.l2j.config.Config.ENABLE_PATH_FINDING) {
			if (!geo.canMoveToTarget(npc.x(), npc.y(), npc.z(), targetX, targetY, targetZ)) {
				java.util.List<int[]> path = geo.findPath(npc.x(), npc.y(), npc.z(), targetX, targetY, targetZ, npc.instanceId());
				if (path != null && !path.isEmpty()) {
					int[] wp = path.get(0);
					goalX = wp[0];
					goalY = wp[1];
					goalZ = wp[2];
				}
			}
		}

		double dx = goalX - npc.x();
		double dy = goalY - npc.y();
		double goalDist = Math.hypot(dx, dy);
		if (goalDist <= 0.001) return;
		double actualStep = Math.min(goalDist, step);
		int newX = (int) Math.round(npc.x() + (dx / goalDist) * actualStep);
		int newY = (int) Math.round(npc.y() + (dy / goalDist) * actualStep);
		int heading = (int) Math.round(Math.atan2(dy, dx) * 10430.378);
		int fromX = npc.x();
		int fromY = npc.y();

		int newZ = npc.z();
		if (geo != null && geo.isEnabled()) {
			GeoEngine.Location checked = geo.moveCheck(npc.x(), npc.y(), npc.z(), newX, newY, npc.z());
			newX = checked.x();
			newY = checked.y();
			short geoZ = geo.getHeight(newX, newY, npc.z());
			if (Math.abs(geoZ - npc.z()) <= 100) {
				newZ = geoZ;
			}
		} else {
			int dz = goalZ - npc.z();
			if (Math.abs(dz) <= 48) {
				newZ = goalZ;
			} else if (Math.abs(dz) <= 120) {
				newZ += (int) (Math.signum(dz) * Math.min(Math.abs(dz), 32));
			}
		}

		npc.moveTo(newX, newY, newZ, heading);
		world.updateNpcPosition(npc, fromX, fromY);

		var movePawn = new com.lopez.l2j.network.game.packet.GameServerPacket.MoveToPawn(
				npc.objectId(), targetObjId, stopDistance, npc.x(), npc.y(), npc.z());
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
			if (character != null && !character.isDead() && !character.isGm()
					&& character.karma() > 0 && character.instanceId() == npc.instanceId()) {
				if (Math.abs(session.z() - npc.z()) <= 150) {
					if (hasLineOfSight(character)) {
						return character.objectId();
					}
				}
			}
		}
		return null;
	}

	/**
	 * Quando Config.ALLOW_GUARDS esta ativo (options.properties AllowGuards = True),
	 * guardas de vilas tambem atacam monstros agressivos que se aproximem das entradas.
	 */
	public NpcInstance scanForAggressiveMonsters() {
		if (!com.lopez.l2j.config.Config.ALLOW_GUARDS || world == null) {
			return null;
		}
		var nearbyNpcs = world.findNpcsAround(npc.x(), npc.y(), 600);
		for (NpcInstance mob : nearbyNpcs) {
			if (mob != null && !mob.isDead() && mob.isMonster()
					&& mob.template() != null && mob.template().aggroRange() > 0
					&& Math.abs(mob.z() - npc.z()) <= 150) {
				return mob;
			}
		}
		return null;
	}
}
