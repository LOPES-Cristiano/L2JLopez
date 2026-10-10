package com.lopez.l2j.network.game.handler.bypass;

import com.lopez.l2j.game.autofarm.AutoFarmMode;
import com.lopez.l2j.game.autofarm.AutoFarmService;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Handler modular para bypasses da janela interativa do .autofarm
 */
@Component
public class BypassAutoFarmHandler implements IBypassHandler {

	private final AutoFarmService autoFarmService;

	@Autowired
	public BypassAutoFarmHandler(@Autowired(required = false) AutoFarmService autoFarmService) {
		this.autoFarmService = autoFarmService;
	}

	@Override
	public boolean canHandle(String command) {
		if (command == null) return false;
		return command.startsWith("voiced_autofarm");
	}

	@Override
	public boolean handleBypass(String command, GameSession session) {
		if (session == null || session.activeCharacter() == null || autoFarmService == null) {
			return false;
		}
		var active = session.activeCharacter();
		var skillService = session.context() != null ? session.context().skillService() : null;

		String param = command.length() > 15 ? command.substring(15).trim() : "";

		if (param.isEmpty()) {
			session.send(new NpcHtmlMessage(0, autoFarmService.renderHtml(active, skillService)));
			return true;
		}

		String[] parts = param.split("\\s+");
		String sub = parts[0].toLowerCase(Locale.ROOT);

		switch (sub) {
			case "toggle" -> {
				boolean now = autoFarmService.toggleAutoFarm(active);
				session.send(new CreatureSay(0, CreatureSay.ALL, "AutoFarm", "Auto-Farm " + (now ? "ATIVADO." : "DESATIVADO.")));
				session.send(new NpcHtmlMessage(0, autoFarmService.renderHtml(active, skillService)));
				return true;
			}
			case "mode" -> {
				if (parts.length > 1) {
					try {
						AutoFarmMode mode = AutoFarmMode.valueOf(parts[1].toUpperCase(Locale.ROOT));
						autoFarmService.setMode(active.objectId(), mode);
						session.send(new CreatureSay(0, CreatureSay.ALL, "AutoFarm", "Modo alterado para: " + mode));
					} catch (IllegalArgumentException ignored) {}
				}
				session.send(new NpcHtmlMessage(0, autoFarmService.renderHtml(active, skillService)));
				return true;
			}
			case "radius" -> {
				if (parts.length > 1) {
					try {
						int r = Integer.parseInt(parts[1]);
						autoFarmService.setFarmRadius(active.objectId(), Math.max(300, Math.min(3000, r)));
						session.send(new CreatureSay(0, CreatureSay.ALL, "AutoFarm", "Raio configurado para: " + r));
					} catch (NumberFormatException ignored) {}
				}
				session.send(new NpcHtmlMessage(0, autoFarmService.renderHtml(active, skillService)));
				return true;
			}
			case "hp" -> {
				if (parts.length > 1) {
					try {
						int hp = Integer.parseInt(parts[1]);
						autoFarmService.setAutoPotionHpThreshold(active.objectId(), Math.max(0.1, Math.min(0.95, hp / 100.0)));
						session.send(new CreatureSay(0, CreatureSay.ALL, "AutoFarm", "Pocao de HP configurada para: " + hp + "%"));
					} catch (NumberFormatException ignored) {}
				}
				session.send(new NpcHtmlMessage(0, autoFarmService.renderHtml(active, skillService)));
				return true;
			}
			case "skill" -> {
				if (parts.length > 1) {
					try {
						int skId = Integer.parseInt(parts[1]);
						autoFarmService.setSelectedSkill(active.objectId(), skId);
						session.send(new CreatureSay(0, CreatureSay.ALL, "AutoFarm", "Magia selecionada: ID " + skId));
					} catch (NumberFormatException ignored) {}
				}
				session.send(new NpcHtmlMessage(0, autoFarmService.renderHtml(active, skillService)));
				return true;
			}
			default -> {
				session.send(new NpcHtmlMessage(0, autoFarmService.renderHtml(active, skillService)));
				return true;
			}
		}
	}
}
