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

	public HtmCache(@Value("${l2.datapack.html-dir:data/html}") String htmlDirPath) {
		Path p = Path.of(htmlDirPath);
		if (!Files.isDirectory(p)) {
			Path local = Path.of("data/html");
			p = Files.isDirectory(local) ? local : p;
		}
		this.datapackHtmlDir = p;
		log.info("HtmCache inicializado com diretorio: {}", datapackHtmlDir.toAbsolutePath());
	}

	public String getHtml(String relativePath) {
		return cache.computeIfAbsent(normalize(relativePath), this::loadFile);
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

		// 3. Fallback para npcdefault.htm
		String defaultHtml = getHtml("npcdefault.htm");
		if (defaultHtml != null && !defaultHtml.isBlank()) {
			return defaultHtml;
		}

		// 4. Fallback sintetico inteligente baseado no tipo do NPC
		StringBuilder sb = new StringBuilder();
		sb.append("<html><body><font color=\"LEVEL\">%npc_name%</font>:<br><br>");
		sb.append("Hello, %name%! How may I assist you today?<br><br>");

		String lowerType = npcType != null ? npcType.toLowerCase(java.util.Locale.ROOT) : "";
		if (lowerType.contains("teleport")) {
			sb.append("<a action=\"bypass -h npc_%objectId%_Chat 1\">Teleport</a><br>");
		} else if (lowerType.contains("merchant") || lowerType.contains("trader") || lowerType.contains("grocer")) {
			sb.append("<a action=\"bypass -h npc_%objectId%_Buy 1\">Buy items</a><br>");
		} else if (lowerType.contains("trainer") || lowerType.contains("master") || lowerType.contains("teacher")) {
			sb.append("<a action=\"bypass -h npc_%objectId%_SkillList\">Learn skills</a><br>");
		} else if (lowerType.contains("warehouse")) {
			sb.append("<a action=\"bypass -h npc_%objectId%_DepositP\">Deposit items</a><br>");
			sb.append("<a action=\"bypass -h npc_%objectId%_WithdrawP\">Withdraw items</a><br>");
		}
		sb.append("<a action=\"bypass -h npc_%objectId%_Quest\">Quest</a><br>");
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
				.replace("%name%", playerName != null ? playerName : "");
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
			case "warehouse" -> "warehouse";
			case "trainer" -> "trainer";
			case "villagemaster" -> "villagemaster";
			case "fisherman" -> "fisherman";
			case "symbolmaker" -> "symbolmaker";
			case "doormen", "doorman" -> "doormen";
			default -> "default";
		};
	}

	private String loadFile(String relativePath) {
		// 1. Tenta no sistema de arquivos
		Path file = datapackHtmlDir.resolve(relativePath);
		if (Files.isRegularFile(file)) {
			try {
				return Files.readString(file, StandardCharsets.UTF_8);
			} catch (IOException e) {
				try {
					return Files.readString(file, StandardCharsets.ISO_8859_1);
				} catch (IOException ex) {
					log.warn("Erro ao ler {}: {}", file, ex.getMessage());
				}
			}
		}

		// 2. Tenta nos recursos do classpath
		String resPath = "/data/html/" + relativePath;
		try (InputStream in = getClass().getResourceAsStream(resPath)) {
			if (in != null) {
				return new String(in.readAllBytes(), StandardCharsets.UTF_8);
			}
		} catch (IOException e) {
			log.debug("Recurso classpath nao encontrado: {}", resPath);
		}

		return null;
	}

	private static String normalize(String path) {
		return path.replace('\\', '/').replaceAll("^/+", "");
	}
}
