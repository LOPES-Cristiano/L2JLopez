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
        IS_AIO_BLOCKED
    }

    public static class Participant {
        private final int objectId;
        private final String name;
        private final int level;
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
        public int getKills() { return kills; }
        public void addKill() { this.kills++; }
        public int getOrigX() { return origX; }
        public int getOrigY() { return origY; }
        public int getOrigZ() { return origZ; }
    }

    private volatile EventState state = EventState.INACTIVE;
    private int minLevel = Config.DM_MIN_LEVEL;
    private int maxLevel = Config.DM_MAX_LEVEL;
    private int arenaX = 149457;
    private int arenaY = 46700;
    private int arenaZ = -3413;

    // Top 1, 2, 3 rewards
    private int reward1ItemId = Config.DM_REWARD_ID;
    private int reward1Count = Config.DM_REWARD_AMOUNT;
    private int reward2ItemId = Config.DM_REWARD_ID;
    private int reward2Count = Config.DM_REWARD_AMOUNT / 2;
    private int reward3ItemId = Config.DM_REWARD_ID;
    private int reward3Count = Config.DM_REWARD_AMOUNT / 4;

    private final Map<Integer, Participant> participants = new ConcurrentHashMap<>();

    public DmEventService() {}

    public synchronized void openRegistration() {
        openRegistration(Config.DM_MIN_LEVEL, Config.DM_MAX_LEVEL);
    }

    public synchronized void openRegistration(int minLvl, int maxLvl) {
        this.minLevel = minLvl;
        this.maxLevel = maxLvl;
        this.participants.clear();
        this.state = EventState.REGISTRATION;
        log.info("DM: Registro aberto para niveis {} a {}.", minLevel, maxLevel);
    }

    public RegisterResult register(PlayerCharacter player) {
        if (!Config.DM_ENABLED || state != EventState.REGISTRATION) {
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
            log.warn("DM: Participantes insuficientes ({}). Cancelando.", participants.size());
            state = EventState.INACTIVE;
            participants.clear();
            return false;
        }

        state = EventState.RUNNING;
        log.info("DM: Batalha iniciada com {} gladiadores no DeathMatch!", participants.size());
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
        sb.append("Participantes: ").append(participants.size()).append("<br><br>");

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
            sb.append("<button value=\"Participar\" action=\"bypass -h voiced_dmjoin\" width=90 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"><br>");
            sb.append("<button value=\"Cancelar\" action=\"bypass -h voiced_dmleave\" width=90 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">");
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
}
