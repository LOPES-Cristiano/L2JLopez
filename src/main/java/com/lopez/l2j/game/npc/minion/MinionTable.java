package com.lopez.l2j.game.npc.minion;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tabela que carrega as definicoes de lacaios/escoltas (minions) de chefes e monstros
 * a partir de {@code data/xml/world/minion.xml}.
 */
@Component
public class MinionTable {

	private static final Logger log = LoggerFactory.getLogger(MinionTable.class);

	private final Map<Integer, List<MinionEntry>> minionsByBossId = new ConcurrentHashMap<>();
	private final String minionFilePath;

	public MinionTable(@Value("${l2.datapack.minion-file:data/xml/world/minion.xml}") String minionFilePath) {
		this.minionFilePath = minionFilePath;
	}

	@PostConstruct
	public void load() {
		File file = new File(minionFilePath);
		if (!file.exists()) {
			log.warn("MinionTable: arquivo {} nao encontrado. Nenhum minion carregado.", minionFilePath);
			return;
		}

		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(file);

			NodeList list = doc.getElementsByTagName("minion");
			int count = 0;
			minionsByBossId.clear();

			for (int i = 0; i < list.getLength(); i++) {
				Node node = list.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element el = (Element) node;
					int bossId = Integer.parseInt(el.getAttribute("boss_id"));
					int minionId = Integer.parseInt(el.getAttribute("minion_id"));
					int min = Integer.parseInt(el.getAttribute("amount_min"));
					int max = Integer.parseInt(el.getAttribute("amount_max"));

					MinionEntry entry = new MinionEntry(bossId, minionId, min, max);
					minionsByBossId.computeIfAbsent(bossId, k -> new ArrayList<>()).add(entry);
					count++;
				}
			}

			log.info("MinionTable: carregadas {} regras de lacaios para {} monstros/chefes de {}",
					count, minionsByBossId.size(), file.getPath());
		} catch (Exception e) {
			log.error("Erro ao carregar minion.xml: {}", e.getMessage(), e);
		}
	}

	public List<MinionEntry> getMinionsForBoss(int bossId) {
		return minionsByBossId.getOrDefault(bossId, Collections.emptyList());
	}

	public boolean hasMinions(int bossId) {
		return minionsByBossId.containsKey(bossId);
	}

	public int totalMasters() {
		return minionsByBossId.size();
	}

	public int totalMinionRules() {
		return minionsByBossId.values().stream().mapToInt(List::size).sum();
	}
}
