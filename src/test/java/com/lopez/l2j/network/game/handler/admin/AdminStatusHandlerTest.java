package com.lopez.l2j.network.game.handler.admin;

import com.lopez.l2j.game.model.CharacterRepository;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.AioService;
import com.lopez.l2j.game.service.HeroService;
import com.lopez.l2j.game.service.VipService;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class AdminStatusHandlerTest {

	private HeroService heroService;
	private VipService vipService;
	private AioService aioService;
	private CharacterRepository characterRepository;
	private AdminStatusHandler handler;
	private GameWorld world;

	private PlayerCharacter gmChar;
	private PlayerCharacter player1;
	private PlayerCharacter player2;

	private GameWorld.OnlinePlayer gmPlayer;
	private GameWorld.OnlinePlayer onlineP1;
	private GameWorld.OnlinePlayer onlineP2;

	private List<GameServerPacket> gmPackets;
	private List<GameServerPacket> p1Packets;

	private static class MockOnlinePlayer implements GameWorld.OnlinePlayer {
		private final PlayerCharacter character;
		private final List<GameServerPacket> packets = new ArrayList<>();

		MockOnlinePlayer(PlayerCharacter c) {
			this.character = c;
		}

		@Override public int objectId() { return character.objectId(); }
		@Override public String name() { return character.name(); }
		@Override public int x() { return character.x(); }
		@Override public int y() { return character.y(); }
		@Override public int z() { return character.z(); }
		@Override public PlayerCharacter character() { return character; }
		@Override public void send(GameServerPacket packet) { packets.add(packet); }
	}

	@BeforeEach
	void setUp() {
		heroService = new HeroService();
		vipService = new VipService();
		aioService = new AioService();
		characterRepository = mock(CharacterRepository.class);
		handler = new AdminStatusHandler(heroService, vipService, aioService, characterRepository);

		world = new GameWorld();
		gmPackets = new ArrayList<>();
		p1Packets = new ArrayList<>();

		gmChar = new PlayerCharacter(999, "admin_acc", "AdminChar", 80, 0, 0, 0, 0, 0, false, 0, 0, 0,
				1000, 1000, 1000, 0, 0, 0, 0, "", 100, 0, 0, 0, 0, 0, 0, 1000.0, 1000.0, 1000.0);

		player1 = new PlayerCharacter(1001, "acc1", "Cristiano", 80, 0, 0, 0, 0, 0, false, 0, 0, 0,
				1000, 1000, 1000, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 1000.0, 1000.0, 1000.0);

		player2 = new PlayerCharacter(1002, "acc2", "PlayerTwo", 80, 0, 0, 0, 0, 0, false, 0, 0, 0,
				1000, 1000, 1000, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 1000.0, 1000.0, 1000.0);

		gmPlayer = new MockOnlinePlayer(gmChar);
		onlineP1 = new MockOnlinePlayer(player1);
		onlineP2 = new MockOnlinePlayer(player2);

		world.add(gmPlayer);
		world.add(onlineP1);
		world.add(onlineP2);
	}

	private GameSession createGmSession() {
		GameSession.Context ctx = new GameSession.Context(746, 746, null, null, null, world, "L2Test");
		GameSession session = mock(GameSession.class);
		when(session.activeCharacter()).thenReturn(gmChar);
		when(session.context()).thenReturn(ctx);
		when(session.targetObjectId()).thenReturn(0);
		doAnswer(inv -> {
			gmPackets.add(inv.getArgument(0));
			return null;
		}).when(session).send(any(GameServerPacket.class));
		return session;
	}

	@Test
	void testSetHeroAndRemoveHero() {
		GameSession session = createGmSession();

		// //sethero Cristiano 10
		boolean ok = handler.useAdminCommand("sethero", session, "Cristiano 10");
		assertTrue(ok);
		assertTrue(player1.isHero());
		long exp1 = player1.heroExpiration();
		assertTrue(exp1 > System.currentTimeMillis());

		// //sethero Cristiano 1 -> Adiciona 1 dia ao tempo existente (10 + 1 = 11)
		handler.useAdminCommand("sethero", session, "Cristiano 1");
		assertTrue(player1.isHero());
		long exp2 = player1.heroExpiration();
		assertEquals(86_400_000L, exp2 - exp1);

		// //removehero Cristiano
		handler.useAdminCommand("removehero", session, "Cristiano");
		assertFalse(player1.isHero());
		assertEquals(0L, player1.heroExpiration());
	}

	@Test
	void testSetVipAndRemoveVip() {
		GameSession session = createGmSession();

		// //setvip Cristiano 1
		boolean ok = handler.useAdminCommand("setvip", session, "Cristiano 1");
		assertTrue(ok);
		assertTrue(player1.isVip());
		long exp1 = player1.vipExpiration();
		assertTrue(exp1 > System.currentTimeMillis());

		// //setvip Cristiano 5 -> Soma 5 dias aos restantes
		handler.useAdminCommand("setvip", session, "Cristiano 5");
		assertTrue(player1.isVip());
		long exp2 = player1.vipExpiration();
		assertEquals(5 * 86_400_000L, exp2 - exp1);

		// //removevip Cristiano
		handler.useAdminCommand("removevip", session, "Cristiano");
		assertFalse(player1.isVip());
		assertEquals(0L, player1.vipExpiration());
	}

	@Test
	void testSetAioAndRemoveAio() {
		GameSession session = createGmSession();

		// //setaio Cristiano 10
		boolean ok = handler.useAdminCommand("setaio", session, "Cristiano 10");
		assertTrue(ok);
		assertTrue(player1.isAio());
		long exp1 = player1.aioExpiration();
		assertTrue(exp1 > System.currentTimeMillis());

		// //setaio Cristiano 2 -> Soma 2 dias aos restantes
		handler.useAdminCommand("setaio", session, "Cristiano 2");
		assertTrue(player1.isAio());
		long exp2 = player1.aioExpiration();
		assertEquals(2 * 86_400_000L, exp2 - exp1);

		// //removeaio Cristiano
		handler.useAdminCommand("removeaio", session, "Cristiano");
		assertFalse(player1.isAio());
		assertEquals(0L, player1.aioExpiration());
	}

	@Test
	void testMassHeroAddsDaysToExistingHero() {
		GameSession session = createGmSession();

		// Jogador 1 ja tem 10 dias de hero
		heroService.setHero(player1, 10);
		long initialP1Exp = player1.heroExpiration();
		assertTrue(player1.isHero());

		// Jogador 2 nao tem hero
		assertFalse(player2.isHero());

		// Admin usa //set_masshero 1 (1 dia para todo mundo)
		boolean ok = handler.useAdminCommand("set_masshero", session, "1");
		assertTrue(ok);

		// Jogador 1 agora deve ter exatamente 11 dias (10 dias originais + 1 dia somado)
		assertTrue(player1.isHero());
		assertEquals(86_400_000L, player1.heroExpiration() - initialP1Exp);

		// Jogador 2 agora e Hero por 1 dia
		assertTrue(player2.isHero());
		assertTrue(player2.heroExpiration() > System.currentTimeMillis());
	}

	@Test
	void testMassVipAddsDaysToExistingVip() {
		GameSession session = createGmSession();

		// Jogador 1 ja tem 5 dias de VIP
		vipService.setVip(player1, 5);
		long initialP1Exp = player1.vipExpiration();
		assertTrue(player1.isVip());

		// Jogador 2 nao tem VIP
		assertFalse(player2.isVip());

		// Admin executa //set_massvip 3 (3 dias para todos)
		boolean ok = handler.useAdminCommand("set_massvip", session, "3");
		assertTrue(ok);

		// Jogador 1 deve ter 5 + 3 = 8 dias (diferenca de 3 dias)
		assertTrue(player1.isVip());
		assertEquals(3 * 86_400_000L, player1.vipExpiration() - initialP1Exp);

		// Jogador 2 agora e VIP por 3 dias
		assertTrue(player2.isVip());
		assertTrue(player2.vipExpiration() > System.currentTimeMillis());
	}

	@Test
	void testTargetSelectionWithoutCharName() {
		GameSession session = createGmSession();
		// Admin tem player1 selecionado no alvo
		when(session.targetObjectId()).thenReturn(player1.objectId());

		// //sethero 7 (sem digitar o nome, usando o alvo selecionado)
		boolean ok = handler.useAdminCommand("sethero", session, "7");
		assertTrue(ok);
		assertTrue(player1.isHero());
		assertTrue(player1.heroExpiration() > System.currentTimeMillis());

		// //removehero (usando o alvo selecionado)
		handler.useAdminCommand("removehero", session, "");
		assertFalse(player1.isHero());
	}

	@Test
	void testOfflineCharacterSupport() {
		GameSession session = createGmSession();

		PlayerCharacter offlineChar = new PlayerCharacter(2001, "off_acc", "OfflineDude", 80, 0, 0, 0, 0, 0, false, 0, 0, 0,
				1000, 1000, 1000, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 1000.0, 1000.0, 1000.0);
		when(characterRepository.findByName("OfflineDude")).thenReturn(Optional.of(offlineChar));

		// Personagem nao esta no GameWorld
		assertFalse(world.byName("OfflineDude").isPresent());

		// Executa //setvip OfflineDude 5
		boolean ok = handler.useAdminCommand("setvip", session, "OfflineDude 5");
		assertTrue(ok);

		// Executa //sethero OfflineDude 10
		ok = handler.useAdminCommand("sethero", session, "OfflineDude 10");
		assertTrue(ok);

		// Executa //setaio OfflineDude 30
		ok = handler.useAdminCommand("setaio", session, "OfflineDude 30");
		assertTrue(ok);
	}
}
