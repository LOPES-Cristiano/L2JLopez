package com.lopez.l2j.game.fortress.siege;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.castle.siege.SiegeStatus;
import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.fortress.FortressRecord;
import com.lopez.l2j.game.fortress.FortressService;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.npc.NpcTemplateTable;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Servico do motor de cerco a fortalezas (FortSiegeManager de Lineage II Interlude).
 * Gerencia inscricao com taxa de adena, desligamento de reatores, derrota de comandantes e captura da bandeira de combate.
 */
@Service
public class FortressSiegeService {

	private static final Logger log = LoggerFactory.getLogger(FortressSiegeService.class);

	public static final int REGISTRATION_FEE_ADENA = 250_000;
	public static final int MIN_CLAN_LEVEL = 4;
	public static final long SIEGE_CYCLE_MILLIS = 7 * 24 * 60 * 60 * 1000L; // 7 dias
	public static final int FLAGPOLE_NPC_ID = 35063;
	public static final int SKILL_RAISE_FLAG = 2011;

	public record SiegeSpawnConfig(int npcId, int x, int y, int z, int heading) {}
	public record CommanderSpawn(int fortId, int x, int y, int z, int heading, int npcId, int index) {}
	public record CombatFlagSpawn(int fortId, int x, int y, int z, int itemId) {}

	private final Map<Integer, FortressSiegeRecord> sieges = new ConcurrentHashMap<>();
	private final Map<Integer, List<SiegeSpawnConfig>> configuredSpawns = new ConcurrentHashMap<>();
	private final Map<Integer, List<CommanderSpawn>> commanderSpawnsByFort = new ConcurrentHashMap<>();
	private final Map<Integer, List<CombatFlagSpawn>> combatFlagSpawnsByFort = new ConcurrentHashMap<>();
	private final Map<Integer, List<NpcInstance>> activeSiegeNpcs = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> combatFlagCarrierByFort = new ConcurrentHashMap<>(); // fortId -> playerObjId

	private final FortressService fortressService;
	private final JdbcClient jdbc;
	private InventoryService inventoryService;
	private GameWorld world;
	private NpcTemplateTable npcTemplates;
	private ObjectIdFactory objectIds;
	private final ScheduledExecutorService fortSiegeScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = new Thread(r, "FortressSiegeService-Scheduler");
		t.setDaemon(true);
		return t;
	});

	public FortressSiegeService(FortressService fortressService,
								JdbcClient jdbc) {
		this(fortressService, jdbc, null, null, null, null);
	}

	@Autowired
	public FortressSiegeService(FortressService fortressService,
								@Autowired(required = false) JdbcClient jdbc,
								@Autowired(required = false) InventoryService inventoryService,
								@Autowired(required = false) GameWorld world,
								@Autowired(required = false) NpcTemplateTable npcTemplates,
								@Autowired(required = false) ObjectIdFactory objectIds) {
		this.fortressService = fortressService;
		this.jdbc = jdbc;
		this.inventoryService = inventoryService;
		this.world = world;
		this.npcTemplates = npcTemplates;
		this.objectIds = objectIds != null ? objectIds : ObjectIdFactory.sequential(0x58000000);
		loadCommandersAndFlags();
		initSieges();
		loadFortSiegeClans();
		this.fortSiegeScheduler.scheduleAtFixedRate(this::checkScheduledSieges, 60L, 60L, TimeUnit.SECONDS);
	}

	@PreDestroy
	public void shutdown() {
		fortSiegeScheduler.shutdownNow();
	}

	/**
	 * Verifica periodicamente cercos de fortaleza em andamento (expiracao de tempo) ou agendados.
	 */
	public void checkScheduledSieges() {
		long now = System.currentTimeMillis();
		for (FortressSiegeRecord siege : sieges.values()) {
			try {
				if (siege.status() == SiegeStatus.IN_PROGRESS) {
					if (siege.siegeEndDate() > 0L && now >= siege.siegeEndDate()) {
						endSiege(siege.fortId(), siege.victoriousClanId());
					}
				} else if (siege.status() == SiegeStatus.REGISTRATION
						&& !siege.attackerClans().isEmpty()
						&& siege.siegeDate() > 0L
						&& now >= siege.siegeDate()) {
					startSiege(siege.fortId());
				}
			} catch (Exception ex) {
				log.warn("Erro ao verificar ciclo de cerco da fortaleza {}: {}", siege.fortId(), ex.getMessage());
			}
		}
	}

	private void initSieges() {
		if (fortressService == null) {
			return;
		}
		for (FortressRecord fort : fortressService.allFortresses()) {
			long sDate = fort.siegeDate() > 0 ? fort.siegeDate() : System.currentTimeMillis() + SIEGE_CYCLE_MILLIS;
			sieges.put(fort.id(), new FortressSiegeRecord(fort.id(), sDate));
		}
	}

	public Optional<FortressSiegeRecord> getSiege(int fortId) {
		return Optional.ofNullable(sieges.get(fortId));
	}

	public Collection<FortressSiegeRecord> allSieges() {
		return Collections.unmodifiableCollection(sieges.values());
	}

	public static int getSiegeLengthMinutes() {
		return Config.FORT_SIEGE_LENGTH > 0 ? Config.FORT_SIEGE_LENGTH : 60;
	}

	public static int getCountdownLengthMinutes() {
		return Math.max(0, Config.FORT_COUNT_DOWN_LENGTH);
	}

	public static int getSuspiciousMerchantRespawnDelayMinutes() {
		return Math.max(0, Config.SUSPICIOUS_MERCHANT_RESPAWN_DELAY);
	}

	public static int getMaxFlagsPerClan() {
		return Math.max(1, Config.FORT_MAX_FLAGS);
	}

	public static int getMinClanLevel() {
		return Config.FORT_SIEGE_CLAN_MIN_LEVEL > 0 ? Config.FORT_SIEGE_CLAN_MIN_LEVEL : MIN_CLAN_LEVEL;
	}

	public static int getAttackerMaxClans() {
		return Math.max(1, Config.FORT_ATTACKER_MAX_CLANS);
	}

	public static int getCombatFlagItemId() {
		return Config.COMBAT_FLAG_ID > 0 ? Config.COMBAT_FLAG_ID : 9819;
	}

	public List<CommanderSpawn> getCommanderSpawns(int fortId) {
		return Collections.unmodifiableList(commanderSpawnsByFort.getOrDefault(fortId, List.of()));
	}

	public List<CombatFlagSpawn> getCombatFlagSpawns(int fortId) {
		return Collections.unmodifiableList(combatFlagSpawnsByFort.getOrDefault(fortId, List.of()));
	}

	/**
	 * Configura spawns de comandantes ou bandeiras para uma fortaleza especifica.
	 */
	public void addSiegeSpawnConfig(int fortId, int npcId, int x, int y, int z, int heading) {
		configuredSpawns.computeIfAbsent(fortId, k -> new CopyOnWriteArrayList<>())
				.add(new SiegeSpawnConfig(npcId, x, y, z, heading));
	}

	public List<NpcInstance> getActiveSiegeNpcs(int fortId) {
		return activeSiegeNpcs.getOrDefault(fortId, Collections.emptyList());
	}

	/**
	 * Inscreve um cla no cerco a fortaleza mediante pagamento de taxa.
	 */
	public boolean registerAttacker(int fortId, PlayerCharacter player, Clan clan) {
		if (player == null || clan == null) {
			return false;
		}

		FortressSiegeRecord siege = sieges.get(fortId);
		if (siege == null || siege.status() != SiegeStatus.REGISTRATION) {
			return false;
		}

		int minLevel = getMinClanLevel();
		if (clan.level() < minLevel) {
			log.warn("Cla {} nao possui nivel minimo ({}) para disputar fortaleza", clan.name(), minLevel);
			return false;
		}

		int maxClans = getAttackerMaxClans();
		if (maxClans > 0 && siege.attackerClans().size() >= maxClans && !siege.attackerClans().contains(clan.clanId())) {
			log.warn("Fortaleza {} atingiu o limite maximo de clas atacantes ({})", fortId, maxClans);
			return false;
		}

		// Nao pode disputar a propria fortaleza
		FortressRecord fort = fortressService.getFortressById(fortId).orElse(null);
		if (fort != null && fort.ownerClanId() == clan.clanId()) {
			return false;
		}

		// Desconta a taxa de inscricao de Adena (com persistencia via InventoryService se disponivel)
		Inventory inv = player.inventory();
		if (inv != null) {
			var adenaOpt = inv.byItemId(ItemTemplate.ADENA_ID);
			if (adenaOpt.isEmpty() || adenaOpt.get().count() < REGISTRATION_FEE_ADENA) {
				log.warn("{} nao possui a taxa de {} adenas para registrar cerco de fortaleza",
						player.name(), REGISTRATION_FEE_ADENA);
				return false;
			}
			if (inventoryService != null) {
				if (inventoryService.consumeItem(inv, ItemTemplate.ADENA_ID, REGISTRATION_FEE_ADENA, "FortSiegeReg") == null) {
					return false;
				}
			} else {
				int currentCount = adenaOpt.get().count();
				if (currentCount == REGISTRATION_FEE_ADENA) {
					inv.remove(adenaOpt.get());
				} else {
					adenaOpt.get().count(currentCount - REGISTRATION_FEE_ADENA);
				}
			}
		}

		siege.attackerClans().add(clan.clanId());
		saveSiegeClan(fortId, clan.clanId());
		log.info("Cla {} ({}) registrado no cerco da Fortaleza {} por {}",
				clan.name(), clan.clanId(), fortId, player.name());
		return true;
	}

	/**
	 * Inicia a batalha de cerco da fortaleza.
	 */
	public boolean startSiege(int fortId) {
		FortressSiegeRecord siege = sieges.get(fortId);
		if (siege == null || siege.status() == SiegeStatus.IN_PROGRESS) {
			return false;
		}

		long now = System.currentTimeMillis();
		siege.status(SiegeStatus.IN_PROGRESS);
		siege.siegeEndDate(now + (getSiegeLengthMinutes() * 60_000L));
		siege.reactorsDisabled(0);
		siege.commandersDefeated(0);
		siege.flagRaised(false);

		spawnSiegeCommandersAndFlags(fortId);

		if (world != null) {
			String fortName = fortressService.getFortressById(fortId).map(FortressRecord::name).orElse("Fortress #" + fortId);
			world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Fortress Siege",
					"O cerco da Fortaleza " + fortName + " comecou!"));
		}
		log.info("Cerco da Fortaleza {} iniciado!", fortId);
		return true;
	}

	/**
	 * Desativa um reator na sala de controle (Power Control Room).
	 */
	public void disableReactor(int fortId) {
		FortressSiegeRecord siege = sieges.get(fortId);
		if (siege != null && siege.status() == SiegeStatus.IN_PROGRESS) {
			siege.reactorsDisabled(siege.reactorsDisabled() + 1);
			log.info("Fortaleza {}: Reator desativado ({}/{})",
					fortId, siege.reactorsDisabled(), FortressSiegeRecord.TOTAL_REACTORS);
		}
	}

	/**
	 * Derrota um dos comandantes da fortaleza (Subunit Commander).
	 * Quando todos os comandantes sao derrotados, spawna as Bandeiras de Combate (Combat Flags - Item ID 9819) (#395).
	 */
	public void defeatCommander(int fortId) {
		FortressSiegeRecord siege = sieges.get(fortId);
		if (siege != null && siege.status() == SiegeStatus.IN_PROGRESS) {
			siege.commandersDefeated(siege.commandersDefeated() + 1);
			log.info("Fortaleza {}: Comandante derrotado ({}/{})",
					fortId, siege.commandersDefeated(), FortressSiegeRecord.TOTAL_COMMANDERS);
			if (siege.commandersDefeated() >= FortressSiegeRecord.TOTAL_COMMANDERS) {
				spawnCombatFlags(fortId);
			}
		}
	}

	/**
	 * Equipa uma Bandeira de Combate (Combat Flag 9819) no jogador atacante, aplicando penalidade
	 * de velocidade e concedendo a habilidade de captura no mastro (Flagpole 35063) (#395).
	 */
	public boolean pickupCombatFlag(int fortId, PlayerCharacter player) {
		if (player == null || player.clanId() <= 0) {
			return false;
		}
		FortressSiegeRecord siege = sieges.get(fortId);
		if (siege == null || siege.status() != SiegeStatus.IN_PROGRESS || !siege.attackerClans().contains(player.clanId())) {
			return false;
		}
		if (siege.commandersDefeated() < FortressSiegeRecord.TOTAL_COMMANDERS) {
			return false;
		}
		int flagItemId = getCombatFlagItemId();
		if (player.inventory() != null && player.inventory().getItemCount(flagItemId) <= 0) {
			player.inventory().addItem(flagItemId, 1);
		}
		player.skills().put(SKILL_RAISE_FLAG, 1);
		combatFlagCarrierByFort.put(fortId, player.objectId());
		return true;
	}

	/**
	 * Remove a Bandeira de Combate (9819) do portador (por morte, desconexao ou fim do cerco) (#395).
	 */
	public void dropCombatFlag(int fortId, PlayerCharacter player) {
		combatFlagCarrierByFort.remove(fortId);
		if (player != null) {
			player.skills().remove(SKILL_RAISE_FLAG);
			if (player.inventory() != null) {
				int flagItemId = getCombatFlagItemId();
				int count = player.inventory().getItemCount(flagItemId);
				if (count > 0) {
					player.inventory().destroyItemByItemId(flagItemId, count);
				}
			}
		}
	}

	private void spawnCombatFlags(int fortId) {
		if (world == null) {
			return;
		}
		String fortName = fortressService.getFortressById(fortId).map(FortressRecord::name).orElse("Fortress #" + fortId);
		world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Fortress Siege",
				"All barracks of " + fortName + " have been captured! Combat Flags have spawned!"));
	}

	/**
	 * Ergue a bandeira de combate no mastro da fortaleza apos neutralizar as defesas.
	 */
	public boolean raiseCombatFlag(int fortId, int clanId) {
		FortressSiegeRecord siege = sieges.get(fortId);
		if (siege == null || siege.status() != SiegeStatus.IN_PROGRESS) {
			return false;
		}

		if (!siege.canRaiseFlag()) {
			log.warn("Nao e possivel erguer a bandeira: defesas da fortaleza ainda ativas (Reatores: {}/{}, Comandantes: {}/{})",
					siege.reactorsDisabled(), FortressSiegeRecord.TOTAL_REACTORS,
					siege.commandersDefeated(), FortressSiegeRecord.TOTAL_COMMANDERS);
			return false;
		}

		siege.flagRaised(true);
		siege.victoriousClanId(clanId);
		endSiege(fortId, clanId);
		log.info("Cla {} ergueu a bandeira de combate e conquistou a Fortaleza {}!", clanId, fortId);
		return true;
	}

	/**
	 * Finaliza o cerco e concede a fortaleza ao novo cla.
	 */
	public boolean endSiege(int fortId, int winnerClanId) {
		FortressSiegeRecord siege = sieges.get(fortId);
		if (siege == null) {
			return false;
		}

		siege.status(SiegeStatus.FINISHED);
		despawnSiegeCommandersAndFlags(fortId);

		if (winnerClanId > 0) {
			fortressService.setOwner(fortId, winnerClanId);
			fortressService.setFortState(fortId, FortressRecord.STATE_NONE); // Permite escolher contrato ou independencia
			if (Config.FORT_REWARD_ID > 0 && Config.FORT_REWARD_COUNT > 0) {
				log.info("Cla vencedor {} elegivel para recompensa de fortaleza ({}x item {})",
						winnerClanId, Config.FORT_REWARD_COUNT, Config.FORT_REWARD_ID);
			}
		}

		if (world != null) {
			String fortName = fortressService.getFortressById(fortId).map(FortressRecord::name).orElse("Fortress #" + fortId);
			String msg = winnerClanId > 0
					? "O cerco da Fortaleza " + fortName + " terminou! O Cla ID " + winnerClanId + " conquistou a fortaleza!"
					: "O cerco da Fortaleza " + fortName + " terminou sem novo conquistador!";
			world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Fortress Siege", msg));
		}

		// Reagenda cerco para 7 dias no futuro
		long nextSiege = System.currentTimeMillis() + SIEGE_CYCLE_MILLIS;
		siege.siegeDate(nextSiege);
		fortressService.getFortressById(fortId).ifPresent(f -> {
			f.siegeDate(nextSiege);
			fortressService.setFortState(fortId, f.state());
			if (jdbc != null) {
				try {
					jdbc.sql("UPDATE fort SET siegeDate = :sDate WHERE id = :id")
							.param("sDate", nextSiege)
							.param("id", fortId)
							.update();
				} catch (Exception ex) {
					log.warn("Erro ao persistir siegeDate da fortaleza {}: {}", fortId, ex.getMessage());
				}
			}
		});

		clearSiegeClans(fortId);
		log.info("Cerco da Fortaleza {} encerrado! Vencedor: Cla {}", fortId, winnerClanId);
		return true;
	}

	/**
	 * Carrega todos os spawns de comandantes e bandeiras de combate das 21 fortalezas a partir do Config.
	 */
	public final void loadCommandersAndFlags() {
		commanderSpawnsByFort.clear();
		combatFlagSpawnsByFort.clear();

		// 101: Shanty Fortress
		registerFortSpawns(101,
				List.of(Config.SHANTY_COMMANDER_1, Config.SHANTY_COMMANDER_2, Config.SHANTY_COMMANDER_3),
				List.of(Config.SHANTY_FLAG_1, Config.SHANTY_FLAG_2, Config.SHANTY_FLAG_3));

		// 102: Southern Fortress
		registerFortSpawns(102,
				List.of(Config.SOUTHERN_COMMANDER_1, Config.SOUTHERN_COMMANDER_2, Config.SOUTHERN_COMMANDER_3, Config.SOUTHERN_COMMANDER_4),
				List.of(Config.SOUTHERN_FLAG_1, Config.SOUTHERN_FLAG_2, Config.SOUTHERN_FLAG_3));

		// 103: Hive Fortress
		registerFortSpawns(103,
				List.of(Config.HIVE_COMMANDER_1, Config.HIVE_COMMANDER_2, Config.HIVE_COMMANDER_3),
				List.of(Config.HIVE_FLAG_1, Config.HIVE_FLAG_2, Config.HIVE_FLAG_3));

		// 104: Valley Fortress
		registerFortSpawns(104,
				List.of(Config.VALLEY_COMMANDER_1, Config.VALLEY_COMMANDER_2, Config.VALLEY_COMMANDER_3, Config.VALLEY_COMMANDER_4),
				List.of(Config.VALLEY_FLAG_1, Config.VALLEY_FLAG_2, Config.VALLEY_FLAG_3));

		// 105: Ivory Fortress
		registerFortSpawns(105,
				List.of(Config.IVORY_COMMANDER_1, Config.IVORY_COMMANDER_2, Config.IVORY_COMMANDER_3),
				List.of(Config.IVORY_FLAG_1, Config.IVORY_FLAG_2, Config.IVORY_FLAG_3));

		// 106: Narsell Fortress
		registerFortSpawns(106,
				List.of(Config.NARSELL_COMMANDER_1, Config.NARSELL_COMMANDER_2, Config.NARSELL_COMMANDER_3),
				List.of(Config.NARSELL_FLAG_1, Config.NARSELL_FLAG_2, Config.NARSELL_FLAG_3));

		// 107: Bayou Fortress
		registerFortSpawns(107,
				List.of(Config.BAYOU_COMMANDER_1, Config.BAYOU_COMMANDER_2, Config.BAYOU_COMMANDER_3, Config.BAYOU_COMMANDER_4),
				List.of(Config.BAYOU_FLAG_1, Config.BAYOU_FLAG_2, Config.BAYOU_FLAG_3));

		// 108: WhiteSands Fortress
		registerFortSpawns(108,
				List.of(Config.WHITE_SANDS_COMMANDER_1, Config.WHITE_SANDS_COMMANDER_2, Config.WHITE_SANDS_COMMANDER_3),
				List.of(Config.WHITE_SANDS_FLAG_1, Config.WHITE_SANDS_FLAG_2, Config.WHITE_SANDS_FLAG_3));

		// 109: Borderland Fortress
		registerFortSpawns(109,
				List.of(Config.BORDERLAND_COMMANDER_1, Config.BORDERLAND_COMMANDER_2, Config.BORDERLAND_COMMANDER_3, Config.BORDERLAND_COMMANDER_4),
				List.of(Config.BORDERLAND_FLAG_1, Config.BORDERLAND_FLAG_2, Config.BORDERLAND_FLAG_3));

		// 110: Swamp Fortress
		registerFortSpawns(110,
				List.of(Config.SWAMP_COMMANDER_1, Config.SWAMP_COMMANDER_2, Config.SWAMP_COMMANDER_3, Config.SWAMP_COMMANDER_4),
				List.of(Config.SWAMP_FLAG_1, Config.SWAMP_FLAG_2, Config.SWAMP_FLAG_3));

		// 111: Archaic Fortress
		registerFortSpawns(111,
				List.of(Config.ARCHAIC_COMMANDER_1, Config.ARCHAIC_COMMANDER_2, Config.ARCHAIC_COMMANDER_3),
				List.of(Config.ARCHAIC_FLAG_1, Config.ARCHAIC_FLAG_2, Config.ARCHAIC_FLAG_3));

		// 112: Floran Fortress
		registerFortSpawns(112,
				List.of(Config.FLORAN_COMMANDER_1, Config.FLORAN_COMMANDER_2, Config.FLORAN_COMMANDER_3, Config.FLORAN_COMMANDER_4),
				List.of(Config.FLORAN_FLAG_1, Config.FLORAN_FLAG_2, Config.FLORAN_FLAG_3));

		// 113: CloudMountain Fortress
		registerFortSpawns(113,
				List.of(Config.CLOUD_MOUNTAIN_COMMANDER_1, Config.CLOUD_MOUNTAIN_COMMANDER_2, Config.CLOUD_MOUNTAIN_COMMANDER_3, Config.CLOUD_MOUNTAIN_COMMANDER_4),
				List.of(Config.CLOUD_MOUNTAIN_FLAG_1, Config.CLOUD_MOUNTAIN_FLAG_2, Config.CLOUD_MOUNTAIN_FLAG_3));

		// 114: Tanor Fortress
		registerFortSpawns(114,
				List.of(Config.TANOR_COMMANDER_1, Config.TANOR_COMMANDER_2, Config.TANOR_COMMANDER_3),
				List.of(Config.TANOR_FLAG_1, Config.TANOR_FLAG_2, Config.TANOR_FLAG_3));

		// 115: Dragonspine Fortress
		registerFortSpawns(115,
				List.of(Config.DRAGONSPINE_COMMANDER_1, Config.DRAGONSPINE_COMMANDER_2, Config.DRAGONSPINE_COMMANDER_3),
				List.of(Config.DRAGONSPINE_FLAG_1, Config.DRAGONSPINE_FLAG_2, Config.DRAGONSPINE_FLAG_3));

		// 116: Antharas Fortress
		registerFortSpawns(116,
				List.of(Config.ANTHARAS_COMMANDER_1, Config.ANTHARAS_COMMANDER_2, Config.ANTHARAS_COMMANDER_3, Config.ANTHARAS_COMMANDER_4),
				List.of(Config.ANTHARAS_FLAG_1, Config.ANTHARAS_FLAG_2, Config.ANTHARAS_FLAG_3));

		// 117: Western Fortress
		registerFortSpawns(117,
				List.of(Config.WESTERN_COMMANDER_1, Config.WESTERN_COMMANDER_2, Config.WESTERN_COMMANDER_3, Config.WESTERN_COMMANDER_4),
				List.of(Config.WESTERN_FLAG_1, Config.WESTERN_FLAG_2, Config.WESTERN_FLAG_3));

		// 118: Hunters Fortress
		registerFortSpawns(118,
				List.of(Config.HUNTERS_COMMANDER_1, Config.HUNTERS_COMMANDER_2, Config.HUNTERS_COMMANDER_3, Config.HUNTERS_COMMANDER_4),
				List.of(Config.HUNTERS_FLAG_1, Config.HUNTERS_FLAG_2, Config.HUNTERS_FLAG_3));

		// 119: Aaru Fortress
		registerFortSpawns(119,
				List.of(Config.AARU_COMMANDER_1, Config.AARU_COMMANDER_2, Config.AARU_COMMANDER_3),
				List.of(Config.AARU_FLAG_1, Config.AARU_FLAG_2, Config.AARU_FLAG_3));

		// 120: Demon Fortress
		registerFortSpawns(120,
				List.of(Config.DEMON_COMMANDER_1, Config.DEMON_COMMANDER_2, Config.DEMON_COMMANDER_3),
				List.of(Config.DEMON_FLAG_1, Config.DEMON_FLAG_2, Config.DEMON_FLAG_3));

		// 121: Monastic Fortress
		registerFortSpawns(121,
				List.of(Config.MONASTIC_COMMANDER_1, Config.MONASTIC_COMMANDER_2, Config.MONASTIC_COMMANDER_3),
				List.of(Config.MONASTIC_FLAG_1, Config.MONASTIC_FLAG_2, Config.MONASTIC_FLAG_3));
	}

	private void registerFortSpawns(int fortId, List<String> commanderDefs, List<String> flagDefs) {
		List<CommanderSpawn> commanders = new ArrayList<>();
		for (String def : commanderDefs) {
			CommanderSpawn cs = parseCommanderSpawn(fortId, def);
			if (cs != null) {
				commanders.add(cs);
			}
		}
		commanderSpawnsByFort.put(fortId, commanders);

		List<CombatFlagSpawn> flags = new ArrayList<>();
		for (String def : flagDefs) {
			CombatFlagSpawn fs = parseCombatFlagSpawn(fortId, def);
			if (fs != null) {
				flags.add(fs);
			}
		}
		combatFlagSpawnsByFort.put(fortId, flags);
	}

	public static CommanderSpawn parseCommanderSpawn(int fortId, String definition) {
		if (definition == null || definition.isBlank()) {
			return null;
		}
		String[] parts = definition.split(",");
		if (parts.length < 5) {
			return null;
		}
		try {
			int x = Integer.parseInt(parts[0].trim());
			int y = Integer.parseInt(parts[1].trim());
			int z = Integer.parseInt(parts[2].trim());
			int heading = Integer.parseInt(parts[3].trim());
			int npcId = Integer.parseInt(parts[4].trim());
			int index = parts.length >= 6 ? Integer.parseInt(parts[5].trim()) : 1;
			return new CommanderSpawn(fortId, x, y, z, heading, npcId, index);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public static CombatFlagSpawn parseCombatFlagSpawn(int fortId, String definition) {
		if (definition == null || definition.isBlank()) {
			return null;
		}
		String[] parts = definition.split(";");
		if (parts.length < 3) {
			return null;
		}
		try {
			int x = Integer.parseInt(parts[0].trim());
			int y = Integer.parseInt(parts[1].trim());
			int z = Integer.parseInt(parts[2].trim());
			int itemId = parts.length >= 4 ? Integer.parseInt(parts[3].trim()) : getCombatFlagItemId();
			return new CombatFlagSpawn(fortId, x, y, z, itemId);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private void spawnSiegeCommandersAndFlags(int fortId) {
		if (world == null) {
			return;
		}
		List<SiegeSpawnConfig> configs = configuredSpawns.get(fortId);
		List<NpcInstance> spawned = activeSiegeNpcs.computeIfAbsent(fortId, k -> new CopyOnWriteArrayList<>());
		if (configs != null && !configs.isEmpty()) {
			for (SiegeSpawnConfig cfg : configs) {
				NpcTemplate template = (npcTemplates != null ? npcTemplates.get(cfg.npcId()).orElse(null) : null);
				if (template == null) {
					template = NpcTemplate.fallback(cfg.npcId(), "Fortress Commander #" + cfg.npcId(), "L2FortCommander");
				}
				NpcInstance npc = new NpcInstance(objectIds.nextId(), template, cfg.x(), cfg.y(), cfg.z(), cfg.heading());
				world.addNpc(npc);
				world.broadcastAround(npc.x(), npc.y(), GameWorld.VISIBILITY_RADIUS, new GameServerPacket.NpcInfo(npc));
				spawned.add(npc);
			}
			return;
		}
		List<CommanderSpawn> commanders = commanderSpawnsByFort.getOrDefault(fortId, List.of());
		for (CommanderSpawn cs : commanders) {
			NpcTemplate template = (npcTemplates != null ? npcTemplates.get(cs.npcId()).orElse(null) : null);
			if (template == null) {
				template = NpcTemplate.fallback(cs.npcId(), "Fortress Commander #" + cs.npcId(), "L2FortCommander");
			}
			NpcInstance npc = new NpcInstance(objectIds.nextId(), template, cs.x(), cs.y(), cs.z(), cs.heading());
			world.addNpc(npc);
			world.broadcastAround(npc.x(), npc.y(), GameWorld.VISIBILITY_RADIUS, new GameServerPacket.NpcInfo(npc));
			spawned.add(npc);
		}
	}

	private void despawnSiegeCommandersAndFlags(int fortId) {
		List<NpcInstance> spawned = activeSiegeNpcs.remove(fortId);
		if (spawned == null || world == null) {
			return;
		}
		for (NpcInstance npc : spawned) {
			npc.deleteMe();
			world.removeNpc(npc);
			world.broadcastAround(npc.x(), npc.y(), GameWorld.VISIBILITY_RADIUS,
					new GameServerPacket.DeleteObject(npc.objectId()));
		}
	}

	private void saveSiegeClan(int fortId, int clanId) {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("""
					REPLACE INTO fortsiege_clans (fort_id, clan_id)
					VALUES (:fid, :cid)
					""")
					.param("fid", fortId)
					.param("cid", clanId)
					.update();
		} catch (Exception ex) {
			log.warn("Erro ao salvar participante de cerco de fortaleza: {}", ex.getMessage());
		}
	}

	private void clearSiegeClans(int fortId) {
		FortressSiegeRecord siege = sieges.get(fortId);
		if (siege != null) {
			siege.attackerClans().clear();
		}
		if (jdbc != null) {
			try {
				jdbc.sql("DELETE FROM fortsiege_clans WHERE fort_id = :fid")
						.param("fid", fortId)
						.update();
			} catch (Exception ex) {
				log.warn("Erro ao limpar fortsiege_clans: {}", ex.getMessage());
			}
		}
	}

	private void loadFortSiegeClans() {
		if (jdbc == null) {
			return;
		}
		try {
			List<Map<String, Object>> rows = jdbc.sql("""
					SELECT fort_id, clan_id
					FROM fortsiege_clans
					""").query().listOfRows();

			for (var row : rows) {
				int fortId = ((Number) row.get("fort_id")).intValue();
				int clanId = ((Number) row.get("clan_id")).intValue();
				FortressSiegeRecord siege = sieges.get(fortId);
				if (siege != null) {
					siege.attackerClans().add(clanId);
				}
			}
			log.info("FortressSiegeService: participantes de cerco de fortaleza carregados");
		} catch (Exception ex) {
			log.warn("Nao foi possivel carregar fortsiege_clans: {}", ex.getMessage());
		}
	}

	/**
	 * Carrega guardas de cerco de fortalezas a partir da tabela fort_siege_guards (V77 / V1077).
	 */
	public int loadSiegeGuardsFromDb() {
		if (jdbc == null) {
			return 0;
		}
		try {
			List<Map<String, Object>> rows = jdbc.sql("SELECT fortId, npcId, x, y, z, heading FROM fort_siege_guards")
					.query().listOfRows();
			int count = 0;
			for (var r : rows) {
				int fortId = ((Number) r.get("fortId")).intValue();
				int npcId = ((Number) r.get("npcId")).intValue();
				int x = ((Number) r.get("x")).intValue();
				int y = ((Number) r.get("y")).intValue();
				int z = ((Number) r.get("z")).intValue();
				int heading = ((Number) r.get("heading")).intValue();
				addSiegeSpawnConfig(fortId, npcId, x, y, z, heading);
				count++;
			}
			log.info("FortressSiegeService: {} guardas de cerco carregados da tabela fort_siege_guards", count);
			return count;
		} catch (Exception ex) {
			log.debug("FortressSiegeService: fort_siege_guards indisponivel: {}", ex.getMessage());
			return 0;
		}
	}
}
