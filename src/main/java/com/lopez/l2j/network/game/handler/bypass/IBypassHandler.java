package com.lopez.l2j.network.game.handler.bypass;

import com.lopez.l2j.network.game.GameSession;

/**
 * Interface padrao para handlers modulares de bypasses de HTML (bypasses do cliente).
 */
public interface IBypassHandler {

	/**
	 * Verifica se este handler pode processar o comando.
	 *
	 * @param command comando decodificado (ex: "voiced_tvtjoin", "antibot_validate 1")
	 * @return true se o handler aceita processar
	 */
	boolean canHandle(String command);

	/**
	 * Processa o bypass.
	 *
	 * @param command comando decodificado
	 * @param session sessao do jogador
	 * @return true se foi tratado com sucesso
	 */
	boolean handleBypass(String command, GameSession session);
}
