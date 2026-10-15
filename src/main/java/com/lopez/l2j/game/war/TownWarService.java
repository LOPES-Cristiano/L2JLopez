package com.lopez.l2j.game.war;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.game.zone.Zone;
import com.lopez.l2j.game.zone.ZoneShape;
import com.lopez.l2j.game.zone.ZoneTable;
import com.lopez.l2j.game.zone.ZoneType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servico para gerenciamento das Town Wars (Guerras Urbanas em Capitais) - Onda C10.
 *
 * <p>Recursos:</p>
 * <ul>
 *   <li>Conversao dinamica de capitais inteiras (Giran, Dion, Aden) de Peace Zone em War Zone (PvP).</li>
 *   <li>Combate PvP livre sem ganho de karma (AllowKarma=false).</li>
 *   <li>Bloqueio de Gatekeepers e guardas durante o conflito urbano.</li>
 *   <li>Recompensa por abate em Adena (5000 Adena por kill).</li>
 * </ul>
 */
@Service
public class TownWarService {

	private static final Logger log = LoggerFactory.getLogger(TownWarService.class);

	public static final int REWARD_PER_WAR_KILL = 5000;

	public record TownWarConfig(int townId, String name, int centerX, int centerY, int centerZ, int radius) {
		public boolean contains(int x, int y, int z) {
			double dist = Math.hypot(x - centerX, y - centerY);
			return dist <= radius && Math.abs(z - centerZ) <= 1500;
		}

		public boolean contains2D(int x, int y) {
			return Math.hypot(x - centerX, y - centerY) <= radius;
		}
	}

	public static class ActiveTownWar {
		private final TownWarConfig config;
		private final long startTime;
		private final long endTime;
		private final Map<Integer, Integer> killCounters = new ConcurrentHashMap<>();

		public ActiveTownWar(TownWarConfig config, int durationMinutes) {
			this.config = config;
			this.startTime = System.currentTimeMillis();
			this.endTime = this.startTime + (durationMinutes * 60L * 1000L);
		}

		public TownWarConfig config() { return config; }
		public boolean isExpired() { return System.currentTimeMillis() >= endTime; }
		public Map<Integer, Integer> killCounters() { return killCounters; }
	}

	private ZoneTable zoneTable;
	private InventoryService inventoryService;

	private final Map<Integer, TownWarConfig> supportedTowns = new ConcurrentHashMap<>();
	private final Map<Integer, ActiveTownWar> activeWars = new ConcurrentHashMap<>();

	public TownWarService() {
		this(null, null);
	}

	@Autowired
	public TownWarService(
			@Autowired(required = false) ZoneTable zoneTable,
			@Autowired(required = false) InventoryService inventoryService) {
		this.zoneTable = zoneTable;
		this.inventoryService = inventoryService;
		// Registra capitais oficiais de Lineage II
		registerTown(new TownWarConfig(9, "Giran Castle Town", 83400, 148000, -3400, 4500));
		registerTown(new TownWarConfig(8, "Dion Castle Town", 15670, 142983, -2705, 3500));
		registerTown(new TownWarConfig(12, "Aden Town", 147450, 25620, -2000, 5000));
		registerTown(new TownWarConfig(7, "Gludio Castle Town", -14200, 123500, -3100, 3500));
	}

	public void registerTown(TownWarConfig config) {
		supportedTowns.put(config.townId(), config);
	}

	/**
	 * Inicia o evento de Town War em uma capital, transformando-a em zona de combate PvP.
	 */
	public boolean startTownWar(int townId, int durationMinutes) {
		TownWarConfig config = supportedTowns.get(townId);
		if (config == null) {
			log.warn("Tentativa de iniciar Town War em cidade nao cadastrada: {}", townId);
			return false;
		}

		ActiveTownWar war = new ActiveTownWar(config, durationMinutes);
		activeWars.put(townId, war);

		log.info("TOWN WAR INICIADA: A cidade de {} (ID: {}) entrou em estado de guerra por {} minutos! A Peace Zone foi desativada.",
				config.name(), townId, durationMinutes);
		return true;
	}

	/**
	 * Encerra o evento de Town War na capital, restaurando a Peace Zone.
	 */
	public boolean stopTownWar(int townId) {
		ActiveTownWar war = activeWars.remove(townId);
		if (war != null) {
			log.info("TOWN WAR ENCERRADA: A cidade de {} voltou ao estado pacifico.", war.config().name());
			return true;
		}
		return false;
	}

	public boolean isTownWarActive(int townId) {
		ActiveTownWar war = activeWars.get(townId);
		if (war != null) {
			if (war.isExpired()) {
				stopTownWar(townId);
				return false;
			}
			return true;
		}
		return false;
	}

	/**
	 * Verifica se as coordenadas (x, y, z) estao dentro de alguma cidade atualmente em guerra,
	 * consultando tanto os raios das capitais configuradas quanto as TownZones do ZoneTable.
	 */
	public boolean isInsideWarZone(int x, int y, int z) {
		return getActiveWarTownId(x, y, z, this.zoneTable) > 0;
	}

	/**
	 * Verifica se as coordenadas 2D (x, y) estao dentro de alguma cidade atualmente em guerra.
	 */
	public boolean isInsideWarZone(int x, int y) {
		for (ActiveTownWar war : activeWars.values()) {
			if (!war.isExpired() && war.config().contains2D(x, y)) {
				return true;
			}
		}
		if (zoneTable != null) {
			for (Zone zone : zoneTable.zonesByType(ZoneType.TOWN)) {
				if (zone.townId() > 0 && isTownWarActive(zone.townId())) {
					for (ZoneShape shape : zone.shapes()) {
						if (shape.polygon().contains(x, y)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * Verifica se o jogador esta posicionado dentro de uma cidade em guerra ativa.
	 */
	public boolean isInsideWarZone(PlayerCharacter player) {
		if (player == null) {
			return false;
		}
		return isInsideWarZone(player.x(), player.y(), player.z());
	}

	/**
	 * Verifica se o jogador esta posicionado dentro de uma cidade em guerra ativa cruzando com ZoneTable.
	 */
	public boolean isInsideWarZone(PlayerCharacter player, ZoneTable customZoneTable) {
		if (player == null) {
			return false;
		}
		return getActiveWarTownId(player.x(), player.y(), player.z(),
				customZoneTable != null ? customZoneTable : this.zoneTable) > 0;
	}

	/**
	 * Retorna o townId da cidade em guerra nas coordenadas (x, y, z), ou 0 se nao houver guerra ativa no local.
	 */
	public int getActiveWarTownId(int x, int y, int z) {
		return getActiveWarTownId(x, y, z, this.zoneTable);
	}

	public int getActiveWarTownId(int x, int y, int z, ZoneTable zt) {
		for (ActiveTownWar war : activeWars.values()) {
			if (!war.isExpired() && war.config().contains(x, y, z)) {
				return war.config().townId();
			}
		}
		if (zt != null) {
			for (Zone zone : zt.getZonesAt(x, y, z)) {
				if ((zone.type() == ZoneType.TOWN || zone.townId() > 0) && isTownWarActive(zone.townId())) {
					return zone.townId();
				}
			}
		}
		return 0;
	}

	/**
	 * Durante a Town War, o teletransporte de Gatekeeper para a cidade e bloqueado.
	 */
	public boolean isGatekeeperDisabled(int townId) {
		return isTownWarActive(townId);
	}

	/**
	 * Processa o abate de um jogador na zona de guerra urbana:
	 * Premia o vencedor com Adena e dispensa a aplicacao de karma/PK.
	 */
	public boolean onWarKill(PlayerCharacter killer, PlayerCharacter victim) {
		if (killer == null || victim == null || killer.objectId() == victim.objectId()) {
			return false;
		}

		// Checa se o combate ocorreu dentro de uma Town War ativa
		int townId = getActiveWarTownId(victim.x(), victim.y(), victim.z());
		if (townId <= 0) {
			townId = getActiveWarTownId(killer.x(), killer.y(), killer.z());
		}
		if (townId <= 0) {
			return false;
		}

		ActiveTownWar activeWar = activeWars.get(townId);
		if (activeWar != null) {
			activeWar.killCounters().merge(killer.objectId(), 1, Integer::sum);
		}

		// Recompensa em Adena com persistencia via InventoryService e fallback em memoria
		if (killer.inventory() != null) {
			boolean persisted = false;
			if (inventoryService != null) {
				var added = inventoryService.addItem(killer.inventory(), 57, REWARD_PER_WAR_KILL, "TownWarKill");
				persisted = (added != null);
			}
			if (!persisted) {
				var opt = killer.inventory().byItemId(57);
				opt.ifPresent(adena -> adena.count(adena.count() + REWARD_PER_WAR_KILL));
			}
		}

		log.info("Town War Kill: {} eliminou {} dentro da zona de guerra urbana (TownID={}). Recompensa: {} Adena.",
				killer.name(), victim.name(), townId, REWARD_PER_WAR_KILL);
		return true;
	}

	public Map<Integer, ActiveTownWar> activeWars() {
		return Collections.unmodifiableMap(activeWars);
	}

	public Map<Integer, TownWarConfig> supportedTowns() {
		return Collections.unmodifiableMap(supportedTowns);
	}

	public boolean isTownWarActiveAt(int x, int y) {
		return isInsideWarZone(x, y);
	}

	public boolean isTownWarActiveAt(int x, int y, int z) {
		return isInsideWarZone(x, y, z);
	}

	public boolean startTownWar(int townId) {
		return startTownWar(townId, 60);
	}

	public String getStatusHtml() {
		StringBuilder sb = new StringBuilder();
		sb.append("<html><body><title>Town War</title><center>");
		sb.append("<font color=\"LEVEL\">== Guerra Urbana (Town War) ==</font><br><br>");
		if (activeWars.isEmpty()) {
			sb.append("<font color=\"00FF00\">PAZ</font><br>");
			sb.append("Nenhuma guerra urbana em andamento.<br><br>");
		} else {
			sb.append("<font color=\"FF0000\">EM GUERRA!</font><br><br>");
			for (ActiveTownWar war : activeWars.values()) {
				sb.append("Cidade: <font color=\"AAAAAA\">").append(war.config().name()).append("</font><br>");
				sb.append("Baixas Totais: <font color=\"LEVEL\">").append(war.killCounters().values().stream().mapToInt(Integer::intValue).sum()).append("</font><br><br>");
			}
			sb.append("<font color=\"FFAA00\">Atencao: A zona de paz das capitais em guerra esta DESATIVADA!</font>");
		}
		sb.append("</center></body></html>");
		return sb.toString();
	}

	public void setZoneTable(ZoneTable zoneTable) {
		this.zoneTable = zoneTable;
	}

	public void setInventoryService(InventoryService inventoryService) {
		this.inventoryService = inventoryService;
	}
}
