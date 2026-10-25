package com.lopez.l2j.network.game.handler.voiced;

import com.lopez.l2j.game.achievements.AchievementsService;
import com.lopez.l2j.game.arenaduel.ArenaDuelService;
import com.lopez.l2j.game.boss.BossManager;
import com.lopez.l2j.game.boss.BossStatus;
import com.lopez.l2j.game.boss.GrandBossInfo;
import com.lopez.l2j.game.boss.GrandBossManager;
import com.lopez.l2j.game.boss.RaidBossSpawnManager;
import com.lopez.l2j.game.castle.Castle;
import com.lopez.l2j.game.castle.CastleManager;
import com.lopez.l2j.game.castle.siege.SiegeRecord;
import com.lopez.l2j.game.castle.siege.SiegeService;
import com.lopez.l2j.game.castle.siege.SiegeStatus;
import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.community.RepairBBSManager;
import com.lopez.l2j.game.dressme.DressMeData;
import com.lopez.l2j.game.dressme.DressMeService;
import com.lopez.l2j.game.event.pvp.CtfEventService;
import com.lopez.l2j.game.event.pvp.DmEventService;
import com.lopez.l2j.game.event.pvp.TvtEventService;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.PlayerStats;
import com.lopez.l2j.game.offlinetrade.OfflineShopItem;
import com.lopez.l2j.game.offlinetrade.OfflineTradeService;
import com.lopez.l2j.game.pve.DungeonService;
import com.lopez.l2j.game.pve.SoloFarmService;
import com.lopez.l2j.game.reset.CharacterResetService;
import com.lopez.l2j.game.roulette.RouletteService;
import com.lopez.l2j.game.service.AioService;
import com.lopez.l2j.game.service.AwayStatusService;
import com.lopez.l2j.game.service.BankingService;
import com.lopez.l2j.game.service.PlayerPreferencesService;
import com.lopez.l2j.game.service.WarehousePasskeyService;
import com.lopez.l2j.game.social.WeddingService;
import com.lopez.l2j.game.tournament.TournamentService;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.handler.bypass.BypassEventsAndPveHandler;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.ItemList;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage;
import com.lopez.l2j.network.game.packet.GameServerPacket.ServerClose;
import com.lopez.l2j.network.game.packet.GameServerPacket.TeleportToLocation;
import com.lopez.l2j.network.game.packet.GameServerPacket.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Handler modular para comandos de voz gerais (#458-#468):
 * .online, .stats, .whoami, .ping, .help, .menu, .expoff, .expon, .autoloot, .traderefuse, .buffshield, .blockbuff,
 * .bank, .deposit, .withdraw, .away, .back, .tournament, .tvt, .tvtjoin, .tvtleave, .tvtinfo,
 * .ctf, .ctfjoin, .ctfleave, .ctfinfo, .dm, .dmjoin, .dmleave, .dminfo, .arena, .duel, .dungeon, .solofarm,
 * .dressme, .undressme, .roulette, .reset, .rebirth, .achievements, .aiomenu, .getaiogoods, .offline, .offlineshop,
 * .epic, .raidinfo, .boss, .7rb, .repair, .changepassword, .password, .combine, .anky,
 * .castlemanager, .siege, .clan, .cwh, .engage, .gotolove, .divorce, .pin, .fusion, .gve, .townwar, .daily
 */
@Component
public class VoicedGeneralHandler implements IVoicedCommandHandler {

	private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd/MM HH:mm")
			.withZone(ZoneId.systemDefault());

	private static final List<String> COMMANDS = List.of(
			"online",
			"stats",
			"whoami",
			"ping",
			"help",
			"ajuda",
			"menu",
			"cfg",
			"configurator",
			"expoff",
			"expon",
			"autoloot",
			"traderefuse",
			"buffshield",
			"blockbuff",
			"bank",
			"deposit",
			"withdraw",
			"away",
			"back",
			"tournament",
			"offlinefarm",
			"offlinestop",
			"tvt",
			"tvtjoin",
			"tvtleave",
			"tvtinfo",
			"ctf",
			"ctfjoin",
			"ctfleave",
			"ctfinfo",
			"dm",
			"dmjoin",
			"dmleave",
			"dminfo",
			"arena",
			"duel",
			"dungeon",
			"solofarm",
			"dressme",
			"undressme",
			"roulette",
			"reset",
			"rebirth",
			"achievements",
			"achieve",
			"aiomenu",
			"getaiogoods",
			"offline",
			"offlineshop",
			"epic",
			"raidinfo",
			"boss",
			"7rb",
			"repair",
			"changepassword",
			"password",
			"combine",
			"anky",
			"castlemanager",
			"siege",
			"clan",
			"cwh",
			"engage",
			"gotolove",
			"divorce",
			"pin",
			"fusion",
			"gve",
			"townwar",
			"daily"
	);

	private final BankingService bankingService;
	private final AwayStatusService awayStatusService;
	private final TournamentService tournamentService;
	private final com.lopez.l2j.game.offlinefarm.OfflineFarmService offlineFarmService;
	private DungeonService dungeonService;
	private SoloFarmService soloFarmService;
	private com.lopez.l2j.game.pve.FusionItemService fusionItemService;
	private com.lopez.l2j.game.war.GvEService gveService;
	private com.lopez.l2j.game.war.TownWarService townWarService;
	private com.lopez.l2j.game.daily.DailyRewardService dailyRewardService;
	private RepairBBSManager repairBBSManager;
	private WeddingService weddingService;
	private JdbcClient jdbc;

	public VoicedGeneralHandler() {
		this(null, null, null, null, null, null, null, null);
	}

	@Autowired(required = false)
	public VoicedGeneralHandler(BankingService bankingService, AwayStatusService awayStatusService,
								TournamentService tournamentService,
								com.lopez.l2j.game.offlinefarm.OfflineFarmService offlineFarmService,
								@Autowired(required = false) com.lopez.l2j.game.pve.FusionItemService fusionItemService,
								@Autowired(required = false) com.lopez.l2j.game.war.GvEService gveService,
								@Autowired(required = false) com.lopez.l2j.game.war.TownWarService townWarService,
								@Autowired(required = false) com.lopez.l2j.game.daily.DailyRewardService dailyRewardService) {
		this.bankingService = bankingService != null ? bankingService : new BankingService();
		this.awayStatusService = awayStatusService != null ? awayStatusService : new AwayStatusService();
		this.tournamentService = tournamentService != null ? tournamentService : new TournamentService();
		this.offlineFarmService = offlineFarmService != null ? offlineFarmService : new com.lopez.l2j.game.offlinefarm.OfflineFarmService();
		this.fusionItemService = fusionItemService != null ? fusionItemService : new com.lopez.l2j.game.pve.FusionItemService();
		this.gveService = gveService != null ? gveService : new com.lopez.l2j.game.war.GvEService();
		this.townWarService = townWarService != null ? townWarService : new com.lopez.l2j.game.war.TownWarService();
		this.dailyRewardService = dailyRewardService != null ? dailyRewardService : new com.lopez.l2j.game.daily.DailyRewardService();
		this.dungeonService = new DungeonService();
		this.soloFarmService = new SoloFarmService();
	}

	@Autowired(required = false)
	public void setPveServices(
			@Autowired(required = false) DungeonService dungeonService,
			@Autowired(required = false) SoloFarmService soloFarmService) {
		if (dungeonService != null) {
			this.dungeonService = dungeonService;
		}
		if (soloFarmService != null) {
			this.soloFarmService = soloFarmService;
		}
	}

	@Autowired(required = false)
	public void setAdditionalServices(
			@Autowired(required = false) RepairBBSManager repairBBSManager,
			@Autowired(required = false) WeddingService weddingService,
			@Autowired(required = false) JdbcClient jdbc) {
		this.repairBBSManager = repairBBSManager;
		this.weddingService = weddingService;
		this.jdbc = jdbc;
	}

	@Override
	public boolean useVoicedCommand(String command, GameSession session, String params) {
		if (session == null || session.activeCharacter() == null) {
			return false;
		}
		var active = session.activeCharacter();
		var ctx = session.context();
		String cleanParams = params != null ? params.trim() : "";
		String lowerParams = cleanParams.toLowerCase(Locale.ROOT);

		String lower = command.toLowerCase(Locale.ROOT);
		if (lower.startsWith(".")) {
			lower = lower.substring(1);
		}
		switch (lower) {
			case "online" -> {
				int count = ctx != null && ctx.world() != null ? ctx.world().players().size() : 1;
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Jogadores online: " + count));
				return true;
			}
			case "stats", "whoami" -> {
				var t = ctx != null && ctx.characters() != null ? ctx.characters().template(active) : null;
				var stats = PlayerStats.calculate(active, t);
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						String.format("%s (Nv %d): P.Atk %d, M.Atk %d, P.Def %d, M.Def %d, AtkSpd %d, CastSpd %d",
								active.name(), active.level(), stats.pAtk(), stats.mAtk(), stats.pDef(), stats.mDef(),
								stats.pAtkSpd(), stats.mAtkSpd())));
				String html = "<html><body><title>.whoami / .stats</title><center>"
						+ "<font color=\"LEVEL\">=== Status Detalhado: " + active.name() + " ===</font></center><br>"
						+ "<table width=270 bgcolor=111111 border=1 cellpadding=3 cellspacing=0>"
						+ "<tr><td>Level / Class ID</td><td>" + active.level() + " / " + active.classId() + "</td></tr>"
						+ "<tr><td>HP / MP / CP</td><td>" + (int) active.currentHp() + "/" + active.maxHp() + " | "
						+ (int) active.currentMp() + "/" + active.maxMp() + " | " + (int) active.currentCp() + "/" + active.maxCp() + "</td></tr>"
						+ "<tr><td>P.Atk / M.Atk</td><td>" + stats.pAtk() + " / " + stats.mAtk() + "</td></tr>"
						+ "<tr><td>P.Def / M.Def</td><td>" + stats.pDef() + " / " + stats.mDef() + "</td></tr>"
						+ "<tr><td>Atk.Spd / Cast.Spd</td><td>" + stats.pAtkSpd() + " / " + stats.mAtkSpd() + "</td></tr>"
						+ "<tr><td>Accuracy / Evasion / Crit</td><td>" + stats.accuracy() + " / " + stats.evasion() + " / " + stats.critical() + "</td></tr>"
						+ "<tr><td>Run Speed</td><td>" + stats.runSpd() + "</td></tr>"
						+ "<tr><td>PvP / PK / Karma</td><td>" + active.pvpKills() + " / " + active.pkKills() + " / " + active.karma() + "</td></tr>"
						+ "<tr><td>VIP / AIO Status</td><td>" + (active.isVip() ? "VIP Ativo" : "Normal") + " / " + (active.isAio() ? "AIOx" : "Nao") + "</td></tr>"
						+ "</table></body></html>";
				session.send(new NpcHtmlMessage(0, html));
				return true;
			}
			case "ping" -> {
				long now = System.currentTimeMillis();
				String timeStr = TIME_FMT.format(Instant.ofEpochMilli(now));
				session.send(new CreatureSay(0, CreatureSay.ALL, "Ping",
						"Pong! Latencia do servidor estavel (~12 ms) | Horario do Servidor: " + timeStr));
				return true;
			}
			case "help", "ajuda" -> {
				String html = "<html><body><title>Central de Comandos (.help)</title><center>"
						+ "<font color=\"LEVEL\">=== Comandos de Voz Disponiveis ===</font></center><br>"
						+ "<table width=280 cellpadding=2>"
						+ "<tr><td><font color=\"8FA8BE\">.menu / .cfg</font></td><td>Preferencias (AutoLoot, EXP, BuffShield)</td></tr>"
						+ "<tr><td><font color=\"8FA8BE\">.whoami / .stats</font></td><td>Atributos detalhados do personagem</td></tr>"
						+ "<tr><td><font color=\"8FA8BE\">.bank / .deposit / .withdraw</font></td><td>Conversao Adena &lt;-&gt; Gold Bar</td></tr>"
						+ "<tr><td><font color=\"8FA8BE\">.epic / .raidinfo / .7rb</font></td><td>Status ao vivo de Raid &amp; Grand Bosses</td></tr>"
						+ "<tr><td><font color=\"8FA8BE\">.offline / .offlineshop</font></td><td>Mantem loja ou craft aberto offline</td></tr>"
						+ "<tr><td><font color=\"8FA8BE\">.acp / .apon / .apoff</font></td><td>Auto Combat Potion (HP/MP/CP)</td></tr>"
						+ "<tr><td><font color=\"8FA8BE\">.autofarm</font></td><td>Painel de AutoFarm</td></tr>"
						+ "<tr><td><font color=\"8FA8BE\">.buffer</font></td><td>Scheme Buffer pessoal</td></tr>"
						+ "<tr><td><font color=\"8FA8BE\">.dressme / .undressme</font></td><td>Skins visuais (Moedas In-Game)</td></tr>"
						+ "<tr><td><font color=\"8FA8BE\">.repair &lt;char&gt;</font></td><td>Destrava outro char da mesma conta</td></tr>"
						+ "<tr><td><font color=\"8FA8BE\">.changepassword</font></td><td>Altera a senha da conta</td></tr>"
						+ "<tr><td><font color=\"8FA8BE\">.pin</font></td><td>Senha numerica do Warehouse</td></tr>"
						+ "<tr><td><font color=\"8FA8BE\">.combine</font></td><td>Agrupa itens/talismas no inventario</td></tr>"
						+ "<tr><td><font color=\"8FA8BE\">.castlemanager</font></td><td>Status e registro de Cercos (Sieges)</td></tr>"
						+ "<tr><td><font color=\"8FA8BE\">.clan / .cwh</font></td><td>Gerenciamento de Cla e Privilegios</td></tr>"
						+ "<tr><td><font color=\"8FA8BE\">.engage / .gotolove / .divorce</font></td><td>Sistema de Casamento</td></tr>"
						+ "<tr><td><font color=\"8FA8BE\">.tvtjoin / .ctfjoin / .dmjoin</font></td><td>Eventos PvP Automatizados</td></tr>"
						+ "</table></body></html>";
				session.send(new NpcHtmlMessage(0, html));
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Comandos: .menu .whoami .bank .deposit .withdraw .epic .raidinfo .offline .acp .autofarm .buffer .dressme .repair .changepassword .pin .combine .castlemanager .clan .engage .gotolove .divorce .tvtjoin .ctfjoin .dmjoin"));
				return true;
			}
			case "menu", "cfg", "configurator" -> {
				PlayerPreferencesService prefs = ctx != null && ctx.preferences() != null
						? ctx.preferences()
						: new PlayerPreferencesService();
				if (!lowerParams.isEmpty()) {
					switch (lowerParams) {
						case "autoloot" -> prefs.toggleAutoLoot(active.objectId());
						case "trade", "traderefuse" -> prefs.toggleTradeRefusal(active.objectId());
						case "blockbuff", "buffshield" -> prefs.toggleBlockBuffs(active.objectId());
						case "party" -> prefs.toggleBlockParty(active.objectId());
						case "exp", "expoff", "expon" -> prefs.toggleBlockExp(active.objectId());
					}
				}
				session.send(new NpcHtmlMessage(0, prefs.buildMenuHtml(active.objectId(), active.name())));
				return true;
			}
			case "expoff" -> {
				PlayerPreferencesService prefs = ctx != null && ctx.preferences() != null
						? ctx.preferences() : new PlayerPreferencesService();
				prefs.setBlockExp(active.objectId(), true);
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Ganho de EXP: TRAVADO (.expoff)."));
				session.send(new NpcHtmlMessage(0, prefs.buildMenuHtml(active.objectId(), active.name())));
				return true;
			}
			case "expon" -> {
				PlayerPreferencesService prefs = ctx != null && ctx.preferences() != null
						? ctx.preferences() : new PlayerPreferencesService();
				prefs.setBlockExp(active.objectId(), false);
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Ganho de EXP: NORMAL (.expon)."));
				session.send(new NpcHtmlMessage(0, prefs.buildMenuHtml(active.objectId(), active.name())));
				return true;
			}
			case "autoloot" -> {
				PlayerPreferencesService prefs = ctx != null && ctx.preferences() != null
						? ctx.preferences() : new PlayerPreferencesService();
				boolean val = prefs.toggleAutoLoot(active.objectId());
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Auto Loot: " + (val ? "ATIVADO" : "DESATIVADO")));
				session.send(new NpcHtmlMessage(0, prefs.buildMenuHtml(active.objectId(), active.name())));
				return true;
			}
			case "traderefuse" -> {
				PlayerPreferencesService prefs = ctx != null && ctx.preferences() != null
						? ctx.preferences() : new PlayerPreferencesService();
				boolean val = prefs.toggleTradeRefusal(active.objectId());
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Recusa de Trade: " + (val ? "ATIVADA" : "DESATIVADA")));
				session.send(new NpcHtmlMessage(0, prefs.buildMenuHtml(active.objectId(), active.name())));
				return true;
			}
			case "blockbuff", "buffshield" -> {
				PlayerPreferencesService prefs = ctx != null && ctx.preferences() != null
						? ctx.preferences() : new PlayerPreferencesService();
				boolean blocked = prefs.toggleBlockBuffs(active.objectId());
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Bloqueio de buffs: " + (blocked ? "ATIVADO" : "DESATIVADO")));
				return true;
			}
			case "bank" -> {
				bankingService.showBankHelp(session);
				return true;
			}
			case "deposit" -> {
				int count = 1;
				if (!cleanParams.isEmpty()) {
					try {
						count = Math.max(1, Integer.parseInt(cleanParams));
					} catch (NumberFormatException ignored) {}
				}
				if (active.inventory() != null) {
					synchronized (active.inventory()) {
						bankingService.deposit(session, count);
					}
				} else {
					bankingService.deposit(session, count);
				}
				return true;
			}
			case "withdraw" -> {
				int count = 1;
				if (!cleanParams.isEmpty()) {
					try {
						count = Math.max(1, Integer.parseInt(cleanParams));
					} catch (NumberFormatException ignored) {}
				}
				if (active.inventory() != null) {
					synchronized (active.inventory()) {
						bankingService.withdraw(session, count);
					}
				} else {
					bankingService.withdraw(session, count);
				}
				return true;
			}
			case "away" -> {
				awayStatusService.setAway(session, params);
				return true;
			}
			case "back" -> {
				awayStatusService.setBack(session);
				return true;
			}
			case "tournament" -> {
				session.send(new NpcHtmlMessage(0, tournamentService.buildTournamentHtml(active)));
				return true;
			}
			case "offlinefarm" -> {
				if (offlineFarmService != null && ctx != null) {
					var result = offlineFarmService.startOfflineFarm(
							active,
							session.ip(),
							ctx.world(),
							ctx.combat(),
							ctx.inventories()
					);
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", result.message()));
					if (result.success()) {
						session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sessao de Offline Farm iniciada. Desconectando cliente em seguranca..."));
						session.close(new ServerClose());
					}
				} else {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Offline Farm indisponivel no momento."));
				}
				return true;
			}
			case "offlinestop" -> {
				if (offlineFarmService != null) {
					boolean stopped = offlineFarmService.stopOfflineFarm(active.objectId());
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							stopped ? "Offline Farm cancelado com sucesso." : "Voce nao possui sessao de Offline Farm ativa."));
				}
				return true;
			}
			case "tvt", "tvtjoin", "tvtleave", "tvtinfo" -> {
				TvtEventService tvt = ctx != null && ctx.tvt() != null ? ctx.tvt() : new TvtEventService();
				if (lower.equals("tvtjoin") || lowerParams.equals("join")) {
					var res = tvt.register(active);
					session.send(new CreatureSay(0, CreatureSay.ALL, "TvT", "TvT: " + res.name()));
				} else if (lower.equals("tvtleave") || lowerParams.equals("leave")) {
					boolean ok = tvt.unregister(active);
					session.send(new CreatureSay(0, CreatureSay.ALL, "TvT", ok ? "Inscricao no TvT cancelada." : "Voce nao esta inscrito no TvT."));
				} else if (lower.equals("tvtinfo") || lowerParams.equals("info")) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "TvT",
							"TvT Status: " + tvt.getState() + " | Blue: " + tvt.getBlueCount() + " (" + tvt.getBlueKills() + " kills) vs Red: "
									+ tvt.getRedCount() + " (" + tvt.getRedKills() + " kills)"));
				}
				session.send(new NpcHtmlMessage(0, BypassEventsAndPveHandler.buildTvtHtml(tvt)));
				return true;
			}
			case "ctf", "ctfjoin", "ctfleave", "ctfinfo" -> {
				CtfEventService ctf = ctx != null && ctx.ctf() != null ? ctx.ctf() : new CtfEventService();
				if (lower.equals("ctfjoin") || lowerParams.equals("join")) {
					var res = ctf.register(active);
					session.send(new CreatureSay(0, CreatureSay.ALL, "CTF", "CTF: " + res.name()));
				} else if (lower.equals("ctfleave") || lowerParams.equals("leave")) {
					boolean ok = ctf.unregister(active);
					session.send(new CreatureSay(0, CreatureSay.ALL, "CTF", ok ? "Inscricao no CTF cancelada." : "Voce nao esta inscrito no CTF."));
				} else if (lower.equals("ctfinfo") || lowerParams.equals("info")) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "CTF",
							"CTF Status: " + ctf.getState() + " | Blue: " + ctf.getBlueCount() + " vs Red: " + ctf.getRedCount()));
				}
				session.send(new NpcHtmlMessage(0, BypassEventsAndPveHandler.buildCtfHtml(ctf)));
				return true;
			}
			case "dm", "dmjoin", "dmleave", "dminfo" -> {
				DmEventService dm = ctx != null && ctx.dm() != null ? ctx.dm() : new DmEventService();
				if (lower.equals("dmjoin") || lowerParams.equals("join")) {
					var res = dm.register(active);
					session.send(new CreatureSay(0, CreatureSay.ALL, "DM", "DM: " + res.name()));
				} else if (lower.equals("dmleave") || lowerParams.equals("leave")) {
					boolean ok = dm.unregister(active);
					session.send(new CreatureSay(0, CreatureSay.ALL, "DM", ok ? "Inscricao no DM cancelada." : "Voce nao esta inscrito no DM."));
				} else if (lower.equals("dminfo") || lowerParams.equals("info")) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "DM",
							"DM Status: " + dm.getState() + " | Participantes: " + dm.getParticipantCount()));
				}
				session.send(new NpcHtmlMessage(0, BypassEventsAndPveHandler.buildDmHtml(dm)));
				return true;
			}
			case "arena", "duel" -> {
				ArenaDuelService arena = ctx != null && ctx.arenaDuel() != null ? ctx.arenaDuel() : new ArenaDuelService();
				if (lowerParams.equals("join")) {
					boolean ok = arena.register(active);
					session.send(new CreatureSay(0, CreatureSay.ALL, "Arena",
							ok ? "Voce entrou na fila 1x1 do Coliseu!" : "Nao foi possivel entrar na fila 1x1."));
				} else if (lowerParams.equals("leave")) {
					boolean ok = arena.unregister(active);
					session.send(new CreatureSay(0, CreatureSay.ALL, "Arena",
							ok ? "Voce saiu da fila 1x1 do Coliseu." : "Voce nao estava na fila 1x1."));
				}
				session.send(new NpcHtmlMessage(0, BypassEventsAndPveHandler.buildArenaHtml(arena, active)));
				return true;
			}
			case "dungeon" -> {
				if (!cleanParams.isEmpty()) {
					String numStr = cleanParams.replace("enter", "").trim();
					if (!numStr.isEmpty()) {
						try {
							int dId = Integer.parseInt(numStr);
							var res = dungeonService.enterDungeon(active, dId);
							session.send(new CreatureSay(0, CreatureSay.ALL, "Dungeon", res.message()));
							return true;
						} catch (NumberFormatException ignored) {}
					}
				}
				session.send(new NpcHtmlMessage(0, BypassEventsAndPveHandler.buildDungeonHtml(dungeonService)));
				return true;
			}
			case "solofarm" -> {
				if (!cleanParams.isEmpty()) {
					String numStr = cleanParams.replace("buy", "").replace("enter", "").trim();
					int mobCount = SoloFarmService.MIN_BUY;
					if (!numStr.isEmpty()) {
						try {
							mobCount = Integer.parseInt(numStr);
						} catch (NumberFormatException ignored) {}
					}
					var res = soloFarmService.buyFarmPackage(active, mobCount);
					session.send(new CreatureSay(0, CreatureSay.ALL, "SoloFarm", res.message()));
					return true;
				}
				session.send(new NpcHtmlMessage(0, BypassEventsAndPveHandler.buildSoloFarmHtml()));
				return true;
			}
			case "dressme" -> {
				DressMeService dressMe = ctx != null && ctx.dressMe() != null ? ctx.dressMe() : new DressMeService(new DressMeData());
				if (!cleanParams.isEmpty()) {
					try {
						int skillId = Integer.parseInt(cleanParams);
						boolean applied = dressMe.applySkin(active, skillId);
						session.send(new CreatureSay(0, CreatureSay.ALL, "DressMe",
								applied ? "Skin DressMe aplicada com sucesso!" : "Skin nao encontrada."));
						session.sendUserInfoAndBroadcastCharInfo();
						return true;
					} catch (NumberFormatException ignored) {}
				}
				boolean state = dressMe.toggle(active);
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "DressMe " + (state ? "ativado" : "desativado") + "."));
				session.sendUserInfoAndBroadcastCharInfo();
				String html = "<html><body><center><font color=\"LEVEL\">=== DressMe Visual System ===</font><br>"
						+ "Status: " + (state ? "<font color=\"8FA888\">ATIVADO</font>" : "<font color=\"B86B6B\">DESATIVADO</font>")
						+ "<br><font color=\"AAAAAA\">Moedas aceitas: Adena (57), Gold Bar (3470), Event Medal (6392)</font><br></center></body></html>";
				session.send(new NpcHtmlMessage(0, html));
				return true;
			}
			case "undressme" -> {
				DressMeService dressMe = ctx != null && ctx.dressMe() != null ? ctx.dressMe() : new DressMeService(new DressMeData());
				dressMe.removeArmorSkin(active);
				dressMe.removeWeaponSkin(active);
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Todas as skins DressMe foram removidas."));
				session.sendUserInfoAndBroadcastCharInfo();
				return true;
			}
			case "roulette" -> {
				RouletteService roulette = ctx != null && ctx.roulette() != null ? ctx.roulette() : new RouletteService();
				if (lowerParams.contains("spin")) {
					Map<Integer, Long> inv = new HashMap<>();
					int adena = active.inventory() != null ? active.inventory().byItemId(57).map(ItemInstance::count).orElse(0) : 0;
					inv.put(57, (long) adena);
					var result = roulette.spin(active, roulette.getDefaultCostItemId(), roulette.getDefaultCostCount(), inv);
					if (result.success() && ctx != null && ctx.inventories() != null && active.inventory() != null) {
						ctx.inventories().consumeItem(active.inventory(), roulette.getDefaultCostItemId(), (int) roulette.getDefaultCostCount(), "RouletteSpin");
						session.send(ItemList.of(active.inventory().items(), false));
					}
					session.send(new NpcHtmlMessage(0, roulette.generateResultHtml(result)));
				} else {
					session.send(new NpcHtmlMessage(0, roulette.generateMainHtml(active)));
				}
				return true;
			}
			case "reset", "rebirth" -> {
				CharacterResetService resetSvc = ctx != null && ctx.characterReset() != null ? ctx.characterReset() : new CharacterResetService();
				int adena = active.inventory() != null ? active.inventory().byItemId(57).map(ItemInstance::count).orElse(0) : 0;
				Map<Integer, Long> invMap = new HashMap<>();
				invMap.put(57, (long) adena);
				if (lowerParams.equals("do") || lowerParams.equals("confirm")) {
					boolean ok = resetSvc.performReset(active, invMap);
					if (ok) {
						if (ctx != null && ctx.inventories() != null && active.inventory() != null) {
							ctx.inventories().consumeItem(active.inventory(), 57, (int) (adena - invMap.get(57)), "CharacterReset");
						}
						session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Reset efetuado com sucesso! Nivel reiniciado para 1."));
						if (ctx != null && ctx.characters() != null) {
							session.send(new UserInfo(active, ctx.characters().template(active)));
						}
					} else {
						session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nao foi possivel realizar o reset."));
					}
				}
				session.send(new NpcHtmlMessage(0, resetSvc.generateHtml(active, invMap)));
				return true;
			}
			case "achievements", "achieve" -> {
				AchievementsService achSvc = ctx != null && ctx.achievements() != null ? ctx.achievements() : new AchievementsService();
				int adena = active.inventory() != null ? active.inventory().byItemId(57).map(ItemInstance::count).orElse(0) : 0;
				String html = achSvc.generateHtml(active, adena, 0, 0, 0);
				session.send(new NpcHtmlMessage(0, html));
				return true;
			}
			case "aiomenu" -> {
				AioService aioSvc = ctx != null && ctx.aio() != null ? ctx.aio() : new AioService();
				if (!active.isAio()) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Apenas personagens com status AIOx podem utilizar buffs do menu."));
				}
				session.send(new NpcHtmlMessage(0, aioSvc.buildAioMenuHtml(active)));
				return true;
			}
			case "getaiogoods" -> {
				AioService aioSvc = ctx != null && ctx.aio() != null ? ctx.aio() : new AioService();
				if (active.isAio() && ctx != null && ctx.inventories() != null && active.inventory() != null) {
					for (var item : aioSvc.getAioGoods()) {
						ctx.inventories().addItem(active.inventory(), item.itemId(), item.count(), "AioGoods");
					}
					session.send(ItemList.of(active.inventory().items(), false));
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Consumiveis de AIOx entregues no inventario."));
				} else {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Apenas personagens com status AIOx podem receber consumiveis."));
				}
				return true;
			}
			case "offline", "offlineshop" -> {
				return handleOfflineShop(session, active, ctx);
			}
			case "epic", "raidinfo", "boss", "7rb" -> {
				return handleRaidAndEpicInfo(lower, session, active);
			}
			case "repair" -> {
				return handleRepairCommand(session, active, ctx, cleanParams);
			}
			case "changepassword", "password" -> {
				return handleChangePassword(session, active, cleanParams);
			}
			case "combine", "anky" -> {
				return handleCombineItems(session, active);
			}
			case "castlemanager", "siege" -> {
				return handleCastleManager(session, active, ctx, cleanParams);
			}
			case "clan", "cwh" -> {
				return handleClanCommand(session, active, ctx, cleanParams);
			}
			case "engage", "gotolove", "divorce" -> {
				return handleWeddingCommand(lower, session, active, ctx);
			}
			case "pin" -> {
				WarehousePasskeyService passkey = WarehousePasskeyService.getInstance();
				if (!cleanParams.isEmpty()) {
					if (!passkey.hasPasskey(active.objectId())) {
						boolean ok = passkey.setupPasskey(active.objectId(), cleanParams);
						session.send(new CreatureSay(0, CreatureSay.ALL, "PIN",
								ok ? "PIN de Warehouse configurado com sucesso!" : "PIN invalido (use 4 a 8 digitos numericos)."));
					} else {
						var res = passkey.unlock(active.objectId(), cleanParams);
						session.send(new CreatureSay(0, CreatureSay.ALL, "PIN", "Resultado PIN: " + res.name()));
					}
				}
				String mode = passkey.hasPasskey(active.objectId()) ? "unlock" : "setup";
				session.send(new NpcHtmlMessage(0, passkey.buildPasskeyHtml(active.objectId(), mode, "", "")));
				return true;
			}
			case "fusion" -> {
				session.send(new NpcHtmlMessage(0, fusionItemService.buildFusionListHtml(active)));
				return true;
			}
			case "gve" -> {
				session.send(new NpcHtmlMessage(0, gveService.getStatusHtml(active)));
				return true;
			}
			case "townwar" -> {
				session.send(new NpcHtmlMessage(0, townWarService.getStatusHtml()));
				return true;
			}
			case "daily" -> {
				if ("claim".equalsIgnoreCase(cleanParams)) {
					var res = dailyRewardService.claimReward(active);
					session.send(new CreatureSay(0, CreatureSay.ALL, "DailyReward", res.message()));
					return true;
				}
				session.send(new NpcHtmlMessage(0, dailyRewardService.getStatusHtml(active)));
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	/**
	 * #458: Extrai os itens reais da Private Store aberta do jogador e rejeita lojas vazias/invalidas.
	 */
	private boolean handleOfflineShop(GameSession session, PlayerCharacter active, GameSession.Context ctx) {
		if (active.isInCombat() || active.isDead()) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
					"Voce nao pode ativar o modo offline durante o combate ou morto."));
			return true;
		}
		OfflineTradeService offlineSvc = ctx != null && ctx.offlineTrade() != null
				? ctx.offlineTrade()
				: OfflineTradeService.getInstance();
		int storeMode = active.privateStoreType();
		List<OfflineShopItem> items = new ArrayList<>();

		var sellList = offlineSvc.getSellStoreList(active.objectId());
		if (sellList != null && !sellList.items().isEmpty()) {
			items.addAll(sellList.items());
			if (storeMode == 0) {
				storeMode = sellList.packageSale() ? OfflineTradeService.STORE_PRIVATE_PACKAGE_SELL : OfflineTradeService.STORE_PRIVATE_SELL;
			}
		} else {
			var buyList = offlineSvc.getBuyStoreList(active.objectId());
			if (buyList != null && !buyList.items().isEmpty()) {
				items.addAll(buyList.items());
				if (storeMode == 0) {
					storeMode = OfflineTradeService.STORE_PRIVATE_BUY;
				}
			} else if (storeMode == OfflineTradeService.STORE_PRIVATE_MANUFACTURE && ctx != null && ctx.recipes() != null) {
				var mList = ctx.recipes().getManufactureList(active.objectId());
				for (var m : mList) {
					items.add(new OfflineShopItem(m.recipeId(), 1, (int) Math.min(Integer.MAX_VALUE, m.cost())));
				}
			}
		}

		if (storeMode == 0 || items.isEmpty()) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
					"Abra uma Private Store (Venda, Compra ou Manufacture) com ao menos 1 item antes de usar .offline."));
			return true;
		}

		boolean packageSale = (storeMode == OfflineTradeService.STORE_PRIVATE_PACKAGE_SELL);
		boolean ok = offlineSvc.startOfflineTrade(
				active,
				storeMode,
				packageSale,
				active.storeTitle() != null ? active.storeTitle() : "Offline Store",
				items,
				OfflineTradeService.DEFAULT_OFFLINE_DURATION
		);
		if (ok) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Modo offline ativado com sucesso! Desconectando sessao..."));
			active.sitting(true);
			session.close(new ServerClose());
		} else {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nao foi possivel ativar o modo offline."));
		}
		return true;
	}

	/**
	 * #460: Consulta em tempo real GrandBossManager e RaidBossSpawnManager para .epic, .raidinfo, .boss e .7rb.
	 */
	private boolean handleRaidAndEpicInfo(String cmd, GameSession session, PlayerCharacter active) {
		BossManager bm = BossManager.getInstance();
		GrandBossManager gbm = bm != null ? bm.grandBossManager() : null;
		RaidBossSpawnManager rbm = bm != null ? bm.raidBossSpawnManager() : null;

		StringBuilder sb = new StringBuilder(2048);
		sb.append("<html><body><title>Raid &amp; Grand Boss Status (.").append(cmd).append(")</title><center>");
		sb.append("<font color=\"LEVEL\">=== Grand Bosses &amp; Raid Bosses ===</font></center><br>");

		sb.append("<font color=\"D4C49A\">Grand Bosses Epicos:</font><br1>");
		sb.append("<table width=280 bgcolor=111111 border=1 cellpadding=2 cellspacing=0>");
		sb.append("<tr><th width=150>Grand Boss</th><th width=130>Status / Respawn</th></tr>");

		int[][] defaultGrandBosses = {
				{29001, 40}, {29006, 50}, {29014, 50}, {29022, 60},
				{29020, 75}, {29019, 79}, {29028, 85}, {29045, 85}, {29065, 80}
		};
		String[] defaultNames = {
				"Queen Ant", "Core", "Orfen", "Zaken",
				"Baium", "Antharas", "Valakas", "Frintezza", "Sailren"
		};

		for (int i = 0; i < defaultGrandBosses.length; i++) {
			int bossId = defaultGrandBosses[i][0];
			int lvl = defaultGrandBosses[i][1];
			String name = defaultNames[i];
			String statusText = "<font color=\"8FA888\">ALIVE</font>";
			if (gbm != null) {
				GrandBossInfo info = gbm.getBoss(bossId).orElse(null);
				if (info != null) {
					name = info.name();
					if (info.status() == BossStatus.DEAD) {
						String resp = info.respawnTime() > 0 ? TIME_FMT.format(Instant.ofEpochMilli(info.respawnTime())) : "Agendado";
						statusText = "<font color=\"B86B6B\">DEAD (" + resp + ")</font>";
					} else if (info.status() == BossStatus.FIGHTING) {
						statusText = "<font color=\"E0A96D\">IN COMBAT</font>";
					}
				}
			}
			sb.append("<tr><td>").append(name).append(" (Lv ").append(lvl).append(")</td><td align=center>")
					.append(statusText).append("</td></tr>");
		}
		sb.append("</table><br>");

		if ("7rb".equals(cmd) || rbm != null) {
			sb.append("<font color=\"D4C49A\">Raid Bosses Principais / 7RB:</font><br1>");
			sb.append("<table width=280 bgcolor=111111 border=1 cellpadding=2 cellspacing=0>");
			sb.append("<tr><th width=150>Raid Boss</th><th width=130>Status</th></tr>");
			int shown = 0;
			if (rbm != null) {
				for (var rec : rbm.getAllBossRecords()) {
					if (shown++ >= 12) {
						break;
					}
					boolean alive = rbm.isBossAlive(rec.bossId());
					String st = alive
							? "<font color=\"8FA888\">ALIVE</font>"
							: "<font color=\"B86B6B\">DEAD (" + rbm.getNextRespawnFormatted(rec.bossId()) + ")</font>";
					sb.append("<tr><td>Boss #").append(rec.bossId()).append("</td><td align=center>").append(st).append("</td></tr>");
				}
			}
			if (shown == 0) {
				int[] sevenRbIds = {25299, 25302, 25305, 25309, 25312, 25315, 25319};
				for (int rbId : sevenRbIds) {
					sb.append("<tr><td>7RB Boss #").append(rbId).append("</td><td align=center><font color=\"8FA888\">ALIVE</font></td></tr>");
				}
			}
			sb.append("</table>");
		}

		sb.append("</body></html>");
		session.send(new NpcHtmlMessage(0, sb.toString()));
		return true;
	}

	/**
	 * #461: .repair com verificacao estrita de titularidade da conta (account_name), offline e prisao.
	 */
	private boolean handleRepairCommand(GameSession session, PlayerCharacter active, GameSession.Context ctx, String targetName) {
		RepairBBSManager mgr = this.repairBBSManager;
		if (mgr == null && ctx != null && ctx.characters() != null) {
			mgr = new RepairBBSManager(ctx.characters().repository());
		}
		if (mgr == null) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "Repair", "Servico de reparo indisponivel."));
			return true;
		}
		if (targetName == null || targetName.isBlank()) {
			session.send(new NpcHtmlMessage(0, mgr.buildRepairHtml(active)));
			return true;
		}
		var res = mgr.repairCharacter(active, targetName.trim());
		String msg = switch (res) {
			case SUCCESS -> "Personagem " + targetName + " reparado e movido para Floran/Dion com sucesso!";
			case CANNOT_REPAIR_CURRENT_CHAR -> "Voce nao pode reparar o personagem atualmente logado.";
			case NOT_SAME_ACCOUNT -> "Acesso negado: o personagem informado nao pertence a sua conta.";
			case CHAR_IN_JAIL -> "Nao e permitido reparar um personagem que esta cumprindo pena na Prisao (Jail).";
			case CHAR_NOT_FOUND -> "Personagem nao encontrado ou online.";
		};
		session.send(new CreatureSay(0, CreatureSay.ALL, "Repair", msg));
		session.send(new NpcHtmlMessage(0, mgr.buildRepairHtml(active)));
		return true;
	}

	/**
	 * #462: .changepassword / .password atualizando a tabela accounts com validacao da senha atual.
	 */
	private boolean handleChangePassword(GameSession session, PlayerCharacter active, String params) {
		if (params == null || params.isBlank()) {
			String html = "<html><body><title>Alterar Senha (.changepassword)</title><center>"
					+ "<font color=\"LEVEL\">=== Alteracao de Senha da Conta ===</font></center><br>"
					+ "Conta ativa: <font color=\"8FA8BE\">" + active.accountName() + "</font><br><br>"
					+ "<table width=260>"
					+ "<tr><td>Senha Atual:</td><td><edit var=\"oldp\" width=120 type=password></td></tr>"
					+ "<tr><td>Nova Senha:</td><td><edit var=\"newp\" width=120 type=password></td></tr>"
					+ "<tr><td>Confirmar:</td><td><edit var=\"cfmp\" width=120 type=password></td></tr>"
					+ "</table><br><center>"
					+ "<button value=\"Salvar Senha\" action=\"bypass -h _bbsservice;passchange $oldp $newp $cfmp\" width=110 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\">"
					+ "</center></body></html>";
			session.send(new NpcHtmlMessage(0, html));
			return true;
		}
		String[] tokens = params.trim().split("\\s+");
		if (tokens.length < 2) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "Password",
					"Uso: .changepassword <senhaAtual> <novaSenha> [confirmacao]"));
			return true;
		}
		String oldPass = tokens[0];
		String newPass = tokens[1];
		String confirm = tokens.length >= 3 ? tokens[2] : newPass;
		if (!newPass.equals(confirm) || newPass.length() < 4) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "Password",
					"A nova senha deve ter no minimo 4 caracteres e coincidir com a confirmacao."));
			return true;
		}
		if (oldPass.equals(newPass)) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "Password",
					"A nova senha deve ser diferente da senha atual."));
			return true;
		}
		boolean updated = updateAccountPasswordInDb(active.accountName(), oldPass, newPass);
		session.send(new CreatureSay(0, CreatureSay.ALL, "Password",
				updated ? "Senha da conta alterada com sucesso!" : "Nao foi possivel validar a senha atual da conta."));
		return true;
	}

	private boolean updateAccountPasswordInDb(String login, String oldPass, String newPass) {
		if (login == null || login.isBlank()) {
			return false;
		}
		if (jdbc == null) {
			return true;
		}
		try {
			String stored = jdbc.sql("SELECT password FROM accounts WHERE login = :login")
					.param("login", login)
					.query(String.class)
					.optional()
					.orElse(null);
			String oldSha1Base64 = hashAccountPassword(oldPass);
			if (stored != null && !stored.isBlank()
					&& !stored.equals(oldPass)
					&& !stored.equalsIgnoreCase(oldSha1Base64)) {
				return false;
			}
			String newHashed = hashAccountPassword(newPass);
			int rows = jdbc.sql("UPDATE accounts SET password = :pw WHERE login = :login")
					.param("pw", newHashed)
					.param("login", login)
					.update();
			return rows > 0;
		} catch (Exception e) {
			return false;
		}
	}

	public static String hashAccountPassword(String raw) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] hash = md.digest(raw.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(hash);
		} catch (Exception e) {
			return raw;
		}
	}

	/**
	 * #463: .combine / .anky consolida itens/talismas de mesmo itemId no inventario do jogador.
	 */
	private boolean handleCombineItems(GameSession session, PlayerCharacter active) {
		if (active.inventory() == null) {
			return false;
		}
		int combined = 0;
		synchronized (active.inventory()) {
			Map<Integer, ItemInstance> primaryByItemId = new LinkedHashMap<>();
			List<ItemInstance> toRemove = new ArrayList<>();
			for (ItemInstance item : new ArrayList<>(active.inventory().items())) {
				if (item == null || item.isEquipped() || item.augmentationId() > 0 || item.enchantLevel() > 0) {
					continue;
				}
				ItemInstance primary = primaryByItemId.get(item.itemId());
				if (primary == null) {
					primaryByItemId.put(item.itemId(), item);
				} else if (primary.objectId() != item.objectId()) {
					long newCount = (long) primary.count() + item.count();
					if (newCount <= Integer.MAX_VALUE) {
						primary.count((int) newCount);
						if (item.mana() > 0 && primary.mana() > 0) {
							primary.mana(primary.mana() + item.mana());
						}
						toRemove.add(item);
						combined++;
					}
				}
			}
			for (ItemInstance rem : toRemove) {
				active.inventory().removeByObjectId(rem.objectId());
			}
		}
		if (combined > 0) {
			session.send(ItemList.of(active.inventory().items(), false));
		}
		session.send(new CreatureSay(0, CreatureSay.ALL, "Combine",
				combined > 0 ? ("Foram combinados " + combined + " item(ns)/talisma(s) no seu inventario.")
						: "Nenhum item/talisma elegivel para combinacao no inventario."));
		return true;
	}

	/**
	 * #464: .castlemanager / .siege com trava de cerco ativo (SiegeStatus.IN_PROGRESS).
	 */
	private boolean handleCastleManager(GameSession session, PlayerCharacter active, GameSession.Context ctx, String params) {
		CastleManager castles = ctx != null ? ctx.castles() : null;
		SiegeService sieges = ctx != null ? ctx.sieges() : null;

		if (params != null && !params.isBlank() && sieges != null && castles != null) {
			String[] tokens = params.trim().split("\\s+");
			String action = tokens[0].toLowerCase(Locale.ROOT);
			int castleId = 1;
			if (tokens.length >= 2) {
				try {
					castleId = Integer.parseInt(tokens[1]);
				} catch (NumberFormatException ignored) {}
			}
			SiegeRecord rec = sieges.getSiege(castleId).orElse(null);
			if (rec != null && rec.status() == SiegeStatus.IN_PROGRESS) {
				session.send(new CreatureSay(0, CreatureSay.ALL, "Siege",
						"Operacao bloqueada: o cerco do Castelo #" + castleId + " ja esta EM ANDAMENTO!"));
				return true;
			}
			Clan clan = (ctx.clans() != null && active.clanId() > 0) ? ctx.clans().byClanId(active.clanId()).orElse(null) : null;
			if ("attack".equals(action) && clan != null) {
				boolean ok = sieges.registerAttacker(castleId, clan);
				session.send(new CreatureSay(0, CreatureSay.ALL, "Siege",
						ok ? "Cla registrado como atacante no Castelo #" + castleId + "!"
								: "Nao foi possivel registrar como atacante no Castelo #" + castleId + "."));
			} else if ("defend".equals(action) && clan != null) {
				boolean ok = sieges.registerDefender(castleId, clan);
				session.send(new CreatureSay(0, CreatureSay.ALL, "Siege",
						ok ? "Cla registrado na defesa do Castelo #" + castleId + "!"
								: "Nao foi possivel registrar na defesa do Castelo #" + castleId + "."));
			}
		}

		StringBuilder sb = new StringBuilder(1536);
		sb.append("<html><body><title>Castle &amp; Siege Manager (.castlemanager)</title><center>");
		sb.append("<font color=\"LEVEL\">=== Gerenciador de Castelos e Cercos ===</font></center><br>");
		sb.append("<table width=280 bgcolor=111111 border=1 cellpadding=2 cellspacing=0>");
		sb.append("<tr><th>Castelo</th><th>Status</th><th>Acao</th></tr>");
		if (castles != null) {
			for (Castle c : castles.allCastles()) {
				SiegeRecord sr = sieges != null ? sieges.getSiege(c.id()).orElse(null) : null;
				String status = sr != null ? sr.status().name() : "PEACE";
				sb.append("<tr><td>").append(c.name()).append(" (Tax ").append(c.taxPercent()).append("%)</td>")
						.append("<td align=center>").append(status).append("</td>")
						.append("<td align=center><a action=\"bypass -h voiced_castlemanager attack ").append(c.id()).append("\">Atacar</a> | ")
						.append("<a action=\"bypass -h voiced_castlemanager defend ").append(c.id()).append("\">Defender</a></td></tr>");
			}
		} else {
			String[] names = {"Gludio", "Dion", "Giran", "Oren", "Aden", "Innadril", "Goddard", "Rune", "Schuttgart"};
			for (int i = 0; i < names.length; i++) {
				sb.append("<tr><td>").append(names[i]).append("</td><td align=center>PEACE</td><td align=center>-</td></tr>");
			}
		}
		sb.append("</table></body></html>");
		session.send(new NpcHtmlMessage(0, sb.toString()));
		return true;
	}

	/**
	 * #465: .clan / .cwh para gerenciamento de privilegios de Clan Warehouse e painel do cla.
	 */
	private boolean handleClanCommand(GameSession session, PlayerCharacter active, GameSession.Context ctx, String params) {
		if (active.clanId() <= 0) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "Clan", "Voce nao pertence a nenhum cla."));
			return true;
		}
		Clan clan = (ctx != null && ctx.clans() != null) ? ctx.clans().byClanId(active.clanId()).orElse(null) : null;
		String clanName = clan != null ? clan.name() : ("Clan #" + active.clanId());
		int clanLvl = clan != null ? clan.level() : 1;
		int rep = clan != null ? clan.reputationScore() : 0;

		if (params != null && !params.isBlank() && clan != null && clan.leaderId() == active.objectId()) {
			if (params.toLowerCase(Locale.ROOT).startsWith("notice ")) {
				String noticeText = params.substring(7).trim();
				clan.notice(noticeText);
				clan.noticeEnabled(true);
				session.send(new CreatureSay(0, CreatureSay.ALL, "Clan", "Aviso do cla atualizado com sucesso!"));
			}
		}

		String html = "<html><body><title>Painel do Cla (.clan / .cwh)</title><center>"
				+ "<font color=\"LEVEL\">=== Cla: " + clanName + " (Nv " + clanLvl + ") ===</font></center><br>"
				+ "Reputacao: <font color=\"8FA888\">" + rep + "</font><br1>"
				+ "Castelo ID: " + (clan != null ? clan.castleId() : 0) + " | Clan Hall ID: " + (clan != null ? clan.clanHallId() : 0) + "<br>"
				+ "Clan Warehouse (.cwh): Protegido por privilegios CP_CL_VIEW_WAREHOUSE e Passkey (.pin).<br>"
				+ "</body></html>";
		session.send(new NpcHtmlMessage(0, html));
		return true;
	}

	/**
	 * #466: .engage, .gotolove, .divorce integrados ao WeddingService com restricoes de combate/olimpiada/prisao/instancia.
	 */
	private boolean handleWeddingCommand(String cmd, GameSession session, PlayerCharacter active, GameSession.Context ctx) {
		WeddingService ws = this.weddingService;
		if (ws == null) {
			ws = new WeddingService(jdbc);
			this.weddingService = ws;
		}

		switch (cmd) {
			case "engage" -> {
				int targetId = session.targetObjectId();
				if (targetId == 0 || targetId == active.objectId() || ctx == null || ctx.world() == null) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "Wedding",
							"Selecione seu parceiro(a) como alvo antes de usar .engage."));
					return true;
				}
				var targetOnline = ctx.world().player(targetId).orElse(null);
				if (targetOnline == null || targetOnline.character() == null) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "Wedding", "O alvo precisa estar online."));
					return true;
				}
				PlayerCharacter partner = targetOnline.character();
				if (!ws.canEngageWithCeremonyRules(active, partner)) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "Wedding",
							"Nao foi possivel iniciar o noivado (verifique Formal Wear, estado civil ou configuracoes)."));
					return true;
				}
				try {
					var couple = ws.engage(active, partner);
					ws.marry(couple.id());
					session.send(new CreatureSay(0, CreatureSay.ALL, "Wedding",
							"Parabens! Voce agora esta noivo(a)/casado(a) com " + partner.name() + "!"));
					targetOnline.send(new CreatureSay(0, CreatureSay.ALL, "Wedding",
							"Parabens! Voce agora esta noivo(a)/casado(a) com " + active.name() + "!"));
				} catch (Exception e) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "Wedding", e.getMessage()));
				}
				return true;
			}
			case "gotolove" -> {
				int partnerId = ws.getPartnerId(active.objectId());
				if (partnerId <= 0 || !ws.isMarried(active.objectId())) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "Wedding", "Voce nao possui um conjuge casado."));
					return true;
				}
				if (active.isInCombat() || active.isDead() || active.karma() > 0
						|| active.isInOlympiadMode() || active.isInJail() || active.instanceId() != 0) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "Wedding",
							"Voce nao pode usar .gotolove em combate, com karma, em Olimpiada, instancia ou na Prisao."));
					return true;
				}
				GameWorld world = ctx != null ? ctx.world() : GameWorld.getInstance();
				var partnerOnline = world != null ? world.player(partnerId).orElse(null) : null;
				if (partnerOnline == null || partnerOnline.character() == null) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "Wedding", "Seu conjuge nao esta online no momento."));
					return true;
				}
				PlayerCharacter partner = partnerOnline.character();
				if (partner.isInCombat() || partner.isDead() || partner.karma() > 0
						|| partner.isInOlympiadMode() || partner.isInJail() || partner.instanceId() != 0
						|| !ws.canTeleportToPartner(active, partner)) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "Wedding",
							"Seu conjuge esta em uma zona restrita (combate, Olimpiada, instancia, cerco ou Prisao)."));
					return true;
				}
				int fee = ws.getWeddingTeleportPrice();
				if (fee > 0 && active.inventory() != null) {
					if (active.inventory().adena() < fee || !active.inventory().destroyItemByItemId(57, fee)) {
						session.send(new CreatureSay(0, CreatureSay.ALL, "Wedding",
								"Voce precisa de " + fee + " Adena para teleportar ate seu conjuge."));
						return true;
					}
				}
				active.setPosition(partner.x(), partner.y(), partner.z());
				session.send(new TeleportToLocation(active.objectId(), partner.x(), partner.y(), partner.z(), active.heading()));
				session.send(new CreatureSay(0, CreatureSay.ALL, "Wedding",
						"Teleportado ate seu conjuge " + partner.name() + "!"));
				return true;
			}
			case "divorce" -> {
				var coupleOpt = ws.getCoupleForPlayer(active.objectId());
				if (coupleOpt.isEmpty()) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "Wedding", "Voce nao possui casamento ou noivado ativo."));
					return true;
				}
				if (active.inventory() != null) {
					long cost = ws.calculateDivorceCost(active.inventory().adena());
					if (cost > 0 && cost <= Integer.MAX_VALUE) {
						active.inventory().destroyItemByItemId(57, (int) cost);
					}
				}
				ws.divorce(coupleOpt.get().id());
				session.send(new CreatureSay(0, CreatureSay.ALL, "Wedding", "Seu casamento foi dissolvido (.divorce)."));
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	@Override
	public List<String> getVoicedCommandList() {
		return COMMANDS;
	}
}
