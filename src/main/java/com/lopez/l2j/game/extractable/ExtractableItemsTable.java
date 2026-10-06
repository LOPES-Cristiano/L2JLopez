package com.lopez.l2j.game.extractable;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Tabela de itens extraiveis carregada de data/xml/player/extractable_items.xml.
 */
@Component
public class ExtractableItemsTable {

	private static final Logger log = LoggerFactory.getLogger(ExtractableItemsTable.class);

	private final Map<Integer, ExtractableItem> items = new HashMap<>();

	@Autowired
	public ExtractableItemsTable(@Value("${l2j.data.dir:data}") String dataDir) {
		load(Path.of(dataDir, "xml", "player", "extractable_items.xml"));
	}

	public ExtractableItemsTable(Path xmlPath) {
		load(xmlPath);
	}

	public ExtractableItemsTable() {
		// Construtor vazio para testes manuais
	}

	public void add(ExtractableItem item) {
		items.put(item.itemId(), item);
	}

	public void load() {
		load(Path.of("data", "xml", "player", "extractable_items.xml"));
	}

	public void load(Path path) {
		if (!Files.exists(path)) {
			log.warn("Arquivo extractable_items.xml nao encontrado em {}. Tabela vazia.", path.toAbsolutePath());
			return;
		}

		try (InputStream in = Files.newInputStream(path)) {
			var factory = DocumentBuilderFactory.newInstance();
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

			var doc = factory.newDocumentBuilder().parse(in);
			NodeList itemNodes = doc.getElementsByTagName("item");

			for (int i = 0; i < itemNodes.getLength(); i++) {
				if (itemNodes.item(i) instanceof Element itemEl) {
					// Alguns itens estao aninhados dentro de <product> como recompensa,
					// queremos apenas os <item> no nivel raiz da lista
					if (!"list".equalsIgnoreCase(itemEl.getParentNode().getNodeName())) {
						continue;
					}

					int itemId = Integer.parseInt(itemEl.getAttribute("id"));
					List<ExtractableProduct> products = new ArrayList<>();

					for (Node pNode = itemEl.getFirstChild(); pNode != null; pNode = pNode.getNextSibling()) {
						if (pNode instanceof Element pEl && "product".equalsIgnoreCase(pEl.getTagName())) {
							int skillId = pEl.hasAttribute("skillId") ? Integer.parseInt(pEl.getAttribute("skillId")) : 0;
							int skillLevel = pEl.hasAttribute("skillLevel") ? Integer.parseInt(pEl.getAttribute("skillLevel")) : 1;
							int chance = pEl.hasAttribute("chance") ? Integer.parseInt(pEl.getAttribute("chance")) : 100;

							List<ExtractableProduct.ProductItem> pItems = new ArrayList<>();
							for (Node cNode = pEl.getFirstChild(); cNode != null; cNode = cNode.getNextSibling()) {
								if (cNode instanceof Element cEl && "item".equalsIgnoreCase(cEl.getTagName())) {
									int prodItemId = Integer.parseInt(cEl.getAttribute("id"));
									int count = Integer.parseInt(cEl.getAttribute("count"));
									pItems.add(new ExtractableProduct.ProductItem(prodItemId, count));
								}
							}

							products.add(new ExtractableProduct(skillId, skillLevel, chance, pItems));
						}
					}

					items.put(itemId, new ExtractableItem(itemId, products));
				}
			}
			log.info("ExtractableItemsTable: Carregados {} itens extraiveis de {}", items.size(), path.toAbsolutePath());
		} catch (Exception e) {
			log.error("Erro ao carregar extractable_items.xml de {}: {}", path.toAbsolutePath(), e.getMessage(), e);
		}
	}

	public Optional<ExtractableItem> get(int itemId) {
		return Optional.ofNullable(items.get(itemId));
	}

	public boolean isExtractable(int itemId) {
		return items.containsKey(itemId);
	}

	public int size() {
		return items.size();
	}

	public Map<Integer, ExtractableItem> all() {
		return Collections.unmodifiableMap(items);
	}
}
