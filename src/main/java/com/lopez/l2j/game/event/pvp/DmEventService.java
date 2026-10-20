package com.lopez.l2j.game.event.pvp;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Motor do Evento DM (DeathMatch / Todos Contra Todos).
 * Arena isolada (Coliseu ou Fantasy Isle), contagem de mortes individuais,
 * ranking em tempo real do evento e premiação do Top 1, 2 e 3.
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
        MAX_PLAYERS_REACHED
    }

    public static class Participant {
        private final PlayerCharacter player;
        private final int objectId;
        private final String name;
        private final int level;
        private int kills;
        private final int origX;
        private final int origY;
        private final int origZ;

        public Participant(PlayerCharacter player, int origX, int origY, int origZ) {
            this.player = player;
            this.objectId = player.getObjectId();
            this.name = player.getName();
            this.level = player.getLevel();
            this.origX = origX;
            this.origY = origY;
            this.origZ = origZ;
            this.kills = 0;
        }

        public PlayerCharacter getPlayer() { return player; }
        public int getObjectId() { return objectId; }
        public String getName() { return name; }
        public int getLevel() { return level; }
        public int getKills() { return kills; }
        public void addKill() { this.kills++; }
        public int getOrigX() { return origX; }
        public int getOrigY() { return origY; }
        public int getOrigZ() { return origZ; }
    }

    public static final int DM_INSTANCE_ID = 9103;
    public static final int ZARICHE_ID = 8190;
    public static final int AKAMANAH_ID = 8689;

    private volatile EventState state = EventState.INACTIVE;
    private int minLevel = Config.DM_MIN_LEVEL;
    private int maxLevel = Config.DM_MAX_LEVEL;
    private int minPlayers = Config.DM_MIN_PLAYERS;
    private int maxPlayers = Config.DM_MAX_PLAYERS;
    private int arenaX = parseCoord(Config.DM_EVENT_LOCATION, 0, 149457);
    private int arenaY = parseCoord(Config.DM_EVENT_LOCATION, 1, 46700);
    private int arenaZ = parseCoord(Config.DM_EVENT_LOCATION, 2, -3413);

    // Top 1, 2, 3 rewards
    private int reward1ItemId = Config.DM_REWARD_ID;
    private int reward1Count = Config.DM_REWARD_AMOUNT;
    private int reward2ItemId = Config.DM_REWARD_ID;
    private int reward2Count = Config.DM_REWARD_AMOUNT / 2;
    private int reward3ItemId = Config.DM_REWARD_ID;
    private int reward3Count = Config.DM_REWARD_AMOUNT / 4;
    private final com.lopez.l2j.game.service.InventoryService inventoryService;

    private final Map<Integer, Participant> participants = new ConcurrentHashMap<>();
    private java.util.concurrent.ScheduledExecutorService autoScheduler;
    private java.util.concurrent.ScheduledFuture<?> scheduledTask;

    private static int parseCoord(String locStr, int index, int fallback) {
        if (locStr == null || locStr.isBlank()) {
            return fallback;
        }
        String[] parts = locStr.split(",");
        if (parts.length <= index) {
            return fallback;
        }
        try {
            return Integer.parseInt(parts[index].trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

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
        long regMs = Math.max(1, Config.DM_REG_TIME) * 60_000L;
        scheduledTask = autoScheduler.schedule(this::runAutoCycleFight, regMs, java.util.concurrent.TimeUnit.MILLISECONDS);
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
        this.arenaX = parseCoord(Config.DM_EVENT_LOCATION, 0, 149457);
        this.arenaY = parseCoord(Config.DM_EVENT_LOCATION, 1, 46700);
        this.arenaZ = parseCoord(Config.DM_EVENT_LOCATION, 2, -3413);
        this.participants.clear();
        this.state = EventState.REGISTRATION;
        log.info("DM: Registro aberto para niveis {} a {} (tempo de inscricao: {} min).", minLevel, maxLevel, Config.DM_REG_TIME);
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
        if (!Config.DM_CURSED_WEAPON && hasCursedWeapon(player)) {
            return RegisterResult.CURSED_WEAPON_NOT_ALLOWED;
        }
        if (player.isAio()) {
            return RegisterResult.IS_AIO_BLOCKED;
        }

        Participant p = new Participant(player, player.getX(), player.getY(), player.getZ());
        participants.put(player.getObjectId(), p);
        return RegisterResult.SUCCESS;
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
            if (pc != null) {
                if (Config.DM_AURA) {
                    pc.team(1 + (idx % 2));
                }
                if (Config.DM_ON_START_REMOVE_ALL_EFFECTS) {
                    pc.stopAllEffects();
                }
                if (Config.DM_ON_START_UNSUMMON_PET && (pc.hasSummon() || pc.hasPet())) {
                    pc.activeSummon(null);
                    pc.petObjectId(0);
                }
                if (Config.DM_RESET_ALL_SKILL) {
                    pc.clearControls();
                }
                if (Config.DM_ON_START_RESTORE_HP_MP_CP) {
                    pc.currentHp(pc.maxHp());
                    pc.currentMp(pc.maxMp());
                    pc.currentCp(pc.maxCp());
                }
                double angle = (idx++ % 8) * (Math.PI * 2.0 / 8.0);
                int sx = arenaX + (int) (Math.cos(angle) * 200);
                int sy = arenaY + (int) (Math.sin(angle) * 200);
                if (world != null) {
                    world.teleportPlayer(pc, sx, sy, arenaZ, DM_INSTANCE_ID);
                }
            }
        }

        state = EventState.RUNNING;
        log.info("DM: Batalha iniciada com {} gladiadores no DeathMatch (duracao: {} min)!", participants.size(), Config.DM_EVENT_TIME);
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
            return Config.DM_ALLOW_POTION;
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
        if (state != EventState.RUNNING) {
            return false;
        }
        Participant killer = participants.get(killerId);
        Participant victim = participants.get(victimId);
        if (killer == null || victim == null || killerId == victimId) {
            return false;
        }

        killer.addKill();
        PlayerCharacter vChar = victim.getPlayer();
        if (vChar != null) {
            vChar.currentHp(vChar.maxHp());
            vChar.currentMp(vChar.maxMp());
            vChar.currentCp(vChar.maxCp());
            var world = com.lopez.l2j.game.world.GameWorld.getInstance();
            if (world != null) {
                world.sendPacket(vChar.objectId(), new com.lopez.l2j.network.game.packet.GameServerPacket.Revive(vChar.objectId()));
                world.teleportPlayer(vChar, arenaX, arenaY, arenaZ, DM_INSTANCE_ID);
            }
        }
        log.info("DM: {} abateu {}! Frags de {}: {}", killer.getName(), victim.getName(), killer.getName(), killer.getKills());
        return true;
    }

    public List<Participant> getRanking() {
        List<Participant> list = new ArrayList<>(participants.values());
        list.sort(Comparator.comparingInt(Participant::getKills).reversed());
        return list;
    }

    public synchronized List<Participant> stopAndReward() {
        if (state != EventState.RUNNING) {
            return Collections.emptyList();
        }
        List<Participant> ranking = getRanking();
        var world = com.lopez.l2j.game.world.GameWorld.getInstance();
        for (int i = 0; i < ranking.size(); i++) {
            Participant p = ranking.get(i);
            PlayerCharacter pc = p.getPlayer();
            if (pc == null) continue;
            pc.team(0);
            if (Config.DM_RESET_ALL_SKILL) {
                pc.clearControls();
            }
            pc.currentHp(pc.maxHp());
            pc.currentMp(pc.maxMp());
            pc.currentCp(pc.maxCp());
            if (i < 3 && p.getKills() > 0 && pc.inventory() != null) {
                int rId = (i == 0) ? reward1ItemId : (i == 1 ? reward2ItemId : reward3ItemId);
                int rCnt = (i == 0) ? reward1Count : (i == 1 ? reward2Count : reward3Count);
                if (rCnt > 0) {
                    if (inventoryService != null) {
                        var added = inventoryService.addItem(pc.inventory(), rId, rCnt, "DmReward");
                        if (added != null && added.item() != null && world != null) {
                            world.sendPacket(pc.objectId(), new com.lopez.l2j.network.game.packet.GameServerPacket.InventoryUpdate(List.of(
                                    com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo.of(added.item(),
                                            added.created() ? com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo.ADDED : com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo.MODIFIED))));
                        }
                    } else {
                        pc.inventory().byItemId(rId).ifPresent(it -> it.count(it.count() + rCnt));
                    }
                }
            }
            int retX = (Config.DM_ORIGINAL_POSITION || p.getOrigX() != 0 || p.getOrigY() != 0) ? p.getOrigX() : 83400;
            int retY = (Config.DM_ORIGINAL_POSITION || p.getOrigX() != 0 || p.getOrigY() != 0) ? p.getOrigY() : 147943;
            int retZ = (Config.DM_ORIGINAL_POSITION || p.getOrigX() != 0 || p.getOrigY() != 0) ? p.getOrigZ() : -3404;
            if (world != null) {
                world.teleportPlayer(pc, retX, retY, retZ, 0);
            }
        }
        state = EventState.INACTIVE;
        log.info("DM: Evento encerrado! Total gladiadores: {}", ranking.size());
        return ranking;
    }

    public String buildStatusHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><center>");
        sb.append("<font color=\"LEVEL\">=== DeathMatch Arena (FFA) ===</font><br><br>");
        sb.append("Status: ").append(state.name()).append("<br>");
        sb.append("Niveis: ").append(minLevel).append(" - ").append(maxLevel).append("<br>");
        sb.append("Participantes: ").append(participants.size()).append(" / ").append(Config.DM_MAX_PLAYERS).append("<br><br>");

        if (state == EventState.RUNNING) {
            sb.append("<table width=240>");
            sb.append("<tr><td><font color=\"aaccff\">Pos</font></td><td><font color=\"aaccff\">Nome</font></td><td><font color=\"aaccff\">Kills</font></td></tr>");
            List<Participant> ranking = getRanking();
            int limit = Math.min(5, ranking.size());
            for (int i = 0; i < limit; i++) {
                Participant p = ranking.get(i);
                sb.append("<tr><td>").append(i + 1).append("</td><td>").append(p.getName()).append("</td><td>").append(p.getKills()).append("</td></tr>");
            }
            sb.append("</table><br>");
        }

        if (state == EventState.REGISTRATION) {
            sb.append("<button value=\"Participar\" action=\"bypass -h voiced_dmjoin\" width=95 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\"><br>");
            sb.append("<button value=\"Cancelar\" action=\"bypass -h voiced_dmleave\" width=95 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\">");
        }
        sb.append("</center></body></html>");
        return sb.toString();
    }

    // Getters and Setters
    public EventState getState() { return state; }
    public boolean isParticipant(int objectId) { return participants.containsKey(objectId); }
    public Participant getParticipant(int objectId) { return participants.get(objectId); }
    public Map<Integer, Participant> getParticipants() { return Collections.unmodifiableMap(participants); }
    public int getArenaX() { return arenaX; }
    public int getArenaY() { return arenaY; }
    public int getArenaZ() { return arenaZ; }
    public int getReward1ItemId() { return reward1ItemId; }
    public int getReward1Count() { return reward1Count; }
    public int getReward2ItemId() { return reward2ItemId; }
    public int getReward2Count() { return reward2Count; }
    public int getReward3ItemId() { return reward3ItemId; }
    public int getReward3Count() { return reward3Count; }
    public int getMinLevel() { return minLevel; }
    public int getMaxLevel() { return maxLevel; }
    public int getMinPlayers() { return minPlayers; }
    public int getMaxPlayers() { return maxPlayers; }
    public int getRegTimeMinutes() { return Config.DM_REG_TIME; }
    public int getEventTimeMinutes() { return Config.DM_EVENT_TIME; }
    public int getReviveDelaySeconds() { return Config.DM_REVIVE_DELAY; }
}
