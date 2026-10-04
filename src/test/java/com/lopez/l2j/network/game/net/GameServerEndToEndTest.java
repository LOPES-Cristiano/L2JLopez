package com.lopez.l2j.network.game.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.config.ServerProperties;
import com.lopez.l2j.game.item.InMemoryItemRepository;
import com.lopez.l2j.game.item.TestItems;
import com.lopez.l2j.game.model.InMemoryCharacterRepository;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.service.CharacterService;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.crypt.GameCrypt;
import com.lopez.l2j.network.login.packet.PacketReader;
import com.lopez.l2j.network.login.packet.PacketWriter;
import com.lopez.l2j.network.login.service.Account;
import com.lopez.l2j.network.login.service.AccountStore;
import com.lopez.l2j.network.login.service.LoginAccountService;
import com.lopez.l2j.network.session.SessionKey;
import com.lopez.l2j.network.session.SessionKeyRegistry;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Sobe o GameServer real em porta efemera e percorre o fluxo do cliente Interlude por TCP. */
class GameServerEndToEndTest {

	static final int PROTOCOL = 746;

	InMemoryCharacterRepository repo;
	InMemoryItemRepository items;
	LoginAccountService accounts;
	SessionKeyRegistry registry;
	GameWorld world;
	GameServer server;

	@BeforeEach
	void start() {
		Map<String, Account> rows = new HashMap<>();
		AccountStore store = new AccountStore() {
			@Override
			public Optional<Account> find(String login) {
				return Optional.ofNullable(rows.get(login));
			}

			@Override
			public void create(String login, String passwordHash, String ip) {
				rows.put(login, new Account(login, passwordHash, 0, 0));
			}

			@Override
			public void touch(String login, String ip, int lastServerId) {
			}
		};
		accounts = new LoginAccountService(store, new ServerProperties.Login(true, 10, "127.0.0.1", 100, true));
		registry = new SessionKeyRegistry(accounts, true, Clock.systemUTC());
		repo = new InMemoryCharacterRepository();
		items = new InMemoryItemRepository();
		world = new GameWorld();
		var inventories = new InventoryService(TestItems.table(), items, ObjectIdFactory.sequential(0x20000000),
				5000);
		var characters = new CharacterService(repo, new CharTemplateTable(), inventories);
		server = new GameServer(0,
				new GameSession.Context(730, 746, registry, characters, inventories, world, "L2JLopez"));
		server.start();
	}

	@AfterEach
	void stop() {
		server.stop();
	}

	/** Cliente de teste: mesmo enquadramento e cifra do cliente real. */
	final class Client implements AutoCloseable {
		final Socket socket;
		final DataInputStream in;
		final OutputStream out;
		GameCrypt crypt;

		Client() throws IOException {
			socket = new Socket("127.0.0.1", server.port());
			socket.setSoTimeout(5000);
			in = new DataInputStream(socket.getInputStream());
			out = socket.getOutputStream();
		}

		void send(byte[] body) throws IOException {
			byte[] b = body.clone();
			if (crypt != null) {
				crypt.encrypt(b, 0, b.length);
			}
			int total = b.length + 2;
			out.write(new byte[] { (byte) total, (byte) (total >> 8) });
			out.write(b);
			out.flush();
		}

		byte[] read() throws IOException {
			int total = in.readUnsignedByte() | in.readUnsignedByte() << 8;
			byte[] b = new byte[total - 2];
			in.readFully(b);
			if (crypt != null) {
				crypt.decrypt(b, 0, b.length);
			}
			return b;
		}

		/** Le ate achar o opcode pedido (o servidor manda varios pacotes em sequencia). */
		byte[] readUntil(int opcode) throws IOException {
			for (int i = 0; i < 30; i++) {
				byte[] b = read();
				if ((b[0] & 0xff) == opcode) {
					return b;
				}
			}
			throw new AssertionError("opcode 0x" + Integer.toHexString(opcode) + " nao chegou");
		}

		/** ProtocolVersion em claro; KeyPacket em claro; a partir dai tudo cifrado. */
		void handshake() throws IOException {
			send(new PacketWriter().writeC(0x00).writeD(PROTOCOL).toByteArray());
			PacketReader key = new PacketReader(read());
			assertEquals(0x00, key.readC());
			assertEquals(0x01, key.readC(), "protocolo aceito");
			crypt = new GameCrypt(key.readB(16));
			crypt.encrypt(new byte[0], 0, 0); // liga a cifra do lado do cliente
		}

		void authLogin(String account, SessionKey k) throws IOException {
			send(new PacketWriter().writeC(0x08).writeS(account).writeD(k.playOk2()).writeD(k.playOk1())
					.writeD(k.loginOk1()).writeD(k.loginOk2()).toByteArray());
		}

		@Override
		public void close() throws IOException {
			socket.close();
		}
	}

	SessionKey loginAs(String account) {
		assertEquals(com.lopez.l2j.network.login.service.AuthResult.SUCCESS,
				accounts.authenticate(account, "secret", "127.0.0.1").status());
		var key = new SessionKey(11, 22, 33, 44);
		registry.register(account, key);
		return key;
	}

	static byte[] characterCreate(String name, int race, int sex, int classId) {
		PacketWriter w = new PacketWriter().writeC(0x0b).writeS(name).writeD(race).writeD(sex).writeD(classId);
		for (int i = 0; i < 6; i++) {
			w.writeD(0);
		}
		return w.writeD(1).writeD(1).writeD(1).toByteArray();
	}

	@Test
	void fullFlowFromHandshakeToWorldAndLogout() throws Exception {
		SessionKey key = loginAs("tester");
		try (Client c = new Client()) {
			c.handshake();
			c.authLogin("tester", key);

			PacketReader list = new PacketReader(c.read());
			assertEquals(0x13, list.readC());
			assertEquals(0, list.readD(), "conta nova sem personagens");

			c.send(new byte[] { 0x0e });
			PacketReader templates = new PacketReader(c.read());
			assertEquals(0x17, templates.readC());
			assertEquals(10, templates.readD());

			c.send(characterCreate("Hero", 0, 0, 0));
			assertEquals(0x19, c.read()[0]);
			PacketReader afterCreate = new PacketReader(c.read());
			assertEquals(0x13, afterCreate.readC());
			assertEquals(1, afterCreate.readD());
			assertEquals("Hero", afterCreate.readS());

			c.send(new PacketWriter().writeC(0x0d).writeD(0).writeH(0).writeD(0).writeD(0).writeD(0).toByteArray());
			assertEquals((byte) 0xf8, c.read()[0], "SSQInfo antes do CharSelected");
			PacketReader selected = new PacketReader(c.read());
			assertEquals(0x15, selected.readC());
			assertEquals("Hero", selected.readS());
			int objectId = selected.readD();

			c.send(new PacketWriter().writeC(0xd0).writeH(0x08).toByteArray());
			PacketReader manor = new PacketReader(c.read());
			assertEquals(0xfe, manor.readC());
			assertEquals(0x1b, manor.readH());

			c.send(new PacketWriter().writeC(0x03).writeB(new byte[104]).toByteArray());
			PacketReader ui = new PacketReader(c.readUntil(0x04));
			ui.readC();
			assertEquals(-71338, ui.readD());
			assertEquals(258271, ui.readD());
			assertEquals(-3104, ui.readD());
			ui.readD(); // heading
			assertEquals(objectId, ui.readD());
			assertEquals("Hero", ui.readS());
			c.readUntil(0x4a); // mensagem de boas-vindas
			assertEquals(1, world.online());
			assertEquals(Boolean.TRUE, repo.online.get(objectId));

			c.send(new PacketWriter().writeC(0x01).writeD(-71000).writeD(258000).writeD(-3104).writeD(-71338)
					.writeD(258271).writeD(-3104).writeD(1).toByteArray());
			PacketReader move = new PacketReader(c.read());
			assertEquals(0x01, move.readC());
			assertEquals(objectId, move.readD());
			assertEquals(-71000, move.readD());

			c.send(new PacketWriter().writeC(0x38).writeS("ola mundo").writeD(0).toByteArray());
			PacketReader say = new PacketReader(c.read());
			assertEquals(0x4a, say.readC());
			assertEquals(objectId, say.readD());
			assertEquals(0, say.readD());
			assertEquals("Hero", say.readS());
			assertEquals("ola mundo", say.readS());

			c.send(new byte[] { 0x09 });
			assertEquals(0x7e, c.read()[0]);
			assertThrows(EOFException.class, c::read);
		}
		waitFor(() -> !accounts.isOnline("tester"));
		assertEquals(0, world.online());
		assertEquals(Boolean.FALSE, repo.online.get(repo.rows.keySet().iterator().next()));
	}

	@Test
	void restartReturnsToCharacterSelection() throws Exception {
		SessionKey key = loginAs("tester");
		try (Client c = new Client()) {
			c.handshake();
			c.authLogin("tester", key);
			c.read();
			c.send(characterCreate("Alpha", 1, 1, 25));
			c.read();
			c.read();
			c.send(new PacketWriter().writeC(0x0d).writeD(0).writeH(0).writeD(0).writeD(0).writeD(0).toByteArray());
			c.readUntil(0x15);
			c.send(new PacketWriter().writeC(0x03).writeB(new byte[104]).toByteArray());
			c.readUntil(0x4a);

			c.send(new byte[] { 0x46 });
			PacketReader restart = new PacketReader(c.read());
			assertEquals(0x5f, restart.readC());
			assertEquals(1, restart.readD());
			PacketReader list = new PacketReader(c.read());
			assertEquals(0x13, list.readC());
			assertEquals(1, list.readD());
			assertEquals(0, world.online());
			assertTrue(accounts.isOnline("tester"), "restart mantem a conta logada");
		}
	}

	/** Le uma linha do ItemList/InventoryUpdate: devolve {objectId, itemId, count, equipped, bodyPart}. */
	static int[] readItem(PacketReader r) {
		r.readH(); // type1
		int objectId = r.readD();
		int itemId = r.readD();
		int count = r.readD();
		r.readH(); // type2
		r.readH(); // custom type1
		int equipped = r.readH();
		int bodyPart = r.readD();
		r.readH(); // enchant
		r.readH(); // custom type2
		r.readD(); // augmentation
		r.readD(); // mana
		return new int[] { objectId, itemId, count, equipped, bodyPart };
	}

	@Test
	void starterItemsAppearInLobbyAndCanBeEquipped() throws Exception {
		SessionKey key = loginAs("tester");
		try (Client c = new Client()) {
			c.handshake();
			c.authLogin("tester", key);
			c.read();
			c.send(characterCreate("Squire", 0, 0, 0));
			c.read();

			// CharSelectionInfo ja mostra a espada (paperdoll RHAND = 8o slot visivel)
			PacketReader list = new PacketReader(c.read());
			assertEquals(0x13, list.readC());
			assertEquals(1, list.readD());
			list.readS();
			list.readD();
			list.readS();
			for (int i = 0; i < 10; i++) { // sessionId, clan, 0, sex, race, baseClass, 1, 0, 0, 0
				list.readD();
			}
			list.readB(16); // hp, mp
			list.readD();
			list.readB(8); // exp
			for (int i = 0; i < 11; i++) {
				list.readD();
			}
			int[] objectIds = new int[17];
			int[] itemIds = new int[17];
			for (int i = 0; i < 17; i++) {
				objectIds[i] = list.readD();
			}
			for (int i = 0; i < 17; i++) {
				itemIds[i] = list.readD();
			}
			assertEquals(TestItems.SQUIRE_SWORD, itemIds[7], "RHAND");
			assertEquals(TestItems.SQUIRE_SHIRT, itemIds[10], "CHEST");
			assertEquals(TestItems.SQUIRE_PANTS, itemIds[11], "LEGS");
			assertTrue(objectIds[7] != 0);

			c.send(new PacketWriter().writeC(0x0d).writeD(0).writeH(0).writeD(0).writeD(0).writeD(0).toByteArray());
			c.readUntil(0x15);
			c.send(new PacketWriter().writeC(0x03).writeB(new byte[104]).toByteArray());
			PacketReader itemList = new PacketReader(c.readUntil(0x1b));
			itemList.readC();
			assertEquals(0, itemList.readH(), "EnterWorld nao abre a janela");
			int count = itemList.readH();
			assertEquals(6, count, "guia + dagger + shirt + pants + espada + adena");
			int daggerObject = 0;
			int equippedCount = 0;
			for (int i = 0; i < count; i++) {
				int[] item = readItem(itemList);
				equippedCount += item[3];
				if (item[1] == TestItems.DAGGER) {
					daggerObject = item[0];
				}
				if (item[1] == TestItems.ADENA) {
					assertEquals(5000, item[2]);
				}
			}
			assertEquals(3, equippedCount);
			c.readUntil(0x4a);

			// UseItem na dagger: mensagem, InventoryUpdate (dagger + espada) e UserInfo
			c.send(new PacketWriter().writeC(0x14).writeD(daggerObject).writeD(0).toByteArray());
			PacketReader msg = new PacketReader(c.read());
			assertEquals(0x64, msg.readC());
			assertEquals(49, msg.readD(), "S1_EQUIPPED");
			assertEquals(1, msg.readD());
			assertEquals(3, msg.readD());
			assertEquals(TestItems.DAGGER, msg.readD());
			PacketReader update = new PacketReader(c.read());
			assertEquals(0x27, update.readC());
			int changes = update.readH();
			assertEquals(2, changes);
			boolean daggerOn = false;
			for (int i = 0; i < changes; i++) {
				assertEquals(2, update.readH(), "modificado");
				int[] item = readItem(update);
				if (item[1] == TestItems.DAGGER) {
					daggerOn = item[3] == 1;
				} else {
					assertEquals(TestItems.SQUIRE_SWORD, item[1]);
					assertEquals(0, item[3]);
				}
			}
			assertTrue(daggerOn);
			assertEquals(0x04, c.read()[0], "UserInfo com o novo paperdoll");
			assertEquals("PAPERDOLL", items.rows.get(daggerObject).loc());

			// RequestUnEquipItem(SLOT_R_HAND)
			c.send(new PacketWriter().writeC(0x11).writeD(0x0080).toByteArray());
			msg = new PacketReader(c.read());
			msg.readC();
			assertEquals(417, msg.readD(), "S1_DISARMED");
			assertEquals(0x27, c.read()[0]);
			assertEquals(0x04, c.read()[0]);
			assertEquals("INVENTORY", items.rows.get(daggerObject).loc());

			// RequestItemList abre a janela
			c.send(new byte[] { 0x0f });
			PacketReader open = new PacketReader(c.read());
			assertEquals(0x1b, open.readC());
			assertEquals(1, open.readH());
		}
	}

	@Test
	void deletingCharacterRemovesItems() throws Exception {
		SessionKey key = loginAs("tester");
		try (Client c = new Client()) {
			c.handshake();
			c.authLogin("tester", key);
			c.read();
			c.send(characterCreate("Doomed", 0, 0, 0));
			c.read();
			c.read();
			assertEquals(6, items.rows.size());
			c.send(new PacketWriter().writeC(0x0c).writeD(0).toByteArray());
			assertEquals(0x23, c.read()[0]);
			c.read();
			assertTrue(items.rows.isEmpty());
		}
	}

	@Test
	void invalidCharacterNamesAreRejected() throws Exception {
		SessionKey key = loginAs("tester");
		try (Client c = new Client()) {
			c.handshake();
			c.authLogin("tester", key);
			c.read();
			c.send(characterCreate("ab", 0, 0, 0));
			PacketReader fail = new PacketReader(c.read());
			assertEquals(0x1a, fail.readC());
			assertEquals(0x03, fail.readD());

			c.send(characterCreate("bad name!", 0, 0, 0));
			fail = new PacketReader(c.read());
			fail.readC();
			assertEquals(0x04, fail.readD());

			c.send(characterCreate("Elfo", 0, 0, 18)); // classe elfica com raca humana
			fail = new PacketReader(c.read());
			fail.readC();
			assertEquals(0x00, fail.readD());
			assertTrue(repo.rows.isEmpty());
		}
	}

	@Test
	void wrongSessionKeyClosesConnection() throws Exception {
		loginAs("tester");
		try (Client c = new Client()) {
			c.handshake();
			c.authLogin("tester", new SessionKey(11, 22, 33, 999));
			assertThrows(EOFException.class, c::read);
		}
		assertTrue(accounts.isOnline("tester"), "falha de chave nao libera a conta de outra sessao");
	}

	@Test
	void unsupportedProtocolGetsNegativeKeyPacket() throws Exception {
		try (Client c = new Client()) {
			c.send(new PacketWriter().writeC(0x00).writeD(660).toByteArray());
			PacketReader key = new PacketReader(c.read());
			assertEquals(0x00, key.readC());
			assertEquals(0x00, key.readC());
			assertThrows(EOFException.class, c::read);
		}
	}

	@Test
	void unknownOpcodesAreIgnored() throws Exception {
		SessionKey key = loginAs("tester");
		try (Client c = new Client()) {
			c.handshake();
			c.authLogin("tester", key);
			c.read();
			c.send(new byte[] { (byte) 0x99, 1, 2, 3 });
			c.send(new byte[] { 0x0e });
			assertEquals(0x17, c.read()[0], "conexao segue viva apos opcode desconhecido");
		}
	}

	static void waitFor(java.util.function.BooleanSupplier condition) throws InterruptedException {
		for (int i = 0; i < 100 && !condition.getAsBoolean(); i++) {
			Thread.sleep(20);
		}
		assertTrue(condition.getAsBoolean(), "condicao nao satisfeita a tempo");
	}

	@Test
	void expiredPendingKeyReleasesAccount() {
		var clock = new java.util.concurrent.atomic.AtomicReference<>(java.time.Instant.parse("2026-01-01T00:00:00Z"));
		Clock fake = new Clock() {
			@Override
			public java.time.ZoneId getZone() {
				return java.time.ZoneOffset.UTC;
			}

			@Override
			public Clock withZone(java.time.ZoneId zone) {
				return this;
			}

			@Override
			public java.time.Instant instant() {
				return clock.get();
			}
		};
		var reg = new SessionKeyRegistry(accounts, true, fake);
		accounts.authenticate("late", "secret", "127.0.0.1");
		reg.register("late", new SessionKey(1, 2, 3, 4));
		reg.expirePending();
		assertTrue(accounts.isOnline("late"));
		clock.set(clock.get().plus(SessionKeyRegistry.PENDING_TTL).plusSeconds(1));
		reg.expirePending();
		assertFalse(accounts.isOnline("late"));
		assertFalse(reg.claim("late", new SessionKey(1, 2, 3, 4)));
	}

	@Test
	void npcVisibilityAndTargetingFlow() throws Exception {
		NpcTemplate t = new NpcTemplate(
				20001, 20001, "Wolf", true, "Predator", true,
				8.0, 16.0, 1, "male", "L2Monster",
				40, 50, 20, 10, 10, 5, 5, 250, 333,
				0, 0, 0, 50, 100, 0, false);
		NpcInstance npc = new NpcInstance(0x30000001, t, -71300, 258200, -3104, 0);
		world.addNpc(npc);

		SessionKey key = loginAs("npctester");
		try (Client c = new Client()) {
			c.handshake();
			c.authLogin("npctester", key);
			c.read(); // CharSelectionInfo (vazio)

			c.send(characterCreate("NpcHunter", 0, 0, 0));
			c.read(); // CharCreateOk
			c.read(); // CharSelectionInfo

			// Seleciona personagem
			c.send(new PacketWriter().writeC(0x0d).writeD(0).writeH(0).writeD(0).writeD(0).writeD(0).toByteArray());
			c.read(); // SSQInfo
			c.read(); // CharSelected

			// Manor list
			c.send(new PacketWriter().writeC(0xd0).writeH(0x08).toByteArray());
			c.read(); // ExSendManorList

			// EnterWorld (0x03)
			c.send(new PacketWriter().writeC(0x03).writeB(new byte[104]).toByteArray());

			// O jogador deve receber NpcInfo (0x16) do Wolf proximo!
			byte[] npcInfoBytes = c.readUntil(0x16);
			PacketReader r = new PacketReader(npcInfoBytes);
			assertEquals(0x16, r.readC());
			assertEquals(0x30000001, r.readD(), "objectId do NPC");
			assertEquals(20001 + 1000000, r.readD(), "idTemplate + 1000000");
			assertEquals(1, r.readD(), "isAttackable (L2Monster)");
			assertEquals(-71300, r.readD(), "x");
			assertEquals(258200, r.readD(), "y");
			assertEquals(-3104, r.readD(), "z");

			// Clica no NPC (Action 0x04)
			c.send(new PacketWriter().writeC(0x04).writeD(npc.objectId()).writeD(-71338).writeD(258271)
					.writeD(-3104).writeC(0).toByteArray());

			// Deve receber MyTargetSelected (0xa6)
			byte[] targetBytes = c.readUntil(0xa6);
			PacketReader tr = new PacketReader(targetBytes);
			assertEquals(0xa6, tr.readC());
			assertEquals(npc.objectId(), tr.readD());

			// E ValidateLocation (0x61)
			byte[] valBytes = c.readUntil(0x61);
			PacketReader vr = new PacketReader(valBytes);
			assertEquals(0x61, vr.readC());
			assertEquals(npc.objectId(), vr.readD());
		}
	}
}
