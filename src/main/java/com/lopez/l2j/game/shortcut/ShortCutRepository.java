package com.lopez.l2j.game.shortcut;

import java.util.List;

/**
 * Contrato de persistencia para os atalhos de barra do jogador.
 */
public interface ShortCutRepository {

	List<ShortCut> findByCharId(int charId, int classIndex);

	void save(int charId, int classIndex, ShortCut shortcut);

	void delete(int charId, int classIndex, int slot, int page);

	void deleteByTypeAndId(int charId, int type, int shortcutId);
}
