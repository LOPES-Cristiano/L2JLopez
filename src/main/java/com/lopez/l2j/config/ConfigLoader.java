package com.lopez.l2j.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Carrega todos os arquivos .properties localizados no diretorio config/
 * (config/game/main, config/game/custom, config/game/events, config/game/admin, config/login).
 * Permite busca por chave exata, chave normalizada (sem pontuacao/case-insensitive)
 * e chave prefixada por arquivo (ex: "rates.RateXp" ou "mods.AllowOfflineTrade").
 */
public final class ConfigLoader {

	private static final Logger log = LoggerFactory.getLogger(ConfigLoader.class);

	private static final Map<String, String> RAW_PROPERTIES = new ConcurrentHashMap<>();
	private static final Map<String, String> NORMALIZED_PROPERTIES = new ConcurrentHashMap<>();
	private static final Map<String, Map<String, String>> FILE_PROPERTIES = new ConcurrentHashMap<>();
	private static volatile boolean loaded = false;
	private static Path configRootDir = null;

	private ConfigLoader() {
	}

	public static synchronized void load() {
		load(resolveConfigDir());
	}

	public static synchronized void load(Path baseDir) {
		if (baseDir == null || !Files.exists(baseDir)) {
			log.warn("Diretorio de configuracao nao encontrado: {}", baseDir);
			return;
		}

		configRootDir = baseDir;
		RAW_PROPERTIES.clear();
		NORMALIZED_PROPERTIES.clear();
		FILE_PROPERTIES.clear();

		int fileCount = 0;
		try (Stream<Path> stream = Files.walk(baseDir)) {
			var files = stream.filter(Files::isRegularFile)
					.filter(p -> p.getFileName().toString().endsWith(".properties"))
					.sorted()
					.toList();

			for (Path file : files) {
				loadFile(file);
				fileCount++;
			}
		} catch (IOException e) {
			log.error("Erro ao varrer arquivos de configuracao em {}", baseDir, e);
		}

		loaded = true;
		log.info("ConfigLoader: {} arquivos .properties carregados com sucesso ({} propriedades ativas).",
				fileCount, RAW_PROPERTIES.size());
	}

	private static void loadFile(Path file) {
		String filename = file.getFileName().toString();
		String filePrefix = filename.endsWith(".properties")
				? filename.substring(0, filename.length() - ".properties".length()).toLowerCase(Locale.ROOT)
				: filename.toLowerCase(Locale.ROOT);

		String relPath = configRootDir != null
				? configRootDir.relativize(file).toString().replace('\\', '/')
				: filename;
		if (relPath.endsWith(".properties")) {
			relPath = relPath.substring(0, relPath.length() - ".properties".length());
		}
		relPath = relPath.toLowerCase(Locale.ROOT);

		Map<String, String> perFile = new LinkedHashMap<>();

		// Tenta UTF-8 primeiro, depois ISO-8859-1 se falhar
		try {
			readPropertiesFile(file, StandardCharsets.UTF_8, filePrefix, relPath, perFile);
		} catch (Exception e) {
			try {
				readPropertiesFile(file, StandardCharsets.ISO_8859_1, filePrefix, relPath, perFile);
			} catch (Exception ex) {
				log.error("Falha ao ler arquivo de configuracao: {}", file, ex);
			}
		}

		FILE_PROPERTIES.put(filename, perFile);
		FILE_PROPERTIES.put(relPath, perFile);
		FILE_PROPERTIES.put(relPath + ".properties", perFile);
	}

	private static void readPropertiesFile(Path file, Charset charset, String filePrefix, String relPath, Map<String, String> perFile)
			throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(file), charset))) {
			String line;
			int lineNum = 0;
			StringBuilder continuation = new StringBuilder();
			while ((line = reader.readLine()) != null) {
				lineNum++;
				String trimmed = line.trim();
				if (continuation.isEmpty() && (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!"))) {
					continue;
				}

				if (trimmed.endsWith("\\")) {
					continuation.append(trimmed, 0, trimmed.length() - 1);
					continue;
				} else if (!continuation.isEmpty()) {
					continuation.append(trimmed);
					trimmed = continuation.toString();
					continuation.setLength(0);
				}

				int eqIndex = trimmed.indexOf('=');
				if (eqIndex <= 0) {
					continue;
				}

				String key = trimmed.substring(0, eqIndex).trim();
				String value = trimmed.substring(eqIndex + 1).trim();

				// Remove possivel comentario inline no final se nao estiver entre aspas
				if (value.contains("#") && !value.startsWith("\"") && !value.startsWith("'")) {
					int hashIdx = value.indexOf('#');
					value = value.substring(0, hashIdx).trim();
				}

				if (!key.isEmpty()) {
					RAW_PROPERTIES.put(key, value);
					RAW_PROPERTIES.put(filePrefix + "." + key, value);
					RAW_PROPERTIES.put(relPath + "." + key, value);

					String normKey = normalizeKey(key);
					NORMALIZED_PROPERTIES.put(normKey, value);

					perFile.put(key, value);
				}
			}
		}
	}

	public static String normalizeKey(String key) {
		if (key == null) {
			return "";
		}
		return key.replaceAll("[._\\-\\s]", "").toLowerCase(Locale.ROOT);
	}

	public static String getProperty(String key) {
		if (!loaded) {
			load();
		}
		if (key == null) {
			return null;
		}

		// 1. Chave exata
		String val = RAW_PROPERTIES.get(key);
		if (val != null) {
			return val;
		}

		// 2. Chave normalizada
		val = NORMALIZED_PROPERTIES.get(normalizeKey(key));
		if (val != null) {
			return val;
		}

		return null;
	}

	public static String getProperty(String key, String defaultValue) {
		String val = getProperty(key);
		return val != null ? val : defaultValue;
	}

	public static boolean getBoolean(String key, boolean defaultValue) {
		String val = getProperty(key);
		if (val == null) {
			return defaultValue;
		}
		String lower = val.trim().toLowerCase(Locale.ROOT);
		return lower.equals("true") || lower.equals("1") || lower.equals("yes") || lower.equals("on");
	}

	public static String getProperty(String fileOrPath, String key, String defaultValue) {
		if (!loaded) {
			load();
		}
		if (fileOrPath != null && key != null) {
			String clean = fileOrPath.replace('\\', '/').toLowerCase(Locale.ROOT);
			if (clean.endsWith(".properties")) {
				clean = clean.substring(0, clean.length() - ".properties".length());
			}
			String val = RAW_PROPERTIES.get(clean + "." + key);
			if (val != null) {
				return val;
			}
			var map = FILE_PROPERTIES.get(clean);
			if (map != null && map.containsKey(key)) {
				return map.get(key);
			}
			map = FILE_PROPERTIES.get(clean + ".properties");
			if (map != null && map.containsKey(key)) {
				return map.get(key);
			}
		}
		return getProperty(key, defaultValue);
	}

	public static int getInt(String fileOrPath, String key, int defaultValue) {
		String val = getProperty(fileOrPath, key, null);
		if (val == null) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(val.trim());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public static int getInt(String key, int defaultValue) {
		String val = getProperty(key);
		if (val == null) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(val.trim());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public static long getLong(String key, long defaultValue) {
		String val = getProperty(key);
		if (val == null) {
			return defaultValue;
		}
		try {
			return Long.parseLong(val.trim());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public static float getFloat(String key, float defaultValue) {
		String val = getProperty(key);
		if (val == null) {
			return defaultValue;
		}
		try {
			String clean = val.trim();
			if (clean.endsWith(".")) {
				clean = clean + "0";
			}
			return Float.parseFloat(clean);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public static double getDouble(String key, double defaultValue) {
		String val = getProperty(key);
		if (val == null) {
			return defaultValue;
		}
		try {
			String clean = val.trim();
			if (clean.endsWith(".")) {
				clean = clean + "0";
			}
			return Double.parseDouble(clean);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public static List<Integer> getIntList(String key, List<Integer> defaultValue) {
		String val = getProperty(key);
		if (val == null || val.isBlank()) {
			return defaultValue;
		}
		try {
			return Arrays.stream(val.split("[,;\\s]+"))
					.map(String::trim)
					.filter(s -> !s.isEmpty())
					.map(Integer::parseInt)
					.toList();
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static void setProperty(String key, String value) {
		if (key == null) {
			return;
		}
		if (value == null) {
			RAW_PROPERTIES.remove(key);
			NORMALIZED_PROPERTIES.remove(normalizeKey(key));
		} else {
			RAW_PROPERTIES.put(key, value);
			NORMALIZED_PROPERTIES.put(normalizeKey(key), value);
		}
	}

	public static Map<String, String> getAllRawProperties() {
		if (!loaded) {
			load();
		}
		return Collections.unmodifiableMap(RAW_PROPERTIES);
	}

	public static Map<String, Map<String, String>> getAllFileProperties() {
		if (!loaded) {
			load();
		}
		return Collections.unmodifiableMap(FILE_PROPERTIES);
	}

	public static Path getConfigRootDir() {
		if (configRootDir == null) {
			configRootDir = resolveConfigDir();
		}
		return configRootDir;
	}

	public static Path resolveConfigDir() {
		String sysProp = System.getProperty("l2.config.dir");
		if (sysProp != null && !sysProp.isBlank()) {
			Path p = Path.of(sysProp);
			if (Files.exists(p)) {
				return p;
			}
		}

		String env = System.getenv("L2_CONFIG_DIR");
		if (env != null && !env.isBlank()) {
			Path p = Path.of(env);
			if (Files.exists(p)) {
				return p;
			}
		}

		// Ordem de resolucao padrao
		Path p1 = Path.of("config");
		if (Files.exists(p1) && Files.isDirectory(p1)) {
			return p1;
		}

		Path p2 = Path.of("./config");
		if (Files.exists(p2) && Files.isDirectory(p2)) {
			return p2;
		}

		Path p3 = Path.of("../config");
		if (Files.exists(p3) && Files.isDirectory(p3)) {
			return p3;
		}

		Path p4 = Path.of("L2JLopez/config");
		if (Files.exists(p4) && Files.isDirectory(p4)) {
			return p4;
		}

		return p1;
	}
}
