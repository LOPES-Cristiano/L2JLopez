package com.lopez.l2j.game.service;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.List;

/**
 * Servico de gerenciamento de status VIP (Onda 14: add-on.properties e rates.properties).
 * Controla duracao, persistencia relacional (`characters.vip`, `characters.vip_end`),
 * taxas customizadas de XP/SP/Drop/Spoil e cores de nick/titulo.
 */
@Service
public class VipService {

	private static final Logger log = LoggerFactory.getLogger(VipService.class);

	private final JdbcTemplate jdbc;

	public VipService() {
		this(null);
	}

	public VipService(@Autowired(required = false) DataSource dataSource) {
		this.jdbc = dataSource != null ? new JdbcTemplate(dataSource) : null;
	}

	/**
	 * Concede status VIP a um jogador online. Se ja tiver tempo ativo,
	 * os novos dias sao adicionados a expiracao atual.
	 *
	 * @param player jogador alvo
	 * @param days   dias adicionais (se <= 0, tempo permanente)
	 * @return timestamp de expiracao em milissegundos
	 */
	public long setVip(PlayerCharacter player, int days) {
		if (player == null) {
			return 0L;
		}
		long now = System.currentTimeMillis();
		long expiration;
		if (days > 0) {
			long current = player.vipExpiration();
			long base = (player.isVip() && current > now) ? current : now;
			expiration = base + (days * 86_400_000L);
		} else {
			expiration = 0L;
		}

		player.setVip(true);
		player.vipExpiration(expiration);
		applyVipColors(player);
		saveVip(player.objectId(), 1, expiration);

		log.info("VipService: Status VIP ativado para {} [{}] por {} dias (expira em {})",
				player.getName(), player.objectId(), days, expiration);
		return expiration;
	}

	/**
	 * Concede status VIP a um personagem offline no banco de dados.
	 */
	public long setVip(int charId, int days) {
		long now = System.currentTimeMillis();
		long current = getVipExpiration(charId);
		long expiration;
		if (days > 0) {
			long base = (current > now) ? current : now;
			expiration = base + (days * 86_400_000L);
		} else {
			expiration = 0L;
		}

		saveVip(charId, 1, expiration);
		log.info("VipService: Status VIP offline ativado para charId={} por {} dias (expira em {})",
				charId, days, expiration);
		return expiration;
	}

	/**
	 * Remove status VIP de um jogador online e restaura as cores normais.
	 */
	public void removeVip(PlayerCharacter player) {
		if (player == null) {
			return;
		}
		player.setVip(false);
		player.vipExpiration(0L);
		player.nameColor(0xFFFFFF);
		player.titleColor(0xFFFF77);
		saveVip(player.objectId(), 0, 0L);
		log.info("VipService: Status VIP removido do jogador {} [{}]", player.getName(), player.objectId());
	}

	/**
	 * Remove status VIP de um personagem offline no banco.
	 */
	public void removeVip(int charId) {
		saveVip(charId, 0, 0L);
		log.info("VipService: Status VIP offline removido do charId={}", charId);
	}

	/**
	 * Persiste no banco de dados nas colunas vip e vip_end da tabela characters.
	 */
	public void saveVip(int charId, int vip, long expiration) {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.update("UPDATE characters SET vip = ?, vip_end = ? WHERE charId = ?", vip, expiration, charId);
		} catch (Exception e) {
			log.warn("VipService: Erro ao persistir VIP para charId={}: {}", charId, e.getMessage());
		}
	}

	/**
	 * Consulta o timestamp vip_end no banco de dados.
	 */
	public long getVipExpiration(int charId) {
		if (jdbc == null) {
			return 0L;
		}
		try {
			List<Long> results = jdbc.query("SELECT vip_end FROM characters WHERE charId = ? AND vip = 1",
					(rs, rowNum) -> rs.getLong("vip_end"), charId);
			return results.isEmpty() ? 0L : results.get(0);
		} catch (Exception e) {
			log.warn("VipService: Erro ao buscar vip_end para charId={}: {}", charId, e.getMessage());
			return 0L;
		}
	}

	/**
	 * Carrega e valida o status VIP do jogador ao entrar no mundo.
	 */
	public void loadVip(PlayerCharacter player) {
		if (player == null) {
			return;
		}
		if (player.isVip()) {
			long exp = player.vipExpiration();
			if (exp > 0 && exp <= System.currentTimeMillis()) {
				removeVip(player);
			} else {
				applyVipColors(player);
			}
		}
	}

	public void applyVipColors(PlayerCharacter player) {
		if (player == null) {
			return;
		}
		if (Config.ALLOW_VIP_NAME_COLOR && Config.VIP_NAME_COLOR != null && !Config.VIP_NAME_COLOR.isBlank()) {
			try {
				player.nameColor(Integer.decode("0x" + Config.VIP_NAME_COLOR));
			} catch (Exception e) {
				log.warn("VipService: Falha ao converter cor de nome VIP: {}", Config.VIP_NAME_COLOR);
			}
		}

		if (Config.ALLOW_VIP_TITLE_COLOR && Config.VIP_TITLE_COLOR != null && !Config.VIP_TITLE_COLOR.isBlank()) {
			try {
				player.titleColor(Integer.decode("0x" + Config.VIP_TITLE_COLOR));
			} catch (Exception e) {
				log.warn("VipService: Falha ao converter cor de titulo VIP: {}", Config.VIP_TITLE_COLOR);
			}
		}
	}

	public boolean isVip(PlayerCharacter player) {
		if (player == null || !player.isVip()) {
			return false;
		}
		long exp = player.vipExpiration();
		if (exp > 0 && exp <= System.currentTimeMillis()) {
			removeVip(player);
			return false;
		}
		return true;
	}

	public float getXpMultiplier(PlayerCharacter player) {
		return (isVip(player) && Config.ALLOW_VIP_XPSP) ? Config.VIP_XP : 1.0f;
	}

	public float getSpMultiplier(PlayerCharacter player) {
		return (isVip(player) && Config.ALLOW_VIP_XPSP) ? Config.VIP_SP : 1.0f;
	}

	public float getDropMultiplier(PlayerCharacter player) {
		return isVip(player) ? Config.VIP_DROP_RATE : 1.0f;
	}

	public float getSpoilMultiplier(PlayerCharacter player) {
		return isVip(player) ? Config.VIP_SPOIL_RATE : 1.0f;
	}

	public int getVipDaysByTier(int tier) {
		return switch (tier) {
			case 2 -> Config.VIP_DIAS_2;
			case 3 -> Config.VIP_DIAS_3;
			default -> Config.VIP_DIAS;
		};
	}
}
