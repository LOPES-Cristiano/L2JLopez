package com.lopez.l2j.network.game;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.CharacterService;
import com.lopez.l2j.game.service.CharacterService.CreateRequest;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.game.service.InventoryService.EquipResult;
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
import com.lopez.l2j.network.game.packet.GameServerPacket.MyTargetSelected;
import com.lopez.l2j.network.game.packet.GameServerPacket.NewCharacterSuccess;
import com.lopez.l2j.network.game.packet.GameServerPacket.QuestList;
import com.lopez.l2j.network.game.packet.GameServerPacket.RestartResponse;
import com.lopez.l2j.network.game.packet.GameServerPacket.ShortCutInit;
import com.lopez.l2j.network.game.packet.GameServerPacket.ShortCutRegister;
import com.lopez.l2j.network.game.packet.GameServerPacket.SkillList;
import com.lopez.l2j.network.game.packet.GameServerPacket.MagicSkillUse;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestShortCutReg;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestShortCutDel;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestMagicSkillUse;
import com.lopez.l2j.game.shortcut.ShortCut;
import com.lopez.l2j.game.shortcut.ShortCutRepository;
import com.lopez.l2j.game.skill.Skill;
import com.lopez.l2j.game.skill.SkillRepository;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
			SkillRepository skills, String serverName) {
		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, null, null, null, null, null, null, null, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, null, null, null, null, null, null, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists, null, null, null, null, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists, combat, null, null, null, serverName);
		}

		public Context(int protocolMin, int protocolMax, SessionKeyRegistry sessionKeys,
				CharacterService characters, InventoryService inventories, GameWorld world, HtmCache htmls,
				TeleportLocationTable teleports, BuyListTable buylists, CombatService combat,
				com.lopez.l2j.game.drop.DropService drops, String serverName) {
			this(protocolMin, protocolMax, sessionKeys, characters, inventories, world, htmls, teleports, buylists, combat, drops, null, null, serverName);
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
	private final Set<Integer> knownObjects = ConcurrentHashMap.newKeySet();

	public GameSession(Context ctx, byte[] cryptKey, String ip, Consumer<GameServerPacket> sink) {
		this.ctx = ctx;
		this.cryptKey = cryptKey.clone();
		this.ip = ip;
		this.sink = sink;
	}

	public State state() {
		return state;
	}

	public String account() {
		return account;
	}

	public PlayerCharacter activeCharacter() {
		return active;
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
			case RequestSkillList p -> send(new SkillList(ctx.skills() != null ? ctx.skills().findByCharId(active.objectId(), 0) : List.of()));
			case RequestQuestList p -> send(new QuestList());
			case Logout p -> onLogout();
			case RequestRestart p -> onRestart();
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
		send(ItemList.of(active.inventory().items(), false));
		send(new ShortCutInit(ctx.shortcuts() != null ? ctx.shortcuts().findByCharId(active.objectId(), 0) : List.of()));
		send(new HennaInfo());
		send(new QuestList());
		send(new EtcStatusUpdate());
		send(ExStorageMaxCount.defaults());
		send(new FriendList());
		send(new UserInfo(active, t));
		send(new SkillList(ctx.skills() != null ? ctx.skills().findByCharId(active.objectId(), 0) : List.of()));
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
		String text = p.text().length() > MAX_CHAT_LENGTH ? p.text().substring(0, MAX_CHAT_LENGTH) : p.text();
		switch (p.channel()) {
			case CreatureSay.SHOUT -> ctx.world().broadcast(
					new CreatureSay(active.objectId(), CreatureSay.SHOUT, active.name(), text), x -> true);
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

	private void onAction(Action p) {
		if (!inWorld) {
			send(new ActionFailed());
			return;
		}
		if (p.objectId() == active.objectId()) {
			targetObjectId = active.objectId();
			send(new MyTargetSelected(active.objectId(), 0));
			return;
		}
		var npcOpt = ctx.world().npc(p.objectId());
		if (npcOpt.isPresent()) {
			var npc = npcOpt.get();
			if (targetObjectId == npc.objectId()) {
				// 2º clique: se atacavel, ataca; senao abre dialogo
				if (npc.template().isAttackable()) {
					onAttackNpc(npc);
				} else {
					showNpcHtml(npc, 0);
				}
			} else {
				// 1º clique: seleciona NPC como alvo
				targetObjectId = npc.objectId();
				send(new MyTargetSelected(npc.objectId(), 0));
				send(new ValidateLocation(npc.objectId(), npc.x(), npc.y(), npc.z(), npc.heading()));
			}
			return;
		}
		var playerOpt = ctx.world().player(p.objectId());
		if (playerOpt.isPresent()) {
			var other = playerOpt.get();
			targetObjectId = other.objectId();
			send(new MyTargetSelected(other.objectId(), 0));
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
		}
	}

	private void onAttackNpc(NpcInstance npc) {
		if (npc.isDead()) {
			send(new ActionFailed());
			return;
		}
		if (ctx.combat() == null) {
			send(new ActionFailed());
			return;
		}
		var t = ctx.characters().template(active);
		var hit = ctx.combat().attackNpc(active, t, npc);

		var atk = new Attack(active.objectId(), npc.objectId(), hit.damage(), hit.flags(), active.x(), active.y(),
				active.z());
		send(atk);
		ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, atk, false);

		var su = StatusUpdate.hp(npc.objectId(), hit.remainingHp(), hit.maxHp());
		send(su);
		ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, su, false);

		if (hit.isDead()) {
			var die = new Die(npc.objectId(), false);
			send(die);
			ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, die, false);

			active.exp(active.exp() + hit.expReward());
			active.sp(active.sp() + hit.spReward());
			send(new StatusUpdate(active.objectId(),
					List.of(new StatusUpdate.Attribute(StatusUpdate.EXP, (int) active.exp()),
							new StatusUpdate.Attribute(StatusUpdate.SP, active.sp()))));
			send(new UserInfo(active, t));

			if (ctx.drops() != null) {
				var rewarded = ctx.drops().rewardMonsterDeath(active, npc.npcId(), ctx.inventories(), this::send);
				if (!rewarded.isEmpty()) {
					send(ItemList.of(active.inventory().items(), false));
				}
			}

			ctx.characters().save(active, true);
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
		if (!inWorld) {
			return;
		}
		switch (p.actionId()) {
			case 0 -> {
				active.sitting(!active.sitting());
				send(new ChangeWaitType(active.objectId(), active.sitting() ? 0 : 1, x(), y(), z()));
			}
			case 1 -> {
				active.running(!active.running());
				send(new ChangeMoveType(active.objectId(), active.running()));
			}
			default -> send(new ActionFailed());
		}
	}

	/** UseItem: por enquanto so equipaveis; consumiveis chegam com os item handlers. */
	private void onUseItem(UseItem p) {
		if (!inWorld) {
			return;
		}
		var item = active.inventory().byObjectId(p.objectId()).orElse(null);
		if (item == null) {
			send(new ActionFailed());
			return;
		}
		if (!item.template().isEquipable()) {
			send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED, new SystemMessage.ItemName(item.itemId())));
			send(new ActionFailed());
			return;
		}
		if (active.sitting()) {
			send(new ActionFailed());
			return;
		}
		afterEquipChange(ctx.inventories().toggleEquip(active.inventory(), item.objectId()));
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
		if (inWorld) {
			ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, new DeleteObject(active.objectId()), false);
			ctx.world().remove(this);
			ctx.characters().save(active, false);
			knownObjects.clear();
			log.info("{} saiu do mundo", active.name());
		}
		inWorld = false;
		active = null;
	}

	private void onShortCutReg(RequestShortCutReg p) {
		if (!inWorld) {
			send(new ActionFailed());
			return;
		}
		int slot = p.slot() % 12;
		int page = p.slot() / 12;
		var sc = new ShortCut(slot, page, p.type(), p.id(), -1, p.characterType());
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

	private void onMagicSkillUse(RequestMagicSkillUse p) {
		if (!inWorld) {
			send(new ActionFailed());
			return;
		}
		int targetId = targetObjectId != 0 ? targetObjectId : active.objectId();
		int tx = active.x(), ty = active.y(), tz = active.z();

		var targetNpc = ctx.world().npc(targetId).orElse(null);
		if (targetNpc != null) {
			tx = targetNpc.x();
			ty = targetNpc.y();
			tz = targetNpc.z();
		}

		var msu = new MagicSkillUse(active.objectId(), targetId, p.magicId(), 1, 1000, 2000,
				active.x(), active.y(), active.z(), tx, ty, tz);
		send(msu);
		ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, msu, false);

		if (targetNpc != null && targetNpc.isAttackable() && !targetNpc.isDead() && ctx.combat() != null) {
			var t = ctx.characters().template(active);
			var hit = ctx.combat().attackNpc(active, t, targetNpc);
			var su = StatusUpdate.hp(targetNpc.objectId(), hit.remainingHp(), hit.maxHp());
			send(su);
			ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, su, false);

			if (hit.isDead()) {
				var die = new Die(targetNpc.objectId(), false);
				send(die);
				ctx.world().broadcastAround(this, GameWorld.VISIBILITY_RADIUS, die, false);

				active.exp(active.exp() + hit.expReward());
				active.sp(active.sp() + hit.spReward());
				send(new StatusUpdate(active.objectId(),
						List.of(new StatusUpdate.Attribute(StatusUpdate.EXP, (int) active.exp()),
								new StatusUpdate.Attribute(StatusUpdate.SP, active.sp()))));
				send(new UserInfo(active, t));

				if (ctx.drops() != null) {
					var rewarded = ctx.drops().rewardMonsterDeath(active, targetNpc.npcId(), ctx.inventories(), this::send);
					if (!rewarded.isEmpty()) {
						send(ItemList.of(active.inventory().items(), false));
					}
				}
				ctx.characters().save(active, true);
			}
		}
	}

	/** Envia um pacote a este cliente; seguro para chamar de outras threads (o sink sincroniza). */
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
}
