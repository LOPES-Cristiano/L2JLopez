package com.lopez.l2j.game.manor;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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
 * Gerenciador do Sistema de Manor de Lineage II Interlude.
 * Carrega seeds.xml e gerencia producao de sementes e compra de colheitas pelos castelos.
 */
@Service
public class CastleManorManager {

	private static final Logger log = LoggerFactory.getLogger(CastleManorManager.class);

	public static final int PERIOD_CURRENT = 0;
	public static final int PERIOD_NEXT = 1;

	private final Map<Integer, SeedTemplate> seedsById = new ConcurrentHashMap<>();
	private final Map<Integer, List<SeedTemplate>> seedsByCastle = new ConcurrentHashMap<>();

	// castleId -> (cropId -> CropProcure)
	private final Map<Integer, Map<Integer, CropProcure>> procureCurrent = new ConcurrentHashMap<>();
	private final Map<Integer, Map<Integer, CropProcure>> procureNext = new ConcurrentHashMap<>();

	// castleId -> (seedId -> SeedProduction)
	private final Map<Integer, Map<Integer, SeedProduction>> productionCurrent = new ConcurrentHashMap<>();
	private final Map<Integer, Map<Integer, SeedProduction>> productionNext = new ConcurrentHashMap<>();

	private final JdbcClient jdbc;

	@Autowired
	public CastleManorManager(@Autowired(required = false) JdbcClient jdbc) {
		this("data/xml/world/seeds.xml", jdbc);
	}

	public CastleManorManager(String xmlPath, JdbcClient jdbc) {
		this.jdbc = jdbc;
		loadSeedsXml(xmlPath);
	}

	@PostConstruct
	public void init() {
		loadFromDb();
	}

	public Optional<SeedTemplate> getSeed(int seedId) {
		return Optional.ofNullable(seedsById.get(seedId));
	}

	public List<SeedTemplate> getSeedsForCastle(int castleId) {
		return seedsByCastle.getOrDefault(castleId, List.of());
	}

	public Collection<SeedTemplate> allSeeds() {
		return Collections.unmodifiableCollection(seedsById.values());
	}

	public synchronized boolean setSeedProduction(int castleId, SeedProduction prod, int period) {
		var map = period == PERIOD_CURRENT ? productionCurrent : productionNext;
		map.computeIfAbsent(castleId, k -> new ConcurrentHashMap<>()).put(prod.seedId(), prod);
		persistProduction(castleId, prod, period);
		return true;
	}

	public synchronized boolean setCropProcure(int castleId, CropProcure proc, int period) {
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
		var opt = getSeedProduction(castleId, seedId, PERIOD_CURRENT);
		if (opt.isEmpty()) {
			return -1;
		}
		var prod = opt.get();
		if (prod.amount() < count) {
			return -1;
		}
		prod.amount(prod.amount() - count);
		persistProduction(castleId, prod, PERIOD_CURRENT);
		return (long) prod.price() * count;
	}

	/**
	 * Jogador vende colheita ao feudo.
	 *
	 * @return recompensa em adena paga ao jogador, ou -1 se demanda insuficiente
	 */
	public synchronized long sellCrop(int castleId, int cropId, int count) {
		var opt = getCropProcure(castleId, cropId, PERIOD_CURRENT);
		if (opt.isEmpty()) {
			return -1;
		}
		var proc = opt.get();
		if (proc.amount() < count) {
			return -1;
		}
		proc.amount(proc.amount() - count);
		persistProcure(castleId, proc, PERIOD_CURRENT);
		return (long) proc.price() * count;
	}

	public synchronized void approveNextPeriod() {
		procureCurrent.clear();
		for (var entry : procureNext.entrySet()) {
			procureCurrent.put(entry.getKey(), new ConcurrentHashMap<>(entry.getValue()));
		}

		productionCurrent.clear();
		for (var entry : productionNext.entrySet()) {
			productionCurrent.put(entry.getKey(), new ConcurrentHashMap<>(entry.getValue()));
		}
		log.info("CastleManorManager: Configurações do próximo período aprovadas para o período corrente");
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
