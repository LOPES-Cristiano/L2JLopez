package com.lopez.l2j.game.clan.skill;

/**
 * Registro de uma habilidade de cla da arvore oficial (pledge_skill_tree.xml do L2JDream).
 */
public record PledgeSkillRecord(
		int skillId,
		int level,
		String name,
		int minClanLevel,
		int repCost,
		int itemId
) {}
