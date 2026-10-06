package com.lopez.l2j.game.event.pvp;

import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DmEventServiceTest {

    private DmEventService dmService;

    @BeforeEach
    void setUp() {
        dmService = new DmEventService();
    }

    private PlayerCharacter createPlayer(int objectId, String name, int level) {
        return new PlayerCharacter(objectId, "acc1", name, level, 0, 0, 0, 1, 1, false,
                0, 0, 0, 1000, 500, 500, 0, 0, 0, 0, "Title", 0, 0, 0,
                10000, 20000, -3000, 0, 1000.0, 500.0, 500.0);
    }

    @Test
    void testDeathMatchFreeForAllRanking() {
        dmService.openRegistration(60, 80);

        PlayerCharacter p1 = createPlayer(1, "Gladiator1", 78);
        PlayerCharacter p2 = createPlayer(2, "Gladiator2", 76);
        PlayerCharacter p3 = createPlayer(3, "Gladiator3", 75);

        dmService.register(p1);
        dmService.register(p2);
        dmService.register(p3);

        assertTrue(dmService.startFight());
        assertEquals(DmEventService.EventState.RUNNING, dmService.getState());

        // Combat kills
        dmService.onKill(1, 2);
        dmService.onKill(1, 3);
        dmService.onKill(2, 3);

        List<DmEventService.Participant> ranking = dmService.getRanking();
        assertEquals(3, ranking.size());
        assertEquals(1, ranking.get(0).getObjectId()); // p1 with 2 kills
        assertEquals(2, ranking.get(0).getKills());
        assertEquals(2, ranking.get(1).getObjectId()); // p2 with 1 kill
        assertEquals(1, ranking.get(1).getKills());
        assertEquals(3, ranking.get(2).getObjectId()); // p3 with 0 kills

        // Event end and rewards
        List<DmEventService.Participant> winners = dmService.stopAndReward();
        assertEquals(3, winners.size());
        assertEquals(DmEventService.EventState.INACTIVE, dmService.getState());
    }

    @Test
    void testHtmlAndCommands() {
        dmService.openRegistration(60, 80);
        String html = dmService.buildStatusHtml();
        assertTrue(html.contains("DeathMatch"));
        assertTrue(html.contains("voiced_dmjoin"));
    }
}
