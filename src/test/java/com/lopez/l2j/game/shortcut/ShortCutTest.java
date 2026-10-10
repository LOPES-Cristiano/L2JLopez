package com.lopez.l2j.game.shortcut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.network.game.packet.GameServerPacket.MagicSkillUse;
import com.lopez.l2j.network.game.packet.GameServerPacket.ShortCutInit;
import com.lopez.l2j.network.game.packet.GameServerPacket.ShortCutRegister;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShortCutTest {

	private InMemoryShortCutRepository repo;

	static class InMemoryShortCutRepository implements ShortCutRepository {
		private final List<ShortCut> list = new ArrayList<>();

		@Override
		public List<ShortCut> findByCharId(int charId, int classIndex) {
			return List.copyOf(list);
		}

		@Override
		public void save(int charId, int classIndex, ShortCut shortcut) {
			list.removeIf(s -> s.slot() == shortcut.slot() && s.page() == shortcut.page());
			list.add(shortcut);
		}

		@Override
		public void delete(int charId, int classIndex, int slot, int page) {
			list.removeIf(s -> s.slot() == slot && s.page() == page);
		}

		@Override
		public void deleteByTypeAndId(int charId, int type, int shortcutId) {
			list.removeIf(s -> s.type() == type && s.id() == shortcutId);
		}

		@Override
		public void deleteAll(int charId, int classIndex) {
			list.clear();
		}
	}

	@BeforeEach
	void setUp() {
		repo = new InMemoryShortCutRepository();
	}

	@Test
	void registersAndRetrievesShortCut() {
		// F1 na pagina 0 (slot 0, page 0) para o item 1234
		ShortCut sc = new ShortCut(0, 0, ShortCut.TYPE_ITEM, 1234, -1, 1);
		repo.save(1, 0, sc);

		var list = repo.findByCharId(1, 0);
		assertEquals(1, list.size());
		assertEquals(0, list.get(0).slot());
		assertEquals(0, list.get(0).page());
		assertEquals(ShortCut.TYPE_ITEM, list.get(0).type());
		assertEquals(1234, list.get(0).id());

		// Substitui mesmo slot
		ShortCut sc2 = new ShortCut(0, 0, ShortCut.TYPE_SKILL, 3, 1, 1);
		repo.save(1, 0, sc2);
		list = repo.findByCharId(1, 0);
		assertEquals(1, list.size());
		assertEquals(ShortCut.TYPE_SKILL, list.get(0).type());
		assertEquals(3, list.get(0).id());

		// Deleta
		repo.delete(1, 0, 0, 0);
		assertTrue(repo.findByCharId(1, 0).isEmpty());
	}

	@Test
	void shortCutPacketsEncodeCorrectly() {
		ShortCut sc = new ShortCut(1, 0, ShortCut.TYPE_ITEM, 57, -1, 1);
		byte[] regBytes = new ShortCutRegister(sc).encode();
		assertEquals(0x44, regBytes[0]);
		assertTrue(regBytes.length > 5);

		byte[] initBytes = new ShortCutInit(List.of(sc)).encode();
		assertEquals(0x45, initBytes[0]);
		assertTrue(initBytes.length > 5);
	}

	@Test
	void magicSkillUsePacketEncodesCorrectly() {
		MagicSkillUse msu = new MagicSkillUse(0x10000001, 0x20000001, 3, 1, 1500, 2000, 100, 200, 300, 150, 250, 300);
		byte[] bytes = msu.encode();
		assertEquals(0x48, bytes[0]);
		assertTrue(bytes.length >= 49);
	}
}
