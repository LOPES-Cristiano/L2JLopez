package com.lopez.l2j.game.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LotteryServiceTest {

    private LotteryService lotteryService;

    @BeforeEach
    void setUp() {
        lotteryService = new LotteryService();
    }

    @Test
    void testTicketPurchaseValidation() {
        // Quantidade errada de numeros (< 5)
        Optional<LotteryService.LotteryTicket> invalid = lotteryService.buyTicket(1, "Player1", Set.of(1, 2, 3));
        assertTrue(invalid.isEmpty());

        // Numero fora da faixa (> 20)
        Optional<LotteryService.LotteryTicket> outOfRange = lotteryService.buyTicket(1, "Player1", Set.of(1, 2, 3, 4, 25));
        assertTrue(outOfRange.isEmpty());

        // Compra valida
        Optional<LotteryService.LotteryTicket> valid = lotteryService.buyTicket(1, "Player1", Set.of(2, 5, 8, 12, 19));
        assertTrue(valid.isPresent());
        assertEquals(1, lotteryService.getCurrentTickets().size());
    }

    @Test
    void testDrawLotteryCycle() {
        lotteryService.buyTicket(1, "Player1", Set.of(1, 2, 3, 4, 5));
        int initialLotteryId = lotteryService.getCurrentLotteryId();

        var result = lotteryService.drawLottery();
        assertEquals(initialLotteryId, result.lotteryId());
        assertEquals(5, result.winningNumbers().size());
        assertTrue(lotteryService.getCurrentTickets().isEmpty(), "Ingressos devem resetar apos o sorteio");
        assertEquals(initialLotteryId + 1, lotteryService.getCurrentLotteryId());
    }
}
