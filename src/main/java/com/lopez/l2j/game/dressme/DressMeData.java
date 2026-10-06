package com.lopez.l2j.game.dressme;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Carregador de dados das skins do sistema DressMe (Item 30 do Roteiro Mestre).
 * Leitura do arquivo XML `data/xml/DressMeData.xml`.
 */
@Component
public class DressMeData {

	private static final Logger log = LoggerFactory.getLogger(DressMeData.class);
	private static final String DEFAULT_PATH = "data/xml/DressMeData.xml";

	private final Map<Integer, DressMeEntry> entriesBySkillId = new ConcurrentHashMap<>();

	public DressMeData() {
		load(DEFAULT_PATH);
	}

	public DressMeData(String customPath) {
		load(customPath);
	}

	public synchronized void load(String filePath) {
		entriesBySkillId.clear();
		File f = new File(filePath);
		if (!f.exists()) {
			log.warn("Arquivo DressMe nao encontrado em {}", f.getAbsolutePath());
			return;
		}

		try {
			var factory = DocumentBuilderFactory.newInstance();
			var builder = factory.newDocumentBuilder();
			Document doc = builder.parse(f);
			doc.getDocumentElement().normalize();

			NodeList dressNodes = doc.getElementsByTagName("dress");
			for (int i = 0; i < dressNodes.getLength(); i++) {
				Element dEl = (Element) dressNodes.item(i);
				int skillId = Integer.parseInt(dEl.getAttribute("skillId"));
				String name = dEl.getAttribute("name");
				String type = dEl.getAttribute("type");
				boolean vip = Boolean.parseBoolean(dEl.getAttribute("isVip"));

				DressMeEntry.VisualArmor armor = null;
				DressMeEntry.VisualWeapon weapon = null;
				DressMeEntry.VisualEffect effect = null;

				NodeList sets = dEl.getElementsByTagName("visualSet");
				if (sets.getLength() > 0) {
					Element setEl = (Element) sets.item(0);
					int chest = parseAttrInt(setEl, "chest", 0);
					int legs = parseAttrInt(setEl, "legs", 0);
					int gloves = parseAttrInt(setEl, "gloves", 0);
					int feet = parseAttrInt(setEl, "feet", 0);
					int helmet = parseAttrInt(setEl, "helmet", 0);
					armor = new DressMeEntry.VisualArmor(chest, legs, gloves, feet, helmet);
				}

				NodeList weps = dEl.getElementsByTagName("visualWep");
				if (weps.getLength() > 0) {
					Element wepEl = (Element) weps.item(0);
					String wepType = wepEl.getAttribute("type");
					int rhand = parseAttrInt(wepEl, "rhand", 0);
					int lhand = parseAttrInt(wepEl, "lhand", 0);
					int lrhand = parseAttrInt(wepEl, "lrhand", 0);
					weapon = new DressMeEntry.VisualWeapon(wepType, rhand, lhand, lrhand);
				}

				NodeList fx = dEl.getElementsByTagName("visualEffect");
				if (fx.getLength() > 0) {
					Element fxEl = (Element) fx.item(0);
					int fxSkillId = parseAttrInt(fxEl, "skillId", 0);
					int fxLevel = parseAttrInt(fxEl, "level", 1);
					boolean recurring = Boolean.parseBoolean(fxEl.getAttribute("recurring"));
					int interval = parseAttrInt(fxEl, "interval", 30);
					effect = new DressMeEntry.VisualEffect(fxSkillId, fxLevel, recurring, interval);
				}

				var entry = new DressMeEntry(skillId, name, type, vip, armor, weapon, effect);
				entriesBySkillId.put(skillId, entry);
			}

			log.info("DressMeData: {} skins visuais carregadas de {}", entriesBySkillId.size(), filePath);
		} catch (Exception e) {
			log.error("Erro ao processar DressMeData.xml: {}", e.getMessage(), e);
		}
	}

	public Optional<DressMeEntry> get(int skillId) {
		return Optional.ofNullable(entriesBySkillId.get(skillId));
	}

	public Collection<DressMeEntry> all() {
		return Collections.unmodifiableCollection(entriesBySkillId.values());
	}

	public int size() {
		return entriesBySkillId.size();
	}

	private static int parseAttrInt(Element el, String attr, int def) {
		String val = el.getAttribute(attr);
		if (val == null || val.isBlank()) {
			return def;
		}
		try {
			return Integer.parseInt(val.trim());
		} catch (Exception e) {
			return def;
		}
	}
}
