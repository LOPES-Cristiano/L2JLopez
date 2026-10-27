package com.lopez.l2j.game.pet;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Tabela de estatisticas de mascotes e alimentos carregada de data/xml/player/pet_stats.xml.
 * Porta de PetDataTable de L2JDream / L2JLucera2.
 */
@Component
public class PetDataTable {

	private static final Logger log = LoggerFactory.getLogger(PetDataTable.class);

	private static volatile PetDataTable instance;

	// IDs de NPCs de mascotes
	public static final int WOLF_ID = 12077;
	public static final int BLACK_WOLF_ID = 16030;
	public static final int GREAT_WOLF_ID = 16025;
	public static final int SNOW_GREAT_WOLF_ID = 16037;
	public static final int FENRIR_ID = 16041;
	public static final int SNOW_FENRIR_ID = 16042;
	public static final int HATCHLING_WIND_ID = 12311;
	public static final int HATCHLING_STAR_ID = 12312;
	public static final int HATCHLING_TWILIGHT_ID = 12313;
	public static final int STRIDER_WIND_ID = 12526;
	public static final int STRIDER_STAR_ID = 12527;
	public static final int STRIDER_TWILIGHT_ID = 12528;
	public static final int RED_STRIDER_WIND_ID = 16038;
	public static final int RED_STRIDER_STAR_ID = 16039;
	public static final int RED_STRIDER_TWILIGHT_ID = 16040;
	public static final int WYVERN_ID = 12621;
	public static final int BABY_BUFFALO_ID = 12780;
	public static final int BABY_KOOKABURRA_ID = 12781;
	public static final int BABY_COUGAR_ID = 12782;
	public static final int IMPROVED_BABY_BUFFALO_ID = 16034;
	public static final int IMPROVED_BABY_KOOKABURRA_ID = 16035;
	public static final int IMPROVED_BABY_COUGAR_ID = 16036;
	public static final int SIN_EATER_ID = 12564;

	// Alimentos oficiais
	public static final int FOOD_WOLF = 2515;
	public static final int FOOD_HATCHLING = 4038;
	public static final int FOOD_STRIDER = 5168;
	public static final int FOOD_STRIDER_DELUXE = 5169;
	public static final int FOOD_WYVERN = 6316;
	public static final int FOOD_BABY = 7582;
	public static final int FOOD_GREAT_WOLF = 9668;
	public static final int FOOD_IMPROVED_BABY = 10425;

	private final Map<Long, PetStatTemplate> stats = new HashMap<>();
	private final Map<Integer, Set<Integer>> petFoodMap = new HashMap<>();
	private final Map<Integer, Integer> collarToNpcMap = new HashMap<>();
	private final Map<Integer, Integer> npcToCollarMap = new HashMap<>();

	private final Set<Integer> mountablePets = Set.of(
			STRIDER_WIND_ID, STRIDER_STAR_ID, STRIDER_TWILIGHT_ID,
			RED_STRIDER_WIND_ID, RED_STRIDER_STAR_ID, RED_STRIDER_TWILIGHT_ID,
			WYVERN_ID, GREAT_WOLF_ID, SNOW_GREAT_WOLF_ID, FENRIR_ID, SNOW_FENRIR_ID
	);

	@Autowired
	public PetDataTable(@Value("${l2j.data.dir:data}") String dataDir) {
		initFoodMap();
		initCollarMap();
		load(Path.of(dataDir, "xml", "player", "pet_stats.xml"));
		instance = this;
	}

	public PetDataTable(Path xmlPath) {
		initFoodMap();
		initCollarMap();
		load(xmlPath);
		instance = this;
	}

	public PetDataTable() {
		initFoodMap();
		initCollarMap();
		if (instance == null) {
			instance = this;
		}
	}

	public static PetDataTable getInstance() {
		if (instance == null) {
			PetDataTable table = new PetDataTable();
			Path defaultPath = Path.of("data", "xml", "player", "pet_stats.xml");
			if (Files.exists(defaultPath)) {
				table.load(defaultPath);
			}
			instance = table;
		}
		return instance;
	}

	private void initFoodMap() {
		petFoodMap.put(WOLF_ID, new LinkedHashSet<>(List.of(FOOD_WOLF)));
		petFoodMap.put(BLACK_WOLF_ID, new LinkedHashSet<>(List.of(FOOD_WOLF)));
		petFoodMap.put(SIN_EATER_ID, new LinkedHashSet<>(List.of(FOOD_WOLF)));
		petFoodMap.put(GREAT_WOLF_ID, new LinkedHashSet<>(List.of(FOOD_GREAT_WOLF, FOOD_WOLF)));
		petFoodMap.put(SNOW_GREAT_WOLF_ID, new LinkedHashSet<>(List.of(FOOD_GREAT_WOLF, FOOD_WOLF)));
		petFoodMap.put(FENRIR_ID, new LinkedHashSet<>(List.of(FOOD_GREAT_WOLF, FOOD_WOLF)));
		petFoodMap.put(SNOW_FENRIR_ID, new LinkedHashSet<>(List.of(FOOD_GREAT_WOLF, FOOD_WOLF)));
		petFoodMap.put(HATCHLING_WIND_ID, new LinkedHashSet<>(List.of(FOOD_HATCHLING)));
		petFoodMap.put(HATCHLING_STAR_ID, new LinkedHashSet<>(List.of(FOOD_HATCHLING)));
		petFoodMap.put(HATCHLING_TWILIGHT_ID, new LinkedHashSet<>(List.of(FOOD_HATCHLING)));
		petFoodMap.put(STRIDER_WIND_ID, new LinkedHashSet<>(List.of(FOOD_STRIDER, FOOD_STRIDER_DELUXE)));
		petFoodMap.put(STRIDER_STAR_ID, new LinkedHashSet<>(List.of(FOOD_STRIDER, FOOD_STRIDER_DELUXE)));
		petFoodMap.put(STRIDER_TWILIGHT_ID, new LinkedHashSet<>(List.of(FOOD_STRIDER, FOOD_STRIDER_DELUXE)));
		petFoodMap.put(RED_STRIDER_WIND_ID, new LinkedHashSet<>(List.of(FOOD_STRIDER, FOOD_STRIDER_DELUXE)));
		petFoodMap.put(RED_STRIDER_STAR_ID, new LinkedHashSet<>(List.of(FOOD_STRIDER, FOOD_STRIDER_DELUXE)));
		petFoodMap.put(RED_STRIDER_TWILIGHT_ID, new LinkedHashSet<>(List.of(FOOD_STRIDER, FOOD_STRIDER_DELUXE)));
		petFoodMap.put(WYVERN_ID, new LinkedHashSet<>(List.of(FOOD_WYVERN)));
		petFoodMap.put(BABY_BUFFALO_ID, new LinkedHashSet<>(List.of(FOOD_BABY)));
		petFoodMap.put(BABY_KOOKABURRA_ID, new LinkedHashSet<>(List.of(FOOD_BABY)));
		petFoodMap.put(BABY_COUGAR_ID, new LinkedHashSet<>(List.of(FOOD_BABY)));
		petFoodMap.put(IMPROVED_BABY_BUFFALO_ID, new LinkedHashSet<>(List.of(FOOD_IMPROVED_BABY, FOOD_BABY)));
		petFoodMap.put(IMPROVED_BABY_KOOKABURRA_ID, new LinkedHashSet<>(List.of(FOOD_IMPROVED_BABY, FOOD_BABY)));
		petFoodMap.put(IMPROVED_BABY_COUGAR_ID, new LinkedHashSet<>(List.of(FOOD_IMPROVED_BABY, FOOD_BABY)));
	}

	private void initCollarMap() {
		registerCollar(2375, WOLF_ID);
		registerCollar(3500, HATCHLING_WIND_ID);
		registerCollar(3501, HATCHLING_STAR_ID);
		registerCollar(3502, HATCHLING_TWILIGHT_ID);
		registerCollar(4422, STRIDER_WIND_ID);
		registerCollar(4423, STRIDER_STAR_ID);
		registerCollar(4424, STRIDER_TWILIGHT_ID);
		registerCollar(4425, SIN_EATER_ID);
		registerCollar(6648, BABY_BUFFALO_ID);
		registerCollar(6649, BABY_COUGAR_ID);
		registerCollar(6650, BABY_KOOKABURRA_ID);
		registerCollar(8663, WYVERN_ID);
		registerCollar(9882, GREAT_WOLF_ID);
		registerCollar(10163, GREAT_WOLF_ID);
		registerCollar(10307, SNOW_GREAT_WOLF_ID);
		registerCollar(10426, FENRIR_ID);
		registerCollar(10611, SNOW_FENRIR_ID);
		registerCollar(10308, RED_STRIDER_WIND_ID);
		registerCollar(10309, RED_STRIDER_STAR_ID);
		registerCollar(10310, RED_STRIDER_TWILIGHT_ID);
		registerCollar(10311, IMPROVED_BABY_BUFFALO_ID);
		registerCollar(10312, IMPROVED_BABY_COUGAR_ID);
		registerCollar(10313, IMPROVED_BABY_KOOKABURRA_ID);
	}

	private void registerCollar(int collarItemId, int npcId) {
		collarToNpcMap.put(collarItemId, npcId);
		npcToCollarMap.putIfAbsent(npcId, collarItemId);
	}

	public void load() {
		load(Path.of("data", "xml", "player", "pet_stats.xml"));
	}

	public void load(Path path) {
		stats.clear();
		if (!Files.exists(path)) {
			log.warn("Arquivo pet_stats.xml nao encontrado em {}. Tabela vazia.", path.toAbsolutePath());
			return;
		}

		try (InputStream in = Files.newInputStream(path)) {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			Document doc = factory.newDocumentBuilder().parse(in);

			for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling()) {
				if ("list".equalsIgnoreCase(n.getNodeName())) {
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
						if ("pet".equalsIgnoreCase(d.getNodeName())) {
							NamedNodeMap a = d.getAttributes();
							String type = a.getNamedItem("type").getNodeValue();
							int typeId = Integer.parseInt(a.getNamedItem("typeID").getNodeValue());
							int level = Integer.parseInt(a.getNamedItem("level").getNodeValue());
							long expMax = Long.parseLong(a.getNamedItem("expMax").getNodeValue());
							int hpMax = Integer.parseInt(a.getNamedItem("hpMax").getNodeValue());
							int mpMax = Integer.parseInt(a.getNamedItem("mpMax").getNodeValue());
							int pAtk = Integer.parseInt(a.getNamedItem("patk").getNodeValue());
							int pDef = Integer.parseInt(a.getNamedItem("pdef").getNodeValue());
							int mAtk = Integer.parseInt(a.getNamedItem("matk").getNodeValue());
							int mDef = Integer.parseInt(a.getNamedItem("mdef").getNodeValue());
							int acc = Integer.parseInt(a.getNamedItem("acc").getNodeValue());
							int evasion = Integer.parseInt(a.getNamedItem("evasion").getNodeValue());
							int crit = Integer.parseInt(a.getNamedItem("crit").getNodeValue());
							int speed = Integer.parseInt(a.getNamedItem("speed").getNodeValue());
							int atkSpeed = Integer.parseInt(a.getNamedItem("atk_speed").getNodeValue());
							int castSpeed = Integer.parseInt(a.getNamedItem("cast_speed").getNodeValue());
							int feedMax = Integer.parseInt(a.getNamedItem("feedMax").getNodeValue());
							int feedBattle = Integer.parseInt(a.getNamedItem("feedbattle").getNodeValue());
							int feedNormal = Integer.parseInt(a.getNamedItem("feednormal").getNodeValue());
							int loadMax = Integer.parseInt(a.getNamedItem("loadMax").getNodeValue());
							int hpRegen = Integer.parseInt(a.getNamedItem("hpregen").getNodeValue());
							int mpRegen = Integer.parseInt(a.getNamedItem("mpregen").getNodeValue());
							double ownerExpTaken = Double.parseDouble(a.getNamedItem("owner_exp_taken").getNodeValue());

							PetStatTemplate tpl = new PetStatTemplate(type, typeId, level, expMax, hpMax, mpMax,
									pAtk, pDef, mAtk, mDef, acc, evasion, crit, speed, atkSpeed, castSpeed,
									feedMax, feedBattle, feedNormal, loadMax, hpRegen, mpRegen, ownerExpTaken);
							addStat(tpl);
						}
					}
				}
			}
			log.info("PetDataTable: {} niveis de mascotes carregados de {}", stats.size(), path.toAbsolutePath());
		} catch (Exception e) {
			log.error("Erro ao carregar pet_stats.xml de {}", path, e);
		}
	}

	public void addStat(PetStatTemplate tpl) {
		if (tpl != null) {
			stats.put(key(tpl.typeId(), tpl.level()), tpl);
		}
	}

	public Optional<PetStatTemplate> getStat(int npcId, int level) {
		PetStatTemplate direct = stats.get(key(npcId, level));
		if (direct != null) {
			return Optional.of(direct);
		}
		// Fallback de variantes equivalentes (Snow Great Wolf -> Great Wolf, Red Striders -> Striders, etc.)
		int baseNpcId = resolveEquivalentStatNpcId(npcId);
		if (baseNpcId != npcId) {
			return Optional.ofNullable(stats.get(key(baseNpcId, level)));
		}
		return Optional.empty();
	}

	public PetStatTemplate getStatOrFallback(int npcId, int level) {
		int lv = Math.max(1, level);
		return getStat(npcId, lv).orElseGet(() -> {
			double expTaken = npcId == SIN_EATER_ID ? 1.0 : 0.10;
			int hpMax = 100 + lv * 35;
			int mpMax = 50 + lv * 18;
			int pAtk = 15 + lv * 10;
			int pDef = 25 + lv * 7;
			int mAtk = 12 + lv * 8;
			int mDef = 20 + lv * 6;
			int acc = 35 + lv;
			int eva = 33 + lv;
			int crit = 44;
			int spd = isMountable(npcId) ? 160 : 135;
			int atkSpd = 300;
			int castSpd = 333;
			int feedMax = 300 + lv * 15;
			int feedBattle = Math.max(2, lv / 10 + 2);
			int feedNormal = Math.max(1, lv / 15 + 1);
			int loadMax = 54000;
			int hpRegen = Math.max(2, lv / 8 + 2);
			int mpRegen = Math.max(1, lv / 10 + 1);
			long expMax = (long) lv * lv * 500L;
			return new PetStatTemplate("pet", npcId, lv, expMax, hpMax, mpMax, pAtk, pDef, mAtk, mDef,
					acc, eva, crit, spd, atkSpd, castSpd, feedMax, feedBattle, feedNormal, loadMax,
					hpRegen, mpRegen, expTaken);
		});
	}

	private static int resolveEquivalentStatNpcId(int npcId) {
		return switch (npcId) {
			case BLACK_WOLF_ID -> WOLF_ID;
			case SNOW_GREAT_WOLF_ID, FENRIR_ID, SNOW_FENRIR_ID -> GREAT_WOLF_ID;
			case RED_STRIDER_WIND_ID -> STRIDER_WIND_ID;
			case RED_STRIDER_STAR_ID -> STRIDER_STAR_ID;
			case RED_STRIDER_TWILIGHT_ID -> STRIDER_TWILIGHT_ID;
			case IMPROVED_BABY_BUFFALO_ID -> BABY_BUFFALO_ID;
			case IMPROVED_BABY_KOOKABURRA_ID -> BABY_KOOKABURRA_ID;
			case IMPROVED_BABY_COUGAR_ID -> BABY_COUGAR_ID;
			default -> npcId;
		};
	}

	public boolean isPetFood(int npcId, int itemId) {
		Set<Integer> validFoods = petFoodMap.get(npcId);
		return validFoods != null && validFoods.contains(itemId);
	}

	public static boolean isAnyPetFood(int itemId) {
		return itemId == FOOD_WOLF
				|| itemId == FOOD_HATCHLING
				|| itemId == FOOD_STRIDER
				|| itemId == FOOD_STRIDER_DELUXE
				|| itemId == FOOD_WYVERN
				|| itemId == FOOD_BABY
				|| itemId == FOOD_GREAT_WOLF
				|| itemId == FOOD_IMPROVED_BABY;
	}

	public Set<Integer> getFoodIds(int npcId) {
		Set<Integer> validFoods = petFoodMap.get(npcId);
		return validFoods != null ? Collections.unmodifiableSet(validFoods) : Set.of();
	}

	public int getPrimaryFood(int npcId) {
		Set<Integer> validFoods = petFoodMap.get(npcId);
		if (validFoods != null && !validFoods.isEmpty()) {
			return validFoods.iterator().next();
		}
		return 0;
	}

	public int getFoodItemId(int npcId) {
		return getPrimaryFood(npcId);
	}

	public static int getFoodSkillId(int foodItemId) {
		return switch (foodItemId) {
			case FOOD_WOLF -> 2048;
			case FOOD_HATCHLING -> 2099;
			case FOOD_STRIDER -> 2100;
			case FOOD_STRIDER_DELUXE -> 2101;
			case FOOD_WYVERN -> 2187;
			case FOOD_BABY -> 2188;
			case FOOD_GREAT_WOLF, FOOD_IMPROVED_BABY -> 2361;
			default -> 2048;
		};
	}

	public boolean isMountable(int npcId) {
		return mountablePets.contains(npcId);
	}

	public boolean isPetCollar(int itemId) {
		return collarToNpcMap.containsKey(itemId);
	}

	public int getNpcIdByCollarItemId(int collarItemId) {
		return collarToNpcMap.getOrDefault(collarItemId, 0);
	}

	public int getCollarItemIdByNpcId(int npcId) {
		return npcToCollarMap.getOrDefault(npcId, 0);
	}

	public static boolean isWolf(int npcId) {
		return npcId == WOLF_ID || npcId == BLACK_WOLF_ID;
	}

	public static boolean isGreatWolf(int npcId) {
		return npcId == GREAT_WOLF_ID || npcId == SNOW_GREAT_WOLF_ID || npcId == FENRIR_ID || npcId == SNOW_FENRIR_ID;
	}

	public static boolean isHatchling(int npcId) {
		return npcId == HATCHLING_WIND_ID || npcId == HATCHLING_STAR_ID || npcId == HATCHLING_TWILIGHT_ID;
	}

	public static boolean isStrider(int npcId) {
		return npcId == STRIDER_WIND_ID || npcId == STRIDER_STAR_ID || npcId == STRIDER_TWILIGHT_ID
				|| npcId == RED_STRIDER_WIND_ID || npcId == RED_STRIDER_STAR_ID || npcId == RED_STRIDER_TWILIGHT_ID;
	}

	public static boolean isWyvern(int npcId) {
		return npcId == WYVERN_ID;
	}

	public static boolean isBaby(int npcId) {
		return npcId == BABY_BUFFALO_ID || npcId == BABY_KOOKABURRA_ID || npcId == BABY_COUGAR_ID
				|| npcId == IMPROVED_BABY_BUFFALO_ID || npcId == IMPROVED_BABY_KOOKABURRA_ID || npcId == IMPROVED_BABY_COUGAR_ID;
	}

	public static boolean isSinEater(int npcId) {
		return npcId == SIN_EATER_ID;
	}

	public static boolean isPetWeapon(int itemId) {
		// Presas de lobo (2500..2505, 3902..3912), chifres/garras de hatchling/strider (3913..3918, 5170..5175), great wolf (9661..9665)
		return (itemId >= 2500 && itemId <= 2505)
				|| (itemId >= 3902 && itemId <= 3918)
				|| (itemId >= 5170 && itemId <= 5175)
				|| (itemId >= 9661 && itemId <= 9665);
	}

	public static boolean isPetArmor(int itemId) {
		// Armaduras de lobo, hatchling, strider, baby pet (2506..2508, 3919..3925, 5176..5186, 9666..9667)
		// Exclui Spiritshots (2509..2514) e L2Day Scrolls (3926..3935)
		return (itemId >= 2506 && itemId <= 2508)
				|| (itemId >= 3919 && itemId <= 3925)
				|| (itemId >= 5176 && itemId <= 5186)
				|| (itemId >= 9666 && itemId <= 9667);
	}

	public int size() {
		return stats.size();
	}

	private static long key(int npcId, int level) {
		return (((long) npcId) << 32) | (level & 0xFFFFFFFFL);
	}
}
