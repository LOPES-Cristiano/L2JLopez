package com.lopez.l2j.game.skill;

import com.lopez.l2j.game.skill.SkillTemplate.EffectTemplate;
import com.lopez.l2j.game.skill.SkillTemplate.OperateType;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
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
 * Carrega os skills de data/xml/stats/skills/*.xml (porta enxuta de DocumentSkill/SkillTable do L2JDream).
 * Le tabelas {@code #nome}, {@code <set>}, funcoes e efeitos do {@code <for>} e a {@code <cond>} de uso. Niveis de
 * encantamento (101+) ainda nao sao carregados.
 */
@Component
public class SkillTable {

	private static final Logger log = LoggerFactory.getLogger(SkillTable.class);

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
			return ench != null ? Optional.ofNullable(ench.get(level)) : Optional.empty();
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

		int enchantLevels1 = e.hasAttribute("enchantLevels1") ? Integer.parseInt(e.getAttribute("enchantLevels1").trim()) : 0;
		int enchantLevels2 = e.hasAttribute("enchantLevels2") ? Integer.parseInt(e.getAttribute("enchantLevels2").trim()) : 0;

		Map<String, String[]> tables = new HashMap<>();
		Map<String, String> sets = new HashMap<>();
		Element forElement = null;
		Element condElement = null;
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
				case "enchant1" -> enchantSets.computeIfAbsent(1, k -> new HashMap<>()).put(child.getAttribute("name"), child.getAttribute("val"));
				case "enchant2" -> enchantSets.computeIfAbsent(2, k -> new HashMap<>()).put(child.getAttribute("name"), child.getAttribute("val"));
				case "enchant1for" -> enchantFors.put(1, child);
				case "enchant2for" -> enchantFors.put(2, child);
				case "enchant1cond" -> enchantConds.put(1, child);
				case "enchant2cond" -> enchantConds.put(2, child);
				default -> {
				}
			}
		}

		SkillCondition castCond = null;
		String condMsg = null;
		if (condElement != null) {
			var kids = SkillCondition.children(condElement);
			if (kids.size() == 1) {
				castCond = SkillCondition.parse(kids.get(0));
			} else if (kids.size() > 1) {
				List<SkillCondition> parsedKids = new ArrayList<>();
				for (Element kid : kids) {
					SkillCondition sc = SkillCondition.parse(kid);
					if (sc != null) {
						parsedKids.add(sc);
					}
				}
				if (!parsedKids.isEmpty()) {
					castCond = (p, target) -> parsedKids.stream().allMatch(sc -> sc.test(p, target));
				}
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
			final SkillCondition baseCond = castCond;
			castCond = baseCond != null ? ((p, target) -> wCond.test(p, target) && baseCond.test(p, target)) : wCond;
			if (condMsg == null || condMsg.isBlank()) {
				condMsg = "Equipamento incorreto para usar esta habilidade.";
			}
		}

		SkillTemplate[] out = new SkillTemplate[levels];
		for (int lvl = 1; lvl <= levels; lvl++) {
			var r = new Resolver(tables, lvl);
			List<StatFunc> funcs = new ArrayList<>();
			List<EffectTemplate> effects = new ArrayList<>();
			if (forElement != null) {
				for (Element f : SkillCondition.children(forElement)) {
					if (f.getTagName().equals("effect")) {
						effects.add(parseEffect(f, r));
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
			out[lvl - 1] = new SkillTemplate(id, lvl, name, operate,
					r.str(sets.get("skillType"), "NOTDONE").toUpperCase(Locale.ROOT),
					r.str(sets.get("target"), "TARGET_SELF").toUpperCase(Locale.ROOT),
					r.bool(sets.get("isMagic")), r.integer(sets.get("mpConsume"), 0),
					r.integer(sets.get("mpInitialConsume"), 0), r.integer(sets.get("hpConsume"), 0),
					r.number(sets.get("power"), 0), r.integer(sets.get("castRange"), 0),
					r.integer(sets.get("skillRadius"), 80), r.integer(sets.get("hitTime"), 0),
					r.integer(sets.get("coolTime"), 0), r.integer(sets.get("reuseDelay"), 0),
					r.integer(sets.get("magicLvl"), 0), r.number(sets.get("absorbPart"), 0),
					r.bool(sets.get("nextActionAttack")),
					r.integer(sets.get("itemConsumeId"), 0), r.integer(sets.get("itemConsumeCount"), 0),
					r.integer(sets.get("giveCharges"), 0), r.integer(sets.get("maxCharges"), 0),
					r.integer(sets.get("needCharges"), 0), r.bool(sets.get("consumeCharges"), true),
					sets.containsKey("continueAfterMax")
							? r.bool(sets.get("continueAfterMax"), false)
							: (r.integer(sets.get("giveCharges"), 0) > 0 && !"TARGET_SELF".equalsIgnoreCase(r.str(sets.get("target"), "TARGET_SELF"))),
					List.copyOf(funcs), List.copyOf(effects), castCond,
					condMsg);
		}
		byId.put(id, out);

		if (levels > 0 && (enchantLevels1 > 0 || enchantLevels2 > 0)) {
			SkillTemplate base = out[levels - 1];
			int[] routeStarts = { 0, 101, 141 };
			int[] routeCounts = { 0, enchantLevels1, enchantLevels2 };
			for (int route = 1; route <= 2; route++) {
				int count = routeCounts[route];
				if (count <= 0) continue;
				int start = routeStarts[route];
				var routeSets = enchantSets.getOrDefault(route, Map.of());
				Element routeFor = enchantFors.getOrDefault(route, forElement);
				Element routeCond = enchantConds.getOrDefault(route, condElement);

				SkillCondition rCastCond = castCond;
				String rCondMsg = condMsg;
				if (routeCond != null && routeCond != condElement) {
					var kids = SkillCondition.children(routeCond);
					if (kids.size() == 1) {
						rCastCond = SkillCondition.parse(kids.get(0));
					} else if (kids.size() > 1) {
						List<SkillCondition> parsedKids = new ArrayList<>();
						for (Element kid : kids) {
							SkillCondition sc = SkillCondition.parse(kid);
							if (sc != null) parsedKids.add(sc);
						}
						if (!parsedKids.isEmpty()) {
							rCastCond = (p, target) -> parsedKids.stream().allMatch(sc -> sc.test(p, target));
						}
					}
					rCondMsg = routeCond.getAttribute("msg");
				}
				int rWeaponsAllowed = weaponsAllowed;
				if (routeSets.containsKey("weaponsAllowed")) {
					try {
						rWeaponsAllowed = Integer.parseInt(routeSets.get("weaponsAllowed").trim());
					} catch (NumberFormatException ignored) {}
				}
				if (rWeaponsAllowed > 0 && rCastCond != castCond) {
					final int mask = rWeaponsAllowed;
					SkillCondition wCond = (p, target) -> SkillCondition.checkWeaponsAllowed(p, mask);
					final SkillCondition baseCond = rCastCond;
					rCastCond = baseCond != null ? ((p, target) -> wCond.test(p, target) && baseCond.test(p, target)) : wCond;
				}

				for (int i = 0; i < count; i++) {
					int lvl = start + i;
					var r = new Resolver(tables, i + 1);

					List<StatFunc> funcs = new ArrayList<>();
					List<EffectTemplate> effects = new ArrayList<>();
					if (routeFor != null) {
						for (Element f : SkillCondition.children(routeFor)) {
							if (f.getTagName().equals("effect")) {
								effects.add(parseEffect(f, r));
							} else {
								StatFunc func = parseFunc(f, r);
								if (func != null) {
									funcs.add(func);
								}
							}
						}
					}

					String skillType = routeSets.containsKey("skillType") ? r.str(routeSets.get("skillType"), base.skillType()).toUpperCase(Locale.ROOT) : base.skillType();
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

					SkillTemplate st = new SkillTemplate(id, lvl, name, base.operateType(),
							skillType, target, magic, mpConsume, mpInitial, hpConsume, power,
							castRange, skillRadius, hitTime, coolTime, reuseDelay, magicLvl,
							absorb, base.nextActionAttack(),
							base.itemConsumeId(), base.itemConsumeCount(),
							base.giveCharges(), base.maxCharges(), base.needCharges(), base.consumeCharges(),
							base.continueAfterMax(),
							funcs.isEmpty() && routeFor == forElement ? base.funcs() : List.copyOf(funcs),
							effects.isEmpty() && routeFor == forElement ? base.effects() : List.copyOf(effects),
							rCastCond, rCondMsg);
					enchantedById.computeIfAbsent(id, k -> new HashMap<>()).put(lvl, st);
				}
			}
		}
	}

	private static EffectTemplate parseEffect(Element e, Resolver r) {
		List<StatFunc> funcs = new ArrayList<>();
		for (Element f : SkillCondition.children(e)) {
			StatFunc func = parseFunc(f, r);
			if (func != null) {
				funcs.add(func);
			}
		}
		String stack = r.str(e.getAttribute("stackType"), "none");
		return new EffectTemplate(e.getAttribute("name"), r.integer(e.getAttribute("count"), 1),
				r.integer(e.getAttribute("time"), 0), r.number(e.getAttribute("val"), 0),
				stack.isBlank() ? "none" : stack, r.number(e.getAttribute("stackOrder"), 0), List.copyOf(funcs));
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
			} else {
				cond = (p, target) -> false; // condicao que o servidor nao sabe avaliar: nao aplica
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
