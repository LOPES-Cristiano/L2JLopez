package com.lopez.l2j.game.skill.handler;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.castle.siege.SiegeRecord;
import com.lopez.l2j.game.castle.siege.SiegeStatus;
import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.clan.ClanMember;
import com.lopez.l2j.game.door.DoorInstance;
import com.lopez.l2j.game.event.pvp.CtfEventService;
import com.lopez.l2j.game.event.pvp.DmEventService;
import com.lopez.l2j.game.event.pvp.TvtEventService;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.party.Party;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.game.summon.SummonInstance;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.game.zone.Zone;
import com.lopez.l2j.game.zone.ZoneType;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.DeleteObject;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.PetDelete;
import com.lopez.l2j.network.game.packet.GameServerPacket.PetInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.PetStatusShow;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro centralizado de {@link ISkillHandler} por {@code skillType} do Lineage II Interlude.
 * Desacopla a execução de habilidades especiais (Summons, Recall/Teleport, Seal of Ruler, Siege Flag,
 * Sweep, Unlock, Balance Life, Spoil, Resurrect, Curas, Buffs e Ofensivas) do {@code MagicSkillPacketHandler}.
 */
@Service
public class SkillHandlerRegistry {

	private static volatile SkillHandlerRegistry instance;

	private final Map<String, ISkillHandler> handlersByType = new ConcurrentHashMap<>();
	private final ObjectIdFactory idFactory;

	@Autowired
	public SkillHandlerRegistry(@Autowired(required = false) ObjectIdFactory idFactory) {
		this.idFactory = idFactory != null ? idFactory : ObjectIdFactory.sequential(0x38000000);
		registerDefaultHandlers();
		instance = this;
	}

	public SkillHandlerRegistry() {
		this(null);
	}

	public static SkillHandlerRegistry getInstance() {
		if (instance == null) {
			instance = new SkillHandlerRegistry();
		}
		return instance;
	}

	public void register(ISkillHandler handler) {
		if (handler == null || handler.skillTypes() == null) {
			return;
		}
		for (String type : handler.skillTypes()) {
			if (type != null && !type.isBlank()) {
				handlersByType.put(type.toUpperCase(Locale.ROOT), handler);
			}
		}
	}

	public ISkillHandler getHandler(String skillType) {
		if (skillType == null) {
			return null;
		}
		return handlersByType.get(skillType.toUpperCase(Locale.ROOT));
	}

	public boolean dispatchSpecialSkill(
			GameSession caster,
			SkillTemplate skill,
			NpcInstance mainNpc,
			DoorInstance doorTarget,
			GameSession targetPlayer,
			boolean ss,
			boolean sps,
			boolean bss) {
		if (caster == null || skill == null) {
			return false;
		}
		// Verifica skills específicos por ID quando o XML usa skillType genérico
		if (skill.id() == 246) {
			return handleTakeCastle(caster, skill, mainNpc);
		}
		if (skill.id() == 247) {
			return handleSiegeFlag(caster, skill);
		}
		if (skill.id() == 1050 || skill.id() == 1255) {
			return handleRecall(caster, skill, targetPlayer);
		}
		if (skill.id() == 1403 || skill.id() == 1429 || skill.id() == 3632) {
			return handleSummonFriendOrGate(caster, skill, targetPlayer);
		}
		ISkillHandler handler = getHandler(skill.skillType());
		if (handler != null) {
			return handler.useSkill(caster, skill, mainNpc, doorTarget, targetPlayer, ss, sps, bss);
		}
		return false;
	}

	private void registerDefaultHandlers() {
		// 1. SUMMON (Servitors de Warlock, Elemental Summoner, Phantom Summoner, Necromancer, Warsmith)
		register(new ISkillHandler() {
			@Override
			public List<String> skillTypes() {
				return List.of("SUMMON");
			}

			@Override
			public boolean useSkill(GameSession caster, SkillTemplate skill, NpcInstance mainNpc,
					DoorInstance doorTarget, GameSession targetPlayer, boolean ss, boolean sps, boolean bss) {
				return handleSummonServitor(caster, skill);
			}
		});

		// 2. RECALL / TELEPORT (Return, Party Recall, Mass Teleport)
		register(new ISkillHandler() {
			@Override
			public List<String> skillTypes() {
				return List.of("RECALL", "TELEPORT");
			}

			@Override
			public boolean useSkill(GameSession caster, SkillTemplate skill, NpcInstance mainNpc,
					DoorInstance doorTarget, GameSession targetPlayer, boolean ss, boolean sps, boolean bss) {
				return handleRecall(caster, skill, targetPlayer);
			}
		});

		// 3. TAKECASTLE (Seal of Ruler - Skill 246)
		register(new ISkillHandler() {
			@Override
			public List<String> skillTypes() {
				return List.of("TAKECASTLE");
			}

			@Override
			public boolean useSkill(GameSession caster, SkillTemplate skill, NpcInstance mainNpc,
					DoorInstance doorTarget, GameSession targetPlayer, boolean ss, boolean sps, boolean bss) {
				return handleTakeCastle(caster, skill, mainNpc);
			}
		});

		// 4. SIEGEFLAG (Build Headquarters - Skill 247)
		register(new ISkillHandler() {
			@Override
			public List<String> skillTypes() {
				return List.of("SIEGEFLAG");
			}

			@Override
			public boolean useSkill(GameSession caster, SkillTemplate skill, NpcInstance mainNpc,
					DoorInstance doorTarget, GameSession targetPlayer, boolean ss, boolean sps, boolean bss) {
				return handleSiegeFlag(caster, skill);
			}
		});

		// 5. SUMMON_FRIEND / GATE_CHANT / CLAN_GATE (Skills 1403, 1429, 3632)
		register(new ISkillHandler() {
			@Override
			public List<String> skillTypes() {
				return List.of("SUMMON_FRIEND", "GATE_CHANT", "CLAN_GATE");
			}

			@Override
			public boolean useSkill(GameSession caster, SkillTemplate skill, NpcInstance mainNpc,
					DoorInstance doorTarget, GameSession targetPlayer, boolean ss, boolean sps, boolean bss) {
				return handleSummonFriendOrGate(caster, skill, targetPlayer);
			}
		});
	}

	private boolean handleSummonServitor(GameSession caster, SkillTemplate skill) {
		PlayerCharacter active = caster.active();
		GameSession.Context ctx = caster.ctx();
		if (active == null || ctx == null) {
			return false;
		}
		// Se ja possuir um summon ativo, remove o anterior
		SummonInstance prev = active.activeSummon();
		if (prev != null) {
			active.activeSummon(null);
			caster.send(new PetDelete(prev.summonType(), prev.objectId()));
			if (ctx.world() != null) {
				ctx.world().removeSummon(prev);
				ctx.world().broadcastAround(caster, GameWorld.VISIBILITY_RADIUS, new DeleteObject(prev.objectId()), true);
			}
		}

		int npcId = resolveServitorNpcId(skill.id(), skill.level());
		NpcTemplate tpl = null;
		if (ctx.npcTemplates() != null) {
			tpl = ctx.npcTemplates().get(npcId).orElse(null);
		}
		if (tpl == null) {
			tpl = NpcTemplate.fallback(npcId, skill.name() != null && !skill.name().isBlank() ? skill.name() : "Servitor", "L2Servitor");
		}

		int summonObjId = idFactory.nextId();
		int servitorLvl = Math.max(active.level(), tpl.level());
		SummonInstance servitor = new SummonInstance(
				summonObjId,
				active,
				tpl,
				SummonInstance.TYPE_SERVITOR,
				0,
				skill.id(),
				servitorLvl);

		active.activeSummon(servitor);
		if (ctx.world() != null) {
			ctx.world().addSummon(servitor);
		}

		caster.send(new PetStatusShow(SummonInstance.TYPE_SERVITOR));
		caster.send(new PetInfo(servitor, 2));
		caster.send(NpcInfo.forSummon(servitor));
		if (ctx.world() != null) {
			ctx.world().broadcastAround(caster, GameWorld.VISIBILITY_RADIUS, NpcInfo.forSummon(servitor), false);
		}
		caster.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Servitor " + servitor.name() + " invocado!"));
		return true;
	}

	private boolean canTeleportPlayer(GameSession session, boolean isRecallTarget) {
		if (session == null || session.active() == null || session.active().isDead()) {
			return false;
		}
		PlayerCharacter p = session.active();
		if (p.isOlympiadMode() || p.isInJail() || p.isDisabled() || p.isRooted() || p.isAlikeDead() || p.instanceId() > 0) {
			return false;
		}
		if (isRecallTarget && (p.pvpFlag() > 0 || p.karma() > 0)) {
			return false;
		}
		if (!isRecallTarget && !Config.ALT_KARMA_PLAYER_CAN_TELEPORT && p.karma() > 0) {
			return false;
		}
		GameSession.Context ctx = session.ctx();
		if (ctx != null) {
			if (ctx.tvt() != null && ctx.tvt().isParticipant(p.objectId()) && ctx.tvt().getState() == TvtEventService.EventState.RUNNING) {
				return false;
			}
			if (ctx.ctf() != null && ctx.ctf().isParticipant(p.objectId()) && ctx.ctf().getState() == CtfEventService.EventState.RUNNING) {
				return false;
			}
			if (ctx.dm() != null && ctx.dm().isParticipant(p.objectId()) && ctx.dm().getState() == DmEventService.EventState.RUNNING) {
				return false;
			}
			if (ctx.dimensionalRift() != null && ctx.dimensionalRift().isInRift(p.objectId())) {
				return false;
			}
		}
		return true;
	}

	private boolean isInRestrictedSummonZone(GameSession session) {
		if (session == null || session.active() == null || session.ctx() == null || session.ctx().zones() == null) {
			return false;
		}
		PlayerCharacter p = session.active();
		List<Zone> zones = session.ctx().zones().getZonesAt(p.x(), p.y(), p.z());
		for (Zone z : zones) {
			if (z.type() == ZoneType.BOSS || z.type() == ZoneType.SIEGE || z.type() == ZoneType.ARENA || z.isArena()) {
				return true;
			}
		}
		return false;
	}

	private boolean handleRecall(GameSession caster, SkillTemplate skill, GameSession targetPlayer) {
		PlayerCharacter active = caster.active();
		GameSession.Context ctx = caster.ctx();
		if (active == null || ctx == null) {
			return false;
		}

		int destX = 82698, destY = 148638, destZ = -3473; // Default Giran
		if (ctx.mapRegions() != null) {
			int[] coords = ctx.mapRegions().getRestartCoordinates(
					active.x(), active.y(), active.z(), active.race(), active.karma() > 0);
			if (coords != null && coords.length >= 3 && (coords[0] != 0 || coords[1] != 0)) {
				destX = coords[0];
				destY = coords[1];
				destZ = coords[2];
			}
		}

		boolean isPartyRecall = "TARGET_PARTY".equalsIgnoreCase(skill.target()) || skill.id() == 1255;
		if (isPartyRecall && caster.party() != null) {
			for (GameSession member : caster.party().members()) {
				if (member != null && canTeleportPlayer(member, member != caster)) {
					double dist = Math.hypot(member.active().x() - active.x(), member.active().y() - active.y());
					if (dist <= Party.getPartyRange()) {
						member.teleportToLocation(destX, destY, destZ, 0);
					}
				}
			}
		} else if (targetPlayer != null && targetPlayer != caster) {
			if (!canTeleportPlayer(targetPlayer, true)) {
				caster.send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED, new SystemMessage.SkillName(skill.id(), skill.level())));
				return true;
			}
			targetPlayer.teleportToLocation(destX, destY, destZ, 0);
		} else {
			if (!canTeleportPlayer(caster, false)) {
				caster.send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED, new SystemMessage.SkillName(skill.id(), skill.level())));
				return true;
			}
			caster.teleportToLocation(destX, destY, destZ, 0);
		}
		return true;
	}

	private boolean handleSummonFriendOrGate(GameSession caster, SkillTemplate skill, GameSession targetPlayer) {
		PlayerCharacter active = caster.active();
		GameSession.Context ctx = caster.ctx();
		if (active == null || ctx == null) {
			return false;
		}
		if (!canTeleportPlayer(caster, false) || isInRestrictedSummonZone(caster) || active.isInSiege()) {
			caster.send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED, new SystemMessage.SkillName(skill.id(), skill.level())));
			return true;
		}

		if (skill.id() == 1429 || "GATE_CHANT".equalsIgnoreCase(skill.skillType())) {
			// Gate Chant (Doomcryer): teleports all eligible party members without consuming crystals
			if (caster.party() == null) {
				caster.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Gate Chant requer estar em uma Party."));
				return true;
			}
			for (GameSession member : caster.party().members()) {
				if (member != null && member != caster && canTeleportPlayer(member, true) && !isInRestrictedSummonZone(member)) {
					member.teleportToLocation(active.x(), active.y(), active.z(), 0);
				}
			}
			return true;
		}

		if (skill.id() == 3632 || "CLAN_GATE".equalsIgnoreCase(skill.skillType())) {
			// Clan Gate: Clan Leader opens a gate and teleports eligible online clan members
			if (active.clanId() <= 0 || !active.clanLeader()) {
				caster.send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED, new SystemMessage.SkillName(skill.id(), skill.level())));
				return true;
			}
			Clan clan = ctx.clans() != null ? ctx.clans().byClanId(active.clanId()).orElse(null) : active.clan();
			if (clan == null || ctx.world() == null) {
				return true;
			}
			active.startAbnormalEffect(0x04000000); // Magic Circle / Clan Gate visual
			caster.sendUserInfoAndBroadcastCharInfo();
			CreatureSay gateNotice = new CreatureSay(0, CreatureSay.CLAN, active.name(),
					"Clan Gate aberto! Membros elegiveis do cla estao sendo invocados.");
			clan.broadcastToOnlineMembers(ctx.world(), gateNotice);
			for (ClanMember cm : clan.members()) {
				if (cm.objectId() == active.objectId()) {
					continue;
				}
				ctx.world().player(cm.objectId()).ifPresent(online -> {
					if (online instanceof GameSession memberSession) {
						if (canTeleportPlayer(memberSession, true)
								&& !memberSession.active().isInCombat()
								&& !isInRestrictedSummonZone(memberSession)) {
							memberSession.teleportToLocation(active.x(), active.y(), active.z(), 0);
						}
					}
				});
			}
			GameSession.autoAttackScheduler().schedule(() -> {
				if (caster.inWorld() && caster.active() == active) {
					active.stopAbnormalEffect(0x04000000);
					caster.sendUserInfoAndBroadcastCharInfo();
				}
			}, 120_000L, java.util.concurrent.TimeUnit.MILLISECONDS);
			return true;
		}

		// 1403 (Summon Friend)
		if (targetPlayer == null || targetPlayer == caster || targetPlayer.active() == null) {
			caster.send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
			return true;
		}
		if (caster.party() == null || !caster.party().members().contains(targetPlayer)) {
			caster.send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
			return true;
		}
		if (!canTeleportPlayer(targetPlayer, true) || isInRestrictedSummonZone(targetPlayer) || targetPlayer.active().isInCombat()) {
			caster.send(new CreatureSay(0, CreatureSay.ALL, "SYS", targetPlayer.active().name() + " nao pode ser invocado no momento."));
			return true;
		}
		boolean consumed = targetPlayer.consumeItem(8615, 1) || caster.consumeItem(8615, 1);
		if (!consumed && !Config.getBoolean("FreeSummonFriend", false)) {
			caster.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
					targetPlayer.active().name() + " precisa de um Summoning Crystal (8615) para ser invocado."));
			targetPlayer.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
					active.name() + " tentou invocar voce, mas voce precisa de um Summoning Crystal (8615)."));
			return true;
		}
		targetPlayer.teleportToLocation(active.x(), active.y(), active.z(), 0);
		return true;
	}

	private boolean handleTakeCastle(GameSession caster, SkillTemplate skill, NpcInstance mainNpc) {
		PlayerCharacter active = caster.active();
		GameSession.Context ctx = caster.ctx();
		if (active == null || ctx == null || ctx.sieges() == null) {
			return false;
		}
		if (active.clanId() <= 0 || !active.clanLeader()) {
			caster.send(new CreatureSay(0, CreatureSay.ALL, "Siege", "Apenas o lider de um cla atacante pode gravar o selo!"));
			return true;
		}
		for (SiegeRecord siege : ctx.sieges().allSieges()) {
			if (siege.status() == SiegeStatus.IN_PROGRESS && ctx.sieges().isAttacker(siege.castleId(), active.clanId())) {
				boolean ok = ctx.sieges().midVictory(siege.castleId(), active.clanId());
				if (ok) {
					caster.send(new CreatureSay(0, CreatureSay.HERO_VOICE, "Siege",
							"O selo foi gravado com sucesso! Seu cla assumiu o controle do Castelo!"));
					if (ctx.world() != null) {
						ctx.world().broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Siege",
								"O cla de " + active.name() + " conquistou o Castelo " + siege.castleId() + " (Mid-Victory)!"));
					}
				}
				return true;
			}
		}
		caster.send(new CreatureSay(0, CreatureSay.ALL, "Siege", "Seu cla nao esta registrado como atacante em um cerco ativo."));
		return true;
	}

	private boolean handleSiegeFlag(GameSession caster, SkillTemplate skill) {
		PlayerCharacter active = caster.active();
		GameSession.Context ctx = caster.ctx();
		if (active == null || ctx == null || ctx.sieges() == null) {
			return false;
		}
		if (active.clanId() <= 0 || !active.clanLeader()) {
			caster.send(new CreatureSay(0, CreatureSay.ALL, "Siege", "Apenas o lider do cla pode construir um Quartel-General (Headquarters)."));
			return true;
		}
		for (SiegeRecord siege : ctx.sieges().allSieges()) {
			if (siege.status() == SiegeStatus.IN_PROGRESS && ctx.sieges().isAttacker(siege.castleId(), active.clanId())) {
				NpcTemplate flagTpl = ctx.npcTemplates() != null
						? ctx.npcTemplates().get(35062).orElse(NpcTemplate.fallback(35062, active.name() + "'s Headquarters", "L2SiegeFlag"))
						: NpcTemplate.fallback(35062, active.name() + "'s Headquarters", "L2SiegeFlag");
				NpcInstance flag = new NpcInstance(
						idFactory.nextId(),
						flagTpl,
						active.x() + 30,
						active.y() + 30,
						active.z(),
						active.heading());
				flag.instanceId(active.instanceId());
				if (ctx.world() != null) {
					ctx.world().addNpc(flag);
					ctx.world().broadcastAround(caster, GameWorld.VISIBILITY_RADIUS, NpcInfo.of(flag), true);
				}
				caster.send(new CreatureSay(0, CreatureSay.ALL, "Siege", "Quartel-General (Siege Headquarters) construido com sucesso!"));
				return true;
			}
		}
		caster.send(new CreatureSay(0, CreatureSay.ALL, "Siege", "Quartel-General so pode ser construido durante um cerco ativo."));
		return true;
	}

	private static int resolveServitorNpcId(int skillId, int skillLevel) {
		return switch (skillId) {
			case 1129 -> 14001; // Summon Reanimated Man
			case 1154 -> 14019; // Summon Corrupted Man
			case 1225 -> 14111; // Summon Mew the Cat
			case 1226 -> 14251; // Summon Unicorn Boxer
			case 1227 -> 14401; // Summon Mirage the Unicorn
			case 1228 -> 14561; // Summon Silhouette
			case 1276 -> 14149; // Summon Kai the Cat
			case 1277 -> 14289; // Summon Merrow the Unicorn
			case 1278 -> 14601; // Summon Soulless
			case 1331 -> 14835; // Summon Queen of Cats
			case 1332 -> 14849; // Summon Unicorn Seraphim
			case 1333 -> 14863; // Summon Nightshade
			case 1334 -> 14038; // Summon Cursed Man
			case 25 -> 14702;   // Summon Mechanic Golem
			case 13 -> 14737;   // Summon Siege Golem
			case 299 -> 14768;  // Summon Wild Hog Cannon
			case 448 -> 14801;  // Summon Swoop Cannon
			default -> 14111;
		};
	}
}
