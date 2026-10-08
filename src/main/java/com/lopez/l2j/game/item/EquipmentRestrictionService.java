package com.lopez.l2j.game.item;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;

/**
 * Servico de validacao de restricoes de equipamentos (armaduras, tipos de armas por classe
 * e checagem de nivel maximo de encantamento ao equipar).
 */
public final class EquipmentRestrictionService {

	private EquipmentRestrictionService() {
	}

	/**
	 * Verifica se o jogador possui permissao para equipar o item.
	 *
	 * @param player Jogador tentando equipar
	 * @param item Item que esta sendo equipado
	 * @return true se permitido, false se proibido pelas configuracoes do servidor
	 */
	public static boolean canEquip(PlayerCharacter player, ItemInstance item) {
		if (player == null || item == null) {
			return false;
		}

		// 1. Verificacao de nivel de encantamento maximo permitido para equipar
		if (Config.CHECK_ENCHANT_LEVEL_EQUIP && Config.ENCHANT_OVER_CHANT_CHECK > 0) {
			if (item.enchant() > Config.ENCHANT_OVER_CHANT_CHECK) {
				return false;
			}
		}

		ItemTemplate template = item.template();
		if (template == null) {
			return true;
		}

		int classId = player.classId();
		String subType = template.subType() != null ? template.subType().toLowerCase() : "";

		// 2. Restricoes de Armadura (Heavy e Light)
		if (template.kind() == ItemTemplate.Kind.ARMOR) {
			if ("heavy".equals(subType)) {
				if (!Config.ALLOW_LIGHT_USE_HEAVY && Config.NOT_ALLOWED_USE_HEAVY != null
						&& Config.NOT_ALLOWED_USE_HEAVY.contains(classId)) {
					return false;
				}
			} else if ("light".equals(subType)) {
				if (!Config.ALLOW_HEAVY_USE_LIGHT && Config.NOT_ALLOWED_USE_LIGHT != null
						&& Config.NOT_ALLOWED_USE_LIGHT.contains(classId)) {
					return false;
				}
			}
		}

		// 3. Restricoes de Armas por Classe
		if (template.kind() == ItemTemplate.Kind.WEAPON) {
			if ("bow".equals(subType)) {
				if (Config.ALT_DISABLE_BOW && Config.DISABLE_BOW_FOR_CLASSES != null
						&& Config.DISABLE_BOW_FOR_CLASSES.contains(classId)) {
					return false;
				}
			} else if ("dagger".equals(subType)) {
				if (Config.ALT_DISABLE_DAGGER && Config.DISABLE_DAGGER_FOR_CLASSES != null
						&& Config.DISABLE_DAGGER_FOR_CLASSES.contains(classId)) {
					return false;
				}
			} else if ("blunt".equals(subType)) {
				if (Config.ALT_DISABLE_BLUNT && Config.DISABLE_BLUNT_FOR_CLASSES != null
						&& Config.DISABLE_BLUNT_FOR_CLASSES.contains(classId)) {
					return false;
				}
			} else if ("dual".equals(subType) || "dualfist".equals(subType)) {
				if (Config.ALT_DISABLE_DUAL && Config.DISABLE_DUAL_FOR_CLASSES != null
						&& Config.DISABLE_DUAL_FOR_CLASSES.contains(classId)) {
					return false;
				}
			} else if ("pole".equals(subType) || "polle".equals(subType)) {
				if (Config.ALT_DISABLE_POLLE && Config.DISABLE_POLLE_FOR_CLASSES != null
						&& Config.DISABLE_POLLE_FOR_CLASSES.contains(classId)) {
					return false;
				}
			} else if (isBigSword(template, subType)) {
				if (Config.ALT_DISABLE_BIG_SWORD && Config.DISABLE_BIG_SWORD_FOR_CLASSES != null
						&& Config.DISABLE_BIG_SWORD_FOR_CLASSES.contains(classId)) {
					return false;
				}
			} else if ("sword".equals(subType)) {
				if (Config.ALT_DISABLE_SWORD && Config.DISABLE_SWORD_FOR_CLASSES != null
						&& Config.DISABLE_SWORD_FOR_CLASSES.contains(classId)) {
					return false;
				}
			}
		}

		return true;
	}

	private static boolean isBigSword(ItemTemplate template, String subType) {
		if ("bigsword".equals(subType) || "big_sword".equals(subType) || "twohandsword".equals(subType)) {
			return true;
		}
		return "sword".equals(subType) && (template.bodyPart() & ItemSlots.SLOT_LR_HAND) != 0;
	}
}
