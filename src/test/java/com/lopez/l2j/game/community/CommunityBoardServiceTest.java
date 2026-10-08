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
        com.lopez.l2j.config.Config.load();
        com.lopez.l2j.config.Config.COMMUNITY_TYPE = "Full";
        com.lopez.l2j.config.Config.RESTRICT_CB_WHEN = "JAIL";
        com.lopez.l2j.config.Config.COMMUNITY_BUFFER_EXCLUDE_ON = "RB OLYMPIAD PVP SIEGE EVENT ATTACK NOTINTOWN TRADE";
        com.lopez.l2j.config.Config.GATEKEEPER_EXCLUDE_ON = "RB OLYMPIAD PVP SIEGE EVENT ATTACK NOTINTOWN TRADE";
        com.lopez.l2j.config.Config.DISABLED_PAGES = "";
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

    @Test
    void testCommunityTypeOffDisablesBBS() {
        com.lopez.l2j.config.Config.COMMUNITY_TYPE = "off";
        PlayerCharacter player = dummyPlayer(1, "BBSPlayer", 75);

        String result = communityBoardService.handleCommand(player, "_bbshome");
        assertTrue(result.contains("desativado no momento"));

        com.lopez.l2j.config.Config.COMMUNITY_TYPE = "Full";
    }

    @Test
    void testRestrictedConditionsBlockBBS() {
        try {
            com.lopez.l2j.config.Config.RESTRICT_CB_WHEN = "JAIL COMBAT OLY";
            PlayerCharacter player = dummyPlayer(1, "JailedPlayer", 75);
            player.setInJail(true);

            String resJail = communityBoardService.handleCommand(player, "_bbshome");
            assertTrue(resJail.contains("restrito"), () -> "Expected restrito for JAIL but got: " + resJail);

            player.setInJail(false);
            player.setInCombat(true);
            String resCombat = communityBoardService.handleCommand(player, "_bbshome");
            assertTrue(resCombat.contains("restrito"), () -> "Expected restrito for COMBAT but got: " + resCombat);

            player.setInCombat(false);
            player.setOlympiadMode(true);
            String resOly = communityBoardService.handleCommand(player, "_bbshome");
            assertTrue(resOly.contains("restrito"), () -> "Expected restrito for OLY but got: " + resOly);
        } finally {
            com.lopez.l2j.config.Config.RESTRICT_CB_WHEN = "JAIL";
        }
    }

    @Test
    void testBufferAndGatekeeperExclusions() {
        try {
            com.lopez.l2j.config.Config.RESTRICT_CB_WHEN = "";
            com.lopez.l2j.config.Config.COMMUNITY_BUFFER_EXCLUDE_ON = "OLYMPIAD SIEGE PVP";
            com.lopez.l2j.config.Config.GATEKEEPER_EXCLUDE_ON = "OLYMPIAD SIEGE PVP";

            PlayerCharacter player = dummyPlayer(1, "CombatPlayer", 75);
            player.setInCombat(true);

            String buffRes = communityBoardService.handleCommand(player, "_bbsbuffer");
            assertTrue(buffRes.contains("servico de Buff esta desativado"), () -> "Expected buff exclusion but got: " + buffRes);

            String teleRes = communityBoardService.handleCommand(player, "_bbsteleport");
            assertTrue(teleRes.contains("servico de Teleporte esta desativado"), () -> "Expected teleport exclusion but got: " + teleRes);

            player.setInCombat(false);
            player.setInSiege(true);

            String buffRes2 = communityBoardService.handleCommand(player, "_bbsbuff_heal");
            assertTrue(buffRes2.contains("servico de Buff esta desativado"), () -> "Expected buff exclusion for siege but got: " + buffRes2);

            String teleRes2 = communityBoardService.handleCommand(player, "_bbsteleport_to 1");
            assertTrue(teleRes2.contains("servico de Teleporte esta desativado"), () -> "Expected teleport exclusion for siege but got: " + teleRes2);
        } finally {
            com.lopez.l2j.config.Config.RESTRICT_CB_WHEN = "JAIL";
            com.lopez.l2j.config.Config.COMMUNITY_BUFFER_EXCLUDE_ON = "RB OLYMPIAD PVP SIEGE EVENT ATTACK NOTINTOWN TRADE";
            com.lopez.l2j.config.Config.GATEKEEPER_EXCLUDE_ON = "RB OLYMPIAD PVP SIEGE EVENT ATTACK NOTINTOWN TRADE";
        }
    }

    @Test
    void testDisabledPages() {
        com.lopez.l2j.config.Config.RESTRICT_CB_WHEN = "";
        com.lopez.l2j.config.Config.DISABLED_PAGES = "_bbsteleport _bbsbuffer";

        PlayerCharacter player = dummyPlayer(1, "NormalPlayer", 75);
        String teleRes = communityBoardService.handleCommand(player, "_bbsteleport");
        assertTrue(teleRes.contains("desativada pelo administrador"));

        String homeRes = communityBoardService.handleCommand(player, "_bbshome");
        assertFalse(homeRes.contains("desativada pelo administrador"));

        com.lopez.l2j.config.Config.DISABLED_PAGES = "";
    }
}
