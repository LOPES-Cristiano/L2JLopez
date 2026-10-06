package com.lopez.l2j.game.fortress.siege;

import com.lopez.l2j.game.castle.siege.SiegeStatus;
import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.fortress.FortressRecord;
import com.lopez.l2j.game.fortress.FortressService;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemTemplate;
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
 * Servico do motor de cerco a fortalezas (FortSiegeManager de Lineage II Interlude).
 * Gerencia inscricao com taxa de adena, desligamento de reatores, derrota de comandantes e captura da bandeira de combate.
 */
@Service
public class FortressSiegeService {

	private static final Logger log = LoggerFactory.getLogger(FortressSiegeService.class);

	public static final int REGISTRATION_FEE_ADENA = 250_000;
	public static final int MIN_CLAN_LEVEL = 4;
	public static final long SIEGE_CYCLE_MILLIS = 7 * 24 * 60 * 60 * 1000L; // 7 dias

	private final Map<Integer, FortressSiegeRecord> sieges = new ConcurrentHashMap<>();
	private final FortressService fortressService;
	private final JdbcClient jdbc;

	@Autowired
	public FortressSiegeService(FortressService fortressService,
								@Autowired(required = false) JdbcClient jdbc) {
		this.fortressService = fortressService;
		this.jdbc = jdbc;
		initSieges();
		loadFortSiegeClans();
	}

	private void initSieges() {
		for (FortressRecord fort : fortressService.allFortresses()) {
			long sDate = fort.siegeDate() > 0 ? fort.siegeDate() : System.currentTimeMillis() + SIEGE_CYCLE_MILLIS;
			sieges.put(fort.id(), new FortressSiegeRecord(fort.id(), sDate));
		}
	}

	public Optional<FortressSiegeRecord> getSiege(int fortId) {
		return Optional.ofNullable(sieges.get(fortId));
	}

	public Collection<FortressSiegeRecord> allSieges() {
		return Collections.unmodifiableCollection(sieges.values());
	}

	/**
	 * Inscreve um cla no cerco a fortaleza mediante pagamento de taxa.
	 */
	public boolean registerAttacker(int fortId, PlayerCharacter player, Clan clan) {
		if (player == null || clan == null) {
			return false;
		}

		FortressSiegeRecord siege = sieges.get(fortId);
		if (siege == null || siege.status() != SiegeStatus.REGISTRATION) {
			return false;
		}

		if (clan.level() < MIN_CLAN_LEVEL) {
			log.warn("Cla {} nao possui nivel minimo ({}) para disputar fortaleza", clan.name(), MIN_CLAN_LEVEL);
			return false;
		}

		// Nao pode disputar a propria fortaleza
		FortressRecord fort = fortressService.getFortressById(fortId).orElse(null);
		if (fort != null && fort.ownerClanId() == clan.clanId()) {
			return false;
		}

		// Desconta a taxa de inscricao de Adena
		Inventory inv = player.inventory();
		if (inv != null) {
			var adenaOpt = inv.byItemId(ItemTemplate.ADENA_ID);
			if (adenaOpt.isEmpty() || adenaOpt.get().count() < REGISTRATION_FEE_ADENA) {
				log.warn("{} nao possui a taxa de {} adenas para registrar cerco de fortaleza",
						player.name(), REGISTRATION_FEE_ADENA);
				return false;
			}
			int currentCount = adenaOpt.get().count();
			if (currentCount == REGISTRATION_FEE_ADENA) {
				inv.remove(adenaOpt.get());
			} else {
				adenaOpt.get().count(currentCount - REGISTRATION_FEE_ADENA);
			}
		}

		siege.attackerClans().add(clan.clanId());
		saveSiegeClan(fortId, clan.clanId());
		log.info("Cla {} ({}) registrado no cerco da Fortaleza {} por {}",
				clan.name(), clan.clanId(), fortId, player.name());
		return true;
	}

	/**
	 * Inicia a batalha de cerco da fortaleza.
	 */
	public boolean startSiege(int fortId) {
		FortressSiegeRecord siege = sieges.get(fortId);
		if (siege == null || siege.status() == SiegeStatus.IN_PROGRESS) {
			return false;
		}

		siege.status(SiegeStatus.IN_PROGRESS);
		siege.reactorsDisabled(0);
		siege.commandersDefeated(0);
		siege.flagRaised(false);
		log.info("Cerco da Fortaleza {} iniciado!", fortId);
		return true;
	}

	/**
	 * Desativa um reator na sala de controle (Power Control Room).
	 */
	public void disableReactor(int fortId) {
		FortressSiegeRecord siege = sieges.get(fortId);
		if (siege != null && siege.status() == SiegeStatus.IN_PROGRESS) {
			siege.reactorsDisabled(siege.reactorsDisabled() + 1);
			log.info("Fortaleza {}: Reator desativado ({}/{})",
					fortId, siege.reactorsDisabled(), FortressSiegeRecord.TOTAL_REACTORS);
		}
	}

	/**
	 * Derrota um dos comandantes da fortaleza (Subunit Commander).
	 */
	public void defeatCommander(int fortId) {
		FortressSiegeRecord siege = sieges.get(fortId);
		if (siege != null && siege.status() == SiegeStatus.IN_PROGRESS) {
			siege.commandersDefeated(siege.commandersDefeated() + 1);
			log.info("Fortaleza {}: Comandante derrotado ({}/{})",
					fortId, siege.commandersDefeated(), FortressSiegeRecord.TOTAL_COMMANDERS);
		}
	}

	/**
	 * Ergue a bandeira de combate no mastro da fortaleza apos neutralizar as defesas.
	 */
	public boolean raiseCombatFlag(int fortId, int clanId) {
		FortressSiegeRecord siege = sieges.get(fortId);
		if (siege == null || siege.status() != SiegeStatus.IN_PROGRESS) {
			return false;
		}

		if (!siege.canRaiseFlag()) {
			log.warn("Nao e possivel erguer a bandeira: defesas da fortaleza ainda ativas (Reatores: {}/{}, Comandantes: {}/{})",
					siege.reactorsDisabled(), FortressSiegeRecord.TOTAL_REACTORS,
					siege.commandersDefeated(), FortressSiegeRecord.TOTAL_COMMANDERS);
			return false;
		}

		siege.flagRaised(true);
		siege.victoriousClanId(clanId);
		endSiege(fortId, clanId);
		log.info("Cla {} ergueu a bandeira de combate e conquistou a Fortaleza {}!", clanId, fortId);
		return true;
	}

	/**
	 * Finaliza o cerco e concede a fortaleza ao novo cla.
	 */
	public boolean endSiege(int fortId, int winnerClanId) {
		FortressSiegeRecord siege = sieges.get(fortId);
		if (siege == null) {
			return false;
		}

		siege.status(SiegeStatus.FINISHED);

		if (winnerClanId > 0) {
			fortressService.setOwner(fortId, winnerClanId);
			fortressService.setFortState(fortId, FortressRecord.STATE_NONE); // Permite escolher contrato ou independencia
		}

		// Reagenda cerco para 7 dias no futuro
		long nextSiege = System.currentTimeMillis() + SIEGE_CYCLE_MILLIS;
		siege.siegeDate(nextSiege);
		fortressService.getFortressById(fortId).ifPresent(f -> f.siegeDate(nextSiege));

		clearSiegeClans(fortId);
		log.info("Cerco da Fortaleza {} encerrado! Vencedor: Cla {}", fortId, winnerClanId);
		return true;
	}

	private void saveSiegeClan(int fortId, int clanId) {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("""
					REPLACE INTO fortsiege_clans (fort_id, clan_id)
					VALUES (:fid, :cid)
					""")
					.param("fid", fortId)
					.param("cid", clanId)
					.update();
		} catch (Exception ex) {
			log.warn("Erro ao salvar participante de cerco de fortaleza: {}", ex.getMessage());
		}
	}

	private void clearSiegeClans(int fortId) {
		FortressSiegeRecord siege = sieges.get(fortId);
		if (siege != null) {
			siege.attackerClans().clear();
		}
		if (jdbc != null) {
			try {
				jdbc.sql("DELETE FROM fortsiege_clans WHERE fort_id = :fid")
						.param("fid", fortId)
						.update();
			} catch (Exception ex) {
				log.warn("Erro ao limpar fortsiege_clans: {}", ex.getMessage());
			}
		}
	}

	private void loadFortSiegeClans() {
		if (jdbc == null) {
			return;
		}
		try {
			List<Map<String, Object>> rows = jdbc.sql("""
					SELECT fort_id, clan_id
					FROM fortsiege_clans
					""").query().listOfRows();

			for (var row : rows) {
				int fortId = ((Number) row.get("fort_id")).intValue();
				int clanId = ((Number) row.get("clan_id")).intValue();
				FortressSiegeRecord siege = sieges.get(fortId);
				if (siege != null) {
					siege.attackerClans().add(clanId);
				}
			}
			log.info("FortressSiegeService: participantes de cerco de fortaleza carregados");
		} catch (Exception ex) {
			log.warn("Nao foi possivel carregar fortsiege_clans: {}", ex.getMessage());
		}
	}
}
