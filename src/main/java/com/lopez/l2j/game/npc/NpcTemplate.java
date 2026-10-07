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
		boolean isUndead,
		long exp,
		int sp,
		String factionId,
		int factionRange) {

	public NpcTemplate(
			int id, int idTemplate, String name, boolean serverSideName, String title,
			boolean serverSideTitle, double collisionRadius, double collisionHeight, int level,
			String sex, String type, int attackRange, int maxHp, int maxMp, int pAtk,
			int pDef, int mAtk, int mDef, int pAtkSpd, int mAtkSpd, int rhand,
			int lhand, int armor, int walkSpd, int runSpd, int aggroRange, boolean isUndead) {
		this(id, idTemplate, name, serverSideName, title, serverSideTitle, collisionRadius,
				collisionHeight, level, sex, type, attackRange, maxHp, maxMp, pAtk, pDef,
				mAtk, mDef, pAtkSpd, mAtkSpd, rhand, lhand, armor, walkSpd, runSpd,
				aggroRange, isUndead, 0L, 0, null, 0);
	}

	public NpcTemplate(
			int id, int idTemplate, String name, boolean serverSideName, String title,
			boolean serverSideTitle, double collisionRadius, double collisionHeight, int level,
			String sex, String type, int attackRange, int maxHp, int maxMp, int pAtk,
			int pDef, int mAtk, int mDef, int pAtkSpd, int mAtkSpd, int rhand,
			int lhand, int armor, int walkSpd, int runSpd, int aggroRange, boolean isUndead,
			long exp, int sp) {
		this(id, idTemplate, name, serverSideName, title, serverSideTitle, collisionRadius,
				collisionHeight, level, sex, type, attackRange, maxHp, maxMp, pAtk, pDef,
				mAtk, mDef, pAtkSpd, mAtkSpd, rhand, lhand, armor, walkSpd, runSpd,
				aggroRange, isUndead, exp, sp, null, 0);
	}

	public NpcTemplate(
			int id, int idTemplate, String name, boolean serverSideName, String title,
			boolean serverSideTitle, double collisionRadius, double collisionHeight, int level,
			String sex, String type, int attackRange, int maxHp, int maxMp, int pAtk,
			int pDef, int mAtk, int mDef, int pAtkSpd, int mAtkSpd, int rhand,
			int lhand, int armor, int walkSpd, int runSpd, int aggroRange, boolean isUndead,
			String factionId, int factionRange) {
		this(id, idTemplate, name, serverSideName, title, serverSideTitle, collisionRadius,
				collisionHeight, level, sex, type, attackRange, maxHp, maxMp, pAtk, pDef,
				mAtk, mDef, pAtkSpd, mAtkSpd, rhand, lhand, armor, walkSpd, runSpd,
				aggroRange, isUndead, 0L, 0, factionId, factionRange);
	}

	public NpcTemplate {
		name = name == null ? "" : name;
		title = title == null ? "" : title;
		type = type == null ? "L2Npc" : type;
		sex = sex == null ? "male" : sex;
		factionId = factionId == null || factionId.isBlank() ? null : factionId.trim();
	}

	public boolean isMonster() {
		if (type == null) {
			return false;
		}
		String clean = type.toLowerCase(java.util.Locale.ROOT);
		return clean.contains("monster")
				|| clean.contains("boss")
				|| clean.contains("minion")
				|| clean.contains("chest")
				|| clean.contains("beast")
				|| clean.contains("invader")
				|| clean.contains("angel")
				|| clean.contains("squash")
				|| clean.contains("siegeguard")
				|| clean.contains("commander")
				|| clean.contains("friendlymob")
				|| clean.contains("tower")
				|| clean.contains("larva");
	}

	public boolean isRaidBoss() {
		return type != null && ("L2RaidBoss".equalsIgnoreCase(type) || "L2GrandBoss".equalsIgnoreCase(type)
				|| "L2FrintezzaBoss".equalsIgnoreCase(type));
	}

	public boolean isGrandBoss() {
		return type != null && ("L2GrandBoss".equalsIgnoreCase(type) || "L2FrintezzaBoss".equalsIgnoreCase(type));
	}

	public boolean isMinion() {
		return type != null && "L2Minion".equalsIgnoreCase(type);
	}

	public boolean isGuard() {
		return type != null && type.toLowerCase(java.util.Locale.ROOT).contains("guard");
	}

	public boolean isAttackable() {
		return isMonster() || isGuard();
	}

	public static NpcTemplate fallback(int id, String name, String type) {
		return new NpcTemplate(
				id, id, name, true, "", false,
				9.0, 24.0, 70, "male", type, 40,
				2000, 1000, 150, 150, 150, 150, 250, 333,
				0, 0, 0, 50, 120, 0, false, 0L, 0);
	}
}
