package com.lopez.l2j.game.zone;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.world.GameWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
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

	public TimeFarmZoneService() {
		loadZones();
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
			if (inv.count(57) < zone.costAdena()) {
				player.sendMessage("Adena insuficiente para entrar na zona de farm temporal.");
				return false;
			}
			inv.destroyItemByItemId(57, (int) zone.costAdena());
		}

		long expire = System.currentTimeMillis() + (zone.intervalMinutes() * 60_000L);
		activeSessions.put(player.objectId(), new ZoneSession(zoneId, expire));
		player.teleToLocation(zone.enterX(), zone.enterY(), zone.enterZ());
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
		if (session != null) {
			TimeFarmZone zone = zones.get(session.zoneId());
			if (zone != null) {
				player.teleToLocation(zone.exitX(), zone.exitY(), zone.exitZ());
			}
		}
	}

	public void checkExpirations(GameWorld world) {
		long now = System.currentTimeMillis();
		for (Map.Entry<Integer, ZoneSession> entry : activeSessions.entrySet()) {
			if (now >= entry.getValue().expireTimeMillis()) {
				int playerId = entry.getKey();
				ZoneSession session = activeSessions.remove(playerId);
				TimeFarmZone zone = zones.get(session.zoneId());
				if (zone != null && world != null) {
					var onlinePlayer = world.findPlayer(playerId);
					if (onlinePlayer != null && onlinePlayer.character() != null) {
						var pc = onlinePlayer.character();
						pc.teleToLocation(zone.exitX(), zone.exitY(), zone.exitZ());
						pc.sendMessage("Seu tempo na zona de farm temporal expirou!");
					}
				}
			}
		}
	}
}
