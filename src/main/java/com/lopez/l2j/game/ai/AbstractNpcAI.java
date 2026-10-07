package com.lopez.l2j.game.ai;

import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcSkillTable;
import com.lopez.l2j.game.skill.SkillTable;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket.AutoAttackStop;
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

	public AbstractNpcAI(NpcInstance npc, GameWorld world, CombatService combatService,
			CharTemplateTable charTemplates, NpcSkillTable npcSkillTable, SkillTable skillTable) {
		this.npc = npc;
		this.world = world;
		this.combatService = combatService;
		this.charTemplates = charTemplates;
		this.npcSkillTable = npcSkillTable;
		this.skillTable = skillTable;
	}

	public abstract AiArchetype getArchetype();

	/**
	 * Processamento ciclico de combate do arquetipo.
	 */
	public abstract void processCombat();

	public NpcInstance getNpc() {
		return npc;
	}

	protected SkillTemplate chooseMonsterSkill() {
		if (npcSkillTable == null || skillTable == null) {
			return null;
		}
		var skillList = npcSkillTable.getSkills(npc.npcId());
		if (skillList == null || skillList.isEmpty()) {
			return null;
		}

		// 1. Se estiver ferido (HP < 50%), prioriza habilidades de cura
		boolean lowHp = npc.currentHp() < (npc.template().maxHp() * 0.5);
		if (lowHp) {
			for (var entry : skillList) {
				var opt = skillTable.get(entry.skillId(), entry.level());
				if (opt.isPresent() && opt.get().isHeal() && npc.currentMp() >= opt.get().mpConsume()) {
					return opt.get();
				}
			}
		}

		// 2. Chance de 30% de usar habilidade ofensiva ou debuff
		if (ThreadLocalRandom.current().nextInt(100) < 30) {
			for (var entry : skillList) {
				var opt = skillTable.get(entry.skillId(), entry.level());
				if (opt.isPresent() && !opt.get().isHeal() && npc.currentMp() >= opt.get().mpConsume()) {
					return opt.get();
				}
			}
		}

		return null;
	}

	protected void moveTowards(int targetX, int targetY, int targetZ, int targetObjId, int attackRange, double dist) {
		npc.running(true);
		double runSpeed = Math.max(60, npc.template().runSpd());
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

		var movePawn = new MoveToPawn(npc.objectId(), targetObjId, attackRange, npc.x(), npc.y(), npc.z());
		world.player(targetObjId).ifPresent(p -> p.send(movePawn));
		world.broadcastAround(npc.x(), npc.y(), GameWorld.VISIBILITY_RADIUS, movePawn);
	}
}
