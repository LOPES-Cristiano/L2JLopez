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
 * Servico de gerenciamento de receitas de Crafting (Dwarven e Common) e livros de receitas (RecipeBook).
 * Carrega receitas de XML (data/xml/recipes.xml ou data/recipes.xml) com catalogo fallback embutido.
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
	public RecipeService(@org.springframework.beans.factory.annotation.Autowired(required = false) org.springframework.jdbc.core.simple.JdbcClient jdbc) {
		this.jdbc = jdbc;
		instance = this;
		loadRecipes();
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
				Path.of("data", "xml", "recipes.xml"),
				Path.of("data", "recipes.xml")
		};
		for (Path path : candidatePaths) {
			if (Files.isRegularFile(path)) {
				int loaded = parseXmlFile(path.toFile());
				if (loaded > 0) {
					log.info("RecipeService: {} receitas carregadas de {}", loaded, path);
				}
			}
		}

		String[] classpathCandidates = new String[] { "/data/xml/recipes.xml", "/data/recipes.xml" };
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
		// Materiais basicos classicos (Levels 1-6)
		addRecipeToCatalog(new RecipeEntry(1, "mk_braided_hemp", 1786, 1, 1878, 1, 30, 100, true,
				List.of(new Ingredient(1864, 5)))); // Stem x5 -> Braided Hemp
		addRecipeToCatalog(new RecipeEntry(2, "mk_cokes", 1787, 1, 1879, 1, 30, 100, true,
				List.of(new Ingredient(1870, 3), new Ingredient(1871, 3)))); // Coal x3 + Charcoal x3 -> Cokes
		addRecipeToCatalog(new RecipeEntry(3, "mk_steel", 1788, 1, 1880, 1, 30, 100, true,
				List.of(new Ingredient(1865, 5), new Ingredient(1869, 5)))); // Varnish x5 + Iron Ore x5 -> Steel
		addRecipeToCatalog(new RecipeEntry(4, "mk_coarse_bone_powder", 1789, 1, 1881, 1, 30, 100, true,
				List.of(new Ingredient(1872, 10)))); // Animal Bone x10 -> Coarse Bone Powder
		addRecipeToCatalog(new RecipeEntry(5, "mk_leather", 1790, 1, 1882, 1, 30, 100, true,
				List.of(new Ingredient(1867, 6)))); // Animal Skin x6 -> Leather
		addRecipeToCatalog(new RecipeEntry(6, "mk_steel_mold", 1791, 2, 1883, 1, 45, 100, true,
				List.of(new Ingredient(1878, 5), new Ingredient(1869, 5), new Ingredient(1870, 5)))); // Steel Mold
		addRecipeToCatalog(new RecipeEntry(7, "mk_cord", 1792, 2, 1884, 20, 45, 100, true,
				List.of(new Ingredient(1880, 2), new Ingredient(1868, 25)))); // Steel x2 + Thread x25 -> Cord x20
		addRecipeToCatalog(new RecipeEntry(8, "mk_high_grade_suede", 1793, 2, 1885, 1, 45, 100, true,
				List.of(new Ingredient(1881, 1), new Ingredient(1866, 3)))); // High Grade Suede
		addRecipeToCatalog(new RecipeEntry(9, "mk_silver_mold", 1794, 2, 1886, 1, 45, 100, true,
				List.of(new Ingredient(1878, 5), new Ingredient(1879, 5), new Ingredient(1873, 10)))); // Silver Mold
		addRecipeToCatalog(new RecipeEntry(10, "mk_varnish_of_purity", 1795, 2, 1887, 1, 45, 100, true,
				List.of(new Ingredient(1881, 3), new Ingredient(1865, 3), new Ingredient(1875, 1)))); // Varnish of Purity
		addRecipeToCatalog(new RecipeEntry(11, "mk_synthetic_cokes", 1796, 3, 1888, 1, 60, 100, true,
				List.of(new Ingredient(1879, 3), new Ingredient(1874, 1)))); // Synthetic Cokes
		addRecipeToCatalog(new RecipeEntry(12, "mk_compound_braid", 1797, 3, 1889, 1, 60, 100, true,
				List.of(new Ingredient(1878, 5), new Ingredient(1868, 5)))); // Compound Braid
		addRecipeToCatalog(new RecipeEntry(13, "mk_mithril_alloy", 1798, 4, 1890, 1, 75, 100, true,
				List.of(new Ingredient(1887, 1), new Ingredient(1880, 2), new Ingredient(1876, 1)))); // Mithril Alloy
		addRecipeToCatalog(new RecipeEntry(14, "mk_artisans_frame", 1799, 4, 1891, 1, 75, 100, true,
				List.of(new Ingredient(1883, 1), new Ingredient(1887, 5), new Ingredient(1877, 10)))); // Artisan's Frame
		addRecipeToCatalog(new RecipeEntry(15, "mk_blacksmith_frame", 1800, 4, 1892, 1, 75, 100, true,
				List.of(new Ingredient(1886, 1), new Ingredient(1887, 5), new Ingredient(1876, 10)))); // Blacksmith's Frame
		addRecipeToCatalog(new RecipeEntry(16, "mk_oriharukon", 1801, 4, 1893, 1, 75, 100, true,
				List.of(new Ingredient(1888, 1), new Ingredient(1873, 12), new Ingredient(1874, 4)))); // Oriharukon
		addRecipeToCatalog(new RecipeEntry(17, "mk_crafted_leather", 1802, 3, 1894, 1, 60, 100, true,
				List.of(new Ingredient(1884, 4), new Ingredient(1882, 4), new Ingredient(1870, 4)))); // Crafted Leather
		addRecipeToCatalog(new RecipeEntry(49, "mk_metallic_fiber", 2135, 2, 1895, 20, 45, 100, true,
				List.of(new Ingredient(1884, 15), new Ingredient(1873, 15)))); // Metallic Fiber x20
		addRecipeToCatalog(new RecipeEntry(542, "mk_metallic_thread", 5472, 2, 5549, 1, 45, 100, true,
				List.of(new Ingredient(1868, 10), new Ingredient(1869, 5)))); // Metallic Thread
		addRecipeToCatalog(new RecipeEntry(543, "mk_durable_metal_plate", 5473, 4, 5550, 1, 75, 100, true,
				List.of(new Ingredient(5549, 5), new Ingredient(1876, 5)))); // Durable Metal Plate

		// Soulshots (D / C / B / A / S)
		addRecipeToCatalog(new RecipeEntry(18, "mk_soulshot_d", 1804, 2, 1463, 156, 30, 100, true,
				List.of(new Ingredient(1785, 3), new Ingredient(1458, 1)))); // Soul Ore x3 + Crystal D x1
		addRecipeToCatalog(new RecipeEntry(19, "mk_soulshot_c", 1805, 4, 1464, 476, 60, 100, true,
				List.of(new Ingredient(1785, 15), new Ingredient(1459, 1)))); // Soul Ore x15 + Crystal C x1
		addRecipeToCatalog(new RecipeEntry(20, "mk_soulshot_b", 1806, 6, 1465, 450, 100, 100, true,
				List.of(new Ingredient(1785, 54), new Ingredient(1460, 1)))); // Soul Ore x54 + Crystal B x1
		addRecipeToCatalog(new RecipeEntry(21, "mk_soulshot_a", 1807, 7, 1466, 300, 120, 100, true,
				List.of(new Ingredient(1785, 36), new Ingredient(1461, 1)))); // Soul Ore x36 + Crystal A x1
		addRecipeToCatalog(new RecipeEntry(22, "mk_soulshot_s", 1808, 8, 1467, 350, 150, 100, true,
				List.of(new Ingredient(1785, 40), new Ingredient(1462, 1)))); // Soul Ore x40 + Crystal S x1

		// Spiritshots (D / C / B / A / S)
		addRecipeToCatalog(new RecipeEntry(308, "mk_spiritshot_d", 3032, 2, 2510, 100, 30, 100, true,
				List.of(new Ingredient(3031, 3), new Ingredient(1458, 1)))); // Spirit Ore x3 + Crystal D x1
		addRecipeToCatalog(new RecipeEntry(309, "mk_spiritshot_c", 3033, 4, 2511, 200, 60, 100, true,
				List.of(new Ingredient(3031, 10), new Ingredient(1459, 1)))); // Spirit Ore x10 + Crystal C x1
		addRecipeToCatalog(new RecipeEntry(310, "mk_spiritshot_b", 3034, 6, 2512, 150, 100, 100, true,
				List.of(new Ingredient(3031, 15), new Ingredient(1460, 1)))); // Spirit Ore x15 + Crystal B x1
		addRecipeToCatalog(new RecipeEntry(311, "mk_spiritshot_a", 3035, 7, 2513, 200, 120, 100, true,
				List.of(new Ingredient(3031, 22), new Ingredient(1461, 1)))); // Spirit Ore x22 + Crystal A x1
		addRecipeToCatalog(new RecipeEntry(312, "mk_spiritshot_s", 3036, 8, 2514, 200, 150, 100, true,
				List.of(new Ingredient(3031, 15), new Ingredient(1462, 1)))); // Spirit Ore x15 + Crystal S x1

		// Blessed Spiritshots (D / C / B / A / S)
		addRecipeToCatalog(new RecipeEntry(313, "mk_blessed_spiritshot_d", 3953, 2, 3948, 100, 30, 100, true,
				List.of(new Ingredient(3031, 8), new Ingredient(1458, 2)))); // Spirit Ore x8 + Crystal D x2
		addRecipeToCatalog(new RecipeEntry(314, "mk_blessed_spiritshot_c", 3954, 4, 3949, 200, 60, 100, true,
				List.of(new Ingredient(3031, 30), new Ingredient(1459, 2)))); // Spirit Ore x30 + Crystal C x2
		addRecipeToCatalog(new RecipeEntry(315, "mk_blessed_spiritshot_b", 3955, 6, 3950, 100, 100, 100, true,
				List.of(new Ingredient(3031, 16), new Ingredient(1460, 2)))); // Spirit Ore x16 + Crystal B x2
		addRecipeToCatalog(new RecipeEntry(316, "mk_blessed_spiritshot_a", 3956, 7, 3951, 200, 120, 100, true,
				List.of(new Ingredient(3031, 70), new Ingredient(1461, 2)))); // Spirit Ore x70 + Crystal A x2
		addRecipeToCatalog(new RecipeEntry(317, "mk_blessed_spiritshot_s", 3957, 8, 3952, 200, 150, 100, true,
				List.of(new Ingredient(3031, 50), new Ingredient(1462, 2)))); // Spirit Ore x50 + Crystal S x2

		// Equipamentos principais S-Grade / A-Grade
		addRecipeToCatalog(new RecipeEntry(681, "mk_draconic_bow", 6857, 8, 7575, 1, 225, 60, true,
				List.of(new Ingredient(6688, 17), new Ingredient(5550, 140), new Ingredient(1890, 140),
						new Ingredient(1888, 70), new Ingredient(1462, 211), new Ingredient(2134, 43))));
		addRecipeToCatalog(new RecipeEntry(683, "mk_arcana_mace", 6859, 8, 6608, 1, 225, 60, true,
				List.of(new Ingredient(6690, 17), new Ingredient(5550, 140), new Ingredient(1893, 70),
						new Ingredient(1888, 140), new Ingredient(1462, 211), new Ingredient(2134, 43))));
		addRecipeToCatalog(new RecipeEntry(685, "mk_heavens_divider", 6861, 8, 6372, 1, 225, 60, true,
				List.of(new Ingredient(6692, 17), new Ingredient(1890, 140), new Ingredient(1888, 140),
						new Ingredient(5550, 70), new Ingredient(1462, 211), new Ingredient(2134, 43))));
		addRecipeToCatalog(new RecipeEntry(687, "mk_angel_slayer", 6863, 8, 6367, 1, 225, 60, true,
				List.of(new Ingredient(6694, 17), new Ingredient(1890, 140), new Ingredient(5550, 140),
						new Ingredient(1888, 70), new Ingredient(1462, 211), new Ingredient(2134, 43))));
		addRecipeToCatalog(new RecipeEntry(695, "mk_imperial_crusader_breastplate", 6871, 8, 6373, 1, 225, 60, true,
				List.of(new Ingredient(6701, 17), new Ingredient(5550, 48), new Ingredient(1890, 24),
						new Ingredient(1888, 24), new Ingredient(1462, 160), new Ingredient(2134, 34))));
		addRecipeToCatalog(new RecipeEntry(701, "mk_draconic_leather_armor", 6877, 8, 6379, 1, 225, 60, true,
				List.of(new Ingredient(6707, 17), new Ingredient(1894, 60), new Ingredient(1889, 60),
						new Ingredient(1895, 60), new Ingredient(1462, 120), new Ingredient(2134, 25))));
		addRecipeToCatalog(new RecipeEntry(705, "mk_major_arcana_robe", 6881, 8, 6383, 1, 225, 60, true,
				List.of(new Ingredient(6711, 17), new Ingredient(1894, 60), new Ingredient(1885, 30),
						new Ingredient(1895, 120), new Ingredient(1462, 120), new Ingredient(2134, 25))));
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
				jdbc.sql("INSERT INTO character_recipebook (charId, id, type) VALUES (:cId, :rId, :t) ON DUPLICATE KEY UPDATE type = VALUES(type)")
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
		return (int) Math.max(0L, Math.round(baseSp * Math.max(0.0, com.lopez.l2j.config.Config.ALT_GAME_CREATION_RATE_SP)));
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
		if (recipe == null) return 0;
		int rate = recipe.successRate();
		if (player != null && com.lopez.l2j.game.craft.DwarfCraftBonus.isDwarf(player)) {
			rate += com.lopez.l2j.game.craft.DwarfCraftBonus.getCraftBonusPercent();
		}
		return Math.min(100, Math.max(1, rate));
	}
}
