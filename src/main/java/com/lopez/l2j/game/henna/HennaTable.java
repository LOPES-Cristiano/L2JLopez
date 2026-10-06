package com.lopez.l2j.game.henna;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Tabela de definicoes de Henna/Dyes (carregada de data/xml/player/henna.xml).
 */
@Component
public class HennaTable {

	private static final Logger log = LoggerFactory.getLogger(HennaTable.class);

	private final Map<Integer, Henna> bySymbolId = new ConcurrentHashMap<>();
	private final Map<Integer, Henna> byDyeId = new ConcurrentHashMap<>();

	public HennaTable() {
		loadFromXml("data/xml/player/henna.xml");
	}

	public HennaTable(String xmlPath) {
		loadFromXml(xmlPath);
	}

	public Henna get(int symbolId) {
		return bySymbolId.get(symbolId);
	}

	public Henna byDyeId(int dyeId) {
		return byDyeId.get(dyeId);
	}

	public Collection<Henna> all() {
		return Collections.unmodifiableCollection(bySymbolId.values());
	}

	public int size() {
		return bySymbolId.size();
	}

	private void loadFromXml(String filePath) {
		File file = new File(filePath);
		if (!file.exists()) {
			log.warn("Arquivo henna.xml nao encontrado: {}", file.getAbsolutePath());
			return;
		}

		try {
			var factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			Document doc = factory.newDocumentBuilder().parse(file);
			NodeList nodes = doc.getElementsByTagName("henna");

			for (int i = 0; i < nodes.getLength(); i++) {
				Node node = nodes.item(i);
				if (node.getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}
				Element elem = (Element) node;

				int symbolId = Integer.parseInt(elem.getAttribute("symbol_id"));
				String name = elem.getAttribute("symbol_name");
				int dyeId = Integer.parseInt(elem.getAttribute("dye_id"));
				int dyeAmount = Integer.parseInt(elem.getAttribute("dye_amount"));
				int price = Integer.parseInt(elem.getAttribute("price"));
				int statInt = Integer.parseInt(elem.getAttribute("stat_INT"));
				int statStr = Integer.parseInt(elem.getAttribute("stat_STR"));
				int statCon = Integer.parseInt(elem.getAttribute("stat_CON"));
				int statMen = Integer.parseInt(elem.getAttribute("stat_MEM"));
				int statDex = Integer.parseInt(elem.getAttribute("stat_DEX"));
				int statWit = Integer.parseInt(elem.getAttribute("stat_WIT"));

				Henna henna = new Henna(symbolId, name, dyeId, dyeAmount, price, statInt, statStr, statCon, statMen, statDex, statWit);
				bySymbolId.put(symbolId, henna);
				byDyeId.put(dyeId, henna);
			}

			log.info("HennaTable carregada com sucesso: {} hennas indexadas de {}", bySymbolId.size(), filePath);
		} catch (Exception ex) {
			log.error("Erro ao carregar HennaTable de {}", filePath, ex);
		}
	}
}
