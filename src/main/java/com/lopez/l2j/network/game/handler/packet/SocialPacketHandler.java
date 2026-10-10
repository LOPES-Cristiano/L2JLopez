package com.lopez.l2j.network.game.handler.packet;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.ActionFailed;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.FriendList;

public class SocialPacketHandler {

	private final GameSession session;

	public SocialPacketHandler(GameSession session) {
		this.session = session;
	}

	private GameSession.Context ctx() {
		return session.context();
	}

	private PlayerCharacter active() {
		return session.activeChar();
	}

	private void send(GameServerPacket p) {
		session.send(p);
	}

	public void handleFriendList() {
		PlayerCharacter active = active();
		if (!session.inWorld() || active == null || ctx() == null || ctx().friends() == null) {
			send(new ActionFailed());
			return;
		}
		var list = ctx().friends().loadFriends(active.objectId(), id -> ctx().world().player(id).isPresent());
		send(new FriendList(list));
	}

	public void handleFriendInvite(GameClientPacket.RequestFriendInvite p) {
		PlayerCharacter active = active();
		if (!session.inWorld() || active == null || p.name() == null || p.name().isBlank()) {
			send(new ActionFailed());
			return;
		}
		if (p.name().equalsIgnoreCase(active.name())) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao pode adicionar a si mesmo como amigo."));
			return;
		}
		var targetPlayer = ctx().world().byName(p.name()).orElse(null);
		if (targetPlayer == null || targetPlayer.character() == null) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "O jogador " + p.name() + " nao esta online."));
			return;
		}
		if (ctx().friends() != null && ctx().friends().isBlocked(targetPlayer.character(), active)) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", targetPlayer.name() + " esta bloqueando seus pedidos."));
			return;
		}
		if (targetPlayer instanceof GameSession s) {
			s.pendingFriendInviteFrom(active.objectId());
		}
		send(new CreatureSay(0, CreatureSay.ALL, "SYS",
				"Convite de amizade enviado para " + targetPlayer.name() + "."));
	}

	public void handleAnswerFriendInvite(GameClientPacket.RequestAnswerFriendInvite p) {
		PlayerCharacter active = active();
		if (!session.inWorld() || active == null || session.pendingFriendInviteFrom() == 0) {
			send(new ActionFailed());
			return;
		}
		int inviterId = session.pendingFriendInviteFrom();
		session.pendingFriendInviteFrom(0);
		var inviterPlayer = ctx().world().player(inviterId).orElse(null);
		if (inviterPlayer == null || inviterPlayer.character() == null) {
			return;
		}
		if (p.response() == 1) {
			if (ctx().friends() != null) {
				ctx().friends().addFriendship(active.objectId(), active.name(), inviterPlayer.character().objectId(),
						inviterPlayer.name());
			}
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce agora e amigo de " + inviterPlayer.name() + "."));
			inviterPlayer.send(
					new CreatureSay(0, CreatureSay.ALL, "SYS", active.name() + " aceitou seu pedido de amizade!"));
			handleFriendList();
			if (inviterPlayer instanceof GameSession s) {
				s.onFriendList();
			}
		} else {
			inviterPlayer.send(
					new CreatureSay(0, CreatureSay.ALL, "SYS", active.name() + " recusou seu pedido de amizade."));
		}
	}

	public void handleFriendDel(GameClientPacket.RequestFriendDel p) {
		PlayerCharacter active = active();
		if (!session.inWorld() || active == null || p.name() == null || p.name().isBlank()) {
			send(new ActionFailed());
			return;
		}
		var targetChar = ctx().characters().findByName(p.name()).orElse(null);
		if (targetChar != null && ctx().friends() != null) {
			ctx().friends().removeFriendship(active.objectId(), targetChar.objectId());
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", p.name() + " foi removido da sua lista de amigos."));
			handleFriendList();
			ctx().world().player(targetChar.objectId()).ifPresent(pPlayer -> {
				if (pPlayer instanceof GameSession s) {
					s.onFriendList();
				}
			});
		}
	}

	public void handleBlock(GameClientPacket.RequestBlock p) {
		PlayerCharacter active = active();
		if (!session.inWorld() || active == null || ctx().friends() == null) {
			send(new ActionFailed());
			return;
		}
		switch (p.type()) {
			case GameClientPacket.RequestBlock.BLOCK -> {
				if (p.name() != null && !p.name().isBlank()) {
					if (ctx().friends().addBlock(active, p.name())) {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								p.name() + " foi adicionado a lista de bloqueados."));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nao foi possivel bloquear " + p.name() + "."));
					}
				}
			}
			case GameClientPacket.RequestBlock.UNBLOCK -> {
				if (p.name() != null && !p.name().isBlank()) {
					if (ctx().friends().removeBlock(active, p.name())) {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								p.name() + " foi removido da lista de bloqueados."));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								p.name() + " nao estava na lista de bloqueados."));
					}
				}
			}
			case GameClientPacket.RequestBlock.BLOCKLIST -> {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "--- Lista de Bloqueados ---"));
				for (String name : active.blockList()) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "- " + name));
				}
			}
			case GameClientPacket.RequestBlock.ALLBLOCK -> {
				active.setBlockingAll(true);
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce esta bloqueando todas as mensagens privadas."));
			}
			case GameClientPacket.RequestBlock.ALLUNBLOCK -> {
				active.setBlockingAll(false);
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce desbloqueou o recebimento de mensagens."));
			}
		}
	}
}
