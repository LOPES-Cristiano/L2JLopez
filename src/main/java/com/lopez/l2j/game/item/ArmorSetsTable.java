package com.lopez.l2j.game.item;

import com.lopez.l2j.game.skill.StatFunc;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Tabela de bonus de conjuntos de armadura (armorsets.xml + custom_armorsets SQL).
 * Contem todos os conjuntos Interlude (No-Grade, D, C, B, A, S, Clan Oath e Apella),
 * skills de escudo, skills de enchant +6, skill_lvl e modificadores de atributos base (STR/CON/DEX/INT/WIT/MEN).
 */
@Component
public class ArmorSetsTable {

	private static final Logger log = LoggerFactory.getLogger(ArmorSetsTable.class);

	public record ArmorSet(
			int id,
			int chest,
			int legs,
			int head,
			int gloves,
			int feet,
			int skillId,
			int skillLvl,
			int skillSetId,
			int shield,
			int shieldSkillId,
			int enchant6Skill,
			int str,
			int con,
			int dex,
			int intStat,
			int wit,
			int men
	) {

		public ArmorSet(int id, int chest, int legs, int head, int gloves, int feet,
				int skillId, int shield, int shieldSkillId, int enchant6Skill) {
			this(id, chest, legs, head, gloves, feet, skillId, 1, 3006, shield, shieldSkillId, enchant6Skill,
					defaultStr(skillId), defaultCon(skillId), defaultDex(skillId),
					defaultInt(skillId), defaultWit(skillId), defaultMen(skillId));
		}

		public ArmorSet(int id, int chest, int legs, int head, int gloves, int feet,
				int skillId, int skillLvl, int shield, int shieldSkillId, int enchant6Skill) {
			this(id, chest, legs, head, gloves, feet, skillId, Math.max(1, skillLvl), 3006, shield, shieldSkillId, enchant6Skill,
					defaultStr(skillId), defaultCon(skillId), defaultDex(skillId),
					defaultInt(skillId), defaultWit(skillId), defaultMen(skillId));
		}

		public List<StatFunc> statFuncs() {
			List<StatFunc> funcs = new ArrayList<>();
			if (str != 0) funcs.add(new StatFunc("STR", StatFunc.Op.ADD, 0x10, str));
			if (con != 0) funcs.add(new StatFunc("CON", StatFunc.Op.ADD, 0x10, con));
			if (dex != 0) funcs.add(new StatFunc("DEX", StatFunc.Op.ADD, 0x10, dex));
			if (intStat != 0) funcs.add(new StatFunc("INT", StatFunc.Op.ADD, 0x10, intStat));
			if (wit != 0) funcs.add(new StatFunc("WIT", StatFunc.Op.ADD, 0x10, wit));
			if (men != 0) funcs.add(new StatFunc("MEN", StatFunc.Op.ADD, 0x10, men));
			return List.copyOf(funcs);
		}

		public boolean containAll(Inventory inv) {
			if (inv == null) {
				return false;
			}
			if (!inv.isEquipped(chest)) {
				return false;
			}
			if (legs != 0 && !inv.isEquipped(legs)) {
				return false;
			}
			if (head != 0 && !inv.isEquipped(head)) {
				return false;
			}
			if (gloves != 0 && !inv.isEquipped(gloves)) {
				return false;
			}
			if (feet != 0 && !inv.isEquipped(feet)) {
				return false;
			}
			return true;
		}

		public boolean containShield(Inventory inv) {
			return shield != 0 && inv != null && inv.isEquipped(shield);
		}

		public boolean hasShield(Inventory inv) {
			return containShield(inv);
		}

		public boolean isEnchanted6(Inventory inv) {
			return isEnchant6(inv);
		}

		public boolean isEnchant6(Inventory inv) {
			if (inv == null || !containAll(inv)) {
				return false;
			}
			if (getEnchant(inv, chest) < 6) return false;
			if (legs != 0 && getEnchant(inv, legs) < 6) return false;
			if (head != 0 && getEnchant(inv, head) < 6) return false;
			if (gloves != 0 && getEnchant(inv, gloves) < 6) return false;
			if (feet != 0 && getEnchant(inv, feet) < 6) return false;
			return true;
		}

		private static int getEnchant(Inventory inv, int itemId) {
			return inv.equippedItems().stream()
					.filter(it -> it.itemId() == itemId)
					.mapToInt(ItemInstance::enchant)
					.max()
					.orElse(0);
		}
	}

	private final Map<Integer, ArmorSet> byChest = new LinkedHashMap<>();
	private final List<ArmorSet> allSets = new ArrayList<>();

	public ArmorSetsTable() {
		this(Path.of("data/xml/player/armorsets.xml"), null);
	}

	@Autowired
	public ArmorSetsTable(@Value("${l2.datapack.xml-dir:data/xml}") String xmlDir,
						  @Autowired(required = false) JdbcClient jdbc) {
		this(Path.of(xmlDir, "player", "armorsets.xml"), jdbc);
	}

	public ArmorSetsTable(Path xmlFile) {
		this(xmlFile, null);
	}

	public ArmorSetsTable(Path xmlFile, JdbcClient jdbc) {
		loadBuiltInInterludeSets();
		if (xmlFile != null && Files.isRegularFile(xmlFile)) {
			load(xmlFile);
		} else if (xmlFile != null) {
			log.warn("Arquivo armorsets.xml nao encontrado em {}, usando catalogo Interlude embutido ({} sets)",
					xmlFile.toAbsolutePath(), byChest.size());
		}
		if (jdbc != null) {
			loadCustomArmorSetsFromDb(jdbc);
		}
		rebuildAllSetsList();
	}

	private void registerSet(ArmorSet set) {
		byChest.put(set.chest(), set);
	}

	private void rebuildAllSetsList() {
		allSets.clear();
		allSets.addAll(byChest.values());
	}

	public int size() {
		return allSets.size();
	}

	public Optional<ArmorSet> byChest(int chestId) {
		return Optional.ofNullable(byChest.get(chestId));
	}

	public List<ArmorSet> all() {
		return Collections.unmodifiableList(allSets);
	}

	/**
	 * Retorna o conjunto completo ativo para o inventario informado (ou empty).
	 */
	public Optional<ArmorSet> activeSet(Inventory inv) {
		if (inv == null) {
			return Optional.empty();
		}
		for (ItemInstance eq : inv.equippedItems()) {
			ArmorSet set = byChest.get(eq.itemId());
			if (set != null && set.containAll(inv)) {
				return Optional.of(set);
			}
		}
		return Optional.empty();
	}

	public ArmorSet findMatchingSet(Inventory inv) {
		return activeSet(inv).orElse(null);
	}

	private void loadBuiltInInterludeSets() {
		// No-Grade (2 sets)
		registerSet(new ArmorSet(1, 23, 2386, 43, 0, 0, 3500, 0, 0, 0));
		registerSet(new ArmorSet(2, 1101, 1104, 44, 0, 0, 3501, 0, 0, 0));
		// D-Grade (6 sets)
		registerSet(new ArmorSet(3, 58, 59, 47, 0, 0, 3502, 628, 3543, 3611));
		registerSet(new ArmorSet(4, 352, 2378, 2411, 0, 0, 3506, 2493, 3544, 3611));
		registerSet(new ArmorSet(5, 394, 416, 0, 0, 2422, 3503, 0, 0, 3612));
		registerSet(new ArmorSet(6, 395, 417, 0, 0, 2424, 3505, 0, 0, 3612));
		registerSet(new ArmorSet(7, 436, 469, 0, 2447, 0, 3504, 0, 0, 3613));
		registerSet(new ArmorSet(8, 437, 470, 0, 2450, 0, 3507, 0, 0, 3613));
		// C-Grade (10 sets)
		registerSet(new ArmorSet(9, 354, 381, 2413, 0, 0, 3509, 2495, 3545, 3614));
		registerSet(new ArmorSet(10, 60, 0, 517, 0, 0, 3512, 107, 3546, 3614));
		registerSet(new ArmorSet(11, 356, 0, 2414, 0, 0, 3516, 2497, 3547, 3614));
		registerSet(new ArmorSet(12, 397, 2387, 0, 0, 62, 3508, 0, 0, 3615));
		registerSet(new ArmorSet(13, 398, 418, 0, 0, 2431, 3511, 0, 0, 3615));
		registerSet(new ArmorSet(14, 400, 420, 0, 0, 2436, 3514, 0, 0, 3615));
		registerSet(new ArmorSet(15, 401, 0, 0, 0, 2437, 3515, 0, 0, 3615));
		registerSet(new ArmorSet(16, 439, 471, 0, 2454, 0, 3510, 0, 0, 3616));
		registerSet(new ArmorSet(17, 441, 472, 0, 2459, 0, 3513, 0, 0, 3616));
		registerSet(new ArmorSet(18, 442, 473, 0, 2463, 0, 3517, 0, 0, 3616));
		// B-Grade (12 sets)
		registerSet(new ArmorSet(19, 357, 383, 503, 5710, 5726, 3518, 0, 0, 3617));
		registerSet(new ArmorSet(20, 2384, 2388, 503, 5711, 5727, 3520, 0, 0, 3618));
		registerSet(new ArmorSet(21, 2397, 2402, 503, 5712, 5728, 3522, 0, 0, 3619));
		registerSet(new ArmorSet(22, 2376, 2379, 2415, 5714, 5730, 3519, 673, 3548, 3617));
		registerSet(new ArmorSet(23, 2390, 0, 2415, 5715, 5731, 3521, 0, 0, 3618));
		registerSet(new ArmorSet(24, 2406, 0, 2415, 5716, 5732, 3523, 0, 0, 3619));
		registerSet(new ArmorSet(25, 358, 2380, 2416, 5718, 5734, 3524, 0, 0, 3617));
		registerSet(new ArmorSet(26, 2391, 0, 2416, 5719, 5735, 3526, 0, 0, 3618));
		registerSet(new ArmorSet(27, 2398, 2403, 2416, 5720, 5736, 3528, 0, 0, 3619));
		registerSet(new ArmorSet(28, 2381, 0, 2417, 5722, 5738, 3525, 110, 3549, 3617));
		registerSet(new ArmorSet(29, 2392, 0, 2417, 5723, 5739, 3527, 0, 0, 3618));
		registerSet(new ArmorSet(30, 2399, 2404, 2417, 5724, 5740, 3529, 0, 0, 3619));
		// A-Grade (12 sets)
		registerSet(new ArmorSet(31, 365, 388, 512, 5765, 5777, 3530, 641, 3550, 3620));
		registerSet(new ArmorSet(32, 2385, 2389, 512, 5766, 5778, 3532, 0, 0, 3621));
		registerSet(new ArmorSet(33, 2407, 0, 512, 5767, 5779, 3535, 0, 0, 3622));
		registerSet(new ArmorSet(34, 2382, 0, 547, 5768, 5780, 3531, 0, 0, 3620));
		registerSet(new ArmorSet(35, 2393, 0, 547, 5769, 5781, 3533, 0, 0, 3621));
		registerSet(new ArmorSet(36, 2400, 2405, 547, 5770, 5782, 3534, 0, 0, 3622));
		registerSet(new ArmorSet(37, 374, 0, 2418, 5771, 5783, 3536, 2498, 3551, 3620));
		registerSet(new ArmorSet(38, 2394, 0, 2418, 5772, 5784, 3538, 0, 0, 3621));
		registerSet(new ArmorSet(39, 2408, 0, 2418, 5773, 5785, 3540, 0, 0, 3622));
		registerSet(new ArmorSet(40, 2383, 0, 2419, 5774, 5786, 3537, 0, 0, 3620));
		registerSet(new ArmorSet(41, 2395, 0, 2419, 5775, 5787, 3539, 0, 0, 3621));
		registerSet(new ArmorSet(42, 2409, 0, 2419, 5776, 5788, 3541, 0, 0, 3622));
		// S-Grade (3 sets)
		registerSet(new ArmorSet(43, 6373, 6374, 6378, 6375, 6376, 3553, 6377, 3554, 3623));
		registerSet(new ArmorSet(44, 6379, 0, 6382, 6380, 6381, 3555, 0, 0, 3624));
		registerSet(new ArmorSet(45, 6383, 0, 6386, 6384, 6385, 3556, 0, 0, 3625));
		// Clan Oath & Apella (6 sets)
		registerSet(new ArmorSet(46, 7851, 0, 7850, 7852, 7853, 3605, 0, 0, 3611));
		registerSet(new ArmorSet(47, 7854, 0, 7850, 7855, 7856, 3606, 0, 0, 3612));
		registerSet(new ArmorSet(48, 7857, 0, 7850, 7858, 7859, 3607, 0, 0, 3613));
		registerSet(new ArmorSet(49, 7861, 0, 7860, 7862, 7863, 3608, 0, 0, 3620));
		registerSet(new ArmorSet(50, 7864, 0, 7860, 7865, 7866, 3609, 0, 0, 3621));
		registerSet(new ArmorSet(51, 7867, 0, 7860, 7868, 7869, 3610, 0, 0, 3622));
	}

	private void load(Path file) {
		try (InputStream in = Files.newInputStream(file)) {
			var factory = DocumentBuilderFactory.newInstance();
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			var doc = factory.newDocumentBuilder().parse(in);

			NodeList nodes = doc.getElementsByTagName("armorset");
			for (int i = 0; i < nodes.getLength(); i++) {
				Element e = (Element) nodes.item(i);
				int id = parseIntAttr(e, "id", i + 1);
				int chest = parseIntAttr(e, "chest", 0);
				int legs = parseIntAttr(e, "legs", 0);
				int head = parseIntAttr(e, "head", 0);
				int gloves = parseIntAttr(e, "gloves", 0);
				int feet = parseIntAttr(e, "feet", 0);

				String rawSkill = e.getAttribute("skill_id");
				int skillId = 0;
				int skillLvl = parseIntAttr(e, "skill_lvl", 1);
				if (rawSkill != null && !rawSkill.isBlank()) {
					String clean = rawSkill.trim().replace(";", "");
					int dash = clean.indexOf('-');
					if (dash > 0) {
						skillId = Integer.parseInt(clean.substring(0, dash).trim());
						skillLvl = Integer.parseInt(clean.substring(dash + 1).trim());
					} else {
						skillId = Integer.parseInt(clean);
					}
				}
				int skillSetId = parseIntAttr(e, "skillset_id", 3006);
				int shield = parseIntAttr(e, "shield", 0);
				int shieldSkillId = parseIntAttr(e, "shield_skill_id", 0);
				int enchant6Skill = parseIntAttr(e, "enchant6skill", 0);

				int str = parseIntAttr(e, "str", defaultStr(skillId));
				int con = parseIntAttr(e, "con", defaultCon(skillId));
				int dex = parseIntAttr(e, "dex", defaultDex(skillId));
				int intStat = parseIntAttr(e, "int", defaultInt(skillId));
				int wit = parseIntAttr(e, "wit", defaultWit(skillId));
				int men = parseIntAttr(e, "men", defaultMen(skillId));

				ArmorSet set = new ArmorSet(id, chest, legs, head, gloves, feet,
						skillId, Math.max(1, skillLvl), skillSetId, shield, shieldSkillId, enchant6Skill,
						str, con, dex, intStat, wit, men);
				registerSet(set);
			}

			log.info("ArmorSetsTable: {} conjuntos de armadura disponiveis apos ler {}", byChest.size(), file.getFileName());
		} catch (Exception ex) {
			log.error("Erro ao carregar armorsets de {}: {}", file, ex.getMessage(), ex);
		}
	}

	private void loadCustomArmorSetsFromDb(JdbcClient jdbc) {
		try {
			List<Map<String, Object>> rows = jdbc.sql("SELECT * FROM custom_armorsets").query().listOfRows();
			for (var r : rows) {
				int id = ((Number) r.get("id")).intValue();
				int chest = ((Number) r.get("chest")).intValue();
				int legs = r.get("legs") != null ? ((Number) r.get("legs")).intValue() : 0;
				int head = r.get("head") != null ? ((Number) r.get("head")).intValue() : 0;
				int gloves = r.get("gloves") != null ? ((Number) r.get("gloves")).intValue() : 0;
				int feet = r.get("feet") != null ? ((Number) r.get("feet")).intValue() : 0;
				int skillId = r.get("skill_id") != null ? ((Number) r.get("skill_id")).intValue() : 0;
				int skillLvl = r.get("skill_lvl") != null ? Math.max(1, ((Number) r.get("skill_lvl")).intValue()) : 1;
				int skillSetId = r.get("skillset_id") != null ? ((Number) r.get("skillset_id")).intValue() : 3006;
				int shield = r.get("shield") != null ? ((Number) r.get("shield")).intValue() : 0;
				int shieldSkillId = r.get("shield_skill_id") != null ? ((Number) r.get("shield_skill_id")).intValue() : 0;
				int enchant6 = r.get("enchant6skill") != null ? ((Number) r.get("enchant6skill")).intValue() : 0;
				registerSet(new ArmorSet(id, chest, legs, head, gloves, feet,
						skillId, skillLvl, skillSetId, shield, shieldSkillId, enchant6,
						defaultStr(skillId), defaultCon(skillId), defaultDex(skillId),
						defaultInt(skillId), defaultWit(skillId), defaultMen(skillId)));
			}
		} catch (Exception ex) {
			log.debug("custom_armorsets indisponivel: {}", ex.getMessage());
		}
	}

	private static int parseIntAttr(Element e, String name, int def) {
		if (!e.hasAttribute(name)) {
			return def;
		}
		try {
			return Integer.parseInt(e.getAttribute(name).trim());
		} catch (NumberFormatException ex) {
			return def;
		}
	}

	private static int defaultStr(int skillId) {
		return switch (skillId) {
			case 3511 -> 4;  // Plated Leather (+4 STR, -1 CON)
			case 3515 -> -2; // Drake Leather (+2 DEX, -2 STR)
			case 3524 -> 3;  // Blue Wolf Heavy (+3 STR, -1 CON, -2 DEX)
			case 3525 -> -3; // Doom Heavy (+3 CON, -3 STR)
			case 3527 -> -1; // Doom Light (+3 DEX, -2 CON, -1 STR)
			case 3530 -> -2; // Dark Crystal Heavy (+2 CON, -2 STR)
			case 3532 -> 1;  // Dark Crystal Light (+1 STR, +1 DEX, -2 CON)
			case 3531 -> 2;  // Tallum Heavy (+2 STR, -2 CON)
			case 3537 -> 2;  // Majestic Heavy (+2 STR, -2 CON)
			case 3553 -> 2;  // Imperial Crusader (+2 STR, -2 DEX)
			case 3555 -> 1;  // Draconic Leather (+1 DEX, +1 STR, -2 CON)
			default -> 0;
		};
	}

	private static int defaultCon(int skillId) {
		return switch (skillId) {
			case 3511 -> -1; // Plated Leather
			case 3524 -> -1; // Blue Wolf Heavy
			case 3525 -> 3;  // Doom Heavy
			case 3527 -> -2; // Doom Light
			case 3530 -> 2;  // Dark Crystal Heavy
			case 3532 -> -2; // Dark Crystal Light
			case 3531 -> -2; // Tallum Heavy
			case 3536 -> 2;  // Nightmare Heavy (+2 CON, -2 DEX)
			case 3538 -> -1; // Nightmare Light (+1 DEX, -1 CON)
			case 3537 -> -2; // Majestic Heavy
			case 3539 -> -1; // Majestic Light (+1 DEX, -1 CON)
			case 3555 -> -2; // Draconic Leather
			default -> 0;
		};
	}

	private static int defaultDex(int skillId) {
		return switch (skillId) {
			case 3515 -> 2;  // Drake Leather
			case 3524 -> -2; // Blue Wolf Heavy
			case 3527 -> 3;  // Doom Light
			case 3532 -> 1;  // Dark Crystal Light
			case 3536 -> -2; // Nightmare Heavy
			case 3538 -> 1;  // Nightmare Light
			case 3539 -> 1;  // Majestic Light
			case 3553 -> -2; // Imperial Crusader
			case 3555 -> 1;  // Draconic Leather
			default -> 0;
		};
	}

	private static int defaultInt(int skillId) {
		return switch (skillId) {
			case 3504 -> 2;  // Knowledge (+2 INT, -2 MEN)
			case 3513 -> 4;  // Demon (+4 INT, -1 WIT)
			case 3517 -> -1; // Divine (+1 WIT, -1 INT)
			case 3526 -> -2; // Blue Wolf Light (+3 MEN, -2 INT, -1 WIT)
			case 3528 -> -2; // Blue Wolf Robe (+3 WIT, -2 INT, -1 MEN)
			case 3529 -> 2;  // Doom Robe (+2 INT, +1 MEN, -3 WIT)
			case 3534 -> -2; // Tallum Robe (+2 WIT, -2 INT)
			case 3540 -> 2;  // Nightmare Robe (+2 INT, -2 WIT)
			case 3541 -> -1; // Majestic Robe (+1 MEN, -1 INT)
			case 3556 -> 1;  // Major Arcana (+1 WIT, +1 INT, -2 MEN)
			default -> 0;
		};
	}

	private static int defaultWit(int skillId) {
		return switch (skillId) {
			case 3507 -> 1;  // Mithril Tunic (+1 WIT, -1 INT)
			case 3513 -> -1; // Demon
			case 3517 -> 1;  // Divine
			case 3523 -> 2;  // Avadon Robe
			case 3526 -> -1; // Blue Wolf Light
			case 3528 -> 3;  // Blue Wolf Robe
			case 3529 -> -3; // Doom Robe
			case 3535 -> 2;  // Dark Crystal Robe (+2 WIT, -2 MEN)
			case 3533 -> -2; // Tallum Light (+2 MEN, -2 WIT)
			case 3534 -> 2;  // Tallum Robe
			case 3540 -> -2; // Nightmare Robe
			case 3556 -> 1;  // Major Arcana
			default -> 0;
		};
	}

	private static int defaultMen(int skillId) {
		return switch (skillId) {
			case 3504 -> -2; // Knowledge
			case 3523 -> -2; // Avadon Robe
			case 3526 -> 3;  // Blue Wolf Light
			case 3528 -> -1; // Blue Wolf Robe
			case 3529 -> 1;  // Doom Robe
			case 3535 -> -2; // Dark Crystal Robe
			case 3533 -> 2;  // Tallum Light
			case 3541 -> 1;  // Majestic Robe
			case 3556 -> -2; // Major Arcana
			default -> 0;
		};
	}
}
