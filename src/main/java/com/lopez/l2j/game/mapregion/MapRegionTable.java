package com.lopez.l2j.game.mapregion;

import java.awt.Polygon;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
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
 * Tabela de regioes do mapa e pontos de renascimento (mapregion.xml).
 * Determina para qual vila um jogador vai ao morrer (RequestRestartPoint) ou ao usar Scroll of Escape.
 */
@Component
public class MapRegionTable {

	private static final Logger log = LoggerFactory.getLogger(MapRegionTable.class);

	public record Point3D(int x, int y, int z) {
	}

	public record RestartPoint(int id, String name, int bbs, int locName,
			List<Point3D> points, List<Point3D> chaosPoints) {
		public Point3D getRandomPoint(boolean chaotic) {
			if (chaotic && !chaosPoints.isEmpty()) {
				int idx = ThreadLocalRandom.current().nextInt(chaosPoints.size());
				return chaosPoints.get(idx);
			}
			if (!points.isEmpty()) {
				int idx = ThreadLocalRandom.current().nextInt(points.size());
				return points.get(idx);
			}
			return new Point3D(0, 0, 0);
		}
	}

	public record RegionArea(int id, int zMin, int zMax, Polygon polygon, Map<Integer, Integer> restartByRace) {
		public boolean contains(int x, int y, int z) {
			if (z < zMin || z > zMax) {
				return false;
			}
			return polygon.contains(x, y);
		}
	}

	private final Map<Integer, RestartPoint> restartPoints = new HashMap<>();
	private final List<RegionArea> regions = new ArrayList<>();

	@Autowired
	public MapRegionTable(@Value("${l2.datapack.xml-dir:data/xml}") String xmlDir) {
		this(Path.of(xmlDir, "world", "mapregion", "mapregion.xml"));
	}

	public MapRegionTable(Path xmlFile) {
		if (Files.isRegularFile(xmlFile)) {
			load(xmlFile);
		} else {
			log.warn("Arquivo mapregion nao encontrado em {}", xmlFile.toAbsolutePath());
		}
	}

	public int restartPointsCount() {
		return restartPoints.size();
	}

	public int regionsCount() {
		return regions.size();
	}

	public RestartPoint getRestartPoint(int id) {
		return restartPoints.get(id);
	}

	/**
	 * Localiza as coordenadas de renascimento de acordo com a posicao atual, raca e status caotico (PK).
	 */
	public int[] getRestartCoordinates(int x, int y, int z, int raceId) {
		Point3D p = getRestartPoint3D(x, y, z, raceId, false);
		return new int[] { p.x(), p.y(), p.z() };
	}

	public int[] getRestartCoordinates(int x, int y, int z, int raceId, boolean chaotic) {
		Point3D p = getRestartPoint3D(x, y, z, raceId, chaotic);
		return new int[] { p.x(), p.y(), p.z() };
	}

	public static boolean isTownRestart(RestartPoint rp) {
		if (rp == null || rp.name() == null) {
			return false;
		}
		String name = rp.name().toLowerCase(Locale.ROOT);
		// Ignora arenas, pistas, salas de GM e catacumbas
		if (name.contains("monster_race") || name.contains("colosseum") || name.contains("gm_room")
				|| name.contains("catacomb") || name.contains("oracle") || name.contains("quarry")
				|| name.contains("hall") || name.contains("school") || name.contains("temple")
				|| name.contains("glade") || name.contains("rift") || name.contains("dmz")
				|| name.contains("habor")) {
			return false;
		}
		return name.contains("town") || name.contains("village") || name.equals("primeval_isle");
	}

	public Point3D getRestartPoint3D(int x, int y, int z, int raceId, boolean chaotic) {
		for (RegionArea area : regions) {
			if (area.contains(x, y, z)) {
				Integer rId = area.restartByRace().get(raceId);
				if (rId == null) {
					rId = area.restartByRace().get(0); // fallback Human
				}
				if (rId != null) {
					RestartPoint rp = restartPoints.get(rId);
					if (rp != null) {
						return rp.getRandomPoint(chaotic);
					}
				}
			}
		}

		// Fallback 1: match horizontal por poligono ignorando z (se jogador estiver acima/abaixo dos limites de altura da regiao)
		for (RegionArea area : regions) {
			if (area.polygon().contains(x, y)) {
				Integer rId = area.restartByRace().get(raceId);
				if (rId == null) {
					rId = area.restartByRace().get(0);
				}
				if (rId != null) {
					RestartPoint rp = restartPoints.get(rId);
					if (rp != null && isTownRestart(rp)) {
						return rp.getRandomPoint(chaotic);
					}
				}
			}
		}

		// Fallback 2: vila/cidade real mais proxima em distancia euclidiana
		RestartPoint nearest = null;
		long minSq = Long.MAX_VALUE;
		for (RestartPoint rp : restartPoints.values()) {
			if (rp.points().isEmpty() || !isTownRestart(rp)) {
				continue;
			}
			Point3D p = rp.points().get(0);
			long dx = (long) x - p.x();
			long dy = (long) y - p.y();
			long sq = dx * dx + dy * dy;
			if (sq < minSq) {
				minSq = sq;
				nearest = rp;
			}
		}

		if (nearest != null) {
			return nearest.getRandomPoint(chaotic);
		}
		// Ultimo caso: Giran
		return new Point3D(83400, 147943, -3404);
	}

	private void load(Path file) {
		try (InputStream in = Files.newInputStream(file)) {
			var factory = DocumentBuilderFactory.newInstance();
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			var doc = factory.newDocumentBuilder().parse(in);

			// 1. Restart Points
			NodeList rpNodes = doc.getElementsByTagName("restartpoint");
			for (int i = 0; i < rpNodes.getLength(); i++) {
				Element e = (Element) rpNodes.item(i);
				int id = Integer.parseInt(e.getAttribute("id"));
				String name = e.getAttribute("name");
				int bbs = e.hasAttribute("bbs") ? Integer.parseInt(e.getAttribute("bbs")) : 0;
				int locname = e.hasAttribute("locname") ? Integer.parseInt(e.getAttribute("locname")) : 0;

				List<Point3D> points = new ArrayList<>();
				List<Point3D> chaosPoints = new ArrayList<>();

				NodeList children = e.getChildNodes();
				for (int j = 0; j < children.getLength(); j++) {
					if (children.item(j) instanceof Element child) {
						if ("point".equalsIgnoreCase(child.getTagName())) {
							points.add(new Point3D(
									Integer.parseInt(child.getAttribute("X")),
									Integer.parseInt(child.getAttribute("Y")),
									Integer.parseInt(child.getAttribute("Z"))));
						} else if ("chaospoint".equalsIgnoreCase(child.getTagName())) {
							chaosPoints.add(new Point3D(
									Integer.parseInt(child.getAttribute("X")),
									Integer.parseInt(child.getAttribute("Y")),
									Integer.parseInt(child.getAttribute("Z"))));
						}
					}
				}
				restartPoints.put(id, new RestartPoint(id, name, bbs, locname, points, chaosPoints));
			}

			// 2. Regions
			NodeList regNodes = doc.getElementsByTagName("region");
			for (int i = 0; i < regNodes.getLength(); i++) {
				Element e = (Element) regNodes.item(i);
				int id = Integer.parseInt(e.getAttribute("id"));
				int zMin = -999999;
				int zMax = 999999;
				Polygon polygon = new Polygon();
				Map<Integer, Integer> restartByRace = new HashMap<>();

				NodeList children = e.getChildNodes();
				for (int j = 0; j < children.getLength(); j++) {
					if (children.item(j) instanceof Element child) {
						if ("zheight".equalsIgnoreCase(child.getTagName())) {
							zMin = Integer.parseInt(child.getAttribute("min"));
							zMax = Integer.parseInt(child.getAttribute("max"));
						} else if ("point".equalsIgnoreCase(child.getTagName())) {
							polygon.addPoint(
									Integer.parseInt(child.getAttribute("X")),
									Integer.parseInt(child.getAttribute("Y")));
						} else if ("restart".equalsIgnoreCase(child.getTagName())) {
							String raceStr = child.getAttribute("race").toLowerCase(Locale.ROOT);
							int rId = parseRace(raceStr);
							int restartId = Integer.parseInt(child.getAttribute("restartId"));
							restartByRace.put(rId, restartId);
						}
					}
				}
				regions.add(new RegionArea(id, zMin, zMax, polygon, restartByRace));
			}

			log.info("MapRegionTable: {} restart points e {} regioes carregadas", restartPoints.size(), regions.size());
		} catch (Exception ex) {
			log.error("Erro ao carregar mapregion de {}: {}", file, ex.getMessage(), ex);
		}
	}

	private static int parseRace(String r) {
		return switch (r) {
			case "human" -> 0;
			case "elf" -> 1;
			case "darkelf", "dark elf" -> 2;
			case "orc" -> 3;
			case "dwarf" -> 4;
			default -> 0;
		};
	}
}
