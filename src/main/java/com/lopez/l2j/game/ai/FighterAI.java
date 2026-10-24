package com.lopez.l2j.game.ai;

import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcSkillTable;
import com.lopez.l2j.game.skill.SkillTable;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket.*;

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
		if (isPeaceBlocked(player.x(), player.y(), player.z(), character)) {
			return;
		}

		double dx = player.x() - npc.x();
		double dy = player.y() - npc.y();
		double dz = player.z() - npc.z();
		double dist = Math.hypot(dx, dy);
		double dist3d = Math.sqrt(dx * dx + dy * dy + dz * dz);

		if (dist3d > 1500.0) return;

		// Diferença vertical severa ou sem linha de visão (ex: monstro na caverna subterrânea e jogador na superfície)
		if (Math.abs(dz) > 250.0 || !hasLineOfSight(character)) {
			return;
		}

		int effectiveRange = getEffectiveAttackRange();
		int reach = effectiveRange + 30;

		if (npc.isDisabled() || (dist > reach && npc.isRooted())) {
			return;
		}

		boolean isTallOrBoss = npc.isRaid()
				|| (npc.template() != null && (npc.template().isRaidBoss() || npc.template().isGrandBoss() || npc.template().collisionHeight() > 30));
		double maxAllowedDz = isTallOrBoss ? 250.0 : 80.0;

		if (dist > reach || Math.abs(dz) > maxAllowedDz) {
			if (dist > reach && dist <= 600.0
					&& java.util.concurrent.ThreadLocalRandom.current().nextInt(100) < 20) {
				SkillTemplate rangedSkill = chooseRangedMonsterSkill(dist);
				if (rangedSkill != null) {
					long now = System.currentTimeMillis();
					npc.lastAttackTime(now);
					var template = charTemplates != null ? charTemplates.get(character.classId()).orElse(null) : null;
					castMonsterSkill(rangedSkill, player, character, template);
					return;
				}
			}
			moveTowards(player.x(), player.y(), player.z(), player.objectId(), effectiveRange, dist);
			double newDist = Math.hypot(player.x() - npc.x(), player.y() - npc.y());
			double newDz = player.z() - npc.z();
			if (newDist <= reach && Math.abs(newDz) <= maxAllowedDz) {
				executeAttack(player, character);
			}
		} else {
			executeAttack(player, character);
		}
	}

	protected SkillTemplate chooseRangedMonsterSkill(double dist) {
		if (npcSkillTable == null || skillTable == null) {
			return null;
		}
		var skillList = npcSkillTable.getSkills(npc.npcId(), npc.idTemplate());
		if (skillList == null || skillList.isEmpty()) {
			return null;
		}
		java.util.List<SkillTemplate> candidates = new java.util.ArrayList<>();
		for (var entry : skillList) {
			var opt = skillTable.get(entry.skillId(), entry.level());
			if (opt.isEmpty()) {
				continue;
			}
			SkillTemplate sk = opt.get();
			if (sk.isPassive() || sk.isToggle() || sk.isHeal() || sk.isBuff() || "TARGET_SELF".equalsIgnoreCase(sk.target())) {
				continue;
			}
			if (sk.castRange() < dist || npc.isSkillDisabled(sk.id()) || npc.currentMp() < sk.mpConsume()) {
				continue;
			}
			if (sk.isMagic() && npc.isMuted()) {
				continue;
			}
			if (!sk.isMagic() && npc.isPhysicalMuted()) {
				continue;
			}
			candidates.add(sk);
		}
		if (candidates.isEmpty()) {
			return null;
		}
		return candidates.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(candidates.size()));
	}

	protected void executeAttack(com.lopez.l2j.game.world.GameWorld.OnlinePlayer player, com.lopez.l2j.game.model.PlayerCharacter character) {
		if (character.isDead() || npc.isCasting() || npc.isAttacking() || player.isTeleporting()) {
			return;
		}
		if (isPeaceBlocked(player.x(), player.y(), player.z(), character)) {
			return;
		}

		int attackRange = Math.max(40, npc.template() != null ? npc.template().attackRange() : 40);
		boolean isRanged = attackRange >= 300;
		boolean isTallOrBoss = npc.isRaid()
				|| (npc.template() != null && (npc.template().isRaidBoss() || npc.template().isGrandBoss() || npc.template().collisionHeight() > 30));
		int maxDeltaZ = isRanged ? 350 : (isTallOrBoss ? 250 : 150);
		if (Math.abs(player.z() - npc.z()) > maxDeltaZ) {
			return;
		}

		int pAtkSpd = Math.max(100, npc.pAtkSpd());
		long cooldownMs = Math.max(400L, 333_000L / pAtkSpd);
		long now = System.currentTimeMillis();

		if (now - npc.lastAttackTime() >= cooldownMs) {
			npc.lastAttackTime(now);
			npc.attackEndTime(now + cooldownMs);
			var template = charTemplates != null ? charTemplates.get(character.classId()).orElse(null) : null;
			SkillTemplate chosenSkill = chooseMonsterSkill();
			if (chosenSkill != null) {
				castMonsterSkill(chosenSkill, player, character, template);
			} else if (template != null && combatService != null) {
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
						if (isPeaceBlocked(player.x(), player.y(), player.z(), character)) {
							return;
						}
						double curDist = Math.hypot(player.x() - npc.x(), player.y() - npc.y());
						if (curDist > (getEffectiveAttackRange() + 180) || Math.abs(player.z() - npc.z()) > (isRanged ? 400 : 250)) {
							return;
						}
						if (!hasLineOfSight(character)) {
							return;
						}
						if (plan.miss()) {
							player.send(SystemMessage.of(SystemMessage.AVOIDED_S1_ATTACK,
									new SystemMessage.NpcName(npc.npcId())));
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

							// Reflect Damage em ataques melee de monstros (Song of Vengeance 266, Chant of Revenge 1284, Reflect Damage 86)
							if (!isRanged && !npc.isDead()) {
								double reflectPct = com.lopez.l2j.game.model.PlayerStats.applyStat(character, npc, "reflectDam", 0.0);
								if (reflectPct > 0.0) {
									int reflectedDmg = (int) Math.round(hit.damage() * (reflectPct / 100.0));
									if (reflectedDmg > 0) {
										var refHit = combatService.applyDamage(npc, reflectedDmg, 0);
										var suNpc = StatusUpdate.hp(npc.objectId(), refHit.isDead() ? 0 : Math.max(0, refHit.remainingHp()), (int) npc.maxHp());
										player.send(suNpc);
										world.broadcastAround(npc.x(), npc.y(), GameWorld.VISIBILITY_RADIUS, suNpc);
									}
								}
							}

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

	protected void castMonsterSkill(SkillTemplate skill, com.lopez.l2j.game.world.GameWorld.OnlinePlayer player,
			com.lopez.l2j.game.model.PlayerCharacter character, com.lopez.l2j.game.template.CharTemplate template) {
		boolean isSelfSkill = skill.isHeal() || skill.isBuff() || "TARGET_SELF".equalsIgnoreCase(skill.target());
		if (!isSelfSkill && (character.isDead() || player.isTeleporting())) {
			return;
		}
		if (!isSelfSkill && (Math.abs(player.z() - npc.z()) > 400 || !hasLineOfSight(character))) {
			return;
		}
		if (!isSelfSkill && isPeaceBlocked(player.x(), player.y(), player.z(), character)) {
			return;
		}
		if (skill.mpConsume() > 0) {
			npc.currentMp(Math.max(0, npc.currentMp() - skill.mpConsume()));
		}

		int spd = skill.isMagic() ? Math.max(100, npc.mAtkSpd()) : Math.max(100, npc.pAtkSpd());
		int effHitTime = Math.max(400, (skill.hitTime() * 333) / spd);

		boolean isSelfBuff = !skill.isHeal() && (skill.isBuff() || "TARGET_SELF".equalsIgnoreCase(skill.target()));
		long reuseMs = isSelfBuff
				? Math.max(30_000L, skill.reuseDelay())
				: Math.max(4000L, skill.reuseDelay());
		npc.disableSkill(skill.id(), reuseMs);
		npc.attackEndTime(System.currentTimeMillis() + effHitTime + 200);
		npc.casting(true);

		if (skill.isHeal()) {
			double healAmount = skill.power() > 0 ? skill.power() : (npc.maxHp() * 0.25);
			npc.currentHp(Math.min(npc.maxHp(), npc.currentHp() + healAmount));
			var msu = new MagicSkillUse(npc.objectId(), npc.objectId(), skill.id(), skill.level(),
					effHitTime, skill.reuseDelay(), npc.x(), npc.y(), npc.z(), npc.x(), npc.y(), npc.z());
			var su = StatusUpdate.hp(npc.objectId(), (int) npc.currentHp(), (int) npc.maxHp());
			if (player != null) {
				player.send(msu);
				player.send(su);
			}
			world.broadcastAround(npc.x(), npc.y(), GameWorld.VISIBILITY_RADIUS, msu);
			world.broadcastAround(npc.x(), npc.y(), GameWorld.VISIBILITY_RADIUS, su);
			npc.casting(false);
			npc.attackEndTime(System.currentTimeMillis() + 300);
		} else if (isSelfBuff) {
			npc.applySkillBuff(skill);
			var msu = new MagicSkillUse(npc.objectId(), npc.objectId(), skill.id(), skill.level(),
					effHitTime, skill.reuseDelay(), npc.x(), npc.y(), npc.z(), npc.x(), npc.y(), npc.z());
			if (player != null) {
				player.send(msu);
			}
			world.broadcastAround(npc.x(), npc.y(), GameWorld.VISIBILITY_RADIUS, msu);
			npc.casting(false);
			npc.attackEndTime(System.currentTimeMillis() + 300);
		} else {
			var msu = new MagicSkillUse(npc.objectId(), character.objectId(), skill.id(), skill.level(),
					effHitTime, skill.reuseDelay(), npc.x(), npc.y(), npc.z(), player.x(), player.y(), player.z());
			player.send(msu);
			world.broadcastAround(player, GameWorld.VISIBILITY_RADIUS, msu, false);

			var task = scheduler.schedule(() -> {
				npc.casting(false);
				npc.attackEndTime(System.currentTimeMillis() + 300);
				if (npc.isDead() || !npc.inCombat() || npc.targetPlayerId() != player.objectId()) {
					return;
				}
				if (character.isDead() || character.invul() || player.isTeleporting()) {
					return;
				}
				if (isPeaceBlocked(player.x(), player.y(), player.z(), character)) {
					return;
				}
				double curDist = Math.hypot(player.x() - npc.x(), player.y() - npc.y());
				if (curDist > 1500.0 || Math.abs(player.z() - npc.z()) > 400.0) {
					return;
				}
				if (!hasLineOfSight(character)) {
					return;
				}

				applyOffensiveSkillToTarget(skill, player, character, template);

				boolean isAura = skill.isAreaAroundSelf();
				boolean isArea = skill.isAreaAroundTarget() || "TARGET_AREA".equalsIgnoreCase(skill.target());
				if ((isAura || isArea) && world != null) {
					int radius = skill.skillRadius() > 0 ? skill.skillRadius() : 200;
					int centerX = isAura ? npc.x() : player.x();
					int centerY = isAura ? npc.y() : player.y();
					var nearbyPlayers = world.findPlayersAround(centerX, centerY, radius, npc.instanceId());
					for (var otherPlayer : nearbyPlayers) {
						if (otherPlayer == null || otherPlayer.objectId() == player.objectId() || otherPlayer.isTeleporting()) {
							continue;
						}
						var otherChar = otherPlayer.character();
						if (otherChar == null || otherChar.isDead() || otherChar.invul()) {
							continue;
						}
						if (Math.abs(otherPlayer.z() - npc.z()) > 400.0 || isPeaceBlocked(otherPlayer.x(), otherPlayer.y(), otherPlayer.z(), otherChar)) {
							continue;
						}
						if (!hasLineOfSight(otherChar)) {
							continue;
						}
						var otherTpl = charTemplates != null ? charTemplates.get(otherChar.classId()).orElse(template) : template;
						applyOffensiveSkillToTarget(skill, otherPlayer, otherChar, otherTpl);
					}
				}
			}, effHitTime, java.util.concurrent.TimeUnit.MILLISECONDS);
			npc.currentCastTask(task);
		}
	}

	private void applyOffensiveSkillToTarget(SkillTemplate skill, com.lopez.l2j.game.world.GameWorld.OnlinePlayer targetPlayer,
			com.lopez.l2j.game.model.PlayerCharacter targetChar, com.lopez.l2j.game.template.CharTemplate targetTpl) {
		boolean isDmg = (skill.isPhysicalDamage() || skill.isMagicDamage()) && skill.power() > 0;
		if (isDmg && combatService != null && targetTpl != null) {
			var counter = combatService.skillAttackPlayer(npc, targetChar, targetTpl, skill.power(),
					skill.isMagicDamage(), skill.magicLevel());
			if (counter != null && counter.damage() > 0) {
				if ((counter.flags() & 0x40) != 0) {
					targetPlayer.send(SystemMessage.of(
							"DRAIN".equalsIgnoreCase(skill.skillType()) ? SystemMessage.RESISTED_S1_DRAIN : SystemMessage.RESISTED_S1_MAGIC,
							new SystemMessage.NpcName(npc.npcId())));
				}
				targetPlayer.onAttacked(npc.objectId(), counter.damage());
				targetPlayer.send(SystemMessage.of(SystemMessage.S1_GAVE_YOU_S2_DMG,
						new SystemMessage.Text(npc.name()),
						new SystemMessage.Number(counter.damage())));
				targetPlayer.send(StatusUpdate.hp(targetChar.objectId(), (int) targetChar.currentHp(), targetChar.maxHp()));
				targetPlayer.send(new UserInfo(targetChar, targetTpl));
				if (counter.isDead()) {
					targetPlayer.onDeath(npc.objectId());
					return;
				}
			}
		} else if (skill.isManaBurn() && skill.power() > 0 && combatService != null && targetTpl != null) {
			int mpDam = combatService.skillManaDamNpcToPlayer(npc, targetChar, targetTpl, skill.power(), skill.magicLevel());
			if (mpDam > 0) {
				targetChar.currentMp(Math.max(0, targetChar.currentMp() - mpDam));
				targetPlayer.send(StatusUpdate.mp(targetChar.objectId(), (int) targetChar.currentMp(), targetChar.maxMp()));
				targetPlayer.send(SystemMessage.of(SystemMessage.S2_MP_HAS_BEEN_DRAINED_BY_S1,
						new SystemMessage.Text(npc.name()),
						new SystemMessage.Number(mpDam)));
			}
		}

		if (!targetChar.isDead() && combatService != null && targetPlayer instanceof com.lopez.l2j.network.game.GameSession gs) {
			if (skill.effects().isEmpty()) {
				String st = skill.skillType().toLowerCase(java.util.Locale.ROOT);
				boolean control = st.equals("stun") || st.equals("sleep") || st.equals("paralyze")
						|| st.equals("root") || st.equals("mute") || st.equals("fear");
				if (control) {
					if (combatService.debuffLandsPlayer(skill.power() > 0 ? skill.power() : 50, skill.magicLevel(),
							npc.template().level(), targetChar, false, false, st)) {
						long until = System.currentTimeMillis() + 15000L;
						gs.applyControlEffect(skill.id(), skill.level(), st, until);
						targetPlayer.send(SystemMessage.of(SystemMessage.YOU_FEEL_S1_EFFECT,
								new SystemMessage.SkillName(skill.id(), skill.level())));
					} else {
						targetPlayer.send(SystemMessage.of(SystemMessage.RESISTED_S1_MAGIC,
								new SystemMessage.NpcName(npc.npcId())));
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
							npc.template().level(), targetChar, false, false, name)) {
						long until = System.currentTimeMillis() + Math.max(1000, e.durationMs());
						gs.applyControlEffect(skill.id(), skill.level(), name, until);
						targetPlayer.send(SystemMessage.of(SystemMessage.YOU_FEEL_S1_EFFECT,
								new SystemMessage.SkillName(skill.id(), skill.level())));
					} else {
						targetPlayer.send(SystemMessage.of(SystemMessage.RESISTED_S1_MAGIC,
								new SystemMessage.NpcName(npc.npcId())));
					}
				} else if (name.equals("damovertime") || name.equals("manadamovertime") || name.equals("poison")
						|| name.equals("bleed")) {
					if (combatService.debuffLandsPlayer(skill.power() > 0 ? skill.power() : 50, skill.magicLevel(),
							npc.template().level(), targetChar, false, false, name)) {
						gs.startSkillDot(skill, e);
					} else {
						targetPlayer.send(SystemMessage.of(SystemMessage.RESISTED_S1_MAGIC,
								new SystemMessage.NpcName(npc.npcId())));
					}
				} else if (!e.funcs().isEmpty() || skill.isDebuff() || skill.isOffensive() || name.equals("debuff")) {
					if (combatService.debuffLandsPlayer(skill.power() > 0 ? skill.power() : 50, skill.magicLevel(),
							npc.template().level(), targetChar, false, false, skill.skillType())) {
						gs.magicSkillHandler().applySkillEffects(skill, false, 0, true);
					} else {
						targetPlayer.send(SystemMessage.of(SystemMessage.RESISTED_S1_MAGIC,
								new SystemMessage.NpcName(npc.npcId())));
					}
				}
			}
		}
	}
}
