package com.lopez.l2j.game.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.config.Config;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WordFilterTableTest {

	@Test
	void testWordFilterReplacesCensoredWords(@TempDir Path tempDir) throws Exception {
		Path filterFile = tempDir.resolve("sayfilter.txt");
		Files.writeString(filterFile, "badword\ncurse\nspam\\d+\n# comment\n");

		var table = new WordFilterTable(filterFile.toString());
		table.load();

		assertEquals(3, table.size());

		String rep = Config.getString("ChatFilterChars", "...");

		// Test direct replacement
		String clean = table.filter("hello badword and another CURSE here");
		assertEquals("hello " + rep + " and another " + rep + " here", clean);

		String regexClean = table.filter("test spam123 test");
		assertEquals("test " + rep + " test", regexClean);
	}

	@Test
	void testDynamicAddAndClear() {
		String rep = Config.getString("ChatFilterChars", "...");
		var table = new WordFilterTable("nonexistent.txt");
		assertEquals(0, table.size());

		table.addPattern("prohibited");
		assertEquals(1, table.size());

		String res = table.filter("this is prohibited content");
		assertEquals("this is " + rep + " content", res);

		table.clear();
		assertEquals(0, table.size());
		assertEquals("this is prohibited content", table.filter("this is prohibited content"));
	}
}
