package com.lopez.l2j.game.item;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.item.EnchantScrollTable.ScrollInfo;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servico de resolucao de chances de encantamento, safe limits, bônus de Dwarf
 * e comportamento de falhas (AltEncLvlAfterFail, EnchantRollBack, etc).
 */
public final class EnchantTableService {

	private static final Map<String, NavigableMap<Integer, Integer>> CACHED_SERIES = new ConcurrentHashMap<>();

	private EnchantTableService() {
	}

	/**
	 * Converte strings no formato "1,100;2,100;3,100;4,96;..." ou "1.100;2.100;..."
	 * em um mapa ordenado de nivel -> probabilidade inteira (0 a 100).
	 */
	public static NavigableMap<Integer, Integer> parseSeries(String series) {
		if (series == null || series.isBlank()) {
			return new TreeMap<>();
		}
		return CACHED_SERIES.computeIfAbsent(series.trim(), s -> {
			NavigableMap<Integer, Integer> map = new TreeMap<>();
			String[] pairs = s.split(";");
			for (String pair : pairs) {
				String trimmed = pair.trim();
				if (trimmed.isEmpty()) {
					continue;
				}
				String[] parts = trimmed.split("[,.]");
				if (parts.length >= 2) {
					try {
						int lvl = Integer.parseInt(parts[0].trim());
						int chance = Integer.parseInt(parts[1].trim());
						map.put(lvl, chance);
					} catch (NumberFormatException ignored) {
					}
				}
			}
			return map;
		});
	}

	public static int parseAndGetChance(String series, int targetEnchantLevel) {
		NavigableMap<Integer, Integer> map = parseSeries(series);
		if (map.isEmpty()) {
			return 66; // Chance default caso nenhuma serie seja informada
		}
		if (targetEnchantLevel <= 0) {
			return 100;
		}
		if (map.containsKey(targetEnchantLevel)) {
			return map.get(targetEnchantLevel);
		}
		if (targetEnchantLevel > map.lastKey()) {
			return map.lastEntry().getValue();
		}
		if (targetEnchantLevel < map.firstKey()) {
			return map.firstEntry().getValue();
		}
		Map.Entry<Integer, Integer> floor = map.floorEntry(targetEnchantLevel);
		return floor != null ? floor.getValue() : 66;
	}

	/**
	 * Verifica se o pergaminho esta habilitado no servidor.
	 */
	public static boolean isScrollAllowed(ScrollInfo scroll) {
		if (scroll == null) {
			return false;
		}
		if (scroll.isCrystal() && !Config.ALLOW_CRYSTAL_SCROLL) {
			return false;
		}
		return true;
	}

	/**
	 * Limite seguro de encantamento antes de haver risco de quebra ou falha.
	 */
	public static int getSafeLimit(ItemTemplate template) {
		if (template == null) {
			return Config.ENCHANT_SAFE_MAX;
		}
		if (template.bodyPart() == ItemSlots.SLOT_FULL_ARMOR) {
			return Config.ENCHANT_SAFE_MAX_FULL;
		}
		return Config.ENCHANT_SAFE_MAX;
	}

	/**
	 * Verifica se o item ja atingiu ou superou os limites maximos configurados.
	 */
	public static boolean isOverEnchant(ItemInstance item, ScrollInfo scroll) {
		if (item == null || scroll == null) {
			return true;
		}
		int current = item.enchant();
		if (Config.ENCHANT_OVER_CHANT_CHECK > 0 && current >= Config.ENCHANT_OVER_CHANT_CHECK) {
			return true;
		}

		boolean isWeapon = item.template().type2() == ItemTemplate.TYPE2_WEAPON;
		boolean isJewelry = item.template().type2() == ItemTemplate.TYPE2_ACCESSORY;

		if (scroll.isDonator()) {
			if (isWeapon && Config.ENCHANT_MAX_WEAPON_DONATOR > 0 && current >= Config.ENCHANT_MAX_WEAPON_DONATOR) {
				return true;
			}
			if (isJewelry && Config.ENCHANT_MAX_JEWELRY_DONATOR > 0 && current >= Config.ENCHANT_MAX_JEWELRY_DONATOR) {
				return true;
			}
			if (!isWeapon && !isJewelry && Config.ENCHANT_MAX_ARMOR_DONATOR > 0 && current >= Config.ENCHANT_MAX_ARMOR_DONATOR) {
				return true;
			}
		} else if (scroll.isCrystal()) {
			if (isWeapon && Config.ENCHANT_MAX_WEAPON_CRYSTAL > 0 && current >= Config.ENCHANT_MAX_WEAPON_CRYSTAL) {
				return true;
			}
			if (isJewelry && Config.ENCHANT_MAX_JEWELRY_CRYSTAL > 0 && current >= Config.ENCHANT_MAX_JEWELRY_CRYSTAL) {
				return true;
			}
			if (!isWeapon && !isJewelry && Config.ENCHANT_MAX_ARMOR_CRYSTAL > 0 && current >= Config.ENCHANT_MAX_ARMOR_CRYSTAL) {
				return true;
			}
		} else if (scroll.isBlessed()) {
			if (isWeapon && Config.ENCHANT_MAX_WEAPON_BLESSED > 0 && current >= Config.ENCHANT_MAX_WEAPON_BLESSED) {
				return true;
			}
			if (isJewelry && Config.ENCHANT_MAX_JEWELRY_BLESSED > 0 && current >= Config.ENCHANT_MAX_JEWELRY_BLESSED) {
				return true;
			}
			if (!isWeapon && !isJewelry && Config.ENCHANT_MAX_ARMOR_BLESSED > 0 && current >= Config.ENCHANT_MAX_ARMOR_BLESSED) {
				return true;
			}
		} else {
			if (isWeapon && Config.ENCHANT_MAX_WEAPON_NORMAL > 0 && current >= Config.ENCHANT_MAX_WEAPON_NORMAL) {
				return true;
			}
			if (isJewelry && Config.ENCHANT_MAX_JEWELRY_NORMAL > 0 && current >= Config.ENCHANT_MAX_JEWELRY_NORMAL) {
				return true;
			}
			if (!isWeapon && !isJewelry && Config.ENCHANT_MAX_ARMOR_NORMAL > 0 && current >= Config.ENCHANT_MAX_ARMOR_NORMAL) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Calcula a probabilidade final (0 a 100%) para o proximo nivel de encantamento.
	 */
	public static int getEnchantChance(ScrollInfo scroll, ItemInstance item, int playerRace) {
		if (item == null || scroll == null) {
			return 0;
		}

		int safeLimit = getSafeLimit(item.template());
		if (item.enchant() < safeLimit) {
			return 100;
		}

		boolean isWeapon = item.template().type2() == ItemTemplate.TYPE2_WEAPON;
		boolean isJewelry = item.template().type2() == ItemTemplate.TYPE2_ACCESSORY;

		String series;
		if (scroll.isDonator()) {
			series = isWeapon ? Config.DONATOR_WEAPON_ENCHANT_LEVEL
					: (isJewelry ? Config.DONATOR_JEWELRY_ENCHANT_LEVEL : Config.DONATOR_ARMOR_ENCHANT_LEVEL);
		} else if (scroll.isCrystal()) {
			series = isWeapon ? Config.CRYSTAL_WEAPON_ENCHANT_LEVEL
					: (isJewelry ? Config.CRYSTAL_JEWELRY_ENCHANT_LEVEL : Config.CRYSTAL_ARMOR_ENCHANT_LEVEL);
		} else if (scroll.isBlessed()) {
			series = isWeapon ? Config.BLESS_WEAPON_ENCHANT_LEVEL
					: (isJewelry ? Config.BLESS_JEWELRY_ENCHANT_LEVEL : Config.BLESS_ARMOR_ENCHANT_LEVEL);
		} else {
			series = isWeapon ? Config.NORMAL_WEAPON_ENCHANT_LEVEL
					: (isJewelry ? Config.NORMAL_JEWELRY_ENCHANT_LEVEL : Config.NORMAL_ARMOR_ENCHANT_LEVEL);
		}

		int chance = parseAndGetChance(series, item.enchant() + 1);

		// Sistema Dwarf de bonificacao de chance
		if (Config.ENCHANT_DWARF_SYSTEM && playerRace == 4) {
			if (item.enchant() >= Config.ENCHANT_DWARF_3_ENCHANT_LEVEL) {
				chance += Config.ENCHANT_DWARF_3_CHANCE;
			} else if (item.enchant() >= Config.ENCHANT_DWARF_2_ENCHANT_LEVEL) {
				chance += Config.ENCHANT_DWARF_2_CHANCE;
			} else if (item.enchant() >= Config.ENCHANT_DWARF_1_ENCHANT_LEVEL) {
				chance += Config.ENCHANT_DWARF_1_CHANCE;
			}
		}

		return Math.min(100, Math.max(0, chance));
	}

	/**
	 * Determina o nivel resultante em caso de falha.
	 * Retorna -1 para indicar que o item deve evaporar (destruir).
	 */
	public static int calculateFailureEnchant(ScrollInfo scroll, ItemInstance item) {
		if (scroll == null || item == null) {
			return -1;
		}
		if (scroll.isDonator() || scroll.isCrystal()) {
			// Scroll Donator ou Cristal: falha mantem o nivel de encantamento sem quebrar o item
			return item.enchant();
		}
		if (scroll.isBlessed()) {
			if (Config.ALT_ENC_LVL_AFTER_FAIL) {
				if (Config.ENCHANT_ROLL_BACK) {
					return Math.max(0, item.enchant() - Config.ENCHANT_ROLL_BACK_VALUE);
				} else {
					return getSafeLimit(item.template());
				}
			} else {
				return 0;
			}
		}
		// Pergaminho normal: item quebra e evapora
		return -1;
	}

	/**
	 * ID do item de cristal correspondente a grade (D, C, B, A, S).
	 */
	public static int getCrystalId(String crystalType) {
		if (crystalType == null) {
			return 0;
		}
		return switch (crystalType.toLowerCase(java.util.Locale.ROOT)) {
			case "d" -> 1458;
			case "c" -> 1459;
			case "b" -> 1460;
			case "a" -> 1461;
			case "s" -> 1462;
			default -> 0;
		};
	}

	/**
	 * Calcula a quantidade de cristais concedidos quando um item quebra no encantamento.
	 */
	public static int calculateCrystalsOnBreak(ItemInstance item) {
		if (item == null || item.template() == null) {
			return 0;
		}
		int crystalId = getCrystalId(item.template().crystalType());
		if (crystalId == 0) {
			return 0;
		}
		int price = item.template().price();
		int crystalPrice = switch (item.template().crystalType().toLowerCase(java.util.Locale.ROOT)) {
			case "d" -> 650;
			case "c" -> 2500;
			case "b" -> 11000;
			case "a" -> 39000;
			case "s" -> 180000;
			default -> 0;
		};
		if (crystalPrice <= 0) {
			return 0;
		}
		int baseCrystals = Math.max(1, price / crystalPrice);
		int enchant = item.enchant();
		if (enchant <= 0) {
			return baseCrystals;
		}
		boolean isWeapon = item.template().type2() == ItemTemplate.TYPE2_WEAPON;
		int bonusPerEnchant = switch (item.template().crystalType().toLowerCase(java.util.Locale.ROOT)) {
			case "d" -> isWeapon ? 11 : 3;
			case "c" -> isWeapon ? 15 : 4;
			case "b" -> isWeapon ? 19 : 5;
			case "a" -> isWeapon ? 23 : 6;
			case "s" -> isWeapon ? 27 : 7;
			default -> 0;
		};
		if (item.template().bodyPart() == ItemSlots.SLOT_FULL_ARMOR) {
			bonusPerEnchant = (int) Math.round(bonusPerEnchant * 1.5);
		}
		return baseCrystals + (enchant * bonusPerEnchant);
	}
}
