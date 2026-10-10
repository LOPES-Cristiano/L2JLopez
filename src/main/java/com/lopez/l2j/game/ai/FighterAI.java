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

	public FighterAI(NpcInstance npc, GameWorld world, CombatService combatService,
			CharTemplateTable charTemplates, NpcSkillTable npcSkillTable, SkillTable skillTable,
			java.util.concurrent.ScheduledExecutorService scheduler, com.lopez.l2j.game.zone.ZoneTable zones) {
		super(npc, world, combatService, charTemplates, npcSkillTable, skillTable, scheduler, zones);
	}

	@Override
	public AiArchetype getArchetype() {
		return AiArchetype.FIGHTER;
	}

	@Override
	public void processCombat() {
		if (npc.isDead() || npc.isCasting() || npc.isAttacking() || npc.targetPlayerId() == 0) return;
		var playerOpt = world.player(npc.targetPlayerId());
		if (playerOpt.isEmpty()) return;

		var player = playerOpt.get();
		var character = player.character();
		if (character == null || character.isDead() || player.isTeleporting()) return;
		if (zones != null && (zones.isInsidePeace(player.x(), player.y(), player.z())
				|| zones.isInsidePeace(npc.x(), npc.y(), npc.z()))) {
			return;
		}

		double dx = player.x() - npc.x();
		double dy = player.y() - npc.y();
		double dz = player.z() - npc.z();
		double dist = Math.hypot(dx, dy);
		double dist3d = Math.sqrt(dx * dx + dy * dy + dz * dz);

		if (dist3d > 1500.0) return;

		// Diferença vertical severa ou sem linha de visão (ex: monstro na caverna subterrânea e jogador na superfície)
		if (Math.abs(dz) > 250.0 || !combatService.canSeeTarget(npc, character)) {
			return;
		}

		int attackRange = Math.max(40, npc.template().attackRange());
		int reach = attackRange + 30;

		if (npc.isDisabled() || (dist > reach && npc.isRooted())) {
			return;
		}

		if (dist > reach || Math.abs(dz) > 80.0) {
			moveTowards(player.x(), player.y(), player.z(), player.objectId(), attackRange, dist);
		} else {
			executeAttack(player, character);
		}
	}

	protected void executeAttack(com.lopez.l2j.game.world.GameWorld.OnlinePlayer player, com.lopez.l2j.game.model.PlayerCharacter character) {
		if (character.isDead() || npc.isCasting() || npc.isAttacking() || player.isTeleporting()) {
			return;
		}
		if (zones != null && (zones.isInsidePeace(player.x(), player.y(), player.z())
				|| zones.isInsidePeace(npc.x(), npc.y(), npc.z()))) {
			return;
		}

		int attackRange = Math.max(40, npc.template().attackRange());
		boolean isRanged = attackRange >= 300;
		int maxDeltaZ = isRanged ? 350 : 120;
		if (Math.abs(player.z() - npc.z()) > maxDeltaZ) {
			return;
		}

		int pAtkSpd = Math.max(100, npc.template().pAtkSpd());
		long cooldownMs = 500_000L / pAtkSpd;
		long now = System.currentTimeMillis();

		if (now - npc.lastAttackTime() >= cooldownMs) {
			npc.lastAttackTime(now);
			npc.attackEndTime(now + cooldownMs);
			var template = charTemplates.get(character.classId()).orElse(null);
			if (template != null) {
				SkillTemplate chosenSkill = chooseMonsterSkill();
				if (chosenSkill != null) {
					castMonsterSkill(chosenSkill, player, character, template);
				} else {
					int timeToHit = isRanged ? (int) (cooldownMs * 0.70) : (int) (cooldownMs * 0.62);

					var plan = combatService.planAttackPlayerByNpc(npc, character, template);
					if (plan != null) {
						int heading = (int) Math.round(Math.atan2(player.y() - npc.y(), player.x() - npc.x()) * 10430.378);
						npc.heading(heading);

						var atk = new Attack(npc.objectId(), character.objectId(), plan.damage(), plan.flags(),
								npc.x(), npc.y(), npc.z());
						player.send(atk);
						world.broadcastAround(player, GameWorld.VISIBILITY_RADIUS, atk, false);

						var task = scheduler.schedule(() -> {
							if (npc.isDead() || !npc.inCombat() || npc.targetPlayerId() != player.objectId()) {
								return;
							}
							if (character.isDead() || character.invul() || player.isTeleporting()) {
								return;
							}
							if (zones != null && (zones.isInsidePeace(player.x(), player.y(), player.z())
									|| zones.isInsidePeace(npc.x(), npc.y(), npc.z()))) {
								return;
							}
							double curDist = Math.hypot(player.x() - npc.x(), player.y() - npc.y());
							if (curDist > (attackRange + 150) || Math.abs(player.z() - npc.z()) > (isRanged ? 400 : 150)) {
								return;
							}
							if (!combatService.canSeeTarget(npc, character)) {
								return;
							}
							if (plan.miss()) {
								return;
							}

							var hit = combatService.applyDamageToPlayer(npc, character, plan.damage(), plan.flags());
							if (hit != null && hit.damage() > 0) {
								player.onAttacked(npc.objectId(), hit.damage());
								player.send(SystemMessage.of(SystemMessage.S1_GAVE_YOU_S2_DMG,
										new SystemMessage.Text(npc.name()),
										new SystemMessage.Number(hit.damage())));
								player.send(StatusUpdate.hp(character.objectId(), (int) character.currentHp(), character.maxHp()));
								player.send(new UserInfo(character, template));

								if (hit.isDead()) {
									player.onDeath(npc.objectId());
								}
							}
						}, timeToHit, java.util.concurrent.TimeUnit.MILLISECONDS);
						npc.currentAttackTask(task);
					}
				}
			}
		}
	}

	protected void castMonsterSkill(SkillTemplate skill, com.lopez.l2j.game.world.GameWorld.OnlinePlayer player,
			com.lopez.l2j.game.model.PlayerCharacter character, com.lopez.l2j.game.template.CharTemplate template) {
		if ((character.isDead() || player.isTeleporting()) && !skill.isHeal()) {
			return;
		}
		if (!skill.isHeal() && (Math.abs(player.z() - npc.z()) > 400 || !combatService.canSeeTarget(npc, character))) {
			return;
		}
		if (zones != null && (zones.isInsidePeace(player.x(), player.y(), player.z())
				|| zones.isInsidePeace(npc.x(), npc.y(), npc.z()))) {
			return;
		}
		if (skill.mpConsume() > 0) {
			npc.currentMp(Math.max(0, npc.currentMp() - skill.mpConsume()));
		}

		int mAtkSpd = Math.max(100, npc.template().mAtkSpd());
		int effHitTime = Math.max(400, (skill.hitTime() * 333) / mAtkSpd);

		npc.casting(true);

		if (skill.isHeal()) {
			double healAmount = skill.power() > 0 ? skill.power() : (npc.template().maxHp() * 0.25);
			npc.currentHp(Math.min(npc.template().maxHp(), npc.currentHp() + healAmount));
			var msu = new MagicSkillUse(npc.objectId(), npc.objectId(), skill.id(), skill.level(),
					effHitTime, skill.reuseDelay(), npc.x(), npc.y(), npc.z(), npc.x(), npc.y(), npc.z());
			player.send(msu);
			world.broadcastAround(npc.x(), npc.y(), GameWorld.VISIBILITY_RADIUS, msu);
			npc.casting(false);
		} else {
			var msu = new MagicSkillUse(npc.objectId(), character.objectId(), skill.id(), skill.level(),
					effHitTime, skill.reuseDelay(), npc.x(), npc.y(), npc.z(), player.x(), player.y(), player.z());
			player.send(msu);
			world.broadcastAround(player, GameWorld.VISIBILITY_RADIUS, msu, false);

			var task = scheduler.schedule(() -> {
				npc.casting(false);
				if (npc.isDead() || !npc.inCombat() || npc.targetPlayerId() != player.objectId()) {
					return;
				}
				if (character.isDead() || character.invul() || player.isTeleporting()) {
					return;
				}
				if (zones != null && (zones.isInsidePeace(player.x(), player.y(), player.z())
						|| zones.isInsidePeace(npc.x(), npc.y(), npc.z()))) {
					return;
				}
				double curDist = Math.hypot(player.x() - npc.x(), player.y() - npc.y());
				if (curDist > 1500.0 || Math.abs(player.z() - npc.z()) > 400.0) {
					return;
				}
				if (!combatService.canSeeTarget(npc, character)) {
					return;
				}

				boolean isDmg = (skill.isPhysicalDamage() || skill.isMagicDamage()) && skill.power() > 0;
				if (isDmg) {
					var counter = combatService.skillAttackPlayer(npc, character, template, skill.power(),
							skill.isMagicDamage(), skill.magicLevel());
					if (counter != null && counter.damage() > 0) {
						player.onAttacked(npc.objectId(), counter.damage());
						player.send(SystemMessage.of(SystemMessage.S1_GAVE_YOU_S2_DMG,
								new SystemMessage.Text(npc.name()),
								new SystemMessage.Number(counter.damage())));
						player.send(StatusUpdate.hp(character.objectId(), (int) character.currentHp(), character.maxHp()));
						player.send(new UserInfo(character, template));
						if (counter.isDead()) {
							player.onDeath(npc.objectId());
							return;
						}
					}
				} else if (skill.isManaBurn() && skill.power() > 0) {
					int mpDam = combatService.skillManaDamNpcToPlayer(npc, character, template, skill.power(), skill.magicLevel());
					if (mpDam > 0) {
						character.currentMp(Math.max(0, character.currentMp() - mpDam));
						player.send(StatusUpdate.mp(character.objectId(), (int) character.currentMp(), character.maxMp()));
						player.send(SystemMessage.of(SystemMessage.S2_MP_HAS_BEEN_DRAINED_BY_S1,
								new SystemMessage.Text(npc.name()),
								new SystemMessage.Number(mpDam)));
					}
				}

				if (!character.isDead() && player instanceof com.lopez.l2j.network.game.GameSession gs) {
					if (skill.effects().isEmpty()) {
						String st = skill.skillType().toLowerCase(java.util.Locale.ROOT);
						boolean control = st.equals("stun") || st.equals("sleep") || st.equals("paralyze")
								|| st.equals("root") || st.equals("mute") || st.equals("fear");
						if (control) {
							if (combatService.debuffLandsPlayer(skill.power() > 0 ? skill.power() : 50, skill.magicLevel(),
									npc.template().level(), character, false, false)) {
								long until = System.currentTimeMillis() + 15000L;
								gs.applyControlEffect(skill.id(), skill.level(), st, until);
								player.send(SystemMessage.of(SystemMessage.YOU_FEEL_S1_EFFECT,
										new SystemMessage.SkillName(skill.id(), skill.level())));
							}
						}
					}
					for (var e : skill.effects()) {
						String name = e.name().toLowerCase(java.util.Locale.ROOT);
						boolean control = name.equals("stun") || name.equals("sleep") || name.equals("paralyze")
								|| name.equals("root") || name.equals("petrification") || name.equals("medusa")
								|| name.equals("silence") || name.equals("mute") || name.equals("fear")
								|| name.equals("physicalmute") || name.equals("silencemagicphysical");
						if (control) {
							if (combatService.debuffLandsPlayer(skill.power() > 0 ? skill.power() : 50, skill.magicLevel(),
									npc.template().level(), character, false, false)) {
								long until = System.currentTimeMillis() + Math.max(1000, e.durationMs());
								gs.applyControlEffect(skill.id(), skill.level(), name, until);
								player.send(SystemMessage.of(SystemMessage.YOU_FEEL_S1_EFFECT,
										new SystemMessage.SkillName(skill.id(), skill.level())));
							} else {
								player.send(SystemMessage.of(SystemMessage.S1_WAS_UNAFFECTED_BY_S2,
										new SystemMessage.Text(character.name()),
										new SystemMessage.SkillName(skill.id(), skill.level())));
							}
						} else if (name.equals("damovertime") || name.equals("manadamovertime") || name.equals("poison")
								|| name.equals("bleed")) {
							if (combatService.debuffLandsPlayer(skill.power() > 0 ? skill.power() : 50, skill.magicLevel(),
									npc.template().level(), character, false, false)) {
								gs.startSkillDot(skill, e);
							}
						} else if (!e.funcs().isEmpty() || skill.isDebuff() || skill.isOffensive() || name.equals("debuff")) {
							if (combatService.debuffLandsPlayer(skill.power() > 0 ? skill.power() : 50, skill.magicLevel(),
									npc.template().level(), character, false, false)) {
								gs.applySkillEffects(skill, false);
							}
						}
					}
				}
			}, effHitTime, java.util.concurrent.TimeUnit.MILLISECONDS);
			npc.currentCastTask(task);
		}
	}
}
