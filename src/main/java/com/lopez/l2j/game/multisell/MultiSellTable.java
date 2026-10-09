package com.lopez.l2j.game.multisell;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Tabela de listas de troca e criacao (MultiSell) carregadas de data/xml/multisell.
 */
@Component
public class MultiSellTable {

	private static final Logger log = LoggerFactory.getLogger(MultiSellTable.class);

	private final Map<Integer, MultiSellContainer> lists = new ConcurrentHashMap<>();
	private final Path multisellDir;

	public MultiSellTable(@Value("${l2.datapack.multisell-dir:data/xml/multisell}") String dirPath) {
		Path p = Path.of(dirPath);
		if (!Files.isDirectory(p)) {
			Path local = Path.of("data/xml/multisell");
			p = Files.isDirectory(local) ? local : p;
		}
		this.multisellDir = p;
		loadAll();
	}

	public void loadAll() {
		lists.clear();
		if (!Files.isDirectory(multisellDir)) {
			log.info("Diretorio de multisell {} nao encontrado", multisellDir);
			return;
		}

		try (var stream = Files.list(multisellDir)) {
			stream.filter(f -> f.toString().endsWith(".xml")).forEach(this::loadFile);
		} catch (Exception e) {
			log.warn("Erro ao listar diretorio de multisell {}: {}", multisellDir, e.getMessage());
		}

		log.info("Carregadas {} listas de multisell de {}", lists.size(), multisellDir.toAbsolutePath());
	}

	public Optional<MultiSellContainer> get(int listId) {
		MultiSellContainer c = lists.get(listId);
		if (c != null) {
			return Optional.of(c);
		}

		// Alias canônicos do Lineage 2 Interlude
		int mappedId = switch (listId) {
			case 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75 -> 20000 + (listId > 70 ? 70 : listId);
			case 519 -> 313750003;
			case 520 -> 313820003;
			case 522 -> 313750001;
			case 523 -> 313750002;
			case 524 -> 313820001;
			case 525 -> 313820002;
			case 521 -> 526;
			default -> listId;
		};

		if (mappedId != listId) {
			c = lists.get(mappedId);
			if (c != null) {
				lists.put(listId, c);
				return Optional.of(c);
			}
		}

		// Tenta carregar sob demanda se o arquivo existir (ex: 002.xml para listId=2 ou mappedId)
		int[] checkIds = mappedId != listId ? new int[]{listId, mappedId} : new int[]{listId};
		for (int idToSearch : checkIds) {
			String[] fileNames = {
					String.valueOf(idToSearch) + ".xml",
					String.format("%03d.xml", idToSearch),
					String.format("%04d.xml", idToSearch),
					String.format("%05d.xml", idToSearch)
			};
			for (String fn : fileNames) {
				Path p = multisellDir.resolve(fn);
				if (Files.isRegularFile(p)) {
					loadFile(p);
					c = lists.get(idToSearch);
					if (c != null) {
						if (listId != idToSearch) {
							lists.put(listId, c);
						}
						return Optional.of(c);
					}
				}
			}
		}
		return Optional.empty();
	}

	private void loadFile(Path file) {
		try (InputStream in = Files.newInputStream(file)) {
			String fn = file.getFileName().toString().replace(".xml", "");
			int listId;
			try {
				listId = Integer.parseInt(fn);
			} catch (NumberFormatException e) {
				return;
			}

			var dbf = DocumentBuilderFactory.newInstance();
			dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			Document doc = dbf.newDocumentBuilder().parse(in);
			Element root = doc.getDocumentElement();
			if (root == null || !"list".equalsIgnoreCase(root.getTagName())) {
				return;
			}

			boolean applyTaxes = "true".equalsIgnoreCase(root.getAttribute("applyTaxes"));
			boolean maintainEnchantment = "true".equalsIgnoreCase(root.getAttribute("maintainEnchantment"));

			List<MultiSellContainer.MultiSellEntry> entries = new ArrayList<>();
			NodeList items = root.getElementsByTagName("item");

			for (int i = 0; i < items.getLength(); i++) {
				Node itemNode = items.item(i);
				if (itemNode.getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}
				Element itemEl = (Element) itemNode;
				int entryId = entries.size() + 1;
				String idAttr = itemEl.getAttribute("id");
				if (!idAttr.isBlank()) {
					try {
						entryId = Integer.parseInt(idAttr.trim());
					} catch (NumberFormatException ignored) {
					}
				}

				List<MultiSellContainer.Ingredient> ingredients = new ArrayList<>();
				List<MultiSellContainer.Ingredient> products = new ArrayList<>();

				NodeList children = itemEl.getChildNodes();
				for (int c = 0; c < children.getLength(); c++) {
					Node child = children.item(c);
					if (child.getNodeType() != Node.ELEMENT_NODE) {
						continue;
					}
					Element childEl = (Element) child;
					String tag = childEl.getTagName();

					if ("ingredient".equalsIgnoreCase(tag) || "production".equalsIgnoreCase(tag)) {
						int itemId = Integer.parseInt(childEl.getAttribute("id").trim());
						long count = Long.parseLong(childEl.getAttribute("count").trim());
						int enchant = 0;
						if (childEl.hasAttribute("enchant")) {
							try {
								enchant = Integer.parseInt(childEl.getAttribute("enchant").trim());
							} catch (NumberFormatException ignored) {
							}
						}

						var ing = new MultiSellContainer.Ingredient(itemId, count, enchant);
						if ("ingredient".equalsIgnoreCase(tag)) {
							ingredients.add(ing);
						} else {
							products.add(ing);
						}
					}
				}

				if (!ingredients.isEmpty() && !products.isEmpty()) {
					entries.add(new MultiSellContainer.MultiSellEntry(entryId, ingredients, products));
				}
			}

			if (!entries.isEmpty()) {
				lists.put(listId, new MultiSellContainer(listId, applyTaxes, maintainEnchantment,
						Collections.unmodifiableList(entries)));
			}
		} catch (Exception e) {
			log.warn("Erro ao carregar multisell {}: {}", file.getFileName(), e.getMessage());
		}
	}
}
