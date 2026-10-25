package com.lopez.l2j.network.game.handler.packet;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.PlayerStats;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.ActionFailed;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.EtcStatusUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.FriendList;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import com.lopez.l2j.network.game.packet.GameServerPacket.UserInfo;

public class SocialPacketHandler {

	public static final int SM_S1_ADDED_TO_FRIENDS = 132;
	public static final int SM_S1_REMOVED_FROM_FRIENDS = 133;
	public static final int SM_CANNOT_ADD_YOURSELF = 165;
	public static final int SM_FAILED_TO_INVITE_FRIEND = 166;
	public static final int SM_USER_NOT_IN_GAME = 170;
	public static final int SM_S1_NOT_LOGGED_IN = 171;
	public static final int SM_MESSAGE_REFUSAL_MODE = 176;
	public static final int SM_MESSAGE_ACCEPTANCE_MODE = 177;
	public static final int SM_S1_NOT_ON_FRIENDS_LIST = 462;
	public static final int SM_S1_ADDED_TO_IGNORE_LIST = 613;
	public static final int SM_S1_REMOVED_FROM_IGNORE_LIST = 614;
	public static final int SM_IGNORE_LIST_HEADER = 615;
	public static final int SM_THAT_CHARACTER_IS_RECOMMENDED = 790;
	public static final int SM_NO_MORE_RECOMMENDATIONS_TO_HAVE = 794;
	public static final int SM_YOU_CANNOT_RECOMMEND_YOURSELF = 829;
	public static final int SM_YOU_HAVE_RECOMMENDED_S1_S2_LEFT = 830;
	public static final int SM_YOU_HAVE_BEEN_RECOMMENDED_BY_S1 = 831;
	public static final int SM_TARGET_NO_LONGER_RECEIVE_RECOMMENDATION = 1188;
	public static final int SM_ONLY_LEVEL_SUP_10_CAN_RECOMMEND = 1298;

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
			send(SystemMessage.id(SM_CANNOT_ADD_YOURSELF));
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao pode adicionar a si mesmo como amigo."));
			return;
		}
		var targetPlayer = ctx().world().byName(p.name()).orElse(null);
		if (targetPlayer == null || targetPlayer.character() == null) {
			send(SystemMessage.id(SM_USER_NOT_IN_GAME));
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "O jogador " + p.name() + " nao esta online."));
			return;
		}
		if (ctx().friends() != null && ctx().friends().isBlocked(targetPlayer.character(), active)) {
			send(SystemMessage.of(SM_MESSAGE_REFUSAL_MODE));
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", targetPlayer.name() + " esta bloqueando seus pedidos."));
			return;
		}
		if (ctx().friends() != null) {
			var existing = ctx().friends().loadFriends(active.objectId(), null);
			for (var f : existing) {
				if (f.friendId() == targetPlayer.character().objectId() || f.name().equalsIgnoreCase(targetPlayer.name())) {
					send(SystemMessage.of(167, new SystemMessage.Text(targetPlayer.name())));
					return;
				}
			}
		}
		if (targetPlayer instanceof GameSession s) {
			s.pendingFriendInviteFrom(active.objectId());
		}
		targetPlayer.send(new GameServerPacket.FriendAddRequest(active.name()));
		send(new CreatureSay(0, CreatureSay.ALL, "SYS",
				"Convite de amizade enviado para " + targetPlayer.name() + "."));
	}

	public void handleSendFriendMsg(GameClientPacket.RequestSendFriendMsg p) {
		PlayerCharacter active = active();
		if (!session.inWorld() || active == null || p.receiver() == null || p.receiver().isBlank()
				|| p.message() == null || p.message().isBlank()) {
			send(new ActionFailed());
			return;
		}
		String receiverName = p.receiver().trim();
		var targetPlayer = ctx() != null && ctx().world() != null
				? ctx().world().byName(receiverName).orElse(null)
				: null;
		if (targetPlayer == null || targetPlayer.character() == null) {
			send(new GameServerPacket.FriendStatus(false, receiverName, 0));
			send(SystemMessage.of(SM_S1_NOT_LOGGED_IN, new SystemMessage.Text(receiverName)));
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "O amigo " + p.receiver() + " nao esta online."));
			return;
		}
		if (ctx() != null && ctx().friends() != null) {
			var friends = ctx().friends().loadFriends(active.objectId(), null);
			if (friends != null && !friends.isEmpty()) {
				boolean isFriend = false;
				for (var f : friends) {
					if (f.friendId() == targetPlayer.character().objectId()
							|| (f.name() != null && f.name().equalsIgnoreCase(receiverName))) {
						isFriend = true;
						break;
					}
				}
				if (!isFriend) {
					send(SystemMessage.of(SM_S1_NOT_ON_FRIENDS_LIST, new SystemMessage.Text(receiverName)));
					return;
				}
			}
		}
		if (Config.LOG_CHAT_ON_FILE && ctx() != null && ctx().wordFilter() != null) {
			ctx().wordFilter().logChatMessage(active.name(), CreatureSay.TELL, "[" + targetPlayer.name() + "] " + p.message());
		}
		var recvPacket = new GameServerPacket.FriendRecvMsg(active.name(), targetPlayer.name(), p.message());
		targetPlayer.send(recvPacket);
		send(new GameServerPacket.L2FriendSay(active.name(), targetPlayer.name(), p.message()));
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
			send(SystemMessage.of(SM_S1_ADDED_TO_FRIENDS, new SystemMessage.Text(inviterPlayer.name())));
			inviterPlayer.send(SystemMessage.of(SM_S1_ADDED_TO_FRIENDS, new SystemMessage.Text(active.name())));
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce agora e amigo de " + inviterPlayer.name() + "."));
			inviterPlayer.send(
					new CreatureSay(0, CreatureSay.ALL, "SYS", active.name() + " aceitou seu pedido de amizade!"));
			handleFriendList();
			if (inviterPlayer instanceof GameSession s) {
				s.onFriendList();
			}
		} else {
			inviterPlayer.send(SystemMessage.id(SM_FAILED_TO_INVITE_FRIEND));
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
			send(SystemMessage.of(SM_S1_REMOVED_FROM_FRIENDS, new SystemMessage.Text(p.name())));
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", p.name() + " foi removido da sua lista de amigos."));
			handleFriendList();
			ctx().world().player(targetChar.objectId()).ifPresent(pPlayer -> {
				pPlayer.send(SystemMessage.of(SM_S1_REMOVED_FROM_FRIENDS, new SystemMessage.Text(active.name())));
				if (pPlayer instanceof GameSession s) {
					s.onFriendList();
				}
			});
		}
	}

	public void handleEvaluate(GameClientPacket.RequestEvaluate p) {
		PlayerCharacter active = active();
		if (!session.inWorld() || active == null || ctx() == null || ctx().world() == null) {
			send(new ActionFailed());
			return;
		}
		int targetObjId = p.targetId() > 0 ? p.targetId() : session.targetObjectId();
		var targetPlayer = ctx().world().player(targetObjId).orElse(null);
		if (targetPlayer == null || targetPlayer.character() == null) {
			send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
			send(new ActionFailed());
			return;
		}
		PlayerCharacter target = targetPlayer.character();
		if (target.objectId() == active.objectId()) {
			send(SystemMessage.id(SM_YOU_CANNOT_RECOMMEND_YOURSELF));
			send(new ActionFailed());
			return;
		}
		if (active.level() < 10) {
			send(SystemMessage.id(SM_ONLY_LEVEL_SUP_10_CAN_RECOMMEND));
			send(new ActionFailed());
			return;
		}
		if (active.recomLeft() <= 0) {
			send(SystemMessage.id(SM_NO_MORE_RECOMMENDATIONS_TO_HAVE));
			send(new ActionFailed());
			return;
		}
		if (target.recomHave() >= 255) {
			send(SystemMessage.id(SM_TARGET_NO_LONGER_RECEIVE_RECOMMENDATION));
			send(new ActionFailed());
			return;
		}
		if (active.recommendedToday().contains(target.objectId())) {
			send(SystemMessage.id(SM_THAT_CHARACTER_IS_RECOMMENDED));
			send(new ActionFailed());
			return;
		}
		if (ctx().recommendations() != null) {
			var result = ctx().recommendations().evaluate(active, target);
			if (!result.success()) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", result.message()));
				send(new ActionFailed());
				return;
			}
		} else {
			active.recomLeft(active.recomLeft() - 1);
			target.recomHave(Math.min(255, target.recomHave() + 1));
			active.recommendedToday().add(target.objectId());
		}
		if (ctx().characters() != null) {
			ctx().characters().save(active, true);
			ctx().characters().save(target, true);
		}
		send(SystemMessage.of(SM_YOU_HAVE_RECOMMENDED_S1_S2_LEFT,
				new SystemMessage.Text(target.name()),
				new SystemMessage.Number(active.recomLeft())));
		targetPlayer.send(SystemMessage.of(SM_YOU_HAVE_BEEN_RECOMMENDED_BY_S1,
				new SystemMessage.Text(active.name())));
		var myTpl = ctx().characters() != null ? ctx().characters().template(active) : null;
		send(new UserInfo(active, myTpl));
		var targetTpl = ctx().characters() != null ? ctx().characters().template(target) : null;
		targetPlayer.send(new UserInfo(target, targetTpl));
		if (targetPlayer instanceof GameSession targetGs) {
			targetGs.broadcastAppearance();
		}
	}

	public void handleBlock(GameClientPacket.RequestBlock p) {
		PlayerCharacter active = active();
		if (!session.inWorld() || active == null) {
			send(new ActionFailed());
			return;
		}
		switch (p.type()) {
			case GameClientPacket.RequestBlock.BLOCK -> {
				if (p.name() != null && !p.name().isBlank() && ctx() != null && ctx().friends() != null) {
					if (ctx().friends().addBlock(active, p.name())) {
						send(SystemMessage.of(SM_S1_ADDED_TO_IGNORE_LIST, new SystemMessage.Text(p.name())));
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								p.name() + " foi adicionado a lista de bloqueados."));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nao foi possivel bloquear " + p.name() + "."));
					}
				}
			}
			case GameClientPacket.RequestBlock.UNBLOCK -> {
				if (p.name() != null && !p.name().isBlank() && ctx() != null && ctx().friends() != null) {
					if (ctx().friends().removeBlock(active, p.name())) {
						send(SystemMessage.of(SM_S1_REMOVED_FROM_IGNORE_LIST, new SystemMessage.Text(p.name())));
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								p.name() + " foi removido da lista de bloqueados."));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								p.name() + " nao estava na lista de bloqueados."));
					}
				}
			}
			case GameClientPacket.RequestBlock.BLOCKLIST -> {
				send(SystemMessage.id(SM_IGNORE_LIST_HEADER));
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "--- Lista de Bloqueados ---"));
				for (String name : active.blockList()) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "- " + name));
				}
			}
			case GameClientPacket.RequestBlock.ALLBLOCK -> {
				active.setBlockingAll(true);
				var tpl = (ctx() != null && ctx().characters() != null) ? ctx().characters().template(active) : null;
				var stats = tpl != null ? PlayerStats.calculate(active, tpl) : null;
				send(new EtcStatusUpdate(active, stats));
				send(SystemMessage.id(SM_MESSAGE_REFUSAL_MODE));
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce esta bloqueando todas as mensagens privadas."));
			}
			case GameClientPacket.RequestBlock.ALLUNBLOCK -> {
				active.setBlockingAll(false);
				var tpl = (ctx() != null && ctx().characters() != null) ? ctx().characters().template(active) : null;
				var stats = tpl != null ? PlayerStats.calculate(active, tpl) : null;
				send(new EtcStatusUpdate(active, stats));
				send(SystemMessage.id(SM_MESSAGE_ACCEPTANCE_MODE));
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce desbloqueou o recebimento de mensagens."));
			}
		}
	}
}
