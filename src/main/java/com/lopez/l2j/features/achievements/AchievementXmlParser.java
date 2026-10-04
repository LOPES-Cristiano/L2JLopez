package com.lopez.l2j.features.achievements;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/** Le o achievements.xml (formato do L2JDream) e devolve as conquistas validadas. */
final class AchievementXmlParser {

	private static final java.util.Set<String> META_ATTRIBUTES =
			java.util.Set.of("id", "name", "description", "reward", "repeatable");

	private AchievementXmlParser() {
	}

	static Map<Integer, Achievement> parse(InputStream xml) {
		Document doc;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			// XML vem de arquivo de configuracao, mas desligamos DTD/entidades externas (XXE) por padrao.
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setIgnoringComments(true);
			doc = factory.newDocumentBuilder().parse(xml);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			throw new IllegalStateException("Falha ao ler achievements.xml", e);
		}

		Map<Integer, Achievement> result = new LinkedHashMap<>();
		Node root = doc.getDocumentElement();
		if (!"list".equalsIgnoreCase(root.getNodeName())) {
			throw new IllegalStateException("achievements.xml: raiz deve ser <list>, achei <" + root.getNodeName() + ">");
		}
		for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node.getNodeType() == Node.ELEMENT_NODE && "achievement".equalsIgnoreCase(node.getNodeName())) {
				Achievement achievement = parseAchievement(node.getAttributes());
				if (result.putIfAbsent(achievement.id(), achievement) != null) {
					throw new IllegalStateException("achievements.xml: id duplicado " + achievement.id());
				}
			}
		}
		return result;
	}

	private static Achievement parseAchievement(NamedNodeMap attrs) {
		int id = Integer.parseInt(required(attrs, "id"));
		String name = required(attrs, "name");
		String description = attrs.getNamedItem("description") == null ? "" : attrs.getNamedItem("description").getNodeValue();
		Map<Integer, Long> rewards = parseRewards(id, required(attrs, "reward"));
		boolean repeatable = attrs.getNamedItem("repeatable") != null
				&& Boolean.parseBoolean(attrs.getNamedItem("repeatable").getNodeValue());

		List<AchievementCondition> conditions = new ArrayList<>();
		for (int i = 0; i < attrs.getLength(); i++) {
			String attr = attrs.item(i).getNodeName();
			if (META_ATTRIBUTES.contains(attr)) {
				continue;
			}
			AchievementCondition condition;
			try {
				condition = Conditions.fromAttribute(attr, attrs.item(i).getNodeValue()).orElse(null);
			} catch (IllegalArgumentException e) {
				throw new IllegalStateException("achievements.xml: conquista " + id + ", atributo '" + attr + "': " + e.getMessage(), e);
			}
			if (condition != null) {
				conditions.add(condition);
			} else if (!isBooleanFlag(attrs.item(i).getNodeValue())) {
				throw new IllegalStateException("achievements.xml: conquista " + id + ", atributo desconhecido '" + attr + "'");
			}
		}
		return new Achievement(id, name, description, rewards, repeatable, conditions);
	}

	private static boolean isBooleanFlag(String value) {
		return "false".equalsIgnoreCase(value.trim());
	}

	private static String required(NamedNodeMap attrs, String name) {
		Node n = attrs.getNamedItem(name);
		if (n == null || n.getNodeValue().isBlank()) {
			throw new IllegalStateException("achievements.xml: atributo obrigatorio ausente: " + name);
		}
		return n.getNodeValue();
	}

	/** Formato "itemId,qtd;itemId,qtd". */
	private static Map<Integer, Long> parseRewards(int achievementId, String raw) {
		Map<Integer, Long> rewards = new LinkedHashMap<>();
		for (String entry : raw.split(";")) {
			if (entry.isBlank()) {
				continue;
			}
			String[] parts = entry.split(",");
			try {
				if (parts.length != 2) {
					throw new NumberFormatException("esperado 'item,qtd'");
				}
				rewards.merge(Integer.parseInt(parts[0].trim()), Long.parseLong(parts[1].trim()), Long::sum);
			} catch (NumberFormatException e) {
				throw new IllegalStateException("achievements.xml: conquista " + achievementId + ", reward invalido '" + entry + "'", e);
			}
		}
		return rewards;
	}
}
