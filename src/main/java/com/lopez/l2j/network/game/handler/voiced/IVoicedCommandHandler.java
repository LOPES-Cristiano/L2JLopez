package com.lopez.l2j.network.game.handler.voiced;

import com.lopez.l2j.network.game.GameSession;
import java.util.List;

/**
 * Interface padrao para handlers de comandos de voz (.menu, .online, .autofarm, etc.).
 */
public interface IVoicedCommandHandler {

	/**
	 * Executa o comando de voz.
	 *
	 * @param command comando invocado (ex: "menu", "online", "autofarm")
	 * @param session sessao do jogador
	 * @param params  argumentos passados apos o comando
	 * @return true se o comando foi tratado com sucesso
	 */
	boolean useVoicedCommand(String command, GameSession session, String params);

	/**
	 * Lista de nomes de comandos tratados por este handler (sem o ponto '.').
	 */
	List<String> getVoicedCommandList();
}
