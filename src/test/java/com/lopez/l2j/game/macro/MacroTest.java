package com.lopez.l2j.game.macro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestDeleteMacro;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestMakeMacro;
import com.lopez.l2j.network.game.packet.GameServerPacket.SendMacroList;
import com.lopez.l2j.network.login.packet.PacketWriter;

class MacroTest {

	@Test
	void testMacroCommandsSerializationAndParsing() {
		List<MacroCmd> original = List.of(
				new MacroCmd(1, MacroCmd.TYPE_SKILL, 1068, 1, "Might"),
				new MacroCmd(2, MacroCmd.TYPE_ACTION, 0, 0, "/target Crist"),
				new MacroCmd(3, MacroCmd.TYPE_SHORTCUT, 1, 0, "Hello, world!"));

		String serialized = JdbcMacroRepository.serializeCommands(original);
		List<MacroCmd> parsed = JdbcMacroRepository.parseCommands(serialized);

		assertEquals(3, parsed.size());
		assertEquals(MacroCmd.TYPE_SKILL, parsed.get(0).type());
		assertEquals(1068, parsed.get(0).d1());
		assertEquals(1, parsed.get(0).d2());
		assertEquals("Might", parsed.get(0).cmd());

		assertEquals(MacroCmd.TYPE_ACTION, parsed.get(1).type());
		assertEquals("/target Crist", parsed.get(1).cmd());

		assertEquals(MacroCmd.TYPE_SHORTCUT, parsed.get(2).type());
		assertEquals("Hello, world!", parsed.get(2).cmd());
	}

	@Test
	void testSendMacroListEmptyEncode() {
		SendMacroList packet = new SendMacroList(1, 0, null);
		byte[] encoded = packet.encode();

		assertNotNull(encoded);
		// Opcode 0xE8, revision (4 bytes), byte 0, count 0, hasMacro 0 -> 1+4+1+1+1 = 8 bytes
		assertEquals((byte) 0xe8, encoded[0]);
		assertEquals(0, encoded[5]); // 0 byte
		assertEquals(0, encoded[6]); // count 0
		assertEquals(0, encoded[7]); // hasMacro 0
	}

	@Test
	void testSendMacroListWithDataEncode() {
		Macro m = new Macro(1000, 2, "TestMacro", "Desc", "TM", List.of(
				new MacroCmd(1, MacroCmd.TYPE_SKILL, 123, 1, "Heal")));
		SendMacroList packet = new SendMacroList(1, 1, m);
		byte[] encoded = packet.encode();

		assertNotNull(encoded);
		assertEquals((byte) 0xe8, encoded[0]);
		assertEquals(1, encoded[6]); // count 1
		assertEquals(1, encoded[7]); // hasMacro 1
	}

	@Test
	void testDecodeRequestMakeMacro() {
		PacketWriter w = new PacketWriter();
		w.writeC(0xc1); // opcode
		w.writeD(0); // new macro id = 0
		w.writeS("MyMacro");
		w.writeS("Macro description");
		w.writeS("MM");
		w.writeC(3); // icon
		w.writeC(2); // 2 commands
		// cmd 1
		w.writeC(1); // entry
		w.writeC(1); // type
		w.writeD(1068); // d1
		w.writeC(0); // d2
		w.writeS("Might");
		// cmd 2
		w.writeC(2); // entry
		w.writeC(3); // type
		w.writeD(0); // d1
		w.writeC(0); // d2
		w.writeS("/target Self");

		byte[] bytes = w.toByteArray();
		var opt = GameClientPacket.decode(GameClientPacket.State.IN_GAME, bytes);
		assertTrue(opt.isPresent());
		assertTrue(opt.get() instanceof RequestMakeMacro);

		RequestMakeMacro rmm = (RequestMakeMacro) opt.get();
		assertEquals("MyMacro", rmm.macro().name());
		assertEquals("Macro description", rmm.macro().descr());
		assertEquals("MM", rmm.macro().acronym());
		assertEquals(3, rmm.macro().icon());
		assertEquals(2, rmm.macro().commands().size());
		assertEquals("Might", rmm.macro().commands().get(0).cmd());
		assertEquals("/target Self", rmm.macro().commands().get(1).cmd());
	}

	@Test
	void testDecodeRequestDeleteMacro() {
		PacketWriter w = new PacketWriter();
		w.writeC(0xc2); // opcode
		w.writeD(1005); // macro id to delete

		byte[] bytes = w.toByteArray();
		var opt = GameClientPacket.decode(GameClientPacket.State.IN_GAME, bytes);
		assertTrue(opt.isPresent());
		assertTrue(opt.get() instanceof RequestDeleteMacro);

		RequestDeleteMacro rdm = (RequestDeleteMacro) opt.get();
		assertEquals(1005, rdm.id());
	}

	static class InMemoryMacroRepository implements MacroRepository {
		final List<Macro> list = new ArrayList<>();

		@Override
		public List<Macro> findByCharId(int charId) {
			return List.copyOf(list);
		}

		@Override
		public void save(int charId, Macro macro) {
			list.removeIf(m -> m.id() == macro.id());
			list.add(macro);
		}

		@Override
		public void delete(int charId, int macroId) {
			list.removeIf(m -> m.id() == macroId);
		}

		@Override
		public void deleteAll(int charId) {
			list.clear();
		}
	}

	@Test
	void testGameSessionMacroLifecycle() throws Exception {
		var macroRepo = new InMemoryMacroRepository();
		var ctx = new GameSession.Context(macroRepo);

		List<com.lopez.l2j.network.game.packet.GameServerPacket> sent = new ArrayList<>();
		com.lopez.l2j.network.game.GameSession session = new com.lopez.l2j.network.game.GameSession(ctx, new byte[8], "127.0.0.1", sent::add);

		var player = new com.lopez.l2j.game.model.PlayerCharacter(1001, "Hero", "Hero", 1, 0, 0, 0, 0, 0, false, 0, 0, 0,
				200, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 200.0, 100.0, 100.0);

		setField(session, "state", GameClientPacket.State.IN_GAME);
		setField(session, "active", player);
		setField(session, "inWorld", true);

		// 1. Make Macro
		PacketWriter w = new PacketWriter();
		w.writeC(0xc1);
		w.writeD(0);
		w.writeS("AttackMacro");
		w.writeS("Desc");
		w.writeS("ATK");
		w.writeC(1);
		w.writeC(1);
		w.writeC(1);
		w.writeC(MacroCmd.TYPE_ACTION);
		w.writeD(2);
		w.writeC(0);
		w.writeS("/attack");

		session.handle(w.toByteArray());

		assertEquals(1, macroRepo.list.size());
		Macro saved = macroRepo.list.get(0);
		assertEquals("AttackMacro", saved.name());
		assertTrue(saved.id() >= 1000);

		// SendMacroList foi enviado
		assertTrue(sent.stream().anyMatch(p -> p instanceof SendMacroList sml && sml.count() == 1));

		// 2. Delete Macro
		sent.clear();
		PacketWriter wDel = new PacketWriter();
		wDel.writeC(0xc2);
		wDel.writeD(saved.id());

		session.handle(wDel.toByteArray());

		assertEquals(0, macroRepo.list.size());
		assertTrue(sent.stream().anyMatch(p -> p instanceof SendMacroList sml && sml.count() == 0));
	}

	private static void setField(Object target, String name, Object val) throws Exception {
		var f = target.getClass().getDeclaredField(name);
		f.setAccessible(true);
		f.set(target, val);
	}
}
