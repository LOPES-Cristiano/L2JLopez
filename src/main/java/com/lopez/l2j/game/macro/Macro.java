package com.lopez.l2j.game.macro;

import java.util.List;

/**
 * Representa um macro do jogador (Interlude C6).
 *
 * @param id ID unico do macro (inicia a partir de 1000)
 * @param icon Indice do icone do macro (0 a 6)
 * @param name Nome do macro (max 40 caracteres)
 * @param descr Descricao do macro (max 32 caracteres)
 * @param acronym Sigla de 4 letras que aparece no atalho
 * @param commands Linhas de comando (max 12)
 */
public record Macro(
		int id,
		int icon,
		String name,
		String descr,
		String acronym,
		List<MacroCmd> commands) {

	public Macro {
		name = name != null ? name : "";
		descr = descr != null ? descr : "";
		acronym = acronym != null ? acronym : "";
		commands = commands != null ? List.copyOf(commands) : List.of();
	}

	public Macro withId(int newId) {
		return new Macro(newId, icon, name, descr, acronym, commands);
	}

	/**
	 * Verifica se algum comando do macro viola Config.CHECK_PLAYER_MACRO / Config.MACRO_RESTRICTED_COMMAND_LIST.
	 */
	public boolean isValidMacro() {
		if (!com.lopez.l2j.config.Config.CHECK_PLAYER_MACRO) {
			return true;
		}
		for (MacroCmd cmd : commands) {
			if (cmd != null && isRestrictedCommand(cmd.command())) {
				return false;
			}
		}
		return true;
	}

	public Macro sanitized() {
		if (!com.lopez.l2j.config.Config.CHECK_PLAYER_MACRO) {
			return this;
		}
		List<MacroCmd> filtered = commands.stream()
				.filter(c -> c != null && !isRestrictedCommand(c.command()))
				.toList();
		return new Macro(id, icon, name, descr, acronym, filtered);
	}

	public static boolean isRestrictedCommand(String commandText) {
		if (!com.lopez.l2j.config.Config.CHECK_PLAYER_MACRO || commandText == null || commandText.isBlank()) {
			return false;
		}
		List<String> restricted = com.lopez.l2j.config.Config.MACRO_RESTRICTED_COMMAND_LIST;
		if (restricted == null || restricted.isEmpty()) {
			return false;
		}
		String lower = commandText.trim().toLowerCase(java.util.Locale.ROOT);
		for (String token : restricted) {
			if (token != null && !token.isBlank() && lower.contains(token.trim().toLowerCase(java.util.Locale.ROOT))) {
				return true;
			}
		}
		return false;
	}
}
