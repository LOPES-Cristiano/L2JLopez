package com.lopez.l2j.game.castle.reward;

import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.clan.ClanMember;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.item.ItemTemplateTable;
import com.lopez.l2j.game.model.PlayerCharacter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Servico de premiacao e recompensas adicionais de cerco a castelos (SiegeRewardManager do L2JDream).
 * Distribui recompensas automaticas (Blood Alliance, Knight's Epaulettes, Adena, Medals) para os
 * membros do cla vitorioso, suportando entrega imediata (online) e pendente via tabela reward_list (offline).
 */
@Service
public class SiegeRewardService {

	private static final Logger log = LoggerFactory.getLogger(SiegeRewardService.class);

	public record RewardRule(int itemId, long count, boolean leaderOnly) {}

	// Regras padrao de premiacao de cerco do Interlude
	public static final int BLOOD_ALLIANCE = 9911; // Usado para level-up de cla e skills
	public static final int KNIGHT_EPAULETTE = 9912;
	public static final int ADENA = 57;

	private final List<RewardRule> rewardRules = new CopyOnWriteArrayList<>();
	// charId -> Lista de recompensas pendentes
	private final Map<Integer, List<SiegeRewardRecord>> pendingRewards = new ConcurrentHashMap<>();

	private final ItemTemplateTable itemTable;
	private final ObjectIdFactory idFactory;
	private final JdbcClient jdbc;

	@Autowired
	public SiegeRewardService(@Autowired(required = false) ItemTemplateTable itemTable,
							  @Autowired(required = false) ObjectIdFactory idFactory,
							  @Autowired(required = false) JdbcClient jdbc) {
		this.itemTable = itemTable;
		this.idFactory = idFactory != null ? idFactory : ObjectIdFactory.sequential(0x55000000);
		this.jdbc = jdbc;

		initDefaultRules();
		loadOfflineRewards();
	}

	private void initDefaultRules() {
		// Lorde recebe 1 Blood Alliance e 10.000.000 Adena
		rewardRules.add(new RewardRule(BLOOD_ALLIANCE, 1, true));
		rewardRules.add(new RewardRule(ADENA, 10_000_000L, true));
		// Todos os membros recebem 50 Knight Epaulettes e 1.000.000 Adena
		rewardRules.add(new RewardRule(KNIGHT_EPAULETTE, 50, false));
		rewardRules.add(new RewardRule(ADENA, 1_000_000L, false));
	}

	public void addRewardRule(int itemId, long count, boolean leaderOnly) {
		rewardRules.add(new RewardRule(itemId, count, leaderOnly));
	}

	public List<RewardRule> getRewardRules() {
		return Collections.unmodifiableList(rewardRules);
	}

	public List<SiegeRewardRecord> getPendingRewards(int charId) {
		return pendingRewards.getOrDefault(charId, Collections.emptyList());
	}

	/**
	 * Notifica o encerramento do cerco e distribui recompensas aos membros do cla vencedor.
	 */
	public void rewardWinningClan(Clan winnerClan, String castleName, Map<Integer, PlayerCharacter> onlinePlayers) {
		if (winnerClan == null) {
			return;
		}

		for (ClanMember member : winnerClan.members()) {
			int charId = member.objectId();
			boolean isLeader = winnerClan.isLeader(charId);

			PlayerCharacter online = onlinePlayers != null ? onlinePlayers.get(charId) : null;

			for (RewardRule rule : rewardRules) {
				if (rule.leaderOnly() && !isLeader) {
					continue;
				}

				if (online != null && online.inventory() != null) {
					// Jogador online: entrega imediata
					deliverRewardToInventory(online, rule.itemId(), rule.count());
					log.info("Recompensa de cerco (item {} x{}) entregue diretamente a {}",
							rule.itemId(), rule.count(), online.name());
				} else {
					// Jogador offline: salva pendencia no banco e memoria
					SiegeRewardRecord rec = new SiegeRewardRecord(charId, rule.itemId(), rule.count(), castleName, false);
					pendingRewards.computeIfAbsent(charId, k -> new CopyOnWriteArrayList<>()).add(rec);
					saveRewardToDb(rec);
					log.info("Recompensa de cerco (item {} x{}) arquivada para entrega posterior a charId {}",
							rule.itemId(), rule.count(), charId);
				}
			}
		}
	}

	/**
	 * Reivindica e entrega todas as recompensas pendentes de um jogador ao entrar no mundo.
	 */
	public int claimRewards(PlayerCharacter player) {
		if (player == null || player.inventory() == null) {
			return 0;
		}

		List<SiegeRewardRecord> list = pendingRewards.remove(player.objectId());
		if (list == null || list.isEmpty()) {
			return 0;
		}

		int count = 0;
		for (SiegeRewardRecord rec : list) {
			deliverRewardToInventory(player, rec.itemId(), rec.count());
			deleteRewardFromDb(player.objectId(), rec.itemId());
			count++;
			log.info("Recompensa pendente de cerco (item {} x{}) entregue a {} ({})",
					rec.itemId(), rec.count(), player.name(), rec.castleName());
		}
		return count;
	}

	private void deliverRewardToInventory(PlayerCharacter player, int itemId, long count) {
		Inventory inv = player.inventory();
		var existingOpt = inv.byItemId(itemId);
		if (existingOpt.isPresent() && existingOpt.get().template().stackable()) {
			ItemInstance item = existingOpt.get();
			item.count((int) Math.min(Integer.MAX_VALUE, item.count() + count));
		} else {
			ItemTemplate tmpl = resolveTemplate(itemId);
			ItemInstance newItem = new ItemInstance(idFactory.nextId(), tmpl, player.objectId(), (int) Math.min(Integer.MAX_VALUE, count));
			inv.add(newItem);
		}
	}

	private ItemTemplate resolveTemplate(int itemId) {
		if (itemTable != null) {
			var opt = itemTable.get(itemId);
			if (opt.isPresent()) {
				return opt.get();
			}
		}
		return ItemTemplate.etc(itemId, itemId, "Siege Reward", "reward", "stackable", 1, "none", 0, true, true, true, true);
	}

	private void saveRewardToDb(SiegeRewardRecord rec) {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("""
					REPLACE INTO reward_list (charid, itemId, count, castle_name, rewarded)
					VALUES (:charId, :itemId, :count, :cName, :rew)
					""")
					.param("charId", rec.charId())
					.param("itemId", rec.itemId())
					.param("count", rec.count())
					.param("cName", rec.castleName())
					.param("rew", rec.rewarded() ? 1 : 0)
					.update();
		} catch (Exception ex) {
			log.warn("Erro ao salvar recompensa de cerco no banco: {}", ex.getMessage());
		}
	}

	private void deleteRewardFromDb(int charId, int itemId) {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("DELETE FROM reward_list WHERE charid = :cid AND itemId = :iid")
					.param("cid", charId)
					.param("iid", itemId)
					.update();
		} catch (Exception ex) {
			log.warn("Erro ao deletar recompensa resgatada de reward_list: {}", ex.getMessage());
		}
	}

	private void loadOfflineRewards() {
		if (jdbc == null) {
			return;
		}
		try {
			List<Map<String, Object>> rows = jdbc.sql("""
					SELECT charid, itemId, count, castle_name, rewarded
					FROM reward_list
					WHERE rewarded = 0
					""").query().listOfRows();

			for (var row : rows) {
				int charId = ((Number) row.get("charid")).intValue();
				int itemId = ((Number) row.get("itemId")).intValue();
				long count = ((Number) row.get("count")).longValue();
				String castleName = (String) row.get("castle_name");
				boolean rewarded = ((Number) row.get("rewarded")).intValue() == 1;

				SiegeRewardRecord rec = new SiegeRewardRecord(charId, itemId, count, castleName, rewarded);
				pendingRewards.computeIfAbsent(charId, k -> new CopyOnWriteArrayList<>()).add(rec);
			}
			log.info("SiegeRewardService: carregadas recompensas pendentes de reward_list");
		} catch (Exception ex) {
			log.warn("Nao foi possivel carregar reward_list: {}", ex.getMessage());
		}
	}
}
