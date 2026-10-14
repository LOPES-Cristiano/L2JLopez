package com.lopez.l2j.game.event.partyfarm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PartyFarmEventServiceTest {

    private PartyFarmEventService partyFarmService;

    @BeforeEach
    void setUp() {
        partyFarmService = new PartyFarmEventService();
    }

    @Test
    void testScheduleCheckAndLifecycle() {
        assertTrue(partyFarmService.isEnabled());
        assertFalse(partyFarmService.getSpawnConfigs().isEmpty());

        // Segunda-feira (Day 1) as 15:00
        // 2026-10-05 e uma Segunda-feira (DayOfWeek = MONDAY)
        LocalDateTime monday1500 = LocalDateTime.of(2026, 10, 5, 15, 0);
        Optional<String> msg = partyFarmService.checkSchedule(monday1500);

        assertTrue(msg.isPresent());
        assertTrue(msg.get().contains("Party Farm"));
        assertEquals(PartyFarmEventService.EventStatus.PREPARATION, partyFarmService.getStatus());

        // Start event
        partyFarmService.startEvent();
        assertEquals(PartyFarmEventService.EventStatus.RUNNING, partyFarmService.getStatus());

        // End event
        partyFarmService.endEvent();
        assertEquals(PartyFarmEventService.EventStatus.INACTIVE, partyFarmService.getStatus());
    }

    @Test
    void testInactiveDayIgnored() {
        // Terca-feira (Day 2) as 15:00 -> Nao deve disparar pois dias ativos sao 1, 3, 5
        LocalDateTime tuesday1500 = LocalDateTime.of(2026, 10, 6, 15, 0);
        Optional<String> msg = partyFarmService.checkSchedule(tuesday1500);
        assertTrue(msg.isEmpty());
    }

    @Test
    void testRealWorldSpawningAndCleanup() {
        com.lopez.l2j.game.world.GameWorld world = new com.lopez.l2j.game.world.GameWorld();
        PartyFarmEventService svc = new PartyFarmEventService(world, null);

        assertEquals(0, world.totalNpcs());

        svc.startEvent();
        // Default has 5 + 3 = 8 monsters
        assertEquals(8, svc.getSpawnedMonsterObjectIds().size());
        assertEquals(8, world.totalNpcs());
        for (int objId : svc.getSpawnedMonsterObjectIds()) {
            assertTrue(world.npc(objId).isPresent());
            assertTrue(svc.isPartyFarmMonster(objId));
        }

        svc.endEvent();
        assertTrue(svc.getSpawnedMonsterObjectIds().isEmpty());
        assertEquals(0, world.totalNpcs());
    }
}
