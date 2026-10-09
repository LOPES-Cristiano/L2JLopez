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
 * Inteligencia Artificial especifica de Valakas (ai.Valakas do Lucera2 / L2JDream).
 * Executa Lava Skin (4680), regeneracao progressiva por porcentagem de vida (4691),
 * pisoteios de cauda e pata (4681, 4682, 4685, 4688), sopro de fogo de curto e longo alcance (4683, 4684),
 * chuva de meteoros ardentes (4690), medo do dragao (4689) e retalicao a ataques a distancia.
 */
public class ValakasAI extends RaidBossAI {

	public static final int SKILL_LAVA_SKIN = 4680;
	public static final int SKILL_FEAR = 4689;
	public static final int SKILL_REGEN = 4691;
	public static final int SKILL_TREMPLE_LEFT = 4681;
	public static final int SKILL_TREMPLE_RIGHT = 4682;
	public static final int SKILL_TAIL_STOMP = 4685;
	public static final int SKILL_TAIL_LASH = 4688;
	public static final int SKILL_METEOR = 4690;
	public static final int SKILL_BREATH_LOW = 4683;
	public static final int SKILL_BREATH_HIGH = 4684;

	private int hpPhase = 0;

	public ValakasAI(NpcInstance npc, GameWorld world, CombatService combatService,
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
			applyBuff(SKILL_LAVA_SKIN, 1);
			applyBuff(SKILL_REGEN, 1);
			hpPhase = 1;
		} else if (hpPercent < 80.0 && hpPhase == 1) {
			applyBuff(SKILL_REGEN, 2);
			hpPhase = 2;
		} else if (hpPercent < 50.0 && hpPhase == 2) {
			applyBuff(SKILL_REGEN, 3);
			hpPhase = 3;
		} else if (hpPercent < 30.0 && hpPhase == 3) {
			applyBuff(SKILL_REGEN, 4);
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
		int skillId;

		if (hpPhase <= 1) {
			skillId = roll < 30 ? SKILL_BREATH_LOW : (roll < 60 ? SKILL_TAIL_STOMP : (roll < 85 ? SKILL_METEOR : SKILL_FEAR));
		} else if (hpPhase <= 3) {
			skillId = roll < 20 ? SKILL_BREATH_LOW : (roll < 40 ? SKILL_BREATH_HIGH : (roll < 60 ? SKILL_TAIL_LASH : (roll < 80 ? SKILL_METEOR : SKILL_FEAR)));
		} else {
			// Phase 4: Enraged! Meteor e sopros em alta frequencia
			skillId = roll < 35 ? SKILL_METEOR : (roll < 60 ? SKILL_BREATH_HIGH : (roll < 80 ? SKILL_TAIL_LASH : SKILL_FEAR));
		}

		return skillTable.get(skillId, 1).orElseGet(super::chooseMonsterSkill);
	}
}
