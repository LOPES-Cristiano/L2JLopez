package com.lopez.l2j.game.trade;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Tabela de buylists carregada de buylists.xml.
 */
@Component
public class BuyListTable {

	private static final Logger log = LoggerFactory.getLogger(BuyListTable.class);

	private final Map<Integer, NpcBuyList> buyLists = new HashMap<>();

	public BuyListTable(
			@Value("${l2.datapack.buylists-file:data/xml/world/buylists.xml}") String filePath) {
		load(filePath);
	}

	public void load(String filePath) {
		buyLists.clear();
		if (filePath == null || filePath.isBlank()) {
			return;
		}
		Path p = Path.of(filePath);
		if (Files.exists(p)) {
			try (InputStream in = Files.newInputStream(p)) {
				parseXml(in);
				log.info("Carregadas {} listas de compra de {}", buyLists.size(), p.toAbsolutePath());
				return;
			} catch (Exception e) {
				log.warn("Falha ao carregar buylists de {}: {}", p, e.getMessage());
			}
		}

		// Fallback para classpath apenas se procurava no padrão
		if (filePath.contains("buylists.xml")) {
			try (InputStream in = getClass().getResourceAsStream("/data/xml/world/buylists.xml")) {
				if (in != null) {
					parseXml(in);
					log.info("Carregadas {} listas de compra do classpath", buyLists.size());
					return;
				}
			} catch (Exception e) {
				log.warn("Falha ao carregar buylists do classpath: {}", e.getMessage());
			}
		}

		log.warn("Nenhum arquivo buylists.xml encontrado (procurado em: {})", filePath);
	}

	private void parseXml(InputStream in) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		Document doc = factory.newDocumentBuilder().parse(in);
		Node root = doc.getFirstChild();
		for (Node n = root; n != null; n = n.getNextSibling()) {
			if ("list".equalsIgnoreCase(n.getNodeName())) {
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
					if ("buylist".equalsIgnoreCase(d.getNodeName())) {
						NamedNodeMap attrs = d.getAttributes();
						int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
						int npcId = Integer.parseInt(attrs.getNamedItem("npcId").getNodeValue());
						List<NpcBuyList.Product> prods = new ArrayList<>();

						for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling()) {
							if ("product".equalsIgnoreCase(c.getNodeName())) {
								NamedNodeMap pAttrs = c.getAttributes();
								int prodId = Integer.parseInt(pAttrs.getNamedItem("id").getNodeValue());
								int price = 0;
								Node priceAttr = pAttrs.getNamedItem("price");
								if (priceAttr != null) {
									price = Integer.parseInt(priceAttr.getNodeValue());
								}
								int count = -1;
								Node countAttr = pAttrs.getNamedItem("count");
								if (countAttr != null) {
									count = Integer.parseInt(countAttr.getNodeValue());
								}
								prods.add(new NpcBuyList.Product(prodId, price, count));
							}
						}
						buyLists.put(id, new NpcBuyList(id, npcId, List.copyOf(prods)));
					}
				}
			}
		}
	}

	public Optional<NpcBuyList> get(int listId) {
		return Optional.ofNullable(buyLists.get(listId));
	}

	public int size() {
		return buyLists.size();
	}

	public List<NpcBuyList> byNpcId(int npcId) {
		return buyLists.values().stream().filter(b -> b.npcId() == npcId).toList();
	}

	public Map<Integer, NpcBuyList> all() {
		return Collections.unmodifiableMap(buyLists);
	}
}
