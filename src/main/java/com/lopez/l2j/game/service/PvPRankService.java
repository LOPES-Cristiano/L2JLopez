package com.lopez.l2j.game.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sistema de Patentes e Tiers PvP (pvprank.xml).
 * Proteção anti-feed (mesmo IP/HWID e intervalo mínimo entre abates do mesmo alvo).
 * Multiplicadores de pontos por faixas, decay diário de pontos e patentes com recompensas.
 */
@Service
public class PvPRankService {

    private static final Logger log = LoggerFactory.getLogger(PvPRankService.class);

    public record Tier(int level, int pointsRequired, String rewardDescription) {}

    public record Rank(String name, List<Tier> tiers) {}

    public record RankProgression(String rankName, int tierLevel, int currentPoints) {}

    private boolean enabled = true;
    private boolean checkIp = true;
    private int minKillIntervalMinutes = 10;
    private int basePvpPoints = 10;
    private int basePkPoints = 5;

    // Ranks e tiers
    private final List<Rank> ranks = new ArrayList<>();
    // Player objectId -> accumulated PvP ranking points
    private final Map<Integer, Integer> playerPoints = new ConcurrentHashMap<>();
    // KillerId_VictimId -> last kill timestamp (ms)
    private final Map<String, Long> lastKillTimes = new ConcurrentHashMap<>();

    public PvPRankService() {
        initDefaultRanks();
    }

    private void initDefaultRanks() {
        ranks.add(new Rank("Newbie", Arrays.asList(
                new Tier(1, 100, "1921-2;1867-8;1866-1"),
                new Tier(2, 200, "1921-2;1867-8;1866-1"),
                new Tier(3, 300, "1921-2;1867-8;1866-1")
        )));
        ranks.add(new Rank("Novato", Arrays.asList(
                new Tier(1, 500, "1921-2;1867-8;1866-1"),
                new Tier(2, 1000, "1921-2;1867-8;1866-1"),
                new Tier(3, 1500, "1921-2;1867-8;1866-1")
        )));
        ranks.add(new Rank("Madeira", Arrays.asList(
                new Tier(1, 2000, "1921-2;1867-8;1866-1"),
                new Tier(2, 3000, "1921-2;1867-8;1866-1"),
                new Tier(3, 4000, "1921-2;1867-8;1866-1")
        )));
        ranks.add(new Rank("Bronze", Arrays.asList(
                new Tier(1, 5000, "1921-4;1867-16;1866-2"),
                new Tier(2, 6500, "1921-4;1867-16;1866-2"),
                new Tier(3, 8000, "1921-4;1867-16;1866-2")
        )));
        ranks.add(new Rank("Prata", Arrays.asList(
                new Tier(1, 10000, "1921-6;1867-24;1866-3"),
                new Tier(2, 12500, "1921-6;1867-24;1866-3"),
                new Tier(3, 15000, "1921-6;1867-24;1866-3")
        )));
        ranks.add(new Rank("Ouro", Arrays.asList(
                new Tier(1, 20000, "1921-10;1867-40;1866-5"),
                new Tier(2, 25000, "1921-10;1867-40;1866-5"),
                new Tier(3, 30000, "1921-10;1867-40;1866-5")
        )));
    }

    /**
     * Verifica e adiciona pontos ao jogador vencedor com proteção anti-feed.
     * Retorna a quantidade de pontos concedidos (0 se bloqueado por anti-feed).
     */
    public synchronized int registerPvPKill(int killerId, String killerIp, int victimId, String victimIp, boolean isPk) {
        if (!enabled || killerId == victimId) {
            return 0;
        }

        // Anti-feed: checagem de mesmo IP
        if (checkIp && killerIp != null && killerIp.equals(victimIp) && !killerIp.isEmpty()) {
            log.warn("PvPRank: Anti-feed acionado! Mesmo IP entre killer ({}) e victim ({}): {}", killerId, victimId, killerIp);
            return 0;
        }

        // Anti-feed: checagem de intervalo mínimo entre abates do mesmo alvo
        String key = killerId + "_" + victimId;
        long now = System.currentTimeMillis();
        Long lastKill = lastKillTimes.get(key);
        if (lastKill != null) {
            long diffMin = (now - lastKill) / (60 * 1000);
            if (diffMin < minKillIntervalMinutes) {
                log.info("PvPRank: Intervalo mínimo não respeitado ({} min < {} min) entre {} e {}",
                        diffMin, minKillIntervalMinutes, killerId, victimId);
                return 0;
            }
        }
        lastKillTimes.put(key, now);

        // Multiplicador baseado na pontuação atual
        int currentPoints = playerPoints.getOrDefault(killerId, 0);
        double multiplier = 1.0;
        if (currentPoints >= 2000) {
            multiplier = 2.0;
        } else if (currentPoints >= 1000) {
            multiplier = 1.5;
        } else if (currentPoints >= 500) {
            multiplier = 1.2;
        }

        int pointsToAdd = (int) Math.round((isPk ? basePkPoints : basePvpPoints) * multiplier);
        playerPoints.put(killerId, currentPoints + pointsToAdd);
        log.info("PvPRank: {} ganhou {} pontos de PvP (total: {}).", killerId, pointsToAdd, currentPoints + pointsToAdd);
        return pointsToAdd;
    }

    /**
     * Retorna a patente e tier atual do jogador.
     */
    public RankProgression getRankProgression(int playerId) {
        int points = playerPoints.getOrDefault(playerId, 0);
        String currentRankName = "Iniciante";
        int currentTierLevel = 0;

        for (Rank rank : ranks) {
            for (Tier tier : rank.tiers()) {
                if (points >= tier.pointsRequired()) {
                    currentRankName = rank.name();
                    currentTierLevel = tier.level();
                } else {
                    return new RankProgression(currentRankName, currentTierLevel, points);
                }
            }
        }
        return new RankProgression(currentRankName, currentTierLevel, points);
    }

    /**
     * Aplica o decay diário de pontos em todos os jogadores.
     */
    public synchronized void applyDailyDecay() {
        for (Map.Entry<Integer, Integer> entry : playerPoints.entrySet()) {
            int p = entry.getValue();
            double decayPercent;
            if (p >= 10000) {
                decayPercent = 0.03;
            } else if (p >= 2000) {
                decayPercent = 0.02;
            } else if (p >= 1000) {
                decayPercent = 0.01;
            } else {
                decayPercent = 0.005;
            }
            int loss = (int) Math.round(p * decayPercent);
            int newPoints = Math.max(0, p - loss);
            playerPoints.put(entry.getKey(), newPoints);
        }
        log.info("PvPRank: Decay diário aplicado em {} jogadores registrados.", playerPoints.size());
    }

    public int getPlayerPoints(int playerId) { return playerPoints.getOrDefault(playerId, 0); }
    public void setPlayerPoints(int playerId, int points) { playerPoints.put(playerId, points); }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isCheckIp() { return checkIp; }
    public void setCheckIp(boolean checkIp) { this.checkIp = checkIp; }
    public int getMinKillIntervalMinutes() { return minKillIntervalMinutes; }
    public void setMinKillIntervalMinutes(int minKillIntervalMinutes) { this.minKillIntervalMinutes = minKillIntervalMinutes; }
    public List<Rank> getRanks() { return Collections.unmodifiableList(ranks); }
}
