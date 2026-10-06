package com.lopez.l2j.game.pet;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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
 * Porta de PetDataTable de L2JDream.
 */
@Component
public class PetDataTable {

	private static final Logger log = LoggerFactory.getLogger(PetDataTable.class);

	// IDs de NPCs de mascotes
	public static final int WOLF_ID = 12077;
	public static final int GREAT_WOLF_ID = 16025;
	public static final int HATCHLING_WIND_ID = 12311;
	public static final int HATCHLING_STAR_ID = 12312;
	public static final int HATCHLING_TWILIGHT_ID = 12313;
	public static final int STRIDER_WIND_ID = 12526;
	public static final int STRIDER_STAR_ID = 12527;
	public static final int STRIDER_TWILIGHT_ID = 12528;
	public static final int WYVERN_ID = 12621;
	public static final int BABY_BUFFALO_ID = 12780;
	public static final int BABY_KOOKABURRA_ID = 12781;
	public static final int BABY_COUGAR_ID = 12782;
	public static final int SIN_EATER_ID = 12564;

	// Alimentos oficiais
	public static final int FOOD_WOLF = 2515;
	public static final int FOOD_HATCHLING = 4038;
	public static final int FOOD_STRIDER = 5168;
	public static final int FOOD_STRIDER_DELUXE = 5169;
	public static final int FOOD_WYVERN = 6316;
	public static final int FOOD_BABY = 7582;
	public static final int FOOD_GREAT_WOLF = 9668;

	private final Map<Long, PetStatTemplate> stats = new HashMap<>();
	private final Map<Integer, Set<Integer>> petFoodMap = new HashMap<>();
	private final Set<Integer> mountablePets = Set.of(
			STRIDER_WIND_ID, STRIDER_STAR_ID, STRIDER_TWILIGHT_ID,
			WYVERN_ID, GREAT_WOLF_ID
	);

	@Autowired
	public PetDataTable(@Value("${l2j.data.dir:data}") String dataDir) {
		initFoodMap();
		load(Path.of(dataDir, "xml", "player", "pet_stats.xml"));
	}

	public PetDataTable(Path xmlPath) {
		initFoodMap();
		load(xmlPath);
	}

	public PetDataTable() {
		initFoodMap();
	}

	private void initFoodMap() {
		petFoodMap.put(WOLF_ID, Set.of(FOOD_WOLF));
		petFoodMap.put(SIN_EATER_ID, Set.of(FOOD_WOLF));
		petFoodMap.put(GREAT_WOLF_ID, Set.of(FOOD_GREAT_WOLF, FOOD_WOLF));
		petFoodMap.put(HATCHLING_WIND_ID, Set.of(FOOD_HATCHLING));
		petFoodMap.put(HATCHLING_STAR_ID, Set.of(FOOD_HATCHLING));
		petFoodMap.put(HATCHLING_TWILIGHT_ID, Set.of(FOOD_HATCHLING));
		petFoodMap.put(STRIDER_WIND_ID, Set.of(FOOD_STRIDER, FOOD_STRIDER_DELUXE));
		petFoodMap.put(STRIDER_STAR_ID, Set.of(FOOD_STRIDER, FOOD_STRIDER_DELUXE));
		petFoodMap.put(STRIDER_TWILIGHT_ID, Set.of(FOOD_STRIDER, FOOD_STRIDER_DELUXE));
		petFoodMap.put(WYVERN_ID, Set.of(FOOD_WYVERN));
		petFoodMap.put(BABY_BUFFALO_ID, Set.of(FOOD_BABY));
		petFoodMap.put(BABY_KOOKABURRA_ID, Set.of(FOOD_BABY));
		petFoodMap.put(BABY_COUGAR_ID, Set.of(FOOD_BABY));
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
							int ownerExpTaken = Integer.parseInt(a.getNamedItem("owner_exp_taken").getNodeValue());

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
		return Optional.ofNullable(stats.get(key(npcId, level)));
	}

	public boolean isPetFood(int npcId, int itemId) {
		Set<Integer> validFoods = petFoodMap.get(npcId);
		return validFoods != null && validFoods.contains(itemId);
	}

	public int getPrimaryFood(int npcId) {
		Set<Integer> validFoods = petFoodMap.get(npcId);
		if (validFoods != null && !validFoods.isEmpty()) {
			return validFoods.iterator().next();
		}
		return 0;
	}

	public boolean isMountable(int npcId) {
		return mountablePets.contains(npcId);
	}

	public int size() {
		return stats.size();
	}

	private static long key(int npcId, int level) {
		return (((long) npcId) << 32) | (level & 0xFFFFFFFFL);
	}
}
