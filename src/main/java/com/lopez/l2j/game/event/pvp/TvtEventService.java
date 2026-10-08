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
        IS_AIO_BLOCKED
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
        public int getSpawnX() { return spawnX; }
        public int getSpawnY() { return spawnY; }
        public int getSpawnZ() { return spawnZ; }
    }

    public static class Participant {
        private final int objectId;
        private final String name;
        private final int level;
        private TeamType team;
        private int kills;
        private final int origX;
        private final int origY;
        private final int origZ;

        public Participant(PlayerCharacter player, int origX, int origY, int origZ) {
            this.objectId = player.getObjectId();
            this.name = player.getName();
            this.level = player.getLevel();
            this.origX = origX;
            this.origY = origY;
            this.origZ = origZ;
            this.kills = 0;
        }

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

    private volatile EventState state = EventState.INACTIVE;
    private int minLevel = Config.TVT_MIN_LEVEL;
    private int maxLevel = Config.TVT_MAX_LEVEL;
    private int rewardItemId = Config.TVT_REWARD_ID;
    private int rewardItemCount = Config.TVT_REWARD_AMOUNT;

    private final Map<Integer, Participant> participants = new ConcurrentHashMap<>();
    private final Map<TeamType, Integer> teamScores = new ConcurrentHashMap<>();

    public TvtEventService() {
        teamScores.put(TeamType.BLUE, 0);
        teamScores.put(TeamType.RED, 0);
    }

    public synchronized void openRegistration() {
        openRegistration(Config.TVT_MIN_LEVEL, Config.TVT_MAX_LEVEL);
    }

    public synchronized void openRegistration(int minLvl, int maxLvl) {
        this.minLevel = minLvl;
        this.maxLevel = maxLvl;
        this.participants.clear();
        this.teamScores.put(TeamType.BLUE, 0);
        this.teamScores.put(TeamType.RED, 0);
        this.state = EventState.REGISTRATION;
        log.info("TvT: Registro aberto para niveis {} a {}.", minLevel, maxLevel);
    }

    public RegisterResult register(PlayerCharacter player) {
        if (!Config.TVT_ENABLED || state != EventState.REGISTRATION) {
            return RegisterResult.NOT_IN_REGISTRATION;
        }
        if (participants.containsKey(player.getObjectId())) {
            return RegisterResult.ALREADY_REGISTERED;
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
            log.warn("TvT: Participantes insuficientes ({}). Cancelando evento.", participants.size());
            state = EventState.INACTIVE;
            participants.clear();
            return false;
        }

        // Divide and balance teams by level
        List<Participant> list = new ArrayList<>(participants.values());
        list.sort(Comparator.comparingInt(Participant::getLevel).reversed());

        int blueCount = 0;
        int redCount = 0;
        for (Participant p : list) {
            if (blueCount <= redCount) {
                p.setTeam(TeamType.BLUE);
                blueCount++;
            } else {
                p.setTeam(TeamType.RED);
                redCount++;
            }
        }

        teamScores.put(TeamType.BLUE, 0);
        teamScores.put(TeamType.RED, 0);
        state = EventState.RUNNING;
        log.info("TvT: Batalha iniciada! Azul: {} players, Vermelho: {} players.", blueCount, redCount);
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
        // Friendly fire protection
        if (killer.getTeam() == victim.getTeam()) {
            return false;
        }

        killer.addKill();
        teamScores.merge(killer.getTeam(), 1, Integer::sum);
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

        state = EventState.INACTIVE;
        log.info("TvT: Encerrado! Azul: {}, Vermelho: {}. Vencedor: {}", blueScore, redScore, winner);
        return winner;
    }

    public String buildStatusHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><center>");
        sb.append("<font color=\"LEVEL\">=== Team vs Team (TvT) ===</font><br><br>");
        sb.append("Status: ").append(state.name()).append("<br>");
        sb.append("Niveis: ").append(minLevel).append(" - ").append(maxLevel).append("<br>");
        sb.append("Inscritos: ").append(participants.size()).append("<br>");
        if (state == EventState.RUNNING) {
            sb.append("<br><font color=\"0000FF\">Time Azul: </font>").append(teamScores.getOrDefault(TeamType.BLUE, 0)).append(" kills<br>");
            sb.append("<font color=\"FF0000\">Time Vermelho: </font>").append(teamScores.getOrDefault(TeamType.RED, 0)).append(" kills<br>");
        }
        sb.append("<br>");
        if (state == EventState.REGISTRATION) {
            sb.append("<button value=\"Participar\" action=\"bypass -h voiced_tvtjoin\" width=90 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"><br>");
            sb.append("<button value=\"Cancelar\" action=\"bypass -h voiced_tvtleave\" width=90 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">");
        }
        sb.append("</center></body></html>");
        return sb.toString();
    }

    // Getters and setters
    public EventState getState() { return state; }
    public boolean isParticipant(int objectId) { return participants.containsKey(objectId); }
    public Participant getParticipant(int objectId) { return participants.get(objectId); }
    public Map<Integer, Participant> getParticipants() { return Collections.unmodifiableMap(participants); }
    public int getTeamScore(TeamType team) { return teamScores.getOrDefault(team, 0); }
    public int getMinLevel() { return minLevel; }
    public int getMaxLevel() { return maxLevel; }
    public int getRewardItemId() { return rewardItemId; }
    public void setRewardItemId(int rewardItemId) { this.rewardItemId = rewardItemId; }
    public int getRewardItemCount() { return rewardItemCount; }
    public void setRewardItemCount(int rewardItemCount) { this.rewardItemCount = rewardItemCount; }
}
