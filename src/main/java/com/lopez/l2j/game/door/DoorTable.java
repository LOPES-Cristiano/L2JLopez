package com.lopez.l2j.game.door;

import com.lopez.l2j.game.model.ObjectIdFactory;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Tabela de portas do mundo (door.xml). Carrega portas de castelos, clanhalls, fortes e masmorras.
 */
@Component
public class DoorTable {

	private static final Logger log = LoggerFactory.getLogger(DoorTable.class);
	private static volatile DoorTable instance;

	public static DoorTable getInstance() {
		return instance;
	}

	private final Map<Integer, DoorInstance> byDoorId = new ConcurrentHashMap<>();
	private final Map<Integer, DoorInstance> byObjectId = new ConcurrentHashMap<>();
	private final ObjectIdFactory objectIdFactory;
	private final java.util.concurrent.ScheduledExecutorService autoDoorScheduler =
			java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
				Thread t = Thread.ofVirtual().name("DoorAutoActionScheduler").unstarted(r);
				return t;
			});

	@Autowired
	public DoorTable(@Value("${l2.datapack.xml-dir:data/xml}") String xmlDir, ObjectIdFactory idFactory) {
		this(Path.of(xmlDir, "world", "door.xml"), idFactory);
	}

	public DoorTable(Path xmlFile, ObjectIdFactory idFactory) {
		instance = this;
		this.objectIdFactory = idFactory != null ? idFactory : ObjectIdFactory.sequential(0x50000000);
		if (Files.isRegularFile(xmlFile)) {
			load(xmlFile);
		} else {
			log.warn("Arquivo door.xml nao encontrado em {}", xmlFile.toAbsolutePath());
		}
	}

	public DoorTable() {
		this(Path.of("data/xml/world/door.xml"), ObjectIdFactory.sequential(0x50000000));
	}

	public void register(DoorInstance door) {
		if (door != null) {
			byDoorId.put(door.doorId(), door);
			byObjectId.put(door.objectId(), door);
			com.lopez.l2j.game.geodata.GeoEngine geo = com.lopez.l2j.game.geodata.GeoEngine.peekInstance();
			if (geo != null) {
				geo.addGeoObject(door);
			}
			if (door.autoActionDelayMs() > 0) {
				door.scheduleAutoAction(autoDoorScheduler);
			}
		}
	}

	public int size() {
		return byDoorId.size();
	}

	public Collection<DoorInstance> allDoors() {
		return Collections.unmodifiableCollection(byDoorId.values());
	}

	public Collection<DoorInstance> getDoors() {
		return allDoors();
	}

	public Optional<DoorInstance> byDoorId(int doorId) {
		return Optional.ofNullable(byDoorId.get(doorId));
	}

	public DoorInstance getDoor(int doorId) {
		return byDoorId.get(doorId);
	}

	public DoorInstance door(int id) {
		DoorInstance d = byDoorId.get(id);
		return d != null ? d : byObjectId.get(id);
	}

	public java.util.List<DoorInstance> findDoorsAround(int x, int y, int radius) {
		long r2 = (long) radius * radius;
		java.util.List<DoorInstance> result = new java.util.ArrayList<>();
		for (DoorInstance d : byDoorId.values()) {
			long dx = d.x() - x;
			long dy = d.y() - y;
			if (dx * dx + dy * dy <= r2) {
				result.add(d);
			}
		}
		return result;
	}

	public Optional<DoorInstance> byObjectId(int objectId) {
		return Optional.ofNullable(byObjectId.get(objectId));
	}

	public boolean openDoor(int doorId) {
		DoorInstance d = byDoorId.get(doorId);
		return d != null && d.openDoor();
	}

	public boolean closeDoor(int doorId) {
		DoorInstance d = byDoorId.get(doorId);
		return d != null && d.closeDoor();
	}

	public boolean toggleDoor(int doorId) {
		DoorInstance d = byDoorId.get(doorId);
		return d != null && d.toggleDoor();
	}

	private void load(Path file) {
		try (InputStream in = Files.newInputStream(file)) {
			var factory = DocumentBuilderFactory.newInstance();
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			var doc = factory.newDocumentBuilder().parse(in);

			NodeList doorNodes = doc.getElementsByTagName("door");
			for (int i = 0; i < doorNodes.getLength(); i++) {
				Element e = (Element) doorNodes.item(i);
				int doorId = Integer.parseInt(e.getAttribute("id"));
				String name = e.getAttribute("name");
				int castleId = e.hasAttribute("castleId") ? Integer.parseInt(e.getAttribute("castleId")) : 0;
				int clanHallId = e.hasAttribute("clanHallId") ? Integer.parseInt(e.getAttribute("clanHallId")) : 0;
				int fortId = e.hasAttribute("fortId") ? Integer.parseInt(e.getAttribute("fortId")) : 0;
				long autoDelayMs = e.hasAttribute("autoActionDelay") ? Long.parseLong(e.getAttribute("autoActionDelay")) : 0L;

				int x = 0, y = 0, z = 0;
				int xMin = 0, xMax = 0, yMin = 0, yMax = 0, zMin = 0, zMax = 0;
				int hp = 100_000, pdef = 500, mdef = 500;
				boolean unlockable = false;
				boolean isOpen = false;

				NodeList children = e.getChildNodes();
				for (int j = 0; j < children.getLength(); j++) {
					if (children.item(j) instanceof Element child) {
						String tag = child.getTagName().toLowerCase(java.util.Locale.ROOT);
						if ("position".equals(tag)) {
							x = Integer.parseInt(child.getAttribute("x"));
							y = Integer.parseInt(child.getAttribute("y"));
							z = Integer.parseInt(child.getAttribute("z"));
						} else if ("range".equals(tag)) {
							xMin = Integer.parseInt(child.getAttribute("XMin"));
							xMax = Integer.parseInt(child.getAttribute("XMax"));
							yMin = Integer.parseInt(child.getAttribute("YMin"));
							yMax = Integer.parseInt(child.getAttribute("YMax"));
							zMin = Integer.parseInt(child.getAttribute("ZMin"));
							zMax = Integer.parseInt(child.getAttribute("ZMax"));
						} else if ("stat".equals(tag)) {
							hp = Integer.parseInt(child.getAttribute("hp"));
							pdef = Integer.parseInt(child.getAttribute("pdef"));
							mdef = Integer.parseInt(child.getAttribute("mdef"));
							unlockable = Boolean.parseBoolean(child.getAttribute("unlockable"));
							isOpen = Boolean.parseBoolean(child.getAttribute("isOpen"));
							if (child.hasAttribute("autoActionDelay")) {
								autoDelayMs = Long.parseLong(child.getAttribute("autoActionDelay"));
							} else if (child.hasAttribute("closeTime")) {
								autoDelayMs = Long.parseLong(child.getAttribute("closeTime")) * 1000L;
							}
						}
					}
				}

				int objId = objectIdFactory.nextId();
				DoorInstance door = new DoorInstance(objId, doorId, name, x, y, z,
						xMin, xMax, yMin, yMax, zMin, zMax, hp, pdef, mdef, unlockable, isOpen);
				door.castleId(castleId);
				door.clanHallId(clanHallId);
				door.fortId(fortId);
				door.autoActionDelayMs(autoDelayMs);
				register(door);
			}

			log.info("DoorTable: {} portas carregadas de {}", byDoorId.size(), file.getFileName());
		} catch (Exception ex) {
			log.error("Erro ao carregar portas de {}: {}", file, ex.getMessage(), ex);
		}
	}

	/**
	 * Carrega portas de castelos a partir da tabela castle_door (V19 / V1019).
	 */
	public void loadFromDb(org.springframework.jdbc.core.simple.JdbcClient jdbc) {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("SELECT castleId, id, name, x, y, z, range_xmin, range_ymin, range_zmin, range_xmax, range_ymax, range_zmax, hp, pDef, mDef FROM castle_door")
					.query((rs, i) -> {
						int castleId = rs.getInt("castleId");
						int doorId = rs.getInt("id");
						String name = rs.getString("name");
						int x = rs.getInt("x");
						int y = rs.getInt("y");
						int z = rs.getInt("z");
						int xMin = rs.getInt("range_xmin");
						int yMin = rs.getInt("range_ymin");
						int zMin = rs.getInt("range_zmin");
						int xMax = rs.getInt("range_xmax");
						int yMax = rs.getInt("range_ymax");
						int zMax = rs.getInt("range_zmax");
						int hp = rs.getInt("hp");
						int pDef = rs.getInt("pDef");
						int mDef = rs.getInt("mDef");

						int objId = objectIdFactory != null ? objectIdFactory.nextId() : doorId;
						DoorInstance door = new DoorInstance(objId, doorId, name, x, y, z,
								xMin, xMax, yMin, yMax, zMin, zMax, hp, pDef, mDef, false, false);
						door.castleId(castleId);
						register(door);
						return null;
					}).list();
			log.info("DoorTable: {} portas de castelos registradas de castle_door", byDoorId.size());
		} catch (Exception e) {
			log.debug("DoorTable: castle_door indisponivel: {}", e.getMessage());
		}
	}
}
