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

	record ValidatePosition(int x, int y, int z, int heading, int data) implements GameClientPacket {
		public ValidatePosition(int x, int y, int z, int heading) {
			this(x, y, z, heading, 0);
		}
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

	record UseItem(int objectId, boolean ctrlPressed) implements GameClientPacket {
		public UseItem(int objectId) {
			this(objectId, false);
		}
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
	record MultiSellChoose(int listId, int entryId, int amount, int enchant, int augmentationId, int mana, int rawEntryId)
			implements GameClientPacket {
		public MultiSellChoose(int listId, int entryId, int amount) {
			this(listId, entryId, amount, 0, 0, -1, entryId);
		}

		public MultiSellChoose(int listId, int entryId, int amount, int enchant, int augmentationId, int mana) {
			this(listId, entryId, amount, enchant, augmentationId, mana, entryId);
		}
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

	/** 0xa6 - definir horario do cerco do castelo (RequestSetCastleSiegeTime). */
	record RequestSetCastleSiegeTime(int castleId, int time) implements GameClientPacket {
		public RequestSetCastleSiegeTime() {
			this(0, 0);
		}
	}

	/** 0xd0:0x93 - verificar se a conta possui senha secundaria (2nd Password). */
	record RequestEx2ndPasswordCheck() implements GameClientPacket {
	}

	/** 0xd0:0x94 - verificar/validar a senha secundaria (2nd Password). */
	record RequestEx2ndPasswordVerify(int mode, String pin) implements GameClientPacket {
		public RequestEx2ndPasswordVerify(String pin) {
			this(0, pin != null ? pin : "");
		}

		public RequestEx2ndPasswordVerify() {
			this(0, "");
		}
	}

	/** 0xd0:0x95 - criar ou alterar a senha secundaria (2nd Password). */
	record RequestEx2ndPasswordReq(int changePass, String pin, String newPin) implements GameClientPacket {
		public RequestEx2ndPasswordReq(String pin, String newPin) {
			this(0, pin != null ? pin : "", newPin != null ? newPin : "");
		}

		public RequestEx2ndPasswordReq(String pin) {
			this(0, pin != null ? pin : "", "");
		}

		public RequestEx2ndPasswordReq() {
			this(0, "", "");
		}
	}

	/** 0xa8 - resposta de latencia (NetPing) do cliente. */
	record NetPing(int kId, int ping, int data) implements GameClientPacket {
		public NetPing(int kId) {
			this(kId, 0, 0);
		}

		public NetPing() {
			this(0, 0, 0);
		}
	}

	/** 0xc5 - resposta a janela ConfirmDlg (ressurreicao, summon friend, casamento, porta). */
	record DlgAnswer(int messageId, int answer, int requesterId) implements GameClientPacket {
	}

	/** 0xc8 - avaliacao de atendimento de peticao (PetitionVote). */
	record PetitionVote(int type, int unk1, String feedback) implements GameClientPacket {
		public PetitionVote() {
			this(0, 0, "");
		}
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

	/** 0xd0:0x21 - solicitar mapeamento de teclas (RequestKeyMapping). */
	record RequestKeyMapping() implements GameClientPacket {
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

	/** 0xd0:0x36 - retornar a tela de selecao de personagens (GotoLobby). */
	record GotoLobby() implements GameClientPacket {
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

	/** 0x1c - alternar entre andar (0) e correr (1). */
	record RequestChangeMoveType(boolean typeRun) implements GameClientPacket {}

	/** 0x1d - alternar entre sentar (0) e levantar (1). */
	record RequestChangeWaitType(boolean typeStand) implements GameClientPacket {}

	/** 0x20 - clique em link de HTML estatico. */
	record RequestLinkHtml(String link) implements GameClientPacket {}

	/** 0x36 - parada de movimento por colisao no cliente. */
	record CannotMoveAnymore(int x, int y, int z, int heading) implements GameClientPacket {}

	/** 0x41 - movimento relativo por teclado (delta). */
	record MoveWithDelta(int dx, int dy, int dz) implements GameClientPacket {}

	/** 0x42 - embarcar em barco/veiculo. */
	record RequestGetOnVehicle(int boatId, int x, int y, int z) implements GameClientPacket {}

	/** 0x43 - desembarcar de barco/veiculo. */
	record RequestGetOffVehicle(int boatId, int x, int y, int z) implements GameClientPacket {}

	/** 0x47 - solicitar informacoes de Siege. */
	record RequestSiegeInfo() implements GameClientPacket {}

	/** 0x4a - iniciar rotacao pelo teclado. */
	record StartRotating(int degree, int side) implements GameClientPacket {}

	/** 0x4b - finalizar rotacao pelo teclado. */
	record FinishRotating(int degree, int unknown) implements GameClientPacket {}

	/** 0x4e - responder declaracao de guerra de cla. */
	record RequestReplyStartPledgeWar(String reqName, int answer) implements GameClientPacket {}

	/** 0x50 - responder pedido de cessar-fogo de guerra de cla. */
	record RequestReplyStopPledgeWar(String reqName, int answer) implements GameClientPacket {}

	/** 0x51 - render-se em guerra de cla. */
	record RequestSurrenderPledgeWar(String pledgeName) implements GameClientPacket {}

	/** 0x5c - mover dentro de barco/veiculo. */
	record RequestMoveToLocationInVehicle(int boatId, int targetX, int targetY, int targetZ, int originX, int originY, int originZ) implements GameClientPacket {}

	/** 0x5d - parada de movimento dentro de barco/veiculo. */
	record CannotMoveAnymoreInVehicle(int boatId, int x, int y, int z, int heading) implements GameClientPacket {}

	/** 0x67 - informacoes estendidas do cla. */
	record RequestPledgeExtendedInfo(String name) implements GameClientPacket {}

	/** 0x69 - rendicao individual em guerra de cla. */
	record RequestSurrenderPersonally(String pledgeName) implements GameClientPacket {}

	/** 0x6f - abrir/configurar busca de grupo (Party Matching). */
	record RequestPartyMatchConfig(int auto, int loc, int lvl) implements GameClientPacket {}

	/** 0x70 - criar/atualizar ou listar salas de Party Matching. */
	record RequestPartyMatchList(int roomId, int maxMembers, int minLvl, int maxLvl, int lootType, String roomTitle) implements GameClientPacket {}

	/** 0x71 - solicitar detalhes de sala de Party Matching. */
	record RequestPartyMatchDetail(int roomId, int unk1, int unk2, int unk3) implements GameClientPacket {}

	/** 0x75 - cancelar gerenciamento de loja pessoal. */
	record RequestPrivateStoreManageCancel() implements GameClientPacket {}

	/** 0x7f - enviar peticao de suporte aos GMs. */
	record RequestPetition(String content, int type) implements GameClientPacket {}

	/** 0x80 - cancelar peticao de suporte. */
	record RequestPetitionCancel() implements GameClientPacket {}

	/** 0x81 - listar GMs online (/gmlist). */
	record RequestGmList() implements GameClientPacket {}

	/** 0x84 - sair da alianca (/allyleave). */
	record AllyLeave() implements GameClientPacket {}

	/** 0x85 - expulsar cla da alianca (/allydismiss). */
	record AllyDismiss(String clanName) implements GameClientPacket {}

	/** 0x86 - dissolver alianca (/allydissolve). */
	record RequestDismissAlly() implements GameClientPacket {}

	/** 0x87 - definir brasao da alianca (8x12 BMP). */
	record RequestSetAllyCrest(byte[] data) implements GameClientPacket {}

	/** 0x88 - solicitar brasao da alianca por crestId. */
	record RequestAllyCrest(int crestId) implements GameClientPacket {}

	/** 0x89 - definir nome do Pet. */
	record RequestChangePetName(String name) implements GameClientPacket {}

	/** 0x8a - usar item do inventario do Pet. */
	record RequestPetUseItem(int objectId) implements GameClientPacket {}

	/** 0x8b - transferir item do jogador para o Pet. */
	record RequestGiveItemToPet(int objectId, int amount) implements GameClientPacket {}

	/** 0x8c - transferir item do Pet para o jogador. */
	record RequestGetItemFromPet(int objectId, int amount, int unknown) implements GameClientPacket {}

	/** 0x8e - consultar informacoes da alianca (/allyinfo). */
	record RequestAllyInfo() implements GameClientPacket {}

	/** 0x8f - comandar o Pet a pegar item no chao. */
	record RequestPetGetItem(int objectId) implements GameClientPacket {}

	/** 0x9e - solicitar lista de itens enviaveis por Freight. */
	record RequestPackageSendableItemList(int playerObjectId) implements GameClientPacket {}

	/** 0x9f - enviar pacote de itens por Freight para outro personagem da conta. */
	record RequestPackageSend(int playerObjectId, List<WareHouseItemRequest> items) implements GameClientPacket {}

	/** 0xab / 0xb4 - encerrar modo Snoop (escuta de chat). */
	record SnoopQuit(int snoopId) implements GameClientPacket {}

	/** 0xad - apagar receita do livro de receitas. */
	record RequestRecipeBookDestroy(int recipeId) implements GameClientPacket {}

	/** 0xb0 - abrir gerenciamento de loja de craft (Private Manufacture). */
	record RequestRecipeShopManageList() implements GameClientPacket {}

	/** 0xb1 - definir mensagem da loja de craft (Private Manufacture). */
	record RequestRecipeShopMessageSet(String name) implements GameClientPacket {}

	record ManufactureRecipeRequest(int recipeId, int cost) {}

	/** 0xb2 - confirmar lista de receitas e precos na loja de craft. */
	record RequestRecipeShopListSet(List<ManufactureRecipeRequest> items) implements GameClientPacket {}

	/** 0xb3 - fechar loja de craft (Private Manufacture). */
	record RequestRecipeShopManageQuit() implements GameClientPacket {}

	/** 0xb5 - inspecionar receita na loja de craft de outro jogador. */
	record RequestRecipeShopMakeInfo(int playerObjectId, int recipeId) implements GameClientPacket {}

	/** 0xb6 - fabricar item na loja de craft de outro jogador. */
	record RequestRecipeShopMakeItem(int playerObjectId, int recipeId, int unknown) implements GameClientPacket {}

	/** 0xb7 - voltar a tela anterior na loja de craft de outro jogador. */
	record RequestRecipeShopManagePrev(int playerObjectId) implements GameClientPacket {
		public RequestRecipeShopManagePrev() {
			this(0);
		}
	}

	/** 0xb8 - sair do modo observador (Siege / Broadcast Crystal). */
	record ObserverReturn() implements GameClientPacket {}

	/** 0xc3 - vender colheitas (Procure) no Manor Manager. */
	record RequestBuyProcure(int listId, List<ItemRequest> items, List<ProcureCropRequest> procureItems) implements GameClientPacket {
		public RequestBuyProcure(int listId, List<ItemRequest> items) {
			this(listId, items, java.util.List.of());
		}

		public int manorId() {
			return listId;
		}
	}

	/** 0xc4 - comprar sementes (Seeds) no Manor Manager. */
	record RequestBuySeed(int manorId, List<ItemRequest> seeds) implements GameClientPacket {}

	/** 0xc6 - provar itens na loja (Wear / Try-On). */
	record RequestPreviewItem(int unk, int listId, List<Integer> items) implements GameClientPacket {}

	/** 0xca - resposta do cliente ao GameGuardQuery. */
	record GameGuardReply(int dx, int dy, int dz, int dw) implements GameClientPacket {}

	/** 0xcc - enviar mensagem privada na janela de amigos (L2FriendSay). */
	record RequestSendFriendMsg(String message, String receiver) implements GameClientPacket {}

	/** 0xcf - solicitar reenvio de objetos visiveis ao redor (/loc). */
	record RequestRecordInfo() implements GameClientPacket {}

	/** 0xd0:0x01 - expulsar jogador da sala de Party Matching. */
	record RequestOustFromPartyRoom(int charId) implements GameClientPacket {}

	/** 0xd0:0x02 - encerrar sala de Party Matching. */
	record RequestDismissPartyRoom(int roomId, int data2) implements GameClientPacket {}

	/** 0xd0:0x03 - sair da sala de Party Matching. */
	record RequestWithdrawPartyRoom(int roomId, int unk1) implements GameClientPacket {}

	record ProcureCropRequest(int objectId, int itemId, int manorId, int count) {}

	/** 0xd0:0x09 - entregar colheitas no Manor Manager. */
	record RequestProcureCropList(List<ProcureCropRequest> items) implements GameClientPacket {}

	record SeedSettingRequest(int itemId, int sales, int price) {}

	/** 0xd0:0x0a - configurar producao de sementes do castelo (Manor). */
	record RequestSetSeed(int manorId, List<SeedSettingRequest> seeds) implements GameClientPacket {}

	record CropSettingRequest(int itemId, int sales, int price, int type) {}

	/** 0xd0:0x0b - configurar compra de colheitas do castelo (Manor). */
	record RequestSetCrop(int manorId, List<CropSettingRequest> crops) implements GameClientPacket {}

	/** 0xd0:0x0c - definir mensagem de Heroi no Monumento. */
	record RequestWriteHeroWords(String heroWords) implements GameClientPacket {}

	/** 0xd0:0x0d - convidar grupo para o Command Channel (MPCC). */
	record RequestExAskJoinMPCC(String name) implements GameClientPacket {}

	/** 0xd0:0x0e - responder convite do Command Channel (MPCC). */
	record RequestExAcceptJoinMPCC(int response, int unk) implements GameClientPacket {
		public RequestExAcceptJoinMPCC(int response) {
			this(response, 0);
		}
	}

	/** 0xd0:0x0f - expulsar grupo do Command Channel (MPCC). */
	record RequestExOustFromMPCC(String name) implements GameClientPacket {}

	/** 0xd0:0x12 - sair do modo espectador das Olimpiadas. */
	record RequestOlympiadObserverEnd() implements GameClientPacket {}

	/** 0xd0:0x13 - solicitar lista de lutas das Olimpiadas (em modo espectador). */
	record RequestOlympiadMatchList() implements GameClientPacket {}

	/** 0xd0:0x14 - convidar jogador para sala de Party Matching. */
	record RequestAskJoinPartyRoom(String name) implements GameClientPacket {}

	/** 0xd0:0x15 - responder convite de sala de Party Matching. */
	record AnswerJoinPartyRoom(int answer) implements GameClientPacket {}

	/** 0xd0:0x16 - listar jogadores na fila de espera de Party Matching. */
	record RequestListPartyMatchingWaitingRoom(int page, int minLvl, int maxLvl, int mode) implements GameClientPacket {}

	/** 0xd0:0x17 - sair da fila de espera de Party Matching. */
	record RequestExitPartyMatchingWaitingRoom() implements GameClientPacket {}

	/** 0xd0:0x19 - definir ou remover padrinho (Sponsor) de membro da Academia do cla. */
	record RequestPledgeSetAcademyMaster(int set, String currPlayerName, String targetPlayerName) implements GameClientPacket {}

	/** 0xd0:0x1f - solicitar ranking do Campeonato de Pesca. */
	record RequestExFishRanking() implements GameClientPacket {}

	/** 0xd0:0x20 - usar codigo de cupom de PC Cafe. */
	record RequestPCCafeCouponUse(String couponCode) implements GameClientPacket {}

	/** 0xd0:0x24 - reorganizar membro entre sub-unidades do cla. */
	record RequestPledgeReorganizeMember(int isMemberSelected, String memberName, int newPledgeType, String selectedMember) implements GameClientPacket {}

	/** 0xd0:0x26 - exibir membros de uma party especifica no Command Channel (MPCC). */
	record RequestExMPCCShowPartyMembersInfo(int partyLeaderId) implements GameClientPacket {}

	/** 0xd0:0x2f - conjurar habilidade de alvo no chao (Ground Target Skill). */
	record RequestExMagicSkillUseGround(int x, int y, int z, int skillId, boolean ctrlPressed, boolean shiftPressed) implements GameClientPacket {}

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
					case 0xa8 -> readNetPing(r);
					case 0xca -> readGameGuardReply(r);
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
					case 0x21 -> new RequestBypassToServer(r.remaining() >= 2 ? r.readS() : "");
					case 0x68 -> new RequestPledgeCrest(r.readD());
					case 0xa8 -> readNetPing(r);
					case 0xca -> readGameGuardReply(r);
					case 0xd0 -> extendedAuthed(r);
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
					case 0x48 -> new ValidatePosition(r.readD(), r.readD(), r.readD(), r.readD(), r.remaining() >= 4 ? r.readD() : 0);
					case 0x38 -> say2(r);
					case 0x04 -> new Action(r.readD(), r.readD(), r.readD(), r.readD(), r.readC());
					case 0x1c -> new RequestChangeMoveType(r.remaining() >= 4 && r.readD() == 1);
					case 0x1d -> new RequestChangeWaitType(r.remaining() >= 4 && r.readD() == 1);
					case 0x20 -> readLinkHtml(r);
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
					case 0x36 -> r.remaining() >= 16 ? new CannotMoveAnymore(r.readD(), r.readD(), r.readD(), r.readD()) : new Unknown(op, -1);
					case 0x37 -> new RequestTargetCancel();
					case 0x3c -> new RequestPledgeMemberList();
					case 0x41 -> r.remaining() >= 12 ? new MoveWithDelta(r.readD(), r.readD(), r.readD()) : new Unknown(op, -1);
					case 0x42 -> r.remaining() >= 16 ? new RequestGetOnVehicle(r.readD(), r.readD(), r.readD(), r.readD()) : new Unknown(op, -1);
					case 0x43 -> r.remaining() >= 16 ? new RequestGetOffVehicle(r.readD(), r.readD(), r.readD(), r.readD()) : new Unknown(op, -1);
					case 0x44 -> r.remaining() >= 4 ? new AnswerTradeRequest(r.readD()) : new Unknown(op, -1);
					case 0x45 -> new RequestActionUse(r.readD(), r.readD() == 1, r.readC() == 1);
					case 0x47 -> new RequestSiegeInfo();
					case 0x4a -> r.remaining() >= 8 ? new StartRotating(r.readD(), r.readD()) : new Unknown(op, -1);
					case 0x4b -> r.remaining() >= 8 ? new FinishRotating(r.readD(), r.readD()) : new Unknown(op, -1);
					case 0x4d -> r.remaining() >= 2 ? new RequestStartPledgeWar(r.readS()) : new Unknown(op, -1);
					case 0x4e -> readReplyPledgeWar(r, true);
					case 0x4f -> r.remaining() >= 2 ? new RequestStopPledgeWar(r.readS()) : new Unknown(op, -1);
					case 0x50 -> readReplyPledgeWar(r, false);
					case 0x51 -> r.remaining() >= 2 ? new RequestSurrenderPledgeWar(r.readS()) : new Unknown(op, -1);
					case 0x0f -> new RequestItemList();
					case 0x14 -> new UseItem(r.readD(), r.remaining() >= 4 && r.readD() != 0);
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
					case 0x55 -> readGiveNickName(r, op);
					case 0x57 -> new RequestShowBoard(r.remaining() >= 4 ? r.readD() : 101);
					case 0x58 -> r.remaining() >= 4 ? new RequestEnchantItem(r.readD()) : new Unknown(op, -1);
					case 0x59 -> r.remaining() >= 8 ? new RequestDestroyItem(r.readD(), r.readD()) : new Unknown(op, -1);
					case 0x5b -> new SendBypassBuildCmd(r.readS());
					case 0x5c -> r.remaining() >= 28 ? new RequestMoveToLocationInVehicle(r.readD(), r.readD(), r.readD(), r.readD(), r.readD(), r.readD(), r.readD()) : new Unknown(op, -1);
					case 0x5d -> r.remaining() >= 20 ? new CannotMoveAnymoreInVehicle(r.readD(), r.readD(), r.readD(), r.readD(), r.readD()) : new Unknown(op, -1);
					case 0x64 -> r.remaining() >= 4 ? new RequestQuestAbort(r.readD()) : new Unknown(op, -1);
					case 0x66 -> new RequestPledgeInfo(r.readD());
					case 0x67 -> new RequestPledgeExtendedInfo(r.remaining() >= 2 ? r.readS() : "");
					case 0x68 -> new RequestPledgeCrest(r.readD());
					case 0x69 -> new RequestSurrenderPersonally(r.remaining() >= 2 ? r.readS() : "");
					case 0x6b -> readAcquireSkillInfo(r);
					case 0x6c -> readAcquireSkill(r);
					case 0x6d -> new RequestRestartPoint(r.readD());
					case 0x6e -> {
						String target = r.readS();
						int cmd = r.remaining() >= 4 ? r.readD() : 1;
						yield new RequestGMCommand(target, cmd);
					}
					case 0x6f -> r.remaining() >= 12 ? new RequestPartyMatchConfig(r.readD(), r.readD(), r.readD()) : new RequestPartyMatchConfig(0, 0, 0);
					case 0x70 -> r.remaining() >= 22 ? new RequestPartyMatchList(r.readD(), r.readD(), r.readD(), r.readD(), r.readD(), r.readS()) : new RequestPartyMatchList(0, 8, 1, 80, 0, "");
					case 0x71 -> r.remaining() >= 16 ? new RequestPartyMatchDetail(r.readD(), r.readD(), r.readD(), r.readD()) : new RequestPartyMatchDetail(r.remaining() >= 4 ? r.readD() : 0, 0, 0, 0);
					case 0x72 -> r.remaining() >= 8 ? new RequestCrystallizeItem(r.readD(), r.readD()) : new Unknown(op, -1);
					case 0x73 -> new RequestPrivateStoreManageSell();
					case 0x74 -> readSetPrivateStoreListSell(r);
					case 0x75 -> new RequestPrivateStoreManageCancel();
					case 0x76 -> new RequestPrivateStoreQuitSell();
					case 0x77 -> new SetPrivateStoreMsgSell(r.readS());
					case 0x79 -> readRequestPrivateStoreBuy(r);
					case 0x7b -> new RequestTutorialLinkHtml(r.readS());
					case 0x7c -> new RequestTutorialPassCmdToServer(r.readS());
					case 0x7d -> new RequestTutorialQuestionMark(r.readD());
					case 0x7e -> new RequestTutorialClientEvent(r.readD());
					case 0x7f -> new RequestPetition(r.remaining() >= 2 ? r.readS() : "", r.remaining() >= 4 ? r.readD() : 1);
					case 0x80 -> new RequestPetitionCancel();
					case 0x81 -> new RequestGmList();
					case 0x82 -> r.remaining() >= 4 ? new RequestJoinAlly(r.readD()) : new Unknown(op, -1);
					case 0x83 -> r.remaining() >= 4 ? new RequestAnswerJoinAlly(r.readD()) : new Unknown(op, -1);
					case 0x84 -> new AllyLeave();
					case 0x85 -> new AllyDismiss(r.remaining() >= 2 ? r.readS() : "");
					case 0x86 -> new RequestDismissAlly();
					case 0x87 -> readSetAllyCrest(r);
					case 0x88 -> r.remaining() >= 4 ? new RequestAllyCrest(r.readD()) : new Unknown(op, -1);
					case 0x89 -> new RequestChangePetName(r.remaining() >= 2 ? r.readS() : "");
					case 0x8a -> r.remaining() >= 4 ? new RequestPetUseItem(r.readD()) : new Unknown(op, -1);
					case 0x8b -> r.remaining() >= 8 ? new RequestGiveItemToPet(r.readD(), r.readD()) : new Unknown(op, -1);
					case 0x8c -> r.remaining() >= 8 ? new RequestGetItemFromPet(r.readD(), r.readD(), r.remaining() >= 4 ? r.readD() : 0) : new Unknown(op, -1);
					case 0x8e -> new RequestAllyInfo();
					case 0x8f -> r.remaining() >= 4 ? new RequestPetGetItem(r.readD()) : new Unknown(op, -1);
					case 0x90 -> new RequestPrivateStoreManageBuy();
					case 0x91 -> readSetPrivateStoreListBuy(r);
					case 0x93 -> new RequestPrivateStoreQuitBuy();
					case 0x94 -> new SetPrivateStoreMsgBuy(r.remaining() >= 2 ? r.readS() : "");
					case 0x96 -> readRequestPrivateStoreSell(r);
					case 0x9d -> new RequestSkillCoolTime();
					case 0xa6 -> r.remaining() >= 8 ? new RequestSetCastleSiegeTime(r.readD(), r.readD()) : new RequestSetCastleSiegeTime(r.remaining() >= 4 ? r.readD() : 0, 0);
					case 0x9e -> r.remaining() >= 4 ? new RequestPackageSendableItemList(r.readD()) : new Unknown(op, -1);
					case 0x9f -> readPackageSend(r);
					case 0x5e -> new RequestFriendInvite(r.readS());
					case 0x5f -> new RequestAnswerFriendInvite(r.readD());
					case 0x60 -> new RequestFriendList();
					case 0x61 -> new RequestFriendDel(r.readS());
					case 0x63 -> new RequestQuestList();
					case 0x09 -> new Logout();
					case 0x46 -> new RequestRestart();
					case 0xa8 -> readNetPing(r);
					case 0xaa -> new RequestUserCommand(r.readD());
					case 0xab, 0xb4 -> new SnoopQuit(r.remaining() >= 4 ? r.readD() : 0);
					case 0xac -> new RequestRecipeBookOpen(r.remaining() >= 4 ? r.readD() == 0 : true);
					case 0xad -> r.remaining() >= 4 ? new RequestRecipeBookDestroy(r.readD()) : new Unknown(op, -1);
					case 0xae -> r.remaining() >= 4 ? new RequestRecipeItemMakeInfo(r.readD()) : new Unknown(op, -1);
					case 0xaf -> r.remaining() >= 4 ? new RequestRecipeItemMakeSelf(r.readD()) : new Unknown(op, -1);
					case 0xb0 -> new RequestRecipeShopManageList();
					case 0xb1 -> new RequestRecipeShopMessageSet(r.remaining() >= 2 ? r.readS() : "");
					case 0xb2 -> readRecipeShopListSet(r);
					case 0xb3 -> new RequestRecipeShopManageQuit();
					case 0xb5 -> r.remaining() >= 8 ? new RequestRecipeShopMakeInfo(r.readD(), r.readD()) : new Unknown(op, -1);
					case 0xb6 -> r.remaining() >= 8 ? new RequestRecipeShopMakeItem(r.readD(), r.readD(), r.remaining() >= 4 ? r.readD() : 0) : new Unknown(op, -1);
					case 0xb7 -> new RequestRecipeShopManagePrev(r.remaining() >= 4 ? r.readD() : 0);
					case 0xb8 -> new ObserverReturn();
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
					case 0xc3 -> readBuyProcure(r);
					case 0xc4 -> readBuySeed(r);
					case 0xc5 -> r.remaining() >= 8 ? new DlgAnswer(r.readD(), r.readD(), r.remaining() >= 4 ? r.readD() : 0) : new Unknown(op, -1);
					case 0xc6 -> readPreviewItem(r);
					case 0xc7 -> r.remaining() >= 1 ? new RequestSSQStatus(r.readC()) : new Unknown(op, -1);
					case 0xc8 -> readPetitionVote(r);
					case 0xca -> readGameGuardReply(r);
					case 0xcc -> new RequestSendFriendMsg(r.remaining() >= 2 ? r.readS() : "", r.remaining() >= 2 ? r.readS() : "");
					case 0xcd -> new RequestShowMiniMap();
					case 0xcf -> new RequestRecordInfo();
					case 0xd0 -> extended(r);
					default -> new Unknown(op, -1);
				};
			});
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	private static NetPing readNetPing(PacketReader r) {
		int kId = r.remaining() >= 4 ? r.readD() : 0;
		int ping = r.remaining() >= 4 ? r.readD() : 0;
		int data = r.remaining() >= 4 ? r.readD() : 0;
		return new NetPing(kId, ping, data);
	}

	private static PetitionVote readPetitionVote(PacketReader r) {
		int type = r.remaining() >= 4 ? r.readD() : 0;
		int unk1 = r.remaining() >= 4 ? r.readD() : 0;
		String feedback = r.remaining() >= 2 ? r.readS() : "";
		if (feedback.length() > 2000) {
			feedback = feedback.substring(0, 2000);
		}
		return new PetitionVote(type, unk1, feedback);
	}

	private static RequestLinkHtml readLinkHtml(PacketReader r) {
		String link = r.remaining() >= 2 ? r.readS() : "";
		if (link.indexOf('\0') >= 0 || link.indexOf('\r') >= 0 || link.indexOf('\n') >= 0) {
			link = "";
		}
		return new RequestLinkHtml(link);
	}

	private static GameClientPacket readGiveNickName(PacketReader r, int op) {
		if (r.remaining() < 4) {
			return new Unknown(op, -1);
		}
		String target = r.readS();
		String title = r.remaining() >= 2 ? r.readS() : "";
		if (target.length() > 16) {
			target = target.substring(0, 16);
		}
		if (title.length() > 16) {
			title = title.substring(0, 16);
		}
		return new RequestGiveNickName(target, title);
	}

	private static RequestAcquireSkillInfo readAcquireSkillInfo(PacketReader r) {
		int skillId = r.remaining() >= 4 ? r.readD() : 0;
		int level = r.remaining() >= 4 ? r.readD() : 0;
		int skillType = r.remaining() >= 4 ? r.readD() : 0;
		if (skillType < 0 || skillType > 2) {
			skillType = 0;
		}
		return new RequestAcquireSkillInfo(skillId, level, skillType);
	}

	private static RequestAcquireSkill readAcquireSkill(PacketReader r) {
		int skillId = r.remaining() >= 4 ? r.readD() : 0;
		int level = r.remaining() >= 4 ? r.readD() : 0;
		int skillType = r.remaining() >= 4 ? r.readD() : 0;
		if (skillType < 0 || skillType > 2) {
			skillType = 0;
		}
		return new RequestAcquireSkill(skillId, level, skillType);
	}

	private static SetPrivateStoreListBuy readSetPrivateStoreListBuy(PacketReader r) {
		if (r.remaining() < 4) {
			return new SetPrivateStoreListBuy(java.util.List.of());
		}
		int count = r.readD();
		if (count <= 0 || count > 100) {
			return new SetPrivateStoreListBuy(java.util.List.of());
		}
		if (r.remaining() >= count * 16) {
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
		if (r.remaining() >= count * 12) {
			java.util.List<BuyStoreItemRequest> list = new java.util.ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				int itemId = r.readD();
				int cnt = r.readD();
				int price = r.readD();
				list.add(new BuyStoreItemRequest(itemId, 0, cnt, price));
			}
			return new SetPrivateStoreListBuy(list);
		}
		return new SetPrivateStoreListBuy(java.util.List.of());
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
		int rawEntryId = r.readD();
		int amount = r.readD();
		int enchant = r.remaining() >= 2 ? r.readH() : 0;
		if (r.remaining() >= 2) {
			r.readH(); // pad/unk
		}
		int augmentationId = r.remaining() >= 4 ? r.readD() : 0;
		int mana = r.remaining() >= 4 ? r.readD() : -1;
		int realEntryId = rawEntryId >= 100000 ? rawEntryId / 100000 : rawEntryId;
		return new MultiSellChoose(listId, realEntryId, Math.max(1, amount), enchant, augmentationId, mana, rawEntryId);
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

	private static GameClientPacket extendedAuthed(PacketReader r) {
		if (r.remaining() < 2) {
			return new Unknown(0xd0, -1);
		}
		int sub = r.readH();
		return switch (sub) {
			case 0x08 -> new RequestManorList();
			case 0x21 -> new RequestKeyMapping();
			case 0x36 -> new GotoLobby();
			case 0x93 -> new RequestEx2ndPasswordCheck();
			case 0x94 -> {
				int mode = r.remaining() >= 1 ? r.readC() : 0;
				String pin = r.remaining() >= 2 ? r.readS() : "";
				yield new RequestEx2ndPasswordVerify(mode, pin);
			}
			case 0x95 -> {
				int changePass = r.remaining() >= 1 ? r.readC() : 0;
				String pin = r.remaining() >= 2 ? r.readS() : "";
				String newPin = r.remaining() >= 2 ? r.readS() : "";
				yield new RequestEx2ndPasswordReq(changePass, pin, newPin);
			}
			default -> new Unknown(0xd0, sub);
		};
	}

	private static GameClientPacket extended(PacketReader r) {
		if (r.remaining() < 2) {
			return new Unknown(0xd0, -1);
		}
		int sub = r.readH();
		return switch (sub) {
			case 0x01 -> r.remaining() >= 4 ? new RequestOustFromPartyRoom(r.readD()) : new Unknown(0xd0, sub);
			case 0x02 -> r.remaining() >= 8 ? new RequestDismissPartyRoom(r.readD(), r.readD()) : new RequestDismissPartyRoom(r.remaining() >= 4 ? r.readD() : 0, 0);
			case 0x03 -> r.remaining() >= 8 ? new RequestWithdrawPartyRoom(r.readD(), r.readD()) : new RequestWithdrawPartyRoom(r.remaining() >= 4 ? r.readD() : 0, 0);
			case 0x04, 0xee -> r.remaining() >= 2 ? new RequestChangePartyLeader(r.readS()) : new Unknown(0xd0, sub);
			case 0x08 -> new RequestManorList();
			case 0x05 -> r.remaining() >= 8 ? new RequestAutoSoulShot(r.readD(), r.readD()) : new Unknown(0xd0, sub);
			case 0x06 -> r.remaining() >= 8 ? new RequestExEnchantSkillInfo(r.readD(), r.readD()) : new Unknown(0xd0, sub);
			case 0x07 -> r.remaining() >= 8 ? new RequestExEnchantSkill(r.readD(), r.readD()) : new Unknown(0xd0, sub);
			case 0x09 -> readProcureCropList(r);
			case 0x0a -> readSetSeed(r);
			case 0x0b -> readSetCrop(r);
			case 0x0c -> new RequestWriteHeroWords(r.remaining() >= 2 ? r.readS() : "");
			case 0x0d -> new RequestExAskJoinMPCC(r.remaining() >= 2 ? r.readS() : "");
			case 0x0e -> new RequestExAcceptJoinMPCC(r.remaining() >= 4 ? r.readD() : 0, r.remaining() >= 4 ? r.readD() : 0);
			case 0x0f -> new RequestExOustFromMPCC(r.remaining() >= 2 ? r.readS() : "");
			case 0x10 -> r.remaining() >= 4 ? new RequestExPledgeCrestLarge(r.readD()) : new Unknown(0xd0, sub);
			case 0x11 -> readSetPledgeCrestLarge(r);
			case 0x12 -> new RequestOlympiadObserverEnd();
			case 0x13 -> new RequestOlympiadMatchList();
			case 0x14 -> new RequestAskJoinPartyRoom(r.remaining() >= 2 ? r.readS() : "");
			case 0x15 -> new AnswerJoinPartyRoom(r.remaining() >= 4 ? r.readD() : 0);
			case 0x16 -> r.remaining() >= 16 ? new RequestListPartyMatchingWaitingRoom(r.readD(), r.readD(), r.readD(), r.readD()) : new RequestListPartyMatchingWaitingRoom(1, 1, 80, 0);
			case 0x17 -> new RequestExitPartyMatchingWaitingRoom();
			case 0x18 -> r.remaining() >= 4 ? new RequestGetBossRecord(r.readD()) : new RequestGetBossRecord(0);
			case 0x19 -> r.remaining() >= 8 ? new RequestPledgeSetAcademyMaster(r.readD(), r.readS(), r.remaining() >= 2 ? r.readS() : "") : new Unknown(0xd0, sub);
			case 0x1a -> new RequestPledgePowerGradeList();
			case 0x1b -> r.remaining() >= 6 ? new RequestPledgeMemberPowerInfo(r.readD(), r.readS()) : new Unknown(0xd0, sub);
			case 0x1c, 0x25 -> r.remaining() >= 6 ? new RequestPledgeSetMemberPowerGrade(r.readS(), r.remaining() >= 4 ? r.readD() : 0) : new Unknown(0xd0, sub);
			case 0x1d -> r.remaining() >= 6 ? new RequestPledgeMemberInfo(r.readD(), r.readS()) : new Unknown(0xd0, sub);
			case 0x1e -> r.remaining() >= 8 ? new RequestPledgeWarList(r.readD(), r.readD()) : new RequestPledgeWarList(0, 0);
			case 0x1f -> new RequestExFishRanking();
			case 0x20 -> new RequestPCCafeCouponUse(r.remaining() >= 2 ? r.readS() : "");
			case 0x21 -> new RequestKeyMapping();
			case 0x22, 0x45 -> new RequestCursedWeaponList();
			case 0x23, 0x46 -> new RequestCursedWeaponLocation();
			case 0x24 -> readPledgeReorganizeMember(r);
			case 0x26 -> new RequestExMPCCShowPartyMembersInfo(r.remaining() >= 4 ? r.readD() : 0);
			case 0x27 -> r.remaining() >= 6 ? new RequestDuelStart(r.readS(), r.remaining() >= 4 ? r.readD() : 0) : new Unknown(0xd0, sub);
			case 0x28 -> r.remaining() >= 12 ? new RequestDuelAnswerStart(r.readD(), r.readD(), r.readD()) : new Unknown(0xd0, sub);
			case 0x29 -> r.remaining() >= 4 ? new RequestConfirmTargetItem(r.readD()) : new Unknown(0xd0, sub);
			case 0x2a -> r.remaining() >= 8 ? new RequestConfirmRefinerItem(r.readD(), r.readD()) : new Unknown(0xd0, sub);
			case 0x2b -> r.remaining() >= 16 ? new RequestConfirmGemStone(r.readD(), r.readD(), r.readD(), r.readD()) : new Unknown(0xd0, sub);
			case 0x2c -> r.remaining() >= 16 ? new RequestRefine(r.readD(), r.readD(), r.readD(), r.readD()) : new Unknown(0xd0, sub);
			case 0x2d -> r.remaining() >= 4 ? new RequestConfirmCancelItem(r.readD()) : new Unknown(0xd0, sub);
			case 0x2e -> r.remaining() >= 4 ? new RequestRefineCancel(r.readD()) : new Unknown(0xd0, sub);
			case 0x2f -> r.remaining() >= 21 ? new RequestExMagicSkillUseGround(r.readD(), r.readD(), r.readD(), r.readD(), r.readD() != 0, r.readC() != 0) : new Unknown(0xd0, sub);
			case 0x30 -> new RequestDuelSurrender();
			case 0x36 -> new GotoLobby();
			case 0x93 -> new RequestEx2ndPasswordCheck();
			case 0x94 -> {
				int mode = r.remaining() >= 1 ? r.readC() : 0;
				String pin = r.remaining() >= 2 ? r.readS() : "";
				yield new RequestEx2ndPasswordVerify(mode, pin);
			}
			case 0x95 -> {
				int changePass = r.remaining() >= 1 ? r.readC() : 0;
				String pin = r.remaining() >= 2 ? r.readS() : "";
				String newPin = r.remaining() >= 2 ? r.readS() : "";
				yield new RequestEx2ndPasswordReq(changePass, pin, newPin);
			}
			default -> new Unknown(0xd0, sub);
		};
	}

	private static GameGuardReply readGameGuardReply(PacketReader r) {
		int dx = r.remaining() >= 4 ? r.readD() : 0;
		int dy = r.remaining() >= 4 ? r.readD() : 0;
		int dz = r.remaining() >= 4 ? r.readD() : 0;
		int dw = r.remaining() >= 4 ? r.readD() : 0;
		return new GameGuardReply(dx, dy, dz, dw);
	}

	private static GameClientPacket readReplyPledgeWar(PacketReader r, boolean start) {
		String reqName = "";
		if (r.remaining() > 4) {
			reqName = r.readS();
		}
		int answer = r.remaining() >= 4 ? r.readD() : 0;
		return start ? new RequestReplyStartPledgeWar(reqName, answer) : new RequestReplyStopPledgeWar(reqName, answer);
	}

	private static RequestSetAllyCrest readSetAllyCrest(PacketReader r) {
		if (r.remaining() < 4) {
			return new RequestSetAllyCrest(new byte[0]);
		}
		int length = r.readD();
		if (length <= 0 || length > 192 || r.remaining() < length) {
			return new RequestSetAllyCrest(new byte[0]);
		}
		return new RequestSetAllyCrest(r.readB(length));
	}

	private static RequestPackageSend readPackageSend(PacketReader r) {
		if (r.remaining() < 8) {
			return new RequestPackageSend(0, java.util.List.of());
		}
		int playerObjId = r.readD();
		int count = r.readD();
		if (count <= 0 || count > 80 || r.remaining() < count * 8) {
			return new RequestPackageSend(playerObjId, java.util.List.of());
		}
		java.util.List<WareHouseItemRequest> items = new java.util.ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			items.add(new WareHouseItemRequest(r.readD(), r.readD()));
		}
		return new RequestPackageSend(playerObjId, items);
	}

	private static RequestRecipeShopListSet readRecipeShopListSet(PacketReader r) {
		if (r.remaining() < 4) {
			return new RequestRecipeShopListSet(java.util.List.of());
		}
		int count = r.readD();
		if (count <= 0 || count > 100 || r.remaining() < count * 8) {
			return new RequestRecipeShopListSet(java.util.List.of());
		}
		java.util.List<ManufactureRecipeRequest> items = new java.util.ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			items.add(new ManufactureRecipeRequest(r.readD(), r.readD()));
		}
		return new RequestRecipeShopListSet(items);
	}

	private static RequestBuyProcure readBuyProcure(PacketReader r) {
		if (r.remaining() < 8) {
			return new RequestBuyProcure(0, java.util.List.of(), java.util.List.of());
		}
		int listId = r.readD();
		int count = r.readD();
		if (count <= 0 || count > 100 || r.remaining() < count * 12) {
			return new RequestBuyProcure(listId, java.util.List.of(), java.util.List.of());
		}
		java.util.List<ItemRequest> items = new java.util.ArrayList<>(count);
		java.util.List<ProcureCropRequest> procureItems = new java.util.ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			int objId = r.readD();
			int itemId = r.readD();
			int cnt = r.readD();
			items.add(new ItemRequest(itemId, cnt));
			procureItems.add(new ProcureCropRequest(objId, itemId, listId, cnt));
		}
		return new RequestBuyProcure(listId, items, procureItems);
	}

	private static RequestBuySeed readBuySeed(PacketReader r) {
		if (r.remaining() < 8) {
			return new RequestBuySeed(0, java.util.List.of());
		}
		int manorId = r.readD();
		int count = r.readD();
		if (count <= 0 || count > 100 || r.remaining() < count * 8) {
			return new RequestBuySeed(manorId, java.util.List.of());
		}
		java.util.List<ItemRequest> seeds = new java.util.ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			seeds.add(new ItemRequest(r.readD(), r.readD()));
		}
		return new RequestBuySeed(manorId, seeds);
	}

	private static RequestPreviewItem readPreviewItem(PacketReader r) {
		if (r.remaining() < 12) {
			return new RequestPreviewItem(0, 0, java.util.List.of());
		}
		int unk = r.readD();
		int listId = r.readD();
		int count = r.readD();
		if (count <= 0 || count > 100 || r.remaining() < count * 4) {
			return new RequestPreviewItem(unk, listId, java.util.List.of());
		}
		java.util.List<Integer> items = new java.util.ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			items.add(r.readD());
		}
		return new RequestPreviewItem(unk, listId, items);
	}

	private static RequestProcureCropList readProcureCropList(PacketReader r) {
		if (r.remaining() < 4) {
			return new RequestProcureCropList(java.util.List.of());
		}
		int count = r.readD();
		if (count <= 0 || count > 100 || r.remaining() < count * 16) {
			return new RequestProcureCropList(java.util.List.of());
		}
		java.util.List<ProcureCropRequest> items = new java.util.ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			items.add(new ProcureCropRequest(r.readD(), r.readD(), r.readD(), r.readD()));
		}
		return new RequestProcureCropList(items);
	}

	private static RequestSetSeed readSetSeed(PacketReader r) {
		if (r.remaining() < 8) {
			return new RequestSetSeed(0, java.util.List.of());
		}
		int manorId = r.readD();
		int count = r.readD();
		if (count <= 0 || count > 500 || r.remaining() < count * 12) {
			return new RequestSetSeed(manorId, java.util.List.of());
		}
		java.util.List<SeedSettingRequest> seeds = new java.util.ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			seeds.add(new SeedSettingRequest(r.readD(), r.readD(), r.readD()));
		}
		return new RequestSetSeed(manorId, seeds);
	}

	private static RequestSetCrop readSetCrop(PacketReader r) {
		if (r.remaining() < 8) {
			return new RequestSetCrop(0, java.util.List.of());
		}
		int manorId = r.readD();
		int count = r.readD();
		if (count <= 0 || count > 500 || r.remaining() < count * 13) {
			return new RequestSetCrop(manorId, java.util.List.of());
		}
		java.util.List<CropSettingRequest> crops = new java.util.ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			crops.add(new CropSettingRequest(r.readD(), r.readD(), r.readD(), r.readC()));
		}
		return new RequestSetCrop(manorId, crops);
	}

	private static RequestPledgeReorganizeMember readPledgeReorganizeMember(PacketReader r) {
		if (r.remaining() < 4) {
			return new RequestPledgeReorganizeMember(0, "", 0, "");
		}
		int isMemberSelected = r.readD();
		String memberName = r.remaining() >= 2 ? r.readS() : "";
		int newPledgeType = r.remaining() >= 4 ? r.readD() : 0;
		String selectedMember = r.remaining() >= 2 ? r.readS() : "";
		return new RequestPledgeReorganizeMember(isMemberSelected, memberName, newPledgeType, selectedMember);
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
