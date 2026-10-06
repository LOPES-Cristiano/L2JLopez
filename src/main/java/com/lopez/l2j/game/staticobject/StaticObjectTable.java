package com.lopez.l2j.game.staticobject;

import com.lopez.l2j.game.model.ObjectIdFactory;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Tabela e gerenciador de objetos estaticos do mundo (StaticObjects do L2JDream).
 * Carrega data/xml/world/staticobjects.xml e registra mapas de vilas, placas e estatuas.
 */
@Component
public class StaticObjectTable {

	private static final Logger log = LoggerFactory.getLogger(StaticObjectTable.class);
	private static final String DEFAULT_FILE = "data/xml/world/staticobjects.xml";

	private final Map<Integer, StaticObjectInstance> byStaticId = new ConcurrentHashMap<>();
	private final Map<Integer, StaticObjectInstance> byObjectId = new ConcurrentHashMap<>();
	private final ObjectIdFactory idFactory;
	private final String filePath;

	public StaticObjectTable() {
		this(null, DEFAULT_FILE);
	}

	@Autowired
	public StaticObjectTable(@Autowired(required = false) ObjectIdFactory idFactory) {
		this(idFactory, DEFAULT_FILE);
	}

	public StaticObjectTable(ObjectIdFactory idFactory, String filePath) {
		this.idFactory = idFactory != null ? idFactory : ObjectIdFactory.sequential(0x70000000);
		this.filePath = filePath;
	}

	@PostConstruct
	public void init() {
		load(Path.of(filePath));
	}

	public int size() {
		return byStaticId.size();
	}

	public Collection<StaticObjectInstance> allStaticObjects() {
		return Collections.unmodifiableCollection(byStaticId.values());
	}

	public Optional<StaticObjectInstance> byStaticId(int staticId) {
		return Optional.ofNullable(byStaticId.get(staticId));
	}

	public Optional<StaticObjectInstance> byObjectId(int objectId) {
		return Optional.ofNullable(byObjectId.get(objectId));
	}

	public java.util.List<StaticObjectInstance> findAround(int x, int y, int radius) {
		long r2 = (long) radius * radius;
		java.util.List<StaticObjectInstance> result = new java.util.ArrayList<>();
		for (StaticObjectInstance o : byStaticId.values()) {
			long dx = o.x() - x;
			long dy = o.y() - y;
			if (dx * dx + dy * dy <= r2) {
				result.add(o);
			}
		}
		return result;
	}

	public void load(Path file) {
		byStaticId.clear();
		byObjectId.clear();
		if (!Files.exists(file)) {
			log.warn("StaticObjectTable: arquivo {} nao encontrado.", file);
			return;
		}

		try (InputStream in = Files.newInputStream(file)) {
			var factory = DocumentBuilderFactory.newInstance();
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			var builder = factory.newDocumentBuilder();
			Document doc = builder.parse(in);

			NodeList objects = doc.getElementsByTagName("object");
			for (int i = 0; i < objects.getLength(); i++) {
				Element el = (Element) objects.item(i);
				int staticId = Integer.parseInt(el.getAttribute("id"));
				int type = Integer.parseInt(el.getAttribute("type"));
				String texture = el.getAttribute("texture");

				int x = 0, y = 0, z = 0, mapX = 0, mapY = 0;
				NodeList posList = el.getElementsByTagName("position");
				if (posList.getLength() > 0) {
					Element pos = (Element) posList.item(0);
					x = Integer.parseInt(pos.getAttribute("x"));
					y = Integer.parseInt(pos.getAttribute("y"));
					z = Integer.parseInt(pos.getAttribute("z"));
					if (pos.hasAttribute("map_x")) {
						mapX = Integer.parseInt(pos.getAttribute("map_x"));
					}
					if (pos.hasAttribute("map_y")) {
						mapY = Integer.parseInt(pos.getAttribute("map_y"));
					}
				}

				int objectId = idFactory.nextId();
				var instance = new StaticObjectInstance(objectId, staticId, type, texture, x, y, z, mapX, mapY);
				byStaticId.put(staticId, instance);
				byObjectId.put(objectId, instance);
			}

			log.info("StaticObjectTable: {} objetos estaticos carregados de {}", byStaticId.size(), file);
		} catch (Exception e) {
			log.error("StaticObjectTable: falha ao carregar {}", file, e);
		}
	}
}
