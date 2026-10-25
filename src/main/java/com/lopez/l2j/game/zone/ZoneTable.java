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

	private static final int GRID_SHIFT = 12; // 4096 unidades de mundo por celula de indice espacial

	private final List<Zone> allZones = new CopyOnWriteArrayList<>();
	private final Map<ZoneType, List<Zone>> byType = new ConcurrentHashMap<>();
	private final Map<Integer, Zone> byId = new ConcurrentHashMap<>();
	private final Map<Long, List<Zone>> spatialGrid = new ConcurrentHashMap<>();
	private volatile int indexedZoneCount = -1;

	private final Map<Integer, java.util.Set<Integer>> bossZoneAllowedPlayers = new ConcurrentHashMap<>();
	private final Map<Integer, java.util.Set<Integer>> playerActiveZones = new ConcurrentHashMap<>();
	private final Map<Integer, java.util.concurrent.ScheduledFuture<?>> noLandingTasks = new ConcurrentHashMap<>();

	private final java.util.concurrent.ScheduledExecutorService zoneValidatorScheduler =
			java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
				Thread t = Thread.ofVirtual().name("ZoneValidationTask").unstarted(r);
				return t;
			});

	public ZoneTable() {
		loadFromDirectory("data/xml/zone");
		startZoneValidationTask();
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
		rebuildSpatialIndex();
	}

	private static long gridKey(int gx, int gy) {
		return (((long) gx) << 32) | (gy & 0xFFFFFFFFL);
	}

	private void ensureSpatialIndex() {
		if (indexedZoneCount != allZones.size()) {
			rebuildSpatialIndex();
		}
	}

	public synchronized void rebuildSpatialIndex() {
		spatialGrid.clear();
		for (Zone z : allZones) {
			byId.putIfAbsent(z.id(), z);
			List<Zone> typeList = byType.computeIfAbsent(z.type(), k -> new CopyOnWriteArrayList<>());
			if (!typeList.contains(z)) {
				typeList.add(z);
			}
			indexZoneInGrid(z);
		}
		indexedZoneCount = allZones.size();
	}

	private void indexZoneInGrid(Zone z) {
		if (z == null || z.shapes() == null) {
			return;
		}
		for (ZoneShape shape : z.shapes()) {
			if (shape.polygon() == null || shape.polygon().npoints == 0) {
				continue;
			}
			var bounds = shape.polygon().getBounds();
			int minGx = bounds.x >> GRID_SHIFT;
			int maxGx = (bounds.x + bounds.width) >> GRID_SHIFT;
			int minGy = bounds.y >> GRID_SHIFT;
			int maxGy = (bounds.y + bounds.height) >> GRID_SHIFT;
			for (int gx = minGx; gx <= maxGx; gx++) {
				for (int gy = minGy; gy <= maxGy; gy++) {
					List<Zone> bucket = spatialGrid.computeIfAbsent(gridKey(gx, gy), k -> new CopyOnWriteArrayList<>());
					if (!bucket.contains(z)) {
						bucket.add(z);
					}
				}
			}
		}
	}

	private List<Zone> candidateZonesAt(int x, int y) {
		ensureSpatialIndex();
		List<Zone> bucket = spatialGrid.get(gridKey(x >> GRID_SHIFT, y >> GRID_SHIFT));
		return bucket != null ? bucket : List.of();
	}

	private void startZoneValidationTask() {
		zoneValidatorScheduler.scheduleAtFixedRate(() -> {
			try {
				var gw = com.lopez.l2j.game.world.GameWorld.getInstance();
				if (gw == null) {
					return;
				}
				for (var op : gw.players()) {
					if (op != null && op.character() != null) {
						revalidatePlayerZones(op.character(), gw);
					}
				}
			} catch (Exception ignored) {
			}
		}, 2000L, 1000L, java.util.concurrent.TimeUnit.MILLISECONDS);
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

	private static boolean isJailZone(Zone zone) {
		return zone != null && zone.name() != null
				&& zone.name().toLowerCase(Locale.ROOT).contains("jail");
	}

	@org.springframework.beans.factory.annotation.Autowired(required = false)
	@org.springframework.context.annotation.Lazy
	private com.lopez.l2j.game.war.TownWarService townWarService;

	public void setTownWarService(com.lopez.l2j.game.war.TownWarService townWarService) {
		this.townWarService = townWarService;
	}

	/**
	 * Verifica se as coordenadas informadas estao dentro de uma Zona de Paz.
	 */
	public boolean isInsidePeace(int x, int y, int z) {
		if (townWarService != null && townWarService.isTownWarActiveAt(x, y)) {
			return false;
		}
		for (Zone zone : candidateZonesAt(x, y)) {
			if (zone.type() == ZoneType.TOWN && com.lopez.l2j.config.Config.ZONE_TOWN == 2) {
				continue;
			}
			if (isJailZone(zone) && com.lopez.l2j.config.Config.JAIL_IS_PVP_ZONE) {
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
		for (Zone zone : candidateZonesAt(x, y)) {
			if ((zone.isArena() || zone.type() == ZoneType.ARENA
					|| (zone.type() == ZoneType.TOWN && com.lopez.l2j.config.Config.ZONE_TOWN == 2)
					|| (isJailZone(zone) && com.lopez.l2j.config.Config.JAIL_IS_PVP_ZONE))
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
		for (Zone zone : candidateZonesAt(x, y)) {
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
		for (Zone zone : candidateZonesAt(x, y)) {
			if ((zone.isArena() || zone.type() == ZoneType.ARENA || zone.type() == ZoneType.SIEGE || zone.type() == ZoneType.FLAG
					|| (zone.type() == ZoneType.TOWN && com.lopez.l2j.config.Config.ZONE_TOWN == 2)
					|| (isJailZone(zone) && com.lopez.l2j.config.Config.JAIL_IS_PVP_ZONE))
					&& zone.contains(x, y, z)) {
				return true;
			}
		}
		return false;
	}

	public boolean isInsidePvp(int x, int y, int z) {
		return isInsidePvpCombatZone(x, y, z);
	}

	/**
	 * Verifica se as coordenadas informadas estao dentro de uma Zona de Agua (nado/afogamento).
	 */
	public boolean isInsideWater(int x, int y, int z) {
		if (!com.lopez.l2j.config.Config.ALLOW_WATER) {
			return false;
		}
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

	public boolean isInsideNoLanding(int x, int y, int z) {
		for (Zone zone : candidateZonesAt(x, y)) {
			if ((zone.type() == ZoneType.NOLANDING || zone.type() == ZoneType.SIEGE) && zone.contains(x, y, z)) {
				return true;
			}
		}
		return false;
	}

	public boolean isInsideSiege(int x, int y, int z) {
		for (Zone zone : candidateZonesAt(x, y)) {
			if (zone.type() == ZoneType.SIEGE && zone.contains(x, y, z)) {
				return true;
			}
		}
		return false;
	}

	public boolean isInsideMotherTree(int x, int y, int z) {
		for (Zone zone : candidateZonesAt(x, y)) {
			if (zone.type() == ZoneType.MOTHERTREE && zone.contains(x, y, z)) {
				return true;
			}
		}
		return false;
	}

	public boolean isInsideSwamp(int x, int y, int z) {
		for (Zone zone : candidateZonesAt(x, y)) {
			if (zone.type() == ZoneType.SWAMP && zone.contains(x, y, z)) {
				return true;
			}
		}
		return false;
	}

	public double getSwampMoveMultiplier(int x, int y, int z) {
		for (Zone zone : candidateZonesAt(x, y)) {
			if (zone.type() == ZoneType.SWAMP && zone.contains(x, y, z)) {
				return Math.max(0.1, 1.0 + zone.moveBonus());
			}
		}
		return 1.0;
	}

	public boolean isInsideMonsterTrack(int x, int y, int z) {
		for (Zone zone : candidateZonesAt(x, y)) {
			if (zone.type() == ZoneType.MONSTERTRACK && zone.contains(x, y, z)) {
				return true;
			}
		}
		return false;
	}

	public boolean isInsideNoStore(int x, int y, int z) {
		for (Zone zone : candidateZonesAt(x, y)) {
			if (zone.type() == ZoneType.NOSTORE && zone.contains(x, y, z)) {
				return true;
			}
		}
		return false;
	}

	public boolean isInsideNoRestart(int x, int y, int z) {
		for (Zone zone : candidateZonesAt(x, y)) {
			if (zone.type() == ZoneType.NORESTART && zone.contains(x, y, z)) {
				return true;
			}
		}
		return false;
	}

	public boolean isFishingAllowed(int x, int y, int z) {
		return com.lopez.l2j.config.Config.ALLOW_FISHING;
	}

	public boolean isBoatAllowed() {
		return com.lopez.l2j.config.Config.ALLOW_BOAT;
	}

	public int getNoRestartKickTimeSeconds() {
		return com.lopez.l2j.config.Config.NO_RESTART_KICK_TIME;
	}

	// ---- Controle de Allow-List e Expulsao de L2BossZone ----

	public void allowPlayerEntry(int zoneId, int playerObjectId) {
		bossZoneAllowedPlayers.computeIfAbsent(zoneId, k -> ConcurrentHashMap.newKeySet()).add(playerObjectId);
	}

	public boolean isPlayerAllowedInBossZone(int zoneId, int playerObjectId) {
		java.util.Set<Integer> allowed = bossZoneAllowedPlayers.get(zoneId);
		return allowed == null || allowed.isEmpty() || allowed.contains(playerObjectId);
	}

	public void clearBossZoneAllowedPlayers(int zoneId) {
		bossZoneAllowedPlayers.remove(zoneId);
	}

	public void checkBossZoneEviction(com.lopez.l2j.game.model.PlayerCharacter player, com.lopez.l2j.game.world.GameWorld world) {
		if (player == null || player.accessLevel() > 0) {
			return;
		}
		for (Zone zone : candidateZonesAt(player.x(), player.y())) {
			if (zone.type() == ZoneType.BOSS && zone.contains(player.x(), player.y(), player.z())) {
				if (!isPlayerAllowedInBossZone(zone.id(), player.objectId())) {
					if (world != null) {
						world.teleportPlayer(player, 82698, 148638, -3473, 0);
					} else {
						player.teleToLocation(82698, 148638, -3473);
					}
					player.sendMessage("Voce foi expulso da zona restrita de Grand Boss.");
					return;
				}
			}
		}
	}

	/**
	 * Revalida as zonas ativas do jogador, disparando callbacks de onEnter / onExit / tick ambiental.
	 */
	public void revalidatePlayerZones(com.lopez.l2j.game.model.PlayerCharacter player, com.lopez.l2j.game.world.GameWorld world) {
		if (player == null || player.isDead()) {
			return;
		}
		int pid = player.objectId();
		List<Zone> currentZones = getZonesAt(player.x(), player.y(), player.z());
		java.util.Set<Integer> prevZoneIds = playerActiveZones.computeIfAbsent(pid, k -> ConcurrentHashMap.newKeySet());
		java.util.Set<Integer> curZoneIds = new java.util.HashSet<>(currentZones.size());

		boolean inNoLanding = false;
		for (Zone z : currentZones) {
			curZoneIds.add(z.id());
			boolean justEntered = prevZoneIds.add(z.id());

			if (z.type() == ZoneType.MOTHERTREE && justEntered && player.race() == 1) {
				if (world != null) {
					world.sendPacket(pid, com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage.id(1287));
				}
			}
			if (z.type() == ZoneType.NOLANDING || z.type() == ZoneType.SIEGE) {
				inNoLanding = true;
			}
			if (z.type() == ZoneType.DAMAGE && z.damagePerSec() > 0 && player.currentHp() > 1) {
				int nextHp = Math.max(1, (int) player.currentHp() - z.damagePerSec());
				player.currentHp(nextHp);
			}
			if (z.type() == ZoneType.BOSS && !isPlayerAllowedInBossZone(z.id(), pid) && player.accessLevel() <= 0) {
				checkBossZoneEviction(player, world);
				return;
			}
		}

		var it = prevZoneIds.iterator();
		while (it.hasNext()) {
			int oldZoneId = it.next();
			if (!curZoneIds.contains(oldZoneId)) {
				it.remove();
				Zone oldZone = byId.get(oldZoneId);
				if (oldZone != null && oldZone.type() == ZoneType.MOTHERTREE && player.race() == 1 && world != null) {
					world.sendPacket(pid, com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage.id(1288));
				}
			}
		}

		// Zona NoLanding: se montado em Wyvern (mountType == 2), agenda desmontagem em 5s
		if (inNoLanding && player.mountType() == 2) {
			noLandingTasks.computeIfAbsent(pid, k -> {
				player.sendMessage("Esta area nao permite pouso de Wyvern. Voce sera desmontado em 5 segundos!");
				return zoneValidatorScheduler.schedule(() -> {
					noLandingTasks.remove(pid);
					if (isInsideNoLanding(player.x(), player.y(), player.z()) && player.mountType() == 2) {
						player.dismount();
						player.sendMessage("Seu Wyvern foi desmontado devido a restricao da zona.");
					}
				}, 5L, java.util.concurrent.TimeUnit.SECONDS);
			});
		} else {
			var task = noLandingTasks.remove(pid);
			if (task != null) {
				task.cancel(false);
			}
		}
	}

	/**
	 * Verifica se atacante e Grand Boss estao na mesma Boss Zone (respeitando CAN_ATTACK_FROM_ANOTHER_ZONE_TO_EPIC).
	 */
	public boolean isInSameBossZone(int x1, int y1, int z1, int x2, int y2, int z2) {
		if (com.lopez.l2j.config.Config.CAN_ATTACK_FROM_ANOTHER_ZONE_TO_EPIC) {
			return true;
		}
		List<Zone> bossZones = byType.get(ZoneType.BOSS);
		if (bossZones == null || bossZones.isEmpty()) {
			return true;
		}
		for (Zone bz : bossZones) {
			boolean c1 = bz.contains(x1, y1, z1);
			boolean c2 = bz.contains(x2, y2, z2);
			if (c1 || c2) {
				return c1 && c2;
			}
		}
		return true;
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
	 * Retorna todas as zonas presentes nas coordenadas especificadas (usando indice espacial O(1)).
	 */
	public List<Zone> getZonesAt(int x, int y, int z) {
		List<Zone> matches = new ArrayList<>();
		for (Zone zone : candidateZonesAt(x, y)) {
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
		rebuildSpatialIndex();

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
				boolean isArena = type == ZoneType.ARENA || type == ZoneType.MONSTERTRACK;
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
						NodeList points = childElem.getElementsByTagName("point");

						if ("cylinder".equalsIgnoreCase(shapeType) || childElem.hasAttribute("rad")) {
							int rad = 0;
							if (childElem.hasAttribute("rad")) {
								try {
									rad = Integer.parseInt(childElem.getAttribute("rad"));
								} catch (NumberFormatException ignored) {}
							}
							if (points.getLength() >= 1) {
								Element pt = (Element) points.item(0);
								int cx = Integer.parseInt(pt.getAttribute("x"));
								int cy = Integer.parseInt(pt.getAttribute("y"));
								if (rad <= 0 && pt.hasAttribute("rad")) {
									try {
										rad = Integer.parseInt(pt.getAttribute("rad"));
									} catch (NumberFormatException ignored) {}
								}
								if (rad > 0) {
									shapes.add(ZoneShape.cylinder(zMin, zMax, cx, cy, rad));
								}
							}
							continue;
						}

						Polygon poly = new Polygon();
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
					indexZoneInGrid(zone);
				}
			}
			indexedZoneCount = allZones.size();
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
		if (lowerFile.contains("mothertree")) return ZoneType.MOTHERTREE;
		if (lowerFile.contains("swamp")) return ZoneType.SWAMP;
		if (lowerFile.contains("nolanding")) return ZoneType.NOLANDING;
		if (lowerFile.contains("monstertrack")) return ZoneType.MONSTERTRACK;
		if (lowerFile.contains("effect")) return ZoneType.EFFECT;
		if (lowerFile.contains("nostore")) return ZoneType.NOSTORE;
		if (lowerFile.contains("norestart")) return ZoneType.NORESTART;
		if (lowerFile.contains("jail")) return ZoneType.JAIL;
		return ZoneType.OTHER;
	}
}
