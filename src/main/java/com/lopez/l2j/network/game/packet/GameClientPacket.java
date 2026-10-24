package com.lopez.l2j.network.game.packet;

import com.lopez.l2j.network.login.packet.PacketReader;
import java.util.List;
import java.util.Optional;

/**
 * Pacotes do cliente Interlude para o game server, decodificados por estado (porta do L2GamePacketHandler).
 * Opcodes desconhecidos/fora de estado viram {@link Unknown} (o legado apenas os ignora).
 */
public sealed interface GameClientPacket {

	enum State {
		CONNECTED, AUTHED, IN_GAME
	}

	record ProtocolVersion(int version) implements GameClientPacket {
	}

	record AuthLogin(String account, int playOk2, int playOk1, int loginOk1, int loginOk2) implements GameClientPacket {
	}

	record NewCharacter() implements GameClientPacket {
	}

	record CharacterCreate(String name, int race, int sex, int classId, int intel, int str, int con, int men,
			int dex, int wit, int hairStyle, int hairColor, int face) implements GameClientPacket {
	}

	record CharacterDelete(int slot) implements GameClientPacket {
	}

	record CharacterRestore(int slot) implements GameClientPacket {
	}

	record CharacterSelect(int slot) implements GameClientPacket {
	}

	record RequestManorList() implements GameClientPacket {
	}

	record EnterWorld() implements GameClientPacket {
	}

	record MoveBackwardToLocation(int targetX, int targetY, int targetZ, int originX, int originY, int originZ,
			int moveMovement) implements GameClientPacket {
		public MoveBackwardToLocation(int targetX, int targetY, int targetZ, int originX, int originY, int originZ) {
			this(targetX, targetY, targetZ, originX, originY, originZ, 1);
		}
	}

	record AttackRequest(int objectId, int originX, int originY, int originZ, int attackId)
			implements GameClientPacket {
	}

	record ValidatePosition(int x, int y, int z, int heading) implements GameClientPacket {
	}

	record Say2(String text, int channel, String target) implements GameClientPacket {
	}

	record Action(int objectId, int originX, int originY, int originZ, int shift) implements GameClientPacket {
	}

	record RequestTargetCancel() implements GameClientPacket {
	}

	record RequestActionUse(int actionId, boolean ctrl, boolean shift) implements GameClientPacket {
	}

	record RequestSocialAction(int actionId) implements GameClientPacket {
	}

	record RequestWithDrawalParty() implements GameClientPacket {
	}

	record RequestOustPartyMember(String name) implements GameClientPacket {
	}

	record RequestShowMiniMap() implements GameClientPacket {
	}

	record RequestRestartPoint(int requestedPointType) implements GameClientPacket {
	}

	record RequestUserCommand(int commandId) implements GameClientPacket {
	}

	record RequestItemList() implements GameClientPacket {
	}

	record UseItem(int objectId) implements GameClientPacket {
	}

	record RequestDropItem(int objectId, int count, int x, int y, int z) implements GameClientPacket {
	}

	/** {@code bodyPart} = mascara L2Item.SLOT_* do slot clicado. */
	record RequestUnEquipItem(int bodyPart) implements GameClientPacket {
	}

	record RequestSkillList() implements GameClientPacket {
	}

	record RequestQuestList() implements GameClientPacket {
	}

	record Logout() implements GameClientPacket {
	}

	record RequestRestart() implements GameClientPacket {
	}

	record RequestBypassToServer(String command) implements GameClientPacket {
	}

	record RequestBBSwrite(String url, String arg1, String arg2, String arg3, String arg4, String arg5) implements GameClientPacket {
	}

	/** 0x57 - requisicao para abrir a janela da Comunidade BBS (Alt + B). */
	record RequestShowBoard(int boardId) implements GameClientPacket {
	}

	/** 0xac - requisicao para abrir o livro de receitas (Dwarven ou Common). */
	record RequestRecipeBookOpen(boolean isDwarvenCraft) implements GameClientPacket {
	}

	/** 0x5b - comando digitado na caixa de chat com prefixo // (ex.: //admin). */
	record SendBypassBuildCmd(String command) implements GameClientPacket {
	}

	record ItemRequest(int itemId, int count) {
	}

	record RequestBuyItem(int listId, java.util.List<ItemRequest> items) implements GameClientPacket {
		public RequestBuyItem {
			items = java.util.List.copyOf(items);
		}
	}

	record SellItemRequest(int objectId, int itemId, int count) {
	}

	record RequestSellItem(int listId, java.util.List<SellItemRequest> items) implements GameClientPacket {
		public RequestSellItem {
			items = java.util.List.copyOf(items);
		}
	}

	record RequestShortCutReg(int type, int slot, int id, int characterType) implements GameClientPacket {
	}

	record RequestShortCutDel(int id) implements GameClientPacket {
	}

	record RequestMakeMacro(com.lopez.l2j.game.macro.Macro macro) implements GameClientPacket {
	}

	record RequestDeleteMacro(int id) implements GameClientPacket {
	}

	record RequestMagicSkillUse(int magicId, boolean ctrlPressed, boolean shiftPressed) implements GameClientPacket {
	}

	/** 0xD0:0x05 - liga (type 1) ou desliga (type 0) o uso automatico de um soulshot/spiritshot. */
	record RequestAutoSoulShot(int itemId, int type) implements GameClientPacket {
	}

	/** 0x6b - detalhes (custo/livros) de um skill da janela do treinador. */
	record RequestAcquireSkillInfo(int skillId, int level, int skillType) implements GameClientPacket {
	}

	/** 0x6c - aprender o skill selecionado na janela do treinador. */
	record RequestAcquireSkill(int skillId, int level, int skillType) implements GameClientPacket {
	}

	/** 0xD0:0x06 - detalhes de encantamento do skill. */
	record RequestExEnchantSkillInfo(int skillId, int level) implements GameClientPacket {
	}

	/** 0xD0:0x07 - executa o encantamento do skill. */
	record RequestExEnchantSkill(int skillId, int level) implements GameClientPacket {
	}

	/** 0x29 - convite para grupo (party). */
	record RequestJoinParty(String name, int itemDistribution) implements GameClientPacket {
	}

	/** 0x2a - resposta ao convite de grupo (1 = aceitar, 0 = recusar). */
	record RequestAnswerJoinParty(int response) implements GameClientPacket {
	}

	/** 0xa7 - selecao e compra de item em janela MultiSell. */
	record MultiSellChoose(int listId, int entryId, int amount) implements GameClientPacket {
	}

	/** 0x58 - aplicacao de enchant scroll no item selecionado. */
	record RequestEnchantItem(int objectId) implements GameClientPacket {
	}

	record WareHouseItemRequest(int objectId, int count) {
	}

	/** 0x31 - lista de itens a depositar no armazem. */
	record SendWareHouseDepositList(List<WareHouseItemRequest> items) implements GameClientPacket {
	}

	/** 0x32 - lista de itens a retirar do armazem. */
	record SendWareHouseWithDrawList(List<WareHouseItemRequest> items) implements GameClientPacket {
	}

	/** 0x59 - destruicao de item arrastado para a lixeira. */
	record RequestDestroyItem(int objectId, int count) implements GameClientPacket {
	}

	/** 0x6e - comando de GM (Alt+G, etc). */
	record RequestGMCommand(String targetName, int command) implements GameClientPacket {
	}

	/** 0x68 - solicitacao de brasao de cla por crestId. */
	record RequestPledgeCrest(int crestId) implements GameClientPacket {
	}

	/** 0x66 - solicitacao de informacoes do cla. */
	record RequestPledgeInfo(int clanId) implements GameClientPacket {
	}

	/** 0x3c - solicitacao de lista de membros do cla. */
	record RequestPledgeMemberList() implements GameClientPacket {
	}

	/** 0x52 - resposta a pedido de rendicao de guerra de cla. */
	record RequestReplySurrenderPledgeWar(String reqName, int answer) implements GameClientPacket {
	}

	/** 0x53 - upload de novo brasao de cla. */
	record RequestSetPledgeCrest(byte[] data) implements GameClientPacket {
	}

	/** 0x24 - convidar jogador para o cla. */
	record RequestJoinPledge(int targetId, int pledgeType) implements GameClientPacket {
	}

	/** 0x25 - responder convite de cla (1=aceitar, 0=recusar). */
	record RequestAnswerJoinPledge(int answer) implements GameClientPacket {
	}

	/** 0x26 - sair do cla. */
	record RequestWithdrawalPledge() implements GameClientPacket {
	}

	/** 0x27 - expulsar membro do cla. */
	record RequestOustPledgeMember(String target) implements GameClientPacket {
	}

	/** 0x55 - definir titulo (nickname) de membro do cla. */
	record RequestGiveNickName(String target, String title) implements GameClientPacket {
	}

	/** 0x64 - abortar quest na janela de quests (Alt+U). */
	record RequestQuestAbort(int questId) implements GameClientPacket {
	}

	/** 0x72 - cristalizar item (Dwarven Crystallize). */
	record RequestCrystallizeItem(int objectId, int count) implements GameClientPacket {
	}

	/** 0x9d - solicitar tempo de recarga das skills (SkillCoolTime). */
	record RequestSkillCoolTime() implements GameClientPacket {
	}

	/** 0xc5 - resposta a janela ConfirmDlg (ressurreicao, summon friend, casamento, porta). */
	record DlgAnswer(int messageId, int answer, int requesterId) implements GameClientPacket {
	}

	/** 0xd0:0x04 - transferir lideranca da party. */
	record RequestChangePartyLeader(String name) implements GameClientPacket {
	}

	/** 0xd0:0x10 - solicitar brasao grande (large crest) do cla. */
	record RequestExPledgeCrestLarge(int crestId) implements GameClientPacket {
	}

	/** 0xd0:0x11 - enviar brasao grande (large crest) do cla. */
	record RequestExSetPledgeCrestLarge(byte[] data) implements GameClientPacket {
	}

	/** 0xd0:0x1a - solicitar lista de ranks de privilegio do cla. */
	record RequestPledgePowerGradeList() implements GameClientPacket {
	}

	/** 0xd0:0x1b - solicitar privilegios de um membro especifico do cla. */
	record RequestPledgeMemberPowerInfo(int unk1, String player) implements GameClientPacket {
	}

	/** 0xd0:0x1c - definir rank de privilegio de um membro do cla. */
	record RequestPledgeSetMemberPowerGrade(String member, int powerGrade) implements GameClientPacket {
	}

	/** 0xd0:0x1d - solicitar informacoes de um membro do cla. */
	record RequestPledgeMemberInfo(int unk1, String player) implements GameClientPacket {
	}

	/** 0xd0:0x1e - solicitar lista de guerras do cla na janela de cla. */
	record RequestPledgeWarList(int unk1, int tab) implements GameClientPacket {
	}

	/** 0xd0:0x27 - solicitar duelo 1v1 ou em party. */
	record RequestDuelStart(String player, int partyDuel) implements GameClientPacket {
	}

	/** 0xd0:0x28 - responder pedido de duelo. */
	record RequestDuelAnswerStart(int partyDuel, int unk1, int response) implements GameClientPacket {
	}

	/** 0xd0:0x30 - render-se em duelo. */
	record RequestDuelSurrender() implements GameClientPacket {
	}

	/** 0xba - solicitacao de lista de tatuagens disponiveis no Symbol Maker. */
	record RequestHennaList() implements GameClientPacket {
	}

	/** 0xbb - solicitacao de detalhes de uma tatuagem para gravacao. */
	record RequestHennaItemInfo(int symbolId) implements GameClientPacket {
	}

	/** 0xbc - confirmacao de gravacao de tatuagem. */
	record RequestHennaEquip(int symbolId) implements GameClientPacket {
	}

	/** 0xbd - solicitacao da lista de tatuagens atuais para remocao. */
	record RequestHennaUnequipList(int symbolId) implements GameClientPacket {
	}

	/** 0xbe - solicitacao de detalhes de remocao de uma tatuagem. */
	record RequestHennaUnequipInfo(int symbolId) implements GameClientPacket {
	}

	/** 0xbf - confirmacao de remocao de tatuagem. */
	record RequestHennaUnequip(int symbolId) implements GameClientPacket {
	}

	record RequestHennaRemove(int symbolId) implements GameClientPacket {
	}

	/** 0xd0:0x18 - solicitacao de historico/ranking de Raid Bosses no mapa mundi (Alt+M). */
	record RequestGetBossRecord(int bossId) implements GameClientPacket {
	}

	/** 0xd0:0x45 - solicitacao da lista de armas amaldicoadas ativas. */
	record RequestCursedWeaponList() implements GameClientPacket {
	}

	/** 0xd0:0x46 - solicitacao da localizacao no mapa das armas amaldicoadas. */
	record RequestCursedWeaponLocation() implements GameClientPacket {
	}

	/** 0xd0:0x29 - confirmacao de item para augmentacao. */
	record RequestConfirmTargetItem(int itemObjId) implements GameClientPacket {
	}

	/** 0xd0:0x2a - confirmacao de Life Stone para augmentacao. */
	record RequestConfirmRefinerItem(int targetItemObjId, int refinerItemObjId) implements GameClientPacket {
	}

	/** 0xd0:0x2b - confirmacao de Gemstones para augmentacao. */
	record RequestConfirmGemStone(int targetItemObjId, int refinerItemObjId, int gemstoneItemObjId, int gemstoneCount) implements GameClientPacket {
	}

	/** 0xd0:0x2c - execucao da augmentacao de arma. */
	record RequestRefine(int targetItemObjId, int refinerItemObjId, int gemstoneItemObjId, int gemstoneCount) implements GameClientPacket {
	}

	/** 0xd0:0x2d - confirmacao de item para remocao de augmentacao. */
	record RequestConfirmCancelItem(int itemObjId) implements GameClientPacket {
	}

	/** 0xd0:0x2e - execucao do cancelamento de augmentacao de arma. */
	record RequestRefineCancel(int itemObjId) implements GameClientPacket {
	}

	/** 0xc7 - solicitacao da pagina de status do Seven Signs. */
	record RequestSSQStatus(int page) implements GameClientPacket {
	}

	/** 0x30 - notificacao do cliente que concluiu o carregamento do mapa/teleporte e esta pronto para aparecer no mundo. */
	record RequestAppearing() implements GameClientPacket {
	}

	/** 0xb9 - recomendacao de jogador (evalscore). */
	record RequestEvaluate(int targetId) implements GameClientPacket {
	}

	/** 0x5e - convidar jogador para amigos. */
	record RequestFriendInvite(String name) implements GameClientPacket {
	}

	/** 0x5f - responder a convite de amigos (1=aceitar, 0=recusar). */
	record RequestAnswerFriendInvite(int response) implements GameClientPacket {
	}

	/** 0x60 - abrir lista de amigos. */
	record RequestFriendList() implements GameClientPacket {
	}

	/** 0x61 - remover amigo. */
	record RequestFriendDel(String name) implements GameClientPacket {
	}

	/** 0xa0 - bloquear/desbloquear jogador ou listar bloqueios. */
	record RequestBlock(int type, String name) implements GameClientPacket {
		public static final int BLOCK = 0;
		public static final int UNBLOCK = 1;
		public static final int BLOCKLIST = 2;
		public static final int ALLBLOCK = 3;
		public static final int ALLUNBLOCK = 4;
	}

	record StoreItemRequest(int objectId, int count, int price) {}

	/** 0x73 - abrir configuracao de venda da loja pessoal. */
	record RequestPrivateStoreManageSell() implements GameClientPacket {}

	/** 0x74 - definir lista de itens a venda na loja pessoal. */
	record SetPrivateStoreListSell(boolean packageSale, java.util.List<StoreItemRequest> items) implements GameClientPacket {}

	/** 0x76 - fechar loja pessoal de venda. */
	record RequestPrivateStoreQuitSell() implements GameClientPacket {}

	/** 0x77 - definir mensagem da loja pessoal de venda. */
	record SetPrivateStoreMsgSell(String storeMsg) implements GameClientPacket {}

	/** 0x79 - comprar itens/buffs de uma loja pessoal. */
	record RequestPrivateStoreBuy(int sellerId, java.util.List<StoreItemRequest> items) implements GameClientPacket {}

	/** 0x7b - link clicado na janela HTML de tutorial. */
	record RequestTutorialLinkHtml(String link) implements GameClientPacket {}

	/** 0x7c - comando/bypass passado pelo tutorial ao servidor. */
	record RequestTutorialPassCmdToServer(String bypass) implements GameClientPacket {}

	/** 0x7d - clique no ponto de interrogacao do tutorial. */
	record RequestTutorialQuestionMark(int number) implements GameClientPacket {}

	/** 0x7e - evento disparado pelo cliente para o tutorial. */
	record RequestTutorialClientEvent(int eventId) implements GameClientPacket {}

	/** 0x15 - solicitar troca direta com outro jogador. */
	record TradeRequest(int objectId) implements GameClientPacket {}

	/** 0x16 - adicionar item na janela de troca. */
	record AddTradeItem(int tradeId, int objectId, int count) implements GameClientPacket {}

	/** 0x17 - confirmar (1) ou cancelar (0) a troca. */
	record TradeDone(int response) implements GameClientPacket {}

	/** 0x44 - responder ao pedido de troca (1 = aceitar, 0 = recusar). */
	record AnswerTradeRequest(int response) implements GameClientPacket {}

	record BuyStoreItemRequest(int itemId, int enchant, int count, int price) {}
	record SellToBuyStoreRequest(int objectId, int itemId, int enchant, int count, int price) {}

	/** 0x90 - abrir configuracao de loja pessoal de compra. */
	record RequestPrivateStoreManageBuy() implements GameClientPacket {}

	/** 0x91 - definir lista de itens a comprar na loja pessoal. */
	record SetPrivateStoreListBuy(java.util.List<BuyStoreItemRequest> items) implements GameClientPacket {}

	/** 0x93 - fechar loja pessoal de compra. */
	record RequestPrivateStoreQuitBuy() implements GameClientPacket {}

	/** 0x94 - definir mensagem da loja pessoal de compra. */
	record SetPrivateStoreMsgBuy(String storeMsg) implements GameClientPacket {}

	/** 0x96 - vender itens para a loja pessoal de compra de outro jogador. */
	record RequestPrivateStoreSell(int buyerId, java.util.List<SellToBuyStoreRequest> items) implements GameClientPacket {}

	/** 0xae - abrir janela de criacao de item (recipe info). */
	record RequestRecipeItemMakeInfo(int id) implements GameClientPacket {}

	/** 0xaf - executar criacao de item (craftar). */
	record RequestRecipeItemMakeSelf(int id) implements GameClientPacket {}

	/** 0x4d - declarar guerra de cla. */
	record RequestStartPledgeWar(String pledgeName) implements GameClientPacket {}

	/** 0x4f - encerrar guerra de cla. */
	record RequestStopPledgeWar(String pledgeName) implements GameClientPacket {}

	/** 0x82 - convidar cla para alianca. */
	record RequestJoinAlly(int targetObjId) implements GameClientPacket {}

	/** 0x83 - responder convite de alianca (1 = aceitar, 0 = recusar). */
	record RequestAnswerJoinAlly(int response) implements GameClientPacket {}

	/** 0xa2 - solicitar lista de atacantes de Siege. */
	record RequestSiegeAttackerList(int castleId) implements GameClientPacket {}

	/** 0xa3 - solicitar lista de defensores de Siege. */
	record RequestSiegeDefenderList(int castleId) implements GameClientPacket {}

	/** 0xa4 - inscrever ou remover cla de uma Siege. */
	record RequestJoinSiege(int castleId, int isAttacker, int isJoining) implements GameClientPacket {}

	/** 0xa5 - aprovar defensor na lista de espera da Siege. */
	record RequestConfirmSiegeWaitingList(int castleId, int clanId, int approved) implements GameClientPacket {}

	/** 0xc0 - consultar ou definir privilegios de membro/rank do cla. */
	record RequestPledgePower(int rank, int action, int privs) implements GameClientPacket {}

	record Unknown(int opcode, int subOpcode) implements GameClientPacket {
	}

	/** Pacote com tamanho/campos invalidos devolve vazio (a sessao deve fechar). */
	static Optional<GameClientPacket> decode(State state, byte[] body) {
		try {
			PacketReader r = new PacketReader(body);
			int op = r.readC();
			return Optional.of(switch (state) {
				case CONNECTED -> switch (op) {
					case 0x00 -> new ProtocolVersion(r.readD());
					case 0x08 -> new AuthLogin(r.readS().toLowerCase(java.util.Locale.ROOT), r.readD(), r.readD(),
							r.readD(), r.readD());
					default -> new Unknown(op, -1);
				};
				case AUTHED -> switch (op) {
					case 0x0e -> new NewCharacter();
					case 0x0b -> new CharacterCreate(r.readS(), r.readD(), r.readD(), r.readD(), r.readD(),
							r.readD(), r.readD(), r.readD(), r.readD(), r.readD(), r.readD(), r.readD(), r.readD());
					case 0x0c -> new CharacterDelete(r.readD());
					case 0x62 -> new CharacterRestore(r.readD());
					case 0x0d -> new CharacterSelect(r.readD());
					case 0x09 -> new Logout();
					case 0x68 -> new RequestPledgeCrest(r.readD());
					case 0xd0 -> extended(r);
					default -> new Unknown(op, -1);
				};
				case IN_GAME -> switch (op) {
					case 0x03 -> new EnterWorld();
					case 0x01 -> {
						int tx = r.readD(), ty = r.readD(), tz = r.readD();
						int ox = r.readD(), oy = r.readD(), oz = r.readD();
						int mm = r.remaining() >= 4 ? r.readD() : 1;
						yield new MoveBackwardToLocation(tx, ty, tz, ox, oy, oz, mm);
					}
					case 0x0a -> new AttackRequest(r.readD(), r.readD(), r.readD(), r.readD(), r.readC());
					case 0x48 -> new ValidatePosition(r.readD(), r.readD(), r.readD(), r.readD());
					case 0x38 -> say2(r);
					case 0x04 -> new Action(r.readD(), r.readD(), r.readD(), r.readD(), r.readC());
					case 0x21 -> new RequestBypassToServer(r.readS());
					case 0x22 -> readBBSwrite(r);
					case 0x15 -> r.remaining() >= 4 ? new TradeRequest(r.readD()) : new Unknown(op, -1);
					case 0x16 -> r.remaining() >= 12 ? new AddTradeItem(r.readD(), r.readD(), r.readD()) : new Unknown(op, -1);
					case 0x17 -> r.remaining() >= 4 ? new TradeDone(r.readD()) : new Unknown(op, -1);
					case 0x1b -> new RequestSocialAction(r.readD());
					case 0x1e -> readSellItem(r);
					case 0x1f -> readBuyItem(r);
					case 0x24 -> r.remaining() >= 4 ? new RequestJoinPledge(r.readD(), r.remaining() >= 4 ? r.readD() : 0) : new Unknown(op, -1);
					case 0x25 -> r.remaining() >= 4 ? new RequestAnswerJoinPledge(r.readD()) : new Unknown(op, -1);
					case 0x26 -> new RequestWithdrawalPledge();
					case 0x27 -> r.remaining() >= 2 ? new RequestOustPledgeMember(r.readS()) : new Unknown(op, -1);
					case 0x29 -> new RequestJoinParty(r.readS(), r.readD());
					case 0x2a -> new RequestAnswerJoinParty(r.readD());
					case 0x2b -> new RequestWithDrawalParty();
					case 0x2c -> new RequestOustPartyMember(r.readS());
					case 0x2f -> new RequestMagicSkillUse(r.readD(), r.readD() != 0, r.readC() != 0);
					case 0x30 -> new RequestAppearing();
					case 0x33 -> new RequestShortCutReg(r.readD(), r.readD(), r.readD(), r.readD());
					case 0x35 -> new RequestShortCutDel(r.readD());
					case 0x37 -> new RequestTargetCancel();
					case 0x3c -> new RequestPledgeMemberList();
					case 0x44 -> r.remaining() >= 4 ? new AnswerTradeRequest(r.readD()) : new Unknown(op, -1);
					case 0x45 -> new RequestActionUse(r.readD(), r.readD() == 1, r.readC() == 1);
					case 0x4d -> r.remaining() >= 2 ? new RequestStartPledgeWar(r.readS()) : new Unknown(op, -1);
					case 0x4f -> r.remaining() >= 2 ? new RequestStopPledgeWar(r.readS()) : new Unknown(op, -1);
					case 0x0f -> new RequestItemList();
					case 0x14 -> new UseItem(r.readD());
					case 0x11 -> new RequestUnEquipItem(r.readD());
					case 0x12 -> new RequestDropItem(r.readD(), r.readD(), r.readD(), r.readD(), r.readD());
					case 0x31 -> readWareHouseList(r, true);
					case 0x32 -> readWareHouseList(r, false);
					case 0x3f -> new RequestSkillList();
					case 0x52 -> {
						if (r.remaining() >= 6) {
							var peekReader = new PacketReader(body);
							peekReader.readC();
							int peekLen = peekReader.readD();
							if (peekLen > 0 && peekLen <= 2176 && r.remaining() == 4 + peekLen) {
								yield readSetPledgeCrest(r);
							}
							yield new RequestReplySurrenderPledgeWar(r.readS(), r.remaining() >= 4 ? r.readD() : 0);
						}
						yield new RequestSetPledgeCrest(new byte[0]);
					}
					case 0x53 -> r.remaining() >= 4 ? readSetPledgeCrest(r) : new RequestPledgeMemberList();
					case 0x55 -> r.remaining() >= 4 ? new RequestGiveNickName(r.readS(), r.remaining() >= 2 ? r.readS() : "") : new Unknown(op, -1);
					case 0x57 -> new RequestShowBoard(r.remaining() >= 4 ? r.readD() : 101);
					case 0x58 -> r.remaining() >= 4 ? new RequestEnchantItem(r.readD()) : new Unknown(op, -1);
					case 0x59 -> r.remaining() >= 8 ? new RequestDestroyItem(r.readD(), r.readD()) : new Unknown(op, -1);
					case 0x5b -> new SendBypassBuildCmd(r.readS());
					case 0x64 -> r.remaining() >= 4 ? new RequestQuestAbort(r.readD()) : new Unknown(op, -1);
					case 0x66 -> new RequestPledgeInfo(r.readD());
					case 0x68 -> new RequestPledgeCrest(r.readD());
					case 0x6b -> new RequestAcquireSkillInfo(r.readD(), r.readD(), r.readD());
					case 0x6c -> new RequestAcquireSkill(r.readD(), r.readD(), r.readD());
					case 0x6d -> new RequestRestartPoint(r.readD());
					case 0x6e -> {
						String target = r.readS();
						int cmd = r.remaining() >= 4 ? r.readD() : 1;
						yield new RequestGMCommand(target, cmd);
					}
					case 0x72 -> r.remaining() >= 8 ? new RequestCrystallizeItem(r.readD(), r.readD()) : new Unknown(op, -1);
					case 0x73 -> new RequestPrivateStoreManageSell();
					case 0x74 -> readSetPrivateStoreListSell(r);
					case 0x76 -> new RequestPrivateStoreQuitSell();
					case 0x77 -> new SetPrivateStoreMsgSell(r.readS());
					case 0x79 -> readRequestPrivateStoreBuy(r);
					case 0x7b -> new RequestTutorialLinkHtml(r.readS());
					case 0x7c -> new RequestTutorialPassCmdToServer(r.readS());
					case 0x7d -> new RequestTutorialQuestionMark(r.readD());
					case 0x7e -> new RequestTutorialClientEvent(r.readD());
					case 0x82 -> r.remaining() >= 4 ? new RequestJoinAlly(r.readD()) : new Unknown(op, -1);
					case 0x83 -> r.remaining() >= 4 ? new RequestAnswerJoinAlly(r.readD()) : new Unknown(op, -1);
					case 0x90 -> new RequestPrivateStoreManageBuy();
					case 0x91 -> readSetPrivateStoreListBuy(r);
					case 0x93 -> new RequestPrivateStoreQuitBuy();
					case 0x94 -> new SetPrivateStoreMsgBuy(r.remaining() >= 2 ? r.readS() : "");
					case 0x96 -> readRequestPrivateStoreSell(r);
					case 0x9d -> new RequestSkillCoolTime();
					case 0x5e -> new RequestFriendInvite(r.readS());
					case 0x5f -> new RequestAnswerFriendInvite(r.readD());
					case 0x60 -> new RequestFriendList();
					case 0x61 -> new RequestFriendDel(r.readS());
					case 0x63 -> new RequestQuestList();
					case 0x09 -> new Logout();
					case 0x46 -> new RequestRestart();
					case 0xaa -> new RequestUserCommand(r.readD());
					case 0xac -> new RequestRecipeBookOpen(r.remaining() >= 4 ? r.readD() == 0 : true);
					case 0xae -> r.remaining() >= 4 ? new RequestRecipeItemMakeInfo(r.readD()) : new Unknown(op, -1);
					case 0xaf -> r.remaining() >= 4 ? new RequestRecipeItemMakeSelf(r.readD()) : new Unknown(op, -1);
					case 0xa0 -> readBlock(r);
					case 0xa2 -> r.remaining() >= 4 ? new RequestSiegeAttackerList(r.readD()) : new Unknown(op, -1);
					case 0xa3 -> r.remaining() >= 4 ? new RequestSiegeDefenderList(r.readD()) : new Unknown(op, -1);
					case 0xa4 -> r.remaining() >= 12 ? new RequestJoinSiege(r.readD(), r.readD(), r.readD()) : new Unknown(op, -1);
					case 0xa5 -> r.remaining() >= 12 ? new RequestConfirmSiegeWaitingList(r.readD(), r.readD(), r.readD()) : new Unknown(op, -1);
					case 0xa7 -> readMultiSellChoose(r);
					case 0xb9 -> new RequestEvaluate(r.readD());
					case 0xba -> new RequestHennaList();
					case 0xbb -> new RequestHennaItemInfo(r.readD());
					case 0xbc -> new RequestHennaEquip(r.readD());
					case 0xbd -> new RequestHennaUnequipList(r.remaining() >= 4 ? r.readD() : 0);
					case 0xbe -> new RequestHennaUnequipInfo(r.readD());
					case 0xbf -> new RequestHennaUnequip(r.readD());
					case 0xc0 -> {
						int rank = r.remaining() >= 4 ? r.readD() : 0;
						int action = r.remaining() >= 4 ? r.readD() : 0;
						int privs = (action == 2 && r.remaining() >= 4) ? r.readD() : 0;
						yield new RequestPledgePower(rank, action, privs);
					}
					case 0xc1 -> readMakeMacro(r);
					case 0xc2 -> new RequestDeleteMacro(r.readD());
					case 0xc5 -> r.remaining() >= 8 ? new DlgAnswer(r.readD(), r.readD(), r.remaining() >= 4 ? r.readD() : 0) : new Unknown(op, -1);
					case 0xc7 -> r.remaining() >= 1 ? new RequestSSQStatus(r.readC()) : new Unknown(op, -1);
					case 0xcd -> new RequestShowMiniMap();
					case 0xd0 -> extended(r);
					default -> new Unknown(op, -1);
				};
			});
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	private static SetPrivateStoreListBuy readSetPrivateStoreListBuy(PacketReader r) {
		if (r.remaining() < 4) {
			return new SetPrivateStoreListBuy(java.util.List.of());
		}
		int count = r.readD();
		if (count <= 0 || count > 100 || r.remaining() < count * 12) {
			return new SetPrivateStoreListBuy(java.util.List.of());
		}
		java.util.List<BuyStoreItemRequest> list = new java.util.ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			int itemId = r.readD();
			int enchant = r.readH();
			r.readH(); // pad
			int cnt = r.readD();
			int price = r.readD();
			list.add(new BuyStoreItemRequest(itemId, enchant, cnt, price));
		}
		return new SetPrivateStoreListBuy(list);
	}

	private static RequestPrivateStoreSell readRequestPrivateStoreSell(PacketReader r) {
		if (r.remaining() < 8) {
			return new RequestPrivateStoreSell(0, java.util.List.of());
		}
		int buyerId = r.readD();
		int count = r.readD();
		if (count <= 0 || count > 100 || r.remaining() < count * 20) {
			return new RequestPrivateStoreSell(buyerId, java.util.List.of());
		}
		java.util.List<SellToBuyStoreRequest> list = new java.util.ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			int objId = r.readD();
			int itemId = r.readD();
			int enchant = r.readH();
			r.readH();
			int cnt = r.readD();
			int price = r.readD();
			list.add(new SellToBuyStoreRequest(objId, itemId, enchant, cnt, price));
		}
		return new RequestPrivateStoreSell(buyerId, list);
	}

	private static RequestBlock readBlock(PacketReader r) {
		int type = r.readD();
		String name = (type == RequestBlock.BLOCK || type == RequestBlock.UNBLOCK) && r.remaining() > 0 ? r.readS() : "";
		return new RequestBlock(type, name);
	}

	private static SetPrivateStoreListSell readSetPrivateStoreListSell(PacketReader r) {
		boolean packageSale = r.readD() == 1;
		int count = r.readD();
		if (count <= 0 || count > 100 || r.remaining() < count * 12) {
			return new SetPrivateStoreListSell(packageSale, java.util.List.of());
		}
		java.util.List<StoreItemRequest> list = new java.util.ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			list.add(new StoreItemRequest(r.readD(), r.readD(), r.readD()));
		}
		return new SetPrivateStoreListSell(packageSale, list);
	}

	private static RequestPrivateStoreBuy readRequestPrivateStoreBuy(PacketReader r) {
		int sellerId = r.readD();
		int count = r.readD();
		if (count <= 0 || count > 100 || r.remaining() < count * 12) {
			return new RequestPrivateStoreBuy(sellerId, java.util.List.of());
		}
		java.util.List<StoreItemRequest> list = new java.util.ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			list.add(new StoreItemRequest(r.readD(), r.readD(), r.readD()));
		}
		return new RequestPrivateStoreBuy(sellerId, list);
	}

	private static MultiSellChoose readMultiSellChoose(PacketReader r) {
		if (r.remaining() < 12) {
			return new MultiSellChoose(0, 0, 0);
		}
		int listId = r.readD();
		int entryId = r.readD();
		int amount = r.readD();
		int realEntryId = entryId >= 100000 ? entryId / 100000 : entryId;
		return new MultiSellChoose(listId, realEntryId, Math.max(1, amount));
	}

	private static RequestBuyItem readBuyItem(PacketReader r) {
		int listId = r.readD();
		int count = r.readD();
		if (count <= 0 || count > 100 || r.remaining() < count * 8) {
			return new RequestBuyItem(listId, java.util.List.of());
		}
		java.util.List<ItemRequest> items = new java.util.ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			items.add(new ItemRequest(r.readD(), r.readD()));
		}
		return new RequestBuyItem(listId, items);
	}

	private static RequestSellItem readSellItem(PacketReader r) {
		int listId = r.readD();
		int count = r.readD();
		if (count <= 0 || count > 100 || r.remaining() < count * 12) {
			return new RequestSellItem(listId, java.util.List.of());
		}
		java.util.List<SellItemRequest> items = new java.util.ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			items.add(new SellItemRequest(r.readD(), r.readD(), r.readD()));
		}
		return new RequestSellItem(listId, items);
	}

	private static GameClientPacket readWareHouseList(PacketReader r, boolean deposit) {
		if (r.remaining() < 4) {
			return deposit ? new SendWareHouseDepositList(java.util.List.of()) : new SendWareHouseWithDrawList(java.util.List.of());
		}
		int count = r.readD();
		if (count <= 0 || count > 100 || r.remaining() < count * 8) {
			return deposit ? new SendWareHouseDepositList(java.util.List.of()) : new SendWareHouseWithDrawList(java.util.List.of());
		}
		java.util.List<WareHouseItemRequest> items = new java.util.ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			items.add(new WareHouseItemRequest(r.readD(), r.readD()));
		}
		return deposit ? new SendWareHouseDepositList(items) : new SendWareHouseWithDrawList(items);
	}

	private static GameClientPacket extended(PacketReader r) {
		if (r.remaining() < 2) {
			return new Unknown(0xd0, -1);
		}
		int sub = r.readH();
		return switch (sub) {
			case 0x04 -> r.remaining() >= 2 ? new RequestChangePartyLeader(r.readS()) : new Unknown(0xd0, sub);
			case 0x08 -> new RequestManorList();
			case 0x05 -> r.remaining() >= 8 ? new RequestAutoSoulShot(r.readD(), r.readD()) : new Unknown(0xd0, sub);
			case 0x06 -> r.remaining() >= 8 ? new RequestExEnchantSkillInfo(r.readD(), r.readD()) : new Unknown(0xd0, sub);
			case 0x07 -> r.remaining() >= 8 ? new RequestExEnchantSkill(r.readD(), r.readD()) : new Unknown(0xd0, sub);
			case 0x10 -> r.remaining() >= 4 ? new RequestExPledgeCrestLarge(r.readD()) : new Unknown(0xd0, sub);
			case 0x11 -> readSetPledgeCrestLarge(r);
			case 0x1a -> new RequestPledgePowerGradeList();
			case 0x1b -> r.remaining() >= 6 ? new RequestPledgeMemberPowerInfo(r.readD(), r.readS()) : new Unknown(0xd0, sub);
			case 0x1c -> r.remaining() >= 6 ? new RequestPledgeSetMemberPowerGrade(r.readS(), r.remaining() >= 4 ? r.readD() : 0) : new Unknown(0xd0, sub);
			case 0x1d -> r.remaining() >= 6 ? new RequestPledgeMemberInfo(r.readD(), r.readS()) : new Unknown(0xd0, sub);
			case 0x1e -> r.remaining() >= 8 ? new RequestPledgeWarList(r.readD(), r.readD()) : new RequestPledgeWarList(0, 0);
			case 0x22, 0x45 -> new RequestCursedWeaponList();
			case 0x23, 0x46 -> new RequestCursedWeaponLocation();
			case 0x27 -> r.remaining() >= 6 ? new RequestDuelStart(r.readS(), r.remaining() >= 4 ? r.readD() : 0) : new Unknown(0xd0, sub);
			case 0x28 -> r.remaining() >= 12 ? new RequestDuelAnswerStart(r.readD(), r.readD(), r.readD()) : new Unknown(0xd0, sub);
			case 0x29 -> r.remaining() >= 4 ? new RequestConfirmTargetItem(r.readD()) : new Unknown(0xd0, sub);
			case 0x2a -> r.remaining() >= 8 ? new RequestConfirmRefinerItem(r.readD(), r.readD()) : new Unknown(0xd0, sub);
			case 0x2b -> r.remaining() >= 16 ? new RequestConfirmGemStone(r.readD(), r.readD(), r.readD(), r.readD()) : new Unknown(0xd0, sub);
			case 0x2c -> r.remaining() >= 16 ? new RequestRefine(r.readD(), r.readD(), r.readD(), r.readD()) : new Unknown(0xd0, sub);
			case 0x2d -> r.remaining() >= 4 ? new RequestConfirmCancelItem(r.readD()) : new Unknown(0xd0, sub);
			case 0x2e -> r.remaining() >= 4 ? new RequestRefineCancel(r.readD()) : new Unknown(0xd0, sub);
			case 0x30 -> new RequestDuelSurrender();
			case 0x18 -> r.remaining() >= 4 ? new RequestGetBossRecord(r.readD()) : new RequestGetBossRecord(0);
			default -> new Unknown(0xd0, sub);
		};
	}

	private static GameClientPacket readSetPledgeCrestLarge(PacketReader r) {
		if (r.remaining() < 4) {
			return new RequestExSetPledgeCrestLarge(new byte[0]);
		}
		int length = r.readD();
		if (length <= 0 || length > 2176 || r.remaining() < length) {
			return new RequestExSetPledgeCrestLarge(new byte[0]);
		}
		return new RequestExSetPledgeCrestLarge(r.readB(length));
	}

	private static Say2 say2(PacketReader r) {
		String text = r.readS();
		int channel = r.readD();
		String target = channel == 2 && r.remaining() >= 2 ? r.readS() : null;
		return new Say2(text, channel, target);
	}

	private static RequestBBSwrite readBBSwrite(PacketReader r) {
		String url = r.remaining() >= 2 ? r.readS() : "";
		String arg1 = r.remaining() >= 2 ? r.readS() : "";
		String arg2 = r.remaining() >= 2 ? r.readS() : "";
		String arg3 = r.remaining() >= 2 ? r.readS() : "";
		String arg4 = r.remaining() >= 2 ? r.readS() : "";
		String arg5 = r.remaining() >= 2 ? r.readS() : "";
		return new RequestBBSwrite(url, arg1, arg2, arg3, arg4, arg5);
	}

	private static GameClientPacket readSetPledgeCrest(PacketReader r) {
		if (r.remaining() < 4) {
			return new RequestSetPledgeCrest(new byte[0]);
		}
		int length = r.readD();
		if (length <= 0 || length > 2176 || r.remaining() < length) {
			return new RequestSetPledgeCrest(new byte[0]);
		}
		byte[] data = r.readB(length);
		return new RequestSetPledgeCrest(data);
	}

	private static RequestMakeMacro readMakeMacro(PacketReader r) {
		int id = r.readD();
		String name = r.readS();
		String descr = r.readS();
		String acronym = r.readS();
		int icon = r.readC();
		int count = r.readC();
		if (count > 12) {
			count = 12;
		}
		java.util.List<com.lopez.l2j.game.macro.MacroCmd> commands = new java.util.ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			int entry = r.readC();
			int type = r.readC();
			int d1 = r.readD();
			int d2 = r.readC();
			String cmd = r.readS();
			commands.add(new com.lopez.l2j.game.macro.MacroCmd(entry, type, d1, d2, cmd));
		}
		return new RequestMakeMacro(new com.lopez.l2j.game.macro.Macro(id, icon, name, descr, acronym, commands));
	}
}
