package com.lopez.l2j.game.summon;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
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
 * Tabela de itens invocadores carregada de data/xml/player/summon_items.xml.
 */
@Component
public class SummonItemsTable {

	private static final Logger log = LoggerFactory.getLogger(SummonItemsTable.class);

	private final Map<Integer, SummonItem> summonItems = new HashMap<>();

	@Autowired
	public SummonItemsTable(@Value("${l2j.data.dir:data}") String dataDir) {
		load(Path.of(dataDir, "xml", "player", "summon_items.xml"));
	}

	public SummonItemsTable(Path xmlPath) {
		load(xmlPath);
	}

	public SummonItemsTable() {
		// Construtor vazio para testes manuais
	}

	public void add(SummonItem item) {
		summonItems.put(item.itemId(), item);
	}

	private void load(Path path) {
		if (!Files.exists(path)) {
			log.warn("Arquivo summon_items.xml nao encontrado em {}. Tabela vazia.", path.toAbsolutePath());
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
				if (itemNodes.item(i) instanceof Element el) {
					int itemId = Integer.parseInt(el.getAttribute("id"));
					int npcId = 0;
					int summonType = 0;

					for (Node child = el.getFirstChild(); child != null; child = child.getNextSibling()) {
						if (child instanceof Element childEl) {
							if ("npcid".equalsIgnoreCase(childEl.getTagName())) {
								npcId = Integer.parseInt(childEl.getAttribute("val"));
							} else if ("summonType".equalsIgnoreCase(childEl.getTagName())) {
								summonType = Integer.parseInt(childEl.getAttribute("val"));
							}
						}
					}

					summonItems.put(itemId, new SummonItem(itemId, npcId, summonType));
				}
			}
			log.info("SummonItemsTable: Carregados {} itens invocadores de {}", summonItems.size(), path.toAbsolutePath());
		} catch (Exception e) {
			log.error("Erro ao carregar summon_items.xml de {}: {}", path.toAbsolutePath(), e.getMessage(), e);
		}
	}

	public Optional<SummonItem> get(int itemId) {
		return Optional.ofNullable(summonItems.get(itemId));
	}

	public boolean isSummonItem(int itemId) {
		return summonItems.containsKey(itemId);
	}

	public Map<Integer, SummonItem> all() {
		return Collections.unmodifiableMap(summonItems);
	}
}
