package com.lopez.l2j.game.trade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.game.item.InMemoryItemRepository;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemSlots;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.item.TestItems;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TradeValidationTest {

	private TradeService tradeService;
	private InventoryService inventoryService;
	private PlayerCharacter player1;
	private PlayerCharacter player2;

	@BeforeEach
	void setUp() {
		InMemoryItemRepository repo = new InMemoryItemRepository();
		inventoryService = new InventoryService(TestItems.table(), repo, ObjectIdFactory.sequential(0x20000000), 10_000_000);
		tradeService = new TradeService(inventoryService);

		player1 = new PlayerCharacter(0x10000001, "Alice", "Adventurer", 1, 0, 0, 0, 0, 0, false, 0, 0, 0,
				1000, 500, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 1000.0, 500.0, 100.0);
		player2 = new PlayerCharacter(0x10000002, "Bob", "Adventurer", 1, 0, 0, 0, 0, 0, false, 0, 0, 0,
				1000, 500, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 1000.0, 500.0, 100.0);

		var inv1 = inventoryService.giveStarterItems(player1.objectId(), 0);
		player1.inventory(inv1);

		var inv2 = inventoryService.giveStarterItems(player2.objectId(), 0);
		player2.inventory(inv2);

		player1.moveTo(0, 0, 0);
		player2.moveTo(50, 0, 0);
	}

	@Test
	@DisplayName("V.23: Distancia maxima de 150 unidades para iniciar e manter trade")
	void testTradeDistanceValidation() {
		// Distancia inicial: 50 unidades -> OK
		assertTrue(tradeService.requestTrade(player1, player2, null));

		// Rejeita resposta se partner estiver longe (> 150)
		player2.moveTo(200, 0, 0); // 200 > 150
		var sessionOpt = tradeService.answerTrade(player2, player1, true, null, null);
		assertTrue(sessionOpt.isEmpty(), "Trade nao deve abrir se a distancia for maior que 150");

		// Reposiciona a 80 unidades e aceita com sucesso
		player2.moveTo(80, 0, 0);
		assertTrue(tradeService.requestTrade(player1, player2, null));
		sessionOpt = tradeService.answerTrade(player2, player1, true, null, null);
		assertTrue(sessionOpt.isPresent(), "Trade deve iniciar dentro do raio de 150");
		assertTrue(tradeService.isTrading(player1.objectId()));

		// Durante o trade, jogador se afasta para 250 unidades -> cancelamento automatico
		player2.moveTo(250, 0, 0);
		tradeService.checkMovementDistance(player2, null, null);
		assertFalse(tradeService.isTrading(player1.objectId()), "Trade deve ser cancelado se distanciar > 150");
	}

	@Test
	@DisplayName("V.24: Trava de trade em combate, morte ou negociacao ativa")
	void testTradeCombatAndDeadLock() {
		// Bloqueio se em combate
		player1.enterCombat();
		assertFalse(tradeService.requestTrade(player1, player2, null), "Nao pode iniciar trade em combate");
		player1.leaveCombat();

		// Bloqueio se morto
		player2.currentHp(0);
		assertFalse(tradeService.requestTrade(player1, player2, null), "Nao pode iniciar trade se alvo estiver morto");
		player2.currentHp(1000);

		// Bloqueio se ja estiver em trade
		assertTrue(tradeService.requestTrade(player1, player2, null));
		tradeService.answerTrade(player2, player1, true, null, null);
		assertTrue(tradeService.isTrading(player1.objectId()));

		PlayerCharacter player3 = new PlayerCharacter(0x10000003, "Charlie", "Adventurer", 1, 0, 0, 0, 0, 0, false, 0, 0, 0,
				1000, 500, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 1000.0, 500.0, 100.0);
		assertFalse(tradeService.requestTrade(player3, player1, null), "Nao pode solicitar trade com quem ja esta negociando");
	}

	@Test
	@DisplayName("V.25: Bloqueio de itens equipados, de quest ou marcados como nao-negociaveis")
	void testItemTradeableValidation() {
		assertTrue(tradeService.requestTrade(player1, player2, null));
		tradeService.answerTrade(player2, player1, true, null, null);

		// Tentativa de colocar item equipado (ex: espada inicial de Alice)
		var equippedSword = player1.inventory().paperdoll(ItemSlots.RHAND);
		assertTrue(equippedSword.isEquipped());
		assertFalse(tradeService.addItem(player1, equippedSword.objectId(), 1, null),
				"Item equipado nao pode ser adicionado ao trade");

		// Cria item de quest (non-tradeable)
		ItemTemplate questTemplate = ItemTemplate.etc(9999, 9999, "Quest Item", "quest", "normal", 0,
				"none", 0, false, false, false, false); // tradeable = false
		ItemInstance questItem = new ItemInstance(0x30000001, questTemplate, player1.objectId(), 1);
		player1.inventory().add(questItem);

		assertFalse(tradeService.addItem(player1, questItem.objectId(), 1, null),
				"Item nao negociavel/quest nao pode ser adicionado ao trade");

		// Item negociavel desequipado (Adena)
		var adenaItem = player1.inventory().byItemId(ItemTemplate.ADENA_ID).orElseThrow();
		assertTrue(tradeService.addItem(player1, adenaItem.objectId(), 1000, null),
				"Item negociavel deve ser aceito na oferta de trade");
	}

	@Test
	@DisplayName("V.26: Commit atomico com lock duplo ordenado por objectId e troca segura de itens")
	void testAtomicDoubleLockCommit() {
		assertTrue(tradeService.requestTrade(player1, player2, null));
		tradeService.answerTrade(player2, player1, true, null, null);

		long adenaAliceBefore = player1.inventory().adena();
		long adenaBobBefore = player2.inventory().adena();

		var adenaAlice = player1.inventory().byItemId(ItemTemplate.ADENA_ID).orElseThrow();
		var adenaBob = player2.inventory().byItemId(ItemTemplate.ADENA_ID).orElseThrow();

		// Alice coloca 5.000 de Adena, Bob coloca 2.000 de Adena
		assertTrue(tradeService.addItem(player1, adenaAlice.objectId(), 5000, null));
		assertTrue(tradeService.addItem(player2, adenaBob.objectId(), 2000, null));

		// Alice e Bob travam
		assertTrue(tradeService.lockTrade(player1));
		assertTrue(tradeService.lockTrade(player2));

		// Confirmam a troca
		tradeService.confirmTrade(player1, null, null);
		tradeService.confirmTrade(player2, null, null);

		// Sessao deve ter sido finalizada
		assertFalse(tradeService.isTrading(player1.objectId()));
		assertFalse(tradeService.isTrading(player2.objectId()));

		// Saldos devem estar transferidos atomicamente (-5000 + 2000 = -3000 para Alice; -2000 + 5000 = +3000 para Bob)
		assertEquals(adenaAliceBefore - 3000, player1.inventory().adena());
		assertEquals(adenaBobBefore + 3000, player2.inventory().adena());
	}
}
