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
		int runSpeed
) {

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

	public static PlayerStats calculate(PlayerCharacter player, CharTemplate template) {
		int pAtk = template.pAtk();
		int mAtk = template.mAtk();
		int pDef = template.pDef();
		int mDef = template.mDef();
		int pAtkSpd = template.pAtkSpd();
		int mAtkSpd = template.mAtkSpd();
		int critical = template.critical();
		int accuracy = template.accuracy();
		int evasion = template.evasion();
		int runSpeed = template.runSpeed();

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
					critical = wt.critical();
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
			}
		}

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
				Math.max(1, runSpeed));
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
