package com.lopez.l2j.game.clan.skill;

import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.clan.ClanMember;
import com.lopez.l2j.game.clan.ClanTable;
import com.lopez.l2j.game.model.PlayerCharacter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Servico de habilidades de cla (Clan Skills / Pledge Skill Tree do L2JDream).
 * Gerencia aprendizado, consumo de reputacao/itens necessarios e aplicacao aos membros elegiveis.
 */
@Service
public class ClanSkillService {

	private static final Logger log = LoggerFactory.getLogger(ClanSkillService.class);

	public enum LearnResult {
		SUCCESS,
		NOT_IN_CLAN,
		NOT_LEADER,
		SKILL_NOT_FOUND,
		MAX_LEVEL_REACHED,
		CLAN_LEVEL_TOO_LOW,
		NOT_ENOUGH_REPUTATION,
		MISSING_REQUIRED_ITEM,
		ERROR
	}

	private final ClanTable clanTable;
	private final ClanSkillTreeTable skillTreeTable;
	private final JdbcClient jdbc;

	@Autowired
	public ClanSkillService(ClanTable clanTable, ClanSkillTreeTable skillTreeTable,
			@Autowired(required = false) JdbcClient jdbc) {
		this.clanTable = clanTable;
		this.skillTreeTable = skillTreeTable;
		this.jdbc = jdbc;
	}

	/**
	 * Retorna a lista de habilidades de cla que o cla esta qualificado a aprender no momento.
	 */
	public List<PledgeSkillRecord> getAvailableSkills(Clan clan) {
		if (clan == null) {
			return Collections.emptyList();
		}
		List<PledgeSkillRecord> available = new ArrayList<>();
		for (var candidate : skillTreeTable.allSkills()) {
			int currentLvl = clan.getSkillLevel(candidate.skillId());
			if (candidate.level() == currentLvl + 1 && clan.level() >= candidate.minClanLevel()) {
				available.add(candidate);
			}
		}
		return available;
	}

	/**
	 * Realiza o aprendizado de uma habilidade de cla pelo lider.
	 */
	public synchronized LearnResult learnSkill(PlayerCharacter leader, int skillId) {
		if (leader == null || leader.clanId() <= 0) {
			return LearnResult.NOT_IN_CLAN;
		}
		var clanOpt = clanTable.byClanId(leader.clanId());
		if (clanOpt.isEmpty()) {
			return LearnResult.NOT_IN_CLAN;
		}
		Clan clan = clanOpt.get();

		if (!clan.isLeader(leader.objectId())) {
			return LearnResult.NOT_LEADER;
		}

		int currentLevel = clan.getSkillLevel(skillId);
		int nextLevel = currentLevel + 1;

		var skillOpt = skillTreeTable.getSkill(skillId, nextLevel);
		if (skillOpt.isEmpty()) {
			// Se o nivel 1 sequer existe na arvore
			if (skillTreeTable.getSkillsForSkillId(skillId).isEmpty()) {
				return LearnResult.SKILL_NOT_FOUND;
			}
			return LearnResult.MAX_LEVEL_REACHED;
		}

		PledgeSkillRecord skill = skillOpt.get();

		if (clan.level() < skill.minClanLevel()) {
			return LearnResult.CLAN_LEVEL_TOO_LOW;
		}

		if (clan.reputationScore() < skill.repCost()) {
			return LearnResult.NOT_ENOUGH_REPUTATION;
		}

		// Checa se o lider possui o item necessario no inventario
		if (skill.itemId() > 0) {
			var inv = leader.inventory();
			if (inv == null || inv.getItemCount(skill.itemId()) < 1) {
				return LearnResult.MISSING_REQUIRED_ITEM;
			}
			// Consome o item
			inv.destroyItemByItemId(skill.itemId(), 1);
		}

		// Consome pontos de reputacao
		if (skill.repCost() > 0) {
			clan.reputationScore(clan.reputationScore() - skill.repCost());
		}

		// Adiciona habilidade ao cla
		clan.addSkill(skillId, nextLevel);

		if (jdbc != null) {
			try {
				jdbc.sql("""
						REPLACE INTO clan_skills (clan_id, skill_id, skill_level, skill_name)
						VALUES (:clanId, :skillId, :lvl, :name)
						""")
						.param("clanId", clan.clanId())
						.param("skillId", skillId)
						.param("lvl", nextLevel)
						.param("name", skill.name())
						.update();

				jdbc.sql("""
						UPDATE clan_data
						SET reputation_score = :rep
						WHERE clan_id = :clanId
						""")
						.param("rep", clan.reputationScore())
						.param("clanId", clan.clanId())
						.update();
			} catch (Exception ex) {
				log.error("Erro ao persistir habilidade de cla {} lvl {} para cla {}",
						skillId, nextLevel, clan.clanId(), ex);
				return LearnResult.ERROR;
			}
		}

		log.info("Cla '{}' (ID {}) aprendeu habilidade de cla '{}' Lvl {} (Custo: {} Rep)",
				clan.name(), clan.clanId(), skill.name(), nextLevel, skill.repCost());
		return LearnResult.SUCCESS;
	}

	/**
	 * Retorna o mapa de habilidades de cla aprendidas (skillId -> level).
	 */
	public Map<Integer, Integer> getClanSkills(int clanId) {
		var clanOpt = clanTable.byClanId(clanId);
		return clanOpt.map(Clan::skills).orElse(Collections.emptyMap());
	}

	/**
	 * Checa se um membro de cla e elegivel para receber os efeitos das habilidades de cla.
	 * Na regra oficial do Interlude, membros da Academia nao recebem habilidades de cla.
	 */
	public boolean isMemberEligibleForClanSkills(ClanMember member) {
		if (member == null) {
			return false;
		}
		// Membros da Academia (SUBUNIT_ACADEMY = -1) nao recebem habilidades de cla
		return member.pledgeType() != Clan.SUBUNIT_ACADEMY;
	}
}
