package com.lopez.l2j.game.skill;

import java.util.List;
import java.util.Set;

/**
 * Definicao imutavel de um skill num nivel (porta enxuta de L2Skill, lida de data/xml/stats/skills).
 *
 * @param funcs   funcoes diretas do {@code <for>} (passivas: bonus permanentes enquanto o skill e conhecido)
 * @param effects efeitos do {@code <for>} (buffs, debuffs, curas ao longo do tempo...)
 * @param castCondition condicao de uso ({@code <cond>}), null = sem restricao conhecida
 */
public record SkillTemplate(int id, int level, String name, OperateType operateType, String skillType,
		String target, boolean magic, int mpConsume, int mpInitialConsume, int hpConsume, double power,
		int castRange, int skillRadius, int hitTime, int coolTime, int reuseDelay, int magicLevel,
		double absorbPart, boolean nextActionAttack, List<StatFunc> funcs, List<EffectTemplate> effects,
		SkillCondition castCondition, String condMsg) {

	public enum OperateType {
		ACTIVE, PASSIVE, TOGGLE
	}

	/**
	 * Efeito de um skill ({@code <effect name="Buff" count="1" time="1200" ...>}).
	 *
	 * @param count  numero de ciclos
	 * @param period duracao de cada ciclo em segundos (duracao total = count * period)
	 * @param val    valor por tick (HealOverTime etc.)
	 */
	public record EffectTemplate(String name, int count, int period, double val, String stackType,
			double stackOrder, List<StatFunc> funcs) {

		public long durationMs() {
			return (long) Math.max(1, count) * Math.max(0, period) * 1000L;
		}
	}

	private static final Set<String> OFFENSIVE = Set.of("PDAM", "MDAM", "BLOW", "CHARGEDAM", "FATALCOUNTER", "DRAIN",
			"DEATHLINK", "MANADAM", "DOT", "MDOT", "POISON", "BLEED", "DEBUFF", "STUN", "SLEEP", "ROOT", "PARALYZE",
			"MUTE", "FEAR", "CONFUSION", "WEAKNESS", "AGGDEBUFF", "AGGDAMAGE", "CANCEL", "MAGE_BANE", "WARRIOR_BANE",
			"SPOIL", "ERASE", "BETRAY", "DISARM", "CONFUSE_MOB_ONLY", "DRAIN_SOUL", "AGGREDUCE", "AGGREDUCE_CHAR",
			"AGGREMOVE", "STEAL_BUFF", "SWITCH", "FATAL", "SOW", "HARVEST");

	private static final Set<String> PHYSICAL_DAMAGE = Set.of("PDAM", "BLOW", "CHARGEDAM", "FATALCOUNTER");
	private static final Set<String> MAGIC_DAMAGE = Set.of("MDAM", "DEATHLINK", "DRAIN", "MANADAM");

	public boolean isPassive() {
		return operateType == OperateType.PASSIVE;
	}

	public boolean isToggle() {
		return operateType == OperateType.TOGGLE;
	}

	public boolean isOffensive() {
		return OFFENSIVE.contains(skillType);
	}

	public boolean isPhysicalDamage() {
		return PHYSICAL_DAMAGE.contains(skillType);
	}

	public boolean isMagicDamage() {
		return MAGIC_DAMAGE.contains(skillType);
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
