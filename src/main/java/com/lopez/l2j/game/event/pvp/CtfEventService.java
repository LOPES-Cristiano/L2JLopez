package com.lopez.l2j.game.event.pvp;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Motor do Evento CTF (Capture The Flag).
 * Suporta equipes Azul e Vermelho, bandeiras das bases,
 * restrição de portador (sem montaria/invisibilidade), captura na base aliada e pontuação.
 */
@Service
public class CtfEventService {

    private static final Logger log = LoggerFactory.getLogger(CtfEventService.class);

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

    public enum TeamType {
        BLUE("Blue", 0x0000FF, 149457, 46700, -3413),
        RED("Red", 0xFF0000, 147457, 46700, -3413);

        private final String teamName;
        private final int nameColor;
        private final int baseX;
        private final int baseY;
        private final int baseZ;

        TeamType(String teamName, int nameColor, int baseX, int baseY, int baseZ) {
            this.teamName = teamName;
            this.nameColor = nameColor;
            this.baseX = baseX;
            this.baseY = baseY;
            this.baseZ = baseZ;
        }

        public String getTeamName() { return teamName; }
        public int getNameColor() { return nameColor; }
        public int getBaseX() {
            String cfg = this == BLUE
                    ? (Config.CTF_BLUE_TEAM_LOC != null && !Config.CTF_BLUE_TEAM_LOC.isBlank() ? Config.CTF_BLUE_TEAM_LOC : Config.TVT_BLUE_TEAM_LOC)
                    : (Config.CTF_RED_TEAM_LOC != null && !Config.CTF_RED_TEAM_LOC.isBlank() ? Config.CTF_RED_TEAM_LOC : Config.TVT_RED_TEAM_LOC);
            return parseCoord(cfg, 0, baseX);
        }
        public int getBaseY() {
            String cfg = this == BLUE
                    ? (Config.CTF_BLUE_TEAM_LOC != null && !Config.CTF_BLUE_TEAM_LOC.isBlank() ? Config.CTF_BLUE_TEAM_LOC : Config.TVT_BLUE_TEAM_LOC)
                    : (Config.CTF_RED_TEAM_LOC != null && !Config.CTF_RED_TEAM_LOC.isBlank() ? Config.CTF_RED_TEAM_LOC : Config.TVT_RED_TEAM_LOC);
            return parseCoord(cfg, 1, baseY);
        }
        public int getBaseZ() {
            String cfg = this == BLUE
                    ? (Config.CTF_BLUE_TEAM_LOC != null && !Config.CTF_BLUE_TEAM_LOC.isBlank() ? Config.CTF_BLUE_TEAM_LOC : Config.TVT_BLUE_TEAM_LOC)
                    : (Config.CTF_RED_TEAM_LOC != null && !Config.CTF_RED_TEAM_LOC.isBlank() ? Config.CTF_RED_TEAM_LOC : Config.TVT_RED_TEAM_LOC);
            return parseCoord(cfg, 2, baseZ);
        }
        public int getFlagX() {
            return parseCoord(this == BLUE ? Config.CTF_BLUE_FLAG_LOC : Config.CTF_RED_FLAG_LOC, 0, getBaseX());
        }
        public int getFlagY() {
            return parseCoord(this == BLUE ? Config.CTF_BLUE_FLAG_LOC : Config.CTF_RED_FLAG_LOC, 1, getBaseY());
        }
        public int getFlagZ() {
            return parseCoord(this == BLUE ? Config.CTF_BLUE_FLAG_LOC : Config.CTF_RED_FLAG_LOC, 2, getBaseZ());
        }
    }

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

    public static class Participant {
        private final PlayerCharacter player;
        private final int objectId;
        private final String name;
        private final int level;
        private TeamType team;
        private int flagCaptures;
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
            this.flagCaptures = 0;
        }

        public PlayerCharacter getPlayer() { return player; }
        public int getObjectId() { return objectId; }
        public String getName() { return name; }
        public int getLevel() { return level; }
        public TeamType getTeam() { return team; }
        public void setTeam(TeamType team) { this.team = team; }
        public int getFlagCaptures() { return flagCaptures; }
        public void addCapture() { this.flagCaptures++; }
        public int getOrigX() { return origX; }
        public int getOrigY() { return origY; }
        public int getOrigZ() { return origZ; }
    }

    public enum FlagStatus {
        IN_BASE,
        CARRIED,
        DROPPED
    }

    public static final int CTF_INSTANCE_ID = 9102;
    public static final int CTF_FLAG_ITEM_ID = 6718; // Combat Flag
    public static final int ZARICHE_ID = 8190;
    public static final int AKAMANAH_ID = 8689;
    private static final int[] COLISEUM_DOORS = { 24190001, 24190002, 24190003, 24190004 };

    private volatile EventState state = EventState.INACTIVE;
    private int minLevel = Config.CTF_MIN_LEVEL;
    private int maxLevel = Config.CTF_MAX_LEVEL;
    private int minPlayers = Config.CTF_MIN_PLAYERS;
    private int maxPlayers = Config.CTF_MAX_PLAYERS;
    private int rewardItemId = Config.CTF_REWARD_ID;
    private int rewardItemCount = Config.CTF_REWARD_AMOUNT;
    private volatile boolean coliseumDoorsClosed = false;
    private final com.lopez.l2j.game.service.InventoryService inventoryService;

    private final Map<Integer, Participant> participants = new ConcurrentHashMap<>();
    private final Map<TeamType, Integer> teamScores = new ConcurrentHashMap<>();

    // Flag statuses and carriers (TeamType key is the flag belonging to that team)
    private final Map<TeamType, FlagStatus> flagStatuses = new ConcurrentHashMap<>();
    private final Map<TeamType, Integer> flagCarriers = new ConcurrentHashMap<>(); // flag team -> carrier player objectId
    private java.util.concurrent.ScheduledExecutorService autoScheduler;
    private java.util.concurrent.ScheduledFuture<?> scheduledTask;

    public CtfEventService() {
        this(null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public CtfEventService(@org.springframework.beans.factory.annotation.Autowired(required = false) com.lopez.l2j.game.service.InventoryService inventoryService) {
        this.inventoryService = inventoryService;
        resetFlags();
        teamScores.put(TeamType.BLUE, 0);
        teamScores.put(TeamType.RED, 0);
    }

    @jakarta.annotation.PostConstruct
    public void initAutoStart() {
        if (Config.CTF_AUTO_START && Config.CTF_ENABLED) {
            startAutoScheduler(Config.CTF_DELAY_ON_BOOT);
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
                Thread t = new Thread(r, "CTF-AutoScheduler");
                t.setDaemon(true);
                return t;
            });
        }
        long delayMs = Math.max(1, initialDelayMinutes) * 60_000L;
        log.info("CTF: Auto-start agendado para iniciar em {} minuto(s).", initialDelayMinutes);
        scheduledTask = autoScheduler.schedule(this::runAutoCycleRegistration, delayMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    private void runAutoCycleRegistration() {
        if (!Config.CTF_ENABLED) {
            return;
        }
        openRegistration();
        long joinMs = Math.max(1, Config.CTF_JOIN_TIME) * 60_000L;
        scheduledTask = autoScheduler.schedule(this::runAutoCycleFight, joinMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    private void runAutoCycleFight() {
        if (participants.size() < Math.max(2, Config.CTF_MIN_PLAYERS)) {
            log.warn("CTF AutoCycle: Participantes insuficientes ({} < {}). Cancelando ciclo.", participants.size(), Config.CTF_MIN_PLAYERS);
            state = EventState.INACTIVE;
            participants.clear();
            scheduleNextAutoCycle();
            return;
        }
        if (startFight()) {
            long fightMs = Math.max(1, Config.CTF_EVENT_TIME) * 60_000L;
            scheduledTask = autoScheduler.schedule(() -> {
                stopAndCalculateWinner();
                scheduleNextAutoCycle();
            }, fightMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        } else {
            scheduleNextAutoCycle();
        }
    }

    private void scheduleNextAutoCycle() {
        if (Config.CTF_AUTO_START && autoScheduler != null && !autoScheduler.isShutdown()) {
            long nextDelayMs = Math.max(1, Config.CTF_NEXT_DELAY) * 60_000L;
            scheduledTask = autoScheduler.schedule(this::runAutoCycleRegistration, nextDelayMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }

    private void toggleColiseumDoors(boolean close) {
        var world = com.lopez.l2j.game.world.GameWorld.getInstance();
        if (world == null) {
            return;
        }
        for (int doorId : COLISEUM_DOORS) {
            world.doorById(doorId).ifPresent(door -> door.open(!close));
        }
    }

    private void resetFlags() {
        flagStatuses.put(TeamType.BLUE, FlagStatus.IN_BASE);
        flagStatuses.put(TeamType.RED, FlagStatus.IN_BASE);
        flagCarriers.remove(TeamType.BLUE);
        flagCarriers.remove(TeamType.RED);
    }

    public synchronized void openRegistration() {
        openRegistration(Config.CTF_MIN_LEVEL, Config.CTF_MAX_LEVEL);
    }

    public synchronized void openRegistration(int minLvl, int maxLvl) {
        this.minLevel = minLvl;
        this.maxLevel = maxLvl;
        this.minPlayers = Config.CTF_MIN_PLAYERS;
        this.maxPlayers = Config.CTF_MAX_PLAYERS;
        this.participants.clear();
        this.teamScores.put(TeamType.BLUE, 0);
        this.teamScores.put(TeamType.RED, 0);
        resetFlags();
        this.state = EventState.REGISTRATION;
        log.info("CTF: Registro aberto para niveis {} a {} (tempo de inscricao: {} min).", minLevel, maxLevel, Config.CTF_JOIN_TIME);
    }

    public static boolean hasCursedWeapon(PlayerCharacter player) {
        if (player == null || player.inventory() == null) {
            return false;
        }
        return player.inventory().byItemId(ZARICHE_ID).isPresent()
                || player.inventory().byItemId(AKAMANAH_ID).isPresent();
    }

    public RegisterResult register(PlayerCharacter player) {
        if (!Config.CTF_ENABLED || state != EventState.REGISTRATION) {
            return RegisterResult.NOT_IN_REGISTRATION;
        }
        if (participants.containsKey(player.getObjectId())) {
            return RegisterResult.ALREADY_REGISTERED;
        }
        if (Config.CTF_MAX_PLAYERS > 0 && participants.size() >= Config.CTF_MAX_PLAYERS) {
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
        if (!Config.CTF_JOIN_WITH_CURSED_WEAPON && hasCursedWeapon(player)) {
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

    public synchronized void onDisconnect(int playerId) {
        if (state == EventState.RUNNING) {
            for (Map.Entry<TeamType, Integer> entry : flagCarriers.entrySet()) {
                if (entry.getValue() == playerId) {
                    TeamType flagTeam = entry.getKey();
                    flagStatuses.put(flagTeam, FlagStatus.IN_BASE);
                    flagCarriers.remove(flagTeam);
                    log.info("CTF: Portador {} desconectou; bandeira {} retornada a base!", playerId, flagTeam.getTeamName());
                }
            }
        }
        Participant removed = participants.remove(playerId);
        if (removed != null && removed.getPlayer() != null) {
            PlayerCharacter pc = removed.getPlayer();
            pc.team(0);
            if (pc.inventory() != null) {
                pc.inventory().destroyItemByItemId(CTF_FLAG_ITEM_ID, 1);
            }
        }
    }

    public synchronized boolean startFight() {
        if (state != EventState.REGISTRATION) {
            return false;
        }
        if (participants.size() < 2) {
            log.warn("CTF: Participantes insuficientes ({}). Cancelando.", participants.size());
            state = EventState.INACTIVE;
            participants.clear();
            return false;
        }

        this.coliseumDoorsClosed = Config.CTF_CLOSE_COLISEUM_DOORS;
        if (this.coliseumDoorsClosed) {
            toggleColiseumDoors(true);
        }
        int targetInstanceId = Config.CTF_IN_INSTANCE ? CTF_INSTANCE_ID : 0;

        List<Participant> list = new ArrayList<>(participants.values());
        list.sort(Comparator.comparingInt(Participant::getLevel).reversed());

        int blueCount = 0;
        int redCount = 0;
        var world = com.lopez.l2j.game.world.GameWorld.getInstance();
        for (Participant p : list) {
            if (blueCount <= redCount) {
                p.setTeam(TeamType.BLUE);
                blueCount++;
            } else {
                p.setTeam(TeamType.RED);
                redCount++;
            }
            PlayerCharacter pc = p.getPlayer();
            if (pc != null) {
                if (pc.getParty() != null) {
                    pc.getParty().removePartyMember(pc.objectId());
                    pc.setParty(null);
                }
                if (Config.CTF_AURA) {
                    pc.team(p.getTeam() == TeamType.BLUE ? 1 : 2);
                } else {
                    pc.team(0);
                }
                if (Config.CTF_ON_START_REMOVE_ALL_EFFECTS) {
                    pc.stopAllEffects();
                }
                if (Config.CTF_ON_START_UNSUMMON_PET && (pc.hasSummon() || pc.hasPet())) {
                    pc.activeSummon(null);
                    pc.petObjectId(0);
                }
                pc.currentHp(pc.maxHp());
                pc.currentMp(pc.maxMp());
                pc.currentCp(pc.maxCp());
                if (world != null) {
                    world.teleportPlayer(pc, p.getTeam().getBaseX(), p.getTeam().getBaseY(), p.getTeam().getBaseZ(), targetInstanceId);
                    world.player(pc.objectId()).ifPresent(op -> {
                        if (op instanceof com.lopez.l2j.network.game.GameSession gs) {
                            gs.sendUserInfoAndBroadcastCharInfo();
                        }
                    });
                }
            }
        }

        if (world != null) {
            if (targetInstanceId > 0) {
                world.removeNpcsByInstance(targetInstanceId);
            }
            var blueFlagTmpl = com.lopez.l2j.game.npc.NpcTemplate.fallback(35062, "Blue Flag", "L2Npc");
            var redFlagTmpl = com.lopez.l2j.game.npc.NpcTemplate.fallback(35062, "Red Flag", "L2Npc");
            var blueFlag = new com.lopez.l2j.game.npc.NpcInstance(0x57000001, blueFlagTmpl, TeamType.BLUE.getFlagX(), TeamType.BLUE.getFlagY(), TeamType.BLUE.getFlagZ(), 0);
            blueFlag.instanceId(targetInstanceId);
            var redFlag = new com.lopez.l2j.game.npc.NpcInstance(0x57000002, redFlagTmpl, TeamType.RED.getFlagX(), TeamType.RED.getFlagY(), TeamType.RED.getFlagZ(), 0);
            redFlag.instanceId(targetInstanceId);
            world.addNpc(blueFlag);
            world.addNpc(redFlag);
        }

        teamScores.put(TeamType.BLUE, 0);
        teamScores.put(TeamType.RED, 0);
        resetFlags();
        state = EventState.RUNNING;
        log.info("CTF: Evento iniciado com {} jogadores na instancia {} (duracao: {} min, portas fechadas: {})!",
                list.size(), targetInstanceId, Config.CTF_EVENT_TIME, coliseumDoorsClosed);
        return true;
    }

    public boolean canInteract(int actorId, int targetId) {
        if (state != EventState.RUNNING) {
            return true;
        }
        boolean aIn = participants.containsKey(actorId);
        boolean tIn = participants.containsKey(targetId);
        if (aIn != tIn) {
            return Config.CTF_ALLOW_INTERFERENCE;
        }
        return true;
    }

    public boolean canTeamCast(int casterId, int targetId) {
        if (state != EventState.RUNNING) {
            return true;
        }
        Participant caster = participants.get(casterId);
        Participant target = participants.get(targetId);
        if (caster != null && target != null && caster.getTeam() == target.getTeam()) {
            return Config.CTF_ALLOW_TEAM_CASTING;
        }
        return true;
    }

    public boolean canUsePotions(int objectId) {
        if (state == EventState.RUNNING && participants.containsKey(objectId)) {
            return Config.CTF_ALLOW_POTIONS;
        }
        return true;
    }

    public boolean canSummon(int objectId) {
        if (state == EventState.RUNNING && participants.containsKey(objectId)) {
            return Config.CTF_ALLOW_SUMMON;
        }
        return true;
    }

    public void reviveParticipant(int victimId) {
        Participant victim = participants.get(victimId);
        if (victim == null || victim.getPlayer() == null || victim.getTeam() == null) {
            return;
        }
        dropFlag(victimId);
        PlayerCharacter vChar = victim.getPlayer();
        if (Config.CTF_REVIVE_RECOVERY) {
            vChar.currentHp(vChar.maxHp());
            vChar.currentMp(vChar.maxMp());
            vChar.currentCp(vChar.maxCp());
        } else {
            vChar.currentHp(Math.max(1.0, vChar.maxHp() * 0.7));
        }
        int targetInstanceId = Config.CTF_IN_INSTANCE ? CTF_INSTANCE_ID : 0;
        var world = com.lopez.l2j.game.world.GameWorld.getInstance();
        if (world != null) {
            world.sendPacket(vChar.objectId(), new com.lopez.l2j.network.game.packet.GameServerPacket.Revive(vChar.objectId()));
            world.teleportPlayer(vChar, victim.getTeam().getBaseX(), victim.getTeam().getBaseY(), victim.getTeam().getBaseZ(), targetInstanceId);
        }
    }

    /**
     * O jogador tenta pegar a bandeira inimiga.
     * Retorna true se pegou com sucesso.
     */
    public synchronized boolean takeEnemyFlag(int playerId) {
        if (state != EventState.RUNNING) {
            return false;
        }
        Participant p = participants.get(playerId);
        if (p == null) {
            return false;
        }

        TeamType enemyTeam = (p.getTeam() == TeamType.BLUE) ? TeamType.RED : TeamType.BLUE;
        FlagStatus status = flagStatuses.get(enemyTeam);

        if (status == FlagStatus.IN_BASE || status == FlagStatus.DROPPED) {
            flagStatuses.put(enemyTeam, FlagStatus.CARRIED);
            flagCarriers.put(enemyTeam, playerId);
            PlayerCharacter pc = p.getPlayer();
            if (pc != null && pc.inventory() != null) {
                var flagItem = pc.inventory().addItem(CTF_FLAG_ITEM_ID, 1);
                if (flagItem != null) {
                    flagItem.equipped(true);
                }
            }
            log.info("CTF: Jogador {} pegou a bandeira da equipe {}!", p.getName(), enemyTeam.getTeamName());
            return true;
        }
        return false;
    }

    /**
     * Solta a bandeira carregada (ex: ao morrer).
     */
    public synchronized boolean dropFlag(int playerId) {
        if (state != EventState.RUNNING) {
            return false;
        }
        for (Map.Entry<TeamType, Integer> entry : flagCarriers.entrySet()) {
            if (entry.getValue() == playerId) {
                TeamType flagTeam = entry.getKey();
                flagStatuses.put(flagTeam, FlagStatus.DROPPED);
                flagCarriers.remove(flagTeam);
                Participant p = participants.get(playerId);
                if (p != null && p.getPlayer() != null && p.getPlayer().inventory() != null) {
                    p.getPlayer().inventory().destroyItemByItemId(CTF_FLAG_ITEM_ID, 1);
                }
                log.info("CTF: Bandeira {} derrubada no chao!", flagTeam.getTeamName());
                return true;
            }
        }
        return false;
    }

    /**
     * Retorna a bandeira derrubada de volta para a base.
     */
    public synchronized boolean returnFlagToBase(int playerId, TeamType flagTeam) {
        if (state != EventState.RUNNING) {
            return false;
        }
        Participant p = participants.get(playerId);
        if (p == null || p.getTeam() != flagTeam) {
            return false; // Apenas o dono da bandeira pode devolve-la
        }
        if (flagStatuses.get(flagTeam) == FlagStatus.DROPPED) {
            flagStatuses.put(flagTeam, FlagStatus.IN_BASE);
            log.info("CTF: Bandeira {} retornada para a base!", flagTeam.getTeamName());
            return true;
        }
        return false;
    }

    /**
     * O portador da bandeira inimiga chega a sua base para pontuar.
     */
    public synchronized boolean captureFlag(int playerId) {
        if (state != EventState.RUNNING) {
            return false;
        }
        Participant p = participants.get(playerId);
        if (p == null) {
            return false;
        }
        TeamType myTeam = p.getTeam();
        TeamType enemyTeam = (myTeam == TeamType.BLUE) ? TeamType.RED : TeamType.BLUE;

        // Player must be carrying enemy flag
        Integer carrierId = flagCarriers.get(enemyTeam);
        if (carrierId == null || carrierId != playerId) {
            return false;
        }
        // Player's own team flag must be safely in base
        if (flagStatuses.get(myTeam) != FlagStatus.IN_BASE) {
            return false;
        }

        // Score!
        p.addCapture();
        teamScores.merge(myTeam, 1, Integer::sum);
        // Reset enemy flag back to base
        flagStatuses.put(enemyTeam, FlagStatus.IN_BASE);
        flagCarriers.remove(enemyTeam);
        if (p.getPlayer() != null && p.getPlayer().inventory() != null) {
            p.getPlayer().inventory().destroyItemByItemId(CTF_FLAG_ITEM_ID, 1);
        }
        log.info("CTF: GOAL! {} capturou a bandeira inimiga! Placar: Azul {} x {} Vermelho",
                p.getName(), teamScores.get(TeamType.BLUE), teamScores.get(TeamType.RED));
        return true;
    }

    public synchronized TeamType stopAndCalculateWinner() {
        if (state != EventState.RUNNING) {
            return null;
        }
        int blueScore = teamScores.getOrDefault(TeamType.BLUE, 0);
        int redScore = teamScores.getOrDefault(TeamType.RED, 0);

        TeamType winner;
        if (blueScore > redScore) {
            winner = TeamType.BLUE;
        } else if (redScore > blueScore) {
            winner = TeamType.RED;
        } else {
            winner = null; // Tie
        }

        if (this.coliseumDoorsClosed) {
            toggleColiseumDoors(false);
        }
        this.coliseumDoorsClosed = false;

        var world = com.lopez.l2j.game.world.GameWorld.getInstance();
        if (world != null) {
            world.removeNpcsByInstance(CTF_INSTANCE_ID);
        }
        for (Participant p : participants.values()) {
            PlayerCharacter pc = p.getPlayer();
            if (pc == null) continue;
            pc.team(0);
            pc.currentHp(pc.maxHp());
            pc.currentMp(pc.maxMp());
            pc.currentCp(pc.maxCp());
            if (pc.inventory() != null) {
                pc.inventory().destroyItemByItemId(CTF_FLAG_ITEM_ID, 1);
            }
            if (winner != null && p.getTeam() == winner && rewardItemCount > 0 && pc.inventory() != null) {
                if (inventoryService != null) {
                    var added = inventoryService.addItem(pc.inventory(), rewardItemId, rewardItemCount, "CtfReward");
                    if (added != null && added.item() != null && world != null) {
                        world.sendPacket(pc.objectId(), new com.lopez.l2j.network.game.packet.GameServerPacket.InventoryUpdate(List.of(
                                com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo.of(added.item(),
                                        added.created() ? com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo.ADDED : com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo.MODIFIED))));
                    }
                } else {
                    pc.inventory().addItem(rewardItemId, rewardItemCount);
                }
            }
            int retX = (Config.CTF_ORIGINAL_POSITION || p.getOrigX() != 0 || p.getOrigY() != 0) ? p.getOrigX() : 83400;
            int retY = (Config.CTF_ORIGINAL_POSITION || p.getOrigX() != 0 || p.getOrigY() != 0) ? p.getOrigY() : 147943;
            int retZ = (Config.CTF_ORIGINAL_POSITION || p.getOrigX() != 0 || p.getOrigY() != 0) ? p.getOrigZ() : -3404;
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
        resetFlags();
        log.info("CTF: Encerrado! Vencedor: {}", winner);
        return winner;
    }

    public boolean isFlagCarrier(int playerId) {
        return flagCarriers.containsValue(playerId);
    }

    public String buildStatusHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><center>");
        sb.append("<font color=\"LEVEL\">=== Capture The Flag (CTF) ===</font><br><br>");
        sb.append("Status: ").append(state.name()).append("<br>");
        sb.append("Niveis: ").append(minLevel).append(" - ").append(maxLevel).append("<br>");
        sb.append("Inscritos: ").append(participants.size()).append(" / ").append(Config.CTF_MAX_PLAYERS).append("<br>");
        if (state == EventState.RUNNING) {
            sb.append("<br><font color=\"8FA8BE\">Bandeiras Azul: </font>").append(teamScores.getOrDefault(TeamType.BLUE, 0)).append("<br>");
            sb.append("<font color=\"B86B6B\">Bandeiras Vermelho: </font>").append(teamScores.getOrDefault(TeamType.RED, 0)).append("<br>");
            sb.append("Status Bandeira Azul: ").append(flagStatuses.get(TeamType.BLUE)).append("<br>");
            sb.append("Status Bandeira Vermelha: ").append(flagStatuses.get(TeamType.RED)).append("<br>");
        }
        sb.append("<br>");
        if (state == EventState.REGISTRATION) {
            sb.append("<button value=\"Participar\" action=\"bypass -h voiced_ctfjoin\" width=95 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\"><br>");
            sb.append("<button value=\"Cancelar\" action=\"bypass -h voiced_ctfleave\" width=95 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\">");
        }
        sb.append("</center></body></html>");
        return sb.toString();
    }

    // Getters
    public EventState getState() { return state; }
    public boolean isParticipant(int objectId) { return participants.containsKey(objectId); }
    public boolean isPlayerInEvent(int objectId) { return isParticipant(objectId); }
    public Participant getParticipant(int objectId) { return participants.get(objectId); }
    public Map<Integer, Participant> getParticipants() { return Collections.unmodifiableMap(participants); }
    public int getTeamScore(TeamType team) { return teamScores.getOrDefault(team, 0); }
    public int getBlueCount() {
        return (int) participants.values().stream().filter(p -> p.getTeam() == TeamType.BLUE).count();
    }
    public int getRedCount() {
        return (int) participants.values().stream().filter(p -> p.getTeam() == TeamType.RED).count();
    }
    public FlagStatus getFlagStatus(TeamType team) { return flagStatuses.get(team); }
    public Integer getFlagCarrier(TeamType team) { return flagCarriers.get(team); }
    public int getRewardItemId() { return rewardItemId; }
    public void setRewardItemId(int rewardItemId) { this.rewardItemId = rewardItemId; }
    public int getRewardItemCount() { return rewardItemCount; }
    public void setRewardItemCount(int rewardItemCount) { this.rewardItemCount = rewardItemCount; }
    public int getMinLevel() { return minLevel; }
    public int getMaxLevel() { return maxLevel; }
    public int getMinPlayers() { return minPlayers; }
    public int getMaxPlayers() { return maxPlayers; }
    public int getJoinTimeMinutes() { return Config.CTF_JOIN_TIME; }
    public int getEventTimeMinutes() { return Config.CTF_EVENT_TIME; }
    public int getReviveDelaySeconds() { return Config.CTF_REVIVE_DELAY; }
    public boolean isColiseumDoorsClosed() { return coliseumDoorsClosed; }
}
