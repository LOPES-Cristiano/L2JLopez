package com.lopez.l2j.game.html;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HtmCacheTest {

	private HtmCache cache;

	@BeforeEach
	void setUp() {
		cache = new HtmCache("D:/Cristiano/Lineage/L2JDreamV2/game/data/html");
	}

	@Test
	void shouldRenderVariablesCorrectly() {
		String template = "<html><body>Hello %name%! My name is %npc_name% (obj: %objectId%).</body></html>";
		String rendered = cache.render(template, 0x12345678, "Roxxy", "PlayerOne");

		assertThat(rendered).isEqualTo("<html><body>Hello PlayerOne! My name is Roxxy (obj: 305419896).</body></html>");
	}

	@Test
	void shouldLoadTeleporterHtmlFromLegacyDatapack() {
		// Gatekeeper Roxxy (npcId 30006, tipo L2Teleporter)
		String html = cache.getNpcHtml(30006, "L2Teleporter", 0);
		assertThat(html).isNotNull();
		assertThat(html).contains("Gatekeeper Roxxy");
		assertThat(html).contains("bypass -h npc_%objectId%_Chat 1");
	}

	@Test
	void shouldLoadSubPageChatHtml() {
		// Subpagina de teleporte (val = 1 -> teleporter/30006-1.htm)
		String html = cache.getNpcHtml(30006, "L2Teleporter", 1);
		assertThat(html).isNotNull();
		assertThat(html).contains("The Village of Gludin");
		assertThat(html).contains("bypass -h npc_%objectId%_goto 85");
	}

	@Test
	void shouldFallbackGracefullyForUnknownNpc() {
		String html = cache.getNpcHtml(99999, "L2Npc", 0);
		assertThat(html).isNotNull();
		assertThat(html).contains("%objectId%");
	}
}
