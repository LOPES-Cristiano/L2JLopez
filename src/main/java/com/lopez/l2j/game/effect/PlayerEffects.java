package com.lopez.l2j.game.effect;

import com.lopez.l2j.game.skill.StatFunc;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Buffs ativos de um jogador, indexados por stackType (mesma regra do L2J: um efeito por stackType, o novo
 * substitui o antigo). Thread-safe: e lido pelo calculo de stats e alterado pelos timers de expiracao.
 */
public final class PlayerEffects {

	/** Duracao "infinita" usada por toggles (o icone vai com -1). */
	public static final long PERMANENT = Long.MAX_VALUE;

	/**
	 * Buff temporario com modificadores de stats (porta enxuta de L2Effect + FuncAdd/FuncMul).
	 *
	 * @param funcs funcoes de stat vindas do skill (buffs/debuffs/toggles); as pocoes usam os campos fixos
	 */
	public record ActiveBuff(int skillId, int level, String stackType, long endTimeMillis, int runSpdAdd,
			double pAtkSpdMul, double mAtkSpdMul, int accuracyAdd, List<StatFunc> funcs) {

		public ActiveBuff(int skillId, int level, String stackType, long endTimeMillis, int runSpdAdd,
				double pAtkSpdMul, double mAtkSpdMul, int accuracyAdd) {
			this(skillId, level, stackType, endTimeMillis, runSpdAdd, pAtkSpdMul, mAtkSpdMul, accuracyAdd, List.of());
		}

		/** Buff de skill: so funcoes de stat. */
		public static ActiveBuff ofSkill(int skillId, int level, String stackType, long endTimeMillis,
				List<StatFunc> funcs) {
			return new ActiveBuff(skillId, level, stackType, endTimeMillis, 0, 1.0, 1.0, 0, List.copyOf(funcs));
		}

		public boolean isPermanent() {
			return endTimeMillis == PERMANENT;
		}

		public int remainingSeconds(long now) {
			if (isPermanent()) {
				return -1;
			}
			return (int) Math.max(0, (endTimeMillis - now) / 1000);
		}
	}

	private final Map<String, ActiveBuff> byStack = new ConcurrentHashMap<>();

	public void put(ActiveBuff buff) {
		byStack.put(buff.stackType(), buff);
	}

	/** Remove apenas se o buff do stack ainda for este (evita apagar um buff renovado). */
	public boolean remove(ActiveBuff buff) {
		return byStack.remove(buff.stackType(), buff);
	}

	/** Remove todos os efeitos de um skill (toggle desligado); devolve true se havia algum. */
	public boolean removeSkill(int skillId) {
		return byStack.values().removeIf(b -> b.skillId() == skillId);
	}

	public boolean hasSkill(int skillId) {
		long now = System.currentTimeMillis();
		return byStack.values().stream().anyMatch(b -> b.skillId() == skillId && b.endTimeMillis() > now);
	}

	public void clear() {
		byStack.clear();
	}

	public void addBuff(int skillId, int level, long durationMs) {
		long end = durationMs == PERMANENT ? PERMANENT : (System.currentTimeMillis() + durationMs);
		put(new ActiveBuff(skillId, level, "skill_" + skillId, end, 0, 1.0, 1.0, 0, List.of()));
	}

	public List<ActiveBuff> active() {
		long now = System.currentTimeMillis();
		List<ActiveBuff> out = new ArrayList<>();
		for (ActiveBuff b : byStack.values()) {
			if (b.endTimeMillis() > now) {
				out.add(b);
			}
		}
		return out;
	}

	public List<ActiveBuff> activeBuffs() {
		return active();
	}

	/** Funcoes de stat de todos os buffs de skill ativos. */
	public List<StatFunc> funcs() {
		List<StatFunc> out = new ArrayList<>();
		for (ActiveBuff b : active()) {
			out.addAll(b.funcs());
		}
		return out;
	}

	public int runSpdAdd() {
		return active().stream().mapToInt(ActiveBuff::runSpdAdd).sum();
	}

	public int accuracyAdd() {
		return active().stream().mapToInt(ActiveBuff::accuracyAdd).sum();
	}

	public double pAtkSpdMul() {
		return active().stream().mapToDouble(ActiveBuff::pAtkSpdMul).reduce(1.0, (a, b) -> a * b);
	}

	public double mAtkSpdMul() {
		return active().stream().mapToDouble(ActiveBuff::mAtkSpdMul).reduce(1.0, (a, b) -> a * b);
	}
}
