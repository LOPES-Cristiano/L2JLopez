package com.lopez.l2j.game.clan.clanhall;

import com.lopez.l2j.game.model.PlayerCharacter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Servico de funcoes de Clan Hall (ClanHall.ClanHallFunction do L2JDream).
 * Gerencia regeneracao acelerada de HP/MP, recuperacao de XP ao ressuscitar,
 * teleporte privado e buffs de suporte para membros do cla.
 */
@Service
public class ClanHallFunctionService {

	private static final Logger log = LoggerFactory.getLogger(ClanHallFunctionService.class);

	// hallId -> Map<type, ClanHallFunctionRecord>
	private final Map<Integer, Map<Integer, ClanHallFunctionRecord>> hallFunctions = new ConcurrentHashMap<>();
	private final ClanHallService clanHallService;
	private final JdbcClient jdbc;

	@Autowired
	public ClanHallFunctionService(ClanHallService clanHallService,
			@Autowired(required = false) JdbcClient jdbc) {
		this.clanHallService = clanHallService;
		this.jdbc = jdbc;
		loadFunctions();
	}

	public Collection<ClanHallFunctionRecord> getFunctions(int hallId) {
		var map = hallFunctions.get(hallId);
		return map != null ? Collections.unmodifiableCollection(map.values()) : Collections.emptyList();
	}

	public Optional<ClanHallFunctionRecord> getFunction(int hallId, int type) {
		var map = hallFunctions.get(hallId);
		return map != null ? Optional.ofNullable(map.get(type)) : Optional.empty();
	}

	/**
	 * Instala ou atualiza uma funcao em um Clan Hall.
	 */
	public synchronized boolean setFunction(int hallId, int type, int level, int fee, long rateMillis) {
		var hallOpt = clanHallService.getClanHall(hallId);
		if (hallOpt.isEmpty() || hallOpt.get().ownerId() <= 0) {
			return false;
		}

		long now = System.currentTimeMillis();
		long endTime = now + rateMillis;
		var record = new ClanHallFunctionRecord(hallId, type, level, fee, rateMillis, endTime);

		hallFunctions.computeIfAbsent(hallId, k -> new ConcurrentHashMap<>()).put(type, record);

		if (jdbc != null) {
			try {
				jdbc.sql("""
						REPLACE INTO clanhall_functions (hall_id, type, lvl, lease, rate, endTime)
						VALUES (:hallId, :type, :lvl, :lease, :rate, :endTime)
						""")
						.param("hallId", hallId)
						.param("type", type)
						.param("lvl", level)
						.param("lease", fee)
						.param("rate", rateMillis)
						.param("endTime", endTime)
						.update();
			} catch (Exception ex) {
				log.error("Erro ao persistir funcao {} no clanhall {}", type, hallId, ex);
				return false;
			}
		}

		log.info("Funcao {} Lvl {} instalada no ClanHall ID {} (Custo: {}, Validade: {})",
				type, level, hallId, fee, endTime);
		return true;
	}

	/**
	 * Remove uma funcao do Clan Hall.
	 */
	public synchronized boolean removeFunction(int hallId, int type) {
		var map = hallFunctions.get(hallId);
		if (map != null) {
			map.remove(type);
		}

		if (jdbc != null) {
			try {
				jdbc.sql("DELETE FROM clanhall_functions WHERE hall_id = :hallId AND type = :type")
						.param("hallId", hallId)
						.param("type", type)
						.update();
			} catch (Exception ex) {
				log.error("Erro ao deletar funcao {} do clanhall {}", type, hallId, ex);
				return false;
			}
		}

		log.info("Funcao {} removida do ClanHall ID {}", type, hallId);
		return true;
	}

	/**
	 * Calcula multiplicador de regeneracao de HP dentro do Clan Hall.
	 * Exemplo: lvl 100 -> +100% (multiplicador 2.0x).
	 */
	public double calculateHpRegenBonus(PlayerCharacter player, int clanHallId) {
		if (player == null || player.clanId() <= 0 || clanHallId <= 0) {
			return 1.0;
		}
		var hallOpt = clanHallService.getClanHall(clanHallId);
		if (hallOpt.isEmpty() || hallOpt.get().ownerId() != player.clanId()) {
			return 1.0;
		}
		var funcOpt = getFunction(clanHallId, ClanHallFunctionRecord.FUNC_RESTORE_HP);
		if (funcOpt.isPresent() && funcOpt.get().endTime() > System.currentTimeMillis()) {
			return 1.0 + (funcOpt.get().level() / 100.0);
		}
		return 1.0;
	}

	/**
	 * Calcula multiplicador de regeneracao de MP dentro do Clan Hall.
	 */
	public double calculateMpRegenBonus(PlayerCharacter player, int clanHallId) {
		if (player == null || player.clanId() <= 0 || clanHallId <= 0) {
			return 1.0;
		}
		var hallOpt = clanHallService.getClanHall(clanHallId);
		if (hallOpt.isEmpty() || hallOpt.get().ownerId() != player.clanId()) {
			return 1.0;
		}
		var funcOpt = getFunction(clanHallId, ClanHallFunctionRecord.FUNC_RESTORE_MP);
		if (funcOpt.isPresent() && funcOpt.get().endTime() > System.currentTimeMillis()) {
			return 1.0 + (funcOpt.get().level() / 100.0);
		}
		return 1.0;
	}

	/**
	 * Retorna porcentagem de EXP recuperada ao ressuscitar no Clan Hall (e.g. 10%, 20%, 30%, 40%, 50%).
	 */
	public int getExpRestorePercent(PlayerCharacter player, int clanHallId) {
		if (player == null || player.clanId() <= 0 || clanHallId <= 0) {
			return 0;
		}
		var hallOpt = clanHallService.getClanHall(clanHallId);
		if (hallOpt.isEmpty() || hallOpt.get().ownerId() != player.clanId()) {
			return 0;
		}
		var funcOpt = getFunction(clanHallId, ClanHallFunctionRecord.FUNC_RESTORE_EXP);
		if (funcOpt.isPresent() && funcOpt.get().endTime() > System.currentTimeMillis()) {
			return funcOpt.get().level();
		}
		return 0;
	}

	/**
	 * Retorna lista de IDs de skills de buff de suporte conforme o nivel da funcao do Clan Hall.
	 */
	public List<Integer> getAvailableSupportBuffSkills(int supportLevel) {
		return switch (supportLevel) {
			case 1 -> List.of(4342, 4343, 4344); // Wind Walk, Shield, Might
			case 2 -> List.of(4342, 4343, 4344, 4345, 4346); // + Bless the Body, Bless the Soul
			case 3 -> List.of(4342, 4343, 4344, 4345, 4346, 4347, 4348); // + Concentration, Acumen
			case 4 -> List.of(4342, 4343, 4344, 4345, 4346, 4347, 4348, 4349, 4350); // + Berserker Spirit, Empower
			case 5, 6, 7, 8 -> List.of(4342, 4343, 4344, 4345, 4346, 4347, 4348, 4349, 4350, 4351, 4352, 4353); // Full Support
			default -> Collections.emptyList();
		};
	}

	private void loadFunctions() {
		if (jdbc == null) {
			return;
		}
		try {
			var rows = jdbc.sql("""
					SELECT hall_id, type, lvl, lease, rate, endTime
					FROM clanhall_functions
					""").query().listOfRows();

			for (var row : rows) {
				int hallId = ((Number) row.get("hall_id")).intValue();
				int type = ((Number) row.get("type")).intValue();
				int lvl = ((Number) row.get("lvl")).intValue();
				int lease = ((Number) row.get("lease")).intValue();
				long rate = ((Number) row.get("rate")).longValue();
				long endTime = ((Number) row.get("endTime")).longValue();

				var record = new ClanHallFunctionRecord(hallId, type, lvl, lease, rate, endTime);
				hallFunctions.computeIfAbsent(hallId, k -> new ConcurrentHashMap<>()).put(type, record);
			}
			log.info("ClanHallFunctionService: {} funcoes de clanhalls carregadas", rows.size());
		} catch (Exception ex) {
			log.warn("Nao foi possivel carregar funcoes de clanhalls: {}", ex.getMessage());
		}
	}
}
