package com.lopez.l2j.game.announcements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnnouncementsTest {

	@Test
	void managesAnnouncementsLifecycle() {
		Announcements ann = new Announcements();
		assertFalse(ann.list().isEmpty());

		int initialSize = ann.list().size();
		ann.add("Server event in 10 minutes!");
		assertEquals(initialSize + 1, ann.list().size());

		List<String> received = new ArrayList<>();
		ann.showToPlayer(packet -> {
			if (packet instanceof com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay say) {
				received.add(say.text());
			}
		});

		assertTrue(received.contains("Server event in 10 minutes!"));
		assertTrue(ann.delete(initialSize));
		assertEquals(initialSize, ann.list().size());
	}
}
