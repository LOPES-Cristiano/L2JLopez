package com.lopez.l2j.game.manor;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.castle.Castle;
import com.lopez.l2j.game.castle.CastleManager;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Gerenciador do Sistema de Manor de Lineage II Interlude (#274, #507).
 * Carrega seeds.xml, agenda ciclos diarios de Refresh (20:00) e Manutencao/Aprovacao (06:00 AM),
 * integra com a tesouraria dos castelos e persiste producao de sementes e compra de colheitas.
 */
@Service
public class CastleManorManager {

	private static final Logger log = LoggerFactory.getLogger(CastleManorManager.class);

	public static final int PERIOD_CURRENT = 0;
	public static final int PERIOD_NEXT = 1;

	private static volatile CastleManorManager instance;

	private final Map<Integer, SeedTemplate> seedsById = new ConcurrentHashMap<>();
	private final Map<Integer, List<SeedTemplate>> seedsByCastle = new ConcurrentHashMap<>();

	// castleId -> (cropId -> CropProcure)
	private final Map<Integer, Map<Integer, CropProcure>> procureCurrent = new ConcurrentHashMap<>();
	private final Map<Integer, Map<Integer, CropProcure>> procureNext = new ConcurrentHashMap<>();

	// castleId -> (seedId -> SeedProduction)
	private final Map<Integer, Map<Integer, SeedProduction>> productionCurrent = new ConcurrentHashMap<>();
	private final Map<Integer, Map<Integer, SeedProduction>> productionNext = new ConcurrentHashMap<>();

	private final JdbcClient jdbc;
	private CastleManager castleManager;
	private ScheduledExecutorService scheduler;
	private volatile boolean underMaintenance = false;

	@Autowired
	public CastleManorManager(@Autowired(required = false) JdbcClient jdbc) {
		this("data/xml/world/seeds.xml", jdbc);
	}

	public CastleManorManager(String xmlPath, JdbcClient jdbc) {
		this.jdbc = jdbc;
		instance = this;
		loadSeedsXml(xmlPath);
	}

	public static CastleManorManager getInstance() {
		return instance;
	}

	@Autowired(required = false)
	public void setCastleManager(CastleManager castleManager) {
		this.castleManager = castleManager;
	}

	@PostConstruct
	public void init() {
		if (!isManorEnabled()) {
			log.info("CastleManorManager: sistema de Manor desabilitado via Config.ALLOW_MANOR");
			return;
		}
		initSchema();
		loadFromDb();
		startSchedule();
	}

	@PreDestroy
	public void shutdown() {
		if (scheduler != null) {
			scheduler.shutdownNow();
		}
		saveAllToDb();
	}

	public void initSchema() {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("""
					CREATE TABLE IF NOT EXISTS castle_manor_production (
						castle_id INT NOT NULL DEFAULT 0,
						seed_id INT NOT NULL DEFAULT 0,
						can_produce INT NOT NULL DEFAULT 0,
						start_produce INT NOT NULL DEFAULT 0,
						seed_price INT NOT NULL DEFAULT 0,
						period INT NOT NULL DEFAULT 0,
						PRIMARY KEY (castle_id, seed_id, period)
					)
					""").update();
			jdbc.sql("""
					CREATE TABLE IF NOT EXISTS castle_manor_seed (
						castle_id INT NOT NULL DEFAULT 0,
						seed_id INT NOT NULL DEFAULT 0,
						can_produce INT NOT NULL DEFAULT 0,
						start_produce INT NOT NULL DEFAULT 0,
						seed_price INT NOT NULL DEFAULT 0,
						period INT NOT NULL DEFAULT 0,
						PRIMARY KEY (castle_id, seed_id, period)
					)
					""").update();
			jdbc.sql("""
					CREATE TABLE IF NOT EXISTS castle_manor_procure (
						castle_id INT NOT NULL DEFAULT 0,
						crop_id INT NOT NULL DEFAULT 0,
						can_buy INT NOT NULL DEFAULT 0,
						start_buy INT NOT NULL DEFAULT 0,
						price INT NOT NULL DEFAULT 0,
						reward_type INT NOT NULL DEFAULT 0,
						period INT NOT NULL DEFAULT 0,
						PRIMARY KEY (castle_id, crop_id, period)
					)
					""").update();
		} catch (Exception ex) {
			log.debug("CastleManorManager: initSchema ignorado: {}", ex.getMessage());
		}
	}

	private void startSchedule() {
		if (scheduler != null && !scheduler.isShutdown()) {
			return;
		}
		scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "CastleManor-Scheduler");
			t.setDaemon(true);
			return t;
		});
		scheduleNextRefresh();
		scheduleNextApprove();
		long saveRateHours = getSavePeriodRateHours();
		scheduler.scheduleAtFixedRate(this::saveAllToDb, saveRateHours, saveRateHours, TimeUnit.HOURS);
	}

	private void scheduleNextRefresh() {
		if (scheduler == null || scheduler.isShutdown()) {
			return;
		}
		long delay = Math.max(1000L, getNextRefreshTimeMillis() - System.currentTimeMillis());
		scheduler.schedule(() -> {
			try {
				onRefreshPeriod();
			} finally {
				scheduleNextRefresh();
			}
		}, delay, TimeUnit.MILLISECONDS);
	}

	private void scheduleNextApprove() {
		if (scheduler == null || scheduler.isShutdown()) {
			return;
		}
		long delay = Math.max(1000L, getNextApproveTimeMillis() - System.currentTimeMillis());
		scheduler.schedule(() -> {
			try {
				underMaintenance = true;
				approveNextPeriod();
			} finally {
				underMaintenance = false;
				scheduleNextApprove();
			}
		}, delay, TimeUnit.MILLISECONDS);
	}

	/**
	 * Executado as 20:00 (ALT_MANOR_REFRESH_TIME): salva o estado corrente e inicia janela de recepcao/processamento.
	 */
	public synchronized void onRefreshPeriod() {
		saveAllToDb();
		log.info("CastleManorManager: ciclo de refresh das 20:00 concluido e persistido.");
	}

	public boolean isUnderMaintenance() {
		return underMaintenance;
	}

	public static boolean isManorEnabled() {
		return Config.ALLOW_MANOR;
	}

	public static int getManorDropRate() {
		return Math.max(1, Math.round(Config.RATE_DROP_MANOR));
	}

	/**
	 * Calcula a quantidade de colheitas (crops) obtida ao usar o Harvester, multiplicada por Config.RATE_DROP_MANOR.
	 */
	public static int calculateManorCropDropCount(int baseCount) {
		if (baseCount <= 0) {
			return 0;
		}
		return baseCount * getManorDropRate();
	}

	public static long getMaintenancePeriodMillis() {
		return Math.max(1, Config.ALT_MANOR_MAINTENANCE_PERIOD) * 60_000L;
	}

	public static boolean shouldSaveAllActions() {
		return Config.ALT_MANOR_SAVE_ALL_ACTIONS;
	}

	public static int getSavePeriodRateHours() {
		return Math.max(1, Config.ALT_MANOR_SAVE_PERIOD_RATE);
	}

	/**
	 * Calcula o proximo horario de refresh do Manor (Config.ALT_MANOR_REFRESH_TIME : Config.ALT_MANOR_REFRESH_MIN).
	 */
	public static long getNextRefreshTimeMillis() {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, Math.max(0, Math.min(23, Config.ALT_MANOR_REFRESH_TIME)));
		cal.set(Calendar.MINUTE, Math.max(0, Math.min(59, Config.ALT_MANOR_REFRESH_MIN)));
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
			cal.add(Calendar.DAY_OF_YEAR, 1);
		}
		return cal.getTimeInMillis();
	}

	/**
	 * Calcula o proximo horario de aprovacao do Manor (Config.ALT_MANOR_APPROVE_TIME : Config.ALT_MANOR_APPROVE_MIN).
	 */
	public static long getNextApproveTimeMillis() {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, Math.max(0, Math.min(23, Config.ALT_MANOR_APPROVE_TIME)));
		cal.set(Calendar.MINUTE, Math.max(0, Math.min(59, Config.ALT_MANOR_APPROVE_MIN)));
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
			cal.add(Calendar.DAY_OF_YEAR, 1);
		}
		return cal.getTimeInMillis();
	}

	public Optional<SeedTemplate> getSeed(int seedId) {
		return Optional.ofNullable(seedsById.get(seedId));
	}

	public Optional<SeedTemplate> getSeedByCrop(int cropId) {
		return seedsById.values().stream().filter(s -> s.cropId() == cropId).findFirst();
	}

	public List<SeedTemplate> getSeedsForCastle(int castleId) {
		return seedsByCastle.getOrDefault(castleId, List.of());
	}

	public Collection<SeedTemplate> allSeeds() {
		return Collections.unmodifiableCollection(seedsById.values());
	}

	public synchronized boolean setSeedProduction(int castleId, List<SeedProduction> prods, int period) {
		if (!isManorEnabled() || prods == null) {
			return false;
		}
		for (SeedProduction p : prods) {
			setSeedProduction(castleId, p, period);
		}
		return true;
	}

	public synchronized boolean setSeedProduction(int castleId, SeedProduction prod, int period) {
		if (!isManorEnabled()) {
			return false;
		}
		var map = period == PERIOD_CURRENT ? productionCurrent : productionNext;
		map.computeIfAbsent(castleId, k -> new ConcurrentHashMap<>()).put(prod.seedId(), prod);
		persistProduction(castleId, prod, period);
		return true;
	}

	public synchronized boolean setCropProcure(int castleId, List<CropProcure> procs, int period) {
		if (!isManorEnabled() || procs == null) {
			return false;
		}
		for (CropProcure p : procs) {
			setCropProcure(castleId, p, period);
		}
		return true;
	}

	public synchronized boolean setCropProcure(int castleId, CropProcure proc, int period) {
		if (!isManorEnabled()) {
			return false;
		}
		var map = period == PERIOD_CURRENT ? procureCurrent : procureNext;
		map.computeIfAbsent(castleId, k -> new ConcurrentHashMap<>()).put(proc.cropId(), proc);
		persistProcure(castleId, proc, period);
		return true;
	}

	public Optional<SeedProduction> getSeedProduction(int castleId, int seedId, int period) {
		var map = period == PERIOD_CURRENT ? productionCurrent.get(castleId) : productionNext.get(castleId);
		return map != null ? Optional.ofNullable(map.get(seedId)) : Optional.empty();
	}

	public Optional<CropProcure> getCropProcure(int castleId, int cropId, int period) {
		var map = period == PERIOD_CURRENT ? procureCurrent.get(castleId) : procureNext.get(castleId);
		return map != null ? Optional.ofNullable(map.get(cropId)) : Optional.empty();
	}

	/**
	 * Jogador compra sementes do feudo.
	 *
	 * @return preco total a pagar em adena, ou -1 se estoque insuficiente
	 */
	public synchronized long buySeed(int castleId, int seedId, int count) {
		if (!isManorEnabled() || count <= 0) {
			return -1;
		}
		var opt = getSeedProduction(castleId, seedId, PERIOD_CURRENT);
		if (opt.isEmpty()) {
			return -1;
		}
		var prod = opt.get();
		if (prod.amount() < count) {
			return -1;
		}
		prod.amount(prod.amount() - count);
		long totalCost = (long) prod.price() * count;
		if (castleManager != null && totalCost > 0) {
			castleManager.getCastleById(castleId).ifPresent(castle -> castle.addToTreasury(totalCost));
		}
		if (shouldSaveAllActions() || jdbc != null) {
			persistProduction(castleId, prod, PERIOD_CURRENT);
		}
		return totalCost;
	}

	/**
	 * Jogador vende colheita ao feudo.
	 *
	 * @return recompensa em adena paga ao jogador, ou -1 se demanda insuficiente
	 */
	public synchronized long sellCrop(int castleId, int cropId, int count) {
		if (!isManorEnabled() || count <= 0) {
			return -1;
		}
		var opt = getCropProcure(castleId, cropId, PERIOD_CURRENT);
		if (opt.isEmpty()) {
			return -1;
		}
		var proc = opt.get();
		if (proc.amount() < count) {
			return -1;
		}
		proc.amount(proc.amount() - count);
		if (shouldSaveAllActions() || jdbc != null) {
			persistProcure(castleId, proc, PERIOD_CURRENT);
		}
		return (long) proc.price() * count;
	}

	/**
	 * Manutencao das 06:00 AM: transfere o planejamento do proximo periodo para o periodo corrente,
	 * debita reservas da tesouraria do castelo (quando aplicavel) e persiste no SQL.
	 */
	public synchronized void approveNextPeriod() {
		procureCurrent.clear();
		for (var entry : procureNext.entrySet()) {
			int castleId = entry.getKey();
			Castle castle = castleManager != null ? castleManager.getCastleById(castleId).orElse(null) : null;
			Map<Integer, CropProcure> currentMap = new ConcurrentHashMap<>();
			for (var procEntry : entry.getValue().entrySet()) {
				CropProcure nextProc = procEntry.getValue();
				long reserveCost = (long) nextProc.startAmount() * nextProc.price();
				if (castle != null && reserveCost > 0) {
					if (castle.treasury() >= reserveCost) {
						castle.treasury(castle.treasury() - reserveCost);
					}
				}
				CropProcure approved = new CropProcure(
						nextProc.cropId(),
						nextProc.startAmount(),
						nextProc.startAmount(),
						nextProc.price(),
						nextProc.rewardType());
				currentMap.put(procEntry.getKey(), approved);
			}
			procureCurrent.put(castleId, currentMap);
		}

		productionCurrent.clear();
		for (var entry : productionNext.entrySet()) {
			int castleId = entry.getKey();
			Map<Integer, SeedProduction> currentMap = new ConcurrentHashMap<>();
			for (var prodEntry : entry.getValue().entrySet()) {
				SeedProduction nextProd = prodEntry.getValue();
				SeedProduction approved = new SeedProduction(
						nextProd.seedId(),
						nextProd.startAmount(),
						nextProd.startAmount(),
						nextProd.price());
				currentMap.put(prodEntry.getKey(), approved);
			}
			productionCurrent.put(castleId, currentMap);
		}

		saveAllToDb();
		log.info("CastleManorManager: Configurações do próximo período aprovadas para o período corrente");
	}

	/**
	 * Salva producoes de sementes e compras de colheita de todos os castelos no banco (#507).
	 */
	public synchronized int saveAllToDb() {
		if (jdbc == null) {
			return 0;
		}
		int saved = 0;
		for (var entry : productionCurrent.entrySet()) {
			for (SeedProduction p : entry.getValue().values()) {
				persistProduction(entry.getKey(), p, PERIOD_CURRENT);
				saved++;
			}
		}
		for (var entry : productionNext.entrySet()) {
			for (SeedProduction p : entry.getValue().values()) {
				persistProduction(entry.getKey(), p, PERIOD_NEXT);
				saved++;
			}
		}
		for (var entry : procureCurrent.entrySet()) {
			for (CropProcure p : entry.getValue().values()) {
				persistProcure(entry.getKey(), p, PERIOD_CURRENT);
				saved++;
			}
		}
		for (var entry : procureNext.entrySet()) {
			for (CropProcure p : entry.getValue().values()) {
				persistProcure(entry.getKey(), p, PERIOD_NEXT);
				saved++;
			}
		}
		return saved;
	}

	private void loadSeedsXml(String filePath) {
		File file = new File(filePath);
		if (!file.exists()) {
			log.warn("Arquivo seeds.xml nao encontrado em {}", file.getAbsolutePath());
			return;
		}

		try {
			var factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			Document doc = factory.newDocumentBuilder().parse(file);
			NodeList manorNodes = doc.getElementsByTagName("manor");

			for (int i = 0; i < manorNodes.getLength(); i++) {
				Node mNode = manorNodes.item(i);
				if (mNode.getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}
				Element mElem = (Element) mNode;
				int castleId = Integer.parseInt(mElem.getAttribute("id"));

				List<SeedTemplate> castleSeeds = new ArrayList<>();
				NodeList seedNodes = mElem.getElementsByTagName("seed");

				for (int j = 0; j < seedNodes.getLength(); j++) {
					Node sNode = seedNodes.item(j);
					if (sNode.getNodeType() != Node.ELEMENT_NODE) {
						continue;
					}
					Element sElem = (Element) sNode;

					int seedId = Integer.parseInt(sElem.getAttribute("id"));
					int level = Integer.parseInt(sElem.getAttribute("level"));
					int cropId = Integer.parseInt(sElem.getAttribute("cropId"));
					int matureId = Integer.parseInt(sElem.getAttribute("matureId"));
					int reward1 = Integer.parseInt(sElem.getAttribute("reward1"));
					int reward2 = Integer.parseInt(sElem.getAttribute("reward2"));
					boolean isAlt = Boolean.parseBoolean(sElem.getAttribute("isAlt"));
					int limitSeeds = Integer.parseInt(sElem.getAttribute("limitSeeds"));
					int limitCrops = Integer.parseInt(sElem.getAttribute("limitCrops"));

					SeedTemplate template = new SeedTemplate(seedId, castleId, level, cropId, matureId,
							reward1, reward2, isAlt, limitSeeds, limitCrops);

					seedsById.put(seedId, template);
					castleSeeds.add(template);
				}
				seedsByCastle.put(castleId, castleSeeds);
			}

			log.info("CastleManorManager carregou {} sementes em {} castelos", seedsById.size(), seedsByCastle.size());
		} catch (Exception ex) {
			log.error("Erro ao carregar seeds.xml de {}", filePath, ex);
		}
	}

	private void loadFromDb() {
		if (jdbc == null) {
			return;
		}
		try {
			var prodRows = jdbc.sql("""
					SELECT castle_id, seed_id, can_produce, start_produce, seed_price, period
					FROM castle_manor_production
					""").query().listOfRows();

			for (var r : prodRows) {
				int castleId = ((Number) r.get("castle_id")).intValue();
				int seedId = ((Number) r.get("seed_id")).intValue();
				int canProduce = ((Number) r.get("can_produce")).intValue();
				int startProduce = ((Number) r.get("start_produce")).intValue();
				int price = ((Number) r.get("seed_price")).intValue();
				int period = ((Number) r.get("period")).intValue();

				var map = period == PERIOD_CURRENT ? productionCurrent : productionNext;
				map.computeIfAbsent(castleId, k -> new ConcurrentHashMap<>())
						.put(seedId, new SeedProduction(seedId, canProduce, startProduce, price));
			}

			var procRows = jdbc.sql("""
					SELECT castle_id, crop_id, can_buy, start_buy, price, reward_type, period
					FROM castle_manor_procure
					""").query().listOfRows();

			for (var r : procRows) {
				int castleId = ((Number) r.get("castle_id")).intValue();
				int cropId = ((Number) r.get("crop_id")).intValue();
				int canBuy = ((Number) r.get("can_buy")).intValue();
				int startBuy = ((Number) r.get("start_buy")).intValue();
				int price = ((Number) r.get("price")).intValue();
				int rewardType = ((Number) r.get("reward_type")).intValue();
				int period = ((Number) r.get("period")).intValue();

				var map = period == PERIOD_CURRENT ? procureCurrent : procureNext;
				map.computeIfAbsent(castleId, k -> new ConcurrentHashMap<>())
						.put(cropId, new CropProcure(cropId, canBuy, startBuy, price, rewardType));
			}
			log.info("CastleManorManager: {} producoes e {} compras de colheita carregadas do banco",
					prodRows.size(), procRows.size());
		} catch (Exception ex) {
			log.warn("Nao foi possivel carregar manor tables do banco: {}", ex.getMessage());
		}
	}

	private void persistProduction(int castleId, SeedProduction p, int period) {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("""
					INSERT INTO castle_manor_production (castle_id, seed_id, can_produce, start_produce, seed_price, period)
					VALUES (:castle_id, :seed_id, :can_produce, :start_produce, :seed_price, :period)
					ON DUPLICATE KEY UPDATE
						can_produce = :can_produce,
						start_produce = :start_produce,
						seed_price = :seed_price
					""")
					.param("castle_id", castleId)
					.param("seed_id", p.seedId())
					.param("can_produce", p.amount())
					.param("start_produce", p.startAmount())
					.param("seed_price", p.price())
					.param("period", period)
					.update();
		} catch (Exception ex) {
			log.error("Erro ao persistir producao de semente {}", p.seedId(), ex);
		}
	}

	private void persistProcure(int castleId, CropProcure p, int period) {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("""
					INSERT INTO castle_manor_procure (castle_id, crop_id, can_buy, start_buy, price, reward_type, period)
					VALUES (:castle_id, :crop_id, :can_buy, :start_buy, :price, :reward_type, :period)
					ON DUPLICATE KEY UPDATE
						can_buy = :can_buy,
						start_buy = :start_buy,
						price = :price,
						reward_type = :reward_type
					""")
					.param("castle_id", castleId)
					.param("crop_id", p.cropId())
					.param("can_buy", p.amount())
					.param("start_buy", p.startAmount())
					.param("price", p.price())
					.param("reward_type", p.rewardType())
					.param("period", period)
					.update();
		} catch (Exception ex) {
			log.error("Erro ao persistir compra de colheita {}", p.cropId(), ex);
		}
	}
}
