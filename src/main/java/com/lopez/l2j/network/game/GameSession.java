package com.lopez.l2j.network.game;

import com.lopez.l2j.game.item.ItemSlots;
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
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.teleport.TeleportLocation;
import com.lopez.l2j.game.teleport.TeleportLocationTable;
import com.lopez.l2j.game.trade.BuyListTable;
import com.lopez.l2j.game.trade.NpcBuyList;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestBuyItem;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestBypassToServer;
import com.lopez.l2j.network.game.packet.GameServerPacket.BuyList;
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
			com.lopez.l2j.config.ServerProperties.Rates rates, String serverName) {
		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, null, null, null, null, null, null, null, null, null, null, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, null, null, null, null, null, null, null, null, null, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists, null, null, null, null, null, null, null, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists, combat, null, null, null, null, null, null, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat,
				com.lopez.l2j.game.drop.DropService drops, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists, combat, drops, null, null, null, null, null, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat,
				com.lopez.l2j.game.drop.DropService drops, ShortCutRepository shortcuts,
				SkillRepository skills, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists, combat, drops, shortcuts, skills, null, null, null, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat,
				com.lopez.l2j.game.drop.DropService drops, ShortCutRepository shortcuts,
				SkillRepository skills, com.lopez.l2j.game.ai.NpcAiService npcAi, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists, combat, drops, shortcuts, skills, npcAi, null, null, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat,
				com.lopez.l2j.game.drop.DropService drops, ShortCutRepository shortcuts,
				SkillRepository skills, com.lopez.l2j.game.ai.NpcAiService npcAi, SkillService skillService,
				String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists, combat, drops, shortcuts, skills, npcAi, skillService, null, serverName);
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
	/** Reuse por skillId do consumivel (epoch ms em que libera). */
	private final Map<Integer, Long> consumableReuse = new ConcurrentHashMap<>();
	/** Curas ao longo do tempo ativas por stackType (HpRecover/MpRecover). */
	private final Map<String, ScheduledFuture<?>> hotTasks = new ConcurrentHashMap<>();

	private com.lopez.l2j.game.party.Party party;
	private RequestPartyPending pendingPartyInvite;
	private volatile long attackEndTime;
	private volatile int pendingNpcInteractObjectId;

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
			case RequestBypassToServer p -> onBypass(p);
			case RequestBuyItem p -> onBuyItem(p);
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
		updateKnownObjects();
		send(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, ctx.serverName(),
				"Bem-vindo ao " + ctx.serverName() + ", " + active.name() + "!"));
		log.info("{} ({}) entrou no mundo em {},{},{}", active.name(), account, x(), y(), z());
	}

	private void onMove(MoveBackwardToLocation p) {
		if (!inWorld || active.sitting()) {
			send(new ActionFailed());
			return;
		}
		pendingNpcInteractObjectId = 0;
		// Cancela auto-attack ao se movimentar manualmente se estava atacando
		if (autoAttacking) {
			autoAttacking = false;
			send(new AutoAttackStop(active.objectId()));
			ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, new AutoAttackStop(active.objectId()), false);
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
				// 2º clique: se atacavel, checa alcance e ataca; senao abre dialogo
				double dx = active.x() - npc.x();
				double dy = active.y() - npc.y();
				double distSq = dx * dx + dy * dy;

				if (npc.template().isAttackable()) {
					int attackRange = getPhysicalAttackRange(active);
					double maxDist = attackRange + 50.0;
					if (distSq > maxDist * maxDist) {
						autoAttacking = true;
						var movePawn = new MoveToPawn(active.objectId(), npc.objectId(), attackRange, active.x(), active.y(), active.z());
						send(movePawn);
						ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, movePawn, false);
						schedulePlayerAutoAttack(npc, 500);
						return;
					}
					onAttackNpc(npc);
				} else {
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

	private void onCancelTarget() {
		targetObjectId = 0;
		if (active != null) {
			send(new TargetUnselected(active.objectId(), x(), y(), z()));
			if (autoAttacking) {
				autoAttacking = false;
				send(new AutoAttackStop(active.objectId()));
				ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, new AutoAttackStop(active.objectId()), false);
			}
		}
	}

	private void onAttackNpc(NpcInstance npc) {
		if (npc.isDead()) {
			send(new ActionFailed());
			send(new AutoAttackStop(active.objectId()));
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

		// Flechas para arco: checa e consome antes do disparo
		if (!checkAndConsumeArrow()) {
			return;
		}

		var stats = PlayerStats.calculate(active, t);
		int pAtkSpd = Math.max(100, stats.pAtkSpd());
		int timeAtk = (int) (500_000L / pAtkSpd);
		boolean bow = isBow(activeWeapon());
		int timeToHit = bow ? (int) (timeAtk * 0.70) : (int) (timeAtk * 0.50);
		attackEndTime = now + timeAtk;

		// Ativa postura de combate no cliente se ainda nao estiver
		if (!autoAttacking) {
			autoAttacking = true;
			send(new AutoAttackStart(active.objectId()));
			ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, new AutoAttackStart(active.objectId()), false);
		}

		// Soulshot: recarrega o automatico antes do golpe e gasta a carga neste hit (acertando ou nao)
		if (!soulshotCharged) {
			rechargeAutoSoulShots();
		}
		int ssGrade = soulshotCharged ? chargedGrade : -1;
		soulshotCharged = false;

		var hit = ctx.combat().attackNpc(active, t, npc, ssGrade);

		// 1. Envia a animacao de ataque imediatamente para o cliente iniciar o swing/tiro
		var atk = new Attack(active.objectId(), npc.objectId(), hit.damage(), hit.flags(), active.x(), active.y(),
				active.z());
		send(atk);
		ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, atk, false);

		// 2. Agenda a aplicacao do dano e atualizacoes no momento exato do impacto (timeToHit)
		autoAttackScheduler.schedule(() -> {
			if (!inWorld || active == null || npc == null) {
				return;
			}

			if (hit.damage() > 0) {
				send(SystemMessage.of(SystemMessage.YOU_DID_S1_DMG, new SystemMessage.Number(hit.damage())));
			}

			var su = StatusUpdate.hp(npc.objectId(), hit.remainingHp(), hit.maxHp());
			send(su);
			ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, su, false);

			if (hit.isDead()) {
				// Finaliza postura de combate
				autoAttacking = false;
				send(new AutoAttackStop(active.objectId()));
				ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, new AutoAttackStop(active.objectId()), false);

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
			} else {
				// Monstro entra em combate continuo e persegue o jogador via NpcAiService
				if (ctx.npcAi() != null) {
					ctx.npcAi().startCombat(npc, active.objectId());
				}
				schedulePlayerAutoAttack(npc, Math.max(50, timeAtk - timeToHit));
			}
		}, timeToHit, TimeUnit.MILLISECONDS);
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
			double maxDist = attackRange + 50.0;
			if (distSq > maxDist * maxDist) {
				var movePawn = new MoveToPawn(active.objectId(), npc.objectId(), attackRange, active.x(), active.y(), active.z());
				send(movePawn);
				ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, movePawn, false);
				schedulePlayerAutoAttack(npc, 500);
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
		if (cmd.startsWith("npc_")) {
			// Formato: npc_%objectId%_Chat 1 ou npc_%objectId%_Quest etc
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
							if (action.length() > 5) {
								try {
									val = Integer.parseInt(action.substring(5).trim());
								} catch (NumberFormatException ignored) {
								}
							}
							showNpcHtml(npc, val);
							return;
						} else if (action.startsWith("Quest")) {
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
							try {
								int listId = Integer.parseInt(action.substring(3).trim());
								showBuyList(npc, listId);
								return;
							} catch (NumberFormatException ignored) {
							}
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

		active.moveTo(loc.locX(), loc.locY(), loc.locZ());
		ctx.characters().save(active, true);
		send(new TeleportToLocation(active.objectId(), loc.locX(), loc.locY(), loc.locZ()));
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
							double dx = active.x() - npc.x();
							double dy = active.y() - npc.y();
							double distSq = dx * dx + dy * dy;
							int attackRange = getPhysicalAttackRange(active);
							double maxDist = attackRange + 50.0;
							if (distSq > maxDist * maxDist) {
								autoAttacking = true;
								var movePawn = new MoveToPawn(active.objectId(), npc.objectId(), attackRange, active.x(), active.y(), active.z());
								send(movePawn);
								ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, movePawn, false);
								schedulePlayerAutoAttack(npc, 500);
							} else {
								onAttackNpc(npc);
							}
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
		var d = c.buff();
		var buff = new ActiveBuff(c.skillId(), c.level(), d.stackType(), System.currentTimeMillis() + d.durationMs(),
				d.runSpdAdd(), d.pAtkSpdMul(), d.mAtkSpdMul(), d.accuracyAdd());
		PlayerCharacter owner = active;
		owner.effects().put(buff);
		send(SystemMessage.of(SystemMessage.YOU_FEEL_S1_EFFECT, new SystemMessage.SkillName(c.skillId(), c.level())));
		refreshBuffs();
		autoAttackScheduler.schedule(() -> {
			if (owner.effects().remove(buff) && active == owner && inWorld) {
				send(SystemMessage.of(SystemMessage.S1_HAS_WORN_OFF, new SystemMessage.SkillName(c.skillId(), c.level())));
				refreshBuffs();
			}
		}, d.durationMs(), TimeUnit.MILLISECONDS);
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

	// ---- Soulshots ----

	private void onAutoSoulShot(RequestAutoSoulShot p) {
		if (!inWorld || active.isDead()) {
			return;
		}
		if (!ConsumableTable.isSoulshot(p.itemId())) {
			// spiritshots/beast shots ainda sem sistema de magia: so garante o icone apagado
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
			chargeSoulShot(p.itemId(), false);
		} else {
			autoSoulShots.remove(p.itemId());
			send(new ExAutoSoulShot(p.itemId(), 0));
			send(SystemMessage.of(SystemMessage.AUTO_USE_OF_S1_CANCELLED, name));
		}
	}

	private void rechargeAutoSoulShots() {
		for (int itemId : autoSoulShots) {
			if (chargeSoulShot(itemId, true)) {
				return;
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
		consumableReuse.clear();
		skillReuse.clear();
		var cast = castTask;
		if (cast != null) {
			cast.cancel(false);
		}
		casting = false;
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
		if (!inWorld || active.isDead()) {
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
		if (!inWorld || active == null || active.isDead()) {
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
			send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED, new SystemMessage.SkillName(sk.id(), sk.level())));
			send(new ActionFailed());
			return;
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
		GameSession playerTarget = this;
		if (sk.isOffensive()) {
			if (!sk.isAreaAroundSelf()) {
				npcTarget = ctx.world().npc(targetObjectId).filter(n -> n.isAttackable() && !n.isDead()).orElse(null);
				if (npcTarget == null) {
					send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
					send(new ActionFailed());
					return;
				}
			}
		} else if (!sk.isSelfTargeted() && targetObjectId != 0 && targetObjectId != active.objectId()) {
			var other = ctx.world().player(targetObjectId).orElse(null);
			if (other instanceof GameSession gs && gs.active != null && !gs.active.isDead()) {
				playerTarget = gs;
			}
		}

		// Alcance (o L2J anda ate o alvo e depois conjura)
		int tx = npcTarget != null ? npcTarget.x() : playerTarget.x();
		int ty = npcTarget != null ? npcTarget.y() : playerTarget.y();
		int tz = npcTarget != null ? npcTarget.z() : playerTarget.z();
		if (sk.castRange() > 0 && (npcTarget != null || playerTarget != this)) {
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
		double speedFactor = 333.0 / Math.max(1, sk.magic() ? stats.mAtkSpd() : stats.pAtkSpd());
		int hitTime = sk.hitTime() > 0 ? Math.max(300, (int) (sk.hitTime() * speedFactor)) : 0;
		int reuse = (int) (sk.reuseDelay() * speedFactor);
		active.currentMp(active.currentMp() - sk.mpInitialConsume());
		if (reuse > 0) {
			skillReuse.put(sk.id(), now + Math.max(reuse, hitTime));
		}
		boolean resumeAttack = autoAttacking && npcTarget != null && sk.isOffensive();
		autoAttacking = false; // o auto-ataque para durante o cast

		int mainTargetId = npcTarget != null ? npcTarget.objectId() : playerTarget.objectId();
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
		GameSession friend = playerTarget;
		Runnable finish = () -> {
			casting = false;
			castTask = null;
			if (active != owner || !inWorld || owner.isDead()) {
				return;
			}
			try {
				finishCast(sk, npc, friend, resumeAttack);
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
	private void finishCast(SkillTemplate sk, NpcInstance mainNpc, GameSession friend, boolean resumeAttack) {
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

			boolean ss = false;
			if (sk.isPhysicalDamage()) {
				if (!soulshotCharged) {
					rechargeAutoSoulShots();
				}
				ss = soulshotCharged;
				soulshotCharged = false;
			}
			for (NpcInstance n : targets) {
				applyOffensive(sk, n, t, ss);
			}
			sendVitals();
			if (resumeAttack && mainNpc != null && !mainNpc.isDead() && targetObjectId == mainNpc.objectId()) {
				autoAttacking = true;
				schedulePlayerAutoAttack(mainNpc);
			}
			return;
		}

		GameSession target = friend != null && friend.active != null ? friend : this;
		broadcastLaunched(sk, List.of(target.objectId()));
		target.receivePositiveSkill(sk, active.name());
		sendVitals();
	}

	private void broadcastLaunched(SkillTemplate sk, List<Integer> targets) {
		var launched = new GameServerPacket.MagicSkillLaunched(active.objectId(), sk.id(), sk.level(), targets);
		send(launched);
		ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, launched, false);
	}

	/** Dano + debuffs de um skill ofensivo num monstro. */
	private void applyOffensive(SkillTemplate sk, NpcInstance npc, CharTemplate t, boolean soulshot) {
		var combat = ctx.combat();
		if (combat == null) {
			return;
		}
		CombatService.HitResult hit = null;
		if (sk.isPhysicalDamage()) {
			hit = combat.skillPhysicalNpc(active, t, npc, sk.power(), soulshot, sk.skillType().equals("BLOW"));
		} else if (sk.isMagicDamage() && sk.power() > 0) {
			hit = combat.skillMagicNpc(active, t, npc, sk.power());
			if (sk.skillType().equals("DRAIN") && hit.damage() > 0) {
				double absorb = sk.absorbPart() > 0 ? sk.absorbPart() : 0.2;
				active.currentHp(active.currentHp() + hit.damage() * absorb);
			}
		}

		// Debuffs de controle (Stun/Sleep/Paralyze/Root) nos monstros
		if (!npc.isDead()) {
			boolean damageSkill = hit != null;
			for (var e : sk.effects()) {
				String name = e.name().toLowerCase(java.util.Locale.ROOT);
				boolean control = name.equals("stun") || name.equals("sleep") || name.equals("paralyze")
						|| name.equals("root") || name.equals("petrification");
				if (!control) {
					continue;
				}
				double base = damageSkill ? 50 : sk.power();
				if (!combat.debuffLands(base, sk.magicLevel(), active.level(), npc)) {
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
			}
		}

		if (hit != null) {
			handleNpcHit(npc, hit, t, sk);
		} else if (!npc.isDead() && ctx.npcAi() != null) {
			ctx.npcAi().startCombat(npc, active.objectId()); // debuff puro tambem gera aggro
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
			}
			if (targetObjectId == npc.objectId() && autoAttacking) {
				autoAttacking = false;
				send(new AutoAttackStop(active.objectId()));
				ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, new AutoAttackStop(active.objectId()), false);
			}
			var die = new Die(npc.objectId(), false);
			send(die);
			ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, die, false);
			applyExpAndSp(hit.expReward(), hit.spReward(), t);
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
		if (active == null || active.isDead()) {
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

	private void healHp(double amount) {
		double before = active.currentHp();
		active.currentHp(before + amount);
		send(SystemMessage.of(SystemMessage.S1_HP_RESTORED, new SystemMessage.Number((int) (active.currentHp() - before))));
	}

	/** Aplica os {@code <effect>} do skill no proprio jogador desta sessao (buffs, HoT, toggles). */
	private void applySkillEffects(SkillTemplate sk, boolean toggle) {
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
			if (e.funcs().isEmpty()) {
				continue; // efeito sem stats (Stun, Fear, etc.) ainda nao tem motor no jogador
			}
			String stack = e.stackType() == null || e.stackType().equalsIgnoreCase("none")
					? "skill_" + sk.id() + "_" + name : e.stackType();
			long end = toggle ? com.lopez.l2j.game.effect.PlayerEffects.PERMANENT
					: System.currentTimeMillis() + e.durationMs();
			var buff = ActiveBuff.ofSkill(sk.id(), sk.level(), stack, end, e.funcs());
			owner.effects().put(buff);
			any = true;
			if (!toggle) {
				autoAttackScheduler.schedule(() -> {
					if (owner.effects().remove(buff) && active == owner && inWorld) {
						send(SystemMessage.of(SystemMessage.EFFECT_S1_DISAPPEARED,
								new SystemMessage.SkillName(sk.id(), sk.level())));
						refreshBuffs();
					}
				}, e.durationMs(), TimeUnit.MILLISECONDS);
			}
		}
		if (any) {
			send(SystemMessage.of(SystemMessage.YOU_FEEL_S1_EFFECT, new SystemMessage.SkillName(sk.id(), sk.level())));
			refreshBuffs();
		}
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
	@Override
	public void send(GameServerPacket packet) {
		sink.accept(packet);
	}

	// ---- OnlinePlayer ----

	@Override
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
}
