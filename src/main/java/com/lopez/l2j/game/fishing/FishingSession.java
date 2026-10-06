package com.lopez.l2j.game.fishing;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Sessao ativa de pesca e combate contra o peixe para um jogador.
 */
public class FishingSession {

	public enum State {
		WAITING_BITE,
		COMBAT,
		FINISHED_WIN,
		FINISHED_LOST
	}

	private final int ownerId;
	private final FishData fish;
	private final int x;
	private final int y;
	private final int z;
	private final boolean nightLure;
	private final int lureType;
	private final boolean upperGrade;

	private int curHp;
	private final int maxHp;
	private int remainingSeconds;
	private int combatMode; // 0 = Pumping, 1 = Reeling
	private int deceptiveMode; // 0 = normal, 1 = enganoso
	private int goodUse; // 0 = resistiu, 1 = sucesso (dano), 2 = falha (peixe cura)
	private int anim; // 0 = tick, 1 = pumping, 2 = reeling
	private int penalty;
	private State state;

	public FishingSession(int ownerId, FishData fish, int x, int y, int z, boolean nightLure, boolean isNoob, boolean isUpperGrade) {
		this.ownerId = ownerId;
		this.fish = fish;
		this.x = x;
		this.y = y;
		this.z = z;
		this.nightLure = nightLure;
		this.maxHp = fish.hp();
		this.curHp = fish.hp();
		this.remainingSeconds = Math.max(10, fish.combatTime() / 1000);
		this.upperGrade = isUpperGrade;
		this.state = State.WAITING_BITE;

		if (isUpperGrade) {
			this.deceptiveMode = ThreadLocalRandom.current().nextInt(100) >= 90 ? 1 : 0;
			this.lureType = 2;
		} else {
			this.deceptiveMode = 0;
			this.lureType = isNoob ? 0 : 1;
		}
		this.combatMode = ThreadLocalRandom.current().nextInt(100) >= 80 ? 1 : 0;
	}

	public void startCombat() {
		this.state = State.COMBAT;
	}

	/**
	 * Executado a cada 1 segundo durante o combate.
	 */
	public synchronized void tickAi() {
		if (state != State.COMBAT) {
			return;
		}

		remainingSeconds--;
		anim = 0;

		// No modo 1 (ou enganoso), o peixe regenera HP
		if (combatMode == 1) {
			if (deceptiveMode == 0) {
				curHp += fish.hpRegen();
			}
		} else if (deceptiveMode == 1) {
			curHp += fish.hpRegen();
		}

		// Chance de alternar modo
		if (ThreadLocalRandom.current().nextInt(100) >= 70) {
			combatMode = combatMode == 0 ? 1 : 0;
		}
		if (upperGrade && ThreadLocalRandom.current().nextInt(100) >= 90) {
			deceptiveMode = deceptiveMode == 0 ? 1 : 0;
		}

		checkCombatStatus();
	}

	/**
	 * Aplicacao da habilidade Pumping.
	 */
	public synchronized void usePumping(int dmg, int pen) {
		if (state != State.COMBAT) {
			return;
		}
		this.anim = 1;
		this.penalty = pen;

		if (ThreadLocalRandom.current().nextInt(100) > 90) {
			// Peixe resistiu
			this.goodUse = 0;
			changeHp(0);
			return;
		}

		if (combatMode == 0) {
			if (deceptiveMode == 0) {
				this.goodUse = 1;
				changeHp(-dmg);
			} else {
				this.goodUse = 2;
				changeHp(dmg);
			}
		} else if (deceptiveMode == 0) {
			this.goodUse = 2;
			changeHp(dmg);
		} else {
			this.goodUse = 1;
			changeHp(-dmg);
		}
	}

	/**
	 * Aplicacao da habilidade Reeling.
	 */
	public synchronized void useReeling(int dmg, int pen) {
		if (state != State.COMBAT) {
			return;
		}
		this.anim = 2;
		this.penalty = pen;

		if (ThreadLocalRandom.current().nextInt(100) > 90) {
			// Peixe resistiu
			this.goodUse = 0;
			changeHp(0);
			return;
		}

		if (combatMode == 1) {
			if (deceptiveMode == 0) {
				this.goodUse = 1;
				changeHp(-dmg);
			} else {
				this.goodUse = 2;
				changeHp(dmg);
			}
		} else if (deceptiveMode == 0) {
			this.goodUse = 2;
			changeHp(dmg);
		} else {
			this.goodUse = 1;
			changeHp(-dmg);
		}
	}

	private void changeHp(int delta) {
		curHp += delta;
		if (curHp < 0) {
			curHp = 0;
		}
		checkCombatStatus();
	}

	private void checkCombatStatus() {
		if (curHp <= 0) {
			state = State.FINISHED_WIN;
		} else if (curHp >= maxHp * 2 || remainingSeconds <= 0) {
			state = State.FINISHED_LOST;
		}
	}

	public int ownerId() { return ownerId; }
	public FishData fish() { return fish; }
	public int x() { return x; }
	public int y() { return y; }
	public int z() { return z; }
	public boolean nightLure() { return nightLure; }
	public int lureType() { return lureType; }
	public int curHp() { return curHp; }
	public int maxHp() { return maxHp; }
	public int remainingSeconds() { return remainingSeconds; }
	public int combatMode() { return combatMode; }
	public int deceptiveMode() { return deceptiveMode; }
	public int goodUse() { return goodUse; }
	public int anim() { return anim; }
	public int penalty() { return penalty; }
	public State state() { return state; }
	public void state(State state) { this.state = state; }
	public boolean isFinished() { return state == State.FINISHED_WIN || state == State.FINISHED_LOST; }
	public boolean isWon() { return state == State.FINISHED_WIN; }
}
