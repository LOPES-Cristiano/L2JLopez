package com.lopez.l2j;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Validacao estatica das migracoes de dados estaticos (db/data, geradas por tools/GenDataMigrations.java). */
class DataMigrationFilesTest {

	private static final Path DATA = Paths.get("src/main/resources/db/data");
	private static final Path SCHEMA = Paths.get("src/main/resources/db/migration");
	private static final Pattern NAME = Pattern.compile("V(\\d+)__data_([a-z0-9_]+)\\.sql");
	private static final Pattern SCHEMA_NAME = Pattern.compile("V(\\d+)__([a-z0-9_]+)\\.sql");

	private static List<Path> list(Path dir) throws IOException {
		try (Stream<Path> s = Files.list(dir)) {
			return s.filter(p -> p.toString().endsWith(".sql")).sorted().toList();
		}
	}

	@Test
	void eachDataFileMatchesItsSchemaMigration() throws IOException {
		Set<String> schemaNames = new HashSet<>();
		for (Path p : list(SCHEMA)) {
			Matcher m = SCHEMA_NAME.matcher(p.getFileName().toString());
			if (m.matches()) {
				schemaNames.add(m.group(1) + ":" + m.group(2));
			}
		}
		List<Path> data = list(DATA);
		assertTrue(data.size() >= 40, "esperado ao menos 40 tabelas de dados, veio " + data.size());
		for (Path p : data) {
			Matcher m = NAME.matcher(p.getFileName().toString());
			assertTrue(m.matches(), "nome fora do padrao: " + p);
			int version = Integer.parseInt(m.group(1));
			assertTrue(version > 1000, "dados devem usar V1xxx: " + p);
			assertTrue(schemaNames.contains((version - 1000) + ":" + m.group(2)),
					"sem migracao de schema correspondente (V" + (version - 1000) + "__" + m.group(2) + "): " + p);
		}
	}

	@Test
	void dataFilesOnlyInsertAndRestoreSqlMode() throws IOException {
		for (Path p : list(DATA)) {
			String sql = Files.readString(p);
			String upper = sql.toUpperCase();
			assertTrue(!upper.contains("DROP TABLE") && !upper.contains("CREATE TABLE") && !upper.contains("DELETE FROM"),
					"migracao de dados nao pode alterar schema/apagar: " + p);
			assertTrue(sql.contains("INSERT INTO"), "sem INSERT: " + p);
			assertTrue(sql.trim().endsWith("SET SESSION sql_mode = DEFAULT;"), "sql_mode nao restaurado: " + p);
		}
	}
}
