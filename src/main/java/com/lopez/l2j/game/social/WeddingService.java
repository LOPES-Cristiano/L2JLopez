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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Servico de casamento e pareamento de personagens (Item 29 do Roteiro Mestre / #490).
 * Gerencia noivados, casamentos (com verificacao de Formal Wear 6408 e entrega de Cupid's Bow 9140)
 * e divorcios com persistencia na tabela `couples`.
 */
@Service
public class WeddingService {

	private static final Logger log = LoggerFactory.getLogger(WeddingService.class);
	public static final int FORMAL_WEAR_ITEM_ID = 6408;
	public static final int CUPIDS_BOW_ITEM_ID = 9140;

	public record Couple(int id, int player1Id, int player2Id, boolean married, long affiancedDate, long weddingDate) {}
	public record WeddingGift(int itemId, int count) {}

	private final JdbcClient jdbc;
	private final Map<Integer, Couple> couplesById = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> coupleIdByPlayer = new ConcurrentHashMap<>();

	public WeddingService() {
		this(null);
	}

	@Autowired
	public WeddingService(@Autowired(required = false) JdbcClient jdbc) {
		this.jdbc = jdbc;
		load();
	}

	public synchronized void load() {
		couplesById.clear();
		coupleIdByPlayer.clear();
		if (jdbc == null) {
			return;
		}
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
		return Math.max(1, Config.WEDDING_TELEPORT_INTERVAL);
	}

	public boolean isSpawnWeddingNpcEnabled() {
		return Config.SPAWN_WEDDING_NPC;
	}

	public boolean isHoneyMoonEnabled() {
		return Config.WEDDING_HONEYMOON;
	}

	public int[] getHoneyMoonTeleportLoc() {
		return new int[] { Config.WEDDING_TELEPORT_X, Config.WEDDING_TELEPORT_Y, Config.WEDDING_TELEPORT_Z };
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

	private static int parseColorHex(String hex, int fallback) {
		if (hex == null || hex.isBlank()) {
			return fallback;
		}
		try {
			return Integer.decode("0x" + hex.trim().replace("0x", "").replace("#", ""));
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	public int getMarriedNickColor(PlayerCharacter player1, PlayerCharacter player2) {
		if (!Config.WEDDING_USE_NICK_COLOR || player1 == null || player2 == null) {
			return parseColorHex(Config.WEDDING_NORMAL_PAIR_NICK_COLOR, 0xBF0000);
		}
		if (player1.female() && player2.female()) {
			return parseColorHex(Config.WEDDING_LESBI_PAIR_NICK_COLOR, 0xBF00BF);
		}
		if (!player1.female() && !player2.female()) {
			return parseColorHex(Config.WEDDING_GAY_PAIR_NICK_COLOR, 0x0000BF);
		}
		return parseColorHex(Config.WEDDING_NORMAL_PAIR_NICK_COLOR, 0xBF0000);
	}

	public List<WeddingGift> getCeremonyGiftsForPlayer(PlayerCharacter player) {
		List<WeddingGift> gifts = new ArrayList<>();
		if (Config.WEDDING_GIVE_BOW) {
			gifts.add(new WeddingGift(CUPIDS_BOW_ITEM_ID, 1));
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

		if (jdbc != null) {
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
		}

		couplesById.put(nextId, couple);
		coupleIdByPlayer.put(player1.objectId(), nextId);
		coupleIdByPlayer.put(player2.objectId(), nextId);

		return couple;
	}

	/**
	 * Conclui a cerimonia oficializando o matrimonio, entregando Cupid's Bow (9140)
	 * e aplicando cor de nick quando os conjuges estao online no mundo.
	 */
	public synchronized boolean marry(int coupleId) {
		Couple c = couplesById.get(coupleId);
		if (c == null || c.married()) {
			return false;
		}

		long now = System.currentTimeMillis();
		Couple updated = new Couple(c.id(), c.player1Id(), c.player2Id(), true, c.affiancedDate(), now);

		if (jdbc != null) {
			try {
				jdbc.sql("UPDATE couples SET maried = 1, weddingDate = :wd WHERE id = :id")
						.param("wd", now)
						.param("id", coupleId)
						.update();
			} catch (Exception e) {
				log.warn("Erro ao registrar casamento id {}: {}", coupleId, e.getMessage());
			}
		}

		couplesById.put(coupleId, updated);

		GameWorld world = GameWorld.getInstance();
		if (world != null) {
			PlayerCharacter p1 = world.player(c.player1Id()).map(GameWorld.OnlinePlayer::character).orElse(null);
			PlayerCharacter p2 = world.player(c.player2Id()).map(GameWorld.OnlinePlayer::character).orElse(null);
			applyPostMarriageBenefits(p1, p2);
		}

		if (Config.ALLOW_WEDDING) {
			String msg = "Wedding Ceremony #" + coupleId + " has been celebrated!";
			log.info(msg);
			if (world != null) {
				CreatureSay packet = new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Wedding", msg);
				for (var online : world.onlinePlayers()) {
					online.send(packet);
				}
			}
		}
		return true;
	}

	public void applyPostMarriageBenefits(PlayerCharacter p1, PlayerCharacter p2) {
		if (p1 != null && p2 != null && Config.WEDDING_USE_NICK_COLOR) {
			int color = getMarriedNickColor(p1, p2);
			p1.nameColor(color);
			p2.nameColor(color);
		}
		for (PlayerCharacter p : new PlayerCharacter[]{ p1, p2 }) {
			if (p == null || p.inventory() == null) continue;
			for (WeddingGift gift : getCeremonyGiftsForPlayer(p)) {
				if (p.inventory().byItemId(gift.itemId()).isEmpty()) {
					p.inventory().addItem(gift.itemId(), gift.count());
				}
			}
			if (isHoneyMoonEnabled()) {
				int[] loc = getHoneyMoonTeleportLoc();
				if (loc[0] != 0 || loc[1] != 0) {
					p.teleport(loc[0], loc[1], loc[2]);
				}
			}
		}
	}

	/**
	 * Realiza o divorcio e dissolve o casal.
	 */
	public synchronized void divorce(int coupleId) {
		Couple c = couplesById.remove(coupleId);
		if (c != null) {
			coupleIdByPlayer.remove(c.player1Id());
			coupleIdByPlayer.remove(c.player2Id());

			if (jdbc != null) {
				try {
					jdbc.sql("DELETE FROM couples WHERE id = :id")
							.param("id", coupleId)
							.update();
				} catch (Exception e) {
					log.warn("Erro ao remover casal id {}: {}", coupleId, e.getMessage());
				}
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
		if (actor.isInCombat() || partner.isInCombat() || actor.isInJail() || partner.isInJail()
				|| actor.isOlympiadMode() || partner.isOlympiadMode()) {
			return false;
		}
		return true;
	}
}
