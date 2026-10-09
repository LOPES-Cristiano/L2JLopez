package com.lopez.l2j.game.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.game.html.HtmCache;
import com.lopez.l2j.game.multisell.MultiSellTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NpcQuestAndMultiSellIntegrationTest {

	private MultiSellTable multiSellTable;
	private HtmCache htmCache;

	@BeforeEach
	void setUp() {
		multiSellTable = new MultiSellTable("data/xml/multisell");
		htmCache = new HtmCache("data/html");
	}

	@Test
	void testMultiSellTableLoadsAndResolvesAliases() {
		// Teste 1: Multisell direto 1 ou 1000
		assertTrue(multiSellTable.get(1).isPresent() || multiSellTable.get(1000).isPresent(),
				"Deve carregar multisell direto 1 ou 1000");

		// Teste 2: Life Crystals do Adventurer's Guildsman (aliases 64 a 70 mapeados para 20064..20070)
		var ms64 = multiSellTable.get(64);
		assertTrue(ms64.isPresent(), "Multisell 64 (Craft Life Crystal) deve ser encontrado");
		assertEquals(64, ms64.get().listId());

		var ms65 = multiSellTable.get(65);
		assertTrue(ms65.isPresent(), "Multisell 65 (Craft Adventurer's Box) deve ser encontrado");

		var ms70 = multiSellTable.get(70);
		assertTrue(ms70.isPresent(), "Multisell 70 deve ser encontrado");

		// Teste 3: Lojas de facção Ketra / Varka
		var ms522 = multiSellTable.get(522);
		assertTrue(ms522.isPresent(), "Multisell 522 (Ketra Orcs tier 1) deve ser encontrado");

		var ms524 = multiSellTable.get(524);
		assertTrue(ms524.isPresent(), "Multisell 524 (Varka Silenos tier 1) deve ser encontrado");

		// Teste 4: Loja do Golden Ram (Abercrombie 526 e 521)
		var ms526 = multiSellTable.get(526);
		assertTrue(ms526.isPresent(), "Multisell 526 (Golden Ram Supplies) deve ser encontrado");
		assertFalse(ms526.get().entries().isEmpty(), "Multisell 526 deve conter entradas de produtos");

		var ms521 = multiSellTable.get(521);
		assertTrue(ms521.isPresent(), "Multisell 521 (Golden Ram alias) deve ser encontrado");

		// Teste 5: Castle Blacksmith (35098000)
		var msCastle = multiSellTable.get(35098000);
		assertTrue(msCastle.isPresent(), "Multisell 35098000 deve ser encontrado");
	}

	@Test
	void testHtmCacheRendersCastleAndNpcTokens() {
		String template = "<html><body><a action=\"bypass -h npc_%objectId%_multisell 35098000%castleid%\">Hello %player_name% from %npc_name% (ID: %npcId%)</a></body></html>";
		String rendered = htmCache.render(template, 1001, "Blacksmith Noel", "HeroPlayer", 3, 35182);

		assertTrue(rendered.contains("350980003"), "Token %castleid% deve ser substituido pelo ID do castelo (3 - Giran)");
		assertTrue(rendered.contains("npc_1001"), "Token %objectId% deve ser substituido pelo ID do objeto (1001)");
		assertTrue(rendered.contains("HeroPlayer"), "Token %player_name% deve ser substituido pelo nome do jogador");
		assertTrue(rendered.contains("Blacksmith Noel"), "Token %npc_name% deve ser substituido pelo nome do NPC");
		assertTrue(rendered.contains("ID: 35182"), "Token %npcId% deve ser substituido pelo ID do template do NPC");
	}

	@Test
	void testQuestCatalogIntegrity() {
		// Validar catálogo de quests de segunda classe
		var secondClassQuests = com.lopez.l2j.game.quest.SecondClassQuestCatalog.getAllQuests();
		assertNotNull(secondClassQuests);
		assertFalse(secondClassQuests.isEmpty(), "Catalogo de 2a classe deve conter as 23 trials/testimonies/tests");
		assertEquals(23, secondClassQuests.size());

		// Validar catálogo de quests de primeira classe
		var firstClassQuests = com.lopez.l2j.game.quest.FirstClassQuestCatalog.getAllQuests();
		assertNotNull(firstClassQuests);
		assertFalse(firstClassQuests.isEmpty(), "Catalogo de 1a classe deve conter todas as quests de 1a classe");
		assertEquals(18, firstClassQuests.size());
	}
}
