package com.lopez.l2j.game.zone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.game.item.InMemoryItemRepository;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.item.TestItems;
import com.lopez.l2j.game.model.InMemoryCharacterRepository;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.CharacterService;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.Die;
import com.lopez.l2j.network.game.packet.GameServerPacket.SetupGauge;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ZoneEnvironmentValidationTest {

	private ZoneTable zoneTable;
	private GameWorld world;
	private GameSession session;
	private PlayerCharacter player;
	private List<GameServerPacket> sentPackets;

	@BeforeEach
	void setUp() {
		sentPackets = new ArrayList<>();
		world = new GameWorld();
		zoneTable = new ZoneTable("data/xml/zone");

		// Cria uma zona custom de WATER e uma de DAMAGE para testes controlados
		Polygon waterPoly = new Polygon(new int[]{100, 200, 200, 100}, new int[]{100, 100, 200, 200}, 4);
		ZoneShape waterShape = new ZoneShape(-100, 100, waterPoly);
		Zone waterZone = new Zone(90001, "TestWaterZone", ZoneType.WATER, false, false, 0, 0, List.of(waterShape));

		Polygon dmgPoly = new Polygon(new int[]{500, 600, 600, 500}, new int[]{500, 500, 600, 600}, 4);
		ZoneShape dmgShape = new ZoneShape(-100, 100, dmgPoly);
		Zone dmgZone = new Zone(90002, "TestDamageZone", ZoneType.DAMAGE, false, false, 0, 0, List.of(dmgShape));

		// Injeta as zonas na zoneTable via reflexao para o teste
		addZoneToTable(zoneTable, waterZone);
		addZoneToTable(zoneTable, dmgZone);

		var repo = new InMemoryItemRepository();
		var inventoryService = new InventoryService(TestItems.table(), repo, ObjectIdFactory.sequential(0x20000000), 10_000_000);
		var charTemplates = new CharTemplateTable();
		var charRepo = new InMemoryCharacterRepository();
		var charService = new CharacterService(charRepo, charTemplates, inventoryService);

		GameSession.Context ctx = new GameSession.Context(746, 746, null, charService, inventoryService, world, null,
				null, null, null, null, null, null, null, null, null, null, null, null, List.of(),
				null, null, null, null, null, null, null, null, zoneTable, null, null, null, null, null, null, null, null, null, null,
				null, "TestServer");

		player = new PlayerCharacter(0x10000050, "Swimmer", "Adventurer", 40, 0, 0, 0, 0, 0, false, 0, 0, 0,
				1000, 500, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 1000.0, 500.0, 100.0);
		player.inventory(new Inventory(player.objectId()));
		player.moveTo(0, 0, 0);

		session = new GameSession(ctx, new byte[8], "127.0.0.1", sentPackets::add);
		setField(session, "state", GameClientPacket.State.IN_GAME);
		setField(session, "active", player);
		setField(session, "inWorld", true);
		world.add(session);
	}

	@Test
	@DisplayName("V.28: Entrada em zona de agua ativa SetupGauge CYAN de 60s e saida cancela o gauge")
	void testWaterZoneEntryAndExit() {
		// Jogador entra na zona de agua (150, 150, 0)
		player.moveTo(150, 150, 0);
		session.checkZoneEnvironment();

		// Deve enviar SetupGauge com cor CYAN (2) e 60000 ms
		boolean cyanGaugeFound = sentPackets.stream().anyMatch(p -> p instanceof SetupGauge g
				&& g.color() == SetupGauge.CYAN && g.time() == 60000);
		assertTrue(cyanGaugeFound, "Entrada na agua deve enviar barra de respiracao de 60s (CYAN)");

		// Jogador sai da agua (0, 0, 0)
		sentPackets.clear();
		player.moveTo(0, 0, 0);
		session.checkZoneEnvironment();

		// Deve enviar SetupGauge cancelando a barra (time = 0)
		boolean cancelGaugeFound = sentPackets.stream().anyMatch(p -> p instanceof SetupGauge g
				&& g.color() == SetupGauge.CYAN && g.time() == 0);
		assertTrue(cancelGaugeFound, "Saida da agua deve limpar a barra de respiracao (time = 0)");
	}

	@Test
	@DisplayName("V.28: Afogamento continuo apos 60s na agua reduz HP e causa morte")
	void testWaterDrowningDamageAndDeath() {
		player.moveTo(150, 150, 0);
		session.checkZoneEnvironment();

		// Simula que o jogador permaneceu submerso por mais de 60 segundos
		long longAgo = System.currentTimeMillis() - 65_000L;
		setField(session, "waterEntryTime", longAgo);
		setField(session, "lastDrownDamageTime", 0L);

		double hpBefore = player.currentHp();
		session.checkZoneEnvironment();

		// HP deve ter sido reduzido pelo afogamento (5% de 1000 = 50)
		assertTrue(player.currentHp() < hpBefore, "Jogador sem ar deve sofrer dano por afogamento");
		assertEquals(hpBefore - 50.0, player.currentHp(), 0.1);

		// Dano letal por afogamento
		player.currentHp(10.0);
		setField(session, "lastDrownDamageTime", 0L);
		session.checkZoneEnvironment();

		assertTrue(player.isDead(), "Jogador deve morrer quando afogamento zerar o HP");
		boolean diePacketFound = sentPackets.stream().anyMatch(p -> p instanceof Die);
		assertTrue(diePacketFound, "Morte por afogamento deve emitir pacote Die");
	}

	@Test
	@DisplayName("V.29: Zonas de Dano Ambiental (Lava / Pantano Acido) aplicam dano periodico")
	void testDamageZonePeriodicHarmAndDeath() {
		// Jogador entra na zona de dano ambiental (550, 550, 0)
		player.moveTo(550, 550, 0);

		double hpBefore = player.currentHp();
		setField(session, "lastDamageZoneTickTime", 0L);
		session.checkZoneEnvironment();

		// Deve sofrer dano da lava/pantano (3% de 1000 = 30)
		assertTrue(player.currentHp() < hpBefore, "Jogador dentro de zona de dano deve sofrer dano ambiental");
		assertEquals(hpBefore - 30.0, player.currentHp(), 0.1);

		// Dano letal ambiental
		player.currentHp(5.0);
		setField(session, "lastDamageZoneTickTime", 0L);
		session.checkZoneEnvironment();

		assertTrue(player.isDead(), "Dano ambiental continuo deve levar a morte");
		boolean diePacketFound = sentPackets.stream().anyMatch(p -> p instanceof Die);
		assertTrue(diePacketFound, "Morte por dano ambiental deve emitir pacote Die");
	}

	@Test
	@DisplayName("V.20: Requisito de reagentes de skill bloqueia conjuracao se sem itens e consome ao conjurar")
	void testSkillReagentValidation() {
		// Mock de skill com consumo de reagente: itemId 3031 (Spirit Ore), count 1
		com.lopez.l2j.game.skill.SkillTemplate sk = new com.lopez.l2j.game.skill.SkillTemplate(
				9999, 1, "Blessed Blood", com.lopez.l2j.game.skill.SkillTemplate.OperateType.ACTIVE, "BUFF",
				"target_self", true, 50, 0, 0, 0.0,
				0, 0, 1000, 0, 0, 40,
				0.0, false, 3031, 1,
				List.of(), List.of(), null, null
		);

		// Sem o reagente no inventario: a invocacao do cast e rejeitada com S1_CANNOT_BE_USED
		var inv = player.inventory();
		assertEquals(0, inv.getItemCount(3031));

		// Invoca o metodo castSkill via reflexao
		invokeMethod(session, "castSkill", new Class<?>[]{com.lopez.l2j.game.skill.SkillTemplate.class, boolean.class},
				new Object[]{sk, false});

		boolean cannotBeUsedMsg = sentPackets.stream().anyMatch(p -> p instanceof SystemMessage sm
				&& sm.id() == SystemMessage.S1_CANNOT_BE_USED);
		assertTrue(cannotBeUsedMsg, "Conjuracao sem reagentes deve falhar com S1_CANNOT_BE_USED");

		// Adiciona 5 Spirit Ores ao inventario
		ItemTemplate oreTpl = ItemTemplate.etc(3031, 3031, "Spirit Ore", "material", "asset", 1, "none", 100, true, true, true, true);
		inv.add(new ItemInstance(0x30000088, oreTpl, player.objectId(), 5));
		assertEquals(5, inv.getItemCount(3031));

		sentPackets.clear();
		invokeMethod(session, "castSkill", new Class<?>[]{com.lopez.l2j.game.skill.SkillTemplate.class, boolean.class},
				new Object[]{sk, false});

		// Conjuracao iniciou (casting = true)
		boolean casting = (boolean) getField(session, "casting");
		assertTrue(casting, "Com reagentes presentes no inventario, a conjuracao deve iniciar com sucesso");
	}

	@SuppressWarnings("unchecked")
	private static void addZoneToTable(ZoneTable table, Zone zone) {
		try {
			var allZonesField = ZoneTable.class.getDeclaredField("allZones");
			allZonesField.setAccessible(true);
			var allZones = (List<Zone>) allZonesField.get(table);
			allZones.add(zone);

			var byTypeField = ZoneTable.class.getDeclaredField("byType");
			byTypeField.setAccessible(true);
			var byType = (java.util.Map<ZoneType, List<Zone>>) byTypeField.get(table);
			byType.computeIfAbsent(zone.type(), k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(zone);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static void setField(Object target, String name, Object val) {
		try {
			var f = target.getClass().getDeclaredField(name);
			f.setAccessible(true);
			f.set(target, val);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static Object getField(Object target, String name) {
		try {
			var f = target.getClass().getDeclaredField(name);
			f.setAccessible(true);
			return f.get(target);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static Object invokeMethod(Object target, String name, Class<?>[] paramTypes, Object[] args) {
		try {
			var m = target.getClass().getDeclaredMethod(name, paramTypes);
			m.setAccessible(true);
			return m.invoke(target, args);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
