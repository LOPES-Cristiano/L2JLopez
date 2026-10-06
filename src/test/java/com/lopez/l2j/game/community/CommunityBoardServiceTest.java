package com.lopez.l2j.game.community;

import com.lopez.l2j.game.model.CharacterRepository;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.world.GameWorld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommunityBoardServiceTest {

    private CharacterRepository characterRepository;
    private GameWorld gameWorld;
    private CommunityBoardService communityBoardService;

    private PlayerCharacter dummyPlayer(int id, String name, int level) {
        return new PlayerCharacter(id, "acc", name, level, 0L, 0, 0, 0, 0, false,
                0, 0, 0, 1000, 500, 300, 0, 0, 0, 0, "", 0, 0L, 0L, 0, 0, 0, 0, 1000.0, 500.0, 300.0);
    }

    @BeforeEach
    void setUp() {
        characterRepository = Mockito.mock(CharacterRepository.class);
        gameWorld = Mockito.mock(GameWorld.class);
        communityBoardService = new CommunityBoardService(characterRepository, gameWorld);
    }

    @Test
    void testHomeHtmlRendersServerStatusAndTopRankings() {
        PlayerCharacter player = dummyPlayer(1, "HeroPlayer", 80);
        player.pvpKills(150);
        player.pkKills(10);

        PlayerCharacter topPvp1 = dummyPlayer(2, "GladiatorOne", 79);
        topPvp1.pvpKills(200);

        when(characterRepository.findTopPvP(5)).thenReturn(List.of(topPvp1));
        when(characterRepository.findTopPK(5)).thenReturn(List.of());
        when(gameWorld.players()).thenReturn(List.of());

        String html = communityBoardService.handleCommand(player, "_bbshome");
        assertNotNull(html);
        assertTrue(html.contains("HeroPlayer"));
        assertTrue(html.contains("GladiatorOne"));
        assertTrue(html.contains("PAINEL DA COMUNIDADE"));
    }

    @Test
    void testRankingsHtmlRendersLeaderboard() {
        PlayerCharacter player = dummyPlayer(1, "Viewer", 60);
        PlayerCharacter pvpChar = dummyPlayer(2, "PvPKiller", 78);
        pvpChar.pvpKills(88);

        when(characterRepository.findTopPvP(10)).thenReturn(List.of(pvpChar));
        when(characterRepository.findTopPK(10)).thenReturn(List.of());

        String html = communityBoardService.handleCommand(player, "_bbstop");
        assertTrue(html.contains("TOP 10 PVP"));
        assertTrue(html.contains("PvPKiller"));
        assertTrue(html.contains("TOP 10 PK"));
    }

    @Test
    void testTeleportHtmlAndExecution() {
        PlayerCharacter player = dummyPlayer(1, "Traveler", 70);

        // Verifica tela de menu de teleportes
        String menu = communityBoardService.handleCommand(player, "_bbsteleport");
        assertTrue(menu.contains("Town of Giran"));
        assertTrue(menu.contains("Town of Aden"));

        // Executa teleporte para Giran (loc 1: 83400, 147943, -3404)
        communityBoardService.handleCommand(player, "_bbsteleport_to 1");
        assertEquals(83400, player.x());
        assertEquals(147943, player.y());
        assertEquals(-3404, player.z());
    }

    @Test
    void testDeadPlayerCannotTeleport() {
        PlayerCharacter deadPlayer = dummyPlayer(1, "DeadGuy", 40);
        deadPlayer.currentHp(0); // morto

        String result = communityBoardService.handleCommand(deadPlayer, "_bbsteleport_to 1");
        assertTrue(result.contains("Voce nao pode se teletransportar enquanto estiver morto"));
        assertNotEquals(83400, deadPlayer.x());
    }

    @Test
    void testBufferRestoresHpMpCp() {
        PlayerCharacter player = dummyPlayer(1, "FighterPlayer", 75);
        player.currentHp(10);
        player.currentMp(5);
        player.currentCp(0);

        communityBoardService.handleCommand(player, "_bbsbuff_heal");

        assertEquals(player.maxHp(), player.currentHp());
        assertEquals(player.maxMp(), player.currentMp());
        assertEquals(player.maxCp(), player.currentCp());
    }
}
