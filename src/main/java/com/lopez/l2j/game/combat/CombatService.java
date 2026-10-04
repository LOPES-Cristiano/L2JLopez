package com.lopez.l2j.game.combat;

import com.lopez.l2j.game.item.ItemSlots;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.template.CharTemplate;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

/**
 * Calculo de combate e formulas físicas/mágicas do Lineage II Interlude.
 */
@Service
public class CombatService {

	public record HitResult(int damage, int flags, boolean isDead, int remainingHp, int maxHp, long expReward,
			int spReward) {
	}

	public HitResult attackNpc(PlayerCharacter attacker, CharTemplate template, NpcInstance target) {
		if (target.isDead()) {
			return new HitResult(0, 0, true, 0, target.template().maxHp(), 0, 0);
		}

		double pAtk = template.pAtk();
		var weapon = attacker.inventory().paperdoll(ItemSlots.RHAND);
		if (weapon == null) {
			weapon = attacker.inventory().paperdoll(ItemSlots.LRHAND);
		}
		if (weapon != null) {
			pAtk += weapon.template().pAtk();
		}

		double pDef = Math.max(1, target.template().pDef());

		// Chance de acerto: precisao vs evasao
		int acc = template.accuracy();
		int eva = Math.max(20, target.template().level() + 30);
		int diff = acc - eva;
		int hitChance = Math.max(28, Math.min(98, 80 + diff * 2));
		boolean miss = ThreadLocalRandom.current().nextInt(100) >= hitChance;

		if (miss) {
			return new HitResult(0, 0x80, false, (int) target.currentHp(), target.template().maxHp(), 0, 0);
		}

		// Chance de critico (base 40-120 per 1000)
		int critRate = template.critical();
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

		int newHp = (int) Math.max(0, target.currentHp() - damage);
		target.currentHp(newHp);
		boolean isDead = newHp <= 0;
		if (isDead) {
			target.dead(true);
		}

		long exp = isDead ? (long) target.template().level() * 150L + 50L : 0;
		int sp = isDead ? (int) (exp / 10) : 0;

		return new HitResult(damage, flags, isDead, newHp, target.template().maxHp(), exp, sp);
	}
}
