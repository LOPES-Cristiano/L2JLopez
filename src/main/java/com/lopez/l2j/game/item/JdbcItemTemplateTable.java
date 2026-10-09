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
		List<CreationItem> creation = new ArrayList<>();
		try {
			creation.addAll(jdbc.sql("SELECT classId, itemId, amount, equipped FROM char_creation_items")
					.query((rs, i) -> new CreationItem(rs.getInt("classId"), rs.getInt("itemId"), rs.getInt("amount"),
							"true".equalsIgnoreCase(rs.getString("equipped"))))
					.list());
		} catch (RuntimeException e) {
			log.warn("char_creation_items indisponivel: {}", e.getMessage());
		}
		log.info("ItemTable: {} etc, {} armaduras, {} armas, {} custom, {} itens iniciais em {} ms", etc, armor, weapon,
				custom, creation.size(), (System.nanoTime() - start) / 1_000_000);
		return new Data(Map.copyOf(byId), List.copyOf(creation));
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
		int avoidModify = optInt(rs, "avoid_modify");
		int mpBonus = optInt(rs, "mp_bonus");
		String skillsItem = optString(rs, "skills_item");
		return ItemTemplate.armor(rs.getInt("item_id"), displayId, rs.getString("name"), rs.getString("bodypart"),
				rs.getString("armor_type"), rs.getInt("weight"), rs.getString("crystal_type"), rs.getInt("p_def"),
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
