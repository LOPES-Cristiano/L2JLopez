package com.lopez.l2j.game.npc;

/**
 * Template imutavel de NPC / Monstro / Guard / Merchant / Teleporter, unificando os dados da tabela {@code npc}.
 */
public record NpcTemplate(
		int id,
		int idTemplate,
		String name,
		boolean serverSideName,
		String title,
		boolean serverSideTitle,
		double collisionRadius,
		double collisionHeight,
		int level,
		String sex,
		String type,
		int attackRange,
		int maxHp,
		int maxMp,
		int pAtk,
		int pDef,
		int mAtk,
		int mDef,
		int pAtkSpd,
		int mAtkSpd,
		int rhand,
		int lhand,
		int armor,
		int walkSpd,
		int runSpd,
		int aggroRange,
		boolean isUndead) {

	public NpcTemplate {
		name = name == null ? "" : name;
		title = title == null ? "" : title;
		type = type == null ? "L2Npc" : type;
		sex = sex == null ? "male" : sex;
	}

	public boolean isMonster() {
		return "L2Monster".equalsIgnoreCase(type) || "L2RaidBoss".equalsIgnoreCase(type)
				|| "L2GrandBoss".equalsIgnoreCase(type);
	}

	public boolean isAttackable() {
		return isMonster();
	}
}
