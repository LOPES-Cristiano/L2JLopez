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
        IS_AIO_BLOCKED
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
        public int getBaseX() { return baseX; }
        public int getBaseY() { return baseY; }
        public int getBaseZ() { return baseZ; }
    }

    public static class Participant {
        private final int objectId;
        private final String name;
        private final int level;
        private TeamType team;
        private int flagCaptures;
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
            this.flagCaptures = 0;
        }

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

    private volatile EventState state = EventState.INACTIVE;
    private int minLevel = Config.CTF_MIN_LEVEL;
    private int maxLevel = Config.CTF_MAX_LEVEL;
    private int rewardItemId = Config.CTF_REWARD_ID;
    private int rewardItemCount = Config.CTF_REWARD_AMOUNT;

    private final Map<Integer, Participant> participants = new ConcurrentHashMap<>();
    private final Map<TeamType, Integer> teamScores = new ConcurrentHashMap<>();

    // Flag statuses and carriers (TeamType key is the flag belonging to that team)
    private final Map<TeamType, FlagStatus> flagStatuses = new ConcurrentHashMap<>();
    private final Map<TeamType, Integer> flagCarriers = new ConcurrentHashMap<>(); // flag team -> carrier player objectId

    public CtfEventService() {
        resetFlags();
        teamScores.put(TeamType.BLUE, 0);
        teamScores.put(TeamType.RED, 0);
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
        this.participants.clear();
        this.teamScores.put(TeamType.BLUE, 0);
        this.teamScores.put(TeamType.RED, 0);
        resetFlags();
        this.state = EventState.REGISTRATION;
        log.info("CTF: Registro aberto para niveis {} a {}.", minLevel, maxLevel);
    }

    public RegisterResult register(PlayerCharacter player) {
        if (!Config.CTF_ENABLED || state != EventState.REGISTRATION) {
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
            log.warn("CTF: Participantes insuficientes ({}). Cancelando.", participants.size());
            state = EventState.INACTIVE;
            participants.clear();
            return false;
        }

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
        resetFlags();
        state = EventState.RUNNING;
        log.info("CTF: Evento iniciado com {} jogadores!", list.size());
        return true;
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
        sb.append("Inscritos: ").append(participants.size()).append("<br>");
        if (state == EventState.RUNNING) {
            sb.append("<br><font color=\"0000FF\">Bandeiras Azul: </font>").append(teamScores.getOrDefault(TeamType.BLUE, 0)).append("<br>");
            sb.append("<font color=\"FF0000\">Bandeiras Vermelho: </font>").append(teamScores.getOrDefault(TeamType.RED, 0)).append("<br>");
            sb.append("Status Bandeira Azul: ").append(flagStatuses.get(TeamType.BLUE)).append("<br>");
            sb.append("Status Bandeira Vermelha: ").append(flagStatuses.get(TeamType.RED)).append("<br>");
        }
        sb.append("<br>");
        if (state == EventState.REGISTRATION) {
            sb.append("<button value=\"Participar\" action=\"bypass -h voiced_ctfjoin\" width=90 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"><br>");
            sb.append("<button value=\"Cancelar\" action=\"bypass -h voiced_ctfleave\" width=90 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">");
        }
        sb.append("</center></body></html>");
        return sb.toString();
    }

    // Getters
    public EventState getState() { return state; }
    public boolean isParticipant(int objectId) { return participants.containsKey(objectId); }
    public Participant getParticipant(int objectId) { return participants.get(objectId); }
    public Map<Integer, Participant> getParticipants() { return Collections.unmodifiableMap(participants); }
    public int getTeamScore(TeamType team) { return teamScores.getOrDefault(team, 0); }
    public FlagStatus getFlagStatus(TeamType team) { return flagStatuses.get(team); }
    public Integer getFlagCarrier(TeamType team) { return flagCarriers.get(team); }
    public int getRewardItemId() { return rewardItemId; }
    public void setRewardItemId(int rewardItemId) { this.rewardItemId = rewardItemId; }
    public int getRewardItemCount() { return rewardItemCount; }
    public void setRewardItemCount(int rewardItemCount) { this.rewardItemCount = rewardItemCount; }
    public int getMinLevel() { return minLevel; }
    public int getMaxLevel() { return maxLevel; }
}
