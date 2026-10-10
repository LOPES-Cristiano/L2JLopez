package com.lopez.l2j.game.zone;

import java.awt.Polygon;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Tabela e gerenciador de zonas do mundo (ZoneTable / ZoneManager do L2JDream).
 * Carrega todos os limites de zonas de paz, arena, castelos, agua e chefes de data/xml/zone/*.xml.
 */
@Component
public class ZoneTable {

	private static final Logger log = LoggerFactory.getLogger(ZoneTable.class);

	private final List<Zone> allZones = new CopyOnWriteArrayList<>();
	private final Map<ZoneType, List<Zone>> byType = new ConcurrentHashMap<>();
	private final Map<Integer, Zone> byId = new ConcurrentHashMap<>();

	public ZoneTable() {
		loadFromDirectory("data/xml/zone");
	}

	public ZoneTable(String dirPath) {
		loadFromDirectory(dirPath);
	}

	public ZoneTable(List<Zone> zones) {
		if (zones != null) {
			this.allZones.addAll(zones);
			for (Zone z : zones) {
				byId.put(z.id(), z);
				byType.computeIfAbsent(z.type(), k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(z);
			}
		}
	}

	public int size() {
		return allZones.size();
	}

	public List<Zone> allZones() {
		return Collections.unmodifiableList(allZones);
	}

	public List<Zone> zonesByType(ZoneType type) {
		return byType.getOrDefault(type, List.of());
	}

	public Optional<Zone> zoneById(int id) {
		return Optional.ofNullable(byId.get(id));
	}

	/**
	 * Verifica se as coordenadas informadas estao dentro de uma Zona de Paz.
	 */
	public boolean isInsidePeace(int x, int y, int z) {
		for (Zone zone : allZones) {
			if (zone.type() == ZoneType.TOWN && com.lopez.l2j.config.Config.ZONE_TOWN == 2) {
				continue;
			}
			if ((zone.isPeace() || zone.type() == ZoneType.PEACE || zone.type() == ZoneType.TOWN)
					&& !zone.isArena() && zone.contains(x, y, z)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Alias para isInsidePeace(x, y, z).
	 */
	public boolean isInsidePeaceZone(int x, int y, int z) {
		return isInsidePeace(x, y, z);
	}

	/**
	 * Verifica se as coordenadas informadas estao dentro de uma Zona de Arena (PvP livre sem karma).
	 */
	public boolean isInsideArena(int x, int y, int z) {
		for (Zone zone : allZones) {
			if ((zone.isArena() || zone.type() == ZoneType.ARENA
					|| (zone.type() == ZoneType.TOWN && com.lopez.l2j.config.Config.ZONE_TOWN == 2))
					&& zone.contains(x, y, z)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Verifica se as coordenadas informadas estao dentro de uma Zona de Flag (nome roxo automatico).
	 */
	public boolean isInsideFlagZone(int x, int y, int z) {
		for (Zone zone : allZones) {
			if (zone.type() == ZoneType.FLAG && zone.contains(x, y, z)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Verifica se as coordenadas informadas estao dentro de uma Zona de Combate/PvP livre (Arena, Siege, Flag Zone).
	 */
	public boolean isInsidePvpCombatZone(int x, int y, int z) {
		for (Zone zone : allZones) {
			if ((zone.isArena() || zone.type() == ZoneType.ARENA || zone.type() == ZoneType.SIEGE || zone.type() == ZoneType.FLAG
					|| (zone.type() == ZoneType.TOWN && com.lopez.l2j.config.Config.ZONE_TOWN == 2))
					&& zone.contains(x, y, z)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Verifica se as coordenadas informadas estao dentro de uma Zona de Agua (nado/afogamento).
	 */
	public boolean isInsideWater(int x, int y, int z) {
		List<Zone> waters = byType.get(ZoneType.WATER);
		if (waters == null || waters.isEmpty()) {
			return false;
		}
		for (Zone zone : waters) {
			if (zone.contains(x, y, z)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Verifica se as coordenadas informadas estao dentro de uma Zona de Dano Ambiental (Lava, Pantano Toxico).
	 */
	public boolean isInsideDamage(int x, int y, int z) {
		List<Zone> damages = byType.get(ZoneType.DAMAGE);
		if (damages == null || damages.isEmpty()) {
			return false;
		}
		for (Zone zone : damages) {
			if (zone.contains(x, y, z)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Retorna todas as zonas presentes nas coordenadas especificadas.
	 */
	public List<Zone> getZonesAt(int x, int y, int z) {
		List<Zone> matches = new ArrayList<>();
		for (Zone zone : allZones) {
			if (zone.contains(x, y, z)) {
				matches.add(zone);
			}
		}
		return matches;
	}

	/**
	 * Carrega todos os arquivos .xml da pasta de zonas.
	 */
	private void loadFromDirectory(String dirPath) {
		File dir = new File(dirPath);
		if (!dir.exists() || !dir.isDirectory()) {
			log.warn("Diretorio de zonas nao encontrado: {}", dir.getAbsolutePath());
			return;
		}

		File[] files = dir.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).endsWith(".xml"));
		if (files == null || files.length == 0) {
			log.warn("Nenhum arquivo XML de zonas encontrado em {}", dir.getAbsolutePath());
			return;
		}

		for (File f : files) {
			loadXmlFile(f);
		}

		log.info("ZoneTable carregada: total de {} zonas indexadas de {} arquivos", allZones.size(), files.length);
	}

	public void loadXmlFile(File file) {
		try {
			var factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			Document doc = factory.newDocumentBuilder().parse(file);
			NodeList zoneNodes = doc.getElementsByTagName("zone");

			for (int i = 0; i < zoneNodes.getLength(); i++) {
				Node node = zoneNodes.item(i);
				if (node.getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}
				Element elem = (Element) node;

				int id = Integer.parseInt(elem.getAttribute("id"));
				String name = elem.getAttribute("name");
				String typeStr = elem.getAttribute("type");
				ZoneType type = parseZoneType(typeStr, file.getName());

				boolean isPeace = type == ZoneType.PEACE || type == ZoneType.TOWN;
				boolean isArena = type == ZoneType.ARENA;
				int castleId = 0;
				int townId = 0;

				List<ZoneShape> shapes = new ArrayList<>();

				NodeList children = elem.getChildNodes();
				for (int j = 0; j < children.getLength(); j++) {
					Node child = children.item(j);
					if (child.getNodeType() != Node.ELEMENT_NODE) {
						continue;
					}
					Element childElem = (Element) child;
					String tag = childElem.getTagName().toLowerCase(Locale.ROOT);

					if ("settings".equals(tag)) {
						String pvp = childElem.getAttribute("pvp");
						if ("Peace".equalsIgnoreCase(pvp)) {
							isPeace = true;
						} else if ("Arena".equalsIgnoreCase(pvp)) {
							isArena = true;
						}
					} else if ("entity".equals(tag)) {
						if (childElem.hasAttribute("castleId")) {
							try {
								castleId = Integer.parseInt(childElem.getAttribute("castleId"));
							} catch (NumberFormatException ignored) {}
						}
						if (childElem.hasAttribute("townId")) {
							try {
								townId = Integer.parseInt(childElem.getAttribute("townId"));
							} catch (NumberFormatException ignored) {}
						}
					} else if ("shape".equals(tag)) {
						int zMin = -65535;
						int zMax = 65535;
						if (childElem.hasAttribute("zMin")) {
							zMin = Integer.parseInt(childElem.getAttribute("zMin"));
						}
						if (childElem.hasAttribute("zMax")) {
							zMax = Integer.parseInt(childElem.getAttribute("zMax"));
						}

						String shapeType = childElem.getAttribute("type");
						Polygon poly = new Polygon();
						NodeList points = childElem.getElementsByTagName("point");

						if ("rect".equalsIgnoreCase(shapeType) && points.getLength() == 2) {
							Element pt1 = (Element) points.item(0);
							Element pt2 = (Element) points.item(1);
							int x1 = Integer.parseInt(pt1.getAttribute("x"));
							int y1 = Integer.parseInt(pt1.getAttribute("y"));
							int x2 = Integer.parseInt(pt2.getAttribute("x"));
							int y2 = Integer.parseInt(pt2.getAttribute("y"));
							int minX = Math.min(x1, x2);
							int maxX = Math.max(x1, x2);
							int minY = Math.min(y1, y2);
							int maxY = Math.max(y1, y2);
							poly.addPoint(minX, minY);
							poly.addPoint(maxX, minY);
							poly.addPoint(maxX, maxY);
							poly.addPoint(minX, maxY);
						} else {
							for (int k = 0; k < points.getLength(); k++) {
								Element pt = (Element) points.item(k);
								int px = Integer.parseInt(pt.getAttribute("x"));
								int py = Integer.parseInt(pt.getAttribute("y"));
								poly.addPoint(px, py);
							}
						}

						if (poly.npoints >= 3) {
							shapes.add(new ZoneShape(zMin, zMax, poly));
						}
					}
				}

				if (!shapes.isEmpty()) {
					Zone zone = new Zone(id, name, type, isPeace, isArena, castleId, townId, Collections.unmodifiableList(shapes));
					allZones.add(zone);
					byId.put(id, zone);
					byType.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(zone);
				}
			}
		} catch (Exception ex) {
			log.error("Erro ao carregar zonas do arquivo {}", file.getAbsolutePath(), ex);
		}
	}

	private ZoneType parseZoneType(String typeStr, String fileName) {
		if (typeStr != null && !typeStr.isBlank()) {
			String clean = typeStr.trim().toUpperCase(Locale.ROOT);
			try {
				return ZoneType.valueOf(clean);
			} catch (IllegalArgumentException ignored) {}
		}

		String lowerFile = fileName.toLowerCase(Locale.ROOT);
		if (lowerFile.contains("peace")) return ZoneType.PEACE;
		if (lowerFile.contains("arena")) return ZoneType.ARENA;
		if (lowerFile.contains("water")) return ZoneType.WATER;
		if (lowerFile.contains("castle")) return ZoneType.CASTLE;
		if (lowerFile.contains("siege")) return ZoneType.SIEGE;
		if (lowerFile.contains("clanhall")) return ZoneType.CLANHALL;
		if (lowerFile.contains("boss")) return ZoneType.BOSS;
		if (lowerFile.contains("damage")) return ZoneType.DAMAGE;
		if (lowerFile.contains("flag")) return ZoneType.FLAG;
		return ZoneType.OTHER;
	}
}
