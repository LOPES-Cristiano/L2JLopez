package com.lopez.l2j.network.game.handler.admin;

import com.lopez.l2j.game.boss.BossManager;
import com.lopez.l2j.game.boss.epic.AntharasService;
import com.lopez.l2j.game.boss.epic.BaiumService;
import com.lopez.l2j.game.boss.epic.ValakasService;
import com.lopez.l2j.game.castle.siege.SiegeService;
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
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.DeleteObject;
import com.lopez.l2j.network.game.packet.GameServerPacket.Die;
import com.lopez.l2j.network.game.packet.GameServerPacket.StatusUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handler modular para comandos administrativos essenciais e operacao de eventos/sistemas:
 * //heal, //kill, //delete, //del, //unspawn,
 * //tvt, //ctf, //dm, //siege, //phantom, //epic, //oly
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
			"admin_phantom",
			"phantom",
			"admin_epic",
			"epic",
			"admin_oly",
			"oly"
	);

	private final AtomicInteger phantomSeq = new AtomicInteger(1);

	private PhantomService phantomService;
	private AntharasService antharasService;
	private ValakasService valakasService;
	private BaiumService baiumService;
	private FrintezzaService frintezzaService;

	public AdminGeneralHandler() {
		this(null, null, null, null, null);
	}

	@Autowired(required = false)
	public AdminGeneralHandler(
			@Autowired(required = false) PhantomService phantomService,
			@Autowired(required = false) AntharasService antharasService,
			@Autowired(required = false) ValakasService valakasService,
			@Autowired(required = false) BaiumService baiumService,
			@Autowired(required = false) FrintezzaService frintezzaService) {
		this.phantomService = phantomService;
		this.antharasService = antharasService;
		this.valakasService = valakasService;
		this.baiumService = baiumService;
		this.frintezzaService = frintezzaService;
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
			case "siege" -> {
				SiegeService sieges = ctx != null ? ctx.sieges() : null;
				if (sieges == null) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Servico de Siege indisponivel."));
					return true;
				}
				String[] tokens = safeParams.split("\\s+");
				if (tokens.length < 2) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //siege <start|stop> <castleId>"));
					return true;
				}
				String action = tokens[0].toLowerCase(Locale.ROOT);
				int castleId;
				try {
					castleId = Integer.parseInt(tokens[1]);
				} catch (NumberFormatException e) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Castle ID invalido: " + tokens[1]));
					return true;
				}
				if ("start".equals(action)) {
					boolean ok = sieges.startSiege(castleId);
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							ok ? "Cerco iniciado no castelo ID " + castleId + "."
									: "Nao foi possivel iniciar o cerco no castelo ID " + castleId + "."));
				} else if ("stop".equals(action)) {
					int winnerClanId = active.clanId();
					boolean ok = sieges.endSiege(castleId, winnerClanId);
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							ok ? "Cerco encerrado no castelo ID " + castleId + "."
									: "Nao foi possivel encerrar o cerco no castelo ID " + castleId + "."));
				} else {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //siege <start|stop> <castleId>"));
				}
				return true;
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
			default -> {
				return false;
			}
		}
	}

	@Override
	public List<String> getAdminCommandList() {
		return COMMANDS;
	}
}
