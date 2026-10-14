package com.lopez.l2j.game.social;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Servico de casamento e pareamento de personagens (Item 29 do Roteiro Mestre).
 * Gerencia noivados, casamentos e divorcios com persistencia na tabela `couples`.
 */
@Service
public class WeddingService {

	private static final Logger log = LoggerFactory.getLogger(WeddingService.class);
	public static final int FORMAL_WEAR_ITEM_ID = 6408;

	public record Couple(int id, int player1Id, int player2Id, boolean married, long affiancedDate, long weddingDate) {}
	public record WeddingGift(int itemId, int count) {}

	private final JdbcClient jdbc;
	private final Map<Integer, Couple> couplesById = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> coupleIdByPlayer = new ConcurrentHashMap<>();

	public WeddingService(JdbcClient jdbc) {
		this.jdbc = jdbc;
		load();
	}

	public synchronized void load() {
		couplesById.clear();
		coupleIdByPlayer.clear();
		try {
			var list = jdbc.sql("SELECT id, player1Id, player2Id, maried, affiancedDate, weddingDate FROM couples")
					.query((rs, rowNum) -> new Couple(
							rs.getInt("id"),
							rs.getInt("player1Id"),
							rs.getInt("player2Id"),
							rs.getBoolean("maried"),
							rs.getLong("affiancedDate"),
							rs.getLong("weddingDate")
					))
					.list();
			for (Couple c : list) {
				couplesById.put(c.id(), c);
				coupleIdByPlayer.put(c.player1Id(), c.id());
				coupleIdByPlayer.put(c.player2Id(), c.id());
			}
			log.info("WeddingService: {} casais carregados do banco de dados.", list.size());
		} catch (Exception e) {
			log.warn("Erro ao carregar casais da tabela couples: {}", e.getMessage());
		}
	}

	public boolean isWeddingEnabled() {
		return Config.ALLOW_WEDDING;
	}

	public int getWeddingPrice() {
		return Math.max(0, Config.WEDDING_PRICE);
	}

	public boolean isPunishInfidelityEnabled() {
		return Config.WEDDING_PUNISH_INFIDELITY;
	}

	public boolean isWeddingTeleportEnabled() {
		return Config.WEDDING_TELEPORT;
	}

	public int getWeddingTeleportPrice() {
		return Math.max(0, Config.WEDDING_TELEPORT_PRICE);
	}

	public int getWeddingTeleportDurationSeconds() {
		return Math.max(1, Config.WEDDING_TELEPORT_DURATION);
	}

	public boolean isSameSexAllowed() {
		return Config.WEDDING_ALLOW_SAME_SEX;
	}

	public boolean isFormalWearRequired() {
		return Config.WEDDING_FORMAL_WEAR;
	}

	public int getDivorceCostsPercent() {
		return Math.max(0, Math.min(100, Config.WEDDING_DIVORCE_COSTS));
	}

	public long calculateDivorceCost(long playerAdena) {
		if (playerAdena <= 0 || getDivorceCostsPercent() <= 0) {
			return 0L;
		}
		return (playerAdena * getDivorceCostsPercent()) / 100L;
	}

	public boolean isWearingFormalWear(PlayerCharacter player) {
		if (player == null || player.inventory() == null || player.inventory().items() == null) {
			return false;
		}
		for (ItemInstance item : player.inventory().items()) {
			if (item != null && item.isEquipped() && item.itemId() == FORMAL_WEAR_ITEM_ID) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Valida as regras completas de cerimonia configuradas em fun_events.properties.
	 */
	public boolean canEngageWithCeremonyRules(PlayerCharacter player1, PlayerCharacter player2) {
		if (!isWeddingEnabled() || player1 == null || player2 == null || player1.objectId() == player2.objectId()) {
			return false;
		}
		if (coupleIdByPlayer.containsKey(player1.objectId()) || coupleIdByPlayer.containsKey(player2.objectId())) {
			return false;
		}
		if (!isSameSexAllowed() && player1.female() == player2.female()) {
			return false;
		}
		if (isFormalWearRequired() && (!isWearingFormalWear(player1) || !isWearingFormalWear(player2))) {
			return false;
		}
		return true;
	}

	public int getMarriedNickColor(PlayerCharacter player1, PlayerCharacter player2) {
		if (!Config.WEDDING_CHANGE_NICK_COLOR || player1 == null || player2 == null) {
			return Config.WEDDING_NORMAL_NICK_COLOR;
		}
		if (player1.female() && player2.female()) {
			return Config.WEDDING_LESBI_NICK_COLOR;
		}
		if (!player1.female() && !player2.female()) {
			return Config.WEDDING_GAY_NICK_COLOR;
		}
		return Config.WEDDING_NORMAL_NICK_COLOR;
	}

	public List<WeddingGift> getCeremonyGiftsForPlayer(PlayerCharacter player) {
		List<WeddingGift> gifts = new ArrayList<>();
		if (Config.WEDDING_GIVE_SALVATION_BOW && Config.WEDDING_BOW_ITEM_ID > 0) {
			gifts.add(new WeddingGift(Config.WEDDING_BOW_ITEM_ID, 1));
		}
		if (Config.WEDDING_GIVE_RING && player != null) {
			int ringId = player.female() ? Config.WEDDING_FEMALE_RING_ID : Config.WEDDING_MALE_RING_ID;
			if (ringId > 0) {
				gifts.add(new WeddingGift(ringId, 1));
			}
		}
		return gifts;
	}

	public Optional<Couple> getCouple(int coupleId) {
		return Optional.ofNullable(couplesById.get(coupleId));
	}

	public Optional<Couple> getCoupleForPlayer(int playerId) {
		Integer id = coupleIdByPlayer.get(playerId);
		if (id == null) {
			return Optional.empty();
		}
		return getCouple(id);
	}

	public int getPartnerId(int playerId) {
		var coupleOpt = getCoupleForPlayer(playerId);
		if (coupleOpt.isEmpty()) {
			return 0;
		}
		var c = coupleOpt.get();
		return c.player1Id() == playerId ? c.player2Id() : c.player1Id();
	}

	public boolean isMarried(int playerId) {
		return getCoupleForPlayer(playerId).map(Couple::married).orElse(false);
	}

	/**
	 * Cria noivado entre dois jogadores solteiros.
	 */
	public synchronized Couple engage(PlayerCharacter player1, PlayerCharacter player2) {
		if (player1 == null || player2 == null || player1.objectId() == player2.objectId()) {
			throw new IllegalArgumentException("Personagens invalidos para casamento.");
		}
		if (coupleIdByPlayer.containsKey(player1.objectId()) || coupleIdByPlayer.containsKey(player2.objectId())) {
			if (isPunishInfidelityEnabled()) {
				log.warn("WeddingService: Tentativa de bigamia/infidelidade detectada entre {} e {}",
						player1.name(), player2.name());
			}
			throw new IllegalStateException("Um dos jogadores ja possui noivado ou casamento ativo.");
		}

		long now = System.currentTimeMillis();
		int nextId = couplesById.keySet().stream().max(Integer::compareTo).orElse(0) + 1;

		Couple couple = new Couple(nextId, player1.objectId(), player2.objectId(), false, now, 0);

		try {
			jdbc.sql("""
					INSERT INTO couples (id, player1Id, player2Id, maried, affiancedDate, weddingDate)
					VALUES (:id, :p1, :p2, 0, :ad, 0)
					""")
					.param("id", nextId)
					.param("p1", player1.objectId())
					.param("p2", player2.objectId())
					.param("ad", now)
					.update();
		} catch (Exception e) {
			log.warn("Erro ao registrar noivado no banco de dados: {}", e.getMessage());
		}

		couplesById.put(nextId, couple);
		coupleIdByPlayer.put(player1.objectId(), nextId);
		coupleIdByPlayer.put(player2.objectId(), nextId);

		return couple;
	}

	/**
	 * Conclui a cerimonia oficializando o matrimomio.
	 */
	public synchronized boolean marry(int coupleId) {
		Couple c = couplesById.get(coupleId);
		if (c == null || c.married()) {
			return false;
		}

		long now = System.currentTimeMillis();
		Couple updated = new Couple(c.id(), c.player1Id(), c.player2Id(), true, c.affiancedDate(), now);

		try {
			jdbc.sql("UPDATE couples SET maried = 1, weddingDate = :wd WHERE id = :id")
					.param("wd", now)
					.param("id", coupleId)
					.update();
		} catch (Exception e) {
			log.warn("Erro ao registrar casamento id {}: {}", coupleId, e.getMessage());
		}

		couplesById.put(coupleId, updated);

		if (Config.WEDDING_ANNOUNCE) {
			String msg = "Wedding Ceremony #" + coupleId + " has been celebrated!";
			log.info(msg);
			GameWorld world = GameWorld.getInstance();
			if (world != null) {
				CreatureSay packet = new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Wedding", msg);
				for (var online : world.onlinePlayers()) {
					online.send(packet);
				}
			}
		}
		return true;
	}

	/**
	 * Realiza o divorcio e dissolve o casal.
	 */
	public synchronized void divorce(int coupleId) {
		Couple c = couplesById.remove(coupleId);
		if (c != null) {
			coupleIdByPlayer.remove(c.player1Id());
			coupleIdByPlayer.remove(c.player2Id());

			try {
				jdbc.sql("DELETE FROM couples WHERE id = :id")
						.param("id", coupleId)
						.update();
			} catch (Exception e) {
				log.warn("Erro ao remover casal id {}: {}", coupleId, e.getMessage());
			}
		}
	}

	/**
	 * Verifica condicoes para teleporte do jogador ate seu conjuge.
	 */
	public boolean canTeleportToPartner(PlayerCharacter actor, PlayerCharacter partner) {
		if (!isWeddingTeleportEnabled() || actor == null || partner == null) {
			return false;
		}
		if (!isMarried(actor.objectId())) {
			return false;
		}
		if (getPartnerId(actor.objectId()) != partner.objectId()) {
			return false;
		}
		if (actor.isDead() || partner.isDead()) {
			return false;
		}
		if (actor.karma() > 0 || partner.karma() > 0) {
			return false;
		}
		return true;
	}
}
