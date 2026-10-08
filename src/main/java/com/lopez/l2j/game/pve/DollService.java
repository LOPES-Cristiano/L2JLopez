package com.lopez.l2j.game.pve;

import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servico para gerenciamento das Dolls de Chefes (Baium, Antharas, Zaken, etc.) - Onda C9.
 *
 * <p>Regras:</p>
 * <ul>
 *   <li>Dolls no inventario concedem passivas e atributos leves ao personagem.</li>
 *   <li><b>Nao-cumulativas</b>: Dolls do mesmo chefe nao acumulam (aplica-se apenas a de maior nivel).</li>
 *   <li>Dolls de chefes distintos funcionam em conjunto simultaneamente.</li>
 * </ul>
 */
@Service
public class DollService {

	private static final Logger log = LoggerFactory.getLogger(DollService.class);

	public record BossDoll(int itemId, String boss, int skillId, int level, double power) {}

	private final Map<Integer, BossDoll> dollsById = new ConcurrentHashMap<>();

	public DollService() {
		loadDolls();
	}

	public void loadDolls() {
		dollsById.clear();
		try {
			File file = new File("data/xml/custom/Dolls.xml");
			InputStream is = file.exists() ? new java.io.FileInputStream(file) : getClass().getResourceAsStream("/data/xml/custom/Dolls.xml");
			if (is == null) {
				log.warn("Dolls.xml nao encontrado.");
				return;
			}

			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
			NodeList dollNodes = doc.getElementsByTagName("Doll");

			for (int i = 0; i < dollNodes.getLength(); i++) {
				Element elem = (Element) dollNodes.item(i);
				int id = Integer.parseInt(elem.getAttribute("Id"));
				String boss = elem.hasAttribute("Boss") ? elem.getAttribute("Boss") : "Boss_" + id;
				int skillId = Integer.parseInt(elem.getAttribute("SkillId"));
				int skillLvl = Integer.parseInt(elem.getAttribute("SkillLvl"));
				double power = elem.hasAttribute("SkillPower") ? Double.parseDouble(elem.getAttribute("SkillPower")) : 2.0;

				dollsById.put(id, new BossDoll(id, boss, skillId, skillLvl, power));
			}

			log.info("DollService: {} dolls de chefes carregadas com sucesso.", dollsById.size());
		} catch (Exception e) {
			log.error("Erro ao carregar Dolls.xml: {}", e.getMessage(), e);
		}
	}

	public boolean isDoll(int itemId) {
		return dollsById.containsKey(itemId);
	}

	public BossDoll getDoll(int itemId) {
		return dollsById.get(itemId);
	}

	/**
	 * Filtra as dolls ativas do jogador aplicando a regra de NAO-CUMULATIVIDADE:
	 * se possuir mais de uma doll do mesmo chefe (ex: Baium Lv 1 e Lv 3), apenas a de MAIOR nivel e considerada.
	 */
	public Map<String, BossDoll> getActiveDolls(PlayerCharacter player) {
		if (player == null || player.inventory() == null) {
			return Collections.emptyMap();
		}

		Map<String, BossDoll> activeBossDolls = new HashMap<>();

		for (ItemInstance item : player.inventory().items()) {
			if (item == null) continue;
			BossDoll doll = dollsById.get(item.itemId());
			if (doll != null) {
				// Verifica se ja temos uma doll desse boss
				BossDoll existing = activeBossDolls.get(doll.boss());
				if (existing == null || doll.level() > existing.level()) {
					activeBossDolls.put(doll.boss(), doll);
				}
			}
		}

		return Collections.unmodifiableMap(activeBossDolls);
	}

	/**
	 * Calcula o somatorio de poder conferido pelas dolls ativas.
	 */
	public double calculateTotalPower(PlayerCharacter player) {
		Map<String, BossDoll> active = getActiveDolls(player);
		double total = 0.0;
		for (BossDoll doll : active.values()) {
			total += doll.power();
		}
		return total;
	}

	public Map<Integer, BossDoll> allDolls() {
		return Collections.unmodifiableMap(dollsById);
	}
}
