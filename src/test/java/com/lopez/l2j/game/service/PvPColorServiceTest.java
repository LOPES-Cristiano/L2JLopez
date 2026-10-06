package com.lopez.l2j.game.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PvPColorServiceTest {

    private PvPColorService colorService;

    @BeforeEach
    void setUp() {
        colorService = new PvPColorService();
    }

    @Test
    void testNameColorsByPvpTiers() {
        int defaultColor = 0xFFFFFF;

        // < 50 kills: padrao
        assertEquals(defaultColor, colorService.getNameColor(0, defaultColor));
        assertEquals(defaultColor, colorService.getNameColor(49, defaultColor));

        // 50 .. 99 kills: faixa 1 (Verde)
        assertEquals(colorService.getNameColor1(), colorService.getNameColor(50, defaultColor));
        assertEquals(colorService.getNameColor1(), colorService.getNameColor(99, defaultColor));

        // 100 .. 149 kills: faixa 2 (Amarelo)
        assertEquals(colorService.getNameColor2(), colorService.getNameColor(100, defaultColor));

        // 150 .. 249 kills: faixa 3 (Roxo)
        assertEquals(colorService.getNameColor3(), colorService.getNameColor(150, defaultColor));

        // 250 .. 499 kills: faixa 4 (Laranja)
        assertEquals(colorService.getNameColor4(), colorService.getNameColor(250, defaultColor));

        // >= 500 kills: faixa 5 (Vermelho)
        assertEquals(colorService.getNameColor5(), colorService.getNameColor(500, defaultColor));
        assertEquals(colorService.getNameColor5(), colorService.getNameColor(1500, defaultColor));
    }

    @Test
    void testDisabledColorSystem() {
        colorService.setEnabled(false);
        int defaultColor = 0xFFFFFF;
        assertEquals(defaultColor, colorService.getNameColor(1000, defaultColor));
    }
}
