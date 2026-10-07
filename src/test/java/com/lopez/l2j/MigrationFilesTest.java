package com.lopez.l2j;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

/**
 * Validacao estatica das migracoes Flyway. Nao executamos no H2 porque o DDL e do MariaDB
 * (ex.: nomes de indice por tabela); a execucao real exige um MariaDB.
 */
class MigrationFilesTest {

	private static final Path DIR = Paths.get("src/main/resources/db/migration");
	private static final Pattern NAME = Pattern.compile("V(\\d+)__([a-z0-9_]+)\\.sql");

	private static List<Path> files() throws IOException {
		try (Stream<Path> s = Files.list(DIR)) {
			return s.filter(p -> p.getFileName().toString().endsWith(".sql")).sorted().toList();
		}
	}

	@Test
	void versionsAreUniqueAndContiguousUpToLegacyCount() throws IOException {
		Set<Integer> versions = new HashSet<>();
		for (Path p : files()) {
			Matcher m = NAME.matcher(p.getFileName().toString());
			assertTrue(m.matches(), "nome fora do padrao: " + p);
			assertTrue(versions.add(Integer.parseInt(m.group(1))), "versao duplicada: " + p);
		}
		// V1 baseline + V2..V122 (121 tabelas legadas) + V123 em diante (novas)
		for (int v = 1; v <= 123; v++) {
			assertTrue(versions.contains(v), "faltando V" + v);
		}
	}

	@Test
	void everyMigrationCreatesATableIdempotentlyAndUsesInnoDb() throws IOException {
		for (Path p : files()) {
			String sql = Files.readString(p);
			if (p.getFileName().toString().startsWith("V1__") || sql.contains("INSERT INTO")) {
				continue; // baseline escrito a mao ou inserts de dados
			}
			assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS"), "sem CREATE TABLE IF NOT EXISTS: " + p);
			assertTrue(!sql.contains("MyISAM"), "MyISAM nao permitido: " + p);
			assertTrue(!Pattern.compile("IF NOT EXISTS\\s+IF NOT EXISTS").matcher(sql).find(), "IF NOT EXISTS duplicado: " + p);
			assertTrue(!sql.toUpperCase().contains("DROP TABLE"), "DROP TABLE proibido em migracao: " + p);
			assertEquals(count(sql, "("), count(sql, ")"), "parenteses desbalanceados: " + p);
			assertTrue(sql.trim().endsWith(";"), "sem ponto-e-virgula final: " + p);
		}
	}

	private static long count(String s, String ch) {
		return s.chars().filter(c -> c == ch.charAt(0)).count();
	}
}
