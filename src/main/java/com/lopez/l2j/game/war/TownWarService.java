package com.lopez.l2j.game.war;

import com.lopez.l2j.game.model.PlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private final Map<Integer, TownWarConfig> supportedTowns = new ConcurrentHashMap<>();
	private final Map<Integer, ActiveTownWar> activeWars = new ConcurrentHashMap<>();

	public TownWarService() {
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
	 * Verifica se as coordenadas fornecidas estao dentro de alguma cidade atualmente em guerra.
	 */
	public boolean isInsideWarZone(int x, int y, int z) {
		for (ActiveTownWar war : activeWars.values()) {
			if (!war.isExpired() && war.config().contains(x, y, z)) {
				return true;
			}
		}
		return false;
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
		if (!isInsideWarZone(victim.x(), victim.y(), victim.z())) {
			return false;
		}

		// Recompensa em Adena
		if (killer.inventory() != null) {
			var opt = killer.inventory().byItemId(57);
			opt.ifPresent(adena -> adena.count(adena.count() + REWARD_PER_WAR_KILL));
		}

		log.info("Town War Kill: {} eliminou {} dentro da zona de guerra urbana. Recompensa: {} Adena.",
				killer.name(), victim.name(), REWARD_PER_WAR_KILL);
		return true;
	}

	public Map<Integer, ActiveTownWar> activeWars() {
		return Collections.unmodifiableMap(activeWars);
	}

	public Map<Integer, TownWarConfig> supportedTowns() {
		return Collections.unmodifiableMap(supportedTowns);
	}
}
