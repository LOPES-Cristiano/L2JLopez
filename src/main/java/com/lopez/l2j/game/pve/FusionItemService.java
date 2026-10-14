package com.lopez.l2j.game.pve;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemRepository;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.item.ItemTemplateTable;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servico para fusao de armas, armaduras e acessorios - Onda C9.
 *
 * <p>Regra de Herança de Encantamento:</p>
 * <ul>
 *   <li>O novo item herda diretamente o valor de encantamento do <b>item base</b>.</li>
 *   <li>Se um dos ingredientes fornecidos estiver equipado, ele e considerado o item base.</li>
 *   <li>Senao, o ingrediente com <b>maior nivel de encantamento</b> e selecionado como item base.</li>
 * </ul>
 */
@Service
public class FusionItemService {

	private static final Logger log = LoggerFactory.getLogger(FusionItemService.class);

	public record FusionIngredient(int itemId, int count) {}

	public record FusionRecipe(
			String category,
			List<FusionIngredient> ingredients,
			int resultItemId,
			int resultCount,
			int chance,
			int failItemId,
			int failCount
	) {}

	public record FusionResult(
			boolean success,
			int resultItemId,
			int resultCount,
			int resultingEnchant,
			String message
	) {
		public static FusionResult ok(int id, int count, int enchant, String msg) {
			return new FusionResult(true, id, count, enchant, msg);
		}
		public static FusionResult fail(int id, int count, int enchant, String msg) {
			return new FusionResult(false, id, count, enchant, msg);
		}
	}

	private final List<FusionRecipe> recipes = new ArrayList<>();
	private final Map<String, List<FusionRecipe>> recipesByCategory = new ConcurrentHashMap<>();
	private final AtomicInteger objIdGen = new AtomicInteger(880000);
	private final Random random = new Random();

	@Autowired(required = false)
	private InventoryService inventoryService;

	@Autowired(required = false)
	private ItemTemplateTable itemTemplateTable;

	@Autowired(required = false)
	private ObjectIdFactory objectIdFactory;

	@Autowired(required = false)
	private ItemRepository itemRepository;

	public FusionItemService() {
		loadRecipes();
	}

	public FusionItemService(
			InventoryService inventoryService,
			ItemTemplateTable itemTemplateTable,
			ObjectIdFactory objectIdFactory) {
		this.inventoryService = inventoryService;
		this.itemTemplateTable = itemTemplateTable;
		this.objectIdFactory = objectIdFactory;
		loadRecipes();
	}

	@Autowired
	public FusionItemService(
			@Autowired(required = false) InventoryService inventoryService,
			@Autowired(required = false) ItemTemplateTable itemTemplateTable,
			@Autowired(required = false) ObjectIdFactory objectIdFactory,
			@Autowired(required = false) ItemRepository itemRepository) {
		this.inventoryService = inventoryService;
		this.itemTemplateTable = itemTemplateTable;
		this.objectIdFactory = objectIdFactory;
		this.itemRepository = itemRepository;
		loadRecipes();
	}

	public void setInventoryService(InventoryService inventoryService) {
		this.inventoryService = inventoryService;
	}

	public void setItemTemplateTable(ItemTemplateTable itemTemplateTable) {
		this.itemTemplateTable = itemTemplateTable;
	}

	public void setObjectIdFactory(ObjectIdFactory objectIdFactory) {
		this.objectIdFactory = objectIdFactory;
	}

	public void setItemRepository(ItemRepository itemRepository) {
		this.itemRepository = itemRepository;
	}

	public void loadRecipes() {
		recipes.clear();
		recipesByCategory.clear();
		try {
			File file = new File("data/xml/custom/FusionItems.xml");
			InputStream is = file.exists() ? new java.io.FileInputStream(file) : getClass().getResourceAsStream("/data/xml/custom/FusionItems.xml");
			if (is == null) {
				log.warn("FusionItems.xml nao encontrado.");
				return;
			}

			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
			NodeList list = doc.getElementsByTagName("fusion");

			for (int i = 0; i < list.getLength(); i++) {
				Element elem = (Element) list.item(i);
				String category = elem.getAttribute("category");

				List<FusionIngredient> ingredients = new ArrayList<>();
				// Considera os items dentro de <ingredients>
				Element ingParent = (Element) elem.getElementsByTagName("ingredients").item(0);
				if (ingParent != null) {
					NodeList subItems = ingParent.getElementsByTagName("item");
					for (int j = 0; j < subItems.getLength(); j++) {
						Element itemElem = (Element) subItems.item(j);
						ingredients.add(new FusionIngredient(
								Integer.parseInt(itemElem.getAttribute("id")),
								Integer.parseInt(itemElem.getAttribute("count"))
						));
					}
				}

				Element resElem = (Element) elem.getElementsByTagName("result").item(0);
				int resId = Integer.parseInt(resElem.getAttribute("id"));
				int resCount = Integer.parseInt(resElem.getAttribute("count"));

				int chance = 100;
				NodeList chNodes = elem.getElementsByTagName("chance");
				if (chNodes.getLength() > 0) {
					chance = Integer.parseInt(chNodes.item(0).getTextContent().trim());
				}

				int failId = resId;
				int failCount = 1;
				NodeList failNodes = elem.getElementsByTagName("failItem");
				if (failNodes.getLength() > 0) {
					Element fElem = (Element) failNodes.item(0);
					failId = Integer.parseInt(fElem.getAttribute("id"));
					failCount = Integer.parseInt(fElem.getAttribute("count"));
				}

				FusionRecipe recipe = new FusionRecipe(category, ingredients, resId, resCount, chance, failId, failCount);
				recipes.add(recipe);
				recipesByCategory.computeIfAbsent(category.toLowerCase(), k -> new ArrayList<>()).add(recipe);
			}

			log.info("FusionItemService: {} receitas de fusao carregadas com sucesso.", recipes.size());
		} catch (Exception e) {
			log.error("Erro ao carregar FusionItems.xml: {}", e.getMessage(), e);
		}
	}

	/**
	 * Executa a fusao automatica buscando os ingredientes da receita no inventario do jogador.
	 */
	public synchronized FusionResult fuseByRecipe(PlayerCharacter player, FusionRecipe recipe) {
		if (player == null || player.inventory() == null || recipe == null) {
			return FusionResult.fail(0, 0, 0, "Parametros de fusao invalidos.");
		}
		Inventory inv = player.inventory();
		List<ItemInstance> resolvedIngredients = new ArrayList<>();
		for (FusionIngredient ing : recipe.ingredients()) {
			var itemOpt = inv.byItemId(ing.itemId());
			if (itemOpt.isEmpty() || itemOpt.get().count() < ing.count()) {
				return FusionResult.fail(0, 0, 0, "Ingredientes insuficientes para a fusao (falta item " + ing.itemId() + ").");
			}
			resolvedIngredients.add(itemOpt.get());
		}
		return fuse(player, recipe, resolvedIngredients);
	}

	/**
	 * Executa a fusao de itens aplicando a regra estrita de heranca de encantamento do item base.
	 */
	public synchronized FusionResult fuse(PlayerCharacter player, FusionRecipe recipe, List<ItemInstance> providedIngredients) {
		if (player == null || recipe == null) {
			return FusionResult.fail(0, 0, 0, "Parametros de fusao invalidos.");
		}

		if (providedIngredients == null || providedIngredients.isEmpty()) {
			return FusionResult.fail(0, 0, 0, "Nenhum ingrediente fornecido.");
		}

		// 1. Identificacao do item base para heranca de encantamento:
		// Se algum ingrediente estiver equipado, e ele; senao o de MAIOR encantamento.
		ItemInstance baseItem = null;
		for (ItemInstance item : providedIngredients) {
			if (item != null && item.isEquipped()) {
				baseItem = item;
				break;
			}
		}

		if (baseItem == null) {
			int maxEnchant = -1;
			for (ItemInstance item : providedIngredients) {
				if (item != null && item.enchant() > maxEnchant) {
					maxEnchant = item.enchant();
					baseItem = item;
				}
			}
		}

		int inheritedEnchant = baseItem != null ? baseItem.enchant() : 0;

		// 2. Consumo dos ingredientes e taxa em Adena do inventario do jogador (persistindo no banco se inventoryService != null)
		Set<Integer> consumedItemIds = new HashSet<>();
		if (player.inventory() != null) {
			Inventory inv = player.inventory();
			for (ItemInstance item : providedIngredients) {
				if (item == null) {
					continue;
				}
				consumedItemIds.add(item.itemId());
				int requiredCount = findRequiredCount(recipe, item.itemId(), item.count());
				consumeIngredientInstance(inv, item, requiredCount);
			}

			// Se a receita exigir algum ingrediente adicional (ex.: taxa de Adena 57) nao listado em providedIngredients
			if (recipe.ingredients() != null) {
				for (FusionIngredient ing : recipe.ingredients()) {
					if (!consumedItemIds.contains(ing.itemId()) && inv.byItemId(ing.itemId()).isPresent()) {
						consumeByItemId(inv, ing.itemId(), ing.count());
					}
				}
			}
		}

		// 3. Avaliacao de sucesso com base na chance
		int roll = random.nextInt(100) + 1; // 1..100
		if (roll <= recipe.chance()) {
			// SUCESSO: Gera o novo item HERDANDO o encantamento base!
			ItemInstance resultItem = createAndPersistResultItem(
					player, recipe.resultItemId(), recipe.resultCount(), inheritedEnchant,
					"Fused Item " + recipe.resultItemId(), "FusionSuccess");

			log.info("Fusao BEM-SUCEDIDA para {}: gerado item {} com enchant herdado +{}.",
					player.name(), resultItem.itemId(), inheritedEnchant);
			return FusionResult.ok(recipe.resultItemId(), recipe.resultCount(), inheritedEnchant,
					"Fusao concluida com sucesso! O novo item herdou encantamento +" + inheritedEnchant + ".");
		} else {
			// FALHA: Devolve failItem sem encantamento
			createAndPersistResultItem(
					player, recipe.failItemId(), recipe.failCount(), 0,
					"Fail Item " + recipe.failItemId(), "FusionFail");

			log.info("Fusao FALHOU para {}: entregue failItem {}.", player.name(), recipe.failItemId());
			return FusionResult.fail(recipe.failItemId(), recipe.failCount(), 0,
					"A fusao falhou! Voce recebeu o item de compensacao.");
		}
	}

	private int findRequiredCount(FusionRecipe recipe, int itemId, int defaultCount) {
		if (recipe != null && recipe.ingredients() != null) {
			for (FusionIngredient ing : recipe.ingredients()) {
				if (ing.itemId() == itemId && ing.count() > 0) {
					return Math.min(defaultCount, ing.count());
				}
			}
		}
		return defaultCount;
	}

	private void consumeIngredientInstance(Inventory inv, ItemInstance item, int countToConsume) {
		if (inventoryService != null) {
			var destroyed = inventoryService.destroyItem(inv, item.objectId(), countToConsume, "FusionConsume");
			if (destroyed != null) {
				return;
			}
			var consumed = inventoryService.consumeItem(inv, item.itemId(), countToConsume, "FusionConsume");
			if (consumed != null) {
				return;
			}
		}
		inv.remove(item, countToConsume);
		if (itemRepository != null) {
			if (inv.byObjectId(item.objectId()).isEmpty()) {
				itemRepository.delete(item.objectId());
			} else {
				itemRepository.update(item);
			}
		}
	}

	private void consumeByItemId(Inventory inv, int itemId, int count) {
		if (count <= 0) {
			return;
		}
		if (inventoryService != null) {
			var consumed = inventoryService.consumeItem(inv, itemId, count, "FusionFee");
			if (consumed != null) {
				return;
			}
		}
		var opt = inv.byItemId(itemId);
		if (opt.isPresent()) {
			ItemInstance item = opt.get();
			inv.remove(item, count);
			if (itemRepository != null) {
				if (inv.byObjectId(item.objectId()).isEmpty()) {
					itemRepository.delete(item.objectId());
				} else {
					itemRepository.update(item);
				}
			}
		}
	}

	private ItemInstance createAndPersistResultItem(
			PlayerCharacter player, int itemId, int count, int enchant, String fallbackName, String process) {
		Inventory inv = player.inventory();
		if (inventoryService != null && inv != null) {
			var added = inventoryService.addItem(inv, itemId, count, process);
			if (added != null && added.item() != null) {
				ItemInstance created = added.item();
				if (enchant > 0) {
					created.enchant(enchant);
					inventoryService.saveItem(created);
				}
				return created;
			}
		}

		ItemTemplate tmpl = resolveTemplate(itemId, fallbackName);
		int nextObjId = (objectIdFactory != null) ? objectIdFactory.nextId() : objIdGen.incrementAndGet();
		ItemInstance item = new ItemInstance(nextObjId, tmpl, player.objectId(), count);
		if (enchant > 0) {
			item.enchant(enchant);
		}
		if (inv != null) {
			inv.add(item);
		}
		if (itemRepository != null) {
			itemRepository.insert(item, process);
		} else if (inventoryService != null) {
			inventoryService.saveItem(item);
		}
		return item;
	}

	private ItemTemplate resolveTemplate(int itemId, String fallbackName) {
		if (itemTemplateTable != null) {
			var opt = itemTemplateTable.get(itemId);
			if (opt.isPresent()) {
				return opt.get();
			}
		}
		if (inventoryService != null && inventoryService.templates() != null) {
			var opt = inventoryService.templates().get(itemId);
			if (opt.isPresent()) {
				return opt.get();
			}
		}
		return ItemTemplate.etc(itemId, itemId, fallbackName, "other", "normal", 100, "none", 0, true, true, true, true);
	}

	public List<FusionRecipe> getRecipes() {
		return Collections.unmodifiableList(recipes);
	}

	public List<FusionRecipe> getRecipesByCategory(String category) {
		return recipesByCategory.getOrDefault(category.toLowerCase(), List.of());
	}
}
