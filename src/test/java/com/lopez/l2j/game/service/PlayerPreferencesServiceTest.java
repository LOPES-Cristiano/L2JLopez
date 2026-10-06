package com.lopez.l2j.game.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerPreferencesServiceTest {

    private PlayerPreferencesService prefService;

    @BeforeEach
    void setUp() {
        prefService = new PlayerPreferencesService();
    }

    @Test
    void testToggles() {
        int playerId = 777;

        var pref = prefService.getPreferences(playerId);
        assertTrue(pref.isAutoLoot());
        assertFalse(pref.isTradeRefusal());
        assertFalse(pref.isBlockBuffs());

        // Toggle autoloot
        assertFalse(prefService.toggleAutoLoot(playerId));
        assertTrue(prefService.toggleAutoLoot(playerId));

        // Toggle trade refusal
        assertTrue(prefService.toggleTradeRefusal(playerId));
        assertFalse(prefService.toggleTradeRefusal(playerId));

        // Toggle block buffs
        assertTrue(prefService.toggleBlockBuffs(playerId));
        assertFalse(prefService.toggleBlockBuffs(playerId));

        // Toggle party
        assertTrue(prefService.toggleBlockParty(playerId));

        // Toggle exp lock
        assertTrue(prefService.toggleBlockExp(playerId));
    }

    @Test
    void testMenuHtml() {
        String html = prefService.buildMenuHtml(777, "PlayerX");
        assertTrue(html.contains(".menu"));
        assertTrue(html.contains("PlayerX"));
        assertTrue(html.contains("voiced_menutoggle autoloot"));
    }
}
