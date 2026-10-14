package com.lopez.l2j.game.service;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MonsterRaceServiceTest {

    private MonsterRaceService raceService;

    @BeforeEach
    void setUp() {
        raceService = new MonsterRaceService();
    }

    private PlayerCharacter createPlayer(int id, String name, int initialAdena) {
        PlayerCharacter player = new PlayerCharacter(id, "acc", name, 40, 0, 0, 0, 0, 0, false,
                0, 0, 0, 1000, 500, 500, 0, 0, 0, 0, "", 0, 0, 0,
                0, 0, 0, 0, 1000, 500, 500);
        Inventory inv = new Inventory(id);
        if (initialAdena > 0) {
            inv.addAdena(initialAdena);
        }
        player.inventory(inv);
        return player;
    }

    @Test
    void testMonsterRaceSimulation() {
        int initialRaceId = raceService.getCurrentRaceId();

        var outcome = raceService.runRace();
        assertEquals(initialRaceId, outcome.raceId());
        assertTrue(outcome.firstPlaceLane() >= 1 && outcome.firstPlaceLane() <= 8);
        assertTrue(outcome.secondPlaceLane() >= 1 && outcome.secondPlaceLane() <= 8);
        assertNotEquals(outcome.firstPlaceLane(), outcome.secondPlaceLane());
        assertEquals(initialRaceId + 1, raceService.getCurrentRaceId());
    }

    @Test
    void testBettingAndPayout() {
        // Registra aposta simples na Raia 3
        Optional<MonsterRaceService.RaceBet> optBet = raceService.placeBet(10, "Gambler", 3, 0, 1000);
        assertTrue(optBet.isPresent());

        MonsterRaceService.RaceBet bet = optBet.get();

        // Se a raia 3 vencer:
        MonsterRaceService.RaceOutcome winOutcome = new MonsterRaceService.RaceOutcome(1, 3, 5, new int[8][20]);
        int payoutWin = raceService.calculatePayout(bet, winOutcome);
        assertTrue(payoutWin > 0);

        // Se a raia 4 vencer:
        MonsterRaceService.RaceOutcome loseOutcome = new MonsterRaceService.RaceOutcome(1, 4, 3, new int[8][20]);
        int payoutLose = raceService.calculatePayout(bet, loseOutcome);
        assertEquals(0, payoutLose);
    }

    @Test
    void testPlayerPlaceBetAndRunRaceAndSettle() {
        PlayerCharacter player = createPlayer(50, "DerbyPlayer", 10000);
        // Aposta 1000 em todas as 8 raias para garantir que uma venca
        for (int lane = 1; lane <= 8; lane++) {
            assertTrue(raceService.placeBet(player, lane, 1000));
        }
        assertEquals(2000L, player.inventory().adena());

        int won = raceService.runRaceAndSettlePlayer(player);
        assertEquals(3500, won);
        assertEquals(5500L, player.inventory().adena());
    }
}

