package com.lopez.l2j.game.ai.boss;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.ai.FighterAI;
import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcSkillTable;
import com.lopez.l2j.game.skill.SkillTable;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Inteligencia Artificial Base para Raid Bosses (L2JDream / L2JLucera2).
 * Frequencia elevada de skills ofensivas e de area, retorno a coordenada de spawn
 * se for puxado para fora do raio maximo ou para zonas de paz/vila, e coordenacao com lacaios.
 */
public class RaidBossAI extends FighterAI {

	public static final int MAX_PURSUE_RANGE = 2500;

	public RaidBossAI(NpcInstance npc, GameWorld world, CombatService combatService,
			CharTemplateTable charTemplates, NpcSkillTable npcSkillTable, SkillTable skillTable) {
		super(npc, world, combatService, charTemplates, npcSkillTable, skillTable);
	}

	public RaidBossAI(NpcInstance npc, GameWorld world, CombatService combatService,
			CharTemplateTable charTemplates, NpcSkillTable npcSkillTable, SkillTable skillTable,
			java.util.concurrent.ScheduledExecutorService scheduler, com.lopez.l2j.game.zone.ZoneTable zones) {
		super(npc, world, combatService, charTemplates, npcSkillTable, skillTable, scheduler, zones);
	}

	@Override
	public void processCombat() {
		if (npc.isDead()) return;

		// 1. Verificacao de retorno ao ponto de spawn (Anti-kiting / Leash)
		double distFromSpawn = Math.hypot(npc.x() - npc.spawnX(), npc.y() - npc.spawnY());
		if (distFromSpawn > MAX_PURSUE_RANGE) {
			returnHome();
			return;
		}

		// 2. Verificacao de zonas proibidas (Vilas / Zonas de Paz / Zonas PvP)
		if (Config.RETURN_HOME_BOSSES_FROM_TOWN && zones != null
				&& zones.isInsidePeace(npc.x(), npc.y(), npc.z())) {
			returnHome();
			return;
		}
		if (Config.RETURN_HOME_BOSSES_FROM_PVP && zones != null
				&& zones.isInsidePvp(npc.x(), npc.y(), npc.z())
				&& !zones.isInsidePvp(npc.spawnX(), npc.spawnY(), npc.spawnZ())) {
			returnHome();
			return;
		}

		// 3. Verificacao de ataque entre zonas diferentes contra Epic/Grand Boss (CAN_ATTACK_FROM_ANOTHER_ZONE_TO_EPIC)
		if (!Config.CAN_ATTACK_FROM_ANOTHER_ZONE_TO_EPIC
				&& npc.template() != null && npc.template().isGrandBoss()
				&& zones != null && world != null && npc.targetPlayerId() != 0) {
			var targetOpt = world.player(npc.targetPlayerId());
			if (targetOpt.isPresent()) {
				var target = targetOpt.get();
				if (!zones.isInSameBossZone(npc.x(), npc.y(), npc.z(), target.x(), target.y(), target.z())) {
					npc.targetPlayerId(0);
					npc.inCombat(false);
					return;
				}
			}
		}

		super.processCombat();
	}

	@Override
	protected SkillTemplate chooseMonsterSkill() {
		if (npcSkillTable == null || skillTable == null) {
			return null;
		}
		var skillList = npcSkillTable.getSkills(npc.npcId());
		if (skillList == null || skillList.isEmpty()) {
			return null;
		}

		// Raid Bosses tem 50% de chance por acao de usar uma de suas habilidades especiais
		if (ThreadLocalRandom.current().nextInt(100) < 50) {
			// Sorteia uma habilidade do boss
			int idx = ThreadLocalRandom.current().nextInt(skillList.size());
			var entry = skillList.get(idx);
			var opt = skillTable.get(entry.skillId(), entry.level());
			if (opt.isPresent() && (npc.currentMp() >= opt.get().mpConsume() || opt.get().mpConsume() == 0)) {
				return opt.get();
			}
		}

		return super.chooseMonsterSkill();
	}

	public void returnHome() {
		npc.targetPlayerId(0);
		npc.inCombat(false);
		int fromX = npc.x();
		int fromY = npc.y();
		npc.moveTo(npc.spawnX(), npc.spawnY(), npc.spawnZ(), npc.spawnHeading());
		world.updateNpcPosition(npc, fromX, fromY);
		npc.currentHp(npc.maxHp());
	}
}
