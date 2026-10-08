package com.lopez.l2j.game.phantom;

import com.lopez.l2j.game.model.PlayerCharacter;

import java.util.Random;

/**
 * Comportamento social e economico modular de Phantoms em vilas e cidades - Onda C8:
 * Navegacao e passeio em pracas publicas, abertura de lojas privadas (venda e compra).
 */
public class PhantomVillageBehavior {

	private static final Random RNG = new Random();

	private static final String[] DEFAULT_STORE_TITLES = {
			"Buying mats / adena",
			"Selling Enchant Scrolls",
			"Soul Crystals / Pots",
			"Top Weapons & Armor",
			"Buying Recipes & Parts",
			"Fast Trade Giran!"
	};

	/**
	 * Decide acao para o modo de loja privada em vila.
	 */
	public PhantomAction processTownStore(PlayerCharacter phantom, String customTitle) {
		if (phantom == null) {
			return PhantomAction.idle();
		}

		String title = (customTitle != null && !customTitle.isBlank())
				? customTitle
				: DEFAULT_STORE_TITLES[Math.abs(phantom.objectId()) % DEFAULT_STORE_TITLES.length];

		if (!phantom.sitting() || phantom.privateStoreType() == 0) {
			phantom.sitting(true);
			phantom.privateStoreType(1); // STORE_PRIVATE_SELL
			phantom.storeTitle(title);
			return PhantomAction.sitStore(title);
		}

		return PhantomAction.idle();
	}

	/**
	 * Decide proximo passo de movimentacao suave pela vila.
	 */
	public PhantomAction processTownRoam(PlayerCharacter phantom, int anchorX, int anchorY, int anchorZ, int roamRadius) {
		if (phantom == null) {
			return PhantomAction.idle();
		}

		// Se estava sentado em loja, levanta primeiro
		if (phantom.sitting()) {
			phantom.sitting(false);
			phantom.privateStoreType(0);
			phantom.storeTitle("");
			return PhantomAction.standUp();
		}

		// Gera um waypoint aleatorio ao redor do ponto de ancoragem da cidade
		double angle = RNG.nextDouble() * 2 * Math.PI;
		double distance = RNG.nextDouble() * roamRadius;
		int targetX = (int) Math.round(anchorX + distance * Math.cos(angle));
		int targetY = (int) Math.round(anchorY + distance * Math.sin(angle));

		phantom.x(targetX);
		phantom.y(targetY);
		phantom.z(anchorZ);

		return PhantomAction.moveTo(targetX, targetY, anchorZ);
	}
}
