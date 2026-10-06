package com.lopez.l2j.game.clan.privilege;

import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.clan.ClanMember;
import com.lopez.l2j.game.clan.ClanTable;
import com.lopez.l2j.game.model.PlayerCharacter;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Servico de privilegios de cla e sub-unidades (ClanPrivs / SubPledges / Academy do L2JDream).
 * Gerencia os 9 niveis de rank (power grades), permissoes bitmask e criacao de Academia, Guarda Real e Ordens de Cavalaria.
 */
@Service
public class ClanPrivilegeService {

	private static final Logger log = LoggerFactory.getLogger(ClanPrivilegeService.class);

	public static final int REP_COST_ROYAL = 5000;
	public static final int REP_COST_KNIGHT = 10000;

	public enum SubPledgeResult {
		SUCCESS,
		NOT_IN_CLAN,
		NOT_CLAN_LEADER,
		CLAN_LEVEL_TOO_LOW,
		NOT_ENOUGH_REPUTATION,
		ALREADY_EXISTS,
		INVALID_SUB_TYPE,
		SUB_LEADER_REQUIRED,
		ERROR
	}

	private final ClanTable clanTable;
	private final JdbcClient jdbc;

	@Autowired
	public ClanPrivilegeService(ClanTable clanTable, @Autowired(required = false) JdbcClient jdbc) {
		this.clanTable = clanTable;
		this.jdbc = jdbc;
	}

	/**
	 * Verifica se o jogador possui o privilegio especificado no cla.
	 * O Lider do cla possui SEMPRE todos os privilegios.
	 */
	public boolean hasPrivilege(PlayerCharacter player, int privilegeBit) {
		if (player == null || player.clanId() <= 0) {
			return false;
		}
		var clanOpt = clanTable.byClanId(player.clanId());
		if (clanOpt.isEmpty()) {
			return false;
		}
		Clan clan = clanOpt.get();

		// Lider do cla possui acesso irrestrito
		if (clan.isLeader(player.objectId())) {
			return true;
		}

		ClanMember member = clan.getMember(player.objectId());
		if (member == null) {
			return false;
		}

		int rank = member.powerGrade();
		int rankPrivs = clan.getRankPrivilege(rank);
		return (rankPrivs & privilegeBit) == privilegeBit;
	}

	/**
	 * Atualiza os privilegios associados a um determinado power grade (1 a 9).
	 */
	public synchronized boolean setRankPrivilege(PlayerCharacter leader, int rank, int privBitmask) {
		if (leader == null || leader.clanId() <= 0 || rank < 1 || rank > 9) {
			return false;
		}
		var clanOpt = clanTable.byClanId(leader.clanId());
		if (clanOpt.isEmpty()) {
			return false;
		}
		Clan clan = clanOpt.get();
		if (!clan.isLeader(leader.objectId())) {
			return false;
		}

		clan.setRankPrivilege(rank, privBitmask);

		if (jdbc != null) {
			try {
				jdbc.sql("""
						REPLACE INTO clan_privs (clan_id, rank, party, privilleges)
						VALUES (:clanId, :rank, 0, :privs)
						""")
						.param("clanId", clan.clanId())
						.param("rank", rank)
						.param("privs", privBitmask)
						.update();
			} catch (Exception ex) {
				log.error("Erro ao persistir privilegios do rank {} para cla {}", rank, clan.clanId(), ex);
				return false;
			}
		}

		log.info("Privilegios do rank {} no cla '{}' atualizados para {}", rank, clan.name(), privBitmask);
		return true;
	}

	/**
	 * Altera o power grade de um membro do cla.
	 */
	public synchronized boolean setMemberPowerGrade(PlayerCharacter leader, int targetObjectId, int newGrade) {
		if (leader == null || leader.clanId() <= 0 || newGrade < 1 || newGrade > 9) {
			return false;
		}
		var clanOpt = clanTable.byClanId(leader.clanId());
		if (clanOpt.isEmpty()) {
			return false;
		}
		Clan clan = clanOpt.get();
		if (!clan.isLeader(leader.objectId())) {
			return false;
		}

		ClanMember targetMember = clan.getMember(targetObjectId);
		if (targetMember == null) {
			return false;
		}

		targetMember.powerGrade(newGrade);

		if (jdbc != null) {
			try {
				jdbc.sql("UPDATE characters SET power_grade = :grade WHERE charId = :charId")
						.param("grade", newGrade)
						.param("charId", targetObjectId)
						.update();
			} catch (Exception ignored) {}
		}

		log.info("Membro '{}' do cla '{}' teve seu power grade alterado para {}",
				targetMember.name(), clan.name(), newGrade);
		return true;
	}

	/**
	 * Cria uma sub-unidade de cla (Academia, Guarda Real 1/2 ou Ordem de Cavalaria 1-4).
	 */
	public synchronized SubPledgeResult createSubPledge(PlayerCharacter leader, int subType, String name, int subLeaderId) {
		if (leader == null || leader.clanId() <= 0) {
			return SubPledgeResult.NOT_IN_CLAN;
		}
		var clanOpt = clanTable.byClanId(leader.clanId());
		if (clanOpt.isEmpty()) {
			return SubPledgeResult.NOT_IN_CLAN;
		}
		Clan clan = clanOpt.get();
		if (!clan.isLeader(leader.objectId())) {
			return SubPledgeResult.NOT_CLAN_LEADER;
		}

		if (clan.getSubPledge(subType) != null) {
			return SubPledgeResult.ALREADY_EXISTS;
		}

		int repCost = 0;
		switch (subType) {
			case Clan.SUBUNIT_ACADEMY -> {
				if (clan.level() < 5) return SubPledgeResult.CLAN_LEVEL_TOO_LOW;
			}
			case Clan.SUBUNIT_ROYAL1, Clan.SUBUNIT_ROYAL2 -> {
				if (clan.level() < 6) return SubPledgeResult.CLAN_LEVEL_TOO_LOW;
				if (subLeaderId <= 0 || clan.getMember(subLeaderId) == null) return SubPledgeResult.SUB_LEADER_REQUIRED;
				repCost = REP_COST_ROYAL;
			}
			case Clan.SUBUNIT_KNIGHT1, Clan.SUBUNIT_KNIGHT2, Clan.SUBUNIT_KNIGHT3, Clan.SUBUNIT_KNIGHT4 -> {
				if (clan.level() < 7) return SubPledgeResult.CLAN_LEVEL_TOO_LOW;
				if (subLeaderId <= 0 || clan.getMember(subLeaderId) == null) return SubPledgeResult.SUB_LEADER_REQUIRED;
				repCost = REP_COST_KNIGHT;
			}
			default -> {
				return SubPledgeResult.INVALID_SUB_TYPE;
			}
		}

		if (repCost > 0 && clan.reputationScore() < repCost) {
			return SubPledgeResult.NOT_ENOUGH_REPUTATION;
		}

		if (repCost > 0) {
			clan.reputationScore(clan.reputationScore() - repCost);
		}

		String cleanName = (name != null && !name.isBlank()) ? name.trim() : ("SubPledge_" + subType);
		clan.addSubPledge(subType, cleanName, subLeaderId);

		if (jdbc != null) {
			try {
				jdbc.sql("""
						REPLACE INTO clan_subpledges (clan_id, sub_pledge_id, name, leader_id)
						VALUES (:clanId, :subId, :name, :leaderId)
						""")
						.param("clanId", clan.clanId())
						.param("subId", subType)
						.param("name", cleanName)
						.param("leaderId", subLeaderId)
						.update();

				if (repCost > 0) {
					jdbc.sql("UPDATE clan_data SET reputation_score = :rep WHERE clan_id = :clanId")
							.param("rep", clan.reputationScore())
							.param("clanId", clan.clanId())
							.update();
				}
			} catch (Exception ex) {
				log.error("Erro ao persistir criacao de subpledge {} para cla {}", subType, clan.clanId(), ex);
				return SubPledgeResult.ERROR;
			}
		}

		log.info("SubPledge tipo {} '{}' criada com sucesso pelo cla '{}' (Custo: {} Rep)",
				subType, cleanName, clan.name(), repCost);
		return SubPledgeResult.SUCCESS;
	}
}
