package com.lopez.l2j.game.service;

import com.lopez.l2j.game.model.PlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    public static final int ADENA_ITEM_ID = 57;
    public static final int TICKET_PRICE = 2000;

    public record LotteryTicket(int ticketId, int playerId, String playerName, Set<Integer> numbers, int lotteryId) {}

    public record DrawResult(int lotteryId, List<Integer> winningNumbers, int totalJackpot,
                             int firstPrizeWinners, int secondPrizeWinners, int thirdPrizeWinners) {}

    private InventoryService inventoryService;

    private int currentLotteryId = 1001;
    private int ticketPrice = TICKET_PRICE; // 2.000 Adena por bilhete
    private int prizePool = 1000000; // Jackpot inicial acumulado: 1.000.000 Adena
    private int nextTicketId = 1;

    private final List<LotteryTicket> currentTickets = new CopyOnWriteArrayList<>();
    private final Map<Integer, List<LotteryTicket>> ticketsByRound = new ConcurrentHashMap<>();
    private final Map<Integer, DrawResult> drawHistory = new ConcurrentHashMap<>();
    private final Set<String> claimedPrizes = ConcurrentHashMap.newKeySet();

    public LotteryService() {}

    @Autowired
    public LotteryService(@Autowired(required = false) InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    public void setInventoryService(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /**
     * Compra de bilhete integrada ao PlayerCharacter: valida números, cobra TICKET_PRICE (2.000 Adena)
     * do inventário/banco via InventoryService e registra o bilhete para o jogador.
     */
    public synchronized boolean buyTicket(PlayerCharacter player, Set<Integer> numbers) {
        if (player == null || !isValidNumbers(numbers)) {
            return false;
        }
        var inv = player.inventory();
        if (inv == null) {
            return false;
        }

        if (inventoryService != null) {
            if (inventoryService.consumeItem(inv, ADENA_ITEM_ID, ticketPrice, "Lottery") == null) {
                return false;
            }
        } else {
            if (!inv.destroyItemByItemId(ADENA_ITEM_ID, ticketPrice)) {
                return false;
            }
        }

        return buyTicket(player.objectId(), player.name(), numbers).isPresent();
    }

    /**
     * Compra de bilhete de loteria escolhendo 5 números entre 1 e 20.
     */
    public synchronized Optional<LotteryTicket> buyTicket(int playerId, String playerName, Set<Integer> numbers) {
        if (!isValidNumbers(numbers)) {
            log.warn("Lottery: O jogador {} tentou apostar com números inválidos ({}).", playerName, numbers);
            return Optional.empty();
        }

        prizePool += (ticketPrice * 8) / 10; // 80% do valor do bilhete vai para o jackpot acumulado
        LotteryTicket ticket = new LotteryTicket(nextTicketId++, playerId, playerName, new HashSet<>(numbers), currentLotteryId);
        currentTickets.add(ticket);
        ticketsByRound.computeIfAbsent(currentLotteryId, k -> new CopyOnWriteArrayList<>()).add(ticket);
        log.info("Lottery: Jogador {} comprou bilhete #{} para a loteria #{}: {}", playerName, ticket.ticketId(), currentLotteryId, numbers);
        return Optional.of(ticket);
    }

    private boolean isValidNumbers(Set<Integer> numbers) {
        if (numbers == null || numbers.size() != 5) {
            return false;
        }
        for (int num : numbers) {
            if (num < 1 || num > 20) {
                return false;
            }
        }
        return true;
    }

    /**
     * Realiza o sorteio oficial da rodada atual da Loteria de Aden.
     */
    public synchronized DrawResult drawLottery() {
        Set<Integer> winningSet = new HashSet<>();
        while (winningSet.size() < 5) {
            winningSet.add(ThreadLocalRandom.current().nextInt(1, 21));
        }
        return drawLotteryWithNumbers(winningSet);
    }

    /**
     * Realiza o sorteio oficial da rodada atual com números definidos (útil para testes determinísticos).
     */
    public synchronized DrawResult drawLotteryWithNumbers(Set<Integer> winningSet) {
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
        ticketsByRound.put(currentLotteryId, new CopyOnWriteArrayList<>(currentTickets));

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

    /**
     * Calcula e resgata o prêmio pendente do jogador em uma determinada rodada,
     * impedindo resgate duplicado e entregando Adena via InventoryService.
     */
    public synchronized int claimPrize(PlayerCharacter player, int roundNumber) {
        if (player == null) {
            return 0;
        }
        String claimKey = roundNumber + ":" + player.objectId();
        if (claimedPrizes.contains(claimKey)) {
            return 0;
        }

        DrawResult result = drawHistory.get(roundNumber);
        List<LotteryTicket> roundTickets = ticketsByRound.get(roundNumber);
        if (result == null || roundTickets == null || roundTickets.isEmpty()) {
            return 0;
        }

        Set<Integer> winningSet = new HashSet<>(result.winningNumbers());
        int totalPrize = 0;

        for (LotteryTicket ticket : roundTickets) {
            if (ticket.playerId() != player.objectId()) {
                continue;
            }
            long matches = ticket.numbers().stream().filter(winningSet::contains).count();
            if (matches == 5 && result.firstPrizeWinners() > 0) {
                totalPrize += (int) (((long) result.totalJackpot() * 50L / 100L) / result.firstPrizeWinners());
            } else if (matches == 4 && result.secondPrizeWinners() > 0) {
                totalPrize += (int) (((long) result.totalJackpot() * 30L / 100L) / result.secondPrizeWinners());
            } else if (matches == 3 && result.thirdPrizeWinners() > 0) {
                totalPrize += (int) (((long) result.totalJackpot() * 20L / 100L) / result.thirdPrizeWinners());
            }
        }

        if (totalPrize <= 0) {
            return 0;
        }

        claimedPrizes.add(claimKey);
        var inv = player.inventory();
        if (inv != null) {
            if (inventoryService != null) {
                var added = inventoryService.addItem(inv, ADENA_ITEM_ID, totalPrize, "LotteryPrize");
                if (added == null) {
                    inv.addAdena(totalPrize);
                }
            } else {
                inv.addAdena(totalPrize);
            }
        }

        log.info("Lottery: Jogador {} resgatou {} Adena referente à rodada #{}", player.name(), totalPrize, roundNumber);
        return totalPrize;
    }

    public int getCurrentLotteryId() { return currentLotteryId; }
    public int getPrizePool() { return prizePool; }
    public int getTicketPrice() { return ticketPrice; }
    public void setTicketPrice(int ticketPrice) { this.ticketPrice = ticketPrice; }
    public List<LotteryTicket> getCurrentTickets() { return Collections.unmodifiableList(currentTickets); }
    public Optional<DrawResult> getDrawResult(int lotteryId) { return Optional.ofNullable(drawHistory.get(lotteryId)); }
}

