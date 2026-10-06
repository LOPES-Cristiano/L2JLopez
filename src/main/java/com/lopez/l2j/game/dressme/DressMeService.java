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

	public DressMeService(DressMeData data) {
		this.data = data;
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
		} else if (entry.isWeapon()) {
			player.dressMeWeapon(entry);
		}

		player.dressMeEnabled(true);
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
		if (player.dressMeWeapon() == null) {
			player.dressMeEnabled(false);
		}
	}

	/**
	 * Remove a skin de arma.
	 */
	public void removeWeaponSkin(PlayerCharacter player) {
		if (player == null) {
			return;
		}
		player.dressMeWeapon(null);
		if (player.dressMeArmor() == null) {
			player.dressMeEnabled(false);
		}
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
		return newState;
	}
}
