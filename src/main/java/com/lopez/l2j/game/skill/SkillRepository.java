package com.lopez.l2j.game.skill;

import java.util.List;

/**
 * Contrato de persistencia para as habilidades aprendidas pelo jogador.
 */
public interface SkillRepository {

	List<Skill> findByCharId(int charId, int classIndex);

	void save(int charId, int classIndex, Skill skill);

	void delete(int charId, int classIndex, int skillId);

	void deleteAll(int charId, int classIndex);
}
