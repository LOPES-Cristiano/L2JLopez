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

	private boolean isFestivalParticipant(PlayerCharacter player) {
		if (player == null) {
			return false;
		}
		var ctx = session.context();
		if (ctx != null && ctx.sevenSigns() != null) {
			return ctx.sevenSigns().isFestivalParticipant(player.objectId());
		}
		return false;
	}

	private boolean isPartyMemberInFestival(Party party, String memberName) {
		if (party == null) {
			return false;
		}
		var ctx = session.context();
		if (ctx == null || ctx.sevenSigns() == null) {
			return false;
		}
		for (var m : party.members()) {
			if (m.character() != null) {
				if (memberName == null || m.character().name().equalsIgnoreCase(memberName.trim())) {
					if (ctx.sevenSigns().isFestivalParticipant(m.character().objectId())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public void handleLeaveParty() {
		PlayerCharacter active = session.activeChar();
		Party party = session.party();
		if (party == null || active == null) {
			session.send(SystemMessage.id(200)); // YOU_ARE_NOT_IN_A_PARTY
			return;
		}
		if (isFestivalParticipant(active)) {
			session.send(SystemMessage.id(1275)); // FAILED_TO_WITHDRAW_FROM_PARTY
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao pode sair do grupo enquanto participa do Festival of Darkness."));
			return;
		}
		party.removeMember(session);
		session.party(null);
	}

	public void handleWithdrawalParty(GameClientPacket.RequestWithDrawalParty p) {
		handleLeaveParty();
	}

	public void handleWithdrawalParty() {
		handleLeaveParty();
	}

	public void handleDismissParty() {
		PlayerCharacter active = session.activeChar();
		Party party = session.party();
		if (party == null || active == null || !party.isLeader(active.objectId())) {
			return;
		}
		if (isFestivalParticipant(active) || isPartyMemberInFestival(party, null)) {
			session.send(SystemMessage.id(1275));
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nao e possivel desfazer o grupo durante o Festival of Darkness."));
			return;
		}
		for (var m : new java.util.ArrayList<>(party.members())) {
			party.removeMember(m);
			m.party(null);
		}
	}

	public void handleExpelPartyMember(String name) {
		PlayerCharacter active = session.activeChar();
		Party party = session.party();
		if (party != null && active != null && party.isLeader(active.objectId()) && name != null) {
			if (isFestivalParticipant(active) || isPartyMemberInFestival(party, name)) {
				session.send(SystemMessage.id(1275)); // FAILED_TO_EXPEL_THE_PARTY_MEMBER
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao pode expulsar membros do grupo durante o Festival of Darkness."));
				return;
			}
			party.oust(name.trim());
		}
	}

	public void handleOustPartyMember(GameClientPacket.RequestOustPartyMember p) {
		if (p != null) {
			handleExpelPartyMember(p.name());
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
			session.send(new GameServerPacket.PledgeInfo(clan.clanId(), clan.name(), clan.allyName()));
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
		boolean hasPriv = clan != null && (clan.isLeader(active.objectId())
				|| (ctx.clanPrivileges() != null && ctx.clanPrivileges().hasPrivilege(active, com.lopez.l2j.game.clan.Clan.CP_CL_REGISTER_CREST)));
		if (clan == null || !hasPriv) {
			session.send(SystemMessage.id(SystemMessage.NOT_AUTHORIZED_TO_BESTOW_RIGHTS));
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao possui privilegio para alterar o brasao do cla."));
			return;
		}
		if (clan.dissolvingExpiryTime() > System.currentTimeMillis()) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nao e permitido registrar ou remover brasao durante dissolucao do cla."));
			return;
		}
		if (p.data() == null || p.data().length == 0) {
			if (clan.crestId() != 0) {
				ctx.crests().removePledgeCrest(clan.crestId());
				ctx.clans().updateCrest(clan.clanId(), 0);
				session.send(new PledgeShowInfoUpdate(clan));
				if (ctx.world() != null) {
					clan.broadcastToOnlineMembers(ctx.world(), new PledgeShowInfoUpdate(clan));
				}
				session.sendUserInfoAndBroadcastCharInfo();
				session.send(SystemMessage.id(1861)); // CLAN_CREST_HAS_BEEN_DELETED
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Brasao do cla removido."));
			}
			return;
		}
		if (clan.level() < 3) {
			session.send(SystemMessage.id(272)); // CLAN_LVL_3_NEEDED_TO_SET_CREST
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
					"Cla nivel 3 ou superior e necessario para definir brasao."));
			return;
		}
		if (p.data().length > 256) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "O arquivo do brasao nao pode exceder 256 bytes (16x12 BMP 256 cores)."));
			return;
		}
		if (clan.crestId() != 0) {
			ctx.crests().removePledgeCrest(clan.crestId());
		}
		int crestId = ctx.crests().savePledgeCrest(p.data());
		if (crestId != 0) {
			ctx.clans().updateCrest(clan.clanId(), crestId);
			session.send(new PledgeShowInfoUpdate(clan));
			if (ctx.world() != null) {
				clan.broadcastToOnlineMembers(ctx.world(), new PledgeShowInfoUpdate(clan));
			}
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
			session.send(SystemMessage.id(SystemMessage.NOT_AUTHORIZED_TO_BESTOW_RIGHTS));
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Apenas o lider pode dissolver o cla."));
			return;
		}
		var clan = clanOpt.get();
		if (clan.hasCastle() || clan.hasClanHall() || clan.fortId() > 0) {
			session.send(SystemMessage.id(263)); // CANNOT_DISPERSE_THE_CLANS_IN_ALLY / HAS_CASTLE_OR_HIDEOUT
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nao e possivel dissolver um cla que possui Castelo, Fortaleza ou Clan Hall."));
			return;
		}
		if (!clan.enemyClanIds().isEmpty() || !clan.attackerClanIds().isEmpty()) {
			session.send(SystemMessage.id(264)); // CANNOT_DISSOLVE_WHILE_IN_WAR
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nao e possivel dissolver o cla durante uma guerra de clas."));
			return;
		}
		if (ctx.sieges() != null) {
			for (var siege : ctx.sieges().allSieges()) {
				if (siege.attackers().containsKey(clan.clanId()) || siege.defenders().containsKey(clan.clanId())) {
					session.send(SystemMessage.id(265)); // CANNOT_DISSOLVE_WHILE_IN_SIEGE
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nao e possivel dissolver o cla enquanto inscrito em uma Siege."));
					return;
				}
			}
		}
		if (clan.dissolvingExpiryTime() > System.currentTimeMillis()) {
			session.send(SystemMessage.id(266)); // ALREADY_REQUESTED_DISSOLUTION
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "A dissolucao do cla ja foi solicitada."));
			return;
		}
		if (com.lopez.l2j.game.clan.ClanTable.getDissolveDelayMillis() > 0L) {
			ctx.clans().requestDissolveClan(clan, active);
			session.send(new PledgeShowInfoUpdate(clan));
			if (ctx.world() != null) {
				clan.broadcastToOnlineMembers(ctx.world(), new PledgeShowInfoUpdate(clan));
			}
			session.send(SystemMessage.id(193)); // CLAN_SKILL_WILL_BE_DELETED_AFTER_DISSOLUTION
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Pedido de dissolucao do cla registrado."));
		} else {
			int oldClanId = active.clanId();
			if (ctx.clanSkills() != null) {
				ctx.clanSkills().removeClanSkillsFromPlayer(active);
			}
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

	public void recoverClan() {
		var ctx = session.context();
		PlayerCharacter active = session.activeChar();
		if (ctx == null || ctx.clans() == null || active == null || active.clanId() <= 0) {
			session.send(new ActionFailed());
			return;
		}
		var clanOpt = ctx.clans().byClanId(active.clanId());
		if (clanOpt.isEmpty() || !clanOpt.get().isLeader(active.objectId())) {
			session.send(SystemMessage.id(SystemMessage.NOT_AUTHORIZED_TO_BESTOW_RIGHTS));
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Apenas o lider pode recuperar o cla."));
			return;
		}
		var clan = clanOpt.get();
		if (clan.dissolvingExpiryTime() <= 0) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nao ha pedidos de dissolucao ativos para este cla."));
			return;
		}
		clan.dissolvingExpiryTime(0L);
		active.clanCreateExpiryTime(0L);
		session.send(new PledgeShowInfoUpdate(clan));
		if (ctx.world() != null) {
			clan.broadcastToOnlineMembers(ctx.world(), new PledgeShowInfoUpdate(clan));
		}
		session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "A dissolucao do cla " + clan.name() + " foi cancelada!"));
	}

	// ---- Clan Wars, Alliance, Siege UI & Privileges ----

	private int pendingAllyInviterId;

	public void handleStartPledgeWar(GameClientPacket.RequestStartPledgeWar p) {
		var ctx = session.context();
		PlayerCharacter active = session.activeChar();
		if (ctx == null || ctx.clanWars() == null || active == null) {
			session.send(new ActionFailed());
			return;
		}
		if (ctx.clanPrivileges() != null && !ctx.clanPrivileges().hasPrivilege(active, com.lopez.l2j.game.clan.Clan.CP_CL_PLEDGE_WAR)) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao possui privilegio para declarar guerra de cla."));
			session.send(new ActionFailed());
			return;
		}
		var res = ctx.clanWars().declareWar(active, p.pledgeName());
		if (res == com.lopez.l2j.game.clan.war.ClanWarService.DeclareResult.SUCCESS) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Guerra de cla declarada contra " + p.pledgeName() + "!"));
			session.sendUserInfoAndBroadcastCharInfo();
		} else {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nao foi possivel declarar guerra: " + res.name()));
			session.send(new ActionFailed());
		}
	}

	public void handleStopPledgeWar(GameClientPacket.RequestStopPledgeWar p) {
		var ctx = session.context();
		PlayerCharacter active = session.activeChar();
		if (ctx == null || ctx.clanWars() == null || active == null) {
			session.send(new ActionFailed());
			return;
		}
		if (ctx.clanPrivileges() != null && !ctx.clanPrivileges().hasPrivilege(active, com.lopez.l2j.game.clan.Clan.CP_CL_PLEDGE_WAR)) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao possui privilegio para encerrar guerra de cla."));
			session.send(new ActionFailed());
			return;
		}
		var res = ctx.clanWars().stopWar(active, p.pledgeName());
		if (res == com.lopez.l2j.game.clan.war.ClanWarService.StopResult.SUCCESS) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Guerra de cla encerrada com " + p.pledgeName() + "."));
			session.sendUserInfoAndBroadcastCharInfo();
		} else {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nao foi possivel encerrar guerra: " + res.name()));
			session.send(new ActionFailed());
		}
	}

	public void handleJoinAlly(GameClientPacket.RequestJoinAlly p) {
		var ctx = session.context();
		PlayerCharacter active = session.activeChar();
		if (ctx == null || ctx.alliances() == null || ctx.world() == null || active == null) {
			session.send(new ActionFailed());
			return;
		}
		var targetOpt = ctx.world().player(p.targetObjId());
		if (targetOpt.isEmpty() || !(targetOpt.get() instanceof GameSession targetSession) || targetSession.activeChar() == null) {
			session.send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
			return;
		}
		var check = ctx.alliances().checkAllyJoinCondition(active, targetSession.activeChar());
		if (check == com.lopez.l2j.game.clan.alliance.AllianceService.InviteResult.SUCCESS) {
			targetSession.partyClanHandler().pendingAllyInviterId = active.objectId();
			targetSession.send(new GameServerPacket.AskJoinAlly(active.objectId(), active.name()));
			targetSession.send(new CreatureSay(0, CreatureSay.ALL, "SYS", active.name() + " convidou seu cla para a alianca."));
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Convite de alianca enviado para " + targetSession.activeChar().name() + "."));
		} else {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Falha ao convidar para alianca: " + check.name()));
		}
	}

	public void handleAnswerJoinAlly(GameClientPacket.RequestAnswerJoinAlly p) {
		var ctx = session.context();
		PlayerCharacter active = session.activeChar();
		int inviterId = this.pendingAllyInviterId;
		this.pendingAllyInviterId = 0;
		if (ctx == null || ctx.alliances() == null || ctx.clans() == null || ctx.world() == null || active == null || inviterId == 0) {
			return;
		}
		if (p.response() != 1) {
			return;
		}
		var inviterOpt = ctx.world().player(inviterId);
		if (inviterOpt.isEmpty() || inviterOpt.get().character() == null) {
			return;
		}
		var inviterClan = ctx.clans().byClanId(inviterOpt.get().character().clanId()).orElse(null);
		var myClan = ctx.clans().byClanId(active.clanId()).orElse(null);
		if (inviterClan != null && myClan != null && inviterClan.allyId() > 0) {
			if (ctx.alliances().addClanToAlliance(inviterClan.allyId(), myClan)) {
				session.sendUserInfoAndBroadcastCharInfo();
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Seu cla ingressou na alianca " + inviterClan.allyName() + "!"));
			}
		}
	}

	public void handleSiegeAttackerList(GameClientPacket.RequestSiegeAttackerList p) {
		var ctx = session.context();
		if (ctx == null || ctx.sieges() == null) {
			return;
		}
		var siegeOpt = ctx.sieges().getSiege(p.castleId());
		java.util.List<Integer> clanIds = new java.util.ArrayList<>();
		java.util.List<String> clanNames = new java.util.ArrayList<>();
		java.util.List<String> leaderNames = new java.util.ArrayList<>();
		if (siegeOpt.isPresent() && ctx.clans() != null) {
			for (int cid : siegeOpt.get().attackers().keySet()) {
				clanIds.add(cid);
				var cOpt = ctx.clans().byClanId(cid);
				clanNames.add(cOpt.map(com.lopez.l2j.game.clan.Clan::name).orElse("Clan #" + cid));
				leaderNames.add(cOpt.map(com.lopez.l2j.game.clan.Clan::leaderName).orElse(""));
			}
		}
		session.send(new GameServerPacket.SiegeAttackerList(p.castleId(), clanIds, clanNames, leaderNames));
	}

	public void handleSiegeDefenderList(GameClientPacket.RequestSiegeDefenderList p) {
		var ctx = session.context();
		if (ctx == null || ctx.sieges() == null) {
			return;
		}
		var siegeOpt = ctx.sieges().getSiege(p.castleId());
		java.util.List<Integer> clanIds = new java.util.ArrayList<>();
		java.util.List<String> clanNames = new java.util.ArrayList<>();
		java.util.List<String> leaderNames = new java.util.ArrayList<>();
		if (siegeOpt.isPresent() && ctx.clans() != null) {
			for (int cid : siegeOpt.get().defenders().keySet()) {
				clanIds.add(cid);
				var cOpt = ctx.clans().byClanId(cid);
				clanNames.add(cOpt.map(com.lopez.l2j.game.clan.Clan::name).orElse("Clan #" + cid));
				leaderNames.add(cOpt.map(com.lopez.l2j.game.clan.Clan::leaderName).orElse(""));
			}
		}
		session.send(new GameServerPacket.SiegeDefenderList(p.castleId(), clanIds, clanNames, leaderNames));
	}

	public void handleJoinSiege(GameClientPacket.RequestJoinSiege p) {
		var ctx = session.context();
		PlayerCharacter active = session.activeChar();
		if (ctx == null || ctx.sieges() == null || ctx.clans() == null || active == null || active.clanId() <= 0) {
			session.send(new ActionFailed());
			return;
		}
		var clanOpt = ctx.clans().byClanId(active.clanId());
		boolean canManageSiege = clanOpt.isPresent() && (clanOpt.get().isLeader(active.objectId())
				|| (ctx.clanPrivileges() != null && ctx.clanPrivileges().hasPrivilege(active, com.lopez.l2j.game.clan.Clan.CP_CS_MANAGE_SIEGE)));
		if (clanOpt.isEmpty() || !canManageSiege) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao possui privilegio para gerenciar inscricoes de Siege."));
			return;
		}
		if (p.isJoining() == 1) {
			boolean ok = p.isAttacker() == 1
					? ctx.sieges().registerAttacker(p.castleId(), clanOpt.get())
					: ctx.sieges().registerDefender(p.castleId(), clanOpt.get());
			if (ok) {
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Cla inscrito na Siege do castelo " + p.castleId() + "!"));
			} else {
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nao foi possivel inscrever o cla na Siege."));
			}
		}
		if (p.isAttacker() == 1) {
			handleSiegeAttackerList(new GameClientPacket.RequestSiegeAttackerList(p.castleId()));
		} else {
			handleSiegeDefenderList(new GameClientPacket.RequestSiegeDefenderList(p.castleId()));
		}
	}

	public void handleConfirmSiegeWaitingList(GameClientPacket.RequestConfirmSiegeWaitingList p) {
		var ctx = session.context();
		PlayerCharacter active = session.activeChar();
		if (ctx == null || ctx.sieges() == null || active == null || active.clanId() <= 0) {
			return;
		}
		if (ctx.clanPrivileges() != null && !ctx.clanPrivileges().hasPrivilege(active, com.lopez.l2j.game.clan.Clan.CP_CS_MANAGE_SIEGE)) {
			return;
		}
		if (p.approved() == 1) {
			ctx.sieges().approveDefender(p.castleId(), p.clanId(), active.clanId());
		}
		handleSiegeDefenderList(new GameClientPacket.RequestSiegeDefenderList(p.castleId()));
	}

	public void handlePledgePower(GameClientPacket.RequestPledgePower p) {
		var ctx = session.context();
		PlayerCharacter active = session.activeChar();
		if (ctx == null || ctx.clans() == null || active == null || active.clanId() <= 0) {
			return;
		}
		var clanOpt = ctx.clans().byClanId(active.clanId());
		if (clanOpt.isEmpty()) {
			return;
		}
		var clan = clanOpt.get();
		if (p.action() == 2) {
			boolean updated;
			if (ctx.clanPrivileges() != null) {
				updated = ctx.clanPrivileges().setRankPrivilege(active, p.rank(), p.privs());
			} else if (clan.isLeader(active.objectId())) {
				clan.setRankPrivilege(p.rank(), p.privs());
				updated = true;
			} else {
				updated = false;
			}
			if (updated) {
				session.send(new PledgeShowInfoUpdate(clan));
			}
		} else {
			int privs = clan.getRankPrivilege(p.rank());
			session.send(new GameServerPacket.ManagePledgePower(active.clanId(), p.action(), p.rank(), privs));
		}
	}

	private int pendingClanInviterId;
	private int pendingClanPledgeType;

	public void handleJoinPledge(GameClientPacket.RequestJoinPledge p) {
		var ctx = session.context();
		PlayerCharacter active = session.activeChar();
		if (ctx == null || ctx.clans() == null || ctx.world() == null || active == null || active.clanId() <= 0) {
			session.send(new ActionFailed());
			return;
		}
		var clanOpt = ctx.clans().byClanId(active.clanId());
		if (clanOpt.isEmpty()) {
			session.send(new ActionFailed());
			return;
		}
		var clan = clanOpt.get();
		boolean canJoin = clan.isLeader(active.objectId())
				|| (ctx.clanPrivileges() != null && ctx.clanPrivileges().hasPrivilege(active, com.lopez.l2j.game.clan.Clan.CP_CL_JOIN_CLAN));
		if (!canJoin) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao possui privilegio para convidar membros ao cla."));
			session.send(new ActionFailed());
			return;
		}
		var targetOpt = ctx.world().player(p.targetId());
		if (targetOpt.isEmpty() || !(targetOpt.get() instanceof GameSession targetSession) || targetSession.activeChar() == null) {
			session.send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
			return;
		}
		PlayerCharacter target = targetSession.activeChar();
		if (target.clanId() != 0) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", target.name() + " ja pertence a um cla."));
			return;
		}
		if (target.hasClanJoinPenalty()) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", target.name() + " possui penalidade ativa para ingressar em cla."));
			return;
		}
		if (clan.hasCharPenalty()) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Seu cla possui penalidade ativa para recrutar novos membros."));
			return;
		}
		if (p.pledgeType() == com.lopez.l2j.game.clan.Clan.SUBUNIT_ACADEMY && (target.level() >= 40 || target.classId() >= 20)) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Apenas personagens abaixo do nivel 40 sem 2a classe podem entrar na Academia."));
			return;
		}
		targetSession.partyClanHandler().pendingClanInviterId = active.objectId();
		targetSession.partyClanHandler().pendingClanPledgeType = p.pledgeType();
		targetSession.send(new GameServerPacket.AskJoinPledge(active.objectId(), clan.name()));
	}

	public void handleAnswerJoinPledge(GameClientPacket.RequestAnswerJoinPledge p) {
		var ctx = session.context();
		PlayerCharacter active = session.activeChar();
		int inviterId = this.pendingClanInviterId;
		int pledgeType = this.pendingClanPledgeType;
		this.pendingClanInviterId = 0;
		this.pendingClanPledgeType = 0;
		if (ctx == null || ctx.clans() == null || ctx.world() == null || active == null || inviterId == 0) {
			return;
		}
		var inviterOpt = ctx.world().player(inviterId);
		if (inviterOpt.isEmpty() || !(inviterOpt.get() instanceof GameSession inviterSession) || inviterSession.activeChar() == null) {
			return;
		}
		if (p.answer() == 0) {
			inviterSession.send(new CreatureSay(0, CreatureSay.ALL, "SYS", active.name() + " recusou o convite para o cla."));
			return;
		}
		var clanOpt = ctx.clans().byClanId(inviterSession.activeChar().clanId());
		if (clanOpt.isEmpty()) {
			return;
		}
		var clan = clanOpt.get();
		if (ctx.clans().addClanMember(clan, active)) {
			var member = clan.getMember(active.objectId());
			if (member != null) {
				member.pledgeType(pledgeType);
				member.setOnline(true);
				active.subPledge(pledgeType);
				active.pledgeRank(member.powerGrade());
			}
			if (ctx.clanSkills() != null) {
				ctx.clanSkills().applyClanSkillsToPlayer(active);
			}
			session.send(new GameServerPacket.JoinPledge(clan.clanId()));
			session.send(new PledgeShowInfoUpdate(clan));
			session.send(new PledgeShowMemberListAll(clan, pledgeType));
			clan.broadcastToOnlineMembers(ctx.world(), new PledgeShowMemberListUpdate(active.name(), active.level(), active.classId(), true));
			session.sendSkillList();
			session.sendUserInfoAndBroadcastCharInfo();
		}
	}

	public void handleWithdrawalPledge() {
		var ctx = session.context();
		PlayerCharacter active = session.activeChar();
		if (ctx == null || ctx.clans() == null || active == null || active.clanId() <= 0) {
			session.send(new ActionFailed());
			return;
		}
		var clanOpt = ctx.clans().byClanId(active.clanId());
		if (clanOpt.isEmpty()) {
			return;
		}
		var clan = clanOpt.get();
		if (clan.isLeader(active.objectId())) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "O lider nao pode abandonar o cla diretamente."));
			return;
		}
		if (ctx.clans().removeClanMember(clan, active, false)) {
			if (ctx.clanSkills() != null) {
				ctx.clanSkills().removeClanSkillsFromPlayer(active);
			}
			active.title("");
			active.clanPrivileges(0);
			active.pledgeRank(0);
			active.subPledge(0);
			session.sendSkillList();
			session.sendUserInfoAndBroadcastCharInfo();
			clan.broadcastToOnlineMembers(ctx.world(), new PledgeShowInfoUpdate(clan));
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce saiu do cla " + clan.name() + "."));
		}
	}

	public void handleOustPledgeMember(GameClientPacket.RequestOustPledgeMember p) {
		var ctx = session.context();
		PlayerCharacter active = session.activeChar();
		if (ctx == null || ctx.clans() == null || active == null || active.clanId() <= 0 || p.target() == null) {
			session.send(new ActionFailed());
			return;
		}
		var clanOpt = ctx.clans().byClanId(active.clanId());
		if (clanOpt.isEmpty()) {
			return;
		}
		var clan = clanOpt.get();
		boolean canDismiss = clan.isLeader(active.objectId())
				|| (ctx.clanPrivileges() != null && ctx.clanPrivileges().hasPrivilege(active, com.lopez.l2j.game.clan.Clan.CP_CL_DISMISS));
		if (!canDismiss) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao possui privilegio para expulsar membros do cla."));
			return;
		}
		com.lopez.l2j.game.clan.ClanMember targetMember = null;
		for (var m : clan.members()) {
			if (m.name().equalsIgnoreCase(p.target().trim())) {
				targetMember = m;
				break;
			}
		}
		if (targetMember == null || clan.isLeader(targetMember.objectId())) {
			return;
		}
		var targetOnlineOpt = ctx.world() != null ? ctx.world().player(targetMember.objectId()) : java.util.Optional.<com.lopez.l2j.game.world.GameWorld.OnlinePlayer>empty();
		final com.lopez.l2j.game.clan.ClanMember foundMember = targetMember;
		PlayerCharacter targetChar = targetOnlineOpt.map(com.lopez.l2j.game.world.GameWorld.OnlinePlayer::character).orElseGet(() -> { var pc = new PlayerCharacter(foundMember.objectId(), foundMember.name()); pc.clanId(clan.clanId()); return pc; }); /*
				.orElseGet(() -> new PlayerCharacter(targetMember.objectId(), "", targetMember.name(), 0, 0, targetMember.classId(),
						targetMember.level(), 0, 0, 100, 100, 100, 100, 100, 100, 0, 0, 0, false, 0, 0, 0, 0, 0, 0, 0,
						targetMember.title(), clan.clanId(), null, null)); */
		if (ctx.clans().removeClanMember(clan, targetChar, true)) {
			if (targetOnlineOpt.isPresent() && targetOnlineOpt.get() instanceof GameSession targetGs) {
				if (ctx.clanSkills() != null) {
					ctx.clanSkills().removeClanSkillsFromPlayer(targetChar);
				}
				targetChar.title("");
				targetChar.clanPrivileges(0);
				targetChar.pledgeRank(0);
				targetChar.subPledge(0);
				targetGs.sendSkillList();
				targetGs.sendUserInfoAndBroadcastCharInfo();
				targetGs.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce foi expulso do cla " + clan.name() + "."));
			}
			session.send(new PledgeShowMemberListAll(clan, 0));
			clan.broadcastToOnlineMembers(ctx.world(), new PledgeShowInfoUpdate(clan));
		}
	}

	public void handleGiveNickName(GameClientPacket.RequestGiveNickName p) {
		var ctx = session.context();
		PlayerCharacter active = session.activeChar();
		if (ctx == null || ctx.clans() == null || active == null || active.clanId() <= 0 || p.target() == null) {
			session.send(new ActionFailed());
			return;
		}
		var clanOpt = ctx.clans().byClanId(active.clanId());
		if (clanOpt.isEmpty()) {
			return;
		}
		var clan = clanOpt.get();
		if (clan.level() < 3) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "O cla precisa estar no nivel 3 ou superior para conceder titulos."));
			return;
		}
		boolean canGiveTitle = clan.isLeader(active.objectId())
				|| (ctx.clanPrivileges() != null && ctx.clanPrivileges().hasPrivilege(active, com.lopez.l2j.game.clan.Clan.CP_CL_GIVE_TITLE));
		if (!canGiveTitle) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao possui privilegio para alterar titulos."));
			return;
		}
		String title = p.title() != null ? p.title().trim() : "";
		if (title.length() > 16) {
			title = title.substring(0, 16);
		}
		for (var m : clan.members()) {
			if (m.name().equalsIgnoreCase(p.target().trim())) {
				m.title(title);
				if (ctx.world() != null) {
					String finalTitle = title;
					ctx.world().player(m.objectId()).ifPresent(op -> {
						if (op.character() != null) {
							op.character().title(finalTitle);
							if (ctx.characters() != null) {
								ctx.characters().save(op.character(), true);
							}
						}
						op.send(new GameServerPacket.TitleUpdate(m.objectId(), finalTitle));
						op.sendUserInfoAndBroadcastCharInfo();
					});
				}
				session.send(new PledgeShowMemberListAll(clan, 0));
				return;
			}
		}
	}

	public void handleExPledgeCrestLarge(GameClientPacket.RequestExPledgeCrestLarge p) {
		var ctx = session.context();
		if (p.crestId() == 0 || ctx == null || ctx.crests() == null) {
			return;
		}
		byte[] data = ctx.crests().getPledgeCrestLarge(p.crestId());
		if (data != null) {
			session.send(new GameServerPacket.ExPledgeCrestLarge(p.crestId(), data));
		}
	}

	public void handleExSetPledgeCrestLarge(GameClientPacket.RequestExSetPledgeCrestLarge p) {
		var ctx = session.context();
		PlayerCharacter active = session.activeChar();
		if (ctx == null || ctx.clans() == null || ctx.crests() == null || active == null || active.clanId() <= 0) {
			return;
		}
		var clan = ctx.clans().byClanId(active.clanId()).orElse(null);
		boolean hasPriv = clan != null && (clan.isLeader(active.objectId())
				|| (ctx.clanPrivileges() != null && ctx.clanPrivileges().hasPrivilege(active, com.lopez.l2j.game.clan.Clan.CP_CL_REGISTER_CREST)));
		if (clan == null || !hasPriv) {
			session.send(SystemMessage.id(SystemMessage.NOT_AUTHORIZED_TO_BESTOW_RIGHTS));
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao possui privilegio para registrar insignia grande."));
			return;
		}
		if (p.data() == null || p.data().length == 0) {
			if (clan.crestLargeId() != 0) {
				ctx.crests().removePledgeCrestLarge(clan.crestLargeId());
				ctx.clans().updateCrestLarge(clan.clanId(), 0);
				session.send(new PledgeShowInfoUpdate(clan));
				if (ctx.world() != null) {
					clan.broadcastToOnlineMembers(ctx.world(), new PledgeShowInfoUpdate(clan));
				}
				session.sendUserInfoAndBroadcastCharInfo();
				session.send(SystemMessage.id(1861));
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Insignia grande removida."));
			}
			return;
		}
		if (clan.castleId() <= 0 && clan.clanHallId() <= 0) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Apenas clas com Castelo ou Clan Hall podem registrar insignia grande."));
			return;
		}
		if (p.data().length > 2176) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "O arquivo da insignia grande nao pode exceder 2176 bytes (64x64 BMP 256 cores)."));
			return;
		}
		if (clan.crestLargeId() != 0) {
			ctx.crests().removePledgeCrestLarge(clan.crestLargeId());
		}
		int crestId = ctx.crests().savePledgeCrestLarge(p.data());
		ctx.clans().updateCrestLarge(clan.clanId(), crestId);
		session.send(new PledgeShowInfoUpdate(clan));
		if (ctx.world() != null) {
			clan.broadcastToOnlineMembers(ctx.world(), new PledgeShowInfoUpdate(clan));
		}
		session.sendUserInfoAndBroadcastCharInfo();
		session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Insignia grande registrada com sucesso."));
	}

	public void handleAllyCrest(GameClientPacket.RequestAllyCrest p) {
		var ctx = session.context();
		if (p == null || p.crestId() == 0 || ctx == null || ctx.crests() == null) {
			return;
		}
		byte[] data = ctx.crests().getAllyCrest(p.crestId());
		if (data != null) {
			session.send(new GameServerPacket.AllyCrest(p.crestId(), data));
		}
	}

	public void handleSetAllyCrest(GameClientPacket.RequestSetAllyCrest p) {
		var ctx = session.context();
		PlayerCharacter active = session.activeChar();
		if (p == null || ctx == null || ctx.clans() == null || ctx.crests() == null || active == null || active.clanId() <= 0) {
			return;
		}
		var clan = ctx.clans().byClanId(active.clanId()).orElse(null);
		if (clan == null || clan.allyId() <= 0 || clan.allyId() != clan.clanId() || !clan.isLeader(active.objectId())) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Apenas o lider da alianca pode definir o brasao da alianca."));
			return;
		}
		if (p.data() == null || p.data().length == 0) {
			if (clan.allyCrestId() != 0) {
				ctx.crests().removeAllyCrest(clan.allyCrestId());
				for (var c : ctx.clans().allClans()) {
					if (c.allyId() == clan.allyId()) {
						ctx.clans().updateAllyCrest(c.clanId(), 0);
						if (ctx.world() != null) {
							c.broadcastToOnlineMembers(ctx.world(), new PledgeShowInfoUpdate(c));
						}
					}
				}
				session.sendUserInfoAndBroadcastCharInfo();
				session.send(SystemMessage.id(1861));
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Brasao da alianca removido."));
			}
			return;
		}
		if (p.data().length > 192) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "O arquivo do brasao da alianca nao pode exceder 192 bytes (8x12 BMP 256 cores)."));
			return;
		}
		if (clan.allyCrestId() != 0) {
			ctx.crests().removeAllyCrest(clan.allyCrestId());
		}
		int crestId = ctx.crests().saveAllyCrest(p.data());
		if (crestId != 0) {
			for (var c : ctx.clans().allClans()) {
				if (c.allyId() == clan.allyId()) {
					ctx.clans().updateAllyCrest(c.clanId(), crestId);
					if (ctx.world() != null) {
						c.broadcastToOnlineMembers(ctx.world(), new PledgeShowInfoUpdate(c));
					}
				}
			}
			session.sendUserInfoAndBroadcastCharInfo();
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Brasao da alianca atualizado com sucesso."));
		}
	}

	public void handleSetPledgeAcademyMaster(GameClientPacket.RequestPledgeSetAcademyMaster p) {
		var ctx = session.context();
		PlayerCharacter active = session.activeChar();
		if (p == null || ctx == null || ctx.clans() == null || active == null || active.clanId() <= 0) {
			return;
		}
		var clan = ctx.clans().byClanId(active.clanId()).orElse(null);
		if (clan == null) {
			return;
		}
		boolean hasPriv = clan.isLeader(active.objectId())
				|| (ctx.clanPrivileges() != null && ctx.clanPrivileges().hasPrivilege(active, com.lopez.l2j.game.clan.Clan.CP_CL_MASTER_RIGHTS));
		if (!hasPriv) {
			session.send(SystemMessage.id(SystemMessage.NOT_AUTHORIZED_TO_BESTOW_RIGHTS));
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao possui privilegio para gerenciar padrinhos da Academia."));
			return;
		}
		com.lopez.l2j.game.clan.ClanMember currMember = null;
		com.lopez.l2j.game.clan.ClanMember targetMember = null;
		for (var m : clan.members()) {
			if (p.currPlayerName() != null && m.name().equalsIgnoreCase(p.currPlayerName().trim())) {
				currMember = m;
			}
			if (p.targetPlayerName() != null && m.name().equalsIgnoreCase(p.targetPlayerName().trim())) {
				targetMember = m;
			}
		}
		if (currMember == null || targetMember == null) {
			return;
		}
		com.lopez.l2j.game.clan.ClanMember apprentice;
		com.lopez.l2j.game.clan.ClanMember sponsor;
		if (currMember.pledgeType() == com.lopez.l2j.game.clan.Clan.SUBUNIT_ACADEMY) {
			apprentice = currMember;
			sponsor = targetMember;
		} else if (targetMember.pledgeType() == com.lopez.l2j.game.clan.Clan.SUBUNIT_ACADEMY) {
			apprentice = targetMember;
			sponsor = currMember;
		} else {
			return;
		}
		if (p.set() == 1) {
			if (apprentice.sponsor() != 0 || sponsor.apprentice() != 0) {
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Um dos membros selecionados ja possui vinculo de padrinho/aprendiz."));
				return;
			}
			if (ctx.clans().initApprenticeAndSponsor(clan, apprentice.objectId(), sponsor.objectId())) {
				session.send(SystemMessage.of(1755,
						new SystemMessage.Text(apprentice.name()),
						new SystemMessage.Text(sponsor.name()))); // S2_HAS_BEEN_DESIGNATED_AS_APPRENTICE_OF_CLAN_MEMBER_S1
				session.send(new PledgeShowMemberListAll(clan, 0));
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						sponsor.name() + " foi designado padrinho do aprendiz " + apprentice.name() + "."));
			}
		} else {
			if (ctx.clans().removeApprenticeAndSponsor(clan, apprentice.objectId(), sponsor.objectId())) {
				session.send(SystemMessage.of(1763,
						new SystemMessage.Text(apprentice.name()),
						new SystemMessage.Text(sponsor.name()))); // S2_CLAN_MEMBER_S1_APPRENTICE_HAS_BEEN_REMOVED
				session.send(new PledgeShowMemberListAll(clan, 0));
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Vinculo de padrinho entre " + sponsor.name() + " e " + apprentice.name() + " removido."));
			}
		}
	}

	public void handlePledgeSetAcademyMaster(GameClientPacket.RequestPledgeSetAcademyMaster p) {
		handleSetPledgeAcademyMaster(p);
	}

	public void handlePledgePowerGradeList() {
		var ctx = session.context();
		PlayerCharacter active = session.activeChar();
		if (ctx == null || ctx.clans() == null || active == null || active.clanId() <= 0) {
			return;
		}
		var clan = ctx.clans().byClanId(active.clanId()).orElse(null);
		if (clan == null) {
			return;
		}
		int[] counts = new int[10];
		for (var m : clan.members()) {
			int g = m.powerGrade();
			if (g >= 1 && g <= 9) {
				counts[g]++;
			}
		}
		java.util.Map<Integer, Integer> grades = new java.util.HashMap<>(9); for (int i = 1; i <= 9; i++) grades.put(i, counts[i]); session.send(new GameServerPacket.PledgePowerGradeList(grades)); /*
		for (int i = 1; i <= 9; i++) {
			grades.add(new GameServerPacket.PledgePowerGradeList.GradeInfo(i, counts[i]));
		}
		*/
	}

	public void handlePledgeMemberPowerInfo(GameClientPacket.RequestPledgeMemberPowerInfo p) {
		var ctx = session.context();
		PlayerCharacter active = session.activeChar();
		if (ctx == null || ctx.clans() == null || active == null || active.clanId() <= 0 || p.player() == null) {
			return;
		}
		var clan = ctx.clans().byClanId(active.clanId()).orElse(null);
		if (clan == null) {
			return;
		}
		for (var m : clan.members()) {
			if (m.name().equalsIgnoreCase(p.player().trim())) {
				int privs = clan.isLeader(m.objectId()) ? com.lopez.l2j.game.clan.Clan.CP_ALL : clan.getRankPrivilege(m.powerGrade());
				session.send(new GameServerPacket.PledgeReceivePowerInfo(m.powerGrade(), m.name(), privs));
				return;
			}
		}
	}

	public void handlePledgeSetMemberPowerGrade(GameClientPacket.RequestPledgeSetMemberPowerGrade p) {
		if (handleMemberPowerGrade(p.member(), p.powerGrade())) {
			handlePledgeMemberPowerInfo(new GameClientPacket.RequestPledgeMemberPowerInfo(0, p.member()));
			handlePledgePowerGradeList();
		}
	}

	public void handlePledgeMemberInfo(GameClientPacket.RequestPledgeMemberInfo p) {
		var ctx = session.context();
		PlayerCharacter active = session.activeChar();
		if (ctx == null || ctx.clans() == null || active == null || active.clanId() <= 0 || p.player() == null) {
			return;
		}
		var clan = ctx.clans().byClanId(active.clanId()).orElse(null);
		if (clan == null) {
			return;
		}
		for (var m : clan.members()) {
			if (m.name().equalsIgnoreCase(p.player().trim())) {
				String subName = clan.name();
				var sub = clan.getSubPledge(m.pledgeType());
				if (sub != null && sub.name() != null) {
					subName = sub.name();
				}
				String appName = "";
				if (m.apprentice() > 0) {
					var app = clan.getMember(m.apprentice());
					if (app != null) appName = app.name();
				} else if (m.sponsor() > 0) {
					var sp = clan.getMember(m.sponsor());
					if (sp != null) appName = sp.name();
				}
				session.send(new GameServerPacket.PledgeReceiveMemberInfo(m.pledgeType(), m.name(), m.title(), m.powerGrade(), subName, appName));
				return;
			}
		}
	}

	public void handlePledgeWarList(GameClientPacket.RequestPledgeWarList p) {
		var ctx = session.context();
		PlayerCharacter active = session.activeChar();
		if (ctx == null || ctx.clans() == null || active == null || active.clanId() <= 0) {
			return;
		}
		var clan = ctx.clans().byClanId(active.clanId()).orElse(null);
		if (clan == null) {
			return;
		}
		java.util.Set<Integer> warIds = p.tab() == 0 ? clan.enemyClanIds() : clan.attackerClanIds();
		List<GameServerPacket.PledgeReceiveWarList.WarEntry> entries = new java.util.ArrayList<>();
		for (int cid : warIds) {
			ctx.clans().byClanId(cid).ifPresent(enemy -> {
				entries.add(new GameServerPacket.PledgeReceiveWarList.WarEntry(enemy.name(), p.tab(), enemy.members().size()));
			});
		}
		session.send(new GameServerPacket.PledgeReceiveWarList(p.tab(), entries));
	}

	public void handleReplySurrenderPledgeWar(GameClientPacket.RequestReplySurrenderPledgeWar p) {
		if (p.answer() == 1 && p.reqName() != null && !p.reqName().isBlank()) {
			handleStopPledgeWar(new GameClientPacket.RequestStopPledgeWar(p.reqName()));
		}
	}

	public void handleChangePartyLeader(GameClientPacket.RequestChangePartyLeader p) {
		PlayerCharacter active = session.activeChar();
		Party party = session.party();
		if (active == null || party == null || !party.isLeader(active.objectId()) || p.name() == null) {
			return;
		}
		for (var m : party.members()) {
			if (m.character() != null && m.character().name().equalsIgnoreCase(p.name().trim())) {
				party.changeLeader(m.character().objectId());
				return;
			}
		}
	}

	public boolean handleMemberPowerGrade(String memberName, int powerGrade) {
		var ctx = session.context();
		PlayerCharacter active = session.activeChar();
		if (ctx == null || ctx.clans() == null || ctx.clanPrivileges() == null || active == null || active.clanId() <= 0) {
			return false;
		}
		var clanOpt = ctx.clans().byClanId(active.clanId());
		if (clanOpt.isEmpty()) {
			return false;
		}
		var clan = clanOpt.get();
		for (var member : clan.members()) {
			if (member.name().equalsIgnoreCase(memberName)) {
				boolean ok = ctx.clanPrivileges().setMemberPowerGrade(active, member.objectId(), powerGrade);
				if (ok) {
					session.send(new PledgeShowMemberListAll(clan, 0));
				}
				return ok;
			}
		}
		return false;
	}

	public boolean handleCreateSubPledge(int pledgeType, String subPledgeName, String leaderName) {
		var ctx = session.context();
		PlayerCharacter active = session.activeChar();
		if (ctx == null || ctx.clans() == null || ctx.clanPrivileges() == null || active == null || active.clanId() <= 0) {
			return false;
		}
		int subLeaderId = 0;
		if (leaderName != null && !leaderName.isBlank()) {
			subLeaderId = ctx.clans().byClanId(active.clanId())
					.flatMap(c -> c.members().stream()
							.filter(m -> m.name() != null && m.name().equalsIgnoreCase(leaderName.trim()))
							.findFirst())
					.map(com.lopez.l2j.game.clan.ClanMember::objectId)
					.orElse(0);
		}
		boolean ok = ctx.clanPrivileges().createSubPledge(active, pledgeType, subPledgeName, subLeaderId)
				== com.lopez.l2j.game.clan.privilege.ClanPrivilegeService.SubPledgeResult.SUCCESS;
		if (ok) {
			ctx.clans().byClanId(active.clanId()).ifPresent(clan -> {
				session.send(new PledgeShowInfoUpdate(clan));
				session.send(new PledgeShowMemberListAll(clan, 0));
				session.send(new GameServerPacket.PledgeReceiveSubPledgeCreated(pledgeType, subPledgeName, leaderName != null ? leaderName : ""));
			});
		}
		return ok;
	}

	public void handleLearnClanSkill(int skillId, int skillLevel) {
		var ctx = session.context();
		PlayerCharacter active = session.activeChar();
		if (ctx == null || ctx.clanSkills() == null || ctx.clans() == null || active == null || active.clanId() <= 0) {
			session.send(new ActionFailed());
			return;
		}
		var res = ctx.clanSkills().learnSkill(active, skillId);
		if (res == com.lopez.l2j.game.clan.skill.ClanSkillService.LearnResult.SUCCESS) {
			ctx.clans().byClanId(active.clanId()).ifPresent(clan -> {
				clan.broadcastToOnlineMembers(ctx.world(), new PledgeShowInfoUpdate(clan));
			});
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Skill de cla aprendida com sucesso!"));
		} else {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nao foi possivel aprender a skill de cla: " + res.name()));
			session.send(new ActionFailed());
		}
	}

	public void applyClanSkillsOnLogin() {
		var ctx = session.context();
		PlayerCharacter active = session.activeChar();
		if (ctx != null && ctx.clanSkills() != null && active != null && active.clanId() > 0) {
			ctx.clanSkills().applyClanSkillsToPlayer(active);
		}
	}
}
