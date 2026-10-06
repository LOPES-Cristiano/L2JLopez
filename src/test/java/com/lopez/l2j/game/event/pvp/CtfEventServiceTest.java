package com.lopez.l2j.game.event.pvp;

import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CtfEventServiceTest {

    private CtfEventService ctfService;

    @BeforeEach
    void setUp() {
        ctfService = new CtfEventService();
    }

    private PlayerCharacter createPlayer(int objectId, String name, int level) {
        return new PlayerCharacter(objectId, "acc1", name, level, 0, 0, 0, 1, 1, false,
                0, 0, 0, 1000, 500, 500, 0, 0, 0, 0, "Title", 0, 0, 0,
                10000, 20000, -3000, 0, 1000.0, 500.0, 500.0);
    }

    @Test
    void testFlagCaptureWorkflow() {
        ctfService.openRegistration(60, 80);

        PlayerCharacter p1 = createPlayer(1, "BlueRunner", 78);
        PlayerCharacter p2 = createPlayer(2, "RedDefender", 76);

        ctfService.register(p1);
        ctfService.register(p2);

        assertTrue(ctfService.startFight());
        assertEquals(CtfEventService.EventState.RUNNING, ctfService.getState());

        var part1 = ctfService.getParticipant(1);
        var part2 = ctfService.getParticipant(2);
        assertNotEquals(part1.getTeam(), part2.getTeam());

        // Player 1 takes enemy flag
        assertTrue(ctfService.takeEnemyFlag(1));
        assertEquals(CtfEventService.FlagStatus.CARRIED, ctfService.getFlagStatus(part2.getTeam()));
        assertTrue(ctfService.isFlagCarrier(1));

        // Player 1 brings enemy flag to own base and scores!
        assertTrue(ctfService.captureFlag(1));
        assertEquals(1, part1.getFlagCaptures());
        assertEquals(1, ctfService.getTeamScore(part1.getTeam()));
        assertEquals(CtfEventService.FlagStatus.IN_BASE, ctfService.getFlagStatus(part2.getTeam()));
        assertFalse(ctfService.isFlagCarrier(1));

        // Winner check
        CtfEventService.TeamType winner = ctfService.stopAndCalculateWinner();
        assertEquals(part1.getTeam(), winner);
        assertEquals(CtfEventService.EventState.INACTIVE, ctfService.getState());
    }

    @Test
    void testFlagDropAndReturn() {
        ctfService.openRegistration(60, 80);
        ctfService.register(createPlayer(1, "Runner1", 78));
        ctfService.register(createPlayer(2, "Runner2", 76));
        ctfService.startFight();

        var part1 = ctfService.getParticipant(1);
        var part2 = ctfService.getParticipant(2);

        // Player 1 takes enemy flag
        assertTrue(ctfService.takeEnemyFlag(1));
        // Player 1 is killed, flag drops
        assertTrue(ctfService.dropFlag(1));
        assertEquals(CtfEventService.FlagStatus.DROPPED, ctfService.getFlagStatus(part2.getTeam()));

        // Enemy player 2 touches their own dropped flag to return it
        assertTrue(ctfService.returnFlagToBase(2, part2.getTeam()));
        assertEquals(CtfEventService.FlagStatus.IN_BASE, ctfService.getFlagStatus(part2.getTeam()));
    }
}
