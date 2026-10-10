package com.lopez.l2j.network.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.config.Config;
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
import com.lopez.l2j.network.game.packet.GameServerPacket.Die;
import com.lopez.l2j.network.game.packet.GameServerPacket.MagicEffectIcons;
import com.lopez.l2j.network.game.packet.GameServerPacket.MyTargetSelected;
import com.lopez.l2j.network.game.packet.GameServerPacket.Revive;
import com.lopez.l2j.network.game.packet.GameServerPacket.SetupGauge;
import com.lopez.l2j.network.game.packet.GameServerPacket.ShowMiniMap;
import com.lopez.l2j.network.game.packet.GameServerPacket.ExGetBossRecord;
import com.lopez.l2j.network.game.packet.GameServerPacket.SocialAction;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import com.lopez.l2j.network.game.packet.GameServerPacket.TeleportToLocation;
import com.lopez.l2j.network.game.packet.GameServerPacket.ChooseInventoryItem;
import com.lopez.l2j.network.game.packet.GameServerPacket.EnchantResult;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage;
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
	private GameSession.Context ctx;
	private FakeCharacterSkillSaveRepository buffRepo;
	private PlayerCharacter player;
	private GameWorld world;
	private ItemTemplateTable itemTemplates;
	private InventoryService inventoryService;
	private NpcInstance monster;

	@BeforeEach
	void setUp() {
		Config.LEAVE_BUFFS_ON_DIE = true;
		sent = new ArrayList<>();
		world = new GameWorld();
		var repo = new com.lopez.l2j.game.item.InMemoryItemRepository();
		inventoryService = new InventoryService(com.lopez.l2j.game.item.TestItems.table(), repo,
				com.lopez.l2j.game.model.ObjectIdFactory.sequential(0x20000000), 10_000_000);
		var charTemplates = new CharTemplateTable();
		var charRepo = new com.lopez.l2j.game.model.InMemoryCharacterRepository();
		var charService = new com.lopez.l2j.game.service.CharacterService(charRepo, charTemplates, inventoryService);
		var combat = new CombatService();

		var htmls = new com.lopez.l2j.game.html.HtmCache("data/html");
		var multisell = new com.lopez.l2j.game.multisell.MultiSellTable("data/xml/multisell");
		var warehouse = new com.lopez.l2j.game.service.WarehouseService(repo, com.lopez.l2j.game.item.TestItems.table(),
				com.lopez.l2j.game.model.ObjectIdFactory.sequential(0x30000000));
		buffRepo = new FakeCharacterSkillSaveRepository();
		ctx = new GameSession.Context(746, 746, null, charService, inventoryService, world, htmls,
				null, null, combat, null, null, null, null, null, multisell, warehouse, buffRepo, null, "TestServer");

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

	private static Object getField(Object target, String name) {
		try {
			var f = target.getClass().getDeclaredField(name);
			f.setAccessible(true);
			return f.get(target);
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

		// 2º ataque imediato: dentro do cooldown do swing, nao deve disparar novo Attack nem enviar ActionFailed (para nao abortar a animacao no cliente)
		invokeMethod(session, "onAttackNpc", new Class<?>[] { NpcInstance.class }, monster);
		int attacksCount2 = (int) sent.stream().filter(p -> p instanceof Attack).count();
		assertEquals(1, attacksCount2, "Spam clicks nao devem disparar novo Attack dentro do cooldown");
		assertFalse(sent.stream().anyMatch(p -> p instanceof ActionFailed), "Nao deve responder ActionFailed no spam click para evitar cancelamento de animacao");
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
		assertTrue(sent.stream().anyMatch(p -> p instanceof ExGetBossRecord),
				"Pressionar Alt+M deve enviar ExGetBossRecord para inicializar a aba de raids");
	}

	@Test
	void requestGetBossRecordSendsExGetBossRecord() {
		// Envia opcode 0xd0, sub 0x0018 (RequestGetBossRecord - disparado ao abrir a aba/janela de raids no mapa)
		session.handle(new byte[] { (byte) 0xd0, 0x18, 0x00, 0x00, 0x00, 0x00, 0x00 });

		assertTrue(sent.stream().anyMatch(p -> p instanceof ExGetBossRecord),
				"Clicar em Raid Info ou consultar raids deve responder com ExGetBossRecord (0xfe:0x33)");
	}

	@Test
	void restartPointRevivesPlayerAndTeleportsToTown() {
		// Mata o player (HP zerado)
		player.currentHp(0.0);
		assertTrue(player.isDead());

		// Envia opcode 0x6d (RequestRestartPoint, tipo 0 = To Village)
		session.handle(new byte[] { 0x6d, 0x00, 0x00, 0x00, 0x00 });
		session.onAppearing();

		assertFalse(player.isDead(), "Player deve estar vivo apos clicar To Village");
		assertTrue(player.currentHp() > 0, "HP deve ser restaurado");
		assertTrue(sent.stream().anyMatch(p -> p instanceof Revive), "Deve enviar pacote Revive");
		assertTrue(sent.stream().anyMatch(p -> p instanceof TeleportToLocation), "Deve enviar TeleportToLocation para a vila");
	}

	@Test
	void playerDeathSendsDiePacketWithToVillageButton() {
		session.handlePlayerDeath(null);

		assertTrue(player.isDead(), "Player deve estar morto apos //kill");
		var dieOpt = sent.stream()
				.filter(p -> p instanceof Die)
				.map(p -> (Die) p)
				.findFirst();
		assertTrue(dieOpt.isPresent(), "Deve enviar pacote Die quando o player morre");
		assertTrue(dieOpt.get().toVillage(), "O pacote Die deve ter toVillage=true para exibir o botao To Nearest Village");
	}

	@Test
	void playerDeathClearsBuffsWhenNoNoblesse() {
		player.effects().addBuff(1068, 1, 1_200_000L); // Might
		assertFalse(player.effects().active().isEmpty(), "Player deve ter buff ativo antes de morrer");

		session.handlePlayerDeath(null);

		assertTrue(player.isDead());
		assertTrue(player.effects().active().isEmpty(), "Buffs devem ser limpos ao morrer sem Noblesse");
		assertTrue(sent.stream().anyMatch(p -> p instanceof MagicEffectIcons mei && mei.icons().isEmpty()),
				"Deve enviar MagicEffectIcons vazio ao morrer");
	}

	@Test
	void playerDeathPreservesBuffsWithNoblesseBlessing() {
		player.effects().addBuff(1068, 1, 1_200_000L); // Might
		player.effects().addBuff(1323, 1, 1_200_000L); // Blessing of Noblesse
		assertEquals(2, player.effects().active().size());

		session.handlePlayerDeath(null);

		assertTrue(player.isDead());
		assertFalse(player.effects().hasSkill(1323), "Noblesse Blessing deve ser consumido na morte");
		assertTrue(player.effects().hasSkill(1068), "Might deve ser preservado pela Noblesse Blessing");
	}

	@Test
	void userCommandLocSendsCoordinates() {
		// Envia opcode 0xaa (RequestUserCommand, id = 0 -> /loc)
		session.handle(new byte[] { (byte) 0xaa, 0x00, 0x00, 0x00, 0x00 });

		assertTrue(sent.stream().anyMatch(p -> (p instanceof GameServerPacket.CreatureSay cs && cs.text().contains("Location:"))
				|| (p instanceof GameServerPacket.SystemMessage sm && sm.text().contains("Location:"))),
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

		assertTrue(sent.stream().anyMatch(p -> (p instanceof GameServerPacket.CreatureSay cs && cs.text().contains("Jogadores online:"))
				|| (p instanceof GameServerPacket.SystemMessage sm && sm.text().contains("Jogadores online:"))),
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

	@Test
	void testBuffsPersistAcrossSessions() {
		// Aplica um buff ativo (ex: Potion de Haste id 2034)
		long now = System.currentTimeMillis();
		player.effects().put(new com.lopez.l2j.game.effect.PlayerEffects.ActiveBuff(
				2034, 1, "haste_potion", now + 60_000L, 0, 1.33, 1.0, 0));
		assertFalse(player.effects().active().isEmpty(), "Player deve ter buff ativo antes de sair");

		// Sai do mundo (simulando restart ou relog)
		invokeMethod(session, "leaveWorld", new Class<?>[0]);

		// Buffs devem ter sido salvos no repositorio
		var saved = buffRepo.restoreBuffs(player.objectId(), 0);
		assertEquals(1, saved.size(), "Deve salvar 1 buff no repositorio");
		assertEquals(2034, saved.getFirst().skillId());

		// Novo login do mesmo personagem apos restart
		var player2 = new PlayerCharacter(1001, "Archer", "Hero", 1, 0, 0, 0, 0, 0, false, 0, 0, 0,
				200, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 200.0, 100.0, 100.0);
		player2.inventory(new Inventory(player2.objectId()));
		assertTrue(player2.effects().active().isEmpty(), "Novo personagem inicia sem efeitos em memoria");

		var session2 = new GameSession(ctx, new byte[8], "127.0.0.1", sent::add);
		setField(session2, "active", player2);
		setField(session2, "inWorld", true);

		// Restaura buffs
		invokeMethod(session2, "restoreBuffs", new Class<?>[0]);

		// Deve ter restaurado o buff em player2.effects()
		assertFalse(player2.effects().active().isEmpty(), "Buffs devem ser restaurados para o player");
		assertTrue(player2.effects().hasSkill(2034), "Buff 2034 deve estar ativo no player");
	}

	@Test
	void testBuffsDeletedOnDeathWhenLeavingWorld() {
		long now = System.currentTimeMillis();
		player.effects().put(new com.lopez.l2j.game.effect.PlayerEffects.ActiveBuff(
				2034, 1, "haste_potion", now + 60_000L, 0, 1.33, 1.0, 0));

		// Marca player como morto
		player.currentHp(0);
		assertTrue(player.isDead());

		// Sai do mundo
		invokeMethod(session, "leaveWorld", new Class<?>[0]);

		// Buffs devem ter sido limpos
		var saved = buffRepo.restoreBuffs(player.objectId(), 0);
		assertTrue(saved.isEmpty(), "Buffs devem ser deletados quando o personagem morre");
	}

	@Test
	void testAdminCommandUnauthorizedForNormalPlayer() {
		player.accessLevel(0);
		assertFalse(player.isGm());

		sent.clear();
		invokeMethod(session, "onSay", new Class<?>[] { GameClientPacket.Say2.class },
				new GameClientPacket.Say2("//admin", 0, null));

		boolean denied = sent.stream().anyMatch(p -> (p instanceof GameServerPacket.CreatureSay cs
				&& cs.text().contains("permissao")) || (p instanceof GameServerPacket.SystemMessage sm && sm.text().contains("permissao")));
		assertTrue(denied, "Comando admin deve ser rejeitado para jogador normal");
	}

	@Test
	void testAdminPanelHtmlViaSayAndBypass() {
		player.accessLevel(100);
		assertTrue(player.isGm());

		// 1. //admin via chat
		sent.clear();
		invokeMethod(session, "onSay", new Class<?>[] { GameClientPacket.Say2.class },
				new GameClientPacket.Say2("//admin", 0, null));
		boolean hasAdminHtml = sent.stream().anyMatch(p -> p instanceof NpcHtmlMessage html
				&& html.npcObjectId() == 0
				&& html.html().contains("Main Control Panel"));
		assertTrue(hasAdminHtml, "Deve abrir o menu admin principal com npcObjectId 0");

		// 2. admin via SendBypassBuildCmd (opcode 0x5b)
		sent.clear();
		invokeMethod(session, "handleAdminCommand", new Class<?>[] { String.class }, "admin");
		boolean hasBuildCmdHtml = sent.stream().anyMatch(p -> p instanceof NpcHtmlMessage html
				&& html.npcObjectId() == 0
				&& html.html().contains("Main Control Panel"));
		assertTrue(hasBuildCmdHtml, "Deve abrir o menu admin principal via SendBypassBuildCmd");

		// 3. admin_gamemenu via bypass
		sent.clear();
		invokeMethod(session, "onBypass", new Class<?>[] { GameClientPacket.RequestBypassToServer.class },
				new GameClientPacket.RequestBypassToServer("admin_gamemenu"));
		boolean hasGameHtml = sent.stream().anyMatch(p -> p instanceof NpcHtmlMessage html
				&& html.npcObjectId() == 0
				&& html.html().contains("Game Control Panel"));
		assertTrue(hasGameHtml, "Deve abrir o menu Game");

		// 4. admin_show_moves (teleports) via bypass
		sent.clear();
		invokeMethod(session, "onBypass", new Class<?>[] { GameClientPacket.RequestBypassToServer.class },
				new GameClientPacket.RequestBypassToServer("admin_show_moves"));
		boolean hasTeleHtml = sent.stream().anyMatch(p -> p instanceof NpcHtmlMessage html
				&& html.npcObjectId() == 0
				&& html.html().contains("Teleport Panel"));
		assertTrue(hasTeleHtml, "Deve abrir o painel de teleports");

		// 5. admin_help tele/towns/aden.htm via bypass
		sent.clear();
		invokeMethod(session, "onBypass", new Class<?>[] { GameClientPacket.RequestBypassToServer.class },
				new GameClientPacket.RequestBypassToServer("admin_help tele/towns/aden.htm"));
		boolean hasAdenTele = sent.stream().anyMatch(p -> p instanceof NpcHtmlMessage html
				&& html.npcObjectId() == 0
				&& html.html().contains("Aden"));
		assertTrue(hasAdenTele, "Deve abrir o submenu de teleport para Aden");
	}

	@Test
	void testDecodeSendBypassBuildCmdPacket() {
		byte[] body = new byte[] {
				0x5b,
				'a', 0, 'd', 0, 'm', 0, 'i', 0, 'n', 0, 0, 0
		};
		var decoded = GameClientPacket.decode(GameClientPacket.State.IN_GAME, body);
		assertTrue(decoded.isPresent());
		assertTrue(decoded.get() instanceof GameClientPacket.SendBypassBuildCmd);
		assertEquals("admin", ((GameClientPacket.SendBypassBuildCmd) decoded.get()).command());
	}

	@Test
	void testAdminCommandsInGame() {
		player.accessLevel(100);

		// Teleport //move_to
		sent.clear();
		invokeMethod(session, "onSay", new Class<?>[] { GameClientPacket.Say2.class },
				new GameClientPacket.Say2("//move_to 1234 5678 -100", 0, null));
		assertEquals(1234, player.x());
		assertEquals(5678, player.y());
		assertEquals(-100, player.z());
		assertTrue(sent.stream().anyMatch(p -> p instanceof TeleportToLocation));

		// Criação de item //item 57 5000
		sent.clear();
		long adenaBefore = player.inventory().adena();
		invokeMethod(session, "onSay", new Class<?>[] { GameClientPacket.Say2.class },
				new GameClientPacket.Say2("//item 57 5000", 0, null));
		assertEquals(adenaBefore + 5000, player.inventory().adena());

		// Cura //heal
		player.currentHp(10.0);
		player.currentMp(5.0);
		player.currentCp(0.0);
		invokeMethod(session, "onSay", new Class<?>[] { GameClientPacket.Say2.class },
				new GameClientPacket.Say2("//heal", 0, null));
		assertEquals(player.maxHp(), player.currentHp());
		assertEquals(player.maxMp(), player.currentMp());
		assertEquals(player.maxCp(), player.currentCp());

		// Alterar nível //setlevel 40
		invokeMethod(session, "onSay", new Class<?>[] { GameClientPacket.Say2.class },
				new GameClientPacket.Say2("//setlevel 40", 0, null));
		assertEquals(40, player.level());

		// GM Speed //speed 4
		invokeMethod(session, "onSay", new Class<?>[] { GameClientPacket.Say2.class },
				new GameClientPacket.Say2("//speed 4", 0, null));
		assertEquals(4, player.gmSpeed());
		assertTrue(player.effects().hasSkill(7029), "Super Haste deve estar ativo no jogador");

		// GM Speed //speed 0
		invokeMethod(session, "onSay", new Class<?>[] { GameClientPacket.Say2.class },
				new GameClientPacket.Say2("//speed 0", 0, null));
		assertEquals(0, player.gmSpeed());
		assertFalse(player.effects().hasSkill(7029), "Super Haste deve ser desativado com //speed 0");

		// Paralysis //para e //unpara
		invokeMethod(session, "onSay", new Class<?>[] { GameClientPacket.Say2.class },
				new GameClientPacket.Say2("//para", 0, null));
		assertTrue(player.isDisabled());
		invokeMethod(session, "onSay", new Class<?>[] { GameClientPacket.Say2.class },
				new GameClientPacket.Say2("//unpara", 0, null));
		assertFalse(player.isDisabled());

		// Invisibilidade //invis e //vis
		invokeMethod(session, "onSay", new Class<?>[] { GameClientPacket.Say2.class },
				new GameClientPacket.Say2("//invis", 0, null));
		assertTrue(player.invis());
		invokeMethod(session, "onSay", new Class<?>[] { GameClientPacket.Say2.class },
				new GameClientPacket.Say2("//vis", 0, null));
		assertFalse(player.invis());

		// Conceder e remover skill //skill e //removeskill
		invokeMethod(session, "onSay", new Class<?>[] { GameClientPacket.Say2.class },
				new GameClientPacket.Say2("//skill 1218 33", 0, null));
		assertEquals(33, player.skillLevel(1218));
		invokeMethod(session, "onSay", new Class<?>[] { GameClientPacket.Say2.class },
				new GameClientPacket.Say2("//removeskill 1218", 0, null));
		assertEquals(0, player.skillLevel(1218));

		// Bypasses de menus e submenus nao devem dar comando desconhecido
		sent.clear();
		invokeMethod(session, "onBypass", new Class<?>[] { GameClientPacket.RequestBypassToServer.class },
				new GameClientPacket.RequestBypassToServer("admin_gmshop"));
		assertFalse(sent.stream().anyMatch(p -> p instanceof GameServerPacket.CreatureSay cs && cs.text().contains("desconhecido")),
				"admin_gmshop nao deve ser desconhecido");

		sent.clear();
		invokeMethod(session, "onBypass", new Class<?>[] { GameClientPacket.RequestBypassToServer.class },
				new GameClientPacket.RequestBypassToServer("admin_enchant"));
		assertFalse(sent.stream().anyMatch(p -> p instanceof GameServerPacket.CreatureSay cs && cs.text().contains("desconhecido")),
				"admin_enchant nao deve ser desconhecido");

		sent.clear();
		invokeMethod(session, "onBypass", new Class<?>[] { GameClientPacket.RequestBypassToServer.class },
				new GameClientPacket.RequestBypassToServer("admin_spawn_menu"));
		assertFalse(sent.stream().anyMatch(p -> p instanceof GameServerPacket.CreatureSay cs && cs.text().contains("desconhecido")),
				"admin_spawn_menu nao deve ser desconhecido");
	}

	@Test
	void testPartyGroupHealAndBuffs() {
		// Cria segundo membro da party
		var player2 = new PlayerCharacter(1002, "Mage", "Partner", 1, 0, 0, 0, 0, 0, false, 0, 0, 0,
				200, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 200.0, 100.0, 100.0);
		player2.inventory(new Inventory(player2.objectId()));
		player2.moveTo(50, 50, 0);

		List<GameServerPacket> sent2 = new ArrayList<>();
		var session2 = new GameSession(ctx, new byte[8], "127.0.0.1", sent2::add);
		setField(session2, "state", GameClientPacket.State.IN_GAME);
		setField(session2, "active", player2);
		setField(session2, "inWorld", true);
		world.add(session2);

		var party = new com.lopez.l2j.game.party.Party(session, session2, 0);
		session.party(party);
		session2.party(party);

		// Reduz HP de ambos os membros
		player.currentHp(50.0);
		player2.currentHp(60.0);

		var groupHeal = new com.lopez.l2j.game.skill.SkillTemplate(1217, 1, "Greater Group Heal",
				com.lopez.l2j.game.skill.SkillTemplate.OperateType.ACTIVE, "HEAL", "TARGET_PARTY", true,
				10, 0, 0, 100.0, 0, 1000, 0, 0, 0, 40, 0, false, List.of(), List.of(), null, null);

		// Dispara a cura em grupo
		invokeMethod(session, "finishCast",
				new Class<?>[] { com.lopez.l2j.game.skill.SkillTemplate.class, NpcInstance.class,
						com.lopez.l2j.game.door.DoorInstance.class, GameSession.class,
						boolean.class, boolean.class, boolean.class },
				groupHeal, null, null, null, false, false, false);

		// Ambos os membros devem ter recebido a cura
		assertEquals(150.0, player.currentHp());
		assertEquals(160.0, player2.currentHp());
	}

	@Test
	void testResurrectionSkill() {
		var deadPlayer = new PlayerCharacter(1003, "DeadHero", "Corpse", 1, 0, 0, 0, 0, 0, false, 0, 0, 0,
				500, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 0.0, 100.0, 0.0);
		deadPlayer.inventory(new Inventory(deadPlayer.objectId()));
		deadPlayer.moveTo(20, 20, 0);
		assertTrue(deadPlayer.isDead());

		List<GameServerPacket> deadSent = new ArrayList<>();
		var deadSession = new GameSession(ctx, new byte[8], "127.0.0.1", deadSent::add);
		setField(deadSession, "state", GameClientPacket.State.IN_GAME);
		setField(deadSession, "active", deadPlayer);
		setField(deadSession, "inWorld", true);
		world.add(deadSession);

		var resurrect = new com.lopez.l2j.game.skill.SkillTemplate(1016, 1, "Resurrection",
				com.lopez.l2j.game.skill.SkillTemplate.OperateType.ACTIVE, "RESURRECT", "TARGET_CORPSE_PLAYER", true,
				10, 0, 0, 50.0, 400, 0, 0, 0, 0, 20, 0, false, List.of(), List.of(), null, null);

		invokeMethod(session, "finishCast",
				new Class<?>[] { com.lopez.l2j.game.skill.SkillTemplate.class, NpcInstance.class,
						com.lopez.l2j.game.door.DoorInstance.class, GameSession.class,
						boolean.class, boolean.class, boolean.class },
				resurrect, null, null, deadSession, false, false, false);

		// O jogador morto deve estar vivo novamente com 50% de HP
		assertFalse(deadPlayer.isDead());
		assertEquals(250.0, deadPlayer.currentHp());
		assertTrue(deadSent.stream().anyMatch(p -> p instanceof Revive));
	}

	@Test
	void testPvPOffensiveSkillDamagesCpAndHp() {
		var enemy = new PlayerCharacter(1004, "Enemy", "Foe", 1, 0, 0, 0, 0, 0, false, 0, 0, 0,
				200, 100, 50, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 200.0, 100.0, 50.0);
		enemy.inventory(new Inventory(enemy.objectId()));
		enemy.moveTo(30, 0, 0);

		List<GameServerPacket> enemySent = new ArrayList<>();
		var enemySession = new GameSession(ctx, new byte[8], "127.0.0.1", enemySent::add);
		setField(enemySession, "state", GameClientPacket.State.IN_GAME);
		setField(enemySession, "active", enemy);
		setField(enemySession, "inWorld", true);
		world.add(enemySession);

		// Skill mágico de dano 100
		var nuke = new com.lopez.l2j.game.skill.SkillTemplate(1177, 1, "Wind Strike",
				com.lopez.l2j.game.skill.SkillTemplate.OperateType.ACTIVE, "MDAM", "TARGET_ONE", true,
				10, 0, 0, 100.0, 600, 0, 0, 0, 0, 20, 0, false, List.of(), List.of(), null, null);

		invokeMethod(session, "finishCast",
				new Class<?>[] { com.lopez.l2j.game.skill.SkillTemplate.class, NpcInstance.class,
						com.lopez.l2j.game.door.DoorInstance.class, GameSession.class,
						boolean.class, boolean.class, boolean.class },
				nuke, null, null, enemySession, false, false, false);

		// Em PvP, CP absorve dano primeiro
		// O dano foi maior que o CP total (50), então CP deve ter sido zerado e o restante afetou o HP
		assertEquals(0.0, enemy.currentCp());
		assertTrue(enemy.currentHp() < 200.0);
		assertTrue(sent.stream().anyMatch(p -> p instanceof SystemMessage sm && sm.id() == SystemMessage.YOU_DID_S1_DMG));
		assertTrue(enemySent.stream().anyMatch(p -> p instanceof SystemMessage sm && sm.id() == SystemMessage.S1_GAVE_YOU_S2_DMG));
	}

	@Test
	void testControlDebuffAndSleepDamageWakeup() {
		var targetChar = new PlayerCharacter(1005, "Target", "Victim", 1, 0, 0, 0, 0, 0, false, 0, 0, 0,
				200, 100, 50, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 200.0, 100.0, 50.0);
		targetChar.inventory(new Inventory(targetChar.objectId()));
		targetChar.moveTo(30, 0, 0);

		List<GameServerPacket> targetSent = new ArrayList<>();
		var targetSession = new GameSession(ctx, new byte[8], "127.0.0.1", targetSent::add);
		setField(targetSession, "state", GameClientPacket.State.IN_GAME);
		setField(targetSession, "active", targetChar);
		setField(targetSession, "inWorld", true);
		world.add(targetSession);

		// Aplica sleep de 10 segundos
		targetSession.applyControlEffect("sleep", System.currentTimeMillis() + 10000L);
		assertTrue(targetChar.isDisabled());

		// Ao receber dano, acorda do sleep
		targetChar.onDamaged();
		assertFalse(targetChar.isDisabled());
	}

	@Test
	void testNpcLinkBypassDisplaysHtml() {
		sent.clear();
		invokeMethod(session, "onBypass", new Class<?>[] { GameClientPacket.RequestBypassToServer.class },
				new GameClientPacket.RequestBypassToServer("npc_30005_Link common/duals_01.htm"));

		assertTrue(sent.stream().anyMatch(p -> p instanceof GameServerPacket.NpcHtmlMessage nh && nh.html().contains("dual")),
				"Deve renderizar HTML de duals_01.htm");

		sent.clear();
		invokeMethod(session, "onBypass", new Class<?>[] { GameClientPacket.RequestBypassToServer.class },
				new GameClientPacket.RequestBypassToServer("Link common/duals_01.htm"));
		assertTrue(sent.stream().anyMatch(p -> p instanceof GameServerPacket.NpcHtmlMessage nh && nh.html().contains("dual")),
				"Deve renderizar HTML mesmo sem prefixo npc_");
	}

	@Test
	void testNpcExcMultisellDisplaysList() {
		sent.clear();
		invokeMethod(session, "onBypass", new Class<?>[] { GameClientPacket.RequestBypassToServer.class },
				new GameClientPacket.RequestBypassToServer("npc_30005_exc_multisell 003"));

		assertTrue(sent.stream().anyMatch(p -> p instanceof GameServerPacket.MultiSellList ml && ml.listId() == 3),
				"Deve enviar pacote MultiSellList para o ID 3");
	}

	@Test
	void testNpcSellListAndSellExecution() {
		// 1. NPC Sell abre SellList
		sent.clear();
		invokeMethod(session, "onBypass", new Class<?>[] { GameClientPacket.RequestBypassToServer.class },
				new GameClientPacket.RequestBypassToServer("npc_30005_Sell"));
		assertTrue(sent.stream().anyMatch(p -> p instanceof GameServerPacket.SellList),
				"Bypass de Sell deve enviar pacote SellList");

		// 2. Execucao de venda de item stackable (ex: Wooden Arrow)
		var added = inventoryService.addItem(player.inventory(), 17, 10, "TestSell");
		assertNotNull(added);
		int objId = added.item().objectId();
		long adenaBefore = player.inventory().adena();

		invokeMethod(session, "onSellItem", new Class<?>[] { GameClientPacket.RequestSellItem.class },
				new GameClientPacket.RequestSellItem(1, List.of(new GameClientPacket.SellItemRequest(objId, 17, 4))));

		assertEquals(6, player.inventory().byObjectId(objId).orElseThrow().count());
		assertTrue(player.inventory().adena() > adenaBefore, "Adena do jogador deve aumentar apos venda");
		assertTrue(sent.stream().anyMatch(p -> p instanceof GameServerPacket.InventoryUpdate),
				"Deve atualizar inventario");
		assertTrue(sent.stream().anyMatch(p -> p instanceof GameServerPacket.ItemList),
				"Deve reenviar lista de itens");
	}

	@Test
	void testNpcQuestMonsterDerbyTrackTeleport() {
		sent.clear();
		invokeMethod(session, "onBypass", new Class<?>[] { GameClientPacket.RequestBypassToServer.class },
				new GameClientPacket.RequestBypassToServer("npc_30005_Quest 1101_teleport_to_race_track"));

		assertEquals(12661, player.x());
		assertEquals(181687, player.y());
		assertEquals(-3560, player.z());
		assertTrue(sent.stream().anyMatch(p -> p instanceof GameServerPacket.TeleportToLocation));
	}

	@Test
	void testFunctionalNpcHtmlFallbacksWhenHtmlMissing() {
		var cache = ctx.htmls();
		assertNotNull(cache);

		// Teleporter
		String teleHtml = cache.getNpcHtml(99999, "L2Teleporter", 0);
		assertTrue(teleHtml.contains("Teleport"), "Teleporter fallback deve conter opcao de Teleport");
		assertFalse(teleHtml.contains("I have nothing to say to you"), "Nao deve cair em npcdefault");

		// Merchant
		String merchHtml = cache.getNpcHtml(99999, "L2Merchant", 0);
		assertTrue(merchHtml.contains("Buy items") && merchHtml.contains("Sell items"),
				"Merchant fallback deve conter Buy e Sell");

		// Blacksmith
		String bsHtml = cache.getNpcHtml(99999, "L2Blacksmith", 0);
		assertTrue(bsHtml.contains("Craft Dual Swords") && bsHtml.contains("Augment Item"),
				"Blacksmith fallback deve conter Craft e Augment");

		// Warehouse
		String whHtml = cache.getNpcHtml(99999, "L2Warehouse", 0);
		assertTrue(whHtml.contains("Deposit Item") && whHtml.contains("Withdraw Item"),
				"Warehouse fallback deve conter operacoes de bau");
	}

	@Test
	void testDebuffIconsOpcodeAndFastRestore() {
		// 1. Validar que o opcode do pacote e 0x7f (MagicEffectIcons do Interlude)
		var icon = new MagicEffectIcons.Icon(1160, 1, 30);
		var packet = new MagicEffectIcons(List.of(icon));
		byte[] encoded = packet.encode();
		assertEquals((byte) 0x7f, encoded[0], "Opcode do Interlude para barra de buffs/debuffs deve ser 0x7f");

		// 2. Debuff ativo no player deve ser transmitido no pacote
		player.effects().put(com.lopez.l2j.game.effect.PlayerEffects.ActiveBuff.ofSkill(
				1160, 1, "slow", System.currentTimeMillis() + 30_000L, List.of(), true));
		session.refreshBuffs();
		var sentIcons = sent.stream()
				.filter(p -> p instanceof MagicEffectIcons)
				.map(p -> (MagicEffectIcons) p)
				.toList();
		assertFalse(sentIcons.isEmpty(), "Deve enviar pacote de icones com debuff");
		assertTrue(sentIcons.getLast().icons().stream().anyMatch(i -> i.skillId() == 1160),
				"Debuff deve constar na lista de icones enviada ao cliente");
	}

	private static class FakeCharacterSkillSaveRepository extends com.lopez.l2j.game.effect.CharacterSkillSaveRepository {
		final java.util.Map<Integer, List<SavedBuff>> saved = new java.util.concurrent.ConcurrentHashMap<>();

		FakeCharacterSkillSaveRepository() {
			super(null);
		}

		@Override
		public void saveBuffs(int charId, int classIndex, List<com.lopez.l2j.game.effect.PlayerEffects.ActiveBuff> buffs) {
			long now = System.currentTimeMillis();
			var list = buffs.stream()
					.map(b -> new SavedBuff(b.skillId(), b.level(), b.remainingSeconds(now), b.endTimeMillis(), 1))
					.toList();
			saved.put(charId, list);
		}

		@Override
		public List<SavedBuff> restoreBuffs(int charId, int classIndex) {
			return saved.getOrDefault(charId, List.of());
		}

		@Override
		public void deleteBuffs(int charId) {
			saved.remove(charId);
		}
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

	@Test
	void testAdminCharListRendersPlayersAndPagination() {
		player.accessLevel(100);
		var bob = new PlayerCharacter(1002, "BobAcc", "Bob", 1, 0, 0, 0, 0, 0, false, 0, 0, 0,
				200, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 200.0, 100.0, 100.0);
		var bobSession = new GameSession(ctx, new byte[8], "127.0.0.1", p -> {});
		setField(bobSession, "active", bob);
		world.add(bobSession);

		invokeMethod(session, "handleAdminCommand", new Class<?>[] { String.class }, "charlist");

		NpcHtmlMessage htmlMsg = (NpcHtmlMessage) sent.stream()
				.filter(p -> p instanceof NpcHtmlMessage)
				.reduce((first, second) -> second)
				.orElse(null);
		assertNotNull(htmlMsg, "Deve enviar NpcHtmlMessage");
		String html = htmlMsg.html();
		assertFalse(html.contains("%players%"), "Nao deve conter %players% literal");
		assertFalse(html.contains("%pages%"), "Nao deve conter %pages% literal");
		assertTrue(html.contains("admin_character_info Hero"), "Deve conter link para Hero");
		assertTrue(html.contains("admin_character_info Bob"), "Deve conter link para Bob");
	}

	@Test
	void testAdminCharInfoRendersStats() {
		player.accessLevel(100);
		invokeMethod(session, "handleAdminCommand", new Class<?>[] { String.class }, "character_info Hero");

		NpcHtmlMessage htmlMsg = (NpcHtmlMessage) sent.stream()
				.filter(p -> p instanceof NpcHtmlMessage)
				.reduce((first, second) -> second)
				.orElse(null);
		assertNotNull(htmlMsg, "Deve enviar NpcHtmlMessage");
		String html = htmlMsg.html();
		assertFalse(html.contains("%name%"), "Nao deve conter %name% literal");
		assertFalse(html.contains("%currenthp%"), "Nao deve conter %currenthp% literal");
		assertFalse(html.contains("%patk%"), "Nao deve conter %patk% literal");
		assertTrue(html.contains("Hero"), "Deve conter o nome Hero");
		assertTrue(html.contains("admin_teleportto Hero"), "Deve conter botao admin_teleportto");
		assertTrue(html.contains("admin_kick Hero"), "Deve conter botao admin_kick");
	}

	@Test
	void testRequestGMCommandAltGStatus() {
		player.accessLevel(100);

		// Test decode 0x6e
		byte[] targetBytes = "Hero".getBytes(java.nio.charset.StandardCharsets.UTF_16LE);
		ByteBuffer buf = ByteBuffer.allocate(1 + targetBytes.length + 2 + 4).order(ByteOrder.LITTLE_ENDIAN);
		buf.put((byte) 0x6e);
		buf.put(targetBytes);
		buf.putShort((short) 0);
		buf.putInt(1); // command 1 = status
		var decoded = GameClientPacket.decode(GameClientPacket.State.IN_GAME, buf.array());
		assertTrue(decoded.isPresent(), "Pacote 0x6e deve ser decodificado");
		assertTrue(decoded.get() instanceof GameClientPacket.RequestGMCommand);
		var cmd = (GameClientPacket.RequestGMCommand) decoded.get();
		assertEquals("Hero", cmd.targetName());
		assertEquals(1, cmd.command());

		// Test handle in session
		session.handle(buf.array());
		boolean hasCharInfo = sent.stream().anyMatch(p -> p instanceof GameServerPacket.GMViewCharacterInfo);
		assertTrue(hasCharInfo, "Deve enviar GMViewCharacterInfo para o Alt+G");

		// Test packet encoding starts with 0x8f
		var charInfo = (GameServerPacket.GMViewCharacterInfo) sent.stream()
				.filter(p -> p instanceof GameServerPacket.GMViewCharacterInfo)
				.findFirst()
				.orElseThrow();
		byte[] encoded = charInfo.encode();
		assertEquals((byte) 0x8f, encoded[0], "Opcode do GMViewCharacterInfo deve ser 0x8f");
	}

	@Test
	void testRequestGMCommandAltGInventory() {
		player.accessLevel(100);
		byte[] targetBytes = "Hero".getBytes(java.nio.charset.StandardCharsets.UTF_16LE);
		ByteBuffer buf = ByteBuffer.allocate(1 + targetBytes.length + 2 + 4).order(ByteOrder.LITTLE_ENDIAN);
		buf.put((byte) 0x6e);
		buf.put(targetBytes);
		buf.putShort((short) 0);
		buf.putInt(5); // command 5 = inventory
		session.handle(buf.array());

		boolean hasItemList = sent.stream().anyMatch(p -> p instanceof GameServerPacket.GMViewItemList);
		boolean hasHenna = sent.stream().anyMatch(p -> p instanceof GameServerPacket.GMHennaInfo);
		assertTrue(hasItemList, "Deve enviar GMViewItemList para o tab de inventario do Alt+G");
		assertTrue(hasHenna, "Deve enviar GMHennaInfo para o Alt+G");
	}

	@Test
	void testAdminTeleportToPlayer() {
		player.accessLevel(100);
		var bob = new PlayerCharacter(1002, "BobAcc", "Bob", 1, 0, 0, 0, 0, 0, false, 0, 0, 0,
				200, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 200.0, 100.0, 100.0);
		bob.moveTo(5000, 6000, -1000);
		var bobSession = new GameSession(ctx, new byte[8], "127.0.0.1", p -> {});
		setField(bobSession, "active", bob);
		world.add(bobSession);

		invokeMethod(session, "handleAdminCommand", new Class<?>[] { String.class }, "teleportto Bob");
		assertEquals(5000, player.x());
		assertEquals(6000, player.y());
		assertEquals(-1000, player.z());
	}

	@Test
	void testTeleportLifecycleAndRequestAppearing() {
		sent.clear();
		session.teleportToLocation(83400, 147943, -3404);

		// Confere envio de TeleportToLocation
		boolean hasTeleport = sent.stream().anyMatch(p -> p instanceof GameServerPacket.TeleportToLocation);
		assertTrue(hasTeleport, "Deve enviar TeleportToLocation");

		// Envia RequestAppearing (0x30)
		var buf = ByteBuffer.allocate(1).order(ByteOrder.LITTLE_ENDIAN);
		buf.put((byte) 0x30);
		session.handle(buf.array());

		// Apos RequestAppearing, UserInfo deve ser enviado
		boolean hasUserInfo = sent.stream().anyMatch(p -> p instanceof GameServerPacket.UserInfo);
		assertTrue(hasUserInfo, "Deve enviar UserInfo apos RequestAppearing");
	}

	@Test
	void testTeleportClearsOldLocationFromKnownObjects() {
		var bob = new PlayerCharacter(1002, "BobAcc", "Bob", 1, 0, 0, 0, 0, 0, false, 0, 0, 0,
				200, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 200.0, 100.0, 100.0);
		bob.moveTo(player.x() + 100, player.y() + 100, player.z());
		List<GameServerPacket> bobSent = new ArrayList<>();
		var bobSession = new GameSession(ctx, new byte[8], "127.0.0.1", bobSent::add);
		setField(bobSession, "active", bob);
		setField(bobSession, "inWorld", true);
		world.add(bobSession);

		// Inicializa visibilidade mutua
		invokeMethod(session, "updateKnownObjects", new Class<?>[] {});

		// Player teletransporta para longe
		session.teleportToLocation(83400, 147943, -3404);

		// Bob deve receber DeleteObject do player que teleportou
		boolean bobGotDelete = bobSent.stream().anyMatch(p -> p instanceof GameServerPacket.DeleteObject del && del.objectId() == player.objectId());
		assertTrue(bobGotDelete, "Outro jogador no local antigo deve receber DeleteObject");
	}

	@Test
	void testPlayerAutoAttackAndPvpFlag() {
		var bob = new PlayerCharacter(1003, "BobAcc2", "BobTarget", 1, 0, 0, 0, 0, 0, false, 0, 0, 0,
				1000, 500, 500, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 1000.0, 500.0, 500.0);
		bob.moveTo(player.x() + 30, player.y() + 30, player.z());
		List<GameServerPacket> bobSent = new ArrayList<>();
		var bobSession = new GameSession(ctx, new byte[8], "127.0.0.1", bobSent::add);
		setField(bobSession, "active", bob);
		setField(bobSession, "inWorld", true);
		world.add(bobSession);

		sent.clear();

		// Envia AttackRequest no Bob (opcode 0x0a)
		var buf = ByteBuffer.allocate(18).order(ByteOrder.LITTLE_ENDIAN);
		buf.put((byte) 0x0a); // 0x0a AttackRequest
		buf.putInt(bob.objectId());
		buf.putInt(player.x());
		buf.putInt(player.y());
		buf.putInt(player.z());
		buf.put((byte) 0); // shift / attackId
		session.handle(buf.array());

		// Confere se enviou AutoAttackStart (combat stance)
		boolean hasAutoAttackStart = sent.stream().anyMatch(p -> p instanceof GameServerPacket.AutoAttackStart);
		assertTrue(hasAutoAttackStart, "Deve enviar AutoAttackStart ao atacar jogador");

		// Confere se ativou pvpFlag (nome roxo)
		assertEquals(1, player.pvpFlag(), "Jogador deve receber pvpFlag=1");

		// Confere se enviou Attack packet
		boolean hasAttack = sent.stream().anyMatch(p -> p instanceof GameServerPacket.Attack);
		assertTrue(hasAttack, "Deve enviar pacote Attack contra o jogador");
	}

	@Test
	void testCancelCastOnMove() throws Exception {
		// Equipa ou garante que player tem skill com hitTime
		setField(session, "casting", true);

		// Move enquanto esta casting (opcode 0x01 MoveBackwardToLocation)
		var buf = ByteBuffer.allocate(29).order(ByteOrder.LITTLE_ENDIAN);
		buf.put((byte) 0x01); // 0x01 MoveBackwardToLocation
		buf.putInt(player.x() + 500);
		buf.putInt(player.y() + 500);
		buf.putInt(player.z());
		buf.putInt(player.x());
		buf.putInt(player.y());
		buf.putInt(player.z());
		buf.putInt(1); // moveMovement = 1 (click no chao)
		session.handle(buf.array());

		// casting deve ter sido cancelado imediatamente
		boolean isCasting = (boolean) getField(session, "casting");
		assertFalse(isCasting, "Casting deve ser false apos mover");

		// Deve enviar MagicSkillCanceld
		boolean hasCancel = sent.stream().anyMatch(p -> p instanceof GameServerPacket.MagicSkillCanceld);
		assertTrue(hasCancel, "Deve enviar pacote MagicSkillCanceld ao andar durante o cast");
	}

	@Test
	void testAttackSkillBlocksMoveDuringCast() throws Exception {
		sent.clear();
		var nuke = new com.lopez.l2j.game.skill.SkillTemplate(1177, 1, "Wind Strike",
				com.lopez.l2j.game.skill.SkillTemplate.OperateType.ACTIVE, "MDAM", "TARGET_ONE", true,
				10, 0, 0, 100.0, 600, 0, 0, 0, 0, 20, 0, false, List.of(), List.of(), null, null);
		setField(session, "casting", true);
		setField(session, "castingSkill", nuke);

		int startX = player.x();
		int startY = player.y();

		var buf = ByteBuffer.allocate(29).order(ByteOrder.LITTLE_ENDIAN);
		buf.put((byte) 0x01); // MoveBackwardToLocation
		buf.putInt(startX + 500);
		buf.putInt(startY + 500);
		buf.putInt(player.z());
		buf.putInt(startX);
		buf.putInt(startY);
		buf.putInt(player.z());
		buf.putInt(1);
		session.handle(buf.array());

		// Casting NAO deve ser cancelado ao tentar andar usando skill de ataque
		boolean isCasting = (boolean) getField(session, "casting");
		assertTrue(isCasting, "Casting de skill de ataque deve permanecer ativo");
		assertEquals(startX, player.x(), "Jogador nao deve andar durante cast de ataque");
		assertEquals(startY, player.y(), "Jogador nao deve andar durante cast de ataque");

		// Nao deve enviar MagicSkillCanceld nem MoveToLocation
		boolean hasCancel = sent.stream().anyMatch(p -> p instanceof GameServerPacket.MagicSkillCanceld);
		assertFalse(hasCancel, "Nao deve enviar MagicSkillCanceld para skill de ataque");
		boolean hasMove = sent.stream().anyMatch(p -> p instanceof GameServerPacket.MoveToLocation);
		assertFalse(hasMove, "Nao deve enviar MoveToLocation durante cast de ataque");
		boolean hasActionFailed = sent.stream().anyMatch(p -> p instanceof GameServerPacket.ActionFailed);
		assertTrue(hasActionFailed, "Deve enviar ActionFailed ao bloquear movimento durante cast de ataque");
	}

	@Test
	void testHealSkillBlocksMoveDuringCastAndCompletes() throws Exception {
		sent.clear();
		var heal = new com.lopez.l2j.game.skill.SkillTemplate(1217, 1, "Greater Group Heal",
				com.lopez.l2j.game.skill.SkillTemplate.OperateType.ACTIVE, "HEAL", "TARGET_PARTY", true,
				10, 0, 0, 100.0, 0, 1000, 0, 0, 0, 40, 0, false, List.of(), List.of(), null, null);
		setField(session, "casting", true);
		setField(session, "castingSkill", heal);

		int startX = player.x();
		int startY = player.y();

		var buf = ByteBuffer.allocate(29).order(ByteOrder.LITTLE_ENDIAN);
		buf.put((byte) 0x01); // MoveBackwardToLocation
		buf.putInt(startX + 500);
		buf.putInt(startY + 500);
		buf.putInt(player.z());
		buf.putInt(startX);
		buf.putInt(startY);
		buf.putInt(player.z());
		buf.putInt(1);
		session.handle(buf.array());

		// Ao andar durante o cast de cura, NAO deve cancelar o cast; personagem executa a skill
		boolean isCasting = (boolean) getField(session, "casting");
		assertTrue(isCasting, "Casting de cura deve permanecer ativo ao clicar para andar");

		boolean hasCancel = sent.stream().anyMatch(p -> p instanceof GameServerPacket.MagicSkillCanceld);
		assertFalse(hasCancel, "Nao deve enviar MagicSkillCanceld para skill de cura ao andar");
		boolean hasMove = sent.stream().anyMatch(p -> p instanceof GameServerPacket.MoveToLocation);
		assertFalse(hasMove, "Nao deve enviar MoveToLocation durante o cast");
		boolean hasActionFailed = sent.stream().anyMatch(p -> p instanceof GameServerPacket.ActionFailed);
		assertTrue(hasActionFailed, "Deve enviar ActionFailed ao tentar andar enquanto cura");
	}

	@Test
	void testSkillStopsMovementOnCastStart() {
		sent.clear();
		var heal = new com.lopez.l2j.game.skill.SkillTemplate(1217, 1, "Greater Group Heal",
				com.lopez.l2j.game.skill.SkillTemplate.OperateType.ACTIVE, "HEAL", "TARGET_PARTY", true,
				10, 0, 0, 100.0, 0, 1000, 1000, 0, 0, 40, 0, false, List.of(), List.of(), null, null);

		session.castSkill(heal, false);

		// Deve enviar StopMove para o personagem parar ("dar uma paradinha") e conjurar
		boolean hasStop = sent.stream().anyMatch(p -> p instanceof GameServerPacket.StopMove);
		assertTrue(hasStop, "Deve enviar pacote StopMove para parar a movimentacao ao iniciar o cast");
	}

	@Test
	void testAdminSoundsAndPlaySound() throws Exception {
		player.accessLevel(100);
		sent.clear();

		// Testa bypass admin_sounds e comando //sounds
		session.handle(buildBypassPacket("admin_sounds"));
		boolean hasHtml = sent.stream().anyMatch(p -> p instanceof GameServerPacket.NpcHtmlMessage);
		assertTrue(hasHtml, "Deve enviar NpcHtmlMessage ao executar admin_sounds");

		sent.clear();
		// Testa comando play_sound via invokeMethod
		invokeMethod(session, "handleAdminCommand", new Class<?>[] { String.class }, "play_sound ls01_f");
		boolean hasPlaySound = sent.stream().anyMatch(p -> p instanceof GameServerPacket.PlaySound ps && "ls01_f".equals(ps.soundFile()));
		assertTrue(hasPlaySound, "Deve enviar pacote PlaySound para ls01_f");
	}

	@Test
	void testAdminRblist() throws Exception {
		player.accessLevel(100);
		sent.clear();

		// Testa comando rblist sem argumentos (menu principal de raid)
		session.handle(buildBypassPacket("admin_rblist"));
		boolean hasMainRaidMenu = sent.stream().anyMatch(p -> p instanceof GameServerPacket.NpcHtmlMessage);
		assertTrue(hasMainRaidMenu, "Deve abrir menu de Raid Boss com admin_rblist");

		sent.clear();
		// Testa comando rblist 20 (faixa de level 20-29)
		session.handle(buildBypassPacket("admin_rblist 20"));
		boolean hasRaidList20 = sent.stream().anyMatch(p -> p instanceof GameServerPacket.NpcHtmlMessage html && html.html().contains("Raid Bosses (20-29)"));
		assertTrue(hasRaidList20, "Deve listar Raid Bosses da faixa 20-29");

		sent.clear();
		// Testa comando rblist grand (Grand Bosses)
		session.handle(buildBypassPacket("admin_rblist grand"));
		boolean hasGrandBossList = sent.stream().anyMatch(p -> p instanceof GameServerPacket.NpcHtmlMessage html && html.html().contains("Grand Bosses") && html.html().contains("Antharas"));
		assertTrue(hasGrandBossList, "Deve listar Grand Bosses incluindo Antharas com coordenadas e teleport");
	}

	@Test
	void chargesIncreaseAndCapWithoutSpamWhenAlreadyMax() {
		player.charges(6);
		sent.clear();
		session.increaseCharges(1, 7);
		assertEquals(7, player.charges());
		assertTrue(sent.stream().anyMatch(p -> p instanceof SystemMessage sm && sm.id() == SystemMessage.FORCE_MAXLEVEL_REACHED),
				"Ao atingir o maximo de charges deve enviar FORCE_MAXLEVEL_REACHED");

		// Quando ja esta no maximo (ex: batendo com Sonic Rage no limite de 7)
		sent.clear();
		session.increaseCharges(1, 7);
		assertEquals(7, player.charges(), "Charges nao devem ultrapassar o maximo");
		assertFalse(sent.stream().anyMatch(p -> p instanceof SystemMessage sm && sm.id() == SystemMessage.FORCE_MAXLEVEL_REACHED),
				"Quando ja esta no maximo, nao deve enviar spam de FORCE_MAXLEVEL_REACHED");
	}

	@Test
	void serverSysMessagesAreRoutedToSystemMessageInTopChatWindow() {
		sent.clear();
		// 1. Mensagem de SYS deve ser convertida para SystemMessage (ID 1987) e nao CreatureSay no chat branco
		session.send(new GameServerPacket.CreatureSay(0, GameServerPacket.CreatureSay.ALL, "SYS", "Voce nao pode usar habilidades ofensivas em zona de paz."));
		assertEquals(1, sent.size());
		assertTrue(sent.get(0) instanceof GameServerPacket.SystemMessage sm
				&& sm.id() == GameServerPacket.SystemMessage.S1
				&& "Voce nao pode usar habilidades ofensivas em zona de paz.".equals(sm.text()),
				"Mensagem de SYS deve ir como SystemMessage para a janela de sistema no topo");

		// 2. session.sendMessage direto deve enviar SystemMessage
		sent.clear();
		session.sendMessage("Parabens! Voce agora e um Human Wizard!");
		assertEquals(1, sent.size());
		assertTrue(sent.get(0) instanceof GameServerPacket.SystemMessage sm
				&& sm.id() == GameServerPacket.SystemMessage.S1
				&& "Parabens! Voce agora e um Human Wizard!".equals(sm.text()));

		// 3. Modulo como ACP/Seguranca com objectId 0 deve vir com tag no SystemMessage
		sent.clear();
		session.send(new GameServerPacket.CreatureSay(0, GameServerPacket.CreatureSay.ALL, "ACP", "Auto Combat Potion (ACP) ATIVADO."));
		assertEquals(1, sent.size());
		assertTrue(sent.get(0) instanceof GameServerPacket.SystemMessage sm
				&& sm.id() == GameServerPacket.SystemMessage.S1
				&& "[ACP] Auto Combat Potion (ACP) ATIVADO.".equals(sm.text()));

		// 4. Chat comum de jogador (objectId > 0) NAO deve ser alterado e deve permanecer CreatureSay
		sent.clear();
		session.send(new GameServerPacket.CreatureSay(player.objectId(), GameServerPacket.CreatureSay.ALL, player.name(), "Ola mundo!"));
		assertEquals(1, sent.size());
		assertTrue(sent.get(0) instanceof GameServerPacket.CreatureSay cs
				&& cs.channel() == GameServerPacket.CreatureSay.ALL
				&& "Ola mundo!".equals(cs.text()),
				"Chat de jogador deve continuar sendo CreatureSay no chat comum");
	}

	@Test
	void testAugmentationPacketOpcodesAndLifeStoneUse() {
		// 1. Validar opcodes dos pacotes de augmentacao (Interlude C6 client table: 0x51 a 0x58)
		byte[] makeWindow = new GameServerPacket.ExShowVariationMakeWindow().encode();
		assertEquals((byte) 0xfe, makeWindow[0]);
		assertEquals((short) 0x51, ByteBuffer.wrap(makeWindow, 1, 2).order(ByteOrder.LITTLE_ENDIAN).getShort(),
				"ExShowVariationMakeWindow deve ter sub-opcode 0x51");

		byte[] cancelWindow = new GameServerPacket.ExShowVariationCancelWindow().encode();
		assertEquals((byte) 0xfe, cancelWindow[0]);
		assertEquals((short) 0x52, ByteBuffer.wrap(cancelWindow, 1, 2).order(ByteOrder.LITTLE_ENDIAN).getShort(),
				"ExShowVariationCancelWindow deve ter sub-opcode 0x52");

		byte[] putItemMake = new GameServerPacket.ExPutItemResultForVariationMake(12345).encode();
		assertEquals((byte) 0xfe, putItemMake[0]);
		assertEquals((short) 0x53, ByteBuffer.wrap(putItemMake, 1, 2).order(ByteOrder.LITTLE_ENDIAN).getShort(),
				"ExPutItemResultForVariationMake deve ter sub-opcode 0x53");

		byte[] putIntensive = new GameServerPacket.ExPutIntensiveResultForVariationMake(1, 8723, 2130, 20).encode();
		assertEquals((byte) 0xfe, putIntensive[0]);
		assertEquals((short) 0x54, ByteBuffer.wrap(putIntensive, 1, 2).order(ByteOrder.LITTLE_ENDIAN).getShort(),
				"ExPutIntensiveResultForVariationMake deve ter sub-opcode 0x54");

		byte[] putCommission = new GameServerPacket.ExPutCommissionResultForVariationMake(2, 20, 2130).encode();
		assertEquals((byte) 0xfe, putCommission[0]);
		assertEquals((short) 0x55, ByteBuffer.wrap(putCommission, 1, 2).order(ByteOrder.LITTLE_ENDIAN).getShort(),
				"ExPutCommissionResultForVariationMake deve ter sub-opcode 0x55");

		byte[] variationResult = new GameServerPacket.ExVariationResult(100, 200, 1).encode();
		assertEquals((byte) 0xfe, variationResult[0]);
		assertEquals((short) 0x56, ByteBuffer.wrap(variationResult, 1, 2).order(ByteOrder.LITTLE_ENDIAN).getShort(),
				"ExVariationResult deve ter sub-opcode 0x56");

		byte[] putItemCancel = new GameServerPacket.ExPutItemResultForVariationCancel(12345, 100, 200, 210000).encode();
		assertEquals((byte) 0xfe, putItemCancel[0]);
		assertEquals((short) 0x57, ByteBuffer.wrap(putItemCancel, 1, 2).order(ByteOrder.LITTLE_ENDIAN).getShort(),
				"ExPutItemResultForVariationCancel deve ter sub-opcode 0x57");
		assertEquals(12345, ByteBuffer.wrap(putItemCancel, 3, 4).order(ByteOrder.LITTLE_ENDIAN).getInt(),
				"Primeiro campo de ExPutItemResultForVariationCancel deve ser o itemObjId");

		byte[] cancelResult = new GameServerPacket.ExVariationCancelResult(1).encode();
		assertEquals((byte) 0xfe, cancelResult[0]);
		assertEquals((short) 0x58, ByteBuffer.wrap(cancelResult, 1, 2).order(ByteOrder.LITTLE_ENDIAN).getShort(),
				"ExVariationCancelResult deve ter sub-opcode 0x58");
		assertEquals(7, cancelResult.length, "ExVariationCancelResult deve ter 7 bytes no total");
		assertEquals(1, ByteBuffer.wrap(cancelResult, 3, 4).order(ByteOrder.LITTLE_ENDIAN).getInt());

		// 2. Validar que usar Life Stone do inventario abre a janela de augmentacao
		ItemTemplate lsTemplate = ItemTemplate.etc(8723, 8723, "Life Stone", "none", "normal", 1, "none", 0, true, true, true, true);
		ItemInstance lifeStone = new ItemInstance(999, lsTemplate, player.objectId(), 1);
		player.inventory().addItem(lifeStone);
		sent.clear();
		session.onUseItem(new GameClientPacket.UseItem(lifeStone.objectId()));
		assertTrue(sent.stream().anyMatch(p -> p instanceof GameServerPacket.ExShowVariationMakeWindow),
				"Usar Life Stone do inventario deve abrir ExShowVariationMakeWindow");
	}
}
