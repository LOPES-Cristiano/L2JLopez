package com.lopez.l2j.game.instance.dimensionalrift;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DimensionalRiftServiceTest {

    private DimensionalRiftService riftService;

    private PlayerCharacter dummyPlayer(int id, String name, int level) {
        PlayerCharacter player = new PlayerCharacter(id, "acc", name, level, 0L, 0, 0, 0, 0, false,
                0, 0, 0, 100, 100, 100, 0, 0, 0, 0, "", 0, 0L, 0L, 0, 0, 0, 0, 0.0, 0.0, 0.0);
        player.inventory(new Inventory(id));
        return player;
    }

    private ItemTemplate fragmentTemplate() {
        return ItemTemplate.etc(DimensionalRiftService.DIMENSIONAL_FRAGMENT_ID,
                DimensionalRiftService.DIMENSIONAL_FRAGMENT_ID,
                "Dimensional Fragment", "quest", "none", 1, "none", 0, false, false, false, false);
    }

    @BeforeEach
    void setUp() {
        riftService = new DimensionalRiftService(null, ObjectIdFactory.sequential(1000));
    }

    @Test
    void testEnterRiftSuccessAndFragmentConsumption() {
        PlayerCharacter leader = dummyPlayer(1, "RiftLeader", 76);
        PlayerCharacter member = dummyPlayer(2, "RiftMember", 75);
        List<PlayerCharacter> party = List.of(leader, member);

        // Tier HERO: minLevel 70, cost 33 fragments
        leader.inventory().add(new ItemInstance(101, fragmentTemplate(), leader.objectId(), 50));
        member.inventory().add(new ItemInstance(102, fragmentTemplate(), member.objectId(), 50));

        boolean entered = riftService.enterRift(leader, RiftTier.HERO, party);
        assertTrue(entered);

        // Assert fragments consumed: 50 - 33 = 17 remaining
        assertEquals(17, leader.inventory().byItemId(DimensionalRiftService.DIMENSIONAL_FRAGMENT_ID).orElseThrow().count());
        assertEquals(17, member.inventory().byItemId(DimensionalRiftService.DIMENSIONAL_FRAGMENT_ID).orElseThrow().count());

        assertTrue(riftService.isInRift(1));
        assertTrue(riftService.isInRift(2));

        RiftSession session = riftService.getSessionByLeader(1);
        assertNotNull(session);
        assertEquals(RiftTier.HERO, session.getTier());
        assertEquals(1, session.getJumpsCount());
        assertNotEquals(0, leader.x());
    }

    @Test
    void testEnterRiftFailsWhenNotEnoughFragments() {
        PlayerCharacter leader = dummyPlayer(1, "RiftLeader", 76);
        PlayerCharacter member = dummyPlayer(2, "RiftMember", 75);
        List<PlayerCharacter> party = List.of(leader, member);

        // Member has only 10 fragments (needs 33)
        leader.inventory().add(new ItemInstance(101, fragmentTemplate(), leader.objectId(), 50));
        member.inventory().add(new ItemInstance(102, fragmentTemplate(), member.objectId(), 10));

        boolean entered = riftService.enterRift(leader, RiftTier.HERO, party);
        assertFalse(entered);
        assertFalse(riftService.isInRift(1));
    }

    @Test
    void testJumpProgressionAndBossDefeat() {
        PlayerCharacter leader = dummyPlayer(1, "RiftLeader", 76);
        PlayerCharacter member = dummyPlayer(2, "RiftMember", 75);
        List<PlayerCharacter> party = List.of(leader, member);

        leader.inventory().add(new ItemInstance(101, fragmentTemplate(), leader.objectId(), 100));
        member.inventory().add(new ItemInstance(102, fragmentTemplate(), member.objectId(), 100));

        riftService.enterRift(leader, RiftTier.HERO, party);

        // Jump 2
        assertTrue(riftService.jumpToNextRoom(leader.objectId(), party));
        // Jump 3
        assertTrue(riftService.jumpToNextRoom(leader.objectId(), party));
        // Jump 4 (Boss room)
        assertTrue(riftService.jumpToNextRoom(leader.objectId(), party));

        RiftSession session = riftService.getSessionByLeader(1);
        assertNotNull(session);
        assertTrue(session.isBossRoom());
        assertEquals(9, session.getCurrentRoom());

        // Boss Anakazel defeat (bossId 25338 for HERO)
        boolean bossKill = riftService.onBossDefeated(leader.objectId(), 25338, party);
        assertTrue(bossKill);

        // Verify Ancient Adena rewarded: tier HERO = 6 * 25,000 = 150,000
        assertEquals(150000, leader.inventory().byItemId(DimensionalRiftService.ANCIENT_ADENA_ID).orElseThrow().count());
        assertEquals(150000, member.inventory().byItemId(DimensionalRiftService.ANCIENT_ADENA_ID).orElseThrow().count());

        // Next jump beyond max jumps exits to waiting room
        riftService.jumpToNextRoom(leader.objectId(), party);
        assertFalse(riftService.isInRift(1));
        assertEquals(DimensionalRiftService.WAITING_ROOM_X, leader.x());
        assertEquals(DimensionalRiftService.WAITING_ROOM_Y, leader.y());
        assertEquals(DimensionalRiftService.WAITING_ROOM_Z, leader.z());
    }

    @Test
    void testManualJumpCanOnlyBeUsedOnce() {
        PlayerCharacter leader = dummyPlayer(1, "RiftLeader", 45);
        PlayerCharacter member = dummyPlayer(2, "RiftMember", 45);
        List<PlayerCharacter> party = List.of(leader, member);

        leader.inventory().add(new ItemInstance(101, fragmentTemplate(), leader.objectId(), 50));
        member.inventory().add(new ItemInstance(102, fragmentTemplate(), member.objectId(), 50));

        riftService.enterRift(leader, RiftTier.OFFICER, party);

        // First manual jump succeeds
        boolean manual1 = riftService.manualJump(leader, party);
        assertTrue(manual1);

        // Second manual jump fails
        boolean manual2 = riftService.manualJump(leader, party);
        assertFalse(manual2);
    }
}
