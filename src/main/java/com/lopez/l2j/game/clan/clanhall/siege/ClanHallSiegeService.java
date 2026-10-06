package com.lopez.l2j.game.clan.clanhall.siege;

import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.clan.ClanTable;
import com.lopez.l2j.game.clan.clanhall.ClanHallService;
import com.lopez.l2j.game.clan.clanhall.siege.ClanHallSiegeRecord.SiegeState;
import com.lopez.l2j.game.model.PlayerCharacter;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Servico de sieges de Clan Halls contestaveis (Devastated Castle, Bandit Stronghold, Rainbow Springs etc.).
 * Cuida do registro de clas atacantes, ciclo de vida da batalha e atribuicao de vitoria/posse.
 */
@Service
public class ClanHallSiegeService {

	private static final Logger log = LoggerFactory.getLogger(ClanHallSiegeService.class);

	public static final int MIN_CLAN_LEVEL_FOR_SIEGE = 4;
	public static final int MAX_REGISTERED_CLANS = 5;
	public static final long SIEGE_INTERVAL_MILLIS = 7L * 86_400_000L; // 7 dias

	public enum RegisterResult {
		SUCCESS,
		NOT_IN_CLAN,
		NOT_CLAN_LEADER,
		CLAN_LEVEL_TOO_LOW,
		ALREADY_HAS_CLAN_HALL,
		ALREADY_REGISTERED,
		MAX_CLANS_REACHED,
		SIEGE_NOT_IN_REGISTRATION,
		SIEGE_NOT_FOUND,
		ERROR
	}

	private final Map<Integer, ClanHallSiegeRecord> sieges = new ConcurrentHashMap<>();
	private final ClanHallService clanHallService;
	private final ClanTable clanTable;
	private final JdbcClient jdbc;

	@Autowired
	public ClanHallSiegeService(ClanHallService clanHallService, ClanTable clanTable,
			@Autowired(required = false) JdbcClient jdbc) {
		this.clanHallService = clanHallService;
		this.clanTable = clanTable;
		this.jdbc = jdbc;
		loadSieges();
	}

	public Optional<ClanHallSiegeRecord> getSiege(int hallId) {
		return Optional.ofNullable(sieges.get(hallId));
	}

	public Collection<ClanHallSiegeRecord> getAllSieges() {
		return Collections.unmodifiableCollection(sieges.values());
	}

	public boolean isUnderSiege(int hallId) {
		var s = sieges.get(hallId);
		return s != null && s.state() == SiegeState.IN_PROGRESS;
	}

	/**
	 * Inscreve um cla para disputar a siege do Clan Hall contestavel.
	 */
	public synchronized RegisterResult registerClan(PlayerCharacter leader, int hallId) {
		if (leader == null || leader.clanId() <= 0) {
			return RegisterResult.NOT_IN_CLAN;
		}
		var clanOpt = clanTable.byClanId(leader.clanId());
		if (clanOpt.isEmpty()) {
			return RegisterResult.NOT_IN_CLAN;
		}
		Clan clan = clanOpt.get();
		if (!clan.isLeader(leader.objectId())) {
			return RegisterResult.NOT_CLAN_LEADER;
		}
		if (clan.level() < MIN_CLAN_LEVEL_FOR_SIEGE) {
			return RegisterResult.CLAN_LEVEL_TOO_LOW;
		}
		if (clanHallService.getClanHallByOwner(clan.clanId()).isPresent()) {
			return RegisterResult.ALREADY_HAS_CLAN_HALL;
		}

		ClanHallSiegeRecord siege = sieges.get(hallId);
		if (siege == null) {
			return RegisterResult.SIEGE_NOT_FOUND;
		}
		if (siege.state() != SiegeState.REGISTRATION) {
			return RegisterResult.SIEGE_NOT_IN_REGISTRATION;
		}
		if (siege.isRegistered(clan.clanId())) {
			return RegisterResult.ALREADY_REGISTERED;
		}
		if (siege.registeredClanIds().size() >= MAX_REGISTERED_CLANS) {
			return RegisterResult.MAX_CLANS_REACHED;
		}

		siege.registerClan(clan.clanId());
		log.info("Cla '{}' registrado com sucesso para a siege de ClanHall ID {} '{}'",
				clan.name(), hallId, siege.name());
		return RegisterResult.SUCCESS;
	}

	/**
	 * Cancela a inscricao do cla na siege.
	 */
	public synchronized boolean unregisterClan(PlayerCharacter leader, int hallId) {
		if (leader == null || leader.clanId() <= 0) {
			return false;
		}
		ClanHallSiegeRecord siege = sieges.get(hallId);
		if (siege == null || siege.state() != SiegeState.REGISTRATION) {
			return false;
		}
		boolean removed = siege.unregisterClan(leader.clanId());
		if (removed) {
			log.info("Cla ID {} cancelou inscricao na siege de ClanHall ID {}", leader.clanId(), hallId);
		}
		return removed;
	}

	/**
	 * Inicia a batalha da siege.
	 */
	public synchronized boolean startSiege(int hallId) {
		ClanHallSiegeRecord siege = sieges.get(hallId);
		if (siege == null || siege.state() == SiegeState.IN_PROGRESS) {
			return false;
		}

		siege.state(SiegeState.IN_PROGRESS);
		log.info("Siege do ClanHall ID {} '{}' iniciada com {} clas inscritos",
				hallId, siege.name(), siege.registeredClanIds().size());
		return true;
	}

	/**
	 * Finaliza a siege e premia o cla vencedor, agendando a proxima data.
	 */
	public synchronized boolean endSiege(int hallId, Integer winnerClanId) {
		ClanHallSiegeRecord siege = sieges.get(hallId);
		if (siege == null) {
			return false;
		}

		siege.state(SiegeState.FINISHED);

		if (winnerClanId != null && winnerClanId > 0) {
			clanHallService.setOwner(hallId, winnerClanId);
			log.info("Siege do ClanHall ID {} encerrada. Vencedor: Cla ID {}", hallId, winnerClanId);
		} else {
			log.info("Siege do ClanHall ID {} encerrada sem vencedor", hallId);
		}

		// Agenda proxima siege
		long nextDate = System.currentTimeMillis() + SIEGE_INTERVAL_MILLIS;
		siege.siegeDate(nextDate);
		siege.clearRegisteredClans();
		siege.state(SiegeState.REGISTRATION);

		if (jdbc != null) {
			try {
				jdbc.sql("""
						UPDATE clanhall_siege
						SET siege_data = :data
						WHERE id = :id
						""")
						.param("data", nextDate)
						.param("id", hallId)
						.update();
			} catch (Exception ex) {
				log.error("Erro ao persistir proxima data da siege do clanhall {}", hallId, ex);
			}
		}

		return true;
	}

	private void loadSieges() {
		if (jdbc != null) {
			try {
				var rows = jdbc.sql("SELECT id, name, siege_data FROM clanhall_siege").query().listOfRows();
				for (var row : rows) {
					int id = ((Number) row.get("id")).intValue();
					String name = (String) row.get("name");
					long date = row.get("siege_data") != null ? ((Number) row.get("siege_data")).longValue() : 0L;
					sieges.put(id, new ClanHallSiegeRecord(id, name, date, SiegeState.REGISTRATION));
				}
				if (!rows.isEmpty()) {
					log.info("ClanHallSiegeService: {} sieges carregadas do banco", sieges.size());
					return;
				}
			} catch (Exception ex) {
				log.warn("Nao foi possivel carregar clanhall_siege: {}", ex.getMessage());
			}
		}

		// Sieges oficiais de Clan Halls contestaveis
		populateDefaultSieges();
	}

	private void populateDefaultSieges() {
		long defaultNext = System.currentTimeMillis() + SIEGE_INTERVAL_MILLIS;
		sieges.put(21, new ClanHallSiegeRecord(21, "Fortress of Resistance", defaultNext, SiegeState.REGISTRATION));
		sieges.put(34, new ClanHallSiegeRecord(34, "Devastated Castle", defaultNext, SiegeState.REGISTRATION));
		sieges.put(35, new ClanHallSiegeRecord(35, "Bandit Stronghold", defaultNext, SiegeState.REGISTRATION));
		sieges.put(62, new ClanHallSiegeRecord(62, "Rainbow Springs", defaultNext, SiegeState.REGISTRATION));
		sieges.put(63, new ClanHallSiegeRecord(63, "Beast Farm", defaultNext, SiegeState.REGISTRATION));
		sieges.put(64, new ClanHallSiegeRecord(64, "Fortress of the Dead", defaultNext, SiegeState.REGISTRATION));
	}
}
