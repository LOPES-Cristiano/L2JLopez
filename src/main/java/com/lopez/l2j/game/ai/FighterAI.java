package com.lopez.l2j.game.ai;

import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcSkillTable;
import com.lopez.l2j.game.skill.SkillTable;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket.*;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Arquetipo FighterAI: Monstros de combate corpo-a-corpo padrão.
 * Avanca diretamente em direcao ao alvo ate a distancia de alcance de melee e ataca.
 */
public class FighterAI extends AbstractNpcAI {

	public FighterAI(NpcInstance npc, GameWorld world, CombatService combatService,
			CharTemplateTable charTemplates, NpcSkillTable npcSkillTable, SkillTable skillTable) {
		super(npc, world, combatService, charTemplates, npcSkillTable, skillTable);
	}

	@Override
	public AiArchetype getArchetype() {
		return AiArchetype.FIGHTER;
	}

	@Override
	public void processCombat() {
		if (npc.isDead() || npc.targetPlayerId() == 0) return;
		var playerOpt = world.player(npc.targetPlayerId());
		if (playerOpt.isEmpty()) return;

		var player = playerOpt.get();
		var character = player.character();
		if (character == null || character.isDead()) return;

		double dx = player.x() - npc.x();
		double dy = player.y() - npc.y();
		double dist = Math.hypot(dx, dy);

		if (dist > 1500.0) return;

		int attackRange = Math.max(40, npc.template().attackRange());
		int reach = attackRange + 30;

		if (npc.isDisabled() || (dist > reach && npc.isRooted())) {
			return;
		}

		if (dist > reach) {
			moveTowards(player.x(), player.y(), player.z(), player.objectId(), attackRange, dist);
		} else {
			executeAttack(player, character);
		}
	}

	protected void executeAttack(com.lopez.l2j.game.world.GameWorld.OnlinePlayer player, com.lopez.l2j.game.model.PlayerCharacter character) {
		int pAtkSpd = Math.max(100, npc.template().pAtkSpd());
		long cooldownMs = 500_000L / pAtkSpd;
		long now = System.currentTimeMillis();

		if (now - npc.lastAttackTime() >= cooldownMs) {
			npc.lastAttackTime(now);
			var template = charTemplates.get(character.classId()).orElse(null);
			if (template != null) {
				SkillTemplate chosenSkill = chooseMonsterSkill();
				if (chosenSkill != null) {
					castMonsterSkill(chosenSkill, player, character, template);
				} else {
					var counter = combatService.attackPlayer(npc, character, template);
					if (counter != null) {
						player.onAttacked(npc.objectId(), counter.damage());
						var atk = new Attack(npc.objectId(), character.objectId(), counter.damage(), counter.flags(),
								npc.x(), npc.y(), npc.z());
						player.send(atk);
						world.broadcastAround(player, GameWorld.VISIBILITY_RADIUS, atk, false);

						if (counter.damage() > 0) {
							player.send(SystemMessage.of(SystemMessage.S1_GAVE_YOU_S2_DMG,
									new SystemMessage.NpcName(npc.npcId()),
									new SystemMessage.Number(counter.damage())));
						}
						player.send(StatusUpdate.hp(character.objectId(), (int) character.currentHp(), character.maxHp()));
						player.send(new UserInfo(character, template));

						if (character.isDead()) {
							player.onDeath(npc.objectId());
						}
					}
				}
			}
		}
	}

	protected void castMonsterSkill(SkillTemplate skill, com.lopez.l2j.game.world.GameWorld.OnlinePlayer player,
			com.lopez.l2j.game.model.PlayerCharacter character, com.lopez.l2j.game.template.CharTemplate template) {
		if (skill.mpConsume() > 0) {
			npc.currentMp(Math.max(0, npc.currentMp() - skill.mpConsume()));
		}

		if (skill.isHeal()) {
			double healAmount = skill.power() > 0 ? skill.power() : (npc.template().maxHp() * 0.25);
			npc.currentHp(Math.min(npc.template().maxHp(), npc.currentHp() + healAmount));
			var msu = new MagicSkillUse(npc.objectId(), npc.objectId(), skill.id(), skill.level(),
					skill.hitTime(), skill.reuseDelay(), npc.x(), npc.y(), npc.z(), npc.x(), npc.y(), npc.z());
			player.send(msu);
			world.broadcastAround(npc.x(), npc.y(), GameWorld.VISIBILITY_RADIUS, msu);
		} else {
			var msu = new MagicSkillUse(npc.objectId(), character.objectId(), skill.id(), skill.level(),
					skill.hitTime(), skill.reuseDelay(), npc.x(), npc.y(), npc.z(), player.x(), player.y(), player.z());
			player.send(msu);
			world.broadcastAround(player, GameWorld.VISIBILITY_RADIUS, msu, false);

			var counter = combatService.skillAttackPlayer(npc, character, template, skill.power(),
					skill.isMagicDamage() || skill.magic(), skill.magicLevel());
			if (counter != null) {
				player.onAttacked(npc.objectId(), counter.damage());
				player.send(StatusUpdate.hp(character.objectId(), (int) character.currentHp(), character.maxHp()));
				player.send(new UserInfo(character, template));
				if (character.isDead()) {
					player.onDeath(npc.objectId());
				}
			}
		}
	}
}
