package com.lopez.l2j.features;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Servico de gerenciamento de status VIP, Hero, AIOx, Castle Lord e Newbie no login
 * (add-on.properties).
 */
@Service("featureVipService")
public class VipService {

	private static final Logger log = LoggerFactory.getLogger(VipService.class);

	/**
	 * Coleta os anuncios globais de login habilitados em add-on.properties para o jogador.
	 */
	public List<String> collectLoginAnnouncements(PlayerCharacter player, boolean isNewbie) {
		List<String> messages = new ArrayList<>();
		if (player == null) {
			return messages;
		}
		if (Config.ANNOUNCE_HERO_LOGIN && player.isHero()) {
			messages.add("O Heroi " + player.name() + " acabou de logar!");
		}
		if (Config.ANNOUNCE_AIOX_LOGIN && player.isAio()) {
			messages.add("O jogador AIOx " + player.name() + " acabou de logar!");
		}
		if (Config.ANNOUNCE_VIP_LOGIN && player.isVip()) {
			messages.add("O jogador VIP " + player.name() + " acabou de logar!");
		}
		if (Config.ANNOUNCE_LORD_LOGIN && player.isClanLeader() && player.clanId() > 0) {
			var clan = player.getClan();
			if (clan == null || clan.hasCastle()) {
				String castleSuffix = (clan != null && clan.name() != null) ? " (Cla " + clan.name() + ")" : "";
				messages.add("O Lorde " + player.name() + castleSuffix + " acabou de logar!");
			}
		}
		if (Config.ANNOUNCE_NEWBIE_LOGIN && (isNewbie || player.level() == 1)) {
			messages.add("Bem-vindo ao servidor, novo aventureiro " + player.name() + "!");
		}
		for (String msg : messages) {
			log.info("[LoginAnnounce] {}", msg);
		}
		return messages;
	}

	public boolean isHeroSkillAllowedOnSubClass() {
		return Config.ALLOW_HERO_SKILL_ON_SUB;
	}

	public boolean canUseHeroSkills(PlayerCharacter player) {
		if (player == null || !player.isHero()) {
			return false;
		}
		if (player.isSubClassActive() && !Config.ALLOW_HERO_SKILL_ON_SUB) {
			return false;
		}
		return true;
	}
}
