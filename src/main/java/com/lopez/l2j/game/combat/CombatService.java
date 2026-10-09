package com.lopez.l2j.game.combat;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.PlayerStats;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.template.CharTemplate;
import com.lopez.l2j.config.Config;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Calculo de combate e formulas físicas/mágicas do Lineage II Interlude.
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

	@org.springframework.beans.factory.annotation.Autowired
	public CombatService(
			@Value("${l2.rates.xp:1.0}") double rateXp,
			@Value("${l2.rates.sp:1.0}") double rateSp,
			@org.springframework.beans.factory.annotation.Autowired(required = false) com.lopez.l2j.game.geodata.GeoEngine geoEngine,
			@org.springframework.beans.factory.annotation.Autowired(required = false) com.lopez.l2j.game.olympiad.OlyClassDamageManager olyDamageManager,
			@org.springframework.beans.factory.annotation.Autowired(required = false) com.lopez.l2j.game.champion.ChampionService championService,
			@org.springframework.beans.factory.annotation.Autowired(required = false) com.lopez.l2j.game.zone.ZoneTable zones) {
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
		return true;
	}

	public boolean canSeeTarget(PlayerCharacter player, NpcInstance npc) {
		if (geoEngine != null && geoEngine.isEnabled()) {
			if (player == null || npc == null) {
				return false;
			}
			return geoEngine.canSeeTarget(player, npc);
		}
		return true;
	}

	public boolean canSeeTarget(NpcInstance npc, PlayerCharacter player) {
		if (geoEngine != null && geoEngine.isEnabled()) {
			if (npc == null || player == null) {
				return false;
			}
			return geoEngine.canSeeTarget(npc, player);
		}
		return true;
	}

	public boolean canSeeTarget(PlayerCharacter player, PlayerCharacter target) {
		if (geoEngine != null && geoEngine.isEnabled()) {
			if (player == null || target == null) {
				return false;
			}
			return geoEngine.canSeeTarget(player, target);
		}
		return true;
	}

	public boolean canMoveToTarget(int x, int y, int z, int tx, int ty, int tz) {
		if (geoEngine != null && geoEngine.isEnabled()) {
			return geoEngine.canMoveToTarget(x, y, z, tx, ty, tz);
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

	public static boolean calcBlowSuccess(int attackerX, int attackerY, int targetX, int targetY, int targetHeading) {
		RelativePosition pos = getRelativePosition(attackerX, attackerY, targetX, targetY, targetHeading);
		int chance = getBlowChance(pos);
		return ThreadLocalRandom.current().nextInt(100) < chance;
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
		boolean isRaid = target.template().isRaidBoss() || target.template().isGrandBoss()
				|| (target.isMinion() && target.masterObjectId() != 0);
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
	 * Planeja o golpe contra o monstro, calculando dano, acerto/erro, crítico e soulshot
	 * SEM alterar o HP nem o estado de vida do monstro antes da animação atingir o alvo.
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
		double pDef = Math.max(1, target.pDef());

		// Chance de acerto: precisao vs evasao
		int acc = stats.accuracy();
		int eva = Math.max(20, target.template().level() + 30);
		int diff = acc - eva;
		int hitChance = Math.max(28, Math.min(98, 80 + diff * 2));
		boolean miss = ThreadLocalRandom.current().nextInt(100) >= hitChance;

		if (miss) {
			return new HitPlan(0, 0x80, true, false);
		}

		// Chance de critico (base 40-120 per 1000)
		int critRate = stats.critical();
		boolean crit = ThreadLocalRandom.current().nextInt(1000) < Math.max(40, critRate);

		// Formula base Interlude: (pAtk * 70.0) / pDef
		double baseDam = (pAtk * 70.0) / pDef;
		if (crit) {
			baseDam *= 2.0;
		}

		// Variacao aleatoria baseada no rnd_dam da arma (ex: 5% dagger, 10% sword, 20% blunt)
		double rnd = calcRndMultiplier(attacker);
		double bowMult = calcBowDistanceMultiplier(attacker, target.x(), target.y());
		int damage = Math.max(1, (int) Math.round(baseDam * rnd * bowMult));
		int flags = crit ? 0x20 : 0x00;
		if (soulshot) {
			flags |= 0x10 | soulshotGrade;
		}

		return new HitPlan(damage, flags, false, crit);
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

		int acc = atkStats.accuracy();
		int eva = tgtStats.evasion();
		int diff = acc - eva;
		int hitChance = Math.max(28, Math.min(98, 80 + diff * 2));
		boolean miss = ThreadLocalRandom.current().nextInt(100) >= hitChance;
		if (miss) {
			return new HitPlan(0, 0x80, true, false);
		}

		int critRate = atkStats.critical();
		boolean crit = ThreadLocalRandom.current().nextInt(1000) < Math.max(40, critRate);
		double baseDam = (pAtk * 70.0) / pDef;
		if (crit) {
			baseDam *= 2.0;
		}
		double rnd = calcRndMultiplier(attacker);
		double bowMult = calcBowDistanceMultiplier(attacker, target.x(), target.y());
		double finalDam = baseDam * rnd * bowMult;
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
		return applyDamage(target, plan.damage(), plan.flags());
	}

	/**
	 * Dano de skill fisico (PDAM/BLOW...): (pAtk + power) * 70 / pDef, como Formulas.calcPhysDam do L2J para
	 * skills. Com soulshot o pAtk dobra. Flag 0x20 = critico (so BLOW tem critico garantido-ish).
	 */
	public HitResult skillPhysicalNpc(PlayerCharacter attacker, CharTemplate template, NpcInstance target,
			double power, boolean soulshot, boolean blow) {
		return skillPhysicalNpc(attacker, template, target, power, soulshot, blow, false);
	}

	public HitResult skillPhysicalNpc(PlayerCharacter attacker, CharTemplate template, NpcInstance target,
			double power, boolean soulshot, boolean blow, boolean chargeDam) {
		if (target.isDead()) {
			return new HitResult(0, 0, false, 0, target.template().maxHp(), 0, 0); // ja morto: nada a recompensar
		}
		if (checkRaidCurse(attacker, target)) {
			return new HitResult(0, 0, false, (int) target.currentHp(), target.template().maxHp(), 0, 0, false);
		}
		if (!canSeeTarget(attacker, target)) {
			return new HitResult(0, 0x80, false, (int) target.currentHp(), target.template().maxHp(), 0, 0, true);
		}
		var stats = PlayerStats.calculate(attacker, template);
		double pAtk = stats.pAtk() * (soulshot ? 2.0 : 1.0);
		double pDef = Math.max(1, target.pDef());
		double dmg = (pAtk + power) * 70.0 / pDef;
		if (chargeDam) {
			dmg *= (0.8 + 0.201 * attacker.charges());
		}
		boolean crit;
		if (blow) {
			boolean blowSuccess = calcBlowSuccess(attacker.x(), attacker.y(), target.x(), target.y(), target.heading());
			if (!blowSuccess) {
				return new HitResult(0, 0x80, false, (int) target.currentHp(), target.template().maxHp(), 0, 0, false);
			}
			crit = true;
		} else {
			crit = ThreadLocalRandom.current().nextInt(1000) < Math.max(40, stats.critical()) / 2;
		}
		if (crit) {
			dmg *= 2.0;
		}
		dmg *= 0.95 + ThreadLocalRandom.current().nextDouble() * 0.10;
		return applyDamage(target, Math.max(1, (int) Math.round(dmg)), crit ? 0x20 : 0);
	}

	/**
	 * Dano de skill magico (MDAM/DRAIN...): 91 * sqrt(mAtk) * power / mDef (Formulas.calcMagicDam do L2J).
	 * Com BSS mAtk x4, com SPS mAtk x2; critico magico ~5% com dano x3.
	 */
	public HitResult skillMagicNpc(PlayerCharacter attacker, CharTemplate template, NpcInstance target,
			double power) {
		return skillMagicNpc(attacker, template, target, power, 0, false, false);
	}

	public HitResult skillMagicNpc(PlayerCharacter attacker, CharTemplate template, NpcInstance target,
			double power, boolean sps, boolean bss) {
		return skillMagicNpc(attacker, template, target, power, 0, sps, bss);
	}

	public HitResult skillMagicNpc(PlayerCharacter attacker, CharTemplate template, NpcInstance target,
			double power, int magicLevel, boolean sps, boolean bss) {
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
		if (bss) {
			mAtk *= 4.0;
		} else if (sps) {
			mAtk *= 2.0;
		}
		double mDef = Math.max(1, target.mDef());
		double dmg = 91.0 * Math.sqrt(mAtk) * power / mDef;
		int mCritCap = Config.getInt("AltMCriticalCap", 200);
		int mCritRate = (int) Math.min(mCritCap, Math.max(10, Math.round(template.wit() * 1.5)));
		boolean crit = ThreadLocalRandom.current().nextInt(1000) < mCritRate;
		if (crit) {
			dmg *= Config.M_CRIT_RATE > 0 ? (double) Config.M_CRIT_RATE : 3.0;
		}
		dmg *= 0.95 + ThreadLocalRandom.current().nextDouble() * 0.10;

		// --- PENALIDADE DE LEVEL DE SKILLS & RESISTENCIA MAGICA (Retail L2 / L2JDream) ---
		int effMagicLvl = magicLevel > 0 ? Math.min(magicLevel, attacker.level()) : attacker.level();
		int targetLvl = target.template().level();
		int lvlDiff = targetLvl - effMagicLvl;
		boolean resisted = false;

		if (lvlDiff > 0) {
			boolean success = calcMagicSuccess(magicLevel, attacker.level(), targetLvl);
			if (!success) {
				resisted = true;
				if (lvlDiff <= 9) {
					// Resistencia parcial: metade do dano
					dmg /= 2.0;
				} else {
					// Resistencia total: diferenca superior a 9 niveis (ex: skill inicial lvl 1 vs bicho lvl 80)
					// O alvo resiste com sucesso e sofre apenas 1 de dano
					dmg = 1.0;
				}
			} else if (lvlDiff > 9) {
				// Acerto improvavel: diferenca severa de nivel ainda reduz o impacto
				double penaltyMod = Math.max(0.05, 1.0 - ((lvlDiff - 9) * 0.10));
				dmg *= penaltyMod;
			}
		}

		return applyDamage(target, Math.max(1, (int) Math.round(dmg)), (crit ? 0x20 : 0) | (resisted ? 0x40 : 0), resisted);
	}

	/** Chance (0-100) de um debuff pegar num monstro: base = power do skill, menos a diferenca de nivel. */
	public boolean debuffLands(double basePower, int magicLevel, int attackerLevel, NpcInstance target) {
		return debuffLands(basePower, magicLevel, attackerLevel, target, false, false);
	}

	public boolean debuffLands(double basePower, int magicLevel, int attackerLevel, NpcInstance target,
			boolean sps, boolean bss) {
		double chance = basePower > 0 ? basePower : 50;
		int lvl = magicLevel > 0 ? Math.min(magicLevel, attackerLevel) : attackerLevel;
		double lvlDiff = Math.max(0, target.template().level() - lvl);
		if (lvlDiff >= 15) {
			return false; // Diferenca de 15+ niveis: debuff nunca pega
		}
		chance -= lvlDiff * 4.0;
		if (bss) {
			chance = Math.min(95, chance * 1.5);
		} else if (sps) {
			chance = Math.min(90, chance * 1.25);
		}
		if (lvlDiff > 9) {
			chance = Math.max(0, Math.min(10, chance));
		} else {
			chance = Math.max(5, Math.min(90, chance));
		}
		return ThreadLocalRandom.current().nextDouble(100) < chance;
	}

	public HitResult applyDamage(NpcInstance target, int damage, int flags) {
		return applyDamage(target, damage, flags, false);
	}

	public HitResult applyDamage(NpcInstance target, int damage, int flags, boolean resisted) {
		int newHp;
		synchronized (target) {
			if (target.isDead() || target.invul()) {
				return new HitResult(0, 0, false, (int) target.currentHp(), target.template().maxHp(), 0, 0, false); // ja morto ou invulneravel: nada a recompensar
			}
			newHp = (int) Math.max(0, target.currentHp() - damage);
			target.currentHp(newHp);
			target.onDamaged();
			if (newHp <= 0) {
				target.dead(true);
			}
		}
		boolean isDead = newHp <= 0;
		long baseExp = target.template().exp() > 0 ? target.template().exp() : (long) target.template().level() * 150L + 50L;
		int baseSp = target.template().sp() > 0 ? target.template().sp() : (int) (baseExp / 10);
		if (target.isChampion() && championService != null) {
			baseExp = championService.calculateExp(target, baseExp);
			baseSp = championService.calculateSp(target, baseSp);
		}
		double activeRateXp = Config.RATE_XP > 0 ? Config.RATE_XP : (rateXp > 0 ? rateXp : 1.0);
		double activeRateSp = Config.RATE_SP > 0 ? Config.RATE_SP : (rateSp > 0 ? rateSp : 1.0);
		long exp = isDead ? (long) Math.round(baseExp * activeRateXp) : 0;
		int sp = isDead ? (int) Math.round(baseSp * activeRateSp) : 0;
		return new HitResult(damage, flags, isDead, newHp, (int) target.maxHp(), exp, sp, resisted);
	}

	public HitPlan planAttackPlayerByNpc(NpcInstance attacker, PlayerCharacter target, CharTemplate targetTemplate) {
		if (attacker.isDead() || target.isDead() || target.invul()) {
			return new HitPlan(0, 0, false, false);
		}
		if (zones != null && (zones.isInsidePeace(attacker.x(), attacker.y(), attacker.z())
				|| zones.isInsidePeace(target.x(), target.y(), target.z()))) {
			return new HitPlan(0, 0x80, true, false);
		}
		int attackRange = Math.max(40, attacker.template().attackRange());
		double dist = Math.hypot(attacker.x() - target.x(), attacker.y() - target.y());
		if (dist > (attackRange + 150)) {
			return new HitPlan(0, 0x80, true, false);
		}
		if (!canSeeTarget(attacker, target)) {
			return new HitPlan(0, 0x80, true, false);
		}

		double pAtk = attacker.pAtk();
		var targetStats = PlayerStats.calculate(target, targetTemplate);
		double pDef = Math.max(1, targetStats.pDef());

		int hitChance = 80;
		boolean miss = ThreadLocalRandom.current().nextInt(100) >= hitChance;
		if (miss) {
			return new HitPlan(0, 0x80, true, false);
		}

		boolean crit = ThreadLocalRandom.current().nextInt(1000) < 40;
		double baseDam = (pAtk * 70.0) / pDef;
		if (crit) {
			baseDam *= 2.0;
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
		var res = applyDamagePlayer(target, damage);
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
		// Validacao de alcance: ataque fisico nao pode acertar jogador distante
		int attackRange = Math.max(40, attacker.template().attackRange());
		double dist = Math.hypot(attacker.x() - target.x(), attacker.y() - target.y());
		if (dist > (attackRange + 150)) {
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
		// Validacao de alcance: skill ofensivo de monstro nao pode ultrapassar o limite maximo de combate (1500u)
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
			baseDam = 91.0 * Math.sqrt(mAtk) * pwr / mDef;

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
			double pDef = Math.max(1, targetStats.pDef());
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
		return skillPhysicalPlayer(attacker, attackerTemplate, target, targetTemplate, power, soulshot, blow, false);
	}

	public int skillPhysicalPlayer(PlayerCharacter attacker, CharTemplate attackerTemplate,
			PlayerCharacter target, CharTemplate targetTemplate, double power, boolean soulshot, boolean blow,
			boolean chargeDam) {
		if (target.isDead() || target.invul() || !canSeeTarget(attacker, target)) {
			return 0;
		}
		var attackerStats = PlayerStats.calculate(attacker, attackerTemplate);
		var targetStats = PlayerStats.calculate(target, targetTemplate);
		double pAtk = attackerStats.pAtk() * (soulshot ? 2.0 : 1.0);
		double pDef = Math.max(1, targetStats.pDef());
		double dmg = (pAtk + power) * 70.0 / pDef;
		if (chargeDam) {
			dmg *= (0.8 + 0.201 * attacker.charges());
		}
		boolean crit;
		if (blow) {
			boolean blowSuccess = calcBlowSuccess(attacker.x(), attacker.y(), target.x(), target.y(), target.heading());
			if (!blowSuccess) {
				return 0;
			}
			crit = true;
		} else {
			crit = ThreadLocalRandom.current().nextInt(1000) < Math.max(40, attackerStats.critical()) / 2;
		}
		if (crit) {
			dmg *= 2.0;
		}
		dmg *= 0.95 + ThreadLocalRandom.current().nextDouble() * 0.10;
		if (attacker != null && target != null && attacker.isOlympiadMode() && target.isOlympiadMode() && olyDamageManager != null) {
			dmg *= olyDamageManager.getDamageMultiplier(attacker, target);
		}
		return Math.max(1, (int) Math.round(dmg));
	}

	public int skillMagicPlayer(PlayerCharacter attacker, CharTemplate attackerTemplate,
			PlayerCharacter target, CharTemplate targetTemplate, double power, boolean sps, boolean bss) {
		return skillMagicPlayer(attacker, attackerTemplate, target, targetTemplate, power, 0, sps, bss);
	}

	public int skillMagicPlayer(PlayerCharacter attacker, CharTemplate attackerTemplate,
			PlayerCharacter target, CharTemplate targetTemplate, double power, int magicLevel, boolean sps, boolean bss) {
		if (target.isDead() || target.invul() || !canSeeTarget(attacker, target)) {
			return 0;
		}
		var attackerStats = PlayerStats.calculate(attacker, attackerTemplate);
		var targetStats = PlayerStats.calculate(target, targetTemplate);
		double mAtk = Math.max(1, attackerStats.mAtk());
		if (bss) {
			mAtk *= 4.0;
		} else if (sps) {
			mAtk *= 2.0;
		}
		double mDef = Math.max(1, targetStats.mDef());
		double dmg = 91.0 * Math.sqrt(mAtk) * power / mDef;
		boolean crit = ThreadLocalRandom.current().nextInt(100) < 5;
		if (crit) {
			dmg *= 3.0;
		}
		dmg *= 0.95 + ThreadLocalRandom.current().nextDouble() * 0.10;

		// --- PENALIDADE DE LEVEL DE SKILLS & RESISTENCIA MAGICA (PvP) ---
		int effMagicLvl = magicLevel > 0 ? Math.min(magicLevel, attacker.level()) : attacker.level();
		int lvlDiff = target.level() - effMagicLvl;
		if (lvlDiff > 0) {
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
		if (bss) {
			mAtk *= 4.0;
		} else if (sps) {
			mAtk *= 2.0;
		}
		double mDef = Math.max(1, target.template().mDef());
		double targetMp = Math.min(3500.0, Math.max(1.0, target.template().maxMp()));
		double dmg = (Math.sqrt(mAtk) * power * (targetMp / 97.0)) / mDef;
		boolean crit = ThreadLocalRandom.current().nextInt(100) < 5;
		if (crit) {
			dmg *= 3.0;
		}
		dmg *= 0.95 + ThreadLocalRandom.current().nextDouble() * 0.10;

		int effMagicLvl = magicLevel > 0 ? Math.min(magicLevel, attacker.level()) : attacker.level();
		int lvlDiff = target.template().level() - effMagicLvl;
		if (lvlDiff > 0) {
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
		if (bss) {
			mAtk *= 4.0;
		} else if (sps) {
			mAtk *= 2.0;
		}
		double mDef = Math.max(1, targetStats.mDef());
		double targetMp = Math.max(1, target.maxMp());
		double dmg = (Math.sqrt(mAtk) * power * (targetMp / 97.0)) / mDef;
		boolean crit = ThreadLocalRandom.current().nextInt(100) < 5;
		if (crit) {
			dmg *= 3.0;
		}
		dmg *= 0.95 + ThreadLocalRandom.current().nextDouble() * 0.10;

		int effMagicLvl = magicLevel > 0 ? Math.min(magicLevel, attacker.level()) : attacker.level();
		int lvlDiff = target.level() - effMagicLvl;
		if (lvlDiff > 0) {
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
		double dmg = (Math.sqrt(mAtk) * power * (targetMp / 97.0)) / mDef;
		boolean crit = ThreadLocalRandom.current().nextInt(100) < 5;
		if (crit) {
			dmg *= 3.0;
		}
		dmg *= 0.95 + ThreadLocalRandom.current().nextDouble() * 0.10;

		int effMagicLvl = magicLevel > 0 ? magicLevel : attacker.template().level();
		int lvlDiff = target.level() - effMagicLvl;
		if (lvlDiff > 0) {
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
		double chance = basePower > 0 ? basePower : 50;
		int lvl = magicLevel > 0 ? Math.min(magicLevel, attackerLevel) : attackerLevel;
		double lvlDiff = Math.max(0, target.level() - lvl);
		if (lvlDiff >= 15) {
			return false; // Diferenca de 15+ niveis: debuff nunca pega
		}
		chance -= lvlDiff * 4.0;
		if (bss) {
			chance = Math.min(95, chance * 1.5);
		} else if (sps) {
			chance = Math.min(90, chance * 1.25);
		}
		if (lvlDiff > 9) {
			chance = Math.max(0, Math.min(10, chance));
		} else {
			chance = Math.max(5, Math.min(90, chance));
		}
		return ThreadLocalRandom.current().nextDouble(100) < chance;
	}

	/**
	 * Em PvP no Lineage II, dano consome Combat Points (CP) antes de atingir o HP.
	 */
	public PlayerDamageResult applyDamagePlayer(PlayerCharacter target, int damage) {
		if (target.isDead() || target.invul()) {
			return new PlayerDamageResult(0, 0, 0, target.isDead(), (int) target.currentHp(), (int) target.currentCp());
		}
		double curCp = target.currentCp();
		double curHp = target.currentHp();
		int cpDamage;
		int hpDamage;
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
		target.onDamaged();
		boolean isDead = target.isDead();
		return new PlayerDamageResult(damage, cpDamage, hpDamage, isDead, (int) target.currentHp(),
				(int) target.currentCp());
	}

	private static double calcRndMultiplier(PlayerCharacter attacker) {
		int rndDam = 5;
		if (attacker != null && attacker.inventory() != null) {
			var w = attacker.inventory().paperdoll(com.lopez.l2j.game.item.ItemSlots.RHAND);
			if (w == null) {
				w = attacker.inventory().paperdoll(com.lopez.l2j.game.item.ItemSlots.LRHAND);
			}
			if (w != null && w.template() != null && w.template().rndDam() > 0) {
				rndDam = w.template().rndDam();
			}
		}
		double spread = rndDam / 100.0;
		return (1.0 - spread) + (ThreadLocalRandom.current().nextDouble() * 2.0 * spread);
	}

	public static double calcBowDistanceMultiplier(PlayerCharacter attacker, int tx, int ty) {
		if (!com.lopez.l2j.config.Config.USE_BOW_DISTANCE_PENALTY || attacker == null || attacker.inventory() == null) {
			return 1.0;
		}
		var w = attacker.inventory().paperdoll(com.lopez.l2j.game.item.ItemSlots.LRHAND);
		if (w == null) {
			w = attacker.inventory().paperdoll(com.lopez.l2j.game.item.ItemSlots.RHAND);
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
		float minMult = com.lopez.l2j.config.Config.MAX_BOW_DISTANCE_PENALTY;
		return minMult + (1.0 - minMult) * (dist / maxRange);
	}
}
