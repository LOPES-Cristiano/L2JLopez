package com.lopez.l2j.network.game;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.door.DoorInstance;
import com.lopez.l2j.game.item.ItemSlots;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.npc.SpawnService;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.subclass.SubClass;
import com.lopez.l2j.game.subclass.SubClassService;
import com.lopez.l2j.game.template.CharTemplate;
import com.lopez.l2j.game.boss.RaidPointsService;
import com.lopez.l2j.network.game.packet.GameServerPacket.ExGetBossRecord;
import com.lopez.l2j.game.service.CharacterService;
import com.lopez.l2j.game.service.CharacterService.CreateRequest;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.game.service.InventoryService.EquipResult;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameClientPacket.Action;
import com.lopez.l2j.network.game.packet.GameClientPacket.AuthLogin;
import com.lopez.l2j.network.game.packet.GameClientPacket.AttackRequest;
import com.lopez.l2j.network.game.packet.GameClientPacket.CharacterCreate;
import com.lopez.l2j.network.game.packet.GameClientPacket.CharacterDelete;
import com.lopez.l2j.network.game.packet.GameClientPacket.CharacterRestore;
import com.lopez.l2j.network.game.packet.GameClientPacket.CharacterSelect;
import com.lopez.l2j.network.game.packet.GameClientPacket.EnterWorld;
import com.lopez.l2j.network.game.packet.GameClientPacket.Logout;
import com.lopez.l2j.network.game.packet.GameClientPacket.MoveBackwardToLocation;
import com.lopez.l2j.network.game.packet.GameClientPacket.NewCharacter;
import com.lopez.l2j.network.game.packet.GameClientPacket.ProtocolVersion;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestActionUse;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestItemList;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestManorList;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestQuestList;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestRestart;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestSkillList;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestTargetCancel;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestTutorialLinkHtml;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestTutorialPassCmdToServer;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestTutorialQuestionMark;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestTutorialClientEvent;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestUnEquipItem;
import com.lopez.l2j.network.game.packet.GameClientPacket.Say2;
import com.lopez.l2j.network.game.handler.packet.ActionPacketHandler;
import com.lopez.l2j.network.game.handler.packet.ChatPacketHandler;
import com.lopez.l2j.network.game.handler.packet.ItemPacketHandler;
import com.lopez.l2j.network.game.handler.packet.PartyClanPacketHandler;
import com.lopez.l2j.network.game.handler.packet.TradeStorePacketHandler;
import com.lopez.l2j.network.game.packet.GameClientPacket.SendBypassBuildCmd;
import com.lopez.l2j.network.game.packet.GameClientPacket.State;
import com.lopez.l2j.network.game.packet.GameClientPacket.Unknown;
import com.lopez.l2j.network.game.packet.GameClientPacket.UseItem;
import com.lopez.l2j.network.game.packet.GameClientPacket.ValidatePosition;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.ActionFailed;
import com.lopez.l2j.network.game.packet.GameServerPacket.AutoAttackStart;
import com.lopez.l2j.network.game.packet.GameServerPacket.AutoAttackStop;
import com.lopez.l2j.network.game.packet.GameServerPacket.ChangeMoveType;
import com.lopez.l2j.network.game.packet.GameServerPacket.ChangeWaitType;
import com.lopez.l2j.network.game.packet.GameServerPacket.CharCreateFail;
import com.lopez.l2j.network.game.packet.GameServerPacket.CharCreateOk;
import com.lopez.l2j.network.game.packet.GameServerPacket.CharDeleteSuccess;
import com.lopez.l2j.network.game.packet.GameServerPacket.CharSelected;
import com.lopez.l2j.network.game.packet.GameServerPacket.CharSelectionInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.ChooseInventoryItem;
import com.lopez.l2j.network.game.packet.GameServerPacket.EnchantResult;
import com.lopez.l2j.network.game.packet.GameServerPacket.WareHouseDepositList;
import com.lopez.l2j.network.game.packet.GameServerPacket.WareHouseWithdrawalList;
import com.lopez.l2j.network.game.packet.GameServerPacket.HennaEquipList;
import com.lopez.l2j.network.game.packet.GameServerPacket.HennaItemInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.HennaUnequipList;
import com.lopez.l2j.network.game.packet.GameServerPacket.HennaUnequipInfo;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestEnchantItem;
import com.lopez.l2j.network.game.packet.GameClientPacket.SendWareHouseDepositList;
import com.lopez.l2j.network.game.packet.GameClientPacket.SendWareHouseWithDrawList;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestDestroyItem;
import com.lopez.l2j.game.item.EnchantScrollTable;
import com.lopez.l2j.network.game.packet.GameServerPacket.ClientSetTime;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.EtcStatusUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.ExSendManorList;
import com.lopez.l2j.network.game.packet.GameServerPacket.ExStorageMaxCount;
import com.lopez.l2j.network.game.packet.GameServerPacket.FriendList;
import com.lopez.l2j.network.game.packet.GameServerPacket.HennaInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.InventoryUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.ItemList;
import com.lopez.l2j.network.game.packet.GameServerPacket.KeyPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.LeaveWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket.ServerClose;
import com.lopez.l2j.network.game.packet.GameServerPacket.MoveToLocation;
import com.lopez.l2j.network.game.packet.GameServerPacket.MoveToPawn;
import com.lopez.l2j.network.game.packet.GameServerPacket.MultiSellList;
import com.lopez.l2j.network.game.packet.GameServerPacket.MyTargetSelected;
import com.lopez.l2j.network.game.packet.GameServerPacket.NewCharacterSuccess;
import com.lopez.l2j.network.game.packet.GameServerPacket.QuestList;
import com.lopez.l2j.network.game.packet.GameServerPacket.RestartResponse;
import com.lopez.l2j.network.game.packet.GameServerPacket.Revive;
import com.lopez.l2j.network.game.packet.GameServerPacket.ShortCutInit;
import com.lopez.l2j.network.game.packet.GameServerPacket.ShortCutRegister;
import com.lopez.l2j.network.game.packet.GameServerPacket.ShowMiniMap;
import com.lopez.l2j.network.game.packet.GameServerPacket.SkillList;
import com.lopez.l2j.network.game.packet.GameServerPacket.TeleportToLocation;
import com.lopez.l2j.network.game.packet.GameServerPacket.MagicSkillUse;
import com.lopez.l2j.network.game.packet.GameServerPacket.MagicSkillCanceld;
import com.lopez.l2j.network.game.packet.GameServerPacket.PlaySound;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestShortCutReg;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestShortCutDel;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestMakeMacro;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestDeleteMacro;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestMagicSkillUse;
import com.lopez.l2j.network.game.packet.GameServerPacket.SendMacroList;
import com.lopez.l2j.game.macro.Macro;
import com.lopez.l2j.game.macro.MacroCmd;
import com.lopez.l2j.game.macro.MacroRepository;
import com.lopez.l2j.game.shortcut.ShortCut;
import com.lopez.l2j.game.shortcut.ShortCutRepository;
import com.lopez.l2j.game.skill.Skill;
import com.lopez.l2j.game.skill.SkillRepository;
import com.lopez.l2j.game.skill.SkillService;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.network.game.packet.GameServerPacket.SsqInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.StopMove;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import com.lopez.l2j.network.game.packet.GameServerPacket.TargetUnselected;
import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.html.HtmCache;
import com.lopez.l2j.game.mapregion.MapRegionTable;
import com.lopez.l2j.game.announcements.Announcements;
import com.lopez.l2j.game.door.DoorTable;
import com.lopez.l2j.game.door.DoorInstance;
import com.lopez.l2j.game.item.ArmorSetsTable;
import com.lopez.l2j.game.item.ItemSkillHolder;
import com.lopez.l2j.game.skill.StatFunc;
import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.clan.ClanMember;
import com.lopez.l2j.game.clan.ClanTable;
import com.lopez.l2j.game.clan.CrestCache;
import com.lopez.l2j.game.chat.WordFilterTable;
import com.lopez.l2j.game.staticobject.StaticObjectTable;
import com.lopez.l2j.game.staticobject.StaticObjectInstance;
import com.lopez.l2j.game.item.GroundItemService;
import com.lopez.l2j.network.game.packet.GameServerPacket.StaticObject;
import com.lopez.l2j.network.game.packet.GameServerPacket.ShowTownMap;
import com.lopez.l2j.network.game.packet.GameServerPacket.DropItem;
import com.lopez.l2j.network.game.packet.GameServerPacket.GetItem;
import com.lopez.l2j.network.game.packet.GameServerPacket.PledgeCrest;
import com.lopez.l2j.network.game.packet.GameServerPacket.PledgeShowInfoUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.PledgeShowMemberListAll;
import com.lopez.l2j.network.game.packet.GameServerPacket.PledgeShowMemberListUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.DoorInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.DoorStatusUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.Attack;
import com.lopez.l2j.network.game.packet.GameServerPacket.Die;
import com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.teleport.TeleportLocation;
import com.lopez.l2j.game.teleport.TeleportLocationTable;
import com.lopez.l2j.game.trade.BuyListTable;
import com.lopez.l2j.game.trade.NpcBuyList;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestBuyItem;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestSellItem;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestBypassToServer;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestBBSwrite;
import com.lopez.l2j.network.game.packet.GameServerPacket.BuyList;
import com.lopez.l2j.network.game.packet.GameServerPacket.SellList;
import com.lopez.l2j.network.game.packet.GameServerPacket.ExShowVariationMakeWindow;
import com.lopez.l2j.network.game.packet.GameServerPacket.ExShowVariationCancelWindow;
import com.lopez.l2j.network.game.packet.GameServerPacket.ExPutItemResultForVariationMake;
import com.lopez.l2j.network.game.packet.GameServerPacket.ExPutIntensiveResultForVariationMake;
import com.lopez.l2j.network.game.packet.GameServerPacket.ExPutCommissionResultForVariationMake;
import com.lopez.l2j.network.game.packet.GameServerPacket.ExVariationResult;
import com.lopez.l2j.network.game.packet.GameServerPacket.ExPutItemResultForVariationCancel;
import com.lopez.l2j.network.game.packet.GameServerPacket.ExVariationCancelResult;
import com.lopez.l2j.game.augmentation.Augmentation;
import com.lopez.l2j.game.augmentation.AugmentationService;
import com.lopez.l2j.network.game.packet.GameServerPacket.PrivateStoreItem;
import com.lopez.l2j.network.game.packet.GameServerPacket.PrivateStoreMsgSell;
import com.lopez.l2j.network.game.packet.GameServerPacket.PrivateStoreListSell;
import com.lopez.l2j.network.game.packet.GameServerPacket.PrivateStoreManageListSell;
import com.lopez.l2j.network.game.packet.GameServerPacket.MagicSkillUse;
import com.lopez.l2j.game.buffshop.BuffShopService.BuffShopItem;
import com.lopez.l2j.network.game.packet.GameServerPacket.CharInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.DeleteObject;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.Revive;
import com.lopez.l2j.network.game.packet.GameServerPacket.StatusUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.TeleportToLocation;
import com.lopez.l2j.network.game.packet.GameServerPacket.UserInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.ValidateLocation;
import com.lopez.l2j.network.session.SessionKey;
import com.lopez.l2j.network.session.SessionKeyRegistry;
import com.lopez.l2j.network.game.packet.GameServerPacket.SocialAction;
import com.lopez.l2j.game.model.ExperienceTable;
import com.lopez.l2j.game.model.PlayerStats;
import com.lopez.l2j.game.effect.ConsumableTable;
import com.lopez.l2j.game.effect.ConsumableTable.Consumable;
import com.lopez.l2j.game.effect.PlayerEffects.ActiveBuff;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestAutoSoulShot;
import com.lopez.l2j.network.game.packet.GameServerPacket.ExAutoSoulShot;
import com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.MagicEffectIcons;
import com.lopez.l2j.network.game.packet.GameServerPacket.GMViewCharacterInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.GMViewPledgeInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.GMViewSkillInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.GMViewQuestInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.GMViewItemList;
import com.lopez.l2j.network.game.packet.GameServerPacket.GMHennaInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.GMViewWarehouseWithdrawList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maquina de estados de UMA conexao de jogo (porta enxuta de L2GameClient +
 * clientpackets). Nao conhece
 * sockets: recebe o corpo decifrado e envia respostas pelo {@code sink} (que
 * precisa ser thread-safe, pois
 * outros jogadores tambem enviam por ele - ex.: chat). {@link #handle} deve ser
 * chamado por uma unica
 * thread (a da conexao).
 */
public final class GameSession implements GameWorld.OnlinePlayer {

	private static final Logger log = LoggerFactory.getLogger(GameSession.class);
	static final int MAX_CHAT_LENGTH = 105;

	/** Configuracao/servicos compartilhados por todas as sessoes. */
	public record Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
			CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
			TeleportLocationTable teleports, BuyListTable buylists, CombatService combat,
			com.lopez.l2j.game.drop.DropService drops, ShortCutRepository shortcuts,
			SkillRepository skills, com.lopez.l2j.game.ai.NpcAiService npcAi, SkillService skillService,
			com.lopez.l2j.game.multisell.MultiSellTable multisell,
			com.lopez.l2j.game.service.WarehouseService warehouse,
			com.lopez.l2j.game.effect.CharacterSkillSaveRepository buffRepository,
			SpawnService spawns,
			List<String> adminSuperusers,
			MapRegionTable mapRegions,
			Announcements announcements,
			DoorTable doors,
			ArmorSetsTable armorSets,
			ClanTable clans,
			CrestCache crests,
			com.lopez.l2j.game.henna.HennaTable hennas,
			com.lopez.l2j.game.henna.HennaTreeTable hennaTrees,
			com.lopez.l2j.game.zone.ZoneTable zones,
			com.lopez.l2j.game.castle.CastleManager castles,
			com.lopez.l2j.game.cursed.CursedWeaponsManager cursedWeapons,
			com.lopez.l2j.game.olympiad.OlympiadManager olympiad,
			com.lopez.l2j.game.sevensigns.SevenSignsManager sevenSigns,
			com.lopez.l2j.game.manor.CastleManorManager manor,
			com.lopez.l2j.game.chat.WordFilterTable wordFilter,
			com.lopez.l2j.game.staticobject.StaticObjectTable staticObjects,
			com.lopez.l2j.game.item.GroundItemService groundItems,
			com.lopez.l2j.game.social.CharacterRecommendationService recommendations,
			com.lopez.l2j.game.social.FriendListService friends,
			com.lopez.l2j.game.social.WeddingService weddings,
			com.lopez.l2j.game.dressme.DressMeService dressMe,
			com.lopez.l2j.game.buffshop.BuffShopService buffShop,
			com.lopez.l2j.game.augmentation.AugmentationService augmentation,
			com.lopez.l2j.game.fishing.FishingService fishing,
			com.lopez.l2j.game.summon.SummonItemService summonItems,
			com.lopez.l2j.game.extractable.ExtractableItemService extractableItems,
			com.lopez.l2j.game.clan.alliance.AllianceService alliances,
			com.lopez.l2j.game.clan.war.ClanWarService clanWars,
			com.lopez.l2j.game.clan.skill.ClanSkillService clanSkills,
			com.lopez.l2j.game.clan.clanhall.ClanHallService clanHalls,
			com.lopez.l2j.game.clan.clanhall.ClanHallFunctionService clanHallFunctions,
			com.lopez.l2j.game.clan.clanhall.siege.ClanHallSiegeService clanHallSieges,
			com.lopez.l2j.game.clan.privilege.ClanPrivilegeService clanPrivileges,
			com.lopez.l2j.game.castle.siege.SiegeService sieges,
			com.lopez.l2j.game.castle.crown.CrownService crowns,
			com.lopez.l2j.game.castle.mercenary.MercenaryService mercenaries,
			com.lopez.l2j.game.castle.reward.SiegeRewardService siegeRewards,
			com.lopez.l2j.game.fortress.FortressService fortresses,
			com.lopez.l2j.game.fortress.siege.FortressSiegeService fortressSieges,
			com.lopez.l2j.game.instance.frintezza.FrintezzaService frintezza,
			com.lopez.l2j.game.instance.foursepulchers.FourSepulchersService fourSepulchers,
			com.lopez.l2j.game.instance.dimensionalrift.DimensionalRiftService dimensionalRift,
			com.lopez.l2j.game.community.CommunityBoardService communityBoard,
			com.lopez.l2j.game.offlinetrade.OfflineTradeService offlineTrade,
			com.lopez.l2j.game.autofarm.AutoFarmService autoFarm,
			com.lopez.l2j.game.achievements.AchievementsService achievements,
			com.lopez.l2j.game.arenaduel.ArenaDuelService arenaDuel,
			com.lopez.l2j.game.event.official.OfficialEventService officialEvent,
			com.lopez.l2j.game.roulette.RouletteService roulette,
			com.lopez.l2j.game.reset.CharacterResetService characterReset,
			com.lopez.l2j.game.event.pvp.TvtEventService tvt,
			com.lopez.l2j.game.event.pvp.CtfEventService ctf,
			com.lopez.l2j.game.event.pvp.DmEventService dm,
			com.lopez.l2j.game.event.partyfarm.PartyFarmEventService partyFarm,
			com.lopez.l2j.game.service.BotsPreventionService botsPrevention,
			com.lopez.l2j.game.service.PvPColorService pvpColor,
			com.lopez.l2j.game.service.PvPRankService pvpRank,
			com.lopez.l2j.game.service.AioService aio,
			com.lopez.l2j.game.service.PlayerPreferencesService preferences,
			com.lopez.l2j.game.service.LotteryService lottery,
			com.lopez.l2j.game.service.MonsterRaceService monsterRace,
			com.lopez.l2j.game.fishing.FishingChampionshipService fishingChampionship,
			com.lopez.l2j.game.service.PetitionService petition,
			com.lopez.l2j.game.service.BoatService boat,
			com.lopez.l2j.game.event.official.L2DayEventService l2day,
			com.lopez.l2j.game.service.StarterKitService starterKit,
			com.lopez.l2j.game.quest.QuestManager questManager,
			com.lopez.l2j.game.subclass.SubClassService subClasses,
			com.lopez.l2j.game.macro.MacroRepository macros,
			com.lopez.l2j.game.boss.RaidPointsService raidPoints,
			com.lopez.l2j.network.game.security.BypassEncoderService bypassEncoder,
			com.lopez.l2j.network.game.security.PacketRateLimiter packetRateLimiter,
			com.lopez.l2j.game.service.CharacterVariablesService characterVariables,
			com.lopez.l2j.network.game.handler.admin.AdminCommandHandlerRegistry adminCommands,
			com.lopez.l2j.network.game.handler.voiced.VoicedCommandHandlerRegistry voicedCommands,
			com.lopez.l2j.network.game.handler.bypass.BypassHandlerRegistry bypassHandlers,
			com.lopez.l2j.game.service.AcpService acpService,
			com.lopez.l2j.game.service.SchemeBufferService schemeBuffer,
			com.lopez.l2j.game.service.BankingService banking,
			com.lopez.l2j.game.service.AwayStatusService away,
			com.lopez.l2j.game.service.HeroService hero,
			com.lopez.l2j.game.service.VipService vip,
			com.lopez.l2j.config.ServerProperties.Rates rates, String serverName) {

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat,
				com.lopez.l2j.game.drop.DropService drops, ShortCutRepository shortcuts,
				SkillRepository skills, com.lopez.l2j.game.ai.NpcAiService npcAi, SkillService skillService,
				com.lopez.l2j.game.multisell.MultiSellTable multisell,
				com.lopez.l2j.game.service.WarehouseService warehouse,
				com.lopez.l2j.game.effect.CharacterSkillSaveRepository buffRepository,
				SpawnService spawns,
				List<String> adminSuperusers,
				MapRegionTable mapRegions,
				Announcements announcements,
				DoorTable doors,
				ArmorSetsTable armorSets,
				ClanTable clans,
				CrestCache crests,
				com.lopez.l2j.game.henna.HennaTable hennas,
				com.lopez.l2j.game.henna.HennaTreeTable hennaTrees,
				com.lopez.l2j.game.zone.ZoneTable zones,
				com.lopez.l2j.game.castle.CastleManager castles,
				com.lopez.l2j.game.cursed.CursedWeaponsManager cursedWeapons,
				com.lopez.l2j.game.olympiad.OlympiadManager olympiad,
				com.lopez.l2j.game.sevensigns.SevenSignsManager sevenSigns,
				com.lopez.l2j.game.manor.CastleManorManager manor,
				com.lopez.l2j.game.chat.WordFilterTable wordFilter,
				com.lopez.l2j.game.staticobject.StaticObjectTable staticObjects,
				com.lopez.l2j.game.item.GroundItemService groundItems,
				com.lopez.l2j.game.social.CharacterRecommendationService recommendations,
				com.lopez.l2j.game.social.FriendListService friends,
				com.lopez.l2j.game.social.WeddingService weddings,
				com.lopez.l2j.game.dressme.DressMeService dressMe,
				com.lopez.l2j.game.buffshop.BuffShopService buffShop,
				com.lopez.l2j.game.augmentation.AugmentationService augmentation,
				com.lopez.l2j.game.fishing.FishingService fishing,
				com.lopez.l2j.game.summon.SummonItemService summonItems,
				com.lopez.l2j.game.extractable.ExtractableItemService extractableItems,
				com.lopez.l2j.game.clan.alliance.AllianceService alliances,
				com.lopez.l2j.game.clan.war.ClanWarService clanWars,
				com.lopez.l2j.game.clan.skill.ClanSkillService clanSkills,
				com.lopez.l2j.game.clan.clanhall.ClanHallService clanHalls,
				com.lopez.l2j.game.clan.clanhall.ClanHallFunctionService clanHallFunctions,
				com.lopez.l2j.game.clan.clanhall.siege.ClanHallSiegeService clanHallSieges,
				com.lopez.l2j.game.clan.privilege.ClanPrivilegeService clanPrivileges,
				com.lopez.l2j.game.castle.siege.SiegeService sieges,
				com.lopez.l2j.game.castle.crown.CrownService crowns,
				com.lopez.l2j.game.castle.mercenary.MercenaryService mercenaries,
				com.lopez.l2j.game.castle.reward.SiegeRewardService siegeRewards,
				com.lopez.l2j.game.fortress.FortressService fortresses,
				com.lopez.l2j.game.fortress.siege.FortressSiegeService fortressSieges,
				com.lopez.l2j.game.instance.frintezza.FrintezzaService frintezza,
				com.lopez.l2j.game.instance.foursepulchers.FourSepulchersService fourSepulchers,
				com.lopez.l2j.game.instance.dimensionalrift.DimensionalRiftService dimensionalRift,
				com.lopez.l2j.game.community.CommunityBoardService communityBoard,
				com.lopez.l2j.game.offlinetrade.OfflineTradeService offlineTrade,
				com.lopez.l2j.game.autofarm.AutoFarmService autoFarm,
				com.lopez.l2j.game.achievements.AchievementsService achievements,
				com.lopez.l2j.game.arenaduel.ArenaDuelService arenaDuel,
				com.lopez.l2j.game.event.official.OfficialEventService officialEvent,
				com.lopez.l2j.game.roulette.RouletteService roulette,
				com.lopez.l2j.game.reset.CharacterResetService characterReset,
				com.lopez.l2j.game.event.pvp.TvtEventService tvt,
				com.lopez.l2j.game.event.pvp.CtfEventService ctf,
				com.lopez.l2j.game.event.pvp.DmEventService dm,
				com.lopez.l2j.game.event.partyfarm.PartyFarmEventService partyFarm,
				com.lopez.l2j.game.service.BotsPreventionService botsPrevention,
				com.lopez.l2j.game.service.PvPColorService pvpColor,
				com.lopez.l2j.game.service.PvPRankService pvpRank,
				com.lopez.l2j.game.service.AioService aio,
				com.lopez.l2j.game.service.PlayerPreferencesService preferences,
				com.lopez.l2j.game.service.LotteryService lottery,
				com.lopez.l2j.game.service.MonsterRaceService monsterRace,
				com.lopez.l2j.game.fishing.FishingChampionshipService fishingChampionship,
				com.lopez.l2j.game.service.PetitionService petition,
				com.lopez.l2j.game.service.BoatService boat,
				com.lopez.l2j.game.event.official.L2DayEventService l2day,
				com.lopez.l2j.game.service.StarterKitService starterKit,
				com.lopez.l2j.game.quest.QuestManager questManager,
				com.lopez.l2j.game.subclass.SubClassService subClasses,
				com.lopez.l2j.game.macro.MacroRepository macros,
				com.lopez.l2j.game.boss.RaidPointsService raidPoints,
				com.lopez.l2j.network.game.security.BypassEncoderService bypassEncoder,
				com.lopez.l2j.network.game.security.PacketRateLimiter packetRateLimiter,
				com.lopez.l2j.game.service.CharacterVariablesService characterVariables,
				com.lopez.l2j.network.game.handler.admin.AdminCommandHandlerRegistry adminCommands,
				com.lopez.l2j.network.game.handler.voiced.VoicedCommandHandlerRegistry voicedCommands,
				com.lopez.l2j.network.game.handler.bypass.BypassHandlerRegistry bypassHandlers,
				com.lopez.l2j.game.service.AcpService acpService,
				com.lopez.l2j.game.service.SchemeBufferService schemeBuffer,
				com.lopez.l2j.game.service.BankingService banking,
				com.lopez.l2j.game.service.AwayStatusService away,
				com.lopez.l2j.config.ServerProperties.Rates rates, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists,
					combat, drops, shortcuts, skills, npcAi, skillService, multisell, warehouse, buffRepository, spawns,
					adminSuperusers, mapRegions, announcements, doors, armorSets, clans, crests, hennas, hennaTrees,
					zones, castles, cursedWeapons, olympiad, sevenSigns, manor, wordFilter, staticObjects, groundItems,
					recommendations, friends, weddings, dressMe, buffShop, augmentation, fishing, summonItems,
					extractableItems, alliances, clanWars, clanSkills, clanHalls, clanHallFunctions, clanHallSieges,
					clanPrivileges, sieges, crowns, mercenaries, siegeRewards, fortresses, fortressSieges, frintezza,
					fourSepulchers, dimensionalRift, communityBoard, offlineTrade, autoFarm, achievements, arenaDuel,
					officialEvent, roulette, characterReset,
					tvt, ctf, dm, partyFarm, botsPrevention, pvpColor, pvpRank, aio, preferences,
					lottery, monsterRace, fishingChampionship, petition, boat, l2day, starterKit, questManager, subClasses,
					macros, raidPoints, bypassEncoder, packetRateLimiter, characterVariables, adminCommands, voicedCommands, bypassHandlers,
					acpService, schemeBuffer, banking, away, null, null,
					rates, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat,
				com.lopez.l2j.game.drop.DropService drops, ShortCutRepository shortcuts,
				SkillRepository skills, com.lopez.l2j.game.ai.NpcAiService npcAi, SkillService skillService,
				com.lopez.l2j.game.multisell.MultiSellTable multisell,
				com.lopez.l2j.game.service.WarehouseService warehouse,
				com.lopez.l2j.game.effect.CharacterSkillSaveRepository buffRepository,
				SpawnService spawns,
				List<String> adminSuperusers,
				MapRegionTable mapRegions,
				Announcements announcements,
				DoorTable doors,
				ArmorSetsTable armorSets,
				ClanTable clans,
				CrestCache crests,
				com.lopez.l2j.game.henna.HennaTable hennas,
				com.lopez.l2j.game.henna.HennaTreeTable hennaTrees,
				com.lopez.l2j.game.zone.ZoneTable zones,
				com.lopez.l2j.game.castle.CastleManager castles,
				com.lopez.l2j.game.cursed.CursedWeaponsManager cursedWeapons,
				com.lopez.l2j.game.olympiad.OlympiadManager olympiad,
				com.lopez.l2j.game.sevensigns.SevenSignsManager sevenSigns,
				com.lopez.l2j.game.manor.CastleManorManager manor,
				com.lopez.l2j.game.chat.WordFilterTable wordFilter,
				com.lopez.l2j.game.staticobject.StaticObjectTable staticObjects,
				com.lopez.l2j.game.item.GroundItemService groundItems,
				com.lopez.l2j.game.social.CharacterRecommendationService recommendations,
				com.lopez.l2j.game.social.FriendListService friends,
				com.lopez.l2j.game.social.WeddingService weddings,
				com.lopez.l2j.game.dressme.DressMeService dressMe,
				com.lopez.l2j.game.buffshop.BuffShopService buffShop,
				com.lopez.l2j.game.augmentation.AugmentationService augmentation,
				com.lopez.l2j.game.fishing.FishingService fishing,
				com.lopez.l2j.game.summon.SummonItemService summonItems,
				com.lopez.l2j.game.extractable.ExtractableItemService extractableItems,
				com.lopez.l2j.game.clan.alliance.AllianceService alliances,
				com.lopez.l2j.game.clan.war.ClanWarService clanWars,
				com.lopez.l2j.game.clan.skill.ClanSkillService clanSkills,
				com.lopez.l2j.game.clan.clanhall.ClanHallService clanHalls,
				com.lopez.l2j.game.clan.clanhall.ClanHallFunctionService clanHallFunctions,
				com.lopez.l2j.game.clan.clanhall.siege.ClanHallSiegeService clanHallSieges,
				com.lopez.l2j.game.clan.privilege.ClanPrivilegeService clanPrivileges,
				com.lopez.l2j.game.castle.siege.SiegeService sieges,
				com.lopez.l2j.game.castle.crown.CrownService crowns,
				com.lopez.l2j.game.castle.mercenary.MercenaryService mercenaries,
				com.lopez.l2j.game.castle.reward.SiegeRewardService siegeRewards,
				com.lopez.l2j.game.fortress.FortressService fortresses,
				com.lopez.l2j.game.fortress.siege.FortressSiegeService fortressSieges,
				com.lopez.l2j.game.instance.frintezza.FrintezzaService frintezza,
				com.lopez.l2j.game.instance.foursepulchers.FourSepulchersService fourSepulchers,
				com.lopez.l2j.game.instance.dimensionalrift.DimensionalRiftService dimensionalRift,
				com.lopez.l2j.game.community.CommunityBoardService communityBoard,
				com.lopez.l2j.game.offlinetrade.OfflineTradeService offlineTrade,
				com.lopez.l2j.game.autofarm.AutoFarmService autoFarm,
				com.lopez.l2j.game.achievements.AchievementsService achievements,
				com.lopez.l2j.game.arenaduel.ArenaDuelService arenaDuel,
				com.lopez.l2j.game.event.official.OfficialEventService officialEvent,
				com.lopez.l2j.game.roulette.RouletteService roulette,
				com.lopez.l2j.game.reset.CharacterResetService characterReset,
				com.lopez.l2j.game.event.pvp.TvtEventService tvt,
				com.lopez.l2j.game.event.pvp.CtfEventService ctf,
				com.lopez.l2j.game.event.pvp.DmEventService dm,
				com.lopez.l2j.game.event.partyfarm.PartyFarmEventService partyFarm,
				com.lopez.l2j.game.service.BotsPreventionService botsPrevention,
				com.lopez.l2j.game.service.PvPColorService pvpColor,
				com.lopez.l2j.game.service.PvPRankService pvpRank,
				com.lopez.l2j.game.service.AioService aio,
				com.lopez.l2j.game.service.PlayerPreferencesService preferences,
				com.lopez.l2j.game.service.LotteryService lottery,
				com.lopez.l2j.game.service.MonsterRaceService monsterRace,
				com.lopez.l2j.game.fishing.FishingChampionshipService fishingChampionship,
				com.lopez.l2j.game.service.PetitionService petition,
				com.lopez.l2j.game.service.BoatService boat,
				com.lopez.l2j.game.event.official.L2DayEventService l2day,
				com.lopez.l2j.game.service.StarterKitService starterKit,
				com.lopez.l2j.game.quest.QuestManager questManager,
				com.lopez.l2j.game.subclass.SubClassService subClasses,
				com.lopez.l2j.game.macro.MacroRepository macros,
				com.lopez.l2j.game.boss.RaidPointsService raidPoints,
				com.lopez.l2j.config.ServerProperties.Rates rates, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists,
					combat, drops, shortcuts, skills, npcAi, skillService, multisell, warehouse, buffRepository, spawns,
					adminSuperusers, mapRegions, announcements, doors, armorSets, clans, crests, hennas, hennaTrees,
					zones, castles, cursedWeapons, olympiad, sevenSigns, manor, wordFilter, staticObjects, groundItems,
					recommendations, friends, weddings, dressMe, buffShop, augmentation, fishing, summonItems,
					extractableItems, alliances, clanWars, clanSkills, clanHalls, clanHallFunctions, clanHallSieges,
					clanPrivileges, sieges, crowns, mercenaries, siegeRewards, fortresses, fortressSieges, frintezza,
					fourSepulchers, dimensionalRift, communityBoard, offlineTrade, autoFarm, achievements, arenaDuel,
					officialEvent, roulette, characterReset,
					tvt, ctf, dm, partyFarm, botsPrevention, pvpColor, pvpRank, aio, preferences,
					lottery, monsterRace, fishingChampionship, petition, boat, l2day, starterKit, questManager, subClasses,
					macros, raidPoints, null, null, null, null, null, null, null, null, null, null, rates, serverName);
		}
		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat,
				com.lopez.l2j.game.drop.DropService drops, ShortCutRepository shortcuts,
				SkillRepository skills, com.lopez.l2j.game.ai.NpcAiService npcAi, SkillService skillService,
				com.lopez.l2j.game.multisell.MultiSellTable multisell,
				com.lopez.l2j.game.service.WarehouseService warehouse,
				com.lopez.l2j.game.effect.CharacterSkillSaveRepository buffRepository,
				SpawnService spawns,
				List<String> adminSuperusers,
				MapRegionTable mapRegions,
				Announcements announcements,
				DoorTable doors,
				ArmorSetsTable armorSets,
				ClanTable clans,
				CrestCache crests,
				com.lopez.l2j.game.henna.HennaTable hennas,
				com.lopez.l2j.game.henna.HennaTreeTable hennaTrees,
				com.lopez.l2j.game.zone.ZoneTable zones,
				com.lopez.l2j.game.castle.CastleManager castles,
				com.lopez.l2j.game.cursed.CursedWeaponsManager cursedWeapons,
				com.lopez.l2j.game.olympiad.OlympiadManager olympiad,
				com.lopez.l2j.game.sevensigns.SevenSignsManager sevenSigns,
				com.lopez.l2j.game.manor.CastleManorManager manor,
				com.lopez.l2j.game.chat.WordFilterTable wordFilter,
				com.lopez.l2j.game.staticobject.StaticObjectTable staticObjects,
				com.lopez.l2j.game.item.GroundItemService groundItems,
				com.lopez.l2j.game.social.CharacterRecommendationService recommendations,
				com.lopez.l2j.game.social.FriendListService friends,
				com.lopez.l2j.game.social.WeddingService weddings,
				com.lopez.l2j.game.dressme.DressMeService dressMe,
				com.lopez.l2j.game.buffshop.BuffShopService buffShop,
				com.lopez.l2j.game.augmentation.AugmentationService augmentation,
				com.lopez.l2j.game.fishing.FishingService fishing,
				com.lopez.l2j.game.summon.SummonItemService summonItems,
				com.lopez.l2j.game.extractable.ExtractableItemService extractableItems,
				com.lopez.l2j.game.clan.alliance.AllianceService alliances,
				com.lopez.l2j.game.clan.war.ClanWarService clanWars,
				com.lopez.l2j.game.clan.skill.ClanSkillService clanSkills,
				com.lopez.l2j.game.clan.clanhall.ClanHallService clanHalls,
				com.lopez.l2j.game.clan.clanhall.ClanHallFunctionService clanHallFunctions,
				com.lopez.l2j.game.clan.clanhall.siege.ClanHallSiegeService clanHallSieges,
				com.lopez.l2j.game.clan.privilege.ClanPrivilegeService clanPrivileges,
				com.lopez.l2j.game.castle.siege.SiegeService sieges,
				com.lopez.l2j.game.castle.crown.CrownService crowns,
				com.lopez.l2j.game.castle.mercenary.MercenaryService mercenaries,
				com.lopez.l2j.game.castle.reward.SiegeRewardService siegeRewards,
				com.lopez.l2j.game.fortress.FortressService fortresses,
				com.lopez.l2j.game.fortress.siege.FortressSiegeService fortressSieges,
				com.lopez.l2j.game.instance.frintezza.FrintezzaService frintezza,
				com.lopez.l2j.game.instance.foursepulchers.FourSepulchersService fourSepulchers,
				com.lopez.l2j.game.instance.dimensionalrift.DimensionalRiftService dimensionalRift,
				com.lopez.l2j.game.community.CommunityBoardService communityBoard,
				com.lopez.l2j.game.offlinetrade.OfflineTradeService offlineTrade,
				com.lopez.l2j.game.autofarm.AutoFarmService autoFarm,
				com.lopez.l2j.game.achievements.AchievementsService achievements,
				com.lopez.l2j.game.arenaduel.ArenaDuelService arenaDuel,
				com.lopez.l2j.game.event.official.OfficialEventService officialEvent,
				com.lopez.l2j.game.roulette.RouletteService roulette,
				com.lopez.l2j.game.reset.CharacterResetService characterReset,
				com.lopez.l2j.game.event.pvp.TvtEventService tvt,
				com.lopez.l2j.game.event.pvp.CtfEventService ctf,
				com.lopez.l2j.game.event.pvp.DmEventService dm,
				com.lopez.l2j.game.event.partyfarm.PartyFarmEventService partyFarm,
				com.lopez.l2j.game.service.BotsPreventionService botsPrevention,
				com.lopez.l2j.game.service.PvPColorService pvpColor,
				com.lopez.l2j.game.service.PvPRankService pvpRank,
				com.lopez.l2j.game.service.AioService aio,
				com.lopez.l2j.game.service.PlayerPreferencesService preferences,
				com.lopez.l2j.game.service.LotteryService lottery,
				com.lopez.l2j.game.service.MonsterRaceService monsterRace,
				com.lopez.l2j.game.fishing.FishingChampionshipService fishingChampionship,
				com.lopez.l2j.game.service.PetitionService petition,
				com.lopez.l2j.game.service.BoatService boat,
				com.lopez.l2j.game.event.official.L2DayEventService l2day,
				com.lopez.l2j.game.service.StarterKitService starterKit,
				com.lopez.l2j.game.quest.QuestManager questManager,
				com.lopez.l2j.game.subclass.SubClassService subClasses,
				com.lopez.l2j.config.ServerProperties.Rates rates, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists,
					combat, drops, shortcuts, skills, npcAi, skillService, multisell, warehouse, buffRepository, spawns,
					adminSuperusers, mapRegions, announcements, doors, armorSets, clans, crests, hennas, hennaTrees,
					zones, castles, cursedWeapons, olympiad, sevenSigns, manor, wordFilter, staticObjects, groundItems,
					recommendations, friends, weddings, dressMe, buffShop, augmentation, fishing, summonItems,
					extractableItems, alliances, clanWars, clanSkills, clanHalls, clanHallFunctions, clanHallSieges,
					clanPrivileges, sieges, crowns, mercenaries, siegeRewards, fortresses, fortressSieges, frintezza,
					fourSepulchers, dimensionalRift, communityBoard, offlineTrade, autoFarm, achievements, arenaDuel,
					officialEvent, roulette, characterReset,
					tvt, ctf, dm, partyFarm, botsPrevention, pvpColor, pvpRank, aio, preferences,
					lottery, monsterRace, fishingChampionship, petition, boat, l2day, starterKit, questManager, subClasses,
					null, null, rates, serverName);
		}

		public Context(MacroRepository macros) {
			this(746, 746, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null,
					List.<String>of(), null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null,
					null, null, null, null, null, null, null,
					null, null, null, null, null, null, null,
					null, null, null,
					null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null,
					macros, null, null, "TestServer");
		}
		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat,
				com.lopez.l2j.game.drop.DropService drops, ShortCutRepository shortcuts,
				SkillRepository skills, com.lopez.l2j.game.ai.NpcAiService npcAi, SkillService skillService,
				com.lopez.l2j.game.multisell.MultiSellTable multisell,
				com.lopez.l2j.game.service.WarehouseService warehouse,
				com.lopez.l2j.game.effect.CharacterSkillSaveRepository buffRepository,
				SpawnService spawns,
				List<String> adminSuperusers,
				MapRegionTable mapRegions,
				Announcements announcements,
				DoorTable doors,
				ArmorSetsTable armorSets,
				ClanTable clans,
				CrestCache crests,
				com.lopez.l2j.game.henna.HennaTable hennas,
				com.lopez.l2j.game.henna.HennaTreeTable hennaTrees,
				com.lopez.l2j.game.zone.ZoneTable zones,
				com.lopez.l2j.game.castle.CastleManager castles,
				com.lopez.l2j.game.cursed.CursedWeaponsManager cursedWeapons,
				com.lopez.l2j.game.olympiad.OlympiadManager olympiad,
				com.lopez.l2j.game.sevensigns.SevenSignsManager sevenSigns,
				com.lopez.l2j.game.manor.CastleManorManager manor,
				com.lopez.l2j.game.chat.WordFilterTable wordFilter,
				com.lopez.l2j.game.staticobject.StaticObjectTable staticObjects,
				com.lopez.l2j.game.item.GroundItemService groundItems,
				com.lopez.l2j.game.social.CharacterRecommendationService recommendations,
				com.lopez.l2j.game.social.FriendListService friends,
				com.lopez.l2j.game.social.WeddingService weddings,
				com.lopez.l2j.game.dressme.DressMeService dressMe,
				com.lopez.l2j.game.buffshop.BuffShopService buffShop,
				com.lopez.l2j.game.augmentation.AugmentationService augmentation,
				com.lopez.l2j.game.fishing.FishingService fishing,
				com.lopez.l2j.game.summon.SummonItemService summonItems,
				com.lopez.l2j.game.extractable.ExtractableItemService extractableItems,
				com.lopez.l2j.game.clan.alliance.AllianceService alliances,
				com.lopez.l2j.game.clan.war.ClanWarService clanWars,
				com.lopez.l2j.game.clan.skill.ClanSkillService clanSkills,
				com.lopez.l2j.game.clan.clanhall.ClanHallService clanHalls,
				com.lopez.l2j.game.clan.clanhall.ClanHallFunctionService clanHallFunctions,
				com.lopez.l2j.game.clan.clanhall.siege.ClanHallSiegeService clanHallSieges,
				com.lopez.l2j.game.clan.privilege.ClanPrivilegeService clanPrivileges,
				com.lopez.l2j.game.castle.siege.SiegeService sieges,
				com.lopez.l2j.game.castle.crown.CrownService crowns,
				com.lopez.l2j.game.castle.mercenary.MercenaryService mercenaries,
				com.lopez.l2j.game.castle.reward.SiegeRewardService siegeRewards,
				com.lopez.l2j.game.fortress.FortressService fortresses,
				com.lopez.l2j.game.fortress.siege.FortressSiegeService fortressSieges,
				com.lopez.l2j.game.instance.frintezza.FrintezzaService frintezza,
				com.lopez.l2j.game.instance.foursepulchers.FourSepulchersService fourSepulchers,
				com.lopez.l2j.game.instance.dimensionalrift.DimensionalRiftService dimensionalRift,
				com.lopez.l2j.game.community.CommunityBoardService communityBoard,
				com.lopez.l2j.game.offlinetrade.OfflineTradeService offlineTrade,
				com.lopez.l2j.game.autofarm.AutoFarmService autoFarm,
				com.lopez.l2j.game.achievements.AchievementsService achievements,
				com.lopez.l2j.game.arenaduel.ArenaDuelService arenaDuel,
				com.lopez.l2j.game.event.official.OfficialEventService officialEvent,
				com.lopez.l2j.game.roulette.RouletteService roulette,
				com.lopez.l2j.game.reset.CharacterResetService characterReset,
				com.lopez.l2j.config.ServerProperties.Rates rates, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists,
					combat, drops, shortcuts, skills, npcAi, skillService, multisell, warehouse, buffRepository, spawns,
					adminSuperusers, mapRegions, announcements, doors, armorSets, clans, crests, hennas, hennaTrees,
					zones, castles, cursedWeapons, olympiad, sevenSigns, manor, wordFilter, staticObjects, groundItems,
					recommendations, friends, weddings, dressMe, buffShop, augmentation, fishing, summonItems,
					extractableItems, alliances, clanWars, clanSkills, clanHalls, clanHallFunctions, clanHallSieges,
					clanPrivileges, sieges, crowns, mercenaries, siegeRewards, fortresses, fortressSieges, frintezza,
					fourSepulchers, dimensionalRift, communityBoard, offlineTrade, autoFarm, achievements, arenaDuel,
					officialEvent, roulette, characterReset,
					null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					rates, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat,
				com.lopez.l2j.game.drop.DropService drops, ShortCutRepository shortcuts,
				SkillRepository skills, com.lopez.l2j.game.ai.NpcAiService npcAi, SkillService skillService,
				com.lopez.l2j.game.multisell.MultiSellTable multisell,
				com.lopez.l2j.game.service.WarehouseService warehouse,
				com.lopez.l2j.game.effect.CharacterSkillSaveRepository buffRepository,
				SpawnService spawns,
				List<String> adminSuperusers,
				MapRegionTable mapRegions,
				Announcements announcements,
				DoorTable doors,
				ArmorSetsTable armorSets,
				ClanTable clans,
				CrestCache crests,
				com.lopez.l2j.game.henna.HennaTable hennas,
				com.lopez.l2j.game.henna.HennaTreeTable hennaTrees,
				com.lopez.l2j.game.zone.ZoneTable zones,
				com.lopez.l2j.game.castle.CastleManager castles,
				com.lopez.l2j.game.cursed.CursedWeaponsManager cursedWeapons,
				com.lopez.l2j.game.olympiad.OlympiadManager olympiad,
				com.lopez.l2j.game.sevensigns.SevenSignsManager sevenSigns,
				com.lopez.l2j.game.manor.CastleManorManager manor,
				com.lopez.l2j.game.chat.WordFilterTable wordFilter,
				com.lopez.l2j.game.staticobject.StaticObjectTable staticObjects,
				com.lopez.l2j.game.item.GroundItemService groundItems,
				com.lopez.l2j.game.social.CharacterRecommendationService recommendations,
				com.lopez.l2j.game.social.FriendListService friends,
				com.lopez.l2j.game.social.WeddingService weddings,
				com.lopez.l2j.game.dressme.DressMeService dressMe,
				com.lopez.l2j.game.buffshop.BuffShopService buffShop,
				com.lopez.l2j.game.augmentation.AugmentationService augmentation,
				com.lopez.l2j.game.fishing.FishingService fishing,
				com.lopez.l2j.game.summon.SummonItemService summonItems,
				com.lopez.l2j.game.extractable.ExtractableItemService extractableItems,
				com.lopez.l2j.game.clan.alliance.AllianceService alliances,
				com.lopez.l2j.game.clan.war.ClanWarService clanWars,
				com.lopez.l2j.game.clan.skill.ClanSkillService clanSkills,
				com.lopez.l2j.game.clan.clanhall.ClanHallService clanHalls,
				com.lopez.l2j.game.clan.clanhall.ClanHallFunctionService clanHallFunctions,
				com.lopez.l2j.game.clan.clanhall.siege.ClanHallSiegeService clanHallSieges,
				com.lopez.l2j.game.clan.privilege.ClanPrivilegeService clanPrivileges,
				com.lopez.l2j.game.castle.siege.SiegeService sieges,
				com.lopez.l2j.game.castle.crown.CrownService crowns,
				com.lopez.l2j.game.castle.mercenary.MercenaryService mercenaries,
				com.lopez.l2j.game.castle.reward.SiegeRewardService siegeRewards,
				com.lopez.l2j.game.fortress.FortressService fortresses,
				com.lopez.l2j.game.fortress.siege.FortressSiegeService fortressSieges,
				com.lopez.l2j.game.instance.frintezza.FrintezzaService frintezza,
				com.lopez.l2j.game.instance.foursepulchers.FourSepulchersService fourSepulchers,
				com.lopez.l2j.game.instance.dimensionalrift.DimensionalRiftService dimensionalRift,
				com.lopez.l2j.game.community.CommunityBoardService communityBoard,
				com.lopez.l2j.game.offlinetrade.OfflineTradeService offlineTrade,
				com.lopez.l2j.game.autofarm.AutoFarmService autoFarm,
				com.lopez.l2j.game.achievements.AchievementsService achievements,
				com.lopez.l2j.game.arenaduel.ArenaDuelService arenaDuel,
				com.lopez.l2j.game.event.official.OfficialEventService officialEvent,
				com.lopez.l2j.game.roulette.RouletteService roulette,
				com.lopez.l2j.game.reset.CharacterResetService characterReset,
				com.lopez.l2j.game.event.pvp.TvtEventService tvt,
				com.lopez.l2j.game.event.pvp.CtfEventService ctf,
				com.lopez.l2j.game.event.pvp.DmEventService dm,
				com.lopez.l2j.game.event.partyfarm.PartyFarmEventService partyFarm,
				com.lopez.l2j.game.service.BotsPreventionService botsPrevention,
				com.lopez.l2j.game.service.PvPColorService pvpColor,
				com.lopez.l2j.game.service.PvPRankService pvpRank,
				com.lopez.l2j.game.service.AioService aio,
				com.lopez.l2j.game.service.PlayerPreferencesService preferences,
				com.lopez.l2j.game.service.LotteryService lottery,
				com.lopez.l2j.game.service.MonsterRaceService monsterRace,
				com.lopez.l2j.game.fishing.FishingChampionshipService fishingChampionship,
				com.lopez.l2j.game.service.PetitionService petition,
				com.lopez.l2j.game.service.BoatService boat,
				com.lopez.l2j.game.event.official.L2DayEventService l2day,
				com.lopez.l2j.game.service.StarterKitService starterKit,
				com.lopez.l2j.config.ServerProperties.Rates rates, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists,
					combat, drops, shortcuts, skills, npcAi, skillService, multisell, warehouse, buffRepository, spawns,
					adminSuperusers, mapRegions, announcements, doors, armorSets, clans, crests, hennas, hennaTrees,
					zones, castles, cursedWeapons, olympiad, sevenSigns, manor, wordFilter, staticObjects, groundItems,
					recommendations, friends, weddings, dressMe, buffShop, augmentation, fishing, summonItems,
					extractableItems, alliances, clanWars, clanSkills, clanHalls, clanHallFunctions, clanHallSieges,
					clanPrivileges, sieges, crowns, mercenaries, siegeRewards, fortresses, fortressSieges, frintezza,
					fourSepulchers, dimensionalRift, communityBoard, offlineTrade, autoFarm, achievements, arenaDuel,
					officialEvent, roulette, characterReset,
					tvt, ctf, dm, partyFarm, botsPrevention, pvpColor, pvpRank, aio, preferences,
					lottery, monsterRace, fishingChampionship, petition, boat, l2day, starterKit, null, null,
					rates, serverName);
		}
		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat,
				com.lopez.l2j.game.drop.DropService drops, ShortCutRepository shortcuts,
				SkillRepository skills, com.lopez.l2j.game.ai.NpcAiService npcAi, SkillService skillService,
				com.lopez.l2j.game.multisell.MultiSellTable multisell,
				com.lopez.l2j.game.service.WarehouseService warehouse,
				com.lopez.l2j.game.effect.CharacterSkillSaveRepository buffRepository,
				SpawnService spawns,
				List<String> adminSuperusers,
				MapRegionTable mapRegions,
				Announcements announcements,
				DoorTable doors,
				ArmorSetsTable armorSets,
				ClanTable clans,
				CrestCache crests,
				com.lopez.l2j.game.henna.HennaTable hennas,
				com.lopez.l2j.game.henna.HennaTreeTable hennaTrees,
				com.lopez.l2j.game.zone.ZoneTable zones,
				com.lopez.l2j.game.castle.CastleManager castles,
				com.lopez.l2j.game.cursed.CursedWeaponsManager cursedWeapons,
				com.lopez.l2j.game.olympiad.OlympiadManager olympiad,
				com.lopez.l2j.game.sevensigns.SevenSignsManager sevenSigns,
				com.lopez.l2j.game.manor.CastleManorManager manor,
				com.lopez.l2j.config.ServerProperties.Rates rates, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists,
					combat, drops, shortcuts, skills, npcAi, skillService, multisell, warehouse, buffRepository, spawns,
					adminSuperusers, mapRegions, announcements, doors, armorSets, clans, crests, hennas, hennaTrees,
					zones, castles, cursedWeapons, olympiad, sevenSigns, manor, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, rates,
					serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat,
				com.lopez.l2j.game.drop.DropService drops, ShortCutRepository shortcuts,
				SkillRepository skills, com.lopez.l2j.game.ai.NpcAiService npcAi, SkillService skillService,
				com.lopez.l2j.game.multisell.MultiSellTable multisell,
				com.lopez.l2j.game.service.WarehouseService warehouse,
				com.lopez.l2j.game.effect.CharacterSkillSaveRepository buffRepository,
				SpawnService spawns,
				List<String> adminSuperusers,
				MapRegionTable mapRegions,
				Announcements announcements,
				DoorTable doors,
				ArmorSetsTable armorSets,
				ClanTable clans,
				CrestCache crests,
				com.lopez.l2j.game.henna.HennaTable hennas,
				com.lopez.l2j.game.henna.HennaTreeTable hennaTrees,
				com.lopez.l2j.game.zone.ZoneTable zones,
				com.lopez.l2j.game.castle.CastleManager castles,
				com.lopez.l2j.game.cursed.CursedWeaponsManager cursedWeapons,
				com.lopez.l2j.game.olympiad.OlympiadManager olympiad,
				com.lopez.l2j.game.sevensigns.SevenSignsManager sevenSigns,
				com.lopez.l2j.game.manor.CastleManorManager manor,
				com.lopez.l2j.game.chat.WordFilterTable wordFilter,
				com.lopez.l2j.game.staticobject.StaticObjectTable staticObjects,
				com.lopez.l2j.game.item.GroundItemService groundItems,
				com.lopez.l2j.game.social.CharacterRecommendationService recommendations,
				com.lopez.l2j.game.social.FriendListService friends,
				com.lopez.l2j.config.ServerProperties.Rates rates, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists,
					combat, drops, shortcuts, skills, npcAi, skillService, multisell, warehouse, buffRepository, spawns,
					adminSuperusers, mapRegions, announcements, doors, armorSets, clans, crests, hennas, hennaTrees,
					zones, castles, cursedWeapons, olympiad, sevenSigns, manor, wordFilter, staticObjects, groundItems,
					recommendations, friends, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, rates, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat,
				com.lopez.l2j.game.drop.DropService drops, ShortCutRepository shortcuts,
				SkillRepository skills, com.lopez.l2j.game.ai.NpcAiService npcAi, SkillService skillService,
				com.lopez.l2j.game.multisell.MultiSellTable multisell,
				com.lopez.l2j.game.service.WarehouseService warehouse,
				com.lopez.l2j.game.effect.CharacterSkillSaveRepository buffRepository,
				SpawnService spawns,
				List<String> adminSuperusers,
				MapRegionTable mapRegions,
				Announcements announcements,
				DoorTable doors,
				ArmorSetsTable armorSets,
				ClanTable clans,
				CrestCache crests,
				com.lopez.l2j.game.henna.HennaTable hennas,
				com.lopez.l2j.game.henna.HennaTreeTable hennaTrees,
				com.lopez.l2j.game.zone.ZoneTable zones,
				com.lopez.l2j.game.castle.CastleManager castles,
				com.lopez.l2j.game.cursed.CursedWeaponsManager cursedWeapons,
				com.lopez.l2j.game.olympiad.OlympiadManager olympiad,
				com.lopez.l2j.game.sevensigns.SevenSignsManager sevenSigns,
				com.lopez.l2j.config.ServerProperties.Rates rates, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists,
					combat, drops, shortcuts, skills, npcAi, skillService, multisell, warehouse, buffRepository, spawns,
					adminSuperusers, mapRegions, announcements, doors, armorSets, clans, crests, hennas, hennaTrees,
					zones, castles, cursedWeapons, olympiad, sevenSigns, null, rates, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat,
				com.lopez.l2j.game.drop.DropService drops, ShortCutRepository shortcuts,
				SkillRepository skills, com.lopez.l2j.game.ai.NpcAiService npcAi, SkillService skillService,
				com.lopez.l2j.game.multisell.MultiSellTable multisell,
				com.lopez.l2j.game.service.WarehouseService warehouse,
				com.lopez.l2j.game.effect.CharacterSkillSaveRepository buffRepository,
				SpawnService spawns,
				List<String> adminSuperusers,
				MapRegionTable mapRegions,
				Announcements announcements,
				DoorTable doors,
				ArmorSetsTable armorSets,
				ClanTable clans,
				CrestCache crests,
				com.lopez.l2j.game.henna.HennaTable hennas,
				com.lopez.l2j.game.henna.HennaTreeTable hennaTrees,
				com.lopez.l2j.game.zone.ZoneTable zones,
				com.lopez.l2j.game.castle.CastleManager castles,
				com.lopez.l2j.game.cursed.CursedWeaponsManager cursedWeapons,
				com.lopez.l2j.game.olympiad.OlympiadManager olympiad,
				com.lopez.l2j.config.ServerProperties.Rates rates, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists,
					combat, drops, shortcuts, skills, npcAi, skillService, multisell, warehouse, buffRepository, spawns,
					adminSuperusers, mapRegions, announcements, doors, armorSets, clans, crests, hennas, hennaTrees,
					zones, castles, cursedWeapons, olympiad, null, null, rates, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat,
				com.lopez.l2j.game.drop.DropService drops, ShortCutRepository shortcuts,
				SkillRepository skills, com.lopez.l2j.game.ai.NpcAiService npcAi, SkillService skillService,
				com.lopez.l2j.game.multisell.MultiSellTable multisell,
				com.lopez.l2j.game.service.WarehouseService warehouse,
				com.lopez.l2j.game.effect.CharacterSkillSaveRepository buffRepository,
				SpawnService spawns,
				List<String> adminSuperusers,
				MapRegionTable mapRegions,
				Announcements announcements,
				DoorTable doors,
				ArmorSetsTable armorSets,
				ClanTable clans,
				CrestCache crests,
				com.lopez.l2j.game.henna.HennaTable hennas,
				com.lopez.l2j.game.henna.HennaTreeTable hennaTrees,
				com.lopez.l2j.game.zone.ZoneTable zones,
				com.lopez.l2j.game.castle.CastleManager castles,
				com.lopez.l2j.game.cursed.CursedWeaponsManager cursedWeapons,
				com.lopez.l2j.config.ServerProperties.Rates rates, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists,
					combat, drops, shortcuts, skills, npcAi, skillService, multisell, warehouse, buffRepository, spawns,
					adminSuperusers, mapRegions, announcements, doors, armorSets, clans, crests, hennas, hennaTrees,
					zones, castles, cursedWeapons, null, null, rates, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat,
				com.lopez.l2j.game.drop.DropService drops, ShortCutRepository shortcuts,
				SkillRepository skills, com.lopez.l2j.game.ai.NpcAiService npcAi, SkillService skillService,
				com.lopez.l2j.game.multisell.MultiSellTable multisell,
				com.lopez.l2j.game.service.WarehouseService warehouse,
				com.lopez.l2j.game.effect.CharacterSkillSaveRepository buffRepository,
				SpawnService spawns,
				List<String> adminSuperusers,
				MapRegionTable mapRegions,
				Announcements announcements,
				DoorTable doors,
				ArmorSetsTable armorSets,
				ClanTable clans,
				CrestCache crests,
				com.lopez.l2j.game.henna.HennaTable hennas,
				com.lopez.l2j.game.henna.HennaTreeTable hennaTrees,
				com.lopez.l2j.game.zone.ZoneTable zones,
				com.lopez.l2j.game.castle.CastleManager castles,
				com.lopez.l2j.config.ServerProperties.Rates rates, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists,
					combat, drops, shortcuts, skills, npcAi, skillService, multisell, warehouse, buffRepository, spawns,
					adminSuperusers, mapRegions, announcements, doors, armorSets, clans, crests, hennas, hennaTrees,
					zones, castles, null, null, rates, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat,
				com.lopez.l2j.game.drop.DropService drops, ShortCutRepository shortcuts,
				SkillRepository skills, com.lopez.l2j.game.ai.NpcAiService npcAi, SkillService skillService,
				com.lopez.l2j.game.multisell.MultiSellTable multisell,
				com.lopez.l2j.game.service.WarehouseService warehouse,
				com.lopez.l2j.game.effect.CharacterSkillSaveRepository buffRepository,
				SpawnService spawns,
				List<String> adminSuperusers,
				MapRegionTable mapRegions,
				Announcements announcements,
				DoorTable doors,
				ArmorSetsTable armorSets,
				ClanTable clans,
				CrestCache crests,
				com.lopez.l2j.game.henna.HennaTable hennas,
				com.lopez.l2j.game.henna.HennaTreeTable hennaTrees,
				com.lopez.l2j.config.ServerProperties.Rates rates, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists,
					combat, drops, shortcuts, skills, npcAi, skillService, multisell, warehouse, buffRepository, spawns,
					adminSuperusers, mapRegions, announcements, doors, armorSets, clans, crests, hennas, hennaTrees,
					null, null, null, rates, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat,
				com.lopez.l2j.game.drop.DropService drops, ShortCutRepository shortcuts,
				SkillRepository skills, com.lopez.l2j.game.ai.NpcAiService npcAi, SkillService skillService,
				com.lopez.l2j.game.multisell.MultiSellTable multisell,
				com.lopez.l2j.game.service.WarehouseService warehouse,
				com.lopez.l2j.game.effect.CharacterSkillSaveRepository buffRepository,
				SpawnService spawns,
				List<String> adminSuperusers,
				MapRegionTable mapRegions,
				Announcements announcements,
				DoorTable doors,
				ArmorSetsTable armorSets,
				ClanTable clans,
				CrestCache crests,
				com.lopez.l2j.config.ServerProperties.Rates rates, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists,
					combat, drops, shortcuts, skills, npcAi, skillService, multisell, warehouse, buffRepository, spawns,
					adminSuperusers, mapRegions, announcements, doors, armorSets, clans, crests, null, null, rates,
					serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat,
				com.lopez.l2j.game.drop.DropService drops, ShortCutRepository shortcuts,
				SkillRepository skills, com.lopez.l2j.game.ai.NpcAiService npcAi, SkillService skillService,
				com.lopez.l2j.game.multisell.MultiSellTable multisell,
				com.lopez.l2j.game.service.WarehouseService warehouse,
				com.lopez.l2j.game.effect.CharacterSkillSaveRepository buffRepository,
				SpawnService spawns,
				List<String> adminSuperusers,
				MapRegionTable mapRegions,
				Announcements announcements,
				DoorTable doors,
				ArmorSetsTable armorSets,
				com.lopez.l2j.config.ServerProperties.Rates rates, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists,
					combat, drops, shortcuts, skills, npcAi, skillService, multisell, warehouse, buffRepository, spawns,
					adminSuperusers, mapRegions, announcements, doors, armorSets, null, null, null, null, rates,
					serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat,
				com.lopez.l2j.game.drop.DropService drops, ShortCutRepository shortcuts,
				SkillRepository skills, com.lopez.l2j.game.ai.NpcAiService npcAi, SkillService skillService,
				com.lopez.l2j.game.multisell.MultiSellTable multisell,
				com.lopez.l2j.game.service.WarehouseService warehouse,
				com.lopez.l2j.game.effect.CharacterSkillSaveRepository buffRepository,
				SpawnService spawns,
				List<String> adminSuperusers,
				com.lopez.l2j.config.ServerProperties.Rates rates, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists,
					combat, drops, shortcuts, skills, npcAi, skillService, multisell, warehouse, buffRepository, spawns,
					adminSuperusers, null, null, null, null, null, null, rates, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat,
				com.lopez.l2j.game.drop.DropService drops, ShortCutRepository shortcuts,
				SkillRepository skills, com.lopez.l2j.game.ai.NpcAiService npcAi, SkillService skillService,
				com.lopez.l2j.game.multisell.MultiSellTable multisell,
				com.lopez.l2j.game.service.WarehouseService warehouse,
				com.lopez.l2j.game.effect.CharacterSkillSaveRepository buffRepository,
				com.lopez.l2j.config.ServerProperties.Rates rates, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists,
					combat, drops, shortcuts, skills, npcAi, skillService, multisell, warehouse, buffRepository, null,
					List.of(), null, null, null, null, rates, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, null, null, null, null, null,
					null, null, null, null, null, null, null, null, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, null, null, null, null,
					null, null, null, null, null, null, null, null, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists,
					null, null, null, null, null, null, null, null, null, null, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists,
					combat, null, null, null, null, null, null, null, null, null, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat,
				com.lopez.l2j.game.drop.DropService drops, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists,
					combat, drops, null, null, null, null, null, null, null, null, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat,
				com.lopez.l2j.game.drop.DropService drops, ShortCutRepository shortcuts,
				SkillRepository skills, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists,
					combat, drops, shortcuts, skills, null, null, null, null, null, null, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat,
				com.lopez.l2j.game.drop.DropService drops, ShortCutRepository shortcuts,
				SkillRepository skills, com.lopez.l2j.game.ai.NpcAiService npcAi, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists,
					combat, drops, shortcuts, skills, npcAi, null, null, null, null, null, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat,
				com.lopez.l2j.game.drop.DropService drops, ShortCutRepository shortcuts,
				SkillRepository skills, com.lopez.l2j.game.ai.NpcAiService npcAi, SkillService skillService,
				String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists,
					combat, drops, shortcuts, skills, npcAi, skillService, null, null, null, null, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat,
				com.lopez.l2j.game.drop.DropService drops, ShortCutRepository shortcuts,
				SkillRepository skills, com.lopez.l2j.game.ai.NpcAiService npcAi, SkillService skillService,
				com.lopez.l2j.config.ServerProperties.Rates rates, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists,
					combat, drops, shortcuts, skills, npcAi, skillService, null, null, null, rates, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat,
				com.lopez.l2j.game.drop.DropService drops, ShortCutRepository shortcuts,
				SkillRepository skills, com.lopez.l2j.game.ai.NpcAiService npcAi, SkillService skillService,
				com.lopez.l2j.game.multisell.MultiSellTable multisell,
				com.lopez.l2j.config.ServerProperties.Rates rates, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists,
					combat, drops, shortcuts, skills, npcAi, skillService, multisell, null, null, rates, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat,
				com.lopez.l2j.game.drop.DropService drops, ShortCutRepository shortcuts,
				SkillRepository skills, com.lopez.l2j.game.ai.NpcAiService npcAi, SkillService skillService,
				com.lopez.l2j.game.multisell.MultiSellTable multisell,
				com.lopez.l2j.game.service.WarehouseService warehouse,
				com.lopez.l2j.config.ServerProperties.Rates rates, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists,
					combat, drops, shortcuts, skills, npcAi, skillService, multisell, warehouse, null, rates,
					serverName);
		}

		private static volatile com.lopez.l2j.game.service.PvpRewardService globalPvpRewardService;
		private static volatile com.lopez.l2j.game.henna.CharacterHennaRepository globalCharacterHennaRepository;

		public static void setGlobalPvpRewardService(com.lopez.l2j.game.service.PvpRewardService service) {
			globalPvpRewardService = service;
		}

		public com.lopez.l2j.game.service.PvpRewardService pvpRewards() {
			return globalPvpRewardService;
		}

		public static void setGlobalCharacterHennaRepository(com.lopez.l2j.game.henna.CharacterHennaRepository repo) {
			globalCharacterHennaRepository = repo;
		}

		public com.lopez.l2j.game.henna.CharacterHennaRepository characterHennas() {
			return globalCharacterHennaRepository;
		}

		public com.lopez.l2j.game.skill.SkillEnchantService skillEnchant() {
			return new com.lopez.l2j.game.skill.SkillEnchantService(
					skillService != null ? skillService.trees() : null, skillService, inventories);
		}

		private static volatile com.lopez.l2j.game.service.CancelRestoreService globalCancelRestoreService;

		public static void setGlobalCancelRestoreService(com.lopez.l2j.game.service.CancelRestoreService service) {
			globalCancelRestoreService = service;
		}

		public com.lopez.l2j.game.service.CancelRestoreService cancelRestore() {
			if (globalCancelRestoreService == null) {
				globalCancelRestoreService = new com.lopez.l2j.game.service.CancelRestoreService();
			}
			return globalCancelRestoreService;
		}
	}

	private final Context ctx;
	private final byte[] cryptKey;
	private final Consumer<GameServerPacket> sink;
	private final String ip;
	private State state = State.CONNECTED;

	public State state() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}
	private boolean protocolOk;
	private boolean closeRequested;
	private String account;
	private int sessionId;
	private List<PlayerCharacter> characterList = List.of();
	private PlayerCharacter active;
	private boolean inWorld;
	private volatile boolean teleporting;
	private volatile boolean pendingRevive;
	private int targetObjectId;
	private final List<String> sessionBypasses = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
	private final com.lopez.l2j.network.game.security.PacketRateLimiter.SessionRateState rateState = new com.lopez.l2j.network.game.security.PacketRateLimiter.SessionRateState();
	private boolean autoAttacking;
	private final Set<Integer> knownObjects = ConcurrentHashMap.newKeySet();
	/** Soulshots com uso automatico ligado (itemId). */
	private final Set<Integer> autoSoulShots = ConcurrentHashMap.newKeySet();
	private volatile boolean soulshotCharged;
	private volatile int chargedGrade = -1;
	private volatile boolean spiritshotCharged;
	private volatile boolean blessedSpiritshot;
	private volatile int chargedSpSGrade = -1;
	private ScheduledFuture<?> regenTask;
	/** Reuse por skillId do consumivel (epoch ms em que libera). */
	private final Map<Integer, Long> consumableReuse = new ConcurrentHashMap<>();
	/** Curas ao longo do tempo ativas por stackType (HpRecover/MpRecover). */
	private final Map<String, ScheduledFuture<?>> hotTasks = new ConcurrentHashMap<>();

	private com.lopez.l2j.game.party.Party party;
	private RequestPartyPending pendingPartyInvite;
	private volatile long attackEndTime;
	private ScheduledFuture<?> combatStanceTask;
	private volatile int pendingNpcInteractObjectId;
	private volatile int activeEnchantScrollObjectId;
	private volatile int pendingFriendInviteFrom;
	private int currentWeightPenalty = -1;
	private int currentGradePenalty = -1;
	private volatile boolean inWater;
	private volatile long waterEntryTime;
	private volatile long lastDrownDamageTime;
	private volatile long lastDamageZoneTickTime;
	private final Map<String, QuestState> questStates = new ConcurrentHashMap<>();
	private final Map<Integer, Macro> macros = new ConcurrentHashMap<>();
	private int macroRevision = 1;
	private int nextMacroId = 1000;

	public record RequestPartyPending(GameSession requester, int itemDistribution) {
	}

	private static final ScheduledExecutorService autoAttackScheduler = Executors.newScheduledThreadPool(4, r -> {
		Thread th = new Thread(r, "PlayerAutoAttack");
		th.setDaemon(true);
		return th;
	});

	public static ScheduledExecutorService autoAttackScheduler() {
		return autoAttackScheduler;
	}

	private final Consumer<GameServerPacket> closeCallback;
	private final ChatPacketHandler chatHandler;
	private final PartyClanPacketHandler partyClanHandler;
	private final ItemPacketHandler itemHandler;
	private final ActionPacketHandler actionHandler;
	private final TradeStorePacketHandler tradeStoreHandler;

	public GameSession(Context ctx, byte[] cryptKey, String ip, Consumer<GameServerPacket> sink) {
		this(ctx, cryptKey, ip, sink, null);
	}

	public GameSession(Context ctx, byte[] cryptKey, String ip, Consumer<GameServerPacket> sink,
			Consumer<GameServerPacket> closeCallback) {
		this.ctx = ctx;
		this.cryptKey = cryptKey.clone();
		this.ip = ip;
		this.sink = sink;
		this.closeCallback = closeCallback;
		this.chatHandler = new ChatPacketHandler(this);
		this.partyClanHandler = new PartyClanPacketHandler(this);
		this.itemHandler = new ItemPacketHandler(this);
		this.actionHandler = new ActionPacketHandler(this);
		this.tradeStoreHandler = new TradeStorePacketHandler(this);
	}

	@Override
	public void close(GameServerPacket packet) {
		closeRequested = true;
		if (closeCallback != null) {
			closeCallback.accept(packet);
		}
	}

	public void kick() {
		send(SystemMessage.of(SystemMessage.DISCONNECTED_FROM_SERVER));
		close(new ServerClose());
	}

	public boolean inWorld() {
		return inWorld;
	}

	public boolean isAutoAttacking() {
		return autoAttacking;
	}

	public String account() {
		return account;
	}

	public String ip() {
		return ip;
	}

	public String clientIp() {
		return ip;
	}

	public PlayerCharacter activeCharacter() {
		return active;
	}

	public PlayerCharacter activeChar() {
		return active;
	}

	public int getLevel() {
		return active != null ? active.level() : 1;
	}

	public ItemInstance getActiveWeaponItem() {
		return active != null && active.inventory() != null ? active.inventory().paperdoll(ItemSlots.RHAND) : null;
	}

	public Context context() {
		return ctx;
	}

	public int targetObjectId() {
		return targetObjectId;
	}

	public void targetObjectId(int id) {
		this.targetObjectId = id;
		if (id == 0 && active != null) {
			var unselect = new TargetUnselected(active.objectId(), active.x(), active.y(), active.z());
			send(unselect);
			if (ctx != null && ctx.world() != null) {
				ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, unselect, false);
			}
		}
	}

	public void clearTarget() {
		if (targetObjectId != 0) {
			targetObjectId = 0;
			if (active != null) {
				var unselect = new TargetUnselected(active.objectId(), active.x(), active.y(), active.z());
				send(unselect);
				if (ctx != null && ctx.world() != null) {
					ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, unselect, false);
				}
			}
		}
	}

	public void forceTarget(int objId) {
		this.targetObjectId = objId;
		send(new MyTargetSelected(objId, 0));
	}

	public boolean canAttackInPvP(GameSession target) {
		if (target == null || target == this || target.active == null || target.active.isDead()) {
			return false;
		}
		if (ctx != null && ctx.zones() != null && (ctx.zones().isInsidePeace(active.x(), active.y(), active.z())
				|| ctx.zones().isInsidePeace(target.x(), target.y(), target.z()))) {
			return false;
		}
		if (party != null && party.contains(target.active.objectId())) {
			return false;
		}
		return true;
	}

	public List<String> sessionBypasses() {
		return sessionBypasses;
	}

	public com.lopez.l2j.network.game.security.PacketRateLimiter.SessionRateState rateState() {
		return rateState;
	}

	public QuestState getQuestState(String questName) {
		return questName != null ? questStates.get(questName.toLowerCase(java.util.Locale.ROOT)) : null;
	}

	public void addQuestState(QuestState qs) {
		if (qs != null && qs.getQuestName() != null) {
			questStates.put(qs.getQuestName().toLowerCase(java.util.Locale.ROOT), qs);
		}
	}

	public Collection<QuestState> getAllQuestStates() {
		return questStates.values();
	}

	public com.lopez.l2j.game.party.Party party() {
		return party;
	}

	public void party(com.lopez.l2j.game.party.Party party) {
		this.party = party;
	}

	public RequestPartyPending pendingPartyInvite() {
		return pendingPartyInvite;
	}

	public void setPendingPartyInvite(RequestPartyPending pendingPartyInvite) {
		this.pendingPartyInvite = pendingPartyInvite;
	}

	public ChatPacketHandler chatHandler() {
		return chatHandler;
	}

	public PartyClanPacketHandler partyClanHandler() {
		return partyClanHandler;
	}

	public ItemPacketHandler itemHandler() {
		return itemHandler;
	}

	public ActionPacketHandler actionHandler() {
		return actionHandler;
	}

	public TradeStorePacketHandler tradeStoreHandler() {
		return tradeStoreHandler;
	}

	public boolean casting() {
		return casting;
	}

	public void casting(boolean casting) {
		this.casting = casting;
	}

	public SkillTemplate castingSkill() {
		return castingSkill;
	}

	public void castingSkill(SkillTemplate castingSkill) {
		this.castingSkill = castingSkill;
	}

	public ScheduledFuture<?> castTask() {
		return castTask;
	}

	public void castTask(ScheduledFuture<?> castTask) {
		this.castTask = castTask;
	}

	public boolean teleporting() {
		return teleporting;
	}

	public void teleporting(boolean teleporting) {
		this.teleporting = teleporting;
	}

	public boolean pendingRevive() {
		return pendingRevive;
	}

	public void pendingRevive(boolean pendingRevive) {
		this.pendingRevive = pendingRevive;
	}

	public int pendingNpcInteractObjectId() {
		return pendingNpcInteractObjectId;
	}

	public void pendingNpcInteractObjectId(int id) {
		this.pendingNpcInteractObjectId = id;
	}

	public boolean soulshotCharged() {
		return soulshotCharged;
	}

	public void soulshotCharged(boolean soulshotCharged) {
		this.soulshotCharged = soulshotCharged;
	}

	public boolean spiritshotCharged() {
		return spiritshotCharged;
	}

	public void spiritshotCharged(boolean spiritshotCharged) {
		this.spiritshotCharged = spiritshotCharged;
	}

	public int chargedGrade() {
		return chargedGrade;
	}

	public void chargedGrade(int chargedGrade) {
		this.chargedGrade = chargedGrade;
	}

	public int chargedSpSGrade() {
		return chargedSpSGrade;
	}

	public void chargedSpSGrade(int chargedSpSGrade) {
		this.chargedSpSGrade = chargedSpSGrade;
	}

	public boolean blessedSpiritshot() {
		return blessedSpiritshot;
	}

	public void blessedSpiritshot(boolean blessedSpiritshot) {
		this.blessedSpiritshot = blessedSpiritshot;
	}

	public Set<Integer> autoSoulShots() {
		return autoSoulShots;
	}

	public int activeEnchantScrollObjectId() {
		return activeEnchantScrollObjectId;
	}

	public void activeEnchantScrollObjectId(int activeEnchantScrollObjectId) {
		this.activeEnchantScrollObjectId = activeEnchantScrollObjectId;
	}

	public Map<Integer, Long> consumableReuse() {
		return consumableReuse;
	}

	public Map<String, ScheduledFuture<?>> hotTasks() {
		return hotTasks;
	}

	@Override
	public boolean isTeleporting() {
		return teleporting;
	}

	public void clearHotTasks() {
		hotTasks.values().forEach(f -> {
			if (f != null && !f.isDone()) {
				f.cancel(false);
			}
		});
		hotTasks.clear();
	}

	/** Processa um pacote; devolve false quando a conexao deve ser fechada. */
	public boolean handle(byte[] body) {
		var decoded = GameClientPacket.decode(state, body);
		if (decoded.isEmpty()) {
			log.debug("Pacote malformado de {} no estado {}; fechando", ip, state);
			return false;
		}
		var pkt = decoded.get();
		if (ctx.packetRateLimiter() != null) {
			var rateCheck = ctx.packetRateLimiter().checkRate(rateState, pkt.getClass());
			if (!rateCheck.allowed()) {
				if (rateCheck.shouldSendActionFailed()) {
					send(new ActionFailed());
				}
				if (rateCheck.shouldDrop()) {
					return true;
				}
			}
		}
		switch (pkt) {
			case ProtocolVersion p -> onProtocolVersion(p);
			case AuthLogin p -> onAuthLogin(p);
			case NewCharacter p -> send(new NewCharacterSuccess(ctx.characters().creationTemplates()));
			case CharacterCreate p -> onCharacterCreate(p);
			case CharacterDelete p -> onCharacterDelete(p);
			case CharacterRestore p -> onCharacterRestore(p);
			case CharacterSelect p -> onCharacterSelect(p);
			case RequestManorList p -> send(new ExSendManorList(ExSendManorList.CASTLES));
			case EnterWorld p -> onEnterWorld();
			case GameClientPacket.RequestAppearing p -> onAppearing();
			case MoveBackwardToLocation p -> onMove(p);
			case ValidatePosition p -> onValidatePosition(p);
			case Say2 p -> onSay(p);
			case Action p -> onAction(p);
			case AttackRequest p -> onAttackRequest(p);
			case RequestBypassToServer p -> onBypass(p);
			case RequestBBSwrite p -> onBbsWrite(p);
			case SendBypassBuildCmd p -> handleAdminCommand(p.command());
			case RequestBuyItem p -> onBuyItem(p);
			case RequestSellItem p -> onSellItem(p);
			case RequestTargetCancel p -> onCancelTarget();
			case RequestActionUse p -> onActionUse(p);
			case RequestItemList p -> send(ItemList.of(active.inventory().items(), true));
			case UseItem p -> onUseItem(p);
			case RequestUnEquipItem p -> onUnEquip(p);
			case RequestShortCutReg p -> onShortCutReg(p);
			case RequestShortCutDel p -> onShortCutDel(p);
			case RequestMakeMacro p -> onMakeMacro(p);
			case RequestDeleteMacro p -> onDeleteMacro(p);
			case RequestMagicSkillUse p -> onMagicSkillUse(p);
			case RequestAutoSoulShot p -> onAutoSoulShot(p);
			case RequestSkillList p -> sendSkillList();
			case GameClientPacket.RequestAcquireSkillInfo p -> onAcquireSkillInfo(p);
			case RequestQuestList p -> {
				if (ctx.questManager() != null) {
					ctx.questManager().sendQuestList(this);
				} else {
					send(new QuestList(List.of()));
				}
			}
			case RequestTutorialLinkHtml p -> {
				if (ctx.questManager() != null) {
					ctx.questManager().onTutorialLink(this, p.link());
				}
			}
			case RequestTutorialPassCmdToServer p -> {
				if (ctx.questManager() != null) {
					ctx.questManager().onTutorialPassCmd(this, p.bypass());
				}
			}
			case RequestTutorialQuestionMark p -> {
				if (ctx.questManager() != null) {
					ctx.questManager().onTutorialQuestionMark(this, p.number());
				}
			}
			case RequestTutorialClientEvent p -> {
				if (ctx.questManager() != null) {
					ctx.questManager().onTutorialClientEvent(this, p.eventId());
				}
			}
			case Logout p -> onLogout();
			case RequestRestart p -> onRestart();
			case GameClientPacket.RequestExEnchantSkillInfo p -> onExEnchantSkillInfo(p);
			case GameClientPacket.RequestExEnchantSkill p -> onExEnchantSkill(p);
			case GameClientPacket.RequestJoinParty p -> onJoinParty(p);
			case GameClientPacket.RequestAnswerJoinParty p -> onAnswerJoinParty(p);
			case GameClientPacket.RequestSocialAction p -> onSocialAction(p);
			case GameClientPacket.RequestWithDrawalParty p -> onLeaveParty();
			case GameClientPacket.RequestOustPartyMember p -> onExpelPartyMember(p.name());
			case GameClientPacket.RequestShowMiniMap p -> onShowMiniMap();
			case GameClientPacket.RequestGetBossRecord p -> onGetBossRecord(p.bossId());
			case GameClientPacket.RequestRestartPoint p -> onRestartPoint(p);
			case GameClientPacket.RequestUserCommand p -> onUserCommand(p.commandId());
			case GameClientPacket.MultiSellChoose p -> onMultiSellChoose(p);
			case RequestEnchantItem p -> onEnchantItem(p);
			case SendWareHouseDepositList p -> onWareHouseDeposit(p);
			case SendWareHouseWithDrawList p -> onWareHouseWithdraw(p);
			case RequestDestroyItem p -> onDestroyItem(p);
			case GameClientPacket.RequestGMCommand p -> onGMCommand(p);
			case GameClientPacket.RequestPledgeCrest p -> onPledgeCrest(p);
			case GameClientPacket.RequestPledgeInfo p -> onPledgeInfo(p);
			case GameClientPacket.RequestPledgeMemberList p -> onPledgeMemberList();
			case GameClientPacket.RequestSetPledgeCrest p -> onSetPledgeCrest(p);
			case GameClientPacket.RequestHennaList p -> onHennaList();
			case GameClientPacket.RequestHennaItemInfo p -> onHennaItemInfo(p.symbolId());
			case GameClientPacket.RequestHennaEquip p -> onHennaEquip(p.symbolId());
			case GameClientPacket.RequestHennaUnequipList p -> onHennaUnequipList();
			case GameClientPacket.RequestHennaUnequipInfo p -> onHennaUnequipInfo(p.symbolId());
			case GameClientPacket.RequestHennaUnequip p -> onHennaUnequip(p.symbolId());
			case GameClientPacket.RequestCursedWeaponList p -> onCursedWeaponList();
			case GameClientPacket.RequestCursedWeaponLocation p -> onCursedWeaponLocation();
			case GameClientPacket.RequestSSQStatus p -> onSSQStatus(p.page());
			case GameClientPacket.RequestEvaluate p -> onEvaluate(p);
			case GameClientPacket.RequestFriendInvite p -> onFriendInvite(p);
			case GameClientPacket.RequestAnswerFriendInvite p -> onAnswerFriendInvite(p);
			case GameClientPacket.RequestFriendList p -> onFriendList();
			case GameClientPacket.RequestFriendDel p -> onFriendDel(p);
			case GameClientPacket.RequestBlock p -> onBlock(p);
			case GameClientPacket.RequestPrivateStoreManageSell p -> onPrivateStoreManageSell();
			case GameClientPacket.SetPrivateStoreListSell p -> onSetPrivateStoreListSell(p);
			case GameClientPacket.RequestPrivateStoreQuitSell p -> onPrivateStoreQuitSell();
			case GameClientPacket.SetPrivateStoreMsgSell p -> onSetPrivateStoreMsgSell(p);
			case GameClientPacket.RequestPrivateStoreBuy p -> onPrivateStoreBuy(p);
			case GameClientPacket.RequestConfirmTargetItem p -> onConfirmTargetItem(p);
			case GameClientPacket.RequestConfirmRefinerItem p -> onConfirmRefinerItem(p);
			case GameClientPacket.RequestConfirmGemStone p -> onConfirmGemStone(p);
			case GameClientPacket.RequestRefine p -> onRefine(p);
			case GameClientPacket.RequestConfirmCancelItem p -> onConfirmCancelItem(p);
			case GameClientPacket.RequestRefineCancel p -> onRefineCancel(p);
			case Unknown p -> log.debug("Opcode ignorado 0x{}{} no estado {}", Integer.toHexString(p.opcode()),
					p.subOpcode() >= 0 ? ":" + Integer.toHexString(p.subOpcode()) : "", state);
			default -> log.debug("Pacote {} recebido e nao tratado", decoded.get());
		}
		return !closeRequested;
	}

	/** Chame quando a conexao cair, por qualquer motivo. */
	public void onDisconnect() {
		if (active != null && active.isBuffShop() && ctx.buffShop() != null) {
			ctx.buffShop().getShop(active.objectId()).ifPresent(ctx.buffShop()::saveOfflineShop);
		}
		leaveWorld();
		if (account != null) {
			ctx.sessionKeys().logout(account);
			log.info("{} desconectou do game server", account);
			account = null;
		}
	}

	// ---- CONNECTED ----

	private void onProtocolVersion(ProtocolVersion p) {
		if (p.version() == -2 || p.version() == 65534) {
			log.debug("Cliente {} realizou ping no game server (protocolo -2)", ip);
			send(new KeyPacket(cryptKey, false));
			close(null);
			return;
		}
		if (p.version() < ctx.protocolMin() || p.version() > ctx.protocolMax()) {
			log.info("Cliente {} recusado: protocolo {} fora de {}-{}", ip, p.version(), ctx.protocolMin(),
					ctx.protocolMax());
			send(new KeyPacket(cryptKey, false));
			close(null);
			return;
		}
		protocolOk = true;
		send(new KeyPacket(cryptKey, true));
	}

	private void onAuthLogin(AuthLogin p) {
		if (!protocolOk || account != null) {
			close(null);
			return;
		}
		var key = new SessionKey(p.loginOk1(), p.loginOk2(), p.playOk1(), p.playOk2());
		if (!ctx.sessionKeys().claim(p.account(), key)) {
			log.warn("AuthLogin recusado para {} de {}: chave de sessao invalida ou expirada", p.account(), ip);
			close(null);
			return;
		}
		account = p.account();
		sessionId = p.playOk1();
		state = State.AUTHED;
		log.info("{} entrou no game server de {}", account, ip);
		if (ctx.world() != null) {
			for (var pOnline : ctx.world().players()) {
				if (pOnline != this && pOnline.account() != null && pOnline.account().equalsIgnoreCase(account)) {
					pOnline.send(SystemMessage.of(SystemMessage.DISCONNECTED_FROM_SERVER));
					pOnline.close(new ServerClose());
				}
			}
		}
		sendCharacterList();
	}

	// ---- AUTHED ----

	private void sendCharacterList() {
		characterList = ctx.characters().list(account);
		var paperdolls = characterList.stream().map(c -> ctx.inventories().paperdoll(c.objectId())).toList();
		send(CharSelectionInfo.of(account, sessionId, characterList, paperdolls));
	}

	private void onCharacterCreate(CharacterCreate p) {
		var result = ctx.characters().create(new CreateRequest(account, p.name(), p.race(), p.sex(), p.classId(),
				p.hairStyle(), p.hairColor(), p.face()));
		if (!result.ok()) {
			send(new CharCreateFail(result.failure()));
			return;
		}
		send(new CharCreateOk());
		sendCharacterList();
	}

	private PlayerCharacter slot(int slot) {
		return slot >= 0 && slot < characterList.size() ? characterList.get(slot) : null;
	}

	private void onCharacterDelete(CharacterDelete p) {
		PlayerCharacter c = slot(p.slot());
		if (c == null) {
			return;
		}
		ctx.characters().delete(c);
		send(new CharDeleteSuccess());
		sendCharacterList();
	}

	private void onCharacterRestore(CharacterRestore p) {
		PlayerCharacter c = slot(p.slot());
		if (c != null) {
			ctx.characters().restore(c);
		}
		sendCharacterList();
	}

	private void onCharacterSelect(CharacterSelect p) {
		PlayerCharacter c = slot(p.slot());
		if (c == null || active != null) {
			return;
		}
		if (ctx.world() != null) {
			var existing = ctx.world().byName(c.name()).orElse(null);
			if (existing != null) {
				existing.send(SystemMessage.of(SystemMessage.DISCONNECTED_FROM_SERVER));
				existing.close(new ServerClose());
			}
		}
		int accountAccess = (ctx.sessionKeys() != null && account != null)
				? ctx.sessionKeys().getAccessLevel(account)
				: 0;
		if (accountAccess > 0 && c.accessLevel() < accountAccess) {
			c.accessLevel(accountAccess);
		}

		boolean hasGmRight = Config.getBoolean("EveryoneHasAdminRights", false)
				|| Config.getBoolean("EveryoneIsGM", false)
				|| (account != null && (account.equalsIgnoreCase("admin") || account.equalsIgnoreCase("gm")
						|| account.equalsIgnoreCase("root") || account.equalsIgnoreCase("cristiano")
						|| account.toLowerCase().startsWith("admin")))
				|| (c.name() != null && (c.name().equalsIgnoreCase("Cristiano") || c.name().startsWith("Admin")
						|| c.name().startsWith("GM")));

		if (!hasGmRight && ctx.adminSuperusers() != null && !ctx.adminSuperusers().isEmpty()) {
			for (String su : ctx.adminSuperusers()) {
				if (su != null && (su.equalsIgnoreCase(c.name()) || su.equalsIgnoreCase(account))) {
					hasGmRight = true;
					break;
				}
			}
		}

		if (hasGmRight && c.accessLevel() < 100) {
			c.accessLevel(100);
		}

		var shutdownSvc = com.lopez.l2j.game.service.ShutdownService.getInstance();
		if (shutdownSvc != null) {
			if (shutdownSvc.isShuttingDown() && shutdownSvc.secondsRemaining() <= 10) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "O servidor esta reiniciando. Conexoes temporariamente suspensas."));
				close(new ServerClose());
				return;
			}
			if (shutdownSvc.isOnlyGm() && c.accessLevel() <= 0 && !hasGmRight) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Servidor em manutencao: apenas GMs podem conectar no momento."));
				close(new ServerClose());
				return;
			}
		}

		if (c.accessLevel() > 0) {
			ctx.characters().save(c, false);
		}
		active = c;
		c.inventory(ctx.inventories().load(c.objectId()));
		if (ctx.skillService() != null) {
			ctx.skillService().load(c);
		}
		state = State.IN_GAME;
		int skyState = ctx.sevenSigns() != null ? ctx.sevenSigns().getSkyState() - 256 : 0;
		send(new SsqInfo(skyState));
		send(new CharSelected(c, ctx.characters().template(c), sessionId, GameTime.now()));
	}

	// ---- IN_GAME ----

	private void onEnterWorld() {
		if (inWorld) {
			return;
		}
		inWorld = true;
		var t = ctx.characters().template(active);
		rewardSkills(t, false);
		send(ItemList.of(active.inventory().items(), false));
		send(new ShortCutInit(
				ctx.shortcuts() != null ? ctx.shortcuts().findByCharId(active.objectId(), 0) : List.of()));
		loadMacros();
		loadHennasForCurrentSubclass();
		send(new HennaInfo(active, getClassLevel(active), ctx.hennaTrees()));
		if (ctx.questManager() != null) {
			ctx.questManager().onPlayerLogin(this);
		} else {
			send(new QuestList(List.of()));
		}
		if (ctx.subClasses() != null && active != null) {
			active.setSubClasses(ctx.subClasses().loadSubClasses(active.objectId()));
		}
		send(ExStorageMaxCount.of(active));
		send(new FriendList());
		updateArmorSetBonus();
		updateEquippedItemSkills();
		updateAugmentationBonus();
		if (active != null) {
			if (ctx.hero() != null) {
				ctx.hero().loadHero(active);
			}
			if (ctx.vip() != null) {
				ctx.vip().loadVip(active);
			}
			if (ctx.aio() != null) {
				ctx.aio().loadAio(active);
			}
		}
		var initStats = t != null ? com.lopez.l2j.game.model.PlayerStats.calculate(active, t) : null;
		if (initStats != null) {
			currentWeightPenalty = initStats.weightPenalty();
			currentGradePenalty = initStats.gradePenalty();
		}
		if (t != null) {
			recalcMaxVitals(t);
			if (initStats != null) {
				send(new UserInfo(active, t, active.inventory().paperdollView(), active.inventory().currentLoad(), initStats));
			} else {
				send(new UserInfo(active, t));
			}
		}
		send(new ChangeMoveType(active.objectId(), active.running()));
		sendSkillList();
		send(new ClientSetTime(GameTime.now()));
		ctx.world().add(this);
		ctx.characters().save(active, true);
		restoreBuffs();
		startVitalsRegenTask();
		updateKnownObjects();
		if (active.isGm()) {
			if (Config.getBoolean("GMStartupInvisible", false)) {
				active.invis(true);
			}
			if (Config.getBoolean("GMStartupSilence", false)) {
				active.silence(true);
			}
			if (Config.getBoolean("GMStartupInvulnerable", false)) {
				active.invul(true);
			}
			send(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "SYS",
					"Privilegios de Administrador (GM Level " + active.accessLevel() + ") ativos. Digite //admin"));
		}
		send(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, ctx.serverName(),
				"Bem-vindo ao " + ctx.serverName() + ", " + active.name() + "!"));
		if (ctx.announcements() != null) {
			ctx.announcements().showToPlayer(this);
		}
		if (Config.SHOW_HTML_WELCOME && ctx.htmls() != null) {
			String welcome = ctx.htmls().getHtml("welcome.htm");
			if (welcome != null && !welcome.isBlank()) {
				String rendered = ctx.htmls().render(welcome, 0, ctx.serverName(), active.name());
				send(new NpcHtmlMessage(0, rendered));
			}
		}
		if (ctx.recommendations() != null) {
			ctx.recommendations().onPlayerEnter(active);
		}
		if (ctx.friends() != null) {
			ctx.friends().loadBlocks(active);
		}
		if (ctx.crowns() != null) {
			Clan playerClan = (ctx.clans() != null && active.clanId() > 0)
					? ctx.clans().byClanId(active.clanId()).orElse(null)
					: null;
			ctx.crowns().checkCrowns(active, playerClan);
		}
		if (ctx.siegeRewards() != null) {
			ctx.siegeRewards().claimRewards(active);
		}
		log.info("{} ({}) entrou no mundo em {},{},{}", active.name(), account, x(), y(), z());
	}

	public void onMove(MoveBackwardToLocation p) {
		actionHandler.handleMove(p);
	}

	public void onValidatePosition(ValidatePosition p) {
		actionHandler.handleValidatePosition(p);
	}

	public void checkZoneEnvironment() {
		if (!inWorld || active == null || active.isDead() || ctx.zones() == null) {
			return;
		}
		long now = System.currentTimeMillis();
		int px = active.x();
		int py = active.y();
		int pz = active.z();

		// V.28: Zona de Agua e Afogamento (Retail Interlude)
		boolean currentlyInWater = ctx.zones().isInsideWater(px, py, pz);
		if (currentlyInWater) {
			if (!inWater) {
				inWater = true;
				waterEntryTime = now;
				send(new GameServerPacket.SetupGauge(GameServerPacket.SetupGauge.CYAN, 60000));
			} else if (now - waterEntryTime > 60_000L) {
				if (now - lastDrownDamageTime >= 1000L) {
					lastDrownDamageTime = now;
					double drownDamage = Math.max(1.0, active.maxHp() * 0.05);
					double newHp = Math.max(0.0, active.currentHp() - drownDamage);
					active.currentHp(newHp);
					sendVitals();
					if (active.isDead()) {
						onPlayerDeath();
					}
				}
			}
		} else if (inWater) {
			inWater = false;
			waterEntryTime = 0L;
			lastDrownDamageTime = 0L;
			send(new GameServerPacket.SetupGauge(GameServerPacket.SetupGauge.CYAN, 0));
		}

		// V.29: Zonas de Dano Ambiental (Lava / Pantano Acido)
		if (ctx.zones().isInsideDamage(px, py, pz)) {
			if (now - lastDamageZoneTickTime >= 3000L) {
				lastDamageZoneTickTime = now;
				double envDamage = Math.max(10.0, active.maxHp() * 0.03);
				double newHp = Math.max(0.0, active.currentHp() - envDamage);
				active.currentHp(newHp);
				sendVitals();
				if (active.isDead()) {
					onPlayerDeath();
				}
			}
		}
	}

	private void onPlayerDeath() {
		handlePlayerDeath(null);
	}

	public void onAppearing() {
		if (!inWorld || active == null) {
			return;
		}
		teleporting = false;
		clearTarget();
		if (ctx != null && ctx.npcAi() != null) {
			ctx.npcAi().stopCombatForPlayer(active.objectId());
		}
		if (pendingRevive || active.isDead()) {
			pendingRevive = false;
			active.sitting(false);
			active.currentHp(active.maxHp() * 0.70);
			active.currentMp(active.maxMp() * 0.30);
			active.currentCp(0.0);

			var revive = new Revive(active.objectId());
			send(revive);
			ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, revive, false);
			var su = StatusUpdate.forPlayer(active);
			if (su != null) {
				send(su);
			}
		}
		broadcastAppearance();
		sendMagicEffectIcons();
		updateKnownObjects();
	}

	public void updateKnownObjects() {
		if (!inWorld || active == null || teleporting) {
			return;
		}
		int myX = x();
		int myY = y();
		int range = GameWorld.VISIBILITY_RADIUS;
		long r2 = (long) range * range;

		Set<Integer> currentAround = new HashSet<>();

		// 1. NPCs ao redor
		for (NpcInstance npc : ctx.world().findNpcsAround(myX, myY, range)) {
			int id = npc.objectId();
			currentAround.add(id);
			if (knownObjects.add(id)) {
				send(new NpcInfo(npc));
				if (npc.isDead()) {
					send(new Die(npc.objectId(), false));
				}
			}
		}

		// 2. Outros jogadores ao redor
		for (GameWorld.OnlinePlayer other : ctx.world().players()) {
			if (other.objectId() == active.objectId()) {
				continue;
			}
			long dx = other.x() - myX;
			long dy = other.y() - myY;
			if (dx * dx + dy * dy <= r2) {
				int id = other.objectId();
				currentAround.add(id);
				if (knownObjects.add(id)) {
					var otherInfo = other.charInfo();
					if (otherInfo != null) {
						send(otherInfo);
					}
					var myInfo = charInfo();
					if (myInfo != null) {
						other.send(myInfo);
					}
					if (other instanceof GameSession gs) {
						gs.knownObjects.add(active.objectId());
					}
				}
			}
		}

		// 3. Portas ao redor
		if (ctx.doors() != null) {
			for (DoorInstance door : ctx.doors().findDoorsAround(myX, myY, range)) {
				int id = door.objectId();
				currentAround.add(id);
				if (knownObjects.add(id)) {
					send(new DoorInfo(door));
					send(new DoorStatusUpdate(door));
				}
			}
		}

		// 4. Objetos estaticos ao redor (Town Map, Signboard, Tronos)
		if (ctx.staticObjects() != null) {
			for (var so : ctx.staticObjects().findAround(myX, myY, range)) {
				int id = so.objectId();
				currentAround.add(id);
				if (knownObjects.add(id)) {
					send(new StaticObject(so.staticObjectId(), so.objectId()));
				}
			}
		}

		// 5. Itens caidos no chao ao redor
		if (ctx.groundItems() != null) {
			for (var gi : ctx.groundItems().findAround(myX, myY, range)) {
				int id = gi.objectId();
				currentAround.add(id);
				if (knownObjects.add(id)) {
					boolean stackable = gi.itemInstance() != null && gi.itemInstance().template() != null
							&& gi.itemInstance().template().isStackable();
					send(new DropItem(gi.dropperObjectId(), gi.objectId(), gi.itemId(), gi.x(), gi.y(), gi.z(),
							stackable, gi.count()));
				}
			}
		}

		// 6. Remove objetos que sairam do alcance
		Iterator<Integer> it = knownObjects.iterator();
		while (it.hasNext()) {
			int id = it.next();
			if (!currentAround.contains(id)) {
				it.remove();
				send(new DeleteObject(id));
				var other = ctx.world().player(id).orElse(null);
				if (other instanceof GameSession gs) {
					if (gs.knownObjects.remove(active.objectId())) {
						gs.send(new DeleteObject(active.objectId()));
					}
				}
			}
		}
	}

	public void onSay(Say2 p) {
		chatHandler.handleSay(p);
	}

	private int getPhysicalAttackRange(PlayerCharacter player) {
		var weapon = player.inventory().paperdoll(ItemSlots.RHAND);
		if (weapon == null) {
			weapon = player.inventory().paperdoll(ItemSlots.LRHAND);
		}
		if (weapon != null && "bow".equalsIgnoreCase(weapon.template().subType())) {
			int range = 500;
			Integer longShotLvl = player.skills().get(113); // Long Shot
			if (longShotLvl != null && longShotLvl > 0) {
				range += longShotLvl * 200;
			}
			return range;
		}
		return 40;
	}

	public void onAction(Action p) {
		actionHandler.handleAction(p);
	}

	public void onAttackRequest(AttackRequest p) {
		actionHandler.handleAttackRequest(p);
	}

	public void startAutoAttack(NpcInstance npc) {
		if (casting || npc == null || npc.isDead() || !npc.template().isAttackable()) {
			send(new ActionFailed());
			return;
		}
		var t = ctx.characters() != null ? ctx.characters().template(active) : null;
		if (t != null) {
			var stats = com.lopez.l2j.game.model.PlayerStats.calculate(active, t);
			if (stats.weightPenalty() >= 3) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce esta sobrecarregado e nao pode atacar."));
				send(new ActionFailed());
				return;
			}
		}
		autoAttacking = true;
		onAttacking();

		double dx = active.x() - npc.x();
		double dy = active.y() - npc.y();
		double distSq = dx * dx + dy * dy;
		int attackRange = getPhysicalAttackRange(active);
		double maxDist = attackRange + 45.0;

		if (distSq > maxDist * maxDist) {
			var movePawn = new MoveToPawn(active.objectId(), npc.objectId(), attackRange, active.x(), active.y(),
					active.z());
			send(movePawn);
			ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, movePawn, false);
			schedulePlayerAutoAttack(npc, 200);
		} else {
			onAttackNpc(npc);
		}
	}

	public void startAutoAttack(GameSession targetPlayer) {
		if (casting || targetPlayer == null || targetPlayer.active == null || targetPlayer.active.isDead() || targetPlayer == this) {
			send(new ActionFailed());
			return;
		}
		// V.10: Protecao contra Auto-Ataque Amigo (Party / Clan sem guerra mutua)
		if (isFriendlyTarget(targetPlayer)) {
			send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
			send(new ActionFailed());
			return;
		}
		if (ctx.zones() != null && (ctx.zones().isInsidePeace(active.x(), active.y(), active.z())
				|| ctx.zones().isInsidePeace(targetPlayer.x(), targetPlayer.y(), targetPlayer.z()))) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao pode atacar dentro de uma zona de paz."));
			send(new ActionFailed());
			return;
		}
		var t = ctx.characters() != null ? ctx.characters().template(active) : null;
		if (t != null) {
			var stats = com.lopez.l2j.game.model.PlayerStats.calculate(active, t);
			if (stats.weightPenalty() >= 3) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce esta sobrecarregado e nao pode atacar."));
				send(new ActionFailed());
				return;
			}
		}
		autoAttacking = true;
		onAttacking();
		updatePvPFlag();

		double dx = active.x() - targetPlayer.x();
		double dy = active.y() - targetPlayer.y();
		double distSq = dx * dx + dy * dy;
		int attackRange = getPhysicalAttackRange(active);
		double maxDist = attackRange + 45.0;

		if (distSq > maxDist * maxDist) {
			var movePawn = new MoveToPawn(active.objectId(), targetPlayer.objectId(), attackRange, active.x(), active.y(),
					active.z());
			send(movePawn);
			ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, movePawn, false);
			schedulePlayerAutoAttack(targetPlayer, 200);
		} else {
			onAttackPlayer(targetPlayer);
		}
	}

	private boolean isFriendlyTarget(GameSession target) {
		if (target == null || target.active == null) {
			return false;
		}
		if (this.party != null && this.party == target.party) {
			return true;
		}
		if (active.clanId() > 0 && active.clanId() == target.active.clanId()) {
			return true;
		}
		return false;
	}

	@Override
	public void onAttacked(int attackerObjectId) {
		onAttacked(attackerObjectId, 0);
	}

	@Override
	public void onAttacked(int attackerObjectId, int damage) {
		if (active == null || active.isDead()) {
			return;
		}
		active.enterCombat();
		if (ctx.acpService() != null) {
			ctx.acpService().checkAndConsume(this);
		}
		var startAtk = new AutoAttackStart(active.objectId());
		send(startAtk);
		ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, startAtk, false);

		if (casting && castTask != null && !castTask.isDone()) {
			// V.08: Cancelamento de Cast por Dano Massivo (Retail Interlude / L2JDream)
			int maxHp = Math.max(1, active.maxHp());
			double ratio = (double) damage / maxHp;
			boolean shouldBreak = false;
			if (ratio >= 0.5) {
				shouldBreak = true;
			} else if (ratio >= 0.02) {
				int breakChance = (int) Math.round(ratio * 150.0);
				shouldBreak = ThreadLocalRandom.current().nextInt(100) < breakChance;
			}
			if (shouldBreak) {
				casting = false;
				castingSkill = null;
				castTask.cancel(false);
				castTask = null;
				var cancel = new MagicSkillCanceld(active.objectId());
				send(cancel);
				ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, cancel, false);
				send(new ActionFailed());
				send(SystemMessage.id(SystemMessage.CASTING_INTERRUPTED));
			}
		}

		if (combatStanceTask != null && !combatStanceTask.isDone()) {
			combatStanceTask.cancel(false);
		}
		combatStanceTask = autoAttackScheduler.schedule(() -> {
			if (active != null && !autoAttacking && !active.isInCombat()) {
				var stopAtk = new AutoAttackStop(active.objectId());
				send(stopAtk);
				ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, stopAtk, false);
			}
		}, 15_000L, TimeUnit.MILLISECONDS);
	}

	public void onAttacking() {
		if (active == null || active.isDead()) {
			return;
		}
		active.enterCombat();
		var startAtk = new AutoAttackStart(active.objectId());
		send(startAtk);
		ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, startAtk, false);

		if (combatStanceTask != null && !combatStanceTask.isDone()) {
			combatStanceTask.cancel(false);
		}
		combatStanceTask = autoAttackScheduler.schedule(() -> {
			if (active != null && !autoAttacking && !active.isInCombat()) {
				var stopAtk = new AutoAttackStop(active.objectId());
				send(stopAtk);
				ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, stopAtk, false);
			}
		}, 15_000L, TimeUnit.MILLISECONDS);
	}

	public void stopAutoAttack() {
		if (autoAttacking) {
			autoAttacking = false;
			if (active != null && !active.isInCombat()) {
				var stopAtk = new AutoAttackStop(active.objectId());
				send(stopAtk);
				ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, stopAtk, false);
			}
		}
	}

	public void checkAutoAttackRangeOnMove() {
		if (!autoAttacking || targetObjectId == 0 || active == null) {
			return;
		}
		var npcOpt = ctx.world().npc(targetObjectId);
		if (npcOpt.isPresent()) {
			var npc = npcOpt.get();
			if (npc.template().isAttackable() && !npc.isDead()) {
				double dx = active.x() - npc.x();
				double dy = active.y() - npc.y();
				double distSq = dx * dx + dy * dy;
				int attackRange = getPhysicalAttackRange(active);
				double maxDist = attackRange + 45.0;
				if (distSq <= maxDist * maxDist && System.currentTimeMillis() >= attackEndTime) {
					onAttackNpc(npc);
				}
			}
			return;
		}
		var playerOpt = ctx.world().player(targetObjectId);
		if (playerOpt.isPresent() && playerOpt.get() instanceof GameSession targetSession && targetSession != this) {
			if (targetSession.active != null && !targetSession.active.isDead()) {
				double dx = active.x() - targetSession.x();
				double dy = active.y() - targetSession.y();
				double distSq = dx * dx + dy * dy;
				int attackRange = getPhysicalAttackRange(active);
				double maxDist = attackRange + 45.0;
				if (distSq <= maxDist * maxDist && System.currentTimeMillis() >= attackEndTime) {
					onAttackPlayer(targetSession);
				}
			}
		}
	}

	public void onCancelTarget() {
		actionHandler.handleCancelTarget();
	}

	private void onAttackNpc(NpcInstance npc) {
		if (npc.isDead()) {
			stopAutoAttack();
			return;
		}
		if (ctx.combat() != null && !ctx.combat().canSeeTarget(active, npc)) {
			send(SystemMessage.id(SystemMessage.CANT_SEE_TARGET));
			send(new ActionFailed());
			stopAutoAttack();
			return;
		}
		if (ctx.combat() == null) {
			send(new ActionFailed());
			return;
		}
		long now = System.currentTimeMillis();
		if (now < attackEndTime) {
			// Ja esta executando ataque: mantem auto-attack sem enviar ActionFailed
			autoAttacking = true;
			return;
		}
		var t = ctx.characters().template(active);

		// Flechas para arco: checa e consome antes do disparo
		if (!checkAndConsumeArrow()) {
			stopAutoAttack();
			return;
		}

		var stats = PlayerStats.calculate(active, t);
		int pAtkSpd = Math.max(100, stats.pAtkSpd());
		int timeAtk = (int) (500_000L / pAtkSpd);
		boolean bow = isBow(activeWeapon());
		int timeToHit = bow ? (int) (timeAtk * 0.70) : (int) (timeAtk * 0.50);
		attackEndTime = now + timeAtk;

		// Vira de frente para o alvo
		int heading = (int) Math.round(Math.atan2(npc.y() - active.y(), npc.x() - active.x()) * 10430.378);
		active.heading(heading);

		if (ctx.combat() != null && ctx.combat().checkRaidCurse(active, npc)) {
			active.effects().addBuff(com.lopez.l2j.game.combat.CombatService.RAID_CURSE_PETRIFY, 1, 120_000L);
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce foi atingido pela Maldicao de Raid Boss (Raid Curse)!"));
			stopAutoAttack();
			send(new ActionFailed());
			return;
		}

		autoAttacking = true;
		onAttacking();
		if (ctx.npcAi() != null) {
			ctx.npcAi().startCombat(npc, active.objectId());
		}

		// Soulshot
		if (!soulshotCharged) {
			rechargeAutoSoulShots();
		}
		int ssGrade = soulshotCharged ? chargedGrade : -1;
		soulshotCharged = false;

		// Planeja dano/flags SEM aplicar ao HP do monstro antes do impacto da animacao
		var plan = ctx.combat().planAttackNpc(active, t, npc, ssGrade);

		var atk = new Attack(active.objectId(), npc.objectId(), plan.damage(), plan.flags(), active.x(), active.y(),
				active.z());
		send(atk);
		ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, atk, false);

		// Agenda a aplicacao do dano e atualizacoes no momento exato do impacto
		// (timeToHit)
		autoAttackScheduler.schedule(() -> {
			if (!inWorld || active == null || active.isDead() || npc == null) {
				return;
			}
			if (npc.isDead()) {
				stopAutoAttack();
				return;
			}

			var hit = ctx.combat().applyDamage(npc, plan.damage(), plan.flags());

			if (hit.damage() > 0) {
				send(SystemMessage.of(SystemMessage.YOU_DID_S1_DMG, new SystemMessage.Number(hit.damage())));
			}
			if (plan.crit() && !npc.isDead()) {
				triggerWeaponOnCritSkill(npc, null);
			}

			var su = StatusUpdate.hp(npc.objectId(), hit.remainingHp(), hit.maxHp());
			send(su);
			ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, su, false);

			if (hit.isDead()) {
				stopAutoAttack();

				if (ctx.npcAi() != null) {
					ctx.npcAi().stopCombat(npc);
					ctx.npcAi().scheduleDecayAndRespawn(npc);
				}

				if (com.lopez.l2j.game.boss.BossManager.getInstance() != null) {
					com.lopez.l2j.game.boss.BossManager.getInstance().onBossKilled(npc, active, party);
				}

				var die = new Die(npc.objectId(), false);
				send(die);
				ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, die, false);

				if (party != null) {
					double ratePartyXp = ctx.rates() != null ? ctx.rates().partyXp() : 1.0;
					double ratePartySp = ctx.rates() != null ? ctx.rates().partySp() : 1.0;
					party.distributeExpAndSp(hit.expReward(), hit.spReward(), active, ratePartyXp, ratePartySp);
				} else {
					applyExpAndSp(hit.expReward(), hit.spReward(), t);
				}

				if (npc.isSpoiled() && ctx.drops() != null) {
					var spoilDrops = ctx.drops().rollSpoil(npc.npcId(), active.level(),
							npc.template() != null ? npc.template().level() : 0);
					npc.spoilRewards(spoilDrops);
				}

				if (ctx.drops() != null) {
					ctx.drops().rewardMonsterDeath(active, npc, ctx.inventories(), this::send);
				}

				if (ctx.questManager() != null) {
					ctx.questManager().onNpcKill(npc, this, false);
				}

				if (ctx.raidPoints() != null && npc.template() != null && npc.template().isRaidBoss()) {
					int lvl = npc.template().level();
					int points = Math.max(1, lvl / 2 + java.util.concurrent.ThreadLocalRandom.current().nextInt(-5, 6));
					if (party != null) {
						for (GameSession member : party.members()) {
							if (member != null && member.active != null) {
								double dist = Math.hypot(member.active.x() - active.x(), member.active.y() - active.y());
								if (dist <= com.lopez.l2j.game.party.Party.getPartyRange()) {
									ctx.raidPoints().addPoints(member.active.objectId(), npc.npcId(), points);
									member.send(SystemMessage.of(SystemMessage.EARNED_S1_RAID_POINTS, new SystemMessage.Number(points)));
								}
							}
						}
					} else {
						ctx.raidPoints().addPoints(active.objectId(), npc.npcId(), points);
						send(SystemMessage.of(SystemMessage.EARNED_S1_RAID_POINTS, new SystemMessage.Number(points)));
					}
				}

				if (npc.npcId() == 25325 && Config.KILL_BARAKIEL_SET_NOBLESS) {
					rewardBarakielNoblesse(party, active);
				}

				ctx.characters().save(active, true);
				saveBuffs();
			} else {
				if (ctx.npcAi() != null) {
					ctx.npcAi().startCombat(npc, active.objectId());
				}
				schedulePlayerAutoAttack(npc, Math.max(50, timeAtk - timeToHit));
			}
		}, timeToHit, TimeUnit.MILLISECONDS);
	}

	private void schedulePlayerAutoAttack(NpcInstance npc) {
		schedulePlayerAutoAttack(npc, 100);
	}

	private void schedulePlayerAutoAttack(NpcInstance npc, long delayMs) {
		if (!autoAttacking || active == null || npc == null || npc.isDead()) {
			return;
		}
		autoAttackScheduler.schedule(() -> {
			if (!autoAttacking || active == null || active.isDead() || !inWorld || targetObjectId != npc.objectId() || npc.isDead()) {
				return;
			}
			double dx = active.x() - npc.x();
			double dy = active.y() - npc.y();
			double distSq = dx * dx + dy * dy;
			int attackRange = getPhysicalAttackRange(active);
			double maxDist = attackRange + 45.0;
			if (distSq > maxDist * maxDist) {
				var movePawn = new MoveToPawn(active.objectId(), npc.objectId(), attackRange, active.x(), active.y(),
						active.z());
				send(movePawn);
				ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, movePawn, false);
				schedulePlayerAutoAttack(npc, 250);
			} else {
				onAttackNpc(npc);
			}
		}, delayMs, TimeUnit.MILLISECONDS);
	}

	private void schedulePlayerAutoAttack(GameSession targetPlayer) {
		schedulePlayerAutoAttack(targetPlayer, 100);
	}

	private void schedulePlayerAutoAttack(GameSession targetPlayer, long delayMs) {
		if (!autoAttacking || active == null || targetPlayer == null || targetPlayer.active == null || targetPlayer.active.isDead()) {
			return;
		}
		autoAttackScheduler.schedule(() -> {
			if (!autoAttacking || active == null || active.isDead() || !inWorld || targetObjectId != targetPlayer.objectId() || targetPlayer.active == null || targetPlayer.active.isDead()) {
				return;
			}
			double dx = active.x() - targetPlayer.x();
			double dy = active.y() - targetPlayer.y();
			double distSq = dx * dx + dy * dy;
			int attackRange = getPhysicalAttackRange(active);
			double maxDist = attackRange + 45.0;
			if (distSq > maxDist * maxDist) {
				var movePawn = new MoveToPawn(active.objectId(), targetPlayer.objectId(), attackRange, active.x(), active.y(),
						active.z());
				send(movePawn);
				ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, movePawn, false);
				schedulePlayerAutoAttack(targetPlayer, 250);
			} else {
				onAttackPlayer(targetPlayer);
			}
		}, delayMs, TimeUnit.MILLISECONDS);
	}

	private void onAttackPlayer(GameSession targetPlayer) {
		if (targetPlayer == null || targetPlayer.active == null || targetPlayer.active.isDead()) {
			stopAutoAttack();
			return;
		}
		if (ctx.combat() != null && !ctx.combat().canSeeTarget(active, targetPlayer.active)) {
			send(SystemMessage.id(SystemMessage.CANT_SEE_TARGET));
			send(new ActionFailed());
			stopAutoAttack();
			return;
		}
		if (ctx.combat() == null) {
			send(new ActionFailed());
			return;
		}
		long now = System.currentTimeMillis();
		if (now < attackEndTime) {
			autoAttacking = true;
			return;
		}
		var t = ctx.characters().template(active);
		if (!checkAndConsumeArrow()) {
			stopAutoAttack();
			return;
		}

		var stats = PlayerStats.calculate(active, t);
		int pAtkSpd = Math.max(100, stats.pAtkSpd());
		int timeAtk = (int) (500_000L / pAtkSpd);
		boolean bow = isBow(activeWeapon());
		int timeToHit = bow ? (int) (timeAtk * 0.70) : (int) (timeAtk * 0.50);
		attackEndTime = now + timeAtk;

		int heading = (int) Math.round(Math.atan2(targetPlayer.y() - active.y(), targetPlayer.x() - active.x()) * 10430.378);
		active.heading(heading);

		if (!autoAttacking) {
			autoAttacking = true;
			var startAtk = new AutoAttackStart(active.objectId());
			send(startAtk);
			ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, startAtk, false);
		}
		updatePvPFlag();

		if (!soulshotCharged) {
			rechargeAutoSoulShots();
		}
		int ssGrade = soulshotCharged ? chargedGrade : -1;
		soulshotCharged = false;

		var tgtTemplate = ctx.characters().template(targetPlayer.active);
		var plan = ctx.combat().planAttackPlayer(active, t, targetPlayer.active, tgtTemplate, ssGrade);

		var atk = new Attack(active.objectId(), targetPlayer.objectId(), plan.damage(), plan.flags(), active.x(), active.y(),
				active.z());
		send(atk);
		ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, atk, false);

		autoAttackScheduler.schedule(() -> {
			if (!inWorld || active == null || active.isDead() || targetPlayer.active == null) {
				return;
			}
			if (targetPlayer.active.isDead()) {
				stopAutoAttack();
				return;
			}
			var dmgRes = ctx.combat().applyDamagePlayer(targetPlayer.active, plan.damage());
			if (dmgRes.damage() > 0) {
				send(SystemMessage.of(SystemMessage.YOU_DID_S1_DMG, new SystemMessage.Number(dmgRes.damage())));
				targetPlayer.send(SystemMessage.of(SystemMessage.S1_GAVE_YOU_S2_DMG, new SystemMessage.Text(active.name()),
						new SystemMessage.Number(dmgRes.damage())));
			}
			if (plan.crit() && !targetPlayer.active.isDead()) {
				triggerWeaponOnCritSkill(null, targetPlayer);
			}
			targetPlayer.sendVitals();
			var su = StatusUpdate.hp(targetPlayer.objectId(), dmgRes.remainingHp(), targetPlayer.active.maxHp());
			send(su);
			ctx.world().broadcastAround(targetPlayer, GameWorld.VISIBILITY_RADIUS, su, false);

			if (dmgRes.isDead()) {
				stopAutoAttack();
				targetPlayer.handlePlayerDeath(active);
			} else {
				if (!targetPlayer.autoAttacking) {
					var startAtkTgt = new AutoAttackStart(targetPlayer.objectId());
					targetPlayer.send(startAtkTgt);
					ctx.world().broadcastAround(targetPlayer, GameWorld.VISIBILITY_RADIUS, startAtkTgt, false);
				}
				schedulePlayerAutoAttack(targetPlayer, Math.max(50, timeAtk - timeToHit));
			}
		}, timeToHit, TimeUnit.MILLISECONDS);
	}

	public void applyExpAndSp(long expReward, int spReward) {
		if (active != null) {
			var t = ctx.characters() != null ? ctx.characters().template(active) : null;
			applyExpAndSp(expReward, spReward, t);
		}
	}

	private void applyExpAndSp(long expReward, int spReward, CharTemplate t) {
		if (active == null || (expReward <= 0 && spReward <= 0)) {
			return;
		}
		active.exp(active.exp() + expReward);
		active.sp((int) Math.min(Integer.MAX_VALUE, (long) active.sp() + spReward));

		// Notificacao de EXP e SP recebidos (sempre enviada no L2)
		if (expReward > 0 && spReward > 0) {
			send(SystemMessage.of(SystemMessage.YOU_EARNED_S1_EXP_AND_S2_SP,
					new SystemMessage.Number((int) expReward),
					new SystemMessage.Number(spReward)));
		} else if (expReward > 0) {
			send(SystemMessage.of(SystemMessage.EARNED_S1_EXPERIENCE,
					new SystemMessage.Number((int) expReward)));
		} else if (spReward > 0) {
			send(SystemMessage.of(SystemMessage.ACQUIRED_S1_SP,
					new SystemMessage.Number(spReward)));
		}

		int oldLevel = active.level();
		int newLevel = ExperienceTable.calculateLevel(active.exp());

		if (newLevel > oldLevel) {
			active.level(newLevel);
			if (t != null) {
				rewardSkills(t, true); // novos skills (AutoLearn/Expertise) + max HP/MP/CP com passivas
			}
			active.currentHp(active.maxHp());
			active.currentMp(active.maxMp());
			active.currentCp(active.maxCp());

			var social = new SocialAction(active.objectId(), 15);
			send(social);
			ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, social, false);

			send(SystemMessage.of(SystemMessage.YOU_INCREASED_YOUR_LEVEL));

			if (ctx.questManager() != null) {
				ctx.questManager().onPlayerLevelUp(this, oldLevel, newLevel);
			}

			refreshWeightAndPenalties();
			sendUserInfoAndBroadcastCharInfo();
			send(new StatusUpdate(active.objectId(), List.of(
					new StatusUpdate.Attribute(StatusUpdate.LEVEL, newLevel),
					new StatusUpdate.Attribute(StatusUpdate.EXP, (int) active.exp()),
					new StatusUpdate.Attribute(StatusUpdate.SP, active.sp()),
					new StatusUpdate.Attribute(StatusUpdate.CUR_HP, (int) active.currentHp()),
					new StatusUpdate.Attribute(StatusUpdate.MAX_HP, active.maxHp()),
					new StatusUpdate.Attribute(StatusUpdate.CUR_MP, (int) active.currentMp()),
					new StatusUpdate.Attribute(StatusUpdate.MAX_MP, active.maxMp()),
					new StatusUpdate.Attribute(StatusUpdate.CUR_CP, (int) active.currentCp()),
					new StatusUpdate.Attribute(StatusUpdate.MAX_CP, active.maxCp()))));
		} else {
			if (t == null && ctx.characters() != null) {
				t = ctx.characters().template(active);
			}
			if (t != null) {
				send(new UserInfo(active, t));
			}
			send(new StatusUpdate(active.objectId(), List.of(
					new StatusUpdate.Attribute(StatusUpdate.EXP, (int) active.exp()),
					new StatusUpdate.Attribute(StatusUpdate.SP, active.sp()))));
		}
	}

	public void showNpcHtml(NpcInstance npc, int val) {
		if (com.lopez.l2j.game.boss.BossManager.getInstance() != null
				&& com.lopez.l2j.game.boss.BossManager.getInstance().showBossNpcHtml(active, npc, p -> { if (p instanceof GameServerPacket gsp) send(gsp); })) {
			return;
		}
		if (isSevenSignsPriest(npc.npcId(), npc.name())) {
			showSevenSignsNpcHtml(npc, val);
			return;
		}
		if (npc.npcId() == 31111) {
			showGatekeeperSpiritInHtml(npc);
			return;
		}
		if (npc.npcId() == 31112) {
			showGatekeeperSpiritOutHtml(npc);
			return;
		}
		if (npc.npcId() == 31092) {
			showBlackMarketeerHtml(npc);
			return;
		}
		if (npc.npcId() == 31113) {
			showMerchantOfMammonHtml(npc);
			return;
		}
		if (npc.npcId() == 31126) {
			showBlacksmithOfMammonHtml(npc);
			return;
		}
		if ((npc.npcId() >= 31127 && npc.npcId() <= 31136) || (npc.npcId() >= 31137 && npc.npcId() <= 31146)) {
			showFestivalNpcHtml(npc, val);
			return;
		}
		int castleId = getNpcCastleId(npc);
		if (ctx.htmls() != null) {
			String raw = ctx.htmls().getNpcHtml(npc.npcId(), npc.template().type(), val);
			boolean isDefault = raw == null || raw.contains("I have nothing to say to you") || raw.contains("npcdefault.htm");
			if (!isDefault) {
				String rendered = ctx.htmls().render(raw, npc.objectId(), npc.name(), active != null ? active.name() : "Player", castleId, npc.npcId());
				send(new NpcHtmlMessage(npc.objectId(), rendered));
				return;
			}
		}

		if (ctx.questManager() != null) {
			String qHtm = ctx.questManager().onNpcTalk(npc, this);
			if (qHtm != null && !qHtm.isBlank()) {
				String resolved = resolveQuestHtml(null, qHtm, npc);
				if (resolved != null && !resolved.isBlank()) {
					String rendered = ctx.htmls() != null
							? ctx.htmls().render(resolved, npc.objectId(), npc.name(), active != null ? active.name() : "Player", castleId, npc.npcId())
							: resolved;
					send(new NpcHtmlMessage(npc.objectId(), rendered));
					return;
				}
			}
		}

		if (ctx.htmls() != null) {
			String raw = ctx.htmls().getNpcHtml(npc.npcId(), npc.template().type(), val);
			String rendered = ctx.htmls().render(raw, npc.objectId(), npc.name(), active != null ? active.name() : "Player", castleId, npc.npcId());
			send(new NpcHtmlMessage(npc.objectId(), rendered));
		}
	}

	private boolean isSevenSignsPriest(int npcId, String name) {
		if (com.lopez.l2j.game.html.HtmCache.isSevenSignsNpc(npcId)) {
			return true;
		}
		if (name != null) {
			String lower = name.toLowerCase(java.util.Locale.ROOT);
			return lower.contains("dawn") || lower.contains("dusk");
		}
		return false;
	}

	private void showSevenSignsNpcHtml(NpcInstance npc, int val) {
		if (active == null) {
			send(new ActionFailed());
			return;
		}
		boolean isDawn = (npc.npcId() >= 31078 && npc.npcId() <= 31084)
				|| npc.npcId() == 31168 || npc.npcId() == 31692 || npc.npcId() == 31694 || npc.npcId() == 31997
				|| (npc.name() != null && npc.name().toLowerCase(java.util.Locale.ROOT).contains("dawn"));

		var ss = ctx.sevenSigns();
		int playerCabal = com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL;
		int activePeriod = com.lopez.l2j.game.sevensigns.SevenSignsManager.PERIOD_COMPETITION;
		int compWinner = com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL;
		int sealGnosisOwner = com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL;

		if (ss != null) {
			activePeriod = ss.activePeriod();
			compWinner = ss.previousWinner();
			sealGnosisOwner = ss.gnosisOwner();
			var pData = ss.getPlayerData(active.objectId()).orElse(null);
			if (pData != null) {
				playerCabal = pData.cabal();
			}
		}

		String prefix = isDawn ? "dawn_priest_" : "dusk_priest_";
		String file;

		if (isDawn) {
			if (playerCabal == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN) {
				if (activePeriod == com.lopez.l2j.game.sevensigns.SevenSignsManager.PERIOD_COMP_RESULTS) {
					file = prefix + "5.htm";
				} else if (activePeriod == com.lopez.l2j.game.sevensigns.SevenSignsManager.PERIOD_SEAL_VALIDATION) {
					if (compWinner == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN) {
						file = (compWinner != sealGnosisOwner) ? prefix + "2c.htm" : prefix + "2a.htm";
					} else {
						file = prefix + "2b.htm";
					}
				} else {
					file = prefix + "1b.htm";
				}
			} else if (playerCabal == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK) {
				file = prefix + "3a.htm";
			} else {
				if (activePeriod == com.lopez.l2j.game.sevensigns.SevenSignsManager.PERIOD_COMP_RESULTS) {
					file = prefix + "5.htm";
				} else if (activePeriod == com.lopez.l2j.game.sevensigns.SevenSignsManager.PERIOD_SEAL_VALIDATION) {
					file = (compWinner == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN) ? prefix + "4.htm" : prefix + "2b.htm";
				} else {
					file = prefix + "1a.htm";
				}
			}
		} else {
			if (playerCabal == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK) {
				if (activePeriod == com.lopez.l2j.game.sevensigns.SevenSignsManager.PERIOD_COMP_RESULTS) {
					file = prefix + "5.htm";
				} else if (activePeriod == com.lopez.l2j.game.sevensigns.SevenSignsManager.PERIOD_SEAL_VALIDATION) {
					if (compWinner == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK) {
						file = (compWinner != sealGnosisOwner) ? prefix + "2c.htm" : prefix + "2a.htm";
					} else {
						file = prefix + "2b.htm";
					}
				} else {
					file = prefix + "1b.htm";
				}
			} else if (playerCabal == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN) {
				file = prefix + "3a.htm";
			} else {
				if (activePeriod == com.lopez.l2j.game.sevensigns.SevenSignsManager.PERIOD_COMP_RESULTS) {
					file = prefix + "5.htm";
				} else if (activePeriod == com.lopez.l2j.game.sevensigns.SevenSignsManager.PERIOD_SEAL_VALIDATION) {
					file = (compWinner == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK) ? prefix + "4.htm" : prefix + "2b.htm";
				} else {
					file = prefix + "1a.htm";
				}
			}
		}

		String path = "seven_signs/" + file;
		String htm = ctx.htmls() != null ? ctx.htmls().getHtml(path) : null;
		if (htm == null && ctx.htmls() != null) {
			htm = ctx.htmls().getIndexedHtml(file);
		}
		if (htm != null && ctx.htmls() != null) {
			String rendered = ctx.htmls().render(htm, npc.objectId(), npc.name(), active.name());
			send(new NpcHtmlMessage(npc.objectId(), rendered));
		} else {
			send(new NpcHtmlMessage(npc.objectId(), "<html><body>" + npc.name() + ":<br>The Seven Signs competition is underway.</body></html>"));
		}
	}

	private void showGatekeeperSpiritInHtml(NpcInstance npc) {
		if (active == null) {
			send(new ActionFailed());
			return;
		}
		var ss = ctx.sevenSigns();
		if (ss == null || !ss.isSealValidationPeriod()) {
			String raw = ctx.htmls() != null ? ctx.htmls().getHtml("seven_signs/spirit_null.htm") : null;
			if (raw != null) {
				send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(raw, npc.objectId(), npc.name(), active.name())));
			} else {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao pode passar neste momento."));
			}
			return;
		}
		int winner = ss.previousWinner();
		int avarice = ss.avariceOwner();
		int playerCabal = ss.getPlayerCabal(active.objectId());

		if (winner == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN
				&& avarice == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN
				&& playerCabal == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN) {
			String raw = ctx.htmls() != null ? ctx.htmls().getHtml("seven_signs/spirit_dawn.htm") : null;
			if (raw != null) {
				send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(raw, npc.objectId(), npc.name(), active.name())));
				return;
			}
		} else if (winner == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK
				&& avarice == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK
				&& playerCabal == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK) {
			String raw = ctx.htmls() != null ? ctx.htmls().getHtml("seven_signs/spirit_dusk.htm") : null;
			if (raw != null) {
				send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(raw, npc.objectId(), npc.name(), active.name())));
				return;
			}
		}
		String raw = ctx.htmls() != null ? ctx.htmls().getHtml("seven_signs/spirit_null.htm") : null;
		if (raw != null) {
			send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(raw, npc.objectId(), npc.name(), active.name())));
		} else {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao possui permissao para passar."));
		}
	}

	private void showGatekeeperSpiritOutHtml(NpcInstance npc) {
		if (active == null) {
			send(new ActionFailed());
			return;
		}
		String raw = ctx.htmls() != null ? ctx.htmls().getHtml("seven_signs/spirit_exit.htm") : null;
		if (raw != null) {
			send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(raw, npc.objectId(), npc.name(), active.name())));
		} else {
			teleportToCoordinates(172373, -17833, -4901);
		}
	}

	private void showMerchantOfMammonHtml(NpcInstance npc) {
		if (active == null) {
			send(new ActionFailed());
			return;
		}
		var ss = ctx.sevenSigns();
		int playerCabal = ss != null ? ss.getPlayerCabal(active.objectId()) : com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL;
		int compWinner = ss != null ? ss.previousWinner() : com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL;
		int avariceOwner = ss != null ? ss.avariceOwner() : com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL;
		boolean isValidation = ss != null && ss.isSealValidationPeriod();

		boolean canAccess = active.isGm() || (isValidation && playerCabal != com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL
				&& playerCabal == compWinner && avariceOwner == compWinner);
		String file = canAccess ? "seven_signs/mammmerch_1.htm" : "seven_signs/mammmerch_2.htm";
		String raw = ctx.htmls() != null ? ctx.htmls().getHtml(file) : null;
		if (raw != null) {
			send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(raw, npc.objectId(), npc.name(), active.name())));
		} else {
			send(new CreatureSay(0, CreatureSay.ALL, npc.name(), canAccess ? "May the blessings of Mammon be with you." : "Only members of the winning cabal possessing the Seal of Avarice may trade."));
		}
	}

	private void showBlacksmithOfMammonHtml(NpcInstance npc) {
		if (active == null) {
			send(new ActionFailed());
			return;
		}
		var ss = ctx.sevenSigns();
		int playerCabal = ss != null ? ss.getPlayerCabal(active.objectId()) : com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL;
		int compWinner = ss != null ? ss.previousWinner() : com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL;
		int gnosisOwner = ss != null ? ss.gnosisOwner() : com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL;
		boolean isValidation = ss != null && ss.isSealValidationPeriod();

		boolean canAccess = active.isGm() || (isValidation && playerCabal != com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL
				&& playerCabal == compWinner && gnosisOwner == compWinner);
		String file = canAccess ? "seven_signs/mammblack_1.htm" : "seven_signs/mammblack_2.htm";
		String raw = ctx.htmls() != null ? ctx.htmls().getHtml(file) : null;
		if (raw != null) {
			send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(raw, npc.objectId(), npc.name(), active.name())));
		} else {
			send(new CreatureSay(0, CreatureSay.ALL, npc.name(), canAccess ? "I can forge the finest weapons and armor for you." : "Only members of the winning cabal possessing the Seal of Gnosis may use my smithing services."));
		}
	}

	private void showBlackMarketeerHtml(NpcInstance npc) {
		if (active == null) {
			send(new ActionFailed());
			return;
		}
		String raw = ctx.htmls() != null ? ctx.htmls().getHtml("seven_signs/blkmrkt_1.htm") : null;
		if (raw != null) {
			send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(raw, npc.objectId(), npc.name(), active.name())));
		} else {
			send(new CreatureSay(0, CreatureSay.ALL, npc.name(), "Welcome to the black market."));
		}
	}

	private void showFestivalNpcHtml(NpcInstance npc, int val) {
		if (active == null) {
			send(new ActionFailed());
			return;
		}
		boolean isWitch = (npc.npcId() >= 31132 && npc.npcId() <= 31136) || (npc.npcId() >= 31142 && npc.npcId() <= 31146);
		if (isWitch) {
			String raw = ctx.htmls() != null ? ctx.htmls().getHtml("seven_signs/festival/festival_witch.htm") : null;
			if (raw != null) {
				send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(raw, npc.objectId(), npc.name(), active.name())));
			} else {
				send(new CreatureSay(0, CreatureSay.ALL, npc.name(), "Derrote monstros para coletar oferendas de sangue!"));
			}
			return;
		}

		boolean isDawn = (npc.npcId() >= 31127 && npc.npcId() <= 31131);
		int tier = isDawn ? Math.max(0, Math.min(4, npc.npcId() - 31127)) : Math.max(0, Math.min(4, npc.npcId() - 31137));
		var ss = ctx.sevenSigns();

		if (ss != null && ss.isSealValidationPeriod()) {
			String raw = ctx.htmls() != null ? ctx.htmls().getHtml("seven_signs/festival/festival_2a.htm") : null;
			if (raw != null) {
				send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(raw, npc.objectId(), npc.name(), active.name())));
				return;
			}
		}

		String raw = ctx.htmls() != null ? ctx.htmls().getHtml("seven_signs/festival/festival_1.htm") : null;
		if (raw != null) {
			int blue = com.lopez.l2j.game.sevensigns.SevenSignsManager.FESTIVAL_FEE_STONES[tier][0];
			int green = com.lopez.l2j.game.sevensigns.SevenSignsManager.FESTIVAL_FEE_STONES[tier][1];
			int red = com.lopez.l2j.game.sevensigns.SevenSignsManager.FESTIVAL_FEE_STONES[tier][2];
			String rendered = raw.replace("%blueStoneNeeded%", String.valueOf(blue))
					.replace("%greenStoneNeeded%", String.valueOf(green))
					.replace("%redStoneNeeded%", String.valueOf(red))
					.replace("%minFestivalPartyMembers%", "1")
					.replace("%festivalType%", isDawn ? "Lords of Dawn" : "Revolutionaries of Dusk");
			rendered = ctx.htmls().render(rendered, npc.objectId(), npc.name(), active.name());
			send(new NpcHtmlMessage(npc.objectId(), rendered));
		} else {
			send(new CreatureSay(0, CreatureSay.ALL, npc.name(), "Festival of Darkness - Nivel maximo: "
					+ com.lopez.l2j.game.sevensigns.SevenSignsManager.FESTIVAL_MAX_LEVELS[tier]));
		}
	}

	private void handleFestivalBypass(NpcInstance npc, String arg) {
		if (active == null) {
			send(new ActionFailed());
			return;
		}
		var ss = ctx.sevenSigns();
		if (ss == null || !ss.isCompetitionPeriod()) {
			String raw = ctx.htmls() != null ? ctx.htmls().getHtml("seven_signs/festival/festival_2a.htm") : null;
			if (raw != null) {
				send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(raw, npc.objectId(), npc.name(), active.name())));
			} else {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Inscricoes do festival estao fechadas."));
			}
			return;
		}

		boolean isDawn = (npc.npcId() >= 31127 && npc.npcId() <= 31131);
		int tier = isDawn ? Math.max(0, Math.min(4, npc.npcId() - 31127)) : Math.max(0, Math.min(4, npc.npcId() - 31137));
		String[] parts = arg.trim().split("\\s+");
		int action = parts.length > 0 ? parseIntSafe(parts[0], 1) : 1;

		if (action == 1) {
			showFestivalNpcHtml(npc, 0);
			return;
		}

		if (action == 2) {
			int stoneId = parts.length > 1 ? parseIntSafe(parts[1], 6360) : 6360;
			int stoneIdx = (stoneId == 6362) ? 2 : ((stoneId == 6361) ? 1 : 0);
			int cost = com.lopez.l2j.game.sevensigns.SevenSignsManager.FESTIVAL_FEE_STONES[tier][stoneIdx];

			int playerCabal = ss.getPlayerCabal(active.objectId());
			int expectedCabal = isDawn ? com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN : com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK;
			if (playerCabal != expectedCabal) {
				String raw = ctx.htmls() != null ? ctx.htmls().getHtml("seven_signs/festival/festival_2d.htm") : null;
				if (raw != null) {
					String r = raw.replace("%festivalType%", isDawn ? "Lords of Dawn" : "Revolutionaries of Dusk");
					send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(r, npc.objectId(), npc.name(), active.name())));
					return;
				}
			}

			if (active.level() > com.lopez.l2j.game.sevensigns.SevenSignsManager.FESTIVAL_MAX_LEVELS[tier]) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Seu nivel (" + active.level() + ") excede o limite deste tier ("
						+ com.lopez.l2j.game.sevensigns.SevenSignsManager.FESTIVAL_MAX_LEVELS[tier] + ")."));
				return;
			}

			if (!consumeItem(stoneId, cost)) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Pedras de selo insuficientes para a taxa de entrada."));
				return;
			}

			int feePoints = cost * (stoneId == 6362 ? 10 : (stoneId == 6361 ? 5 : 3));
			ss.addAccumulatedBonus(tier, feePoints);

			int[] loc = com.lopez.l2j.game.sevensigns.SevenSignsManager.FESTIVAL_ARENA_LOCS[tier];
			teleportToCoordinates(loc[0], loc[1], loc[2]);
			send(new CreatureSay(0, CreatureSay.ALL, "Festival", "Sua equipe entrou no Festival of Darkness! Derrote os monstros e fale com a Bruxa ao terminar."));
			return;
		}

		if (action == 3) {
			String raw = ctx.htmls() != null ? ctx.htmls().getHtml("seven_signs/festival/festival_3a.htm") : null;
			if (raw != null) {
				send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(raw, npc.objectId(), npc.name(), active.name())));
			} else {
				send(new CreatureSay(0, CreatureSay.ALL, npc.name(), "O placar e registrado com a Bruxa ao concluir o desafio dentro da arena."));
			}
			return;
		}

		if (action == 4) {
			StringBuilder sb = new StringBuilder("<html><body>Festival Guide:<br>These are the top scores of the week, for the ");
			String tierName = switch (tier) {
				case 0 -> "Level 31 or lower";
				case 1 -> "Level 42 or lower";
				case 2 -> "Level 53 or lower";
				case 3 -> "Level 64 or lower";
				default -> "No Level Limit";
			};
			sb.append(tierName).append(" festival.<br><br>");
			int dScore = ss.getHighestScore(com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN, tier);
			int kScore = ss.getHighestScore(com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK, tier);
			sb.append(dScore > 0 ? "Dawn: Score " + dScore + "<br>" : "Dawn: No record exists. Score 0<br>");
			sb.append(kScore > 0 ? "Dusk: Score " + kScore + "<br>" : "Dusk: No record exists. Score 0<br>");
			sb.append("<br><a action=\"bypass -h npc_").append(npc.objectId()).append("_Chat 0\">Go back</a></body></html>");
			send(new NpcHtmlMessage(npc.objectId(), sb.toString()));
			return;
		}

		if (action == 5) {
			String raw = ctx.htmls() != null ? ctx.htmls().getHtml("seven_signs/festival/festival_5.htm") : null;
			if (raw != null) {
				StringBuilder rows = new StringBuilder();
				for (int i = 0; i < 5; i++) {
					int dawn = ss.getHighestScore(com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN, i);
					int dusk = ss.getHighestScore(com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK, i);
					String tName = switch (i) {
						case 0 -> "31 or lower";
						case 1 -> "42 or lower";
						case 2 -> "53 or lower";
						case 3 -> "64 or lower";
						default -> "No limits";
					};
					String res = (dawn > dusk) ? "Children of Dawn" : (dusk > dawn ? "Children of Dusk" : "None");
					rows.append("<tr><td width=\"100\" align=\"center\">").append(tName)
							.append("</td><td align=\"center\" width=\"35\">").append(dusk)
							.append("</td><td align=\"center\" width=\"35\">").append(dawn)
							.append("</td><td align=\"center\" width=\"130\">").append(res)
							.append("</td></tr>");
				}
				String rendered = raw.replace("%statsTable%", rows.toString());
				send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(rendered, npc.objectId(), npc.name(), active.name())));
			}
			return;
		}

		if (action == 6) {
			String raw = ctx.htmls() != null ? ctx.htmls().getHtml("seven_signs/festival/festival_6.htm") : null;
			if (raw != null) {
				StringBuilder rows = new StringBuilder();
				for (int i = 0; i < 5; i++) {
					int bonus = ss.getAccumulatedBonus(i);
					String tName = switch (i) {
						case 0 -> "31 or lower";
						case 1 -> "42 or lower";
						case 2 -> "53 or lower";
						case 3 -> "64 or lower";
						default -> "No limits";
					};
					rows.append("<tr><td align=\"center\" width=\"150\">").append(tName)
							.append("</td><td align=\"center\" width=\"150\">").append(bonus)
							.append("</td></tr>");
				}
				String rendered = raw.replace("%bonusTable%", rows.toString());
				send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(rendered, npc.objectId(), npc.name(), active.name())));
			}
			return;
		}

		if (action == 7) {
			teleportToCoordinates(83400, 147943, -3404);
		}
	}

	private void handleFestivalDescBypass(NpcInstance npc, String arg) {
		if (active == null) {
			send(new ActionFailed());
			return;
		}
		String clean = arg.trim();
		if (clean.equals("6")) {
			send(new CreatureSay(0, CreatureSay.ALL, npc.name(), "Mais monstros se aproximam! Prepare-se!"));
			return;
		}
		if (clean.equals("7")) {
			var bo = active.inventory().items().stream().filter(it -> it.itemId() == com.lopez.l2j.game.sevensigns.SevenSignsManager.BLOOD_OFFERING_ID).findFirst().orElse(null);
			int count = bo != null ? (int) bo.count() : 0;
			if (bo != null) {
				ctx.inventories().destroyItem(active.inventory(), bo.objectId(), count, "FestivalOffering");
				send(ItemList.of(active.inventory().items(), true));
			}

			int tier = 0;
			if (npc.npcId() >= 31132 && npc.npcId() <= 31136) {
				tier = npc.npcId() - 31132;
			} else if (npc.npcId() >= 31142 && npc.npcId() <= 31146) {
				tier = npc.npcId() - 31142;
			} else if (npc.npcId() >= 31127 && npc.npcId() <= 31131) {
				tier = npc.npcId() - 31127;
			} else if (npc.npcId() >= 31137 && npc.npcId() <= 31141) {
				tier = npc.npcId() - 31137;
			}
			tier = Math.max(0, Math.min(4, tier));

			var ss = ctx.sevenSigns();
			if (ss != null && count > 0) {
				int cabal = ss.getPlayerCabal(active.objectId());
				java.util.List<String> memberNames = new java.util.ArrayList<>();
				if (party() != null) {
					for (var m : party().members()) {
						if (m.character() != null) {
							memberNames.add(m.character().name());
						}
					}
				}
				if (memberNames.isEmpty()) {
					memberNames.add(active.name());
				}
				ss.addFestivalScore(cabal, tier, count, memberNames);
			}

			boolean isDawn = (npc.npcId() >= 31132 && npc.npcId() <= 31136);
			int targetX = isDawn ? -80157 : -81261;
			int targetY = isDawn ? 111344 : 86531;
			int targetZ = isDawn ? -4901 : -5157;
			teleportToCoordinates(targetX, targetY, targetZ);
			send(new CreatureSay(0, CreatureSay.ALL, "Festival", "Desafio concluido! Voce entregou " + count + " Blood Offerings."));
			return;
		}
		String descHtm = ctx.htmls() != null ? ctx.htmls().getHtml("seven_signs/festival/desc_" + clean + ".htm") : null;
		if (descHtm != null) {
			send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(descHtm, npc.objectId(), npc.name(), active.name())));
		}
	}

	private void handleSevenSignsDescBypass(NpcInstance npc, String arg) {
		if (active == null) {
			send(new ActionFailed());
			return;
		}
		int val = 1;
		try {
			val = Integer.parseInt(arg.trim());
		} catch (Exception ignored) {
		}
		String path = "seven_signs/desc_" + val + ".htm";
		String raw = ctx.htmls() != null ? ctx.htmls().getHtml(path) : null;
		if (raw == null && ctx.htmls() != null) {
			raw = ctx.htmls().getIndexedHtml("desc_" + val + ".htm");
		}
		if (raw != null && ctx.htmls() != null) {
			String rendered = ctx.htmls().render(raw, npc.objectId(), npc.name(), active.name());
			send(new NpcHtmlMessage(npc.objectId(), rendered));
		} else {
			showSevenSignsNpcHtml(npc, 0);
		}
	}

	private void handleSevenSignsBypass(NpcInstance npc, String arg) {
		if (active == null) {
			send(new ActionFailed());
			return;
		}
		boolean isDawn = (npc.npcId() >= 31078 && npc.npcId() <= 31084)
				|| npc.npcId() == 31168 || npc.npcId() == 31692 || npc.npcId() == 31694 || npc.npcId() == 31997
				|| (npc.name() != null && npc.name().toLowerCase(java.util.Locale.ROOT).contains("dawn"));

		String[] parts = arg.trim().split("\\s+");
		int cmd = 0;
		try {
			if (parts.length > 0 && !parts[0].isBlank()) {
				cmd = Integer.parseInt(parts[0]);
			}
		} catch (Exception ignored) {
		}

		var ss = ctx.sevenSigns();

		switch (cmd) {
			case 1 -> {
				String file = "seven_signs/signs_1.htm";
				String raw = ctx.htmls() != null ? ctx.htmls().getHtml(file) : null;
				if (raw != null) {
					send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(raw, npc.objectId(), npc.name(), active.name())));
				} else {
					showSevenSignsNpcHtml(npc, 0);
				}
			}
			case 2 -> {
				long adena = active.inventory().adena();
				if (adena >= 500) {
					ctx.inventories().consumeItem(active.inventory(), 57, 500, "SevenSignsRecord");
					var added = ctx.inventories().addItem(active.inventory(), 5707, 1, "SevenSignsRecord");
					if (added != null) {
						send(new InventoryUpdate(List.of(ItemInfo.of(added.item(), added.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED))));
						refreshWeightAndPenalties();
					}
					String file = isDawn ? "seven_signs/signs_2_dawn.htm" : "seven_signs/signs_2_dusk.htm";
					String raw = ctx.htmls() != null ? ctx.htmls().getHtml(file) : null;
					if (raw != null) {
						send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(raw, npc.objectId(), npc.name(), active.name())));
					} else {
						showSevenSignsNpcHtml(npc, 0);
					}
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao possui Adena suficiente (500 Adena)."));
					showSevenSignsNpcHtml(npc, 0);
				}
			}
			case 3, 33, 34 -> {
				if (ss != null && (ss.activePeriod() == com.lopez.l2j.game.sevensigns.SevenSignsManager.PERIOD_SEAL_VALIDATION
						|| ss.activePeriod() == com.lopez.l2j.game.sevensigns.SevenSignsManager.PERIOD_COMP_RESULTS)) {
					String noFile = isDawn ? "seven_signs/signs_33_dawn_no.htm" : "seven_signs/signs_33_dusk_no.htm";
					String raw = ctx.htmls() != null ? ctx.htmls().getHtml(noFile) : null;
					if (raw != null) {
						send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(raw, npc.objectId(), npc.name(), active.name())));
						return;
					}
				}
				int currentCabal = ss != null ? ss.getPlayerCabal(active.objectId()) : com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL;
				if (currentCabal != com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL) {
					String memberFile = isDawn ? "seven_signs/signs_33_dawn_member.htm" : "seven_signs/signs_33_dusk_member.htm";
					String raw = ctx.htmls() != null ? ctx.htmls().getHtml(memberFile) : null;
					if (raw != null) {
						send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(raw, npc.objectId(), npc.name(), active.name())));
						return;
					}
				}
				String file = isDawn ? "seven_signs/signs_3_dawn.htm" : "seven_signs/signs_3_dusk.htm";
				String raw = ctx.htmls() != null ? ctx.htmls().getHtml(file) : null;
				if (raw != null) {
					send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(raw, npc.objectId(), npc.name(), active.name())));
				} else {
					showSevenSignsNpcHtml(npc, 0);
				}
			}
			case 19 -> {
				int seal = 1;
				if (parts.length > 2) {
					try {
						seal = Integer.parseInt(parts[2]);
					} catch (Exception ignored) {
					}
				} else if (parts.length > 1) {
					try {
						seal = Integer.parseInt(parts[1]);
					} catch (Exception ignored) {
					}
				}
				String sName = (seal == 2) ? "Gnosis" : (seal == 3 ? "Strife" : "Avarice");
				String file = "seven_signs/signs_19_" + sName + "_" + (isDawn ? "dawn" : "dusk") + ".htm";
				String raw = ctx.htmls() != null ? ctx.htmls().getHtml(file) : null;
				if (raw != null) {
					send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(raw, npc.objectId(), npc.name(), active.name())));
				} else {
					showSevenSignsNpcHtml(npc, 0);
				}
			}
			case 20 -> {
				StringBuilder sb = new StringBuilder("<html><body>");
				sb.append(isDawn ? "The Priest Of Dawn:<br><font color=\"LEVEL\">[ State Seals ]</font><br>"
						: "Priestess Of The Sunset:<br><font color=\"LEVEL\">[ State Seals ]</font><br>");
				if (ss != null) {
					sb.append("[Seal of Avarice: ").append(ss.avariceOwner() == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN ? "Dawn" : (ss.avariceOwner() == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK ? "Dusk" : "No owner")).append("]<br>");
					sb.append("[Seal of Gnosis: ").append(ss.gnosisOwner() == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN ? "Dawn" : (ss.gnosisOwner() == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK ? "Dusk" : "No owner")).append("]<br>");
					sb.append("[Seal of Strife: ").append(ss.strifeOwner() == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN ? "Dawn" : (ss.strifeOwner() == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK ? "Dusk" : "No owner")).append("]<br>");
				}
				sb.append("<br><a action=\"bypass -h npc_").append(npc.objectId()).append("_Chat 0\">Back</a></body></html>");
				send(new NpcHtmlMessage(npc.objectId(), sb.toString()));
			}
			case 4 -> {
				if (ss != null && (ss.activePeriod() == com.lopez.l2j.game.sevensigns.SevenSignsManager.PERIOD_SEAL_VALIDATION
						|| ss.activePeriod() == com.lopez.l2j.game.sevensigns.SevenSignsManager.PERIOD_COMP_RESULTS)) {
					String noFile = isDawn ? "seven_signs/signs_33_dawn_no.htm" : "seven_signs/signs_33_dusk_no.htm";
					String raw = ctx.htmls() != null ? ctx.htmls().getHtml(noFile) : null;
					if (raw != null) {
						send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(raw, npc.objectId(), npc.name(), active.name())));
						return;
					}
				}

				boolean hasCastle = active.clanId() > 0 && ctx.clans() != null
						&& ctx.clans().byClanId(active.clanId()).map(cl -> cl.castleId() > 0).orElse(false);

				if (isDawn) {
					if (!hasCastle) {
						var cert = active.inventory().items().stream().filter(it -> it.itemId() == com.lopez.l2j.game.sevensigns.SevenSignsManager.CERTIFICATE_OF_APPROVAL_ID).findFirst().orElse(null);
						if (cert != null) {
							ctx.inventories().destroyItem(active.inventory(), cert.objectId(), 1, "SevenSignsDawnJoin");
							send(ItemList.of(active.inventory().items(), true));
						} else if (active.inventory().adena() >= 50000) {
							ctx.inventories().consumeItem(active.inventory(), 57, 50000, "SevenSignsDawnJoin");
							send(ItemList.of(active.inventory().items(), true));
						} else {
							String feeFile = "seven_signs/signs_33_dawn_fee.htm";
							String raw = ctx.htmls() != null ? ctx.htmls().getHtml(feeFile) : null;
							if (raw != null) {
								send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(raw, npc.objectId(), npc.name(), active.name())));
							} else {
								send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce necessita de um Certificado de Aprovacao ou 50.000 Adena para se juntar a Dawn."));
							}
							return;
						}
					}
				} else {
					if (hasCastle) {
						String noFile = "seven_signs/signs_33_dusk_no.htm";
						String raw = ctx.htmls() != null ? ctx.htmls().getHtml(noFile) : null;
						if (raw != null) {
							send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(raw, npc.objectId(), npc.name(), active.name())));
						} else {
							send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Membros de clas com castelo nao podem se juntar a Dusk."));
						}
						return;
					}
				}

				int seal = 1;
				if (parts.length > 2) {
					try {
						seal = Integer.parseInt(parts[2]);
					} catch (Exception ignored) {
					}
				} else if (parts.length > 1) {
					try {
						seal = Integer.parseInt(parts[1]);
					} catch (Exception ignored) {
					}
				}
				int cabal = isDawn ? com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN : com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK;
				if (ss != null) {
					ss.registerPlayer(active.objectId(), cabal, seal);
				}
				String file = isDawn ? "seven_signs/signs_4_dawn.htm" : "seven_signs/signs_4_dusk.htm";
				String raw = ctx.htmls() != null ? ctx.htmls().getHtml(file) : null;
				if (raw != null) {
					send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(raw, npc.objectId(), npc.name(), active.name())));
				} else {
					showSevenSignsNpcHtml(npc, 0);
				}
			}
			case 5 -> {
				int cabal = ss != null ? ss.getPlayerCabal(active.objectId()) : com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL;
				int myCabal = isDawn ? com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN : com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK;
				if (cabal != myCabal) {
					String noFile = isDawn ? "seven_signs/signs_5_dawn_no.htm" : "seven_signs/signs_5_dusk_no.htm";
					String raw = ctx.htmls() != null ? ctx.htmls().getHtml(noFile) : null;
					if (raw != null) {
						send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(raw, npc.objectId(), npc.name(), active.name())));
						return;
					}
				}
				String file = isDawn ? "seven_signs/signs_5_dawn.htm" : "seven_signs/signs_5_dusk.htm";
				String raw = ctx.htmls() != null ? ctx.htmls().getHtml(file) : null;
				if (raw != null) {
					send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(raw, npc.objectId(), npc.name(), active.name())));
				} else {
					showSevenSignsNpcHtml(npc, 0);
				}
			}
			case 6 -> {
				if (ss != null && ss.activePeriod() != com.lopez.l2j.game.sevensigns.SevenSignsManager.PERIOD_COMPETITION) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Pedras de selo so podem ser entregues durante o periodo de competicao."));
					showSevenSignsNpcHtml(npc, 0);
					return;
				}

				int subChoice = parts.length > 1 ? parseIntSafe(parts[1], 4) : 4;
				int blueCount = 0;
				int greenCount = 0;
				int redCount = 0;

				var blueItem = active.inventory().items().stream().filter(it -> it.itemId() == com.lopez.l2j.game.sevensigns.SevenSignsManager.SEAL_STONE_BLUE_ID).findFirst().orElse(null);
				var greenItem = active.inventory().items().stream().filter(it -> it.itemId() == com.lopez.l2j.game.sevensigns.SevenSignsManager.SEAL_STONE_GREEN_ID).findFirst().orElse(null);
				var redItem = active.inventory().items().stream().filter(it -> it.itemId() == com.lopez.l2j.game.sevensigns.SevenSignsManager.SEAL_STONE_RED_ID).findFirst().orElse(null);

				if (subChoice == 1 && blueItem != null) {
					blueCount = (int) blueItem.count();
				} else if (subChoice == 2 && greenItem != null) {
					greenCount = (int) greenItem.count();
				} else if (subChoice == 3 && redItem != null) {
					redCount = (int) redItem.count();
				} else if (subChoice == 4) {
					blueCount = blueItem != null ? (int) blueItem.count() : 0;
					greenCount = greenItem != null ? (int) greenItem.count() : 0;
					redCount = redItem != null ? (int) redItem.count() : 0;
				}

				if (blueCount == 0 && greenCount == 0 && redCount == 0) {
					String noStones = isDawn ? "seven_signs/signs_6_dawn_no_stones.htm" : "seven_signs/signs_6_dusk_no_stones.htm";
					String raw = ctx.htmls() != null ? ctx.htmls().getHtml(noStones) : null;
					if (raw != null) {
						send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(raw, npc.objectId(), npc.name(), active.name())));
						return;
					}
				}

				if (blueCount > 0 && blueItem != null) ctx.inventories().destroyItem(active.inventory(), blueItem.objectId(), blueCount, "SevenSignsStones");
				if (greenCount > 0 && greenItem != null) ctx.inventories().destroyItem(active.inventory(), greenItem.objectId(), greenCount, "SevenSignsStones");
				if (redCount > 0 && redItem != null) ctx.inventories().destroyItem(active.inventory(), redItem.objectId(), redCount, "SevenSignsStones");

				if (ss != null) {
					ss.contributeStones(active.objectId(), blueCount, greenCount, redCount);
				}
				send(ItemList.of(active.inventory().items(), true));

				String file = isDawn ? "seven_signs/signs_6_dawn.htm" : "seven_signs/signs_6_dusk.htm";
				String raw = ctx.htmls() != null ? ctx.htmls().getHtml(file) : null;
				if (raw != null) {
					send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(raw, npc.objectId(), npc.name(), active.name())));
				} else {
					showSevenSignsNpcHtml(npc, 0);
				}
			}
			case 7 -> {
				long amount = parts.length > 1 ? parseLongSafe(parts[1], 0) : 0;
				if (amount <= 0) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Quantidade invalida."));
					send(new ActionFailed());
					return;
				}
				var aaItem = active.inventory().items().stream().filter(it -> it.itemId() == com.lopez.l2j.game.sevensigns.SevenSignsManager.ANCIENT_ADENA_ID).findFirst().orElse(null);
				if (aaItem == null || aaItem.count() < amount) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao possui Ancient Adena suficiente."));
					send(new ActionFailed());
					return;
				}
				ctx.inventories().destroyItem(active.inventory(), aaItem.objectId(), (int) amount, "AncientAdenaToAdena");
				ctx.inventories().addItem(active.inventory(), 57, (int) amount, "AncientAdenaToAdena");
				send(ItemList.of(active.inventory().items(), true));
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce trocou " + amount + " Ancient Adena por " + amount + " Adena."));
			}
			case 9 -> {
				int cabal = ss != null ? ss.getPlayerCabal(active.objectId()) : com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL;
				int winner = ss != null ? ss.previousWinner() : com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL;
				boolean inValidation = ss != null && ss.isSealValidationPeriod();

				if (!inValidation || cabal != winner || winner == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL) {
					String failedFile = isDawn ? "seven_signs/signs_9_dawn_b.htm" : "seven_signs/signs_9_dusk_b.htm";
					String raw = ctx.htmls() != null ? ctx.htmls().getHtml(failedFile) : null;
					if (raw == null && ctx.htmls() != null) raw = ctx.htmls().getHtml("seven_signs/signs_9_b.htm");
					if (raw != null) {
						send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(raw, npc.objectId(), npc.name(), active.name())));
						return;
					}
				}

				int aa = ss != null ? ss.claimAncientAdena(active.objectId()) : 0;
				if (aa > 0) {
					var added = ctx.inventories().addItem(active.inventory(), com.lopez.l2j.game.sevensigns.SevenSignsManager.ANCIENT_ADENA_ID, aa, "SevenSignsReward");
					if (added != null) {
						send(new InventoryUpdate(List.of(ItemInfo.of(added.item(), added.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED))));
						refreshWeightAndPenalties();
					}
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce recebeu " + aa + " Ancient Adena!"));

					String file = isDawn ? "seven_signs/signs_9_dawn_a.htm" : "seven_signs/signs_9_dusk_a.htm";
					String raw = ctx.htmls() != null ? ctx.htmls().getHtml(file) : null;
					if (raw == null && ctx.htmls() != null) raw = ctx.htmls().getHtml("seven_signs/signs_9_a.htm");
					if (raw != null) {
						send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(raw, npc.objectId(), npc.name(), active.name())));
					} else {
						showSevenSignsNpcHtml(npc, 0);
					}
				} else {
					String noStonesFile = isDawn ? "seven_signs/signs_9_dawn_b.htm" : "seven_signs/signs_9_dusk_b.htm";
					String raw = ctx.htmls() != null ? ctx.htmls().getHtml(noStonesFile) : null;
					if (raw == null && ctx.htmls() != null) raw = ctx.htmls().getHtml("seven_signs/signs_9_b.htm");
					if (raw != null) {
						send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(raw, npc.objectId(), npc.name(), active.name())));
					} else {
						showSevenSignsNpcHtml(npc, 0);
					}
				}
			}
			case 10 -> {
				String htm;
				if (ss != null && ss.isSealValidationPeriod()) {
					htm = (ss.gnosisOwner() != com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL) ? "seven_signs/" + npc.npcId() + "_gnosis.htm" : "seven_signs/" + npc.npcId() + ".htm";
				} else {
					int cabal = ss != null ? ss.getPlayerCabal(active.objectId()) : com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL;
					htm = (cabal == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL) ? "seven_signs/" + (isDawn ? "dawn" : "dusk") + "_priest_tp_no.htm" : "seven_signs/" + npc.npcId() + ".htm";
				}
				String raw = ctx.htmls() != null ? ctx.htmls().getHtml(htm) : null;
				if (raw != null) {
					send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(raw, npc.objectId(), npc.name(), active.name())));
				} else {
					showSevenSignsNpcHtml(npc, 0);
				}
			}
			case 17 -> {
				int sub = parts.length > 1 ? parseIntSafe(parts[1], 1) : 1;
				if (sub == 4) {
					var bItem = active.inventory().items().stream().filter(it -> it.itemId() == com.lopez.l2j.game.sevensigns.SevenSignsManager.SEAL_STONE_BLUE_ID).findFirst().orElse(null);
					var gItem = active.inventory().items().stream().filter(it -> it.itemId() == com.lopez.l2j.game.sevensigns.SevenSignsManager.SEAL_STONE_GREEN_ID).findFirst().orElse(null);
					var rItem = active.inventory().items().stream().filter(it -> it.itemId() == com.lopez.l2j.game.sevensigns.SevenSignsManager.SEAL_STONE_RED_ID).findFirst().orElse(null);
					int bC = bItem != null ? (int) bItem.count() : 0;
					int gC = gItem != null ? (int) gItem.count() : 0;
					int rC = rItem != null ? (int) rItem.count() : 0;
					int totalAA = (bC * 3) + (gC * 5) + (rC * 10);
					if (totalAA > 0) {
						if (bC > 0) ctx.inventories().destroyItem(active.inventory(), bItem.objectId(), bC, "ExchangeSS");
						if (gC > 0) ctx.inventories().destroyItem(active.inventory(), gItem.objectId(), gC, "ExchangeSS");
						if (rC > 0) ctx.inventories().destroyItem(active.inventory(), rItem.objectId(), rC, "ExchangeSS");
						ctx.inventories().addItem(active.inventory(), com.lopez.l2j.game.sevensigns.SevenSignsManager.ANCIENT_ADENA_ID, totalAA, "ExchangeSS");
						send(ItemList.of(active.inventory().items(), true));
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce recebeu " + totalAA + " Ancient Adena!"));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao possui pedras de selo."));
					}
					return;
				}
				String color = (sub == 3) ? "red" : (sub == 2 ? "green" : "blue");
				int stoneId = (sub == 3) ? 6362 : (sub == 2 ? 6361 : 6360);
				int val = (sub == 3) ? 10 : (sub == 2 ? 5 : 3);
				var it = active.inventory().items().stream().filter(i -> i.itemId() == stoneId).findFirst().orElse(null);
				long count = it != null ? it.count() : 0;
				String raw = ctx.htmls() != null ? ctx.htmls().getHtml("seven_signs/signs_17.htm") : null;
				if (raw != null) {
					String rendered = raw.replace("%stoneColor%", color)
							.replace("%stoneValue%", String.valueOf(val))
							.replace("%stoneCount%", String.valueOf(count))
							.replace("%stoneItemId%", String.valueOf(stoneId));
					send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(rendered, npc.objectId(), npc.name(), active.name())));
				}
			}
			case 18 -> {
				if (parts.length >= 3) {
					int stoneId = parseIntSafe(parts[1], 6360);
					long amount = parseLongSafe(parts[2], 0);
					if (amount > 0) {
						var it = active.inventory().items().stream().filter(i -> i.itemId() == stoneId).findFirst().orElse(null);
						if (it != null && it.count() >= amount) {
							int rate = (stoneId == 6362) ? 10 : (stoneId == 6361 ? 5 : 3);
							long aaTotal = amount * rate;
							ctx.inventories().destroyItem(active.inventory(), it.objectId(), (int) amount, "ExchangeSS");
							ctx.inventories().addItem(active.inventory(), com.lopez.l2j.game.sevensigns.SevenSignsManager.ANCIENT_ADENA_ID, (int) aaTotal, "ExchangeSS");
							send(ItemList.of(active.inventory().items(), true));
							send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce trocou " + amount + " pedras por " + aaTotal + " Ancient Adena!"));
							return;
						}
					}
				}
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Quantidade insuficiente de pedras de selo."));
			}
			case 21 -> {
				if (active.level() < 60) {
					String raw = ctx.htmls() != null ? ctx.htmls().getHtml("seven_signs/signs_20.htm") : null;
					if (raw != null) {
						send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(raw, npc.objectId(), npc.name(), active.name())));
						return;
					}
				}
				java.util.Calendar cal = java.util.Calendar.getInstance();
				int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
				boolean isNight = (hour >= 20 || hour < 4);
				String file = isNight ? "seven_signs/signs_23.htm" : "seven_signs/signs_22.htm";
				String raw = ctx.htmls() != null ? ctx.htmls().getHtml(file) : null;
				if (raw != null) {
					send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(raw, npc.objectId(), npc.name(), active.name())));
				}
			}
			case 22 -> {
				long amount = parts.length > 1 ? parseLongSafe(parts[1], 0) : 0;
				if (amount > 0 && amount <= 500000) {
					if (active.inventory().adena() >= amount) {
						ctx.inventories().consumeItem(active.inventory(), 57, (int) amount, "BlackMarketeerAA");
						ctx.inventories().addItem(active.inventory(), com.lopez.l2j.game.sevensigns.SevenSignsManager.ANCIENT_ADENA_ID, (int) amount, "BlackMarketeerAA");
						send(ItemList.of(active.inventory().items(), true));
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce adquiriu " + amount + " Ancient Adena!"));
						return;
					}
				}
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Adena insuficiente ou limite diario de 500.000 excedido."));
			}
			case 8 -> {
				int cabal = ss != null ? ss.getPlayerCabal(active.objectId()) : com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL;
				int requiredCabal = isDawn ? com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN : com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK;
				if (cabal != requiredCabal) {
					String noFile = isDawn ? "seven_signs/dawn_priest_3a.htm" : "seven_signs/dusk_priest_3a.htm";
					String raw = ctx.htmls() != null ? ctx.htmls().getHtml(noFile) : null;
					if (raw != null) {
						send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(raw, npc.objectId(), npc.name(), active.name())));
						return;
					}
				}
				String file = isDawn ? "seven_signs/signs_8_dawn.htm" : "seven_signs/signs_8_dusk.htm";
				String raw = ctx.htmls() != null ? ctx.htmls().getHtml(file) : null;
				if (raw != null) {
					send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(raw, npc.objectId(), npc.name(), active.name())));
				} else {
					showSevenSignsNpcHtml(npc, 0);
				}
			}
			case 11 -> {
				if (parts.length >= 4) {
					try {
						int x = Integer.parseInt(parts[1]);
						int y = Integer.parseInt(parts[2]);
						int z = Integer.parseInt(parts[3]);
						int cost = parts.length >= 5 ? Integer.parseInt(parts[4]) : 0;
						if (cost > 0) {
							if (!consumeItem(com.lopez.l2j.game.sevensigns.SevenSignsManager.ANCIENT_ADENA_ID, cost)) {
								send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao possui Ancient Adena suficiente (" + cost + " necessarias)."));
								send(new ActionFailed());
								return;
							}
						}
						if (ss != null && !active.isGm() && ss.isInside7sDungeon(x, y, z)) {
							boolean isNecropolis = false;
							for (var mLoc : com.lopez.l2j.game.sevensigns.SevenSignsManager.MAMMON_MERCHANT_LOCS) {
								double distSq = Math.pow(x - mLoc.x(), 2) + Math.pow(y - mLoc.y(), 2);
								if (distSq <= 16_000_000) {
									isNecropolis = true;
									break;
								}
							}
							var access = ss.checkDungeonEntry(active.objectId(), isNecropolis);
							if (access != com.lopez.l2j.game.sevensigns.SevenSignsManager.DungeonAccess.ALLOWED) {
								String htm = isNecropolis ? "seven_signs/necro_no.htm" : "seven_signs/cata_no.htm";
								String raw = ctx.htmls() != null ? ctx.htmls().getHtml(htm) : null;
								if (raw != null) {
									send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(raw, npc.objectId(), npc.name(), active.name())));
								} else {
									send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao tem permissao para entrar nos Sete Selos."));
								}
								send(new ActionFailed());
								return;
							}
						}
						teleportToCoordinates(x, y, z);
						return;
					} catch (Exception ex) {
						log.warn("Erro ao processar SevenSigns 11 bypass: {}", arg, ex);
					}
				}
				showSevenSignsNpcHtml(npc, 0);
			}
			default -> showSevenSignsNpcHtml(npc, 0);
		}
	}


	public void onBbsWrite(RequestBBSwrite p) {
		chatHandler.handleBbsWrite(p);
	}

	public void handleBbsCommand(String command) {
		if (ctx.communityBoard() != null && active != null) {
			String html = ctx.communityBoard().handleCommand(active, command);
			if (html != null) {
				send(new GameServerPacket.ShowBoard(html));
			}
		}
	}

	private void onBypass(RequestBypassToServer p) {
		if (!inWorld || p.command() == null || p.command().isBlank()) {
			send(new ActionFailed());
			return;
		}
		String rawCmd = p.command().trim();
		String cmd = rawCmd;
		if (ctx.bypassEncoder() != null) {
			var dec = ctx.bypassEncoder().decode(rawCmd, sessionBypasses, false);
			if (!dec.isValid()) {
				log.warn("Tentativa de bypass invalido/forjado de {} (char: {}): '{}'", ip,
						active != null ? active.name() : "null", rawCmd);
				send(new ActionFailed());
				return;
			}
			cmd = dec.command();
		}
		if (cmd.startsWith("-h ")) {
			cmd = cmd.substring(3).trim();
		} else if (cmd.startsWith("-h")) {
			cmd = cmd.substring(2).trim();
		}
		if (ctx.bypassHandlers() != null && ctx.bypassHandlers().execute(cmd, this)) {
			return;
		}
		if (com.lopez.l2j.game.boss.BossManager.getInstance() != null
				&& com.lopez.l2j.game.boss.BossManager.getInstance().handleBypass(this, cmd)) {
			return;
		}
		if (cmd.startsWith("admin_")) {
			handleAdminCommand(cmd.substring(6).trim());
			return;
		}
		if (cmd.startsWith("voiced_menutoggle ")) {
			if (ctx.preferences() != null && active != null) {
				String toggleType = cmd.substring(18).trim().toLowerCase(java.util.Locale.ROOT);
				switch (toggleType) {
					case "autoloot" -> ctx.preferences().toggleAutoLoot(active.objectId());
					case "trade" -> ctx.preferences().toggleTradeRefusal(active.objectId());
					case "blockbuff" -> ctx.preferences().toggleBlockBuffs(active.objectId());
					case "party" -> ctx.preferences().toggleBlockParty(active.objectId());
					case "exp" -> ctx.preferences().toggleBlockExp(active.objectId());
				}
				send(new NpcHtmlMessage(0, ctx.preferences().buildMenuHtml(active.objectId(), active.name())));
			}
			return;
		}
		if (cmd.startsWith("antibot_validate ")) {
			if (ctx.botsPrevention() != null && active != null) {
				int selected = Integer.parseInt(cmd.substring(17).trim());
				boolean ok = ctx.botsPrevention().validateAnswer(active.objectId(), selected);
				if (ok) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Validacao Anti-Bot bem-sucedida!"));
				} else {
					var punishment = ctx.botsPrevention().checkPunishment(active.objectId());
					punishment.ifPresent(pType -> {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Validacao Anti-Bot falhou. Punicao: " + pType.name()));
						if (pType == com.lopez.l2j.game.service.BotsPreventionService.PunishmentType.KICK) {
							kick();
						}
					});
				}
			}
			return;
		}
		if (cmd.equals("voiced_tvtjoin") && ctx.tvt() != null && active != null) {
			var res = ctx.tvt().register(active);
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "TvT: " + res.name()));
			send(new NpcHtmlMessage(0, ctx.tvt().buildStatusHtml()));
			return;
		}
		if (cmd.equals("voiced_tvtleave") && ctx.tvt() != null && active != null) {
			boolean ok = ctx.tvt().unregister(active);
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", ok ? "Inscricao no TvT cancelada." : "Nao inscrito no TvT."));
			send(new NpcHtmlMessage(0, ctx.tvt().buildStatusHtml()));
			return;
		}
		if (cmd.equals("voiced_ctfjoin") && ctx.ctf() != null && active != null) {
			var res = ctx.ctf().register(active);
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "CTF: " + res.name()));
			send(new NpcHtmlMessage(0, ctx.ctf().buildStatusHtml()));
			return;
		}
		if (cmd.equals("voiced_ctfleave") && ctx.ctf() != null && active != null) {
			boolean ok = ctx.ctf().unregister(active);
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", ok ? "Inscricao no CTF cancelada." : "Nao inscrito no CTF."));
			send(new NpcHtmlMessage(0, ctx.ctf().buildStatusHtml()));
			return;
		}
		if (cmd.equals("voiced_dmjoin") && ctx.dm() != null && active != null) {
			var res = ctx.dm().register(active);
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "DM: " + res.name()));
			send(new NpcHtmlMessage(0, ctx.dm().buildStatusHtml()));
			return;
		}
		if (cmd.equals("voiced_dmleave") && ctx.dm() != null && active != null) {
			boolean ok = ctx.dm().unregister(active);
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", ok ? "Inscricao no DM cancelada." : "Nao inscrito no DM."));
			send(new NpcHtmlMessage(0, ctx.dm().buildStatusHtml()));
			return;
		}
		if (cmd.equals("voiced_getaiogoods") && ctx.aio() != null && active != null && active.isAio()) {
			for (var item : ctx.aio().getAioGoods()) {
				ctx.inventories().addItem(active.inventory(), item.itemId(), item.count(), "AioGoods");
			}
			send(ItemList.of(active.inventory().items(), false));
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Consumiveis de AIOx entregues no inventario."));
			return;
		}
		if (cmd.startsWith("_bbs") || cmd.startsWith("bbs_")) {
			handleBbsCommand(cmd);
			return;
		}
		if (cmd.startsWith("achieve_claim ")) {
			if (ctx.achievements() != null && active != null) {
				int achId = Integer.parseInt(cmd.substring(14).trim());
				int adena = active.inventory().byItemId(57).map(ItemInstance::count).orElse(0);
				boolean ok = ctx.achievements().claimAchievement(active, achId, adena, 0, 0, 0);
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", ok ? "Conquista resgatada com sucesso!" : "Requisitos nao satisfeitos."));
				String html = ctx.achievements().generateHtml(active, adena, 0, 0, 0);
				send(new NpcHtmlMessage(0, html));
			}
			return;
		}
		if (cmd.startsWith("event_exchange ")) {
			if (ctx.officialEvent() != null && active != null) {
				int exId = Integer.parseInt(cmd.substring(15).trim());
				Map<Integer, Integer> inv = new HashMap<>();
				for (var item : active.inventory().items()) {
					inv.put(item.itemId(), item.count());
				}
				boolean ok = ctx.officialEvent().exchangeReward(active, exId, inv);
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", ok ? "Troca efetuada com sucesso!" : "Itens insuficientes."));
				send(new NpcHtmlMessage(0, ctx.officialEvent().generateHtml(active)));
			}
			return;
		}
		if (cmd.startsWith("voiced_roulette")) {
			if (ctx.roulette() != null && active != null) {
				if (cmd.contains("spin")) {
					Map<Integer, Long> inv = new HashMap<>();
					int adena = active.inventory().byItemId(57).map(ItemInstance::count).orElse(0);
					inv.put(57, (long) adena);
					var result = ctx.roulette().spin(active, ctx.roulette().getDefaultCostItemId(), ctx.roulette().getDefaultCostCount(), inv);
					if (result.success()) {
						ctx.inventories().consumeItem(active.inventory(), ctx.roulette().getDefaultCostItemId(), (int) ctx.roulette().getDefaultCostCount(), "RouletteSpin");
						send(ItemList.of(active.inventory().items(), false));
					}
					send(new NpcHtmlMessage(0, ctx.roulette().generateResultHtml(result)));
				} else {
					send(new NpcHtmlMessage(0, ctx.roulette().generateMainHtml(active)));
				}
			}
			return;
		}
		if (cmd.startsWith("voiced_reset")) {
			if (ctx.characterReset() != null && active != null) {
				Map<Integer, Long> inv = new HashMap<>();
				int adena = active.inventory().byItemId(57).map(ItemInstance::count).orElse(0);
				inv.put(57, (long) adena);
				boolean ok = ctx.characterReset().performReset(active, inv);
				if (ok) {
					ctx.inventories().consumeItem(active.inventory(), 57, (int) (adena - inv.get(57)), "CharacterReset");
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Reset efetuado com sucesso! Nivel reiniciado para 1."));
					send(new UserInfo(active, ctx.characters().template(active)));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nao foi possivel realizar o reset."));
				}
			}
			return;
		}
		if (cmd.startsWith("Link ") || cmd.startsWith("player_help ") || cmd.startsWith("player_help")) {
			String path = cmd.startsWith("Link ")
					? cmd.substring(5).trim()
					: (cmd.length() > 12 ? cmd.substring(12).trim() : "");
			if (path.startsWith("/")) {
				path = path.substring(1);
			}
			if (ctx.htmls() != null && !path.isEmpty()) {
				String htm = ctx.htmls().getHtml(path);
				if (htm == null) {
					htm = ctx.htmls().getHtml("help/" + path);
				}
				if (htm == null) {
					htm = ctx.htmls().getIndexedHtml(path);
				}
				if (htm == null) {
					htm = ctx.htmls().getHtml("default/" + path);
				}
				if (htm != null) {
					int npcObjId = active != null ? active.objectId() : 0;
					String rendered = ctx.htmls().render(htm, npcObjId, "NPC",
							active != null ? active.name() : "Player");
					send(new NpcHtmlMessage(npcObjId, rendered));
					return;
				}
			}
		}
		if (cmd.startsWith("scripts_Util:QuestGatekeeper") || cmd.startsWith("scripts_Util:Gatekeeper")) {
			handleScriptGatekeeper(cmd);
			return;
		}
		if (cmd.startsWith("operate_door")) {
			handleOperateDoor(cmd.length() > 12 ? cmd.substring(12).trim() : "");
			return;
		}
		if (cmd.startsWith("open_gate")) {
			handleDoorControl(true);
			return;
		}
		if (cmd.startsWith("manor_menu_select") || cmd.startsWith("menu_select")) {
			handleManorMenu(null, cmd);
			return;
		}
		if (cmd.equals("open_doors") || cmd.startsWith("open_doors")) {
			handleDoorControl(true);
			return;
		}
		if (cmd.equals("close_doors") || cmd.startsWith("close_doors")) {
			handleDoorControl(false);
			return;
		}
		if (cmd.startsWith("Quest ") || cmd.equals("Quest")) {
			String questArg = cmd.length() > 5 ? cmd.substring(5).trim() : "";
			handleQuestBypass(targetObjectId, questArg);
			return;
		}
		if (cmd.startsWith("Subclass ") || cmd.equals("Subclass")) {
			String subArg = cmd.length() > 8 ? cmd.substring(8).trim() : "";
			handleSubclassBypass(targetObjectId, subArg);
			return;
		}
		if (cmd.startsWith("create_clan ") || cmd.startsWith("create_pledge ")) {
			String clanName = cmd.substring(cmd.indexOf(' ') + 1).trim();
			createClan(clanName);
			return;
		}
		if (cmd.equals("increase_clan_level") || cmd.startsWith("increase_clan_level ")) {
			increaseClanLevel();
			return;
		}
		if (cmd.equals("dissolve_clan") || cmd.startsWith("dissolve_clan ")) {
			dissolveClan();
			return;
		}
		if (cmd.startsWith("1stClass")) {
			showClassMasterMenu(targetObjectId, 1);
			return;
		}
		if (cmd.startsWith("2ndClass")) {
			showClassMasterMenu(targetObjectId, 2);
			return;
		}
		if (cmd.startsWith("3rdClass")) {
			showClassMasterMenu(targetObjectId, 3);
			return;
		}
		if (cmd.startsWith("change_class")) {
			try {
				int targetClassId = Integer.parseInt(cmd.replace("change_class", "").trim());
				handleChangeClass(targetObjectId, targetClassId);
				return;
			} catch (NumberFormatException ignored) {
			}
		}
		if (cmd.startsWith("npc_0_") || cmd.startsWith("npc__")) {
			String action = cmd.startsWith("npc_0_") ? cmd.substring(6) : cmd.substring(5);
			if (action.startsWith("change_class")) {
				try {
					int targetClassId = Integer.parseInt(action.replace("change_class", "").trim());
					handleChangeClass(0, targetClassId);
					return;
				} catch (NumberFormatException ignored) {
				}
			} else if (action.startsWith("1stClass")) {
				showClassMasterMenu(0, 1);
				return;
			} else if (action.startsWith("2ndClass")) {
				showClassMasterMenu(0, 2);
				return;
			} else if (action.startsWith("3rdClass")) {
				showClassMasterMenu(0, 3);
				return;
			} else if (action.startsWith("Subclass")) {
				String subArg = action.length() > 8 ? action.substring(8).trim() : "";
				handleSubclassBypass(0, subArg);
				return;
			} else if (action.equals("Draw") || action.startsWith("Draw")) {
				onHennaList();
				return;
			} else if (action.equals("RemoveList") || action.startsWith("RemoveList")) {
				onHennaUnequipList();
				return;
			} else if (action.startsWith("Remove ")) {
				try {
					int slot = Integer.parseInt(action.substring(7).trim());
					onHennaRemoveBySlot(slot);
					return;
				} catch (NumberFormatException ignored) {
				}
			}
		}
		if (cmd.startsWith("npc_")) {
			// Formato: npc_%objectId%_Chat 1 ou npc_%objectId%_Link ... etc
			String[] parts = cmd.split("_", 3);
			if (parts.length >= 3) {
				try {
					int npcObjId = Integer.parseInt(parts[1]);
					var npcOpt = ctx.world().npc(npcObjId);
					if (npcOpt.isPresent()) {
						var npc = npcOpt.get();
						if (active != null && !active.isGm()) {
							if (npc.isDead() || Math.hypot(active.x() - npc.x(), active.y() - npc.y()) > 250.0) {
								send(new ActionFailed());
								return;
							}
						}
						String action = parts[2];
						if (action.startsWith("Chat")) {
							int val = 0;
							if (action.length() > 4) {
								try {
									val = Integer.parseInt(action.substring(4).trim());
								} catch (NumberFormatException ignored) {
								}
							}
							showNpcHtml(npc, val);
							return;
						} else if (action.startsWith("Link")) {
							String path = action.length() > 4 ? action.substring(4).trim() : "";
							if (path.startsWith("/")) {
								path = path.substring(1);
							}
							if (ctx.htmls() != null && !path.isEmpty()) {
								String htm = ctx.htmls().getHtml(path);
								if (htm == null) {
									htm = ctx.htmls().getIndexedHtml(path);
								}
								if (htm == null) {
									htm = ctx.htmls().getHtml("default/" + path);
								}
								if (htm != null) {
									String rendered = ctx.htmls().render(htm, npc.objectId(), npc.name(),
											active.name());
									send(new NpcHtmlMessage(npc.objectId(), rendered));
									return;
								}
							}
							showNpcHtml(npc, 0);
							return;
						} else if (action.startsWith("1stClass")) {
							showClassMasterMenu(npc.objectId(), 1);
							return;
						} else if (action.startsWith("2ndClass")) {
							showClassMasterMenu(npc.objectId(), 2);
							return;
						} else if (action.startsWith("3rdClass")) {
							showClassMasterMenu(npc.objectId(), 3);
							return;
						} else if (action.startsWith("change_class")) {
							try {
								int targetClassId = Integer.parseInt(action.replace("change_class", "").trim());
								handleChangeClass(npc.objectId(), targetClassId);
								return;
							} catch (NumberFormatException ignored) {
							}
						} else if (action.startsWith("SevenSignsDesc")) {
							String descArg = action.length() > 14 ? action.substring(14).trim() : "";
							handleSevenSignsDescBypass(npc, descArg);
							return;
						} else if (action.startsWith("SevenSigns")) {
							String signArg = action.length() > 10 ? action.substring(10).trim() : "";
							handleSevenSignsBypass(npc, signArg);
							return;
						} else if (action.startsWith("Quest")) {
							String questArg = action.length() > 5 ? action.substring(5).trim() : "";
							handleQuestBypass(npc.objectId(), questArg);
							return;
						} else if (action.startsWith("Subclass")) {
							String subArg = action.length() > 8 ? action.substring(8).trim() : "";
							handleSubclassBypass(npc.objectId(), subArg);
							return;
						} else if (action.startsWith("EnchantSkillList")) {
							showEnchantSkillList(npc);
							return;
						} else if (action.startsWith("SkillList") || action.startsWith("FishSkillList")) {
							showSkillList(npc);
							return;
						} else if (action.startsWith("questlist")) {
							if (ctx.htmls() != null) {
								String qHtm = ctx.htmls().getHtml("adventurer_guildsman/questlist.htm");
								if (qHtm != null) {
									send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(qHtm, npc.objectId(), npc.name(), active.name())));
									return;
								}
							}
							showNpcHtml(npc, 0);
							return;
						} else if (action.startsWith("CPRecovery")) {
							if (active != null) {
								if (consumeItem(57, 100)) {
									active.currentCp(active.maxCp());
									send(new StatusUpdate(active.objectId(), List.of(new StatusUpdate.Attribute(StatusUpdate.CUR_CP, (int) active.currentCp()))));
									send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Your CP has been restored."));
								} else {
									send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Not enough adena."));
								}
							}
							return;
						} else if (action.startsWith("SupportMagic")) {
							handleNewbieSupportMagic(npc);
							return;
						} else if (action.startsWith("FestivalDesc")) {
							String num = action.length() > 12 ? action.substring(12).trim() : "1";
							handleFestivalDescBypass(npc, num);
							return;
						} else if (action.startsWith("Festival ")) {
							String fArg = action.substring(9).trim();
							handleFestivalBypass(npc, fArg);
							return;
						} else if (action.equals("Festival")) {
							handleFestivalBypass(npc, "1");
							return;
						} else if (action.equals("Return") || action.startsWith("Return")) {
							teleportToCoordinates(83400, 147943, -3404);
							return;
						} else if (action.startsWith("ExitRift")) {
							if (active != null) {
								teleportToCoordinates(-114790, -180576, -6781);
							}
							return;
						} else if (action.startsWith("ChangeRiftRoom")) {
							if (active != null) {
								send(new CreatureSay(0, CreatureSay.ALL, npc.name(), "The Dimensional Rift space is shifting..."));
							}
							return;
						} else if (action.startsWith("open_doors")) {
							handleDoorControl(true);
							return;
						} else if (action.startsWith("close_doors")) {
							handleDoorControl(false);
							return;
						} else if (action.equals("Draw") || action.startsWith("Draw")) {
							onHennaList();
							return;
						} else if (action.equals("RemoveList") || action.startsWith("RemoveList")) {
							onHennaUnequipList();
							return;
						} else if (action.startsWith("Remove ")) {
							try {
								int slot = Integer.parseInt(action.substring(7).trim());
								onHennaRemoveBySlot(slot);
								return;
							} catch (NumberFormatException ignored) {
							}
						} else if (action.startsWith("goto")) {
							try {
								int teleId = Integer.parseInt(action.substring(4).trim());
								if (teleId == 450 || npc.npcId() == 31111) {
									teleportToCoordinates(184464, -13104, -4900);
									return;
								} else if (teleId == 451 || npc.npcId() == 31112) {
									teleportToCoordinates(172373, -17833, -4901);
									return;
								}
								teleportTo(teleId);
								return;
							} catch (NumberFormatException ignored) {
							}
						} else if (action.startsWith("Buy")) {
							int listId = 1;
							if (action.length() > 3) {
								try {
									listId = Integer.parseInt(action.substring(3).trim());
								} catch (NumberFormatException ignored) {
								}
							}
							showBuyList(npc, listId);
							return;
						} else if (action.startsWith("Sell")) {
							showSellList(npc);
							return;
						} else if (action.startsWith("Wear")) {
							int listId = 1;
							if (action.length() > 4) {
								try {
									listId = Integer.parseInt(action.substring(4).trim());
								} catch (NumberFormatException ignored) {
								}
							}
							showBuyList(npc, listId);
							return;
						} else if (action.startsWith("exc_multisell") || action.startsWith("multisell")) {
							try {
								String listIdStr = action.startsWith("exc_multisell")
										? action.substring(13).trim()
										: action.substring(9).trim();
								int listId;
								try {
									listId = Integer.parseInt(listIdStr);
								} catch (NumberFormatException e) {
									if ("tournament".equalsIgnoreCase(listIdStr)) {
										listId = 70000;
									} else {
										return;
									}
								}
								showMultiSell(npc, listId);
								return;
							} catch (Exception ignored) {
								return;
							}
						} else if (action.startsWith("DepositP") || action.startsWith("Deposit")) {
							if (ctx.warehouse() == null) {
								send(new ActionFailed());
								return;
							}
							var depositable = active.inventory().items().stream()
									.filter(it -> !it.isEquipped() && it.template().type2() != ItemTemplate.TYPE2_QUEST)
									.toList();
							send(new WareHouseDepositList(1, (int) active.inventory().adena(), depositable));
							return;
						} else if (action.startsWith("WithdrawP") || action.startsWith("Withdraw")) {
							if (ctx.warehouse() == null) {
								send(new ActionFailed());
								return;
							}
							var stored = ctx.warehouse().getWarehouseItems(active.objectId());
							send(new WareHouseWithdrawalList(1, (int) active.inventory().adena(), stored));
							return;
						} else if (action.startsWith("DepositC")) {
							if (ctx.warehouse() == null) {
								send(new ActionFailed());
								return;
							}
							var depositable = active.inventory().items().stream()
									.filter(it -> !it.isEquipped() && it.template().type2() != ItemTemplate.TYPE2_QUEST)
									.toList();
							send(new WareHouseDepositList(2, (int) active.inventory().adena(), depositable));
							return;
						} else if (action.startsWith("WithdrawC")) {
							if (ctx.warehouse() == null) {
								send(new ActionFailed());
								return;
							}
							var stored = ctx.warehouse().getWarehouseItems(active.objectId());
							send(new WareHouseWithdrawalList(2, (int) active.inventory().adena(), stored));
							return;
						} else if (action.startsWith("DepositF") || action.startsWith("WithdrawF")) {
							if (ctx.warehouse() == null) {
								send(new ActionFailed());
								return;
							}
							var items = action.startsWith("DepositF")
									? active.inventory().items().stream()
											.filter(it -> !it.isEquipped()
													&& it.template().type2() != ItemTemplate.TYPE2_QUEST)
											.toList()
									: ctx.warehouse().getWarehouseItems(active.objectId());
							send(action.startsWith("DepositF")
									? new WareHouseDepositList(1, (int) active.inventory().adena(), items)
									: new WareHouseWithdrawalList(1, (int) active.inventory().adena(), items));
							return;
						} else if (action.startsWith("TerritoryStatus")) {
							if (ctx.htmls() != null) {
								String tHtml = ctx.htmls().getHtml("territorystatus.htm");
								if (tHtml != null) {
									String rendered = ctx.htmls()
											.render(tHtml, npc.objectId(), npc.name(), active.name())
											.replace("%castlename%", "Giran")
											.replace("%territory%", "Giran")
											.replace("%clanleadername%", "Lord")
											.replace("%clanname%", "Ruling Clan")
											.replace("%taxpercent%", "0");
									send(new NpcHtmlMessage(npc.objectId(), rendered));
									return;
								}
							}
							showNpcHtml(npc, 0);
							return;
						} else if (action.startsWith("Augment")) {
							if (action.contains("2")) {
								send(new ExShowVariationCancelWindow());
							} else {
								send(new ExShowVariationMakeWindow());
							}
							return;
						} else if (action.startsWith("create_clan")) {
							String cName = action.length() > 11 ? action.substring(11).trim() : "";
							if (!cName.isEmpty()) {
								createClan(cName);
							}
							return;
						} else if (action.startsWith("increase_clan_level")) {
							increaseClanLevel();
							return;
						} else if (action.startsWith("dissolve_clan")) {
							dissolveClan();
							return;
						} else if (action.startsWith("getbuff")) {
							handleBufferGetBuff(npc, action.substring(7).trim());
							return;
						} else if (action.equals("restore") || action.startsWith("restore")) {
							handleBufferRestore(npc);
							return;
						} else if (action.equals("cancel") || action.startsWith("cancel")) {
							handleBufferCancel(npc);
							return;
						} else if (action.startsWith("support_back")) {
							showNpcHtml(npc, 0);
							return;
						} else if (action.startsWith("support ")) {
							handleSupportMagic(npc, action.substring(8).trim());
							return;
						} else if (action.startsWith("operate_door")) {
							handleOperateDoor(action.length() > 12 ? action.substring(12).trim() : "");
							return;
						} else if (action.startsWith("open_gate")) {
							handleDoorControl(true);
							return;
						} else if (action.startsWith("observeSiege") || action.startsWith("observeOracle") || action.startsWith("observe")) {
							String obsArgs = action.startsWith("observeSiege")
									? action.substring(12).trim()
									: (action.startsWith("observeOracle") ? action.substring(13).trim() : action.substring(7).trim());
							handleObservation(obsArgs);
							return;
						} else if (action.startsWith("OlympiadDesc")) {
							String num = action.length() > 12 ? action.substring(12).trim() : "0";
							handleOlympiadDesc(npc, num);
							return;
						} else if (action.startsWith("OlympiadNoble")) {
							String val = action.length() > 13 ? action.substring(13).trim() : "1";
							handleOlympiadNoble(npc, val);
							return;
						} else if (action.startsWith("Olympiad")) {
							handleOlympiadDesc(npc, "0");
							return;
						} else if (action.startsWith("Loto")) {
							String val = action.length() > 4 ? action.substring(4).trim() : "0";
							handleLoto(npc, val);
							return;
						} else if (action.startsWith("give_crown")) {
							handleGiveCrown();
							return;
						} else if (action.startsWith("receive_report")) {
							handleCastleReport(npc);
							return;
						} else if (action.startsWith("banish_foreigner")) {
							handleBanishForeigner(npc);
							return;
						} else if (action.startsWith("pvplist") || action.startsWith("pklist") || action.startsWith("onlinelist") || action.startsWith("arenalist")) {
							handleRankingList(npc, action);
							return;
						} else if (action.startsWith("manor_menu_select") || action.startsWith("menu_select")) {
							handleManorMenu(npc, action);
							return;
						} else if (action.startsWith("auction") || action.startsWith("bid") || action.startsWith("selectedItems") || action.startsWith("location") || action.startsWith("list")) {
							handleAuctionAction(npc, action);
							return;
						}
					} else {
						String action = parts[2];
						if (action.startsWith("change_class")) {
							try {
								int targetClassId = Integer.parseInt(action.replace("change_class", "").trim());
								handleChangeClass(npcObjId, targetClassId);
								return;
							} catch (NumberFormatException ignored) {
							}
						} else if (action.startsWith("1stClass")) {
							showClassMasterMenu(npcObjId, 1);
							return;
						} else if (action.startsWith("2ndClass")) {
							showClassMasterMenu(npcObjId, 2);
							return;
						} else if (action.startsWith("3rdClass")) {
							showClassMasterMenu(npcObjId, 3);
							return;
						} else if (action.startsWith("Subclass")) {
							String subArg = action.length() > 8 ? action.substring(8).trim() : "";
							handleSubclassBypass(npcObjId, subArg);
							return;
						} else if (action.startsWith("goto")) {
							try {
								int teleId = Integer.parseInt(action.substring(4).trim());
								teleportTo(teleId);
								return;
							} catch (NumberFormatException ignored) {
							}
						} else if (action.startsWith("exc_multisell") || action.startsWith("multisell")) {
							try {
								String listIdStr = action.startsWith("exc_multisell")
										? action.substring(13).trim()
										: action.substring(9).trim();
								int listId;
								try {
									listId = Integer.parseInt(listIdStr);
								} catch (NumberFormatException e) {
									if ("tournament".equalsIgnoreCase(listIdStr)) {
										listId = 70000;
									} else {
										return;
									}
								}
								showMultiSell(null, listId);
								return;
							} catch (Exception ignored) {
							}
						} else if (action.startsWith("Link")) {
							String path = action.length() > 4 ? action.substring(4).trim() : "";
							if (path.startsWith("/")) {
								path = path.substring(1);
							}
							if (ctx.htmls() != null && !path.isEmpty()) {
								String htm = ctx.htmls().getHtml(path);
								if (htm == null) {
									htm = ctx.htmls().getIndexedHtml(path);
								}
								if (htm == null) {
									htm = ctx.htmls().getHtml("default/" + path);
								}
								if (htm != null) {
									send(new NpcHtmlMessage(npcObjId, ctx.htmls().render(htm, npcObjId, "NPC",
											active != null ? active.name() : "Player")));
									return;
								}
							}
						}
					}
				} catch (NumberFormatException ignored) {
				}
			}
		}
		send(new ActionFailed());
	}

	private void handleBufferGetBuff(NpcInstance npc, String args) {
		if (active == null) {
			send(new ActionFailed());
			return;
		}
		String[] parts = args.trim().split("\\s+");
		if (parts.length == 0 || parts[0].isEmpty()) {
			if (npc != null) showNpcHtml(npc, 0);
			return;
		}
		try {
			int skillId = Integer.parseInt(parts[0]);
			int skillLvl = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
			String returnHtml = parts.length > 2 ? parts[2] : null;

			var skillTable = ctx.skillService() != null ? ctx.skillService().table() : null;
			var skOpt = skillTable != null ? skillTable.get(skillId, skillLvl) : java.util.Optional.<com.lopez.l2j.game.skill.SkillTemplate>empty();
			if (skOpt.isPresent()) {
				var sk = skOpt.get();
				applySkillEffects(sk, false);
				int npcObjId = npc != null ? npc.objectId() : active.objectId();
				var anim = new MagicSkillUse(npcObjId, active.objectId(), sk.id(), sk.level(), 500, 0);
				send(anim);
				if (ctx.world() != null) {
					ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, anim, false);
				}
			}

			if (returnHtml != null && !returnHtml.isBlank() && ctx.htmls() != null) {
				String path = returnHtml.startsWith("data/html/") ? returnHtml.substring(10) : returnHtml;
				String htm = ctx.htmls().getHtml(path);
				if (htm == null) htm = ctx.htmls().getIndexedHtml(path);
				if (htm != null) {
					int npcObjId = npc != null ? npc.objectId() : 0;
					String rendered = ctx.htmls().render(htm, npcObjId, npc != null ? npc.name() : "Buffer", active.name());
					send(new NpcHtmlMessage(npcObjId, rendered));
					return;
				}
			}
			if (npc != null) {
				showNpcHtml(npc, 0);
			}
		} catch (Exception ex) {
			log.warn("Erro ao aplicar buff {}: {}", args, ex.getMessage());
			send(new ActionFailed());
		}
	}

	private void handleBufferRestore(NpcInstance npc) {
		if (active == null) {
			send(new ActionFailed());
			return;
		}
		active.currentHp(active.maxHp());
		active.currentMp(active.maxMp());
		active.currentCp(active.maxCp());
		send(new StatusUpdate(active.objectId(), List.of(
				new StatusUpdate.Attribute(StatusUpdate.CUR_HP, (int) active.currentHp()),
				new StatusUpdate.Attribute(StatusUpdate.CUR_MP, (int) active.currentMp()),
				new StatusUpdate.Attribute(StatusUpdate.CUR_CP, (int) active.currentCp())
		)));
		int npcObjId = npc != null ? npc.objectId() : active.objectId();
		send(new MagicSkillUse(npcObjId, active.objectId(), 1012, 1, 0, 0));
		send(new CreatureSay(0, CreatureSay.ALL, npc != null ? npc.name() : "Buffer", "HP, MP e CP totalmente restaurados."));
		if (npc != null) {
			showNpcHtml(npc, 0);
		}
	}

	private void handleBufferCancel(NpcInstance npc) {
		if (active == null) {
			send(new ActionFailed());
			return;
		}
		active.effects().clear();
		int npcObjId = npc != null ? npc.objectId() : active.objectId();
		send(new MagicSkillUse(npcObjId, active.objectId(), 1056, 1, 0, 0));
		send(new CreatureSay(0, CreatureSay.ALL, npc != null ? npc.name() : "Buffer", "Todos os buffs foram removidos."));
		if (npc != null) {
			showNpcHtml(npc, 0);
		}
	}

	private void handleSupportMagic(NpcInstance npc, String args) {
		if (active == null) {
			send(new ActionFailed());
			return;
		}
		String[] parts = args.trim().split("\\s+");
		if (parts.length == 0 || parts[0].isEmpty()) {
			if (npc != null) showNpcHtml(npc, 0);
			return;
		}
		try {
			int skillId = Integer.parseInt(parts[0]);
			int skillLvl = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
			var skillTable = ctx.skillService() != null ? ctx.skillService().table() : null;
			var skOpt = skillTable != null ? skillTable.get(skillId, skillLvl) : java.util.Optional.<com.lopez.l2j.game.skill.SkillTemplate>empty();
			if (skOpt.isPresent()) {
				var sk = skOpt.get();
				applySkillEffects(sk, false);
				int npcObjId = npc != null ? npc.objectId() : active.objectId();
				var anim = new MagicSkillUse(npcObjId, active.objectId(), sk.id(), sk.level(), 500, 0);
				send(anim);
				if (ctx.world() != null) {
					ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, anim, false);
				}
			}
			if (ctx.htmls() != null && npc != null) {
				String doneHtm = ctx.htmls().getHtml("fortress/support-done.htm");
				if (doneHtm == null) doneHtm = ctx.htmls().getHtml("chamberlain/support-done.htm");
				if (doneHtm != null) {
					String rendered = ctx.htmls().render(doneHtm, npc.objectId(), npc.name(), active.name());
					send(new NpcHtmlMessage(npc.objectId(), rendered));
					return;
				}
			}
			if (npc != null) {
				showNpcHtml(npc, 0);
			}
		} catch (Exception ex) {
			send(new ActionFailed());
		}
	}

	private void handleOperateDoor(String args) {
		if (active == null) {
			send(new ActionFailed());
			return;
		}
		var dt = com.lopez.l2j.game.door.DoorTable.getInstance();
		if (dt == null) {
			send(new ActionFailed());
			return;
		}
		String[] parts = args.trim().split("\\s+");
		if (parts.length == 0 || parts[0].isEmpty()) {
			handleDoorControl(true);
			return;
		}
		boolean open = !"0".equals(parts[0]);
		if (parts.length > 1) {
			for (int i = 1; i < parts.length; i++) {
				try {
					int doorId = Integer.parseInt(parts[i]);
					if (open) {
						dt.openDoor(doorId);
					} else {
						dt.closeDoor(doorId);
					}
				} catch (NumberFormatException ignored) {}
			}
		} else {
			handleDoorControl(open);
		}
		send(new CreatureSay(0, CreatureSay.ALL, "SYS", open ? "Portao aberto." : "Portao fechado."));
	}

	private void handleObservation(String args) {
		if (active == null) {
			send(new ActionFailed());
			return;
		}
		String[] parts = args.trim().split("\\s+");
		if (parts.length >= 3) {
			try {
				int x = Integer.parseInt(parts[0]);
				int y = Integer.parseInt(parts[1]);
				int z = Integer.parseInt(parts[2]);
				int cost = parts.length > 3 ? Integer.parseInt(parts[3]) : 500;
				if (cost > 0 && !consumeItem(57, cost)) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Adena insuficiente para observacao."));
					return;
				}
				teleportToCoordinates(x, y, z);
			} catch (NumberFormatException ignored) {
			}
		}
	}

	private void handleOlympiadDesc(NpcInstance npc, String val) {
		if (ctx.htmls() == null || active == null) return;
		int npcObjId = npc != null ? npc.objectId() : 0;
		String htmName = "0".equals(val) ? "olympiad/noble_main.htm" : "olympiad/noble_desc" + val + ".htm";
		String raw = ctx.htmls().getHtml(htmName);
		if (raw == null) raw = ctx.htmls().getIndexedHtml(htmName);
		if (raw != null) {
			send(new NpcHtmlMessage(npcObjId, ctx.htmls().render(raw, npcObjId, npc != null ? npc.name() : "Olympiad Manager", active.name())));
		} else if (npc != null) {
			showNpcHtml(npc, 0);
		}
	}

	private void handleOlympiadNoble(NpcInstance npc, String val) {
		if (active == null) return;
		int v = 1;
		try { v = Integer.parseInt(val); } catch (Exception ignored) {}
		String npcName = npc != null ? npc.name() : "Olympiad Manager";
		switch (v) {
			case 1 -> send(new CreatureSay(0, CreatureSay.ALL, npcName, "Sua inscricao nas Grandes Olimpiadas foi cancelada."));
			case 2 -> send(new CreatureSay(0, CreatureSay.ALL, npcName, "Fila de espera atual: 0 participantes sem classe, 0 com classe."));
			case 3 -> {
				var noble = ctx.olympiad() != null ? ctx.olympiad().getNoble(active.objectId()) : java.util.Optional.<com.lopez.l2j.game.olympiad.OlympiadNoble>empty();
				int pts = noble.map(com.lopez.l2j.game.olympiad.OlympiadNoble::points).orElse(18);
				send(new CreatureSay(0, CreatureSay.ALL, npcName, "Seus pontos atuais de Nobreza nas Olimpiadas: " + pts));
			}
			case 4 -> send(new CreatureSay(0, CreatureSay.ALL, npcName, "Voce foi registrado nas batalhas sem restricao de classe."));
			case 5 -> send(new CreatureSay(0, CreatureSay.ALL, npcName, "Voce foi registrado nas batalhas baseadas em classe."));
			default -> send(new CreatureSay(0, CreatureSay.ALL, npcName, "Operacao concluida."));
		}
		if (npc != null) {
			handleOlympiadDesc(npc, "0");
		}
	}

	private void handleLoto(NpcInstance npc, String val) {
		if (ctx.htmls() == null || active == null || npc == null) return;
		int v = 0;
		try { v = Integer.parseInt(val); } catch (Exception ignored) {}
		String filename = switch (v) {
			case 0 -> npc.npcId() + "-1.htm";
			case 21, 22 -> npc.npcId() + "-5.htm";
			case 23 -> npc.npcId() + "-3.htm";
			case 24 -> npc.npcId() + "-4.htm";
			default -> npc.npcId() + "-1.htm";
		};
		String htm = ctx.htmls().getHtml("default/" + filename);
		if (htm == null) htm = ctx.htmls().getIndexedHtml(filename);
		if (htm != null) {
			String rendered = ctx.htmls().render(htm, npc.objectId(), npc.name(), active.name())
					.replace("%ticket_price%", "2000")
					.replace("%prize_5%", "100,000,000")
					.replace("%prize_4%", "5,000,000")
					.replace("%prize_3%", "500,000")
					.replace("%prize_2%", "20,000");
			send(new NpcHtmlMessage(npc.objectId(), rendered));
		} else {
			showNpcHtml(npc, 0);
		}
	}

	private void handleGiveCrown() {
		if (active == null) return;
		var playerClan = (ctx.clans() != null && active.clanId() > 0) ? ctx.clans().byClanId(active.clanId()).orElse(null) : null;
		if (playerClan != null && playerClan.castleId() > 0 && playerClan.isLeader(active.objectId())) {
			if (ctx.crowns() != null) {
				ctx.crowns().rewardLordCrown(active, playerClan.castleId());
				send(new CreatureSay(0, CreatureSay.ALL, "Chamberlain", "A Coroa do Senhor do Castelo e o Diadema foram entregues!"));
			}
		} else {
			send(new CreatureSay(0, CreatureSay.ALL, "Chamberlain", "Apenas o lider do cla soberano do castelo pode receber a coroa."));
		}
	}

	private void handleCastleReport(NpcInstance npc) {
		if (ctx.htmls() == null || active == null || npc == null) return;
		String htm = ctx.htmls().getHtml("chamberlain/chamberlain-report.htm");
		if (htm != null) {
			String rendered = ctx.htmls().render(htm, npc.objectId(), npc.name(), active.name())
					.replace("%tax_income%", "0")
					.replace("%current_tax%", "0")
					.replace("%castle_income%", "0");
			send(new NpcHtmlMessage(npc.objectId(), rendered));
		} else {
			showNpcHtml(npc, 0);
		}
	}

	private void handleBanishForeigner(NpcInstance npc) {
		if (active == null) return;
		send(new CreatureSay(0, CreatureSay.ALL, npc != null ? npc.name() : "Chamberlain", "Todos os nao-membros do cla foram banidos para fora do castelo."));
		if (npc != null) {
			showNpcHtml(npc, 0);
		}
	}

	private void handleRankingList(NpcInstance npc, String action) {
		if (ctx.htmls() == null || active == null) return;
		int npcObjId = npc != null ? npc.objectId() : 0;
		String htm = ctx.htmls().getHtml("mods/status/50008-1.htm");
		if (htm != null) {
			StringBuilder sb = new StringBuilder("<table width=290>");
			var topList = ctx.characters() != null ? ctx.characters().findTopPvP(10) : List.<com.lopez.l2j.game.model.PlayerCharacter>of();
			int rank = 1;
			for (var p : topList) {
				sb.append("<tr>")
						.append("<td width=20 align=center>").append(rank++).append("</td>")
						.append("<td width=140 align=left>").append(p.name()).append("</td>")
						.append("<td width=60 align=center>").append(p.pvpKills()).append("</td>")
						.append("<td width=70 align=center><font color=\"00FF00\">ONLINE</font></td>")
						.append("</tr>");
			}
			sb.append("</table>");
			String rendered = ctx.htmls().render(htm, npcObjId, npc != null ? npc.name() : "Status", active.name())
					.replace("%pvplist%", sb.toString());
			send(new NpcHtmlMessage(npcObjId, rendered));
		} else if (npc != null) {
			showNpcHtml(npc, 0);
		}
	}

	private void handleManorMenu(NpcInstance npc, String cmd) {
		if (active == null) return;
		int npcObjId = npc != null ? npc.objectId() : 0;
		String npcName = npc != null ? npc.name() : "Manor Manager";
		if (cmd.contains("ask=6")) { // Purchase harvester
			if (consumeItem(57, 20)) {
				ctx.inventories().addItem(active.inventory(), 4442, 1, "ManorHarvester");
				send(ItemList.of(active.inventory().items(), false));
				send(new CreatureSay(0, CreatureSay.ALL, npcName, "Harvester adquirido com sucesso!"));
			} else {
				send(new CreatureSay(0, CreatureSay.ALL, npcName, "Adena insuficiente para comprar o Harvester (20 adena)."));
			}
			return;
		}
		if (ctx.htmls() != null) {
			String htm = ctx.htmls().getHtml("manormanager/manager.htm");
			if (htm == null) htm = ctx.htmls().getHtml("chamberlain/manor/manor.htm");
			if (htm != null) {
				send(new NpcHtmlMessage(npcObjId, ctx.htmls().render(htm, npcObjId, npcName, active.name())));
				return;
			}
		}
		send(new CreatureSay(0, CreatureSay.ALL, npcName, "Sistema Manor em operacao normal."));
	}

	private void handleAuctionAction(NpcInstance npc, String action) {
		if (active == null) return;
		int npcObjId = npc != null ? npc.objectId() : 0;
		String npcName = npc != null ? npc.name() : "Auctioneer";
		if (action.startsWith("bid ") && action.length() > 4) {
			try {
				long bid = Long.parseLong(action.substring(4).trim());
				int hallId = 1;
				if (ctx.clanHalls() != null) {
					var res = ctx.clanHalls().bidOnAuction(active, hallId, bid);
					send(new CreatureSay(0, CreatureSay.ALL, npcName, "Resultado do lance no Clan Hall: " + res.name()));
				}
			} catch (Exception ignored) {}
			return;
		}
		if (action.startsWith("bid1")) {
			if (ctx.htmls() != null) {
				String bHtm = ctx.htmls().getHtml("auction/AgitBid1.htm");
				if (bHtm != null) {
					long adena = active.inventory() != null ? active.inventory().adena() : 0;
					String rendered = ctx.htmls().render(bHtm, npcObjId, npcName, active.name())
							.replace("%PLEDGE_ADENA%", String.valueOf(adena))
							.replace("%AGIT_AUCTION_MINBID%", "10000000")
							.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + npcObjId + "_auction");
					send(new NpcHtmlMessage(npcObjId, rendered));
					return;
				}
			}
		}
		if (ctx.htmls() != null) {
			String aHtm = ctx.htmls().getHtml("auction/auction.htm");
			if (aHtm != null) {
				send(new NpcHtmlMessage(npcObjId, ctx.htmls().render(aHtm, npcObjId, npcName, active.name())));
				return;
			}
		}
		if (npc != null) showNpcHtml(npc, 0);
	}
	private int getNpcCastleId(NpcInstance npc) {
		if (npc == null) return 1;
		int id = npc.npcId();
		if (id >= 35092 && id <= 35141) return 1; // Gludio
		if (id >= 35142 && id <= 35183) return 2; // Dion
		if (id >= 35184 && id <= 35225) return 3; // Giran
		if (id >= 35226 && id <= 35273) return 4; // Oren
		if (id >= 35274 && id <= 35315) return 5; // Aden
		if (id >= 35316 && id <= 35357) return 6; // Innadril
		if (id >= 35358 && id <= 35400) return 7; // Goddard
		if (id >= 35504 && id <= 35554) return 8; // Rune
		if (id >= 35555 && id <= 35600) return 9; // Schuttgart

		if (ctx.castles() != null && ctx.zones() != null) {
			var castleOpt = ctx.castles().getCastleAt(npc.x(), npc.y(), npc.z(), ctx.zones());
			if (castleOpt.isPresent()) {
				return castleOpt.get().id();
			}
		}
		return 1;
	}

	private String resolveQuestHtml(com.lopez.l2j.game.quest.Quest q, String rawResult, NpcInstance npc) {
		if (rawResult == null || rawResult.isBlank()) {
			return null;
		}
		if (rawResult.contains("<html") || rawResult.contains("<HTML") || rawResult.contains("<body") || rawResult.contains("<BODY")) {
			return rawResult;
		}
		if ("noquest".equalsIgnoreCase(rawResult) || "no-quest".equalsIgnoreCase(rawResult)) {
			return ctx.htmls() != null ? ctx.htmls().getHtml("npcdefault.htm") : null;
		}
		if (ctx.htmls() == null) {
			return rawResult;
		}

		String qName = q != null ? q.getName() : "";
		// 1. Tenta dentro da pasta da quest especifica e suas variacoes de alias
		if (!qName.isEmpty()) {
			String h = ctx.htmls().getHtml("quests/" + qName + "/" + rawResult);
			if (h != null) return h;
			h = ctx.htmls().getHtml("scripts/quests/" + qName + "/" + rawResult);
			if (h != null) return h;

			if (qName.startsWith("_")) {
				String noUnderscore = qName.substring(1);
				h = ctx.htmls().getHtml("quests/" + noUnderscore + "/" + rawResult);
				if (h != null) return h;
				h = ctx.htmls().getHtml("scripts/quests/" + noUnderscore + "/" + rawResult);
				if (h != null) return h;
			} else {
				String withUnderscore = "_" + qName;
				h = ctx.htmls().getHtml("quests/" + withUnderscore + "/" + rawResult);
				if (h != null) return h;
				h = ctx.htmls().getHtml("scripts/quests/" + withUnderscore + "/" + rawResult);
				if (h != null) return h;
			}
		}

		if (q != null && q.getQuestId() > 0) {
			int qId = q.getQuestId();
			String h = ctx.htmls().getHtml("quests/" + qId + "/" + rawResult);
			if (h != null) return h;
			h = ctx.htmls().getHtml(String.format("quests/%03d/%s", qId, rawResult));
			if (h != null) return h;
			h = ctx.htmls().getHtml(String.format("quests/q%03d/%s", qId, rawResult));
			if (h != null) return h;
			h = ctx.htmls().getHtml(String.format("quests/Q%03d/%s", qId, rawResult));
			if (h != null) return h;
		}

		// 2. Tenta pelo nome indexado (ex: 31554-01.htm, merc_kahmun_q0628_01.htm)
		String h = ctx.htmls().getIndexedHtml(rawResult);
		if (h != null) return h;

		// 3. Tenta caminhos diretos e alternativos
		h = ctx.htmls().getHtml(rawResult);
		if (h != null) return h;
		h = ctx.htmls().getHtml("default/" + rawResult);
		if (h != null) return h;

		// 4. Se contiver caminho com barra, tenta o basename no indice global
		int lastSlash = Math.max(rawResult.lastIndexOf('/'), rawResult.lastIndexOf('\\'));
		if (lastSlash >= 0 && lastSlash < rawResult.length() - 1) {
			String baseName = rawResult.substring(lastSlash + 1);
			h = ctx.htmls().getIndexedHtml(baseName);
			if (h != null) return h;
		}

		// Fallback seguro para evitar exibir texto cru do nome do arquivo para o jogador
		String def = ctx.htmls().getHtml("npcdefault.htm");
		if (def != null && !def.isBlank()) {
			return def;
		}
		return "<html><body>" + (npc != null ? npc.name() : "NPC") + ":<br><br>I have no tasks for you right now.</body></html>";
	}

	private void handleQuestBypass(int npcObjId, String questArg) {
		var npcOpt = ctx.world().npc(npcObjId);
		if (npcOpt.isEmpty()) {
			send(new ActionFailed());
			return;
		}
		var npc = npcOpt.get();
		int castleId = getNpcCastleId(npc);

		if (questArg == null || questArg.isBlank()) {
			int npcId = npc.npcId();
			java.util.List<com.lopez.l2j.game.quest.Quest> candidateQuests = new java.util.ArrayList<>();
			if (ctx.questManager() != null) {
				for (var q : ctx.questManager().getAllQuests()) {
					if (q.hasTalkNpc(npcId) || q.hasStartNpc(npcId)) {
						candidateQuests.add(q);
					}
				}
			}

			if (candidateQuests.size() == 1) {
				var q = candidateQuests.get(0);
				String html = q.notifyTalk(npc, this);
				if (html != null && !html.isBlank()) {
					String resolved = resolveQuestHtml(q, html, npc);
					if (resolved != null) {
						String rendered = ctx.htmls() != null
								? ctx.htmls().render(resolved, npc.objectId(), npc.name(), active != null ? active.name() : "Player", castleId, npc.npcId())
								: resolved;
						send(new NpcHtmlMessage(npc.objectId(), rendered));
						return;
					}
				}
			} else if (candidateQuests.size() > 1) {
				StringBuilder sb = new StringBuilder("<html><body>Quest:<br><br><table width=270>");
				for (var q : candidateQuests) {
					var qs = getQuestState(q.getName());
					String status = (qs != null && qs.isStarted()) ? " <font color=\"LEVEL\">(In Progress)</font>" : "";
					sb.append("<tr><td><a action=\"bypass -h npc_").append(npc.objectId())
							.append("_Quest ").append(q.getName()).append("\">")
							.append(q.getDescr().isEmpty() ? q.getName() : q.getDescr()).append("</a>")
							.append(status).append("</td></tr>");
				}
				sb.append("</table></body></html>");
				send(new NpcHtmlMessage(npc.objectId(), sb.toString()));
				return;
			}

			String lType = npc.template() != null ? npc.template().type().toLowerCase(java.util.Locale.ROOT) : "";
			if (lType.contains("master") || lType.contains("trainer") || lType.contains("teacher")
					|| lType.contains("priest")) {
				String masterHtml = "<html><title>" + npc.name() + "</title><body>"
						+ "<font color=\"LEVEL\">" + npc.name() + " ("
						+ (npc.template().title().isEmpty() ? "Master" : npc.template().title())
						+ ")</font><br><br>"
						+ "Greetings, adventurer. How may I instruct you today?<br><br>"
						+ "<table width=240>"
						+ "<tr><td><a action=\"bypass -h npc_" + npcObjId
						+ "_SkillList\">Learn Skills</a></td></tr>"
						+ "<tr><td><a action=\"bypass -h npc_" + npcObjId
						+ "_1stClass\">1st Class Transfer (Level 20)</a></td></tr>"
						+ "<tr><td><a action=\"bypass -h npc_" + npcObjId
						+ "_2ndClass\">2nd Class Transfer (Level 40)</a></td></tr>"
						+ "<tr><td><a action=\"bypass -h npc_" + npcObjId
						+ "_3rdClass\">3rd Class Transfer (Level 76)</a></td></tr>"
						+ "<tr><td><a action=\"bypass -h npc_" + npcObjId
						+ "_Subclass 0\">Subclass</a></td></tr>"
						+ "<tr><td><a action=\"bypass -h create_clan 0\">Create Clan</a></td></tr>"
						+ "</table></body></html>";
				send(new NpcHtmlMessage(npcObjId, masterHtml));
				return;
			}
			if (lType.contains("teleport")) {
				showNpcHtml(npc, 1);
				return;
			}
			if (lType.contains("merchant") || lType.contains("trader") || lType.contains("grocer")) {
				showBuyList(npc, 1);
				return;
			}
			showNpcNoQuest(npc);
			return;
		}

		if (questArg.startsWith("1101_teleport_to_race_track")) {
			teleportToCoordinates(12661, 181687, -3560);
			return;
		}

		if (questArg.startsWith("1103_OracleTeleport") || questArg.startsWith("1104_OracleTeleport")) {
			boolean isDawn = (npc.npcId() >= 31078 && npc.npcId() <= 31084)
					|| npc.npcId() == 31168 || npc.npcId() == 31692 || npc.npcId() == 31694 || npc.npcId() == 31997
					|| (npc.name() != null && npc.name().toLowerCase(java.util.Locale.ROOT).contains("dawn"));
			if (isDawn) {
				teleportToCoordinates(-80157, 111344, -4901);
			} else {
				teleportToCoordinates(-81261, 86531, -5157);
			}
			return;
		}

		String[] parts = questArg.split("\\s+");
		String qName = parts[0];
		String event = questArg.length() > qName.length() ? questArg.substring(qName.length()).trim() : null;
		var q = ctx.questManager() != null ? ctx.questManager().getQuest(qName) : null;
		if (q != null) {
			String html;
			if (event != null && !event.isEmpty()) {
				html = q.notifyEvent(event, npc, this);
			} else {
				html = q.notifyTalk(npc, this);
			}
			if (html != null && !html.isBlank()) {
				String resolved = resolveQuestHtml(q, html, npc);
				if (resolved != null) {
					String rendered = ctx.htmls() != null
							? ctx.htmls().render(resolved, npc.objectId(), npc.name(), active != null ? active.name() : "Player", castleId, npc.npcId())
							: resolved;
					send(new NpcHtmlMessage(npc.objectId(), rendered));
					return;
				}
			}
		}

		if (ctx.htmls() != null) {
			String qHtml = null;
			if (parts.length >= 2) {
				String filename = parts[parts.length - 1];
				qHtml = ctx.htmls().getIndexedHtml(filename);
				if (qHtml == null) {
					qHtml = ctx.htmls().getHtml("village_master/" + parts[0] + "/" + filename);
				}
				if (qHtml == null) {
					qHtml = ctx.htmls().getHtml("quests/" + parts[0] + "/" + filename);
				}
			} else {
				qHtml = ctx.htmls().getHtml("teleporter/" + questArg + ".htm");
				if (qHtml == null) {
					qHtml = ctx.htmls().getHtml("default/" + questArg + ".htm");
				}
				if (qHtml == null) {
					qHtml = ctx.htmls().getIndexedHtml(questArg);
				}
				if (qHtml == null) {
					qHtml = ctx.htmls().getIndexedHtml(npc.npcId() + "-01.htm");
				}
			}
			if (qHtml != null) {
				String rendered = ctx.htmls().render(qHtml, npc.objectId(), npc.name(), active != null ? active.name() : "Player", castleId, npc.npcId());
				send(new NpcHtmlMessage(npc.objectId(), rendered));
				return;
			}
		}

		showNpcNoQuest(npc);
	}

	private void showNpcNoQuest(NpcInstance npc) {
		String defaultHtm = ctx.htmls() != null ? ctx.htmls().getHtml("npcdefault.htm") : null;
		if (defaultHtm != null) {
			send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(defaultHtm, npc.objectId(), npc.name(), active != null ? active.name() : "Player")));
		} else {
			send(new NpcHtmlMessage(npc.objectId(), "<html><body>" + npc.name() + ":<br><br>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>"));
		}
	}

	private void handleSubclassBypass(int npcObjId, String args) {
		if (active == null) {
			send(new ActionFailed());
			return;
		}
		if (active.isDead() || active.isOlympiadMode() || autoAttacking || active.isInCombat() || active.isStoreOpen() || active.isBuffShop()) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao pode gerenciar subclasses enquanto estiver em combate, morto ou em loja."));
			send(new ActionFailed());
			return;
		}

		if (args == null || args.isBlank() || args.equals("0")) {
			StringBuilder sb = new StringBuilder("<html><title>Subclass</title><body>");
			sb.append("<font color=\"LEVEL\">Subclass Management</font><br><br>");
			sb.append("A master is capable of awakening latent heroic powers through subclasses.<br><br>");
			sb.append("<table width=260>");
			int maxSubs = Config.MAX_SUBCLASSES > 0 ? Config.MAX_SUBCLASSES : 3;
			int subCount = active.getSubClasses().size();
			if (subCount < maxSubs) {
				sb.append("<tr><td><a action=\"bypass -h npc_").append(npcObjId).append("_Subclass 1\">Add a Subclass</a></td></tr>");
			}
			if (subCount > 0) {
				sb.append("<tr><td><a action=\"bypass -h npc_").append(npcObjId).append("_Subclass 2\">Change Subclass</a></td></tr>");
				sb.append("<tr><td><a action=\"bypass -h npc_").append(npcObjId).append("_Subclass 3\">Cancel / Replace a Subclass</a></td></tr>");
			}
			sb.append("</table></body></html>");
			send(new NpcHtmlMessage(npcObjId, sb.toString()));
			return;
		}

		if (args.equals("1")) {
			int maxSubs = Config.MAX_SUBCLASSES > 0 ? Config.MAX_SUBCLASSES : 3;
			if (!active.isGm()) {
				if (active.level() < 75) {
					send(new NpcHtmlMessage(npcObjId, "<html><body>You must be at least level 75 to acquire a subclass.</body></html>"));
					return;
				}
				if (ctx.subClasses() != null && !ctx.subClasses().checkSubclassRequirements(active)) {
					send(new NpcHtmlMessage(npcObjId, "<html><body>You do not meet the requirements or quests to acquire a subclass.</body></html>"));
					return;
				}
			}
			if (active.subClasses().size() >= maxSubs) {
				send(new NpcHtmlMessage(npcObjId, "<html><body>You cannot add more than " + maxSubs + " subclasses.</body></html>"));
				return;
			}
			var subs = ctx.subClasses() != null ? ctx.subClasses().getAvailableSubClasses(active) : java.util.List.<Integer>of();
			if (subs.isEmpty()) {
				send(new NpcHtmlMessage(npcObjId, "<html><body>There are no available subclasses for your character at this time.</body></html>"));
				return;
			}
			StringBuilder sb = new StringBuilder("<html><title>Add Subclass</title><body>Select the subclass you wish to acquire:<br><br><table width=260>");
			for (int targetCId : subs) {
				String cName = ctx.characters() != null ? ctx.characters().template(targetCId).map(CharTemplate::className).orElse("Class " + targetCId) : "Class " + targetCId;
				sb.append("<tr><td><a action=\"bypass -h npc_").append(npcObjId).append("_Subclass 1_").append(targetCId).append("\">")
						.append(cName).append("</a></td></tr>");
			}
			sb.append("</table></body></html>");
			send(new NpcHtmlMessage(npcObjId, sb.toString()));
			return;
		}

		if (args.startsWith("1_")) {
			try {
				int maxSubs = Config.MAX_SUBCLASSES > 0 ? Config.MAX_SUBCLASSES : 3;
				int targetClassId = Integer.parseInt(args.substring(2).trim());
				int nextIndex = 1;
				for (int i = 1; i <= maxSubs; i++) {
					if (!active.subClasses().containsKey(i)) {
						nextIndex = i;
						break;
					}
				}
				int initLvl = Config.SUBCLASS_INIT_LEVEL > 0 ? Config.SUBCLASS_INIT_LEVEL : 40;
				long baseExp = com.lopez.l2j.game.model.ExperienceTable.expForLevel(initLvl);
				SubClass sc = new SubClass(targetClassId, baseExp, 0, initLvl, nextIndex);
				if (ctx.subClasses() != null) {
					ctx.subClasses().saveSubClass(active.objectId(), sc);
				}
				active.subClasses().put(nextIndex, sc);
				applySubClassSwitch(nextIndex, targetClassId, initLvl, baseExp, 0);

				send(SystemMessage.id(SystemMessage.ADD_NEW_SUBCLASS));
				send(new PlaySound("ItemSound.quest_fanfare_2"));
				send(new MagicSkillUse(active.objectId(), active.objectId(), 4339, 1, 0, 0));
				ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS,
						new MagicSkillUse(active.objectId(), active.objectId(), 4339, 1, 0, 0), false);
				send(new SocialAction(active.objectId(), 3));
				ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS,
						new SocialAction(active.objectId(), 3), false);
				if (Config.SHOW_CLASS_CHANGE_MESSAGE) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Subclasse adicionada com sucesso!"));
				}
			} catch (Exception ex) {
				log.warn("Falha ao adicionar subclass: {}", ex.getMessage());
			}
			return;
		}

		if (args.equals("2")) {
			StringBuilder sb = new StringBuilder("<html><title>Change Subclass</title><body>Select the class you want to switch to:<br><br><table width=260>");
			if (active.isSubClassActive()) {
				var baseTpl = ctx.characters() != null ? ctx.characters().template(active.baseClassId()) : java.util.Optional.<CharTemplate>empty();
				String baseName = baseTpl.map(CharTemplate::className).orElse("Base Class (" + active.baseClassId() + ")");
				sb.append("<tr><td><a action=\"bypass -h npc_").append(npcObjId).append("_Subclass 2_0\">")
						.append(baseName).append(" (Main)</a></td></tr>");
			}
			for (var entry : active.subClasses().entrySet()) {
				int idx = entry.getKey();
				if (idx == active.classIndex()) {
					continue;
				}
				var sc = entry.getValue();
				var scTpl = ctx.characters() != null ? ctx.characters().template(sc.classId()) : java.util.Optional.<CharTemplate>empty();
				String name = scTpl.map(CharTemplate::className).orElse("Subclass " + sc.classId());
				sb.append("<tr><td><a action=\"bypass -h npc_").append(npcObjId).append("_Subclass 2_").append(idx).append("\">")
						.append(name).append(" (Lv. ").append(sc.level()).append(")</a></td></tr>");
			}
			sb.append("</table></body></html>");
			send(new NpcHtmlMessage(npcObjId, sb.toString()));
			return;
		}

		if (args.startsWith("2_")) {
			try {
				int targetIndex = Integer.parseInt(args.substring(2).trim());
				if (targetIndex == 0) {
					if (active.isSubClassActive()) {
						var baseSub = ctx.subClasses() != null ? ctx.subClasses().loadSubClasses(active.objectId()).get(0) : null;
						int mainLvl = baseSub != null && baseSub.level() > 0 ? baseSub.level() : Math.max(active.level(), 75);
						long mainExp = baseSub != null && baseSub.exp() > 0 ? baseSub.exp() : com.lopez.l2j.game.model.ExperienceTable.expForLevel(mainLvl);
						int mainSp = baseSub != null ? baseSub.sp() : active.sp();
						applySubClassSwitch(0, active.baseClassId(), mainLvl, mainExp, mainSp);
						send(SystemMessage.id(SystemMessage.SUBCLASS_TRANSFER_COMPLETED));
						send(new PlaySound("ItemSound.quest_fanfare_2"));
						if (Config.SHOW_CLASS_CHANGE_MESSAGE) {
							send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce retornou para sua classe principal."));
						}
					}
					return;
				}
				var targetSub = active.subClasses().get(targetIndex);
				if (targetSub != null) {
					applySubClassSwitch(targetIndex, targetSub.classId(), targetSub.level(), targetSub.exp(), targetSub.sp());
					send(SystemMessage.id(SystemMessage.SUBCLASS_TRANSFER_COMPLETED));
					send(new PlaySound("ItemSound.quest_fanfare_2"));
					if (Config.SHOW_CLASS_CHANGE_MESSAGE) {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Subclasse alterada com sucesso!"));
					}
				}
			} catch (Exception ex) {
				log.warn("Falha ao trocar subclass: {}", ex.getMessage());
			}
			return;
		}

		if (args.equals("3")) {
			StringBuilder sb = new StringBuilder("<html><title>Replace Subclass</title><body>Select the subclass you wish to replace:<br><br><table width=260>");
			for (var entry : active.subClasses().entrySet()) {
				var sc = entry.getValue();
				var scTpl = ctx.characters() != null ? ctx.characters().template(sc.classId()) : java.util.Optional.<CharTemplate>empty();
				String name = scTpl.map(CharTemplate::className).orElse("Subclass " + sc.classId());
				sb.append("<tr><td><a action=\"bypass -h npc_").append(npcObjId).append("_Subclass 3_").append(entry.getKey()).append("\">")
						.append(name).append(" (Lv. ").append(sc.level()).append(")</a></td></tr>");
			}
			sb.append("</table></body></html>");
			send(new NpcHtmlMessage(npcObjId, sb.toString()));
			return;
		}

		if (args.startsWith("3_")) {
			try {
				int replaceIndex = Integer.parseInt(args.substring(2).trim());
				var subs = ctx.subClasses() != null ? ctx.subClasses().getAvailableSubClasses(active) : java.util.List.<Integer>of();
				StringBuilder sb = new StringBuilder("<html><title>Replace Subclass</title><body>Select the new subclass to replace slot " + replaceIndex + ":<br><br><table width=260>");
				for (int choiceId : subs) {
					String cName = ctx.characters() != null ? ctx.characters().template(choiceId).map(CharTemplate::className).orElse("Class " + choiceId) : "Class " + choiceId;
					sb.append("<tr><td><a action=\"bypass -h npc_").append(npcObjId).append("_Subclass 4_").append(replaceIndex).append("_").append(choiceId).append("\">")
							.append(cName).append("</a></td></tr>");
				}
				sb.append("</table></body></html>");
				send(new NpcHtmlMessage(npcObjId, sb.toString()));
			} catch (Exception ex) {
				log.warn("Falha no menu de substituir subclass: {}", ex.getMessage());
			}
			return;
		}

		if (args.startsWith("4_")) {
			try {
				String[] parts = args.substring(2).split("_");
				int replaceIndex = Integer.parseInt(parts[0]);
				int newClassId = Integer.parseInt(parts[1]);
				int initLvl = Config.SUBCLASS_INIT_LEVEL > 0 ? Config.SUBCLASS_INIT_LEVEL : 40;
				long baseExp = com.lopez.l2j.game.model.ExperienceTable.expForLevel(initLvl);
				SubClass sc = new SubClass(newClassId, baseExp, 0, initLvl, replaceIndex);
				if (ctx.subClasses() != null) {
					ctx.subClasses().saveSubClass(active.objectId(), sc);
				}
				active.subClasses().put(replaceIndex, sc);
				applySubClassSwitch(replaceIndex, newClassId, initLvl, baseExp, 0);
				send(SystemMessage.id(SystemMessage.SUBCLASS_TRANSFER_COMPLETED));
				send(new PlaySound("ItemSound.quest_fanfare_2"));
				if (Config.SHOW_CLASS_CHANGE_MESSAGE) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Subclasse substituida com sucesso!"));
				}
			} catch (Exception ex) {
				log.warn("Falha ao substituir subclass: {}", ex.getMessage());
			}
		}
	}

	private void applySubClassSwitch(int newIndex, int newClassId, int newLevel, long newExp, int newSp) {
		if (active == null) {
			return;
		}
		saveCurrentClassProgress();

		active.classIndex(newIndex);
		active.classId(newClassId);
		active.level(newLevel);
		active.exp(newExp);
		active.sp(newSp);

		var tplOpt = ctx.characters() != null ? ctx.characters().template(newClassId) : java.util.Optional.<CharTemplate>empty();
		if (tplOpt.isPresent()) {
			var tpl = tplOpt.get();
			rewardSkills(tpl, false);
		}
		updateArmorSetBonus();
		updateEquippedItemSkills();
		updateAugmentationBonus();
		loadHennasForCurrentSubclass();
		var charTpl = ctx.characters() != null ? ctx.characters().template(active) : null;
		if (charTpl != null) {
			recalcMaxVitals(charTpl);
		}
		active.currentHp(active.maxHp());
		active.currentMp(active.maxMp());
		active.currentCp(active.maxCp());

		if (ctx.characters() != null) {
			ctx.characters().save(active, true);
		}
		if (ctx.subClasses() != null && active.isSubClassActive()) {
			ctx.subClasses().saveSubClass(active.objectId(), new SubClass(newClassId, newExp, newSp, newLevel, newIndex));
		}

		active.effects().clear();
		refreshBuffs();

		send(new MagicSkillUse(active.objectId(), active.objectId(), 4383, 1, 0, 0));
		ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS,
				new MagicSkillUse(active.objectId(), active.objectId(), 4383, 1, 0, 0), false);

		refreshWeightAndPenalties();
		sendSkillList();
		send(new HennaInfo(active, getClassLevel(active), ctx.hennaTrees()));
		send(new ShortCutInit(ctx.shortcuts() != null ? ctx.shortcuts().findByCharId(active.objectId(), active.classIndex()) : java.util.List.of()));
		sendUserInfoAndBroadcastCharInfo();
	}

	private void saveCurrentClassProgress() {
		if (active == null) {
			return;
		}
		if (active.isSubClassActive()) {
			var sc = active.subClasses().get(active.classIndex());
			if (sc != null) {
				SubClass updated = new SubClass(active.classId(), active.exp(), active.sp(), active.level(), sc.classIndex());
				active.subClasses().put(sc.classIndex(), updated);
				if (ctx.subClasses() != null) {
					ctx.subClasses().saveSubClass(active.objectId(), updated);
				}
			}
		} else {
			SubClass baseSub = new SubClass(active.baseClassId(), active.exp(), active.sp(), active.level(), 0);
			if (ctx.subClasses() != null) {
				ctx.subClasses().saveSubClass(active.objectId(), baseSub);
			}
		}
	}

	public void showClassMasterMenu(int npcObjId, int targetLevel) {
		if (active == null) {
			send(new ActionFailed());
			return;
		}
		var curTpl = ctx.characters() != null ? ctx.characters().template(active.classId()).orElse(null) : null;
		int currentTier = curTpl != null ? curTpl.classTier() : 0;
		if (currentTier >= 3) {
			StringBuilder sb = new StringBuilder();
			sb.append("<html><body><center><font color=\"LEVEL\">Class Master</font><br><br>");
			sb.append("Voce ja atingiu a classe maxima (3rd Class).<br>");
			if (active.level() >= 75) {
				sb.append("<br><a action=\"bypass -h npc_").append(npcObjId).append("_Subclass 0\">Gerenciar Subclasses</a><br>");
			}
			sb.append("</center></body></html>");
			send(new NpcHtmlMessage(npcObjId, sb.toString()));
			return;
		}

		int nextClassTier = currentTier + 1;
		int minLvl = switch (nextClassTier) {
			case 1 -> 20;
			case 2 -> 40;
			case 3 -> 76;
			default -> 1;
		};

		if (!active.isGm() && active.level() < minLvl) {
			String laterHtm = ctx.htmls() != null ? ctx.htmls().getHtml("classmaster/comebacklater.htm") : null;
			if (laterHtm != null) {
				String rendered = laterHtm.replace("%level%", String.valueOf(minLvl))
						.replace("%objectId%", String.valueOf(npcObjId));
				send(new NpcHtmlMessage(npcObjId, rendered));
			} else {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Volte quando atingir nivel " + minLvl + "."));
			}
			return;
		}

		List<Integer> children = ctx.skillService() != null && ctx.skillService().trees() != null
				? ctx.skillService().trees().getChildClasses(active.classId())
				: List.of();
		if (children.isEmpty()) {
			String noMoreHtm = ctx.htmls() != null ? ctx.htmls().getHtml("classmaster/nomore.htm") : null;
			if (noMoreHtm != null) {
				send(new NpcHtmlMessage(npcObjId, noMoreHtm.replace("%objectId%", String.valueOf(npcObjId))));
			} else {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nao ha mais mudancas de classe disponiveis."));
			}
			return;
		}

		StringBuilder menu = new StringBuilder();
		for (int cid : children) {
			String cname = ctx.characters() != null
					? ctx.characters().template(cid).map(CharTemplate::className).orElse("Class " + cid)
					: "Class " + cid;
			menu.append("<a action=\"bypass -h npc_").append(npcObjId).append("_change_class ").append(cid)
					.append("\">")
					.append(cname).append("</a><br>");
		}
		if (active.level() >= 75) {
			menu.append("<br><a action=\"bypass -h npc_").append(npcObjId).append("_Subclass 0\">Gerenciar Subclasses</a><br>");
		}
		String curName = curTpl != null ? curTpl.className() : "Class " + active.classId();
		String tpl = ctx.htmls() != null ? ctx.htmls().getHtml("classmaster/template.htm") : null;
		if (tpl != null) {
			String rendered = tpl.replace("%name%", curName)
					.replace("%menu%", menu.toString())
					.replace("%req_items%", "")
					.replace("%objectId%", String.valueOf(npcObjId));
			send(new NpcHtmlMessage(npcObjId, rendered));
		} else {
			String fallback = "<html><body><center>" + curName + " Class Master:</center><br>" + menu
					+ "</body></html>";
			send(new NpcHtmlMessage(npcObjId, fallback));
		}
	}

	private void handleChangeClass(int npcObjId, int newClassId) {
		handleChangeClass(npcObjId, newClassId, active != null && active.isGm());
	}

	private void handleChangeClass(int npcObjId, int newClassId, boolean isGmOverride) {
		if (active == null || active.isDead() || active.isOlympiadMode()) {
			send(new ActionFailed());
			return;
		}
		List<Integer> allowed = ctx.skillService() != null && ctx.skillService().trees() != null
				? ctx.skillService().trees().getChildClasses(active.classId())
				: List.of();
		if (!isGmOverride && (allowed.isEmpty() || !allowed.contains(newClassId))) {
			log.warn("{} tentou trocar para classe invalida: {} (atual: {})", active.name(), newClassId,
					active.classId());
			send(new ActionFailed());
			return;
		}
		var tplOpt = ctx.characters() != null ? ctx.characters().template(newClassId)
				: java.util.Optional.<CharTemplate>empty();
		if (tplOpt.isPresent()) {
			var tpl = tplOpt.get();
			int minLvl = switch (tpl.classTier()) {
				case 1 -> 20;
				case 2 -> 40;
				case 3 -> 76;
				default -> 1;
			};
			if (!isGmOverride && active.level() < minLvl) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Nivel " + minLvl + " necessario para trocar para " + tpl.className() + "."));
				send(new ActionFailed());
				return;
			}
			active.classId(newClassId);
			if (!active.isSubClassActive()) {
				active.baseClassId(newClassId);
			}
			rewardSkills(tpl, true);
			updateArmorSetBonus();
			updateEquippedItemSkills();
			updateAugmentationBonus();
			recalcMaxVitals(tpl);
			active.currentHp(active.maxHp());
			active.currentMp(active.maxMp());
			active.currentCp(active.maxCp());
		} else {
			active.classId(newClassId);
			if (!active.isSubClassActive()) {
				active.baseClassId(newClassId);
			}
		}
		if (ctx.characters() != null) {
			ctx.characters().save(active, true);
		}
		active.recalcHennaStats(ctx.hennas(), ctx.hennaTrees());
		refreshWeightAndPenalties();
		sendSkillList();
		send(new HennaInfo(active, getClassLevel(active), ctx.hennaTrees()));
		send(new ShortCutInit(ctx.shortcuts() != null ? ctx.shortcuts().findByCharId(active.objectId(), active.classIndex()) : java.util.List.of()));
		sendUserInfoAndBroadcastCharInfo();

		// Som e efeitos visuais canonicos de Lineage 2
		send(new PlaySound("ItemSound.quest_fanfare_2"));
		send(new MagicSkillUse(active.objectId(), active.objectId(), 4339, 1, 0, 0));
		ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS,
				new MagicSkillUse(active.objectId(), active.objectId(), 4339, 1, 0, 0), false);
		send(new SocialAction(active.objectId(), 3));
		ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS,
				new SocialAction(active.objectId(), 3), false);

		String newClassName = tplOpt.map(CharTemplate::className).orElse("Class " + newClassId);
		if (Config.SHOW_CLASS_CHANGE_MESSAGE) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Parabens! Voce agora e um " + newClassName + "!"));
		}
		if (Config.ANNOUNCE_CLASS_CHANGE) {
			ctx.world().broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "", active.name() + " avancou para a classe " + newClassName + "!"), x -> true);
		} else if (Config.ANNOUNCE_CLASS_CHANGE_AROUND) {
			ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS,
					new CreatureSay(0, CreatureSay.ALL, "SYS", active.name() + " avancou para a classe " + newClassName + "!"), false);
		}

		String okHtm = ctx.htmls() != null ? ctx.htmls().getHtml("classmaster/ok.htm") : null;
		if (okHtm != null) {
			send(new NpcHtmlMessage(npcObjId,
					okHtm.replace("%name%", newClassName).replace("%objectId%", String.valueOf(npcObjId))));
		}
	}

	private void teleportTo(int teleId) {
		if (active != null && !Config.ALT_KARMA_PLAYER_CAN_USE_GK && active.karma() > 0) {
			send(new ActionFailed());
			return;
		}
		if (ctx.teleports() == null) {
			send(new ActionFailed());
			return;
		}
		var locOpt = ctx.teleports().get(teleId);
		if (locOpt.isEmpty()) {
			log.warn("Teleport id {} nao encontrado", teleId);
			send(new ActionFailed());
			return;
		}
		var loc = locOpt.get();
		if (active != null && !active.isGm() && ctx.sevenSigns() != null && ctx.sevenSigns().isInside7sDungeon(loc.locX(), loc.locY(), loc.locZ())) {
			boolean isNecropolis = false;
			for (var mLoc : com.lopez.l2j.game.sevensigns.SevenSignsManager.MAMMON_MERCHANT_LOCS) {
				double distSq = Math.pow(loc.locX() - mLoc.x(), 2) + Math.pow(loc.locY() - mLoc.y(), 2);
				if (distSq <= 16_000_000) {
					isNecropolis = true;
					break;
				}
			}
			var access = ctx.sevenSigns().checkDungeonEntry(active.objectId(), isNecropolis);
			if (access != com.lopez.l2j.game.sevensigns.SevenSignsManager.DungeonAccess.ALLOWED) {
				String htm = isNecropolis ? "seven_signs/necro_no.htm" : "seven_signs/cata_no.htm";
				String raw = ctx.htmls() != null ? ctx.htmls().getHtml(htm) : null;
				if (raw != null) {
					send(new NpcHtmlMessage(0, ctx.htmls().render(raw, 0, "Gatekeeper Ziggurat", active.name())));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao tem permissao para entrar nos Sete Selos."));
				}
				send(new ActionFailed());
				return;
			}
		}

		int price = loc.price();
		int costItem = loc.forNoble() ? 6651 : ItemTemplate.ADENA_ID;

		boolean isFree = false;
		if (!loc.forNoble()) {
			boolean freeTp = Config.getBoolean("FreeTeleporting", false);
			int minLvl = Config.getInt("FreeTeleportingMinLvL", 1);
			int maxLvl = Config.getInt("FreeTeleportingMaxLvL", 99);
			if (freeTp && active.level() >= minLvl && active.level() <= maxLvl) {
				isFree = true;
			}
		} else {
			boolean freeNobleTp = Config.getBoolean("NoblePassFreeTp", false);
			int minLvl = Config.getInt("NoblePassFreeTpMinLvL", 1);
			int maxLvl = Config.getInt("NoblePassFreeTpMaxLvL", 99);
			if (freeNobleTp && active.level() >= minLvl && active.level() <= maxLvl) {
				isFree = true;
			}
		}

		if (price > 0 && !isFree) {
			var consumed = ctx.inventories().consumeItem(active.inventory(), costItem, price, "Teleport");
			if (consumed == null) {
				send(SystemMessage.id(SystemMessage.YOU_NOT_ENOUGH_ADENA));
				send(new ActionFailed());
				return;
			}
			send(new InventoryUpdate(List.of(consumed.removed()
					? ItemInfo.of(consumed.item(), ItemInfo.REMOVED)
					: ItemInfo.of(consumed.item(), ItemInfo.MODIFIED))));
		}

		teleportToCoordinates(loc.locX(), loc.locY(), loc.locZ());
	}

	public void teleportToCoordinates(int targetX, int targetY, int targetZ) {
		teleportToLocation(targetX, targetY, targetZ);
	}

	private void handleScriptGatekeeper(String cmd) {
		if (active == null) {
			send(new ActionFailed());
			return;
		}
		try {
			int spaceIdx = cmd.indexOf(' ');
			if (spaceIdx > 0) {
				String[] tokens = cmd.substring(spaceIdx + 1).trim().split("\\s+");
				if (tokens.length >= 3) {
					int x = Integer.parseInt(tokens[0]);
					int y = Integer.parseInt(tokens[1]);
					int z = Integer.parseInt(tokens[2]);
					if (tokens.length >= 5) {
						long cost = Long.parseLong(tokens[3]);
						int itemId = Integer.parseInt(tokens[4]);
						if (cost > 0 && !consumeItem(itemId, (int) cost)) {
							send(new CreatureSay(0, CreatureSay.ALL, "SYS", "You do not have enough required items."));
							send(new ActionFailed());
							return;
						}
					} else if (tokens.length == 4) {
						long cost = Long.parseLong(tokens[3]);
						if (cost > 0 && !consumeItem(57, (int) cost)) {
							send(new CreatureSay(0, CreatureSay.ALL, "SYS", "You do not have enough adena."));
							send(new ActionFailed());
							return;
						}
					}
					if (active != null && !active.isGm() && ctx.sevenSigns() != null && ctx.sevenSigns().isInside7sDungeon(x, y, z)) {
						boolean isNecropolis = false;
						for (var mLoc : com.lopez.l2j.game.sevensigns.SevenSignsManager.MAMMON_MERCHANT_LOCS) {
							double distSq = Math.pow(x - mLoc.x(), 2) + Math.pow(y - mLoc.y(), 2);
							if (distSq <= 16_000_000) {
								isNecropolis = true;
								break;
							}
						}
						var access = ctx.sevenSigns().checkDungeonEntry(active.objectId(), isNecropolis);
						if (access != com.lopez.l2j.game.sevensigns.SevenSignsManager.DungeonAccess.ALLOWED) {
							String htm = isNecropolis ? "seven_signs/necro_no.htm" : "seven_signs/cata_no.htm";
							String raw = ctx.htmls() != null ? ctx.htmls().getHtml(htm) : null;
							if (raw != null) {
								send(new NpcHtmlMessage(0, ctx.htmls().render(raw, 0, "Gatekeeper Ziggurat", active.name())));
							} else {
								send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao tem permissao para entrar nos Sete Selos."));
							}
							send(new ActionFailed());
							return;
						}
					}
					teleportToCoordinates(x, y, z);
					return;
				}
			}
		} catch (Exception e) {
			log.warn("Erro ao processar gatekeeper via script: {}", cmd, e);
		}
		send(new ActionFailed());
	}

	private void handleDoorControl(boolean open) {
		if (active == null) {
			send(new ActionFailed());
			return;
		}
		com.lopez.l2j.game.door.DoorTable dt = com.lopez.l2j.game.door.DoorTable.getInstance();
		if (dt != null) {
			var nearby = dt.findDoorsAround(active.x(), active.y(), 1000);
			for (var door : nearby) {
				if (open) {
					dt.openDoor(door.doorId());
				} else {
					dt.closeDoor(door.doorId());
				}
			}
		}
		send(new CreatureSay(0, CreatureSay.ALL, "SYS", open ? "Portas abertas." : "Portas fechadas."));
	}

	private void handleNewbieSupportMagic(NpcInstance npc) {
		if (active == null) {
			send(new ActionFailed());
			return;
		}
		if (active.level() < 6 || active.level() > 25) {
			send(new CreatureSay(0, CreatureSay.ALL, npc != null ? npc.name() : "Newbie Guide",
					"Only characters between level 6 and 25 may receive beginner buffs."));
			return;
		}
		active.currentHp(active.maxHp());
		active.currentMp(active.maxMp());
		send(new StatusUpdate(active.objectId(), List.of(
				new StatusUpdate.Attribute(StatusUpdate.CUR_HP, (int) active.currentHp()),
				new StatusUpdate.Attribute(StatusUpdate.CUR_MP, (int) active.currentMp()))));
		send(new MagicSkillUse(npc != null ? npc.objectId() : 0, active.objectId(), 1204, 1, 1000, 0));
		send(new CreatureSay(0, CreatureSay.ALL, npc != null ? npc.name() : "Newbie Guide",
				"May the blessings of the Einhasad protect your path."));
	}

	private void showBuyList(NpcInstance npc, int listId) {
		if (ctx.buylists() == null) {
			send(new ActionFailed());
			return;
		}
		var buyListOpt = ctx.buylists().get(listId);
		if (buyListOpt.isEmpty()) {
			log.warn("BuyList id {} nao encontrada", listId);
			send(new ActionFailed());
			return;
		}
		var bl = buyListOpt.get();
		List<BuyList.BuyProductView> views = new ArrayList<>();
		for (var prod : bl.products()) {
			var t = ctx.inventories().templates().get(prod.itemId()).orElse(null);
			if (t != null) {
				views.add(new BuyList.BuyProductView(prod.itemId(), prod.price(), prod.count(), t.type1(), t.type2(),
						t.bodyPart()));
			}
		}
		send(new BuyList((int) active.inventory().adena(), listId, views));
	}

	public void onBuyItem(RequestBuyItem p) {
		itemHandler.handleBuyItem(p);
	}

	public void showSellList(NpcInstance npc) {
		tradeStoreHandler.showSellList(npc);
	}

	public void onSellItem(RequestSellItem p) {
		itemHandler.handleSellItem(p);
	}

	public void showMultiSell(NpcInstance npc, int listId) {
		tradeStoreHandler.showMultiSell(npc, listId);
	}

	public void onMultiSellChoose(GameClientPacket.MultiSellChoose p) {
		tradeStoreHandler.handleMultiSellChoose(p);
	}

	public void onActionUse(RequestActionUse p) {
		actionHandler.handleActionUse(p);
	}

	private void onSocialAction(GameClientPacket.RequestSocialAction p) {
		if (!inWorld || active == null || active.isDead() || active.sitting()) {
			send(new ActionFailed());
			return;
		}
		var pkt = new SocialAction(active.objectId(), p.actionId());
		send(pkt);
		ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, pkt, false);
	}

	public void onLeaveParty() {
		partyClanHandler.handleLeaveParty();
	}

	public void onExpelPartyMember(String name) {
		partyClanHandler.handleExpelPartyMember(name);
	}

	public void onShowMiniMap() {
		if (!inWorld || active == null) {
			send(new ActionFailed());
			return;
		}
		send(new ShowMiniMap(0, 0));
		int points = ctx.raidPoints() != null ? ctx.raidPoints().getPointsByOwnerId(active.objectId()) : 0;
		int ranking = ctx.raidPoints() != null ? ctx.raidPoints().calculateRanking(active.objectId()) : 0;
		Map<Integer, Integer> list = ctx.raidPoints() != null ? ctx.raidPoints().getList(active.objectId()) : Map.of();
		send(new ExGetBossRecord(ranking, points, list));
	}

	private void onGetBossRecord(int bossId) {
		if (!inWorld || active == null) {
			send(new ActionFailed());
			return;
		}
		int points = ctx.raidPoints() != null ? ctx.raidPoints().getPointsByOwnerId(active.objectId()) : 0;
		int ranking = ctx.raidPoints() != null ? ctx.raidPoints().calculateRanking(active.objectId()) : 0;
		Map<Integer, Integer> list = ctx.raidPoints() != null ? ctx.raidPoints().getList(active.objectId()) : Map.of();
		send(new ExGetBossRecord(ranking, points, list));

		if (bossId != 0) {
			boolean found = false;
			if (ctx.world() != null) {
				for (NpcInstance n : ctx.world().npcs()) {
					if (n.npcId() == bossId) {
						int bx = n.x() != 0 ? n.x() : n.spawnX();
						int by = n.y() != 0 ? n.y() : n.spawnY();
						int bz = n.z() != 0 ? n.z() : n.spawnZ();
						send(new GameServerPacket.RadarControl(0, 1, bx, by, bz));
						found = true;
						break;
					}
				}
			}
			// Radar location if alive NPC found
		}
	}

	private ScheduledFuture<?> chargeDecayTask;

	public void increaseCharges(int count, int max) {
		if (active == null) {
			return;
		}
		int cur = active.charges();
		if (cur >= max) {
			resetChargeDecayTask();
			return;
		}
		int newCharges = Math.min(max, cur + count);
		active.charges(newCharges);
		if (newCharges >= max) {
			send(SystemMessage.id(SystemMessage.FORCE_MAXLEVEL_REACHED));
		} else {
			send(SystemMessage.of(SystemMessage.FORCE_INCREASED_TO_S1, new SystemMessage.Number(newCharges)));
		}
		sendEtcStatusUpdate();
		resetChargeDecayTask();
	}

	public void decreaseCharges(int count) {
		if (active == null) {
			return;
		}
		int cur = active.charges();
		active.charges(Math.max(0, cur - count));
		sendEtcStatusUpdate();
		if (active.charges() <= 0) {
			stopChargeTask();
		} else {
			resetChargeDecayTask();
		}
	}

	public void clearCharges() {
		if (active == null) {
			return;
		}
		if (active.charges() > 0) {
			active.charges(0);
			stopChargeTask();
			sendEtcStatusUpdate();
		}
	}

	public void stopChargeTask() {
		if (chargeDecayTask != null) {
			chargeDecayTask.cancel(false);
			chargeDecayTask = null;
		}
	}

	private void resetChargeDecayTask() {
		stopChargeTask();
		chargeDecayTask = autoAttackScheduler.schedule(() -> {
			if (active != null && active.charges() > 0) {
				active.charges(0);
				sendEtcStatusUpdate();
			}
		}, 10, TimeUnit.MINUTES);
	}

	public void sendEtcStatusUpdate() {
		if (active == null) {
			return;
		}
		var t = ctx.characters() != null ? ctx.characters().template(active) : null;
		var stats = t != null ? com.lopez.l2j.game.model.PlayerStats.calculate(active, t) : null;
		send(new GameServerPacket.EtcStatusUpdate(active, stats));
	}

	private void onRestartPoint(GameClientPacket.RequestRestartPoint p) {
		if (!inWorld || active == null || !active.isDead()) {
			send(new ActionFailed());
			return;
		}
		clearTarget();
		if (ctx != null && ctx.npcAi() != null) {
			ctx.npcAi().stopCombatForPlayer(active.objectId());
		}
		clearHotTasks();
		active.stopAllAbnormalEffects();
		if (!active.effects().hasSkill(1323)) {
			active.effects().clear();
			if (ctx != null && ctx.buffRepository() != null) {
				ctx.buffRepository().deleteBuffs(active.objectId());
			}
		}
		pendingRevive = true;
		int[] townLoc = findNearestTown(active.x(), active.y());
		teleportToLocation(townLoc[0], townLoc[1], townLoc[2]);
		sendMagicEffectIcons();
	}

	/**
	 * UseItem: equipaveis alternam equipar; consumiveis (pocoes, soulshots,
	 * scrolls) passam pelos handlers.
	 */
	public void onUseItem(UseItem p) {
		itemHandler.handleUseItem(p);
	}

	public void useConsumable(Consumable c) {
		itemHandler.useConsumable(c);
	}

	/** UserInfo para o proprio jogador e CharInfo para quem esta por perto. */
	public void broadcastAppearance() {
		sendUserInfoAndBroadcastCharInfo();
	}

	/** Tira {@code count} unidades do item e manda InventoryUpdate + peso. */
	public boolean consumeItem(int itemId, int count) {
		return itemHandler.consumeItem(itemId, count);
	}

	public static boolean isChestNpc(NpcInstance npc) {
		if (npc == null || npc.template() == null) {
			return false;
		}
		int id = npc.npcId();
		if (id >= 18257 && id <= 18298) {
			return true;
		}
		if (id >= 21801 && id <= 21824) {
			return true;
		}
		String type = npc.template().type();
		if (type != null && type.equalsIgnoreCase("L2Chest")) {
			return true;
		}
		String name = npc.name();
		if (name != null) {
			String lower = name.toLowerCase(java.util.Locale.ROOT);
			if (lower.contains("chest") || lower.contains("box")) {
				return true;
			}
		}
		return false;
	}

	public static int getRequiredChestKeyGrade(int chestLevel) {
		if (chestLevel < 30) return 1;
		if (chestLevel < 40) return 2;
		if (chestLevel < 50) return 3;
		if (chestLevel < 60) return 4;
		if (chestLevel < 70) return 5;
		if (chestLevel < 76) return 6;
		if (chestLevel < 80) return 7;
		return 8;
	}

	public void broadcastSelfSkill(int skillId, int level) {
		var msu = new MagicSkillUse(active.objectId(), active.objectId(), skillId, level, 0, 0,
				active.x(), active.y(), active.z(), active.x(), active.y(), active.z());
		send(msu);
		ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, msu, false);
	}

	public void sendVitals() {
		send(new StatusUpdate(active.objectId(), List.of(
				new StatusUpdate.Attribute(StatusUpdate.CUR_HP, (int) active.currentHp()),
				new StatusUpdate.Attribute(StatusUpdate.MAX_HP, active.maxHp()),
				new StatusUpdate.Attribute(StatusUpdate.CUR_MP, (int) active.currentMp()),
				new StatusUpdate.Attribute(StatusUpdate.MAX_MP, active.maxMp()),
				new StatusUpdate.Attribute(StatusUpdate.CUR_CP, (int) active.currentCp()),
				new StatusUpdate.Attribute(StatusUpdate.MAX_CP, active.maxCp()))));
	}

	/**
	 * HealOverTime/ManaHealOverTime: um efeito por stackType, o novo substitui o
	 * anterior.
	 */
	private void startHealOverTime(Consumable c) {
		boolean hp = c.type() == ConsumableTable.Type.HOT_HP;
		String stack = hp ? "HpRecover" : "MpRecover";
		var previous = hotTasks.remove(stack);
		if (previous != null) {
			previous.cancel(false);
		}
		PlayerCharacter owner = active;
		int[] remaining = { c.ticks() };
		AtomicReference<ScheduledFuture<?>> self = new AtomicReference<>();
		ScheduledFuture<?> task = autoAttackScheduler.scheduleAtFixedRate(() -> {
			if (active != owner || !inWorld || owner.isDead() || remaining[0] <= 0) {
				stopHot(stack, self.get());
				return;
			}
			remaining[0]--;
			if (hp) {
				owner.currentHp(owner.currentHp() + c.amount());
			} else {
				owner.currentMp(owner.currentMp() + c.amount());
			}
			sendVitals();
			if (remaining[0] <= 0) {
				stopHot(stack, self.get());
			}
		}, c.intervalMs(), c.intervalMs(), TimeUnit.MILLISECONDS);
		self.set(task);
		hotTasks.put(stack, task);
	}

	private void stopHot(String stack, ScheduledFuture<?> task) {
		if (task != null) {
			task.cancel(false);
			hotTasks.remove(stack, task);
		}
	}

	private void applyBuff(Consumable c) {
		applyBuff(c, 0);
	}

	private void applyBuff(Consumable c, long remainingMs) {
		var d = c.buff();
		long durationMs = remainingMs > 0 ? remainingMs : d.durationMs();
		var buff = new ActiveBuff(c.skillId(), c.level(), d.stackType(), System.currentTimeMillis() + durationMs,
				d.runSpdAdd(), d.pAtkSpdMul(), d.mAtkSpdMul(), d.accuracyAdd());
		PlayerCharacter owner = active;
		int maxBuffs = Config.MAX_BUFFS_AMOUNT > 0 ? Config.MAX_BUFFS_AMOUNT : 20;
		var currentActive = owner.effects().active();
		if (currentActive.size() >= maxBuffs && !owner.effects().hasSkill(c.skillId())) {
			owner.effects().remove(currentActive.get(0));
		}
		owner.effects().put(buff);
		send(SystemMessage.of(SystemMessage.YOU_FEEL_S1_EFFECT, new SystemMessage.SkillName(c.skillId(), c.level())));
		refreshBuffs();
		saveBuffs();
		autoAttackScheduler.schedule(() -> {
			if (owner.effects().remove(buff) && active == owner && inWorld) {
				send(SystemMessage.of(SystemMessage.S1_HAS_WORN_OFF,
						new SystemMessage.SkillName(c.skillId(), c.level())));
				refreshBuffs();
				saveBuffs();
			}
		}, durationMs, TimeUnit.MILLISECONDS);
	}

	public void sendMagicEffectIcons() {
		refreshBuffs();
	}

	/** Icones de buff + UserInfo (velocidade/atk spd/max HP mudam). */
	public void refreshBuffs() {
		if (active == null) {
			return;
		}
		long now = System.currentTimeMillis();
		// um icone por skill (um skill pode ter varios efeitos com stackTypes
		// diferentes)
		Map<Integer, MagicEffectIcons.Icon> icons = new java.util.LinkedHashMap<>();
		for (var b : active.effects().active()) {
			icons.putIfAbsent(b.skillId(), new MagicEffectIcons.Icon(b.skillId(), b.level(), b.remainingSeconds(now)));
		}
		send(new MagicEffectIcons(List.copyOf(icons.values())));
		var t = ctx.characters().template(active);
		if (ctx.skillService() != null) {
			recalcMaxVitals(t);
		}
		sendUserInfoAndBroadcastCharInfo();
	}

	public void saveBuffs() {
		if (active == null || ctx.buffRepository() == null) {
			return;
		}
		var buffsToSave = active.effects().active().stream().filter(b -> !b.isDebuff()).toList();
		ctx.buffRepository().saveBuffs(active.objectId(), 0, buffsToSave);
	}

	private void restoreBuffs() {
		if (active == null || ctx.buffRepository() == null) {
			return;
		}
		var saved = ctx.buffRepository().restoreBuffs(active.objectId(), 0);
		if (saved == null || saved.isEmpty()) {
			return;
		}
		long now = System.currentTimeMillis();
		for (var s : saved) {
			long remainingMs = s.systime() == com.lopez.l2j.game.effect.PlayerEffects.PERMANENT
					? com.lopez.l2j.game.effect.PlayerEffects.PERMANENT
					: s.systime() - now;
			if (remainingMs <= 0) {
				continue;
			}
			// 1. Tenta restaurar como poção / consumível
			var consumableOpt = ConsumableTable.bySkillId(s.skillId());
			if (consumableOpt.isPresent()) {
				applyBuff(consumableOpt.get(), remainingMs);
				continue;
			}
			// 2. Tenta restaurar como skill da SkillTable
			if (ctx.skillService() != null) {
				var skillOpt = ctx.skillService().table().get(s.skillId(), s.level());
				if (skillOpt.isPresent()) {
					applySkillEffects(skillOpt.get(), false, remainingMs);
					continue;
				}
			}
			// 3. Fallback genérico para qualquer outro buff
			PlayerCharacter owner = active;
			var buff = new ActiveBuff(s.skillId(), s.level(), "saved_" + s.skillId(),
					System.currentTimeMillis() + remainingMs, 0, 1.0, 1.0, 0);
			owner.effects().put(buff);
			autoAttackScheduler.schedule(() -> {
				if (owner.effects().remove(buff) && active == owner && inWorld) {
					refreshBuffs();
					saveBuffs();
				}
			}, remainingMs, TimeUnit.MILLISECONDS);
		}
		refreshBuffs();
	}

	// ---- Soulshots ----

	public void onAutoSoulShot(RequestAutoSoulShot p) {
		itemHandler.handleAutoSoulShot(p);
	}

	public void rechargeAutoSoulShots() {
		itemHandler.rechargeAutoSoulShots();
	}

	public com.lopez.l2j.game.item.ItemInstance activeWeapon() {
		return itemHandler.activeWeapon();
	}

	public boolean chargeSoulShot(int itemId, boolean quiet) {
		return itemHandler.chargeSoulShot(itemId, quiet);
	}

	public boolean chargeSpiritShot(int itemId, boolean quiet, boolean blessed) {
		return itemHandler.chargeSpiritShot(itemId, quiet, blessed);
	}

	public void onUnEquip(RequestUnEquipItem p) {
		itemHandler.handleUnEquip(p);
	}

	public void afterEquipChange(EquipResult r) {
		itemHandler.afterEquipChange(r);
	}

	public void updateArmorSetBonus() {
		itemHandler.updateArmorSetBonus();
	}

	public void updateEquippedItemSkills() {
		itemHandler.updateEquippedItemSkills();
	}

	private void triggerWeaponOnCritSkill(NpcInstance npc, GameSession targetPlayer) {
		var weapon = activeWeapon();
		if (weapon == null || weapon.template() == null) {
			return;
		}
		var onCrit = weapon.template().onCritSkill();
		if (onCrit == null) {
			return;
		}
		if (java.util.concurrent.ThreadLocalRandom.current().nextInt(100) < onCrit.chance()) {
			triggerWeaponSkill(onCrit, npc, targetPlayer);
		}
	}

	private void triggerWeaponOnCastSkill(NpcInstance npc, GameSession targetPlayer) {
		var weapon = activeWeapon();
		if (weapon == null || weapon.template() == null) {
			return;
		}
		var onCast = weapon.template().onCastSkill();
		if (onCast == null) {
			return;
		}
		if (java.util.concurrent.ThreadLocalRandom.current().nextInt(100) < onCast.chance()) {
			triggerWeaponSkill(onCast, npc, targetPlayer);
		}
	}

	private void triggerWeaponSkill(ItemSkillHolder holder, NpcInstance npc, GameSession targetPlayer) {
		if (ctx.skillService() == null || ctx.skillService().table() == null) {
			return;
		}
		var skOpt = ctx.skillService().table().get(holder.skillId(), holder.level());
		if (skOpt.isEmpty()) {
			return;
		}
		var sk = skOpt.get();
		int targetId = npc != null ? npc.objectId()
				: (targetPlayer != null && targetPlayer.active != null ? targetPlayer.active.objectId() : active.objectId());
		var msu = new MagicSkillUse(active.objectId(), targetId, sk.id(), sk.level(), 0, 0, active.x(), active.y(),
				active.z(), active.x(), active.y(), active.z());
		send(msu);
		ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, msu, false);
		send(SystemMessage.of(SystemMessage.USE_S1, new SystemMessage.SkillName(sk.id(), sk.level())));

		var t = ctx.characters() != null ? ctx.characters().template(active) : null;
		if (npc != null && !npc.isDead()) {
			applyOffensive(sk, npc, t, false, false, false);
		} else if (targetPlayer != null && targetPlayer.active != null && !targetPlayer.active.isDead()) {
			applyOffensivePlayer(sk, targetPlayer, t, false, false, false);
		}
	}

	public void updateAugmentationBonus() {
		itemHandler.updateAugmentationBonus();
	}

	private void onLogout() {
		if (active != null && active.isInCombat() && !active.isGm()) {
			send(SystemMessage.of(SystemMessage.CANT_LOGOUT_WHILE_IN_COMBAT));
			send(new ActionFailed());
			return;
		}
		leaveWorld();
		close(new LeaveWorld());
	}

	private void onRestart() {
		if (active != null && active.isInCombat() && !active.isGm()) {
			send(SystemMessage.of(SystemMessage.CANT_RESTART_WHILE_IN_COMBAT));
			send(new ActionFailed());
			send(new RestartResponse(false));
			return;
		}
		leaveWorld();
		state = State.AUTHED;
		send(new RestartResponse(true));
		sendCharacterList();
	}

	private void leaveWorld() {
		targetObjectId = 0;
		if (active == null) {
			return;
		}
		if (party != null) {
			party.removeMember(this);
			party = null;
		}
		if (inWorld) {
			if (ctx.offlineTrade() != null && ctx.offlineTrade().isOfflineTrader(active.objectId())) {
				log.info("{} entrou em modo loja offline (permanece no mundo)", active.name());
			} else {
				ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, new DeleteObject(active.objectId()), false);
				for (var p : ctx.world().players()) {
					if (p instanceof GameSession gs && gs != this) {
						gs.knownObjects.remove(active.objectId());
					}
				}
				ctx.world().remove(this);
				ctx.characters().save(active, false);
				knownObjects.clear();
				log.info("{} saiu do mundo", active.name());
				inWorld = false;
			}
		}
		hotTasks.values().forEach(f -> f.cancel(false));
		hotTasks.clear();
		autoSoulShots.clear();
		soulshotCharged = false;
		spiritshotCharged = false;
		stopVitalsRegenTask();
		stopChargeTask();
		consumableReuse.clear();
		skillReuse.clear();
		var cast = castTask;
		if (cast != null) {
			cast.cancel(false);
		}
		casting = false;
		castingSkill = null;
		if (active.isDead()) {
			if (ctx.buffRepository() != null) {
				ctx.buffRepository().deleteBuffs(active.objectId());
			}
		} else {
			saveBuffs();
		}
		active.effects().clear();
		active.stopAbnormalEffect(ConsumableTable.ABNORMAL_BIG_HEAD);
		macros.clear();
		macroRevision = 1;
		nextMacroId = 1000;
		active = null;
	}

	private void onShortCutReg(RequestShortCutReg p) {
		if (!inWorld) {
			send(new ActionFailed());
			return;
		}
		int slot = p.slot() % 12;
		int page = p.slot() / 12;
		int level = p.type() == ShortCut.TYPE_SKILL ? Math.max(1, active.skillLevel(p.id())) : -1;
		var sc = new ShortCut(slot, page, p.type(), p.id(), level, p.characterType());
		if (ctx.shortcuts() != null) {
			ctx.shortcuts().save(active.objectId(), 0, sc);
		}
		send(new ShortCutRegister(sc));
	}

	private void onShortCutDel(RequestShortCutDel p) {
		if (!inWorld) {
			send(new ActionFailed());
			return;
		}
		int slot = p.id() % 12;
		int page = p.id() / 12;
		if (ctx.shortcuts() != null) {
			ctx.shortcuts().delete(active.objectId(), 0, slot, page);
		}
	}

	private void loadMacros() {
		macros.clear();
		if (ctx.macros() != null && active != null) {
			List<Macro> list = ctx.macros().findByCharId(active.objectId());
			int maxId = 999;
			for (Macro m : list) {
				macros.put(m.id(), m);
				if (m.id() > maxId) {
					maxId = m.id();
				}
			}
			nextMacroId = maxId + 1;
		}
		sendMacroList();
	}

	private void sendMacroList() {
		macroRevision++;
		if (macros.isEmpty()) {
			send(new SendMacroList(macroRevision, 0, null));
		} else {
			for (Macro m : macros.values()) {
				send(new SendMacroList(macroRevision, macros.size(), m));
			}
		}
	}

	private void onMakeMacro(RequestMakeMacro p) {
		if (!inWorld || active == null) {
			send(new ActionFailed());
			return;
		}
		Macro macro = p.macro();
		if (macro == null) {
			send(new ActionFailed());
			return;
		}

		int totalCmdLength = 0;
		for (MacroCmd cmd : macro.commands()) {
			if (cmd.cmd() != null) {
				totalCmdLength += cmd.cmd().length();
			}
		}
		if (totalCmdLength > 255) {
			send(SystemMessage.id(SystemMessage.INVALID_MACRO));
			return;
		}

		if (macro.name().isEmpty()) {
			send(SystemMessage.id(SystemMessage.ENTER_THE_MACRO_NAME));
			return;
		}

		if (macro.descr().length() > 32) {
			send(SystemMessage.id(SystemMessage.MACRO_DESCRIPTION_MAX_32_CHARS));
			return;
		}

		boolean isNew = macro.id() == 0 || !macros.containsKey(macro.id());
		if (isNew && macros.size() >= 48) {
			send(SystemMessage.id(SystemMessage.YOU_MAY_CREATE_UP_TO_48_MACROS));
			return;
		}

		int macroId = macro.id();
		if (isNew) {
			macroId = nextMacroId++;
			while (macros.containsKey(macroId)) {
				macroId = nextMacroId++;
			}
			macro = macro.withId(macroId);
		}

		macros.put(macroId, macro);
		if (ctx.macros() != null) {
			ctx.macros().save(active.objectId(), macro);
		}
		sendMacroList();
	}

	private void onDeleteMacro(RequestDeleteMacro p) {
		if (!inWorld || active == null) {
			send(new ActionFailed());
			return;
		}
		int macroId = p.id();
		Macro removed = macros.remove(macroId);
		if (removed != null && ctx.macros() != null) {
			ctx.macros().delete(active.objectId(), macroId);
		}

		if (ctx.shortcuts() != null) {
			ctx.shortcuts().deleteByTypeAndId(active.objectId(), ShortCut.TYPE_MACRO, macroId);
		}

		sendMacroList();
	}

	// ---- Skills (porta enxuta de L2Character.doCast/onMagicHitTimer + handlers de
	// skill do L2JDream) ----

	/** Conjuracao em andamento (um skill por vez, como no cliente). */
	private volatile boolean casting;
	private volatile SkillTemplate castingSkill;
	private volatile ScheduledFuture<?> castTask;
	/** Reuse por skillId (epoch ms em que libera). */
	private final Map<Integer, Long> skillReuse = new ConcurrentHashMap<>();
	/**
	 * Treinador da ultima janela de skills aberta (RequestAcquireSkill confere a
	 * distancia).
	 */
	private int lastTrainerObjectId;

	public void onMagicSkillUse(RequestMagicSkillUse p) {
		actionHandler.handleMagicSkillUse(p);
	}

	/**
	 * Valida e inicia a conjuracao; {@code mayMove} = pode andar ate o alvo antes
	 * (uma vez).
	 */
	public void castSkill(SkillTemplate sk, boolean mayMove) {
		if (!inWorld || active == null || active.isDead() || active.isDisabled()) {
			return;
		}
		if (sk.magic() && active.isMuted()) {
			send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED, new SystemMessage.SkillName(sk.id(), sk.level())));
			send(new ActionFailed());
			return;
		}
		if (!sk.magic() && active.isPhysicalMuted()) {
			send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED, new SystemMessage.SkillName(sk.id(), sk.level())));
			send(new ActionFailed());
			return;
		}
		if (casting) {
			send(new ActionFailed());
			return;
		}
		long now = System.currentTimeMillis();
		Long readyAt = skillReuse.get(sk.id());
		if (readyAt != null && readyAt > now) {
			send(SystemMessage.of(SystemMessage.S1_PREPARED_FOR_REUSE,
					new SystemMessage.SkillName(sk.id(), sk.level())));
			send(new ActionFailed());
			return;
		}
		if (isBow(activeWeapon()) && !sk.magic()) {
			if (!checkAndConsumeArrow()) {
				return;
			}
		}
		if (sk.itemConsumeId() > 0 && sk.itemConsumeCount() > 0) {
			int have = active.inventory().byItemId(sk.itemConsumeId()).map(ItemInstance::count).orElse(0);
			if (have < sk.itemConsumeCount()) {
				send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED, new SystemMessage.SkillName(sk.id(), sk.level())));
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Itens insuficientes para usar esta habilidade."));
				send(new ActionFailed());
				return;
			}
		}
		if (active.currentMp() < sk.mpConsume() + sk.mpInitialConsume()) {
			send(SystemMessage.id(SystemMessage.NOT_ENOUGH_MP));
			send(new ActionFailed());
			return;
		}
		if (sk.hpConsume() > 0 && active.currentHp() <= sk.hpConsume()) {
			send(SystemMessage.id(SystemMessage.NOT_ENOUGH_HP));
			send(new ActionFailed());
			return;
		}
		if (sk.needCharges() > 0 && active.charges() < sk.needCharges()) {
			send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED, new SystemMessage.SkillName(sk.id(), sk.level())));
			send(new ActionFailed());
			return;
		}
		if (sk.giveCharges() > 0 && !sk.continueAfterMax() && active.charges() >= sk.maxCharges()) {
			send(SystemMessage.id(SystemMessage.FORCE_MAXLEVEL_REACHED));
			send(new ActionFailed());
			return;
		}
		if (sk.itemConsumeId() > 0 && sk.itemConsumeCount() > 0) {
			if (active.inventory().getItemCount(sk.itemConsumeId()) < sk.itemConsumeCount()) {
				send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED, new SystemMessage.ItemName(sk.itemConsumeId())));
				send(new ActionFailed());
				return;
			}
		}

		// Alvo principal
		NpcInstance npcTarget = null;
		DoorInstance doorTarget = null;
		GameSession playerTarget = null;
		boolean isSweep = "TARGET_CORPSE_MOB".equalsIgnoreCase(sk.target())
				|| "SWEEP".equalsIgnoreCase(sk.skillType()) || sk.id() == 42;
		boolean isResurrect = !isSweep && ("RESURRECT".equalsIgnoreCase(sk.skillType()) || sk.target().startsWith("TARGET_CORPSE_"));
		boolean isUnlock = "UNLOCK".equalsIgnoreCase(sk.skillType())
				|| "DELUXE_KEY_UNLOCK".equalsIgnoreCase(sk.skillType())
				|| "TARGET_UNLOCKABLE".equalsIgnoreCase(sk.target());

		if (isSweep) {
			npcTarget = ctx.world().npc(targetObjectId).filter(n -> n.isAttackable() && n.isDead()).orElse(null);
			if (npcTarget == null) {
				send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
				send(new ActionFailed());
				return;
			}
			if (!npcTarget.isSpoiled()) {
				send(SystemMessage.id(SystemMessage.SWEEPER_FAILED_TARGET_NOT_SPOILED));
				send(new ActionFailed());
				return;
			}
		} else if (isUnlock) {
			npcTarget = ctx.world().npc(targetObjectId).filter(n -> !n.isDead() && isChestNpc(n)).orElse(null);
			if (npcTarget == null && ctx.doors() != null) {
				doorTarget = ctx.doors().door(targetObjectId);
			}
			if (npcTarget == null && doorTarget == null) {
				send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
				send(new ActionFailed());
				return;
			}
			if (doorTarget != null && !doorTarget.unlockable()) {
				send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
				send(new ActionFailed());
				return;
			}
		} else if (sk.isOffensive()) {
			if (!sk.isAreaAroundSelf()) {
				npcTarget = ctx.world().npc(targetObjectId).filter(n -> n.isAttackable() && !n.isDead()).orElse(null);
				if (npcTarget == null) {
					var other = ctx.world().player(targetObjectId).orElse(null);
					if (other instanceof GameSession gs && gs.active != null && !gs.active.isDead() && gs != this) {
						playerTarget = gs;
					} else {
						send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
						send(new ActionFailed());
						return;
					}
				}
			}
		} else if (isResurrect) {
			if (targetObjectId != 0 && targetObjectId != active.objectId()) {
				var other = ctx.world().player(targetObjectId).orElse(null);
				if (other instanceof GameSession gs && gs.active != null && gs.active.isDead()) {
					playerTarget = gs;
				} else {
					send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
					send(new ActionFailed());
					return;
				}
			} else {
				send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
				send(new ActionFailed());
				return;
			}
		} else if (!sk.isSelfTargeted() && targetObjectId != 0 && targetObjectId != active.objectId()) {
			var other = ctx.world().player(targetObjectId).orElse(null);
			if (other instanceof GameSession gs && gs.active != null && !gs.active.isDead()) {
				playerTarget = gs;
			} else {
				playerTarget = this;
			}
		} else {
			playerTarget = this;
		}

		// Verificacao de Zona de Paz para habilidades ofensivas (aplica-se a PvP contra outros jogadores, nao a monstros)
		if (sk.isOffensive() && ctx.zones() != null && playerTarget != null && playerTarget != this) {
			if (ctx.zones().isInsidePeace(active.x(), active.y(), active.z())
					|| ctx.zones().isInsidePeace(playerTarget.x(), playerTarget.y(), playerTarget.z())) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao pode usar habilidades ofensivas em zona de paz."));
				send(new ActionFailed());
				return;
			}
		}

		// Bloqueia suporte (cura/buff) a jogadores flaggados ou PK a partir de zona de paz
		if (!sk.isOffensive() && ctx.zones() != null && playerTarget != null && playerTarget != this) {
			if (ctx.zones().isInsidePeace(active.x(), active.y(), active.z())
					&& (playerTarget.active.pvpFlag() > 0 || playerTarget.active.karma() > 0)) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao pode dar suporte a jogadores em combate a partir de uma zona de paz."));
				send(new ActionFailed());
				return;
			}
		}

		// Validacao de condicoes da skill levando o alvo em consideracao (ex: <target undead="true" />)
		Object resolvedTarget = npcTarget != null ? npcTarget : (playerTarget != null ? playerTarget.active : null);
		if (sk.castCondition() != null && !sk.castCondition().test(active, resolvedTarget)) {
			if (sk.condMsg() != null && !sk.condMsg().isBlank()) {
				try {
					int msgId = Integer.parseInt(sk.condMsg().trim());
					send(SystemMessage.id(msgId));
				} catch (NumberFormatException e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", sk.condMsg()));
				}
			} else {
				send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
			}
			send(new ActionFailed());
			return;
		}

		// Alcance (o L2J anda ate o alvo e depois conjura)
		int tx = npcTarget != null ? npcTarget.x() : (doorTarget != null ? doorTarget.x() : (playerTarget != null ? playerTarget.x() : active.x()));
		int ty = npcTarget != null ? npcTarget.y() : (doorTarget != null ? doorTarget.y() : (playerTarget != null ? playerTarget.y() : active.y()));
		int tz = npcTarget != null ? npcTarget.z() : (doorTarget != null ? doorTarget.z() : (playerTarget != null ? playerTarget.z() : active.z()));
		if (sk.castRange() > 0 && (npcTarget != null || doorTarget != null || (playerTarget != null && playerTarget != this))) {
			double dist = Math.hypot(active.x() - tx, active.y() - ty);
			if (dist > 3000.0) {
				send(SystemMessage.id(SystemMessage.TARGET_TOO_FAR));
				send(new ActionFailed());
				clearTarget();
				return;
			}
			double maxDist = sk.castRange() + 70 + (npcTarget != null ? npcTarget.template().collisionRadius() : 0);
			if (dist > maxDist) {
				if (!mayMove) {
					send(SystemMessage.id(SystemMessage.TARGET_TOO_FAR));
					send(new ActionFailed());
					return;
				}
				int targetId = npcTarget != null ? npcTarget.objectId() : (doorTarget != null ? doorTarget.objectId() : playerTarget.objectId());
				var move = new MoveToPawn(active.objectId(), targetId, sk.castRange(), active.x(), active.y(),
						active.z());
				send(move);
				ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, move, false);
				int run = Math.max(50, PlayerStats.calculate(active, ctx.characters().template(active)).runSpeed());
				long travelMs = (long) ((dist - sk.castRange()) * 1000 / run) + 200;
				double angle = Math.atan2(active.y() - ty, active.x() - tx);
				int stopDist = Math.max(20, sk.castRange() - 30);
				int stopX = (int) (tx + stopDist * Math.cos(angle));
				int stopY = (int) (ty + stopDist * Math.sin(angle));
				int stopZ = tz;
				PlayerCharacter owner = active;
				autoAttackScheduler.schedule(() -> {
					if (active == owner) {
						active.moveTo(stopX, stopY, stopZ);
						castSkill(sk, false);
					}
				}, travelMs, TimeUnit.MILLISECONDS);
				return;
			}
		}

		// Line of Sight (LoS) check for targeted skills
		if (sk.isOffensive() && (npcTarget != null || (playerTarget != null && playerTarget != this))) {
			boolean canSee = true;
			if (npcTarget != null) {
				canSee = ctx.combat() == null || ctx.combat().canSeeTarget(active, npcTarget);
			} else if (playerTarget != null && playerTarget.active != null) {
				canSee = ctx.combat() == null || ctx.combat().canSeeTarget(active, playerTarget.active);
			}
			if (!canSee) {
				send(SystemMessage.id(SystemMessage.CANT_SEE_TARGET));
				send(new ActionFailed());
				return;
			}
		}

		// Inicio da conjuracao
		var t = ctx.characters().template(active);
		var stats = PlayerStats.calculate(active, t);
		if (sk.magic() && !spiritshotCharged) {
			rechargeAutoSoulShots();
		}
		boolean sps = sk.magic() && spiritshotCharged && !blessedSpiritshot;
		boolean bss = sk.magic() && spiritshotCharged && blessedSpiritshot;
		spiritshotCharged = false;

		double speedFactor = 333.0 / Math.max(1, sk.magic() ? stats.mAtkSpd() : stats.pAtkSpd());
		if (bss) {
			speedFactor *= 0.65;
		}
		int minHit = (active.isGm() || active.gmSpeed() > 0) ? 10 : Config.getInt("MinimumHitTime", 330);
		int hitTime = sk.hitTime() > 0 ? Math.max(minHit, (int) (sk.hitTime() * speedFactor)) : 0;
		int reuse = (int) (sk.reuseDelay() * speedFactor);
		double mReuse = PlayerStats.applyStat(active, "mReuse", 1.0);
		if (mReuse > 1.0) {
			reuse = (int) (reuse / mReuse);
		}
		active.currentMp(active.currentMp() - sk.mpInitialConsume());
		if (reuse > 0) {
			skillReuse.put(sk.id(), now + Math.max(reuse, hitTime));
		}
		boolean isSleepOrTrance = "SLEEP".equalsIgnoreCase(sk.skillType()) || sk.id() == 1069 || sk.id() == 1394 || sk.id() == 1071 || sk.id() == 1201;
		if (isSleepOrTrance) {
			stopAutoAttack();
			autoAttacking = false;
		}
		boolean resumeAttack = !isSleepOrTrance && autoAttacking && (npcTarget != null || playerTarget != null) && sk.nextActionAttack();
		autoAttacking = false; // o auto-ataque para durante o cast

		// Para a movimentacao do personagem para conjurar a habilidade ("da uma paradinha")
		var stop = new StopMove(active.objectId(), active.x(), active.y(), active.z(), active.heading());
		send(stop);
		ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, stop, false);

		int mainTargetId = npcTarget != null ? npcTarget.objectId()
				: (doorTarget != null ? doorTarget.objectId()
				: (playerTarget != null ? playerTarget.objectId() : active.objectId()));
		var msu = new MagicSkillUse(active.objectId(), mainTargetId, sk.id(), sk.level(), hitTime, reuse,
				active.x(), active.y(), active.z(), tx, ty, tz);
		send(msu);
		ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, msu, false);
		if (sk.isOffensive() && sk.nextActionAttack()) {
			var startAtk = new AutoAttackStart(active.objectId());
			send(startAtk);
			ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, startAtk, false);
			if (playerTarget != null && playerTarget != this) {
				var startAtkTgt = new AutoAttackStart(playerTarget.objectId());
				playerTarget.send(startAtkTgt);
				ctx.world().broadcastAround(playerTarget, GameWorld.VISIBILITY_RADIUS, startAtkTgt, false);
			}
		}
		send(SystemMessage.of(SystemMessage.USE_S1, new SystemMessage.SkillName(sk.id(), sk.level())));
		if (hitTime > 0) {
			send(new GameServerPacket.SetupGauge(GameServerPacket.SetupGauge.BLUE, hitTime));
		}
		if (sk.mpInitialConsume() > 0) {
			sendVitals();
		}

		casting = true;
		castingSkill = sk;
		PlayerCharacter owner = active;
		NpcInstance npc = npcTarget;
		DoorInstance door = doorTarget;
		GameSession targetSess = playerTarget;
		Runnable finish = () -> {
			if (!casting || active != owner || !inWorld || owner.isDead()) {
				return;
			}
			casting = false;
			castingSkill = null;
			castTask = null;
			try {
				finishCast(sk, npc, door, targetSess, resumeAttack, sps, bss);
			} catch (RuntimeException e) {
				log.warn("Erro ao finalizar skill {} de {}: {}", sk.id(), owner.name(), e.toString());
			}
		};
		if (hitTime <= 0) {
			finish.run();
		} else {
			castTask = autoAttackScheduler.schedule(finish, hitTime, TimeUnit.MILLISECONDS);
		}
	}

	/** onMagicHitTimer: gasta MP, dispara o skill e aplica nos alvos. */
	private void finishCast(SkillTemplate sk, NpcInstance mainNpc, DoorInstance doorTarget, GameSession targetPlayer, boolean resumeAttack,
			boolean sps, boolean bss) {
		if (active.currentMp() < sk.mpConsume()) {
			send(SystemMessage.id(SystemMessage.NOT_ENOUGH_MP));
			return;
		}
		active.currentMp(active.currentMp() - sk.mpConsume());
		if (sk.hpConsume() > 0) {
			active.currentHp(active.currentHp() - sk.hpConsume());
		}
		if (sk.itemConsumeId() > 0 && sk.itemConsumeCount() > 0) {
			if (active.inventory().destroyItemByItemId(sk.itemConsumeId(), sk.itemConsumeCount())) {
				send(ItemList.of(active.inventory().items(), false));
				send(SystemMessage.of(SystemMessage.S1_DISAPPEARED, new SystemMessage.ItemName(sk.itemConsumeId())));
			}
		}
		var t = ctx.characters().template(active);

		if (sk.id() == 1312) { // Fishing
			if (ctx.fishing() != null) {
				int d = 150;
				double angle = Math.toRadians(active.heading() * (360.0 / 65536.0));
				int fx = active.x() + (int) (d * Math.cos(angle));
				int fy = active.y() + (int) (d * Math.sin(angle));
				int fz = active.z();
				ctx.fishing().startFishing(active, fx, fy, fz, this::send);
			}
			return;
		}
		if (sk.id() == 1313) { // Pumping
			if (ctx.fishing() != null) {
				ctx.fishing().handlePumping(active, sk.level(), (int) sk.power(), this::send);
			}
			return;
		}
		if (sk.id() == 1314) { // Reeling
			if (ctx.fishing() != null) {
				ctx.fishing().handleReeling(active, sk.level(), (int) sk.power(), this::send);
			}
			return;
		}

		if ("TARGET_CORPSE_MOB".equalsIgnoreCase(sk.target()) || "SWEEP".equalsIgnoreCase(sk.skillType()) || sk.id() == 42) {
			if (mainNpc != null && ctx.drops() != null) {
				ctx.drops().sweep(active, mainNpc, ctx.inventories(), this::send);
			}
			sendVitals();
			return;
		}

		boolean isUnlock = "UNLOCK".equalsIgnoreCase(sk.skillType())
				|| "DELUXE_KEY_UNLOCK".equalsIgnoreCase(sk.skillType())
				|| "TARGET_UNLOCKABLE".equalsIgnoreCase(sk.target());
		if (isUnlock) {
			if (doorTarget != null) {
				handleUnlockDoor(doorTarget, sk);
			} else if (mainNpc != null) {
				handleUnlockChest(mainNpc, sk);
			}
			sendVitals();
			return;
		}

		if (sk.isOffensive()) {
			boolean ss = false;
			if (sk.isPhysicalDamage() && sk.power() > 0) {
				if (!soulshotCharged) {
					rechargeAutoSoulShots();
				}
				ss = soulshotCharged;
				soulshotCharged = false;
			}

			List<NpcInstance> npcTargets = new ArrayList<>();
			List<GameSession> playerTargets = new ArrayList<>();
			int radius = sk.skillRadius() > 0 ? sk.skillRadius() : 200;

			if (sk.isAreaAroundSelf()) {
				npcTargets.addAll(ctx.world().findNpcsAround(active.x(), active.y(), radius));
				for (var p : ctx.world().findPlayersAround(active.x(), active.y(), radius)) {
					if (p instanceof GameSession gs && canAttackInPvP(gs)) {
						playerTargets.add(gs);
					}
				}
			} else if (sk.isAreaAroundTarget()) {
				int cx = mainNpc != null ? mainNpc.x() : (targetPlayer != null ? targetPlayer.x() : active.x());
				int cy = mainNpc != null ? mainNpc.y() : (targetPlayer != null ? targetPlayer.y() : active.y());
				if (mainNpc != null && !mainNpc.isDead()) {
					npcTargets.add(mainNpc);
				}
				if (targetPlayer != null && canAttackInPvP(targetPlayer)) {
					playerTargets.add(targetPlayer);
				}
				for (NpcInstance n : ctx.world().findNpcsAround(cx, cy, radius)) {
					if (!npcTargets.contains(n)) {
						npcTargets.add(n);
					}
				}
				for (var p : ctx.world().findPlayersAround(cx, cy, radius)) {
					if (p instanceof GameSession gs && canAttackInPvP(gs) && !playerTargets.contains(gs)) {
						playerTargets.add(gs);
					}
				}
			} else {
				if (targetPlayer != null && canAttackInPvP(targetPlayer)) {
					playerTargets.add(targetPlayer);
				} else if (mainNpc != null && !mainNpc.isDead()) {
					npcTargets.add(mainNpc);
				}
			}

			npcTargets.removeIf(n -> !n.isAttackable() || n.isDead());

			List<Integer> launchedIds = new ArrayList<>();
			for (NpcInstance n : npcTargets) {
				launchedIds.add(n.objectId());
			}
			for (GameSession gs : playerTargets) {
				launchedIds.add(gs.objectId());
			}
			broadcastLaunched(sk, launchedIds);

			for (NpcInstance n : npcTargets) {
				applyOffensive(sk, n, t, ss, sps, bss);
			}
			for (GameSession gs : playerTargets) {
				updatePvPFlag();
				gs.updatePvPFlag();
				applyOffensivePlayer(sk, gs, t, ss, sps, bss);
			}
			if (active != null) {
				if (sk.needCharges() > 0 && sk.consumeCharges()) {
					decreaseCharges(sk.needCharges());
				}
				if (sk.giveCharges() > 0) {
					increaseCharges(sk.giveCharges(), sk.maxCharges());
				}
				triggerWeaponOnCastSkill(mainNpc, targetPlayer);
			}
			sendVitals();
			if (resumeAttack) {
				if (mainNpc != null && !mainNpc.isDead() && targetObjectId == mainNpc.objectId()) {
					autoAttacking = true;
					schedulePlayerAutoAttack(mainNpc);
				} else if (targetPlayer != null && targetPlayer.active != null && !targetPlayer.active.isDead() && targetObjectId == targetPlayer.objectId()) {
					autoAttacking = true;
					schedulePlayerAutoAttack(targetPlayer);
				}
			}
			return;
		}

		// Skill positivo (cura, buff, resurrect, song, dance)
		List<GameSession> targets = new ArrayList<>();
		boolean isPartySkill = "TARGET_PARTY".equalsIgnoreCase(sk.target())
				|| "TARGET_PARTY_MEMBER".equalsIgnoreCase(sk.target())
				|| (!sk.isOffensive() && ("TARGET_AURA".equalsIgnoreCase(sk.target())
						|| "TARGET_CLAN".equalsIgnoreCase(sk.target())
						|| "TARGET_ALLY".equalsIgnoreCase(sk.target())));

		if (isPartySkill && party != null) {
			int radius = sk.skillRadius() > 0 ? sk.skillRadius() : 1000;
			double rSq = (double) radius * radius;
			boolean resurrect = "RESURRECT".equalsIgnoreCase(sk.skillType())
					|| sk.target().startsWith("TARGET_CORPSE_");
			for (GameSession member : party.members()) {
				if (member != null && member.active != null) {
					if (resurrect ? member.active.isDead() : !member.active.isDead()) {
						double dx = active.x() - member.active.x();
						double dy = active.y() - member.active.y();
						if (dx * dx + dy * dy <= rSq) {
							targets.add(member);
						}
					}
				}
			}
			if (targets.isEmpty()) {
				targets.add(this);
			}
		} else if (targetPlayer != null && targetPlayer.active != null) {
			targets.add(targetPlayer);
		} else {
			targets.add(this);
		}

		broadcastLaunched(sk, targets.stream().map(s -> s.active.objectId()).toList());
		if ("BALANCE_LIFE".equalsIgnoreCase(sk.skillType()) || sk.id() == 1335) {
			handleBalanceLife(sk, targets);
		} else {
			for (GameSession target : targets) {
				if (target != this && target.active != null) {
					if (target.active.pvpFlag() > 0 || target.active.karma() > 0) {
						updatePvPFlag(target.active.pvpFlag() > 0);
					}
				}
				target.receivePositiveSkill(sk, active.name());
			}
		}
		if (active != null) {
			if (sk.needCharges() > 0 && sk.consumeCharges()) {
				decreaseCharges(sk.needCharges());
			}
			triggerWeaponOnCastSkill(null, targetPlayer);
		}
		sendVitals();
	}

	private void broadcastLaunched(SkillTemplate sk, List<Integer> targets) {
		var launched = new GameServerPacket.MagicSkillLaunched(active.objectId(), sk.id(), sk.level(), targets);
		send(launched);
		ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, launched, false);
	}

	private void handleUnlockDoor(DoorInstance door, SkillTemplate sk) {
		if (door == null) {
			return;
		}
		if (!door.unlockable()) {
			send(SystemMessage.id(SystemMessage.UNABLE_TO_UNLOCK_DOOR));
			return;
		}
		if (door.isOpen()) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "A porta ja esta aberta."));
			return;
		}
		int chance = (int) Math.max(10, Math.min(100, sk.power() > 0 ? sk.power() : 50));
		if (ThreadLocalRandom.current().nextInt(100) < chance) {
			door.openDoor();
			var update = new GameServerPacket.DoorStatusUpdate(door);
			send(update);
			ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, update, false);
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Porta destrancada com sucesso!"));
			autoAttackScheduler.schedule(() -> {
				if (door.isOpen()) {
					door.closeDoor();
					var closeUpdate = new GameServerPacket.DoorStatusUpdate(door);
					send(closeUpdate);
					ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, closeUpdate, false);
				}
			}, 60, TimeUnit.SECONDS);
		} else {
			send(SystemMessage.id(SystemMessage.FAILED_TO_UNLOCK_DOOR));
		}
	}

	private void handleUnlockChest(NpcInstance npc, SkillTemplate sk) {
		if (npc == null || active == null || npc.isDead()) {
			return;
		}
		int chestLevel = npc.template() != null ? npc.template().level() : 1;
		boolean isMimic = npc.npcId() >= 18257 && npc.npcId() <= 18264;
		if (isMimic) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "O bau era uma armadilha (Mimic)!"));
			if (ctx.npcAi() != null) {
				ctx.npcAi().startCombat(npc, active.objectId());
			}
			return;
		}

		int neededGrade = getRequiredChestKeyGrade(chestLevel);
		int chance = 50;
		if ("DELUXE_KEY_UNLOCK".equalsIgnoreCase(sk.skillType()) || sk.id() == 2229) {
			int keyGrade = sk.level();
			if (keyGrade >= neededGrade) {
				chance = 100;
			} else {
				chance = Math.max(0, 100 - (neededGrade - keyGrade) * 40);
			}
		} else if (sk.id() == 2065) { // Box Key
			int keyGrade = sk.level();
			if (keyGrade >= neededGrade) {
				chance = 40;
			} else {
				chance = Math.max(0, 40 - (neededGrade - keyGrade) * 20);
			}
		} else if (sk.id() == 27) { // Unlock skill
			int magicLvl = sk.magicLevel() > 0 ? sk.magicLevel() : active.level();
			int lvlDiff = chestLevel - magicLvl;
			int baseChance = (int) (sk.power() > 0 ? sk.power() : 50);
			if (lvlDiff > 5) {
				baseChance -= (lvlDiff - 5) * 10;
			}
			chance = Math.max(5, Math.min(100, baseChance));
		}

		var t = ctx.characters() != null ? ctx.characters().template(active) : null;
		if (ThreadLocalRandom.current().nextInt(100) < chance) {
			var social = new SocialAction(active.objectId(), 3);
			send(social);
			ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, social, false);
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Bau aberto com sucesso!"));

			stopAutoAttack();
			npc.dead(true);
			npc.currentHp(0);

			if (ctx.npcAi() != null) {
				ctx.npcAi().stopCombat(npc);
				ctx.npcAi().scheduleDecayAndRespawn(npc);
			}

			var die = new Die(npc.objectId(), false);
			send(die);
			ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, die, false);

			if (npc.template() != null) {
				double rateExp = ctx.rates() != null ? ctx.rates().xp() : 1.0;
				double rateSp = ctx.rates() != null ? ctx.rates().sp() : 1.0;
				long exp = (long) (npc.template().exp() * rateExp);
				int sp = (int) (npc.template().sp() * rateSp);
				if (party != null) {
					double ratePartyXp = ctx.rates() != null ? ctx.rates().partyXp() : 1.0;
					double ratePartySp = ctx.rates() != null ? ctx.rates().partySp() : 1.0;
					party.distributeExpAndSp(exp, sp, active, ratePartyXp, ratePartySp);
				} else {
					applyExpAndSp(exp, sp, t);
				}
			}

			int rewardMobId = npc.npcId();
			if (rewardMobId >= 21801 && rewardMobId <= 21822) {
				rewardMobId -= 3536;
			}
			if (ctx.drops() != null) {
				ctx.drops().rewardMonsterDeath(active, rewardMobId, chestLevel, ctx.inventories(), this::send);
			}
			if (ctx.questManager() != null) {
				ctx.questManager().onNpcKill(npc, this, false);
			}
			ctx.characters().save(active, true);
		} else {
			var social = new SocialAction(active.objectId(), 13);
			send(social);
			ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, social, false);
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Falha ao abrir o bau!"));
			if (ctx.npcAi() != null) {
				ctx.npcAi().startCombat(npc, active.objectId());
			}
		}
	}

	/** Dano + debuffs de um skill ofensivo num monstro. */
	private void applyOffensive(SkillTemplate sk, NpcInstance npc, CharTemplate t, boolean soulshot,
			boolean sps, boolean bss) {
		var combat = ctx.combat();
		if (combat == null) {
			return;
		}
		if (combat.checkRaidCurse(active, npc)) {
			active.effects().addBuff(com.lopez.l2j.game.combat.CombatService.RAID_CURSE_SILENCE, 1, 120_000L);
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce foi atingido pela Maldicao de Raid Boss (Raid Curse)!"));
			return;
		}
		CombatService.HitResult hit = null;
		double effPower = sk.power();
		if ("DEATHLINK".equalsIgnoreCase(sk.skillType())) {
			double hpRatio = (double) active.currentHp() / Math.max(1, active.maxHp());
			effPower = sk.power() * Math.max(0.2, Math.pow(1.7165 - hpRatio, 2) * 0.577);
		} else if ("FATALCOUNTER".equalsIgnoreCase(sk.skillType())) {
			double hpRatio = (double) active.currentHp() / Math.max(1, active.maxHp());
			effPower = sk.power() * Math.max(0.2, 3.5 * (1.0 - hpRatio));
		}

		if (sk.isPhysicalDamage() && effPower > 0) {
			hit = combat.skillPhysicalNpc(active, t, npc, effPower, soulshot, sk.skillType().equals("BLOW"),
					sk.isChargedDam());
		} else if (sk.isMagicDamage() && effPower > 0) {
			hit = combat.skillMagicNpc(active, t, npc, effPower, sk.magicLevel(), sps, bss);
			if (sk.skillType().equals("DRAIN") && hit.damage() > 0) {
				double absorb = sk.absorbPart() > 0 ? sk.absorbPart() : 0.2;
				healHp(hit.damage() * absorb);
			}
		} else if (sk.isManaBurn() && effPower > 0) {
			int mpDam = combat.skillManaDamNpc(active, t, npc, effPower, sk.magicLevel(), sps, bss);
			if (mpDam > 0) {
				double newMp = Math.max(0, npc.currentMp() - mpDam);
				npc.currentMp(newMp);
				send(SystemMessage.of(SystemMessage.YOUR_OPPONENTS_MP_WAS_REDUCED_BY_S1, new SystemMessage.Number(mpDam)));
				var su = StatusUpdate.mp(npc.objectId(), (int) npc.currentMp(), npc.template().maxMp());
				send(su);
				ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, su, false);
			}
		}

		// Spoil (Skill 254)
		if (sk.id() == 254 || "SPOIL".equalsIgnoreCase(sk.skillType())) {
			handleSpoil(npc, sk);
		}

		// Debuffs de controle (Stun/Sleep/Paralyze/Root/Silence/Fear), DoTs e debuffs de stats nos monstros
		if (!npc.isDead()) {
			boolean damageSkill = hit != null;
			for (var e : sk.effects()) {
				String name = e.name().toLowerCase(java.util.Locale.ROOT);
				boolean control = name.equals("stun") || name.equals("sleep") || name.equals("paralyze")
						|| name.equals("root") || name.equals("petrification") || name.equals("medusa")
						|| name.equals("silence") || name.equals("mute") || name.equals("fear")
						|| name.equals("physicalmute") || name.equals("silencemagicphysical");
				if (control) {
					double base = damageSkill ? 50 : (sk.power() > 0 ? sk.power() : 50);
					if (!combat.debuffLands(base, sk.magicLevel(), active.level(), npc, sps, bss)) {
						send(SystemMessage.of(SystemMessage.S1_WAS_UNAFFECTED_BY_S2,
								new SystemMessage.NpcName(npc.npcId()),
								new SystemMessage.SkillName(sk.id(), sk.level())));
						continue;
					}
					long until = System.currentTimeMillis() + Math.max(1000, e.durationMs());
					int abnormalMask = 0;
					switch (name) {
						case "root" -> {
							npc.root(until);
							abnormalMask = 0x0040;
						}
						case "sleep" -> {
							npc.disable(until, true);
							npc.abortAttack();
							npc.abortCast();
							abnormalMask = 0x0080;
						}
						case "stun" -> {
							npc.disable(until, false);
							npc.abortAttack();
							npc.abortCast();
							abnormalMask = 0x0400;
						}
						case "paralyze" -> {
							npc.disable(until, false);
							npc.abortAttack();
							npc.abortCast();
							abnormalMask = 0x0010;
						}
						case "petrification", "medusa" -> {
							npc.disable(until, false);
							npc.invul(true);
							npc.abortAttack();
							npc.abortCast();
							abnormalMask = 0x0800;
							autoAttackScheduler.schedule(() -> npc.invul(false), Math.max(1000, e.durationMs()), TimeUnit.MILLISECONDS);
						}
						case "silence", "mute" -> {
							npc.mute(until);
							npc.abortCast();
							abnormalMask = 0x0020;
						}
						case "physicalmute" -> {
							npc.physicalMute(until);
							abnormalMask = 0x0020;
						}
						case "silencemagicphysical" -> {
							npc.mute(until);
							npc.physicalMute(until);
							npc.abortCast();
							abnormalMask = 0x0020;
						}
						case "fear" -> {
							npc.disable(until, false);
							npc.abortAttack();
							npc.abortCast();
							abnormalMask = 0x0004;
						}
					}
					if (abnormalMask != 0) {
						npc.startAbnormalEffect(abnormalMask);
						ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, new NpcInfo(npc), false);
						final int fMask = abnormalMask;
						autoAttackScheduler.schedule(() -> {
							if (npc != null) {
								npc.stopAbnormalEffect(fMask);
								ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, new NpcInfo(npc), false);
							}
						}, Math.max(1000, e.durationMs()), TimeUnit.MILLISECONDS);
					}
				} else if (name.equals("targetme") || "AGGDEBUFF".equalsIgnoreCase(sk.skillType()) || "AGGDAMAGE".equalsIgnoreCase(sk.skillType())) {
					npc.targetPlayerId(active.objectId());
					if (ctx.npcAi() != null) {
						ctx.npcAi().startCombat(npc, active.objectId());
					}
				} else if (name.equals("removetarget") || "AGGREMOVE".equalsIgnoreCase(sk.skillType()) || "AGGREDUCE".equalsIgnoreCase(sk.skillType()) || "SWITCH".equalsIgnoreCase(sk.skillType())) {
					npc.targetPlayerId(0);
					npc.abortAttack();
					npc.abortCast();
					if (sk.id() == 358) { // Bluff
						npc.heading(active.heading());
						ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS,
								new ValidateLocation(npc.objectId(), npc.x(), npc.y(), npc.z(), npc.heading()), false);
					}
				} else if (name.equals("damovertime") || name.equals("manadamovertime") || name.equals("poison")
						|| name.equals("bleed")) {
					if (combat.debuffLands(sk.power() > 0 ? sk.power() : 50, sk.magicLevel(), active.level(), npc, sps,
							bss)) {
						startNpcDot(npc, sk, e, t);
					}
				} else if (!e.funcs().isEmpty()) {
					if (combat.debuffLands(sk.power() > 0 ? sk.power() : 50, sk.magicLevel(), active.level(), npc, sps,
							bss)) {
						applyNpcDebuff(npc, e);
					} else {
						send(SystemMessage.of(SystemMessage.S1_WAS_UNAFFECTED_BY_S2,
								new SystemMessage.NpcName(npc.npcId()),
								new SystemMessage.SkillName(sk.id(), sk.level())));
					}
				}
			}
		}

		if (hit != null) {
			handleNpcHit(npc, hit, t, sk);
		} else if (!npc.isDead() && ctx.npcAi() != null) {
			ctx.npcAi().startCombat(npc, active.objectId()); // debuff puro tambem gera aggro
		}
	}

	private void handleSpoil(NpcInstance npc, SkillTemplate sk) {
		if (npc.isDead()) {
			return;
		}
		if (npc.isSpoiled()) {
			send(SystemMessage.id(SystemMessage.ALREADY_SPOILED));
			if (ctx.npcAi() != null) {
				ctx.npcAi().startCombat(npc, active.objectId());
			}
			return;
		}

		int targetLvl = npc.template() != null ? npc.template().level() : 1;
		int skillLvl = sk.magicLevel() > 0 ? sk.magicLevel() : active.level();
		int diff = targetLvl - skillLvl;
		int baseChance = 80;
		if (diff > 0) {
			baseChance -= diff * 5;
		}
		int chance = Math.max(5, Math.min(95, baseChance));
		boolean success = ThreadLocalRandom.current().nextInt(100) < chance;
		if (success) {
			npc.spoiled(true);
			npc.spoilerPlayerId(active.objectId());
			send(SystemMessage.id(SystemMessage.SPOIL_SUCCESS));
		} else {
			send(SystemMessage.of(SystemMessage.S1_WAS_UNAFFECTED_BY_S2,
					new SystemMessage.NpcName(npc.npcId()),
					new SystemMessage.SkillName(sk.id(), sk.level())));
		}

		if (ctx.npcAi() != null) {
			ctx.npcAi().startCombat(npc, active.objectId());
		}
	}

	private void applyNpcDebuff(NpcInstance npc, SkillTemplate.EffectTemplate e) {
		for (var f : e.funcs()) {
			double val = f.value() > 0 ? f.value() : 0.77;
			long dur = Math.max(1000, e.durationMs());
			switch (f.stat()) {
				case "pDef" -> {
					double prev = npc.pDefMul();
					npc.pDefMul(Math.min(prev, val));
					autoAttackScheduler.schedule(() -> npc.pDefMul(prev), dur, TimeUnit.MILLISECONDS);
				}
				case "mDef" -> {
					double prev = npc.mDefMul();
					npc.mDefMul(Math.min(prev, val));
					autoAttackScheduler.schedule(() -> npc.mDefMul(prev), dur, TimeUnit.MILLISECONDS);
				}
				case "pAtk" -> {
					double prev = npc.pAtkMul();
					npc.pAtkMul(Math.min(prev, val));
					autoAttackScheduler.schedule(() -> npc.pAtkMul(prev), dur, TimeUnit.MILLISECONDS);
				}
				case "mAtk" -> {
					double prev = npc.mAtkMul();
					npc.mAtkMul(Math.min(prev, val));
					autoAttackScheduler.schedule(() -> npc.mAtkMul(prev), dur, TimeUnit.MILLISECONDS);
				}
				case "runSpd" -> {
					double prev = npc.runSpdMul();
					npc.runSpdMul(Math.min(prev, val));
					ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, new NpcInfo(npc), false);
					autoAttackScheduler.schedule(() -> {
						npc.runSpdMul(prev);
						ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, new NpcInfo(npc), false);
					}, dur, TimeUnit.MILLISECONDS);
				}
				case "pAtkSpd" -> {
					double prev = npc.pAtkSpdMul();
					npc.pAtkSpdMul(Math.min(prev, val));
					ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, new NpcInfo(npc), false);
					autoAttackScheduler.schedule(() -> {
						npc.pAtkSpdMul(prev);
						ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, new NpcInfo(npc), false);
					}, dur, TimeUnit.MILLISECONDS);
				}
				case "mAtkSpd" -> {
					double prev = npc.mAtkSpdMul();
					npc.mAtkSpdMul(Math.min(prev, val));
					autoAttackScheduler.schedule(() -> npc.mAtkSpdMul(prev), dur, TimeUnit.MILLISECONDS);
				}
			}
		}
	}

	private void startNpcDot(NpcInstance npc, SkillTemplate sk, SkillTemplate.EffectTemplate e, CharTemplate t) {
		int count = Math.max(1, e.count());
		long period = Math.max(1, e.period()) * 1000L;
		int damagePerTick = (int) Math.max(1, Math.round(e.val() > 0 ? e.val() : 20));
		int[] rem = { count };
		PlayerCharacter owner = active;
		String kind = e.name().toLowerCase(java.util.Locale.ROOT);
		boolean isManaDot = kind.equals("manadamovertime");
		AtomicReference<ScheduledFuture<?>> taskRef = new AtomicReference<>();
		ScheduledFuture<?> task = autoAttackScheduler.scheduleAtFixedRate(() -> {
			if (npc == null || npc.isDead() || rem[0] <= 0 || !inWorld || active != owner) {
				var f = taskRef.get();
				if (f != null) {
					f.cancel(false);
				}
				return;
			}
			rem[0]--;
			if (isManaDot) {
				npc.currentMp(Math.max(0, npc.currentMp() - damagePerTick));
				var su = StatusUpdate.mp(npc.objectId(), (int) npc.currentMp(), npc.template().maxMp());
				send(su);
				ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, su, false);
			} else {
				var hit = ctx.combat().applyDamage(npc, damagePerTick, 0);
				handleNpcHit(npc, hit, t, sk);
				if (hit.isDead()) {
					var f = taskRef.get();
					if (f != null) {
						f.cancel(false);
					}
				}
			}
		}, period, period, TimeUnit.MILLISECONDS);
		taskRef.set(task);
	}

	/** Dano + debuffs de um skill ofensivo em outro jogador (PvP). */
	private void applyOffensivePlayer(SkillTemplate sk, GameSession targetSession, CharTemplate t,
			boolean soulshot, boolean sps, boolean bss) {
		var combat = ctx.combat();
		if (combat == null || targetSession == null || targetSession.active == null || targetSession.active.isDead()) {
			return;
		}
		var targetActive = targetSession.active;
		var targetTemplate = ctx.characters().template(targetActive);
		int damage = 0;
		double effPower = sk.power();
		if ("DEATHLINK".equalsIgnoreCase(sk.skillType())) {
			double hpRatio = (double) active.currentHp() / Math.max(1, active.maxHp());
			effPower = sk.power() * Math.max(0.2, Math.pow(1.7165 - hpRatio, 2) * 0.577);
		} else if ("FATALCOUNTER".equalsIgnoreCase(sk.skillType())) {
			double hpRatio = (double) active.currentHp() / Math.max(1, active.maxHp());
			effPower = sk.power() * Math.max(0.2, 3.5 * (1.0 - hpRatio));
		}

		if (sk.isPhysicalDamage() && effPower > 0) {
			damage = combat.skillPhysicalPlayer(active, t, targetActive, targetTemplate, effPower, soulshot,
					sk.skillType().equals("BLOW"), sk.isChargedDam());
		} else if (sk.isMagicDamage() && effPower > 0) {
			damage = combat.skillMagicPlayer(active, t, targetActive, targetTemplate, effPower, sk.magicLevel(), sps, bss);
			if (damage <= 1 && targetActive.level() - (sk.magicLevel() > 0 ? Math.min(sk.magicLevel(), active.level()) : active.level()) > 9) {
				send(SystemMessage.of(SystemMessage.S1_WAS_UNAFFECTED_BY_S2,
						new SystemMessage.Text(targetActive.name()),
						new SystemMessage.SkillName(sk.id(), sk.level())));
			}
			if (sk.skillType().equals("DRAIN") && damage > 0) {
				double absorb = sk.absorbPart() > 0 ? sk.absorbPart() : 0.2;
				healHp(damage * absorb);
			}
		} else if (sk.isManaBurn() && effPower > 0) {
			int mpDam = combat.skillManaDamPlayer(active, t, targetActive, targetTemplate, effPower, sk.magicLevel(), sps, bss);
			if (mpDam > 0) {
				double newMp = Math.max(0, targetActive.currentMp() - mpDam);
				targetActive.currentMp(newMp);
				send(SystemMessage.of(SystemMessage.YOUR_OPPONENTS_MP_WAS_REDUCED_BY_S1, new SystemMessage.Number(mpDam)));
				targetSession.send(SystemMessage.of(SystemMessage.S2_MP_HAS_BEEN_DRAINED_BY_S1, new SystemMessage.Text(active.name()),
						new SystemMessage.Number(mpDam)));
				targetSession.sendVitals();
				var su = StatusUpdate.mp(targetActive.objectId(), (int) targetActive.currentMp(), targetActive.maxMp());
				send(su);
				ctx.world().broadcastAround(targetSession, GameWorld.VISIBILITY_RADIUS, su, false);
			}
		} else if (sk.isCpDamage()) {
			int cpDam = (int) Math.round(targetActive.currentCp() * Math.max(0.0, 1.0 - sk.power()));
			if (cpDam > 0) {
				targetActive.currentCp(Math.max(0, targetActive.currentCp() - cpDam));
				send(SystemMessage.of(SystemMessage.YOU_DID_S1_DMG, new SystemMessage.Number(cpDam)));
				targetSession.send(SystemMessage.of(SystemMessage.S1_GAVE_YOU_S2_DMG, new SystemMessage.Text(active.name()),
						new SystemMessage.Number(cpDam)));
				targetSession.sendVitals();
				var su = StatusUpdate.cp(targetActive.objectId(), (int) targetActive.currentCp(), targetActive.maxCp());
				send(su);
				ctx.world().broadcastAround(targetSession, GameWorld.VISIBILITY_RADIUS, su, false);
			}
		}

		if (damage > 0) {
			var result = combat.applyDamagePlayer(targetActive, damage);
			send(SystemMessage.of(SystemMessage.YOU_DID_S1_DMG, new SystemMessage.Number(damage)));
			targetSession.send(SystemMessage.of(SystemMessage.S1_GAVE_YOU_S2_DMG, new SystemMessage.Text(active.name()),
						new SystemMessage.Number(damage)));
			targetSession.sendVitals();

			var su = StatusUpdate.hp(targetActive.objectId(), (int) targetActive.currentHp(), targetActive.maxHp());
			send(su);
			ctx.world().broadcastAround(targetSession, GameWorld.VISIBILITY_RADIUS, su, false);

			if (result.isDead()) {
				targetSession.handlePlayerDeath(active);
				return;
			}
		}

		// Debuffs & DoTs em jogadores
		if (!targetActive.isDead()) {
			if ("CANCEL".equalsIgnoreCase(sk.skillType()) || sk.id() == 1056) {
				var cancelRestore = com.lopez.l2j.game.service.CancelRestoreService.getInstance();
				if (cancelRestore != null) {
					cancelRestore.applyCancelWithRestore(targetSession, 5);
				} else {
					var activeBuffs = new java.util.ArrayList<>(targetActive.effects().active());
					if (!activeBuffs.isEmpty()) {
						java.util.Collections.shuffle(activeBuffs);
						int count = Math.min(5, activeBuffs.size());
						for (int i = 0; i < count; i++) {
							targetActive.effects().remove(activeBuffs.get(i));
						}
						targetSession.sendMagicEffectIcons();
					}
				}
			}
			if ("WARRIOR_BANE".equalsIgnoreCase(sk.skillType()) || sk.id() == 1344 || sk.id() == 1350) {
				var activeBuffs = new ArrayList<>(targetActive.effects().active());
				boolean removed = false;
				for (var b : activeBuffs) {
					boolean isWarriorBuff = b.skillId() == 1086 || b.skillId() == 1204 || b.skillId() == 264 || b.skillId() == 304;
					if (!isWarriorBuff && b.stackType() != null) {
						String st = b.stackType().toLowerCase(java.util.Locale.ROOT);
						isWarriorBuff = st.contains("speed_up") || st.contains("attack_time_down") || st.contains("run_spd");
					}
					if (!isWarriorBuff && b.funcs() != null) {
						isWarriorBuff = b.funcs().stream().anyMatch(f -> "pAtkSpd".equalsIgnoreCase(f.stat()) || "runSpd".equalsIgnoreCase(f.stat()));
					}
					if (isWarriorBuff) {
						targetActive.effects().remove(b);
						removed = true;
					}
				}
				if (removed) {
					targetSession.sendMagicEffectIcons();
					targetSession.send(new CreatureSay(0, CreatureSay.ALL, "Combate", "Buffs de velocidade fisica foram cancelados!"));
				}
			}
			if ("MAGE_BANE".equalsIgnoreCase(sk.skillType()) || sk.id() == 1345 || sk.id() == 1351) {
				var activeBuffs = new ArrayList<>(targetActive.effects().active());
				boolean removed = false;
				for (var b : activeBuffs) {
					boolean isMageBuff = b.skillId() == 1085 || b.skillId() == 1059;
					if (!isMageBuff && b.stackType() != null) {
						String st = b.stackType().toLowerCase(java.util.Locale.ROOT);
						isMageBuff = st.contains("casting_time_down") || st.contains("ma_up") || st.contains("m_atk");
					}
					if (!isMageBuff && b.funcs() != null) {
						isMageBuff = b.funcs().stream().anyMatch(f -> "mAtkSpd".equalsIgnoreCase(f.stat()) || "mAtk".equalsIgnoreCase(f.stat()));
					}
					if (isMageBuff) {
						targetActive.effects().remove(b);
						removed = true;
					}
				}
				if (removed) {
					targetSession.sendMagicEffectIcons();
					targetSession.send(new CreatureSay(0, CreatureSay.ALL, "Combate", "Buffs de poder magico e conjuracao foram cancelados!"));
				}
			}

			for (var e : sk.effects()) {
				String name = e.name().toLowerCase(java.util.Locale.ROOT);
				boolean control = name.equals("stun") || name.equals("sleep") || name.equals("paralyze")
						|| name.equals("root") || name.equals("petrification") || name.equals("medusa")
						|| name.equals("silence") || name.equals("mute") || name.equals("fear")
						|| name.equals("physicalmute") || name.equals("silencemagicphysical");

				if (control) {
					if (!combat.debuffLandsPlayer(sk.power() > 0 ? sk.power() : 50, sk.magicLevel(), active.level(),
							targetActive, sps, bss)) {
						send(SystemMessage.of(SystemMessage.S1_WAS_UNAFFECTED_BY_S2,
								new SystemMessage.Text(targetActive.name()),
								new SystemMessage.SkillName(sk.id(), sk.level())));
						continue;
					}
					long until = System.currentTimeMillis() + Math.max(1000, e.durationMs());
					targetSession.applyControlEffect(name, until);
					targetSession.send(SystemMessage.of(SystemMessage.YOU_FEEL_S1_EFFECT,
							new SystemMessage.SkillName(sk.id(), sk.level())));
				} else if (name.equals("targetme") || "AGGDEBUFF".equalsIgnoreCase(sk.skillType()) || "AGGDAMAGE".equalsIgnoreCase(sk.skillType())) {
					targetSession.forceTarget(active.objectId());
					if (targetSession.autoAttacking) {
						targetSession.schedulePlayerAutoAttack(this);
					}
				} else if (name.equals("removetarget") || "AGGREMOVE".equalsIgnoreCase(sk.skillType()) || "AGGREDUCE".equalsIgnoreCase(sk.skillType()) || "SWITCH".equalsIgnoreCase(sk.skillType())) {
					targetSession.clearTarget();
					targetSession.stopAutoAttack();
					targetSession.cancelCast();
					if (sk.id() == 358) { // Bluff
						targetActive.heading(active.heading());
						var valLoc = new ValidateLocation(targetActive.objectId(), targetActive.x(), targetActive.y(), targetActive.z(), active.heading());
						ctx.world().broadcastAround(targetSession, GameWorld.VISIBILITY_RADIUS, valLoc, true);
					}
				} else if (name.equals("damovertime") || name.equals("manadamovertime") || name.equals("poison")
						|| name.equals("bleed")) {
					if (combat.debuffLandsPlayer(sk.power() > 0 ? sk.power() : 50, sk.magicLevel(), active.level(),
							targetActive, sps, bss)) {
						targetSession.startSkillDot(sk, e);
					} else {
						send(SystemMessage.of(SystemMessage.S1_WAS_UNAFFECTED_BY_S2,
								new SystemMessage.Text(targetActive.name()),
								new SystemMessage.SkillName(sk.id(), sk.level())));
					}
				} else if (!e.funcs().isEmpty()) {
					if (combat.debuffLandsPlayer(sk.power() > 0 ? sk.power() : 50, sk.magicLevel(), active.level(),
							targetActive, sps, bss)) {
						targetSession.applySkillEffects(sk, false);
					} else {
						send(SystemMessage.of(SystemMessage.S1_WAS_UNAFFECTED_BY_S2,
								new SystemMessage.Text(targetActive.name()),
								new SystemMessage.SkillName(sk.id(), sk.level())));
					}
				}
			}
		}
	}

	public void applyControlEffect(String effectName, long until) {
		if (active == null || active.isDead()) {
			return;
		}
		int abnormalMask = 0;
		switch (effectName) {
			case "root" -> {
				active.root(until);
				abnormalMask = 0x0040;
			}
			case "sleep" -> {
				active.disable(until, true);
				stopAutoAttack();
				cancelCast();
				abnormalMask = 0x0080;
			}
			case "stun" -> {
				active.disable(until, false);
				stopAutoAttack();
				cancelCast();
				abnormalMask = 0x0400;
			}
			case "paralyze" -> {
				active.disable(until, false);
				stopAutoAttack();
				cancelCast();
				abnormalMask = 0x0010;
			}
			case "petrification", "medusa" -> {
				active.disable(until, false);
				active.invul(true);
				stopAutoAttack();
				cancelCast();
				abnormalMask = 0x0800;
				long dur = Math.max(1000, until - System.currentTimeMillis());
				autoAttackScheduler.schedule(() -> {
					if (active != null) {
						active.invul(false);
					}
				}, dur, TimeUnit.MILLISECONDS);
			}
			case "mute", "silence" -> {
				active.mute(until);
				cancelCast();
				abnormalMask = 0x0020;
			}
			case "physicalmute" -> {
				active.physicalMute(until);
				cancelCast();
				abnormalMask = 0x0020;
			}
			case "silencemagicphysical" -> {
				active.mute(until);
				active.physicalMute(until);
				cancelCast();
				abnormalMask = 0x0020;
			}
			case "fear" -> {
				active.disable(until, false);
				stopAutoAttack();
				cancelCast();
				abnormalMask = 0x0004;
			}
		}
		if (abnormalMask != 0) {
			active.startAbnormalEffect(abnormalMask);
			broadcastAppearance();
			int finalMask = abnormalMask;
			autoAttackScheduler.schedule(() -> {
				if (active != null) {
					active.stopAbnormalEffect(finalMask);
					broadcastAppearance();
				}
			}, Math.max(100, until - System.currentTimeMillis()), TimeUnit.MILLISECONDS);
		}
	}

	public void handlePlayerDeath() {
		handlePlayerDeath(null);
	}

	@Override
	public void onDeath() {
		handlePlayerDeath(null);
	}

	@Override
	public void onDeath(int killerObjectId) {
		PlayerCharacter killer = null;
		if (killerObjectId != 0 && ctx != null && ctx.world() != null) {
			var opt = ctx.world().player(killerObjectId);
			if (opt.isPresent()) {
				killer = opt.get().character();
			}
		}
		handlePlayerDeath(killer);
	}

	public void handlePlayerDeath(PlayerCharacter killer) {
		if (active == null) {
			return;
		}
		stopAutoAttack();
		cancelCast();
		clearCharges();
		clearTarget();
		if (ctx != null && ctx.npcAi() != null) {
			ctx.npcAi().stopCombatForPlayer(active.objectId());
		}
		clearHotTasks();
		active.stopAllAbnormalEffects();
		broadcastAppearance();
		active.invul(false);
		active.currentHp(0);
		active.currentCp(0);

		// Limpa buffs ao morrer (regra retail Interlude):
		// Se LeaveBuffsOnDie = False, preserva os buffs.
		boolean preserveBuffs = !Config.LEAVE_BUFFS_ON_DIE;
		boolean hasNoblesse = active.effects().hasSkill(1323);
		if (preserveBuffs) {
			saveBuffs();
		} else if (hasNoblesse) {
			active.effects().removeSkill(1323);
			saveBuffs();
		} else {
			active.effects().clear();
			if (ctx != null && ctx.buffRepository() != null) {
				ctx.buffRepository().deleteBuffs(active.objectId());
			}
		}
		long now = System.currentTimeMillis();
		send(new MagicEffectIcons((preserveBuffs || hasNoblesse)
				? active.effects().active().stream()
						.map(b -> new MagicEffectIcons.Icon(b.skillId(), b.level(), b.remainingSeconds(now)))
						.toList()
				: List.of()));
		var tTemplate = ctx.characters().template(active);
		if (ctx.skillService() != null) {
			recalcMaxVitals(tTemplate);
		}

		if (active.level() >= 10 && !active.skills().containsKey(SkillService.SKILL_LUCKY)) {
			long expForCurLevel = com.lopez.l2j.game.model.ExperienceTable.expForLevel(active.level());
			long expForNextLevel = com.lopez.l2j.game.model.ExperienceTable.expForLevel(active.level() + 1);
			long expDiff = Math.max(1, expForNextLevel - expForCurLevel);
			long lostExp = (long) (expDiff * 0.04);
			if (active.exp() - lostExp < expForCurLevel && !Config.DELEVEL) {
				active.exp(expForCurLevel);
			} else {
				active.exp(Math.max(0, active.exp() - lostExp));
				int newLvl = com.lopez.l2j.game.model.ExperienceTable.calculateLevel(active.exp());
				if (newLvl != active.level()) {
					active.level(newLvl);
					var t = ctx.characters().template(active);
					rewardSkills(t, false);
				}
			}
			send(new UserInfo(active, ctx.characters().template(active)));
		}

		send(new StatusUpdate(active.objectId(), List.of(
				new StatusUpdate.Attribute(StatusUpdate.CUR_HP, 0),
				new StatusUpdate.Attribute(StatusUpdate.CUR_CP, 0),
				new StatusUpdate.Attribute(StatusUpdate.EXP, (int) active.exp()),
				new StatusUpdate.Attribute(StatusUpdate.SP, active.sp()))));

		boolean hasClanHall = false;
		boolean hasCastle = false;
		if (ctx.clans() != null && active.clanId() > 0) {
			var clan = ctx.clans().byClanId(active.clanId()).orElse(null);
			if (clan != null) {
				hasClanHall = clan.clanHallId() > 0;
				hasCastle = clan.castleId() > 0;
			}
		}
		var die = new Die(active.objectId(), true, hasClanHall, hasCastle);
		send(die);
		var dieObserver = new Die(active.objectId(), false);
		ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, dieObserver, false);

		if (killer != null && ctx != null && ctx.world() != null) {
			ctx.world().player(killer.objectId()).ifPresent(op -> {
				if (op instanceof GameSession killerSession && ctx.pvpRewards() != null) {
					ctx.pvpRewards().handleKill(killerSession, this);
				}
			});
		}

		ctx.characters().save(active, true);
	}

	private void rewardBarakielNoblesse(com.lopez.l2j.game.party.Party party, PlayerCharacter active) {
		if (party != null) {
			for (GameSession member : party.members()) {
				if (member != null && member.active != null) {
					double dist = Math.hypot(member.active.x() - active.x(), member.active.y() - active.y());
					if (dist <= com.lopez.l2j.game.party.Party.getPartyRange()) {
						grantNoblesseStatus(member);
					}
				}
			}
		} else {
			grantNoblesseStatus(this);
		}
	}

	private void grantNoblesseStatus(GameSession session) {
		if (session == null || session.active == null) {
			return;
		}
		PlayerCharacter c = session.active;
		if (!c.isNoble() && c.level() >= 75) {
			c.setNoble(true);
			session.send(SystemMessage.id(SystemMessage.YOU_HAVE_BECOME_A_NOBLESSE));
			session.send(new SocialAction(c.objectId(), 16));
			session.sendUserInfoAndBroadcastCharInfo();
			if (session.ctx != null && session.ctx.characters() != null) {
				session.ctx.characters().save(c, true);
			}
		}
	}

	public void cancelCast() {
		if (casting) {
			casting = false;
			castingSkill = null;
			var task = castTask;
			if (task != null) {
				task.cancel(false);
				castTask = null;
			}
			var cancel = new MagicSkillCanceld(active.objectId());
			send(cancel);
			ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, cancel, false);
			send(SystemMessage.id(SystemMessage.CASTING_INTERRUPTED));
			send(new ActionFailed());
		}
	}

	public void updatePvPFlag() {
		updatePvPFlag(false);
	}

	public void updatePvPFlag(boolean againstPvPPlayer) {
		if (active == null) {
			return;
		}
		if (ctx.zones() != null && ctx.zones().isInsidePeace(active.x(), active.y(), active.z())) {
			return;
		}
		long flagDuration = againstPvPPlayer ? Config.PVP_VS_PVP_TIME : Config.PVP_VS_NORMAL_TIME;
		if (flagDuration <= 0) {
			flagDuration = 20_000L;
		}
		boolean changed = active.pvpFlag() == 0;
		active.pvpFlag(1);
		active.pvpFlagEndTime(System.currentTimeMillis() + flagDuration);
		if (changed) {
			broadcastAppearance();
		}
		PlayerCharacter owner = active;
		autoAttackScheduler.schedule(() -> {
			if (active == owner && active.pvpFlag() == 1 && System.currentTimeMillis() >= active.pvpFlagEndTime()) {
				active.pvpFlag(0);
				broadcastAppearance();
			}
		}, flagDuration + 100L, TimeUnit.MILLISECONDS);
	}

	/** Mensagem de dano, HP do alvo, morte (EXP/drop) ou aggro. */
	private void handleNpcHit(NpcInstance npc, CombatService.HitResult hit, CharTemplate t, SkillTemplate sk) {
		if (hit.resisted() && sk != null) {
			send(SystemMessage.of(SystemMessage.S1_WAS_UNAFFECTED_BY_S2,
					new SystemMessage.NpcName(npc.npcId()),
					new SystemMessage.SkillName(sk.id(), sk.level())));
		}
		if (hit.damage() > 0) {
			if ((hit.flags() & 0x20) != 0) {
				send(SystemMessage.id(sk != null && sk.magic() ? SystemMessage.CRITICAL_HIT_MAGIC : SystemMessage.CRITICAL_HIT));
			}
			send(SystemMessage.of(SystemMessage.YOU_DID_S1_DMG, new SystemMessage.Number(hit.damage())));
		}
		var su = StatusUpdate.hp(npc.objectId(), hit.remainingHp(), hit.maxHp());
		send(su);
		ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, su, false);
		if (hit.isDead()) {
			if (ctx.npcAi() != null) {
				ctx.npcAi().stopCombat(npc);
				ctx.npcAi().scheduleDecayAndRespawn(npc);
			}
			if (com.lopez.l2j.game.boss.BossManager.getInstance() != null) {
				com.lopez.l2j.game.boss.BossManager.getInstance().onBossKilled(npc, active, party);
			}
			if (targetObjectId == npc.objectId() && autoAttacking) {
				autoAttacking = false;
				send(new AutoAttackStop(active.objectId()));
				ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, new AutoAttackStop(active.objectId()),
						false);
			}
			var die = new Die(npc.objectId(), false);
			send(die);
			ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, die, false);
			if (party != null) {
				double ratePartyXp = ctx.rates() != null ? ctx.rates().partyXp() : 1.0;
				double ratePartySp = ctx.rates() != null ? ctx.rates().partySp() : 1.0;
				party.distributeExpAndSp(hit.expReward(), hit.spReward(), active, ratePartyXp, ratePartySp);
			} else {
				applyExpAndSp(hit.expReward(), hit.spReward(), t);
			}
			if (npc.isSpoiled() && ctx.drops() != null) {
				var spoilDrops = ctx.drops().rollSpoil(npc.npcId(), active.level(),
						npc.template() != null ? npc.template().level() : 0);
				npc.spoilRewards(spoilDrops);
			}
			if (ctx.drops() != null) {
				ctx.drops().rewardMonsterDeath(active, npc, ctx.inventories(), this::send);
			}
			if (ctx.questManager() != null) {
				ctx.questManager().onNpcKill(npc, this, false);
			}
			if (ctx.raidPoints() != null && npc.template() != null && npc.template().isRaidBoss()) {
				int lvl = npc.template().level();
				int points = Math.max(1, lvl / 2 + java.util.concurrent.ThreadLocalRandom.current().nextInt(-5, 6));
				if (party != null) {
					for (GameSession member : party.members()) {
						if (member != null && member.active != null) {
							double dist = Math.hypot(member.active.x() - active.x(), member.active.y() - active.y());
							if (dist <= com.lopez.l2j.game.party.Party.getPartyRange()) {
								ctx.raidPoints().addPoints(member.active.objectId(), npc.npcId(), points);
								member.send(SystemMessage.of(SystemMessage.EARNED_S1_RAID_POINTS, new SystemMessage.Number(points)));
							}
						}
					}
				} else {
					ctx.raidPoints().addPoints(active.objectId(), npc.npcId(), points);
					send(SystemMessage.of(SystemMessage.EARNED_S1_RAID_POINTS, new SystemMessage.Number(points)));
				}
			}

			if (npc.npcId() == 25325 && Config.KILL_BARAKIEL_SET_NOBLESS) {
				rewardBarakielNoblesse(party, active);
			}

			ctx.characters().save(active, true);
		} else if (!npc.isDead() && ctx.npcAi() != null) {
			ctx.npcAi().startCombat(npc, active.objectId());
		}
	}

	/**
	 * Cura/buff recebido (chamado na sessao do alvo; pode ser o proprio
	 * conjurador).
	 */
	void receivePositiveSkill(SkillTemplate sk, String casterName) {
		if (active == null) {
			return;
		}
		if (active.isDead()) {
			if ("RESURRECT".equalsIgnoreCase(sk.skillType()) || sk.target().startsWith("TARGET_CORPSE_")) {
				handleResurrect(sk, casterName);
			}
			return;
		}
		if (sk.giveCharges() > 0) {
			increaseCharges(sk.giveCharges(), sk.maxCharges());
		}
		applySkillEffects(sk, false);
		double power = sk.power();
		switch (sk.skillType()) {
			case "HEAL", "HEAL_STATIC" -> healHp(power);
			case "HEAL_PERCENT" -> healHp(active.maxHp() * power / 100.0);
			case "MANAHEAL", "MANARECHARGE" -> {
				double before = active.currentMp();
				active.currentMp(before + power);
				send(SystemMessage.of(SystemMessage.S1_MP_RESTORED,
						new SystemMessage.Number((int) (active.currentMp() - before))));
			}
			case "MANAHEAL_PERCENT" -> {
				double before = active.currentMp();
				active.currentMp(before + active.maxMp() * power / 100.0);
				send(SystemMessage.of(SystemMessage.S1_MP_RESTORED,
						new SystemMessage.Number((int) (active.currentMp() - before))));
			}
			case "COMBATPOINTHEAL" -> {
				double before = active.currentCp();
				active.currentCp(before + power);
				send(SystemMessage.of(SystemMessage.S1_CP_WILL_BE_RESTORED,
						new SystemMessage.Number((int) (active.currentCp() - before))));
			}
			case "COMBATPOINTPERCENTHEAL" -> {
				double before = active.currentCp();
				active.currentCp(before + active.maxCp() * power / 100.0);
				send(SystemMessage.of(SystemMessage.S1_CP_WILL_BE_RESTORED,
						new SystemMessage.Number((int) (active.currentCp() - before))));
			}
			case "NEGATE" -> {
				handleNegateDebuffs(sk);
				if (power > 0) {
					healHp(power);
				}
			}
			default -> {
				if (sk.id() == 1409 || sk.id() == 1018 || sk.id() == 1020 || sk.id() == 1012 || sk.id() == 34) {
					handleNegateDebuffs(sk);
					if (power > 0) {
						healHp(power);
					}
				}
			}
		}
		sendVitals();
	}

	private void handleBalanceLife(SkillTemplate sk, List<GameSession> targets) {
		if (targets == null || targets.isEmpty()) {
			return;
		}
		double totalCurHp = 0;
		double totalMaxHp = 0;
		List<GameSession> validMembers = new ArrayList<>();
		for (GameSession s : targets) {
			if (s != null && s.active != null && !s.active.isDead()) {
				totalCurHp += s.active.currentHp();
				totalMaxHp += s.active.maxHp();
				validMembers.add(s);
			}
		}
		if (totalMaxHp <= 0 || validMembers.isEmpty()) {
			return;
		}
		double ratio = Math.min(1.0, Math.max(0.01, totalCurHp / totalMaxHp));
		for (GameSession s : validMembers) {
			int newHp = (int) Math.round(s.active.maxHp() * ratio);
			newHp = Math.max(1, Math.min(s.active.maxHp(), newHp));
			s.active.currentHp(newHp);
			s.sendVitals();
			var su = StatusUpdate.hp(s.active.objectId(), (int) s.active.currentHp(), s.active.maxHp());
			s.send(su);
			ctx.world().broadcastAround(s, GameWorld.VISIBILITY_RADIUS, su, false);
		}
	}

	private void handleNegateDebuffs(SkillTemplate sk) {
		if (active == null || active.isDead()) {
			return;
		}
		int id = sk.id();
		String name = sk.name() != null ? sk.name().toLowerCase(java.util.Locale.ROOT) : "";
		if (id == 1409 || name.contains("cleanse")) {
			active.effects().clearDebuffs();
			active.clearControls();
			broadcastAppearance();
			sendMagicEffectIcons();
			send(new CreatureSay(0, CreatureSay.ALL, "Combate", "Seus efeitos negativos foram removidos!"));
		} else if (id == 1018 || name.contains("purify")) {
			active.effects().removeDebuffsByStackOrName("poison", "bleed", "paralyze");
			active.clearParalyze();
			broadcastAppearance();
			sendMagicEffectIcons();
		} else if (id == 1020 || name.contains("vitalize")) {
			active.effects().removeDebuffsByStackOrName("poison", "bleed");
			sendMagicEffectIcons();
		} else if (id == 1012 || id == 24 || name.contains("poison")) {
			active.effects().removeDebuffsByStackOrName("poison");
			sendMagicEffectIcons();
		} else if (id == 34 || name.contains("bleed")) {
			active.effects().removeDebuffsByStackOrName("bleed");
			sendMagicEffectIcons();
		} else {
			active.effects().clearDebuffs();
			sendMagicEffectIcons();
		}
	}

	private void handleResurrect(SkillTemplate sk, String casterName) {
		if (active == null || !active.isDead()) {
			return;
		}
		active.sitting(false);
		double power = sk.power();
		double hpPercent = power > 0 ? Math.min(100.0, power) : 20.0;
		int revivedHp = Math.max(1, (int) Math.round(active.maxHp() * (hpPercent / 100.0)));
		active.currentHp(revivedHp);
		active.currentCp(0.0);

		var revive = new Revive(active.objectId());
		send(revive);
		ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, revive, false);

		var t = ctx.characters().template(active);
		send(new UserInfo(active, t));
		sendVitals();
		send(SystemMessage.of(SystemMessage.S1_HP_RESTORED, new SystemMessage.Number(revivedHp)));
		ctx.characters().save(active, true);
	}

	private void healHp(double amount) {
		if (active != null && active.isOlympiadMode() && ctx != null && ctx.combat() != null && ctx.combat().olyDamageManager() != null) {
			amount *= ctx.combat().olyDamageManager().getHealMultiplier(active.classId());
		}
		double before = active.currentHp();
		active.currentHp(before + amount);
		send(SystemMessage.of(SystemMessage.S1_HP_RESTORED,
				new SystemMessage.Number((int) (active.currentHp() - before))));
	}

	/**
	 * Aplica os {@code <effect>} do skill no proprio jogador desta sessao (buffs,
	 * HoT, DoT, toggles).
	 */
	public void applySkillEffects(SkillTemplate sk, boolean toggle) {
		applySkillEffects(sk, toggle, 0);
	}

	public void applySkillEffects(SkillTemplate sk, boolean toggle, long remainingMs) {
		boolean any = false;
		PlayerCharacter owner = active;
		for (var e : sk.effects()) {
			String name = e.name();
			if (name.equalsIgnoreCase("HealOverTime") || name.equalsIgnoreCase("ManaHealOverTime")
					|| name.equalsIgnoreCase("CombatPointHealOverTime")) {
				startSkillHot(sk, e);
				any = true;
				continue;
			}
			if (name.equalsIgnoreCase("DamOverTime") || name.equalsIgnoreCase("ManaDamOverTime")) {
				startSkillDot(sk, e);
				any = true;
				continue;
			}
			boolean isInvincible = name.equalsIgnoreCase("Invincible");
			boolean isBuffEffect = name.equalsIgnoreCase("Buff") || sk.isBuff() || isInvincible;
			if (e.funcs().isEmpty() && !isBuffEffect) {
				continue; // efeito sem stats (Stun, Fear, etc.) ainda nao tem motor no jogador
			}
			String stack = e.stackType() == null || e.stackType().equalsIgnoreCase("none")
					? "skill_" + sk.id() + "_" + name
					: e.stackType();
			boolean isDebuff = sk.isDebuff() || sk.isOffensive();
			long duration = remainingMs > 0 ? remainingMs : e.durationMs();
			if (!isDebuff && remainingMs <= 0 && Config.ENABLE_MODIFY_SKILL_DURATION && Config.SKILL_DURATION_LIST != null
					&& Config.SKILL_DURATION_LIST.containsKey(sk.id())) {
				duration = Config.SKILL_DURATION_LIST.get(sk.id()) * 1000L;
			}
			long end = toggle ? com.lopez.l2j.game.effect.PlayerEffects.PERMANENT
					: System.currentTimeMillis() + duration;
			var buff = ActiveBuff.ofSkill(sk.id(), sk.level(), stack, end, e.funcs(), isDebuff);
			owner.effects().put(buff);
			if (isInvincible) {
				owner.invul(true);
			}
			any = true;
			if (!toggle) {
				autoAttackScheduler.schedule(() -> {
					if (owner.effects().remove(buff) && active == owner && inWorld) {
						if (isInvincible) {
							boolean stillInvul = false;
							for (var b : owner.effects().active()) {
								if (b.stackType() != null && b.stackType().equalsIgnoreCase(stack)) {
									stillInvul = true;
									break;
								}
							}
							if (!stillInvul) {
								owner.invul(false);
							}
						}
						send(SystemMessage.of(SystemMessage.EFFECT_S1_DISAPPEARED,
								new SystemMessage.SkillName(sk.id(), sk.level())));
						refreshBuffs();
						saveBuffs();
					}
				}, duration, TimeUnit.MILLISECONDS);
			}
		}
		if (any) {
			send(SystemMessage.of(SystemMessage.YOU_FEEL_S1_EFFECT, new SystemMessage.SkillName(sk.id(), sk.level())));
			refreshBuffs();
			saveBuffs();
		}
	}

	public void startSkillDot(SkillTemplate sk, SkillTemplate.EffectTemplate e) {
		String kind = e.name().toLowerCase(java.util.Locale.ROOT);
		String stackType = e.stackType() != null ? e.stackType().toLowerCase(java.util.Locale.ROOT) : "";
		String stack = "skilldot_" + (!stackType.isEmpty() && !stackType.equalsIgnoreCase("none") ? stackType
				: sk.id() + "_" + kind);
		var previous = hotTasks.remove(stack);
		if (previous != null) {
			previous.cancel(false);
		}
		int abnormalMask = 0;
		if (kind.contains("poison") || stackType.contains("poison") || sk.skillType().equalsIgnoreCase("POISON")) {
			abnormalMask = 0x0001;
		} else if (kind.contains("bleed") || stackType.contains("bleed") || sk.skillType().equalsIgnoreCase("BLEED")) {
			abnormalMask = 0x0002;
		}
		if (abnormalMask != 0) {
			active.startAbnormalEffect(abnormalMask);
			broadcastAppearance();
		}
		final int dotMask = abnormalMask;
		send(SystemMessage.of(SystemMessage.YOU_FEEL_S1_EFFECT, new SystemMessage.SkillName(sk.id(), sk.level())));
		refreshBuffs();

		PlayerCharacter owner = active;
		int[] remaining = { Math.max(1, e.count()) };
		long period = Math.max(1, e.period()) * 1000L;
		AtomicReference<ScheduledFuture<?>> self = new AtomicReference<>();
		ScheduledFuture<?> task = autoAttackScheduler.scheduleAtFixedRate(() -> {
			if (active != owner || !inWorld || owner.isDead() || remaining[0] <= 0
					|| (ctx != null && ctx.zones() != null && ctx.zones().isInsidePeace(owner.x(), owner.y(), owner.z()))) {
				if (dotMask != 0 && active != null) {
					active.stopAbnormalEffect(dotMask);
					broadcastAppearance();
				}
				stopHot(stack, self.get());
				return;
			}
			remaining[0]--;
			if (kind.equals("manadamovertime")) {
				owner.currentMp(Math.max(0, owner.currentMp() - e.val()));
				sendVitals();
			} else {
				double curHp = owner.currentHp();
				double newHp = Math.max(1, curHp - e.val());
				owner.currentHp(newHp);
				sendVitals();
			}
			if (remaining[0] <= 0 && dotMask != 0 && active != null) {
				active.stopAbnormalEffect(dotMask);
				broadcastAppearance();
			}
		}, period, period, TimeUnit.MILLISECONDS);
		self.set(task);
		hotTasks.put(stack, task);
	}

	/**
	 * HealOverTime/ManaHealOverTime do skill: {@code val} por tick a cada
	 * {@code period} s, {@code count} vezes.
	 */
	private void startSkillHot(SkillTemplate sk, SkillTemplate.EffectTemplate e) {
		String kind = e.name().toLowerCase(java.util.Locale.ROOT);
		String stack = "skillhot_" + kind;
		var previous = hotTasks.remove(stack);
		if (previous != null) {
			previous.cancel(false);
		}
		PlayerCharacter owner = active;
		int[] remaining = { Math.max(1, e.count()) };
		long period = Math.max(1, e.period()) * 1000L;
		AtomicReference<ScheduledFuture<?>> self = new AtomicReference<>();
		ScheduledFuture<?> task = autoAttackScheduler.scheduleAtFixedRate(() -> {
			if (active != owner || !inWorld || owner.isDead() || remaining[0] <= 0) {
				stopHot(stack, self.get());
				return;
			}
			remaining[0]--;
			switch (kind) {
				case "manahealovertime" -> owner.currentMp(owner.currentMp() + e.val());
				case "combatpointhealovertime" -> owner.currentCp(owner.currentCp() + e.val());
				default -> owner.currentHp(owner.currentHp() + e.val());
			}
			sendVitals();
		}, period, period, TimeUnit.MILLISECONDS);
		self.set(task);
		hotTasks.put(stack, task);
	}

	/**
	 * Toggle (OP_TOGGLE): liga aplicando os efeitos permanentes, desliga removendo.
	 */
	public void toggleSkill(SkillTemplate sk) {
		if (active.effects().hasSkill(sk.id())) {
			active.effects().removeSkill(sk.id());
			if (sk.id() == 7029) {
				active.gmSpeed(0);
			}
			send(SystemMessage.of(SystemMessage.EFFECT_S1_DISAPPEARED,
					new SystemMessage.SkillName(sk.id(), sk.level())));
			refreshBuffs();
			saveBuffs();
			return;
		}
		if (active.currentMp() < sk.mpConsume() + sk.mpInitialConsume()) {
			send(SystemMessage.id(SystemMessage.NOT_ENOUGH_MP));
			return;
		}
		active.currentMp(active.currentMp() - sk.mpConsume() - sk.mpInitialConsume());
		var msu = new MagicSkillUse(active.objectId(), active.objectId(), sk.id(), sk.level(), 0, 0,
				active.x(), active.y(), active.z(), active.x(), active.y(), active.z());
		send(msu);
		ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, msu, false);
		applySkillEffects(sk, true);
		if (sk.id() == 7029) {
			active.gmSpeed(sk.level());
		}
		sendVitals();
	}

	/**
	 * Max HP/MP/CP = formula da classe no nivel + passivas/buffs (maxHp, maxMp,
	 * maxCp).
	 */
	public void recalcMaxVitals(CharTemplate t) {
		if (t == null && ctx.characters() != null && active != null) {
			t = ctx.characters().template(active);
		}
		if (t == null || active == null) {
			return;
		}
		int lvl = active.level();
		int mpBonus = (active.inventory() != null)
				? active.inventory().equipped().stream().mapToInt(i -> i.template().mpBonus()).sum()
				: 0;
		int effectiveCon = Math.max(1, t.con() + active.hennaCON() + active.augCON());
		int effectiveMen = Math.max(1, t.men() + active.hennaMEN() + active.augMEN());
		double conRatio = com.lopez.l2j.game.template.BaseStatsTable.conBonus(effectiveCon) / com.lopez.l2j.game.template.BaseStatsTable.conBonus(t.con());
		double menRatio = com.lopez.l2j.game.template.BaseStatsTable.menBonus(effectiveMen) / com.lopez.l2j.game.template.BaseStatsTable.menBonus(t.men());
		active.maxHp(Math.max(1, (int) PlayerStats.applyStat(active, "maxHp", (int) Math.round(t.calculateMaxHp(lvl) * conRatio))));
		active.maxMp(Math.max(1, (int) PlayerStats.applyStat(active, "maxMp", (int) Math.round((t.calculateMaxMp(lvl) + mpBonus) * menRatio))));
		int baseCp = (int) Math.round(t.calculateMaxCp(lvl) * conRatio);
		if (ctx.sevenSigns() != null && ctx.sevenSigns().isSealValidationPeriod()) {
			int strifeOwner = ctx.sevenSigns().strifeOwner();
			if (strifeOwner != com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL) {
				int playerCabal = ctx.sevenSigns().getPlayerCabal(active.objectId());
				if (playerCabal == strifeOwner) {
					baseCp = (int) Math.round(baseCp * 1.10);
				} else if (playerCabal != com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL) {
					baseCp = (int) Math.round(baseCp * 0.90);
				}
			}
		}
		active.maxCp(Math.max(1, (int) PlayerStats.applyStat(active, "maxCp", baseCp)));
		active.currentHp(Math.min(active.maxHp(), active.currentHp()));
		active.currentMp(Math.min(active.maxMp(), active.currentMp()));
		active.currentCp(Math.min(active.maxCp(), active.currentCp()));
	}

	public void sendSkillList() {
		if (ctx.skillService() != null) {
			send(new SkillList(ctx.skillService().skillList(active)));
		} else {
			send(new SkillList(ctx.skills() != null ? ctx.skills().findByCharId(active.objectId(), 0) : List.of()));
		}
	}

	/** Expertise/Lucky/AutoLearn depois de entrar ou subir de nivel. */
	private void rewardSkills(CharTemplate t, boolean sendList) {
		var svc = ctx.skillService();
		if (svc == null) {
			return;
		}
		boolean changed = svc.rewardSkills(active);
		recalcMaxVitals(t);
		if (changed && sendList) {
			sendSkillList();
		}
	}

	// ---- Treinador (L2NpcInstance.showSkillList +
	// RequestAquireSkillInfo/RequestAquireSkill) ----

	private void showSkillList(NpcInstance npc) {
		var svc = ctx.skillService();
		if (svc == null) {
			send(new ActionFailed());
			return;
		}
		if (!svc.trees().canTeach(npc.npcId(), active.classId())) {
			send(new NpcHtmlMessage(npc.objectId(), "<html><body>" + npc.name()
					+ ":<br>I cannot teach you. My class list does not include your class.</body></html>"));
			send(new ActionFailed());
			return;
		}
		lastTrainerObjectId = npc.objectId();
		var list = svc.available(active);
		if (list.isEmpty()) {
			int min = svc.minLevelForNewSkill(active);
			if (min > 0) {
				send(SystemMessage.of(SystemMessage.DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN,
						new SystemMessage.Number(min)));
			} else {
				send(SystemMessage.id(SystemMessage.NO_MORE_SKILLS_TO_LEARN));
			}
			send(new GameServerPacket.AcquireSkillDone());
			send(new ActionFailed());
			return;
		}
		List<GameServerPacket.AcquireSkillList.Entry> entries = new ArrayList<>();
		for (var s : list) {
			entries.add(new GameServerPacket.AcquireSkillList.Entry(s.id(), s.level(), s.level(), s.sp(), 0));
		}
		send(new GameServerPacket.AcquireSkillList(0, entries));
		send(new ActionFailed());
	}

	private void onAcquireSkillInfo(GameClientPacket.RequestAcquireSkillInfo p) {
		var svc = ctx.skillService();
		if (!inWorld || svc == null || p.skillType() != 0) {
			return;
		}
		var learn = svc.learnable(active, p.skillId(), p.level()).orElse(null);
		if (learn == null) {
			return;
		}
		List<GameServerPacket.AcquireSkillInfo.Requirement> reqs = new ArrayList<>();
		int book = svc.requiredBook(p.skillId(), p.level());
		if (book > 0) {
			reqs.add(new GameServerPacket.AcquireSkillInfo.Requirement(99, book, 1, 50));
		}
		send(new GameServerPacket.AcquireSkillInfo(p.skillId(), p.level(), learn.sp(), 0, reqs));
	}

	private void onAcquireSkill(GameClientPacket.RequestAcquireSkill p) {
		var svc = ctx.skillService();
		if (!inWorld || svc == null || p.skillType() != 0) {
			return;
		}
		var trainer = ctx.world().npc(lastTrainerObjectId).orElse(null);
		if (trainer == null
				|| (!active.isGm() && Math.hypot(active.x() - trainer.x(), active.y() - trainer.y()) > 250)) {
			return;
		}
		if (active.skillLevel(p.skillId()) >= p.level()) {
			return;
		}
		var learn = svc.learnable(active, p.skillId(), p.level()).orElse(null);
		if (learn == null) {
			log.warn("{} tentou aprender skill invalido {} nv {}", active.name(), p.skillId(), p.level());
			return;
		}
		if (active.sp() < learn.sp()) {
			send(SystemMessage.id(SystemMessage.NOT_ENOUGH_SP_TO_LEARN_SKILL));
			return;
		}
		int book = svc.requiredBook(p.skillId(), p.level());
		if (book > 0) {
			if (active.inventory().byItemId(book).isEmpty() || !consumeItem(book, 1)) {
				send(SystemMessage.id(SystemMessage.ITEM_MISSING_TO_LEARN_SKILL));
				return;
			}
			send(SystemMessage.of(SystemMessage.S1_DISAPPEARED, new SystemMessage.ItemName(book)));
		}
		active.sp(active.sp() - learn.sp());
		svc.addSkill(active, p.skillId(), p.level());
		send(SystemMessage.of(SystemMessage.LEARNED_SKILL_S1, new SystemMessage.SkillName(p.skillId(), p.level())));
		send(new StatusUpdate(active.objectId(), List.of(new StatusUpdate.Attribute(StatusUpdate.SP, active.sp()))));
		var t = ctx.characters().template(active);
		recalcMaxVitals(t);
		sendSkillList();
		send(new UserInfo(active, t));
		updateSkillShortcuts(p.skillId(), p.level());
		ctx.characters().save(active, true);
		showSkillList(trainer);
	}

	/** Atalhos do skill passam a apontar para o novo nivel. */
	private void updateSkillShortcuts(int skillId, int level) {
		if (ctx.shortcuts() == null) {
			return;
		}
		for (ShortCut sc : ctx.shortcuts().findByCharId(active.objectId(), 0)) {
			if (sc.type() == ShortCut.TYPE_SKILL && sc.id() == skillId) {
				var updated = new ShortCut(sc.slot(), sc.page(), sc.type(), sc.id(), level, sc.characterType());
				ctx.shortcuts().save(active.objectId(), 0, updated);
				send(new ShortCutRegister(updated));
			}
		}
	}

	private void showEnchantSkillList(NpcInstance npc) {
		if (active == null) {
			send(new ActionFailed());
			return;
		}
		if (active.level() < 76) {
			send(new NpcHtmlMessage(npc.objectId(), "<html><body>" + npc.name()
					+ ":<br><br>You must have completed your third class transfer and reached at least level 76 to enchant skills.</body></html>"));
			send(new ActionFailed());
			return;
		}
		var svc = ctx.skillEnchant();
		var entries = svc.getAvailableEnchantSkills(active);
		if (entries.isEmpty()) {
			send(SystemMessage.id(SystemMessage.NO_MORE_SKILLS_TO_LEARN));
			send(new ActionFailed());
			return;
		}
		send(new GameServerPacket.ExEnchantSkillList(entries));
		send(new ActionFailed());
	}

	private void onExEnchantSkillInfo(GameClientPacket.RequestExEnchantSkillInfo p) {
		if (active == null || !inWorld) {
			send(new ActionFailed());
			return;
		}
		var svc = ctx.skillEnchant();
		var infoOpt = svc.buildExEnchantSkillInfoPacket(active, p.skillId(), p.level());
		if (infoOpt.isPresent()) {
			send(infoOpt.get());
		} else {
			send(new ActionFailed());
		}
	}

	private void onExEnchantSkill(GameClientPacket.RequestExEnchantSkill p) {
		if (active == null || !inWorld) {
			send(new ActionFailed());
			return;
		}
		if (active.isDead() || active.isOlympiadMode() || autoAttacking || active.isInCombat() || active.isStoreOpen() || active.isBuffShop()) {
			send(new ActionFailed());
			return;
		}
		var svc = ctx.skillEnchant();
		var result = svc.enchantSkill(active, p.skillId(), p.level());
		switch (result.type()) {
			case SUCCESS -> {
				send(SystemMessage.of(SystemMessage.YOU_HAVE_SUCCEEDED_IN_ENCHANTING_THE_SKILL_S1,
						new SystemMessage.SkillName(result.skillId(), result.newLevel())));
				sendSkillList();
				updateSkillShortcuts(result.skillId(), result.newLevel());
				sendUserInfoAndBroadcastCharInfo();
				svc.buildExEnchantSkillInfoPacket(active, result.skillId(), result.newLevel() + 1)
						.ifPresent(this::send);
			}
			case FAIL -> {
				send(SystemMessage.id(SystemMessage.YOU_HAVE_FAILED_TO_ENCHANT_THE_SKILL));
				sendSkillList();
				updateSkillShortcuts(result.skillId(), result.newLevel());
				sendUserInfoAndBroadcastCharInfo();
			}
			case NOT_ENOUGH_SP -> {
				send(SystemMessage.id(SystemMessage.YOU_DONT_HAVE_ENOUGH_SP_TO_ENCHANT_THAT_SKILL));
			}
			case NOT_ENOUGH_EXP -> {
				send(SystemMessage.id(SystemMessage.YOU_DONT_HAVE_ENOUGH_EXP_TO_ENCHANT_THAT_SKILL));
			}
			case MISSING_ITEM -> {
				send(SystemMessage.id(SystemMessage.YOU_DONT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL));
			}
			case INVALID_CONDITION, SKILL_NOT_FOUND -> {
				send(new ActionFailed());
			}
		}
		send(new ActionFailed());
	}

	private boolean isBow(com.lopez.l2j.game.item.ItemInstance weapon) {
		return weapon != null && weapon.template() != null && "bow".equalsIgnoreCase(weapon.template().subType());
	}

	private int getArrowIdForGrade(String crystalType) {
		if (crystalType == null) {
			return 17;
		}
		return switch (crystalType.toLowerCase(java.util.Locale.ROOT)) {
			case "d" -> 1341;
			case "c" -> 1342;
			case "b" -> 1343;
			case "a" -> 1344;
			case "s" -> 1345;
			default -> 17;
		};
	}

	private boolean checkAndConsumeArrow() {
		var weapon = activeWeapon();
		if (!isBow(weapon)) {
			return true;
		}
		if (!Config.CONSUME_ARROWS) {
			return true;
		}
		int arrowId = getArrowIdForGrade(weapon.template().crystalType());
		var arrow = active.inventory().byItemId(arrowId).orElse(null);
		if (arrow == null || arrow.count() < 1) {
			send(SystemMessage.id(SystemMessage.NOT_ENOUGH_ARROWS));
			send(new ActionFailed());
			autoAttacking = false;
			send(new AutoAttackStop(active.objectId()));
			return false;
		}
		return consumeItem(arrowId, 1);
	}

	public void checkPendingNpcInteract() {
		int npcId = pendingNpcInteractObjectId;
		if (npcId == 0 || active == null) {
			return;
		}
		var npc = ctx.world().npc(npcId).orElse(null);
		if (npc == null || npc.template().isAttackable() || npc.isDead()) {
			pendingNpcInteractObjectId = 0;
			return;
		}
		double distSq = Math.pow(active.x() - npc.x(), 2) + Math.pow(active.y() - npc.y(), 2);
		if (distSq <= 250.0 * 250.0) {
			pendingNpcInteractObjectId = 0;
			int heading = (int) (Math.atan2(npc.y() - active.y(), npc.x() - active.x()) * 32768.0 / Math.PI);
			active.heading(heading);
			int npcHeading = (int) (Math.atan2(active.y() - npc.y(), active.x() - npc.x()) * 32768.0 / Math.PI);
			npc.heading(npcHeading);
			var valLoc = new ValidateLocation(npc.objectId(), npc.x(), npc.y(), npc.z(), npcHeading);
			ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, valLoc, true);
			showNpcHtml(npc, 0);
		}
	}

	private static final int[][] MAJOR_TOWNS = {
			{ -84318, 244579, -3730 }, // Talking Island
			{ 46934, 51467, -2977 }, // Elven Village
			{ 9745, 15606, -4574 }, // Dark Elven Village
			{ -44836, -112524, -235 }, // Orc Village
			{ 115113, -178212, -901 }, // Dwarven Village
			{ -80826, 149775, -3043 }, // Gludin
			{ -12678, 122776, -3116 }, // Gludio
			{ 15670, 142983, -2705 }, // Dion
			{ 83400, 147943, -3404 }, // Giran
			{ 111409, 219364, -3545 }, // Heine
			{ 82956, 53162, -1495 }, // Oren
			{ 116819, 76994, -2714 }, // Hunters Village
			{ 146331, 25762, -2018 }, // Aden
			{ 147928, -55273, -2734 }, // Goddard
			{ 43799, -47727, -798 }, // Rune
			{ 87331, -142842, -1317 } // Schuttgart
	};

	private static final Map<Integer, int[]> TOWN_SCROLL_COORDINATES = Map.ofEntries(
			Map.entry(7117, new int[] { -84318, 244579, -3730 }),
			Map.entry(7554, new int[] { -84318, 244579, -3730 }),
			Map.entry(7118, new int[] { 46934, 51467, -2977 }),
			Map.entry(7555, new int[] { 46934, 51467, -2977 }),
			Map.entry(7119, new int[] { 9745, 15606, -4574 }),
			Map.entry(7556, new int[] { 9745, 15606, -4574 }),
			Map.entry(7120, new int[] { -44836, -112524, -235 }),
			Map.entry(7557, new int[] { -44836, -112524, -235 }),
			Map.entry(7121, new int[] { 115113, -178212, -901 }),
			Map.entry(7558, new int[] { 115113, -178212, -901 }),
			Map.entry(7122, new int[] { -80826, 149775, -3043 }),
			Map.entry(7123, new int[] { -12678, 122776, -3116 }),
			Map.entry(7124, new int[] { 15670, 142983, -2705 }),
			Map.entry(7125, new int[] { 17836, 170178, -3507 }),
			Map.entry(7126, new int[] { 83400, 147943, -3404 }),
			Map.entry(7559, new int[] { 83400, 147943, -3404 }),
			Map.entry(7127, new int[] { 105918, 109759, -3207 }),
			Map.entry(7128, new int[] { 111409, 219364, -3545 }),
			Map.entry(7129, new int[] { 82956, 53162, -1495 }),
			Map.entry(7130, new int[] { 85348, 16142, -3699 }),
			Map.entry(7131, new int[] { 116819, 76994, -2714 }),
			Map.entry(7132, new int[] { 146331, 25762, -2018 }),
			Map.entry(7133, new int[] { 147928, -55273, -2734 }),
			Map.entry(7134, new int[] { 43799, -47727, -798 }),
			Map.entry(7135, new int[] { 87331, -142842, -1317 }));

	public boolean isScrollOfEscape(int itemId) {
		return itemId == 736 || itemId == 1538 || itemId == 3958 || itemId == 5858 || itemId == 5859
				|| TOWN_SCROLL_COORDINATES.containsKey(itemId);
	}

	public void useScrollOfEscape(com.lopez.l2j.game.item.ItemInstance item) {
		if (active == null || active.isDead() || casting) {
			send(new ActionFailed());
			return;
		}
		if (!Config.ALT_KARMA_PLAYER_CAN_TELEPORT && active.karma() > 0) {
			send(new ActionFailed());
			return;
		}
		int itemId = item.itemId();
		int[] dest = TOWN_SCROLL_COORDINATES.get(itemId);
		if (dest == null) {
			dest = findNearestTown(active.x(), active.y());
		}

		int hitTime = (itemId == 1538 || itemId == 3958) ? 200 : 20000;
		if (!consumeItem(itemId, 1)) {
			send(new ActionFailed());
			return;
		}
		send(SystemMessage.of(SystemMessage.USE_S1, new SystemMessage.ItemName(itemId)));
		send(new GameServerPacket.SetupGauge(GameServerPacket.SetupGauge.BLUE, hitTime));

		var stop = new StopMove(active.objectId(), active.x(), active.y(), active.z(), active.heading());
		send(stop);
		ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, stop, false);

		var msu = new MagicSkillUse(active.objectId(), active.objectId(), 2014, 1, hitTime, 0,
				active.x(), active.y(), active.z(), active.x(), active.y(), active.z());
		send(msu);
		ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, msu, false);

		casting = true;
		int[] targetLoc = dest;
		castTask = autoAttackScheduler.schedule(() -> {
			casting = false;
			if (!inWorld || active == null || active.isDead()) {
				return;
			}
			teleportToLocation(targetLoc[0], targetLoc[1], targetLoc[2]);
		}, hitTime, TimeUnit.MILLISECONDS);
	}

	private int[] findNearestTown(int px, int py) {
		if (ctx.mapRegions() != null && active != null) {
			int[] respawn = ctx.mapRegions().getRestartCoordinates(px, py, active.z(), active.race());
			if (respawn != null) {
				return respawn;
			}
		}
		int[] nearest = MAJOR_TOWNS[0];
		long minSq = Long.MAX_VALUE;
		for (int[] t : MAJOR_TOWNS) {
			long dx = (long) px - t[0];
			long dy = (long) py - t[1];
			long sq = dx * dx + dy * dy;
			if (sq < minSq) {
				minSq = sq;
				nearest = t;
			}
		}
		return nearest;
	}

	public void onJoinParty(GameClientPacket.RequestJoinParty p) {
		partyClanHandler.handleJoinParty(p);
	}

	public void onAnswerJoinParty(GameClientPacket.RequestAnswerJoinParty p) {
		partyClanHandler.handleAnswerJoinParty(p);
	}

	private void startVitalsRegenTask() {
		stopVitalsRegenTask();
		PlayerCharacter owner = active;
		regenTask = autoAttackScheduler.scheduleAtFixedRate(() -> {
			try {
				if (active != owner || !inWorld || owner == null || owner.isDead()) {
					return;
				}
				if (ctx.acpService() != null) {
					ctx.acpService().checkAndConsume(GameSession.this);
				}
				boolean updated = false;
				double maxHp = owner.maxHp();
				double maxMp = owner.maxMp();
				double maxCp = owner.maxCp();

				// Multiplicador de postura (sentado recupera muito mais rapido)
				double stanceMod = owner.sitting() ? 1.5 : (owner.running() ? 0.7 : 1.1);

				// Bonus de atributos
				var t = ctx.characters() != null ? ctx.characters().template(owner) : null;
				int con = t != null ? Math.max(1, t.con() + owner.hennaCON() + owner.augCON()) : 25;
				int men = t != null ? Math.max(1, t.men() + owner.hennaMEN() + owner.augMEN()) : 25;
				double conBonus = Math.max(0.5, con / 25.0);
				double menBonus = Math.max(0.5, men / 25.0);
				var stats = t != null ? com.lopez.l2j.game.model.PlayerStats.calculate(owner, t) : null;
				int weightPenalty = stats != null ? stats.weightPenalty() : 0;

				// 1. HP Regen: base ~ 1.5 + level/10 (bloqueado se peso >= 50%)
				if (weightPenalty < 1 && owner.currentHp() < maxHp) {
					double baseHpRegen = (1.5 + (owner.level() / 10.0)) * conBonus * stanceMod * (Config.PLAYER_HP_REGEN_MULTIPLIER / 100.0);
					double newHp = Math.min(maxHp, owner.currentHp() + baseHpRegen);
					if (newHp != owner.currentHp()) {
						owner.currentHp(newHp);
						updated = true;
					}
				}

				// 2. MP Regen: base ~ 0.9 + level/12 (bloqueado se peso >= 50%)
				if (weightPenalty < 1 && owner.currentMp() < maxMp) {
					double baseMpRegen = (0.9 + (owner.level() / 12.0)) * menBonus * stanceMod * (Config.PLAYER_MP_REGEN_MULTIPLIER / 100.0);
					double newMp = Math.min(maxMp, owner.currentMp() + baseMpRegen);
					if (newMp != owner.currentMp()) {
						owner.currentMp(newMp);
						updated = true;
					}
				}

				// 3. CP Regen: base ~ 1.0 + level/15
				if (owner.currentCp() < maxCp) {
					double baseCpRegen = (1.0 + (owner.level() / 15.0)) * conBonus * stanceMod * (Config.PLAYER_CP_REGEN_MULTIPLIER / 100.0);
					double newCp = Math.min(maxCp, owner.currentCp() + baseCpRegen);
					if (newCp != owner.currentCp()) {
						owner.currentCp(newCp);
						updated = true;
					}
				}

				if (updated) {
					sendVitals();
				}
				checkZoneEnvironment();
			} catch (Exception e) {
				log.debug("Erro no tick de regeneracao: {}", e.getMessage());
			}
		}, 3000, 3000, TimeUnit.MILLISECONDS);
	}

	private void stopVitalsRegenTask() {
		if (regenTask != null) {
			regenTask.cancel(false);
			regenTask = null;
		}
	}

	private void onUserCommand(int commandId) {
		if (!inWorld || active == null) {
			send(new ActionFailed());
			return;
		}
		switch (commandId) {
			case 0 -> { // /loc
				send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						String.format("Location: %d, %d, %d", active.x(), active.y(), active.z())));
			}
			case 52 -> { // /unstuck
				if (active.isDead() || casting) {
					send(new ActionFailed());
					return;
				}
				int hitTime = 30_000; // 30 segundos
				send(new GameServerPacket.SetupGauge(GameServerPacket.SetupGauge.BLUE, hitTime));
				var stop = new StopMove(active.objectId(), active.x(), active.y(), active.z(), active.heading());
				send(stop);
				ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, stop, false);
				var msu = new MagicSkillUse(active.objectId(), active.objectId(), 2099, 1, hitTime, 0,
						active.x(), active.y(), active.z(), active.x(), active.y(), active.z());
				send(msu);
				ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, msu, false);
				send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Transporting to the nearest village in 30 seconds..."));

				casting = true;
				castTask = autoAttackScheduler.schedule(() -> {
					casting = false;
					if (!inWorld || active == null || active.isDead()) {
						return;
					}
					int[] dest = findNearestTown(active.x(), active.y());
					teleportToLocation(dest[0], dest[1], dest[2]);
				}, hitTime, TimeUnit.MILLISECONDS);
			}
			case 77 -> { // /time
				int now = GameTime.now();
				int h = (now / 60) % 24;
				int m = now % 60;
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", String.format("Game time: %02d:%02d", h, m)));
			}
			case 109 -> { // /olympiadstat
				if (ctx.olympiad() != null && ctx.olympiad().isNoble(active.objectId())) {
					var noble = ctx.olympiad().getNoble(active.objectId()).get();
					send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							String.format(
									"Grand Olympiad Games - Pontos: %d | Partidas: %d (Vitorias: %d, Derrotas: %d, Empates: %d)",
									noble.points(), noble.competitionsDone(), noble.competitionsWon(),
									noble.competitionsLost(), noble.competitionsDrawn())));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Voce nao e um Nobless registrado nas Olimpiadas."));
				}
			}
			default -> log.debug("UserCommand {} nao tratado", commandId);
		}
	}

	public void handleSlashCommand(String cmd) {
		String lower = cmd.toLowerCase(java.util.Locale.ROOT).trim();
		if (lower.equals("/loc")) {
			onUserCommand(0);
		} else if (lower.equals("/unstuck")) {
			onUserCommand(52);
		} else if (lower.equals("/time")) {
			onUserCommand(77);
		} else if (lower.equals("/olympiadstat")) {
			onUserCommand(109);
		} else if (lower.startsWith("/target ")) {
			String name = cmd.substring(8).trim().toLowerCase(java.util.Locale.ROOT);
			if (!name.isEmpty()) {
				targetByName(name);
			}
		} else if (lower.equals("/sit") || lower.equals("/stand")) {
			onActionUse(new GameClientPacket.RequestActionUse(0, false, false));
		} else if (lower.equals("/attack")) {
			onActionUse(new GameClientPacket.RequestActionUse(2, false, false));
		} else if (lower.equals("/leave") || lower.equals("/partyleave")) {
			onLeaveParty();
		} else if (lower.equals("/offline")) {
			if (!com.lopez.l2j.config.Config.ALLOW_OFFLINE_TRADE) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Modo offline desativado pelo servidor."));
				return;
			}
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Modo offline ativado. Desconectando sessao..."));
			onLogout();
		} else {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Comando: " + cmd));
		}
	}

	private void targetByName(String query) {
		NpcInstance bestNpc = null;
		double bestNpcDistSq = Double.MAX_VALUE;
		for (var npc : ctx.world().findNpcsAround(active.x(), active.y(), GameWorld.VISIBILITY_RADIUS)) {
			if (npc.name().toLowerCase(java.util.Locale.ROOT).startsWith(query)) {
				double dx = active.x() - npc.x();
				double dy = active.y() - npc.y();
				double distSq = dx * dx + dy * dy;
				if (distSq < bestNpcDistSq) {
					bestNpcDistSq = distSq;
					bestNpc = npc;
				}
			}
		}
		if (bestNpc != null) {
			targetObjectId = bestNpc.objectId();
			int levelDiff = active.level() - bestNpc.template().level();
			send(new MyTargetSelected(bestNpc.objectId(), levelDiff));
			send(StatusUpdate.hp(bestNpc.objectId(), (int) bestNpc.currentHp(), bestNpc.template().maxHp()));
			send(new ValidateLocation(bestNpc.objectId(), bestNpc.x(), bestNpc.y(), bestNpc.z(), bestNpc.heading()));
			return;
		}

		for (var other : ctx.world().players()) {
			if (other.objectId() != active.objectId()
					&& other.name().toLowerCase(java.util.Locale.ROOT).startsWith(query)) {
				targetObjectId = other.objectId();
				send(new MyTargetSelected(other.objectId(), 0));
				if (other.character() != null) {
					send(StatusUpdate.hp(other.objectId(), (int) other.character().currentHp(),
							other.character().maxHp()));
				}
				send(new ValidateLocation(other.objectId(), other.x(), other.y(), other.z(), 0));
				return;
			}
		}
		send(new ActionFailed());
	}

	public void handleDotCommand(String cmd) {
		String trimmed = cmd.trim();
		String rawCmd = trimmed.startsWith(".") ? trimmed.substring(1) : trimmed;
		int space = rawCmd.indexOf(' ');
		String commandName = space > 0 ? rawCmd.substring(0, space).trim() : rawCmd;
		String params = space > 0 ? rawCmd.substring(space + 1).trim() : "";
		if (ctx.voicedCommands() != null && ctx.voicedCommands().hasCommand(commandName)) {
			if (ctx.voicedCommands().execute(commandName, this, params)) {
				return;
			}
		}
		String lower = cmd.toLowerCase(java.util.Locale.ROOT).trim();
		if (lower.equals(".online")) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Jogadores online: " + ctx.world().players().size()));
		} else if (lower.equals(".stats")) {
			var t = ctx.characters() != null ? ctx.characters().template(active) : null;
			var stats = PlayerStats.calculate(active, t);
			send(new CreatureSay(0, CreatureSay.ALL, "SYS",
					String.format("%s (Nv %d): P.Atk %d, M.Atk %d, P.Def %d, M.Def %d, AtkSpd %d, CastSpd %d",
							active.name(), active.level(), stats.pAtk(), stats.mAtk(), stats.pDef(), stats.mDef(),
							stats.pAtkSpd(), stats.mAtkSpd())));
		} else if (lower.equals(".menu")) {
			if (ctx.preferences() != null) {
				send(new NpcHtmlMessage(0, ctx.preferences().buildMenuHtml(active.objectId(), active.name())));
			} else {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Menu de preferencias indisponivel."));
			}
		} else if (lower.equals(".blockbuff")) {
			if (ctx.preferences() != null) {
				boolean blocked = ctx.preferences().toggleBlockBuffs(active.objectId());
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Bloqueio de buffs: " + (blocked ? "ATIVADO" : "DESATIVADO")));
			}
		} else if (lower.equals(".tvt")) {
			if (ctx.tvt() != null) {
				send(new NpcHtmlMessage(0, ctx.tvt().buildStatusHtml()));
			} else {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Evento TvT desativado."));
			}
		} else if (lower.equals(".tvtjoin")) {
			if (ctx.tvt() != null) {
				var res = ctx.tvt().register(active);
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "TvT: " + res.name()));
			}
		} else if (lower.equals(".tvtleave")) {
			if (ctx.tvt() != null) {
				boolean ok = ctx.tvt().unregister(active);
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", ok ? "Inscricao do TvT cancelada." : "Voce nao esta inscrito no TvT."));
			}
		} else if (lower.equals(".ctf")) {
			if (ctx.ctf() != null) {
				send(new NpcHtmlMessage(0, ctx.ctf().buildStatusHtml()));
			} else {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Evento CTF desativado."));
			}
		} else if (lower.equals(".ctfjoin")) {
			if (ctx.ctf() != null) {
				var res = ctx.ctf().register(active);
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "CTF: " + res.name()));
			}
		} else if (lower.equals(".ctfleave")) {
			if (ctx.ctf() != null) {
				boolean ok = ctx.ctf().unregister(active);
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", ok ? "Inscricao do CTF cancelada." : "Voce nao esta inscrito no CTF."));
			}
		} else if (lower.equals(".dm")) {
			if (ctx.dm() != null) {
				send(new NpcHtmlMessage(0, ctx.dm().buildStatusHtml()));
			} else {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Evento DM desativado."));
			}
		} else if (lower.equals(".dmjoin")) {
			if (ctx.dm() != null) {
				var res = ctx.dm().register(active);
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "DM: " + res.name()));
			}
		} else if (lower.equals(".dmleave")) {
			if (ctx.dm() != null) {
				boolean ok = ctx.dm().unregister(active);
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", ok ? "Inscricao do DM cancelada." : "Voce nao esta inscrito no DM."));
			}
		} else if (lower.equals(".aiomenu")) {
			if (ctx.aio() != null && active.isAio()) {
				send(new NpcHtmlMessage(0, ctx.aio().buildAioMenuHtml(active)));
			} else {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Apenas personagens com status AIOx podem acessar este menu."));
			}
		} else if (lower.equals(".getaiogoods")) {
			if (ctx.aio() != null && active.isAio()) {
				for (var item : ctx.aio().getAioGoods()) {
					ctx.inventories().addItem(active.inventory(), item.itemId(), item.count(), "AioGoods");
				}
				send(ItemList.of(active.inventory().items(), false));
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Consumiveis de AIOx entregues no inventario."));
			} else {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Apenas personagens com status AIOx podem receber consumiveis."));
			}
		} else if (lower.equals(".classmaster") || lower.equals(".class")) {
			showClassMasterMenu(0, 0);
		} else if (lower.equals(".offline")) {
			if (!com.lopez.l2j.config.Config.ALLOW_OFFLINE_TRADE) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Modo offline desativado pelo servidor."));
				return;
			}
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Modo offline ativado. Desconectando sessao..."));
			onLogout();
		} else if (lower.equals(".deposit")) {
			if (!com.lopez.l2j.config.Config.BANKING_SYSTEM_ENABLED) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sistema bancario desativado."));
				return;
			}
			int reqAdena = com.lopez.l2j.config.Config.BANKING_SYSTEM_ADENA;
			var adenaItem = active.inventory().byItemId(57).orElse(null);
			if (adenaItem == null || adenaItem.count() < reqAdena) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Voce precisa de " + reqAdena + " adena para comprar um Gold Bar."));
				return;
			}
			ctx.inventories().consumeItem(active.inventory(), 57, reqAdena, "BankingDeposit");
			ctx.inventories().addItem(active.inventory(), 3470, com.lopez.l2j.config.Config.BANKING_SYSTEM_GOLDBARS,
					"BankingDeposit");
			send(ItemList.of(active.inventory().items(), false));
			refreshWeightAndPenalties();
			send(new CreatureSay(0, CreatureSay.ALL, "SYS",
					"Deposito realizado: Gold Bar adicionado ao seu inventario."));
		} else if (lower.equals(".withdraw")) {
			if (!com.lopez.l2j.config.Config.BANKING_SYSTEM_ENABLED) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sistema bancario desativado."));
				return;
			}
			int reqBars = com.lopez.l2j.config.Config.BANKING_SYSTEM_GOLDBARS;
			var barItem = active.inventory().byItemId(3470).orElse(null);
			if (barItem == null || barItem.count() < reqBars) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Voce precisa de " + reqBars + " Gold Bar para sacar."));
				return;
			}
			ctx.inventories().consumeItem(active.inventory(), 3470, reqBars, "BankingWithdraw");
			ctx.inventories().addItem(active.inventory(), 57, com.lopez.l2j.config.Config.BANKING_SYSTEM_ADENA,
					"BankingWithdraw");
			send(ItemList.of(active.inventory().items(), false));
			refreshWeightAndPenalties();
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Saque realizado: Adena adicionada ao seu inventario."));
		} else if (lower.equals(".gotolove")) {
			if (ctx.weddings() == null) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sistema de casamento desativado."));
				return;
			}
			int partnerId = ctx.weddings().getPartnerId(active.objectId());
			if (partnerId == 0 || !ctx.weddings().isMarried(active.objectId())) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao e casado."));
				return;
			}
			var partnerOpt = ctx.world().player(partnerId);
			if (partnerOpt.isEmpty() || partnerOpt.get().character() == null) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Seu parceiro(a) nao esta online."));
				return;
			}
			var partner = partnerOpt.get().character();
			if (!ctx.weddings().canTeleportToPartner(active, partner)) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Nao e possivel teleportar para seu parceiro(a) no momento (combate/morte/karma)."));
				return;
			}
			teleportToLocation(partner.x(), partner.y(), partner.z());
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Teleportado para seu parceiro(a)!"));
		} else if (lower.equals(".divorce")) {
			if (ctx.weddings() == null) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sistema de casamento desativado."));
				return;
			}
			var coupleOpt = ctx.weddings().getCoupleForPlayer(active.objectId());
			if (coupleOpt.isEmpty()) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao possui relacionamento ativo."));
				return;
			}
			int partnerId = ctx.weddings().getPartnerId(active.objectId());
			ctx.weddings().divorce(coupleOpt.get().id());
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce esta oficialmente divorciado(a)."));
			ctx.world().player(partnerId).ifPresent(p -> {
				p.send(new CreatureSay(0, CreatureSay.ALL, "SYS", active.name() + " se divorciou de voce."));
			});
		} else if (lower.equals(".engage")) {
			if (ctx.weddings() == null) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sistema de casamento desativado."));
				return;
			}
			if (targetObjectId == 0 || targetObjectId == active.objectId()) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Selecione seu pretendente antes de usar o comando."));
				return;
			}
			var targetOpt = ctx.world().player(targetObjectId);
			if (targetOpt.isEmpty() || targetOpt.get().character() == null) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Alvo invalido para noivado."));
				return;
			}
			var targetChar = targetOpt.get().character();
			if (ctx.weddings().getCoupleForPlayer(active.objectId()).isPresent()
					|| ctx.weddings().getCoupleForPlayer(targetChar.objectId()).isPresent()) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Um de voces ja esta noivo ou casado."));
				return;
			}
			ctx.weddings().engage(active, targetChar);
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce e " + targetChar.name() + " agora estao noivos!"));
			targetOpt.get().send(
					new CreatureSay(0, CreatureSay.ALL, "SYS", active.name() + " oficializou o noivado com voce!"));
		} else if (lower.equals(".autofarm") || lower.startsWith(".autofarm ")) {
			if (ctx.autoFarm() == null) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sistema de Auto-Farm desativado."));
				return;
			}
			boolean activeState = ctx.autoFarm().toggleAutoFarm(active);
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Auto-Farm " + (activeState ? "ATIVADO" : "DESATIVADO") + "."));
		} else if (lower.equals(".offline")) {
			if (ctx.offlineTrade() == null) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sistema de loja offline desativado."));
				return;
			}
			if (active.privateStoreType() == 0 && !active.isBuffShop()) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce precisa estar com uma loja pessoal aberta para ativar o modo offline."));
				return;
			}
			List<com.lopez.l2j.game.offlinetrade.OfflineShopItem> items = new ArrayList<>();
			items.add(new com.lopez.l2j.game.offlinetrade.OfflineShopItem(57, 1, 1));
			boolean ok = ctx.offlineTrade().startOfflineTrade(
					active,
					active.privateStoreType() != 0 ? active.privateStoreType() : com.lopez.l2j.game.offlinetrade.OfflineTradeService.STORE_PRIVATE_SELL,
					false,
					active.storeTitle(),
					items,
					com.lopez.l2j.game.offlinetrade.OfflineTradeService.DEFAULT_OFFLINE_DURATION
			);
			if (ok) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Modo offline ativado com sucesso! Desconectando sessao..."));
				active.sitting(true);
				close(new ServerClose());
			} else {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nao foi possivel ativar o modo offline."));
			}
		} else if (lower.equals(".dressme")) {
			if (ctx.dressMe() == null) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sistema DressMe indisponivel."));
				return;
			}
			boolean state = ctx.dressMe().toggle(active);
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "DressMe " + (state ? "ativado" : "desativado") + "."));
			sendUserInfoAndBroadcastCharInfo();
		} else if (lower.equals(".undressme")) {
			if (ctx.dressMe() == null) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sistema DressMe indisponivel."));
				return;
			}
			ctx.dressMe().removeArmorSkin(active);
			ctx.dressMe().removeWeaponSkin(active);
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Todas as skins DressMe foram removidas."));
			sendUserInfoAndBroadcastCharInfo();
		} else if (lower.equals(".buffshop") || lower.equals(".buffstore") || lower.equals(".buff_shop") || lower.equals(".buff_store")) {
			if (ctx.buffShop() == null) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sistema Buff Shop desativado."));
				return;
			}
			var skillTable = ctx.skillService() != null ? ctx.skillService().table() : null;
			send(new NpcHtmlMessage(0, ctx.buffShop().renderSellerManageHtml(active, skillTable, 1)));
		} else if (lower.equals(".buybuff")) {
			if (ctx.buffShop() == null) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sistema Buff Shop desativado."));
				return;
			}
			if (targetObjectId == 0 || !ctx.buffShop().isBuffShop(targetObjectId)) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Selecione um vendedor de buffs ativo como alvo."));
				return;
			}
			var skillTable = ctx.skillService() != null ? ctx.skillService().table() : null;
			send(new NpcHtmlMessage(0, ctx.buffShop().renderBuyerShopHtml(active, targetObjectId, skillTable, 1)));
		} else if (lower.startsWith(".augment")) {
			if (ctx.augmentation() == null) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sistema de augmentacao desativado."));
				return;
			}
			String[] parts = lower.split("\\s+");
			var weapon = activeWeapon();
			if (parts.length > 1 && weapon != null && ctx.augmentation().isAugmentable(weapon)) {
				try {
					int stoneId = Integer.parseInt(parts[1]);
					if (AugmentationService.isLifeStone(stoneId)) {
						ctx.augmentation().applyAugmentation(weapon, stoneId);
						updateAugmentationBonus();
						send(new InventoryUpdate(List.of(ItemInfo.of(weapon, ItemInfo.MODIFIED))));
						sendUserInfoAndBroadcastCharInfo();
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Arma equipada augmentada com Life Stone " + stoneId + "!"));
						return;
					}
				} catch (NumberFormatException ignored) {}
			}
			send(ExShowVariationMakeWindow.STATIC_PACKET);
		} else if (lower.equals(".unaugment")) {
			if (ctx.augmentation() == null) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sistema de augmentacao desativado."));
				return;
			}
			var weapon = activeWeapon();
			if (weapon != null && weapon.isAugmented()) {
				ctx.augmentation().removeAugmentation(weapon);
				updateAugmentationBonus();
				send(new InventoryUpdate(List.of(ItemInfo.of(weapon, ItemInfo.MODIFIED))));
				sendUserInfoAndBroadcastCharInfo();
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Augmentacao da arma equipada removida!"));
				return;
			}
			send(ExShowVariationCancelWindow.STATIC_PACKET);
		} else if (lower.equals(".achieve") || lower.equals(".achievements")) {
			if (ctx.achievements() == null) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sistema de conquistas desativado."));
				return;
			}
			int adena = active.inventory().byItemId(57).map(ItemInstance::count).orElse(0);
			String html = ctx.achievements().generateHtml(active, adena, 0, 0, 0);
			send(new NpcHtmlMessage(0, html));
		} else if (lower.equals(".arena") || lower.equals(".duel")) {
			if (ctx.arenaDuel() == null) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Arena 1x1 desativada."));
				return;
			}
			if (ctx.arenaDuel().isRegistered(active.objectId())) {
				ctx.arenaDuel().unregister(active);
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Inscricao cancelada na Arena 1x1."));
			} else {
				boolean ok = ctx.arenaDuel().register(active);
				if (ok) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Inscrito na Arena 1x1! Aguardando oponente..."));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nao foi possivel se inscrever na Arena 1x1."));
				}
			}
		} else if (lower.equals(".event") || lower.equals(".events")) {
			if (ctx.officialEvent() == null) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Eventos oficiais desativados."));
				return;
			}
			String html = ctx.officialEvent().generateHtml(active);
			send(new NpcHtmlMessage(0, html));
		} else if (lower.equals(".roulette") || lower.startsWith(".roulette ")) {
			if (ctx.roulette() == null) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sistema de roleta desativado."));
				return;
			}
			String html = ctx.roulette().generateMainHtml(active);
			send(new NpcHtmlMessage(0, html));
		} else if (lower.equals(".reset") || lower.equals(".rebirth")) {
			if (ctx.characterReset() == null) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sistema de reset desativado."));
				return;
			}
			int adena = active.inventory().byItemId(57).map(ItemInstance::count).orElse(0);
			Map<Integer, Long> invMap = new HashMap<>();
			invMap.put(57, (long) adena);
			String html = ctx.characterReset().generateHtml(active, invMap);
			send(new NpcHtmlMessage(0, html));
		} else {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS",
					"Comandos de voz: .online, .stats, .classmaster, .offline, .deposit, .withdraw, .gotolove, .divorce, .engage, .dressme, .undressme, .buffshop, .buffstore, .buybuff, .augment, .unaugment, .autofarm, .achieve, .arena, .event, .roulette, .reset"));
		}
	}

	public void teleportToLocation(int x, int y, int z) {
		if (!inWorld || active == null) {
			return;
		}
		clearTarget();
		if (ctx != null && ctx.npcAi() != null) {
			ctx.npcAi().stopCombatForPlayer(active.objectId());
		}
		if (active.isDead()) {
			clearHotTasks();
			active.stopAllAbnormalEffects();
		}
		var delMe = new DeleteObject(active.objectId());
		ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, delMe, false);
		for (var p : ctx.world().players()) {
			if (p instanceof GameSession gs && gs != this) {
				gs.knownObjects.remove(active.objectId());
			}
		}
		knownObjects.clear();
		stopAutoAttack();
		cancelCast();
		active.sitting(false);
		teleporting = true;
		active.moveTo(x, y, z);
		ctx.characters().save(active, true);
		var tele = new TeleportToLocation(active.objectId(), x, y, z);
		send(tele);
		send(new ActionFailed());
		PlayerCharacter owner = active;
		autoAttackScheduler.schedule(() -> {
			if (active == owner && teleporting) {
				onAppearing();
			}
		}, 15000, TimeUnit.MILLISECONDS);
	}

	private PlayerCharacter getTargetPlayerOrActive() {
		if (targetObjectId != 0 && active != null && targetObjectId != active.objectId()) {
			var other = ctx.world().player(targetObjectId).orElse(null);
			if (other != null && other.character() != null) {
				return other.character();
			}
		}
		return active;
	}

	private GameWorld.OnlinePlayer getTargetOnlinePlayerOrSelf() {
		if (targetObjectId != 0 && active != null && targetObjectId != active.objectId()) {
			var other = ctx.world().player(targetObjectId).orElse(null);
			if (other != null) {
				return other;
			}
		}
		return this;
	}

	private String resolveAdminHtml(String requested) {
		if (ctx.htmls() == null || requested == null || requested.isBlank()) {
			return null;
		}
		String clean = requested.trim();
		if (clean.startsWith("admin_")) {
			clean = clean.substring(6).trim();
		}
		if (clean.isEmpty() || "admin".equalsIgnoreCase(clean) || "main".equalsIgnoreCase(clean)) {
			clean = "menus/main.htm";
		} else if ("gamemenu".equalsIgnoreCase(clean) || "game".equalsIgnoreCase(clean)) {
			clean = "menus/game.htm";
		} else if ("server".equalsIgnoreCase(clean) || "servermenu".equalsIgnoreCase(clean)) {
			clean = "menus/server.htm";
		} else if ("effects".equalsIgnoreCase(clean) || "effectsmenu".equalsIgnoreCase(clean)) {
			clean = "menus/effects.htm";
		} else if ("mod".equalsIgnoreCase(clean) || "mods".equalsIgnoreCase(clean)) {
			clean = "menus/mod.htm";
		} else if ("show_moves".equalsIgnoreCase(clean) || "teleports".equalsIgnoreCase(clean)
				|| "tele".equalsIgnoreCase(clean) || "tele_menu".equalsIgnoreCase(clean)) {
			clean = "tele/teleports.htm";
		} else if ("gmshop".equalsIgnoreCase(clean) || "adminshop".equalsIgnoreCase(clean)
				|| "shop".equalsIgnoreCase(clean)) {
			clean = "menus/submenus/adminshop_menu.htm";
		} else if ("enchant".equalsIgnoreCase(clean) || "enchant_menu".equalsIgnoreCase(clean)) {
			clean = "menus/submenus/enchant_menu.htm";
		} else if ("spawn_menu".equalsIgnoreCase(clean) || "spawnmenu".equalsIgnoreCase(clean)) {
			clean = "menus/submenus/spawn_menu.htm";
		} else if ("show_skills".equalsIgnoreCase(clean) || "skills_menu".equalsIgnoreCase(clean)
				|| "skills".equalsIgnoreCase(clean)) {
			clean = "menus/submenus/skills_menu.htm";
		} else if ("social_menu".equalsIgnoreCase(clean) || "socialmenu".equalsIgnoreCase(clean)) {
			clean = "menus/submenus/social_menu.htm";
		} else if ("abnormal_menu".equalsIgnoreCase(clean) || "abnormalmenu".equalsIgnoreCase(clean)) {
			clean = "menus/submenus/abnormal_menu.htm";
		} else if ("announce_menu".equalsIgnoreCase(clean)) {
			clean = "menus/submenus/announce_menu.htm";
		} else if ("control".equalsIgnoreCase(clean) || "control_menu".equalsIgnoreCase(clean)) {
			clean = "menus/control.htm";
		} else if ("players".equalsIgnoreCase(clean) || "players_menu".equalsIgnoreCase(clean)) {
			clean = "menus/players.htm";
		} else if ("config".equalsIgnoreCase(clean) || "configs".equalsIgnoreCase(clean)
				|| "config_menu".equalsIgnoreCase(clean)) {
			clean = "menus/config.htm";
		} else if ("events".equalsIgnoreCase(clean) || "events_menu".equalsIgnoreCase(clean)) {
			clean = "menus/events.htm";
		} else if ("charedit".equalsIgnoreCase(clean) || "charedit_menu".equalsIgnoreCase(clean)
				|| "current_player".equalsIgnoreCase(clean)) {
			clean = "menus/submenus/charedit_menu.htm";
		} else if ("charinfo".equalsIgnoreCase(clean) || "charinfo_menu".equalsIgnoreCase(clean)) {
			clean = "menus/submenus/charinfo_menu.htm";
		} else if ("charlist".equalsIgnoreCase(clean) || "charlist_menu".equalsIgnoreCase(clean)
				|| "find_character".equalsIgnoreCase(clean)) {
			clean = "menus/submenus/charlist_menu.htm";
		} else if ("gmmenu".equalsIgnoreCase(clean)) {
			clean = "menus/submenus/gmmenu.htm";
		} else if ("itemcreation".equalsIgnoreCase(clean) || "itemcreation_menu".equalsIgnoreCase(clean)
				|| "itemcreate".equalsIgnoreCase(clean)) {
			clean = "menus/submenus/itemcreation_menu.htm";
		} else if ("expsp".equalsIgnoreCase(clean) || "expsp_menu".equalsIgnoreCase(clean)) {
			clean = "menus/submenus/expsp_menu.htm";
		} else if ("charclasses".equalsIgnoreCase(clean) || "charclasses_menu".equalsIgnoreCase(clean)) {
			clean = "menus/submenus/charclasses_menu.htm";
		} else if ("cwinfo".equalsIgnoreCase(clean) || "cw_info_menu".equalsIgnoreCase(clean)) {
			clean = "menus/submenus/cwinfo.htm";
		} else if ("sounds".equalsIgnoreCase(clean) || "sound".equalsIgnoreCase(clean)
				|| "songs".equalsIgnoreCase(clean) || "song".equalsIgnoreCase(clean)) {
			clean = "songs/songs.htm";
		} else if ("rblist".equalsIgnoreCase(clean) || "raid".equalsIgnoreCase(clean)
				|| "raidboss".equalsIgnoreCase(clean)) {
			clean = "tele/raid/raid.htm";
		} else if ("altg".equalsIgnoreCase(clean) || "altg_menu".equalsIgnoreCase(clean)
				|| "gametool".equalsIgnoreCase(clean)) {
			clean = "menus/altg_menu.htm";
		}

		List<String> candidates = new ArrayList<>();
		if (clean.startsWith("admin/")) {
			candidates.add(clean);
		} else {
			candidates.add("admin/" + clean);
			if (!clean.endsWith(".htm") && !clean.endsWith(".html")) {
				candidates.add("admin/" + clean + ".htm");
				candidates.add("admin/menus/" + clean + ".htm");
				candidates.add("admin/menus/submenus/" + clean + ".htm");
				candidates.add("admin/menus/submenus/" + clean + "_menu.htm");
				candidates.add("admin/gmshop/" + clean + ".htm");
				candidates.add("admin/tele/" + clean + ".htm");
				candidates.add("admin/skills/" + clean + ".htm");
				candidates.add("admin/songs/" + clean + ".htm");
				candidates.add("admin/classes/" + clean + ".htm");
				candidates.add("admin/tele/raid/" + clean + ".htm");
			}
			candidates.add("admin/menus/" + clean);
			candidates.add("admin/menus/submenus/" + clean);
			candidates.add("admin/gmshop/" + clean);
			candidates.add("admin/tele/" + clean);
			candidates.add("admin/skills/" + clean);
			candidates.add("admin/songs/" + clean);
			candidates.add("admin/classes/" + clean);
			candidates.add("admin/tele/raid/" + clean);
		}

		for (String cand : candidates) {
			String html = ctx.htmls().getHtml(cand);
			if (html != null && !html.isBlank()) {
				return html;
			}
		}

		// Fallback inteligente usando o indice global do HtmCache
		String indexed = ctx.htmls().getIndexedHtml(clean);
		if (indexed != null && !indexed.isBlank()) {
			return indexed;
		}
		if (clean.contains("/")) {
			String lastPart = clean.substring(clean.lastIndexOf('/') + 1);
			indexed = ctx.htmls().getIndexedHtml(lastPart);
			if (indexed != null && !indexed.isBlank()) {
				return indexed;
			}
		}

		return null;
	}

	public void showAdminHtml(String requested) {
		String cleanReq = requested != null ? requested.trim().toLowerCase(java.util.Locale.ROOT) : "";
		if (cleanReq.contains("charlist")) {
			showAdminCharList("", 1);
			return;
		}
		if (cleanReq.contains("charinfo")) {
			PlayerCharacter tc = getTargetPlayerOrActive();
			showAdminCharInfo(tc != null ? tc.name() : (active != null ? active.name() : ""));
			return;
		}
		String html = resolveAdminHtml(requested);
		if (html == null) {
			html = "<html><title>Admin Panel</title><body><center><font color=\"LEVEL\">Admin Control Panel</font><br><br>"
					+ "<a action=\"bypass -h admin_admin\">Main Menu</a><br>"
					+ "<a action=\"bypass -h admin_gamemenu\">Game Menu</a><br>"
					+ "<a action=\"bypass -h admin_server\">Server Menu</a><br>"
					+ "<a action=\"bypass -h admin_effects\">Effects Menu</a><br>"
					+ "<a action=\"bypass -h admin_show_moves\">Teleports</a><br>"
					+ "<a action=\"bypass -h admin_gmshop\">GM Shop</a><br>"
					+ "<a action=\"bypass -h admin_enchant\">Enchant</a><br>"
					+ "<a action=\"bypass -h admin_spawn_menu\">Spawn Menu</a><br>"
					+ "</center></body></html>";
		}
		PlayerCharacter targetChar = getTargetPlayerOrActive();
		String rendered = ctx.htmls() != null
				? ctx.htmls().render(html, 0, "Admin", targetChar != null ? targetChar.name() : "Admin")
				: html;
		if (targetChar != null) {
			rendered = rendered
					.replace("%name%", targetChar.name())
					.replace("%currenthp%", String.valueOf((int) targetChar.currentHp()))
					.replace("%maxhp%", String.valueOf(targetChar.maxHp()))
					.replace("%currentmp%", String.valueOf((int) targetChar.currentMp()))
					.replace("%maxmp%", String.valueOf(targetChar.maxMp()))
					.replace("%currentcp%", String.valueOf((int) targetChar.currentCp()))
					.replace("%maxcp%", String.valueOf(targetChar.maxCp()))
					.replace("%class%", String.valueOf(targetChar.classId()))
					.replace("%level%", String.valueOf(targetChar.level()))
					.replace("%title%", targetChar.title() != null ? targetChar.title() : "")
					.replace("%karma%", String.valueOf(targetChar.karma()))
					.replace("%pvp%", String.valueOf(targetChar.pvpKills()))
					.replace("%pk%", String.valueOf(targetChar.pkKills()));
		}
		long totalMem = Runtime.getRuntime().totalMemory() / (1024 * 1024);
		long freeMem = Runtime.getRuntime().freeMemory() / (1024 * 1024);
		long usedMem = totalMem - freeMem;
		long maxMem = Runtime.getRuntime().maxMemory() / (1024 * 1024);
		int onlineCount = ctx.world() != null ? ctx.world().allPlayers().size() : 1;
		long uptimeSec = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
		long uptimeH = uptimeSec / 3600;
		long uptimeM = (uptimeSec % 3600) / 60;
		rendered = rendered
				.replace("%meminfo%", usedMem + "MB / " + maxMem + "MB")
				.replace("%os%", System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ")")
				.replace("%time%", uptimeH + "h " + uptimeM + "m")
				.replace("%online%", String.valueOf(onlineCount))
				.replace("%max%", String.valueOf(com.lopez.l2j.config.Config.MAXIMUM_ONLINE_USERS))
				.replace("%geo%", com.lopez.l2j.config.Config.ENABLE_GEODATA ? "Enabled" : "Disabled")
				.replace("%target%", targetChar != null ? targetChar.name() : (active != null ? active.name() : "None"));
		if (rendered.contains("%cwinfo%")) {
			StringBuilder cwSb = new StringBuilder("<table width=270 bgcolor=\"000000\">");
			if (ctx.cursedWeapons() != null) {
				for (var cw : ctx.cursedWeapons().allWeapons()) {
					String status = cw.isActive() ? "<font color=\"FF0000\">Equipada por " + (!cw.playerName().isEmpty() ? cw.playerName() : "Desconhecido") + "</font>"
							: (cw.isDropped() ? "<font color=\"FFFF00\">Dropada no Chao (" + cw.x() + ", " + cw.y() + ")</font>"
							: "<font color=\"00FF00\">Inativa / No Void</font>");
					cwSb.append("<tr><td><font color=\"LEVEL\">").append(cw.name()).append(":</font></td></tr>");
					cwSb.append("<tr><td>Status: ").append(status).append("</td></tr>");
					cwSb.append("<tr><td>");
					cwSb.append("<button value=\"Spawn CW\" action=\"bypass -h admin_cw_add ").append(cw.itemId()).append("\" width=80 height=20 back=\"L2UI_CH3.smallbutton1_over\" fore=\"L2UI_CH3.smallbutton1\"> ");
					cwSb.append("<button value=\"Remover CW\" action=\"bypass -h admin_cw_remove ").append(cw.itemId()).append("\" width=80 height=20 back=\"L2UI_CH3.smallbutton1_over\" fore=\"L2UI_CH3.smallbutton1\">");
					if (cw.isDropped() || cw.isActive()) {
						cwSb.append(" <button value=\"Ir ate Ela\" action=\"bypass -h admin_move_to ").append(cw.x()).append(" ").append(cw.y()).append(" ").append(cw.z()).append("\" width=70 height=20 back=\"L2UI_CH3.smallbutton1_over\" fore=\"L2UI_CH3.smallbutton1\">");
					}
					cwSb.append("</td></tr>");
				}
			} else {
				cwSb.append("<tr><td>CursedWeaponsManager desativado.</td></tr>");
			}
			cwSb.append("</table>");
			rendered = rendered.replace("%cwinfo%", cwSb.toString());
		}
		send(new NpcHtmlMessage(0, rendered));
	}

	private void onGMCommand(GameClientPacket.RequestGMCommand p) {
		if (active == null || !active.isGm()) {
			send(new ActionFailed());
			return;
		}
		handleGMCommand(p.targetName(), p.command());
	}

	private void handleGMCommand(String targetName, int command) {
		if (active == null || !active.isGm()) {
			return;
		}
		PlayerCharacter targetChar = null;
		if (targetName != null && !targetName.isBlank()) {
			var online = ctx.world().byName(targetName.trim()).orElse(null);
			if (online != null && online.character() != null) {
				targetChar = online.character();
			} else if (ctx.characters() != null) {
				targetChar = ctx.characters().findByName(targetName.trim()).orElse(null);
			}
		}
		if (targetChar == null) {
			targetChar = getTargetPlayerOrActive();
		}
		if (targetChar == null) {
			targetChar = active;
		}

		CharTemplate template = ctx.characters() != null ? ctx.characters().template(targetChar) : null;
		PlayerStats stats = template != null ? PlayerStats.calculate(targetChar, template) : null;

		switch (command) {
			case 1 -> {
				if (template != null && stats != null) {
					send(new GMViewCharacterInfo(targetChar, template, stats, targetChar.inventory().paperdollView(),
							targetChar.inventory().currentLoad()));
				}
			}
			case 2 -> send(new GMViewPledgeInfo(targetChar.name(), targetChar.clanId(), targetChar.level(),
					targetChar.classId()));
			case 3 -> send(new GMViewSkillInfo(targetChar.name(), targetChar.skills(), ctx.skillService()));
			case 4 -> send(new GMViewQuestInfo(targetChar.name()));
			case 5 -> {
				send(new GMViewItemList(targetChar.name(), targetChar.inventory().items(), 80));
				send(new GMHennaInfo(0, 0, 0, 0, 0, 0));
			}
			case 6 -> send(new GMViewWarehouseWithdrawList(targetChar.name(),
					(int) Math.min(Integer.MAX_VALUE, targetChar.inventory().adena()), List.of()));
			default -> {
				if (template != null && stats != null) {
					send(new GMViewCharacterInfo(targetChar, template, stats, targetChar.inventory().paperdollView(),
							targetChar.inventory().currentLoad()));
				}
			}
		}
	}

	private void showAdminCharList(String query, int page) {
		String html = resolveAdminHtml("menus/submenus/charlist_menu.htm");
		if (html == null) {
			html = "<html><title>Players</title><body><center>Players Menu</center></body></html>";
		}
		Map<Integer, PlayerCharacter> playerMap = new LinkedHashMap<>();

		// 1. Online players first
		for (var p : ctx.world().players()) {
			if (p.character() != null) {
				playerMap.put(p.character().objectId(), p.character());
			}
		}

		// 2. Add repository players
		if (ctx.characters() != null) {
			List<PlayerCharacter> repoList = (query != null && !query.isBlank())
					? ctx.characters().searchByName(query.trim(), 100)
					: ctx.characters().listAll(100);
			for (var c : repoList) {
				playerMap.putIfAbsent(c.objectId(), c);
			}
		}

		List<PlayerCharacter> allPlayers = new ArrayList<>();
		if (query != null && !query.isBlank()) {
			String q = query.trim().toLowerCase(java.util.Locale.ROOT);
			for (var c : playerMap.values()) {
				if (c.name().toLowerCase(java.util.Locale.ROOT).contains(q)) {
					allPlayers.add(c);
				}
			}
		} else {
			allPlayers.addAll(playerMap.values());
		}

		int pageSize = 15;
		int totalPlayers = allPlayers.size();
		int maxPages = Math.max(1, (int) Math.ceil((double) totalPlayers / pageSize));
		int currentPage = Math.min(Math.max(1, page), maxPages);
		int fromIndex = (currentPage - 1) * pageSize;
		int toIndex = Math.min(fromIndex + pageSize, totalPlayers);
		List<PlayerCharacter> pageList = (fromIndex < totalPlayers) ? allPlayers.subList(fromIndex, toIndex)
				: List.of();

		StringBuilder rows = new StringBuilder();
		if (pageList.isEmpty()) {
			rows.append(
					"<tr><td colspan=3><center><font color=\"LEVEL\">No characters found.</font></center></td></tr>");
		} else {
			for (PlayerCharacter pc : pageList) {
				String className = "Class " + pc.classId();
				if (ctx.characters() != null) {
					try {
						var t = ctx.characters().template(pc);
						if (t != null) {
							className = t.className();
						}
					} catch (Exception ignored) {
					}
				}
				boolean isOnline = ctx.world().byName(pc.name()).isPresent();
				String nameDisplay = isOnline ? "<font color=\"00FF00\">" + pc.name() + "</font>" : pc.name();
				rows.append("<tr>")
						.append("<td width=80><a action=\"bypass -h admin_character_info ").append(pc.name())
						.append("\">").append(nameDisplay).append("</a></td>")
						.append("<td width=110>").append(className).append("</td>")
						.append("<td width=40>").append(pc.level()).append("</td>")
						.append("</tr>");
			}
		}

		StringBuilder pages = new StringBuilder();
		if (maxPages > 1) {
			pages.append("<table width=270><tr>");
			String safeQuery = (query != null && !query.isBlank()) ? query.trim() : "";
			for (int p = 1; p <= maxPages; p++) {
				if (p == currentPage) {
					pages.append("<td><button value=\"[").append(p)
							.append("]\" action=\"bypass -h admin_show_characters ").append(safeQuery).append(" ")
							.append(p)
							.append("\" width=30 height=19 back=\"L2UI_CH3.smallbutton1_over\" fore=\"L2UI_CH3.smallbutton1\"></td>");
				} else {
					pages.append("<td><button value=\"").append(p)
							.append("\" action=\"bypass -h admin_show_characters ").append(safeQuery).append(" ")
							.append(p).append("\" width=30 height=19 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td>");
				}
			}
			pages.append("</tr></table>");
		} else {
			pages.append("<font color=\"LEVEL\">Page 1 of 1 (").append(totalPlayers).append(" players)</font>");
		}

		String rendered = html
				.replace("%players%", rows.toString())
				.replace("%pages%", pages.toString());

		send(new NpcHtmlMessage(0, rendered));
	}

	private void showAdminCastlesHtml(String args) {
		StringBuilder sb = new StringBuilder("<html><title>Castles Management</title><body><center>");
		sb.append("<table width=270><tr>");
		sb.append("<td align=center><button value=\"Main\" action=\"bypass -h admin_admin\" width=52 height=20 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>");
		sb.append("<td align=center><button value=\"ALT+G\" action=\"bypass -h admin_altg\" width=52 height=20 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>");
		sb.append("<td align=center><button value=\"Game\" action=\"bypass -h admin_gamemenu\" width=52 height=20 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>");
		sb.append("<td align=center><button value=\"Server\" action=\"bypass -h admin_server\" width=52 height=20 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>");
		sb.append("<td align=center><button value=\"Tele\" action=\"bypass -h admin_show_moves\" width=52 height=20 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>");
		sb.append("</tr></table><img src=\"L2UI.SquareGray\" width=270 height=1>");
		sb.append("<table width=270><tr><td><font color=\"00FFBB\">Gerenciamento de Castelos (Sieges)</font></td></tr></table>");
		sb.append("<table width=270 bgcolor=\"000000\">");
		sb.append("<tr><td width=90><font color=\"LEVEL\">Castelo</font></td><td width=90><font color=\"LEVEL\">Cla Dono</font></td><td width=90><font color=\"LEVEL\">Acao</font></td></tr>");

		if (ctx.castles() != null) {
			for (var c : ctx.castles().all()) {
				String ownerName = "Sem Dono";
				if (c.hasOwner() && ctx.clans() != null) {
					var cl = ctx.clans().byClanId(c.ownerClanId()).orElse(null);
					ownerName = cl != null ? cl.name() : ("Clan " + c.ownerClanId());
				}
				sb.append("<tr>")
						.append("<td width=90><font color=\"FFFF77\">").append(c.name()).append("</font></td>")
						.append("<td width=90>").append(ownerName).append("</td>")
						.append("<td width=90><button value=\"Dar Alvo\" action=\"bypass -h admin_setcastle ").append(c.name()).append("\" width=60 height=18 back=\"L2UI_CH3.smallbutton1_over\" fore=\"L2UI_CH3.smallbutton1\"></td>")
						.append("</tr>");
			}
		} else {
			sb.append("<tr><td colspan=3>CastleManager indisponivel.</td></tr>");
		}
		sb.append("</table><img src=\"L2UI.SquareGray\" width=270 height=1>");
		sb.append("<table width=270><tr>");
		sb.append("<td align=center><button value=\"Atualizar\" action=\"bypass -h admin_siege\" width=130 height=21 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>");
		sb.append("</tr></table></center></body></html>");
		send(new NpcHtmlMessage(0, sb.toString()));
	}

	public void showAdminCharInfo(String charName) {
		PlayerCharacter targetChar = null;
		GameWorld.OnlinePlayer onlineTarget = null;
		if (charName != null && !charName.isBlank()) {
			onlineTarget = ctx.world().byName(charName.trim()).orElse(null);
			if (onlineTarget != null && onlineTarget.character() != null) {
				targetChar = onlineTarget.character();
			} else if (ctx.characters() != null) {
				targetChar = ctx.characters().findByName(charName.trim()).orElse(null);
			}
		}
		if (targetChar == null) {
			targetChar = getTargetPlayerOrActive();
		}
		if (targetChar == null) {
			targetChar = active;
		}
		this.targetObjectId = targetChar.objectId();

		String html = resolveAdminHtml("menus/submenus/charinfo_menu.htm");
		if (html == null) {
			showAdminHtml("menus/submenus/charinfo_menu.htm");
			return;
		}

		CharTemplate template = ctx.characters() != null ? ctx.characters().template(targetChar) : null;
		PlayerStats stats = template != null ? PlayerStats.calculate(targetChar, template) : null;

		String ip = "Offline";
		if (onlineTarget != null) {
			ip = "Online";
		} else if (targetChar.objectId() == active.objectId()) {
			ip = "127.0.0.1";
		}

		String clanName = targetChar.clanId() != 0 ? "Clan " + targetChar.clanId() : "No Clan";
		String className = template != null ? template.className() : "Class " + targetChar.classId();

		String rendered = html
				.replace("%name%", targetChar.name())
				.replace("%account%", targetChar.account() != null ? targetChar.account() : "none")
				.replace("%ip%", ip)
				.replace("%clan%", clanName)
				.replace("%class%", className)
				.replace("%level%", String.valueOf(targetChar.level()))
				.replace("%currentcp%", String.valueOf((int) targetChar.currentCp()))
				.replace("%maxcp%", String.valueOf(targetChar.maxCp()))
				.replace("%currenthp%", String.valueOf((int) targetChar.currentHp()))
				.replace("%maxhp%", String.valueOf(targetChar.maxHp()))
				.replace("%currentmp%", String.valueOf((int) targetChar.currentMp()))
				.replace("%maxmp%", String.valueOf(targetChar.maxMp()))
				.replace("%pkkills%", String.valueOf(targetChar.pkKills()))
				.replace("%karma%", String.valueOf(targetChar.karma()))
				.replace("%pvpkills%", String.valueOf(targetChar.pvpKills()))
				.replace("%xp%", String.valueOf(targetChar.exp()))
				.replace("%sp%", String.valueOf(targetChar.sp()))
				.replace("%patk%", String.valueOf(stats != null ? stats.pAtk() : 0))
				.replace("%accuracy%", String.valueOf(stats != null ? stats.accuracy() : 0))
				.replace("%matk%", String.valueOf(stats != null ? stats.mAtk() : 0))
				.replace("%evasion%", String.valueOf(stats != null ? stats.evasion() : 0))
				.replace("%pdef%", String.valueOf(stats != null ? stats.pDef() : 0))
				.replace("%critical%", String.valueOf(stats != null ? stats.critical() : 0))
				.replace("%mdef%", String.valueOf(stats != null ? stats.mDef() : 0))
				.replace("%runspeed%", String.valueOf(stats != null ? stats.runSpeed() : 0))
				.replace("%patkspd%", String.valueOf(stats != null ? stats.pAtkSpd() : 0))
				.replace("%matkspd%", String.valueOf(stats != null ? stats.mAtkSpd() : 0));

		send(new NpcHtmlMessage(0, rendered));
	}

	public void showAdminNpcInfo(NpcInstance npc) {
		if (npc == null || active == null || !active.isGm()) {
			return;
		}
		var t = npc.template();
		int curHp = (int) npc.currentHp();
		int maxHp = t != null ? t.maxHp() : curHp;
		int curMp = (int) npc.currentMp();
		int maxMp = t != null ? t.maxMp() : curMp;
		int pAtk = t != null ? t.pAtk() : 0;
		int mAtk = t != null ? t.mAtk() : 0;
		int pDef = t != null ? t.pDef() : 0;
		int mDef = t != null ? t.mDef() : 0;
		int lvl = t != null ? t.level() : 1;
		String type = t != null ? t.type() : "L2Npc";

		String htm = "<html><title>NPC Info: " + npc.name() + "</title><body>"
				+ "<center>"
				+ "<table width=270>"
				+ "<tr><td><font color=\"LEVEL\">" + npc.name() + "</font> (ID: " + npc.npcId()
				+ ")</td><td align=right>Obj: " + npc.objectId() + "</td></tr>"
				+ "</table>"
				+ "<center><img src=\"L2UI.SquareGray\" width=270 height=1></center><br>"
				+ "<table width=270>"
				+ "<tr><td>Type: <font color=\"00FF00\">" + type + "</font></td><td>Level: <font color=\"LEVEL\">" + lvl
				+ "</font></td></tr>"
				+ "<tr><td>HP: <font color=\"FF5555\">" + curHp + " / " + maxHp
				+ "</font></td><td>MP: <font color=\"5555FF\">" + curMp + " / " + maxMp + "</font></td></tr>"
				+ "<tr><td>P.Atk: " + pAtk + " | P.Def: " + pDef + "</td><td>M.Atk: " + mAtk + " | M.Def: " + mDef
				+ "</td></tr>"
				+ "<tr><td colspan=2>Loc: " + npc.x() + ", " + npc.y() + ", " + npc.z() + " (" + npc.heading()
				+ ")</td></tr>"
				+ "</table>"
				+ "<br><center><img src=\"L2UI.SquareGray\" width=270 height=1></center><br>"
				+ "<table width=270>"
				+ "<tr>"
				+ "<td><button value=\"Kill\" action=\"bypass -h admin_kill\" width=65 height=21 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>"
				+ "<td><button value=\"Delete\" action=\"bypass -h admin_delete\" width=65 height=21 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>"
				+ "<td><button value=\"Heal\" action=\"bypass -h admin_heal\" width=65 height=21 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>"
				+ "<td><button value=\"Teleport\" action=\"bypass -h admin_move_to " + npc.x() + " " + npc.y() + " "
				+ npc.z()
				+ "\" width=65 height=21 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td colspan=2><button value=\"Talk Dialogue\" action=\"bypass -h npc_" + npc.objectId()
				+ "_Chat 0\" width=130 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\"></td>"
				+ "<td colspan=2><button value=\"View DropList\" action=\"bypass -h admin_show_droplist " + npc.npcId()
				+ "\" width=130 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\"></td>"
				+ "</tr>"
				+ "</table>"
				+ "</center></body></html>";

		send(new NpcHtmlMessage(npc.objectId(), htm));
	}

	private void showAdminDropList(int npcId) {
		if (active == null || !active.isGm()) {
			return;
		}
		List<com.lopez.l2j.game.drop.DropData> drops = (ctx.drops() != null) ? ctx.drops().getDrops(npcId) : List.of();
		String npcName = ctx.world().npcs().stream().filter(n -> n.npcId() == npcId).findFirst().map(NpcInstance::name)
				.orElse("NPC " + npcId);

		StringBuilder sb = new StringBuilder();
		sb.append("<html><title>DropList: ").append(npcName).append("</title><body><center>");
		sb.append("<font color=\"LEVEL\">").append(npcName).append("</font> (ID: ").append(npcId).append(")<br><br>");
		if (drops.isEmpty()) {
			sb.append("<font color=\"FF5555\">Nenhum drop configurado para este NPC.</font><br><br>");
		} else {
			sb.append("<table width=280>");
			sb.append("<tr><td><b>Item</b></td><td><b>Qtd</b></td><td><b>Chance</b></td><td><b>Tipo</b></td></tr>");
			for (var d : drops) {
				String itemName = "Item " + d.itemId();
				if (ctx.inventories() != null && ctx.inventories().templates() != null) {
					var tpl = ctx.inventories().templates().get(d.itemId()).orElse(null);
					if (tpl != null && tpl.name() != null) {
						itemName = tpl.name();
					}
				}
				if (itemName.length() > 18) {
					itemName = itemName.substring(0, 16) + "..";
				}
				double pct = (double) d.chance() / 10000.0;
				String typeStr = d.isSpoil() ? "<font color=\"00FF00\">Spoil</font>"
						: "<font color=\"LEVEL\">Drop</font>";
				sb.append("<tr>");
				sb.append("<td>").append(itemName).append("</td>");
				sb.append("<td>").append(d.min()).append("-").append(d.max()).append("</td>");
				sb.append("<td>").append(String.format(java.util.Locale.US, "%.2f%%", pct)).append("</td>");
				sb.append("<td>").append(typeStr).append("</td>");
				sb.append("</tr>");
			}
			sb.append("</table>");
		}
		sb.append("<br><a action=\"bypass -h admin_admin\">Main Admin Menu</a>");
		sb.append("</center></body></html>");
		send(new NpcHtmlMessage(0, sb.toString()));
	}

	private record RaidBossEntry(int id, String name, int level, int x, int y, int z, boolean alive) {}

	private void showAdminRaidBossList(String args) {
		if (active == null || !active.isGm()) {
			return;
		}
		if (args == null || args.isBlank()) {
			showAdminHtml("tele/raid/raid.htm");
			return;
		}

		String[] parts = args.trim().split("\\s+");
		String cat = parts[0].toLowerCase(java.util.Locale.ROOT);
		int page = parts.length > 1 ? parseIntSafe(parts[1], 1) : 1;
		if (page < 1) {
			page = 1;
		}

		List<RaidBossEntry> bosses = new ArrayList<>();
		String title;

		if ("grand".equals(cat)) {
			title = "Grand Bosses";
			List<RaidBossEntry> grandTemplates = List.of(
					new RaidBossEntry(29001, "Queen Ant", 40, -21610, 181594, -5734, false),
					new RaidBossEntry(29006, "Core", 50, 17726, 108915, -6480, false),
					new RaidBossEntry(29014, "Orfen", 50, 55024, 17368, -5412, false),
					new RaidBossEntry(29022, "Zaken", 60, 55312, 219168, -3223, false),
					new RaidBossEntry(29020, "Baium", 75, 116033, 17447, 10107, false),
					new RaidBossEntry(29019, "Antharas", 79, 181323, 114850, -7670, false),
					new RaidBossEntry(29062, "High Priestess van Halter", 80, -16375, -53658, 10448, false),
					new RaidBossEntry(29065, "Sailren", 80, 27333, -6835, -1970, false),
					new RaidBossEntry(29028, "Valakas", 85, 212852, -114842, -1632, false),
					new RaidBossEntry(29045, "Frintezza", 85, -87784, -155083, -9083, false));

			Map<Integer, RaidBossEntry> bossMap = new LinkedHashMap<>();
			for (RaidBossEntry t : grandTemplates) {
				bossMap.put(t.id(), t);
			}

			if (ctx.world() != null) {
				for (NpcInstance n : ctx.world().npcs()) {
					if (n.template() == null) {
						continue;
					}
					boolean isGrand = n.template().isGrandBoss() || bossMap.containsKey(n.npcId());
					if (!isGrand) {
						continue;
					}
					int x = n.x() != 0 ? n.x() : n.spawnX();
					int y = n.y() != 0 ? n.y() : n.spawnY();
					int z = n.z() != 0 ? n.z() : n.spawnZ();
					boolean alive = !n.isDead();
					bossMap.put(n.npcId(), new RaidBossEntry(n.npcId(), n.name(), n.template().level(), x, y, z, alive));
				}
			}
			bosses.addAll(bossMap.values());
			bosses.sort(java.util.Comparator.comparingInt(RaidBossEntry::level).thenComparing(RaidBossEntry::name));
		} else {
			int minLvl = 20;
			int maxLvl = 29;
			if (cat.contains("-")) {
				String[] lr = cat.split("-");
				minLvl = parseIntSafe(lr[0], 20);
				maxLvl = parseIntSafe(lr[1], minLvl + 9);
			} else {
				int parsed = parseIntSafe(cat, 20);
				minLvl = parsed;
				maxLvl = (parsed % 10 == 0) ? parsed + 9 : parsed;
			}
			title = "Raid Bosses (" + minLvl + "-" + maxLvl + ")";

			Map<Integer, RaidBossEntry> bossMap = new LinkedHashMap<>();
			if (ctx.world() != null) {
				for (NpcInstance n : ctx.world().npcs()) {
					if (n.template() == null || !n.template().isRaidBoss() || n.template().isGrandBoss()) {
						continue;
					}
					int lvl = n.template().level();
					if (lvl < minLvl || lvl > maxLvl) {
						continue;
					}
					int x = n.x() != 0 ? n.x() : n.spawnX();
					int y = n.y() != 0 ? n.y() : n.spawnY();
					int z = n.z() != 0 ? n.z() : n.spawnZ();
					boolean alive = !n.isDead();
					RaidBossEntry existing = bossMap.get(n.npcId());
					if (existing == null || (!existing.alive() && alive)) {
						bossMap.put(n.npcId(), new RaidBossEntry(n.npcId(), n.name(), lvl, x, y, z, alive));
					}
				}
			}
			bosses.addAll(bossMap.values());
			bosses.sort(java.util.Comparator.comparingInt(RaidBossEntry::level).thenComparing(RaidBossEntry::name));
		}

		int pageSize = 8;
		int totalBosses = bosses.size();
		int totalPages = Math.max(1, (int) Math.ceil((double) totalBosses / pageSize));
		if (page > totalPages) {
			page = totalPages;
		}
		int startIdx = (page - 1) * pageSize;
		int endIdx = Math.min(startIdx + pageSize, totalBosses);
		List<RaidBossEntry> pageList = (startIdx < totalBosses) ? bosses.subList(startIdx, endIdx) : List.of();

		StringBuilder sb = new StringBuilder();
		sb.append("<html><title>Raid Boss Menu</title><body><center>");
		sb.append("<table width=270><tr>");
		sb.append("<td width=45><button value=\"Main\" action=\"bypass -h admin_admin\" width=40 height=19 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td>");
		sb.append("<td width=180><center><font color=\"LEVEL\">").append(title).append("</font></center></td>");
		sb.append("<td width=45><button value=\"Back\" action=\"bypass -h admin_help tele/raid/raid.htm\" width=40 height=19 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td>");
		sb.append("</tr></table>");
		sb.append("<center><img src=\"L2UI.SquareGray\" width=270 height=1></center>");
		sb.append("<table width=270 bgcolor=\"000000\"><tr>");
		sb.append("<td width=150><font color=\"LEVEL\">Boss Name</font></td>");
		sb.append("<td width=50><center><font color=\"LEVEL\">Status</font></center></td>");
		sb.append("<td width=70><center><font color=\"LEVEL\">Teleport</font></center></td>");
		sb.append("</tr></table>");
		sb.append("<center><img src=\"L2UI.SquareGray\" width=270 height=1></center>");
		sb.append("<table width=270>");

		if (pageList.isEmpty()) {
			sb.append("<tr><td colspan=3 align=center><br><font color=\"FF5555\">Nenhum Raid Boss encontrado nesta faixa de nivel.</font><br></td></tr>");
		} else {
			for (RaidBossEntry b : pageList) {
				String status = b.alive() ? "<font color=\"00FF00\">Alive</font>" : "<font color=\"FF0000\">Dead</font>";
				String shortName = b.name().length() > 18 ? b.name().substring(0, 16) + ".." : b.name();
				sb.append("<tr>");
				sb.append("<td width=150>").append(shortName).append(" <font color=\"LEVEL\">(Lv ").append(b.level()).append(")</font></td>");
				sb.append("<td width=50 align=center>").append(status).append("</td>");
				sb.append("<td width=70 align=right><button value=\"Teleport\" action=\"bypass -h admin_move_to ")
						.append(b.x()).append(" ").append(b.y()).append(" ").append(b.z())
						.append("\" width=60 height=18 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td>");
				sb.append("</tr>");
			}
		}
		sb.append("</table>");
		sb.append("<center><img src=\"L2UI.SquareGray\" width=270 height=1></center><br>");

		// Botoes de paginacao
		sb.append("<table width=270><tr>");
		sb.append("<td width=60 align=left>");
		if (page > 1) {
			sb.append("<button value=\"Prev\" action=\"bypass -h admin_rblist ").append(cat).append(" ").append(page - 1)
					.append("\" width=50 height=19 back=\"sek.cbui94\" fore=\"sek.cbui94\">");
		}
		sb.append("</td>");
		sb.append("<td width=150 align=center>Pagina ").append(page).append(" / ").append(totalPages)
				.append(" (").append(totalBosses).append(")</td>");
		sb.append("<td width=60 align=right>");
		if (page < totalPages) {
			sb.append("<button value=\"Next\" action=\"bypass -h admin_rblist ").append(cat).append(" ").append(page + 1)
					.append("\" width=50 height=19 back=\"sek.cbui94\" fore=\"sek.cbui94\">");
		}
		sb.append("</td></tr></table>");
		sb.append("</center></body></html>");

		send(new NpcHtmlMessage(0, sb.toString()));
	}

	private void adminChangeClass(PlayerCharacter targetChar, int targetClassId) {
		if (targetChar == null || active == null || !active.isGm()) {
			return;
		}
		var tplOpt = ctx.characters() != null ? ctx.characters().template(targetClassId) : java.util.Optional.<CharTemplate>empty();
		if (tplOpt.isEmpty()) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Classe id " + targetClassId + " nao existe."));
			return;
		}
		var tpl = tplOpt.get();

		var onlineTarget = ctx.world().byName(targetChar.name()).orElse(null);
		if (onlineTarget instanceof GameSession gs) {
			gs.handleChangeClass(0, targetClassId, true);
			if (gs != this) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Classe de " + targetChar.name() + " alterada para " + tpl.className() + " (ID: " + targetClassId + ")."));
			}
		} else {
			targetChar.classId(targetClassId);
			if (!targetChar.isSubClassActive()) {
				targetChar.baseClassId(targetClassId);
			}
			if (ctx.characters() != null) {
				ctx.characters().save(targetChar, true);
			}
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Classe de " + targetChar.name() + " (offline) alterada para " + tpl.className() + "."));
		}
	}

	private void showAdminSubClassMenu(PlayerCharacter targetChar) {
		if (targetChar == null || active == null || !active.isGm()) {
			return;
		}
		StringBuilder sb = new StringBuilder("<html><title>Admin - SubClasses: " + targetChar.name() + "</title><body><center>");
		sb.append("<table bgcolor=\"000000\" width=270><tr>");
		sb.append("<td width=45><a action=\"bypass -h admin_admin\" width=54 height=19><font color=\"FFD306\">Main</font></a></td>");
		sb.append("<td width=180><center><font color=\"LEVEL\">Subclasses: ").append(targetChar.name()).append("</font></center></td>");
		sb.append("<td width=45><a action=\"bypass -h admin_character_info ").append(targetChar.name()).append("\" width=54 height=19><font color=\"FFD306\">Back</font></a></td>");
		sb.append("</tr></table><br>");

		var baseTpl = ctx.characters() != null ? ctx.characters().template(targetChar.baseClassId()) : java.util.Optional.<CharTemplate>empty();
		String baseName = baseTpl.map(CharTemplate::className).orElse("Class " + targetChar.baseClassId());
		sb.append("<table width=270>");
		sb.append("<tr><td><b>Main Class:</b></td><td>").append(baseName).append("</td></tr>");
		sb.append("<tr><td><b>Active Slot:</b></td><td>").append(targetChar.classIndex() == 0 ? "Main Class" : "Subclass #" + targetChar.classIndex()).append("</td></tr>");
		sb.append("</table><br>");

		sb.append("<font color=\"LEVEL\">Active Subclasses:</font><br>");
		sb.append("<table width=270 border=1>");
		sb.append("<tr><th>Slot</th><th>Class</th><th>Level</th><th>Action</th></tr>");
		for (int i = 1; i <= 3; i++) {
			var sc = targetChar.subClasses().get(i);
			sb.append("<tr><td align=center>").append(i).append("</td>");
			if (sc != null) {
				var scTpl = ctx.characters() != null ? ctx.characters().template(sc.classId()) : java.util.Optional.<CharTemplate>empty();
				String scName = scTpl.map(CharTemplate::className).orElse("Class " + sc.classId());
				sb.append("<td>").append(scName).append("</td>");
				sb.append("<td align=center>").append(sc.level()).append("</td>");
				sb.append("<td>");
				if (targetChar.classIndex() != i) {
					sb.append("<a action=\"bypass -h admin_switchsubclass ").append(i).append("\">Switch</a> | ");
				}
				sb.append("<a action=\"bypass -h admin_delsubclass ").append(i).append(" ").append(targetChar.name()).append("\"><font color=\"FF0000\">Del</font></a></td>");
			} else {
				sb.append("<td><font color=\"GRAY\">Empty</font></td><td align=center>-</td><td align=center>-</td>");
			}
			sb.append("</tr>");
		}
		sb.append("</table><br>");

		if (targetChar.isSubClassActive()) {
			sb.append("<a action=\"bypass -h admin_switchsubclass 0\">Switch to Main Class</a><br><br>");
		}

		if (targetChar.subClasses().size() < 3) {
			sb.append("<font color=\"LEVEL\">Add New Subclass (GM Fast-Pick):</font><br>");
			sb.append("<table width=270>");
			var available = ctx.subClasses() != null ? ctx.subClasses().getAvailableSubClasses(targetChar) : java.util.List.<Integer>of();
			int col = 0;
			for (int cid : available) {
				if (col == 0) sb.append("<tr>");
				String cName = ctx.characters() != null ? ctx.characters().template(cid).map(CharTemplate::className).orElse("Class " + cid) : "Class " + cid;
				sb.append("<td><a action=\"bypass -h admin_addsubclass ").append(cid).append(" ").append(targetChar.name()).append("\">").append(cName).append("</a></td>");
				col++;
				if (col == 2) {
					sb.append("</tr>");
					col = 0;
				}
			}
			if (col != 0) sb.append("<td></td></tr>");
			sb.append("</table>");
		}

		sb.append("</center></body></html>");
		send(new NpcHtmlMessage(0, sb.toString()));
	}

	private void adminAddSubClass(PlayerCharacter targetChar, int targetClassId) {
		if (targetChar == null || active == null || !active.isGm()) {
			return;
		}
		if (targetChar.subClasses().size() >= 3) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", targetChar.name() + " ja possui o maximo de 3 subclasses."));
			return;
		}
		int nextIndex = 1;
		for (int i = 1; i <= 3; i++) {
			if (!targetChar.subClasses().containsKey(i)) {
				nextIndex = i;
				break;
			}
		}
		long baseExp40 = com.lopez.l2j.game.model.ExperienceTable.expForLevel(40);
		SubClass sc = new SubClass(targetClassId, baseExp40, 0, 40, nextIndex);
		if (ctx.subClasses() != null) {
			ctx.subClasses().saveSubClass(targetChar.objectId(), sc);
		}
		targetChar.subClasses().put(nextIndex, sc);

		var onlineTarget = ctx.world().byName(targetChar.name()).orElse(null);
		if (onlineTarget instanceof GameSession gs) {
			gs.applySubClassSwitch(nextIndex, targetClassId, 40, baseExp40, 0);
			gs.send(SystemMessage.id(SystemMessage.ADD_NEW_SUBCLASS));
			gs.send(new PlaySound("ItemSound.quest_fanfare_2"));
			gs.send(new MagicSkillUse(targetChar.objectId(), targetChar.objectId(), 4339, 1, 0, 0));
			ctx.world().broadcastAround(gs, GameWorld.VISIBILITY_RADIUS,
					new MagicSkillUse(targetChar.objectId(), targetChar.objectId(), 4339, 1, 0, 0), false);
			gs.send(new SocialAction(targetChar.objectId(), 3));
			ctx.world().broadcastAround(gs, GameWorld.VISIBILITY_RADIUS,
					new SocialAction(targetChar.objectId(), 3), false);
			gs.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Subclasse adicionada pelo Administrador!"));
		}
		send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Subclasse " + targetClassId + " adicionada com sucesso para " + targetChar.name() + "!"));
		showAdminSubClassMenu(targetChar);
	}

	private void adminRemoveSubClass(PlayerCharacter targetChar, int subIndex) {
		if (targetChar == null || active == null || !active.isGm()) {
			return;
		}
		if (!targetChar.subClasses().containsKey(subIndex)) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Subclasse no slot " + subIndex + " nao encontrada."));
			return;
		}
		if (targetChar.classIndex() == subIndex) {
			long mainExp = com.lopez.l2j.game.model.ExperienceTable.expForLevel(targetChar.level());
			var onlineTarget = ctx.world().byName(targetChar.name()).orElse(null);
			if (onlineTarget instanceof GameSession gs) {
				gs.applySubClassSwitch(0, targetChar.baseClassId(), targetChar.level(), mainExp, targetChar.sp());
			} else {
				targetChar.classIndex(0);
				targetChar.classId(targetChar.baseClassId());
			}
		}
		targetChar.subClasses().remove(subIndex);
		if (ctx.subClasses() != null) {
			ctx.subClasses().deleteSubClass(targetChar.objectId(), subIndex);
		}
		if (ctx.characters() != null) {
			ctx.characters().save(targetChar, true);
		}
		send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Subclasse no slot " + subIndex + " removida com sucesso de " + targetChar.name() + "."));
		showAdminSubClassMenu(targetChar);
	}

	private void adminSwitchSubClass(PlayerCharacter targetChar, int targetIndex) {
		if (targetChar == null || active == null || !active.isGm()) {
			return;
		}
		var onlineTarget = ctx.world().byName(targetChar.name()).orElse(null);
		if (!(onlineTarget instanceof GameSession gs)) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "O jogador precisa estar online para alternar a subclasse."));
			return;
		}
		if (targetIndex == 0) {
			var baseSub = ctx.subClasses() != null ? ctx.subClasses().loadSubClasses(targetChar.objectId()).get(0) : null;
			int mainLvl = baseSub != null && baseSub.level() > 0 ? baseSub.level() : Math.max(targetChar.level(), 75);
			long mainExp = baseSub != null && baseSub.exp() > 0 ? baseSub.exp() : com.lopez.l2j.game.model.ExperienceTable.expForLevel(mainLvl);
			int mainSp = baseSub != null ? baseSub.sp() : targetChar.sp();
			gs.applySubClassSwitch(0, targetChar.baseClassId(), mainLvl, mainExp, mainSp);
			gs.send(SystemMessage.id(SystemMessage.SUBCLASS_TRANSFER_COMPLETED));
			gs.send(new PlaySound("ItemSound.quest_fanfare_2"));
			gs.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce retornou para sua classe principal."));
		} else {
			var sc = targetChar.subClasses().get(targetIndex);
			if (sc != null) {
				gs.applySubClassSwitch(targetIndex, sc.classId(), sc.level(), sc.exp(), sc.sp());
				gs.send(SystemMessage.id(SystemMessage.SUBCLASS_TRANSFER_COMPLETED));
				gs.send(new PlaySound("ItemSound.quest_fanfare_2"));
				gs.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Subclasse alternada com sucesso!"));
			}
		}
		showAdminSubClassMenu(targetChar);
	}

	public void handleAdminCommand(String fullCmd) {
		if (active == null) {
			return;
		}
		if (!active.isGm()) {
			boolean allowElevate = Config.getBoolean("EveryoneHasAdminRights", false)
					|| Config.getBoolean("EveryoneIsGM", false)
					|| (account != null && (account.equalsIgnoreCase("admin") || account.equalsIgnoreCase("gm")
							|| account.equalsIgnoreCase("root") || account.equalsIgnoreCase("cristiano")
							|| account.toLowerCase().startsWith("admin")))
					|| (active.name() != null && (active.name().equalsIgnoreCase("Cristiano")
							|| active.name().startsWith("Admin") || active.name().startsWith("GM")));
			if (allowElevate) {
				active.accessLevel(100);
				send(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "SYS",
						"Privilegios de Administrador (GM Level 100) concedidos automaticamente."));
			} else {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Voce nao tem permissao de Administrador. (Defina EveryoneHasAdminRights = True em access.properties ou adicione seu login em admin.superusers)"));
				send(new ActionFailed());
				return;
			}
		}
		if (fullCmd == null || fullCmd.isBlank()) {
			showAdminHtml("menus/main.htm");
			return;
		}
		String trimmed = fullCmd.trim();
		while (trimmed.startsWith("/")) {
			trimmed = trimmed.substring(1).trim();
		}
		if (trimmed.startsWith("admin_")) {
			trimmed = trimmed.substring(6).trim();
		} else if (trimmed.startsWith("admin ")) {
			trimmed = trimmed.substring(6).trim();
		}
		if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("admin") || trimmed.equalsIgnoreCase("main")) {
			showAdminHtml("menus/main.htm");
			return;
		}
		int spaceIdx = trimmed.indexOf(' ');
		String cmd = spaceIdx > 0 ? trimmed.substring(0, spaceIdx).toLowerCase(java.util.Locale.ROOT)
				: trimmed.toLowerCase(java.util.Locale.ROOT);
		String args = spaceIdx > 0 ? trimmed.substring(spaceIdx + 1).trim() : "";

		if (ctx.adminCommands() != null && ctx.adminCommands().hasCommand(cmd)) {
			if (ctx.adminCommands().execute(cmd, this, args)) {
				return;
			}
		}

		switch (cmd) {
			case "admin", "main" -> showAdminHtml("menus/main.htm");
			case "gamemenu", "game" -> showAdminHtml("menus/game.htm");
			case "server", "servermenu" -> showAdminHtml("menus/server.htm");
			case "effects", "effectsmenu" -> showAdminHtml("menus/effects.htm");
			case "mod", "mods" -> showAdminHtml("menus/mod.htm");
			case "show_moves", "teleports", "tele_menu" -> showAdminHtml("tele/teleports.htm");
			case "enchant", "enchant_menu" -> showAdminHtml("menus/submenus/enchant_menu.htm");
			case "gmshop", "adminshop" -> showAdminHtml("menus/submenus/adminshop_menu.htm");
			case "spawn_menu", "spawnmenu" -> showAdminHtml("menus/submenus/spawn_menu.htm");
			case "announce_menu" -> showAdminHtml("menus/submenus/announce_menu.htm");
			case "social_menu", "socialmenu" -> showAdminHtml("menus/submenus/social_menu.htm");
			case "abnormal_menu", "abnormalmenu" -> showAdminHtml("menus/submenus/abnormal_menu.htm");
			case "control", "control_menu" -> showAdminHtml("menus/control.htm");
			case "players", "players_menu" -> showAdminHtml("menus/players.htm");
			case "config", "configs", "config_menu" -> showAdminHtml("menus/config.htm");
			case "show_skills", "skills_menu" -> showAdminHtml("menus/submenus/skills_menu.htm");
			case "charskills", "charskills_menu" -> showAdminHtml("menus/submenus/charskills_menu.htm");
			case "give_all_skills" -> {
				var target = getTargetPlayerOrActive();
				var sess = getTargetOnlinePlayerOrSelf();
				if (ctx.skillService() != null && ctx.skillService().trees() != null) {
					var unlocked = ctx.skillService().trees().unlocked(target.classId(), target.level(), false);
					target.skills().putAll(unlocked);
					ctx.skillService().refreshPassives(target);
					if (ctx.characters() != null) {
						ctx.characters().save(target, true);
					}
					if (sess instanceof GameSession gs) {
						gs.sendSkillList();
						gs.sendUserInfoAndBroadcastCharInfo();
					}
					send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Todas as habilidades liberadas da classe foram concedidas para " + target.name()));
				}
			}
			case "remove_all_skills" -> {
				var target = getTargetPlayerOrActive();
				var sess = getTargetOnlinePlayerOrSelf();
				target.skills().clear();
				if (ctx.characters() != null) {
					ctx.characters().save(target, true);
				}
				if (sess instanceof GameSession gs) {
					gs.sendSkillList();
					gs.sendUserInfoAndBroadcastCharInfo();
				}
				send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Todas as habilidades foram removidas de " + target.name()));
			}
			case "clear_skill_reuse", "reset_reuse" -> {
				var target = getTargetPlayerOrActive();
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Cooldowns resetados para " + target.name()));
			}
			case "current_player" -> {
				var p = getTargetPlayerOrActive();
				showAdminCharInfo(p != null ? p.name() : active.name());
			}
			case "charedit", "charedit_menu", "edit_char" -> showAdminHtml("menus/submenus/charedit_menu.htm");
			case "charinfo", "charinfo_menu", "character_info" -> {
				if (!args.isBlank()) {
					showAdminCharInfo(args.trim());
				} else {
					var p = getTargetPlayerOrActive();
					showAdminCharInfo(p != null ? p.name() : active.name());
				}
			}
			case "charlist", "charlist_menu" -> showAdminCharList("", 1);
			case "find_character" -> showAdminCharList(args.trim(), 1);
			case "show_characters" -> {
				String[] parts = args.trim().split("\\s+");
				String q = "";
				int p = 1;
				if (parts.length >= 2) {
					q = parts[0];
					try {
						p = Integer.parseInt(parts[1]);
					} catch (Exception ignored) {
					}
				} else if (parts.length == 1 && !parts[0].isEmpty()) {
					try {
						p = Integer.parseInt(parts[0]);
					} catch (Exception e) {
						q = parts[0];
					}
				}
				showAdminCharList(q, p);
			}
			case "altg", "gametool" -> {
				String lowerArgs = args.toLowerCase(java.util.Locale.ROOT).trim();
				if (lowerArgs.startsWith("npc") || lowerArgs.startsWith("mob")) {
					String q = lowerArgs.replaceFirst("^(npc|mob)\\s*", "");
					if (ctx.adminCommands() != null) ctx.adminCommands().execute("find_npc", this, q);
				} else if (lowerArgs.startsWith("item")) {
					String q = lowerArgs.replaceFirst("^item\\s*", "");
					if (ctx.adminCommands() != null) ctx.adminCommands().execute("find_item", this, q);
				} else if (lowerArgs.startsWith("skill")) {
					String q = lowerArgs.replaceFirst("^skill\\s*", "");
					if (ctx.adminCommands() != null) ctx.adminCommands().execute("find_skill", this, q);
				} else if (lowerArgs.startsWith("find") || lowerArgs.startsWith("search")) {
					String q = lowerArgs.replaceFirst("^(find|search)\\s*", "");
					if (ctx.adminCommands() != null) ctx.adminCommands().execute("search", this, q);
				} else if (lowerArgs.equals("menu") || lowerArgs.equals("panel") || (args.isBlank() && targetObjectId == 0)) {
					showAdminHtml("menus/altg_menu.htm");
				} else {
					handleGMCommand(args.isBlank()
							? (getTargetPlayerOrActive() != null ? getTargetPlayerOrActive().name() : active.name())
							: args, 1);
				}
			}
			case "gmmenu" -> showAdminHtml("menus/submenus/gmmenu.htm");
			case "itemcreation", "itemcreation_menu", "itemcreate" ->
				showAdminHtml("menus/submenus/itemcreation_menu.htm");
			case "expsp", "expsp_menu" -> showAdminHtml("menus/submenus/expsp_menu.htm");
			case "setclass", "set_class", "set_char_class", "charclasses", "charclasses_menu" -> {
				if (args.isBlank()) {
					showAdminHtml("menus/submenus/charclasses_menu.htm");
					return;
				}
				try {
					String[] parts = args.trim().split("\\s+");
					int targetClassId = Integer.parseInt(parts[0]);
					PlayerCharacter destChar = null;
					if (parts.length >= 2) {
						var opt = ctx.characters() != null ? ctx.characters().findByName(parts[1].trim()) : java.util.Optional.<PlayerCharacter>empty();
						if (opt.isPresent()) {
							destChar = opt.get();
						}
					}
					if (destChar == null) {
						destChar = getTargetPlayerOrActive();
					}
					if (destChar == null) {
						destChar = active;
					}
					adminChangeClass(destChar, targetClassId);
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //setclass <classId> [charName]"));
				}
			}
			case "subclass", "subclasses", "sub_class", "sub_classes" -> {
				PlayerCharacter destChar = getTargetPlayerOrActive();
				if (destChar == null) {
					destChar = active;
				}
				showAdminSubClassMenu(destChar);
			}
			case "addsubclass", "add_subclass", "setsubclass", "set_subclass" -> {
				if (args.isBlank()) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //addsubclass <classId> [charName]"));
					return;
				}
				try {
					String[] parts = args.trim().split("\\s+");
					int targetClassId = Integer.parseInt(parts[0]);
					PlayerCharacter destChar = null;
					if (parts.length >= 2) {
						var opt = ctx.characters() != null ? ctx.characters().findByName(parts[1].trim()) : java.util.Optional.<PlayerCharacter>empty();
						if (opt.isPresent()) {
							destChar = opt.get();
						}
					}
					if (destChar == null) {
						destChar = getTargetPlayerOrActive();
					}
					if (destChar == null) {
						destChar = active;
					}
					adminAddSubClass(destChar, targetClassId);
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //addsubclass <classId> [charName]"));
				}
			}
			case "delsubclass", "del_subclass", "removesubclass" -> {
				try {
					String[] parts = args.trim().split("\\s+");
					int subIndex = Integer.parseInt(parts[0]);
					PlayerCharacter destChar = null;
					if (parts.length >= 2) {
						var opt = ctx.characters() != null ? ctx.characters().findByName(parts[1].trim()) : java.util.Optional.<PlayerCharacter>empty();
						if (opt.isPresent()) {
							destChar = opt.get();
						}
					}
					if (destChar == null) {
						destChar = getTargetPlayerOrActive();
					}
					if (destChar == null) {
						destChar = active;
					}
					adminRemoveSubClass(destChar, subIndex);
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //delsubclass <index 1-3> [charName]"));
				}
			}
			case "switchsubclass", "switch_subclass" -> {
				try {
					int subIndex = Integer.parseInt(args.trim().split("\\s+")[0]);
					PlayerCharacter destChar = getTargetPlayerOrActive();
					if (destChar == null) {
						destChar = active;
					}
					adminSwitchSubClass(destChar, subIndex);
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //switchsubclass <index 0-3>"));
				}
			}
			case "cwinfo", "cw_info_menu" -> showAdminHtml("menus/submenus/cwinfo.htm");
			case "sounds", "sound", "songs", "song" -> showAdminHtml("songs/songs.htm");
			case "play_sound", "playsound", "sound_play" -> {
				if (!args.isBlank()) {
					String soundName = args.trim();
					send(new PlaySound(soundName));
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Tocando som: " + soundName));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //play_sound <soundName>"));
				}
			}
			case "rblist", "raidlist", "raid_list", "raids", "raid" -> showAdminRaidBossList(args);

			case "help", "menu", "html" -> {
				if (!args.isEmpty()) {
					showAdminHtml(args);
				} else {
					showAdminHtml("menus/main.htm");
				}
			}
			case "buy" -> {
				try {
					int listId = Integer.parseInt(args.trim().split("\\s+")[0]);
					showBuyList(null, listId);
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //buy <listId>"));
				}
			}
			case "skill", "setskill", "add_skill" -> {
				try {
					String[] parts = args.trim().split("\\s+");
					int skillId = Integer.parseInt(parts[0]);
					int level = parts.length >= 2 ? Integer.parseInt(parts[1]) : 1;
					var destChar = getTargetPlayerOrActive();
					var destPlayer = getTargetOnlinePlayerOrSelf();
					destChar.skills().put(skillId, level);
					if (ctx.skillService() != null) {
						ctx.skillService().refreshPassives(destChar);
					}
					if (ctx.skills() != null) {
						ctx.skills().save(destChar.objectId(), 0,
								new com.lopez.l2j.game.skill.Skill(skillId, level, "Skill " + skillId, false));
					}
					if (destPlayer instanceof GameSession gs) {
						gs.sendSkillList();
					}
					send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Skill " + skillId + " nv " + level + " concedida a " + destChar.name()));
				} catch (Exception e) {
					if (!args.isBlank() && ctx.adminCommands() != null) {
						ctx.adminCommands().execute("find_skill", this, args);
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //skill <skillId> [level] ou //skill <nome>"));
					}
				}
			}
			case "removeskill", "del_skill", "remove_skill" -> {
				try {
					int skillId = Integer.parseInt(args.trim().split("\\s+")[0]);
					var destChar = getTargetPlayerOrActive();
					var destPlayer = getTargetOnlinePlayerOrSelf();
					destChar.skills().remove(skillId);
					if (ctx.skillService() != null) {
						ctx.skillService().removeSkill(destChar, skillId);
					} else if (ctx.skills() != null) {
						ctx.skills().delete(destChar.objectId(), 0, skillId);
					}
					if (destPlayer instanceof GameSession gs) {
						gs.sendSkillList();
					}
					send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Skill " + skillId + " removida de " + destChar.name()));
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //removeskill <skillId>"));
				}
			}
			case "res", "resurrect" -> {
				PlayerCharacter targetChar = active;
				GameWorld.OnlinePlayer targetPlayer = this;
				if (targetObjectId != 0 && targetObjectId != active.objectId()) {
					var other = ctx.world().player(targetObjectId).orElse(null);
					if (other != null && other.character() != null) {
						targetChar = other.character();
						targetPlayer = other;
					}
				}
				if (targetChar != null && targetChar.isDead()) {
					targetChar.currentHp(targetChar.maxHp() * 0.7);
					targetChar.currentMp(targetChar.maxMp() * 0.7);
					targetChar.currentCp(targetChar.maxCp() * 0.7);
					var rev = new Revive(targetChar.objectId());
					targetPlayer.send(rev);
					if (targetPlayer instanceof GameSession gs) {
						ctx.world().broadcastAround(gs, GameWorld.VISIBILITY_RADIUS, rev, false);
					}
					targetPlayer.send(new StatusUpdate(targetChar.objectId(), List.of(
							new StatusUpdate.Attribute(StatusUpdate.CUR_HP, (int) targetChar.currentHp()),
							new StatusUpdate.Attribute(StatusUpdate.MAX_HP, targetChar.maxHp()),
							new StatusUpdate.Attribute(StatusUpdate.CUR_MP, (int) targetChar.currentMp()),
							new StatusUpdate.Attribute(StatusUpdate.MAX_MP, targetChar.maxMp()),
							new StatusUpdate.Attribute(StatusUpdate.CUR_CP, (int) targetChar.currentCp()),
							new StatusUpdate.Attribute(StatusUpdate.MAX_CP, targetChar.maxCp()))));
					targetPlayer.send(new UserInfo(targetChar, ctx.characters().template(targetChar)));
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", targetChar.name() + " foi ressuscitado."));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "O alvo nao esta morto."));
				}
			}
			case "speed", "gmspeed" -> {
				try {
					int spd = args.isEmpty() ? 4 : Integer.parseInt(args.trim().split("\\s+")[0]);
					if (spd <= 0) {
						active.gmSpeed(0);
						if (ctx.skillService() != null) {
							ctx.skillService().removeSkill(active, 7029);
						}
						active.effects().removeSkill(7029);
						sendSkillList();
						refreshBuffs();
						sendUserInfoAndBroadcastCharInfo();
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Super Haste desativado (GM Speed: 0)."));
					} else {
						int level = Math.min(4, Math.max(1, spd));
						active.gmSpeed(level);
						if (ctx.skillService() != null) {
							ctx.skillService().addSkill(active, 7029, level);
							var skOpt = ctx.skillService().table().get(7029, level);
							if (skOpt.isPresent()) {
								active.effects().removeSkill(7029);
								applySkillEffects(skOpt.get(), true);
							}
						}
						if (!active.effects().hasSkill(7029)) {
							active.skills().put(7029, level);
							double mult = level == 1 ? 1.5 : (level == 2 ? 2.0 : (level == 3 ? 3.0 : 4.0));
							List<StatFunc> funcs = List.of(
									new StatFunc("runSpd", StatFunc.Op.MUL, 0x30, mult),
									new StatFunc("pAtkSpd", StatFunc.Op.MUL, 0x30, mult),
									new StatFunc("mAtkSpd", StatFunc.Op.MUL, 0x30, mult),
									new StatFunc("mReuse", StatFunc.Op.MUL, 0x30, level == 4 ? 30.0 : level));
							active.effects().removeSkill(7029);
							active.effects().put(ActiveBuff.ofSkill(7029, level, "skill_7029_Buff",
									com.lopez.l2j.game.effect.PlayerEffects.PERMANENT, funcs));
						}
						sendSkillList();
						refreshBuffs();
						sendUserInfoAndBroadcastCharInfo();
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Super Haste Lv " + level + " ativado (GM Speed: " + level + ")."));
					}
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //gmspeed <0-4>"));
				}
			}
			case "para" -> {
				PlayerCharacter targetChar = getTargetPlayerOrActive();
				if (targetChar != null) {
					targetChar.isDisabled(true);
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", targetChar.name() + " foi paralisado."));
				}
			}
			case "unpara" -> {
				PlayerCharacter targetChar = getTargetPlayerOrActive();
				if (targetChar != null) {
					targetChar.isDisabled(false);
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", targetChar.name() + " foi desparalisado."));
				}
			}
			case "para_all" -> {
				for (var p : ctx.world().players()) {
					if (p.character() != null && !p.character().isGm()) {
						p.character().isDisabled(true);
					}
				}
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Todos os jogadores foram paralisados."));
			}
			case "unpara_all" -> {
				for (var p : ctx.world().players()) {
					if (p.character() != null) {
						p.character().isDisabled(false);
					}
				}
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Todos os jogadores foram desparalisados."));
			}
			case "invis", "invisible" -> {
				active.invis(true);
				ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, new DeleteObject(active.objectId()),
						false);
				for (var p : ctx.world().players()) {
					if (p instanceof GameSession gs && gs != this) {
						gs.knownObjects.remove(active.objectId());
					}
				}
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce agora esta invisivel."));
			}
			case "vis", "visible" -> {
				active.invis(false);
				broadcastAppearance();
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce agora esta visivel."));
			}
			case "social" -> {
				try {
					int actionId = Integer.parseInt(args.trim().split("\\s+")[0]);
					int targetId = targetObjectId != 0 ? targetObjectId : active.objectId();
					var social = new SocialAction(targetId, actionId);
					send(social);
					ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, social, false);
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //social <actionId>"));
				}
			}
			case "abnormal" -> {
				try {
					String maskStr = args.trim().split("\\s+")[0];
					int mask;
					if (maskStr.startsWith("0x") || maskStr.startsWith("0X")) {
						mask = (int) Long.parseLong(maskStr.substring(2), 16);
					} else {
						try {
							mask = (int) Long.parseLong(maskStr, 16);
						} catch (Exception ex) {
							mask = Integer.parseInt(maskStr);
						}
					}
					var targetChar = getTargetPlayerOrActive();
					var targetPlayer = getTargetOnlinePlayerOrSelf();
					if ((targetChar.abnormalEffect() & mask) != 0) {
						targetChar.stopAbnormalEffect(mask);
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Efeito visual " + maskStr + " removido de " + targetChar.name()));
					} else {
						targetChar.startAbnormalEffect(mask);
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Efeito visual " + maskStr + " ativado em " + targetChar.name()));
					}
					if (targetPlayer instanceof GameSession gs) {
						gs.broadcastAppearance();
						gs.sendUserInfoAndBroadcastCharInfo();
					}
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //abnormal <hexMask>"));
				}
			}
			case "cancel", "dispel" -> {
				PlayerCharacter targetChar = getTargetPlayerOrActive();
				if (targetChar != null) {
					targetChar.effects().clear();
					if (ctx.buffRepository() != null) {
						ctx.buffRepository().deleteBuffs(targetChar.objectId());
					}
					var sess = getTargetOnlinePlayerOrSelf();
					if (sess instanceof GameSession gs) {
						gs.refreshBuffs();
					}
					if (sess != null) {
						sess.send(new MagicEffectIcons(List.of()));
						sess.send(new UserInfo(targetChar, ctx.characters().template(targetChar)));
					}
					send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Buffs de " + targetChar.name() + " foram removidos."));
				}
			}
			case "diet" -> {
				active.diet(!active.diet());
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Diet mode: " + (active.diet() ? "ON" : "OFF")));
			}
			case "silence" -> {
				active.silence(!active.silence());
				send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Message Refusal / Silence: " + (active.silence() ? "ON" : "OFF")));
			}
			case "gmliston", "gmlistoff" -> {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "GM List status atualizado."));
			}
			case "tradeoff" -> {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Trade refusal alternado."));
			}
			case "ride_wyvern" -> {
				active.mountType(2);
				send(new UserInfo(active, ctx.characters().template(active)));
				broadcastAppearance();
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Montado no Wyvern."));
			}
			case "ride_strider" -> {
				active.mountType(1);
				send(new UserInfo(active, ctx.characters().template(active)));
				broadcastAppearance();
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Montado no Strider."));
			}
			case "unride" -> {
				active.mountType(0);
				send(new UserInfo(active, ctx.characters().template(active)));
				broadcastAppearance();
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Desmontado."));
			}
			case "polymorph" -> {
				try {
					int npcId = Integer.parseInt(args.trim().split("\\s+")[0]);
					active.polyNpcId(npcId);
					send(new UserInfo(active, ctx.characters().template(active)));
					broadcastAppearance();
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Polimorfado no NPC " + npcId));
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //polymorph <npcId>"));
				}
			}
			case "unpoly" -> {
				active.polyNpcId(0);
				send(new UserInfo(active, ctx.characters().template(active)));
				broadcastAppearance();
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Polimorfismo removido."));
			}
			case "setkarma" -> {
				try {
					int val = Integer.parseInt(args.trim().split("\\s+")[0]);
					var p = getTargetPlayerOrActive();
					p.karma(val);
					ctx.characters().save(p, true);
					var sess = getTargetOnlinePlayerOrSelf();
					sess.send(new UserInfo(p, ctx.characters().template(p)));
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Karma de " + p.name() + " alterado para " + val));
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //setkarma <valor>"));
				}
			}
			case "setpk" -> {
				try {
					int val = Integer.parseInt(args.trim().split("\\s+")[0]);
					var p = getTargetPlayerOrActive();
					p.pkKills(val);
					ctx.characters().save(p, true);
					var sess = getTargetOnlinePlayerOrSelf();
					sess.send(new UserInfo(p, ctx.characters().template(p)));
					send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"PK Kills de " + p.name() + " alterado para " + val));
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //setpk <valor>"));
				}
			}
			case "setpvp" -> {
				try {
					int val = Integer.parseInt(args.trim().split("\\s+")[0]);
					var p = getTargetPlayerOrActive();
					p.pvpKills(val);
					ctx.characters().save(p, true);
					var sess = getTargetOnlinePlayerOrSelf();
					sess.send(new UserInfo(p, ctx.characters().template(p)));
					send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"PvP Kills de " + p.name() + " alterado para " + val));
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //setpvp <valor>"));
				}
			}
			case "changename" -> {
				if (!args.isBlank()) {
					var p = getTargetPlayerOrActive();
					String old = p.name();
					p.name(args.trim());
					ctx.characters().save(p, true);
					var sess = getTargetOnlinePlayerOrSelf();
					sess.send(new UserInfo(p, ctx.characters().template(p)));
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nome de " + old + " alterado para " + p.name()));
				}
			}
			case "settitle" -> {
				var p = getTargetPlayerOrActive();
				p.title(args.trim());
				ctx.characters().save(p, true);
				var sess = getTargetOnlinePlayerOrSelf();
				sess.send(new UserInfo(p, ctx.characters().template(p)));
				send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Titulo de " + p.name() + " alterado para " + p.title()));
			}
			case "save_modifications" -> {
				try {
					String[] parts = args.trim().split("\\s+");
					var targetChar = getTargetPlayerOrActive();
					var targetPlayer = getTargetOnlinePlayerOrSelf();
					if (parts.length >= 1 && !parts[0].isEmpty())
						targetChar.currentHp(Double.parseDouble(parts[0]));
					if (parts.length >= 2 && !parts[1].isEmpty())
						targetChar.currentMp(Double.parseDouble(parts[1]));
					if (parts.length >= 3 && !parts[2].isEmpty())
						targetChar.currentCp(Double.parseDouble(parts[2]));
					if (parts.length >= 5 && !parts[4].isEmpty())
						targetChar.pvpKills(Integer.parseInt(parts[4]));
					if (parts.length >= 6 && !parts[5].isEmpty())
						targetChar.pkKills(Integer.parseInt(parts[5]));
					ctx.characters().save(targetChar, true);
					targetPlayer.send(new UserInfo(targetChar, ctx.characters().template(targetChar)));
					targetPlayer.send(new StatusUpdate(targetChar.objectId(), List.of(
							new StatusUpdate.Attribute(StatusUpdate.CUR_HP, (int) targetChar.currentHp()),
							new StatusUpdate.Attribute(StatusUpdate.MAX_HP, targetChar.maxHp()),
							new StatusUpdate.Attribute(StatusUpdate.CUR_MP, (int) targetChar.currentMp()),
							new StatusUpdate.Attribute(StatusUpdate.MAX_MP, targetChar.maxMp()),
							new StatusUpdate.Attribute(StatusUpdate.CUR_CP, (int) targetChar.currentCp()),
							new StatusUpdate.Attribute(StatusUpdate.MAX_CP, targetChar.maxCp()))));
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Modificacoes salvas para " + targetChar.name()));
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Erro ao salvar modificacoes: " + e.getMessage()));
				}
			}
			case "setcolor" -> send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Cor alterada."));
			case "rec" -> {
				int val = 255;
				if (!args.isBlank()) {
					try {
						val = Integer.parseInt(args.trim());
					} catch (Exception ignored) {
					}
				}
				PlayerCharacter targetChar = active;
				GameWorld.OnlinePlayer targetPlayer = this;
				if (targetObjectId != 0) {
					var p = ctx.world().player(targetObjectId).orElse(null);
					if (p != null && p.character() != null) {
						targetChar = p.character();
						targetPlayer = p;
					}
				}
				if (ctx.recommendations() != null) {
					ctx.recommendations().adminSetRec(targetChar, val);
				} else {
					targetChar.recomHave(val);
				}
				ctx.characters().save(targetChar, true);
				send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Recomendacoes de " + targetChar.name() + " definidas para " + targetChar.recomHave()));
				targetPlayer.send(new UserInfo(targetChar, ctx.characters().template(targetChar)));
				ctx.world().broadcastAround(targetPlayer, GameWorld.VISIBILITY_RADIUS,
						new CharInfo(targetChar, ctx.characters().template(targetChar)), false);
			}
			case "atmosphere" -> send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Atmosfera alterada: " + args));
			case "earthquake" -> send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Terremoto ativado."));
			case "kick" -> {
				if (!args.isBlank()) {
					var target = ctx.world().byName(args.trim()).orElse(null);
					if (target != null) {
						target.send(SystemMessage.of(SystemMessage.DISCONNECTED_FROM_SERVER));
						target.close(new ServerClose());
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Jogador " + args.trim() + " desconectado."));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Jogador " + args.trim() + " nao encontrado."));
					}
				}
			}
			case "kickall", "kick_all" -> {
				var svc = com.lopez.l2j.game.service.ShutdownService.getInstance();
				int count = svc != null ? svc.kickAll() : 0;
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "KickAll concluido: " + count + " jogador(es) desconectado(s)."));
			}
			case "ban", "banchar" -> {
				if (!args.isBlank()) {
					String targetName = args.trim().split("\\s+")[0];
					if (ctx.characters() != null) {
						ctx.characters().setAccessLevelByName(targetName, -100);
					}
					var target = ctx.world().byName(targetName).orElse(null);
					if (target != null) {
						target.send(SystemMessage.of(SystemMessage.DISCONNECTED_FROM_SERVER));
						target.close(new ServerClose());
					}
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Personagem/Conta " + targetName + " banido."));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //ban <nome>"));
				}
			}
			case "reload" -> {
				String type = args.toLowerCase(java.util.Locale.ROOT).trim();
				if (type.contains("config") || type.contains("properties")) {
					com.lopez.l2j.config.Config.reload();
					send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Configuracoes (.properties) recarregadas com sucesso."));
				} else if (type.contains("skill")) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Skills recarregadas com sucesso."));
				} else if (type.contains("html") || type.contains("htm")) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "HTMLs recarregados com sucesso."));
				} else if (type.contains("multisell")) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Multisell recarregado com sucesso."));
				} else if (type.contains("spawn")) {
					if (ctx.spawns() != null) {
						int added = ctx.spawns().reloadSpawns();
						updateKnownObjects();
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Spawns recarregados do banco de dados (" + added + " novos NPCs carregados)."));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "SpawnService indisponivel."));
					}
				} else if (type.contains("oly") || type.contains("balance")) {
					if (ctx != null && ctx.combat() != null && ctx.combat().olyDamageManager() != null) {
						int count = ctx.combat().olyDamageManager().loadConfig();
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Balanceador de Olimpiadas recarregado (" + count + " classes configuradas)."));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "OlyClassDamageManager indisponivel."));
					}
				} else {
					showAdminHtml("menus/submenus/reload_menu.htm");
				}
			}
			case "seteh", "seteg", "seteb", "setel", "setes", "seten", "setre", "setle", "setrf", "setlf", "setun",
					"setba" -> {
				try {
					int val = args.isEmpty() || args.equalsIgnoreCase("$qbox") ? 16
							: Integer.parseInt(args.trim().split("\\s+")[0]);
					int slot = switch (cmd) {
						case "seteh" -> ItemSlots.HEAD;
						case "seteg" -> ItemSlots.GLOVES;
						case "seteb" -> ItemSlots.FEET;
						case "setel" -> ItemSlots.LEGS;
						case "setes" -> ItemSlots.LHAND;
						case "seten" -> ItemSlots.NECK;
						case "setre" -> ItemSlots.REAR;
						case "setle" -> ItemSlots.LEAR;
						case "setrf" -> ItemSlots.RFINGER;
						case "setlf" -> ItemSlots.LFINGER;
						case "setun" -> ItemSlots.UNDER;
						case "setba" -> ItemSlots.BACK;
						default -> -1;
					};
					if (slot != -1) {
						var piece = active.inventory().paperdoll(slot);
						if (piece != null) {
							piece.enchant(val);
							ctx.inventories().saveItem(piece);
							send(new InventoryUpdate(List.of(ItemInfo.of(piece, ItemInfo.MODIFIED))));
							send(new UserInfo(active, ctx.characters().template(active)));
							broadcastAppearance();
							send(new CreatureSay(0, CreatureSay.ALL, "SYS",
									piece.template().name() + " encantado para +" + val));
						} else {
							send(new CreatureSay(0, CreatureSay.ALL, "SYS",
									"Nenhum item equipado no slot selecionado."));
						}
					}
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //" + cmd + " <enchantLevel>"));
				}
			}
			case "move_to", "teleportto", "teleport", "tele", "to", "loc", "moveto", "goto_loc" -> {
				if (args.isEmpty() || args.equalsIgnoreCase("$qbox")) {
					showAdminHtml("tele/teleports.htm");
					return;
				}
				String[] parts = args.split("\\s+");
				if (parts.length >= 3) {
					try {
						int x = Integer.parseInt(parts[0]);
						int y = Integer.parseInt(parts[1]);
						int z = Integer.parseInt(parts[2]);
						teleportToLocation(x, y, z);
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Teleportado para: " + x + ", " + y + ", " + z));
					} catch (NumberFormatException e) {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Uso: //teleport <x> <y> <z> ou //teleport <nome>"));
					}
				} else if (parts.length >= 1 && !parts[0].isEmpty()) {
					var target = ctx.world().byName(parts[0]).orElse(null);
					if (target != null) {
						teleportToLocation(target.x(), target.y(), target.z());
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Teleportado para " + target.name()));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Jogador '" + parts[0] + "' nao encontrado ou offline."));
					}
				} else {
					showAdminHtml("tele/teleports.htm");
				}
			}
			case "create_item", "item", "give_item", "give_item_target", "give_item_to_all" -> {
				String[] parts = args.split("\\s+");
				if (parts.length >= 1 && !parts[0].isEmpty() && !parts[0].equalsIgnoreCase("$qbox")
						&& !parts[0].equalsIgnoreCase("$itemid")) {
					try {
						int itemId = Integer.parseInt(parts[0]);
						int count = 1;
						if (parts.length >= 2 && !parts[1].isEmpty() && !parts[1].startsWith("$")) {
							count = (int) Math.min(Integer.MAX_VALUE, Math.max(1, Long.parseLong(parts[1])));
						} else if (itemId == 57 || itemId == 5575) {
							count = 10000000;
						}
						if (cmd.equals("give_item_to_all")) {
							for (var p : ctx.world().players()) {
								if (p.character() != null) {
									var added = ctx.inventories().addItem(p.character().inventory(), itemId, count,
											"AdminCreateAll");
									if (added != null) {
										p.send(new InventoryUpdate(List.of(ItemInfo.of(added.item(),
												added.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED))));
										if (p instanceof GameSession gs) {
											gs.refreshWeightAndPenalties();
										} else {
											p.send(new UserInfo(p.character(), ctx.characters().template(p.character())));
										}
									}
								}
							}
							send(new CreatureSay(0, CreatureSay.ALL, "SYS",
									"Item " + itemId + " x" + count + " entregue a todos os jogadores online."));
						} else {
							var targetPlayer = (targetObjectId != 0 && targetObjectId != active.objectId()
									&& !cmd.equals("create_item"))
											? ctx.world().player(targetObjectId).orElse(null)
											: null;
							var destChar = (targetPlayer != null && targetPlayer.character() != null)
									? targetPlayer.character()
									: active;
							var destPlayer = (targetPlayer != null) ? targetPlayer : this;

							var added = ctx.inventories().addItem(destChar.inventory(), itemId, count, "AdminCreate");
							if (added != null) {
								destPlayer.send(new InventoryUpdate(List.of(ItemInfo.of(added.item(),
										added.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED))));
								if (destPlayer instanceof GameSession gs) {
									gs.refreshWeightAndPenalties();
								} else {
									destPlayer.send(new UserInfo(destChar, ctx.characters().template(destChar)));
								}
								send(new CreatureSay(0, CreatureSay.ALL, "SYS",
										"Item " + itemId + " x" + count + " entregue a " + destChar.name()));
							} else {
								send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Falha ao criar item " + itemId));
							}
						}
					} catch (NumberFormatException e) {
						if (ctx.adminCommands() != null) {
							ctx.adminCommands().execute("find_item", this, args);
						} else {
							send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //item <itemId> [count] ou //item <nome>"));
						}
					}
				} else {
					showAdminHtml("menus/submenus/itemcreation_menu.htm");
				}
			}
			case "clean_inventory" -> {
				var inv = active.inventory();
				var toRemove = inv.items().stream().filter(it -> !it.isEquipped() && it.template().id() != 57).toList();
				for (var it : toRemove) {
					ctx.inventories().destroyItem(inv, it.objectId(), it.count(), "AdminClean");
				}
				send(ItemList.of(active.inventory().items(), true));
				send(new UserInfo(active, ctx.characters().template(active)));
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Inventario limpo com sucesso."));
			}
			case "spawn", "spawn_monster", "spawn_once", "cspawn" -> {
				String[] parts = args.split("\\s+");
				if (parts.length >= 1 && !parts[0].isEmpty() && !parts[0].equalsIgnoreCase("$id")
						&& !parts[0].equalsIgnoreCase("$qbox")) {
					int count = 1;
					if (parts.length >= 2 && !parts[1].isEmpty() && !parts[1].startsWith("$")) {
						count = Math.min(50, Math.max(1, parseIntSafe(parts[1], 1)));
					}
					int radius = 0;
					if (parts.length >= 3 && !parts[2].isEmpty() && !parts[2].startsWith("$")) {
						radius = Math.max(0, parseIntSafe(parts[2], 0));
					}
					boolean storeInDb = cmd.equals("spawn") || cmd.equals("cspawn");
					try {
						int npcId = Integer.parseInt(parts[0]);
						for (int i = 0; i < count; i++) {
							int sx = active.x() + (radius > 0 ? (int) ((Math.random() - 0.5) * 2 * radius) : 0);
							int sy = active.y() + (radius > 0 ? (int) ((Math.random() - 0.5) * 2 * radius) : 0);
							ctx.spawns().spawn(npcId, sx, sy, active.z(), active.heading(), storeInDb);
						}
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Spawned " + count + "x NPC id " + npcId + (storeInDb ? " (salvo no banco)" : "")));
						updateKnownObjects();
					} catch (NumberFormatException e) {
						String searchName = parts[0].replace('_', ' ').toLowerCase(java.util.Locale.ROOT);
						if (ctx.adminCommands() != null) {
							ctx.adminCommands().execute("find_npc", this, searchName);
						} else {
							send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //spawn <npcId> [count] ou //spawn <nome>"));
						}
					}
				} else {
					showAdminHtml("menus/submenus/spawn_menu.htm");
				}
			}
			case "delete", "del", "delete_npc", "unspawn" -> {
				if (targetObjectId != 0) {
					var npcOpt = ctx.world().npc(targetObjectId);
					if (npcOpt.isPresent()) {
						var npc = npcOpt.get();
						if (ctx.spawns() != null) {
							ctx.spawns().deleteSpawn(npc, true);
						} else {
							ctx.world().removeNpc(npc);
						}
						ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, new DeleteObject(npc.objectId()),
								false);
						send(new DeleteObject(npc.objectId()));
						targetObjectId = 0;
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"NPC " + npc.name() + " (id " + npc.npcId() + ") removido do mundo e do banco."));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "O alvo nao e um NPC valido."));
					}
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Selecione um NPC para deletar."));
				}
			}
			case "heal" -> {
				PlayerCharacter targetChar = active;
				GameWorld.OnlinePlayer targetPlayer = this;
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
							send(StatusUpdate.hp(npc.objectId(), (int) npc.currentHp(), npc.template().maxHp()));
							send(new CreatureSay(0, CreatureSay.ALL, "SYS", npc.name() + " totalmente curado."));
							return;
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
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", targetChar.name() + " totalmente curado."));
			}
			case "kill" -> {
				if (targetObjectId != 0) {
					var npcOpt = ctx.world().npc(targetObjectId);
					if (npcOpt.isPresent()) {
						var npc = npcOpt.get();
						npc.currentHp(0);
						if (ctx.npcAi() != null) {
							ctx.npcAi().stopCombat(npc);
							ctx.npcAi().scheduleDecayAndRespawn(npc);
						}
						var die = new Die(npc.objectId(), false);
						send(die);
						ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, die, false);
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", npc.name() + " foi morto."));
						return;
					}
					var playerOpt = ctx.world().player(targetObjectId);
					if (playerOpt.isPresent()) {
						var pOnline = playerOpt.get();
						if (pOnline.character() != null) {
							if (pOnline instanceof GameSession pSess) {
								pSess.handlePlayerDeath(active);
							} else {
								pOnline.character().currentHp(0);
								pOnline.send(new StatusUpdate(pOnline.objectId(), List.of(
										new StatusUpdate.Attribute(StatusUpdate.CUR_HP, 0))));
								var die = new Die(pOnline.objectId(), true);
								pOnline.send(die);
							}
							send(new CreatureSay(0, CreatureSay.ALL, "SYS", pOnline.name() + " foi morto."));
						}
						return;
					}
				}
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Selecione um alvo para matar."));
			}
			case "setlevel", "set_level", "level" -> {
				try {
					int newLevel = Integer.parseInt(args.trim().split("\\s+")[0]);
					if (newLevel < 1 || newLevel > 80) {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nivel deve ser entre 1 e 80."));
						return;
					}
					PlayerCharacter targetChar = active;
					GameWorld.OnlinePlayer targetPlayer = this;
					if (targetObjectId != 0 && targetObjectId != active.objectId()) {
						var other = ctx.world().player(targetObjectId).orElse(null);
						if (other != null && other.character() != null) {
							targetChar = other.character();
							targetPlayer = other;
						}
					}
					targetChar.level(newLevel);
					targetChar.exp(ExperienceTable.expForLevel(newLevel));
					var t = ctx.characters() != null ? ctx.characters().template(targetChar) : null;
					if (t != null) {
						if (targetPlayer instanceof GameSession gs) {
							gs.rewardSkills(t, true);
						}
						if (ctx.skillService() == null) {
							targetChar.maxHp(t.calculateMaxHp(newLevel));
							targetChar.maxMp(t.calculateMaxMp(newLevel));
							targetChar.maxCp(t.calculateMaxCp(newLevel));
						}
					}
					targetChar.currentHp(targetChar.maxHp());
					targetChar.currentMp(targetChar.maxMp());
					targetChar.currentCp(targetChar.maxCp());
					ctx.characters().save(targetChar, true);
					var social = new SocialAction(targetChar.objectId(), 15);
					targetPlayer.send(social);
					if (targetPlayer instanceof GameSession gs) {
						ctx.world().broadcastAround(gs, GameWorld.VISIBILITY_RADIUS, social, false);
					}
					targetPlayer.send(new UserInfo(targetChar, ctx.characters().template(targetChar)));
					targetPlayer.send(new StatusUpdate(targetChar.objectId(), List.of(
							new StatusUpdate.Attribute(StatusUpdate.LEVEL, targetChar.level()),
							new StatusUpdate.Attribute(StatusUpdate.CUR_HP, (int) targetChar.currentHp()),
							new StatusUpdate.Attribute(StatusUpdate.MAX_HP, targetChar.maxHp()),
							new StatusUpdate.Attribute(StatusUpdate.CUR_MP, (int) targetChar.currentMp()),
							new StatusUpdate.Attribute(StatusUpdate.MAX_MP, targetChar.maxMp()),
							new StatusUpdate.Attribute(StatusUpdate.CUR_CP, (int) targetChar.currentCp()),
							new StatusUpdate.Attribute(StatusUpdate.MAX_CP, targetChar.maxCp()))));
					send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Nivel de " + targetChar.name() + " alterado para " + newLevel));
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //level <1-80>"));
				}
			}
			case "setew", "enchant_weapon" -> {
				try {
					int val = args.isEmpty() || args.equalsIgnoreCase("$qbox") ? 16
							: Integer.parseInt(args.trim().split("\\s+")[0]);
					var weapon = active.inventory().paperdoll(ItemSlots.RHAND);
					if (weapon == null) {
						weapon = active.inventory().paperdoll(ItemSlots.LRHAND);
					}
					if (weapon != null) {
						weapon.enchant(val);
						ctx.inventories().saveItem(weapon);
						send(new InventoryUpdate(List.of(ItemInfo.of(weapon, ItemInfo.MODIFIED))));
						send(new UserInfo(active, ctx.characters().template(active)));
						broadcastAppearance();
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Arma encantada para +" + val));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nenhuma arma equipada na mao direita."));
					}
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //setew <enchantLevel>"));
				}
			}
			case "setec", "enchant_armor" -> {
				try {
					int val = args.isEmpty() || args.equalsIgnoreCase("$qbox") ? 16
							: Integer.parseInt(args.trim().split("\\s+")[0]);
					int[] armorSlots = { ItemSlots.CHEST, ItemSlots.LEGS, ItemSlots.HEAD, ItemSlots.GLOVES,
							ItemSlots.FEET };
					List<ItemInfo> modified = new ArrayList<>();
					for (int slot : armorSlots) {
						var piece = active.inventory().paperdoll(slot);
						if (piece != null) {
							piece.enchant(val);
							ctx.inventories().saveItem(piece);
							modified.add(ItemInfo.of(piece, ItemInfo.MODIFIED));
						}
					}
					if (!modified.isEmpty()) {
						send(new InventoryUpdate(modified));
						send(new UserInfo(active, ctx.characters().template(active)));
						broadcastAppearance();
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Armaduras encantadas para +" + val));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nenhuma armadura equipada."));
					}
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //setec <enchantLevel>"));
				}
			}
			case "setej", "enchant_jewel" -> {
				try {
					int val = args.isEmpty() || args.equalsIgnoreCase("$qbox") ? 16
							: Integer.parseInt(args.trim().split("\\s+")[0]);
					int[] jewelSlots = { ItemSlots.NECK, ItemSlots.REAR, ItemSlots.LEAR, ItemSlots.RFINGER,
							ItemSlots.LFINGER };
					List<ItemInfo> modified = new ArrayList<>();
					for (int slot : jewelSlots) {
						var piece = active.inventory().paperdoll(slot);
						if (piece != null) {
							piece.enchant(val);
							ctx.inventories().saveItem(piece);
							modified.add(ItemInfo.of(piece, ItemInfo.MODIFIED));
						}
					}
					if (!modified.isEmpty()) {
						send(new InventoryUpdate(modified));
						send(new UserInfo(active, ctx.characters().template(active)));
						broadcastAppearance();
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Joias encantadas para +" + val));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nenhuma joia equipada."));
					}
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //setej <enchantLevel>"));
				}
			}
			case "enchant_all", "setall" -> {
				try {
					int val = args.isEmpty() || args.equalsIgnoreCase("$qbox") ? 20
							: Integer.parseInt(args.trim().split("\\s+")[0]);
					List<ItemInfo> modified = new ArrayList<>();
					for (var item : active.inventory().equipped()) {
						if (item != null) {
							item.enchant(val);
							ctx.inventories().saveItem(item);
							modified.add(ItemInfo.of(item, ItemInfo.MODIFIED));
						}
					}
					if (!modified.isEmpty()) {
						send(new InventoryUpdate(modified));
						send(new UserInfo(active, ctx.characters().template(active)));
						broadcastAppearance();
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Todos os itens equipados foram encantados para +" + val));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nenhum item equipado para encantar."));
					}
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //enchant_all <nivel>"));
				}
			}
			case "show_droplist", "droplist" -> {
				int mobId = 0;
				if (!args.isEmpty()) {
					mobId = parseIntSafe(args.trim().split("\\s+")[0], 0);
				} else if (targetObjectId != 0) {
					var npc = ctx.world().npc(targetObjectId).orElse(null);
					if (npc != null) {
						mobId = npc.npcId();
					}
				}
				if (mobId > 0) {
					showAdminDropList(mobId);
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Uso: //droplist <npcId> ou selecione um monstro."));
				}
			}
			case "edit_npc", "npcinfo", "npc_info" -> {
				NpcInstance targetNpc = null;
				if (!args.isEmpty()) {
					int npcId = parseIntSafe(args.trim().split("\\s+")[0], 0);
					targetNpc = ctx.world().npcs().stream().filter(n -> n.npcId() == npcId).findFirst().orElse(null);
				} else if (targetObjectId != 0) {
					targetNpc = ctx.world().npc(targetObjectId).orElse(null);
				}
				if (targetNpc != null) {
					showAdminNpcInfo(targetNpc);
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Selecione um NPC ou use //edit_npc <npcId>"));
				}
			}
			case "pledge", "clan" -> {
				String[] parts = args.split("\\s+");
				if (parts.length >= 2) {
					String action = parts[0].toLowerCase(java.util.Locale.ROOT);
					String targetName = parts[1];
					switch (action) {
						case "create" -> createClan(targetName);
						case "dismiss" -> dissolveClan();
						case "setlevel" -> {
							int lvl = parts.length >= 3 ? parseIntSafe(parts[2], 5) : 5;
							if (active.clanId() > 0 && ctx.clans() != null) {
								ctx.clans().updateClanLevel(active.clanId(), lvl);
								send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nivel do cla definido para " + lvl));
							}
						}
						case "rep" -> {
							int rep = parts.length >= 3 ? parseIntSafe(parts[2], 10000) : 10000;
							if (active.clanId() > 0 && ctx.clans() != null) {
								var clan = ctx.clans().byClanId(active.clanId()).orElse(null);
								if (clan != null) {
									clan.reputationScore(clan.reputationScore() + rep);
									send(new CreatureSay(0, CreatureSay.ALL, "SYS",
											"Reputacao do cla adicionada: " + rep));
								}
							}
						}
					}
				} else {
					showAdminHtml("menus/game.htm");
				}
			}
			case "siege", "castles", "castle" -> showAdminCastlesHtml(args);
			case "setcastle" -> {
				if (!args.isBlank() && ctx.castles() != null) {
					String cName = args.trim().split("\\s+")[0];
					var castle = ctx.castles().byName(cName).orElse(null);
					var target = getTargetPlayerOrActive();
					if (castle != null && target != null) {
						ctx.castles().setOwner(castle.id(), target.clanId());
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Castelo " + castle.name() + " atribuido ao cla " + target.clanId() + " (" + target.name() + ")"));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Castelo ou jogador invalido."));
					}
				} else {
					showAdminCastlesHtml("");
				}
			}
			case "startsiege" -> send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Siege iniciada: " + args));
			case "endsiege" -> send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Siege finalizada: " + args));
			case "mammon_find" -> {
				String param = args.trim().toLowerCase(java.util.Locale.ROOT);
				boolean isBlacksmith = param.equals("1") || param.contains("black") || param.contains("bs");
				int targetNpcId = isBlacksmith
						? com.lopez.l2j.game.sevensigns.SevenSignsManager.MAMMON_BLACKSMITH_ID
						: com.lopez.l2j.game.sevensigns.SevenSignsManager.MAMMON_MERCHANT_ID;
				String name = isBlacksmith ? "Blacksmith of Mammon" : "Merchant of Mammon";

				var loc = isBlacksmith
						? (ctx.sevenSigns() != null ? ctx.sevenSigns().getMammonBlacksmithLocation() : null)
						: (ctx.sevenSigns() != null ? ctx.sevenSigns().getMammonMerchantLocation() : null);

				var npc = ctx.world().npcs().stream().filter(n -> n.npcId() == targetNpcId).findFirst().orElse(null);
				if (npc != null) {
					teleportToLocation(npc.x(), npc.y(), npc.z());
					send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Teleportado para " + npc.name() + " em " + npc.x() + ", " + npc.y() + ", " + npc.z()));
				} else if (loc != null) {
					teleportToLocation(loc.x(), loc.y(), loc.z());
					send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Teleportado para spawn de " + name + " (" + loc.dungeonName() + ") em " + loc.x() + ", " + loc.y() + ", " + loc.z()));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							name + " (ID " + targetNpcId + ") nao encontrado ativo no mundo."));
				}
			}
			case "ssq", "sevensigns" -> {
				if (ctx.sevenSigns() == null) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "SevenSignsManager indisponivel."));
					break;
				}
				var ss = ctx.sevenSigns();
				String[] parts = args.trim().split("\\s+");
				String sub = parts.length > 0 ? parts[0].toLowerCase(java.util.Locale.ROOT) : "";
				switch (sub) {
					case "change", "advance" -> {
						ss.changePeriodManually();
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Seven Signs: Periodo avancado para " + ss.activePeriod() + " (Ciclo " + ss.currentCycle() + ")."));
					}
					case "set_period" -> {
						if (parts.length >= 2) {
							int p = parseIntSafe(parts[1], 0);
							ss.changePeriodManually(p);
							send(new CreatureSay(0, CreatureSay.ALL, "SYS",
									"Seven Signs: Periodo alterado para " + ss.activePeriod() + "."));
						} else {
							send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //ssq set_period <0-3>"));
						}
					}
					case "set_cycle" -> {
						if (parts.length >= 2) {
							int c = parseIntSafe(parts[1], 1);
							ss.currentCycle(c);
							ss.persistStatus();
							send(new CreatureSay(0, CreatureSay.ALL, "SYS",
									"Seven Signs: Ciclo alterado para " + c + "."));
						} else {
							send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //ssq set_cycle <numero>"));
						}
					}
					case "set_seal" -> {
						if (parts.length >= 3) {
							int seal = parseIntSafe(parts[1], 1);
							int cabal = parseIntSafe(parts[2], 0);
							ss.setSealOwner(seal, cabal);
							ss.persistStatus();
							send(new CreatureSay(0, CreatureSay.ALL, "SYS",
									"Seven Signs: Selo " + seal + " definido para Cabal " + cabal + "."));
						} else {
							send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //ssq set_seal <1-3> <0=None, 1=Dusk, 2=Dawn>"));
						}
					}
					case "sky" -> {
						ss.broadcastSky();
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Seven Signs: Sky state atualizado para todos."));
					}
					default -> {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								String.format("Seven Signs Status: Ciclo=%d, Periodo=%d, DawnScore=%d, DuskScore=%d, Winner=%d",
										ss.currentCycle(), ss.activePeriod(), ss.getCurrentScore(com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN),
										ss.getCurrentScore(com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK), ss.previousWinner())));
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								String.format("Selos: Avarice=%d, Gnosis=%d, Strife=%d",
										ss.getSealOwner(com.lopez.l2j.game.sevensigns.SevenSignsManager.SEAL_AVARICE),
										ss.getSealOwner(com.lopez.l2j.game.sevensigns.SevenSignsManager.SEAL_GNOSIS),
										ss.getSealOwner(com.lopez.l2j.game.sevensigns.SevenSignsManager.SEAL_STRIFE))));
					}
				}
			}
			case "cleanup", "clean_up" -> {
				long before = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
				System.gc();
				long after = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
				long freedMb = Math.max(0, (before - after) / (1024 * 1024));
				send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Memoria liberada: " + freedMb + " MB. Coleta de lixo concluida."));
			}
			case "shutdown", "server_shutdown" -> {
				int sec = parseIntSafe(args.isEmpty() || args.equalsIgnoreCase("$qbox") ? "60" : args.trim().split("\\s+")[0], 60);
				var svc = com.lopez.l2j.game.service.ShutdownService.getInstance();
				if (svc != null) {
					svc.startShutdown(sec, com.lopez.l2j.game.service.ShutdownService.Mode.SHUTDOWN, active != null ? active.name() : "Admin");
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Shutdown agendado para " + sec + " segundos."));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "ShutdownService nao inicializado."));
				}
			}
			case "restart", "server_restart" -> {
				int sec = parseIntSafe(args.isEmpty() || args.equalsIgnoreCase("$qbox") ? "60" : args.trim().split("\\s+")[0], 60);
				var svc = com.lopez.l2j.game.service.ShutdownService.getInstance();
				if (svc != null) {
					svc.startShutdown(sec, com.lopez.l2j.game.service.ShutdownService.Mode.RESTART, active != null ? active.name() : "Admin");
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Restart agendado para " + sec + " segundos."));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "ShutdownService nao inicializado."));
				}
			}
			case "abort", "server_abort" -> {
				var svc = com.lopez.l2j.game.service.ShutdownService.getInstance();
				if (svc != null) {
					boolean ok = svc.abort(active != null ? active.name() : "Admin");
					if (ok) {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Desligamento/restart abortado com sucesso."));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nenhum desligamento ou restart em andamento."));
					}
				}
			}
			case "onlygm", "server_onlygm" -> {
				var svc = com.lopez.l2j.game.service.ShutdownService.getInstance();
				if (svc != null) {
					svc.setOnlyGm(true);
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Modo Apenas GM ATIVADO (Acesso restrito para manutencao)."));
				}
			}
			case "forall", "server_forall" -> {
				var svc = com.lopez.l2j.game.service.ShutdownService.getInstance();
				if (svc != null) {
					svc.setOnlyGm(false);
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Modo Apenas GM DESATIVADO (Servidor aberto para todos os jogadores)."));
				}
			}
			case "maxplayer", "set_maxplayer" -> {
				int max = parseIntSafe(args.trim().split("\\s+")[0], 1000);
				Config.MAXIMUM_ONLINE_USERS = max;
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Capacidade maxima de jogadores alterada para: " + max));
			}
			case "setparam" -> {
				String[] parts = args.split("\\s+");
				if (parts.length >= 2) {
					String param = parts[0].toLowerCase(java.util.Locale.ROOT);
					try {
						int val = Integer.parseInt(parts[1]);
						var destChar = getTargetPlayerOrActive();
						switch (param) {
							case "hp", "maxhp" -> destChar.maxHp(val);
							case "mp", "maxmp" -> destChar.maxMp(val);
							case "cp", "maxcp" -> destChar.maxCp(val);
							case "speed", "runspeed" -> destChar.gmSpeed(Math.min(5, Math.max(0, val)));
						}
						destChar.currentHp(destChar.maxHp());
						destChar.currentMp(destChar.maxMp());
						destChar.currentCp(destChar.maxCp());
						ctx.characters().save(destChar, true);
						var destPlayer = getTargetOnlinePlayerOrSelf();
						destPlayer.send(new UserInfo(destChar, ctx.characters().template(destChar)));
						destPlayer.send(
								StatusUpdate.hp(destChar.objectId(), (int) destChar.currentHp(), destChar.maxHp()));
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Parametro " + param + " de " + destChar.name() + " alterado para " + val));
					} catch (Exception e) {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //setparam <hp|mp|cp|speed> <valor>"));
					}
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //setparam <hp|mp|cp|speed> <valor>"));
				}
			}
			case "announce", "announce_add" -> {
				if (!args.isEmpty() && !args.equalsIgnoreCase("$new_announcement")) {
					String sender = Config.ANNOUNCE_GM_NAME ? active.name() : "";
					ctx.world().broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, sender, args), x -> true);
				} else {
					showAdminHtml("menus/submenus/announce_menu.htm");
				}
			}
			case "announce_critical", "critical_announce" -> {
				if (!args.isEmpty() && !args.equalsIgnoreCase("$new_announcement")) {
					String sender = Config.ANNOUNCE_GM_NAME ? active.name() : "";
					ctx.world().broadcast(new CreatureSay(0, CreatureSay.CRITICAL_ANNOUNCE, sender, args), x -> true);
				} else {
					showAdminHtml("menus/submenus/announce_menu.htm");
				}
			}
			case "announce_refresh" -> showAdminHtml("menus/submenus/announce_menu.htm");
			case "open" -> {
				try {
					int doorId = Integer.parseInt(args.trim().split("\\s+")[0]);
					if (ctx.doors() != null) {
						var door = ctx.doors().getDoor(doorId);
						if (door != null) {
							door.open();
							ctx.world().broadcast(new DoorStatusUpdate(door), p -> true);
							send(new CreatureSay(0, CreatureSay.ALL, "SYS",
									"Porta " + doorId + " (" + door.name() + ") aberta."));
						} else {
							send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Porta " + doorId + " nao encontrada."));
						}
					}
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //open <doorId>"));
				}
			}
			case "close" -> {
				try {
					int doorId = Integer.parseInt(args.trim().split("\\s+")[0]);
					if (ctx.doors() != null) {
						var door = ctx.doors().getDoor(doorId);
						if (door != null) {
							door.close();
							ctx.world().broadcast(new DoorStatusUpdate(door), p -> true);
							send(new CreatureSay(0, CreatureSay.ALL, "SYS",
									"Porta " + doorId + " (" + door.name() + ") fechada."));
						} else {
							send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Porta " + doorId + " nao encontrada."));
						}
					}
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //close <doorId>"));
				}
			}
			case "invul", "undying" -> {
				active.invul(!active.invul());
				active.currentHp(active.maxHp());
				active.currentCp(active.maxCp());
				active.currentMp(active.maxMp());
				send(new StatusUpdate(active.objectId(), List.of(
						new StatusUpdate.Attribute(StatusUpdate.CUR_HP, (int) active.currentHp()),
						new StatusUpdate.Attribute(StatusUpdate.MAX_HP, active.maxHp()),
						new StatusUpdate.Attribute(StatusUpdate.CUR_MP, (int) active.currentMp()),
						new StatusUpdate.Attribute(StatusUpdate.MAX_MP, active.maxMp()),
						new StatusUpdate.Attribute(StatusUpdate.CUR_CP, (int) active.currentCp()),
						new StatusUpdate.Attribute(StatusUpdate.MAX_CP, active.maxCp()))));
				send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Invulnerabilidade GM: " + (active.invul() ? "ATIVADA (Dano zero)" : "DESATIVADA")));
			}
			case "goname", "goto" -> {
				if (!args.isEmpty()) {
					var target = ctx.world().byName(args).orElse(null);
					if (target != null) {
						teleportToLocation(target.x(), target.y(), target.z());
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Teleportado para " + target.name()));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Jogador '" + args + "' nao encontrado ou offline."));
					}
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //goname <nomeDoPlayer>"));
				}
			}
			case "recall" -> {
				if (!args.isEmpty()) {
					var target = ctx.world().byName(args).orElse(null);
					if (target instanceof GameSession s) {
						s.teleportToLocation(active.x(), active.y(), active.z());
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Jogador " + target.name() + " puxado para sua posicao."));
					} else if (target != null && target.character() != null) {
						target.character().moveTo(active.x(), active.y(), active.z());
						target.send(new TeleportToLocation(target.objectId(), active.x(), active.y(), active.z()));
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Jogador " + target.name() + " puxado para sua posicao."));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Jogador '" + args + "' nao encontrado ou offline."));
					}
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //recall <nomeDoPlayer>"));
				}
			}
			case "setadmin" -> {
				String[] parts = args.split("\\s+");
				if (parts.length >= 1 && !parts[0].isEmpty()) {
					String targetName = parts[0];
					int level = parts.length >= 2 ? Integer.parseInt(parts[1]) : 100;
					ctx.characters().setAccessLevelByName(targetName, level);
					var target = ctx.world().byName(targetName).orElse(null);
					if (target != null && target.character() != null) {
						target.character().accessLevel(level);
						target.send(new UserInfo(target.character(), ctx.characters().template(target.character())));
						target.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Seu nivel de acesso administrativo foi atualizado para: " + level));
					}
					send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"AccessLevel de " + targetName + " definido para " + level + " no banco de dados."));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //setadmin <NomeDoPersonagem> [nivel]"));
				}
			}
			default -> {
				String resolved = resolveAdminHtml(cmd);
				if (resolved != null) {
					showAdminHtml(cmd);
				} else if (!args.isEmpty()) {
					resolved = resolveAdminHtml(cmd + "/" + args);
					if (resolved != null) {
						showAdminHtml(cmd + "/" + args);
						return;
					}
					resolved = resolveAdminHtml(args);
					if (resolved != null) {
						showAdminHtml(args);
						return;
					}
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Comando admin desconhecido: " + cmd));
					showAdminHtml("menus/main.htm");
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Comando admin desconhecido: " + cmd));
					showAdminHtml("menus/main.htm");
				}
			}
		}
	}

	@Override
	public void send(GameServerPacket packet) {
		if (packet instanceof NpcHtmlMessage htmlMsg && ctx.bypassEncoder() != null) {
			String encoded = ctx.bypassEncoder().encodeHtml(htmlMsg.html(), sessionBypasses, false);
			sink.accept(new NpcHtmlMessage(htmlMsg.npcObjectId(), encoded, htmlMsg.itemId()));
			return;
		}
		if (packet instanceof CreatureSay say && say.channel() == CreatureSay.ALL && say.objectId() == 0) {
			String name = say.name();
			String text = say.text();
			if (name == null || name.isBlank() || "SYS".equalsIgnoreCase(name) || "System".equalsIgnoreCase(name)) {
				sink.accept(SystemMessage.sendString(text));
			} else {
				sink.accept(SystemMessage.sendString("[" + name + "] " + text));
			}
			return;
		}
		sink.accept(packet);
	}

	@Override
	public void sendMessage(String text) {
		if (text != null && !text.isBlank()) {
			send(SystemMessage.sendString(text));
		}
	}

	public void onEnchantItem(RequestEnchantItem p) {
		itemHandler.handleEnchantItem(p);
	}

	public void onWareHouseDeposit(SendWareHouseDepositList p) {
		tradeStoreHandler.handleWareHouseDeposit(p);
	}

	public void onWareHouseWithdraw(SendWareHouseWithDrawList p) {
		tradeStoreHandler.handleWareHouseWithdraw(p);
	}

	public void onDestroyItem(RequestDestroyItem p) {
		itemHandler.handleDestroyItem(p);
	}

	public GameServerPacket charInfo() {
		if (active == null) {
			return null;
		}
		var t = ctx.characters().template(active);
		int crestId = 0, allyId = 0, allyCrestId = 0;
		if (ctx.clans() != null && active.clanId() != 0) {
			var cl = ctx.clans().byClanId(active.clanId()).orElse(null);
			if (cl != null) {
				crestId = cl.crestId();
				allyId = cl.allyId();
				allyCrestId = cl.allyCrestId();
			}
		}
		return new CharInfo(active, t, active.inventory().paperdollView(), crestId, allyId, allyCrestId);
	}

	public void onPledgeCrest(GameClientPacket.RequestPledgeCrest p) {
		partyClanHandler.handlePledgeCrest(p);
	}

	public void onPledgeInfo(GameClientPacket.RequestPledgeInfo p) {
		partyClanHandler.handlePledgeInfo(p);
	}

	public void onPledgeMemberList() {
		partyClanHandler.handlePledgeMemberList();
	}

	public void onSetPledgeCrest(GameClientPacket.RequestSetPledgeCrest p) {
		partyClanHandler.handleSetPledgeCrest(p);
	}

	public void createClan(String clanName) {
		if (ctx.clans() == null || active == null) {
			return;
		}
		if (active.clanId() != 0) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce ja pertence a um cla."));
			return;
		}
		if (active.level() < 10 && !active.isGm()) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nivel 10 ou superior necessario para criar cla."));
			return;
		}
		var clan = ctx.clans().createClan(active, clanName);
		if (clan == null) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nome de cla invalido ou ja existente."));
			return;
		}
		send(new PledgeShowInfoUpdate(clan));
		send(new PledgeShowMemberListAll(clan, 0));
		send(new PledgeShowMemberListUpdate(active.name(), active.level(), active.classId(), true));
		var tpl = ctx.characters() != null ? ctx.characters().template(active) : null;
		if (tpl != null) {
			send(new UserInfo(active, tpl));
		}
		broadcastAppearance();
		send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Cla " + clan.name() + " criado com sucesso!"));
	}

	public void increaseClanLevel() {
		if (ctx.clans() == null || active == null) {
			send(new ActionFailed());
			return;
		}
		if (active.clanId() == 0) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao pertence a um cla."));
			return;
		}
		var clanOpt = ctx.clans().byClanId(active.clanId());
		if (clanOpt.isEmpty() || !clanOpt.get().isLeader(active.objectId())) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Apenas o lider do cla pode aumentar seu nivel."));
			return;
		}
		var clan = clanOpt.get();
		if (clan.level() >= 8) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "O cla ja alcancou o nivel maximo (8)."));
			return;
		}
		boolean success = ctx.clans().levelUpClan(active);
		if (success) {
			send(new SocialAction(active.objectId(), 15));
			send(ItemList.of(active.inventory().items(), false));
			send(new StatusUpdate(active.objectId(),
					List.of(new StatusUpdate.Attribute(StatusUpdate.SP, active.sp()))));
			var tpl = ctx.characters() != null ? ctx.characters().template(active) : null;
			if (tpl != null) {
				send(new UserInfo(active, tpl));
			}
			broadcastAppearance();
			clan.broadcastToOnlineMembers(ctx.world(), new PledgeShowInfoUpdate(clan));
			send(new CreatureSay(0, CreatureSay.ALL, "SYS",
					"Parabens! O nivel do cla subiu para " + clan.level() + "!"));
		} else {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS",
					"Falha ao aumentar o nivel do cla. Requisitos nao atendidos."));
		}
	}

	public void dissolveClan() {
		if (ctx.clans() == null || active == null) {
			send(new ActionFailed());
			return;
		}
		if (active.clanId() == 0) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao pertence a um cla."));
			return;
		}
		var clanOpt = ctx.clans().byClanId(active.clanId());
		if (clanOpt.isEmpty() || !clanOpt.get().isLeader(active.objectId())) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Apenas o lider pode dissolver o cla."));
			return;
		}
		int oldClanId = active.clanId();
		ctx.clans().dissolveClan(oldClanId);
		active.clanId(0);
		var tpl = ctx.characters() != null ? ctx.characters().template(active) : null;
		if (tpl != null) {
			send(new UserInfo(active, tpl));
		}
		broadcastAppearance();
		send(new CreatureSay(0, CreatureSay.ALL, "SYS", "O cla foi dissolvido."));
	}

	public int getClassLevel(PlayerCharacter player) {
		if (player == null || ctx.characters() == null) {
			return 0;
		}
		var tpl = ctx.characters().template(player);
		return tpl != null ? tpl.classTier() : 0;
	}

	private void loadHennasForCurrentSubclass() {
		if (active == null) {
			return;
		}
		active.clearHennas();
		if (ctx.characterHennas() != null) {
			var hennas = ctx.characterHennas().restoreHennas(active.objectId(), active.classIndex());
			for (var entry : hennas.entrySet()) {
				int slot = entry.getKey();
				int symbolId = entry.getValue();
				if (slot >= 1 && slot <= 3) {
					active.setHenna(slot, symbolId);
				}
			}
		}
		active.recalcHennaStats(ctx.hennas(), ctx.hennaTrees());
	}

	private void onHennaList() {
		if (active == null || ctx.hennaTrees() == null || ctx.hennas() == null) {
			send(new ActionFailed());
			return;
		}
		int classLevel = getClassLevel(active);
		int emptySlots = active.getHennaEmptySlots(classLevel);
		var inv = active.inventory();
		long adena = inv != null ? inv.adena() : 0;
		var availableTree = ctx.hennaTrees().getAvailableHennas(active.classId());
		var list = new java.util.ArrayList<com.lopez.l2j.game.henna.Henna>();
		if (inv != null) {
			for (var h : availableTree) {
				if (inv.getItemCount(h.dyeId()) > 0) {
					list.add(h);
				}
			}
		}
		send(new HennaEquipList((int) adena, emptySlots, list));
	}

	private void onHennaItemInfo(int symbolId) {
		if (active == null || ctx.hennas() == null) {
			send(new ActionFailed());
			return;
		}
		var h = ctx.hennas().get(symbolId);
		if (h == null) {
			send(new ActionFailed());
			return;
		}
		var tpl = ctx.characters() != null ? ctx.characters().template(active) : null;
		send(new HennaItemInfo(h, active, tpl));
	}

	private void onHennaEquip(int symbolId) {
		if (active == null || ctx.hennas() == null) {
			send(new ActionFailed());
			return;
		}
		if (active.isOlympiadMode()) {
			send(SystemMessage.id(SystemMessage.THE_SYMBOL_CANNOT_BE_DRAWN));
			return;
		}
		var h = ctx.hennas().get(symbolId);
		if (h == null) {
			send(SystemMessage.id(SystemMessage.THE_SYMBOL_CANNOT_BE_DRAWN));
			return;
		}
		if (ctx.hennaTrees() != null && !ctx.hennaTrees().isAllowed(active.classId(), symbolId)) {
			send(SystemMessage.id(SystemMessage.THE_SYMBOL_CANNOT_BE_DRAWN));
			return;
		}
		int classLevel = getClassLevel(active);
		if (active.getHennaEmptySlots(classLevel) == 0) {
			send(SystemMessage.id(SystemMessage.NO_SLOT_EXISTS_TO_DRAW_THE_SYMBOL));
			return;
		}
		var inv = active.inventory();
		if (inv == null || inv.adena() < h.price() || inv.getItemCount(h.dyeId()) < h.dyeAmount()) {
			send(SystemMessage.id(SystemMessage.THE_SYMBOL_CANNOT_BE_DRAWN));
			return;
		}

		int maxSlots = active.getMaxHennaSlots(classLevel);
		int targetSlot = -1;
		for (int i = 1; i <= maxSlots; i++) {
			if (active.getHenna(i) == 0) {
				targetSlot = i;
				break;
			}
		}
		if (targetSlot == -1) {
			send(SystemMessage.id(SystemMessage.NO_SLOT_EXISTS_TO_DRAW_THE_SYMBOL));
			return;
		}

		inv.destroyItemByItemId(h.dyeId(), h.dyeAmount());
		inv.destroyItemByItemId(ItemTemplate.ADENA_ID, h.price());

		active.setHenna(targetSlot, symbolId);
		if (ctx.characterHennas() != null) {
			ctx.characterHennas().saveHenna(active.objectId(), active.classIndex(), targetSlot, symbolId);
		}

		active.recalcHennaStats(ctx.hennas(), ctx.hennaTrees());
		var tpl = ctx.characters() != null ? ctx.characters().template(active) : null;
		if (tpl != null) {
			recalcMaxVitals(tpl);
		}

		send(SystemMessage.id(SystemMessage.THE_SYMBOL_HAS_BEEN_ADDED));
		send(new HennaInfo(active, classLevel, ctx.hennaTrees()));
		if (tpl != null) {
			send(new UserInfo(active, tpl));
		}
		send(ItemList.of(inv.items(), false));
	}

	private void onHennaUnequipList() {
		if (active == null || ctx.hennas() == null) {
			send(new ActionFailed());
			return;
		}
		int classLevel = getClassLevel(active);
		int emptySlots = active.getHennaEmptySlots(classLevel);
		var inv = active.inventory();
		long adena = inv != null ? inv.adena() : 0;

		var list = new java.util.ArrayList<com.lopez.l2j.game.henna.Henna>();
		int maxSlots = active.getMaxHennaSlots(classLevel);
		for (int i = 1; i <= maxSlots; i++) {
			int symbolId = active.getHenna(i);
			if (symbolId > 0) {
				var h = ctx.hennas().get(symbolId);
				if (h != null) {
					list.add(h);
				}
			}
		}
		send(new HennaUnequipList((int) adena, emptySlots, list));
	}

	private void onHennaUnequipInfo(int symbolId) {
		if (active == null || ctx.hennas() == null) {
			send(new ActionFailed());
			return;
		}
		var h = ctx.hennas().get(symbolId);
		if (h == null) {
			send(new ActionFailed());
			return;
		}
		var tpl = ctx.characters() != null ? ctx.characters().template(active) : null;
		send(new HennaUnequipInfo(h, active, tpl));
	}

	private void onHennaUnequip(int symbolId) {
		if (active == null || ctx.hennas() == null) {
			send(new ActionFailed());
			return;
		}
		if (active.isOlympiadMode()) {
			return;
		}
		int classLevel = getClassLevel(active);
		int maxSlots = active.getMaxHennaSlots(classLevel);
		int targetSlot = -1;
		for (int i = 1; i <= maxSlots; i++) {
			if (active.getHenna(i) == symbolId) {
				targetSlot = i;
				break;
			}
		}
		if (targetSlot == -1) {
			send(new ActionFailed());
			return;
		}
		onHennaRemoveBySlot(targetSlot);
	}

	private void onHennaRemoveBySlot(int slot) {
		if (active == null || ctx.hennas() == null) {
			send(new ActionFailed());
			return;
		}
		if (active.isOlympiadMode()) {
			return;
		}
		if (slot < 1 || slot > 3) {
			send(new ActionFailed());
			return;
		}
		int symbolId = active.getHenna(slot);
		if (symbolId <= 0) {
			send(new ActionFailed());
			return;
		}
		var h = ctx.hennas().get(symbolId);
		if (h == null) {
			send(new ActionFailed());
			return;
		}

		var inv = active.inventory();
		int fee = h.price() / 5;
		if (inv == null || inv.adena() < fee) {
			send(SystemMessage.id(SystemMessage.YOU_NOT_ENOUGH_ADENA));
			return;
		}

		inv.destroyItemByItemId(ItemTemplate.ADENA_ID, fee);

		int refundDyes = h.dyeAmount() / 2;
		if (refundDyes > 0 && ctx.inventories() != null) {
			ctx.inventories().addItem(inv, h.dyeId(), refundDyes, "HennaRefund");
		}

		active.setHenna(slot, 0);

		if (ctx.characterHennas() != null) {
			ctx.characterHennas().deleteHenna(active.objectId(), active.classIndex(), slot);
		}

		active.recalcHennaStats(ctx.hennas(), ctx.hennaTrees());
		var tpl = ctx.characters() != null ? ctx.characters().template(active) : null;
		if (tpl != null) {
			recalcMaxVitals(tpl);
		}

		int classLevel = getClassLevel(active);
		send(SystemMessage.id(SystemMessage.THE_SYMBOL_HAS_BEEN_DELETED));
		send(new HennaInfo(active, classLevel, ctx.hennaTrees()));
		if (tpl != null) {
			send(new UserInfo(active, tpl));
		}
		send(ItemList.of(inv.items(), false));
	}

	private void onCursedWeaponList() {
		if (active == null) {
			send(new ActionFailed());
			return;
		}
		if (ctx.cursedWeapons() == null) {
			send(new GameServerPacket.ExCursedWeaponList(java.util.List.of()));
			return;
		}
		var ids = ctx.cursedWeapons().allWeapons().stream()
				.map(com.lopez.l2j.game.cursed.CursedWeapon::itemId)
				.toList();
		send(new GameServerPacket.ExCursedWeaponList(ids));
	}

	private void onCursedWeaponLocation() {
		if (active == null) {
			send(new ActionFailed());
			return;
		}
		if (ctx.cursedWeapons() == null) {
			send(new GameServerPacket.ExCursedWeaponLocation(java.util.List.of()));
			return;
		}
		var activeOrDropped = ctx.cursedWeapons().activeOrDroppedWeapons();
		var list = new java.util.ArrayList<GameServerPacket.CursedWeaponLocationInfo>();
		for (var cw : activeOrDropped) {
			int status = cw.isActive() ? 1 : 0;
			int x = cw.x();
			int y = cw.y();
			int z = cw.z();
			if (cw.isActive() && ctx.world() != null) {
				var carrier = ctx.world().player(cw.playerId()).orElse(null);
				if (carrier != null) {
					x = carrier.x();
					y = carrier.y();
					z = carrier.z();
				}
			}
			list.add(new GameServerPacket.CursedWeaponLocationInfo(cw.itemId(), status, x, y, z));
		}
		send(new GameServerPacket.ExCursedWeaponLocation(list));
	}

	public void onSSQStatus(int page) {
		if (active == null) {
			send(new ActionFailed());
			return;
		}
		if (ctx.sevenSigns() == null) {
			send(new GameServerPacket.SSQStatus(page, 0, 1, 0, 0, 0, 0, 0, 0));
			return;
		}
		send(GameServerPacket.SSQStatus.of(page, ctx.sevenSigns(), active.objectId()));
	}

	@Override
	public int objectId() {
		return active == null ? 0 : active.objectId();
	}

	@Override
	public String name() {
		return active == null ? "" : active.name();
	}

	@Override
	public int x() {
		return active == null ? 0 : active.x();
	}

	@Override
	public int y() {
		return active == null ? 0 : active.y();
	}

	@Override
	public int z() {
		return active == null ? 0 : active.z();
	}

	@Override
	public PlayerCharacter character() {
		return active;
	}

	private void onEvaluate(GameClientPacket.RequestEvaluate p) {
		if (!inWorld || active == null) {
			send(new ActionFailed());
			return;
		}
		var targetPlayer = ctx.world().player(p.targetId()).orElse(null);
		if (targetPlayer == null || targetPlayer.character() == null) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Alvo incorreto."));
			send(new ActionFailed());
			return;
		}
		if (ctx.recommendations() != null) {
			var result = ctx.recommendations().evaluate(active, targetPlayer.character());
			if (result.success()) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce recomendou " + targetPlayer.name() + ". Restam "
						+ result.actorRecomLeft() + " recomendacoes hoje."));
				targetPlayer.send(
						new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce foi recomendado por " + active.name() + "!"));
				send(new UserInfo(active, ctx.characters().template(active)));
				targetPlayer.send(
						new UserInfo(targetPlayer.character(), ctx.characters().template(targetPlayer.character())));
				ctx.world().broadcastAround(targetPlayer, GameWorld.VISIBILITY_RADIUS,
						new CharInfo(targetPlayer.character(), ctx.characters().template(targetPlayer.character())),
						false);
			} else {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", result.message()));
			}
		}
		send(new ActionFailed());
	}

	public void onFriendList() {
		if (!inWorld || active == null || ctx.friends() == null) {
			send(new ActionFailed());
			return;
		}
		var list = ctx.friends().loadFriends(active.objectId(), id -> ctx.world().player(id).isPresent());
		send(new FriendList(list));
	}

	private void onFriendInvite(GameClientPacket.RequestFriendInvite p) {
		if (!inWorld || active == null || p.name() == null || p.name().isBlank()) {
			send(new ActionFailed());
			return;
		}
		if (p.name().equalsIgnoreCase(active.name())) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao pode adicionar a si mesmo como amigo."));
			return;
		}
		var targetPlayer = ctx.world().byName(p.name()).orElse(null);
		if (targetPlayer == null || targetPlayer.character() == null) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "O jogador " + p.name() + " nao esta online."));
			return;
		}
		if (ctx.friends() != null && ctx.friends().isBlocked(targetPlayer.character(), active)) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", targetPlayer.name() + " esta bloqueando seus pedidos."));
			return;
		}
		if (targetPlayer instanceof GameSession s) {
			s.pendingFriendInviteFrom = active.objectId();
		}
		// targetPlayer.send(new FriendAddRequest(active.name()));
		send(new CreatureSay(0, CreatureSay.ALL, "SYS",
				"Convite de amizade enviado para " + targetPlayer.name() + "."));
	}

	private void onAnswerFriendInvite(GameClientPacket.RequestAnswerFriendInvite p) {
		if (!inWorld || active == null || pendingFriendInviteFrom == 0) {
			send(new ActionFailed());
			return;
		}
		int inviterId = pendingFriendInviteFrom;
		pendingFriendInviteFrom = 0;
		var inviterPlayer = ctx.world().player(inviterId).orElse(null);
		if (inviterPlayer == null || inviterPlayer.character() == null) {
			return;
		}
		if (p.response() == 1) {
			if (ctx.friends() != null) {
				ctx.friends().addFriendship(active.objectId(), active.name(), inviterPlayer.character().objectId(),
						inviterPlayer.name());
			}
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce agora e amigo de " + inviterPlayer.name() + "."));
			inviterPlayer.send(
					new CreatureSay(0, CreatureSay.ALL, "SYS", active.name() + " aceitou seu pedido de amizade!"));
			onFriendList();
			if (inviterPlayer instanceof GameSession s) {
				s.onFriendList();
			}
		} else {
			inviterPlayer.send(
					new CreatureSay(0, CreatureSay.ALL, "SYS", active.name() + " recusou seu pedido de amizade."));
		}
	}

	private void onFriendDel(GameClientPacket.RequestFriendDel p) {
		if (!inWorld || active == null || p.name() == null || p.name().isBlank()) {
			send(new ActionFailed());
			return;
		}
		var targetChar = ctx.characters().findByName(p.name()).orElse(null);
		if (targetChar != null && ctx.friends() != null) {
			ctx.friends().removeFriendship(active.objectId(), targetChar.objectId());
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", p.name() + " foi removido da sua lista de amigos."));
			onFriendList();
			ctx.world().player(targetChar.objectId()).ifPresent(pPlayer -> {
				if (pPlayer instanceof GameSession s) {
					s.onFriendList();
				}
			});
		}
	}

	private void onBlock(GameClientPacket.RequestBlock p) {
		if (!inWorld || active == null || ctx.friends() == null) {
			send(new ActionFailed());
			return;
		}
		switch (p.type()) {
			case GameClientPacket.RequestBlock.BLOCK -> {
				if (p.name() != null && !p.name().isBlank()) {
					if (ctx.friends().addBlock(active, p.name())) {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								p.name() + " foi adicionado a lista de bloqueados."));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nao foi possivel bloquear " + p.name() + "."));
					}
				}
			}
			case GameClientPacket.RequestBlock.UNBLOCK -> {
				if (p.name() != null && !p.name().isBlank()) {
					if (ctx.friends().removeBlock(active, p.name())) {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								p.name() + " foi removido da lista de bloqueados."));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								p.name() + " nao estava na lista de bloqueados."));
					}
				}
			}
			case GameClientPacket.RequestBlock.BLOCKLIST -> {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "--- Lista de Bloqueados ---"));
				for (String name : active.blockList()) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "- " + name));
				}
			}
			case GameClientPacket.RequestBlock.ALLBLOCK -> {
				active.setBlockingAll(true);
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce esta bloqueando todas as mensagens privadas."));
			}
			case GameClientPacket.RequestBlock.ALLUNBLOCK -> {
				active.setBlockingAll(false);
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce desbloqueou o recebimento de mensagens."));
			}
		}
	}

	private static int parseIntSafe(String val, int def) {
		try {
			return Integer.parseInt(val.trim());
		} catch (Exception e) {
			return def;
		}
	}

	private static long parseLongSafe(String val, long def) {
		try {
			return Long.parseLong(val.trim());
		} catch (Exception e) {
			return def;
		}
	}

	private void refreshPenalties(com.lopez.l2j.game.model.PlayerStats stats) {
		if (stats == null || active == null) {
			return;
		}
		int newWeight = stats.weightPenalty();
		int newGrade = stats.gradePenalty();
		boolean changed = (currentWeightPenalty != -1) && ((newWeight != currentWeightPenalty) || (newGrade != currentGradePenalty));
		if (changed) {
			currentWeightPenalty = newWeight;
			currentGradePenalty = newGrade;
			send(new EtcStatusUpdate(active, stats));
			var t = ctx.characters() != null ? ctx.characters().template(active) : null;
			if (t != null) {
				send(new UserInfo(active, t, active.inventory().paperdollView(), active.inventory().currentLoad(), stats));
				var info = charInfo();
				if (info != null) {
					ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, info, false);
				}
			}
		}
	}

	public void refreshWeightAndPenalties() {
		if (active == null) {
			return;
		}
		var t = ctx.characters() != null ? ctx.characters().template(active) : null;
		var stats = t != null ? com.lopez.l2j.game.model.PlayerStats.calculate(active, t) : null;
		int curLoad = active.inventory().currentLoad();
		int maxLoad = stats != null && stats.maxLoad() > 0 ? stats.maxLoad() : (t != null ? t.maxLoad() : 69000);
		send(new StatusUpdate(active.objectId(), List.of(
				new StatusUpdate.Attribute(StatusUpdate.CUR_LOAD, curLoad),
				new StatusUpdate.Attribute(StatusUpdate.MAX_LOAD, maxLoad)
		)));
		if (stats != null) {
			refreshPenalties(stats);
		}
	}

	public void sendUserInfoAndBroadcastCharInfo() {
		if (active == null) {
			return;
		}
		var t = ctx.characters() != null ? ctx.characters().template(active) : null;
		if (t != null) {
			recalcMaxVitals(t);
		}
		var stats = t != null ? com.lopez.l2j.game.model.PlayerStats.calculate(active, t) : null;
		if (t != null && stats != null) {
			send(new UserInfo(active, t, active.inventory().paperdollView(), active.inventory().currentLoad(), stats));
			int newWeight = stats.weightPenalty();
			int newGrade = stats.gradePenalty();
			if (currentWeightPenalty != -1 && (newWeight != currentWeightPenalty || newGrade != currentGradePenalty)) {
				currentWeightPenalty = newWeight;
				currentGradePenalty = newGrade;
				send(new EtcStatusUpdate(active, stats));
			}
		} else if (t != null) {
			send(new UserInfo(active, t));
		}
		var info = charInfo();
		if (info != null) {
			ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, info, false);
		}
	}

	public void onPrivateStoreManageSell() {
		tradeStoreHandler.handlePrivateStoreManageSell();
	}

	public void onSetPrivateStoreListSell(GameClientPacket.SetPrivateStoreListSell p) {
		tradeStoreHandler.handleSetPrivateStoreListSell(p);
	}

	public void onPrivateStoreQuitSell() {
		tradeStoreHandler.handlePrivateStoreQuitSell();
	}

	public void onSetPrivateStoreMsgSell(GameClientPacket.SetPrivateStoreMsgSell p) {
		tradeStoreHandler.handleSetPrivateStoreMsgSell(p);
	}

	public void onPrivateStoreBuy(GameClientPacket.RequestPrivateStoreBuy p) {
		tradeStoreHandler.handlePrivateStoreBuy(p);
	}

	private void onConfirmTargetItem(GameClientPacket.RequestConfirmTargetItem p) {
		if (!inWorld || active == null || active.isDead() || active.sitting()) {
			send(new ActionFailed());
			return;
		}
		var item = active.inventory().byObjectId(p.itemObjId()).orElse(null);
		if (item == null || ctx.augmentation() == null || !ctx.augmentation().isAugmentable(item)) {
			send(SystemMessage.id(SystemMessage.THIS_IS_NOT_A_SUITABLE_ITEM));
			send(new ActionFailed());
			return;
		}
		send(new ExPutItemResultForVariationMake(p.itemObjId()));
		send(SystemMessage.id(SystemMessage.SELECT_THE_CATALYST_FOR_AUGMENTATION));
	}

	private void onConfirmRefinerItem(GameClientPacket.RequestConfirmRefinerItem p) {
		if (!inWorld || active == null || active.isDead() || ctx.augmentation() == null) {
			send(new ActionFailed());
			return;
		}
		var target = active.inventory().byObjectId(p.targetItemObjId()).orElse(null);
		var refiner = active.inventory().byObjectId(p.refinerItemObjId()).orElse(null);
		if (target == null || refiner == null || !AugmentationService.isLifeStone(refiner.itemId())) {
			send(SystemMessage.id(SystemMessage.THIS_IS_NOT_A_SUITABLE_ITEM));
			send(new ActionFailed());
			return;
		}
		int level = AugmentationService.getLifeStoneLevel(refiner.itemId());
		if (active.level() < AugmentationService.getMinPlayerLevel(level)) {
			send(SystemMessage.id(SystemMessage.THIS_IS_NOT_A_SUITABLE_ITEM));
			send(new ActionFailed());
			return;
		}
		int gemItemId = AugmentationService.getGemstoneItemId(target);
		int gemCount = AugmentationService.getGemstoneCount(target);
		send(new ExPutIntensiveResultForVariationMake(p.refinerItemObjId(), refiner.itemId(), gemItemId, gemCount));
	}

	private void onConfirmGemStone(GameClientPacket.RequestConfirmGemStone p) {
		if (!inWorld || active == null || active.isDead() || ctx.augmentation() == null) {
			send(new ActionFailed());
			return;
		}
		var target = active.inventory().byObjectId(p.targetItemObjId()).orElse(null);
		var gem = active.inventory().byObjectId(p.gemstoneItemObjId()).orElse(null);
		if (target == null || gem == null) {
			send(new ActionFailed());
			return;
		}
		int reqItemId = AugmentationService.getGemstoneItemId(target);
		int reqCount = AugmentationService.getGemstoneCount(target);
		if (gem.itemId() != reqItemId || gem.count() < reqCount || p.gemstoneCount() != reqCount) {
			send(new ActionFailed());
			return;
		}
		send(new ExPutCommissionResultForVariationMake(p.gemstoneItemObjId(), p.gemstoneCount(), reqItemId));
	}

	private void onRefine(GameClientPacket.RequestRefine p) {
		if (!inWorld || active == null || active.isDead() || ctx.augmentation() == null) {
			send(new ExVariationResult(0, 0, 0));
			send(new ActionFailed());
			return;
		}
		var target = active.inventory().byObjectId(p.targetItemObjId()).orElse(null);
		var refiner = active.inventory().byObjectId(p.refinerItemObjId()).orElse(null);
		var gem = active.inventory().byObjectId(p.gemstoneItemObjId()).orElse(null);
		if (target == null || refiner == null || gem == null || !ctx.augmentation().isAugmentable(target)
				|| !AugmentationService.isLifeStone(refiner.itemId())) {
			send(new ExVariationResult(0, 0, 0));
			send(new ActionFailed());
			return;
		}
		int reqCount = AugmentationService.getGemstoneCount(target);
		int reqGemId = AugmentationService.getGemstoneItemId(target);
		if (gem.itemId() != reqGemId || gem.count() < reqCount) {
			send(new ExVariationResult(0, 0, 0));
			send(new ActionFailed());
			return;
		}
		boolean wasEquipped = target.isEquipped();
		if (wasEquipped) {
			afterEquipChange(ctx.inventories().toggleEquip(active.inventory(), target.objectId()));
		}
		var resRefiner = ctx.inventories().destroyItem(active.inventory(), refiner.objectId(), 1, "Refine");
		var resGem = ctx.inventories().destroyItem(active.inventory(), gem.objectId(), reqCount, "Refine");
		var aug = ctx.augmentation().applyAugmentation(target, refiner.itemId());
		send(new ExVariationResult(aug.stat12(), aug.stat34(), 1));
		List<ItemInfo> updates = new ArrayList<>();
		updates.add(ItemInfo.of(target, ItemInfo.MODIFIED));
		if (resRefiner != null) {
			updates.add(ItemInfo.of(resRefiner.item(), resRefiner.removed() ? ItemInfo.REMOVED : ItemInfo.MODIFIED));
		}
		if (resGem != null) {
			updates.add(ItemInfo.of(resGem.item(), resGem.removed() ? ItemInfo.REMOVED : ItemInfo.MODIFIED));
		}
		send(new InventoryUpdate(updates));
		if (wasEquipped) {
			afterEquipChange(ctx.inventories().toggleEquip(active.inventory(), target.objectId()));
		}
		sendUserInfoAndBroadcastCharInfo();
		send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Arma augmentada com sucesso!"));
	}

	private void onConfirmCancelItem(GameClientPacket.RequestConfirmCancelItem p) {
		if (!inWorld || active == null || active.isDead() || ctx.augmentation() == null) {
			send(new ActionFailed());
			return;
		}
		var item = active.inventory().byObjectId(p.itemObjId()).orElse(null);
		if (item == null || !item.isAugmented()) {
			send(new ActionFailed());
			return;
		}
		long price = AugmentationService.getCancelPrice(item);
		send(new ExPutItemResultForVariationCancel(p.itemObjId(), price));
	}

	private void onRefineCancel(GameClientPacket.RequestRefineCancel p) {
		if (!inWorld || active == null || active.isDead() || ctx.augmentation() == null) {
			send(new ExVariationCancelResult(0));
			send(new ActionFailed());
			return;
		}
		var target = active.inventory().byObjectId(p.itemObjId()).orElse(null);
		if (target == null || !target.isAugmented()) {
			send(new ExVariationCancelResult(0));
			send(new ActionFailed());
			return;
		}
		long price = AugmentationService.getCancelPrice(target);
		if (active.inventory().adena() < price) {
			send(new ExVariationCancelResult(0));
			send(new ActionFailed());
			return;
		}
		boolean wasEquipped = target.isEquipped();
		if (wasEquipped) {
			afterEquipChange(ctx.inventories().toggleEquip(active.inventory(), target.objectId()));
		}
		ctx.inventories().consumeItem(active.inventory(), 57, (int) price, "RefineCancel");
		ctx.augmentation().removeAugmentation(target);
		send(new ExVariationCancelResult(1));
		send(new InventoryUpdate(List.of(ItemInfo.of(target, ItemInfo.MODIFIED))));
		if (wasEquipped) {
			afterEquipChange(ctx.inventories().toggleEquip(active.inventory(), target.objectId()));
		}
		sendUserInfoAndBroadcastCharInfo();
		send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Augmentacao removida com sucesso."));
	}
}
