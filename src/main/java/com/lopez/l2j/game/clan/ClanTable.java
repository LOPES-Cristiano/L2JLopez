package com.lopez.l2j.game.clan;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Tabela e gerenciador de clas do servidor (ClanTable do L2JDream).
 * Armazena todos os clas em memoria e sincroniza com as tabelas clan_data e characters.
 */
@Component
public class ClanTable {

	private static final Logger log = LoggerFactory.getLogger(ClanTable.class);
	private static final Pattern CLAN_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9]{3,16}$");

	private static volatile ClanTable instance;

	private final Map<Integer, Clan> byClanId = new ConcurrentHashMap<>();
	private final Map<String, Clan> byClanName = new ConcurrentHashMap<>();
	private final JdbcClient jdbc;
	private final ObjectIdFactory idFactory;
	private final ClanLevelUpPricesTable pricesTable;

	public ClanTable(JdbcClient jdbc, ObjectIdFactory idFactory) {
		this(jdbc, idFactory, null);
	}

	@Autowired
	public ClanTable(@Autowired(required = false) JdbcClient jdbc, ObjectIdFactory idFactory,
			@Autowired(required = false) ClanLevelUpPricesTable pricesTable) {
		this.jdbc = jdbc;
		this.idFactory = idFactory != null ? idFactory : ObjectIdFactory.sequential(0x40000000);
		this.pricesTable = pricesTable != null ? pricesTable : new ClanLevelUpPricesTable();
		instance = this;
		loadClans();
	}

	public static ClanTable getInstance() {
		return instance;
	}

	public static void setInstance(ClanTable table) {
		instance = table;
	}

	public int size() {
		return byClanId.size();
	}

	public Collection<Clan> allClans() {
		return Collections.unmodifiableCollection(byClanId.values());
	}

	public Optional<Clan> byClanId(int clanId) {
		return Optional.ofNullable(byClanId.get(clanId));
	}

	public Clan getClan(int clanId) {
		return byClanId.get(clanId);
	}

	public Optional<Clan> byName(String name) {
		if (name == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(byClanName.get(name.toLowerCase(java.util.Locale.ROOT)));
	}

	/**
	 * Cria um novo cla com o lider especificado.
	 *
	 * @return O cla criado ou null se falhar validacao
	 */
	public synchronized Clan createClan(PlayerCharacter leader, String clanName) {
		if (leader == null || clanName == null) {
			return null;
		}
		if (leader.clanId() != 0) {
			log.warn("{} tentou criar cla '{}' mas ja pertence a um cla ({})", leader.name(), clanName, leader.clanId());
			return null;
		}
		if (leader.hasClanJoinPenalty() || leader.hasClanCreatePenalty()) {
			log.warn("{} tentou criar cla '{}' mas possui penalidade de cla ativa", leader.name(), clanName);
			return null;
		}
		if (leader.level() < Config.MIN_LEVEL_TO_CREATE_PLEDGE && !leader.isGm()) {
			log.warn("{} tentou criar cla '{}' com nivel insuficiente ({})", leader.name(), clanName, leader.level());
			return null;
		}
		String cleanName = clanName.trim();
		if (!CLAN_NAME_PATTERN.matcher(cleanName).matches()) {
			log.warn("{} tentou criar cla com nome invalido: '{}'", leader.name(), cleanName);
			return null;
		}
		if (byClanName.containsKey(cleanName.toLowerCase(java.util.Locale.ROOT))) {
			log.warn("Cla '{}' ja existe", cleanName);
			return null;
		}

		int clanId = idFactory.nextId();
		Clan clan = new Clan(clanId, cleanName, leader.objectId(), leader.name(), 0);

		ClanMember leaderMember = new ClanMember(leader.objectId(), leader.name(), leader.level(),
				leader.classId(), leader.title(), true, 0);
		clan.addMember(leaderMember);

		byClanId.put(clanId, clan);
		byClanName.put(cleanName.toLowerCase(java.util.Locale.ROOT), clan);

		leader.clanId(clanId);

		if (jdbc != null) {
			try {
				jdbc.sql("""
						INSERT INTO clan_data (clan_id, clan_name, clan_level, leader_id, reputation_score)
						VALUES (:clanId, :name, 0, :leaderId, 0)
						""")
						.param("clanId", clanId)
						.param("name", cleanName)
						.param("leaderId", leader.objectId())
						.update();

				jdbc.sql("UPDATE characters SET clanid = :clanId WHERE charId = :charId")
						.param("clanId", clanId)
						.param("charId", leader.objectId())
						.update();
			} catch (Exception ex) {
				log.error("Erro ao persistir criacao de cla {}", cleanName, ex);
			}
		}

		log.info("Cla '{}' (ID {}) criado por {}", cleanName, clanId, leader.name());
		return clan;
	}

	/**
	 * Remove/dissolve um cla.
	 */
	public synchronized boolean dissolveClan(int clanId) {
		Clan clan = byClanId.remove(clanId);
		if (clan == null) {
			return false;
		}
		byClanName.remove(clan.name().toLowerCase(java.util.Locale.ROOT));

		for (ClanMember m : clan.members()) {
			if (jdbc != null) {
				try {
					jdbc.sql("UPDATE characters SET clanid = 0 WHERE charId = :charId")
							.param("charId", m.objectId())
							.update();
				} catch (Exception ignored) {}
			}
		}

		if (jdbc != null) {
			try {
				jdbc.sql("DELETE FROM clan_data WHERE clan_id = :clanId")
						.param("clanId", clanId)
						.update();
			} catch (Exception ex) {
				log.error("Erro ao deletar cla {} de clan_data", clanId, ex);
			}
		}

		log.info("Cla '{}' (ID {}) dissolvido", clan.name(), clanId);
		return true;
	}

	/**
	 * Eleva o nivel do cla se o lider atender a todos os requisitos da tabela de precos.
	 *
	 * @return true se o nivel foi aumentado com sucesso, false caso contrario
	 */
	public synchronized boolean levelUpClan(PlayerCharacter leader) {
		if (leader == null || leader.clanId() == 0) {
			return false;
		}
		Clan clan = byClanId.get(leader.clanId());
		if (clan == null || !clan.isLeader(leader.objectId())) {
			log.warn("{} tentou subir nivel do cla mas nao e o lider", leader.name());
			return false;
		}

		int nextLevel = clan.level() + 1;
		if (nextLevel > 8) {
			log.info("Cla {} ja atingiu o nivel maximo (8)", clan.name());
			return false;
		}

		var req = pricesTable.getRequirement(nextLevel);
		if (req != null) {
			// Validacao de SP
			if (leader.sp() < req.sp()) {
				log.warn("{} nao tem SP suficiente para cla lvl {} (requer {}, possui {})",
						leader.name(), nextLevel, req.sp(), leader.sp());
				return false;
			}

			// Validacao de Reputacao
			if (clan.reputationScore() < req.reputation()) {
				log.warn("Cla {} nao tem reputacao suficiente para lvl {} (requer {}, possui {})",
						clan.name(), nextLevel, req.reputation(), clan.reputationScore());
				return false;
			}

			// Validacao de Membros
			if (clan.membersCount() < req.needMembers()) {
				log.warn("Cla {} nao tem membros suficientes para lvl {} (requer {}, possui {})",
						clan.name(), nextLevel, req.needMembers(), clan.membersCount());
				return false;
			}

			// Validacao de Itens (Adena, Blood Mark, Alliance Manifesto, Seal of Aspiration)
			var inv = leader.inventory();
			if (inv != null) {
				for (var entry : req.items().entrySet()) {
					int itemId = entry.getKey();
					int count = entry.getValue();
					if (inv.getItemCount(itemId) < count) {
						log.warn("{} nao possui item {} x{} para cla lvl {}", leader.name(), itemId, count, nextLevel);
						return false;
					}
				}

				// Se todas as validacoes passaram, desconta os itens e SP
				for (var entry : req.items().entrySet()) {
					inv.destroyItemByItemId(entry.getKey(), entry.getValue());
				}
			}

			if (req.sp() > 0) {
				leader.sp(leader.sp() - req.sp());
			}

			if (req.reputation() > 0) {
				clan.reputationScore(clan.reputationScore() - req.reputation());
			}
		}

		clan.level(nextLevel);

		if (jdbc != null) {
			try {
				jdbc.sql("""
						UPDATE clan_data
						SET clan_level = :lvl, reputation_score = :rep
						WHERE clan_id = :id
						""")
						.param("lvl", nextLevel)
						.param("rep", clan.reputationScore())
						.param("id", clan.clanId())
						.update();
			} catch (Exception ex) {
				log.error("Erro ao persistir nivel {} do cla {}", nextLevel, clan.name(), ex);
			}
		}

		log.info("Cla '{}' subiu para o nivel {} com sucesso pelo lider {}", clan.name(), nextLevel, leader.name());
		return true;
	}

	/**
	 * Tenta adicionar um jogador a um cla, aplicando validacoes de penalidade e limite de membros (V.34).
	 *
	 * @param clan O cla de destino
	 * @param player O jogador a ser adicionado
	 * @return true se adicionado com sucesso, false caso contrario
	 */
	public synchronized boolean addClanMember(Clan clan, PlayerCharacter player) {
		if (clan == null || player == null) {
			return false;
		}
		if (player.clanId() != 0) {
			log.warn("{} ja pertence a outro cla ({})", player.name(), player.clanId());
			return false;
		}
		if (player.hasClanJoinPenalty()) {
			log.warn("{} possui penalidade ativa para entrar em cla ate {}", player.name(), player.clanJoinExpiryTime());
			return false;
		}
		if (clan.hasCharPenalty()) {
			log.warn("Cla {} possui penalidade ativa para recrutar novos membros ate {}", clan.name(), clan.charPenaltyExpiryTime());
			return false;
		}

		int maxMembers = clan.getMaxMembers(clan.level());
		if (clan.membersCount() >= maxMembers) {
			log.warn("Cla {} atingiu o limite maximo de membros ({}) para o nivel {}", clan.name(), maxMembers, clan.level());
			return false;
		}

		ClanMember member = new ClanMember(player.objectId(), player.name(), player.level(),
				player.classId(), player.title(), false, 0);
		clan.addMember(member);
		player.clanId(clan.clanId());

		if (jdbc != null) {
			try {
				jdbc.sql("UPDATE characters SET clanid = :clanId WHERE charId = :charId")
						.param("clanId", clan.clanId())
						.param("charId", player.objectId())
						.update();
			} catch (Exception ex) {
				log.error("Erro ao persistir adicao de membro {} ao cla {}", player.name(), clan.name(), ex);
			}
		}

		log.info("Jogador {} adicionado ao cla {}", player.name(), clan.name());
		return true;
	}

	/**
	 * Remove um membro do cla (por saida voluntaria ou expulsao) aplicando as penalidades de 24h (V.34).
	 *
	 * @param clan O cla
	 * @param player O jogador que esta saindo ou sendo expulso
	 * @param isDismissed true se for expulsao pelo lider, false se for saida voluntaria
	 * @return true se o membro foi removido com sucesso
	 */
	public synchronized boolean removeClanMember(Clan clan, PlayerCharacter player, boolean isDismissed) {
		if (clan == null || player == null) {
			return false;
		}
		if (clan.isLeader(player.objectId())) {
			log.warn("Lider do cla {} nao pode ser expulso ou sair do cla diretamente", clan.name());
			return false;
		}

		ClanMember member = clan.removeMember(player.objectId());
		if (member == null) {
			return false;
		}

		player.clanId(0);
		long penaltyTime = System.currentTimeMillis() + (Math.max(1, Config.DAYS_BEFORE_JOIN_A_CLAN) * 86_400_000L);
		player.clanJoinExpiryTime(penaltyTime);

		if (isDismissed) {
			clan.charPenaltyExpiryTime(penaltyTime);
			if (jdbc != null) {
				try {
					jdbc.sql("UPDATE clan_data SET char_penalty_expiry_time = :exp WHERE clan_id = :clanId")
							.param("exp", penaltyTime)
							.param("clanId", clan.clanId())
							.update();
				} catch (Exception ex) {
					log.error("Erro ao persistir char_penalty_expiry_time do cla {}", clan.name(), ex);
				}
			}
		}

		if (jdbc != null) {
			try {
				jdbc.sql("UPDATE characters SET clanid = 0, clan_join_expiry_time = :exp WHERE charId = :charId")
						.param("exp", penaltyTime)
						.param("charId", player.objectId())
						.update();
			} catch (Exception ex) {
				log.error("Erro ao persistir remocao de membro {} do cla {}", player.name(), clan.name(), ex);
			}
		}

		log.info("Jogador {} removido do cla {} (expulso: {}), penalidade ate {}", player.name(), clan.name(), isDismissed, penaltyTime);
		return true;
	}

	public void updateClanLevel(int clanId, int newLevel) {
		Clan clan = byClanId.get(clanId);
		if (clan != null) {
			clan.level(newLevel);
			if (jdbc != null) {
				try {
					jdbc.sql("UPDATE clan_data SET clan_level = :lvl WHERE clan_id = :id")
							.param("lvl", newLevel)
							.param("id", clanId)
							.update();
				} catch (Exception ignored) {}
			}
		}
	}

	public void updateClanReputation(int clanId, int newReputation) {
		Clan clan = byClanId.get(clanId);
		if (clan != null) {
			clan.reputationScore(newReputation);
			if (jdbc != null) {
				try {
					jdbc.sql("UPDATE clan_data SET reputation_score = :rep WHERE clan_id = :id")
							.param("rep", newReputation)
							.param("id", clanId)
							.update();
				} catch (Exception ignored) {}
			}
		}
	}

	public boolean isAllyExists(String allyName) {
		if (allyName == null || allyName.isBlank()) {
			return false;
		}
		for (Clan clan : byClanId.values()) {
			if (clan.allyName() != null && clan.allyName().equalsIgnoreCase(allyName.trim())) {
				return true;
			}
		}
		return false;
	}

	public java.util.List<Clan> getClanAllies(int allyId) {
		if (allyId <= 0) {
			return Collections.emptyList();
		}
		return byClanId.values().stream()
				.filter(c -> c.allyId() == allyId)
				.toList();
	}

	public void registerClan(Clan clan) {
		if (clan != null) {
			byClanId.put(clan.clanId(), clan);
			byClanName.put(clan.name().toLowerCase(java.util.Locale.ROOT), clan);
		}
	}

	public void updateCrest(int clanId, int crestId) {
		Clan clan = byClanId.get(clanId);
		if (clan != null) {
			clan.crestId(crestId);
			if (jdbc != null) {
				try {
					jdbc.sql("UPDATE clan_data SET crest_id = :crest WHERE clan_id = :id")
							.param("crest", crestId)
							.param("id", clanId)
							.update();
				} catch (Exception ignored) {}
			}
		}
	}

	private void loadClans() {
		if (jdbc == null) {
			return;
		}
		try {
			var rows = jdbc.sql("""
					SELECT clan_id, clan_name, clan_level, hasCastle, hasFort, ally_id, ally_name,
					       leader_id, crest_id, crest_large_id, ally_crest_id, reputation_score,
					       ally_penalty_expiry_time, ally_penalty_type, char_penalty_expiry_time,
					       dissolving_expiry_time, auction_bid_at
					FROM clan_data
					""").query().listOfRows();

			for (var row : rows) {
				int clanId = ((Number) row.get("clan_id")).intValue();
				String name = (String) row.get("clan_name");
				int level = row.get("clan_level") != null ? ((Number) row.get("clan_level")).intValue() : 0;
				int leaderId = row.get("leader_id") != null ? ((Number) row.get("leader_id")).intValue() : 0;

				Clan clan = new Clan(clanId, name, leaderId, "", level);
				if (row.get("hasCastle") != null) clan.castleId(((Number) row.get("hasCastle")).intValue());
				if (row.get("hasFort") != null) clan.fortId(((Number) row.get("hasFort")).intValue());
				if (row.get("ally_id") != null) clan.allyId(((Number) row.get("ally_id")).intValue());
				if (row.get("ally_name") != null) clan.allyName((String) row.get("ally_name"));
				if (row.get("crest_id") != null) clan.crestId(((Number) row.get("crest_id")).intValue());
				if (row.get("crest_large_id") != null) clan.crestLargeId(((Number) row.get("crest_large_id")).intValue());
				if (row.get("ally_crest_id") != null) clan.allyCrestId(((Number) row.get("ally_crest_id")).intValue());
				if (row.get("reputation_score") != null) clan.reputationScore(((Number) row.get("reputation_score")).intValue());
				if (row.get("ally_penalty_expiry_time") != null) {
					clan.setAllyPenalty(((Number) row.get("ally_penalty_expiry_time")).longValue(),
							row.get("ally_penalty_type") != null ? ((Number) row.get("ally_penalty_type")).intValue() : 0);
				}
				if (row.get("char_penalty_expiry_time") != null) clan.charPenaltyExpiryTime(((Number) row.get("char_penalty_expiry_time")).longValue());
				if (row.get("dissolving_expiry_time") != null) clan.dissolvingExpiryTime(((Number) row.get("dissolving_expiry_time")).longValue());
				if (row.get("auction_bid_at") != null) clan.auctionBiddedAt(((Number) row.get("auction_bid_at")).intValue());

				byClanId.put(clanId, clan);
				if (name != null) {
					byClanName.put(name.toLowerCase(java.util.Locale.ROOT), clan);
				}
			}

			// Carrega os membros de cada cla a partir da tabela characters
			var membersRows = jdbc.sql("""
					SELECT charId, char_name, level, classid, title, clanid
					FROM characters WHERE clanid > 0
					""").query().listOfRows();

			for (var mRow : membersRows) {
				int clanId = ((Number) mRow.get("clanid")).intValue();
				Clan clan = byClanId.get(clanId);
				if (clan != null) {
					int charId = ((Number) mRow.get("charId")).intValue();
					String charName = (String) mRow.get("char_name");
					int lvl = ((Number) mRow.get("level")).intValue();
					int classId = ((Number) mRow.get("classid")).intValue();
					String title = (String) mRow.get("title");

					ClanMember member = new ClanMember(charId, charName, lvl, classId, title, false, 0);
					clan.addMember(member);
					if (clan.leaderId() == charId) {
						clan.leaderName(charName);
					}
				}
			}

			// Carrega guerras de clan_wars
			try {
				var warRows = jdbc.sql("SELECT clan1, clan2, expiry_time FROM clan_wars").query().listOfRows();
				long now = System.currentTimeMillis();
				for (var row : warRows) {
					int c1 = Integer.parseInt(row.get("clan1").toString());
					int c2 = Integer.parseInt(row.get("clan2").toString());
					long expiry = row.get("expiry_time") != null ? ((Number) row.get("expiry_time")).longValue() : 0L;
					Clan clan1 = byClanId.get(c1);
					Clan clan2 = byClanId.get(c2);
					if (clan1 != null && clan2 != null) {
						if (expiry > 0) {
							if (expiry > now) {
								clan1.addWarPenaltyTime(c2, expiry);
							}
						} else {
							clan1.addEnemyClan(c2);
							clan2.addAttackerClan(c1);
						}
					}
				}
			} catch (Exception ex) {
				log.debug("Aviso ao carregar clan_wars: {}", ex.getMessage());
			}

			// Carrega clan_skills
			try {
				var skillRows = jdbc.sql("SELECT clan_id, skill_id, skill_level FROM clan_skills").query().listOfRows();
				for (var row : skillRows) {
					int cid = ((Number) row.get("clan_id")).intValue();
					int sid = ((Number) row.get("skill_id")).intValue();
					int slvl = ((Number) row.get("skill_level")).intValue();
					Clan clan = byClanId.get(cid);
					if (clan != null) {
						clan.addSkill(sid, slvl);
					}
				}
			} catch (Exception ex) {
				log.debug("Aviso ao carregar clan_skills: {}", ex.getMessage());
			}

			// Carrega clan_subpledges
			try {
				var subRows = jdbc.sql("SELECT clan_id, sub_pledge_id, name, leader_id FROM clan_subpledges").query().listOfRows();
				for (var row : subRows) {
					int cid = ((Number) row.get("clan_id")).intValue();
					int subId = ((Number) row.get("sub_pledge_id")).intValue();
					String sname = (String) row.get("name");
					int sLeader = ((Number) row.get("leader_id")).intValue();
					Clan clan = byClanId.get(cid);
					if (clan != null) {
						clan.addSubPledge(subId, sname, sLeader);
					}
				}
			} catch (Exception ex) {
				log.debug("Aviso ao carregar clan_subpledges: {}", ex.getMessage());
			}

			// Carrega clan_privs
			try {
				var privRows = jdbc.sql("SELECT clan_id, rank, privilleges FROM clan_privs").query().listOfRows();
				for (var row : privRows) {
					int cid = ((Number) row.get("clan_id")).intValue();
					int rank = ((Number) row.get("rank")).intValue();
					int privs = ((Number) row.get("privilleges")).intValue();
					Clan clan = byClanId.get(cid);
					if (clan != null) {
						clan.setRankPrivilege(rank, privs);
					}
				}
			} catch (Exception ex) {
				log.debug("Aviso ao carregar clan_privs: {}", ex.getMessage());
			}

			log.info("ClanTable: {} clas carregados da base de dados", byClanId.size());
		} catch (Exception ex) {
			log.warn("Nao foi possivel carregar clas de clan_data (pode ser ambiente inicial ou de testes): {}", ex.getMessage());
		}
	}
}
