package com.lopez.l2j.game.social;

import com.lopez.l2j.game.model.PlayerCharacter;
import java.util.Calendar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Servico de recomendacoes e avaliacoes de personagens (Item 27 do Roteiro Mestre).
 * Gerencia a pontuacao de recomendacoes (recomHave / recomLeft), reset diario as 13:00,
 * decaimento natural e persistencia nas tabelas `characters` e `character_recommends`.
 */
@Service
public class CharacterRecommendationService {

	private static final Logger log = LoggerFactory.getLogger(CharacterRecommendationService.class);

	private final JdbcClient jdbc;

	public CharacterRecommendationService(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public record EvaluationResult(boolean success, String message, int actorRecomLeft, int targetRecomHave) {
		public static EvaluationResult fail(String msg) {
			return new EvaluationResult(false, msg, 0, 0);
		}

		public static EvaluationResult ok(int actorRecomLeft, int targetRecomHave) {
			return new EvaluationResult(true, "OK", actorRecomLeft, targetRecomHave);
		}
	}

	/**
	 * Inicializa as recomendacoes de um personagem ao entrar no jogo.
	 */
	public void onPlayerEnter(PlayerCharacter player) {
		if (player == null) {
			return;
		}

		try {
			// Carrega quem ele ja recomendou no ciclo atual
			var targets = jdbc.sql("SELECT target_id FROM character_recommends WHERE charId = :id")
					.param("id", player.objectId())
					.query(Integer.class)
					.list();
			player.recommendedToday().addAll(targets);
		} catch (Exception e) {
			log.warn("Erro ao carregar recomendacoes para {}: {}", player.name(), e.getMessage());
		}

		checkDailyReset(player);
	}

	/**
	 * Verifica e aplica o reset diario de recomendacoes (padrao retail: 13:00 / 1:00 PM).
	 */
	public void checkDailyReset(PlayerCharacter player) {
		if (player == null || player.level() < 10) {
			return;
		}

		long last = player.lastRecomDate();
		long now = System.currentTimeMillis();

		if (last == 0) {
			resetRecommendations(player);
			return;
		}

		Calendar check = Calendar.getInstance();
		check.setTimeInMillis(last);
		check.add(Calendar.DAY_OF_MONTH, 1);
		check.set(Calendar.HOUR_OF_DAY, 13);
		check.set(Calendar.MINUTE, 0);
		check.set(Calendar.SECOND, 0);
		check.set(Calendar.MILLISECOND, 0);

		if (now >= check.getTimeInMillis()) {
			resetRecommendations(player);
		}
	}

	private void resetRecommendations(PlayerCharacter player) {
		int lvl = player.level();
		if (lvl < 20) {
			player.recomLeft(3);
			player.recomHave(player.recomHave() - 1);
		} else if (lvl < 40) {
			player.recomLeft(6);
			player.recomHave(player.recomHave() - 2);
		} else {
			player.recomLeft(20);
			player.recomHave(player.recomHave() - 3);
		}

		player.recommendedToday().clear();
		player.lastRecomDate(System.currentTimeMillis());

		try {
			jdbc.sql("DELETE FROM character_recommends WHERE charId = :id")
					.param("id", player.objectId())
					.update();
		} catch (Exception e) {
			log.warn("Erro ao limpar character_recommends para {}: {}", player.name(), e.getMessage());
		}
	}

	/**
	 * Avalia e concede uma recomendacao de `actor` para `target`.
	 */
	public EvaluationResult evaluate(PlayerCharacter actor, PlayerCharacter target) {
		if (actor == null || target == null) {
			return EvaluationResult.fail("Alvo incorreto.");
		}
		if (actor.objectId() == target.objectId()) {
			return EvaluationResult.fail("Voce nao pode recomendar a si mesmo.");
		}
		if (actor.level() < 10) {
			return EvaluationResult.fail("Apenas personagens nivel 10 ou superior podem recomendar.");
		}
		if (actor.recomLeft() <= 0) {
			return EvaluationResult.fail("Voce nao possui mais recomendacoes disponiveis hoje.");
		}
		if (target.recomHave() >= 255) {
			return EvaluationResult.fail("Seu alvo ja atingiu a quantidade maxima de recomendacoes (255).");
		}
		if (actor.recommendedToday().contains(target.objectId())) {
			return EvaluationResult.fail("Voce ja recomendou este personagem hoje.");
		}

		actor.recomLeft(actor.recomLeft() - 1);
		target.recomHave(target.recomHave() + 1);
		actor.recommendedToday().add(target.objectId());

		try {
			jdbc.sql("REPLACE INTO character_recommends (charId, target_id) VALUES (:c, :t)")
					.param("c", actor.objectId())
					.param("t", target.objectId())
					.update();
		} catch (Exception e) {
			log.warn("Erro ao salvar character_recommends entre {} e {}: {}", actor.name(), target.name(), e.getMessage());
		}

		return EvaluationResult.ok(actor.recomLeft(), target.recomHave());
	}

	/**
	 * Comando GM para definir pontuacao de recomendacoes.
	 */
	public void adminSetRec(PlayerCharacter target, int count) {
		if (target != null) {
			target.recomHave(count);
		}
	}
}
