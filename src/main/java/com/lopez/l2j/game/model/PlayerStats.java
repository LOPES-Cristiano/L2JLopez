package com.lopez.l2j.game.model;

import com.lopez.l2j.game.item.ItemSlots;
import com.lopez.l2j.game.skill.StatFunc;
import com.lopez.l2j.game.template.CharTemplate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Calculo dinamico dos status efetivos do jogador considerando classe base, arma,
 * armaduras, escudos e acessorios equipados no paperdoll.
 */
public record PlayerStats(
		int pAtk,
		int pDef,
		int mAtk,
		int mDef,
		int pAtkSpd,
		int mAtkSpd,
		int critical,
		int accuracy,
		int evasion,
		int runSpeed,
		int maxLoad,
		int weightPenalty,
		int gradePenalty
) {

	public PlayerStats(int pAtk, int pDef, int mAtk, int mDef, int pAtkSpd, int mAtkSpd,
			int critical, int accuracy, int evasion, int runSpeed) {
		this(pAtk, pDef, mAtk, mDef, pAtkSpd, mAtkSpd, critical, accuracy, evasion, runSpeed, 69000, 0, 0);
	}

	public PlayerStats(int pAtk, int pDef, int mAtk, int mDef, int pAtkSpd, int mAtkSpd,
			int critical, int accuracy, int evasion, int runSpeed, int weightPenalty, int gradePenalty) {
		this(pAtk, pDef, mAtk, mDef, pAtkSpd, mAtkSpd, critical, accuracy, evasion, runSpeed, 69000, weightPenalty, gradePenalty);
	}

	public int walkSpeed(CharTemplate template) {
		int baseRun = template != null ? Math.max(1, template.runSpeed()) : 120;
		return Math.max(1, (int) Math.round(80.0 * runSpeed / baseRun));
	}

	public double movementSpeedMultiplier(CharTemplate template) {
		int baseRun = template != null ? Math.max(1, template.runSpeed()) : 120;
		return (double) runSpeed / baseRun;
	}

	public double attackSpeedMultiplier() {
		return pAtkSpd / 277.478340719;
	}

	private static final double[] DEX_BONUS = {
		0.00,
		0.39, 0.40, 0.41, 0.43, 0.44, 0.45, 0.47, 0.48, 0.50, 0.51, // 1-10
		0.53, 0.55, 0.57, 0.59, 0.61, 0.63, 0.65, 0.67, 0.70, 0.72, // 11-20
		0.74, 0.77, 0.80, 0.83, 0.86, 0.89, 0.92, 0.95, 0.98, 1.00, // 21-30 (DEX 30 = 1.00)
		1.03, 1.06, 1.09, 1.12, 1.15, 1.18, 1.21, 1.24, 1.27, 1.30, // 31-40
		1.33, 1.36, 1.39, 1.42, 1.45, 1.48, 1.51, 1.54, 1.57, 1.60, // 41-50
		1.63, 1.66, 1.69, 1.72, 1.75, 1.78, 1.81, 1.84, 1.87, 1.90, // 51-60
		1.93, 1.96, 1.99, 2.02, 2.05, 2.08, 2.11, 2.14, 2.17, 2.20  // 61-70
	};

	public static double dexBonus(int dex) {
		if (dex <= 0) {
			return 0.39;
		}
		if (dex >= DEX_BONUS.length) {
			return DEX_BONUS[DEX_BONUS.length - 1];
		}
		return DEX_BONUS[dex];
	}

	public static PlayerStats calculate(PlayerCharacter player, CharTemplate template) {
		int pAtk = template.pAtk();
		int mAtk = template.mAtk();
		int pDef = template.pDef();
		int mDef = template.mDef();
		int pAtkSpd = template.pAtkSpd();
		int mAtkSpd = template.mAtkSpd();
		int runSpeed = template.runSpeed();

		int effectiveStr = Math.max(1, template.str() + player.hennaSTR() + player.augSTR());
		int effectiveCon = Math.max(1, template.con() + player.hennaCON() + player.augCON());
		int effectiveDex = Math.max(1, template.dex() + player.hennaDEX());
		int effectiveInt = Math.max(1, template.intel() + player.hennaINT() + player.augINT());
		int effectiveWit = Math.max(1, template.wit() + player.hennaWIT());
		int effectiveMen = Math.max(1, template.men() + player.hennaMEN() + player.augMEN());
		int level = Math.max(1, player.level());

		double levelModRatio = (level + 89.0) / 90.0;
		double strRatio = com.lopez.l2j.game.template.BaseStatsTable.strBonus(effectiveStr) / com.lopez.l2j.game.template.BaseStatsTable.strBonus(template.str());
		double conRatio = com.lopez.l2j.game.template.BaseStatsTable.conBonus(effectiveCon) / com.lopez.l2j.game.template.BaseStatsTable.conBonus(template.con());
		double dexRatio = com.lopez.l2j.game.template.BaseStatsTable.dexBonus(effectiveDex) / com.lopez.l2j.game.template.BaseStatsTable.dexBonus(template.dex());
		double intRatio = com.lopez.l2j.game.template.BaseStatsTable.intBonus(effectiveInt) / com.lopez.l2j.game.template.BaseStatsTable.intBonus(template.intel());
		double witRatio = com.lopez.l2j.game.template.BaseStatsTable.witBonus(effectiveWit) / com.lopez.l2j.game.template.BaseStatsTable.witBonus(template.wit());
		double menRatio = com.lopez.l2j.game.template.BaseStatsTable.menBonus(effectiveMen) / com.lopez.l2j.game.template.BaseStatsTable.menBonus(template.men());

		int baseCrit = template.critical();
		if (baseCrit > 0 && baseCrit <= 20) {
			baseCrit *= 10;
		}

		int hitModify = 0;
		int avoidModify = 0;

		var inv = player.inventory();
		if (inv != null) {
			// Arma equipada na mao direita ou ambas
			var weapon = inv.paperdoll(ItemSlots.RHAND);
			if (weapon == null) {
				weapon = inv.paperdoll(ItemSlots.LRHAND);
			}
			if (weapon != null) {
				var wt = weapon.template();
				pAtk += wt.pAtk() + (weapon.enchant() * 2);
				mAtk += wt.mAtk() + (weapon.enchant() * 2);
				if (wt.atkSpeed() > 0) {
					pAtkSpd = wt.atkSpeed();
				}
				if (wt.critical() > 0) {
					baseCrit = wt.critical();
					if (baseCrit <= 20) {
						baseCrit *= 10;
					}
				}
			}

			// Armaduras, escudos e acessorios
			for (var item : inv.equipped()) {
				var it = item.template();
				if (it.pDef() > 0) {
					pDef += it.pDef() + item.enchant();
				}
				if (it.mDef() > 0) {
					mDef += it.mDef() + item.enchant();
				}
				avoidModify += it.avoidModify();
				hitModify += it.hitModify();
			}

			// Penalidade oficial de escudo: -8 de evasion quando equipado com escudo
			var shield = inv.paperdoll(ItemSlots.LHAND);
			if (shield != null && (shield.template().shieldDef() > 0
					|| shield.template().kind() == com.lopez.l2j.game.item.ItemTemplate.Kind.ARMOR)) {
				avoidModify -= 8;
			}
		}

		// Formulas retail Interlude baseadas em Level e atributos:
		// Accuracy: Math.round(Math.sqrt(dex) * 6) + level + weapon_hit_modify
		// Evasion: Math.round(Math.sqrt(dex) * 6) + level + avoid_modify
		// Critical: baseCrit * dexBonus
		int accuracy = (int) Math.round(Math.sqrt(effectiveDex) * 6) + level + hitModify;
		int evasion = (int) Math.round(Math.sqrt(effectiveDex) * 6) + level + avoidModify;
		int critical = (int) Math.round(baseCrit * dexBonus(effectiveDex));

		// Modificadores proporcionais de nivel e atributos base
		pAtk = (int) Math.round(pAtk * strRatio * levelModRatio);
		pDef = (int) Math.round(pDef * conRatio * levelModRatio);
		mAtk = (int) Math.round(mAtk * (intRatio * intRatio) * (levelModRatio * levelModRatio));
		mDef = (int) Math.round(mDef * menRatio * levelModRatio);
		pAtkSpd = (int) Math.round(pAtkSpd * dexRatio);
		mAtkSpd = (int) Math.round(mAtkSpd * witRatio);

		// Buffs ativos (pocoes): add depois dos itens, mul por ultimo (ordem 0x30/0x40 do L2J simplificada)
		var fx = player.effects();
		runSpeed += fx.runSpdAdd();
		accuracy += fx.accuracyAdd();
		pAtkSpd = (int) Math.round(pAtkSpd * fx.pAtkSpdMul());
		mAtkSpd = (int) Math.round(mAtkSpd * fx.mAtkSpdMul());

		// Passivas e buffs de skill (funcoes do datapack em ordem crescente de "order")
		List<StatFunc> funcs = allFuncs(player);
		if (!funcs.isEmpty()) {
			pAtk = (int) Math.round(apply(player, funcs, "pAtk", pAtk));
			pDef = (int) Math.round(apply(player, funcs, "pDef", pDef));
			mAtk = (int) Math.round(apply(player, funcs, "mAtk", mAtk));
			mDef = (int) Math.round(apply(player, funcs, "mDef", mDef));
			pAtkSpd = (int) Math.round(apply(player, funcs, "pAtkSpd", pAtkSpd));
			mAtkSpd = (int) Math.round(apply(player, funcs, "mAtkSpd", mAtkSpd));
			critical = (int) Math.round(apply(player, funcs, "rCrit", critical));
			accuracy = (int) Math.round(apply(player, funcs, "accCombat", accuracy));
			evasion = (int) Math.round(apply(player, funcs, "rEvas", evasion));
			runSpeed = (int) Math.round(apply(player, funcs, "runSpd", runSpeed));
		}

		// Penalidade de Grau (Grade / Expertise Penalty)
		int charExpertise = player.expertiseGrade();
		int weaponPenalty = 0;
		int armorPenalty = 0;
		if (inv != null) {
			for (var item : inv.equipped()) {
				var it = item.template();
				int grade = it.crystalGrade();
				if (grade > charExpertise) {
					int diff = grade - charExpertise;
					if (it.kind() == com.lopez.l2j.game.item.ItemTemplate.Kind.WEAPON) {
						if (diff > weaponPenalty) {
							weaponPenalty = diff;
						}
					} else {
						if (diff > armorPenalty) {
							armorPenalty = diff;
						}
					}
				}
			}
		}
		int gradePenalty = Math.max(weaponPenalty, armorPenalty);

		if (weaponPenalty > 0) {
			accuracy -= 16 * weaponPenalty;
			critical = (int) Math.round(critical * Math.max(0.1, 1.0 - (0.20 * weaponPenalty)));
			pAtk = (int) Math.round(pAtk * 0.67);
			pAtkSpd = (int) Math.round(pAtkSpd * 0.67);
			mAtk = (int) Math.round(mAtk * 0.67);
			mAtkSpd = (int) Math.round(mAtkSpd * 0.67);
		}

		if (armorPenalty > 0) {
			evasion -= 8 * armorPenalty;
			runSpeed = (int) Math.round(runSpeed * Math.max(0.2, 1.0 - (0.20 * armorPenalty)));
			pDef = (int) Math.round(pDef * Math.max(0.2, 1.0 - (0.20 * armorPenalty)));
			mDef = (int) Math.round(mDef * Math.max(0.2, 1.0 - (0.20 * armorPenalty)));
		}

		// Penalidade de Carga/Peso (Weight Penalty)
		double baseCapacity = template != null && template.maxLoad() > 0
				? template.maxLoad() * conRatio
				: com.lopez.l2j.game.template.BaseStatsTable.conBonus(effectiveCon) * 69000.0;
		double baseLoad = Math.floor(baseCapacity
				* (com.lopez.l2j.config.Config.ALT_WEIGHT_LIMIT > 0 ? com.lopez.l2j.config.Config.ALT_WEIGHT_LIMIT : 1.0));
		int maxLoad = (int) Math.round(apply(player, funcs, "maxLoad", baseLoad));
		if (maxLoad <= 0) {
			maxLoad = template != null && template.maxLoad() > 0 ? template.maxLoad() : 69000;
		}
		int currentLoad = inv != null ? inv.currentLoad() : 0;
		long weightPermill = ((long) currentLoad * 1000L) / maxLoad;
		int weightPenalty = 0;
		if (weightPermill >= 1000) {
			weightPenalty = 4;
		} else if (weightPermill >= 800) {
			weightPenalty = 3;
		} else if (weightPermill >= 666) {
			weightPenalty = 2;
		} else if (weightPermill >= 500) {
			weightPenalty = 1;
		}

		if (weightPenalty == 2) {
			runSpeed = (int) Math.round(runSpeed * 0.67);
		} else if (weightPenalty == 3) {
			runSpeed = (int) Math.round(runSpeed * 0.50);
		} else if (weightPenalty >= 4) {
			runSpeed = 1;
		}

		int maxPAtkSpeed = com.lopez.l2j.config.Config.getInt("MaxPAtkSpeed", 9999);
		int maxMAtkSpeed = com.lopez.l2j.config.Config.getInt("MaxMAtkSpeed", 9999);
		int maxRunSpeed = com.lopez.l2j.config.Config.getInt("MaxRunSpeed", 9999);
		int maxEvasion = com.lopez.l2j.config.Config.getInt("MaxEvasion", 200);
		int maxPCritical = com.lopez.l2j.config.Config.getInt("AltPCriticalCap", 500);

		if (player.gmSpeed() > 0) {
			runSpeed = Math.min(maxRunSpeed, runSpeed + player.gmSpeed() * 50);
		}

		if (maxPAtkSpeed > 0 && pAtkSpd > maxPAtkSpeed) {
			pAtkSpd = maxPAtkSpeed;
		}
		if (maxMAtkSpeed > 0 && mAtkSpd > maxMAtkSpeed) {
			mAtkSpd = maxMAtkSpeed;
		}
		if (maxRunSpeed > 0 && runSpeed > maxRunSpeed) {
			runSpeed = maxRunSpeed;
		}
		if (maxEvasion > 0 && evasion > maxEvasion) {
			evasion = maxEvasion;
		}
		if (maxPCritical > 0 && critical > maxPCritical) {
			critical = maxPCritical;
		}

		return new PlayerStats(Math.max(1, pAtk), Math.max(1, pDef), Math.max(1, mAtk), Math.max(1, mDef),
				Math.max(1, pAtkSpd), Math.max(1, mAtkSpd), Math.max(0, critical), accuracy, evasion,
				Math.max(1, runSpeed), maxLoad, weightPenalty, gradePenalty);
	}

	/** Passivas + buffs de skill + sets de armadura + augmentacao do jogador. */
	public static List<StatFunc> allFuncs(PlayerCharacter player) {
		var passive = player.passiveFuncs();
		var buffs = player.effects().funcs();
		var sets = player.armorSetFuncs();
		var aug = player.augmentationFuncs();
		if (buffs.isEmpty() && sets.isEmpty() && aug.isEmpty()) {
			return passive;
		}
		List<StatFunc> all = new ArrayList<>(passive.size() + buffs.size() + sets.size() + aug.size());
		all.addAll(passive);
		all.addAll(buffs);
		all.addAll(sets);
		all.addAll(aug);
		return all;
	}

	/** Aplica ao valor base as funcoes do stat (ex.: "maxHp"), em ordem crescente de order. */
	public static double applyStat(PlayerCharacter player, String stat, double base) {
		return apply(player, allFuncs(player), stat, base);
	}

	private static double apply(PlayerCharacter player, List<StatFunc> funcs, String stat, double base) {
		List<StatFunc> matching = new ArrayList<>();
		for (StatFunc f : funcs) {
			if (f.stat().equals(stat) && f.appliesTo(player)) {
				matching.add(f);
			}
		}
		if (matching.isEmpty()) {
			return base;
		}
		matching.sort(Comparator.comparingInt(StatFunc::order));
		double v = base;
		for (StatFunc f : matching) {
			v = f.apply(v, base);
		}
		return v;
	}
}
