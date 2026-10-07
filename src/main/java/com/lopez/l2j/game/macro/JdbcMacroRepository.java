package com.lopez.l2j.game.macro;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcMacroRepository implements MacroRepository {

	private final JdbcClient jdbc;

	public JdbcMacroRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<Macro> findByCharId(int charId) {
		return jdbc.sql("""
				SELECT id, icon, name, descr, acronym, commands
				FROM character_macroses
				WHERE charId = ?
				ORDER BY id ASC
				""")
				.param(charId)
				.query((rs, rowNum) -> {
					int id = rs.getInt("id");
					int icon = rs.getInt("icon");
					String name = rs.getString("name");
					String descr = rs.getString("descr");
					String acronym = rs.getString("acronym");
					String rawCmds = rs.getString("commands");
					List<MacroCmd> cmds = parseCommands(rawCmds);
					return new Macro(id, icon, name != null ? name : "", descr != null ? descr : "",
							acronym != null ? acronym : "", cmds);
				})
				.list();
	}

	@Override
	public void save(int charId, Macro macro) {
		String serialized = serializeCommands(macro.commands());
		jdbc.sql("""
				REPLACE INTO character_macroses (charId, id, icon, name, descr, acronym, commands)
				VALUES (?, ?, ?, ?, ?, ?, ?)
				""")
				.params(charId, macro.id(), macro.icon(), macro.name(), macro.descr(), macro.acronym(), serialized)
				.update();
	}

	@Override
	public void delete(int charId, int macroId) {
		jdbc.sql("""
				DELETE FROM character_macroses
				WHERE charId = ? AND id = ?
				""")
				.params(charId, macroId)
				.update();
	}

	@Override
	public void deleteAll(int charId) {
		jdbc.sql("""
				DELETE FROM character_macroses
				WHERE charId = ?
				""")
				.param(charId)
				.update();
	}

	public static List<MacroCmd> parseCommands(String raw) {
		if (raw == null || raw.isBlank()) {
			return List.of();
		}
		List<MacroCmd> list = new ArrayList<>();
		StringTokenizer st = new StringTokenizer(raw, ";");
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (token.isBlank()) {
				continue;
			}
			String[] parts = token.split(",", 4);
			if (parts.length >= 3) {
				try {
					int type = Integer.parseInt(parts[0].trim());
					int d1 = Integer.parseInt(parts[1].trim());
					int d2 = Integer.parseInt(parts[2].trim());
					String cmd = parts.length >= 4 ? parts[3] : "";
					list.add(new MacroCmd(list.size() + 1, type, d1, d2, cmd));
				} catch (NumberFormatException ignored) {
				}
			}
		}
		return list;
	}

	public static String serializeCommands(List<MacroCmd> cmds) {
		if (cmds == null || cmds.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (MacroCmd c : cmds) {
			sb.append(c.type()).append(',').append(c.d1()).append(',').append(c.d2());
			if (c.cmd() != null && !c.cmd().isEmpty()) {
				sb.append(',').append(c.cmd());
			}
			sb.append(';');
		}
		return sb.length() > 255 ? sb.substring(0, 255) : sb.toString();
	}
}
