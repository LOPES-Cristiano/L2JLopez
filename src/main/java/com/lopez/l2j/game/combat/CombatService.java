package com.lopez.l2j.game.combat;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.item.ItemSlots;
import com.lopez.l2j.game.model.Creature;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.PlayerStats;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.game.template.BaseStatsTable;
import com.lopez.l2j.game.template.CharTemplate;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Calculo de combate e formulas fisicas/magicas do Lineage II Interlude.
 */
@Service
public class CombatService {

	private final double rateXp;
	private final double rateSp;
	private final com.lopez.l2j.game.geodata.GeoEngine geoEngine;
	private final com.lopez.l2j.game.olympiad.OlyClassDamageManager olyDamageManager;
	private final com.lopez.l2j.game.champion.ChampionService championService;
	private final com.lopez.l2j.game.zone.ZoneTable zones;

	public CombatService() {
		this(1.0, 1.0, null, null, null, null);
	}

	public CombatService(double rateXp, double rateSp) {
		this(rateXp, rateSp, null, null, null, null);
	}

	public CombatService(double rateXp, double rateSp, com.lopez.l2j.game.geodata.GeoEngine geoEngine) {
		this(rateXp, rateSp, geoEngine, null, null, null);
	}

	public CombatService(
			double rateXp,
			double rateSp,
			com.lopez.l2j.game.geodata.GeoEngine geoEngine,
			com.lopez.l2j.game.olympiad.OlyClassDamageManager olyDamageManager) {
		this(rateXp, rateSp, geoEngine, olyDamageManager, null, null);
	}

	public CombatService(
			double rateXp,
			double rateSp,
			com.lopez.l2j.game.geodata.GeoEngine geoEngine,
			com.lopez.l2j.game.olympiad.OlyClassDamageManager olyDamageManager,
			com.lopez.l2j.game.champion.ChampionService championService) {
		this(rateXp, rateSp, geoEngine, olyDamageManager, championService, null);
	}

	@Autowired
	public CombatService(
			@Value("${l2.rates.xp:1.0}") double rateXp,
			@Value("${l2.rates.sp:1.0}") double rateSp,
			@Autowired(required = false) com.lopez.l2j.game.geodata.GeoEngine geoEngine,
			@Autowired(required = false) com.lopez.l2j.game.olympiad.OlyClassDamageManager olyDamageManager,
			@Autowired(required = false) com.lopez.l2j.game.champion.ChampionService championService,
			@Autowired(required = false) com.lopez.l2j.game.zone.ZoneTable zones) {
		this.rateXp = rateXp;
		this.rateSp = rateSp;
		this.geoEngine = geoEngine;
		this.olyDamageManager = olyDamageManager;
		this.championService = championService;
		this.zones = zones;
	}

	public com.lopez.l2j.game.olympiad.OlyClassDamageManager olyDamageManager() {
		return olyDamageManager;
	}

	public com.lopez.l2j.game.geodata.GeoEngine geoEngine() {
		return geoEngine;
	}

	public boolean canSeeTarget(int x, int y, int z, int tx, int ty, int tz) {
		if (geoEngine != null && geoEngine.isEnabled()) {
			return geoEngine.canSeeTarget(x, y, z, tx, ty, tz);
		}
		return Math.abs(tz - z) <= 250;
	}

	public boolean canSeeTarget(Creature actor, Creature target) {
		if (actor == null || target == null) {
			return false;
		}
		if (actor.instanceId() != target.instanceId()) {
			return false;
		}
		if (geoEngine != null && geoEngine.isEnabled()) {
			return geoEngine.canSeeTarget(actor, target);
		}
		return Math.abs(actor.z() - target.z()) <= 250;
	}

	public boolean canSeeTarget(PlayerCharacter player, NpcInstance npc) {
		return canSeeTarget((Creature) player, (Creature) npc);
	}

	public boolean canSeeTarget(NpcInstance npc, PlayerCharacter player) {
		return canSeeTarget((Creature) npc, (Creature) player);
	}

	public boolean canSeeTarget(PlayerCharacter player, PlayerCharacter target) {
		return canSeeTarget((Creature) player, (Creature) target);
	}

	public boolean canMoveToTarget(int x, int y, int z, int tx, int ty, int tz) {
		if (geoEngine != null && geoEngine.isEnabled()) {
			return geoEngine.canMoveToTarget(x, y, z, tx, ty, tz);
		}
		return true;
	}

	public boolean canMoveToTarget(Creature actor, Creature target) {
		if (actor == null || target == null) {
			return false;
		}
		if (actor.instanceId() != target.instanceId()) {
			return false;
		}
		if (geoEngine != null) {
			return geoEngine.canMoveToTarget(actor, target);
		}
		return true;
	}

	public boolean canMoveToTarget(PlayerCharacter player, PlayerCharacter target) {
		if (geoEngine != null && player != null && target != null) {
			return geoEngine.canMoveToTarget(player, target);
		}
		return true;
	}

	public boolean canMoveToTarget(PlayerCharacter player, NpcInstance npc) {
		if (geoEngine != null && player != null && npc != null) {
			return geoEngine.canMoveToTarget(player, npc);
		}
		return true;
	}

	public boolean canMoveToTarget(NpcInstance npc, PlayerCharacter player) {
		if (geoEngine != null && npc != null && player != null) {
			return geoEngine.canMoveToTarget(npc, player);
		}
		return true;
	}

	public short getHeight(int x, int y, int z) {
		if (geoEngine != null) {
			return geoEngine.getHeight(x, y, z);
		}
		return (short) z;
	}

	public record HitResult(int damage, int flags, boolean isDead, int remainingHp, int maxHp, long expReward,
			int spReward, boolean resisted) {
		public HitResult(int damage, int flags, boolean isDead, int remainingHp, int maxHp, long expReward, int spReward) {
			this(damage, flags, isDead, remainingHp, maxHp, expReward, spReward, false);
		}
	}

	public record HitPlan(int damage, int flags, boolean miss, boolean crit) {
	}

	/**
	 * Calcula se uma magia passa com sucesso ou se falha/resiste devido a diferenca de nivel
	 * entre o nivel magico do skill e o nivel do alvo (retail Lineage 2 / L2JDream Formulas.calcMagicSuccess).
	 */
	public static boolean calcMagicSuccess(int magicLevel, int attackerLevel, int targetLevel) {
		return Formulas.calcMagicSuccess(magicLevel, attackerLevel, targetLevel);
	}

	public enum RelativePosition {
		FRONT,
		SIDE,
		BEHIND
	}

	public static RelativePosition getRelativePosition(int attackerX, int attackerY, int targetX, int targetY, int targetHeading) {
		double dx = attackerX - targetX;
		double dy = attackerY - targetY;
		double targetFacingRad = (targetHeading / 65536.0) * 2 * Math.PI;
		double attackAngleRad = Math.atan2(dy, dx);
		double diff = attackAngleRad - targetFacingRad;
		while (diff <= -Math.PI) diff += 2 * Math.PI;
		while (diff > Math.PI) diff -= 2 * Math.PI;
		double deg = Math.toDegrees(Math.abs(diff));
		if (deg <= 60.0) {
			return RelativePosition.FRONT;
		} else if (deg >= 120.0) {
			return RelativePosition.BEHIND;
		} else {
			return RelativePosition.SIDE;
		}
	}

	public static int getBlowChance(RelativePosition pos) {
		return switch (pos) {
			case BEHIND -> Config.BLOW_BEHIND;
			case SIDE -> Config.BLOW_SIDE;
			case FRONT -> Config.BLOW_FRONT;
		};
	}

	public static boolean calcBlowSuccess(PlayerCharacter attacker, int attackerX, int attackerY, int targetX, int targetY, int targetHeading) {
		RelativePosition pos = getRelativePosition(attackerX, attackerY, targetX, targetY, targetHeading);
		int dex = 30;
		if (attacker != null) {
			dex = Math.max(1, 30 + PlayerStats.clampHennaBonus(attacker.hennaDEX()) + attacker.augDEX());
		}
		double chance = Formulas.calcBlowRate(attacker, pos, dex);
		return ThreadLocalRandom.current().nextDouble(100.0) < chance;
	}

	public static boolean calcBlowSuccess(int attackerX, int attackerY, int targetX, int targetY, int targetHeading) {
		return calcBlowSuccess(null, attackerX, attackerY, targetX, targetY, targetHeading);
	}

	public static double getLethalRateMultiplier(boolean isDagger, boolean isBow) {
		if (isDagger) {
			return Config.ALT_LETHAL_RATE_DAGGER;
		} else if (isBow) {
			return Config.ALT_LETHAL_RATE_ARCHERY;
		} else {
			return Config.ALT_LETHAL_RATE_OTHER;
		}
	}

	public static boolean calcLethalSuccess(double baseChance, boolean isDagger, boolean isBow) {
		double rate = baseChance * getLethalRateMultiplier(isDagger, isBow);
		return ThreadLocalRandom.current().nextDouble(100.0) < Math.min(100.0, rate);
	}

	public static boolean isUsingDaggerWeapon(PlayerCharacter player) {
		if (player == null || player.inventory() == null) {
			return false;
		}
		var w = player.inventory().paperdoll(ItemSlots.RHAND);
		if (w == null) {
			w = player.inventory().paperdoll(ItemSlots.LRHAND);
		}
		return w != null && w.template() != null && w.template().subType() != null
				&& w.template().subType().toLowerCase(Locale.ROOT).contains("dagger");
	}

	public HitResult attackNpc(PlayerCharacter attacker, CharTemplate template, NpcInstance target) {
		return attackNpc(attacker, template, target, -1);
	}

	public static final int RAID_CURSE_SILENCE = 4215;
	public static final int RAID_CURSE_PETRIFY = 4515;

	/**
	 * Verifica se o jogador sofre a Maldicao de Raid Boss (Raid Curse)
	 * por estar em nivel muito superior ao chefe (Lucera / Dream / retail L2).
	 */
	public boolean checkRaidCurse(PlayerCharacter attacker, NpcInstance target) {
		if (attacker == null || target == null || target.template() == null) {
			return false;
		}
		if (attacker.isGm()) {
			return false;
		}
		boolean isRaid = target.template().isRaidBoss() || target.template().isGrandBoss()
				|| target.isRaidMinion();
		if (!isRaid) {
			return false;
		}

		// Verificacao especial Queen Ant: nivel maximo seguro (retail: 48)
		if (target.npcId() >= 29001 && target.npcId() <= 29005) {
			if (attacker.level() > Config.QUEEN_ANT_MAX_SAFE_LEVEL) {
				return true;
			}
		}

		if (Config.PARALIZE_ON_RAID_LEVEL_DIFF) {
			int diff = attacker.level() - target.template().level();
			if (diff > Config.RAID_MAX_LEVEL_DIFF) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Planeja o golpe contra o monstro, calculando dano, acerto/erro, critico posicional e soulshot
	 * SEM alterar o HP nem o estado de vida do monstro antes da animacao atingir o alvo.
	 */
	public HitPlan planAttackNpc(PlayerCharacter attacker, CharTemplate template, NpcInstance target,
			int soulshotGrade) {
		if (target.isDead()) {
			return new HitPlan(0, 0, false, false);
		}

		if (checkRaidCurse(attacker, target)) {
			return new HitPlan(0, 0, false, false);
		}

		var stats = PlayerStats.calculate(attacker, template);
		boolean soulshot = soulshotGrade >= 0;
		double pAtk = stats.pAtk() * (soulshot ? 2.0 : 1.0);
		double pDef = getEffectiveNpcPDef(target) * calcNpcTypeDefBonus(attacker, target);

		RelativePosition pos = attacker != null
				? getRelativePosition(attacker.x(), attacker.y(), target.x(), target.y(), target.heading())
				: RelativePosition.FRONT;

		// Chance de acerto: precisao vs evasao com bonus posicional (+0/+5/+10)
		int acc = stats.accuracy();
		int eva = Math.max(20, target.template().level() + 30);
		int hitChance = Formulas.calcHitChance(acc, eva, pos);
		boolean miss = ThreadLocalRandom.current().nextInt(100) >= hitChance;

		if (miss) {
			return new HitPlan(0, 0x80, true, false);
		}

		// Chance de critico (base 40-500 per 1000, com multiplicador posicional Front/Side/Behind)
		double posCritMult = Formulas.calcPositionCritMultiplier(pos);
		double rawCrit = PlayerStats.applyStat(attacker, target, "rCrit", stats.critical() * posCritMult);
		int critRate = Math.min(Formulas.MAX_PCRIT_CAP, Math.max(40, (int) Math.round(rawCrit)));
		boolean crit = ThreadLocalRandom.current().nextInt(1000) < critRate;

		// Formula base Interlude: (pAtk * 70.0) / pDef
		double baseDam = (pAtk * 70.0 * calcNpcTypeAtkBonus(attacker, target) * calcElementalModifier(attacker, null, target, null)) / pDef;
		if (crit) {
			double cAtk = PlayerStats.applyStat(attacker, target, "cAtk", 1.0);
			if (isUsingBowWeapon(attacker)) {
				cAtk *= PlayerStats.applyStat(attacker, target, "bowCritRate", 1.0);
			}
			double cAtkAdd = PlayerStats.applyStat(attacker, target, "cAtkAdd", 0.0);
			baseDam = Formulas.calcCritDamageWithAdd(baseDam, cAtk, cAtkAdd, pDef);
		}
		if (Config.ALT_GAME_TIREDNESS && attacker != null && attacker.maxCp() > 0) {
			double tirednessMod = (1000.0 - Math.max(0.0, attacker.maxCp() - attacker.currentCp()) * 0.1) / 1000.0;
			baseDam *= Math.max(0.1, tirednessMod);
		}

		double rnd = calcRndMultiplier(attacker);
		double bowMult = calcBowDistanceMultiplier(attacker, target.x(), target.y());
		int damage = Math.max(1, (int) Math.round(baseDam * rnd * bowMult));
		int flags = crit ? 0x20 : 0x00;
		if (soulshot) {
			flags |= 0x10 | soulshotGrade;
		}

		return new HitPlan(damage, flags, false, crit);
	}

	/**
	 * Calcula a resistencia/vulnerabilidade de arma fisica (ex.: Deflect Arrow -> bowWpnVuln,
	 * Dance of Alignment / Song of Invocation -> bluntWpnVuln, daggerWpnVuln, swordWpnVuln, dualWpnVuln, poleWpnVuln).
	 */
	public static double calcWeaponVulnerability(PlayerCharacter attacker, PlayerCharacter target) {
		if (attacker == null || target == null || attacker.inventory() == null) {
			return 1.0;
		}
		var w = attacker.inventory().paperdoll(ItemSlots.RHAND);
		if (w == null) {
			w = attacker.inventory().paperdoll(ItemSlots.LRHAND);
		}
		if (w == null || w.template() == null || w.template().subType() == null) {
			return PlayerStats.applyStat(target, "fistWpnVuln", 1.0);
		}
		String sub = w.template().subType().toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "");
		String vulnStat = switch (sub) {
			case "bow", "crossbow" -> "bowWpnVuln";
			case "dagger", "dualdagger" -> "daggerWpnVuln";
			case "blunt", "bigblunt", "twohandblunt" -> "bluntWpnVuln";
			case "sword", "bigsword", "twohandsword", "rapier", "ancientsword" -> "swordWpnVuln";
			case "dual", "dualsword" -> "dualWpnVuln";
			case "pole", "polearm", "spear" -> "poleWpnVuln";
			case "fist", "dualfist" -> "fistWpnVuln";
			default -> null;
		};
		return vulnStat != null ? PlayerStats.applyStat(target, vulnStat, 1.0) : 1.0;
	}

	/**
	 * Calcula a vulnerabilidade/defesa de conjuntos de cla (Clan Oath / Apella / Grudge) em PvP.
	 */
	public static double calcClanArmorVulnerability(PlayerCharacter attacker, PlayerCharacter target, boolean isMagic, boolean isSkill) {
		if (target == null) {
			return 1.0;
		}
		double defMod = 1.0;
		if (isMagic) {
			defMod = PlayerStats.applyStat(target, "pvpMagicalDef", 1.0);
		} else if (isSkill) {
			defMod = PlayerStats.applyStat(target, "pvpPhysSkillsDef", 1.0);
		} else {
			defMod = PlayerStats.applyStat(target, "pvpPhysDef", 1.0);
		}
		return defMod > 0 ? (1.0 / defMod) : 1.0;
	}

	/**
	 * Calcula se o alvo bloqueia o golpe com escudo (com arco frontal de 120 graus ou 360 graus Aegis,
	 * ignoreShld, 3x contra arco e bloqueio perfeito).
	 */
	public static int calcShieldDefense(PlayerCharacter target, CharTemplate targetTemplate, boolean attackerUsingBow) {
		return Formulas.calcShldUse(0, 0, target, targetTemplate, attackerUsingBow, false);
	}

	public static int calcShieldDefense(int attackerX, int attackerY, PlayerCharacter target,
			CharTemplate targetTemplate, boolean attackerUsingBow, boolean ignoreShld) {
		return Formulas.calcShldUse(attackerX, attackerY, target, targetTemplate, attackerUsingBow, ignoreShld);
	}

	/**
	 * Calcula multiplicador de dano contra tipos raciais de monstros (Holy Weapon, Dance of Light,
	 * Detect Monster/Animal/Plant/Dragon/Insect/Giant Weakness, Valakas Attribute) (#138).
	 */
	public static double calcNpcTypeAtkBonus(PlayerCharacter attacker, NpcInstance target) {
		if (attacker == null || target == null || target.template() == null) {
			return 1.0;
		}
		double mult = 1.0;
		if (target.template().isUndead()) {
			mult *= PlayerStats.applyStat(attacker, "pAtk-undead", 1.0);
			double holyAtk = PlayerStats.applyStat(attacker, "holyPower", 0.0);
			if (holyAtk > 0) {
				mult *= (1.0 + Math.min(0.50, holyAtk / 100.0));
			}
		}
		if (target.npcId() == 29028) {
			mult *= PlayerStats.applyStat(attacker, "valakas", 1.0);
		}
		mult *= PlayerStats.applyStat(attacker, "pAtk-monsters", 1.0);
		mult *= PlayerStats.applyStat(attacker, "pAtk-animals", 1.0);
		mult *= PlayerStats.applyStat(attacker, "pAtk-plants", 1.0);
		mult *= PlayerStats.applyStat(attacker, "pAtk-insects", 1.0);
		mult *= PlayerStats.applyStat(attacker, "pAtk-dragons", 1.0);
		mult *= PlayerStats.applyStat(attacker, "pAtk-reptiles", 1.0);
		mult *= PlayerStats.applyStat(attacker, "pAtk-giants", 1.0);
		mult *= PlayerStats.applyStat(attacker, "pAtk-magicCreature", 1.0);
		return mult;
	}

	/**
	 * Calcula o multiplicador de defesa racial do jogador contra o monstro (PDefAnimals, PDefMonsters, ValakasRes, etc.) (#138).
	 */
	public static double calcNpcTypeDefBonus(PlayerCharacter defender, NpcInstance attackerNpc) {
		if (defender == null || attackerNpc == null || attackerNpc.template() == null) {
			return 1.0;
		}
		double mult = 1.0;
		if (attackerNpc.template().isUndead()) {
			mult *= PlayerStats.applyStat(defender, "pDef-undead", 1.0);
		}
		if (attackerNpc.npcId() == 29028) {
			mult *= PlayerStats.applyStat(defender, "valakasRes", 1.0);
		}
		mult *= PlayerStats.applyStat(defender, "pDef-monsters", 1.0);
		mult *= PlayerStats.applyStat(defender, "pDef-animals", 1.0);
		mult *= PlayerStats.applyStat(defender, "pDef-plants", 1.0);
		mult *= PlayerStats.applyStat(defender, "pDef-insects", 1.0);
		mult *= PlayerStats.applyStat(defender, "pDef-dragons", 1.0);
		mult *= PlayerStats.applyStat(defender, "pDef-reptiles", 1.0);
		mult *= PlayerStats.applyStat(defender, "pDef-giants", 1.0);
		mult *= PlayerStats.applyStat(defender, "pDef-magicCreature", 1.0);
		return Math.max(0.1, mult);
	}

	/**
	 * Calcula o multiplicador elemental pareando estritamente o elemento da skill/ataque com a vulnerabilidade
	 * correspondente do alvo (#121).
	 */
	public static double calcElementalModifier(PlayerCharacter attacker, PlayerCharacter targetPlayer, NpcInstance targetNpc) {
		return calcElementalModifier(attacker, targetPlayer, targetNpc, null);
	}

	public static double calcElementalModifier(PlayerCharacter attacker, PlayerCharacter targetPlayer,
			NpcInstance targetNpc, String element) {
		String normElem = normalizeElementName(element);
		if (normElem != null) {
			String powerStat = normElem + "Power";
			String vulnStat = normElem + "Vuln";
			double mod = 1.0;
			if (attacker != null) {
				double pwr = Math.max(0.0, PlayerStats.applyStat(attacker, powerStat, 0.0));
				if (pwr > 0) {
					mod *= (1.0 + Math.min(0.50, pwr / 100.0));
				}
			}
			if (targetPlayer != null) {
				double vuln = PlayerStats.applyStat(targetPlayer, vulnStat, 1.0);
				mod *= Math.max(0.20, Math.min(2.0, vuln));
			}
			return mod;
		}

		// Sem elemento explicito de skill (ex: ataque fisico ou chamada generica):
		// verifica quais elementos de ataque estao ativos no atacante e aplica apenas a vulnerabilidade correspondente
		if (attacker == null) {
			return 1.0;
		}
		String[] elements = { "fire", "water", "wind", "earth", "holy", "dark" };
		double totalMod = 1.0;
		for (String el : elements) {
			double pwr = Math.max(0.0, PlayerStats.applyStat(attacker, el + "Power", 0.0));
			if (pwr > 0) {
				double elMod = 1.0 + Math.min(0.50, pwr / 100.0);
				if (targetPlayer != null) {
					double vuln = PlayerStats.applyStat(targetPlayer, el + "Vuln", 1.0);
					elMod *= Math.max(0.20, Math.min(2.0, vuln));
				}
				totalMod *= elMod;
			}
		}
		return totalMod;
	}

	private static String normalizeElementName(String element) {
		if (element == null || element.isBlank()) {
			return null;
		}
		return switch (element.trim().toLowerCase(Locale.ROOT)) {
			case "1", "wind" -> "wind";
			case "2", "fire" -> "fire";
			case "3", "water" -> "water";
			case "4", "earth" -> "earth";
			case "5", "holy", "sacred" -> "holy";
			case "6", "dark", "unholy" -> "dark";
			default -> null;
		};
	}

	/**
	 * Aplica todas as vulnerabilidades de debuff do alvo (debuffVuln + vulnerabilidade especifica do efeito,
	 * como stunVuln, sleepVuln, rootVuln, paralyzeVuln, derangementVuln, confusionVuln, poisonVuln, bleedVuln).
	 */
	public static double calcDebuffVulnerability(PlayerCharacter target, String effectOrSkillType) {
		if (target == null) {
			return 1.0;
		}
		double vuln = PlayerStats.applyStat(target, "debuffVuln", 1.0);
		if (effectOrSkillType != null && !effectOrSkillType.isBlank()) {
			String k = effectOrSkillType.trim().toLowerCase(Locale.ROOT);
			String specificStat = switch (k) {
				case "stun" -> "stunVuln";
				case "sleep" -> "sleepVuln";
				case "root" -> "rootVuln";
				case "paralyze", "petrification", "medusa" -> "paralyzeVuln";
				case "mute", "silence", "physicalmute", "silencemagicphysical", "fear", "betray", "derangement" -> "derangementVuln";
				case "confusion", "confuse", "confuse_mob_only", "switch", "aggdebuff", "aggdamage", "aggremove", "aggreduce" -> "confusionVuln";
				case "poison" -> "poisonVuln";
				case "bleed" -> "bleedVuln";
				case "cancel", "warrior_bane", "mage_bane", "steal_buff" -> "cancelVuln";
				default -> null;
			};
			if (specificStat != null) {
				vuln *= PlayerStats.applyStat(target, specificStat, 1.0);
			}
		}
		return Math.max(0.0, Math.min(2.0, vuln));
	}

	/**
	 * Calcula a chance efetiva de um golpe critico magico usando a formula retail (80 * WITBonus, cap 300 = 30%) (#111).
	 */
	public static int calcMagicCritRate(PlayerCharacter attacker, CharTemplate template) {
		int wit = template != null ? template.wit() : 20;
		if (attacker != null && template != null) {
			wit = PlayerStats.calculate(attacker, template).wit();
		}
		return Formulas.calcRetailMagicCritRate(attacker, wit);
	}

	public static boolean isUsingBowWeapon(PlayerCharacter player) {
		if (player == null || player.inventory() == null) {
			return false;
		}
		var w = player.inventory().paperdoll(ItemSlots.RHAND);
		if (w == null) {
			w = player.inventory().paperdoll(ItemSlots.LRHAND);
		}
		return w != null && w.template() != null && "bow".equalsIgnoreCase(w.template().subType());
	}

	public static double getEffectiveNpcPDef(NpcInstance target) {
		if (target == null) {
			return 1.0;
		}
		double pDef = Math.max(1.0, target.pDef());
		boolean isRaid = target.template() != null
				&& (target.template().isRaidBoss() || target.template().isGrandBoss() || target.isRaidMinion());
		if (isRaid && Config.RAID_P_DEFENCE_MULTIPLIER > 0 && Config.RAID_P_DEFENCE_MULTIPLIER != 100) {
			pDef *= Config.RAID_P_DEFENCE_MULTIPLIER / 100.0;
		}
		return Math.max(1.0, pDef);
	}

	public static double getEffectiveNpcMDef(NpcInstance target) {
		if (target == null) {
			return 1.0;
		}
		double mDef = Math.max(1.0, target.mDef());
		boolean isRaid = target.template() != null
				&& (target.template().isRaidBoss() || target.template().isGrandBoss() || target.isRaidMinion());
		if (isRaid && Config.RAID_M_DEFENCE_MULTIPLIER > 0 && Config.RAID_M_DEFENCE_MULTIPLIER != 100) {
			mDef *= Config.RAID_M_DEFENCE_MULTIPLIER / 100.0;
		}
		return Math.max(1.0, mDef);
	}

	public HitPlan planAttackPlayer(PlayerCharacter attacker, CharTemplate attackerTemplate,
			PlayerCharacter target, CharTemplate targetTemplate, int soulshotGrade) {
		if (target.isDead() || target.invul()) {
			return new HitPlan(0, 0, false, false);
		}
		var atkStats = PlayerStats.calculate(attacker, attackerTemplate);
		var tgtStats = PlayerStats.calculate(target, targetTemplate);
		boolean soulshot = soulshotGrade >= 0;
		double pAtk = atkStats.pAtk() * (soulshot ? 2.0 : 1.0);
		double pDef = Math.max(1, tgtStats.pDef());

		RelativePosition pos = attacker != null
				? getRelativePosition(attacker.x(), attacker.y(), target.x(), target.y(), target.heading())
				: RelativePosition.FRONT;

		int acc = atkStats.accuracy();
		int eva = tgtStats.evasion();
		int hitChance = Formulas.calcHitChance(acc, eva, pos);
		boolean miss = ThreadLocalRandom.current().nextInt(100) >= hitChance;
		if (miss) {
			return new HitPlan(0, 0x80, true, false);
		}

		int shldDef = calcShieldDefense(
				attacker != null ? attacker.x() : 0,
				attacker != null ? attacker.y() : 0,
				target, targetTemplate, isUsingBowWeapon(attacker), false);
		if (shldDef == Integer.MAX_VALUE) {
			return new HitPlan(1, soulshot ? (0x10 | soulshotGrade) : 0, false, false);
		} else if (shldDef > 0) {
			pDef += shldDef;
		}

		double posCritMult = Formulas.calcPositionCritMultiplier(pos);
		double rawCrit = PlayerStats.applyStat(attacker, target, "rCrit", atkStats.critical() * posCritMult);
		int critRate = Math.min(Formulas.MAX_PCRIT_CAP, Math.max(40, (int) Math.round(rawCrit)));
		boolean crit = ThreadLocalRandom.current().nextInt(1000) < critRate;
		double baseDam = (pAtk * 70.0) / pDef;
		if (crit) {
			double cAtk = PlayerStats.applyStat(attacker, target, "cAtk", 1.0);
			if (isUsingBowWeapon(attacker)) {
				cAtk *= PlayerStats.applyStat(attacker, target, "bowCritRate", 1.0);
			}
			double cAtkAdd = PlayerStats.applyStat(attacker, target, "cAtkAdd", 0.0);
			double critVuln = PlayerStats.applyStat(target, attacker, "critVuln", 1.0);
			baseDam = Formulas.calcCritDamageWithAdd(baseDam, cAtk, cAtkAdd, pDef) * critVuln;
		}
		if (Config.ALT_GAME_TIREDNESS && attacker != null && attacker.maxCp() > 0) {
			double tirednessMod = (1000.0 - Math.max(0.0, attacker.maxCp() - attacker.currentCp()) * 0.1) / 1000.0;
			baseDam *= Math.max(0.1, tirednessMod);
		}
		double rnd = calcRndMultiplier(attacker);
		double bowMult = calcBowDistanceMultiplier(attacker, target.x(), target.y());
		double pvpPhysDmg = PlayerStats.applyStat(attacker, "pvpPhysDmg", 1.0)
				* calcClanArmorVulnerability(attacker, target, false, false);
		double wpnVuln = calcWeaponVulnerability(attacker, target);
		double elemMod = calcElementalModifier(attacker, target, null, null);
		double finalDam = baseDam * rnd * bowMult * pvpPhysDmg * wpnVuln * elemMod;
		if (attacker != null && target != null && attacker.isOlympiadMode() && target.isOlympiadMode() && olyDamageManager != null) {
			finalDam *= olyDamageManager.getDamageMultiplier(attacker, target);
		}
		int damage = Math.max(1, (int) Math.round(finalDam));
		int flags = crit ? 0x20 : 0x00;
		if (soulshot) {
			flags |= 0x10 | soulshotGrade;
		}
		return new HitPlan(damage, flags, false, crit);
	}

	/**
	 * @param soulshotGrade grade do soulshot carregado (0 = no grade ... 5 = S) ou -1 sem soulshot. Com shot o
	 *                      pAtk dobra (ssBoost do L2J) e o hit leva o flag USESS|grade para o cliente animar.
	 */
	public HitResult attackNpc(PlayerCharacter attacker, CharTemplate template, NpcInstance target,
			int soulshotGrade) {
		if (target.isDead()) {
			return new HitResult(0, 0, true, 0, target.template().maxHp(), 0, 0);
		}
		if (!canSeeTarget(attacker, target)) {
			return new HitResult(0, 0x80, false, (int) target.currentHp(), target.template().maxHp(), 0, 0);
		}
		var plan = planAttackNpc(attacker, template, target, soulshotGrade);
		if (plan.miss()) {
			return new HitResult(0, plan.flags(), false, (int) target.currentHp(), target.template().maxHp(), 0, 0);
		}
		return applyDamage(target, plan.damage(), plan.flags(), false, attacker != null ? attacker.level() : 0);
	}

	/**
	 * Dano de skill fisico (PDAM/BLOW/CHARGEDAM/FATALCOUNTER...):
	 * usa STRBonus para critico de skill fisica (#116), formula retail de BLOW (#119),
	 * CHARGEDAM (1.0 + 0.25 * charges) e suporte a Lethal 1 / Lethal 2 (#120).
	 */
	public HitResult skillPhysicalNpc(PlayerCharacter attacker, CharTemplate template, NpcInstance target,
			double power, boolean soulshot, boolean blow) {
		return skillPhysicalNpc(attacker, template, target, power, soulshot, blow, false, null);
	}

	public HitResult skillPhysicalNpc(PlayerCharacter attacker, CharTemplate template, NpcInstance target,
			double power, boolean soulshot, boolean blow, boolean chargeDam) {
		return skillPhysicalNpc(attacker, template, target, power, soulshot, blow, chargeDam, null);
	}

	public HitResult skillPhysicalNpc(PlayerCharacter attacker, CharTemplate template, NpcInstance target,
			double power, boolean soulshot, boolean blow, boolean chargeDam, SkillTemplate skill) {
		if (target.isDead()) {
			return new HitResult(0, 0, false, 0, target.template().maxHp(), 0, 0);
		}
		if (checkRaidCurse(attacker, target)) {
			return new HitResult(0, 0, false, (int) target.currentHp(), target.template().maxHp(), 0, 0, false);
		}
		if (!canSeeTarget(attacker, target)) {
			return new HitResult(0, 0x80, false, (int) target.currentHp(), target.template().maxHp(), 0, 0, true);
		}
		var stats = PlayerStats.calculate(attacker, template);
		double pDef = getEffectiveNpcPDef(target);
		String elem = skill != null ? skill.element() : null;
		double elemMod = calcNpcTypeAtkBonus(attacker, target) * calcElementalModifier(attacker, null, target, elem);

		double effPower = power;
		if (skill != null && "FATALCOUNTER".equalsIgnoreCase(skill.skillType()) && attacker != null) {
			effPower = Formulas.calcFatalCounterPower(power, attacker.currentHp(), attacker.maxHp());
		}

		boolean crit;
		double dmg;
		if (blow) {
			boolean blowSuccess = calcBlowSuccess(attacker, attacker.x(), attacker.y(), target.x(), target.y(), target.heading());
			if (!blowSuccess) {
				return new HitResult(0, 0x80, false, (int) target.currentHp(), target.template().maxHp(), 0, 0, false);
			}
			crit = true;
			double cAtk = PlayerStats.applyStat(attacker, target, "cAtk", 1.0);
			double cAtkAdd = PlayerStats.applyStat(attacker, target, "cAtkAdd", 0.0);
			double ssBoost = skill != null ? skill.ssBoost() : 0.0;
			dmg = Formulas.calcBlowDamage(stats.pAtk(), effPower, soulshot, ssBoost, cAtk, cAtkAdd, pDef, 1.0, 1.0, elemMod);
		} else {
			double pAtk = stats.pAtk() * (soulshot ? 2.0 : 1.0);
			dmg = (pAtk + effPower) * 70.0 * elemMod / pDef;
			if (chargeDam) {
				dmg *= Formulas.calcChargeDamMultiplier(attacker != null ? attacker.charges() : 0);
			}
			int baseSkillCrit = skill != null ? skill.baseCritRate() : 0;
			crit = baseSkillCrit > 0 && Formulas.calcSkillCrit(baseSkillCrit, stats.str());
			if (crit) {
				double cAtk = PlayerStats.applyStat(attacker, target, "cAtk", 1.0);
				double cAtkAdd = PlayerStats.applyStat(attacker, target, "cAtkAdd", 0.0);
				dmg = Formulas.calcCritDamageWithAdd(dmg, cAtk, cAtkAdd, pDef);
			}
		}

		// Lethal 1 & Lethal 2 check (#120)
		if (skill != null && (skill.lethal1() > 0 || skill.lethal2() > 0)) {
			var lethal = Formulas.calcLethalHit(attacker, target, skill.lethal1(), skill.lethal2(),
					skill.magicLevel(), stats.dex(), isUsingDaggerWeapon(attacker), isUsingBowWeapon(attacker));
			if (lethal.lethal2() && target.currentHp() > 1) {
				dmg = Math.max(dmg, target.currentHp() - 1.0);
			} else if (lethal.lethal1() && target.currentHp() > 2) {
				dmg += target.currentHp() * 0.50;
			}
		}

		dmg *= 0.95 + ThreadLocalRandom.current().nextDouble() * 0.10;
		return applyDamage(target, Math.max(1, (int) Math.round(dmg)), crit ? 0x20 : 0, false, attacker != null ? attacker.level() : 0);
	}

	/**
	 * Dano de skill magico (MDAM/DRAIN...): 91 * sqrt(mAtk) * power / mDef (Formulas.calcMagicDam do L2J).
	 * Com BSS mAtk x4, com SPS mAtk x2; critico magico retail (4x PvE / Config.M_CRIT_RATE).
	 */
	public HitResult skillMagicNpc(PlayerCharacter attacker, CharTemplate template, NpcInstance target,
			double power) {
		return skillMagicNpc(attacker, template, target, power, 0, false, false, null);
	}

	public HitResult skillMagicNpc(PlayerCharacter attacker, CharTemplate template, NpcInstance target,
			double power, boolean sps, boolean bss) {
		return skillMagicNpc(attacker, template, target, power, 0, sps, bss, null);
	}

	public HitResult skillMagicNpc(PlayerCharacter attacker, CharTemplate template, NpcInstance target,
			double power, int magicLevel, boolean sps, boolean bss) {
		return skillMagicNpc(attacker, template, target, power, magicLevel, sps, bss, null);
	}

	public HitResult skillMagicNpc(PlayerCharacter attacker, CharTemplate template, NpcInstance target,
			double power, int magicLevel, boolean sps, boolean bss, String element) {
		if (target.isDead() || target.invul()) {
			return new HitResult(0, 0, false, (int) target.currentHp(), target.template().maxHp(), 0, 0, false);
		}
		if (checkRaidCurse(attacker, target)) {
			return new HitResult(0, 0, false, (int) target.currentHp(), target.template().maxHp(), 0, 0, false);
		}
		if (!canSeeTarget(attacker, target)) {
			return new HitResult(0, 0x80, false, (int) target.currentHp(), target.template().maxHp(), 0, 0, true);
		}
		var stats = PlayerStats.calculate(attacker, template);
		double mAtk = Math.max(1, stats.mAtk());
		double mDef = getEffectiveNpcMDef(target);
		int mCritRate = calcMagicCritRate(attacker, template);
		boolean crit = ThreadLocalRandom.current().nextInt(1000) < mCritRate;
		double dmg = Formulas.calcRetailMagicDamage(mAtk, power, mDef, sps, bss, crit, false, 0)
				* calcElementalModifier(attacker, null, target, element);
		dmg *= 0.95 + ThreadLocalRandom.current().nextDouble() * 0.10;

		// Penalidade de Grade de Arma em magia (95% chance de falha magica quando weaponPenalty > 0) (#185)
		if (PlayerStats.calcWeaponGradePenalty(attacker) > 0 && ThreadLocalRandom.current().nextInt(100) < 95) {
			dmg = 1.0;
		}

		// --- PENALIDADE DE LEVEL DE SKILLS & RESISTENCIA MAGICA (Retail L2 / L2JDream) ---
		int effMagicLvl = Config.USE_LEVEL_PENALTY
				? (magicLevel > 0 ? Math.min(magicLevel, attacker.level()) : attacker.level())
				: (magicLevel > 0 ? magicLevel : attacker.level());
		int targetLvl = target.template().level();
		int lvlDiff = targetLvl - effMagicLvl;
		boolean resisted = false;

		if (Config.MAGIC_FAILURES && lvlDiff > 0) {
			boolean success = calcMagicSuccess(magicLevel, attacker.level(), targetLvl);
			if (!success) {
				resisted = true;
				if (lvlDiff <= 9) {
					dmg /= 2.0;
				} else {
					dmg = 1.0;
				}
			} else if (lvlDiff > 9) {
				double penaltyMod = Math.max(0.05, 1.0 - ((lvlDiff - 9) * 0.10));
				dmg *= penaltyMod;
			}
		}

		return applyDamage(target, Math.max(1, (int) Math.round(dmg)), (crit ? 0x20 : 0) | (resisted ? 0x40 : 0), resisted, attacker != null ? attacker.level() : 0);
	}

	/** Chance (0-100) de um debuff pegar num monstro: usa Formulas.calcSkillSuccess. */
	public boolean debuffLands(double basePower, int magicLevel, int attackerLevel, NpcInstance target) {
		return debuffLands(basePower, magicLevel, attackerLevel, target, false, false);
	}

	public boolean debuffLands(double basePower, int magicLevel, int attackerLevel, NpcInstance target,
			boolean sps, boolean bss) {
		double chance = basePower > 0 ? basePower : 50;
		int lvl = Config.USE_LEVEL_PENALTY
				? (magicLevel > 0 ? Math.min(magicLevel, attackerLevel) : attackerLevel)
				: (magicLevel > 0 ? magicLevel : attackerLevel);
		double lvlDiff = Config.USE_CHAR_LEVEL_MODIFIER ? Math.max(0, target.template().level() - lvl) : 0;
		if (lvlDiff >= 15) {
			return false;
		}
		chance -= lvlDiff * 4.0;
		if (bss) {
			chance = Math.min(99, chance * 2.0);
		} else if (sps) {
			chance = Math.min(95, chance * 1.4);
		}
		if (lvlDiff > 9) {
			chance = Math.max(0, Math.min(10, chance));
		} else {
			chance = Math.max(1, Math.min(99, chance));
		}
		return ThreadLocalRandom.current().nextDouble(100) < chance;
	}

	public HitResult applyDamage(NpcInstance target, int damage, int flags) {
		return applyDamage(target, damage, flags, false, 0);
	}

	public HitResult applyDamage(NpcInstance target, int damage, int flags, boolean resisted) {
		return applyDamage(target, damage, flags, resisted, 0);
	}

	public HitResult applyDamage(NpcInstance target, int damage, int flags, boolean resisted, int attackerLevel) {
		int newHp;
		synchronized (target) {
			if (target.isDead() || target.invul()) {
				return new HitResult(0, 0, target.isDead(), (int) target.currentHp(), (int) target.maxHp(), 0, 0, false);
			}
			newHp = (int) Math.max(0, target.currentHp() - damage);
			target.currentHp(newHp);
			target.onDamaged();
			if (newHp <= 0) {
				target.dead(true);
				target.currentHp(0);
				newHp = 0;
			}
		}
		boolean isDead = newHp <= 0 || target.isDead();
		long baseExp = target.template().exp() > 0 ? target.template().exp() : (long) target.template().level() * 150L + 50L;
		int baseSp = target.template().sp() > 0
				? target.template().sp()
				: (int) Math.min(Integer.MAX_VALUE, Math.max(0L, baseExp / 10));
		if (target.isChampion() && championService != null) {
			baseExp = championService.calculateExp(target, baseExp);
			baseSp = championService.calculateSp(target, baseSp);
		}
		double activeRateXp = Config.RATE_XP > 0 ? Config.RATE_XP : (rateXp > 0 ? rateXp : 1.0);
		double activeRateSp = Config.RATE_SP > 0 ? Config.RATE_SP : (rateSp > 0 ? rateSp : 1.0);
		long exp = isDead ? Math.max(0L, Math.round(baseExp * activeRateXp)) : 0L;
		int sp = isDead ? (int) Math.min(Integer.MAX_VALUE, Math.max(0L, Math.round(baseSp * activeRateSp))) : 0;
		if (isDead && attackerLevel > 0 && target.template() != null) {
			exp = applyExponentXpPenalty(exp, attackerLevel, target.template().level());
			sp = applyExponentSpPenalty(sp, attackerLevel, target.template().level());
		}
		return new HitResult(damage, flags, isDead, isDead ? 0 : newHp, (int) target.maxHp(), exp, sp, resisted);
	}

	public static long applyExponentXpPenalty(long exp, int attackerLevel, int mobLevel) {
		if (Config.ALT_GAME_EXPONENT_XP != 0 && attackerLevel > 0 && mobLevel > 0) {
			int diff = Math.abs(attackerLevel - mobLevel);
			if (diff > Config.ALT_GAME_EXPONENT_XP) {
				double factor = Math.pow(2.0, -((diff - Config.ALT_GAME_EXPONENT_XP) / (double) Config.ALT_GAME_EXPONENT_XP));
				return Math.max(0L, Math.round(exp * factor));
			}
		}
		return Math.max(0L, exp);
	}

	public static int applyExponentSpPenalty(int sp, int attackerLevel, int mobLevel) {
		long safeSp = Math.max(0, sp);
		if (Config.ALT_GAME_EXPONENT_SP != 0 && attackerLevel > 0 && mobLevel > 0) {
			int diff = Math.abs(attackerLevel - mobLevel);
			if (diff > Config.ALT_GAME_EXPONENT_SP) {
				double factor = Math.pow(2.0, -((diff - Config.ALT_GAME_EXPONENT_SP) / (double) Config.ALT_GAME_EXPONENT_SP));
				return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, Math.round(safeSp * factor)));
			}
		}
		return (int) Math.min(Integer.MAX_VALUE, safeSp);
	}

	public HitPlan planAttackPlayerByNpc(NpcInstance attacker, PlayerCharacter target, CharTemplate targetTemplate) {
		if (attacker.isDead() || target.isDead() || target.invul()) {
			return new HitPlan(0, 0, false, false);
		}
		if (zones != null && (zones.isInsidePeace(attacker.x(), attacker.y(), attacker.z())
				|| zones.isInsidePeace(target.x(), target.y(), target.z()))) {
			return new HitPlan(0, 0x80, true, false);
		}
		int npcRadius = attacker.template() != null ? (int) Math.ceil(attacker.template().collisionRadius()) : 10;
		int attackRange = Math.max(40, attacker.template().attackRange()) + npcRadius + 20;
		double dist = Math.hypot(attacker.x() - target.x(), attacker.y() - target.y());
		if (dist > (attackRange + 180)) {
			return new HitPlan(0, 0x80, true, false);
		}
		if (!canSeeTarget(attacker, target)) {
			return new HitPlan(0, 0x80, true, false);
		}

		double pAtk = attacker.pAtk();
		var targetStats = PlayerStats.calculate(target, targetTemplate);
		double pDef = Math.max(1, targetStats.pDef()) * calcNpcTypeDefBonus(target, attacker);

		int hitChance = 80;
		boolean miss = ThreadLocalRandom.current().nextInt(100) >= hitChance;
		if (miss) {
			return new HitPlan(0, 0x80, true, false);
		}

		boolean isBowNpc = attackRange >= 300;
		int shldDef = calcShieldDefense(attacker.x(), attacker.y(), target, targetTemplate, isBowNpc, false);
		if (shldDef == Integer.MAX_VALUE) {
			return new HitPlan(1, 0, false, false);
		} else if (shldDef > 0) {
			pDef += shldDef;
		}
		boolean crit = ThreadLocalRandom.current().nextInt(1000) < 40;
		double baseDam = (pAtk * 70.0) / pDef;
		if (isBowNpc) {
			baseDam *= PlayerStats.applyStat(target, "bowWpnVuln", 1.0);
		}
		if (crit) {
			baseDam *= 2.0 * PlayerStats.applyStat(target, "critVuln", 1.0);
		}

		double rnd = 0.95 + (ThreadLocalRandom.current().nextDouble() * 0.10);
		int damage = Math.max(1, (int) Math.round(baseDam * rnd));
		int flags = crit ? 0x20 : 0x00;

		return new HitPlan(damage, flags, false, crit);
	}

	public HitResult applyDamageToPlayer(NpcInstance attacker, PlayerCharacter target, int damage, int flags) {
		if (target.isDead() || target.invul()) {
			return new HitResult(0, flags, target.isDead(), (int) target.currentHp(), target.maxHp(), 0, 0);
		}
		if (zones != null && (zones.isInsidePeace(attacker.x(), attacker.y(), attacker.z())
				|| zones.isInsidePeace(target.x(), target.y(), target.z()))) {
			if (!(Config.ALT_KARMA_PLAYER_CAN_BE_KILLED_IN_PEACE_ZONE && target.karma() > 0)) {
				return new HitResult(0, 0x80, target.isDead(), (int) target.currentHp(), target.maxHp(), 0, 0);
			}
		}
		var res = applyDamagePlayer(target, damage, false);
		// Reflexao de dano melee contra monstros (PvE reflectDam) (#127)
		if (attacker != null && !attacker.isDead() && res.damage() > 0) {
			int reflected = Formulas.calcMeleeReflectDamage(target, res.damage());
			if (reflected > 0) {
				attacker.applyDamage(reflected);
			}
		}
		return new HitResult(res.damage(), flags, res.isDead(), res.remainingHp(), target.maxHp(), 0, 0);
	}

	public HitResult attackPlayer(NpcInstance attacker, PlayerCharacter target, CharTemplate targetTemplate) {
		if (attacker.isDead() || target.isDead() || target.invul()) {
			return new HitResult(0, 0, target.isDead(), (int) target.currentHp(), target.maxHp(), 0, 0);
		}
		if (zones != null && (zones.isInsidePeace(attacker.x(), attacker.y(), attacker.z())
				|| zones.isInsidePeace(target.x(), target.y(), target.z()))) {
			if (!(Config.ALT_KARMA_PLAYER_CAN_BE_KILLED_IN_PEACE_ZONE && target.karma() > 0)) {
				return new HitResult(0, 0x80, target.isDead(), (int) target.currentHp(), target.maxHp(), 0, 0);
			}
		}
		int npcRadius = attacker.template() != null ? (int) Math.ceil(attacker.template().collisionRadius()) : 10;
		int attackRange = Math.max(40, attacker.template().attackRange()) + npcRadius + 20;
		double dist = Math.hypot(attacker.x() - target.x(), attacker.y() - target.y());
		if (dist > (attackRange + 180)) {
			return new HitResult(0, 0x80, target.isDead(), (int) target.currentHp(), target.maxHp(), 0, 0);
		}
		if (!canSeeTarget(attacker, target)) {
			return new HitResult(0, 0x80, target.isDead(), (int) target.currentHp(), target.maxHp(), 0, 0);
		}

		var plan = planAttackPlayerByNpc(attacker, target, targetTemplate);
		if (plan.miss()) {
			return new HitResult(0, plan.flags(), false, (int) target.currentHp(), target.maxHp(), 0, 0);
		}
		return applyDamageToPlayer(attacker, target, plan.damage(), plan.flags());
	}

	/**
	 * Dano de habilidade de monstro/NPC contra um jogador (habilidade fisica ou magica).
	 */
	public HitResult skillAttackPlayer(NpcInstance attacker, PlayerCharacter target, CharTemplate targetTemplate,
			double power, boolean magic, int magicLevel) {
		if (attacker.isDead() || target.isDead() || target.invul() || power <= 0) {
			return new HitResult(0, 0, target.isDead(), (int) target.currentHp(), target.maxHp(), 0, 0);
		}
		if (zones != null && (zones.isInsidePeace(attacker.x(), attacker.y(), attacker.z())
				|| zones.isInsidePeace(target.x(), target.y(), target.z()))) {
			if (!(Config.ALT_KARMA_PLAYER_CAN_BE_KILLED_IN_PEACE_ZONE && target.karma() > 0)) {
				return new HitResult(0, 0x80, target.isDead(), (int) target.currentHp(), target.maxHp(), 0, 0);
			}
		}
		double dist = Math.hypot(attacker.x() - target.x(), attacker.y() - target.y());
		if (dist > 1500.0) {
			return new HitResult(0, 0x80, target.isDead(), (int) target.currentHp(), target.maxHp(), 0, 0);
		}
		if (!canSeeTarget(attacker, target)) {
			return new HitResult(0, 0x80, target.isDead(), (int) target.currentHp(), target.maxHp(), 0, 0);
		}

		var targetStats = PlayerStats.calculate(target, targetTemplate);
		double baseDam;
		int flags = 0;

		if (magic) {
			double mAtk = Math.max(1, attacker.mAtk());
			double mDef = Math.max(1, targetStats.mDef());
			double pwr = power;
			baseDam = (91.0 * Math.sqrt(mAtk) * pwr / mDef) * calcElementalModifier(null, target, attacker, null);

			boolean crit = ThreadLocalRandom.current().nextInt(1000) < 50;
			if (crit) {
				baseDam *= 3.0;
				flags |= 0x20;
			}

			int targetLvl = target.level();
			int effMagicLvl = magicLevel > 0 ? magicLevel : attacker.template().level();
			int lvlDiff = targetLvl - effMagicLvl;
			if (lvlDiff > 0 && !calcMagicSuccess(effMagicLvl, attacker.template().level(), targetLvl)) {
				baseDam /= 2.0;
				flags |= 0x40;
			}
		} else {
			if (Formulas.calcPhysicalSkillEvasion(target)) {
				return new HitResult(0, 0x80, target.isDead(), (int) target.currentHp(), target.maxHp(), 0, 0);
			}
			double pDef = Math.max(1, targetStats.pDef()) * calcNpcTypeDefBonus(target, attacker);
			int shldDef = calcShieldDefense(attacker.x(), attacker.y(), target, targetTemplate, false, false);
			if (shldDef == Integer.MAX_VALUE) {
				return applyDamageToPlayer(attacker, target, 1, 0);
			} else if (shldDef > 0) {
				pDef += shldDef;
			}
			double pAtk = attacker.pAtk();
			baseDam = ((pAtk + power) * 70.0) / pDef;

			boolean crit = ThreadLocalRandom.current().nextInt(1000) < 40;
			if (crit) {
				baseDam *= 2.0;
				flags |= 0x20;
			}
		}

		double rnd = 0.95 + (ThreadLocalRandom.current().nextDouble() * 0.10);
		int damage = Math.max(1, (int) Math.round(baseDam * rnd));

		return applyDamageToPlayer(attacker, target, damage, flags);
	}

	// ==================== PVP COMBAT & SKILLS ====================

	public record PlayerDamageResult(int damage, int cpDamage, int hpDamage, boolean isDead, int remainingHp,
			int remainingCp) {
	}

	public int skillPhysicalPlayer(PlayerCharacter attacker, CharTemplate attackerTemplate,
			PlayerCharacter target, CharTemplate targetTemplate, double power, boolean soulshot, boolean blow) {
		return skillPhysicalPlayer(attacker, attackerTemplate, target, targetTemplate, power, soulshot, blow, false, null);
	}

	public int skillPhysicalPlayer(PlayerCharacter attacker, CharTemplate attackerTemplate,
			PlayerCharacter target, CharTemplate targetTemplate, double power, boolean soulshot, boolean blow,
			boolean chargeDam) {
		return skillPhysicalPlayer(attacker, attackerTemplate, target, targetTemplate, power, soulshot, blow, chargeDam, null);
	}

	public int skillPhysicalPlayer(PlayerCharacter attacker, CharTemplate attackerTemplate,
			PlayerCharacter target, CharTemplate targetTemplate, double power, boolean soulshot, boolean blow,
			boolean chargeDam, SkillTemplate skill) {
		if (target.isDead() || target.invul() || !canSeeTarget(attacker, target)) {
			return 0;
		}
		// Evasao de skill fisica (pSkillEvas - Dodge / Mirage) (#125)
		if (Formulas.calcPhysicalSkillEvasion(target)) {
			return 0;
		}
		var attackerStats = PlayerStats.calculate(attacker, attackerTemplate);
		var targetStats = PlayerStats.calculate(target, targetTemplate);
		double pDef = Math.max(1, targetStats.pDef());
		boolean ignoreShld = skill != null && skill.ignoreShld();
		int shldDef = calcShieldDefense(
				attacker != null ? attacker.x() : 0,
				attacker != null ? attacker.y() : 0,
				target, targetTemplate, isUsingBowWeapon(attacker), ignoreShld);
		if (shldDef == Integer.MAX_VALUE) {
			return 1;
		} else if (shldDef > 0) {
			pDef += shldDef;
		}

		String elem = skill != null ? skill.element() : null;
		double wpnVuln = calcWeaponVulnerability(attacker, target);
		double elemMod = calcElementalModifier(attacker, target, null, elem);
		double effPower = power;
		if (skill != null && "FATALCOUNTER".equalsIgnoreCase(skill.skillType()) && attacker != null) {
			effPower = Formulas.calcFatalCounterPower(power, attacker.currentHp(), attacker.maxHp());
		}

		double dmg;
		if (blow) {
			boolean blowSuccess = calcBlowSuccess(attacker, attacker.x(), attacker.y(), target.x(), target.y(), target.heading());
			if (!blowSuccess) {
				return 0;
			}
			double cAtk = PlayerStats.applyStat(attacker, target, "cAtk", 1.0);
			double cAtkAdd = PlayerStats.applyStat(attacker, target, "cAtkAdd", 0.0);
			double critVuln = PlayerStats.applyStat(target, attacker, "critVuln", 1.0);
			double ssBoost = skill != null ? skill.ssBoost() : 0.0;
			dmg = Formulas.calcBlowDamage(attackerStats.pAtk(), effPower, soulshot, ssBoost, cAtk, cAtkAdd, pDef, critVuln, wpnVuln, elemMod);
		} else {
			double pAtk = attackerStats.pAtk() * (soulshot ? 2.0 : 1.0);
			dmg = ((pAtk + effPower) * 70.0 / pDef) * wpnVuln * elemMod;
			if (chargeDam) {
				dmg *= Formulas.calcChargeDamMultiplier(attacker != null ? attacker.charges() : 0);
			}
			int baseSkillCrit = skill != null ? skill.baseCritRate() : 0;
			boolean crit = baseSkillCrit > 0 && Formulas.calcSkillCrit(baseSkillCrit, attackerStats.str());
			if (crit) {
				double cAtk = PlayerStats.applyStat(attacker, target, "cAtk", 1.0);
				double cAtkAdd = PlayerStats.applyStat(attacker, target, "cAtkAdd", 0.0);
				double critVuln = PlayerStats.applyStat(target, attacker, "critVuln", 1.0);
				dmg = Formulas.calcCritDamageWithAdd(dmg, cAtk, cAtkAdd, pDef) * critVuln;
			}
		}

		// Lethal 1 & Lethal 2 check in PvP (#120)
		if (skill != null && (skill.lethal1() > 0 || skill.lethal2() > 0)) {
			var lethal = Formulas.calcLethalHit(attacker, target, skill.lethal1(), skill.lethal2(),
					skill.magicLevel(), attackerStats.dex(), isUsingDaggerWeapon(attacker), isUsingBowWeapon(attacker));
			if (lethal.lethal2()) {
				target.currentCp(1.0);
				target.currentHp(1.0);
			} else if (lethal.lethal1()) {
				target.currentCp(1.0);
			}
		}

		double pvpPhysSkillsDmg = PlayerStats.applyStat(attacker, "pvpPhysSkillsDmg", 1.0)
				* calcClanArmorVulnerability(attacker, target, false, true);
		dmg *= pvpPhysSkillsDmg;
		dmg *= 0.95 + ThreadLocalRandom.current().nextDouble() * 0.10;
		if (attacker != null && target != null && attacker.isOlympiadMode() && target.isOlympiadMode() && olyDamageManager != null) {
			dmg *= olyDamageManager.getDamageMultiplier(attacker, target);
		}
		return Math.max(1, (int) Math.round(dmg));
	}

	public int skillMagicPlayer(PlayerCharacter attacker, CharTemplate attackerTemplate,
			PlayerCharacter target, CharTemplate targetTemplate, double power, boolean sps, boolean bss) {
		return skillMagicPlayer(attacker, attackerTemplate, target, targetTemplate, power, 0, sps, bss, null);
	}

	public int skillMagicPlayer(PlayerCharacter attacker, CharTemplate attackerTemplate,
			PlayerCharacter target, CharTemplate targetTemplate, double power, int magicLevel, boolean sps, boolean bss) {
		return skillMagicPlayer(attacker, attackerTemplate, target, targetTemplate, power, magicLevel, sps, bss, null);
	}

	public int skillMagicPlayer(PlayerCharacter attacker, CharTemplate attackerTemplate,
			PlayerCharacter target, CharTemplate targetTemplate, double power, int magicLevel,
			boolean sps, boolean bss, String element) {
		if (target.isDead() || target.invul() || !canSeeTarget(attacker, target)) {
			return 0;
		}
		var attackerStats = PlayerStats.calculate(attacker, attackerTemplate);
		var targetStats = PlayerStats.calculate(target, targetTemplate);
		double mAtk = Math.max(1, attackerStats.mAtk());
		double mDef = Math.max(1, targetStats.mDef());

		int shldDef = Config.ALT_SHIELD_BLOCKS
				? calcShieldDefense(
						attacker != null ? attacker.x() : 0,
						attacker != null ? attacker.y() : 0,
						target, targetTemplate, false, false)
				: 0;
		int mCritRate = calcMagicCritRate(attacker, attackerTemplate);
		boolean crit = ThreadLocalRandom.current().nextInt(1000) < mCritRate;
		double dmg = Formulas.calcRetailMagicDamage(mAtk, power, mDef, sps, bss, crit, true, shldDef)
				* calcElementalModifier(attacker, target, null, element);

		double pvpMagicalDmg = PlayerStats.applyStat(attacker, "pvpMagicalDmg", 1.0)
				* calcClanArmorVulnerability(attacker, target, true, true);
		dmg *= pvpMagicalDmg;
		dmg *= 0.95 + ThreadLocalRandom.current().nextDouble() * 0.10;

		// Penalidade de Grade de Arma em magia (95% chance de falha magica) (#185)
		if (PlayerStats.calcWeaponGradePenalty(attacker) > 0 && ThreadLocalRandom.current().nextInt(100) < 95) {
			dmg = 1.0;
		}

		// --- PENALIDADE DE LEVEL DE SKILLS & RESISTENCIA MAGICA (PvP) ---
		int effMagicLvl = Config.USE_LEVEL_PENALTY
				? (magicLevel > 0 ? Math.min(magicLevel, attacker.level()) : attacker.level())
				: (magicLevel > 0 ? magicLevel : attacker.level());
		int lvlDiff = target.level() - effMagicLvl;
		if (Config.MAGIC_FAILURES && lvlDiff > 0) {
			boolean success = calcMagicSuccess(magicLevel, attacker.level(), target.level());
			if (!success) {
				if (lvlDiff <= 9) {
					dmg /= 2.0;
				} else {
					dmg = 1.0;
				}
			} else if (lvlDiff > 9) {
				double penaltyMod = Math.max(0.05, 1.0 - ((lvlDiff - 9) * 0.10));
				dmg *= penaltyMod;
			}
		}

		if (attacker != null && target != null && attacker.isOlympiadMode() && target.isOlympiadMode() && olyDamageManager != null) {
			dmg *= olyDamageManager.getDamageMultiplier(attacker, target);
		}
		return Math.max(1, (int) Math.round(dmg));
	}

	public int skillManaDamNpc(PlayerCharacter attacker, CharTemplate attackerTemplate,
			NpcInstance target, double power, int magicLevel, boolean sps, boolean bss) {
		if (target.isDead() || target.invul()) {
			return 0;
		}
		var attackerStats = PlayerStats.calculate(attacker, attackerTemplate);
		double mAtk = Math.max(1, attackerStats.mAtk());
		double mDef = getEffectiveNpcMDef(target);
		double targetMp = Math.min(3500.0, Math.max(1.0, target.template().maxMp()));
		boolean crit = ThreadLocalRandom.current().nextInt(1000) < calcMagicCritRate(attacker, attackerTemplate);
		double dmg = Formulas.calcManaDam(mAtk, power, targetMp, mDef, sps, bss, crit);

		int effMagicLvl = Config.USE_LEVEL_PENALTY
				? (magicLevel > 0 ? Math.min(magicLevel, attacker.level()) : attacker.level())
				: (magicLevel > 0 ? magicLevel : attacker.level());
		int lvlDiff = target.template().level() - effMagicLvl;
		if (Config.MAGIC_FAILURES && lvlDiff > 0) {
			boolean success = calcMagicSuccess(magicLevel, attacker.level(), target.template().level());
			if (!success) {
				if (lvlDiff <= 9) {
					dmg /= 2.0;
				} else {
					dmg = 1.0;
				}
			} else if (lvlDiff > 9) {
				double penaltyMod = Math.max(0.05, 1.0 - ((lvlDiff - 9) * 0.10));
				dmg *= penaltyMod;
			}
		}

		return Math.max(1, (int) Math.round(dmg));
	}

	public int skillManaDamPlayer(PlayerCharacter attacker, CharTemplate attackerTemplate,
			PlayerCharacter target, CharTemplate targetTemplate, double power, int magicLevel, boolean sps, boolean bss) {
		if (target.isDead() || target.invul()) {
			return 0;
		}
		var attackerStats = PlayerStats.calculate(attacker, attackerTemplate);
		var targetStats = PlayerStats.calculate(target, targetTemplate);
		double mAtk = Math.max(1, attackerStats.mAtk());
		double mDef = Math.max(1, targetStats.mDef());
		double targetMp = Math.max(1, target.maxMp());
		boolean crit = ThreadLocalRandom.current().nextInt(1000) < calcMagicCritRate(attacker, attackerTemplate);
		double dmg = Formulas.calcManaDam(mAtk, power, targetMp, mDef, sps, bss, crit);

		int effMagicLvl = Config.USE_LEVEL_PENALTY
				? (magicLevel > 0 ? Math.min(magicLevel, attacker.level()) : attacker.level())
				: (magicLevel > 0 ? magicLevel : attacker.level());
		int lvlDiff = target.level() - effMagicLvl;
		if (Config.MAGIC_FAILURES && lvlDiff > 0) {
			boolean success = calcMagicSuccess(magicLevel, attacker.level(), target.level());
			if (!success) {
				if (lvlDiff <= 9) {
					dmg /= 2.0;
				} else {
					dmg = 1.0;
				}
			} else if (lvlDiff > 9) {
				double penaltyMod = Math.max(0.05, 1.0 - ((lvlDiff - 9) * 0.10));
				dmg *= penaltyMod;
			}
		}

		if (attacker != null && target != null && attacker.isOlympiadMode() && target.isOlympiadMode() && olyDamageManager != null) {
			dmg *= olyDamageManager.getDamageMultiplier(attacker, target);
		}
		return Math.max(1, (int) Math.round(dmg));
	}

	public int skillManaDamNpcToPlayer(NpcInstance attacker, PlayerCharacter target, CharTemplate targetTemplate,
			double power, int magicLevel) {
		if (target.isDead() || target.invul() || power <= 0) {
			return 0;
		}
		var targetStats = PlayerStats.calculate(target, targetTemplate);
		double mAtk = Math.max(1, attacker.mAtk());
		double mDef = Math.max(1, targetStats.mDef());
		double targetMp = Math.max(1, target.maxMp());
		boolean crit = ThreadLocalRandom.current().nextInt(100) < 5;
		double dmg = Formulas.calcManaDam(mAtk, power, targetMp, mDef, false, false, crit);

		int effMagicLvl = magicLevel > 0 ? magicLevel : attacker.template().level();
		int lvlDiff = target.level() - effMagicLvl;
		if (Config.MAGIC_FAILURES && lvlDiff > 0) {
			boolean success = calcMagicSuccess(magicLevel, attacker.template().level(), target.level());
			if (!success) {
				if (lvlDiff <= 9) {
					dmg /= 2.0;
				} else {
					dmg = 1.0;
				}
			} else if (lvlDiff > 9) {
				double penaltyMod = Math.max(0.05, 1.0 - ((lvlDiff - 9) * 0.10));
				dmg *= penaltyMod;
			}
		}

		return Math.max(1, (int) Math.round(dmg));
	}

	public boolean debuffLandsPlayer(double basePower, int magicLevel, int attackerLevel, PlayerCharacter target,
			boolean sps, boolean bss) {
		return debuffLandsPlayer(basePower, magicLevel, attackerLevel, target, sps, bss, null);
	}

	public boolean debuffLandsPlayer(double basePower, int magicLevel, int attackerLevel, PlayerCharacter target,
			boolean sps, boolean bss, String effectOrSkillType) {
		double vuln = calcDebuffVulnerability(target, effectOrSkillType);
		if (vuln <= 0.0) {
			return false;
		}
		double chance = (basePower > 0 ? basePower : 50) * vuln;
		int lvl = Config.USE_LEVEL_PENALTY
				? (magicLevel > 0 ? Math.min(magicLevel, attackerLevel) : attackerLevel)
				: (magicLevel > 0 ? magicLevel : attackerLevel);
		double lvlDiff = Config.USE_CHAR_LEVEL_MODIFIER ? Math.max(0, target.level() - lvl) : 0;
		if (lvlDiff >= 15) {
			return false;
		}
		chance -= lvlDiff * 4.0;
		if (bss) {
			chance = Math.min(99, chance * 2.0);
		} else if (sps) {
			chance = Math.min(95, chance * 1.4);
		}
		if (lvlDiff > 9) {
			chance = Math.max(0, Math.min(10, chance));
		} else {
			chance = Math.max(1, Math.min(99, chance));
		}
		return ThreadLocalRandom.current().nextDouble(100) < chance;
	}

	public boolean calcSkillSuccess(PlayerCharacter attacker, Object target, SkillTemplate skill,
			boolean sps, boolean bss, boolean isCubicOrBoss) {
		if (skill == null) {
			return false;
		}
		double power = skill.effectPower() > 0 ? skill.effectPower() : skill.power();
		String type = skill.effectType() != null ? skill.effectType() : skill.skillType();
		return Formulas.calcSkillSuccess(attacker, target, power, skill.magicLevel(),
				skill.lvlDepend(), skill.saveVs(), type, sps, bss, isCubicOrBoss);
	}

	/**
	 * Em PvP no Lineage II, dano consome Combat Points (CP) antes de atingir o HP.
	 * Em PvE (monstros/NPCs), o dano vai diretamente para o HP sem tocar no CP.
	 */
	public PlayerDamageResult applyDamagePlayer(PlayerCharacter target, int damage) {
		return applyDamagePlayer(target, damage, true);
	}

	public PlayerDamageResult applyDamagePlayer(PlayerCharacter target, int damage, boolean isPvP) {
		if (target.isDead() || target.invul()) {
			return new PlayerDamageResult(0, 0, 0, target.isDead(), (int) target.currentHp(), (int) target.currentCp());
		}
		double curHp = target.currentHp();
		double curCp = target.currentCp();
		int cpDamage = 0;
		int hpDamage = damage;
		if (isPvP) {
			if (curCp >= damage) {
				cpDamage = damage;
				hpDamage = 0;
				target.currentCp(curCp - damage);
			} else {
				cpDamage = (int) curCp;
				target.currentCp(0.0);
				int rem = damage - cpDamage;
				hpDamage = rem;
				target.currentHp(curHp - rem);
			}
		} else {
			target.currentHp(curHp - damage);
		}
		target.onDamaged();
		boolean isDead = target.isDead();
		return new PlayerDamageResult(damage, cpDamage, hpDamage, isDead, (int) target.currentHp(),
				(int) target.currentCp());
	}

	private static double calcRndMultiplier(PlayerCharacter attacker) {
		int rndDam = 5;
		if (attacker != null && attacker.inventory() != null) {
			var w = attacker.inventory().paperdoll(ItemSlots.RHAND);
			if (w == null) {
				w = attacker.inventory().paperdoll(ItemSlots.LRHAND);
			}
			if (w != null && w.template() != null && w.template().rndDam() > 0) {
				rndDam = w.template().rndDam();
			}
		}
		double spread = rndDam / 100.0;
		return (1.0 - spread) + (ThreadLocalRandom.current().nextDouble() * 2.0 * spread);
	}

	public static double calcBowDistanceMultiplier(PlayerCharacter attacker, int tx, int ty) {
		if (!Config.USE_BOW_DISTANCE_PENALTY || attacker == null || attacker.inventory() == null) {
			return 1.0;
		}
		var w = attacker.inventory().paperdoll(ItemSlots.LRHAND);
		if (w == null) {
			w = attacker.inventory().paperdoll(ItemSlots.RHAND);
		}
		if (w == null || w.template() == null || !"bow".equalsIgnoreCase(w.template().subType())) {
			return 1.0;
		}

		double dx = attacker.x() - tx;
		double dy = attacker.y() - ty;
		double dist = Math.sqrt(dx * dx + dy * dy);
		double maxRange = 850.0;
		if (dist >= maxRange) {
			return 1.0;
		}
		float minMult = Config.MAX_BOW_DISTANCE_PENALTY;
		return minMult + (1.0 - minMult) * (dist / maxRange);
	}

	public HitResult summonAttackNpc(com.lopez.l2j.game.summon.SummonInstance summon, NpcInstance target) {
		if (summon == null || target == null || summon.isDead() || target.isDead()) {
			return new HitResult(0, 0x80, target != null && target.isDead(), target != null ? (int) target.currentHp() : 0,
					target != null ? (int) target.maxHp() : 1, 0L, 0);
		}
		int pAtk = summon.pAtk();
		if (summon.soulshotCharged()) {
			pAtk *= 2;
		}
		int pDef = Math.max(1, (int) Math.round(getEffectiveNpcPDef(target)));
		boolean crit = ThreadLocalRandom.current().nextInt(1000) < summon.critical() * 10;
		double raw = (70.0 * pAtk) / pDef;
		if (crit) {
			raw *= 2.0;
		}
		raw *= 0.90 + ThreadLocalRandom.current().nextDouble() * 0.20;
		int damage = Math.max(1, (int) Math.round(raw));
		int flags = 0;
		if (summon.soulshotCharged()) {
			flags |= 0x10;
			summon.soulshotCharged(false);
		}
		if (crit) {
			flags |= 0x20;
		}
		boolean died = target.applyDamage(damage);
		summon.markCombat();
		long expReward = 0L;
		int spReward = 0;
		if (died && target.template() != null) {
			double activeRateXp = Config.RATE_XP > 0 ? Config.RATE_XP : (rateXp > 0 ? rateXp : 1.0);
			double activeRateSp = Config.RATE_SP > 0 ? Config.RATE_SP : (rateSp > 0 ? rateSp : 1.0);
			expReward = Math.max(0L, Math.round(target.template().exp() * activeRateXp));
			spReward = (int) Math.min(Integer.MAX_VALUE, Math.max(0L, Math.round(target.template().sp() * activeRateSp)));
			expReward = applyExponentXpPenalty(expReward, summon.level(), target.template().level());
			spReward = applyExponentSpPenalty(spReward, summon.level(), target.template().level());
		}
		return new HitResult(damage, flags, died, (int) target.currentHp(), (int) target.maxHp(), expReward, spReward);
	}

	public PlayerDamageResult summonAttackPlayer(com.lopez.l2j.game.summon.SummonInstance summon, PlayerCharacter target, CharTemplate targetTpl) {
		if (summon == null || target == null || summon.isDead() || target.isDead()) {
			return new PlayerDamageResult(0, 0, 0, target != null && target.isDead(),
					target != null ? (int) target.currentHp() : 0, target != null ? (int) target.currentCp() : 0);
		}
		int pAtk = summon.pAtk();
		if (summon.soulshotCharged()) {
			pAtk *= 2;
			summon.soulshotCharged(false);
		}
		int pDef = Math.max(1, PlayerStats.calculate(target, targetTpl).pDef());
		boolean crit = ThreadLocalRandom.current().nextInt(1000) < summon.critical() * 10;
		double raw = (70.0 * pAtk) / pDef;
		if (crit) {
			raw *= 2.0;
		}
		raw *= 0.90 + ThreadLocalRandom.current().nextDouble() * 0.20;
		int damage = Math.max(1, (int) Math.round(raw));
		summon.markCombat();
		return applyDamagePlayer(target, damage, true);
	}

	public boolean applyDamageCreature(Creature target, int damage, boolean isPvP) {
		if (target == null || target.isDead() || target.invul() || damage <= 0) {
			return target != null && target.isDead();
		}
		if (target instanceof PlayerCharacter pc) {
			return applyDamagePlayer(pc, damage, isPvP).isDead();
		} else if (target instanceof NpcInstance npc) {
			return npc.applyDamage(damage);
		} else if (target instanceof com.lopez.l2j.game.summon.SummonInstance sum) {
			return sum.applyDamage(damage);
		} else {
			target.onDamaged();
			target.currentHp(Math.max(0.0, target.currentHp() - damage));
			return target.isDead();
		}
	}

	// ==================== KARMA, PK, SA, PROTECTION & PLAYER RULES ====================

	public static int calculateKarmaGain(int pkCount, int victimLevel) {
		int minKarma = Math.max(1, Config.MIN_KARMA);
		int maxKarma = Math.max(minKarma, Config.MAX_KARMA);
		float rate = Config.KARMA_RATE > 0 ? Config.KARMA_RATE : 1.0f;
		int raw = (int) Math.round(minKarma * (1.0 + 0.05 * Math.max(0, pkCount)) * rate);
		return Math.min(maxKarma, Math.max(minKarma, raw));
	}

	public static int calculateKarmaLost(long expEarned) {
		int divider = Math.max(1, Config.XP_DIVIDER);
		int lost = (int) (Math.max(0L, expEarned) / divider);
		return Math.max(Math.max(0, Config.BASE_KARMA_LOST), lost);
	}

	public static long calculateDeathExpLost(PlayerCharacter victim, long baseExpLost) {
		if (victim != null && victim.karma() > 0 && Config.RATE_KARMA_EXP_LOST > 0) {
			return Math.round(baseExpLost * Config.RATE_KARMA_EXP_LOST);
		}
		return baseExpLost;
	}

	public static boolean shouldAwardPvpPointForPkKill(PlayerCharacter victim) {
		return victim != null && victim.karma() > 0 && Config.AWARD_PK_KILL_PVP_POINT;
	}

	public static boolean rollSoulCrystalLevelUp() {
		int chance = Math.max(0, Config.CHANCE_TO_LEVEL_SA);
		return chance > 0 && ThreadLocalRandom.current().nextInt(100) < chance;
	}

	public static int calcBufferHate(int baseAggro) {
		int div = Math.max(1, Config.BUFFER_HATE);
		return Math.max(1, baseAggro / div);
	}

	public static boolean shouldConsumeSkillCost(boolean skillSucceeded) {
		return !Config.CONSUME_ON_SUCCESS || skillSucceeded;
	}

	public static long getSpawnProtectionDurationMs() {
		return Math.max(0, Config.PLAYER_SPAWN_PROTECTION) * 1000L;
	}

	public static long getFakeDeathUpProtectionDurationMs() {
		return Math.max(0, Config.PLAYER_FAKE_DEATH_UP_PROTECTION) * 1000L;
	}

	public static boolean rollDeathPenalty(PlayerCharacter player, boolean killedByMob) {
		if (!killedByMob || player == null || player.isGm()) {
			return false;
		}
		int chance = Math.max(0, Config.DEATH_PENALTY_CHANCE);
		return chance > 0 && ThreadLocalRandom.current().nextInt(100) < chance;
	}

	public static boolean canInteractWithNpc(PlayerCharacter player, boolean isCursedWeaponEquipped) {
		if (isCursedWeaponEquipped && !Config.CURSED_WEAPON_NPC_INTERACT) {
			return player != null && player.isGm();
		}
		return true;
	}

	public static boolean isUserMenuAllowed() {
		return Config.ALLOW_USER_MENU;
	}

	public static boolean isExpSetAllowed() {
		return Config.ALLOW_USE_EXP_SET;
	}

	public static boolean isKeyboardMovementAllowed() {
		return Config.ALLOW_KEYBOARD_MOVEMENT;
	}
}
