import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gera migracoes Flyway de DADOS (db/data/V1xxx__data_<tabela>.sql) a partir dos INSERTs de tools/sql do
 * L2JDreamV2. Versao = 1000 + versao da migracao de schema da mesma tabela. Agrupa 500 linhas por INSERT.
 * Uso: java GenDataMigrations.java <tools/sql> <db/migration> <db/data>
 */
public class GenDataMigrations {
	static final Pattern INSERT = Pattern.compile("^INSERT INTO `?(\\w+)`? VALUES\\s*(\\(.*\\));\\s*$");
	static final Pattern MIG = Pattern.compile("V(\\d+)__(\\w+)\\.sql");
	static final java.util.Set<String> SKIP = java.util.Set.of("gameservers");
	static final int BATCH = 500;

	public static void main(String[] args) throws IOException {
		Path sqlDir = Path.of(args[0]);
		Path migDir = Path.of(args[1]);
		Path outDir = Path.of(args[2]);
		Files.createDirectories(outDir);
		Map<String, Integer> versions = new TreeMap<>();
		try (var s = Files.list(migDir)) {
			s.forEach(p -> {
				Matcher m = MIG.matcher(p.getFileName().toString());
				if (m.matches()) {
					versions.put(m.group(2), Integer.parseInt(m.group(1)));
				}
			});
		}
		int total = 0;
		List<Path> files;
		try (var s = Files.list(sqlDir)) {
			files = s.filter(p -> p.toString().endsWith(".sql")).sorted().toList();
		}
		for (Path f : files) {
			String table = f.getFileName().toString().replace(".sql", "");
			List<String> rows = new ArrayList<>();
			for (String line : Files.readAllLines(f, StandardCharsets.UTF_8)) {
				if (!line.startsWith("INSERT")) {
					continue;
				}
				Matcher m = INSERT.matcher(line);
				if (!m.matches() || !m.group(1).equals(table)) {
					throw new IllegalStateException("INSERT inesperado em " + f + ": " + line.substring(0, Math.min(120, line.length())));
				}
				rows.add(m.group(2));
			}
			if (rows.isEmpty() || SKIP.contains(table)) {
				continue;
			}
			Integer v = versions.get(table);
			if (v == null) {
				throw new IllegalStateException("sem migracao de schema para " + table);
			}
			StringBuilder sb = new StringBuilder();
			sb.append("-- Dados de tools/sql/").append(table).append(".sql do L2JDreamV2 (").append(rows.size())
					.append(" linhas). Gerado por GenDataMigrations; nao editar a mao.\n");
			sb.append("-- Valores legados ('' em colunas numericas etc.) exigem modo SQL nao estrito nesta sessao.\n");
			sb.append("SET SESSION sql_mode = 'NO_ENGINE_SUBSTITUTION';\n");
			for (int i = 0; i < rows.size(); i += BATCH) {
				sb.append("INSERT INTO `").append(table).append("` VALUES\n");
				List<String> chunk = rows.subList(i, Math.min(rows.size(), i + BATCH));
				sb.append(String.join(",\n", chunk)).append(";\n");
			}
			sb.append("SET SESSION sql_mode = DEFAULT;\n");
			Path out = outDir.resolve("V" + (1000 + v) + "__data_" + table + ".sql");
			Files.writeString(out, sb.toString(), StandardCharsets.UTF_8);
			total += rows.size();
			System.out.printf("%-32s %6d linhas -> %s%n", table, rows.size(), out.getFileName());
		}
		System.out.println("TOTAL " + total);
	}
}
