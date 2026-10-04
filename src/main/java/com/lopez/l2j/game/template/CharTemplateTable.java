package com.lopez.l2j.game.template;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Porta do CharTemplateTable: carrega {@code data/player/char_template.xml} do classpath. Atributos
 * ausentes ou invalidos falham na carga (melhor nao subir do que criar personagens quebrados).
 */
@Component
public class CharTemplateTable {

	public static final String RESOURCE = "data/player/char_template.xml";

	/** Ordem enviada pelo legado no NewCharacterSuccess (o template 0 aparece duas vezes, de proposito). */
	static final int[] CREATION_ORDER = { 0, 0, 10, 18, 25, 31, 38, 44, 49, 53 };

	private final Map<Integer, CharTemplate> byClass;

	public CharTemplateTable() {
		this(new ClassPathResource(RESOURCE));
	}

	CharTemplateTable(ClassPathResource resource) {
		try (InputStream in = resource.getInputStream()) {
			byClass = Collections.unmodifiableMap(parse(in));
		} catch (IOException e) {
			throw new IllegalStateException("Nao foi possivel ler " + resource.getPath(), e);
		}
	}

	public Optional<CharTemplate> get(int classId) {
		return Optional.ofNullable(byClass.get(classId));
	}

	public int size() {
		return byClass.size();
	}

	/** Templates exibidos na tela de criacao, na ordem do legado. */
	public List<CharTemplate> creationTemplates() {
		return java.util.Arrays.stream(CREATION_ORDER).mapToObj(byClass::get).toList();
	}

	static Map<Integer, CharTemplate> parse(InputStream in) {
		try {
			var factory = DocumentBuilderFactory.newInstance();
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			var doc = factory.newDocumentBuilder().parse(in);
			NodeList classes = doc.getElementsByTagName("class");
			Map<Integer, CharTemplate> result = new LinkedHashMap<>();
			for (int i = 0; i < classes.getLength(); i++) {
				Element c = (Element) classes.item(i);
				Element s = child(c, "stats");
				Element l = child(s, "lvlup");
				var t = new CharTemplate(
						i(c, "Id"), c.getAttribute("name"), i(c, "RaceId"),
						i(s, "str"), i(s, "con"), i(s, "dex"), i(s, "_int"), i(s, "wit"), i(s, "men"),
						i(s, "p_atk"), i(s, "p_def"), i(s, "m_atk"), i(s, "m_def"), i(s, "p_spd"), i(s, "m_spd"),
						i(s, "acc"), i(s, "critical"), i(s, "evasion"), i(s, "move_spd"), i(s, "_load"),
						i(s, "x"), i(s, "y"), i(s, "z"),
						i(s, "canCraft") == 1,
						d(s, "m_col_r"), d(s, "m_col_h"), d(s, "f_col_r"), d(s, "f_col_h"),
						d(l, "hpbase"), d(l, "mpbase"), d(l, "cpbase"),
						i(l, "class_lvl"));
				if (result.put(t.classId(), t) != null) {
					throw new IllegalStateException("classId duplicado em char_template.xml: " + t.classId());
				}
			}
			for (int id : CREATION_ORDER) {
				if (!result.containsKey(id)) {
					throw new IllegalStateException("char_template.xml sem a classe inicial " + id);
				}
			}
			return result;
		} catch (IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("char_template.xml invalido: " + e.getMessage(), e);
		}
	}

	private static Element child(Element parent, String tag) {
		NodeList list = parent.getElementsByTagName(tag);
		if (list.getLength() == 0) {
			throw new IllegalStateException("<" + tag + "> ausente em " + parent.getTagName() + " "
					+ parent.getAttribute("Id"));
		}
		return (Element) list.item(0);
	}

	private static int i(Element e, String attr) {
		return Integer.parseInt(required(e, attr));
	}

	private static double d(Element e, String attr) {
		return Double.parseDouble(required(e, attr));
	}

	private static String required(Element e, String attr) {
		String v = e.getAttribute(attr);
		if (v.isEmpty()) {
			throw new IllegalStateException("atributo " + attr + " ausente em <" + e.getTagName() + ">");
		}
		return v.trim();
	}
}
