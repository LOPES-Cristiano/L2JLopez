package com.lopez.l2j.game.ai;

import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcSkillTable;
import com.lopez.l2j.game.skill.SkillTable;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.world.GameWorld;

/**
 * Arquetipo SiegeGuardAI: Guardas de cerco de Castelos e Fortalezas (L2SiegeGuard / L2FortSiegeGuard).
 * Ataca jogadores hostis/atacantes que se aproximam durante cercos ou no perimetro de defesa.
 */
public class SiegeGuardAI extends GuardAI {

	public SiegeGuardAI(NpcInstance npc, GameWorld world, CombatService combatService,
			CharTemplateTable charTemplates, NpcSkillTable npcSkillTable, SkillTable skillTable) {
		super(npc, world, combatService, charTemplates, npcSkillTable, skillTable);
	}

	public SiegeGuardAI(NpcInstance npc, GameWorld world, CombatService combatService,
			CharTemplateTable charTemplates, NpcSkillTable npcSkillTable, SkillTable skillTable,
			java.util.concurrent.ScheduledExecutorService scheduler, com.lopez.l2j.game.zone.ZoneTable zones) {
		super(npc, world, combatService, charTemplates, npcSkillTable, skillTable, scheduler, zones);
	}

	@Override
	public void processCombat() {
		if (npc.isDead() || npc.isCasting() || npc.isAttacking()) {
			return;
		}
		if (npc.targetPlayerId() == 0) {
			Integer targetId = scanForSiegeAttackers();
			if (targetId == null) {
				targetId = scanForCriminals();
			}
			if (targetId != null) {
				npc.targetPlayerId(targetId);
				npc.inCombat(true);
			} else {
				return;
			}
		}
		super.processCombat();
	}

	/**
	 * Varre arredores buscando jogadores em zona de cerco (Siege Zone) que nao sejam GMs/ mortos.
	 */
	public Integer scanForSiegeAttackers() {
		if (world == null) {
			return null;
		}
		int aggroRange = (npc.template() != null && npc.template().aggroRange() > 0)
				? npc.template().aggroRange() : 900;
		var players = world.findPlayersAround(npc.x(), npc.y(), aggroRange);
		for (var session : players) {
			var character = session.character();
			if (character != null && !character.isDead() && !character.isGm()
					&& character.instanceId() == npc.instanceId()) {
				if (Math.abs(session.z() - npc.z()) <= 200) {
					boolean inSiege = (zones != null && zones.isInsideSiege(session.x(), session.y(), session.z()))
							|| character.karma() > 0 || character.pvpFlag() > 0;
					if (inSiege && hasLineOfSight(character)) {
						return character.objectId();
					}
				}
			}
		}
		return null;
	}
}
