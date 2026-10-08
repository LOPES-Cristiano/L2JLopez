package com.lopez.l2j.game.castle.siege;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.castle.Castle;
import com.lopez.l2j.game.castle.CastleManager;
import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.clan.ClanTable;
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
 * Servico de gerenciamento de cercos aos castelos (Siege Engine de Lineage II Interlude).
 * Gerencia inscricoes de atacantes e defensores, artefatos, mid-victory e troca de posse.
 */
@Service
public class SiegeService {

	private static final Logger log = LoggerFactory.getLogger(SiegeService.class);

	public static final int MIN_CLAN_LEVEL = 4;
	public static final int MIN_CLAN_MEMBERS = 15;
	public static final long SIEGE_DURATION_MILLIS = 2 * 60 * 60 * 1000L; // 2 horas
	public static final long SIEGE_CYCLE_MILLIS = 14 * 24 * 60 * 60 * 1000L; // 14 dias

	private final Map<Integer, SiegeRecord> sieges = new ConcurrentHashMap<>();
	private final CastleManager castleManager;
	private final ClanTable clanTable;
	private final JdbcClient jdbc;

	@Autowired
	public SiegeService(CastleManager castleManager,
						@Autowired(required = false) ClanTable clanTable,
						@Autowired(required = false) JdbcClient jdbc) {
		this.castleManager = castleManager;
		this.clanTable = clanTable;
		this.jdbc = jdbc;
		initSieges();
		loadSiegeClans();
	}

	private void initSieges() {
		for (Castle castle : castleManager.allCastles()) {
			long sDate = castle.siegeDate() > 0 ? castle.siegeDate() : System.currentTimeMillis() + SIEGE_CYCLE_MILLIS;
			SiegeRecord record = new SiegeRecord(castle.id(), sDate);
			if (castle.hasOwner()) {
				record.defenders().put(castle.ownerClanId(), new SiegeClan(castle.id(), castle.ownerClanId(), SiegeClan.SiegeClanType.OWNER, true));
			}
			sieges.put(castle.id(), record);
		}
	}

	public Optional<SiegeRecord> getSiege(int castleId) {
		return Optional.ofNullable(sieges.get(castleId));
	}

	public Collection<SiegeRecord> allSieges() {
		return Collections.unmodifiableCollection(sieges.values());
	}

	/**
	 * Inscreve um cla como atacante no cerco do castelo.
	 */
	public boolean registerAttacker(int castleId, Clan clan) {
		if (clan == null) {
			return false;
		}

		SiegeRecord siege = sieges.get(castleId);
		if (siege == null || siege.isRegistrationOver() || siege.status() == SiegeStatus.IN_PROGRESS) {
			return false;
		}

		int minLvl = Config.SIEGE_CLAN_MIN_LEVEL > 0 ? Config.SIEGE_CLAN_MIN_LEVEL : MIN_CLAN_LEVEL;
		int minMembers = Config.SIEGE_CLAN_MIN_MEMBERS_COUNT > 0 ? Config.SIEGE_CLAN_MIN_MEMBERS_COUNT : MIN_CLAN_MEMBERS;
		if (clan.level() < minLvl || clan.membersCount() < minMembers) {
			return false;
		}

		if (Config.ATTACKER_MAX_CLANS > 0 && siege.attackers().size() >= Config.ATTACKER_MAX_CLANS) {
			log.warn("Cerco ao Castelo {}: limite maximo de atacantes ({}) atingido", castleId, Config.ATTACKER_MAX_CLANS);
			return false;
		}

		// Nao pode possuir outro castelo
		if (clan.castleId() > 0) {
			return false;
		}

		// Nao pode atacar o proprio castelo ou da mesma alianca
		Castle castle = castleManager.getCastleById(castleId).orElse(null);
		if (castle != null && castle.hasOwner()) {
			Clan ownerClan = clanTable != null ? clanTable.byClanId(castle.ownerClanId()).orElse(null) : null;
			if (ownerClan != null && ownerClan.allyId() > 0 && ownerClan.allyId() == clan.allyId()) {
				return false;
			}
		}

		SiegeClan sc = new SiegeClan(castleId, clan.clanId(), SiegeClan.SiegeClanType.ATTACKER, false);
		siege.attackers().put(clan.clanId(), sc);
		saveSiegeClan(sc);
		log.info("Cla {} ({}) registrado como atacante no Castelo {}", clan.name(), clan.clanId(), castleId);
		return true;
	}

	/**
	 * Inscreve um cla como defensor pendente de aprovacao pelo lorde do castelo.
	 */
	public boolean registerDefender(int castleId, Clan clan) {
		if (clan == null) {
			return false;
		}

		SiegeRecord siege = sieges.get(castleId);
		if (siege == null || siege.isRegistrationOver() || siege.status() == SiegeStatus.IN_PROGRESS) {
			return false;
		}

		Castle castle = castleManager.getCastleById(castleId).orElse(null);
		if (castle == null || !castle.hasOwner()) {
			return false; // Sem lorde, nao ha quem defenda alem de mercenarios/NPCs
		}

		int minLvl = Config.SIEGE_CLAN_MIN_LEVEL > 0 ? Config.SIEGE_CLAN_MIN_LEVEL : MIN_CLAN_LEVEL;
		if (clan.level() < minLvl) {
			return false;
		}

		if (Config.DEFENDER_MAX_CLANS > 0 && (siege.defenders().size() + siege.waitingDefenders().size()) >= Config.DEFENDER_MAX_CLANS) {
			log.warn("Cerco ao Castelo {}: limite maximo de defensores ({}) atingido", castleId, Config.DEFENDER_MAX_CLANS);
			return false;
		}

		SiegeClan sc = new SiegeClan(castleId, clan.clanId(), SiegeClan.SiegeClanType.DEFENDER_NOT_APPROVED, false);
		siege.waitingDefenders().put(clan.clanId(), sc);
		saveSiegeClan(sc);
		log.info("Cla {} ({}) solicitou inscricao na defesa do Castelo {}", clan.name(), clan.clanId(), castleId);
		return true;
	}

	/**
	 * O lorde do castelo aprova a participacao de um cla defensor.
	 */
	public boolean approveDefender(int castleId, int clanId, int requestingClanId) {
		Castle castle = castleManager.getCastleById(castleId).orElse(null);
		if (castle == null || castle.ownerClanId() != requestingClanId) {
			return false;
		}

		SiegeRecord siege = sieges.get(castleId);
		if (siege == null) {
			return false;
		}

		SiegeClan waiting = siege.waitingDefenders().remove(clanId);
		if (waiting != null) {
			waiting.type(SiegeClan.SiegeClanType.DEFENDER);
			siege.defenders().put(clanId, waiting);
			saveSiegeClan(waiting);
			log.info("Lorde aprovou o cla {} como defensor do Castelo {}", clanId, castleId);
			return true;
		}
		return false;
	}

	/**
	 * Inicia a batalha de cerco.
	 */
	public boolean startSiege(int castleId) {
		SiegeRecord siege = sieges.get(castleId);
		if (siege == null || siege.status() == SiegeStatus.IN_PROGRESS) {
			return false;
		}

		siege.status(SiegeStatus.IN_PROGRESS);
		siege.registrationOver(true);
		siege.controlTowersActive(3);
		siege.flameTowersActive(2);
		log.info("Cerco ao Castelo {} iniciado com sucesso!", castleId);
		return true;
	}

	/**
	 * Processa a gravacao do artefato / Mid-Victory (Seal of Ruler).
	 * A posse do castelo e transferida temporariamente durante o cerco.
	 */
	public boolean midVictory(int castleId, int newOwnerClanId) {
		SiegeRecord siege = sieges.get(castleId);
		if (siege == null || siege.status() != SiegeStatus.IN_PROGRESS) {
			return false;
		}

		SiegeClan newOwnerSc = siege.attackers().remove(newOwnerClanId);
		if (newOwnerSc == null) {
			return false;
		}

		// Antigos defensores viram atacantes
		for (SiegeClan def : siege.defenders().values()) {
			def.type(SiegeClan.SiegeClanType.ATTACKER);
			def.isCastleOwner(false);
			siege.attackers().put(def.clanId(), def);
			saveSiegeClan(def);
		}
		siege.defenders().clear();

		// Novo cla vira dono e defensor
		newOwnerSc.type(SiegeClan.SiegeClanType.OWNER);
		newOwnerSc.isCastleOwner(true);
		siege.defenders().put(newOwnerClanId, newOwnerSc);
		saveSiegeClan(newOwnerSc);

		castleManager.setOwner(castleId, newOwnerClanId);

		// Se nao sobrou nenhum outro atacante, cerco encerra imediatamente
		if (siege.attackers().isEmpty()) {
			endSiege(castleId, newOwnerClanId);
		}

		log.info("Mid-Victory no Castelo {}: Nova posse temporaria pertence ao Cla {}", castleId, newOwnerClanId);
		return true;
	}

	/**
	 * Conclui o cerco e define o vencedor final.
	 */
	public boolean endSiege(int castleId, int winnerClanId) {
		SiegeRecord siege = sieges.get(castleId);
		if (siege == null) {
			return false;
		}

		siege.status(SiegeStatus.FINISHED);
		siege.registrationOver(false);

		Castle castle = castleManager.getCastleById(castleId).orElse(null);
		if (castle != null && winnerClanId > 0) {
			boolean defended = castle.hasOwner() && castle.ownerClanId() == winnerClanId;
			castleManager.setOwner(castleId, winnerClanId);

			if (clanTable != null) {
				Clan winnerClan = clanTable.byClanId(winnerClanId).orElse(null);
				if (winnerClan != null) {
					clanTable.updateClanReputation(winnerClanId, winnerClan.reputationScore() + 1000);
					if (defended && Config.BLOOD_ALLIANCE_REWARD > 0) {
						log.info("Cerco ao Castelo {}: Defesa bem-sucedida! Cla {} premiado com {} Blood Alliance",
								castleId, winnerClanId, Config.BLOOD_ALLIANCE_REWARD);
					}
				}
			}
		}

		// Reagenda cerco para 14 dias no futuro
		long nextSiege = System.currentTimeMillis() + SIEGE_CYCLE_MILLIS;
		siege.siegeDate(nextSiege);
		if (castle != null) {
			castle.siegeDate(nextSiege);
		}

		clearSiegeClans(castleId);
		log.info("Cerco ao Castelo {} finalizado! Vencedor: Cla {}", castleId, winnerClanId);
		return true;
	}

	public boolean isAttacker(int castleId, int clanId) {
		SiegeRecord s = sieges.get(castleId);
		return s != null && s.attackers().containsKey(clanId);
	}

	public boolean isDefender(int castleId, int clanId) {
		SiegeRecord s = sieges.get(castleId);
		return s != null && s.defenders().containsKey(clanId);
	}

	public boolean isParticipant(int castleId, int clanId) {
		return isAttacker(castleId, clanId) || isDefender(castleId, clanId);
	}

	private void saveSiegeClan(SiegeClan sc) {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("""
					REPLACE INTO siege_clans (castle_id, clan_id, type, castle_owner)
					VALUES (:castleId, :clanId, :type, :owner)
					""")
					.param("castleId", sc.castleId())
					.param("clanId", sc.clanId())
					.param("type", sc.type().id())
					.param("owner", sc.isCastleOwner() ? 1 : 0)
					.update();
		} catch (Exception ex) {
			log.warn("Erro ao salvar participante de cerco na tabela siege_clans: {}", ex.getMessage());
		}
	}

	private void clearSiegeClans(int castleId) {
		SiegeRecord siege = sieges.get(castleId);
		if (siege != null) {
			siege.attackers().clear();
			siege.waitingDefenders().clear();
			// Mantem apenas o dono atual na lista de defensores
			Castle castle = castleManager.getCastleById(castleId).orElse(null);
			siege.defenders().clear();
			if (castle != null && castle.hasOwner()) {
				SiegeClan owner = new SiegeClan(castleId, castle.ownerClanId(), SiegeClan.SiegeClanType.OWNER, true);
				siege.defenders().put(castle.ownerClanId(), owner);
				saveSiegeClan(owner);
			}
		}

		if (jdbc != null) {
			try {
				jdbc.sql("DELETE FROM siege_clans WHERE castle_id = :cid AND castle_owner = 0")
						.param("cid", castleId)
						.update();
			} catch (Exception ex) {
				log.warn("Erro ao limpar siege_clans do castelo {}: {}", castleId, ex.getMessage());
			}
		}
	}

	private void loadSiegeClans() {
		if (jdbc == null) {
			return;
		}
		try {
			List<Map<String, Object>> rows = jdbc.sql("""
					SELECT castle_id, clan_id, type, castle_owner
					FROM siege_clans
					""").query().listOfRows();

			for (var row : rows) {
				int castleId = ((Number) row.get("castle_id")).intValue();
				int clanId = ((Number) row.get("clan_id")).intValue();
				int typeId = ((Number) row.get("type")).intValue();
				boolean isOwner = row.get("castle_owner") != null && ((Number) row.get("castle_owner")).intValue() == 1;

				SiegeRecord siege = sieges.get(castleId);
				if (siege != null) {
					SiegeClan sc = new SiegeClan(castleId, clanId, SiegeClan.SiegeClanType.fromId(typeId), isOwner);
					switch (sc.type()) {
						case ATTACKER -> siege.attackers().put(clanId, sc);
						case DEFENDER, OWNER -> siege.defenders().put(clanId, sc);
						case DEFENDER_NOT_APPROVED -> siege.waitingDefenders().put(clanId, sc);
					}
				}
			}
			log.info("SiegeService: participantes de cercos carregados do banco");
		} catch (Exception ex) {
			log.warn("Nao foi possivel carregar siege_clans: {}. Mantendo defaults.", ex.getMessage());
		}
	}
}
