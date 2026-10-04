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
import com.lopez.l2j.network.game.packet.GameServerPacket.SkillList;
import com.lopez.l2j.network.game.packet.GameServerPacket.SsqInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.StopMove;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import com.lopez.l2j.network.game.packet.GameServerPacket.TargetUnselected;
import com.lopez.l2j.network.game.packet.GameServerPacket.UserInfo;
import com.lopez.l2j.network.session.SessionKey;
import com.lopez.l2j.network.session.SessionKeyRegistry;
import java.util.List;
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
			CharacterService characters, InventoryService inventories, GameWorld world, String serverName) {
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
			case RequestTargetCancel p -> send(new TargetUnselected(active.objectId(), x(), y(), z()));
			case RequestActionUse p -> onActionUse(p);
			case RequestItemList p -> send(ItemList.of(active.inventory().items(), true));
			case UseItem p -> onUseItem(p);
			case RequestUnEquipItem p -> onUnEquip(p);
			case RequestSkillList p -> send(new SkillList());
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
		send(new ShortCutInit());
		send(new HennaInfo());
		send(new QuestList());
		send(new EtcStatusUpdate());
		send(ExStorageMaxCount.defaults());
		send(new FriendList());
		send(new UserInfo(active, t));
		send(new SkillList());
		send(new ClientSetTime(GameTime.now()));
		ctx.world().add(this);
		ctx.characters().save(active, true);
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
		send(new MoveToLocation(active.objectId(), p.targetX(), p.targetY(), p.targetZ(), p.originX(),
				p.originY(), p.originZ()));
	}

	private void onValidatePosition(ValidatePosition p) {
		if (!inWorld || (p.x() == 0 && p.y() == 0)) {
			return;
		}
		active.moveTo(p.x(), p.y(), p.z());
		active.heading(p.heading());
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
		if (inWorld && p.objectId() == active.objectId()) {
			send(new MyTargetSelected(active.objectId(), 0));
		} else {
			send(new ActionFailed());
		}
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
		if (active == null) {
			return;
		}
		if (inWorld) {
			ctx.world().remove(this);
			ctx.characters().save(active, false);
			log.info("{} saiu do mundo", active.name());
		}
		inWorld = false;
		active = null;
	}

	/** Envia um pacote a este cliente; seguro para chamar de outras threads (o sink sincroniza). */
	@Override
	public void send(GameServerPacket packet) {
		sink.accept(packet);
	}

	// ---- OnlinePlayer ----

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
