package com.lopez.l2j.game.offlinetrade;

import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OfflineTradeServiceTest {

    private OfflineTradeService offlineTradeService;

    private PlayerCharacter dummyPlayer(int id, String name, int level) {
        return new PlayerCharacter(id, "acc", name, level, 0L, 0, 0, 0, 0, false,
                0, 0, 0, 1000, 500, 300, 0, 0, 0, 0, "", 0, 0L, 0L, 0, 0, 0, 0, 1000.0, 500.0, 300.0);
    }

    private boolean originalAllowOfflineTrade;

    @BeforeEach
    void setUp() {
        originalAllowOfflineTrade = com.lopez.l2j.config.Config.ALLOW_OFFLINE_TRADE;
        com.lopez.l2j.config.Config.ALLOW_OFFLINE_TRADE = true;
        // Testando com jdbc nulo para focar na lógica de estado, validacao e ciclo de vida
        offlineTradeService = new OfflineTradeService(null);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        com.lopez.l2j.config.Config.ALLOW_OFFLINE_TRADE = originalAllowOfflineTrade;
    }

    @Test
    void testStartOfflineTradeSuccess() {
        PlayerCharacter player = dummyPlayer(1, "MerchantBob", 52);
        List<OfflineShopItem> items = List.of(
                new OfflineShopItem(57, 1000000, 1),
                new OfflineShopItem(1463, 50, 1500)
        );

        boolean started = offlineTradeService.startOfflineTrade(
                player,
                OfflineTradeService.STORE_PRIVATE_SELL,
                false,
                "Enchanted Soulshots and Adena!",
                items,
                3600000L // 1 hora
        );

        assertTrue(started);
        assertTrue(offlineTradeService.isOfflineTrader(1));
        assertEquals(1, offlineTradeService.getActiveTraderCount());

        OfflineTrader trader = offlineTradeService.getTrader(1);
        assertNotNull(trader);
        assertEquals(1, trader.charId());
        assertEquals("Enchanted Soulshots and Adena!", trader.title());
        assertEquals(2, trader.items().size());
        assertFalse(trader.isExpired());
    }

    @Test
    void testStartOfflineTradeRejectsDeadOrKarmaOrEmptyItems() {
        PlayerCharacter deadPlayer = dummyPlayer(1, "DeadTrader", 52);
        deadPlayer.currentHp(0);

        boolean deadResult = offlineTradeService.startOfflineTrade(
                deadPlayer, OfflineTradeService.STORE_PRIVATE_SELL, false, "title",
                List.of(new OfflineShopItem(57, 100, 1)), 3600000L);
        assertFalse(deadResult);

        PlayerCharacter pkPlayer = dummyPlayer(2, "PKTrader", 52);
        pkPlayer.karma(500);

        boolean pkResult = offlineTradeService.startOfflineTrade(
                pkPlayer, OfflineTradeService.STORE_PRIVATE_SELL, false, "title",
                List.of(new OfflineShopItem(57, 100, 1)), 3600000L);
        assertFalse(pkResult);

        PlayerCharacter validPlayer = dummyPlayer(3, "EmptyTrader", 52);
        boolean emptyResult = offlineTradeService.startOfflineTrade(
                validPlayer, OfflineTradeService.STORE_PRIVATE_SELL, false, "title",
                List.of(), 3600000L);
        assertFalse(emptyResult);
    }

    @Test
    void testStopOfflineTrade() {
        PlayerCharacter player = dummyPlayer(1, "MerchantBob", 52);
        List<OfflineShopItem> items = List.of(new OfflineShopItem(57, 1000, 1));

        offlineTradeService.startOfflineTrade(
                player, OfflineTradeService.STORE_PRIVATE_SELL, false, "title", items, 3600000L);
        assertTrue(offlineTradeService.isOfflineTrader(1));

        offlineTradeService.stopOfflineTrade(1);
        assertFalse(offlineTradeService.isOfflineTrader(1));
        assertEquals(0, offlineTradeService.getActiveTraderCount());
    }

    @Test
    void testCheckExpiredTraders() {
        PlayerCharacter player = dummyPlayer(1, "OldMerchant", 52);
        List<OfflineShopItem> items = List.of(new OfflineShopItem(57, 1000, 1));

        // Inicia com duracao negativa (-1000ms), portanto ja expirado
        offlineTradeService.startOfflineTrade(
                player, OfflineTradeService.STORE_PRIVATE_SELL, false, "title", items, -1000L);

        OfflineTrader trader = offlineTradeService.getTrader(1);
        assertNotNull(trader);
        assertTrue(trader.isExpired());

        int expired = offlineTradeService.checkExpiredTraders();
        assertEquals(1, expired);
        assertFalse(offlineTradeService.isOfflineTrader(1));
    }
}
