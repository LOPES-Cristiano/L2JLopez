package com.lopez.l2j.game.olympiad;

import com.lopez.l2j.game.model.PlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import jakarta.annotation.PostConstruct;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Balanceador Exclusivo de Olimpiadas (Onda C5 - Repatriacao L2JRadical).
 *
 * <p>Regra de Negocio:</p>
 * <ul>
 *   <li>Carrega multiplicadores declarativos de {@code olympiad_balance.xml}.</li>
 *   <li>Isolamento Total: Caso qualquer um dos envolvidos nao esteja em modo Olimpiada
 *       ({@code !player.isOlympiadMode()}), o multiplicador retornado e estritamente 1.0 (retail C6).</li>
 *   <li>Quando ambos estao na arena: multiplica dano por classe atacante x classe defensora.</li>
 * </ul>
 */
@Service
public class OlyClassDamageManager {

	private static final Logger log = LoggerFactory.getLogger(OlyClassDamageManager.class);

	public static final String XML_PATH_EXTERNAL = "data/xml/olympiad_balance.xml";
	public static final String XML_PATH_CLASSPATH = "data/xml/olympiad_balance.xml";

	public record OlyClassBalance(
			int classId,
			String className,
			double toFighter,
			double toMage,
			double byFighter,
			double byMage,
			double pAtkMul,
			double mAtkMul,
			double critMul,
			double healMul
	) {
		public static final OlyClassBalance DEFAULT = new OlyClassBalance(
				0, "Default", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0
		);
	}

	private final Map<Integer, OlyClassBalance> balances = new ConcurrentHashMap<>();

	@PostConstruct
	public void init() {
		loadConfig();
	}

	/**
	 * Recarrega a configuracao declarativa do XML sem reiniciar a JVM (GM //reload oly / balance).
	 *
	 * @return quantidade de classes carregadas
	 */
	public synchronized int loadConfig() {
		File externalFile = new File(XML_PATH_EXTERNAL);
		if (externalFile.exists()) {
			try (InputStream is = new FileInputStream(externalFile)) {
				return load(is, "arquivo externo " + externalFile.getAbsolutePath());
			} catch (Exception e) {
				log.warn("Erro ao carregar olympiad_balance.xml externo: {}", e.getMessage(), e);
			}
		}

		try {
			ClassPathResource resource = new ClassPathResource(XML_PATH_CLASSPATH);
			if (resource.exists()) {
				try (InputStream is = resource.getInputStream()) {
					return load(is, "classpath " + XML_PATH_CLASSPATH);
				}
			}
		} catch (Exception e) {
			log.warn("Erro ao carregar olympiad_balance.xml do classpath: {}", e.getMessage(), e);
		}

		log.info("Nenhum olympiad_balance.xml encontrado. Mantendo valores padrao (1.0).");
		return balances.size();
	}

	public synchronized int load(InputStream in, String sourceDescription) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			Document doc = factory.newDocumentBuilder().parse(in);
			Element root = doc.getDocumentElement();

			NodeList nodes = root.getElementsByTagName("balance");
			Map<Integer, OlyClassBalance> loaded = new ConcurrentHashMap<>();

			for (int i = 0; i < nodes.getLength(); i++) {
				Node node = nodes.item(i);
				if (node.getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}
				Element element = (Element) node;
				int classId = Integer.parseInt(element.getAttribute("classId"));
				String className = element.hasAttribute("className") ? element.getAttribute("className") : "Class " + classId;

				double toFighter = parseDouble(element, "toFighter", 1.0);
				double toMage = parseDouble(element, "toMage", 1.0);
				double byFighter = parseDouble(element, "byFighter", 1.0);
				double byMage = parseDouble(element, "byMage", 1.0);
				double pAtkMul = parseDouble(element, "pAtkMul", 1.0);
				double mAtkMul = parseDouble(element, "mAtkMul", 1.0);
				double critMul = parseDouble(element, "critMul", 1.0);
				double healMul = parseDouble(element, "healMul", 1.0);

				OlyClassBalance balance = new OlyClassBalance(
						classId, className, toFighter, toMage, byFighter, byMage,
						pAtkMul, mAtkMul, critMul, healMul
				);
				loaded.put(classId, balance);
			}

			balances.clear();
			balances.putAll(loaded);
			log.info("OlyClassDamageManager: {} classes balanceadas carregadas de {}.", balances.size(), sourceDescription);
			return balances.size();
		} catch (Exception e) {
			log.error("Falha ao parsear XML de balanceamento de Olimpiadas: {}", e.getMessage(), e);
			return 0;
		}
	}

	private double parseDouble(Element element, String attr, double defaultValue) {
		if (element.hasAttribute(attr)) {
			try {
				return Double.parseDouble(element.getAttribute(attr));
			} catch (NumberFormatException ignored) {
			}
		}
		return defaultValue;
	}

	/**
	 * Calcula o multiplicador combinado de dano entre atacante e atacado.
	 *
	 * <p>Regra de Ouro (Isolamento Total): Se qualquer um dos personagens NAO estiver no modo
	 * Olimpiada, retorna estritamente 1.0 (mantendo o PvE e PvP de mundo aberto 100% oficial).</p>
	 */
	public double getDamageMultiplier(PlayerCharacter attacker, PlayerCharacter attacked) {
		if (attacker == null || attacked == null) {
			return 1.0;
		}
		if (!attacker.isOlympiadMode() || !attacked.isOlympiadMode()) {
			return 1.0;
		}

		double attackerMulti = attacked.isMage()
				? getClassDamageToMage(attacker.classId())
				: getClassDamageToFighter(attacker.classId());

		double attackedMulti = attacker.isMage()
				? getClassDamageByMage(attacked.classId())
				: getClassDamageByFighter(attacked.classId());

		return attackerMulti * attackedMulti;
	}

	public double getClassDamageToMage(int classId) {
		return balances.getOrDefault(classId, OlyClassBalance.DEFAULT).toMage();
	}

	public double getClassDamageToFighter(int classId) {
		return balances.getOrDefault(classId, OlyClassBalance.DEFAULT).toFighter();
	}

	public double getClassDamageByMage(int classId) {
		return balances.getOrDefault(classId, OlyClassBalance.DEFAULT).byMage();
	}

	public double getClassDamageByFighter(int classId) {
		return balances.getOrDefault(classId, OlyClassBalance.DEFAULT).byFighter();
	}

	public double getPatkMultiplier(int classId) {
		return balances.getOrDefault(classId, OlyClassBalance.DEFAULT).pAtkMul();
	}

	public double getMatkMultiplier(int classId) {
		return balances.getOrDefault(classId, OlyClassBalance.DEFAULT).mAtkMul();
	}

	public double getCritMultiplier(int classId) {
		return balances.getOrDefault(classId, OlyClassBalance.DEFAULT).critMul();
	}

	public double getHealMultiplier(int classId) {
		return balances.getOrDefault(classId, OlyClassBalance.DEFAULT).healMul();
	}

	public void setBalance(int classId, OlyClassBalance balance) {
		if (balance != null) {
			balances.put(classId, balance);
		} else {
			balances.remove(classId);
		}
	}

	public OlyClassBalance getBalance(int classId) {
		return balances.getOrDefault(classId, OlyClassBalance.DEFAULT);
	}

	public Map<Integer, OlyClassBalance> getAllBalances() {
		return Collections.unmodifiableMap(balances);
	}
}
