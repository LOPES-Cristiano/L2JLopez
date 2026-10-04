package com.lopez.l2j.network.login.packet;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.network.login.packet.LoginClientPacket.AuthGameGuard;
import com.lopez.l2j.network.login.packet.LoginClientPacket.RequestAuthLogin;
import com.lopez.l2j.network.login.packet.LoginClientPacket.RequestServerList;
import com.lopez.l2j.network.login.packet.LoginClientPacket.RequestServerLogin;
import com.lopez.l2j.network.login.packet.LoginClientPacket.State;
import com.lopez.l2j.network.login.packet.LoginServerPacket.AuthFail;
import com.lopez.l2j.network.login.packet.LoginServerPacket.AuthFailReason;
import com.lopez.l2j.network.login.packet.LoginServerPacket.AuthOk;
import com.lopez.l2j.network.login.packet.LoginServerPacket.GgAuth;
import com.lopez.l2j.network.login.packet.LoginServerPacket.Init;
import com.lopez.l2j.network.login.packet.LoginServerPacket.PlayFail;
import com.lopez.l2j.network.login.packet.LoginServerPacket.PlayFailReason;
import com.lopez.l2j.network.login.packet.LoginServerPacket.PlayOk;
import com.lopez.l2j.network.login.packet.LoginServerPacket.ServerEntry;
import com.lopez.l2j.network.login.packet.LoginServerPacket.ServerList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PacketsTest {

	@Test
	void writerAndReaderAreLittleEndianAndSymmetric() {
		byte[] b = new PacketWriter().writeC(0xAB).writeD(0x01020304).writeH(0x0506).toByteArray();
		assertArrayEquals(new byte[] { (byte) 0xAB, 4, 3, 2, 1, 6, 5 }, b);
		PacketReader r = new PacketReader(b);
		assertEquals(0xAB, r.readC());
		assertEquals(0x01020304, r.readD());
		assertEquals(2, r.remaining());
	}

	@Test
	void readerNeverReadsPastTheEnd() {
		PacketReader r = new PacketReader(new byte[3]);
		assertThrows(IllegalArgumentException.class, r::readD);
		assertThrows(IllegalArgumentException.class, () -> r.readB(4));
	}

	@Test
	void simpleServerPacketsHaveTheExactLegacyLayout() {
		assertArrayEquals(new byte[] { 0x01, 0x03 }, new AuthFail(AuthFailReason.USER_OR_PASS_WRONG).encode());
		assertArrayEquals(new byte[] { 0x06, 0x0f }, new PlayFail(PlayFailReason.TOO_MANY_PLAYERS).encode());
		assertArrayEquals(new byte[] { 0x07, 1, 0, 0, 0, 2, 0, 0, 0 }, new PlayOk(1, 2).encode());
		byte[] gg = new GgAuth(0x11223344).encode();
		assertEquals(21, gg.length);
		assertEquals(0x0b, gg[0]);
		assertEquals(0x44, gg[1]);
		byte[] ok = new AuthOk(5, 6).encode();
		assertEquals(49, ok.length);
		assertEquals(0x03, ok[0]);
	}

	@Test
	void initLayoutMatchesLegacy() {
		byte[] init = new Init(7, new byte[128], new byte[16]).encode();
		// C + D + D + 128 + 4*D + 16 + C
		assertEquals(1 + 4 + 4 + 128 + 16 + 16 + 1, init.length);
		assertEquals(0x00, init[0]);
		assertEquals(0x21, init[5] & 0xff); // 0x0000c621 little-endian
		assertEquals(0xc6, init[6] & 0xff);
	}

	@Test
	void initRejectsWrongKeySizesAndCopiesInput() {
		assertThrows(IllegalArgumentException.class, () -> new Init(1, new byte[127], new byte[16]));
		byte[] key = new byte[16];
		Init init = new Init(1, new byte[128], key);
		key[0] = 9;
		assertEquals(0, init.encode()[1 + 4 + 4 + 128 + 16]);
	}

	@Test
	void serverListEncodesEntriesAndFallsBackWhenLastServerIsDown() {
		ServerEntry up = new ServerEntry(1, "10.1.2.3", 7777, true, 5, 100, true, false, false, false);
		ServerEntry down = new ServerEntry(2, "10.1.2.4", 7778, false, 0, 50, false, true, true, true);
		byte[] list = new ServerList(List.of(up, down), 2).encode();
		assertEquals(0x04, list[0]);
		assertEquals(2, list[1]);
		assertEquals(0, list[2]); // ultimo servidor esta fora do ar
		assertEquals(1, list[3]);
		assertEquals(10, list[4]);
		assertEquals(2, list[6]); // 10.1.2.3
		assertEquals(3 + 21 * 2, list.length);
		assertEquals(1, new ServerList(List.of(up), 1).encode()[2]);
	}

	@Test
	void decodesOnlyOpcodesValidForTheState() {
		byte[] gg = new PacketWriter().writeC(0x07).writeD(42).writeD(1).writeD(2).writeD(3).writeD(4).toByteArray();
		assertEquals(new AuthGameGuard(42), LoginClientPacket.decode(State.CONNECTED, gg).orElseThrow());
		assertTrue(LoginClientPacket.decode(State.AUTHED_GG, gg).isEmpty());

		byte[] auth = new byte[1 + 128];
		var decoded = LoginClientPacket.decode(State.AUTHED_GG, auth).orElseThrow();
		assertTrue(decoded instanceof RequestAuthLogin);
		assertTrue(LoginClientPacket.decode(State.CONNECTED, auth).isEmpty());

		byte[] list = new PacketWriter().writeC(0x05).writeD(1).writeD(2).toByteArray();
		assertEquals(new RequestServerList(1, 2), LoginClientPacket.decode(State.AUTHED_LOGIN, list).orElseThrow());
		byte[] login = new PacketWriter().writeC(0x02).writeD(1).writeD(2).writeC(3).toByteArray();
		assertEquals(new RequestServerLogin(1, 2, 3), LoginClientPacket.decode(State.AUTHED_LOGIN, login).orElseThrow());
	}

	@Test
	void truncatedOrEmptyPacketsAreRejectedNotThrown() {
		assertTrue(LoginClientPacket.decode(State.CONNECTED, new byte[] { 0x07, 1, 2 }).isEmpty());
		assertTrue(LoginClientPacket.decode(State.AUTHED_GG, new byte[] { 0x00, 1 }).isEmpty());
		assertTrue(LoginClientPacket.decode(State.AUTHED_LOGIN, new byte[] { 0x02, 1 }).isEmpty());
		assertTrue(LoginClientPacket.decode(State.CONNECTED, new byte[0]).isEmpty());
		assertTrue(LoginClientPacket.decode(State.CONNECTED, null).isEmpty());
		assertTrue(LoginClientPacket.decode(State.AUTHED_LOGIN, new byte[] { 0x7f }).isEmpty());
	}
}