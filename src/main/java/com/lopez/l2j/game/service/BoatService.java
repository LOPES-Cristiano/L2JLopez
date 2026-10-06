package com.lopez.l2j.game.service;

import com.lopez.l2j.game.model.PlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Sistema de Veículos e Barcos Retail (BoatManager / BoatService).
 * Gerencia rotas marítimas (Talking Island <-> Gludin, Giran, Primeval Isle e Rune),
 * validação de bilhetes de passagem, embarque/desembarque de passageiros e navegação por waypoints.
 */
@Service
public class BoatService {

    private static final Logger log = LoggerFactory.getLogger(BoatService.class);

    public record BoatWaypoint(int x, int y, int z, int speed) {}

    public enum BoatState {
        DOCKED,
        PREPARING_TO_DEPART,
        SAILING,
        ARRIVED
    }

    public static class BoatRoute {
        private final int boatId;
        private final String name;
        private final int requiredTicketId;
        private final List<BoatWaypoint> waypoints;
        private BoatState state = BoatState.DOCKED;
        private int currentWaypointIndex = 0;
        private final Set<Integer> onboardPlayers = new CopyOnWriteArraySet<>();

        public BoatRoute(int boatId, String name, int requiredTicketId, List<BoatWaypoint> waypoints) {
            this.boatId = boatId;
            this.name = name;
            this.requiredTicketId = requiredTicketId;
            this.waypoints = waypoints;
        }

        public int getBoatId() { return boatId; }
        public String getName() { return name; }
        public int getRequiredTicketId() { return requiredTicketId; }
        public List<BoatWaypoint> getWaypoints() { return waypoints; }
        public BoatState getState() { return state; }
        public void setState(BoatState state) { this.state = state; }
        public int getCurrentWaypointIndex() { return currentWaypointIndex; }
        public void setCurrentWaypointIndex(int idx) { this.currentWaypointIndex = idx; }
        public Set<Integer> getOnboardPlayers() { return onboardPlayers; }

        public BoatWaypoint getCurrentWaypoint() {
            if (waypoints.isEmpty()) return new BoatWaypoint(0, 0, 0, 0);
            return waypoints.get(Math.min(currentWaypointIndex, waypoints.size() - 1));
        }
    }

    private final Map<Integer, BoatRoute> boats = new ConcurrentHashMap<>();

    public BoatService() {
        initDefaultRoutes();
    }

    private void initDefaultRoutes() {
        // Barco 1: Talking Island <-> Gludin Harbor (Ticket 1074)
        boats.put(1, new BoatRoute(1, "Talking Island - Gludin", 1074, Arrays.asList(
                new BoatWaypoint(-96883, 262135, -3600, 300),
                new BoatWaypoint(-95500, 250000, -3600, 400),
                new BoatWaypoint(-90000, 200000, -3600, 450),
                new BoatWaypoint(-90015, 150000, -3600, 300)
        )));

        // Barco 2: Gludin <-> Talking Island (Ticket 1075)
        boats.put(2, new BoatRoute(2, "Gludin - Talking Island", 1075, Arrays.asList(
                new BoatWaypoint(-90015, 150000, -3600, 300),
                new BoatWaypoint(-90000, 200000, -3600, 450),
                new BoatWaypoint(-95500, 250000, -3600, 400),
                new BoatWaypoint(-96883, 262135, -3600, 300)
        )));

        // Barco 3: Rune <-> Primeval Isle (Ticket 8925)
        boats.put(3, new BoatRoute(3, "Rune - Primeval Isle", 8925, Arrays.asList(
                new BoatWaypoint(34513, -38009, -3610, 350),
                new BoatWaypoint(20000, -30000, -3610, 450),
                new BoatWaypoint(10447, -24982, -3610, 350)
        )));
    }

    public boolean boardBoat(PlayerCharacter player, int boatId) {
        BoatRoute boat = boats.get(boatId);
        if (boat == null || (boat.getState() != BoatState.DOCKED && boat.getState() != BoatState.PREPARING_TO_DEPART)) {
            return false;
        }

        boat.getOnboardPlayers().add(player.getObjectId());
        BoatWaypoint wp = boat.getCurrentWaypoint();
        player.setX(wp.x());
        player.setY(wp.y());
        player.setZ(wp.z());
        log.info("BoatService: Jogador {} embarcou no barco #{} ({})", player.getName(), boatId, boat.getName());
        return true;
    }

    public boolean disembarkBoat(PlayerCharacter player, int boatId) {
        BoatRoute boat = boats.get(boatId);
        if (boat == null) {
            return false;
        }
        boolean removed = boat.getOnboardPlayers().remove(player.getObjectId());
        if (removed) {
            log.info("BoatService: Jogador {} desembarcou do barco #{}", player.getName(), boatId);
        }
        return removed;
    }

    public void advanceWaypoint(int boatId) {
        BoatRoute boat = boats.get(boatId);
        if (boat == null) return;

        int nextIdx = boat.getCurrentWaypointIndex() + 1;
        if (nextIdx < boat.getWaypoints().size()) {
            boat.setCurrentWaypointIndex(nextIdx);
            boat.setState(BoatState.SAILING);
        } else {
            boat.setState(BoatState.ARRIVED);
        }
    }

    public Optional<BoatRoute> getBoat(int boatId) { return Optional.ofNullable(boats.get(boatId)); }
    public Collection<BoatRoute> getAllBoats() { return Collections.unmodifiableCollection(boats.values()); }
}
