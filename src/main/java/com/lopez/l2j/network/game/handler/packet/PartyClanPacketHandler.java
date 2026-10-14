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
		boolean hasPriv = clan != null && (clan.isLeader(active.objectId())
				|| (ctx.clanPrivileges() != null && ctx.clanPrivileges().hasPrivilege(active, com.lopez.l2j.game.clan.Clan.CP_CL_REGISTER_CREST)));
		if (clan == null || !hasPriv) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao possui privilegio para alterar o brasao do cla."));
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
		boolean ok = ctx.clanPrivileges().createSubPledge(active, pledgeType, subPledgeName, leaderName);
		if (ok) {
			ctx.clans().byClanId(active.clanId()).ifPresent(clan -> {
				session.send(new PledgeShowInfoUpdate(clan));
				session.send(new PledgeShowMemberListAll(clan, 0));
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
		var res = ctx.clanSkills().learnSkill(active, skillId, skillLevel);
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
