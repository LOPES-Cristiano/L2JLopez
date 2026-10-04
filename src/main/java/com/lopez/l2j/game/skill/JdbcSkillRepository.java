package com.lopez.l2j.game.skill;

import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSkillRepository implements SkillRepository {

	private final JdbcClient jdbc;

	public JdbcSkillRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<Skill> findByCharId(int charId, int classIndex) {
		return jdbc.sql("""
				SELECT skill_id, skill_level, skill_name
				FROM character_skills
				WHERE charId = ? AND class_index = ?
				""")
				.params(charId, classIndex)
				.query((rs, rowNum) -> {
					int id = rs.getInt("skill_id");
					int level = rs.getInt("skill_level");
					String name = rs.getString("skill_name");
					return new Skill(id, level, name != null ? name : "", false);
				})
				.list();
	}

	@Override
	public void save(int charId, int classIndex, Skill skill) {
		jdbc.sql("""
				REPLACE INTO character_skills (charId, skill_id, skill_level, skill_name, class_index)
				VALUES (?, ?, ?, ?, ?)
				""")
				.params(charId, skill.id(), skill.level(), skill.name(), classIndex)
				.update();
	}

	@Override
	public void delete(int charId, int classIndex, int skillId) {
		jdbc.sql("""
				DELETE FROM character_skills
				WHERE charId = ? AND class_index = ? AND skill_id = ?
				""")
				.params(charId, classIndex, skillId)
				.update();
	}
}
