package com.lopez.l2j.game.service;

import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AioServiceTest {

    private AioService aioService;

    @BeforeEach
    void setUp() {
        aioService = new AioService();
    }

    private PlayerCharacter createPlayer(int objectId, String name) {
        return new PlayerCharacter(objectId, "acc1", name, 78, 0, 0, 0, 1, 1, false,
                0, 0, 0, 1000, 500, 500, 0, 0, 0, 0, "Title", 0, 0, 0,
                10000, 20000, -3000, 0, 1000.0, 500.0, 500.0);
    }

    @Test
    void testAioStatusAndPeaceZoneRestriction() {
        PlayerCharacter player = createPlayer(100, "SuperBuffer");

        assertFalse(player.isAio());
        assertFalse(aioService.isAio(100));

        // Concede status AIOx
        aioService.setAioStatus(player, true);
        assertTrue(player.isAio());
        assertTrue(aioService.isAio(100));

        // Restricao territorial: fora de zona de paz (peace zone = false) -> bloqueado
        assertFalse(aioService.canCastBuffs(player, false));
        // Dentro de zona de paz (peace zone = true) -> permitido
        assertTrue(aioService.canCastBuffs(player, true));

        // Jogador comum pode usar skills em qualquer lugar
        PlayerCharacter normal = createPlayer(101, "NormalChar");
        assertTrue(aioService.canCastBuffs(normal, false));
    }

    @Test
    void testAioGoodsAndMenu() {
        PlayerCharacter player = createPlayer(100, "BufferGuy");
        aioService.setAioStatus(player, true);

        assertFalse(aioService.getAioBuffs().isEmpty());
        assertFalse(aioService.getAioGoods().isEmpty());

        String html = aioService.buildAioMenuHtml(player);
        assertTrue(html.contains("Buffer AIOx"));
        assertTrue(html.contains(".getaiogoods"));
    }
}
