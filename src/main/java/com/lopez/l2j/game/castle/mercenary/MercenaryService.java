package com.lopez.l2j.game.castle.mercenary;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.castle.Castle;
import com.lopez.l2j.game.castle.CastleManager;
import com.lopez.l2j.game.castle.siege.SiegeService;
import com.lopez.l2j.game.castle.siege.SiegeStatus;
import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.npc.NpcTemplateTable;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.game.world.GameWorld;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Servico de gerenciamento de bilhetes de mercenarios defensores de castelo (MercTicketManager do L2JDream).
 * Permite que lordes contratem guardas e arqueiros para defender as muralhas durante o cerco.
 */
@Service
public class MercenaryService {

	private static final Logger log = LoggerFactory.getLogger(MercenaryService.class);

	public static final long DEFAULT_TICKET_BASE_PRICE = 10_000L;

	private final CastleManager castleManager;
	private final SiegeService siegeService;
	private final JdbcClient jdbc;
	private InventoryService inventoryService;
	private GameWorld gameWorld;
	private NpcTemplateTable npcTemplates;
	private ObjectIdFactory idFactory;
	private final AtomicInteger idCounter = new AtomicInteger(1000);

	// castleId -> Lista de MercenaryRecord contratados
	private final Map<Integer, List<MercenaryRecord>> hiredMercs = new ConcurrentHashMap<>();
	// castleId -> Lista de NpcInstance spawnados no mundo
	private final Map<Integer, List<NpcInstance>> spawnedMercNpcs = new ConcurrentHashMap<>();

	public MercenaryService(CastleManager castleManager,
							SiegeService siegeService,
							JdbcClient jdbc) {
		this(castleManager, siegeService, jdbc, null, null, null, null);
	}

	@Autowired
	public MercenaryService(CastleManager castleManager,
							@Autowired(required = false) SiegeService siegeService,
							@Autowired(required = false) JdbcClient jdbc,
							@Autowired(required = false) InventoryService inventoryService,
							@Autowired(required = false) GameWorld gameWorld,
							@Autowired(required = false) NpcTemplateTable npcTemplates,
							@Autowired(required = false) ObjectIdFactory idFactory) {
		this.castleManager = castleManager;
		this.siegeService = siegeService;
		this.jdbc = jdbc;
		this.inventoryService = inventoryService;
		this.gameWorld = gameWorld;
		this.npcTemplates = npcTemplates;
		this.idFactory = idFactory != null ? idFactory : ObjectIdFactory.sequential(0x56000000);
		loadHiredMercenaries();
	}

	/**
	 * Limite maximo de mercenarios por castelo (baseado no legado L2JDream e Config.MAX_GUARD_COUNT).
	 */
	public int getMaxMercenaries(int castleId) {
		int baseMax = switch (castleId) {
			case CastleManager.GLUDIO, CastleManager.GIRAN, CastleManager.OREN -> 40;
			case CastleManager.DION -> 60;
			case CastleManager.ADEN, CastleManager.INNADRIL, CastleManager.GODDARD,
				 CastleManager.RUNE, CastleManager.SCHUTTGART -> 80;
			default -> 30;
		};
		int globalMax = Config.MAX_GUARD_COUNT;
		return globalMax > 0 ? Math.min(baseMax, globalMax) : baseMax;
	}

	public boolean isSpawnSiegeGuardEnabled() {
		return Config.SPAWN_SIEGE_GUARD;
	}

	public long calculateGuardPrice(long basePrice) {
		float rate = Config.RATE_SIEGE_GUARDS_PRICE > 0 ? Config.RATE_SIEGE_GUARDS_PRICE : 1.0f;
		return Math.max(1L, Math.round(basePrice * rate));
	}

	/**
	 * Retorna a lista de mercenarios contratados para determinado castelo.
	 */
	public List<MercenaryRecord> getHiredMercenaries(int castleId) {
		if (!Config.SPAWN_SIEGE_GUARD) {
			return Collections.emptyList();
		}
		return hiredMercs.getOrDefault(castleId, Collections.emptyList());
	}

	public List<NpcInstance> getSpawnedMercenaryNpcs(int castleId) {
		return spawnedMercNpcs.getOrDefault(castleId, Collections.emptyList());
	}

	/**
	 * Compra bilhetes de mercenario para o castelo cobrando Adena via InventoryService.
	 */
	public boolean buyMercenaryTicket(PlayerCharacter player, int castleId, int ticketItemId, int count) {
		if (player == null || count <= 0 || player.inventory() == null) {
			return false;
		}
		if (castleManager != null) {
			var castleOpt = castleManager.getCastleById(castleId);
			if (castleOpt.isEmpty() || castleOpt.get().ownerClanId() <= 0 || castleOpt.get().ownerClanId() != player.clanId()) {
				return false;
			}
		}
		long totalPrice = calculateGuardPrice(DEFAULT_TICKET_BASE_PRICE) * count;
		if (totalPrice > Integer.MAX_VALUE) {
			return false;
		}
		int adenaCost = (int) totalPrice;
		Inventory inv = player.inventory();
		if (inv.getItemCount(ItemTemplate.ADENA_ID) < adenaCost) {
			return false;
		}
		if (inventoryService != null) {
			if (inventoryService.consumeItem(inv, ItemTemplate.ADENA_ID, adenaCost, "BuyMercenaryTicket") == null) {
				return false;
			}
			var added = inventoryService.addItem(inv, ticketItemId, count, "BuyMercenaryTicket");
			if (added != null) {
				return true;
			}
		} else {
			if (!inv.destroyItemByItemId(ItemTemplate.ADENA_ID, adenaCost)) {
				return false;
			}
		}
		var existingOpt = inv.byItemId(ticketItemId);
		if (existingOpt.isPresent()) {
			existingOpt.get().count(existingOpt.get().count() + count);
		} else {
			ItemTemplate tmpl = ItemTemplate.etc(ticketItemId, ticketItemId, "Mercenary Ticket", "ticket", "stackable", 1, "none", 0, true, false, true, true);
			inv.add(new ItemInstance(idFactory.nextId(), tmpl, player.objectId(), count));
		}
		return true;
	}

	/**
	 * Posiciona um bilhete de mercenario no chao/muralha do castelo e spawna o guarda no GameWorld.
	 */
	public boolean dropMercenaryTicket(PlayerCharacter player, Clan clan, int ticketItemId, int x, int y, int z, int heading) {
		return hireMercenary(player, clan, ticketItemId, x, y, z, heading);
	}

	/**
	 * Contrata e posiciona um mercenario na muralha do castelo atraves de um bilhete.
	 */
	public boolean hireMercenary(PlayerCharacter player, Clan clan, int ticketItemId, int x, int y, int z, int heading) {
		if (player == null || clan == null || !Config.SPAWN_SIEGE_GUARD) {
			return false;
		}

		int castleId = clan.castleId();
		if (castleId <= 0) {
			log.warn("{} tentou contratar mercenario sem possuir castelo", player.name());
			return false;
		}

		// Valida se o cerco esta em andamento
		if (siegeService != null) {
			var siegeOpt = siegeService.getSiege(castleId);
			if (siegeOpt.isPresent() && siegeOpt.get().status() == SiegeStatus.IN_PROGRESS) {
				log.warn("Nao e permitido contratar mercenarios durante a batalha de cerco");
				return false;
			}
		}

		List<MercenaryRecord> list = hiredMercs.computeIfAbsent(castleId, k -> new CopyOnWriteArrayList<>());
		if (list.size() >= getMaxMercenaries(castleId)) {
			log.warn("Castelo {} atingiu o limite maximo de mercenarios ({})", castleId, getMaxMercenaries(castleId));
			return false;
		}

		// Verifica e consome o bilhete do inventario
		Inventory inv = player.inventory();
		if (inv != null) {
			var ticketOpt = inv.byItemId(ticketItemId);
			if (ticketOpt.isEmpty() || ticketOpt.get().count() < 1) {
				log.warn("{} nao possui o bilhete de mercenario {}", player.name(), ticketItemId);
				return false;
			}
			if (inventoryService != null) {
				if (inventoryService.consumeItem(inv, ticketItemId, 1, "HireMercenary") == null) {
					return false;
				}
			} else {
				ItemInstance ticketItem = ticketOpt.get();
				if (ticketItem.count() > 1) {
					ticketItem.count(ticketItem.count() - 1);
				} else {
					inv.remove(ticketItem);
				}
			}
		}

		int npcId = resolveNpcIdFromTicket(ticketItemId);
		int id = idCounter.incrementAndGet();
		MercenaryRecord record = new MercenaryRecord(id, castleId, npcId, x, y, z, heading, 0, true);
		list.add(record);

		spawnMercenaryNpc(record);
		saveMercenary(record);
		log.info("Mercenario {} (NPC {}) contratado para o Castelo {} em ({}, {}, {}) por {}",
				id, npcId, castleId, x, y, z, player.name());
		return true;
	}

	private void spawnMercenaryNpc(MercenaryRecord record) {
		if (gameWorld == null) {
			return;
		}
		NpcTemplate tpl = null;
		if (npcTemplates != null) {
			tpl = npcTemplates.get(record.npcId()).orElse(null);
		}
		if (tpl == null) {
			tpl = new NpcTemplate(
					record.npcId(), record.npcId(), "Siege Mercenary", false, "Castle Guard",
					false, 12.0, 24.0, 75, "male", "L2SiegeGuard", 40, 15000, 3000, 1200,
					600, 500, 500, 278, 333, 0, 0, 0, 80, 120, 500, false
			);
		}
		NpcInstance npc = new NpcInstance(idFactory.nextId(), tpl, record.x(), record.y(), record.z(), record.heading());
		gameWorld.addNpc(npc);
		spawnedMercNpcs.computeIfAbsent(record.castleId(), k -> new CopyOnWriteArrayList<>()).add(npc);
	}

	/**
	 * Remove todos os mercenarios contratados quando o castelo e conquistado ou cerco termina.
	 */
	public void clearCastleMercenaries(int castleId) {
		hiredMercs.remove(castleId);
		List<NpcInstance> npcs = spawnedMercNpcs.remove(castleId);
		if (npcs != null && gameWorld != null) {
			for (NpcInstance npc : npcs) {
				gameWorld.removeNpc(npc);
			}
		}
		if (jdbc != null) {
			try {
				jdbc.sql("DELETE FROM castle_siege_guards WHERE castleId = :cid AND isHired = 1")
						.param("cid", castleId)
						.update();
			} catch (Exception ex) {
				log.warn("Erro ao limpar mercenarios do castelo {}: {}", castleId, ex.getMessage());
			}
		}
		log.info("Mercenarios contratados do Castelo {} foram dispensados", castleId);
	}

	/**
	 * Converte o ID do item de bilhete de mercenario para o respectivo template de NPC de guarda (#391).
	 */
	public int resolveNpcIdFromTicket(int ticketItemId) {
		if (ticketItemId >= 3960 && ticketItemId <= 3972) {
			return 35010 + (ticketItemId - 3960); // Gludio
		}
		if (ticketItemId >= 3973 && ticketItemId <= 3985) {
			return 35020 + (ticketItemId - 3973); // Dion
		}
		if (ticketItemId >= 3986 && ticketItemId <= 3998) {
			return 35030 + (ticketItemId - 3986); // Giran
		}
		if (ticketItemId >= 3999 && ticketItemId <= 4011) {
			return 35040 + (ticketItemId - 3999); // Oren
		}
		if (ticketItemId >= 4012 && ticketItemId <= 4026) {
			return 35050 + (ticketItemId - 4012); // Aden
		}
		if (ticketItemId >= 5205 && ticketItemId <= 5217) {
			return 35065 + (ticketItemId - 5205); // Innadril
		}
		if (ticketItemId >= 6019 && ticketItemId <= 6032) {
			return 35080 + (ticketItemId - 6019); // Goddard
		}
		if (ticketItemId >= 6301 && ticketItemId <= 6312) {
			return 35095 + (ticketItemId - 6301); // Rune
		}
		if (ticketItemId >= 7998 && ticketItemId <= 8029) {
			return 35500 + (ticketItemId - 7998); // Schuttgart / Dawn / Dusk Mercenaries
		}
		return 35010; // Fallback padrao: Siege Guard
	}

	private void saveMercenary(MercenaryRecord record) {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("""
					INSERT INTO castle_siege_guards (castleId, npcId, x, y, z, heading, respawnDelay, isHired)
					VALUES (:castleId, :npcId, :x, :y, :z, :heading, :respawnDelay, 1)
					""")
					.param("castleId", record.castleId())
					.param("npcId", record.npcId())
					.param("x", record.x())
					.param("y", record.y())
					.param("z", record.z())
					.param("heading", record.heading())
					.param("respawnDelay", record.respawnDelay())
					.update();
		} catch (Exception ex) {
			log.warn("Erro ao persistir guarda mercenario no banco: {}", ex.getMessage());
		}
	}

	private void loadHiredMercenaries() {
		if (jdbc == null) {
			return;
		}
		try {
			List<Map<String, Object>> rows = jdbc.sql("""
					SELECT id, castleId, npcId, x, y, z, heading, respawnDelay, isHired
					FROM castle_siege_guards
					WHERE isHired = 1
					""").query().listOfRows();

			for (var row : rows) {
				int id = ((Number) row.get("id")).intValue();
				int castleId = ((Number) row.get("castleId")).intValue();
				int npcId = ((Number) row.get("npcId")).intValue();
				int x = ((Number) row.get("x")).intValue();
				int y = ((Number) row.get("y")).intValue();
				int z = ((Number) row.get("z")).intValue();
				int heading = row.get("heading") != null ? ((Number) row.get("heading")).intValue() : 0;
				int respawnDelay = row.get("respawnDelay") != null ? ((Number) row.get("respawnDelay")).intValue() : 0;
				boolean isHired = ((Number) row.get("isHired")).intValue() == 1;

				MercenaryRecord rec = new MercenaryRecord(id, castleId, npcId, x, y, z, heading, respawnDelay, isHired);
				hiredMercs.computeIfAbsent(castleId, k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(rec);
				idCounter.updateAndGet(curr -> Math.max(curr, id));
			}
			log.info("MercenaryService: carregados mercenarios contratados do banco de dados");
		} catch (Exception ex) {
			log.warn("Nao foi possivel carregar mercenarios de castle_siege_guards: {}", ex.getMessage());
		}
	}
}
