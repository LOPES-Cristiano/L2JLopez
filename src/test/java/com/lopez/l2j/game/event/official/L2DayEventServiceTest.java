package com.lopez.l2j.game.event.official;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class L2DayEventServiceTest {

    private L2DayEventService l2dayService;

    @BeforeEach
    void setUp() {
        l2dayService = new L2DayEventService();
    }

    @Test
    void testWordRecipes() {
        assertTrue(l2dayService.isActive());

        var optLineage = l2dayService.getRecipe("LINEAGEII");
        assertTrue(optLineage.isPresent());
        assertEquals("LINEAGEII", optLineage.get().word());
        assertEquals(948, optLineage.get().rewardItemId());

        var optNcsoft = l2dayService.getRecipe("ncsoft");
        assertTrue(optNcsoft.isPresent());
        assertEquals("NCSOFT", optNcsoft.get().word());
    }

    @Test
    void testHasLettersInventoryCheck() {
        Map<Integer, Integer> inv = new HashMap<>();
        inv.put(L2DayEventService.LETTER_L, 1);
        inv.put(L2DayEventService.LETTER_I, 1);
        inv.put(L2DayEventService.LETTER_N, 1);
        inv.put(L2DayEventService.LETTER_E, 2);
        inv.put(L2DayEventService.LETTER_A, 1);
        inv.put(L2DayEventService.LETTER_G, 1);
        inv.put(L2DayEventService.LETTER_II, 1);

        assertTrue(l2dayService.hasLetters("LINEAGEII", inv));

        // Falta um E (apenas 1 ao inves de 2)
        inv.put(L2DayEventService.LETTER_E, 1);
        assertFalse(l2dayService.hasLetters("LINEAGEII", inv));
    }
}
