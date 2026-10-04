package com.lopez.l2j.network.login.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.config.ServerProperties;
import com.lopez.l2j.network.login.LoginSessionFactory;
import com.lopez.l2j.network.login.crypt.L2Blowfish;
import com.lopez.l2j.network.login.crypt.LegacyPasswordHasher;
import com.lopez.l2j.network.login.crypt.LoginChecksum;
import com.lopez.l2j.network.login.crypt.ScrambledRsaKeyPair;
import com.lopez.l2j.network.login.packet.PacketReader;
import com.lopez.l2j.network.login.packet.PacketWriter;
import com.lopez.l2j.network.login.service.Account;
import com.lopez.l2j.network.login.service.AccountStore;
import com.lopez.l2j.network.login.packet.LoginServerPacket.ServerEntry;
import com.lopez.l2j.network.login.service.GameServerDirectory;
import com.lopez.l2j.network.login.service.LoginAccountService;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Cipher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Sobe o LoginServer real em porta efemera e conversa com ele por TCP como o cliente do jogo faria. */
class LoginServerEndToEndTest {

	private static final byte[] STATIC_KEY = {
			(byte) 0x6b, (byte) 0x60, (byte) 0xcb, (byte) 0x5b, (byte) 0x82, (byte) 0xce, (byte) 0x90, (byte) 0xb1,
			(byte) 0xcc, (byte) 0x2b, (byte) 0x6c, (byte) 0x55, (byte) 0x6c, (byte) 0x6c, (byte) 0x6c, (byte) 0x6c };

	final Map<String, Account> accounts = new HashMap<>();
	LoginServer server;

	@BeforeEach
	void start() {
		accounts.put("alice", new Account("alice", LegacyPasswordHasher.hash("pw"), 0, 0));
		var login = new ServerProperties.Login(false, 1, "127.0.0.1", 10, true);
		var props = new ServerProperties("t", null,
				new ServerProperties.Network(2106, 7777, 9014, 730, 746), login, null);
		var service = new LoginAccountService(new AccountStore() {
			public Optional<Account> find(String l) {
				return Optional.ofNullable(accounts.get(l));
			}

			public void create(String l, String h, String ip) {
			}

			public void touch(String l, String ip, int s) {
			}
		}, login);
		GameServerDirectory directory = () -> java.util.List.of(new ServerEntry(1, "127.0.0.1", 7777, true, 0, 10, true, false, false, false));
		var factory = new LoginSessionFactory(props, service, directory);
		server = new LoginServer(0, factory);
		server.start();
	}

	@AfterEach
	void stop() {
		server.stop();
	}

	/** Cliente de teste: implementa o lado do cliente do protocolo. */
	static final class Client implements AutoCloseable {
		final Socket socket;
		final DataInputStream in;
		final OutputStream out;
		L2Blowfish session;
		int sessionId;
		BigInteger modulus;

		Client(int port) throws IOException {
			socket = new Socket(InetAddress.getLoopbackAddress(), port);
			socket.setSoTimeout(5000);
			in = new DataInputStream(socket.getInputStream());
			out = socket.getOutputStream();
		}

		byte[] readFrame() throws IOException {
			int total = in.readUnsignedByte() | (in.readUnsignedByte() << 8);
			byte[] payload = new byte[total - 2];
			in.readFully(payload);
			return payload;
		}

		/** Le o Init: Blowfish estatico + desfaz o XOR pass; extrai sessionId, modulo RSA e chave de sessao. */
		void handshake() throws IOException {
			byte[] p = readFrame();
			new L2Blowfish(STATIC_KEY).decrypt(p, 0, p.length);
			LoginChecksum.decXorPass(p, 0, p.length);
			PacketReader r = new PacketReader(p);
			assertEquals(0x00, r.readC());
			sessionId = r.readD();
			assertEquals(0x0000c621, r.readD());
			byte[] scrambled = r.readB(128);
			modulus = new BigInteger(1, ScrambledRsaKeyPair.unscramble(scrambled));
			r.readD();
			r.readD();
			r.readD();
			r.readD();
			session = new L2Blowfish(r.readB(16));
		}

		void send(byte[] body) throws IOException {
			int size = ((body.length + 4 + 7) / 8) * 8;
			byte[] buf = Arrays.copyOf(body, size);
			LoginChecksum.append(buf, 0, size);
			session.encrypt(buf, 0, size);
			sendRaw(buf);
		}

		void sendRaw(byte[] payload) throws IOException {
			int total = payload.length + 2;
			out.write(new byte[] { (byte) total, (byte) (total >> 8) });
			out.write(payload);
			out.flush();
		}

		/** Le e decifra um pacote do servidor; verifica o checksum. */
		byte[] receive() throws IOException {
			byte[] p = readFrame();
			session.decrypt(p, 0, p.length);
			assertTrue(LoginChecksum.verify(p, 0, p.length), "checksum do servidor invalido");
			return p;
		}

		byte[] rsaBlock(String user, String pass) throws Exception {
			byte[] plain = new byte[128];
			System.arraycopy(user.getBytes(StandardCharsets.ISO_8859_1), 0, plain, 0x5E, user.length());
			System.arraycopy(pass.getBytes(StandardCharsets.ISO_8859_1), 0, plain, 0x6C, pass.length());
			Cipher c = Cipher.getInstance("RSA/ECB/NoPadding");
			c.init(Cipher.ENCRYPT_MODE, KeyFactory.getInstance("RSA")
					.generatePublic(new RSAPublicKeySpec(modulus, BigInteger.valueOf(65537))));
			return c.doFinal(plain);
		}

		/** true se o servidor encerrou a conexao (EOF/reset). */
		boolean closedByServer() {
			try {
				return in.read() == -1;
			} catch (IOException e) {
				return true;
			}
		}

		@Override
		public void close() throws IOException {
			socket.close();
		}
	}

	@Test
	void fullLoginFlowOverTcp() throws Exception {
		try (Client c = new Client(server.port())) {
			c.handshake();

			c.send(new PacketWriter().writeC(0x07).writeD(c.sessionId).writeD(0).writeD(0).writeD(0).writeD(0)
					.toByteArray());
			PacketReader gg = new PacketReader(c.receive());
			assertEquals(0x0b, gg.readC());
			assertEquals(c.sessionId, gg.readD());

			c.send(new PacketWriter().writeC(0x00).writeB(c.rsaBlock("alice", "pw")).toByteArray());
			PacketReader ok = new PacketReader(c.receive());
			assertEquals(0x03, ok.readC());
			int k1 = ok.readD();
			int k2 = ok.readD();

			c.send(new PacketWriter().writeC(0x05).writeD(k1).writeD(k2).toByteArray());
			byte[] list = c.receive();
			assertEquals(0x04, list[0]);
			assertEquals(1, list[1]); // um servidor

			c.send(new PacketWriter().writeC(0x02).writeD(k1).writeD(k2).writeC(1).toByteArray());
			PacketReader play = new PacketReader(c.receive());
			assertEquals(0x07, play.readC());
		}
	}

	@Test
	void wrongPasswordGetsAuthFailAndConnectionIsClosed() throws Exception {
		try (Client c = new Client(server.port())) {
			c.handshake();
			c.send(new PacketWriter().writeC(0x07).writeD(c.sessionId).writeD(0).writeD(0).writeD(0).writeD(0)
					.toByteArray());
			c.receive();
			c.send(new PacketWriter().writeC(0x00).writeB(c.rsaBlock("alice", "bad")).toByteArray());
			byte[] fail = c.receive();
			assertEquals(0x01, fail[0]);
			assertEquals(0x03, fail[1]);
			assertTrue(c.closedByServer());
		}
	}

	@Test
	void tamperedPacketClosesConnectionWithoutReply() throws Exception {
		try (Client c = new Client(server.port())) {
			c.handshake();
			byte[] garbage = new byte[32];
			new java.security.SecureRandom().nextBytes(garbage); // aleatorio: blocos iguais fechariam o checksum por acaso
			c.sendRaw(garbage);
			assertTrue(c.closedByServer());
		}
	}

	@Test
	void oversizedOrMisalignedFramesAreRejected() throws Exception {
		try (Client c = new Client(server.port())) {
			c.handshake();
			c.out.write(new byte[] { (byte) 0xff, (byte) 0xff }); // 65535 bytes declarados
			c.out.flush();
			assertTrue(c.closedByServer());
		}
		try (Client c = new Client(server.port())) {
			c.handshake();
			c.sendRaw(new byte[13]); // nao multiplo de 8
			assertTrue(c.closedByServer());
		}
	}

	@Test
	void opcodeInWrongStateClosesConnection() throws Exception {
		try (Client c = new Client(server.port())) {
			c.handshake();
			// RequestAuthLogin antes do GameGuard
			c.send(new PacketWriter().writeC(0x00).writeB(c.rsaBlock("alice", "pw")).toByteArray());
			byte[] fail = c.receive();
			assertEquals(0x01, fail[0]);
			assertEquals(0x04, fail[1]); // ACCESS_FAILED
			assertTrue(c.closedByServer());
		}
	}

	@Test
	void disconnectingMidSessionReleasesTheAccount() throws Exception {
		try (Client c = new Client(server.port())) {
			c.handshake();
			c.send(new PacketWriter().writeC(0x07).writeD(c.sessionId).writeD(0).writeD(0).writeD(0).writeD(0)
					.toByteArray());
			c.receive();
			c.send(new PacketWriter().writeC(0x00).writeB(c.rsaBlock("alice", "pw")).toByteArray());
			assertEquals(0x03, c.receive()[0]);
		}
		// o servidor detecta o fechamento e libera "alice": um segundo login deve funcionar
		Thread.sleep(300);
		try (Client c2 = new Client(server.port())) {
			c2.handshake();
			c2.send(new PacketWriter().writeC(0x07).writeD(c2.sessionId).writeD(0).writeD(0).writeD(0).writeD(0)
					.toByteArray());
			c2.receive();
			c2.send(new PacketWriter().writeC(0x00).writeB(c2.rsaBlock("alice", "pw")).toByteArray());
			assertEquals(0x03, c2.receive()[0], "conta deveria ter sido liberada");
		}
	}

	@Test
	void serverStopsAcceptingAfterStop() throws Exception {
		int port = server.port();
		server.stop();
		assertFalse(server.isRunning());
		boolean refused;
		try (Socket s = new Socket(InetAddress.getLoopbackAddress(), port)) {
			s.setSoTimeout(1000);
			refused = s.getInputStream().read() == -1;
		} catch (IOException e) {
			refused = true;
		}
		assertTrue(refused);
	}
}
