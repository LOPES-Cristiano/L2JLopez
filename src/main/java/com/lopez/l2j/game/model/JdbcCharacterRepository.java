package com.lopez.l2j.game.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
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
			accesslevel, lastAccess, deletetime, x, y, z, heading, rec_have, rec_left, last_recom_date,
			vip, vip_end, aio, aio_end, nobless, in_jail, jail_timer, pccaffe_points, varka_ketra_ally,
			death_penalty_level, pledge_rank, subpledge, clan_privs, apprentice, sponsor,
			clan_join_expiry_time, clan_create_expiry_time, arena_wins, arena_defeats, buffshop_slots,
			cancraft, newbie, onlinetime, isin7sdungeon, wantspeace, lvl_joined_academy, isCensor, isBanned""";

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
				UPDATE characters SET sex = :sex, classid = :classId, base_class = :baseClass, x = :x, y = :y, z = :z, heading = :heading, level = :level,
				  exp = :exp, sp = :sp, maxHp = :maxHp, maxMp = :maxMp, maxCp = :maxCp, curHp = :hp,
				  curMp = :mp, curCp = :cp, face = :face, hairStyle = :hairStyle, hairColor = :hairColor,
				  karma = :karma, pvpkills = :pvpkills, pkkills = :pkkills, clanid = :clanid, title = :title,
				  online = :online, lastAccess = :now, accesslevel = :accesslevel,
				  rec_have = :recHave, rec_left = :recLeft, last_recom_date = :lastRecomDate,
				  vip = :vip, vip_end = :vipEnd, aio = :aio, aio_end = :aioEnd,
				  nobless = :nobless, in_jail = :inJail, jail_timer = :jailTimer, pccaffe_points = :pcCaffe,
				  varka_ketra_ally = :varkaKetra, death_penalty_level = :deathPenalty, pledge_rank = :pledgeRank,
				  subpledge = :subpledge, clan_privs = :clanPrivs, apprentice = :apprentice, sponsor = :sponsor,
				  clan_join_expiry_time = :clanJoinExp, clan_create_expiry_time = :clanCreateExp,
				  arena_wins = :arenaWins, arena_defeats = :arenaDefeats, buffshop_slots = :buffSlots,
				  cancraft = :canCraft, newbie = :newbie, onlinetime = :onlineTime,
				  isin7sdungeon = :in7s, wantspeace = :wantsPeace, lvl_joined_academy = :lvlAcademy,
				  isCensor = :censor, isBanned = :banned WHERE charId = :id
				""")
				.param("sex", c.female() ? 1 : 0)
				.param("classId", c.classId())
				.param("baseClass", c.baseClassId())
				.param("x", c.x())
				.param("y", c.y())
				.param("z", c.z())
				.param("heading", c.heading())
				.param("level", c.level())
				.param("exp", Math.max(0L, c.exp()))
				.param("sp", Math.max(0, c.sp()))
				.param("maxHp", c.maxHp())
				.param("maxMp", c.maxMp())
				.param("maxCp", c.maxCp())
				.param("hp", (int) c.currentHp())
				.param("mp", (int) c.currentMp())
				.param("cp", (int) c.currentCp())
				.param("face", c.face())
				.param("hairStyle", c.hairStyle())
				.param("hairColor", c.hairColor())
				.param("karma", c.karma())
				.param("pvpkills", c.pvpKills())
				.param("pkkills", c.pkKills())
				.param("clanid", c.clanId())
				.param("title", c.title())
				.param("online", online ? 1 : 0)
				.param("now", System.currentTimeMillis())
				.param("accesslevel", c.accessLevel())
				.param("recHave", c.recomHave())
				.param("recLeft", c.recomLeft())
				.param("lastRecomDate", c.lastRecomDate())
				.param("vip", c.isVip() ? 1 : 0)
				.param("vipEnd", c.vipExpiration())
				.param("aio", c.isAio() ? 1 : 0)
				.param("aioEnd", c.aioExpiration())
				.param("nobless", c.isNoble() ? 1 : 0)
				.param("inJail", c.isInJail() ? 1 : 0)
				.param("jailTimer", c.jailTimer())
				.param("pcCaffe", c.pcCaffePoints())
				.param("varkaKetra", c.varkaKetraAlly())
				.param("deathPenalty", c.deathPenaltyLevel())
				.param("pledgeRank", c.pledgeRank())
				.param("subpledge", c.subPledge())
				.param("clanPrivs", c.clanPrivs())
				.param("apprentice", c.apprentice())
				.param("sponsor", c.sponsor())
				.param("clanJoinExp", c.clanJoinExpiryTime())
				.param("clanCreateExp", c.clanCreateExpiryTime())
				.param("arenaWins", c.arenaWins())
				.param("arenaDefeats", c.arenaDefeats())
				.param("buffSlots", c.buffShopSlots())
				.param("canCraft", c.canCraft() ? 1 : 0)
				.param("newbie", c.newbie())
				.param("onlineTime", c.onlineTime())
				.param("in7s", c.isIn7sDungeon() ? 1 : 0)
				.param("wantsPeace", c.wantsPeace() ? 1 : 0)
				.param("lvlAcademy", c.lvlJoinedAcademy())
				.param("censor", c.isCensor() ? 1 : 0)
				.param("banned", c.isBanned() ? 1 : 0)
				.param("id", c.objectId())
				.update();
	}

	@Override
	public void setAccessLevel(String charName, int accessLevel) {
		jdbc.sql("UPDATE characters SET accesslevel = :lvl WHERE LOWER(char_name) = LOWER(:name)")
				.param("lvl", accessLevel)
				.param("name", charName)
				.update();
	}

	@Override
	public List<PlayerCharacter> listAll(int limit) {
		return jdbc.sql("SELECT " + COLUMNS + " FROM characters ORDER BY online DESC, lastAccess DESC LIMIT :lim")
				.param("lim", limit)
				.query(JdbcCharacterRepository::map)
				.list();
	}

	@Override
	public List<PlayerCharacter> searchByName(String query, int limit) {
		return jdbc.sql("SELECT " + COLUMNS + " FROM characters WHERE LOWER(char_name) LIKE :q ORDER BY char_name ASC LIMIT :lim")
				.param("q", "%" + query.toLowerCase(java.util.Locale.ROOT) + "%")
				.param("lim", limit)
				.query(JdbcCharacterRepository::map)
				.list();
	}

	@Override
	public Optional<PlayerCharacter> findByName(String name) {
		return jdbc.sql("SELECT " + COLUMNS + " FROM characters WHERE LOWER(char_name) = LOWER(:name)")
				.param("name", name)
				.query(JdbcCharacterRepository::map)
				.optional();
	}

	@Override
	public List<PlayerCharacter> findTopPvP(int limit) {
		return jdbc.sql("SELECT " + COLUMNS + " FROM characters WHERE pvpkills > 0 ORDER BY pvpkills DESC LIMIT :lim")
				.param("lim", limit)
				.query(JdbcCharacterRepository::map)
				.list();
	}

	@Override
	public List<PlayerCharacter> findTopPK(int limit) {
		return jdbc.sql("SELECT " + COLUMNS + " FROM characters WHERE pkkills > 0 ORDER BY pkkills DESC LIMIT :lim")
				.param("lim", limit)
				.query(JdbcCharacterRepository::map)
				.list();
	}

	private static PlayerCharacter map(ResultSet rs, int row) throws SQLException {
		var c = new PlayerCharacter(rs.getInt("charId"), rs.getString("account_name"), rs.getString("char_name"),
				rs.getInt("level"), rs.getLong("exp"), rs.getInt("sp"), rs.getInt("race"), rs.getInt("classid"),
				rs.getInt("base_class"), rs.getInt("sex") == 1, rs.getInt("face"), rs.getInt("hairStyle"),
				rs.getInt("hairColor"), rs.getInt("maxHp"), rs.getInt("maxMp"), rs.getInt("maxCp"),
				rs.getInt("karma"), rs.getInt("pvpkills"), rs.getInt("pkkills"), rs.getInt("clanid"),
				rs.getString("title"), rs.getInt("accesslevel"), rs.getLong("lastAccess"), rs.getLong("deletetime"),
				rs.getInt("x"), rs.getInt("y"), rs.getInt("z"), rs.getInt("heading"), rs.getInt("curHp"),
				rs.getInt("curMp"), rs.getInt("curCp"));
		c.recomHave(rs.getInt("rec_have"));
		c.recomLeft(rs.getInt("rec_left"));
		c.lastRecomDate(rs.getLong("last_recom_date"));
		try {
			c.setVip(rs.getInt("vip") == 1);
			c.vipExpiration(rs.getLong("vip_end"));
		} catch (SQLException ignored) {
		}
		try {
			c.setAio(rs.getInt("aio") == 1);
			c.aioExpiration(rs.getLong("aio_end"));
		} catch (SQLException ignored) {
		}
		try {
			c.setNoble(rs.getInt("nobless") == 1);
			c.setInJail(rs.getInt("in_jail") == 1);
			c.jailTimer(rs.getInt("jail_timer"));
			c.pcCaffePoints(rs.getInt("pccaffe_points"));
			c.varkaKetraAlly(rs.getInt("varka_ketra_ally"));
			c.deathPenaltyLevel(rs.getInt("death_penalty_level"));
			c.pledgeRank(rs.getInt("pledge_rank"));
			c.subPledge(rs.getInt("subpledge"));
			c.clanPrivs(rs.getInt("clan_privs"));
			c.apprentice(rs.getInt("apprentice"));
			c.sponsor(rs.getInt("sponsor"));
			c.clanJoinExpiryTime(rs.getLong("clan_join_expiry_time"));
			c.clanCreateExpiryTime(rs.getLong("clan_create_expiry_time"));
			c.arenaWins(rs.getInt("arena_wins"));
			c.arenaDefeats(rs.getInt("arena_defeats"));
			c.buffShopSlots(rs.getInt("buffshop_slots"));
			c.canCraft(rs.getInt("cancraft") == 1);
			c.newbie(rs.getInt("newbie"));
			c.onlineTime(rs.getInt("onlinetime"));
			c.isIn7sDungeon(rs.getInt("isin7sdungeon") == 1);
			c.wantsPeace(rs.getInt("wantspeace") == 1);
			c.lvlJoinedAcademy(rs.getInt("lvl_joined_academy"));
			c.isCensor(rs.getInt("isCensor") == 1);
			c.isBanned(rs.getInt("isBanned") == 1);
		} catch (SQLException ignored) {
		}
		return c;
	}
}
