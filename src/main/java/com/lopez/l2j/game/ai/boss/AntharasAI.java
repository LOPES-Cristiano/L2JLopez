package com.lopez.l2j.game.ai.boss;

import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcSkillTable;
import com.lopez.l2j.game.skill.SkillTable;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Inteligencia Artificial especifica de Antharas (ai.Antharas do Lucera2 / L2JDream).
 * Executa fases de regeneracao conforme HP decresce (4239, 4240, 4241),
 * rajadas de sopro de terra (4110), chuva de meteoros (5093), medo (4108, 5092),
 * paralisia (4111), maldicao (4109) e choque sísmico (4106, 4107).
 */
public class AntharasAI extends RaidBossAI {

	public static final int SKILL_FEAR = 4108;
	public static final int SKILL_FEAR2 = 5092;
	public static final int SKILL_CURSE = 4109;
	public static final int SKILL_PARALYZE = 4111;
	public static final int SKILL_SHOCK = 4106;
	public static final int SKILL_SHOCK2 = 4107;
	public static final int SKILL_BREATH = 4110;
	public static final int SKILL_METEOR = 5093;
	public static final int SKILL_REGEN1 = 4239;
	public static final int SKILL_REGEN2 = 4240;
	public static final int SKILL_REGEN3 = 4241;

	private int hpPhase = 0;

	public AntharasAI(NpcInstance npc, GameWorld world, CombatService combatService,
			CharTemplateTable charTemplates, NpcSkillTable npcSkillTable, SkillTable skillTable) {
		super(npc, world, combatService, charTemplates, npcSkillTable, skillTable);
	}

	@Override
	public void processCombat() {
		if (npc.isDead()) return;

		double hpPercent = (npc.currentHp() / npc.maxHp()) * 100.0;
		checkHpPhaseBuff(hpPercent);

		super.processCombat();
	}

	private void checkHpPhaseBuff(double hpPercent) {
		if (hpPhase == 0) {
			applyBuff(SKILL_REGEN1, 1);
			hpPhase = 1;
		} else if (hpPercent < 75.0 && hpPhase == 1) {
			applyBuff(SKILL_REGEN2, 1);
			hpPhase = 2;
		} else if (hpPercent < 50.0 && hpPhase == 2) {
			applyBuff(SKILL_REGEN3, 1);
			hpPhase = 3;
		} else if (hpPercent < 30.0 && hpPhase == 3) {
			applyBuff(SKILL_REGEN3, 1);
			hpPhase = 4;
		}
	}

	private void applyBuff(int skillId, int level) {
		if (skillTable != null) {
			skillTable.get(skillId, level).ifPresent(sk -> {
				npc.currentHp(Math.min(npc.maxHp(), npc.currentHp() + (npc.maxHp() * 0.05)));
			});
		}
	}

	@Override
	protected SkillTemplate chooseMonsterSkill() {
		if (skillTable == null) return null;

		int roll = ThreadLocalRandom.current().nextInt(100);
		int skillId = switch (hpPhase) {
			case 1 -> roll < 35 ? SKILL_METEOR : (roll < 65 ? SKILL_CURSE : SKILL_PARALYZE);
			case 2 -> roll < 30 ? SKILL_METEOR : (roll < 55 ? SKILL_FEAR2 : (roll < 75 ? SKILL_BREATH : SKILL_PARALYZE));
			case 3 -> roll < 25 ? SKILL_METEOR : (roll < 45 ? SKILL_SHOCK2 : (roll < 65 ? SKILL_BREATH : SKILL_FEAR2));
			default -> roll < 25 ? SKILL_METEOR : (roll < 50 ? SKILL_BREATH : (roll < 70 ? SKILL_SHOCK : SKILL_FEAR));
		};

		return skillTable.get(skillId, 1).orElseGet(super::chooseMonsterSkill);
	}
}
