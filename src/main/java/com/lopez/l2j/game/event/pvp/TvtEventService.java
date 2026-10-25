package com.lopez.l2j.game.event.pvp;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Motor do Evento TvT (Team vs Team).
 * Suporta registro, balanceamento de equipes (Azul e Vermelho),
 * bloqueio de classes/buffs proibidos, contagem de abates e teleporte de retorno.
 */
@Service
public class TvtEventService {

    private static final Logger log = LoggerFactory.getLogger(TvtEventService.class);

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
        private final int spawnX;
        private final int spawnY;
        private final int spawnZ;

        TeamType(String teamName, int nameColor, int spawnX, int spawnY, int spawnZ) {
            this.teamName = teamName;
            this.nameColor = nameColor;
            this.spawnX = spawnX;
            this.spawnY = spawnY;
            this.spawnZ = spawnZ;
        }

        public String getTeamName() { return teamName; }
        public int getNameColor() { return nameColor; }
        public int getSpawnX() { return parseCoord(this == BLUE ? Config.TVT_BLUE_TEAM_LOC : Config.TVT_RED_TEAM_LOC, 0, spawnX); }
        public int getSpawnY() { return parseCoord(this == BLUE ? Config.TVT_BLUE_TEAM_LOC : Config.TVT_RED_TEAM_LOC, 1, spawnY); }
        public int getSpawnZ() { return parseCoord(this == BLUE ? Config.TVT_BLUE_TEAM_LOC : Config.TVT_RED_TEAM_LOC, 2, spawnZ); }
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
        public TeamType getTeam() { return team; }
        public void setTeam(TeamType team) { this.team = team; }
        public int getKills() { return kills; }
        public void addKill() { this.kills++; }
        public int getOrigX() { return origX; }
        public int getOrigY() { return origY; }
        public int getOrigZ() { return origZ; }
    }

    public static final int TVT_INSTANCE_ID = 9101;
    public static final int ZARICHE_ID = 8190;
    public static final int AKAMANAH_ID = 8689;
    private static final int[] COLISEUM_DOORS = { 24190001, 24190002, 24190003, 24190004 };

    private volatile EventState state = EventState.INACTIVE;
    private int minLevel = Config.TVT_MIN_LEVEL;
    private int maxLevel = Config.TVT_MAX_LEVEL;
    private int minPlayers = Config.TVT_MIN_PLAYERS;
    private int maxPlayers = Config.TVT_MAX_PLAYERS;
    private int rewardItemId = Config.TVT_REWARD_ID;
    private int rewardItemCount = Config.TVT_REWARD_AMOUNT;
    private volatile boolean coliseumDoorsClosed = false;
    private final com.lopez.l2j.game.service.InventoryService inventoryService;

    private final Map<Integer, Participant> participants = new ConcurrentHashMap<>();
    private final Map<TeamType, Integer> teamScores = new ConcurrentHashMap<>();
    private java.util.concurrent.ScheduledExecutorService autoScheduler;
    private java.util.concurrent.ScheduledFuture<?> scheduledTask;

    public TvtEventService() {
        this(null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public TvtEventService(@org.springframework.beans.factory.annotation.Autowired(required = false) com.lopez.l2j.game.service.InventoryService inventoryService) {
        this.inventoryService = inventoryService;
        teamScores.put(TeamType.BLUE, 0);
        teamScores.put(TeamType.RED, 0);
    }

    @jakarta.annotation.PostConstruct
    public void initAutoStart() {
        if (Config.TVT_AUTO_START && Config.TVT_ENABLED) {
            startAutoScheduler(Config.TVT_DELAY_ON_BOOT);
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
                Thread t = new Thread(r, "TvT-AutoScheduler");
                t.setDaemon(true);
                return t;
            });
        }
        long delayMs = Math.max(1, initialDelayMinutes) * 60_000L;
        log.info("TvT: Auto-start agendado para iniciar em {} minuto(s).", initialDelayMinutes);
        scheduledTask = autoScheduler.schedule(this::runAutoCycleRegistration, delayMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    private void runAutoCycleRegistration() {
        if (!Config.TVT_ENABLED) {
            return;
        }
        openRegistration();
        long joinMs = Math.max(1, Config.TVT_JOIN_TIME) * 60_000L;
        scheduledTask = autoScheduler.schedule(this::runAutoCycleFight, joinMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    private void runAutoCycleFight() {
        if (participants.size() < Math.max(2, Config.TVT_MIN_PLAYERS)) {
            log.warn("TvT AutoCycle: Participantes insuficientes ({} < {}). Cancelando ciclo.", participants.size(), Config.TVT_MIN_PLAYERS);
            state = EventState.INACTIVE;
            participants.clear();
            scheduleNextAutoCycle();
            return;
        }
        if (startFight()) {
            long fightMs = Math.max(1, Config.TVT_EVENT_TIME) * 60_000L;
            scheduledTask = autoScheduler.schedule(() -> {
                stopAndCalculateWinner();
                scheduleNextAutoCycle();
            }, fightMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        } else {
            scheduleNextAutoCycle();
        }
    }

    private void scheduleNextAutoCycle() {
        if (Config.TVT_AUTO_START && autoScheduler != null && !autoScheduler.isShutdown()) {
            long nextDelayMs = Math.max(1, Config.TVT_NEXT_DELAY) * 60_000L;
            scheduledTask = autoScheduler.schedule(this::runAutoCycleRegistration, nextDelayMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }

    private void announce(String msg) {
        var world = com.lopez.l2j.game.world.GameWorld.getInstance();
        if (world != null) {
            var pkt = new com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay(
                    0, com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay.ANNOUNCEMENT, "TvT", msg);
            world.broadcast(pkt);
        }
    }

    private void toggleColiseumDoors(boolean close) {
        var world = com.lopez.l2j.game.world.GameWorld.getInstance();
        if (world == null) {
            return;
        }
        for (int doorId : COLISEUM_DOORS) {
            world.doorById(doorId).ifPresent(door -> {
                door.open(!close);
            });
        }
    }

    public synchronized void openRegistration() {
        openRegistration(Config.TVT_MIN_LEVEL, Config.TVT_MAX_LEVEL);
    }

    public synchronized void openRegistration(int minLvl, int maxLvl) {
        this.minLevel = minLvl;
        this.maxLevel = maxLvl;
        this.minPlayers = Config.TVT_MIN_PLAYERS;
        this.maxPlayers = Config.TVT_MAX_PLAYERS;
        this.participants.clear();
        this.teamScores.put(TeamType.BLUE, 0);
        this.teamScores.put(TeamType.RED, 0);
        this.state = EventState.REGISTRATION;
        announce("Registro para Team vs Team (TvT) aberto para niveis " + minLevel + "-" + maxLevel + "! Digite .tvtjoin");
        log.info("TvT: Registro aberto para niveis {} a {} (tempo de inscricao: {} min).", minLevel, maxLevel, Config.TVT_JOIN_TIME);
    }

    public static boolean hasCursedWeapon(PlayerCharacter player) {
        if (player == null || player.inventory() == null) {
            return false;
        }
        return player.inventory().byItemId(ZARICHE_ID).isPresent()
                || player.inventory().byItemId(AKAMANAH_ID).isPresent();
    }

    public RegisterResult register(PlayerCharacter player) {
        if (!Config.TVT_ENABLED || state != EventState.REGISTRATION) {
            return RegisterResult.NOT_IN_REGISTRATION;
        }
        if (participants.containsKey(player.getObjectId())) {
            return RegisterResult.ALREADY_REGISTERED;
        }
        if (Config.TVT_MAX_PLAYERS > 0 && participants.size() >= Config.TVT_MAX_PLAYERS) {
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
        if (!Config.TVT_JOIN_WITH_CURSED_WEAPON && hasCursedWeapon(player)) {
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
        Participant removed = participants.remove(playerId);
        if (removed != null && removed.getPlayer() != null) {
            PlayerCharacter pc = removed.getPlayer();
            pc.team(0);
            if (state == EventState.RUNNING) {
                int retX = (Config.TVT_ORIGINAL_POSITION || removed.getOrigX() != 0 || removed.getOrigY() != 0) ? removed.getOrigX() : 83400;
                int retY = (Config.TVT_ORIGINAL_POSITION || removed.getOrigX() != 0 || removed.getOrigY() != 0) ? removed.getOrigY() : 147943;
                int retZ = (Config.TVT_ORIGINAL_POSITION || removed.getOrigX() != 0 || removed.getOrigY() != 0) ? removed.getOrigZ() : -3404;
                pc.teleport(retX, retY, retZ);
                pc.instanceId(0);
            }
        }
    }

    public synchronized boolean startFight() {
        if (state != EventState.REGISTRATION) {
            return false;
        }
        if (participants.size() < 2) {
            log.warn("TvT: Participantes insuficientes ({}). Cancelando evento.", participants.size());
            state = EventState.INACTIVE;
            participants.clear();
            return false;
        }

        this.coliseumDoorsClosed = Config.TVT_CLOSE_COLISEUM_DOORS;
        if (this.coliseumDoorsClosed) {
            toggleColiseumDoors(true);
        }

        // Divide and balance teams by level
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
                if (Config.TVT_AURA) {
                    pc.team(p.getTeam() == TeamType.BLUE ? 1 : 2);
                } else {
                    pc.team(0);
                }
                if (Config.TVT_ON_START_REMOVE_ALL_EFFECTS) {
                    pc.stopAllEffects();
                }
                if (Config.TVT_ON_START_UNSUMMON_PET && (pc.hasSummon() || pc.hasPet())) {
                    pc.activeSummon(null);
                    pc.petObjectId(0);
                }
                pc.currentHp(pc.maxHp());
                pc.currentMp(pc.maxMp());
                pc.currentCp(pc.maxCp());
                if (world != null) {
                    world.teleportPlayer(pc, p.getTeam().getSpawnX(), p.getTeam().getSpawnY(), p.getTeam().getSpawnZ(), TVT_INSTANCE_ID);
                    world.player(pc.objectId()).ifPresent(op -> {
                        if (op instanceof com.lopez.l2j.network.game.GameSession gs) {
                            gs.sendUserInfoAndBroadcastCharInfo();
                        }
                    });
                }
            }
        }

        teamScores.put(TeamType.BLUE, 0);
        teamScores.put(TeamType.RED, 0);
        state = EventState.RUNNING;
        announce("Batalha TvT iniciada! Time Azul (" + blueCount + ") vs Time Vermelho (" + redCount + ").");
        log.info("TvT: Batalha iniciada! Azul: {} players, Vermelho: {} players (duracao: {} min, portas fechadas: {}).",
                blueCount, redCount, Config.TVT_EVENT_TIME, coliseumDoorsClosed);
        return true;
    }

    public boolean canAttack(int attackerId, int targetId) {
        if (state != EventState.RUNNING) {
            return true;
        }
        Participant att = participants.get(attackerId);
        Participant tgt = participants.get(targetId);
        if (att == null && tgt == null) {
            return true;
        }
        if (att == null || tgt == null) {
            return Config.TVT_ALLOW_INTERFERENCE;
        }
        if (att.getTeam() == tgt.getTeam()) {
            return Config.TVT_ALLOW_TEAM_ATTACKING;
        }
        return true;
    }

    public boolean canCastSkill(int casterId, int targetId, boolean isHelpful) {
        if (state != EventState.RUNNING) {
            return true;
        }
        Participant caster = participants.get(casterId);
        Participant target = participants.get(targetId);
        if (caster == null && target == null) {
            return true;
        }
        if (caster == null || target == null) {
            return Config.TVT_ALLOW_INTERFERENCE;
        }
        if (caster.getTeam() == target.getTeam()) {
            return isHelpful || Config.TVT_ALLOW_TEAM_CASTING;
        } else {
            return !isHelpful || Config.TVT_ALLOW_ENEMY_HEALING;
        }
    }

    public boolean canUsePotions(int objectId) {
        if (state == EventState.RUNNING && participants.containsKey(objectId)) {
            return Config.TVT_ALLOW_POTIONS;
        }
        return true;
    }

    public boolean canSummon(int objectId) {
        if (state == EventState.RUNNING && participants.containsKey(objectId)) {
            return Config.TVT_ALLOW_SUMMON;
        }
        return true;
    }

    public boolean onKill(int killerId, int victimId) {
        if (state != EventState.RUNNING) {
            return false;
        }
        Participant killer = participants.get(killerId);
        Participant victim = participants.get(victimId);
        if (killer == null || victim == null) {
            return false;
        }
        // Friendly fire protection unless TVT_ALLOW_TEAM_ATTACKING is enabled
        if (killer.getTeam() == victim.getTeam() && !Config.TVT_ALLOW_TEAM_ATTACKING) {
            return false;
        }

        killer.addKill();
        teamScores.merge(killer.getTeam(), 1, Integer::sum);

        PlayerCharacter vChar = victim.getPlayer();
        if (vChar != null && victim.getTeam() != null) {
            if (Config.TVT_REVIVE_RECOVERY) {
                vChar.currentHp(vChar.maxHp());
                vChar.currentMp(vChar.maxMp());
                vChar.currentCp(vChar.maxCp());
            } else {
                vChar.currentHp(Math.max(1.0, vChar.maxHp() * 0.7));
            }
            var world = com.lopez.l2j.game.world.GameWorld.getInstance();
            if (world != null) {
                world.sendPacket(vChar.objectId(), new com.lopez.l2j.network.game.packet.GameServerPacket.Revive(vChar.objectId()));
                world.teleportPlayer(vChar, victim.getTeam().getSpawnX(), victim.getTeam().getSpawnY(), victim.getTeam().getSpawnZ(), TVT_INSTANCE_ID);
            }
        }
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
        for (Participant p : participants.values()) {
            PlayerCharacter pc = p.getPlayer();
            if (pc == null) continue;
            pc.team(0);
            pc.currentHp(pc.maxHp());
            pc.currentMp(pc.maxMp());
            pc.currentCp(pc.maxCp());
            boolean eligibleForReward = Config.TVT_PRICE_NO_KILLS || p.getKills() > 0;
            boolean isWinner = (winner != null && p.getTeam() == winner);
            boolean isTieRewarded = (winner == null && blueScore > 0 && Config.TVT_REWARD_TEAM_TIE);
            if ((isWinner || isTieRewarded) && eligibleForReward && rewardItemCount > 0 && pc.inventory() != null) {
                int countToGrant = isTieRewarded ? Math.max(1, rewardItemCount / 2) : rewardItemCount;
                if (inventoryService != null) {
                    var added = inventoryService.addItem(pc.inventory(), rewardItemId, countToGrant, "TvTReward");
                    if (added != null && added.item() != null && world != null) {
                        world.sendPacket(pc.objectId(), new com.lopez.l2j.network.game.packet.GameServerPacket.InventoryUpdate(List.of(
                                com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo.of(added.item(),
                                        added.created() ? com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo.ADDED : com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo.MODIFIED))));
                    }
                } else {
                    pc.inventory().addItem(rewardItemId, countToGrant);
                }
            }
            int retX = (Config.TVT_ORIGINAL_POSITION || p.getOrigX() != 0 || p.getOrigY() != 0) ? p.getOrigX() : 83400;
            int retY = (Config.TVT_ORIGINAL_POSITION || p.getOrigX() != 0 || p.getOrigY() != 0) ? p.getOrigY() : 147943;
            int retZ = (Config.TVT_ORIGINAL_POSITION || p.getOrigX() != 0 || p.getOrigY() != 0) ? p.getOrigZ() : -3404;
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
        announce("TvT encerrado! Azul " + blueScore + " x " + redScore + " Vermelho. Vencedor: " + (winner != null ? winner.getTeamName() : "Empate"));
        log.info("TvT: Encerrado! Azul: {}, Vermelho: {}. Vencedor: {}", blueScore, redScore, winner);
        return winner;
    }

    public String buildStatusHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><center>");
        sb.append("<font color=\"LEVEL\">=== Team vs Team (TvT) ===</font><br><br>");
        sb.append("Status: ").append(state.name()).append("<br>");
        sb.append("Niveis: ").append(minLevel).append(" - ").append(maxLevel).append("<br>");
        sb.append("Inscritos: ").append(participants.size()).append(" / ").append(Config.TVT_MAX_PLAYERS).append("<br>");
        if (state == EventState.RUNNING) {
            sb.append("<br><font color=\"8FA8BE\">Time Azul: </font>").append(teamScores.getOrDefault(TeamType.BLUE, 0)).append(" kills<br>");
            sb.append("<font color=\"B86B6B\">Time Vermelho: </font>").append(teamScores.getOrDefault(TeamType.RED, 0)).append(" kills<br>");
        }
        sb.append("<br>");
        if (state == EventState.REGISTRATION) {
            sb.append("<button value=\"Participar\" action=\"bypass -h voiced_tvtjoin\" width=95 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\"><br>");
            sb.append("<button value=\"Cancelar\" action=\"bypass -h voiced_tvtleave\" width=95 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\">");
        }
        sb.append("</center></body></html>");
        return sb.toString();
    }

    // Getters and setters
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
    public int getBlueKills() { return getTeamScore(TeamType.BLUE); }
    public int getRedKills() { return getTeamScore(TeamType.RED); }
    public int getMinLevel() { return minLevel; }
    public int getMaxLevel() { return maxLevel; }
    public int getMinPlayers() { return minPlayers; }
    public int getMaxPlayers() { return maxPlayers; }
    public int getJoinTimeMinutes() { return Config.TVT_JOIN_TIME; }
    public int getEventTimeMinutes() { return Config.TVT_EVENT_TIME; }
    public int getReviveDelaySeconds() { return Config.TVT_REVIVE_DELAY; }
    public boolean isColiseumDoorsClosed() { return coliseumDoorsClosed; }
    public int getRewardItemId() { return rewardItemId; }
    public void setRewardItemId(int rewardItemId) { this.rewardItemId = rewardItemId; }
    public int getRewardItemCount() { return rewardItemCount; }
    public void setRewardItemCount(int rewardItemCount) { this.rewardItemCount = rewardItemCount; }
}
