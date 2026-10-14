package com.lopez.l2j.network.game.handler.packet;

import com.lopez.l2j.network.game.GameSession;

/**
 * Handler especializado para pacotes e operacoes de Pledge (Cla),
 * privilegios, subpledges (Academy / Royal Guard / Order of Knights),
 * skills de cla e inscricoes de Siege.
 * Estende {@link PartyClanPacketHandler} para manter total compatibilidade
 * com {@link GameSession#partyClanHandler()}.
 */
public class PledgePacketHandler extends PartyClanPacketHandler {

	public PledgePacketHandler(GameSession session) {
		super(session);
	}
}
