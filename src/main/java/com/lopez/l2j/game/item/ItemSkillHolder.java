package com.lopez.l2j.game.item;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa um skill associado a um item (ex: SA de armas, bônus de joias de boss,
 * skills de chance onCrit/onCast ou bônus de dual +4).
 *
 * @param skillId ID do skill correspondente na SkillTable
 * @param level   Nível do skill a aplicar
 * @param chance  Chance em porcentagem (0-100) para ativação em onCrit/onCast (padrão 100 para passivas/ativas)
 */
public record ItemSkillHolder(int skillId, int level, int chance) {

	public ItemSkillHolder(int skillId, int level) {
		this(skillId, level, 100);
	}

	/**
	 * Converte uma lista de skills no formato oficial do L2J/L2JDream (ex: "3047-1;3558-1")
	 * para uma lista imutável de {@link ItemSkillHolder}.
	 */
	public static List<ItemSkillHolder> parseList(String raw) {
		if (raw == null || raw.isBlank()) {
			return List.of();
		}
		List<ItemSkillHolder> list = new ArrayList<>();
		for (String part : raw.split(";")) {
			ItemSkillHolder h = parse(part);
			if (h != null) {
				list.add(h);
			}
		}
		return List.copyOf(list);
	}

	/**
	 * Converte uma string individual (ex: "3047-1" ou "3020-1-12") para um {@link ItemSkillHolder}.
	 */
	public static ItemSkillHolder parse(String raw) {
		if (raw == null || raw.isBlank() || raw.equals("0-0")) {
			return null;
		}
		String[] parts = raw.trim().split("-");
		if (parts.length >= 2) {
			try {
				int id = Integer.parseInt(parts[0].trim());
				int lvl = Integer.parseInt(parts[1].trim());
				int chance = parts.length >= 3 ? Integer.parseInt(parts[2].trim()) : 100;
				if (id > 0 && lvl > 0) {
					return new ItemSkillHolder(id, lvl, chance);
				}
			} catch (NumberFormatException ignored) {
			}
		}
		return null;
	}
}
