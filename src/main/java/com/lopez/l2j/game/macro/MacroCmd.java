package com.lopez.l2j.game.macro;

/**
 * Comando individual de um macro do jogador.
 *
 * @param entry Indice de execucao (1 a 12)
 * @param type Tipo do comando: 1=Skill, 3=Action, 4=Shortcut
 * @param d1 Primeiro parametro numerico (ex: skillId)
 * @param d2 Segundo parametro numerico (ex: pagina de shortcut)
 * @param cmd Texto do comando (ex: "/target mob", chat ou nome da skill)
 */
public record MacroCmd(int entry, int type, int d1, int d2, String cmd) {

	public static final int TYPE_SKILL = 1;
	public static final int TYPE_ACTION = 3;
	public static final int TYPE_SHORTCUT = 4;

	public MacroCmd {
		if (cmd == null) {
			cmd = "";
		}
	}
}
