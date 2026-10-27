package com.lopez.l2j.network.game.handler.packet;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.castle.CastleManager;
import com.lopez.l2j.game.manor.CastleManorManager;
import com.lopez.l2j.game.manor.CropProcure;
import com.lopez.l2j.game.manor.SeedProduction;
import com.lopez.l2j.game.manor.SeedTemplate;
import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.clan.ClanMember;
import com.lopez.l2j.game.clan.alliance.AllianceService;
import com.lopez.l2j.game.augmentation.AugmentationService;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.party.Party;
import com.lopez.l2j.game.pet.PetService;
import com.lopez.l2j.game.recipe.RecipeEntry;
import com.lopez.l2j.game.recipe.RecipeService;
import com.lopez.l2j.game.cursed.CursedWeaponsManager;
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
 * Handler modular para todos os pacotes estendidos do protocolo Interlude
 * (Revision 746):
 * Alianca, Clan Extended, Party Matching Room, MPCC (Command Channel), Dwarven
 * Manufacture,
 * Manor/Castelos, Pets, Barcos/Veiculos, Rotacao, Observacao/Olimpiadas,
 * Peticoes e Ambiente.
 */
public class ExtendedProtocolPacketHandler {

	private static final Map<Integer, PartyMatchRoomState> PARTY_ROOMS = new ConcurrentHashMap<>();
	private static final java.util.Set<Integer> PARTY_WAITING_ROOM = ConcurrentHashMap.newKeySet();
	private final java.util.Set<String> redeemedCoupons = ConcurrentHashMap.newKeySet();
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

		public PartyMatchRoomState(int id, String title, int location, int minLvl, int maxLvl, int maxMembers,
				int lootType, int leaderObjId, String leaderName) {
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
		if (p.link() == null || p.link().isBlank() || p.link().contains("..") || p.link().contains("\\")
				|| p.link().contains(":")) {
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
		send(new ServerObjectInfo(active.objectId(), 30000, active.name(), false, active.x(), active.y(), active.z(), 0,
				10.0, 20.0, 100, 100));
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
		PlayerCharacter active = active();
		int resolvedCastleId = 1;
		if (ctx() != null && ctx().castles() != null && active != null) {
			if (session.targetObjectId() > 0 && ctx().world() != null) {
				var npcOpt = ctx().world().npc(session.targetObjectId());
				if (npcOpt.isPresent()) {
					var cAtNpc = ctx().castles().getCastleAt(npcOpt.get().x(), npcOpt.get().y(), npcOpt.get().z(),
							ctx().zones());
					if (cAtNpc.isPresent()) {
						resolvedCastleId = cAtNpc.get().id();
					}
				}
			}
			if (resolvedCastleId == 1) {
				var cAtPlayer = ctx().castles().getCastleAt(active.x(), active.y(), active.z(), ctx().zones());
				if (cAtPlayer.isPresent()) {
					resolvedCastleId = cAtPlayer.get().id();
				} else if (active.clanId() > 0 && ctx().clans() != null) {
					var clanOpt = ctx().clans().byClanId(active.clanId());
					if (clanOpt.isPresent() && clanOpt.get().castleId() > 0) {
						resolvedCastleId = clanOpt.get().castleId();
					}
				}
			}
		}
		int castleId = resolvedCastleId;
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
		send(new SiegeInfo(castleId, ownerClanId, castleName, ownerName, leaderName,
				(int) (System.currentTimeMillis() / 1000L + 3600)));
		send(new ExShowFortressMapInfo(101, false, 3));
		if (p == null)
			return;
	}

	public void handleSetCastleSiegeTime(RequestSetCastleSiegeTime p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		int castleId = p.castleId() > 0 ? p.castleId() : 1;
		long newSiegeTimeMs = p.time() > 100_000_000 ? p.time() * 1000L
				: System.currentTimeMillis() + Math.max(1, p.time()) * 3600_000L;
		if (ctx() != null && ctx().castles() != null) {
			ctx().castles().getCastleById(castleId).ifPresent(c -> c.siegeDate(newSiegeTimeMs));
		}
		handleSiegeInfo(new RequestSiegeInfo());
	}

	public void handleReplyStartPledgeWar(RequestReplyStartPledgeWar p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		String pledgeName = p.reqName() != null ? p.reqName() : "";
		if (p.answer() == 1) {
			if (ctx() != null && ctx().clanWars() != null && !pledgeName.isBlank()) {
				ctx().clanWars().declareWar(active, pledgeName);
			}
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
			if (ctx() != null && ctx().clanWars() != null && !pledgeName.isBlank()) {
				ctx().clanWars().stopWar(active, pledgeName);
			}
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
		if (active() != null && ctx() != null && ctx().clanWars() != null && p.pledgeName() != null
				&& !p.pledgeName().isBlank()) {
			ctx().clanWars().stopWar(active(), p.pledgeName());
		}
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
		List<Clan> allyClans = ctx().alliances() != null ? ctx().alliances().getAllianceClans(clan.allyId())
				: List.of(clan);
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
			session.partyClanHandler()
					.handlePledgeMemberInfo(new RequestPledgeMemberInfo(apprentice.pledgeType(), apprentice.name()));
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
			if (p.selectedMember() != null && !p.selectedMember().isBlank()
					&& m.name().equalsIgnoreCase(p.selectedMember())) {
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
		PartyMatchRoomState room = new PartyMatchRoomState(roomId, active.name() + "'s Room", p.loc(), 1, 85, 8, 0,
				active.objectId(), active.name());
		room.members.add(new PartyRoomMemberEntry(active.objectId(), active.name(), active.classId(), active.level(),
				p.loc(), 1));
		PARTY_ROOMS.put(roomId, room);
		currentPartyRoomId = roomId;
		send(new PartyMatchDetail(room.id, room.maxMembers, room.minLvl, room.maxLvl, room.lootType, room.location,
				room.title));
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
			entries.add(new PartyMatchRoomEntry(r.id, r.title, r.location, r.minLvl, r.maxLvl, r.members.size(),
					r.maxMembers, r.leaderName));
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
			var memberEntry = new PartyRoomMemberEntry(active.objectId(), active.name(), active.classId(),
					active.level(), room.location,
					room.leaderObjId == active.objectId() ? 1 : 0);
			if (room.members.stream().noneMatch(m -> m.objectId() == active.objectId())) {
				room.members.add(memberEntry);
			}
			send(new PartyMatchDetail(room.id, room.maxMembers, room.minLvl, room.maxLvl, room.lootType, room.location,
					room.title));
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
		if (active() != null) {
			PARTY_WAITING_ROOM.add(active().objectId());
		}
		List<PartyMatchingWaiterEntry> waiters = new ArrayList<>();
		if (ctx() != null && ctx().world() != null) {
			for (var pl : ctx().world().players()) {
				if (pl.character() != null && pl.character().level() >= p.minLvl()
						&& pl.character().level() <= p.maxLvl()) {
					waiters.add(
							new PartyMatchingWaiterEntry(pl.name(), pl.character().classId(), pl.character().level()));
				}
			}
		}
		send(new ExListPartyMatchingWaitingRoom(waiters.size(), waiters));
	}

	public void handleExitPartyMatchingWaitingRoom(RequestExitPartyMatchingWaitingRoom p) {
		PlayerCharacter active = active();
		if (active != null) {
			PARTY_WAITING_ROOM.remove(active.objectId());
			PartyMatchRoomState room = PARTY_ROOMS.get(currentPartyRoomId);
			if (room != null) {
				room.members.removeIf(m -> m.objectId() == active.objectId());
			}
		}
		currentPartyRoomId = 0;
		send(new ActionFailed());
	}

	public void handleExAskJoinMPCC(RequestExAskJoinMPCC p) {
		PlayerCharacter active = active();
		if (active == null || p.name() == null || ctx() == null || ctx().world() == null) {
			return;
		}
		Party requestorParty = session.party();
		ctx().world().byName(p.name().trim()).ifPresent(target -> {
			if (target instanceof GameSession ts) {
				if (requestorParty != null && ts.party() != null && ts.party() == requestorParty) {
					send(new ActionFailed());
					return;
				}
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
		Party targetParty = session.party();
		Party inviterParty = null;
		if (inviter != null && ctx() != null && ctx().world() != null) {
			var inviterOpt = ctx().world().byName(inviter);
			if (inviterOpt.isPresent() && inviterOpt.get() instanceof GameSession is) {
				inviterParty = is.party();
			}
		}
		if (inviterParty != null && targetParty != null && inviterParty != targetParty) {
			Party.CommandChannel cc = inviterParty.commandChannel();
			if (cc == null) {
				cc = new Party.CommandChannel(inviterParty);
			}
			cc.addParty(targetParty);
			return;
		}
		send(new ExOpenMPCC());
		List<MpccPartyEntry> parties = List.of(new MpccPartyEntry(active.name(), active.objectId(),
				targetParty != null ? targetParty.members().size() : 1));
		send(new ExMultiPartyCommandChannelInfo(leaderName, 0, parties.get(0).memberCount(), parties));
	}

	public void handleExOustFromMPCC(RequestExOustFromMPCC p) {
		Party party = session.party();
		if (party != null && party.commandChannel() != null && p.name() != null) {
			Party.CommandChannel cc = party.commandChannel();
			for (Party pt : cc.parties()) {
				if (pt.leader() != null && pt.leader().name().equalsIgnoreCase(p.name().trim())) {
					cc.removeParty(pt);
					return;
				}
			}
		}
		send(new ExCloseMPCC());
		if (p.name() != null && ctx() != null && ctx().world() != null) {
			ctx().world().byName(p.name().trim()).ifPresent(target -> target.send(new ExCloseMPCC()));
		}
	}

	public void handleExMPCCShowPartyMembersInfo(RequestExMPCCShowPartyMembersInfo p) {
		List<MpccPartyMemberEntry> members = new ArrayList<>();
		Party party = session.party();
		Party targetParty = party;
		if (party != null && party.commandChannel() != null) {
			for (Party pt : party.commandChannel().parties()) {
				if (pt.leader() != null && pt.leader().objectId() == p.partyLeaderId()) {
					targetParty = pt;
					break;
				}
			}
		}
		if (targetParty != null) {
			for (var m : targetParty.members()) {
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
		send(new PartyMemberPosition(pos));
		List<EffectInfo> effects = new ArrayList<>();
		if (active.effects() != null && active.effects().active() != null) {
			long now = System.currentTimeMillis();
			for (var buff : active.effects().active()) {
				int durSec = buff.endTimeMillis() == Long.MAX_VALUE ? -1
						: Math.max(0, (int) ((buff.endTimeMillis() - now) / 1000L));
				effects.add(new EffectInfo(buff.skillId(), buff.level(), durSec));
			}
		}
		send(new PartySpelled(0, active.objectId(), effects));
	}

	// =========================================================================
	// 4. Dwarven Manufacture, Recipe Book, Freight, Manor, Augmentation & Preview
	// =========================================================================

	public void handlePrivateStoreManageCancel(RequestPrivateStoreManageCancel p) {
		PlayerCharacter active = active();
		if (active != null && !active.isStoreOpen()) {
			active.privateStoreType(PlayerCharacter.STORE_TYPE_NONE);
		}
		send(new ActionFailed());
	}

	public List<ManufactureRecipeEntry> getManufactureList() {
		return List.copyOf(manufactureList);
	}

	public void syncManufactureList(List<ManufactureRecipeEntry> list) {
		manufactureList.clear();
		if (list != null) {
			manufactureList.addAll(list);
		}
	}

	public void handleRecipeBookDestroy(RequestRecipeBookDestroy p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		if (active.privateStoreType() == PlayerCharacter.STORE_MANUFACTURE) {
			send(SystemMessage.id(SystemMessage.CANT_ALTER_RECIPEBOOK_WHILE_CRAFTING));
			send(new ActionFailed());
			return;
		}
		recipeBook.remove(Integer.valueOf(p.recipeId()));
		RecipeService rs = (ctx() != null && ctx().recipes() != null)
				? ctx().recipes()
				: (GameEngineContext.getBean(RecipeService.class) != null
						? GameEngineContext.getBean(RecipeService.class)
						: RecipeService.getInstance());
		boolean isDwarven = true;
		if (rs != null) {
			var recipeOpt = rs.getRecipe(p.recipeId());
			if (recipeOpt.isPresent()) {
				isDwarven = recipeOpt.get().isDwarvenRecipe();
			}
			rs.unregisterRecipe(active.objectId(), p.recipeId());
			List<Integer> updated = new ArrayList<>(rs.getRecipeBook(active.objectId(), isDwarven));
			send(new RecipeBookItemList(isDwarven, active.maxMp(), updated));
			return;
		}
		send(new RecipeBookItemList(true, active.maxMp(), new ArrayList<>(recipeBook)));
	}

	public void handleRecipeShopManageList(RequestRecipeShopManageList p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		if (active.privateStoreType() == PlayerCharacter.STORE_MANUFACTURE) {
			active.privateStoreType(PlayerCharacter.STORE_TYPE_NONE);
			active.sitting(false);
			session.sendUserInfoAndBroadcastCharInfo();
		}
		RecipeService rs = (ctx() != null && ctx().recipes() != null)
				? ctx().recipes()
				: (GameEngineContext.getBean(RecipeService.class) != null
						? GameEngineContext.getBean(RecipeService.class)
						: RecipeService.getInstance());
		List<Integer> book = new ArrayList<>(recipeBook);
		if (rs != null) {
			for (Integer rId : rs.getRecipeBook(active.objectId(), true)) {
				if (!book.contains(rId)) {
					book.add(rId);
				}
			}
			if (manufactureList.isEmpty()) {
				for (var item : rs.getManufactureList(active.objectId())) {
					if (book.contains(item.recipeId())) {
						manufactureList.add(new ManufactureRecipeEntry(item.recipeId(), item.cost()));
					}
				}
			}
		}
		send(new RecipeShopManageList(active.objectId(), (int) active.inventory().adena(), true, book,
				manufactureList));
	}

	public void handleRecipeShopMessageSet(RequestRecipeShopMessageSet p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		manufactureMessage = p.name() != null ? p.name() : "";
		if (manufactureMessage.length() > 29) {
			manufactureMessage = manufactureMessage.substring(0, 29);
		}
		active.storeTitle(manufactureMessage);
		send(new RecipeShopMsg(active.objectId(), manufactureMessage));
	}

	public void handleRecipeShopListSet(RequestRecipeShopListSet p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		if (active.isInCombat()) {
			send(SystemMessage.id(SystemMessage.CANT_CRAFT_DURING_COMBAT));
			send(new ActionFailed());
			return;
		}
		int maxSlots = active.race() == 4 ? Config.MAX_PVTSTORE_SLOTS_DWARF : Config.MAX_PVTSTORE_SLOTS_OTHER;
		if (p != null && p.items() != null && p.items().size() > maxSlots) {
			send(SystemMessage.id(SystemMessage.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED));
			handleRecipeShopManageQuit(new RequestRecipeShopManageQuit());
			return;
		}
		RecipeService rs = (ctx() != null && ctx().recipes() != null)
				? ctx().recipes()
				: (GameEngineContext.getBean(RecipeService.class) != null
						? GameEngineContext.getBean(RecipeService.class)
						: RecipeService.getInstance());
		manufactureList.clear();
		List<RecipeService.ManufactureItem> serviceItems = new ArrayList<>();
		if (p != null && p.items() != null) {
			for (var it : p.items()) {
				if (it.cost() >= 0) {
					manufactureList.add(new ManufactureRecipeEntry(it.recipeId(), it.cost()));
					serviceItems.add(new RecipeService.ManufactureItem(it.recipeId(), it.cost()));
				}
			}
		}
		if (rs != null) {
			rs.setManufactureList(active.objectId(), serviceItems);
		}
		if (manufactureList.isEmpty()) {
			handleRecipeShopManageQuit(new RequestRecipeShopManageQuit());
			return;
		}
		String msg = active.storeTitle() != null && !active.storeTitle().isEmpty() ? active.storeTitle()
				: manufactureMessage;
		active.storeTitle(msg);
		active.sitting(true);
		active.privateStoreType(PlayerCharacter.STORE_TYPE_MANUFACTURE);
		session.sendUserInfoAndBroadcastCharInfo();
		var msgPkt = new RecipeShopMsg(active.objectId(), msg);
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
		manufactureList.clear();
		RecipeService rs = (ctx() != null && ctx().recipes() != null)
				? ctx().recipes()
				: (GameEngineContext.getBean(RecipeService.class) != null
						? GameEngineContext.getBean(RecipeService.class)
						: RecipeService.getInstance());
		if (rs != null) {
			rs.setManufactureList(active.objectId(), List.of());
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
		int curMp = (int) active.currentMp();
		int maxMp = active.maxMp();
		if (ctx() != null && ctx().world() != null) {
			var crafterOpt = ctx().world().byId(p.playerObjectId());
			if (crafterOpt.isPresent() && crafterOpt.get().character() != null) {
				curMp = (int) crafterOpt.get().character().currentMp();
				maxMp = crafterOpt.get().character().maxMp();
			}
		}
		send(new RecipeShopItemInfo(p.playerObjectId(), p.recipeId(), curMp, maxMp, -1));
	}

	public void handleRecipeShopMakeItem(RequestRecipeShopMakeItem p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		if (ctx() != null && ctx().world() != null && p.playerObjectId() != active.objectId()) {
			var crafterOpt = ctx().world().byId(p.playerObjectId());
			if (crafterOpt.isPresent() && crafterOpt.get().character() != null
					&& crafterOpt.get().character().privateStoreType() == PlayerCharacter.STORE_TYPE_MANUFACTURE) {
				session.tradeStoreHandler().handleRecipeShopMakeItem(p);
				return;
			}
		}
		RecipeService rs = GameEngineContext.getBean(RecipeService.class);
		if (rs == null) {
			rs = RecipeService.getInstance();
		}
		RecipeEntry recipe = rs != null ? rs.getRecipe(p.recipeId()).orElse(null) : null;
		boolean ok = recipe != null;
		send(new RecipeShopItemInfo(p.playerObjectId(), p.recipeId(), (int) active.currentMp(), active.maxMp(),
				ok ? 1 : 0));
	}

	public void handleRecipeShopManagePrev(RequestRecipeShopManagePrev p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		if (ctx() != null && ctx().world() != null && p != null && p.playerObjectId() != 0
				&& p.playerObjectId() != active.objectId()) {
			var crafterOpt = ctx().world().byId(p.playerObjectId());
			if (crafterOpt.isPresent() && crafterOpt.get().character() != null) {
				session.tradeStoreHandler().showRecipeShopSellList(crafterOpt.get().character());
				return;
			}
		}
		send(new RecipeShopSellList(active.objectId(), (int) active.currentMp(), active.maxMp(),
				(int) active.inventory().adena(), manufactureList));
	}

	public void handlePackageSendableItemList(RequestPackageSendableItemList p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		Map<Integer, String> chars = new LinkedHashMap<>();
		if (ctx() != null && ctx().warehouse() != null && session.account() != null) {
			chars.putAll(ctx().warehouse().getAccountCharactersForFreight(session.account(), active.objectId()));
		}
		if (chars.isEmpty()) {
			chars.put(p.playerObjectId(), "AccountChar");
		}
		send(new PackageToList(chars));
		List<WarehouseItem> items = new ArrayList<>();
		for (ItemInstance it : active.inventory().items()) {
			if (!it.isEquipped() && it.itemId() != ItemTemplate.ADENA_ID
					&& (it.template() == null || it.template().freightable())) {
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
		if (ctx() != null && ctx().warehouse() != null) {
			int targetCharId = p.playerObjectId() > 0 ? p.playerObjectId() : active.objectId();
			for (var wItem : p.items()) {
				if (wItem.count() <= 0) {
					continue;
				}
				ctx().warehouse().depositFreightItem(active.inventory(), targetCharId, wItem.objectId(), wItem.count());
			}
			send(ItemList.of(active.inventory().items(), false));
			session.sendStatusUpdate();
			send(SystemMessage.id(282)); // Deposited in warehouse.
			return;
		}
		session.onWareHouseDeposit(new SendWareHouseDepositList(p.items()));
	}

	public void handleManorList(RequestManorList p) {
		List<String> manors = List.of(
				"gludio", "dion", "giran", "oren", "aden",
				"innadril", "goddard", "rune", "schuttgart");
		send(new ExSendManorList(manors));
	}

	public void handleBuyProcure(RequestBuyProcure p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		CastleManorManager manor = ctx() != null ? ctx().manor() : null;
		List<ProcureSellEntry> list = new ArrayList<>();
		List<ItemInfo> changes = new ArrayList<>();
		if (p.items() != null) {
			for (var it : p.items()) {
				if (it.count() <= 0) {
					continue;
				}
				int rewardItemId = 1864;
				int rewardCount = it.count();
				if (manor != null) {
					SeedTemplate st = manor.getSeedByCrop(it.itemId()).orElse(null);
					if (st != null) {
						rewardItemId = st.reward1();
					}
					CropProcure cp = manor.getCropProcure(p.manorId() > 0 ? p.manorId() : 1, it.itemId(),
							CastleManorManager.PERIOD_CURRENT).orElse(null);
					if (cp != null && st != null) {
						rewardItemId = cp.reward() == 2 ? st.reward2() : st.reward1();
					}
				}
				var invCropOpt = active.inventory().byItemId(it.itemId());
				int cropPrice = (manor != null && manor.getCropProcure(p.manorId() > 0 ? p.manorId() : 1, it.itemId(),
						CastleManorManager.PERIOD_CURRENT).isPresent())
								? manor.getCropProcure(p.manorId() > 0 ? p.manorId() : 1, it.itemId(),
										CastleManorManager.PERIOD_CURRENT).get().price()
								: 0;
				int rewardRefPrice = (ctx() != null && ctx().items() != null
						&& ctx().items().get(rewardItemId).isPresent()
						&& ctx().items().get(rewardItemId).get().price() > 0)
								? ctx().items().get(rewardItemId).get().price()
								: 100;
				int cropObjId = invCropOpt.map(ItemInstance::objectId).orElse(0);
				if (invCropOpt.isPresent() && ctx() != null && ctx().inventories() != null) {
					ItemInstance cropItem = invCropOpt.get();
					int actualCount = Math.min(it.count(), cropItem.count());
					if (actualCount > 0 && (manor == null
							|| manor.sellCrop(p.manorId() > 0 ? p.manorId() : 1, it.itemId(), actualCount) >= 0)) {
						if (ctx().inventories().destroyItem(active.inventory(), cropItem.objectId(), actualCount,
								"SellProcureCrop") != null) {
							ItemInstance remaining = active.inventory().byObjectId(cropItem.objectId()).orElse(null);
							if (remaining != null) {
								changes.add(ItemInfo.of(remaining, ItemInfo.MODIFIED));
							} else {
								changes.add(ItemInfo.of(cropItem, ItemInfo.REMOVED));
							}
							int calcRewardCount = cropPrice > 0
									? Math.max(1, (int) ((long) actualCount * cropPrice / Math.max(1, rewardRefPrice)))
									: actualCount;
							rewardCount = calcRewardCount;
							var rewardRes = ctx().inventories().addItem(active.inventory(), rewardItemId,
									calcRewardCount, "ProcureReward");
							if (rewardRes != null && rewardRes.item() != null) {
								changes.add(ItemInfo.of(rewardRes.item(), ItemInfo.MODIFIED));
							}
						}
					}
				}
				list.add(new ProcureSellEntry(4, cropObjId, it.itemId(), rewardCount, 5));
			}
		}
		if (!changes.isEmpty()) {
			send(new InventoryUpdate(changes));
		}
		send(new SellListProcure((int) active.inventory().adena(), list));
	}

	public void handleBuySeed(RequestBuySeed p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		CastleManorManager manor = ctx() != null ? ctx().manor() : null;
		CastleManager castles = ctx() != null ? ctx().castles() : null;
		List<SeedBuyEntry> seeds = new ArrayList<>();
		List<ItemInfo> changes = new ArrayList<>();
		long totalCost = 0L;

		if (p.seeds() != null) {
			// 1. Validate quantities & prices first (ZERO DOAÇÕES - must pay Adena)
			for (var s : p.seeds()) {
				if (s.count() <= 0) {
					send(new ActionFailed());
					return;
				}
				int unitPrice = 100;
				if (manor != null) {
					SeedProduction sp = manor
							.getSeedProduction(p.manorId(), s.itemId(), CastleManorManager.PERIOD_CURRENT).orElse(null);
					if (sp != null) {
						if (sp.amount() < s.count()) {
							send(SystemMessage.id(348)); // Incorrect item count.
							send(new ActionFailed());
							return;
						}
						unitPrice = Math.max(1, sp.price());
					} else {
						SeedTemplate st = manor.getSeed(s.itemId()).orElse(null);
						if (st != null) {
							unitPrice = Math.max(1, st.seedDefaultPrice());
						}
					}
				}
				totalCost = Math.addExact(totalCost, (long) unitPrice * s.count());
			}

			if (totalCost > active.inventory().adena() || totalCost > Integer.MAX_VALUE) {
				send(SystemMessage.id(279)); // You do not have enough adena.
				send(new ActionFailed());
				return;
			}

			// 2. Deduct Adena upfront
			if (totalCost > 0 && ctx() != null && ctx().inventories() != null) {
				ItemInstance adena = active.inventory().byItemId(ItemTemplate.ADENA_ID).orElse(null);
				if (adena == null || ctx().inventories().destroyItem(active.inventory(), adena.objectId(),
						(int) totalCost, "BuySeed") == null) {
					send(SystemMessage.id(279));
					send(new ActionFailed());
					return;
				}
				ItemInstance remAdena = active.inventory().byItemId(ItemTemplate.ADENA_ID).orElse(null);
				if (remAdena != null) {
					changes.add(ItemInfo.of(remAdena, ItemInfo.MODIFIED));
				} else {
					changes.add(ItemInfo.of(adena, ItemInfo.REMOVED));
				}
				if (castles != null && p.manorId() > 0 && manor == null) {
					castles.addToTreasury(p.manorId(), totalCost);
				}
			}

			// 3. Deliver seeds and decrement Manor stock
			for (var s : p.seeds()) {
				int unitPrice = 100;
				if (manor != null) {
					SeedProduction sp = manor
							.getSeedProduction(p.manorId(), s.itemId(), CastleManorManager.PERIOD_CURRENT).orElse(null);
					if (sp != null) {
						unitPrice = Math.max(1, sp.price());
						manor.buySeed(p.manorId(), s.itemId(), s.count());
					} else {
						SeedTemplate st = manor.getSeed(s.itemId()).orElse(null);
						if (st != null) {
							unitPrice = Math.max(1, st.seedDefaultPrice());
						}
					}
				}
				seeds.add(new SeedBuyEntry(s.itemId(), s.count(), unitPrice));
				if (ctx() != null && ctx().inventories() != null && s.count() > 0) {
					var addedRes = ctx().inventories().addItem(active.inventory(), s.itemId(), s.count(), "BuySeed");
					if (addedRes != null && addedRes.item() != null) {
						changes.add(ItemInfo.of(addedRes.item(), ItemInfo.MODIFIED));
					}
				}
			}
		}
		if (!changes.isEmpty()) {
			send(new InventoryUpdate(changes));
			session.sendStatusUpdate();
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
		int count = p.items() != null ? Math.min(p.items().size(), 17) : 0;
		long totalWearCost = (long) count * Math.max(0, Config.WEAR_PRICE);
		if (totalWearCost > 0 && ctx() != null && ctx().inventories() != null
				&& active.inventory().adena() >= totalWearCost) {
			ItemInstance adena = active.inventory().byItemId(ItemTemplate.ADENA_ID).orElse(null);
			if (adena != null) {
				ctx().inventories().destroyItem(active.inventory(), adena.objectId(), (int) totalWearCost,
						"WearPreview");
			}
		}
		if (p.items() != null) {
			for (int i = 0; i < p.items().size() && i < 17; i++) {
				int itemId = p.items().get(i);
				int bodyPart = 0;
				int type2 = 0;
				if (ctx() != null && ctx().items() != null) {
					ItemTemplate tpl = ctx().items().get(itemId).orElse(null);
					if (tpl != null) {
						bodyPart = tpl.bodyPart();
						type2 = tpl.type2();
					}
				}
				int slot = mapBodyPartToPreviewSlot(bodyPart, i);
				paperdoll[slot] = itemId;
				products.add(new ShopPreviewProduct(itemId, type2, bodyPart, Config.WEAR_PRICE));
			}
		}
		send(new ShopPreviewList(p.listId(), (int) active.inventory().adena(), products));
		send(new ShopPreviewInfo(paperdoll));
	}

	private static int mapBodyPartToPreviewSlot(int bodyPart, int fallbackIndex) {
		return switch (bodyPart) {
			case com.lopez.l2j.game.item.ItemSlots.SLOT_UNDERWEAR -> 0;
			case com.lopez.l2j.game.item.ItemSlots.SLOT_R_EAR -> 1;
			case com.lopez.l2j.game.item.ItemSlots.SLOT_L_EAR -> 2;
			case com.lopez.l2j.game.item.ItemSlots.SLOT_NECK -> 3;
			case com.lopez.l2j.game.item.ItemSlots.SLOT_R_FINGER -> 4;
			case com.lopez.l2j.game.item.ItemSlots.SLOT_L_FINGER -> 5;
			case com.lopez.l2j.game.item.ItemSlots.SLOT_HEAD -> 6;
			case com.lopez.l2j.game.item.ItemSlots.SLOT_R_HAND -> 7;
			case com.lopez.l2j.game.item.ItemSlots.SLOT_L_HAND -> 8;
			case com.lopez.l2j.game.item.ItemSlots.SLOT_GLOVES -> 9;
			case com.lopez.l2j.game.item.ItemSlots.SLOT_CHEST, com.lopez.l2j.game.item.ItemSlots.SLOT_FULL_ARMOR,
					com.lopez.l2j.game.item.ItemSlots.SLOT_ALLDRESS ->
				10;
			case com.lopez.l2j.game.item.ItemSlots.SLOT_LEGS -> 11;
			case com.lopez.l2j.game.item.ItemSlots.SLOT_FEET -> 12;
			case com.lopez.l2j.game.item.ItemSlots.SLOT_BACK -> 13;
			case com.lopez.l2j.game.item.ItemSlots.SLOT_LR_HAND -> 14;
			case com.lopez.l2j.game.item.ItemSlots.SLOT_HAIR -> 15;
			case com.lopez.l2j.game.item.ItemSlots.SLOT_FACE, com.lopez.l2j.game.item.ItemSlots.SLOT_HAIRALL -> 16;
			default -> Math.max(0, Math.min(16, fallbackIndex));
		};
	}

	public void handleProcureCropList(RequestProcureCropList p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		CastleManorManager manor = ctx() != null ? ctx().manor() : null;
		List<SellCropEntry> crops = new ArrayList<>();
		List<CropProcureDetailEntry> details = new ArrayList<>();
		List<ItemInfo> changes = new ArrayList<>();
		if (p.items() != null) {
			for (var c : p.items()) {
				int seedLevel = 1;
				int reward1 = 1864;
				int reward2 = 1865;
				int price = 100;
				int rewardType = 1;
				if (manor != null) {
					SeedTemplate st = manor.getSeedByCrop(c.itemId()).orElse(null);
					if (st != null) {
						seedLevel = st.level();
						reward1 = st.reward1();
						reward2 = st.reward2();
						price = st.cropDefaultPrice();
					}
					CropProcure cp = manor.getCropProcure(c.manorId(), c.itemId(), CastleManorManager.PERIOD_CURRENT)
							.orElse(null);
					if (cp != null) {
						price = cp.price();
						rewardType = cp.reward();
					}
				}
				var invCropOpt = active.inventory().byObjectId(c.objectId());
				if (invCropOpt.isPresent() && c.count() > 0 && ctx() != null && ctx().inventories() != null) {
					ItemInstance invCrop = invCropOpt.get();
					int sellCount = Math.min(c.count(), invCrop.count());
					if (sellCount > 0 && (manor == null || manor.sellCrop(c.manorId(), c.itemId(), sellCount) >= 0)) {
						if (ctx().inventories().destroyItem(active.inventory(), invCrop.objectId(), sellCount,
								"ProcureCrop") != null) {
							ItemInstance rem = active.inventory().byObjectId(invCrop.objectId()).orElse(null);
							if (rem != null) {
								changes.add(ItemInfo.of(rem, ItemInfo.MODIFIED));
							} else {
								changes.add(ItemInfo.of(invCrop, ItemInfo.REMOVED));
							}
							int rewardItemId = rewardType == 2 ? reward2 : reward1;
							int procPrice = (manor != null
									&& manor.getCropProcure(c.manorId(), c.itemId(), CastleManorManager.PERIOD_CURRENT)
											.isPresent())
													? manor.getCropProcure(c.manorId(), c.itemId(),
															CastleManorManager.PERIOD_CURRENT).get().price()
													: 0;
							int rewardRefPrice = (ctx().items() != null && ctx().items().get(rewardItemId).isPresent()
									&& ctx().items().get(rewardItemId).get().price() > 0)
											? ctx().items().get(rewardItemId).get().price()
											: 100;
							int calcRewardCount = procPrice > 0
									? Math.max(1, (int) ((long) sellCount * procPrice / Math.max(1, rewardRefPrice)))
									: sellCount;
							var rewardRes = ctx().inventories().addItem(active.inventory(), rewardItemId,
									calcRewardCount, "ProcureCropReward");
							if (rewardRes != null && rewardRes.item() != null) {
								changes.add(ItemInfo.of(rewardRes.item(), ItemInfo.MODIFIED));
							}
						}
					}
				}
				crops.add(new SellCropEntry(c.objectId(), c.itemId(), seedLevel, reward1, reward2, c.manorId(),
						c.count(), price, rewardType, c.count()));
				details.add(new CropProcureDetailEntry(c.manorId(), c.count(), price, rewardType));
			}
		}
		if (!changes.isEmpty()) {
			send(new InventoryUpdate(changes));
		}
		send(new ExShowSellCropList(1, crops));
		send(new ExShowProcureCropDetail(crops.isEmpty() ? 0 : crops.get(0).cropId(), details));
	}

	public void handleSetSeed(RequestSetSeed p) {
		CastleManorManager manor = ctx() != null ? ctx().manor() : null;
		List<SeedInfoEntry> seeds = new ArrayList<>();
		List<SeedProduction> nextPeriod = new ArrayList<>();
		if (p.seeds() != null) {
			for (var s : p.seeds()) {
				int level = 1;
				int r1 = 1864;
				int r2 = 1865;
				if (manor != null) {
					SeedTemplate st = manor.getSeed(s.itemId()).orElse(null);
					if (st != null) {
						level = st.level();
						r1 = st.reward1();
						r2 = st.reward2();
					}
				}
				seeds.add(new SeedInfoEntry(s.itemId(), s.sales(), s.sales(), s.price(), level, r1, r2));
				if (s.sales() >= 0 && s.price() >= 0) {
					nextPeriod.add(new SeedProduction(s.itemId(), s.sales(), s.sales(), s.price()));
				}
			}
		}
		if (manor != null && p.manorId() > 0 && !nextPeriod.isEmpty()) {
			manor.setSeedProduction(p.manorId(), nextPeriod, CastleManorManager.PERIOD_NEXT);
		}
		send(new ExShowSeedSetting(p.manorId(), seeds));
		send(new ExShowSeedInfo(p.manorId(), seeds));
	}

	public void handleSetCrop(RequestSetCrop p) {
		CastleManorManager manor = ctx() != null ? ctx().manor() : null;
		List<CropInfoEntry> crops = new ArrayList<>();
		List<ManorDefaultEntry> defaults = new ArrayList<>();
		List<CropProcure> nextPeriod = new ArrayList<>();
		if (p.crops() != null) {
			for (var c : p.crops()) {
				int level = 1;
				int r1 = 1864;
				int r2 = 1865;
				int defSeedPrice = 10;
				if (manor != null) {
					SeedTemplate st = manor.getSeedByCrop(c.itemId()).orElse(null);
					if (st != null) {
						level = st.level();
						r1 = st.reward1();
						r2 = st.reward2();
						defSeedPrice = st.seedDefaultPrice();
					}
				}
				crops.add(new CropInfoEntry(c.itemId(), c.sales(), c.sales(), c.price(), c.type(), level, r1, r2));
				defaults.add(new ManorDefaultEntry(c.itemId(), level, defSeedPrice, c.price(), r1, r2));
				if (c.sales() >= 0 && c.price() >= 0) {
					nextPeriod.add(new CropProcure(c.itemId(), c.sales(), c.sales(), c.price(), c.type()));
				}
			}
		}
		if (manor != null && p.manorId() > 0 && !nextPeriod.isEmpty()) {
			manor.setCropProcure(p.manorId(), nextPeriod, CastleManorManager.PERIOD_NEXT);
		}
		send(new ExShowCropSetting(p.manorId(), crops));
		send(new ExShowCropInfo(p.manorId(), crops));
		send(new ExShowManorDefaultInfo(defaults));
	}

	// =========================================================================
	// 4B. Augmentation (Refinery & Cancel) (#067-#070, #279)
	// =========================================================================

	public void handleConfirmTargetItem(RequestConfirmTargetItem p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		ItemInstance item = active.inventory().byObjectId(p.itemObjId()).orElse(null);
		if (item == null || !AugmentationService.isValidAugmentTarget(item) || item.isAugmented()) {
			send(SystemMessage.id(1959)); // This is not a suitable item.
			send(new ActionFailed());
			return;
		}
		send(new ExPutItemResultForVariationMake(p.itemObjId()));
	}

	public void handleConfirmRefinerItem(RequestConfirmRefinerItem p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		ItemInstance targetItem = active.inventory().byObjectId(p.targetItemObjId()).orElse(null);
		ItemInstance refinerItem = active.inventory().byObjectId(p.refinerItemObjId()).orElse(null);
		if (targetItem == null || refinerItem == null || !AugmentationService.isLifeStone(refinerItem.itemId())
				|| !AugmentationService.isValidLifeStoneForPlayer(refinerItem.itemId(), active.level())) {
			send(SystemMessage.id(1959));
			send(new ActionFailed());
			return;
		}
		int gemStoneId = AugmentationService.getRequiredGemstoneId(targetItem);
		int gemStoneCount = AugmentationService.getRequiredGemstoneCount(targetItem);
		send(new ExPutIntensiveResultForVariationMake(p.refinerItemObjId(), refinerItem.itemId(), gemStoneId,
				gemStoneCount));
	}

	public void handleConfirmGemStone(RequestConfirmGemStone p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		ItemInstance targetItem = active.inventory().byObjectId(p.targetItemObjId()).orElse(null);
		ItemInstance gemStone = active.inventory().byObjectId(p.gemstoneItemObjId()).orElse(null);
		int reqGemId = AugmentationService.getRequiredGemstoneId(targetItem);
		int reqGemCount = AugmentationService.getRequiredGemstoneCount(targetItem);
		if (targetItem == null || gemStone == null || gemStone.itemId() != reqGemId
				|| p.gemstoneCount() < reqGemCount) {
			send(SystemMessage.id(1960)); // Gemstone quantity is incorrect.
			send(new ActionFailed());
			return;
		}
		send(new ExPutCommissionResultForVariationMake(p.gemstoneItemObjId(), p.gemstoneCount()));
	}

	public void handleRefine(RequestRefine p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		AugmentationService augSvc = ctx() != null ? ctx().augmentations() : null;
		if (augSvc == null) {
			send(new ExVariationResult(0, 0, 0));
			return;
		}
		ItemInstance targetItem = active.inventory().byObjectId(p.targetItemObjId()).orElse(null);
		ItemInstance refinerItem = active.inventory().byObjectId(p.refinerItemObjId()).orElse(null);
		ItemInstance gemStone = active.inventory().byObjectId(p.gemstoneItemObjId()).orElse(null);
		int reqGemId = AugmentationService.getRequiredGemstoneId(targetItem);
		int reqGemCount = AugmentationService.getRequiredGemstoneCount(targetItem);
		if (targetItem == null || refinerItem == null || gemStone == null
				|| !AugmentationService.isValidAugmentTarget(targetItem)
				|| targetItem.isAugmented()
				|| !AugmentationService.isLifeStone(refinerItem.itemId())
				|| !AugmentationService.isValidLifeStoneForPlayer(refinerItem.itemId(), active.level())
				|| gemStone.itemId() != reqGemId
				|| gemStone.count() < reqGemCount
				|| p.gemstoneCount() < reqGemCount) {
			send(new ExVariationResult(0, 0, 0));
			return;
		}
		if (ctx().inventories() != null) {
			if (ctx().inventories().destroyItem(active.inventory(), refinerItem.objectId(), 1,
					"AugmentLifeStone") == null
					|| ctx().inventories().destroyItem(active.inventory(), gemStone.objectId(), reqGemCount,
							"AugmentGemStone") == null) {
				send(new ExVariationResult(0, 0, 0));
				return;
			}
		}
		var aug = augSvc.augmentItem(targetItem, refinerItem.itemId());
		if (aug == null) {
			send(new ExVariationResult(0, 0, 0));
			return;
		}
		int stat12 = aug.effect1Id();
		int stat34 = aug.effect2Id();
		send(new ExVariationResult(stat12, stat34, 1));
		send(new InventoryUpdate(List.of(ItemInfo.of(targetItem, ItemInfo.MODIFIED))));
		session.sendStatusUpdate();
	}

	public void handleConfirmCancelItem(RequestConfirmCancelItem p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		ItemInstance item = active.inventory().byObjectId(p.itemObjId()).orElse(null);
		if (item == null || !item.isAugmented()) {
			send(SystemMessage.id(1964)); // Augmentation removal can only be done on an augmented item.
			send(new ActionFailed());
			return;
		}
		long price = AugmentationService.getCancelFee(item);
		int augId = item.augmentation() != null ? item.augmentation().augmentationId() : 0;
		send(new ExPutItemResultForVariationCancel(item.objectId(), item.itemId(), augId & 0xFFFF,
				(augId >> 16) & 0xFFFF, price));
	}

	public void handleRefineCancel(RequestRefineCancel p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		AugmentationService augSvc = ctx() != null ? ctx().augmentations() : null;
		ItemInstance item = active.inventory().byObjectId(p.itemObjId()).orElse(null);
		if (augSvc == null || item == null || !item.isAugmented()) {
			send(new ExVariationCancelResult(0, 0));
			return;
		}
		long fee = AugmentationService.getCancelFee(item);
		if (active.inventory().adena() < fee) {
			send(SystemMessage.id(279)); // You do not have enough adena.
			send(new ExVariationCancelResult(0, 0));
			return;
		}
		if (fee > 0 && ctx() != null && ctx().inventories() != null) {
			ItemInstance adena = active.inventory().byItemId(ItemTemplate.ADENA_ID).orElse(null);
			if (adena == null || ctx().inventories().destroyItem(active.inventory(), adena.objectId(), (int) fee,
					"AugmentCancelFee") == null) {
				send(new ExVariationCancelResult(0, 0));
				return;
			}
		}
		if (item.isEquipped() && item.augmentation() != null && item.augmentation().skillId() > 0) {
			active.removeSkill(item.augmentation().skillId());
			session.sendSkillList();
		}
		augSvc.removeAugmentation(item);
		send(new ExVariationCancelResult(1, 1));
		send(new InventoryUpdate(List.of(ItemInfo.of(item, ItemInfo.MODIFIED))));
		session.sendUserInfoAndBroadcastCharInfo();
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
		PetService.getInstance().changePetName(active, summon, p.name(), this::send);
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
		PetService.getInstance().usePetItem(active, active.activeSummon(), p.objectId(), this::send);
		var itemOpt = active.inventory().byObjectId(p.objectId());
		List<PetInventoryItem> items = new ArrayList<>();
		itemOpt.ifPresent(it -> items.add(new PetInventoryItem(2, 0, it.objectId(), it.itemId(), it.count(), 0, 0,
				it.isEquipped(), 0, it.enchant(), 0)));
		send(new PetInventoryUpdate(items));
	}

	public void handleGiveItemToPet(RequestGiveItemToPet p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		List<PetInventoryItem> items = new ArrayList<>();
		active.inventory().byObjectId(p.objectId()).ifPresent(it -> items.add(new PetInventoryItem(1, 0, it.objectId(),
				it.itemId(), Math.min(it.count(), p.amount()), 0, 0, false, 0, it.enchant(), 0)));
		PetService.getInstance().transferItemToPet(active, active.activeSummon(), p.objectId(), p.amount(), this::send);
		var summon = active.activeSummon();
		if (summon != null && summon.petInventory() != null && !summon.petInventory().items().isEmpty()) {
			send(PetService.getInstance().buildPetItemListPacket(summon));
		} else {
			send(new PetItemList(items));
		}
		send(new PetInventoryUpdate(items));
	}

	public void handleGetItemFromPet(RequestGetItemFromPet p) {
		PlayerCharacter active = active();
		if (active != null) {
			PetService.getInstance().transferItemFromPet(active, active.activeSummon(), p.objectId(), p.amount(),
					this::send);
		}
		List<PetInventoryItem> items = List
				.of(new PetInventoryItem(3, 0, p.objectId(), 0, p.amount(), 0, 0, false, 0, 0, 0));
		send(new PetInventoryUpdate(items));
	}

	public void handlePetGetItem(RequestPetGetItem p) {
		PlayerCharacter active = active();
		if (active != null && active.activeSummon() != null && ctx() != null && ctx().groundItems() != null) {
			var gi = ctx().groundItems().byObjectId(p.objectId()).orElse(null);
			if (gi != null) {
				PetService.getInstance().petPickUpItem(active, active.activeSummon(), gi, ctx().groundItems(),
						this::send);
			}
		}
		if (active == null || active.activeSummon() == null || ctx() == null || ctx().groundItems() == null
				|| ctx().groundItems().byObjectId(p.objectId()).isPresent()) {
			session.actionHandler().tryPickupGroundItem(p.objectId());
		}
		send(new PetInventoryUpdate(List.of()));
	}

	// =========================================================================
	// 6. Peticoes, GMList, Snoop, Olimpiadas, Hero, Cursed Weapons, Pesca & PC Cafe
	// =========================================================================

	public void handleCursedWeaponList(RequestCursedWeaponList p) {
		List<Integer> ids = new ArrayList<>();
		if (ctx() != null && ctx().cursedWeapons() != null) {
			ids.addAll(ctx().cursedWeapons().getCursedWeaponIds());
		} else {
			ids.add(CursedWeaponsManager.ZARICHE);
			ids.add(CursedWeaponsManager.AKAMANAH);
		}
		send(new ExCursedWeaponList(ids));
	}

	public void handleCursedWeaponLocation(RequestCursedWeaponLocation p) {
		List<CursedWeaponLocationInfo> locs = new ArrayList<>();
		if (ctx() != null && ctx().cursedWeapons() != null) {
			for (var cw : ctx().cursedWeapons().getActiveCursedWeapons()) {
				int x = cw.x();
				int y = cw.y();
				int z = cw.z();
				if (cw.ownerId() > 0 && ctx().world() != null) {
					var ownerOpt = ctx().world().byId(cw.ownerId());
					if (ownerOpt.isPresent()) {
						x = ownerOpt.get().x();
						y = ownerOpt.get().y();
						z = ownerOpt.get().z();
					}
				}
				locs.add(new CursedWeaponLocationInfo(cw.itemId(), 1, x, y, z));
			}
		}
		send(new ExCursedWeaponLocation(locs));
	}

	public void handlePetition(RequestPetition p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		PetitionService svc = (ctx() != null && ctx().petition() != null) ? ctx().petition()
				: GameEngineContext.getBean(PetitionService.class);
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
		PetitionService svc = (ctx() != null && ctx().petition() != null) ? ctx().petition()
				: GameEngineContext.getBean(PetitionService.class);
		if (svc != null) {
			for (var t : svc.getPendingPetitions()) {
				if (t.getPetitionerId() == active.objectId()) {
					svc.closePetition(t.getId());
				}
			}
		}
		send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sua peticao foi cancelada."));
	}

	public void handlePetitionVote(PetitionVote p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		PetitionService svc = (ctx() != null && ctx().petition() != null) ? ctx().petition()
				: GameEngineContext.getBean(PetitionService.class);
		if (svc != null) {
			for (var t : svc.getPendingPetitions()) {
				if (t.getPetitionerId() == active.objectId()) {
					svc.closePetition(t.getId());
				}
			}
		}
		send(new PetitionVotePacket());
		send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Avaliacao de suporte GM registrada (nota=" + p.unk1() + ")."));
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
		if (ctx() != null && ctx().olympiad() != null && active.inObserverMode()) {
			ctx().olympiad().leaveSpectatorMode(active);
		}
		send(new ObservationReturn(rx, ry, rz));
		send(new NormalCamera());
		send(new CameraMode(0));
	}

	public void handleOlympiadObserverEnd(RequestOlympiadObserverEnd p) {
		send(new ExOlympiadMatchEnd());
		send(new ExOlympiadMode(0));
		handleObserverReturn(new ObserverReturn());
	}

	public void handleOlympiadMatchList(RequestOlympiadMatchList p) {
		PlayerCharacter active = active();
		if (active != null) {
			send(new ExOlympiadUserInfoSpectator(1, active.objectId(), active.name(), active.classId(),
					(int) active.currentHp(), active.maxHp(), (int) active.currentCp(), active.maxCp()));
			send(new ExOlympiadSpelledInfo(active.objectId(), List.of()));
		}
		String html = "<html><body>Olympiad Matches</body></html>";
		if (ctx() != null && ctx().olympiad() != null) {
			var matches = ctx().olympiad().getActiveMatches();
			if (matches != null && !matches.isEmpty()) {
				StringBuilder sb = new StringBuilder(
						"<html><body><center>Grand Olympiad Games</center><br><table width=270>");
				for (var entry : matches.entrySet()) {
					var m = entry.getValue();
					sb.append("<tr><td><a action=\"bypass -h _olympiad_observe_").append(entry.getKey())
							.append("\">Arena ")
							.append(entry.getKey() + 1).append("</a></td><td>")
							.append(m.playerOneName()).append(" vs ").append(m.playerTwoName()).append("</td></tr>");
				}
				sb.append("</table></body></html>");
				html = sb.toString();
			}
		}
		send(new NpcHtmlMessage(0, html));
	}

	public void handleWriteHeroWords(RequestWriteHeroWords p) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		if (ctx() != null && ctx().olympiad() != null && active.isHero()) {
			ctx().olympiad().setHeroMessage(active.objectId(), p.heroWords() != null ? p.heroWords() : "");
		}
		List<HeroListEntry> heroes = List.of(new HeroListEntry(active.name(), active.classId(), "", 0, "", 0, 1));
		send(new ExHeroList(heroes));
		send(new CreatureSay(0, CreatureSay.ALL, "SYS",
				"Mensagem de Heroi registrada: " + (p.heroWords() != null ? p.heroWords() : "")));
	}

	public void handleExFishRanking(RequestExFishRanking p) {
		send(new ExAutoFishing(0));
		StringBuilder sb = new StringBuilder(
				"<html><body><center><font color=\"LEVEL\">Fishing Championship Ranking</font></center><br>");
		if (ctx() != null && ctx().fishingChampionship() != null) {
			var top = ctx().fishingChampionship().getTopFishers(5);
			sb.append("<table width=270><tr><td>Rank</td><td>Fisher</td><td>Length</td></tr>");
			if (top != null && !top.isEmpty()) {
				int rank = 1;
				for (var f : top) {
					sb.append("<tr><td>#").append(rank++).append("</td><td>").append(f.playerName())
							.append("</td><td>").append(String.format(java.util.Locale.US, "%.2f", f.fishLength()))
							.append("</td></tr>");
				}
			} else {
				sb.append("<tr><td colspan=3>No entries yet.</td></tr>");
			}
			sb.append("</table>");
			var prev = ctx().fishingChampionship().getPreviousWinners();
			if (prev != null && !prev.isEmpty()) {
				sb.append("<br><font color=\"LEVEL\">Previous Period Winners:</font><table width=270>");
				int r = 1;
				for (var w : prev) {
					sb.append("<tr><td>#").append(r++).append("</td><td>").append(w.playerName())
							.append("</td><td>").append(String.format(java.util.Locale.US, "%.2f", w.fishLength()))
							.append("</td></tr>");
				}
				sb.append("</table>");
			}
		} else {
			sb.append("Fishing Championship Ranking");
		}
		sb.append("</body></html>");
		send(new NpcHtmlMessage(0, sb.toString()));
	}

	public void handlePCCafeCouponUse(RequestPCCafeCouponUse p) {
		send(new ShowPCCafeCouponShowUI());
		PlayerCharacter active = active();
		String code = p.couponCode() != null ? p.couponCode().trim().toUpperCase(java.util.Locale.ROOT) : "";
		int added = 0;
		if (!code.isEmpty() && code.length() >= 4 && redeemedCoupons.add(code)) {
			added = 100;
			if (active != null) {
				active.pcCaffePoints(active.pcCaffePoints() + added);
			}
		}
		int totalPoints = active != null ? (active.pcCaffePoints() > 0 ? active.pcCaffePoints() : 100) : 100;
		send(new ExPCCafePointInfo(totalPoints, added > 0 ? added : 10, 1, 3600, 0));
	}

	public void handleGameGuardReply(GameGuardReply p) {
		session.setGameGuardAuthed(true);
	}

	public void sendProtocolExtendedDiagnostics(NpcInstance npc, ItemInstance item) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		send(new TargetSelected(active.objectId(), npc != null ? npc.objectId() : active.objectId(), active.x(),
				active.y(), active.z()));
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
				(int) active.currentHp(), active.maxHp(), (int) active.currentMp(), active.maxMp(),
				(int) active.currentCp(), active.maxCp()));
		send(new ExEnchantSkillInfoDetail(6622, 1));
		send(new RequestEnchant(1));
		send(new TradeItemUpdate(List.of()));
		send(new ExPrivateStoreSetWholeMsg(active.objectId(), active.storeTitle() != null ? active.storeTitle() : ""));
	}
}
