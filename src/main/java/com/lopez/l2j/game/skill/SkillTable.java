package com.lopez.l2j.game.skill;

import com.lopez.l2j.game.skill.SkillTemplate.EffectTemplate;
import com.lopez.l2j.game.skill.SkillTemplate.OperateType;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

/**
 * Carrega os skills de data/xml/stats/skills/*.xml (porta completa de DocumentSkill/SkillTable do L2JDream/Lucera).
 * Le tabelas {@code #nome}, {@code <set>}, {@code <triggers>}, funcoes e efeitos do {@code <for>},
 * {@code <cond>} de uso e de efeito, e rotas de encantamento {@code <enchant1>}..{@code <enchant8>}.
 */
@Component
public class SkillTable {

	private static final Logger log = LoggerFactory.getLogger(SkillTable.class);

	private static final int MAX_ENCHANT_ROUTES = 8;
	private static final int[] ENCHANT_ROUTE_BASE_LEVELS = { 0, 101, 141, 181, 221, 261, 301, 341, 381 };

	private final Map<Integer, SkillTemplate[]> byId = new HashMap<>();
	private final Map<Integer, Map<Integer, SkillTemplate>> enchantedById = new HashMap<>();

	public SkillTable() {
		// Construtor vazio para testes unitarios em memoria
	}

	@Autowired
	public SkillTable(@Value("${l2.datapack.xml-dir:data/xml}") String xmlDir) {
		this(Path.of(xmlDir, "stats", "skills"));
	}

	public SkillTable(Path skillsDir) {
		if (!Files.isDirectory(skillsDir)) {
			log.warn("Diretorio de skills {} nao encontrado; nenhum skill carregado", skillsDir.toAbsolutePath());
			return;
		}
		try (Stream<Path> files = Files.list(skillsDir)) {
			DocumentBuilder builder = newBuilder();
			for (Path f : files.filter(p -> p.toString().endsWith(".xml")).sorted().toList()) {
				try (InputStream in = Files.newInputStream(f)) {
					parseFile(builder.parse(in).getDocumentElement());
				} catch (Exception e) {
					log.warn("Falha ao ler {}: {}", f.getFileName(), e.getMessage());
				}
			}
		} catch (Exception e) {
			log.warn("Falha ao listar {}: {}", skillsDir, e.getMessage());
		}
		log.info("SkillTable: {} skills carregados de {}", byId.size(), skillsDir);
	}

	public Optional<SkillTemplate> get(int id, int level) {
		if (level < 1) {
			return Optional.empty();
		}
		if (level >= 100) {
			var ench = enchantedById.get(id);
			if (ench != null && ench.containsKey(level)) {
				return Optional.ofNullable(ench.get(level));
			}
			SkillTemplate[] levels = byId.get(id);
			if (levels != null && levels.length > 0) {
				return Optional.ofNullable(levels[levels.length - 1]);
			}
			return Optional.empty();
		}
		SkillTemplate[] levels = byId.get(id);
		if (levels == null || level > levels.length) {
			return Optional.empty();
		}
		return Optional.ofNullable(levels[level - 1]);
	}

	public int maxLevel(int id) {
		SkillTemplate[] levels = byId.get(id);
		return levels == null ? 0 : levels.length;
	}

	public int size() {
		return byId.size();
	}

	public Collection<SkillTemplate> allSkillsMaxLevel() {
		List<SkillTemplate> list = new ArrayList<>(byId.size());
		for (SkillTemplate[] arr : byId.values()) {
			if (arr != null && arr.length > 0) {
				SkillTemplate last = arr[arr.length - 1];
				if (last != null) {
					list.add(last);
				}
			}
		}
		return list;
	}

	public void register(SkillTemplate skill) {
		if (skill == null || skill.level() < 1) {
			return;
		}
		if (skill.level() >= 100) {
			enchantedById.computeIfAbsent(skill.id(), k -> new HashMap<>()).put(skill.level(), skill);
			return;
		}
		SkillTemplate[] existing = byId.get(skill.id());
		int newLen = Math.max(skill.level(), existing != null ? existing.length : 0);
		SkillTemplate[] updated = new SkillTemplate[newLen];
		if (existing != null) {
			System.arraycopy(existing, 0, updated, 0, existing.length);
		}
		updated[skill.level() - 1] = skill;
		byId.put(skill.id(), updated);
	}

	// ---- parsing ----

	private static DocumentBuilder newBuilder() throws Exception {
		DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
		f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		f.setExpandEntityReferences(false);
		f.setIgnoringComments(true);
		return f.newDocumentBuilder();
	}

	private void parseFile(Element root) {
		for (Element skill : SkillCondition.children(root)) {
			if (!skill.getTagName().equals("skill")) {
				continue;
			}
			try {
				parseSkill(skill);
			} catch (RuntimeException e) {
				log.debug("Skill {} ignorado: {}", skill.getAttribute("id"), e.toString());
			}
		}
	}

	private void parseSkill(Element e) {
		int id = Integer.parseInt(e.getAttribute("id").trim());
		int levels = Integer.parseInt(e.getAttribute("levels").trim());
		String name = e.getAttribute("name");

		Map<Integer, Integer> enchantRoutes = new LinkedHashMap<>();
		for (int route = 1; route <= MAX_ENCHANT_ROUTES; route++) {
			String attr = "enchantLevels" + route;
			if (e.hasAttribute(attr)) {
				try {
					int count = Integer.parseInt(e.getAttribute(attr).trim());
					if (count > 0) {
						enchantRoutes.put(route, count);
					}
				} catch (NumberFormatException ignored) {
				}
			}
		}

		Map<String, String[]> tables = new HashMap<>();
		Map<String, String> sets = new HashMap<>();
		Element forElement = null;
		Element condElement = null;
		Element triggersElement = null;
		Map<Integer, Map<String, String>> enchantSets = new HashMap<>();
		Map<Integer, Element> enchantFors = new HashMap<>();
		Map<Integer, Element> enchantConds = new HashMap<>();

		for (Element child : SkillCondition.children(e)) {
			String tag = child.getTagName();
			switch (tag) {
				case "table" -> tables.put(child.getAttribute("name"), child.getTextContent().trim().split("\\s+"));
				case "set" -> sets.put(child.getAttribute("name"), child.getAttribute("val"));
				case "for" -> forElement = child;
				case "cond" -> condElement = child;
				case "triggers" -> triggersElement = child;
				default -> {
					if (tag.startsWith("enchant")) {
						parseEnchantTag(tag, child, enchantSets, enchantFors, enchantConds);
					}
				}
			}
		}

		if (triggersElement != null) {
			for (Element trig : SkillCondition.children(triggersElement)) {
				if ("trigger".equalsIgnoreCase(trig.getTagName())) {
					if (trig.hasAttribute("id")) {
						sets.putIfAbsent("triggerId", trig.getAttribute("id"));
						sets.putIfAbsent("triggeredId", trig.getAttribute("id"));
					}
					if (trig.hasAttribute("level")) {
						sets.putIfAbsent("triggerLevel", trig.getAttribute("level"));
						sets.putIfAbsent("triggeredLevel", trig.getAttribute("level"));
					}
					if (trig.hasAttribute("type")) {
						sets.putIfAbsent("chanceType", trig.getAttribute("type"));
					} else if (trig.hasAttribute("chanceType")) {
						sets.putIfAbsent("chanceType", trig.getAttribute("chanceType"));
					}
				}
			}
		}

		List<SkillCondition> parsedConditions = new ArrayList<>();
		SkillCondition castCond = null;
		String condMsg = null;
		if (condElement != null) {
			var kids = SkillCondition.children(condElement);
			for (Element kid : kids) {
				SkillCondition sc = SkillCondition.parse(kid);
				if (sc != null) {
					parsedConditions.add(sc);
				}
			}
			if (parsedConditions.size() == 1) {
				castCond = parsedConditions.get(0);
			} else if (parsedConditions.size() > 1) {
				List<SkillCondition> copy = List.copyOf(parsedConditions);
				castCond = (p, target) -> copy.stream().allMatch(sc -> sc.test(p, target));
			}
			condMsg = condElement.getAttribute("msg");
		}

		int weaponsAllowed = 0;
		if (sets.containsKey("weaponsAllowed")) {
			try {
				weaponsAllowed = Integer.parseInt(sets.get("weaponsAllowed").trim());
			} catch (NumberFormatException ignored) {
			}
		}
		if (weaponsAllowed > 0) {
			final int mask = weaponsAllowed;
			SkillCondition wCond = (p, target) -> SkillCondition.checkWeaponsAllowed(p, mask);
			parsedConditions.add(wCond);
			final SkillCondition baseCond = castCond;
			castCond = baseCond != null ? ((p, target) -> wCond.test(p, target) && baseCond.test(p, target)) : wCond;
			if (condMsg == null || condMsg.isBlank()) {
				condMsg = "Equipamento incorreto para usar esta habilidade.";
			}
		}

		SkillTemplate[] out = new SkillTemplate[levels];
		for (int lvl = 1; lvl <= levels; lvl++) {
			var r = new Resolver(tables, lvl);
			String parsedSkillType = r.str(sets.get("skillType"), "NOTDONE").toUpperCase(Locale.ROOT);
			boolean isDebuffFlag = r.bool(sets.get("isDebuff"), false);
			if (isDebuffFlag && "NOTDONE".equalsIgnoreCase(parsedSkillType)) {
				parsedSkillType = "DEBUFF";
			}
			boolean isDebuffSkill = isDebuffFlag || "DEBUFF".equalsIgnoreCase(parsedSkillType)
					|| "POISON".equalsIgnoreCase(parsedSkillType) || "BLEED".equalsIgnoreCase(parsedSkillType)
					|| "WEAKNESS".equalsIgnoreCase(parsedSkillType) || "DOT".equalsIgnoreCase(parsedSkillType)
					|| "MDOT".equalsIgnoreCase(parsedSkillType) || "AGGDEBUFF".equalsIgnoreCase(parsedSkillType)
					|| "CONT".equalsIgnoreCase(parsedSkillType);

			List<StatFunc> funcs = new ArrayList<>();
			List<EffectTemplate> effects = new ArrayList<>();
			if (forElement != null) {
				for (Element f : SkillCondition.children(forElement)) {
					if (f.getTagName().equals("effect")) {
						effects.add(parseEffect(id, f, r, isDebuffSkill));
					} else {
						StatFunc func = parseFunc(f, r);
						if (func != null) {
							funcs.add(func);
						}
					}
				}
			}

			String op = sets.getOrDefault("operateType", "OP_ACTIVE").toUpperCase(Locale.ROOT);
			OperateType operate = switch (op) {
				case "OP_PASSIVE", "OP_CHANCE" -> OperateType.PASSIVE;
				case "OP_TOGGLE" -> OperateType.TOGGLE;
				default -> OperateType.ACTIVE;
			};

			int displayId = r.integer(sets.get("displayId"), id);
			boolean staticHitTime = r.bool(sets.get("staticHitTime"), false);
			boolean staticReuse = r.bool(sets.get("staticReuse"), false);
			boolean isDance = sets.containsKey("isDance") ? r.bool(sets.get("isDance"), false)
					: (sets.containsKey("isSong") ? r.bool(sets.get("isSong"), false) : isDefaultDanceOrSong(id, name));
			boolean isPotion = sets.containsKey("isPotion") ? r.bool(sets.get("isPotion"), false)
					: "POTION".equalsIgnoreCase(parsedSkillType);
			boolean ignoreShld = r.bool(sets.get("ignoreShld"), false);
			boolean overHit = r.bool(sets.get("overHit"), false);
			int baseCritRate = r.integer(sets.getOrDefault("baseCritRate", sets.get("criticalRate")), 0);
			int lethal1 = r.integer(sets.get("lethal1"), 0);
			int lethal2 = r.integer(sets.get("lethal2"), 0);

			int rawNumCharges = r.integer(sets.getOrDefault("num_charges", sets.get("numCharges")), 0);
			int giveCharges = r.integer(sets.get("giveCharges"), 0);
			int needCharges = r.integer(sets.get("needCharges"), 0);
			if (rawNumCharges > 0) {
				if ("CHARGE".equalsIgnoreCase(parsedSkillType) || "CHARGE_EFFECT".equalsIgnoreCase(parsedSkillType)
						|| id == 345 || id == 346) {
					if (giveCharges == 0) {
						giveCharges = rawNumCharges;
					}
				} else if ("CHARGEDAM".equalsIgnoreCase(parsedSkillType) || "PDAM".equalsIgnoreCase(parsedSkillType)) {
					if (needCharges == 0) {
						needCharges = rawNumCharges;
					}
				}
			}
			int maxCharges = r.integer(sets.getOrDefault("maxCharges", sets.get("max_charges")), 0);
			int numCharges = rawNumCharges > 0 ? rawNumCharges : Math.max(giveCharges, needCharges);

			int triggerId = r.integer(sets.getOrDefault("triggerId", sets.get("triggeredId")), 0);
			int triggerLevel = r.integer(sets.getOrDefault("triggerLevel", sets.get("triggeredLevel")), 0);
			String chanceType = r.str(sets.get("chanceType"), null);
			int triggeredId = r.integer(sets.getOrDefault("triggeredId", sets.get("triggerId")), triggerId);
			int triggeredLevel = r.integer(sets.getOrDefault("triggeredLevel", sets.get("triggerLevel")), triggerLevel);
			int npcId = r.integer(sets.getOrDefault("npcId", sets.get("effectNpcId")), 0);
			String element = r.str(sets.get("element"), null);
			String saveVs = r.str(sets.get("saveVs"), null);
			int lvlDepend = r.integer(sets.get("lvlDepend"), 0);
			double ssBoost = r.number(sets.get("ssBoost"), 0.0);
			String effectType = r.str(sets.get("effectType"), null);
			double effectPower = r.number(sets.get("effectPower"), 0.0);

			String targetStr = r.str(sets.get("target"), "TARGET_SELF").toUpperCase(Locale.ROOT);
			boolean continueAfterMax = sets.containsKey("continueAfterMax")
					? r.bool(sets.get("continueAfterMax"), false)
					: (giveCharges > 0 && !"TARGET_SELF".equalsIgnoreCase(targetStr));

			out[lvl - 1] = new SkillTemplate(
					id, lvl, displayId <= 0 ? id : displayId, name, operate,
					parsedSkillType,
					targetStr,
					r.bool(sets.get("isMagic")),
					r.integer(sets.get("mpConsume"), 0),
					r.integer(sets.get("mpInitialConsume"), 0),
					r.integer(sets.get("hpConsume"), 0),
					r.number(sets.get("power"), 0),
					r.integer(sets.get("castRange"), 0),
					r.integer(sets.get("skillRadius"), 80),
					r.integer(sets.get("hitTime"), 0),
					r.integer(sets.get("coolTime"), 0),
					r.integer(sets.get("reuseDelay"), 0),
					r.integer(sets.get("magicLvl"), 0),
					r.number(sets.get("absorbPart"), 0),
					r.bool(sets.get("nextActionAttack")),
					r.integer(sets.get("itemConsumeId"), 0),
					r.integer(sets.get("itemConsumeCount"), 0),
					giveCharges, maxCharges, needCharges,
					r.bool(sets.get("consumeCharges"), true),
					continueAfterMax,
					staticHitTime, staticReuse,
					isDance, isPotion, ignoreShld, overHit,
					baseCritRate, lethal1, lethal2, numCharges,
					triggerId, triggerLevel, chanceType, triggeredId, triggeredLevel,
					npcId, element, saveVs, lvlDepend, ssBoost, effectType, effectPower,
					funcs, effects, castCond, condMsg, parsedConditions, enchantRoutes);
		}
		byId.put(id, out);

		if (levels > 0 && !enchantRoutes.isEmpty()) {
			SkillTemplate base = out[levels - 1];
			for (Map.Entry<Integer, Integer> entry : enchantRoutes.entrySet()) {
				int route = entry.getKey();
				int count = entry.getValue();
				if (count <= 0 || route < 1 || route > MAX_ENCHANT_ROUTES) {
					continue;
				}
				int start = ENCHANT_ROUTE_BASE_LEVELS[route];
				var routeSets = enchantSets.getOrDefault(route, Map.of());
				Element routeFor = enchantFors.getOrDefault(route, forElement);
				Element routeCond = enchantConds.getOrDefault(route, condElement);

				List<SkillCondition> rConditions = new ArrayList<>();
				SkillCondition rCastCond = castCond;
				String rCondMsg = condMsg;
				int rWeaponsAllowed = weaponsAllowed;
				if (routeSets.containsKey("weaponsAllowed")) {
					try {
						rWeaponsAllowed = Integer.parseInt(routeSets.get("weaponsAllowed").trim());
					} catch (NumberFormatException ignored) {
					}
				}

				if (routeCond != null && routeCond != condElement) {
					rCastCond = null;
					var kids = SkillCondition.children(routeCond);
					for (Element kid : kids) {
						SkillCondition sc = SkillCondition.parse(kid);
						if (sc != null) {
							rConditions.add(sc);
						}
					}
					if (rConditions.size() == 1) {
						rCastCond = rConditions.get(0);
					} else if (rConditions.size() > 1) {
						List<SkillCondition> copy = List.copyOf(rConditions);
						rCastCond = (p, target) -> copy.stream().allMatch(sc -> sc.test(p, target));
					}
					rCondMsg = routeCond.getAttribute("msg");
					if (rWeaponsAllowed > 0) {
						final int mask = rWeaponsAllowed;
						SkillCondition wCond = (p, target) -> SkillCondition.checkWeaponsAllowed(p, mask);
						rConditions.add(wCond);
						final SkillCondition baseCond = rCastCond;
						rCastCond = baseCond != null ? ((p, target) -> wCond.test(p, target) && baseCond.test(p, target)) : wCond;
					}
				} else {
					rConditions.addAll(parsedConditions);
					if (rWeaponsAllowed > 0 && rWeaponsAllowed != weaponsAllowed) {
						final int mask = rWeaponsAllowed;
						SkillCondition wCond = (p, target) -> SkillCondition.checkWeaponsAllowed(p, mask);
						rConditions.add(wCond);
						final SkillCondition baseCond = rCastCond;
						rCastCond = baseCond != null ? ((p, target) -> wCond.test(p, target) && baseCond.test(p, target)) : wCond;
					}
				}

				for (int i = 0; i < count; i++) {
					int lvl = start + i;
					var r = new Resolver(tables, i + 1);

					List<StatFunc> funcs = new ArrayList<>();
					List<EffectTemplate> effects = new ArrayList<>();
					String skillType = routeSets.containsKey("skillType")
							? r.str(routeSets.get("skillType"), base.skillType()).toUpperCase(Locale.ROOT)
							: base.skillType();
					boolean routeDebuff = base.isDebuff() || "DEBUFF".equalsIgnoreCase(skillType);
					if (routeFor != null) {
						for (Element f : SkillCondition.children(routeFor)) {
							if (f.getTagName().equals("effect")) {
								effects.add(parseEffect(id, f, r, routeDebuff));
							} else {
								StatFunc func = parseFunc(f, r);
								if (func != null) {
									funcs.add(func);
								}
							}
						}
					}

					String target = routeSets.containsKey("target") ? r.str(routeSets.get("target"), base.target()).toUpperCase(Locale.ROOT) : base.target();
					boolean magic = routeSets.containsKey("isMagic") ? r.bool(routeSets.get("isMagic")) : base.magic();
					int mpConsume = routeSets.containsKey("mpConsume") ? r.integer(routeSets.get("mpConsume"), base.mpConsume()) : base.mpConsume();
					int mpInitial = routeSets.containsKey("mpInitialConsume") ? r.integer(routeSets.get("mpInitialConsume"), base.mpInitialConsume()) : base.mpInitialConsume();
					int hpConsume = routeSets.containsKey("hpConsume") ? r.integer(routeSets.get("hpConsume"), base.hpConsume()) : base.hpConsume();
					double power = routeSets.containsKey("power") ? r.number(routeSets.get("power"), base.power()) : base.power();
					int castRange = routeSets.containsKey("castRange") ? r.integer(routeSets.get("castRange"), base.castRange()) : base.castRange();
					int skillRadius = routeSets.containsKey("skillRadius") ? r.integer(routeSets.get("skillRadius"), base.skillRadius()) : base.skillRadius();
					int hitTime = routeSets.containsKey("hitTime") ? r.integer(routeSets.get("hitTime"), base.hitTime()) : base.hitTime();
					int coolTime = routeSets.containsKey("coolTime") ? r.integer(routeSets.get("coolTime"), base.coolTime()) : base.coolTime();
					int reuseDelay = routeSets.containsKey("reuseDelay") ? r.integer(routeSets.get("reuseDelay"), base.reuseDelay()) : base.reuseDelay();
					int magicLvl = routeSets.containsKey("magicLvl") ? r.integer(routeSets.get("magicLvl"), base.magicLevel()) : base.magicLevel();
					double absorb = routeSets.containsKey("absorbPart") ? r.number(routeSets.get("absorbPart"), base.absorbPart()) : base.absorbPart();

					int displayId = routeSets.containsKey("displayId") ? r.integer(routeSets.get("displayId"), base.displayId()) : base.displayId();
					boolean staticHitTime = routeSets.containsKey("staticHitTime") ? r.bool(routeSets.get("staticHitTime"), base.staticHitTime()) : base.staticHitTime();
					boolean staticReuse = routeSets.containsKey("staticReuse") ? r.bool(routeSets.get("staticReuse"), base.staticReuse()) : base.staticReuse();
					boolean ignoreShld = routeSets.containsKey("ignoreShld") ? r.bool(routeSets.get("ignoreShld"), base.ignoreShld()) : base.ignoreShld();
					boolean overHit = routeSets.containsKey("overHit") ? r.bool(routeSets.get("overHit"), base.overHit()) : base.overHit();
					int baseCritRate = routeSets.containsKey("baseCritRate") ? r.integer(routeSets.get("baseCritRate"), base.baseCritRate()) : base.baseCritRate();
					int lethal1 = routeSets.containsKey("lethal1") ? r.integer(routeSets.get("lethal1"), base.lethal1()) : base.lethal1();
					int lethal2 = routeSets.containsKey("lethal2") ? r.integer(routeSets.get("lethal2"), base.lethal2()) : base.lethal2();
					double effectPower = routeSets.containsKey("effectPower") ? r.number(routeSets.get("effectPower"), base.effectPower()) : base.effectPower();

					SkillTemplate st = new SkillTemplate(
							id, lvl, displayId <= 0 ? id : displayId, name, base.operateType(),
							skillType, target, magic, mpConsume, mpInitial, hpConsume, power,
							castRange, skillRadius, hitTime, coolTime, reuseDelay, magicLvl,
							absorb, base.nextActionAttack(),
							base.itemConsumeId(), base.itemConsumeCount(),
							base.giveCharges(), base.maxCharges(), base.needCharges(), base.consumeCharges(),
							base.continueAfterMax(),
							staticHitTime, staticReuse,
							base.isDance(), base.isPotion(), ignoreShld, overHit,
							baseCritRate, lethal1, lethal2, base.numCharges(),
							base.triggerId(), base.triggerLevel(), base.chanceType(),
							base.triggeredId(), base.triggeredLevel(), base.npcId(),
							base.element(), base.saveVs(), base.lvlDepend(), base.ssBoost(),
							base.effectType(), effectPower,
							funcs.isEmpty() && routeFor == forElement ? base.funcs() : funcs,
							effects.isEmpty() && routeFor == forElement ? base.effects() : effects,
							rCastCond, rCondMsg, rConditions, enchantRoutes);
					enchantedById.computeIfAbsent(id, k -> new HashMap<>()).put(lvl, st);
				}
			}
		}
	}

	private static void parseEnchantTag(
			String tag,
			Element child,
			Map<Integer, Map<String, String>> enchantSets,
			Map<Integer, Element> enchantFors,
			Map<Integer, Element> enchantConds) {
		for (int route = 1; route <= MAX_ENCHANT_ROUTES; route++) {
			String prefix = "enchant" + route;
			if (tag.equals(prefix)) {
				enchantSets.computeIfAbsent(route, k -> new HashMap<>()).put(child.getAttribute("name"), child.getAttribute("val"));
				return;
			}
			if (tag.equals(prefix + "for")) {
				enchantFors.put(route, child);
				return;
			}
			if (tag.equals(prefix + "cond")) {
				enchantConds.put(route, child);
				return;
			}
		}
	}

	private static boolean isDefaultDanceOrSong(int id, String name) {
		if ((id >= 264 && id <= 277) || (id >= 304 && id <= 311) || id == 349 || (id >= 363 && id <= 367)) {
			return true;
		}
		if (name != null) {
			String lower = name.toLowerCase(Locale.ROOT);
			return lower.startsWith("song of ") || lower.startsWith("dance of ") || lower.contains("siren's dance");
		}
		return false;
	}

	private static EffectTemplate parseEffect(int skillId, Element e, Resolver r, boolean isDebuffSkill) {
		List<StatFunc> funcs = new ArrayList<>();
		SkillCondition effectCond = null;
		for (Element f : SkillCondition.children(e)) {
			if ("cond".equalsIgnoreCase(f.getTagName())) {
				var kids = SkillCondition.children(f);
				if (kids.size() == 1) {
					effectCond = SkillCondition.parse(kids.get(0));
				} else if (kids.size() > 1) {
					List<SkillCondition> parsedKids = new ArrayList<>();
					for (Element kid : kids) {
						SkillCondition sc = SkillCondition.parse(kid);
						if (sc != null) {
							parsedKids.add(sc);
						}
					}
					if (!parsedKids.isEmpty()) {
						effectCond = (p, target) -> parsedKids.stream().allMatch(c -> c.test(p, target));
					}
				}
				continue;
			}
			StatFunc func = parseFunc(f, r);
			if (func != null) {
				funcs.add(func);
			}
		}
		String stack = r.str(e.getAttribute("stackType"), "none");
		int baseTime = r.integer(e.getAttribute("time"), 0);
		String effectName = e.getAttribute("name");
		boolean isHotSpringsBuff = skillId >= 4551 && skillId <= 4554;
		boolean skipBuffMultiplier = !isHotSpringsBuff && (isDebuffSkill
				|| "Debuff".equalsIgnoreCase(effectName)
				|| "DamOverTime".equalsIgnoreCase(effectName)
				|| "ManaDamOverTime".equalsIgnoreCase(effectName)
				|| "Poison".equalsIgnoreCase(effectName)
				|| "Bleed".equalsIgnoreCase(effectName)
				|| "Fear".equalsIgnoreCase(effectName)
				|| "Stun".equalsIgnoreCase(effectName)
				|| "Sleep".equalsIgnoreCase(effectName)
				|| "Root".equalsIgnoreCase(effectName)
				|| "Paralyze".equalsIgnoreCase(effectName)
				|| "Mute".equalsIgnoreCase(effectName)
				|| "SilenceMagicPhysical".equalsIgnoreCase(effectName)
				|| "PhysicalMute".equalsIgnoreCase(effectName)
				|| "Petrification".equalsIgnoreCase(effectName)
				|| "Petrify".equalsIgnoreCase(effectName)
				|| "Medusa".equalsIgnoreCase(effectName)
				|| "StunSelf".equalsIgnoreCase(effectName)
				|| "ConfuseMob".equalsIgnoreCase(effectName)
				|| "Confusion".equalsIgnoreCase(effectName)
				|| "BigHead".equalsIgnoreCase(effectName)
				|| "Grow".equalsIgnoreCase(effectName));
		int effectiveTime = skipBuffMultiplier ? baseTime : resolveBuffDurationSeconds(skillId, baseTime);
		String abnormalAttr = r.str(e.getAttribute("abnormal"), null);
		int abnormalMask = AbnormalEffect.parseMask(abnormalAttr, effectName);
		double effectPower = r.number(e.getAttribute("effectPower"), 0.0);
		String effectType = r.str(e.getAttribute("effectType"), null);
		return new EffectTemplate(effectName, r.integer(e.getAttribute("count"), 1),
				effectiveTime, r.number(e.getAttribute("val"), 0),
				stack.isBlank() ? "none" : stack, r.number(e.getAttribute("stackOrder"), 0),
				abnormalMask, abnormalAttr, effectPower, effectType, effectCond, funcs);
	}

	/**
	 * Aplica os multiplicadores de duracao de buffs do altgame.properties (ALT_BUFFER_TIME, ALT_5MIN_TIME,
	 * ALT_DANCE_TIME, ALT_SONG_TIME, ALT_HERO_TIME, ALT_CH_TIME), exceto para skills em UNAFFECTED_SKILLS.
	 */
	public static int resolveBuffDurationSeconds(int skillId, int baseTimeSeconds) {
		if (baseTimeSeconds <= 0 || isUnaffectedSkill(skillId)) {
			return baseTimeSeconds;
		}
		float mult = 1.0f;
		if (skillId == 395 || skillId == 396 || (skillId >= 1374 && skillId <= 1376)) {
			mult = com.lopez.l2j.config.Config.ALT_HERO_TIME;
		} else if ((skillId >= 271 && skillId <= 277) || skillId == 307 || skillId == 309 || skillId == 310 || skillId == 311 || skillId == 365 || skillId == 366 || skillId == 367) {
			mult = com.lopez.l2j.config.Config.ALT_DANCE_TIME;
		} else if ((skillId >= 264 && skillId <= 270) || (skillId >= 304 && skillId <= 306) || skillId == 308 || skillId == 349 || skillId == 363 || skillId == 364) {
			mult = com.lopez.l2j.config.Config.ALT_SONG_TIME;
		} else if (skillId >= 4342 && skillId <= 4360) {
			mult = com.lopez.l2j.config.Config.ALT_CH_TIME;
		} else if (baseTimeSeconds >= 240 && baseTimeSeconds <= 300) {
			mult = com.lopez.l2j.config.Config.ALT_5MIN_TIME;
		} else if (baseTimeSeconds > 300 && baseTimeSeconds <= 1800) {
			mult = com.lopez.l2j.config.Config.ALT_BUFFER_TIME;
		}
		if (mult <= 0f || mult == 1.0f) {
			return baseTimeSeconds;
		}
		return Math.max(1, Math.round(baseTimeSeconds * mult));
	}

	public static boolean isUnaffectedSkill(int skillId) {
		String raw = com.lopez.l2j.config.Config.UNAFFECTED_SKILLS;
		if (raw == null || raw.isBlank() || "0".equals(raw.trim())) {
			return false;
		}
		for (String part : raw.split(",")) {
			try {
				if (Integer.parseInt(part.trim()) == skillId) {
					return true;
				}
			} catch (NumberFormatException ignored) {
			}
		}
		return false;
	}

	private static StatFunc parseFunc(Element f, Resolver r) {
		StatFunc.Op op = StatFunc.Op.parse(f.getTagName());
		if (op == null || !f.hasAttribute("stat")) {
			return null;
		}
		SkillCondition cond = null;
		var kids = SkillCondition.children(f);
		if (!kids.isEmpty()) {
			List<SkillCondition> parsedKids = new ArrayList<>();
			for (Element kid : kids) {
				SkillCondition sc = SkillCondition.parse(kid);
				if (sc != null) {
					parsedKids.add(sc);
				}
			}
			if (parsedKids.size() == 1) {
				cond = parsedKids.get(0);
			} else if (parsedKids.size() > 1) {
				cond = (p, target) -> parsedKids.stream().allMatch(c -> c.test(p, target));
			}
		}
		return new StatFunc(f.getAttribute("stat"), op, parseOrder(f.getAttribute("order")),
				r.number(f.getAttribute("val"), 0), cond);
	}

	private static int parseOrder(String s) {
		if (s == null || s.isBlank()) {
			return 0;
		}
		s = s.trim();
		try {
			return s.startsWith("0x") || s.startsWith("0X") ? Integer.parseInt(s.substring(2), 16)
					: Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/** Resolve valores {@code #tabela} para o nivel corrente (tabelas curtas repetem o ultimo valor). */
	private record Resolver(Map<String, String[]> tables, int level) {

		String str(String raw, String def) {
			if (raw == null || raw.isBlank()) {
				return def;
			}
			raw = raw.trim();
			if (raw.startsWith("#")) {
				String[] t = tables.get(raw);
				if (t == null || t.length == 0) {
					return def;
				}
				return t[Math.min(level, t.length) - 1];
			}
			return raw;
		}

		double number(String raw, double def) {
			String s = str(raw, null);
			if (s == null) {
				return def;
			}
			try {
				return Double.parseDouble(s);
			} catch (NumberFormatException e) {
				return def;
			}
		}

		int integer(String raw, int def) {
			return (int) Math.round(number(raw, def));
		}

		boolean bool(String raw, boolean def) {
			String s = str(raw, null);
			if (s == null) {
				return def;
			}
			return Boolean.parseBoolean(s);
		}

		boolean bool(String raw) {
			return bool(raw, false);
		}
	}
}
