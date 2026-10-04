package com.lopez.l2j.game.skill;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

/**
 * Arvores de skills por classe (skill_tree.xml, com heranca pelo parent_id), quais NPCs ensinam quais classes
 * (skill_learn.xml) e os livros exigidos (skill_spellbooks.xml). Porta de SkillTreeTable do L2JDream.
 */
@Component
public class SkillTreeTable {

	private static final Logger log = LoggerFactory.getLogger(SkillTreeTable.class);

	/** Entrada da arvore: aprender {@code id} no nivel {@code level} custa {@code sp} a partir de {@code minLevel}. */
	public record SkillLearn(int id, int level, String name, int sp, int minLevel) {
	}

	public record EnchantSkillLearn(int id, int level, int minSkillLvl, int baseLvl, String name, int sp, int exp,
			int rate76, int rate77, int rate78) {
		public int rate(int playerLevel) {
			if (playerLevel <= 76) {
				return rate76;
			}
			if (playerLevel == 77) {
				return rate77;
			}
			return rate78;
		}
	}

	private record ClassTree(int parentId, List<SkillLearn> skills) {
	}

	private final Map<Integer, ClassTree> trees = new HashMap<>();
	private final Map<Integer, Set<Integer>> classesByTrainer = new HashMap<>();
	private final Map<Integer, Integer> spellbookBySkill = new HashMap<>();
	private final List<EnchantSkillLearn> enchantSkills = new ArrayList<>();
	private final Map<Long, EnchantSkillLearn> enchantBySkillAndLevel = new HashMap<>();

	@Autowired
	public SkillTreeTable(@Value("${l2.datapack.xml-dir:data/xml}") String xmlDir) {
		this(Path.of(xmlDir, "player", "skills"));
	}

	public SkillTreeTable(Path dir) {
		parse(dir.resolve("skill_tree.xml"), this::readTree);
		parse(dir.resolve("skill_learn.xml"), this::readLearn);
		parse(dir.resolve("skill_spellbooks.xml"), this::readSpellbooks);
		parse(dir.resolve("enchant_skill_tree.xml"), this::readEnchant);
		log.info("SkillTreeTable: {} classes, {} treinadores, {} livros, {} skills encantaveis", trees.size(),
				classesByTrainer.size(), spellbookBySkill.size(), enchantSkills.size());
	}

	/** Todas as entradas da classe, incluindo as herdadas das classes pai. */
	public List<SkillLearn> allFor(int classId) {
		List<SkillLearn> out = new ArrayList<>();
		Set<Integer> seen = new HashSet<>();
		Integer current = classId;
		while (current != null && current >= 0 && seen.add(current)) {
			ClassTree t = trees.get(current);
			if (t == null) {
				break;
			}
			out.addAll(t.skills());
			current = t.parentId();
		}
		return out;
	}

	/**
	 * Skills que o jogador pode aprender agora (getAvailableSkills do L2J): o proximo nivel de um skill conhecido
	 * ou o nivel 1 de um skill novo, com min_level atingido.
	 */
	public List<SkillLearn> available(int classId, int playerLevel, Map<Integer, Integer> known) {
		List<SkillLearn> out = new ArrayList<>();
		for (SkillLearn s : allFor(classId)) {
			if (s.minLevel() > playerLevel) {
				continue;
			}
			Integer current = known.get(s.id());
			if (current == null ? s.level() == 1 : current == s.level() - 1) {
				out.add(s);
			}
		}
		return out;
	}

	/** Menor nivel em que surge um skill novo com custo de SP (0 = nenhum). */
	public int minLevelForNewSkill(int classId, int playerLevel) {
		int min = 0;
		for (SkillLearn s : allFor(classId)) {
			if (s.minLevel() > playerLevel && s.sp() != 0 && (min == 0 || s.minLevel() < min)) {
				min = s.minLevel();
			}
		}
		return min;
	}

	/**
	 * Maior nivel de cada skill ja liberado para o nivel do personagem (giveAvailableSkills do AutoLearnSkills).
	 *
	 * @param freeOnly so entradas com sp=0 (Expertise, Common Craft...), que o L2J entrega sempre
	 */
	public Map<Integer, Integer> unlocked(int classId, int playerLevel, boolean freeOnly) {
		Map<Integer, Integer> out = new HashMap<>();
		for (SkillLearn s : allFor(classId)) {
			if (s.minLevel() <= playerLevel && (!freeOnly || s.sp() == 0)) {
				out.merge(s.id(), s.level(), Math::max);
			}
		}
		return out;
	}

	public boolean canTeach(int npcId, int classId) {
		Set<Integer> classes = classesByTrainer.get(npcId);
		return classes != null && classes.contains(classId);
	}

	public boolean isTrainer(int npcId) {
		return classesByTrainer.containsKey(npcId);
	}

	/** Item exigido para aprender o nivel 1 do skill, ou -1. */
	public int spellbookFor(int skillId) {
		return spellbookBySkill.getOrDefault(skillId, -1);
	}

	public Collection<Integer> classIds() {
		return trees.keySet();
	}

	// ---- parsing ----

	private interface Reader {
		void read(Element root);
	}

	private static void parse(Path file, Reader reader) {
		if (!Files.isRegularFile(file)) {
			log.warn("{} nao encontrado", file.toAbsolutePath());
			return;
		}
		try (InputStream in = Files.newInputStream(file)) {
			var f = DocumentBuilderFactory.newInstance();
			f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			f.setIgnoringComments(true);
			reader.read(f.newDocumentBuilder().parse(in).getDocumentElement());
		} catch (Exception e) {
			log.warn("Falha ao ler {}: {}", file.getFileName(), e.getMessage());
		}
	}

	private void readTree(Element root) {
		for (Element cls : SkillCondition.children(root)) {
			if (!cls.getTagName().equals("skill")) {
				continue;
			}
			int classId = Integer.parseInt(cls.getAttribute("class_id").trim());
			int parentId = Integer.parseInt(cls.getAttribute("parent_id").trim());
			List<SkillLearn> skills = new ArrayList<>();
			for (Element d : SkillCondition.children(cls)) {
				if (!d.getTagName().equals("data")) {
					continue;
				}
				skills.add(new SkillLearn(Integer.parseInt(d.getAttribute("skill_id").trim()),
						Integer.parseInt(d.getAttribute("level").trim()), d.getAttribute("name"),
						Integer.parseInt(d.getAttribute("sp").trim()),
						Integer.parseInt(d.getAttribute("min_level").trim())));
			}
			trees.put(classId, new ClassTree(parentId, List.copyOf(skills)));
		}
	}

	private void readLearn(Element root) {
		for (Element l : SkillCondition.children(root)) {
			if (l.getTagName().equals("learn")) {
				classesByTrainer.computeIfAbsent(Integer.parseInt(l.getAttribute("npc_id").trim()), k -> new HashSet<>())
						.add(Integer.parseInt(l.getAttribute("class_id").trim()));
			}
		}
	}

	public List<EnchantSkillLearn> availableEnchantSkills(Map<Integer, Integer> known) {
		List<EnchantSkillLearn> out = new ArrayList<>();
		for (EnchantSkillLearn s : enchantSkills) {
			Integer current = known.get(s.id());
			if (current != null && current == s.minSkillLvl()) {
				out.add(s);
			}
		}
		return out;
	}

	public java.util.Optional<EnchantSkillLearn> getEnchantSkill(int id, int level) {
		return java.util.Optional.ofNullable(enchantBySkillAndLevel.get(((long) id << 32) | (long) level));
	}

	private void readSpellbooks(Element root) {
		for (Element b : SkillCondition.children(root)) {
			if (b.getTagName().equals("skill_spellbook")) {
				spellbookBySkill.put(Integer.parseInt(b.getAttribute("skill_id").trim()),
						Integer.parseInt(b.getAttribute("item_id").trim()));
			}
		}
	}

	private void readEnchant(Element root) {
		for (Element e : SkillCondition.children(root)) {
			if (!e.getTagName().equals("enchant")) {
				continue;
			}
			int id = Integer.parseInt(e.getAttribute("id").trim());
			String name = e.getAttribute("name");
			int baseLvl = Integer.parseInt(e.getAttribute("base_lvl").trim());
			for (Element d : SkillCondition.children(e)) {
				if (!d.getTagName().equals("data")) {
					continue;
				}
				int lvl = Integer.parseInt(d.getAttribute("level").trim());
				int sp = Integer.parseInt(d.getAttribute("sp").trim());
				int exp = Integer.parseInt(d.getAttribute("exp").trim());
				int rate76 = Integer.parseInt(d.getAttribute("rate76").trim());
				int rate77 = Integer.parseInt(d.getAttribute("rate77").trim());
				int rate78 = Integer.parseInt(d.getAttribute("rate78").trim());
				int minSkillLvl = (lvl == 101 || lvl == 141 || lvl == 181 || lvl == 221) ? baseLvl : lvl - 1;
				var es = new EnchantSkillLearn(id, lvl, minSkillLvl, baseLvl, name, sp, exp, rate76, rate77, rate78);
				enchantSkills.add(es);
				enchantBySkillAndLevel.put(((long) id << 32) | (long) lvl, es);
			}
		}
	}
}
