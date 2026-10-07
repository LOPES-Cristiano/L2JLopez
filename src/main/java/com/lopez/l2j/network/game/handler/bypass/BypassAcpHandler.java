package com.lopez.l2j.network.game.handler.bypass;

import com.lopez.l2j.game.service.AcpService;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Handler modular para bypasses da janela interativa do .acp
 */
@Component
public class BypassAcpHandler implements IBypassHandler {

	private final AcpService acpService;

	@Autowired
	public BypassAcpHandler(AcpService acpService) {
		this.acpService = acpService;
	}

	@Override
	public boolean canHandle(String command) {
		if (command == null) return false;
		return command.startsWith("voiced_acp");
	}

	@Override
	public boolean handleBypass(String command, GameSession session) {
		if (session == null || session.activeCharacter() == null || acpService == null) {
			return false;
		}
		var active = session.activeCharacter();
		String param = command.length() > 10 ? command.substring(10).trim() : "";

		if (param.isEmpty()) {
			session.send(new NpcHtmlMessage(0, acpService.renderHtml(active)));
			return true;
		}

		String[] parts = param.split("\\s+");
		String sub = parts[0].toLowerCase(Locale.ROOT);

		switch (sub) {
			case "on" -> {
				acpService.setEnabled(active, true);
				session.send(new CreatureSay(0, CreatureSay.ALL, "ACP", "Auto Combat Potion (ACP) ATIVADO."));
			}
			case "off" -> {
				acpService.setEnabled(active, false);
				session.send(new CreatureSay(0, CreatureSay.ALL, "ACP", "Auto Combat Potion (ACP) DESATIVADO."));
			}
			case "hp" -> {
				if (parts.length > 1) {
					try {
						int val = Integer.parseInt(parts[1]);
						acpService.setThresholds(active, val, null, null);
						session.send(new CreatureSay(0, CreatureSay.ALL, "ACP", "Limiar de HP: " + val + "%."));
					} catch (NumberFormatException ignored) {}
				}
			}
			case "cp" -> {
				if (parts.length > 1) {
					try {
						int val = Integer.parseInt(parts[1]);
						acpService.setThresholds(active, null, val, null);
						session.send(new CreatureSay(0, CreatureSay.ALL, "ACP", "Limiar de CP: " + val + "%."));
					} catch (NumberFormatException ignored) {}
				}
			}
			case "mp" -> {
				if (parts.length > 1) {
					try {
						int val = Integer.parseInt(parts[1]);
						acpService.setThresholds(active, null, null, val);
						session.send(new CreatureSay(0, CreatureSay.ALL, "ACP", "Limiar de MP: " + val + "%."));
					} catch (NumberFormatException ignored) {}
				}
			}
		}

		session.send(new NpcHtmlMessage(0, acpService.renderHtml(active)));
		return true;
	}
}
