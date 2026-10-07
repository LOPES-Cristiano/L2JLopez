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
}
