package com.lopez.l2j.network.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.item.ItemTemplateTable;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.ActionFailed;
import com.lopez.l2j.network.game.packet.GameServerPacket.Attack;
import com.lopez.l2j.network.game.packet.GameServerPacket.ChangeWaitType;
import com.lopez.l2j.network.game.packet.GameServerPacket.MyTargetSelected;
import com.lopez.l2j.network.game.packet.GameServerPacket.Revive;
import com.lopez.l2j.network.game.packet.GameServerPacket.SetupGauge;
import com.lopez.l2j.network.game.packet.GameServerPacket.ShowMiniMap;
import com.lopez.l2j.network.game.packet.GameServerPacket.SocialAction;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import com.lopez.l2j.network.game.packet.GameServerPacket.TeleportToLocation;
import com.lopez.l2j.network.game.packet.GameServerPacket.ChooseInventoryItem;
import com.lopez.l2j.network.game.packet.GameServerPacket.EnchantResult;
import com.lopez.l2j.network.game.packet.GameServerPacket.WareHouseDepositList;
import com.lopez.l2j.network.game.packet.GameServerPacket.WareHouseWithdrawalList;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameSessionFeaturesTest {

	private List<GameServerPacket> sent;
	private GameSession session;
	private PlayerCharacter player;
	private GameWorld world;
	private ItemTemplateTable itemTemplates;
	private InventoryService inventoryService;
	private NpcInstance monster;

	@BeforeEach
	void setUp() {
		sent = new ArrayList<>();
		world = new GameWorld();
		var repo = new com.lopez.l2j.game.item.InMemoryItemRepository();
		inventoryService = new InventoryService(com.lopez.l2j.game.item.TestItems.table(), repo,
				com.lopez.l2j.game.model.ObjectIdFactory.sequential(0x20000000), 10_000_000);
		var charTemplates = new CharTemplateTable();
		var charService = new com.lopez.l2j.game.service.CharacterService(null, charTemplates, inventoryService);
		var combat = new CombatService();

		var htmls = new com.lopez.l2j.game.html.HtmCache("data/html");
		var multisell = new com.lopez.l2j.game.multisell.MultiSellTable("data/xml/multisell");
		var warehouse = new com.lopez.l2j.game.service.WarehouseService(repo, com.lopez.l2j.game.item.TestItems.table(),
				com.lopez.l2j.game.model.ObjectIdFactory.sequential(0x30000000));
		var ctx = new GameSession.Context(746, 746, null, charService, inventoryService, world, htmls,
				null, null, combat, null, null, null, null, null, multisell, warehouse, null, "TestServer");

		player = new PlayerCharacter(1001, "Archer", "Hero", 1, 0, 0, 0, 0, 0, false, 0, 0, 0,
				200, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 200.0, 100.0, 100.0);
		player.inventory(new Inventory(player.objectId()));
		player.moveTo(0, 0, 0);

		session = new GameSession(ctx, new byte[8], "127.0.0.1", sent::add);
		setField(session, "state", GameClientPacket.State.IN_GAME);
		setField(session, "active", player);
		setField(session, "inWorld", true);
		world.add(session);

		var monsterTemplate = new NpcTemplate(20001, 20001, "Gremlin", false, "", false, 10.0, 15.0, 1, "male",
				"L2Monster", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		monster = new NpcInstance(30001, monsterTemplate, 30, 0, 0, 0);
		world.addNpc(monster);

		var whTemplate = new NpcTemplate(30005, 30005, "Wilford", false, "", false, 10.0, 15.0, 1, "male",
				"L2Warehouse", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
		world.addNpc(new NpcInstance(30005, whTemplate, 0, 0, 0, 0));
	}

	private static void setField(Object target, String name, Object val) {
		try {
			var f = target.getClass().getDeclaredField(name);
			f.setAccessible(true);
			f.set(target, val);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void bowRequiresArrowsAndFailsWhenMissing() {
		// Equip Bow (Bow: id 14, subType bow, crystalType none)
		var bowInst = inventoryService.addItem(player.inventory(), 14, 1, "Test").item();
		inventoryService.toggleEquip(player.inventory(), bowInst.objectId());

		// Tenta atacar monstro sem flechas
		invokeMethod(session, "onAttackNpc", new Class<?>[] { NpcInstance.class }, monster);

		// Deve conter mensagem SystemMessage 726 (NOT_ENOUGH_ARROWS) e ActionFailed
		boolean hasNotEnoughArrows = sent.stream().anyMatch(p -> p instanceof SystemMessage sm && sm.id() == SystemMessage.NOT_ENOUGH_ARROWS);
		assertTrue(hasNotEnoughArrows, "Deve recusar o ataque por falta de flechas");
	}

	@Test
	void bowConsumesArrowWhenAttacking() {
		// Equip Bow (Bow id 14)
		var bowInst = inventoryService.addItem(player.inventory(), 14, 1, "Test").item();
		inventoryService.toggleEquip(player.inventory(), bowInst.objectId());

		// Adiciona 10 Wooden Arrows (id 17)
		inventoryService.addItem(player.inventory(), 17, 10, "Test");
		assertEquals(10, player.inventory().byItemId(17).orElseThrow().count());

		// Ataca o monstro
		invokeMethod(session, "onAttackNpc", new Class<?>[] { NpcInstance.class }, monster);

		// Deve ter consumido 1 flecha
		assertEquals(9, player.inventory().byItemId(17).orElseThrow().count());

		// E deve ter enviado o pacote Attack (animacao)
		assertTrue(sent.stream().anyMatch(p -> p instanceof Attack), "Deve enviar o pacote Attack");
	}

	@Test
	void attackCooldownRejectsSpamClicks() {
		// Adiciona adaga (Dagger id 10)
		var dagger = inventoryService.addItem(player.inventory(), 10, 1, "Test").item();
		inventoryService.toggleEquip(player.inventory(), dagger.objectId());

		// 1º ataque: aceito e dispara Attack
		invokeMethod(session, "onAttackNpc", new Class<?>[] { NpcInstance.class }, monster);
		int attacksCount1 = (int) sent.stream().filter(p -> p instanceof Attack).count();
		assertEquals(1, attacksCount1);

		// 2º ataque imediato: dentro do cooldown do swing, deve falhar (ActionFailed) e NAO gerar novo Attack
		invokeMethod(session, "onAttackNpc", new Class<?>[] { NpcInstance.class }, monster);
		int attacksCount2 = (int) sent.stream().filter(p -> p instanceof Attack).count();
		assertEquals(1, attacksCount2, "Spam clicks nao devem disparar novo Attack dentro do cooldown");
		assertTrue(sent.stream().anyMatch(p -> p instanceof ActionFailed), "Deve responder ActionFailed no spam click");
	}

	@Test
	void actionTwoExecutesAttack() {
		// Seleciona monstro como alvo
		setField(session, "targetObjectId", monster.objectId());

		// Dispara Action 2 (Attack)
		session.handle(new byte[] { 0x45, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 }); // RequestActionUse(2, false, false)

		// Deve ter iniciado o ataque
		assertTrue(sent.stream().anyMatch(p -> p instanceof Attack), "Acao 2 deve iniciar o ataque ao alvo selecionado");
	}

	@Test
	void scrollOfEscapeShowsCastGaugeAndConsumesScroll() {
		// Adiciona Scroll of Escape (id 736)
		var soe = inventoryService.addItem(player.inventory(), 736, 1, "Test").item();

		// Usa o item
		session.handle(new byte[] { 0x14, (byte) soe.objectId(), (byte) (soe.objectId() >> 8), (byte) (soe.objectId() >> 16), (byte) (soe.objectId() >> 24) });

		// Deve ter consumido o scroll
		assertTrue(player.inventory().byItemId(736).isEmpty(), "Scroll deve ter sido consumido");

		// Deve ter enviado SetupGauge da barra de conjuracao
		assertTrue(sent.stream().anyMatch(p -> p instanceof SetupGauge), "Deve exibir a barra de conjuracao");
	}

	@Test
	void socialActionSendsSocialPacket() {
		// Envia opcode 0x1b com actionId 8 (Danca)
		session.handle(new byte[] { 0x1b, 0x08, 0x00, 0x00, 0x00 });

		assertTrue(sent.stream().anyMatch(p -> p instanceof SocialAction sa && sa.actionId() == 8),
				"Deve emitir pacote SocialAction correspondente a acao social clicada");
	}

	@Test
	void sitStandTogglesSittingAndSendsChangeWaitType() {
		assertFalse(player.sitting());

		// Envia Action 0 (Sit)
		session.handle(new byte[] { 0x45, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 });
		assertTrue(player.sitting(), "Personagem deve estar sentado");
		assertTrue(sent.stream().anyMatch(p -> p instanceof ChangeWaitType cwt && cwt.type() == 0),
				"Deve enviar ChangeWaitType tipo 0 (sentado)");

		// Envia Action 0 (Stand)
		session.handle(new byte[] { 0x45, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 });
		assertFalse(player.sitting(), "Personagem deve estar em pe");
		assertTrue(sent.stream().anyMatch(p -> p instanceof ChangeWaitType cwt && cwt.type() == 1),
				"Deve enviar ChangeWaitType tipo 1 (em pe)");
	}

	@Test
	void nextTargetSelectsNearestAttackableMob() {
		// Dispara Action 4 (Target Next)
		session.handle(new byte[] { 0x45, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 });

		assertTrue(sent.stream().anyMatch(p -> p instanceof MyTargetSelected mts && mts.objectId() == monster.objectId()),
				"Target Next deve selecionar o monstro mais proximo");
	}

	@Test
	void altMRequestShowMiniMapSendsShowMiniMap() {
		// Envia opcode 0xcd (RequestShowMiniMap - disparado ao apertar Alt+M ou clicar no icone de mapa)
		session.handle(new byte[] { (byte) 0xcd });

		assertTrue(sent.stream().anyMatch(p -> p instanceof ShowMiniMap),
				"Pressionar Alt+M ou clicar no mapa deve enviar pacote ShowMiniMap (0x9d) para abrir a janela de mapa mundi");
	}

	@Test
	void restartPointRevivesPlayerAndTeleportsToTown() {
		// Mata o player (HP zerado)
		player.currentHp(0.0);
		assertTrue(player.isDead());

		// Envia opcode 0x6d (RequestRestartPoint, tipo 0 = To Village)
		session.handle(new byte[] { 0x6d, 0x00, 0x00, 0x00, 0x00 });

		assertFalse(player.isDead(), "Player deve estar vivo apos clicar To Village");
		assertTrue(player.currentHp() > 0, "HP deve ser restaurado");
		assertTrue(sent.stream().anyMatch(p -> p instanceof Revive), "Deve enviar pacote Revive");
		assertTrue(sent.stream().anyMatch(p -> p instanceof TeleportToLocation), "Deve enviar TeleportToLocation para a vila");
	}

	@Test
	void userCommandLocSendsCoordinates() {
		// Envia opcode 0xaa (RequestUserCommand, id = 0 -> /loc)
		session.handle(new byte[] { (byte) 0xaa, 0x00, 0x00, 0x00, 0x00 });

		assertTrue(sent.stream().anyMatch(p -> p instanceof GameServerPacket.CreatureSay cs && cs.text().contains("Location:")),
				"Comando /loc deve enviar as coordenadas atuais no chat");
	}

	@Test
	void userCommandUnstuckStartsCastAndGauge() {
		// Envia opcode 0xaa (RequestUserCommand, id = 52 -> /unstuck)
		session.handle(new byte[] { (byte) 0xaa, 0x34, 0x00, 0x00, 0x00 });

		assertTrue(sent.stream().anyMatch(p -> p instanceof SetupGauge),
				"Comando /unstuck deve enviar barra de progresso (SetupGauge)");
		assertTrue(sent.stream().anyMatch(p -> p instanceof GameServerPacket.MagicSkillUse msu && msu.skillId() == 2099),
				"Comando /unstuck deve disparar o skill 2099 de fuga");
	}

	@Test
	void chatSlashCommandTargetSelectsNearestMob() {
		// Envia Say2 com "/target Gremlin"
		invokeMethod(session, "onSay", new Class<?>[] { GameClientPacket.Say2.class },
				new GameClientPacket.Say2("/target Gremlin", 0, null));

		assertTrue(sent.stream().anyMatch(p -> p instanceof MyTargetSelected mts && mts.objectId() == monster.objectId()),
				"Comando /target deve selecionar o mob mais proximo pelo nome");
	}

	@Test
	void bypassWithHyphenHPrefixIsProperlyHandled() {
		// Cliente envia bypass com prefixo retail "-h npc_%objectId%_Chat 1"
		invokeMethod(session, "onBypass", new Class<?>[] { GameClientPacket.RequestBypassToServer.class },
				new GameClientPacket.RequestBypassToServer("-h npc_30001_Chat 1"));

		// Deve responder com NpcHtmlMessage em vez de falhar com ActionFailed
		assertTrue(sent.stream().anyMatch(p -> p instanceof GameServerPacket.NpcHtmlMessage),
				"Bypass com prefixo retail -h deve abrir o dialogo do NPC com sucesso");
	}

	@Test
	void chatDotOnlineCommandReturnsOnlineCount() {
		invokeMethod(session, "onSay", new Class<?>[] { GameClientPacket.Say2.class },
				new GameClientPacket.Say2(".online", 0, null));

		assertTrue(sent.stream().anyMatch(p -> p instanceof GameServerPacket.CreatureSay cs && cs.text().contains("Jogadores online:")),
				"Comando .online deve informar a quantidade de jogadores");
	}

	@Test
	void spiritshotBoostsMagicDamage() {
		var charTemplates = new CharTemplateTable();
		var template = charTemplates.get(player.classId()).orElseThrow();
		var combat = new CombatService();
		// Roda ate pegar hits sem critico magico (flag 0x20) para comparar o multiplicador base dos shots
		CombatService.HitResult normalHit;
		do {
			monster.currentHp(1000.0);
			monster.dead(false);
			normalHit = combat.skillMagicNpc(player, template, monster, 40.0, false, false);
		} while ((normalHit.flags() & 0x20) != 0);

		CombatService.HitResult spsHit;
		do {
			monster.currentHp(1000.0);
			monster.dead(false);
			spsHit = combat.skillMagicNpc(player, template, monster, 40.0, true, false);
		} while ((spsHit.flags() & 0x20) != 0);

		CombatService.HitResult bssHit;
		do {
			monster.currentHp(1000.0);
			monster.dead(false);
			bssHit = combat.skillMagicNpc(player, template, monster, 40.0, false, true);
		} while ((bssHit.flags() & 0x20) != 0);

		assertTrue(spsHit.damage() > normalHit.damage(), "Spiritshot normal deve causar mais dano magico que sem shot");
		assertTrue(bssHit.damage() > spsHit.damage(), "Blessed Spiritshot deve causar mais dano magico que Spiritshot normal");
	}

	@Test
	void chargeSpiritShotConsumesItemAndSetsCharged() {
		// Adiciona Spiritshot No-Grade (item 2509) e equipa arma No-Grade (item 14)
		var weapon = inventoryService.addItem(player.inventory(), 14, 1, "Test").item();
		inventoryService.toggleEquip(player.inventory(), weapon.objectId());
		inventoryService.addItem(player.inventory(), 2509, 10, "Test");

		// Usa o consumivel Spiritshot No-Grade
		var shot = com.lopez.l2j.game.effect.ConsumableTable.get(2509).orElseThrow();
		invokeMethod(session, "useConsumable", new Class<?>[] { com.lopez.l2j.game.effect.ConsumableTable.Consumable.class }, shot);

		// Verifica que consumiu 1 unidade e enviou ENABLED_SOULSHOT
		assertEquals(9, player.inventory().byItemId(2509).orElseThrow().count(), "Deve ter consumido 1 spiritshot");
		assertTrue(sent.stream().anyMatch(p -> p instanceof SystemMessage sm && sm.id() == SystemMessage.ENABLED_SOULSHOT),
				"Deve notificar que o shot foi ativado na arma");
	}

	@Test
	void multiSellShowsWindowAndExchangesItems() {
		// Adiciona 10 Dimension Diamonds (item 7562)
		inventoryService.addItem(player.inventory(), 7562, 10, "Test");

		// Abre o multisell 002 via bypass do NPC
		invokeMethod(session, "onBypass", new Class<?>[] { GameClientPacket.RequestBypassToServer.class },
				new GameClientPacket.RequestBypassToServer("npc_30001_multisell 002"));

		assertTrue(sent.stream().anyMatch(p -> p instanceof GameServerPacket.MultiSellList msl && msl.listId() == 2),
				"Deve enviar o pacote MultiSellList com id 2");

		// Executa troca do entry 1 no multisell 2 (5 Dimension Diamonds -> 1 SoE Talking Island 7117)
		session.handle(new byte[] {
				(byte) 0xa7,
				0x02, 0x00, 0x00, 0x00, // listId = 2
				0x01, 0x00, 0x00, 0x00, // entryId = 1
				0x01, 0x00, 0x00, 0x00  // amount = 1
		});

		assertEquals(5, player.inventory().byItemId(7562).orElseThrow().count(), "Deve ter consumido 5 Dimension Diamonds");
		assertEquals(1, player.inventory().byItemId(7117).orElseThrow().count(), "Deve ter entregue 1 SoE Talking Island");
	}

	@Test
	void htmCacheResolvesAcrossAllDatapackFolders() {
		var htmls = new com.lopez.l2j.game.html.HtmCache("data/html");

		// NPC 30005 (Warehouse Keeper Wilford) com tipo generico "L2Npc"
		String html = htmls.getNpcHtml(30005, "L2Npc", 0);
		assertNotNull(html);
		assertTrue(html.contains("Warehouse Keeper Wilford"), "Deve encontrar o arquivo retail em warehouse/30005.htm mesmo com tipo L2Npc");
	}

	@Test
	void testEnchantScrollOpensChooseInventoryItemAndEnchantsWeapon() {
		// Adiciona D-Sword (2499) e Scroll: Enchant Weapon D (955)
		var sword = inventoryService.addItem(player.inventory(), 2499, 1, "Test").item();
		var scroll = inventoryService.addItem(player.inventory(), 955, 1, "Test").item();

		sent.clear();
		// Usa o scroll (0x14 UseItem)
		byte[] useItem = ByteBuffer.allocate(9).order(ByteOrder.LITTLE_ENDIAN)
				.put((byte) 0x14)
				.putInt(scroll.objectId())
				.putInt(0)
				.array();
		session.handle(useItem);

		// Deve enviar ChooseInventoryItem(955)
		boolean hasChooseItem = sent.stream().anyMatch(p -> p instanceof ChooseInventoryItem ci && ci.itemId() == 955);
		assertTrue(hasChooseItem, "Deve enviar ChooseInventoryItem para abrir janela de enchant");

		sent.clear();
		// Envia RequestEnchantItem (0x58) selecionando a espada
		byte[] reqEnchant = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN)
				.put((byte) 0x58)
				.putInt(sword.objectId())
				.array();
		session.handle(reqEnchant);

		// Seguro ate +3: sucesso garantido
		boolean hasEnchantSuccess = sent.stream().anyMatch(p -> p instanceof EnchantResult er && er.result() == EnchantResult.RES_SUCCESS);
		assertTrue(hasEnchantSuccess, "Deve aplicar sucesso no encantamento seguro (+1)");
		assertEquals(1, sword.enchant(), "Espada deve ter nivel de encanto 1");
	}

	@Test
	void testEnchantScrollGradeMismatchRejected() {
		var sword = inventoryService.addItem(player.inventory(), 2499, 1, "Test").item();
		var armorScroll = inventoryService.addItem(player.inventory(), 956, 1, "Test").item(); // Scroll Armor D

		// Usa o scroll de armor
		byte[] useItem = ByteBuffer.allocate(9).order(ByteOrder.LITTLE_ENDIAN)
				.put((byte) 0x14)
				.putInt(armorScroll.objectId())
				.putInt(0)
				.array();
		session.handle(useItem);

		sent.clear();
		// Tenta encantar uma arma com scroll de armadura
		byte[] reqEnchant = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN)
				.put((byte) 0x58)
				.putInt(sword.objectId())
				.array();
		session.handle(reqEnchant);

		boolean hasMismatch = sent.stream().anyMatch(p -> p instanceof SystemMessage sm && sm.id() == SystemMessage.INAPPROPRIATE_ENCHANT_CONDITION);
		assertTrue(hasMismatch, "Deve rejeitar encantamento incompativel (armor scroll em arma)");
		assertEquals(0, sword.enchant(), "Espada nao deve ser modificada");
	}

	@Test
	void testEnchantScrollBlessedResetsToZeroOnFailure() {
		var sword = inventoryService.addItem(player.inventory(), 2499, 1, "Test").item();
		sword.enchant(10); // Ja esta em +10 (alem do limite seguro)

		boolean failed = false;
		for (int i = 0; i < 50; i++) {
			var blessedScroll = inventoryService.addItem(player.inventory(), 6575, 1, "Test").item();
			byte[] useItem = ByteBuffer.allocate(9).order(ByteOrder.LITTLE_ENDIAN)
					.put((byte) 0x14)
					.putInt(blessedScroll.objectId())
					.putInt(0)
					.array();
			session.handle(useItem);

			sent.clear();
			byte[] reqEnchant = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN)
					.put((byte) 0x58)
					.putInt(sword.objectId())
					.array();
			session.handle(reqEnchant);

			boolean hasBlessedFail = sent.stream().anyMatch(p -> p instanceof EnchantResult er && er.result() == EnchantResult.RES_BLESSED_FAIL);
			if (hasBlessedFail) {
				failed = true;
				break;
			}
		}

		assertTrue(failed, "Com chance de 66%, apos multiplas tentativas deve ocorrer falha blessed");
		assertEquals(0, sword.enchant(), "Item blessed que falhou deve ter o enchant resetado para 0");
		assertTrue(player.inventory().byObjectId(sword.objectId()).isPresent(), "Item blessed nao deve quebrar/ser deletado ao falhar");
	}

	@Test
	void testWarehouseDepositAndWithdraw() {
		// Adiciona 10.000 Adena e uma espada
		inventoryService.addItem(player.inventory(), 57, 10_000, "Test");
		var sword = inventoryService.addItem(player.inventory(), 2499, 1, "Test").item();

		sent.clear();
		// Clica em Deposit (bypass npc_30005_DepositP)
		byte[] bypassDeposit = buildBypassPacket("npc_30005_DepositP");
		session.handle(bypassDeposit);

		// Recebe WareHouseDepositList contendo a espada
		boolean hasDepositList = sent.stream().anyMatch(p -> p instanceof WareHouseDepositList dl && dl.items().stream().anyMatch(i -> i.objectId() == sword.objectId()));
		assertTrue(hasDepositList, "Deve listar a espada como item disponivel para deposito");

		sent.clear();
		// Deposita a espada (SendWareHouseDepositList 0x31)
		byte[] depositPacket = ByteBuffer.allocate(13).order(ByteOrder.LITTLE_ENDIAN)
				.put((byte) 0x31)
				.putInt(1) // 1 item
				.putInt(sword.objectId())
				.putInt(1) // count
				.array();
		session.handle(depositPacket);

		// Espada nao deve mais estar no inventario do player
		assertFalse(player.inventory().byObjectId(sword.objectId()).isPresent(), "Espada deve ter saido do inventario do jogador");
		// Taxa de 30 adena descontada
		assertEquals(9_970, player.inventory().adena(), "Deve descontar taxa de 30 adena no deposito");

		sent.clear();
		// Clica em Withdraw (bypass npc_30005_WithdrawP)
		byte[] bypassWithdraw = buildBypassPacket("npc_30005_WithdrawP");
		session.handle(bypassWithdraw);

		boolean hasWithdrawList = sent.stream().anyMatch(p -> p instanceof WareHouseWithdrawalList wl && wl.items().stream().anyMatch(i -> i.objectId() == sword.objectId()));
		assertTrue(hasWithdrawList, "Deve listar a espada guardada no armazem");

		sent.clear();
		// Retira a espada (SendWareHouseWithDrawList 0x32)
		byte[] withdrawPacket = ByteBuffer.allocate(13).order(ByteOrder.LITTLE_ENDIAN)
				.put((byte) 0x32)
				.putInt(1) // 1 item
				.putInt(sword.objectId())
				.putInt(1) // count
				.array();
		session.handle(withdrawPacket);

		// Espada voltou para o inventario
		assertTrue(player.inventory().byObjectId(sword.objectId()).isPresent(), "Espada deve ter voltado para o inventario");
	}

	@Test
	void testDestroyItem() {
		var potions = inventoryService.addItem(player.inventory(), 1060, 100, "Test").item();

		// Destroi 40 pocoes (RequestDestroyItem 0x59)
		byte[] destroyPacket = ByteBuffer.allocate(9).order(ByteOrder.LITTLE_ENDIAN)
				.put((byte) 0x59)
				.putInt(potions.objectId())
				.putInt(40)
				.array();
		session.handle(destroyPacket);

		assertEquals(60, player.inventory().byObjectId(potions.objectId()).orElseThrow().count(), "Deve restar 60 pocoes no inventario");
	}

	private static byte[] buildBypassPacket(String command) {
		byte[] cmdBytes = command.getBytes(java.nio.charset.StandardCharsets.UTF_16LE);
		ByteBuffer buf = ByteBuffer.allocate(1 + cmdBytes.length + 2).order(ByteOrder.LITTLE_ENDIAN);
		buf.put((byte) 0x21); // RequestBypassToServer
		buf.put(cmdBytes);
		buf.putShort((short) 0);
		return buf.array();
	}

	private static void invokeMethod(Object target, String name, Class<?>[] paramTypes, Object... args) {
		try {
			var m = target.getClass().getDeclaredMethod(name, paramTypes);
			m.setAccessible(true);
			m.invoke(target, args);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
