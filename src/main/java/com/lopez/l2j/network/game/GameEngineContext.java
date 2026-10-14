package com.lopez.l2j.network.game;

import com.lopez.l2j.config.ServerProperties;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Service Locator centralizado do motor de jogo baseado no {@link ApplicationContext} do Spring.
 * Elimina a necessidade de expandir o construtor gigante de {@link GameSession.Context} a cada novo
 * serviço adicionado, permitindo lookup tipado sob demanda via {@link #getBean(Class)}.
 */
@Component
public class GameEngineContext implements ApplicationContextAware {

	private static volatile ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext ctx) throws BeansException {
		applicationContext = ctx;
		GameSession.Context.setApplicationContext(ctx);
	}

	public static ApplicationContext context() {
		return applicationContext;
	}

	public static <T> T getBean(Class<T> type) {
		ApplicationContext ctx = applicationContext;
		if (ctx == null || type == null) {
			return null;
		}
		try {
			return ctx.getBean(type);
		} catch (BeansException e) {
			return null;
		}
	}

	public static GameSession.Context buildSessionContext(ApplicationContext appCtx, ServerProperties props) {
		setStaticContext(appCtx);
		int protoMin = props != null && props.network() != null ? props.network().protocolMin() : 740;
		int protoMax = props != null && props.network() != null ? props.network().protocolMax() : 746;
		java.util.List<String> superusers = props != null && props.admin() != null && props.admin().superusers() != null
				? props.admin().superusers()
				: java.util.List.of();
		ServerProperties.Rates rates = props != null ? props.rates() : null;
		String serverName = props != null && props.serverName() != null ? props.serverName() : "L2JLopez";

		return new GameSession.Context(
				protoMin,
				protoMax,
				bean(appCtx, com.lopez.l2j.network.session.SessionKeyRegistry.class),
				bean(appCtx, com.lopez.l2j.game.service.CharacterService.class),
				bean(appCtx, com.lopez.l2j.game.service.InventoryService.class),
				bean(appCtx, com.lopez.l2j.game.world.GameWorld.class),
				bean(appCtx, com.lopez.l2j.game.html.HtmCache.class),
				bean(appCtx, com.lopez.l2j.game.teleport.TeleportLocationTable.class),
				bean(appCtx, com.lopez.l2j.game.trade.BuyListTable.class),
				bean(appCtx, com.lopez.l2j.game.combat.CombatService.class),
				bean(appCtx, com.lopez.l2j.game.drop.DropService.class),
				bean(appCtx, com.lopez.l2j.game.shortcut.ShortCutRepository.class),
				bean(appCtx, com.lopez.l2j.game.skill.SkillRepository.class),
				bean(appCtx, com.lopez.l2j.game.ai.NpcAiService.class),
				bean(appCtx, com.lopez.l2j.game.skill.SkillService.class),
				bean(appCtx, com.lopez.l2j.game.multisell.MultiSellTable.class),
				bean(appCtx, com.lopez.l2j.game.service.WarehouseService.class),
				bean(appCtx, com.lopez.l2j.game.effect.CharacterSkillSaveRepository.class),
				bean(appCtx, com.lopez.l2j.game.npc.SpawnService.class),
				superusers,
				bean(appCtx, com.lopez.l2j.game.mapregion.MapRegionTable.class),
				bean(appCtx, com.lopez.l2j.game.announcements.Announcements.class),
				bean(appCtx, com.lopez.l2j.game.door.DoorTable.class),
				bean(appCtx, com.lopez.l2j.game.item.ArmorSetsTable.class),
				bean(appCtx, com.lopez.l2j.game.clan.ClanTable.class),
				bean(appCtx, com.lopez.l2j.game.clan.CrestCache.class),
				bean(appCtx, com.lopez.l2j.game.henna.HennaTable.class),
				bean(appCtx, com.lopez.l2j.game.henna.HennaTreeTable.class),
				bean(appCtx, com.lopez.l2j.game.zone.ZoneTable.class),
				bean(appCtx, com.lopez.l2j.game.castle.CastleManager.class),
				bean(appCtx, com.lopez.l2j.game.cursed.CursedWeaponsManager.class),
				bean(appCtx, com.lopez.l2j.game.olympiad.OlympiadManager.class),
				bean(appCtx, com.lopez.l2j.game.sevensigns.SevenSignsManager.class),
				bean(appCtx, com.lopez.l2j.game.manor.CastleManorManager.class),
				bean(appCtx, com.lopez.l2j.game.chat.WordFilterTable.class),
				bean(appCtx, com.lopez.l2j.game.staticobject.StaticObjectTable.class),
				bean(appCtx, com.lopez.l2j.game.item.GroundItemService.class),
				bean(appCtx, com.lopez.l2j.game.social.CharacterRecommendationService.class),
				bean(appCtx, com.lopez.l2j.game.social.FriendListService.class),
				bean(appCtx, com.lopez.l2j.game.social.WeddingService.class),
				bean(appCtx, com.lopez.l2j.game.dressme.DressMeService.class),
				bean(appCtx, com.lopez.l2j.game.buffshop.BuffShopService.class),
				bean(appCtx, com.lopez.l2j.game.augmentation.AugmentationService.class),
				bean(appCtx, com.lopez.l2j.game.fishing.FishingService.class),
				bean(appCtx, com.lopez.l2j.game.summon.SummonItemService.class),
				bean(appCtx, com.lopez.l2j.game.extractable.ExtractableItemService.class),
				bean(appCtx, com.lopez.l2j.game.clan.alliance.AllianceService.class),
				bean(appCtx, com.lopez.l2j.game.clan.war.ClanWarService.class),
				bean(appCtx, com.lopez.l2j.game.clan.skill.ClanSkillService.class),
				bean(appCtx, com.lopez.l2j.game.clan.clanhall.ClanHallService.class),
				bean(appCtx, com.lopez.l2j.game.clan.clanhall.ClanHallFunctionService.class),
				bean(appCtx, com.lopez.l2j.game.clan.clanhall.siege.ClanHallSiegeService.class),
				bean(appCtx, com.lopez.l2j.game.clan.privilege.ClanPrivilegeService.class),
				bean(appCtx, com.lopez.l2j.game.castle.siege.SiegeService.class),
				bean(appCtx, com.lopez.l2j.game.castle.crown.CrownService.class),
				bean(appCtx, com.lopez.l2j.game.castle.mercenary.MercenaryService.class),
				bean(appCtx, com.lopez.l2j.game.castle.reward.SiegeRewardService.class),
				bean(appCtx, com.lopez.l2j.game.fortress.FortressService.class),
				bean(appCtx, com.lopez.l2j.game.fortress.siege.FortressSiegeService.class),
				bean(appCtx, com.lopez.l2j.game.instance.frintezza.FrintezzaService.class),
				bean(appCtx, com.lopez.l2j.game.instance.foursepulchers.FourSepulchersService.class),
				bean(appCtx, com.lopez.l2j.game.instance.dimensionalrift.DimensionalRiftService.class),
				bean(appCtx, com.lopez.l2j.game.community.CommunityBoardService.class),
				bean(appCtx, com.lopez.l2j.game.offlinetrade.OfflineTradeService.class),
				bean(appCtx, com.lopez.l2j.game.autofarm.AutoFarmService.class),
				bean(appCtx, com.lopez.l2j.game.achievements.AchievementsService.class),
				bean(appCtx, com.lopez.l2j.game.arenaduel.ArenaDuelService.class),
				bean(appCtx, com.lopez.l2j.game.event.official.OfficialEventService.class),
				bean(appCtx, com.lopez.l2j.game.roulette.RouletteService.class),
				bean(appCtx, com.lopez.l2j.game.reset.CharacterResetService.class),
				bean(appCtx, com.lopez.l2j.game.event.pvp.TvtEventService.class),
				bean(appCtx, com.lopez.l2j.game.event.pvp.CtfEventService.class),
				bean(appCtx, com.lopez.l2j.game.event.pvp.DmEventService.class),
				bean(appCtx, com.lopez.l2j.game.event.partyfarm.PartyFarmEventService.class),
				bean(appCtx, com.lopez.l2j.game.service.BotsPreventionService.class),
				bean(appCtx, com.lopez.l2j.game.service.PvPColorService.class),
				bean(appCtx, com.lopez.l2j.game.service.PvPRankService.class),
				bean(appCtx, com.lopez.l2j.game.service.AioService.class),
				bean(appCtx, com.lopez.l2j.game.service.PlayerPreferencesService.class),
				bean(appCtx, com.lopez.l2j.game.service.LotteryService.class),
				bean(appCtx, com.lopez.l2j.game.service.MonsterRaceService.class),
				bean(appCtx, com.lopez.l2j.game.fishing.FishingChampionshipService.class),
				bean(appCtx, com.lopez.l2j.game.service.PetitionService.class),
				bean(appCtx, com.lopez.l2j.game.service.BoatService.class),
				bean(appCtx, com.lopez.l2j.game.event.official.L2DayEventService.class),
				bean(appCtx, com.lopez.l2j.game.service.StarterKitService.class),
				bean(appCtx, com.lopez.l2j.game.quest.QuestManager.class),
				bean(appCtx, com.lopez.l2j.game.subclass.SubClassService.class),
				bean(appCtx, com.lopez.l2j.game.macro.MacroRepository.class),
				bean(appCtx, com.lopez.l2j.game.boss.RaidPointsService.class),
				bean(appCtx, com.lopez.l2j.network.game.security.BypassEncoderService.class),
				bean(appCtx, com.lopez.l2j.network.game.security.PacketRateLimiter.class),
				bean(appCtx, com.lopez.l2j.game.service.CharacterVariablesService.class),
				bean(appCtx, com.lopez.l2j.network.game.handler.admin.AdminCommandHandlerRegistry.class),
				bean(appCtx, com.lopez.l2j.network.game.handler.voiced.VoicedCommandHandlerRegistry.class),
				bean(appCtx, com.lopez.l2j.network.game.handler.bypass.BypassHandlerRegistry.class),
				bean(appCtx, com.lopez.l2j.game.service.AcpService.class),
				bean(appCtx, com.lopez.l2j.game.service.SchemeBufferService.class),
				bean(appCtx, com.lopez.l2j.game.service.BankingService.class),
				bean(appCtx, com.lopez.l2j.game.service.AwayStatusService.class),
				bean(appCtx, com.lopez.l2j.game.service.HeroService.class),
				bean(appCtx, com.lopez.l2j.game.service.VipService.class),
				rates,
				serverName
		);
	}

	private static void setStaticContext(ApplicationContext appCtx) {
		if (appCtx != null) {
			applicationContext = appCtx;
			GameSession.Context.setApplicationContext(appCtx);
		}
	}

	private static <T> T bean(ApplicationContext ctx, Class<T> type) {
		if (ctx == null) {
			return null;
		}
		try {
			return ctx.getBean(type);
		} catch (BeansException e) {
			return null;
		}
	}
}
