package com.lopez.l2j.game.war;

import com.lopez.l2j.game.model.PlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servico para gerenciamento do modo Good vs Evil (GvE) - Onda C10.
 *
 * <p>Recursos:</p>
 * <ul>
 *   <li>Duas facções rivais: Good (Azul) e Evil (Vermelho).</li>
 *   <li>Relacionamento automatico: membros da mesma facção sao aliados, membros opostos sao inimigos diretos (Karma-free).</li>
 *   <li>Pontuacao por abates e conquista de relicarios territoriais.</li>
 *   <li>Recompensas organicas em Adena por vitoria em combate.</li>
 * </ul>
 */
@Service
public class GvEService {

	private static final Logger log = LoggerFactory.getLogger(GvEService.class);

	public static final int REWARD_PER_KILL_ADENA = 5000;
	public static final int RELIC_CAPTURE_POINTS = 50;

	public record GvEZone(String name, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
		public boolean contains(int x, int y, int z) {
			return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
		}
	}

	private volatile boolean active = true;
	private final AtomicInteger goodScore = new AtomicInteger(0);
	private final AtomicInteger evilScore = new AtomicInteger(0);
	private final List<GvEZone> gveZones = new CopyOnWriteArrayList<>();
	private final Map<String, FactionType> relicOwners = new ConcurrentHashMap<>();

	public GvEService() {
		// Registra zona padrao de fronteira GvE (ex: Gludio Borderlands)
		registerGvEZone("Gludio Borderlands", -20000, 20000, 100000, 140000, -5000, 5000);
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public void registerGvEZone(String name, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
		gveZones.add(new GvEZone(name, minX, maxX, minY, maxY, minZ, maxZ));
	}

	public boolean isInsideGvEZone(int x, int y, int z) {
		for (GvEZone zone : gveZones) {
			if (zone.contains(x, y, z)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Associa o jogador a uma facção e atualiza sua cor de nome.
	 */
	public void joinFaction(PlayerCharacter player, FactionType faction) {
		if (player == null || faction == null) {
			return;
		}
		player.faction(faction);
		player.nameColor(faction.nameColor());
		log.info("Jogador {} ingressou na faccao {} (Cor: 0x{})",
				player.name(), faction.displayName(), Integer.toHexString(faction.nameColor()));
	}

	/**
	 * Desassocia o jogador da facção e restaura a cor de nome padrao.
	 */
	public void leaveFaction(PlayerCharacter player) {
		if (player == null) {
			return;
		}
		player.faction(FactionType.NONE);
		player.nameColor(0xFFFFFF);
		log.info("Jogador {} deixou sua faccao GvE.", player.name());
	}

	/**
	 * Determina se o alvo e considerado um inimigo sob as regras de facção GvE.
	 */
	public boolean isEnemy(PlayerCharacter attacker, PlayerCharacter target) {
		if (!active || attacker == null || target == null) {
			return false;
		}
		if (attacker.faction() == FactionType.NONE || target.faction() == FactionType.NONE) {
			return false;
		}
		return attacker.faction().isOpponent(target.faction());
	}

	/**
	 * Determina se dois jogadores sao aliados da mesma facção.
	 */
	public boolean isAlly(PlayerCharacter p1, PlayerCharacter p2) {
		if (!active || p1 == null || p2 == null) {
			return false;
		}
		if (p1.faction() == FactionType.NONE || p2.faction() == FactionType.NONE) {
			return false;
		}
		return p1.faction() == p2.faction();
	}

	/**
	 * Processa o abate de um inimigo no GvE conferindo pontos e recompensa em Adena.
	 */
	public boolean onKill(PlayerCharacter killer, PlayerCharacter victim) {
		if (!active || killer == null || victim == null) {
			return false;
		}
		if (killer.objectId() == victim.objectId()) {
			return false;
		}
		if (!isEnemy(killer, victim)) {
			return false;
		}

		if (killer.faction() == FactionType.GOOD) {
			goodScore.incrementAndGet();
		} else if (killer.faction() == FactionType.EVIL) {
			evilScore.incrementAndGet();
		}

		// Recompensa em Adena
		if (killer.inventory() != null) {
			var opt = killer.inventory().byItemId(57);
			opt.ifPresent(adena -> adena.count(adena.count() + REWARD_PER_KILL_ADENA));
		}

		log.info("GvE Kill: {} [{}] eliminou {} [{}]. Placar Good: {} vs Evil: {}",
				killer.name(), killer.faction(), victim.name(), victim.faction(),
				goodScore.get(), evilScore.get());
		return true;
	}

	/**
	 * Processa a conquista de um relicário territorial por uma facção.
	 */
	public void captureRelic(String relicName, FactionType faction) {
		if (faction == null || faction == FactionType.NONE) {
			return;
		}
		relicOwners.put(relicName, faction);
		if (faction == FactionType.GOOD) {
			goodScore.addAndGet(RELIC_CAPTURE_POINTS);
		} else if (faction == FactionType.EVIL) {
			evilScore.addAndGet(RELIC_CAPTURE_POINTS);
		}
		log.info("GvE Relic: A faccao {} conquistou o relicario '{}'! (+{} pontos)",
				faction.displayName(), relicName, RELIC_CAPTURE_POINTS);
	}

	public int getScore(FactionType faction) {
		if (faction == FactionType.GOOD) return goodScore.get();
		if (faction == FactionType.EVIL) return evilScore.get();
		return 0;
	}

	public FactionType getRelicOwner(String relicName) {
		return relicOwners.getOrDefault(relicName, FactionType.NONE);
	}
}
