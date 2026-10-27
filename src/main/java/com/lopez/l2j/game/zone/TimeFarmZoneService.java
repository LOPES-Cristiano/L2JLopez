package com.lopez.l2j.game.zone;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.CharacterVariablesService;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket.InventoryUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.ItemList;
import com.lopez.l2j.network.game.packet.GameServerPacket.TeleportToLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servico de Zonas de Farm Temporizadas com cobranca em Adena (NEXORA IL Time Farm Zones) - Onda F7.
 */
@Service
public class TimeFarmZoneService {

	private static final Logger log = LoggerFactory.getLogger(TimeFarmZoneService.class);

	public record TimeFarmZone(int id, String name, int intervalMinutes, long costAdena,
							   int enterX, int enterY, int enterZ, int exitX, int exitY, int exitZ) {}

	public record ZoneSession(int zoneId, long expireTimeMillis) {}

	private final Map<Integer, TimeFarmZone> zones = new ConcurrentHashMap<>();
	private final Map<Integer, ZoneSession> activeSessions = new ConcurrentHashMap<>();
	@Autowired(required = false)
	private InventoryService inventoryService;
	@Autowired(required = false)
	private CharacterVariablesService variablesService;
	private final java.util.concurrent.ScheduledExecutorService expirationScheduler =
			java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
				Thread t = Thread.ofVirtual().name("TimeFarmZoneExpirationTask").unstarted(r);
				return t;
			});

	public TimeFarmZoneService() {
		this(null, null);
	}

	@Autowired
	public TimeFarmZoneService(
			@Autowired(required = false) InventoryService inventoryService,
			@Autowired(required = false) CharacterVariablesService variablesService) {
		this.inventoryService = inventoryService;
		this.variablesService = variablesService;
		loadZones();
		expirationScheduler.scheduleAtFixedRate(() -> {
			try {
				checkExpiredSessions();
			} catch (Exception ignored) {
			}
		}, 5000L, 5000L, java.util.concurrent.TimeUnit.MILLISECONDS);
	}

	public synchronized void loadZones() {
		zones.clear();
		try {
			File file = new File("data/xml/custom/TimeFarmZones.xml");
			InputStream is = file.exists() ? new java.io.FileInputStream(file) : getClass().getResourceAsStream("/data/xml/custom/TimeFarmZones.xml");
			if (is == null) {
				log.warn("TimeFarmZones.xml nao encontrado, configurando zonas padrao.");
				zones.put(1, new TimeFarmZone(1, "Forbidden Cemetery", 60, 500000L, 17144, 20847, -3400, 83400, 147943, -3404));
				zones.put(2, new TimeFarmZone(2, "Ivory Tower", 30, 250000L, 85337, 16167, -3696, 83400, 147943, -3404));
				return;
			}

			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
			NodeList list = doc.getElementsByTagName("zone");

			for (int i = 0; i < list.getLength(); i++) {
				Element elem = (Element) list.item(i);
				int id = Integer.parseInt(elem.getAttribute("id"));
				String name = elem.getAttribute("name");
				int minutes = Integer.parseInt(elem.getAttribute("intervalMinutes"));
				long cost = elem.hasAttribute("costAdena") ? Long.parseLong(elem.getAttribute("costAdena")) : 0L;
				int enterX = Integer.parseInt(elem.getAttribute("enterX"));
				int enterY = Integer.parseInt(elem.getAttribute("enterY"));
				int enterZ = Integer.parseInt(elem.getAttribute("enterZ"));
				int exitX = Integer.parseInt(elem.getAttribute("exitX"));
				int exitY = Integer.parseInt(elem.getAttribute("exitY"));
				int exitZ = Integer.parseInt(elem.getAttribute("exitZ"));

				zones.put(id, new TimeFarmZone(id, name, minutes, cost, enterX, enterY, enterZ, exitX, exitY, exitZ));
			}

			log.info("TimeFarmZoneService: {} zonas de farm temporal carregadas com sucesso.", zones.size());
		} catch (Exception e) {
			log.error("Erro ao carregar TimeFarmZones.xml: {}", e.getMessage(), e);
		}
	}

	public boolean enterZone(PlayerCharacter player, int zoneId) {
		if (player == null || player.inventory() == null) return false;
		TimeFarmZone zone = zones.get(zoneId);
		if (zone == null) return false;

		Inventory inv = player.inventory();
		if (zone.costAdena() > 0) {
			int cost = (int) zone.costAdena();
			if (inv.count(57) < zone.costAdena()) {
				player.sendMessage("Adena insuficiente para entrar na zona de farm temporal.");
				return false;
			}
			if (inventoryService != null) {
				var res = inventoryService.consumeItem(inv, 57, cost, "TimeFarmZone");
				if (res == null) {
					player.sendMessage("Adena insuficiente para entrar na zona de farm temporal.");
					return false;
				}
				GameWorld gw = GameWorld.getInstance();
				if (gw != null) {
					gw.sendPacket(player.objectId(), new InventoryUpdate(List.of(
							ItemInfo.of(res.item(), res.removed() ? ItemInfo.REMOVED : ItemInfo.MODIFIED))));
				}
			} else {
				inv.destroyItemByItemId(57, cost);
				GameWorld gw = GameWorld.getInstance();
				if (gw != null) {
					gw.sendPacket(player.objectId(), ItemList.of(inv.items(), false));
				}
			}
		}

		long expire = System.currentTimeMillis() + (zone.intervalMinutes() * 60_000L);
		activeSessions.put(player.objectId(), new ZoneSession(zoneId, expire));
		if (variablesService != null) {
			variablesService.setVariable(player.objectId(), "time_farm_zone_id", String.valueOf(zoneId));
			variablesService.setVariable(player.objectId(), "time_farm_expire_ms", String.valueOf(expire));
		}
		player.teleToLocation(zone.enterX(), zone.enterY(), zone.enterZ());
		GameWorld gw = GameWorld.getInstance();
		if (gw != null) {
			gw.teleportPlayer(player, zone.enterX(), zone.enterY(), zone.enterZ(), player.instanceId());
		}
		player.sendMessage("Voce entrou em " + zone.name() + ". Tempo de permanencia: " + zone.intervalMinutes() + " minutos.");
		return true;
	}

	public long getRemainingTimeSeconds(int playerId) {
		ZoneSession session = activeSessions.get(playerId);
		if (session == null) return 0;
		long remaining = session.expireTimeMillis() - System.currentTimeMillis();
		return Math.max(0, remaining / 1000);
	}

	public boolean isInsideTimeZone(int playerId) {
		return activeSessions.containsKey(playerId);
	}

	public void exitZone(PlayerCharacter player) {
		if (player == null) return;
		ZoneSession session = activeSessions.remove(player.objectId());
		if (variablesService != null) {
			variablesService.deleteVariable(player.objectId(), "time_farm_zone_id");
			variablesService.deleteVariable(player.objectId(), "time_farm_expire_ms");
		}
		if (session != null) {
			TimeFarmZone zone = zones.get(session.zoneId());
			if (zone != null) {
				player.teleToLocation(zone.exitX(), zone.exitY(), zone.exitZ());
				GameWorld gw = GameWorld.getInstance();
				if (gw != null) {
					gw.teleportPlayer(player, zone.exitX(), zone.exitY(), zone.exitZ(), 0);
				}
			}
		}
	}

	public void checkExpiredSessions() {
		checkExpirations(GameWorld.getInstance());
	}

	public void checkExpirations(GameWorld world) {
		long now = System.currentTimeMillis();
		for (Map.Entry<Integer, ZoneSession> entry : activeSessions.entrySet()) {
			if (now >= entry.getValue().expireTimeMillis()) {
				int playerId = entry.getKey();
				ZoneSession session = activeSessions.remove(playerId);
				if (variablesService != null) {
					variablesService.deleteVariable(playerId, "time_farm_zone_id");
					variablesService.deleteVariable(playerId, "time_farm_expire_ms");
				}
				TimeFarmZone zone = session != null ? zones.get(session.zoneId()) : null;
				if (zone != null && world != null) {
					var onlinePlayer = world.findPlayer(playerId);
					if (onlinePlayer != null) {
						var pc = onlinePlayer.character();
						if (pc != null) {
							pc.teleToLocation(zone.exitX(), zone.exitY(), zone.exitZ());
							world.teleportPlayer(pc, zone.exitX(), zone.exitY(), zone.exitZ(), 0);
							pc.sendMessage("Seu tempo na zona de farm temporal expirou!");
						} else {
							onlinePlayer.send(new TeleportToLocation(playerId, zone.exitX(), zone.exitY(), zone.exitZ(), 0));
							onlinePlayer.sendMessage("Seu tempo na zona de farm temporal expirou!");
						}
					}
				}
			} else if (variablesService != null) {
				int playerId = entry.getKey();
				long remSec = Math.max(0L, (entry.getValue().expireTimeMillis() - now) / 1000L);
				variablesService.setVariable(playerId, "time_farm_remaining_sec", String.valueOf(remSec));
			}
		}
	}
}
