package com.lopez.l2j.network.game;

import com.lopez.l2j.game.item.ItemSlots;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.npc.SpawnService;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.template.CharTemplate;
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
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestUnEquipItem;
import com.lopez.l2j.network.game.packet.GameClientPacket.Say2;
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
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestShortCutReg;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestShortCutDel;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestMagicSkillUse;
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
import com.lopez.l2j.network.game.packet.GameServerPacket.BuyList;
import com.lopez.l2j.network.game.packet.GameServerPacket.SellList;
import com.lopez.l2j.network.game.packet.GameServerPacket.ExShowVariationMakeWindow;
import com.lopez.l2j.network.game.packet.GameServerPacket.ExShowVariationCancelWindow;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maquina de estados de UMA conexao de jogo (porta enxuta de L2GameClient + clientpackets). Nao conhece
 * sockets: recebe o corpo decifrado e envia respostas pelo {@code sink} (que precisa ser thread-safe, pois
 * outros jogadores tambem enviam por ele - ex.: chat). {@link #handle} deve ser chamado por uma unica
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
			com.lopez.l2j.config.ServerProperties.Rates rates, String serverName) {
		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat,
				com.lopez.l2j.game.drop.DropService drops, ShortCutRepository shortcuts,
				SkillRepository skills, com.lopez.l2j.game.ai.NpcAiService npcAi, SkillService skillService,
				com.lopez.l2j.game.multisell.MultiSellTable multisell,
				com.lopez.l2j.game.service.WarehouseService warehouse,
				com.lopez.l2j.game.effect.CharacterSkillSaveRepository buffRepository,
				com.lopez.l2j.config.ServerProperties.Rates rates, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists, combat, drops, shortcuts, skills, npcAi, skillService, multisell, warehouse, buffRepository, null, List.of(), rates, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, null, null, null, null, null, null, null, null, null, null, null, null, null, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, null, null, null, null, null, null, null, null, null, null, null, null, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists, null, null, null, null, null, null, null, null, null, null, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists, combat, null, null, null, null, null, null, null, null, null, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat,
				com.lopez.l2j.game.drop.DropService drops, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists, combat, drops, null, null, null, null, null, null, null, null, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat,
				com.lopez.l2j.game.drop.DropService drops, ShortCutRepository shortcuts,
				SkillRepository skills, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists, combat, drops, shortcuts, skills, null, null, null, null, null, null, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat,
				com.lopez.l2j.game.drop.DropService drops, ShortCutRepository shortcuts,
				SkillRepository skills, com.lopez.l2j.game.ai.NpcAiService npcAi, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists, combat, drops, shortcuts, skills, npcAi, null, null, null, null, null, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat,
				com.lopez.l2j.game.drop.DropService drops, ShortCutRepository shortcuts,
				SkillRepository skills, com.lopez.l2j.game.ai.NpcAiService npcAi, SkillService skillService,
				String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists, combat, drops, shortcuts, skills, npcAi, skillService, null, null, null, null, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat,
				com.lopez.l2j.game.drop.DropService drops, ShortCutRepository shortcuts,
				SkillRepository skills, com.lopez.l2j.game.ai.NpcAiService npcAi, SkillService skillService,
				com.lopez.l2j.config.ServerProperties.Rates rates, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists, combat, drops, shortcuts, skills, npcAi, skillService, null, null, null, rates, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat,
				com.lopez.l2j.game.drop.DropService drops, ShortCutRepository shortcuts,
				SkillRepository skills, com.lopez.l2j.game.ai.NpcAiService npcAi, SkillService skillService,
				com.lopez.l2j.game.multisell.MultiSellTable multisell,
				com.lopez.l2j.config.ServerProperties.Rates rates, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists, combat, drops, shortcuts, skills, npcAi, skillService, multisell, null, null, rates, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat,
				com.lopez.l2j.game.drop.DropService drops, ShortCutRepository shortcuts,
				SkillRepository skills, com.lopez.l2j.game.ai.NpcAiService npcAi, SkillService skillService,
				com.lopez.l2j.game.multisell.MultiSellTable multisell,
				com.lopez.l2j.game.service.WarehouseService warehouse,
				com.lopez.l2j.config.ServerProperties.Rates rates, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists, combat, drops, shortcuts, skills, npcAi, skillService, multisell, warehouse, null, rates, serverName);
		}
	}

	private final Context ctx;
	private final byte[] cryptKey;
	private final Consumer<GameServerPacket> sink;
	private final String ip;

	private State state = State.CONNECTED;
	private boolean protocolOk;
	private boolean closeRequested;
	private String account;
	private int sessionId;
	private List<PlayerCharacter> characterList = List.of();
	private PlayerCharacter active;
	private boolean inWorld;
	private int targetObjectId;
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
	private volatile int pendingNpcInteractObjectId;
	private volatile int activeEnchantScrollObjectId;

	record RequestPartyPending(GameSession requester, int itemDistribution) {}

	private static final ScheduledExecutorService autoAttackScheduler = Executors.newScheduledThreadPool(4, r -> {
		Thread th = new Thread(r, "PlayerAutoAttack");
		th.setDaemon(true);
		return th;
	});

	public GameSession(Context ctx, byte[] cryptKey, String ip, Consumer<GameServerPacket> sink) {
		this.ctx = ctx;
		this.cryptKey = cryptKey.clone();
		this.ip = ip;
		this.sink = sink;
	}

	public State state() {
		return state;
	}

	public boolean inWorld() {
		return inWorld;
	}

	public String account() {
		return account;
	}

	public PlayerCharacter activeCharacter() {
		return active;
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

	/** Processa um pacote; devolve false quando a conexao deve ser fechada. */
	public boolean handle(byte[] body) {
		var decoded = GameClientPacket.decode(state, body);
		if (decoded.isEmpty()) {
			log.debug("Pacote malformado de {} no estado {}; fechando", ip, state);
			return false;
		}
		switch (decoded.get()) {
			case ProtocolVersion p -> onProtocolVersion(p);
			case AuthLogin p -> onAuthLogin(p);
			case NewCharacter p -> send(new NewCharacterSuccess(ctx.characters().creationTemplates()));
			case CharacterCreate p -> onCharacterCreate(p);
			case CharacterDelete p -> onCharacterDelete(p);
			case CharacterRestore p -> onCharacterRestore(p);
			case CharacterSelect p -> onCharacterSelect(p);
			case RequestManorList p -> send(new ExSendManorList(ExSendManorList.CASTLES));
			case EnterWorld p -> onEnterWorld();
			case MoveBackwardToLocation p -> onMove(p);
			case ValidatePosition p -> onValidatePosition(p);
			case Say2 p -> onSay(p);
			case Action p -> onAction(p);
			case AttackRequest p -> onAttackRequest(p);
			case RequestBypassToServer p -> onBypass(p);
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
			case RequestMagicSkillUse p -> onMagicSkillUse(p);
			case RequestAutoSoulShot p -> onAutoSoulShot(p);
			case RequestSkillList p -> sendSkillList();
			case GameClientPacket.RequestAcquireSkillInfo p -> onAcquireSkillInfo(p);
			case GameClientPacket.RequestAcquireSkill p -> onAcquireSkill(p);
			case RequestQuestList p -> send(new QuestList());
			case Logout p -> onLogout();
			case RequestRestart p -> onRestart();
			case GameClientPacket.RequestExEnchantSkillInfo p -> log.debug("RequestExEnchantSkillInfo recebido: {}", p);
			case GameClientPacket.RequestExEnchantSkill p -> log.debug("RequestExEnchantSkill recebido: {}", p);
			case GameClientPacket.RequestJoinParty p -> onJoinParty(p);
			case GameClientPacket.RequestAnswerJoinParty p -> onAnswerJoinParty(p);
			case GameClientPacket.RequestSocialAction p -> onSocialAction(p);
			case GameClientPacket.RequestWithDrawalParty p -> onLeaveParty();
			case GameClientPacket.RequestOustPartyMember p -> onExpelPartyMember(p.name());
			case GameClientPacket.RequestShowMiniMap p -> onShowMiniMap();
			case GameClientPacket.RequestRestartPoint p -> onRestartPoint(p);
			case GameClientPacket.RequestUserCommand p -> onUserCommand(p.commandId());
			case GameClientPacket.MultiSellChoose p -> onMultiSellChoose(p);
			case RequestEnchantItem p -> onEnchantItem(p);
			case SendWareHouseDepositList p -> onWareHouseDeposit(p);
			case SendWareHouseWithDrawList p -> onWareHouseWithdraw(p);
			case RequestDestroyItem p -> onDestroyItem(p);
			case GameClientPacket.RequestGMCommand p -> onGMCommand(p);
			case Unknown p -> log.debug("Opcode ignorado 0x{}{} no estado {}", Integer.toHexString(p.opcode()),
					p.subOpcode() >= 0 ? ":" + Integer.toHexString(p.subOpcode()) : "", state);
		}
		return !closeRequested;
	}

	/** Chame quando a conexao cair, por qualquer motivo. */
	public void onDisconnect() {
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
			closeRequested = true;
			return;
		}
		if (p.version() < ctx.protocolMin() || p.version() > ctx.protocolMax()) {
			log.info("Cliente {} recusado: protocolo {} fora de {}-{}", ip, p.version(), ctx.protocolMin(),
					ctx.protocolMax());
			send(new KeyPacket(cryptKey, false));
			closeRequested = true;
			return;
		}
		protocolOk = true;
		send(new KeyPacket(cryptKey, true));
	}

	private void onAuthLogin(AuthLogin p) {
		if (!protocolOk || account != null) {
			closeRequested = true;
			return;
		}
		var key = new SessionKey(p.loginOk1(), p.loginOk2(), p.playOk1(), p.playOk2());
		if (!ctx.sessionKeys().claim(p.account(), key)) {
			log.warn("AuthLogin recusado para {} de {}: chave de sessao invalida ou expirada", p.account(), ip);
			closeRequested = true;
			return;
		}
		account = p.account();
		sessionId = p.playOk1();
		state = State.AUTHED;
		log.info("{} entrou no game server de {}", account, ip);
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
		int accountAccess = (ctx.sessionKeys() != null && account != null)
				? ctx.sessionKeys().getAccessLevel(account)
				: 0;
		if (accountAccess > 0 && c.accessLevel() < accountAccess) {
			c.accessLevel(accountAccess);
		}

		if (ctx.adminSuperusers() != null && !ctx.adminSuperusers().isEmpty()) {
			for (String su : ctx.adminSuperusers()) {
				if (su != null && (su.equalsIgnoreCase(c.name()) || su.equalsIgnoreCase(account))) {
					if (c.accessLevel() < 100) {
						c.accessLevel(100);
					}
					break;
				}
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
		send(new SsqInfo(0));
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
		send(new ShortCutInit(ctx.shortcuts() != null ? ctx.shortcuts().findByCharId(active.objectId(), 0) : List.of()));
		send(new HennaInfo());
		send(new QuestList());
		send(new EtcStatusUpdate());
		send(ExStorageMaxCount.defaults());
		send(new FriendList());
		send(new UserInfo(active, t));
		sendSkillList();
		send(new ClientSetTime(GameTime.now()));
		ctx.world().add(this);
		ctx.characters().save(active, true);
		restoreBuffs();
		startVitalsRegenTask();
		updateKnownObjects();
		if (active.isGm()) {
			send(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "SYS",
					"Privilegios de Administrador (GM Level " + active.accessLevel() + ") ativos. Digite //admin"));
		}
		send(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, ctx.serverName(),
				"Bem-vindo ao " + ctx.serverName() + ", " + active.name() + "!"));
		log.info("{} ({}) entrou no mundo em {},{},{}", active.name(), account, x(), y(), z());
	}

	private void onMove(MoveBackwardToLocation p) {
		if (!inWorld || active.sitting() || active.isDisabled() || active.isRooted()) {
			send(new ActionFailed());
			return;
		}
		pendingNpcInteractObjectId = 0;
		// Cancela auto-attack apenas se for movimento manual no chão (moveMovement != 0)
		if (p.moveMovement() != 0) {
			stopAutoAttack();
		}

		if (p.targetX() == p.originX() && p.targetY() == p.originY() && p.targetZ() == p.originZ()) {
			send(new StopMove(active.objectId(), x(), y(), z(), active.heading()));
			return;
		}
		// Sem geodata/simulacao de movimento ainda: confiamos na origem do cliente e no ValidatePosition.
		active.moveTo(p.originX(), p.originY(), p.originZ());
		var move = new MoveToLocation(active.objectId(), p.targetX(), p.targetY(), p.targetZ(), p.originX(),
				p.originY(), p.originZ());
		send(move);
		ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, move, false);
		updateKnownObjects();
	}

	private void onValidatePosition(ValidatePosition p) {
		if (!inWorld || (p.x() == 0 && p.y() == 0)) {
			return;
		}
		active.moveTo(p.x(), p.y(), p.z());
		active.heading(p.heading());
		ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS,
				new ValidateLocation(active.objectId(), p.x(), p.y(), p.z(), p.heading()), false);
		updateKnownObjects();
		checkPendingNpcInteract();
		checkAutoAttackRangeOnMove();
	}

	private void updateKnownObjects() {
		if (!inWorld || active == null) {
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
				}
			}
		}

		// 3. Remove objetos que sairam do alcance
		Iterator<Integer> it = knownObjects.iterator();
		while (it.hasNext()) {
			int id = it.next();
			if (!currentAround.contains(id)) {
				it.remove();
				send(new DeleteObject(id));
			}
		}
	}

	private void onSay(Say2 p) {
		if (!inWorld || p.text() == null || p.text().isBlank()) {
			return;
		}
		String raw = p.text().length() > MAX_CHAT_LENGTH ? p.text().substring(0, MAX_CHAT_LENGTH) : p.text();
		raw = raw.trim();

		if (raw.startsWith("//")) {
			handleAdminCommand(raw.substring(2).trim());
			return;
		}
		if (raw.startsWith("/")) {
			handleSlashCommand(raw);
			return;
		}
		if (raw.startsWith(".")) {
			handleDotCommand(raw);
			return;
		}

		int channel = p.channel();
		String text = raw;

		// Detecta e extrai prefixos digitados na caixa principal de chat
		if (raw.startsWith("!")) {
			channel = CreatureSay.SHOUT;
			text = raw.substring(1).trim();
		} else if (raw.startsWith("+")) {
			channel = CreatureSay.TRADE;
			text = raw.substring(1).trim();
		} else if (raw.startsWith("%")) {
			channel = CreatureSay.HERO;
			text = raw.substring(1).trim();
		} else if (raw.startsWith("#")) {
			channel = CreatureSay.PARTY;
			text = raw.substring(1).trim();
		} else if (raw.startsWith("@")) {
			channel = CreatureSay.CLAN;
			text = raw.substring(1).trim();
		} else if (raw.startsWith("$")) {
			channel = CreatureSay.ALLIANCE;
			text = raw.substring(1).trim();
		}

		if (text.isBlank()) {
			return;
		}

		switch (channel) {
			case CreatureSay.SHOUT -> ctx.world().broadcast(
					new CreatureSay(active.objectId(), CreatureSay.SHOUT, active.name(), text), x -> true);
			case CreatureSay.TRADE -> ctx.world().broadcast(
					new CreatureSay(active.objectId(), CreatureSay.TRADE, active.name(), text), x -> true);
			case CreatureSay.HERO -> ctx.world().broadcast(
					new CreatureSay(active.objectId(), CreatureSay.HERO, active.name(), text), x -> true);
			case CreatureSay.PARTY -> {
				if (party != null) {
					party.broadcast(new CreatureSay(active.objectId(), CreatureSay.PARTY, active.name(), text));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "System", "Voce nao esta em uma party."));
				}
			}
			case CreatureSay.CLAN -> {
				if (active.clanId() > 0) {
					ctx.world().broadcast(
							new CreatureSay(active.objectId(), CreatureSay.CLAN, active.name(), text),
							s -> s.character().clanId() == active.clanId());
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "System", "Voce nao esta em um cla."));
				}
			}
			case CreatureSay.ALLIANCE -> {
				if (active.clanId() > 0) {
					ctx.world().broadcast(
							new CreatureSay(active.objectId(), CreatureSay.ALLIANCE, active.name(), text),
							s -> s.character().clanId() == active.clanId());
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "System", "Voce nao esta em uma alianca."));
				}
			}
			case CreatureSay.TELL -> {
				var target = ctx.world().byName(p.target());
				if (target.isEmpty()) {
					send(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, ctx.serverName(),
							p.target() + " nao esta online."));
					return;
				}
				target.get().send(new CreatureSay(active.objectId(), CreatureSay.TELL, active.name(), text));
				send(new CreatureSay(active.objectId(), CreatureSay.TELL, "->" + target.get().name(), text));
			}
			default -> ctx.world().broadcastAround(this, GameWorld.LOCAL_CHAT_RANGE,
					new CreatureSay(active.objectId(), CreatureSay.ALL, active.name(), text));
		}
	}

	private int getPhysicalAttackRange(PlayerCharacter player) {
		var weapon = player.inventory().paperdoll(ItemSlots.RHAND);
		if (weapon == null) {
			weapon = player.inventory().paperdoll(ItemSlots.LRHAND);
		}
		if (weapon != null && "bow".equalsIgnoreCase(weapon.template().subType())) {
			return 500;
		}
		return 40;
	}

	private void onAction(Action p) {
		if (!inWorld) {
			send(new ActionFailed());
			return;
		}
		if (p.objectId() == active.objectId()) {
			targetObjectId = active.objectId();
			send(new MyTargetSelected(active.objectId(), 0));
			send(StatusUpdate.hp(active.objectId(), (int) active.currentHp(), active.maxHp()));
			return;
		}
		var npcOpt = ctx.world().npc(p.objectId());
		if (npcOpt.isPresent()) {
			var npc = npcOpt.get();
			if (targetObjectId == npc.objectId()) {
				// 2º clique: se atacavel, inicia ataque/perseguicao; senao abre dialogo
				if (npc.template().isAttackable()) {
					startAutoAttack(npc);
				} else {
					double dx = active.x() - npc.x();
					double dy = active.y() - npc.y();
					double distSq = dx * dx + dy * dy;
					double interactDist = 150.0;
					if (distSq > interactDist * interactDist) {
						pendingNpcInteractObjectId = npc.objectId();
						var movePawn = new MoveToPawn(active.objectId(), npc.objectId(), 60, active.x(), active.y(), active.z());
						send(movePawn);
						ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, movePawn, false);
						return;
					}
					pendingNpcInteractObjectId = 0;
					showNpcHtml(npc, 0);
				}
			} else {
				// 1º clique: seleciona NPC como alvo e envia StatusUpdate com HP atual/maximo
				targetObjectId = npc.objectId();
				int levelDiff = active.level() - npc.template().level();
				send(new MyTargetSelected(npc.objectId(), levelDiff));
				send(StatusUpdate.hp(npc.objectId(), (int) npc.currentHp(), npc.template().maxHp()));
				send(new ValidateLocation(npc.objectId(), npc.x(), npc.y(), npc.z(), npc.heading()));
			}
			return;
		}
		var playerOpt = ctx.world().player(p.objectId());
		if (playerOpt.isPresent()) {
			var other = playerOpt.get();
			targetObjectId = other.objectId();
			send(new MyTargetSelected(other.objectId(), 0));
			if (other.character() != null) {
				send(StatusUpdate.hp(other.objectId(), (int) other.character().currentHp(), other.character().maxHp()));
			}
			send(new ValidateLocation(other.objectId(), other.x(), other.y(), other.z(), 0));
			return;
		}
		targetObjectId = 0;
		send(new ActionFailed());
	}

	private void onAttackRequest(AttackRequest p) {
		if (!inWorld || active == null || active.isDead() || active.sitting() || active.isDisabled()) {
			send(new ActionFailed());
			return;
		}
		var npcOpt = ctx.world().npc(p.objectId());
		if (npcOpt.isPresent()) {
			var npc = npcOpt.get();
			if (!npc.template().isAttackable() || npc.isDead()) {
				send(new ActionFailed());
				return;
			}
			if (targetObjectId != npc.objectId()) {
				targetObjectId = npc.objectId();
				int levelDiff = active.level() - npc.template().level();
				send(new MyTargetSelected(npc.objectId(), levelDiff));
				send(StatusUpdate.hp(npc.objectId(), (int) npc.currentHp(), npc.template().maxHp()));
				send(new ValidateLocation(npc.objectId(), npc.x(), npc.y(), npc.z(), npc.heading()));
			}
			startAutoAttack(npc);
			return;
		}
		send(new ActionFailed());
	}

	private void startAutoAttack(NpcInstance npc) {
		if (npc == null || npc.isDead() || !npc.template().isAttackable()) {
			send(new ActionFailed());
			return;
		}
		if (!autoAttacking) {
			autoAttacking = true;
			var startAtk = new AutoAttackStart(active.objectId());
			send(startAtk);
			ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, startAtk, false);
		}

		double dx = active.x() - npc.x();
		double dy = active.y() - npc.y();
		double distSq = dx * dx + dy * dy;
		int attackRange = getPhysicalAttackRange(active);
		double maxDist = attackRange + 45.0;

		if (distSq > maxDist * maxDist) {
			var movePawn = new MoveToPawn(active.objectId(), npc.objectId(), attackRange, active.x(), active.y(), active.z());
			send(movePawn);
			ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, movePawn, false);
			schedulePlayerAutoAttack(npc, 200);
		} else {
			onAttackNpc(npc);
		}
	}

	private void stopAutoAttack() {
		if (autoAttacking) {
			autoAttacking = false;
			if (active != null) {
				var stopAtk = new AutoAttackStop(active.objectId());
				send(stopAtk);
				ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, stopAtk, false);
			}
		}
	}

	private void checkAutoAttackRangeOnMove() {
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
		}
	}

	private void onCancelTarget() {
		targetObjectId = 0;
		if (active != null) {
			send(new TargetUnselected(active.objectId(), x(), y(), z()));
			stopAutoAttack();
		}
	}

	private void onAttackNpc(NpcInstance npc) {
		if (npc.isDead()) {
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

		if (!autoAttacking) {
			autoAttacking = true;
			var startAtk = new AutoAttackStart(active.objectId());
			send(startAtk);
			ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, startAtk, false);
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

		// Agenda a aplicacao do dano e atualizacoes no momento exato do impacto (timeToHit)
		autoAttackScheduler.schedule(() -> {
			if (!inWorld || active == null || npc == null) {
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

			var su = StatusUpdate.hp(npc.objectId(), hit.remainingHp(), hit.maxHp());
			send(su);
			ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, su, false);

			if (hit.isDead()) {
				stopAutoAttack();

				if (ctx.npcAi() != null) {
					ctx.npcAi().stopCombat(npc);
					ctx.npcAi().scheduleDecayAndRespawn(npc);
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

				if (ctx.drops() != null) {
					ctx.drops().rewardMonsterDeath(active, npc.npcId(), ctx.inventories(), this::send);
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
			if (!autoAttacking || active == null || !inWorld || targetObjectId != npc.objectId() || npc.isDead()) {
				return;
			}
			double dx = active.x() - npc.x();
			double dy = active.y() - npc.y();
			double distSq = dx * dx + dy * dy;
			int attackRange = getPhysicalAttackRange(active);
			double maxDist = attackRange + 45.0;
			if (distSq > maxDist * maxDist) {
				var movePawn = new MoveToPawn(active.objectId(), npc.objectId(), attackRange, active.x(), active.y(), active.z());
				send(movePawn);
				ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, movePawn, false);
				schedulePlayerAutoAttack(npc, 250);
			} else {
				onAttackNpc(npc);
			}
		}, delayMs, TimeUnit.MILLISECONDS);
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
				if (ctx.skillService() == null) {
					active.maxHp(t.calculateMaxHp(newLevel));
					active.maxMp(t.calculateMaxMp(newLevel));
					active.maxCp(t.calculateMaxCp(newLevel));
				}
			}
			active.currentHp(active.maxHp());
			active.currentMp(active.maxMp());
			active.currentCp(active.maxCp());

			var social = new SocialAction(active.objectId(), 15);
			send(social);
			ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, social, false);

			send(SystemMessage.of(SystemMessage.YOU_INCREASED_YOUR_LEVEL));

			send(new StatusUpdate(active.objectId(), List.of(
					new StatusUpdate.Attribute(StatusUpdate.LEVEL, newLevel),
					new StatusUpdate.Attribute(StatusUpdate.SP, active.sp()),
					new StatusUpdate.Attribute(StatusUpdate.CUR_HP, (int) active.currentHp()),
					new StatusUpdate.Attribute(StatusUpdate.MAX_HP, active.maxHp()),
					new StatusUpdate.Attribute(StatusUpdate.CUR_MP, (int) active.currentMp()),
					new StatusUpdate.Attribute(StatusUpdate.MAX_MP, active.maxMp()),
					new StatusUpdate.Attribute(StatusUpdate.CUR_CP, (int) active.currentCp()),
					new StatusUpdate.Attribute(StatusUpdate.MAX_CP, active.maxCp())
			)));
		} else {
			send(new StatusUpdate(active.objectId(), List.of(
					new StatusUpdate.Attribute(StatusUpdate.SP, active.sp())
			)));
		}
		if (t != null) {
			send(new UserInfo(active, t));
		}
	}

	private void showNpcHtml(NpcInstance npc, int val) {
		if (ctx.htmls() == null) {
			return;
		}
		String raw = ctx.htmls().getNpcHtml(npc.npcId(), npc.template().type(), val);
		String rendered = ctx.htmls().render(raw, npc.objectId(), npc.name(), active.name());
		send(new NpcHtmlMessage(npc.objectId(), rendered));
	}

	private void onBypass(RequestBypassToServer p) {
		if (!inWorld || p.command() == null || p.command().isBlank()) {
			send(new ActionFailed());
			return;
		}
		String cmd = p.command().trim();
		if (cmd.startsWith("-h ")) {
			cmd = cmd.substring(3).trim();
		} else if (cmd.startsWith("-h")) {
			cmd = cmd.substring(2).trim();
		}
		if (cmd.startsWith("admin_")) {
			handleAdminCommand(cmd.substring(6).trim());
			return;
		}
		if (cmd.startsWith("Link ")) {
			String path = cmd.substring(5).trim();
			if (path.startsWith("/")) {
				path = path.substring(1);
			}
			if (ctx.htmls() != null) {
				String htm = ctx.htmls().getHtml(path);
				if (htm == null) {
					htm = ctx.htmls().getIndexedHtml(path);
				}
				if (htm == null) {
					htm = ctx.htmls().getHtml("default/" + path);
				}
				if (htm != null) {
					int npcObjId = active != null ? active.objectId() : 0;
					String rendered = ctx.htmls().render(htm, npcObjId, "NPC", active != null ? active.name() : "Player");
					send(new NpcHtmlMessage(npcObjId, rendered));
					return;
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
									String rendered = ctx.htmls().render(htm, npc.objectId(), npc.name(), active.name());
									send(new NpcHtmlMessage(npc.objectId(), rendered));
									return;
								}
							}
							showNpcHtml(npc, 0);
							return;
						} else if (action.startsWith("Quest")) {
							String questArg = action.length() > 5 ? action.substring(5).trim() : "";
							if (questArg.startsWith("1101_teleport_to_race_track")) {
								teleportToCoordinates(12661, 181687, -3560);
								return;
							}
							if (!questArg.isEmpty() && ctx.htmls() != null) {
								String qHtml = ctx.htmls().getHtml("teleporter/" + questArg + ".htm");
								if (qHtml == null) {
									qHtml = ctx.htmls().getHtml("default/" + questArg + ".htm");
								}
								if (qHtml == null) {
									qHtml = ctx.htmls().getIndexedHtml(questArg);
								}
								if (qHtml != null) {
									send(new NpcHtmlMessage(npc.objectId(), ctx.htmls().render(qHtml, npc.objectId(), npc.name(), active.name())));
									return;
								}
							}
							showNpcHtml(npc, 0);
							return;
						} else if (action.startsWith("SkillList")) {
							showSkillList(npc);
							return;
						} else if (action.startsWith("goto")) {
							try {
								int teleId = Integer.parseInt(action.substring(4).trim());
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
								int listId = Integer.parseInt(listIdStr);
								showMultiSell(npc, listId);
								return;
							} catch (NumberFormatException ignored) {
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
									? active.inventory().items().stream().filter(it -> !it.isEquipped() && it.template().type2() != ItemTemplate.TYPE2_QUEST).toList()
									: ctx.warehouse().getWarehouseItems(active.objectId());
							send(action.startsWith("DepositF")
									? new WareHouseDepositList(1, (int) active.inventory().adena(), items)
									: new WareHouseWithdrawalList(1, (int) active.inventory().adena(), items));
							return;
						} else if (action.startsWith("TerritoryStatus")) {
							if (ctx.htmls() != null) {
								String tHtml = ctx.htmls().getHtml("territorystatus.htm");
								if (tHtml != null) {
									String rendered = ctx.htmls().render(tHtml, npc.objectId(), npc.name(), active.name())
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
						}
					}
				} catch (NumberFormatException ignored) {
				}
			}
		}
		send(new ActionFailed());
	}

	private void teleportTo(int teleId) {
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
		int price = loc.price();
		int costItem = loc.forNoble() ? 6651 : ItemTemplate.ADENA_ID;

		if (price > 0) {
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

	private void teleportToCoordinates(int targetX, int targetY, int targetZ) {
		active.moveTo(targetX, targetY, targetZ);
		ctx.characters().save(active, true);
		send(new TeleportToLocation(active.objectId(), targetX, targetY, targetZ));
		updateKnownObjects();
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

	private void onBuyItem(RequestBuyItem p) {
		if (!inWorld || p.items().isEmpty()) {
			send(new ActionFailed());
			return;
		}
		if (ctx.buylists() == null) {
			send(new ActionFailed());
			return;
		}
		var blOpt = ctx.buylists().get(p.listId());
		if (blOpt.isEmpty()) {
			send(new ActionFailed());
			return;
		}
		var bl = blOpt.get();
		long totalCost = 0;
		int slots = 0;

		for (var itemReq : p.items()) {
			var prodOpt = bl.getProduct(itemReq.itemId());
			if (prodOpt.isEmpty()) {
				send(new ActionFailed());
				return;
			}
			var prod = prodOpt.get();
			var template = ctx.inventories().templates().get(prod.itemId()).orElse(null);
			if (template == null) {
				send(new ActionFailed());
				return;
			}
			totalCost += (long) prod.price() * itemReq.count();
			if (!template.stackable()) {
				slots += itemReq.count();
			} else if (active.inventory().byItemId(prod.itemId()).isEmpty()) {
				slots++;
			}
		}

		if (totalCost > Integer.MAX_VALUE || totalCost < 0) {
			send(new ActionFailed());
			return;
		}

		if (active.inventory().adena() < totalCost) {
			send(SystemMessage.id(SystemMessage.YOU_NOT_ENOUGH_ADENA));
			send(new ActionFailed());
			return;
		}

		if (active.inventory().size() + slots > 80) {
			send(SystemMessage.id(SystemMessage.SLOTS_FULL));
			send(new ActionFailed());
			return;
		}

		List<ItemInfo> updates = new ArrayList<>();
		if (totalCost > 0) {
			var consumed = ctx.inventories().consumeItem(active.inventory(), ItemTemplate.ADENA_ID, (int) totalCost,
					"Buy");
			if (consumed != null) {
				updates.add(consumed.removed()
						? ItemInfo.of(consumed.item(), ItemInfo.REMOVED)
						: ItemInfo.of(consumed.item(), ItemInfo.MODIFIED));
			}
		}

		for (var itemReq : p.items()) {
			var added = ctx.inventories().addItem(active.inventory(), itemReq.itemId(), itemReq.count(), "Buy");
			if (added != null) {
				updates.add(ItemInfo.of(added.item(), added.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED));
			}
		}

		send(new InventoryUpdate(updates));
		send(new UserInfo(active, ctx.characters().template(active)));
	}

	private void showSellList(NpcInstance npc) {
		if (active == null) {
			return;
		}
		var sellable = active.inventory().items().stream()
				.filter(it -> !it.isEquipped() && it.template().type2() != ItemTemplate.TYPE2_QUEST && it.template().price() > 0)
				.map(it -> new SellList.SellItemView(
						it.objectId(),
						it.itemId(),
						(int) it.count(),
						it.template().type1(),
						it.template().type2(),
						it.template().bodyPart(),
						it.enchant(),
						Math.max(1, it.template().price() / 2)))
				.toList();
		send(new SellList((int) active.inventory().adena(), 0, sellable));
	}

	private void onSellItem(RequestSellItem p) {
		if (!inWorld || p.items().isEmpty()) {
			send(new ActionFailed());
			return;
		}
		long totalEarned = 0;
		List<ItemInfo> updates = new ArrayList<>();
		for (var req : p.items()) {
			var itOpt = active.inventory().byObjectId(req.objectId());
			if (itOpt.isEmpty()) {
				continue;
			}
			var item = itOpt.get();
			if (item.isEquipped() || item.template().type2() == ItemTemplate.TYPE2_QUEST) {
				continue;
			}
			int count = Math.min((int) item.count(), Math.max(1, req.count()));
			int pricePerItem = Math.max(1, item.template().price() / 2);
			totalEarned += (long) pricePerItem * count;
			var upd = ctx.inventories().destroyItem(active.inventory(), item.objectId(), count, "Sell");
			if (upd != null) {
				updates.add(upd.removed()
						? ItemInfo.of(upd.item(), ItemInfo.REMOVED)
						: ItemInfo.of(upd.item(), ItemInfo.MODIFIED));
			}
		}
		if (totalEarned > 0) {
			var adenaUpd = ctx.inventories().addItem(active.inventory(), ItemTemplate.ADENA_ID, (int) totalEarned, "SellReward");
			if (adenaUpd != null) {
				updates.add(ItemInfo.of(adenaUpd.item(), adenaUpd.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED));
			}
		}
		if (!updates.isEmpty()) {
			send(new InventoryUpdate(updates));
			send(ItemList.of(active.inventory().items(), false));
			send(new UserInfo(active, ctx.characters().template(active)));
		}
	}

	private void showMultiSell(NpcInstance npc, int listId) {
		if (ctx.multisell() == null) {
			send(new ActionFailed());
			return;
		}
		var containerOpt = ctx.multisell().get(listId);
		if (containerOpt.isEmpty()) {
			log.warn("MultiSell id {} nao encontrada", listId);
			send(new ActionFailed());
			return;
		}
		var container = containerOpt.get();
		List<MultiSellList.MultiSellEntryView> views = new ArrayList<>();
		for (var entry : container.entries()) {
			List<MultiSellList.ItemView> ingredients = new ArrayList<>();
			for (var ing : entry.ingredients()) {
				var t = ctx.inventories().templates().get(ing.itemId()).orElse(null);
				int type2 = t != null ? t.type2() : 0;
				ingredients.add(new MultiSellList.ItemView(ing.itemId(), 0, type2, ing.count(), ing.enchantLevel()));
			}
			List<MultiSellList.ItemView> products = new ArrayList<>();
			for (var prod : entry.products()) {
				var t = ctx.inventories().templates().get(prod.itemId()).orElse(null);
				int bodyPart = t != null ? t.bodyPart() : 0;
				int type2 = t != null ? t.type2() : 0;
				products.add(new MultiSellList.ItemView(prod.itemId(), bodyPart, type2, prod.count(), prod.enchantLevel()));
			}
			views.add(new MultiSellList.MultiSellEntryView(entry.entryId(), ingredients, products));
		}
		send(new MultiSellList(listId, 1, 1, 40, views));
	}

	private void onMultiSellChoose(GameClientPacket.MultiSellChoose p) {
		if (!inWorld || active == null || p.amount() <= 0 || ctx.multisell() == null) {
			send(new ActionFailed());
			return;
		}
		var containerOpt = ctx.multisell().get(p.listId());
		if (containerOpt.isEmpty()) {
			send(new ActionFailed());
			return;
		}
		var container = containerOpt.get();
		var entryOpt = container.entries().stream().filter(e -> e.entryId() == p.entryId()).findFirst();
		if (entryOpt.isEmpty()) {
			send(new ActionFailed());
			return;
		}
		var entry = entryOpt.get();
		int amount = Math.min(5000, p.amount());

		// 1. Verifica se o jogador possui todos os ingredientes na quantidade necessaria
		for (var ing : entry.ingredients()) {
			long needed = ing.count() * amount;
			long count = active.inventory().byItemId(ing.itemId()).map(i -> (long) i.count()).orElse(0L);
			if (count < needed) {
				send(SystemMessage.id(SystemMessage.YOU_NOT_ENOUGH_ADENA));
				send(new ActionFailed());
				return;
			}
		}

		// 2. Consome os ingredientes
		for (var ing : entry.ingredients()) {
			long needed = ing.count() * amount;
			ctx.inventories().consumeItem(active.inventory(), ing.itemId(), (int) needed, "MultiSell");
		}

		// 3. Adiciona os produtos
		for (var prod : entry.products()) {
			long totalAdd = prod.count() * amount;
			ctx.inventories().addItem(active.inventory(), prod.itemId(), (int) totalAdd, "MultiSell");
			send(SystemMessage.of(SystemMessage.YOU_PICKED_UP_S1_S2,
					new SystemMessage.ItemName(prod.itemId()),
					new SystemMessage.Number((int) totalAdd)));
		}

		// 4. Atualiza o inventario do jogador
		send(ItemList.of(active.inventory().items(), false));
		var t = ctx.characters() != null ? ctx.characters().template(active) : null;
		if (t != null) {
			send(new UserInfo(active, t));
		}
	}

	private void onActionUse(RequestActionUse p) {
		if (!inWorld || active == null || active.isDead()) {
			send(new ActionFailed());
			return;
		}
		switch (p.actionId()) {
			case 0 -> { // Sit / Stand
				active.sitting(!active.sitting());
				var wait = new ChangeWaitType(active.objectId(), active.sitting() ? 0 : 1, x(), y(), z());
				send(wait);
				ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, wait, false);
			}
			case 1 -> { // Walk / Run
				active.running(!active.running());
				var move = new ChangeMoveType(active.objectId(), active.running());
				send(move);
				ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, move, false);
			}
			case 2 -> { // Acao de Ataque (icone de espada)
				if (targetObjectId != 0) {
					var npcOpt = ctx.world().npc(targetObjectId);
					if (npcOpt.isPresent()) {
						var npc = npcOpt.get();
						if (npc.template().isAttackable() && !npc.isDead()) {
							startAutoAttack(npc);
							return;
						}
					}
				}
				send(new ActionFailed());
			}
			case 3 -> { // Trade com alvo selecionado
				if (targetObjectId != 0) {
					var targetPlayerOpt = ctx.world().player(targetObjectId);
					if (targetPlayerOpt.isPresent() && targetPlayerOpt.get() instanceof GameSession targetSession && targetSession != this) {
						send(SystemMessage.of(SystemMessage.YOU_INVITED_S1_TO_PARTY, new SystemMessage.Text(targetSession.character().name())));
						return;
					}
				}
				send(new ActionFailed());
			}
			case 4 -> { // Target Next (proximo alvo / mob atacavel mais proximo)
				double bestDistSq = 900.0 * 900.0;
				NpcInstance bestNpc = null;
				for (var npc : ctx.world().findNpcsAround(active.x(), active.y(), 900)) {
					if (npc.template().isAttackable() && !npc.isDead()) {
						double dx = active.x() - npc.x();
						double dy = active.y() - npc.y();
						double d2 = dx * dx + dy * dy;
						if (d2 < bestDistSq) {
							bestDistSq = d2;
							bestNpc = npc;
						}
					}
				}
				if (bestNpc != null) {
					targetObjectId = bestNpc.objectId();
					send(new MyTargetSelected(bestNpc.objectId(), 0));
					send(new ValidateLocation(bestNpc.objectId(), bestNpc.x(), bestNpc.y(), bestNpc.z(), bestNpc.heading()));
				} else {
					send(new ActionFailed());
				}
			}
			case 5 -> { // Pickup
				send(new ActionFailed());
			}
			case 6 -> { // Assist
				if (targetObjectId != 0) {
					var targetPlayerOpt = ctx.world().player(targetObjectId);
					if (targetPlayerOpt.isPresent() && targetPlayerOpt.get() instanceof GameSession targetSession) {
						if (targetSession.targetObjectId != 0) {
							targetObjectId = targetSession.targetObjectId;
							send(new MyTargetSelected(targetObjectId, 0));
							return;
						}
					}
				}
				send(new ActionFailed());
			}
			case 15 -> { // Party Invite via icone
				if (targetObjectId != 0) {
					var targetPlayerOpt = ctx.world().player(targetObjectId);
					if (targetPlayerOpt.isPresent() && targetPlayerOpt.get() instanceof GameSession targetSession && targetSession != this) {
						onJoinParty(new GameClientPacket.RequestJoinParty(targetSession.character().name(), 0));
						return;
					}
				}
				send(new ActionFailed());
			}
			case 16 -> onLeaveParty(); // Party Leave
			case 17 -> { // Party Dismiss / Expel
				if (party != null && party.isLeader(active.objectId()) && targetObjectId != 0) {
					var targetPlayerOpt = ctx.world().player(targetObjectId);
					if (targetPlayerOpt.isPresent() && targetPlayerOpt.get() instanceof GameSession targetSession) {
						party.oust(targetSession.character().name());
						return;
					}
				}
				send(new ActionFailed());
			}
			case 18 -> { // Change Party Leader
				if (party != null && party.isLeader(active.objectId()) && targetObjectId != 0) {
					party.changeLeader(targetObjectId);
					return;
				}
				send(new ActionFailed());
			}
			default -> send(new ActionFailed());
		}
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

	private void onLeaveParty() {
		if (!inWorld || active == null) {
			send(new ActionFailed());
			return;
		}
		if (party != null) {
			party.removeMember(this);
			this.party = null;
		} else {
			send(new ActionFailed());
		}
	}

	private void onExpelPartyMember(String name) {
		if (!inWorld || active == null) {
			send(new ActionFailed());
			return;
		}
		if (party != null && party.isLeader(active.objectId())) {
			party.oust(name);
		} else {
			send(new ActionFailed());
		}
	}

	private void onShowMiniMap() {
		if (!inWorld || active == null) {
			send(new ActionFailed());
			return;
		}
		send(new ShowMiniMap(0, 0));
	}

	private void onRestartPoint(GameClientPacket.RequestRestartPoint p) {
		if (!inWorld || active == null || !active.isDead()) {
			send(new ActionFailed());
			return;
		}
		int[] townLoc = findNearestTown(active.x(), active.y());
		active.sitting(false);
		active.currentHp(active.maxHp() * 0.70);
		active.currentMp(active.maxMp() * 0.30);
		active.currentCp(0.0);
		active.moveTo(townLoc[0], townLoc[1], townLoc[2]);

		var revive = new Revive(active.objectId());
		send(revive);
		ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, revive, false);

		var tele = new TeleportToLocation(active.objectId(), townLoc[0], townLoc[1], townLoc[2]);
		send(tele);
		ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, tele, false);

		send(new UserInfo(active, ctx.characters().template(active)));
	}

	/** UseItem: equipaveis alternam equipar; consumiveis (pocoes, soulshots, scrolls) passam pelos handlers. */
	private void onUseItem(UseItem p) {
		if (!inWorld || active == null) {
			return;
		}
		var item = active.inventory().byObjectId(p.objectId()).orElse(null);
		if (item == null) {
			send(new ActionFailed());
			return;
		}
		if (!item.template().isEquipable()) {
			if (isScrollOfEscape(item.itemId())) {
				useScrollOfEscape(item);
				return;
			}
			if (item.itemId() == 1665 || item.itemId() == 1863) {
				onShowMiniMap();
				return;
			}
			if (EnchantScrollTable.isEnchantScroll(item.itemId())) {
				activeEnchantScrollObjectId = item.objectId();
				send(new ChooseInventoryItem(item.itemId()));
				send(new ActionFailed());
				return;
			}
			var consumable = ConsumableTable.get(item.itemId());
			if (consumable.isPresent()) {
				useConsumable(consumable.get());
			} else {
				send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED, new SystemMessage.ItemName(item.itemId())));
			}
			send(new ActionFailed());
			return;
		}
		if (active.sitting()) {
			send(new ActionFailed());
			return;
		}
		afterEquipChange(ctx.inventories().toggleEquip(active.inventory(), item.objectId()));
	}

	// ---- Consumiveis (porta de handler/item/Potions e SoulShots do L2JDream) ----

	private void useConsumable(Consumable c) {
		if (active.isDead()) {
			return;
		}
		if (c.type() == ConsumableTable.Type.SOULSHOT) {
			chargeSoulShot(c.itemId(), false);
			return;
		}
		if (c.type() == ConsumableTable.Type.SPIRITSHOT || c.type() == ConsumableTable.Type.BLESSED_SPIRITSHOT) {
			chargeSpiritShot(c.itemId(), false, c.type() == ConsumableTable.Type.BLESSED_SPIRITSHOT);
			return;
		}
		long now = System.currentTimeMillis();
		Long readyAt = consumableReuse.get(c.skillId());
		if (readyAt != null && readyAt > now) {
			send(SystemMessage.of(SystemMessage.S1_PREPARED_FOR_REUSE, new SystemMessage.ItemName(c.itemId())));
			return;
		}
		if (c.type() == ConsumableTable.Type.HAIR_STYLE && !active.female() && c.amount() > MALE_MAX_HAIR_STYLE) {
			// homens so tem estilos A-E; F e G sao exclusivos de personagens femininos
			send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED, new SystemMessage.ItemName(c.itemId())));
			return;
		}
		if (!consumeItem(c.itemId(), 1)) {
			return;
		}
		if (c.reuseMs() > 0) {
			consumableReuse.put(c.skillId(), now + c.reuseMs());
		}
		send(SystemMessage.of(SystemMessage.USE_S1, new SystemMessage.ItemName(c.itemId())));
		broadcastSelfSkill(c.skillId(), c.level());

		switch (c.type()) {
			case HEAL_HP -> {
				double before = active.currentHp();
				active.currentHp(before + c.amount());
				send(SystemMessage.of(SystemMessage.S1_HP_RESTORED,
						new SystemMessage.Number((int) (active.currentHp() - before))));
				sendVitals();
			}
			case HEAL_MP -> {
				double before = active.currentMp();
				active.currentMp(before + c.amount());
				send(SystemMessage.of(SystemMessage.S1_MP_RESTORED,
						new SystemMessage.Number((int) (active.currentMp() - before))));
				sendVitals();
			}
			case HEAL_CP -> {
				double before = active.currentCp();
				active.currentCp(before + c.amount());
				send(SystemMessage.of(SystemMessage.S1_CP_WILL_BE_RESTORED,
						new SystemMessage.Number((int) (active.currentCp() - before))));
				sendVitals();
			}
			case HOT_HP, HOT_MP -> startHealOverTime(c);
			case BUFF -> applyBuff(c);
			case FACE, HAIR_COLOR, HAIR_STYLE -> changeAppearance(c);
			case MYSTERY -> startBigHead(c);
			case REMEDY -> {
				// ainda nao existem efeitos de veneno/sangramento para remover: so animacao + consumo
			}
			default -> {
			}
		}
	}

	private static final int MALE_MAX_HAIR_STYLE = 4;

	/** CharChangePotions: muda rosto/cabelo, salva e reenvia a aparencia para todos. */
	private void changeAppearance(Consumable c) {
		int value = (int) c.amount();
		switch (c.type()) {
			case FACE -> active.face(value);
			case HAIR_COLOR -> active.hairColor(value);
			case HAIR_STYLE -> active.hairStyle(value);
			default -> {
				return;
			}
		}
		ctx.characters().save(active, true);
		broadcastAppearance();
	}

	/** MysteryPotion: AbnormalEffect BIG_HEAD pela duracao do skill 2103 (20 min). */
	private void startBigHead(Consumable c) {
		PlayerCharacter owner = active;
		var previous = hotTasks.remove("BigHead");
		if (previous != null) {
			previous.cancel(false);
		}
		owner.startAbnormalEffect(ConsumableTable.ABNORMAL_BIG_HEAD);
		send(SystemMessage.of(SystemMessage.YOU_FEEL_S1_EFFECT, new SystemMessage.SkillName(c.skillId(), c.level())));
		broadcastAppearance();
		AtomicReference<ScheduledFuture<?>> self = new AtomicReference<>();
		ScheduledFuture<?> task = autoAttackScheduler.schedule(() -> {
			if (!hotTasks.remove("BigHead", self.get())) {
				return;
			}
			owner.stopAbnormalEffect(ConsumableTable.ABNORMAL_BIG_HEAD);
			if (active == owner && inWorld) {
				send(SystemMessage.of(SystemMessage.S1_HAS_WORN_OFF, new SystemMessage.SkillName(c.skillId(), c.level())));
				broadcastAppearance();
			}
		}, (long) c.ticks() * c.intervalMs(), TimeUnit.MILLISECONDS);
		self.set(task);
		hotTasks.put("BigHead", task);
	}

	/** UserInfo para o proprio jogador e CharInfo para quem esta por perto. */
	private void broadcastAppearance() {
		send(new UserInfo(active, ctx.characters().template(active)));
		var info = charInfo();
		if (info != null) {
			ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, info, false);
		}
	}

	/** Tira {@code count} unidades do item e manda InventoryUpdate + peso. */
	private boolean consumeItem(int itemId, int count) {
		var r = ctx.inventories().consumeItem(active.inventory(), itemId, count, "Consume");
		if (r == null) {
			return false;
		}
		send(new InventoryUpdate(List.of(ItemInfo.of(r.item(), r.removed() ? ItemInfo.REMOVED : ItemInfo.MODIFIED))));
		send(new StatusUpdate(active.objectId(),
				List.of(new StatusUpdate.Attribute(StatusUpdate.CUR_LOAD, active.inventory().currentLoad()))));
		return true;
	}

	private void broadcastSelfSkill(int skillId, int level) {
		var msu = new MagicSkillUse(active.objectId(), active.objectId(), skillId, level, 0, 0,
				active.x(), active.y(), active.z(), active.x(), active.y(), active.z());
		send(msu);
		ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, msu, false);
	}

	private void sendVitals() {
		send(new StatusUpdate(active.objectId(), List.of(
				new StatusUpdate.Attribute(StatusUpdate.CUR_HP, (int) active.currentHp()),
				new StatusUpdate.Attribute(StatusUpdate.CUR_MP, (int) active.currentMp()),
				new StatusUpdate.Attribute(StatusUpdate.CUR_CP, (int) active.currentCp()))));
	}

	/** HealOverTime/ManaHealOverTime: um efeito por stackType, o novo substitui o anterior. */
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
		owner.effects().put(buff);
		send(SystemMessage.of(SystemMessage.YOU_FEEL_S1_EFFECT, new SystemMessage.SkillName(c.skillId(), c.level())));
		refreshBuffs();
		saveBuffs();
		autoAttackScheduler.schedule(() -> {
			if (owner.effects().remove(buff) && active == owner && inWorld) {
				send(SystemMessage.of(SystemMessage.S1_HAS_WORN_OFF, new SystemMessage.SkillName(c.skillId(), c.level())));
				refreshBuffs();
				saveBuffs();
			}
		}, durationMs, TimeUnit.MILLISECONDS);
	}

	/** Icones de buff + UserInfo (velocidade/atk spd/max HP mudam). */
	private void refreshBuffs() {
		if (active == null) {
			return;
		}
		long now = System.currentTimeMillis();
		// um icone por skill (um skill pode ter varios efeitos com stackTypes diferentes)
		Map<Integer, MagicEffectIcons.Icon> icons = new java.util.LinkedHashMap<>();
		for (var b : active.effects().active()) {
			icons.putIfAbsent(b.skillId(), new MagicEffectIcons.Icon(b.skillId(), b.level(), b.remainingSeconds(now)));
		}
		send(new MagicEffectIcons(List.copyOf(icons.values())));
		var t = ctx.characters().template(active);
		if (ctx.skillService() != null) {
			recalcMaxVitals(t);
		}
		send(new UserInfo(active, t));
	}

	private void saveBuffs() {
		if (active == null || ctx.buffRepository() == null) {
			return;
		}
		ctx.buffRepository().saveBuffs(active.objectId(), 0, active.effects().active());
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

	private void onAutoSoulShot(RequestAutoSoulShot p) {
		if (!inWorld || active.isDead()) {
			return;
		}
		if (!ConsumableTable.isShot(p.itemId())) {
			send(new ExAutoSoulShot(p.itemId(), 0));
			return;
		}
		if (active.inventory().byItemId(p.itemId()).isEmpty()) {
			return;
		}
		var name = new SystemMessage.ItemName(p.itemId());
		if (p.type() == 1) {
			autoSoulShots.add(p.itemId());
			send(new ExAutoSoulShot(p.itemId(), 1));
			send(SystemMessage.of(SystemMessage.USE_OF_S1_WILL_BE_AUTO, name));
			if (ConsumableTable.isSoulshot(p.itemId())) {
				chargeSoulShot(p.itemId(), false);
			} else {
				chargeSpiritShot(p.itemId(), false, ConsumableTable.isBlessedSpiritshot(p.itemId()));
			}
		} else {
			autoSoulShots.remove(p.itemId());
			send(new ExAutoSoulShot(p.itemId(), 0));
			send(SystemMessage.of(SystemMessage.AUTO_USE_OF_S1_CANCELLED, name));
		}
	}

	private void rechargeAutoSoulShots() {
		for (int itemId : autoSoulShots) {
			if (ConsumableTable.isSoulshot(itemId)) {
				if (!soulshotCharged && chargeSoulShot(itemId, true)) {
					// soulshot carregado
				}
			} else if (ConsumableTable.isSpiritshot(itemId)) {
				if (!spiritshotCharged && chargeSpiritShot(itemId, true, ConsumableTable.isBlessedSpiritshot(itemId))) {
					// spiritshot carregado
				}
			}
		}
	}

	private com.lopez.l2j.game.item.ItemInstance activeWeapon() {
		var inv = active.inventory();
		var w = inv.paperdoll(com.lopez.l2j.game.item.ItemSlots.RHAND);
		if (w == null) {
			w = inv.paperdoll(com.lopez.l2j.game.item.ItemSlots.LRHAND);
		}
		return w != null && w.template().kind() == com.lopez.l2j.game.item.ItemTemplate.Kind.WEAPON ? w : null;
	}

	/**
	 * Carrega a arma com um soulshot (SoulShots.useItem do legado). {@code quiet} suprime avisos de arma/grade
	 * quando a chamada vem do uso automatico antes de cada golpe.
	 */
	private boolean chargeSoulShot(int itemId, boolean quiet) {
		if (soulshotCharged) {
			return true;
		}
		var c = ConsumableTable.get(itemId).orElse(null);
		if (c == null || c.type() != ConsumableTable.Type.SOULSHOT) {
			return false;
		}
		var weapon = activeWeapon();
		if (weapon == null) {
			if (!quiet) {
				send(SystemMessage.id(SystemMessage.CANNOT_USE_SOULSHOTS));
			}
			return false;
		}
		int weaponGrade = ConsumableTable.gradeIndex(weapon.template().crystalType());
		if (weaponGrade != ConsumableTable.gradeIndex(c.grade())) {
			if (!quiet) {
				send(SystemMessage.id(SystemMessage.SOULSHOTS_GRADE_MISMATCH));
			}
			return false;
		}
		if (!consumeItem(itemId, 1)) {
			if (autoSoulShots.remove(itemId)) {
				send(new ExAutoSoulShot(itemId, 0));
				send(SystemMessage.of(SystemMessage.AUTO_USE_OF_S1_CANCELLED, new SystemMessage.ItemName(itemId)));
			} else {
				send(SystemMessage.id(SystemMessage.NOT_ENOUGH_SOULSHOTS));
			}
			return false;
		}
		chargedGrade = weaponGrade;
		soulshotCharged = true;
		send(SystemMessage.id(SystemMessage.ENABLED_SOULSHOT));
		broadcastSelfSkill(c.skillId(), 1);
		return true;
	}

	private boolean chargeSpiritShot(int itemId, boolean quiet, boolean blessed) {
		if (spiritshotCharged) {
			return true;
		}
		var c = ConsumableTable.get(itemId).orElse(null);
		if (c == null) {
			return false;
		}
		var weapon = activeWeapon();
		if (weapon == null) {
			if (!quiet) {
				send(SystemMessage.id(SystemMessage.CANNOT_USE_SOULSHOTS));
			}
			return false;
		}
		int weaponGrade = ConsumableTable.gradeIndex(weapon.template().crystalType());
		if (weaponGrade != ConsumableTable.gradeIndex(c.grade())) {
			if (!quiet) {
				send(SystemMessage.id(SystemMessage.SOULSHOTS_GRADE_MISMATCH));
			}
			return false;
		}
		if (!consumeItem(itemId, 1)) {
			if (autoSoulShots.remove(itemId)) {
				send(new ExAutoSoulShot(itemId, 0));
				send(SystemMessage.of(SystemMessage.AUTO_USE_OF_S1_CANCELLED, new SystemMessage.ItemName(itemId)));
			} else {
				send(SystemMessage.id(SystemMessage.NOT_ENOUGH_SOULSHOTS));
			}
			return false;
		}
		chargedSpSGrade = weaponGrade;
		blessedSpiritshot = blessed;
		spiritshotCharged = true;
		send(SystemMessage.id(SystemMessage.ENABLED_SOULSHOT));
		broadcastSelfSkill(c.skillId(), 1);
		return true;
	}

	private void onUnEquip(RequestUnEquipItem p) {
		if (!inWorld) {
			return;
		}
		afterEquipChange(ctx.inventories().unequipBodyPart(active.inventory(), p.bodyPart()));
	}

	private void afterEquipChange(EquipResult r) {
		if (!r.ok()) {
			send(new ActionFailed());
			return;
		}
		var item = r.item();
		var name = new SystemMessage.ItemName(item.itemId());
		if (r.equipped()) {
			send(item.enchant() > 0
					? SystemMessage.of(SystemMessage.S1_S2_EQUIPPED, new SystemMessage.Number(item.enchant()), name)
					: SystemMessage.of(SystemMessage.S1_EQUIPPED, name));
		} else {
			send(item.enchant() > 0
					? SystemMessage.of(SystemMessage.EQUIPMENT_S1_S2_REMOVED, new SystemMessage.Number(item.enchant()),
							name)
					: SystemMessage.of(SystemMessage.S1_DISARMED, name));
		}
		send(InventoryUpdate.modified(r.changed()));
		send(new UserInfo(active, ctx.characters().template(active)));
		// Trocar de arma descarrega o soulshot; o automatico tenta recarregar com o grade novo
		if (r.changed().stream().anyMatch(i -> i.template().kind() == com.lopez.l2j.game.item.ItemTemplate.Kind.WEAPON)) {
			soulshotCharged = false;
			rechargeAutoSoulShots();
		}
	}

	private void onLogout() {
		leaveWorld();
		send(new LeaveWorld());
		closeRequested = true;
	}

	private void onRestart() {
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
			ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, new DeleteObject(active.objectId()), false);
			ctx.world().remove(this);
			ctx.characters().save(active, false);
			knownObjects.clear();
			log.info("{} saiu do mundo", active.name());
		}
		inWorld = false;
		hotTasks.values().forEach(f -> f.cancel(false));
		hotTasks.clear();
		autoSoulShots.clear();
		soulshotCharged = false;
		spiritshotCharged = false;
		stopVitalsRegenTask();
		consumableReuse.clear();
		skillReuse.clear();
		var cast = castTask;
		if (cast != null) {
			cast.cancel(false);
		}
		casting = false;
		if (active.isDead()) {
			if (ctx.buffRepository() != null) {
				ctx.buffRepository().deleteBuffs(active.objectId());
			}
		} else {
			saveBuffs();
		}
		active.effects().clear();
		active.stopAbnormalEffect(ConsumableTable.ABNORMAL_BIG_HEAD);
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

	// ---- Skills (porta enxuta de L2Character.doCast/onMagicHitTimer + handlers de skill do L2JDream) ----

	/** Conjuracao em andamento (um skill por vez, como no cliente). */
	private volatile boolean casting;
	private volatile ScheduledFuture<?> castTask;
	/** Reuse por skillId (epoch ms em que libera). */
	private final Map<Integer, Long> skillReuse = new ConcurrentHashMap<>();
	/** Treinador da ultima janela de skills aberta (RequestAcquireSkill confere a distancia). */
	private int lastTrainerObjectId;

	private void onMagicSkillUse(RequestMagicSkillUse p) {
		if (!inWorld || active.isDead() || active.isDisabled()) {
			send(new ActionFailed());
			return;
		}
		var svc = ctx.skillService();
		var sk = svc == null ? null : svc.known(active, p.magicId()).orElse(null);
		if (sk == null || sk.isPassive()) {
			send(new ActionFailed());
			return;
		}
		if (sk.isToggle()) {
			toggleSkill(sk);
			send(new ActionFailed());
			return;
		}
		castSkill(sk, true);
	}

	/** Valida e inicia a conjuracao; {@code mayMove} = pode andar ate o alvo antes (uma vez). */
	private void castSkill(SkillTemplate sk, boolean mayMove) {
		if (!inWorld || active == null || active.isDead() || active.isDisabled()) {
			return;
		}
		if (casting) {
			send(new ActionFailed());
			return;
		}
		long now = System.currentTimeMillis();
		Long readyAt = skillReuse.get(sk.id());
		if (readyAt != null && readyAt > now) {
			send(SystemMessage.of(SystemMessage.S1_PREPARED_FOR_REUSE, new SystemMessage.SkillName(sk.id(), sk.level())));
			send(new ActionFailed());
			return;
		}
		if (sk.castCondition() != null && !sk.castCondition().test(active)) {
			if (sk.condMsg() != null && !sk.condMsg().isBlank()) {
				try {
					int msgId = Integer.parseInt(sk.condMsg().trim());
					send(SystemMessage.id(msgId));
				} catch (NumberFormatException e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", sk.condMsg()));
				}
			} else {
				send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED, new SystemMessage.SkillName(sk.id(), sk.level())));
			}
			send(new ActionFailed());
			return;
		}
		if (isBow(activeWeapon()) && !sk.magic()) {
			if (!checkAndConsumeArrow()) {
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

		// Alvo principal
		NpcInstance npcTarget = null;
		GameSession playerTarget = null;
		boolean isResurrect = "RESURRECT".equalsIgnoreCase(sk.skillType()) || sk.target().startsWith("TARGET_CORPSE_");

		if (sk.isOffensive()) {
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

		// Alcance (o L2J anda ate o alvo e depois conjura)
		int tx = npcTarget != null ? npcTarget.x() : (playerTarget != null ? playerTarget.x() : active.x());
		int ty = npcTarget != null ? npcTarget.y() : (playerTarget != null ? playerTarget.y() : active.y());
		int tz = npcTarget != null ? npcTarget.z() : (playerTarget != null ? playerTarget.z() : active.z());
		if (sk.castRange() > 0 && (npcTarget != null || (playerTarget != null && playerTarget != this))) {
			double dist = Math.hypot(active.x() - tx, active.y() - ty);
			double maxDist = sk.castRange() + 40 + (npcTarget != null ? npcTarget.template().collisionRadius() : 0);
			if (dist > maxDist) {
				if (!mayMove) {
					send(SystemMessage.id(SystemMessage.TARGET_TOO_FAR));
					send(new ActionFailed());
					return;
				}
				int targetId = npcTarget != null ? npcTarget.objectId() : playerTarget.objectId();
				var move = new MoveToPawn(active.objectId(), targetId, sk.castRange(), active.x(), active.y(), active.z());
				send(move);
				ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, move, false);
				int run = Math.max(50, PlayerStats.calculate(active, ctx.characters().template(active)).runSpeed());
				long travelMs = (long) ((dist - sk.castRange()) * 1000 / run) + 300;
				PlayerCharacter owner = active;
				autoAttackScheduler.schedule(() -> {
					if (active == owner) {
						castSkill(sk, false);
					}
				}, travelMs, TimeUnit.MILLISECONDS);
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
		int hitTime = sk.hitTime() > 0 ? Math.max(150, (int) (sk.hitTime() * speedFactor)) : 0;
		int reuse = (int) (sk.reuseDelay() * speedFactor);
		active.currentMp(active.currentMp() - sk.mpInitialConsume());
		if (reuse > 0) {
			skillReuse.put(sk.id(), now + Math.max(reuse, hitTime));
		}
		boolean resumeAttack = autoAttacking && npcTarget != null && sk.isOffensive();
		autoAttacking = false; // o auto-ataque para durante o cast

		int mainTargetId = npcTarget != null ? npcTarget.objectId() : (playerTarget != null ? playerTarget.objectId() : active.objectId());
		var msu = new MagicSkillUse(active.objectId(), mainTargetId, sk.id(), sk.level(), hitTime, reuse,
				active.x(), active.y(), active.z(), tx, ty, tz);
		send(msu);
		ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, msu, false);
		send(SystemMessage.of(SystemMessage.USE_S1, new SystemMessage.SkillName(sk.id(), sk.level())));
		if (hitTime > 0) {
			send(new GameServerPacket.SetupGauge(GameServerPacket.SetupGauge.BLUE, hitTime));
		}
		if (sk.mpInitialConsume() > 0) {
			sendVitals();
		}

		casting = true;
		PlayerCharacter owner = active;
		NpcInstance npc = npcTarget;
		GameSession targetSess = playerTarget;
		Runnable finish = () -> {
			casting = false;
			castTask = null;
			if (active != owner || !inWorld || owner.isDead()) {
				return;
			}
			try {
				finishCast(sk, npc, targetSess, resumeAttack, sps, bss);
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
	private void finishCast(SkillTemplate sk, NpcInstance mainNpc, GameSession targetPlayer, boolean resumeAttack,
			boolean sps, boolean bss) {
		if (active.currentMp() < sk.mpConsume()) {
			send(SystemMessage.id(SystemMessage.NOT_ENOUGH_MP));
			return;
		}
		active.currentMp(active.currentMp() - sk.mpConsume());
		if (sk.hpConsume() > 0) {
			active.currentHp(active.currentHp() - sk.hpConsume());
		}
		var t = ctx.characters().template(active);

		if (sk.isOffensive()) {
			boolean ss = false;
			if (sk.isPhysicalDamage()) {
				if (!soulshotCharged) {
					rechargeAutoSoulShots();
				}
				ss = soulshotCharged;
				soulshotCharged = false;
			}

			if (targetPlayer != null && !targetPlayer.active.isDead()) {
				// PvP ofensivo direcionado a outro jogador
				broadcastLaunched(sk, List.of(targetPlayer.objectId()));
				applyOffensivePlayer(sk, targetPlayer, t, ss, sps, bss);
			} else {
				// Alvo NPC ou em area
				List<NpcInstance> targets = new ArrayList<>();
				if (sk.isAreaAroundSelf()) {
					targets.addAll(ctx.world().findNpcsAround(active.x(), active.y(), sk.skillRadius()));
				} else if (mainNpc != null) {
					targets.add(mainNpc);
					if (sk.isAreaAroundTarget()) {
						for (NpcInstance n : ctx.world().findNpcsAround(mainNpc.x(), mainNpc.y(), sk.skillRadius())) {
							if (n != mainNpc) {
								targets.add(n);
							}
						}
					}
				}
				targets.removeIf(n -> !n.isAttackable() || n.isDead());
				broadcastLaunched(sk, targets.stream().map(NpcInstance::objectId).toList());

				for (NpcInstance n : targets) {
					applyOffensive(sk, n, t, ss, sps, bss);
				}
			}
			sendVitals();
			if (resumeAttack && mainNpc != null && !mainNpc.isDead() && targetObjectId == mainNpc.objectId()) {
				autoAttacking = true;
				schedulePlayerAutoAttack(mainNpc);
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
			boolean resurrect = "RESURRECT".equalsIgnoreCase(sk.skillType()) || sk.target().startsWith("TARGET_CORPSE_");
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
		for (GameSession target : targets) {
			target.receivePositiveSkill(sk, active.name());
		}
		sendVitals();
	}

	private void broadcastLaunched(SkillTemplate sk, List<Integer> targets) {
		var launched = new GameServerPacket.MagicSkillLaunched(active.objectId(), sk.id(), sk.level(), targets);
		send(launched);
		ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, launched, false);
	}

	/** Dano + debuffs de um skill ofensivo num monstro. */
	private void applyOffensive(SkillTemplate sk, NpcInstance npc, CharTemplate t, boolean soulshot,
			boolean sps, boolean bss) {
		var combat = ctx.combat();
		if (combat == null) {
			return;
		}
		CombatService.HitResult hit = null;
		if (sk.isPhysicalDamage()) {
			hit = combat.skillPhysicalNpc(active, t, npc, sk.power(), soulshot, sk.skillType().equals("BLOW"));
		} else if (sk.isMagicDamage() && sk.power() > 0) {
			hit = combat.skillMagicNpc(active, t, npc, sk.power(), sps, bss);
			if (sk.skillType().equals("DRAIN") && hit.damage() > 0) {
				double absorb = sk.absorbPart() > 0 ? sk.absorbPart() : 0.2;
				active.currentHp(active.currentHp() + hit.damage() * absorb);
			}
		}

		// Debuffs de controle (Stun/Sleep/Paralyze/Root), DoTs e debuffs de stats nos monstros
		if (!npc.isDead()) {
			boolean damageSkill = hit != null;
			for (var e : sk.effects()) {
				String name = e.name().toLowerCase(java.util.Locale.ROOT);
				boolean control = name.equals("stun") || name.equals("sleep") || name.equals("paralyze")
						|| name.equals("root") || name.equals("petrification");
				if (control) {
					double base = damageSkill ? 50 : sk.power();
					if (!combat.debuffLands(base, sk.magicLevel(), active.level(), npc, sps, bss)) {
						send(SystemMessage.of(SystemMessage.S1_WAS_UNAFFECTED_BY_S2, new SystemMessage.NpcName(npc.npcId()),
								new SystemMessage.SkillName(sk.id(), sk.level())));
						continue;
					}
					long until = System.currentTimeMillis() + Math.max(1000, e.durationMs());
					if (name.equals("root")) {
						npc.root(until);
					} else {
						npc.disable(until, name.equals("sleep"));
					}
				} else if (name.equals("damovertime")) {
					if (combat.debuffLands(sk.power() > 0 ? sk.power() : 50, sk.magicLevel(), active.level(), npc, sps, bss)) {
						startNpcDot(npc, sk, e, t);
					}
				} else if (!e.funcs().isEmpty()) {
					if (combat.debuffLands(sk.power() > 0 ? sk.power() : 50, sk.magicLevel(), active.level(), npc, sps, bss)) {
						applyNpcDebuff(npc, e);
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

	private void applyNpcDebuff(NpcInstance npc, SkillTemplate.EffectTemplate e) {
		for (var f : e.funcs()) {
			if (f.stat().equals("pDef")) {
				npc.pDefMul(0.77);
				autoAttackScheduler.schedule(() -> npc.pDefMul(1.0), Math.max(1000, e.durationMs()), TimeUnit.MILLISECONDS);
			} else if (f.stat().equals("mDef")) {
				npc.mDefMul(0.77);
				autoAttackScheduler.schedule(() -> npc.mDefMul(1.0), Math.max(1000, e.durationMs()), TimeUnit.MILLISECONDS);
			} else if (f.stat().equals("pAtk")) {
				npc.pAtkMul(0.77);
				autoAttackScheduler.schedule(() -> npc.pAtkMul(1.0), Math.max(1000, e.durationMs()), TimeUnit.MILLISECONDS);
			}
		}
	}

	private void startNpcDot(NpcInstance npc, SkillTemplate sk, SkillTemplate.EffectTemplate e, CharTemplate t) {
		int count = Math.max(1, e.count());
		long period = Math.max(1, e.period()) * 1000L;
		int damagePerTick = (int) Math.max(1, Math.round(e.val() > 0 ? e.val() : 20));
		int[] rem = { count };
		PlayerCharacter owner = active;
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
			var hit = ctx.combat().applyDamage(npc, damagePerTick, 0);
			handleNpcHit(npc, hit, t, sk);
			if (hit.isDead()) {
				var f = taskRef.get();
				if (f != null) {
					f.cancel(false);
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

		if (sk.isPhysicalDamage()) {
			damage = combat.skillPhysicalPlayer(active, t, targetActive, targetTemplate, sk.power(), soulshot,
					sk.skillType().equals("BLOW"));
		} else if (sk.isMagicDamage() && sk.power() > 0) {
			damage = combat.skillMagicPlayer(active, t, targetActive, targetTemplate, sk.power(), sps, bss);
			if (sk.skillType().equals("DRAIN") && damage > 0) {
				double absorb = sk.absorbPart() > 0 ? sk.absorbPart() : 0.2;
				healHp(damage * absorb);
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
			for (var e : sk.effects()) {
				String name = e.name().toLowerCase(java.util.Locale.ROOT);
				boolean control = name.equals("stun") || name.equals("sleep") || name.equals("paralyze")
						|| name.equals("root") || name.equals("petrification");

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
				} else if (name.equals("damovertime") || name.equals("manadamovertime")) {
					if (combat.debuffLandsPlayer(sk.power() > 0 ? sk.power() : 50, sk.magicLevel(), active.level(),
							targetActive, sps, bss)) {
						targetSession.startSkillDot(sk, e);
					}
				} else if (!e.funcs().isEmpty()) {
					if (combat.debuffLandsPlayer(sk.power() > 0 ? sk.power() : 50, sk.magicLevel(), active.level(),
							targetActive, sps, bss)) {
						targetSession.applySkillEffects(sk, false);
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
			case "paralyze", "petrification" -> {
				active.disable(until, false);
				stopAutoAttack();
				cancelCast();
				abnormalMask = 0x0010;
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

	public void handlePlayerDeath(PlayerCharacter killer) {
		if (active == null) {
			return;
		}
		stopAutoAttack();
		cancelCast();
		active.currentHp(0);
		active.currentCp(0);

		send(new StatusUpdate(active.objectId(), List.of(
				new StatusUpdate.Attribute(StatusUpdate.CUR_HP, 0),
				new StatusUpdate.Attribute(StatusUpdate.CUR_CP, 0)
		)));

		var die = new Die(active.objectId(), false);
		send(die);
		ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, die, false);
		ctx.characters().save(active, true);
	}

	public void cancelCast() {
		if (casting) {
			casting = false;
			var task = castTask;
			if (task != null) {
				task.cancel(false);
				castTask = null;
			}
			send(SystemMessage.id(SystemMessage.CASTING_INTERRUPTED));
			send(new ActionFailed());
		}
	}

	/** Mensagem de dano, HP do alvo, morte (EXP/drop) ou aggro. */
	private void handleNpcHit(NpcInstance npc, CombatService.HitResult hit, CharTemplate t, SkillTemplate sk) {
		if (hit.damage() > 0) {
			if ((hit.flags() & 0x20) != 0) {
				send(SystemMessage.id(sk.magic() ? SystemMessage.CRITICAL_HIT_MAGIC : SystemMessage.CRITICAL_HIT));
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
			if (targetObjectId == npc.objectId() && autoAttacking) {
				autoAttacking = false;
				send(new AutoAttackStop(active.objectId()));
				ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, new AutoAttackStop(active.objectId()), false);
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
			if (ctx.drops() != null) {
				ctx.drops().rewardMonsterDeath(active, npc.npcId(), ctx.inventories(), this::send);
			}
			ctx.characters().save(active, true);
		} else if (!npc.isDead() && ctx.npcAi() != null) {
			ctx.npcAi().startCombat(npc, active.objectId());
		}
	}

	/** Cura/buff recebido (chamado na sessao do alvo; pode ser o proprio conjurador). */
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
		double power = sk.power();
		switch (sk.skillType()) {
			case "HEAL", "HEAL_STATIC" -> healHp(power);
			case "HEAL_PERCENT" -> healHp(active.maxHp() * power / 100.0);
			case "MANAHEAL", "MANARECHARGE" -> {
				double before = active.currentMp();
				active.currentMp(before + power);
				send(SystemMessage.of(SystemMessage.S1_MP_RESTORED, new SystemMessage.Number((int) (active.currentMp() - before))));
			}
			case "MANAHEAL_PERCENT" -> {
				double before = active.currentMp();
				active.currentMp(before + active.maxMp() * power / 100.0);
				send(SystemMessage.of(SystemMessage.S1_MP_RESTORED, new SystemMessage.Number((int) (active.currentMp() - before))));
			}
			case "COMBATPOINTHEAL" -> {
				double before = active.currentCp();
				active.currentCp(before + power);
				send(SystemMessage.of(SystemMessage.S1_CP_WILL_BE_RESTORED, new SystemMessage.Number((int) (active.currentCp() - before))));
			}
			default -> {
			}
		}
		applySkillEffects(sk, false);
		sendVitals();
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
		double before = active.currentHp();
		active.currentHp(before + amount);
		send(SystemMessage.of(SystemMessage.S1_HP_RESTORED, new SystemMessage.Number((int) (active.currentHp() - before))));
	}

	/** Aplica os {@code <effect>} do skill no proprio jogador desta sessao (buffs, HoT, DoT, toggles). */
	private void applySkillEffects(SkillTemplate sk, boolean toggle) {
		applySkillEffects(sk, toggle, 0);
	}

	private void applySkillEffects(SkillTemplate sk, boolean toggle, long remainingMs) {
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
			if (e.funcs().isEmpty()) {
				continue; // efeito sem stats (Stun, Fear, etc.) ainda nao tem motor no jogador
			}
			String stack = e.stackType() == null || e.stackType().equalsIgnoreCase("none")
					? "skill_" + sk.id() + "_" + name : e.stackType();
			long duration = remainingMs > 0 ? remainingMs : e.durationMs();
			long end = toggle ? com.lopez.l2j.game.effect.PlayerEffects.PERMANENT
					: System.currentTimeMillis() + duration;
			var buff = ActiveBuff.ofSkill(sk.id(), sk.level(), stack, end, e.funcs());
			owner.effects().put(buff);
			any = true;
			if (!toggle) {
				autoAttackScheduler.schedule(() -> {
					if (owner.effects().remove(buff) && active == owner && inWorld) {
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
		String stack = "skilldot_" + (e.stackType() != null && !e.stackType().equalsIgnoreCase("none") ? e.stackType() : sk.id() + "_" + kind);
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
			if (kind.equals("manadamovertime")) {
				owner.currentMp(Math.max(0, owner.currentMp() - e.val()));
				sendVitals();
			} else {
				double curHp = owner.currentHp();
				double newHp = Math.max(1, curHp - e.val());
				owner.currentHp(newHp);
				sendVitals();
			}
		}, period, period, TimeUnit.MILLISECONDS);
		self.set(task);
		hotTasks.put(stack, task);
	}

	/** HealOverTime/ManaHealOverTime do skill: {@code val} por tick a cada {@code period} s, {@code count} vezes. */
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

	/** Toggle (OP_TOGGLE): liga aplicando os efeitos permanentes, desliga removendo. */
	private void toggleSkill(SkillTemplate sk) {
		if (active.effects().hasSkill(sk.id())) {
			active.effects().removeSkill(sk.id());
			send(SystemMessage.of(SystemMessage.EFFECT_S1_DISAPPEARED, new SystemMessage.SkillName(sk.id(), sk.level())));
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
		sendVitals();
	}

	/** Max HP/MP/CP = formula da classe no nivel + passivas/buffs (maxHp, maxMp, maxCp). */
	private void recalcMaxVitals(CharTemplate t) {
		int lvl = active.level();
		active.maxHp(Math.max(1, (int) PlayerStats.applyStat(active, "maxHp", t.calculateMaxHp(lvl))));
		active.maxMp(Math.max(1, (int) PlayerStats.applyStat(active, "maxMp", t.calculateMaxMp(lvl))));
		active.maxCp(Math.max(1, (int) PlayerStats.applyStat(active, "maxCp", t.calculateMaxCp(lvl))));
		active.currentHp(active.currentHp());
		active.currentMp(active.currentMp());
		active.currentCp(active.currentCp());
	}

	private void sendSkillList() {
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

	// ---- Treinador (L2NpcInstance.showSkillList + RequestAquireSkillInfo/RequestAquireSkill) ----

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
				send(SystemMessage.of(SystemMessage.DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN, new SystemMessage.Number(min)));
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
		if (trainer == null || (!active.isGm() && Math.hypot(active.x() - trainer.x(), active.y() - trainer.y()) > 250)) {
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

	private void checkPendingNpcInteract() {
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
		if (distSq <= 180.0 * 180.0) {
			pendingNpcInteractObjectId = 0;
			showNpcHtml(npc, 0);
		}
	}

	private static final int[][] MAJOR_TOWNS = {
			{ -84318, 244579, -3730 }, // Talking Island
			{ 46934, 51467, -2977 },   // Elven Village
			{ 9745, 15606, -4574 },    // Dark Elven Village
			{ -44836, -112524, -235 }, // Orc Village
			{ 115113, -178212, -901 }, // Dwarven Village
			{ -80826, 149775, -3043 }, // Gludin
			{ -12678, 122776, -3116 }, // Gludio
			{ 15670, 142983, -2705 },  // Dion
			{ 83400, 147943, -3404 },  // Giran
			{ 111409, 219364, -3545 }, // Heine
			{ 82956, 53162, -1495 },   // Oren
			{ 116819, 76994, -2714 },  // Hunters Village
			{ 146331, 25762, -2018 },  // Aden
			{ 147928, -55273, -2734 }, // Goddard
			{ 43799, -47727, -798 },   // Rune
			{ 87331, -142842, -1317 }  // Schuttgart
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
			Map.entry(7135, new int[] { 87331, -142842, -1317 })
	);

	private boolean isScrollOfEscape(int itemId) {
		return itemId == 736 || itemId == 1538 || itemId == 3958 || itemId == 5858 || itemId == 5859
				|| TOWN_SCROLL_COORDINATES.containsKey(itemId);
	}

	private void useScrollOfEscape(com.lopez.l2j.game.item.ItemInstance item) {
		if (active == null || active.isDead() || casting) {
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
			active.moveTo(targetLoc[0], targetLoc[1], targetLoc[2]);
			ctx.characters().save(active, true);
			var tele = new TeleportToLocation(active.objectId(), targetLoc[0], targetLoc[1], targetLoc[2]);
			send(tele);
			ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, tele, false);
			updateKnownObjects();
		}, hitTime, TimeUnit.MILLISECONDS);
	}

	private int[] findNearestTown(int px, int py) {
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

	private void onJoinParty(GameClientPacket.RequestJoinParty p) {
		if (!inWorld || active == null || active.isDead()) {
			send(new ActionFailed());
			return;
		}
		var target = ctx.world().byName(p.name());
		if (target.isEmpty() || target.get().character() == null) {
			send(SystemMessage.id(SystemMessage.TARGET_CANT_FOUND));
			send(new ActionFailed());
			return;
		}
		if (!(target.get() instanceof GameSession targetSession)) {
			send(new ActionFailed());
			return;
		}
		if (targetSession == this || targetSession.character().objectId() == active.objectId()) {
			send(SystemMessage.id(SystemMessage.CANT_INVITE_YOURSELF));
			send(new ActionFailed());
			return;
		}
		if (targetSession.party() != null) {
			send(SystemMessage.of(SystemMessage.PLAYER_ALREADY_IN_PARTY, new SystemMessage.Text(targetSession.character().name())));
			send(new ActionFailed());
			return;
		}
		if (party != null) {
			if (!party.isLeader(active.objectId())) {
				send(SystemMessage.id(SystemMessage.ONLY_LEADER_CAN_INVITE));
				send(new ActionFailed());
				return;
			}
			if (party.isFull()) {
				send(SystemMessage.id(SystemMessage.PARTY_FULL));
				send(new ActionFailed());
				return;
			}
		}
		if (targetSession.pendingPartyInvite() != null) {
			send(SystemMessage.id(SystemMessage.WAITING_FOR_REPLY));
			send(new ActionFailed());
			return;
		}
		targetSession.setPendingPartyInvite(new RequestPartyPending(this, p.itemDistribution()));
		targetSession.send(new GameServerPacket.AskJoinParty(active.name(), p.itemDistribution()));
		send(SystemMessage.of(SystemMessage.YOU_INVITED_S1_TO_PARTY, new SystemMessage.Text(targetSession.character().name())));
	}

	private void onAnswerJoinParty(GameClientPacket.RequestAnswerJoinParty p) {
		if (!inWorld || active == null || pendingPartyInvite == null) {
			return;
		}
		var pending = pendingPartyInvite;
		pendingPartyInvite = null;
		var requester = pending.requester();
		if (requester == null || requester.character() == null || !requester.inWorld()) {
			return;
		}
		if (p.response() == 0) {
			requester.send(SystemMessage.of(SystemMessage.S1_REFUSED_PARTY, new SystemMessage.Text(active.name())));
			return;
		}
		// Aceitou o convite (response == 1)
		if (requester.party() == null) {
			var newParty = new com.lopez.l2j.game.party.Party(requester, this, pending.itemDistribution());
			requester.party(newParty);
			this.party = newParty;
			send(new GameServerPacket.JoinParty(1));
			requester.send(new GameServerPacket.PartySmallWindowAll(requester.character().objectId(), pending.itemDistribution(), newParty.characters(), requester.character().objectId()));
			send(new GameServerPacket.PartySmallWindowAll(requester.character().objectId(), pending.itemDistribution(), newParty.characters(), active.objectId()));
			requester.send(SystemMessage.of(SystemMessage.S1_JOINED_PARTY, new SystemMessage.Text(active.name())));
			send(SystemMessage.of(SystemMessage.YOU_JOINED_PARTY, new SystemMessage.Text(requester.character().name())));
		} else {
			var existingParty = requester.party();
			if (existingParty.isFull()) {
				send(SystemMessage.id(SystemMessage.PARTY_FULL));
				return;
			}
			if (existingParty.addMember(this)) {
				this.party = existingParty;
				send(new GameServerPacket.JoinParty(1));
			}
		}
	}

	private void startVitalsRegenTask() {
		stopVitalsRegenTask();
		PlayerCharacter owner = active;
		regenTask = autoAttackScheduler.scheduleAtFixedRate(() -> {
			try {
				if (active != owner || !inWorld || owner == null || owner.isDead()) {
					return;
				}
				boolean updated = false;
				double maxHp = owner.maxHp();
				double maxMp = owner.maxMp();
				double maxCp = owner.maxCp();

				// Multiplicador de postura (sentado recupera muito mais rapido)
				double stanceMod = owner.sitting() ? 1.5 : (owner.running() ? 0.7 : 1.1);

				// Bonus de atributos
				var t = ctx.characters() != null ? ctx.characters().template(owner) : null;
				int con = t != null ? t.con() : 25;
				int men = t != null ? t.men() : 25;
				double conBonus = Math.max(0.5, con / 25.0);
				double menBonus = Math.max(0.5, men / 25.0);

				// 1. HP Regen: base ~ 1.5 + level/10
				if (owner.currentHp() < maxHp) {
					double baseHpRegen = (1.5 + (owner.level() / 10.0)) * conBonus * stanceMod;
					double newHp = Math.min(maxHp, owner.currentHp() + baseHpRegen);
					if (newHp != owner.currentHp()) {
						owner.currentHp(newHp);
						updated = true;
					}
				}

				// 2. MP Regen: base ~ 0.9 + level/12
				if (owner.currentMp() < maxMp) {
					double baseMpRegen = (0.9 + (owner.level() / 12.0)) * menBonus * stanceMod;
					double newMp = Math.min(maxMp, owner.currentMp() + baseMpRegen);
					if (newMp != owner.currentMp()) {
						owner.currentMp(newMp);
						updated = true;
					}
				}

				// 3. CP Regen: base ~ 1.0 + level/15
				if (owner.currentCp() < maxCp) {
					double baseCpRegen = (1.0 + (owner.level() / 15.0)) * conBonus * stanceMod;
					double newCp = Math.min(maxCp, owner.currentCp() + baseCpRegen);
					if (newCp != owner.currentCp()) {
						owner.currentCp(newCp);
						updated = true;
					}
				}

				if (updated) {
					sendVitals();
				}
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
				var msu = new MagicSkillUse(active.objectId(), active.objectId(), 2099, 1, hitTime, 0,
						active.x(), active.y(), active.z(), active.x(), active.y(), active.z());
				send(msu);
				ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, msu, false);
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Transporting to the nearest village in 30 seconds..."));

				casting = true;
				castTask = autoAttackScheduler.schedule(() -> {
					casting = false;
					if (!inWorld || active == null || active.isDead()) {
						return;
					}
					int[] dest = findNearestTown(active.x(), active.y());
					active.moveTo(dest[0], dest[1], dest[2]);
					ctx.characters().save(active, true);
					var tele = new TeleportToLocation(active.objectId(), dest[0], dest[1], dest[2]);
					send(tele);
					ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, tele, false);
					updateKnownObjects();
				}, hitTime, TimeUnit.MILLISECONDS);
			}
			case 77 -> { // /time
				int now = GameTime.now();
				int h = (now / 60) % 24;
				int m = now % 60;
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", String.format("Game time: %02d:%02d", h, m)));
			}
			default -> log.debug("UserCommand {} nao tratado", commandId);
		}
	}

	private void handleSlashCommand(String cmd) {
		String lower = cmd.toLowerCase(java.util.Locale.ROOT).trim();
		if (lower.equals("/loc")) {
			onUserCommand(0);
		} else if (lower.equals("/unstuck")) {
			onUserCommand(52);
		} else if (lower.equals("/time")) {
			onUserCommand(77);
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
			if (other.objectId() != active.objectId() && other.name().toLowerCase(java.util.Locale.ROOT).startsWith(query)) {
				targetObjectId = other.objectId();
				send(new MyTargetSelected(other.objectId(), 0));
				if (other.character() != null) {
					send(StatusUpdate.hp(other.objectId(), (int) other.character().currentHp(), other.character().maxHp()));
				}
				send(new ValidateLocation(other.objectId(), other.x(), other.y(), other.z(), 0));
				return;
			}
		}
		send(new ActionFailed());
	}

	private void handleDotCommand(String cmd) {
		String lower = cmd.toLowerCase(java.util.Locale.ROOT).trim();
		if (lower.equals(".online")) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Jogadores online: " + ctx.world().players().size()));
		} else if (lower.equals(".stats") || lower.equals(".menu")) {
			var t = ctx.characters() != null ? ctx.characters().template(active) : null;
			var stats = PlayerStats.calculate(active, t);
			send(new CreatureSay(0, CreatureSay.ALL, "SYS",
					String.format("%s (Nv %d): P.Atk %d, M.Atk %d, P.Def %d, M.Def %d, AtkSpd %d, CastSpd %d",
							active.name(), active.level(), stats.pAtk(), stats.mAtk(), stats.pDef(), stats.mDef(),
							stats.pAtkSpd(), stats.mAtkSpd())));
		} else {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Comandos de voz: .online, .stats"));
		}
	}

	public void teleportToLocation(int x, int y, int z) {
		if (!inWorld || active == null) {
			return;
		}
		active.sitting(false);
		active.moveTo(x, y, z);
		ctx.characters().save(active, true);
		var tele = new TeleportToLocation(active.objectId(), x, y, z);
		send(tele);
		updateKnownObjects();
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
		} else if ("show_moves".equalsIgnoreCase(clean) || "teleports".equalsIgnoreCase(clean) || "tele".equalsIgnoreCase(clean) || "tele_menu".equalsIgnoreCase(clean)) {
			clean = "tele/teleports.htm";
		} else if ("gmshop".equalsIgnoreCase(clean) || "adminshop".equalsIgnoreCase(clean) || "shop".equalsIgnoreCase(clean)) {
			clean = "menus/submenus/adminshop_menu.htm";
		} else if ("enchant".equalsIgnoreCase(clean) || "enchant_menu".equalsIgnoreCase(clean)) {
			clean = "menus/submenus/enchant_menu.htm";
		} else if ("spawn_menu".equalsIgnoreCase(clean) || "spawnmenu".equalsIgnoreCase(clean)) {
			clean = "menus/submenus/spawn_menu.htm";
		} else if ("show_skills".equalsIgnoreCase(clean) || "skills_menu".equalsIgnoreCase(clean) || "skills".equalsIgnoreCase(clean)) {
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
		} else if ("config".equalsIgnoreCase(clean) || "configs".equalsIgnoreCase(clean) || "config_menu".equalsIgnoreCase(clean)) {
			clean = "menus/config.htm";
		} else if ("events".equalsIgnoreCase(clean) || "events_menu".equalsIgnoreCase(clean)) {
			clean = "menus/events.htm";
		} else if ("charedit".equalsIgnoreCase(clean) || "charedit_menu".equalsIgnoreCase(clean) || "current_player".equalsIgnoreCase(clean)) {
			clean = "menus/submenus/charedit_menu.htm";
		} else if ("charinfo".equalsIgnoreCase(clean) || "charinfo_menu".equalsIgnoreCase(clean)) {
			clean = "menus/submenus/charinfo_menu.htm";
		} else if ("charlist".equalsIgnoreCase(clean) || "charlist_menu".equalsIgnoreCase(clean) || "find_character".equalsIgnoreCase(clean)) {
			clean = "menus/submenus/charlist_menu.htm";
		} else if ("gmmenu".equalsIgnoreCase(clean)) {
			clean = "menus/submenus/gmmenu.htm";
		} else if ("itemcreation".equalsIgnoreCase(clean) || "itemcreation_menu".equalsIgnoreCase(clean) || "itemcreate".equalsIgnoreCase(clean)) {
			clean = "menus/submenus/itemcreation_menu.htm";
		} else if ("expsp".equalsIgnoreCase(clean) || "expsp_menu".equalsIgnoreCase(clean)) {
			clean = "menus/submenus/expsp_menu.htm";
		} else if ("charclasses".equalsIgnoreCase(clean) || "charclasses_menu".equalsIgnoreCase(clean)) {
			clean = "menus/submenus/charclasses_menu.htm";
		} else if ("cwinfo".equalsIgnoreCase(clean) || "cw_info_menu".equalsIgnoreCase(clean)) {
			clean = "menus/submenus/cwinfo.htm";
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
			}
			candidates.add("admin/menus/" + clean);
			candidates.add("admin/menus/submenus/" + clean);
			candidates.add("admin/gmshop/" + clean);
			candidates.add("admin/tele/" + clean);
			candidates.add("admin/skills/" + clean);
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

	private void showAdminHtml(String requested) {
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
					.replace("%currenthp%", String.valueOf((int) targetChar.currentHp()))
					.replace("%maxhp%", String.valueOf(targetChar.maxHp()))
					.replace("%currentmp%", String.valueOf((int) targetChar.currentMp()))
					.replace("%maxmp%", String.valueOf(targetChar.maxMp()))
					.replace("%currentcp%", String.valueOf((int) targetChar.currentCp()))
					.replace("%maxcp%", String.valueOf(targetChar.maxCp()))
					.replace("%class%", String.valueOf(targetChar.classId()))
					.replace("%level%", String.valueOf(targetChar.level()))
					.replace("%title%", targetChar.title() != null ? targetChar.title() : "");
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
					send(new GMViewCharacterInfo(targetChar, template, stats, targetChar.inventory().paperdollView(), targetChar.inventory().currentLoad()));
				}
			}
			case 2 -> send(new GMViewPledgeInfo(targetChar.name(), targetChar.clanId(), targetChar.level(), targetChar.classId()));
			case 3 -> send(new GMViewSkillInfo(targetChar.name(), targetChar.skills(), ctx.skillService()));
			case 4 -> send(new GMViewQuestInfo(targetChar.name()));
			case 5 -> {
				send(new GMViewItemList(targetChar.name(), targetChar.inventory().items(), 80));
				send(new GMHennaInfo(0, 0, 0, 0, 0, 0));
			}
			case 6 -> send(new GMViewWarehouseWithdrawList(targetChar.name(), (int) Math.min(Integer.MAX_VALUE, targetChar.inventory().adena()), List.of()));
			default -> {
				if (template != null && stats != null) {
					send(new GMViewCharacterInfo(targetChar, template, stats, targetChar.inventory().paperdollView(), targetChar.inventory().currentLoad()));
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
		List<PlayerCharacter> pageList = (fromIndex < totalPlayers) ? allPlayers.subList(fromIndex, toIndex) : List.of();

		StringBuilder rows = new StringBuilder();
		if (pageList.isEmpty()) {
			rows.append("<tr><td colspan=3><center><font color=\"LEVEL\">No characters found.</font></center></td></tr>");
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
						.append("<td width=80><a action=\"bypass -h admin_character_info ").append(pc.name()).append("\">").append(nameDisplay).append("</a></td>")
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
					pages.append("<td><button value=\"[").append(p).append("]\" action=\"bypass -h admin_show_characters ").append(safeQuery).append(" ").append(p).append("\" width=30 height=19 back=\"L2UI_CH3.smallbutton1_over\" fore=\"L2UI_CH3.smallbutton1\"></td>");
				} else {
					pages.append("<td><button value=\"").append(p).append("\" action=\"bypass -h admin_show_characters ").append(safeQuery).append(" ").append(p).append("\" width=30 height=19 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td>");
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

	private void showAdminCharInfo(String charName) {
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

	private void handleAdminCommand(String fullCmd) {
		if (active == null || !active.isGm()) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao tem permissao de Administrador."));
			send(new ActionFailed());
			return;
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
		String cmd = spaceIdx > 0 ? trimmed.substring(0, spaceIdx).toLowerCase(java.util.Locale.ROOT) : trimmed.toLowerCase(java.util.Locale.ROOT);
		String args = spaceIdx > 0 ? trimmed.substring(spaceIdx + 1).trim() : "";

		switch (cmd) {
			case "admin", "main" -> showAdminHtml("menus/main.htm");
			case "gamemenu", "game" -> showAdminHtml("menus/game.htm");
			case "server", "servermenu" -> showAdminHtml("menus/server.htm");
			case "effects", "effectsmenu" -> showAdminHtml("menus/effects.htm");
			case "mod", "mods" -> showAdminHtml("menus/mod.htm");
			case "show_moves", "teleports", "tele", "tele_menu" -> showAdminHtml("tele/teleports.htm");
			case "enchant", "enchant_menu" -> showAdminHtml("menus/submenus/enchant_menu.htm");
			case "gmshop", "adminshop" -> showAdminHtml("menus/submenus/adminshop_menu.htm");
			case "spawn_menu", "spawnmenu" -> showAdminHtml("menus/submenus/spawn_menu.htm");
			case "announce_menu" -> showAdminHtml("menus/submenus/announce_menu.htm");
			case "social_menu", "socialmenu" -> showAdminHtml("menus/submenus/social_menu.htm");
			case "abnormal_menu", "abnormalmenu" -> showAdminHtml("menus/submenus/abnormal_menu.htm");
			case "control", "control_menu" -> showAdminHtml("menus/control.htm");
			case "players", "players_menu" -> showAdminHtml("menus/players.htm");
			case "config", "configs", "config_menu" -> showAdminHtml("menus/config.htm");
			case "events", "events_menu" -> showAdminHtml("menus/events.htm");
			case "show_skills", "skills_menu" -> showAdminHtml("menus/submenus/skills_menu.htm");
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
					try { p = Integer.parseInt(parts[1]); } catch (Exception ignored) {}
				} else if (parts.length == 1 && !parts[0].isEmpty()) {
					try {
						p = Integer.parseInt(parts[0]);
					} catch (Exception e) {
						q = parts[0];
					}
				}
				showAdminCharList(q, p);
			}
			case "altg", "gametool" -> handleGMCommand(args.isBlank() ? (getTargetPlayerOrActive() != null ? getTargetPlayerOrActive().name() : active.name()) : args, 1);
			case "gmmenu" -> showAdminHtml("menus/submenus/gmmenu.htm");
			case "itemcreation", "itemcreation_menu", "itemcreate" -> showAdminHtml("menus/submenus/itemcreation_menu.htm");
			case "expsp", "expsp_menu" -> showAdminHtml("menus/submenus/expsp_menu.htm");
			case "charclasses", "charclasses_menu" -> showAdminHtml("menus/submenus/charclasses_menu.htm");
			case "cwinfo", "cw_info_menu" -> showAdminHtml("menus/submenus/cwinfo.htm");

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
						ctx.skills().save(destChar.objectId(), 0, new com.lopez.l2j.game.skill.Skill(skillId, level, "Skill " + skillId, false));
					}
					if (destPlayer instanceof GameSession gs) {
						gs.sendSkillList();
					}
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Skill " + skillId + " nv " + level + " concedida a " + destChar.name()));
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //skill <skillId> [level]"));
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
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Skill " + skillId + " removida de " + destChar.name()));
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
							new StatusUpdate.Attribute(StatusUpdate.MAX_CP, targetChar.maxCp())
					)));
					targetPlayer.send(new UserInfo(targetChar, ctx.characters().template(targetChar)));
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", targetChar.name() + " foi ressuscitado."));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "O alvo nao esta morto."));
				}
			}
			case "speed", "gmspeed" -> {
				try {
					int spd = args.isEmpty() ? 0 : Integer.parseInt(args.trim().split("\\s+")[0]);
					active.gmSpeed(spd);
					send(new UserInfo(active, ctx.characters().template(active)));
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "GM Speed definido para: " + spd));
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //speed <0-5>"));
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
				ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, new DeleteObject(active.objectId()), false);
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
					int mask = Integer.parseInt(args.trim().split("\\s+")[0]);
					var targetChar = getTargetPlayerOrActive();
					var targetPlayer = getTargetOnlinePlayerOrSelf();
					targetPlayer.send(new MagicEffectIcons(List.of()));
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Efeito abnormal " + mask + " aplicado a " + targetChar.name()));
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //abnormal <bitmask>"));
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
					sess.send(new MagicEffectIcons(List.of()));
					sess.send(new UserInfo(targetChar, ctx.characters().template(targetChar)));
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Buffs de " + targetChar.name() + " foram removidos."));
				}
			}
			case "diet" -> {
				active.diet(!active.diet());
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Diet mode: " + (active.diet() ? "ON" : "OFF")));
			}
			case "silence" -> {
				active.silence(!active.silence());
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Message Refusal / Silence: " + (active.silence() ? "ON" : "OFF")));
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
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "PK Kills de " + p.name() + " alterado para " + val));
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
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "PvP Kills de " + p.name() + " alterado para " + val));
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
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Titulo de " + p.name() + " alterado para " + p.title()));
			}
			case "save_modifications" -> {
				try {
					String[] parts = args.trim().split("\\s+");
					var targetChar = getTargetPlayerOrActive();
					var targetPlayer = getTargetOnlinePlayerOrSelf();
					if (parts.length >= 1 && !parts[0].isEmpty()) targetChar.currentHp(Double.parseDouble(parts[0]));
					if (parts.length >= 2 && !parts[1].isEmpty()) targetChar.currentMp(Double.parseDouble(parts[1]));
					if (parts.length >= 3 && !parts[2].isEmpty()) targetChar.currentCp(Double.parseDouble(parts[2]));
					if (parts.length >= 5 && !parts[4].isEmpty()) targetChar.pvpKills(Integer.parseInt(parts[4]));
					if (parts.length >= 6 && !parts[5].isEmpty()) targetChar.pkKills(Integer.parseInt(parts[5]));
					ctx.characters().save(targetChar, true);
					targetPlayer.send(new UserInfo(targetChar, ctx.characters().template(targetChar)));
					targetPlayer.send(new StatusUpdate(targetChar.objectId(), List.of(
							new StatusUpdate.Attribute(StatusUpdate.CUR_HP, (int) targetChar.currentHp()),
							new StatusUpdate.Attribute(StatusUpdate.MAX_HP, targetChar.maxHp()),
							new StatusUpdate.Attribute(StatusUpdate.CUR_MP, (int) targetChar.currentMp()),
							new StatusUpdate.Attribute(StatusUpdate.MAX_MP, targetChar.maxMp()),
							new StatusUpdate.Attribute(StatusUpdate.CUR_CP, (int) targetChar.currentCp()),
							new StatusUpdate.Attribute(StatusUpdate.MAX_CP, targetChar.maxCp())
					)));
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Modificacoes salvas para " + targetChar.name()));
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Erro ao salvar modificacoes: " + e.getMessage()));
				}
			}
			case "setcolor" -> send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Cor alterada."));
			case "rec" -> send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Recomendacoes atualizadas."));
			case "atmosphere" -> send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Atmosfera alterada: " + args));
			case "earthquake" -> send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Terremoto ativado."));
			case "kick" -> {
				if (!args.isBlank()) {
					var target = ctx.world().byName(args.trim()).orElse(null);
					if (target != null) {
						target.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce foi desconectado pelo administrador."));
						target.send(new ActionFailed());
						if (target instanceof GameSession s) {
							s.closeRequested = true;
							s.leaveWorld();
						}
						ctx.world().remove(target);
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Jogador " + args.trim() + " desconectado."));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Jogador " + args.trim() + " nao encontrado."));
					}
				}
			}
			case "ban", "banchar" -> {
				if (!args.isBlank()) {
					String targetName = args.trim().split("\\s+")[0];
					if (ctx.characters() != null) {
						ctx.characters().setAccessLevelByName(targetName, -100);
					}
					var target = ctx.world().byName(targetName).orElse(null);
					if (target != null) {
						target.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sua conta foi banida pelo administrador."));
						target.send(new ActionFailed());
						if (target instanceof GameSession s) {
							s.closeRequested = true;
							s.leaveWorld();
						}
						ctx.world().remove(target);
					}
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Personagem/Conta " + targetName + " banido."));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //ban <nome>"));
				}
			}
			case "reload" -> {
				String type = args.toLowerCase(java.util.Locale.ROOT).trim();
				if (type.contains("skill")) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Skills recarregadas com sucesso."));
				} else if (type.contains("html") || type.contains("htm")) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "HTMLs recarregados com sucesso."));
				} else if (type.contains("multisell")) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Multisell recarregado com sucesso."));
				} else {
					showAdminHtml("menus/submenus/reload_menu.htm");
				}
			}
			case "seteh", "seteg", "seteb", "setel", "setes", "seten", "setre", "setle", "setrf", "setlf", "setun", "setba" -> {
				try {
					int val = Integer.parseInt(args.trim().split("\\s+")[0]);
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
							send(new CreatureSay(0, CreatureSay.ALL, "SYS", piece.template().name() + " encantado para +" + val));
						} else {
							send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nenhum item equipado no slot selecionado."));
						}
					}
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //" + cmd + " <enchantLevel>"));
				}
			}
			case "move_to", "teleportto" -> {
				String[] parts = args.split("\\s+");
				if (parts.length >= 3) {
					try {
						int x = Integer.parseInt(parts[0]);
						int y = Integer.parseInt(parts[1]);
						int z = Integer.parseInt(parts[2]);
						teleportToLocation(x, y, z);
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Teleportado para: " + x + ", " + y + ", " + z));
					} catch (NumberFormatException e) {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //teleportto <x> <y> <z> ou //teleportto <nome>"));
					}
				} else if (parts.length >= 1 && !parts[0].isEmpty()) {
					var target = ctx.world().byName(parts[0]).orElse(null);
					if (target != null) {
						teleportToLocation(target.x(), target.y(), target.z());
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Teleportado para " + target.name()));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Jogador '" + parts[0] + "' nao encontrado ou offline."));
					}
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //teleportto <x> <y> <z> ou //teleportto <nome>"));
				}
			}
			case "create_item", "item" -> {
				String[] parts = args.split("\\s+");
				if (parts.length >= 1 && !parts[0].isEmpty()) {
					try {
						int itemId = Integer.parseInt(parts[0]);
						int count = parts.length >= 2 ? (int) Math.min(Integer.MAX_VALUE, Long.parseLong(parts[1])) : 1;
						var targetPlayer = (targetObjectId != 0 && targetObjectId != active.objectId())
								? ctx.world().player(targetObjectId).orElse(null)
								: null;
						var destChar = (targetPlayer != null && targetPlayer.character() != null) ? targetPlayer.character() : active;
						var destPlayer = (targetPlayer != null) ? targetPlayer : this;

						var added = ctx.inventories().addItem(destChar.inventory(), itemId, count, "AdminCreate");
						if (added != null) {
							destPlayer.send(new InventoryUpdate(List.of(ItemInfo.of(added.item(), added.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED))));
							destPlayer.send(new UserInfo(destChar, ctx.characters().template(destChar)));
							send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Item " + itemId + " x" + count + " entregue a " + destChar.name()));
						} else {
							send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Falha ao criar item " + itemId));
						}
					} catch (NumberFormatException e) {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //item <itemId> [count]"));
					}
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //item <itemId> [count]"));
				}
			}
			case "spawn", "spawn_monster" -> {
				String[] parts = args.split("\\s+");
				if (parts.length >= 1 && !parts[0].isEmpty() && ctx.spawns() != null) {
					int count = parts.length >= 2 ? Math.min(50, Math.max(1, parseIntSafe(parts[1], 1))) : 1;
					try {
						int npcId = Integer.parseInt(parts[0]);
						for (int i = 0; i < count; i++) {
							ctx.spawns().spawn(npcId, active.x(), active.y(), active.z(), active.heading());
						}
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Spawned " + count + "x NPC id " + npcId));
						updateKnownObjects();
					} catch (NumberFormatException e) {
						String searchName = parts[0].replace('_', ' ').toLowerCase(java.util.Locale.ROOT);
						var found = ctx.world().npcs().stream()
								.filter(n -> n.name().toLowerCase(java.util.Locale.ROOT).contains(searchName))
								.findFirst()
								.map(NpcInstance::npcId);
						if (found.isPresent()) {
							for (int i = 0; i < count; i++) {
								ctx.spawns().spawn(found.get(), active.x(), active.y(), active.z(), active.heading());
							}
							send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Spawned " + count + "x NPC id " + found.get()));
							updateKnownObjects();
						} else {
							send(new CreatureSay(0, CreatureSay.ALL, "SYS", "NPC nao encontrado por nome/id: " + parts[0]));
						}
					}
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //spawn <npcId|npcName> [count]"));
				}
			}
			case "delete", "unspawn" -> {
				if (targetObjectId != 0) {
					var npcOpt = ctx.world().npc(targetObjectId);
					if (npcOpt.isPresent()) {
						var npc = npcOpt.get();
						ctx.world().removeNpc(npc);
						ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, new DeleteObject(npc.objectId()), false);
						send(new DeleteObject(npc.objectId()));
						targetObjectId = 0;
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "NPC " + npc.name() + " removido do mundo."));
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
						new StatusUpdate.Attribute(StatusUpdate.MAX_CP, targetChar.maxCp())
				)));
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", targetChar.name() + " foi totalmente curado."));
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
							pOnline.character().currentHp(0);
							pOnline.send(new StatusUpdate(pOnline.objectId(), List.of(
									new StatusUpdate.Attribute(StatusUpdate.CUR_HP, 0)
							)));
							var die = new Die(pOnline.objectId(), false);
							pOnline.send(die);
							if (pOnline instanceof GameSession pSess) {
								ctx.world().broadcastAround(pSess, GameWorld.VISIBILITY_RADIUS, die, false);
							}
							send(new CreatureSay(0, CreatureSay.ALL, "SYS", pOnline.name() + " foi morto."));
						}
					}
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Selecione um alvo para matar."));
				}
			}
			case "setlevel", "set_level" -> {
				try {
					int newLevel = Integer.parseInt(args.trim());
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
							new StatusUpdate.Attribute(StatusUpdate.MAX_CP, targetChar.maxCp())
					)));
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nivel de " + targetChar.name() + " alterado para " + newLevel));
				} catch (NumberFormatException e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //setlevel <1-80>"));
				}
			}
			case "setew", "enchant_weapon" -> {
				try {
					int val = Integer.parseInt(args.trim());
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
				} catch (NumberFormatException e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //setew <enchantLevel>"));
				}
			}
			case "setec", "enchant_armor" -> {
				try {
					int val = Integer.parseInt(args.trim());
					int[] armorSlots = { ItemSlots.CHEST, ItemSlots.LEGS, ItemSlots.HEAD, ItemSlots.GLOVES, ItemSlots.FEET };
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
				} catch (NumberFormatException e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //setec <enchantLevel>"));
				}
			}
			case "announce" -> {
				if (!args.isEmpty()) {
					ctx.world().broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, active.name(), args), x -> true);
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //announce <mensagem>"));
				}
			}
			case "invul" -> {
				active.currentHp(active.maxHp());
				active.currentCp(active.maxCp());
				active.currentMp(active.maxMp());
				send(new StatusUpdate(active.objectId(), List.of(
						new StatusUpdate.Attribute(StatusUpdate.CUR_HP, (int) active.currentHp()),
						new StatusUpdate.Attribute(StatusUpdate.MAX_HP, active.maxHp()),
						new StatusUpdate.Attribute(StatusUpdate.CUR_MP, (int) active.currentMp()),
						new StatusUpdate.Attribute(StatusUpdate.MAX_MP, active.maxMp()),
						new StatusUpdate.Attribute(StatusUpdate.CUR_CP, (int) active.currentCp()),
						new StatusUpdate.Attribute(StatusUpdate.MAX_CP, active.maxCp())
				)));
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Invulnerabilidade e status no maximo."));
			}
			case "goname", "goto" -> {
				if (!args.isEmpty()) {
					var target = ctx.world().byName(args).orElse(null);
					if (target != null) {
						teleportToLocation(target.x(), target.y(), target.z());
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Teleportado para " + target.name()));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Jogador '" + args + "' nao encontrado ou offline."));
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
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Jogador " + target.name() + " puxado para sua posicao."));
					} else if (target != null && target.character() != null) {
						target.character().moveTo(active.x(), active.y(), active.z());
						target.send(new TeleportToLocation(target.objectId(), active.x(), active.y(), active.z()));
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Jogador " + target.name() + " puxado para sua posicao."));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Jogador '" + args + "' nao encontrado ou offline."));
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
						target.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Seu nivel de acesso administrativo foi atualizado para: " + level));
					}
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "AccessLevel de " + targetName + " definido para " + level + " no banco de dados."));
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
		sink.accept(packet);
	}

	private void onEnchantItem(RequestEnchantItem p) {
		if (!inWorld || active == null || active.isDead()) {
			send(new ActionFailed());
			return;
		}
		int scrollObjectId = activeEnchantScrollObjectId;
		activeEnchantScrollObjectId = 0;
		if (scrollObjectId == 0) {
			send(EnchantResult.CANCEL);
			send(new ActionFailed());
			return;
		}
		var scrollOpt = active.inventory().byObjectId(scrollObjectId);
		if (scrollOpt.isEmpty()) {
			send(EnchantResult.CANCEL);
			send(new ActionFailed());
			return;
		}
		var scroll = scrollOpt.get();
		var scrollInfoOpt = EnchantScrollTable.get(scroll.itemId());
		if (scrollInfoOpt.isEmpty()) {
			send(EnchantResult.CANCEL);
			send(new ActionFailed());
			return;
		}
		var scrollInfo = scrollInfoOpt.get();
		var targetOpt = active.inventory().byObjectId(p.objectId());
		if (targetOpt.isEmpty()) {
			send(EnchantResult.CANCEL);
			send(new ActionFailed());
			return;
		}
		var target = targetOpt.get();
		if (!target.template().isEquipable()) {
			send(SystemMessage.id(SystemMessage.INAPPROPRIATE_ENCHANT_CONDITION));
			send(EnchantResult.CANCEL);
			return;
		}

		// Valida compatibilidade de grade
		String targetGrade = target.template().crystalType();
		if (targetGrade == null || !targetGrade.equalsIgnoreCase(scrollInfo.grade())) {
			send(SystemMessage.id(SystemMessage.INAPPROPRIATE_ENCHANT_CONDITION));
			send(EnchantResult.CANCEL);
			return;
		}

		// Valida tipo: arma vs armor/accessory/shield
		boolean isWeapon = target.template().type2() == ItemTemplate.TYPE2_WEAPON;
		if (scrollInfo.isWeapon() != isWeapon) {
			send(SystemMessage.id(SystemMessage.INAPPROPRIATE_ENCHANT_CONDITION));
			send(EnchantResult.CANCEL);
			return;
		}

		// Consome o scroll
		var consumedScroll = ctx.inventories().destroyItem(active.inventory(), scroll.objectId(), 1, "Enchant");
		if (consumedScroll == null) {
			send(EnchantResult.CANCEL);
			return;
		}
		send(new InventoryUpdate(List.of(ItemInfo.of(consumedScroll.item(), consumedScroll.removed() ? ItemInfo.REMOVED : ItemInfo.MODIFIED))));

		// Calculo de seguranca e chance de sucesso
		int safeLimit = (target.template().bodyPart() == com.lopez.l2j.game.item.ItemSlots.SLOT_FULL_ARMOR) ? 4 : 3;
		boolean success;
		if (target.enchant() < safeLimit) {
			success = true;
		} else {
			// Taxa retail: 66% de chance
			success = java.util.concurrent.ThreadLocalRandom.current().nextInt(100) < 66;
		}

		if (success) {
			target.enchant(target.enchant() + 1);
			ctx.inventories().saveItem(target);
			send(EnchantResult.SUCCESS);
			if (target.enchant() == 1) {
				send(SystemMessage.of(SystemMessage.S1_SUCCESSFULLY_ENCHANTED, new SystemMessage.ItemName(target.itemId())));
			} else {
				send(SystemMessage.of(SystemMessage.S1_S2_SUCCESSFULLY_ENCHANTED,
						new SystemMessage.Number(target.enchant()), new SystemMessage.ItemName(target.itemId())));
			}
			send(new InventoryUpdate(List.of(ItemInfo.of(target, ItemInfo.MODIFIED))));
			broadcastAppearance();
		} else {
			if (scrollInfo.isBlessed()) {
				// Blessed scroll: nao quebra, reseta para 0
				target.enchant(0);
				ctx.inventories().saveItem(target);
				send(SystemMessage.id(SystemMessage.BLESSED_ENCHANT_FAILED));
				send(EnchantResult.BLESSED_FAIL);
				send(new InventoryUpdate(List.of(ItemInfo.of(target, ItemInfo.MODIFIED))));
				broadcastAppearance();
			} else {
				// Normal scroll: quebra o item
				int oldEnchant = target.enchant();
				int itemId = target.itemId();
				if (target.isEquipped()) {
					afterEquipChange(ctx.inventories().toggleEquip(active.inventory(), target.objectId()));
				}
				ctx.inventories().destroyItem(active.inventory(), target.objectId(), 1, "EnchantBreak");
				if (oldEnchant > 0) {
					send(SystemMessage.of(SystemMessage.ENCHANTMENT_FAILED_S1_S2_EVAPORATED,
							new SystemMessage.Number(oldEnchant), new SystemMessage.ItemName(itemId)));
				} else {
					send(SystemMessage.of(SystemMessage.ENCHANTMENT_FAILED_S1_EVAPORATED, new SystemMessage.ItemName(itemId)));
				}
				send(EnchantResult.FAIL);
				send(new InventoryUpdate(List.of(ItemInfo.of(target, ItemInfo.REMOVED))));
				broadcastAppearance();
			}
		}
	}

	private void onWareHouseDeposit(SendWareHouseDepositList p) {
		if (!inWorld || active == null || ctx.warehouse() == null || p.items().isEmpty()) {
			send(new ActionFailed());
			return;
		}
		List<ItemInfo> updates = new ArrayList<>();
		var adenaBefore = active.inventory().byItemId(ItemTemplate.ADENA_ID).map(ItemInstance::count).orElse(0);

		for (var req : p.items()) {
			var opt = active.inventory().byObjectId(req.objectId());
			if (opt.isEmpty()) {
				continue;
			}
			var item = opt.get();
			int count = Math.min(req.count(), item.count());
			if (count <= 0 || item.isEquipped()) {
				continue;
			}
			int prevCount = item.count();
			boolean ok = ctx.warehouse().depositItem(active.inventory(), item.objectId(), count);
			if (ok) {
				if (prevCount == count) {
					updates.add(ItemInfo.of(item, ItemInfo.REMOVED));
				} else {
					updates.add(ItemInfo.of(item, ItemInfo.MODIFIED));
				}
			}
		}

		var adenaAfter = active.inventory().byItemId(ItemTemplate.ADENA_ID).orElse(null);
		if (adenaAfter != null && adenaAfter.count() != adenaBefore) {
			updates.add(ItemInfo.of(adenaAfter, adenaAfter.count() == 0 ? ItemInfo.REMOVED : ItemInfo.MODIFIED));
		}

		if (!updates.isEmpty()) {
			send(new InventoryUpdate(updates));
			send(new StatusUpdate(active.objectId(),
					List.of(new StatusUpdate.Attribute(StatusUpdate.CUR_LOAD, active.inventory().currentLoad()))));
		}
		send(new ActionFailed());
	}

	private void onWareHouseWithdraw(SendWareHouseWithDrawList p) {
		if (!inWorld || active == null || ctx.warehouse() == null || p.items().isEmpty()) {
			send(new ActionFailed());
			return;
		}
		List<ItemInfo> updates = new ArrayList<>();
		for (var req : p.items()) {
			int beforeCount = active.inventory().byObjectId(req.objectId()).map(ItemInstance::count).orElse(0);
			boolean ok = ctx.warehouse().withdrawItem(active.inventory(), active.objectId(), req.objectId(), req.count());
			if (ok) {
				var item = active.inventory().byObjectId(req.objectId()).orElse(null);
				if (item != null) {
					updates.add(ItemInfo.of(item, beforeCount == 0 ? ItemInfo.ADDED : ItemInfo.MODIFIED));
				}
			}
		}
		if (!updates.isEmpty()) {
			send(new InventoryUpdate(updates));
			send(new StatusUpdate(active.objectId(),
					List.of(new StatusUpdate.Attribute(StatusUpdate.CUR_LOAD, active.inventory().currentLoad()))));
		}
		send(new ActionFailed());
	}

	private void onDestroyItem(RequestDestroyItem p) {
		if (!inWorld || active == null || active.isDead()) {
			send(new ActionFailed());
			return;
		}
		var opt = active.inventory().byObjectId(p.objectId());
		if (opt.isEmpty()) {
			send(new ActionFailed());
			return;
		}
		var item = opt.get();
		if (item.isEquipped() || !item.template().destroyable() || p.count() <= 0 || item.count() < p.count()) {
			send(new ActionFailed());
			return;
		}
		var result = ctx.inventories().destroyItem(active.inventory(), p.objectId(), p.count(), "UserDestroy");
		if (result == null) {
			send(new ActionFailed());
			return;
		}
		send(new InventoryUpdate(List.of(ItemInfo.of(result.item(), result.removed() ? ItemInfo.REMOVED : ItemInfo.MODIFIED))));
		send(new StatusUpdate(active.objectId(),
				List.of(new StatusUpdate.Attribute(StatusUpdate.CUR_LOAD, active.inventory().currentLoad()))));
		send(new ActionFailed());
	}

	public GameServerPacket charInfo() {
		return active == null ? null : new CharInfo(active, ctx.characters().template(active));
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

	private static int parseIntSafe(String val, int def) {
		try {
			return Integer.parseInt(val.trim());
		} catch (Exception e) {
			return def;
		}
	}
}
