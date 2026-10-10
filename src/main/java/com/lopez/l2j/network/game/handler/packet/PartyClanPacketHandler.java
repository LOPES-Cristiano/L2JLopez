package com.lopez.l2j.network.game.handler.packet;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.party.Party;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.ActionFailed;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.ItemList;
import com.lopez.l2j.network.game.packet.GameServerPacket.PledgeCrest;
import com.lopez.l2j.network.game.packet.GameServerPacket.PledgeShowInfoUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.PledgeShowMemberListAll;
import com.lopez.l2j.network.game.packet.GameServerPacket.PledgeShowMemberListUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.SocialAction;
import com.lopez.l2j.network.game.packet.GameServerPacket.StatusUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import com.lopez.l2j.network.game.packet.GameServerPacket.UserInfo;

import java.util.List;

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
		if (ctx.away() != null && ctx.away().isAway(targetSession.character().objectId())) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", targetSession.character().name() + " esta no modo ausente (AFK)."));
			session.send(new ActionFailed());
			return;
		}
		if (ctx.preferences() != null && ctx.preferences().getPreferences(targetSession.character().objectId()).isBlockParty()) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", targetSession.character().name() + " esta recusando convites de grupo."));
			session.send(new ActionFailed());
			return;
		}
		if (com.lopez.l2j.config.Config.BLOCK_PARTY_INVITE_ON_COMBAT && (active.isInCombat() || targetSession.character().isInCombat())) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nao e permitido convidar ou aceitar grupo durante o modo combate."));
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

	public void createClan(String clanName) {
		var ctx = session.context();
		PlayerCharacter active = session.activeChar();
		if (ctx == null || ctx.clans() == null || active == null) {
			return;
		}
		if (active.clanId() != 0) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce ja pertence a um cla."));
			return;
		}
		if (active.level() < 10 && !active.isGm()) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nivel 10 ou superior necessario para criar cla."));
			return;
		}
		var clan = ctx.clans().createClan(active, clanName);
		if (clan == null) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nome de cla invalido ou ja existente."));
			return;
		}
		session.send(new PledgeShowInfoUpdate(clan));
		session.send(new PledgeShowMemberListAll(clan, 0));
		session.send(new PledgeShowMemberListUpdate(active.name(), active.level(), active.classId(), true));
		var tpl = ctx.characters() != null ? ctx.characters().template(active) : null;
		if (tpl != null) {
			session.send(new UserInfo(active, tpl));
		}
		session.broadcastAppearance();
		session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Cla " + clan.name() + " criado com sucesso!"));
	}

	public void increaseClanLevel() {
		var ctx = session.context();
		PlayerCharacter active = session.activeChar();
		if (ctx == null || ctx.clans() == null || active == null) {
			session.send(new ActionFailed());
			return;
		}
		if (active.clanId() == 0) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao pertence a um cla."));
			return;
		}
		var clanOpt = ctx.clans().byClanId(active.clanId());
		if (clanOpt.isEmpty() || !clanOpt.get().isLeader(active.objectId())) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Apenas o lider do cla pode aumentar seu nivel."));
			return;
		}
		var clan = clanOpt.get();
		if (clan.level() >= 8) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "O cla ja alcancou o nivel maximo (8)."));
			return;
		}
		boolean success = ctx.clans().levelUpClan(active);
		if (success) {
			session.send(new SocialAction(active.objectId(), 15));
			session.send(ItemList.of(active.inventory().items(), false));
			session.send(new StatusUpdate(active.objectId(),
					List.of(new StatusUpdate.Attribute(StatusUpdate.SP, active.sp()))));
			var tpl = ctx.characters() != null ? ctx.characters().template(active) : null;
			if (tpl != null) {
				session.send(new UserInfo(active, tpl));
			}
			session.broadcastAppearance();
			clan.broadcastToOnlineMembers(ctx.world(), new PledgeShowInfoUpdate(clan));
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
					"Parabens! O nivel do cla subiu para " + clan.level() + "!"));
		} else {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
					"Falha ao aumentar o nivel do cla. Requisitos nao atendidos."));
		}
	}

	public void dissolveClan() {
		var ctx = session.context();
		PlayerCharacter active = session.activeChar();
		if (ctx == null || ctx.clans() == null || active == null) {
			session.send(new ActionFailed());
			return;
		}
		if (active.clanId() == 0) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao pertence a um cla."));
			return;
		}
		var clanOpt = ctx.clans().byClanId(active.clanId());
		if (clanOpt.isEmpty() || !clanOpt.get().isLeader(active.objectId())) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Apenas o lider pode dissolver o cla."));
			return;
		}
		int oldClanId = active.clanId();
		ctx.clans().dissolveClan(oldClanId);
		active.clanId(0);
		var tpl = ctx.characters() != null ? ctx.characters().template(active) : null;
		if (tpl != null) {
			session.send(new UserInfo(active, tpl));
		}
		session.broadcastAppearance();
		session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "O cla foi dissolvido."));
	}
}
