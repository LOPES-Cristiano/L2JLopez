package com.lopez.l2j.game.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BotsPreventionServiceTest {

    private BotsPreventionService botsService;

    @BeforeEach
    void setUp() {
        botsService = new BotsPreventionService();
        botsService.setBaseKillsCounter(5);
        botsService.setKillsRandomization(0); // Fixo em 5 kills para testes deterministicos
        botsService.setValidationTimeoutSeconds(60);
    }

    @Test
    void testMobKillTriggerAndSuccessfulAnswer() {
        int playerId = 1234;

        // 4 kills: nenhum desafio
        for (int i = 0; i < 4; i++) {
            Optional<BotsPreventionService.CaptchaChallenge> challenge = botsService.onMobKill(playerId);
            assertTrue(challenge.isEmpty());
        }

        // 5º kill: gera desafio
        Optional<BotsPreventionService.CaptchaChallenge> opt = botsService.onMobKill(playerId);
        assertTrue(opt.isPresent());
        var challenge = opt.get();
        assertEquals(4, challenge.getOptions().size());
        assertTrue(challenge.getOptions().contains(challenge.getCorrectNumber()));
        assertTrue(botsService.hasPendingChallenge(playerId));

        // Resposta correta
        boolean ok = botsService.validateAnswer(playerId, challenge.getCorrectNumber());
        assertTrue(ok);
        assertFalse(botsService.hasPendingChallenge(playerId));
    }

    @Test
    void testIncorrectAnswerAndPunishment() {
        int playerId = 5678;

        for (int i = 0; i < 5; i++) {
            botsService.onMobKill(playerId);
        }

        var challenge = botsService.getPendingChallenge(playerId);
        assertNotNull(challenge);

        // Resposta errada
        int wrongNum = challenge.getCorrectNumber() + 1;
        assertFalse(botsService.validateAnswer(playerId, wrongNum));
        assertTrue(botsService.hasPendingChallenge(playerId));
    }
}
