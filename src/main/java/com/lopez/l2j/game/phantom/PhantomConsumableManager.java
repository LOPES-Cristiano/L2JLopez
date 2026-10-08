package com.lopez.l2j.game.phantom;

import com.lopez.l2j.game.model.PlayerCharacter;

/**
 * Gerenciador modular de consumiveis (Pocoes de HP/MP e Soulshots/Spiritshots) - Onda C8.
 */
public class PhantomConsumableManager {

	public static final int GREATER_HEALING_POTION = 1061;
	public static final int MANA_POTION = 728;
	public static final int QUICK_HEALING_POTION = 1539;

	// Soulshots D, C, B, A, S
	public static final int SOULSHOT_D = 1463;
	public static final int SOULSHOT_C = 1464;
	public static final int SOULSHOT_B = 1465;
	public static final int SOULSHOT_A = 1466;
	public static final int SOULSHOT_S = 1467;

	// Spiritshots Blessed D, C, B, A, S
	public static final int BLESSED_SPIRITSHOT_D = 3948;
	public static final int BLESSED_SPIRITSHOT_C = 3949;
	public static final int BLESSED_SPIRITSHOT_B = 3950;
	public static final int BLESSED_SPIRITSHOT_A = 3951;
	public static final int BLESSED_SPIRITSHOT_S = 3952;

	public static final double HP_POTION_THRESHOLD = 0.60;
	public static final double MP_POTION_THRESHOLD = 0.35;

	/**
	 * Avalia se o personagem precisa ingerir pocao de HP ou MP.
	 */
	public PhantomAction checkPotions(PlayerCharacter player) {
		if (player == null || player.isDead()) {
			return null;
		}

		double hpRatio = player.currentHp() / Math.max(1.0, player.maxHp());
		if (hpRatio < HP_POTION_THRESHOLD) {
			return PhantomAction.usePotion(GREATER_HEALING_POTION);
		}

		double mpRatio = player.currentMp() / Math.max(1.0, player.maxMp());
		if (mpRatio < MP_POTION_THRESHOLD) {
			return PhantomAction.usePotion(MANA_POTION);
		}

		return null;
	}

	/**
	 * Retorna o ID apropriado de soulshot ou spiritshot de acordo com o kit do Phantom.
	 */
	public int getAppropriateShotId(PhantomCombatKit kit, int grade) {
		if (kit == PhantomCombatKit.MAGE_NUKE || kit == PhantomCombatKit.MAGE_DOT_CC || kit == PhantomCombatKit.HEALER_SUPPORT) {
			return BLESSED_SPIRITSHOT_S;
		}
		return SOULSHOT_S;
	}
}
