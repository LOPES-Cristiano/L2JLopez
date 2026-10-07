package com.lopez.l2j.game.combat;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Formulas canonicas de combate do Chronicle 6 (Interlude).
 * Calibradas com base nos padroes retail e documentacao L2J / Lucera2:
 * - LevelMod = (Level + 89.0) / 100.0
 * - PhysicalDamage = (77.0 * P.Atk * SoulshotBoost / P.Def) * RandomDam * (Crit ? 2.0 : 1.0)
 * - MagicDamage = (91.0 * Power * sqrt(M.Atk * SpiritshotBoost) / M.Def) * (MCrit ? 3.0 : 1.0)
 * - CritChance = min(500, BaseCritRate * DexBonus * BuffMod)
 * - MagicCritChance = min(200, BaseMagicCrit * WitBonus)
 * - Absorcao e penetracao de dano em CP no PvP
 */
public final class Formulas {

	private Formulas() {}

	public static final int MAX_PCRIT_CAP = 500; // 50.0%
	public static final int MAX_MCRIT_CAP = 200; // 20.0%

	/**
	 * Modificador multiplicativo baseado no nivel do personagem.
	 */
	public static double calcLevelMod(int level) {
		return (Math.max(1, level) + 89.0) / 100.0;
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
	 * Calculo de dano fisico padrao com reducao de escudo opcional.
	 */
	public static int calcPhysicalDamage(double pAtk, double pDef, boolean soulshot, double rndMult,
			boolean crit, boolean shieldBlock, int shieldDef) {
		double ssMult = soulshot ? 2.0 : 1.0;
		double effectivePDef = Math.max(1.0, pDef);

		if (shieldBlock && shieldDef > 0) {
			effectivePDef += shieldDef;
		}

		double baseDamage = (77.0 * pAtk * ssMult) / effectivePDef;
		if (crit) {
			baseDamage *= 2.0;
		}

		double finalDmg = baseDamage * (rndMult > 0 ? rndMult : 1.0);
		return Math.max(1, (int) Math.round(finalDmg));
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
	 * Resistencia magica baseada na diferenca de nivel entre o caster e o alvo.
	 */
	public static boolean calcMagicSuccess(int magicLevel, int attackerLevel, int targetLevel) {
		int effMagicLvl = magicLevel > 0 ? Math.min(magicLevel, attackerLevel) : attackerLevel;
		int lvlDiff = targetLevel - effMagicLvl;
		if (lvlDiff <= 0) {
			return true;
		}
		int rate = (int) Math.round(Math.pow(1.3, lvlDiff) * 100.0);
		if (rate >= 10000) {
			return false;
		}
		return ThreadLocalRandom.current().nextInt(10000) >= rate;
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
