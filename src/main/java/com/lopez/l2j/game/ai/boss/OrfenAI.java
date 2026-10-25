package com.lopez.l2j.game.ai.boss;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcSkillTable;
import com.lopez.l2j.game.skill.SkillTable;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Inteligencia Artificial especifica de Orfen (ai.Orfen do Lucera2 / L2JDream).
 * Se for atacada a distancia (> 300 e < 1000), puxa/teleporta o atacante para a sua frente!
 * Quando o HP atinge menos de 50%, recua para o ninho interno para regenerar.
 */
public class OrfenAI extends RaidBossAI {

	public static final int ORFEN_NEST_X = 43728;
	public static final int ORFEN_NEST_Y = 17220;
	public static final int ORFEN_NEST_Z = -4342;

	private boolean retreated = false;

	public OrfenAI(NpcInstance npc, GameWorld world, CombatService combatService,
			CharTemplateTable charTemplates, NpcSkillTable npcSkillTable, SkillTable skillTable) {
		super(npc, world, combatService, charTemplates, npcSkillTable, skillTable);
	}

	public OrfenAI(NpcInstance npc, GameWorld world, CombatService combatService,
			CharTemplateTable charTemplates, NpcSkillTable npcSkillTable, SkillTable skillTable,
			java.util.concurrent.ScheduledExecutorService scheduler, com.lopez.l2j.game.zone.ZoneTable zones) {
		super(npc, world, combatService, charTemplates, npcSkillTable, skillTable, scheduler, zones);
	}

	@Override
	public void processCombat() {
		if (npc.isDead()) return;

		double hpPercent = (npc.currentHp() / npc.maxHp()) * 100.0;
		if (hpPercent < 50.0 && !retreated) {
			retreated = true;
			int fromX = npc.x();
			int fromY = npc.y();
			npc.moveTo(ORFEN_NEST_X, ORFEN_NEST_Y, ORFEN_NEST_Z, 0);
			if (world != null) {
				world.updateNpcPosition(npc, fromX, fromY);
				var telePkt = new com.lopez.l2j.network.game.packet.GameServerPacket.TeleportToLocation(
						npc.objectId(), ORFEN_NEST_X, ORFEN_NEST_Y, ORFEN_NEST_Z, 0);
				world.broadcastAround(fromX, fromY, GameWorld.VISIBILITY_RADIUS, telePkt);
				world.broadcastAround(ORFEN_NEST_X, ORFEN_NEST_Y, GameWorld.VISIBILITY_RADIUS,
						new com.lopez.l2j.network.game.packet.GameServerPacket.NpcInfo(npc));
			}
			return;
		} else if (hpPercent > 95.0 && retreated) {
			retreated = false;
			int fromX = npc.x();
			int fromY = npc.y();
			int sx = npc.spawnX();
			int sy = npc.spawnY();
			int sz = npc.spawnZ();
			npc.moveTo(sx, sy, sz, npc.spawnHeading());
			if (world != null) {
				world.updateNpcPosition(npc, fromX, fromY);
				var telePkt = new com.lopez.l2j.network.game.packet.GameServerPacket.TeleportToLocation(
						npc.objectId(), sx, sy, sz, npc.spawnHeading());
				world.broadcastAround(fromX, fromY, GameWorld.VISIBILITY_RADIUS, telePkt);
				world.broadcastAround(sx, sy, GameWorld.VISIBILITY_RADIUS,
						new com.lopez.l2j.network.game.packet.GameServerPacket.NpcInfo(npc));
			}
		}

		if (Config.ORFEN_USE_TELEPORT && npc.targetPlayerId() != 0 && world != null) {
			world.player(npc.targetPlayerId()).ifPresent(p -> {
				var ch = p.character();
				if (ch != null && !ch.isDead()) {
					double dist = Math.hypot(ch.x() - npc.x(), ch.y() - npc.y());
					if (dist > 300.0 && dist < 1200.0 && ThreadLocalRandom.current().nextInt(100) < 25) {
						// Puxa o jogador para a frente da Orfen e conjura Paralysis (4064)
						world.teleportPlayer(ch, npc.x() + 50, npc.y() + 50, npc.z(), ch.instanceId());
						var msu = new com.lopez.l2j.network.game.packet.GameServerPacket.MagicSkillUse(
								npc.objectId(), ch.objectId(), 4064, 1, 1500, 3000,
								npc.x(), npc.y(), npc.z(), ch.x(), ch.y(), ch.z());
						p.send(msu);
						world.broadcastAround(npc.x(), npc.y(), GameWorld.VISIBILITY_RADIUS, msu);
					}
				}
			});
		}

		super.processCombat();
	}
}
