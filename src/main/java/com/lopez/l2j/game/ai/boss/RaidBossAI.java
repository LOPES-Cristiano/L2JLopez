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
		return super.chooseMonsterSkill();
	}

	public void returnHome() {
		npc.targetPlayerId(0);
		npc.inCombat(false);
		npc.clearAggro();
		npc.clearActiveEffects();
		int fromX = npc.x();
		int fromY = npc.y();
		int sx = npc.spawnX();
		int sy = npc.spawnY();
		int sz = npc.spawnZ();
		int sh = npc.spawnHeading();
		npc.moveTo(sx, sy, sz, sh);
		if (world != null) {
			world.updateNpcPosition(npc, fromX, fromY);
			var telePkt = new com.lopez.l2j.network.game.packet.GameServerPacket.TeleportToLocation(npc.objectId(), sx, sy, sz, sh);
			world.broadcastAround(fromX, fromY, GameWorld.VISIBILITY_RADIUS, telePkt);
			world.broadcastAround(sx, sy, GameWorld.VISIBILITY_RADIUS, new com.lopez.l2j.network.game.packet.GameServerPacket.NpcInfo(npc));
		}
		npc.currentHp(npc.maxHp());
	}
}
