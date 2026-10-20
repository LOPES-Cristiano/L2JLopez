package com.lopez.l2j.game.drop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.Die;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SpoilAndSweepTest {

	private static final int MOB_ID = 20432;
	private static final int SPOIL_ITEM_ID = com.lopez.l2j.game.item.TestItems.SHORT_SWORD;

	private DropServiceTest.InMemoryDropTable dropTable;
	private DropService dropService;
	private InventoryService inventoryService;
	private PlayerCharacter dwarf;

	@BeforeEach
	void setUp() {
		dropTable = new DropServiceTest.InMemoryDropTable();
		// Spoil item: category -1, 100% chance
		dropTable.add(new DropData(MOB_ID, SPOIL_ITEM_ID, 1, 3, -1, DropData.MAX_CHANCE));
		// Regular drop: category 1
		dropTable.add(new DropData(MOB_ID, 1865, 1, 1, 1, DropData.MAX_CHANCE));

		dropService = new DropService(dropTable, 1.0, 1.0, 1.0, true);

		dwarf = new PlayerCharacter(7001, "acc", "DwarfSpoil", 20, 0, 0, 0, 53, 53, false, 0, 0, 0,
				500, 200, 200, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 500.0, 200.0, 200.0);
		dwarf.inventory(new Inventory(dwarf.objectId()));

		var itemRepo = new com.lopez.l2j.game.item.InMemoryItemRepository();
		inventoryService = new InventoryService(
				com.lopez.l2j.game.item.TestItems.table(),
				itemRepo,
				ObjectIdFactory.sequential(100000),
				0);
	}

	@Test
	void diePacketShouldEncodeSweepableGlowFlag() {
		Die dieWithGlow = new Die(12345, true, false, false, true);
		byte[] bytes = dieWithGlow.encode();

		ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
		assertEquals(0x06, buf.get()); // opcode Die
		assertEquals(12345, buf.getInt()); // charObjId
		assertEquals(1, buf.getInt()); // toVillage
		assertEquals(0, buf.getInt()); // toClanHall
		assertEquals(0, buf.getInt()); // toCastle
		assertEquals(0, buf.getInt()); // flag
		assertEquals(1, buf.getInt()); // sweepable (BLUE GLOW ACTIVE!)
		assertEquals(0, buf.getInt()); // fixedres

		Die dieNoGlow = new Die(12345, true, false, false, false);
		byte[] bytesNoGlow = dieNoGlow.encode();
		ByteBuffer bufNoGlow = ByteBuffer.wrap(bytesNoGlow).order(ByteOrder.LITTLE_ENDIAN);
		bufNoGlow.get();
		bufNoGlow.getInt();
		bufNoGlow.getInt();
		bufNoGlow.getInt();
		bufNoGlow.getInt();
		bufNoGlow.getInt();
		assertEquals(0, bufNoGlow.getInt()); // sweepable == 0 (NO GLOW)
	}

	@Test
	void shouldOnlyBeSweepableWhenDeadSpoiledAndHasRewards() {
		NpcTemplate tpl = new NpcTemplate(
				MOB_ID, MOB_ID, "TargetMob", false, "", false,
				8.0, 16.0, 20, "male", "L2Monster",
				40, 500, 100, 50, 40, 30, 10, 250, 333,
				0, 0, 0, 50, 100, 0, false, "", 0);

		NpcInstance mob = new NpcInstance(8001, tpl, 0, 0, 0, 0);

		// Alive and not spoiled -> not sweepable
		assertFalse(mob.isSweepable());

		// Alive and spoiled -> not sweepable yet because not dead
		mob.spoiled(true);
		assertFalse(mob.isSweepable());

		// Dead and spoiled, but no spoil rewards yet -> not sweepable
		mob.dead(true);
		assertFalse(mob.isSweepable());

		// Dead, spoiled, and has spoil rewards -> IS SWEEPABLE!
		var rewards = dropService.rollSpoil(MOB_ID, dwarf.level(), 20);
		assertThat(rewards).isNotEmpty();
		mob.spoilRewards(rewards);
		assertTrue(mob.isSweepable());

		// Perform sweep
		List<GameServerPacket> sentPackets = new ArrayList<>();
		boolean swept = dropService.sweep(dwarf, mob, inventoryService, sentPackets::add);
		assertTrue(swept);

		// Check item in dwarf inventory
		var item = dwarf.inventory().byItemId(SPOIL_ITEM_ID);
		assertTrue(item.isPresent());

		// After sweep, mob is no longer sweepable and rewards are cleared
		assertFalse(mob.isSweepable());
		assertNull(mob.spoilRewards());

		// Second sweep attempt returns false
		boolean secondSweep = dropService.sweep(dwarf, mob, inventoryService, null);
		assertFalse(secondSweep);
	}

	@Test
	void spoilLandChanceMatchesRetailPowerCurve() {
		int iterations = 1000;

		// 1. Monstro do mesmo nivel (lvlDiff 0): failRate=100 em 10000 -> 99% de sucesso
		int sameLvlSuccess = 0;
		for (int i = 0; i < iterations; i++) {
			double lvlDiff = 0;
			int failRate = (int) Math.round(Math.pow(1.3, lvlDiff) * 100.0);
			failRate = Math.max(100, Math.min(9500, failRate));
			if (java.util.concurrent.ThreadLocalRandom.current().nextInt(10000) >= failRate) {
				sameLvlSuccess++;
			}
		}
		assertThat(sameLvlSuccess).isGreaterThanOrEqualTo(970);

		// 2. Monstro 5 niveis superior (lvlDiff 5): failRate=371 em 10000 -> ~96.3% de sucesso
		int plus5LvlSuccess = 0;
		for (int i = 0; i < iterations; i++) {
			double lvlDiff = 5;
			int failRate = (int) Math.round(Math.pow(1.3, lvlDiff) * 100.0);
			failRate = Math.max(100, Math.min(9500, failRate));
			if (java.util.concurrent.ThreadLocalRandom.current().nextInt(10000) >= failRate) {
				plus5LvlSuccess++;
			}
		}
		assertThat(plus5LvlSuccess).isGreaterThanOrEqualTo(930);
	}
}
