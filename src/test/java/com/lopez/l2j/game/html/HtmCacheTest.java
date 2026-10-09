package com.lopez.l2j.game.html;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HtmCacheTest {

	private HtmCache cache;

	@BeforeEach
	void setUp() {
		cache = new HtmCache("data/html");
	}

	@Test
	@DisplayName("Renderiza variaveis canonicas do Lineage 2 (%name%, %npc_name%, %objectId%)")
	void shouldRenderVariablesCorrectly() {
		String template = "<html><body>Hello %name%! My name is %npc_name% (obj: %objectId%).</body></html>";
		String rendered = cache.render(template, 0x12345678, "Roxxy", "PlayerOne");

		assertThat(rendered).isEqualTo("<html><body>Hello PlayerOne! My name is Roxxy (obj: 305419896).</body></html>");
	}

	@Test
	@DisplayName("Onda 1: Teleporters (Gatekeepers, Castle GK, Spirit, Race Track)")
	void shouldResolveWave1Teleporters() {
		// Gatekeeper Roxxy (30006)
		String html = cache.getNpcHtml(30006, "L2Teleporter", 0);
		assertThat(html).isNotNull();
		assertThat(html).contains("Gatekeeper Roxxy");
		assertThat(html).contains("bypass -h npc_%objectId%_Chat 1");

		// Subpagina de teleporte Roxxy (val = 1)
		String htmlSub = cache.getNpcHtml(30006, "L2Teleporter", 1);
		assertThat(htmlSub).isNotNull();
		assertThat(htmlSub).contains("The Village of Gludin");
		assertThat(htmlSub).contains("bypass -h npc_%objectId%_goto 85");

		// Castle Mass Teleporter (35092)
		String castleGk = cache.getNpcHtml(35092, "L2CastleTeleporter", 0);
		assertThat(castleGk).isNotNull();
		assertThat(castleGk).contains("bypass -h npc_%objectId%_tele");

		// Gatekeeper Spirit (31111)
		String spirit = cache.getNpcHtml(31111, "L2Teleporter", 0);
		assertThat(spirit).isNotNull();
		assertThat(spirit).contains("Gatekeeper Spirit");
		assertThat(spirit).contains("Anakim");

		// Monster Race Track Guide (31211)
		String raceGuide = cache.getNpcHtml(31211, "L2Teleporter", 0);
		assertThat(raceGuide).isNotNull();
		assertThat(raceGuide).contains("Monster Derby Track");
	}

	@Test
	@DisplayName("Onda 2: Trainers, Mystics, Priests, Village Masters, Symbols")
	void shouldResolveWave2TrainersAndMasters() {
		// Symbol Maker Marsden (31046)
		String symbolMaker = cache.getNpcHtml(31046, "L2SymbolMaker", 0);
		assertThat(symbolMaker).isNotNull();
		assertThat(symbolMaker).contains("bypass -h npc_%objectId%_Draw");
		assertThat(symbolMaker).contains("bypass -h npc_%objectId%_RemoveList");

		// Village Master Gallint (30017)
		String master = cache.getNpcHtml(30017, "L2VillageMaster", 0);
		assertThat(master).isNotNull();
		assertThat(master).containsIgnoringCase("Gallint");

		// Trainer / Priest (30022)
		String trainer = cache.getNpcHtml(30022, "L2Trainer", 0);
		assertThat(trainer).isNotNull();
	}

	@Test
	@DisplayName("Onda 3: Merchants, Warehouses, Fishermen, Newbie Helpers")
	void shouldResolveWave3MerchantsAndServices() {
		// Newbie Helper (30598)
		String newbie = cache.getNpcHtml(30598, "L2NewbieHelper", 0);
		assertThat(newbie).isNotNull();
		assertThat(newbie).containsIgnoringCase("Newbie");

		// Trader/Merchant (30001)
		String merchant = cache.getNpcHtml(30001, "L2Merchant", 0);
		assertThat(merchant).isNotNull();
		assertThat(merchant).containsIgnoringCase("Trader Lector");

		// Warehouse (30005)
		String wh = cache.getNpcHtml(30005, "L2Warehouse", 0);
		assertThat(wh).isNotNull();
	}

	@Test
	@DisplayName("Onda 4: Castle, Fortress, Clan Hall, Manor, Auction, Doormen")
	void shouldResolveWave4CastleAndResidenceNpcs() {
		// Castle Chamberlain (35099)
		String chamberlain = cache.getNpcHtml(35099, "L2CastleChamberlain", 0);
		assertThat(chamberlain).isNotNull();
		assertThat(chamberlain).containsIgnoringCase("Chamberlain");

		// Castle Magician (35100)
		String magician = cache.getNpcHtml(35100, "L2CastleMagician", 0);
		assertThat(magician).isNotNull();
		assertThat(magician).containsIgnoringCase("Court Magician");

		// Manor Manager (35101)
		String manor = cache.getNpcHtml(35101, "L2ManorManager", 0);
		assertThat(manor).isNotNull();
		assertThat(manor).containsIgnoringCase("Purchase seed");

		// Auctioneer (30985)
		String auctioneer = cache.getNpcHtml(30985, "L2Auctioneer", 0);
		assertThat(auctioneer).isNotNull();
		assertThat(auctioneer).containsIgnoringCase("clan hall");
		assertThat(auctioneer).contains("bypass -h npc_%objectId%_list");

		// Doorman (30492)
		String doorman = cache.getNpcHtml(30492, "L2Doormen", 0);
		assertThat(doorman).isNotNull();
	}

	@Test
	@DisplayName("Onda 5: Seven Signs (Mammons, Witches, Guides, Rift, Ketra/Varka)")
	void shouldResolveWave5SevenSignsAndSpecials() {
		// Merchant of Mammon (31113)
		String mammonMerch = cache.getNpcHtml(31113, "L2Merchant", 0);
		assertThat(mammonMerch).isNotNull();
		assertThat(mammonMerch).containsIgnoringCase("Merchant of Mammon");

		// Blacksmith of Mammon (31126)
		String mammonBlack = cache.getNpcHtml(31126, "L2Trainer", 0);
		assertThat(mammonBlack).isNotNull();
		assertThat(mammonBlack).containsIgnoringCase("Blacksmith of Mammon");

		// Black Marketeer of Mammon (31092)
		String blkMrkt = cache.getNpcHtml(31092, "L2Merchant", 0);
		assertThat(blkMrkt).isNotNull();
		assertThat(blkMrkt).containsIgnoringCase("Black Marketeer");

		// Festival Witch (31132)
		String witch = cache.getNpcHtml(31132, "L2FestivalGuide", 0);
		assertThat(witch).isNotNull();
		assertThat(witch).containsIgnoringCase("Festival Witch");

		// Rift Guardian (31865)
		String rift = cache.getNpcHtml(31865, "L2Teleporter", 0);
		assertThat(rift).isNotNull();
		assertThat(rift).containsIgnoringCase("Border");

		// Ketra Friend Kurfa (31376)
		String ketraGk = cache.getNpcHtml(31376, "L2Teleporter", 0);
		assertThat(ketraGk).isNotNull();
		assertThat(ketraGk).containsIgnoringCase("Kurfa");
	}

	@Test
	@DisplayName("Fallback gracioso para NPCs desconhecidos (npcdefault.htm)")
	void shouldFallbackGracefullyForUnknownNpc() {
		String html = cache.getNpcHtml(99999, "L2Npc", 0);
		assertThat(html).isNotNull();
		assertThat(html).contains("%objectId%");
	}
}
