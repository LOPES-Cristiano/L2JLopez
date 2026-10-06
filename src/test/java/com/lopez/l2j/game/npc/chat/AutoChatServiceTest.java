package com.lopez.l2j.game.npc.chat;

import static org.junit.jupiter.api.Assertions.*;

import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AutoChatServiceTest {

	private AutoChatTable table;
	private GameWorld world;
	private AutoChatService service;

	private static class MockOnlinePlayer implements GameWorld.OnlinePlayer {
		private final int objectId;
		private final String name;
		private final int x;
		private final int y;
		private final int z;
		final List<GameServerPacket> packets = new ArrayList<>();

		MockOnlinePlayer(int objectId, String name, int x, int y, int z) {
			this.objectId = objectId;
			this.name = name;
			this.x = x;
			this.y = y;
			this.z = z;
		}

		@Override public int objectId() { return objectId; }
		@Override public String name() { return name; }
		@Override public int x() { return x; }
		@Override public int y() { return y; }
		@Override public int z() { return z; }
		@Override public void send(GameServerPacket packet) { packets.add(packet); }
	}

	@BeforeEach
	void setUp() {
		table = new AutoChatTable();
		world = new GameWorld();
		service = new AutoChatService(table, world);
	}

	@Test
	@DisplayName("Carregamento do XML real auto_chat.xml")
	void testLoadRealAutoChatXml() {
		AutoChatTable realTable = new AutoChatTable();
		realTable.load();
		assertTrue(realTable.size() > 0, "Deve carregar chats de auto_chat.xml");

		var preacherOpt = realTable.getByNpcId(31093);
		assertTrue(preacherOpt.isPresent());
		assertTrue(preacherOpt.get().chatTexts().size() > 0);
		assertTrue(preacherOpt.get().chatTexts().get(0).contains("%player_cabal_loser%"));
	}

	@Test
	@DisplayName("NPC profere frases em intervalos e substitui marcadores de jogador")
	void testAutoChatExecution() {
		NpcTemplate tpl = NpcTemplate.fallback(31093, "Preacher of Doom", "L2Npc");
		NpcInstance npc = new NpcInstance(100, tpl, 500, 500, 0, 0);
		world.addNpc(npc);

		MockOnlinePlayer player = new MockOnlinePlayer(1, "Arthur", 550, 550, 0);
		world.add(player);

		// Registra chat customizado com 2 frases e intervalo de 5000ms
		service.registerCustomChat(npc, 5000L, List.of(
				"%player_cabal_loser%! The doom is near!",
				"All is lost!"
		));
		assertEquals(1, service.activeSessionsCount());

		long time = 1000L;
		// Primeira verificacao (antes de atingir o intervalo de 5000ms)
		service.step(time);
		assertEquals(0, player.packets.size());

		// Avanca 5000ms (tempo = 6000ms) -> deve proferir a frase 1 com nome substituido
		time = 6000L;
		service.step(time);
		assertEquals(1, player.packets.size());
		assertTrue(player.packets.get(0) instanceof CreatureSay cs
				&& cs.text().equals("Arthur! The doom is near!"));

		// Avanca mais 5000ms (tempo = 11000ms) -> deve proferir a frase 2
		time = 11000L;
		service.step(time);
		assertEquals(2, player.packets.size());
		assertTrue(player.packets.get(1) instanceof CreatureSay cs
				&& cs.text().equals("All is lost!"));

		// Descadastra NPC
		service.unregisterNpc(100);
		assertEquals(0, service.activeSessionsCount());
	}
}
