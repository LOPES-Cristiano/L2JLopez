package com.lopez.l2j.network.game.handler.bypass;

import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Handler modular para bypasses de menus de preferencias e anti-bot.
 */
@Component
public class BypassPreferencesHandler implements IBypassHandler {

	@Override
	public boolean canHandle(String command) {
		if (command == null) {
			return false;
		}
		return command.startsWith("voiced_menutoggle ") || command.startsWith("antibot_validate ");
	}

	@Override
	public boolean handleBypass(String command, GameSession session) {
		if (session == null || session.activeCharacter() == null) {
			return false;
		}
		var active = session.activeCharacter();
		var ctx = session.context();

		if (command.startsWith("voiced_menutoggle ")) {
			if (ctx.preferences() != null) {
				String toggleType = command.substring(18).trim().toLowerCase(Locale.ROOT);
				switch (toggleType) {
					case "autoloot" -> ctx.preferences().toggleAutoLoot(active.objectId());
					case "trade" -> ctx.preferences().toggleTradeRefusal(active.objectId());
					case "blockbuff" -> ctx.preferences().toggleBlockBuffs(active.objectId());
					case "party" -> ctx.preferences().toggleBlockParty(active.objectId());
					case "exp" -> ctx.preferences().toggleBlockExp(active.objectId());
				}
				session.send(new NpcHtmlMessage(0, ctx.preferences().buildMenuHtml(active.objectId(), active.name())));
				return true;
			}
		} else if (command.startsWith("antibot_validate ")) {
			if (ctx.botsPrevention() != null) {
				try {
					int selected = Integer.parseInt(command.substring(17).trim());
					boolean ok = ctx.botsPrevention().validateAnswer(active.objectId(), selected);
					if (ok) {
						session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Validacao Anti-Bot bem-sucedida!"));
					} else {
						var punishment = ctx.botsPrevention().checkPunishment(active.objectId());
						punishment.ifPresent(pType -> {
							session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Validacao Anti-Bot falhou. Punicao: " + pType.name()));
							if (pType == com.lopez.l2j.game.service.BotsPreventionService.PunishmentType.KICK) {
								session.kick();
							}
						});
					}
					return true;
				} catch (NumberFormatException ignored) {}
			}
		}
		return false;
	}
}
