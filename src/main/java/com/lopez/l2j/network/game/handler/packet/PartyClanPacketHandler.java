package com.lopez.l2j.network.game.handler.packet;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.party.Party;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.ActionFailed;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.PledgeCrest;
import com.lopez.l2j.network.game.packet.GameServerPacket.PledgeShowInfoUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.PledgeShowMemberListAll;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;

/**
 * Handler modular para pacotes e fluxos de Party (Grupo) e Clan/Pledge (Clã).
 * Desacoplado do GameSession para manter o ciclo de vida e comandos de rede isolados.
 */
public class PartyClanPacketHandler {

	private final GameSession session;

	public PartyClanPacketHandler(GameSession session) {
		this.session = session;
	}

	public void handleJoinParty(GameClientPacket.RequestJoinParty p) {
		PlayerCharacter active = session.activeChar();
		if (!session.inWorld() || active == null || active.isDead()) {
			session.send(new ActionFailed());
			return;
		}
		var ctx = session.context();
		if (ctx == null || ctx.world() == null) {
			session.send(new ActionFailed());
			return;
		}

		var target = ctx.world().byName(p.name());
		if (target.isEmpty() || target.get().character() == null) {
			session.send(SystemMessage.id(SystemMessage.TARGET_CANT_FOUND));
			session.send(new ActionFailed());
			return;
		}
		if (!(target.get() instanceof GameSession targetSession)) {
			session.send(new ActionFailed());
			return;
		}
		if (targetSession == session || targetSession.character().objectId() == active.objectId()) {
			session.send(SystemMessage.id(SystemMessage.CANT_INVITE_YOURSELF));
			session.send(new ActionFailed());
			return;
		}
		if (targetSession.party() != null) {
			session.send(SystemMessage.of(SystemMessage.PLAYER_ALREADY_IN_PARTY,
					new SystemMessage.Text(targetSession.character().name())));
			session.send(new ActionFailed());
			return;
		}
		Party currentParty = session.party();
		if (currentParty != null) {
			if (!currentParty.isLeader(active.objectId())) {
				session.send(SystemMessage.id(SystemMessage.ONLY_LEADER_CAN_INVITE));
				session.send(new ActionFailed());
				return;
			}
			if (currentParty.isFull()) {
				session.send(SystemMessage.id(SystemMessage.PARTY_FULL));
				session.send(new ActionFailed());
				return;
			}
		}
		if (targetSession.pendingPartyInvite() != null) {
			session.send(SystemMessage.id(SystemMessage.WAITING_FOR_REPLY));
			session.send(new ActionFailed());
			return;
		}
		targetSession.setPendingPartyInvite(new GameSession.RequestPartyPending(session, p.itemDistribution()));
		targetSession.send(new GameServerPacket.AskJoinParty(active.name(), p.itemDistribution()));
		session.send(SystemMessage.of(SystemMessage.YOU_INVITED_S1_TO_PARTY,
				new SystemMessage.Text(targetSession.character().name())));
	}

	public void handleAnswerJoinParty(GameClientPacket.RequestAnswerJoinParty p) {
		PlayerCharacter active = session.activeChar();
		var pending = session.pendingPartyInvite();
		if (!session.inWorld() || active == null || pending == null) {
			return;
		}
		session.setPendingPartyInvite(null);
		var requester = pending.requester();
		if (requester == null || requester.character() == null || !requester.inWorld()) {
			return;
		}
		if (p.response() == 0) {
			requester.send(SystemMessage.of(SystemMessage.S1_REFUSED_PARTY, new SystemMessage.Text(active.name())));
			return;
		}

		// Aceitou o convite (response == 1)
		if (requester.party() == null) {
			var newParty = new Party(requester, session, pending.itemDistribution());
			requester.party(newParty);
			session.party(newParty);
			session.send(new GameServerPacket.JoinParty(1));
			requester.send(new GameServerPacket.PartySmallWindowAll(requester.character().objectId(),
					pending.itemDistribution(), newParty.characters(), requester.character().objectId()));
			session.send(new GameServerPacket.PartySmallWindowAll(requester.character().objectId(), pending.itemDistribution(),
					newParty.characters(), active.objectId()));
			requester.send(SystemMessage.of(SystemMessage.S1_JOINED_PARTY, new SystemMessage.Text(active.name())));
			session.send(SystemMessage.of(SystemMessage.YOU_JOINED_PARTY,
					new SystemMessage.Text(requester.character().name())));
		} else {
			var existingParty = requester.party();
			if (existingParty.isFull()) {
				session.send(SystemMessage.id(SystemMessage.PARTY_FULL));
				return;
			}
			if (existingParty.addMember(session)) {
				session.party(existingParty);
				session.send(new GameServerPacket.JoinParty(1));
			}
		}
	}

	public void handleLeaveParty() {
		PlayerCharacter active = session.activeChar();
		Party party = session.party();
		if (party != null && active != null) {
			party.removeMember(session);
			session.party(null);
		}
	}

	public void handleExpelPartyMember(String name) {
		PlayerCharacter active = session.activeChar();
		Party party = session.party();
		if (party != null && active != null && party.isLeader(active.objectId())) {
			party.oust(name);
		}
	}

	public void handlePledgeCrest(GameClientPacket.RequestPledgeCrest p) {
		var ctx = session.context();
		if (p.crestId() == 0 || ctx == null || ctx.crests() == null) {
			return;
		}
		byte[] data = ctx.crests().getPledgeCrest(p.crestId());
		if (data != null) {
			session.send(new PledgeCrest(p.crestId(), data));
		}
	}

	public void handlePledgeInfo(GameClientPacket.RequestPledgeInfo p) {
		var ctx = session.context();
		if (ctx == null || ctx.clans() == null) {
			return;
		}
		var clan = ctx.clans().byClanId(p.clanId()).orElse(null);
		if (clan != null) {
			session.send(new PledgeShowInfoUpdate(clan));
		}
	}

	public void handlePledgeMemberList() {
		var ctx = session.context();
		PlayerCharacter active = session.activeChar();
		if (ctx == null || ctx.clans() == null || active == null || active.clanId() == 0) {
			return;
		}
		var clan = ctx.clans().byClanId(active.clanId()).orElse(null);
		if (clan != null) {
			session.send(new PledgeShowMemberListAll(clan, 0));
		}
	}

	public void handleSetPledgeCrest(GameClientPacket.RequestSetPledgeCrest p) {
		var ctx = session.context();
		PlayerCharacter active = session.activeChar();
		if (ctx == null || ctx.clans() == null || ctx.crests() == null || active == null || active.clanId() == 0) {
			return;
		}
		var clan = ctx.clans().byClanId(active.clanId()).orElse(null);
		if (clan == null || !clan.isLeader(active.objectId())) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Apenas o lider do cla pode alterar o brasao."));
			return;
		}
		if (clan.level() < 3) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
					"Cla nivel 3 ou superior e necessario para definir brasao."));
			return;
		}
		int crestId = ctx.crests().savePledgeCrest(p.data());
		if (crestId != 0) {
			ctx.clans().updateCrest(clan.clanId(), crestId);
			session.send(new PledgeShowInfoUpdate(clan));
			session.sendUserInfoAndBroadcastCharInfo();
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Brasao de cla atualizado com sucesso."));
		}
	}
}
