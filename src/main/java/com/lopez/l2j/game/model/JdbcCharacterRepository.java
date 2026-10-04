package com.lopez.l2j.game.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * JDBC sobre a tabela legada {@code characters}. Object ids vem do {@link ObjectIdFactory} (faixa do IdFactory
 * legado, compartilhada com itens).
 */
@Repository
class JdbcCharacterRepository implements CharacterRepository {

	static final int FIRST_OBJECT_ID = ObjectIdFactory.FIRST_OBJECT_ID;

	private static final String COLUMNS = """
			account_name, charId, char_name, level, exp, sp, race, classid, base_class, sex, face, hairStyle,
			hairColor, maxHp, maxMp, maxCp, curHp, curMp, curCp, karma, pvpkills, pkkills, clanid, title,
			accesslevel, lastAccess, deletetime, x, y, z, heading""";

	private final JdbcClient jdbc;
	private final ObjectIdFactory ids;

	JdbcCharacterRepository(JdbcClient jdbc, ObjectIdFactory ids) {
		this.jdbc = jdbc;
		this.ids = ids;
	}

	@Override
	public List<PlayerCharacter> findByAccount(String account) {
		return jdbc.sql("SELECT " + COLUMNS + " FROM characters WHERE account_name = :acc ORDER BY charId")
				.param("acc", account)
				.query(JdbcCharacterRepository::map)
				.list();
	}

	@Override
	public boolean nameExists(String name) {
		return jdbc.sql("SELECT COUNT(*) FROM characters WHERE LOWER(char_name) = LOWER(:name)")
				.param("name", name)
				.query(Integer.class)
				.single() > 0;
	}

	@Override
	public PlayerCharacter create(NewCharacter c) {
		int id = ids.nextId();
		long now = System.currentTimeMillis();
		jdbc.sql("""
				INSERT INTO characters (account_name, charId, char_name, level, maxHp, curHp, maxCp, curCp, maxMp,
				  curMp, face, hairStyle, hairColor, sex, heading, x, y, z, exp, sp, karma, pvpkills, pkkills,
				  clanid, race, classid, base_class, deletetime, cancraft, title, online, char_slot, newbie,
				  lastAccess, accesslevel)
				VALUES (:acc, :id, :name, 1, :hp, :hp, :cp, :cp, :mp, :mp, :face, :hairStyle, :hairColor, :sex, 0,
				  :x, :y, :z, 0, 0, 0, 0, 0, 0, :race, :classId, :classId, 0, :craft, '', 0, 0, 1, :now, 0)
				""")
				.param("acc", c.account())
				.param("id", id)
				.param("name", c.name())
				.param("hp", c.maxHp())
				.param("cp", c.maxCp())
				.param("mp", c.maxMp())
				.param("face", c.face())
				.param("hairStyle", c.hairStyle())
				.param("hairColor", c.hairColor())
				.param("sex", c.female() ? 1 : 0)
				.param("x", c.x())
				.param("y", c.y())
				.param("z", c.z())
				.param("race", c.race())
				.param("classId", c.classId())
				.param("craft", c.canCraft() ? 1 : 0)
				.param("now", now)
				.update();
		return new PlayerCharacter(id, c.account(), c.name(), 1, 0, 0, c.race(), c.classId(), c.classId(),
				c.female(), c.face(), c.hairStyle(), c.hairColor(), c.maxHp(), c.maxMp(), c.maxCp(), 0, 0, 0, 0,
				"", 0, now, 0, c.x(), c.y(), c.z(), 0, c.maxHp(), c.maxMp(), c.maxCp());
	}

	@Override
	public void delete(int objectId) {
		jdbc.sql("DELETE FROM characters WHERE charId = :id").param("id", objectId).update();
	}

	@Override
	public void updateDeleteTime(int objectId, long deleteTime) {
		jdbc.sql("UPDATE characters SET deletetime = :t WHERE charId = :id")
				.param("t", deleteTime)
				.param("id", objectId)
				.update();
	}

	@Override
	public void saveState(PlayerCharacter c, boolean online) {
		jdbc.sql("""
				UPDATE characters SET x = :x, y = :y, z = :z, heading = :heading, level = :level,
				  exp = :exp, sp = :sp, maxHp = :maxHp, maxMp = :maxMp, maxCp = :maxCp, curHp = :hp,
				  curMp = :mp, curCp = :cp, face = :face, hairStyle = :hairStyle, hairColor = :hairColor,
				  online = :online, lastAccess = :now WHERE charId = :id
				""")
				.param("x", c.x())
				.param("y", c.y())
				.param("z", c.z())
				.param("heading", c.heading())
				.param("level", c.level())
				.param("exp", c.exp())
				.param("sp", c.sp())
				.param("maxHp", c.maxHp())
				.param("maxMp", c.maxMp())
				.param("maxCp", c.maxCp())
				.param("hp", (int) c.currentHp())
				.param("mp", (int) c.currentMp())
				.param("cp", (int) c.currentCp())
				.param("face", c.face())
				.param("hairStyle", c.hairStyle())
				.param("hairColor", c.hairColor())
				.param("online", online ? 1 : 0)
				.param("now", System.currentTimeMillis())
				.param("id", c.objectId())
				.update();
	}

	private static PlayerCharacter map(ResultSet rs, int row) throws SQLException {
		return new PlayerCharacter(rs.getInt("charId"), rs.getString("account_name"), rs.getString("char_name"),
				rs.getInt("level"), rs.getLong("exp"), rs.getInt("sp"), rs.getInt("race"), rs.getInt("classid"),
				rs.getInt("base_class"), rs.getInt("sex") == 1, rs.getInt("face"), rs.getInt("hairStyle"),
				rs.getInt("hairColor"), rs.getInt("maxHp"), rs.getInt("maxMp"), rs.getInt("maxCp"),
				rs.getInt("karma"), rs.getInt("pvpkills"), rs.getInt("pkkills"), rs.getInt("clanid"),
				rs.getString("title"), rs.getInt("accesslevel"), rs.getLong("lastAccess"), rs.getLong("deletetime"),
				rs.getInt("x"), rs.getInt("y"), rs.getInt("z"), rs.getInt("heading"), rs.getInt("curHp"),
				rs.getInt("curMp"), rs.getInt("curCp"));
	}
}
