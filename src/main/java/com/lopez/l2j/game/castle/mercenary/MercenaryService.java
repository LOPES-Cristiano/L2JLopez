package com.lopez.l2j.game.castle.mercenary;

import com.lopez.l2j.game.castle.Castle;
import com.lopez.l2j.game.castle.CastleManager;
import com.lopez.l2j.game.castle.siege.SiegeService;
import com.lopez.l2j.game.castle.siege.SiegeStatus;
import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.model.PlayerCharacter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Servico de gerenciamento de bilhetes de mercenarios defensores de castelo (MercTicketManager do L2JDream).
 * Permite que lordes contratem guardas e arqueiros para defender as muralhas durante o cerco.
 */
@Service
public class MercenaryService {

	private static final Logger log = LoggerFactory.getLogger(MercenaryService.class);

	private final CastleManager castleManager;
	private final SiegeService siegeService;
	private final JdbcClient jdbc;
	private final AtomicInteger idCounter = new AtomicInteger(1000);

	// castleId -> Lista de MercenaryRecord contratados
	private final Map<Integer, List<MercenaryRecord>> hiredMercs = new ConcurrentHashMap<>();

	@Autowired
	public MercenaryService(CastleManager castleManager,
							@Autowired(required = false) SiegeService siegeService,
							@Autowired(required = false) JdbcClient jdbc) {
		this.castleManager = castleManager;
		this.siegeService = siegeService;
		this.jdbc = jdbc;
		loadHiredMercenaries();
	}

	/**
	 * Limite maximo de mercenarios por castelo (baseado no legado L2JDream).
	 */
	public int getMaxMercenaries(int castleId) {
		return switch (castleId) {
			case CastleManager.GLUDIO, CastleManager.GIRAN, CastleManager.OREN -> 40;
			case CastleManager.DION -> 60;
			case CastleManager.ADEN, CastleManager.INNADRIL, CastleManager.GODDARD,
				 CastleManager.RUNE, CastleManager.SCHUTTGART -> 80;
			default -> 30;
		};
	}

	/**
	 * Retorna a lista de mercenarios contratados para determinado castelo.
	 */
	public List<MercenaryRecord> getHiredMercenaries(int castleId) {
		return hiredMercs.getOrDefault(castleId, Collections.emptyList());
	}

	/**
	 * Contrata e posiciona um mercenario na muralha do castelo atraves de um bilhete.
	 */
	public boolean hireMercenary(PlayerCharacter player, Clan clan, int ticketItemId, int x, int y, int z, int heading) {
		if (player == null || clan == null) {
			return false;
		}

		int castleId = clan.castleId();
		if (castleId <= 0) {
			log.warn("{} tentou contratar mercenario sem possuir castelo", player.name());
			return false;
		}

		// Valida se o cerco esta em andamento
		if (siegeService != null) {
			var siegeOpt = siegeService.getSiege(castleId);
			if (siegeOpt.isPresent() && siegeOpt.get().status() == SiegeStatus.IN_PROGRESS) {
				log.warn("Nao e permitido contratar mercenarios durante a batalha de cerco");
				return false;
			}
		}

		List<MercenaryRecord> list = hiredMercs.computeIfAbsent(castleId, k -> new java.util.concurrent.CopyOnWriteArrayList<>());
		if (list.size() >= getMaxMercenaries(castleId)) {
			log.warn("Castelo {} atingiu o limite maximo de mercenarios ({})", castleId, getMaxMercenaries(castleId));
			return false;
		}

		// Verifica e consome o bilhete do inventario
		Inventory inv = player.inventory();
		if (inv != null) {
			var ticketOpt = inv.byItemId(ticketItemId);
			if (ticketOpt.isEmpty()) {
				log.warn("{} nao possui o bilhete de mercenario {}", player.name(), ticketItemId);
				return false;
			}
			inv.remove(ticketOpt.get());
		}

		int npcId = resolveNpcIdFromTicket(ticketItemId);
		int id = idCounter.incrementAndGet();
		MercenaryRecord record = new MercenaryRecord(id, castleId, npcId, x, y, z, heading, 0, true);
		list.add(record);

		saveMercenary(record);
		log.info("Mercenario {} (NPC {}) contratado para o Castelo {} em ({}, {}, {}) por {}",
				id, npcId, castleId, x, y, z, player.name());
		return true;
	}

	/**
	 * Remove todos os mercenarios contratados quando o castelo e conquistado ou cerco termina.
	 */
	public void clearCastleMercenaries(int castleId) {
		hiredMercs.remove(castleId);
		if (jdbc != null) {
			try {
				jdbc.sql("DELETE FROM castle_siege_guards WHERE castleId = :cid AND isHired = 1")
						.param("cid", castleId)
						.update();
			} catch (Exception ex) {
				log.warn("Erro ao limpar mercenarios do castelo {}: {}", castleId, ex.getMessage());
			}
		}
		log.info("Mercenarios contratados do Castelo {} foram dispensados", castleId);
	}

	/**
	 * Converte o ID do item de bilhete de mercenario para o respectivo template de NPC de guarda.
	 */
	public int resolveNpcIdFromTicket(int ticketItemId) {
		// No Interlude, bilhetes variam de 3960-3972 (Gludio), 4400-4415 (Dion), etc.
		// Mapeamento padrao de template de guarda de cerco
		if (ticketItemId >= 3960 && ticketItemId <= 3975) {
			return 35010 + (ticketItemId - 3960);
		}
		return 35010; // Fallback padrao: Siege Guard
	}

	private void saveMercenary(MercenaryRecord record) {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("""
					INSERT INTO castle_siege_guards (castleId, npcId, x, y, z, heading, respawnDelay, isHired)
					VALUES (:castleId, :npcId, :x, :y, :z, :heading, :respawnDelay, 1)
					""")
					.param("castleId", record.castleId())
					.param("npcId", record.npcId())
					.param("x", record.x())
					.param("y", record.y())
					.param("z", record.z())
					.param("heading", record.heading())
					.param("respawnDelay", record.respawnDelay())
					.update();
		} catch (Exception ex) {
			log.warn("Erro ao persistir guarda mercenario no banco: {}", ex.getMessage());
		}
	}

	private void loadHiredMercenaries() {
		if (jdbc == null) {
			return;
		}
		try {
			List<Map<String, Object>> rows = jdbc.sql("""
					SELECT id, castleId, npcId, x, y, z, heading, respawnDelay, isHired
					FROM castle_siege_guards
					WHERE isHired = 1
					""").query().listOfRows();

			for (var row : rows) {
				int id = ((Number) row.get("id")).intValue();
				int castleId = ((Number) row.get("castleId")).intValue();
				int npcId = ((Number) row.get("npcId")).intValue();
				int x = ((Number) row.get("x")).intValue();
				int y = ((Number) row.get("y")).intValue();
				int z = ((Number) row.get("z")).intValue();
				int heading = row.get("heading") != null ? ((Number) row.get("heading")).intValue() : 0;
				int respawnDelay = row.get("respawnDelay") != null ? ((Number) row.get("respawnDelay")).intValue() : 0;
				boolean isHired = ((Number) row.get("isHired")).intValue() == 1;

				MercenaryRecord rec = new MercenaryRecord(id, castleId, npcId, x, y, z, heading, respawnDelay, isHired);
				hiredMercs.computeIfAbsent(castleId, k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(rec);
				idCounter.updateAndGet(curr -> Math.max(curr, id));
			}
			log.info("MercenaryService: carregados mercenarios contratados do banco de dados");
		} catch (Exception ex) {
			log.warn("Nao foi possivel carregar mercenarios de castle_siege_guards: {}", ex.getMessage());
		}
	}
}
