package com.lopez.l2j.game.item;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Tabela de bonus de conjuntos de armadura (armorsets.xml).
 * Aplica as habilidades passivas correspondentes (ex: Dark Crystal Robe +15% Cast Spd, Majestic, Tallum, etc.).
 */
@Component
public class ArmorSetsTable {

	private static final Logger log = LoggerFactory.getLogger(ArmorSetsTable.class);

	public record ArmorSet(int id, int chest, int legs, int head, int gloves, int feet,
			int skillId, int shield, int shieldSkillId, int enchant6Skill) {

		public boolean containAll(Inventory inv) {
			if (inv == null) {
				return false;
			}
			// Chest deve estar equipado
			if (!inv.isEquipped(chest)) {
				return false;
			}
			if (legs != 0 && !inv.isEquipped(legs)) {
				return false;
			}
			if (head != 0 && !inv.isEquipped(head)) {
				return false;
			}
			if (gloves != 0 && !inv.isEquipped(gloves)) {
				return false;
			}
			if (feet != 0 && !inv.isEquipped(feet)) {
				return false;
			}
			return true;
		}

		public boolean containShield(Inventory inv) {
			return shield != 0 && inv != null && inv.isEquipped(shield);
		}

		public boolean hasShield(Inventory inv) {
			return containShield(inv);
		}

		public boolean isEnchanted6(Inventory inv) {
			return isEnchant6(inv);
		}

		public boolean isEnchant6(Inventory inv) {
			if (inv == null || !containAll(inv)) {
				return false;
			}
			if (getEnchant(inv, chest) < 6) return false;
			if (legs != 0 && getEnchant(inv, legs) < 6) return false;
			if (head != 0 && getEnchant(inv, head) < 6) return false;
			if (gloves != 0 && getEnchant(inv, gloves) < 6) return false;
			if (feet != 0 && getEnchant(inv, feet) < 6) return false;
			return true;
		}

		private static int getEnchant(Inventory inv, int itemId) {
			return inv.equippedItems().stream()
					.filter(it -> it.itemId() == itemId)
					.mapToInt(ItemInstance::enchant)
					.max()
					.orElse(0);
		}
	}

	private final Map<Integer, ArmorSet> byChest = new HashMap<>();
	private final List<ArmorSet> allSets = new ArrayList<>();

	@Autowired
	public ArmorSetsTable(@Value("${l2.datapack.xml-dir:data/xml}") String xmlDir) {
		this(Path.of(xmlDir, "player", "armorsets.xml"));
	}

	public ArmorSetsTable(Path xmlFile) {
		if (Files.isRegularFile(xmlFile)) {
			load(xmlFile);
		} else {
			log.warn("Arquivo armorsets.xml nao encontrado em {}", xmlFile.toAbsolutePath());
		}
	}

	public int size() {
		return allSets.size();
	}

	public Optional<ArmorSet> byChest(int chestId) {
		return Optional.ofNullable(byChest.get(chestId));
	}

	public List<ArmorSet> all() {
		return Collections.unmodifiableList(allSets);
	}

	/**
	 * Retorna o conjunto completo ativo para o inventario informado (ou empty).
	 */
	public Optional<ArmorSet> activeSet(Inventory inv) {
		if (inv == null) {
			return Optional.empty();
		}
		for (ItemInstance eq : inv.equippedItems()) {
			ArmorSet set = byChest.get(eq.itemId());
			if (set != null && set.containAll(inv)) {
				return Optional.of(set);
			}
		}
		return Optional.empty();
	}

	public ArmorSet findMatchingSet(Inventory inv) {
		return activeSet(inv).orElse(null);
	}

	private void load(Path file) {
		try (InputStream in = Files.newInputStream(file)) {
			var factory = DocumentBuilderFactory.newInstance();
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			var doc = factory.newDocumentBuilder().parse(in);

			NodeList nodes = doc.getElementsByTagName("armorset");
			for (int i = 0; i < nodes.getLength(); i++) {
				Element e = (Element) nodes.item(i);
				int id = Integer.parseInt(e.getAttribute("id"));
				int chest = Integer.parseInt(e.getAttribute("chest"));
				int legs = Integer.parseInt(e.getAttribute("legs"));
				int head = Integer.parseInt(e.getAttribute("head"));
				int gloves = Integer.parseInt(e.getAttribute("gloves"));
				int feet = Integer.parseInt(e.getAttribute("feet"));
				int skillId = Integer.parseInt(e.getAttribute("skill_id"));
				int shield = Integer.parseInt(e.getAttribute("shield"));
				int shieldSkillId = Integer.parseInt(e.getAttribute("shield_skill_id"));
				int enchant6Skill = Integer.parseInt(e.getAttribute("enchant6skill"));

				ArmorSet set = new ArmorSet(id, chest, legs, head, gloves, feet,
						skillId, shield, shieldSkillId, enchant6Skill);
				byChest.put(chest, set);
				allSets.add(set);
			}

			log.info("ArmorSetsTable: {} conjuntos de armadura carregados de {}", allSets.size(), file.getFileName());
		} catch (Exception ex) {
			log.error("Erro ao carregar armorsets de {}: {}", file, ex.getMessage(), ex);
		}
	}
}
