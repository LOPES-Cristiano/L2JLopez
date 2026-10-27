package com.lopez.l2j.game.clan.clanhall;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.InventoryService;
import java.util.Collection;
import java.util.Collections;
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
 * Servico de funcoes de Clan Hall (ClanHall.ClanHallFunction do L2JDream).
 * Gerencia regeneracao acelerada de HP/MP, recuperacao de XP ao ressuscitar,
 * teleporte privado e buffs de suporte para membros do cla.
 */
@Service
public class ClanHallFunctionService {

	private static final Logger log = LoggerFactory.getLogger(ClanHallFunctionService.class);

	public static final long DEFAULT_FUNCTION_RATE_MILLIS = 7L * 86_400_000L; // 7 dias

	public enum FunctionType {
		RESTORE_HP(ClanHallFunctionRecord.FUNC_RESTORE_HP, 1_000),
		RESTORE_MP(ClanHallFunctionRecord.FUNC_RESTORE_MP, 1_500),
		RESTORE_EXP(ClanHallFunctionRecord.FUNC_RESTORE_EXP, 2_000),
		TELEPORT(ClanHallFunctionRecord.FUNC_TELEPORT, 50_000),
		SUPPORT(ClanHallFunctionRecord.FUNC_SUPPORT, 35_000),
		DECO_CURTAINS(ClanHallFunctionRecord.FUNC_DECO_CURTAINS, 25_000),
		ITEM_CREATE(ClanHallFunctionRecord.FUNC_ITEM_CREATE, 30_000),
		DECO_FRONTPLATEFORM(ClanHallFunctionRecord.FUNC_DECO_FRONTPLATEFORM, 25_000);

		private final int typeId;
		private final int feePerLevel;

		FunctionType(int typeId, int feePerLevel) {
			this.typeId = typeId;
			this.feePerLevel = feePerLevel;
		}

		public int typeId() {
			return typeId;
		}

		public int calculateFee(int level) {
			if (level <= 0) {
				return 0;
			}
			int configuredFee = switch (this) {
				case TELEPORT -> switch (level) {
					case 1 -> Config.CLAN_HALL_TELEPORT_FUNCTION_FEE_LVL_1;
					case 2 -> Config.CLAN_HALL_TELEPORT_FUNCTION_FEE_LVL_2;
					default -> 0;
				};
				case SUPPORT -> switch (level) {
					case 1 -> Config.CLAN_HALL_SUPPORT_FEE_LVL_1;
					case 2 -> Config.CLAN_HALL_SUPPORT_FEE_LVL_2;
					case 3 -> Config.CLAN_HALL_SUPPORT_FEE_LVL_3;
					case 4 -> Config.CLAN_HALL_SUPPORT_FEE_LVL_4;
					case 5 -> Config.CLAN_HALL_SUPPORT_FEE_LVL_5;
					case 6 -> Config.CLAN_HALL_SUPPORT_FEE_LVL_6;
					case 7 -> Config.CLAN_HALL_SUPPORT_FEE_LVL_7;
					case 8 -> Config.CLAN_HALL_SUPPORT_FEE_LVL_8;
					default -> 0;
				};
				case RESTORE_MP -> switch (level) {
					case 1 -> Config.CLAN_HALL_MP_REGENERATION_FEE_LVL_1;
					case 2, 10 -> Config.CLAN_HALL_MP_REGENERATION_FEE_LVL_2;
					case 3, 15 -> Config.CLAN_HALL_MP_REGENERATION_FEE_LVL_3;
					case 4, 30 -> Config.CLAN_HALL_MP_REGENERATION_FEE_LVL_4;
					case 5, 40 -> Config.CLAN_HALL_MP_REGENERATION_FEE_LVL_5;
					default -> 0;
				};
				case RESTORE_HP -> switch (level) {
					case 1, 20 -> Config.CLAN_HALL_HP_REGENERATION_FEE_LVL_1;
					case 2, 40 -> Config.CLAN_HALL_HP_REGENERATION_FEE_LVL_2;
					case 3, 80 -> Config.CLAN_HALL_HP_REGENERATION_FEE_LVL_3;
					case 4, 100 -> Config.CLAN_HALL_HP_REGENERATION_FEE_LVL_4;
					case 5, 120 -> Config.CLAN_HALL_HP_REGENERATION_FEE_LVL_5;
					case 6, 140 -> Config.CLAN_HALL_HP_REGENERATION_FEE_LVL_6;
					case 7, 160 -> Config.CLAN_HALL_HP_REGENERATION_FEE_LVL_7;
					case 8, 180 -> Config.CLAN_HALL_HP_REGENERATION_FEE_LVL_8;
					case 9, 200 -> Config.CLAN_HALL_HP_REGENERATION_FEE_LVL_9;
					case 10, 220 -> Config.CLAN_HALL_HP_REGENERATION_FEE_LVL_10;
					case 11, 240 -> Config.CLAN_HALL_HP_REGENERATION_FEE_LVL_11;
					case 12, 260 -> Config.CLAN_HALL_HP_REGENERATION_FEE_LVL_12;
					case 13, 300 -> Config.CLAN_HALL_HP_REGENERATION_FEE_LVL_13;
					default -> 0;
				};
				case RESTORE_EXP -> switch (level) {
					case 1 -> Config.CLAN_HALL_EXP_REGENERATION_FEE_LVL_1;
					case 2, 10 -> Config.CLAN_HALL_EXP_REGENERATION_FEE_LVL_2;
					case 3, 15 -> Config.CLAN_HALL_EXP_REGENERATION_FEE_LVL_3;
					case 4, 25 -> Config.CLAN_HALL_EXP_REGENERATION_FEE_LVL_4;
					case 5, 35 -> Config.CLAN_HALL_EXP_REGENERATION_FEE_LVL_5;
					case 6, 40 -> Config.CLAN_HALL_EXP_REGENERATION_FEE_LVL_6;
					case 7, 50 -> Config.CLAN_HALL_EXP_REGENERATION_FEE_LVL_7;
					default -> 0;
				};
				case ITEM_CREATE -> switch (level) {
					case 1 -> Config.CLAN_HALL_ITEM_CREATION_FUNCTION_FEE_LVL_1;
					case 2 -> Config.CLAN_HALL_ITEM_CREATION_FUNCTION_FEE_LVL_2;
					case 3 -> Config.CLAN_HALL_ITEM_CREATION_FUNCTION_FEE_LVL_3;
					default -> 0;
				};
				case DECO_CURTAINS -> switch (level) {
					case 1 -> Config.CLAN_HALL_CURTAIN_FUNCTION_FEE_LVL_1;
					case 2 -> Config.CLAN_HALL_CURTAIN_FUNCTION_FEE_LVL_2;
					default -> 0;
				};
				case DECO_FRONTPLATEFORM -> switch (level) {
					case 1 -> Config.CLAN_HALL_FRONT_PLATFORM_FUNCTION_FEE_LVL_1;
					case 2 -> Config.CLAN_HALL_FRONT_PLATFORM_FUNCTION_FEE_LVL_2;
					default -> 0;
				};
			};
			return configuredFee > 0 ? configuredFee : Math.max(0, level) * feePerLevel;
		}

		public long getFeeRatio() {
			long ratio = switch (this) {
				case TELEPORT -> Config.CLAN_HALL_TELEPORT_FUNCTION_FEE_RATIO;
				case SUPPORT -> Config.CLAN_HALL_SUPPORT_FUNCTION_FEE_RATIO;
				case RESTORE_MP -> Config.CLAN_HALL_MP_REGENERATION_FUNCTION_FEE_RATIO;
				case RESTORE_HP -> Config.CLAN_HALL_HP_REGENERATION_FUNCTION_FEE_RATIO;
				case RESTORE_EXP -> Config.CLAN_HALL_EXP_REGENERATION_FUNCTION_FEE_RATIO;
				case ITEM_CREATE -> Config.CLAN_HALL_ITEM_CREATION_FUNCTION_FEE_RATIO;
				case DECO_CURTAINS -> Config.CLAN_HALL_CURTAIN_FUNCTION_FEE_RATIO;
				case DECO_FRONTPLATEFORM -> Config.CLAN_HALL_FRONT_PLATFORM_FUNCTION_FEE_RATIO;
			};
			return ratio > 0 ? ratio : DEFAULT_FUNCTION_RATE_MILLIS;
		}

		public static FunctionType fromId(int id) {
			for (FunctionType ft : values()) {
				if (ft.typeId == id) {
					return ft;
				}
			}
			return null;
		}
	}

	// hallId -> Map<type, ClanHallFunctionRecord>
	private final Map<Integer, Map<Integer, ClanHallFunctionRecord>> hallFunctions = new ConcurrentHashMap<>();
	private final ClanHallService clanHallService;
	private final JdbcClient jdbc;
	private InventoryService inventoryService;

	public ClanHallFunctionService(ClanHallService clanHallService, JdbcClient jdbc) {
		this(clanHallService, jdbc, null);
	}

	@Autowired
	public ClanHallFunctionService(ClanHallService clanHallService,
			@Autowired(required = false) JdbcClient jdbc,
			@Autowired(required = false) InventoryService inventoryService) {
		this.clanHallService = clanHallService;
		this.jdbc = jdbc;
		this.inventoryService = inventoryService;
		loadFunctions();
	}

	public Collection<ClanHallFunctionRecord> getFunctions(int hallId) {
		var map = hallFunctions.get(hallId);
		return map != null ? Collections.unmodifiableCollection(map.values()) : Collections.emptyList();
	}

	public Optional<ClanHallFunctionRecord> getFunction(int hallId, int type) {
		var map = hallFunctions.get(hallId);
		return map != null ? Optional.ofNullable(map.get(type)) : Optional.empty();
	}

	/**
	 * Ativa ou atualiza uma funcao de Clan Hall debitando a taxa em Adena do
	 * jogador/lider.
	 */
	public synchronized boolean activateFunction(PlayerCharacter player, int clanHallId, FunctionType type, int level) {
		if (type == null) {
			return false;
		}
		return activateFunction(player, clanHallId, type.typeId(), level, type.calculateFee(level), type.getFeeRatio());
	}

	/**
	 * Ativa ou atualiza uma funcao de Clan Hall pelo ID numerico do tipo, debitando
	 * Adena do jogador.
	 */
	public synchronized boolean activateFunction(PlayerCharacter player, int clanHallId, int type, int level) {
		FunctionType ft = FunctionType.fromId(type);
		int fee = ft != null ? ft.calculateFee(level) : Math.max(0, level * 1_000);
		long rateMillis = ft != null ? ft.getFeeRatio() : DEFAULT_FUNCTION_RATE_MILLIS;
		return activateFunction(player, clanHallId, type, level, fee, rateMillis);
	}

	/**
	 * Ativa ou atualiza uma funcao de Clan Hall validando posse do cla e debitando
	 * Adena via InventoryService.
	 */
	public synchronized boolean activateFunction(PlayerCharacter player, int clanHallId, int type, int level, int fee,
			long rateMillis) {
		if (player == null || player.clanId() <= 0 || level <= 0) {
			return false;
		}
		var hallOpt = clanHallService.getClanHall(clanHallId);
		if (hallOpt.isEmpty() || hallOpt.get().ownerId() != player.clanId()) {
			return false;
		}

		if (fee > 0) {
			Inventory inv = player.inventory();
			if (inv == null || inv.getItemCount(ItemTemplate.ADENA_ID) < fee) {
				log.warn("{} nao possui {} adenas para ativar funcao {} no ClanHall {}",
						player.name(), fee, type, clanHallId);
				return false;
			}
			if (inventoryService != null) {
				if (inventoryService.consumeItem(inv, ItemTemplate.ADENA_ID, fee, "ClanHallFunction") == null) {
					return false;
				}
			} else {
				inv.destroyItemByItemId(ItemTemplate.ADENA_ID, fee);
			}
			var gw = com.lopez.l2j.game.world.GameWorld.getInstance();
			if (gw != null) {
				var online = gw.findPlayer(player.objectId());
				if (online != null) {
					var remaining = inv.getItemByItemId(ItemTemplate.ADENA_ID);
					if (remaining != null) {
						online.sendPacket(
								com.lopez.l2j.network.game.packet.GameServerPacket.InventoryUpdate.modified(java.util.List.of(remaining)));
					} else {
						online.sendPacket(
								new com.lopez.l2j.network.game.packet.GameServerPacket.ItemList(player, false));
					}
				}
			}
		}

		return setFunction(clanHallId, type, level, fee, rateMillis);
	}

	/**
	 * Instala ou atualiza uma funcao em um Clan Hall.
	 */
	public synchronized boolean setFunction(int hallId, int type, int level, int fee, long rateMillis) {
		var hallOpt = clanHallService.getClanHall(hallId);
		if (hallOpt.isEmpty() || hallOpt.get().ownerId() <= 0) {
			return false;
		}

		long now = System.currentTimeMillis();
		long endTime = now + rateMillis;
		var record = new ClanHallFunctionRecord(hallId, type, level, fee, rateMillis, endTime);

		hallFunctions.computeIfAbsent(hallId, k -> new ConcurrentHashMap<>()).put(type, record);

		if (jdbc != null) {
			try {
				jdbc.sql("""
						REPLACE INTO clanhall_functions (hall_id, type, lvl, lease, rate, endTime)
						VALUES (:hallId, :type, :lvl, :lease, :rate, :endTime)
						""")
						.param("hallId", hallId)
						.param("type", type)
						.param("lvl", level)
						.param("lease", fee)
						.param("rate", rateMillis)
						.param("endTime", endTime)
						.update();
			} catch (Exception ex) {
				log.error("Erro ao persistir funcao {} no clanhall {}", type, hallId, ex);
				return false;
			}
		}

		log.info("Funcao {} Lvl {} instalada no ClanHall ID {} (Custo: {}, Validade: {})",
				type, level, hallId, fee, endTime);
		return true;
	}

	/**
	 * Remove uma funcao do Clan Hall.
	 */
	public synchronized boolean removeFunction(int hallId, int type) {
		var map = hallFunctions.get(hallId);
		if (map != null) {
			map.remove(type);
		}

		if (jdbc != null) {
			try {
				jdbc.sql("DELETE FROM clanhall_functions WHERE hall_id = :hallId AND type = :type")
						.param("hallId", hallId)
						.param("type", type)
						.update();
			} catch (Exception ex) {
				log.error("Erro ao deletar funcao {} do clanhall {}", type, hallId, ex);
				return false;
			}
		}

		log.info("Funcao {} removida do ClanHall ID {}", type, hallId);
		return true;
	}

	/**
	 * Verifica e renova periodicamente as funcoes de Clan Hall vencidas debitando
	 * do ClanWarehouse;
	 * caso nao haja saldo suficiente, desativa e remove a funcao do banco (#398).
	 */
	public synchronized void processExpiredFunctions(long nowMillis,
			java.util.function.IntFunction<Inventory> clanWarehouseByClanId) {
		for (var hallEntry : hallFunctions.entrySet()) {
			int hallId = hallEntry.getKey();
			var hallOpt = clanHallService.getClanHall(hallId);
			if (hallOpt.isEmpty() || hallOpt.get().ownerId() <= 0) {
				for (Integer typeId : new java.util.ArrayList<>(hallEntry.getValue().keySet())) {
					removeFunction(hallId, typeId);
				}
				continue;
			}
			int ownerClanId = hallOpt.get().ownerId();
			Inventory clanWh = clanWarehouseByClanId != null ? clanWarehouseByClanId.apply(ownerClanId) : null;
			for (var func : new java.util.ArrayList<>(hallEntry.getValue().values())) {
				if (func.endTime() <= nowMillis) {
					int fee = Math.max(0, func.fee());
					if (clanWh != null && fee > 0) {
						if (clanWh.getItemCount(ItemTemplate.ADENA_ID) >= fee) {
							clanWh.destroyItemByItemId(ItemTemplate.ADENA_ID, fee);
							setFunction(hallId, func.type(), func.level(), fee, func.rateMillis());
						} else {
							removeFunction(hallId, func.type());
						}
					} else if (clanWh == null && fee > 0) {
						removeFunction(hallId, func.type());
					} else {
						setFunction(hallId, func.type(), func.level(), fee, func.rateMillis());
					}
				}
			}
		}
	}

	/**
	 * Calcula multiplicador de regeneracao de HP dentro do Clan Hall.
	 * Exemplo: lvl 100 -> +100% (multiplicador 2.0x).
	 */
	public double calculateHpRegenBonus(PlayerCharacter player, int clanHallId) {
		if (player == null || player.clanId() <= 0 || clanHallId <= 0) {
			return 1.0;
		}
		var hallOpt = clanHallService.getClanHall(clanHallId);
		if (hallOpt.isEmpty() || hallOpt.get().ownerId() != player.clanId()) {
			return 1.0;
		}
		var funcOpt = getFunction(clanHallId, ClanHallFunctionRecord.FUNC_RESTORE_HP);
		if (funcOpt.isPresent() && funcOpt.get().endTime() > System.currentTimeMillis()) {
			return 1.0 + (funcOpt.get().level() / 100.0);
		}
		return 1.0;
	}

	/**
	 * Calcula multiplicador de regeneracao de MP dentro do Clan Hall.
	 */
	public double calculateMpRegenBonus(PlayerCharacter player, int clanHallId) {
		if (player == null || player.clanId() <= 0 || clanHallId <= 0) {
			return 1.0;
		}
		var hallOpt = clanHallService.getClanHall(clanHallId);
		if (hallOpt.isEmpty() || hallOpt.get().ownerId() != player.clanId()) {
			return 1.0;
		}
		var funcOpt = getFunction(clanHallId, ClanHallFunctionRecord.FUNC_RESTORE_MP);
		if (funcOpt.isPresent() && funcOpt.get().endTime() > System.currentTimeMillis()) {
			return 1.0 + (funcOpt.get().level() / 100.0);
		}
		return 1.0;
	}

	/**
	 * Retorna porcentagem de EXP recuperada ao ressuscitar no Clan Hall (e.g. 10%,
	 * 20%, 30%, 40%, 50%).
	 */
	public int getExpRestorePercent(PlayerCharacter player, int clanHallId) {
		if (player == null || player.clanId() <= 0 || clanHallId <= 0) {
			return 0;
		}
		var hallOpt = clanHallService.getClanHall(clanHallId);
		if (hallOpt.isEmpty() || hallOpt.get().ownerId() != player.clanId()) {
			return 0;
		}
		var funcOpt = getFunction(clanHallId, ClanHallFunctionRecord.FUNC_RESTORE_EXP);
		if (funcOpt.isPresent() && funcOpt.get().endTime() > System.currentTimeMillis()) {
			return funcOpt.get().level();
		}
		return 0;
	}

	/**
	 * Retorna lista de IDs de skills de buff de suporte conforme o nivel da funcao
	 * do Clan Hall.
	 */
	public List<Integer> getAvailableSupportBuffSkills(int supportLevel) {
		return switch (supportLevel) {
			case 1 -> List.of(4342, 4343, 4344); // Wind Walk, Shield, Might
			case 2 -> List.of(4342, 4343, 4344, 4345, 4346); // + Bless the Body, Bless the Soul
			case 3 -> List.of(4342, 4343, 4344, 4345, 4346, 4347, 4348); // + Concentration, Acumen
			case 4 -> List.of(4342, 4343, 4344, 4345, 4346, 4347, 4348, 4349, 4350); // + Berserker Spirit, Empower
			case 5, 6, 7, 8 -> List.of(4342, 4343, 4344, 4345, 4346, 4347, 4348, 4349, 4350, 4351, 4352, 4353); // Full
																												// Support
			default -> Collections.emptyList();
		};
	}

	private void loadFunctions() {
		if (jdbc == null) {
			return;
		}
		try {
			var rows = jdbc.sql("""
					SELECT hall_id, type, lvl, lease, rate, endTime
					FROM clanhall_functions
					""").query().listOfRows();

			for (var row : rows) {
				int hallId = ((Number) row.get("hall_id")).intValue();
				int type = ((Number) row.get("type")).intValue();
				int lvl = ((Number) row.get("lvl")).intValue();
				int lease = ((Number) row.get("lease")).intValue();
				long rate = ((Number) row.get("rate")).longValue();
				long endTime = ((Number) row.get("endTime")).longValue();

				var record = new ClanHallFunctionRecord(hallId, type, lvl, lease, rate, endTime);
				hallFunctions.computeIfAbsent(hallId, k -> new ConcurrentHashMap<>()).put(type, record);
			}
			log.info("ClanHallFunctionService: {} funcoes de clanhalls carregadas", rows.size());
		} catch (Exception ex) {
			log.warn("Nao foi possivel carregar funcoes de clanhalls: {}", ex.getMessage());
		}
	}
}
