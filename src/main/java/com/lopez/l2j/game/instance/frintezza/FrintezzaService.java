package com.lopez.l2j.game.instance.frintezza;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.boss.GrandBossManager;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.npc.NpcTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.DeleteObject;
import com.lopez.l2j.network.game.packet.GameServerPacket.Earthquake;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.SpecialCamera;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
	private NpcInstance frintezzaNpc;
	private NpcInstance activeScarlet;

	private final GrandBossManager grandBossManager;
	private final JdbcClient jdbc;
	private final NpcTemplateTable templates;
	private final GameWorld world;
	private final ObjectIdFactory objectIds;

	public FrintezzaService(GrandBossManager grandBossManager, JdbcClient jdbc) {
		this(grandBossManager, jdbc, null, null, null);
	}

	@Autowired
	public FrintezzaService(
			@Autowired(required = false) GrandBossManager grandBossManager,
			@Autowired(required = false) JdbcClient jdbc,
			@Autowired(required = false) NpcTemplateTable templates,
			@Autowired(required = false) GameWorld world,
			@Autowired(required = false) ObjectIdFactory objectIds) {
		this.grandBossManager = grandBossManager;
		this.jdbc = jdbc;
		this.templates = templates;
		this.world = world;
		this.objectIds = objectIds;
		loadState();
	}

	public static int getRegistrationMode() {
		return Config.REGISTRATION_MODE;
	}

	public static int getRegistrationTimeMinutes() {
		return Math.max(1, Config.REGISTRATION_TIME);
	}

	public static int getMinPartyCount() {
		return Math.max(1, Config.MIN_PARTY_COUNT);
	}

	public static int getMaxPartyCount() {
		return Math.max(getMinPartyCount(), Config.MAX_PARTY_COUNT);
	}

	public static int getMinPlayerCount() {
		return Math.max(1, Config.MIN_PLAYER_COUNT);
	}

	public static int getMaxPlayerCount() {
		return Math.max(getMinPlayerCount(), Config.MAX_PLAYER_COUNT);
	}

	public static int getTimeLimitMinutes() {
		return Math.max(1, Config.TIME_LIMIT);
	}

	public static long getTimeLimitMillis() {
		return getTimeLimitMinutes() * 60_000L;
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

	public Optional<NpcInstance> activeScarlet() {
		return Optional.ofNullable(activeScarlet);
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
	 * Verifica se o grupo/command channel atende aos limites configurados para Last Imperial Tomb
	 * (Config.REGISTRATION_MODE, Config.MIN_PARTY_COUNT, Config.MAX_PARTY_COUNT, Config.MIN_PLAYER_COUNT, Config.MAX_PLAYER_COUNT).
	 */
	public boolean canEnterWithGroup(PlayerCharacter leader, int partyCount, List<PlayerCharacter> members) {
		if (!canEnter(leader)) {
			return false;
		}
		int totalPlayers = members != null ? members.size() : 1;
		if (Config.REGISTRATION_MODE == 0) {
			if (partyCount < getMinPartyCount() || partyCount > getMaxPartyCount()) {
				return false;
			}
		} else {
			if (totalPlayers < getMinPlayerCount() || totalPlayers > getMaxPlayerCount()) {
				return false;
			}
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
		if (partyMembers != null && !partyMembers.isEmpty()) {
			if (partyMembers.size() < getMinPlayerCount() || partyMembers.size() > getMaxPlayerCount()) {
				return false;
			}
		}

		Inventory inv = leader.inventory();
		inv.byItemId(FORCE_FIELD_SCROLL).ifPresent(inv::remove);

		GameWorld gw = world != null ? world : GameWorld.getInstance();
		playersInside.clear();
		if (partyMembers != null) {
			for (PlayerCharacter pc : partyMembers) {
				if (pc != null && pc.level() >= MIN_LEVEL) {
					if (gw != null) {
						gw.teleportPlayer(pc, TOMB_X, TOMB_Y, TOMB_Z, 0);
					} else {
						pc.x(TOMB_X);
						pc.y(TOMB_Y);
						pc.z(TOMB_Z);
					}
					playersInside.add(pc.objectId());
				}
			}
		} else {
			if (gw != null) {
				gw.teleportPlayer(leader, TOMB_X, TOMB_Y, TOMB_Z, 0);
			} else {
				leader.x(TOMB_X);
				leader.y(TOMB_Y);
				leader.z(TOMB_Z);
			}
			playersInside.add(leader.objectId());
		}

		status = FrintezzaStatus.ENTRY;
		log.info("{} e seu grupo entraram no Ultimo Tumulo Imperial ({} jogadores, limite {} min)",
				leader.name(), playersInside.size(), getTimeLimitMinutes());
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
		spawnScarletPhase(SCARLET_WEAK_ID, "Scarlet Van Halisha");
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
		spawnScarletPhase(SCARLET_MEDIUM_ID, "Scarlet Van Halisha");
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
		spawnScarletPhase(SCARLET_STRONG_ID, "Scarlet Van Halisha");
		log.info("Scarlet Van Halisha atinge sua forma suprema (Fase 3 - NPC {})!", SCARLET_STRONG_ID);
	}

	/**
	 * Conclusao do encontro: Scarlet e derrotada, Frintezza se lamenta e o colar epico e gerado.
	 */
	public void onScarletDefeated() {
		status = FrintezzaStatus.DEAD;
		respawnTime = System.currentTimeMillis() + RESPAWN_INTERVAL_MILLIS;

		GameWorld gw = world != null ? world : GameWorld.getInstance();
		if (activeScarlet != null && gw != null) {
			gw.removeNpc(activeScarlet);
			activeScarlet = null;
		}
		if (frintezzaNpc != null && gw != null) {
			gw.removeNpc(frintezzaNpc);
			frintezzaNpc = null;
		}
		if (gw != null) {
			gw.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Frintezza",
					"Scarlet Van Halisha has fallen! The Imperial Tomb falls silent..."), p -> true);
		}

		if (grandBossManager != null) {
			grandBossManager.notifyBossKilled(FRINTEZZA_NPC_ID);
		}

		saveState();
		log.info("Scarlet Van Halisha foi derrotada! O Colar de Frintezza (Item {}) foi liberado.", NECKLACE_OF_FRINTEZZA);
	}

	private void spawnScarletPhase(int scarletNpcId, String name) {
		GameWorld gw = world != null ? world : GameWorld.getInstance();
		if (gw == null || objectIds == null) {
			return;
		}
		if (activeScarlet != null) {
			int oldId = activeScarlet.objectId();
			gw.removeNpc(activeScarlet);
			gw.broadcast(new DeleteObject(oldId), p -> playersInside.contains(p.objectId()));
			activeScarlet = null;
		}
		if (frintezzaNpc == null) {
			NpcTemplate frintTpl = templates != null
					? templates.get(FRINTEZZA_NPC_ID).orElseGet(() -> NpcTemplate.fallback(FRINTEZZA_NPC_ID, "Frintezza", "L2GrandBoss"))
					: NpcTemplate.fallback(FRINTEZZA_NPC_ID, "Frintezza", "L2GrandBoss");
			frintezzaNpc = new NpcInstance(objectIds.nextId(), frintTpl, TOMB_X, TOMB_Y - 500, TOMB_Z + 100, 16384);
			gw.addNpc(frintezzaNpc);
			gw.broadcast(NpcInfo.of(frintezzaNpc), p -> playersInside.contains(p.objectId()));
		}

		NpcTemplate scarletTpl = templates != null
				? templates.get(scarletNpcId).orElseGet(() -> NpcTemplate.fallback(scarletNpcId, name, "L2GrandBoss"))
				: NpcTemplate.fallback(scarletNpcId, name, "L2GrandBoss");
		activeScarlet = new NpcInstance(objectIds.nextId(), scarletTpl, TOMB_X, TOMB_Y, TOMB_Z, 0);
		gw.addNpc(activeScarlet);

		gw.broadcast(NpcInfo.of(activeScarlet), p -> playersInside.contains(p.objectId()));
		gw.broadcast(new Earthquake(TOMB_X, TOMB_Y, TOMB_Z, 35, 8), p -> playersInside.contains(p.objectId()));
		gw.broadcast(new SpecialCamera(activeScarlet.objectId(), 250, 180, 25, 2500, 6000, 0, 0, 1, 0),
				p -> playersInside.contains(p.objectId()));
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
