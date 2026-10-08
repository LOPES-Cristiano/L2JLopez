package com.lopez.l2j.game.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.buffshop.BuffShopService;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.offlinetrade.OfflineShopItem;
import com.lopez.l2j.game.offlinetrade.OfflineTradeService;
import com.lopez.l2j.network.game.GameSession;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ModsAndSecurityConfigurationTest {

	private BotsPreventionService botsService;
	private OfflineTradeService offlineTradeService;
	private BuffShopService buffShopService;
	private PvpRewardService pvpRewardService;

	private PlayerCharacter createPlayer(int objId, String name, int level) {
		return new PlayerCharacter(objId, "acc", name, level, 0L, 0, 0, 0, 0, false, 0, 0, 0,
				1000, 1000, 1000, 0, 0, 0, 0, "Title", 0, 0L, 0L, 0, 0, 0, 0, 1000.0, 1000.0, 1000.0);
	}

	private ItemTemplate createItemTemplate(int id, String name) {
		return new ItemTemplate(
				id, id, name, ItemTemplate.Kind.ETC, "none",
				ItemTemplate.TYPE1_ITEM_QUESTITEM_ADENA, ItemTemplate.TYPE2_OTHER,
				0, 10, true, "none", 100,
				0, 0, 0, 0, 0, 0, 0, true, true, true, true
		);
	}

	@BeforeEach
	void setUp() {
		Config.load();
		botsService = new BotsPreventionService();
		offlineTradeService = new OfflineTradeService(null);
		buffShopService = new BuffShopService(null);
		pvpRewardService = new PvpRewardService(null, null);
	}

	@Test
	void testCaptchaTriggerAfterKills() {
		botsService.setEnabled(true);
		botsService.setBaseKillsCounter(3);
		botsService.setKillsRandomization(0);

		int playerId = 999;
		assertFalse(botsService.onMobKill(playerId).isPresent());
		assertFalse(botsService.onMobKill(playerId).isPresent());

		var challengeOpt = botsService.onMobKill(playerId);
		assertTrue(challengeOpt.isPresent());
		assertTrue(challengeOpt.get().getCorrectNumber() >= 100);
	}

	@Test
	void testOfflineTradeRespectsConfig() {
		PlayerCharacter trader = createPlayer(1001, "TraderJoe", 70);
		var items = List.of(new OfflineShopItem(57, 100, 1000));

		// Disallowed offline trade
		Config.ALLOW_OFFLINE_TRADE = false;
		assertFalse(offlineTradeService.startOfflineTrade(trader, OfflineTradeService.STORE_PRIVATE_SELL, false, "Store", items, 0));

		// Allowed offline trade
		Config.ALLOW_OFFLINE_TRADE = true;
		Config.ALLOW_OFFLINE_TRADE_PROTECTION = true;
		Config.ALLOW_OFFLINE_TRADE_COLOR_NAME = true;
		Config.OFFLINE_TRADE_COLOR_NAME = "999999";

		assertTrue(offlineTradeService.startOfflineTrade(trader, OfflineTradeService.STORE_PRIVATE_SELL, false, "Store", items, 0));
		assertTrue(trader.invul(), "Trader must become invulnerable when protection is enabled");
		assertEquals(Integer.decode("0x999999").intValue(), trader.nameColor());

		// Disallowed offline craft
		Config.ALLOW_OFFLINE_TRADE_CRAFT = false;
		PlayerCharacter crafter = createPlayer(1002, "CrafterDwarf", 70);
		assertFalse(offlineTradeService.startOfflineTrade(crafter, OfflineTradeService.STORE_PRIVATE_MANUFACTURE, false, "Craft", items, 0));
	}

	@Test
	void testCustomCurrencyStoreSellByItem() {
		Config.SELL_BY_ITEM = true;
		Config.SELL_ITEM = 3470; // Gold Bar
		Config.COIN_TEXT = "Gold Bar";

		PlayerCharacter buyer = createPlayer(2001, "Buyer", 75);
		PlayerCharacter seller = createPlayer(2002, "Seller", 75);

		// Initialize seller buff shop
		buffShopService.startShop(seller, "Buffs for Gold", List.of(new BuffShopService.BuffShopItem(1068, 1, 5, "Might")));

		// Buyer has no Gold Bars
		var resNoMoney = buffShopService.purchaseBuffs(buyer, seller.objectId(), List.of(1068), seller, null, null, null);
		assertFalse(resNoMoney.success());
		assertTrue(resNoMoney.message().contains("Gold Bar"));

		// Buyer has 10 Gold Bars
		var goldBarTemplate = createItemTemplate(3470, "Gold Bar");
		buyer.inventory().add(new ItemInstance(3001, goldBarTemplate, buyer.objectId(), 10));

		var resSuccess = buffShopService.purchaseBuffs(buyer, seller.objectId(), List.of(1068), seller, null, null, null);
		assertTrue(resSuccess.success());
		assertEquals(5, resSuccess.totalCost());
	}

	@Test
	void testQuakePvPAndWarLegendHeroAura() {
		Config.ALLOW_QUAKE_SYSTEM = true;
		Config.WAR_LEGEND_AURA = true;
		Config.KILLS_TO_GET_WAR_LEGEND_AURA = 5;

		PlayerCharacter killer = createPlayer(3001, "Warlord", 80);
		PlayerCharacter victim = createPlayer(3002, "Victim", 80);
		victim.pvpFlag(1);

		GameSession killerSession = mock(GameSession.class);
		when(killerSession.activeCharacter()).thenReturn(killer);
		when(killerSession.ip()).thenReturn("192.168.1.10");

		GameSession victimSession = mock(GameSession.class);
		when(victimSession.activeCharacter()).thenReturn(victim);
		when(victimSession.ip()).thenReturn("192.168.1.20");

		assertFalse(killer.hero());

		// Execute 5 kills to trigger War Legend Hero Aura
		for (int i = 0; i < 5; i++) {
			pvpRewardService.handleKill(killerSession, victimSession);
		}

		assertEquals(5, killer.pvpKills());
		assertTrue(killer.hero(), "Killer should get Hero Aura after hitting KILLS_TO_GET_WAR_LEGEND_AURA");
		assertEquals(5, pvpRewardService.getKillStreak(killer.objectId()));

		// Test Quake message generation
		assertEquals("Warlord is on a Rampage!", PvpRewardService.getQuakeMessage("Warlord", 5));
		assertEquals("Warlord is GODLIKE!", PvpRewardService.getQuakeMessage("Warlord", 10));
	}
}
