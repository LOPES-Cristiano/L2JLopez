package com.lopez.l2j.game.upgrade;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.item.ItemTemplateTable;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
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
 * Servico de evolucao e melhoria de equipamentos (NEXORA & Lucera) - Onda F7.
 * Preserva o nivel de encantamento original no item melhorado e valida requisitos sem doacao.
 */
@Service
public class UpgradeItemService {

	private static final Logger log = LoggerFactory.getLogger(UpgradeItemService.class);

	public record UpgradeRecipe(int itemId, int newItemId, int chance, int bossId, int reqItemId, long reqCount) {}

	public record UpgradeResult(boolean success, int resultItemId, int resultEnchant, String message) {
		public static UpgradeResult ok(int id, int enchant, String msg) {
			return new UpgradeResult(true, id, enchant, msg);
		}
		public static UpgradeResult fail(String msg) {
			return new UpgradeResult(false, 0, 0, msg);
		}
	}

	private final Map<Integer, UpgradeRecipe> recipes = new ConcurrentHashMap<>();
	private final AtomicInteger objIdGen = new AtomicInteger(890000);
	private final Random random = new Random();

	@Autowired(required = false)
	private ItemTemplateTable itemTemplateTable;

	@Autowired(required = false)
	private ObjectIdFactory objectIdFactory;

	public UpgradeItemService() {
		loadRecipes();
	}

	@Autowired
	public UpgradeItemService(ItemTemplateTable itemTemplateTable, ObjectIdFactory objectIdFactory) {
		this.itemTemplateTable = itemTemplateTable;
		this.objectIdFactory = objectIdFactory;
		loadRecipes();
	}

	public synchronized void loadRecipes() {
		recipes.clear();
		try {
			File file = new File("data/xml/custom/UpgradeItems.xml");
			InputStream is = file.exists() ? new java.io.FileInputStream(file) : getClass().getResourceAsStream("/data/xml/custom/UpgradeItems.xml");
			if (is == null) {
				log.warn("UpgradeItems.xml nao encontrado, inicializando fallback padrao.");
				registerRecipe(new UpgradeRecipe(6658, 7577, 90, 25325, 57, 5000000L));
				registerRecipe(new UpgradeRecipe(2375, 9882, 100, 0, 57, 1000000L));
				registerRecipe(new UpgradeRecipe(6580, 6660, 80, 29001, 3470, 10L));
				return;
			}

			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
			NodeList list = doc.getElementsByTagName("upgrade");

			for (int i = 0; i < list.getLength(); i++) {
				Element elem = (Element) list.item(i);
				int itemId = Integer.parseInt(elem.getAttribute("itemId"));
				int newItemId = Integer.parseInt(elem.getAttribute("newItemId"));
				int chance = Integer.parseInt(elem.getAttribute("chance"));
				int bossId = elem.hasAttribute("bossId") ? Integer.parseInt(elem.getAttribute("bossId")) : 0;
				int reqItemId = elem.hasAttribute("reqItemId") ? Integer.parseInt(elem.getAttribute("reqItemId")) : 57;
				long reqCount = elem.hasAttribute("reqCount") ? Long.parseLong(elem.getAttribute("reqCount")) : 0L;

				UpgradeRecipe recipe = new UpgradeRecipe(itemId, newItemId, chance, bossId, reqItemId, reqCount);
				registerRecipe(recipe);
			}

			log.info("UpgradeItemService: {} receitas de upgrade carregadas com sucesso.", recipes.size());
		} catch (Exception e) {
			log.error("Erro ao carregar UpgradeItems.xml: {}", e.getMessage(), e);
		}
	}

	public void registerRecipe(UpgradeRecipe recipe) {
		if (recipe != null) {
			recipes.put(recipe.itemId(), recipe);
		}
	}

	public Optional<UpgradeRecipe> findRecipe(int sourceItemId) {
		return Optional.ofNullable(recipes.get(sourceItemId));
	}

	public UpgradeResult upgradeItem(PlayerCharacter player, int sourceItemId) {
		return upgradeItem(player, sourceItemId, 0);
	}

	public synchronized UpgradeResult upgradeItem(PlayerCharacter player, int sourceItemId, int bossId) {
		if (player == null || player.inventory() == null) {
			return UpgradeResult.fail("Parametros invalidos.");
		}

		UpgradeRecipe recipe = recipes.get(sourceItemId);
		if (recipe == null) {
			return UpgradeResult.fail("Receita de upgrade nao encontrada para o item " + sourceItemId + ".");
		}

		if (recipe.bossId() > 0 && bossId > 0 && recipe.bossId() != bossId) {
			return UpgradeResult.fail("Requisito de Raid Boss nao atendido para este upgrade.");
		}

		Inventory inv = player.inventory();
		Optional<ItemInstance> sourceOpt = inv.byItemId(sourceItemId);
		if (sourceOpt.isEmpty()) {
			return UpgradeResult.fail("Voce nao possui o item base no inventario.");
		}

		ItemInstance sourceItem = sourceOpt.get();
		int originalEnchant = sourceItem.enchant();

		// Valida e consome taxa de materiais / Adena
		if (recipe.reqCount() > 0) {
			if (inv.count(recipe.reqItemId()) < recipe.reqCount()) {
				return UpgradeResult.fail("Materiais ou Adena insuficientes para o upgrade.");
			}
			inv.destroyItemByItemId(recipe.reqItemId(), (int) recipe.reqCount());
		}

		// Rola chance de sucesso
		int roll = random.nextInt(100) + 1;
		if (roll <= recipe.chance()) {
			// Remove item base
			inv.remove(sourceItem);

			// Cria novo item melhorado preservando o encantamento!
			int nextId = (objectIdFactory != null) ? objectIdFactory.nextId() : objIdGen.incrementAndGet();
			ItemTemplate tmpl = resolveTemplate(recipe.newItemId(), "UpgradedItem " + recipe.newItemId());
			ItemInstance newItem = new ItemInstance(nextId, tmpl, player.objectId(), 1);
			newItem.enchant(originalEnchant);
			inv.addItem(newItem);

			return UpgradeResult.ok(recipe.newItemId(), originalEnchant,
					"Upgrade concluido com sucesso! O novo item herdou encantamento +" + originalEnchant + ".");
		} else {
			return UpgradeResult.fail("O upgrade falhou. Seus materiais foram consumidos.");
		}
	}

	private ItemTemplate resolveTemplate(int itemId, String fallbackName) {
		if (itemTemplateTable != null) {
			var opt = itemTemplateTable.get(itemId);
			if (opt.isPresent()) {
				return opt.get();
			}
		}
		return ItemTemplate.etc(itemId, itemId, fallbackName, "other", "normal", 100, "none", 0, true, true, true, true);
	}
}
