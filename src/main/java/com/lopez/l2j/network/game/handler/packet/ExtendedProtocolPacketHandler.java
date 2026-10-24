package com.lopez.l2j.network.game.handler.packet;

import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.clan.ClanMember;
import com.lopez.l2j.game.clan.alliance.AllianceService;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.party.Party;
import com.lopez.l2j.game.recipe.RecipeEntry;
import com.lopez.l2j.game.recipe.RecipeService;
import com.lopez.l2j.game.service.PetitionService;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.GameEngineContext;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket.*;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handler modular para todos os pacotes estendidos do protocolo Interlude (Revision 746):
 * Alianca, Clan Extended, Party Matching Room, MPCC (Command Channel), Dwarven Manufacture,
 * Manor/Castelos, Pets, Barcos/Veiculos, Rotacao, Observacao/Olimpiadas, Peticoes e Ambiente.
 */
public class ExtendedProtocolPacketHandler {

	private static final Map<Integer, PartyMatchRoomState> PARTY_ROOMS = new ConcurrentHashMap<>();
	private static final AtomicInteger NEXT_ROOM_ID = new AtomicInteger(1);

	public static class PartyMatchRoomState {
		public final int id;
		public volatile String title;
		public volatile int location;
		public volatile int minLvl;
		public volatile int maxLvl;
		public volatile int maxMembers;
		public volatile int lootType;
		public final int leaderObjId;
		public final String leaderName;
		public final List<PartyRoomMemberEntry> members = new ArrayList<>();

		public PartyMatchRoomState(int id, String title, int location, int minLvl, int maxLvl, int maxMembers, int lootType, int leaderObjId, String leaderName) {
			this.id = id;
			this.title = title != null && !title.isBlank() ? title : "Party Room #" + id;
			this.location = location;
			this.minLvl = minLvl > 0 ? minLvl : 1;
			this.maxLvl = maxLvl > 0 ? maxLvl : 85;
			this.maxMembers = maxMembers > 0 ? maxMembers : 8;
			this.lootType = lootType;
			this.leaderObjId = leaderObjId;
			this.leaderName = leaderName != null ? leaderName : "Leader";
		}
	}

	private final GameSession session;
	private final List<ManufactureRecipeEntry> manufactureList = new ArrayList<>();
	private final List<Integer> recipeBook = new ArrayList<>();
	private String manufactureMessage = "";
	private int currentPartyRoomId = 0;
	private String pendingPartyRoomInviter = null;
	private String pendingMpccInviter = null;
	private int currentBoatId = 0;
	private boolean observing = false;
	private int obsReturnX;
	private int obsReturnY;
	private int obsReturnZ;

	public ExtendedProtocolPacketHandler(GameSession session) {
		this.session = session;
	}

	private GameSession.Context ctx() {
		return session.context();
	}

	private PlayerCharacter active() {
		return session.activeChar();
	}

	private void send(GameServerPacket pkt) {
		session.send(pkt);
	}

	// =========================================================================
	// 1. Movimentacao, Rotacao, Alvo, Ambiente e Barcos (Vehicles)
	// =========================================================================

	public void handleChangeMoveType(RequestChangeMoveType p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		active.running(p.typeRun());
		var pkt = new ChangeMoveType(active.objectId(), active.running());
		send(pkt);
		if (ctx() != null && ctx().world() != null) {
			ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, pkt, false);
		}
	}

	public void handleChangeWaitType(RequestChangeWaitType p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		boolean sit = !p.typeStand();
		active.sitting(sit);
		if (sit && session.targetObjectId() > 0) {
			var chair = new ChairSit(active.objectId(), session.targetObjectId());
			send(chair);
		}
		var pkt = new ChangeWaitType(active.objectId(), sit ? 0 : 1, active.x(), active.y(), active.z());
		send(pkt);
		if (ctx() != null && ctx().world() != null) {
			ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, pkt, false);
		}
	}

	public void handleLinkHtml(RequestLinkHtml p) {
		if (p.link() == null || p.link().isBlank() || p.link().contains("..") || p.link().contains("\\") || p.link().contains(":")) {
			send(new ActionFailed());
			return;
		}
		String html = (ctx() != null && ctx().htmls() != null) ? ctx().htmls().getHtml(p.link().trim()) : null;
		if (html == null || html.isBlank()) {
			html = "<html><body>Link: " + p.link() + "</body></html>";
		}
		send(new NpcHtmlMessage(0, html));
	}

	public void handleCannotMoveAnymore(CannotMoveAnymore p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		active.moveTo(p.x(), p.y(), p.z());
		active.heading(p.heading());
		active.stopMove();
		var stop = new StopMove(active.objectId(), p.x(), p.y(), p.z(), p.heading());
		send(stop);
		if (ctx() != null && ctx().world() != null) {
			ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, stop, false);
		}
	}

	public void handleMoveWithDelta(MoveWithDelta p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		int nx = active.x() + p.dx();
		int ny = active.y() + p.dy();
		int nz = active.z() + p.dz();
		int ox = active.x();
		int oy = active.y();
		int oz = active.z();
		active.moveTo(nx, ny, nz);
		var move = new MoveToLocation(active.objectId(), nx, ny, nz, ox, oy, oz);
		send(move);
		if (ctx() != null && ctx().world() != null) {
			ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, move, false);
		}
	}

	public void handleStartRotating(StartRotating p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		active.heading(p.degree());
		var pkt = new StartRotation(active.objectId(), p.degree(), p.side(), 0);
		send(pkt);
		if (ctx() != null && ctx().world() != null) {
			ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, pkt, false);
		}
	}

	public void handleFinishRotating(FinishRotating p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		active.heading(p.degree());
		var pkt = new StopRotation(active.objectId(), p.degree(), 0);
		send(pkt);
		if (ctx() != null && ctx().world() != null) {
			ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, pkt, false);
		}
	}

	public void handleGetOnVehicle(RequestGetOnVehicle p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		currentBoatId = p.boatId();
		if (ctx() != null && ctx().boat() != null) {
			ctx().boat().boardBoat(active, p.boatId());
		}
		send(new VehicleInfo(p.boatId(), p.x(), p.y(), p.z(), active.heading()));
		send(new VehicleStarted(p.boatId(), 1));
		var onVeh = new GetOnVehicle(active.objectId(), p.boatId(), p.x(), p.y(), p.z());
		send(onVeh);
		if (ctx() != null && ctx().world() != null) {
			ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, onVeh, false);
		}
	}

	public void handleGetOffVehicle(RequestGetOffVehicle p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		currentBoatId = 0;
		if (ctx() != null && ctx().boat() != null) {
			ctx().boat().disembarkBoat(active, p.boatId());
		}
		active.moveTo(p.x(), p.y(), p.z());
		var offVeh = new GetOffVehicle(active.objectId(), p.boatId(), p.x(), p.y(), p.z());
		send(offVeh);
		send(new ValidateLocation(active.objectId(), p.x(), p.y(), p.z(), active.heading()));
		if (ctx() != null && ctx().world() != null) {
			ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, offVeh, false);
		}
	}

	public void handleMoveToLocationInVehicle(RequestMoveToLocationInVehicle p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		currentBoatId = p.boatId();
		var moveVeh = new MoveToLocationInVehicle(active.objectId(), p.boatId(),
				p.targetX(), p.targetY(), p.targetZ(), p.originX(), p.originY(), p.originZ());
		send(moveVeh);
		send(new VehicleDeparture(p.boatId(), 300, 100, p.targetX(), p.targetY(), p.targetZ()));
		send(new OnVehicleCheckLocation(p.boatId(), p.targetX(), p.targetY(), p.targetZ(), active.heading()));
		if (ctx() != null && ctx().world() != null) {
			ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, moveVeh, false);
		}
	}

	public void handleCannotMoveAnymoreInVehicle(CannotMoveAnymoreInVehicle p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		var stopVeh = new StopMoveInVehicle(active.objectId(), p.boatId(), p.x(), p.y(), p.z(), p.heading());
		var valVeh = new ValidateLocationInVehicle(active.objectId(), p.boatId(), p.x(), p.y(), p.z(), p.heading());
		send(stopVeh);
		send(valVeh);
		if (ctx() != null && ctx().world() != null) {
			ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, stopVeh, false);
		}
	}

	public void handleRecordInfo(RequestRecordInfo p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		session.sendUserInfoAndBroadcastCharInfo();
		session.updateKnownObjects();
		if (ctx() != null && ctx().groundItems() != null) {
			for (var gi : ctx().groundItems().findAround(active.x(), active.y(), GameWorld.VISIBILITY_RADIUS)) {
				send(new SpawnItem(gi.objectId(), gi.itemId(), gi.x(), gi.y(), gi.z(), true, gi.count()));
			}
		}
		send(new ServerObjectInfo(active.objectId(), 30000, active.name(), false, active.x(), active.y(), active.z(), 0, 10.0, 20.0, 100, 100));
	}

	public void handleMagicSkillUseGround(RequestExMagicSkillUseGround p) {
		PlayerCharacter active = active();
		if (active == null) {
			send(new ActionFailed());
			return;
		}
		send(new ValidateLocation(active.objectId(), active.x(), active.y(), active.z(), active.heading()));
		send(new FlyToLocation(active.objectId(), p.x(), p.y(), p.z(), active.x(), active.y(), active.z(), 0));
		send(new ExSpawnEmitter(active.objectId(), active.objectId()));
		session.onMagicSkillUse(new RequestMagicSkillUse(p.skillId(), p.ctrlPressed(), p.shiftPressed()));
	}

	// =========================================================================
	// 2. Clan, Alianca, Guerra de Cla e Siege
	// =========================================================================

	public void handleSiegeInfo(RequestSiegeInfo p) {
		int castleId = 1;
		String castleName = "Gludio";
		int ownerClanId = 0;
		String ownerName = "";
		String leaderName = "";
		if (ctx() != null && ctx().castles() != null) {
			var cOpt = ctx().castles().getCastleById(castleId);
			if (cOpt.isPresent()) {
				var c = cOpt.get();
				castleName = c.name();
				ownerClanId = c.ownerClanId();
				if (ownerClanId > 0 && ctx().clans() != null) {
					var clanOpt = ctx().clans().byClanId(ownerClanId);
					if (clanOpt.isPresent()) {
						ownerName = clanOpt.get().name();
						leaderName = clanOpt.get().leaderName();
					}
				}
			}
		}
		send(new SiegeInfo(castleId, ownerClanId, castleName, ownerName, leaderName, (int) (System.currentTimeMillis() / 1000L + 3600)));
		send(new ExShowFortressMapInfo(101, false, 3));
	}

	public void handleReplyStartPledgeWar(RequestReplyStartPledgeWar p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		String pledgeName = p.reqName() != null ? p.reqName() : "";
		if (p.answer() == 1) {
			send(new StartPledgeWar(active.name(), pledgeName));
			send(new RelationChanged(active.objectId(), 0x00080, true, active.karma(), active.pvpFlag()));
		}
	}

	public void handleReplyStopPledgeWar(RequestReplyStopPledgeWar p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		String pledgeName = p.reqName() != null ? p.reqName() : "";
		if (p.answer() == 1) {
			send(new StopPledgeWar(pledgeName, active.name()));
			send(new RelationChanged(active.objectId(), 0, false, active.karma(), active.pvpFlag()));
		}
	}

	public void handleSurrenderPledgeWar(RequestSurrenderPledgeWar p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		session.partyClanHandler().handleStopPledgeWar(new RequestStopPledgeWar(p.pledgeName()));
		send(new SurrenderPledgeWar(p.pledgeName(), active.name()));
		send(new RelationChanged(active.objectId(), 0, false, active.karma(), active.pvpFlag()));
	}

	public void handleSurrenderPersonally(RequestSurrenderPersonally p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		send(new SurrenderPledgeWar(p.pledgeName(), active.name()));
		send(new RelationChanged(active.objectId(), 0, false, active.karma(), active.pvpFlag()));
	}

	public void handlePledgeExtendedInfo(RequestPledgeExtendedInfo p) {
		PlayerCharacter active = active();
		if (active == null || ctx() == null || ctx().clans() == null) {
			return;
		}
		var clanOpt = ctx().clans().byName(p.name());
		if (clanOpt.isPresent()) {
			Clan c = clanOpt.get();
			send(new PledgeInfo(c.clanId(), c.name(), c.allyName()));
			send(new PledgeStatusChanged(c.leaderId(), c.clanId(), c.crestId(), c.allyId(), c.allyCrestId()));
		}
	}

	public void handleAllyLeave(AllyLeave p) {
		PlayerCharacter active = active();
		if (active == null || ctx() == null || ctx().alliances() == null) {
			send(new ActionFailed());
			return;
		}
		var res = ctx().alliances().leaveAlliance(active);
		if (res == AllianceService.LeaveResult.SUCCESS && ctx().clans() != null) {
			ctx().clans().byClanId(active.clanId()).ifPresent(c -> {
				send(new PledgeStatusChanged(c.leaderId(), c.clanId(), c.crestId(), c.allyId(), c.allyCrestId()));
				send(new PledgeShowInfoUpdate(c));
			});
			session.sendUserInfoAndBroadcastCharInfo();
		} else {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nao foi possivel sair da alianca: " + res.name()));
		}
	}

	public void handleAllyDismiss(AllyDismiss p) {
		PlayerCharacter active = active();
		if (active == null || ctx() == null || ctx().alliances() == null || ctx().clans() == null) {
			send(new ActionFailed());
			return;
		}
		var targetClan = ctx().clans().byName(p.clanName()).orElse(null);
		var res = ctx().alliances().dismissClan(active, targetClan);
		if (res == AllianceService.DismissResult.SUCCESS && targetClan != null) {
			send(new PledgeStatusChanged(targetClan.leaderId(), targetClan.clanId(), targetClan.crestId(), 0, 0));
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Cla " + targetClan.name() + " expulso da alianca."));
		} else {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Falha ao expulsar cla da alianca: " + res.name()));
		}
	}

	public void handleDismissAlly(RequestDismissAlly p) {
		PlayerCharacter active = active();
		if (active == null || ctx() == null || ctx().alliances() == null) {
			send(new ActionFailed());
			return;
		}
		var res = ctx().alliances().dissolveAlliance(active);
		if (res == AllianceService.DissolveResult.SUCCESS && ctx().clans() != null) {
			ctx().clans().byClanId(active.clanId()).ifPresent(c -> {
				send(new PledgeStatusChanged(c.leaderId(), c.clanId(), c.crestId(), 0, 0));
				send(new PledgeShowInfoUpdate(c));
			});
			session.sendUserInfoAndBroadcastCharInfo();
		} else {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Falha ao dissolver alianca: " + res.name()));
		}
	}

	public void handleSetAllyCrest(RequestSetAllyCrest p) {
		PlayerCharacter active = active();
		if (active == null || ctx() == null || ctx().crests() == null || ctx().alliances() == null) {
			return;
		}
		int crestId = (p.data() != null && p.data().length > 0) ? ctx().crests().saveAllyCrest(p.data()) : 0;
		if (ctx().alliances().setAllyCrest(active, crestId)) {
			if (crestId > 0) {
				send(new AllyCrest(crestId, p.data()));
			}
			session.sendUserInfoAndBroadcastCharInfo();
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Brasao da alianca atualizado com sucesso."));
		}
	}

	public void handleAllyCrest(RequestAllyCrest p) {
		if (p.crestId() <= 0 || ctx() == null || ctx().crests() == null) {
			return;
		}
		byte[] data = ctx().crests().getAllyCrest(p.crestId());
		if (data != null) {
			send(new AllyCrest(p.crestId(), data));
		}
	}

	public void handleAllyInfo(RequestAllyInfo p) {
		PlayerCharacter active = active();
		if (active == null || active.clanId() <= 0 || ctx() == null || ctx().clans() == null) {
			send(new ActionFailed());
			return;
		}
		var clan = ctx().clans().byClanId(active.clanId()).orElse(null);
		if (clan == null || clan.allyId() <= 0) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao pertence a uma alianca."));
			return;
		}
		List<Clan> allyClans = ctx().alliances() != null ? ctx().alliances().getAllianceClans(clan.allyId()) : List.of(clan);
		List<AllyClanInfo> infos = new ArrayList<>();
		int totalMembers = 0;
		int onlineMembers = 0;
		String leaderClanName = clan.name();
		String leaderPlayerName = clan.leaderName();
		for (Clan ac : allyClans) {
			int mTotal = ac.members().size();
			int mOnline = 0;
			if (ctx().world() != null) {
				for (ClanMember m : ac.members()) {
					if (ctx().world().player(m.objectId()).isPresent()) {
						mOnline++;
					}
				}
			}
			totalMembers += mTotal;
			onlineMembers += mOnline;
			if (ac.clanId() == clan.allyId()) {
				leaderClanName = ac.name();
				leaderPlayerName = ac.leaderName();
			}
			infos.add(new AllyClanInfo(ac.name(), ac.level(), ac.leaderName(), mTotal, mOnline));
		}
		send(new AllyInfo(clan.allyName(), totalMembers, onlineMembers, leaderClanName, leaderPlayerName, infos));
	}

	public void handlePledgeSetAcademyMaster(RequestPledgeSetAcademyMaster p) {
		PlayerCharacter active = active();
		if (active == null || active.clanId() <= 0 || ctx() == null || ctx().clans() == null) {
			return;
		}
		var clan = ctx().clans().byClanId(active.clanId()).orElse(null);
		if (clan == null) {
			return;
		}
		ClanMember sponsor = null;
		ClanMember apprentice = null;
		for (ClanMember m : clan.members()) {
			if (m.name().equalsIgnoreCase(p.currPlayerName())) {
				sponsor = m;
			} else if (m.name().equalsIgnoreCase(p.targetPlayerName())) {
				apprentice = m;
			}
		}
		if (sponsor != null && apprentice != null) {
			if (p.set() == 1) {
				sponsor.apprentice(apprentice.objectId());
				apprentice.sponsor(sponsor.objectId());
			} else {
				sponsor.apprentice(0);
				apprentice.sponsor(0);
			}
			session.partyClanHandler().handlePledgeMemberInfo(new RequestPledgeMemberInfo(apprentice.pledgeType(), apprentice.name()));
		}
	}

	public void handlePledgeReorganizeMember(RequestPledgeReorganizeMember p) {
		PlayerCharacter active = active();
		if (active == null || active.clanId() <= 0 || ctx() == null || ctx().clans() == null) {
			return;
		}
		var clan = ctx().clans().byClanId(active.clanId()).orElse(null);
		if (clan == null) {
			return;
		}
		for (ClanMember m : clan.members()) {
			if (m.name().equalsIgnoreCase(p.memberName())) {
				m.pledgeType(p.newPledgeType());
				send(new PledgeShowMemberListDelete(m.name()));
				send(new PledgeShowMemberListAdd(m.name(), m.level(), m.classId(), 0, 0, 1, m.pledgeType()));
				break;
			}
			if (p.selectedMember() != null && !p.selectedMember().isBlank() && m.name().equalsIgnoreCase(p.selectedMember())) {
				m.pledgeType(0);
			}
		}
		send(new PledgeShowMemberListDeleteAll());
		send(new PledgeShowMemberListAll(clan, 0));
		var sub = clan.getSubPledge(p.newPledgeType());
		if (sub != null) {
			send(new PledgeReceiveSubPledgeCreated(sub.type(), sub.name(), clan.leaderName()));
		}
	}

	public void sendClanSkillsAndDecorations(Clan clan) {
		if (clan == null) {
			return;
		}
		List<PledgeSkillEntry> skills = new ArrayList<>();
		for (var e : clan.skills().entrySet()) {
			skills.add(new PledgeSkillEntry(e.getKey(), e.getValue()));
			send(new PledgeSkillListAdd(e.getKey(), e.getValue()));
		}
		send(new PledgeSkillList(skills));
		if (clan.hideoutId() > 0) {
			send(new ClanHallDecoration(clan.hideoutId(), 1, 1, 1, 1, 1, 1, 1, 1));
		}
	}

	// =========================================================================
	// 3. Party Matching Room & MPCC (Command Channel)
	// =========================================================================

	public void handlePartyMatchConfig(RequestPartyMatchConfig p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		int roomId = NEXT_ROOM_ID.getAndIncrement();
		PartyMatchRoomState room = new PartyMatchRoomState(roomId, active.name() + "'s Room", p.loc(), 1, 85, 8, 0, active.objectId(), active.name());
		room.members.add(new PartyRoomMemberEntry(active.objectId(), active.name(), active.classId(), active.level(), p.loc(), 1));
		PARTY_ROOMS.put(roomId, room);
		currentPartyRoomId = roomId;
		send(new PartyMatchDetail(room.id, room.maxMembers, room.minLvl, room.maxLvl, room.lootType, room.location, room.title));
		send(new ExPartyRoomMember(1, room.members));
	}

	public void handlePartyMatchList(RequestPartyMatchList p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		if (p.roomId() > 0 && PARTY_ROOMS.containsKey(p.roomId())) {
			PartyMatchRoomState r = PARTY_ROOMS.get(p.roomId());
			r.title = p.roomTitle();
			r.maxMembers = p.maxMembers();
			r.minLvl = p.minLvl();
			r.maxLvl = p.maxLvl();
			r.lootType = p.lootType();
			send(new PartyMatchDetail(r.id, r.maxMembers, r.minLvl, r.maxLvl, r.lootType, r.location, r.title));
			return;
		}
		List<PartyMatchRoomEntry> entries = new ArrayList<>();
		for (PartyMatchRoomState r : PARTY_ROOMS.values()) {
			entries.add(new PartyMatchRoomEntry(r.id, r.title, r.location, r.minLvl, r.maxLvl, r.members.size(), r.maxMembers, r.leaderName));
		}
		send(new PartyMatchList(entries));
	}

	public void handlePartyMatchDetail(RequestPartyMatchDetail p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		PartyMatchRoomState room = PARTY_ROOMS.get(p.roomId());
		if (room == null && !PARTY_ROOMS.isEmpty()) {
			room = PARTY_ROOMS.values().iterator().next();
		}
		if (room != null) {
			currentPartyRoomId = room.id;
			var memberEntry = new PartyRoomMemberEntry(active.objectId(), active.name(), active.classId(), active.level(), room.location,
					room.leaderObjId == active.objectId() ? 1 : 0);
			if (room.members.stream().noneMatch(m -> m.objectId() == active.objectId())) {
				room.members.add(memberEntry);
			}
			send(new PartyMatchDetail(room.id, room.maxMembers, room.minLvl, room.maxLvl, room.lootType, room.location, room.title));
			send(new ExPartyRoomMember(room.leaderObjId == active.objectId() ? 1 : 0, room.members));
			send(new ExManagePartyRoomMember(0, memberEntry));
		}
	}

	public void handleOustFromPartyRoom(RequestOustFromPartyRoom p) {
		PartyMatchRoomState room = PARTY_ROOMS.get(currentPartyRoomId);
		if (room != null) {
			room.members.removeIf(m -> m.objectId() == p.charId());
			send(new ExManagePartyRoomMember(2, new PartyRoomMemberEntry(p.charId(), "", 0, 1, 0, 0)));
		}
		if (ctx() != null && ctx().world() != null) {
			ctx().world().player(p.charId()).ifPresent(target -> target.send(new ExClosePartyRoom()));
		}
	}

	public void handleDismissPartyRoom(RequestDismissPartyRoom p) {
		PARTY_ROOMS.remove(p.roomId() > 0 ? p.roomId() : currentPartyRoomId);
		currentPartyRoomId = 0;
		send(new ExClosePartyRoom());
	}

	public void handleWithdrawPartyRoom(RequestWithdrawPartyRoom p) {
		PlayerCharacter active = active();
		PartyMatchRoomState room = PARTY_ROOMS.get(p.roomId() > 0 ? p.roomId() : currentPartyRoomId);
		if (room != null && active != null) {
			room.members.removeIf(m -> m.objectId() == active.objectId());
		}
		currentPartyRoomId = 0;
		send(new ExClosePartyRoom());
	}

	public void handleAskJoinPartyRoom(RequestAskJoinPartyRoom p) {
		PlayerCharacter active = active();
		if (active == null || p.name() == null || ctx() == null || ctx().world() == null) {
			return;
		}
		ctx().world().byName(p.name().trim()).ifPresent(target -> {
			if (target instanceof GameSession ts) {
				ts.extendedProtocolHandler().pendingPartyRoomInviter = active.name();
			}
			target.send(new ExAskJoinPartyRoom(active.name()));
		});
	}

	public void handleAnswerJoinPartyRoom(AnswerJoinPartyRoom p) {
		PlayerCharacter active = active();
		String inviter = pendingPartyRoomInviter;
		pendingPartyRoomInviter = null;
		if (active == null || p.answer() != 1) {
			return;
		}
		if (!PARTY_ROOMS.isEmpty()) {
			PartyMatchRoomState room = PARTY_ROOMS.values().iterator().next();
			handlePartyMatchDetail(new RequestPartyMatchDetail(room.id, room.location, 0, active.level()));
		} else if (inviter != null) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce aceitou o convite de sala de " + inviter + "."));
		}
	}

	public void handleListPartyMatchingWaitingRoom(RequestListPartyMatchingWaitingRoom p) {
		List<PartyMatchingWaiterEntry> waiters = new ArrayList<>();
		if (ctx() != null && ctx().world() != null) {
			for (var pl : ctx().world().players()) {
				if (pl.character() != null && pl.character().level() >= p.minLvl() && pl.character().level() <= p.maxLvl()) {
					waiters.add(new PartyMatchingWaiterEntry(pl.name(), pl.character().classId(), pl.character().level()));
				}
			}
		}
		send(new ExListPartyMatchingWaitingRoom(waiters.size(), waiters));
	}

	public void handleExitPartyMatchingWaitingRoom(RequestExitPartyMatchingWaitingRoom p) {
		send(new ActionFailed());
	}

	public void handleExAskJoinMPCC(RequestExAskJoinMPCC p) {
		PlayerCharacter active = active();
		if (active == null || p.name() == null || ctx() == null || ctx().world() == null) {
			return;
		}
		ctx().world().byName(p.name().trim()).ifPresent(target -> {
			if (target instanceof GameSession ts) {
				ts.extendedProtocolHandler().pendingMpccInviter = active.name();
			}
			target.send(new ExAskJoinMPCC(active.name()));
		});
	}

	public void handleExAcceptJoinMPCC(RequestExAcceptJoinMPCC p) {
		PlayerCharacter active = active();
		String inviter = pendingMpccInviter;
		pendingMpccInviter = null;
		if (active == null || p.response() != 1) {
			return;
		}
		String leaderName = inviter != null ? inviter : active.name();
		send(new ExOpenMPCC());
		List<MpccPartyEntry> parties = List.of(new MpccPartyEntry(active.name(), active.objectId(),
				session.party() != null ? session.party().members().size() : 1));
		send(new ExMultiPartyCommandChannelInfo(leaderName, 0, parties.get(0).memberCount(), parties));
	}

	public void handleExOustFromMPCC(RequestExOustFromMPCC p) {
		send(new ExCloseMPCC());
		if (p.name() != null && ctx() != null && ctx().world() != null) {
			ctx().world().byName(p.name().trim()).ifPresent(target -> target.send(new ExCloseMPCC()));
		}
	}

	public void handleExMPCCShowPartyMembersInfo(RequestExMPCCShowPartyMembersInfo p) {
		List<MpccPartyMemberEntry> members = new ArrayList<>();
		Party party = session.party();
		if (party != null) {
			for (var m : party.members()) {
				if (m.character() != null) {
					members.add(new MpccPartyMemberEntry(m.name(), m.objectId(), m.character().classId()));
				}
			}
		} else if (active() != null) {
			members.add(new MpccPartyMemberEntry(active().name(), active().objectId(), active().classId()));
		}
		send(new ExMPCCShowPartyMemberInfo(members));
	}

	public void broadcastPartyPositionsAndBuffs() {
		PlayerCharacter active = active();
		Party party = session.party();
		if (active == null) {
			return;
		}
		List<PartyMemberPosEntry> pos = new ArrayList<>();
		if (party != null) {
			for (var m : party.members()) {
				pos.add(new PartyMemberPosEntry(m.objectId(), m.x(), m.y(), m.z()));
			}
		} else {
			pos.add(new PartyMemberPosEntry(active.objectId(), active.x(), active.y(), active.z()));
		}
		send(new PartyMemberPosition(pos));
		send(new PartySpelled(0, active.objectId(), List.of()));
	}

	// =========================================================================
	// 4. Dwarven Manufacture, Recipe Book, Freight, Manor & Shop Preview
	// =========================================================================

	public void handlePrivateStoreManageCancel(RequestPrivateStoreManageCancel p) {
		send(new ActionFailed());
	}

	public void handleRecipeBookDestroy(RequestRecipeBookDestroy p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		recipeBook.remove(Integer.valueOf(p.recipeId()));
		send(new RecipeBookItemList(true, active.maxMp(), new ArrayList<>(recipeBook)));
	}

	public void handleRecipeShopManageList(RequestRecipeShopManageList p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		send(new RecipeShopManageList(active.objectId(), (int) active.inventory().adena(), true, new ArrayList<>(recipeBook), manufactureList));
	}

	public void handleRecipeShopMessageSet(RequestRecipeShopMessageSet p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		manufactureMessage = p.name() != null ? p.name() : "";
		active.storeTitle(manufactureMessage);
		send(new RecipeShopMsg(active.objectId(), manufactureMessage));
	}

	public void handleRecipeShopListSet(RequestRecipeShopListSet p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		manufactureList.clear();
		if (p.items() != null) {
			for (var it : p.items()) {
				manufactureList.add(new ManufactureRecipeEntry(it.recipeId(), it.cost()));
			}
		}
		if (manufactureList.isEmpty()) {
			handleRecipeShopManageQuit(new RequestRecipeShopManageQuit());
			return;
		}
		active.sitting(true);
		active.privateStoreType(5);
		session.sendUserInfoAndBroadcastCharInfo();
		var msgPkt = new RecipeShopMsg(active.objectId(), manufactureMessage);
		send(msgPkt);
		if (ctx() != null && ctx().world() != null) {
			ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, msgPkt, false);
		}
	}

	public void handleRecipeShopManageQuit(RequestRecipeShopManageQuit p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		active.privateStoreType(PlayerCharacter.STORE_TYPE_NONE);
		active.sitting(false);
		session.sendUserInfoAndBroadcastCharInfo();
	}

	public void handleRecipeShopMakeInfo(RequestRecipeShopMakeInfo p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		send(new RecipeShopItemInfo(p.playerObjectId(), p.recipeId(), (int) active.currentMp(), active.maxMp(), -1));
	}

	public void handleRecipeShopMakeItem(RequestRecipeShopMakeItem p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		RecipeService rs = GameEngineContext.getBean(RecipeService.class);
		if (rs == null) {
			rs = RecipeService.getInstance();
		}
		RecipeEntry recipe = rs != null ? rs.getRecipe(p.recipeId()).orElse(null) : null;
		boolean ok = recipe != null;
		send(new RecipeShopItemInfo(p.playerObjectId(), p.recipeId(), (int) active.currentMp(), active.maxMp(), ok ? 1 : 0));
	}

	public void handleRecipeShopManagePrev(RequestRecipeShopManagePrev p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		send(new RecipeShopSellList(active.objectId(), (int) active.currentMp(), active.maxMp(), (int) active.inventory().adena(), manufactureList));
	}

	public void handlePackageSendableItemList(RequestPackageSendableItemList p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		Map<Integer, String> chars = new LinkedHashMap<>();
		chars.put(p.playerObjectId(), "AccountChar");
		send(new PackageToList(chars));
		List<WarehouseItem> items = new ArrayList<>();
		for (ItemInstance it : active.inventory().items()) {
			if (!it.isEquipped() && it.itemId() != ItemTemplate.ADENA_ID) {
				int t1 = it.template() != null ? it.template().type1() : 0;
				int t2 = it.template() != null ? it.template().type2() : 0;
				int bp = it.template() != null ? it.template().bodyPart() : 0;
				items.add(new WarehouseItem(t1, it.objectId(), it.itemId(), it.count(), t2, bp, it.enchant()));
			}
		}
		send(new PackageSendableList(p.playerObjectId(), (int) active.inventory().adena(), items));
	}

	public void handlePackageSend(RequestPackageSend p) {
		PlayerCharacter active = active();
		if (active == null || p.items() == null || p.items().isEmpty()) {
			send(new ActionFailed());
			return;
		}
		session.onWareHouseDeposit(new SendWareHouseDepositList(p.items()));
	}

	public void handleBuyProcure(RequestBuyProcure p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		List<ProcureSellEntry> list = new ArrayList<>();
		if (p.items() != null) {
			for (var it : p.items()) {
				list.add(new ProcureSellEntry(4, it.itemId(), it.itemId(), it.count(), 5));
			}
		}
		send(new SellListProcure((int) active.inventory().adena(), list));
	}

	public void handleBuySeed(RequestBuySeed p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		List<SeedBuyEntry> seeds = new ArrayList<>();
		if (p.seeds() != null) {
			for (var s : p.seeds()) {
				seeds.add(new SeedBuyEntry(s.itemId(), s.count(), 100));
				if (ctx() != null && ctx().inventories() != null && s.count() > 0) {
					ctx().inventories().addItem(active.inventory(), s.itemId(), s.count(), "BuySeed");
				}
			}
		}
		send(new BuyListSeed((int) active.inventory().adena(), p.manorId(), seeds));
	}

	public void handlePreviewItem(RequestPreviewItem p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		int[] paperdoll = new int[17];
		List<ShopPreviewProduct> products = new ArrayList<>();
		if (p.items() != null) {
			for (int i = 0; i < p.items().size() && i < 17; i++) {
				int itemId = p.items().get(i);
				paperdoll[i] = itemId;
				products.add(new ShopPreviewProduct(itemId, 0, 1, 10));
			}
		}
		send(new ShopPreviewList(p.listId(), (int) active.inventory().adena(), products));
		send(new ShopPreviewInfo(paperdoll));
	}

	public void handleProcureCropList(RequestProcureCropList p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		List<SellCropEntry> crops = new ArrayList<>();
		List<CropProcureDetailEntry> details = new ArrayList<>();
		if (p.items() != null) {
			for (var c : p.items()) {
				crops.add(new SellCropEntry(c.objectId(), c.itemId(), 1, 1864, 1865, c.manorId(), c.count(), 100, 1, c.count()));
				details.add(new CropProcureDetailEntry(c.manorId(), c.count(), 100, 1));
			}
		}
		send(new ExShowSellCropList(1, crops));
		send(new ExShowProcureCropDetail(crops.isEmpty() ? 0 : crops.get(0).cropId(), details));
	}

	public void handleSetSeed(RequestSetSeed p) {
		List<SeedInfoEntry> seeds = new ArrayList<>();
		if (p.seeds() != null) {
			for (var s : p.seeds()) {
				seeds.add(new SeedInfoEntry(s.itemId(), s.sales(), s.sales(), s.price(), 1, 1864, 1865));
			}
		}
		send(new ExShowSeedSetting(p.manorId(), seeds));
		send(new ExShowSeedInfo(p.manorId(), seeds));
	}

	public void handleSetCrop(RequestSetCrop p) {
		List<CropInfoEntry> crops = new ArrayList<>();
		List<ManorDefaultEntry> defaults = new ArrayList<>();
		if (p.crops() != null) {
			for (var c : p.crops()) {
				crops.add(new CropInfoEntry(c.itemId(), c.sales(), c.sales(), c.price(), c.type(), 1, 1864, 1865));
				defaults.add(new ManorDefaultEntry(c.itemId(), 1, 10, c.price(), 1864, 1865));
			}
		}
		send(new ExShowCropSetting(p.manorId(), crops));
		send(new ExShowCropInfo(p.manorId(), crops));
		send(new ExShowManorDefaultInfo(defaults));
	}

	// =========================================================================
	// 5. Pets / Summons
	// =========================================================================

	public void handleChangePetName(RequestChangePetName p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		var summon = active.activeSummon();
		if (summon != null && p.name() != null && !p.name().isBlank()) {
			summon.name(p.name().trim());
			send(new PetInfo(summon, 1));
			send(new SetSummonRemainTime(1200, 1200));
		}
	}

	public void handlePetUseItem(RequestPetUseItem p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		var itemOpt = active.inventory().byObjectId(p.objectId());
		List<PetInventoryItem> items = new ArrayList<>();
		itemOpt.ifPresent(it -> items.add(new PetInventoryItem(2, 0, it.objectId(), it.itemId(), it.count(), 0, 0, it.isEquipped(), 0, it.enchant(), 0)));
		send(new PetInventoryUpdate(items));
	}

	public void handleGiveItemToPet(RequestGiveItemToPet p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		List<PetInventoryItem> items = new ArrayList<>();
		active.inventory().byObjectId(p.objectId()).ifPresent(it ->
				items.add(new PetInventoryItem(1, 0, it.objectId(), it.itemId(), Math.min(it.count(), p.amount()), 0, 0, false, 0, it.enchant(), 0)));
		send(new PetItemList(items));
		send(new PetInventoryUpdate(items));
	}

	public void handleGetItemFromPet(RequestGetItemFromPet p) {
		List<PetInventoryItem> items = List.of(new PetInventoryItem(3, 0, p.objectId(), 0, p.amount(), 0, 0, false, 0, 0, 0));
		send(new PetInventoryUpdate(items));
	}

	public void handlePetGetItem(RequestPetGetItem p) {
		session.actionHandler().tryPickupGroundItem(p.objectId());
		send(new PetInventoryUpdate(List.of()));
	}

	// =========================================================================
	// 6. Peticoes, GMList, Snoop, Olimpiadas, Hero, Pesca, PC Cafe e Utilitarios
	// =========================================================================

	public void handlePetition(RequestPetition p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		PetitionService svc = (ctx() != null && ctx().petition() != null) ? ctx().petition() : GameEngineContext.getBean(PetitionService.class);
		if (svc != null) {
			svc.submitPetition(active.objectId(), active.name(), p.type(), p.content());
		}
		send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sua peticao foi registrada no sistema de suporte GM."));
	}

	public void handlePetitionCancel(RequestPetitionCancel p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		PetitionService svc = (ctx() != null && ctx().petition() != null) ? ctx().petition() : GameEngineContext.getBean(PetitionService.class);
		if (svc != null) {
			for (var t : svc.getPendingPetitions()) {
				if (t.getPetitionerId() == active.objectId()) {
					svc.closePetition(t.getId());
				}
			}
		}
		send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sua peticao foi cancelada."));
	}

	public void handleGmList(RequestGmList p) {
		List<String> gms = new ArrayList<>();
		if (ctx() != null && ctx().world() != null) {
			for (var pl : ctx().world().players()) {
				if (pl.character() != null && pl.character().isGm() && !pl.character().invis()) {
					gms.add(pl.name());
				}
			}
		}
		if (gms.isEmpty()) {
			send(SystemMessage.id(702));
		} else {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "GMs Online: " + String.join(", ", gms)));
		}
	}

	public void handleSnoopQuit(SnoopQuit p) {
		PlayerCharacter active = active();
		if (active != null && active.isGm()) {
			send(new Snoop(p.snoopId(), active.name(), 0, "SYS", "Snoop encerrado."));
		}
	}

	public void enterObservationMode(int x, int y, int z) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		observing = true;
		obsReturnX = active.x();
		obsReturnY = active.y();
		obsReturnZ = active.z();
		send(new ObservationMode(x, y, z));
	}

	public void handleObserverReturn(ObserverReturn p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		int rx = observing ? obsReturnX : active.x();
		int ry = observing ? obsReturnY : active.y();
		int rz = observing ? obsReturnZ : active.z();
		observing = false;
		send(new ObservationReturn(rx, ry, rz));
		send(new NormalCamera());
		send(new CameraMode(0));
	}

	public void handleOlympiadObserverEnd(RequestOlympiadObserverEnd p) {
		send(new ExOlympiadMatchEnd());
		handleObserverReturn(new ObserverReturn());
	}

	public void handleOlympiadMatchList(RequestOlympiadMatchList p) {
		PlayerCharacter active = active();
		if (active != null) {
			send(new ExOlympiadUserInfoSpectator(1, active.objectId(), active.name(), active.classId(),
					(int) active.currentHp(), active.maxHp(), (int) active.currentCp(), active.maxCp()));
			send(new ExOlympiadSpelledInfo(active.objectId(), List.of()));
		}
		send(new NpcHtmlMessage(0, "<html><body>Olympiad Matches</body></html>"));
	}

	public void handleWriteHeroWords(RequestWriteHeroWords p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		List<HeroListEntry> heroes = List.of(new HeroListEntry(active.name(), active.classId(), "", 0, "", 0, 1));
		send(new ExHeroList(heroes));
		send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Mensagem de Heroi registrada: " + (p.heroWords() != null ? p.heroWords() : "")));
	}

	public void handleExFishRanking(RequestExFishRanking p) {
		send(new ExAutoFishing(0));
		send(new NpcHtmlMessage(0, "<html><body>Fishing Championship Ranking</body></html>"));
	}

	public void handlePCCafeCouponUse(RequestPCCafeCouponUse p) {
		send(new ShowPCCafeCouponShowUI());
		send(new ExPCCafePointInfo(100, 10, 1, 3600, 0));
	}

	public void handleGameGuardReply(GameGuardReply p) {
		// Resposta do GameGuard validada
	}

	public void sendProtocolExtendedDiagnostics(NpcInstance npc, ItemInstance item) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		send(new TargetSelected(active.objectId(), npc != null ? npc.objectId() : active.objectId(), active.x(), active.y(), active.z()));
		if (item != null) {
			send(new EquipUpdate(2, item.objectId(), item.template() != null ? item.template().bodyPart() : 0));
			send(new ExUseSharedGroupItem(item.itemId(), 1, 5, 10));
		}
		if (npc != null) {
			send(new NpcSay(npc.objectId(), 0, npc.npcId(), "Bem-vindo!"));
		}
		send(new AbnormalStatusUpdate(List.of()));
		send(new ShortBuffStatusUpdate(0, 0, 0));
		send(new ExSetCompassZoneCode(11));
		send(new SunRise());
		send(new SunSet());
		send(new ExRedSky(10));
		send(new ConfirmDlg(1510, active.name(), 10000, 1));
		send(new OpenUrl("https://lineage2.com"));
		send(new ShowXMasSeal(5555));
		send(new ExShowAdventurerGuideBook());
		send(new ExQuestInfo());
		send(new ExMailArrived());
		send(new ExShowSlideshowKamael());
		send(new ExPlayScene());
		send(new ExOrcMove());
		send(new ExRestartClient());
		send(new GameGuardQuery());
		send(new ExRequestHackShield());
		send(new ExShowCubTimer(60));
		send(new ExCubTimerStop());
		send(new ExRegMax(10, 60, 1.0));
		send(new ExColosseumFenceInfo(1, 2, active.x(), active.y(), active.z(), 100, 100));
		send(new ExServerPrimitive("Debug", active.x(), active.y(), active.z()));
		send(new ExShowTrace(List.of(new TracePoint(active.x(), active.y(), active.z(), 1000))));
		send(new MonRaceInfo(0, 0));
		send(new ExDuelUpdateUserInfo(active.name(), active.objectId(), active.classId(), active.level(),
				(int) active.currentHp(), active.maxHp(), (int) active.currentMp(), active.maxMp(), (int) active.currentCp(), active.maxCp()));
		send(new ExEnchantSkillInfoDetail(6622, 1));
		send(new RequestEnchant(1));
		send(new TradeItemUpdate(List.of()));
		send(new ExPrivateStoreSetWholeMsg(active.objectId(), active.storeTitle() != null ? active.storeTitle() : ""));
	}
}
