package com.lopez.l2j.game.skill;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Definicao imutavel de um skill num nivel (porta completa de L2Skill, lida de data/xml/stats/skills).
 *
 * @param funcs         funcoes diretas do {@code <for>} (passivas: bonus permanentes enquanto o skill e conhecido)
 * @param effects       efeitos do {@code <for>} (buffs, debuffs, curas ao longo do tempo...)
 * @param castCondition condicao de uso ({@code <cond>}), null = sem restricao conhecida
 */
public record SkillTemplate(
		int id,
		int level,
		int displayId,
		String name,
		OperateType operateType,
		String skillType,
		String target,
		boolean magic,
		int mpConsume,
		int mpInitialConsume,
		int hpConsume,
		double power,
		int castRange,
		int skillRadius,
		int hitTime,
		int coolTime,
		int reuseDelay,
		int magicLevel,
		double absorbPart,
		boolean nextActionAttack,
		int itemConsumeId,
		int itemConsumeCount,
		int giveCharges,
		int maxCharges,
		int needCharges,
		boolean consumeCharges,
		boolean continueAfterMax,
		boolean staticHitTime,
		boolean staticReuse,
		boolean isDance,
		boolean isPotion,
		boolean ignoreShld,
		boolean overHit,
		int baseCritRate,
		int lethal1,
		int lethal2,
		int numCharges,
		int triggerId,
		int triggerLevel,
		String chanceType,
		int triggeredId,
		int triggeredLevel,
		int npcId,
		String element,
		String saveVs,
		int lvlDepend,
		double ssBoost,
		String effectType,
		double effectPower,
		List<StatFunc> funcs,
		List<EffectTemplate> effects,
		SkillCondition castCondition,
		String condMsg,
		List<SkillCondition> conditions,
		Map<Integer, Integer> enchantRoutes) {

	public SkillTemplate {
		funcs = funcs != null ? List.copyOf(funcs) : List.of();
		effects = effects != null ? List.copyOf(effects) : List.of();
		if (conditions == null) {
			conditions = castCondition != null ? List.of(castCondition) : List.of();
		} else {
			conditions = List.copyOf(conditions);
		}
		enchantRoutes = enchantRoutes != null ? Map.copyOf(enchantRoutes) : Map.of();
	}

	public SkillTemplate(int id, int level, int displayId, String name, OperateType operateType, String skillType,
			String target, boolean magic, int mpConsume, int mpInitialConsume, int hpConsume, double power,
			int castRange, int skillRadius, int hitTime, int coolTime, int reuseDelay, int magicLevel,
			double absorbPart, boolean nextActionAttack, int itemConsumeId, int itemConsumeCount,
			int giveCharges, int maxCharges, int needCharges, boolean consumeCharges, boolean continueAfterMax,
			boolean staticHitTime, boolean staticReuse,
			List<StatFunc> funcs, List<EffectTemplate> effects,
			SkillCondition castCondition, String condMsg) {
		this(id, level, displayId, name, operateType, skillType, target, magic, mpConsume, mpInitialConsume, hpConsume, power,
				castRange, skillRadius, hitTime, coolTime, reuseDelay, magicLevel, absorbPart, nextActionAttack,
				itemConsumeId, itemConsumeCount, giveCharges, maxCharges, needCharges, consumeCharges, continueAfterMax,
				staticHitTime, staticReuse,
				computeDefaultDance(id, name), "POTION".equalsIgnoreCase(skillType),
				false, false, 0, 0, 0, Math.max(giveCharges, needCharges),
				0, 0, null, 0, 0, 0, null, null, 0, 0.0, null, 0.0,
				funcs, effects, castCondition, condMsg,
				castCondition != null ? List.of(castCondition) : List.of(), Map.of());
	}

	public SkillTemplate(int id, int level, String name, OperateType operateType, String skillType,
			String target, boolean magic, int mpConsume, int mpInitialConsume, int hpConsume, double power,
			int castRange, int skillRadius, int hitTime, int coolTime, int reuseDelay, int magicLevel,
			double absorbPart, boolean nextActionAttack, int itemConsumeId, int itemConsumeCount,
			int giveCharges, int maxCharges, int needCharges, boolean consumeCharges, boolean continueAfterMax,
			List<StatFunc> funcs, List<EffectTemplate> effects,
			SkillCondition castCondition, String condMsg) {
		this(id, level, id, name, operateType, skillType, target, magic, mpConsume, mpInitialConsume, hpConsume, power,
				castRange, skillRadius, hitTime, coolTime, reuseDelay, magicLevel, absorbPart, nextActionAttack,
				itemConsumeId, itemConsumeCount, giveCharges, maxCharges, needCharges, consumeCharges, continueAfterMax,
				false, false, funcs, effects, castCondition, condMsg);
	}

	public SkillTemplate(int id, int level, String name, OperateType operateType, String skillType,
			String target, boolean magic, int mpConsume, int mpInitialConsume, int hpConsume, double power,
			int castRange, int skillRadius, int hitTime, int coolTime, int reuseDelay, int magicLevel,
			double absorbPart, boolean nextActionAttack, int itemConsumeId, int itemConsumeCount,
			int giveCharges, int maxCharges, int needCharges, boolean consumeCharges,
			List<StatFunc> funcs, List<EffectTemplate> effects,
			SkillCondition castCondition, String condMsg) {
		this(id, level, id, name, operateType, skillType, target, magic, mpConsume, mpInitialConsume, hpConsume, power,
				castRange, skillRadius, hitTime, coolTime, reuseDelay, magicLevel, absorbPart, nextActionAttack,
				itemConsumeId, itemConsumeCount, giveCharges, maxCharges, needCharges, consumeCharges, false,
				false, false, funcs, effects, castCondition, condMsg);
	}

	public SkillTemplate(int id, int level, String name, OperateType operateType, String skillType,
			String target, boolean magic, int mpConsume, int mpInitialConsume, int hpConsume, double power,
			int castRange, int skillRadius, int hitTime, int coolTime, int reuseDelay, int magicLevel,
			double absorbPart, boolean nextActionAttack, int itemConsumeId, int itemConsumeCount,
			List<StatFunc> funcs, List<EffectTemplate> effects,
			SkillCondition castCondition, String condMsg) {
		this(id, level, id, name, operateType, skillType, target, magic, mpConsume, mpInitialConsume, hpConsume, power,
				castRange, skillRadius, hitTime, coolTime, reuseDelay, magicLevel, absorbPart, nextActionAttack,
				itemConsumeId, itemConsumeCount, 0, 0, 0, true, false, false, false, funcs, effects, castCondition, condMsg);
	}

	public SkillTemplate(int id, int level, String name, OperateType operateType, String skillType,
			String target, boolean magic, int mpConsume, int mpInitialConsume, int hpConsume, double power,
			int castRange, int skillRadius, int hitTime, int coolTime, int reuseDelay, int magicLevel,
			double absorbPart, boolean nextActionAttack, List<StatFunc> funcs, List<EffectTemplate> effects,
			SkillCondition castCondition, String condMsg) {
		this(id, level, id, name, operateType, skillType, target, magic, mpConsume, mpInitialConsume, hpConsume, power,
				castRange, skillRadius, hitTime, coolTime, reuseDelay, magicLevel, absorbPart, nextActionAttack,
				0, 0, 0, 0, 0, true, false, false, false, funcs, effects, castCondition, condMsg);
	}

	public enum OperateType {
		ACTIVE, PASSIVE, TOGGLE
	}

	/**
	 * Efeito de um skill ({@code <effect name="Buff" count="1" time="1200" ...>}).
	 *
	 * @param count        numero de ciclos
	 * @param period       duracao de cada ciclo em segundos (duracao total = count * period)
	 * @param val          valor por tick (HealOverTime etc.)
	 * @param abnormalMask bitmask visual do protocolo Interlude (AbnormalEffect)
	 */
	public record EffectTemplate(
			String name,
			int count,
			int period,
			double val,
			String stackType,
			double stackOrder,
			int abnormalMask,
			String abnormal,
			double effectPower,
			String effectType,
			SkillCondition condition,
			List<StatFunc> funcs) {

		public EffectTemplate {
			funcs = funcs != null ? List.copyOf(funcs) : List.of();
		}

		public EffectTemplate(String name, int count, int period, double val, String stackType,
				double stackOrder, int abnormalMask, List<StatFunc> funcs) {
			this(name, count, period, val, stackType, stackOrder, abnormalMask,
					null, 0.0, null, null, funcs);
		}

		public EffectTemplate(String name, int count, int period, double val, String stackType,
				double stackOrder, List<StatFunc> funcs) {
			this(name, count, period, val, stackType, stackOrder,
					AbnormalEffect.parseMask(null, name), null, 0.0, null, null, funcs);
		}

		public int time() {
			return period;
		}

		public long durationMs() {
			return (long) Math.max(1, count) * Math.max(0, period) * 1000L;
		}
	}

	private static final Set<String> OFFENSIVE = Set.of("PDAM", "MDAM", "BLOW", "CHARGEDAM", "FATALCOUNTER", "DRAIN",
			"DEATHLINK", "MANADAM", "DOT", "MDOT", "POISON", "BLEED", "DEBUFF", "STUN", "SLEEP", "ROOT", "PARALYZE",
			"MUTE", "FEAR", "CONFUSION", "WEAKNESS", "AGGDEBUFF", "AGGDAMAGE", "CANCEL", "MAGE_BANE", "WARRIOR_BANE",
			"SPOIL", "ERASE", "BETRAY", "DISARM", "CONFUSE_MOB_ONLY", "DRAIN_SOUL", "AGGREDUCE", "AGGREDUCE_CHAR",
			"AGGREMOVE", "STEAL_BUFF", "SWITCH", "FATAL", "CPDAM", "DELUXE_KEY_UNLOCK", "UNLOCK");

	private static final Set<String> PHYSICAL_DAMAGE = Set.of("PDAM", "BLOW", "CHARGEDAM", "FATALCOUNTER", "FATAL", "STRSIEGEASSAULT");
	private static final Set<String> MAGIC_DAMAGE = Set.of("MDAM", "DEATHLINK", "DRAIN");

	private static boolean computeDefaultDance(int id, String name) {
		if ((id >= 264 && id <= 277) || (id >= 304 && id <= 311) || id == 349 || (id >= 363 && id <= 367)) {
			return true;
		}
		if (name != null) {
			String lower = name.toLowerCase(java.util.Locale.ROOT);
			return lower.startsWith("song of ") || lower.startsWith("dance of ") || lower.contains("siren's dance");
		}
		return false;
	}

	public boolean isPassive() {
		return operateType == OperateType.PASSIVE;
	}

	public boolean isToggle() {
		return operateType == OperateType.TOGGLE;
	}

	public boolean isMagic() {
		return magic;
	}

	public boolean isOffensive() {
		return OFFENSIVE.contains(skillType);
	}

	public boolean isPhysicalDamage() {
		return PHYSICAL_DAMAGE.contains(skillType);
	}

	public boolean isChargedDam() {
		return "CHARGEDAM".equalsIgnoreCase(skillType);
	}

	public boolean isMagicDamage() {
		return MAGIC_DAMAGE.contains(skillType);
	}

	public boolean isManaBurn() {
		return "MANADAM".equalsIgnoreCase(skillType);
	}

	public boolean isCpDamage() {
		return "CPDAM".equalsIgnoreCase(skillType);
	}

	public boolean isHeal() {
		return "HEAL".equalsIgnoreCase(skillType) || "HEAL_PERCENT".equalsIgnoreCase(skillType)
				|| "HOT".equalsIgnoreCase(skillType);
	}

	public boolean isBuff() {
		return "BUFF".equalsIgnoreCase(skillType);
	}

	public boolean isDebuff() {
		return "DEBUFF".equalsIgnoreCase(skillType) || "STUN".equalsIgnoreCase(skillType)
				|| "ROOT".equalsIgnoreCase(skillType) || "SLEEP".equalsIgnoreCase(skillType)
				|| "PARALYZE".equalsIgnoreCase(skillType) || "MUTE".equalsIgnoreCase(skillType)
				|| "FEAR".equalsIgnoreCase(skillType) || "CONFUSION".equalsIgnoreCase(skillType)
				|| "WEAKNESS".equalsIgnoreCase(skillType) || "POISON".equalsIgnoreCase(skillType)
				|| "BLEED".equalsIgnoreCase(skillType) || "DOT".equalsIgnoreCase(skillType)
				|| "MDOT".equalsIgnoreCase(skillType) || "AGGDEBUFF".equalsIgnoreCase(skillType)
				|| "CONT".equalsIgnoreCase(skillType);
	}

	public boolean isDanceOrSong() {
		return isDance || computeDefaultDance(id, name);
	}

	public boolean isAreaAroundSelf() {
		return target.equals("TARGET_AURA") || target.equals("TARGET_FRONT_AURA")
				|| target.equals("TARGET_BEHIND_AURA");
	}

	public boolean isAreaAroundTarget() {
		return target.equals("TARGET_AREA") || target.equals("TARGET_FRONT_AREA")
				|| target.equals("TARGET_BEHIND_AREA") || target.equals("TARGET_MULTIFACE");
	}

	/** Skills de "si mesmo/grupo": sem party/clan ainda, todos caem no proprio jogador. */
	public boolean isSelfTargeted() {
		return switch (target) {
			case "TARGET_SELF", "TARGET_PARTY", "TARGET_CLAN", "TARGET_ALLY", "TARGET_CORPSE_ALLY",
					"TARGET_CORPSE_CLAN", "TARGET_GROUND" -> true;
			default -> false;
		};
	}
}
