package com.lopez.l2j.game.shortcut;

/**
 * Representa um atalho da barra rapida (F1-F12 em multiplas paginas).
 *
 * @param slot Indice do slot na pagina (0 a 11)
 * @param page Pagina da barra (0 a 9)
 * @param type Tipo: 1=Item, 2=Skill, 3=Action, 4=Macro, 5=Recipe
 * @param id ID do elemento (objectId do item, skillId, actionId, macroId)
 * @param level Nivel (para skills) ou -1
 * @param characterType Tipo de personagem (1=Player)
 */
public record ShortCut(
		int slot,
		int page,
		int type,
		int id,
		int level,
		int characterType) {

	public static final int TYPE_ITEM = 1;
	public static final int TYPE_SKILL = 2;
	public static final int TYPE_ACTION = 3;
	public static final int TYPE_MACRO = 4;
	public static final int TYPE_RECIPE = 5;

	public int globalSlot() {
		return slot + page * 12;
	}
}
