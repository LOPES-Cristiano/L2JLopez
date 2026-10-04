package com.lopez.l2j.game.skill;

import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemSlots;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.PlayerCharacter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Condicoes do datapack ({@code <and>}, {@code <or>}, {@code <not>}, {@code <using kind=...>},
 * {@code <player level/hp=...>}). So o subconjunto que o servidor sabe avaliar; o parser devolve null para o
 * resto e quem chama decide (passivas desconhecidas nao aplicam, conds de uso sao permissivas).
 */
@FunctionalInterface
public interface SkillCondition {

	boolean test(PlayerCharacter p);

	Set<String> ARMOR_KINDS = Set.of("light", "heavy", "magic");

	/** @return condicao ou null se algum elemento nao for suportado */
	static SkillCondition parse(Element e) {
		String tag = e.getTagName().toLowerCase(Locale.ROOT);
		switch (tag) {
			case "and", "or" -> {
				List<SkillCondition> parts = new ArrayList<>();
				for (Element child : children(e)) {
					SkillCondition c = parse(child);
					if (c == null) {
						return null;
					}
					parts.add(c);
				}
				if (parts.isEmpty()) {
					return null;
				}
				return tag.equals("and") ? p -> parts.stream().allMatch(c -> c.test(p))
						: p -> parts.stream().anyMatch(c -> c.test(p));
			}
			case "not" -> {
				var kids = children(e);
				SkillCondition inner = kids.size() == 1 ? parse(kids.get(0)) : null;
				return inner == null ? null : p -> !inner.test(p);
			}
			case "using" -> {
				String kind = e.getAttribute("kind");
				if (kind.isBlank()) {
					return null;
				}
				List<String> kinds = new ArrayList<>();
				for (String k : kind.split(",")) {
					kinds.add(normalize(k));
				}
				return p -> kinds.stream().anyMatch(k -> isUsing(p, k));
			}
			case "player" -> {
				if (e.hasAttribute("level") && e.getAttributes().getLength() == 1) {
					int min = Integer.parseInt(e.getAttribute("level").trim());
					return p -> p.level() >= min;
				}
				if (e.hasAttribute("hp") && e.getAttributes().getLength() == 1) {
					double pct = Double.parseDouble(e.getAttribute("hp").trim());
					return p -> p.maxHp() > 0 && p.currentHp() * 100.0 / p.maxHp() <= pct;
				}
				return null;
			}
			default -> {
				return null;
			}
		}
	}

	static List<Element> children(Element e) {
		List<Element> out = new ArrayList<>();
		for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
			if (n instanceof Element el) {
				out.add(el);
			}
		}
		return out;
	}

	static String normalize(String kind) {
		return kind.trim().toLowerCase(Locale.ROOT).replace(" ", "");
	}

	/** Nome do tipo de arma no formato do datapack (Big Sword, Dual Sword...), normalizado. */
	static String weaponKind(ItemTemplate t) {
		String sub = t.subType();
		boolean twoHanded = (t.bodyPart() & ItemSlots.SLOT_LR_HAND) == ItemSlots.SLOT_LR_HAND;
		return switch (sub) {
			case "sword" -> twoHanded ? "bigsword" : "sword";
			case "blunt" -> twoHanded ? "bigblunt" : "blunt";
			case "dual" -> "dualsword";
			case "dualfist" -> "dualfist";
			default -> sub.replace(" ", "");
		};
	}

	private static boolean isUsing(PlayerCharacter p, String kind) {
		var inv = p.inventory();
		if (ARMOR_KINDS.contains(kind)) {
			ItemInstance chest = inv.paperdoll(ItemSlots.CHEST);
			return chest != null && kind.equals(chest.template().subType());
		}
		ItemInstance w = inv.paperdoll(ItemSlots.RHAND);
		if (w == null) {
			w = inv.paperdoll(ItemSlots.LRHAND);
		}
		if (w == null || w.template().kind() != ItemTemplate.Kind.WEAPON) {
			return false;
		}
		String wk = weaponKind(w.template());
		// "Sword"/"Blunt" no datapack tambem casam com as versoes de duas maos
		return wk.equals(kind) || wk.equals("big" + kind);
	}
}
