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
		return command.startsWith("voiced_menutoggle ") || command.startsWith("antibot_validate ")
				|| command.startsWith("voice_");
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
				sendMenuHtml(session, active);
				return true;
			}
		} else if (command.startsWith("voice_")) {
			if (ctx.preferences() != null) {
				String action = command.substring(6).trim();
				switch (action) {
					case "enableAutoloot" -> ctx.preferences().setAutoLoot(active.objectId(), true);
					case "disableAutoloot" -> ctx.preferences().setAutoLoot(active.objectId(), false);
					case "enableTrade" -> ctx.preferences().setTradeRefusal(active.objectId(), false);
					case "disableTrade" -> ctx.preferences().setTradeRefusal(active.objectId(), true);
					case "enableblockbuff" -> ctx.preferences().setBlockBuffs(active.objectId(), true);
					case "disableblockbuff" -> ctx.preferences().setBlockBuffs(active.objectId(), false);
					case "enableGainExp" -> ctx.preferences().setBlockExp(active.objectId(), false);
					case "disableGainExp" -> ctx.preferences().setBlockExp(active.objectId(), true);
					case "blockparty" -> ctx.preferences().setBlockParty(active.objectId(), true);
					case "unblockparty" -> ctx.preferences().setBlockParty(active.objectId(), false);
					case "getaiogoods" -> {
						if (ctx.aio() != null && active.isAio()) {
							for (var item : ctx.aio().getAioGoods()) {
								ctx.inventories().addItem(active.inventory(), item.itemId(), item.count(), "AioGoods");
							}
							session.send(com.lopez.l2j.network.game.packet.GameServerPacket.ItemList.of(active.inventory().items(), false));
							session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Consumiveis de AIOx entregues no inventario."));
						}
					}
				}
				sendMenuHtml(session, active);
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

	private void sendMenuHtml(GameSession session, com.lopez.l2j.game.model.PlayerCharacter active) {
		var ctx = session.context();
		if (ctx != null && ctx.htmls() != null) {
			String menuHtm = ctx.htmls().getHtml("mods/menu.htm");
			if (menuHtm != null) {
				var p = ctx.preferences().getPreferences(active.objectId());
				String on = "<font color=\"00FF00\">ON</font>";
				String off = "<font color=\"CC0000\">OFF</font>";
				String rendered = menuHtm
						.replace("%notraders%", off)
						.replace("%notrade%", p.isTradeRefusal() ? on : off)
						.replace("%autoloot%", p.isAutoLoot() ? on : off)
						.replace("%nomsg%", off)
						.replace("%buffanim%", on)
						.replace("%gainexp%", p.isBlockExp() ? on : off)
						.replace("%blockparty%", p.isBlockParty() ? on : off)
						.replace("%blockbuff%", p.isBlockBuffs() ? on : off)
						.replace("%skillsucceed%", on);
				session.send(new NpcHtmlMessage(0, rendered));
				return;
			}
		}
		session.send(new NpcHtmlMessage(0, ctx.preferences().buildMenuHtml(active.objectId(), active.name())));
	}
}
