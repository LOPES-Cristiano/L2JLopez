package com.lopez.l2j.game.effect;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.combat.Formulas;
import com.lopez.l2j.game.geodata.GeoEngine;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.skill.StatFunc;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * Buffs ativos de um jogador, indexados por stackType (mesma regra do L2J: um efeito por stackType, o novo
 * substitui o antigo respeitando stackOrder). Thread-safe: e lido pelo calculo de stats e alterado pelos timers de expiracao.
 */
public final class PlayerEffects {

	/** Duracao "infinita" usada por toggles (o icone vai com -1). */
	public static final long PERMANENT = Long.MAX_VALUE;

	/** Limite padrao de debuffs simultaneos no L2J Interlude. */
	public static final int MAX_DEBUFF_AMOUNT = 10;

	/** Mascara AbnormalEffect de Fear/TurnFlee no cliente Interlude. */
	public static final int ABNORMAL_FEAR = 0x00000010;

	/**
	 * Gatilho de skill reativa (ChanceSkillTrigger: Mirage, Counterattack, Augmentation triggers, etc.).
	 */
	public record ChanceTrigger(int sourceSkillId, int triggeredSkillId, int triggeredLevel, String chanceType, double chance) {
	}

	/**
	 * Buff temporario com modificadores de stats (porta enxuta de L2Effect + FuncAdd/FuncMul).
	 *
	 * @param funcs funcoes de stat vindas do skill (buffs/debuffs/toggles); as pocoes usam os campos fixos
	 */
	public record ActiveBuff(int skillId, int level, String stackType, int stackOrder, long endTimeMillis,
			int runSpdAdd, double pAtkSpdMul, double mAtkSpdMul, int accuracyAdd, List<StatFunc> funcs,
			boolean isDebuff, int abnormalMask, String effectName, double tickVal, int periodSeconds) {

		public ActiveBuff(int skillId, int level, String stackType, int stackOrder, long endTimeMillis,
				int runSpdAdd, double pAtkSpdMul, double mAtkSpdMul, int accuracyAdd, List<StatFunc> funcs,
				boolean isDebuff, int abnormalMask) {
			this(skillId, level, stackType, stackOrder, endTimeMillis, runSpdAdd, pAtkSpdMul, mAtkSpdMul, accuracyAdd,
					funcs, isDebuff, abnormalMask, "", 0.0, 0);
		}

		public ActiveBuff(int skillId, int level, String stackType, int stackOrder, long endTimeMillis,
				int runSpdAdd, double pAtkSpdMul, double mAtkSpdMul, int accuracyAdd, List<StatFunc> funcs,
				boolean isDebuff) {
			this(skillId, level, stackType, stackOrder, endTimeMillis, runSpdAdd, pAtkSpdMul, mAtkSpdMul, accuracyAdd,
					funcs, isDebuff, 0, "", 0.0, 0);
		}

		public ActiveBuff(int skillId, int level, String stackType, long endTimeMillis, int runSpdAdd,
				double pAtkSpdMul, double mAtkSpdMul, int accuracyAdd, List<StatFunc> funcs, boolean isDebuff) {
			this(skillId, level, stackType, level, endTimeMillis, runSpdAdd, pAtkSpdMul, mAtkSpdMul, accuracyAdd,
					funcs, isDebuff, 0, "", 0.0, 0);
		}

		public ActiveBuff(int skillId, int level, String stackType, long endTimeMillis, int runSpdAdd,
				double pAtkSpdMul, double mAtkSpdMul, int accuracyAdd, List<StatFunc> funcs) {
			this(skillId, level, stackType, level, endTimeMillis, runSpdAdd, pAtkSpdMul, mAtkSpdMul, accuracyAdd,
					funcs, false, 0, "", 0.0, 0);
		}

		public ActiveBuff(int skillId, int level, String stackType, long endTimeMillis, int runSpdAdd,
				double pAtkSpdMul, double mAtkSpdMul, int accuracyAdd) {
			this(skillId, level, stackType, level, endTimeMillis, runSpdAdd, pAtkSpdMul, mAtkSpdMul, accuracyAdd,
					List.of(), false, 0, "", 0.0, 0);
		}

		/** Buff de skill: so funcoes de stat. */
		public static ActiveBuff ofSkill(int skillId, int level, String stackType, long endTimeMillis,
				List<StatFunc> funcs) {
			return new ActiveBuff(skillId, level, stackType, level, endTimeMillis, 0, 1.0, 1.0, 0,
					List.copyOf(funcs), false, 0, "", 0.0, 0);
		}

		public static ActiveBuff ofSkill(int skillId, int level, String stackType, long endTimeMillis,
				List<StatFunc> funcs, boolean isDebuff) {
			return new ActiveBuff(skillId, level, stackType, level, endTimeMillis, 0, 1.0, 1.0, 0,
					List.copyOf(funcs), isDebuff, 0, "", 0.0, 0);
		}

		public static ActiveBuff ofSkill(int skillId, int level, String stackType, int stackOrder, long endTimeMillis,
				List<StatFunc> funcs) {
			return new ActiveBuff(skillId, level, stackType, stackOrder, endTimeMillis, 0, 1.0, 1.0, 0,
					List.copyOf(funcs), false, 0, "", 0.0, 0);
		}

		public static ActiveBuff ofSkill(int skillId, int level, String stackType, int stackOrder, long endTimeMillis,
				List<StatFunc> funcs, boolean isDebuff) {
			return new ActiveBuff(skillId, level, stackType, stackOrder, endTimeMillis, 0, 1.0, 1.0, 0,
					List.copyOf(funcs), isDebuff, 0, "", 0.0, 0);
		}

		public static ActiveBuff ofSkill(int skillId, int level, String stackType, int stackOrder, long endTimeMillis,
				List<StatFunc> funcs, boolean isDebuff, int abnormalMask) {
			return new ActiveBuff(skillId, level, stackType, stackOrder, endTimeMillis, 0, 1.0, 1.0, 0,
					List.copyOf(funcs), isDebuff, abnormalMask, "", 0.0, 0);
		}

		public static ActiveBuff ofSkill(int skillId, int level, String stackType, int stackOrder, long endTimeMillis,
				List<StatFunc> funcs, boolean isDebuff, int abnormalMask, String effectName, double tickVal,
				int periodSeconds) {
			return new ActiveBuff(skillId, level, stackType, stackOrder, endTimeMillis, 0, 1.0, 1.0, 0,
					List.copyOf(funcs), isDebuff, abnormalMask, effectName != null ? effectName : "", tickVal,
					periodSeconds);
		}

		public boolean isHerb() {
			return isHerbSkill(skillId)
					|| (effectName != null && effectName.toLowerCase(Locale.ROOT).contains("herb"))
					|| (stackType != null && stackType.toLowerCase(Locale.ROOT).startsWith("herb_"));
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

	/** Retorna true para Skills de Herb (2278-2285 e 2512-2514) no Interlude. */
	public static boolean isHerbSkill(int skillId) {
		return (skillId >= 2278 && skillId <= 2285) || (skillId >= 2512 && skillId <= 2514);
	}

	/** Retorna true se o ActiveBuff for um efeito de Herb. */
	public static boolean isHerbBuff(ActiveBuff buff) {
		return buff != null && buff.isHerb();
	}

	/** Retorna true para Songs e Dances de Bard no Interlude. */
	public static boolean isDanceOrSong(int skillId) {
		return (skillId >= 264 && skillId <= 311) || skillId == 349 || (skillId >= 363 && skillId <= 367);
	}

	/** Retorna slots extras de buff conferidos por Divine Inspiration (skill 1405, niveis 1 a 4). */
	public static int getExtraBuffSlots(PlayerCharacter ch) {
		return ch != null && ch.skills() != null ? Math.max(0, Math.min(4, ch.skills().getOrDefault(1405, 0))) : 0;
	}

	/**
	 * Converte o stackOrder do XML (que pode ser fracionario como 0.2..0.3 no Focus ou 1.08..1.35 em Might/Shield/Acumen/Haste)
	 * para uma escala inteira preservando precisao de duas casas decimais, evitando que Math.round transforme 0.3 em 0
	 * ou 1.08..1.35 em 1 (abaixo do proprio level do skill).
	 */
	public static int resolveStackOrder(double xmlStackOrder, int skillLevel) {
		if (xmlStackOrder <= 0) {
			return Math.max(1, skillLevel);
		}
		if (Math.abs(xmlStackOrder - Math.rint(xmlStackOrder)) > 1e-6) {
			return Math.max(1, (int) Math.round(xmlStackOrder * 100.0));
		}
		int rounded = (int) Math.round(xmlStackOrder);
		return rounded < 10 ? rounded * 100 : rounded;
	}

	private final Map<String, ActiveBuff> byStack = new java.util.LinkedHashMap<>();
	/** Buffs regulares sombreados temporariamente por Herbs do mesmo stackType (#176). */
	private final Map<String, ActiveBuff> shadowedByHerb = new ConcurrentHashMap<>();
	/** Gatilhos de ChanceSkillTrigger ativos (#175). */
	private final Map<Integer, ChanceTrigger> chanceTriggers = new ConcurrentHashMap<>();

	private int extraBuffSlots = 0;
	private volatile boolean explicitBuffBlocked = false;
	private volatile boolean explicitDebuffBlocked = false;
	private volatile long afraidUntilMs = 0L;
	private volatile int fearCasterX = 0;
	private volatile int fearCasterY = 0;

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
		purgeExpiredAndRestoreShadowed(now);

		// #173: BlockBuff (Celestial Shield 1418) e BlockDebuff (Mystic Immunity 1411)
		if (buff.isDebuff()) {
			if (isDebuffBlocked()) {
				return false;
			}
		} else if (!buff.isPermanent() && buff.skillId() != 1418 && buff.skillId() != 1411) {
			if (isBuffBlocked()) {
				return false;
			}
		}

		String fallbackKey1 = "skill_" + buff.skillId();
		String fallbackKey2 = "saved_" + buff.skillId();
		if (!buff.stackType().equals(fallbackKey1)) {
			byStack.remove(fallbackKey1);
		}
		if (!buff.stackType().equals(fallbackKey2)) {
			byStack.remove(fallbackKey2);
		}
		if (buff.stackType().equals(fallbackKey1) || buff.stackType().equals(fallbackKey2)) {
			for (ActiveBuff b : byStack.values()) {
				if (b.skillId() == buff.skillId() && !b.stackType().equals(buff.stackType())) {
					return false;
				}
			}
		}

		ActiveBuff existing = byStack.get(buff.stackType());
		if (existing != null && existing.remainingMillis(now) > 0) {
			// #176: Herb Shadow Buffs - Herb sombreia buff normal sem destrui-lo
			if (buff.isHerb() && !existing.isHerb() && !existing.isDebuff() && !existing.isPermanent()) {
				shadowedByHerb.put(buff.stackType(), existing);
				byStack.put(buff.stackType(), buff);
				autoRegisterBuiltInChanceTrigger(buff);
				return true;
			}
			// Se um buff normal for lancado enquanto uma Herb do mesmo stackType estiver ativa,
			// atualiza o buff sombreado para que ele vigore quando a Herb expirar.
			if (!buff.isHerb() && existing.isHerb() && !buff.isDebuff() && !buff.isPermanent()) {
				ActiveBuff prevShadow = shadowedByHerb.get(buff.stackType());
				if (prevShadow == null || !Config.CANCEL_LESSER_EFFECT || buff.stackOrder() >= prevShadow.stackOrder()) {
					shadowedByHerb.put(buff.stackType(), buff);
				}
				if (buff.stackOrder() > existing.stackOrder()) {
					shadowedByHerb.remove(buff.stackType());
					byStack.put(buff.stackType(), buff);
					autoRegisterBuiltInChanceTrigger(buff);
				}
				return true;
			}
			if (Config.CANCEL_LESSER_EFFECT && !buff.isPermanent() && !existing.isPermanent()
					&& buff.stackOrder() < existing.stackOrder()) {
				return false;
			}
		} else {
			if (existing != null) {
				byStack.remove(buff.stackType());
				if (existing.isHerb()) {
					restoreShadowedIfPresent(buff.stackType(), now);
				}
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
			} else if (!buff.isPermanent() && !buff.isHerb()) {
				int maxBuffs = Math.max(1, Config.MAX_BUFFS_AMOUNT) + Math.max(0, extraSlots);
				List<ActiveBuff> normalBuffs = active().stream()
						.filter(b -> !b.isDebuff() && !b.isPermanent() && !isDanceOrSong(b.skillId()) && !b.isHerb())
						.toList();
				if (normalBuffs.size() >= maxBuffs) {
					evictOldest(normalBuffs);
				}
			}
		}
		byStack.put(buff.stackType(), buff);
		autoRegisterBuiltInChanceTrigger(buff);
		return true;
	}

	private void purgeExpiredAndRestoreShadowed(long now) {
		List<ActiveBuff> expired = new ArrayList<>();
		for (ActiveBuff b : byStack.values()) {
			if (!b.isPermanent() && b.endTimeMillis() <= now) {
				expired.add(b);
			}
		}
		for (ActiveBuff b : expired) {
			byStack.remove(b.stackType(), b);
			chanceTriggers.remove(b.skillId());
			if (b.isHerb()) {
				restoreShadowedIfPresent(b.stackType(), now);
			}
		}
		shadowedByHerb.values().removeIf(b -> !b.isPermanent() && b.endTimeMillis() <= now);
	}

	private void restoreShadowedIfPresent(String stackType, long now) {
		if (stackType == null) {
			return;
		}
		ActiveBuff shadowed = shadowedByHerb.remove(stackType);
		if (shadowed != null && (shadowed.isPermanent() || shadowed.endTimeMillis() > now)) {
			byStack.put(stackType, shadowed);
		}
	}

	public synchronized ActiveBuff getShadowedBuff(String stackType) {
		return stackType != null ? shadowedByHerb.get(stackType) : null;
	}

	private void autoRegisterBuiltInChanceTrigger(ActiveBuff buff) {
		if (buff == null) {
			return;
		}
		// Mirage (445) -> Mirage Trigger (5144) ao ser atacado
		if (buff.skillId() == 445) {
			registerChanceTrigger(445, 5144, 1, "ON_ATTACKED", 60.0);
		} else if (buff.skillId() == 447) {
			// Counterattack (447) -> reflete/dispara em ON_ATTACKED_HIT
			registerChanceTrigger(447, 447, buff.level(), "ON_ATTACKED_HIT", 50.0);
		}
	}

	private void evictOldest(List<ActiveBuff> candidates) {
		if (!candidates.isEmpty()) {
			remove(candidates.get(0));
		}
	}

	/** Remove apenas se o buff do stack ainda for este (evita apagar um buff renovado). */
	public synchronized boolean remove(ActiveBuff buff) {
		if (buff == null) {
			return false;
		}
		boolean removed = byStack.remove(buff.stackType(), buff);
		if (removed) {
			chanceTriggers.remove(buff.skillId());
			if (buff.isHerb()) {
				restoreShadowedIfPresent(buff.stackType(), System.currentTimeMillis());
			}
		} else if (!buff.isHerb()) {
			shadowedByHerb.remove(buff.stackType(), buff);
		}
		return removed;
	}

	/** Remove todos os efeitos de um skill (toggle desligado); devolve true se havia algum. */
	public synchronized boolean removeSkill(int skillId) {
		chanceTriggers.remove(skillId);
		shadowedByHerb.values().removeIf(b -> b.skillId() == skillId);
		List<ActiveBuff> toRemove = new ArrayList<>();
		for (ActiveBuff b : byStack.values()) {
			if (b.skillId() == skillId) {
				toRemove.add(b);
			}
		}
		if (toRemove.isEmpty()) {
			return false;
		}
		long now = System.currentTimeMillis();
		for (ActiveBuff b : toRemove) {
			byStack.remove(b.stackType(), b);
			if (b.isHerb()) {
				restoreShadowedIfPresent(b.stackType(), now);
			}
		}
		return true;
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
		return cancelBuffs(maxCancel, 78, 78, 25.0, 0.0, 0.0);
	}

	/**
	 * #136: Executa Cancel com rolagem individual por buff (Formulas.calcCancelSuccess) do fim para o inicio
	 * (buffs mais recentes e dances/songs primeiro), sem tocar em debuffs ou toggles.
	 */
	public synchronized int cancelBuffs(int maxCancel, int magicLevel, int targetLevel, double baseCancelRate,
			double cancelProf, double cancelVuln) {
		return cancelBuffsWithDetails(maxCancel, magicLevel, targetLevel, baseCancelRate, cancelProf, cancelVuln).size();
	}

	public synchronized List<ActiveBuff> cancelBuffsWithDetails(int maxCancel, int magicLevel, int targetLevel,
			double baseCancelRate, double cancelProf, double cancelVuln) {
		int limit = maxCancel > 0 ? maxCancel : 5;
		long now = System.currentTimeMillis();
		List<ActiveBuff> cancelable = active().stream()
				.filter(b -> !b.isDebuff() && !b.isPermanent() && b.skillId() != 1323 && b.skillId() != 1410
						&& b.skillId() != 438 && b.skillId() != 5041 && b.skillId() != 2168)
				.toList();
		if (cancelable.isEmpty()) {
			return List.of();
		}
		List<ActiveBuff> ordered = new ArrayList<>(cancelable);
		if ("old".equalsIgnoreCase(Config.CANCEL_MODE)) {
			Collections.reverse(ordered);
		} else {
			ordered.sort((a, b) -> {
				boolean da = isDanceOrSong(a.skillId());
				boolean db = isDanceOrSong(b.skillId());
				if (da != db) {
					return da ? -1 : 1;
				}
				return Long.compare(b.endTimeMillis(), a.endTimeMillis());
			});
		}
		List<ActiveBuff> removedList = new ArrayList<>();
		double effBaseRate = baseCancelRate > 0 ? baseCancelRate : 25.0;
		for (ActiveBuff b : ordered) {
			if (removedList.size() >= limit) {
				break;
			}
			int remSec = Math.max(1, b.remainingSeconds(now));
			if (Formulas.calcCancelSuccess(magicLevel, targetLevel, effBaseRate, remSec, cancelProf, cancelVuln)) {
				if (remove(b)) {
					removedList.add(b);
				}
			}
		}
		// Se nenhum passou no roll individual mas havia buffs cancelaveis, garante ao menos 1 se baseRate >= 25
		if (removedList.isEmpty() && !ordered.isEmpty() && effBaseRate >= 25.0) {
			ActiveBuff first = ordered.get(0);
			if (Formulas.calcCancelSuccess(magicLevel, targetLevel, effBaseRate + 25.0,
					Math.max(1, first.remainingSeconds(now)), cancelProf, cancelVuln) && remove(first)) {
				removedList.add(first);
			}
		}
		return removedList;
	}

	/**
	 * #137: Warrior Bane remove buffs com stackType attack_time_down (Haste) e speed_up (Wind Walk).
	 */
	public synchronized boolean removeWarriorBaneBuffs() {
		return removeBuffsByIdsOrStackOrStats(
				Set.of(1086, 1204, 2011, 2012, 2034, 2035, 2054, 2058, 2282, 2285),
				Set.of("attack_time_down", "speed_up", "p_atk_spd", "run_spd"),
				Set.of("pAtkSpd", "runSpd"));
	}

	/**
	 * #137: Mage Bane remove buffs com stackType casting_time_down (Acumen) e ma_up (Empower).
	 */
	public synchronized boolean removeMageBaneBuffs() {
		return removeBuffsByIdsOrStackOrStats(
				Set.of(1059, 1085, 2053, 2056, 2169, 2281, 2283),
				Set.of("casting_time_down", "ma_up", "m_atk_spd", "m_atk"),
				Set.of("mAtkSpd", "mAtk"));
	}

	public synchronized boolean hasSkill(int skillId) {
		long now = System.currentTimeMillis();
		return byStack.values().stream().anyMatch(b -> b.skillId() == skillId && b.endTimeMillis() > now)
				|| shadowedByHerb.values().stream().anyMatch(b -> b.skillId() == skillId && b.endTimeMillis() > now);
	}

	public synchronized int getSkillLevel(int skillId) {
		long now = System.currentTimeMillis();
		int activeLvl = byStack.values().stream()
				.filter(b -> b.skillId() == skillId && b.endTimeMillis() > now)
				.mapToInt(ActiveBuff::level)
				.max()
				.orElse(0);
		if (activeLvl > 0) {
			return activeLvl;
		}
		return shadowedByHerb.values().stream()
				.filter(b -> b.skillId() == skillId && b.endTimeMillis() > now)
				.mapToInt(ActiveBuff::level)
				.max()
				.orElse(0);
	}

	private synchronized boolean hasActiveEffectName(String... effectNames) {
		long now = System.currentTimeMillis();
		for (ActiveBuff b : byStack.values()) {
			if (b.endTimeMillis() <= now || b.effectName() == null || b.effectName().isEmpty()) {
				continue;
			}
			for (String name : effectNames) {
				if (b.effectName().equalsIgnoreCase(name)) {
					return true;
				}
			}
		}
		return false;
	}

	private synchronized boolean hasActiveStack(String... stacks) {
		long now = System.currentTimeMillis();
		for (ActiveBuff b : byStack.values()) {
			if (b.endTimeMillis() <= now || b.stackType() == null) {
				continue;
			}
			String st = b.stackType().toLowerCase(Locale.ROOT);
			for (String s : stacks) {
				if (st.contains(s.toLowerCase(Locale.ROOT))) {
					return true;
				}
			}
		}
		return false;
	}

	// =========================================================================
	// #166 - PhoenixBless / Salvation (Skill 1410 Salvation, Skill 438 Soul of the Phoenix)
	// =========================================================================

	public synchronized boolean hasSalvation() {
		return hasSkill(1410) || hasSkill(438)
				|| hasActiveEffectName("PhoenixBless", "Salvation")
				|| hasActiveStack("phoenix_blessing", "salvation");
	}

	public synchronized boolean hasPhoenixBless() {
		return hasSalvation();
	}

	public synchronized boolean consumeSalvation() {
		if (!hasSalvation()) {
			return false;
		}
		removeSkill(1410);
		removeSkill(438);
		byStack.values().removeIf(b -> {
			String en = b.effectName() != null ? b.effectName() : "";
			String st = b.stackType() != null ? b.stackType().toLowerCase(Locale.ROOT) : "";
			return en.equalsIgnoreCase("PhoenixBless") || en.equalsIgnoreCase("Salvation")
					|| st.contains("phoenix_blessing") || st.contains("salvation");
		});
		return true;
	}

	public synchronized boolean consumePhoenixBless() {
		return consumeSalvation();
	}

	// =========================================================================
	// #167 - NoblesseBless (Skill 1323) & LuckyCharm / Charm of Luck (Skill 2168)
	// =========================================================================

	public synchronized boolean hasNoblesseBless() {
		return hasSkill(1323) || hasActiveEffectName("NoblesseBless") || hasActiveStack("noblesse_blessing");
	}

	public synchronized boolean consumeNoblesseBless() {
		if (!hasNoblesseBless()) {
			return false;
		}
		removeSkill(1323);
		byStack.values().removeIf(b -> {
			String en = b.effectName() != null ? b.effectName() : "";
			String st = b.stackType() != null ? b.stackType().toLowerCase(Locale.ROOT) : "";
			return en.equalsIgnoreCase("NoblesseBless") || st.contains("noblesse_blessing");
		});
		return true;
	}

	public synchronized boolean hasCharmOfLuck() {
		return hasSkill(2168) || hasActiveEffectName("CharmOfLuck", "LuckyCharm")
				|| hasActiveStack("charm_of_luck", "lucky_charm");
	}

	public synchronized boolean hasLuckyCharm() {
		return hasCharmOfLuck();
	}

	public synchronized boolean consumeCharmOfLuck() {
		if (!hasCharmOfLuck()) {
			return false;
		}
		removeSkill(2168);
		byStack.values().removeIf(b -> {
			String en = b.effectName() != null ? b.effectName() : "";
			String st = b.stackType() != null ? b.stackType().toLowerCase(Locale.ROOT) : "";
			return en.equalsIgnoreCase("CharmOfLuck") || en.equalsIgnoreCase("LuckyCharm")
					|| st.contains("charm_of_luck") || st.contains("lucky_charm");
		});
		return true;
	}

	// =========================================================================
	// #168 - CharmOfCourage (Skill 5041)
	// =========================================================================

	public synchronized boolean hasCharmOfCourage() {
		return hasSkill(5041) || hasActiveEffectName("CharmOfCourage") || hasActiveStack("charm_of_courage");
	}

	public synchronized boolean consumeCharmOfCourage() {
		if (!hasCharmOfCourage()) {
			return false;
		}
		removeSkill(5041);
		byStack.values().removeIf(b -> {
			String en = b.effectName() != null ? b.effectName() : "";
			String st = b.stackType() != null ? b.stackType().toLowerCase(Locale.ROOT) : "";
			return en.equalsIgnoreCase("CharmOfCourage") || st.contains("charm_of_courage");
		});
		return true;
	}

	// =========================================================================
	// #169 - SilentMove (221 / 296), FakeDeath (60), Relax (226) & onActionTime
	// =========================================================================

	public synchronized boolean isSilentMoving() {
		return hasSkill(221) || hasSkill(296)
				|| hasActiveEffectName("SilentMove", "ChameleonRest")
				|| hasActiveStack("silent_move", "chameleon_rest");
	}

	public synchronized boolean isFakeDeath() {
		return hasSkill(60) || hasActiveEffectName("FakeDeath") || hasActiveStack("fake_death");
	}

	public synchronized boolean isRelaxing() {
		return hasSkill(226) || hasActiveEffectName("Relax") || hasActiveStack("relax");
	}

	public void onActionTime(PlayerCharacter player) {
		onActionTime(player, null);
	}

	/**
	 * #169: Processa o tick periodico de toggles (Relax 226, Silent Move 221, Chameleon Rest 296, Fake Death 60, etc.).
	 */
	public synchronized void onActionTime(PlayerCharacter player, Consumer<GameServerPacket> packetSender) {
		if (player == null) {
			return;
		}
		long now = System.currentTimeMillis();
		purgeExpiredAndRestoreShadowed(now);
		if (player.isDead()) {
			return;
		}
		List<ActiveBuff> snapshot = new ArrayList<>(byStack.values());
		for (ActiveBuff b : snapshot) {
			if (!b.isPermanent()) {
				continue;
			}
			int id = b.skillId();
			String en = b.effectName() != null ? b.effectName() : "";
			boolean isRelax = id == 226 || en.equalsIgnoreCase("Relax");
			boolean isFakeDeath = id == 60 || en.equalsIgnoreCase("FakeDeath");
			boolean isSilentMove = id == 221 || id == 296 || en.equalsIgnoreCase("SilentMove")
					|| en.equalsIgnoreCase("ChameleonRest");

			if (isRelax) {
				if (!player.isSitting()) {
					removeSkill(id);
					if (packetSender != null) {
						packetSender.accept(SystemMessage.of(SystemMessage.EFFECT_S1_DISAPPEARED,
								new SystemMessage.SkillName(id, b.level())));
					}
					continue;
				}
				if (player.currentHp() >= player.maxHp()) {
					continue;
				}
				double mpDrain = b.tickVal() > 0 ? b.tickVal() : 10.0;
				if (player.currentMp() < mpDrain) {
					removeSkill(id);
					if (packetSender != null) {
						packetSender.accept(SystemMessage.id(SystemMessage.SKILL_REMOVED_DUE_LACK_MP));
					}
					continue;
				}
				player.currentMp(player.currentMp() - mpDrain);
				player.currentHp(Math.min(player.maxHp(), player.currentHp() + (5.0 * Math.max(1, b.level()))));
			} else if (isFakeDeath || isSilentMove || b.tickVal() > 0) {
				double mpDrain = b.tickVal() > 0 ? b.tickVal() : (isFakeDeath ? 10.0 : 5.0);
				if (player.currentMp() < mpDrain) {
					removeSkill(id);
					if (packetSender != null) {
						packetSender.accept(SystemMessage.id(SystemMessage.SKILL_REMOVED_DUE_LACK_MP));
						if (isFakeDeath) {
							packetSender.accept(new GameServerPacket.ChangeWaitType(
									player.objectId(),
									3,
									player.x(), player.y(), player.z()));
						}
					}
				} else {
					player.currentMp(player.currentMp() - mpDrain);
				}
			}
		}
	}

	// =========================================================================
	// #170 - ImmobileBuff / ImmobilePetBuff / ImmobileUntilAttacked
	// =========================================================================

	public synchronized boolean isImmobilizedByBuff() {
		return hasSkill(368) || hasSkill(444) || hasSkill(1430)
				|| hasActiveEffectName("ImmobileBuff", "ImmobilePetBuff", "ImmobileUntilAttacked");
	}

	public synchronized boolean onPlayerMovedOrStoodUp() {
		boolean removed = removeSkill(1430);
		removed |= byStack.values().removeIf(b -> "ImmobileUntilAttacked".equalsIgnoreCase(b.effectName()));
		return removed;
	}

	public synchronized boolean onPlayerAttacked() {
		boolean removed = removeSkill(1430);
		removed |= byStack.values().removeIf(b -> "ImmobileUntilAttacked".equalsIgnoreCase(b.effectName()));
		return removed;
	}

	// =========================================================================
	// #171 & #172 - Bluff (358) & Fear / TurnFlee (0x00000010)
	// =========================================================================

	public static int computeBluffHeading(int casterHeading) {
		return (casterHeading + 32768) & 0xFFFF;
	}

	public void startFear(long durationMs, int casterX, int casterY) {
		this.afraidUntilMs = System.currentTimeMillis() + Math.max(0L, durationMs);
		this.fearCasterX = casterX;
		this.fearCasterY = casterY;
	}

	public void stopFear() {
		this.afraidUntilMs = 0L;
	}

	public boolean isAfraid() {
		return afraidUntilMs > System.currentTimeMillis()
				|| hasActiveEffectName("Fear", "TurnFlee")
				|| hasActiveStack("turn_flee", "fear");
	}

	/**
	 * #172: Calcula o destino de fuga (Fear) afastando o alvo das coordenadas do conjurador,
	 * validado via GeoEngine.moveCheck.
	 */
	public int[] computeFearDestination(PlayerCharacter target) {
		if (target == null) {
			return new int[] { 0, 0, 0 };
		}
		double dx = target.x() - fearCasterX;
		double dy = target.y() - fearCasterY;
		double dist = Math.hypot(dx, dy);
		double angle = dist < 1.0
				? (target.heading() * Math.PI / 32768.0)
				: Math.atan2(dy, dx);
		int rawX = target.x() + (int) Math.round(Math.cos(angle) * 300.0);
		int rawY = target.y() + (int) Math.round(Math.sin(angle) * 300.0);
		int rawZ = target.z();
		try {
			return GeoEngine.getInstance().moveCheck(target.x(), target.y(), target.z(), rawX, rawY, rawZ).toArray();
		} catch (Exception e) {
			return new int[] { rawX, rawY, rawZ };
		}
	}

	// =========================================================================
	// #173 - BlockBuff (Celestial Shield 1418) & BlockDebuff (Mystic Immunity 1411)
	// =========================================================================

	public void setBuffBlocked(boolean blocked) {
		this.explicitBuffBlocked = blocked;
	}

	public void setDebuffBlocked(boolean blocked) {
		this.explicitDebuffBlocked = blocked;
	}

	public synchronized boolean isBuffBlocked() {
		return explicitBuffBlocked || hasSkill(1418)
				|| hasActiveEffectName("BlockBuff", "PreventBuff", "CelestialShield")
				|| hasActiveStack("block_buff", "celestial_shield");
	}

	public synchronized boolean isDebuffBlocked() {
		return explicitDebuffBlocked || hasSkill(1411)
				|| hasActiveEffectName("BlockDebuff", "DebuffImmunity")
				|| hasActiveStack("block_debuff", "mystic_immunity");
	}

	// =========================================================================
	// #174 - TouchOfDeath (Skill 342)
	// =========================================================================

	public synchronized int applyTouchOfDeath(PlayerCharacter target, int magicLevel, double baseCancelRate) {
		int targetLvl = target != null ? target.level() : 78;
		int cancelled = cancelBuffs(5, magicLevel > 0 ? magicLevel : 78, targetLvl,
				baseCancelRate > 0 ? baseCancelRate : 25.0, 0.0, 0.0);
		if (target != null && target.currentCp() > 1.0) {
			target.currentCp(Math.max(1.0, target.currentCp() * 0.10));
		}
		long end = System.currentTimeMillis() + 120_000L;
		List<StatFunc> todFuncs = List.of(
				new StatFunc("maxCp", StatFunc.Op.MUL, 0x30, 0.10),
				new StatFunc("gainHp", StatFunc.Op.MUL, 0x30, 0.70),
				new StatFunc("debuffVuln", StatFunc.Op.ADD, 0x40, 30.0));
		put(ActiveBuff.ofSkill(342, 1, "touch_of_death", 1, end, todFuncs, true, 0, "TouchOfDeath", 0.0, 120));
		return cancelled;
	}

	// =========================================================================
	// #175 - ChanceSkillTrigger (Mirage 445, Counterattack 447, Trigger Skills)
	// =========================================================================

	public void registerChanceTrigger(int sourceSkillId, int triggeredSkillId, int triggeredLevel,
			String chanceType, double chance) {
		if (triggeredSkillId <= 0) {
			return;
		}
		chanceTriggers.put(sourceSkillId, new ChanceTrigger(
				sourceSkillId,
				triggeredSkillId,
				Math.max(1, triggeredLevel),
				chanceType != null ? chanceType : "ON_HIT",
				chance > 0 ? chance : 15.0));
	}

	public void unregisterChanceTrigger(int sourceSkillId) {
		chanceTriggers.remove(sourceSkillId);
	}

	public List<ChanceTrigger> rollTriggeredSkills(String eventType) {
		if (eventType == null || chanceTriggers.isEmpty()) {
			return List.of();
		}
		List<ChanceTrigger> triggered = new ArrayList<>();
		for (ChanceTrigger ct : chanceTriggers.values()) {
			if (ct.chanceType().equalsIgnoreCase(eventType) || "ANY".equalsIgnoreCase(ct.chanceType())) {
				if (ThreadLocalRandom.current().nextDouble(100.0) < ct.chance()) {
					triggered.add(ct);
				}
			}
		}
		return triggered;
	}

	// =========================================================================
	// #181 - TransferDam (Transfer Pain 1262)
	// =========================================================================

	public synchronized double getTransferDamagePercent() {
		double percent = 0.0;
		for (StatFunc f : funcs()) {
			if ("transDam".equalsIgnoreCase(f.stat())) {
				percent += f.value();
			}
		}
		int tpLvl = getSkillLevel(1262);
		if (tpLvl > 0) {
			percent = Math.max(percent, tpLvl * 10.0);
		}
		return Math.min(50.0, Math.max(0.0, percent));
	}

	/**
	 * Remove buffs positivos por IDs de skill, stackType ou atributo modificado em StatFunc.
	 * Usado por Warrior Bane, Mage Bane, Block Shield e Block Wind Walk.
	 */
	public synchronized boolean removeBuffsByIdsOrStackOrStats(java.util.Set<Integer> skillIds,
			java.util.Set<String> stackKeywords, java.util.Set<String> statNames) {
		long now = System.currentTimeMillis();
		List<ActiveBuff> matching = new ArrayList<>();
		for (ActiveBuff b : byStack.values()) {
			if (b.isDebuff() || b.isPermanent() || b.endTimeMillis() <= now) {
				continue;
			}
			boolean hit = false;
			if (skillIds != null && skillIds.contains(b.skillId())) {
				hit = true;
			}
			if (!hit && stackKeywords != null && b.stackType() != null) {
				String st = b.stackType().toLowerCase(java.util.Locale.ROOT);
				for (String kw : stackKeywords) {
					if (st.contains(kw.toLowerCase(java.util.Locale.ROOT))) {
						hit = true;
						break;
					}
				}
			}
			if (!hit && statNames != null) {
				if (statNames.contains("runSpd") && b.runSpdAdd() > 0) {
					hit = true;
				} else if (statNames.contains("pAtkSpd") && b.pAtkSpdMul() > 1.0) {
					hit = true;
				} else if (statNames.contains("mAtkSpd") && b.mAtkSpdMul() > 1.0) {
					hit = true;
				} else if (b.funcs() != null) {
					for (StatFunc f : b.funcs()) {
						for (String s : statNames) {
							if (s.equalsIgnoreCase(f.stat())) {
								hit = true;
								break;
							}
						}
						if (hit) {
							break;
						}
					}
				}
			}
			if (hit) {
				matching.add(b);
			}
		}
		boolean anyRemoved = false;
		for (ActiveBuff b : matching) {
			shadowedByHerb.remove(b.stackType());
			if (remove(b)) {
				anyRemoved = true;
			}
		}
		return anyRemoved;
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
		shadowedByHerb.clear();
		chanceTriggers.clear();
		explicitBuffBlocked = false;
		explicitDebuffBlocked = false;
		afraidUntilMs = 0L;
	}

	public synchronized void clearDebuffs() {
		byStack.values().removeIf(ActiveBuff::isDebuff);
		afraidUntilMs = 0L;
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

	public void addEffect(int skillId, int level, long durationMs, String stackType, boolean isDebuff) {
		long end = durationMs == PERMANENT ? PERMANENT : (System.currentTimeMillis() + durationMs);
		String st = (stackType != null && !stackType.isBlank()) ? stackType : ("skill_" + skillId);
		put(new ActiveBuff(skillId, level, st, level, end, 0, 1.0, 1.0, 0, List.of(), isDebuff, 0));
	}

	public synchronized List<ActiveBuff> active() {
		long now = System.currentTimeMillis();
		purgeExpiredAndRestoreShadowed(now);
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
