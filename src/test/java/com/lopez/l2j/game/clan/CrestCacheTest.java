package com.lopez.l2j.game.clan;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.game.model.ObjectIdFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CrestCacheTest {

	@Test
	void savesAndRetrievesPledgeCrest(@TempDir Path tempDir) {
		CrestCache cache = new CrestCache(tempDir, ObjectIdFactory.sequential(5000));
		byte[] fakeBmp = new byte[] { 0x42, 0x4D, 0x10, 0x00, 0x00, 0x00 };

		int crestId = cache.savePledgeCrest(fakeBmp);
		assertTrue(crestId > 0);

		byte[] retrieved = cache.getPledgeCrest(crestId);
		assertNotNull(retrieved);
		assertArrayEquals(fakeBmp, retrieved);

		cache.removePledgeCrest(crestId);
		assertNull(cache.getPledgeCrest(crestId));
	}

	@Test
	void savesAndRetrievesAllyCrest(@TempDir Path tempDir) {
		CrestCache cache = new CrestCache(tempDir, ObjectIdFactory.sequential(6000));
		byte[] fakeBmp = new byte[] { 0x42, 0x4D, 0x20, 0x00, 0x00, 0x00 };

		int crestId = cache.saveAllyCrest(fakeBmp);
		assertTrue(crestId > 0);

		byte[] retrieved = cache.getAllyCrest(crestId);
		assertNotNull(retrieved);
		assertArrayEquals(fakeBmp, retrieved);
	}
}
