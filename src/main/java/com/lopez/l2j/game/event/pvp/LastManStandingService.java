package com.lopez.l2j.game.event.pvp;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.game.world.GameWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

/**
 * Evento Last Man Standing (Battle Royale / Sobrevivencia de 1 Vida) - #483.
 * Verifica periodicamente o raio de fronteira da arena e finaliza imediatamente
 * quando restar apenas 1 sobrevivente vivo na instancia.
 */
@Service
public class LastManStandingService {

    private static final Logger log = LoggerFactory.getLogger(LastManStandingService.class);

    public static final int LMS_INSTANCE_ID = 9104;
    public static final int REWARD_ADENA_ID = 57;
    public static final int REWARD_ADENA_AMOUNT = 5_000_000;
    public static final int ARENA_CENTER_X = 148457;
    public static final int ARENA_CENTER_Y = 46700;
    public static final int ARENA_CENTER_Z = -3413;
    public static final double ARENA_MAX_RADIUS = 2500.0;

    public enum EventState {
        INACTIVE,
        REGISTRATION,
        RUNNING
    }

    public record LmsParticipant(PlayerCharacter player, int origX, int origY, int origZ) {}

    private final InventoryService inventoryService;
    private final Map<Integer, LmsParticipant> participants = new ConcurrentHashMap<>();
    private final Set<Integer> aliveIds = ConcurrentHashMap.newKeySet();
    private volatile EventState state = EventState.INACTIVE;
    private ScheduledExecutorService boundaryScheduler;

    public LastManStandingService() {
        this(null);
    }

    @Autowired
    public LastManStandingService(@Autowired(required = false) InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    public synchronized void openRegistration() {
        participants.clear();
        aliveIds.clear();
        state = EventState.REGISTRATION;
        log.info("LMS: Registro aberto para Last Man Standing!");
    }

    public synchronized boolean register(PlayerCharacter player) {
        if (state != EventState.REGISTRATION || player == null || player.isDead() || player.karma() > 0) {
            return false;
        }
        if (participants.containsKey(player.objectId())) {
            return false;
        }
        participants.put(player.objectId(), new LmsParticipant(player, player.x(), player.y(), player.z()));
        return true;
    }

    public synchronized boolean startFight() {
        if (state != EventState.REGISTRATION || participants.size() < 2) {
            state = EventState.INACTIVE;
            participants.clear();
            return false;
        }
        aliveIds.clear();
        GameWorld world = GameWorld.getInstance();
        int i = 0;
        for (LmsParticipant lp : participants.values()) {
            PlayerCharacter pc = lp.player();
            if (pc == null) continue;
            aliveIds.add(pc.objectId());
            if (pc.getParty() != null) {
                pc.getParty().removePartyMember(pc.objectId());
                pc.setParty(null);
            }
            pc.currentHp(pc.maxHp());
            pc.currentMp(pc.maxMp());
            pc.currentCp(pc.maxCp());
            double angle = (2 * Math.PI * i++) / Math.max(1, participants.size());
            int sx = ARENA_CENTER_X + (int) (Math.cos(angle) * 400);
            int sy = ARENA_CENTER_Y + (int) (Math.sin(angle) * 400);
            if (world != null) {
                world.teleportPlayer(pc, sx, sy, ARENA_CENTER_Z, LMS_INSTANCE_ID);
            } else {
                pc.teleport(sx, sy, ARENA_CENTER_Z);
                pc.instanceId(LMS_INSTANCE_ID);
            }
        }
        state = EventState.RUNNING;
        startBoundaryTask();
        return true;
    }

    private void startBoundaryTask() {
        stopBoundaryTask();
        boundaryScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "LMS-BoundaryTask");
            t.setDaemon(true);
            return t;
        });
        boundaryScheduler.scheduleAtFixedRate(this::checkArenaBoundaries, 2, 2, TimeUnit.SECONDS);
    }

    private void stopBoundaryTask() {
        if (boundaryScheduler != null) {
            boundaryScheduler.shutdownNow();
            boundaryScheduler = null;
        }
    }

    public synchronized void checkArenaBoundaries() {
        if (state != EventState.RUNNING) {
            return;
        }
        for (Integer objId : new ArrayList<>(aliveIds)) {
            LmsParticipant lp = participants.get(objId);
            if (lp == null || lp.player() == null || lp.player().isDead()) {
                eliminatePlayer(objId);
                continue;
            }
            PlayerCharacter pc = lp.player();
            double dist = Math.hypot(pc.x() - ARENA_CENTER_X, pc.y() - ARENA_CENTER_Y);
            if (dist > ARENA_MAX_RADIUS || (pc.instanceId() != 0 && pc.instanceId() != LMS_INSTANCE_ID)) {
                log.info("LMS: Jogador {} desclassificado por sair da fronteira da arena.", pc.name());
                eliminatePlayer(objId);
            }
        }
    }

    public synchronized void onPlayerDeath(int victimId) {
        if (state != EventState.RUNNING) {
            return;
        }
        eliminatePlayer(victimId);
    }

    public synchronized void onDisconnect(int playerId) {
        if (state == EventState.REGISTRATION) {
            participants.remove(playerId);
        } else if (state == EventState.RUNNING) {
            eliminatePlayer(playerId);
        }
    }

    private void eliminatePlayer(int playerId) {
        if (!aliveIds.remove(playerId)) {
            return;
        }
        LmsParticipant lp = participants.get(playerId);
        if (lp != null && lp.player() != null) {
            returnPlayer(lp);
        }
        if (aliveIds.size() <= 1 && state == EventState.RUNNING) {
            finishEvent();
        }
    }

    public synchronized PlayerCharacter finishEvent() {
        if (state != EventState.RUNNING) {
            return null;
        }
        stopBoundaryTask();
        PlayerCharacter winner = null;
        if (aliveIds.size() == 1) {
            int winnerId = aliveIds.iterator().next();
            LmsParticipant lp = participants.get(winnerId);
            if (lp != null && lp.player() != null) {
                winner = lp.player();
                if (winner.inventory() != null) {
                    if (inventoryService != null) {
                        inventoryService.addItem(winner.inventory(), REWARD_ADENA_ID, REWARD_ADENA_AMOUNT, "LmsReward");
                    } else {
                        winner.inventory().addItem(REWARD_ADENA_ID, REWARD_ADENA_AMOUNT);
                    }
                }
            }
        }
        for (LmsParticipant lp : participants.values()) {
            returnPlayer(lp);
        }
        aliveIds.clear();
        participants.clear();
        state = EventState.INACTIVE;
        return winner;
    }

    private void returnPlayer(LmsParticipant lp) {
        PlayerCharacter pc = lp.player();
        if (pc == null) return;
        pc.currentHp(pc.maxHp());
        pc.currentMp(pc.maxMp());
        pc.currentCp(pc.maxCp());
        GameWorld world = GameWorld.getInstance();
        if (world != null) {
            world.teleportPlayer(pc, lp.origX(), lp.origY(), lp.origZ(), 0);
        } else {
            pc.teleport(lp.origX(), lp.origY(), lp.origZ());
            pc.instanceId(0);
        }
    }

    public EventState getState() { return state; }
    public int getAliveCount() { return aliveIds.size(); }
}
