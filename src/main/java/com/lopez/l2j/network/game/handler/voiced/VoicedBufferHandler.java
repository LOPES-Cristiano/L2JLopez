package com.lopez.l2j.network.game.handler.voiced;

import com.lopez.l2j.game.service.SchemeBufferService;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Handler para o comando de voz .buffer (Buffer por Esquemas) - Onda 3.
 */
@Component
public class VoicedBufferHandler implements IVoicedCommandHandler {

	private static final List<String> COMMANDS = List.of("buffer");

	private final SchemeBufferService schemeBufferService;

	@Autowired
	public VoicedBufferHandler(SchemeBufferService schemeBufferService) {
		this.schemeBufferService = schemeBufferService;
	}

	@Override
	public List<String> getVoicedCommandList() {
		return COMMANDS;
	}

	@Override
	public boolean useVoicedCommand(String command, GameSession session, String params) {
		if (session == null || session.activeCharacter() == null || schemeBufferService == null) {
			return false;
		}
		var active = session.activeCharacter();

		if (params == null || params.isBlank() || params.equalsIgnoreCase("menu")) {
			session.send(new NpcHtmlMessage(0, schemeBufferService.renderHtml(active)));
			return true;
		}

		String sub = params.trim().toLowerCase(Locale.ROOT);
		switch (sub) {
			case "heal" -> schemeBufferService.heal(active, session);
			case "cancel" -> schemeBufferService.cancelBuffs(active, session);
			case "1" -> schemeBufferService.applyScheme(active, session, 1, false);
			case "2" -> schemeBufferService.applyScheme(active, session, 2, false);
			case "3" -> schemeBufferService.applyScheme(active, session, 3, false);
			default -> session.send(new NpcHtmlMessage(0, schemeBufferService.renderHtml(active)));
		}
		return true;
	}
}
