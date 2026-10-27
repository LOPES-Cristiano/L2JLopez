package com.lopez.l2j.game.model;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemSlots;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.skill.StatFunc;
import com.lopez.l2j.game.stats.CalculatorEngine;
import com.lopez.l2j.game.stats.FuncAdd;
import com.lopez.l2j.game.stats.FuncDiv;
import com.lopez.l2j.game.stats.FuncEnchant;
import com.lopez.l2j.game.stats.FuncMul;
import com.lopez.l2j.game.stats.Stat;
import com.lopez.l2j.game.template.BaseStatsTable;
import com.lopez.l2j.game.template.CharTemplate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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
		int gradePenalty,
		int str,
		int dex,
		int con,
		int intel,
		int wit,
		int men
) {

	public static final int RETAIL_MAX_PCRIT_RATE = 500;
	public static final int RETAIL_MAX_PATK_SPD = 1500;
	public static final int RETAIL_MAX_MATK_SPD = 3333;
	public static final int MAX_HENNA_STAT_BONUS = 5;

	private static volatile com.lopez.l2j.game.template.CharTemplateTable cachedTemplateTable;

	public static CharTemplate findTemplate(int classId) {
		com.lopez.l2j.game.template.CharTemplateTable tbl = cachedTemplateTable;
		if (tbl == null) {
			synchronized (PlayerStats.class) {
				tbl = cachedTemplateTable;
				if (tbl == null) {
					tbl = new com.lopez.l2j.game.template.CharTemplateTable();
					cachedTemplateTable = tbl;
				}
			}
		}
		return tbl.get(classId).orElse(null);
	}

	public PlayerStats(int pAtk, int pDef, int mAtk, int mDef, int pAtkSpd, int mAtkSpd,
			int critical, int accuracy, int evasion, int runSpeed) {
		this(pAtk, pDef, mAtk, mDef, pAtkSpd, mAtkSpd, critical, accuracy, evasion, runSpeed, 69000, 0, 0, 40, 30, 43, 21, 20, 25);
	}

	public PlayerStats(int pAtk, int pDef, int mAtk, int mDef, int pAtkSpd, int mAtkSpd,
			int critical, int accuracy, int evasion, int runSpeed, int weightPenalty, int gradePenalty) {
		this(pAtk, pDef, mAtk, mDef, pAtkSpd, mAtkSpd, critical, accuracy, evasion, runSpeed, 69000, weightPenalty, gradePenalty, 40, 30, 43, 21, 20, 25);
	}

	public int runSpd() {
		return runSpeed;
	}

	public PlayerStats(int pAtk, int pDef, int mAtk, int mDef, int pAtkSpd, int mAtkSpd,
			int critical, int accuracy, int evasion, int runSpeed, int maxLoad, int weightPenalty, int gradePenalty) {
		this(pAtk, pDef, mAtk, mDef, pAtkSpd, mAtkSpd, critical, accuracy, evasion, runSpeed, maxLoad, weightPenalty, gradePenalty, 40, 30, 43, 21, 20, 25);
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

	public static double strBonus(int str) {
		return BaseStatsTable.strBonus(str);
	}

	public static double conBonus(int con) {
		return BaseStatsTable.conBonus(con);
	}

	public static double intBonus(int intel) {
		return BaseStatsTable.intBonus(intel);
	}

	public static double witBonus(int wit) {
		return BaseStatsTable.witBonus(wit);
	}

	public static double menBonus(int men) {
		return BaseStatsTable.menBonus(men);
	}

	public static double calcLevelMod(int level) {
		return (Math.max(1, level) + 89.0) / 100.0;
	}

	/**
	 * Clamps henna stat bonus to retail +5 cap per attribute.
	 */
	public static int clampHennaBonus(int bonus) {
		return Math.min(MAX_HENNA_STAT_BONUS, bonus);
	}

	/**
	 * Retail accuracy / evasion level step bonus:
	 * +1 per level, +1 extra per level above 69, +1 extra per level above 77.
	 */
	public static int calcAccuracyOrEvasionLevelBonus(int level) {
		int lvl = Math.max(1, level);
		int bonus = lvl;
		if (lvl > 69) {
			bonus += (lvl - 69);
		}
		if (lvl > 77) {
			bonus += (lvl - 77);
		}
		return bonus;
	}

	/**
	 * Calcula o bonus de P.Atk de encantamento de arma por Crystal Grade (D/C/B/A/S) e tipo
	 * (Bow, Dual/2H Sword/Blunt/Fist, 1H) seguindo a tabela oficial Interlude (+1..+3 e over-enchant +4+).
	 */
	public static int calcWeaponEnchantPAtkBonus(ItemTemplate wt, int enchant) {
		if (wt == null || enchant <= 0) {
			return 0;
		}
		int grade = wt.crystalGrade();
		String sub = wt.subType() != null ? wt.subType().toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "") : "";
		boolean isBow = "bow".equals(sub) || "crossbow".equals(sub);
		boolean isTwoHanded = (wt.bodyPart() & ItemSlots.SLOT_LR_HAND) == ItemSlots.SLOT_LR_HAND;
		boolean isDualOr2H = !isBow && ("dual".equals(sub) || "dualsword".equals(sub) || "dualfist".equals(sub)
				|| "fist".equals(sub) || "bigsword".equals(sub) || "bigblunt".equals(sub)
				|| (isTwoHanded && ("sword".equals(sub) || "blunt".equals(sub))));

		int overEnchant = Math.max(0, enchant - 3);
		return switch (grade) {
			case 5 -> { // S-Grade
				if (isBow) yield 10 * enchant + 10 * overEnchant;
				if (isDualOr2H) yield 6 * enchant + 6 * overEnchant;
				yield 5 * enchant + 5 * overEnchant;
			}
			case 4 -> { // A-Grade
				if (isBow) yield 8 * enchant + 8 * overEnchant;
				if (isDualOr2H) yield 5 * enchant + 5 * overEnchant;
				yield 4 * enchant + 4 * overEnchant;
			}
			case 3, 2 -> { // B-Grade, C-Grade
				if (isBow) yield 6 * enchant + 6 * overEnchant;
				if (isDualOr2H) yield 4 * enchant + 4 * overEnchant;
				yield 3 * enchant + 3 * overEnchant;
			}
			default -> { // D-Grade (1) & No-Grade (0)
				if (isBow) yield 4 * enchant + 4 * overEnchant;
				yield 2 * enchant + 2 * overEnchant;
			}
		};
	}

	/**
	 * Calcula o bonus de M.Atk de encantamento de arma por Crystal Grade (D/C/B/A/S)
	 * seguindo a tabela oficial Interlude (+1..+3 e over-enchant +4+).
	 */
	public static int calcWeaponEnchantMAtkBonus(ItemTemplate wt, int enchant) {
		if (wt == null || enchant <= 0) {
			return 0;
		}
		int grade = wt.crystalGrade();
		int overEnchant = Math.max(0, enchant - 3);
		return switch (grade) {
			case 5 -> 4 * enchant + 4 * overEnchant; // S-Grade: +4 (1..3), +8 (4+)
			case 4, 3, 2 -> 3 * enchant + 3 * overEnchant; // A/B/C-Grade: +3 (1..3), +6 (4+)
			default -> 2 * enchant + 2 * overEnchant; // D/No-Grade: +2 (1..3), +4 (4+)
		};
	}

	/**
	 * Calcula o bonus de P.Def ou M.Def de encantamento de armaduras, escudos e joias:
	 * +1 por nivel ate +3, e +3 por nivel de over-enchant (+4 em diante).
	 */
	public static int calcArmorEnchantDefBonus(int enchant) {
		if (enchant <= 0) {
			return 0;
		}
		int overEnchant = Math.max(0, enchant - 3);
		return enchant + (2 * overEnchant);
	}

	/**
	 * Calcula o bonus de HP de armadura completa (+4 full-body ou +6 armor set).
	 */
	public static int calcArmorEnchantHpBonus(Inventory inv) {
		if (inv == null) {
			return 0;
		}
		var chest = inv.paperdoll(ItemSlots.CHEST);
		if (chest == null || chest.template() == null) {
			return 0;
		}
		int minEnchant = chest.enchant();
		boolean isFullArmor = chest.template().bodyPart() == ItemSlots.SLOT_FULL_ARMOR;
		if (!isFullArmor) {
			var legs = inv.paperdoll(ItemSlots.LEGS);
			if (legs == null) {
				return 0;
			}
			minEnchant = Math.min(minEnchant, legs.enchant());
		} else if (chest.enchant() >= 4 && chest.enchant() < 6) {
			// Full-body armor at +4/+5 grants an early HP bonus tier
			return 40 * Math.max(1, chest.template().crystalGrade());
		}
		var head = inv.paperdoll(ItemSlots.HEAD);
		var gloves = inv.paperdoll(ItemSlots.GLOVES);
		var feet = inv.paperdoll(ItemSlots.FEET);
		if (head == null || gloves == null || feet == null) {
			return 0;
		}
		minEnchant = Math.min(minEnchant, Math.min(head.enchant(), Math.min(gloves.enchant(), feet.enchant())));
		if (minEnchant < 6) {
			return 0;
		}
		return switch (chest.template().crystalGrade()) {
			case 5 -> 440; // S-Grade +6 set
			case 4 -> 360; // A-Grade +6 set
			case 3 -> 280; // B-Grade +6 set
			case 2 -> 200; // C-Grade +6 set
			case 1 -> 120; // D-Grade +6 set
			default -> 0;
		};
	}

	/**
	 * Calcula a deducao de P.Def base dos slots de armadura ocupados (Interlude FuncPDefMod).
	 */
	public static int calcOccupiedSlotBasePDefDeduction(Inventory inv, boolean isMage) {
		if (inv == null) {
			return 0;
		}
		int deduct = 0;
		if (inv.paperdoll(ItemSlots.HEAD) != null) deduct += 12;
		var chest = inv.paperdoll(ItemSlots.CHEST);
		if (chest != null) {
			deduct += isMage ? 15 : 31;
			if (chest.template() != null && chest.template().bodyPart() == ItemSlots.SLOT_FULL_ARMOR) {
				deduct += isMage ? 8 : 18;
			}
		}
		if (inv.paperdoll(ItemSlots.LEGS) != null && (chest == null || chest.template() == null
				|| chest.template().bodyPart() != ItemSlots.SLOT_FULL_ARMOR)) {
			deduct += isMage ? 8 : 18;
		}
		if (inv.paperdoll(ItemSlots.GLOVES) != null) deduct += 8;
		if (inv.paperdoll(ItemSlots.FEET) != null) deduct += 7;
		return deduct;
	}

	/**
	 * Calcula a deducao de M.Def base dos slots de joia ocupados (Interlude FuncMDefMod:
	 * Ring=5, Earring=9, Necklace=13).
	 */
	public static int calcOccupiedSlotBaseMDefDeduction(Inventory inv) {
		if (inv == null) {
			return 0;
		}
		int deduct = 0;
		if (inv.paperdoll(ItemSlots.LFINGER) != null) deduct += 5;
		if (inv.paperdoll(ItemSlots.RFINGER) != null) deduct += 5;
		if (inv.paperdoll(ItemSlots.LEAR) != null) deduct += 9;
		if (inv.paperdoll(ItemSlots.REAR) != null) deduct += 9;
		if (inv.paperdoll(ItemSlots.NECK) != null) deduct += 13;
		return deduct;
	}

	/**
	 * Formula quadratica retail de MaxHP por nivel (lvlHpAdd / lvlHpMod) multiplicada por CON bonus.
	 */
	public static double calcRetailMaxHp(CharTemplate template, int level, int effectiveCon) {
		if (template == null) {
			return 100.0 * conBonus(effectiveCon);
		}
		return template.maxHpForLevel(Math.max(1, level)) * conBonus(effectiveCon);
	}

	/**
	 * Formula quadratica retail de MaxMP por nivel (lvlMpAdd / lvlMpMod) multiplicada por MEN bonus.
	 */
	public static double calcRetailMaxMp(CharTemplate template, int level, int effectiveMen) {
		if (template == null) {
			return 50.0 * menBonus(effectiveMen);
		}
		return template.maxMpForLevel(Math.max(1, level)) * menBonus(effectiveMen);
	}

	/**
	 * Formula quadratica retail de MaxCP por nivel (lvlCpAdd / lvlCpMod) multiplicada por CON bonus.
	 */
	public static double calcRetailMaxCp(CharTemplate template, int level, int effectiveCon) {
		if (template == null) {
			return 50.0 * conBonus(effectiveCon);
		}
		return template.maxCpForLevel(Math.max(1, level)) * conBonus(effectiveCon);
	}

	/**
	 * Constroi uma instancia de CalculatorEngine populada com todas as StatFuncs ativas do jogador.
	 */
	public static CalculatorEngine buildCalculatorEngine(PlayerCharacter player) {
		CalculatorEngine engine = new CalculatorEngine();
		if (player == null) {
			return engine;
		}
		for (StatFunc f : allFuncs(player)) {
			Stat stat = Stat.fromString(f.stat());
			if (stat == null || !f.appliesTo(player, null)) {
				continue;
			}
			switch (f.op()) {
				case ADD -> {
					if (f.order() == 0x20 || f.order() == 0x0C) {
						engine.addFunc(new FuncEnchant(stat, f, f.value()));
					} else {
						engine.addFunc(new FuncAdd(stat, f, f.value()));
					}
				}
				case SUB -> engine.addFunc(new FuncAdd(stat, f, -f.value()));
				case MUL -> engine.addFunc(new FuncMul(stat, f, f.value()));
				case DIV -> engine.addFunc(new FuncDiv(stat, f, f.value()));
				default -> {
				}
			}
		}
		return engine;
	}

	/**
	 * Verifica se o jogador possui penalidade de grade de arma equipada.
	 */
	public static int calcWeaponGradePenalty(PlayerCharacter player) {
		if (player == null || !com.lopez.l2j.config.Config.GRADE_PENALTY || player.inventory() == null) {
			return 0;
		}
		int charExpertise = player.expertiseGrade();
		int weaponPenalty = 0;
		for (var item : player.inventory().equipped()) {
			var it = item.template();
			if (it != null && it.kind() == ItemTemplate.Kind.WEAPON) {
				int diff = it.crystalGrade() - charExpertise;
				if (diff > weaponPenalty) {
					weaponPenalty = diff;
				}
			}
		}
		return Math.min(5, weaponPenalty);
	}

	/**
	 * Verifica se o jogador possui penalidade de grade de armadura/joia equipada.
	 */
	public static int calcArmorGradePenalty(PlayerCharacter player) {
		if (player == null || !com.lopez.l2j.config.Config.GRADE_PENALTY || player.inventory() == null) {
			return 0;
		}
		int charExpertise = player.expertiseGrade();
		int armorPenalty = 0;
		for (var item : player.inventory().equipped()) {
			var it = item.template();
			if (it == null || it.kind() == ItemTemplate.Kind.WEAPON) {
				continue;
			}
			int bp = it.bodyPart();
			if (bp == ItemSlots.SLOT_HAIR || bp == ItemSlots.SLOT_FACE || bp == ItemSlots.SLOT_HAIRALL) {
				continue;
			}
			int diff = it.crystalGrade() - charExpertise;
			if (diff > armorPenalty) {
				armorPenalty = diff;
			}
		}
		return Math.min(5, armorPenalty);
	}

	/**
	 * Calcula o nivel de penalidade de peso (0 a 4) com base na carga atual e maxima.
	 */
	public static int calcWeightPenaltyLevel(int currentLoad, int maxLoad) {
		if (maxLoad <= 0) {
			return 0;
		}
		long weightPermill = ((long) Math.max(0, currentLoad) * 1000L) / maxLoad;
		if (weightPermill >= 1000) {
			return 4;
		} else if (weightPermill >= 800) {
			return 3;
		} else if (weightPermill >= 666) {
			return 2;
		} else if (weightPermill >= 500) {
			return 1;
		}
		return 0;
	}

	public static PlayerStats calculate(PlayerCharacter player, CharTemplate template) {
		int pAtk = template.pAtk();
		int mAtk = template.mAtk();
		int pDef = template.pDef();
		int mDef = template.mDef();
		int pAtkSpd = template.pAtkSpd();
		int mAtkSpd = template.mAtkSpd();
		int runSpeed = template.runSpeed();

		List<StatFunc> funcs = allFuncs(player);

		int baseStr = template.str() + clampHennaBonus(player.hennaSTR()) + player.augSTR();
		int baseCon = template.con() + clampHennaBonus(player.hennaCON()) + player.augCON();
		int baseDex = template.dex() + clampHennaBonus(player.hennaDEX()) + player.augDEX();
		int baseInt = template.intel() + clampHennaBonus(player.hennaINT()) + player.augINT();
		int baseWit = template.wit() + clampHennaBonus(player.hennaWIT()) + player.augWIT();
		int baseMen = template.men() + clampHennaBonus(player.hennaMEN()) + player.augMEN();

		int effectiveStr = Math.max(1, (int) Math.round(apply(player, funcs, "STR", baseStr)));
		int effectiveCon = Math.max(1, (int) Math.round(apply(player, funcs, "CON", baseCon)));
		int effectiveDex = Math.max(1, (int) Math.round(apply(player, funcs, "DEX", baseDex)));
		int effectiveInt = Math.max(1, (int) Math.round(apply(player, funcs, "INT", baseInt)));
		int effectiveWit = Math.max(1, (int) Math.round(apply(player, funcs, "WIT", baseWit)));
		int effectiveMen = Math.max(1, (int) Math.round(apply(player, funcs, "MEN", baseMen)));
		int level = Math.max(1, player.level());

		double levelModRatio = com.lopez.l2j.config.Config.USE_CHAR_LEVEL_MODIFIER ? (calcLevelMod(level) / calcLevelMod(1)) : 1.0;
		double strRatio = BaseStatsTable.strBonus(effectiveStr) / BaseStatsTable.strBonus(template.str());
		double conRatio = BaseStatsTable.conBonus(effectiveCon) / BaseStatsTable.conBonus(template.con());
		double dexRatio = BaseStatsTable.dexBonus(effectiveDex) / BaseStatsTable.dexBonus(template.dex());
		double intRatio = BaseStatsTable.intBonus(effectiveInt) / BaseStatsTable.intBonus(template.intel());
		double witRatio = BaseStatsTable.witBonus(effectiveWit) / BaseStatsTable.witBonus(template.wit());
		double menRatio = BaseStatsTable.menBonus(effectiveMen) / BaseStatsTable.menBonus(template.men());

		int baseCrit = template.critical();
		if (baseCrit <= 0) {
			baseCrit = 40;
		} else if (baseCrit <= 20) {
			baseCrit *= 10;
		}

		int hitModify = 0;
		int avoidModify = 0;
		int weaponEnchantPAtk = 0;
		int weaponEnchantMAtk = 0;
		int armorEnchantPDef = 0;
		int armorEnchantMDef = 0;

		var inv = player.inventory();
		if (inv != null) {
			// Arma equipada na mao direita ou ambas
			var weapon = inv.paperdoll(ItemSlots.RHAND);
			if (weapon == null) {
				weapon = inv.paperdoll(ItemSlots.LRHAND);
			}
			if (weapon != null) {
				var wt = weapon.template();
				pAtk += wt.pAtk();
				mAtk += wt.mAtk();
				weaponEnchantPAtk = calcWeaponEnchantPAtkBonus(wt, weapon.enchant());
				weaponEnchantMAtk = calcWeaponEnchantMAtkBonus(wt, weapon.enchant());
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
					pDef += it.pDef();
					armorEnchantPDef += calcArmorEnchantDefBonus(item.enchant());
				}
				if (it.mDef() > 0) {
					mDef += it.mDef();
					armorEnchantMDef += calcArmorEnchantDefBonus(item.enchant());
				}
				avoidModify += it.avoidModify();
				hitModify += it.hitModify();
			}

			// Penalidade oficial de escudo: -8 de evasion quando equipado com escudo
			var shield = inv.paperdoll(ItemSlots.LHAND);
			if (shield != null && (shield.template().shieldDef() > 0
					|| shield.template().kind() == ItemTemplate.Kind.ARMOR)) {
				avoidModify -= 8;
			}
		}

		// Formulas retail Interlude baseadas em Level (com step >69 e >77) e atributos:
		// Accuracy: Math.round(Math.sqrt(dex) * 6) + levelBonus + weapon_hit_modify
		// Evasion: Math.round(Math.sqrt(dex) * 6) + levelBonus + avoid_modify
		// Critical: baseCrit * dexBonus
		int levelAccEvaBonus = calcAccuracyOrEvasionLevelBonus(level);
		int accuracy = (int) Math.round(Math.sqrt(effectiveDex) * 6) + levelAccEvaBonus + hitModify;
		int evasion = (int) Math.round(Math.sqrt(effectiveDex) * 6) + levelAccEvaBonus + avoidModify;
		int critical = (int) Math.round(baseCrit * dexBonus(effectiveDex));

		// Modificadores proporcionais de nivel e atributos base, somando o bonus de enchant (order 0x0C/0x20)
		pAtk = (int) Math.round(pAtk * strRatio * levelModRatio) + weaponEnchantPAtk;
		pDef = (int) Math.round(pDef * conRatio * levelModRatio) + armorEnchantPDef;
		mAtk = (int) Math.round(mAtk * (intRatio * intRatio) * (levelModRatio * levelModRatio)) + weaponEnchantMAtk;
		mDef = (int) Math.round(mDef * menRatio * levelModRatio) + armorEnchantMDef;
		pAtkSpd = (int) Math.round(pAtkSpd * dexRatio);
		mAtkSpd = (int) Math.round(mAtkSpd * witRatio);

		// Buffs ativos (pocoes): add depois dos itens, mul por ultimo (ordem 0x30/0x40 do L2J simplificada)
		var fx = player.effects();
		runSpeed += fx.runSpdAdd();
		accuracy += fx.accuracyAdd();
		pAtkSpd = (int) Math.round(pAtkSpd * fx.pAtkSpdMul());
		mAtkSpd = (int) Math.round(mAtkSpd * fx.mAtkSpdMul());

		// Passivas, buffs de skill, ArmorSets, Augmentations e SA de itens (funcoes do datapack em ordem crescente de "order")
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

		// Multiplicador global de velocidade de movimento (RateRunSpeed)
		if (com.lopez.l2j.config.Config.RATE_RUN_SPEED > 0 && com.lopez.l2j.config.Config.RATE_RUN_SPEED != 1.0f) {
			runSpeed = (int) Math.round(runSpeed * com.lopez.l2j.config.Config.RATE_RUN_SPEED);
		}

		// Penalidade de Grau (Grade / Expertise Penalty)
		int weaponPenalty = calcWeaponGradePenalty(player);
		int armorPenalty = calcArmorGradePenalty(player);
		int gradePenalty = Math.max(weaponPenalty, armorPenalty);

		if (weaponPenalty > 0) {
			accuracy -= 16 * weaponPenalty;
			critical = (int) Math.round(critical * Math.max(0.1, 1.0 - (0.10 * weaponPenalty)));
			pAtk = (int) Math.round(pAtk * 0.67);
			pAtkSpd = (int) Math.round(pAtkSpd * 0.80);
			mAtk = (int) Math.round(mAtk * 0.67);
			mAtkSpd = (int) Math.round(mAtkSpd * 0.80);
		}

		if (armorPenalty > 0) {
			evasion -= 8 * armorPenalty;
			runSpeed = (int) Math.round(runSpeed * Math.max(0.2, 1.0 - (0.20 * armorPenalty)));
			pDef = (int) Math.round(pDef * Math.max(0.2, 1.0 - (0.20 * armorPenalty)));
			mDef = (int) Math.round(mDef * Math.max(0.2, 1.0 - (0.20 * armorPenalty)));
			pAtkSpd = (int) Math.round(pAtkSpd * Math.max(0.2, 1.0 - (0.1667 * armorPenalty)));
			mAtkSpd = (int) Math.round(mAtkSpd * Math.max(0.2, 1.0 - (0.1667 * armorPenalty)));
		}

		// Penalidade de Carga/Peso (Weight Penalty)
		double baseCapacity = template != null && template.maxLoad() > 0
				? template.maxLoad() * conRatio
				: BaseStatsTable.conBonus(effectiveCon) * 69000.0;
		if (com.lopez.l2j.config.Config.INCREASE_WEIGHT_LIMIT_BY_LEVEL) {
			baseCapacity *= levelModRatio;
		}
		double weightLimitMultiplier = com.lopez.l2j.config.Config.ALT_WEIGHT_LIMIT > 0
				? com.lopez.l2j.config.Config.ALT_WEIGHT_LIMIT
				: 1.0;
		double baseLoad = Math.floor(Math.floor(baseCapacity) * weightLimitMultiplier);
		double appliedLoad = apply(player, funcs, "maxLoad", baseLoad);
		long rawMaxLoad = Math.round(appliedLoad);
		int maxLoad;
		if (rawMaxLoad >= Integer.MAX_VALUE || Double.isInfinite(appliedLoad) || Double.isNaN(appliedLoad) || rawMaxLoad <= 0) {
			if (weightLimitMultiplier > 1.0) {
				maxLoad = Integer.MAX_VALUE;
			} else {
				maxLoad = template != null && template.maxLoad() > 0 ? template.maxLoad() : 69000;
			}
		} else {
			maxLoad = (int) Math.min(Integer.MAX_VALUE, Math.max(1L, rawMaxLoad));
		}
		int currentLoad = inv != null ? inv.currentLoad() : 0;
		int weightPenalty = calcWeightPenaltyLevel(currentLoad, maxLoad);

		if (weightPenalty == 2) {
			runSpeed = (int) Math.round(runSpeed * 0.67);
		} else if (weightPenalty == 3) {
			runSpeed = (int) Math.round(runSpeed * 0.50);
		} else if (weightPenalty >= 4) {
			runSpeed = 1;
		}

		int maxPAtkSpeed = com.lopez.l2j.config.Config.getInt("MaxPAtkSpeed", RETAIL_MAX_PATK_SPD);
		int maxMAtkSpeed = com.lopez.l2j.config.Config.getInt("MaxMAtkSpeed", RETAIL_MAX_MATK_SPD);
		int maxRunSpeed = com.lopez.l2j.config.Config.getInt("MaxRunSpeed", 500);
		int maxEvasion = com.lopez.l2j.config.Config.getInt("MaxEvasion", 200);
		int maxPCritical = com.lopez.l2j.config.Config.getInt("AltPCriticalCap", RETAIL_MAX_PCRIT_RATE);

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
				Math.max(1, runSpeed), maxLoad, weightPenalty, gradePenalty,
				effectiveStr, effectiveDex, effectiveCon, effectiveInt, effectiveWit, effectiveMen);
	}

	/** Passivas + buffs de skill + sets de armadura + augmentacao + Boss Dolls + XML stats / SA dos itens equipados. */
	public static List<StatFunc> allFuncs(PlayerCharacter player) {
		var passive = player.passiveFuncs();
		var buffs = player.effects().funcs();
		var sets = player.armorSetFuncs();
		var aug = player.augmentationFuncs();
		var dolls = com.lopez.l2j.game.pve.DollService.getInstance().getActiveStatFuncs(player);
		List<StatFunc> equipFuncs = List.of();
		if (player.inventory() != null) {
			for (ItemInstance item : player.inventory().equipped()) {
				if (item.template() != null && !item.template().statFuncs().isEmpty()) {
					if (equipFuncs.isEmpty()) {
						equipFuncs = new ArrayList<>();
					}
					equipFuncs.addAll(item.template().statFuncs());
				}
			}
			if (com.lopez.l2j.config.Config.getBoolean("EnchantArmorBonusHp", true)) {
				int armorHpBonus = calcArmorEnchantHpBonus(player.inventory());
				if (armorHpBonus > 0) {
					if (equipFuncs.isEmpty()) {
						equipFuncs = new ArrayList<>();
					}
					equipFuncs.add(new StatFunc("maxHp", StatFunc.Op.ADD, 0x40, armorHpBonus));
				}
			}
		}
		if (buffs.isEmpty() && sets.isEmpty() && aug.isEmpty() && dolls.isEmpty() && equipFuncs.isEmpty()) {
			return passive;
		}
		List<StatFunc> all = new ArrayList<>(passive.size() + buffs.size() + sets.size() + aug.size() + dolls.size() + equipFuncs.size());
		all.addAll(passive);
		all.addAll(buffs);
		all.addAll(sets);
		all.addAll(aug);
		all.addAll(dolls);
		all.addAll(equipFuncs);
		return all;
	}

	/** Aplica ao valor base as funcoes do stat (ex.: "maxHp"), em ordem crescente de order. */
	public static double applyStat(PlayerCharacter player, String stat, double base) {
		return applyStat(player, null, stat, base);
	}

	/** Aplica ao valor base as funcoes do stat considerando tambem o alvo (ex.: posicoes front/behind). */
	public static double applyStat(PlayerCharacter player, Object target, String stat, double base) {
		if (player == null) {
			return base;
		}
		return apply(player, target, allFuncs(player), stat, base);
	}

	private static double apply(PlayerCharacter player, List<StatFunc> funcs, String stat, double base) {
		return apply(player, null, funcs, stat, base);
	}

	private static double apply(PlayerCharacter player, Object target, List<StatFunc> funcs, String stat, double base) {
		List<StatFunc> matching = new ArrayList<>();
		for (StatFunc f : funcs) {
			if (f.stat().equalsIgnoreCase(stat) && f.appliesTo(player, target)) {
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
