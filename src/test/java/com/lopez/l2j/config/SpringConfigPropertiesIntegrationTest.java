package com.lopez.l2j.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.game.boss.GrandBossManager;
import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.drop.DropService;
import com.lopez.l2j.game.drop.DropTable;
import com.lopez.l2j.game.model.CharacterRepository;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.PlayerStats;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.service.CharacterService;
import com.lopez.l2j.game.service.CharacterService.CreateRequest;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.network.game.packet.GameServerPacket.ExStorageMaxCount;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SpringConfigPropertiesIntegrationTest {

	@Autowired
	ServerProperties serverProperties;

	@Autowired
	CharacterService characterService;

	@Autowired
	InventoryService inventoryService;

	@Autowired
	CombatService combatService;

	@Autowired
	DropTable dropTable;

	@Autowired
	DropService dropService;

	@Autowired
	GrandBossManager grandBossManager;

	@Test
	void serverPropertiesBoundFromPropertiesFiles() {
		assertNotNull(serverProperties);

		// rates.properties -> RateXp = 100., RateSp = 100., RateDropAdena = 100
		assertEquals(100.0, serverProperties.rates().xp(), 0.01);
		assertEquals(100.0, serverProperties.rates().sp(), 0.01);
		assertEquals(100.0, serverProperties.rates().adena(), 0.01);

		// custom.properties -> StartingAdena = 10000000
		assertEquals(10_000_000, serverProperties.game().startingAdena());

		// gameserver.properties -> ServerName = L2JLopez
		assertEquals("L2JLopez", serverProperties.serverName());
	}

	@Test
	void startingAdenaAppliedWhenCreatingCharacter() {
		assertEquals(10_000_000, serverProperties.game().startingAdena());
		assertEquals(10_000_000, Config.STARTING_ADENA);

		var repo = new com.lopez.l2j.game.model.InMemoryCharacterRepository();
		var items = new com.lopez.l2j.game.item.InMemoryItemRepository();
		var ids = com.lopez.l2j.game.model.ObjectIdFactory.sequential(0x20000000);
		var invService = new InventoryService(com.lopez.l2j.game.item.TestItems.table(), items, ids, serverProperties);
		var charService = new CharacterService(repo, new CharTemplateTable(), invService);

		var res = charService.create(new CreateRequest(
				"cfg_test_acc", "CfgChar" + System.currentTimeMillis() % 10000, 0, 0, 0, 0, 0, 0));
		assertTrue(res.ok(), "Personagem deve ser criado");

		var inv = invService.load(res.character().objectId());
		var adena = inv.byItemId(57);
		assertTrue(adena.isPresent(), "Deve ter adena de inicializacao");
		assertEquals(10_000_000, adena.get().count(), "Deve conter 10.000.000 adena conforme custom.properties");
	}

	@Test
	void combatServiceUsesActiveXpSpRates() {
		NpcTemplate template = new NpcTemplate(20001, 20001, "Wolf", false, "", false, 10.0, 15.0, 10, "male",
				"L2Monster", 40, 100, 20, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false, 100L, 10);
		NpcInstance npc = new NpcInstance(99999, template, 0, 0, 0, 0);

		// Aplica dano fatal
		var hit = combatService.applyDamage(npc, 200, 0);
		assertTrue(hit.isDead());
		// baseExp = 100, RateXp = 100.0 -> expReward = 10000
		assertEquals(10000, hit.expReward());
		// baseSp = 10, RateSp = 100.0 -> spReward = 1000
		assertEquals(1000, hit.spReward());
	}

	@Test
	void offlineTradeFlagFromModsProperties() {
		// mods.properties -> AllowOfflineTrade = false
		assertFalse(Config.ALLOW_OFFLINE_TRADE);

		// Ao alterar dinamicamente:
		Config.setProperty("AllowOfflineTrade", "True");
		assertTrue(Config.ALLOW_OFFLINE_TRADE);

		// Restaura para o valor do arquivo
		Config.reload();
		assertFalse(Config.ALLOW_OFFLINE_TRADE);
	}

	@Test
	void storageSlotsRespectDwarfAndProperties() {
		PlayerCharacter human = new PlayerCharacter(101, "acc", "Human", 1, 0, 0, 0, 0, 0, false, 0, 0, 0, 100, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100, 100, 100);
		PlayerCharacter dwarf = new PlayerCharacter(102, "acc", "Dwarf", 1, 0, 0, 4, 53, 53, false, 0, 0, 0, 100, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100, 100, 100);

		ExStorageMaxCount humanStorage = ExStorageMaxCount.of(human);
		ExStorageMaxCount dwarfStorage = ExStorageMaxCount.of(dwarf);

		// Conforme player.properties e rates.properties
		assertEquals(80, humanStorage.inventory());
		assertEquals(100, dwarfStorage.inventory());
		assertEquals(100, humanStorage.warehouse());
		assertEquals(120, dwarfStorage.warehouse());
		assertEquals(4, humanStorage.privateSell());
		assertEquals(6, dwarfStorage.privateSell());
	}

	@Test
	void grandBossManagerLoadsRespawnFromBossesProperties() {
		var qa = grandBossManager.getBoss(GrandBossManager.QUEEN_ANT).orElseThrow();
		assertEquals(1140, qa.minRespawnMinutes());
		assertEquals(2160, qa.maxRespawnMinutes());

		var baium = grandBossManager.getBoss(GrandBossManager.BAIUM).orElseThrow();
		assertEquals(7200, baium.minRespawnMinutes());
		assertEquals(10080, baium.maxRespawnMinutes());
	}

	@Test
	void dropServiceAppliesDeepBluePenalty() {
		// Monstro nivel 10 abatido por jogador nivel 30 (diferenca 20 niveis >= 9)
		var dropsWithPenalty = dropService.rollDrops(20001, 30, 10);
		assertTrue(dropsWithPenalty.isEmpty(), "Com UseDeepBlueDropRules e 20 niveis de diferenca, drop deve zerar");
	}
}
