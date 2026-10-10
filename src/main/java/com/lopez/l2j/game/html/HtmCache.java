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
		String clean = filename.trim().toLowerCase(java.util.Locale.ROOT);
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
		int lastSlash = Math.max(clean.lastIndexOf('/'), clean.lastIndexOf('\\'));
		if (lastSlash >= 0 && lastSlash < clean.length() - 1) {
			String justName = clean.substring(lastSlash + 1);
			String relJustName = indexedFiles.get(justName);
			if (relJustName != null) {
				return getHtml(relJustName);
			}
		}
		return getHtml(filename);
	}

	public static boolean isSevenSignsNpc(int npcId) {
		return (npcId >= 31078 && npcId <= 31091) || npcId == 31168 || npcId == 31169
				|| npcId == 31692 || npcId == 31693 || npcId == 31694 || npcId == 31695
				|| npcId == 31997 || npcId == 31998;
	}

	public String getNpcHtml(int npcId, String npcType, int val) {
		// 1. Roteamento de NPCs Especiais de Seven Signs, Mammon, Rift e Olimpiadas
		if (npcId == 31092) {
			String h = getHtml("seven_signs/blkmrkt_1.htm");
			if (h != null) return h;
		} else if (npcId == 31093) {
			String h = getHtml("seven_signs/31093.htm");
			if (h != null) return h;
		} else if (npcId == 31094) {
			String h = getHtml("seven_signs/31094.htm");
			if (h != null) return h;
		} else if (npcId == 31113) {
			String h = getHtml("seven_signs/mammmerch_1.htm");
			if (h != null) return h;
		} else if (npcId == 31126) {
			String h = getHtml("seven_signs/mammblack_1.htm");
			if (h != null) return h;
		} else if (npcId == 31111) {
			String h = getHtml("seven_signs/spirit_dawn.htm");
			if (h != null) return h;
		} else if (npcId == 31112) {
			String h = getHtml("seven_signs/spirit_exit.htm");
			if (h != null) return h;
		} else if (npcId >= 31127 && npcId <= 31131) {
			String h = getHtml("seven_signs/festival/dawn_guide.htm");
			if (h != null) return h;
		} else if (npcId >= 31137 && npcId <= 31141) {
			String h = getHtml("seven_signs/festival/dusk_guide.htm");
			if (h != null) return h;
		} else if ((npcId >= 31132 && npcId <= 31136) || (npcId >= 31142 && npcId <= 31146)) {
			String h = getHtml("seven_signs/festival/festival_witch.htm");
			if (h != null) return h;
		} else if (npcId >= 31865 && npcId <= 31918) {
			String h = getHtml("seven_signs/rift/GuardianOfBorder.htm");
			if (h != null) return h;
		} else if (npcId == 31688) {
			String h = val > 0 ? getHtml("olympiad/noble_menu" + val + ".htm") : getHtml("olympiad/noble_main.htm");
			if (h != null) return h;
		} else if (npcId == 31690 || (npcId >= 31769 && npcId <= 31772)) {
			String h = getHtml("olympiad/hero_main.htm");
			if (h != null) return h;
		}

		// 1b. Amigos de Ketra / Varka / Primeval Isle (npc_friend)
		String friendHtm = getHtml("npc_friend/" + npcId + (val > 0 ? "-" + val : "") + ".htm");
		if (friendHtm != null && !friendHtm.isBlank()) {
			return friendHtm;
		}

		String folder = folderForType(npcType);
		String suffix = val > 0 ? "-" + val : "";

		// 2. Tenta na pasta do tipo especifico (ex: teleporter/30006.htm, teleporter/30006-1.htm)
		String pathType = folder + "/" + npcId + suffix + ".htm";
		String html = getHtml(pathType);
		if (html != null && !html.isBlank()) {
			return html;
		}

		// 2b. Formato com padding zero (ex: 30006-01.htm)
		if (val > 0) {
			String altType = folder + "/" + npcId + "-0" + val + ".htm";
			html = getHtml(altType);
			if (html != null && !html.isBlank()) {
				return html;
			}
		}

		// 3. Tenta na pasta default (se folder != default)
		if (!"default".equals(folder)) {
			String pathDefault = "default/" + npcId + suffix + ".htm";
			html = getHtml(pathDefault);
			if (html != null && !html.isBlank()) {
				return html;
			}
			if (val > 0) {
				String altDef = "default/" + npcId + "-0" + val + ".htm";
				html = getHtml(altDef);
				if (html != null && !html.isBlank()) {
					return html;
				}
			}
		}

		// 4. Shared / Type Template HTML (Templates compartilhados oficiais de cada tipo)
		String shared = resolveSharedTypeHtml(folder, npcId, val);
		if (shared != null && !shared.isBlank()) {
			return shared;
		}

		// 5. Busca no indice global de arquivos HTML para encontrar o dialogo oficial retail em qualquer subpasta
		String[] candidates = val > 0
				? new String[] { npcId + "-" + val + ".htm", npcId + "-0" + val + ".htm", npcId + "-" + val + ".html" }
				: new String[] { npcId + ".htm", npcId + "-1.htm", npcId + "-01.htm", npcId + ".html" };

		for (String cand : candidates) {
			String indexedPath = indexedFiles.get(cand.toLowerCase(java.util.Locale.ROOT));
			if (indexedPath != null && !indexedPath.toLowerCase(java.util.Locale.ROOT).contains("npcloc")) {
				html = getHtml(indexedPath);
				if (html != null && !html.isBlank()) {
					return html;
				}
			}
		}

		// 6. Guarda sem diálogo individual recebe fala padronizada de guarda
		if ("guard".equals(folder)) {
			String guardHtm = getHtml("guard/guard.htm");
			if (guardHtm != null && !guardHtm.isBlank()) {
				return guardHtm;
			}
		}

		// 7. Fallback oficial canônico: npcdefault.htm
		String defaultHtml = getHtml("npcdefault.htm");
		if (defaultHtml != null && !defaultHtml.isBlank()) {
			return defaultHtml;
		}

		return "<html><body>%npc_name%:<br><br>I have nothing to say to you.<br><a action=\"bypass -h npc_%objectId%_Quest\">Quest</a></body></html>";
	}

	private String resolveSharedTypeHtml(String folder, int npcId, int val) {
		String suffix = val > 0 ? "-" + val : "";
		return switch (folder) {
			case "symbolmaker" -> {
				String h = val > 0 ? getHtml("symbolmaker/SymbolMaker" + suffix + ".htm") : getHtml("symbolmaker/SymbolMaker.htm");
				yield h != null ? h : getHtml("symbolmaker/SymbolMaker.htm");
			}
			case "manormanager" -> {
				String h = val > 0 ? getHtml("manormanager/manager" + suffix + ".htm") : getHtml("manormanager/manager.htm");
				yield h != null ? h : getHtml("manormanager/manager.htm");
			}
			case "auction" -> {
				String h = val > 0 ? getHtml("auction/auction" + suffix + ".htm") : getHtml("auction/auction.htm");
				yield h != null ? h : getHtml("auction/auction.htm");
			}
			case "clanHallManager" -> {
				String h = getHtml("clanHallManager/chamberlain.htm");
				yield h != null ? h : getHtml("clanHallManager/manage.htm");
			}
			case "castleblacksmith" -> {
				String h = val > 0 ? getHtml("castleblacksmith/castleblacksmith" + suffix + ".htm") : getHtml("castleblacksmith/castleblacksmith.htm");
				yield h != null ? h : getHtml("castleblacksmith/castleblacksmith.htm");
			}
			case "castlewarehouse" -> {
				String h = val > 0 ? getHtml("castlewarehouse/castlewarehouse" + suffix + ".htm") : getHtml("castlewarehouse/castlewarehouse.htm");
				yield h != null ? h : getHtml("castlewarehouse/castlewarehouse.htm");
			}
			case "castleteleporter" -> {
				String h = val > 0 ? getHtml("castleteleporter/MassGK" + suffix + ".htm") : getHtml("castleteleporter/MassGK.htm");
				if (h == null) {
					h = getHtml("teleporter/castleteleporter.htm");
				}
				yield h;
			}
			case "teleporter" -> {
				if (npcId >= 35092 && npcId <= 35565) {
					yield val > 0 ? getHtml("castleteleporter/MassGK" + suffix + ".htm") : getHtml("castleteleporter/MassGK.htm");
				}
				String h = getHtml("teleporter/teleporter.htm");
				if (h == null) {
					h = "<html><body>%npc_name%:<br><br>Where would you like to go?<br><a action=\"bypass -h npc_%objectId%_Chat 1\">Teleport</a><br><a action=\"bypass -h npc_%objectId%_Quest\">Quest</a></body></html>";
				}
				yield h;
			}
			case "merchant" -> {
				String h = getHtml("merchant/merchant.htm");
				if (h == null) {
					h = "<html><body>%npc_name%:<br><br>Welcome. How may I help you?<br><a action=\"bypass -h npc_%objectId%_Buy 0\">Buy items</a><br><a action=\"bypass -h npc_%objectId%_Sell\">Sell items</a><br><a action=\"bypass -h npc_%objectId%_Quest\">Quest</a></body></html>";
				}
				yield h;
			}
			case "blacksmith" -> {
				String h = getHtml("blacksmith/blacksmith.htm");
				if (h == null) {
					h = "<html><body>%npc_name%:<br><br>Welcome. What would you like to forge today?<br><a action=\"bypass -h npc_%objectId%_Multisell 0\">Craft Dual Swords</a><br><a action=\"bypass -h npc_%objectId%_Augment 1\">Augment Item</a><br><a action=\"bypass -h npc_%objectId%_Quest\">Quest</a></body></html>";
				}
				yield h;
			}
			case "warehouse" -> {
				String h = getHtml("warehouse/warehouse.htm");
				if (h == null) {
					h = "<html><body>%npc_name%:<br><br>Welcome to the Warehouse.<br><a action=\"bypass -h npc_%objectId%_DepositP\">Deposit Item (Private Warehouse)</a><br><a action=\"bypass -h npc_%objectId%_WithdrawP\">Withdraw Item (Private Warehouse)</a><br><a action=\"bypass -h npc_%objectId%_DepositC\">Deposit Item (Clan Warehouse)</a><br><a action=\"bypass -h npc_%objectId%_WithdrawC\">Withdraw Item (Clan Warehouse)</a><br><a action=\"bypass -h npc_%objectId%_Quest\">Quest</a></body></html>";
				}
				yield h;
			}
			case "chamberlain" -> {
				String specific = getHtml("chamberlain/" + npcId + "-d.htm");
				if (specific != null && !specific.isBlank()) {
					yield specific;
				}
				yield getHtml("chamberlain/chamberlain.htm");
			}
			case "castlemagician" -> getHtml("castlemagician/magician.htm");
			case "mercmanager" -> getHtml("mercmanager/mercmanager.htm");
			case "wyvernmanager" -> getHtml("wyvernmanager/wyvernmanager.htm");
			case "classmaster" -> getHtml("classmaster/classmaster.htm");
			case "fortress" -> {
				String h = val > 0 ? getHtml("fortress/supportunit" + suffix + ".htm") : getHtml("fortress/supportunit.htm");
				yield h != null ? h : getHtml("fortress/supportunit.htm");
			}
			case "siege" -> getHtml("siege/" + npcId + "-busy.htm");
			case "doormen" -> {
				String h = getHtml("doormen/fortress/" + npcId + suffix + ".htm");
				if (h != null) yield h;
				h = getHtml("doormen/" + npcId + "-no.htm");
				if (h != null) yield h;
				yield getHtml("doormen/35602-no.htm");
			}
			case "newbiehelper" -> {
				String h = switch (npcId) {
					case 30598 -> getHtml("newbiehelper/guide_human_cnacelot/guide_human_cnacelot001.htm");
					case 30599 -> getHtml("newbiehelper/guide_gludin_nina/guide_gludin_nina001.htm");
					case 30600 -> getHtml("newbiehelper/guide_gludio_euria/guide_gludio_euria001.htm");
					case 30601, 30528 -> getHtml("newbiehelper/guide_dwarf_gullin/guide_dwarf_gullin001.htm");
					case 30602, 30370 -> getHtml("newbiehelper/guide_elf_roios/guide_elf_roios001.htm");
					case 30129 -> getHtml("newbiehelper/guide_delf_frankia/guide_delf_frankia001.htm");
					case 30573 -> getHtml("newbiehelper/guide_orc_tanai/guide_orc_tanai001.htm");
					default -> null;
				};
				if (h == null) {
					h = getHtml("newbiehelper/newbie_guide001.htm");
				}
				yield h;
			}
			default -> null;
		};
	}

	private static final java.util.regex.Pattern BRACKET_LINK_PATTERN =
			java.util.regex.Pattern.compile("\\[([^\n\\]|]+)\\|([^\n\\]]+)\\]");

	public static String convertBracketLinks(String html) {
		if (html == null || !html.contains("[") || !html.contains("|")) {
			return html;
		}
		var matcher = BRACKET_LINK_PATTERN.matcher(html);
		var sb = new StringBuilder(html.length() + 64);
		while (matcher.find()) {
			String action = matcher.group(1).trim();
			String text = matcher.group(2).trim();
			String replacement;
			if (action.startsWith("bypass ") || action.startsWith("link ")) {
				replacement = "<a action=\"" + action + "\">" + text + "</a>";
			} else {
				replacement = "<a action=\"bypass -h " + action + "\">" + text + "</a>";
			}
			matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	public String render(String rawHtml, int npcObjectId, String npcName, String playerName) {
		return render(rawHtml, npcObjectId, npcName, playerName, 1, 0);
	}

	public String render(String rawHtml, int npcObjectId, String npcName, String playerName, int castleId, int npcId) {
		if (rawHtml == null) {
			return "";
		}
		String s = rawHtml
				.replace("%objectId%", String.valueOf(npcObjectId))
				.replace("%npc_name%", npcName != null ? npcName : "")
				.replace("%npc_name", npcName != null ? npcName : "")
				.replace("%npcname%", npcName != null ? npcName : "")
				.replace("%name%", playerName != null ? playerName : "")
				.replace("%name", playerName != null ? playerName : "")
				.replace("%player_name%", playerName != null ? playerName : "")
				.replace("%playername%", playerName != null ? playerName : "");
		if (castleId > 0) {
			s = s.replace("%castleid%", String.valueOf(castleId))
				 .replace("%castle_id%", String.valueOf(castleId));
		}
		if (npcId > 0) {
			s = s.replace("%npcId%", String.valueOf(npcId))
				 .replace("%npc_id%", String.valueOf(npcId));
		}
		s = convertBracketLinks(s);
		return s;
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
			case "teleporter" -> "teleporter";
			case "castleteleporter" -> "castleteleporter";
			case "merchant" -> "merchant";
			case "guard", "guardnohtml", "fortguard", "siegeguard", "warden" -> "guard";
			case "warehouse" -> "warehouse";
			case "castlewarehouse" -> "castlewarehouse";
			case "trainer", "mysticmaster", "priestmaster" -> "trainer";
			case "villagemaster" -> "villagemaster";
			case "fisherman" -> "fisherman";
			case "symbolmaker" -> "symbolmaker";
			case "doormen", "doorman" -> "doormen";
			case "newbiehelper" -> "newbiehelper";
			case "adventurer", "adventurer_guildsman" -> "adventurer_guildsman";
			case "castleblacksmith" -> "castleblacksmith";
			case "blacksmith" -> "blacksmith";
			case "castlemagician", "magician" -> "castlemagician";
			case "chamberlain", "castlechamberlain" -> "chamberlain";
			case "clanhallmanager" -> "clanHallManager";
			case "classmaster" -> "classmaster";
			case "olympiad", "olympiadmanager" -> "olympiad";
			case "seven_signs", "sevensigns", "signspriest", "festivalguide" -> "seven_signs";
			case "manormanager" -> "manormanager";
			case "auctioneer" -> "auction";
			case "wyvernmanager", "fortwyvernmanager" -> "wyvernmanager";
			case "observation" -> "observation";
			case "sepulchernpc" -> "SepulcherNpc";
			case "mercmanager" -> "mercmanager";
			case "siegenpc", "siege" -> "siege";
			case "fortmanager", "fortsupportunit", "fortcommander", "fortenvoy", "fortsiegenpc" -> "fortress";
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
