package com.lopez.l2j.game.html;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

	private static final Pattern QUEST_ID_PREFIX_PATTERN = Pattern.compile("^[_qQ]*(\\d+)");
	private static final Pattern FILE_ZERO_PAD_PATTERN = Pattern.compile("^(.*)-0(\\d)\\.(htm|html)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern FILE_NO_PAD_PATTERN = Pattern.compile("^(.*)-(\\d)\\.(htm|html)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern NPC_FILE_PREFIX_PATTERN = Pattern.compile("^(\\d{4,5})(?:[-_].*)?\\.(htm|html)$", Pattern.CASE_INSENSITIVE);
	private static final String[] NPC_FALLBACK_FOLDERS = {
		"default", "trainer", "villagemaster", "merchant", "guard", "teleporter", "warehouse"
	};

	private final Path datapackHtmlDir;
	private final Map<String, String> cache = new ConcurrentHashMap<>();
	private final Map<String, String> indexedFiles = new ConcurrentHashMap<>();
	private final Map<Integer, List<String>> questDirsById = new ConcurrentHashMap<>();
	private final Map<String, List<String>> scriptsDirsByCategory = new ConcurrentHashMap<>();

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
		indexQuestAndScriptDirectories(scriptsDir);
		log.info("HtmCache inicializado com diretorio: {} ({} arquivos indexados, {} quests mapeadas)",
				datapackHtmlDir.toAbsolutePath(), indexedFiles.size(), questDirsById.size());
	}

	public void reload() {
		cache.clear();
		indexedFiles.clear();
		questDirsById.clear();
		scriptsDirsByCategory.clear();
		indexHtmlFiles(this.datapackHtmlDir, this.datapackHtmlDir);
		Path scriptsDir = Path.of("data/scripts");
		if (Files.isDirectory(scriptsDir)) {
			indexHtmlFiles(scriptsDir, scriptsDir);
		}
		indexQuestAndScriptDirectories(scriptsDir);
		log.info("HtmCache recarregado: {} arquivos indexados, {} quests mapeadas",
				indexedFiles.size(), questDirsById.size());
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
					indexedFiles.compute(name, (k, existing) -> {
						if (existing == null) {
							return rel;
						}
						boolean existingIsQuest = existing.toLowerCase(java.util.Locale.ROOT).startsWith("quests/");
						boolean newIsQuest = rel.toLowerCase(java.util.Locale.ROOT).startsWith("quests/");
						if (existingIsQuest && !newIsQuest) {
							return rel;
						}
						return existing;
					});
				}
			});
		} catch (IOException e) {
			log.warn("Erro ao indexar arquivos HTML em {}: {}", dir, e.getMessage());
		}
	}

	private void indexQuestAndScriptDirectories(Path scriptsDir) {
		scanQuestSubdirs(datapackHtmlDir.resolve("quests"), "quests/");
		scanQuestSubdirs(datapackHtmlDir.resolve("scripts/quests"), "scripts/quests/");
		if (scriptsDir != null && Files.isDirectory(scriptsDir)) {
			scanQuestSubdirs(scriptsDir.resolve("quests"), "quests/");
			scanQuestSubdirs(scriptsDir.resolve("quests"), "scripts/quests/");
			try (var categories = Files.list(scriptsDir)) {
				categories.filter(Files::isDirectory).forEach(catDir -> {
					String category = catDir.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
					List<String> list = scriptsDirsByCategory.computeIfAbsent(category, k -> new CopyOnWriteArrayList<>());
					try (var subDirs = Files.list(catDir)) {
						subDirs.filter(Files::isDirectory).forEach(sub -> {
							String rel = normalize(scriptsDir.relativize(sub).toString());
							if (!list.contains(rel)) {
								list.add(rel);
							}
						});
					} catch (IOException ignored) {
					}
				});
			} catch (IOException ignored) {
			}
		}
	}

	private void scanQuestSubdirs(Path questsRoot, String prefix) {
		if (questsRoot == null || !Files.isDirectory(questsRoot)) {
			return;
		}
		try (var stream = Files.list(questsRoot)) {
			stream.filter(Files::isDirectory).forEach(subDir -> {
				String folderName = subDir.getFileName().toString();
				Matcher m = QUEST_ID_PREFIX_PATTERN.matcher(folderName);
				if (m.find()) {
					try {
						int qId = Integer.parseInt(m.group(1));
						if (qId > 0) {
							String relDir = prefix + folderName;
							List<String> dirs = questDirsById.computeIfAbsent(qId, k -> new CopyOnWriteArrayList<>());
							if (!dirs.contains(relDir)) {
								dirs.add(relDir);
							}
						}
					} catch (NumberFormatException ignored) {
					}
				}
			});
		} catch (IOException e) {
			log.warn("Erro ao escanear pastas de quests em {}: {}", questsRoot, e.getMessage());
		}
	}

	public Map<Integer, List<String>> getQuestDirsById() {
		return java.util.Collections.unmodifiableMap(questDirsById);
	}

	public Map<String, List<String>> getScriptsDirsByCategory() {
		return java.util.Collections.unmodifiableMap(scriptsDirsByCategory);
	}

	public String getQuestHtml(int questId, String questName, String fileName) {
		if (fileName == null || fileName.isBlank()) {
			return null;
		}
		String trimmed = fileName.trim();
		if (trimmed.contains("<html") || trimmed.contains("<HTML")
				|| trimmed.contains("<body") || trimmed.contains("<BODY")) {
			return fileName;
		}

		int effectiveQuestId = questId;
		if (effectiveQuestId <= 0 && questName != null && !questName.isBlank()) {
			Matcher m = QUEST_ID_PREFIX_PATTERN.matcher(questName.trim());
			if (m.find()) {
				try {
					effectiveQuestId = Integer.parseInt(m.group(1));
				} catch (NumberFormatException ignored) {
				}
			}
		}

		String normalizedInput = normalize(trimmed);
		int lastSlash = normalizedInput.lastIndexOf('/');
		String baseName = lastSlash >= 0 ? normalizedInput.substring(lastSlash + 1) : normalizedInput;
		List<String> fileVariants = buildFileNameVariants(baseName);

		// Passo 1: Se questId > 0, procura em todos os diretórios registrados em questDirsById.get(questId)
		if (effectiveQuestId > 0) {
			List<String> registeredDirs = questDirsById.get(effectiveQuestId);
			if (registeredDirs != null) {
				for (String dir : registeredDirs) {
					for (String variant : fileVariants) {
						String h = getHtml(dir + "/" + variant);
						if (h != null && !h.isBlank()) {
							return h;
						}
					}
				}
			}
		}

		// Passo 2: Procura por variações de pasta com questName
		Set<String> folderCandidates = buildQuestFolderCandidates(effectiveQuestId, questName);
		for (String folder : folderCandidates) {
			for (String variant : fileVariants) {
				String h = getHtml(folder + "/" + variant);
				if (h != null && !h.isBlank()) {
					return h;
				}
			}
		}

		// Passo 3: Se fileName começar com um npcId (ex: 30048-01.htm), tenta nas pastas de tipos de NPC
		for (String variant : fileVariants) {
			if (NPC_FILE_PREFIX_PATTERN.matcher(variant).matches()) {
				for (String npcFolder : NPC_FALLBACK_FOLDERS) {
					String h = getHtml(npcFolder + "/" + variant);
					if (h != null && !h.isBlank()) {
						return h;
					}
				}
			}
		}

		// Passo 4: Consulta getIndexedHtml e getHtml
		if (lastSlash >= 0) {
			String direct = getHtml(normalizedInput);
			if (direct != null && !direct.isBlank()) {
				return direct;
			}
		}
		for (String variant : fileVariants) {
			String indexed = getIndexedHtml(variant);
			if (indexed != null && !indexed.isBlank()) {
				return indexed;
			}
			String direct = getHtml(variant);
			if (direct != null && !direct.isBlank()) {
				return direct;
			}
		}

		return getIndexedHtml(normalizedInput);
	}

	private static List<String> buildFileNameVariants(String fileName) {
		Set<String> variants = new LinkedHashSet<>();
		String clean = fileName.trim();
		variants.add(clean);
		String lower = clean.toLowerCase(java.util.Locale.ROOT);
		variants.add(lower);

		if (!lower.endsWith(".htm") && !lower.endsWith(".html")) {
			variants.add(clean + ".htm");
			variants.add(lower + ".htm");
			variants.add(clean + ".html");
			variants.add(lower + ".html");
		}

		List<String> snapshot = new ArrayList<>(variants);
		for (String cand : snapshot) {
			Matcher zeroPad = FILE_ZERO_PAD_PATTERN.matcher(cand);
			if (zeroPad.matches()) {
				String unpadded = zeroPad.group(1) + "-" + zeroPad.group(2) + "." + zeroPad.group(3);
				variants.add(unpadded);
				variants.add(unpadded.toLowerCase(java.util.Locale.ROOT));
			} else {
				Matcher noPad = FILE_NO_PAD_PATTERN.matcher(cand);
				if (noPad.matches()) {
					String padded = noPad.group(1) + "-0" + noPad.group(2) + "." + noPad.group(3);
					variants.add(padded);
					variants.add(padded.toLowerCase(java.util.Locale.ROOT));
				}
			}
		}
		return new ArrayList<>(variants);
	}

	private static Set<String> buildQuestFolderCandidates(int questId, String questName) {
		Set<String> folders = new LinkedHashSet<>();
		if (questName != null && !questName.isBlank()) {
			String qn = questName.trim();
			String noUnder = qn.startsWith("_") ? qn.substring(1) : qn;
			String withUnder = qn.startsWith("_") ? qn : "_" + qn;

			folders.add("quests/" + qn);
			folders.add("scripts/quests/" + qn);
			folders.add("quests/" + noUnder);
			folders.add("scripts/quests/" + noUnder);
			folders.add("quests/" + withUnder);
			folders.add("scripts/quests/" + withUnder);

			int underscoreIdx = noUnder.indexOf('_');
			if (questId > 0 && underscoreIdx > 0) {
				String suffix = noUnder.substring(underscoreIdx);
				folders.add(String.format("quests/Q%03d%s", questId, suffix));
				folders.add(String.format("quests/q%03d%s", questId, suffix));
				folders.add(String.format("quests/%03d%s", questId, suffix));
				folders.add(String.format("quests/%d%s", questId, suffix));
				folders.add(String.format("scripts/quests/%d%s", questId, suffix));
				folders.add(String.format("scripts/quests/%03d%s", questId, suffix));
				folders.add(String.format("scripts/quests/Q%03d%s", questId, suffix));
			}

			folders.add("village_master/" + qn);
			folders.add("village_master/" + noUnder);
			folders.add("teleports/" + qn);
			folders.add("teleports/" + noUnder);
			folders.add("custom/" + qn);
			folders.add("custom/" + noUnder);
		}
		if (questId > 0) {
			folders.add("quests/" + questId);
			folders.add(String.format("quests/%03d", questId));
			folders.add(String.format("quests/Q%03d", questId));
			folders.add(String.format("quests/q%03d", questId));
		}
		return folders;
	}

	public String getHtml(String relativePath) {
		if (relativePath == null || relativePath.isBlank()) {
			return null;
		}
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

		// 1c. NPCs Buffer estilo Lucera (npc-40010.htm, npc-40011.htm)
		if (val == 0) {
			String luceraBufferHtm = getHtml("mods/buffer/npc-" + npcId + ".htm");
			if (luceraBufferHtm != null && !luceraBufferHtm.isBlank()) {
				return luceraBufferHtm;
			}
		}

		String folder = folderForType(npcType);
		String suffix = val > 0 ? "-" + val : "";

		// 2. Tenta na pasta do tipo especifico (ex: teleporter/30006.htm, mods/buffer/50016-1.htm)
		String pathType = folder + "/" + npcId + suffix + ".htm";
		String html = getHtml(pathType);
		if (html != null && !html.isBlank()) {
			return html;
		}
		if ("mods/buffer".equals(folder) && val > 0) {
			String luceraPage = getHtml("mods/buffer/" + val + ".htm");
			if (luceraPage != null && !luceraPage.isBlank()) {
				return luceraPage;
			}
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
					case 30599, 30370 -> getHtml("newbiehelper/guide_elf_roios/guide_elf_roios001.htm");
					case 30600, 30129 -> getHtml("newbiehelper/guide_delf_frankia/guide_delf_frankia001.htm");
					case 30601, 30528 -> getHtml("newbiehelper/guide_dwarf_gullin/guide_dwarf_gullin001.htm");
					case 30602, 30573 -> getHtml("newbiehelper/guide_orc_tanai/guide_orc_tanai001.htm");
					case 31076 -> getHtml("newbiehelper/guide_gludin_nina/guide_gludin_nina001.htm");
					case 31077 -> getHtml("newbiehelper/guide_gludio_euria/guide_gludio_euria001.htm");
					case 32327 -> getHtml("newbiehelper/blessing_benefector/blessing_benefector001.htm");
					default -> null;
				};
				if (h == null) {
					h = getHtml("newbiehelper/newbie_guide001.htm");
				}
				yield h;
			}
			case "mods/buffer" -> {
				String h = val > 0 ? getHtml("mods/buffer/50016" + suffix + ".htm") : getHtml("mods/buffer/50016.htm");
				if (h == null) {
					h = val > 0 ? getHtml("mods/buffer/" + val + ".htm") : getHtml("mods/buffer/npc-40010.htm");
				}
				yield h;
			}
			case "mods/buffervip" -> val > 0 ? getHtml("mods/buffervip/50005" + suffix + ".htm") : getHtml("mods/buffervip/50005.htm");
			default -> null;
		};
	}

	private static final java.util.regex.Pattern BRACKET_LINK_PATTERN =
			java.util.regex.Pattern.compile("\\[([^\\]\\|]*?)\\|([^\\]]*?)\\]", java.util.regex.Pattern.DOTALL);
	private static final java.util.regex.Pattern PTS_STAR_PATTERN =
			java.util.regex.Pattern.compile("(\\s|\"|'|\\(|^|\n)\\*(.*?)\\*(\\s|\"|'|\\)|\\?|\\.|!|:|;|,|$|\n)");
	private static final java.util.regex.Pattern PTS_DOLLAR_PATTERN =
			java.util.regex.Pattern.compile("(\\s|\"|'|\\(|^|\n)\\$(.*?)\\$(\\s|\"|'|\\)|\\?|\\.|!|:|;|,|$|\n)");
	private static final java.util.regex.Pattern PTS_TITLE_PATTERN =
			java.util.regex.Pattern.compile("^!(.*?)$", java.util.regex.Pattern.MULTILINE);
	private static final java.util.regex.Pattern PTS_BR1_PATTERN =
			java.util.regex.Pattern.compile("%%\\s*\n");
	private static final java.util.regex.Pattern PTS_BR_PATTERN =
			java.util.regex.Pattern.compile("\n\n+");

	public static String convertBracketLinks(String html) {
		if (html == null || html.isEmpty()) {
			return html;
		}
		String s = html;
		boolean hasHtmlTags = s.contains("<html") || s.contains("<HTML") || s.contains("<body") || s.contains("<BODY");
		boolean isPtsFormat = !hasHtmlTags && (s.startsWith("!") || s.contains("\n!") || s.contains("%%") || s.contains("\n\n"));
		if (isPtsFormat) {
			s = s.replace("\r", "");
			s = PTS_STAR_PATTERN.matcher(s).replaceAll("$1<font color=\"LEVEL\">$2</font>$3");
			s = PTS_DOLLAR_PATTERN.matcher(s).replaceAll("$1<font color=\"00FFFF\">$2</font>$3");
			s = PTS_TITLE_PATTERN.matcher(s).replaceAll("<font color=\"FFFFFF\">$1</font>\n\n");
			s = PTS_BR1_PATTERN.matcher(s).replaceAll("<br1>");
			s = PTS_BR_PATTERN.matcher(s).replaceAll("<br>");
		}
		if (s.contains("[") && s.contains("|")) {
			var matcher = BRACKET_LINK_PATTERN.matcher(s);
			var sb = new StringBuilder(s.length() + 64);
			while (matcher.find()) {
				String action = matcher.group(1).trim().replace(" @", "\" msg=\"");
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
			s = sb.toString();
		}
		if (isPtsFormat) {
			s = "<html><body>" + s + "</body></html>";
		}
		return s;
	}

	public String render(String rawHtml, int npcObjectId, String npcName, String playerName) {
		return render(rawHtml, npcObjectId, npcName, playerName, 1, 0, "");
	}

	public String render(String rawHtml, int npcObjectId, String npcName, String playerName, int castleId, int npcId) {
		return render(rawHtml, npcObjectId, npcName, playerName, castleId, npcId, "");
	}

	public String render(String rawHtml, int npcObjectId, String npcName, String playerName, int castleId, int npcId, String questName) {
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
				.replace("%playername%", playerName != null ? playerName : "")
				.replace("%player%", playerName != null ? playerName : "");
		int effectiveCastleId = castleId > 0 ? castleId : 1;
		s = s.replace("%castleid%", String.valueOf(effectiveCastleId))
			 .replace("%castle_id%", String.valueOf(effectiveCastleId));
		if (npcId > 0) {
			s = s.replace("%npcId%", String.valueOf(npcId))
				 .replace("%npc_id%", String.valueOf(npcId));
		}
		if (questName != null && !questName.isEmpty()) {
			s = s.replace("%QuestName%", questName)
				 .replace("%quest_name%", questName)
				 .replace("%questname%", questName);
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
			case "buffer", "npcbuffer", "custombuffer", "bufferevent" -> "mods/buffer";
			case "buffervip" -> "mods/buffervip";
			case "gmshop" -> "mods/gmshop";
			case "teleporter" -> "teleporter";
			case "castleteleporter" -> "castleteleporter";
			case "merchant" -> "merchant";
			case "petmanager" -> "petmanager";
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
