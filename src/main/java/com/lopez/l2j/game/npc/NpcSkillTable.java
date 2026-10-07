package com.lopez.l2j.game.npc;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
 * Tabela oficial de habilidades vinculadas a NPCs e Monstros do Lineage II Interlude.
 * Carrega {@code data/xml/world/npc_skills.xml}, indexando as habilidades ativas e passivas
 * por {@code npc_id}.
 */
@Component
public class NpcSkillTable {

	private static final Logger log = LoggerFactory.getLogger(NpcSkillTable.class);
	public static final String FILE_PATH = "data/xml/world/npc_skills.xml";

	public record NpcSkillHolder(int skillId, int level) {}

	private final Map<Integer, List<NpcSkillHolder>> skillsByNpc = new HashMap<>();

	@PostConstruct
	public void init() {
		load();
	}

	public synchronized void load() {
		File file = new File(FILE_PATH);
		if (!file.exists()) {
			log.warn("NpcSkillTable: arquivo {} não encontrado", file.getAbsolutePath());
			return;
		}

		skillsByNpc.clear();
		long start = System.currentTimeMillis();
		int totalAssociations = 0;

		try {
			var factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			Document doc = factory.newDocumentBuilder().parse(file);
			NodeList list = doc.getElementsByTagName("npc_skills");

			for (int i = 0; i < list.getLength(); i++) {
				Node node = list.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element el = (Element) node;
					int npcId = Integer.parseInt(el.getAttribute("npc_id"));
					int skillId = Integer.parseInt(el.getAttribute("skill_id"));
					int level = Integer.parseInt(el.getAttribute("level"));

					skillsByNpc.computeIfAbsent(npcId, k -> new ArrayList<>())
							.add(new NpcSkillHolder(skillId, level));
					totalAssociations++;
				}
			}

			long elapsed = System.currentTimeMillis() - start;
			log.info("NpcSkillTable: {} habilidades carregadas para {} NPCs em {} ms",
					totalAssociations, skillsByNpc.size(), elapsed);
		} catch (Exception e) {
			log.error("NpcSkillTable: falha ao carregar {}: {}", FILE_PATH, e.getMessage(), e);
		}
	}

	public List<NpcSkillHolder> getSkills(int npcId) {
		return skillsByNpc.getOrDefault(npcId, Collections.emptyList());
	}

	public boolean hasSkill(int npcId, int skillId) {
		List<NpcSkillHolder> holders = skillsByNpc.get(npcId);
		if (holders == null || holders.isEmpty()) {
			return false;
		}
		for (NpcSkillHolder h : holders) {
			if (h.skillId() == skillId) {
				return true;
			}
		}
		return false;
	}

	public int getSkillLevel(int npcId, int skillId) {
		List<NpcSkillHolder> holders = skillsByNpc.get(npcId);
		if (holders != null) {
			for (NpcSkillHolder h : holders) {
				if (h.skillId() == skillId) {
					return h.level();
				}
			}
		}
		return 0;
	}

	public int size() {
		return skillsByNpc.size();
	}
}
