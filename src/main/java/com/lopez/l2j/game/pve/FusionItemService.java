package com.lopez.l2j.game.pve;

import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	public FusionItemService() {
		loadRecipes();
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
				NodeList ingNodes = elem.getElementsByTagName("item");
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
	 * Executa a fusao de itens aplicando a regra estrita de heranca de encantamento do item base.
	 */
	public FusionResult fuse(PlayerCharacter player, FusionRecipe recipe, List<ItemInstance> providedIngredients) {
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
			if (item.isEquipped()) {
				baseItem = item;
				break;
			}
		}

		if (baseItem == null) {
			int maxEnchant = -1;
			for (ItemInstance item : providedIngredients) {
				if (item.enchant() > maxEnchant) {
					maxEnchant = item.enchant();
					baseItem = item;
				}
			}
		}

		int inheritedEnchant = baseItem != null ? baseItem.enchant() : 0;

		// 2. Consumo dos ingredientes do inventario do jogador
		if (player.inventory() != null) {
			for (ItemInstance item : providedIngredients) {
				player.inventory().remove(item);
			}
		}

		// 3. Avaliacao de sucesso com base na chance
		int roll = random.nextInt(100) + 1; // 1..100
		if (roll <= recipe.chance()) {
			// SUCESSO: Gera o novo item HERDANDO o encantamento base!
			ItemTemplate tmpl = ItemTemplate.etc(recipe.resultItemId(), recipe.resultItemId(), "Fused Item " + recipe.resultItemId(), "other", "normal", 100, "none", 0, true, true, true, true);
			ItemInstance resultItem = new ItemInstance(objIdGen.incrementAndGet(), tmpl, player.objectId(), recipe.resultCount());
			resultItem.enchant(inheritedEnchant);

			if (player.inventory() != null) {
				player.inventory().add(resultItem);
			}

			log.info("Fusao BEM-SUCEDIDA para {}: gerado item {} com enchant herdado +{}.",
					player.name(), resultItem.itemId(), inheritedEnchant);
			return FusionResult.ok(recipe.resultItemId(), recipe.resultCount(), inheritedEnchant,
					"Fusao concluida com sucesso! O novo item herdou encantamento +" + inheritedEnchant + ".");
		} else {
			// FALHA: Devolve failItem sem encantamento
			ItemTemplate tmpl = ItemTemplate.etc(recipe.failItemId(), recipe.failItemId(), "Fail Item " + recipe.failItemId(), "other", "normal", 100, "none", 0, true, true, true, true);
			ItemInstance failItem = new ItemInstance(objIdGen.incrementAndGet(), tmpl, player.objectId(), recipe.failCount());

			if (player.inventory() != null) {
				player.inventory().add(failItem);
			}

			log.info("Fusao FALHOU para {}: entregue failItem {}.", player.name(), recipe.failItemId());
			return FusionResult.fail(recipe.failItemId(), recipe.failCount(), 0,
					"A fusao falhou! Voce recebeu o item de compensacao.");
		}
	}

	public List<FusionRecipe> getRecipes() {
		return Collections.unmodifiableList(recipes);
	}

	public List<FusionRecipe> getRecipesByCategory(String category) {
		return recipesByCategory.getOrDefault(category.toLowerCase(), List.of());
	}
}
