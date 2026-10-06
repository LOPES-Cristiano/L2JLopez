package com.lopez.l2j.game.event.pvp;

import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TvtEventServiceTest {

    private TvtEventService tvtService;

    @BeforeEach
    void setUp() {
        tvtService = new TvtEventService();
    }

    private PlayerCharacter createPlayer(int objectId, String name, int level) {
        return new PlayerCharacter(objectId, "acc1", name, level, 0, 0, 0, 1, 1, false,
                0, 0, 0, 1000, 500, 500, 0, 0, 0, 0, "Title", 0, 0, 0,
                10000, 20000, -3000, 0, 1000.0, 500.0, 500.0);
    }

    @Test
    void testRegistrationLifecycle() {
        assertEquals(TvtEventService.EventState.INACTIVE, tvtService.getState());

        PlayerCharacter p1 = createPlayer(101, "Alice", 75);
        assertEquals(TvtEventService.RegisterResult.NOT_IN_REGISTRATION, tvtService.register(p1));

        tvtService.openRegistration(70, 80);
        assertEquals(TvtEventService.EventState.REGISTRATION, tvtService.getState());

        // Level too low
        PlayerCharacter lowLvl = createPlayer(102, "Lowbie", 60);
        assertEquals(TvtEventService.RegisterResult.LEVEL_TOO_LOW, tvtService.register(lowLvl));

        // Normal registration
        assertEquals(TvtEventService.RegisterResult.SUCCESS, tvtService.register(p1));
        assertEquals(TvtEventService.RegisterResult.ALREADY_REGISTERED, tvtService.register(p1));
        assertTrue(tvtService.isParticipant(101));

        // Unregister
        assertTrue(tvtService.unregister(p1));
        assertFalse(tvtService.isParticipant(101));
    }

    @Test
    void testTeamBalancingAndCombat() {
        tvtService.openRegistration(60, 80);

        PlayerCharacter p1 = createPlayer(1, "PlayerOne", 78);
        PlayerCharacter p2 = createPlayer(2, "PlayerTwo", 76);
        PlayerCharacter p3 = createPlayer(3, "PlayerThree", 70);
        PlayerCharacter p4 = createPlayer(4, "PlayerFour", 68);

        tvtService.register(p1);
        tvtService.register(p2);
        tvtService.register(p3);
        tvtService.register(p4);

        assertTrue(tvtService.startFight());
        assertEquals(TvtEventService.EventState.RUNNING, tvtService.getState());

        var part1 = tvtService.getParticipant(1);
        var part2 = tvtService.getParticipant(2);
        assertNotNull(part1);
        assertNotNull(part2);
        assertNotEquals(part1.getTeam(), part2.getTeam(), "Teams should be balanced");

        // p1 kills p2 (opposite team)
        assertTrue(tvtService.onKill(1, 2));
        assertEquals(1, part1.getKills());
        assertEquals(1, tvtService.getTeamScore(part1.getTeam()));

        // Friendly fire: p1 kills teammate should be rejected
        int teammateId = part1.getTeam() == tvtService.getParticipant(3).getTeam() ? 3 : 4;
        assertFalse(tvtService.onKill(1, teammateId));

        // Stop and calculate winner
        TvtEventService.TeamType winner = tvtService.stopAndCalculateWinner();
        assertEquals(part1.getTeam(), winner);
        assertEquals(TvtEventService.EventState.INACTIVE, tvtService.getState());
    }

    @Test
    void testStatusHtml() {
        tvtService.openRegistration(70, 80);
        String html = tvtService.buildStatusHtml();
        assertNotNull(html);
        assertTrue(html.contains("REGISTRATION"));
        assertTrue(html.contains("voiced_tvtjoin"));
    }
}
