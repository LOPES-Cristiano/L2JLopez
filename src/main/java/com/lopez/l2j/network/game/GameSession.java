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
import com.lopez.l2j.network.game.handler.packet.AdminPacketHandler;
import com.lopez.l2j.network.game.handler.packet.NpcBypassPacketHandler;
import com.lopez.l2j.network.game.handler.packet.MagicSkillPacketHandler;
import com.lopez.l2j.network.game.handler.packet.ChatPacketHandler;
import com.lopez.l2j.network.game.handler.packet.ItemPacketHandler;
import com.lopez.l2j.network.game.handler.packet.HennaPacketHandler;
import com.lopez.l2j.network.game.handler.packet.SocialPacketHandler;
import com.lopez.l2j.network.game.handler.packet.CommandPacketHandler;
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
import com.lopez.l2j.network.game.packet.GameServerPacket.RecipeBookItemList;
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
				com.lopez.l2j.game.zone.ZoneTable zones, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists,
					combat, null, null, null, null, null, null, null, null, null, List.of(), null, null, null, null, null, null, null, null,
					zones, null, null, null, serverName);
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

		private static volatile com.lopez.l2j.game.npc.NpcTemplateTable globalNpcTemplateTable;

		public static void setGlobalNpcTemplateTable(com.lopez.l2j.game.npc.NpcTemplateTable table) {
			globalNpcTemplateTable = table;
		}

		public com.lopez.l2j.game.npc.NpcTemplateTable npcTemplates() {
			if (spawns != null && spawns.templates() != null) {
				return spawns.templates();
			}
			return globalNpcTemplateTable;
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
	private ScheduledFuture<?> playerAutoAttackTask;
	private ScheduledFuture<?> combatStanceTask;
	private volatile GameSession followingTarget;
	private ScheduledFuture<?> followTask;
	private volatile int pendingNpcInteractObjectId;
	private volatile int pendingGroundItemPickupObjectId;
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
	private final AdminPacketHandler adminHandler;
	private final NpcBypassPacketHandler npcBypassHandler;
	private final MagicSkillPacketHandler magicSkillHandler;
	private final HennaPacketHandler hennaHandler;
	private final SocialPacketHandler socialHandler;
	private final CommandPacketHandler commandHandler;

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
		this.adminHandler = new AdminPacketHandler(this);
		this.npcBypassHandler = new NpcBypassPacketHandler(this);
		this.magicSkillHandler = new MagicSkillPacketHandler(this);
		this.hennaHandler = new HennaPacketHandler(this);
		this.socialHandler = new SocialPacketHandler(this);
		this.commandHandler = new CommandPacketHandler(this);
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

	public void autoAttacking(boolean autoAttacking) {
		this.autoAttacking = autoAttacking;
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

	public PlayerCharacter active() {
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

	public Context ctx() {
		return ctx;
	}

	public Set<Integer> knownObjects() {
		return knownObjects;
	}

	@Override
	public void addKnownObject(int objectId) {
		knownObjects.add(objectId);
	}

	@Override
	public void removeKnownObject(int objectId) {
		knownObjects.remove(objectId);
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
		stopFollow();
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

	public int pendingGroundItemPickupObjectId() {
		return pendingGroundItemPickupObjectId;
	}

	public void pendingGroundItemPickupObjectId(int id) {
		this.pendingGroundItemPickupObjectId = id;
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
			case GameClientPacket.RequestShowBoard p -> handleBbsCommand("_bbshome");
			case GameClientPacket.RequestRecipeBookOpen p -> onRecipeBookOpen(p);
			case SendBypassBuildCmd p -> handleAdminCommand(p.command());
			case RequestBuyItem p -> onBuyItem(p);
			case RequestSellItem p -> onSellItem(p);
			case RequestTargetCancel p -> onCancelTarget();
			case RequestActionUse p -> onActionUse(p);
			case RequestItemList p -> send(ItemList.of(active.inventory().items(), true));
			case UseItem p -> onUseItem(p);
			case RequestUnEquipItem p -> onUnEquip(p);
			case GameClientPacket.RequestDropItem p -> itemHandler.handleDropItem(p);
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
		if (ctx.subClasses() != null) {
			c.setSubClasses(ctx.subClasses().loadSubClasses(c.objectId()));
			if (c.subClasses() != null && c.classId() != c.baseClassId()) {
				for (var entry : c.subClasses().entrySet()) {
					if (entry.getValue().classId() == c.classId()) {
						c.classIndex(entry.getKey());
						break;
					}
				}
			}
		}
		if (c.classIndex() == 0 && c.baseClassId() == 0 && c.classId() > 0
				&& (c.subClasses() == null || c.subClasses().isEmpty())) {
			c.baseClassId(c.classId());
			if (ctx.characters() != null) {
				ctx.characters().save(c, false);
			}
		}
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
				ctx.shortcuts() != null ? ctx.shortcuts().findByCharId(active.objectId(), active.classIndex()) : List.of()));
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
			send(com.lopez.l2j.network.game.packet.GameServerPacket.StatusUpdate.forPlayer(active));
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
		if (canChangeClass()) {
			checkClassMasterPopup();
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
					send(StatusUpdate.hp(npc.objectId(), 0, (int) npc.maxHp()));
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
		if (weapon != null) {
			String sub = weapon.template().subType();
			if ("bow".equalsIgnoreCase(sub)) {
				int range = 500;
				Integer longShotLvl = player.skills().get(113); // Long Shot
				if (longShotLvl != null && longShotLvl > 0) {
					range += longShotLvl * 200;
				}
				return range;
			}
			if ("pole".equalsIgnoreCase(sub) || "polearm".equalsIgnoreCase(sub) || "spear".equalsIgnoreCase(sub)) {
				return 80;
			}
		}
		return 40;
	}

	public void onAction(Action p) {
		actionHandler.handleAction(p);
	}

	public void onAttackRequest(AttackRequest p) {
		actionHandler.handleAttackRequest(p);
	}

	public boolean isAttackingNow() {
		return System.currentTimeMillis() < attackEndTime;
	}

	public synchronized void cancelPlayerAutoAttackTask() {
		if (playerAutoAttackTask != null && !playerAutoAttackTask.isDone()) {
			playerAutoAttackTask.cancel(false);
			playerAutoAttackTask = null;
		}
	}

	public void startAutoAttack(NpcInstance npc) {
		if (isAttackingNow()) {
			send(new ActionFailed());
			return;
		}
		if (npc != null && npc.isDead()) {
			send(StatusUpdate.hp(npc.objectId(), 0, (int) npc.maxHp()));
			send(new Die(npc.objectId(), false));
			send(new ActionFailed());
			return;
		}
		if (casting || npc == null || !npc.template().isAttackable()) {
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
		stopFollow();
		autoAttacking = true;
		onAttacking();

		double dx = active.x() - npc.x();
		double dy = active.y() - npc.y();
		double distSq = dx * dx + dy * dy;
		int attackRange = getPhysicalAttackRange(active);
		double colRad = (npc.template() != null ? npc.template().collisionRadius() : 30.0);
		double maxDist = attackRange + colRad + 50.0;

		if (distSq > maxDist * maxDist) {
			var movePawn = new MoveToPawn(active.objectId(), npc.objectId(), (int) (attackRange + colRad), active.x(), active.y(),
					active.z());
			send(movePawn);
			ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, movePawn, false);
			schedulePlayerAutoAttack(npc, 200);
		} else {
			onAttackNpc(npc);
		}
	}

	public void startAutoAttack(GameSession targetPlayer) {
		startAutoAttack(targetPlayer, false);
	}

	public void startAutoAttack(GameSession targetPlayer, boolean force) {
		if (isAttackingNow()) {
			send(new ActionFailed());
			return;
		}
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
		// Sem CTRL (force attack), só ataca se estiver em zona de combate/arena ou alvo com flag/karma/war
		// Caso contrário, apenas segue o player (follow)
		if (!force && !canAutoAttackPlayerWithoutCtrl(targetPlayer)) {
			startFollow(targetPlayer);
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
		stopFollow();
		autoAttacking = true;
		onAttacking();
		updatePvPFlag();

		double dx = active.x() - targetPlayer.x();
		double dy = active.y() - targetPlayer.y();
		double distSq = dx * dx + dy * dy;
		int attackRange = getPhysicalAttackRange(active);
		double colRad = 25.0;
		double maxDist = attackRange + colRad + 50.0;

		if (distSq > maxDist * maxDist) {
			var movePawn = new MoveToPawn(active.objectId(), targetPlayer.objectId(), (int) (attackRange + colRad), active.x(), active.y(),
					active.z());
			send(movePawn);
			ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, movePawn, false);
			schedulePlayerAutoAttack(targetPlayer, 200);
		} else {
			onAttackPlayer(targetPlayer);
		}
	}

	public boolean canAutoAttackPlayerWithoutCtrl(GameSession targetPlayer) {
		if (targetPlayer == null || targetPlayer.active == null || targetPlayer.active.isDead() || targetPlayer == this) {
			return false;
		}
		if (isFriendlyTarget(targetPlayer)) {
			return false;
		}
		if (ctx != null && ctx.zones() != null && (ctx.zones().isInsidePeace(active.x(), active.y(), active.z())
				|| ctx.zones().isInsidePeace(targetPlayer.x(), targetPlayer.y(), targetPlayer.z()))) {
			return false;
		}
		if (isInsidePvpCombatZone() || targetPlayer.isInsidePvpCombatZone()) {
			return true;
		}
		if (targetPlayer.active.pvpFlag() > 0) {
			return true;
		}
		if (targetPlayer.active.karma() > 0) {
			return true;
		}
		if (active.clanId() > 0 && targetPlayer.active.clanId() > 0 && ctx != null && ctx.clanWars() != null) {
			if (ctx.clanWars().isAtWar(active.clanId(), targetPlayer.active.clanId())) {
				return true;
			}
		}
		return false;
	}

	public boolean isInsidePvpCombatZone() {
		if (active == null || ctx == null || ctx.zones() == null) {
			return false;
		}
		return ctx.zones().isInsidePvpCombatZone(active.x(), active.y(), active.z());
	}

	public void startFollow(GameSession targetPlayer) {
		if (casting || targetPlayer == null || targetPlayer.active == null || targetPlayer.active.isDead() || targetPlayer == this) {
			send(new ActionFailed());
			return;
		}
		stopAutoAttack();
		stopFollow();

		this.followingTarget = targetPlayer;
		this.targetObjectId = targetPlayer.objectId();

		int offset = 60;
		var movePawn = new MoveToPawn(active.objectId(), targetPlayer.objectId(), offset, active.x(), active.y(), active.z());
		send(movePawn);
		if (ctx != null && ctx.world() != null) {
			ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, movePawn, false);
		}

		scheduleFollow(targetPlayer);
	}

	private void scheduleFollow(GameSession targetPlayer) {
		if (followTask != null && !followTask.isDone()) {
			followTask.cancel(false);
		}
		followTask = autoAttackScheduler.schedule(() -> {
			if (!inWorld || active == null || active.isDead() || followingTarget != targetPlayer
					|| targetPlayer == null || targetPlayer.active == null || targetPlayer.active.isDead()) {
				stopFollow();
				return;
			}
			double dx = active.x() - targetPlayer.x();
			double dy = active.y() - targetPlayer.y();
			double distSq = dx * dx + dy * dy;
			double maxFollowDist = 3000.0;
			if (distSq > maxFollowDist * maxFollowDist) {
				stopFollow();
				return;
			}
			if (distSq > 100.0 * 100.0) {
				var movePawn = new MoveToPawn(active.objectId(), targetPlayer.objectId(), 60, active.x(), active.y(), active.z());
				send(movePawn);
				if (ctx != null && ctx.world() != null) {
					ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, movePawn, false);
				}
			}
			scheduleFollow(targetPlayer);
		}, 1000L, TimeUnit.MILLISECONDS);
	}

	public void stopFollow() {
		followingTarget = null;
		if (followTask != null && !followTask.isDone()) {
			followTask.cancel(false);
			followTask = null;
		}
	}

	public boolean isFollowing() {
		return followingTarget != null;
	}

	public GameSession followingTarget() {
		return followingTarget;
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
		cancelPlayerAutoAttackTask();
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
				double colRad = (npc.template() != null ? npc.template().collisionRadius() : 30.0);
				double maxDist = attackRange + colRad + 50.0;
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
				double colRad = 25.0;
				double maxDist = attackRange + colRad + 50.0;
				if (distSq <= maxDist * maxDist && System.currentTimeMillis() >= attackEndTime) {
					onAttackPlayer(targetSession);
				}
			}
		}
	}

	public void onCancelTarget() {
		actionHandler.handleCancelTarget();
	}

	private synchronized void onAttackNpc(NpcInstance npc) {
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
		double distToNpc = Math.hypot(npc.x() - active.x(), npc.y() - active.y());
		int timeToHit = calculateTimeToHit(timeAtk, activeWeapon(), distToNpc);
		attackEndTime = now + timeAtk;

		// Vira de frente para o alvo
		int heading = (int) Math.round(Math.atan2(npc.y() - active.y(), npc.x() - active.x()) * 10430.378);
		active.heading(heading);

		if (ctx.combat() != null && ctx.combat().checkRaidCurse(active, npc)) {
			applyControlEffect(com.lopez.l2j.game.combat.CombatService.RAID_CURSE_PETRIFY, 1, "petrification", System.currentTimeMillis() + 120_000L);
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
		rechargeAutoSoulShots();

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
				var su = StatusUpdate.hp(npc.objectId(), 0, (int) npc.maxHp());
				send(su);
				send(new Die(npc.objectId(), false));
				return;
			}

			var hit = ctx.combat().applyDamage(npc, plan.damage(), plan.flags());

			if (hit.damage() > 0) {
				send(SystemMessage.of(SystemMessage.YOU_DID_S1_DMG, new SystemMessage.Number(hit.damage())));
			}
			if (plan.crit() && !npc.isDead()) {
				triggerWeaponOnCritSkill(npc, null);
			}

			int remainingHp = hit.isDead() ? 0 : Math.max(0, hit.remainingHp());
			var su = StatusUpdate.hp(npc.objectId(), remainingHp, (int) npc.maxHp());
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
				schedulePlayerAutoAttack(npc, Math.max(50, (timeAtk - timeToHit) + 50));
			}
		}, timeToHit, TimeUnit.MILLISECONDS);
	}

	public void schedulePlayerAutoAttack(NpcInstance npc) {
		schedulePlayerAutoAttack(npc, 100);
	}

	private synchronized void schedulePlayerAutoAttack(NpcInstance npc, long delayMs) {
		cancelPlayerAutoAttackTask();
		if (!autoAttacking || active == null || npc == null || npc.isDead()) {
			return;
		}
		playerAutoAttackTask = autoAttackScheduler.schedule(() -> {
			if (!autoAttacking || active == null || active.isDead() || !inWorld || targetObjectId != npc.objectId() || npc.isDead()) {
				return;
			}
			double dx = active.x() - npc.x();
			double dy = active.y() - npc.y();
			double distSq = dx * dx + dy * dy;
			int attackRange = getPhysicalAttackRange(active);
			double colRad = (npc.template() != null ? npc.template().collisionRadius() : 30.0);
			double maxDist = attackRange + colRad + 50.0;
			if (distSq > maxDist * maxDist) {
				double dist = Math.sqrt(distSq);
				var t = ctx.characters() != null ? ctx.characters().template(active) : null;
				int runSpd = t != null ? Math.max(60, PlayerStats.calculate(active, t).runSpeed()) : 150;
				double step = runSpd * (delayMs / 1000.0);
				double moveDist = Math.min(dist - attackRange - colRad, step);
				if (moveDist > 0) {
					double angle = Math.atan2(npc.y() - active.y(), npc.x() - active.x());
					int nextX = (int) Math.round(active.x() + Math.cos(angle) * moveDist);
					int nextY = (int) Math.round(active.y() + Math.sin(angle) * moveDist);
					active.moveTo(nextX, nextY, npc.z());
				}
				double newDx = active.x() - npc.x();
				double newDy = active.y() - npc.y();
				if (newDx * newDx + newDy * newDy <= maxDist * maxDist) {
					onAttackNpc(npc);
				} else {
					schedulePlayerAutoAttack(npc, 200);
				}
			} else {
				onAttackNpc(npc);
			}
		}, delayMs, TimeUnit.MILLISECONDS);
	}

	public void schedulePlayerAutoAttack(GameSession targetPlayer) {
		schedulePlayerAutoAttack(targetPlayer, 100);
	}

	private synchronized void schedulePlayerAutoAttack(GameSession targetPlayer, long delayMs) {
		cancelPlayerAutoAttackTask();
		if (!autoAttacking || active == null || targetPlayer == null || targetPlayer.active == null || targetPlayer.active.isDead()) {
			return;
		}
		playerAutoAttackTask = autoAttackScheduler.schedule(() -> {
			if (!autoAttacking || active == null || active.isDead() || !inWorld || targetObjectId != targetPlayer.objectId() || targetPlayer.active == null || targetPlayer.active.isDead()) {
				return;
			}
			double dx = active.x() - targetPlayer.x();
			double dy = active.y() - targetPlayer.y();
			double distSq = dx * dx + dy * dy;
			int attackRange = getPhysicalAttackRange(active);
			double colRad = 25.0;
			double maxDist = attackRange + colRad + 50.0;
			if (distSq > maxDist * maxDist) {
				double dist = Math.sqrt(distSq);
				var t = ctx.characters() != null ? ctx.characters().template(active) : null;
				int runSpd = t != null ? Math.max(60, PlayerStats.calculate(active, t).runSpeed()) : 150;
				double step = runSpd * (delayMs / 1000.0);
				double moveDist = Math.min(dist - attackRange - colRad, step);
				if (moveDist > 0) {
					double angle = Math.atan2(targetPlayer.y() - active.y(), targetPlayer.x() - active.x());
					int nextX = (int) Math.round(active.x() + Math.cos(angle) * moveDist);
					int nextY = (int) Math.round(active.y() + Math.sin(angle) * moveDist);
					active.moveTo(nextX, nextY, targetPlayer.z());
				}
				double newDx = active.x() - targetPlayer.x();
				double newDy = active.y() - targetPlayer.y();
				if (newDx * newDx + newDy * newDy <= maxDist * maxDist) {
					onAttackPlayer(targetPlayer);
				} else {
					schedulePlayerAutoAttack(targetPlayer, 200);
				}
			} else {
				onAttackPlayer(targetPlayer);
			}
		}, delayMs, TimeUnit.MILLISECONDS);
	}

	private synchronized void onAttackPlayer(GameSession targetPlayer) {
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
			send(new ActionFailed());
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
		double distToPlayer = Math.hypot(targetPlayer.x() - active.x(), targetPlayer.y() - active.y());
		int timeToHit = calculateTimeToHit(timeAtk, activeWeapon(), distToPlayer);
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
		rechargeAutoSoulShots();

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
				schedulePlayerAutoAttack(targetPlayer, Math.max(50, (timeAtk - timeToHit) + 50));
			}
		}, timeToHit, TimeUnit.MILLISECONDS);
	}

	public void applyExpAndSp(long expReward, int spReward) {
		if (active != null) {
			var t = ctx.characters() != null ? ctx.characters().template(active) : null;
			applyExpAndSp(expReward, spReward, t);
		}
	}

	public void applyExpAndSp(long expReward, int spReward, CharTemplate t) {
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
			if (ctx.world() != null) {
				ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, social, false);
			}

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

			checkClassMasterPopup(oldLevel, newLevel);
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

	public NpcBypassPacketHandler npcBypassHandler() {
		return npcBypassHandler;
	}

	public void showNpcHtml(NpcInstance npc, int val) {
		npcBypassHandler.showNpcHtml(npc, val);
	}
	public void onBypass(RequestBypassToServer p) {
		npcBypassHandler.onBypass(p);
	}

	public void onBbsWrite(RequestBBSwrite p) {
		npcBypassHandler.onBbsWrite(p);
	}

	public void handleBbsCommand(String command) {
		npcBypassHandler.handleBbsCommand(command);
	}

	public void handleQuestBypass(int npcObjId, String questArg) {
		npcBypassHandler.handleQuestBypass(npcObjId, questArg);
	}

	public void handleSubclassBypass(int npcObjId, String command) {
		npcBypassHandler.handleSubclassBypass(npcObjId, command);
	}

	public void applySubClassSwitch(int slot, int classId, int level, long exp, int sp) {
		npcBypassHandler.applySubClassSwitch(slot, classId, level, exp, sp);
	}

	public void saveCurrentClassProgress() {
		npcBypassHandler.saveCurrentClassProgress();
	}

	public void showClassMasterMenu(NpcInstance npc, int menuLevel) {
		npcBypassHandler.showClassMasterMenu(npc != null ? npc.objectId() : 0, menuLevel);
	}

	public void showClassMasterMenu(int npcObjectId, int menuLevel) {
		npcBypassHandler.showClassMasterMenu(npcObjectId, menuLevel);
	}

	public boolean canChangeClass() {
		if (active == null) {
			return false;
		}
		var curTpl = ctx.characters() != null ? ctx.characters().template(active.classId()).orElse(null) : null;
		int currentTier = curTpl != null ? curTpl.classTier() : 0;
		if (currentTier >= 3) {
			return false;
		}
		int nextClassTier = currentTier + 1;
		int minLvl = switch (nextClassTier) {
			case 1 -> 20;
			case 2 -> 40;
			case 3 -> 76;
			default -> 1;
		};
		if (active.level() < minLvl) {
			return false;
		}
		if (ctx.skillService() != null && ctx.skillService().trees() != null) {
			var children = ctx.skillService().trees().getChildClasses(active.classId());
			return children != null && !children.isEmpty();
		}
		return true;
	}

	public void checkClassMasterPopup() {
		checkClassMasterPopup(0, active != null ? active.level() : 0);
	}

	public void checkClassMasterPopup(int oldLevel, int newLevel) {
		if (active == null) {
			return;
		}
		if (!com.lopez.l2j.config.Config.CLASS_MASTER_POPUP_WINDOW) {
			return;
		}
		if (!com.lopez.l2j.config.Config.CLASS_MASTER && !com.lopez.l2j.config.Config.ALT_CLASS_MASTER) {
			return;
		}
		if (canChangeClass()) {
			var curTpl = ctx.characters() != null ? ctx.characters().template(active.classId()).orElse(null) : null;
			int currentTier = curTpl != null ? curTpl.classTier() : 0;
			int nextClassTier = currentTier + 1;
			int minLvl = switch (nextClassTier) {
				case 1 -> 20;
				case 2 -> 40;
				case 3 -> 76;
				default -> 1;
			};
			if (oldLevel == 0 || (oldLevel < minLvl && newLevel >= minLvl) || (newLevel >= minLvl && oldLevel < newLevel)) {
				showClassMasterMenu(0, nextClassTier);
			}
		}
	}

	public void handleChangeClass(int npcObjectId, int classId) {
		npcBypassHandler.handleChangeClass(npcObjectId, classId);
	}

	public void handleChangeClass(int npcObjectId, int classId, boolean quiet) {
		npcBypassHandler.handleChangeClass(npcObjectId, classId, quiet);
	}

	public void teleportTo(int teleId) {
		npcBypassHandler.teleportTo(teleId);
	}

	public void teleportToCoordinates(int x, int y, int z) {
		npcBypassHandler.teleportToCoordinates(x, y, z);
	}

	public void showBuyList(NpcInstance npc, int listId) {
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

	public void sendSocialAction(int actionId) {
		if (!inWorld || active == null || active.isDead() || active.sitting() || active.isFishing()) {
			send(new ActionFailed());
			return;
		}
		var pkt = new SocialAction(active.objectId(), actionId);
		send(pkt);
		if (ctx.world() != null) {
			ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, pkt, false);
		}
	}

	private void onSocialAction(GameClientPacket.RequestSocialAction p) {
		sendSocialAction(p.actionId());
	}

	public void onRecipeBookOpen(GameClientPacket.RequestRecipeBookOpen p) {
		if (!inWorld || active == null) {
			send(new ActionFailed());
			return;
		}
		send(new RecipeBookItemList(p.isDwarvenCraft(), 0, java.util.List.of()));
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

	public void stopHot(String stack, ScheduledFuture<?> task) {
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
		PlayerCharacter owner = active;
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
				var c = consumableOpt.get();
				var d = c.buff();
				long durationMs = remainingMs > 0 ? remainingMs : d.durationMs();
				var buff = new ActiveBuff(c.skillId(), c.level(), d.stackType(), System.currentTimeMillis() + durationMs,
						d.runSpdAdd(), d.pAtkSpdMul(), d.mAtkSpdMul(), d.accuracyAdd());
				owner.effects().put(buff);
				autoAttackScheduler.schedule(() -> {
					if (owner.effects().remove(buff) && active == owner && inWorld) {
						send(SystemMessage.of(SystemMessage.S1_HAS_WORN_OFF,
								new SystemMessage.SkillName(c.skillId(), c.level())));
						refreshBuffs();
						saveBuffs();
					}
				}, durationMs, TimeUnit.MILLISECONDS);
				continue;
			}
			// 2. Tenta restaurar como skill da SkillTable
			if (ctx.skillService() != null) {
				var skillOpt = ctx.skillService().table().get(s.skillId(), s.level());
				if (skillOpt.isPresent()) {
					var sk = skillOpt.get();
					for (var e : sk.effects()) {
						String name = e.name();
						boolean isDebuff = sk.isDebuff() || sk.isOffensive();
						boolean isInvincible = name.equalsIgnoreCase("Invincible");
						boolean isBuffOrDebuff = name.equalsIgnoreCase("Buff") || name.equalsIgnoreCase("Debuff") || sk.isBuff() || isDebuff || isInvincible;
						if (e.funcs().isEmpty() && !isBuffOrDebuff) {
							continue;
						}
						String stack = e.stackType() == null || e.stackType().equalsIgnoreCase("none")
								? "skill_" + sk.id() + "_" + name
								: e.stackType();
						long duration = remainingMs > 0 ? remainingMs : e.durationMs();
						if (duration <= 0) {
							duration = 30_000L;
						}
						long end = System.currentTimeMillis() + duration;
						var buff = ActiveBuff.ofSkill(sk.id(), sk.level(), stack, end, e.funcs(), isDebuff);
						owner.effects().put(buff);
						if (isInvincible) {
							owner.invul(true);
						}
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
					continue;
				}
			}
			// 3. Fallback genérico para qualquer outro buff
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

	public void triggerWeaponOnCastSkill(NpcInstance npc, GameSession targetPlayer) {
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

	public void onLogout() {
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
				if (ctx.questManager() != null) {
					for (var qs : getAllQuestStates()) {
						ctx.questManager().saveQuestState(qs);
					}
				}
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
			ctx.shortcuts().save(active.objectId(), active.classIndex(), sc);
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
			ctx.shortcuts().delete(active.objectId(), active.classIndex(), slot, page);
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
	public MagicSkillPacketHandler magicSkillHandler() {
		return magicSkillHandler;
	}

	public void castSkill(SkillTemplate sk, boolean mayMove) {
		magicSkillHandler.castSkill(sk, mayMove);
	}

	public void finishCast(SkillTemplate sk, NpcInstance mainNpc, DoorInstance doorTarget, GameSession targetPlayer, boolean resumeAttack, boolean sps, boolean bss) {
		magicSkillHandler.finishCast(sk, mainNpc, doorTarget, targetPlayer, resumeAttack, sps, bss);
	}

	public void applyControlEffect(String effectName, long until) {
		magicSkillHandler.applyControlEffect(effectName, until);
	}

	public void applyControlEffect(int skillId, int skillLevel, String effectName, long until) {
		magicSkillHandler.applyControlEffect(skillId, skillLevel, effectName, until);
	}

	public void handlePlayerDeath() {
		magicSkillHandler.handlePlayerDeath();
	}

	public void onDeath() {
		magicSkillHandler.onDeath();
	}

	public void onDeath(int killerObjectId) {
		magicSkillHandler.onDeath(killerObjectId);
	}

	public void handlePlayerDeath(PlayerCharacter killer) {
		magicSkillHandler.handlePlayerDeath(killer);
	}

	public void rewardBarakielNoblesse(com.lopez.l2j.game.party.Party party, PlayerCharacter active) {
		magicSkillHandler.rewardBarakielNoblesse(party, active);
	}

	public void grantNoblesseStatus(GameSession session) {
		magicSkillHandler.grantNoblesseStatus(session);
	}

	public void grantNoblesseStatus(PlayerCharacter p) {
		magicSkillHandler.grantNoblesseStatus(p);
	}

	public void cancelCast() {
		magicSkillHandler.cancelCast();
	}

	public void updatePvPFlag() {
		magicSkillHandler.updatePvPFlag();
	}

	public void updatePvPFlag(boolean againstPvPPlayer) {
		magicSkillHandler.updatePvPFlag(againstPvPPlayer);
	}

	public void receivePositiveSkill(SkillTemplate sk, String casterName) {
		magicSkillHandler.receivePositiveSkill(sk, casterName);
	}

	public void applySkillEffects(SkillTemplate sk, boolean toggle) {
		magicSkillHandler.applySkillEffects(sk, toggle);
	}

	public void applySkillEffects(SkillTemplate sk, boolean toggle, long remainingMs) {
		magicSkillHandler.applySkillEffects(sk, toggle, remainingMs);
	}

	public void startSkillDot(SkillTemplate sk, SkillTemplate.EffectTemplate e) {
		magicSkillHandler.startSkillDot(sk, e);
	}

	public void toggleSkill(SkillTemplate sk) {
		magicSkillHandler.toggleSkill(sk);
	}

	public void recalcMaxVitals(CharTemplate t) {
		magicSkillHandler.recalcMaxVitals(t);
	}

	public void applyOffensive(SkillTemplate sk, NpcInstance npc, CharTemplate t, boolean soulshot, boolean sps, boolean bss) {
		magicSkillHandler.applyOffensive(sk, npc, t, soulshot, sps, bss);
	}

	public void applyOffensivePlayer(SkillTemplate sk, GameSession targetSession, CharTemplate t, boolean soulshot, boolean sps, boolean bss) {
		magicSkillHandler.applyOffensivePlayer(sk, targetSession, t, soulshot, sps, bss);
	}

	public void sendSkillList() {
		if (ctx.skillService() != null) {
			send(new SkillList(ctx.skillService().skillList(active)));
		} else {
			send(new SkillList(ctx.skills() != null ? ctx.skills().findByCharId(active.objectId(), active.classIndex()) : List.of()));
		}
	}

	/** Expertise/Lucky/AutoLearn depois de entrar ou subir de nivel. */
	public void rewardSkills(CharTemplate t, boolean sendList) {
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

	public void showSkillList(NpcInstance npc) {
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
		for (ShortCut sc : ctx.shortcuts().findByCharId(active.objectId(), active.classIndex())) {
			if (sc.type() == ShortCut.TYPE_SKILL && sc.id() == skillId) {
				var updated = new ShortCut(sc.slot(), sc.page(), sc.type(), sc.id(), level, sc.characterType());
				ctx.shortcuts().save(active.objectId(), active.classIndex(), updated);
				send(new ShortCutRegister(updated));
			}
		}
	}

	public void showEnchantSkillList(NpcInstance npc) {
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

	public boolean isBow(com.lopez.l2j.game.item.ItemInstance weapon) {
		return weapon != null && weapon.template() != null && "bow".equalsIgnoreCase(weapon.template().subType());
	}

	private int calculateTimeToHit(int timeAtk, com.lopez.l2j.game.item.ItemInstance weapon, double targetDist) {
		if (weapon == null || weapon.template() == null) {
			return (int) (timeAtk * 0.60);
		}
		String subType = weapon.template().subType();
		if ("bow".equalsIgnoreCase(subType)) {
			int flightTime = (int) Math.min(400, Math.max(100, targetDist / 2.5));
			return Math.min(timeAtk - 50, (int) (timeAtk * 0.70) + flightTime);
		}
		if ("dagger".equalsIgnoreCase(subType)) {
			return (int) (timeAtk * 0.58);
		}
		if ("dual".equalsIgnoreCase(subType) || "dualfist".equalsIgnoreCase(subType)) {
			return (int) (timeAtk * 0.68);
		}
		if ("pole".equalsIgnoreCase(subType) || "twohandsword".equalsIgnoreCase(subType) || "twohandblunt".equalsIgnoreCase(subType)) {
			return (int) (timeAtk * 0.65);
		}
		return (int) (timeAtk * 0.62);
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

	public boolean checkAndConsumeArrow() {
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

	public void onUserCommand(int commandId) {
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

	public CommandPacketHandler commandHandler() {
		return commandHandler;
	}

	public void handleSlashCommand(String cmd) {
		commandHandler.handleSlashCommand(cmd);
	}

	public void targetByName(String query) {
		commandHandler.targetByName(query);
	}

	public void handleDotCommand(String cmd) {
		commandHandler.handleDotCommand(cmd);
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

	public AdminPacketHandler adminHandler() {
		return adminHandler;
	}

	public PlayerCharacter getTargetPlayerOrActive() {
		return adminHandler.getTargetPlayerOrActive();
	}

	public GameWorld.OnlinePlayer getTargetOnlinePlayerOrSelf() {
		return adminHandler.getTargetOnlinePlayerOrSelf();
	}

	public String resolveAdminHtml(String requested) {
		return adminHandler.resolveAdminHtml(requested);
	}

	public void showAdminHtml(String requested) {
		adminHandler.showAdminHtml(requested);
	}

	public void onGMCommand(GameClientPacket.RequestGMCommand p) {
		adminHandler.onGMCommand(p);
	}

	public void handleGMCommand(String targetName, int command) {
		adminHandler.handleGMCommand(targetName, command);
	}

	public void showAdminCharList(String query, int page) {
		adminHandler.showAdminCharList(query, page);
	}

	public void showAdminCastlesHtml(String args) {
		adminHandler.showAdminCastlesHtml(args);
	}

	public void showAdminCastlesHtml() {
		adminHandler.showAdminCastlesHtml("");
	}

	public void showAdminCharInfo(String charName) {
		adminHandler.showAdminCharInfo(charName);
	}

	public void showAdminCharInfo(PlayerCharacter target) {
		adminHandler.showAdminCharInfo(target != null ? target.name() : "");
	}

	public void showAdminSpawnIndex(int level, int page) {
		adminHandler.showAdminSpawnIndex(level, page);
	}

	public void showAdminNpcInfo(NpcInstance npc) {
		adminHandler.showAdminNpcInfo(npc);
	}

	public void showAdminDropList(int npcId) {
		adminHandler.showAdminDropList(npcId);
	}

	public void showAdminDropList(NpcInstance npc, int page) {
		adminHandler.showAdminDropList(npc != null ? npc.npcId() : 0);
	}

	public void showAdminRaidBossList(String args) {
		adminHandler.showAdminRaidBossList(args);
	}

	public void showAdminRaidBossList(int page) {
		adminHandler.showAdminRaidBossList(String.valueOf(page));
	}

	public void adminChangeClass(PlayerCharacter targetChar, int targetClassId) {
		adminHandler.adminChangeClass(targetChar, targetClassId);
	}

	public void showAdminSubClassMenu(PlayerCharacter targetChar) {
		adminHandler.showAdminSubClassMenu(targetChar);
	}

	public void adminAddSubClass(PlayerCharacter targetChar, int targetClassId) {
		adminHandler.adminAddSubClass(targetChar, targetClassId);
	}

	public void adminRemoveSubClass(PlayerCharacter targetChar, int subIndex) {
		adminHandler.adminRemoveSubClass(targetChar, subIndex);
	}

	public void adminSwitchSubClass(PlayerCharacter targetChar, int targetIndex) {
		adminHandler.adminSwitchSubClass(targetChar, targetIndex);
	}

	public void handleAdminCommand(String fullCmd) {
		adminHandler.handleAdminCommand(fullCmd);
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
		var t = ctx.characters() != null ? ctx.characters().template(active) : null;
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
		partyClanHandler.createClan(clanName);
	}

	public void increaseClanLevel() {
		partyClanHandler.increaseClanLevel();
	}

	public void dissolveClan() {
		partyClanHandler.dissolveClan();
	}

	public HennaPacketHandler hennaHandler() {
		return hennaHandler;
	}

	public int getClassLevel(PlayerCharacter player) {
		return hennaHandler.getClassLevel(player);
	}

	public void loadHennasForCurrentSubclass() {
		hennaHandler.loadHennasForCurrentSubclass();
	}

	public void onHennaList() {
		hennaHandler.handleHennaList();
	}

	public void onHennaItemInfo(int symbolId) {
		hennaHandler.handleHennaItemInfo(symbolId);
	}

	public void onHennaEquip(int symbolId) {
		hennaHandler.handleHennaEquip(symbolId);
	}

	public void onHennaUnequipList() {
		hennaHandler.handleHennaUnequipList();
	}

	public void onHennaUnequipInfo(int symbolId) {
		hennaHandler.handleHennaUnequipInfo(symbolId);
	}

	public void onHennaUnequip(int symbolId) {
		hennaHandler.handleHennaUnequip(symbolId);
	}

	public void onHennaRemoveBySlot(int slot) {
		hennaHandler.handleHennaRemoveBySlot(slot);
	}

	public void onCursedWeaponList() {
		hennaHandler.handleCursedWeaponList();
	}

	public void onCursedWeaponLocation() {
		hennaHandler.handleCursedWeaponLocation();
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

	public SocialPacketHandler socialHandler() {
		return socialHandler;
	}

	public int pendingFriendInviteFrom() {
		return pendingFriendInviteFrom;
	}

	public void pendingFriendInviteFrom(int pendingFriendInviteFrom) {
		this.pendingFriendInviteFrom = pendingFriendInviteFrom;
	}

	public void onFriendList() {
		socialHandler.handleFriendList();
	}

	public void onFriendInvite(GameClientPacket.RequestFriendInvite p) {
		socialHandler.handleFriendInvite(p);
	}

	public void onAnswerFriendInvite(GameClientPacket.RequestAnswerFriendInvite p) {
		socialHandler.handleAnswerFriendInvite(p);
	}

	public void onFriendDel(GameClientPacket.RequestFriendDel p) {
		socialHandler.handleFriendDel(p);
	}

	public void onBlock(GameClientPacket.RequestBlock p) {
		socialHandler.handleBlock(p);
	}

	public static int parseIntSafe(String val, int def) {
		try {
			return Integer.parseInt(val.trim());
		} catch (Exception e) {
			return def;
		}
	}

	public static long parseLongSafe(String val, long def) {
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
				if (info != null && ctx.world() != null) {
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
		send(com.lopez.l2j.network.game.packet.GameServerPacket.StatusUpdate.forPlayer(active));
		var info = charInfo();
		if (info != null && ctx.world() != null) {
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
		var aug = item.augmentation();
		int stat12 = aug != null ? aug.stat12() : 0;
		int stat34 = aug != null ? aug.stat34() : 0;
		send(new ExPutItemResultForVariationCancel(p.itemObjId(), stat12, stat34, price));
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
		var resAdena = ctx.inventories().consumeItem(active.inventory(), 57, (int) price, "RefineCancel");
		ctx.augmentation().removeAugmentation(target);
		send(new ExVariationCancelResult(1));
		List<ItemInfo> updates = new ArrayList<>();
		updates.add(ItemInfo.of(target, ItemInfo.MODIFIED));
		if (resAdena != null) {
			updates.add(ItemInfo.of(resAdena.item(), resAdena.removed() ? ItemInfo.REMOVED : ItemInfo.MODIFIED));
		}
		send(new InventoryUpdate(updates));
		if (wasEquipped) {
			afterEquipChange(ctx.inventories().toggleEquip(active.inventory(), target.objectId()));
		}
		sendUserInfoAndBroadcastCharInfo();
		send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Augmentacao removida com sucesso."));
	}
}
