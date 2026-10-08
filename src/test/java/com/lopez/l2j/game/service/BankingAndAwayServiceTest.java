package com.lopez.l2j.game.service;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BankingAndAwayServiceTest {

	private BankingService bankingService;
	private AwayStatusService awayStatusService;
	private GameSession session;
	private PlayerCharacter player;
	private Inventory inventory;
	private InventoryService inventoryService;
	private GameSession.Context context;
	private List<GameServerPacket> sentPackets;

	@BeforeEach
	void setUp() {
		Config.BANKING_SYSTEM_ENABLED = true;
		Config.BANKING_SYSTEM_ADENA = 500_000_000;
		Config.BANKING_SYSTEM_GOLDBARS = 1;

		bankingService = new BankingService();
		awayStatusService = new AwayStatusService();

		sentPackets = new ArrayList<>();
		session = mock(GameSession.class);
		player = mock(PlayerCharacter.class);
		inventory = mock(Inventory.class);
		inventoryService = mock(InventoryService.class);
		context = mock(GameSession.Context.class);

		when(session.activeCharacter()).thenReturn(player);
		when(session.context()).thenReturn(context);
		when(context.inventories()).thenReturn(inventoryService);
		when(player.inventory()).thenReturn(inventory);
		when(player.objectId()).thenReturn(1001);
		when(player.name()).thenReturn("HeroTester");

		doAnswer(inv -> {
			sentPackets.add(inv.getArgument(0));
			return null;
		}).when(session).send(any());
	}

	@Test
	void testBankingDepositSuccess() {
		// Player has 1.000.000.000 adenas
		ItemInstance adena = mock(ItemInstance.class);
		when(adena.count()).thenReturn(1_000_000_000);
		when(inventory.byItemId(57)).thenReturn(Optional.of(adena));

		boolean result = bankingService.deposit(session, 1);
		assertTrue(result);

		verify(inventoryService).consumeItem(eq(inventory), eq(57), eq(500_000_000), eq("BankingDeposit"));
		verify(inventoryService).addItem(eq(inventory), eq(3470), eq(1), eq("BankingDeposit"));
	}

	@Test
	void testBankingDepositInsufficientAdena() {
		// Player has only 10.000 adenas
		ItemInstance adena = mock(ItemInstance.class);
		when(adena.count()).thenReturn(10_000);
		when(inventory.byItemId(57)).thenReturn(Optional.of(adena));

		boolean result = bankingService.deposit(session, 1);
		assertFalse(result);

		verify(inventoryService, never()).consumeItem(any(), anyInt(), anyInt(), anyString());
		verify(inventoryService, never()).addItem(any(), anyInt(), anyInt(), anyString());
	}

	@Test
	void testBankingDepositOverflowProtection() {
		// Solicitando quantidade que estoura Integer.MAX_VALUE
		boolean result = bankingService.deposit(session, 10);
		// 10 * 500.000.000 = 5.000.000.000 > Integer.MAX_VALUE
		assertFalse(result);
		verify(inventoryService, never()).consumeItem(any(), anyInt(), anyInt(), anyString());
	}

	@Test
	void testBankingWithdrawSuccess() {
		// Player has 2 Gold Bars and 100 adenas
		ItemInstance bar = mock(ItemInstance.class);
		when(bar.count()).thenReturn(2);
		ItemInstance adena = mock(ItemInstance.class);
		when(adena.count()).thenReturn(100);

		when(inventory.byItemId(3470)).thenReturn(Optional.of(bar));
		when(inventory.byItemId(57)).thenReturn(Optional.of(adena));

		boolean result = bankingService.withdraw(session, 1);
		assertTrue(result);

		verify(inventoryService).consumeItem(eq(inventory), eq(3470), eq(1), eq("BankingWithdraw"));
		verify(inventoryService).addItem(eq(inventory), eq(57), eq(500_000_000), eq("BankingWithdraw"));
	}

	@Test
	void testBankingWithdrawOverflowProtection() {
		// Player has 5 Gold Bars, mas já tem 1.900.000.000 adenas.
		// 1.900.000.000 + 500.000.000 = 2.400.000.000 > Integer.MAX_VALUE (2.147.483.647)
		ItemInstance bar = mock(ItemInstance.class);
		when(bar.count()).thenReturn(5);
		ItemInstance adena = mock(ItemInstance.class);
		when(adena.count()).thenReturn(1_900_000_000);

		when(inventory.byItemId(3470)).thenReturn(Optional.of(bar));
		when(inventory.byItemId(57)).thenReturn(Optional.of(adena));

		boolean result = bankingService.withdraw(session, 1);
		assertFalse(result);
		verify(inventoryService, never()).consumeItem(any(), anyInt(), anyInt(), anyString());
	}

	@Test
	void testBankingShowHelp() {
		when(inventory.byItemId(57)).thenReturn(Optional.empty());
		when(inventory.byItemId(3470)).thenReturn(Optional.empty());

		bankingService.showBankHelp(session);
		assertEquals(1, sentPackets.size());
		assertTrue(sentPackets.get(0) instanceof GameServerPacket.NpcHtmlMessage);
	}

	@Test
	void testAwayStatusLifecycle() {
		when(player.title()).thenReturn("Sir Knight");
		when(player.isDead()).thenReturn(false);
		when(player.isInCombat()).thenReturn(false);
		when(player.pvpFlag()).thenReturn(0);
		when(player.karma()).thenReturn(0);
		when(player.isOlympiadMode()).thenReturn(false);

		// Ativa .away
		boolean awayResult = awayStatusService.setAway(session, "Almoco");
		assertTrue(awayResult);
		assertTrue(awayStatusService.isAway(1001));
		verify(player).title("[AFK - Almoco]");
		verify(session).broadcastAppearance();

		// Tentar ficar away novamente deve falhar
		assertFalse(awayStatusService.setAway(session, "Novamente"));

		// Retorna com .back
		boolean backResult = awayStatusService.setBack(session);
		assertTrue(backResult);
		assertFalse(awayStatusService.isAway(1001));
		verify(player).title("Sir Knight");
	}

	@Test
	void testAwayStatusBlockedInCombatOrDead() {
		when(player.title()).thenReturn("Mage");
		when(player.isDead()).thenReturn(true);
		assertFalse(awayStatusService.setAway(session, ""));

		when(player.isDead()).thenReturn(false);
		when(player.isInCombat()).thenReturn(true);
		assertFalse(awayStatusService.setAway(session, ""));

		when(player.isInCombat()).thenReturn(false);
		when(player.karma()).thenReturn(500);
		assertFalse(awayStatusService.setAway(session, ""));
	}

	@Test
	void testAwayStatusAutoRemoveOnAction() {
		when(player.title()).thenReturn("OriginalTitle");
		when(player.isDead()).thenReturn(false);
		when(player.isInCombat()).thenReturn(false);

		awayStatusService.setAway(session, "");
		assertTrue(awayStatusService.isAway(1001));

		// Jogador move-se
		boolean removed = awayStatusService.checkAndRemoveAway(session);
		assertTrue(removed);
		assertFalse(awayStatusService.isAway(1001));
		verify(player).title("OriginalTitle");
	}

	@Test
	void testPlayerPreferencesWithCharacterVariables() {
		CharacterVariablesService charVars = mock(CharacterVariablesService.class);
		when(charVars.getBoolean(eq(999), eq("pref_autoloot"), anyBoolean())).thenReturn(false);
		when(charVars.getBoolean(eq(999), eq("pref_blockbuff"), anyBoolean())).thenReturn(true);

		PlayerPreferencesService prefs = new PlayerPreferencesService(charVars);
		var p = prefs.getPreferences(999);
		assertFalse(p.isAutoLoot());
		assertTrue(p.isBlockBuffs());

		// Toggling should persist to charVars
		prefs.toggleAutoLoot(999);
		verify(charVars).set(999, "pref_autoloot", true);

		prefs.toggleBlockParty(999);
		verify(charVars).set(999, "pref_blockparty", true);
	}
}
