package com.lopez.l2j.game.ai;

import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.geodata.GeoEngine;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcSkillTable;
import com.lopez.l2j.game.skill.SkillTable;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket.MoveToPawn;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Classe abstrata base para os Arquétipos de Inteligencia Artificial (L2J / Lucera2).
 */
public abstract class AbstractNpcAI {

	public enum AiArchetype {
		FIGHTER,
		RANGER,
		MYSTIC,
		PRIEST,
		GUARD
	}

	protected final NpcInstance npc;
	protected final GameWorld world;
	protected final CombatService combatService;
	protected final CharTemplateTable charTemplates;
	protected final NpcSkillTable npcSkillTable;
	protected final SkillTable skillTable;
	protected final java.util.concurrent.ScheduledExecutorService scheduler;
	protected final com.lopez.l2j.game.zone.ZoneTable zones;

	public AbstractNpcAI(NpcInstance npc, GameWorld world, CombatService combatService,
			CharTemplateTable charTemplates, NpcSkillTable npcSkillTable, SkillTable skillTable) {
		this(npc, world, combatService, charTemplates, npcSkillTable, skillTable, null, null);
	}

	public AbstractNpcAI(NpcInstance npc, GameWorld world, CombatService combatService,
			CharTemplateTable charTemplates, NpcSkillTable npcSkillTable, SkillTable skillTable,
			java.util.concurrent.ScheduledExecutorService scheduler, com.lopez.l2j.game.zone.ZoneTable zones) {
		this.npc = npc;
		this.world = world;
		this.combatService = combatService;
		this.charTemplates = charTemplates;
		this.npcSkillTable = npcSkillTable;
		this.skillTable = skillTable;
		this.scheduler = scheduler != null ? scheduler : com.lopez.l2j.network.game.GameSession.autoAttackScheduler();
		this.zones = zones;
		if (npc != null) {
			npc.applyPassiveSkills(npcSkillTable, skillTable);
		}
	}

	public abstract AiArchetype getArchetype();

	/**
	 * Processamento ciclico de combate do arquetipo.
	 */
	public abstract void processCombat();

	public NpcInstance getNpc() {
		return npc;
	}

	protected boolean isGuardNpc() {
		if (getArchetype() == AiArchetype.GUARD) {
			return true;
		}
		if (npc != null && npc.template() != null) {
			String type = npc.template().type();
			String name = npc.template().name() != null ? npc.template().name().toLowerCase(java.util.Locale.ROOT) : "";
			return "L2Guard".equalsIgnoreCase(type) || npc.template().isGuard() || name.contains("guard") || name.contains("sentry");
		}
		return false;
	}

	protected boolean isPeaceBlocked(int px, int py, int pz, com.lopez.l2j.game.model.PlayerCharacter character) {
		if (zones == null) {
			return false;
		}
		if (isGuardNpc() && character != null && character.karma() > 0) {
			return false;
		}
		boolean insidePeace = zones.isInsidePeace(px, py, pz) || zones.isInsidePeace(npc.x(), npc.y(), npc.z());
		return insidePeace && (!com.lopez.l2j.config.Config.ALT_MOB_AGGRO_IN_PEACE_ZONE || zones != null);
	}

	protected boolean hasLineOfSight(com.lopez.l2j.game.model.PlayerCharacter character) {
		if (character == null) {
			return false;
		}
		if (npc.instanceId() != character.instanceId()) {
			return false;
		}
		if (combatService == null) {
			return Math.abs(character.z() - npc.z()) <= 250;
		}
		if (combatService.geoEngine() == null) {
			return Math.abs(character.z() - npc.z()) <= 250;
		}
		return combatService.canSeeTarget(npc, character);
	}

	protected int getEffectiveAttackRange() {
		int baseRange = Math.max(40, npc.template() != null ? npc.template().attackRange() : 40);
		int npcRadius = npc.template() != null ? (int) Math.ceil(npc.template().collisionRadius()) : 10;
		return baseRange + npcRadius + 20; // inclui collisionRadius do monstro/boss + raio do player
	}

	protected SkillTemplate chooseMonsterSkill() {
		if (npcSkillTable == null || skillTable == null) {
			return null;
		}
		var skillList = npcSkillTable.getSkills(npc.npcId(), npc.idTemplate());
		if (skillList == null || skillList.isEmpty()) {
			return null;
		}

		// 1. Se estiver ferido (HP < 50%), prioriza habilidades de cura ativas disponiveis
		boolean lowHp = npc.currentHp() < (npc.maxHp() * 0.5);
		if (lowHp) {
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

		// 2. Chance equilibrada (25% em monstros normais, 35% em RaidBosses) de usar habilidade ativa
		int skillChance = npc.isRaid() ? 35 : 25;
		if (ThreadLocalRandom.current().nextInt(100) < skillChance) {
			java.util.List<SkillTemplate> candidates = new java.util.ArrayList<>();
			for (var entry : skillList) {
				var opt = skillTable.get(entry.skillId(), entry.level());
				if (opt.isEmpty()) {
					continue;
				}
				SkillTemplate sk = opt.get();
				if (sk.isPassive() || sk.isToggle() || sk.isHeal()) {
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
			if (!candidates.isEmpty()) {
				return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
			}
		}

		return null;
	}

	protected void moveTowards(int targetX, int targetY, int targetZ, int targetObjId, int attackRange, double dist) {
		if (dist <= 0.001) {
			return;
		}
		npc.running(true);
		int stopDistance = Math.max(20, attackRange);
		double runSpeed = Math.max(60, npc.runSpd());
		double step = Math.max(16.0, Math.min(Math.max(0.0, dist - stopDistance), runSpeed * 0.5));

		int goalX = targetX;
		int goalY = targetY;
		int goalZ = targetZ;

		GeoEngine geo = (combatService != null) ? combatService.geoEngine() : null;
		if (geo != null && geo.isEnabled() && com.lopez.l2j.config.Config.ENABLE_PATH_FINDING) {
			if (!geo.canMoveToTarget(npc.x(), npc.y(), npc.z(), targetX, targetY, targetZ)) {
				List<int[]> path = geo.findPath(npc.x(), npc.y(), npc.z(), targetX, targetY, targetZ, npc.instanceId());
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
		if (goalDist <= 0.001) {
			return;
		}
		double actualStep = Math.min(goalDist, step);
		int newX = (int) Math.round(npc.x() + (dx / goalDist) * actualStep);
		int newY = (int) Math.round(npc.y() + (dy / goalDist) * actualStep);
		int heading = (int) Math.round(Math.atan2(dy, dx) * 10430.378);
		int fromX = npc.x();
		int fromY = npc.y();

		// Calculo de Z seguro: nunca teletransporta monstro subterraneo para a superficie!
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

		var movePawn = new MoveToPawn(npc.objectId(), targetObjId, stopDistance, npc.x(), npc.y(), npc.z());
		world.player(targetObjId).ifPresent(p -> p.send(movePawn));
		world.broadcastAround(npc.x(), npc.y(), GameWorld.VISIBILITY_RADIUS, movePawn);

		// Se for lider com minions e os minions estiverem ociosos/distantes, acompanha a formacao do lider (#312)
		if (npc.hasMinions()) {
			moveMinionsWithMaster(geo);
		}
	}

	protected void moveMinionsWithMaster(GeoEngine geo) {
		if (!npc.hasMinions() || world == null) {
			return;
		}
		int idx = 0;
		int total = Math.max(1, npc.minions().size());
		for (NpcInstance minion : npc.minions()) {
			idx++;
			if (minion == null || minion.isDead() || minion.isCasting() || minion.isAttacking()) {
				continue;
			}
			double distToMaster = Math.hypot(minion.x() - npc.x(), minion.y() - npc.y());
			if (distToMaster <= 180.0) {
				continue;
			}
			double angle = (2 * Math.PI * idx) / total;
			int offset = 60;
			int mx = npc.x() + (int) Math.round(Math.cos(angle) * offset);
			int my = npc.y() + (int) Math.round(Math.sin(angle) * offset);
			int mz = npc.z();
			if (geo != null && geo.isEnabled()) {
				GeoEngine.Location checked = geo.moveCheck(minion.x(), minion.y(), minion.z(), mx, my, mz);
				mx = checked.x();
				my = checked.y();
				mz = checked.z();
			}
			int mFromX = minion.x();
			int mFromY = minion.y();
			int mFromZ = minion.z();
			minion.moveTo(mx, my, mz, npc.heading());
			world.updateNpcPosition(minion, mFromX, mFromY);
			var movePkt = new com.lopez.l2j.network.game.packet.GameServerPacket.MoveToLocation(
					minion.objectId(), mx, my, mz, mFromX, mFromY, mFromZ);
			world.broadcastAround(mx, my, GameWorld.VISIBILITY_RADIUS, movePkt);
		}
	}
}
