package com.lopez.l2j.network.game.handler.packet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.chat.WordFilterTable;
import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemSlots;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.template.CharTemplate;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.zone.Zone;
import com.lopez.l2j.game.zone.ZoneShape;
import com.lopez.l2j.game.zone.ZoneTable;
import com.lopez.l2j.game.zone.ZoneType;
import com.lopez.l2j.network.game.packet.GameClientPacket.Say2;
import com.lopez.l2j.network.game.packet.GameServerPacket.ActionFailed;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChatConfigurationTest {

	@BeforeEach
	void resetConfigs() {
		Config.reload();
		Config.GLOBAL_CHAT = "GLOBAL";
		Config.TRADE_CHAT = "GLOBAL";
		Config.USE_CHAT_FILTER = false;
		Config.CHAT_FILTER_CHARS = "...";
		Config.ALLOW_MULTILINE_CHAT = false;
		Config.CHAT_LENGTH = 120;
		Config.CHAT_FILTER_KARMA = 0;
		Config.SHOUT_CHAT_REUSE_DELAY = 0;
		Config.TRADE_CHAT_REUSE_DELAY = 0;
		Config.HERO_CHAT_REUSE_DELAY = 0;
		Config.SHOUT_CHAT_LEVEL = 1;
		Config.TRADE_CHAT_LEVEL = 1;
		Config.ZONE_TOWN = 0;
		Config.USE_BOW_DISTANCE_PENALTY = false;
		Config.MAX_BOW_DISTANCE_PENALTY = 0.6f;
	}

	@Test
	@DisplayName("WordFilterTable deve respeitar ChatFilterChars do options.properties")
	void wordFilterTableRespectsOptionsProperties() {
		WordFilterTable filter = new WordFilterTable("non_existent_file.txt");
		filter.addPattern("palavrao");
		filter.addPattern("hack");

		// Com replacement padrão
		Config.CHAT_FILTER_CHARS = "...";
		assertEquals("ola seu ...", filter.filter("ola seu palavrao"));

		// Com replacement customizado
		Config.CHAT_FILTER_CHARS = "***";
		assertEquals("ola seu ***", filter.filter("ola seu palavrao"));
		assertEquals("nao use ***", filter.filter("nao use hack"));
	}

	@Test
	@DisplayName("ZoneTown = 2 deve transformar cidade em Zona de Combate em ZoneTable")
	void zoneTownTwoDisablesPeaceZoneInTown() {
		List<Zone> zones = new ArrayList<>();
		java.awt.Polygon poly = new java.awt.Polygon(new int[] { 0, 1000, 1000, 0 }, new int[] { 0, 0, 1000, 1000 }, 4);
		ZoneShape shape = new ZoneShape(-1000, 1000, poly);
		// Cria uma zona do tipo TOWN
		zones.add(new Zone(1, "Giran Castle Town", ZoneType.TOWN, true, false, 0, 1, List.of(shape)));
		ZoneTable table = new ZoneTable(zones);

		// ZoneTown = 0 (Peace Always)
		Config.ZONE_TOWN = 0;
		assertTrue(table.isInsidePeace(500, 500, 0), "ZoneTown=0 deve manter cidade como zona de paz");
		assertFalse(table.isInsideArena(500, 500, 0), "ZoneTown=0 nao e arena");

		// ZoneTown = 2 (The combat zone always)
		Config.ZONE_TOWN = 2;
		assertFalse(table.isInsidePeace(500, 500, 0), "ZoneTown=2 deve remover status de paz da cidade");
		assertTrue(table.isInsideArena(500, 500, 0), "ZoneTown=2 deve considerar cidade como zona de combate");
	}

	@Test
	@DisplayName("UseBowDistancePenalty reduz dano de arco a curta distancia")
	void bowDistancePenaltyReducesDamageAtCloseRange() {
		CombatService combat = new CombatService(1.0, 1.0, null);
		CharTemplateTable templates = new CharTemplateTable();
		CharTemplate template = templates.get(0).orElseThrow();

		PlayerCharacter attacker = new PlayerCharacter(1001, "acc", "Archer", 40, 1000, 100, 0, 9, 9, false, 0, 0, 0, 1000, 500, 500, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100, 100, 100);
		// Posiciona atacante em 0,0,0
		attacker.teleport(0, 0, 0);

		// Equipa arco de TestItems
		ItemTemplate bowTemplate = com.lopez.l2j.game.item.TestItems.templates().stream()
				.filter(t -> t.id() == com.lopez.l2j.game.item.TestItems.BOW)
				.findFirst()
				.orElseThrow();
		ItemInstance bowItem = new ItemInstance(5001, bowTemplate, attacker.objectId(), 1);
		attacker.inventory().add(bowItem);
		attacker.inventory().equip(bowItem);

		PlayerCharacter target = new PlayerCharacter(1002, "acc", "Target", 40, 1000, 100, 0, 0, 0, false, 0, 0, 0, 1000, 500, 500, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100, 100, 100);

		// Alvo colado a 10 de distancia
		target.teleport(10, 0, 0);

		// Sem penalidade
		Config.USE_BOW_DISTANCE_PENALTY = false;
		double multNormal = CombatService.calcBowDistanceMultiplier(attacker, target.x(), target.y());
		assertEquals(1.0, multNormal, 0.001);

		// Com penalidade (MaxBowDistancePenalty = 0.6) a curta distancia (10 unidades)
		Config.USE_BOW_DISTANCE_PENALTY = true;
		Config.MAX_BOW_DISTANCE_PENALTY = 0.6f;
		double multClose = CombatService.calcBowDistanceMultiplier(attacker, target.x(), target.y());
		assertTrue(multClose < 0.65, "Multiplicador de dano de arco a queima-roupa deve ficar proximo de 0.6");

		// A longa distancia (850 unidades) a forca volta a 100% (1.0)
		double multFar = CombatService.calcBowDistanceMultiplier(attacker, 850, 0);
		assertEquals(1.0, multFar, 0.001, "Multiplicador de dano de arco na distancia maxima deve ser 1.0");
	}
}
