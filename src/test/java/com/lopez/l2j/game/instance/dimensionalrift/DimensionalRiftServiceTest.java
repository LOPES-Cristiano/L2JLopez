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

    @Test
    void testRiftWithInventoryServiceAndGameWorldSpawnsAndTeleports() {
        com.lopez.l2j.game.item.InMemoryItemRepository repo = new com.lopez.l2j.game.item.InMemoryItemRepository();
        ItemTemplate fragTpl = ItemTemplate.etc(DimensionalRiftService.DIMENSIONAL_FRAGMENT_ID,
                DimensionalRiftService.DIMENSIONAL_FRAGMENT_ID,
                "Dimensional Fragment", "quest", "asset", 1, "none", 0, false, false, false, false);
        ItemTemplate aaTpl = ItemTemplate.etc(DimensionalRiftService.ANCIENT_ADENA_ID,
                DimensionalRiftService.ANCIENT_ADENA_ID, "Ancient Adena", "asset", "asset", 0, "none", 0, false, false, true, false);
        com.lopez.l2j.game.item.ItemTemplateTable itemTable = com.lopez.l2j.game.item.ItemTemplateTable.of(List.of(fragTpl, aaTpl), List.of());
        ObjectIdFactory ids = ObjectIdFactory.sequential(5000);
        com.lopez.l2j.game.service.InventoryService invService = new com.lopez.l2j.game.service.InventoryService(itemTable, repo, ids, 0);
        com.lopez.l2j.game.world.GameWorld world = new com.lopez.l2j.game.world.GameWorld();
        com.lopez.l2j.game.npc.NpcTemplateTable npcTemplates = com.lopez.l2j.game.npc.NpcTemplateTable.of(List.of());

        DimensionalRiftService fullRiftService = new DimensionalRiftService(itemTable, ids, invService, world, npcTemplates);

        PlayerCharacter leader = dummyPlayer(10, "RiftDbLeader", 76);
        PlayerCharacter member = dummyPlayer(20, "RiftDbMember", 75);
        List<PlayerCharacter> party = List.of(leader, member);

        invService.addItem(leader.inventory(), DimensionalRiftService.DIMENSIONAL_FRAGMENT_ID, 50, "Test");
        invService.addItem(member.inventory(), DimensionalRiftService.DIMENSIONAL_FRAGMENT_ID, 50, "Test");

        java.util.List<com.lopez.l2j.network.game.packet.GameServerPacket> leaderPackets = new java.util.ArrayList<>();
        world.add(new com.lopez.l2j.game.world.GameWorld.OnlinePlayer() {
            @Override public int objectId() { return leader.objectId(); }
            @Override public String name() { return leader.name(); }
            @Override public int x() { return leader.x(); }
            @Override public int y() { return leader.y(); }
            @Override public int z() { return leader.z(); }
            @Override public PlayerCharacter character() { return leader; }
            @Override public void send(com.lopez.l2j.network.game.packet.GameServerPacket packet) { leaderPackets.add(packet); }
        });

        assertTrue(fullRiftService.enterRift(leader, RiftTier.HERO, party));

        // Fragment consumption persisted in ItemRepository (50 - 33 = 17)
        assertEquals(17, repo.findInventory(leader.objectId()).get(0).count());
        assertEquals(17, repo.findInventory(member.objectId()).get(0).count());

        // Teleport packet sent to session
        assertTrue(leaderPackets.stream().anyMatch(p -> p instanceof com.lopez.l2j.network.game.packet.GameServerPacket.TeleportToLocation));

        // Monsters spawned in GameWorld for the room
        assertFalse(fullRiftService.getSpawnedMonsters(leader.objectId()).isEmpty());
        assertTrue(world.totalNpcs() > 0);

        // Leave rift cleans up monsters and teleports to waiting room
        fullRiftService.leaveRift(leader.objectId(), party);
        assertTrue(fullRiftService.getSpawnedMonsters(leader.objectId()).isEmpty());
        assertEquals(0, world.totalNpcs());
        assertEquals(DimensionalRiftService.WAITING_ROOM_X, leader.x());
    }
}
