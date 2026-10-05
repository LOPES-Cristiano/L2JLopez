package com.lopez.l2j.game.html;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Cache e renderizador de HTMLs de diálogo de NPCs (porta do HtmCache legado).
 * Procura nos caminhos de datapack configurados ou nos recursos embutidos.
 */
@Component
public class HtmCache {

	private static final Logger log = LoggerFactory.getLogger(HtmCache.class);

	private final Path datapackHtmlDir;
	private final Map<String, String> cache = new ConcurrentHashMap<>();
	private final Map<String, String> indexedFiles = new ConcurrentHashMap<>();

	public HtmCache(@Value("${l2.datapack.html-dir:data/html}") String htmlDirPath) {
		Path p = Path.of(htmlDirPath);
		if (!Files.isDirectory(p)) {
			Path local = Path.of("data/html");
			p = Files.isDirectory(local) ? local : p;
		}
		this.datapackHtmlDir = p;
		indexHtmlFiles(this.datapackHtmlDir, this.datapackHtmlDir);
		Path scriptsDir = Path.of("data/scripts");
		if (Files.isDirectory(scriptsDir)) {
			indexHtmlFiles(scriptsDir, scriptsDir);
		}
		log.info("HtmCache inicializado com diretorio: {} ({} arquivos indexados)",
				datapackHtmlDir.toAbsolutePath(), indexedFiles.size());
	}

	private void indexHtmlFiles(Path dir, Path baseDir) {
		if (!Files.isDirectory(dir)) {
			return;
		}
		try (var stream = Files.walk(dir)) {
			stream.filter(Files::isRegularFile).forEach(file -> {
				String name = file.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
				if (name.endsWith(".htm") || name.endsWith(".html")) {
					String rel = normalize(baseDir.relativize(file).toString());
					indexedFiles.putIfAbsent(name, rel);
				}
			});
		} catch (IOException e) {
			log.warn("Erro ao indexar arquivos HTML em {}: {}", dir, e.getMessage());
		}
	}

	public String getHtml(String relativePath) {
		return cache.computeIfAbsent(normalize(relativePath), this::loadFile);
	}

	public String getIndexedHtml(String filename) {
		if (filename == null || filename.isBlank()) {
			return null;
		}
		String clean = filename.toLowerCase(java.util.Locale.ROOT);
		if (!clean.endsWith(".htm") && !clean.endsWith(".html")) {
			String rel = indexedFiles.get(clean + ".htm");
			if (rel != null) {
				return getHtml(rel);
			}
			rel = indexedFiles.get(clean + "_menu.htm");
			if (rel != null) {
				return getHtml(rel);
			}
		}
		String rel = indexedFiles.get(clean);
		if (rel != null) {
			return getHtml(rel);
		}
		return getHtml(filename);
	}

	public String getNpcHtml(int npcId, String npcType, int val) {
		String folder = folderForType(npcType);
		String suffix = val > 0 ? "-" + val : "";

		// 1. Tenta na pasta do tipo especifico (ex: teleporter/30006.htm)
		String pathType = folder + "/" + npcId + suffix + ".htm";
		String html = getHtml(pathType);
		if (html != null && !html.isBlank()) {
			return html;
		}

		// 2. Tenta na pasta default (ex: default/30006.htm)
		if (!"default".equals(folder)) {
			String pathDefault = "default/" + npcId + suffix + ".htm";
			html = getHtml(pathDefault);
			if (html != null && !html.isBlank()) {
				return html;
			}
		}

		// 3. Busca no indice global de arquivos HTML para encontrar o dialogo oficial retail em qualquer subpasta
		String[] candidates = val > 0
				? new String[] { npcId + "-" + val + ".htm", npcId + "-0" + val + ".htm", npcId + "-" + val + ".html" }
				: new String[] { npcId + ".htm", npcId + "-1.htm", npcId + "-01.htm", npcId + ".html" };

		for (String cand : candidates) {
			String indexedPath = indexedFiles.get(cand.toLowerCase(java.util.Locale.ROOT));
			if (indexedPath != null) {
				html = getHtml(indexedPath);
				if (html != null && !html.isBlank()) {
					return html;
				}
			}
		}

		String lowerType = npcType != null ? npcType.toLowerCase(java.util.Locale.ROOT) : "";
		boolean isFunctional = lowerType.contains("teleport")
				|| lowerType.contains("merchant") || lowerType.contains("trader") || lowerType.contains("grocer")
				|| lowerType.contains("blacksmith") || lowerType.contains("trainer") || lowerType.contains("master")
				|| lowerType.contains("teacher") || lowerType.contains("warehouse") || lowerType.contains("guard")
				|| lowerType.contains("fisherman") || lowerType.contains("symbolmaker");

		// 4. Se for NPC funcional ou se val > 0, gera o dialogo sintetico interativo rico
		if (isFunctional || val > 0) {
			return generateSmartNpcHtml(npcId, lowerType, val);
		}

		// 5. Fallback para npcdefault.htm se nao for NPC funcional
		String defaultHtml = getHtml("npcdefault.htm");
		if (defaultHtml != null && !defaultHtml.isBlank()) {
			return defaultHtml;
		}

		return generateSmartNpcHtml(npcId, lowerType, val);
	}

	private String generateSmartNpcHtml(int npcId, String lowerType, int val) {
		StringBuilder sb = new StringBuilder();
		sb.append("<html><body><font color=\"LEVEL\">%npc_name%</font>:<br><br>");

		if (lowerType.contains("teleport")) {
			if (val > 0) {
				sb.append("Select your destination:<br><br>");
				sb.append("<a action=\"bypass -h npc_%objectId%_goto 1\">Town of Gludio - 10,000 Adena</a><br1>");
				sb.append("<a action=\"bypass -h npc_%objectId%_goto 269\">Town of Giran - 6,800 Adena</a><br1>");
				sb.append("<a action=\"bypass -h npc_%objectId%_goto 275\">Town of Aden - 52,000 Adena</a><br1>");
				sb.append("<a action=\"bypass -h npc_%objectId%_goto 276\">Town of Oren - 33,000 Adena</a><br1>");
				sb.append("<a action=\"bypass -h npc_%objectId%_goto 270\">Heine - 12,000 Adena</a><br1>");
				sb.append("<a action=\"bypass -h npc_%objectId%_goto 271\">Town of Gludio - 3,400 Adena</a><br1>");
				sb.append("<a action=\"bypass -h npc_%objectId%_goto 272\">Town of Goddard - 71,000 Adena</a><br1>");
				sb.append("<a action=\"bypass -h npc_%objectId%_goto 273\">Rune Township - 57,000 Adena</a><br1>");
				sb.append("<a action=\"bypass -h npc_%objectId%_goto 274\">Town of Schuttgart - 88,000 Adena</a><br><br>");
				sb.append("<a action=\"bypass -h npc_%objectId%_Chat 0\">Back</a><br>");
			} else {
				sb.append("May the starlight guide your path, %name%! Which destination would you like to travel to?<br><br>");
				sb.append("<a action=\"bypass -h npc_%objectId%_Chat 1\">Teleport</a><br>");
				sb.append("<a action=\"bypass -h npc_%objectId%_Quest 1101_teleport_to_race_track\">Move to Monster Derby Track (Free)</a><br>");
				sb.append("<a action=\"bypass -h npc_%objectId%_Quest\">Quest</a><br>");
			}
		} else if (lowerType.contains("merchant") || lowerType.contains("trader") || lowerType.contains("grocer")) {
			sb.append("Greetings %name%! Take a look at our fine wares. Best prices in the realm!<br><br>");
			sb.append("<a action=\"bypass -h npc_%objectId%_Buy 1\">Buy items</a><br>");
			sb.append("<a action=\"bypass -h npc_%objectId%_Sell\">Sell items</a><br>");
			sb.append("<a action=\"bypass -h npc_%objectId%_multisell 1\">Exchange equipment</a><br>");
			sb.append("<a action=\"bypass -h npc_%objectId%_TerritoryStatus\">View territory tax rate</a><br>");
			sb.append("<a action=\"bypass -h npc_%objectId%_Quest\">Quest</a><br>");
		} else if (lowerType.contains("blacksmith")) {
			sb.append("Welcome to the forge, %name%! The anvil never rests.<br><br>");
			sb.append("<a action=\"bypass -h npc_%objectId%_Link common/duals_01.htm\">Craft Dual Swords</a><br>");
			sb.append("<a action=\"bypass -h npc_%objectId%_Link common/crafting_01.htm\">Craft Items</a><br>");
			sb.append("<a action=\"bypass -h npc_%objectId%_Link common/weapon_sa_01.htm\">Bestow Special Ability</a><br>");
			sb.append("<a action=\"bypass -h npc_%objectId%_Link common/augmentation_01.htm\">Augment Item</a><br>");
			sb.append("<a action=\"bypass -h npc_%objectId%_Link common/augmentation_02.htm\">Cancel Item Augmentation</a><br>");
			sb.append("<a action=\"bypass -h npc_%objectId%_SkillList\">Learn Skills</a><br>");
			sb.append("<a action=\"bypass -h npc_%objectId%_TerritoryStatus\">View territory tax rate</a><br>");
			sb.append("<a action=\"bypass -h npc_%objectId%_Quest\">Quest</a><br>");
		} else if (lowerType.contains("trainer") || lowerType.contains("master") || lowerType.contains("teacher") || lowerType.contains("guild")) {
			sb.append("Welcome, pupil %name%. Are you ready to sharpen your abilities?<br><br>");
			sb.append("<a action=\"bypass -h npc_%objectId%_SkillList\">Learn skills</a><br>");
			sb.append("<a action=\"bypass -h npc_%objectId%_Quest\">Quest</a><br>");
		} else if (lowerType.contains("warehouse")) {
			sb.append("Greetings! Your possessions are completely secure in our vault.<br><br>");
			sb.append("<a action=\"bypass -h npc_%objectId%_DepositP\">Deposit Item (Private)</a><br>");
			sb.append("<a action=\"bypass -h npc_%objectId%_WithdrawP\">Withdraw Item (Private)</a><br>");
			sb.append("<a action=\"bypass -h npc_%objectId%_DepositC\">Deposit Item (Clan)</a><br>");
			sb.append("<a action=\"bypass -h npc_%objectId%_WithdrawC\">Withdraw Item (Clan)</a><br>");
			sb.append("<a action=\"bypass -h npc_%objectId%_Quest\">Quest</a><br>");
		} else if (lowerType.contains("guard")) {
			sb.append("The perimeter of this city is secure under our watchful eye. Move along, %name%, and stay safe.<br><br>");
			sb.append("<a action=\"bypass -h npc_%objectId%_Quest\">Quest</a><br>");
		} else {
			sb.append("Hello, %name%! How may I assist you today?<br><br>");
			sb.append("<a action=\"bypass -h npc_%objectId%_Quest\">Quest</a><br>");
		}

		sb.append("</body></html>");
		return sb.toString();
	}

	public String render(String rawHtml, int npcObjectId, String npcName, String playerName) {
		if (rawHtml == null) {
			return "";
		}
		return rawHtml
				.replace("%objectId%", String.valueOf(npcObjectId))
				.replace("%npc_name%", npcName != null ? npcName : "")
				.replace("%npc_name", npcName != null ? npcName : "")
				.replace("%name%", playerName != null ? playerName : "")
				.replace("%name", playerName != null ? playerName : "")
				.replace("%player_name%", playerName != null ? playerName : "");
	}

	private String folderForType(String npcType) {
		if (npcType == null) {
			return "default";
		}
		String clean = npcType.toLowerCase(java.util.Locale.ROOT);
		if (clean.startsWith("l2")) {
			clean = clean.substring(2);
		}
		if (clean.endsWith("instance")) {
			clean = clean.substring(0, clean.length() - "instance".length());
		}
		return switch (clean) {
			case "teleporter", "castleteleporter" -> "teleporter";
			case "merchant" -> "merchant";
			case "guard", "guardnohtml", "fortguard", "siegeguard" -> "guard";
			case "warehouse", "castlewarehouse" -> "warehouse";
			case "trainer" -> "trainer";
			case "villagemaster" -> "villagemaster";
			case "fisherman" -> "fisherman";
			case "symbolmaker" -> "symbolmaker";
			case "doormen", "doorman" -> "doormen";
			case "newbiehelper" -> "newbiehelper";
			case "adventurer_guildsman" -> "adventurer_guildsman";
			case "blacksmith", "castleblacksmith" -> "castleblacksmith";
			case "magician", "castlemagician" -> "castlemagician";
			case "chamberlain" -> "chamberlain";
			case "clanhallmanager" -> "clanHallManager";
			case "classmaster" -> "classmaster";
			case "olympiad" -> "olympiad";
			case "seven_signs", "sevensigns" -> "seven_signs";
			default -> "default";
		};
	}

	private String loadFile(String relativePath) {
		// 1. Tenta no sistema de arquivos em data/html
		Path file = datapackHtmlDir.resolve(relativePath);
		if (Files.isRegularFile(file)) {
			return readToString(file);
		}

		// 2. Tenta em data/scripts
		Path scriptFile = Path.of("data/scripts").resolve(relativePath);
		if (Files.isRegularFile(scriptFile)) {
			return readToString(scriptFile);
		}

		// 3. Tenta em data/
		Path dataFile = Path.of("data").resolve(relativePath);
		if (Files.isRegularFile(dataFile)) {
			return readToString(dataFile);
		}

		// 4. Tenta nos recursos do classpath
		String resPath = "/data/html/" + relativePath;
		try (InputStream in = getClass().getResourceAsStream(resPath)) {
			if (in != null) {
				return new String(in.readAllBytes(), StandardCharsets.UTF_8);
			}
		} catch (IOException ignored) {
		}

		String scriptResPath = "/data/scripts/" + relativePath;
		try (InputStream in = getClass().getResourceAsStream(scriptResPath)) {
			if (in != null) {
				return new String(in.readAllBytes(), StandardCharsets.UTF_8);
			}
		} catch (IOException ignored) {
		}

		return null;
	}

	private String readToString(Path file) {
		try {
			return Files.readString(file, StandardCharsets.UTF_8);
		} catch (IOException e) {
			try {
				return Files.readString(file, StandardCharsets.ISO_8859_1);
			} catch (IOException ex) {
				log.warn("Erro ao ler {}: {}", file, ex.getMessage());
				return null;
			}
		}
	}

	private static String normalize(String path) {
		return path.replace('\\', '/').replaceAll("^/+", "");
	}
}
