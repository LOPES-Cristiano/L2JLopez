package com.lopez.l2j.game.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PvPRankServiceTest {

    private PvPRankService rankService;

    @BeforeEach
    void setUp() {
        rankService = new PvPRankService();
        rankService.setMinKillIntervalMinutes(10);
    }

    @Test
    void testAntiFeedSameIp() {
        // Killer e vitima com mesmo IP -> anti-feed bloqueia
        int points = rankService.registerPvPKill(1, "192.168.1.100", 2, "192.168.1.100", false);
        assertEquals(0, points);
        assertEquals(0, rankService.getPlayerPoints(1));
    }

    @Test
    void testAntiFeedKillCooldown() {
        // Primeiro abate: aceito
        int pts1 = rankService.registerPvPKill(1, "10.0.0.1", 2, "10.0.0.2", false);
        assertEquals(10, pts1);
        assertEquals(10, rankService.getPlayerPoints(1));

        // Segundo abate imediato do mesmo alvo: bloqueado pelo cooldown
        int pts2 = rankService.registerPvPKill(1, "10.0.0.1", 2, "10.0.0.2", false);
        assertEquals(0, pts2);
        assertEquals(10, rankService.getPlayerPoints(1));

        // Abate de outro alvo: aceito
        int pts3 = rankService.registerPvPKill(1, "10.0.0.1", 3, "10.0.0.3", false);
        assertEquals(10, pts3);
        assertEquals(20, rankService.getPlayerPoints(1));
    }

    @Test
    void testRankProgressionAndDecay() {
        int killerId = 10;
        rankService.setPlayerPoints(killerId, 250);

        PvPRankService.RankProgression prog = rankService.getRankProgression(killerId);
        assertEquals("Newbie", prog.rankName());
        assertEquals(2, prog.tierLevel());
        assertEquals(250, prog.currentPoints());

        // Decay diario
        rankService.applyDailyDecay();
        assertTrue(rankService.getPlayerPoints(killerId) < 250);
    }
}
