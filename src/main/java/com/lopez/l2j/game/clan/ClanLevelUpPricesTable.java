package com.lopez.l2j.game.clan;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Carrega a tabela de requisitos e precos para subida de nivel de cla (ClanLevelUpPrice.xml).
 */
@Component
public class ClanLevelUpPricesTable {

	private static final Logger log = LoggerFactory.getLogger(ClanLevelUpPricesTable.class);

	public record ClanLevelRequirement(
			int level,
			int sp,
			int exp,
			int reputation,
			int needMembers,
			Map<Integer, Integer> items
	) {}

	private final Map<Integer, ClanLevelRequirement> levels = new HashMap<>();

	public ClanLevelUpPricesTable() {
		loadFromXml("data/xml/world/ClanLevelUpPrice.xml");
	}

	public ClanLevelUpPricesTable(String xmlPath) {
		loadFromXml(xmlPath);
	}

	public ClanLevelRequirement getRequirement(int targetLevel) {
		return levels.get(targetLevel);
	}

	public Map<Integer, ClanLevelRequirement> allRequirements() {
		return Collections.unmodifiableMap(levels);
	}

	private void loadFromXml(String filePath) {
		File file = new File(filePath);
		if (!file.exists()) {
			log.warn("Arquivo de precos de cla nao encontrado: {}", file.getAbsolutePath());
			return;
		}

		try {
			var factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			Document doc = factory.newDocumentBuilder().parse(file);
			NodeList levelNodes = doc.getElementsByTagName("level");

			for (int i = 0; i < levelNodes.getLength(); i++) {
				Node node = levelNodes.item(i);
				if (node.getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}
				Element elem = (Element) node;
				int lvl = Integer.parseInt(elem.getAttribute("id"));
				int sp = 0;
				int exp = 0;
				int rep = 0;
				int members = 0;
				Map<Integer, Integer> reqItems = new HashMap<>();

				NodeList children = elem.getChildNodes();
				for (int j = 0; j < children.getLength(); j++) {
					Node child = children.item(j);
					if (child.getNodeType() != Node.ELEMENT_NODE) {
						continue;
					}
					Element childElem = (Element) child;
					String tag = childElem.getTagName().toLowerCase();
					switch (tag) {
						case "item" -> {
							int itemId = Integer.parseInt(childElem.getAttribute("id"));
							int count = Integer.parseInt(childElem.getAttribute("count"));
							reqItems.put(itemId, reqItems.getOrDefault(itemId, 0) + count);
						}
						case "sp" -> sp += Integer.parseInt(childElem.getAttribute("count"));
						case "exp" -> exp += Integer.parseInt(childElem.getAttribute("count"));
						case "reputation" -> rep += Integer.parseInt(childElem.getAttribute("count"));
						case "needmembers" -> members += Integer.parseInt(childElem.getAttribute("count"));
					}
				}

				levels.put(lvl, new ClanLevelRequirement(lvl, sp, exp, rep, members, Collections.unmodifiableMap(reqItems)));
			}

			log.info("ClanLevelUpPricesTable carregada: {} niveis configurados de {}", levels.size(), filePath);
		} catch (Exception ex) {
			log.error("Erro ao carregar ClanLevelUpPrice de {}", filePath, ex);
		}
	}
}
