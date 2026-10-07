package com.lopez.l2j.network.game.handler.voiced;

import com.lopez.l2j.game.service.AcpService;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Handler para o comando de voz .acp (Auto Combat Potion) - Onda 3.
 */
@Component
public class VoicedAcpHandler implements IVoicedCommandHandler {

	private static final List<String> COMMANDS = List.of("acp");

	private final AcpService acpService;

	@Autowired
	public VoicedAcpHandler(AcpService acpService) {
		this.acpService = acpService;
	}

	@Override
	public List<String> getVoicedCommandList() {
		return COMMANDS;
	}

	@Override
	public boolean useVoicedCommand(String command, GameSession session, String params) {
		if (session == null || session.activeCharacter() == null || acpService == null) {
			return false;
		}
		var active = session.activeCharacter();

		if (params == null || params.isBlank() || params.equalsIgnoreCase("menu") || params.equalsIgnoreCase("status")) {
			session.send(new NpcHtmlMessage(0, acpService.renderHtml(active)));
			return true;
		}

		String[] parts = params.trim().split("\\s+");
		String sub = parts[0].toLowerCase(Locale.ROOT);

		switch (sub) {
			case "on" -> {
				acpService.setEnabled(active, true);
				session.send(new CreatureSay(0, CreatureSay.ALL, "ACP", "Auto Combat Potion (ACP) ATIVADO."));
				session.send(new NpcHtmlMessage(0, acpService.renderHtml(active)));
				return true;
			}
			case "off" -> {
				acpService.setEnabled(active, false);
				session.send(new CreatureSay(0, CreatureSay.ALL, "ACP", "Auto Combat Potion (ACP) DESATIVADO."));
				session.send(new NpcHtmlMessage(0, acpService.renderHtml(active)));
				return true;
			}
			case "hp" -> {
				if (parts.length > 1) {
					try {
						int val = Integer.parseInt(parts[1]);
						acpService.setThresholds(active, val, null, null);
						session.send(new CreatureSay(0, CreatureSay.ALL, "ACP", "Limiar de HP configurado para " + val + "%."));
					} catch (NumberFormatException ignored) {}
				}
				session.send(new NpcHtmlMessage(0, acpService.renderHtml(active)));
				return true;
			}
			case "cp" -> {
				if (parts.length > 1) {
					try {
						int val = Integer.parseInt(parts[1]);
						acpService.setThresholds(active, null, val, null);
						session.send(new CreatureSay(0, CreatureSay.ALL, "ACP", "Limiar de CP configurado para " + val + "%."));
					} catch (NumberFormatException ignored) {}
				}
				session.send(new NpcHtmlMessage(0, acpService.renderHtml(active)));
				return true;
			}
			case "mp" -> {
				if (parts.length > 1) {
					try {
						int val = Integer.parseInt(parts[1]);
						acpService.setThresholds(active, null, null, val);
						session.send(new CreatureSay(0, CreatureSay.ALL, "ACP", "Limiar de MP configurado para " + val + "%."));
					} catch (NumberFormatException ignored) {}
				}
				session.send(new NpcHtmlMessage(0, acpService.renderHtml(active)));
				return true;
			}
			default -> {
				session.send(new NpcHtmlMessage(0, acpService.renderHtml(active)));
				return true;
			}
		}
	}
}
