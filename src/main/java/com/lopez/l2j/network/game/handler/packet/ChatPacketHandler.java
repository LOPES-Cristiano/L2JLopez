package com.lopez.l2j.network.game.handler.packet;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.party.Party;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestBBSwrite;
import com.lopez.l2j.network.game.packet.GameClientPacket.Say2;
import com.lopez.l2j.network.game.packet.GameServerPacket.ActionFailed;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;

/**
 * Handler modular para pacotes de Chat (Say2, BBSWrite) e roteamento de canais.
 */
public class ChatPacketHandler {

	public static final int DEFAULT_CHAT_LENGTH = 120;

	private final GameSession session;
	private long lastShoutTime = 0;
	private long lastTradeTime = 0;
	private long lastHeroTime = 0;

	public ChatPacketHandler(GameSession session) {
		this.session = session;
	}

	public void handleSay(Say2 p) {
		PlayerCharacter active = session.activeChar();
		if (!session.inWorld() || p.text() == null || p.text().isBlank() || active == null) {
			return;
		}

		int maxLen = com.lopez.l2j.config.Config.CHAT_LENGTH > 0 ? com.lopez.l2j.config.Config.CHAT_LENGTH : DEFAULT_CHAT_LENGTH;
		String raw = p.text().length() > maxLen ? p.text().substring(0, maxLen) : p.text();
		if (!com.lopez.l2j.config.Config.ALLOW_MULTILINE_CHAT) {
			raw = raw.replace('\r', ' ').replace('\n', ' ');
		}
		raw = raw.trim();

		if (raw.startsWith("//")) {
			session.handleAdminCommand(raw.substring(2).trim());
			return;
		}
		if (raw.startsWith("/")) {
			session.handleSlashCommand(raw);
			return;
		}
		if (raw.startsWith(".")) {
			session.handleDotCommand(raw);
			return;
		}

		int channel = p.channel();
		String text = raw;

		// Detecta e extrai prefixos digitados na caixa principal de chat
		if (raw.startsWith("!")) {
			channel = CreatureSay.SHOUT;
			text = raw.substring(1).trim();
		} else if (raw.startsWith("+")) {
			channel = CreatureSay.TRADE;
			text = raw.substring(1).trim();
		} else if (raw.startsWith("%")) {
			channel = CreatureSay.HERO;
			text = raw.substring(1).trim();
		} else if (raw.startsWith("#")) {
			channel = CreatureSay.PARTY;
			text = raw.substring(1).trim();
		} else if (raw.startsWith("@")) {
			channel = CreatureSay.CLAN;
			text = raw.substring(1).trim();
		} else if (raw.startsWith("$")) {
			channel = CreatureSay.ALLIANCE;
			text = raw.substring(1).trim();
		}

		var ctx = session.context();
		// Aplica filtro de palavras censuradas (SayFilter) e penalidade de Karma
		if (com.lopez.l2j.config.Config.USE_CHAT_FILTER && ctx != null && ctx.wordFilter() != null) {
			String filtered = ctx.wordFilter().filter(text);
			if (!text.equals(filtered) && com.lopez.l2j.config.Config.CHAT_FILTER_KARMA > 0) {
				active.karma(active.karma() + com.lopez.l2j.config.Config.CHAT_FILTER_KARMA);
			}
			text = filtered;
		}

		if (text.isBlank()) {
			return;
		}

		if (ctx == null || ctx.world() == null) {
			return;
		}

		long now = System.currentTimeMillis();

		switch (channel) {
			case CreatureSay.SHOUT -> {
				if (!active.isGm()) {
					if (active.level() < com.lopez.l2j.config.Config.SHOUT_CHAT_LEVEL) {
						session.send(new CreatureSay(0, CreatureSay.ALL, "System",
								"Voce precisa de nivel " + com.lopez.l2j.config.Config.SHOUT_CHAT_LEVEL + " para usar o chat shout (!)."));
						return;
					}
					if ("OFF".equalsIgnoreCase(com.lopez.l2j.config.Config.GLOBAL_CHAT)) {
						session.send(new CreatureSay(0, CreatureSay.ALL, "System", "Chat shout esta desativado."));
						return;
					}
					if ("GM".equalsIgnoreCase(com.lopez.l2j.config.Config.GLOBAL_CHAT)) {
						session.send(new CreatureSay(0, CreatureSay.ALL, "System", "Apenas Administradores podem usar o chat shout."));
						return;
					}
					if (com.lopez.l2j.config.Config.SHOUT_CHAT_REUSE_DELAY > 0) {
						if (now - lastShoutTime < com.lopez.l2j.config.Config.SHOUT_CHAT_REUSE_DELAY * 1000L) {
							session.send(new ActionFailed());
							return;
						}
						lastShoutTime = now;
					}
				}

				boolean isGlobal = "GLOBAL".equalsIgnoreCase(com.lopez.l2j.config.Config.GLOBAL_CHAT);
				ctx.world().broadcast(new CreatureSay(active.objectId(), CreatureSay.SHOUT, active.name(), text), s -> {
					if (s.character() == null) return false;
					if (com.lopez.l2j.config.Config.REGION_CHAT_ALSO_BLOCKED && ctx.friends() != null && ctx.friends().isBlocked(s.character(), active)) {
						return false;
					}
					if (isGlobal || s.character().isGm() || active.isGm()) return true;
					double dx = s.character().x() - active.x();
					double dy = s.character().y() - active.y();
					return (dx * dx + dy * dy) <= (15000.0 * 15000.0);
				});
			}
			case CreatureSay.TRADE -> {
				if (!active.isGm()) {
					if (active.level() < com.lopez.l2j.config.Config.TRADE_CHAT_LEVEL) {
						session.send(new CreatureSay(0, CreatureSay.ALL, "System",
								"Voce precisa de nivel " + com.lopez.l2j.config.Config.TRADE_CHAT_LEVEL + " para usar o chat trade (+)."));
						return;
					}
					if ("OFF".equalsIgnoreCase(com.lopez.l2j.config.Config.TRADE_CHAT)) {
						session.send(new CreatureSay(0, CreatureSay.ALL, "System", "Chat trade esta desativado."));
						return;
					}
					if ("GM".equalsIgnoreCase(com.lopez.l2j.config.Config.TRADE_CHAT)) {
						session.send(new CreatureSay(0, CreatureSay.ALL, "System", "Apenas Administradores podem usar o chat trade."));
						return;
					}
					if (com.lopez.l2j.config.Config.TRADE_CHAT_REUSE_DELAY > 0) {
						if (now - lastTradeTime < com.lopez.l2j.config.Config.TRADE_CHAT_REUSE_DELAY * 1000L) {
							session.send(new ActionFailed());
							return;
						}
						lastTradeTime = now;
					}
				}

				boolean isGlobal = "GLOBAL".equalsIgnoreCase(com.lopez.l2j.config.Config.TRADE_CHAT);
				ctx.world().broadcast(new CreatureSay(active.objectId(), CreatureSay.TRADE, active.name(), text), s -> {
					if (s.character() == null) return false;
					if (com.lopez.l2j.config.Config.REGION_CHAT_ALSO_BLOCKED && ctx.friends() != null && ctx.friends().isBlocked(s.character(), active)) {
						return false;
					}
					if (isGlobal || s.character().isGm() || active.isGm()) return true;
					double dx = s.character().x() - active.x();
					double dy = s.character().y() - active.y();
					return (dx * dx + dy * dy) <= (15000.0 * 15000.0);
				});
			}
			case CreatureSay.HERO -> {
				if (!active.isGm()) {
					if (com.lopez.l2j.config.Config.HERO_CHAT_REUSE_DELAY > 0) {
						if (now - lastHeroTime < com.lopez.l2j.config.Config.HERO_CHAT_REUSE_DELAY * 1000L) {
							session.send(new ActionFailed());
							return;
						}
						lastHeroTime = now;
					}
				}
				ctx.world().broadcast(
						new CreatureSay(active.objectId(), CreatureSay.HERO, active.name(), text), x -> true);
			}
			case CreatureSay.PARTY -> {
				Party party = session.party();
				if (party != null) {
					party.broadcast(new CreatureSay(active.objectId(), CreatureSay.PARTY, active.name(), text));
				} else {
					session.send(new CreatureSay(0, CreatureSay.ALL, "System", "Voce nao esta em uma party."));
				}
			}
			case CreatureSay.CLAN -> {
				if (active.clanId() > 0) {
					ctx.world().broadcast(
							new CreatureSay(active.objectId(), CreatureSay.CLAN, active.name(), text),
							s -> s.character().clanId() == active.clanId());
				} else {
					session.send(new CreatureSay(0, CreatureSay.ALL, "System", "Voce nao esta em um cla."));
				}
			}
			case CreatureSay.ALLIANCE -> {
				if (active.clanId() > 0) {
					if (ctx.alliances() != null && ctx.clans() != null) {
						var clanOpt = ctx.clans().byClanId(active.clanId());
						if (clanOpt.isPresent() && clanOpt.get().allyId() > 0) {
							ctx.alliances().broadcastAllyChat(active, text, ctx.world());
						} else {
							session.send(new CreatureSay(0, CreatureSay.ALL, "System", "Voce nao esta em uma alianca."));
						}
					} else {
						ctx.world().broadcast(
								new CreatureSay(active.objectId(), CreatureSay.ALLIANCE, active.name(), text),
								s -> s.character().clanId() == active.clanId());
					}
				} else {
					session.send(new CreatureSay(0, CreatureSay.ALL, "System", "Voce nao esta em uma alianca."));
				}
			}
			case CreatureSay.TELL -> {
				var target = ctx.world().byName(p.target());
				if (target.isEmpty()) {
					String serverName = ctx.serverName() != null ? ctx.serverName() : "Server";
					session.send(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, serverName,
							p.target() + " nao esta online."));
					return;
				}
				if (target.get().character() != null && ctx.friends() != null
						&& ctx.friends().isBlocked(target.get().character(), active)) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							target.get().name() + " esta bloqueando mensagens de voce."));
					return;
				}
				target.get().send(new CreatureSay(active.objectId(), CreatureSay.TELL, active.name(), text));
				session.send(new CreatureSay(active.objectId(), CreatureSay.TELL, "->" + target.get().name(), text));
			}
			default -> ctx.world().broadcastAround(session, GameWorld.LOCAL_CHAT_RANGE,
					new CreatureSay(active.objectId(), CreatureSay.ALL, active.name(), text));
		}
	}

	public void handleBbsWrite(RequestBBSwrite p) {
		if (!session.inWorld() || session.activeChar() == null) {
			session.send(new ActionFailed());
			return;
		}
		String cmd = (p.url() == null || p.url().isBlank()) ? "_bbshome" : p.url().trim();
		session.handleBbsCommand(cmd);
	}
}
