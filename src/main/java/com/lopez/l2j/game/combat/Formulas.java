package com.lopez.l2j.game.combat;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.item.ItemSlots;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.PlayerStats;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.template.BaseStatsTable;
import com.lopez.l2j.game.template.CharTemplate;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Formulas canonicas de combate do Chronicle 6 (Interlude).
 * Calibradas com base nos padroes retail e documentacao L2JDream / Lucera2.
 */
public final class Formulas {

	private Formulas() {}

	public static final int MAX_PCRIT_CAP = 500; // 50.0%
	public static final int MAX_MCRIT_CAP = 200; // 20.0%
	public static final int RETAIL_MCRIT_CAP = 300; // 30.0%

	public record LethalResult(boolean lethal1, boolean lethal2) {
		public static final LethalResult NONE = new LethalResult(false, false);
	}

	/**
	 * Modificador multiplicativo baseado no nivel do personagem: (Level + 89.0) / 100.0.
	 */
	public static double calcLevelMod(int level) {
		if (!Config.USE_CHAR_LEVEL_MODIFIER) {
			return 1.0;
		}
		return (Math.max(1, level) + 89.0) / 100.0;
	}

	/**
	 * Multiplicador posicional de taxa de critico fisico (Front 1.0x, Side 1.1x, Behind 1.2x).
	 */
	public static double calcPositionCritMultiplier(CombatService.RelativePosition pos) {
		if (pos == null) {
			return 1.0;
		}
		return switch (pos) {
			case BEHIND -> 1.20;
			case SIDE -> 1.10;
			case FRONT -> 1.00;
		};
	}

	/**
	 * Bonus posicional de precisao vs evasao (+0 Front, +5 Side, +10 Behind).
	 */
	public static int calcPositionAccuracyBonus(CombatService.RelativePosition pos) {
		if (pos == null) {
			return 0;
		}
		return switch (pos) {
			case BEHIND -> 10;
			case SIDE -> 5;
			case FRONT -> 0;
		};
	}

	/**
	 * Chance de acerto baseada na diferenca entre Precisao (Accuracy) e Evasao.
	 * Retorna chance de acerto em porcentagem [28%..98%].
	 */
	public static int calcHitChance(int accuracy, int evasion) {
		int diff = accuracy - evasion;
		return Math.max(28, Math.min(98, 80 + diff * 2));
	}

	/**
	 * Chance de acerto posicional considerando o diferencial Front (+0), Side (+5) e Behind (+10).
	 */
	public static int calcHitChance(int accuracy, int evasion, CombatService.RelativePosition pos) {
		int diff = (accuracy + calcPositionAccuracyBonus(pos)) - evasion;
		return Math.max(28, Math.min(98, 80 + diff * 2));
	}

	/**
	 * Chance de sucesso de bloqueio por escudo (Shield Block).
	 * Em caso de sucesso, o dano e absorvido parcialmente ou totalmente pela defesa do escudo.
	 */
	public static boolean calcShieldBlock(int shieldRate, int dex) {
		if (shieldRate <= 0) return false;
		double dexBonus = 1.0 + (dex - 30) * 0.01;
		int chance = (int) Math.min(80, Math.max(5, shieldRate * dexBonus));
		return ThreadLocalRandom.current().nextInt(100) < chance;
	}

	/**
	 * Verifica se o bloqueio de escudo foi um bloqueio perfeito (dano reduzido a 1).
	 */
	public static boolean isPerfectShieldBlock() {
		int perfectRate = Math.max(0, Config.ALT_PERFECT_SHIELD_BLOCK_RATE);
		return perfectRate > 0 && ThreadLocalRandom.current().nextInt(100) < perfectRate;
	}

	/**
	 * Verifica o uso de escudo direcional (arco de 120 graus frontal ou 360 graus com Aegis shldAngle),
	 * respeitando ignoreShld, bonus 3x contra arco e bloqueio perfeito (Integer.MAX_VALUE -> 1 de dano).
	 */
	public static int calcShldUse(int attackerX, int attackerY, PlayerCharacter target,
			CharTemplate targetTemplate, boolean attackerUsingBow, boolean ignoreShld) {
		if (ignoreShld || target == null || target.inventory() == null) {
			return 0;
		}
		var shield = target.inventory().paperdoll(ItemSlots.LHAND);
		if (shield == null || shield.template() == null || shield.template().shieldDef() <= 0) {
			return 0;
		}
		// Checagem direcional: 120 graus frontais por padrao (60 graus para cada lado), ou 360 com Aegis (shldAngle)
		if (attackerX != 0 || attackerY != 0) {
			double shldAngle = PlayerStats.applyStat(target, "shldAngle", 120.0);
			if (shldAngle < 360.0) {
				CombatService.RelativePosition pos = CombatService.getRelativePosition(
						attackerX, attackerY, target.x(), target.y(), target.heading());
				if (pos != CombatService.RelativePosition.FRONT) {
					return 0;
				}
			}
		}
		int dex = targetTemplate != null
				? targetTemplate.dex() + PlayerStats.clampHennaBonus(target.hennaDEX()) + target.augDEX()
				: 30;
		double dexMod = BaseStatsTable.dexBonus(dex);
		double baseShldRate = 20.0 * dexMod;
		double shldRate = PlayerStats.applyStat(target, "rShld", baseShldRate);
		if (attackerUsingBow) {
			shldRate *= 3.0;
		}
		shldRate = Math.min(90.0, Math.max(0.0, shldRate));
		if (ThreadLocalRandom.current().nextDouble(100.0) >= shldRate) {
			return 0;
		}
		int perfectRate = Config.ALT_PERFECT_SHIELD_BLOCK_RATE > 0 ? Config.ALT_PERFECT_SHIELD_BLOCK_RATE : 5;
		if (ThreadLocalRandom.current().nextInt(100) < perfectRate) {
			return Integer.MAX_VALUE; // bloqueio perfeito (1 de dano)
		}
		double baseSDef = shield.template().shieldDef() + PlayerStats.calcArmorEnchantDefBonus(shield.enchant());
		return Math.max(1, (int) Math.round(PlayerStats.applyStat(target, "sDef", baseSDef)));
	}

	/**
	 * Calculo de dano fisico padrao com reducao de escudo opcional.
	 */
	public static int calcPhysicalDamage(double pAtk, double pDef, boolean soulshot, double rndMult,
			boolean crit, boolean shieldBlock, int shieldDef) {
		double ssMult = soulshot ? 2.0 : 1.0;
		double effectivePDef = Math.max(1.0, pDef);

		if (shieldBlock && shieldDef > 0) {
			if (Config.ALT_SHIELD_BLOCKS && isPerfectShieldBlock()) {
				return 1;
			}
			effectivePDef += shieldDef;
		}

		double baseDamage = (77.0 * pAtk * ssMult) / effectivePDef;
		if (crit) {
			baseDamage *= 2.0;
		}

		double finalDmg = baseDamage * (rndMult > 0 ? rndMult : 1.0);
		if (shieldBlock && Config.ALT_SHIELD_BLOCKS) {
			finalDmg = Math.max(1.0, finalDmg - shieldDef);
		}
		return Math.max(1, (int) Math.round(finalDmg));
	}

	/**
	 * Calculo de dano critico fisico somando cAtkAdd apos a divisao por pDef (ou escalado por 70/pDef).
	 */
	public static double calcCritDamageWithAdd(double baseDam, double cAtk, double cAtkAdd, double pDef) {
		double scaledAdd = pDef > 0 ? (cAtkAdd * 70.0 / pDef) : cAtkAdd;
		return (baseDam * 2.0 * cAtk) + scaledAdd;
	}

	/**
	 * Calculo de critico de skill fisica usando STRBonus (skill.baseCritRate * STR_Bonus * 10).
	 */
	public static boolean calcSkillCrit(int baseCritRate, int str) {
		int effBase = baseCritRate > 0 ? baseCritRate : 10;
		double rate = effBase * BaseStatsTable.strBonus(str) * 10.0;
		int capped = (int) Math.min(MAX_PCRIT_CAP, Math.max(10, Math.round(rate)));
		return ThreadLocalRandom.current().nextInt(1000) < capped;
	}

	/**
	 * Calcula a chance de acerto de um golpe tipo BLOW considerando posicao (Front/Side/Behind),
	 * DEX bonus do atacante e o stat blowRate.
	 */
	public static double calcBlowRate(PlayerCharacter attacker, CombatService.RelativePosition pos, int dex) {
		int baseChance = switch (pos != null ? pos : CombatService.RelativePosition.FRONT) {
			case BEHIND -> Config.BLOW_BEHIND;
			case SIDE -> Config.BLOW_SIDE;
			case FRONT -> Config.BLOW_FRONT;
		};
		double rate = baseChance * BaseStatsTable.dexBonus(dex);
		if (attacker != null) {
			rate = PlayerStats.applyStat(attacker, "blowRate", rate);
		}
		return Math.max(0.0, Math.min(80.0, rate));
	}

	/**
	 * Calcula o dano retail de uma skill BLOW:
	 * (((pAtk * ssMult + power) * cAtk + 6.5 * cAtkAdd) * 70 / pDef) * critVuln * wpnVuln * elemMod.
	 */
	public static double calcBlowDamage(double pAtk, double power, boolean soulshot, double ssBoost,
			double cAtk, double cAtkAdd, double pDef, double critVuln, double wpnVuln, double elemMod) {
		double effPAtk = soulshot ? pAtk * 2.0 : pAtk;
		double effPower = (soulshot && ssBoost > 0) ? power * ssBoost : power;
		double raw = ((effPAtk + effPower) * cAtk + (6.5 * cAtkAdd)) * 70.0 / Math.max(1.0, pDef);
		return raw * critVuln * wpnVuln * elemMod;
	}

	/**
	 * Calcula Lethal 1 (Half-Kill: CP=1 em players, -50% HP em monstros) e Lethal 2
	 * (Lethal Strike: CP=1 e HP=1 em players, HP=1 em monstros).
	 */
	public static LethalResult calcLethalHit(PlayerCharacter attacker, Object target, int lethal1Rate, int lethal2Rate,
			int magicLevel, int dex, boolean isDagger, boolean isBow) {
		if (lethal1Rate <= 0 && lethal2Rate <= 0) {
			return LethalResult.NONE;
		}
		if (target instanceof NpcInstance npc) {
			if (npc.template() == null || npc.template().isRaidBoss() || npc.template().isGrandBoss() || npc.isRaidMinion()) {
				return LethalResult.NONE;
			}
		}
		int attackerLvl = attacker != null ? attacker.level() : 76;
		int effMagicLvl = magicLevel > 0 ? magicLevel : attackerLvl;
		int targetLvl = attackerLvl;
		if (target instanceof NpcInstance npc && npc.template() != null) {
			targetLvl = npc.template().level();
		} else if (target instanceof PlayerCharacter pc) {
			targetLvl = pc.level();
		}
		if (targetLvl - effMagicLvl > 9) {
			return LethalResult.NONE;
		}
		double dexMod = BaseStatsTable.dexBonus(dex);
		double weaponMult = CombatService.getLethalRateMultiplier(isDagger, isBow);
		double statMult = attacker != null ? PlayerStats.applyStat(attacker, "lethalRate", 1.0) : 1.0;
		double rateMult = dexMod * weaponMult * statMult;

		double roll = ThreadLocalRandom.current().nextDouble(100.0);
		double chance2 = Math.min(25.0, Math.max(0.0, lethal2Rate * rateMult));
		if (lethal2Rate > 0 && roll < chance2) {
			return new LethalResult(false, true);
		}
		double chance1 = Math.min(50.0, Math.max(0.0, lethal1Rate * rateMult));
		if (lethal1Rate > 0 && roll < (chance2 + chance1)) {
			return new LethalResult(true, false);
		}
		return LethalResult.NONE;
	}

	/**
	 * Calculo de dano de skill magico.
	 * Spiritshot: SPS x2, BSS x4. Critico magico retail multiplica por 3.0.
	 */
	public static int calcMagicDamage(double mAtk, double power, double mDef, boolean sps, boolean bss, boolean mcrit) {
		double shotMultiplier = bss ? 4.0 : (sps ? 2.0 : 1.0);
		double effectiveMDef = Math.max(1.0, mDef);
		double baseDamage = (91.0 * power * Math.sqrt(Math.max(1.0, mAtk * shotMultiplier))) / effectiveMDef;

		if (mcrit) {
			baseDamage *= 3.0; // Critico magico oficial L2J/Lucera
		}

		double rnd = 0.95 + ThreadLocalRandom.current().nextDouble() * 0.10;
		return Math.max(1, (int) Math.round(baseDamage * rnd));
	}

	/**
	 * Calculo de dano de skill magico com suporte a bloqueio de escudo e multiplicador PvE (4.0x / Config) vs PvP (2.5x).
	 */
	public static double calcRetailMagicDamage(double mAtk, double power, double mDef,
			boolean sps, boolean bss, boolean mcrit, boolean isPvP, int shldDef) {
		if (shldDef == Integer.MAX_VALUE) {
			return 1.0;
		}
		double shotMultiplier = bss ? 4.0 : (sps ? 2.0 : 1.0);
		double effectiveMDef = Math.max(1.0, mDef + Math.max(0, shldDef));
		double baseDamage = (91.0 * power * Math.sqrt(Math.max(1.0, mAtk * shotMultiplier))) / effectiveMDef;
		if (mcrit) {
			if (isPvP) {
				baseDamage *= 2.5;
			} else {
				baseDamage *= Config.M_CRIT_RATE > 0 ? (double) Config.M_CRIT_RATE : 4.0;
			}
		}
		return baseDamage;
	}

	/**
	 * Calculo da taxa de critico fisico limitado pelo cap oficial (500 per mille = 50%).
	 */
	public static int calcCritRate(int baseCrit, double dexBonus, double buffMultiplier) {
		int rate = (int) Math.round(baseCrit * dexBonus * buffMultiplier);
		return Math.min(MAX_PCRIT_CAP, Math.max(10, rate));
	}

	/**
	 * Calculo da taxa de critico magico limitado pelo cap oficial (200 per mille = 20%).
	 */
	public static int calcMagicCritRate(int baseMagicCrit, double witBonus) {
		int rate = (int) Math.round(baseMagicCrit * witBonus);
		return Math.min(MAX_MCRIT_CAP, Math.max(10, rate));
	}

	/**
	 * Calculo retail de critico magico baseado em BaseStats.WIT (80 * witBonus, cap de 300 = 30%).
	 */
	public static int calcRetailMagicCritRate(PlayerCharacter attacker, int wit) {
		double baseMCrit = 80.0 * BaseStatsTable.witBonus(wit);
		double applied = attacker != null ? PlayerStats.applyStat(attacker, "mCritRate", baseMCrit) : baseMCrit;
		int mCritCap = Config.getInt("AltMCriticalCap", RETAIL_MCRIT_CAP);
		return (int) Math.min(mCritCap, Math.max(1, Math.round(applied)));
	}

	/**
	 * Formula retail de MANADAM (Mana Burn / Mana Storm):
	 * sqrt(mAtk * shotMult) * power * (targetMaxMp / 97) / mDef.
	 */
	public static int calcManaDam(double mAtk, double power, double targetMaxMp, double mDef,
			boolean sps, boolean bss, boolean mcrit) {
		double shotMult = bss ? 4.0 : (sps ? 2.0 : 1.0);
		double effMaxMp = Math.max(1.0, targetMaxMp);
		double dmg = (Math.sqrt(Math.max(1.0, mAtk * shotMult)) * power * (effMaxMp / 97.0)) / Math.max(1.0, mDef);
		if (mcrit) {
			dmg *= 3.0;
		}
		dmg *= 0.95 + ThreadLocalRandom.current().nextDouble() * 0.10;
		return Math.max(1, (int) Math.round(dmg));
	}

	/**
	 * Calcula a cura absorvida por skills DRAIN, limitando o dano absorvido ao HP atual do alvo (sem over-hit).
	 */
	public static int calcDrainHeal(int damageDealt, double targetCurrentHpBeforeHit, double absorbPart, double absorbAbs) {
		double effectiveDamage = Math.min(Math.max(0.0, damageDealt), Math.max(0.0, targetCurrentHpBeforeHit));
		double healed = (effectiveDamage * Math.max(0.0, absorbPart)) + Math.max(0.0, absorbAbs);
		return (int) Math.max(0, Math.round(healed));
	}

	/**
	 * Multiplicador de dano de CHARGEDAM: (1.0 + 0.25 * numCharges).
	 */
	public static double calcChargeDamMultiplier(int numCharges) {
		return 1.0 + (0.25 * Math.max(0, numCharges));
	}

	/**
	 * Multiplicador de poder de FATALCOUNTER (Fatal Counter / Heart Shot):
	 * power * (1.0 + 3.5 * (1.0 - currentHp / maxHp)).
	 */
	public static double calcFatalCounterPower(double basePower, double currentHp, double maxHp) {
		if (maxHp <= 0) {
			return basePower;
		}
		double missingRatio = Math.max(0.0, Math.min(1.0, 1.0 - (currentHp / maxHp)));
		return basePower * (1.0 + 3.5 * missingRatio);
	}

	/**
	 * Calcula a chance de sucesso de FATAL (Deadly Roulette).
	 */
	public static boolean calcDeadlyRouletteSuccess(double currentHp, double maxHp, double baseChance) {
		if (maxHp <= 0 || (currentHp * 100.0 / maxHp) > 10.0) {
			return false;
		}
		double chance = Math.max(1.0, Math.min(95.0, baseChance > 0 ? baseChance : 50.0));
		return ThreadLocalRandom.current().nextDouble(100.0) < chance;
	}

	/**
	 * Calcula o dano de CPDAM sobre o CP atual do alvo.
	 */
	public static int calcCpDam(double targetCurrentCp, double power) {
		if (targetCurrentCp <= 0 || power <= 0) {
			return 0;
		}
		double ratio = power <= 1.0 ? power : (power / 100.0);
		ratio = Math.max(0.0, Math.min(1.0, ratio));
		return (int) Math.max(0, Math.round(targetCurrentCp * ratio));
	}

	/**
	 * Calcula a quantidade de HP restaurada por skills HEAL considerando Spiritshot/BlessedSpiritShot,
	 * gainHp (HEAL_EFFECTIVNESS), giveHp (HEAL_PROFICIENCY) e bonusHp (HEAL_STATIC_BONUS).
	 */
	public static int calcHealAmount(double basePower, int mAtk, boolean sps, boolean bss,
			double healEffectiveness, double healProficiency, double healStaticBonus) {
		double shotMult = bss ? 4.0 : (sps ? 2.0 : 1.0);
		double mAtkBonus = Math.sqrt(Math.max(1.0, mAtk * shotMult));
		double amount = (basePower + mAtkBonus) * Math.max(0.0, healProficiency) * Math.max(0.0, healEffectiveness) + healStaticBonus;
		return (int) Math.max(0, Math.round(amount));
	}

	/**
	 * Calcula a chance de destrancar bau (Chest) vs porta (Door) na skill UNLOCK / DELUXE_KEY_UNLOCK (#160).
	 */
	public static boolean calculateUnlockChance(int skillLevel, int targetLevel, boolean isChest, int doorUnlockLevel) {
		if (skillLevel <= 0) {
			return false;
		}
		if (isChest) {
			int maxChestLvl = switch (skillLevel) {
				case 1 -> 24;
				case 2 -> 29;
				case 3 -> 34;
				case 4 -> 39;
				case 5 -> 44;
				case 6 -> 49;
				case 7 -> 54;
				case 8 -> 59;
				case 9 -> 64;
				case 10 -> 69;
				default -> 85;
			};
			if (targetLevel > maxChestLvl + 6) {
				return false;
			}
			int diff = Math.max(0, targetLevel - maxChestLvl);
			int chance = Math.max(10, 90 - (diff * 15));
			return ThreadLocalRandom.current().nextInt(100) < chance;
		}
		int reqDoorLvl = Math.max(1, doorUnlockLevel);
		if (skillLevel < reqDoorLvl) {
			return false;
		}
		int chance = switch (skillLevel - reqDoorLvl) {
			case 0 -> 30;
			case 1 -> 50;
			case 2 -> 75;
			default -> 100;
		};
		return ThreadLocalRandom.current().nextInt(100) < chance;
	}

	/**
	 * Calcula o sucesso de SPOIL com base na diferenca de nivel entre o Dwarf/skill e o monstro.
	 */
	public static boolean calcSpoilSuccess(int magicLevel, int attackerLevel, int targetLevel) {
		int effLvl = magicLevel > 0 ? magicLevel : attackerLevel;
		int diff = targetLevel - effLvl;
		double rate = 85.0;
		if (diff > 3) {
			rate -= (diff - 3) * 9.0;
		}
		rate = Math.max(5.0, Math.min(99.0, rate));
		return ThreadLocalRandom.current().nextDouble(100.0) < rate;
	}

	/**
	 * Calcula o SP concedido por skills GIVESP (SP Scrolls).
	 */
	public static int calcGiveSpAmount(double basePower, double spMultiplier) {
		return (int) Math.max(0, Math.round(basePower * Math.max(0.1, spMultiplier)));
	}

	/**
	 * Verifica se SUMMON_FRIEND pode puxar o alvo (ambos vivos, fora de Olympiad/Siege).
	 */
	public static boolean canSummonFriend(PlayerCharacter caster, PlayerCharacter target) {
		if (caster == null || target == null || caster.isDead() || target.isDead()) {
			return false;
		}
		if (caster.isOlympiadMode() || target.isOlympiadMode() || caster.isInSiege() || target.isInSiege()) {
			return false;
		}
		return caster.objectId() != target.objectId();
	}

	/**
	 * Calcula o numero de pulsos de um SIGNET com base na duracao e intervalo.
	 */
	public static int calcSignetPulseCount(int durationSeconds, int tickIntervalSeconds) {
		int interval = Math.max(1, tickIntervalSeconds);
		return Math.max(1, durationSeconds / interval);
	}

	/**
	 * Calcula o proximo estagio de um efeito FUSION (Battle Force / Spell Force).
	 */
	public static int calcFusionNextStage(int currentStage, int maxStage) {
		int cap = Math.max(1, maxStage);
		return Math.min(cap, Math.max(1, currentStage + 1));
	}

	/**
	 * Calcula o modificador de atributo de resistencia base (SAVEVS: CON, MEN, DEX, WIT):
	 * 2.0 - Math.sqrt(statBonus).
	 */
	public static double calcSkillStatModifier(String saveVs, int con, int men, int dex, int wit) {
		if (saveVs == null || saveVs.isBlank()) {
			return 1.0;
		}
		double bonus = switch (saveVs.trim().toUpperCase(Locale.ROOT)) {
			case "CON" -> BaseStatsTable.conBonus(con);
			case "MEN" -> BaseStatsTable.menBonus(men);
			case "DEX" -> BaseStatsTable.dexBonus(dex);
			case "WIT" -> BaseStatsTable.witBonus(wit);
			default -> 1.0;
		};
		return Math.max(0.2, 2.0 - Math.sqrt(Math.max(0.1, bonus)));
	}

	/**
	 * Calcula a proficiencia de debuff/status do atacante (*Prof).
	 */
	public static double calcSkillProficiency(PlayerCharacter attacker, String effectOrSkillType) {
		if (attacker == null) {
			return 1.0;
		}
		double prof = PlayerStats.applyStat(attacker, "debuffProf", 1.0);
		if (effectOrSkillType != null && !effectOrSkillType.isBlank()) {
			String k = effectOrSkillType.trim().toLowerCase(Locale.ROOT);
			String specificStat = switch (k) {
				case "stun" -> "stunProf";
				case "sleep" -> "sleepProf";
				case "root" -> "rootProf";
				case "paralyze", "petrification", "medusa" -> "paralyzeProf";
				case "mute", "silence", "physicalmute", "silencemagicphysical", "fear", "betray", "derangement" -> "derangementProf";
				case "confusion", "confuse", "confuse_mob_only", "switch", "aggdebuff", "aggdamage", "aggremove", "aggreduce" -> "confusionProf";
				case "poison" -> "poisonProf";
				case "bleed" -> "bleedProf";
				case "cancel", "warrior_bane", "mage_bane", "steal_buff" -> "cancelProf";
				default -> null;
			};
			if (specificStat != null) {
				prof *= PlayerStats.applyStat(attacker, specificStat, 1.0);
			}
		}
		return Math.max(0.1, Math.min(3.0, prof));
	}

	/**
	 * Formula retail de sucesso de debuffs/skills (calcSkillSuccess):
	 * considera lvlDepend, SAVEVS (CON/MEN/DEX/WIT), proficiencia do atacante vs vulnerabilidade do alvo,
	 * Spiritshot/BlessedSpiritShot (2x em BSS) e limites [1%, 99%] (ou [5%, 95%] para cubic/boss).
	 */
	public static boolean calcSkillSuccess(PlayerCharacter attacker, Object target, double basePower,
			int magicLevel, int lvlDepend, String saveVs, String effectOrSkillType,
			boolean sps, boolean bss, boolean isCubicOrBoss) {
		int attackerLevel = attacker != null ? attacker.level() : 76;
		int effMagicLvl = Config.USE_LEVEL_PENALTY
				? (magicLevel > 0 ? Math.min(magicLevel, attackerLevel) : attackerLevel)
				: (magicLevel > 0 ? magicLevel : attackerLevel);
		int targetLevel = attackerLevel;
		int con = 43, men = 25, dex = 30, wit = 20;
		double vuln = 1.0;

		if (target instanceof NpcInstance npc && npc.template() != null) {
			targetLevel = npc.template().level();
		} else if (target instanceof PlayerCharacter pc) {
			targetLevel = pc.level();
			con = Math.max(1, 43 + PlayerStats.clampHennaBonus(pc.hennaCON()) + pc.augCON());
			men = Math.max(1, 25 + PlayerStats.clampHennaBonus(pc.hennaMEN()) + pc.augMEN());
			dex = Math.max(1, 30 + PlayerStats.clampHennaBonus(pc.hennaDEX()) + pc.augDEX());
			wit = Math.max(1, 20 + PlayerStats.clampHennaBonus(pc.hennaWIT()) + pc.augWIT());
			vuln = CombatService.calcDebuffVulnerability(pc, effectOrSkillType);
			if (vuln <= 0.0) {
				return false;
			}
		}

		int lvlDiff = targetLevel - effMagicLvl;
		if (lvlDiff >= 15) {
			return false;
		}

		double statMod = calcSkillStatModifier(saveVs, con, men, dex, wit);
		double prof = calcSkillProficiency(attacker, effectOrSkillType);
		double rate = (basePower > 0 ? basePower : 50.0) * statMod * prof * vuln;

		int effLvlDepend = lvlDepend > 0 ? lvlDepend : 1;
		if (lvlDiff > 0) {
			rate -= lvlDiff * (4.0 * effLvlDepend);
		}
		if (bss) {
			rate *= 2.0;
		} else if (sps) {
			rate *= 1.4;
		}

		double minRate = isCubicOrBoss ? 5.0 : 1.0;
		double maxRate = isCubicOrBoss ? 95.0 : 99.0;
		if (lvlDiff > 9) {
			maxRate = Math.min(maxRate, 10.0);
		}
		rate = Math.max(minRate, Math.min(maxRate, rate));
		return ThreadLocalRandom.current().nextDouble(100.0) < rate;
	}

	/**
	 * Verifica se o alvo esquiva de uma skill fisica de curto alcance (pSkillEvas - Dodge / Mirage) (#125).
	 */
	public static boolean calcPhysicalSkillEvasion(PlayerCharacter target) {
		if (target == null) {
			return false;
		}
		double evasChance = PlayerStats.applyStat(target, "pSkillEvas", 0.0);
		if (evasChance <= 0.0) {
			return false;
		}
		return ThreadLocalRandom.current().nextDouble(100.0) < Math.min(90.0, evasChance);
	}

	/**
	 * Verifica se o alvo reflete uma skill fisica ou magica (reflectSkillPhysic / reflectSkillMagic) (#126).
	 */
	public static boolean calcSkillReflect(PlayerCharacter target, boolean isMagicSkill) {
		if (target == null) {
			return false;
		}
		String stat = isMagicSkill ? "reflectSkillMagic" : "reflectSkillPhysic";
		double reflectChance = PlayerStats.applyStat(target, stat, 0.0);
		if (reflectChance <= 0.0) {
			return false;
		}
		return ThreadLocalRandom.current().nextDouble(100.0) < Math.min(90.0, reflectChance);
	}

	/**
	 * Calcula o contra-ataque de dano de skill (vengeancePdam / vengeanceMdam - Riposte Stance / Counterattack) (#126).
	 */
	public static int calcVengeanceReflectDamage(PlayerCharacter target, double incomingSkillDamage, boolean isMagicSkill) {
		if (target == null || incomingSkillDamage <= 0) {
			return 0;
		}
		String stat = isMagicSkill ? "vengeanceMdam" : "vengeancePdam";
		double chance = PlayerStats.applyStat(target, stat, 0.0);
		if (chance <= 0.0 || ThreadLocalRandom.current().nextDouble(100.0) >= Math.min(90.0, chance)) {
			return 0;
		}
		return (int) Math.max(1, Math.round(incomingSkillDamage));
	}

	/**
	 * Calcula a porcao de dano melee refletido (reflectDam - Chant of Revenge / Song of Vengeance) (#127).
	 */
	public static int calcMeleeReflectDamage(PlayerCharacter target, double incomingMeleeDamage) {
		if (target == null || incomingMeleeDamage <= 0) {
			return 0;
		}
		double reflectPct = PlayerStats.applyStat(target, "reflectDam", 0.0);
		if (reflectPct <= 0.0) {
			return 0;
		}
		return (int) Math.max(0, Math.round(incomingMeleeDamage * Math.min(100.0, reflectPct) / 100.0));
	}

	/**
	 * Calcula o multiplicador de dano baseado na quantidade de debuffs ativos no alvo (#131).
	 */
	public static double calcTargetDebuffBonus(int activeDebuffCount) {
		if (activeDebuffCount <= 0) {
			return 1.0;
		}
		return 1.0 + (0.05 * Math.min(12, activeDebuffCount));
	}

	/**
	 * Verifica se um golpe recebido quebra o efeito de Stun (75% em critico, 10% em hit normal) (#132).
	 */
	public static boolean calcStunBreak(boolean isCriticalHit) {
		int chance = isCriticalHit ? 75 : 10;
		return ThreadLocalRandom.current().nextInt(100) < chance;
	}

	/**
	 * Calcula a interrupcao de cast magico ao sofrer dano considerando MEN, sqrt(13 * dmg) e o stat cancel (#133).
	 */
	public static boolean calcAtkBreak(PlayerCharacter target, int men, double damage) {
		if (damage <= 0) {
			return false;
		}
		double init = Math.sqrt(13.0 * damage);
		double menBonus = BaseStatsTable.menBonus(Math.max(1, men));
		double cancelRes = target != null ? PlayerStats.applyStat(target, "cancel", 0.0) : 0.0;
		double rate = (init / menBonus) - cancelRes;
		rate = Math.max(0.0, Math.min(99.0, rate));
		return ThreadLocalRandom.current().nextDouble(100.0) < rate;
	}

	/**
	 * Calcula se Skill Mastery (Skills 330 Fighter STR / 331 Mage INT) e ativado no cast (#134).
	 */
	public static boolean calcSkillMastery(PlayerCharacter caster, CharTemplate template, boolean isMage) {
		if (caster == null) {
			return false;
		}
		double masteryStat = PlayerStats.applyStat(caster, "skillMastery", 0.0);
		if (masteryStat <= 0.0 && caster.skillLevel(330) <= 0 && caster.skillLevel(331) <= 0) {
			return false;
		}
		int attr = 30;
		if (template != null) {
			PlayerStats st = PlayerStats.calculate(caster, template);
			attr = isMage ? st.intel() : st.str();
		}
		double bonus = isMage ? BaseStatsTable.intBonus(attr) : BaseStatsTable.strBonus(attr);
		double chance = Math.min(25.0, bonus * 2.0);
		return ThreadLocalRandom.current().nextDouble(100.0) < chance;
	}

	/**
	 * Calcula a chance individual de remover um buff via Cancel / Touch of Death (#135).
	 */
	public static boolean calcCancelSuccess(int magicLevel, int targetLevel, double baseCancelRate,
			int remainingDurationSeconds, double cancelProf, double cancelVuln) {
		double rate = baseCancelRate > 0 ? baseCancelRate : 25.0;
		int diff = magicLevel - targetLevel;
		rate += diff * 2.0;
		rate *= (1.0 + Math.min(1.0, remainingDurationSeconds / 3600.0));
		rate *= Math.max(0.1, cancelProf) * Math.max(0.1, cancelVuln);
		rate = Math.max(25.0, Math.min(75.0, rate));
		return ThreadLocalRandom.current().nextDouble(100.0) < rate;
	}

	/**
	 * Calcula o percentual de EXP restaurado por Resurrect escalado por WIT (teto de +20% adicional e max 90%) (#137).
	 */
	public static double calculateSkillResurrectRestorePercent(double baseRestorePercent, int wit) {
		if (baseRestorePercent <= 0) {
			return 0.0;
		}
		if (baseRestorePercent >= 100.0) {
			return 100.0;
		}
		double witMod = BaseStatsTable.witBonus(Math.max(1, wit));
		double bonus = Math.min(20.0, baseRestorePercent * (witMod - 1.0));
		return Math.min(90.0, Math.max(baseRestorePercent, baseRestorePercent + bonus));
	}

	/**
	 * Calcula a regeneracao de HP do personagem considerando CON, levelMod, estado de movimento e zonas (#113).
	 */
	public static double calcHpRegen(PlayerCharacter player, CharTemplate template,
			boolean inMotherTree, boolean inClanHall, boolean inCastle, boolean inSiegeFlag,
			boolean inSevenSignsFestival) {
		int level = player != null ? Math.max(1, player.level()) : 1;
		double baseHpReg = 1.5 + (level * 0.1);
		int con = template != null ? template.con() : 43;
		if (player != null && template != null) {
			con = PlayerStats.calculate(player, template).con();
		}
		double reg = baseHpReg * calcLevelMod(level) * BaseStatsTable.conBonus(con);
		if (player != null) {
			if (player.isSitting()) {
				reg *= 1.5;
			} else if (!player.isMoving()) {
				reg *= 1.1;
			} else if (player.running()) {
				reg *= 0.7;
			}
			if (inMotherTree) reg *= 1.25;
			if (inClanHall) reg *= 1.40;
			if (inCastle) reg *= 1.40;
			if (inSiegeFlag) reg *= 1.50;
			if (inSevenSignsFestival) reg *= 0.50;
			reg = PlayerStats.applyStat(player, "regHp", reg);
		}
		return Math.max(1.0, reg);
	}

	/**
	 * Calcula a regeneracao de MP do personagem considerando MEN, levelMod, estado de movimento e zonas (#113).
	 */
	public static double calcMpRegen(PlayerCharacter player, CharTemplate template,
			boolean inMotherTree, boolean inClanHall, boolean inCastle) {
		int level = player != null ? Math.max(1, player.level()) : 1;
		double baseMpReg = 0.9 + (level * 0.08);
		int men = template != null ? template.men() : 25;
		if (player != null && template != null) {
			men = PlayerStats.calculate(player, template).men();
		}
		double reg = baseMpReg * calcLevelMod(level) * BaseStatsTable.menBonus(men);
		if (player != null) {
			if (player.isSitting()) {
				reg *= 1.5;
			} else if (!player.isMoving()) {
				reg *= 1.1;
			} else if (player.running()) {
				reg *= 0.7;
			}
			if (inMotherTree) reg *= 2.0;
			if (inClanHall) reg *= 1.40;
			if (inCastle) reg *= 1.40;
			reg = PlayerStats.applyStat(player, "regMp", reg);
		}
		return Math.max(1.0, reg);
	}

	/**
	 * Calcula a regeneracao de CP do personagem considerando CON, levelMod e estado de movimento (#113).
	 */
	public static double calcCpRegen(PlayerCharacter player, CharTemplate template) {
		int level = player != null ? Math.max(1, player.level()) : 1;
		double baseCpReg = 1.5 + (level * 0.1);
		int con = template != null ? template.con() : 43;
		if (player != null && template != null) {
			con = PlayerStats.calculate(player, template).con();
		}
		double reg = baseCpReg * calcLevelMod(level) * BaseStatsTable.conBonus(con);
		if (player != null) {
			if (player.isSitting()) {
				reg *= 1.5;
			} else if (!player.isMoving()) {
				reg *= 1.1;
			} else if (player.running()) {
				reg *= 0.7;
			}
			reg = PlayerStats.applyStat(player, "regCp", reg);
		}
		return Math.max(1.0, reg);
	}

	/**
	 * Resistencia magica baseada na diferenca de nivel entre o caster e o alvo.
	 */
	public static boolean calcMagicSuccess(int magicLevel, int attackerLevel, int targetLevel) {
		if (!Config.MAGIC_FAILURES) {
			return true;
		}
		int effMagicLvl;
		if (Config.USE_LEVEL_PENALTY) {
			effMagicLvl = magicLevel > 0 ? Math.min(magicLevel, attackerLevel) : attackerLevel;
		} else {
			effMagicLvl = magicLevel > 0 ? magicLevel : attackerLevel;
		}
		int lvlDiff = targetLevel - effMagicLvl;
		if (!Config.USE_CHAR_LEVEL_MODIFIER || lvlDiff <= 0) {
			return true;
		}
		int rate = (int) Math.round(Math.pow(1.3, lvlDiff) * 100.0);
		if (rate >= 10000) {
			return false;
		}
		return ThreadLocalRandom.current().nextInt(10000) >= rate;
	}

	/**
	 * Calcula o intervalo entre ataques fisicos considerando ALT_ATTACK_DELAY.
	 */
	public static int calcPAtkDelay(int pAtkSpd) {
		double delayMod = Config.ALT_ATTACK_DELAY > 0 ? Config.ALT_ATTACK_DELAY : 1.0;
		return (int) Math.max(100, Math.round((500000.0 / Math.max(1, pAtkSpd)) * delayMod));
	}

	/**
	 * Calcula o dano de queda considerando ALT_MINIMUM_FALL_HEIGHT.
	 */
	public static int calcFallDamage(int fallHeight, int maxHp) {
		int minHeight = Config.ALT_MINIMUM_FALL_HEIGHT > 0 ? Config.ALT_MINIMUM_FALL_HEIGHT : 400;
		if (fallHeight <= minHeight || maxHp <= 0) {
			return 0;
		}
		double excess = fallHeight - minHeight;
		return (int) Math.max(1, Math.min(maxHp - 1, Math.round(maxHp * (excess / 1000.0))));
	}

	/**
	 * Verifica se a habilidade Fake Death teve sucesso ou falhou (FAIL_FAKE_DEATH).
	 */
	public static boolean calcFakeDeathSuccess(int casterLevel, int targetLevel) {
		if (!Config.FAIL_FAKE_DEATH) {
			return true;
		}
		int diff = targetLevel - casterLevel;
		if (diff <= 0) {
			return true;
		}
		int failChance = Math.min(90, diff * 5);
		return ThreadLocalRandom.current().nextInt(100) >= failChance;
	}

	/**
	 * Verifica se um golpe interrompe a conjuracao de magia (ALT_GAME_CANCEL_BY_HIT).
	 */
	public static boolean calcCastBreakOnHit(boolean hitByBow, boolean isCritical) {
		String mode = Config.ALT_GAME_CANCEL_BY_HIT != null ? Config.ALT_GAME_CANCEL_BY_HIT.trim().toLowerCase() : "all";
		boolean allowed = switch (mode) {
			case "all" -> true;
			case "bow" -> hitByBow;
			case "cast" -> !hitByBow;
			default -> false;
		};
		if (!allowed) {
			return false;
		}
		int chance = isCritical ? 35 : 15;
		return ThreadLocalRandom.current().nextInt(100) < chance;
	}

	/**
	 * Calcula o tempo de recarga (reuse delay) de uma skill considerando reuse estatico,
	 * Olimpiadas, skills de itens e o delay minimo configurado.
	 */
	public static int calcSkillReuseDelay(int baseReuseMs, boolean isItemSkill, boolean isMagic,
			boolean isOlympiad, int pAtkSpd, int mAtkSpd) {
		int minDelay = Math.max(0, Config.SKILL_REUSE_DELAY);
		if (baseReuseMs <= 0) {
			return minDelay;
		}
		if ((isItemSkill && Config.ALT_ITEM_SKILLS_NOT_INFLUENCED)
				|| Config.ENABLE_STATIC_REUSE
				|| (isOlympiad && Config.OLY_USE_STATIC_REUSE)) {
			return Math.max(minDelay, baseReuseMs);
		}
		double spd = isMagic ? Math.max(1, mAtkSpd) : Math.max(1, pAtkSpd);
		int scaled = (int) Math.round(baseReuseMs * (333.0 / spd));
		return Math.max(minDelay, scaled);
	}

	/**
	 * Verifica se a chance de sucesso da skill deve ser exibida ao jogador.
	 */
	public static boolean shouldShowSkillChance(boolean isDebuff) {
		if (!Config.SHOW_SUCCESS_CHANCE) {
			return false;
		}
		return !Config.SHOW_DEBUFF_ONLY || isDebuff;
	}

	/**
	 * Calcula a regeneracao de HP de NPCs e Raid Bosses considerando os multiplicadores configurados.
	 */
	public static double calcNpcHpRegen(double baseRegen, boolean isRaid) {
		int mult = isRaid ? Config.RAID_HP_REGEN_MULTIPLIER : Config.NPC_HP_REGEN_MULTIPLIER;
		return baseRegen * (mult > 0 ? mult / 100.0 : 1.0);
	}

	/**
	 * Calcula a regeneracao de MP de NPCs e Raid Bosses considerando os multiplicadores configurados.
	 */
	public static double calcNpcMpRegen(double baseRegen, boolean isRaid) {
		int mult = isRaid ? Config.RAID_MP_REGEN_MULTIPLIER : Config.NPC_MP_REGEN_MULTIPLIER;
		return baseRegen * (mult > 0 ? mult / 100.0 : 1.0);
	}

	/**
	 * Calcula a regeneracao de HP de Pets considerando o multiplicador configurado.
	 */
	public static double calcPetHpRegen(double baseRegen) {
		int mult = Config.PET_HP_REGEN_MULTIPLIER;
		return baseRegen * (mult > 0 ? mult / 100.0 : 1.0);
	}

	/**
	 * Calcula a regeneracao de MP de Pets considerando o multiplicador configurado.
	 */
	public static double calcPetMpRegen(double baseRegen) {
		int mult = Config.PET_MP_REGEN_MULTIPLIER;
		return baseRegen * (mult > 0 ? mult / 100.0 : 1.0);
	}

	/**
	 * Registro de absorcao de dano em CP no PvP:
	 * O dano recebido esgota primeiro o CP (Combat Points); o excedente afeta o HP.
	 */
	public record CpDamageResult(int cpDamage, int hpDamage, double remainingCp, double remainingHp) {}

	public static CpDamageResult calcCpPenetration(int damage, double currentCp, double currentHp) {
		int cpDmg = 0;
		int hpDmg = 0;
		double newCp = currentCp;
		double newHp = currentHp;

		if (newCp > 0) {
			if (damage <= newCp) {
				cpDmg = damage;
				newCp -= damage;
			} else {
				cpDmg = (int) Math.round(newCp);
				int excess = damage - cpDmg;
				newCp = 0;
				hpDmg = excess;
				newHp = Math.max(0, newHp - excess);
			}
		} else {
			hpDmg = damage;
			newHp = Math.max(0, newHp - damage);
		}

		return new CpDamageResult(cpDmg, hpDmg, newCp, newHp);
	}
}
