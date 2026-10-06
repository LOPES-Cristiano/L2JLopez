package com.lopez.l2j.game.npc.daynight;

import static org.junit.jupiter.api.Assertions.*;

import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.npc.NpcTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DayNightSpawnServiceTest {

	private GameWorld world;
	private NpcTemplateTable templates;
	private ObjectIdFactory objectIds;
	private DayNightSpawnService service;

	private static class MockOnlinePlayer implements GameWorld.OnlinePlayer {
		private final int objectId;
		private final String name;
		private final PlayerCharacter character;
		final List<GameServerPacket> packets = new ArrayList<>();

		MockOnlinePlayer(int objectId, String name, int race) {
			this.objectId = objectId;
			this.name = name;
			this.character = new PlayerCharacter(objectId, "acc", name, 40, 0, 0, race, 0, 0, false,
					0, 0, 0, 100, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100, 100, 100);
		}

		@Override public int objectId() { return objectId; }
		@Override public String name() { return name; }
		@Override public int x() { return 0; }
		@Override public int y() { return 0; }
		@Override public int z() { return 0; }
		@Override public void send(GameServerPacket packet) { packets.add(packet); }
		@Override public PlayerCharacter character() { return character; }
	}

	@BeforeEach
	void setUp() {
		world = new GameWorld();
		AtomicInteger seq = new AtomicInteger(1000);
		objectIds = seq::getAndIncrement;

		Map<Integer, NpcTemplate> tplMap = new ConcurrentHashMap<>();
		// Template normal diurno
		tplMap.put(20001, NpcTemplate.fallback(20001, "Day Wolf", "L2Monster"));
		// Template normal noturno
		tplMap.put(20002, NpcTemplate.fallback(20002, "Night Zombie", "L2Monster"));
		// Template Hellmann (Raid Boss Noturno)
		tplMap.put(DayNightSpawnService.RAID_HELLMANN_ID, NpcTemplate.fallback(DayNightSpawnService.RAID_HELLMANN_ID, "Raid Boss Hellmann", "L2RaidBoss"));

		templates = NpcTemplateTable.of(tplMap.values());

		service = new DayNightSpawnService(null, templates, world, objectIds, null);
	}

	@Test
	@DisplayName("Registro de criaturas e spawn no estado inicial diurno")
	void testInitialDayState() {
		// Inicializa no modo dia (isNight = false)
		service.notifyChangeMode(false);
		assertFalse(service.isNight());

		// Adiciona criatura do dia
		service.addDayCreature(new DayNightSpawnData(20001, 100, 200, -10, 0));
		// Adiciona criatura da noite
		service.addNightCreature(new DayNightSpawnData(20002, 300, 400, -10, 0));

		assertEquals(1, service.daySpawns().size());
		assertEquals(1, service.nightSpawns().size());

		// Apenas o monstro do dia deve estar ativo no mundo
		assertEquals(1, service.activeDayInstances().size());
		assertEquals(0, service.activeNightInstances().size());
		assertNull(service.activeNightBoss());
		assertEquals(1, world.totalNpcs());
	}

	@Test
	@DisplayName("Transicao para noite despawna monstros do dia, invoca monstros da noite e Hellmann")
	void testTransitionToNight() {
		service.notifyChangeMode(false);
		service.addDayCreature(new DayNightSpawnData(20001, 100, 200, -10, 0));
		service.addNightCreature(new DayNightSpawnData(20002, 300, 400, -10, 0));

		// Jogador Humano (race=0) e Dark Elf (race=2)
		MockOnlinePlayer human = new MockOnlinePlayer(1, "HumanFighter", 0);
		MockOnlinePlayer darkElf = new MockOnlinePlayer(2, "DarkMage", 2);
		world.add(human);
		world.add(darkElf);

		// Mudanca para Noite!
		service.notifyChangeMode(true);
		assertTrue(service.isNight());

		// Dia zerado, noite ativo
		assertEquals(0, service.activeDayInstances().size());
		assertEquals(1, service.activeNightInstances().size());
		assertNotNull(service.activeNightBoss());
		assertEquals(DayNightSpawnService.RAID_HELLMANN_ID, service.activeNightBoss().npcId());

		// Total de NPCs no mundo = 1 night mob + 1 night boss = 2
		assertEquals(2, world.totalNpcs());

		// Dark Elf recebeu a mensagem de Shadow Sense ativado
		assertTrue(darkElf.packets.stream().anyMatch(p -> p instanceof SystemMessage sm && sm.id() == SystemMessage.S1_NIGHT_EFFECT_APPLIES));
		// Humano nao recebeu mensagem
		assertFalse(human.packets.stream().anyMatch(p -> p instanceof SystemMessage sm && sm.id() == SystemMessage.S1_NIGHT_EFFECT_APPLIES));
	}

	@Test
	@DisplayName("Transicao de volta para o dia remove Hellmann e mobs noturnos e restaura os diurnos")
	void testTransitionBackToDay() {
		service.notifyChangeMode(true);
		service.addDayCreature(new DayNightSpawnData(20001, 100, 200, -10, 0));
		service.addNightCreature(new DayNightSpawnData(20002, 300, 400, -10, 0));

		MockOnlinePlayer darkElf = new MockOnlinePlayer(2, "DarkMage", 2);
		world.add(darkElf);

		// Mudanca de volta para o Dia
		service.notifyChangeMode(false);
		assertFalse(service.isNight());

		// Hellmann sumiu, monstros noturnos sumiram, monstros diurnos voltaram
		assertNull(service.activeNightBoss());
		assertEquals(0, service.activeNightInstances().size());
		assertEquals(1, service.activeDayInstances().size());
		assertEquals(1, world.totalNpcs());

		// Dark Elf recebeu notificacao de que o efeito de Shadow Sense desapareceu
		assertTrue(darkElf.packets.stream().anyMatch(p -> p instanceof SystemMessage sm && sm.id() == SystemMessage.S1_NIGHT_EFFECT_DISAPPEARS));
	}

	@Test
	@DisplayName("Limpeza total cleanUp remove todas as entidades do mundo")
	void testCleanUp() {
		service.notifyChangeMode(true);
		service.addDayCreature(new DayNightSpawnData(20001, 100, 200, -10, 0));
		service.addNightCreature(new DayNightSpawnData(20002, 300, 400, -10, 0));
		assertTrue(world.totalNpcs() > 0);

		service.cleanUp();
		assertEquals(0, service.daySpawns().size());
		assertEquals(0, service.nightSpawns().size());
		assertEquals(0, service.activeDayInstances().size());
		assertEquals(0, service.activeNightInstances().size());
		assertNull(service.activeNightBoss());
		assertEquals(0, world.totalNpcs());
	}
}
