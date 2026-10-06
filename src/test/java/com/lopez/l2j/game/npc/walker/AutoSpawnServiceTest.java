package com.lopez.l2j.game.npc.walker;

import static org.junit.jupiter.api.Assertions.*;

import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.npc.NpcTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.MoveToLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AutoSpawnServiceTest {

	private NpcWalkerRoutesTable routesTable;
	private GameWorld world;
	private ObjectIdFactory objectIds;
	private NpcTemplateTable templates;
	private AutoSpawnService service;

	private static class MockOnlinePlayer implements GameWorld.OnlinePlayer {
		private final int objectId;
		private final String name;
		private final int x;
		private final int y;
		private final int z;
		final List<GameServerPacket> packets = new ArrayList<>();

		MockOnlinePlayer(int objectId, String name, int x, int y, int z) {
			this.objectId = objectId;
			this.name = name;
			this.x = x;
			this.y = y;
			this.z = z;
		}

		@Override public int objectId() { return objectId; }
		@Override public String name() { return name; }
		@Override public int x() { return x; }
		@Override public int y() { return y; }
		@Override public int z() { return z; }
		@Override public void send(GameServerPacket packet) { packets.add(packet); }
	}

	@BeforeEach
	void setUp() {
		routesTable = new NpcWalkerRoutesTable();
		world = new GameWorld();
		AtomicInteger seq = new AtomicInteger(5000);
		objectIds = seq::getAndIncrement;

		NpcTemplate tpl = NpcTemplate.fallback(31361, "Guard Evans", "L2Guard");
		templates = NpcTemplateTable.of(List.of(tpl));

		service = new AutoSpawnService(routesTable, world, objectIds, templates);
	}

	@Test
	@DisplayName("Carregamento de walkers_routes.xml real")
	void testLoadRealRoutes() {
		NpcWalkerRoutesTable realTable = new NpcWalkerRoutesTable();
		realTable.load();
		assertTrue(realTable.totalRoutes() > 0, "Deve conter dezenas de rotas");
		assertTrue(realTable.totalNodes() > 100, "Deve conter mais de 100 waypoints");

		var route1 = realTable.getRoute(1);
		assertFalse(route1.isEmpty());
		assertEquals(31361, route1.get(0).npcId());
	}

	@Test
	@DisplayName("Walker percorre waypoints, altera coordenadas, fala frases e cicla rota")
	void testWalkerPatrolCycle() {
		// Cria rota de teste com 3 waypoints
		routesTable.add(new NpcWalkerNode(10, 31361, 1, "", 1000, 1000, -100, 2, false));
		routesTable.add(new NpcWalkerNode(10, 31361, 2, "Halt! Who goes there?", 2000, 2000, -100, 5, true));
		routesTable.add(new NpcWalkerNode(10, 31361, 3, "", 3000, 3000, -100, 1, false));

		MockOnlinePlayer nearbyPlayer = new MockOnlinePlayer(1, "PlayerWatcher", 1500, 1500, -100);
		world.add(nearbyPlayer);

		// Spawna o walker na rota 10
		Optional<NpcInstance> walkerOpt = service.spawnAndRegisterWalker(31361, 10);
		assertTrue(walkerOpt.isPresent());
		NpcInstance walker = walkerOpt.get();

		assertEquals(1000, walker.x());
		assertEquals(1000, walker.y());
		assertEquals(1, service.activeWalkersCount());

		long time = 10000L;

		// Passo 1: tempo antes do delay do no 1 -> nao se move ainda
		service.step(time);
		assertEquals(1000, walker.x());

		// Passo 2: tempo atinge o delay do no 1 -> move-se para o no 2
		time += 3000L;
		service.step(time);
		assertEquals(2000, walker.x());
		assertEquals(2000, walker.y());
		assertTrue(walker.isRunning(), "No 2 possui running=true");

		// Verifica que o jogador proximo recebeu MoveToLocation e a fala do guarda
		assertTrue(nearbyPlayer.packets.stream().anyMatch(p -> p instanceof MoveToLocation m && m.toX() == 2000));
		assertTrue(nearbyPlayer.packets.stream().anyMatch(p -> p instanceof CreatureSay cs && cs.text().equals("Halt! Who goes there?")));

		// Passo 3: tempo atinge o delay do no 2 (5 segundos) -> move-se para o no 3
		time += 6000L;
		service.step(time);
		assertEquals(3000, walker.x());
		assertEquals(3000, walker.y());
		assertFalse(walker.isRunning(), "No 3 possui running=false");

		// Passo 4: ciclo volta para o no 1 (loop infinito de patrulha)
		time += 2000L;
		service.step(time);
		assertEquals(1000, walker.x());
		assertEquals(1000, walker.y());
	}

	@Test
	@DisplayName("AutoSpawn periodico: spawn, anuncio, despawn e respawn")
	void testPeriodicAutoSpawn() {
		// Registra auto spawn: a cada 2000ms surge e fica ativo por 1000ms
		service.registerAutoSpawn(1, 31361, 500, 500, 0, 0, 2000L, 1000L, "A traveling merchant has arrived!");
		assertEquals(1, service.autoSpawnsCount());

		MockOnlinePlayer player = new MockOnlinePlayer(2, "MerchantCustomer", 0, 0, 0);
		world.add(player);

		long time = 1000L;
		service.step(time);
		assertEquals(0, world.totalNpcs(), "Ainda nao deu tempo de spawnar");

		// t = 3000ms -> spawn!
		time = 3000L;
		service.step(time);
		assertEquals(1, world.totalNpcs(), "NPC deve ter sido spawnado");
		assertTrue(player.packets.stream().anyMatch(p -> p instanceof CreatureSay cs && cs.text().contains("traveling merchant")));

		// t = 4100ms -> despawn! (ficou ativo por mais de 1000ms)
		time = 4100L;
		service.step(time);
		assertEquals(0, world.totalNpcs(), "NPC deve ter despawnado");

		// Cancela o agendamento
		service.cancelAutoSpawn(1);
		assertEquals(0, service.autoSpawnsCount());
	}
}
