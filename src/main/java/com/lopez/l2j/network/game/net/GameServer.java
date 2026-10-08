package com.lopez.l2j.network.game.net;

import com.lopez.l2j.config.ServerProperties;
import com.lopez.l2j.game.service.CharacterService;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.game.html.HtmCache;
import com.lopez.l2j.game.teleport.TeleportLocationTable;
import com.lopez.l2j.game.trade.BuyListTable;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.crypt.GameCrypt;
import com.lopez.l2j.network.session.SessionKeyRegistry;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * Servidor TCP do jogo (porta {@code l2.network.game-port}). Uma virtual thread por conexao. So sobe com
 * {@code l2.game.listen=true}.
 */
@Component
@ConditionalOnProperty(prefix = "l2.game", name = "listen", havingValue = "true")
public class GameServer implements SmartLifecycle {

	private static final Logger log = LoggerFactory.getLogger(GameServer.class);
	static final int MAX_CONNECTIONS = 2000;

	private final int port;
	private final GameSession.Context context;
	private final SecureRandom random = new SecureRandom();
	private final Semaphore slots = new Semaphore(MAX_CONNECTIONS);
	private volatile ServerSocket serverSocket;
	private volatile ExecutorService executor;
	private volatile boolean running;

	@Autowired
	public GameServer(ServerProperties p, SessionKeyRegistry sessionKeys, CharacterService characters,
			InventoryService inventories, GameWorld world, HtmCache htmls, TeleportLocationTable teleports,
			BuyListTable buylists, com.lopez.l2j.game.combat.CombatService combat,
			com.lopez.l2j.game.drop.DropService drops,
			com.lopez.l2j.game.shortcut.ShortCutRepository shortcuts,
			com.lopez.l2j.game.skill.SkillRepository skills,
			com.lopez.l2j.game.ai.NpcAiService npcAi,
			com.lopez.l2j.game.skill.SkillService skillService,
			@Autowired(required = false) com.lopez.l2j.game.multisell.MultiSellTable multisell,
			@Autowired(required = false) com.lopez.l2j.game.service.WarehouseService warehouse,
			@Autowired(required = false) com.lopez.l2j.game.effect.CharacterSkillSaveRepository buffRepository,
			@Autowired(required = false) com.lopez.l2j.game.npc.SpawnService spawns,
			@Autowired(required = false) com.lopez.l2j.game.mapregion.MapRegionTable mapRegions,
			@Autowired(required = false) com.lopez.l2j.game.announcements.Announcements announcements,
			@Autowired(required = false) com.lopez.l2j.game.door.DoorTable doors,
			@Autowired(required = false) com.lopez.l2j.game.item.ArmorSetsTable armorSets,
			@Autowired(required = false) com.lopez.l2j.game.clan.ClanTable clans,
			@Autowired(required = false) com.lopez.l2j.game.clan.CrestCache crests,
			@Autowired(required = false) com.lopez.l2j.game.henna.HennaTable hennas,
			@Autowired(required = false) com.lopez.l2j.game.henna.HennaTreeTable hennaTrees,
			@Autowired(required = false) com.lopez.l2j.game.zone.ZoneTable zones,
			@Autowired(required = false) com.lopez.l2j.game.castle.CastleManager castles,
			@Autowired(required = false) com.lopez.l2j.game.cursed.CursedWeaponsManager cursedWeapons,
			@Autowired(required = false) com.lopez.l2j.game.olympiad.OlympiadManager olympiad,
			@Autowired(required = false) com.lopez.l2j.game.sevensigns.SevenSignsManager sevenSigns,
			@Autowired(required = false) com.lopez.l2j.game.manor.CastleManorManager manor,
			@Autowired(required = false) com.lopez.l2j.game.chat.WordFilterTable wordFilter,
			@Autowired(required = false) com.lopez.l2j.game.staticobject.StaticObjectTable staticObjects,
			@Autowired(required = false) com.lopez.l2j.game.item.GroundItemService groundItems,
			@Autowired(required = false) com.lopez.l2j.game.social.CharacterRecommendationService recommendations,
			@Autowired(required = false) com.lopez.l2j.game.social.FriendListService friends,
			@Autowired(required = false) com.lopez.l2j.game.social.WeddingService weddings,
			@Autowired(required = false) com.lopez.l2j.game.dressme.DressMeService dressMe,
			@Autowired(required = false) com.lopez.l2j.game.buffshop.BuffShopService buffShop,
			@Autowired(required = false) com.lopez.l2j.game.augmentation.AugmentationService augmentation,
			@Autowired(required = false) com.lopez.l2j.game.fishing.FishingService fishing,
			@Autowired(required = false) com.lopez.l2j.game.summon.SummonItemService summonItems,
			@Autowired(required = false) com.lopez.l2j.game.extractable.ExtractableItemService extractableItems,
			@Autowired(required = false) com.lopez.l2j.game.clan.alliance.AllianceService alliances,
			@Autowired(required = false) com.lopez.l2j.game.clan.war.ClanWarService clanWars,
			@Autowired(required = false) com.lopez.l2j.game.clan.skill.ClanSkillService clanSkills,
			@Autowired(required = false) com.lopez.l2j.game.clan.clanhall.ClanHallService clanHalls,
			@Autowired(required = false) com.lopez.l2j.game.clan.clanhall.ClanHallFunctionService clanHallFunctions,
			@Autowired(required = false) com.lopez.l2j.game.clan.clanhall.siege.ClanHallSiegeService clanHallSieges,
			@Autowired(required = false) com.lopez.l2j.game.clan.privilege.ClanPrivilegeService clanPrivileges,
			@Autowired(required = false) com.lopez.l2j.game.castle.siege.SiegeService sieges,
			@Autowired(required = false) com.lopez.l2j.game.castle.crown.CrownService crowns,
			@Autowired(required = false) com.lopez.l2j.game.castle.mercenary.MercenaryService mercenaries,
			@Autowired(required = false) com.lopez.l2j.game.castle.reward.SiegeRewardService siegeRewards,
			@Autowired(required = false) com.lopez.l2j.game.fortress.FortressService fortresses,
			@Autowired(required = false) com.lopez.l2j.game.fortress.siege.FortressSiegeService fortressSieges,
			@Autowired(required = false) com.lopez.l2j.game.instance.frintezza.FrintezzaService frintezza,
			@Autowired(required = false) com.lopez.l2j.game.instance.foursepulchers.FourSepulchersService fourSepulchers,
			@Autowired(required = false) com.lopez.l2j.game.instance.dimensionalrift.DimensionalRiftService dimensionalRift,
			@Autowired(required = false) com.lopez.l2j.game.community.CommunityBoardService communityBoard,
			@Autowired(required = false) com.lopez.l2j.game.offlinetrade.OfflineTradeService offlineTrade,
			@Autowired(required = false) com.lopez.l2j.game.autofarm.AutoFarmService autoFarm,
			@Autowired(required = false) com.lopez.l2j.game.achievements.AchievementsService achievements,
			@Autowired(required = false) com.lopez.l2j.game.arenaduel.ArenaDuelService arenaDuel,
			@Autowired(required = false) com.lopez.l2j.game.event.official.OfficialEventService officialEvent,
			@Autowired(required = false) com.lopez.l2j.game.roulette.RouletteService roulette,
			@Autowired(required = false) com.lopez.l2j.game.reset.CharacterResetService characterReset,
			@Autowired(required = false) com.lopez.l2j.game.event.pvp.TvtEventService tvt,
			@Autowired(required = false) com.lopez.l2j.game.event.pvp.CtfEventService ctf,
			@Autowired(required = false) com.lopez.l2j.game.event.pvp.DmEventService dm,
			@Autowired(required = false) com.lopez.l2j.game.event.partyfarm.PartyFarmEventService partyFarm,
			@Autowired(required = false) com.lopez.l2j.game.service.BotsPreventionService botsPrevention,
			@Autowired(required = false) com.lopez.l2j.game.service.PvPColorService pvpColor,
			@Autowired(required = false) com.lopez.l2j.game.service.PvPRankService pvpRank,
			@Autowired(required = false) com.lopez.l2j.game.service.AioService aio,
			@Autowired(required = false) com.lopez.l2j.game.service.PlayerPreferencesService preferences,
			@Autowired(required = false) com.lopez.l2j.game.service.LotteryService lottery,
			@Autowired(required = false) com.lopez.l2j.game.service.MonsterRaceService monsterRace,
			@Autowired(required = false) com.lopez.l2j.game.fishing.FishingChampionshipService fishingChampionship,
			@Autowired(required = false) com.lopez.l2j.game.service.PetitionService petition,
			@Autowired(required = false) com.lopez.l2j.game.service.BoatService boat,
			@Autowired(required = false) com.lopez.l2j.game.event.official.L2DayEventService l2day,
			@Autowired(required = false) com.lopez.l2j.game.service.StarterKitService starterKit,
			@Autowired(required = false) com.lopez.l2j.game.quest.QuestManager questManager,
			@Autowired(required = false) com.lopez.l2j.game.subclass.SubClassService subClasses,
			@Autowired(required = false) com.lopez.l2j.game.macro.MacroRepository macros,
			@Autowired(required = false) com.lopez.l2j.game.boss.RaidPointsService raidPoints,
			@Autowired(required = false) com.lopez.l2j.network.game.security.BypassEncoderService bypassEncoder,
			@Autowired(required = false) com.lopez.l2j.network.game.security.PacketRateLimiter packetRateLimiter,
			@Autowired(required = false) com.lopez.l2j.game.service.CharacterVariablesService characterVariables,
			@Autowired(required = false) com.lopez.l2j.network.game.handler.admin.AdminCommandHandlerRegistry adminCommands,
			@Autowired(required = false) com.lopez.l2j.network.game.handler.voiced.VoicedCommandHandlerRegistry voicedCommands,
			@Autowired(required = false) com.lopez.l2j.network.game.handler.bypass.BypassHandlerRegistry bypassHandlers,
			@Autowired(required = false) com.lopez.l2j.game.service.AcpService acpService,
			@Autowired(required = false) com.lopez.l2j.game.service.SchemeBufferService schemeBuffer,
			@Autowired(required = false) com.lopez.l2j.game.service.BankingService banking,
			@Autowired(required = false) com.lopez.l2j.game.service.AwayStatusService away) {
		this(p.network().gamePort(), new GameSession.Context(p.network().protocolMin(), p.network().protocolMax(),
				sessionKeys, characters, inventories, world, htmls, teleports, buylists, combat, drops, shortcuts, skills, npcAi,
				skillService, multisell, warehouse, buffRepository, spawns, p.admin().superusers(), mapRegions, announcements,
				doors, armorSets, clans, crests, hennas, hennaTrees, zones, castles, cursedWeapons, olympiad, sevenSigns, manor,
				wordFilter, staticObjects, groundItems, recommendations, friends, weddings, dressMe, buffShop, augmentation, fishing,
				summonItems, extractableItems, alliances, clanWars, clanSkills, clanHalls, clanHallFunctions, clanHallSieges, clanPrivileges,
				sieges, crowns, mercenaries, siegeRewards, fortresses, fortressSieges,
				frintezza, fourSepulchers, dimensionalRift, communityBoard, offlineTrade, autoFarm,
				achievements, arenaDuel, officialEvent, roulette, characterReset,
				tvt, ctf, dm, partyFarm, botsPrevention, pvpColor, pvpRank, aio, preferences,
				lottery, monsterRace, fishingChampionship, petition, boat, l2day, starterKit, questManager, subClasses,
				macros, raidPoints, bypassEncoder, packetRateLimiter, characterVariables, adminCommands, voicedCommands, bypassHandlers,
				acpService, schemeBuffer, banking, away,
				p.rates(), p.serverName()));
	}

	/** Porta 0 = efemera (testes); use {@link #port()} depois de iniciar. */
	public GameServer(int port, GameSession.Context context) {
		this.port = port;
		this.context = context;
	}

	public int port() {
		ServerSocket s = serverSocket;
		return s == null ? port : s.getLocalPort();
	}

	@Override
	public synchronized void start() {
		if (running) {
			return;
		}
		try {
			ServerSocket s = new ServerSocket();
			s.setReuseAddress(true);
			s.bind(new InetSocketAddress(port));
			serverSocket = s;
		} catch (IOException e) {
			throw new IllegalStateException("Nao foi possivel abrir a porta do game server " + port, e);
		}
		executor = Executors.newVirtualThreadPerTaskExecutor();
		running = true;
		Thread.ofPlatform().name("game-acceptor").daemon(true).start(this::acceptLoop);
		log.info("Game server escutando na porta {} (protocolos {}-{})", serverSocket.getLocalPort(),
				context.protocolMin(), context.protocolMax());
	}

	private void acceptLoop() {
		while (running) {
			Socket client;
			try {
				client = serverSocket.accept();
			} catch (IOException e) {
				if (running) {
					log.warn("Falha no accept do game server", e);
				}
				return;
			}
			if (!slots.tryAcquire()) {
				log.warn("Limite de {} conexoes de jogo atingido; recusando {}", MAX_CONNECTIONS,
						client.getRemoteSocketAddress());
				closeQuietly(client);
				continue;
			}
			try {
				client.setTcpNoDelay(true);
				client.setKeepAlive(true);
				String ip = client.getInetAddress().getHostAddress();
				byte[] key = GameCrypt.newKey(random);
				var connection = new GameConnection(client, key,
						(sink, closer) -> new GameSession(context, key, ip, sink, closer),
						slots::release);
				executor.execute(connection);
			} catch (IOException | RuntimeException e) {
				log.warn("Falha ao aceitar conexao de jogo", e);
				closeQuietly(client);
				slots.release();
			}
		}
	}

	@Override
	public synchronized void stop() {
		running = false;
		closeQuietly(serverSocket);
		ExecutorService ex = executor;
		if (ex != null) {
			ex.shutdownNow();
		}
		log.info("Game server parado");
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	private static void closeQuietly(java.io.Closeable c) {
		try {
			if (c != null) {
				c.close();
			}
		} catch (IOException ignored) {
			// nada a fazer
		}
	}
}
