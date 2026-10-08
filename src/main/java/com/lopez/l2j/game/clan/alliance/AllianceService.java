package com.lopez.l2j.game.clan.alliance;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.clan.ClanMember;
import com.lopez.l2j.game.clan.ClanTable;
import com.lopez.l2j.game.clan.CrestCache;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Servico de gerenciamento de aliancas (L2Alliance / Ally handlers do L2JDream).
 * Cuida de criacao, convites, saida, expulsao, dissolucao, brasoes e chat de alianca.
 */
@Service
public class AllianceService {

	private static final Logger log = LoggerFactory.getLogger(AllianceService.class);
	private static final Pattern ALLY_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9]{3,16}$");

	public static int getMaxClansInAlliance() {
		return Config.ALT_MAX_NUM_OF_CLANS_IN_ALLY;
	}

	public static long getPenaltyCreateDaysMillis() {
		return Math.max(1, Config.DAYS_BEFORE_CREATE_NEW_ALLY_WHEN_DISSOLVED) * 86_400_000L;
	}

	public static long getPenaltyLeaveDaysMillis() {
		return Math.max(1, Config.DAYS_BEFORE_JOIN_ALLY_WHEN_LEAVED) * 86_400_000L;
	}

	public static long getPenaltyDismissDaysMillis() {
		return Math.max(1, Config.DAYS_BEFORE_JOIN_ALLY_WHEN_DISMISSED) * 86_400_000L;
	}

	public static long getPenaltyAcceptNewClanDaysMillis() {
		return Math.max(1, Config.DAYS_BEFORE_ACCEPT_NEW_CLAN_WHEN_DISMISSED) * 86_400_000L;
	}

	public enum CreateResult {
		SUCCESS,
		NULL_LEADER,
		NOT_CLAN_LEADER,
		ALREADY_IN_ALLIANCE,
		LEVEL_TOO_LOW,
		DISSOLVE_PENALTY_ACTIVE,
		CLAN_DISSOLVING,
		INVALID_NAME_LENGTH,
		INVALID_NAME_PATTERN,
		NAME_ALREADY_EXISTS,
		ERROR
	}

	public enum InviteResult {
		SUCCESS,
		NOT_ALLIANCE_LEADER,
		DISMISS_PENALTY_ACTIVE,
		TARGET_NOT_FOUND,
		CANNOT_INVITE_SELF,
		TARGET_NOT_IN_CLAN,
		TARGET_NOT_CLAN_LEADER,
		TARGET_ALREADY_IN_ALLIANCE,
		TARGET_PENALTY_ACTIVE,
		AT_WAR_WITH_TARGET,
		ALLIANCE_FULL,
		ERROR
	}

	public enum LeaveResult {
		SUCCESS,
		NOT_IN_ALLIANCE,
		NOT_CLAN_LEADER,
		LEADER_CANNOT_LEAVE,
		ERROR
	}

	public enum DismissResult {
		SUCCESS,
		NOT_ALLIANCE_LEADER,
		TARGET_NOT_FOUND,
		TARGET_NOT_IN_ALLIANCE,
		CANNOT_DISMISS_LEADER_CLAN,
		ERROR
	}

	public enum DissolveResult {
		SUCCESS,
		NOT_IN_ALLIANCE,
		NOT_ALLIANCE_LEADER,
		ERROR
	}

	private final ClanTable clanTable;
	private final CrestCache crestCache;
	private final JdbcClient jdbc;

	@Autowired
	public AllianceService(ClanTable clanTable,
			@Autowired(required = false) CrestCache crestCache,
			@Autowired(required = false) JdbcClient jdbc) {
		this.clanTable = clanTable;
		this.crestCache = crestCache;
		this.jdbc = jdbc;
	}

	/**
	 * Cria uma nova alianca com o cla do lider especificado.
	 */
	public synchronized CreateResult createAlliance(PlayerCharacter leader, String allyName) {
		if (leader == null) {
			return CreateResult.NULL_LEADER;
		}
		if (leader.clanId() <= 0) {
			return CreateResult.NOT_CLAN_LEADER;
		}
		var clanOpt = clanTable.byClanId(leader.clanId());
		if (clanOpt.isEmpty()) {
			return CreateResult.NOT_CLAN_LEADER;
		}
		Clan clan = clanOpt.get();
		if (!clan.isLeader(leader.objectId())) {
			return CreateResult.NOT_CLAN_LEADER;
		}
		if (clan.allyId() > 0) {
			return CreateResult.ALREADY_IN_ALLIANCE;
		}
		if (clan.level() < 5) {
			return CreateResult.LEVEL_TOO_LOW;
		}
		long now = System.currentTimeMillis();
		if (clan.allyPenaltyExpiryTime() > now && clan.allyPenaltyType() == Clan.PENALTY_TYPE_DISSOLVE_ALLY) {
			return CreateResult.DISSOLVE_PENALTY_ACTIVE;
		}
		if (clan.dissolvingExpiryTime() > now) {
			return CreateResult.CLAN_DISSOLVING;
		}
		if (allyName == null || allyName.trim().length() < 3 || allyName.trim().length() > 16) {
			return CreateResult.INVALID_NAME_LENGTH;
		}
		String cleanName = allyName.trim();
		if (!ALLY_NAME_PATTERN.matcher(cleanName).matches()) {
			return CreateResult.INVALID_NAME_PATTERN;
		}
		if (clanTable.isAllyExists(cleanName)) {
			return CreateResult.NAME_ALREADY_EXISTS;
		}

		int allyId = clan.clanId();
		clan.allyId(allyId);
		clan.allyName(cleanName);
		clan.setAllyPenalty(0L, Clan.PENALTY_TYPE_NONE);

		if (jdbc != null) {
			try {
				jdbc.sql("""
						UPDATE clan_data
						SET ally_id = :allyId, ally_name = :allyName,
						    ally_penalty_expiry_time = 0, ally_penalty_type = 0
						WHERE clan_id = :clanId
						""")
						.param("allyId", allyId)
						.param("allyName", cleanName)
						.param("clanId", clan.clanId())
						.update();
			} catch (Exception ex) {
				log.error("Erro ao persistir criacao de alianca {} para cla {}", cleanName, clan.clanId(), ex);
				return CreateResult.ERROR;
			}
		}

		log.info("Alianca '{}' (ID {}) criada com sucesso pelo cla '{}'", cleanName, allyId, clan.name());
		return CreateResult.SUCCESS;
	}

	/**
	 * Verifica condicoes para convidar outro cla para a alianca.
	 */
	public InviteResult checkAllyJoinCondition(PlayerCharacter leader, PlayerCharacter target) {
		if (leader == null || target == null) {
			return InviteResult.TARGET_NOT_FOUND;
		}
		if (leader.objectId() == target.objectId()) {
			return InviteResult.CANNOT_INVITE_SELF;
		}
		if (leader.clanId() <= 0) {
			return InviteResult.NOT_ALLIANCE_LEADER;
		}
		var leaderClanOpt = clanTable.byClanId(leader.clanId());
		if (leaderClanOpt.isEmpty()) {
			return InviteResult.NOT_ALLIANCE_LEADER;
		}
		Clan leaderClan = leaderClanOpt.get();
		if (leaderClan.allyId() == 0 || !leaderClan.isLeader(leader.objectId()) || leaderClan.clanId() != leaderClan.allyId()) {
			return InviteResult.NOT_ALLIANCE_LEADER;
		}

		long now = System.currentTimeMillis();
		if (leaderClan.allyPenaltyExpiryTime() > now && leaderClan.allyPenaltyType() == Clan.PENALTY_TYPE_DISMISS_CLAN) {
			return InviteResult.DISMISS_PENALTY_ACTIVE;
		}

		if (target.clanId() <= 0) {
			return InviteResult.TARGET_NOT_IN_CLAN;
		}
		var targetClanOpt = clanTable.byClanId(target.clanId());
		if (targetClanOpt.isEmpty()) {
			return InviteResult.TARGET_NOT_IN_CLAN;
		}
		Clan targetClan = targetClanOpt.get();
		if (!targetClan.isLeader(target.objectId())) {
			return InviteResult.TARGET_NOT_CLAN_LEADER;
		}
		if (targetClan.allyId() > 0) {
			return InviteResult.TARGET_ALREADY_IN_ALLIANCE;
		}
		if (targetClan.allyPenaltyExpiryTime() > now) {
			return InviteResult.TARGET_PENALTY_ACTIVE;
		}
		if (leaderClan.isAtWarWith(targetClan.clanId())) {
			return InviteResult.AT_WAR_WITH_TARGET;
		}
		if (clanTable.getClanAllies(leaderClan.allyId()).size() >= getMaxClansInAlliance()) {
			return InviteResult.ALLIANCE_FULL;
		}

		return InviteResult.SUCCESS;
	}

	/**
	 * Adiciona um cla a alianca apos aceitacao do convite.
	 */
	public synchronized boolean addClanToAlliance(int allyId, Clan targetClan) {
		if (targetClan == null || allyId <= 0) {
			return false;
		}
		var leaderClanOpt = clanTable.byClanId(allyId);
		if (leaderClanOpt.isEmpty()) {
			return false;
		}
		Clan leaderClan = leaderClanOpt.get();
		if (clanTable.getClanAllies(allyId).size() >= getMaxClansInAlliance()) {
			return false;
		}

		targetClan.allyId(allyId);
		targetClan.allyName(leaderClan.allyName());
		targetClan.allyCrestId(leaderClan.allyCrestId());
		targetClan.setAllyPenalty(0L, Clan.PENALTY_TYPE_NONE);

		if (jdbc != null) {
			try {
				jdbc.sql("""
						UPDATE clan_data
						SET ally_id = :allyId, ally_name = :allyName, ally_crest_id = :crest,
						    ally_penalty_expiry_time = 0, ally_penalty_type = 0
						WHERE clan_id = :clanId
						""")
						.param("allyId", allyId)
						.param("allyName", leaderClan.allyName())
						.param("crest", leaderClan.allyCrestId())
						.param("clanId", targetClan.clanId())
						.update();
			} catch (Exception ex) {
				log.error("Erro ao adicionar cla {} a alianca {}", targetClan.clanId(), allyId, ex);
				return false;
			}
		}

		log.info("Cla '{}' (ID {}) ingressou na alianca '{}' (ID {})",
				targetClan.name(), targetClan.clanId(), leaderClan.allyName(), allyId);
		return true;
	}

	/**
	 * Cla membro se retira voluntariamente da alianca.
	 */
	public synchronized LeaveResult leaveAlliance(PlayerCharacter clanLeader) {
		if (clanLeader == null || clanLeader.clanId() <= 0) {
			return LeaveResult.NOT_IN_ALLIANCE;
		}
		var clanOpt = clanTable.byClanId(clanLeader.clanId());
		if (clanOpt.isEmpty()) {
			return LeaveResult.NOT_IN_ALLIANCE;
		}
		Clan clan = clanOpt.get();
		if (!clan.isLeader(clanLeader.objectId())) {
			return LeaveResult.NOT_CLAN_LEADER;
		}
		if (clan.allyId() <= 0) {
			return LeaveResult.NOT_IN_ALLIANCE;
		}
		if (clan.clanId() == clan.allyId()) {
			return LeaveResult.LEADER_CANNOT_LEAVE;
		}

		long now = System.currentTimeMillis();
		clan.allyId(0);
		clan.allyName(null);
		clan.allyCrestId(0);
		clan.setAllyPenalty(now + getPenaltyLeaveDaysMillis(), Clan.PENALTY_TYPE_CLAN_LEAVED);

		if (jdbc != null) {
			try {
				jdbc.sql("""
						UPDATE clan_data
						SET ally_id = 0, ally_name = NULL, ally_crest_id = 0,
						    ally_penalty_expiry_time = :expiry, ally_penalty_type = :type
						WHERE clan_id = :clanId
						""")
						.param("expiry", clan.allyPenaltyExpiryTime())
						.param("type", clan.allyPenaltyType())
						.param("clanId", clan.clanId())
						.update();
			} catch (Exception ex) {
				log.error("Erro ao persistir saida da alianca pelo cla {}", clan.clanId(), ex);
				return LeaveResult.ERROR;
			}
		}

		log.info("Cla '{}' saiu da alianca. Penalidade aplicada ate {}", clan.name(), clan.allyPenaltyExpiryTime());
		return LeaveResult.SUCCESS;
	}

	/**
	 * Lider da alianca expulsa um cla membro.
	 */
	public synchronized DismissResult dismissClan(PlayerCharacter allyLeader, Clan targetClan) {
		if (allyLeader == null || allyLeader.clanId() <= 0 || targetClan == null) {
			return DismissResult.NOT_ALLIANCE_LEADER;
		}
		var leaderClanOpt = clanTable.byClanId(allyLeader.clanId());
		if (leaderClanOpt.isEmpty()) {
			return DismissResult.NOT_ALLIANCE_LEADER;
		}
		Clan leaderClan = leaderClanOpt.get();
		if (leaderClan.allyId() <= 0 || !leaderClan.isLeader(allyLeader.objectId()) || leaderClan.clanId() != leaderClan.allyId()) {
			return DismissResult.NOT_ALLIANCE_LEADER;
		}
		if (targetClan.clanId() == leaderClan.clanId()) {
			return DismissResult.CANNOT_DISMISS_LEADER_CLAN;
		}
		if (targetClan.allyId() != leaderClan.allyId()) {
			return DismissResult.TARGET_NOT_IN_ALLIANCE;
		}

		long now = System.currentTimeMillis();
		leaderClan.setAllyPenalty(now + getPenaltyAcceptNewClanDaysMillis(), Clan.PENALTY_TYPE_DISMISS_CLAN);
		targetClan.allyId(0);
		targetClan.allyName(null);
		targetClan.allyCrestId(0);
		targetClan.setAllyPenalty(now + getPenaltyDismissDaysMillis(), Clan.PENALTY_TYPE_CLAN_DISMISSED);

		if (jdbc != null) {
			try {
				jdbc.sql("""
						UPDATE clan_data
						SET ally_penalty_expiry_time = :exp, ally_penalty_type = :type
						WHERE clan_id = :clanId
						""")
						.param("exp", leaderClan.allyPenaltyExpiryTime())
						.param("type", leaderClan.allyPenaltyType())
						.param("clanId", leaderClan.clanId())
						.update();

				jdbc.sql("""
						UPDATE clan_data
						SET ally_id = 0, ally_name = NULL, ally_crest_id = 0,
						    ally_penalty_expiry_time = :exp, ally_penalty_type = :type
						WHERE clan_id = :clanId
						""")
						.param("exp", targetClan.allyPenaltyExpiryTime())
						.param("type", targetClan.allyPenaltyType())
						.param("clanId", targetClan.clanId())
						.update();
			} catch (Exception ex) {
				log.error("Erro ao persistir expulsao do cla {} da alianca", targetClan.clanId(), ex);
				return DismissResult.ERROR;
			}
		}

		log.info("Cla '{}' foi expulso da alianca pelo lider '{}'", targetClan.name(), leaderClan.name());
		return DismissResult.SUCCESS;
	}

	/**
	 * Dissolve a alianca completamente.
	 */
	public synchronized DissolveResult dissolveAlliance(PlayerCharacter allyLeader) {
		if (allyLeader == null || allyLeader.clanId() <= 0) {
			return DissolveResult.NOT_IN_ALLIANCE;
		}
		var leaderClanOpt = clanTable.byClanId(allyLeader.clanId());
		if (leaderClanOpt.isEmpty()) {
			return DissolveResult.NOT_IN_ALLIANCE;
		}
		Clan leaderClan = leaderClanOpt.get();
		int allyId = leaderClan.allyId();
		if (allyId <= 0) {
			return DissolveResult.NOT_IN_ALLIANCE;
		}
		if (!leaderClan.isLeader(allyLeader.objectId()) || leaderClan.clanId() != allyId) {
			return DissolveResult.NOT_ALLIANCE_LEADER;
		}

		long now = System.currentTimeMillis();
		for (Clan clan : clanTable.getClanAllies(allyId)) {
			if (clan.clanId() != allyId) {
				clan.allyId(0);
				clan.allyName(null);
				clan.allyCrestId(0);
				clan.setAllyPenalty(0L, Clan.PENALTY_TYPE_NONE);
				if (jdbc != null) {
					try {
						jdbc.sql("""
								UPDATE clan_data
								SET ally_id = 0, ally_name = NULL, ally_crest_id = 0,
								    ally_penalty_expiry_time = 0, ally_penalty_type = 0
								WHERE clan_id = :clanId
								""").param("clanId", clan.clanId()).update();
					} catch (Exception ignored) {}
				}
			}
		}

		leaderClan.allyId(0);
		leaderClan.allyName(null);
		leaderClan.allyCrestId(0);
		leaderClan.setAllyPenalty(now + getPenaltyCreateDaysMillis(), Clan.PENALTY_TYPE_DISSOLVE_ALLY);

		if (jdbc != null) {
			try {
				jdbc.sql("""
						UPDATE clan_data
						SET ally_id = 0, ally_name = NULL, ally_crest_id = 0,
						    ally_penalty_expiry_time = :exp, ally_penalty_type = :type
						WHERE clan_id = :clanId
						""")
						.param("exp", leaderClan.allyPenaltyExpiryTime())
						.param("type", leaderClan.allyPenaltyType())
						.param("clanId", leaderClan.clanId())
						.update();
			} catch (Exception ex) {
				log.error("Erro ao persistir dissolucao de alianca {}", allyId, ex);
				return DissolveResult.ERROR;
			}
		}

		log.info("Alianca ID {} dissolvida pelo lider {}", allyId, leaderClan.name());
		return DissolveResult.SUCCESS;
	}

	/**
	 * Atualiza o brasao da alianca para todos os clas membros.
	 */
	public synchronized boolean setAllyCrest(PlayerCharacter allyLeader, int crestId) {
		if (allyLeader == null || allyLeader.clanId() <= 0) {
			return false;
		}
		var leaderClanOpt = clanTable.byClanId(allyLeader.clanId());
		if (leaderClanOpt.isEmpty()) {
			return false;
		}
		Clan leaderClan = leaderClanOpt.get();
		int allyId = leaderClan.allyId();
		if (allyId <= 0 || !leaderClan.isLeader(allyLeader.objectId()) || leaderClan.clanId() != allyId) {
			return false;
		}

		for (Clan clan : clanTable.getClanAllies(allyId)) {
			clan.allyCrestId(crestId);
			if (jdbc != null) {
				try {
					jdbc.sql("UPDATE clan_data SET ally_crest_id = :crest WHERE clan_id = :clanId")
							.param("crest", crestId)
							.param("clanId", clan.clanId())
							.update();
				} catch (Exception ignored) {}
			}
		}

		log.info("Brasao da alianca {} atualizado para ID {}", allyId, crestId);
		return true;
	}

	/**
	 * Transmite uma mensagem de chat no canal de alianca ($).
	 */
	public void broadcastAllyChat(PlayerCharacter sender, String text, GameWorld world) {
		if (sender == null || sender.clanId() <= 0 || text == null || world == null) {
			return;
		}
		var clanOpt = clanTable.byClanId(sender.clanId());
		if (clanOpt.isEmpty() || clanOpt.get().allyId() <= 0) {
			return;
		}
		int allyId = clanOpt.get().allyId();
		var packet = new CreatureSay(sender.objectId(), CreatureSay.ALLIANCE, sender.name(), text);
		broadcastToAlliance(allyId, world, packet);
	}

	/**
	 * Transmite um pacote para todos os membros online de todos os clas da alianca.
	 */
	public void broadcastToAlliance(int allyId, GameWorld world, GameServerPacket packet) {
		if (allyId <= 0 || world == null || packet == null) {
			return;
		}
		for (Clan clan : clanTable.getClanAllies(allyId)) {
			clan.broadcastToOnlineMembers(world, packet);
		}
	}

	public List<Clan> getAllianceClans(int allyId) {
		return clanTable.getClanAllies(allyId);
	}
}
