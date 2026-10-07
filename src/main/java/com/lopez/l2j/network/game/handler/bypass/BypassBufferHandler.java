package com.lopez.l2j.network.game.handler.bypass;

import com.lopez.l2j.game.service.SchemeBufferService;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Handler modular para bypasses de buffer interativo (HTML e BBS).
 */
@Component
public class BypassBufferHandler implements IBypassHandler {

	private final SchemeBufferService schemeBufferService;

	@Autowired
	public BypassBufferHandler(SchemeBufferService schemeBufferService) {
		this.schemeBufferService = schemeBufferService;
	}

	@Override
	public boolean canHandle(String command) {
		if (command == null) return false;
		return command.startsWith("voiced_buffer") || command.startsWith("_bbsbuff_");
	}

	@Override
	public boolean handleBypass(String command, GameSession session) {
		if (session == null || session.activeCharacter() == null || schemeBufferService == null) {
			return false;
		}
		var active = session.activeCharacter();
		String param;
		if (command.startsWith("voiced_buffer")) {
			param = command.length() > 13 ? command.substring(13).trim() : "";
		} else {
			param = command.substring(9).trim();
		}

		if (param.isEmpty()) {
			session.send(new NpcHtmlMessage(0, schemeBufferService.renderHtml(active)));
			return true;
		}

		String sub = param.toLowerCase(Locale.ROOT);
		switch (sub) {
			case "heal" -> schemeBufferService.heal(active, session);
			case "cancel" -> schemeBufferService.cancelBuffs(active, session);
			case "1", "fighter" -> schemeBufferService.applyScheme(active, session, 1, false);
			case "2", "mage" -> schemeBufferService.applyScheme(active, session, 2, false);
			case "3" -> schemeBufferService.applyScheme(active, session, 3, false);
			default -> session.send(new NpcHtmlMessage(0, schemeBufferService.renderHtml(active)));
		}
		return true;
	}
}
