package com.lopez.l2j.game.event.pvp;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Motor do Evento DM (DeathMatch - Free For All).
 * Todos contra todos sem equipes, pontos aleatórios de renascimento,
 * e premiação para o Top 1 (ou Top 3) jogadores com mais kills.
 */
@Service
public class DmEventService {

    private static final Logger log = LoggerFactory.getLogger(DmEventService.class);

    public enum EventState {
        INACTIVE,
        REGISTRATION,
        RUNNING
    }

    public enum RegisterResult {
        SUCCESS,
        ALREADY_REGISTERED,
        NOT_IN_REGISTRATION,
        LEVEL_TOO_LOW,
        LEVEL_TOO_HIGH,
        KARMA_NOT_ALLOWED,
        IS_AIO_BLOCKED,
        CURSED_WEAPON_NOT_ALLOWED,
        MAX_PLAYERS_REACHED,
        DUALBOX_NOT_ALLOWED
    }

    public static class Participant {
        private final PlayerCharacter player;
        private final int objectId;
        private final String name;
        private final int level;
        private int kills;
        private int deaths;
        private final int origX;
        private final int origY;
        private final int origZ;
        private volatile long spawnProtectedUntil;

        public Participant(PlayerCharacter player, int origX, int origY, int origZ) {
            this.player = player;
            this.objectId = player.getObjectId();
            this.name = player.getName();
            this.level = player.getLevel();
            this.origX = origX;
            this.origY = origY;
            this.origZ = origZ;
            this.kills = 0;
            this.deaths = 0;
            this.spawnProtectedUntil = 0L;
        }

        public PlayerCharacter getPlayer() { return player; }
        public int getObjectId() { return objectId; }
        public String getName() { return name; }
        public int getLevel() { return level; }
        public int getKills() { return kills; }
        public void addKill() { this.kills++; }
        public int getDeaths() { return deaths; }
        public void addDeath() { this.deaths++; }
        public int getOrigX() { return origX; }
        public int getOrigY() { return origY; }
        public int getOrigZ() { return origZ; }
        public long getSpawnProtectedUntil() { return spawnProtectedUntil; }
        public void setSpawnProtectedUntil(long ts) { this.spawnProtectedUntil = ts; }
        public boolean isSpawnProtected() { return System.currentTimeMillis() < spawnProtectedUntil; }
    }

    // Spawn coordinates inside DM Arena (Coliseum/Giran Arena)
    private static final int[][] ARENA_SPAWNS = {
        { 148457, 46000, -3413 },
        { 149200, 46700, -3413 },
        { 148457, 47400, -3413 },
        { 147700, 46700, -3413 },
        { 148900, 46300, -3413 },
        { 148000, 47100, -3413 }
    };

    public static final int DM_INSTANCE_ID = 9103;
    public static final int ZARICHE_ID = 8190;
    public static final int AKAMANAH_ID = 8689;
    public static final long SPAWN_PROTECTION_MS = 5000L;

    private volatile EventState state = EventState.INACTIVE;
    private int minLevel = Config.DM_MIN_LEVEL;
    private int maxLevel = Config.DM_MAX_LEVEL;
    private int minPlayers = Config.DM_MIN_PLAYERS;
    private int maxPlayers = Config.DM_MAX_PLAYERS;
    private int rewardItemId = Config.DM_REWARD_ID > 0 ? Config.DM_REWARD_ID : parseRewardPart(0, 57);
    private int rewardTop1Count = Config.DM_REWARD_AMOUNT > 0 ? Config.DM_REWARD_AMOUNT : parseRewardPart(1, 50000);
    private int rewardTop2Count = Math.max(1, rewardTop1Count / 2);
    private int rewardTop3Count = Math.max(1, rewardTop1Count / 5);
    private final com.lopez.l2j.game.service.InventoryService inventoryService;

    private static int parseRewardPart(int idx, int fallback) {
        String r = Config.DM_EVENT_REWARDS;
        if (r == null || r.isBlank()) {
            return fallback;
        }
        String firstReward = r.split(";")[0];
        String[] kv = firstReward.split(",");
        if (kv.length <= idx) {
            return fallback;
        }
        try {
            return Integer.parseInt(kv[idx].trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private final Map<Integer, Participant> participants = new ConcurrentHashMap<>();
    private java.util.concurrent.ScheduledExecutorService autoScheduler;
    private java.util.concurrent.ScheduledFuture<?> scheduledTask;

    public DmEventService() {
        this(null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public DmEventService(@org.springframework.beans.factory.annotation.Autowired(required = false) com.lopez.l2j.game.service.InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @jakarta.annotation.PostConstruct
    public void initAutoStart() {
        if (Config.DM_AUTO_START && Config.DM_ENABLED) {
            startAutoScheduler(Config.DM_DELAY_ON_BOOT);
        }
    }

    @jakarta.annotation.PreDestroy
    public void shutdownAutoScheduler() {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
        }
        if (autoScheduler != null) {
            autoScheduler.shutdownNow();
        }
    }

    public synchronized void startAutoScheduler(int initialDelayMinutes) {
        if (autoScheduler == null || autoScheduler.isShutdown()) {
            autoScheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "DM-AutoScheduler");
                t.setDaemon(true);
                return t;
            });
        }
        long delayMs = Math.max(1, initialDelayMinutes) * 60_000L;
        log.info("DM: Auto-start agendado para iniciar em {} minuto(s).", initialDelayMinutes);
        scheduledTask = autoScheduler.schedule(this::runAutoCycleRegistration, delayMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    private void runAutoCycleRegistration() {
        if (!Config.DM_ENABLED) {
            return;
        }
        openRegistration();
        long joinMs = Math.max(1, Config.DM_JOIN_TIME) * 60_000L;
        scheduledTask = autoScheduler.schedule(this::runAutoCycleFight, joinMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    private void runAutoCycleFight() {
        if (participants.size() < Math.max(2, Config.DM_MIN_PLAYERS)) {
            log.warn("DM AutoCycle: Participantes insuficientes ({} < {}). Cancelando ciclo.", participants.size(), Config.DM_MIN_PLAYERS);
            state = EventState.INACTIVE;
            participants.clear();
            scheduleNextAutoCycle();
            return;
        }
        if (startFight()) {
            long fightMs = Math.max(1, Config.DM_EVENT_TIME) * 60_000L;
            scheduledTask = autoScheduler.schedule(() -> {
                stopAndReward();
                scheduleNextAutoCycle();
            }, fightMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        } else {
            scheduleNextAutoCycle();
        }
    }

    private void scheduleNextAutoCycle() {
        if (Config.DM_AUTO_START && autoScheduler != null && !autoScheduler.isShutdown()) {
            long nextDelayMs = Math.max(1, Config.DM_NEXT_DELAY) * 60_000L;
            scheduledTask = autoScheduler.schedule(this::runAutoCycleRegistration, nextDelayMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }

    public synchronized void openRegistration() {
        openRegistration(Config.DM_MIN_LEVEL, Config.DM_MAX_LEVEL);
    }

    public synchronized void openRegistration(int minLvl, int maxLvl) {
        this.minLevel = minLvl;
        this.maxLevel = maxLvl;
        this.minPlayers = Config.DM_MIN_PLAYERS;
        this.maxPlayers = Config.DM_MAX_PLAYERS;
        this.participants.clear();
        this.state = EventState.REGISTRATION;
        log.info("DM: Registro aberto para niveis {} a {} (tempo de inscricao: {} min).", minLevel, maxLevel, Config.DM_JOIN_TIME);
    }

    public static boolean hasCursedWeapon(PlayerCharacter player) {
        if (player == null || player.inventory() == null) {
            return false;
        }
        return player.inventory().byItemId(ZARICHE_ID).isPresent()
                || player.inventory().byItemId(AKAMANAH_ID).isPresent();
    }

    public RegisterResult register(PlayerCharacter player) {
        if (!Config.DM_ENABLED || state != EventState.REGISTRATION) {
            return RegisterResult.NOT_IN_REGISTRATION;
        }
        if (participants.containsKey(player.getObjectId())) {
            return RegisterResult.ALREADY_REGISTERED;
        }
        if (Config.DM_MAX_PLAYERS > 0 && participants.size() >= Config.DM_MAX_PLAYERS) {
            return RegisterResult.MAX_PLAYERS_REACHED;
        }
        if (player.getLevel() < minLevel) {
            return RegisterResult.LEVEL_TOO_LOW;
        }
        if (player.getLevel() > maxLevel) {
            return RegisterResult.LEVEL_TOO_HIGH;
        }
        if (player.getKarma() > 0) {
            return RegisterResult.KARMA_NOT_ALLOWED;
        }
        if (!Config.DM_JOIN_WITH_CURSED_WEAPON && hasCursedWeapon(player)) {
            return RegisterResult.CURSED_WEAPON_NOT_ALLOWED;
        }
        if (player.isAio()) {
            return RegisterResult.IS_AIO_BLOCKED;
        }
        String ip = resolvePlayerIp(player);
        if (ip != null && !"127.0.0.1".equals(ip)) {
            for (Participant existing : participants.values()) {
                if (existing.getPlayer() != null && ip.equals(resolvePlayerIp(existing.getPlayer()))) {
                    log.warn("DM Anti-Dualbox: jogador {} recusado por IP duplicado {}", player.getName(), ip);
                    return RegisterResult.DUALBOX_NOT_ALLOWED;
                }
            }
        }

        Participant p = new Participant(player, player.getX(), player.getY(), player.getZ());
        participants.put(player.getObjectId(), p);
        return RegisterResult.SUCCESS;
    }

    private String resolvePlayerIp(PlayerCharacter player) {
        if (player == null) return null;
        var world = com.lopez.l2j.game.world.GameWorld.getInstance();
        if (world != null) {
            var op = world.findPlayer(player.getObjectId());
            if (op instanceof com.lopez.l2j.network.game.GameSession gs && gs.clientIp() != null && !gs.clientIp().isBlank()) {
                return gs.clientIp();
            }
        }
        return null;
    }

    public boolean unregister(PlayerCharacter player) {
        if (state != EventState.REGISTRATION) {
            return false;
        }
        return participants.remove(player.getObjectId()) != null;
    }

    public synchronized boolean startFight() {
        if (state != EventState.REGISTRATION) {
            return false;
        }
        if (participants.size() < 2) {
            log.warn("DM: Participantes insuficientes ({}). Cancelando.", participants.size());
            state = EventState.INACTIVE;
            participants.clear();
            return false;
        }
        var world = com.lopez.l2j.game.world.GameWorld.getInstance();
        int idx = 0;
        for (Participant p : participants.values()) {
            PlayerCharacter pc = p.getPlayer();
            if (pc == null) continue;
            if (pc.getParty() != null) {
                pc.getParty().removePartyMember(pc.objectId());
                pc.setParty(null);
            }
            pc.team(0);
            if (Config.DM_ON_START_REMOVE_ALL_EFFECTS) {
                pc.stopAllEffects();
            }
            if (Config.DM_ON_START_UNSUMMON_PET && (pc.hasSummon() || pc.hasPet())) {
                pc.activeSummon(null);
                pc.petObjectId(0);
            }
            pc.currentHp(pc.maxHp());
            pc.currentMp(pc.maxMp());
            pc.currentCp(pc.maxCp());
            p.setSpawnProtectedUntil(System.currentTimeMillis() + SPAWN_PROTECTION_MS);
            int[] sp = ARENA_SPAWNS[idx % ARENA_SPAWNS.length];
            idx++;
            if (world != null) {
                world.teleportPlayer(pc, sp[0], sp[1], sp[2], DM_INSTANCE_ID);
                world.player(pc.objectId()).ifPresent(op -> {
                    if (op instanceof com.lopez.l2j.network.game.GameSession gs) {
                        gs.sendUserInfoAndBroadcastCharInfo();
                    }
                });
            }
        }
        state = EventState.RUNNING;
        log.info("DM: DeathMatch (Free For All) iniciado com {} jogadores (duracao: {} min)!", participants.size(), Config.DM_EVENT_TIME);
        return true;
    }

    public boolean canInteract(int actorId, int targetId) {
        if (state != EventState.RUNNING) {
            return true;
        }
        boolean aIn = participants.containsKey(actorId);
        boolean tIn = participants.containsKey(targetId);
        if (aIn != tIn) {
            return Config.DM_ALLOW_INTERFERENCE;
        }
        return true;
    }

    public boolean canUsePotions(int objectId) {
        if (state == EventState.RUNNING && participants.containsKey(objectId)) {
            return Config.DM_ALLOW_POTIONS;
        }
        return true;
    }

    public boolean canSummon(int objectId) {
        if (state == EventState.RUNNING && participants.containsKey(objectId)) {
            return Config.DM_ALLOW_SUMMON;
        }
        return true;
    }

    public boolean onKill(int killerId, int victimId) {
        if (state != EventState.RUNNING || killerId == victimId) {
            return false;
        }
        Participant killer = participants.get(killerId);
        Participant victim = participants.get(victimId);
        if (killer == null || victim == null) {
            return false;
        }

        killer.addKill();
        victim.addDeath();
        victim.setSpawnProtectedUntil(System.currentTimeMillis() + SPAWN_PROTECTION_MS);

        PlayerCharacter vChar = victim.getPlayer();
        if (vChar != null) {
            if (Config.DM_REVIVE_RECOVERY) {
                vChar.currentHp(vChar.maxHp());
                vChar.currentMp(vChar.maxMp());
                vChar.currentCp(vChar.maxCp());
            } else {
                vChar.currentHp(Math.max(1.0, vChar.maxHp() * 0.7));
            }
            int[] sp = getRandomSpawn();
            var world = com.lopez.l2j.game.world.GameWorld.getInstance();
            if (world != null) {
                world.sendPacket(vChar.objectId(), new com.lopez.l2j.network.game.packet.GameServerPacket.Revive(vChar.objectId()));
                world.teleportPlayer(vChar, sp[0], sp[1], sp[2], DM_INSTANCE_ID);
            }
        }
        return true;
    }

    public int[] getRandomSpawn() {
        return ARENA_SPAWNS[ThreadLocalRandom.current().nextInt(ARENA_SPAWNS.length)];
    }

    public List<Participant> getRanking() {
        List<Participant> ranked = new ArrayList<>(participants.values());
        ranked.sort(Comparator.comparingInt(Participant::getKills).reversed()
                .thenComparingInt(Participant::getDeaths));
        return ranked;
    }

    public static List<int[]> parseAllRewards(String raw, int fallbackId, int fallbackCount) {
        List<int[]> list = new ArrayList<>();
        if (raw != null && !raw.isBlank()) {
            for (String token : raw.split(";")) {
                String[] kv = token.trim().split(",");
                if (kv.length >= 2) {
                    try {
                        int id = Integer.parseInt(kv[0].trim());
                        int cnt = Integer.parseInt(kv[1].trim());
                        if (id > 0 && cnt > 0) {
                            list.add(new int[]{ id, cnt });
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        if (list.isEmpty()) {
            list.add(new int[]{ fallbackId, fallbackCount });
        }
        return list;
    }

    public synchronized List<Participant> stopAndReward() {
        if (state != EventState.RUNNING) {
            return Collections.emptyList();
        }
        List<Participant> ranked = getRanking();
        List<Participant> topWinners = ranked.subList(0, Math.min(3, ranked.size()));
        var world = com.lopez.l2j.game.world.GameWorld.getInstance();
        List<int[]> configuredRewards = parseAllRewards(Config.DM_EVENT_REWARDS, rewardItemId, rewardTop1Count);

        int maxKills = ranked.isEmpty() ? 0 : ranked.get(0).getKills();
        List<Participant> tiedTop = ranked.stream()
                .filter(p -> maxKills > 0 && p.getKills() == maxKills)
                .toList();

        for (int i = 0; i < topWinners.size(); i++) {
            Participant w = topWinners.get(i);
            PlayerCharacter pc = w.getPlayer();
            if (pc != null && pc.inventory() != null && w.getKills() > 0) {
                for (int[] r : configuredRewards) {
                    int rId = r[0];
                    int rCnt;
                    if (tiedTop.size() > 1 && w.getKills() == maxKills && Config.DM_REWARD_FIRST_PLAYERS) {
                        rCnt = Math.max(1, r[1] / tiedTop.size());
                    } else {
                        rCnt = i == 0 ? r[1] : (i == 1 ? Math.max(1, r[1] / 2) : Math.max(1, r[1] / 5));
                    }
                    if (inventoryService != null) {
                        var added = inventoryService.addItem(pc.inventory(), rId, rCnt, "DmReward");
                        if (added != null && added.item() != null && world != null) {
                            world.sendPacket(pc.objectId(), new com.lopez.l2j.network.game.packet.GameServerPacket.InventoryUpdate(List.of(
                                    com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo.of(added.item(),
                                            added.created() ? com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo.ADDED : com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo.MODIFIED))));
                        }
                    } else {
                        pc.inventory().addItem(rId, rCnt);
                    }
                }
            }
        }

        for (Participant p : participants.values()) {
            PlayerCharacter pc = p.getPlayer();
            if (pc == null) continue;
            pc.currentHp(pc.maxHp());
            pc.currentMp(pc.maxMp());
            pc.currentCp(pc.maxCp());
            int retX = (p.getOrigX() != 0 || p.getOrigY() != 0) ? p.getOrigX() : 83400;
            int retY = (p.getOrigX() != 0 || p.getOrigY() != 0) ? p.getOrigY() : 147943;
            int retZ = (p.getOrigX() != 0 || p.getOrigY() != 0) ? p.getOrigZ() : -3404;
            if (world != null) {
                world.teleportPlayer(pc, retX, retY, retZ, 0);
                world.player(pc.objectId()).ifPresent(op -> {
                    if (op instanceof com.lopez.l2j.network.game.GameSession gs) {
                        gs.sendUserInfoAndBroadcastCharInfo();
                    }
                });
            }
        }

        state = EventState.INACTIVE;
        if (!topWinners.isEmpty()) {
            log.info("DM: Encerrado! 1o Lugar: {} ({} kills)", topWinners.get(0).getName(), topWinners.get(0).getKills());
        }
        return new ArrayList<>(topWinners);
    }

    public String buildStatusHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><center>");
        sb.append("<font color=\"LEVEL\">=== DeathMatch (DM) ===</font><br><br>");
        sb.append("Status: ").append(state.name()).append("<br>");
        sb.append("Niveis: ").append(minLevel).append(" - ").append(maxLevel).append("<br>");
        sb.append("Inscritos: ").append(participants.size()).append(" / ").append(Config.DM_MAX_PLAYERS).append("<br>");
        if (state == EventState.RUNNING) {
            sb.append("<br><font color=\"LEVEL\">Top Ranking:</font><br>");
            List<Participant> top = getRanking();
            for (int i = 0; i < Math.min(5, top.size()); i++) {
                Participant p = top.get(i);
                sb.append(i + 1).append(". ").append(p.getName()).append(" - ").append(p.getKills()).append(" kills<br>");
            }
        }
        sb.append("<br>");
        if (state == EventState.REGISTRATION) {
            sb.append("<button value=\"Participar\" action=\"bypass -h voiced_dmjoin\" width=95 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\"><br>");
            sb.append("<button value=\"Cancelar\" action=\"bypass -h voiced_dmleave\" width=95 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\">");
        }
        sb.append("</center></body></html>");
        return sb.toString();
    }

    // Getters
    public EventState getState() { return state; }
    public int getParticipantCount() { return participants.size(); }
    public boolean isParticipant(int objectId) { return participants.containsKey(objectId); }
    public boolean isPlayerInEvent(int objectId) { return isParticipant(objectId); }
    public Participant getParticipant(int objectId) { return participants.get(objectId); }
    public int getRewardItemId() { return rewardItemId; }
    public int getReward1ItemId() { return rewardItemId; }
    public int getReward1Count() { return rewardTop1Count; }
    public int getRewardTop1Count() { return rewardTop1Count; }
    public int getRewardTop2Count() { return rewardTop2Count; }
    public int getRewardTop3Count() { return rewardTop3Count; }
    public int getMinLevel() { return minLevel; }
    public int getMaxLevel() { return maxLevel; }
    public int getMinPlayers() { return minPlayers; }
    public int getMaxPlayers() { return maxPlayers; }
    public int getJoinTimeMinutes() { return Config.DM_JOIN_TIME; }
    public int getEventTimeMinutes() { return Config.DM_EVENT_TIME; }
    public int getReviveDelaySeconds() { return Config.DM_REVIVE_DELAY; }
}
