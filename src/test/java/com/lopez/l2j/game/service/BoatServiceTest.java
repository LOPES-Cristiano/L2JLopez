package com.lopez.l2j.game.service;

import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoatServiceTest {

    private BoatService boatService;

    @BeforeEach
    void setUp() {
        boatService = new BoatService();
    }

    private PlayerCharacter createPlayer(int objectId, String name) {
        return new PlayerCharacter(objectId, "acc1", name, 40, 0, 0, 0, 1, 1, false,
                0, 0, 0, 1000, 500, 500, 0, 0, 0, 0, "Title", 0, 0, 0,
                -96883, 262135, -3600, 0, 1000.0, 500.0, 500.0);
    }

    @Test
    void testBoatBoardingAndWaypoints() {
        PlayerCharacter passenger = createPlayer(1, "Sailor");

        // Embarca no barco Talking Island - Gludin (Boat ID 1)
        assertTrue(boatService.boardBoat(passenger, 1));
        var boat = boatService.getBoat(1).orElseThrow();
        assertTrue(boat.getOnboardPlayers().contains(1));

        // Avanco de waypoints
        boatService.advanceWaypoint(1);
        assertEquals(BoatService.BoatState.SAILING, boat.getState());
        assertEquals(1, boat.getCurrentWaypointIndex());

        // Desembarque
        assertTrue(boatService.disembarkBoat(passenger, 1));
        assertFalse(boat.getOnboardPlayers().contains(1));
    }

    @Test
    void testBoardAndTravel() {
        PlayerCharacter passenger = createPlayer(2, "Traveler");
        com.lopez.l2j.game.item.Inventory inv = new com.lopez.l2j.game.item.Inventory(2);
        inv.addAdena(5000);
        passenger.inventory(inv);

        assertTrue(boatService.boardAndTravel(passenger, 1, 1074, 1000));
        assertEquals(4000L, passenger.inventory().adena());
        assertEquals(-95500, passenger.x());
        assertEquals(250000, passenger.y());
        assertEquals(-3600, passenger.z());
    }
}

