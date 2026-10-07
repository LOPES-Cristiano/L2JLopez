package com.lopez.l2j.network.game.handler.admin;

import com.lopez.l2j.network.game.GameSession;
import java.util.List;

/**
 * Interface padrao para handlers de comandos de administracao (admin commands).
 */
public interface IAdminCommandHandler {

	/**
	 * Executa o comando de administracao.
	 *
	 * @param command comando invocado (sem o prefixo 'admin_')
	 * @param session sessao do jogador/GM
	 * @param params  parametros do comando
	 * @return true se o comando foi tratado com sucesso
	 */
	boolean useAdminCommand(String command, GameSession session, String params);

	/**
	 * Lista de nomes de comandos tratados por este handler.
	 */
	List<String> getAdminCommandList();
}
