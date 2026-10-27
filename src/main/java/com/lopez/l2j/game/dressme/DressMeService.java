package com.lopez.l2j.game.dressme;

import com.lopez.l2j.game.model.PlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Servico para gerenciamento das skins do sistema DressMe (Item 30 do Roteiro Mestre).
 * Permite que jogadores apliquem ou removam aparencias customizadas de armaduras e armas.
 */
@Service
public class DressMeService {

	private static final Logger log = LoggerFactory.getLogger(DressMeService.class);

	private final DressMeData data;

	@org.springframework.beans.factory.annotation.Autowired(required = false)
	private com.lopez.l2j.game.service.CharacterVariablesService variablesService;

	public DressMeService(DressMeData data) {
		this(data, null);
	}

	@org.springframework.beans.factory.annotation.Autowired
	public DressMeService(DressMeData data,
			@org.springframework.beans.factory.annotation.Autowired(required = false) com.lopez.l2j.game.service.CharacterVariablesService variablesService) {
		this.data = data;
		this.variablesService = variablesService;
	}

	public DressMeData data() {
		return data;
	}

	/**
	 * Aplica a skin associada ao skillId no personagem.
	 */
	public boolean applySkin(PlayerCharacter player, int skillId) {
		if (player == null) {
			return false;
		}

		var entryOpt = data.get(skillId);
		if (entryOpt.isEmpty()) {
			return false;
		}

		DressMeEntry entry = entryOpt.get();
		if (entry.isArmor()) {
			player.dressMeArmor(entry);
			if (variablesService != null) {
				variablesService.setVar(player.objectId(), "DRESSME_ARMOR", String.valueOf(skillId));
			}
		} else if (entry.isWeapon()) {
			player.dressMeWeapon(entry);
			if (variablesService != null) {
				variablesService.setVar(player.objectId(), "DRESSME_WEAPON", String.valueOf(skillId));
			}
		}

		player.dressMeEnabled(true);
		if (variablesService != null) {
			variablesService.setVar(player.objectId(), "DRESSME_ENABLED", "true");
		}
		player.sendUserInfoAndBroadcastCharInfo();
		log.debug("DressMe: Skin '{}' (skillId {}) aplicada a {}", entry.name(), skillId, player.name());
		return true;
	}

	/**
	 * Remove a skin de armadura/capa.
	 */
	public void removeArmorSkin(PlayerCharacter player) {
		if (player == null) {
			return;
		}
		player.dressMeArmor(null);
		if (variablesService != null) {
			variablesService.removeVar(player.objectId(), "DRESSME_ARMOR");
		}
		if (player.dressMeWeapon() == null) {
			player.dressMeEnabled(false);
			if (variablesService != null) {
				variablesService.setVar(player.objectId(), "DRESSME_ENABLED", "false");
			}
		}
		player.sendUserInfoAndBroadcastCharInfo();
	}

	/**
	 * Remove a skin de arma.
	 */
	public void removeWeaponSkin(PlayerCharacter player) {
		if (player == null) {
			return;
		}
		player.dressMeWeapon(null);
		if (variablesService != null) {
			variablesService.removeVar(player.objectId(), "DRESSME_WEAPON");
		}
		if (player.dressMeArmor() == null) {
			player.dressMeEnabled(false);
			if (variablesService != null) {
				variablesService.setVar(player.objectId(), "DRESSME_ENABLED", "false");
			}
		}
		player.sendUserInfoAndBroadcastCharInfo();
	}

	/**
	 * Alterna a ativacao visual do DressMe.
	 */
	public boolean toggle(PlayerCharacter player) {
		if (player == null) {
			return false;
		}
		boolean newState = !player.isDressMe();
		player.dressMeEnabled(newState);
		if (variablesService != null) {
			variablesService.setVar(player.objectId(), "DRESSME_ENABLED", String.valueOf(newState));
		}
		player.sendUserInfoAndBroadcastCharInfo();
		return newState;
	}

	/**
	 * Restaura as skins salvas de um personagem ao entrar no mundo.
	 */
	public void restoreSkins(PlayerCharacter player) {
		if (player == null || variablesService == null) {
			return;
		}
		int armorSkill = variablesService.getInt(player.objectId(), "DRESSME_ARMOR", 0);
		if (armorSkill > 0) {
			data.get(armorSkill).ifPresent(player::dressMeArmor);
		}
		int weaponSkill = variablesService.getInt(player.objectId(), "DRESSME_WEAPON", 0);
		if (weaponSkill > 0) {
			data.get(weaponSkill).ifPresent(player::dressMeWeapon);
		}
		boolean enabled = variablesService.getBoolean(player.objectId(), "DRESSME_ENABLED",
				player.dressMeArmor() != null || player.dressMeWeapon() != null);
		player.dressMeEnabled(enabled);
	}
}
