package com.lopez.l2j.game.drop;

import java.util.List;

/**
 * Contrato para acesso a tabela de droplist de monstros.
 */
public interface DropTable {

	/**
	 * Retorna a lista de regras de drop configuradas para o monstro indicado.
	 */
	List<DropData> getDrops(int mobId);

	/**
	 * Total de monstros com drop configurado no cache/banco.
	 */
	int size();
}
