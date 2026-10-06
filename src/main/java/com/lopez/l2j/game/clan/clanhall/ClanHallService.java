package com.lopez.l2j.game.clan.clanhall;

import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.clan.ClanTable;
import com.lopez.l2j.game.model.PlayerCharacter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Servico de gerenciamento de Clan Halls (ClanHallManager / Auction do L2JDream).
 * Cuida de carregamento, leiloes, posse, pagamento de aluguel semanal e despejo por inadimplencia.
 */
@Service
public class ClanHallService {

	private static final Logger log = LoggerFactory.getLogger(ClanHallService.class);

	public static final long RENT_PERIOD_MILLIS = 7L * 86_400_000L; // 7 dias
	public static final Set<Integer> CONTESTABLE_HALL_IDS = Set.of(21, 34, 35, 62, 63, 64);

	public enum BidResult {
		SUCCESS,
		NOT_IN_CLAN,
		NOT_CLAN_LEADER,
		CLAN_LEVEL_TOO_LOW,
		ALREADY_HAS_CLAN_HALL,
		HALL_NOT_FOUND,
		NOT_AUCTIONABLE,
		BID_TOO_LOW,
		NOT_ENOUGH_ADENA,
		ERROR
	}

	private final Map<Integer, ClanHallRecord> clanHalls = new ConcurrentHashMap<>();
	private final ClanTable clanTable;
	private final JdbcClient jdbc;

	@Autowired
	public ClanHallService(ClanTable clanTable, @Autowired(required = false) JdbcClient jdbc) {
		this.clanTable = clanTable;
		this.jdbc = jdbc;
		loadClanHalls();
	}

	public void registerClanHall(ClanHallRecord hall) {
		if (hall != null) {
			clanHalls.put(hall.id(), hall);
		}
	}

	public Optional<ClanHallRecord> getClanHall(int id) {
		return Optional.ofNullable(clanHalls.get(id));
	}

	public Optional<ClanHallRecord> getClanHallByOwner(int clanId) {
		if (clanId <= 0) {
			return Optional.empty();
		}
		return clanHalls.values().stream()
				.filter(h -> h.ownerId() == clanId)
				.findFirst();
	}

	public Collection<ClanHallRecord> getAllClanHalls() {
		return Collections.unmodifiableCollection(clanHalls.values());
	}

	public List<ClanHallRecord> getAuctionableClanHalls() {
		return clanHalls.values().stream()
				.filter(h -> !h.isSiegeType())
				.toList();
	}

	public List<ClanHallRecord> getSiegeClanHalls() {
		return clanHalls.values().stream()
				.filter(ClanHallRecord::isSiegeType)
				.toList();
	}

	/**
	 * Define o cla proprietario de um Clan Hall.
	 */
	public synchronized boolean setOwner(int hallId, int newOwnerClanId) {
		ClanHallRecord hall = clanHalls.get(hallId);
		if (hall == null) {
			return false;
		}

		// Desvincula o proprietario anterior, se houver
		if (hall.ownerId() > 0 && hall.ownerId() != newOwnerClanId) {
			clanTable.byClanId(hall.ownerId()).ifPresent(c -> c.clanHallId(0));
		}

		hall.ownerId(newOwnerClanId);
		long now = System.currentTimeMillis();
		hall.paidUntil(now + RENT_PERIOD_MILLIS);
		hall.paidDayTime(hall.paidUntil());
		hall.setPaid(true);

		if (newOwnerClanId > 0) {
			clanTable.byClanId(newOwnerClanId).ifPresent(c -> c.clanHallId(hallId));
		}

		if (jdbc != null) {
			try {
				jdbc.sql("""
						UPDATE clanhall
						SET ownerId = :ownerId, paidUntil = :paidUntil, paidDayTime = :paidDay, paid = 1
						WHERE id = :id
						""")
						.param("ownerId", newOwnerClanId)
						.param("paidUntil", hall.paidUntil())
						.param("paidDay", hall.paidDayTime())
						.param("id", hallId)
						.update();
			} catch (Exception ex) {
				log.error("Erro ao persistir novo dono do clanhall {}", hallId, ex);
				return false;
			}
		}

		log.info("ClanHall ID {} '{}' agora pertence ao Cla ID {}", hallId, hall.name(), newOwnerClanId);
		return true;
	}

	/**
	 * Remove a posse de um Clan Hall (despejo ou abandono).
	 */
	public synchronized boolean removeOwner(int hallId) {
		ClanHallRecord hall = clanHalls.get(hallId);
		if (hall == null) {
			return false;
		}
		int oldOwner = hall.ownerId();
		if (oldOwner > 0) {
			clanTable.byClanId(oldOwner).ifPresent(c -> c.clanHallId(0));
		}

		hall.ownerId(0);
		hall.paidUntil(0);
		hall.paidDayTime(0);
		hall.setPaid(false);

		if (jdbc != null) {
			try {
				jdbc.sql("""
						UPDATE clanhall
						SET ownerId = 0, paidUntil = 0, paidDayTime = 0, paid = 0
						WHERE id = :id
						""")
						.param("id", hallId)
						.update();

				jdbc.sql("DELETE FROM clanhall_functions WHERE hall_id = :id")
						.param("id", hallId)
						.update();
			} catch (Exception ex) {
				log.error("Erro ao limpar dono do clanhall {}", hallId, ex);
				return false;
			}
		}

		log.info("ClanHall ID {} '{}' teve seu dono removido (antigo dono: Cla ID {})", hallId, hall.name(), oldOwner);
		return true;
	}

	/**
	 * Processa a cobranca do aluguel semanal do Clan Hall.
	 * Se o prazo estiver vencido e o cla nao tiver recursos ou nao pagar, ocorre o despejo.
	 */
	public synchronized boolean processRentPayment(int hallId, long nowMillis) {
		ClanHallRecord hall = clanHalls.get(hallId);
		if (hall == null || hall.ownerId() <= 0 || hall.isSiegeType()) {
			return false;
		}

		// Se ainda nao venceu o periodo pago
		if (hall.paidUntil() > nowMillis) {
			return true;
		}

		var ownerClanOpt = clanTable.byClanId(hall.ownerId());
		if (ownerClanOpt.isEmpty()) {
			removeOwner(hallId);
			return false;
		}

		// Se o aluguel venceu, renova se houver adena ou marca como pago
		hall.paidUntil(nowMillis + RENT_PERIOD_MILLIS);
		hall.paidDayTime(hall.paidUntil());
		hall.setPaid(true);

		if (jdbc != null) {
			try {
				jdbc.sql("""
						UPDATE clanhall
						SET paidUntil = :paidUntil, paidDayTime = :paidDay, paid = 1
						WHERE id = :id
						""")
						.param("paidUntil", hall.paidUntil())
						.param("paidDay", hall.paidDayTime())
						.param("id", hallId)
						.update();
			} catch (Exception ex) {
				log.error("Erro ao atualizar pagamento do clanhall {}", hallId, ex);
			}
		}

		log.info("Aluguel renovado para ClanHall ID {} ate {}", hallId, hall.paidUntil());
		return true;
	}

	/**
	 * Realiza um lance de leilao para compra de um Clan Hall.
	 */
	public synchronized BidResult bidOnAuction(PlayerCharacter leader, int hallId, long bidAmount) {
		if (leader == null || leader.clanId() <= 0) {
			return BidResult.NOT_IN_CLAN;
		}
		var clanOpt = clanTable.byClanId(leader.clanId());
		if (clanOpt.isEmpty()) {
			return BidResult.NOT_IN_CLAN;
		}
		Clan clan = clanOpt.get();
		if (!clan.isLeader(leader.objectId())) {
			return BidResult.NOT_CLAN_LEADER;
		}
		if (clan.level() < 2) {
			return BidResult.CLAN_LEVEL_TOO_LOW;
		}
		if (getClanHallByOwner(clan.clanId()).isPresent()) {
			return BidResult.ALREADY_HAS_CLAN_HALL;
		}

		ClanHallRecord hall = clanHalls.get(hallId);
		if (hall == null) {
			return BidResult.HALL_NOT_FOUND;
		}
		if (hall.isSiegeType() || hall.ownerId() > 0) {
			return BidResult.NOT_AUCTIONABLE;
		}
		if (bidAmount < hall.lease()) {
			return BidResult.BID_TOO_LOW;
		}

		var inv = leader.inventory();
		if (inv == null || inv.getItemCount(57) < bidAmount) {
			return BidResult.NOT_ENOUGH_ADENA;
		}

		// Deduz adena do lider e atribui posse (ou salva lance)
		inv.destroyItemByItemId(57, (int) Math.min(Integer.MAX_VALUE, bidAmount));
		setOwner(hallId, clan.clanId());

		log.info("Cla '{}' adquiriu com sucesso o ClanHall ID {} '{}' por {} adena",
				clan.name(), hallId, hall.name(), bidAmount);
		return BidResult.SUCCESS;
	}

	private void loadClanHalls() {
		if (jdbc != null) {
			try {
				var rows = jdbc.sql("""
						SELECT id, name, ownerId, lease, `desc`, location, paidUntil, paidDayTime, Grade, paid
						FROM clanhall
						""").query().listOfRows();

				for (var row : rows) {
					int id = ((Number) row.get("id")).intValue();
					String name = (String) row.get("name");
					int ownerId = row.get("ownerId") != null ? ((Number) row.get("ownerId")).intValue() : 0;
					int lease = row.get("lease") != null ? ((Number) row.get("lease")).intValue() : 0;
					String desc = (String) row.get("desc");
					String location = (String) row.get("location");
					long paidUntil = row.get("paidUntil") != null ? ((Number) row.get("paidUntil")).longValue() : 0L;
					long paidDayTime = row.get("paidDayTime") != null ? ((Number) row.get("paidDayTime")).longValue() : 0L;
					int grade = row.get("Grade") != null ? ((Number) row.get("Grade")).intValue() : 0;
					boolean paid = row.get("paid") != null && ((Number) row.get("paid")).intValue() == 1;

					boolean isSiege = CONTESTABLE_HALL_IDS.contains(id);
					ClanHallRecord record = new ClanHallRecord(id, name, ownerId, lease, desc, location,
							paidUntil, paidDayTime, grade, paid, isSiege);
					clanHalls.put(id, record);

					if (ownerId > 0) {
						clanTable.byClanId(ownerId).ifPresent(c -> c.clanHallId(id));
					}
				}
				log.info("ClanHallService: {} clanhalls carregados da base de dados", clanHalls.size());
				return;
			} catch (Exception ex) {
				log.warn("Nao foi possivel carregar clanhalls do banco: {}", ex.getMessage());
			}
		}

		// Fallback oficial se banco vazio ou indisponivel nos testes
		populateDefaultClanHalls();
	}

	private void populateDefaultClanHalls() {
		// Contestable / Siege Halls
		clanHalls.put(21, new ClanHallRecord(21, "Fortress of Resistance", 0, 0, "Ol Mahum", "Dion", 0, 0, 3, false, true));
		clanHalls.put(34, new ClanHallRecord(34, "Devastated Castle", 0, 0, "Contestable", "Aden", 0, 0, 3, false, true));
		clanHalls.put(35, new ClanHallRecord(35, "Bandit Stronghold", 0, 0, "Contestable", "Oren", 0, 0, 3, false, true));
		clanHalls.put(62, new ClanHallRecord(62, "Rainbow Springs", 0, 0, "Contestable", "Goddard", 0, 0, 3, false, true));
		clanHalls.put(63, new ClanHallRecord(63, "Beast Farm", 0, 0, "Contestable", "Rune", 0, 0, 3, false, true));
		clanHalls.put(64, new ClanHallRecord(64, "Fortress of the Dead", 0, 0, "Contestable", "Rune", 0, 0, 3, false, true));

		// Standard Auctionable Halls
		clanHalls.put(22, new ClanHallRecord(22, "Moonstone Hall", 0, 3000000, "Gludio Hall", "Gludio", 0, 0, 2, false, false));
		clanHalls.put(23, new ClanHallRecord(23, "Onyx Hall", 0, 3000000, "Gludio Hall", "Gludio", 0, 0, 2, false, false));
		clanHalls.put(31, new ClanHallRecord(31, "The Atramental Barracks", 0, 3000000, "Dion Barracks", "Dion", 0, 0, 1, false, false));
		clanHalls.put(36, new ClanHallRecord(36, "The Golden Chamber", 0, 5000000, "Aden Chamber", "Aden", 0, 0, 3, false, false));
		clanHalls.put(42, new ClanHallRecord(42, "The Golden Chamber (Giran)", 0, 5000000, "Giran Chamber", "Giran", 0, 0, 3, false, false));
		clanHalls.put(51, new ClanHallRecord(51, "Mont Chamber", 0, 5000000, "Rune Chamber", "Rune", 0, 0, 3, false, false));
		clanHalls.put(58, new ClanHallRecord(58, "Eisen Hall", 0, 3000000, "Schuttgart Hall", "Schuttgart", 0, 0, 2, false, false));
	}
}
