package com.lopez.l2j.game.social;

import com.lopez.l2j.game.model.PlayerCharacter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Servico de casamento e pareamento de personagens (Item 29 do Roteiro Mestre).
 * Gerencia noivados, casamentos e divorcios com persistencia na tabela `couples`.
 */
@Service
public class WeddingService {

	private static final Logger log = LoggerFactory.getLogger(WeddingService.class);

	public record Couple(int id, int player1Id, int player2Id, boolean married, long affiancedDate, long weddingDate) {}

	private final JdbcClient jdbc;
	private final Map<Integer, Couple> couplesById = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> coupleIdByPlayer = new ConcurrentHashMap<>();

	public WeddingService(JdbcClient jdbc) {
		this.jdbc = jdbc;
		load();
	}

	public synchronized void load() {
		couplesById.clear();
		coupleIdByPlayer.clear();
		try {
			var list = jdbc.sql("SELECT id, player1Id, player2Id, maried, affiancedDate, weddingDate FROM couples")
					.query((rs, rowNum) -> new Couple(
							rs.getInt("id"),
							rs.getInt("player1Id"),
							rs.getInt("player2Id"),
							rs.getBoolean("maried"),
							rs.getLong("affiancedDate"),
							rs.getLong("weddingDate")
					))
					.list();
			for (Couple c : list) {
				couplesById.put(c.id(), c);
				coupleIdByPlayer.put(c.player1Id(), c.id());
				coupleIdByPlayer.put(c.player2Id(), c.id());
			}
			log.info("WeddingService: {} casais carregados do banco de dados.", list.size());
		} catch (Exception e) {
			log.warn("Erro ao carregar casais da tabela couples: {}", e.getMessage());
		}
	}

	public Optional<Couple> getCouple(int coupleId) {
		return Optional.ofNullable(couplesById.get(coupleId));
	}

	public Optional<Couple> getCoupleForPlayer(int playerId) {
		Integer id = coupleIdByPlayer.get(playerId);
		if (id == null) {
			return Optional.empty();
		}
		return getCouple(id);
	}

	public int getPartnerId(int playerId) {
		var coupleOpt = getCoupleForPlayer(playerId);
		if (coupleOpt.isEmpty()) {
			return 0;
		}
		var c = coupleOpt.get();
		return c.player1Id() == playerId ? c.player2Id() : c.player1Id();
	}

	public boolean isMarried(int playerId) {
		return getCoupleForPlayer(playerId).map(Couple::married).orElse(false);
	}

	/**
	 * Cria noivado entre dois jogadores solteiros.
	 */
	public synchronized Couple engage(PlayerCharacter player1, PlayerCharacter player2) {
		if (player1 == null || player2 == null || player1.objectId() == player2.objectId()) {
			throw new IllegalArgumentException("Personagens invalidos para casamento.");
		}
		if (coupleIdByPlayer.containsKey(player1.objectId()) || coupleIdByPlayer.containsKey(player2.objectId())) {
			throw new IllegalStateException("Um dos jogadores ja possui noivado ou casamento ativo.");
		}

		long now = System.currentTimeMillis();
		int nextId = couplesById.keySet().stream().max(Integer::compareTo).orElse(0) + 1;

		Couple couple = new Couple(nextId, player1.objectId(), player2.objectId(), false, now, 0);

		try {
			jdbc.sql("""
					INSERT INTO couples (id, player1Id, player2Id, maried, affiancedDate, weddingDate)
					VALUES (:id, :p1, :p2, 0, :ad, 0)
					""")
					.param("id", nextId)
					.param("p1", player1.objectId())
					.param("p2", player2.objectId())
					.param("ad", now)
					.update();
		} catch (Exception e) {
			log.warn("Erro ao registrar noivado no banco de dados: {}", e.getMessage());
		}

		couplesById.put(nextId, couple);
		coupleIdByPlayer.put(player1.objectId(), nextId);
		coupleIdByPlayer.put(player2.objectId(), nextId);

		return couple;
	}

	/**
	 * Conclui a cerimonia oficializando o matrimomio.
	 */
	public synchronized boolean marry(int coupleId) {
		Couple c = couplesById.get(coupleId);
		if (c == null || c.married()) {
			return false;
		}

		long now = System.currentTimeMillis();
		Couple updated = new Couple(c.id(), c.player1Id(), c.player2Id(), true, c.affiancedDate(), now);

		try {
			jdbc.sql("UPDATE couples SET maried = 1, weddingDate = :wd WHERE id = :id")
					.param("wd", now)
					.param("id", coupleId)
					.update();
		} catch (Exception e) {
			log.warn("Erro ao registrar casamento id {}: {}", coupleId, e.getMessage());
		}

		couplesById.put(coupleId, updated);
		return true;
	}

	/**
	 * Realiza o divorcio e dissolve o casal.
	 */
	public synchronized void divorce(int coupleId) {
		Couple c = couplesById.remove(coupleId);
		if (c != null) {
			coupleIdByPlayer.remove(c.player1Id());
			coupleIdByPlayer.remove(c.player2Id());

			try {
				jdbc.sql("DELETE FROM couples WHERE id = :id")
						.param("id", coupleId)
						.update();
			} catch (Exception e) {
				log.warn("Erro ao remover casal id {}: {}", coupleId, e.getMessage());
			}
		}
	}

	/**
	 * Verifica condicoes para teleporte do jogador ate seu conjuge.
	 */
	public boolean canTeleportToPartner(PlayerCharacter actor, PlayerCharacter partner) {
		if (actor == null || partner == null) {
			return false;
		}
		if (!isMarried(actor.objectId())) {
			return false;
		}
		if (getPartnerId(actor.objectId()) != partner.objectId()) {
			return false;
		}
		if (actor.isDead() || partner.isDead()) {
			return false;
		}
		if (actor.karma() > 0 || partner.karma() > 0) {
			return false;
		}
		return true;
	}
}
