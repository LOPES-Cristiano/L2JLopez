package com.lopez.l2j.game.ai.boss;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.boss.epic.ZakenService;
import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcSkillTable;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.skill.SkillTable;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Inteligencia Artificial especifica de Zaken (ai.ZakenNightly do Lucera2 / L2JDream).
 * Teleporta jogadores aleatoriamente para dentro das celas do navio pirata (habilidade 4216),
 * teleporta a si mesmo para corredores escuros invocando hordas de piratas mortos-vivos (4222).
 */
public class ZakenAI extends RaidBossAI {

	public static final int SKILL_TELEPORT_PLAYER = 4216;
	public static final int SKILL_TELEPORT_ZAKEN = 4222;
	public static final int PIRATE_MINION = 29023;

	private long lastTeleportPlayerTime = 0;
	private long lastTeleportZakenTime = 0;

	public ZakenAI(NpcInstance npc, GameWorld world, CombatService combatService,
			CharTemplateTable charTemplates, NpcSkillTable npcSkillTable, SkillTable skillTable) {
		super(npc, world, combatService, charTemplates, npcSkillTable, skillTable);
	}

	@Override
	public void processCombat() {
		if (npc.isDead()) return;

		long now = System.currentTimeMillis();

		if (Config.ZAKEN_USE_TELEPORT) {
			// A cada 90s, chance de teleportar jogador alvo para cela aleatoria do navio
			if (now - lastTeleportPlayerTime >= 90_000L && npc.targetPlayerId() != 0 && world != null) {
				lastTeleportPlayerTime = now;
				world.player(npc.targetPlayerId()).ifPresent(p -> {
					var ch = p.character();
					if (ch != null && !ch.isDead()) {
						int idx = ThreadLocalRandom.current().nextInt(ZakenService.SHIP_ROOMS.length);
						int[] room = ZakenService.SHIP_ROOMS[idx];
						ch.x(room[0]);
						ch.y(room[1]);
						ch.z(room[2]);
					}
				});
			}

			// A cada 240s, Zaken se teleporta para outra sala do navio e invoca piratas
			if (now - lastTeleportZakenTime >= 240_000L && world != null) {
				lastTeleportZakenTime = now;
				int idx = ThreadLocalRandom.current().nextInt(ZakenService.SHIP_ROOMS.length);
				int[] room = ZakenService.SHIP_ROOMS[idx];
				int fromX = npc.x();
				int fromY = npc.y();
				npc.moveTo(room[0], room[1], room[2], 0);
				world.updateNpcPosition(npc, fromX, fromY);
			}
		}

		super.processCombat();
	}
}
