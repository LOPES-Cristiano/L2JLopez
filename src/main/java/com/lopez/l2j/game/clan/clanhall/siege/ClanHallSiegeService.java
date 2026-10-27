package com.lopez.l2j.game.clan.clanhall.siege;

import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.clan.ClanTable;
import com.lopez.l2j.game.clan.clanhall.ClanHallService;
import com.lopez.l2j.game.clan.clanhall.siege.ClanHallSiegeRecord.SiegeState;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
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

	// Bosses de Clan Halls Contestaveis (#399)
	public static final int NURKA_NPC_ID = 35368;            // Hall 21: Fortress of Resistance
	public static final int DIETRICH_NPC_ID = 35408;         // Hall 34: Devastated Castle
	public static final int MIKHAIL_NPC_ID = 35409;          // Hall 34: Devastated Castle
	public static final int GUSTAV_NPC_ID = 35410;           // Hall 34: Devastated Castle
	public static final int LIDIA_VON_HELLMANN_NPC_ID = 35629; // Hall 64: Fortress of the Dead
	public static final int ALFRED_VON_HELLMANN_NPC_ID = 35630;
	public static final int GISELLE_VON_HELLMANN_NPC_ID = 35631;

	// Itens de qualificacao e decretos (#400, #401)
	public static final int TARLK_AMULET_ITEM_ID = 5009;     // Hall 35: Bandit Stronghold
	public static final int BEAST_FARM_PASS_ITEM_ID = 8093;  // Hall 63: Wild Beast Reserve
	public static final int WAR_DECREE_ITEM_ID = 8034;       // Hall 62: Rainbow Springs Chateau
	public static final int DEFAULT_GOURD_MAX_HP = 50_000;

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
	private GameWorld world;

	public ClanHallSiegeService(ClanHallService clanHallService, ClanTable clanTable, JdbcClient jdbc) {
		this(clanHallService, clanTable, jdbc, null);
	}

	@Autowired
	public ClanHallSiegeService(ClanHallService clanHallService, ClanTable clanTable,
			@Autowired(required = false) JdbcClient jdbc,
			@Autowired(required = false) GameWorld world) {
		this.clanHallService = clanHallService;
		this.clanTable = clanTable;
		this.jdbc = jdbc;
		this.world = world;
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
		saveClanHallAttacker(hallId, clan.clanId(), 0);
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
			deleteClanHallAttacker(hallId, leader.clanId());
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
		if (world != null) {
			world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Clan Hall Siege",
					"A batalha pelo Clan Hall " + siege.name() + " comecou!"));
		}
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
			if (clanHallService != null) {
				clanHallService.setOwner(hallId, winnerClanId);
			}
			String winnerName = clanTable != null
					? clanTable.byClanId(winnerClanId).map(Clan::name).orElse("Cla #" + winnerClanId)
					: ("Cla #" + winnerClanId);
			if (world != null) {
				world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Clan Hall Siege",
						"A siege do Clan Hall " + siege.name() + " terminou! O cla " + winnerName + " conquistou a posse!"));
			}
			log.info("Siege do ClanHall ID {} encerrada. Vencedor: Cla ID {} ({})", hallId, winnerClanId, winnerName);
		} else {
			if (world != null) {
				world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Clan Hall Siege",
						"A siege do Clan Hall " + siege.name() + " terminou sem vencedor."));
			}
			log.info("Siege do ClanHall ID {} encerrada sem vencedor", hallId);
		}

		// Agenda proxima siege
		long nextDate = System.currentTimeMillis() + SIEGE_INTERVAL_MILLIS;
		siege.siegeDate(nextDate);
		siege.clearRegisteredClans();
		siege.state(SiegeState.REGISTRATION);
		clearClanHallAttackers(hallId);

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
				jdbc.sql("""
						CREATE TABLE IF NOT EXISTS clanhall_siege_attackers (
							hall_id INT NOT NULL,
							clan_id INT NOT NULL,
							decrees INT DEFAULT 0,
							PRIMARY KEY (hall_id, clan_id)
						)
						""").update();
			} catch (Exception ex) {
				log.debug("Nao foi possivel criar tabela clanhall_siege_attackers: {}", ex.getMessage());
			}
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
					loadClanHallAttackers();
					return;
				}
			} catch (Exception ex) {
				log.warn("Nao foi possivel carregar clanhall_siege: {}", ex.getMessage());
			}
		}

		// Sieges oficiais de Clan Halls contestaveis
		populateDefaultSieges();
		loadClanHallAttackers();
	}

	private void loadClanHallAttackers() {
		if (jdbc == null) {
			return;
		}
		try {
			var rows = jdbc.sql("SELECT hall_id, clan_id, decrees FROM clanhall_siege_attackers").query().listOfRows();
			for (var row : rows) {
				int hallId = ((Number) row.get("hall_id")).intValue();
				int clanId = ((Number) row.get("clan_id")).intValue();
				int decrees = row.get("decrees") != null ? ((Number) row.get("decrees")).intValue() : 0;
				ClanHallSiegeRecord siege = sieges.get(hallId);
				if (siege != null) {
					siege.registerClan(clanId);
					if (decrees > 0) {
						siege.warDecreesByClanId().put(clanId, decrees);
						siege.gourdHpByClanId().putIfAbsent(clanId, DEFAULT_GOURD_MAX_HP);
					}
				}
			}
		} catch (Exception ex) {
			log.debug("Nao foi possivel carregar clanhall_siege_attackers: {}", ex.getMessage());
		}
	}

	private void saveClanHallAttacker(int hallId, int clanId, int decrees) {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("DELETE FROM clanhall_siege_attackers WHERE hall_id = :hid AND clan_id = :cid")
					.param("hid", hallId)
					.param("cid", clanId)
					.update();
			jdbc.sql("INSERT INTO clanhall_siege_attackers (hall_id, clan_id, decrees) VALUES (:hid, :cid, :dec)")
					.param("hid", hallId)
					.param("cid", clanId)
					.param("dec", decrees)
					.update();
		} catch (Exception ex) {
			log.debug("Erro ao persistir clanhall_siege_attackers: {}", ex.getMessage());
		}
	}

	private void deleteClanHallAttacker(int hallId, int clanId) {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("DELETE FROM clanhall_siege_attackers WHERE hall_id = :hid AND clan_id = :cid")
					.param("hid", hallId)
					.param("cid", clanId)
					.update();
		} catch (Exception ex) {
			log.debug("Erro ao remover de clanhall_siege_attackers: {}", ex.getMessage());
		}
	}

	private void clearClanHallAttackers(int hallId) {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("DELETE FROM clanhall_siege_attackers WHERE hall_id = :hid")
					.param("hid", hallId)
					.update();
		} catch (Exception ex) {
			log.debug("Erro ao limpar clanhall_siege_attackers do hall {}: {}", hallId, ex.getMessage());
		}
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

	/**
	 * Contabiliza dano causado por um cla nos Bosses de Clan Hall (Nurka 35368, Gustav 35410, Lidia 35629) (#399).
	 */
	public synchronized void recordBossDamage(int hallId, int clanId, int npcId, long damage) {
		if (clanId <= 0 || damage <= 0) {
			return;
		}
		ClanHallSiegeRecord siege = sieges.get(hallId);
		if (siege == null || siege.state() != SiegeState.IN_PROGRESS) {
			return;
		}
		var clanOpt = clanTable.byClanId(clanId);
		if (clanOpt.isEmpty() || clanOpt.get().level() < MIN_CLAN_LEVEL_FOR_SIEGE) {
			return;
		}
		siege.addClanDamage(clanId, damage);
	}

	/**
	 * Processa a morte do Boss principal de um Clan Hall (Nurka 35368, Gustav 35410, Lidia 35629)
	 * e concede a posse ao cla elegivel com maior dano acumulado (#399).
	 */
	public synchronized boolean onSiegeBossKilled(int hallId, int npcId) {
		ClanHallSiegeRecord siege = sieges.get(hallId);
		if (siege == null || siege.state() != SiegeState.IN_PROGRESS) {
			return false;
		}
		boolean isFinalBoss = (hallId == 21 && npcId == NURKA_NPC_ID)
				|| (hallId == 34 && npcId == GUSTAV_NPC_ID)
				|| (hallId == 64 && npcId == LIDIA_VON_HELLMANN_NPC_ID);
		if (!isFinalBoss) {
			return false;
		}
		Integer winnerClanId = null;
		long bestDamage = 0L;
		for (var entry : siege.damageByClanId().entrySet()) {
			int cid = entry.getKey();
			long dmg = entry.getValue();
			if ((hallId == 21 || siege.isRegistered(cid)) && dmg > bestDamage) {
				bestDamage = dmg;
				winnerClanId = cid;
			}
		}
		return endSiege(hallId, winnerClanId);
	}

	/**
	 * Seleciona a bandeira de cla (35423..35427) e o NPC aliado (35428..35432) para Bandit Stronghold (35) e Beast Farm (63) (#400).
	 */
	public synchronized boolean selectTeamFlagAndAlly(int hallId, int clanId, int flagNpcId, int allyNpcId) {
		if (hallId != 35 && hallId != 63) {
			return false;
		}
		ClanHallSiegeRecord siege = sieges.get(hallId);
		if (siege == null || !siege.isRegistered(clanId)) {
			return false;
		}
		if (flagNpcId >= 35423 && flagNpcId <= 35427) {
			siege.clanFlagNpcIds().put(clanId, flagNpcId);
		}
		if (allyNpcId >= 35428 && allyNpcId <= 35432) {
			siege.clanAllyNpcIds().put(clanId, allyNpcId);
		}
		return true;
	}

	/**
	 * Elimina a bandeira de um cla em Bandit Stronghold (35) ou Wild Beast Reserve (63).
	 * Quando resta apenas 1 cla com bandeira de pe, declara-o vencedor (#400).
	 */
	public synchronized boolean onTeamFlagDestroyed(int hallId, int defeatedClanId) {
		if (hallId != 35 && hallId != 63) {
			return false;
		}
		ClanHallSiegeRecord siege = sieges.get(hallId);
		if (siege == null || siege.state() != SiegeState.IN_PROGRESS) {
			return false;
		}
		siege.clanFlagNpcIds().remove(defeatedClanId);
		siege.unregisterClan(defeatedClanId);
		deleteClanHallAttacker(hallId, defeatedClanId);
		if (siege.registeredClanIds().size() == 1) {
			Integer lastClanId = siege.registeredClanIds().iterator().next();
			return endSiege(hallId, lastClanId);
		}
		return true;
	}

	/**
	 * Registra um cla em Rainbow Springs Chateau (62) mediante entrega de War Decrees (Item ID 8034) (#401).
	 */
	public synchronized RegisterResult registerRainbowSpringsWithDecrees(PlayerCharacter leader, int decreeCount) {
		if (leader == null || leader.clanId() <= 0 || decreeCount <= 0) {
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
		ClanHallSiegeRecord siege = sieges.get(62);
		if (siege == null || siege.state() != SiegeState.REGISTRATION) {
			return RegisterResult.SIEGE_NOT_IN_REGISTRATION;
		}
		if (leader.inventory() == null || leader.inventory().getItemCount(WAR_DECREE_ITEM_ID) < decreeCount) {
			return RegisterResult.ERROR;
		}
		leader.inventory().destroyItemByItemId(WAR_DECREE_ITEM_ID, decreeCount);
		siege.warDecreesByClanId().merge(clan.clanId(), decreeCount, Integer::sum);

		// Mantem ate 4 clas classificados pelos maiores lances de War Decrees
		var top4 = siege.warDecreesByClanId().entrySet().stream()
				.sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
				.limit(4)
				.map(Map.Entry::getKey)
				.toList();
		for (Integer existing : new java.util.ArrayList<>(siege.registeredClanIds())) {
			if (!top4.contains(existing)) {
				siege.unregisterClan(existing);
				deleteClanHallAttacker(62, existing);
			}
		}
		for (Integer qualified : top4) {
			siege.registerClan(qualified);
			siege.gourdHpByClanId().putIfAbsent(qualified, DEFAULT_GOURD_MAX_HP);
			int totalDecrees = siege.warDecreesByClanId().getOrDefault(qualified, 0);
			saveClanHallAttacker(62, qualified, totalDecrees);
		}
		return RegisterResult.SUCCESS;
	}

	/**
	 * Aplica dano ao Gourd (NPC 35588/35589) de um cla durante o mini-game de Rainbow Springs Chateau (62).
	 * O primeiro cla a zerar o HP de seu Gourd vence o Clan Hall (#401).
	 */
	public synchronized boolean damageRainbowGourd(int clanId, int damage) {
		ClanHallSiegeRecord siege = sieges.get(62);
		if (siege == null || siege.state() != SiegeState.IN_PROGRESS || !siege.isRegistered(clanId) || damage <= 0) {
			return false;
		}
		int currentHp = siege.gourdHpByClanId().getOrDefault(clanId, DEFAULT_GOURD_MAX_HP);
		int newHp = Math.max(0, currentHp - damage);
		siege.gourdHpByClanId().put(clanId, newHp);
		if (newHp == 0) {
			return endSiege(62, clanId);
		}
		return true;
	}
}
