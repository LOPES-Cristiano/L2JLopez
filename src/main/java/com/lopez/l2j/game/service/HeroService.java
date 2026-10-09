package com.lopez.l2j.game.service;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.skill.SkillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.List;

/**
 * Servico para gerenciamento do status Heroi (Hero) com duracao temporal
 * e persistencia na tabela `character_herolist`.
 */
@Service
public class HeroService {

	private static final Logger log = LoggerFactory.getLogger(HeroService.class);

	// Skills exclusivas de Heroi no Lineage 2 Interlude
	private static final List<Integer> HERO_SKILL_IDS = List.of(395, 396, 1374, 1375, 1376);

	private final JdbcTemplate jdbc;
	private final SkillService skillService;

	public HeroService() {
		this(null, null);
	}

	public HeroService(@Autowired(required = false) DataSource dataSource,
					   @Autowired(required = false) SkillService skillService) {
		this.jdbc = dataSource != null ? new JdbcTemplate(dataSource) : null;
		this.skillService = skillService;
		ensureTable();
	}

	private void ensureTable() {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.execute("""
					CREATE TABLE IF NOT EXISTS character_herolist (
					  charId INT NOT NULL,
					  enddate DECIMAL(20,0) NOT NULL DEFAULT 0,
					  PRIMARY KEY (charId)
					)
					""");
		} catch (Exception e) {
			log.warn("HeroService: Nao foi possivel verificar/criar character_herolist: {}", e.getMessage());
		}
	}

	/**
	 * Concede status Hero a um jogador online. Se ja possuir tempo restante de Hero,
	 * os novos dias sao somados ao prazo atual.
	 *
	 * @param player jogador alvo
	 * @param days   dias adicionais (se <= 0, tempo permanente)
	 * @return timestamp de expiracao em milissegundos
	 */
	public long setHero(PlayerCharacter player, int days) {
		if (player == null) {
			return 0L;
		}
		long now = System.currentTimeMillis();
		long newExpiration;
		if (days > 0) {
			long currentEnd = player.heroExpiration();
			long baseTime = (player.isHero() && currentEnd > now) ? currentEnd : now;
			newExpiration = baseTime + (days * 86_400_000L);
		} else {
			newExpiration = 0L;
		}

		player.setHero(true);
		player.heroExpiration(newExpiration);
		saveHero(player.objectId(), newExpiration);
		rewardHeroSkills(player);

		log.info("HeroService: Status Hero concedido ao jogador {} [{}] por {} dias (expira em {})",
				player.getName(), player.objectId(), days, newExpiration);
		return newExpiration;
	}

	/**
	 * Concede status Hero a um personagem offline pelo ID. Soma com o tempo restante se ja tiver.
	 */
	public long setHero(int charId, int days) {
		long now = System.currentTimeMillis();
		long currentEnd = getHeroExpiration(charId);
		long newExpiration;
		if (days > 0) {
			long baseTime = (currentEnd > now) ? currentEnd : now;
			newExpiration = baseTime + (days * 86_400_000L);
		} else {
			newExpiration = 0L;
		}

		saveHero(charId, newExpiration);
		log.info("HeroService: Status Hero concedido ao charId={} offline por {} dias (expira em {})",
				charId, days, newExpiration);
		return newExpiration;
	}

	/**
	 * Remove status Hero de um jogador online.
	 */
	public void removeHero(PlayerCharacter player) {
		if (player == null) {
			return;
		}
		player.setHero(false);
		player.heroExpiration(0L);
		deleteHero(player.objectId());
		removeHeroSkills(player);
		log.info("HeroService: Status Hero removido do jogador {} [{}]", player.getName(), player.objectId());
	}

	/**
	 * Remove status Hero de um personagem offline pelo ID.
	 */
	public void removeHero(int charId) {
		deleteHero(charId);
		log.info("HeroService: Status Hero removido do charId={} offline", charId);
	}

	/**
	 * Salva na tabela character_herolist.
	 */
	public void saveHero(int charId, long enddate) {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.update("DELETE FROM character_herolist WHERE charId = ?", charId);
			jdbc.update("INSERT INTO character_herolist (charId, enddate) VALUES (?, ?)", charId, enddate);
		} catch (Exception e) {
			log.error("HeroService: Erro ao persistir hero em character_herolist para charId={}: {}",
					charId, e.getMessage());
		}
	}

	/**
	 * Remove da tabela character_herolist.
	 */
	public void deleteHero(int charId) {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.update("DELETE FROM character_herolist WHERE charId = ?", charId);
		} catch (Exception e) {
			log.error("HeroService: Erro ao deletar hero de character_herolist para charId={}: {}",
					charId, e.getMessage());
		}
	}

	/**
	 * Consulta a expiracao salva em character_herolist. Retorna -1 se nao encontrado.
	 */
	public long getHeroExpiration(int charId) {
		if (jdbc == null) {
			return -1L;
		}
		try {
			List<Long> results = jdbc.query("SELECT enddate FROM character_herolist WHERE charId = ?",
					(rs, rowNum) -> rs.getLong("enddate"), charId);
			return results.isEmpty() ? -1L : results.get(0);
		} catch (Exception e) {
			log.warn("HeroService: Erro ao consultar hero para charId={}: {}", charId, e.getMessage());
			return -1L;
		}
	}

	/**
	 * Carrega o status de Heroi quando o jogador entra no mundo (onEnterWorld).
	 */
	public void loadHero(PlayerCharacter player) {
		if (player == null) {
			return;
		}
		long enddate = getHeroExpiration(player.objectId());
		long now = System.currentTimeMillis();
		if (enddate >= 0) {
			if (enddate == 0 || enddate > now) {
				player.setHero(true);
				player.heroExpiration(enddate);
				rewardHeroSkills(player);
			} else {
				// Ja expirou enquanto estava offline
				deleteHero(player.objectId());
				player.setHero(false);
				player.heroExpiration(0L);
				removeHeroSkills(player);
			}
		}
	}

	public boolean isExpired(PlayerCharacter player) {
		if (player == null || !player.isHero()) {
			return false;
		}
		long exp = player.heroExpiration();
		return exp > 0 && exp <= System.currentTimeMillis();
	}

	/**
	 * Concede as skills exclusivas de heroi.
	 */
	public void rewardHeroSkills(PlayerCharacter player) {
		if (player == null || skillService == null) {
			return;
		}
		for (int skillId : HERO_SKILL_IDS) {
			skillService.addSkill(player, skillId, 1);
		}
	}

	/**
	 * Remove as skills exclusivas de heroi.
	 */
	public void removeHeroSkills(PlayerCharacter player) {
		if (player == null || skillService == null) {
			return;
		}
		for (int skillId : HERO_SKILL_IDS) {
			skillService.removeSkill(player, skillId);
		}
	}
}
