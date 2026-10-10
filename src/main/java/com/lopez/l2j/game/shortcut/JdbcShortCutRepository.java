package com.lopez.l2j.game.shortcut;

import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcShortCutRepository implements ShortCutRepository {

	private final JdbcClient jdbc;

	public JdbcShortCutRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<ShortCut> findByCharId(int charId, int classIndex) {
		return jdbc.sql("""
				SELECT slot, page, type, shortcut_id, level
				FROM character_shortcuts
				WHERE charId = ? AND class_index = ?
				""")
				.params(charId, classIndex)
				.query((rs, rowNum) -> {
					int slot = rs.getInt("slot");
					int page = rs.getInt("page");
					int type = rs.getInt("type");
					int id = rs.getInt("shortcut_id");
					String lvlStr = rs.getString("level");
					int level = (lvlStr != null && !lvlStr.isBlank()) ? Integer.parseInt(lvlStr) : -1;
					return new ShortCut(slot, page, type, id, level, 1);
				})
				.list();
	}

	@Override
	public void save(int charId, int classIndex, ShortCut sc) {
		jdbc.sql("""
				REPLACE INTO character_shortcuts (charId, slot, page, type, shortcut_id, level, class_index)
				VALUES (?, ?, ?, ?, ?, ?, ?)
				""")
				.params(charId, sc.slot(), sc.page(), sc.type(), sc.id(), String.valueOf(sc.level()), classIndex)
				.update();
	}

	@Override
	public void delete(int charId, int classIndex, int slot, int page) {
		jdbc.sql("""
				DELETE FROM character_shortcuts
				WHERE charId = ? AND slot = ? AND page = ? AND class_index = ?
				""")
				.params(charId, slot, page, classIndex)
				.update();
	}

	@Override
	public void deleteByTypeAndId(int charId, int type, int shortcutId) {
		jdbc.sql("""
				DELETE FROM character_shortcuts
				WHERE charId = ? AND type = ? AND shortcut_id = ?
				""")
				.params(charId, type, shortcutId)
				.update();
	}

	@Override
	public void deleteAll(int charId, int classIndex) {
		jdbc.sql("""
				DELETE FROM character_shortcuts
				WHERE charId = ? AND class_index = ?
				""")
				.params(charId, classIndex)
				.update();
	}
}
