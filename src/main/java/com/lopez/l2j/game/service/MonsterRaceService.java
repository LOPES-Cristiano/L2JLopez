package com.lopez.l2j.game.service;

import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

/**
 * Monster Derby Track Retail (MonsterRaceService) - #488.
 * Corrida de 8 monstros com velocidades aleatórias, ciclo de 20 minutos,
 * emissão de bilhete físico (4443), cálculo de odds dinâmicas e persistência em mdt_bets / mdt_history.
 */
@Service
public class MonsterRaceService {

    private static final Logger log = LoggerFactory.getLogger(MonsterRaceService.class);

    public static final int ADENA_ITEM_ID = 57;
    public static final int RACE_TICKET_ITEM_ID = 4443;
    public static final int RACE_CYCLE_MINUTES = 20;

    public record RaceBet(int betId, int playerId, String playerName, int laneFirst, int laneSecond, int betAmount) {}

    public record RaceOutcome(int raceId, int firstPlaceLane, int secondPlaceLane, int[][] speeds) {}

    private InventoryService inventoryService;
    private JdbcClient jdbc;
    private ScheduledExecutorService scheduler;

    private int currentRaceId = 1;
    private int nextBetId = 1;

    private final int[][] speeds = new int[8][20];
    private final int[] first = new int[2];
    private final int[] second = new int[2];
    private final double[] laneOdds = new double[8];

    private final List<RaceBet> activeBets = new CopyOnWriteArrayList<>();
    private final Map<Integer, RaceOutcome> raceHistory = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> pendingPayouts = new ConcurrentHashMap<>();

    public MonsterRaceService() {
        calculateNewSpeeds();
        recalculateLaneOdds();
    }

    public MonsterRaceService(InventoryService inventoryService) {
        this(inventoryService, null);
    }

    @Autowired
    public MonsterRaceService(
            @Autowired(required = false) InventoryService inventoryService,
            @Autowired(required = false) JdbcClient jdbc) {
        this();
        this.inventoryService = inventoryService;
        this.jdbc = jdbc;
        loadFromDb();
    }

    public void setInventoryService(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    public synchronized void startRaceScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MonsterRace-Scheduler");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::runRace, RACE_CYCLE_MINUTES, RACE_CYCLE_MINUTES, TimeUnit.MINUTES);
    }

    public synchronized void stopRaceScheduler() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    private void recalculateLaneOdds() {
        long totalPool = 0;
        long[] lanePools = new long[8];
        for (RaceBet b : activeBets) {
            if (b.laneFirst() >= 1 && b.laneFirst() <= 8) {
                lanePools[b.laneFirst() - 1] += b.betAmount();
                totalPool += b.betAmount();
            }
        }
        for (int i = 0; i < 8; i++) {
            if (totalPool > 0 && lanePools[i] > 0) {
                double rawOdd = (totalPool * 0.85) / lanePools[i];
                laneOdds[i] = Math.max(1.1, Math.min(15.0, Math.round(rawOdd * 100.0) / 100.0));
            } else {
                laneOdds[i] = 3.5;
            }
        }
    }

    public double getLaneOdd(int lane) {
        if (lane < 1 || lane > 8) {
            return 3.5;
        }
        return laneOdds[lane - 1];
    }

    /**
     * Calcula as velocidades dos 8 monstros pelos 20 setores da pista de corrida.
     * Determina o 1º e 2º colocados e liquida as apostas ativas.
     */
    public synchronized RaceOutcome runRace() {
        calculateNewSpeeds();
        recalculateLaneOdds();
        RaceOutcome outcome = new RaceOutcome(currentRaceId, first[0], second[0], speeds.clone());
        raceHistory.put(currentRaceId, outcome);
        settleBets(outcome);
        saveRaceOutcomeToDb(outcome);

        log.info("MonsterRace: Corrida #{} finalizada! 1º Lugar: Raia {}, 2º Lugar: Raia {}",
                currentRaceId, first[0], second[0]);

        currentRaceId++;
        activeBets.clear();
        recalculateLaneOdds();
        return outcome;
    }

    /**
     * Calcula os pagamentos das apostas ativas frente ao resultado da corrida.
     */
    public synchronized Map<Integer, Integer> settleBets(RaceOutcome outcome) {
        Map<Integer, Integer> roundPayouts = new HashMap<>();
        if (outcome == null) {
            return roundPayouts;
        }
        for (RaceBet bet : activeBets) {
            int payout = calculatePayout(bet, outcome);
            if (payout > 0) {
                roundPayouts.merge(bet.playerId(), payout, Integer::sum);
                pendingPayouts.merge(bet.playerId(), payout, Integer::sum);
            }
        }
        return roundPayouts;
    }

    /**
     * Executa a corrida atual, calcula os pagamentos (settleBets) e deposita a Adena ganha
     * diretamente no inventário/banco do jogador via InventoryService.
     */
    public synchronized int runRaceAndSettlePlayer(PlayerCharacter player) {
        runRace();
        return claimPayout(player);
    }

    /**
     * Resgata os ganhos pendentes do jogador nas corridas e deposita a Adena via InventoryService.
     */
    public synchronized int claimPayout(PlayerCharacter player) {
        if (player == null) {
            return 0;
        }
        Integer payout = pendingPayouts.remove(player.objectId());
        if (payout == null || payout <= 0) {
            return 0;
        }
        var inv = player.inventory();
        if (inv != null) {
            inv.destroyItemByItemId(RACE_TICKET_ITEM_ID, 1);
            if (inventoryService != null) {
                var added = inventoryService.addItem(inv, ADENA_ITEM_ID, payout, "MonsterRacePayout");
                if (added == null) {
                    inv.addAdena(payout);
                }
            } else {
                inv.addAdena(payout);
            }
        }
        log.info("MonsterRace: Jogador {} recebeu {} Adena de prêmio.", player.name(), payout);
        return payout;
    }

    private void calculateNewSpeeds() {
        first[0] = 0;
        first[1] = 0;
        second[0] = 0;
        second[1] = 0;

        for (int i = 0; i < 8; i++) {
            int totalDistance = 0;
            for (int j = 0; j < 20; j++) {
                // Velocidade base 100 com variação retail
                int spd = ThreadLocalRandom.current().nextInt(50, 150);
                speeds[i][j] = spd;
                totalDistance += spd;
            }

            if (totalDistance > first[1]) {
                second[0] = first[0];
                second[1] = first[1];
                first[0] = i + 1; // Raias 1 a 8
                first[1] = totalDistance;
            } else if (totalDistance > second[1]) {
                second[0] = i + 1;
                second[1] = totalDistance;
            }
        }
    }

    /**
     * Registra aposta simples do PlayerCharacter em uma raia, cobrando Adena via InventoryService.
     */
    public synchronized boolean placeBet(PlayerCharacter player, int lane, int amount) {
        return placeBet(player, lane, 0, amount);
    }

    /**
     * Registra aposta (simples ou casada) do PlayerCharacter, cobrando Adena via InventoryService.
     */
    public synchronized boolean placeBet(PlayerCharacter player, int laneFirst, int laneSecond, int amount) {
        if (player == null || laneFirst < 1 || laneFirst > 8 || amount <= 0) {
            return false;
        }
        if (laneSecond < 0 || laneSecond > 8 || laneSecond == laneFirst) {
            return false;
        }
        var inv = player.inventory();
        if (inv == null) {
            return false;
        }

        if (inventoryService != null) {
            if (inventoryService.consumeItem(inv, ADENA_ITEM_ID, amount, "MonsterRaceBet") == null) {
                return false;
            }
        } else {
            if (!inv.destroyItemByItemId(ADENA_ITEM_ID, amount)) {
                return false;
            }
        }

        ItemInstance ticket = null;
        if (inventoryService != null) {
            var added = inventoryService.addItem(inv, RACE_TICKET_ITEM_ID, 1, "MonsterRaceTicket");
            if (added != null) {
                ticket = added.item();
            }
        }
        if (ticket == null) {
            ticket = inv.addItem(RACE_TICKET_ITEM_ID, 1);
        }
        if (ticket != null) {
            ticket.enchantLevel(laneFirst * 10 + laneSecond);
            ticket.customType1(currentRaceId);
            ticket.customType2(amount / 100);
        }

        return placeBet(player.objectId(), player.name(), laneFirst, laneSecond, amount).isPresent();
    }

    /**
     * Registra aposta em monstro (aposta simples no 1º lugar, ou casada 1º e 2º).
     */
    public synchronized Optional<RaceBet> placeBet(int playerId, String playerName, int laneFirst, int laneSecond, int betAmount) {
        if (laneFirst < 1 || laneFirst > 8 || betAmount <= 0) {
            return Optional.empty();
        }
        RaceBet bet = new RaceBet(nextBetId++, playerId, playerName, laneFirst, laneSecond, betAmount);
        activeBets.add(bet);
        recalculateLaneOdds();
        saveBetToDb(laneFirst, betAmount);
        log.info("MonsterRace: Aposta registrada #{} por {} na Raia {} (2º: Raia {}) com {} Adena",
                bet.betId(), playerName, laneFirst, laneSecond, betAmount);
        return Optional.of(bet);
    }

    /**
     * Calcula o retorno da aposta do jogador. Retorna o montante ganho (ou 0 se perdeu).
     */
    public int calculatePayout(RaceBet bet, RaceOutcome outcome) {
        if (bet.laneFirst() == outcome.firstPlaceLane()) {
            if (bet.laneSecond() > 0) {
                // Aposta exata 1º e 2º lugar (Odd 10.0x)
                if (bet.laneSecond() == outcome.secondPlaceLane()) {
                    return bet.betAmount() * 10;
                }
            } else {
                // Aposta simples em vencedor (Odd 3.5x)
                return (int) Math.round(bet.betAmount() * 3.5);
            }
        }
        return 0;
    }

    private void loadFromDb() {
        if (jdbc == null) {
            return;
        }
        try {
            var rows = jdbc.sql("SELECT race_id, first, second, odd_rate FROM mdt_history ORDER BY race_id DESC LIMIT 20")
                    .query().listOfRows();
            for (var r : rows) {
                int rId = ((Number) r.get("race_id")).intValue();
                int f = ((Number) r.get("first")).intValue();
                int s = ((Number) r.get("second")).intValue();
                raceHistory.putIfAbsent(rId, new RaceOutcome(rId, f, s, new int[8][20]));
                if (rId >= currentRaceId) {
                    currentRaceId = rId + 1;
                }
            }
        } catch (Exception e) {
            log.debug("MonsterRaceService loadFromDb: {}", e.getMessage());
        }
    }

    private void saveBetToDb(int lane, int sum) {
        if (jdbc == null) {
            return;
        }
        try {
            jdbc.sql("UPDATE mdt_bets SET bet = bet + :s WHERE lane_id = :l")
                    .param("s", sum)
                    .param("l", lane)
                    .update();
        } catch (Exception e) {
            log.debug("MonsterRaceService saveBetToDb: {}", e.getMessage());
        }
    }

    private void saveRaceOutcomeToDb(RaceOutcome outcome) {
        if (jdbc == null || outcome == null) {
            return;
        }
        try {
            jdbc.sql("INSERT INTO mdt_history (race_id, first, second, odd_rate) VALUES (:id, :f, :s, :odd)")
                    .param("id", outcome.raceId())
                    .param("f", outcome.firstPlaceLane())
                    .param("s", outcome.secondPlaceLane())
                    .param("odd", getLaneOdd(outcome.firstPlaceLane()))
                    .update();
        } catch (Exception e) {
            log.debug("MonsterRaceService saveRaceOutcomeToDb: {}", e.getMessage());
        }
    }

    public int getFirstPlace() { return first[0]; }
    public int getSecondPlace() { return second[0]; }
    public int[][] getSpeeds() { return speeds; }
    public int getCurrentRaceId() { return currentRaceId; }
    public List<RaceBet> getActiveBets() { return Collections.unmodifiableList(activeBets); }
    public Optional<RaceOutcome> getRaceOutcome(int raceId) { return Optional.ofNullable(raceHistory.get(raceId)); }
    public int getPendingPayout(int playerId) { return pendingPayouts.getOrDefault(playerId, 0); }
}
