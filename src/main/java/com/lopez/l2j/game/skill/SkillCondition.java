package com.lopez.l2j.game.skill;

import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemSlots;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.Creature;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Condicoes do datapack ({@code <and>}, {@code <or>}, {@code <not>}, {@code <using kind/slotitem/skill=...>},
 * {@code <player level/hp/mp/cp/state/race/charges/...>}, {@code <target undead/party/class_id_restriction/...>},
 * {@code <game night="true|false">}).
 */
@FunctionalInterface
public interface SkillCondition {

	boolean test(PlayerCharacter p, Object target);

	default boolean test(PlayerCharacter p) {
		return test(p, null);
	}

	Set<String> ARMOR_KINDS = Set.of("light", "heavy", "magic");

	// =========================================================================
	// Named Condition Implementations (#188-#191)
	// =========================================================================

	final class UsingKind implements SkillCondition {
		private final List<String> kinds;

		public UsingKind(String kind) {
			List<String> list = new ArrayList<>();
			if (kind != null) {
				for (String k : kind.split(",")) {
					String norm = normalize(k);
					if (!norm.isEmpty()) {
						list.add(norm);
					}
				}
			}
			this.kinds = List.copyOf(list);
		}

		@Override
		public boolean test(PlayerCharacter p, Object target) {
			return p != null && kinds.stream().anyMatch(k -> isUsing(p, k));
		}
	}

	final class PlayerHp implements SkillCondition {
		private final double maxHpPercent;

		public PlayerHp(double maxHpPercent) {
			this.maxHpPercent = maxHpPercent;
		}

		@Override
		public boolean test(PlayerCharacter p, Object target) {
			return p != null && p.maxHp() > 0 && (p.currentHp() * 100.0 / p.maxHp()) <= maxHpPercent;
		}
	}

	final class PlayerState implements SkillCondition {
		private final String state;
		private final boolean requiredValue;

		public PlayerState(String state, boolean requiredValue) {
			this.state = state != null ? state.trim().toLowerCase(Locale.ROOT) : "";
			this.requiredValue = requiredValue;
		}

		@Override
		public boolean test(PlayerCharacter p, Object target) {
			if (p == null) {
				return false;
			}
			return switch (state) {
				case "resting" -> p.isSitting() == requiredValue;
				case "moving" -> p.isMoving() == requiredValue;
				case "running" -> (!p.isSitting() && p.running()) == requiredValue;
				case "standing" -> (!p.isSitting() && !p.isMoving()) == requiredValue;
				case "flying" -> p.isFlying() == requiredValue;
				case "front" -> {
					if (target instanceof Creature c) {
						boolean isFront = CombatService.getRelativePosition(p.x(), p.y(), c.x(), c.y(), c.heading())
								== CombatService.RelativePosition.FRONT;
						yield isFront == requiredValue;
					}
					yield true;
				}
				case "behind" -> {
					if (target instanceof Creature c) {
						boolean isBehind = CombatService.getRelativePosition(p.x(), p.y(), c.x(), c.y(), c.heading())
								== CombatService.RelativePosition.BEHIND;
						yield isBehind == requiredValue;
					}
					yield !requiredValue;
				}
				case "side" -> {
					if (target instanceof Creature c) {
						boolean isSide = CombatService.getRelativePosition(p.x(), p.y(), c.x(), c.y(), c.heading())
								== CombatService.RelativePosition.SIDE;
						yield isSide == requiredValue;
					}
					yield !requiredValue;
				}
				case "olympiad" -> p.isOlympiadMode() == requiredValue;
				case "chaotic" -> (p.karma() > 0) == requiredValue;
				default -> true;
			};
		}
	}

	final class TargetMyParty implements SkillCondition {
		private final String mode;

		public TargetMyParty(String mode) {
			this.mode = mode != null ? mode.trim().toUpperCase(Locale.ROOT) : "INCLUDE_ME";
		}

		@Override
		public boolean test(PlayerCharacter p, Object target) {
			if (p == null) {
				return false;
			}
			if ("ONLY_ME".equals(mode)) {
				return target == null || target == p
						|| (target instanceof PlayerCharacter t && t.objectId() == p.objectId());
			}
			boolean sameSelf = target == p || (target instanceof PlayerCharacter t && t.objectId() == p.objectId());
			boolean sameClanOrParty = sameSelf || (target instanceof PlayerCharacter t
					&& p.clanId() > 0 && p.clanId() == t.clanId());
			if ("EXCEPT_ME".equals(mode)) {
				return !sameSelf && target instanceof PlayerCharacter;
			}
			if ("FALSE".equals(mode) || "NOT_MY_PARTY".equals(mode)) {
				return !sameSelf;
			}
			return sameClanOrParty || target instanceof PlayerCharacter;
		}
	}

	final class PlayerRace implements SkillCondition {
		private final Set<Integer> allowedRaces;

		public PlayerRace(String racesStr) {
			Set<Integer> set = new HashSet<>();
			if (racesStr != null) {
				for (String part : racesStr.split(",")) {
					int r = parseRaceId(part.trim());
					if (r >= 0) {
						set.add(r);
					}
				}
			}
			this.allowedRaces = Set.copyOf(set);
		}

		@Override
		public boolean test(PlayerCharacter p, Object target) {
			if (p == null) {
				return false;
			}
			return allowedRaces.isEmpty() || allowedRaces.contains(p.race());
		}
	}

	final class GameTime implements SkillCondition {
		private static volatile Boolean nightOverride = null;
		private final boolean requiredNight;

		public GameTime(boolean requiredNight) {
			this.requiredNight = requiredNight;
		}

		public static void setNightOverride(Boolean override) {
			nightOverride = override;
		}

		public static boolean isNight() {
			Boolean ov = nightOverride;
			if (ov != null) {
				return ov;
			}
			// Ciclo oficial L2: 4 horas reais = 24 horas in-game (1 hora in-game = 10 min reais; 00:00-06:00 = noite)
			long gameMinutes = (System.currentTimeMillis() / 10000L) % (24L * 60L);
			long gameHour = gameMinutes / 60L;
			return gameHour < 6;
		}

		@Override
		public boolean test(PlayerCharacter p, Object target) {
			return isNight() == requiredNight;
		}
	}

	final class HasCharges implements SkillCondition {
		private final int minCharges;

		public HasCharges(int minCharges) {
			this.minCharges = minCharges;
		}

		@Override
		public boolean test(PlayerCharacter p, Object target) {
			return p != null && p.charges() >= minCharges;
		}
	}

	final class TargetClassIdRestriction implements SkillCondition {
		private final Set<Integer> allowedClassIds;

		public TargetClassIdRestriction(String classIdsCsv) {
			Set<Integer> ids = new HashSet<>();
			if (classIdsCsv != null) {
				for (String part : classIdsCsv.split(",")) {
					try {
						ids.add(Integer.parseInt(part.trim()));
					} catch (NumberFormatException ignored) {
					}
				}
			}
			this.allowedClassIds = Set.copyOf(ids);
		}

		@Override
		public boolean test(PlayerCharacter p, Object target) {
			if (allowedClassIds.isEmpty()) {
				return true;
			}
			if (target instanceof PlayerCharacter t) {
				return allowedClassIds.contains(t.classId());
			}
			return false;
		}
	}

	// =========================================================================
	// Factory Methods
	// =========================================================================

	static SkillCondition using(String kind) {
		return new UsingKind(kind);
	}

	static SkillCondition playerHp(double pct) {
		return new PlayerHp(pct);
	}

	static SkillCondition playerState(String state, boolean val) {
		return new PlayerState(state, val);
	}

	static SkillCondition targetMyParty(String mode) {
		return new TargetMyParty(mode);
	}

	static SkillCondition playerRace(String race) {
		return new PlayerRace(race);
	}

	static SkillCondition gameTime(boolean night) {
		return new GameTime(night);
	}

	static SkillCondition hasCharges(int charges) {
		return new HasCharges(charges);
	}

	static SkillCondition targetClassIdRestriction(String classIds) {
		return new TargetClassIdRestriction(classIds);
	}

	/** @return condicao avaliavel para tags XML de skills e stats */
	static SkillCondition parse(Element e) {
		if (e == null) {
			return null;
		}
		String tag = e.getTagName().toLowerCase(Locale.ROOT);
		switch (tag) {
			case "and", "or" -> {
				List<SkillCondition> parts = new ArrayList<>();
				for (Element child : children(e)) {
					SkillCondition c = parse(child);
					if (c != null) {
						parts.add(c);
					}
				}
				if (parts.isEmpty()) {
					return (p, target) -> true;
				}
				if (parts.size() == 1) {
					return parts.get(0);
				}
				return tag.equals("and") ? (p, target) -> parts.stream().allMatch(c -> c.test(p, target))
						: (p, target) -> parts.stream().anyMatch(c -> c.test(p, target));
			}
			case "not" -> {
				var kids = children(e);
				SkillCondition inner = kids.size() == 1 ? parse(kids.get(0)) : null;
				return inner == null ? (p, target) -> true : (p, target) -> !inner.test(p, target);
			}
			case "game" -> {
				if (e.hasAttribute("night")) {
					boolean night = Boolean.parseBoolean(e.getAttribute("night").trim());
					return new GameTime(night);
				}
				return (p, target) -> true;
			}
			case "using" -> {
				List<SkillCondition> usingConds = new ArrayList<>();
				String kind = e.getAttribute("kind");
				if (kind != null && !kind.isBlank()) {
					usingConds.add(new UsingKind(kind));
				}
				if (e.hasAttribute("slotitem")) {
					String slotItem = e.getAttribute("slotitem").trim();
					String[] parts = slotItem.split(";");
					if (parts.length >= 2) {
						try {
							int itemId = Integer.parseInt(parts[0].trim());
							int slot = Integer.parseInt(parts[1].trim());
							int enchant = parts.length >= 3 ? Integer.parseInt(parts[2].trim()) : 0;
							usingConds.add((p, target) -> {
								if (p == null || p.inventory() == null) return false;
								ItemInstance equipped = p.inventory().paperdoll(slot);
								if (equipped == null || equipped.itemId() != itemId) return false;
								return equipped.enchant() >= enchant;
							});
						} catch (NumberFormatException ignored) {
						}
					}
				}
				if (e.hasAttribute("skill")) {
					try {
						int skillId = Integer.parseInt(e.getAttribute("skill").trim());
						usingConds.add((p, target) -> p != null && p.skillLevel(skillId) > 0);
					} catch (NumberFormatException ignored) {
					}
				}
				if (usingConds.isEmpty()) {
					return (p, target) -> true;
				}
				if (usingConds.size() == 1) {
					return usingConds.get(0);
				}
				return (p, target) -> usingConds.stream().allMatch(c -> c.test(p, target));
			}
			case "player" -> {
				List<SkillCondition> playerConditions = new ArrayList<>();
				if (e.hasAttribute("hp")) {
					double pct = Double.parseDouble(e.getAttribute("hp").trim());
					playerConditions.add(new PlayerHp(pct));
				}
				if (e.hasAttribute("mp")) {
					double pct = Double.parseDouble(e.getAttribute("mp").trim());
					playerConditions.add((p, target) -> p != null && p.maxMp() > 0 && (p.currentMp() * 100.0 / p.maxMp()) <= pct);
				}
				if (e.hasAttribute("cp")) {
					double pct = Double.parseDouble(e.getAttribute("cp").trim());
					playerConditions.add((p, target) -> p != null && p.maxCp() > 0 && (p.currentCp() * 100.0 / p.maxCp()) <= pct);
				}
				if (e.hasAttribute("level")) {
					int min = Integer.parseInt(e.getAttribute("level").trim());
					playerConditions.add((p, target) -> p != null && p.level() >= min);
				}
				if (e.hasAttribute("charges")) {
					int min = Integer.parseInt(e.getAttribute("charges").trim());
					playerConditions.add(new HasCharges(min));
				}
				if (e.hasAttribute("resting")) {
					playerConditions.add(new PlayerState("resting", Boolean.parseBoolean(e.getAttribute("resting").trim())));
				}
				if (e.hasAttribute("moving")) {
					playerConditions.add(new PlayerState("moving", Boolean.parseBoolean(e.getAttribute("moving").trim())));
				}
				if (e.hasAttribute("running")) {
					playerConditions.add(new PlayerState("running", Boolean.parseBoolean(e.getAttribute("running").trim())));
				}
				if (e.hasAttribute("standing")) {
					playerConditions.add(new PlayerState("standing", Boolean.parseBoolean(e.getAttribute("standing").trim())));
				}
				if (e.hasAttribute("flying")) {
					playerConditions.add(new PlayerState("flying", Boolean.parseBoolean(e.getAttribute("flying").trim())));
				}
				if (e.hasAttribute("front")) {
					playerConditions.add(new PlayerState("front", Boolean.parseBoolean(e.getAttribute("front").trim())));
				}
				if (e.hasAttribute("behind")) {
					playerConditions.add(new PlayerState("behind", Boolean.parseBoolean(e.getAttribute("behind").trim())));
				}
				if (e.hasAttribute("side")) {
					playerConditions.add(new PlayerState("side", Boolean.parseBoolean(e.getAttribute("side").trim())));
				}
				if (e.hasAttribute("olympiad")) {
					playerConditions.add(new PlayerState("olympiad", Boolean.parseBoolean(e.getAttribute("olympiad").trim())));
				}
				if (e.hasAttribute("chaotic")) {
					playerConditions.add(new PlayerState("chaotic", Boolean.parseBoolean(e.getAttribute("chaotic").trim())));
				}
				if (e.hasAttribute("race")) {
					playerConditions.add(new PlayerRace(e.getAttribute("race")));
				}
				if (e.hasAttribute("sex") || e.hasAttribute("gender")) {
					String rawSex = e.hasAttribute("sex") ? e.getAttribute("sex") : e.getAttribute("gender");
					int reqSex = "female".equalsIgnoreCase(rawSex.trim()) || "1".equals(rawSex.trim()) ? 1 : 0;
					playerConditions.add((p, target) -> p != null && p.sex() == reqSex);
				}
				if (e.hasAttribute("class_id_restriction")) {
					Set<Integer> allowed = parseCsvInts(e.getAttribute("class_id_restriction"));
					if (!allowed.isEmpty()) {
						playerConditions.add((p, target) -> p != null && allowed.contains(p.classId()));
					}
				}
				if (e.hasAttribute("active_effect_id")) {
					int effectSkillId = parseFirstInt(e.getAttribute("active_effect_id"), 0);
					if (effectSkillId > 0) {
						playerConditions.add((p, target) -> p != null && p.effects() != null && p.effects().hasSkill(effectSkillId));
					}
				}
				if (e.hasAttribute("active_skill_id")) {
					int skillId = parseFirstInt(e.getAttribute("active_skill_id"), 0);
					if (skillId > 0) {
						playerConditions.add((p, target) -> p != null && p.skillLevel(skillId) > 0);
					}
				}
				if (e.hasAttribute("seed_fire")) {
					int min = Integer.parseInt(e.getAttribute("seed_fire").trim());
					playerConditions.add((p, target) -> p != null && (p.effects().hasSkill(1285) || p.charges() >= min));
				}
				if (e.hasAttribute("seed_water")) {
					int min = Integer.parseInt(e.getAttribute("seed_water").trim());
					playerConditions.add((p, target) -> p != null && (p.effects().hasSkill(1286) || p.charges() >= min));
				}
				if (e.hasAttribute("seed_wind")) {
					int min = Integer.parseInt(e.getAttribute("seed_wind").trim());
					playerConditions.add((p, target) -> p != null && (p.effects().hasSkill(1287) || p.charges() >= min));
				}
				if (e.hasAttribute("seed_any")) {
					int min = Integer.parseInt(e.getAttribute("seed_any").trim());
					playerConditions.add((p, target) -> p != null && (p.effects().hasSkill(1285) || p.effects().hasSkill(1286) || p.effects().hasSkill(1287) || p.charges() >= min));
				}
				if (e.hasAttribute("SiegeZone")) {
					playerConditions.add((p, target) -> p != null && (p.isInSiege() || p.isGm()));
				}
				if (playerConditions.isEmpty()) {
					return (p, target) -> true;
				}
				if (playerConditions.size() == 1) {
					return playerConditions.get(0);
				}
				return (p, target) -> playerConditions.stream().allMatch(c -> c.test(p, target));
			}
			case "target" -> {
				List<SkillCondition> targetConditions = new ArrayList<>();
				if (e.hasAttribute("undead")) {
					boolean needUndead = Boolean.parseBoolean(e.getAttribute("undead").trim());
					targetConditions.add((p, target) -> {
						if (target instanceof NpcInstance npc && npc.template() != null) {
							return npc.template().isUndead() == needUndead;
						}
						return !needUndead;
					});
				}
				if (e.hasAttribute("party") || e.hasAttribute("my_party")) {
					String partyAttr = e.hasAttribute("party") ? e.getAttribute("party") : e.getAttribute("my_party");
					targetConditions.add(new TargetMyParty(partyAttr));
				}
				if (e.hasAttribute("class_id_restriction")) {
					targetConditions.add(new TargetClassIdRestriction(e.getAttribute("class_id_restriction")));
				}
				if (e.hasAttribute("level")) {
					int minLvl = parseFirstInt(e.getAttribute("level"), 1);
					targetConditions.add((p, target) -> {
						if (target instanceof PlayerCharacter t) return t.level() >= minLvl;
						if (target instanceof NpcInstance n && n.template() != null) return n.template().level() >= minLvl;
						return true;
					});
				}
				if (e.hasAttribute("active_effect_id")) {
					int effId = parseFirstInt(e.getAttribute("active_effect_id"), 0);
					if (effId > 0) {
						targetConditions.add((p, target) -> {
							if (target instanceof PlayerCharacter t && t.effects() != null) {
								return t.effects().hasSkill(effId);
							}
							return true;
						});
					}
				}
				if (e.hasAttribute("active_skill_id")) {
					int skId = parseFirstInt(e.getAttribute("active_skill_id"), 0);
					if (skId > 0) {
						targetConditions.add((p, target) -> target instanceof PlayerCharacter t && t.skillLevel(skId) > 0);
					}
				}
				if (e.hasAttribute("mindistance")) {
					int minDist = parseFirstInt(e.getAttribute("mindistance"), 0);
					if (minDist > 0) {
						targetConditions.add((p, target) -> {
							if (p == null) return false;
							if (target instanceof Creature c) {
								return Math.hypot(p.x() - c.x(), p.y() - c.y()) >= minDist;
							}
							return true;
						});
					}
				}
				if (e.hasAttribute("npcId")) {
					Set<Integer> npcIds = parseCsvInts(e.getAttribute("npcId"));
					if (!npcIds.isEmpty()) {
						targetConditions.add((p, target) -> target instanceof NpcInstance n && npcIds.contains(n.npcId()));
					}
				}
				if (e.hasAttribute("playable")) {
					boolean needPlayable = Boolean.parseBoolean(e.getAttribute("playable").trim());
					targetConditions.add((p, target) -> (target instanceof PlayerCharacter) == needPlayable);
				}
				if (e.hasAttribute("mob")) {
					boolean needMob = Boolean.parseBoolean(e.getAttribute("mob").trim());
					targetConditions.add((p, target) -> (target instanceof NpcInstance) == needMob);
				}
				if (targetConditions.isEmpty()) {
					return (p, target) -> true;
				}
				if (targetConditions.size() == 1) {
					return targetConditions.get(0);
				}
				return (p, target) -> targetConditions.stream().allMatch(c -> c.test(p, target));
			}
			default -> {
				return (p, target) -> true;
			}
		}
	}

	private static int parseRaceId(String raceName) {
		if (raceName == null || raceName.isBlank()) {
			return -1;
		}
		return switch (raceName.trim().toLowerCase(Locale.ROOT).replace("_", "").replace(" ", "")) {
			case "human", "0" -> 0;
			case "elf", "1" -> 1;
			case "darkelf", "2" -> 2;
			case "orc", "3" -> 3;
			case "dwarf", "4" -> 4;
			default -> {
				try {
					yield Integer.parseInt(raceName.trim());
				} catch (NumberFormatException e) {
					yield -1;
				}
			}
		};
	}

	private static Set<Integer> parseCsvInts(String csv) {
		Set<Integer> out = new HashSet<>();
		if (csv == null || csv.isBlank()) {
			return out;
		}
		for (String part : csv.split("[,;]")) {
			try {
				out.add(Integer.parseInt(part.trim()));
			} catch (NumberFormatException ignored) {
			}
		}
		return out;
	}

	private static int parseFirstInt(String raw, int def) {
		if (raw == null || raw.isBlank()) {
			return def;
		}
		String first = raw.split("[,;]")[0].trim();
		try {
			return Integer.parseInt(first);
		} catch (NumberFormatException e) {
			return def;
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
		return kind.trim().toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "");
	}

	/** Nome do tipo de arma no formato do datapack (Big Sword, Dual Sword...), normalizado. */
	static String weaponKind(ItemTemplate t) {
		String sub = t.subType();
		if (sub == null) return "none";
		boolean twoHanded = (t.bodyPart() & ItemSlots.SLOT_LR_HAND) == ItemSlots.SLOT_LR_HAND;
		return switch (sub.toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "")) {
			case "sword" -> twoHanded ? "bigsword" : "sword";
			case "blunt" -> twoHanded ? "bigblunt" : "blunt";
			case "dual", "dualsword" -> "dualsword";
			case "dualfist" -> "dualfist";
			case "pole", "polearm" -> "pole";
			case "crossbow" -> "crossbow";
			default -> sub.replace(" ", "").replace("_", "").toLowerCase(Locale.ROOT);
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
		return switch (sub.toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "")) {
			case "sword" -> twoHanded ? (1 << 11) : (1 << 2); // 2048 (BIGSWORD) : 4 (SWORD)
			case "blunt" -> twoHanded ? (1 << 14) : (1 << 3); // 16384 (BIGBLUNT) : 8 (BLUNT)
			case "dagger" -> 1 << 4; // 16
			case "bow" -> 1 << 5; // 32
			case "pole", "polearm" -> 1 << 6; // 64
			case "etc" -> 1 << 7; // 128
			case "fist" -> 1 << 8; // 256
			case "dual", "dualsword" -> 1 << 9; // 512
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
			if (chest == null || !kind.equals(chest.template().subType())) {
				return false;
			}
			if (chest.template().bodyPart() == ItemSlots.SLOT_FULL_ARMOR) {
				return true;
			}
			ItemInstance legs = inv.paperdoll(ItemSlots.LEGS);
			return legs != null && kind.equals(legs.template().subType());
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
