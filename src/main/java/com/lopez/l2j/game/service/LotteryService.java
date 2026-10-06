package com.lopez.l2j.game.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Loteria de Aden Retail (LotteryService).
 * Compra de bilhetes nas capitais com escolha de 5 números (1 a 20),
 * acúmulo do prêmio total da loteria e sorteio automático regular/semanal.
 */
@Service
public class LotteryService {

    private static final Logger log = LoggerFactory.getLogger(LotteryService.class);

    public record LotteryTicket(int ticketId, int playerId, String playerName, Set<Integer> numbers, int lotteryId) {}

    public record DrawResult(int lotteryId, List<Integer> winningNumbers, int totalJackpot,
                             int firstPrizeWinners, int secondPrizeWinners, int thirdPrizeWinners) {}

    private int currentLotteryId = 1001;
    private int ticketPrice = 2000; // 2.000 Adena por bilhete
    private int prizePool = 1000000; // Jackpot inicial acumulado: 1.000.000 Adena
    private int nextTicketId = 1;

    private final List<LotteryTicket> currentTickets = new CopyOnWriteArrayList<>();
    private final Map<Integer, DrawResult> drawHistory = new ConcurrentHashMap<>();

    public LotteryService() {}

    /**
     * Compra de bilhete de loteria escolhendo 5 números entre 1 e 20.
     */
    public synchronized Optional<LotteryTicket> buyTicket(int playerId, String playerName, Set<Integer> numbers) {
        if (numbers == null || numbers.size() != 5) {
            log.warn("Lottery: O jogador {} tentou apostar com quantidade inválida de números ({}).", playerName, numbers != null ? numbers.size() : 0);
            return Optional.empty();
        }

        for (int num : numbers) {
            if (num < 1 || num > 20) {
                log.warn("Lottery: Número {} fora da faixa 1-20.", num);
                return Optional.empty();
            }
        }

        prizePool += (ticketPrice * 8) / 10; // 80% do valor do bilhete vai para o jackpot acumulado
        LotteryTicket ticket = new LotteryTicket(nextTicketId++, playerId, playerName, new HashSet<>(numbers), currentLotteryId);
        currentTickets.add(ticket);
        log.info("Lottery: Jogador {} comprou bilhete #{} para a loteria #{}: {}", playerName, ticket.ticketId(), currentLotteryId, numbers);
        return Optional.of(ticket);
    }

    /**
     * Realiza o sorteio oficial da rodada atual da Loteria de Aden.
     */
    public synchronized DrawResult drawLottery() {
        Set<Integer> winningSet = new HashSet<>();
        while (winningSet.size() < 5) {
            winningSet.add(ThreadLocalRandom.current().nextInt(1, 21));
        }
        List<Integer> winningNumbers = new ArrayList<>(winningSet);
        Collections.sort(winningNumbers);

        int firstMatches = 0;  // 5 acertos
        int secondMatches = 0; // 4 acertos
        int thirdMatches = 0;  // 3 acertos

        for (LotteryTicket ticket : currentTickets) {
            long matches = ticket.numbers().stream().filter(winningSet::contains).count();
            if (matches == 5) {
                firstMatches++;
            } else if (matches == 4) {
                secondMatches++;
            } else if (matches == 3) {
                thirdMatches++;
            }
        }

        DrawResult result = new DrawResult(currentLotteryId, winningNumbers, prizePool, firstMatches, secondMatches, thirdMatches);
        drawHistory.put(currentLotteryId, result);

        log.info("Lottery: Sorteio #{} realizado! Números sorteados: {}. Acertos: 5={}, 4={}, 3={}. Prêmio: {}",
                currentLotteryId, winningNumbers, firstMatches, secondMatches, thirdMatches, prizePool);

        // Se ninguém acertou o 1º prêmio, o prêmio acumula para o próximo sorteio
        if (firstMatches == 0) {
            prizePool += 500000;
        } else {
            prizePool = 1000000; // Reset para o valor base
        }

        currentLotteryId++;
        currentTickets.clear();
        return result;
    }

    public int getCurrentLotteryId() { return currentLotteryId; }
    public int getPrizePool() { return prizePool; }
    public int getTicketPrice() { return ticketPrice; }
    public void setTicketPrice(int ticketPrice) { this.ticketPrice = ticketPrice; }
    public List<LotteryTicket> getCurrentTickets() { return Collections.unmodifiableList(currentTickets); }
    public Optional<DrawResult> getDrawResult(int lotteryId) { return Optional.ofNullable(drawHistory.get(lotteryId)); }
}
