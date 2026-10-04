package com.lopez.l2j.game.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameWorldSpatialGridTest {

	private GameWorld world;
	private NpcTemplate template;

	@BeforeEach
	void setUp() {
		world = new GameWorld();
		template = new NpcTemplate(
				10001, 10001, "Gremlin", false, "", false,
				8.0, 16.0, 1, "male", "L2Monster",
				40, 50, 20, 10, 10, 5, 5, 250, 333,
				0, 0, 0, 50, 100, 0, false);
	}

	@Test
	void shouldIndexNpcsInSpatialGrid() {
		NpcInstance nearNpc = new NpcInstance(0x10000001, template, 1000, 1000, -3000, 0);
		NpcInstance farNpc = new NpcInstance(0x10000002, template, 10000, 10000, -3000, 0);

		world.addNpc(nearNpc);
		world.addNpc(farNpc);

		assertThat(world.totalNpcs()).isEqualTo(2);
		assertThat(world.npc(0x10000001)).contains(nearNpc);
		assertThat(world.npc(0x10000002)).contains(farNpc);

		List<NpcInstance> around = world.findNpcsAround(1000, 1000, 3500);
		assertThat(around).containsExactly(nearNpc);

		world.removeNpc(nearNpc);
		assertThat(world.totalNpcs()).isEqualTo(1);
		assertThat(world.npc(0x10000001)).isEmpty();
		assertThat(world.findNpcsAround(1000, 1000, 3500)).isEmpty();
	}

	@Test
	void shouldFindPlayersAroundAndBroadcast() {
		List<GameServerPacket> received1 = new ArrayList<>();
		List<GameServerPacket> received2 = new ArrayList<>();

		GameWorld.OnlinePlayer p1 = new DummyPlayer(1, "PlayerOne", 0, 0, 0, received1);
		GameWorld.OnlinePlayer p2 = new DummyPlayer(2, "PlayerTwo", 500, 0, 0, received2);

		world.add(p1);
		world.add(p2);

		assertThat(world.online()).isEqualTo(2);
		assertThat(world.findPlayersAround(0, 0, 1000)).containsExactlyInAnyOrder(p1, p2);

		GameServerPacket dummyPacket = new GameServerPacket.ActionFailed();
		world.broadcastAround(p1, 1000, dummyPacket, false);

		// p1 nao recebe broadcast dele mesmo quando includeSelf=false, mas p2 recebe
		assertThat(received1).isEmpty();
		assertThat(received2).containsExactly(dummyPacket);
	}

	private record DummyPlayer(int objectId, String name, int x, int y, int z, List<GameServerPacket> inbox)
			implements GameWorld.OnlinePlayer {
		@Override
		public void send(GameServerPacket packet) {
			inbox.add(packet);
		}
	}
}
