package com.lopez.l2j.game.service;

import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemRepository;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Loteria de Aden Retail (LotteryService).
 * Compra de bilhetes nas capitais com escolha de 5 números (1 a 20),
 * emissão de bilhete físico (item 4442) com bitmask em custom_type1/custom_type2,
 * acúmulo do prêmio total da loteria, persistência SQL na tabela `games` e sorteio regular.
 */
@Service
public class LotteryService {

    private static final Logger log = LoggerFactory.getLogger(LotteryService.class);

    public static final int ADENA_ITEM_ID = 57;
    public static final int LOTTERY_TICKET_ITEM_ID = 4442;
    public static final int TICKET_PRICE = 2000;

    public record LotteryTicket(int ticketId, int playerId, String playerName, Set<Integer> numbers, int lotteryId) {}

    public record DrawResult(int lotteryId, List<Integer> winningNumbers, int totalJackpot,
                             int firstPrizeWinners, int secondPrizeWinners, int thirdPrizeWinners) {}

    private InventoryService inventoryService;
    private ItemRepository itemRepository;
    private JdbcClient jdbc;

    private int currentLotteryId = 1001;
    private int ticketPrice = TICKET_PRICE; // 2.000 Adena por bilhete
    private int prizePool = 1000000; // Jackpot inicial acumulado: 1.000.000 Adena
    private int nextTicketId = 1;

    private final List<LotteryTicket> currentTickets = new CopyOnWriteArrayList<>();
    private final Map<Integer, List<LotteryTicket>> ticketsByRound = new ConcurrentHashMap<>();
    private final Map<Integer, DrawResult> drawHistory = new ConcurrentHashMap<>();
    private final Set<String> claimedPrizes = ConcurrentHashMap.newKeySet();

    public LotteryService() {}

    public LotteryService(InventoryService inventoryService) {
        this(inventoryService, null, null);
    }

    public LotteryService(InventoryService inventoryService, JdbcClient jdbc) {
        this(inventoryService, jdbc, null);
    }

    @Autowired
    public LotteryService(
            @Autowired(required = false) InventoryService inventoryService,
            @Autowired(required = false) JdbcClient jdbc,
            @Autowired(required = false) ItemRepository itemRepository) {
        this.inventoryService = inventoryService;
        this.jdbc = jdbc;
        this.itemRepository = itemRepository;
        loadFromDb();
    }

    public void setInventoryService(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    public void setItemRepository(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public static int encodeNumbersBitmask(Set<Integer> numbers) {
        if (numbers == null) {
            return 0;
        }
        int mask = 0;
        for (int n : numbers) {
            if (n >= 1 && n <= 20) {
                mask |= (1 << (n - 1));
            }
        }
        return mask;
    }

    public static Set<Integer> decodeNumbersBitmask(int customType1, int customType2) {
        int mask = (customType1 & 0xFFFF) | ((customType2 & 0xFFFF) << 16);
        Set<Integer> result = new LinkedHashSet<>();
        for (int i = 1; i <= 20; i++) {
            if ((mask & (1 << (i - 1))) != 0) {
                result.add(i);
            }
        }
        return result;
    }

    /**
     * Compra de bilhete integrada ao PlayerCharacter: valida números, cobra TICKET_PRICE (2.000 Adena)
     * do inventário/banco via InventoryService, entrega o bilhete físico 4442 com bitmask e registra o bilhete.
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

        int mask = encodeNumbersBitmask(numbers);
        int type1 = mask & 0xFFFF;
        int type2 = (mask >>> 16) & 0xFFFF;
        ItemInstance ticketItem = null;
        if (inventoryService != null) {
            var added = inventoryService.addItem(inv, LOTTERY_TICKET_ITEM_ID, 1, "LotteryTicket");
            if (added != null) {
                ticketItem = added.item();
            }
        }
        if (ticketItem == null) {
            ticketItem = inv.addItem(LOTTERY_TICKET_ITEM_ID, 1);
        }
        if (ticketItem != null) {
            ticketItem.enchantLevel(currentLotteryId);
            ticketItem.customType1(type1);
            ticketItem.customType2(type2);
            if (inventoryService != null) {
                inventoryService.saveItem(ticketItem);
            } else if (itemRepository != null) {
                itemRepository.update(ticketItem);
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
        saveRoundToDb(currentLotteryId, prizePool, 0, 0, 0);
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
        int mask = encodeNumbersBitmask(winningSet);
        saveRoundToDb(currentLotteryId, prizePool, 1, mask & 0xFFFF, (mask >>> 16) & 0xFFFF);

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
        saveRoundToDb(currentLotteryId, prizePool, 0, 0, 0);
        return result;
    }

    /**
     * Calcula e resgata o prêmio pendente do jogador em uma determinada rodada,
     * impedindo resgate duplicado, consumindo o bilhete físico (se presente) e entregando Adena via InventoryService.
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
                var consumed = inventoryService.consumeItem(inv, LOTTERY_TICKET_ITEM_ID, 1, "LotteryClaim");
                if (consumed == null) {
                    inv.destroyItemByItemId(LOTTERY_TICKET_ITEM_ID, 1);
                }
                var added = inventoryService.addItem(inv, ADENA_ITEM_ID, totalPrize, "LotteryPrize");
                if (added == null) {
                    inv.addAdena(totalPrize);
                }
            } else {
                var ticketOpt = inv.byItemId(LOTTERY_TICKET_ITEM_ID);
                inv.destroyItemByItemId(LOTTERY_TICKET_ITEM_ID, 1);
                if (itemRepository != null && ticketOpt.isPresent()) {
                    try {
                        itemRepository.delete(ticketOpt.get().objectId());
                    } catch (Exception ignored) {
                    }
                }
                inv.addAdena(totalPrize);
                if (itemRepository != null) {
                    inv.byItemId(ADENA_ITEM_ID).ifPresent(adenaItem -> {
                        try {
                            itemRepository.update(adenaItem);
                        } catch (Exception ignored) {
                        }
                    });
                }
            }
        }

        log.info("Lottery: Jogador {} resgatou {} Adena referente à rodada #{}", player.name(), totalPrize, roundNumber);
        return totalPrize;
    }

    private void loadFromDb() {
        if (jdbc == null) {
            return;
        }
        try {
            var rows = jdbc.sql("SELECT idnr, prize, finished, number1, number2 FROM games WHERE id = 1 ORDER BY idnr DESC")
                    .query().listOfRows();
            for (var r : rows) {
                int idnr = ((Number) r.get("idnr")).intValue();
                int prize = ((Number) r.get("prize")).intValue();
                int finished = ((Number) r.get("finished")).intValue();
                int num1 = ((Number) r.get("number1")).intValue();
                int num2 = ((Number) r.get("number2")).intValue();
                if (finished == 0 && idnr >= currentLotteryId) {
                    currentLotteryId = idnr;
                    prizePool = Math.max(prizePool, prize);
                } else if (finished == 1) {
                    List<Integer> winning = new ArrayList<>(decodeNumbersBitmask(num1, num2));
                    drawHistory.putIfAbsent(idnr, new DrawResult(idnr, winning, prize, 0, 0, 0));
                }
            }
        } catch (Exception e) {
            log.debug("LotteryService loadFromDb: {}", e.getMessage());
        }
    }

    private void saveRoundToDb(int roundId, int prize, int finished, int num1, int num2) {
        if (jdbc == null) {
            return;
        }
        try {
            jdbc.sql("DELETE FROM games WHERE id = 1 AND idnr = :idnr")
                    .param("idnr", roundId)
                    .update();
            jdbc.sql("""
                    INSERT INTO games (id, idnr, enddate, prize, newprize, prize1, prize2, prize3, end1, end2, end3, finished, number1, number2)
                    VALUES (1, :idnr, :end, :prize, :prize, 0, 0, 0, 0, 0, 0, :fin, :n1, :n2)
                    """)
                    .param("idnr", roundId)
                    .param("end", System.currentTimeMillis() + 604_800_000L)
                    .param("prize", prize)
                    .param("fin", finished)
                    .param("n1", num1)
                    .param("n2", num2)
                    .update();
        } catch (Exception e) {
            log.debug("LotteryService saveRoundToDb: {}", e.getMessage());
        }
    }

    public int getCurrentLotteryId() { return currentLotteryId; }
    public int getPrizePool() { return prizePool; }
    public int getTicketPrice() { return ticketPrice; }
    public void setTicketPrice(int ticketPrice) { this.ticketPrice = ticketPrice; }
    public List<LotteryTicket> getCurrentTickets() { return Collections.unmodifiableList(currentTickets); }
    public Optional<DrawResult> getDrawResult(int lotteryId) { return Optional.ofNullable(drawHistory.get(lotteryId)); }
}
