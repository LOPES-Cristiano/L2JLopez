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

	public static int weaponMask(ItemInstance item) {
		if (item == null || item.template() == null) {
			return 1; // 1 << 0: NONE / desarmado
		}
		String sub = item.template().subType();
		if (sub == null) {
			return 1;
		}
		boolean twoHanded = (item.template().bodyPart() & ItemSlots.SLOT_LR_HAND) == ItemSlots.SLOT_LR_HAND;
		return switch (sub.toLowerCase(Locale.ROOT)) {
			case "sword" -> twoHanded ? (1 << 11) : (1 << 2); // 2048 (BIGSWORD) : 4 (SWORD)
			case "blunt" -> twoHanded ? (1 << 14) : (1 << 3); // 16384 (BIGBLUNT) : 8 (BLUNT)
			case "dagger" -> 1 << 4; // 16
			case "bow" -> 1 << 5; // 32
			case "pole" -> 1 << 6; // 64
			case "etc" -> 1 << 7; // 128
			case "fist" -> 1 << 8; // 256
			case "dual" -> 1 << 9; // 512
			case "dualfist" -> 1 << 10; // 1024
			case "bigsword" -> 1 << 11; // 2048
			case "pet" -> 1 << 12; // 4096
			case "rod" -> 1 << 13; // 8192
			case "bigblunt" -> 1 << 14; // 16384
			case "crossbow" -> 1 << 15;
			case "rapier" -> 1 << 16;
			case "ancientsword" -> 1 << 17;
			case "dualdagger" -> 1 << 18;
			default -> 1;
		};
	}

	public static boolean checkWeaponsAllowed(PlayerCharacter p, int mask) {
		if (mask <= 0 || p == null) {
			return true;
		}
		var inv = p.inventory();
		if (inv == null) {
			return false;
		}
		// Verifica restricao de escudo (bit 1, val 2)
		if ((mask & 2) != 0) {
			ItemInstance left = inv.paperdoll(ItemSlots.LHAND);
			boolean hasShield = left != null && (left.template().type2() == ItemTemplate.TYPE2_SHIELD_ARMOR
					|| (left.template().bodyPart() & ItemSlots.SLOT_L_HAND) != 0
							&& !"arrow".equalsIgnoreCase(left.template().subType())
							&& !"bow".equalsIgnoreCase(left.template().subType()));
			if (!hasShield) {
				return false;
			}
		}
		// Verifica restricao de arma (todos os bits exceto bit 1)
		int weaponBits = mask & ~2;
		if (weaponBits != 0) {
			ItemInstance w = inv.paperdoll(ItemSlots.RHAND);
			if (w == null) {
				w = inv.paperdoll(ItemSlots.LRHAND);
			}
			int wMask = weaponMask(w);
			if ((weaponBits & wMask) == 0) {
				return false;
			}
		}
		return true;
	}

	private static boolean isUsing(PlayerCharacter p, String kind) {
		var inv = p.inventory();
		if (inv == null) {
			return false;
		}
		if (ARMOR_KINDS.contains(kind)) {
			ItemInstance chest = inv.paperdoll(ItemSlots.CHEST);
			return chest != null && kind.equals(chest.template().subType());
		}
		if ("shield".equals(kind)) {
			ItemInstance left = inv.paperdoll(ItemSlots.LHAND);
			return left != null && (left.template().type2() == ItemTemplate.TYPE2_SHIELD_ARMOR
					|| (left.template().bodyPart() & ItemSlots.SLOT_L_HAND) != 0
							&& !"arrow".equalsIgnoreCase(left.template().subType())
							&& !"bow".equalsIgnoreCase(left.template().subType()));
		}
		ItemInstance w = inv.paperdoll(ItemSlots.RHAND);
		if (w == null) {
			w = inv.paperdoll(ItemSlots.LRHAND);
		}
		if (w == null || w.template().kind() != ItemTemplate.Kind.WEAPON) {
			return false;
		}
		String wk = weaponKind(w.template());
		if (wk.equals(kind) || wk.equals("big" + kind)) {
			return true;
		}
		if ("dual".equals(kind) && "dualsword".equals(wk)) return true;
		if ("dualsword".equals(kind) && "dual".equals(wk)) return true;
		if ("fist".equals(kind) && "dualfist".equals(wk)) return true;
		if ("dualfist".equals(kind) && "fist".equals(wk)) return true;
		if ("polearm".equals(kind) && "pole".equals(wk)) return true;
		if ("crossbow".equals(kind) && "bow".equals(wk)) return true;
		return false;
	}
}
