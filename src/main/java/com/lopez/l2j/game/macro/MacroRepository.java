package com.lopez.l2j.game.macro;

import java.util.List;

/**
 * Contrato de persistencia para os macros dos personagens.
 */
public interface MacroRepository {

	List<Macro> findByCharId(int charId);

	void save(int charId, Macro macro);

	void delete(int charId, int macroId);

	void deleteAll(int charId);
}
