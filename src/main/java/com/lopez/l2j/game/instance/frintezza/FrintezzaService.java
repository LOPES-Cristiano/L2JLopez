package com.lopez.l2j.game.instance.frintezza;

import com.lopez.l2j.game.boss.GrandBossManager;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.PlayerCharacter;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Servico responsavel pela instancia do Ultimo Tumulo Imperial (Last Imperial Tomb)
 * e o confronto com Frintezza e Scarlet Van Halisha (FrintezzaManager do L2JDream).
 */
@Service
public class FrintezzaService {

	private static final Logger log = LoggerFactory.getLogger(FrintezzaService.class);

	public static final int FRINTEZZA_NPC_ID = 29045;
	public static final int SCARLET_WEAK_ID = 29046;
	public static final int SCARLET_MEDIUM_ID = 29047;
	public static final int SCARLET_STRONG_ID = 29048;

	public static final int FORCE_FIELD_SCROLL = 8073; // Frintezza's Magic Force Field Removal Scroll
	public static final int NECKLACE_OF_FRINTEZZA = 8559;

	public static final int MIN_LEVEL = 74;
	public static final long RESPAWN_INTERVAL_MILLIS = 48 * 60 * 60 * 1000L; // 48 horas

	// Coordenadas da camara principal do Ultimo Tumulo Imperial
	public static final int TOMB_X = -87560;
	public static final int TOMB_Y = -153490;
	public static final int TOMB_Z = -9165;

	private FrintezzaStatus status = FrintezzaStatus.ALIVE;
	private long respawnTime = 0L;
	private final List<Integer> playersInside = new CopyOnWriteArrayList<>();

	private final GrandBossManager grandBossManager;
	private final JdbcClient jdbc;

	@Autowired
	public FrintezzaService(@Autowired(required = false) GrandBossManager grandBossManager,
							@Autowired(required = false) JdbcClient jdbc) {
		this.grandBossManager = grandBossManager;
		this.jdbc = jdbc;
		loadState();
	}

	public FrintezzaStatus getStatus() {
		return status;
	}

	public long getRespawnTime() {
		return respawnTime;
	}

	public List<Integer> getPlayersInside() {
		return Collections.unmodifiableList(playersInside);
	}

	/**
	 * Verifica se o lider e o grupo atendem aos requisitos para entrar no tumulo.
	 */
	public boolean canEnter(PlayerCharacter leader) {
		if (leader == null || leader.level() < MIN_LEVEL) {
			return false;
		}

		if (status != FrintezzaStatus.ALIVE) {
			return false;
		}

		Inventory inv = leader.inventory();
		if (inv == null || inv.byItemId(FORCE_FIELD_SCROLL).isEmpty()) {
			return false;
		}

		return true;
	}

	/**
	 * Consome o pergaminho de remocao do campo de forca e teleporta o grupo para dentro da camara.
	 */
	public boolean enterTomb(PlayerCharacter leader, List<PlayerCharacter> partyMembers) {
		if (!canEnter(leader)) {
			return false;
		}

		Inventory inv = leader.inventory();
		inv.byItemId(FORCE_FIELD_SCROLL).ifPresent(inv::remove);

		playersInside.clear();
		if (partyMembers != null) {
			for (PlayerCharacter pc : partyMembers) {
				if (pc != null && pc.level() >= MIN_LEVEL) {
					pc.x(TOMB_X);
					pc.y(TOMB_Y);
					pc.z(TOMB_Z);
					playersInside.add(pc.objectId());
				}
			}
		} else {
			leader.x(TOMB_X);
			leader.y(TOMB_Y);
			leader.z(TOMB_Z);
			playersInside.add(leader.objectId());
		}

		status = FrintezzaStatus.ENTRY;
		log.info("{} e seu grupo entraram no Ultimo Tumulo Imperial ({} jogadores)",
				leader.name(), playersInside.size());
		return true;
	}

	/**
	 * Inicia a primeira fase: Frintezza toca o orgao e Scarlet Van Halisha (Fraca) surge.
	 */
	public void startFight() {
		if (status != FrintezzaStatus.ENTRY) {
			return;
		}
		status = FrintezzaStatus.PHASE_1_WEAK;
		log.info("Frintezza inicia a melodia do orgao! Scarlet Van Halisha Fase 1 (NPC {}) surge!", SCARLET_WEAK_ID);
	}

	/**
	 * Transicao para a Fase 2 (Media).
	 */
	public void transitionToPhase2() {
		if (status != FrintezzaStatus.PHASE_1_WEAK) {
			return;
		}
		status = FrintezzaStatus.PHASE_2_MEDIUM;
		log.info("Scarlet Van Halisha se transforma para a Fase 2 (Media - NPC {})!", SCARLET_MEDIUM_ID);
	}

	/**
	 * Transicao para a Fase 3 (Forte / Demonio Gigante).
	 */
	public void transitionToPhase3() {
		if (status != FrintezzaStatus.PHASE_2_MEDIUM) {
			return;
		}
		status = FrintezzaStatus.PHASE_3_STRONG;
		log.info("Scarlet Van Halisha atinge sua forma suprema (Fase 3 - NPC {})!", SCARLET_STRONG_ID);
	}

	/**
	 * Conclusao do encontro: Scarlet e derrotada, Frintezza se lamenta e o colar epico e gerado.
	 */
	public void onScarletDefeated() {
		status = FrintezzaStatus.DEAD;
		respawnTime = System.currentTimeMillis() + RESPAWN_INTERVAL_MILLIS;

		if (grandBossManager != null) {
			grandBossManager.notifyBossKilled(FRINTEZZA_NPC_ID);
		}

		saveState();
		log.info("Scarlet Van Halisha foi derrotada! O Colar de Frintezza (Item {}) foi liberado.", NECKLACE_OF_FRINTEZZA);
	}

	public void resetToAlive() {
		status = FrintezzaStatus.ALIVE;
		respawnTime = 0L;
		playersInside.clear();
		saveState();
		log.info("Ultimo Tumulo Imperial resetado para status ALIVE");
	}

	private void saveState() {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("""
					REPLACE INTO grandboss_data (boss_id, loc_x, loc_y, loc_z, heading, respawn_time, currentHp, currentMp, status)
					VALUES (:id, :x, :y, :z, 0, :respawn, 1000000, 500000, :status)
					""")
					.param("id", FRINTEZZA_NPC_ID)
					.param("x", TOMB_X)
					.param("y", TOMB_Y)
					.param("z", TOMB_Z)
					.param("respawn", respawnTime)
					.param("status", status.ordinal())
					.update();
		} catch (Exception ex) {
			log.warn("Erro ao salvar status de Frintezza em grandboss_data: {}", ex.getMessage());
		}
	}

	private void loadState() {
		if (jdbc == null) {
			return;
		}
		try {
			var rows = jdbc.sql("SELECT respawn_time, status FROM grandboss_data WHERE boss_id = :id")
					.param("id", FRINTEZZA_NPC_ID)
					.query().listOfRows();

			if (!rows.isEmpty()) {
				var row = rows.get(0);
				respawnTime = ((Number) row.get("respawn_time")).longValue();
				int stOrdinal = ((Number) row.get("status")).intValue();
				if (respawnTime > System.currentTimeMillis()) {
					status = FrintezzaStatus.INTERVAL;
				} else if (stOrdinal >= 0 && stOrdinal < FrintezzaStatus.values().length) {
					status = FrintezzaStatus.values()[stOrdinal];
				}
				log.info("FrintezzaService: status carregado do banco (status: {}, respawn: {})", status, respawnTime);
			}
		} catch (Exception ex) {
			log.warn("Nao foi possivel carregar grandboss_data de Frintezza: {}", ex.getMessage());
		}
	}
}
