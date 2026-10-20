package com.lopez.l2j.game.ai;

import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcSkillTable;
import com.lopez.l2j.game.skill.SkillTable;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket.MagicSkillUse;
import com.lopez.l2j.network.game.packet.GameServerPacket.StatusUpdate;

import java.util.List;

/**
 * Arquetipo PriestAI: Curandeiros e suportes (sacerdotes, orcs xamans curadores).
 * Monitora constantemente a vida dos aliados proximos na mesma instanceId.
 * Quando um aliado tem HP < 50%, conjura curas de suporte no aliado prioritariamente,
 * restaurando HP e enviando MagicSkillUse e StatusUpdate.
 */
public class PriestAI extends MysticAI {

	private static final SkillTemplate DEFAULT_PRIEST_HEAL = new SkillTemplate(
			4035, 1, "Priest Heal", SkillTemplate.OperateType.ACTIVE, "HEAL",
			"TARGET_ONE", true, 30, 0, 0, 0.0,
			600, 0, 1500, 500, 3000, 40,
			0.0, false, List.of(), List.of(), null, null);

	public PriestAI(NpcInstance npc, GameWorld world, CombatService combatService,
			CharTemplateTable charTemplates, NpcSkillTable npcSkillTable, SkillTable skillTable) {
		super(npc, world, combatService, charTemplates, npcSkillTable, skillTable);
	}

	public PriestAI(NpcInstance npc, GameWorld world, CombatService combatService,
			CharTemplateTable charTemplates, NpcSkillTable npcSkillTable, SkillTable skillTable,
			java.util.concurrent.ScheduledExecutorService scheduler, com.lopez.l2j.game.zone.ZoneTable zones) {
		super(npc, world, combatService, charTemplates, npcSkillTable, skillTable, scheduler, zones);
	}

	@Override
	public AiArchetype getArchetype() {
		return AiArchetype.PRIEST;
	}

	@Override
	public void processCombat() {
		if (npc.isDead() || npc.isCasting()) return;

		// 1. Antes de atacar, checa se ha aliados feridos (HP < 50%) na mesma instanceId necessitando de cura imediata
		NpcInstance woundedAlly = findWoundedAlly();
		if (woundedAlly != null) {
			SkillTemplate healSkill = findHealSkill();
			if (healSkill != null && npc.currentMp() >= healSkill.mpConsume()) {
				healAlly(woundedAlly, healSkill);
				return;
			}
		}

		// 2. Se nenhum aliado precisar de cura, segue comportamento magico padrao
		if (npc.targetPlayerId() != 0) {
			super.processCombat();
		}
	}

	public NpcInstance findWoundedAlly() {
		// Checa a si mesmo primeiro
		int selfMaxHp = npc.maxHp() > 0 ? (int) npc.maxHp() : (npc.template() != null ? npc.template().maxHp() : 0);
		if (selfMaxHp > 0 && npc.currentHp() < (selfMaxHp * 0.5)) {
			return npc;
		}

		// Checa o mestre se for lacaio
		if (npc.isMinion() && npc.masterObjectId() != 0) {
			var masterOpt = world.npc(npc.masterObjectId());
			if (masterOpt.isPresent() && !masterOpt.get().isDead()) {
				NpcInstance master = masterOpt.get();
				int masterMaxHp = master.maxHp() > 0 ? (int) master.maxHp() : (master.template() != null ? master.template().maxHp() : 0);
				if (master.instanceId() == npc.instanceId() && masterMaxHp > 0 && master.currentHp() < (masterMaxHp * 0.5)) {
					return master;
				}
			}
		}

		// Checa lacaios se for mestre
		if (npc.hasMinions()) {
			for (NpcInstance minion : npc.minions()) {
				if (minion != null && !minion.isDead() && minion.instanceId() == npc.instanceId()) {
					int mMaxHp = minion.maxHp() > 0 ? (int) minion.maxHp() : (minion.template() != null ? minion.template().maxHp() : 0);
					if (mMaxHp > 0 && minion.currentHp() < (mMaxHp * 0.5)) {
						return minion;
					}
				}
			}
		}

		// Checa NPCs aliados proximos (< 600u) na mesma instanceId
		var nearby = world.findNpcsAround(npc.x(), npc.y(), 600);
		String myFaction = (npc.template() != null) ? npc.template().factionId() : null;
		boolean hasFaction = myFaction != null && !myFaction.isBlank();

		if (hasFaction) {
			for (NpcInstance ally : nearby) {
				if (ally == npc || ally.isDead() || ally.instanceId() != npc.instanceId()) continue;
				if (ally.template() != null && myFaction.equalsIgnoreCase(ally.template().factionId())) {
					int allyMaxHp = ally.maxHp() > 0 ? (int) ally.maxHp() : ally.template().maxHp();
					if (allyMaxHp > 0 && ally.currentHp() < (allyMaxHp * 0.5)) {
						return ally;
					}
				}
			}
		}

		// Fallback: qualquer monstro/NPC aliado proximo na mesma instanceId com HP < 50%
		for (NpcInstance ally : nearby) {
			if (ally == npc || ally.isDead() || ally.instanceId() != npc.instanceId()) continue;
			int allyMaxHp = ally.maxHp() > 0 ? (int) ally.maxHp() : (ally.template() != null ? ally.template().maxHp() : 0);
			if (allyMaxHp > 0 && ally.currentHp() < (allyMaxHp * 0.5)) {
				return ally;
			}
		}

		return null;
	}

	private SkillTemplate findHealSkill() {
		if (npcSkillTable != null && skillTable != null) {
			var list = npcSkillTable.getSkills(npc.npcId(), npc.idTemplate());
			if (list != null) {
				for (var entry : list) {
					var opt = skillTable.get(entry.skillId(), entry.level());
					if (opt.isPresent() && !opt.get().isPassive() && !opt.get().isToggle() && opt.get().isHeal()
							&& !npc.isSkillDisabled(opt.get().id())) {
						return opt.get();
					}
				}
			}
		}
		if (!npc.isSkillDisabled(DEFAULT_PRIEST_HEAL.id())) {
			return DEFAULT_PRIEST_HEAL;
		}
		return null;
	}

	private void healAlly(NpcInstance target, SkillTemplate skill) {
		if (target == null || target.isDead()) {
			return;
		}
		int mAtkSpd = Math.max(100, npc.mAtkSpd());
		long cooldownMs = Math.max(400L, 333_000L / mAtkSpd);
		long now = System.currentTimeMillis();

		if (now - npc.lastAttackTime() >= cooldownMs) {
			npc.lastAttackTime(now);
			npc.disableSkill(skill.id(), Math.max(4000L, skill.reuseDelay()));
			if (skill.mpConsume() > 0) {
				npc.currentMp(Math.max(0, npc.currentMp() - skill.mpConsume()));
			}

			int targetMaxHp = target.maxHp() > 0 ? (int) target.maxHp() : (target.template() != null ? target.template().maxHp() : 1000);
			double healAmount = skill.power() > 0 ? skill.power() : (targetMaxHp * 0.30);
			target.currentHp(Math.min(targetMaxHp, target.currentHp() + healAmount));

			var msu = new MagicSkillUse(npc.objectId(), target.objectId(), skill.id(), skill.level(),
					skill.hitTime(), skill.reuseDelay(), npc.x(), npc.y(), npc.z(), target.x(), target.y(), target.z());
			var su = StatusUpdate.hp(target.objectId(), (int) target.currentHp(), targetMaxHp);
			if (npc.targetPlayerId() != 0) {
				world.player(npc.targetPlayerId()).ifPresent(p -> {
					p.send(msu);
					p.send(su);
				});
			}
			world.broadcastAround(npc.x(), npc.y(), GameWorld.VISIBILITY_RADIUS, msu);
			world.broadcastAround(target.x(), target.y(), GameWorld.VISIBILITY_RADIUS, su);
		}
	}
}
