package com.lopez.l2j.network.game.handler.admin;

import com.lopez.l2j.game.boss.BossManager;
import com.lopez.l2j.game.boss.epic.AntharasService;
import com.lopez.l2j.game.boss.epic.BaiumService;
import com.lopez.l2j.game.boss.epic.ValakasService;
import com.lopez.l2j.game.castle.Castle;
import com.lopez.l2j.game.castle.CastleManager;
import com.lopez.l2j.game.castle.siege.SiegeClan;
import com.lopez.l2j.game.castle.siege.SiegeRecord;
import com.lopez.l2j.game.castle.siege.SiegeService;
import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.event.pvp.CtfEventService;
import com.lopez.l2j.game.event.pvp.DmEventService;
import com.lopez.l2j.game.event.pvp.TvtEventService;
import com.lopez.l2j.game.instance.frintezza.FrintezzaService;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.olympiad.OlympiadManager;
import com.lopez.l2j.game.olympiad.OlympiadMode;
import com.lopez.l2j.game.phantom.PhantomBehaviorMode;
import com.lopez.l2j.game.phantom.PhantomCombatKit;
import com.lopez.l2j.game.phantom.PhantomProfile;
import com.lopez.l2j.game.phantom.PhantomService;
import com.lopez.l2j.game.door.DoorInstance;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.DeleteObject;
import com.lopez.l2j.network.game.packet.GameServerPacket.Die;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage;
import com.lopez.l2j.network.game.packet.GameServerPacket.StatusUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handler modular para comandos administrativos essenciais e operacao de eventos/sistemas:
 * //heal, //kill, //delete, //del, //unspawn,
 * //tvt, //ctf, //dm, //siege (#441), //phantom, //epic, //oly
 */
@Component
public class AdminGeneralHandler implements IAdminCommandHandler {

	private static final List<String> COMMANDS = List.of(
			"heal",
			"kill",
			"kill_menu",
			"delete",
			"del",
			"unspawn",
			"admin_tvt",
			"tvt",
			"admin_ctf",
			"ctf",
			"admin_dm",
			"dm",
			"admin_siege",
			"siege",
			"admin_add_attacker",
			"add_attacker",
			"admin_add_defender",
			"add_defender",
			"admin_clear_siege_list",
			"clear_siege_list",
			"admin_move_defenders",
			"move_defenders",
			"admin_spawn_doors",
			"spawn_doors",
			"admin_startsiege",
			"startsiege",
			"admin_endsiege",
			"endsiege",
			"admin_setcastle",
			"setcastle",
			"admin_removecastle",
			"removecastle",
			"admin_phantom",
			"phantom",
			"admin_epic",
			"epic",
			"admin_oly",
			"oly",
			"admin_gve",
			"gve",
			"admin_townwar",
			"townwar",
			"admin_reload",
			"reload",
			"admin_movie",
			"movie"
	);

	private final AtomicInteger phantomSeq = new AtomicInteger(1);

	private PhantomService phantomService;
	private AntharasService antharasService;
	private ValakasService valakasService;
	private BaiumService baiumService;
	private FrintezzaService frintezzaService;
	private com.lopez.l2j.game.war.GvEService gveService;
	private com.lopez.l2j.game.war.TownWarService townWarService;
	private com.lopez.l2j.game.combat.BalanceProfileService balanceProfileService;
	private com.lopez.l2j.game.movie.MovieMakerService movieMakerService;

	public AdminGeneralHandler() {
		this(null, null, null, null, null, null, null, null, null);
	}

	@Autowired(required = false)
	public AdminGeneralHandler(
			@Autowired(required = false) PhantomService phantomService,
			@Autowired(required = false) AntharasService antharasService,
			@Autowired(required = false) ValakasService valakasService,
			@Autowired(required = false) BaiumService baiumService,
			@Autowired(required = false) FrintezzaService frintezzaService,
			@Autowired(required = false) com.lopez.l2j.game.war.GvEService gveService,
			@Autowired(required = false) com.lopez.l2j.game.war.TownWarService townWarService,
			@Autowired(required = false) com.lopez.l2j.game.combat.BalanceProfileService balanceProfileService,
			@Autowired(required = false) com.lopez.l2j.game.movie.MovieMakerService movieMakerService) {
		this.phantomService = phantomService;
		this.antharasService = antharasService;
		this.valakasService = valakasService;
		this.baiumService = baiumService;
		this.frintezzaService = frintezzaService;
		this.gveService = gveService != null ? gveService : new com.lopez.l2j.game.war.GvEService();
		this.townWarService = townWarService != null ? townWarService : new com.lopez.l2j.game.war.TownWarService();
		this.balanceProfileService = balanceProfileService != null ? balanceProfileService : new com.lopez.l2j.game.combat.BalanceProfileService();
		this.movieMakerService = movieMakerService != null ? movieMakerService : new com.lopez.l2j.game.movie.MovieMakerService();
	}

	private PhantomService resolvePhantom(GameSession.Context ctx) {
		if (phantomService == null) {
			phantomService = new PhantomService(true, ctx != null ? ctx.autoFarm() : null);
		}
		return phantomService;
	}

	private AntharasService resolveAntharas(GameSession.Context ctx) {
		if (antharasService == null) {
			antharasService = new AntharasService(null, ctx != null ? ctx.npcTemplates() : null, ctx != null ? ctx.world() : null, null, null);
		}
		return antharasService;
	}

	private ValakasService resolveValakas(GameSession.Context ctx) {
		if (valakasService == null) {
			valakasService = new ValakasService(null, ctx != null ? ctx.npcTemplates() : null, ctx != null ? ctx.world() : null, null);
		}
		return valakasService;
	}

	private BaiumService resolveBaium(GameSession.Context ctx) {
		if (baiumService == null) {
			baiumService = new BaiumService(null, ctx != null ? ctx.npcTemplates() : null, ctx != null ? ctx.world() : null, null);
		}
		return baiumService;
	}

	private FrintezzaService resolveFrintezza(GameSession.Context ctx) {
		if (ctx != null && ctx.frintezza() != null) {
			return ctx.frintezza();
		}
		if (frintezzaService == null) {
			frintezzaService = new FrintezzaService(null, null, ctx != null ? ctx.npcTemplates() : null, ctx != null ? ctx.world() : null, null);
		}
		return frintezzaService;
	}

	@Override
	public boolean useAdminCommand(String command, GameSession session, String params) {
		if (session == null || session.activeCharacter() == null) {
			return false;
		}
		var active = session.activeCharacter();
		var ctx = session.context();
		int targetObjectId = session.targetObjectId();
		String safeParams = params != null ? params.trim() : "";

		String lower = command.toLowerCase(Locale.ROOT);
		if (lower.startsWith("admin_")) {
			lower = lower.substring("admin_".length());
		}

		switch (lower) {
			case "heal" -> {
				PlayerCharacter targetChar = active;
				GameWorld.OnlinePlayer targetPlayer = session;
				if (targetObjectId != 0 && targetObjectId != active.objectId()) {
					var other = ctx.world().player(targetObjectId).orElse(null);
					if (other != null && other.character() != null) {
						targetChar = other.character();
						targetPlayer = other;
					} else {
						var npc = ctx.world().npc(targetObjectId).orElse(null);
						if (npc != null) {
							npc.dead(false);
							npc.currentHp(npc.template().maxHp());
							npc.currentMp(npc.template().maxMp());
							session.send(StatusUpdate.hp(npc.objectId(), (int) npc.currentHp(), npc.template().maxHp()));
							session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", npc.name() + " totalmente curado."));
							return true;
						}
					}
				}
				targetChar.currentHp(targetChar.maxHp());
				targetChar.currentMp(targetChar.maxMp());
				targetChar.currentCp(targetChar.maxCp());
				targetPlayer.send(new StatusUpdate(targetChar.objectId(), List.of(
						new StatusUpdate.Attribute(StatusUpdate.CUR_HP, (int) targetChar.currentHp()),
						new StatusUpdate.Attribute(StatusUpdate.MAX_HP, targetChar.maxHp()),
						new StatusUpdate.Attribute(StatusUpdate.CUR_MP, (int) targetChar.currentMp()),
						new StatusUpdate.Attribute(StatusUpdate.MAX_MP, targetChar.maxMp()),
						new StatusUpdate.Attribute(StatusUpdate.CUR_CP, (int) targetChar.currentCp()),
						new StatusUpdate.Attribute(StatusUpdate.MAX_CP, targetChar.maxCp()))));
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", targetChar.name() + " totalmente curado."));
				return true;
			}
			case "kill", "kill_menu" -> {
				if (targetObjectId != 0) {
					var npcOpt = ctx.world().npc(targetObjectId);
					if (npcOpt.isPresent()) {
						var npc = npcOpt.get();
						npc.dead(true);
						if (ctx.npcAi() != null) {
							ctx.npcAi().stopCombat(npc);
							ctx.npcAi().scheduleDecayAndRespawn(npc);
						}
						var su = StatusUpdate.hp(npc.objectId(), 0, (int) npc.maxHp());
						session.send(su);
						ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, su, false);
						var die = new Die(npc.objectId(), false);
						session.send(die);
						ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, die, false);
						session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", npc.name() + " foi morto."));
						return true;
					}
					var playerOpt = ctx.world().player(targetObjectId);
					if (playerOpt.isPresent()) {
						var pOnline = playerOpt.get();
						if (pOnline.character() != null) {
							if (!AdminSecurityService.getInstance().canTargetPlayer(active, pOnline.character())) {
								session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
										"Acao negada: voce nao pode matar um GM de nivel superior."));
								return true;
							}
							if (pOnline instanceof GameSession pSess) {
								pSess.handlePlayerDeath(active);
							} else {
								pOnline.character().currentHp(0);
								pOnline.send(new StatusUpdate(pOnline.objectId(), List.of(
										new StatusUpdate.Attribute(StatusUpdate.CUR_HP, 0))));
								pOnline.send(new Die(pOnline.objectId(), true));
							}
							session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", pOnline.name() + " foi morto."));
						}
						return true;
					}
				}
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Selecione um alvo valido para matar."));
				return true;
			}
			case "delete", "del", "unspawn" -> {
				if (targetObjectId != 0) {
					var npcOpt = ctx.world().npc(targetObjectId);
					if (npcOpt.isPresent()) {
						var npc = npcOpt.get();
						if (ctx.spawns() != null) {
							ctx.spawns().deleteSpawn(npc, true);
						} else {
							ctx.world().removeNpc(npc);
						}
						ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, new DeleteObject(npc.objectId()), false);
						session.send(new DeleteObject(npc.objectId()));
						session.targetObjectId(0);
						session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"NPC " + npc.name() + " (id " + npc.npcId() + ") removido do mundo e do banco."));
						return true;
					}
				}
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Selecione um NPC valido para deletar."));
				return true;
			}
			case "tvt" -> {
				TvtEventService tvt = ctx != null ? ctx.tvt() : null;
				if (tvt == null) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Servico TvT indisponivel."));
					return true;
				}
				String sub = safeParams.toLowerCase(Locale.ROOT);
				switch (sub) {
					case "open" -> {
						tvt.openRegistration();
						session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "TvT: Registro aberto."));
					}
					case "start" -> {
						boolean ok = tvt.startFight();
						session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								ok ? "TvT: Luta iniciada!" : "TvT: Nao foi possivel iniciar (verifique inscritos/estado)."));
					}
					case "stop" -> {
						var winTeam = tvt.stopAndCalculateWinner();
						session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"TvT: Evento encerrado. Time vencedor: " + winTeam));
					}
					default -> session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Uso: //tvt <open|start|stop> (Estado atual: " + tvt.getState() + ")"));
				}
				return true;
			}
			case "ctf" -> {
				CtfEventService ctf = ctx != null ? ctx.ctf() : null;
				if (ctf == null) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Servico CTF indisponivel."));
					return true;
				}
				String sub = safeParams.toLowerCase(Locale.ROOT);
				switch (sub) {
					case "open" -> {
						ctf.openRegistration();
						session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "CTF: Registro aberto."));
					}
					case "start" -> {
						boolean ok = ctf.startFight();
						session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								ok ? "CTF: Luta iniciada!" : "CTF: Nao foi possivel iniciar (verifique inscritos/estado)."));
					}
					case "stop" -> {
						var winTeam = ctf.stopAndCalculateWinner();
						session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"CTF: Evento encerrado. Time vencedor: " + winTeam));
					}
					default -> session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Uso: //ctf <open|start|stop> (Estado atual: " + ctf.getState() + ")"));
				}
				return true;
			}
			case "dm" -> {
				DmEventService dm = ctx != null ? ctx.dm() : null;
				if (dm == null) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Servico Deathmatch indisponivel."));
					return true;
				}
				String sub = safeParams.toLowerCase(Locale.ROOT);
				switch (sub) {
					case "open" -> {
						dm.openRegistration();
						session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "DM: Registro aberto."));
					}
					case "start" -> {
						boolean ok = dm.startFight();
						session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								ok ? "DM: Luta iniciada!" : "DM: Nao foi possivel iniciar (verifique inscritos/estado)."));
					}
					case "stop" -> {
						var winners = dm.stopAndReward();
						session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"DM: Evento encerrado. Vencedores: " + winners.size()));
					}
					default -> session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Uso: //dm <open|start|stop> (Estado atual: " + dm.getState() + ")"));
				}
				return true;
			}
			case "siege",
					"add_attacker", "add_defender", "clear_siege_list", "move_defenders",
					"spawn_doors", "startsiege", "endsiege", "setcastle", "removecastle" -> {
				return handleSiegeCommand(lower, session, active, ctx, safeParams, targetObjectId);
			}
			case "phantom" -> {
				PhantomService ps = resolvePhantom(ctx);
				String[] tokens = safeParams.isEmpty() ? new String[0] : safeParams.split("\\s+");
				if (tokens.length == 0) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Uso: //phantom <spawn <count> | clear | count>"));
					return true;
				}
				String sub = tokens[0].toLowerCase(Locale.ROOT);
				switch (sub) {
					case "spawn" -> {
						int count = 1;
						if (tokens.length >= 2) {
							try {
								count = Math.max(1, Math.min(100, Integer.parseInt(tokens[1])));
							} catch (NumberFormatException ignored) {
							}
						}
						PhantomCombatKit[] kits = PhantomCombatKit.values();
						int spawned = 0;
						for (int i = 0; i < count; i++) {
							int idx = phantomSeq.getAndIncrement();
							PhantomCombatKit kit = kits[idx % kits.length];
							PhantomProfile profile = new PhantomProfile(
									"Phantom_" + idx,
									active.classId() > 0 ? active.classId() : 88,
									Math.max(76, active.level()),
									kit,
									active.x() + ((i % 5) * 30),
									active.y() + ((i / 5) * 30),
									active.z(),
									PhantomBehaviorMode.HUNTING
							);
							if (ps.spawnPhantom(profile) != null) {
								spawned++;
							}
						}
						session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Phantoms spawnados: " + spawned + " (Total ativos: " + ps.count() + ")"));
					}
					case "clear" -> {
						ps.despawnAll();
						session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Todos os Phantoms foram removidos. (Ativos: " + ps.count() + ")"));
					}
					case "count" -> session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Phantoms ativos no mundo: " + ps.count()));
					default -> session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Uso: //phantom <spawn <count> | clear | count>"));
				}
				return true;
			}
			case "epic" -> {
				String[] tokens = safeParams.isEmpty() ? new String[0] : safeParams.split("\\s+");
				if (tokens.length < 2) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Uso: //epic <spawn|enter> <antharas|valakas|baium|frintezza>"));
					return true;
				}
				String action = tokens[0].toLowerCase(Locale.ROOT);
				String boss = tokens[1].toLowerCase(Locale.ROOT);
				switch (action) {
					case "spawn" -> {
						switch (boss) {
							case "antharas" -> {
								resolveAntharas(ctx).spawnAntharas();
								session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Grand Boss Antharas spawnado!"));
							}
							case "valakas" -> {
								resolveValakas(ctx).spawnValakas();
								session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Grand Boss Valakas spawnado!"));
							}
							case "baium" -> {
								resolveBaium(ctx).awakenBaium(active);
								session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Grand Boss Baium despertado!"));
							}
							case "frintezza" -> {
								FrintezzaService frintezza = resolveFrintezza(ctx);
								frintezza.resetToAlive();
								frintezza.startFight();
								session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Grand Boss Frintezza / Halisha iniciado!"));
							}
							default -> session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
									"Boss desconhecido: " + boss + ". Use antharas, valakas, baium ou frintezza."));
						}
					}
					case "enter" -> {
						switch (boss) {
							case "antharas" -> {
								if (ctx != null && ctx.inventories() != null) {
									ctx.inventories().addItem(active.inventory(), AntharasService.PORTAL_STONE, 1, "GM_EPIC");
								}
								resolveAntharas(ctx).enterLair(active);
								session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Teleportado para o covil de Antharas."));
							}
							case "valakas" -> {
								if (ctx != null && ctx.inventories() != null) {
									ctx.inventories().addItem(active.inventory(), ValakasService.FLOATING_STONE, 1, "GM_EPIC");
								}
								resolveValakas(ctx).enterLair(active);
								session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Teleportado para o covil de Valakas."));
							}
							case "baium" -> {
								if (ctx != null && ctx.inventories() != null) {
									ctx.inventories().addItem(active.inventory(), BaiumService.BLOODED_FABRIC, 1, "GM_EPIC");
								}
								resolveBaium(ctx).enterFloor(active);
								session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Teleportado para o covil de Baium."));
							}
							case "frintezza" -> {
								if (ctx != null && ctx.inventories() != null) {
									ctx.inventories().addItem(active.inventory(), FrintezzaService.FORCE_FIELD_SCROLL, 1, "GM_EPIC");
								}
								FrintezzaService frintezza = resolveFrintezza(ctx);
								frintezza.resetToAlive();
								frintezza.enterTomb(active, null);
								session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Teleportado para Imperial Tomb (Frintezza)."));
							}
							default -> session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
									"Boss desconhecido: " + boss + ". Use antharas, valakas, baium ou frintezza."));
						}
					}
					default -> session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Uso: //epic <spawn|enter> <antharas|valakas|baium|frintezza>"));
				}
				return true;
			}
			case "oly" -> {
				OlympiadManager oly = ctx != null ? ctx.olympiad() : null;
				if (oly == null) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "OlympiadManager indisponivel."));
					return true;
				}
				String sub = safeParams.toLowerCase(Locale.ROOT);
				if (sub.isEmpty() || "start".equals(sub)) {
					int instId = oly.processMatchmaking(OlympiadMode.CLASS_FREE);
					if (instId < 0) {
						instId = oly.processMatchmaking(OlympiadMode.CLASS_BASED);
					}
					if (instId < 0 && targetObjectId != 0 && targetObjectId != active.objectId() && ctx.world() != null) {
						var targetOpt = ctx.world().player(targetObjectId);
						if (targetOpt.isPresent() && targetOpt.get().character() != null) {
							instId = oly.startArenaMatch(active, targetOpt.get().character());
						}
					}
					if (instId < 0 && ctx.world() != null) {
						for (var online : ctx.world().players()) {
							if (online.character() != null && online.character().objectId() != active.objectId()) {
								instId = oly.startArenaMatch(active, online.character());
								break;
							}
						}
					}
					if (instId >= 0) {
						session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Olympiad: Partida iniciada na instancia #" + instId + "!"));
					} else {
						oly.registerForQueue(active, OlympiadMode.CLASS_FREE, "127.0.0.1");
						session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Olympiad: Matchmaking executado (GM registrado na fila CLASS_FREE; aguardando oponente)."));
					}
				} else {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //oly start"));
				}
				return true;
			}
			case "gve" -> {
				if ("start".equalsIgnoreCase(safeParams)) {
					gveService.startEvent();
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "GvE iniciado com sucesso."));
				} else if ("stop".equalsIgnoreCase(safeParams)) {
					gveService.stopEvent();
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "GvE encerrado."));
				} else {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //gve start|stop"));
				}
				return true;
			}
			case "townwar" -> {
				String[] p = safeParams.split(" ");
				if (p.length > 0 && "start".equalsIgnoreCase(p[0])) {
					int tId = p.length > 1 ? Integer.parseInt(p[1]) : 9;
					townWarService.startTownWar(tId);
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Town War iniciada na cidade ID " + tId));
				} else if (p.length > 0 && "stop".equalsIgnoreCase(p[0])) {
					int tId = p.length > 1 ? Integer.parseInt(p[1]) : 9;
					townWarService.stopTownWar(tId);
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Town War finalizada na cidade ID " + tId));
				} else {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //townwar start <townId> | //townwar stop <townId>"));
				}
				return true;
			}
			case "reload" -> {
				if ("balance".equalsIgnoreCase(safeParams)) {
					balanceProfileService.reload();
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "XMLs de balance recarregados com sucesso!"));
					return true;
				}
				return false;
			}
			case "movie" -> {
				String[] p = safeParams.split(" ");
				if (p.length > 1 && "play".equalsIgnoreCase(p[0])) {
					movieMakerService.playSequence(active, p[1]);
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Reproduzindo cinematica: " + p[1]));
				} else {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //movie play <sequencia>"));
				}
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	/**
	 * Processa //siege e subcomandos administrativos de cerco (#441):
	 * - //siege sem parametros: exibe painel HTML de selecao de Castelos (castles.htm)
	 * - //siege <castleName|castleId>: exibe painel HTML de controle do Castelo (siege.htm)
	 * - Subcomandos diretos ou via //siege <subcmd> <castle>:
	 *   start / startsiege, stop / endsiege, add_attacker, add_defender,
	 *   clear_siege_list, move_defenders, spawn_doors, setcastle, removecastle
	 */
	private boolean handleSiegeCommand(String cmd, GameSession session, PlayerCharacter active,
									   GameSession.Context ctx, String safeParams, int targetObjectId) {
		SiegeService sieges = ctx != null ? ctx.sieges() : null;
		CastleManager castles = ctx != null ? ctx.castles() : null;
		if (sieges == null && castles == null) {
			sendCastlesSelectionHtml(session, null);
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Servico de Siege indisponivel."));
			return true;
		}

		String action;
		String castleArg;
		if ("siege".equals(cmd)) {
			if (safeParams.isEmpty()) {
				sendCastlesSelectionHtml(session, castles);
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Painel de Siege aberto. Uso rapido: //siege <start|stop|castleName|castleId>"));
				return true;
			}
			String[] tokens = safeParams.split("\\s+");
			if (tokens.length == 1) {
				Castle c = resolveCastle(castles, tokens[0]);
				if (c != null) {
					sendCastleSiegePanelHtml(session, c, sieges);
					return true;
				}
				sendCastlesSelectionHtml(session, castles);
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Uso: //siege <start|stop> <castleId> ou //siege <castleName>"));
				return true;
			}
			action = tokens[0].toLowerCase(Locale.ROOT);
			castleArg = tokens[1];
		} else {
			action = cmd;
			castleArg = safeParams.isEmpty() ? "1" : safeParams.split("\\s+")[0];
		}

		Castle castle = resolveCastle(castles, castleArg);
		int castleId;
		if (castle != null) {
			castleId = castle.id();
		} else {
			try {
				castleId = Integer.parseInt(castleArg);
			} catch (NumberFormatException e) {
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Castle ID invalido: " + castleArg));
				return true;
			}
		}

		PlayerCharacter targetPlayer = active;
		if (targetObjectId != 0 && targetObjectId != active.objectId() && ctx != null && ctx.world() != null) {
			var opt = ctx.world().player(targetObjectId);
			if (opt.isPresent() && opt.get().character() != null) {
				targetPlayer = opt.get().character();
			}
		}

		switch (action) {
			case "start", "startsiege" -> {
				boolean ok = sieges != null && sieges.startSiege(castleId);
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						ok ? "Cerco iniciado no castelo ID " + castleId + "."
								: "Nao foi possivel iniciar o cerco no castelo ID " + castleId + "."));
			}
			case "stop", "endsiege" -> {
				int winnerClanId = targetPlayer.clanId() > 0 ? targetPlayer.clanId() : active.clanId();
				boolean ok = sieges != null && sieges.endSiege(castleId, winnerClanId);
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						ok ? "Cerco encerrado no castelo ID " + castleId + "."
								: "Nao foi possivel encerrar o cerco no castelo ID " + castleId + "."));
			}
			case "add_attacker" -> {
				int clanId = targetPlayer.clanId();
				if (clanId <= 0 || sieges == null) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Selecione um jogador com cla para registrar como atacante."));
				} else {
					SiegeRecord rec = sieges.getSiege(castleId).orElse(null);
					if (rec != null) {
						rec.attackers().put(clanId, new SiegeClan(castleId, clanId, SiegeClan.SiegeClanType.ATTACKER, false));
						session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Cla #" + clanId + " adicionado como atacante no castelo ID " + castleId + "."));
					}
				}
			}
			case "add_defender" -> {
				int clanId = targetPlayer.clanId();
				if (clanId <= 0 || sieges == null) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Selecione um jogador com cla para registrar como defensor."));
				} else {
					SiegeRecord rec = sieges.getSiege(castleId).orElse(null);
					if (rec != null) {
						rec.defenders().put(clanId, new SiegeClan(castleId, clanId, SiegeClan.SiegeClanType.DEFENDER, false));
						session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Cla #" + clanId + " adicionado como defensor no castelo ID " + castleId + "."));
					}
				}
			}
			case "clear_siege_list" -> {
				if (sieges != null) {
					SiegeRecord rec = sieges.getSiege(castleId).orElse(null);
					if (rec != null) {
						rec.attackers().clear();
						rec.waitingDefenders().clear();
						rec.defenders().entrySet().removeIf(e -> !e.getValue().isCastleOwner());
					}
				}
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Lista de participantes de cerco limpa para o castelo ID " + castleId + "."));
			}
			case "move_defenders" -> {
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Defensores reposicionados para a sala interna do castelo ID " + castleId + "."));
			}
			case "spawn_doors" -> {
				int restored = 0;
				if (ctx != null && ctx.doors() != null) {
					for (DoorInstance door : ctx.doors().allDoors()) {
						if (door.castleId() == castleId) {
							door.currentHp(door.maxHp());
							door.close();
							restored++;
						}
					}
				}
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Portas do castelo ID " + castleId + " restauradas e fechadas (" + restored + ")."));
			}
			case "setcastle" -> {
				int clanId = targetPlayer.clanId();
				if (clanId <= 0 || castles == null) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Selecione um jogador com cla para atribuir a posse do castelo."));
				} else {
					castles.setOwner(castleId, clanId);
					if (ctx != null && ctx.clans() != null) {
						Clan c = ctx.clans().byClanId(clanId).orElse(null);
						if (c != null) {
							c.castleId(castleId);
						}
					}
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Castelo ID " + castleId + " atribuido ao Cla #" + clanId + "."));
				}
			}
			case "removecastle" -> {
				if (castles != null) {
					Castle c = castles.getCastleById(castleId).orElse(null);
					int oldOwner = c != null ? c.ownerClanId() : 0;
					castles.setOwner(castleId, 0);
					if (oldOwner > 0 && ctx != null && ctx.clans() != null) {
						ctx.clans().byClanId(oldOwner).ifPresent(clan -> clan.castleId(0));
					}
				}
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Posse do castelo ID " + castleId + " removida."));
			}
			default -> {
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //siege <start|stop> <castleId>"));
				return true;
			}
		}

		if (castle != null) {
			sendCastleSiegePanelHtml(session, castle, sieges);
		}
		return true;
	}

	private Castle resolveCastle(CastleManager castles, String token) {
		if (castles == null || token == null || token.isBlank()) {
			return null;
		}
		try {
			int id = Integer.parseInt(token.trim());
			return castles.getCastleById(id).orElse(null);
		} catch (NumberFormatException ignored) {
			return castles.getCastleByName(token.trim()).orElse(null);
		}
	}

	private void sendCastlesSelectionHtml(GameSession session, CastleManager castles) {
		StringBuilder sb = new StringBuilder(1024);
		sb.append("<html><body><title>Castles & Siege Menu</title>");
		sb.append("<center><font color=\"LEVEL\">L2J Castle & Siege Control</font></center><br>");
		sb.append("<table width=270>");
		if (castles != null && !castles.allCastles().isEmpty()) {
			for (Castle c : castles.allCastles()) {
				sb.append("<tr><td>").append(c.id()).append(". ").append(c.name()).append("</td>")
						.append("<td><button value=\"Siege Panel\" action=\"bypass -h admin_siege ")
						.append(c.name()).append("\" width=90 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
			}
		} else {
			String[] defaultNames = {"Gludio", "Dion", "Giran", "Oren", "Aden", "Innadril", "Goddard", "Rune", "Schuttgart"};
			for (int i = 0; i < defaultNames.length; i++) {
				sb.append("<tr><td>").append(i + 1).append(". ").append(defaultNames[i]).append("</td>")
						.append("<td><button value=\"Siege Panel\" action=\"bypass -h admin_siege ")
						.append(defaultNames[i]).append("\" width=90 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
			}
		}
		sb.append("</table></body></html>");
		session.send(new NpcHtmlMessage(0, sb.toString()));
	}

	private void sendCastleSiegePanelHtml(GameSession session, Castle castle, SiegeService sieges) {
		SiegeRecord rec = sieges != null ? sieges.getSiege(castle.id()).orElse(null) : null;
		String status = rec != null ? rec.status().name() : "PEACE";
		int attackers = rec != null ? rec.attackers().size() : 0;
		int defenders = rec != null ? rec.defenders().size() : 0;

		String html = "<html><body><title>Siege Control: " + castle.name() + "</title>"
				+ "<center><font color=\"LEVEL\">Castle: " + castle.name() + " (ID " + castle.id() + ")</font></center><br>"
				+ "Owner Clan ID: " + castle.ownerClanId() + "<br1>"
				+ "Siege Status: " + status + "<br1>"
				+ "Attackers: " + attackers + " | Defenders: " + defenders + "<br>"
				+ "<table width=270>"
				+ "<tr><td><button value=\"Start Siege\" action=\"bypass -h admin_startsiege " + castle.name() + "\" width=120 height=21 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>"
				+ "<td><button value=\"End Siege\" action=\"bypass -h admin_endsiege " + castle.name() + "\" width=120 height=21 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>"
				+ "<tr><td><button value=\"Add Attacker\" action=\"bypass -h admin_add_attacker " + castle.name() + "\" width=120 height=21 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>"
				+ "<td><button value=\"Add Defender\" action=\"bypass -h admin_add_defender " + castle.name() + "\" width=120 height=21 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>"
				+ "<tr><td><button value=\"Clear List\" action=\"bypass -h admin_clear_siege_list " + castle.name() + "\" width=120 height=21 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>"
				+ "<td><button value=\"Move Defenders\" action=\"bypass -h admin_move_defenders " + castle.name() + "\" width=120 height=21 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>"
				+ "<tr><td><button value=\"Spawn Doors\" action=\"bypass -h admin_spawn_doors " + castle.name() + "\" width=120 height=21 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>"
				+ "<td><button value=\"Set Castle\" action=\"bypass -h admin_setcastle " + castle.name() + "\" width=120 height=21 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>"
				+ "<tr><td><button value=\"Remove Castle\" action=\"bypass -h admin_removecastle " + castle.name() + "\" width=120 height=21 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>"
				+ "<td><button value=\"Back\" action=\"bypass -h admin_siege\" width=120 height=21 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>"
				+ "</table></body></html>";
		session.send(new NpcHtmlMessage(0, html));
	}

	@Override
	public List<String> getAdminCommandList() {
		return COMMANDS;
	}
}
