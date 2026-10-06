package com.lopez.l2j.game.clan.skill;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Tabela que carrega a arvore de habilidades de cla (SkillTreeTable._pledgeSkillTrees do L2JDream).
 * Carrega a partir de data/xml/player/skills/pledge_skill_tree.xml.
 */
@Component
public class ClanSkillTreeTable {

	private static final Logger log = LoggerFactory.getLogger(ClanSkillTreeTable.class);
	private static final String DEFAULT_XML_PATH = "data/xml/player/skills/pledge_skill_tree.xml";

	private final List<PledgeSkillRecord> allSkills = new ArrayList<>();
	// (skillId, level) -> Record
	private final Map<String, PledgeSkillRecord> skillMap = new ConcurrentHashMap<>();

	public ClanSkillTreeTable() {
		loadFromXml(DEFAULT_XML_PATH);
	}

	public void loadFromXml(String filePath) {
		allSkills.clear();
		skillMap.clear();

		File file = new File(filePath);
		if (!file.exists()) {
			log.warn("Arquivo de habilidades de cla nao encontrado em: {}", filePath);
			return;
		}

		try (InputStream is = Files.newInputStream(file.toPath())) {
			var factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			Document doc = factory.newDocumentBuilder().parse(is);

			for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling()) {
				if ("list".equalsIgnoreCase(n.getNodeName())) {
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
						if ("PledgeSkill".equalsIgnoreCase(d.getNodeName())) {
							var attrs = d.getAttributes();
							int skillId = Integer.parseInt(attrs.getNamedItem("skill_id").getNodeValue());
							int level = Integer.parseInt(attrs.getNamedItem("level").getNodeValue());
							String name = attrs.getNamedItem("name").getNodeValue();
							int minClanLevel = Integer.parseInt(attrs.getNamedItem("clan_lvl").getNodeValue());
							int repCost = Integer.parseInt(attrs.getNamedItem("repCost").getNodeValue());
							int itemId = Integer.parseInt(attrs.getNamedItem("itemId").getNodeValue());

							var record = new PledgeSkillRecord(skillId, level, name, minClanLevel, repCost, itemId);
							allSkills.add(record);
							skillMap.put(skillKey(skillId, level), record);
						}
					}
				}
			}
			log.info("ClanSkillTreeTable: {} habilidades de cla carregadas de {}", allSkills.size(), filePath);
		} catch (Exception ex) {
			log.error("Erro ao carregar ClanSkillTreeTable de {}", filePath, ex);
		}
	}

	public List<PledgeSkillRecord> allSkills() {
		return Collections.unmodifiableList(allSkills);
	}

	public Optional<PledgeSkillRecord> getSkill(int skillId, int level) {
		return Optional.ofNullable(skillMap.get(skillKey(skillId, level)));
	}

	public List<PledgeSkillRecord> getSkillsForSkillId(int skillId) {
		return allSkills.stream().filter(s -> s.skillId() == skillId).toList();
	}

	private String skillKey(int skillId, int level) {
		return skillId + "_" + level;
	}
}
