package com.lopez.l2j.game.service;

import com.lopez.l2j.game.model.PlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    public static final String VAR_PVP_RANK_POINTS = "PVP_RANK_POINTS";
    public static final String VAR_PVP_RANK_KILLS = "PVP_RANK_KILLS";

    public record Tier(int level, int pointsRequired, String rewardDescription) {}

    public record Rank(String name, List<Tier> tiers) {}

    public record RankProgression(String rankName, int tierLevel, int currentPoints) {}

    private CharacterVariablesService characterVariables;
    private InventoryService inventoryService;

    private boolean enabled = true;
    private boolean checkIp = true;
    private int minKillIntervalMinutes = 10;
    private int basePvpPoints = 10;
    private int basePkPoints = 5;

    // Ranks e tiers
    private final List<Rank> ranks = new ArrayList<>();
    // Player objectId -> accumulated PvP ranking points
    private final Map<Integer, Integer> playerPoints = new ConcurrentHashMap<>();
    // Player objectId -> accumulated PvP ranking kills
    private final Map<Integer, Integer> playerKills = new ConcurrentHashMap<>();
    // KillerId_VictimId -> last kill timestamp (ms)
    private final Map<String, Long> lastKillTimes = new ConcurrentHashMap<>();

    public PvPRankService() {
        this(null, null);
    }

    @Autowired
    public PvPRankService(
            @Autowired(required = false) CharacterVariablesService characterVariables,
            @Autowired(required = false) InventoryService inventoryService) {
        this.characterVariables = characterVariables;
        this.inventoryService = inventoryService;
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
     * Registra um abate por IDs de objeto e persiste pontos e kills em CharacterVariablesService.
     */
    public synchronized int recordKill(int killerObjectId, int victimObjectId) {
        return registerPvPKill(killerObjectId, null, victimObjectId, null, false);
    }

    /**
     * Registra um abate entre instâncias de PlayerCharacter e persiste pontos e kills em CharacterVariablesService.
     */
    public synchronized int recordKill(PlayerCharacter killer, PlayerCharacter victim) {
        if (killer == null || victim == null) {
            return 0;
        }
        boolean isPk = victim.pvpFlag() == 0 && victim.karma() == 0;
        return registerPvPKill(killer.objectId(), null, victim.objectId(), null, isPk);
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
        int newTotalPoints = currentPoints + pointsToAdd;
        int newTotalKills = playerKills.merge(killerId, 1, Integer::sum);
        playerPoints.put(killerId, newTotalPoints);

        if (characterVariables != null) {
            characterVariables.set(killerId, VAR_PVP_RANK_POINTS, newTotalPoints);
            characterVariables.set(killerId, VAR_PVP_RANK_KILLS, newTotalKills);
        }

        log.info("PvPRank: {} ganhou {} pontos de PvP (total: {}, kills: {}).", killerId, pointsToAdd, newTotalPoints, newTotalKills);
        return pointsToAdd;
    }

    /**
     * Restaura os pontos e kills de ranking PvP do jogador no login a partir de CharacterVariablesService.
     */
    public void loadPlayerRank(PlayerCharacter player) {
        if (player == null) {
            return;
        }
        loadPlayerRank(player.objectId());
    }

    /**
     * Restaura os pontos e kills de ranking PvP pelo objectId do jogador.
     */
    public void loadPlayerRank(int playerObjectId) {
        if (characterVariables == null || playerObjectId <= 0) {
            return;
        }
        int savedPoints = characterVariables.getInt(playerObjectId, VAR_PVP_RANK_POINTS, -1);
        if (savedPoints >= 0) {
            playerPoints.put(playerObjectId, savedPoints);
        }
        int savedKills = characterVariables.getInt(playerObjectId, VAR_PVP_RANK_KILLS, -1);
        if (savedKills >= 0) {
            playerKills.put(playerObjectId, savedKills);
        }
    }

    /**
     * Entrega recompensas de tier ou fim de temporada ao jogador usando InventoryService.
     */
    public void grantSeasonReward(PlayerCharacter player) {
        if (player == null || player.inventory() == null) {
            return;
        }
        int points = getPlayerPoints(player.objectId());
        Tier bestTier = null;
        for (Rank rank : ranks) {
            for (Tier tier : rank.tiers()) {
                if (points >= tier.pointsRequired()) {
                    bestTier = tier;
                }
            }
        }
        if (bestTier != null && bestTier.rewardDescription() != null && !bestTier.rewardDescription().isBlank()) {
            for (String entry : bestTier.rewardDescription().split(";")) {
                if (entry.isBlank()) continue;
                String[] parts = entry.trim().split("-");
                if (parts.length == 2) {
                    try {
                        int itemId = Integer.parseInt(parts[0].trim());
                        int count = Integer.parseInt(parts[1].trim());
                        if (inventoryService != null) {
                            inventoryService.addItem(player.inventory(), itemId, count, "PvPRankSeasonReward");
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
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
            if (characterVariables != null) {
                characterVariables.set(entry.getKey(), VAR_PVP_RANK_POINTS, newPoints);
            }
        }
        log.info("PvPRank: Decay diário aplicado em {} jogadores registrados.", playerPoints.size());
    }

    public int getPlayerPoints(int playerId) { return playerPoints.getOrDefault(playerId, 0); }
    public void setPlayerPoints(int playerId, int points) {
        playerPoints.put(playerId, points);
        if (characterVariables != null) {
            characterVariables.set(playerId, VAR_PVP_RANK_POINTS, points);
        }
    }
    public int getPlayerKills(int playerId) { return playerKills.getOrDefault(playerId, 0); }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isCheckIp() { return checkIp; }
    public void setCheckIp(boolean checkIp) { this.checkIp = checkIp; }
    public int getMinKillIntervalMinutes() { return minKillIntervalMinutes; }
    public void setMinKillIntervalMinutes(int minKillIntervalMinutes) { this.minKillIntervalMinutes = minKillIntervalMinutes; }
    public List<Rank> getRanks() { return Collections.unmodifiableList(ranks); }
    public void setCharacterVariablesService(CharacterVariablesService characterVariables) { this.characterVariables = characterVariables; }
    public void setInventoryService(InventoryService inventoryService) { this.inventoryService = inventoryService; }
}
