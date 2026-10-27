package com.lopez.l2j.game.recipe;

import com.lopez.l2j.game.recipe.RecipeEntry.Ingredient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servico de gerenciamento de receitas de Crafting (Dwarven e Common) e livros
 * de receitas (RecipeBook).
 * Carrega receitas de XML (data/xml/recipes.xml ou data/recipes.xml) com
 * catalogo fallback embutido.
 */
@Service
public class RecipeService {

	private static final Logger log = LoggerFactory.getLogger(RecipeService.class);
	private static volatile RecipeService instance;

	private final Map<Integer, RecipeEntry> recipesById = new ConcurrentHashMap<>();
	private final Map<Integer, RecipeEntry> recipesByItemId = new ConcurrentHashMap<>();
	private final Map<Integer, Set<Integer>> dwarvenBookByChar = new ConcurrentHashMap<>();
	private final Map<Integer, Set<Integer>> commonBookByChar = new ConcurrentHashMap<>();
	private final org.springframework.jdbc.core.simple.JdbcClient jdbc;

	public RecipeService() {
		this(null);
	}

	@org.springframework.beans.factory.annotation.Autowired
	public RecipeService(
			@org.springframework.beans.factory.annotation.Autowired(required = false) org.springframework.jdbc.core.simple.JdbcClient jdbc) {
		this.jdbc = jdbc;
		instance = this;
		initSchema();
		loadRecipes();
	}

	public void initSchema() {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("""
					CREATE TABLE IF NOT EXISTS character_recipebook (
					  charId INT NOT NULL DEFAULT 0,
					  id DECIMAL(11,0) NOT NULL DEFAULT 0,
					  type INT NOT NULL DEFAULT 0,
					  PRIMARY KEY (id, charId)
					)
					""").update();
		} catch (Exception ex) {
			log.debug("character_recipebook schema init: {}", ex.getMessage());
		}
	}

	public static RecipeService getInstance() {
		if (instance == null) {
			synchronized (RecipeService.class) {
				if (instance == null) {
					instance = new RecipeService();
				}
			}
		}
		return instance;
	}

	public final void loadRecipes() {
		loadBuiltInFallbackCatalog();

		Path[] candidatePaths = new Path[] {
				Path.of("data", "xml", "world", "recipes.xml"),
				Path.of("data", "xml", "recipes.xml"),
				Path.of("data", "recipes.xml"),
				Path.of("L2JLopez", "data", "xml", "world", "recipes.xml"),
				Path.of("L2JLopez", "data", "xml", "recipes.xml")
		};
		for (Path path : candidatePaths) {
			if (Files.isRegularFile(path)) {
				int loaded = parseXmlFile(path.toFile());
				if (loaded > 0) {
					log.info("RecipeService: {} receitas carregadas de {}", loaded, path);
				}
			}
		}

		String[] classpathCandidates = new String[] {
				"/data/xml/world/recipes.xml",
				"/data/xml/recipes.xml",
				"/data/recipes.xml"
		};
		for (String cp : classpathCandidates) {
			try (InputStream in = RecipeService.class.getResourceAsStream(cp)) {
				if (in != null) {
					int loaded = parseXmlStream(in);
					if (loaded > 0) {
						log.info("RecipeService: {} receitas carregadas do classpath {}", loaded, cp);
					}
				}
			} catch (Exception ignored) {
			}
		}
	}

	private int parseXmlFile(File file) {
		try (InputStream in = Files.newInputStream(file.toPath())) {
			return parseXmlStream(in);
		} catch (Exception e) {
			log.warn("Falha ao ler arquivo de receitas {}: {}", file, e.getMessage());
			return 0;
		}
	}

	private int parseXmlStream(InputStream in) {
		int count = 0;
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(false);
			Document doc = dbf.newDocumentBuilder().parse(in);
			Element root = doc.getDocumentElement();
			if (root == null) {
				return 0;
			}
			NodeList children = root.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node node = children.item(i);
				if (node.getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}
				Element el = (Element) node;
				String tag = el.getTagName().toLowerCase();
				if (!"item".equals(tag) && !"recipe".equals(tag)) {
					continue;
				}
				RecipeEntry entry = parseRecipeElement(el);
				if (entry != null) {
					addRecipeToCatalog(entry);
					count++;
				}
			}
		} catch (Exception e) {
			log.warn("Erro ao fazer parse de XML de receitas: {}", e.getMessage());
		}
		return count;
	}

	private RecipeEntry parseRecipeElement(Element el) {
		try {
			int id = parseIntAttr(el, "id", 0);
			if (id <= 0) {
				return null;
			}
			String name = el.hasAttribute("name") ? el.getAttribute("name")
					: el.getAttribute("recipeName");
			int recipeItemId = parseIntAttr(el, "recipeId", parseIntAttr(el, "recipeItemId", 0));
			int level = parseIntAttr(el, "level", parseIntAttr(el, "craftLevel", 1));
			int mpCost = parseIntAttr(el, "mpCost", parseIntAttr(el, "mp", 30));
			int successRate = parseIntAttr(el, "successRate", parseIntAttr(el, "rate", 100));
			String typeAttr = el.getAttribute("type");
			boolean isDwarven = el.hasAttribute("isDwarven")
					? Boolean.parseBoolean(el.getAttribute("isDwarven"))
					: !"common".equalsIgnoreCase(typeAttr);

			int productId = parseIntAttr(el, "productId", 0);
			int productCount = parseIntAttr(el, "productCount", 1);
			List<Ingredient> ingredients = new ArrayList<>();

			NodeList subNodes = el.getChildNodes();
			for (int i = 0; i < subNodes.getLength(); i++) {
				Node n = subNodes.item(i);
				if (n.getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}
				Element child = (Element) n;
				String cTag = child.getTagName().toLowerCase();
				if ("production".equals(cTag) || "product".equals(cTag)) {
					productId = parseIntAttr(child, "id", parseIntAttr(child, "itemId", productId));
					productCount = parseIntAttr(child, "count", productCount);
				} else if ("recipe".equals(cTag)) {
					recipeItemId = parseIntAttr(child, "id", parseIntAttr(child, "itemId", recipeItemId));
					level = parseIntAttr(child, "level", parseIntAttr(child, "craftLevel", level));
					if (child.hasAttribute("type")) {
						isDwarven = !"common".equalsIgnoreCase(child.getAttribute("type"));
					}
				} else if ("mpcost".equals(cTag) || "mp".equals(cTag)) {
					try {
						mpCost = Integer.parseInt(child.getTextContent().trim());
					} catch (NumberFormatException ignored) {
					}
				} else if ("successrate".equals(cTag) || "rate".equals(cTag)) {
					try {
						successRate = Integer.parseInt(child.getTextContent().trim());
					} catch (NumberFormatException ignored) {
					}
				} else if ("ingredient".equals(cTag)) {
					int ingId = parseIntAttr(child, "id", parseIntAttr(child, "itemId", 0));
					int ingCount = parseIntAttr(child, "count", 1);
					if (ingId > 0 && ingCount > 0) {
						ingredients.add(new Ingredient(ingId, ingCount));
					}
				} else if ("statuse".equals(cTag) || "stat".equals(cTag)) {
					String statName = child.getAttribute("name");
					if ("MP".equalsIgnoreCase(statName)) {
						mpCost = parseIntAttr(child, "value", mpCost);
					}
				}
			}

			if (productId <= 0) {
				return null;
			}
			return new RecipeEntry(id, name, recipeItemId, level, productId, Math.max(1, productCount),
					mpCost, Math.clamp(successRate, 1, 100), isDwarven, ingredients);
		} catch (Exception e) {
			return null;
		}
	}

	private static int parseIntAttr(Element el, String attr, int def) {
		if (!el.hasAttribute(attr)) {
			return def;
		}
		try {
			return Integer.parseInt(el.getAttribute(attr).trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private void addRecipeToCatalog(RecipeEntry entry) {
		recipesById.put(entry.id(), entry);
		if (entry.recipeItemId() > 0) {
			recipesByItemId.put(entry.recipeItemId(), entry);
		}
	}

	private void loadBuiltInFallbackCatalog() {
		// Materiais basicos classicos (IDs oficiais do Interlude recipe-c.dat /
		// recipes.xml)
		addRecipeToCatalog(new RecipeEntry(1, "mk_wooden_arrow", 1666, 1, 17, 500, 30, 100, true,
				List.of(new Ingredient(1864, 4), new Ingredient(1869, 2)))); // Wooden Arrow x500
		addRecipeToCatalog(new RecipeEntry(2, "mk_broad_sword", 1786, 1, 3, 1, 30, 100, true,
				List.of(new Ingredient(2005, 1), new Ingredient(1869, 18), new Ingredient(1870, 18)))); // Broad Sword
		addRecipeToCatalog(new RecipeEntry(18, "mk_necklace_of_anguish", 1802, 1, 907, 1, 45, 100, true,
				List.of(new Ingredient(1896, 2), new Ingredient(1864, 20), new Ingredient(1865, 4)))); // Necklace of
																										// Anguish
		addRecipeToCatalog(new RecipeEntry(19, "mk_necklace_of_wisdom", 1803, 1, 908, 1, 60, 100, true,
				List.of(new Ingredient(1897, 3), new Ingredient(1864, 50), new Ingredient(1865, 10)))); // Necklace of
																										// Wisdom

		// Soulshots (D / C / B / A / S) - IDs 20 a 24
		addRecipeToCatalog(new RecipeEntry(20, "mk_soulshot_d", 1804, 2, 1463, 156, 40, 100, true,
				List.of(new Ingredient(1785, 3), new Ingredient(1458, 1)))); // Soul Ore x3 + Crystal D x1
		addRecipeToCatalog(new RecipeEntry(21, "mk_soulshot_c", 1805, 4, 1464, 952, 70, 100, true,
				List.of(new Ingredient(1785, 15), new Ingredient(1459, 1)))); // Soul Ore x15 + Crystal C x1
		addRecipeToCatalog(new RecipeEntry(22, "mk_soulshot_b", 1806, 6, 1465, 450, 100, 100, true,
				List.of(new Ingredient(1785, 54), new Ingredient(1460, 1)))); // Soul Ore x54 + Crystal B x1
		addRecipeToCatalog(new RecipeEntry(23, "mk_soulshot_a", 1807, 7, 1466, 300, 120, 100, true,
				List.of(new Ingredient(1785, 36), new Ingredient(1461, 1)))); // Soul Ore x36 + Crystal A x1
		addRecipeToCatalog(new RecipeEntry(24, "mk_soulshot_s", 1808, 8, 1467, 350, 150, 100, true,
				List.of(new Ingredient(1785, 40), new Ingredient(1462, 1)))); // Soul Ore x40 + Crystal S x1

		// Materiais basicos (IDs 25 a 42, 607, 608)
		addRecipeToCatalog(new RecipeEntry(25, "mk_leather", 1814, 1, 1882, 1, 10, 100, true,
				List.of(new Ingredient(1867, 6)))); // Animal Skin x6 -> Leather
		addRecipeToCatalog(new RecipeEntry(26, "mk_cord", 1817, 2, 1884, 20, 20, 100, true,
				List.of(new Ingredient(1880, 2), new Ingredient(1868, 25)))); // Steel x2 + Thread x25 -> Cord x20
		addRecipeToCatalog(new RecipeEntry(27, "mk_oriharukon", 1825, 4, 1893, 1, 40, 100, true,
				List.of(new Ingredient(1888, 1), new Ingredient(1873, 12), new Ingredient(1874, 4)))); // Oriharukon
		addRecipeToCatalog(new RecipeEntry(28, "mk_braided_hemp", 2135, 1, 1878, 1, 10, 100, true,
				List.of(new Ingredient(1864, 5)))); // Stem x5 -> Braided Hemp
		addRecipeToCatalog(new RecipeEntry(29, "mk_cokes", 2136, 1, 1879, 1, 10, 100, true,
				List.of(new Ingredient(1870, 3), new Ingredient(1871, 3)))); // Coal x3 + Charcoal x3 -> Cokes
		addRecipeToCatalog(new RecipeEntry(30, "mk_steel", 2137, 1, 1880, 1, 10, 100, true,
				List.of(new Ingredient(1865, 5), new Ingredient(1869, 5)))); // Varnish x5 + Iron Ore x5 -> Steel
		addRecipeToCatalog(new RecipeEntry(31, "mk_coarse_bone_powder", 2138, 1, 1881, 1, 10, 100, true,
				List.of(new Ingredient(1872, 10)))); // Animal Bone x10 -> Coarse Bone Powder
		addRecipeToCatalog(new RecipeEntry(32, "mk_steel_mold", 2139, 2, 1883, 1, 20, 100, true,
				List.of(new Ingredient(1878, 5), new Ingredient(1869, 5), new Ingredient(1870, 5)))); // Steel Mold
		addRecipeToCatalog(new RecipeEntry(33, "mk_high_grade_suede", 2140, 2, 1885, 1, 20, 100, true,
				List.of(new Ingredient(1881, 1), new Ingredient(1866, 3)))); // High Grade Suede
		addRecipeToCatalog(new RecipeEntry(34, "mk_silver_mold", 2141, 2, 1886, 1, 20, 100, true,
				List.of(new Ingredient(1878, 5), new Ingredient(1879, 5), new Ingredient(1873, 10)))); // Silver Mold
		addRecipeToCatalog(new RecipeEntry(35, "mk_varnish_of_purity", 2142, 2, 1887, 1, 20, 100, true,
				List.of(new Ingredient(1881, 3), new Ingredient(1865, 3), new Ingredient(1875, 1)))); // Varnish of
																										// Purity
		addRecipeToCatalog(new RecipeEntry(36, "mk_synthesis_cokes", 2143, 2, 1888, 1, 20, 100, true,
				List.of(new Ingredient(1879, 3), new Ingredient(1874, 1)))); // Synthetic Cokes
		addRecipeToCatalog(new RecipeEntry(37, "mk_compound_braid", 2144, 2, 1889, 1, 20, 100, true,
				List.of(new Ingredient(1878, 5), new Ingredient(1868, 5)))); // Compound Braid
		addRecipeToCatalog(new RecipeEntry(38, "mk_mithirl_alloy", 2145, 4, 1890, 1, 40, 100, true,
				List.of(new Ingredient(1887, 1), new Ingredient(1880, 2), new Ingredient(1876, 1)))); // Mithril Alloy
		addRecipeToCatalog(new RecipeEntry(39, "mk_artisan's_frame", 2146, 4, 1891, 1, 40, 100, true,
				List.of(new Ingredient(1883, 1), new Ingredient(1887, 5), new Ingredient(1877, 10)))); // Artisan's
																										// Frame
		addRecipeToCatalog(new RecipeEntry(40, "mk_blacksmith's_frame", 2147, 4, 1892, 1, 40, 100, true,
				List.of(new Ingredient(1886, 1), new Ingredient(1887, 5), new Ingredient(1876, 10)))); // Blacksmith's
																										// Frame
		addRecipeToCatalog(new RecipeEntry(41, "mk_crafted_leather", 2148, 4, 1894, 1, 40, 100, true,
				List.of(new Ingredient(1884, 4), new Ingredient(1882, 4), new Ingredient(1870, 4)))); // Crafted Leather
		addRecipeToCatalog(new RecipeEntry(42, "mk_metallic_fiber", 2149, 4, 1895, 20, 40, 100, true,
				List.of(new Ingredient(1884, 20), new Ingredient(1873, 15)))); // Metallic Fiber x20
		addRecipeToCatalog(new RecipeEntry(607, "mk_iron_thread", 5472, 4, 5549, 1, 40, 100, true,
				List.of(new Ingredient(1868, 10), new Ingredient(1869, 5)))); // Metallic Thread
		addRecipeToCatalog(new RecipeEntry(608, "mk_reinforcing_plate", 5473, 4, 5550, 1, 40, 100, true,
				List.of(new Ingredient(5549, 5), new Ingredient(1876, 5)))); // Durable Metal Plate

		// Spiritshots (D / C / B / A / S) - IDs 317 a 321
		addRecipeToCatalog(new RecipeEntry(317, "mk_spiritshot_d", 3032, 2, 2510, 100, 60, 100, true,
				List.of(new Ingredient(3031, 3), new Ingredient(1458, 1)))); // Spirit Ore x3 + Crystal D x1
		addRecipeToCatalog(new RecipeEntry(318, "mk_spiritshot_c", 3033, 4, 2511, 200, 120, 100, true,
				List.of(new Ingredient(3031, 10), new Ingredient(1459, 1)))); // Spirit Ore x10 + Crystal C x1
		addRecipeToCatalog(new RecipeEntry(319, "mk_spiritshot_b", 3034, 6, 2512, 150, 180, 100, true,
				List.of(new Ingredient(3031, 15), new Ingredient(1460, 1)))); // Spirit Ore x15 + Crystal B x1
		addRecipeToCatalog(new RecipeEntry(320, "mk_spiritshot_a", 3035, 7, 2513, 200, 240, 100, true,
				List.of(new Ingredient(3031, 22), new Ingredient(1461, 1)))); // Spirit Ore x22 + Crystal A x1
		addRecipeToCatalog(new RecipeEntry(321, "mk_spiritshot_s", 3036, 8, 2514, 200, 300, 100, true,
				List.of(new Ingredient(3031, 15), new Ingredient(1462, 1)))); // Spirit Ore x15 + Crystal S x1

		// Blessed Spiritshots (D / C / B / A / S) - IDs 323 a 327
		addRecipeToCatalog(new RecipeEntry(323, "mk_blessed_spiritshot_d", 3953, 2, 3948, 100, 60, 100, true,
				List.of(new Ingredient(3031, 8), new Ingredient(1458, 2)))); // Spirit Ore x8 + Crystal D x2
		addRecipeToCatalog(new RecipeEntry(324, "mk_blessed_spiritshot_c", 3954, 4, 3949, 200, 120, 100, true,
				List.of(new Ingredient(3031, 30), new Ingredient(1459, 2)))); // Spirit Ore x30 + Crystal C x2
		addRecipeToCatalog(new RecipeEntry(325, "mk_blessed_spiritshot_b", 3955, 6, 3950, 100, 180, 100, true,
				List.of(new Ingredient(3031, 16), new Ingredient(1460, 2)))); // Spirit Ore x16 + Crystal B x2
		addRecipeToCatalog(new RecipeEntry(326, "mk_blessed_spiritshot_a", 3956, 7, 3951, 200, 240, 100, true,
				List.of(new Ingredient(3031, 70), new Ingredient(1461, 2)))); // Spirit Ore x70 + Crystal A x2
		addRecipeToCatalog(new RecipeEntry(327, "mk_blessed_spiritshot_s", 3957, 8, 3952, 200, 300, 100, true,
				List.of(new Ingredient(3031, 50), new Ingredient(1462, 2)))); // Spirit Ore x50 + Crystal S x2

		// Equipamentos principais S-Grade (IDs oficiais)
		addRecipeToCatalog(new RecipeEntry(632, "mk_angel_slayer_i", 6887, 9, 6367, 1, 225, 60, true,
				List.of(new Ingredient(6887, 1), new Ingredient(6691, 17), new Ingredient(5554, 4),
						new Ingredient(1890, 264), new Ingredient(1888, 264), new Ingredient(5550, 132),
						new Ingredient(4042, 132), new Ingredient(1462, 211), new Ingredient(2134, 43))));
		addRecipeToCatalog(new RecipeEntry(642, "mk_heavens_divider_i", 6897, 9, 6372, 1, 225, 60, true,
				List.of(new Ingredient(6897, 1), new Ingredient(6696, 17), new Ingredient(5554, 4),
						new Ingredient(1890, 148), new Ingredient(1888, 148), new Ingredient(5550, 296),
						new Ingredient(4042, 148), new Ingredient(1462, 212), new Ingredient(2134, 43))));
		addRecipeToCatalog(new RecipeEntry(644, "mk_arcana_mace_i", 6899, 9, 6579, 1, 225, 60, true,
				List.of(new Ingredient(6899, 1), new Ingredient(6697, 17), new Ingredient(5554, 4),
						new Ingredient(1890, 154), new Ingredient(1888, 154), new Ingredient(5550, 308),
						new Ingredient(4042, 77), new Ingredient(1462, 211), new Ingredient(2134, 43))));
		addRecipeToCatalog(new RecipeEntry(652, "mk_sealed_imperial_crusader_armor_i", 6853, 9, 6674, 1, 225, 60, true,
				List.of(new Ingredient(6853, 1), new Ingredient(6701, 17), new Ingredient(5553, 3),
						new Ingredient(1890, 117), new Ingredient(4043, 78), new Ingredient(1462, 62),
						new Ingredient(2134, 11))));
		addRecipeToCatalog(new RecipeEntry(664, "mk_sealed_draconic_leather_armor_i", 6865, 9, 6680, 1, 225, 60, true,
				List.of(new Ingredient(6865, 1), new Ingredient(6707, 17), new Ingredient(5553, 3),
						new Ingredient(1895, 168), new Ingredient(1889, 84), new Ingredient(4043, 84),
						new Ingredient(1462, 75), new Ingredient(2134, 14))));
		addRecipeToCatalog(new RecipeEntry(672, "mk_sealed_major_arcana_robe_i", 6873, 9, 6684, 1, 225, 60, true,
				List.of(new Ingredient(6873, 1), new Ingredient(6711, 17), new Ingredient(5553, 3),
						new Ingredient(1895, 168), new Ingredient(1889, 84), new Ingredient(4043, 84),
						new Ingredient(1462, 75), new Ingredient(2134, 14))));
		addRecipeToCatalog(new RecipeEntry(774, "mk_draconic_bow_i", 7580, 9, 7575, 1, 225, 60, true,
				List.of(new Ingredient(7580, 1), new Ingredient(7579, 17), new Ingredient(5554, 4),
						new Ingredient(1890, 375), new Ingredient(1888, 75), new Ingredient(5550, 150),
						new Ingredient(4042, 75), new Ingredient(1462, 212), new Ingredient(2134, 43))));
	}

	public Optional<RecipeEntry> getRecipe(int recipeId) {
		return Optional.ofNullable(recipesById.get(recipeId));
	}

	public Optional<RecipeEntry> getRecipeByItemId(int recipeItemId) {
		return Optional.ofNullable(recipesByItemId.get(recipeItemId));
	}

	public Collection<RecipeEntry> getAllRecipes() {
		return Collections.unmodifiableCollection(recipesById.values());
	}

	public boolean registerRecipe(int charId, int recipeId) {
		if (!com.lopez.l2j.config.Config.CRAFTING_ENABLED) {
			return false;
		}
		RecipeEntry recipe = recipesById.get(recipeId);
		if (recipe == null) {
			return false;
		}
		Map<Integer, Set<Integer>> targetMap = recipe.isDwarven() ? dwarvenBookByChar : commonBookByChar;
		Set<Integer> book = targetMap.computeIfAbsent(charId, k -> ConcurrentHashMap.newKeySet());
		boolean added = book.add(recipeId);
		if (added && jdbc != null) {
			try {
				int type = recipe.isDwarven() ? 0 : 1;
				jdbc.sql(
						"INSERT INTO character_recipebook (charId, id, type) VALUES (:cId, :rId, :t) ON DUPLICATE KEY UPDATE type = VALUES(type)")
						.param("cId", charId)
						.param("rId", recipeId)
						.param("t", type)
						.update();
			} catch (Exception ex) {
				log.debug("character_recipebook insert: {}", ex.getMessage());
			}
		}
		return added;
	}

	public boolean isCraftingEnabled() {
		return com.lopez.l2j.config.Config.CRAFTING_ENABLED;
	}

	public boolean isAltGameCreationEnabled() {
		return com.lopez.l2j.config.Config.ALT_GAME_CREATION;
	}

	public double getCreationSpeedLoad(int recipeLevel) {
		if (!com.lopez.l2j.config.Config.ALT_GAME_CREATION) {
			return 0.0;
		}
		double speed = Math.max(0.1, com.lopez.l2j.config.Config.ALT_GAME_CREATION_SPEED);
		return Math.max(1, recipeLevel) / speed;
	}

	public long calculateCreationExpReward(RecipeEntry recipe) {
		if (recipe == null || !com.lopez.l2j.config.Config.ALT_GAME_CREATION) {
			return 0L;
		}
		double baseExp = recipe.mpCost() * recipe.level() * 10.0;
		return Math.max(0L, Math.round(baseExp * Math.max(0.0, com.lopez.l2j.config.Config.ALT_GAME_CREATION_RATE_XP)));
	}

	public int calculateCreationSpReward(RecipeEntry recipe) {
		if (recipe == null || !com.lopez.l2j.config.Config.ALT_GAME_CREATION) {
			return 0;
		}
		double baseSp = recipe.mpCost() * recipe.level() * 1.5;
		return (int) Math.max(0L,
				Math.round(baseSp * Math.max(0.0, com.lopez.l2j.config.Config.ALT_GAME_CREATION_RATE_SP)));
	}

	public int calculateEffectiveIngredientCount(Ingredient ingredient) {
		if (ingredient == null) {
			return 0;
		}
		float mult = com.lopez.l2j.config.Config.RATE_CRAFT_COST;
		if (mult <= 0f || mult == 1.0f) {
			return ingredient.count();
		}
		return Math.max(1, Math.round(ingredient.count() * mult));
	}

	public int calculateConsumablePrice(int basePrice) {
		float mult = com.lopez.l2j.config.Config.RATE_CONSUMABLE_COST;
		if (mult <= 0f || mult == 1.0f) {
			return basePrice;
		}
		return Math.max(1, Math.round(basePrice * mult));
	}

	public boolean unregisterRecipe(int charId, int recipeId) {
		boolean removedDwarven = Optional.ofNullable(dwarvenBookByChar.get(charId))
				.map(s -> s.remove(recipeId))
				.orElse(false);
		boolean removedCommon = Optional.ofNullable(commonBookByChar.get(charId))
				.map(s -> s.remove(recipeId))
				.orElse(false);
		if ((removedDwarven || removedCommon) && jdbc != null) {
			try {
				jdbc.sql("DELETE FROM character_recipebook WHERE charId = :cId AND id = :rId")
						.param("cId", charId)
						.param("rId", recipeId)
						.update();
			} catch (Exception ex) {
				log.debug("character_recipebook delete: {}", ex.getMessage());
			}
		}
		return removedDwarven || removedCommon;
	}

	public boolean hasRecipe(int charId, int recipeId) {
		RecipeEntry recipe = recipesById.get(recipeId);
		if (recipe == null) {
			return false;
		}
		Map<Integer, Set<Integer>> targetMap = recipe.isDwarven() ? dwarvenBookByChar : commonBookByChar;
		Set<Integer> book = targetMap.get(charId);
		if (book == null && jdbc != null) {
			loadRecipeBookFromDb(charId);
			book = targetMap.get(charId);
		}
		return book != null && book.contains(recipeId);
	}

	public List<Integer> getRecipeBook(int charId, boolean isDwarven) {
		Map<Integer, Set<Integer>> targetMap = isDwarven ? dwarvenBookByChar : commonBookByChar;
		Set<Integer> book = targetMap.get(charId);
		if ((book == null || book.isEmpty()) && jdbc != null) {
			loadRecipeBookFromDb(charId);
			book = targetMap.get(charId);
		}
		if (book == null || book.isEmpty()) {
			return List.of();
		}
		List<Integer> sorted = new ArrayList<>(book);
		Collections.sort(sorted);
		return sorted;
	}

	public void loadRecipeBookFromDb(int charId) {
		if (jdbc == null) {
			return;
		}
		try {
			List<Map<String, Object>> rows = jdbc.sql("SELECT id, type FROM character_recipebook WHERE charId = :cId")
					.param("cId", charId)
					.query().listOfRows();
			for (var r : rows) {
				int rId = ((Number) r.get("id")).intValue();
				int type = ((Number) r.get("type")).intValue();
				Map<Integer, Set<Integer>> targetMap = (type == 0) ? dwarvenBookByChar : commonBookByChar;
				targetMap.computeIfAbsent(charId, k -> ConcurrentHashMap.newKeySet()).add(rId);
			}
		} catch (Exception ex) {
			log.debug("character_recipebook load: {}", ex.getMessage());
		}
	}

	public int calculateCraftSuccessRate(RecipeEntry recipe, com.lopez.l2j.game.model.PlayerCharacter player) {
		if (recipe == null)
			return 0;
		int rate = recipe.successRate();
		if (player != null && com.lopez.l2j.game.craft.DwarfCraftBonus.isDwarf(player)) {
			rate += com.lopez.l2j.game.craft.DwarfCraftBonus.getCraftBonusPercent();
		}
		return Math.min(100, Math.max(1, rate));
	}

	public record ManufactureItem(int recipeId, int cost) {
	}

	public record CraftResult(boolean attempted, boolean success, int productId, int productCount, String message) {
	}

	private final Map<Integer, List<ManufactureItem>> manufactureLists = new ConcurrentHashMap<>();

	public void setManufactureList(int crafterId, List<ManufactureItem> items) {
		if (items == null || items.isEmpty()) {
			manufactureLists.remove(crafterId);
		} else {
			manufactureLists.put(crafterId, List.copyOf(items));
		}
	}

	public List<ManufactureItem> getManufactureList(int crafterId) {
		return manufactureLists.getOrDefault(crafterId, List.of());
	}

	public Optional<Integer> getManufactureCost(int crafterId, int recipeId) {
		for (ManufactureItem item : getManufactureList(crafterId)) {
			if (item.recipeId() == recipeId) {
				return Optional.of(item.cost());
			}
		}
		return Optional.empty();
	}

	public CraftResult craftItem(com.lopez.l2j.game.model.PlayerCharacter crafter, int recipeId) {
		return craftItem(crafter, recipeId, null);
	}

	public CraftResult craftItem(com.lopez.l2j.game.model.PlayerCharacter crafter, int recipeId,
			com.lopez.l2j.game.service.InventoryService inventoryService) {
		if (!isCraftingEnabled() || crafter == null || crafter.isDead() || crafter.isOlympiadMode()) {
			return new CraftResult(false, false, 0, 0, "Cannot craft in current state");
		}
		RecipeEntry recipe = getRecipe(recipeId).orElseGet(() -> getRecipeByItemId(recipeId).orElse(null));
		if (recipe == null) {
			return new CraftResult(false, false, 0, 0, "Recipe not found");
		}
		var inv = crafter.inventory();
		if (inv == null) {
			return new CraftResult(false, false, 0, 0, "No inventory");
		}
		if (crafter.currentMp() < recipe.mpCost()) {
			return new CraftResult(false, false, 0, 0, "Not enough MP");
		}
		for (Ingredient ing : recipe.ingredients()) {
			int req = calculateEffectiveIngredientCount(ing);
			if (inv.getItemCount(ing.itemId()) < req) {
				return new CraftResult(false, false, 0, 0, "Missing ingredients");
			}
		}
		int maxSlots = crafter.maxInventorySlots() > 0 ? crafter.maxInventorySlots() : 80;
		if (inv.byItemId(recipe.productId()).isEmpty() && inv.size() >= maxSlots) {
			return new CraftResult(false, false, 0, 0, "Inventory slots full");
		}

		crafter.currentMp(crafter.currentMp() - recipe.mpCost());
		for (Ingredient ing : recipe.ingredients()) {
			int req = calculateEffectiveIngredientCount(ing);
			if (inventoryService != null) {
				inventoryService.consumeItem(inv, ing.itemId(), req, "Craft");
			} else {
				inv.destroyItemByItemId(ing.itemId(), req);
			}
		}

		int rate = calculateCraftSuccessRate(recipe, crafter);
		boolean success = java.util.concurrent.ThreadLocalRandom.current().nextInt(100) < rate;
		if (success) {
			if (inventoryService != null) {
				inventoryService.addItem(inv, recipe.productId(), recipe.productCount(), "Craft");
			} else {
				inv.addItem(recipe.productId(), recipe.productCount());
			}
			if (isAltGameCreationEnabled()) {
				long exp = calculateCreationExpReward(recipe);
				int sp = calculateCreationSpReward(recipe);
				if (exp > 0) {
					crafter.exp(crafter.exp() + exp);
				}
				if (sp > 0) {
					crafter.sp(crafter.sp() + sp);
				}
			}
		}
		return new CraftResult(true, success, recipe.productId(), success ? recipe.productCount() : 0,
				success ? "Success" : "Craft failed");
	}

	public CraftResult craftForCustomer(com.lopez.l2j.game.model.PlayerCharacter crafter,
			com.lopez.l2j.game.model.PlayerCharacter customer, int recipeId, int price) {
		return craftForCustomer(crafter, customer, recipeId, price, null);
	}

	public CraftResult craftForCustomer(com.lopez.l2j.game.model.PlayerCharacter crafter,
			com.lopez.l2j.game.model.PlayerCharacter customer, int recipeId, int price,
			com.lopez.l2j.game.service.InventoryService inventoryService) {
		if (!isCraftingEnabled() || crafter == null || customer == null || crafter.isDead() || customer.isDead()
				|| price < 0 || crafter.objectId() == customer.objectId()) {
			return new CraftResult(false, false, 0, 0, "Invalid manufacture request");
		}
		RecipeEntry recipe = getRecipe(recipeId).orElseGet(() -> getRecipeByItemId(recipeId).orElse(null));
		if (recipe == null) {
			return new CraftResult(false, false, 0, 0, "Recipe not found");
		}
		var customerInv = customer.inventory();
		var crafterInv = crafter.inventory();
		if (customerInv == null || crafterInv == null) {
			return new CraftResult(false, false, 0, 0, "No inventory");
		}
		if (crafter.currentMp() < recipe.mpCost()) {
			return new CraftResult(false, false, 0, 0, "Crafter does not have enough MP");
		}
		if (price > 0 && customerInv.adena() < price) {
			return new CraftResult(false, false, 0, 0, "Not enough Adena");
		}
		for (Ingredient ing : recipe.ingredients()) {
			int req = calculateEffectiveIngredientCount(ing);
			if (customerInv.getItemCount(ing.itemId()) < req) {
				return new CraftResult(false, false, 0, 0, "Missing ingredients");
			}
		}
		int maxSlots = customer.maxInventorySlots() > 0 ? customer.maxInventorySlots() : 80;
		if (customerInv.byItemId(recipe.productId()).isEmpty() && customerInv.size() >= maxSlots) {
			return new CraftResult(false, false, 0, 0, "Customer inventory full");
		}

		// Deduz MP do artesao
		crafter.currentMp(crafter.currentMp() - recipe.mpCost());

		// Transfere taxa em Adena (57) do cliente para o artesao
		if (price > 0) {
			if (inventoryService != null) {
				inventoryService.consumeItem(customerInv, 57, price, "ManufactureFee");
				inventoryService.addItem(crafterInv, 57, price, "ManufactureFee");
			} else {
				customerInv.destroyItemByItemId(57, price);
				crafterInv.addAdena(price);
			}
		}

		// Consome ingredientes do cliente
		for (Ingredient ing : recipe.ingredients()) {
			int req = calculateEffectiveIngredientCount(ing);
			if (inventoryService != null) {
				inventoryService.consumeItem(customerInv, ing.itemId(), req, "Manufacture");
			} else {
				customerInv.destroyItemByItemId(ing.itemId(), req);
			}
		}

		int rate = calculateCraftSuccessRate(recipe, crafter);
		boolean success = java.util.concurrent.ThreadLocalRandom.current().nextInt(100) < rate;
		if (success) {
			if (inventoryService != null) {
				inventoryService.addItem(customerInv, recipe.productId(), recipe.productCount(), "Manufacture");
			} else {
				customerInv.addItem(recipe.productId(), recipe.productCount());
			}
		}
		return new CraftResult(true, success, recipe.productId(), success ? recipe.productCount() : 0,
				success ? "Success" : "Manufacture failed");
	}

	public void saveAllToDb() {
		if (jdbc == null) {
			return;
		}
		initSchema();
		for (var entry : dwarvenBookByChar.entrySet()) {
			int charId = entry.getKey();
			for (int rId : entry.getValue()) {
				try {
					jdbc.sql(
							"INSERT INTO character_recipebook (charId, id, type) VALUES (:cId, :rId, 0) ON DUPLICATE KEY UPDATE type = 0")
							.param("cId", charId).param("rId", rId).update();
				} catch (Exception ignored) {
				}
			}
		}
		for (var entry : commonBookByChar.entrySet()) {
			int charId = entry.getKey();
			for (int rId : entry.getValue()) {
				try {
					jdbc.sql(
							"INSERT INTO character_recipebook (charId, id, type) VALUES (:cId, :rId, 1) ON DUPLICATE KEY UPDATE type = 1")
							.param("cId", charId).param("rId", rId).update();
				} catch (Exception ignored) {
				}
			}
		}
	}
}
