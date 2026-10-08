package com.lopez.l2j.game.service;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Servico de gerenciamento de status VIP (Onda 14: add-on.properties e rates.properties).
 * Controla duracao, taxas customizadas de XP/SP/Drop/Spoil e cores de nick/titulo.
 */
@Service
public class VipService {

	private static final Logger log = LoggerFactory.getLogger(VipService.class);

	public void setVip(PlayerCharacter player, int days) {
		if (player == null) {
			return;
		}
		long now = System.currentTimeMillis();
		long expiration = days > 0 ? now + (days * 86_400_000L) : 0L;
		player.setVip(true);
		player.vipExpiration(expiration);

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

		log.info("VipService: Status VIP ativado para {} por {} dias (expira em {})", player.getName(), days, expiration);
	}

	public void removeVip(PlayerCharacter player) {
		if (player == null) {
			return;
		}
		player.setVip(false);
		player.vipExpiration(0L);
		log.info("VipService: Status VIP removido do jogador {}", player.getName());
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
