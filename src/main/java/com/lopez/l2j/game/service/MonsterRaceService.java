package com.lopez.l2j.game.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Monster Derby Track Retail (MonsterRaceService).
 * Corrida de 8 monstros com velocidades aleatórias e sistema de bilhetes/apostas de 1º e 2º lugar.
 */
@Service
public class MonsterRaceService {

    private static final Logger log = LoggerFactory.getLogger(MonsterRaceService.class);

    public record RaceBet(int betId, int playerId, String playerName, int laneFirst, int laneSecond, int betAmount) {}

    public record RaceOutcome(int raceId, int firstPlaceLane, int secondPlaceLane, int[][] speeds) {}

    private int currentRaceId = 1;
    private int nextBetId = 1;

    private final int[][] speeds = new int[8][20];
    private final int[] first = new int[2];
    private final int[] second = new int[2];

    private final List<RaceBet> activeBets = new CopyOnWriteArrayList<>();
    private final Map<Integer, RaceOutcome> raceHistory = new ConcurrentHashMap<>();

    public MonsterRaceService() {
        calculateNewSpeeds();
    }

    /**
     * Calcula as velocidades dos 8 monstros pelos 20 setores da pista de corrida.
     * Determina o 1º e 2º colocados.
     */
    public synchronized RaceOutcome runRace() {
        calculateNewSpeeds();
        RaceOutcome outcome = new RaceOutcome(currentRaceId, first[0], second[0], speeds.clone());
        raceHistory.put(currentRaceId, outcome);

        log.info("MonsterRace: Corrida #{} finalizada! 1º Lugar: Raia {}, 2º Lugar: Raia {}",
                currentRaceId, first[0], second[0]);

        currentRaceId++;
        activeBets.clear();
        return outcome;
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
     * Registra aposta em monstro (aposta simples no 1º lugar, ou casada 1º e 2º).
     */
    public synchronized Optional<RaceBet> placeBet(int playerId, String playerName, int laneFirst, int laneSecond, int betAmount) {
        if (laneFirst < 1 || laneFirst > 8 || betAmount <= 0) {
            return Optional.empty();
        }
        RaceBet bet = new RaceBet(nextBetId++, playerId, playerName, laneFirst, laneSecond, betAmount);
        activeBets.add(bet);
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

    public int getFirstPlace() { return first[0]; }
    public int getSecondPlace() { return second[0]; }
    public int[][] getSpeeds() { return speeds; }
    public int getCurrentRaceId() { return currentRaceId; }
    public List<RaceBet> getActiveBets() { return Collections.unmodifiableList(activeBets); }
    public Optional<RaceOutcome> getRaceOutcome(int raceId) { return Optional.ofNullable(raceHistory.get(raceId)); }
}
