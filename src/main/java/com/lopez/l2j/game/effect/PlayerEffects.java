package com.lopez.l2j.game.effect;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.skill.StatFunc;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Buffs ativos de um jogador, indexados por stackType (mesma regra do L2J: um efeito por stackType, o novo
 * substitui o antigo respeitando stackOrder). Thread-safe: e lido pelo calculo de stats e alterado pelos timers de expiracao.
 */
public final class PlayerEffects {

	/** Duracao "infinita" usada por toggles (o icone vai com -1). */
	public static final long PERMANENT = Long.MAX_VALUE;

	/** Limite padrao de debuffs simultaneos no L2J Interlude. */
	public static final int MAX_DEBUFF_AMOUNT = 10;

	/**
	 * Buff temporario com modificadores de stats (porta enxuta de L2Effect + FuncAdd/FuncMul).
	 *
	 * @param funcs funcoes de stat vindas do skill (buffs/debuffs/toggles); as pocoes usam os campos fixos
	 */
	public record ActiveBuff(int skillId, int level, String stackType, int stackOrder, long endTimeMillis,
			int runSpdAdd, double pAtkSpdMul, double mAtkSpdMul, int accuracyAdd, List<StatFunc> funcs,
			boolean isDebuff) {

		public ActiveBuff(int skillId, int level, String stackType, long endTimeMillis, int runSpdAdd,
				double pAtkSpdMul, double mAtkSpdMul, int accuracyAdd, List<StatFunc> funcs, boolean isDebuff) {
			this(skillId, level, stackType, level, endTimeMillis, runSpdAdd, pAtkSpdMul, mAtkSpdMul, accuracyAdd, funcs, isDebuff);
		}

		public ActiveBuff(int skillId, int level, String stackType, long endTimeMillis, int runSpdAdd,
				double pAtkSpdMul, double mAtkSpdMul, int accuracyAdd, List<StatFunc> funcs) {
			this(skillId, level, stackType, level, endTimeMillis, runSpdAdd, pAtkSpdMul, mAtkSpdMul, accuracyAdd, funcs, false);
		}

		public ActiveBuff(int skillId, int level, String stackType, long endTimeMillis, int runSpdAdd,
				double pAtkSpdMul, double mAtkSpdMul, int accuracyAdd) {
			this(skillId, level, stackType, level, endTimeMillis, runSpdAdd, pAtkSpdMul, mAtkSpdMul, accuracyAdd, List.of(), false);
		}

		/** Buff de skill: so funcoes de stat. */
		public static ActiveBuff ofSkill(int skillId, int level, String stackType, long endTimeMillis,
				List<StatFunc> funcs) {
			return new ActiveBuff(skillId, level, stackType, level, endTimeMillis, 0, 1.0, 1.0, 0, List.copyOf(funcs), false);
		}

		public static ActiveBuff ofSkill(int skillId, int level, String stackType, long endTimeMillis,
				List<StatFunc> funcs, boolean isDebuff) {
			return new ActiveBuff(skillId, level, stackType, level, endTimeMillis, 0, 1.0, 1.0, 0, List.copyOf(funcs), isDebuff);
		}

	public static ActiveBuff ofSkill(int skillId, int level, String stackType, int stackOrder, long endTimeMillis,
				List<StatFunc> funcs) {
			return new ActiveBuff(skillId, level, stackType, stackOrder, endTimeMillis, 0, 1.0, 1.0, 0, List.copyOf(funcs), false);
		}

		public static ActiveBuff ofSkill(int skillId, int level, String stackType, int stackOrder, long endTimeMillis,
				List<StatFunc> funcs, boolean isDebuff) {
			return new ActiveBuff(skillId, level, stackType, stackOrder, endTimeMillis, 0, 1.0, 1.0, 0, List.copyOf(funcs), isDebuff);
		}

		public boolean isPermanent() {
			return endTimeMillis == PERMANENT;
		}

		public long remainingMillis(long now) {
			if (isPermanent()) {
				return Long.MAX_VALUE;
			}
			return Math.max(0L, endTimeMillis - now);
		}

		public int remainingSeconds(long now) {
			if (isPermanent()) {
				return -1;
			}
			return (int) Math.max(0, (endTimeMillis - now) / 1000);
		}
	}

	/** Retorna true para Songs e Dances de Bard no Interlude. */
	public static boolean isDanceOrSong(int skillId) {
		return (skillId >= 264 && skillId <= 311) || skillId == 349 || (skillId >= 363 && skillId <= 367);
	}

	/** Retorna slots extras de buff conferidos por Divine Inspiration (skill 1405, niveis 1 a 4). */
	public static int getExtraBuffSlots(PlayerCharacter ch) {
		return ch != null && ch.skills() != null ? Math.max(0, Math.min(4, ch.skills().getOrDefault(1405, 0))) : 0;
	}

	private final Map<String, ActiveBuff> byStack = new java.util.LinkedHashMap<>();
	private int extraBuffSlots = 0;

	public int extraBuffSlots() {
		return extraBuffSlots;
	}

	public void extraBuffSlots(int slots) {
		this.extraBuffSlots = Math.max(0, slots);
	}

	public int maxBuffAmount() {
		return Math.max(1, Config.MAX_BUFFS_AMOUNT) + extraBuffSlots;
	}

	public void put(ActiveBuff buff) {
		put(buff, this.extraBuffSlots);
	}

	public synchronized boolean put(ActiveBuff buff, int extraSlots) {
		if (buff == null || buff.stackType() == null) {
			return false;
		}
		this.extraBuffSlots = Math.max(0, extraSlots);
		long now = System.currentTimeMillis();
		ActiveBuff existing = byStack.get(buff.stackType());
		if (existing != null && existing.remainingMillis(now) > 0) {
			if (Config.CANCEL_LESSER_EFFECT && !buff.isPermanent() && !existing.isPermanent()
					&& buff.stackOrder() < existing.stackOrder()) {
				return false;
			}
		} else {
			if (existing != null) {
				byStack.remove(buff.stackType());
			}
			if (buff.isDebuff()) {
				List<ActiveBuff> debuffs = active().stream()
						.filter(ActiveBuff::isDebuff)
						.toList();
				if (debuffs.size() >= MAX_DEBUFF_AMOUNT) {
					evictOldest(debuffs);
				}
			} else if (!buff.isPermanent() && isDanceOrSong(buff.skillId())) {
				int maxDances = Math.max(1, Config.MAX_DANCES_AMOUNT);
				List<ActiveBuff> dances = active().stream()
						.filter(b -> !b.isDebuff() && !b.isPermanent() && isDanceOrSong(b.skillId()))
						.toList();
				if (dances.size() >= maxDances) {
					evictOldest(dances);
				}
			} else if (!buff.isPermanent()) {
				int maxBuffs = Math.max(1, Config.MAX_BUFFS_AMOUNT) + Math.max(0, extraSlots);
				List<ActiveBuff> normalBuffs = active().stream()
						.filter(b -> !b.isDebuff() && !b.isPermanent() && !isDanceOrSong(b.skillId()))
						.toList();
				if (normalBuffs.size() >= maxBuffs) {
					evictOldest(normalBuffs);
				}
			}
		}
		byStack.put(buff.stackType(), buff);
		return true;
	}

	private void evictOldest(List<ActiveBuff> candidates) {
		ActiveBuff oldest = null;
		for (ActiveBuff b : candidates) {
			if (oldest == null || b.endTimeMillis() < oldest.endTimeMillis()) {
				oldest = b;
			}
		}
		if (oldest != null) {
			remove(oldest);
		}
	}

	/** Remove apenas se o buff do stack ainda for este (evita apagar um buff renovado). */
	public synchronized boolean remove(ActiveBuff buff) {
		return byStack.remove(buff.stackType(), buff);
	}

	/** Remove todos os efeitos de um skill (toggle desligado); devolve true se havia algum. */
	public synchronized boolean removeSkill(int skillId) {
		return byStack.values().removeIf(b -> b.skillId() == skillId);
	}

	/**
	 * Remove o efeito ativo de uma skill de Argumentacao (Life Stone) ao desequipar a arma,
	 * caso Config.CANCEL_AUGMENTATION_EFFECT esteja habilitado.
	 */
	public synchronized boolean removeAugmentationEffects(int augSkillId) {
		if (!Config.CANCEL_AUGMENTATION_EFFECT) {
			return false;
		}
		if (augSkillId > 0) {
			return removeSkill(augSkillId);
		}
		return byStack.values().removeIf(b -> b.skillId() >= 3123 && b.skillId() <= 3259);
	}

	/**
	 * Executa o efeito Cancel/Bane removendo buffs positivos conforme Config.CANCEL_MODE ("new" ou "old").
	 */
	public synchronized int cancelBuffs(int maxCancel) {
		int limit = maxCancel > 0 ? maxCancel : 5;
		List<ActiveBuff> cancelable = active().stream()
				.filter(b -> !b.isDebuff() && !b.isPermanent())
				.toList();
		if (cancelable.isEmpty()) {
			return 0;
		}
		int removed = 0;
		if ("old".equalsIgnoreCase(Config.CANCEL_MODE)) {
			for (ActiveBuff b : cancelable) {
				if (removed >= limit) {
					break;
				}
				if (java.util.concurrent.ThreadLocalRandom.current().nextInt(100) < 50 && remove(b)) {
					removed++;
				}
			}
		} else {
			List<ActiveBuff> ordered = new ArrayList<>(cancelable);
			ordered.sort((a, b) -> {
				boolean da = isDanceOrSong(a.skillId());
				boolean db = isDanceOrSong(b.skillId());
				if (da != db) {
					return da ? -1 : 1;
				}
				return Long.compare(b.endTimeMillis(), a.endTimeMillis());
			});
			for (ActiveBuff b : ordered) {
				if (removed >= limit) {
					break;
				}
				if (remove(b)) {
					removed++;
				}
			}
		}
		return removed;
	}

	public synchronized boolean hasSkill(int skillId) {
		long now = System.currentTimeMillis();
		return byStack.values().stream().anyMatch(b -> b.skillId() == skillId && b.endTimeMillis() > now);
	}

	public synchronized int getSkillLevel(int skillId) {
		long now = System.currentTimeMillis();
		return byStack.values().stream()
				.filter(b -> b.skillId() == skillId && b.endTimeMillis() > now)
				.mapToInt(ActiveBuff::level)
				.max()
				.orElse(0);
	}

	/**
	 * Remove buffs positivos por IDs de skill, stackType ou atributo modificado em StatFunc.
	 * Usado por Warrior Bane, Mage Bane, Block Shield e Block Wind Walk.
	 */
	public synchronized boolean removeBuffsByIdsOrStackOrStats(java.util.Set<Integer> skillIds,
			java.util.Set<String> stackKeywords, java.util.Set<String> statNames) {
		long now = System.currentTimeMillis();
		return byStack.values().removeIf(b -> {
			if (b.isDebuff() || b.isPermanent() || b.endTimeMillis() <= now) {
				return false;
			}
			if (skillIds != null && skillIds.contains(b.skillId())) {
				return true;
			}
			if (stackKeywords != null && b.stackType() != null) {
				String st = b.stackType().toLowerCase(java.util.Locale.ROOT);
				for (String kw : stackKeywords) {
					if (st.contains(kw.toLowerCase(java.util.Locale.ROOT))) {
						return true;
					}
				}
			}
			if (statNames != null) {
				if (statNames.contains("runSpd") && b.runSpdAdd() > 0) {
					return true;
				}
				if (statNames.contains("pAtkSpd") && b.pAtkSpdMul() > 1.0) {
					return true;
				}
				if (statNames.contains("mAtkSpd") && b.mAtkSpdMul() > 1.0) {
					return true;
				}
				if (b.funcs() != null) {
					for (StatFunc f : b.funcs()) {
						for (String s : statNames) {
							if (s.equalsIgnoreCase(f.stat())) {
								return true;
							}
						}
					}
				}
			}
			return false;
		});
	}

	/**
	 * Rouba até {@code maxSteal} buffs positivos (nao-permanentes) do alvo, removendo-os deste {@link PlayerEffects}
	 * e retornando a lista de buffs roubados para aplicacao no conjurador (Steal Divinity / STEAL_BUFF).
	 */
	public synchronized List<ActiveBuff> stealBuffs(int maxSteal) {
		int limit = maxSteal > 0 ? maxSteal : 3;
		List<ActiveBuff> cancelable = new ArrayList<>(active().stream()
				.filter(b -> !b.isDebuff() && !b.isPermanent())
				.toList());
		if (cancelable.isEmpty()) {
			return List.of();
		}
		cancelable.sort((a, b) -> {
			boolean da = isDanceOrSong(a.skillId());
			boolean db = isDanceOrSong(b.skillId());
			if (da != db) {
				return da ? -1 : 1;
			}
			return Long.compare(b.endTimeMillis(), a.endTimeMillis());
		});
		List<ActiveBuff> stolen = new ArrayList<>();
		for (ActiveBuff b : cancelable) {
			if (stolen.size() >= limit) {
				break;
			}
			if (remove(b)) {
				stolen.add(b);
			}
		}
		return stolen;
	}

	public synchronized void clear() {
		byStack.clear();
	}

	public synchronized void clearDebuffs() {
		byStack.values().removeIf(ActiveBuff::isDebuff);
	}

	public synchronized void removeDebuffsByStackOrName(String... keywords) {
		byStack.values().removeIf(b -> {
			if (!b.isDebuff()) return false;
			String st = b.stackType().toLowerCase(java.util.Locale.ROOT);
			for (String kw : keywords) {
				if (st.contains(kw.toLowerCase(java.util.Locale.ROOT))) {
					return true;
				}
			}
			return false;
		});
	}

	public void stopAllEffects() {
		clear();
	}

	public void addBuff(int skillId, int level, long durationMs) {
		long end = durationMs == PERMANENT ? PERMANENT : (System.currentTimeMillis() + durationMs);
		put(new ActiveBuff(skillId, level, "skill_" + skillId, end, 0, 1.0, 1.0, 0, List.of()));
	}

	public synchronized List<ActiveBuff> active() {
		long now = System.currentTimeMillis();
		List<ActiveBuff> buffs = new ArrayList<>();
		List<ActiveBuff> toggles = new ArrayList<>();
		List<ActiveBuff> debuffs = new ArrayList<>();
		for (ActiveBuff b : byStack.values()) {
			if (b.endTimeMillis() > now) {
				if (b.isDebuff()) {
					debuffs.add(b);
				} else if (b.isPermanent()) {
					toggles.add(b);
				} else {
					buffs.add(b);
				}
			}
		}
		List<ActiveBuff> out = new ArrayList<>(buffs.size() + toggles.size() + debuffs.size());
		out.addAll(buffs);
		out.addAll(toggles);
		out.addAll(debuffs);
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
