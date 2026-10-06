package com.lopez.l2j.game.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PetitionServiceTest {

    private PetitionService petitionService;

    @BeforeEach
    void setUp() {
        petitionService = new PetitionService();
    }

    @Test
    void testPetitionSubmissionAndResolution() {
        // Submissao pelo jogador
        Optional<PetitionService.PetitionTicket> opt = petitionService.submitPetition(100, "StuckPlayer", 1, "Estou preso na pedra de Giran!");
        assertTrue(opt.isPresent());
        var ticket = opt.get();
        assertEquals(PetitionService.PetitionState.PENDING, ticket.getState());
        assertEquals(1, petitionService.getPendingPetitions().size());

        // GM aceita atendimento
        assertTrue(petitionService.acceptPetition(999, "AdminGM", ticket.getId()));
        assertEquals(PetitionService.PetitionState.IN_PROCESS, ticket.getState());
        assertEquals("AdminGM", ticket.getGmResponderName());

        // Troca de mensagens no canal privativo da peticao
        assertTrue(petitionService.sendMessage(ticket.getId(), "AdminGM", "Ola! Vou teleporta-lo agora."));
        assertEquals(2, ticket.getMessages().size());

        // Encerramento
        assertTrue(petitionService.closePetition(ticket.getId()));
        assertEquals(PetitionService.PetitionState.COMPLETED, ticket.getState());
        assertEquals(0, petitionService.getPendingPetitions().size());
    }

    @Test
    void testCannotSubmitMultipleSimultaneousPetitions() {
        petitionService.submitPetition(100, "PlayerOne", 1, "Help 1");
        // Segunda peticao enquanto a primeira esta aberta deve ser rejeitada
        Optional<PetitionService.PetitionTicket> second = petitionService.submitPetition(100, "PlayerOne", 2, "Help 2");
        assertTrue(second.isEmpty());
    }
}
