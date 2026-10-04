package com.lopez.l2j.game.npc;

import static org.assertj.core.api.Assertions.assertThat;

import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.world.GameWorld;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SpawnServiceTest {

	private GameWorld world;
	private NpcTemplateTable table;
	private ObjectIdFactory ids;
	private SpawnService service;

	@BeforeEach
	void setUp() {
		world = new GameWorld();
		NpcTemplate t = new NpcTemplate(
				20001, 20001, "Wolf", false, "", false,
				8.0, 16.0, 1, "male", "L2Monster",
				40, 50, 20, 10, 10, 5, 5, 250, 333,
				0, 0, 0, 50, 100, 0, false);
		table = NpcTemplateTable.of(List.of(t));
		ids = ObjectIdFactory.sequential(0x10000000);
		// Sem jdbc para o teste de spawn isolado
		service = new SpawnService(null, table, world, ids);
	}

	@Test
	void shouldSpawnNpcProgrammatically() {
		var npcOpt = service.spawn(20001, -71338, 258271, -3104, 0);

		assertThat(npcOpt).isPresent();
		NpcInstance npc = npcOpt.get();
		assertThat(npc.objectId()).isEqualTo(0x10000000);
		assertThat(npc.name()).isEqualTo("Wolf");
		assertThat(npc.x()).isEqualTo(-71338);
		assertThat(npc.y()).isEqualTo(258271);

		assertThat(world.npc(npc.objectId())).contains(npc);
		assertThat(world.findNpcsAround(-71338, 258271, 1000)).containsExactly(npc);
	}

	@Test
	void shouldReturnEmptyForUnknownTemplate() {
		var npcOpt = service.spawn(99999, 0, 0, 0, 0);
		assertThat(npcOpt).isEmpty();
		assertThat(world.totalNpcs()).isZero();
	}
}
