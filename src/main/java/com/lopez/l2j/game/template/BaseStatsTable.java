package com.lopez.l2j.game.template;

import java.io.InputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Tabela oficial de multiplicadores de atributos base (STR, CON, DEX, INT, WIT, MEN)
 * e multiplicador por nível (LevelMod), portada diretamente de data/xml/player/statBonus.xml
 * do Lineage II Interlude oficial (L2JDream).
 */
@Component
public class BaseStatsTable {

	private static final Logger log = LoggerFactory.getLogger(BaseStatsTable.class);
	public static final String RESOURCE = "data/player/statBonus.xml";
	public static final int MAX_STAT_VALUE = 100;

	private static final double[] STR_BONUS = new double[MAX_STAT_VALUE];
	private static final double[] INT_BONUS = new double[MAX_STAT_VALUE];
	private static final double[] DEX_BONUS = new double[MAX_STAT_VALUE];
	private static final double[] WIT_BONUS = new double[MAX_STAT_VALUE];
	private static final double[] CON_BONUS = new double[MAX_STAT_VALUE];
	private static final double[] MEN_BONUS = new double[MAX_STAT_VALUE];

	static {
		// Inicializa com curvas padrao caso o arquivo XML seja lido posteriormente
		for (int i = 0; i < MAX_STAT_VALUE; i++) {
			STR_BONUS[i] = Math.pow(1.036, i - 34.845);
			INT_BONUS[i] = Math.pow(1.0285, i - 27.85);
			DEX_BONUS[i] = Math.pow(1.031, i - 30.0);
			WIT_BONUS[i] = Math.pow(1.050, i - 20.0);
			CON_BONUS[i] = Math.pow(1.022, i - 42.0);
			MEN_BONUS[i] = Math.pow(1.025, i - 26.0);
		}
		try {
			load(new ClassPathResource(RESOURCE));
		} catch (Exception ignored) {
		}
	}

	public BaseStatsTable() {
	}

	public static void load(ClassPathResource resource) {
		try (InputStream in = resource.getInputStream()) {
			var factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			Document doc = factory.newDocumentBuilder().parse(in);
			Element root = doc.getDocumentElement();

			NodeList categories = root.getChildNodes();
			for (int i = 0; i < categories.getLength(); i++) {
				Node catNode = categories.item(i);
				if (catNode.getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}
				String catName = catNode.getNodeName().toUpperCase();
				double[] targetArray = switch (catName) {
					case "STR" -> STR_BONUS;
					case "INT" -> INT_BONUS;
					case "DEX" -> DEX_BONUS;
					case "WIT" -> WIT_BONUS;
					case "CON" -> CON_BONUS;
					case "MEN" -> MEN_BONUS;
					default -> null;
				};

				if (targetArray == null) {
					continue;
				}

				NodeList stats = catNode.getChildNodes();
				for (int j = 0; j < stats.getLength(); j++) {
					Node statNode = stats.item(j);
					if (statNode.getNodeType() == Node.ELEMENT_NODE && "stat".equalsIgnoreCase(statNode.getNodeName())) {
						Element el = (Element) statNode;
						int val = Integer.parseInt(el.getAttribute("value"));
						double bonus = Double.parseDouble(el.getAttribute("bonus"));
						if (val >= 0 && val < MAX_STAT_VALUE) {
							targetArray[val] = bonus;
						}
					}
				}
			}
			log.info("BaseStatsTable: carregadas tabelas de bônus de atributos com sucesso ({})", resource.getPath());
		} catch (Exception e) {
			log.warn("BaseStatsTable: falha ao carregar {} ({}), usando curvas matemáticas padrão", resource.getPath(), e.getMessage());
		}
	}

	public static double levelMod(int level) {
		return (Math.max(1, level) + 89.0) / 100.0;
	}

	public static double strBonus(int str) {
		return getBonus(STR_BONUS, str);
	}

	public static double intBonus(int intVal) {
		return getBonus(INT_BONUS, intVal);
	}

	public static double dexBonus(int dex) {
		return getBonus(DEX_BONUS, dex);
	}

	public static double witBonus(int wit) {
		return getBonus(WIT_BONUS, wit);
	}

	public static double conBonus(int con) {
		return getBonus(CON_BONUS, con);
	}

	public static double menBonus(int men) {
		return getBonus(MEN_BONUS, men);
	}

	private static double getBonus(double[] array, int val) {
		if (val <= 0) {
			return array[0] > 0 ? array[0] : 0.3;
		}
		if (val >= MAX_STAT_VALUE) {
			return array[MAX_STAT_VALUE - 1];
		}
		return array[val];
	}
}
