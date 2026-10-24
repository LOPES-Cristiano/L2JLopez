package com.lopez.l2j.game.item;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Carrega etcitem/armor/weapon (+ custom_*) e char_creation_items na primeira consulta. Tabelas ausentes (ex.:
 * H2 dos testes) resultam em catalogo vazio com aviso, sem derrubar o servidor.
 */
@Repository
class JdbcItemTemplateTable implements ItemTemplateTable {

	private static final Logger log = LoggerFactory.getLogger(JdbcItemTemplateTable.class);

	private record Data(Map<Integer, ItemTemplate> byId, List<CreationItem> creation) {
	}

	private final JdbcClient jdbc;
	private volatile Data data;

	JdbcItemTemplateTable(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public Optional<ItemTemplate> get(int itemId) {
		return Optional.ofNullable(data().byId().get(itemId));
	}

	@Override
	public int size() {
		return data().byId().size();
	}

	@Override
	public java.util.Collection<ItemTemplate> all() {
		return data().byId().values();
	}

	@Override
	public List<CreationItem> creationItems(int classId) {
		Data d = data();
		return ItemTemplateTable.filterCreation(d.creation(), classId, d.byId()::containsKey);
	}

	private Data data() {
		Data d = data;
		if (d == null) {
			synchronized (this) {
				d = data;
				if (d == null) {
					data = d = load();
				}
			}
		}
		return d;
	}

	private Data load() {
		long start = System.nanoTime();
		Map<Integer, ItemTemplate> byId = new HashMap<>();
		int etc = load(byId, "etcitem", false, JdbcItemTemplateTable::etc);
		int armor = load(byId, "armor", false, JdbcItemTemplateTable::armor);
		int weapon = load(byId, "weapon", false, JdbcItemTemplateTable::weapon);
		int custom = load(byId, "custom_etcitem", true, JdbcItemTemplateTable::etc)
				+ load(byId, "custom_armor", true, JdbcItemTemplateTable::armor)
				+ load(byId, "custom_weapon", true, JdbcItemTemplateTable::weapon);
		int xmlLoaded = loadXmlItemStats(byId);
		List<CreationItem> creation = new ArrayList<>();
		try {
			creation.addAll(jdbc.sql("SELECT classId, itemId, amount, equipped FROM char_creation_items")
					.query((rs, i) -> new CreationItem(rs.getInt("classId"), rs.getInt("itemId"), rs.getInt("amount"),
							"true".equalsIgnoreCase(rs.getString("equipped"))))
					.list());
		} catch (RuntimeException e) {
			log.warn("char_creation_items indisponivel: {}", e.getMessage());
		}
		log.info("ItemTable: {} etc, {} armaduras, {} armas, {} custom, {} xml stats, {} itens iniciais em {} ms", etc, armor, weapon,
				custom, xmlLoaded, creation.size(), (System.nanoTime() - start) / 1_000_000);
		return new Data(Map.copyOf(byId), List.copyOf(creation));
	}

	private int loadXmlItemStats(Map<Integer, ItemTemplate> byId) {
		int count = 0;
		String[] folders = { "data/xml/stats/armor", "data/xml/stats/weapon", "data/xml/stats/etcitem" };
		javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
		for (String folder : folders) {
			java.nio.file.Path dir = java.nio.file.Path.of(folder);
			if (!java.nio.file.Files.isDirectory(dir)) {
				continue;
			}
			try (var stream = java.nio.file.Files.list(dir)) {
				for (java.nio.file.Path xmlFile : stream.filter(p -> p.toString().endsWith(".xml")).toList()) {
					try {
						org.w3c.dom.Document doc = dbf.newDocumentBuilder().parse(xmlFile.toFile());
						org.w3c.dom.NodeList items = doc.getElementsByTagName("item");
						for (int i = 0; i < items.getLength(); i++) {
							if (!(items.item(i) instanceof org.w3c.dom.Element itemEl)) {
								continue;
							}
							String idStr = itemEl.getAttribute("id");
							if (idStr.isBlank()) {
								continue;
							}
							int itemId = Integer.parseInt(idStr.trim());
							ItemTemplate existing = byId.get(itemId);
							if (existing == null) {
								continue;
							}
							List<com.lopez.l2j.game.skill.StatFunc> funcs = new ArrayList<>();
							org.w3c.dom.NodeList forList = itemEl.getElementsByTagName("for");
							for (int f = 0; f < forList.getLength(); f++) {
								org.w3c.dom.Node forNode = forList.item(f);
								org.w3c.dom.NodeList children = forNode.getChildNodes();
								for (int c = 0; c < children.getLength(); c++) {
									if (!(children.item(c) instanceof org.w3c.dom.Element opEl)) {
										continue;
									}
									String tag = opEl.getTagName();
									var op = com.lopez.l2j.game.skill.StatFunc.Op.parse(tag);
									if (op == null || op == com.lopez.l2j.game.skill.StatFunc.Op.SET) {
										continue;
									}
									String stat = opEl.getAttribute("stat");
									String valStr = opEl.getAttribute("val");
									String orderStr = opEl.getAttribute("order");
									if (stat.isBlank() || valStr.isBlank()) {
										continue;
									}
									// Base pDef/mDef/pAtk/mAtk/accCombat/rEvas at order 0x10 are already in SQL columns
									int order = 0x30;
									if (!orderStr.isBlank()) {
										order = Integer.decode(orderStr.trim());
									}
									if (op == com.lopez.l2j.game.skill.StatFunc.Op.ADD && order == 0x10
											&& ("pDef".equalsIgnoreCase(stat) || "mDef".equalsIgnoreCase(stat)
													|| "pAtk".equalsIgnoreCase(stat) || "mAtk".equalsIgnoreCase(stat)
													|| "accCombat".equalsIgnoreCase(stat) || "rEvas".equalsIgnoreCase(stat)
													|| "sDef".equalsIgnoreCase(stat))) {
										continue;
									}
									double val = Double.parseDouble(valStr.trim());
									funcs.add(new com.lopez.l2j.game.skill.StatFunc(stat.trim(), op, order, val));
								}
							}
							if (!funcs.isEmpty()) {
								byId.put(itemId, existing.withStatFuncs(funcs));
								count++;
							}
						}
					} catch (Exception ignored) {
					}
				}
			} catch (Exception ignored) {
			}
		}
		return count;
	}

	@FunctionalInterface
	private interface RowReader {
		ItemTemplate read(ResultSet rs, int displayId) throws SQLException;
	}

	private int load(Map<Integer, ItemTemplate> into, String table, boolean custom, RowReader reader) {
		try {
			List<ItemTemplate> rows = jdbc.sql("SELECT * FROM " + table).query((rs, i) -> {
				int id = rs.getInt("item_id");
				int display = custom ? rs.getInt("item_display_id") : id;
				return reader.read(rs, display == 0 ? id : display);
			}).list();
			rows.forEach(t -> into.put(t.id(), t));
			return rows.size();
		} catch (RuntimeException e) {
			log.warn("Tabela {} indisponivel: {}", table, e.getMessage());
			return 0;
		}
	}

	private static String optString(ResultSet rs, String column) {
		try {
			String val = rs.getString(column);
			return val == null ? "" : val;
		} catch (SQLException e) {
			return "";
		}
	}

	private static int optInt(ResultSet rs, String column) {
		try {
			return rs.getInt(column);
		} catch (SQLException e) {
			return 0;
		}
	}

	private static boolean flag(ResultSet rs, String column) throws SQLException {
		String v = rs.getString(column);
		return v == null || Boolean.parseBoolean(v);
	}

	private static ItemTemplate etc(ResultSet rs, int displayId) throws SQLException {
		String skill = optString(rs, "skill");
		return ItemTemplate.etc(rs.getInt("item_id"), displayId, rs.getString("name"), rs.getString("item_type"),
				rs.getString("consume_type"), rs.getInt("weight"), rs.getString("crystal_type"), rs.getInt("price"),
				ItemSkillHolder.parseList(skill), flag(rs, "sellable"), flag(rs, "dropable"), flag(rs, "destroyable"),
				flag(rs, "tradeable"));
	}

	private static ItemTemplate armor(ResultSet rs, int displayId) throws SQLException {
		int itemId = rs.getInt("item_id");
		int avoidModify = optInt(rs, "avoid_modify");
		int mpBonus = optInt(rs, "mp_bonus");
		String skillsItem = optString(rs, "skills_item");
		String crystalType = switch (itemId) {
			case 6660 -> "b"; // Ring of Queen Ant (Grade B)
			case 6661, 6662, 8191 -> "a"; // Earring of Orfen, Ring of Core, Frintezza's Necklace (Grade A)
			case 6656, 6657, 6658, 6659 -> "s"; // Antharas, Valakas, Baium, Zaken (Grade S)
			default -> rs.getString("crystal_type");
		};
		return ItemTemplate.armor(itemId, displayId, rs.getString("name"), rs.getString("bodypart"),
				rs.getString("armor_type"), rs.getInt("weight"), crystalType, rs.getInt("p_def"),
				rs.getInt("m_def"), avoidModify, mpBonus, rs.getInt("price"), ItemSkillHolder.parseList(skillsItem),
				flag(rs, "sellable"), flag(rs, "dropable"), flag(rs, "destroyable"), flag(rs, "tradeable"));
	}

	private static ItemTemplate weapon(ResultSet rs, int displayId) throws SQLException {
		int critical = rs.getInt("critical");
		if (critical > 0 && critical <= 20) {
			critical *= 10;
		}
		int hitModify = 0;
		try {
			hitModify = (int) Math.round(rs.getDouble("hit_modify"));
		} catch (SQLException ignored) {
		}
		int avoidModify = optInt(rs, "avoid_modify");
		int soulshots = Math.max(1, optInt(rs, "soulshots"));
		int spiritshots = Math.max(1, optInt(rs, "spiritshots"));
		int rndDam = optInt(rs, "rnd_dam");
		String skillsItem = optString(rs, "skills_item");
		String skillsEnchant4 = optString(rs, "skills_enchant4");
		String skillsOnCrit = optString(rs, "skills_onCrit");
		String skillsOnCast = optString(rs, "skills_onCast");

		return ItemTemplate.weapon(rs.getInt("item_id"), displayId, rs.getString("name"), rs.getString("bodypart"),
				rs.getString("weaponType"), rs.getInt("weight"), rs.getString("crystal_type"), rs.getInt("p_dam"),
				rs.getInt("m_dam"), rs.getInt("atk_speed"), critical, hitModify, avoidModify, rs.getInt("shield_def"),
				soulshots, spiritshots, rndDam, rs.getInt("price"), ItemSkillHolder.parseList(skillsItem),
				ItemSkillHolder.parse(skillsEnchant4), ItemSkillHolder.parse(skillsOnCrit),
				ItemSkillHolder.parse(skillsOnCast), flag(rs, "sellable"), flag(rs, "dropable"),
				flag(rs, "destroyable"), flag(rs, "tradeable"));
	}
}
