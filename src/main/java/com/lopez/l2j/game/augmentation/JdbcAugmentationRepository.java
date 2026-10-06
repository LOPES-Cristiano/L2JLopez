package com.lopez.l2j.game.augmentation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAugmentationRepository implements AugmentationRepository {

	private final JdbcClient jdbc;

	public JdbcAugmentationRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public void save(int itemId, Augmentation aug) {
		if (aug == null) {
			delete(itemId);
			return;
		}
		jdbc.sql("""
				REPLACE INTO item_attributes (itemId, augAttributes, augSkillId, augSkillLevel)
				VALUES (:itemId, :attrs, :skillId, :skillLevel)
				""")
				.param("itemId", itemId)
				.param("attrs", aug.attributes())
				.param("skillId", aug.skillId())
				.param("skillLevel", aug.skillLevel())
				.update();
	}

	@Override
	public void delete(int itemId) {
		jdbc.sql("DELETE FROM item_attributes WHERE itemId = :itemId")
				.param("itemId", itemId)
				.update();
	}

	@Override
	public Optional<Augmentation> findByItemId(int itemId) {
		return jdbc.sql("SELECT augAttributes, augSkillId, augSkillLevel FROM item_attributes WHERE itemId = :itemId")
				.param("itemId", itemId)
				.query((rs, i) -> new Augmentation(rs.getInt("augAttributes"), rs.getInt("augSkillId"), rs.getInt("augSkillLevel")))
				.optional();
	}

	@Override
	public Map<Integer, Augmentation> findByOwnerId(int ownerId) {
		Map<Integer, Augmentation> result = new HashMap<>();
		jdbc.sql("""
				SELECT ia.itemId, ia.augAttributes, ia.augSkillId, ia.augSkillLevel
				FROM item_attributes ia
				JOIN items i ON ia.itemId = i.object_id
				WHERE i.owner_id = :ownerId
				""")
				.param("ownerId", ownerId)
				.query((rs, i) -> {
					int itemId = rs.getInt("itemId");
					int attrs = rs.getInt("augAttributes");
					int skillId = rs.getInt("augSkillId");
					int skillLvl = rs.getInt("augSkillLevel");
					result.put(itemId, new Augmentation(attrs, skillId, skillLvl));
					return null;
				});
		return result;
	}
}
