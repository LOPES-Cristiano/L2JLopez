package com.lopez.l2j.game.clan.war;

import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.clan.ClanTable;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.world.GameWorld;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Servico de gerenciamento de guerras de cla (Clan Wars / RequestStartPledgeWar do L2JDream).
 * Cuida de declaracoes, mutual wars, penalidades de 5 dias e imunidade de karma/PK.
 */
@Service
public class ClanWarService {

	private static final Logger log = LoggerFactory.getLogger(ClanWarService.class);

	public static final int MAX_CLAN_WARS = 30;
	public static final int MIN_CLAN_LEVEL = 3;
	public static final int MIN_CLAN_MEMBERS = 15;
	public static final long WAR_PENALTY_MILLIS = 5L * 86_400_000L; // 5 dias

	public enum DeclareResult {
		SUCCESS,
		NOT_IN_CLAN,
		NOT_AUTHORIZED,
		TARGET_NOT_FOUND,
		SAME_CLAN,
		TOO_MANY_WARS,
		CLAN_LEVEL_OR_MEMBERS_TOO_LOW,
		TARGET_LEVEL_OR_MEMBERS_TOO_LOW,
		SAME_ALLIANCE,
		TARGET_DISSOLVING,
		ALREADY_AT_WAR,
		PENALTY_ACTIVE,
		ERROR
	}

	public enum StopResult {
		SUCCESS,
		NOT_IN_CLAN,
		NOT_AUTHORIZED,
		TARGET_NOT_FOUND,
		NOT_AT_WAR,
		IN_COMBAT,
		ERROR
	}

	private final ClanTable clanTable;
	private final JdbcClient jdbc;

	@Autowired
	public ClanWarService(ClanTable clanTable, @Autowired(required = false) JdbcClient jdbc) {
		this.clanTable = clanTable;
		this.jdbc = jdbc;
	}

	/**
	 * Declara guerra contra um cla adversario.
	 */
	public synchronized DeclareResult declareWar(PlayerCharacter player, String targetClanName) {
		if (player == null || player.clanId() <= 0) {
			return DeclareResult.NOT_IN_CLAN;
		}
		var playerClanOpt = clanTable.byClanId(player.clanId());
		if (playerClanOpt.isEmpty()) {
			return DeclareResult.NOT_IN_CLAN;
		}
		Clan playerClan = playerClanOpt.get();

		// Checa autorizacao (Lider ou privilegio CP_CL_PLEDGE_WAR = 32)
		if (!playerClan.isLeader(player.objectId())) {
			var member = playerClan.getMember(player.objectId());
			int grade = member != null ? member.powerGrade() : 5;
			int privs = playerClan.getRankPrivilege(grade);
			if ((privs & Clan.CP_CL_PLEDGE_WAR) != Clan.CP_CL_PLEDGE_WAR) {
				return DeclareResult.NOT_AUTHORIZED;
			}
		}

		if (targetClanName == null || targetClanName.isBlank()) {
			return DeclareResult.TARGET_NOT_FOUND;
		}
		var targetClanOpt = clanTable.byName(targetClanName.trim());
		if (targetClanOpt.isEmpty()) {
			return DeclareResult.TARGET_NOT_FOUND;
		}
		Clan targetClan = targetClanOpt.get();

		if (playerClan.clanId() == targetClan.clanId()) {
			return DeclareResult.SAME_CLAN;
		}
		if (playerClan.enemyClanIds().size() >= MAX_CLAN_WARS) {
			return DeclareResult.TOO_MANY_WARS;
		}
		if (playerClan.level() < MIN_CLAN_LEVEL || playerClan.membersCount() < MIN_CLAN_MEMBERS) {
			return DeclareResult.CLAN_LEVEL_OR_MEMBERS_TOO_LOW;
		}

		// Se o cla alvo ainda nao declarou contra este, checa nivel e membros do alvo
		if (!playerClan.isAttackerClan(targetClan.clanId())
				&& (targetClan.level() < MIN_CLAN_LEVEL || targetClan.membersCount() < MIN_CLAN_MEMBERS)) {
			return DeclareResult.TARGET_LEVEL_OR_MEMBERS_TOO_LOW;
		}

		if (playerClan.allyId() > 0 && playerClan.allyId() == targetClan.allyId()) {
			return DeclareResult.SAME_ALLIANCE;
		}
		if (targetClan.dissolvingExpiryTime() > System.currentTimeMillis()) {
			return DeclareResult.TARGET_DISSOLVING;
		}
		if (playerClan.isAtWarWith(targetClan.clanId())) {
			return DeclareResult.ALREADY_AT_WAR;
		}
		if (playerClan.hasWarPenalty(targetClan.clanId())) {
			return DeclareResult.PENALTY_ACTIVE;
		}

		playerClan.addEnemyClan(targetClan.clanId());
		targetClan.addAttackerClan(playerClan.clanId());

		if (jdbc != null) {
			try {
				jdbc.sql("""
						REPLACE INTO clan_wars (clan1, clan2, expiry_time)
						VALUES (:c1, :c2, 0)
						""")
						.param("c1", String.valueOf(playerClan.clanId()))
						.param("c2", String.valueOf(targetClan.clanId()))
						.update();
			} catch (Exception ex) {
				log.error("Erro ao persistir declaracao de guerra entre cla {} e cla {}",
						playerClan.clanId(), targetClan.clanId(), ex);
				return DeclareResult.ERROR;
			}
		}

		log.info("Guerra declarada: Cla '{}' (ID {}) -> Cla '{}' (ID {}) (Mutual: {})",
				playerClan.name(), playerClan.clanId(), targetClan.name(), targetClan.clanId(),
				isMutualWar(playerClan.clanId(), targetClan.clanId()));
		return DeclareResult.SUCCESS;
	}

	/**
	 * Interrompe a guerra contra um cla adversario, aplicando penalidade de 5 dias.
	 */
	public synchronized StopResult stopWar(PlayerCharacter player, String targetClanName) {
		if (player == null || player.clanId() <= 0) {
			return StopResult.NOT_IN_CLAN;
		}
		var playerClanOpt = clanTable.byClanId(player.clanId());
		if (playerClanOpt.isEmpty()) {
			return StopResult.NOT_IN_CLAN;
		}
		Clan playerClan = playerClanOpt.get();

		if (!playerClan.isLeader(player.objectId())) {
			var member = playerClan.getMember(player.objectId());
			int grade = member != null ? member.powerGrade() : 5;
			int privs = playerClan.getRankPrivilege(grade);
			if ((privs & Clan.CP_CL_PLEDGE_WAR) != Clan.CP_CL_PLEDGE_WAR) {
				return StopResult.NOT_AUTHORIZED;
			}
		}

		if (targetClanName == null || targetClanName.isBlank()) {
			return StopResult.TARGET_NOT_FOUND;
		}
		var targetClanOpt = clanTable.byName(targetClanName.trim());
		if (targetClanOpt.isEmpty()) {
			return StopResult.TARGET_NOT_FOUND;
		}
		Clan targetClan = targetClanOpt.get();

		if (!playerClan.isAtWarWith(targetClan.clanId())) {
			return StopResult.NOT_AT_WAR;
		}

		long now = System.currentTimeMillis();
		long penaltyExpiry = now + WAR_PENALTY_MILLIS;

		playerClan.removeEnemyClan(targetClan.clanId());
		targetClan.removeAttackerClan(playerClan.clanId());
		playerClan.addWarPenaltyTime(targetClan.clanId(), penaltyExpiry);

		if (jdbc != null) {
			try {
				jdbc.sql("""
						UPDATE clan_wars
						SET expiry_time = :exp
						WHERE clan1 = :c1 AND clan2 = :c2
						""")
						.param("exp", penaltyExpiry)
						.param("c1", String.valueOf(playerClan.clanId()))
						.param("c2", String.valueOf(targetClan.clanId()))
						.update();
			} catch (Exception ex) {
				log.error("Erro ao persistir fim de guerra entre cla {} e cla {}",
						playerClan.clanId(), targetClan.clanId(), ex);
				return StopResult.ERROR;
			}
		}

		log.info("Guerra interrompida: Cla '{}' parou guerra com '{}'. Penalidade ate {}",
				playerClan.name(), targetClan.name(), penaltyExpiry);
		return StopResult.SUCCESS;
	}

	/**
	 * Retorna true se houver declaracao mutua de guerra entre ambos os clas.
	 */
	public boolean isMutualWar(int clan1Id, int clan2Id) {
		if (clan1Id <= 0 || clan2Id <= 0 || clan1Id == clan2Id) {
			return false;
		}
		var c1Opt = clanTable.byClanId(clan1Id);
		var c2Opt = clanTable.byClanId(clan2Id);
		if (c1Opt.isEmpty() || c2Opt.isEmpty()) {
			return false;
		}
		Clan c1 = c1Opt.get();
		Clan c2 = c2Opt.get();
		return c1.isEnemyClan(clan2Id) && c2.isEnemyClan(clan1Id);
	}

	/**
	 * Retorna true se clan1 declarou guerra contra clan2 (unilateral ou mutua).
	 */
	public boolean isAtWar(int clan1Id, int clan2Id) {
		if (clan1Id <= 0 || clan2Id <= 0) {
			return false;
		}
		var c1Opt = clanTable.byClanId(clan1Id);
		return c1Opt.map(clan -> clan.isAtWarWith(clan2Id)).orElse(false);
	}

	/**
	 * Determina se dois jogadores estao em guerra mutua e podem combater livremente sem ganho de karma/PK.
	 */
	public boolean canAttackWithoutPenalty(PlayerCharacter attacker, PlayerCharacter target) {
		if (attacker == null || target == null || attacker.clanId() <= 0 || target.clanId() <= 0) {
			return false;
		}
		return isMutualWar(attacker.clanId(), target.clanId());
	}

	public Set<Integer> getActiveWars(int clanId) {
		var clanOpt = clanTable.byClanId(clanId);
		return clanOpt.map(Clan::enemyClanIds).orElse(Collections.emptySet());
	}
}
