package com.lopez.l2j.game.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Sistema de Suporte GM in-game (PetitionService - F10).
 * Fila de chamados para GMs online, aceitação de chamado, canal privativo de atendimento e encerramento.
 */
@Service
public class PetitionService {

    private static final Logger log = LoggerFactory.getLogger(PetitionService.class);

    public enum PetitionType {
        IMMOBILITY(1, "Personagem Preso / Travado"),
        RECOVERY(2, "Recuperacao de Itens / Conta"),
        BUG(3, "Relato de Falha / Bug"),
        QUEST(4, "Duvida de Missao / Quest"),
        BOT_REPORT(5, "Denuncia de Bot / Macro"),
        GENERAL(6, "Duvidas Gerais");

        private final int id;
        private final String description;

        PetitionType(int id, String description) {
            this.id = id;
            this.description = description;
        }

        public int getId() { return id; }
        public String getDescription() { return description; }

        public static PetitionType fromId(int id) {
            for (PetitionType t : values()) {
                if (t.id == id) return t;
            }
            return GENERAL;
        }
    }

    public enum PetitionState {
        PENDING,
        IN_PROCESS,
        COMPLETED,
        CANCELLED
    }

    public static class PetitionMessage {
        private final String senderName;
        private final String message;
        private final long timestamp;

        public PetitionMessage(String senderName, String message) {
            this.senderName = senderName;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }

        public String getSenderName() { return senderName; }
        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }
    }

    public static class PetitionTicket {
        private final int id;
        private final int petitionerId;
        private final String petitionerName;
        private final PetitionType type;
        private final String initialMessage;
        private PetitionState state;
        private Integer gmResponderId;
        private String gmResponderName;
        private final List<PetitionMessage> messages = new CopyOnWriteArrayList<>();

        public PetitionTicket(int id, int petitionerId, String petitionerName, PetitionType type, String initialMessage) {
            this.id = id;
            this.petitionerId = petitionerId;
            this.petitionerName = petitionerName;
            this.type = type;
            this.initialMessage = initialMessage;
            this.state = PetitionState.PENDING;
            this.messages.add(new PetitionMessage(petitionerName, initialMessage));
        }

        public int getId() { return id; }
        public int getPetitionerId() { return petitionerId; }
        public String getPetitionerName() { return petitionerName; }
        public PetitionType getType() { return type; }
        public String getInitialMessage() { return initialMessage; }
        public PetitionState getState() { return state; }
        public void setState(PetitionState state) { this.state = state; }
        public Integer getGmResponderId() { return gmResponderId; }
        public void setGmResponder(int gmId, String gmName) {
            this.gmResponderId = gmId;
            this.gmResponderName = gmName;
            this.state = PetitionState.IN_PROCESS;
        }
        public String getGmResponderName() { return gmResponderName; }
        public List<PetitionMessage> getMessages() { return Collections.unmodifiableList(messages); }
        public void addMessage(String sender, String text) { messages.add(new PetitionMessage(sender, text)); }
    }

    private int nextPetitionId = 1;
    private final Map<Integer, PetitionTicket> petitions = new ConcurrentHashMap<>();

    public PetitionService() {}

    /**
     * Jogador submete petição pelo menu F10.
     */
    public synchronized Optional<PetitionTicket> submitPetition(int playerId, String playerName, int typeId, String content) {
        // Verifica se o jogador já possui chamado pendente ou em andamento
        for (PetitionTicket ticket : petitions.values()) {
            if (ticket.getPetitionerId() == playerId &&
                    (ticket.getState() == PetitionState.PENDING || ticket.getState() == PetitionState.IN_PROCESS)) {
                log.warn("Petition: Jogador {} já possui a petição #{} em aberto!", playerName, ticket.getId());
                return Optional.empty();
            }
        }

        PetitionType type = PetitionType.fromId(typeId);
        PetitionTicket ticket = new PetitionTicket(nextPetitionId++, playerId, playerName, type, content);
        petitions.put(ticket.getId(), ticket);
        log.info("Petition: Nova petição submetida #{} por {} (Tipo: {})", ticket.getId(), playerName, type.getDescription());
        return Optional.of(ticket);
    }

    /**
     * GM aceita a petição e assume o atendimento.
     */
    public synchronized boolean acceptPetition(int gmId, String gmName, int petitionId) {
        PetitionTicket ticket = petitions.get(petitionId);
        if (ticket == null || ticket.getState() != PetitionState.PENDING) {
            return false;
        }
        ticket.setGmResponder(gmId, gmName);
        log.info("Petition: GM {} assumiu o chamado #{} de {}", gmName, petitionId, ticket.getPetitionerName());
        return true;
    }

    /**
     * Envia mensagem dentro da petição ativa.
     */
    public boolean sendMessage(int petitionId, String senderName, String message) {
        PetitionTicket ticket = petitions.get(petitionId);
        if (ticket == null || ticket.getState() != PetitionState.IN_PROCESS) {
            return false;
        }
        ticket.addMessage(senderName, message);
        return true;
    }

    /**
     * Finaliza a petição.
     */
    public synchronized boolean closePetition(int petitionId) {
        PetitionTicket ticket = petitions.get(petitionId);
        if (ticket == null) {
            return false;
        }
        ticket.setState(PetitionState.COMPLETED);
        log.info("Petition: Chamado #{} finalizado com sucesso.", petitionId);
        return true;
    }

    public List<PetitionTicket> getPendingPetitions() {
        return petitions.values().stream()
                .filter(p -> p.getState() == PetitionState.PENDING)
                .toList();
    }

    public Optional<PetitionTicket> getPetition(int id) { return Optional.ofNullable(petitions.get(id)); }
    public int getTotalCount() { return petitions.size(); }
}
