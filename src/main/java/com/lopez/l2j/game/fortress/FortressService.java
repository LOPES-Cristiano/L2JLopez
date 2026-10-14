package com.lopez.l2j.game.fortress;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.castle.CastleManager;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.InventoryService;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Servico de gerenciamento das 21 fortalezas de Lineage II Interlude (FortManager do L2JDream).
 * Gerencia propriedades, contratos territoriais com castelos, declaracoes de independencia e funcoes internas.
 */
@Service
public class FortressService {

	private static final Logger log = LoggerFactory.getLogger(FortressService.class);

	// Funcoes de fortaleza
	public static final int FUNC_TELEPORT = 1;
	public static final int FUNC_HP_REGEN = 2;
	public static final int FUNC_MP_REGEN = 3;
	public static final int FUNC_EXP_RESTORE = 4;
	public static final int FUNC_SUPPORT = 5;

	public static final int BLOOD_OATH_ITEM_ID = 9910;

	// Catalogo padrao das 21 fortalezas
	private static final List<FortressRecord> DEFAULT_FORTS = List.of(
			new FortressRecord(101, "Shanty", 0L, 0L, 0, FortressRecord.TYPE_SMALL, FortressRecord.STATE_NONE, CastleManager.GLUDIO),
			new FortressRecord(102, "Southern", 0L, 0L, 0, FortressRecord.TYPE_LARGE, FortressRecord.STATE_NONE, CastleManager.DION),
			new FortressRecord(103, "Hive", 0L, 0L, 0, FortressRecord.TYPE_SMALL, FortressRecord.STATE_NONE, CastleManager.DION),
			new FortressRecord(104, "Valley", 0L, 0L, 0, FortressRecord.TYPE_LARGE, FortressRecord.STATE_NONE, CastleManager.GIRAN),
			new FortressRecord(105, "Ivory", 0L, 0L, 0, FortressRecord.TYPE_SMALL, FortressRecord.STATE_NONE, CastleManager.OREN),
			new FortressRecord(106, "Narsell", 0L, 0L, 0, FortressRecord.TYPE_SMALL, FortressRecord.STATE_NONE, CastleManager.ADEN),
			new FortressRecord(107, "Bayou", 0L, 0L, 0, FortressRecord.TYPE_LARGE, FortressRecord.STATE_NONE, CastleManager.ADEN),
			new FortressRecord(108, "WhiteSands", 0L, 0L, 0, FortressRecord.TYPE_SMALL, FortressRecord.STATE_NONE, CastleManager.INNADRIL),
			new FortressRecord(109, "Borderland", 0L, 0L, 0, FortressRecord.TYPE_LARGE, FortressRecord.STATE_NONE, CastleManager.GODDARD),
			new FortressRecord(110, "Swamp", 0L, 0L, 0, FortressRecord.TYPE_LARGE, FortressRecord.STATE_NONE, CastleManager.GODDARD),
			new FortressRecord(111, "Archaic", 0L, 0L, 0, FortressRecord.TYPE_SMALL, FortressRecord.STATE_NONE, CastleManager.SCHUTTGART),
			new FortressRecord(112, "Floran", 0L, 0L, 0, FortressRecord.TYPE_LARGE, FortressRecord.STATE_NONE, CastleManager.DION),
			new FortressRecord(113, "CloudMountain", 0L, 0L, 0, FortressRecord.TYPE_LARGE, FortressRecord.STATE_NONE, CastleManager.ADEN),
			new FortressRecord(114, "Tanor", 0L, 0L, 0, FortressRecord.TYPE_SMALL, FortressRecord.STATE_NONE, CastleManager.DION),
			new FortressRecord(115, "Dragonspine", 0L, 0L, 0, FortressRecord.TYPE_SMALL, FortressRecord.STATE_NONE, CastleManager.GIRAN),
			new FortressRecord(116, "Antharas", 0L, 0L, 0, FortressRecord.TYPE_LARGE, FortressRecord.STATE_NONE, CastleManager.GIRAN),
			new FortressRecord(117, "Western", 0L, 0L, 0, FortressRecord.TYPE_LARGE, FortressRecord.STATE_NONE, CastleManager.GLUDIO),
			new FortressRecord(118, "Hunters", 0L, 0L, 0, FortressRecord.TYPE_LARGE, FortressRecord.STATE_NONE, CastleManager.OREN),
			new FortressRecord(119, "Aaru", 0L, 0L, 0, FortressRecord.TYPE_SMALL, FortressRecord.STATE_NONE, CastleManager.GODDARD),
			new FortressRecord(120, "Demon", 0L, 0L, 0, FortressRecord.TYPE_SMALL, FortressRecord.STATE_NONE, CastleManager.RUNE),
			new FortressRecord(121, "Monastic", 0L, 0L, 0, FortressRecord.TYPE_SMALL, FortressRecord.STATE_NONE, CastleManager.RUNE)
	);

	private final Map<Integer, FortressRecord> byId = new ConcurrentHashMap<>();
	private final Map<String, FortressRecord> byName = new ConcurrentHashMap<>();
	// fortId -> type -> FortressFunctionRecord
	private final Map<Integer, Map<Integer, FortressFunctionRecord>> fortFunctions = new ConcurrentHashMap<>();

	private final JdbcClient jdbc;
	private InventoryService inventoryService;
	private final ObjectIdFactory idFactory = ObjectIdFactory.sequential(0x57000000);

	public FortressService(JdbcClient jdbc) {
		this(jdbc, null);
	}

	@Autowired
	public FortressService(@Autowired(required = false) JdbcClient jdbc,
						   @Autowired(required = false) InventoryService inventoryService) {
		this.jdbc = jdbc;
		this.inventoryService = inventoryService;
		initDefaults();
		loadFromDb();
		loadFunctions();
	}

	public int size() {
		return byId.size();
	}

	public Collection<FortressRecord> allFortresses() {
		return Collections.unmodifiableCollection(byId.values());
	}

	public Optional<FortressRecord> getFortressById(int fortId) {
		return Optional.ofNullable(byId.get(fortId));
	}

	public Optional<FortressRecord> getFortressByName(String name) {
		if (name == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(byName.get(name.toLowerCase(Locale.ROOT)));
	}

	public Optional<FortressRecord> getFortressByOwnerClan(int clanId) {
		if (clanId <= 0) {
			return Optional.empty();
		}
		return byId.values().stream().filter(f -> f.ownerClanId() == clanId).findFirst();
	}

	/**
	 * Define o cla proprietario da fortaleza.
	 */
	public void setOwner(int fortId, int clanId) {
		FortressRecord fort = byId.get(fortId);
		if (fort != null) {
			fort.ownerClanId(clanId);
			fort.lastOwnedTime(System.currentTimeMillis());
			saveFortress(fort);
			log.info("Fortaleza {} (ID {}) agora pertence ao Cla {}", fort.name(), fortId, clanId);
		}
	}

	/**
	 * Atualiza o estado territorial da fortaleza (Contracted com castelo ou Independent).
	 */
	public void setFortState(int fortId, int state) {
		FortressRecord fort = byId.get(fortId);
		if (fort != null) {
			fort.state(state);
			saveFortress(fort);
			log.info("Fortaleza {} (ID {}) estado territorial alterado para: {}", fort.name(), fortId, state);
		}
	}

	/**
	 * Retorna a taxa (fee) em Adena configurada para uma funcao de fortaleza no nivel informado.
	 */
	public static int getFunctionFee(int type, int level) {
		return switch (type) {
			case FUNC_TELEPORT -> switch (level) {
				case 1 -> Config.FORT_TELEPORT_FUNCTION_FEE_LVL_1;
				case 2 -> Config.FORT_TELEPORT_FUNCTION_FEE_LVL_2;
				default -> 0;
			};
			case FUNC_SUPPORT -> switch (level) {
				case 1 -> Config.FORT_SUPPORT_FEE_LVL_1;
				case 2 -> Config.FORT_SUPPORT_FEE_LVL_2;
				default -> 0;
			};
			case FUNC_MP_REGEN -> switch (level) {
				case 1 -> Config.FORT_MP_REGENERATION_FEE_LVL_1;
				case 2 -> Config.FORT_MP_REGENERATION_FEE_LVL_2;
				default -> 0;
			};
			case FUNC_HP_REGEN -> switch (level) {
				case 1 -> Config.FORT_HP_REGENERATION_FEE_LVL_1;
				case 2 -> Config.FORT_HP_REGENERATION_FEE_LVL_2;
				default -> 0;
			};
			case FUNC_EXP_RESTORE -> switch (level) {
				case 1 -> Config.FORT_EXP_REGENERATION_FEE_LVL_1;
				case 2 -> Config.FORT_EXP_REGENERATION_FEE_LVL_2;
				default -> 0;
			};
			default -> 0;
		};
	}

	/**
	 * Retorna o intervalo de cobranca (fee ratio em ms) configurado para uma funcao de fortaleza.
	 */
	public static long getFunctionFeeRatio(int type) {
		return switch (type) {
			case FUNC_TELEPORT -> Config.FORT_TELEPORT_FUNCTION_FEE_RATIO;
			case FUNC_SUPPORT -> Config.FORT_SUPPORT_FUNCTION_FEE_RATIO;
			case FUNC_MP_REGEN -> Config.FORT_MP_REGENERATION_FUNCTION_FEE_RATIO;
			case FUNC_HP_REGEN -> Config.FORT_HP_REGENERATION_FUNCTION_FEE_RATIO;
			case FUNC_EXP_RESTORE -> Config.FORT_EXP_REGENERATION_FUNCTION_FEE_RATIO;
			default -> 86_400_000L;
		};
	}

	/**
	 * Ativa ou aprimora uma funcao de fortaleza usando as taxas e ratios padrao do Config.
	 */
	public boolean activateFunction(PlayerCharacter player, int fortId, int type, int level) {
		int lease = getFunctionFee(type, level);
		long ratio = getFunctionFeeRatio(type);
		return activateFunction(player, fortId, type, level, lease, (int) (ratio / 1000L), ratio);
	}

	/**
	 * Ativa ou aprimora uma funcao de fortaleza cobrando a taxa em Adena do jogador via InventoryService.
	 */
	public boolean activateFunction(PlayerCharacter player, int fortId, int type, int level, int lease, int rate, long durationMillis) {
		if (player == null || player.clanId() <= 0 || level <= 0) {
			return false;
		}
		FortressRecord fort = byId.get(fortId);
		if (fort == null || fort.ownerClanId() != player.clanId()) {
			return false;
		}
		int effectiveLease = lease > 0 ? lease : getFunctionFee(type, level);
		long effectiveDuration = durationMillis > 0 ? durationMillis : getFunctionFeeRatio(type);
		if (effectiveLease > 0) {
			Inventory inv = player.inventory();
			if (inv == null || inv.getItemCount(ItemTemplate.ADENA_ID) < effectiveLease) {
				return false;
			}
			if (inventoryService != null) {
				if (inventoryService.consumeItem(inv, ItemTemplate.ADENA_ID, effectiveLease, "FortressFunction") == null) {
					return false;
				}
			} else {
				inv.destroyItemByItemId(ItemTemplate.ADENA_ID, effectiveLease);
			}
		}
		setFunction(fortId, type, level, effectiveLease, rate, effectiveDuration);
		return true;
	}

	/**
	 * Recompensa o lorde da fortaleza com Blood Oath (ou item configurado em Config.FORT_REWARD_ID / Config.FORT_REWARD_COUNT).
	 */
	public boolean rewardBloodOath(PlayerCharacter leader, int fortId) {
		int count = Config.FORT_REWARD_COUNT > 0 ? Config.FORT_REWARD_COUNT : 1;
		return rewardBloodOath(leader, fortId, count);
	}

	/**
	 * Recompensa o lorde da fortaleza com Blood Oath (9910), persistindo no banco via InventoryService.
	 */
	public boolean rewardBloodOath(PlayerCharacter leader, int fortId, int count) {
		if (leader == null || leader.inventory() == null || count <= 0) {
			return false;
		}
		FortressRecord fort = byId.get(fortId);
		if (fort == null || fort.ownerClanId() <= 0 || fort.ownerClanId() != leader.clanId()) {
			return false;
		}
		int rewardItemId = Config.FORT_REWARD_ID > 0 && count == Config.FORT_REWARD_COUNT
				? Config.FORT_REWARD_ID
				: BLOOD_OATH_ITEM_ID;
		Inventory inv = leader.inventory();
		boolean added = false;
		if (inventoryService != null) {
			var res = inventoryService.addItem(inv, rewardItemId, count, "FortressBloodOath");
			added = (res != null);
		}
		if (!added) {
			var existing = inv.byItemId(rewardItemId);
			if (existing.isPresent()) {
				existing.get().count(existing.get().count() + count);
			} else {
				ItemTemplate tmpl = ItemTemplate.etc(rewardItemId, rewardItemId, "Blood Oath", "none", "stackable", 1, "none", 0, true, true, true, true);
				inv.add(new ItemInstance(idFactory.nextId(), tmpl, leader.objectId(), count));
			}
		}
		return true;
	}

	/**
	 * Configura ou aprimora uma funcao interna da fortaleza (teleporte, regen, buffs).
	 */
	public void setFunction(int fortId, int type, int level, int lease, int rate, long durationMillis) {
		int effectiveLease = lease > 0 ? lease : getFunctionFee(type, level);
		long effectiveDuration = durationMillis > 0 ? durationMillis : getFunctionFeeRatio(type);
		long endTime = System.currentTimeMillis() + effectiveDuration;
		FortressFunctionRecord func = new FortressFunctionRecord(fortId, type, level, effectiveLease, rate, endTime);
		fortFunctions.computeIfAbsent(fortId, k -> new ConcurrentHashMap<>()).put(type, func);
		saveFunction(func);
		log.info("Funcao {} nivel {} configurada na Fortaleza {}", type, level, fortId);
	}

	public void removeFunction(int fortId, int type) {
		var map = fortFunctions.get(fortId);
		if (map != null) {
			map.remove(type);
		}
		if (jdbc != null) {
			try {
				jdbc.sql("DELETE FROM fort_functions WHERE fortId = :fid AND type = :t")
						.param("fid", fortId)
						.param("t", type)
						.update();
			} catch (Exception ex) {
				log.warn("Erro ao deletar fort_functions da fortaleza {}: {}", fortId, ex.getMessage());
			}
		}
	}

	public Optional<FortressFunctionRecord> getFunction(int fortId, int type) {
		var map = fortFunctions.get(fortId);
		if (map == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(map.get(type));
	}

	private void initDefaults() {
		for (FortressRecord fort : DEFAULT_FORTS) {
			byId.put(fort.id(), fort);
			byName.put(fort.name().toLowerCase(Locale.ROOT), fort);
		}
	}

	private void saveFortress(FortressRecord fort) {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("""
					UPDATE fort
					SET owner = :owner, siegeDate = :sDate, lastOwnedTime = :lTime, state = :state, castleId = :cid
					WHERE id = :id
					""")
					.param("owner", fort.ownerClanId())
					.param("sDate", fort.siegeDate())
					.param("lTime", fort.lastOwnedTime())
					.param("state", fort.state())
					.param("cid", fort.castleId())
					.param("id", fort.id())
					.update();
		} catch (Exception ex) {
			log.warn("Erro ao salvar fortaleza no banco: {}", ex.getMessage());
		}
	}

	private void saveFunction(FortressFunctionRecord func) {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("""
					REPLACE INTO fort_functions (fortId, type, lvl, lease, rate, endTime)
					VALUES (:fid, :t, :lvl, :lease, :rate, :eTime)
					""")
					.param("fid", func.fortId())
					.param("t", func.type())
					.param("lvl", func.level())
					.param("lease", func.lease())
					.param("rate", func.rate())
					.param("eTime", func.endTime())
					.update();
		} catch (Exception ex) {
			log.warn("Erro ao salvar fort_functions no banco: {}", ex.getMessage());
		}
	}

	private void loadFromDb() {
		if (jdbc == null) {
			return;
		}
		try {
			List<Map<String, Object>> rows = jdbc.sql("""
					SELECT id, name, siegeDate, lastOwnedTime, owner, fortType, state, castleId
					FROM fort
					""").query().listOfRows();

			for (var row : rows) {
				int id = ((Number) row.get("id")).intValue();
				String name = (String) row.get("name");
				long siegeDate = row.get("siegeDate") != null ? ((Number) row.get("siegeDate")).longValue() : 0L;
				long lastOwnedTime = row.get("lastOwnedTime") != null ? ((Number) row.get("lastOwnedTime")).longValue() : 0L;
				int owner = row.get("owner") != null ? ((Number) row.get("owner")).intValue() : 0;
				int fortType = row.get("fortType") != null ? ((Number) row.get("fortType")).intValue() : 0;
				int state = row.get("state") != null ? ((Number) row.get("state")).intValue() : 0;
				int castleId = row.get("castleId") != null ? ((Number) row.get("castleId")).intValue() : 0;

				FortressRecord rec = new FortressRecord(id, name, siegeDate, lastOwnedTime, owner, fortType, state, castleId);
				byId.put(id, rec);
				byName.put(name.toLowerCase(Locale.ROOT), rec);
			}
			log.info("FortressService: {} fortalezas sincronizadas com o banco", byId.size());
		} catch (Exception ex) {
			log.warn("Nao foi possivel carregar dados de fort: {}. Mantendo defaults.", ex.getMessage());
		}
	}

	private void loadFunctions() {
		if (jdbc == null) {
			return;
		}
		try {
			List<Map<String, Object>> rows = jdbc.sql("""
					SELECT fortId, type, lvl, lease, rate, endTime
					FROM fort_functions
					""").query().listOfRows();

			for (var row : rows) {
				Object fidObj = row.get("fortId");
				if (fidObj == null) {
					fidObj = row.get("fort_id");
				}
				int fortId = fidObj != null ? ((Number) fidObj).intValue() : 0;
				int type = ((Number) row.get("type")).intValue();
				int lvl = ((Number) row.get("lvl")).intValue();
				int lease = ((Number) row.get("lease")).intValue();
				int rate = ((Number) row.get("rate")).intValue();
				long endTime = row.get("endTime") != null ? ((Number) row.get("endTime")).longValue() : 0L;

				FortressFunctionRecord func = new FortressFunctionRecord(fortId, type, lvl, lease, rate, endTime);
				fortFunctions.computeIfAbsent(fortId, k -> new ConcurrentHashMap<>()).put(type, func);
			}
			log.info("FortressService: funcoes de fortalezas carregadas do banco");
		} catch (Exception ex) {
			log.warn("Nao foi possivel carregar dados de fort_functions: {}", ex.getMessage());
		}
	}
}
