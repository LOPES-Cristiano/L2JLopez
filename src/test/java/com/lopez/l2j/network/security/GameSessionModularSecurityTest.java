package com.lopez.l2j.network.security;

import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.handler.admin.AdminCommandHandlerRegistry;
import com.lopez.l2j.network.game.handler.admin.AdminGeneralHandler;
import com.lopez.l2j.network.game.handler.admin.IAdminCommandHandler;
import com.lopez.l2j.network.game.handler.bypass.BypassHandlerRegistry;
import com.lopez.l2j.network.game.handler.bypass.BypassPreferencesHandler;
import com.lopez.l2j.network.game.handler.voiced.VoicedCommandHandlerRegistry;
import com.lopez.l2j.network.game.handler.voiced.VoicedGeneralHandler;
import com.lopez.l2j.network.game.security.BypassEncoderService;
import com.lopez.l2j.network.game.security.PacketRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GameSessionModularSecurityTest {

	private AdminCommandHandlerRegistry adminRegistry;
	private VoicedCommandHandlerRegistry voicedRegistry;
	private BypassHandlerRegistry bypassRegistry;
	private BypassEncoderService bypassEncoder;
	private PacketRateLimiter rateLimiter;

	@BeforeEach
	void setUp() {
		adminRegistry = new AdminCommandHandlerRegistry(List.<IAdminCommandHandler>of(
				new AdminGeneralHandler(),
				new com.lopez.l2j.network.game.handler.admin.AdminStatusHandler(null, null, null, null)
		));
		voicedRegistry = new VoicedCommandHandlerRegistry(List.of(new VoicedGeneralHandler()));
		bypassRegistry = new BypassHandlerRegistry(List.of(new BypassPreferencesHandler()));
		bypassEncoder = new BypassEncoderService();
		rateLimiter = new PacketRateLimiter();
	}

	@Test
	void testAdminCommandsRegistered() {
		assertTrue(adminRegistry.hasCommand("heal"));
		assertTrue(adminRegistry.hasCommand("kill"));
		assertTrue(adminRegistry.hasCommand("delete"));
		assertTrue(adminRegistry.hasCommand("del"));
		assertTrue(adminRegistry.hasCommand("unspawn"));

		// AIO / Hero / VIP commands
		assertTrue(adminRegistry.hasCommand("setaio"));
		assertTrue(adminRegistry.hasCommand("removeaio"));
		assertTrue(adminRegistry.hasCommand("set_massaio"));
		assertTrue(adminRegistry.hasCommand("sethero"));
		assertTrue(adminRegistry.hasCommand("removehero"));
		assertTrue(adminRegistry.hasCommand("set_masshero"));
		assertTrue(adminRegistry.hasCommand("setvip"));
		assertTrue(adminRegistry.hasCommand("removevip"));
		assertTrue(adminRegistry.hasCommand("set_massvip"));

		assertFalse(adminRegistry.hasCommand("unknown_cmd"));
	}

	@Test
	void testVoicedCommandsRegistered() {
		assertTrue(voicedRegistry.hasCommand("online"));
		assertTrue(voicedRegistry.hasCommand(".online"));
		assertTrue(voicedRegistry.hasCommand("stats"));
		assertTrue(voicedRegistry.hasCommand("menu"));
		assertTrue(voicedRegistry.hasCommand("deposit"));
		assertTrue(voicedRegistry.hasCommand("withdraw"));
		assertFalse(voicedRegistry.hasCommand("unknown_voiced"));
	}

	@Test
	void testBypassHandlerCanHandle() {
		BypassPreferencesHandler handler = new BypassPreferencesHandler();
		assertTrue(handler.canHandle("voiced_menutoggle autoloot"));
		assertTrue(handler.canHandle("antibot_validate 2"));
		assertFalse(handler.canHandle("npc_123_Chat 1"));
	}

	@Test
	void testRateLimiterRules() {
		PacketRateLimiter.SessionRateState state = new PacketRateLimiter.SessionRateState();
		long now = System.currentTimeMillis();

		// RequestEnchantItem permite max 1 em 500ms
		assertTrue(state.recordAndCheck(com.lopez.l2j.network.game.packet.GameClientPacket.RequestEnchantItem.class, 1, 500, now));
		assertFalse(state.recordAndCheck(com.lopez.l2j.network.game.packet.GameClientPacket.RequestEnchantItem.class, 1, 500, now + 10));
		assertTrue(state.recordAndCheck(com.lopez.l2j.network.game.packet.GameClientPacket.RequestEnchantItem.class, 1, 500, now + 600));
	}
}
