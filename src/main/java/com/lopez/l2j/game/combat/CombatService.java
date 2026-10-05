package com.lopez.l2j.game.combat;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.PlayerStats;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.template.CharTemplate;
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

	public CombatService() {
		this(1.0, 1.0);
	}

	public CombatService(
			@Value("${l2.rates.xp:1.0}") double rateXp,
			@Value("${l2.rates.sp:1.0}") double rateSp) {
		this.rateXp = rateXp;
		this.rateSp = rateSp;
	}

	public record HitResult(int damage, int flags, boolean isDead, int remainingHp, int maxHp, long expReward,
			int spReward) {
	}

	public record HitPlan(int damage, int flags, boolean miss, boolean crit) {
	}

	public HitResult attackNpc(PlayerCharacter attacker, CharTemplate template, NpcInstance target) {
		return attackNpc(attacker, template, target, -1);
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

		// Variacao aleatoria (+/- 5%)
		double rnd = 0.95 + (ThreadLocalRandom.current().nextDouble() * 0.10);
		int damage = Math.max(1, (int) Math.round(baseDam * rnd));
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
		if (target.isDead()) {
			return new HitResult(0, 0, false, 0, target.template().maxHp(), 0, 0); // ja morto: nada a recompensar
		}
		var stats = PlayerStats.calculate(attacker, template);
		double pAtk = stats.pAtk() * (soulshot ? 2.0 : 1.0);
		double pDef = Math.max(1, target.pDef());
		double dmg = (pAtk + power) * 70.0 / pDef;
		boolean crit = blow ? ThreadLocalRandom.current().nextInt(100) < 50
				: ThreadLocalRandom.current().nextInt(1000) < Math.max(40, stats.critical()) / 2;
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
		return skillMagicNpc(attacker, template, target, power, false, false);
	}

	public HitResult skillMagicNpc(PlayerCharacter attacker, CharTemplate template, NpcInstance target,
			double power, boolean sps, boolean bss) {
		if (target.isDead()) {
			return new HitResult(0, 0, false, 0, target.template().maxHp(), 0, 0); // ja morto: nada a recompensar
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
		boolean crit = ThreadLocalRandom.current().nextInt(100) < 5;
		if (crit) {
			dmg *= 3.0;
		}
		dmg *= 0.95 + ThreadLocalRandom.current().nextDouble() * 0.10;
		return applyDamage(target, Math.max(1, (int) Math.round(dmg)), crit ? 0x20 : 0);
	}

	/** Chance (0-100) de um debuff pegar num monstro: base = power do skill, menos a diferenca de nivel. */
	public boolean debuffLands(double basePower, int magicLevel, int attackerLevel, NpcInstance target) {
		return debuffLands(basePower, magicLevel, attackerLevel, target, false, false);
	}

	public boolean debuffLands(double basePower, int magicLevel, int attackerLevel, NpcInstance target,
			boolean sps, boolean bss) {
		double chance = basePower > 0 ? basePower : 50;
		int lvl = magicLevel > 0 ? magicLevel : attackerLevel;
		chance -= Math.max(0, target.template().level() - lvl) * 3;
		if (bss) {
			chance = Math.min(95, chance * 1.5);
		} else if (sps) {
			chance = Math.min(90, chance * 1.25);
		}
		chance = Math.max(10, Math.min(90, chance));
		return ThreadLocalRandom.current().nextDouble(100) < chance;
	}

	public HitResult applyDamage(NpcInstance target, int damage, int flags) {
		int newHp;
		synchronized (target) {
			if (target.isDead()) {
				return new HitResult(0, 0, false, 0, target.template().maxHp(), 0, 0); // ja morto: nada a recompensar
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
		long exp = isDead ? (long) Math.round(baseExp * rateXp) : 0;
		int sp = isDead ? (int) Math.round(baseSp * rateSp) : 0;
		return new HitResult(damage, flags, isDead, newHp, target.template().maxHp(), exp, sp);
	}

	public HitResult attackPlayer(NpcInstance attacker, PlayerCharacter target, CharTemplate targetTemplate) {
		if (attacker.isDead() || target.isDead()) {
			return new HitResult(0, 0, target.isDead(), (int) target.currentHp(), target.maxHp(), 0, 0);
		}

		double pAtk = attacker.pAtk();
		var targetStats = PlayerStats.calculate(target, targetTemplate);
		double pDef = Math.max(1, targetStats.pDef());

		int hitChance = 80;
		boolean miss = ThreadLocalRandom.current().nextInt(100) >= hitChance;
		if (miss) {
			return new HitResult(0, 0x80, false, (int) target.currentHp(), target.maxHp(), 0, 0);
		}

		boolean crit = ThreadLocalRandom.current().nextInt(1000) < 40;
		double baseDam = (pAtk * 70.0) / pDef;
		if (crit) {
			baseDam *= 2.0;
		}

		double rnd = 0.95 + (ThreadLocalRandom.current().nextDouble() * 0.10);
		int damage = Math.max(1, (int) Math.round(baseDam * rnd));
		int flags = crit ? 0x20 : 0x00;

		double newHp = Math.max(0, target.currentHp() - damage);
		target.currentHp(newHp);
		target.onDamaged();
		boolean isDead = newHp <= 0;

		return new HitResult(damage, flags, isDead, (int) newHp, target.maxHp(), 0, 0);
	}

	// ==================== PVP COMBAT & SKILLS ====================

	public record PlayerDamageResult(int damage, int cpDamage, int hpDamage, boolean isDead, int remainingHp,
			int remainingCp) {
	}

	public int skillPhysicalPlayer(PlayerCharacter attacker, CharTemplate attackerTemplate,
			PlayerCharacter target, CharTemplate targetTemplate, double power, boolean soulshot, boolean blow) {
		if (target.isDead()) {
			return 0;
		}
		var attackerStats = PlayerStats.calculate(attacker, attackerTemplate);
		var targetStats = PlayerStats.calculate(target, targetTemplate);
		double pAtk = attackerStats.pAtk() * (soulshot ? 2.0 : 1.0);
		double pDef = Math.max(1, targetStats.pDef());
		double dmg = (pAtk + power) * 70.0 / pDef;
		boolean crit = blow ? ThreadLocalRandom.current().nextInt(100) < 50
				: ThreadLocalRandom.current().nextInt(1000) < Math.max(40, attackerStats.critical()) / 2;
		if (crit) {
			dmg *= 2.0;
		}
		dmg *= 0.95 + ThreadLocalRandom.current().nextDouble() * 0.10;
		return Math.max(1, (int) Math.round(dmg));
	}

	public int skillMagicPlayer(PlayerCharacter attacker, CharTemplate attackerTemplate,
			PlayerCharacter target, CharTemplate targetTemplate, double power, boolean sps, boolean bss) {
		if (target.isDead()) {
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
		return Math.max(1, (int) Math.round(dmg));
	}

	public boolean debuffLandsPlayer(double basePower, int magicLevel, int attackerLevel, PlayerCharacter target,
			boolean sps, boolean bss) {
		double chance = basePower > 0 ? basePower : 50;
		int lvl = magicLevel > 0 ? magicLevel : attackerLevel;
		chance -= Math.max(0, target.level() - lvl) * 3;
		if (bss) {
			chance = Math.min(95, chance * 1.5);
		} else if (sps) {
			chance = Math.min(90, chance * 1.25);
		}
		chance = Math.max(10, Math.min(90, chance));
		return ThreadLocalRandom.current().nextDouble(100) < chance;
	}

	/**
	 * Em PvP no Lineage II, dano consome Combat Points (CP) antes de atingir o HP.
	 */
	public PlayerDamageResult applyDamagePlayer(PlayerCharacter target, int damage) {
		if (target.isDead()) {
			return new PlayerDamageResult(0, 0, 0, true, 0, 0);
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
}
