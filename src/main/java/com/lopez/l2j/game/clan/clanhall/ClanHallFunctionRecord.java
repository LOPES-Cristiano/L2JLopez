package com.lopez.l2j.game.clan.clanhall;

/**
 * Representa uma funcao ativa de um Clan Hall (clanhall_functions table do L2JDream).
 */
public record ClanHallFunctionRecord(
		int hallId,
		int type,
		int level,
		int lease,
		long rate,
		long endTime
) {
	public static final int FUNC_RESTORE_HP = 1;
	public static final int FUNC_RESTORE_MP = 2;
	public static final int FUNC_RESTORE_EXP = 3;
	public static final int FUNC_TELEPORT = 4;
	public static final int FUNC_SUPPORT = 5;
	public static final int FUNC_DECO_CURTAINS = 6;
	public static final int FUNC_ITEM_CREATE = 7;
	public static final int FUNC_DECO_FRONTPLATEFORM = 8;
}
