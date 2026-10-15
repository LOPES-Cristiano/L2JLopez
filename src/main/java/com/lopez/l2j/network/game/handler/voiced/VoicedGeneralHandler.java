package com.lopez.l2j.network.game.handler.voiced;

import com.lopez.l2j.game.achievements.AchievementsService;
import com.lopez.l2j.game.arenaduel.ArenaDuelService;
import com.lopez.l2j.game.dressme.DressMeData;
import com.lopez.l2j.game.dressme.DressMeService;
import com.lopez.l2j.game.event.pvp.CtfEventService;
import com.lopez.l2j.game.event.pvp.DmEventService;
import com.lopez.l2j.game.event.pvp.TvtEventService;
import com.lopez.l2j.game.item.ItemInstance;
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
import com.lopez.l2j.game.tournament.TournamentService;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.handler.bypass.BypassEventsAndPveHandler;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.ItemList;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage;
import com.lopez.l2j.network.game.packet.GameServerPacket.ServerClose;
import com.lopez.l2j.network.game.packet.GameServerPacket.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Handler modular para comandos de voz gerais:
 * .online, .stats, .menu, .blockbuff, .bank, .deposit, .withdraw, .away, .back, .tournament,
 * .tvt, .ctf, .dm, .arena, .dungeon, .solofarm, .dressme, .roulette, .reset, .achievements, .aiomenu, .offline
 */
@Component
public class VoicedGeneralHandler implements IVoicedCommandHandler {

	private static final List<String> COMMANDS = List.of(
			"online",
			"stats",
			"menu",
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
			"ctf",
			"ctfjoin",
			"ctfleave",
			"dm",
			"dmjoin",
			"dmleave",
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
				int count = ctx != null && ctx.world() != null ? ctx.world().players().size() : 0;
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Jogadores online: " + count));
				return true;
			}
			case "stats" -> {
				var t = ctx != null && ctx.characters() != null ? ctx.characters().template(active) : null;
				var stats = PlayerStats.calculate(active, t);
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						String.format("%s (Nv %d): P.Atk %d, M.Atk %d, P.Def %d, M.Def %d, AtkSpd %d, CastSpd %d",
								active.name(), active.level(), stats.pAtk(), stats.mAtk(), stats.pDef(), stats.mDef(),
								stats.pAtkSpd(), stats.mAtkSpd())));
				return true;
			}
			case "menu" -> {
				if (ctx != null && ctx.preferences() != null) {
					session.send(new NpcHtmlMessage(0, ctx.preferences().buildMenuHtml(active.objectId(), active.name())));
				} else {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Menu de preferencias indisponivel."));
				}
				return true;
			}
			case "blockbuff" -> {
				if (ctx != null && ctx.preferences() != null) {
					boolean blocked = ctx.preferences().toggleBlockBuffs(active.objectId());
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Bloqueio de buffs: " + (blocked ? "ATIVADO" : "DESATIVADO")));
				}
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
				bankingService.deposit(session, count);
				return true;
			}
			case "withdraw" -> {
				int count = 1;
				if (!cleanParams.isEmpty()) {
					try {
						count = Math.max(1, Integer.parseInt(cleanParams));
					} catch (NumberFormatException ignored) {}
				}
				bankingService.withdraw(session, count);
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
			case "tvt", "tvtjoin", "tvtleave" -> {
				TvtEventService tvt = ctx != null && ctx.tvt() != null ? ctx.tvt() : new TvtEventService();
				if (lower.equals("tvtjoin") || lowerParams.equals("join")) {
					var res = tvt.register(active);
					session.send(new CreatureSay(0, CreatureSay.ALL, "TvT", "TvT: " + res.name()));
				} else if (lower.equals("tvtleave") || lowerParams.equals("leave")) {
					boolean ok = tvt.unregister(active);
					session.send(new CreatureSay(0, CreatureSay.ALL, "TvT", ok ? "Inscricao no TvT cancelada." : "Voce nao esta inscrito no TvT."));
				}
				session.send(new NpcHtmlMessage(0, BypassEventsAndPveHandler.buildTvtHtml(tvt)));
				return true;
			}
			case "ctf", "ctfjoin", "ctfleave" -> {
				CtfEventService ctf = ctx != null && ctx.ctf() != null ? ctx.ctf() : new CtfEventService();
				if (lower.equals("ctfjoin") || lowerParams.equals("join")) {
					var res = ctf.register(active);
					session.send(new CreatureSay(0, CreatureSay.ALL, "CTF", "CTF: " + res.name()));
				} else if (lower.equals("ctfleave") || lowerParams.equals("leave")) {
					boolean ok = ctf.unregister(active);
					session.send(new CreatureSay(0, CreatureSay.ALL, "CTF", ok ? "Inscricao no CTF cancelada." : "Voce nao esta inscrito no CTF."));
				}
				session.send(new NpcHtmlMessage(0, BypassEventsAndPveHandler.buildCtfHtml(ctf)));
				return true;
			}
			case "dm", "dmjoin", "dmleave" -> {
				DmEventService dm = ctx != null && ctx.dm() != null ? ctx.dm() : new DmEventService();
				if (lower.equals("dmjoin") || lowerParams.equals("join")) {
					var res = dm.register(active);
					session.send(new CreatureSay(0, CreatureSay.ALL, "DM", "DM: " + res.name()));
				} else if (lower.equals("dmleave") || lowerParams.equals("leave")) {
					boolean ok = dm.unregister(active);
					session.send(new CreatureSay(0, CreatureSay.ALL, "DM", ok ? "Inscricao no DM cancelada." : "Voce nao esta inscrito no DM."));
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
						+ "<br></center></body></html>";
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
			case "offline" -> {
				OfflineTradeService offlineSvc = ctx != null && ctx.offlineTrade() != null ? ctx.offlineTrade() : new OfflineTradeService();
				List<OfflineShopItem> items = new ArrayList<>();
				items.add(new OfflineShopItem(57, 1, 1));
				int storeMode = active.privateStoreType() != 0 ? active.privateStoreType() : OfflineTradeService.STORE_PRIVATE_SELL;
				boolean ok = offlineSvc.startOfflineTrade(
						active,
						storeMode,
						false,
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

	@Override
	public List<String> getVoicedCommandList() {
		return COMMANDS;
	}
}
