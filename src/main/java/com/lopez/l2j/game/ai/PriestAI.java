package com.lopez.l2j.game.ai;

import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcSkillTable;
import com.lopez.l2j.game.skill.SkillTable;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket.MagicSkillUse;

import java.util.List;

/**
 * Arquetipo PriestAI: Curandeiros e suportes (sacerdotes, orcs xamans curadores).
 * Monitora constantemente a vida dos aliados da mesma faccao ou grupo lacaio.
 * Quando um aliado tem HP < 50%, conjura curas de suporte no aliado prioritariamente.
 */
public class PriestAI extends MysticAI {

	public PriestAI(NpcInstance npc, GameWorld world, CombatService combatService,
			CharTemplateTable charTemplates, NpcSkillTable npcSkillTable, SkillTable skillTable) {
		super(npc, world, combatService, charTemplates, npcSkillTable, skillTable);
	}

	@Override
	public AiArchetype getArchetype() {
		return AiArchetype.PRIEST;
	}

	@Override
	public void processCombat() {
		// 1. Antes de atacar, checa se ha aliados feridos necessitando de cura imediata
		NpcInstance woundedAlly = findWoundedAlly();
		if (woundedAlly != null) {
			SkillTemplate healSkill = findHealSkill();
			if (healSkill != null && npc.currentMp() >= healSkill.mpConsume()) {
				healAlly(woundedAlly, healSkill);
				return;
			}
		}

		// 2. Se nenhum aliado precisar de cura, segue comportamento magico padrao
		super.processCombat();
	}

	private NpcInstance findWoundedAlly() {
		// Checa a si mesmo primeiro
		if (npc.currentHp() < (npc.template().maxHp() * 0.5)) {
			return npc;
		}

		// Checa o mestre se for lacaio
		if (npc.isMinion() && npc.masterObjectId() != 0) {
			var masterOpt = world.npc(npc.masterObjectId());
			if (masterOpt.isPresent() && !masterOpt.get().isDead()) {
				NpcInstance master = masterOpt.get();
				if (master.currentHp() < (master.template().maxHp() * 0.5)) {
					return master;
				}
			}
		}

		// Checa aliados da mesma Faccao por perto (< 600u)
		if (npc.template() != null && npc.template().factionId() != null && !npc.template().factionId().isBlank()) {
			var nearby = world.findNpcsAround(npc.x(), npc.y(), 600);
			for (NpcInstance ally : nearby) {
				if (ally == npc || ally.isDead() || !ally.isMonster()) continue;
				if (npc.template().factionId().equalsIgnoreCase(ally.template().factionId())) {
					if (ally.currentHp() < (ally.template().maxHp() * 0.5)) {
						return ally;
					}
				}
			}
		}

		return null;
	}

	private SkillTemplate findHealSkill() {
		if (npcSkillTable == null || skillTable == null) return null;
		var list = npcSkillTable.getSkills(npc.npcId());
		if (list == null) return null;
		for (var entry : list) {
			var opt = skillTable.get(entry.skillId(), entry.level());
			if (opt.isPresent() && opt.get().isHeal()) {
				return opt.get();
			}
		}
		return null;
	}

	private void healAlly(NpcInstance target, SkillTemplate skill) {
		int mAtkSpd = Math.max(100, npc.template().mAtkSpd());
		long cooldownMs = 500_000L / mAtkSpd;
		long now = System.currentTimeMillis();

		if (now - npc.lastAttackTime() >= cooldownMs) {
			npc.lastAttackTime(now);
			if (skill.mpConsume() > 0) {
				npc.currentMp(Math.max(0, npc.currentMp() - skill.mpConsume()));
			}

			double healAmount = skill.power() > 0 ? skill.power() : (target.template().maxHp() * 0.30);
			target.currentHp(Math.min(target.template().maxHp(), target.currentHp() + healAmount));

			var msu = new MagicSkillUse(npc.objectId(), target.objectId(), skill.id(), skill.level(),
					skill.hitTime(), skill.reuseDelay(), npc.x(), npc.y(), npc.z(), target.x(), target.y(), target.z());
			world.broadcastAround(npc.x(), npc.y(), GameWorld.VISIBILITY_RADIUS, msu);
		}
	}
}
