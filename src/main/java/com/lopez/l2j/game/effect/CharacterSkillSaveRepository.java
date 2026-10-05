package com.lopez.l2j.game.effect;

import com.lopez.l2j.game.effect.PlayerEffects.ActiveBuff;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Persistencia de buffs e efeitos ativos na tabela {@code character_skills_save}.
 * Garante que pocoes e buffs de skills sobrevivam a reinicios de servidor, logout e restart.
 */
@Repository
public class CharacterSkillSaveRepository {

	private static final Logger log = LoggerFactory.getLogger(CharacterSkillSaveRepository.class);

	public record SavedBuff(int skillId, int level, int remainingSeconds, long systime, int buffIndex) {
	}

	private final JdbcClient jdbc;

	public CharacterSkillSaveRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/**
	 * Salva todos os buffs ativos do personagem para a classe atual.
	 */
	public void saveBuffs(int charId, int classIndex, List<ActiveBuff> buffs) {
		try {
			// Remove os buffs salvos anteriormente deste personagem
			jdbc.sql("DELETE FROM character_skills_save WHERE charId = :charId AND class_index = :classIndex")
					.param("charId", charId)
					.param("classIndex", classIndex)
					.update();

			if (buffs == null || buffs.isEmpty()) {
				return;
			}

			long now = System.currentTimeMillis();
			int buffIndex = 0;

			for (ActiveBuff b : buffs) {
				if (!b.isPermanent() && b.endTimeMillis() <= now) {
					continue;
				}
				buffIndex++;
				int remainingSec = b.remainingSeconds(now);

				jdbc.sql("""
						REPLACE INTO character_skills_save (
							charId, skill_id, skill_level, effect_count, effect_cur_time,
							reuse_delay, systime, restore_type, class_index, buff_index
						) VALUES (
							:charId, :skillId, :level, 1, :curTime,
							0, :systime, 0, :classIndex, :buffIndex
						)
						""")
						.param("charId", charId)
						.param("skillId", b.skillId())
						.param("level", b.level())
						.param("curTime", remainingSec)
						.param("systime", b.endTimeMillis())
						.param("classIndex", classIndex)
						.param("buffIndex", buffIndex)
						.update();
			}
			log.debug("Salvos {} buffs para o charId {}", buffIndex, charId);
		} catch (Exception e) {
			log.error("Erro ao salvar buffs do charId {}: {}", charId, e.getMessage(), e);
		}
	}

	/**
	 * Carrega os buffs salvos do banco de dados para o personagem.
	 */
	public List<SavedBuff> restoreBuffs(int charId, int classIndex) {
		try {
			return jdbc.sql("""
					SELECT skill_id, skill_level, effect_cur_time, systime, buff_index
					FROM character_skills_save
					WHERE charId = :charId AND class_index = :classIndex
					ORDER BY buff_index ASC
					""")
					.param("charId", charId)
					.param("classIndex", classIndex)
					.query((rs, rowNum) -> new SavedBuff(
							rs.getInt("skill_id"),
							rs.getInt("skill_level"),
							rs.getInt("effect_cur_time"),
							rs.getLong("systime"),
							rs.getInt("buff_index")))
					.list();
		} catch (Exception e) {
			log.error("Erro ao carregar buffs do charId {}: {}", charId, e.getMessage(), e);
			return List.of();
		}
	}

	/**
	 * Remove todos os buffs salvos (ex.: ao deletar personagem ou morrer sem bless).
	 */
	public void deleteBuffs(int charId) {
		try {
			jdbc.sql("DELETE FROM character_skills_save WHERE charId = :charId")
					.param("charId", charId)
					.update();
		} catch (Exception e) {
			log.error("Erro ao deletar buffs do charId {}: {}", charId, e.getMessage(), e);
		}
	}
}
